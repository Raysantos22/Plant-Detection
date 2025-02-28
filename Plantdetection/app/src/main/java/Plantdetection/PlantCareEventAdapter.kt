package Plantdetection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.PlantDetection.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying plant care events in a RecyclerView
 */
class PlantCareEventAdapter(
    private var events: List<PlantDatabaseManager.PlantCareEvent>,
    private val onItemClick: (PlantDatabaseManager.PlantCareEvent) -> Unit,
    private val onCheckboxClick: (PlantDatabaseManager.PlantCareEvent) -> Unit
) : RecyclerView.Adapter<PlantCareEventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant_care_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<PlantDatabaseManager.PlantCareEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventTypeIcon: ImageView = itemView.findViewById(R.id.eventTypeIcon)
        private val eventTime: TextView = itemView.findViewById(R.id.eventTime)
        private val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        private val eventDescription: TextView = itemView.findViewById(R.id.eventDescription)
        private val eventCompleteCheckbox: CheckBox = itemView.findViewById(R.id.eventCompleteCheckbox)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(events[position])
                }
            }

            eventCompleteCheckbox.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (!events[position].completed) {
                        onCheckboxClick(events[position])
                    } else {
                        // If already completed, prevent unchecking
                        eventCompleteCheckbox.isChecked = true
                    }
                }
            }
        }

        fun bind(event: PlantDatabaseManager.PlantCareEvent) {
            // Set icon based on event type
            val iconRes = when (event.eventType.lowercase()) {
                "watering" -> R.drawable.ic_watering
                "treatment" -> R.drawable.ic_treatment
                "scan" -> R.drawable.ic_scan
                else -> R.drawable.ic_plant_care
            }
            eventTypeIcon.setImageResource(iconRes)
            
            // Format time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            eventTime.text = timeFormat.format(event.date)
            
            // Set title based on event type and plant
            val plantDatabaseManager = PlantDatabaseManager(itemView.context)
            val plant = plantDatabaseManager.getPlant(event.plantId)
            val plantName = plant?.name ?: "Unknown plant"
            
            eventTitle.text = when (event.eventType.lowercase()) {
                "watering" -> "Water $plantName"
                "treatment" -> "Treat $plantName"
                "scan" -> "Scan $plantName"
                else -> "${event.eventType} for $plantName"
            }
            
            // Set description
            val description = if (event.conditionName != null) {
                "For ${event.conditionName}"
            } else if (event.notes.isNotEmpty()) {
                event.notes
            } else {
                "No additional details"
            }
            eventDescription.text = description
            
            // Set checkbox state
            eventCompleteCheckbox.isChecked = event.completed
            eventCompleteCheckbox.isEnabled = !event.completed
            
            // Apply completed style if needed
            if (event.completed) {
                eventTitle.alpha = 0.6f
                eventDescription.alpha = 0.6f
            } else {
                eventTitle.alpha = 1.0f
                eventDescription.alpha = 1.0f
            }
        }
    }
}