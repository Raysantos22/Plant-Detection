package com.PlantDetection

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.PlantDetection.PlantManagementActivity
import com.PlantDetection.PlantNotificationService
import com.PlantDetection.R


class PlantNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PlantNotificationService.ACTION_SHOW_CARE_NOTIFICATION) {
            val notificationId = intent.getIntExtra(PlantNotificationService.EXTRA_NOTIFICATION_ID, 0)
            val plantId = intent.getStringExtra(PlantNotificationService.EXTRA_PLANT_ID) ?: return
            val plantName = intent.getStringExtra(PlantNotificationService.EXTRA_PLANT_NAME) ?: "Your plant"
            val eventType = intent.getStringExtra(PlantNotificationService.EXTRA_EVENT_TYPE) ?: "care"
            val conditionName = intent.getStringExtra(PlantNotificationService.EXTRA_CONDITION_NAME)

            // Create intent to open plant management
            val activityIntent = Intent(context, PlantManagementActivity::class.java).apply {
                putExtra("OPEN_PLANT_ID", plantId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Create notification content
            val title: String
            val content: String

            when (eventType.lowercase()) {
                "watering" -> {
                    title = "Time to water your $plantName"
                    content = "Your plant needs water to stay healthy!"
                }
                "treatment" -> {
                    val condition = conditionName ?: "condition"
                    title = "Time to treat your $plantName"
                    content = "Your plant needs treatment for $condition"
                }
                else -> {
                    title = "Plant care reminder for $plantName"
                    content = "Time to check on your plant"
                }
            }

            // Build notification
            val builder = NotificationCompat.Builder(context, PlantNotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_plant_care)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Show notification
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationId, builder.build())
                }
            } catch (e: SecurityException) {
                Log.e("PlantNotification", "Failed to show notification: Permission denied", e)
            }
        }
    }
}