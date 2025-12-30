package com.oneatom.onebrowser.data

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

data class CustomDownloadItem(
        val id: String,
        val url: String,
        val filename: String,
        val totalSize: Long,
        val downloadedBytes: Long,
        val status: Int, // Uses DownloadManager constants for compatibility
        val speed: Long = 0,
        val eta: Long = -1
)

object CustomDownloadEngine {
    private val client = OkHttpClient()
    private val activeDownloads = ConcurrentHashMap<String, DownloadTask>()
    // Merging flow for UI: We might need a combined flow later, or just expose this one
    private val _downloads = MutableStateFlow<List<CustomDownloadItem>>(emptyList())
    val downloads: StateFlow<List<CustomDownloadItem>> = _downloads.asStateFlow()

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: android.app.NotificationManager
    private lateinit var context: Context

    // Constants mapping to DownloadManager for UI compatibility
    const val STATUS_PENDING = 1
    const val STATUS_RUNNING = 2
    const val STATUS_PAUSED = 4
    const val STATUS_SUCCESSFUL = 8
    const val STATUS_FAILED = 16

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
        notificationManager =
                ctx.getSystemService(Context.NOTIFICATION_SERVICE) as
                        android.app.NotificationManager
        // TODO: Load persistent downloads from DB/Prefs so they survive restarts
    }

    fun startDownload(ctx: Context, url: String, filename: String) {
        // Ensure context is initialized even if service hasn't started yet
        if (!::context.isInitialized) {
            initialize(ctx)
        }

        val id = url.hashCode().toString()
        if (activeDownloads.containsKey(id)) {
            // If exists and failed/paused, restart/resume
            val task = activeDownloads[id]
            if (task?.status == STATUS_FAILED || task?.status == STATUS_PAUSED) {
                resumeDownload(id)
            }
            return
        }

        val task = DownloadTask(id, url, filename)
        activeDownloads[id] = task
        engineScope.launch { task.run(context) }
        updateList()
    }

    fun pauseDownload(id: String) {
        activeDownloads[id]?.pause()
        updateList()
    }

    fun resumeDownload(id: String) {
        val task = activeDownloads[id]
        if (task != null) {
            task.resume()
            if (!task.isRunning) {
                engineScope.launch { task.run(context) }
            }
        }
        updateList()
    }

    fun cancelDownload(id: String) {
        val task = activeDownloads[id]
        task?.cancel()
        activeDownloads.remove(id)
        updateList()
        notificationManager.cancel(id.hashCode())
    }

    private fun updateList() {
        val list =
                activeDownloads.values.map {
                    CustomDownloadItem(
                            it.id,
                            it.url,
                            it.filename,
                            it.totalSize,
                            it.downloadedBytes,
                            it.status,
                            it.speed,
                            it.eta
                    )
                }
        _downloads.value = list
    }

    // Internal Task Class
    private class DownloadTask(val id: String, val url: String, val filename: String) {
        var totalSize: Long = 0
        var downloadedBytes: Long = 0
        var status: Int = STATUS_PENDING
        var speed: Long = 0
        var eta: Long = -1

        var isPaused = false
        var isRunning = false
        private var isCancelled = false

        suspend fun run(context: Context) {
            isRunning = true
            status = STATUS_PENDING
            updateNotification(context)
            updateList() // Added

            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)

            // Check for existing partial file for Resume
            var startByte = 0L
            if (file.exists()) {
                startByte = file.length()
                downloadedBytes = startByte
            }

            try {
                val requestBuilder = Request.Builder().url(url)
                if (startByte > 0) {
                    requestBuilder.header("Range", "bytes=$startByte-")
                }
                val request = requestBuilder.build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    status = STATUS_FAILED
                    isRunning = false
                    updateNotification(context)
                    updateList() // Added
                    return
                }

                val body = response.body
                if (body == null) {
                    status = STATUS_FAILED
                    isRunning = false
                    updateList() // Added
                    return
                }

                val contentLength = body.contentLength()
                totalSize = if (contentLength == -1L) -1L else (contentLength + startByte)

                status = STATUS_RUNNING
                updateList() // Added

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(file, true) // Append mode for resume
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int

                var lastUpdate = System.currentTimeMillis()
                var bytesSinceUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled) {
                        outputStream.close()
                        inputStream.close()
                        file.delete()
                        updateList() // Added
                        return
                    }

                    if (isPaused) {
                        status = STATUS_PAUSED
                        updateNotification(context)
                        outputStream.close()
                        inputStream.close()
                        isRunning = false
                        updateList() // Added
                        return // Exit loop, will restart on resume
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 1000) {
                        speed = (bytesSinceUpdate * 1000) / (now - lastUpdate)
                        if (speed > 0 && totalSize > 0) {
                            eta = (totalSize - downloadedBytes) / speed
                        }
                        lastUpdate = now
                        bytesSinceUpdate = 0
                        // Notify UI update
                        updateList() // Added
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (downloadedBytes >= totalSize || totalSize == -1L) {
                    status = STATUS_SUCCESSFUL
                    updateNotification(context)
                    updateList() // Added
                }
            } catch (e: Exception) {
                status = STATUS_FAILED
                e.printStackTrace()
                updateList() // Added
            } finally {
                isRunning = false
                updateNotification(context)
                updateList() // Added
            }
        }

        fun pause() {
            isPaused = true
        }

        fun resume() {
            isPaused = false
        }

        fun cancel() {
            isCancelled = true
        }

        private fun updateNotification(context: Context) {
            // Basic placeholder notification logic
            // Ideally calls back to DownloadService to standardize
        }
    }
}
