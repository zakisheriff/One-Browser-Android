package com.oneatom.onebrowser.ui.components

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.oneatom.onebrowser.data.Tab
import com.oneatom.onebrowser.ui.theme.DarkBackground
import com.oneatom.onebrowser.ui.theme.LightBackground
import com.oneatom.onebrowser.webview.OneBrowserJsInterface

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
        tab: Tab,
        isDarkTheme: Boolean,
        onTitleChanged: (String) -> Unit,
        onUrlChanged: (String) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onLoadingChanged: (Boolean) -> Unit,
        onCanGoBackChanged: (Boolean) -> Unit,
        onCanGoForwardChanged: (Boolean) -> Unit,
        onSslSecureChanged: (Boolean) -> Unit,
        onNavigate: (String) -> Unit,
        onOpenSettings: () -> Unit,
        navigationActions:
                kotlinx.coroutines.flow.SharedFlow<
                        com.oneatom.onebrowser.viewmodel.NavigationAction>? =
                null,
        onCaptureThumbnail: (Bitmap) -> Unit = {},
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
    val currentTabId = tab.id

    // Helper to capture thumbnail
    fun captureWebView(view: WebView) {
        try {
            if (view.width > 0 && view.height > 0) {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                view.draw(canvas)
                onCaptureThumbnail(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Remember webview for this tab
    val webView =
            remember(tab.id) {
                WebView(context).apply {
                    layoutParams =
                            ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )

                    // Configure WebView settings for MOBILE rendering
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true

                        // Viewport settings for proper mobile rendering
                        useWideViewPort = true // Allow viewport meta tag to work
                        loadWithOverviewMode = true // Fit content to screen width

                        // Set initial scale to 0 to let page decide
                        setInitialScale(0)

                        // Zoom settings
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false

                        // Default text zoom
                        textZoom = 100

                        // File access
                        allowFileAccess = true
                        allowContentAccess = true

                        // Mixed content and cache
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT

                        // Media
                        mediaPlaybackRequiresUserGesture = false

                        // Set Chrome-like mobile User Agent
                        val mobileUserAgent =
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        userAgentString = mobileUserAgent

                        // Enable safe browsing
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            safeBrowsingEnabled = true
                        }
                    }

                    // Add JavaScript interface
                    addJavascriptInterface(
                            OneBrowserJsInterface(
                                    onNavigate = onNavigate,
                                    onOpenSettings = onOpenSettings,
                                    getTheme = { if (isDarkTheme) "dark" else "light" }
                            ),
                            "OneBrowser"
                    )

                    // Set WebViewClient
                    webViewClient =
                            object : WebViewClient() {
                                override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    url?.let { onUrlChanged(it) }
                                    onLoadingChanged(true)
                                    onSslSecureChanged(url?.startsWith("https://") == true)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    onLoadingChanged(false)
                                    view?.let {
                                        onCanGoBackChanged(it.canGoBack())
                                        onCanGoForwardChanged(it.canGoForward())
                                        // Capture thumbnail on finish
                                        captureWebView(it)
                                    }

                                    // Inject theme CSS for internal pages
                                    if (url?.startsWith("file:///android_asset/") == true) {
                                        val themeClass = if (isDarkTheme) "dark" else "light"
                                        view?.evaluateJavascript(
                                                "document.documentElement.className = '$themeClass';",
                                                null
                                        )
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false

                                    // Handle internal URLs
                                    when {
                                        url.startsWith("onebrowser://settings") -> {
                                            onOpenSettings()
                                            return true
                                        }
                                        url.startsWith("onebrowser://newtab") -> {
                                            view?.loadUrl("https://www.google.com")
                                            return true
                                        }
                                    }

                                    return false
                                }

                                override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: android.net.http.SslError?
                                ) {
                                    handler?.cancel()
                                    onSslSecureChanged(false)
                                }
                            }

                    // Set Download Listener
                    setDownloadListener {
                            url,
                            userAgent,
                            contentDisposition,
                            mimetype,
                            contentLength ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(url))
                            request.setMimeType(mimetype)
                            val cookies = CookieManager.getInstance().getCookie(url)
                            request.addRequestHeader("cookie", cookies)
                            request.addRequestHeader("User-Agent", userAgent)
                            request.setDescription("Downloading file...")
                            request.setTitle(
                                    URLUtil.guessFileName(url, contentDisposition, mimetype)
                            )
                            request.allowScanningByMediaScanner()
                            request.setNotificationVisibility(
                                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            )
                            request.setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    URLUtil.guessFileName(url, contentDisposition, mimetype)
                            )

                            val dm =
                                    context.getSystemService(Context.DOWNLOAD_SERVICE) as
                                            DownloadManager
                            dm.enqueue(request)
                            Toast.makeText(context, "Downloading...", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                            context,
                                            "Download failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }

                    // Set WebChromeClient for progress and title
                    webChromeClient =
                            object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    onProgressChanged(newProgress)
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    title?.let { onTitleChanged(it) }
                                }

                                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                    super.onReceivedIcon(view, icon)
                                }
                            }
                }
            }

    // Listen for navigation events
    LaunchedEffect(currentTabId) {
        navigationActions?.collect { action ->
            when (action) {
                is com.oneatom.onebrowser.viewmodel.NavigationAction.GoBack -> {
                    if (action.tabId == currentTabId) {
                        if (webView.canGoBack()) webView.goBack()
                    }
                }
                is com.oneatom.onebrowser.viewmodel.NavigationAction.GoForward -> {
                    if (action.tabId == currentTabId) {
                        if (webView.canGoForward()) webView.goForward()
                    }
                }
                is com.oneatom.onebrowser.viewmodel.NavigationAction.Reload -> {
                    if (action.tabId == currentTabId) {
                        webView.reload()
                    }
                }
                is com.oneatom.onebrowser.viewmodel.NavigationAction.Stop -> {
                    if (action.tabId == currentTabId) {
                        webView.stopLoading()
                    }
                }
            }
        }
    }

    // Update theme when it changes
    LaunchedEffect(isDarkTheme) {
        val currentUrl = webView.url
        if (currentUrl?.startsWith("file:///android_asset/") == true) {
            val themeClass = if (isDarkTheme) "dark" else "light"
            webView.evaluateJavascript(
                    "document.documentElement.className = '$themeClass'; " +
                            "if (window.onThemeChange) window.onThemeChange('$themeClass');",
                    null
            )
        }
    }

    // Load URL when it changes
    LaunchedEffect(tab.url) {
        val urlToLoad =
                if (tab.url.isEmpty()) {
                    "https://www.google.com"
                } else {
                    tab.url
                }

        if (webView.url != urlToLoad) {
            webView.loadUrl(urlToLoad)
        }
    }

    // Properly destroy WebView when this composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Capture one last time before destroy
            captureWebView(webView)
            webView.destroy()
        }
    }

    Box(
            modifier =
                    modifier.fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(backgroundColor)
    ) {
        AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
        )
    }
}

// Extension functions for WebView navigation
fun WebView.goBackIfPossible(): Boolean {
    return if (canGoBack()) {
        goBack()
        true
    } else {
        false
    }
}

fun WebView.goForwardIfPossible(): Boolean {
    return if (canGoForward()) {
        goForward()
        true
    } else {
        false
    }
}
