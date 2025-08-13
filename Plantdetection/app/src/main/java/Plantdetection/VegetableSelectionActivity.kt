package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.PlantDetection.databinding.ActivityVegetableSelectionBinding
import kotlinx.coroutines.launch
import java.io.File

/**
 * Activity for vegetable selection with option to go to Plant Management screen
 */
class VegetableSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVegetableSelectionBinding
    private var selectedVegetable: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVegetableSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable the View Detectable Conditions button by default
        binding.plantDetectionButton.isEnabled = true
        binding.plantDetectionButton.alpha = 1.0f

        // Update text to indicate all conditions can be viewed
        binding.detectableConditionsText.text = "View all detectable conditions for all vegetables"

        setupClickListeners()
        loadVegetableImages()
    }

    private fun loadVegetableImages() {
        // Show loading state (optional)
        showImageLoadingState(true)

        lifecycleScope.launch {
            val imageViews: Map<String, ImageView> = mapOf(
                "tomato" to binding.tomatoImage,
                "eggplant" to binding.eggplantImage,
                "okra" to binding.okraImage,
                "bitter_gourd" to binding.bitterGourdImage,
                "chili_pepper" to binding.chiliPepperImage
            )

            ImageDownloader.loadAllVegetableImages(
                context = this@VegetableSelectionActivity,
                imageViews = imageViews,
                onProgress = { vegetableName: String, success: Boolean ->
                    runOnUiThread {
                        if (!success) {
                            // Handle individual image load failure - fall back to default
                            setDefaultImageForVegetable(vegetableName)
                            Toast.makeText(this@VegetableSelectionActivity,
                                "Using default image for $vegetableName", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            // Hide loading state
            showImageLoadingState(false)
        }
    }

    private fun setDefaultImageForVegetable(vegetableName: String) {
        val imageViews: Map<String, ImageView> = mapOf(
            "tomato" to binding.tomatoImage,
            "eggplant" to binding.eggplantImage,
            "okra" to binding.okraImage,
            "bitter_gourd" to binding.bitterGourdImage,
            "chili_pepper" to binding.chiliPepperImage
        )

        imageViews[vegetableName]?.let { imageView: ImageView ->
            val singleImageMap: Map<String, ImageView> = mapOf(vegetableName to imageView)
            ImageDownloader.setDefaultImages(singleImageMap)
        }
    }

    private fun revertToDefaultImages() {
        val imageViews: Map<String, ImageView> = mapOf(
            "tomato" to binding.tomatoImage,
            "eggplant" to binding.eggplantImage,
            "okra" to binding.okraImage,
            "bitter_gourd" to binding.bitterGourdImage,
            "chili_pepper" to binding.chiliPepperImage
        )

        ImageDownloader.setDefaultImages(imageViews)
        Toast.makeText(this, "Reverted to default images", Toast.LENGTH_SHORT).show()
    }

    private fun showImageLoadingState(isLoading: Boolean) {
        // You can add loading indicators here
        if (isLoading) {
            // Optionally show placeholder images or loading animation
            Toast.makeText(this, "Loading vegetable images...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Vegetable selection logic - all vegetable types
        binding.tomatoContainer.setOnClickListener {
            selectVegetable("Tomato")
            highlightSelectedVegetable(binding.tomatoImage)
        }

        binding.eggplantContainer.setOnClickListener {
            selectVegetable("Eggplant")
            highlightSelectedVegetable(binding.eggplantImage)
        }

        binding.okraContainer.setOnClickListener {
            selectVegetable("Okra")
            highlightSelectedVegetable(binding.okraImage)
        }

        binding.bitterGourdContainer.setOnClickListener {
            selectVegetable("Bitter Gourd")
            highlightSelectedVegetable(binding.bitterGourdImage)
        }

        binding.chiliPepperContainer.setOnClickListener {
            selectVegetable("Chili Pepper")
            highlightSelectedVegetable(binding.chiliPepperImage)
        }

        binding.selectButton.setOnClickListener {
            if (selectedVegetable != null) {
                val intent = Intent(this, LoadingActivity::class.java)
                intent.putExtra("SELECTED_VEGETABLE", selectedVegetable)
                startActivity(intent)
            }
        }

        // Add button to go to plant management screen
        binding.managePlantsButton.setOnClickListener {
            val intent = Intent(this, PlantManagementActivity::class.java)
            startActivity(intent)
        }

        // Plant detection button - launches DetectableConditionsActivity without requiring selection
        binding.plantDetectionButton.setOnClickListener {
            // Launch the DetectableConditionsActivity
            val intent = Intent(this, DetectableConditionsActivity::class.java)

            // Pass selected vegetable if one is selected
            if (selectedVegetable != null) {
                intent.putExtra("SELECTED_VEGETABLE", selectedVegetable)
            }

            startActivity(intent)
        }

        // Settings button - show settings dialog
        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Settings")

        val options = arrayOf(
            "Re-download Model Files",
            "Check Model Status",
            "Clear App Data",
            "Reload Vegetable Images",
            "Clear Image Cache"
        )

        dialogBuilder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    // Re-download model files
                    showRedownloadConfirmationDialog()
                }
                1 -> {
                    // Check model status
                    checkModelStatus()
                }
                2 -> {
                    // Clear app data
                    showClearDataConfirmationDialog()
                }
                3 -> {
                    // Reload vegetable images
                    loadVegetableImages()
                }
                4 -> {
                    // Clear image cache and revert to defaults
                    ImageDownloader.clearImageCache(this@VegetableSelectionActivity)
                    // After cache is cleared, revert to default images
                    revertToDefaultImages()
                }
            }
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.create().show()
    }

    // ... [Keep all other existing methods like showRedownloadConfirmationDialog, 
    //      showClearDataConfirmationDialog, redownloadModelFiles, checkModelStatus, 
    //      clearAppData, selectVegetable, highlightSelectedVegetable, 
    //      updateDetectableConditionsInfo exactly as they were]

    private fun showRedownloadConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Re-download Model Files")
            .setMessage("This will delete existing model files and download fresh copies. This may take a few minutes depending on your internet connection.\n\nProceed?")
            .setPositiveButton("Yes, Re-download") { _, _ ->
                redownloadModelFiles()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showClearDataConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear App Data")
            .setMessage("This will delete all model files and require re-downloading on next scan. Are you sure?")
            .setPositiveButton("Yes, Clear") { _, _ ->
                clearAppData()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun redownloadModelFiles() {
        // Create a custom progress dialog using AlertDialog
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Re-downloading Model Files")
            .setMessage("Preparing to download...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // Delete existing files first
        try {
            val modelFile = File(filesDir, Constants.MODEL_PATH)
            val labelsFile = File(filesDir, Constants.LABELS_PATH)

            if (modelFile.exists()) {
                modelFile.delete()
            }
            if (labelsFile.exists()) {
                labelsFile.delete()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error deleting old files: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Start download
        lifecycleScope.launch {
            SupabaseModelDownloader.downloadModelFiles(
                context = this@VegetableSelectionActivity,
                onProgress = { progress, message ->
                    runOnUiThread {
                        progressDialog.setMessage("$message\nProgress: $progress%")
                    }
                },
                onComplete = { modelPath, labelsPath ->
                    runOnUiThread {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@VegetableSelectionActivity)
                            .setTitle("Download Complete")
                            .setMessage("Model files have been successfully re-downloaded!\n\nModel: ${File(modelPath).length() / 1024 / 1024} MB\nLabels: ${File(labelsPath).length() / 1024} KB")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                },
                onError = { exception ->
                    runOnUiThread {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@VegetableSelectionActivity)
                            .setTitle("Download Failed")
                            .setMessage("Failed to re-download model files:\n${exception.message}")
                            .setPositiveButton("Retry") { _, _ ->
                                redownloadModelFiles()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            )
        }
    }

    private fun checkModelStatus() {
        val modelFile = File(filesDir, Constants.MODEL_PATH)
        val labelsFile = File(filesDir, Constants.LABELS_PATH)

        val statusMessage = StringBuilder()
        statusMessage.append("Model Files Status:\n\n")

        if (modelFile.exists()) {
            val modelSizeMB = modelFile.length() / 1024 / 1024
            val modelModified = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(modelFile.lastModified()))
            statusMessage.append("✓ Model File: ${modelSizeMB} MB\n")
            statusMessage.append("  Last Modified: $modelModified\n\n")
        } else {
            statusMessage.append("✗ Model File: Not Found\n\n")
        }

        if (labelsFile.exists()) {
            val labelsSizeKB = labelsFile.length() / 1024
            val labelsModified = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(labelsFile.lastModified()))
            statusMessage.append("✓ Labels File: ${labelsSizeKB} KB\n")
            statusMessage.append("  Last Modified: $labelsModified\n\n")
        } else {
            statusMessage.append("✗ Labels File: Not Found\n\n")
        }

        val canScan = modelFile.exists() && labelsFile.exists() &&
                modelFile.length() > 0 && labelsFile.length() > 0

        if (canScan) {
            statusMessage.append("Status: ✓ Ready for scanning")
        } else {
            statusMessage.append("Status: ✗ Files missing or corrupted\nPlease re-download model files")
        }

        AlertDialog.Builder(this)
            .setTitle("Model Status")
            .setMessage(statusMessage.toString())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Re-download") { _, _ ->
                showRedownloadConfirmationDialog()
            }
            .show()
    }

    private fun clearAppData() {
        try {
            val modelFile = File(filesDir, Constants.MODEL_PATH)
            val labelsFile = File(filesDir, Constants.LABELS_PATH)

            val deletedFiles = mutableListOf<String>()

            if (modelFile.exists() && modelFile.delete()) {
                deletedFiles.add("Model file")
            }
            if (labelsFile.exists() && labelsFile.delete()) {
                deletedFiles.add("Labels file")
            }

            val message = if (deletedFiles.isNotEmpty()) {
                "Deleted: ${deletedFiles.joinToString(", ")}"
            } else {
                "No files found to delete"
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error clearing data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun selectVegetable(vegetable: String) {
        selectedVegetable = vegetable
        binding.selectButton.isEnabled = true
        binding.selectButton.alpha = 1.0f

        // Update the detectable conditions based on selected vegetable
        updateDetectableConditionsInfo(vegetable)
    }

    private fun highlightSelectedVegetable(selectedImageView: ImageView) {
        // Reset all borders
        binding.tomatoImage.setBackgroundResource(0)
        binding.eggplantImage.setBackgroundResource(0)
        binding.okraImage.setBackgroundResource(0)
        binding.bitterGourdImage.setBackgroundResource(0)
        binding.chiliPepperImage.setBackgroundResource(0)

        // Highlight selected vegetable
        selectedImageView.setBackgroundResource(R.drawable.selected_vegetable_border)
    }

    private fun updateDetectableConditionsInfo(vegetable: String) {
        // Update text in the detection info section based on selected vegetable
        val detectableConditions = when (vegetable) {
            "Tomato" -> listOf(
                "Healthy Tomato",
                "Blossom End Rot (Tomato) (Diseased)",
                "Aphids (Infested)",
                "Cutworm (Infested)",
                "Fruit Fly (Infested)",
                "Hippodamia Variegata/Lady Bug"
            )
            "Eggplant" -> listOf(
                "Healthy eggplant",
                "Melon Thrips (Eggplant) (Diseased)",
                "Aphids (Infested)",
                "Cutworm (Infested)",
                "Fruit Fly (Infested)",
                "Hippodamia Variegata/Lady Bug"
            )
            "Okra" -> listOf(
                "Healthy okra",
                "Blossom Blight (Okra) (Diseased)",
                "Aphids (Infested)",
                "Cutworm (Infested)",
                "Fruit Fly (Infested)",
                "Hippodamia Variegata/Lady Bug"
            )
            "Bitter Gourd" -> listOf(
                "Healthy bitter gourd",
                "Phytophthora Fruit Rot (Bitter Gourd) (Diseased)",
                "Aphids (Infested)",
                "Cutworm (Infested)",
                "Fruit Fly (Infested)",
                "Hippodamia Variegata/Lady Bug"
            )
            "Chili pepper" -> listOf(
                "Healthy Chili pepper",
                "Anthracnose (Chili Pepper) (Diseased)",
                "Aphids (Infested)",
                "Cutworm (Infested)",
                "Fruit Fly (Infested)",
                "Hippodamia Variegata/Lady Bug"
            )
            else -> emptyList()
        }

        val conditionsText = detectableConditions.joinToString(", ")
        binding.detectableConditionsText.text = "Detectable conditions: $conditionsText"
    }
}