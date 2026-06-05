package com.kspowerwashing.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * JavaScript bridge injected into the WebView as `window.AndroidBridge`.
 *
 * Call from the HTML/JS side:
 *   window.AndroidBridge?.callPhone('3166806549')
 *   window.AndroidBridge?.showToast('Booking saved!')
 *   if (window.AndroidBridge?.isAndroid()) { ... }
 */
class WebAppInterface(private val context: Context) {

    /** Show a short native Android toast notification. */
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /** Open the system phone dialer pre-filled with [number]. */
    @JavascriptInterface
    fun callPhone(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Open the device email client addressed to [address]. */
    @JavascriptInterface
    fun openEmail(address: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Open [url] in the system browser. */
    @JavascriptInterface
    fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Returns true — lets the page detect it is running inside the Android wrapper. */
    @JavascriptInterface
    fun isAndroid(): Boolean = true

    /** Returns the device Android API level (e.g. 34 for Android 14). */
    @JavascriptInterface
    fun getApiLevel(): Int = android.os.Build.VERSION.SDK_INT
}
