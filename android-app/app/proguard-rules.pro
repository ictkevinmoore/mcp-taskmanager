# Keep @JavascriptInterface methods so they remain callable from the WebView JS context.
-keepclassmembers class com.kspowerwashing.app.WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Keep source file names and line numbers in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
