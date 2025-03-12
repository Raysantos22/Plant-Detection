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
        private val eventCompleteCheckbox: CheckBox = itemView.findViewById(R.id.eventCompleteCheckbox)
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

            // Get plant info
            val plantDatabase = PlantDatabaseManager(itemView.context)
            val plant = plantDatabase.getPlant(event.plantId)
            val plantName = plant?.name ?: "Unknown plant"
            val isPlantGroup = plantName.contains("(") && plantName.contains("plants")

            // For plant groups, provide clearer information
            if (isPlantGroup && plant != null) {
                // Parse plant group details from notes
                var targetPlantCount = 0
                var totalPlantsInGroup = 0
                var plantCountText = ""

                // Extract total plants in group from the plant name
                val match = "\\((\\d+)\\s+plants".toRegex().find(plantName)
                if (match != null) {
                    totalPlantsInGroup = match.groupValues[1].toIntOrNull() ?: 0
                }

                // If we have a condition, determine how many plants have this condition
                if (event.conditionName != null) {
                    val notesLines = plant.notes.split("\n")
                    for (line in notesLines) {
                        if (line.trim().startsWith("-") &&
                            line.contains(event.conditionName, ignoreCase = true) &&
                            line.contains("plants")) {
                            val countPart = line.substringAfter(":").trim()
                            targetPlantCount = countPart.substringBefore(" ").toIntOrNull() ?: 0

                            // Format plant count text
                            plantCountText = if (targetPlantCount == 1) {
                                "(1 plant)"
                            } else {
                                "($targetPlantCount plants)"
                            }

                            break
                        }
                    }
                }

                // Set title and description based on event type
                when {
                    // Watering events apply to all plants
                    event.eventType.equals("watering", ignoreCase = true) -> {
                        eventTitle.text = "Water ${plant.name}"
                        eventDescription.text = "Regular watering for all plants in group"
                    }

                    // Treatment events should clearly indicate which plants they apply to
                    event.eventType.startsWith("Treat: ") || event.eventType.equals("treatment", ignoreCase = true) -> {
                        val conditionName = event.conditionName ?:
                        (if (event.eventType.startsWith("Treat: "))
                            event.eventType.substringAfter("Treat: ")
                        else "condition")

                        // Extract task name if possible
                        val taskName = if (event.notes.contains(":")) {
                            event.notes.split("\n\n").getOrNull(1)?.split(":")?.getOrNull(0)?.trim() ?: "Treatment"
                        } else "Treatment"

                        // Set title with plant count
                        if (plantCountText.isNotEmpty()) {
                            eventTitle.text = "$taskName for $conditionName $plantCountText"
                        } else {
                            eventTitle.text = "$taskName for $conditionName"
                        }

                        // Description includes plant name for context
                        eventDescription.text = "For plants in ${plant.name}"
                    }

                    // Scan events
                    event.eventType.equals("scan", ignoreCase = true) -> {
                        if (event.conditionName != null && plantCountText.isNotEmpty()) {
                            eventTitle.text = "Scan for ${event.conditionName} $plantCountText"
                            eventDescription.text = "In ${plant.name}"
                        } else {
                            eventTitle.text = "Scan all plants in ${plant.name}"
                            eventDescription.text = event.notes.takeIf { it.isNotEmpty() } ?: "Regular scan"
                        }
                    }

                    // Generic fertilizing, pruning, etc.
                    else -> {
                        // If maintenance task, apply to all plants
                        val isMaintenanceTask = event.eventType.lowercase() in listOf("fertilize", "prune", "check")
                        if (isMaintenanceTask) {
                            eventTitle.text = "${event.eventType} all plants in ${plant.name}"
                            eventDescription.text = "Regular maintenance"
                        } else if (event.conditionName != null && plantCountText.isNotEmpty()) {
                            eventTitle.text = "${event.eventType} for ${event.conditionName} $plantCountText"
                            eventDescription.text = "In ${plant.name}"
                        } else {
                            eventTitle.text = "${event.eventType} for ${plant.name}"
                            eventDescription.text = event.notes.takeIf { it.isNotEmpty() } ?: "No details"
                        }
                    }
                }
            } else {
                // Single plant logic (unchanged)
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

                val description = if (event.conditionName != null) {
                    "For ${event.conditionName}"
                } else if (event.notes.isNotEmpty()) {
                    event.notes
                } else {
                    "No additional details"
                }
                eventDescription.text = description
            }

            // Set checkbox state
            eventCompleteCheckbox.setOnCheckedChangeListener(null)
            eventCompleteCheckbox.isChecked = event.completed

            // Only enable checkbox for past or today's events
            val isTodayOrPast = !eventDay.after(today)
            eventCompleteCheckbox.isEnabled = isTodayOrPast

            // Apply styles based on completion status
            if (event.completed) {
                eventTitle.alpha = 0.6f
                eventDescription.alpha = 0.6f
            } else {
                eventTitle.alpha = 1.0f
                eventDescription.alpha = 1.0f
            }

            // Apply future event style if needed
            if (!isTodayOrPast) {
                eventTitle.alpha = 0.8f
                eventDescription.alpha = 0.8f
                itemView.setBackgroundResource(R.drawable.future_event_background)
            } else {
                itemView.setBackgroundResource(R.drawable.current_event_background)
            }

            // Hide rescan button as requested
            rescanButton?.visibility = View.GONE

            // Show reschedule button only for future events
            if (rescheduleButton != null) {
                rescheduleButton.visibility =
                    if (eventDay.after(today) && !event.completed) View.VISIBLE else View.GONE
            }

            // Hide view schedule button as requested
            viewScheduleButton?.visibility = View.GONE
        }
    }
}