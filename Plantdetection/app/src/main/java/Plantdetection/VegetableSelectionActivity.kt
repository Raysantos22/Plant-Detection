package com.PlantDetection

import Plantdetection.PlantDatabaseManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.PlantDetection.databinding.ActivityVegetableSelectionBinding
import com.Plantdetection.LoadingActivity



/**
 * Activity for vegetable selection, either creating a new scan or continuing with a saved plant
 */
class VegetableSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVegetableSelectionBinding
    private var selectedVegetable: String? = null
    private var selectedPlantId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVegetableSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if we're continuing with an existing plant
        selectedPlantId = intent.getStringExtra("SELECTED_PLANT_ID")
        if (selectedPlantId != null) {
            // Load plant details
            val plantDatabaseManager = PlantDatabaseManager(this)
            val plant = plantDatabaseManager.getPlant(selectedPlantId!!)

            // If plant exists, set the vegetable type
            plant?.let {
                selectedVegetable = it.type

                // Show plant name in the title
                binding.titleText.text = "Scanning ${it.name}"

                // Pre-select the vegetable
                when (selectedVegetable) {
                    "Tomato" -> {
                        highlightSelectedVegetable(binding.tomatoImage)
                        binding.selectButton.isEnabled = true
                        binding.selectButton.alpha = 1.0f
                    }
                    "Eggplant" -> {
                        highlightSelectedVegetable(binding.eggplantImage)
                        binding.selectButton.isEnabled = true
                        binding.selectButton.alpha = 1.0f
                    }
                }
            }
        }

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

                // Pass along the plant ID if we have one
                if (selectedPlantId != null) {
                    intent.putExtra("SELECTED_PLANT_ID", selectedPlantId)
                }

                startActivity(intent)
            }
        }
    }

    private fun selectVegetable(vegetable: String) {
        selectedVegetable = vegetable
        binding.selectButton.isEnabled = true
        binding.selectButton.alpha = 1.0f
    }

    private fun highlightSelectedVegetable(selectedImageView: ImageView) {
        // Reset all borders
        binding.tomatoImage.setBackgroundResource(0)
        binding.eggplantImage.setBackgroundResource(0)

        // Highlight selected vegetable
        selectedImageView.setBackgroundResource(R.drawable.selected_vegetable_border)
    }
}
