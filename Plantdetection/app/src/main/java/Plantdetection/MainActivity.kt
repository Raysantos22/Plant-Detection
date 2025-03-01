package com.PlantDetection

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
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
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.orange))
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
                scanButton.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.app_dark_green))
                scanStatusText.text = "Not scanning"
                scanStatusText.setTextColor(ContextCompat.getColor(baseContext, R.color.gray))
                overlay.clear()
                detectionText.visibility = View.INVISIBLE
                captureButton.isEnabled = false
            }
        }
    }

    private fun captureDetection() {
        // If we have a valid detection, show details and save it
        currentDetection?.let { detection ->
            // Check if the detected condition matches our selected vegetable type
            val isRelevantToSelectedVegetable = when (selectedVegetable) {
                "Tomato" -> detection.clsName != "Healthy Eggplant"
                "Eggplant" -> detection.clsName != "Healthy Tomato"
                else -> true
            }

            if (isRelevantToSelectedVegetable && PlantConditionData.conditions.containsKey(detection.clsName)) {
                // First stop scanning
                if (isScanning) {
                    toggleScanning()
                }

                // If we have a plant ID, update the plant's condition
                if (selectedPlantId != null) {
                    updatePlantCondition(selectedPlantId!!, detection.clsName)
                    // Note: showPlantInfoDialog is called inside updatePlantCondition for rescans
                } else {
                    // Show info dialog for fresh scan
                    showPlantInfoDialog(detection.clsName, true, false)
                }
            } else {
                Toast.makeText(this, "Invalid detection for selected vegetable", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No valid detection to capture", Toast.LENGTH_SHORT).show()
        }
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

                    Toast.makeText(this, "Treatment plan created for ${conditionName}", Toast.LENGTH_SHORT).show()
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

                    Toast.makeText(this, "New treatment plan created for ${conditionName}", Toast.LENGTH_SHORT).show()
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
            (it.eventType.startsWith("Treat: ") || it.eventType.equals("Treatment", ignoreCase = true)) &&
                    !it.completed
        }

        // Mark each one as completed
        for (event in pendingTreatments) {
            val updatedEvent = event.copy(completed = true)
            plantDatabaseManager.updatePlantCareEvent(updatedEvent)
        }
    }

    private fun createAutomaticTreatmentSchedule(plantId: String, conditionName: String) {
        // Get the condition data
        val condition = PlantConditionData.conditions[conditionName]

        if (condition != null) {
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
                    task.taskName.contains("Remove", ignoreCase = true) -> today.set(Calendar.HOUR_OF_DAY, 10)
                    task.taskName.contains("Apply", ignoreCase = true) -> today.set(Calendar.HOUR_OF_DAY, 17)
                    else -> today.set(Calendar.HOUR_OF_DAY, 12)
                }
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)

                // Create detailed and descriptive notes for the treatment
                val taskNotes = "${treatmentTitle}\n\n" +
                        "${task.taskName}: ${task.description}\n\n" +
                        "Materials needed:\n" +
                        task.materials.joinToString("\n", "• ") + "\n\n" +
                        "Instructions:\n" +
                        task.instructions.joinToString("\n", "• ")

                // Create treatment event with disease name in title and detailed notes
                val treatmentEvent = PlantDatabaseManager.PlantCareEvent(
                    id = taskId,
                    plantId = plantId,
                    eventType = "Treat: ${condition.name}",
                    date = today.time,
                    conditionName = condition.name,
                    notes = taskNotes,
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

                        val followUpId = "followup_${plantId}_${System.currentTimeMillis() + index + followUpIndex * 100}"

                        // Create detailed notes for follow-up
                        val followUpNotes = "${treatmentTitle}\n\n" +
                                "Follow-up #$followUpIndex: ${task.taskName}\n\n" +
                                "${task.description}\n\n" +
                                "Materials needed:\n" +
                                task.materials.joinToString("\n", "• ") + "\n\n" +
                                "Instructions:\n" +
                                task.instructions.joinToString("\n", "• ")

                        val followUpEvent = PlantDatabaseManager.PlantCareEvent(
                            id = followUpId,
                            plantId = plantId,
                            eventType = "Treat: ${condition.name}",
                            date = followUpCalendar.time,
                            conditionName = condition.name,
                            notes = followUpNotes,
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
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
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
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    // Show dialog with plant condition information, prevention and treatment tips
    private fun showPlantInfoDialog(conditionName: String, showMonitoringOptions: Boolean = false, isRescan: Boolean = false) {
        // Get condition data
        val condition = PlantConditionData.conditions[conditionName] ?: return

        // Create dialog with custom layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plant_info, null)

        // Set condition details
        val conditionTitle = dialogView.findViewById<TextView>(R.id.conditionTitle)
        val conditionDescription = dialogView.findViewById<TextView>(R.id.conditionDescription)
        val preventionTipsContainer = dialogView.findViewById<LinearLayout>(R.id.preventionTipsContainer)
        val treatmentTipsContainer = dialogView.findViewById<LinearLayout>(R.id.treatmentTipsContainer)
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val monitoringSection = dialogView.findViewById<View>(R.id.monitoringSection)
        val setReminderButton = dialogView.findViewById<android.widget.Button>(R.id.setReminderButton)
        val taskCompleteButton = dialogView.findViewById<android.widget.Button>(R.id.taskCompleteButton)
        val addToCalendarButton = dialogView.findViewById<android.widget.Button>(R.id.addToCalendarButton)
        val goToManagementButton = dialogView.findViewById<android.widget.Button>(R.id.goToManagementButton) // Add this to your layout

        conditionTitle.text = condition.name
        conditionDescription.text = condition.description

        // Add prevention tips
        preventionTipsContainer.removeAllViews()
        condition.preventionTips.forEach { tip ->
            val tipView = LayoutInflater.from(this).inflate(R.layout.item_prevention_tip, preventionTipsContainer, false)
            tipView.findViewById<TextView>(R.id.tipText).text = tip
            preventionTipsContainer.addView(tipView)
        }

        // Add treatment tips
        treatmentTipsContainer.removeAllViews()
        condition.treatmentTips.forEach { tip ->
            val tipView = LayoutInflater.from(this).inflate(R.layout.item_treatment_tip, treatmentTipsContainer, false)
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
        val wateringFrequencyText = dialogView.findViewById<TextView>(R.id.wateringFrequencyText)
        val decreaseFrequencyButton = dialogView.findViewById<Button>(R.id.decreaseFrequencyButton)
        val increaseFrequencyButton = dialogView.findViewById<Button>(R.id.increaseFrequencyButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addPlantButton = dialogView.findViewById<Button>(R.id.addPlantButton)

        // Set initial values
        detectionResultText.text = "Detected condition: $conditionName"
        var wateringFrequency = 2 // Default to 2 days
        wateringFrequencyText.text = "$wateringFrequency days"

        // Suggest a default name
        val suggestedName = when {
            conditionName.contains("Tomato") -> "Tomato Plant"
            conditionName.contains("Eggplant") -> "Eggplant Plant"
            else -> "Plant"
        } + " " + (plantDatabaseManager.getAllPlants().size + 1)
        plantNameInput.setText(suggestedName)

        // Set up buttons
        decreaseFrequencyButton.setOnClickListener {
            if (wateringFrequency > 1) {
                wateringFrequency--
                wateringFrequencyText.text = "$wateringFrequency days"
            }
        }

        increaseFrequencyButton.setOnClickListener {
            if (wateringFrequency < 14) {
                wateringFrequency++
                wateringFrequencyText.text = "$wateringFrequency days"
            }
        }

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

                    // Also update the next watering date
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, wateringFrequency)
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)

                    plantDatabaseManager.scheduleWatering(plantId, calendar.time, "Regular watering")

                    // Test notification
                    plantDatabaseManager.testNotification(plantId)
                }

                Toast.makeText(this, "Plant added to monitoring", Toast.LENGTH_SHORT).show()

                // Dismiss dialogs
                infoDialog?.dismiss()

                // Go to plant management screen
                val intent = Intent(this, PlantManagementActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to add plant", Toast.LENGTH_SHORT).show()
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
                            "Treatment scheduled for ${java.text.SimpleDateFormat("MMM d, yyyy HH:mm",
                                java.util.Locale.getDefault()).format(calendar.time)}",
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
                    val healthyCondition = if (it.type == "Tomato") "Healthy Tomato" else "Healthy Eggplant"

                    // Update plant condition
                    val updatedPlant = it.copy(
                        currentCondition = healthyCondition,
                        lastScannedDate = Date()
                    )

                    // Save to database
                    plantDatabaseManager.updatePlant(updatedPlant)

                    Toast.makeText(this, "Great! Plant condition marked as healthy.", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()

                // Return to the plant management screen
                val intent = Intent(this, PlantManagementActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                Toast.makeText(this, "Continue following the treatment plan.", Toast.LENGTH_SHORT).show()
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
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
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

                // Store the highest confidence detection for when the user wants to capture it
                if (boundingBoxes.isNotEmpty()) {
                    val highestConfBox = boundingBoxes.maxByOrNull { it.cnf }

                    if (highestConfBox != null && highestConfBox.cnf > MIN_CONFIDENCE_THRESHOLD) {
                        currentDetection = highestConfBox
                        binding.captureButton.isEnabled = true
                        binding.detectionText.text = "${highestConfBox.clsName}: ${(highestConfBox.cnf * 100).toInt()}%"
                        binding.detectionText.visibility = View.VISIBLE
                    } else {
                        binding.captureButton.isEnabled = false
                        binding.detectionText.visibility = View.INVISIBLE
                        currentDetection = null
                    }
                } else {
                    binding.captureButton.isEnabled = false
                    binding.detectionText.visibility = View.INVISIBLE
                    currentDetection = null
                }
            }
        }
    }
}