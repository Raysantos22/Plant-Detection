package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.PlantDetection.databinding.ActivityVegetableSelectionBinding
import com.PlantDetection.LoadingActivity
import com.Plantdetection.DetectableConditionsActivity

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
        binding.detectableConditionsText.text = "View all detectable conditions for both plants"

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Vegetable selection logic - only tomato and eggplant
        binding.tomatoContainer.setOnClickListener {
            selectVegetable("Tomato")
            highlightSelectedVegetable(binding.tomatoImage)
        }

        binding.eggplantContainer.setOnClickListener {
            selectVegetable("Eggplant")
            highlightSelectedVegetable(binding.eggplantImage)
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

        // Highlight selected vegetable
        selectedImageView.setBackgroundResource(R.drawable.selected_vegetable_border)
    }

    private fun updateDetectableConditionsInfo(vegetable: String) {
        // Update text in the detection info section based on selected vegetable
        val detectableConditions = when (vegetable) {
            "Tomato" -> listOf("Healthy Tomato", "Anthracnose", "Blossom End Rot", "Leaf Caterpillar", "Leaf Roller", "Hippodamia Variegata", "Rice Water Weevil")
            "Eggplant" -> listOf("Healthy Eggplant", "Collectotrichum rot", "Melon thrips", "Leaf Caterpillar", "Leaf Roller", "Hippodamia Variegata", "Rice Water Weevil")
            else -> emptyList()
        }

        val conditionsText = detectableConditions.joinToString(", ")
        binding.detectableConditionsText.text = "Detectable conditions: $conditionsText"
    }
}