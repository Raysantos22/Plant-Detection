package com.PlantDetection

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
            onCheckboxClick = { event -> onEventCompleted(event) },
            onRescheduleClick = { event -> showRescheduleDialog(event) },
            onRescanClick = { event, plantType -> startRescanActivity(event, plantType) },
            onViewScheduleClick = { plantId ->
                val plant = plantDatabaseManager.getPlant(plantId)
                if (plant != null) {
                    showPlantDetailsDialog(plant)
                    // Optionally, automatically switch to the schedule tab
                    // You would need to make scheduleTabButton a class property to access it here
                }
            }
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

        // Handle direct opening for a specific plant
        handleDirectOpen()

        // Check if we should show treatment plan
        if (intent.getBooleanExtra("SHOW_TREATMENT_PLAN", false)) {
            val openPlantId = intent.getStringExtra("OPEN_PLANT_ID")
            if (openPlantId != null) {
                val plant = plantDatabaseManager.getPlant(openPlantId)
                if (plant != null) {
                    // Open the plant details dialog with schedule tab active
                    showPlantDetailsWithTreatmentPlan(plant)
                }
            }
        }
    }
    private fun showPlantDetailsWithTreatmentPlan(plant: PlantDatabaseManager.Plant) {
        val dialog = showPlantDetailsDialog(plant)

        // Find the schedule tab button and click it to show treatment plan
        val scheduleTabButton = dialog.findViewById<TextView>(R.id.scheduleTabButton)
        scheduleTabButton?.performClick()
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

        // Set meaningful text for empty plants state
        noPlantsMessage.text = "You don't have any plants yet. Tap the + button to add a plant."
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

    private fun handleDirectOpen() {
        // Check if we should open a specific plant
        val openPlantId = intent.getStringExtra("OPEN_PLANT_ID")
        if (openPlantId != null) {
            val plant = plantDatabaseManager.getPlant(openPlantId)
            if (plant != null) {
                selectedPlantId = plant.id
                plantAdapter.updateSelectedPlantId(selectedPlantId)
                loadEventsForSelectedDate()
                showPlantDetailsDialog(plant)
            }
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
        // Check if event is from today or past, not future
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val eventDate = Calendar.getInstance()
        eventDate.time = event.date
        eventDate.set(Calendar.HOUR_OF_DAY, 0)
        eventDate.set(Calendar.MINUTE, 0)
        eventDate.set(Calendar.SECOND, 0)
        eventDate.set(Calendar.MILLISECOND, 0)

        // Only allow completing events for today or past
        if (eventDate.after(today)) {
            Toast.makeText(this, "Future events cannot be marked as complete", Toast.LENGTH_SHORT).show()
            return
        }

        // Toggle completion status
        plantDatabaseManager.toggleEventCompletion(event.id)

        // Reload events
        loadEventsForSelectedDate()

        // Show confirmation toast
        val statusMessage = if (!event.completed) {
            "Task marked as complete"
        } else {
            "Task marked as incomplete"
        }

        Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
    }

    private fun askForRescan(event: PlantDatabaseManager.PlantCareEvent, plantType: String) {
        AlertDialog.Builder(this)
            .setTitle("Rescan Plant?")
            .setMessage("Would you like to rescan your plant to check if its condition has improved?")
            .setPositiveButton("Rescan") { _, _ ->
                startRescanActivity(event, plantType)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun startRescanActivity(event: PlantDatabaseManager.PlantCareEvent, plantType: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SELECTED_VEGETABLE", plantType)
            putExtra("SELECTED_PLANT_ID", event.plantId)
        }
        startActivity(intent)
    }

    private fun showRescheduleDialog(event: PlantDatabaseManager.PlantCareEvent) {
        // Show date picker dialog to reschedule the event
        val calendar = Calendar.getInstance()
        calendar.time = event.date

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Show time picker
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Update event with new date
                        val updatedEvent = event.copy(date = calendar.time)
                        if (plantDatabaseManager.updatePlantCareEvent(updatedEvent)) {
                            Toast.makeText(
                                this,
                                "Event rescheduled for ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(calendar.time)}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Reload events
                            loadEventsForSelectedDate()
                        } else {
                            Toast.makeText(this, "Failed to reschedule event", Toast.LENGTH_SHORT).show()
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false // Use 12-hour format with AM/PM
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set min date to today
        val today = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = today.timeInMillis

        datePickerDialog.show()
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

                val wateringFrequency = if (wateringFrequencyText.isEmpty()) 2 else wateringFrequencyText.toIntOrNull() ?: 2

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
                    // Schedule complete care plan for the plant
                    createCompletePlantCarePlan(plantId, plant.type, wateringFrequency)

                    // Update plants list
                    refreshPlantsList()

                    Toast.makeText(this, "Plant added successfully with complete care schedule", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to add plant", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCompletePlantCarePlan(plantId: String, plantType: String, wateringFrequency: Int) {
        val calendar = Calendar.getInstance()

        // 1. Schedule initial watering for tomorrow morning
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val initialWateringDate = calendar.time
        plantDatabaseManager.scheduleWatering(plantId, initialWateringDate, "Initial watering")

        // 2. Schedule regular watering for the next 30 days
        for (day in 2..30 step wateringFrequency) {
            calendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)
            val wateringDate = calendar.time

            val wateringId = "watering_${plantId}_${System.currentTimeMillis() + day}"
            val wateringEvent = PlantDatabaseManager.PlantCareEvent(
                id = wateringId,
                plantId = plantId,
                eventType = "Watering",
                date = wateringDate,
                notes = "Regular watering",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(wateringEvent)
        }

        // 3. Schedule fertilizing every 2 weeks
        calendar.time = initialWateringDate
        calendar.add(Calendar.DAY_OF_MONTH, 7) // First fertilizing after 1 week
        calendar.set(Calendar.HOUR_OF_DAY, 10) // Set to 10 AM

        for (week in 1..4) {
            val fertilizingId = "fertilizing_${plantId}_${System.currentTimeMillis() + week}"
            val fertilizingEvent = PlantDatabaseManager.PlantCareEvent(
                id = fertilizingId,
                plantId = plantId,
                eventType = "Fertilize",
                date = calendar.time,
                notes = "Apply balanced fertilizer according to package instructions",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(fertilizingEvent)

            // Next fertilizing in 2 weeks
            calendar.add(Calendar.DAY_OF_MONTH, 14)
        }

        // 4. Schedule pruning/maintenance checks every 10 days
        calendar.time = initialWateringDate
        calendar.add(Calendar.DAY_OF_MONTH, 10)
        calendar.set(Calendar.HOUR_OF_DAY, 15) // Set to 3 PM

        for (check in 1..3) {
            val maintenanceId = "maintenance_${plantId}_${System.currentTimeMillis() + check}"

            val maintenanceType = if (plantType == "Tomato") "Prune" else "Inspect"
            val notes = if (plantType == "Tomato")
                "Remove suckers and check for yellow leaves"
            else
                "Check for pests and remove damaged leaves"

            val maintenanceEvent = PlantDatabaseManager.PlantCareEvent(
                id = maintenanceId,
                plantId = plantId,
                eventType = maintenanceType,
                date = calendar.time,
                notes = notes,
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(maintenanceEvent)

            // Next maintenance in 10 days
            calendar.add(Calendar.DAY_OF_MONTH, 10)
        }

        // 5. Schedule a health scan every month
        calendar.time = initialWateringDate
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 14) // Set to 2 PM

        val scanId = "scan_${plantId}_${System.currentTimeMillis()}"
        val scanEvent = PlantDatabaseManager.PlantCareEvent(
            id = scanId,
            plantId = plantId,
            eventType = "Scan",
            date = calendar.time,
            notes = "Monthly health check scan",
            completed = false
        )

        plantDatabaseManager.addPlantCareEvent(scanEvent)
    }

    private fun showPlantDetailsDialog(plant: PlantDatabaseManager.Plant): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_details_enhanced, null)

        // Main info section views
        val plantNameText = dialogView.findViewById<TextView>(R.id.plantNameText)
        val plantTypeText = dialogView.findViewById<TextView>(R.id.plantTypeText)
        val plantStatusText = dialogView.findViewById<TextView>(R.id.plantStatusText)
        val nextWateringText = dialogView.findViewById<TextView>(R.id.nextWateringText)

        // Buttons
        val waterNowButton = dialogView.findViewById<android.widget.Button>(R.id.waterNowButton)
        val scanButton = dialogView.findViewById<android.widget.Button>(R.id.scanButton)
        val addTreatmentButton = dialogView.findViewById<android.widget.Button>(R.id.addTreatmentButton)
        val viewScheduleButton = dialogView.findViewById<android.widget.Button>(R.id.viewScheduleButton)

        // Care schedule section views
        val scheduleTabButton = dialogView.findViewById<TextView>(R.id.scheduleTabButton)
        val detailsTabButton = dialogView.findViewById<TextView>(R.id.detailsTabButton)
        val mainInfoSection = dialogView.findViewById<View>(R.id.mainInfoSection)
        val scheduleSection = dialogView.findViewById<View>(R.id.scheduleSection)

        // Schedule section contents
        val upcomingTasksList = dialogView.findViewById<LinearLayout>(R.id.upcomingTasksList)
        val activeTasksList = dialogView.findViewById<LinearLayout>(R.id.activeTasksList)
        val historyTasksList = dialogView.findViewById<LinearLayout>(R.id.historyTasksList)
        val noUpcomingTasksMsg = dialogView.findViewById<TextView>(R.id.noUpcomingTasksMsg)
        val noActiveTasksMsg = dialogView.findViewById<TextView>(R.id.noActiveTasksMsg)
        val noHistoryTasksMsg = dialogView.findViewById<TextView>(R.id.noHistoryTasksMsg)

        // Set plant details
        plantNameText.text = plant.name
        plantTypeText.text = plant.type
        plantStatusText.text = plant.currentCondition ?: "No recent scan"

        // Set next watering date with AM/PM format
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.getDefault())
        nextWateringText.text = plant.nextWateringDate?.let {
            "Next watering: ${dateFormat.format(it)}"
        } ?: "No scheduled watering"

        // Setup tab switching
        scheduleTabButton.setOnClickListener {
            // Switch to schedule view
            mainInfoSection.visibility = View.GONE
            scheduleSection.visibility = View.VISIBLE
            scheduleTabButton.setTextColor(ContextCompat.getColor(this, R.color.app_dark_green))
            scheduleTabButton.setTypeface(null, Typeface.BOLD)
            detailsTabButton.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
            detailsTabButton.setTypeface(null, Typeface.NORMAL)
        }

        detailsTabButton.setOnClickListener {
            // Switch to details view
            mainInfoSection.visibility = View.VISIBLE
            scheduleSection.visibility = View.GONE
            detailsTabButton.setTextColor(ContextCompat.getColor(this, R.color.app_dark_green))
            detailsTabButton.setTypeface(null, Typeface.BOLD)
            scheduleTabButton.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
            scheduleTabButton.setTypeface(null, Typeface.NORMAL)
        }

        // Load care schedule data
        loadPlantCareSchedule(plant, upcomingTasksList, activeTasksList, historyTasksList,
            noUpcomingTasksMsg, noActiveTasksMsg, noHistoryTasksMsg)

        // Set button listeners
        waterNowButton.setOnClickListener {
            // Schedule watering for today
            val now = Calendar.getInstance().time
            plantDatabaseManager.scheduleWatering(plant.id, now, "Manual watering")

            // Reload events
            loadEventsForSelectedDate()

            Toast.makeText(this, "Watering scheduled for today", Toast.LENGTH_SHORT).show()
        }

        scanButton.setOnClickListener {
            // Open scan activity with this plant's information
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SELECTED_VEGETABLE", plant.type)
            intent.putExtra("SELECTED_PLANT_ID", plant.id)
            startActivity(intent)
        }

        // Add treatment button - only show if a condition is present that's not healthy
        addTreatmentButton.visibility = if (plant.currentCondition != null &&
            !plant.currentCondition.startsWith("Healthy")) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Fix button text (was showing "Scan Plant" incorrectly)
        if (addTreatmentButton.visibility == View.VISIBLE) {
            addTreatmentButton.text = "Add Treatment"
        }

        addTreatmentButton.setOnClickListener {
            // Get the condition data to create appropriate treatment tasks
            val conditionName = plant.currentCondition ?: return@setOnClickListener
            val condition = PlantConditionData.conditions[conditionName]

            if (condition != null) {
                showAddTreatmentTasksDialog(plant, condition)
            } else {
                Toast.makeText(this, "No treatment data available for this condition", Toast.LENGTH_SHORT).show()
            }
        }

        // View schedule button (alternative to using tab)
        viewScheduleButton.setOnClickListener {
            scheduleTabButton.performClick()
        }

        // Create and show dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.setOnDismissListener {
            // Refresh events in case schedules were modified
            loadEventsForSelectedDate()
        }

        dialog.show()

        return dialog
    }

    private fun loadPlantCareSchedule(
        plant: PlantDatabaseManager.Plant,
        upcomingTasksList: LinearLayout,
        activeTasksList: LinearLayout,
        historyTasksList: LinearLayout,
        noUpcomingTasksMsg: TextView,
        noActiveTasksMsg: TextView,
        noHistoryTasksMsg: TextView
    ) {
        // Clear existing items
        upcomingTasksList.removeAllViews()
        activeTasksList.removeAllViews()
        historyTasksList.removeAllViews()

        // Get all events for this plant
        val allEvents = plantDatabaseManager.getPlantCareEvents(plant.id)

        // Current date for comparisons
        val now = Calendar.getInstance()
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        // Group events
        val upcomingEvents = mutableListOf<PlantDatabaseManager.PlantCareEvent>()
        val activeEvents = mutableListOf<PlantDatabaseManager.PlantCareEvent>()
        val historyEvents = mutableListOf<PlantDatabaseManager.PlantCareEvent>()

        for (event in allEvents) {
            val eventDay = Calendar.getInstance()
            eventDay.time = event.date
            eventDay.set(Calendar.HOUR_OF_DAY, 0)
            eventDay.set(Calendar.MINUTE, 0)
            eventDay.set(Calendar.SECOND, 0)
            eventDay.set(Calendar.MILLISECOND, 0)

            if (event.completed) {
                // Completed events go to history
                historyEvents.add(event)
            } else if (eventDay.after(today)) {
                // Future events go to upcoming
                upcomingEvents.add(event)
            } else {
                // Today's or past incomplete events go to active
                activeEvents.add(event)
            }
        }

        // Sort events by date
        upcomingEvents.sortBy { it.date }
        activeEvents.sortBy { it.date }
        historyEvents.sortByDescending { it.date } // Most recent first for history

        // Limit to most recent events
        val maxItemsPerCategory = 5

        // Populate upcoming tasks
        if (upcomingEvents.isEmpty()) {
            noUpcomingTasksMsg.visibility = View.VISIBLE
        } else {
            noUpcomingTasksMsg.visibility = View.GONE
            populateTasksList(upcomingEvents.take(maxItemsPerCategory), upcomingTasksList, true)

            // Add "View All" button if needed
            if (upcomingEvents.size > maxItemsPerCategory) {
                addViewAllButton(upcomingTasksList) {
                    showFullTaskList(plant, "Upcoming Tasks", upcomingEvents)
                }
            }
        }

        // Populate active tasks
        if (activeEvents.isEmpty()) {
            noActiveTasksMsg.visibility = View.VISIBLE
        } else {
            noActiveTasksMsg.visibility = View.GONE
            populateTasksList(activeEvents.take(maxItemsPerCategory), activeTasksList, true)

            // Add "View All" button if needed
            if (activeEvents.size > maxItemsPerCategory) {
                addViewAllButton(activeTasksList) {
                    showFullTaskList(plant, "Active Tasks", activeEvents)
                }
            }
        }

        // Populate history
        if (historyEvents.isEmpty()) {
            noHistoryTasksMsg.visibility = View.VISIBLE
        } else {
            noHistoryTasksMsg.visibility = View.GONE
            populateTasksList(historyEvents.take(maxItemsPerCategory), historyTasksList, false)

            // Add "View All" button if needed
            if (historyEvents.size > maxItemsPerCategory) {
                addViewAllButton(historyTasksList) {
                    showFullTaskList(plant, "Task History", historyEvents)
                }
            }
        }
    }
    // Update populateTasksList method to show condition name
    private fun populateTasksList(
        events: List<PlantDatabaseManager.PlantCareEvent>,
        container: LinearLayout,
        showActionButtons: Boolean
    ) {
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.getDefault())

        for (event in events) {
            val taskItem = LayoutInflater.from(this)
                .inflate(R.layout.item_plant_task, container, false)

            val taskDate = taskItem.findViewById<TextView>(R.id.taskDate)
            val taskTitle = taskItem.findViewById<TextView>(R.id.taskTitle)
            val taskStatus = taskItem.findViewById<TextView>(R.id.taskStatus)
            val taskActionButton = taskItem.findViewById<Button>(R.id.taskActionButton)
            val taskCondition = taskItem.findViewById<TextView>(R.id.taskCondition)

            taskDate.text = dateFormat.format(event.date)

            // If the event type starts with "Treat: ", extract the condition name
            // and set it in the taskCondition TextView
            if (event.eventType.startsWith("Treat: ")) {
                val title = event.eventType.substringAfter("Treat: ")
                taskTitle.text = "Treatment"

                if (event.conditionName != null) {
                    taskCondition.text = event.conditionName
                    taskCondition.visibility = View.VISIBLE
                } else {
                    taskCondition.visibility = View.GONE
                }
            } else {
                taskTitle.text = event.eventType
                taskCondition.visibility = View.GONE
            }

            if (event.completed) {
                taskStatus.text = "Completed"
                taskStatus.setTextColor(ContextCompat.getColor(this, R.color.app_dark_green))
                taskActionButton.visibility = View.GONE
            } else {
                val now = Calendar.getInstance().time
                if (event.date.before(now)) {
                    taskStatus.text = "Overdue"
                    taskStatus.setTextColor(ContextCompat.getColor(this, R.color.orange))
                } else {
                    taskStatus.text = "Scheduled"
                    taskStatus.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
                }

                // Show action button if needed
                if (showActionButtons) {
                    taskActionButton.visibility = View.VISIBLE
                    taskActionButton.setOnClickListener {
                        showEventDetailsDialog(event)
                    }
                } else {
                    taskActionButton.visibility = View.GONE
                }
            }

            // Make the whole item clickable
            taskItem.setOnClickListener {
                showEventDetailsDialog(event)
            }

            container.addView(taskItem)
        }
    }
    private fun showFullTaskList(
        plant: PlantDatabaseManager.Plant,
        title: String,
        events: List<PlantDatabaseManager.PlantCareEvent>
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_full_task_list, null)
        val tasksList = dialogView.findViewById<LinearLayout>(R.id.fullTasksList)

        // Whether to show action buttons depends on the type of events
        val showActionButtons = title != "Task History"

        // Populate full list
        populateTasksList(events, tasksList, showActionButtons)

        AlertDialog.Builder(this)
            .setTitle("${plant.name} - $title")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }
    private fun showAddTreatmentTasksDialog(
        plant: PlantDatabaseManager.Plant,
        condition: PlantConditionData.PlantCondition
    ) {
        // Create a multi-select dialog of treatment tasks
        val taskNames = condition.treatmentTasks.map { it.taskName }.toTypedArray()
        val checkedItems = BooleanArray(taskNames.size) { true } // Default all selected

        AlertDialog.Builder(this)
            .setTitle("Add Treatment Tasks")
            .setMultiChoiceItems(taskNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Add Tasks") { _, _ ->
                var tasksAdded = 0

                // For each selected task, create a care event
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        val task = condition.treatmentTasks[i]
                        addTreatmentTaskWithFollowUps(plant, condition, task)
                        tasksAdded++
                    }
                }

                // Reload events
                loadEventsForSelectedDate()

                Toast.makeText(this, "$tasksAdded treatment tasks added with follow-ups", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun addViewAllButton(container: LinearLayout, onClick: () -> Unit) {
        val viewAllButton = LayoutInflater.from(this)
            .inflate(R.layout.item_view_all_button, container, false)

        viewAllButton.setOnClickListener {
            onClick.invoke()
        }

        container.addView(viewAllButton)
    }

    private fun addTreatmentTaskWithFollowUps(
        plant: PlantDatabaseManager.Plant,
        condition: PlantConditionData.PlantCondition,
        task: PlantConditionData.TreatmentTask
    ) {
        // Create event for initial task - set for today
        val eventId = "treatment_${plant.id}_${System.currentTimeMillis()}"
        val today = Calendar.getInstance()

        // Set a reasonable time based on task name
        when {
            task.taskName.contains("Water") -> today.set(Calendar.HOUR_OF_DAY, 9) // Morning
            task.taskName.contains("Fertilize") -> today.set(Calendar.HOUR_OF_DAY, 10) // Mid-morning
            task.taskName.contains("Prune") -> today.set(Calendar.HOUR_OF_DAY, 15) // Afternoon
            task.taskName.contains("Spray") || task.taskName.contains("Apply") -> today.set(Calendar.HOUR_OF_DAY, 17) // Evening
            else -> today.set(Calendar.HOUR_OF_DAY, 12) // Noon default
        }
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)

        val treatmentEvent = PlantDatabaseManager.PlantCareEvent(
            id = eventId,
            plantId = plant.id,
            eventType = "Treatment",
            date = today.time,
            conditionName = condition.name,
            notes = "${task.taskName}: ${task.description}\n\nMaterials: ${task.materials.joinToString(", ")}\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
            completed = false
        )

        plantDatabaseManager.addPlantCareEvent(treatmentEvent)

        // Calculate and add follow-up tasks for the next 30 days
        if (task.scheduleInterval > 0) {
            val calendar = Calendar.getInstance()
            calendar.time = today.time

            // Create follow-ups for approximately 1 month (using 30 days)
            val numberOfFollowUps = (30 / task.scheduleInterval).coerceAtMost(6)

            for (i in 1..numberOfFollowUps) {
                calendar.add(Calendar.DAY_OF_MONTH, task.scheduleInterval)

                val followUpId = "followup_${plant.id}_${System.currentTimeMillis() + i}"
                val followUpEvent = PlantDatabaseManager.PlantCareEvent(
                    id = followUpId,
                    plantId = plant.id,
                    eventType = "Treatment",
                    date = calendar.time,
                    conditionName = condition.name,
                    notes = "Follow-up #$i: ${task.taskName}\n\n${task.description}\n\nMaterials: ${task.materials.joinToString(", ")}\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
                    completed = false
                )

                plantDatabaseManager.addPlantCareEvent(followUpEvent)
            }
        }
    }

    private fun showPlantOptionsDialog(plant: PlantDatabaseManager.Plant) {
        val options = arrayOf("View Care Schedule", "Edit Plant", "Schedule Watering", "Apply Treatment", "Delete Plant")

        AlertDialog.Builder(this)
            .setTitle(plant.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPlantCareScheduleDialog(plant)
                    1 -> showEditPlantDialog(plant)
                    2 -> showScheduleWateringDialog(plant)
                    3 -> showTreatmentOptionsDialog(plant)
                    4 -> confirmDeletePlant(plant)
                }
            }
            .show()
    }

    private fun showPlantCareScheduleDialog(plant: PlantDatabaseManager.Plant) {
        // Get all events for this plant
        val allEvents = plantDatabaseManager.getPlantCareEvents(plant.id)
            .filter { !it.completed && it.date.after(Date()) }
            .sortedBy { it.date }

        if (allEvents.isEmpty()) {
            Toast.makeText(this, "No upcoming events scheduled for this plant", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_care_schedule, null)
        val scheduleList = dialogView.findViewById<LinearLayout>(R.id.scheduleItemsList)
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.getDefault())

        // Group events by month for better organization
        val eventsByMonth = allEvents.groupBy {
            val calendar = Calendar.getInstance()
            calendar.time = it.date
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
        }

        for ((monthKey, monthEvents) in eventsByMonth) {
            // Add month header
            val monthHeader = LayoutInflater.from(this)
                .inflate(R.layout.item_schedule_month_header, scheduleList, false)

            val calendar = Calendar.getInstance()
            calendar.time = monthEvents.first().date
            val monthTitle = monthHeader.findViewById<TextView>(R.id.monthHeaderText)
            monthTitle.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

            scheduleList.addView(monthHeader)

            // Add events for this month
            for (event in monthEvents) {
                val eventItem = LayoutInflater.from(this)
                    .inflate(R.layout.item_schedule_event, scheduleList, false)

                val eventDate = eventItem.findViewById<TextView>(R.id.scheduleEventDate)
                val eventTitle = eventItem.findViewById<TextView>(R.id.scheduleEventTitle)
                val eventDesc = eventItem.findViewById<TextView>(R.id.scheduleEventDesc)

                eventDate.text = dateFormat.format(event.date)
                eventTitle.text = event.eventType
                eventDesc.text = event.notes.split("\n").firstOrNull() ?: ""

                scheduleList.addView(eventItem)
            }
        }

        // Add a button to add more tasks
        val addMoreButton = LayoutInflater.from(this)
            .inflate(R.layout.item_add_more_tasks, scheduleList, false)

        addMoreButton.setOnClickListener {
            showAddExtraTaskDialog(plant)
        }

        scheduleList.addView(addMoreButton)

        AlertDialog.Builder(this)
            .setTitle("${plant.name} Care Schedule")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showAddExtraTaskDialog(plant: PlantDatabaseManager.Plant) {
        val taskTypes = arrayOf("Watering", "Fertilize", "Prune", "Inspect", "Scan", "Repot")
        var selectedTaskType = taskTypes[0]

        AlertDialog.Builder(this)
            .setTitle("Add Custom Task")
            .setSingleChoiceItems(taskTypes, 0) { _, which ->
                selectedTaskType = taskTypes[which]
            }
            .setPositiveButton("Next") { _, _ ->
                showTaskScheduleDialog(plant, selectedTaskType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTaskScheduleDialog(plant: PlantDatabaseManager.Plant, taskType: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_task, null)

        val taskNameInput = dialogView.findViewById<android.widget.EditText>(R.id.taskNameInput)
        val taskNotesInput = dialogView.findViewById<android.widget.EditText>(R.id.taskNotesInput)
        val repeatCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.repeatCheckbox)
        val repeatIntervalInput = dialogView.findViewById<android.widget.EditText>(R.id.repeatIntervalInput)
        val repeatIntervalLayout = dialogView.findViewById<android.view.View>(R.id.repeatIntervalLayout)

        // Set default task name based on type
        taskNameInput.setText(taskType)

        // Set default notes based on type
        when (taskType) {
            "Watering" -> taskNotesInput.setText("Regular watering")
            "Fertilize" -> taskNotesInput.setText("Apply balanced fertilizer")
            "Prune" -> taskNotesInput.setText("Remove suckers and damaged leaves")
            "Inspect" -> taskNotesInput.setText("Check for pests and diseases")
            "Scan" -> taskNotesInput.setText("Scan plant to monitor health")
            "Repot" -> taskNotesInput.setText("Transfer to larger container with fresh soil")
        }

        // Show/hide repeat interval based on checkbox
        repeatCheckbox.setOnCheckedChangeListener { _, isChecked ->
            repeatIntervalLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Show date/time picker for the task
        AlertDialog.Builder(this)
            .setTitle("Schedule $taskType")
            .setView(dialogView)
            .setPositiveButton("Set Date & Time") { _, _ ->
                val taskName = taskNameInput.text.toString().trim()
                val taskNotes = taskNotesInput.text.toString().trim()
                val repeatTask = repeatCheckbox.isChecked
                val repeatInterval = if (repeatTask) {
                    repeatIntervalInput.text.toString().toIntOrNull() ?: 7
                } else 0

                // Show date picker
                showDateTimePickerForTask(plant, taskType, taskName, taskNotes, repeatTask, repeatInterval)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDateTimePickerForTask(
        plant: PlantDatabaseManager.Plant,
        taskType: String,
        taskName: String,
        taskNotes: String,
        repeatTask: Boolean,
        repeatInterval: Int
    ) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Show time picker
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Schedule the task
                        scheduleCustomTask(plant, taskType, taskName, taskNotes, calendar.time, repeatTask, repeatInterval)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false // Use 12-hour format with AM/PM
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set min date to today
        val today = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = today.timeInMillis

        datePickerDialog.show()
    }

    private fun scheduleCustomTask(
        plant: PlantDatabaseManager.Plant,
        taskType: String,
        taskName: String,
        taskNotes: String,
        startDate: Date,
        repeatTask: Boolean,
        repeatInterval: Int
    ) {
        // Create ID for initial task
        val taskId = "${taskType.lowercase()}_${plant.id}_${System.currentTimeMillis()}"

        // Create initial task
        val initialTask = PlantDatabaseManager.PlantCareEvent(
            id = taskId,
            plantId = plant.id,
            eventType = taskName,
            date = startDate,
            notes = taskNotes,
            completed = false
        )

        plantDatabaseManager.addPlantCareEvent(initialTask)

        // If task should repeat, schedule future occurrences
        if (repeatTask && repeatInterval > 0) {
            val calendar = Calendar.getInstance()
            calendar.time = startDate

            // Schedule repeating tasks for approximately 3 months
            // Maximum of 12 repeats to avoid creating too many events
            val repeatCount = minOf(90 / repeatInterval, 12)

            for (i in 1..repeatCount) {
                calendar.add(Calendar.DAY_OF_MONTH, repeatInterval)

                // Create a new ID for each repeated task
                val repeatTaskId = "${taskType.lowercase()}_${plant.id}_${System.currentTimeMillis() + i}"

                // Create the repeated task
                val repeatedTask = PlantDatabaseManager.PlantCareEvent(
                    id = repeatTaskId,
                    plantId = plant.id,
                    eventType = taskName,
                    date = calendar.time,
                    notes = "$taskNotes (Recurring #$i)",
                    completed = false
                )

                plantDatabaseManager.addPlantCareEvent(repeatedTask)
            }

            Toast.makeText(
                this,
                "Scheduled $taskName with $repeatCount repeats every $repeatInterval days",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Scheduled $taskName for ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(startDate)}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Reload events if we're on the start date
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)

        if (calendar.timeInMillis == startCalendar.timeInMillis) {
            loadEventsForSelectedDate()
        }
    }

    private fun showTreatmentOptionsDialog(plant: PlantDatabaseManager.Plant) {
        // First check if we have a condition
        if (plant.currentCondition == null) {
            AlertDialog.Builder(this)
                .setTitle("No Condition Data")
                .setMessage("Please scan your plant first to detect any conditions requiring treatment.")
                .setPositiveButton("Scan Now") { _, _ ->
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("SELECTED_VEGETABLE", plant.type)
                    intent.putExtra("SELECTED_PLANT_ID", plant.id)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // Get the condition data
        val conditionName = plant.currentCondition
        val condition = PlantConditionData.conditions[conditionName]

        if (condition != null) {
            showAddTreatmentTasksDialog(plant, condition)
        } else {
            // If the specific condition isn't found, provide general treatment options
            val generalTasks = arrayOf(
                "Apply Fungicide",
                "Apply Insecticide",
                "Prune Affected Areas",
                "Apply Neem Oil",
                "Adjust Watering Schedule"
            )

            AlertDialog.Builder(this)
                .setTitle("Treatment Options")
                .setItems(generalTasks) { _, which ->
                    val selectedTask = generalTasks[which]

                    // Show task scheduling dialog
                    showTaskScheduleDialog(plant, selectedTask)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEditPlantDialog(plant: PlantDatabaseManager.Plant) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_plant, null)

        val nameInput = dialogView.findViewById<android.widget.EditText>(R.id.plantNameInput)
        val wateringFrequencyInput = dialogView.findViewById<android.widget.EditText>(R.id.wateringFrequencyInput)
        val notesInput = dialogView.findViewById<android.widget.EditText>(R.id.plantNotesInput)

        // Add a warning message about changing watering frequency
        val warningText = dialogView.findViewById<TextView>(R.id.wateringWarningText)
        warningText.text = "Note: Changing watering frequency will only affect future scheduled waterings"

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

                // Check if watering frequency changed
                val wateringFrequencyChanged = plant.wateringFrequency != wateringFrequency

                // Update plant
                val updatedPlant = plant.copy(
                    name = name,
                    wateringFrequency = wateringFrequency,
                    notes = notes
                )

                if (plantDatabaseManager.updatePlant(updatedPlant)) {
                    // Update plants list
                    refreshPlantsList()

                    // If watering frequency changed, ask if user wants to update existing schedule
                    if (wateringFrequencyChanged) {
                        AlertDialog.Builder(this)
                            .setTitle("Update Watering Schedule?")
                            .setMessage("Do you want to update your existing watering schedule to match the new frequency? This will reschedule all future waterings.")
                            .setPositiveButton("Yes") { _, _ ->
                                updateWateringSchedule(plant.id, wateringFrequency)
                                Toast.makeText(this, "Watering schedule updated", Toast.LENGTH_SHORT).show()
                                loadEventsForSelectedDate()
                            }
                            .setNegativeButton("No", null)
                            .show()
                    } else {
                        Toast.makeText(this, "Plant updated successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Failed to update plant", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateWateringSchedule(plantId: String, wateringFrequency: Int) {
        // Get all future watering events
        val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)
        val futureWateringEvents = allEvents.filter {
            it.eventType.equals("Watering", ignoreCase = true) &&
                    !it.completed &&
                    it.date.after(Date())
        }.sortedBy { it.date }

        // If no future events, nothing to update
        if (futureWateringEvents.isEmpty()) return

        // Delete all future watering events
        for (event in futureWateringEvents) {
            plantDatabaseManager.deletePlantCareEvent(event.id)
        }

        // Create new schedule starting from the next planned date
        val startDate = if (futureWateringEvents.isNotEmpty()) {
            futureWateringEvents.first().date
        } else {
            // If no future events, start from tomorrow
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.time
        }

        // Create new watering schedule
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        // Create 12 waterings (covers approximately 3 months with weekly watering)
        for (i in 0 until 12) {
            if (i > 0) {
                // Add watering frequency days for next watering
                calendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)
            }

            val wateringId = "watering_${plantId}_${System.currentTimeMillis() + i}"
            val wateringEvent = PlantDatabaseManager.PlantCareEvent(
                id = wateringId,
                plantId = plantId,
                eventType = "Watering",
                date = calendar.time,
                notes = if (i == 0) "Next watering" else "Regular watering #$i",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(wateringEvent)
        }
    }

    private fun showScheduleWateringDialog(plant: PlantDatabaseManager.Plant) {
        // Create view for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_watering, null)

        val singleWateringRadio = dialogView.findViewById<android.widget.RadioButton>(R.id.singleWateringRadio)
        val multipleWateringRadio = dialogView.findViewById<android.widget.RadioButton>(R.id.multipleWateringRadio)
        val wateringCountLayout = dialogView.findViewById<android.view.View>(R.id.wateringCountLayout)
        val wateringCountText = dialogView.findViewById<android.widget.EditText>(R.id.wateringCountInput)

        // Show/hide watering count based on selection
        singleWateringRadio.setOnCheckedChangeListener { _, isChecked ->
            wateringCountLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        multipleWateringRadio.setOnCheckedChangeListener { _, isChecked ->
            wateringCountLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        AlertDialog.Builder(this)
            .setTitle("Schedule Watering")
            .setView(dialogView)
            .setPositiveButton("Continue") { _, _ ->
                val isMultiple = multipleWateringRadio.isChecked
                val wateringCount = if (isMultiple) {
                    wateringCountText.text.toString().toIntOrNull() ?: 4
                } else 1

                // Create calendar for date picker
                val calendar = Calendar.getInstance()

                // Show date picker
                val datePickerDialog = DatePickerDialog(
                    this,
                    { _, year, month, dayOfMonth ->
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                        // Show time picker
                        val timePickerDialog = TimePickerDialog(
                            this,
                            { _, hourOfDay, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                calendar.set(Calendar.MINUTE, minute)

                                // Create watering event(s)
                                if (isMultiple) {
                                    // Schedule multiple waterings
                                    scheduleMultipleWaterings(plant, calendar.time, wateringCount, plant.wateringFrequency)
                                } else {
                                    // Create single watering event
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
                                        "Watering scheduled for ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(calendar.time)}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false // 12-hour format with AM/PM
                        )
                        timePickerDialog.show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                // Set min date to today
                val today = Calendar.getInstance()
                datePickerDialog.datePicker.minDate = today.timeInMillis

                datePickerDialog.show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleMultipleWaterings(
        plant: PlantDatabaseManager.Plant,
        firstWateringDate: Date,
        wateringCount: Int,
        wateringFrequency: Int
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = firstWateringDate

        for (i in 0 until wateringCount) {
            if (i > 0) {
                // Add watering frequency days for next watering
                calendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)
            }

            val wateringId = "watering_${plant.id}_${System.currentTimeMillis() + i}"
            val wateringEvent = PlantDatabaseManager.PlantCareEvent(
                id = wateringId,
                plantId = plant.id,
                eventType = "Watering",
                date = calendar.time,
                notes = if (i == 0) "First scheduled watering" else "Scheduled watering #${i+1}",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(wateringEvent)
        }

        // Reload events if the first watering is on the currently selected date
        val firstWateringCalendar = Calendar.getInstance()
        firstWateringCalendar.time = firstWateringDate
        firstWateringCalendar.set(Calendar.HOUR_OF_DAY, 0)
        firstWateringCalendar.set(Calendar.MINUTE, 0)
        firstWateringCalendar.set(Calendar.SECOND, 0)

        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.time = selectedDate
        selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
        selectedCalendar.set(Calendar.MINUTE, 0)
        selectedCalendar.set(Calendar.SECOND, 0)

        if (firstWateringCalendar.timeInMillis == selectedCalendar.timeInMillis) {
            loadEventsForSelectedDate()
        }

        // Show confirmation
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val lastDate = calendar.time

        Toast.makeText(
            this,
            "Scheduled $wateringCount waterings from ${dateFormat.format(firstWateringDate)} to ${dateFormat.format(lastDate)}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun confirmDeletePlant(plant: PlantDatabaseManager.Plant) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Plant")
                .setMessage("Are you sure you want to delete ${plant.name}? This action cannot be undone and will remove all scheduled care tasks.")
                .setPositiveButton("Delete") { dialog, _ ->
                    try {
                        // First cancel all pending notifications
                        val careEvents = plantDatabaseManager.getPlantCareEvents(plant.id)
                        for (event in careEvents) {
                            // Cancel notifications for this event to prevent crashes
                            // when notifications for deleted plants get triggered
                            plantDatabaseManager.cancelNotification(event.id)
                        }

                        // Now delete the plant
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
                    } catch (e: Exception) {
                        // Log the error
                        Log.e("PlantManagement", "Error deleting plant: ${e.message}")
                        e.printStackTrace()

                        // Show error message
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            // Log the error
            Log.e("PlantManagement", "Error showing delete dialog: ${e.message}")
            e.printStackTrace()

            // Show error message
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
        val rescheduleButton = dialogView.findViewById<android.widget.Button>(R.id.rescheduleEventButton)
        val rescanButton = dialogView.findViewById<android.widget.Button>(R.id.rescanButton)
        val conditionHeaderText = dialogView.findViewById<TextView>(R.id.conditionHeaderText) // Add this to your layout
        val conditionNameText = dialogView.findViewById<TextView>(R.id.conditionNameText) // Add this to your layout

        // Get plant name
        val plant = plantDatabaseManager.getPlant(event.plantId)
        val plantName = plant?.name ?: "Unknown plant"

        // Set event details
        if (event.eventType.startsWith("Treat: ")) {
            // For treatment events, show specific info
            val conditionName = event.eventType.substringAfter("Treat: ")

            // Get task name from notes if available
            val taskName = event.notes.split("\n\n").getOrNull(1)?.split(":")?.getOrNull(0)?.trim() ?: "Treatment"

            eventTypeText.text = taskName
            conditionHeaderText.visibility = View.VISIBLE
            conditionNameText.visibility = View.VISIBLE
            conditionNameText.text = conditionName
        } else {
            // For other event types
            eventTypeText.text = event.eventType
            conditionHeaderText.visibility = if (event.conditionName != null) View.VISIBLE else View.GONE
            conditionNameText.visibility = if (event.conditionName != null) View.VISIBLE else View.GONE
            conditionNameText.text = event.conditionName ?: ""
        }

        eventDateText.text = SimpleDateFormat("EEE, MMM d, yyyy - h:mm a", Locale.getDefault()).format(event.date)
        eventPlantText.text = plantName

        // Format notes for better readability - particularly for treatment tasks
        if (event.eventType.startsWith("Treat: ")) {
            // Try to extract structured info from notes
            val parts = event.notes.split("\n\n")
            var formattedNotes = ""

            if (parts.size > 1) {
                // Skip the first part which is usually the treatment plan title
                for (i in 1 until parts.size) {
                    val part = parts[i]
                    if (part.contains(":")) {
                        // This is likely a section title with content
                        val sectionParts = part.split(":", limit = 2)
                        formattedNotes += "<b>${sectionParts[0]}:</b>${sectionParts[1]}\n\n"
                    } else {
                        // Just add the part as is
                        formattedNotes += part + "\n\n"
                    }
                }
            } else {
                // Just use the notes as they are
                formattedNotes = event.notes
            }

            // If notes are empty or couldn't be parsed, show default treatment instructions
            if (formattedNotes.isBlank() && event.conditionName != null) {
                val condition = PlantConditionData.conditions[event.conditionName]
                if (condition != null) {
                    formattedNotes = "<b>Treatment for ${condition.name}:</b>\n\n" +
                            condition.treatmentTips.joinToString("\n\n• ", "• ")
                }
            }

            // Use HTML formatting for the notes
            eventNotesText.text = Html.fromHtml(formattedNotes, Html.FROM_HTML_MODE_COMPACT)
        } else {
            // For non-treatment events, just show the notes as they are
            eventNotesText.text = if (event.notes.isNotEmpty()) event.notes else "No notes"
        }

        eventStatusText.text = if (event.completed) "Completed" else "Pending"

        // Update button text based on current status
        completeButton.text = if (event.completed) "Mark Incomplete" else "Mark Complete"

        // Check if event is in the future
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val eventDate = Calendar.getInstance()
        eventDate.time = event.date
        eventDate.set(Calendar.HOUR_OF_DAY, 0)
        eventDate.set(Calendar.MINUTE, 0)
        eventDate.set(Calendar.SECOND, 0)
        eventDate.set(Calendar.MILLISECOND, 0)

        val isFutureEvent = eventDate.after(today)

        // Disable complete button for future events
        completeButton.isEnabled = !isFutureEvent || event.completed
        if (isFutureEvent && !event.completed) {
            completeButton.text = "Cannot complete future event"
        }

        // Only show reschedule for future events
        rescheduleButton.visibility = if (isFutureEvent && !event.completed) View.VISIBLE else View.GONE

        // Only show rescan button for treatment events that aren't completed
        val isTreatmentEvent = event.eventType.startsWith("Treat: ") || event.eventType.equals("Treatment", ignoreCase = true)
        rescanButton.visibility = if (isTreatmentEvent && !event.completed) View.VISIBLE else View.GONE

        // Set button listeners
        completeButton.setOnClickListener {
            // Don't allow completing future events
            if (isFutureEvent && !event.completed) {
                Toast.makeText(this, "Cannot complete future events", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Toggle completion status
            plantDatabaseManager.toggleEventCompletion(event.id)

            // Update UI
            loadEventsForSelectedDate()

            // Update status text and button
            val updatedEvent = plantDatabaseManager.getPlantCareEvent(event.id)
            if (updatedEvent != null) {
                eventStatusText.text = if (updatedEvent.completed) "Completed" else "Pending"
                completeButton.text = if (updatedEvent.completed) "Mark Incomplete" else "Mark Complete"

                // Hide rescan button if event is now completed
                if (updatedEvent.completed) {
                    rescanButton.visibility = View.GONE
                } else {
                    // Show rescan only for treatment events
                    rescanButton.visibility = if (isTreatmentEvent) View.VISIBLE else View.GONE
                }
            }

            // Show confirmation
            val statusMessage = if (updatedEvent?.completed == true) {
                "Task marked as complete"
            } else {
                "Task marked as incomplete"
            }
            Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()

            // If this was a treatment event and now completed, ask if they want to rescan the plant
            if (isTreatmentEvent && updatedEvent?.completed == true) {
                askForRescan(event, plant?.type ?: "")
            }
        }

        // Fix delete functionality to prevent crashes
        deleteButton.setOnClickListener {
            try {
                // Show confirmation dialog
                AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this ${event.eventType} task?")
                    .setPositiveButton("Delete") { confirmDialog, _ ->
                        try {
                            // First cancel any notification for this event
                            plantDatabaseManager.cancelNotification(event.id)

                            // Then delete the event
                            if (plantDatabaseManager.deletePlantCareEvent(event.id)) {
                                // Reload events
                                loadEventsForSelectedDate()
                                Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()

                                // Dismiss both dialogs
                                confirmDialog.dismiss()
                                (deleteButton.parent.parent.parent as android.app.Dialog).dismiss()
                            } else {
                                Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("PlantManagement", "Error deleting event: ${e.message}")
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Log.e("PlantManagement", "Error showing delete confirmation: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        rescheduleButton.setOnClickListener {
            // Show reschedule dialog
            showRescheduleDialog(event)

            // Dismiss current dialog
            (it.parent.parent.parent as android.app.Dialog).dismiss()
        }

        rescanButton.setOnClickListener {
            // Start rescan activity
            if (plant != null) {
                startRescanActivity(event, plant.type)

                // Dismiss dialog
                (it.parent.parent.parent as android.app.Dialog).dismiss()
            } else {
                Toast.makeText(this, "Plant information not available", Toast.LENGTH_SHORT).show()
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

    // Add month markers to the calendar to show days with events
    private fun updateCalendarWithEvents() {
        // Get all events for the next 3 months
        val calendar = Calendar.getInstance()
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 3)
        val endDate = calendar.time

        val allEvents = plantDatabaseManager.getCareEventsInDateRange(startDate, endDate)

        // Group events by date
        val eventsByDate = allEvents.groupBy { event ->
            val eventCal = Calendar.getInstance()
            eventCal.time = event.date
            val year = eventCal.get(Calendar.YEAR)
            val month = eventCal.get(Calendar.MONTH)
            val day = eventCal.get(Calendar.DAY_OF_MONTH)
            "$year-$month-$day"
        }

        // TODO: In the future, we could add custom date decorators to show event counts
        // This would require extending the CalendarView or using a third-party calendar library
    }

    override fun onResume() {
        super.onResume()
        // Refresh data in case it was changed in other activities
        refreshPlantsList()
        loadEventsForSelectedDate()
        updateCalendarWithEvents()
    }
}