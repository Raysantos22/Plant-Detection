package com.PlantDetection

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.PlantDetection.BoundingBox
import com.PlantDetection.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class to manage plant monitoring notifications and tasks
 */
class   PlantMonitoringManager(private val context: Context) {

    companion object {
        private const val TAG = "PlantMonitoring"
        const val CHANNEL_ID = "plant_monitoring_channel"
        const val COMPANION_OBJECT_NAME = "PlantMonitoring"

        // Intent actions
        const val ACTION_SHOW_NOTIFICATION = "com.PlantDetection.SHOW_NOTIFICATION"
        const val ACTION_OPEN_CONDITION_DETAILS = "com.PlantDetection.OPEN_CONDITION_DETAILS"

        // Intent extras
        const val EXTRA_DETECTION_ID = "detection_id"
        const val EXTRA_CONDITION_NAME = "condition_name"
        const val EXTRA_VEGETABLE_TYPE = "vegetable_type"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create the notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Plant Monitoring"
            val descriptionText = "Notifications for plant monitoring tasks"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule a check notification for a plant condition
     */
    fun scheduleCheckNotification(detectionId: String, conditionName: String, vegetableType: String) {
        val sharedPrefs = context.getSharedPreferences(COMPANION_OBJECT_NAME, Context.MODE_PRIVATE)
        val nextCheckTime = sharedPrefs.getLong("${detectionId}_next_check", 0)

        if (nextCheckTime <= 0) {
            Log.e(TAG, "Invalid next check time for detection: $detectionId")
            return
        }

        // Create intent for the notification
        val notificationIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SHOW_NOTIFICATION
            putExtra(EXTRA_DETECTION_ID, detectionId)
            putExtra(EXTRA_CONDITION_NAME, conditionName)
            putExtra(EXTRA_VEGETABLE_TYPE, vegetableType)
            putExtra(EXTRA_NOTIFICATION_ID, detectionId.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            detectionId.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // For demonstration purposes, we're using a shorter interval
        // In production, you would use the actual nextCheckTime
        val triggerTime = SystemClock.elapsedRealtime() + 60 * 1000 // 1 minute for testing

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        // Format the time for logging
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val scheduledTime = Date(System.currentTimeMillis() + (triggerTime - SystemClock.elapsedRealtime()))
        Log.d(TAG, "Scheduled notification for $conditionName at ${sdf.format(scheduledTime)}")
    }

    /**
     * Save a new detected plant condition for monitoring
     */
    fun saveDetection(detection: BoundingBox, vegetableType: String): String {
        val sharedPrefs = context.getSharedPreferences(COMPANION_OBJECT_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Create unique identifier for this detection
        val detectionId = "detection_${System.currentTimeMillis()}"

        // Save basic detection info
        editor.putString("${detectionId}_condition", detection.clsName)
        editor.putString("${detectionId}_date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        editor.putString("${detectionId}_vegetable", vegetableType)
        editor.putFloat("${detectionId}_confidence", detection.cnf)

        // Add to list of detections
        val detectionsList = sharedPrefs.getStringSet("saved_detections", HashSet()) ?: HashSet()
        val updatedList = HashSet(detectionsList)
        updatedList.add(detectionId)
        editor.putStringSet("saved_detections", updatedList)

        // Set up initial monitoring status
        val checkInterval = getCheckIntervalForCondition(detection.clsName)
        editor.putLong("${detectionId}_next_check", System.currentTimeMillis() + checkInterval)

        editor.apply()

        return detectionId
    }

    /**
     * Get appropriate check interval based on condition type
     */
    fun getCheckIntervalForCondition(conditionName: String): Long {
        return when {
            conditionName.startsWith("Healthy") -> 24 * 60 * 60 * 1000 // 24 hours for healthy plants
            else -> 12 * 60 * 60 * 1000 // 12 hours for diseased/pest conditions
        }
    }

    /**
     * Mark a monitoring task as complete
     */
    fun markTaskComplete(detectionId: String, conditionName: String) {
        val sharedPrefs = context.getSharedPreferences(COMPANION_OBJECT_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Update next check time
        val nextCheckTime = System.currentTimeMillis() + getCheckIntervalForCondition(conditionName)
        editor.putLong("${detectionId}_next_check", nextCheckTime)
        editor.putLong("${detectionId}_last_completed", System.currentTimeMillis())

        // Update task completion count
        val completedCount = sharedPrefs.getInt("${detectionId}_completed_count", 0)
        editor.putInt("${detectionId}_completed_count", completedCount + 1)

        editor.apply()
    }

    /**
     * Set a custom reminder time for a condition
     */
    fun setCustomReminder(detectionId: String, reminderTimeMillis: Long) {
        val sharedPrefs = context.getSharedPreferences(COMPANION_OBJECT_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        editor.putLong("${detectionId}_next_check", reminderTimeMillis)
        editor.apply()
    }

    /**
     * Mark a condition as resolved
     */
    fun markConditionResolved(detectionId: String) {
        val sharedPrefs = context.getSharedPreferences(COMPANION_OBJECT_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        editor.putBoolean("${detectionId}_resolved", true)
        editor.putLong("${detectionId}_resolved_date", System.currentTimeMillis())

        editor.apply()

        // Cancel any pending notifications
        cancelScheduledNotification(detectionId)
    }

    /**
     * Cancel a scheduled notification
     */
    private fun cancelScheduledNotification(detectionId: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            detectionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}