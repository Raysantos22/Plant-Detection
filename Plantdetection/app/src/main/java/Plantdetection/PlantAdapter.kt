package com.PlantDetection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.PlantDetection.R

/**
 * Adapter for displaying a list of plants in a RecyclerView
 */
class PlantAdapter(
    private var plants: List<PlantDatabaseManager.Plant>,
    private val onItemClick: (PlantDatabaseManager.Plant) -> Unit,
    private val onItemLongClick: (PlantDatabaseManager.Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private var selectedPlantId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.bind(plant, selectedPlantId == plant.id)
    }

    override fun getItemCount(): Int = plants.size

    fun updatePlants(newPlants: List<PlantDatabaseManager.Plant>) {
        plants = newPlants
        notifyDataSetChanged()
    }

    fun updateSelectedPlantId(plantId: String?) {
        selectedPlantId = plantId
        notifyDataSetChanged()
    }

    inner class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantCard: CardView = itemView.findViewById(R.id.plantCard)
        private val plantIcon: ImageView = itemView.findViewById(R.id.plantIcon)
        private val plantName: TextView = itemView.findViewById(R.id.plantName)
        private val plantStatus: TextView = itemView.findViewById(R.id.plantStatus)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(plants[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(plants[position])
                    return@setOnLongClickListener true
                }
                false
            }
        }

        fun bind(plant: PlantDatabaseManager.Plant, isSelected: Boolean) {
            // Check if this is a plant group by looking for patterns like "(3 plants)" in the name
            val isPlantGroup = plant.name.contains("(") && plant.name.contains("plants")

            // Set plant name - keep it as is
            plantName.text = plant.name

            // Set plant icon based on type
            val iconRes = when (plant.type.lowercase()) {
                "tomato" -> R.drawable.tomato
                "eggplant" -> R.drawable.eggplant
                "okra" -> R.drawable.okra
                "bitter gourd" -> R.drawable.bitter_gourd
                "chili pepper" -> R.drawable.chili_pepper
                else -> R.drawable.vegetable_logo
            }
            plantIcon.setImageResource(iconRes)

            if (isPlantGroup) {
                // Parse plant group details from notes
                val conditionCounts = mutableMapOf<String, Int>()
                val notesLines = plant.notes.split("\n")

                // Extract condition counts from notes lines like "- Condition: X plants"
                for (line in notesLines) {
                    if (line.trim().startsWith("-") && line.contains(":") && line.contains("plants")) {
                        val conditionPart = line.substringAfter("-").substringBefore(":")
                        val countPart = line.substringAfter(":").trim()
                        val count = countPart.substringBefore(" ").toIntOrNull() ?: 0

                        if (count > 0) {
                            conditionCounts[conditionPart.trim()] = count
                        }
                    }
                }

                // Separate healthy and diseased conditions
                val healthyConditions = conditionCounts.filter {
                    it.key.contains("Healthy", ignoreCase = true) ||
                            it.key.contains("Lady Bug", ignoreCase = true) ||
                            it.key.contains("Hippodamia", ignoreCase = true)
                }
                val diseaseConditions = conditionCounts.filter {
                    !it.key.contains("Healthy", ignoreCase = true) &&
                            !it.key.contains("Lady Bug", ignoreCase = true) &&
                            !it.key.contains("Hippodamia", ignoreCase = true)
                }

                // Create a concise status string showing counts for each category
                val healthyCount = healthyConditions.values.sum()
                val diseaseCount = diseaseConditions.values.sum()

                val statusText = StringBuilder()
                if (healthyCount > 0) {
                    statusText.append("$healthyCount healthy")
                }

                if (diseaseCount > 0) {
                    if (statusText.isNotEmpty()) statusText.append(", ")
                    statusText.append("$diseaseCount diseased")
                }

                plantStatus.text = statusText.toString()

                // Set status color based on if any diseased plants
                val statusColor = if (diseaseCount > 0) {
                    R.color.orange
                } else {
                    R.color.app_dark_green
                }
                plantStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))
            } else {
                // Regular single plant
                // Update to account for new conditions in the dataset
                val condition = plant.currentCondition

                // Check if the condition is beneficial (like lady bugs)
                val isBeneficial = condition?.contains("Lady Bug", ignoreCase = true) == true ||
                        condition?.contains("Hippodamia", ignoreCase = true) == true

                // Check if condition refers to known infestations
                val isPest = condition?.contains("(Infested)", ignoreCase = true) == true ||
                        condition?.contains("Aphids", ignoreCase = true) == true ||
                        condition?.contains("Cutworm", ignoreCase = true) == true ||
                        condition?.contains("Fruit Fly", ignoreCase = true) == true

                // Check if condition refers to disease
                val isDisease = condition?.contains("(Diseased)", ignoreCase = true) == true ||
                        condition?.contains("Blossom End Rot", ignoreCase = true) == true ||
                        condition?.contains("Anthracnose", ignoreCase = true) == true ||
                        condition?.contains("Phytophthora", ignoreCase = true) == true ||
                        condition?.contains("Blossom Blight", ignoreCase = true) == true ||
                        condition?.contains("Melon Thrips", ignoreCase = true) == true

                plantStatus.text = when {
                    condition == null || condition.contains("Healthy", ignoreCase = true) -> "Healthy"
                    isBeneficial -> "Beneficial Insects"
                    isPest -> "Pest Infestation"
                    isDisease -> "Disease Detected"
                    else -> "Unknown Condition"
                }

                val statusColor = when {
                    condition == null || condition.contains("Healthy", ignoreCase = true) -> R.color.app_dark_green
                    isBeneficial -> R.color.app_dark_green // Beneficial insects are a positive thing
                    else -> R.color.orange
                }

                plantStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))
            }

            // Set selected state
            if (isSelected) {
                plantCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.app_green))
            } else {
                plantCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            }
        }
    }
}