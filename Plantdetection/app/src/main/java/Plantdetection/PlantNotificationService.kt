package com.PlantDetection

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.PlantDetection.PlantManagementActivity
import java.util.*
import com.PlantDetection.R


/**
 * Service for managing plant care notifications
 */
class PlantNotificationService(private val context: Context) {

    companion object {
        private const val TAG = "PlantNotification"
        const val CHANNEL_ID = "plant_care_channel"
        const val ACTION_SHOW_CARE_NOTIFICATION = "com.PlantDetection.SHOW_CARE_NOTIFICATION"

        // Extras for notification intents
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_PLANT_ID = "plant_id"
        const val EXTRA_PLANT_NAME = "plant_name"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_CONDITION_NAME = "condition_name"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create the notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Plant Care"
            val descriptionText = "Notifications for plant care activities"
            val importance = NotificationManager.IMPORTANCE_HIGH // Use HIGH to ensure user sees it
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule a notification for a plant care event
     */
    fun scheduleNotification(
        plantId: String,
        plantName: String,
        eventId: String,
        eventType: String,
        scheduledTime: Date,
        conditionName: String? = null
    ) {
        val notificationId = eventId.hashCode()

        // Create intent for the notification
        val intent = Intent(context, PlantNotificationReceiver::class.java).apply {
            action = ACTION_SHOW_CARE_NOTIFICATION
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_PLANT_ID, plantId)
            putExtra(EXTRA_PLANT_NAME, plantName)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TYPE, eventType)
            conditionName?.let { putExtra(EXTRA_CONDITION_NAME, it) }
        }

        // Create pending intent
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Get alarm manager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check for permission to schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Show notification immediately instead of scheduling it
                Log.w(TAG, "No permission to schedule exact alarms, showing immediate notification instead")
                showNotificationNow(plantId, plantName, eventType, conditionName)

                // Optionally show a message to the user about permission
                // and direct them to settings
                val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(settingsIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open settings for exact alarm permission", e)
                }
                return
            }
        }

        // Schedule the notification
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTime.time,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    scheduledTime.time,
                    pendingIntent
                )
            }

            Log.d(TAG, "Notification scheduled for ${scheduledTime} for plant: $plantName, event: $eventType")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when scheduling notification", e)
            // Fall back to showing notification immediately
            showNotificationNow(plantId, plantName, eventType, conditionName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification", e)
        }
    }

    /**
     * Cancel a scheduled notification
     */
    fun cancelNotification(eventId: String) {
        val notificationId = eventId.hashCode()

        val intent = Intent(context, PlantNotificationReceiver::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        // Also cancel any showing notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    /**
     * Show a notification immediately (for testing)
     */
    fun showNotificationNow(
        plantId: String,
        plantName: String,
        eventType: String,
        conditionName: String? = null
    ) {
        val notificationId = (plantId + eventType + System.currentTimeMillis()).hashCode()

        // Create intent to open plant details
        val intent = Intent(context, PlantManagementActivity::class.java).apply {
            putExtra("OPEN_PLANT_ID", plantId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Create notification content
        val (title, content) = createNotificationContent(plantName, eventType, conditionName)

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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
            Log.e(TAG, "Failed to show notification: Permission denied", e)
        }
    }

    /**
     * Create notification content based on event type
     */
    private fun createNotificationContent(
        plantName: String,
        eventType: String,
        conditionName: String?
    ): Pair<String, String> {
        return when (eventType.lowercase()) {
            "watering" -> {
                Pair(
                    "Time to water your $plantName",
                    "Your plant needs water to stay healthy!"
                )
            }
            "treatment" -> {
                val condition = conditionName ?: "condition"
                Pair(
                    "Time to treat your $plantName",
                    "Your plant needs treatment for $condition"
                )
            }
            else -> {
                Pair(
                    "Plant care reminder for $plantName",
                    "Time to check on your plant"
                )
            }
        }
    }
}

/**
 * Receiver for handling plant care notifications
 */
