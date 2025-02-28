package Plantdetection

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.PlantDetection.R
import com.PlantDetection.VegetableSelectionActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for managing plants and viewing the calendar of care events
 */
class PlantManagementActivity : AppCompatActivity() {

    private lateinit var plantDatabaseManager: PlantDatabaseManager
    private lateinit var calendarView: android.widget.CalendarView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var plantsRecyclerView: RecyclerView
    private lateinit var addPlantButton: FloatingActionButton
    private lateinit var todayEventsContainer: LinearLayout
    private lateinit var selectedDateHeader: TextView
    private lateinit var noPlantsMessage: TextView
    private lateinit var noEventsMessage: TextView
    
    private lateinit var selectedDate: Date
    private var selectedPlantId: String? = null
    
    private val plantAdapter by lazy {
        PlantAdapter(
            plants = plantDatabaseManager.getAllPlants(),
            onItemClick = { plant -> onPlantSelected(plant) },
            onItemLongClick = { plant -> showPlantOptionsDialog(plant) }
        )
    }
    
    private val eventAdapter by lazy {
        PlantCareEventAdapter(
            events = emptyList(),
            onItemClick = { event -> showEventDetailsDialog(event) },
            onCheckboxClick = { event -> onEventCompleted(event) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_management)
        
        // Initialize plant database manager
        plantDatabaseManager = PlantDatabaseManager(this)
        
        // Initialize views
        initViews()
        
        // Set up calendar
        setupCalendar()
        
        // Set up plants list
        setupPlantsList()
        
        // Set up today's events
        setupTodayEvents()
        
        // Set up listeners
        setupListeners()
    }
    
    private fun initViews() {
        calendarView = findViewById(R.id.calendarView)
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView)
        plantsRecyclerView = findViewById(R.id.plantsRecyclerView)
        addPlantButton = findViewById(R.id.addPlantButton)
        todayEventsContainer = findViewById(R.id.todayEventsContainer)
        selectedDateHeader = findViewById(R.id.selectedDateHeader)
        noPlantsMessage = findViewById(R.id.noPlantsMessage)
        noEventsMessage = findViewById(R.id.noEventsMessage)
    }
    
    private fun setupCalendar() {
        // Set initial selected date to today
        selectedDate = Date()
        updateSelectedDateHeader()
        
        // Set calendar listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            
            updateSelectedDateHeader()
            loadEventsForSelectedDate()
        }
    }
    
    private fun setupPlantsList() {
        plantsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        plantsRecyclerView.adapter = plantAdapter
        
        // Check if plants exist
        if (plantAdapter.itemCount == 0) {
            noPlantsMessage.visibility = View.VISIBLE
            plantsRecyclerView.visibility = View.GONE
        } else {
            noPlantsMessage.visibility = View.GONE
            plantsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun setupTodayEvents() {
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsRecyclerView.adapter = eventAdapter
        
        // Load today's events
        loadEventsForSelectedDate()
    }
    
    private fun setupListeners() {
        // Add plant button
        addPlantButton.setOnClickListener {
            showAddPlantDialog()
        }
    }
    
    private fun loadEventsForSelectedDate() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
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
        
        // Get events for selected date, filtered by selected plant if applicable
        val events = plantDatabaseManager.getCareEventsInDateRange(startDate, endDate)
            .let { eventList ->
                selectedPlantId?.let { plantId ->
                    eventList.filter { it.plantId == plantId }
                } ?: eventList
            }
            .sortedBy { it.date }
        
        // Update events list
        (eventsRecyclerView.adapter as PlantCareEventAdapter).updateEvents(events)
        
        // Show message if no events
        if (events.isEmpty()) {
            noEventsMessage.visibility = View.VISIBLE
            eventsRecyclerView.visibility = View.GONE
        } else {
            noEventsMessage.visibility = View.GONE
            eventsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun updateSelectedDateHeader() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        selectedDateHeader.text = dateFormat.format(selectedDate)
    }
    
    private fun onPlantSelected(plant: PlantDatabaseManager.Plant) {
        // Toggle selection
        selectedPlantId = if (selectedPlantId == plant.id) null else plant.id
        
        // Update adapter to reflect selection state
        plantAdapter.updateSelectedPlantId(selectedPlantId)
        
        // Reload events for selected date filtered by selected plant
        loadEventsForSelectedDate()
        
        // Show plant details if selected
        selectedPlantId?.let {
            showPlantDetailsDialog(plant)
        }
    }
    
    private fun onEventCompleted(event: PlantDatabaseManager.PlantCareEvent) {
        // Mark event as completed
        plantDatabaseManager.markCareEventCompleted(event.id)
        
        // Reload events
        loadEventsForSelectedDate()
        
        // Show confirmation toast
        Toast.makeText(this, "Task marked as complete", Toast.LENGTH_SHORT).show()
    }
    
    private fun showAddPlantDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_plant, null)
        
        val nameInput = dialogView.findViewById<android.widget.EditText>(R.id.plantNameInput)
        val typeSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.plantTypeSpinner)
        val wateringFrequencyInput = dialogView.findViewById<android.widget.EditText>(R.id.wateringFrequencyInput)
        
        AlertDialog.Builder(this)
            .setTitle("Add New Plant")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val type = typeSpinner.selectedItem.toString()
                val wateringFrequencyText = wateringFrequencyInput.text.toString().trim()
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "Plant name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val wateringFrequency = if (wateringFrequencyText.isEmpty()) 1 else wateringFrequencyText.toIntOrNull() ?: 1
                
                // Create plant
                val plantId = "plant_${System.currentTimeMillis()}"
                val plant = PlantDatabaseManager.Plant(
                    id = plantId,
                    name = name,
                    type = type,
                    createdDate = Date(),
                    wateringFrequency = wateringFrequency
                )
                
                // Add to database
                if (plantDatabaseManager.addPlant(plant)) {
                    // Schedule first watering
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)
                    plantDatabaseManager.scheduleWatering(plantId, calendar.time, "Initial watering")
                    
                    // Update plants list
                    refreshPlantsList()
                    
                    Toast.makeText(this, "Plant added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to add plant", Toast.LENGTH_SHORT).show()
                }
                
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPlantDetailsDialog(plant: PlantDatabaseManager.Plant) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_details, null)
        
        val plantNameText = dialogView.findViewById<TextView>(R.id.plantNameText)
        val plantTypeText = dialogView.findViewById<TextView>(R.id.plantTypeText)
        val plantStatusText = dialogView.findViewById<TextView>(R.id.plantStatusText)
        val nextWateringText = dialogView.findViewById<TextView>(R.id.nextWateringText)
        val waterNowButton = dialogView.findViewById<android.widget.Button>(R.id.waterNowButton)
        val scanButton = dialogView.findViewById<android.widget.Button>(R.id.scanButton)
        
        // Set plant details
        plantNameText.text = plant.name
        plantTypeText.text = plant.type
        plantStatusText.text = plant.currentCondition ?: "No recent scan"
        
        // Set next watering date
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        nextWateringText.text = plant.nextWateringDate?.let { 
            "Next watering: ${dateFormat.format(it)}" 
        } ?: "No scheduled watering"
        
        // Set button listeners
        waterNowButton.setOnClickListener {
            // Schedule watering for today
            plantDatabaseManager.scheduleWatering(plant.id, Date(), "Manual watering")
            
            // Reload events
            loadEventsForSelectedDate()
            
            Toast.makeText(this, "Watering scheduled for today", Toast.LENGTH_SHORT).show()
        }
        
        scanButton.setOnClickListener {
            // Open scan activity with this plant's information
            val intent = Intent(this, VegetableSelectionActivity::class.java)
            intent.putExtra("SELECTED_PLANT_ID", plant.id)
            startActivity(intent)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Plant Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun showPlantOptionsDialog(plant: PlantDatabaseManager.Plant) {
        val options = arrayOf("Edit Plant", "Schedule Watering", "Delete Plant")
        
        AlertDialog.Builder(this)
            .setTitle(plant.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPlantDialog(plant)
                    1 -> showScheduleWateringDialog(plant)
                    2 -> confirmDeletePlant(plant)
                }
            }
            .show()
    }
    
    private fun showEditPlantDialog(plant: PlantDatabaseManager.Plant) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_plant, null)
        
        val nameInput = dialogView.findViewById<android.widget.EditText>(R.id.plantNameInput)
        val wateringFrequencyInput = dialogView.findViewById<android.widget.EditText>(R.id.wateringFrequencyInput)
        val notesInput = dialogView.findViewById<android.widget.EditText>(R.id.plantNotesInput)
        
        // Set current values
        nameInput.setText(plant.name)
        wateringFrequencyInput.setText(plant.wateringFrequency.toString())
        notesInput.setText(plant.notes)
        
        AlertDialog.Builder(this)
            .setTitle("Edit Plant")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val wateringFrequencyText = wateringFrequencyInput.text.toString().trim()
                val notes = notesInput.text.toString().trim()
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "Plant name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val wateringFrequency = if (wateringFrequencyText.isEmpty()) 1 else wateringFrequencyText.toIntOrNull() ?: 1
                
                // Update plant
                val updatedPlant = plant.copy(
                    name = name,
                    wateringFrequency = wateringFrequency,
                    notes = notes
                )
                
                if (plantDatabaseManager.updatePlant(updatedPlant)) {
                    // Update plants list
                    refreshPlantsList()
                    
                    Toast.makeText(this, "Plant updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update plant", Toast.LENGTH_SHORT).show()
                }
                
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showScheduleWateringDialog(plant: PlantDatabaseManager.Plant) {
        // Create calendar for date picker
        val calendar = Calendar.getInstance()
        
        // Show date picker
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // Create watering event
                plantDatabaseManager.scheduleWatering(
                    plantId = plant.id,
                    wateringDate = calendar.time,
                    notes = "Scheduled watering"
                )
                
                // Reload events
                loadEventsForSelectedDate()
                
                // Show confirmation
                Toast.makeText(
                    this,
                    "Watering scheduled for ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun confirmDeletePlant(plant: PlantDatabaseManager.Plant) {
        AlertDialog.Builder(this)
            .setTitle("Delete Plant")
            .setMessage("Are you sure you want to delete ${plant.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                if (plantDatabaseManager.deletePlant(plant.id)) {
                    // Clear selection if this plant was selected
                    if (selectedPlantId == plant.id) {
                        selectedPlantId = null
                    }
                    
                    // Update plants list
                    refreshPlantsList()
                    
                    // Reload events
                    loadEventsForSelectedDate()
                    
                    Toast.makeText(this, "Plant deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete plant", Toast.LENGTH_SHORT).show()
                }
                
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEventDetailsDialog(event: PlantDatabaseManager.PlantCareEvent) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_details, null)
        
        val eventTypeText = dialogView.findViewById<TextView>(R.id.eventTypeText)
        val eventDateText = dialogView.findViewById<TextView>(R.id.eventDateText)
        val eventPlantText = dialogView.findViewById<TextView>(R.id.eventPlantText)
        val eventNotesText = dialogView.findViewById<TextView>(R.id.eventNotesText)
        val eventStatusText = dialogView.findViewById<TextView>(R.id.eventStatusText)
        val completeButton = dialogView.findViewById<android.widget.Button>(R.id.completeEventButton)
        val deleteButton = dialogView.findViewById<android.widget.Button>(R.id.deleteEventButton)
        
        // Get plant name
        val plant = plantDatabaseManager.getPlant(event.plantId)
        val plantName = plant?.name ?: "Unknown plant"
        
        // Set event details
        eventTypeText.text = event.eventType
        eventDateText.text = SimpleDateFormat("EEE, MMM d, yyyy - HH:mm", Locale.getDefault()).format(event.date)
        eventPlantText.text = plantName
        eventNotesText.text = if (event.notes.isNotEmpty()) event.notes else "No notes"
        eventStatusText.text = if (event.completed) "Completed" else "Pending"
        
        // Set button states
        completeButton.isEnabled = !event.completed
        
        // Set button listeners
        completeButton.setOnClickListener {
            onEventCompleted(event)
            eventStatusText.text = "Completed"
            completeButton.isEnabled = false
        }
        
        deleteButton.setOnClickListener {
            // Delete event
            if (plantDatabaseManager.deletePlantCareEvent(event.id)) {
                // Reload events
                loadEventsForSelectedDate()
                
                Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                
                // Dismiss dialog
                (it.parent.parent.parent as android.app.Dialog).dismiss()
            } else {
                Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show()
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Event Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun refreshPlantsList() {
        // Update adapter with fresh data
        plantAdapter.updatePlants(plantDatabaseManager.getAllPlants())
        
        // Update visibility
        if (plantAdapter.itemCount == 0) {
            noPlantsMessage.visibility = View.VISIBLE
            plantsRecyclerView.visibility = View.GONE
        } else {
            noPlantsMessage.visibility = View.GONE
            plantsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data in case it was changed in other activities
        refreshPlantsList()
        loadEventsForSelectedDate()
    }
}