package com.oneatom.onebrowser.data

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.text.format.Formatter
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
                val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                val mediaType =
                        it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))

                var speed = 0L
                var eta = -1L

                if (status == DownloadManager.STATUS_RUNNING) {
                    val prevBytes = previousBytesMap[id]
                    val prevTime = previousTimeMap[id]
                    val lastKnownSpeed = previousSpeedMap[id] ?: 0L

                    if (prevBytes != null && prevTime != null) {
                        val timeDiff = currentTime - prevTime
                        val bytesDiff = downloadedBytes - prevBytes
                        if (timeDiff > 0) {
                            // Calculate instantaneous speed
                            val instantSpeed = (bytesDiff * 1000) / timeDiff

                            // Exponential Moving Average (smoothing)
                            // alpha = 0.3 means new value has 30% weight, old has 70%
                            speed =
                                    if (lastKnownSpeed > 0) {
                                        (lastKnownSpeed * 0.7 + instantSpeed * 0.3).toLong()
                                    } else {
                                        instantSpeed
                                    }

                            if (speed > 0) {
                                val remainingBytes = totalSize - downloadedBytes
                                eta = remainingBytes / speed
                            } else {
                                // If speed drops to 0 temporarily (e.g. buffer), keep last known
                                // valid speed
                                // or just show calculating if it stays 0 for too long.
                                // simpler: keep last known speed for a bit?
                                speed = lastKnownSpeed
                                if (speed > 0) {
                                    val remainingBytes = totalSize - downloadedBytes
                                    eta = remainingBytes / speed
                                }
                            }
                        }
                    } else {
                        // First tick, can't calculate speed yet
                        speed = lastKnownSpeed
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
        _downloads.value = newDownloads.sortedByDescending { it.id }
    }
}
