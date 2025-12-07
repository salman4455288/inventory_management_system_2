package com.example.inventorymanagement.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.inventorymanagement.R
import com.example.inventorymanagement.activity.MainActivity

object NotificationHelper {

    // CHANGED: IDs to "_v3" to force a fresh channel creation with sound settings
    private const val CHANNEL_ID_REPORTS = "channel_reports_v3"
    private const val CHANNEL_ID_ALERTS = "channel_alerts_v3"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Define the sound explicitly
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Reports Channel
            val reportChannel = NotificationChannel(
                CHANNEL_ID_REPORTS,
                "Reports",
                NotificationManager.IMPORTANCE_HIGH // HIGH ensures sound + popup
            ).apply {
                description = "Notifications for downloaded PDF reports"
                setSound(soundUri, audioAttributes) // Force sound
                enableVibration(true)
            }

            // 2. Alerts Channel
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Stock Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts when inventory runs low"
                setSound(soundUri, audioAttributes) // Force sound
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(reportChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    // 1. Report Download Notification
    fun showReportDownloadNotification(context: Context, fileName: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Use default notification sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REPORTS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Report Downloaded")
            .setContentText("File saved: $fileName")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Changed to HIGH
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri) // Explicitly set sound on builder for older Androids
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(1001, builder.build())
    }

    // 2. Low Stock Notification
    fun showLowStockNotification(context: Context, count: Int) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Low Stock Alert!")
            .setContentText("Warning: $count items are running low.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Action Required: $count items have dropped below minimum stock levels. Please restock soon."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(2001, builder.build())
    }
}