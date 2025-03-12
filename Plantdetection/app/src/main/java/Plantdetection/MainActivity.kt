package com.PlantDetection

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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

            // Instead of setting a single primary condition, create a summarized status string
            // that shows counts for each condition
            val healthyConditions = detectionsByCondition.keys.filter { it.startsWith("Healthy") }
            val diseaseConditions = detectionsByCondition.keys.filter { !it.startsWith("Healthy") }

            // Build a status string like "2 Healthy, 1 Disease" instead of just "Healthy Eggplant"
            val statusBuilder = StringBuilder()

            // Add healthy plants count
            val healthyCount = healthyConditions.sumOf { detectionsByCondition[it]?.size ?: 0 }
            if (healthyCount > 0) {
                statusBuilder.append("$healthyCount Healthy")
            }

            // Add disease plants count
            val diseaseCount = diseaseConditions.sumOf { detectionsByCondition[it]?.size ?: 0 }
            if (diseaseCount > 0) {
                if (statusBuilder.isNotEmpty()) {
                    statusBuilder.append(", ")
                }
                statusBuilder.append("$diseaseCount Diseased")
            }

            // Create the status string
            val statusString = statusBuilder.toString()

            // Set primary condition for database purposes - still use the highest priority one
            val primaryCondition = getPrimaryCondition(detectionsByCondition.keys.toList())

            // Check if this is a new status
            val isNewCondition = plant.currentCondition != statusString
            val isNowHealthy = diseaseCount == 0

            // Update plant with new status string AND primary condition in notes
            val updatedPlant = it.copy(
                currentCondition = statusString, // Use our custom status string like "2 Healthy, 1 Diseased"
                lastScannedDate = Date()
            )

            // Save new condition information in plant notes
            val conditionSummary = "Multiple plant detection (${Date()}):\n" +
                    detectionsByCondition.entries.joinToString("\n") { entry ->
                        "- ${entry.key}: ${entry.value.size} plants"
                    }

            // Update notes, keeping other information but replacing any previous detection reports
            val baseNotes = plant.notes.substringBefore("Multiple plant detection").trim()
            val updatedNotes = if (baseNotes.isEmpty()) conditionSummary else "$baseNotes\n\n$conditionSummary"

            val finalPlant = updatedPlant.copy(notes = updatedNotes)

            // Save to database
            if (plantDatabaseManager.updatePlant(finalPlant)) {
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

                // Mark treatments as completed for conditions that are no longer present
                for (resolvedCondition in resolvedConditions) {
                    markTreatmentsForConditionAsCompleted(plantId, resolvedCondition)
                }

                // If plant was healthy and now has a disease
                if (wasHealthy && !isNowHealthy) {
                    // Create treatments for all diseased conditions
                    for (condition in detectionsByCondition.keys) {
                        if (!condition.startsWith("Healthy")) {
                            createAutomaticTreatmentSchedule(plantId, condition)
                        }
                    }

                    // Show plant info dialog with all conditions
                    showMultiplePlantInfoDialog(detectionsByCondition, true, true)

                    Toast.makeText(this, "Treatment plans created for detected conditions", Toast.LENGTH_SHORT).show()
                }
                // If changing from diseased to healthy, show congratulations
                else if (!wasHealthy && isNowHealthy) {
                    // Mark all treatments as completed
                    markRemainingTreatmentsAsCompleted(plantId)

                    // Show recovery dialog
                    AlertDialog.Builder(this)
                        .setTitle("Plants Recovered!")
                        .setMessage("Great news! Your ${plant.name} plants have recovered and are now healthy. All treatments have been marked as completed.")
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
                    // Mark old treatments as completed for conditions that are no longer present
                    for (resolvedCondition in resolvedConditions) {
                        markTreatmentsForConditionAsCompleted(plantId, resolvedCondition)
                    }

                    // Create new treatment plans for newly detected conditions
                    for (condition in detectionsByCondition.keys) {
                        if (!condition.startsWith("Healthy") &&
                            !previousConditions.keys.contains(condition)) {
                            createAutomaticTreatmentSchedule(plantId, condition)
                        }
                    }

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
    private fun showAddMultiplePlantsDialog(detectionsByCondition: Map<String, List<BoundingBox>>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_calendar, null)

        // Initialize views
        val detectionResultText = dialogView.findViewById<TextView>(R.id.detectionResultText)
        val plantNameInput = dialogView.findViewById<android.widget.EditText>(R.id.plantNameInput)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addPlantButton = dialogView.findViewById<Button>(R.id.addPlantButton)

        // Get total plants and conditions summary
        val totalPlants = detectionsByCondition.values.sumOf { it.size }
        val totalConditions = detectionsByCondition.size

        // Set initial values
        detectionResultText.text = "Detected: $totalPlants plants with $totalConditions conditions"

        // Fixed default watering frequency
        val wateringFrequency = 2 // Default to 2 days

        // Hide watering frequency controls
        val wateringFrequencyText = dialogView.findViewById<TextView>(R.id.wateringFrequencyText)
        val decreaseFrequencyButton = dialogView.findViewById<Button>(R.id.decreaseFrequencyButton)
        val increaseFrequencyButton = dialogView.findViewById<Button>(R.id.increaseFrequencyButton)

        wateringFrequencyText.visibility = View.GONE
        decreaseFrequencyButton.visibility = View.GONE
        increaseFrequencyButton.visibility = View.GONE

        // Determine vegetable type from conditions
        val vegetableType = when {
            detectionsByCondition.keys.any { it.contains("Tomato", ignoreCase = true) } -> "Tomato"
            detectionsByCondition.keys.any { it.contains("Eggplant", ignoreCase = true) } -> "Eggplant"
            else -> selectedVegetable ?: "Tomato"
        }

        // Suggest a default name
        val suggestedName = "$vegetableType Group (${totalPlants} plants, ${totalConditions} conditions)"
        plantNameInput.setText(suggestedName)

        cancelButton.setOnClickListener {
            // Just dismiss the dialog
            infoDialog?.dismiss()
        }

        addPlantButton.setOnClickListener {
            val plantName = plantNameInput.text.toString().trim()

            if (plantName.isEmpty()) {
                Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add the multiple plants as a group
            val plantId = addMultiplePlantsAsGroup(
                plantName = plantName,
                vegetableType = vegetableType,
                detectionsByCondition = detectionsByCondition,
                wateringFrequency = wateringFrequency
            )

            if (plantId.isNotEmpty()) {
                // Create comprehensive care plan for all plants in the group
                createCompletePlantCarePlan(plantId, vegetableType, wateringFrequency)

                Toast.makeText(this, "Plant group added to monitoring with complete care schedule", Toast.LENGTH_SHORT).show()

                // Dismiss dialogs
                infoDialog?.dismiss()

                // Go to plant management screen
                val intent = Intent(this, PlantManagementActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to add plant group", Toast.LENGTH_SHORT).show()
            }
        }

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        // Replace current info dialog with this new one
        infoDialog?.dismiss()
        infoDialog = dialog
    }
    // 7. Create a method to add multiple plants as a group
    private fun addMultiplePlantsAsGroup(
        plantName: String,
        vegetableType: String,
        detectionsByCondition: Map<String, List<BoundingBox>>,
        wateringFrequency: Int
    ): String {
        // Create summary status string
        val healthyConditions = detectionsByCondition.keys.filter { it.startsWith("Healthy") }
        val diseaseConditions = detectionsByCondition.keys.filter { !it.startsWith("Healthy") }

        // Build a status string like "2 Healthy, 1 Diseased" instead of just using primary condition
        val statusBuilder = StringBuilder()

        // Add healthy plants count
        val healthyCount = healthyConditions.sumOf { detectionsByCondition[it]?.size ?: 0 }
        if (healthyCount > 0) {
            statusBuilder.append("$healthyCount Healthy")
        }

        // Add disease plants count
        val diseaseCount = diseaseConditions.sumOf { detectionsByCondition[it]?.size ?: 0 }
        if (diseaseCount > 0) {
            if (statusBuilder.isNotEmpty()) {
                statusBuilder.append(", ")
            }
            statusBuilder.append("$diseaseCount Diseased")
        }

        // Our custom status string
        val statusString = statusBuilder.toString()

        // Get primary condition for the plant record (still needed for some operations)
        val conditions = detectionsByCondition.keys.toList()
        val primaryCondition = getPrimaryCondition(conditions)

        // Create a plant ID
        val plantId = "plant_${System.currentTimeMillis()}"

        // Create plant notes with detailed detection information
        val totalPlants = detectionsByCondition.values.sumOf { it.size }
        val conditionCounts = detectionsByCondition.entries.joinToString("\n") { (condition, detections) ->
            "- $condition: ${detections.size} plants"
        }

        // Include details of all detected conditions in notes
        val notes = "Plant group containing $totalPlants plants with ${detectionsByCondition.size} different conditions:\n$conditionCounts\n\nDetected on: ${Date()}"

        // Create the plant record
        val plant = PlantDatabaseManager.Plant(
            id = plantId,
            name = plantName,
            type = vegetableType,
            createdDate = Date(),
            lastScannedDate = Date(),
            currentCondition = statusString, // Use our custom status string
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

            // Return the plant ID - DON'T create treatments here to avoid duplication
            // The createCompletePlantCarePlan will handle creating all treatments
            return plantId
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

            // Update plant condition
            val updatedPlant = it.copy(
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

                // If changing from healthy to diseased, create treatment
                if (wasHealthy && !isNowHealthy) {
                    createAutomaticTreatmentSchedule(plantId, conditionName)

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
                    // Mark any remaining treatment tasks as completed
                    markRemainingTreatmentsAsCompleted(plantId)

                    // Show recovery dialog directly
                    AlertDialog.Builder(this)
                        .setTitle("Plant Recovered!")
                        .setMessage("Great news! Your ${plant.name} has recovered and is now healthy. All treatments have been marked as completed.")
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
                    // Mark old treatments as completed
                    markRemainingTreatmentsAsCompleted(plantId)

                    // Create new treatment plan
                    createAutomaticTreatmentSchedule(plantId, conditionName)

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

    private fun createAutomaticTreatmentSchedule(plantId: String, conditionName: String) {
        // First check if treatments already exist for this condition to avoid duplication
        val existingTreatments = plantDatabaseManager.getPlantCareEvents(plantId)
            .filter { event ->
                (event.eventType.startsWith("Treat: $conditionName") ||
                        (event.eventType == "Treatment" && event.conditionName == conditionName)) &&
                        !event.completed
            }

        // If treatments already exist, don't create new ones
        if (existingTreatments.isNotEmpty()) {
            Log.d("PlantManagement", "Treatments already exist for $conditionName - skipping creation")
            return
        }

        // Get the condition data
        val condition = PlantConditionData.conditions[conditionName]
        val plant = plantDatabaseManager.getPlant(plantId)

        if (condition != null && plant != null) {
            // Create a treatment plan title
            val treatmentTitle = "Treatment Plan for ${condition.name}"

            // Show a dialog to inform the user
            AlertDialog.Builder(this)
                .setTitle("Treatment Plan Created")
                .setMessage("A treatment plan has been automatically created for the detected condition: ${condition.name}. Do you want to view the treatment details?")
                .setPositiveButton("View Treatment Plan") { _, _ ->
                    // Take the user to the plant management screen
                    val intent = Intent(this, PlantManagementActivity::class.java)
                    intent.putExtra("OPEN_PLANT_ID", plantId)
                    intent.putExtra("SHOW_TREATMENT_PLAN", true)
                    startActivity(intent)
                }
                .setNegativeButton("Not Now", null)
                .show()

            // Schedule treatments for each treatment task in the condition
            for ((index, task) in condition.treatmentTasks.withIndex()) {
                // Create initial treatment task for today
                val taskId = "treatment_${plantId}_${System.currentTimeMillis() + index}"
                val today = Calendar.getInstance()

                // Set appropriate times for different tasks
                when {
                    task.taskName.contains(
                        "Remove",
                        ignoreCase = true
                    ) -> today.set(Calendar.HOUR_OF_DAY, 10)

                    task.taskName.contains(
                        "Apply",
                        ignoreCase = true
                    ) -> today.set(Calendar.HOUR_OF_DAY, 17)

                    else -> today.set(Calendar.HOUR_OF_DAY, 12)
                }
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)

                // Determine default treatment notes using the first treatment task for the condition
                val defaultNotes = PlantConditionData.conditions[conditionName]
                    ?.treatmentTasks?.firstOrNull()
                    ?.let { firstTask ->
                        "${firstTask.taskName}: ${firstTask.description}\n\nTreatment: ${
                            firstTask.materials.joinToString(
                                ", "
                            )
                        }\n\nInstructions:\n${firstTask.instructions.joinToString("\n- ", "- ")}"
                    } ?: "No specific treatment details available"

                // Create treatment event with disease name in title
                val treatmentEvent = PlantDatabaseManager.PlantCareEvent(
                    id = taskId,
                    plantId = plantId,
                    eventType = "Treat: ${condition.name}",
                    date = today.time,
                    conditionName = condition.name,
                    notes = defaultNotes,
                    completed = false
                )

                // Add initial treatment task
                plantDatabaseManager.addPlantCareEvent(treatmentEvent)

                // Add follow-up tasks based on the schedule interval
                if (task.scheduleInterval > 0) {
                    val followUpCalendar = Calendar.getInstance()
                    followUpCalendar.time = today.time

                    // Create up to 3 follow-up tasks
                    val maxFollowUps = 3

                    for (followUpIndex in 1..maxFollowUps) {
                        followUpCalendar.add(Calendar.DAY_OF_MONTH, task.scheduleInterval)

                        val followUpId =
                            "followup_${plantId}_${System.currentTimeMillis() + index + followUpIndex * 100}"
                        val followUpEvent = PlantDatabaseManager.PlantCareEvent(
                            id = followUpId,
                            plantId = plantId,
                            eventType = "Treat: ${condition.name}",
                            date = followUpCalendar.time,
                            conditionName = condition.name,
                            notes = defaultNotes,
                            completed = false
                        )

                        plantDatabaseManager.addPlantCareEvent(followUpEvent)
                    }
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

        infoDialog?.show()
    }

    private fun showAddToCalendarDialog(conditionName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_calendar, null)

        // Initialize views
        val detectionResultText = dialogView.findViewById<TextView>(R.id.detectionResultText)
        val plantNameInput = dialogView.findViewById<EditText>(R.id.plantNameInput)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addPlantButton = dialogView.findViewById<Button>(R.id.addPlantButton)

        // Set initial values
        detectionResultText.text = "Detected condition: $conditionName"

        // Suggest a default name
        val suggestedName = when {
            conditionName.contains("Tomato") -> "Tomato Plant"
            conditionName.contains("Eggplant") -> "Eggplant Plant"
            else -> "Plant"
        } + " " + (plantDatabaseManager.getAllPlants().size + 1)
        plantNameInput.setText(suggestedName)

        // Hide watering frequency controls - we'll set a default
        val wateringFrequencyText = dialogView.findViewById<TextView>(R.id.wateringFrequencyText)
        val decreaseFrequencyButton = dialogView.findViewById<Button>(R.id.decreaseFrequencyButton)
        val increaseFrequencyButton = dialogView.findViewById<Button>(R.id.increaseFrequencyButton)

        wateringFrequencyText.visibility = View.GONE
        decreaseFrequencyButton.visibility = View.GONE
        increaseFrequencyButton.visibility = View.GONE

        // Fixed default watering frequency
        val wateringFrequency = 2 // Default to 2 days

        cancelButton.setOnClickListener {
            // Just dismiss the dialog
            infoDialog?.dismiss()
        }

        addPlantButton.setOnClickListener {
            val plantName = plantNameInput.text.toString().trim()

            if (plantName.isEmpty()) {
                Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add the plant to the database
            val plantType = when {
                conditionName.contains("Tomato") -> "Tomato"
                conditionName.contains("Eggplant") -> "Eggplant"
                else -> selectedVegetable ?: "Tomato"
            }

            try {
                val plantId = plantDatabaseManager.addDetectionAsPlant(
                    plantName = plantName,
                    vegetableType = plantType,
                    conditionName = conditionName
                )

                if (plantId.isNotEmpty()) {
                    // Update the plant with the custom watering frequency
                    val plant = plantDatabaseManager.getPlant(plantId)
                    plant?.let {
                        val updatedPlant = it.copy(wateringFrequency = wateringFrequency)
                        plantDatabaseManager.updatePlant(updatedPlant)

                        // Create complete care plan for the plant
                        createCompletePlantCarePlan(plantId, plantType, wateringFrequency)
                    }

                    Toast.makeText(this, "Plant added to monitoring with complete care schedule", Toast.LENGTH_SHORT).show()

                    // Dismiss dialogs
                    infoDialog?.dismiss()

                    // Go to plant management screen safely
                    try {
                        val intent = Intent(this, PlantManagementActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error navigating to Plant Management: ${e.message}")
                        Toast.makeText(this, "Plant added successfully. Please go back to home.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Failed to add plant", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error adding plant: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        // Replace current info dialog with this new one
        infoDialog?.dismiss()
        infoDialog = dialog
    }
    private fun createCompletePlantCarePlan(plantId: String, plantType: String, wateringFrequency: Int) {
        val calendar = Calendar.getInstance()
        val plant = plantDatabaseManager.getPlant(plantId)

        // Check if this is a plant group
        val isPlantGroup = plant?.name?.contains("(") == true && plant.name.contains("plants")
        var totalPlantsInGroup = 1
        val diseaseConditions = mutableMapOf<String, Int>()

        // For plant groups, extract condition counts
        if (isPlantGroup && plant != null) {
            // Extract total plant count from name
            val match = "\\((\\d+)\\s+plants".toRegex().find(plant.name)
            if (match != null) {
                totalPlantsInGroup = match.groupValues[1].toIntOrNull() ?: 1
            }

            // Parse conditions from notes
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

        // 1. Schedule watering for TODAY instead of tomorrow
        val initialWateringDate = Calendar.getInstance().apply {
            // No addition of days - set it to today
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        plantDatabaseManager.scheduleWatering(plantId, initialWateringDate,
            if (isPlantGroup) "Initial watering for all $totalPlantsInGroup plants" else "Initial watering")

        // 2. Schedule regular watering for the next 30 days
        val wateringCalendar = Calendar.getInstance()
        wateringCalendar.time = initialWateringDate

        for (day in 2..30 step wateringFrequency) {
            wateringCalendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)
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
        }

        // Rest of the method remains the same...
        // 3. Schedule fertilizing every 2 weeks for all plants
        val fertilizingCalendar = Calendar.getInstance()
        fertilizingCalendar.time = initialWateringDate
        fertilizingCalendar.add(Calendar.DAY_OF_MONTH, 7) // First fertilizing after 1 week
        fertilizingCalendar.set(Calendar.HOUR_OF_DAY, 10) // Set to 10 AM

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

            // Next fertilizing in 2 weeks
            fertilizingCalendar.add(Calendar.DAY_OF_MONTH, 14)
        }

        // 4. Schedule maintenance checks every 10 days for all plants
        val maintenanceCalendar = Calendar.getInstance()
        maintenanceCalendar.time = initialWateringDate
        maintenanceCalendar.add(Calendar.DAY_OF_MONTH, 10)
        maintenanceCalendar.set(Calendar.HOUR_OF_DAY, 15) // Set to 3 PM

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

            // Next maintenance in 10 days
            maintenanceCalendar.add(Calendar.DAY_OF_MONTH, 10)
        }

        // 5. Schedule specific treatments for disease conditions in plant groups
        if (isPlantGroup && diseaseConditions.isNotEmpty()) {
            val treatmentCalendar = Calendar.getInstance()
            treatmentCalendar.add(Calendar.DAY_OF_MONTH, 2) // Start treatments in 2 days
            treatmentCalendar.set(Calendar.HOUR_OF_DAY, 16) // Set to 4 PM

            // Create treatment tasks for each disease
            for ((conditionName, plantCount) in diseaseConditions) {
                val condition = PlantConditionData.conditions[conditionName]
                if (condition != null) {
                    // Schedule treatment tasks
                    for ((index, task) in condition.treatmentTasks.withIndex()) {
                        // Increment time slightly for each task to avoid duplicates
                        treatmentCalendar.add(Calendar.HOUR_OF_DAY, index % 3)

                        val plantCountText = if (plantCount == 1) "(1 plant)" else "($plantCount plants)"

                        val taskId = "treatment_${plantId}_${conditionName}_${System.currentTimeMillis() + index}"
                        val treatmentEvent = PlantDatabaseManager.PlantCareEvent(
                            id = taskId,
                            plantId = plantId,
                            eventType = "Treat: $conditionName",
                            date = treatmentCalendar.time,
                            conditionName = conditionName,
                            notes = "${task.taskName} for $conditionName $plantCountText:\n\n${task.description}\n\nMaterials: ${task.materials.joinToString(", ")}\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
                            completed = false
                        )

                        plantDatabaseManager.addPlantCareEvent(treatmentEvent)

                        // Add follow-up treatments
                        if (task.scheduleInterval > 0) {
                            val followUpCalendar = Calendar.getInstance()
                            followUpCalendar.time = treatmentCalendar.time

                            for (followUp in 1..3) {
                                followUpCalendar.add(Calendar.DAY_OF_MONTH, task.scheduleInterval)

                                val followUpId = "followup_${plantId}_${conditionName}_${System.currentTimeMillis() + index + followUp * 100}"
                                val followUpEvent = PlantDatabaseManager.PlantCareEvent(
                                    id = followUpId,
                                    plantId = plantId,
                                    eventType = "Treat: $conditionName",
                                    date = followUpCalendar.time,
                                    conditionName = conditionName,
                                    notes = "Follow-up #$followUp: ${task.taskName} for $conditionName $plantCountText\n\n${task.description}\n\nMaterials: ${task.materials.joinToString(", ")}\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
                                    completed = false
                                )

                                plantDatabaseManager.addPlantCareEvent(followUpEvent)
                            }
                        }
                    }

                    // Move to next day for next condition's treatments
                    treatmentCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        } else if (!isPlantGroup && plant?.currentCondition != null && !plant.currentCondition.startsWith("Healthy")) {
            // For regular plants with disease, create treatment plan
            createAutomaticTreatmentSchedule(plantId, plant.currentCondition)
        }

        // 6. Schedule health scan every month for all plants
        val scanCalendar = Calendar.getInstance()
        scanCalendar.time = initialWateringDate
        scanCalendar.add(Calendar.MONTH, 1)
        scanCalendar.set(Calendar.HOUR_OF_DAY, 14) // Set to 2 PM

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

        AlertDialog.Builder(this)
            .setTitle("Issue Resolved?")
            .setMessage("Has the plant condition been resolved?")
            .setPositiveButton("Yes") { dialog, _ ->
                // Update plant condition to healthy
                val plant = plantDatabaseManager.getPlant(selectedPlantId!!)
                plant?.let {
                    val healthyCondition =
                        if (it.type == "Tomato") "Healthy Tomato" else "Healthy Eggplant"

                    // Update plant condition
                    val updatedPlant = it.copy(
                        currentCondition = healthyCondition,
                        lastScannedDate = Date()
                    )

                    // Save to database
                    plantDatabaseManager.updatePlant(updatedPlant)

                    Toast.makeText(
                        this,
                        "Great! Plant condition marked as healthy.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                dialog.dismiss()

                // Return to the plant management screen
                val intent = Intent(this, PlantManagementActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                Toast.makeText(this, "Continue following the treatment plan.", Toast.LENGTH_SHORT)
                    .show()
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
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