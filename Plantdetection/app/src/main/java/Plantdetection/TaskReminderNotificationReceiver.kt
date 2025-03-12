package Plantdetection

import android.R.attr.action
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.PlantDetection.PlantManagementActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.PlantDetection.R

class TaskReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == EnhancedPlantNotificationService.ACTION_TASK_REMINDER) {
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
                putExtra("SHOW_TASK_REMINDER", true)
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
                        "Water Your Plant",
                        "Time to water $plantName scheduled for $formattedScheduledTime"
                    )
                }
                "treatment" -> {
                    val condition = conditionName ?: "condition"
                    Pair(
                        "Plant Treatment Reminder",
                        "Treatment for $condition on $plantName is due at $formattedScheduledTime"
                    )
                }
                "fertilize" -> {
                    Pair(
                        "Fertilize Your Plant",
                        "Fertilizing task for $plantName scheduled for $formattedScheduledTime"
                    )
                }
                "inspect" -> {
                    Pair(
                        "Plant Inspection Reminder",
                        "Inspection for $plantName is due at $formattedScheduledTime"
                    )
                }
                "prune" -> {
                    Pair(
                        "Pruning Reminder",
                        "Time to prune $plantName scheduled for $formattedScheduledTime"
                    )
                }
                else -> {
                    Pair(
                        "Plant Care Reminder",
                        "Care task for $plantName is due at $formattedScheduledTime"
                    )
                }
            }

            // Create an action to mark task as completed
            val completeIntent = Intent(context, TaskActionReceiver::class.java).apply {
                action = "com.PlantDetection.ACTION_MARK_TASK_COMPLETE"
                putExtra(EnhancedPlantNotificationService.EXTRA_EVENT_ID, eventId)
                putExtra(EnhancedPlantNotificationService.EXTRA_PLANT_ID, plantId)
            }

            val completeActionPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    context,
                    eventId.hashCode() + 1,
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    context,
                    eventId.hashCode() + 1,
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Build notification with actions
            val builder = NotificationCompat.Builder(context, EnhancedPlantNotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_plant_task_reminder)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 250, 110, 250, 110, 250))
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.baseline_done_24,
                    "Mark Completed",
                    completeActionPendingIntent
                )
                .setAutoCancel(true)

            // Schedule a task failed notification for follow-up
            val notificationService = EnhancedPlantNotificationService(context)
            notificationService.scheduleTaskFailedNotification(
                plantId = plantId,
                plantName = plantName,
                eventId = eventId,
                eventType = eventType,
                conditionName = conditionName,
                scheduledTime = Date(scheduledTime)
            )

            // Show notification
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(eventId.hashCode(), builder.build())
                }
            } catch (e: SecurityException) {
                Log.e("TaskReminderNotification", "Failed to show notification: Permission denied", e)
            }
        }
    }
}
