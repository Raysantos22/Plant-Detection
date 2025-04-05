package Plantdetection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.PlantDetection.PlantDatabaseManager
import com.PlantDetection.PlantManagementActivity
import com.PlantDetection.PlantNotificationService
import com.PlantDetection.R
import java.util.*

class MissedTaskNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CHECK_MISSED_TASKS = "com.PlantDetection.CHECK_MISSED_TASKS"
        const val CHANNEL_ID = "missed_tasks_channel"
        private const val TAG = "MissedTaskNotification"

        // Method to schedule daily missed task check
        fun scheduleDailyMissedTaskCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Create intent for missed task check
            val intent = Intent(context, MissedTaskNotificationReceiver::class.java).apply {
                action = ACTION_CHECK_MISSED_TASKS
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    context,
                    0, // Use a fixed request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    context,
                    0, // Use a fixed request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Use inexact repeating to reduce alarm count
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    getTomorrowMidnight().timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    getTomorrowMidnight().timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }

        private fun getTomorrowMidnight(): Calendar {
            return Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        // Method to create notification channel
        fun createMissedTaskNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Missed Plant Tasks"
                val descriptionText = "Notifications for missed plant care tasks"
                val importance = android.app.NotificationManager.IMPORTANCE_HIGH
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID, 
                    name, 
                    importance
                ).apply {
                    description = descriptionText
                }

                val notificationManager = 
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CHECK_MISSED_TASKS -> checkAndNotifyMissedTasks(context)
        }
    }

    private fun checkAndNotifyMissedTasks(context: Context) {
        // Initialize database manager
        val plantDatabaseManager = PlantDatabaseManager(context)
        val notificationService = PlantNotificationService(context)

        // Get today and yesterday's date
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val yesterday = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_MONTH, -1)
        }.time

        // Get all plants
        val plants = plantDatabaseManager.getAllPlants()

        // Track missed notifications
        var totalMissedTasks = 0

        // Check each plant's events
        for (plant in plants) {
            // Get events that were scheduled for yesterday but not completed
            val missedEvents = plantDatabaseManager.getPlantCareEvents(plant.id)
                .filter { event ->
                    !event.completed && 
                    event.date.before(today) && 
                    event.date.after(yesterday) &&
                    !event.eventType.equals("Scan", ignoreCase = true)
                }

            // If there are missed events, create a notification
            if (missedEvents.isNotEmpty()) {
                totalMissedTasks += missedEvents.size

                // Group events by type
                val missedEventTypes = missedEvents
                    .map { it.eventType }
                    .distinct()

                // Create notification content
                val title = "Missed Plant Care Tasks"
                val content = buildMissedTaskContent(plant, missedEventTypes)

                // Show notification
                showMissedTaskNotification(
                    context, 
                    plant.id, 
                    plant.name, 
                    title, 
                    content
                )
            }
        }

        // Log total missed tasks
        if (totalMissedTasks > 0) {
            Log.d(TAG, "Found $totalMissedTasks missed plant care tasks")
        }
    }

    private fun buildMissedTaskContent(
        plant: PlantDatabaseManager.Plant, 
        missedEventTypes: List<String>
    ): String {
        // Create a readable list of missed task types
        val formattedTypes = missedEventTypes.map { type ->
            when (type.lowercase()) {
                "watering" -> "Watering"
                "fertilize" -> "Fertilizing"
                "treatment" -> "Treatment"
                "prune" -> "Pruning"
                else -> type
            }
        }

        return buildString {
            append("You missed ")
            append(formattedTypes.joinToString(" and "))
            append(" for ${plant.name}")
        }
    }

    private fun showMissedTaskNotification(
        context: Context, 
        plantId: String, 
        plantName: String, 
        title: String, 
        content: String
    ) {
        // Create intent to open plant management
        val intent = Intent(context, PlantManagementActivity::class.java).apply {
            putExtra("OPEN_PLANT_ID", plantId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                plantId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                plantId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plant_care)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show notification
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(plantId.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show missed task notification: Permission denied", e)
        }
    }
}

// Extension functions for PlantDatabaseManager
fun PlantDatabaseManager.getPlantCareEventsForDate(
    plantId: String, 
    date: Date
): List<PlantDatabaseManager.PlantCareEvent> {
    // Get all events for a specific date
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendar.time

    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    val endOfDay = calendar.time

    return getPlantCareEvents(plantId)
        .filter { event ->
            event.date >= startOfDay && event.date <= endOfDay
        }
}