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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private fun getCategoryFromCondition(condition: String): String {
        return when {
            condition.contains("(Diseased)") -> "Disease"
            condition.contains("(Infested)") -> "Pest"
            condition == "Hippodamia Variegata/Lady Bug" -> "Beneficial"
            condition.startsWith("Healthy") -> "Healthy"
            else -> "Unknown"
        }
    }

    private fun isSameVegetableType(cls1: String, cls2: String): Boolean {
        // Get plant type for each condition
        val type1 = getPlantTypeFromCondition(cls1)
        val type2 = getPlantTypeFromCondition(cls2)

        // If either is Unknown, we should be more lenient with matching
        return type1 == type2 || type1 == "Unknown" || type2 == "Unknown"
    }

    private fun captureDetection() {
        // If we have valid detections, show details and save them
        if (currentDetections.isNotEmpty()) {
            // Group detections by condition
            val detectionsByCondition = currentDetections.groupBy { it.clsName }

            // Categorize detections by type (healthy, diseased, infested)
            val tomatoConditions = mutableListOf<String>()
            val eggplantConditions = mutableListOf<String>()
            val okraConditions = mutableListOf<String>()
            val bitterGourdConditions = mutableListOf<String>()
            val chiliPepperConditions = mutableListOf<String>()
            val generalPests = mutableListOf<String>()
            val beneficialInsects = mutableListOf<String>()

            // Organize detections by plant type and condition
            for (condition in detectionsByCondition.keys) {
                when {
                    // Tomato specific conditions
                    condition == "Blossom End Rot (Tomato) (Diseased)" || condition == "Healthy Tomato" ->
                        tomatoConditions.add(condition)

                    // Eggplant specific conditions
                    condition == "Melon Thrips (Eggplant) (Diseased)" || condition == "Healthy eggplant" ->
                        eggplantConditions.add(condition)

                    // Okra specific conditions
                    condition == "Blossom Blight (Okra) (Diseased)" || condition == "Healthy okra" ->
                        okraConditions.add(condition)

                    // Bitter Gourd specific conditions
                    condition == "Phytophthora Fruit Rot (Bitter Gourd) (Diseased)" || condition == "Healthy bitter gourd" ->
                        bitterGourdConditions.add(condition)

                    // Chili Pepper specific conditions
                    condition == "Anthracnose (Chili Pepper) (Diseased)" || condition == "Healthy Chili Pepper" ->
                        chiliPepperConditions.add(condition)

                    // General pests that affect both
                    condition == "Aphids (Infested)" || condition == "Cutworm (Infested)" || condition == "Fruit Fly (Infested)" ->
                        generalPests.add(condition)

                    // Beneficial insects
                    condition == "Hippodamia Variegata/Lady Bug" ->
                        beneficialInsects.add(condition)
                }
            }

            // Determine the selected vegetable type from the intent or detected conditions
            val selectedVegType = when {
                selectedVegetable != null -> selectedVegetable!!
                tomatoConditions.isNotEmpty() && eggplantConditions.isEmpty() &&
                        okraConditions.isEmpty() && bitterGourdConditions.isEmpty() &&
                        chiliPepperConditions.isEmpty() -> "Tomato"
                eggplantConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                        okraConditions.isEmpty() && bitterGourdConditions.isEmpty() &&
                        chiliPepperConditions.isEmpty() -> "Eggplant"
                okraConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                        eggplantConditions.isEmpty() && bitterGourdConditions.isEmpty() &&
                        chiliPepperConditions.isEmpty() -> "Okra"
                bitterGourdConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                        eggplantConditions.isEmpty() && okraConditions.isEmpty() &&
                        chiliPepperConditions.isEmpty() -> "Bitter Gourd"
                chiliPepperConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                        eggplantConditions.isEmpty() && okraConditions.isEmpty() &&
                        bitterGourdConditions.isEmpty() -> "Chili Pepper"
                else -> "Mixed" // If multiple or none are detected
            }

            // Determine if we have any conditions relevant to the detected plants
            val relevantConditions = when (selectedVegType) {
                "Tomato" -> tomatoConditions + generalPests + beneficialInsects
                "Eggplant" -> eggplantConditions + generalPests + beneficialInsects
                "Okra" -> okraConditions + generalPests + beneficialInsects
                "Bitter Gourd" -> bitterGourdConditions + generalPests + beneficialInsects
                "Chili Pepper" -> chiliPepperConditions + generalPests + beneficialInsects
                else -> tomatoConditions + eggplantConditions + okraConditions +
                        bitterGourdConditions + chiliPepperConditions +
                        generalPests + beneficialInsects
            }

            // Make sure we have a valid condition that can be treated
            if (relevantConditions.isNotEmpty() &&
                relevantConditions.any { PlantConditionData.conditions.containsKey(it) }
            ) {
                // First stop scanning
                if (isScanning) {
                    toggleScanning()
                }

                // Filter detections to only include relevant conditions
                val relevantDetections = detectionsByCondition.filter { (key, _) ->
                    relevantConditions.contains(key)
                }

                // If we have a plant ID, update the plant's conditions
                if (selectedPlantId != null) {
                    updatePlantWithMultipleConditions(selectedPlantId!!, relevantDetections)
                } else {
                    // Show info dialog for fresh scan with multiple plants
                    showMultiplePlantInfoDialog(relevantDetections, true, false)
                }
            } else {
                // If detected plants don't match selected type or no valid conditions
                showWrongVegetableDialog(selectedVegType)
            }
        } else {
            Toast.makeText(this, "No valid detections to capture", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showWrongVegetableDialog(selectedVegType: String) {
        // Stop scanning while showing the dialog
        if (isScanning) {
            toggleScanning()
        }

        // Create the dialog
        AlertDialog.Builder(this)
            .setTitle("Wrong Plant Type Detected")
            .setMessage("The detected plant doesn't match your selected plant type ($selectedVegType). Would you like to go back to plant selection?")
            .setPositiveButton("Go to Selection") { _, _ ->
                // Navigate back to vegetable selection activity
                val intent = Intent(this, VegetableSelectionActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Stay Here") { dialog, _ ->
                // Just dismiss the dialog and restart scanning
                dialog.dismiss()
                // Restart scanning if it was previously active
                if (!isScanning) {
                    toggleScanning()
                }
            }
            .setCancelable(false)
            .show()
    }

    // 3. Add method to handle updating a plant with multiple conditions
    private fun updatePlantWithMultipleConditions(
        plantId: String,
        detectionsByCondition: Map<String, List<BoundingBox>>
    ) {
        // Stop camera and scanning to prevent resource issues
        if (isScanning) {
            toggleScanning()
        }
        stopCamera() // Stop camera to free resources

        // Show loading indicator
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Updating plants...")
            setCancelable(false)
        }
        progressDialog.show()

        // Run the update operation in a background thread to prevent UI freezing
        Thread {
            try {
                val plant = plantDatabaseManager.getPlant(plantId)
                plant?.let {
                    // Get current plant data
                    val wasHealthy = plant.currentCondition?.startsWith("Healthy") ?: true

                    // Categorize detected conditions
                    val pestConditions = mutableListOf<String>()
                    val diseaseConditions = mutableListOf<String>()
                    val healthyConditions = mutableListOf<String>()

                    // Organize detections by condition type
                    for (condition in detectionsByCondition.keys) {
                        when (condition) {
                            // Pests
                            "Hippodamia Variegata/Lady Bug", "Fruit Fly (Infested)", "Cutworm (Infested)", "Aphids (Infested)" ->
                                pestConditions.add(condition)

                            // Healthy plants
                            "Healthy Tomato", "Healthy Eggplant" ->
                                healthyConditions.add(condition)

                            // Diseases (everything else)
                            else -> diseaseConditions.add(condition)
                        }
                    }

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
                        if (line.trim()
                                .startsWith("-") && line.contains(":") && line.contains("plants")
                        ) {
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

                    // Set primary condition to the highest priority one
                    // (pest or disease takes priority over healthy)
                    val primaryCondition = if (pestConditions.isNotEmpty()) {
                        // Pests take first priority
                        pestConditions.first()
                    } else if (diseaseConditions.isNotEmpty()) {
                        // Diseases take second priority
                        diseaseConditions.first()
                    } else {
                        // Healthy conditions have lowest priority
                        healthyConditions.firstOrNull() ?: "Healthy"
                    }

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
                    val updatedNotes =
                        if (baseNotes.isEmpty()) conditionSummary else "$baseNotes\n\n$conditionSummary"

                    // Update plant with new information
                    val updatedPlant = it.copy(
                        name = updatedName,
                        currentCondition = primaryCondition,
                        lastScannedDate = Date(),
                        notes = updatedNotes
                    )

                    // Save to database
                    if (plantDatabaseManager.updatePlant(updatedPlant)) {
                        // Special handling when plants become healthy
                        if (!wasHealthy && isNowHealthy) {
                            // Delete ALL treatment events for this plant
                            val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)
                            val treatmentEvents = allEvents.filter { event ->
                                event.eventType.startsWith("Treat: ") ||
                                        event.eventType.equals("Treatment", ignoreCase = true)
                            }

                            Log.d(
                                "MainActivity",
                                "Plant recovered - removing ${treatmentEvents.size} treatment events"
                            )

                            for (event in treatmentEvents) {
                                plantDatabaseManager.deletePlantCareEvent(event.id)
                            }
                        }

                        // Create a scan event for the primary condition
                        val eventId = "scan_${plantId}_${System.currentTimeMillis()}"
                        val scanEvent = PlantDatabaseManager.PlantCareEvent(
                            id = eventId,
                            plantId = plantId,
                            eventType = "Scan",
                            date = Date(),
                            conditionName = primaryCondition,
                            notes = "Multiple conditions detected: ${
                                detectionsByCondition.keys.joinToString(
                                    ", "
                                )
                            }",
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

                        // IMPORTANT: First cancel all future care events
                        cancelFutureCareEvents(plantId)

                        // If conditions changed, cancel old treatments
                        if (isNewCondition || resolvedConditions.isNotEmpty()) {
                            cancelExistingTreatmentTasks(plantId)
                        }

                        // Create events for each additional condition
                        for ((condition, detections) in detectionsByCondition) {
                            if (condition != primaryCondition) {
                                val additionalEventId =
                                    "scan_${plantId}_${condition}_${System.currentTimeMillis()}"
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

                        // CRITICAL: Always recreate the complete plant care plan based on new conditions
                        createCompletePlantCarePlan(
                            plantId,
                            updatedPlant.type,
                            updatedPlant.wateringFrequency
                        )

                        // Additionally create specific treatments for any disease/pest conditions WITHOUT DUPLICATES
                        val processedConditions = mutableSetOf<String>()

                        if (!isNowHealthy) {
                            // Handle pest conditions first (higher priority)
                            for (condition in pestConditions) {
                                if (!processedConditions.contains(condition)) {
                                    processedConditions.add(condition)
                                    // Check for duplicates
                                    checkForDuplicateTreatments(plantId, condition)
                                    // Create treatment schedule for pest
                                    createAutomaticTreatmentSchedule(plantId, condition)
                                    Log.d(
                                        "MainActivity",
                                        "Created pest treatment plan for: $condition"
                                    )
                                }
                            }

                            // Then handle disease conditions
                            for (condition in diseaseConditions) {
                                if (!processedConditions.contains(condition)) {
                                    processedConditions.add(condition)
                                    // Check for duplicates
                                    checkForDuplicateTreatments(plantId, condition)
                                    // Create treatment schedule for disease
                                    createAutomaticTreatmentSchedule(plantId, condition)
                                    Log.d(
                                        "MainActivity",
                                        "Created disease treatment plan for: $condition"
                                    )
                                }
                            }
                        }

                        // Extra verification for healthy plants
                        if (isNowHealthy) {
                            verifyNoTreatmentsForHealthyPlant(plantId)
                        }

                        // Update UI on the main thread
                        runOnUiThread {
                            progressDialog.dismiss()

                            // If plant was healthy and now has a disease/pest
                            if (wasHealthy && !isNowHealthy) {
                                // Show plant info dialog with all conditions
                                showMultiplePlantInfoDialog(detectionsByCondition, true, true)
                                Toast.makeText(
                                    this,
                                    "Treatment plans created for detected conditions",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            // If changing from diseased to healthy, show congratulations
                            else if (!wasHealthy && isNowHealthy) {
                                // Show recovery dialog
                                AlertDialog.Builder(this)
                                    .setTitle("Plants Recovered!")
                                    .setMessage("Great news! Your ${updatedPlant.name} have recovered and are now healthy. All treatments have been removed.")
                                    .setPositiveButton("View Plant") { _, _ ->
                                        val intent =
                                            Intent(this, PlantManagementActivity::class.java)
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
                                Toast.makeText(
                                    this,
                                    "Treatment plans updated based on new scan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            // Just a normal rescan with no change
                            else {
                                // Show plant info dialog with all conditions
                                showMultiplePlantInfoDialog(detectionsByCondition, true, true)
                                Toast.makeText(this, "Plant conditions updated", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Failed to update plant", Toast.LENGTH_SHORT)
                                .show()
                            // Restart camera on error
                            startCamera()
                        }
                    }
                } ?: run {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Plant not found", Toast.LENGTH_SHORT).show()
                        // Restart camera on error
                        startCamera()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Log.e("UpdatePlants", "Error updating plants", e)
                    Toast.makeText(
                        this,
                        "Error updating plants: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    // Restart camera on error
                    startCamera()
                }
            }
        }.start()
    }

    private fun getPlantTypeFromCondition(condition: String): String {
        return when (condition) {
            // Tomato specific conditions
            "Blossom End Rot (Tomato) (Diseased)", "Healthy Tomato" -> "Tomato"

            // Eggplant specific conditions
            "Melon Thrips (Eggplant) (Diseased)", "Healthy eggplant" -> "Eggplant"

            // Okra specific conditions
            "Blossom Blight (Okra) (Diseased)", "Healthy okra" -> "Okra"

            // Bitter Gourd specific conditions
            "Phytophthora Fruit Rot (Bitter Gourd) (Diseased)", "Healthy bitter gourd" -> "Bitter Gourd"

            // Chili Pepper specific conditions
            "Anthracnose (Chili Pepper) (Diseased)", "Healthy Chili Pepper" -> "Chili Pepper"

            // General pests - determine from context or default
            "Aphids (Infested)", "Cutworm (Infested)", "Fruit Fly (Infested)",
            "Hippodamia Variegata/Lady Bug" -> selectedVegetable ?: "Unknown"

            else -> "Unknown"
        }
    }
//    private fun getCategoryFromCondition(condition: String): String {
//        return when {
//            condition.contains("(Diseased)") -> "Disease"
//            condition.contains("(Infested)") -> "Pest"
//            condition == "Hippodamia Variegata/Lady Bug" -> "Beneficial"
//            condition.startsWith("Healthy") -> "Healthy"
//            else -> "Unknown"
//        }
//    }

    private fun showMultiplePlantsInfoDialog(
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
            dialogView.findViewById<android.widget.Button>(R.id.goToManagementButton)

        // Custom title to show multiple plants and conditions were detected
        val totalPlants = detectionsByCondition.values.sumOf { it.size }
        val totalConditions = detectionsByCondition.size
        conditionTitle.text = "$totalPlants Vegetable detected: $totalConditions conditions"

        // Create a more detailed description showing plant counts
        val conditionCounts =
            detectionsByCondition.entries.joinToString("\n") { (condition, detections) ->
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
                conditionData.treatmentTips.take(2)
                    .forEach { tip -> // Limit to 2 tips to save space
                        val tipView = LayoutInflater.from(this)
                            .inflate(R.layout.item_treatment_tip, treatmentTipsContainer, false)
                        tipView.findViewById<TextView>(R.id.tipText).text = tip
                        treatmentTipsContainer.addView(tipView)
                    }
            }
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
            } else {
                // Only restart camera if we're not finishing the activity
                if (!isFinishing) {
                    startCamera()
                }
            }
        }

        // Create and show the dialog
        infoDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Handle dialog dismissal to restart camera if needed
        infoDialog?.setOnDismissListener {
            if (!isFinishing) {
                startCamera()
            }
        }
    }

    private fun cancelExistingTreatmentTasks(plantId: String) {
        val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

        // Find incomplete treatment events
        val pendingTreatments = allEvents.filter {
            (it.eventType.startsWith("Treat: ") || it.eventType.equals(
                "Treatment",
                ignoreCase = true
            )) &&
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
                    (it.eventType.equals(
                        "Treatment",
                        ignoreCase = true
                    ) && it.conditionName == conditionName)) &&
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
//    private fun showMultiplePlantInfoDialog(
//        detectionsByCondition: Map<String, List<BoundingBox>>,
//        showMonitoringOptions: Boolean = false,
//        isRescan: Boolean = false
//    ) {
//        // Stop camera before showing dialog
//        stopCamera()
//
//        // Get primary condition for the main display
//        val conditions = detectionsByCondition.keys.toList()
//        val primaryCondition = getPrimaryCondition(conditions)
//        val condition = PlantConditionData.conditions[primaryCondition] ?: return
//
//        // Create dialog with custom layout
//        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_info, null)
//
//        // Set primary condition details
//        val conditionTitle = dialogView.findViewById<TextView>(R.id.conditionTitle)
//        val conditionDescription = dialogView.findViewById<TextView>(R.id.conditionDescription)
//        val preventionTipsContainer = dialogView.findViewById<LinearLayout>(R.id.preventionTipsContainer)
//        val treatmentTipsContainer = dialogView.findViewById<LinearLayout>(R.id.treatmentTipsContainer)
//        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
//        val monitoringSection = dialogView.findViewById<View>(R.id.monitoringSection)
//        val setReminderButton = dialogView.findViewById<android.widget.Button>(R.id.setReminderButton)
//        val taskCompleteButton = dialogView.findViewById<android.widget.Button>(R.id.taskCompleteButton)
//        val addToCalendarButton = dialogView.findViewById<android.widget.Button>(R.id.addToCalendarButton)
//        val goToManagementButton = dialogView.findViewById<android.widget.Button>(R.id.goToManagementButton)
//
//        // Custom title to show multiple plants and conditions were detected
//        val totalPlants = detectionsByCondition.values.sumOf { it.size }
//        val totalConditions = detectionsByCondition.size
////        conditionTitle.text = "$totalPlants plants detected: $totalConditions conditions"
//
//        // Create a more detailed description showing plant counts
//        val conditionCounts = detectionsByCondition.entries.joinToString("\n") { (condition, detections) ->
//            "• $condition: ${detections.size} plants"
//        }
////        conditionDescription.text = "Plant conditions detected:\n$conditionCounts"
//        conditionDescription.text = "Plants detected:\n$conditionCounts"
//
//
//        // Create a custom card for showing all detected plant conditions
//        //test
////        val addDetailsText = TextView(this)
////        addDetailsText.text = "Multiple plants detected in this scan:"
////        addDetailsText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
////        addDetailsText.textSize = 16f
////        addDetailsText.setTypeface(null, Typeface.BOLD)
////        addDetailsText.setPadding(0, 24, 0, 8)
////        preventionTipsContainer.addView(addDetailsText)
//
//        // Add a summary of each detected condition with plant counts
////        for ((condName, detections) in detectionsByCondition) {
////            val countText = TextView(this)
////            countText.text = "• $condName: ${detections.size} plants"
////            countText.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
////            countText.textSize = 14f
////            countText.setPadding(16, 4, 0, 4)
////            preventionTipsContainer.addView(countText)
////        }
//
//        // Add treatment tips section - first add a header
////        treatmentTipsContainer.removeAllViews()
//
//        // For each condition, add its information
//        for ((condName, detections) in detectionsByCondition) {
//            val conditionData = PlantConditionData.conditions[condName]
//            if (conditionData != null) {
//                // Add condition header
//                val headerText = TextView(this)
////                headerText.text = "$condName (${detections.size} plants)"
//                headerText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
//                headerText.textSize = 16f
//                headerText.setTypeface(null, Typeface.BOLD)
////                headerText.setPadding(0, 24, 0, 8)
//                treatmentTipsContainer.addView(headerText)
//
//                // Add treatment tips for this condition
//                conditionData.treatmentTips.take(2).forEach { tip -> // Limit to 2 tips to save space
//                    val tipView = LayoutInflater.from(this).inflate(R.layout.item_treatment_tip, treatmentTipsContainer, false)
//                    tipView.findViewById<TextView>(R.id.tipText).text = tip
//                    treatmentTipsContainer.addView(tipView)
//                }
//            }
//        }
//
//        // Configure the monitoring section based on scan type (same as your original code)
//        if (isRescan && selectedPlantId != null) {
//            // This is a rescan of an existing plant - show Go to Management button
//            monitoringSection.visibility = View.VISIBLE
//
//            // Hide the regular buttons
//            setReminderButton.visibility = View.GONE
//            taskCompleteButton.visibility = View.GONE
//            addToCalendarButton.visibility = View.GONE
//
//            // Show the go to management button
//            goToManagementButton.visibility = View.VISIBLE
//
//            // Set go to management button listener
//            goToManagementButton.setOnClickListener {
//                val intent = Intent(this, PlantManagementActivity::class.java)
//                intent.putExtra("OPEN_PLANT_ID", selectedPlantId)
//                intent.putExtra("SHOW_TREATMENT_PLAN", true) // Important: This ensures treatment plan is shown
//                startActivity(intent)
//                finish() // Close this activity
//            }
//        } else if (showMonitoringOptions && selectedPlantId != null) {
//            // Regular scan for an existing plant
//            monitoringSection.visibility = View.VISIBLE
//            goToManagementButton.visibility = View.GONE
//            setReminderButton.visibility = View.VISIBLE
//            taskCompleteButton.visibility = View.VISIBLE
//            addToCalendarButton.visibility = View.GONE
//
//            // Set reminder button
//            setReminderButton.setOnClickListener {
//                dialogShowDateTimePicker(primaryCondition)
//            }
//
//            // Task complete button
//            taskCompleteButton.setOnClickListener {
//                markTreatmentComplete(primaryCondition)
//                Toast.makeText(this, "Task marked as complete", Toast.LENGTH_SHORT).show()
//                infoDialog?.dismiss()
//
//                // If this was a disease/pest condition, ask if it's resolved
//                if (!primaryCondition.startsWith("Healthy")) {
//                    showResolutionConfirmationDialog()
//                }
//            }
//        } else if (showMonitoringOptions) {
//            // This is a one-time scan, show option to add to my plants
//            monitoringSection.visibility = View.VISIBLE
//            setReminderButton.visibility = View.GONE
//            taskCompleteButton.visibility = View.GONE
//            goToManagementButton.visibility = View.GONE
//            addToCalendarButton.visibility = View.VISIBLE
//
//            // Add to calendar button - now shows dialog for multiple plants
//            addToCalendarButton.setOnClickListener {
//                showAddMultiplePlantsDialog(detectionsByCondition)
//            }
//        } else {
//            monitoringSection.visibility = View.GONE
//        }
//
//        // Set up close button
//        closeButton.setOnClickListener {
//            infoDialog?.dismiss()
//
//            // If we came from the Plant Management screen and this was a rescan, go back there
//            if (selectedPlantId != null && isRescan) {
//                val intent = Intent(this, PlantManagementActivity::class.java)
//                intent.putExtra("OPEN_PLANT_ID", selectedPlantId)
//                intent.putExtra("SHOW_TREATMENT_PLAN", true) // Important: Force show treatment plan
//                startActivity(intent)
//                finish()
//            } else {
//                // Restart camera if we're not navigating away
//                if (!isFinishing) {
//                    startCamera()
//                }
//            }
//        }
//
//        // Create and show the dialog
//        infoDialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setCancelable(true)
//            .create()
//
//        // Handle dialog dismissal to restart camera if needed
//        infoDialog?.setOnDismissListener {
//            if (!isFinishing) {
//                startCamera()
//            }
//        }
//
//        infoDialog?.show()
//    }

    private fun showMultiplePlantInfoDialog(
        detectionsByCondition: Map<String, List<BoundingBox>>,
        showMonitoringOptions: Boolean = false,
        isRescan: Boolean = false
    ) {
        // Stop camera before showing dialog
        stopCamera()

        // Get primary condition for the main display
        val conditions = detectionsByCondition.keys.toList()
        val primaryCondition = getPrimaryCondition(conditions)
        val condition = PlantConditionData.conditions[primaryCondition] ?: return

        // Create dialog with custom layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_info, null)

        // Get references to all UI elements
        val conditionDescription = dialogView.findViewById<TextView>(R.id.conditionDescription)
        val preventionTipsContainer = dialogView.findViewById<LinearLayout>(R.id.preventionTipsContainer)
        val treatmentTipsContainer = dialogView.findViewById<LinearLayout>(R.id.treatmentTipsContainer)
        val maintenanceTipsContainer = dialogView.findViewById<LinearLayout>(R.id.maintenanceTipsContainer)
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val monitoringSection = dialogView.findViewById<View>(R.id.monitoringSection)

        // Section headers and scrollviews
        val preventionHeader = dialogView.findViewById<TextView>(R.id.preventionHeader)
        val preventionScrollView = dialogView.findViewById<ScrollView>(R.id.preventionScrollView)
        val treatmentHeader = dialogView.findViewById<TextView>(R.id.treatmentHeader)
        val treatmentScrollView = dialogView.findViewById<ScrollView>(R.id.treatmentScrollView)
        val maintenanceHeader = dialogView.findViewById<TextView>(R.id.maintenanceHeader)
        val maintenanceScrollView = dialogView.findViewById<ScrollView>(R.id.maintenanceScrollView)
        val divider1 = dialogView.findViewById<View>(R.id.divider1)
        val divider2 = dialogView.findViewById<View>(R.id.divider2)
        val dividerMaintenance = dialogView.findViewById<View>(R.id.dividerMaintenance)

        // Monitoring buttons
        val setReminderButton = dialogView.findViewById<Button>(R.id.setReminderButton)
        val taskCompleteButton = dialogView.findViewById<Button>(R.id.taskCompleteButton)
        val addToCalendarButton = dialogView.findViewById<Button>(R.id.addToCalendarButton)
        val goToManagementButton = dialogView.findViewById<Button>(R.id.goToManagementButton)

        // Check types of detected conditions
        val onlyLadybugsDetected = detectionsByCondition.keys.all { it == "Hippodamia Variegata/Lady Bug" }
        val hasBeneficialInsects = conditions.contains("Hippodamia Variegata/Lady Bug")
        val hasHealthyCondition = detectionsByCondition.keys.any { it.startsWith("Healthy") }
        val hasUnhealthyCondition = detectionsByCondition.keys.any {
            !it.startsWith("Healthy") && it != "Hippodamia Variegata/Lady Bug"
        }

        // Total plants and conditions
        val totalPlants = detectionsByCondition.values.sumOf { it.size }
        val totalConditions = detectionsByCondition.size

        // Set description text
        if (totalConditions == 1 && detectionsByCondition[primaryCondition]?.size == 1) {
            // Single plant with single condition
            conditionDescription.text = primaryCondition
            conditionDescription.textSize = 14f
        } else {
            // Multiple plants or conditions
            val conditionCounts = detectionsByCondition.entries.joinToString("\n") { (condition, detections) ->
                "• $condition: ${detections.size}"
            }
            conditionDescription.text = "Vegetable detected:\n$conditionCounts"
        }

        // ------------------- MAINTENANCE SECTION -------------------
        // Show maintenance section for healthy plants, ladybugs, or both
        if (hasHealthyCondition || onlyLadybugsDetected || hasBeneficialInsects) {
            maintenanceHeader.visibility = View.VISIBLE
            maintenanceScrollView.visibility = View.VISIBLE
            dividerMaintenance.visibility = View.VISIBLE

            maintenanceTipsContainer.removeAllViews()
            var hasMaintenance = false

            // Process healthy conditions and ladybugs
            for ((condName, detections) in detectionsByCondition) {
                if (!condName.startsWith("Healthy") && condName != "Hippodamia Variegata/Lady Bug") continue

                val conditionData = PlantConditionData.conditions[condName]
                if (conditionData != null) {
                    hasMaintenance = true

                    // Add condition header
                    val headerText = TextView(this)
                    headerText.text = condName
                    headerText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                    headerText.textSize = 14f
                    headerText.setTypeface(null, Typeface.BOLD)
                    headerText.setPadding(0, 8, 0, 8)
                    maintenanceTipsContainer.addView(headerText)

                    // Use prevention tips for maintenance
                    val maintenanceTips = when (condName) {
                        "Hippodamia Variegata/Lady Bug" -> {
                            // Specific handling for ladybugs
                            listOf(
                                "Avoid broad-spectrum insecticides that harm beneficial insects",
                                "Plant diverse flowering plants to provide nectar and pollen",
                                "Create overwintering sites with leaf litter or insect houses",
                                "Maintain areas with aphids to sustain ladybug populations",
                                "Encourage a balanced garden ecosystem"
                            )
                        }
                        else -> conditionData.preventionTips
                    }

                    maintenanceTips.take(3).forEach { tip ->
                        val tipView = LayoutInflater.from(this).inflate(
                            R.layout.item_treatment_tip,
                            maintenanceTipsContainer,
                            false
                        )
                        tipView.findViewById<TextView>(R.id.tipText).text = "• ${limitTextLength(tip, 100)}"
                        maintenanceTipsContainer.addView(tipView)
                    }
                }
            }

            if (!hasMaintenance) {
                val naText = TextView(this)
                naText.text = "No maintenance tips available"
                naText.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
                naText.textSize = 10f
                naText.setPadding(0, 8, 0, 8)
                maintenanceTipsContainer.addView(naText)
            }
        } else {
            // Hide maintenance section if no healthy plants or ladybugs
            maintenanceHeader.visibility = View.GONE
            maintenanceScrollView.visibility = View.GONE
            dividerMaintenance.visibility = View.GONE
        }

        // ------------------- PREVENTION & TREATMENT SECTIONS -------------------
        // Hide prevention and treatment sections for only beneficial cases
        if (onlyLadybugsDetected || (hasHealthyCondition && !hasUnhealthyCondition)) {
            // Hide prevention and treatment sections for only beneficial insects or healthy plants
            preventionHeader.visibility = View.GONE
            preventionScrollView.visibility = View.GONE
            treatmentHeader.visibility = View.GONE
            treatmentScrollView.visibility = View.GONE
            divider1.visibility = View.GONE
            divider2.visibility = View.GONE
        } else {
            // Show prevention section for unhealthy plants
            preventionHeader.visibility = View.VISIBLE
            preventionScrollView.visibility = View.VISIBLE
            divider1.visibility = View.VISIBLE

            // Prevention tips
            preventionTipsContainer.removeAllViews()
            var hasPrevention = false
            val isSingleCondition = totalConditions == 1

            for ((condName, detections) in detectionsByCondition) {
                // Skip healthy conditions and ladybugs
                if (condName.startsWith("Healthy") || condName == "Hippodamia Variegata/Lady Bug") continue

                val conditionData = PlantConditionData.conditions[condName]
                if (conditionData != null && conditionData.preventionTips.isNotEmpty()) {
                    hasPrevention = true

                    // Add condition header for multiple conditions
                    if (!isSingleCondition) {
                        val headerText = TextView(this)
                        headerText.text = condName
                        headerText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                        headerText.textSize = 12f
                        headerText.setTypeface(null, Typeface.BOLD)
                        headerText.setPadding(0, 8, 0, 8)
                        preventionTipsContainer.addView(headerText)
                    }

                    // Add prevention tips (limit to 2, truncate to 100 characters)
                    conditionData.preventionTips.take(2).forEach { tip ->
                        val tipView = LayoutInflater.from(this)
                            .inflate(R.layout.item_prevention_tip, preventionTipsContainer, false)
                        tipView.findViewById<TextView>(R.id.tipText).text = limitTextLength(tip, 100)
                        preventionTipsContainer.addView(tipView)
                    }
                }
            }

            // If no prevention tips, display N/A
            if (!hasPrevention) {
                val naText = TextView(this)
                naText.text = "No prevention tips available"
                naText.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
                naText.textSize = 10f
                naText.setPadding(0, 8, 0, 8)
                preventionTipsContainer.addView(naText)
            }

            // Check if we have ladybugs alongside ONLY pest infestations that ladybugs can control
            val onlyHasControlablePests = detectionsByCondition.keys.all { condition ->
                condition == "Hippodamia Variegata/Lady Bug" ||
                        condition == "Aphids (Infested)" || // Ladybugs primarily control aphids
                        condition == "Fruit Fly (Infested)" || // Ladybugs may help with fruit fly eggs/larvae
                        condition == "Cutworm (Infested)" || // Ladybugs can prey on young cutworms
                        condition.startsWith("Healthy")
            }

            // If ladybugs detected alongside only controllable pests, suppress treatments just for those pests
            if (hasBeneficialInsects && hasUnhealthyCondition && onlyHasControlablePests) {
                // Hide treatment section only for aphids (which ladybugs naturally control)
                treatmentHeader.visibility = View.GONE
                treatmentScrollView.visibility = View.GONE
                divider2.visibility = View.GONE

                // Add a note about ladybugs in the prevention section
                val ladybugNote = TextView(this)
                ladybugNote.text = "Ladybugs detected! These beneficial insects will help control pests naturally."
                ladybugNote.setTextColor(ContextCompat.getColor(this, R.color.app_dark_green))
                ladybugNote.textSize = 14f
                ladybugNote.setTypeface(null, Typeface.BOLD_ITALIC)
                ladybugNote.setPadding(0, 16, 0, 8)
                preventionTipsContainer.addView(ladybugNote)
            } else {
                // Show treatment section for diseases and pests that ladybugs don't control
                treatmentHeader.visibility = View.VISIBLE
                treatmentScrollView.visibility = View.VISIBLE
                divider2.visibility = View.VISIBLE

                // If we have ladybugs, add an informational note
                if (hasBeneficialInsects && hasUnhealthyCondition) {
                    val ladybugNote = TextView(this)
                    ladybugNote.text = "Note: Ladybugs detected! These beneficial insects will help control some pests but not diseases."
                    ladybugNote.setTextColor(ContextCompat.getColor(this, R.color.app_dark_green))
                    ladybugNote.textSize = 12f
                    ladybugNote.setTypeface(null, Typeface.ITALIC)
                    ladybugNote.setPadding(0, 8, 0, 12)
                    treatmentTipsContainer.addView(ladybugNote)
                }

                // Treatment tips
                treatmentTipsContainer.removeAllViews()
                var hasTreatment = false

                for ((condName, detections) in detectionsByCondition) {
                    // Skip healthy conditions and ladybugs
                    if (condName.startsWith("Healthy") || condName == "Hippodamia Variegata/Lady Bug") continue

                    // If we have ladybugs, skip showing treatment for pests that ladybugs can control
                    if (hasBeneficialInsects && (
                                condName == "Aphids (Infested)" ||
                                        condName == "Fruit Fly (Infested)" ||
                                        condName == "Cutworm (Infested)"
                                )) continue

                    val conditionData = PlantConditionData.conditions[condName]
                    if (conditionData != null && conditionData.treatmentTips.isNotEmpty()) {
                        hasTreatment = true

                        // Add condition header for multiple conditions
                        if (!isSingleCondition) {
                            val headerText = TextView(this)
                            headerText.text = condName
                            headerText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                            headerText.textSize = 12f
                            headerText.setTypeface(null, Typeface.BOLD)
                            headerText.setPadding(0, 8, 0, 8)
                            treatmentTipsContainer.addView(headerText)
                        }

                        // Add treatment tips (limit to 2, truncate to 100 characters)
                        conditionData.treatmentTips.take(2).forEach { tip ->
                            val tipView = LayoutInflater.from(this)
                                .inflate(R.layout.item_treatment_tip, treatmentTipsContainer, false)
                            tipView.findViewById<TextView>(R.id.tipText).text = limitTextLength(tip, 100)
                            treatmentTipsContainer.addView(tipView)
                        }
                    }
                }

                // If no treatment tips, display N/A
                if (!hasTreatment) {
                    val naText = TextView(this)
                    naText.text = "No treatment tips available"
                    naText.setTextColor(ContextCompat.getColor(this, R.color.dark_gray))
                    naText.textSize = 10f
                    naText.setPadding(0, 8, 0, 8)
                    treatmentTipsContainer.addView(naText)
                }
            }
        }

        // ------------------- MONITORING SECTION -------------------
        if (isRescan && selectedPlantId != null) {
            // This is a rescan of an existing plant - show Go to Management button
            monitoringSection.visibility = View.VISIBLE
            setReminderButton.visibility = View.GONE
            taskCompleteButton.visibility = View.GONE
            addToCalendarButton.visibility = View.GONE
            goToManagementButton.visibility = View.VISIBLE

            goToManagementButton.setOnClickListener {
                val intent = Intent(this, PlantManagementActivity::class.java)
                intent.putExtra("OPEN_PLANT_ID", selectedPlantId)
                intent.putExtra("SHOW_TREATMENT_PLAN", true)
                startActivity(intent)
                finish()
            }
        } else if (showMonitoringOptions && selectedPlantId != null) {
            // Regular scan for an existing plant
            monitoringSection.visibility = View.VISIBLE
            goToManagementButton.visibility = View.GONE
            setReminderButton.visibility = View.VISIBLE
            taskCompleteButton.visibility = View.VISIBLE
            addToCalendarButton.visibility = View.GONE

            setReminderButton.setOnClickListener {
                dialogShowDateTimePicker(primaryCondition)
            }

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
                intent.putExtra("SHOW_TREATMENT_PLAN", true)
                startActivity(intent)
                finish()
            } else {
                // Restart camera if we're not navigating away
                if (!isFinishing) {
                    startCamera()
                }
            }
        }

        // Create and show dialog
        infoDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        infoDialog?.setOnDismissListener {
            if (!isFinishing) {
                startCamera()
            }
        }

        infoDialog?.show()
    }
    // Helper function to limit text length
    private fun limitTextLength(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength) + "..."
        } else {
            text
        }
    }
    // 6. Add dialog for creating a new plant group from multiple detections
    private fun showAddMultiplePlantsDialog(detectionsByCondition: Map<String, List<BoundingBox>>) {
        // Stop camera and scanning to prevent resource issues
        stopCamera()
        if (isScanning) {
            toggleScanning()
        }
        stopCamera() // Stop camera to free resources

        // Show loading indicator
        val loadingDialog = ProgressDialog(this).apply {
            setMessage("Preparing...")
            setCancelable(false)
            stopCamera()
        }
        loadingDialog.show()
        stopCamera()
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
        val wateringFrequency = 1 // Default to 1 day

        // Hide watering frequency controls
        val wateringFrequencyText = dialogView.findViewById<TextView>(R.id.wateringFrequencyText)
        val decreaseFrequencyButton = dialogView.findViewById<Button>(R.id.decreaseFrequencyButton)
        val increaseFrequencyButton = dialogView.findViewById<Button>(R.id.increaseFrequencyButton)

        wateringFrequencyText.visibility = View.GONE
        decreaseFrequencyButton.visibility = View.GONE
        increaseFrequencyButton.visibility = View.GONE

        // Determine vegetable type from conditions with better detection logic
        val tomatoConditions = detectionsByCondition.keys.filter {
            it == "Blossom End Rot (Tomato) (Diseased)" || it == "Healthy Tomato"
        }
        val eggplantConditions = detectionsByCondition.keys.filter {
            it == "Melon Thrips (Eggplant) (Diseased)" || it == "Healthy eggplant"
        }
        val okraConditions = detectionsByCondition.keys.filter {
            it == "Blossom Blight (Okra) (Diseased)" || it == "Healthy okra"
        }
        val bitterGourdConditions = detectionsByCondition.keys.filter {
            it == "Phytophthora Fruit Rot (Bitter Gourd) (Diseased)" || it == "Healthy bitter gourd"
        }
        val chiliPepperConditions = detectionsByCondition.keys.filter {
            it == "Anthracnose (Chili Pepper) (Diseased)" || it == "Healthy Chili Pepper"
        }

        // Determine the predominant vegetable type
        val vegetableType = when {
            tomatoConditions.isNotEmpty() && eggplantConditions.isEmpty() &&
                    okraConditions.isEmpty() && bitterGourdConditions.isEmpty() &&
                    chiliPepperConditions.isEmpty() -> "Tomato"
            eggplantConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                    okraConditions.isEmpty() && bitterGourdConditions.isEmpty() &&
                    chiliPepperConditions.isEmpty() -> "Eggplant"
            okraConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                    eggplantConditions.isEmpty() && bitterGourdConditions.isEmpty() &&
                    chiliPepperConditions.isEmpty() -> "Okra"
            bitterGourdConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                    eggplantConditions.isEmpty() && okraConditions.isEmpty() &&
                    chiliPepperConditions.isEmpty() -> "Bitter Gourd"
            chiliPepperConditions.isNotEmpty() && tomatoConditions.isEmpty() &&
                    eggplantConditions.isEmpty() && okraConditions.isEmpty() &&
                    bitterGourdConditions.isEmpty() -> "Chili Pepper"
            else -> selectedVegetable ?: "Mixed"
        }

        // Suggest a default name
        val suggestedName = when (vegetableType) {
            "Tomato" -> "Tomato Group"
            "Eggplant" -> "Eggplant Group"
            "Okra" -> "Okra Group"
            "Bitter Gourd" -> "Bitter Gourd Group"
            "Chili Pepper" -> "Chili Pepper Group"
            "Mixed" -> "Mixed Vegetables"
            else -> "Plant Group"
        }

        plantNameInput.setText("")

        // Dismiss loading dialog now that we've set up the dialog
        loadingDialog.dismiss()

        // Add a notice that counts will be appended automatically
        val noticeText =
            dialogView.findViewById<TextView>(R.id.noticeText) ?: TextView(this).apply {
                id = R.id.noticeText
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                text = "Plant count information will be added automatically to the name."
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 16)
                }
                // Add to parent if it doesn't exist
                val parent = plantNameInput.parent as? ViewGroup
                parent?.addView(this, parent.indexOfChild(plantNameInput) + 1)
            }

        cancelButton.setOnClickListener {
            // Just dismiss the dialog
            infoDialog?.dismiss()
            // Restart camera when canceling
            startCamera()
        }

        addPlantButton.setOnClickListener {
            val basePlantName = plantNameInput.text.toString().trim()

            if (basePlantName.isEmpty()) {
                Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Always append the plant count information if it's not already included
            val finalPlantName =
                if (!basePlantName.contains("(") || !basePlantName.contains("plants")) {
                    "$basePlantName (${totalPlants} plants, ${totalConditions} conditions)"
                } else {
                    basePlantName
                }

            // Disable button to prevent multiple clicks
            addPlantButton.isEnabled = false

            // Show progress dialog
            val progressDialog = ProgressDialog(this).apply {
                setMessage("Adding plants group...")
                setCancelable(false)
            }
            progressDialog.show()

            // Add the multiple plants as a group (run on background thread to prevent ANR)
            Thread {
                try {
                    val plantId = addMultiplePlantsAsGroup(
                        plantName = finalPlantName,
                        vegetableType = vegetableType,
                        detectionsByCondition = detectionsByCondition,
                        wateringFrequency = wateringFrequency
                    )

                    runOnUiThread {
                        progressDialog.dismiss()

                        if (plantId.isNotEmpty()) {
                            // Create comprehensive care plan for all plants in the group
                            createCompletePlantCarePlan(plantId, vegetableType, wateringFrequency)

                            // Final cleanup of any duplicate events
                            cleanupDuplicateEvents(plantId)

                            Toast.makeText(
                                this,
                                "Plant group added to monitoring with complete care schedule",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Dismiss dialogs
                            infoDialog?.dismiss()

                            // Go to plant management screen
                            val intent = Intent(this, PlantManagementActivity::class.java)
                            intent.putExtra("OPEN_PLANT_ID", plantId)
                            intent.putExtra("SHOW_TREATMENT_PLAN", true) // Show treatment plan tab
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to add plant group", Toast.LENGTH_SHORT)
                                .show()
                            addPlantButton.isEnabled = true
                            // Restart camera if we're not navigating away
                            startCamera()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Log.e("AddPlantGroup", "Error adding plant group", e)
                        Toast.makeText(
                            this,
                            "Error adding plant group: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                        addPlantButton.isEnabled = true
                        // Restart camera on error
                        startCamera()
                    }
                }
            }.start()
        }

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Handle dialog dismissal to restart camera if needed
        dialog.setOnDismissListener {
            if (infoDialog == dialog) {
                infoDialog = null
                // Only restart camera if we're not already navigating away
                if (!isFinishing) {
                    startCamera()
                }
            }
        }

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
        val conditionCounts =
            detectionsByCondition.entries.joinToString("\n") { (condition, detections) ->
                "- $condition: ${detections.size} plants"
            }

        // Include details of all detected conditions in notes
        val notes =
            "Plant group containing $totalPlants plants with ${detectionsByCondition.size} different conditions:\n$conditionCounts\n\nDetected on: ${Date()}"

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
            Log.d(
                "MainActivity",
                "Added plant group $plantId with ${detectionsByCondition.size} conditions"
            )

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

            // Return the plant ID so we can create a care plan
            return plantId
        }

        return ""
    }

    override fun onPause() {
        super.onPause()
        // Stop camera when app goes into background
        stopCamera()
    }

    private fun openPlantManagementWithTreatmentPlan(plantId: String) {
        val intent = Intent(this, PlantManagementActivity::class.java)
        intent.putExtra("OPEN_PLANT_ID", plantId)
        intent.putExtra("SHOW_TREATMENT_PLAN", true) // Force show the treatment tab
        startActivity(intent)
        finish() // Close this activity
    }

    // 8. Modify updatePlantCondition to use our deduplication logic
    private fun updatePlantCondition(plantId: String, conditionName: String) {
        // Stop camera and scanning to prevent resource issues
        if (isScanning) {
            toggleScanning()
        }
        stopCamera() // Stop camera to free resources

        // Show loading indicator
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Updating plant status...")
            setCancelable(false)
        }
        progressDialog.show()

        // Run the update operation in a background thread to prevent UI freezing
        Thread {
            try {
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

                        // Special handling when a plant becomes healthy
                        if (!wasHealthy && isNowHealthy) {
                            // First, delete ALL treatment events for this plant
                            val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)
                            val treatmentEvents = allEvents.filter { event ->
                                event.eventType.startsWith("Treat: ") ||
                                        event.eventType.equals("Treatment", ignoreCase = true)
                            }
                            for (event in treatmentEvents) {
                                plantDatabaseManager.deletePlantCareEvent(event.id)
                            }
                        }

                        // IMPORTANT: Make sure we fully recreate the monthly plan - this is the key fix
                        updateMonthlyPlan(
                            plantId,
                            updatedPlant,
                            wasHealthy,
                            isNowHealthy,
                            isNewCondition,
                            conditionName
                        )

                        // Final cleanup of any duplicate events that might have been created
                        cleanupDuplicateEvents(plantId)

                        // Extra verification for recovered plants
                        if (isNowHealthy && !wasHealthy) {
                            verifyNoTreatmentsForHealthyPlant(plantId)
                        }

                        // Update UI on the main thread
                        runOnUiThread {
                            progressDialog.dismiss()

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
                                    .setMessage("Great news! Your ${updatedPlant.name} has recovered and is now healthy. All treatments have been removed.")
                                    .setPositiveButton("View Plant") { _, _ ->
                                        val intent =
                                            Intent(this, PlantManagementActivity::class.java)
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

                                Toast.makeText(this, "Plant condition updated", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Failed to update plant", Toast.LENGTH_SHORT)
                                .show()
                            // Restart camera on error
                            startCamera()
                        }
                    }
                } ?: run {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Plant not found", Toast.LENGTH_SHORT).show()
                        // Restart camera on error
                        startCamera()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Log.e("UpdatePlant", "Error updating plant", e)
                    Toast.makeText(
                        this,
                        "Error updating plant: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    // Restart camera on error
                    startCamera()
                }
            }
        }.start()
    }


    // 2. New method to update monthly plan - this ensures the plan is fully recreated
    private fun updateMonthlyPlan(
        plantId: String,
        updatedPlant: PlantDatabaseManager.Plant,
        wasHealthy: Boolean,
        isNowHealthy: Boolean,
        isNewCondition: Boolean,
        conditionName: String
    ) {
        try {
            Log.d("MainActivity", "Updating monthly plan for plant $plantId")
            Log.d(
                "MainActivity",
                "Status changed: wasHealthy=$wasHealthy, isNowHealthy=$isNowHealthy"
            )

            // First, cancel ALL existing treatment tasks regardless of completion status
            // This is especially important when changing from diseased to healthy
            if (isNowHealthy && !wasHealthy) {
                Log.d("MainActivity", "Plant is now healthy, removing ALL treatment tasks")
                val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

                // Find ALL treatment events for this plant
                val treatmentEvents = allEvents.filter { event ->
                    event.eventType.startsWith("Treat: ") ||
                            event.eventType.equals("Treatment", ignoreCase = true)
                }

                // Delete all treatment-related events since they're no longer needed
                for (event in treatmentEvents) {
                    Log.d("MainActivity", "Deleting treatment event: ${event.id}")
                    plantDatabaseManager.deletePlantCareEvent(event.id)
                }
            }

            // Then cancel all future care events in general (except completed ones and scan history)
            cancelFutureCareEvents(plantId)

            // If condition changed, cancel existing treatment tasks
            if (isNewCondition) {
                cancelExistingTreatmentTasks(plantId)
            }

            // Always recreate the complete plant care plan based on new conditions
            createCompletePlantCarePlan(plantId, updatedPlant.type, updatedPlant.wateringFrequency)

            // If the plant is now diseased (and not previously diseased), create specific treatment schedule
            if (!isNowHealthy && (wasHealthy || isNewCondition)) {
                // Make sure we don't create duplicate treatments
                checkForDuplicateTreatments(plantId, conditionName)
                createAutomaticTreatmentSchedule(plantId, conditionName)
                // Double check after creation
                checkForDuplicateTreatments(plantId, conditionName)
            }

            // Do a final verification that if the plant is healthy, there are NO treatment tasks
            if (isNowHealthy) {
                verifyNoTreatmentsForHealthyPlant(plantId)
            }

            Log.d("MainActivity", "Monthly plan updated successfully for plant $plantId")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating monthly plan: ${e.message}", e)
        }
    }

    private fun verifyNoTreatmentsForHealthyPlant(plantId: String) {
        try {
            val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

            // Find any treatment events that are still present
            val remainingTreatments = allEvents.filter { event ->
                (event.eventType.startsWith("Treat: ") ||
                        event.eventType.equals("Treatment", ignoreCase = true))
            }

            // If any treatments still exist, delete them
            if (remainingTreatments.isNotEmpty()) {
                Log.d(
                    "MainActivity",
                    "Found ${remainingTreatments.size} treatments for healthy plant - removing them"
                )

                for (event in remainingTreatments) {
                    plantDatabaseManager.deletePlantCareEvent(event.id)
                }
            }
        } catch (e: Exception) {
            Log.e(
                "MainActivity",
                "Error verifying healthy plant has no treatments: ${e.message}",
                e
            )
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

    private fun createCompletePlantCarePlan(
        plantId: String,
        plantType: String,
        wateringFrequency: Int
    ) {
        Log.d("MainActivity", "Creating complete plant care plan for $plantId (Type: $plantType)")

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

        // Log what we're creating
        Log.d(
            "MainActivity",
            "Plant care plan details: Group=$isPlantGroup, Plants=$totalPlantsInGroup, Diseases=${diseaseConditions.keys}"
        )

        // 1. Schedule watering for TODAY AND future days
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

        plantDatabaseManager.scheduleWatering(
            plantId, todayWatering,
            if (isPlantGroup) "Initial watering for all $totalPlantsInGroup plants" else "Initial watering"
        )

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
            Log.d(
                "MainActivity",
                "Creating treatments for ${diseaseConditions.size} disease conditions"
            )

            // Process each condition only ONCE to avoid duplicates
            val processedConditions = mutableSetOf<String>()

            for ((conditionName, plantCount) in diseaseConditions) {
                if (!processedConditions.contains(conditionName)) {
                    processedConditions.add(conditionName)

                    // First check if there are any existing treatments
                    checkForDuplicateTreatments(plantId, conditionName)

                    // Create the treatment schedule
                    createAutomaticTreatmentSchedule(plantId, conditionName, plantCount)

                    // Check again after creation
                    checkForDuplicateTreatments(plantId, conditionName)
                }
            }
        } else if (!isPlantGroup && plant?.currentCondition != null && !plant.currentCondition.startsWith(
                "Healthy"
            )
        ) {
            Log.d(
                "MainActivity",
                "Creating treatment for single plant with condition: ${plant.currentCondition}"
            )

            // First check for duplicates
            checkForDuplicateTreatments(plantId, plant.currentCondition)

            // Create treatment schedule
            createAutomaticTreatmentSchedule(plantId, plant.currentCondition, 1)

            // Check again after creation to make sure no duplicates were created
            checkForDuplicateTreatments(plantId, plant.currentCondition)
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

        Log.d("MainActivity", "Completed creating care plan for $plantId")
    }

    private fun cleanupDuplicateEvents(plantId: String) {
        Log.d("MainActivity", "Cleaning up all duplicate events for plant $plantId")

        try {
            // Get all plant events
            val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

            // First, clean up any duplicate watering events
            cleanupDuplicateEventsByType(plantId, allEvents, "Watering")

            // Clean up any duplicate inspection events
            cleanupDuplicateEventsByType(plantId, allEvents, "Inspect")

            // Clean up any duplicate fertilizing events
            cleanupDuplicateEventsByType(plantId, allEvents, "Fertilize")

            // Clean up treatments by condition
            val plant = plantDatabaseManager.getPlant(plantId)
            if (plant != null && plant.currentCondition != null) {
                checkForDuplicateTreatments(plantId, plant.currentCondition)
            }

            // For plant groups, check all conditions
            if (plant != null && plant.name.contains("plants") && plant.name.contains("(")) {
                val conditions = mutableSetOf<String>()
                // Parse conditions from notes
                val notesLines = plant.notes.split("\n")
                for (line in notesLines) {
                    if (line.trim()
                            .startsWith("-") && line.contains(":") && line.contains("plants")
                    ) {
                        val conditionName = line.substringAfter("-").substringBefore(":").trim()
                        if (!conditionName.contains("Healthy", ignoreCase = true)) {
                            conditions.add(conditionName)
                        }
                    }
                }

                // Check for duplicates for each condition
                for (condition in conditions) {
                    checkForDuplicateTreatments(plantId, condition)
                }
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error cleaning up duplicate events: ${e.message}", e)
        }
    }

    private fun cleanupDuplicateEventsByType(
        plantId: String,
        allEvents: List<PlantDatabaseManager.PlantCareEvent>,
        eventType: String
    ) {
        try {
            // Get all events of this type that aren't completed
            val typeEvents = allEvents.filter {
                it.eventType == eventType && !it.completed
            }

            // Group events by date (just the day, not time)
            val eventsByDay =
                mutableMapOf<String, MutableList<PlantDatabaseManager.PlantCareEvent>>()

            for (event in typeEvents) {
                val cal = Calendar.getInstance()
                cal.time = event.date
                // Use year/month/day as key
                val key =
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"

                if (!eventsByDay.containsKey(key)) {
                    eventsByDay[key] = mutableListOf()
                }
                eventsByDay[key]?.add(event)
            }

            // For each day, keep only one event (the earliest one)
            for ((day, events) in eventsByDay) {
                if (events.size > 1) {
                    // Sort by time
                    val sortedEvents = events.sortedBy { it.date }
                    // Keep the first one
                    val keepEvent = sortedEvents.first()

                    // Delete the rest
                    for (i in 1 until sortedEvents.size) {
                        val deleteEvent = sortedEvents[i]
                        Log.d(
                            "MainActivity",
                            "Deleting duplicate $eventType on $day: ${deleteEvent.id}"
                        )
                        plantDatabaseManager.deletePlantCareEvent(deleteEvent.id)
                    }

                    Log.d("MainActivity", "Kept $eventType on $day: ${keepEvent.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error cleaning up $eventType events: ${e.message}", e)
        }
    }

    private fun createAutomaticTreatmentSchedule(
        plantId: String,
        conditionName: String,
        plantCount: Int = 1
    ) {
        Log.d(
            "MainActivity",
            "Creating treatment schedule for condition: $conditionName (Plants: $plantCount)"
        )

        // First check for and remove any duplicate treatments that might already exist
        checkForDuplicateTreatments(plantId, conditionName)

        val condition = PlantConditionData.conditions[conditionName]
        if (condition == null) {
            Log.e(
                "MainActivity",
                "Treatment not created: Condition data not found for $conditionName"
            )
            return
        }

        // Log the treatment tasks we're creating
        Log.d(
            "MainActivity",
            "Found ${condition.treatmentTasks.size} treatment tasks for $conditionName"
        )

        // Create an urgent task for today (only ONE task)
        val urgentTask = condition.treatmentTasks.firstOrNull()
        if (urgentTask != null) {
            val treatmentTime = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }.time

            val urgentTaskId =
                "urgent_treat_${plantId}_${conditionName}_${System.currentTimeMillis()}"
            val plantCountText = if (plantCount == 1) "(1 plant)" else "($plantCount plants)"

            val urgentTreatmentEvent = PlantDatabaseManager.PlantCareEvent(
                id = urgentTaskId,
                plantId = plantId,
                eventType = "Treat: $conditionName",
                date = treatmentTime,
                conditionName = conditionName,
                notes = "URGENT: ${urgentTask.taskName} for $conditionName $plantCountText\n\n${urgentTask.description}\n\nMaterials: ${
                    urgentTask.materials.joinToString(
                        ", "
                    )
                }\n\nInstructions:\n${urgentTask.instructions.joinToString("\n- ", "- ")}",
                completed = false
            )

            val success = plantDatabaseManager.addPlantCareEvent(urgentTreatmentEvent)
            Log.d("MainActivity", "Urgent task created: $success (${urgentTask.taskName})")
        }

        // Schedule follow-up treatments with more controlled spacing
        for ((index, task) in condition.treatmentTasks.withIndex()) {
            // Skip the first task as we've already added an urgent task
            if (index == 0) continue

            val scheduleDay = Calendar.getInstance()
            scheduleDay.add(Calendar.DAY_OF_MONTH, index * 2) // Space out tasks

            // Set appropriate times for different tasks
            when {
                task.taskName.contains(
                    "Remove",
                    ignoreCase = true
                ) -> scheduleDay.set(Calendar.HOUR_OF_DAY, 10)

                task.taskName.contains(
                    "Apply",
                    ignoreCase = true
                ) -> scheduleDay.set(Calendar.HOUR_OF_DAY, 17)

                else -> scheduleDay.set(Calendar.HOUR_OF_DAY, 12)
            }
            scheduleDay.set(Calendar.MINUTE, 0)
            scheduleDay.set(Calendar.SECOND, 0)

            val plantCountText = if (plantCount == 1) "(1 plant)" else "($plantCount plants)"
            val taskId =
                "treatment_${plantId}_${conditionName}_${System.currentTimeMillis() + index}"

            val treatmentEvent = PlantDatabaseManager.PlantCareEvent(
                id = taskId,
                plantId = plantId,
                eventType = "Treat: $conditionName",
                date = scheduleDay.time,
                conditionName = conditionName,
                notes = "${task.taskName} for $conditionName $plantCountText\n\n${task.description}\n\nMaterials: ${
                    task.materials.joinToString(
                        ", "
                    )
                }\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
                completed = false
            )

            val success = plantDatabaseManager.addPlantCareEvent(treatmentEvent)
            Log.d("MainActivity", "Treatment task #$index created: $success (${task.taskName})")

            // Add follow-up tasks with controlled scheduling if the task has a schedule interval
            if (task.scheduleInterval > 0) {
                val followUpCalendar = Calendar.getInstance()
                followUpCalendar.time = scheduleDay.time

                // Limit to 1 follow-up task instead of 2 to further reduce duplicates
                val maxFollowUps = 1

                for (followUpIndex in 1..maxFollowUps) {
                    followUpCalendar.add(Calendar.DAY_OF_MONTH, task.scheduleInterval)

                    val followUpId =
                        "followup_${plantId}_${conditionName}_${System.currentTimeMillis() + followUpIndex * 100}"
                    val followUpEvent = PlantDatabaseManager.PlantCareEvent(
                        id = followUpId,
                        plantId = plantId,
                        eventType = "Treat: $conditionName",
                        date = followUpCalendar.time,
                        conditionName = conditionName,
                        notes = "Follow-up #$followUpIndex: ${task.taskName} $plantCountText\n\n${task.description}\n\nMaterials: ${
                            task.materials.joinToString(
                                ", "
                            )
                        }\n\nInstructions:\n${task.instructions.joinToString("\n- ", "- ")}",
                        completed = false
                    )

                    val followUpSuccess = plantDatabaseManager.addPlantCareEvent(followUpEvent)
                    Log.d(
                        "MainActivity",
                        "Follow-up task #$followUpIndex created: $followUpSuccess (${task.taskName})"
                    )
                }
            }
        }

        // Do one final check for duplicates after creation
        checkForDuplicateTreatments(plantId, conditionName)

        Log.d("MainActivity", "Completed creating treatment schedule for $conditionName")
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
        // Stop camera before showing dialog
        stopCamera()

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
            dialogView.findViewById<android.widget.Button>(R.id.goToManagementButton)

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
                intent.putExtra(
                    "SHOW_TREATMENT_PLAN",
                    true
                ) // Important: This ensures treatment plan is shown
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
                intent.putExtra("SHOW_TREATMENT_PLAN", true) // Important: Force show treatment plan
                startActivity(intent)
                finish()
            } else {
                // Only restart camera if we're not finishing the activity
                if (!isFinishing) {
                    startCamera()
                }
            }
        }

        // Create and show the dialog
        infoDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Handle dialog dismissal to restart camera if needed
        infoDialog?.setOnDismissListener {
            // Ensure the reference is cleared
            infoDialog = null

            // Only restart camera if we're not finishing the activity
            if (!isFinishing) {
                startCamera()
            }
        }

        infoDialog?.show()
    }

    private fun showAddToCalendarDialog(conditionName: String) {
        stopCamera()
        try {
            // Stop camera and scanning to prevent resource issues
            if (isScanning) {
                toggleScanning()
                stopCamera()
            }
            stopCamera() // Stop camera to free resources

            // Show loading indicator while preparing dialog
            val loadingDialog = ProgressDialog(this).apply {
                setMessage("Preparing...")
                setCancelable(false)
                stopCamera()
            }
            loadingDialog.show()

            // Validate inputs
            if (conditionName.isBlank()) {
                Toast.makeText(this, "Invalid condition", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
                return
                stopCamera()
            }

            // Safely inflate dialog view
            val dialogView = try {
                LayoutInflater.from(this).inflate(R.layout.dialog_add_to_calendar, null)
            } catch (e: Exception) {
                Log.e("AddPlantDialog", "Failed to inflate dialog layout", e)
                Toast.makeText(this, "Error creating dialog", Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
                return
            }

            // Find views with null checks
            val detectionResultText = dialogView.findViewById<TextView?>(R.id.detectionResultText)
            val plantNameInput = dialogView.findViewById<EditText?>(R.id.plantNameInput)
            val cancelButton = dialogView.findViewById<Button?>(R.id.cancelButton)
            val addPlantButton = dialogView.findViewById<Button?>(R.id.addPlantButton)

            // Verify all critical views are present
            val viewChecks = listOf(
                "Detection Result" to detectionResultText,
                "Plant Name Input" to plantNameInput,
                "Cancel Button" to cancelButton,
                "Add Plant Button" to addPlantButton
            )

            for ((name, view) in viewChecks) {
                if (view == null) {
                    Log.e("AddPlantDialog", "Missing view: $name")
                    Toast.makeText(this, "Dialog setup error", Toast.LENGTH_SHORT).show()
                    loadingDialog.dismiss()
                    return
                }
            }

            // Set detection result text
            detectionResultText.text = "Detected condition: $conditionName"

            // Suggest default plant name
            val suggestedName = try {
                val baseType = when {
                    conditionName.contains("Tomato", ignoreCase = true) -> "Tomato"
                    conditionName.contains("Eggplant", ignoreCase = true) -> "Eggplant"
                    else -> selectedVegetable ?: "Plant"
                }
                val existingPlantsCount = try {
                    plantDatabaseManager.getAllPlants().size
                } catch (e: Exception) {
                    Log.e("AddPlantDialog", "Error getting plant count", e)
                    0
                }
                "$baseType Plant ${existingPlantsCount + 1}"
            } catch (e: Exception) {
                Log.e("AddPlantDialog", "Error generating plant name", e)
                "New Plant"
            }

            plantNameInput.setText(suggestedName)

            // Dismiss loading dialog now that we've set up the dialog
            loadingDialog.dismiss()

            // Cancel button
            cancelButton.setOnClickListener {
                infoDialog?.dismiss()
                // Restart camera when canceling
                startCamera()
            }

            // Add plant button
            addPlantButton.setOnClickListener {
                val plantName = plantNameInput.text.toString().trim()

                if (plantName.isEmpty()) {
                    Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Disable button to prevent multiple clicks
                addPlantButton.isEnabled = false

                // Show progress dialog
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Adding plant...")
                    setCancelable(false)
                }
                progressDialog.show()

                // Use a background thread for database operations
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

                        // Run UI updates on main thread
                        runOnUiThread {
                            progressDialog.dismiss()

                            if (plantId.isNotEmpty()) {
                                // Create care plan
                                createCompletePlantCarePlan(plantId, plantType, 2)

                                // Dismiss dialogs
                                infoDialog?.dismiss()

                                // Navigate to plant management
                                val intent = Intent(this, PlantManagementActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Failed to add plant", Toast.LENGTH_SHORT)
                                    .show()
                                addPlantButton.isEnabled = true
                                // Restart camera if we're not navigating away
                                startCamera()
                            }
                        }
                    } catch (e: Exception) {
                        // Run on UI thread to show error
                        runOnUiThread {
                            progressDialog.dismiss()
                            Log.e("AddPlantDialog", "Error adding plant", e)
                            Toast.makeText(
                                this,
                                "Error adding plant: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                            addPlantButton.isEnabled = true
                            // Restart camera on error
                            startCamera()
                        }
                    }
                }.start()
            }

            // Create and show dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Handle dialog dismissal to restart camera if needed
            dialog.setOnDismissListener {
                if (infoDialog == dialog) {
                    infoDialog = null
                    // Only restart camera if we're not already navigating away
                    if (!isFinishing) {
                        startCamera()
                    }
                }
            }

            dialog.show()
            infoDialog = dialog

        } catch (e: Exception) {
            Log.e("AddPlantDialog", "Unexpected error in dialog creation", e)
            Toast.makeText(this, "Failed to create dialog", Toast.LENGTH_LONG).show()
            // Make sure to restart camera on error
            startCamera()
        }
    }

    // 2. Add function to stop the camera safely
    private fun stopCamera() {
        try {
            imageAnalyzer?.clearAnalyzer()
            cameraProvider?.unbindAll()
            Log.d(TAG, "Camera stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera: ${e.message}")
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

    private fun checkForDuplicateTreatments(plantId: String, conditionName: String) {
        Log.d("MainActivity", "Checking for duplicate treatments for $conditionName")

        try {
            // Get all events for this plant
            val allEvents = plantDatabaseManager.getPlantCareEvents(plantId)

            // Find all treatment events for this specific condition that are not completed
            val treatmentEvents = allEvents.filter { event ->
                (event.eventType == "Treat: $conditionName" ||
                        (event.eventType == "Treatment" && event.conditionName == conditionName)) &&
                        !event.completed
            }

            // Group events by description/task name to find duplicates
            val eventsByDescription =
                mutableMapOf<String, MutableList<PlantDatabaseManager.PlantCareEvent>>()

            for (event in treatmentEvents) {
                // Extract the task name from the notes (usually in first line or after URGENT:)
                val taskName = when {
                    event.notes.startsWith("URGENT:") -> event.notes.substringAfter("URGENT:")
                        .substringBefore(" for").trim()

                    event.notes.contains(":") -> event.notes.substringBefore(":").trim()
                    else -> event.notes.substringBefore("\n").trim()
                }

                // Use simplified task name as key to detect similar tasks
                val key = taskName.toLowerCase().replace(Regex("[^a-z0-9]"), "")

                if (!eventsByDescription.containsKey(key)) {
                    eventsByDescription[key] = mutableListOf()
                }
                eventsByDescription[key]?.add(event)
            }

            // For each group of similar events, keep only the first one and delete the rest
            for ((_, events) in eventsByDescription) {
                if (events.size > 1) {
                    // Keep the first event (sorted by date)
                    val sortedEvents = events.sortedBy { it.date }
                    val keepEvent = sortedEvents.first()

                    // Delete the duplicate events
                    for (i in 1 until sortedEvents.size) {
                        val deleteEvent = sortedEvents[i]
                        Log.d(
                            "MainActivity",
                            "Deleting duplicate treatment: ${deleteEvent.id} (${
                                deleteEvent.notes.substringBefore("\n")
                            })"
                        )
                        plantDatabaseManager.deletePlantCareEvent(deleteEvent.id)
                    }

                    Log.d(
                        "MainActivity",
                        "Kept treatment: ${keepEvent.id} (${keepEvent.notes.substringBefore("\n")})"
                    )
                }
            }

            // Also check for follow-up events that might be duplicated
            val followUpEvents = allEvents.filter { event ->
                event.notes.contains("Follow-up") &&
                        event.conditionName == conditionName &&
                        !event.completed
            }

            // Group follow-ups by follow-up number and task
            val followUpsByNumberAndTask =
                mutableMapOf<String, MutableList<PlantDatabaseManager.PlantCareEvent>>()

            for (event in followUpEvents) {
                // Extract follow-up number
                val followUpMatch = Regex("Follow-up #(\\d+):").find(event.notes)
                val followUpNum = followUpMatch?.groupValues?.getOrNull(1) ?: "?"

                // Extract task name after the follow-up prefix
                val taskName =
                    event.notes.substringAfter("Follow-up #$followUpNum:").substringBefore(" for")
                        .trim()

                // Create a key combining follow-up number and simplified task name
                val key = "$followUpNum-${taskName.toLowerCase().replace(Regex("[^a-z0-9]"), "")}"

                if (!followUpsByNumberAndTask.containsKey(key)) {
                    followUpsByNumberAndTask[key] = mutableListOf()
                }
                followUpsByNumberAndTask[key]?.add(event)
            }

            // Remove duplicates in each follow-up group
            for ((key, events) in followUpsByNumberAndTask) {
                if (events.size > 1) {
                    // Keep the first event (sorted by date)
                    val sortedEvents = events.sortedBy { it.date }
                    val keepEvent = sortedEvents.first()

                    // Delete the duplicate events
                    for (i in 1 until sortedEvents.size) {
                        val deleteEvent = sortedEvents[i]
                        Log.d(
                            "MainActivity",
                            "Deleting duplicate follow-up: ${deleteEvent.id} (${key})"
                        )
                        plantDatabaseManager.deletePlantCareEvent(deleteEvent.id)
                    }

                    Log.d("MainActivity", "Kept follow-up: ${keepEvent.id} (${key})")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking for duplicate treatments: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure to clean up all resources
        try {
            stopCamera()
            detector?.close()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources: ${e.message}", e)
        }

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