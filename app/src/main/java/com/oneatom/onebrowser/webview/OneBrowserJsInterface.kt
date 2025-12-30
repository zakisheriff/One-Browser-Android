package com.oneatom.onebrowser.webview

import android.webkit.JavascriptInterface

class OneBrowserJsInterface(
        private val onNavigate: (String) -> Unit,
        private val onOpenSettings: () -> Unit,
        private val getTheme: () -> String
) {

    @JavascriptInterface
    fun navigate(url: String) {
        onNavigate(url)
    }

    @JavascriptInterface
    fun openSettings() {
        onOpenSettings()
    }

    @JavascriptInterface
    fun getTheme(): String {
        return getTheme.invoke()
    }

    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("OneBrowser", message)
    }
}
