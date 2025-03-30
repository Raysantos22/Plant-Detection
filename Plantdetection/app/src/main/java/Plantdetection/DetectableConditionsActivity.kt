package com.Plantdetection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.PlantDetection.LoadingActivity
import com.PlantDetection.PlantConditionData
import com.PlantDetection.R
import com.PlantDetection.databinding.ActivityDetectableConditionsBinding

class DetectableConditionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetectableConditionsBinding
    private var selectedVegetable: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectableConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get selected vegetable from intent (might be null)
        selectedVegetable = intent.getStringExtra("SELECTED_VEGETABLE")

        // Set title based on whether a vegetable is selected
        title = if (selectedVegetable != null) {
            "Detectable Conditions for $selectedVegetable"
        } else {
            "All Detectable Conditions"
        }

        // Display detectable conditions
        displayDetectableConditions()

        // Set start detection button listener
        binding.startDetectionButton.setOnClickListener {
            if (selectedVegetable != null) {
                val intent = Intent(this, LoadingActivity::class.java)
                intent.putExtra("SELECTED_VEGETABLE", selectedVegetable)
                startActivity(intent)
                finish()
            } else {
                // If no vegetable selected, show message to select vegetable first
                Toast.makeText(
                    this,
                    "Please go back and select a vegetable first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Update button text based on vegetable selection
        binding.startDetectionButton.apply {
            text = "BACK"
            setOnClickListener {
                // Simply finish this activity to go back
                finish()
            }
        }
    }

    private fun displayDetectableConditions() {
        // Clear the container first
        binding.conditionsContainer.removeAllViews()

        // Check if a vegetable is selected
        if (selectedVegetable != null) {
            // Show conditions for the selected vegetable
            val conditions = when (selectedVegetable) {
                "Tomato" -> mapOf(
                    "Healthy Tomato" to PlantConditionData.conditions["Healthy Tomato"]!!,
                    "Anthracnose" to PlantConditionData.conditions["Anthracnose"]!!,
                    "Blossom End Rot" to PlantConditionData.conditions["Blossom End Rot"]!!,
                    "Leaf Caterpillar" to PlantConditionData.conditions["Leaf Caterpillar"]!!,
                    "Leaf Roller" to PlantConditionData.conditions["Leaf Roller"]!!,
                    "Hippodamia Variegata" to PlantConditionData.conditions["Hippodamia Variegata"]!!,
                    "Rice Water Weevil" to PlantConditionData.conditions["Rice Water Weevil"]!!
                )
                "Eggplant" -> mapOf(
                    "Healthy Eggplant" to PlantConditionData.conditions["Healthy Eggplant"]!!,
                    "Collectotrichum rot" to PlantConditionData.conditions["Collectotrichum rot"]!!,
                    "Melon thrips" to PlantConditionData.conditions["Melon thrips"]!!,
                    "Leaf Caterpillar" to PlantConditionData.conditions["Leaf Caterpillar"]!!,
                    "Leaf Roller" to PlantConditionData.conditions["Leaf Roller"]!!,
                    "Hippodamia Variegata" to PlantConditionData.conditions["Hippodamia Variegata"]!!,
                    "Rice Water Weevil" to PlantConditionData.conditions["Rice Water Weevil"]!!
                )
                else -> mapOf()
            }

            // Add conditions to the layout
            for ((name, condition) in conditions) {
                addConditionToLayout(binding.conditionsContainer, name, condition)
            }
        } else {
            // Show all conditions organized by plant type

            // Add Tomato section header
            addSectionHeader(binding.conditionsContainer, "Tomato Plants")

            // Add Tomato conditions
            addConditionToLayout(binding.conditionsContainer, "Healthy Tomato", PlantConditionData.conditions["Healthy Tomato"]!!)
            addConditionToLayout(binding.conditionsContainer, "Anthracnose", PlantConditionData.conditions["Anthracnose"]!!)
            addConditionToLayout(binding.conditionsContainer, "Blossom End Rot", PlantConditionData.conditions["Blossom End Rot"]!!)

            // Add Eggplant section header
            addSectionHeader(binding.conditionsContainer, "Eggplant Plants")

            // Add Eggplant conditions
            addConditionToLayout(binding.conditionsContainer, "Healthy Eggplant", PlantConditionData.conditions["Healthy Eggplant"]!!)
            addConditionToLayout(binding.conditionsContainer, "Collectotrichum rot", PlantConditionData.conditions["Collectotrichum rot"]!!)
            addConditionToLayout(binding.conditionsContainer, "Melon thrips", PlantConditionData.conditions["Melon thrips"]!!)

            // Add Common Pests section header
            addSectionHeader(binding.conditionsContainer, "Common Pests (Both Plants)")

            // Add common pest conditions
            addConditionToLayout(binding.conditionsContainer, "Leaf Caterpillar", PlantConditionData.conditions["Leaf Caterpillar"]!!)
            addConditionToLayout(binding.conditionsContainer, "Leaf Roller", PlantConditionData.conditions["Leaf Roller"]!!)
            addConditionToLayout(binding.conditionsContainer, "Hippodamia Variegata", PlantConditionData.conditions["Hippodamia Variegata"]!!)
            addConditionToLayout(binding.conditionsContainer, "Rice Water Weevil", PlantConditionData.conditions["Rice Water Weevil"]!!)
        }
    }

    private fun addSectionHeader(container: LinearLayout, title: String) {
        // Create a section header view
        val headerView = TextView(this).apply {
            text = title
            textSize = 18f
            setBackgroundColor(resources.getColor(R.color.app_dark_green, null))
            setTextColor(resources.getColor(android.R.color.white, null))
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 16)
            }
        }

        container.addView(headerView)
    }

    private fun addConditionToLayout(container: LinearLayout, name: String, condition: PlantConditionData.PlantCondition) {
        val conditionView = LayoutInflater.from(this).inflate(R.layout.item_detectable_condition, container, false)

        val nameTextView = conditionView.findViewById<TextView>(R.id.conditionName)
        val descriptionTextView = conditionView.findViewById<TextView>(R.id.conditionDescription)

        nameTextView.text = name
        descriptionTextView.text = condition.description

        // Load condition image if available
        val imageView = conditionView.findViewById<ImageView>(R.id.conditionImage)
        val imageResourceId = getConditionImageResource(name)
        if (imageResourceId != 0) {
            imageView.setImageResource(imageResourceId)
        } else {
            // Use a placeholder if no specific image is found
            imageView.setImageResource(R.drawable.eggplant)
        }

        container.addView(conditionView)
    }

    private fun getConditionImageResource(conditionName: String): Int {
        return when (conditionName) {
            "Healthy Tomato" -> R.drawable.htomato
            "Healthy Eggplant" -> R.drawable.heggplant
            "Anthracnose" -> R.drawable.anthracnose
            "Blossom End Rot" -> R.drawable.blossom
            "Collectotrichum rot" -> R.drawable.rot
            "Melon thrips" -> R.drawable.melon
            "Leaf Caterpillar" -> R.drawable.cater
            "Leaf Roller" -> R.drawable.roller
            "Hippodamia Variegata" -> R.drawable.hippodamia
            "Rice Water Weevil" -> R.drawable.weevil
            else -> R.drawable.ic_plant_care
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}