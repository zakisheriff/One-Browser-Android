package com.oneatom.onebrowser.data

import java.util.UUID

data class Tab(
        val id: String = UUID.randomUUID().toString(),
        val url: String = "",
        val title: String = "New Tab",
        val favicon: String? = null,
        val isIncognito: Boolean = false,
        val isLoading: Boolean = false,
        val progress: Int = 0,
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val sslSecure: Boolean = false
)

enum class SearchEngine(val displayName: String, val searchUrl: String, val homeUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q=", "https://www.google.com"),
    BING("Bing", "https://www.bing.com/search?q=", "https://www.bing.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "https://duckduckgo.com"),
    BRAVE("Brave", "https://search.brave.com/search?q=", "https://search.brave.com")
}

enum class ToolbarPosition {
    TOP,
    BOTTOM
}
