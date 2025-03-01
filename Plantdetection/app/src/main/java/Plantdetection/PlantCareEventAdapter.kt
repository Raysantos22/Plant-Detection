package com.PlantDetection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.PlantDetection.PlantDatabaseManager
import com.PlantDetection.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying plant care events in a RecyclerView
 */
class PlantCareEventAdapter(
    private var events: List<PlantDatabaseManager.PlantCareEvent>,
    private val onItemClick: (PlantDatabaseManager.PlantCareEvent) -> Unit,
    private val onCheckboxClick: (PlantDatabaseManager.PlantCareEvent) -> Unit,
    private val onRescheduleClick: ((PlantDatabaseManager.PlantCareEvent) -> Unit)? = null,
    private val onRescanClick: ((PlantDatabaseManager.PlantCareEvent, String) -> Unit)? = null,
    private val onViewScheduleClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<PlantCareEventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_care_event_enhanced, parent, false)
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
        private val eventCompleteCheckbox: CheckBox =
            itemView.findViewById(R.id.eventCompleteCheckbox)
        private val eventDateText: TextView = itemView.findViewById(R.id.eventDateText)
        private val rescanButton: View? = itemView.findViewById(R.id.rescanButton)
        private val rescheduleButton: View? = itemView.findViewById(R.id.rescheduleButton)
        private val viewScheduleButton: View? = itemView.findViewById(R.id.viewScheduleButton)

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
                    val event = events[position]

                    // Only allow checking today's or past events
                    val today = getTodayStart()
                    val eventDay = getDayStart(event.date)

                    if (eventDay.after(today)) {
                        // Future event - can't complete it
                        eventCompleteCheckbox.isChecked = event.completed
                        return@setOnClickListener
                    }

                    // Toggle completed state for past or today's events
                    val isNowChecked = eventCompleteCheckbox.isChecked

                    // Only call handler if it's a change
                    if (isNowChecked != event.completed) {
                        onCheckboxClick(event)
                    }
                }
            }

            // Set up rescan button if it exists
            rescanButton?.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val event = events[position]

                    // Get plant database to retrieve plant type
                    val plantDatabaseManager = PlantDatabaseManager(itemView.context)
                    val plant = plantDatabaseManager.getPlant(event.plantId)

                    if (plant != null) {
                        onRescanClick?.invoke(event, plant.type)
                    }
                }
            }

            // Set up reschedule button if it exists
            rescheduleButton?.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRescheduleClick?.invoke(events[position])
                }
            }

            // Set up view schedule button if it exists
            viewScheduleButton?.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val event = events[position]
                    onViewScheduleClick?.invoke(event.plantId)
                }
            }
        }

        private fun getTodayStart(): Date {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }

        private fun getDayStart(date: Date): Date {
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }

        fun bind(event: PlantDatabaseManager.PlantCareEvent) {
            // Set icon based on event type
            val iconRes = when {
                event.eventType.startsWith("Treat: ") -> R.drawable.ic_treatment
                event.eventType.lowercase() == "watering" -> R.drawable.ic_watering
                event.eventType.lowercase() == "treatment" -> R.drawable.ic_treatment
                event.eventType.lowercase() == "scan" -> R.drawable.ic_scan
                event.eventType.lowercase() == "fertilize" -> R.drawable.ic_plant_care
                event.eventType.lowercase() == "prune" -> R.drawable.ic_plant_care
                event.eventType.lowercase() == "pesticide" -> R.drawable.ic_treatment
                event.eventType.lowercase() == "fungicide" -> R.drawable.ic_treatment
                else -> R.drawable.ic_plant_care
            }
            eventTypeIcon.setImageResource(iconRes)

            // Format time
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            eventTime.text = timeFormat.format(event.date)

            // Show date if this isn't today
            val today = getTodayStart()
            val eventDay = getDayStart(event.date)
            if (eventDay.time != today.time) {
                eventDateText.visibility = View.VISIBLE
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                eventDateText.text = dateFormat.format(event.date)
            } else {
                eventDateText.visibility = View.GONE
            }

            // Get plant name
            val plantDatabaseManager = PlantDatabaseManager(itemView.context)
            val plant = plantDatabaseManager.getPlant(event.plantId)
            val plantName = plant?.name ?: "Unknown plant"

            // Set title based on event type and plant - with more descriptive treatment names
            if (event.eventType.startsWith("Treat: ")) {
                // Extract condition name and check if there's a specific task in the notes
                val conditionName = event.eventType.substringAfter("Treat: ")

                // Look for task name in notes (typically in format "TaskName: Description")
                val taskName =
                    event.notes.split("\n\n").getOrNull(1)?.split(":")?.getOrNull(0)?.trim()
                        ?: "Treatment"

                // Create a more descriptive title that shows what action to take
                eventTitle.text = "$taskName for $plantName"

                // Add condition info to description
                eventDescription.text = "For ${conditionName}"
            } else {
                // For non-treatment events, use standard formatting
                eventTitle.text = when (event.eventType.lowercase()) {
                    "watering" -> "Water $plantName"
                    "treatment" -> "Treat $plantName"
                    "scan" -> "Scan $plantName"
                    "fertilize" -> "Fertilize $plantName"
                    "prune" -> "Prune $plantName"
                    "pesticide" -> "Apply pesticide to $plantName"
                    "fungicide" -> "Apply fungicide to $plantName"
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
            }

            // Set checkbox state (without triggering listener)
            eventCompleteCheckbox.setOnCheckedChangeListener(null)
            eventCompleteCheckbox.isChecked = event.completed

            // Only enable checkbox for past or today's events
            val isTodayOrPast = !eventDay.after(today)
            eventCompleteCheckbox.isEnabled = isTodayOrPast

            // Apply completed style if needed
            if (event.completed) {
                eventTitle.alpha = 0.6f
                eventDescription.alpha = 0.6f
            } else {
                eventTitle.alpha = 1.0f
                eventDescription.alpha = 1.0f
            }

            // Apply future style if needed
            if (!isTodayOrPast) {
                eventTitle.alpha = 0.8f
                eventDescription.alpha = 0.8f
                itemView.setBackgroundResource(R.drawable.future_event_background)
            } else {
                itemView.setBackgroundResource(R.drawable.current_event_background)
            }

            // Show rescan button only for treatment events that are in the future or today AND not completed
            if (rescanButton != null) {
                val showRescan =
                    (event.eventType.startsWith("Treat: ") || event.eventType.lowercase() == "treatment") &&
                            (eventDay.time == today.time || eventDay.after(today)) &&
                            !event.completed

                rescanButton.visibility = if (showRescan) View.VISIBLE else View.GONE
            }

            // Show reschedule button only for future events
            if (rescheduleButton != null) {
                rescheduleButton.visibility =
                    if (eventDay.after(today) && !event.completed) View.VISIBLE else View.GONE
            }

            // Always show view schedule button
            viewScheduleButton?.visibility = View.VISIBLE
        }
    }
}