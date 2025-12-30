package com.oneatom.onebrowser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneatom.onebrowser.data.SearchEngine
import com.oneatom.onebrowser.data.SettingsRepository
import com.oneatom.onebrowser.data.Tab
import com.oneatom.onebrowser.data.ToolbarPosition
import com.oneatom.onebrowser.ui.theme.ThemeMode
import java.net.URLEncoder
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class NavigationAction {
    data class GoBack(val tabId: String) : NavigationAction()
    data class GoForward(val tabId: String) : NavigationAction()
    data class Reload(val tabId: String) : NavigationAction()
    data class Stop(val tabId: String) : NavigationAction()
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    // Navigation Events
    private val _navigationActions = MutableSharedFlow<NavigationAction>()
    val navigationActions: SharedFlow<NavigationAction> = _navigationActions.asSharedFlow()

    // Navigation Commands
    fun goBack() {
        _activeTabId.value?.let { id ->
            viewModelScope.launch { _navigationActions.emit(NavigationAction.GoBack(id)) }
        }
    }

    fun goForward() {
        _activeTabId.value?.let { id ->
            viewModelScope.launch { _navigationActions.emit(NavigationAction.GoForward(id)) }
        }
    }

    fun reload() {
        _activeTabId.value?.let { id ->
            viewModelScope.launch { _navigationActions.emit(NavigationAction.Reload(id)) }
        }
    }

    fun stopLoading() {
        _activeTabId.value?.let { id ->
            viewModelScope.launch { _navigationActions.emit(NavigationAction.Stop(id)) }
        }
    }

    // Tab Management
    private val _tabs = MutableStateFlow<List<Tab>>(listOf(Tab()))
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(_tabs.value.firstOrNull()?.id)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    // Prevent duplicate tab creation
    private var lastNewTabUrl: String? = null
    private var lastNewTabTime: Long = 0

    // Settings
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _searchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    private val _toolbarPosition = MutableStateFlow(ToolbarPosition.BOTTOM)
    val toolbarPosition: StateFlow<ToolbarPosition> = _toolbarPosition.asStateFlow()

    private val _showHomeButton = MutableStateFlow(true)
    val showHomeButton: StateFlow<Boolean> = _showHomeButton.asStateFlow()

    // UI State
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isMenuOpen = MutableStateFlow(false)
    val isMenuOpen: StateFlow<Boolean> = _isMenuOpen.asStateFlow()

    private val _isDownloadsOpen = MutableStateFlow(false)
    val isDownloadsOpen: StateFlow<Boolean> = _isDownloadsOpen.asStateFlow()

    // Search Suggestions
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()
    private var searchJob: Job? = null

    init {
        // Load settings from DataStore
        viewModelScope.launch { settingsRepository.themeModeFlow.collect { _themeMode.value = it } }
        viewModelScope.launch {
            settingsRepository.searchEngineFlow.collect { _searchEngine.value = it }
        }
        viewModelScope.launch {
            settingsRepository.toolbarPositionFlow.collect { _toolbarPosition.value = it }
        }
        viewModelScope.launch {
            settingsRepository.showHomeButtonFlow.collect { _showHomeButton.value = it }
        }
    }

    // Tab operations
    fun addTab(url: String = "", title: String = "New Tab", isIncognito: Boolean = false): String {
        val now = System.currentTimeMillis()

        // Prevent duplicate tabs within 2 seconds
        if (url.isNotEmpty() && url == lastNewTabUrl && (now - lastNewTabTime) < 2000) {
            return _activeTabId.value ?: ""
        }

        lastNewTabUrl = url
        lastNewTabTime = now

        val newTab = Tab(url = url, title = title, isIncognito = isIncognito)

        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id

        return newTab.id
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val tabIndex = currentTabs.indexOfFirst { it.id == tabId }

        if (currentTabs.size == 1) {
            // If closing last tab, create a new empty tab
            val newTab = Tab()
            _tabs.value = listOf(newTab)
            _activeTabId.value = newTab.id
        } else {
            _tabs.value = currentTabs.filter { it.id != tabId }
            deleteThumbnail(tabId)

            // If we closed the active tab, switch to adjacent tab
            if (_activeTabId.value == tabId) {
                val newIndex = (tabIndex - 1).coerceAtLeast(0)
                _activeTabId.value = _tabs.value.getOrNull(newIndex)?.id
            }
        }
    }

    fun setActiveTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    fun updateThumbnail(tabId: String, bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val thumbnailDir = java.io.File(context.filesDir, "thumbnails")
                if (!thumbnailDir.exists()) {
                    thumbnailDir.mkdirs()
                }

                val file = java.io.File(thumbnailDir, "tab_$tabId.jpg")
                val stream = java.io.FileOutputStream(file)

                // Scale down if too big to save space/performance
                // Max dimension 600px is usually enough for a switcher
                val maxDim = 600
                val scale =
                        if (bitmap.width > maxDim || bitmap.height > maxDim) {
                            val ratio =
                                    Math.min(
                                            maxDim.toFloat() / bitmap.width,
                                            maxDim.toFloat() / bitmap.height
                                    )
                            ratio
                        } else {
                            1.0f
                        }

                val saveBitmap =
                        if (scale < 1.0f) {
                            android.graphics.Bitmap.createScaledBitmap(
                                    bitmap,
                                    (bitmap.width * scale).toInt(),
                                    (bitmap.height * scale).toInt(),
                                    true
                            )
                        } else {
                            bitmap
                        }

                saveBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                stream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteThumbnail(tabId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val file = java.io.File(context.filesDir, "thumbnails/tab_$tabId.jpg")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun switchToNextTab() {
        val currentTabs = _tabs.value
        val activeId = _activeTabId.value ?: return
        val currentIndex = currentTabs.indexOfFirst { it.id == activeId }

        if (currentIndex != -1) {
            if (currentIndex < currentTabs.size - 1) {
                _activeTabId.value = currentTabs[currentIndex + 1].id
            } else {
                // Last tab -> Create new tab ONLY if current tab is not already a new empty tab
                val currentTab = currentTabs[currentIndex]
                if (currentTab.url.isNotEmpty()) {
                    addTab()
                }
            }
        }
    }

    fun switchToPreviousTab() {
        val currentTabs = _tabs.value
        val activeId = _activeTabId.value ?: return
        val currentIndex = currentTabs.indexOfFirst { it.id == activeId }

        if (currentIndex > 0) {
            _activeTabId.value = currentTabs[currentIndex - 1].id
        }
    }

    fun updateTab(tabId: String, update: Tab.() -> Tab) {
        _tabs.value = _tabs.value.map { tab -> if (tab.id == tabId) tab.update() else tab }
    }

    fun getActiveTab(): Tab? {
        return _tabs.value.find { it.id == _activeTabId.value }
    }

    // Navigation
    fun navigateTo(input: String) {
        val activeTab = getActiveTab() ?: return
        val url = processInput(input)

        updateTab(activeTab.id) { copy(url = url, isLoading = true, title = "Loading...") }
    }

    private fun processInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // URL patterns
        val urlPattern = Pattern.compile("^(https?://|www\\.)", Pattern.CASE_INSENSITIVE)
        val domainPattern = Pattern.compile("^[\\w-]+(\\.[\\w-]+)+")

        return when {
            urlPattern.matcher(trimmed).find() -> {
                if (!trimmed.startsWith("http")) "https://$trimmed" else trimmed
            }
            domainPattern.matcher(trimmed).find() && !trimmed.contains(" ") -> {
                "https://$trimmed"
            }
            else -> {
                // Use search engine
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                "${_searchEngine.value.searchUrl}$encoded"
            }
        }
    }

    fun goHome() {
        val activeTab = getActiveTab() ?: return
        updateTab(activeTab.id) { copy(url = "", isLoading = false, title = "New Tab") }
    }

    // Settings operations
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun toggleTheme() {
        val newMode =
                when (_themeMode.value) {
                    ThemeMode.DARK -> ThemeMode.LIGHT
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.SYSTEM -> ThemeMode.DARK
                }
        setThemeMode(newMode)
    }

    fun setSearchEngine(engine: SearchEngine) {
        _searchEngine.value = engine
        viewModelScope.launch { settingsRepository.setSearchEngine(engine) }
    }

    fun setToolbarPosition(position: ToolbarPosition) {
        _toolbarPosition.value = position
        viewModelScope.launch { settingsRepository.setToolbarPosition(position) }
    }

    fun setShowHomeButton(show: Boolean) {
        _showHomeButton.value = show
        viewModelScope.launch { settingsRepository.setShowHomeButton(show) }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
        _isMenuOpen.value = false
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun toggleMenu() {
        _isMenuOpen.value = !_isMenuOpen.value
    }

    fun closeMenu() {
        _isMenuOpen.value = false
    }

    // Search Suggestions
    fun fetchSuggestions(query: String) {
        searchJob?.cancel()

        if (query.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }

        if (query.length < 1) { // Allow single letter suggestions
            // Do nothing or clear? If empty handled above, this might be redundant if < 1 means
            // empty.
            // But let's stay safe.
            return
        }

        searchJob =
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val url =
                                "https://suggestqueries.google.com/complete/search?client=firefox&q=${URLEncoder.encode(query, "UTF-8")}"
                        val connection =
                                java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 1000
                        connection.readTimeout = 1000

                        val stream = connection.inputStream
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        // Parse JSON array: ["query", ["suggestion1", "suggestion2", ...]]
                        val jsonString = response.toString()
                        val suggestions = mutableListOf<String>()

                        // Simple regex parsing to avoid adding heavy JSON library for this one task
                        val matcher = Pattern.compile("\"([^\"]+)\"").matcher(jsonString)
                        // Skip the first match which is the query itself
                        if (matcher.find()) {
                            while (matcher.find()) {
                                val suggestion = matcher.group(1)
                                if (suggestion != null && !suggestion.startsWith("http")) {
                                    suggestions.add(suggestion)
                                }
                            }
                        }

                        if (isActive) {
                            _suggestions.value = suggestions.take(5)
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        e.printStackTrace()
                        if (isActive) {
                            _suggestions.value = emptyList()
                        }
                    }
                }
    }

    fun clearSuggestions() {
        searchJob?.cancel()
        _suggestions.value = emptyList()
    }

    fun onOpenDownloads() {
        _isDownloadsOpen.value = true
        _isMenuOpen.value = false
    }

    fun onCloseDownloads() {
        _isDownloadsOpen.value = false
    }
}
