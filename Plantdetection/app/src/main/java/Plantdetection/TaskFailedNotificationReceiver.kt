package Plantdetection

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.PlantDetection.R

class TaskFailedNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == EnhancedPlantNotificationService.ACTION_TASK_FAILED) {
            val plantId = intent.getStringExtra(EnhancedPlantNotificationService.EXTRA_PLANT_ID) ?: return
            val plantName = intent.getStringExtra(EnhancedPlantNotificationService.EXTRA_PLANT_NAME) ?: "Your plant"
            val eventId = intent.getStringExtra(EnhancedPlantNotificationService.EXTRA_EVENT_ID) ?: return
            val eventType = intent.getStringExtra(EnhancedPlantNotificationService.EXTRA_EVENT_TYPE) ?: "care"
            val conditionName = intent.getStringExtra(EnhancedPlantNotificationService.EXTRA_CONDITION_NAME)
            val scheduledTime = intent.getLongExtra(EnhancedPlantNotificationService.EXTRA_SCHEDULED_TIME, 0)

            // Format the scheduled time
            val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            val formattedScheduledTime = dateFormat.format(Date(scheduledTime))

            // Create intent to open plant management
            val activityIntent = Intent(context, PlantManagementActivity::class.java).apply {
                putExtra("OPEN_PLANT_ID", plantId)
                putExtra("SHOW_FAILED_TASK", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    eventId.hashCode(),
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    eventId.hashCode(),
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Determine notification content based on event type
            val (title, content) = when (eventType.lowercase()) {
                "watering" -> {
                    Pair(
                        "Watering Task Failed",
                        "Your $plantName missed its watering on $formattedScheduledTime. Please check and water the plant."
                    )
                }
                "treatment" -> {
                    val condition = conditionName ?: "condition"
                    Pair(
                        "Treatment Task Failed",
                        "Treatment for $condition on $plantName (scheduled for $formattedScheduledTime) was not completed. Proceed to next treatment step."
                    )
                }
                "fertilize" -> {
                    Pair(
                        "Fertilizing Task Failed",
                        "Fertilizing task for $plantName on $formattedScheduledTime was not completed. Schedule a new fertilizing session."
                    )
                }
                else -> {
                    Pair(
                        "Plant Care Task Failed",
                        "A care task for $plantName on $formattedScheduledTime was not completed. Review and reschedule."
                    )
                }
            }

            // Build notification
            val builder = NotificationCompat.Builder(context, EnhancedPlantNotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_task_failed)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 500, 110, 500, 110, 450))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Show notification
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(eventId.hashCode(), builder.build())
                }
            } catch (e: SecurityException) {
                Log.e("TaskFailedNotification", "Failed to show notification: Permission denied", e)
            }

            // Update plant care event status or create a new event for the next step
            updatePlantCareEventStatus(context, plantId, eventId, eventType, conditionName)
        }
    }

    /**
     * Update plant care event status or create a new event
     */
    private fun updatePlantCareEventStatus(
        context: Context,
        plantId: String,
        eventId: String,
        eventType: String,
        conditionName: String?
    ) {
        // Get database manager instance
        val plantDatabaseManager = PlantDatabaseManager(context)

        // Mark the current event as failed
        val currentEvent = plantDatabaseManager.getPlantCareEvent(eventId)
        currentEvent?.let {
            val failedEvent = it.copy(
                completed = false,
                notes = "${it.notes}\n\nFAILED: Task not completed on time."
            )
            plantDatabaseManager.updatePlantCareEvent(failedEvent)
        }

        // Create a new event for the next step based on the event type
        val newEventId = "followup_${plantId}_${System.currentTimeMillis()}"
        val nextEventTime = Calendar.getInstance().apply {
            // Schedule for next day or appropriate interval
            add(Calendar.DAY_OF_MONTH, 1)
            when (eventType.lowercase()) {
                "watering" -> set(Calendar.HOUR_OF_DAY, 9) // Morning watering
                "treatment" -> set(Calendar.HOUR_OF_DAY, 10) // Mid-morning treatment
                "fertilize" -> set(Calendar.HOUR_OF_DAY, 11) // Late morning fertilizing
                else -> set(Calendar.HOUR_OF_DAY, 14) // Afternoon for other tasks
            }
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        val newEvent = PlantDatabaseManager.PlantCareEvent(
            id = newEventId,
            plantId = plantId,
            eventType = "Followup: $eventType",
            date = nextEventTime,
            conditionName = conditionName,
            notes = "Rescheduled after previous task failed. Follow up on ${conditionName ?: "plant care"}.",
            completed = false
        )

        // Add the new event
        plantDatabaseManager.addPlantCareEvent(newEvent)

        // Schedule a notification for the new event
        val notificationService = EnhancedPlantNotificationService(context)
        val plant = plantDatabaseManager.getPlant(plantId)
        plant?.let {
            notificationService.scheduleTaskReminderNotification(
                plantId = plantId,
                plantName = it.name,
                eventId = newEventId,
                eventType = "Followup: $eventType",
                conditionName = conditionName,
                scheduledTime = nextEventTime
            )
        }
    }
}