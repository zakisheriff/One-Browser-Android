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
import com.oneatom.onebrowser.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DownloadItem(
        val id: Long,
        val title: String,
        val status: Int,
        val reason: Int,
        val totalSize: Long,
        val bytesDownloaded: Long,
        val uri: String,
        val mediaType: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(isDarkTheme: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }

    // Refresh downloads
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val cursor = manager.query(DownloadManager.Query())
            val list = mutableListOf<DownloadItem>()
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                    val title =
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
                            )
                    val status =
                            cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                            )
                    val reason =
                            cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                            )
                    val totalSize =
                            cursor.getLong(
                                    cursor.getColumnIndexOrThrow(
                                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                                    )
                            )
                    val bytesDownloaded =
                            cursor.getLong(
                                    cursor.getColumnIndexOrThrow(
                                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                    )
                            )
                    val uri =
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                            )
                                    ?: ""
                    val mediaType =
                            cursor.getString(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)
                            )

                    list.add(
                            DownloadItem(
                                    id,
                                    title,
                                    status,
                                    reason,
                                    totalSize,
                                    bytesDownloaded,
                                    uri,
                                    mediaType
                            )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            downloads = list.sortedByDescending { it.id }
        }
    }

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
                                if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        val uri =
                                                if (item.uri.startsWith("file://"))
                                                        Uri.parse(item.uri)
                                                else Uri.parse(item.uri)
                                        // For API 24+ we might need FileProvider if it's a file://
                                        // URI, but DownloadManager usually gives content:// or we
                                        // rely on system
                                        // Actually DownloadManager returns content:// usually for
                                        // getUriForDownloadedFile
                                        val fileUri =
                                                try {
                                                    (context.getSystemService(
                                                                    Context.DOWNLOAD_SERVICE
                                                            ) as
                                                                    DownloadManager)
                                                            .getUriForDownloadedFile(item.id)
                                                } catch (e: Exception) {
                                                    Uri.parse(item.uri)
                                                }

                                        if (fileUri != null) {
                                            intent.setDataAndType(fileUri, item.mediaType)
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            context.startActivity(intent)
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
                    Divider(color = if (isDarkTheme) DarkBorder else LightBorder)
                }
            }
        }
    }
}

@Composable
fun DownloadItemView(item: DownloadItem, isDarkTheme: Boolean, onClick: () -> Unit) {
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
            Text(item.title, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)

            val statusText =
                    when (item.status) {
                        DownloadManager.STATUS_RUNNING -> "Downloading..."
                        DownloadManager.STATUS_PAUSED -> "Paused"
                        DownloadManager.STATUS_PENDING -> "Pending"
                        DownloadManager.STATUS_SUCCESSFUL ->
                                Formatter.formatFileSize(LocalContext.current, item.totalSize)
                        DownloadManager.STATUS_FAILED -> "Failed"
                        else -> ""
                    }
            Text(statusText, color = mutedColor, style = MaterialTheme.typography.bodySmall)

            if (item.status == DownloadManager.STATUS_RUNNING && item.totalSize > 0) {
                LinearProgressIndicator(
                        progress = item.bytesDownloaded.toFloat() / item.totalSize.toFloat(),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(2.dp)
                )
            }
        }
    }
}
