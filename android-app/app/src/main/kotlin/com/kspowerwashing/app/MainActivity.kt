package com.kspowerwashing.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Show system splash screen while WebView loads.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Dark (light-on-dark) status/navigation bars to match the navy theme.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        window.statusBarColor = 0xFF04182E.toInt()
        window.navigationBarColor = 0xFF04182E.toInt()

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()

        // Route system Back through WebView history before finishing the Activity.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // WebViewAssetLoader serves the HTML from an https:// origin so that
        // localStorage, Fetch API, and CSS env() all work as expected.
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Assets are served under https://appassets.androidplatform.net/assets/
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true       // localStorage / sessionStorage
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false    // disable pinch-zoom; the site is already responsive
            displayZoomControls = false
            setSupportZoom(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            // Allow mixed content so the AI proxy endpoint (http or https) can be reached.
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Expose native Android features to the JavaScript running in the page.
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")

        webView.webViewClient = object : WebViewClientCompat() {

            // Let the AssetLoader intercept requests for local assets.
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): android.webkit.WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
                    ?: super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                return when {
                    // Let the AssetLoader serve its own domain internally.
                    url.contains("appassets.androidplatform.net") -> false

                    // Open tel: links in the system dialer.
                    url.startsWith("tel:") -> {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
                        true
                    }

                    // Open mailto: links in the device email client.
                    url.startsWith("mailto:") -> {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
                        true
                    }

                    // Open any other http/https URL in the system browser.
                    url.startsWith("http://") || url.startsWith("https://") -> {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    }

                    else -> false
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                // Only surface an error overlay when the main frame fails to load.
                if (request.isForMainFrame) {
                    view.loadUrl(
                        "javascript:document.body.innerHTML=" +
                                "'<div style=\"font-family:sans-serif;padding:40px;text-align:center;" +
                                "background:%2304182e;color:%23f8fafc;min-height:100vh;\">" +
                                "<h2 style=\\\"color:%230ea5e9;\\\">Unable to load</h2>" +
                                "<p>Please restart the app and check your connection.</p>" +
                                "<p style=\\\"margin-top:24px;\\\">&#128222; (316) 680-6549</p></div>'"
                    )
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }
    }

    // Pause/resume WebView with the Activity lifecycle to free resources.
    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
