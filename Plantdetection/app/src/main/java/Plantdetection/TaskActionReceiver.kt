package Plantdetection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.PlantDetection.PlantDatabaseManager

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.PlantDetection.ACTION_MARK_TASK_COMPLETE" -> {
                val eventId = intent.getStringExtra(EnhancedPlantNotificationService.EXTRA_EVENT_ID) ?: return
                val plantId = intent.getStringExtra(EnhancedPlantNotificationService.EXTRA_PLANT_ID) ?: return

                // Get database manager instance
                val plantDatabaseManager = PlantDatabaseManager(context)

                // Mark the event as completed
                val event = plantDatabaseManager.getPlantCareEvent(eventId)
                event?.let {
                    val completedEvent = it.copy(
                        completed = true,
                        notes = "${it.notes}\n\nMarked completed via notification action."
                    )
                    plantDatabaseManager.updatePlantCareEvent(completedEvent)

                    // Cancel the corresponding failed task notification
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.cancel(eventId.hashCode())

                    // Optional: Show a toast or create a confirmation notification
                    Toast.makeText(
                        context,
                        "Task marked as completed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}