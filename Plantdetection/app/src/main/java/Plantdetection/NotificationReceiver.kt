package com.PlantDetection

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.PlantDetection.ConditionDetailActivity
import com.PlantDetection.PlantMonitoringManager
import com.PlantDetection.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PlantMonitoringManager.ACTION_SHOW_NOTIFICATION -> {
                showPlantCheckNotification(context, intent)
            }
        }
    }

    private fun showPlantCheckNotification(context: Context, intent: Intent) {
        val detectionId = intent.getStringExtra(PlantMonitoringManager.EXTRA_DETECTION_ID) ?: return
        val conditionName = intent.getStringExtra(PlantMonitoringManager.EXTRA_CONDITION_NAME) ?: return
        val vegetableType = intent.getStringExtra(PlantMonitoringManager.EXTRA_VEGETABLE_TYPE) ?: return
        val notificationId = intent.getIntExtra(PlantMonitoringManager.EXTRA_NOTIFICATION_ID, 0)

        // Create intent to open condition details when notification is tapped
        val detailsIntent = Intent(context, ConditionDetailActivity::class.java).apply {
            action = PlantMonitoringManager.ACTION_OPEN_CONDITION_DETAILS
            putExtra(PlantMonitoringManager.EXTRA_DETECTION_ID, detectionId)
            putExtra(PlantMonitoringManager.EXTRA_CONDITION_NAME, conditionName)
            putExtra(PlantMonitoringManager.EXTRA_VEGETABLE_TYPE, vegetableType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            detectionId.hashCode(),
            detailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification content
        val title = if (conditionName.startsWith("Healthy")) {
            "Time to water your ${vegetableType}"
        } else {
            "Check your ${vegetableType} for ${conditionName}"
        }

        val message = if (conditionName.startsWith("Healthy")) {
            "Your plant needs regular care to stay healthy!"
        } else {
            "Follow the treatment steps to address the ${conditionName} condition."
        }

        // Build the notification
        val builder = NotificationCompat.Builder(context, PlantMonitoringManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status_indicator)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Handle notification permission not granted on Android 13+
                Log.e("PlantMonitoring", "Notification permission not granted", e)
            }
        }
    }
}