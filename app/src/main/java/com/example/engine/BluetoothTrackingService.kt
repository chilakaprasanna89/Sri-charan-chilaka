package com.example.engine

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class BluetoothTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var trackingEngine: TrackingEngine

    companion object {
        const val CHANNEL_ID = "SPA_FIELD_MAPPER_CHANNEL"
        const val NOTIFICATION_ID = 405
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        trackingEngine = TrackingEngine.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopForegroundService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        // Acquire wake lock to keep CPU alive when screen is off
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SPAFieldMapper::TrackingWakeLock").apply {
            acquire(30 * 60 * 1000L) // 30 mins max lock safety
        }

        val notification = buildNotification("SPA Field Mapper: Tracing Active", "0.00m mapped • 0.00 sq.m")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE // Matches target SDK requirements for general background sensor/pulse tracking
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start observing updates in tracking stats to refresh the notification
        serviceScope.launch {
            combineStateUpdates()
        }
    }

    private suspend fun combineStateUpdates() {
        coroutineScope {
            launch {
                trackingEngine.isTracking.collectLatest { isTracking ->
                    if (!isTracking) {
                        stopForegroundService()
                    }
                }
            }

            launch {
                trackingEngine.totalDistance.collectLatest { distance ->
                    updateNotification(distance, trackingEngine.localArea.value)
                }
            }

            launch {
                trackingEngine.localArea.collectLatest { area ->
                    updateNotification(trackingEngine.totalDistance.value, area)
                }
            }
        }
    }

    private fun updateNotification(distance: Double, area: Double) {
        val title = "SPA Field Mapper: Mapping Field..."
        val content = "Distance: %.2fm  •  Area: %.2f sq.m".format(distance, area)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Standard system icon fallback
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Silent background persistent ticker
            .build()
    }

    private fun stopForegroundService() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SPA Field Mapper Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for active Agritech field surveying"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
