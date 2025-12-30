package com.oneatom.onebrowser.data

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import android.webkit.URLUtil
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownloadStatus(
        val id: Long,
        val title: String,
        val totalSize: Long,
        val downloadedBytes: Long,
        val status: Int,
        val speedBytesPerSecond: Long = 0,
        val etaSeconds: Long = -1,
        val mediaType: String? = null,
        val uri: String? = null
) {
    val progress: Float
        get() = if (totalSize > 0) downloadedBytes.toFloat() / totalSize else 0f

    fun getSpeedString(context: Context): String {
        return Formatter.formatFileSize(context, speedBytesPerSecond) + "/s"
    }

    fun getEtaString(): String {
        if (etaSeconds < 0) return "Calculating..."
        val hours = etaSeconds / 3600
        val minutes = (etaSeconds % 3600) / 60
        val seconds = etaSeconds % 60
        return if (hours > 0) {
            String.format("%dh %02dm %02ds left", hours, minutes, seconds)
        } else if (minutes > 0) {
            String.format("%dm %02ds left", minutes, seconds)
        } else {
            String.format("%ds left", seconds)
        }
    }
}

object DownloadTracker {
    private val _downloads = MutableStateFlow<List<DownloadStatus>>(emptyList())
    val downloads: StateFlow<List<DownloadStatus>> = _downloads.asStateFlow()

    // Cache previous speed to smoothing
    private val previousSpeedMap = mutableMapOf<Long, Long>()
    private val previousBytesMap = mutableMapOf<Long, Long>()
    private val previousTimeMap = mutableMapOf<Long, Long>()

    // Start tracking in a coroutine scope provided by ViewModel or Service
    suspend fun startTracking(context: Context) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (true) {
            updateDownloads(downloadManager)
            delay(1000) // Poll every second
        }
    }

    fun cancelDownload(context: Context, id: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(id)
    }

    fun deleteDownload(context: Context, id: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(id) // removes file and record
    }

    private fun updateDownloads(downloadManager: DownloadManager) {
        val query = DownloadManager.Query()
        val cursor: Cursor? =
                try {
                    downloadManager.query(query)
                } catch (e: Exception) {
                    null
                }

        val newDownloads = mutableListOf<DownloadStatus>()
        val currentTime = System.currentTimeMillis()

        // 1. Add System Downloads
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE))
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val totalSize =
                        it.getLong(
                                it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                val downloadedBytes =
                        it.getLong(
                                it.getColumnIndexOrThrow(
                                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                )
                        )
                val mediaType =
                        it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_URI))

                // Calculate speed/eta
                var speed: Long = 0
                var eta: Long = -1

                if (status == DownloadManager.STATUS_RUNNING) {
                    val prevBytes = previousBytesMap[id] ?: 0
                    val prevTime = previousTimeMap[id] ?: currentTime
                    val timeDiff = currentTime - prevTime
                    if (timeDiff > 0) {
                        val bytesDiff = downloadedBytes - prevBytes
                        val instantSpeed = (bytesDiff * 1000) / timeDiff
                        // Simple smoothing
                        val lastSpeed = previousSpeedMap[id] ?: instantSpeed
                        speed = (lastSpeed + instantSpeed) / 2
                    } else {
                        speed = previousSpeedMap[id] ?: 0
                    }

                    if (speed > 0 && totalSize > 0) {
                        eta = (totalSize - downloadedBytes) / speed
                    }

                    previousBytesMap[id] = downloadedBytes
                    previousTimeMap[id] = currentTime
                    previousSpeedMap[id] = speed
                } else {
                    previousBytesMap.remove(id)
                    previousTimeMap.remove(id)
                    previousSpeedMap.remove(id)
                }

                newDownloads.add(
                        DownloadStatus(
                                id,
                                title,
                                totalSize,
                                downloadedBytes,
                                status,
                                speed,
                                eta,
                                mediaType,
                                uri
                        )
                )
            }
        }

        // 2. Add Custom Downloads
        val customItems = CustomDownloadEngine.downloads.value
        for (item in customItems) {
            newDownloads.add(
                    DownloadStatus(
                            item.id.hashCode().toLong(),
                            item.filename,
                            item.totalSize,
                            item.downloadedBytes,
                            item.status,
                            item.speed,
                            item.eta,
                            null,
                            item.url
                    )
            )
        }

        _downloads.value = newDownloads.sortedByDescending { it.id }
    }

    fun restartDownload(context: Context, download: DownloadStatus) {
        // Remove old
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(download.id)

        // Start new
        if (download.uri != null) {
            try {
                val request = DownloadManager.Request(Uri.parse(download.uri))
                if (download.mediaType != null) {
                    request.setMimeType(download.mediaType)
                }
                request.setTitle(download.title)
                request.setDescription("Downloading file...")
                // Ensure system notification manages persistence
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                // Re-guess filename or use existing title if valid filename
                val filename =
                        if (download.title.contains(".")) download.title
                        else URLUtil.guessFileName(download.uri, null, download.mediaType)
                // Clean filename to prevent .bin
                val cleanFilename =
                        if (filename.endsWith(".bin")) filename.replace(".bin", ".apk")
                        else filename

                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        cleanFilename
                )

                downloadManager.enqueue(request)
                com.oneatom.onebrowser.services.DownloadService.start(context)
                Toast.makeText(context, "Restarting download...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to restart: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }
}
