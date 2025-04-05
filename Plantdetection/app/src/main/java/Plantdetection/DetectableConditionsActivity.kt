package com.PlantDetection

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
                    "Healthy Plant" to PlantConditionData.conditions["Healthy Tomato"]!!,
                    "Plant Diseases" to mapOf(
                        "Anthracnose (Diseased)" to PlantConditionData.conditions["Anthracnose (Diseased)"]!!,
                        "Blossom End Rot (Diseased)" to PlantConditionData.conditions["Blossom End Rot (Diseased)"]!!
                    ),
                    "Harmful Pests" to mapOf(
                        "Aphids (Infested)" to PlantConditionData.conditions["Aphids (Infested)"]!!,
                        "Cutworm (Infested)" to PlantConditionData.conditions["Cutworm (Infested)"]!!,
                        "Fruit Fly (Infested)" to PlantConditionData.conditions["Fruit Fly (Infested)"]!!
                    ),
                    "Beneficial Insects" to mapOf(
                        "Hippodamia Variegata/Lady Bug" to PlantConditionData.conditions["Hippodamia Variegata/Lady Bug"]!!
                    )
                )
                "Eggplant" -> mapOf(
                    "Healthy Plant" to PlantConditionData.conditions["Healthy eggplant"]!!,
                    "Plant Diseases" to mapOf(
                        "Collectotrichum rot (Diseased)" to PlantConditionData.conditions["Collectotrichum rot (Diseased)"]!!,
                        "Melon Thrips (Diseased)" to PlantConditionData.conditions["Melon Thrips (Diseased)"]!!
                    ),
                    "Harmful Pests" to mapOf(
                        "Aphids (Infested)" to PlantConditionData.conditions["Aphids (Infested)"]!!,
                        "Cutworm (Infested)" to PlantConditionData.conditions["Cutworm (Infested)"]!!,
                        "Fruit Fly (Infested)" to PlantConditionData.conditions["Fruit Fly (Infested)"]!!
                    ),
                    "Beneficial Insects" to mapOf(
                        "Hippodamia Variegata/Lady Bug" to PlantConditionData.conditions["Hippodamia Variegata/Lady Bug"]!!
                    )
                )
                else -> mapOf()
            }

            // Add conditions to the layout with organized sections
            conditions.forEach { (sectionName, sectionContent) ->
                // Add section header
                addSectionHeader(binding.conditionsContainer, sectionName)

                // Handle nested map for detailed sections
                if (sectionContent is Map<*, *>) {
                    (sectionContent as Map<String, PlantConditionData.PlantCondition>).forEach { (name, condition) ->
                        addConditionToLayout(binding.conditionsContainer, name, condition)
                    }
                } else {
                    // Handle single condition
                    addConditionToLayout(binding.conditionsContainer, sectionName, sectionContent as PlantConditionData.PlantCondition)
                }
            }
        } else {
            // All conditions view
            val allConditions = mapOf(
                "Tomato Plants" to mapOf(
                    "Healthy Tomato" to PlantConditionData.conditions["Healthy Tomato"]!!,
                    "Anthracnose (Diseased)" to PlantConditionData.conditions["Anthracnose (Diseased)"]!!,
                    "Blossom End Rot (Diseased)" to PlantConditionData.conditions["Blossom End Rot (Diseased)"]!!
                ),
                "Eggplant Plants" to mapOf(
                    "Healthy eggplant" to PlantConditionData.conditions["Healthy eggplant"]!!,
                    "Collectotrichum rot (Diseased)" to PlantConditionData.conditions["Collectotrichum rot (Diseased)"]!!,
                    "Melon Thrips (Diseased)" to PlantConditionData.conditions["Melon Thrips (Diseased)"]!!
                ),
                "Common Pests" to mapOf(
                    "Aphids (Infested)" to PlantConditionData.conditions["Aphids (Infested)"]!!,
                    "Cutworm (Infested)" to PlantConditionData.conditions["Cutworm (Infested)"]!!,
                    "Fruit Fly (Infested)" to PlantConditionData.conditions["Fruit Fly (Infested)"]!!
                ),
                "Beneficial Insects" to mapOf(
                    "Hippodamia Variegata/Lady Bug" to PlantConditionData.conditions["Hippodamia Variegata/Lady Bug"]!!
                )
            )

            // Add all conditions with organized sections
            allConditions.forEach { (sectionName, sectionContent) ->
                // Add section header
                addSectionHeader(binding.conditionsContainer, sectionName)

                // Add conditions for this section
                (sectionContent as Map<String, PlantConditionData.PlantCondition>).forEach { (name, condition) ->
                    addConditionToLayout(binding.conditionsContainer, name, condition)
                }
            }
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
            "Healthy eggplant" -> R.drawable.heggplant
            "Anthracnose (Diseased)" -> R.drawable.anthracnose
            "Blossom End Rot (Diseased)" -> R.drawable.blossom
            "Collectotrichum rot (Diseased)" -> R.drawable.rot
            "Melon Thrips (Diseased)" -> R.drawable.melon
            "Aphids (Infested)" -> R.drawable.aphids
            "Cutworm (Infested)" -> R.drawable.cutworm
            "Fruit Fly (Infested)" -> R.drawable.fruitfly
            "Hippodamia Variegata/Lady Bug" -> R.drawable.hippodamia
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