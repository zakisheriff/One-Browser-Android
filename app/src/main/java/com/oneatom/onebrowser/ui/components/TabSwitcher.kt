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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneatom.onebrowser.data.Tab
import com.oneatom.onebrowser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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
                                Text(
                                        text = "${tabs.size} tabs",
                                        color = textColor,
                                        fontSize = 18.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // New tab button
                                        IconButton(
                                                onClick = onNewTab,
                                                modifier =
                                                        Modifier.size(44.dp)
                                                                .clip(CircleShape)
                                                                .background(cardBgColor)
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
                                                        Modifier.size(44.dp)
                                                                .clip(CircleShape)
                                                                .background(cardBgColor)
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

                                        val dismissState =
                                                rememberDismissState(
                                                        confirmValueChange = {
                                                                if (it != DismissValue.Default) {
                                                                        onTabClose(tab.id)
                                                                        true
                                                                } else false
                                                        }
                                                )

                                        SwipeToDismiss(
                                                state = dismissState,
                                                background = {
                                                        val color = Color.Red
                                                        val alignment =
                                                                if (dismissState.dismissDirection ==
                                                                                DismissDirection
                                                                                        .StartToEnd
                                                                )
                                                                        Alignment.CenterStart
                                                                else Alignment.CenterEnd

                                                        Box(
                                                                modifier =
                                                                        Modifier.fillMaxSize()
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                16.dp
                                                                                        )
                                                                                )
                                                                                .background(color)
                                                                                .padding(
                                                                                        horizontal =
                                                                                                16.dp
                                                                                ),
                                                                contentAlignment = alignment
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default
                                                                                        .Delete,
                                                                        contentDescription =
                                                                                "Delete",
                                                                        tint = Color.White
                                                                )
                                                        }
                                                },
                                                dismissContent = {
                                                        Card(
                                                                modifier =
                                                                        Modifier.aspectRatio(0.7f)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                16.dp
                                                                                        )
                                                                                )
                                                                                .border(
                                                                                        width =
                                                                                                if (isActive
                                                                                                )
                                                                                                        2.dp
                                                                                                else
                                                                                                        1.dp,
                                                                                        color =
                                                                                                if (isActive
                                                                                                )
                                                                                                        Color.Blue
                                                                                                else
                                                                                                        borderColor,
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        16.dp
                                                                                                )
                                                                                )
                                                                                .clickable {
                                                                                        onTabClick(
                                                                                                tab.id
                                                                                        )
                                                                                },
                                                                colors =
                                                                        CardDefaults.cardColors(
                                                                                containerColor =
                                                                                        cardBgColor
                                                                        ),
                                                                shape = RoundedCornerShape(16.dp)
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.fillMaxSize()
                                                                ) {
                                                                        // Header: Logo | Title |
                                                                        // Close
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .height(
                                                                                                        40.dp
                                                                                                )
                                                                                                .padding(
                                                                                                        horizontal =
                                                                                                                8.dp
                                                                                                ),
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically,
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween
                                                                        ) {
                                                                                Row(
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                ),
                                                                                        verticalAlignment =
                                                                                                Alignment
                                                                                                        .CenterVertically
                                                                                ) {
                                                                                        Box(
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                        16.dp
                                                                                                                )
                                                                                                                .background(
                                                                                                                        Color.Gray,
                                                                                                                        CircleShape
                                                                                                                ),
                                                                                                contentAlignment =
                                                                                                        Alignment
                                                                                                                .Center
                                                                                        ) {
                                                                                                Text(
                                                                                                        text =
                                                                                                                tab.title
                                                                                                                        .take(
                                                                                                                                1
                                                                                                                        )
                                                                                                                        .uppercase(),
                                                                                                        fontSize =
                                                                                                                10.sp,
                                                                                                        color =
                                                                                                                Color.White
                                                                                                )
                                                                                        }

                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.width(
                                                                                                                8.dp
                                                                                                        )
                                                                                        )

                                                                                        Text(
                                                                                                text =
                                                                                                        tab.title
                                                                                                                .ifEmpty {
                                                                                                                        "New Tab"
                                                                                                                },
                                                                                                fontSize =
                                                                                                        12.sp,
                                                                                                color =
                                                                                                        textColor,
                                                                                                maxLines =
                                                                                                        1,
                                                                                                overflow =
                                                                                                        TextOverflow
                                                                                                                .Ellipsis
                                                                                        )
                                                                                }

                                                                                IconButton(
                                                                                        onClick = {
                                                                                                onTabClose(
                                                                                                        tab.id
                                                                                                )
                                                                                        },
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        24.dp
                                                                                                )
                                                                                ) {
                                                                                        Icon(
                                                                                                imageVector =
                                                                                                        Icons.Default
                                                                                                                .Close,
                                                                                                contentDescription =
                                                                                                        "Close",
                                                                                                tint =
                                                                                                        textColor,
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                16.dp
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }

                                                                        // Thumbnail Body
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .fillMaxWidth()
                                                                                                .background(
                                                                                                        if (isDarkTheme
                                                                                                        )
                                                                                                                Color.Black
                                                                                                        else
                                                                                                                Color.White
                                                                                                )
                                                                        ) {
                                                                                val context =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .platform
                                                                                                .LocalContext
                                                                                                .current
                                                                                val imageFile =
                                                                                        java.io
                                                                                                .File(
                                                                                                        context.filesDir,
                                                                                                        "thumbnails/tab_${tab.id}.jpg"
                                                                                                )

                                                                                if (imageFile
                                                                                                .exists()
                                                                                ) {
                                                                                        coil.compose
                                                                                                .AsyncImage(
                                                                                                        model =
                                                                                                                coil.request
                                                                                                                        .ImageRequest
                                                                                                                        .Builder(
                                                                                                                                context
                                                                                                                        )
                                                                                                                        .data(
                                                                                                                                imageFile
                                                                                                                        )
                                                                                                                        .crossfade(
                                                                                                                                true
                                                                                                                        )
                                                                                                                        .build(),
                                                                                                        contentDescription =
                                                                                                                "Preview",
                                                                                                        contentScale =
                                                                                                                androidx.compose
                                                                                                                        .ui
                                                                                                                        .layout
                                                                                                                        .ContentScale
                                                                                                                        .Crop,
                                                                                                        modifier =
                                                                                                                Modifier.fillMaxSize(),
                                                                                                        alignment =
                                                                                                                Alignment
                                                                                                                        .TopCenter
                                                                                                )
                                                                                } else {
                                                                                        Box(
                                                                                                modifier =
                                                                                                        Modifier.fillMaxSize(),
                                                                                                contentAlignment =
                                                                                                        Alignment
                                                                                                                .Center
                                                                                        ) {
                                                                                                Icon(
                                                                                                        imageVector =
                                                                                                                Icons.Default
                                                                                                                        .Public,
                                                                                                        contentDescription =
                                                                                                                null,
                                                                                                        tint =
                                                                                                                mutedColor
                                                                                                                        .copy(
                                                                                                                                alpha =
                                                                                                                                        0.5f
                                                                                                                        ),
                                                                                                        modifier =
                                                                                                                Modifier.size(
                                                                                                                        48.dp
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        )
                                }
                        }
                }
        }
}
