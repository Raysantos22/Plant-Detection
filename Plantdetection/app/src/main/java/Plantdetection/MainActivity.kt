package com.PlantDetection

import com.PlantDetection.PlantManagementActivity
import android.Manifest
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.PlantDetection.Constants.LABELS_PATH
import com.PlantDetection.Constants.MODEL_PATH
import com.PlantDetection.databinding.ActivityMainBinding
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false
    private var selectedVegetable: String? = null
    private var selectedPlantId: String? = null
    private var infoDialog: Dialog? = null

    // Scanning control variables
    private var isScanning = false
    private var currentDetection: BoundingBox? = null
    private val MIN_CONFIDENCE_THRESHOLD = 0.65f // Minimum confidence to enable capture

    // Plant database integration
    private lateinit var plantDatabaseManager: PlantDatabaseManager

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    private lateinit var cameraExecutor: ExecutorService

    private val currentDetections = mutableListOf<BoundingBox>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "onCreate called")

        // Initialize plant database manager
        plantDatabaseManager = PlantDatabaseManager(this)

        // Get the selected vegetable from the intent
        selectedVegetable = intent.getStringExtra("SELECTED_VEGETABLE")
        selectedPlantId = intent.getStringExtra("SELECTED_PLANT_ID")

        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraExecutor.execute {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        bindListeners()
    }

    private fun bindListeners() {
        binding.apply {
            isGpu.setOnCheckedChangeListener { buttonView, isChecked ->
                cameraExecutor.submit {
                    detector?.restart(isGpu = isChecked)
                }
                if (isChecked) {
                    buttonView.setBackgroundColor(
                        ContextCompat.getColor(
                            baseContext,
                            R.color.orange
                        )
                    )
                } else {
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.gray))
                }
            }

            // Toggle scanning on/off
            scanButton.setOnClickListener {
                toggleScanning()
            }

            // Capture the current detection
            captureButton.setOnClickListener {
                captureDetection()
            }
        }
    }

    private fun toggleScanning() {
        isScanning = !isScanning
        binding.apply {
            if (isScanning) {
                scanButton.text = getString(R.string.stop_scanning)
                scanButton.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.orange))
                scanStatusText.text = "Scanning..."
                scanStatusText.setTextColor(ContextCompat.getColor(baseContext, R.color.orange))
            } else {
                scanButton.text = getString(R.string.start_scanning)
                scanButton.setBackgroundColor(
                    ContextCompat.getColor(
                        baseContext,
                        R.color.app_dark_green
                    )
                )
                scanStatusText.text = "Not scanning"
                scanStatusText.setTextColor(ContextCompat.getColor(baseContext, R.color.gray))
                overlay.clear()
                detectionText.visibility = View.INVISIBLE
                captureButton.isEnabled = false
            }
        }
    }

    //    private fun captureDetection() {
//        // If we have a valid detection, show details and save it
//        currentDetection?.let { detection ->
//            // Check if the detected condition matches our selected vegetable type
//            val isRelevantToSelectedVegetable = when (selectedVegetable) {
//                "Tomato" -> detection.clsName != "Healthy Eggplant"
//                "Eggplant" -> detection.clsName != "Healthy Tomato"
//                else -> true
//            }
//
//            if (isRelevantToSelectedVegetable && PlantConditionData.conditions.containsKey(detection.clsName)) {
//                // First stop scanning
//                if (isScanning) {
//                    toggleScanning()
//                }
//
//                // If we have a plant ID, update the plant's condition
//                if (selectedPlantId != null) {
//                    updatePlantCondition(selectedPlantId!!, detection.clsName)
//                    // Note: showPlantInfoDialog is called inside updatePlantCondition for rescans
//                } else {
//                    // Show info dialog for fresh scan
//                    showPlantInfoDialog(detection.clsName, true, false)
//                }
//            } else {
//                Toast.makeText(this, "Invalid detection for selected vegetable", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        } ?: run {
//            Toast.makeText(this, "No valid detection to capture", Toast.LENGTH_SHORT).show()
//        }
//    }
    private fun isSameVegetableType(cls1: String, cls2: String): Boolean {
        // Consider all tomato variants as "Tomato" and all eggplant variants as "Eggplant"
        val type1 = when {
            cls1.contains("Tomato", ignoreCase = true) -> "Tomato"
            cls1.contains("Eggplant", ignoreCase = true) -> "Eggplant"
            else -> cls1
        }

        val type2 = when {
            cls2.contains("Tomato", ignoreCase = true) -> "Tomato"
            cls2.contains("Eggplant", ignoreCase = true) -> "Eggplant"
            else -> cls2
        }

        return type1 == type2
    }

    // 2. Modify captureDetection method to handle multiple plants
    private fun captureDetection() {
        // If we have valid detections, show details and save them
        if (currentDetections.isNotEmpty()) {
            // Group detections by condition
            val detectionsByCondition = currentDetections.groupBy { it.clsName }

            // Check if the detected conditions match our selected vegetable type
            val selectedVegType = selectedVegetable ?: "Unknown"
            val isRelevantToSelectedVegetable = when (selectedVegType) {
                "Tomato" -> detectionsByCondition.keys.all { !it.contains("Eggplant", ignoreCase = true) }
                "Eggplant" -> detectionsByCondition.keys.all { !it.contains("Tomato", ignoreCase = true) }
                else -> true
            }

            if (isRelevantToSelectedVegetable && detectionsByCondition.keys.any { PlantConditionData.conditions.containsKey(it) }) {
                // First stop scanning
                if (isScanning) {
                    toggleScanning()
                }

                // If we have a plant ID, update the plant's conditions
                if (selectedPlantId != null) {
                    updatePlantWithMultipleConditions(selectedPlantId!!, detectionsByCondition)
                } else {
                    // Show info dialog for fresh scan with multiple plants
                    showMultiplePlantInfoDialog(detectionsByCondition, true, false)
                }
            } else {
                Toast.makeText(this, "Invalid detection for selected vegetable", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No valid detections to capture", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Add method to handle updating a plant with multiple conditions
    private fun updatePlantWithMultipleConditions(plantId: String, detectionsByCondition: Map<String, List<BoundingBox>>) {
        val plant = plantDatabaseManager.getPlant(plantId)
        plant?.let {
            // Get current plant data
            val wasHealthy = plant.currentCondition?.startsWith("Healthy") ?: true

            // Calculate total plants and conditions in new scan
            val totalPlants = detectionsByCondition.values.sumOf { it.size }
            val totalConditions = detectionsByCondition.size

            // Check if the name format needs to be updated
            val isPlantGroup = plant.name.contains("plants") && plant.name.contains("(")
            val updatedName = if (isPlantGroup) {
                // Extract the base name without the count info
                val baseName = plant.name.substringBefore("(").trim()
                // Update with new counts
                "$baseName ($totalPlants plants, $totalConditions conditions)"
            } else {
                // If it wasn't a group before but is being scanned as multiple plants now,
                // update the name format
                if (totalPlants > 1) {
                    "${plant.name} ($totalPlants plants, $totalConditions conditions)"
                } else {
                    plant.name
                }
            }

            // Get the previous conditions from notes (if any)
            val previousConditions = mutableMapOf<String, Int>()
            val notesLines = plant.notes.split("\n")

            for (line in notesLines) {
                if (line.trim().startsWith("-") && line.contains(":") && line.contains("plants")) {
                    val conditionName = line.substringAfter("-").substringBefore(":").trim()
                    val countPart = line.substringAfter(":").trim()
                    val count = countPart.substringBefore(" ").toIntOrNull() ?: 0

                    if (count > 0) {
                        previousConditions[conditionName] = count
                    }
                }
            }

            // Determine which conditions have been resolved
            val resolvedConditions = previousConditions.keys.filter { prevCondition ->
                !detectionsByCondition.keys.any { it == prevCondition } &&
                        !prevCondition.startsWith("Healthy")
            }

            // Set primary condition to the highest priority one (disease takes priority over healthy)
            val primaryCondition = getPrimaryCondition(detectionsByCondition.keys.toList())

            // Check if this is a new primary condition
            val isNewCondition = plant.currentCondition != primaryCondition
            val isNowHealthy = primaryCondition.startsWith("Healthy")

            // Save new condition information in plant notes
            val conditionSummary = "Multiple plant detection (${Date()}):\n" +
                    detectionsByCondition.entries.joinToString("\n") { entry ->
                        "- ${entry.key}: ${entry.value.size} plants"
                    }

            // Update notes, keeping other information but replacing any previous detection reports
            val baseNotes = plant.notes.substringBefore("Multiple plant detection").trim()
            val updatedNotes = if (baseNotes.isEmpty()) conditionSummary else "$baseNotes\n\n$conditionSummary"

            // Update plant with new information
            val updatedPlant = it.copy(
                name = updatedName,
                currentCondition = primaryCondition,
                lastScannedDate = Date(),
                notes = updatedNotes
            )

            // Save to database
            if (plantDatabaseManager.updatePlant(updatedPlant)) {
                // Create a scan event for the primary condition
                val eventId = "scan_${plantId}_${System.currentTimeMillis()}"
                val scanEvent = PlantDatabaseManager.PlantCareEvent(
                    id = eventId,
                    plantId = plantId,
                    eventType = "Scan",
                    date = Date(),
                    conditionName = primaryCondition,
                    notes = "Multiple conditions detected: ${detectionsByCondition.keys.joinToString(", ")}",
                    completed = true
                )

                // Save event
                plantDatabaseManager.addPlantCareEvent(scanEvent)

                // Send broadcast to refresh plant management activity
                val intent = Intent("com.PlantDetection.REFRESH_PLANT_STATUS")
                intent.putExtra("PLANT_ID", plantId)
                try {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error sending broadcast: ${e.message}")
                }

                // Create events for each additional condition
                for ((condition, detections) in detectionsByCondition) {
                    if (condition != primaryCondition) {
                        val additionalEventId = "scan_${plantId}_${condition}_${System.currentTimeMillis()}"
                        val additionalEvent = PlantDatabaseManager.PlantCareEvent(
                            id = additionalEventId,
                            plantId = plantId,
                            eventType = "Scan",
                            date = Date(),
                            conditionName = condition,
                            notes = "Condition detected: $condition (${detections.size} plants)",
                            completed = true
                        )
                        plantDatabaseManager.addPlantCareEvent(additionalEvent)
                    }
                }

                // IMPORTANT: Cancel all existing care events (except completed ones)
                // to recreate the care plan based on new conditions
                cancelFutureCareEvents(plantId)

                // Always recreate the complete plant care plan based on new conditions
                createCompletePlantCarePlan(plantId, updatedPlant.type, updatedPlant.wateringFrequency)

                // If plant was healthy and now has a disease
                if (wasHealthy && !isNowHealthy) {
                    // Show plant info dialog with all conditions
                    showMultiplePlantInfoDialog(detectionsByCondition, true, true)
                    Toast.makeText(this, "Treatment plans created for detected conditions", Toast.LENGTH_SHORT).show()
                }
                // If changing from diseased to healthy, show congratulations
                else if (!wasHealthy && isNowHealthy) {
                    // Show recovery dialog
                    AlertDialog.Builder(this)
                        .setTitle("Plants Recovered!")
                        .setMessage("Great news! Your ${updatedPlant.name} have recovered and are now healthy. All treatments have been updated.")
                        .setPositiveButton("View Plant") { _, _ ->
                            val intent = Intent(this, PlantManagementActivity::class.java)
                            intent.putExtra("OPEN_PLANT_ID", plantId)
                            startActivity(intent)
                            finish() // Close this activity
                        }
                        .setNegativeButton("OK", null)
                        .show()
                }
                // If condition changed but still diseased, update treatment
                else if (isNewCondition && !isNowHealthy) {
                    // Show plant info dialog with all conditions
                    showMultiplePlantInfoDialog(detectionsByCondition, true, true)
                    Toast.makeText(this, "Treatment plans updated based on new scan", Toast.LENGTH_SHORT).show()
                }
                // Just a normal rescan with no change
                else {
                    // Show plant info dialog with all conditions
                    showMultiplePlantInfoDialog(detectionsByCondition, true, true)
                    Toast.makeText(this, "Plant conditions updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun cancelExistingTreatmentTasks(plantId: String) {
        val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

        // Find incomplete treatment events
        val pendingTreatments = allEvents.filter {
            (it.eventType.startsWith("Treat: ") || it.eventType.equals("Treatment", ignoreCase = true)) &&
                    !it.completed
        }

        // Delete each pending treatment task
        for (event in pendingTreatments) {
            plantDatabaseManager.deletePlantCareEvent(event.id)
        }
    }
    private fun markTreatmentsForConditionAsCompleted(plantId: String, conditionName: String) {
        val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

        // Find incomplete treatment events for this specific condition
        val pendingTreatments = allEvents.filter {
            (it.eventType.startsWith("Treat: $conditionName") ||
                    (it.eventType.equals("Treatment", ignoreCase = true) && it.conditionName == conditionName)) &&
                    !it.completed
        }

        // Mark each one as completed
        for (event in pendingTreatments) {
            val updatedEvent = event.copy(completed = true)
            plantDatabaseManager.updatePlantCareEvent(updatedEvent)
        }
    }
    // 4. Helper to determine the primary condition from multiple detections
    private fun getPrimaryCondition(conditions: List<String>): String {
        // Prioritize diseases over healthy conditions
        val diseaseConditions = conditions.filter { !it.startsWith("Healthy") }
        return if (diseaseConditions.isNotEmpty()) {
            diseaseConditions.first()  // Use the first disease as primary
        } else {
            conditions.first()  // Use the first condition (healthy) if no diseases
        }
    }

    // 5. Create a modified dialog to show info for multiple plant detections
    private fun showMultiplePlantInfoDialog(
        detectionsByCondition: Map<String, List<BoundingBox>>,
        showMonitoringOptions: Boolean = false,
        isRescan: Boolean = false
    ) {
        // Get primary condition for the main display
        val conditions = detectionsByCondition.keys.toList()
        val primaryCondition = getPrimaryCondition(conditions)
        val condition = PlantConditionData.conditions[primaryCondition] ?: return

        // Create dialog with custom layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_info, null)

        // Set primary condition details
        val conditionTitle = dialogView.findViewById<TextView>(R.id.conditionTitle)
        val conditionDescription = dialogView.findViewById<TextView>(R.id.conditionDescription)
        val preventionTipsContainer = dialogView.findViewById<LinearLayout>(R.id.preventionTipsContainer)
        val treatmentTipsContainer = dialogView.findViewById<LinearLayout>(R.id.treatmentTipsContainer)
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val monitoringSection = dialogView.findViewById<View>(R.id.monitoringSection)
        val setReminderButton = dialogView.findViewById<android.widget.Button>(R.id.setReminderButton)
        val taskCompleteButton = dialogView.findViewById<android.widget.Button>(R.id.taskCompleteButton)
        val addToCalendarButton = dialogView.findViewById<android.widget.Button>(R.id.addToCalendarButton)
        val goToManagementButton = dialogView.findViewById<android.widget.Button>(R.id.goToManagementButton)

        // Custom title to show multiple plants and conditions were detected
        val totalPlants = detectionsByCondition.values.sumOf { it.size }
        val totalConditions = detectionsByCondition.size
        conditionTitle.text = "$totalPlants plants detected: $totalConditions conditions"

        // Create a more detailed description showing plant counts
        val conditionCounts = detectionsByCondition.entries.joinToString("\n") { (condition, detections) ->
            "• $condition: ${detections.size} plants"
        }
        conditionDescription.text = "Plant conditions detected:\n$conditionCounts"

        // Create a custom card for showing all detected plant conditions
        val addDetailsText = TextView(this)
        addDetailsText.text = "Multiple plants detected in this scan:"
        addDetailsText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        addDetailsText.textSize = 16f
        addDetailsText.setTypeface(null, Typeface.BOLD)
        addDetailsText.setPadding(0, 24, 0, 8)
        preventionTipsContainer.addView(addDetailsText)

        // Add a summary of each detected condition with plant counts
        for ((condName, detections) in detectionsByCondition) {
            val countText = TextView(this)
            countText.text = "• $condName: ${detections.size} plants"
            countText.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
            countText.textSize = 14f
            countText.setPadding(16, 4, 0, 4)
            preventionTipsContainer.addView(countText)
        }

        // Add treatment tips section - first add a header
        treatmentTipsContainer.removeAllViews()

        // For each condition, add its information
        for ((condName, detections) in detectionsByCondition) {
            val conditionData = PlantConditionData.conditions[condName]
            if (conditionData != null) {
                // Add condition header
                val headerText = TextView(this)
                headerText.text = "$condName (${detections.size} plants)"
                headerText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                headerText.textSize = 16f
                headerText.setTypeface(null, Typeface.BOLD)
                headerText.setPadding(0, 24, 0, 8)
                treatmentTipsContainer.addView(headerText)

                // Add treatment tips for this condition
                conditionData.treatmentTips.take(2).forEach { tip -> // Limit to 2 tips to save space
                    val tipView = LayoutInflater.from(this).inflate(R.layout.item_treatment_tip, treatmentTipsContainer, false)
                    tipView.findViewById<TextView>(R.id.tipText).text = tip
                    treatmentTipsContainer.addView(tipView)
                }
            }
        }

        // Configure the monitoring section based on scan type (same as your original code)
        if (isRescan && selectedPlantId != null) {
            // This is a rescan of an existing plant - show Go to Management button
            monitoringSection.visibility = View.VISIBLE

            // Hide the regular buttons
            setReminderButton.visibility = View.GONE
            taskCompleteButton.visibility = View.GONE
            addToCalendarButton.visibility = View.GONE

            // Show the go to management button
            goToManagementButton.visibility = View.VISIBLE

            // Set go to management button listener
            goToManagementButton.setOnClickListener {
                val intent = Intent(this, PlantManagementActivity::class.java)
                intent.putExtra("OPEN_PLANT_ID", selectedPlantId)
                startActivity(intent)
                finish() // Close this activity
            }
        } else if (showMonitoringOptions && selectedPlantId != null) {
            // Regular scan for an existing plant
            monitoringSection.visibility = View.VISIBLE
            goToManagementButton.visibility = View.GONE
            setReminderButton.visibility = View.VISIBLE
            taskCompleteButton.visibility = View.VISIBLE
            addToCalendarButton.visibility = View.GONE

            // Set reminder button
            setReminderButton.setOnClickListener {
                dialogShowDateTimePicker(primaryCondition)
            }

            // Task complete button
            taskCompleteButton.setOnClickListener {
                markTreatmentComplete(primaryCondition)
                Toast.makeText(this, "Task marked as complete", Toast.LENGTH_SHORT).show()
                infoDialog?.dismiss()

                // If this was a disease/pest condition, ask if it's resolved
                if (!primaryCondition.startsWith("Healthy")) {
                    showResolutionConfirmationDialog()
                }
            }
        } else if (showMonitoringOptions) {
            // This is a one-time scan, show option to add to my plants
            monitoringSection.visibility = View.VISIBLE
            setReminderButton.visibility = View.GONE
            taskCompleteButton.visibility = View.GONE
            goToManagementButton.visibility = View.GONE
            addToCalendarButton.visibility = View.VISIBLE

            // Add to calendar button - now shows dialog for multiple plants
            addToCalendarButton.setOnClickListener {
                stopCameraOperations() // Ensure camera is stopped
                showAddMultiplePlantsDialog(detectionsByCondition)
            }
        } else {
            monitoringSection.visibility = View.GONE
        }

        // Set up close button
        closeButton.setOnClickListener {
            infoDialog?.dismiss()

            // If we came from the Plant Management screen and this was a rescan, go back there
            if (selectedPlantId != null && isRescan) {
                val intent = Intent(this, PlantManagementActivity::class.java)
                intent.putExtra("OPEN_PLANT_ID", selectedPlantId)
                startActivity(intent)
                finish()
            }
        }

        // Create and show the dialog
        infoDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        infoDialog?.show()
    }

    // 6. Add dialog for creating a new plant group from multiple detections
    // 1. COMPLETE IMPLEMENTATION FOR showAddMultiplePlantsDialog METHOD
    private fun showAddMultiplePlantsDialog(detectionsByCondition: Map<String, List<BoundingBox>>) {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_calendar, null)

            // Initialize views with null checks
            val detectionResultText = dialogView.findViewById<TextView>(R.id.detectionResultText)
            val plantNameInput = dialogView.findViewById<EditText>(R.id.plantNameInput)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val addPlantButton = dialogView.findViewById<Button>(R.id.addPlantButton)

            if (detectionResultText == null || plantNameInput == null ||
                cancelButton == null || addPlantButton == null) {
                Log.e("MainActivity", "Missing views in dialog layout")
                Toast.makeText(this, "Error creating dialog", Toast.LENGTH_SHORT).show()
                return
            }

            // Get total plants and conditions
            val totalPlants = detectionsByCondition.values.sumOf { it.size }
            val totalConditions = detectionsByCondition.size
            detectionResultText.text = "Detected: $totalPlants plants with $totalConditions conditions"

            // Hide watering controls
            val wateringFrequencyText = dialogView.findViewById<TextView>(R.id.wateringFrequencyText)
            val decreaseFrequencyButton = dialogView.findViewById<Button>(R.id.decreaseFrequencyButton)
            val increaseFrequencyButton = dialogView.findViewById<Button>(R.id.increaseFrequencyButton)
            wateringFrequencyText?.visibility = View.GONE
            decreaseFrequencyButton?.visibility = View.GONE
            increaseFrequencyButton?.visibility = View.GONE

            // Default watering frequency
            val wateringFrequency = 1

            // Determine vegetable type
            val vegetableType = when {
                detectionsByCondition.keys.any { it.contains("Tomato", ignoreCase = true) } -> "Tomato"
                detectionsByCondition.keys.any { it.contains("Eggplant", ignoreCase = true) } -> "Eggplant"
                else -> selectedVegetable ?: "Tomato"
            }

            // Default plant name
            val suggestedName = "$vegetableType Group"
            plantNameInput.setText(suggestedName)

            // Add notice text
            try {
                val parent = plantNameInput.parent as? ViewGroup
                if (parent != null) {
                    val noticeText = TextView(this).apply {
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                        text = "Plant count information will be added automatically."
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(16, 8, 16, 16)
                        }
                    }
                    parent.addView(noticeText, parent.indexOfChild(plantNameInput) + 1)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error adding notice text: ${e.message}")
                // Non-critical error, continue without the notice
            }

            // Cancel button
            cancelButton.setOnClickListener {
                // Just dismiss the dialog
                infoDialog?.dismiss()
            }

            // Add plant button
            addPlantButton.setOnClickListener {
                addPlantButton.isEnabled = false

                val basePlantName = plantNameInput.text.toString().trim()
                if (basePlantName.isEmpty()) {
                    Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show()
                    addPlantButton.isEnabled = true
                    return@setOnClickListener
                }

                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Adding plant group...")
                    setCancelable(false)
                    show()
                }

                Thread {
                    try {
                        val totalPlants = detectionsByCondition.values.sumOf { it.size }
                        val totalConditions = detectionsByCondition.size

                        val finalPlantName = "$basePlantName (${totalPlants} plants, ${totalConditions} conditions)"

                        val vegetableType = when {
                            detectionsByCondition.keys.any { it.contains("Tomato", ignoreCase = true) } -> "Tomato"
                            detectionsByCondition.keys.any { it.contains("Eggplant", ignoreCase = true) } -> "Eggplant"
                            else -> selectedVegetable ?: "Tomato"
                        }

                        val plantId = addMultiplePlantsAsGroup(
                            plantName = finalPlantName,
                            vegetableType = vegetableType,
                            detectionsByCondition = detectionsByCondition,
                            wateringFrequency = 2
                        )

                        runOnUiThread {
                            progressDialog.dismiss()

                            if (plantId.isNotEmpty()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Plant group added successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Create care plan in background
                                Thread {
                                    try {
                                        createCompletePlantCarePlan(plantId, vegetableType, 2)

                                        runOnUiThread {
                                            infoDialog?.dismiss()
                                            navigateToPlantManagement(plantId, true)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Care plan error: ${e.message}", e)
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Error creating care plan",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }.start()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Failed to add plant group",
                                    Toast.LENGTH_SHORT
                                ).show()
                                addPlantButton.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            progressDialog.dismiss()
                            Log.e("MainActivity", "Plant group add error: ${e.message}", e)
                            Toast.makeText(
                                this@MainActivity,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            addPlantButton.isEnabled = true
                        }
                    }
                }.start()
            }

            // Create and show dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Set dialog dismissal listener
            dialog.setOnDismissListener {
                // Clean up
                infoDialog = null
            }

            // Replace existing dialog
            infoDialog?.dismiss()
            infoDialog = dialog
            dialog.show()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in showAddMultiplePlantsDialog: ${e.message}", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun stopCameraOperations() {
        try {
            // Stop scanning
            if (isScanning) {
                toggleScanning()
            }

            // Unbind all use cases
            cameraProvider?.unbindAll()

            // Shutdown camera executor
            cameraExecutor.shutdownNow()

            // Clear any ongoing detection
            currentDetections.clear()
            currentDetection = null

            // Reset UI elements
            binding.apply {
                overlay.clear()
                detectionText.visibility = View.INVISIBLE
                captureButton.isEnabled = false
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping camera operations: ${e.message}", e)
        }
    }
    private fun navigateToPlantManagement(plantId: String, createCarePlan: Boolean = false) {
        try {
            // Show a loading dialog
            val loadingDialog = ProgressDialog(this).apply {
                setMessage("Loading plant details...")
                setCancelable(false)
                show()
            }

            // Verify plant exists before navigating
            val plant = plantDatabaseManager.getPlant(plantId)
            if (plant == null) {
                Log.e("MainActivity", "Attempted to navigate to non-existent plant: $plantId")
                loadingDialog.dismiss()
                Toast.makeText(this, "Plant not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Use Handler to create a deliberate delay
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    // Prepare intent with clear top flag to prevent multiple instances
                    val intent = Intent(this, PlantManagementActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("OPEN_PLANT_ID", plantId)

                        // Optional care plan creation flag
                        if (createCarePlan) {
                            putExtra("CREATE_CARE_PLAN", true)
                            putExtra("PLANT_TYPE", plant.type)
                            putExtra("WATERING_FREQUENCY", plant.wateringFrequency)
                        }
                    }

                    // Dismiss loading dialog
                    loadingDialog.dismiss()

                    // Start activity and finish current one
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e("MainActivity", "Navigation error: ${e.message}", e)
                    Toast.makeText(this, "Error opening plant management", Toast.LENGTH_SHORT).show()
                }
            }, 5000) // 5 seconds (5000 milliseconds) delay
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation preparation error: ${e.message}", e)
            Toast.makeText(this, "Error preparing plant management", Toast.LENGTH_SHORT).show()
        }
    }


    // 7. Create a method to add multiple plants as a group
    private fun addMultiplePlantsAsGroup(
        plantName: String,
        vegetableType: String,
        detectionsByCondition: Map<String, List<BoundingBox>>,
        wateringFrequency: Int
    ): String {
        try {
            // Get primary condition for the plant record
            val conditions = detectionsByCondition.keys.toList()
            val primaryCondition = getPrimaryCondition(conditions)

            // Create a plant ID
            val plantId = "plant_${System.currentTimeMillis()}"

            // Ensure plant name contains the count info
            val totalPlants = detectionsByCondition.values.sumOf { it.size }
            val totalConditions = detectionsByCondition.size

            // Extract plant counts from name or use the detected counts
            val finalPlantName = if (!plantName.contains("(") || !plantName.contains("plants")) {
                "$plantName (${totalPlants} plants, ${totalConditions} conditions)"
            } else {
                plantName
            }

            // Create plant notes with detailed detection information
            val conditionCounts = detectionsByCondition.entries.joinToString("\n") { (condition, detections) ->
                "- $condition: ${detections.size} plants"
            }

            // Include details of all detected conditions in notes
            val notes = "Plant group containing $totalPlants plants with ${detectionsByCondition.size} different conditions:\n$conditionCounts\n\nDetected on: ${Date()}"

            // Create the plant record
            val plant = PlantDatabaseManager.Plant(
                id = plantId,
                name = finalPlantName,
                type = vegetableType,
                createdDate = Date(),
                lastScannedDate = Date(),
                currentCondition = primaryCondition,
                wateringFrequency = wateringFrequency,
                notes = notes
            )

            // Add plant to database
            if (plantDatabaseManager.addPlant(plant)) {
                // Create scan events for each condition
                for ((condition, detections) in detectionsByCondition) {
                    val scanEventId = "scan_${plantId}_${condition}_${System.currentTimeMillis()}"
                    val scanEvent = PlantDatabaseManager.PlantCareEvent(
                        id = scanEventId,
                        plantId = plantId,
                        eventType = "Scan",
                        date = Date(),
                        conditionName = condition,
                        notes = "Initial scan detected $condition in ${detections.size} plants",
                        completed = true
                    )

                    plantDatabaseManager.addPlantCareEvent(scanEvent)
                }

                // Return the plant ID
                return plantId
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in addMultiplePlantsAsGroup: ${e.message}", e)
        }

        return ""
    }
    private fun updatePlantCondition(plantId: String, conditionName: String) {
        val plant = plantDatabaseManager.getPlant(plantId)
        plant?.let {
            // Check if this is a new condition or different from the current one
            val isNewCondition = plant.currentCondition != conditionName
            val wasHealthy = plant.currentCondition?.startsWith("Healthy") ?: true
            val isNowHealthy = conditionName.startsWith("Healthy")

            // Check if the name format needs to be updated (if it was a group before)
            val isPlantGroup = plant.name.contains("plants") && plant.name.contains("(")
            val updatedName = if (isPlantGroup) {
                // For a single plant scan of what was previously a group,
                // maintain the group status but note the current condition
                plant.name
            } else {
                plant.name
            }

            // Update plant condition
            val updatedPlant = it.copy(
                name = updatedName,
                currentCondition = conditionName,
                lastScannedDate = Date()
            )

            // Save to database
            if (plantDatabaseManager.updatePlant(updatedPlant)) {
                // Create a scan event
                val eventId = "scan_${plantId}_${System.currentTimeMillis()}"
                val scanEvent = PlantDatabaseManager.PlantCareEvent(
                    id = eventId,
                    plantId = plantId,
                    eventType = "Scan",
                    date = Date(),
                    conditionName = conditionName,
                    notes = "Condition detected: $conditionName",
                    completed = true
                )

                // Save event
                plantDatabaseManager.addPlantCareEvent(scanEvent)

                // Send broadcast to refresh plant management activity
                val intent = Intent("com.PlantDetection.REFRESH_PLANT_STATUS")
                intent.putExtra("PLANT_ID", plantId)
                try {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error sending broadcast: ${e.message}")
                }

                // IMPORTANT: Cancel all existing care events (except completed ones)
                // to recreate the care plan based on new conditions
                cancelFutureCareEvents(plantId)

                // Always recreate the complete plant care plan based on new conditions
                createCompletePlantCarePlan(plantId, updatedPlant.type, updatedPlant.wateringFrequency)

                // If changing from healthy to diseased
                if (wasHealthy && !isNowHealthy) {
                    // Show plant info dialog with rescan flag = true
                    showPlantInfoDialog(conditionName, true, true)

                    Toast.makeText(
                        this,
                        "Treatment plan created for ${conditionName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // If changing from diseased to healthy, show congratulations
                else if (!wasHealthy && isNowHealthy) {
                    // Show recovery dialog directly
                    AlertDialog.Builder(this)
                        .setTitle("Plant Recovered!")
                        .setMessage("Great news! Your ${updatedPlant.name} has recovered and is now healthy. Care plan has been updated.")
                        .setPositiveButton("View Plant") { _, _ ->
                            val intent = Intent(this, PlantManagementActivity::class.java)
                            intent.putExtra("OPEN_PLANT_ID", plantId)
                            startActivity(intent)
                            finish() // Close this activity
                        }
                        .setNegativeButton("OK", null)
                        .show()
                }
                // If condition changed but still diseased
                else if (isNewCondition && !isNowHealthy) {
                    // Show plant info dialog with rescan flag = true
                    showPlantInfoDialog(conditionName, true, true)

                    Toast.makeText(
                        this,
                        "New treatment plan created for ${conditionName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Just a normal rescan with no change
                else {
                    // Show plant info dialog with rescan flag = true
                    showPlantInfoDialog(conditionName, true, true)

                    Toast.makeText(this, "Plant condition updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun cancelFutureCareEvents(plantId: String) {
        val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

        // Get the start of today
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // Find incomplete events that are from today onwards
        val futureEvents = allEvents.filter {
            !it.completed && // Not completed
                    !it.eventType.startsWith("Scan") && // Not a scan event (we keep scan history)
                    it.date.after(todayStart) // In the future or today
        }

        // Delete each future event
        for (event in futureEvents) {
            plantDatabaseManager.deletePlantCareEvent(event.id)
        }

        Log.d("MainActivity", "Canceled ${futureEvents.size} future care events for plant $plantId")
    }
    private fun markRemainingTreatmentsAsCompleted(plantId: String) {
        val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

        // Find all incomplete treatment events
        val pendingTreatments = allEvents.filter {
            (it.eventType.startsWith("Treat: ") || it.eventType.equals(
                "Treatment",
                ignoreCase = true
            )) &&
                    !it.completed
        }

        // Mark each one as completed
        for (event in pendingTreatments) {
            val updatedEvent = event.copy(completed = true)
            plantDatabaseManager.updatePlantCareEvent(updatedEvent)
        }
    }

    // Modifications to createCompletePlantCarePlan and createAutomaticTreatmentSchedule

    private fun createCompletePlantCarePlan(plantId: String, plantType: String, wateringFrequency: Int) {
        val calendar = Calendar.getInstance()
        val plant = plantDatabaseManager.getPlant(plantId)

        // Check if this is a plant group
        val isPlantGroup = plant?.name?.contains("(") == true && plant.name.contains("plants")
        var totalPlantsInGroup = 1
        val diseaseConditions = mutableMapOf<String, Int>()

        // For plant groups, extract condition counts (keep existing logic)
        if (isPlantGroup && plant != null) {
            // Extract total plant count from name
            val match = "\\((\\d+)\\s+plants".toRegex().find(plant.name)
            if (match != null) {
                totalPlantsInGroup = match.groupValues[1].toIntOrNull() ?: 1
            }

            // Parse conditions from notes (keep existing logic)
            val notesLines = plant.notes.split("\n")
            for (line in notesLines) {
                if (line.trim().startsWith("-") && line.contains(":") && line.contains("plants")) {
                    val conditionName = line.substringAfter("-").substringBefore(":").trim()
                    if (!conditionName.contains("Healthy", ignoreCase = true)) {
                        val countPart = line.substringAfter(":").trim()
                        val count = countPart.substringBefore(" ").toIntOrNull() ?: 0
                        if (count > 0) {
                            diseaseConditions[conditionName] = count
                        }
                    }
                }
            }
        }

        // 1. Schedule watering for TODAY AND future days (keep existing logic)
        val todayWatering = Calendar.getInstance().apply {
            val currentHour = get(Calendar.HOUR_OF_DAY)
            if (currentHour < 7) {
                set(Calendar.HOUR_OF_DAY, 9)
            } else {
                add(Calendar.HOUR_OF_DAY, 2)
            }
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        plantDatabaseManager.scheduleWatering(plantId, todayWatering,
            if (isPlantGroup) "Initial watering for all $totalPlantsInGroup plants" else "Initial watering")

        // Future waterings
        val wateringCalendar = Calendar.getInstance()
        wateringCalendar.time = todayWatering
        wateringCalendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)

        for (day in 1..30 step wateringFrequency) {
            val wateringDate = wateringCalendar.time

            val wateringId = "watering_${plantId}_${System.currentTimeMillis() + day}"
            val wateringEvent = PlantDatabaseManager.PlantCareEvent(
                id = wateringId,
                plantId = plantId,
                eventType = "Watering",
                date = wateringDate,
                notes = if (isPlantGroup) "Regular watering for all $totalPlantsInGroup plants" else "Regular watering",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(wateringEvent)

            wateringCalendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)
        }

        // 2. Schedule inspection task for TODAY
        val todayInspection = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30)
        }.time

        val inspectionId = "inspection_${plantId}_${System.currentTimeMillis()}"
        val inspectionEvent = PlantDatabaseManager.PlantCareEvent(
            id = inspectionId,
            plantId = plantId,
            eventType = "Inspect",
            date = todayInspection,
            notes = if (isPlantGroup)
                "First day inspection: Check all $totalPlantsInGroup plants for signs of stress or damage"
            else
                "First day inspection: Check plant for signs of stress or damage",
            completed = false
        )
        plantDatabaseManager.addPlantCareEvent(inspectionEvent)

        // 3. Schedule fertilizing - first one 3 days from now
        val fertilizingCalendar = Calendar.getInstance()
        fertilizingCalendar.add(Calendar.DAY_OF_MONTH, 3)
        fertilizingCalendar.set(Calendar.HOUR_OF_DAY, 10)

        for (week in 1..4) {
            val fertilizingId = "fertilizing_${plantId}_${System.currentTimeMillis() + week}"
            val fertilizingEvent = PlantDatabaseManager.PlantCareEvent(
                id = fertilizingId,
                plantId = plantId,
                eventType = "Fertilize",
                date = fertilizingCalendar.time,
                notes = if (isPlantGroup)
                    "Apply balanced fertilizer to all $totalPlantsInGroup plants"
                else
                    "Apply balanced fertilizer according to package instructions",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(fertilizingEvent)

            fertilizingCalendar.add(Calendar.DAY_OF_MONTH, 14)
        }

        // 4. Schedule maintenance checks - first one 5 days from now
        val maintenanceCalendar = Calendar.getInstance()
        maintenanceCalendar.add(Calendar.DAY_OF_MONTH, 5)
        maintenanceCalendar.set(Calendar.HOUR_OF_DAY, 15)

        for (check in 1..3) {
            val maintenanceId = "maintenance_${plantId}_${System.currentTimeMillis() + check}"

            val maintenanceType = if (plantType == "Tomato") "Prune" else "Inspect"
            val notes = if (isPlantGroup) {
                if (plantType == "Tomato")
                    "Remove suckers and check for yellow leaves on all $totalPlantsInGroup plants"
                else
                    "Check all $totalPlantsInGroup plants for pests and remove damaged leaves"
            } else {
                if (plantType == "Tomato")
                    "Remove suckers and check for yellow leaves"
                else
                    "Check for pests and remove damaged leaves"
            }

            val maintenanceEvent = PlantDatabaseManager.PlantCareEvent(
                id = maintenanceId,
                plantId = plantId,
                eventType = maintenanceType,
                date = maintenanceCalendar.time,
                notes = notes,
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(maintenanceEvent)

            maintenanceCalendar.add(Calendar.DAY_OF_MONTH, 10)
        }

        // 5. If there are disease conditions, schedule treatments
        if (diseaseConditions.isNotEmpty()) {
            for ((conditionName, plantCount) in diseaseConditions) {
                createAutomaticTreatmentSchedule(plantId, conditionName, plantCount)
            }
        } else if (!isPlantGroup && plant?.currentCondition != null && !plant.currentCondition.startsWith("Healthy")) {
            createAutomaticTreatmentSchedule(plantId, plant.currentCondition, 1)
        }

        // 6. Schedule health scan every month, starting in 30 days
        val scanCalendar = Calendar.getInstance()
        scanCalendar.add(Calendar.MONTH, 1)
        scanCalendar.set(Calendar.HOUR_OF_DAY, 14)

        val scanId = "scan_${plantId}_${System.currentTimeMillis()}"
        val scanEvent = PlantDatabaseManager.PlantCareEvent(
            id = scanId,
            plantId = plantId,
            eventType = "Scan",
            date = scanCalendar.time,
            notes = if (isPlantGroup)
                "Monthly health check scan for all $totalPlantsInGroup plants"
            else
                "Monthly health check scan",
            completed = false
        )

        plantDatabaseManager.addPlantCareEvent(scanEvent)
    }

    private fun createAutomaticTreatmentSchedule(
        plantId: String,
        conditionName: String,
        plantCount: Int = 1
    ) {
        val condition = PlantConditionData.conditions[conditionName] ?: return

        // Create an urgent task for today (only ONE task)
        val urgentTask = condition.treatmentTasks.firstOrNull()
        if (urgentTask != null) {
            val treatmentTime = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }.time

            val urgentTaskId = "urgent_treat_${plantId}_${conditionName}_${System.currentTimeMillis()}"
            val plantCountText = if (plantCount == 1) "(1 plant)" else "($plantCount plants)"

            val urgentTreatmentEvent = PlantDatabaseManager.PlantCareEvent(
                id = urgentTaskId,
                plantId = plantId,
                eventType = "Treat: $conditionName",
                date = treatmentTime,
                conditionName = conditionName,
                notes = "URGENT: ${urgentTask.taskName} for $conditionName $plantCountText\n\n${urgentTask.description}\n\nMaterials: ${urgentTask.materials.joinToString(", ")}\n\nInstructions:\n${urgentTask.instructions.joinToString("\n- ", "- ")}",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(urgentTreatmentEvent)
        }

        // Schedule follow-up treatments with more controlled spacing
        for ((index, task) in condition.treatmentTasks.withIndex()) {
            // Skip the first task as we've already added an urgent task
            if (index == 0) continue

            val scheduleDay = Calendar.getInstance()
            scheduleDay.add(Calendar.DAY_OF_MONTH, index * 2) // Space out tasks

            // Set appropriate times for different tasks
            when {
                task.taskName.contains("Remove", ignoreCase = true) -> scheduleDay.set(Calendar.HOUR_OF_DAY, 10)
                task.taskName.contains("Apply", ignoreCase = true) -> scheduleDay.set(Calendar.HOUR_OF_DAY, 17)
                else -> scheduleDay.set(Calendar.HOUR_OF_DAY, 12)
            }
            scheduleDay.set(Calendar.MINUTE, 0)
            scheduleDay.set(Calendar.SECOND, 0)

            val plantCountText = if (plantCount == 1) "(1 plant)" else "($plantCount plants)"
            val taskId = "treatment_${plantId}_${conditionName}_${System.currentTimeMillis() + index}"

            val treatmentEvent = PlantDatabaseManager.PlantCareEvent(
                id = taskId,
                plantId = plantId,
                eventType = "Treat: $conditionName",
                date = scheduleDay.time,
                conditionName = conditionName,
                notes = "${task.taskName} for $conditionName $plantCountText\n\n${task.description}\n\nMaterials: ${task.materials.joinToString(", ")}\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
                completed = false
            )

            plantDatabaseManager.addPlantCareEvent(treatmentEvent)

            // Add follow-up tasks with controlled scheduling if the task has a schedule interval
            if (task.scheduleInterval > 0) {
                val followUpCalendar = Calendar.getInstance()
                followUpCalendar.time = scheduleDay.time

                // Limit to 2 follow-up tasks instead of 3
                val maxFollowUps = 2

                for (followUpIndex in 1..maxFollowUps) {
                    followUpCalendar.add(Calendar.DAY_OF_MONTH, task.scheduleInterval)

                    val followUpId = "followup_${plantId}_${conditionName}_${System.currentTimeMillis() + followUpIndex * 100}"
                    val followUpEvent = PlantDatabaseManager.PlantCareEvent(
                        id = followUpId,
                        plantId = plantId,
                        eventType = "Treat: $conditionName",
                        date = followUpCalendar.time,
                        conditionName = conditionName,
                        notes = "Follow-up #$followUpIndex: ${task.taskName} $plantCountText\n\n${task.description}\n\nMaterials: ${task.materials.joinToString(", ")}\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
                        completed = false
                    )

                    plantDatabaseManager.addPlantCareEvent(followUpEvent)
                }
            }
        }
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector?.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
    }

    // Show dialog with plant condition information, prevention and treatment tips
    private fun showPlantInfoDialog(
        conditionName: String,
        showMonitoringOptions: Boolean = false,
        isRescan: Boolean = false
    ) {
        // Get condition data
        val condition = PlantConditionData.conditions[conditionName] ?: return

        // Create dialog with custom layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_info, null)

        // Set condition details
        val conditionTitle = dialogView.findViewById<TextView>(R.id.conditionTitle)
        val conditionDescription = dialogView.findViewById<TextView>(R.id.conditionDescription)
        val preventionTipsContainer =
            dialogView.findViewById<LinearLayout>(R.id.preventionTipsContainer)
        val treatmentTipsContainer =
            dialogView.findViewById<LinearLayout>(R.id.treatmentTipsContainer)
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val monitoringSection = dialogView.findViewById<View>(R.id.monitoringSection)
        val setReminderButton =
            dialogView.findViewById<android.widget.Button>(R.id.setReminderButton)
        val taskCompleteButton =
            dialogView.findViewById<android.widget.Button>(R.id.taskCompleteButton)
        val addToCalendarButton =
            dialogView.findViewById<android.widget.Button>(R.id.addToCalendarButton)
        val goToManagementButton =
            dialogView.findViewById<android.widget.Button>(R.id.goToManagementButton) // Add this to your layout

        conditionTitle.text = condition.name
        conditionDescription.text = condition.description

        // Add prevention tips
        preventionTipsContainer.removeAllViews()
        condition.preventionTips.forEach { tip ->
            val tipView = LayoutInflater.from(this)
                .inflate(R.layout.item_prevention_tip, preventionTipsContainer, false)
            tipView.findViewById<TextView>(R.id.tipText).text = tip
            preventionTipsContainer.addView(tipView)
        }

        // Add treatment tips
        treatmentTipsContainer.removeAllViews()
        condition.treatmentTips.forEach { tip ->
            val tipView = LayoutInflater.from(this)
                .inflate(R.layout.item_treatment_tip, treatmentTipsContainer, false)
            tipView.findViewById<TextView>(R.id.tipText).text = tip
            treatmentTipsContainer.addView(tipView)
        }

        // Configure the monitoring section based on scan type
        if (isRescan && selectedPlantId != null) {
            // This is a rescan of an existing plant - show Go to Management button
            monitoringSection.visibility = View.VISIBLE

            // Hide the regular buttons
            setReminderButton.visibility = View.GONE
            taskCompleteButton.visibility = View.GONE
            addToCalendarButton.visibility = View.GONE

            // Show the go to management button
            goToManagementButton.visibility = View.VISIBLE

            // Set go to management button listener
            goToManagementButton.setOnClickListener {
                val intent = Intent(this, PlantManagementActivity::class.java)
                intent.putExtra("OPEN_PLANT_ID", selectedPlantId)
                startActivity(intent)
                finish() // Close this activity
            }
        } else if (showMonitoringOptions && selectedPlantId != null) {
            // Regular scan for an existing plant
            monitoringSection.visibility = View.VISIBLE
            goToManagementButton.visibility = View.GONE
            setReminderButton.visibility = View.VISIBLE
            taskCompleteButton.visibility = View.VISIBLE
            addToCalendarButton.visibility = View.GONE

            // Set reminder button
            setReminderButton.setOnClickListener {
                dialogShowDateTimePicker(conditionName)
            }

            // Task complete button
            taskCompleteButton.setOnClickListener {
                markTreatmentComplete(conditionName)
                Toast.makeText(this, "Task marked as complete", Toast.LENGTH_SHORT).show()
                infoDialog?.dismiss()

                // If this was a disease/pest condition, ask if it's resolved
                if (!conditionName.startsWith("Healthy")) {
                    showResolutionConfirmationDialog()
                }
            }
        } else if (showMonitoringOptions) {
            // This is a one-time scan, show option to add to my plants
            monitoringSection.visibility = View.VISIBLE
            setReminderButton.visibility = View.GONE
            taskCompleteButton.visibility = View.GONE
            goToManagementButton.visibility = View.GONE
            addToCalendarButton.visibility = View.VISIBLE

            // Add to calendar button
            addToCalendarButton.setOnClickListener {
                showAddToCalendarDialog(conditionName)
            }
        } else {
            monitoringSection.visibility = View.GONE
        }

        // Set up close button
        closeButton.setOnClickListener {
            infoDialog?.dismiss()

            // If we came from the Plant Management screen and this was a rescan, go back there
            if (selectedPlantId != null && isRescan) {
                val intent = Intent(this, PlantManagementActivity::class.java)
                intent.putExtra("OPEN_PLANT_ID", selectedPlantId)
                startActivity(intent)
                finish()
            }
        }

        // Create and show the dialog
        infoDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        infoDialog?.setOnDismissListener {
            // Ensure the reference is cleared
            infoDialog = null
        }

        infoDialog?.show()
    }
    private fun showAddToCalendarDialog(conditionName: String) {
        try {
            // Stop camera operations before showing dialog
            stopCameraOperations()

            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_calendar, null)

            val detectionResultText = dialogView.findViewById<TextView>(R.id.detectionResultText)
            val plantNameInput = dialogView.findViewById<EditText>(R.id.plantNameInput)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val addPlantButton = dialogView.findViewById<Button>(R.id.addPlantButton)

            // Null checks with early return
            if (detectionResultText == null || plantNameInput == null ||
                cancelButton == null || addPlantButton == null) {
                Log.e("MainActivity", "Missing views in dialog layout")
                Toast.makeText(this, "Error creating dialog", Toast.LENGTH_SHORT).show()
                return
            }

            // Set detection result text
            detectionResultText.text = "Detected condition: $conditionName"

            // Determine plant type and suggest name
            val baseType = when {
                conditionName.contains("Tomato", ignoreCase = true) -> "Tomato"
                conditionName.contains("Eggplant", ignoreCase = true) -> "Eggplant"
                else -> selectedVegetable ?: "Plant"
            }

            // Get existing plant count safely
            val existingPlantsCount = try {
                plantDatabaseManager.getAllPlants().size
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting plant count", e)
                0
            }

            // Suggest plant name
            val suggestedName = "$baseType Plant ${existingPlantsCount + 1}"
            plantNameInput.setText(suggestedName)

            // Cancel button
            cancelButton.setOnClickListener {
                infoDialog?.dismiss()
                finish() // Close the activity since camera is stopped
            }

            // Add plant button with enhanced error handling
            addPlantButton.setOnClickListener {
                addPlantButton.isEnabled = false

                val plantName = plantNameInput.text.toString().trim()
                if (plantName.isEmpty()) {
                    Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show()
                    addPlantButton.isEnabled = true
                    return@setOnClickListener
                }

                // Show progress dialog
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Adding plant...")
                    setCancelable(false)
                    show()
                }

                // Background thread for database operations
                Thread {
                    try {
                        // Determine plant type
                        val plantType = when {
                            conditionName.contains("Tomato", ignoreCase = true) -> "Tomato"
                            conditionName.contains("Eggplant", ignoreCase = true) -> "Eggplant"
                            else -> selectedVegetable ?: "Tomato"
                        }

                        // Add plant to database
                        val plantId = plantDatabaseManager.addDetectionAsPlant(
                            plantName = plantName,
                            vegetableType = plantType,
                            conditionName = conditionName
                        )

                        // Handle UI on main thread
                        runOnUiThread {
                            try {
                                progressDialog.dismiss()

                                if (plantId.isNotEmpty()) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Plant added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Create care plan and navigate in background
                                    Thread {
                                        try {
                                            createCompletePlantCarePlan(plantId, plantType, 2)

                                            // Navigate to Plant Management on main thread
                                            runOnUiThread {
                                                infoDialog?.dismiss()
                                                navigateToPlantManagement(plantId, true)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Care plan error: ${e.message}", e)
                                            runOnUiThread {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Error creating care plan",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                finish() // Close activity on error
                                            }
                                        }
                                    }.start()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to add plant",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    addPlantButton.isEnabled = true
                                    finish() // Close activity on failure
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "UI update error: ${e.message}", e)
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                addPlantButton.isEnabled = true
                                finish() // Close activity on error
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            progressDialog.dismiss()
                            Log.e("MainActivity", "Plant add error: ${e.message}", e)
                            Toast.makeText(
                                this@MainActivity,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            addPlantButton.isEnabled = true
                            finish() // Close activity on error
                        }
                    }
                }.start()
            }

            // Create and show dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false) // Prevent dismissing
                .create()

            dialog.setOnDismissListener {
                infoDialog = null
                finish() // Close activity if dialog is dismissed
            }
            infoDialog?.dismiss()
            infoDialog = dialog
            dialog.show()

        } catch (e: Exception) {
            Log.e("MainActivity", "Dialog creation error: ${e.message}", e)
            Toast.makeText(this, "Error showing dialog", Toast.LENGTH_SHORT).show()
            finish() // Close activity on error
        }
    }
    private fun navigateToPlantManagement(plantId: String) {
        try {
            val intent = Intent(this, PlantManagementActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("OPEN_PLANT_ID", plantId)
            startActivity(intent)
            finish() // Close current activity
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation error: ${e.message}", e)
            Toast.makeText(this, "Error opening plant management", Toast.LENGTH_SHORT).show()
        }
    }
    private fun dialogShowDateTimePicker(conditionName: String) {
        // Only proceed if we have a plant ID
        if (selectedPlantId == null) return

        // Show a date picker dialog
        val calendar = Calendar.getInstance()

        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // After date is selected, show time picker
                val timePickerDialog = android.app.TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Create a treatment event
                        val eventId = "treatment_${selectedPlantId}_${System.currentTimeMillis()}"
                        val treatmentEvent = PlantDatabaseManager.PlantCareEvent(
                            id = eventId,
                            plantId = selectedPlantId!!,
                            eventType = "Treatment",
                            date = calendar.time,
                            conditionName = conditionName,
                            notes = "Scheduled treatment for $conditionName",
                            completed = false
                        )

                        // Save event
                        plantDatabaseManager.addPlantCareEvent(treatmentEvent)

                        Toast.makeText(
                            this,
                            "Treatment scheduled for ${
                                java.text.SimpleDateFormat(
                                    "MMM d, yyyy HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(calendar.time)
                            }",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun markTreatmentComplete(conditionName: String) {
        // Only proceed if we have a plant ID
        if (selectedPlantId == null) return

        // Find any incomplete treatment events for this condition
        val plantEvents = plantDatabaseManager.getPlantCareEvents(selectedPlantId!!)
        val treatmentEvent = plantEvents.firstOrNull {
            it.eventType == "Treatment" &&
                    it.conditionName == conditionName &&
                    !it.completed
        }

        // If found, mark as complete
        treatmentEvent?.let {
            val updatedEvent = it.copy(completed = true)
            plantDatabaseManager.updatePlantCareEvent(updatedEvent)
        } ?: run {
            // If no event found, create a new completed event
            val eventId = "treatment_${selectedPlantId}_${System.currentTimeMillis()}"
            val completedEvent = PlantDatabaseManager.PlantCareEvent(
                id = eventId,
                plantId = selectedPlantId!!,
                eventType = "Treatment",
                date = Date(),
                conditionName = conditionName,
                notes = "Treatment completed for $conditionName",
                completed = true
            )

            plantDatabaseManager.addPlantCareEvent(completedEvent)
        }
    }

    private fun showResolutionConfirmationDialog() {
        // Only proceed if we have a plant ID
        if (selectedPlantId == null) return

        // Dismiss any existing dialog first
        infoDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Issue Resolved?")
            .setMessage("Has the plant condition been resolved?")
            .setPositiveButton("Yes") { dialogInterface, _ ->
                // ... existing code ...
                dialogInterface.dismiss()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.setOnDismissListener {
            infoDialog = null
        }

        dialog.show()
        infoDialog = dialog
    }
    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()

        // Explicitly dismiss any open dialogs
        infoDialog?.dismiss()
        infoDialog = null
    }
    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
        }
    }



    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"

            // Only show detection results if we're actively scanning
            if (isScanning) {
                binding.overlay.apply {
                    setResults(boundingBoxes)
                    invalidate()
                }

                // Store all valid detections with confidence above threshold
                val validDetections = boundingBoxes.filter { it.cnf > MIN_CONFIDENCE_THRESHOLD }

                if (validDetections.isNotEmpty()) {
                    // Clear previous detections if they're different types
                    if (currentDetections.isNotEmpty() &&
                        !isSameVegetableType(
                            currentDetections.first().clsName,
                            validDetections.first().clsName
                        )
                    ) {
                        currentDetections.clear()
                    }

                    // Add new valid detections
                    currentDetections.clear() // First clear existing
                    currentDetections.addAll(validDetections)

                    // Count total plants and conditions
                    val totalPlants = validDetections.size
                    val uniqueConditions = validDetections.map { it.clsName }.distinct().size

                    // Use the highest confidence detection for display
                    val highestConfBox = validDetections.maxByOrNull { it.cnf }

                    binding.captureButton.isEnabled = true
                    binding.detectionText.text =
                        "$totalPlants plants detected: $uniqueConditions conditions"
                    binding.detectionText.visibility = View.VISIBLE
                } else {
                    binding.captureButton.isEnabled = false
                    binding.detectionText.visibility = View.INVISIBLE
                    currentDetections.clear()
                }
            }
        }
    }
}

//    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
//        runOnUiThread {
//            binding.inferenceTime.text = "${inferenceTime}ms"
//
//            // Only show detection results if we're actively scanning
//            if (isScanning) {
//                binding.overlay.apply {
//                    setResults(boundingBoxes)
//                    invalidate()
//                }
//
//                // Store the highest confidence detection for when the user wants to capture it
//                if (boundingBoxes.isNotEmpty()) {
//                    val highestConfBox = boundingBoxes.maxByOrNull { it.cnf }
//
//                    if (highestConfBox != null && highestConfBox.cnf > MIN_CONFIDENCE_THRESHOLD) {
//                        currentDetection = highestConfBox
//                        binding.captureButton.isEnabled = true
//                        binding.detectionText.text = "${highestConfBox.clsName}: ${(highestConfBox.cnf * 100).toInt()}%"
//                        binding.detectionText.visibility = View.VISIBLE
//                    } else {
//                        binding.captureButton.isEnabled = false
//                        binding.detectionText.visibility = View.INVISIBLE
//                        currentDetection = null
//                    }
//                } else {
//                    binding.captureButton.isEnabled = false
//                    binding.detectionText.visibility = View.INVISIBLE
//                    currentDetection = null
//                }
//            }
//        }
//    }
//}