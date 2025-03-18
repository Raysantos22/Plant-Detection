package com.PlantDetection

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.PlantDetection.PlantNotificationService
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages the plant database for storing and retrieving information about user's plants
 */
class PlantDatabaseManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PLANT_DATABASE_PREFS, Context.MODE_PRIVATE
    )

    // Notification service for scheduling reminders
    private val notificationService = PlantNotificationService(context)

    companion object {
        const val PLANT_DATABASE_PREFS = "plant_database_preferences"

        // Shared Prefs Keys
        const val KEY_PLANT_IDS = "plant_ids"

        // Date formats
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val TIME_FORMAT = SimpleDateFormat("h:mm a", Locale.getDefault()) // Changed to AM/PM format
        val DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val DISPLAY_DATETIME_FORMAT = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) // Added for display
    }

    /**
     * Data class representing a plant in the database
     */
    data class Plant(
        val id: String,
        val name: String,
        val type: String, // "Tomato" or "Eggplant"
        val createdDate: Date,
        val lastScannedDate: Date? = null,
        val currentCondition: String? = null,
        val nextWateringDate: Date? = null,
        val wateringFrequency: Int = 1, // Days between watering
        val notes: String = ""
    )

    /**
     * Data class representing a plant care event
     */
    data class PlantCareEvent(
        val id: String,
        val plantId: String,
        val eventType: String, // "Watering", "Treatment", "Scan", etc.
        val date: Date,
        val conditionName: String? = null,
        val notes: String = "",
        val completed: Boolean = false
    )
    fun PlantDatabaseManager.getPlantCareEventsForDate(
        plantId: String,
        date: Date
    ): List<PlantCareEvent> {
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
    /**
     * Add a new plant to the database
     */
    fun addPlant(plant: Plant): Boolean {
        val editor = sharedPreferences.edit()

        // Store plant data
        editor.putString("plant_${plant.id}_name", plant.name)
        editor.putString("plant_${plant.id}_type", plant.type)
        editor.putString("plant_${plant.id}_created_date", DATE_FORMAT.format(plant.createdDate))

        plant.lastScannedDate?.let {
            editor.putString("plant_${plant.id}_last_scanned", DATETIME_FORMAT.format(it))
        }

        plant.currentCondition?.let {
            editor.putString("plant_${plant.id}_current_condition", it)
        }

        plant.nextWateringDate?.let {
            editor.putString("plant_${plant.id}_next_watering", DATETIME_FORMAT.format(it))
        }

        editor.putInt("plant_${plant.id}_watering_frequency", plant.wateringFrequency)
        editor.putString("plant_${plant.id}_notes", plant.notes)

        // Add to list of plants
        val plantIds = sharedPreferences.getStringSet(KEY_PLANT_IDS, HashSet()) ?: HashSet()
        val updatedPlantIds = HashSet(plantIds)
        updatedPlantIds.add(plant.id)
        editor.putStringSet(KEY_PLANT_IDS, updatedPlantIds)

        val result = editor.commit()

        // Schedule watering notification if next watering date is set
        if (result && plant.nextWateringDate != null) {
            scheduleWateringNotification(plant.id, plant.name, plant.nextWateringDate)
        }

        return result
    }
    fun getMonthEvents(year: Int, month: Int): List<PlantCareEvent> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time

        return getCareEventsInDateRange(startDate, endDate)
    }
    /**
     * Get a plant by ID
     */
    fun getPlant(plantId: String): Plant? {
        if (!sharedPreferences.contains("plant_${plantId}_name")) {
            return null
        }

        val name = sharedPreferences.getString("plant_${plantId}_name", "") ?: ""
        val type = sharedPreferences.getString("plant_${plantId}_type", "") ?: ""

        val createdDateStr = sharedPreferences.getString("plant_${plantId}_created_date", null)
        val createdDate = createdDateStr?.let { DATE_FORMAT.parse(it) } ?: Date()

        val lastScannedStr = sharedPreferences.getString("plant_${plantId}_last_scanned", null)
        val lastScannedDate = lastScannedStr?.let { DATETIME_FORMAT.parse(it) }

        val currentCondition = sharedPreferences.getString("plant_${plantId}_current_condition", null)

        val nextWateringStr = sharedPreferences.getString("plant_${plantId}_next_watering", null)
        val nextWateringDate = nextWateringStr?.let { DATETIME_FORMAT.parse(it) }

        val wateringFrequency = sharedPreferences.getInt("plant_${plantId}_watering_frequency", 1)
        val notes = sharedPreferences.getString("plant_${plantId}_notes", "") ?: ""

        return Plant(
            id = plantId,
            name = name,
            type = type,
            createdDate = createdDate,
            lastScannedDate = lastScannedDate,
            currentCondition = currentCondition,
            nextWateringDate = nextWateringDate,
            wateringFrequency = wateringFrequency,
            notes = notes
        )
    }

    /**
     * Get all plants in the database
     */
    fun getAllPlants(): List<Plant> {
        val plantIds = sharedPreferences.getStringSet(KEY_PLANT_IDS, HashSet()) ?: HashSet()
        return plantIds.mapNotNull { getPlant(it) }
    }

    /**
     * Update plant information
     */
    fun updatePlant(plant: Plant): Boolean {
        // Check if plant exists
        if (!sharedPreferences.contains("plant_${plant.id}_name")) {
            return false
        }

        // Get existing plant to check for changes
        val existingPlant = getPlant(plant.id)

        // Update plant data
        val editor = sharedPreferences.edit()

        editor.putString("plant_${plant.id}_name", plant.name)
        editor.putString("plant_${plant.id}_type", plant.type)

        plant.lastScannedDate?.let {
            editor.putString("plant_${plant.id}_last_scanned", DATETIME_FORMAT.format(it))
        }

        plant.currentCondition?.let {
            editor.putString("plant_${plant.id}_current_condition", it)
        }

        plant.nextWateringDate?.let {
            editor.putString("plant_${plant.id}_next_watering", DATETIME_FORMAT.format(it))
        }

        editor.putInt("plant_${plant.id}_watering_frequency", plant.wateringFrequency)
        editor.putString("plant_${plant.id}_notes", plant.notes)

        val result = editor.commit()

        // If watering date changed, update notification
        if (result && existingPlant != null &&
            (existingPlant.nextWateringDate != plant.nextWateringDate ||
                    existingPlant.name != plant.name)) {

            // If we have a watering date, schedule notification
            if (plant.nextWateringDate != null) {
                scheduleWateringNotification(plant.id, plant.name, plant.nextWateringDate)
            }
        }

        return result
    }

    /**
     * Delete a plant from the database
     */
    fun deletePlant(plantId: String): Boolean {
        // Check if plant exists
        if (!sharedPreferences.contains("plant_${plantId}_name")) {
            return false
        }

        val editor = sharedPreferences.edit()

        // Remove plant data
        editor.remove("plant_${plantId}_name")
        editor.remove("plant_${plantId}_type")
        editor.remove("plant_${plantId}_created_date")
        editor.remove("plant_${plantId}_last_scanned")
        editor.remove("plant_${plantId}_current_condition")
        editor.remove("plant_${plantId}_next_watering")
        editor.remove("plant_${plantId}_watering_frequency")
        editor.remove("plant_${plantId}_notes")

        // Remove from list of plants
        val plantIds = sharedPreferences.getStringSet(KEY_PLANT_IDS, HashSet()) ?: HashSet()
        val updatedPlantIds = HashSet(plantIds)
        updatedPlantIds.remove(plantId)
        editor.putStringSet(KEY_PLANT_IDS, updatedPlantIds)

        // Get all care events for this plant to cancel their notifications
        val careEventIds = getPlantCareEventIds(plantId)
        careEventIds.forEach { eventId ->
            // Cancel notification for this event
            notificationService.cancelNotification(eventId)

            // Remove event data
            editor.remove("care_event_${eventId}_plant_id")
            editor.remove("care_event_${eventId}_type")
            editor.remove("care_event_${eventId}_date")
            editor.remove("care_event_${eventId}_condition")
            editor.remove("care_event_${eventId}_notes")
            editor.remove("care_event_${eventId}_completed")
        }

        editor.remove("plant_${plantId}_care_events")

        return editor.commit()
    }

    /**
     * Add a care event for a plant
     */
    fun addPlantCareEvent(event: PlantCareEvent): Boolean {
        // Check if plant exists
        val plant = getPlant(event.plantId) ?: return false

        val editor = sharedPreferences.edit()

        // Store event data
        editor.putString("care_event_${event.id}_plant_id", event.plantId)
        editor.putString("care_event_${event.id}_type", event.eventType)
        editor.putString("care_event_${event.id}_date", DATETIME_FORMAT.format(event.date))

        event.conditionName?.let {
            editor.putString("care_event_${event.id}_condition", it)
        }

        editor.putString("care_event_${event.id}_notes", event.notes)
        editor.putBoolean("care_event_${event.id}_completed", event.completed)

        // Add to list of care events for this plant
        val careEventIds = sharedPreferences.getStringSet("plant_${event.plantId}_care_events", HashSet()) ?: HashSet()
        val updatedCareEventIds = HashSet(careEventIds)
        updatedCareEventIds.add(event.id)
        editor.putStringSet("plant_${event.plantId}_care_events", updatedCareEventIds)

        val result = editor.commit()

        // Schedule notification for this event if it's in the future and not completed
        if (result && !event.completed && event.date.after(Date())) {
            scheduleEventNotification(
                plantId = event.plantId,
                plantName = plant.name,
                eventId = event.id,
                eventType = event.eventType,
                scheduledTime = event.date,
                conditionName = event.conditionName
            )
        }

        return result
    }

    /**
     * Get all care events for a plant
     */
    fun getPlantCareEvents(plantId: String): List<PlantCareEvent> {
        val careEventIds = getPlantCareEventIds(plantId)
        return careEventIds.mapNotNull { getPlantCareEvent(it) }
    }

    /**
     * Get care event by ID
     */
    fun getPlantCareEvent(eventId: String): PlantCareEvent? {
        val plantId = sharedPreferences.getString("care_event_${eventId}_plant_id", null) ?: return null
        val eventType = sharedPreferences.getString("care_event_${eventId}_type", null) ?: return null
        val dateStr = sharedPreferences.getString("care_event_${eventId}_date", null) ?: return null

        val date = try {
            DATETIME_FORMAT.parse(dateStr) ?: return null
        } catch (e: Exception) {
            return null
        }

        val conditionName = sharedPreferences.getString("care_event_${eventId}_condition", null)
        val notes = sharedPreferences.getString("care_event_${eventId}_notes", "") ?: ""
        val completed = sharedPreferences.getBoolean("care_event_${eventId}_completed", false)

        return PlantCareEvent(
            id = eventId,
            plantId = plantId,
            eventType = eventType,
            date = date,
            conditionName = conditionName,
            notes = notes,
            completed = completed
        )
    }

    /**
     * Update a care event
     */
    fun updatePlantCareEvent(event: PlantCareEvent): Boolean {
        // Check if event exists
        if (!sharedPreferences.contains("care_event_${event.id}_plant_id")) {
            return false
        }

        // Get existing event and plant
        val existingEvent = getPlantCareEvent(event.id)
        val plant = getPlant(event.plantId)

        if (existingEvent == null || plant == null) {
            return false
        }

        val editor = sharedPreferences.edit()

        // Update event data
        editor.putString("care_event_${event.id}_plant_id", event.plantId)
        editor.putString("care_event_${event.id}_type", event.eventType)
        editor.putString("care_event_${event.id}_date", DATETIME_FORMAT.format(event.date))

        event.conditionName?.let {
            editor.putString("care_event_${event.id}_condition", it)
        }

        editor.putString("care_event_${event.id}_notes", event.notes)
        editor.putBoolean("care_event_${event.id}_completed", event.completed)

        val result = editor.commit()

        // If event was completed, cancel notification
        if (result && event.completed && !existingEvent.completed) {
            notificationService.cancelNotification(event.id)

            // If this was a watering event, schedule the next one
            if (event.eventType.equals("watering", ignoreCase = true)) {
                scheduleNextWatering(event.plantId)
            }
        }
        // If date/time changed but not completed, update notification
        else if (result && !event.completed && (
                    existingEvent.date != event.date ||
                            existingEvent.eventType != event.eventType ||
                            existingEvent.conditionName != event.conditionName)) {

            // Cancel the old notification
            notificationService.cancelNotification(event.id)

            // Schedule a new one if it's in the future
            if (event.date.after(Date())) {
                scheduleEventNotification(
                    plantId = event.plantId,
                    plantName = plant.name,
                    eventId = event.id,
                    eventType = event.eventType,
                    scheduledTime = event.date,
                    conditionName = event.conditionName
                )
            }
        }

        return result
    }

    /**
     * Delete a care event
     */
    fun deletePlantCareEvent(eventId: String): Boolean {
        // Check if event exists
        val plantId = sharedPreferences.getString("care_event_${eventId}_plant_id", null) ?: return false

        // Cancel any notification for this event
        try {
            cancelNotification(eventId)
        } catch (e: Exception) {
            Log.e("PlantDatabaseManager", "Error while canceling notification for event $eventId: ${e.message}")
        }

        val editor = sharedPreferences.edit()

        // Remove event data
        editor.remove("care_event_${eventId}_plant_id")
        editor.remove("care_event_${eventId}_type")
        editor.remove("care_event_${eventId}_date")
        editor.remove("care_event_${eventId}_condition")
        editor.remove("care_event_${eventId}_notes")
        editor.remove("care_event_${eventId}_completed")

        // Remove from list of care events for this plant
        val careEventIds = sharedPreferences.getStringSet("plant_${plantId}_care_events", HashSet()) ?: HashSet()
        val updatedCareEventIds = HashSet(careEventIds)
        updatedCareEventIds.remove(eventId)
        editor.putStringSet("plant_${plantId}_care_events", updatedCareEventIds)

        return editor.commit()
    }

    /**
     * Get all care events for all plants in a given date range
     */
    fun getCareEventsInDateRange(startDate: Date, endDate: Date): List<PlantCareEvent> {
        val allPlants = getAllPlants()
        val allEvents = mutableListOf<PlantCareEvent>()

        for (plant in allPlants) {
            val plantEvents = getPlantCareEvents(plant.id)
            allEvents.addAll(plantEvents.filter { event ->
                event.date >= startDate && event.date <= endDate
            })
        }

        return allEvents.sortedBy { it.date }
    }

    /**
     * Get all care events for today
     */
    fun getTodaysCareEvents(): List<PlantCareEvent> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        return getCareEventsInDateRange(startDate, endDate)
    }

    /**
     * Schedule a watering event for a plant
     */
    fun scheduleWatering(plantId: String, wateringDate: Date, notes: String = ""): String {
        val plant = getPlant(plantId) ?: return ""

        // Check if there's already a watering event scheduled for this date
        val calendar = Calendar.getInstance()
        calendar.time = wateringDate
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

        val existingEvents = getPlantCareEvents(plantId)
            .filter {
                it.eventType.equals("Watering", ignoreCase = true) &&
                        !it.completed &&
                        it.date >= startOfDay && it.date <= endOfDay
            }

        // If there's already a watering event for this day, return its ID
        if (existingEvents.isNotEmpty()) {
            return existingEvents.first().id
        }

        // Otherwise create a new watering event
        val eventId = "watering_${plantId}_${System.currentTimeMillis()}"

        val wateringEvent = PlantCareEvent(
            id = eventId,
            plantId = plantId,
            eventType = "Watering",
            date = wateringDate,
            notes = notes,
            completed = false
        )

        addPlantCareEvent(wateringEvent)

        // Update plant's next watering date
        val updatedPlant = plant.copy(nextWateringDate = wateringDate)
        updatePlant(updatedPlant)

        return eventId
    }


    /**
     * Mark a care event as completed
     */
    fun markCareEventCompleted(eventId: String): Boolean {
        val event = getPlantCareEvent(eventId) ?: return false

        // If already completed, return true without doing anything
        if (event.completed) {
            return true
        }

        val updatedEvent = event.copy(completed = true)
        val success = updatePlantCareEvent(updatedEvent)

        // If this was a watering event, schedule the next one
        if (success && event.eventType.equals("watering", ignoreCase = true)) {
            scheduleNextWatering(event.plantId)
        }

        return success
    }

    /**
     * Schedule the next watering based on frequency
     */
// In the PlantDatabaseManager class, update the scheduleNextWatering method
    fun scheduleNextWatering(plantId: String): String {
        val plant = getPlant(plantId) ?: return ""

        // Check if there are any pending watering events in the future
        val futureWateringEvents = getPlantCareEvents(plantId)
            .filter {
                it.eventType.equals("watering", ignoreCase = true) &&
                        !it.completed &&
                        it.date.after(Date())
            }
            .sortedBy { it.date }

        // If there are already future watering events, don't schedule another one
        if (futureWateringEvents.isNotEmpty()) {
            Log.d("PlantDatabaseManager", "Skipping new watering event - future events exist")
            return futureWateringEvents.first().id
        }

        // Calculate next watering date
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, plant.wateringFrequency)

        // Get appropriate time (morning around 9 AM)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        // Update plant's next watering date
        val updatedPlant = plant.copy(nextWateringDate = calendar.time)
        updatePlant(updatedPlant)

        // Schedule next watering
        val eventId = scheduleWatering(plantId, calendar.time, "Regular watering")

        // Format time with AM/PM
        val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        Log.d("PlantDatabaseManager", "Scheduled next watering for ${dateFormat.format(calendar.time)}")

        return eventId
    }
    fun cancelNotification(eventId: String) {
        try {
            val notificationService = PlantNotificationService(context)
            notificationService.cancelNotification(eventId)
        } catch (e: Exception) {
            Log.e("PlantDatabaseManager", "Error canceling notification: ${e.message}")
        }
    }

    fun toggleEventCompletion(eventId: String): Boolean {
        val event = getPlantCareEvent(eventId) ?: return false

        // Create updated event with toggled completion status
        val updatedEvent = event.copy(completed = !event.completed)

        // If we're marking as completed and it's a watering event, schedule next watering
        if (!event.completed && updatedEvent.completed &&
            event.eventType.equals("watering", ignoreCase = true)) {
            scheduleNextWatering(event.plantId)
        }

        // Update event
        return updatePlantCareEvent(updatedEvent)
    }

    /**
     * Schedule a watering notification
     */
    private fun scheduleWateringNotification(plantId: String, plantName: String, wateringDate: Date) {
        val eventId = "watering_${plantId}_${wateringDate.time}"

        // Cancel any existing notification with this ID
        notificationService.cancelNotification(eventId)

        // Only schedule if the date is in the future
        if (wateringDate.after(Date())) {
            // Schedule notification for 30 minutes before watering time
            val notificationTime = Calendar.getInstance()
            notificationTime.time = wateringDate
            notificationTime.add(Calendar.MINUTE, -30)

            notificationService.scheduleNotification(
                plantId = plantId,
                plantName = plantName,
                eventId = eventId,
                eventType = "Watering",
                scheduledTime = notificationTime.time
            )
        }
    }

    /**
     * Schedule a notification for any care event
     */
    private fun scheduleEventNotification(
        plantId: String,
        plantName: String,
        eventId: String,
        eventType: String,
        scheduledTime: Date,
        conditionName: String? = null
    ) {
        // Cancel any existing notification with this ID
        notificationService.cancelNotification(eventId)

        // Only schedule if the date is in the future
        if (scheduledTime.after(Date())) {
            // Schedule notification for 30 minutes before event time
            val notificationTime = Calendar.getInstance()
            notificationTime.time = scheduledTime
            notificationTime.add(Calendar.MINUTE, -30)

            notificationService.scheduleNotification(
                plantId = plantId,
                plantName = plantName,
                eventId = eventId,
                eventType = eventType,
                scheduledTime = notificationTime.time,
                conditionName = conditionName
            )
        }
    }

    /**
     * Get all care event IDs for a plant
     */
    fun getPlantCareEventIds(plantId: String): Set<String> {
        return sharedPreferences.getStringSet("plant_${plantId}_care_events", HashSet()) ?: HashSet()
    }

    /**
     * Add a detection result directly as a plant with scheduling
     */
    fun addDetectionAsPlant(
        plantName: String,
        vegetableType: String,
        conditionName: String
    ): String {
        // Create a new plant ID
        val plantId = "plant_${System.currentTimeMillis()}"

        // Create the plant
        val plant = Plant(
            id = plantId,
            name = plantName,
            type = vegetableType,
            createdDate = Date(),
            lastScannedDate = Date(),
            currentCondition = conditionName,
            wateringFrequency = 2 // Default to every 2 days
        )

        // Add plant to database
        if (addPlant(plant)) {
            // Schedule first watering for tomorrow morning
            val wateringDate = Calendar.getInstance()
            wateringDate.add(Calendar.DAY_OF_MONTH, 1)
            wateringDate.set(Calendar.HOUR_OF_DAY, 9)
            wateringDate.set(Calendar.MINUTE, 0)
            wateringDate.set(Calendar.SECOND, 0)

            scheduleWatering(plantId, wateringDate.time, "Initial watering")

            // If condition is not healthy, create detailed treatment events
            if (!conditionName.startsWith("Healthy")) {
                // Get the condition data to create appropriate treatment tasks
                val condition = PlantConditionData.conditions[conditionName]

                if (condition != null) {
                    // Create a treatment plan title
                    val treatmentTitle = "Treatment Plan for ${condition.name}"

                    // If we have detailed treatment tasks, create individual events for each
                    if (condition.treatmentTasks.isNotEmpty()) {
                        for ((index, task) in condition.treatmentTasks.withIndex()) {
                            // Set appropriate timing for the task
                            val treatmentDate = Calendar.getInstance()
                            treatmentDate.add(Calendar.HOUR_OF_DAY, 4 + index) // Stagger tasks

                            // Create detailed treatment notes
                            val taskNotes = "${treatmentTitle}\n\n" +
                                    "${task.taskName}: ${task.description}\n\n" +
                                    "Materials needed:\n" +
                                    task.materials.joinToString("\n", "• ") + "\n\n" +
                                    "Instructions:\n" +
                                    task.instructions.joinToString("\n", "• ")

                            val taskId = "treatment_${plantId}_${System.currentTimeMillis() + index}"
                            val treatmentEvent = PlantCareEvent(
                                id = taskId,
                                plantId = plantId,
                                eventType = "Treat: ${condition.name}",
                                date = treatmentDate.time,
                                conditionName = conditionName,
                                notes = taskNotes,
                                completed = false
                            )

                            addPlantCareEvent(treatmentEvent)

                            // Schedule follow-up tasks if needed
                            if (task.scheduleInterval > 0) {
                                val followUpCalendar = Calendar.getInstance()
                                followUpCalendar.time = treatmentDate.time

                                for (followUpIndex in 1..2) { // Create 2 follow-ups
                                    followUpCalendar.add(Calendar.DAY_OF_MONTH, task.scheduleInterval)

                                    val followUpId = "followup_${plantId}_${System.currentTimeMillis() + index + followUpIndex * 100}"
                                    val followUpNotes = "${treatmentTitle}\n\n" +
                                            "Follow-up #$followUpIndex: ${task.taskName}\n\n" +
                                            "${task.description}\n\n" +
                                            "Materials needed:\n" +
                                            task.materials.joinToString("\n", "• ") + "\n\n" +
                                            "Instructions:\n" +
                                            task.instructions.joinToString("\n", "• ")

                                    val followUpEvent = PlantCareEvent(
                                        id = followUpId,
                                        plantId = plantId,
                                        eventType = "Treat: ${condition.name}",
                                        date = followUpCalendar.time,
                                        conditionName = conditionName,
                                        notes = followUpNotes,
                                        completed = false
                                    )

                                    addPlantCareEvent(followUpEvent)
                                }
                            }
                        }
                    } else {
                        // Fall back to general treatment tips if no detailed tasks
                        val treatmentDate = Calendar.getInstance()
                        treatmentDate.add(Calendar.HOUR_OF_DAY, 4)

                        val generalTreatmentNotes = "${treatmentTitle}\n\n" +
                                "Recommended treatments:\n" +
                                condition.treatmentTips.joinToString("\n", "• ")

                        val treatmentId = "treatment_${plantId}_${System.currentTimeMillis()}"
                        val treatmentEvent = PlantCareEvent(
                            id = treatmentId,
                            plantId = plantId,
                            eventType = "Treat: ${condition.name}",
                            date = treatmentDate.time,
                            conditionName = conditionName,
                            notes = generalTreatmentNotes,
                            completed = false
                        )

                        addPlantCareEvent(treatmentEvent)
                    }
                } else {
                    // Fallback for unknown conditions
                    val treatmentDate = Calendar.getInstance()
                    treatmentDate.add(Calendar.HOUR_OF_DAY, 4)

                    val treatmentId = "treatment_${plantId}_${System.currentTimeMillis()}"
                    val treatmentEvent = PlantCareEvent(
                        id = treatmentId,
                        plantId = plantId,
                        eventType = "Treatment",
                        date = treatmentDate.time,
                        conditionName = conditionName,
                        notes = "Treatment needed for $conditionName. Consider consulting a plant expert for specific treatment options.",
                        completed = false
                    )

                    addPlantCareEvent(treatmentEvent)
                }
            }

            return plantId
        }

        return ""
    }
    /**
     * Test method to show a notification immediately
     */
    fun testNotification(plantId: String) {
        val plant = getPlant(plantId) ?: return
        notificationService.showNotificationNow(
            plantId = plantId,
            plantName = plant.name,
            eventType = "Watering"
        )
    }
}