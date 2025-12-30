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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

    // State for Custom View (Fullscreen)
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Hoist WebView instance reference for fullscreen use
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

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

    // Initialize WebView
    val webView =
            remember(tab.id) {
                WebView(context).apply {
                    layoutParams =
                            ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true

                    // Zoom settings
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    try {
                        // settings.setInitialScale(1) // Can be problematic on some devices
                    } catch (e: Exception) {}

                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    // Text size fix
                    settings.textZoom = 100

                    // File access
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    try {
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                    } catch (e: Exception) {}

                    settings.mediaPlaybackRequiresUserGesture = false

                    // User Agent
                    // Use a standard generic Android user agent or keep default
                    val defaultUserAgent = settings.userAgentString
                    settings.userAgentString = defaultUserAgent.replace("; wv", "")

                    // Safe Browsing
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = true
                    }

                    // Javascript Interface
                    // Javascript Interface
                    addJavascriptInterface(
                            OneBrowserJsInterface(
                                    onNavigate = { url ->
                                        onNavigate(url)
                                    }, // Wrap if needed or pass directly if signature matches
                                    onOpenSettings = { onOpenSettings() },
                                    getTheme = { if (isDarkTheme) "dark" else "light" }
                            ),
                            "OneBrowser"
                    )
                    addJavascriptInterface(
                            object {
                                @android.webkit.JavascriptInterface
                                fun onData(data: String) {
                                    // Handle data
                                }
                            },
                            "Android"
                    )

                    // Capture reference
                    webViewRef = this

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
                                    // Check if it's an internal page
                                    if (url?.startsWith("file:///android_asset/") == true) {
                                        // Inject theme if needed
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    onLoadingChanged(false)
                                    view?.let {
                                        onCanGoBackChanged(it.canGoBack())
                                        onCanGoForwardChanged(it.canGoForward())
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

                                    if (url.startsWith("http") || url.startsWith("file")) {
                                        return false
                                    }
                                    try {
                                        val intent =
                                                android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        request.url
                                                )
                                        context.startActivity(intent)
                                        return true
                                    } catch (e: Exception) {
                                        return true
                                    }
                                }

                                override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: android.net.http.SslError?
                                ) {
                                    handler?.proceed() // Warning: Checks disabled for demo
                                    onSslSecureChanged(false)
                                }
                            }

                    // Download Listener
                    setDownloadListener {
                            url,
                            userAgent,
                            contentDisposition,
                            mimetype,
                            contentLength ->
                        try {
                            val request = DownloadManager.Request(android.net.Uri.parse(url))
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
                                    android.os.Environment.DIRECTORY_DOWNLOADS,
                                    URLUtil.guessFileName(url, contentDisposition, mimetype)
                            )
                            val dm =
                                    context.getSystemService(
                                            android.content.Context.DOWNLOAD_SERVICE
                                    ) as
                                            DownloadManager
                            dm.enqueue(request)
                            android.widget.Toast.makeText(
                                            context,
                                            "Downloading...",
                                            android.widget.Toast.LENGTH_LONG
                                    )
                                    .show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                            context,
                                            "Download failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }

                    webChromeClient =
                            object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    onProgressChanged(newProgress)
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    title?.let { onTitleChanged(it) }
                                }

                                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                    super.onReceivedIcon(view, icon)
                                }

                                override fun onShowCustomView(
                                        view: android.view.View?,
                                        callback: CustomViewCallback?
                                ) {
                                    if (customView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }

                                    val activity = context as? android.app.Activity ?: return
                                    val decorView =
                                            activity.window.decorView as? ViewGroup ?: return

                                    // Create ComposeView wrapper
                                    val composeView =
                                            androidx.compose.ui.platform.ComposeView(context)
                                                    .apply {
                                                        setContent {
                                                            Box(
                                                                    modifier =
                                                                            Modifier.fillMaxSize()
                                                                                    .background(
                                                                                            androidx.compose
                                                                                                    .ui
                                                                                                    .graphics
                                                                                                    .Color
                                                                                                    .Black
                                                                                    )
                                                            ) {
                                                                // The Video Content (AndroidView)
                                                                AndroidView(
                                                                        factory = { _ ->
                                                                            view
                                                                                    ?: android.view
                                                                                            .View(
                                                                                                    context
                                                                                            )
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxSize()
                                                                )

                                                                // Native Overlay
                                                                var isPlaying by remember {
                                                                    mutableStateOf(false)
                                                                }
                                                                var currentTime by remember {
                                                                    mutableFloatStateOf(0f)
                                                                }
                                                                var duration by remember {
                                                                    mutableFloatStateOf(0f)
                                                                }
                                                                var title by remember {
                                                                    mutableStateOf("")
                                                                }

                                                                // Poll for video state
                                                                LaunchedEffect(Unit) {
                                                                    while (true) {
                                                                        delay(500)
                                                                        withContext(
                                                                                kotlinx.coroutines
                                                                                        .Dispatchers
                                                                                        .Main
                                                                        ) {
                                                                            webViewRef
                                                                                    ?.evaluateJavascript(
                                                                                            """
                                                 (function() {
                                                    var v = document.querySelector('video');
                                                    if (v) {
                                                        return JSON.stringify({
                                                            isPlaying: !v.paused,
                                                            currentTime: v.currentTime,
                                                            duration: v.duration,
                                                            title: document.title
                                                        });
                                                    }
                                                    return null;
                                                 })();
                                                 """.trimIndent()
                                                                                    ) { json ->
                                                                                        if (json !=
                                                                                                        null &&
                                                                                                        json !=
                                                                                                                "null"
                                                                                        ) {
                                                                                            try {
                                                                                                val obj =
                                                                                                        org.json
                                                                                                                .JSONObject(
                                                                                                                        json
                                                                                                                )
                                                                                                isPlaying =
                                                                                                        obj.optBoolean(
                                                                                                                "isPlaying"
                                                                                                        )
                                                                                                currentTime =
                                                                                                        obj.optDouble(
                                                                                                                        "currentTime"
                                                                                                                )
                                                                                                                .toFloat()
                                                                                                duration =
                                                                                                        obj.optDouble(
                                                                                                                        "duration"
                                                                                                                )
                                                                                                                .toFloat()
                                                                                                title =
                                                                                                        obj.optString(
                                                                                                                "title"
                                                                                                        )
                                                                                            } catch (
                                                                                                    e:
                                                                                                            Exception) {
                                                                                                e.printStackTrace()
                                                                                            }
                                                                                        }
                                                                                    }
                                                                        }
                                                                    }
                                                                }

                                                                VideoPlayerControls(
                                                                        isPlaying = isPlaying,
                                                                        currentTime = currentTime,
                                                                        duration = duration,
                                                                        title = title,
                                                                        onPlayPause = {
                                                                            webViewRef
                                                                                    ?.evaluateJavascript(
                                                                                            "var v = document.querySelector('video'); if (v) { if (v.paused) v.play(); else v.pause(); }",
                                                                                            null
                                                                                    )
                                                                            isPlaying =
                                                                                    !isPlaying // Optimistic update
                                                                        },
                                                                        onSeek = { newTime ->
                                                                            webViewRef
                                                                                    ?.evaluateJavascript(
                                                                                            "var v = document.querySelector('video'); if (v) v.currentTime = $newTime;",
                                                                                            null
                                                                                    )
                                                                            currentTime = newTime
                                                                        },
                                                                        onClose = {
                                                                            webViewRef
                                                                                    ?.evaluateJavascript(
                                                                                            "if (document.exitFullscreen) document.exitFullscreen();",
                                                                                            null
                                                                                    )
                                                                        },
                                                                        onRotate = {
                                                                            val requestedOrientation =
                                                                                    if (activity.requestedOrientation ==
                                                                                                    android.content
                                                                                                            .pm
                                                                                                            .ActivityInfo
                                                                                                            .SCREEN_ORIENTATION_LANDSCAPE
                                                                                    )
                                                                                            android.content
                                                                                                    .pm
                                                                                                    .ActivityInfo
                                                                                                    .SCREEN_ORIENTATION_PORTRAIT
                                                                                    else
                                                                                            android.content
                                                                                                    .pm
                                                                                                    .ActivityInfo
                                                                                                    .SCREEN_ORIENTATION_LANDSCAPE
                                                                            activity.requestedOrientation =
                                                                                    requestedOrientation
                                                                        }
                                                                )
                                                            }
                                                        }
                                                    }

                                    // Add to DecorView
                                    decorView.addView(
                                            composeView,
                                            ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                    )

                                    // Hide UI
                                    decorView.systemUiVisibility =
                                            (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                                                    android.view.View
                                                            .SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                                    android.view.View
                                                            .SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

                                    customView = composeView
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    val activity = context as? android.app.Activity ?: return
                                    val decorView =
                                            activity.window.decorView as? ViewGroup ?: return

                                    // Remove the custom view
                                    if (customView != null) {
                                        decorView.removeView(customView)
                                        customView = null
                                    }

                                    // Restore System UI (Clear flags)
                                    decorView.systemUiVisibility =
                                            android.view.View.SYSTEM_UI_FLAG_VISIBLE

                                    // Notify callback
                                    customViewCallback?.onCustomViewHidden()
                                    customViewCallback = null

                                    // Reset orientation
                                    // activity.requestedOrientation =
                                    // android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                }
            }

    // Handle Back Press when in Fullscreen
    androidx.activity.compose.BackHandler(enabled = customView != null) {
        val activity = context as? android.app.Activity
        val decorView = activity?.window?.decorView as? ViewGroup

        if (customView != null && decorView != null) {
            decorView.removeView(customView)
            customView = null
            decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
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
