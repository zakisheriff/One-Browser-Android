# Add project specific ProGuard rules here.
# Keep WebView JavaScript interface
-keepclassmembers class com.oneatom.onebrowser.webview.OneBrowserJsInterface {
    public *;
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
