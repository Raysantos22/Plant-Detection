package Plantdetection

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages the plant database for storing and retrieving information about user's plants
 */
class PlantDatabaseManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PLANT_DATABASE_PREFS, Context.MODE_PRIVATE
    )

    companion object {
        const val PLANT_DATABASE_PREFS = "plant_database_preferences"
        
        // Shared Prefs Keys
        const val KEY_PLANT_IDS = "plant_ids"
        
        // Date formats
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
        val DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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
        
        return editor.commit()
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
        
        return editor.commit()
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
        
        // Remove all care events for this plant
        val careEventIds = getPlantCareEventIds(plantId)
        careEventIds.forEach { eventId ->
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
        if (!sharedPreferences.contains("plant_${event.plantId}_name")) {
            return false
        }
        
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
        
        return editor.commit()
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
        
        return editor.commit()
    }
    
    /**
     * Delete a care event
     */
    fun deletePlantCareEvent(eventId: String): Boolean {
        // Check if event exists
        val plantId = sharedPreferences.getString("care_event_${eventId}_plant_id", null) ?: return false
        
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
        val plant = getPlant(plantId)
        plant?.let {
            val updatedPlant = it.copy(nextWateringDate = wateringDate)
            updatePlant(updatedPlant)
        }
        
        return eventId
    }
    
    /**
     * Mark a care event as completed
     */
    fun markCareEventCompleted(eventId: String): Boolean {
        val event = getPlantCareEvent(eventId) ?: return false
        
        val updatedEvent = event.copy(completed = true)
        val success = updatePlantCareEvent(updatedEvent)
        
        // If this was a watering event, schedule the next one based on frequency
        if (success && event.eventType == "Watering") {
            val plant = getPlant(event.plantId)
            plant?.let {
                // Calculate next watering date
                val calendar = Calendar.getInstance()
                calendar.time = event.date
                calendar.add(Calendar.DAY_OF_MONTH, it.wateringFrequency)
                
                // Schedule next watering
                scheduleWatering(it.id, calendar.time, "Regular watering")
            }
        }
        
        return success
    }
    
    /**
     * Get all care event IDs for a plant
     */
    private fun getPlantCareEventIds(plantId: String): Set<String> {
        return sharedPreferences.getStringSet("plant_${plantId}_care_events", HashSet()) ?: HashSet()
    }
}