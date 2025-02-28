package com.PlantDetection

import Plantdetection.PlantConditionData
import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
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
import com.PlantDetection.databinding.DialogPlantInfoBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false
    private var selectedVegetable: String? = null
    private var lastDetectedCondition: String? = null
    private var infoDialog: Dialog? = null

    // Scanning control variables
    private var isScanning = false
    private var currentDetection: BoundingBox? = null
    private val MIN_CONFIDENCE_THRESHOLD = 0.65f // Minimum confidence to enable capture

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

        // Get the selected vegetable from the intent
        selectedVegetable = intent.getStringExtra("SELECTED_VEGETABLE")

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
                scanButton.text = "Stop Scanning"
                scanButton.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.orange))
                scanStatusText.text = "Scanning..."
                scanStatusText.setTextColor(ContextCompat.getColor(baseContext, R.color.orange))
            } else {
                scanButton.text = "Start Scanning"
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

                // Save the detection
                saveDetection(detection)

                // Show info dialog
                showPlantInfoDialog(detection.clsName, true)
            } else {
                Toast.makeText(this, "Invalid detection for selected vegetable", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No valid detection to capture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDetection(detection: BoundingBox) {
        // Save detection to local database or preferences
        val sharedPrefs = getSharedPreferences("PlantMonitoring", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Create unique identifier for this detection
        val detectionId = "detection_${System.currentTimeMillis()}"

        // Save basic detection info
        editor.putString("${detectionId}_condition", detection.clsName)
        editor.putString("${detectionId}_date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
        editor.putString("${detectionId}_vegetable", selectedVegetable)

        // Add to list of detections
        val detectionsList = sharedPrefs.getStringSet("saved_detections", HashSet<String>()) ?: HashSet()
        val updatedList = HashSet(detectionsList)
        updatedList.add(detectionId)
        editor.putStringSet("saved_detections", updatedList)

        // Set up initial monitoring status
        editor.putLong("${detectionId}_next_check", System.currentTimeMillis() + getCheckIntervalForCondition(detection.clsName))

        editor.apply()

        // Schedule notification for next check
        scheduleCheckNotification(detectionId, detection.clsName)

        Toast.makeText(this, "Detection saved for monitoring", Toast.LENGTH_SHORT).show()
    }

    private fun getCheckIntervalForCondition(conditionName: String): Long {
        // Define check intervals based on condition (in milliseconds)
        return when {
            conditionName.startsWith("Healthy") -> 24 * 60 * 60 * 1000 // 24 hours for healthy plants
            else -> 12 * 60 * 60 * 1000 // 12 hours for diseased/pest conditions
        }
    }

    private fun scheduleCheckNotification(detectionId: String, conditionName: String) {
        // Use WorkManager to schedule notifications
        // This is a simplified example - in a real app, you'd implement WorkManager
        val title = if (conditionName.startsWith("Healthy")) {
            "Time to water your ${selectedVegetable}"
        } else {
            "Check your ${selectedVegetable} for ${conditionName}"
        }

        val message = if (conditionName.startsWith("Healthy")) {
            "Your plant needs regular care to stay healthy!"
        } else {
            "Follow the treatment steps to address the ${conditionName} condition."
        }

        // Here you would schedule the actual notification with WorkManager
        Toast.makeText(this, "Notification scheduled: $title", Toast.LENGTH_SHORT).show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
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

    // Show dialog with plant condition information, prevention and treatment tips
    private fun showPlantInfoDialog(conditionName: String, showMonitoringOptions: Boolean = false) {
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
        val setReminderButton = dialogView.findViewById<Button>(R.id.setReminderButton)
        val taskCompleteButton = dialogView.findViewById<Button>(R.id.taskCompleteButton)

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

        // Show monitoring options if requested
        if (showMonitoringOptions) {
            monitoringSection.visibility = View.VISIBLE

            // Set reminder button
            setReminderButton.setOnClickListener {
                showDateTimePicker(conditionName)
            }

            // Task complete button
            taskCompleteButton.setOnClickListener {
                markTaskComplete(conditionName)
                Toast.makeText(this, "Task marked as complete", Toast.LENGTH_SHORT).show()
                infoDialog?.dismiss()

                // If this was a disease/pest condition, ask if it's resolved
                if (!conditionName.startsWith("Healthy")) {
                    showResolutionConfirmationDialog()
                }
            }
        } else {
            monitoringSection.visibility = View.GONE
        }

        // Set up close button
        closeButton.setOnClickListener {
            infoDialog?.dismiss()
        }

        // Create and show the dialog
        infoDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        infoDialog?.show()
    }

    private fun showDateTimePicker(conditionName: String) {
        // Show a date/time picker dialog to set a custom reminder
        // This is a placeholder - you would implement a proper DateTimePicker
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.HOUR, 24) // Default to 24 hours later

        // Save the custom reminder time
        val sharedPrefs = getSharedPreferences("PlantMonitoring", MODE_PRIVATE)
        val detectionsList = sharedPrefs.getStringSet("saved_detections", HashSet<String>()) ?: HashSet()

        // For simplicity, just update the most recent detection
        if (detectionsList.isNotEmpty()) {
            val latestDetection = detectionsList.maxByOrNull { it }
            latestDetection?.let {
                val editor = sharedPrefs.edit()
                editor.putLong("${it}_next_check", calendar.timeInMillis)
                editor.apply()

                // Reschedule notification
                scheduleCheckNotification(it, conditionName)

                Toast.makeText(this, "Reminder set for ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(calendar.time)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markTaskComplete(conditionName: String) {
        // Mark the current task as complete and schedule the next check
        val sharedPrefs = getSharedPreferences("PlantMonitoring", MODE_PRIVATE)
        val detectionsList = sharedPrefs.getStringSet("saved_detections", HashSet<String>()) ?: HashSet()

        // For simplicity, just update the most recent detection
        if (detectionsList.isNotEmpty()) {
            val latestDetection = detectionsList.maxByOrNull { it }
            latestDetection?.let {
                val editor = sharedPrefs.edit()
                val nextCheckTime = System.currentTimeMillis() + getCheckIntervalForCondition(conditionName)
                editor.putLong("${it}_next_check", nextCheckTime)
                editor.putLong("${it}_last_completed", System.currentTimeMillis())
                editor.apply()

                // Reschedule notification
                scheduleCheckNotification(it, conditionName)
            }
        }
    }

    private fun showResolutionConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Issue Resolved?")
            .setMessage("Has the plant condition been resolved?")
            .setPositiveButton("Yes") { dialog, _ ->
                // Mark the condition as resolved
                Toast.makeText(this, "Great! Plant condition marked as resolved.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Return to the scanning screen
                startCamera()
            }
            .setNegativeButton("No") { dialog, _ ->
                Toast.makeText(this, "Continue following the treatment plan.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
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