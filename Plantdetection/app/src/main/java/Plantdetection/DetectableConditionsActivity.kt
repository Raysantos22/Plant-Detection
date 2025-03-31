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

        // Set title
        title = "All Detectable Conditions"

        // Display all detectable conditions regardless of selection
        displayDetectableConditions()

        // Update button text to go back
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

        // Always show all conditions organized by plant type, regardless of selection

        // Add Tomato section header
        addSectionHeader(binding.conditionsContainer, "Tomato (Diseases)")

        // Add Tomato conditions
        addConditionToLayout(binding.conditionsContainer, "Blossom End Rot (Tomato)",
            PlantConditionData.conditions["Blossom End Rot (Tomato) (Diseased)"]!!)

        // Add Eggplant section header
        addSectionHeader(binding.conditionsContainer, "Eggplant (Diseases)")

        // Add Eggplant conditions
        addConditionToLayout(binding.conditionsContainer, "Melon Thrips (Eggplant)",
            PlantConditionData.conditions["Melon Thrips (Eggplant) (Diseased)"]!!)

        // Add Okra section header
        addSectionHeader(binding.conditionsContainer, "Okra (Diseases)")

        // Add Okra conditions
        addConditionToLayout(binding.conditionsContainer, "Blossom Blight (Okra)",
            PlantConditionData.conditions["Blossom Blight (Okra) (Diseased)"]!!)

        // Add Bitter Gourd section header
        addSectionHeader(binding.conditionsContainer, "Bitter Gourd (Diseases)")

        // Add Bitter Gourd conditions
        addConditionToLayout(binding.conditionsContainer, "Phytophthora Fruit Rot (Bitter Gourd)",
            PlantConditionData.conditions["Phytophthora Fruit Rot (Bitter Gourd) (Diseased)"]!!)

        // Add Chili Pepper section header
        addSectionHeader(binding.conditionsContainer, "Chili Pepper (Diseases)")

        // Add Chili Pepper conditions
        addConditionToLayout(binding.conditionsContainer, "Anthracnose (Chili Pepper)",
            PlantConditionData.conditions["Anthracnose (Chili Pepper) (Diseased)"]!!)

        // Add Common Pests section header
        addSectionHeader(binding.conditionsContainer, "Common Pests (All Vegetables)")

        // Add common pest conditions
        addConditionToLayout(binding.conditionsContainer, "Aphids", PlantConditionData.conditions["Aphids (Infested)"]!!)
        addConditionToLayout(binding.conditionsContainer, "Cutworm", PlantConditionData.conditions["Cutworm (Infested)"]!!)
        addConditionToLayout(binding.conditionsContainer, "Fruit Fly", PlantConditionData.conditions["Fruit Fly (Infested)"]!!)

        // Add Beneficial Insects section
        addSectionHeader(binding.conditionsContainer, "Beneficial Insects")

        // Add beneficial insects
        addConditionToLayout(binding.conditionsContainer, "Hippodamia Variegata (Lady Bug)",
            PlantConditionData.conditions["Hippodamia Variegata/Lady Bug"]!!)

        // Add Healthy Plants section
        addSectionHeader(binding.conditionsContainer, "Healthy Plants")

        // Add healthy plant conditions
        addConditionToLayout(binding.conditionsContainer, "Healthy Tomato",
            PlantConditionData.conditions["Healthy Tomato"]!!)
        addConditionToLayout(binding.conditionsContainer, "Healthy Eggplant",
            PlantConditionData.conditions["Healthy eggplant"]!!)
        addConditionToLayout(binding.conditionsContainer, "Healthy Okra",
            PlantConditionData.conditions["Healthy okra"]!!)
        addConditionToLayout(binding.conditionsContainer, "Healthy Bitter Gourd",
            PlantConditionData.conditions["Healthy bitter gourd"]!!)
        addConditionToLayout(binding.conditionsContainer, "Healthy Chili Pepper",
            PlantConditionData.conditions["Healthy Chili Pepper"]!!)
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

        // Early signs components
        val earlySignsSection = conditionView.findViewById<LinearLayout>(R.id.earlySignsSection)
        val earlySignsTextView = conditionView.findViewById<TextView>(R.id.earlySignsText)
        val earlySignsImageView = conditionView.findViewById<ImageView>(R.id.earlySignsImage)

        nameTextView.text = name
        descriptionTextView.text = condition.description

        // Load condition image using the specific mapping
        val imageView = conditionView.findViewById<ImageView>(R.id.conditionImage)
        val imageResourceId = getConditionImageResource(name)
        imageView.setImageResource(imageResourceId)

        // Set early signs section visibility and content for disease conditions
        if (!name.contains("Healthy", ignoreCase = true) && !name.contains("Lady Bug",  ignoreCase = true)
            && !name.contains("Aphids",  ignoreCase = true)&& !name.contains("Fruit Fly",  ignoreCase = true) && !name.contains("Cutworm",  ignoreCase = true)) {

            earlySignsSection.visibility = android.view.View.VISIBLE

            // Set early signs text
            val earlySigns = getEarlySignsText(name)
            earlySignsTextView.text = "Early Signs: $earlySigns"

            // Set early signs image
            earlySignsImageView.setImageResource(getEarlySignsImageResource(name))
        } else {
            // Hide early signs section for healthy plants and beneficial insects
            earlySignsSection.visibility = android.view.View.GONE
        }

        container.addView(conditionView)
    }

    private fun getConditionImageResource(conditionName: String): Int {
        return when {
            conditionName.contains("Blossom End Rot", ignoreCase = true) -> R.drawable.blossom
            conditionName.contains("Melon Thrips", ignoreCase = true) -> R.drawable.melon
            conditionName.contains("Blossom Blight", ignoreCase = true) -> R.drawable.siraokra
            conditionName.contains("Phytophthora", ignoreCase = true) -> R.drawable.sakitampalaya
            conditionName.contains("Anthracnose", ignoreCase = true) -> R.drawable.silisira
            conditionName.contains("Hippodamia", ignoreCase = true) -> R.drawable.hippodamia
            conditionName.contains("Aphids", ignoreCase = true) -> R.drawable.aphids
            conditionName.contains("Fruit Fly", ignoreCase = true) -> R.drawable.fruitfly
            conditionName.contains("Cutworm", ignoreCase = true) -> R.drawable.cutworm
            conditionName.contains("Healthy Tomato", ignoreCase = true) -> R.drawable.htomato
            conditionName.contains("Healthy Eggplant", ignoreCase = true) -> R.drawable.heggplant
            conditionName.contains("Healthy Okra", ignoreCase = true) -> R.drawable.hokra
            conditionName.contains("Healthy Bitter Gourd", ignoreCase = true) -> R.drawable.hamplaya
            conditionName.contains("Healthy Chili Pepper", ignoreCase = true) -> R.drawable.hsili

            else -> R.drawable.ic_plant_care
        }
    }

    private fun getEarlySignsImageResource(conditionName: String): Int {
        return when {
            conditionName.contains("Blossom End Rot", ignoreCase = true) -> R.drawable.signtom
            conditionName.contains("Melon Thrips", ignoreCase = true) -> R.drawable.signtalong
            conditionName.contains("Blossom Blight", ignoreCase = true) -> R.drawable.signokra
            conditionName.contains("Phytophthora", ignoreCase = true) -> R.drawable.signampalaya
            conditionName.contains("Anthracnose", ignoreCase = true) -> R.drawable.signsili
//            conditionName.contains("Aphids", ignoreCase = true) -> R.drawable.melon
//            conditionName.contains("Fruit Fly", ignoreCase = true) -> R.drawable.melon
//            conditionName.contains("Cutworm", ignoreCase = true) -> R.drawable.melon
            else -> R.drawable.ic_plant_care
        }
    }

    private fun getEarlySignsText(conditionName: String): String {
        return when {
            conditionName.contains("Blossom End Rot", ignoreCase = true) ->
                "Small, water-soaked spots on the blossom end of fruits that gradually enlarge and darken."

            conditionName.contains("Melon Thrips", ignoreCase = true) ->
                "Silvery streaking or stippling on leaves, distorted leaf growth, and black fecal spots."

            conditionName.contains("Blossom Blight", ignoreCase = true) ->
                "Brown spots on flower petals, wilting of flowers, and failure of fruits to develop."

            conditionName.contains("Phytophthora", ignoreCase = true) ->
                "Water-soaked patches on fruits that develop white fuzzy growth, usually starting at the soil contact point."

            conditionName.contains("Anthracnose", ignoreCase = true) ->
                "Small, circular sunken spots on fruits that enlarge and develop pink spore masses in the center."

//            conditionName.contains("Aphids", ignoreCase = true) ->
//                "Clusters of tiny insects on new growth, curling of leaves, and sticky honeydew on plant surfaces."
//
//            conditionName.contains("Fruit Fly", ignoreCase = true) ->
//                "Tiny puncture marks on fruit skin, premature fruit softening, and fruit deformation."
//
//            conditionName.contains("Cutworm", ignoreCase = true) ->
//                "Young seedlings cut off at soil level, feeding damage on leaves near soil, and visible soil disturbance."

            else -> "No early signs information available."
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