package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.PlantDetection.databinding.ActivityDetectableConditionsBinding

class DetectableConditionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetectableConditionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectableConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set activity title
        title = "All Detectable Conditions"

        // Display all conditions
        displayAllConditions()

        // Set back button listener
        binding.startDetectionButton.apply {
            text = "BACK"
            setOnClickListener {
                // Simply finish this activity to go back
                finish()
            }
        }
    }

    private fun displayAllConditions() {
        // Clear the container first
        binding.conditionsContainer.removeAllViews()

        // Tomato Section
        addSectionHeader(binding.conditionsContainer, "Tomato")

        // Healthy Tomato
        addConditionToLayout(
            binding.conditionsContainer,
            "Healthy Tomato",
            PlantConditionData.conditions["Healthy Tomato"]!!
        )

        // Tomato Diseases
        addConditionToLayout(
            binding.conditionsContainer,
            "Anthracnose (Diseased)",
            PlantConditionData.conditions["Anthracnose (Diseased)"]!!
        )

        addConditionToLayout(
            binding.conditionsContainer,
            "Blossom End Rot (Diseased)",
            PlantConditionData.conditions["Blossom End Rot (Diseased)"]!!
        )

        // Eggplant Section
        addSectionHeader(binding.conditionsContainer, "Eggplant")

        // Healthy Eggplant
        addConditionToLayout(
            binding.conditionsContainer,
            "Healthy eggplant",
            PlantConditionData.conditions["Healthy eggplant"]!!
        )

        // Eggplant Diseases
        addConditionToLayout(
            binding.conditionsContainer,
            "Collectotrichum rot (Diseased)",
            PlantConditionData.conditions["Collectotrichum rot (Diseased)"]!!
        )

        addConditionToLayout(
            binding.conditionsContainer,
            "Melon Thrips (Diseased)",
            PlantConditionData.conditions["Melon Thrips (Diseased)"]!!
        )

        // Common Pests Section
        addSectionHeader(binding.conditionsContainer, "Common Pests")

        addConditionToLayout(
            binding.conditionsContainer,
            "Aphids (Infested)",
            PlantConditionData.conditions["Aphids (Infested)"]!!
        )

        addConditionToLayout(
            binding.conditionsContainer,
            "Cutworm (Infested)",
            PlantConditionData.conditions["Cutworm (Infested)"]!!
        )

        addConditionToLayout(
            binding.conditionsContainer,
            "Fruit Fly (Infested)",
            PlantConditionData.conditions["Fruit Fly (Infested)"]!!
        )

        // Beneficial Insects Section
        addSectionHeader(binding.conditionsContainer, "Beneficial Insects")

        addConditionToLayout(
            binding.conditionsContainer,
            "Hippodamia Variegata/Lady Bug",
            PlantConditionData.conditions["Hippodamia Variegata/Lady Bug"]!!
        )
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
            imageView.setImageResource(R.drawable.ic_plant_care)
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