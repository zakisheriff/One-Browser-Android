package com.oneatom.onebrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.oneatom.onebrowser.ui.theme.*

@Composable
fun BrowserMenu(
        isOpen: Boolean,
        isDarkTheme: Boolean,
        onDismiss: () -> Unit,
        onNewTab: () -> Unit,
        onNewIncognitoTab: () -> Unit,
        onOpenDownloads: () -> Unit,
        onOpenSettings: () -> Unit,
        onOpenAbout: () -> Unit,
        modifier: Modifier = Modifier
) {
        if (!isOpen) return

        val backgroundColor =
                if (isDarkTheme) {
                        DarkBackground.copy(alpha = 0.95f)
                } else {
                        LightBackground.copy(alpha = 0.95f)
                }
        val borderColor = if (isDarkTheme) DarkBorder else LightBorder
        val textColor = if (isDarkTheme) DarkText else LightText
        val mutedColor = if (isDarkTheme) DarkMuted else LightMuted

        Popup(onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
                Box(modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss)) {
                        Column(
                                modifier =
                                        modifier.align(Alignment.BottomEnd)
                                                .padding(end = 16.dp, bottom = 80.dp)
                                                .width(220.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(backgroundColor)
                                                .border(
                                                        1.dp,
                                                        borderColor,
                                                        RoundedCornerShape(20.dp)
                                                )
                                                .padding(vertical = 8.dp)
                        ) {
                                MenuItem(
                                        icon = Icons.Default.Add,
                                        text = "New Tab",
                                        textColor = textColor,
                                        mutedColor = mutedColor,
                                        isDarkTheme = isDarkTheme,
                                        onClick = {
                                                onNewTab()
                                                onDismiss()
                                        }
                                )

                                MenuItem(
                                        icon = Icons.Outlined.VisibilityOff,
                                        text = "New Incognito Tab",
                                        textColor = textColor,
                                        mutedColor = mutedColor,
                                        isDarkTheme = isDarkTheme,
                                        onClick = {
                                                onNewIncognitoTab()
                                                onDismiss()
                                        }
                                )

                                Divider(
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 8.dp
                                                ),
                                        color = borderColor
                                )

                                MenuItem(
                                        icon = Icons.Default.Download,
                                        text = "Downloads",
                                        textColor = textColor,
                                        mutedColor = mutedColor,
                                        isDarkTheme = isDarkTheme,
                                        onClick = {
                                                onOpenDownloads()
                                                onDismiss()
                                        }
                                )

                                MenuItem(
                                        icon = Icons.Default.Settings,
                                        text = "Settings",
                                        textColor = textColor,
                                        mutedColor = mutedColor,
                                        isDarkTheme = isDarkTheme,
                                        onClick = {
                                                onOpenSettings()
                                                onDismiss()
                                        }
                                )

                                MenuItem(
                                        icon = Icons.Default.Info,
                                        text = "About One Browser",
                                        textColor = textColor,
                                        mutedColor = mutedColor,
                                        isDarkTheme = isDarkTheme,
                                        onClick = {
                                                onOpenAbout()
                                                onDismiss()
                                        }
                                )
                        }
                }
        }
}

@Composable
private fun MenuItem(
        icon: ImageVector,
        text: String,
        textColor: androidx.compose.ui.graphics.Color,
        mutedColor: androidx.compose.ui.graphics.Color,
        isDarkTheme: Boolean,
        onClick: () -> Unit
) {
        val hoverColor = if (isDarkTheme) DarkHover else LightHover

        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable(onClick = onClick)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = mutedColor,
                        modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(text = text, color = textColor, fontSize = 14.sp)
        }
}
