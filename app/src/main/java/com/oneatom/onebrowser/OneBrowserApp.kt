package com.oneatom.onebrowser

import android.app.Application
import android.webkit.WebView

class OneBrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable WebView debugging in debug builds
        // Using applicationInfo.flags to check debug mode since BuildConfig requires a build first
        val isDebuggable =
                (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
