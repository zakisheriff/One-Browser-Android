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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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

    // --- Native Video Assistant & Player States ---
    var showNativePlayer by remember { mutableStateOf(false) }
    var isVideoPresent by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var title by remember { mutableStateOf("") }

    // Context Menu State
    var contextMenuUri by remember { mutableStateOf<String?>(null) }

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
                    val defaultUserAgent = settings.userAgentString
                    settings.userAgentString = defaultUserAgent.replace("; wv", "")

                    // Safe Browsing
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = true
                    }

                    // Javascript Interface
                    addJavascriptInterface(
                            OneBrowserJsInterface(
                                    onNavigate = { url -> onNavigate(url) },
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
                                    handler?.proceed()
                                    onSslSecureChanged(false)
                                }
                            }

                    setOnLongClickListener { view ->
                        val result = (view as WebView).hitTestResult
                        // Check for image or image link
                        if (result.type == WebView.HitTestResult.IMAGE_TYPE ||
                                        result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                        ) {
                            val url = result.extra
                            if (url != null) {
                                contextMenuUri = url
                                true // Consume event
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }

                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                        // Handle blob and data URLs
                        if (url.startsWith("blob:") || url.startsWith("data:")) {
                            Toast.makeText(
                                            context,
                                            "Blob/Data downloads not fully supported.",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                            return@setDownloadListener
                        }

                        // Robust Filename Guessing
                        var filename = URLUtil.guessFileName(url, contentDisposition, mimetype)

                        // Fix for .bin or missing extension
                        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
                        val mimeTypeMap = MimeTypeMap.getSingleton()
                        val mimeExtension = mimeTypeMap.getExtensionFromMimeType(mimetype)
                        val urlPath =
                                try {
                                    Uri.parse(url).path ?: ""
                                } catch (e: Exception) {
                                    ""
                                }
                        val urlExtFallback =
                                if (urlPath.contains(".")) urlPath.substringAfterLast(".") else null

                        // If filename ends in .bin or has no extension, try to fix it
                        if (filename.endsWith(".bin") || !filename.contains(".")) {
                            if (mimetype == "application/vnd.android.package-archive") {
                                filename = filename.replace(".bin", "") + ".apk"
                            } else if (mimeExtension != null) {
                                filename = filename.replace(".bin", "") + "." + mimeExtension
                            } else if (urlExtFallback != null && urlExtFallback.length in 2..5) {
                                // Trust URL extension if it looks like a file extension (e.g. apk,
                                // pdf, zip)
                                filename = filename.replace(".bin", "") + "." + urlExtFallback
                            }
                        }

                        // Explicitly fix common APK issue where server sends wrong mime
                        if ((url.endsWith(".apk") || (urlExtFallback == "apk")) &&
                                        !filename.endsWith(".apk")
                        ) {
                            filename =
                                    if (filename.endsWith(".bin")) filename.replace(".bin", ".apk")
                                    else "$filename.apk"
                        }

                        // Force APK if mime matches
                        if (mimetype == "application/vnd.android.package-archive" &&
                                        !filename.endsWith(".apk")
                        ) {
                            filename += ".apk"
                        }

                        try {
                            // Use Custom Engine for true Pause/Resume support
                            com.oneatom.onebrowser.data.CustomDownloadEngine.startDownload(
                                    context,
                                    url,
                                    filename
                            )

                            /*
                            // Deprecated System Download Manager logic (kept for reference or fallback if needed)
                             val request = DownloadManager.Request(Uri.parse(url))
                             request.setMimeType(mimetype)
                             // ...
                             val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                             dm.enqueue(request)
                            */

                            // Start Custom Service
                            com.oneatom.onebrowser.services.DownloadService.start(context)

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

                                    decorView.addView(
                                            view,
                                            ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                    )

                                    decorView.systemUiVisibility =
                                            (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                                                    android.view.View
                                                            .SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                                    android.view.View
                                                            .SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

                                    customView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    val activity = context as? android.app.Activity ?: return
                                    val decorView =
                                            activity.window.decorView as? ViewGroup ?: return

                                    if (customView != null) {
                                        decorView.removeView(customView)
                                        customView = null
                                    }

                                    decorView.systemUiVisibility =
                                            android.view.View.SYSTEM_UI_FLAG_VISIBLE

                                    customViewCallback?.onCustomViewHidden()
                                    customViewCallback = null
                                }

                                override fun onConsoleMessage(
                                        consoleMessage: ConsoleMessage?
                                ): Boolean {
                                    android.util.Log.d(
                                            "OneBrowserJS",
                                            consoleMessage?.message() ?: ""
                                    )
                                    return true
                                }
                            }
                }
            }

    // Poll for video state
    LaunchedEffect(Unit) {
        while (true) {
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                // Fallback for YouTube
                if (webView.url?.contains("youtube.com/watch") == true) {
                    isVideoPresent = true
                }

                webViewRef?.evaluateJavascript(
                        """
                        (function() {
                        var v = document.querySelector('video');
                        if (!v && document.getElementsByTagName('video').length > 0) {
                            v = document.getElementsByTagName('video')[0];
                        }
                        if (v) {
                            console.log("Video found: " + v.src);
                            return JSON.stringify({
                                isPresent: true,
                                isPlaying: !v.paused,
                                currentTime: v.currentTime,
                                duration: v.duration,
                                title: document.title
                            });
                        }
                        return JSON.stringify({ isPresent: false });
                        })();
                        """.trimIndent()
                ) { json ->
                    if (json != null && json != "null") {
                        try {
                            val obj = org.json.JSONObject(json)
                            val present = obj.optBoolean("isPresent")

                            // Only update if true or if purely relying on JS
                            if (present) {
                                isVideoPresent = true
                                val newIsPlaying = obj.optBoolean("isPlaying")
                                isPlaying = newIsPlaying

                                val newDuration = obj.optDouble("duration").toFloat()
                                if (newDuration > 0) duration = newDuration

                                val newTime = obj.optDouble("currentTime").toFloat()
                                currentTime = newTime

                                title = obj.optString("title")
                            } else {
                                // Only reset if NOT on a forced site
                                if (webView.url?.contains("youtube.com/watch") != true) {
                                    isVideoPresent = false
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            delay(200)
        }
    }

    // Handle Back Press
    BackHandler(enabled = customView != null || showNativePlayer) {
        val activity = context as? android.app.Activity
        val decorView = activity?.window?.decorView as? ViewGroup

        if (showNativePlayer) {
            showNativePlayer = false
            // Reset system UI
            decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
        } else if (customView != null && decorView != null) {
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

        // Video Assistant Button (Purple FAB)
        if (isVideoPresent && !showNativePlayer && customView == null) {
            Box(
                    modifier =
                            Modifier.align(Alignment.BottomEnd)
                                    .padding(bottom = 24.dp, end = 16.dp)
                                    .zIndex(10f) // Ensure it is drawn on top of WebView
            ) {
                FloatingActionButton(
                        onClick = { showNativePlayer = true },
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White,
                        shape = CircleShape
                ) { Icon(Icons.Filled.PlayArrow, contentDescription = "Open Video Player") }
            }
        }

        // Native Player Overlay
        if (showNativePlayer) {
            val context = LocalContext.current

            // Manage System UI and Video Element CSS
            DisposableEffect(Unit) {
                val activity = context as? android.app.Activity
                val window = activity?.window

                // 1. Hide System UI
                window?.decorView?.systemUiVisibility =
                        (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

                // 2. Inject CSS to force video to fill screen
                webView.evaluateJavascript(
                        """
                    (function() {
                        var v = document.querySelector('video');
                        if (!v && document.getElementsByTagName('video').length > 0) {
                            v = document.getElementsByTagName('video')[0];
                        }
                        if (v) {
                            v.dataset.originalStyle = v.getAttribute('style') || '';
                            v.style.position = 'fixed';
                            v.style.top = '0';
                            v.style.left = '0';
                            v.style.width = '100vw';
                            v.style.height = '100vh';
                            v.style.zIndex = '2147483647';
                            v.style.backgroundColor = 'black';
                            v.style.objectFit = 'contain';
                            document.body.style.overflow = 'hidden';
                        }
                    })();
                """.trimIndent(),
                        null
                )

                onDispose {
                    // Restore System UI
                    window?.decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE

                    // Restore Video Element Style
                    webView.evaluateJavascript(
                            """
                        (function() {
                            var v = document.querySelector('video');
                            if (!v && document.getElementsByTagName('video').length > 0) {
                                v = document.getElementsByTagName('video')[0];
                            }
                            if (v) {
                                v.setAttribute('style', v.dataset.originalStyle || '');
                                document.body.style.overflow = '';
                            }
                        })();
                     """.trimIndent(),
                            null
                    )
                }
            }

            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            Color.Transparent
                                    ) // Transparent to see WebView content behind
                                    .zIndex(20f)
            ) {
                VideoPlayerControls(
                        isPlaying = isPlaying,
                        currentTime = currentTime,
                        duration = duration,
                        title = title,
                        onPlayPause = {
                            // Explicit toggle based on CURRENT UI STATE to prevent desync
                            if (isPlaying) {
                                webView.evaluateJavascript(
                                        "var v = document.querySelector('video'); if (!v && document.getElementsByTagName('video').length > 0) v = document.getElementsByTagName('video')[0]; if (v) v.pause();",
                                        null
                                )
                            } else {
                                webView.evaluateJavascript(
                                        "var v = document.querySelector('video'); if (!v && document.getElementsByTagName('video').length > 0) v = document.getElementsByTagName('video')[0]; if (v) v.play();",
                                        null
                                )
                            }
                            isPlaying = !isPlaying // Optimistic update
                        },
                        onSeek = { newTime ->
                            webView.evaluateJavascript(
                                    "var v = document.querySelector('video'); if (!v && document.getElementsByTagName('video').length > 0) v = document.getElementsByTagName('video')[0]; if (v) v.currentTime = $newTime;",
                                    null
                            )
                            currentTime = newTime
                        },
                        onClose = { showNativePlayer = false },
                        onRotate = {
                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                val requestedOrientation =
                                        if (activity.requestedOrientation ==
                                                        android.content.pm.ActivityInfo
                                                                .SCREEN_ORIENTATION_LANDSCAPE
                                        )
                                                android.content.pm.ActivityInfo
                                                        .SCREEN_ORIENTATION_PORTRAIT
                                        else
                                                android.content.pm.ActivityInfo
                                                        .SCREEN_ORIENTATION_LANDSCAPE
                                activity.requestedOrientation = requestedOrientation
                            }
                        }
                )
            }
        }

        // Permission Launcher
        val notificationPermissionLauncher =
                androidx.activity.compose.rememberLauncherForActivityResult(
                        contract =
                                androidx.activity.result.contract.ActivityResultContracts
                                        .RequestPermission(),
                        onResult = { isGranted ->
                            if (isGranted) {
                                // Permission granted
                            }
                        }
                )

        // Context Menu
        if (contextMenuUri != null) {
            androidx.compose.material3.DropdownMenu(
                    expanded = true,
                    onDismissRequest = { contextMenuUri = null },
                    modifier = Modifier.background(backgroundColor)
            ) {
                androidx.compose.material3.DropdownMenuItem(
                        text = {
                            androidx.compose.material3.Text(
                                    "Download Image",
                                    color =
                                            if (isDarkTheme)
                                                    com.oneatom.onebrowser.ui.theme.DarkText
                                            else com.oneatom.onebrowser.ui.theme.LightText
                            )
                        },
                        onClick = {
                            val url = contextMenuUri!!

                            // Check Permission for Android 13+
                            if (Build.VERSION.SDK_INT >= 33 &&
                                            androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context,
                                                    android.Manifest.permission.POST_NOTIFICATIONS
                                            ) !=
                                                    android.content.pm.PackageManager
                                                            .PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                )
                            }

                            // Trigger Download with Advanced Tracker
                            try {
                                val request = DownloadManager.Request(Uri.parse(url))
                                request.setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_HIDDEN
                                )
                                request.setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        URLUtil.guessFileName(url, null, "image/*")
                                )
                                val dm =
                                        context.getSystemService(Context.DOWNLOAD_SERVICE) as
                                                DownloadManager
                                dm.enqueue(request)

                                // Start Custom Notification Service
                                com.oneatom.onebrowser.services.DownloadService.start(context)
                                Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT)
                                        .show()
                            }
                            contextMenuUri = null
                        }
                )
                androidx.compose.material3.DropdownMenuItem(
                        text = {
                            androidx.compose.material3.Text(
                                    "Open in New Tab",
                                    color =
                                            if (isDarkTheme)
                                                    com.oneatom.onebrowser.ui.theme.DarkText
                                            else com.oneatom.onebrowser.ui.theme.LightText
                            )
                        },
                        onClick = {
                            onNavigate(contextMenuUri!!)
                            contextMenuUri = null
                        }
                )
                androidx.compose.material3.DropdownMenuItem(
                        text = {
                            androidx.compose.material3.Text(
                                    "Copy Image Address",
                                    color =
                                            if (isDarkTheme)
                                                    com.oneatom.onebrowser.ui.theme.DarkText
                                            else com.oneatom.onebrowser.ui.theme.LightText
                            )
                        },
                        onClick = {
                            val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as
                                            android.content.ClipboardManager
                            val clip =
                                    android.content.ClipData.newPlainText(
                                            "Image URL",
                                            contextMenuUri
                                    )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT)
                                    .show()
                            contextMenuUri = null
                        }
                )
                androidx.compose.material3.DropdownMenuItem(
                        text = {
                            androidx.compose.material3.Text(
                                    "Share Image",
                                    color =
                                            if (isDarkTheme)
                                                    com.oneatom.onebrowser.ui.theme.DarkText
                                            else com.oneatom.onebrowser.ui.theme.LightText
                            )
                        },
                        onClick = {
                            val intent =
                                    android.content.Intent(android.content.Intent.ACTION_SEND)
                                            .apply {
                                                type = "text/plain"
                                                putExtra(
                                                        android.content.Intent.EXTRA_TEXT,
                                                        contextMenuUri
                                                )
                                            }
                            context.startActivity(
                                    android.content.Intent.createChooser(intent, "Share Image Link")
                            )
                            contextMenuUri = null
                        }
                )
            }
        }
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
