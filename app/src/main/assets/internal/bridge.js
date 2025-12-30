/**
 * One Browser - JavaScript Bridge
 * Communication between WebView and native Kotlin
 */

// Global OneBrowser bridge (injected by native code)
window.OneBrowserBridge = {
    // Navigate to URL
    navigate: function(url) {
        if (window.OneBrowser && window.OneBrowser.navigate) {
            window.OneBrowser.navigate(url);
        }
    },
    
    // Open settings page
    openSettings: function() {
        if (window.OneBrowser && window.OneBrowser.openSettings) {
            window.OneBrowser.openSettings();
        }
    },
    
    // Get current theme
    getTheme: function() {
        if (window.OneBrowser && window.OneBrowser.getTheme) {
            return window.OneBrowser.getTheme();
        }
        return 'dark';
    },
    
    // Log message to native console
    log: function(message) {
        if (window.OneBrowser && window.OneBrowser.log) {
            window.OneBrowser.log(message);
        }
        console.log('[OneBrowser]', message);
    }
};

// Theme change handler
window.onThemeChange = function(theme) {
    document.documentElement.className = theme;
    OneBrowserBridge.log('Theme changed to: ' + theme);
    
    // Dispatch custom event for components to listen
    window.dispatchEvent(new CustomEvent('themechange', { detail: { theme: theme } }));
};

// Initialize theme on page load
document.addEventListener('DOMContentLoaded', function() {
    var theme = OneBrowserBridge.getTheme();
    document.documentElement.className = theme;
    OneBrowserBridge.log('Page loaded with theme: ' + theme);
});

// Utility functions
window.OneBrowserUtils = {
    // Format bytes to human readable
    formatBytes: function(bytes) {
        if (bytes === 0) return '0 B';
        var k = 1024;
        var sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        var i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    },
    
    // Debounce function
    debounce: function(func, wait) {
        var timeout;
        return function() {
            var context = this, args = arguments;
            clearTimeout(timeout);
            timeout = setTimeout(function() {
                func.apply(context, args);
            }, wait);
        };
    },
    
    // Check if string is URL
    isUrl: function(str) {
        var urlPattern = /^(https?:\/\/|www\.)/i;
        var domainPattern = /^[\w-]+(\.[\w-]+)+/;
        return urlPattern.test(str) || (domainPattern.test(str) && !str.includes(' '));
    },
    
    // Build search URL
    buildSearchUrl: function(query, engine) {
        var searchUrls = {
            google: 'https://www.google.com/search?q=',
            bing: 'https://www.bing.com/search?q=',
            duckduckgo: 'https://duckduckgo.com/?q=',
            brave: 'https://search.brave.com/search?q='
        };
        var baseUrl = searchUrls[engine] || searchUrls.google;
        return baseUrl + encodeURIComponent(query);
    }
};
