package com.oneatom.onebrowser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneatom.onebrowser.data.Tab
import com.oneatom.onebrowser.data.ToolbarPosition
import com.oneatom.onebrowser.ui.components.*
import com.oneatom.onebrowser.ui.theme.*

@Composable
fun BrowserScreen(
        tabs: List<Tab>,
        activeTabId: String?,
        toolbarPosition: ToolbarPosition,
        showHomeButton: Boolean,
        isDarkTheme: Boolean,
        isMenuOpen: Boolean,
        isDownloadsOpen: Boolean,
        onTabClick: (String) -> Unit,
        onTabClose: (String) -> Unit,
        onNewTab: () -> Unit,
        onNewIncognitoTab: () -> Unit,
        onNavigate: (String) -> Unit,
        onGoBack: () -> Unit,
        onGoForward: () -> Unit,
        onReload: () -> Unit,
        onStop: () -> Unit,
        onGoHome: () -> Unit,
        onToggleTheme: () -> Unit,
        onOpenMenu: () -> Unit,
        onCloseMenu: () -> Unit,
        onOpenDownloads: () -> Unit,
        onCloseDownloads: () -> Unit,
        onOpenSettings: () -> Unit,
        onOpenAbout: () -> Unit,
        suggestions: List<String>,
        onQueryChange: (String) -> Unit,
        onUpdateTab: (String, Tab.() -> Tab) -> Unit,
        modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
    val activeTab = tabs.find { it.id == activeTabId }

    // Track if search overlay is open
    var isSearchFocused by remember { mutableStateOf(false) }
    // Track if tab switcher is open
    var isTabSwitcherOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        // Main content
        Column(modifier = Modifier.fillMaxSize()) {
            // WebView content - takes most of the space
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                tabs.forEach { tab ->
                    if (tab.id == activeTabId) {
                        key(tab.id) {
                            WebViewContainer(
                                    tab = tab,
                                    isDarkTheme = isDarkTheme,
                                    onTitleChanged = { title ->
                                        onUpdateTab(tab.id) { copy(title = title) }
                                    },
                                    onUrlChanged = { url ->
                                        onUpdateTab(tab.id) { copy(url = url) }
                                    },
                                    onProgressChanged = { progress ->
                                        onUpdateTab(tab.id) { copy(progress = progress) }
                                    },
                                    onLoadingChanged = { loading ->
                                        onUpdateTab(tab.id) { copy(isLoading = loading) }
                                    },
                                    onCanGoBackChanged = { canGoBack ->
                                        onUpdateTab(tab.id) { copy(canGoBack = canGoBack) }
                                    },
                                    onCanGoForwardChanged = { canGoForward ->
                                        onUpdateTab(tab.id) { copy(canGoForward = canGoForward) }
                                    },
                                    onSslSecureChanged = { secure ->
                                        onUpdateTab(tab.id) { copy(sslSecure = secure) }
                                    },
                                    onNavigate = onNavigate,
                                    onOpenSettings = onOpenSettings,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bottom toolbar - minimal design
            AnimatedVisibility(
                    visible = !isSearchFocused && !isTabSwitcherOpen,
                    enter =
                            slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(200)),
                    exit =
                            slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(150))
            ) {
                BottomToolbar(
                        url = activeTab?.url ?: "",
                        isLoading = activeTab?.isLoading ?: false,
                        canGoBack = activeTab?.canGoBack ?: false,
                        tabCount = tabs.size,
                        isDarkTheme = isDarkTheme,
                        onSearchFocusChange = { isSearchFocused = it },
                        onGoBack = onGoBack,
                        onReload = onReload,
                        onStop = onStop,
                        onOpenTabs = { isTabSwitcherOpen = true },
                        onOpenMenu = onOpenMenu
                )
            }
        }

        // Search overlay - slides up from bottom
        AnimatedVisibility(
                visible = isSearchFocused,
                enter =
                        slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200)),
                exit =
                        slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(150))
        ) {
            SearchOverlay(
                    currentUrl = activeTab?.url ?: "",
                    isDarkTheme = isDarkTheme,
                    suggestions = suggestions,
                    onQueryChange = onQueryChange,
                    onNavigate = { url ->
                        onNavigate(url)
                        isSearchFocused = false
                        onQueryChange("") // Clear suggestions
                    },
                    onDismiss = {
                        isSearchFocused = false
                        onQueryChange("") // Clear suggestions
                    }
            )
        }

        // Tab switcher overlay
        AnimatedVisibility(
                visible = isTabSwitcherOpen,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150))
        ) {
            TabSwitcher(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    isDarkTheme = isDarkTheme,
                    onTabClick = { tabId ->
                        onTabClick(tabId)
                        isTabSwitcherOpen = false
                    },
                    onTabClose = onTabClose,
                    onNewTab = {
                        onNewTab()
                        isTabSwitcherOpen = false
                    },
                    onDismiss = { isTabSwitcherOpen = false }
            )
        }

        // Downloads Screen
        AnimatedVisibility(
                visible = isDownloadsOpen,
                enter =
                        slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(),
                exit =
                        slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut()
        ) { DownloadsScreen(isDarkTheme = isDarkTheme, onBack = onCloseDownloads) }

        // Menu popup
        BrowserMenu(
                isOpen = isMenuOpen,
                isDarkTheme = isDarkTheme,
                onDismiss = onCloseMenu,
                onNewTab = onNewTab,
                onNewIncognitoTab = onNewIncognitoTab,
                onOpenDownloads = onOpenDownloads,
                onOpenSettings = onOpenSettings,
                onOpenAbout = onOpenAbout
        )
    }
}
