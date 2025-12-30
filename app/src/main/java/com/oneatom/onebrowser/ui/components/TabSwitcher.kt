package com.oneatom.onebrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneatom.onebrowser.data.Tab
import com.oneatom.onebrowser.ui.theme.*

@Composable
fun TabSwitcher(
        tabs: List<Tab>,
        activeTabId: String?,
        isDarkTheme: Boolean,
        onTabClick: (String) -> Unit,
        onTabClose: (String) -> Unit,
        onNewTab: () -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
    val cardBgColor = if (isDarkTheme) DarkInputBackground else LightInputBackground
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder
    val textColor = if (isDarkTheme) DarkText else LightText
    val mutedColor = if (isDarkTheme) DarkMuted else LightMuted

    Box(
            modifier =
                    modifier.fillMaxSize()
                            .background(backgroundColor)
                            .clickable(onClick = onDismiss)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${tabs.size} tabs", color = textColor, fontSize = 18.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // New tab button
                    IconButton(
                            onClick = onNewTab,
                            modifier =
                                    Modifier.size(44.dp).clip(CircleShape).background(cardBgColor)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Tab",
                                tint = textColor
                        )
                    }

                    // Close button
                    IconButton(
                            onClick = onDismiss,
                            modifier =
                                    Modifier.size(44.dp).clip(CircleShape).background(cardBgColor)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textColor
                        )
                    }
                }
            }

            // Tab grid
            LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId

                    Box(
                            modifier =
                                    Modifier.aspectRatio(0.7f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardBgColor)
                                            .border(
                                                    width = if (isActive) 2.dp else 1.dp,
                                                    color =
                                                            if (isActive) textColor
                                                            else borderColor,
                                                    shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { onTabClick(tab.id) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Tab preview area
                            Box(
                                    modifier =
                                            Modifier.weight(1f)
                                                    .fillMaxWidth()
                                                    .background(
                                                            if (isDarkTheme) DarkHover
                                                            else LightHover
                                                    ),
                                    contentAlignment = Alignment.Center
                            ) {
                                // Placeholder for tab preview
                                Text(
                                        text = tab.title.take(1).uppercase().ifEmpty { "N" },
                                        color = mutedColor,
                                        fontSize = 32.sp
                                )
                            }

                            // Tab info
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                        text = tab.title.ifEmpty { "New Tab" },
                                        color = textColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                )

                                // Close tab button
                                IconButton(
                                        onClick = { onTabClose(tab.id) },
                                        modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            tint = mutedColor,
                                            modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
