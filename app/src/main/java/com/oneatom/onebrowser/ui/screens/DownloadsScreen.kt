package com.oneatom.onebrowser.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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

        // Handle System Back Button
        BackHandler(onBack = onBack)

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
                                                },
                                                onCancel = {
                                                        DownloadTracker.cancelDownload(
                                                                context,
                                                                item.id
                                                        )
                                                },
                                                onDelete = {
                                                        DownloadTracker.deleteDownload(
                                                                context,
                                                                item.id
                                                        )
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
fun DownloadItemView(
        item: DownloadStatus,
        isDarkTheme: Boolean,
        onClick: () -> Unit,
        onCancel: () -> Unit,
        onDelete: () -> Unit
) {
        val textColor = if (isDarkTheme) DarkText else LightText
        val mutedColor = if (isDarkTheme) DarkMuted else LightMuted
        val context = LocalContext.current

        Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = mutedColor,
                        modifier = Modifier.size(32.dp)
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
                                                val speed = item.getSpeedString(context)
                                                val eta = item.getEtaString()
                                                // Format: 5MB / 10MB (50%) • 2MB/s • 10s left
                                                val downloaded =
                                                        Formatter.formatFileSize(
                                                                context,
                                                                item.downloadedBytes
                                                        )
                                                val total =
                                                        Formatter.formatFileSize(
                                                                context,
                                                                item.totalSize
                                                        )
                                                val percent = (item.progress * 100).toInt()
                                                "$downloaded / $total ($percent%) • $speed • $eta"
                                        }
                                        DownloadManager.STATUS_PAUSED -> "Paused"
                                        DownloadManager.STATUS_PENDING -> "Pending..."
                                        DownloadManager.STATUS_SUCCESSFUL -> {
                                                val size =
                                                        Formatter.formatFileSize(
                                                                context,
                                                                item.totalSize
                                                        )
                                                "Completed • $size"
                                        }
                                        DownloadManager.STATUS_FAILED -> "Failed"
                                        else -> "Unknown"
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
                                                        .padding(top = 8.dp)
                                                        .height(4.dp)
                                )
                        }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Actions
                if (item.status == DownloadManager.STATUS_RUNNING ||
                                item.status == DownloadManager.STATUS_PENDING ||
                                item.status == DownloadManager.STATUS_PAUSED
                ) {
                        IconButton(onClick = onCancel) {
                                Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = textColor
                                )
                        }
                } else if (item.status == DownloadManager.STATUS_SUCCESSFUL ||
                                item.status == DownloadManager.STATUS_FAILED
                ) {
                        IconButton(onClick = onDelete) {
                                Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = textColor
                                )
                        }
                }
        }
}
