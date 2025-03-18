package com.PlantDetection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.*

class DailyTaskReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CHECK_TODAY_TASKS = "com.PlantDetection.CHECK_TODAY_TASKS"
        const val CHANNEL_ID = "daily_task_reminder_channel"
        private const val TAG = "DailyTaskReminder"

        fun scheduleHourlyTaskCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, DailyTaskReminderReceiver::class.java).apply {
                action = ACTION_CHECK_TODAY_TASKS
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Change to exact time check every minute
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                60 * 1000, // Check every minute
                pendingIntent
            )
        }

        fun createDailyTaskReminderChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Daily Plant Tasks"
                val descriptionText = "Notifications for upcoming plant care tasks"
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
            ACTION_CHECK_TODAY_TASKS -> checkAndNotifyTodayTasks(context)
        }
    }

    private fun checkAndNotifyTodayTasks(context: Context) {
        val plantDatabaseManager = PlantDatabaseManager(context)

        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val currentTime = calendar.time
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentHourMinute = timeFormat.format(currentTime)

        Log.d(TAG, "Checking tasks at $currentHourMinute")

        val plants = plantDatabaseManager.getAllPlants()

        for (plant in plants) {
            val todayEvents = plantDatabaseManager.getPlantCareEvents(plant.id)
                .filter { event ->
                    val eventDate = Calendar.getInstance().apply {
                        time = event.date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    !event.completed &&
                            todayDate(eventDate) == todayDate(today) &&
                            isEventDueNow(event.date, currentTime)
                }

            if (todayEvents.isNotEmpty()) {
                Log.d(TAG, "Found ${todayEvents.size} events for ${plant.name}")
                todayEvents.forEach { event ->
                    Log.d(TAG, "Event details: ${event.eventType} at ${event.date}")
                }

                val eventsByType = todayEvents.groupBy { it.eventType }
                eventsByType.forEach { (eventType, events) ->
                    showTodayTaskNotification(
                        context,
                        plant,
                        eventType,
                        events,
                        currentHourMinute
                    )
                }
            }
        }
    }

    private fun isEventDueNow(eventTime: Date, currentTime: Date): Boolean {
        val eventCalendar = Calendar.getInstance().apply { time = eventTime }
        val currentCalendar = Calendar.getInstance().apply { time = currentTime }

        val isExactMatch = eventCalendar.get(Calendar.HOUR_OF_DAY) == currentCalendar.get(Calendar.HOUR_OF_DAY) &&
                eventCalendar.get(Calendar.MINUTE) == currentCalendar.get(Calendar.MINUTE)

        if (isExactMatch) {
            Log.d(TAG, "Event is due now: ${eventTime}")
        }

        return isExactMatch
    }

    private fun todayDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun showTodayTaskNotification(
        context: Context,
        plant: PlantDatabaseManager.Plant,
        eventType: String,
        events: List<PlantDatabaseManager.PlantCareEvent>,
        currentTime: String
    ) {
        Log.d(TAG, "Showing notification for ${plant.name} - $eventType")

        val intent = Intent(context, PlantManagementActivity::class.java).apply {
            putExtra("OPEN_PLANT_ID", plant.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                plant.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                plant.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val (title, content) = createTaskNotificationContent(plant, eventType, events, currentTime)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plant_care)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(plant.id.hashCode(), builder.build())
                Log.d(TAG, "Notification sent successfully")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show task reminder notification: Permission denied", e)
        }
    }

    private fun createTaskNotificationContent(
        plant: PlantDatabaseManager.Plant,
        eventType: String,
        events: List<PlantDatabaseManager.PlantCareEvent>,
        currentTime: String
    ): Pair<String, String> {
        return when (eventType.lowercase()) {
            "watering" -> Pair(
                "Time to Water Your Plant",
                "It's $currentTime - time to water ${plant.name}"
            )
            "fertilize" -> Pair(
                "Fertilizing Reminder",
                "It's $currentTime - time to fertilize ${plant.name}"
            )
            "treatment" -> Pair(
                "Treatment Time",
                "It's $currentTime - treatment needed for ${plant.name}"
            )
            "prune" -> Pair(
                "Pruning Reminder",
                "It's $currentTime - time to prune ${plant.name}"
            )
            else -> Pair(
                "Plant Care Reminder",
                "It's $currentTime - ${eventType} for ${plant.name}"
            )
        }
    }
}