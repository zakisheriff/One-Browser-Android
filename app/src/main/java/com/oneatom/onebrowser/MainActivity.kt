package com.oneatom.onebrowser

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneatom.onebrowser.ui.screens.BrowserScreen
import com.oneatom.onebrowser.ui.theme.OneBrowserTheme
import com.oneatom.onebrowser.ui.theme.isDarkTheme
import com.oneatom.onebrowser.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Allow screenshots in secure mode (optional)
        window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        setContent {
            val viewModel: BrowserViewModel = viewModel()

            // Collect state
            val tabs by viewModel.tabs.collectAsState()
            val activeTabId by viewModel.activeTabId.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val toolbarPosition by viewModel.toolbarPosition.collectAsState()
            val showHomeButton by viewModel.showHomeButton.collectAsState()
            val isMenuOpen by viewModel.isMenuOpen.collectAsState()
            val isDownloadsOpen by viewModel.isDownloadsOpen.collectAsState()
            val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
            val suggestions by viewModel.suggestions.collectAsState()
            val navigationActions = viewModel.navigationActions

            val isDark = isDarkTheme(themeMode)

            // Handle back button
            val activeTab = tabs.find { it.id == activeTabId }
            val canGoBack = activeTab?.canGoBack == true

            androidx.activity.compose.BackHandler(enabled = true) {
                if (canGoBack) {
                    viewModel.goBack()
                } else {
                    finish()
                }
            }

            OneBrowserTheme(themeMode = themeMode) {
                BrowserScreen(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        toolbarPosition = toolbarPosition,
                        showHomeButton = showHomeButton,
                        isDarkTheme = isDark,
                        isMenuOpen = isMenuOpen,
                        isDownloadsOpen = isDownloadsOpen,
                        onTabClick = viewModel::setActiveTab,
                        onTabClose = viewModel::closeTab,
                        onNewTab = { viewModel.addTab() },
                        onNewIncognitoTab = { viewModel.addTab(isIncognito = true) },
                        suggestions = suggestions,
                        onQueryChange = viewModel::fetchSuggestions,
                        onNavigate = viewModel::navigateTo,
                        onGoBack = viewModel::goBack,
                        onGoForward = viewModel::goForward,
                        onReload = viewModel::reload,
                        onStop = viewModel::stopLoading,
                        onGoHome = viewModel::goHome,
                        onToggleTheme = viewModel::toggleTheme,
                        onOpenMenu = viewModel::toggleMenu,
                        onCloseMenu = viewModel::closeMenu,
                        onOpenDownloads = viewModel::onOpenDownloads,
                        onCloseDownloads = viewModel::onCloseDownloads,
                        onOpenSettings = viewModel::openSettings,
                        onOpenAbout = { /* TODO: Show about dialog */},
                        onSwipeNext = viewModel::switchToNextTab,
                        onSwipePrevious = viewModel::switchToPreviousTab,
                        onUpdateTab = viewModel::updateTab,
                        onCaptureThumbnail = viewModel::updateThumbnail,
                        navigationActions = navigationActions,
                        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()
                )
            }
        }
    }
}
