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