package com.oneatom.onebrowser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneatom.onebrowser.ui.theme.*

@Composable
fun BottomToolbar(
        url: String,
        isLoading: Boolean,
        canGoBack: Boolean,
        tabCount: Int,
        isDarkTheme: Boolean,
        onSearchFocusChange: (Boolean) -> Unit,
        onGoBack: () -> Unit,
        onReload: () -> Unit,
        onStop: () -> Unit,
        onOpenTabs: () -> Unit,
        onOpenMenu: () -> Unit,
        modifier: Modifier = Modifier
) {
        val backgroundColor = if (isDarkTheme) DarkInputBackground else LightInputBackground
        val borderColor = if (isDarkTheme) DarkBorder else LightBorder
        val textColor = if (isDarkTheme) DarkText else LightText
        val mutedColor = if (isDarkTheme) DarkMuted else LightMuted

        // Extract just the hostname for display
        val displayUrl =
                remember(url) {
                        try {
                                val cleanUrl =
                                        url.removePrefix("https://")
                                                .removePrefix("http://")
                                                .removePrefix("www.")
                                cleanUrl.substringBefore("/").ifEmpty { "Search or enter URL" }
                        } catch (e: Exception) {
                                "Search or enter URL"
                        }
                }

        Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // Loading Indicator at the top of toolbar
                if (isLoading) {
                        LinearProgressIndicator(
                                modifier =
                                        Modifier.fillMaxWidth().height(2.dp).align(Alignment.Top),
                                color = if (isDarkTheme) Color.White else Color.Black,
                                trackColor = Color.Transparent
                        )
                }

                // Back button - circular
                CircleIconButton(
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        enabled = canGoBack,
                        isDarkTheme = isDarkTheme,
                        onClick = onGoBack
                )

                // Tabs button - square with count
                Box(
                        modifier =
                                Modifier.size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(backgroundColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .clickable { onOpenTabs() },
                        contentAlignment = Alignment.Center
                ) {
                        Text(
                                text = tabCount.toString(),
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                        )
                }

                // URL bar - pill shape, takes remaining space
                Row(
                        modifier =
                                Modifier.weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(backgroundColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(22.dp))
                                        .clickable { onSearchFocusChange(true) }
                                        .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                ) {
                        Text(
                                text = displayUrl,
                                color = if (url.isEmpty()) mutedColor else textColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                        )
                }

                // Reload/Stop button - circular
                if (isLoading) {
                        CircleIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Stop",
                                enabled = true,
                                isDarkTheme = isDarkTheme,
                                onClick = onStop
                        )
                } else {
                        CircleIconButton(
                                icon = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                enabled = true,
                                isDarkTheme = isDarkTheme,
                                onClick = onReload
                        )
                }

                // Menu button - circular with 3 dots
                CircleIconButton(
                        icon = Icons.Default.MoreHoriz,
                        contentDescription = "Menu",
                        enabled = true,
                        isDarkTheme = isDarkTheme,
                        onClick = onOpenMenu
                )
        }
}

@Composable
private fun CircleIconButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String,
        enabled: Boolean,
        isDarkTheme: Boolean,
        onClick: () -> Unit
) {
        val backgroundColor = if (isDarkTheme) DarkInputBackground else LightInputBackground
        val borderColor = if (isDarkTheme) DarkBorder else LightBorder
        val iconColor = if (isDarkTheme) DarkText else LightText
        val disabledColor = if (isDarkTheme) DarkMuted else LightMuted

        Box(
                modifier =
                        Modifier.size(44.dp)
                                .clip(CircleShape)
                                .background(backgroundColor)
                                .border(1.dp, borderColor, CircleShape)
                                .clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint =
                                if (enabled) iconColor.copy(alpha = 0.8f)
                                else disabledColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                )
        }
}

// Full screen search overlay - appears when URL bar is tapped
@Composable
fun SearchOverlay(
        currentUrl: String,
        isDarkTheme: Boolean,
        suggestions: List<String>,
        onQueryChange: (String) -> Unit,
        onNavigate: (String) -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }

        // Use TextFieldValue to handle selection
        var textFieldValue by remember {
                mutableStateOf(
                        TextFieldValue(
                                text = currentUrl,
                                selection = TextRange(0, currentUrl.length)
                        )
                )
        }

        val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
        val inputBgColor = if (isDarkTheme) DarkInputBackground else LightInputBackground
        val borderColor = if (isDarkTheme) DarkBorder else LightBorder
        val textColor = if (isDarkTheme) DarkText else LightText
        val mutedColor = if (isDarkTheme) DarkMuted else LightMuted

        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Box(modifier = modifier.fillMaxSize().background(backgroundColor).imePadding()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        // Search bar at TOP
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(56.dp)
                                                .clip(RoundedCornerShape(28.dp))
                                                .background(inputBgColor)
                                                .border(
                                                        1.dp,
                                                        borderColor,
                                                        RoundedCornerShape(28.dp)
                                                )
                                                .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Back button
                                IconButton(
                                        onClick = {
                                                focusManager.clearFocus()
                                                onDismiss()
                                        },
                                        modifier = Modifier.size(44.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Cancel",
                                                tint = textColor,
                                                modifier = Modifier.size(22.dp)
                                        )
                                }

                                // Text input
                                BasicTextField(
                                        value = textFieldValue,
                                        onValueChange = {
                                                textFieldValue = it
                                                onQueryChange(it.text)
                                        },
                                        modifier =
                                                Modifier.weight(1f).focusRequester(focusRequester),
                                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                                        singleLine = true,
                                        cursorBrush = SolidColor(textColor),
                                        keyboardOptions =
                                                KeyboardOptions(
                                                        keyboardType = KeyboardType.Uri,
                                                        imeAction = ImeAction.Go
                                                ),
                                        keyboardActions =
                                                KeyboardActions(
                                                        onGo = {
                                                                onNavigate(textFieldValue.text)
                                                                focusManager.clearFocus()
                                                                onDismiss()
                                                        }
                                                ),
                                        decorationBox = { innerTextField ->
                                                Box(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentAlignment = Alignment.CenterStart
                                                ) {
                                                        if (textFieldValue.text.isEmpty()) {
                                                                Text(
                                                                        text =
                                                                                "Search or enter URL…",
                                                                        color = mutedColor,
                                                                        fontSize = 16.sp
                                                                )
                                                        }
                                                        innerTextField()
                                                }
                                        }
                                )

                                // Clear button
                                if (textFieldValue.text.isNotEmpty()) {
                                        IconButton(
                                                onClick = {
                                                        textFieldValue =
                                                                TextFieldValue("", TextRange.Zero)
                                                        onQueryChange("")
                                                },
                                                modifier = Modifier.size(44.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Clear",
                                                        tint = mutedColor,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                        }
                                }
                        }
                }

                // Suggestions List
                if (suggestions.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(suggestions) { suggestion ->
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clickable {
                                                                        onNavigate(suggestion)
                                                                        focusManager.clearFocus()
                                                                        onDismiss()
                                                                }
                                                                .padding(
                                                                        vertical = 12.dp,
                                                                        horizontal = 16.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = mutedColor,
                                                        modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                        text = suggestion,
                                                        color = textColor,
                                                        fontSize = 16.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                        }
                                        Divider(color = borderColor.copy(alpha = 0.5f))
                                }
                        }
                }
        }
}
