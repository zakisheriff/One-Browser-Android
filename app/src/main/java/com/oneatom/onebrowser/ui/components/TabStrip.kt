package com.oneatom.onebrowser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.oneatom.onebrowser.data.Tab
import com.oneatom.onebrowser.ui.theme.*

@Composable
fun TabStrip(
        tabs: List<Tab>,
        activeTabId: String?,
        isDarkTheme: Boolean,
        onTabClick: (String) -> Unit,
        onTabClose: (String) -> Unit,
        onNewTab: () -> Unit,
        modifier: Modifier = Modifier
) {
        val listState = rememberLazyListState()

        val backgroundColor = if (isDarkTheme) DarkInputBackground else LightInputBackground
        val textColor = if (isDarkTheme) DarkText else LightText

        Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Tab list
                LazyRow(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                        items(tabs, key = { it.id }) { tab ->
                                val isActive = tab.id == activeTabId

                                TabItem(
                                        tab = tab,
                                        isActive = isActive,
                                        isDarkTheme = isDarkTheme,
                                        onClick = { onTabClick(tab.id) },
                                        onClose = { onTabClose(tab.id) }
                                )
                        }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // New tab button
                IconButton(
                        onClick = onNewTab,
                        modifier =
                                Modifier.size(36.dp).clip(CircleShape).background(backgroundColor)
                ) {
                        Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Tab",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                        )
                }
        }
}

@Composable
private fun TabItem(
        tab: Tab,
        isActive: Boolean,
        isDarkTheme: Boolean,
        onClick: () -> Unit,
        onClose: () -> Unit
) {
        val backgroundColor =
                if (isDarkTheme) {
                        if (isActive) GlassColors.DarkGlassBackground else Color.Transparent
                } else {
                        if (isActive) GlassColors.LightGlassBackground else Color.Transparent
                }

        val borderColor =
                if (isDarkTheme) {
                        if (isActive) GlassColors.DarkGlassBorder else Color.Transparent
                } else {
                        if (isActive) GlassColors.LightGlassBorder else Color.Transparent
                }

        val textColor = if (isDarkTheme) DarkText else LightText
        val mutedColor = if (isDarkTheme) DarkMuted else LightMuted

        val animatedBackgroundColor by
                animateColorAsState(targetValue = backgroundColor, label = "TabBackground")

        val animatedBorderColor by
                animateColorAsState(targetValue = borderColor, label = "TabBorder")

        Row(
                modifier =
                        Modifier.widthIn(min = 120.dp, max = 200.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(animatedBackgroundColor)
                                .border(1.dp, animatedBorderColor, RoundedCornerShape(18.dp))
                                .clickable { onClick() }
                                .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                // Incognito indicator
                if (tab.isIncognito) {
                        Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = "Incognito",
                                tint = mutedColor,
                                modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                } else if (tab.favicon != null) {
                        // Favicon
                        AsyncImage(
                                model = tab.favicon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                }

                // Title
                Text(
                        text = tab.title.ifEmpty { "New Tab" },
                        color = if (isActive) textColor else mutedColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                )

                // Close button
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Tab",
                                tint = mutedColor,
                                modifier = Modifier.size(14.dp)
                        )
                }
        }
}
