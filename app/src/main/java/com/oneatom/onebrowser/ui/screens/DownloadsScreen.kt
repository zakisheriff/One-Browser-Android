package com.oneatom.onebrowser.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneatom.onebrowser.data.DownloadStatus
import com.oneatom.onebrowser.data.DownloadTracker
import com.oneatom.onebrowser.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(isDarkTheme: Boolean, onBack: () -> Unit) {
        val context = LocalContext.current
        // Observe real-time downloads
        val downloads by DownloadTracker.downloads.collectAsState()

        // Ensure we are tracking when this screen is open
        LaunchedEffect(Unit) { launch(Dispatchers.IO) { DownloadTracker.startTracking(context) } }

        val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
        val textColor = if (isDarkTheme) DarkText else LightText

        Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
                // Toolbar
                TopAppBar(
                        title = { Text("Downloads", color = textColor) },
                        navigationIcon = {
                                IconButton(onClick = onBack) {
                                        Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Back",
                                                tint = textColor
                                        )
                                }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )

                if (downloads.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No downloads yet", color = textColor)
                        }
                } else {
                        LazyColumn {
                                items(downloads) { item ->
                                        DownloadItemView(
                                                item = item,
                                                isDarkTheme = isDarkTheme,
                                                onClick = {
                                                        if (item.status ==
                                                                        DownloadManager
                                                                                .STATUS_SUCCESSFUL
                                                        ) {
                                                                try {
                                                                        val intent =
                                                                                Intent(
                                                                                        Intent.ACTION_VIEW
                                                                                )
                                                                        // Handle file URI opening
                                                                        val fileUri =
                                                                                try {
                                                                                        if (item.id >
                                                                                                        0
                                                                                        ) {
                                                                                                (context.getSystemService(
                                                                                                                Context.DOWNLOAD_SERVICE
                                                                                                        ) as
                                                                                                                DownloadManager)
                                                                                                        .getUriForDownloadedFile(
                                                                                                                item.id
                                                                                                        )
                                                                                        } else {
                                                                                                if (item.uri !=
                                                                                                                null
                                                                                                )
                                                                                                        Uri.parse(
                                                                                                                item.uri
                                                                                                        )
                                                                                                else
                                                                                                        null
                                                                                        }
                                                                                } catch (
                                                                                        e:
                                                                                                Exception) {
                                                                                        if (item.uri !=
                                                                                                        null
                                                                                        )
                                                                                                Uri.parse(
                                                                                                        item.uri
                                                                                                )
                                                                                        else null
                                                                                }

                                                                        if (fileUri != null) {
                                                                                intent.setDataAndType(
                                                                                        fileUri,
                                                                                        item.mediaType
                                                                                )
                                                                                intent.addFlags(
                                                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                                )
                                                                                context.startActivity(
                                                                                        intent
                                                                                )
                                                                        } else {
                                                                                Toast.makeText(
                                                                                                context,
                                                                                                "File not found",
                                                                                                Toast.LENGTH_SHORT
                                                                                        )
                                                                                        .show()
                                                                        }
                                                                } catch (e: Exception) {
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Cannot open file",
                                                                                        Toast.LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        }
                                                }
                                        )
                                        Divider(
                                                color = if (isDarkTheme) DarkBorder else LightBorder
                                        )
                                }
                        }
                }
        }
}

@Composable
fun DownloadItemView(item: DownloadStatus, isDarkTheme: Boolean, onClick: () -> Unit) {
        val textColor = if (isDarkTheme) DarkText else LightText
        val mutedColor = if (isDarkTheme) DarkMuted else LightMuted

        Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = mutedColor,
                        modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                item.title,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )

                        val statusText =
                                when (item.status) {
                                        DownloadManager.STATUS_RUNNING -> {
                                                val speed =
                                                        item.getSpeedString(LocalContext.current)
                                                val eta = item.getEtaString()
                                                "Downloading... $speed • $eta"
                                        }
                                        DownloadManager.STATUS_PAUSED -> "Paused"
                                        DownloadManager.STATUS_PENDING -> "Pending"
                                        DownloadManager.STATUS_SUCCESSFUL ->
                                                Formatter.formatFileSize(
                                                        LocalContext.current,
                                                        item.totalSize
                                                )
                                        DownloadManager.STATUS_FAILED -> "Failed"
                                        else -> ""
                                }
                        Text(
                                statusText,
                                color = mutedColor,
                                style = MaterialTheme.typography.bodySmall
                        )

                        if (item.status == DownloadManager.STATUS_RUNNING && item.totalSize > 0) {
                                LinearProgressIndicator(
                                        progress = item.progress,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(top = 4.dp)
                                                        .height(2.dp)
                                )
                        }
                }
        }
}
