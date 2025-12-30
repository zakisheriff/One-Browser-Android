package com.oneatom.onebrowser.services

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.oneatom.onebrowser.R
import com.oneatom.onebrowser.data.DownloadTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val CHANNEL_NAME = "Downloads"

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure service restarts if killed
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Start tracking
        serviceScope.launch {
            launch { DownloadTracker.startTracking(this@DownloadService) }
            launch {
                DownloadTracker.downloads.collectLatest { downloads ->
                    val active =
                            downloads.filter {
                                it.status == DownloadManager.STATUS_RUNNING ||
                                        it.status == DownloadManager.STATUS_PENDING ||
                                        it.status == DownloadManager.STATUS_PAUSED
                            }

                    if (active.isEmpty()) {
                        stopForeground(true)
                        stopSelf()
                    } else {
                        active.forEach { download -> updateNotification(download) }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager
                                    .IMPORTANCE_LOW // Low to not make sound on every update
                    )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(download: com.oneatom.onebrowser.data.DownloadStatus) {
        val speed = download.getSpeedString(this)
        val eta = download.getEtaString()
        val progress = (download.progress * 100).toInt()

        // Open Downloads on click
        val intent =
                Intent(this, com.oneatom.onebrowser.MainActivity::class.java).apply {
                    putExtra("open_downloads", true)
                }
        val pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

        val contentText =
                when (download.status) {
                    DownloadManager.STATUS_RUNNING -> "$speed • $eta"
                    DownloadManager.STATUS_PENDING -> "Pending..."
                    DownloadManager.STATUS_PAUSED -> "Paused"
                    else -> ""
                }

        val notification =
                NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(download.title)
                        .setContentText(contentText)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setProgress(100, progress, false)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setContentIntent(pendingIntent)
                        .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    download.id.toInt(),
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(download.id.toInt(), notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
