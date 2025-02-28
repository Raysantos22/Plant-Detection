package Plantdetection

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
            plantName.text = plant.name
            
            // Set plant icon based on type
            val iconRes = when (plant.type.lowercase()) {
                "tomato" -> R.drawable.tomato
                "eggplant" -> R.drawable.eggplant
                else -> R.drawable.vegetable_logo
            }
            plantIcon.setImageResource(iconRes)
            
            // Set plant status
            plantStatus.text = plant.currentCondition ?: "Healthy"
            val statusColor = if (plant.currentCondition == null || plant.currentCondition?.startsWith("Healthy") == true) {
                R.color.app_dark_green
            } else {
                R.color.orange
            }
            plantStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))
            
            // Set selected state
            if (isSelected) {
                plantCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.app_green))
            } else {
                plantCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            }
        }
    }
}