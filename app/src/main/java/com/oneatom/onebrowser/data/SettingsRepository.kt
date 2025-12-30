package com.oneatom.onebrowser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oneatom.onebrowser.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        private val TOOLBAR_POSITION = stringPreferencesKey("toolbar_position")
        private val SHOW_HOME_BUTTON = stringPreferencesKey("show_home_button")
    }

    // Theme Mode
    val themeModeFlow: Flow<ThemeMode> =
            context.dataStore.data.map { preferences ->
                when (preferences[THEME_MODE]) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    "system" -> ThemeMode.SYSTEM
                    else -> ThemeMode.DARK // Default to dark
                }
            }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] =
                    when (mode) {
                        ThemeMode.LIGHT -> "light"
                        ThemeMode.DARK -> "dark"
                        ThemeMode.SYSTEM -> "system"
                    }
        }
    }

    // Search Engine
    val searchEngineFlow: Flow<SearchEngine> =
            context.dataStore.data.map { preferences ->
                when (preferences[SEARCH_ENGINE]) {
                    "google" -> SearchEngine.GOOGLE
                    "bing" -> SearchEngine.BING
                    "duckduckgo" -> SearchEngine.DUCKDUCKGO
                    "brave" -> SearchEngine.BRAVE
                    else -> SearchEngine.GOOGLE // Default
                }
            }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_ENGINE] =
                    when (engine) {
                        SearchEngine.GOOGLE -> "google"
                        SearchEngine.BING -> "bing"
                        SearchEngine.DUCKDUCKGO -> "duckduckgo"
                        SearchEngine.BRAVE -> "brave"
                    }
        }
    }

    // Toolbar Position
    val toolbarPositionFlow: Flow<ToolbarPosition> =
            context.dataStore.data.map { preferences ->
                when (preferences[TOOLBAR_POSITION]) {
                    "top" -> ToolbarPosition.TOP
                    "bottom" -> ToolbarPosition.BOTTOM
                    else -> ToolbarPosition.BOTTOM // Default to bottom
                }
            }

    suspend fun setToolbarPosition(position: ToolbarPosition) {
        context.dataStore.edit { preferences ->
            preferences[TOOLBAR_POSITION] =
                    when (position) {
                        ToolbarPosition.TOP -> "top"
                        ToolbarPosition.BOTTOM -> "bottom"
                    }
        }
    }

    // Show Home Button
    val showHomeButtonFlow: Flow<Boolean> =
            context.dataStore.data.map { preferences ->
                preferences[SHOW_HOME_BUTTON] != "false" // Default true
            }

    suspend fun setShowHomeButton(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_HOME_BUTTON] = if (show) "true" else "false"
        }
    }
}
