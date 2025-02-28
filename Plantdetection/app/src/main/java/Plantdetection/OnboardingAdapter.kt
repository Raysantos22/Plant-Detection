package com.PlantDetection

import android.view.View
import android.widget.TextView
import com.PlantDetection.R


class OnboardingAdapter(
    private val texts: List<String>
) : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(texts[position])
    }

    override fun getItemCount(): Int = texts.size

    inner class OnboardingViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.onboardingText)

        fun bind(text: String) {
            textView.text = text
        }
    }
}