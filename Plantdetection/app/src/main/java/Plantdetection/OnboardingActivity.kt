package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.PlantDetection.VegetableSelectionActivity
import com.PlantDetection.databinding.ActivityOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingAdapter

    private val onboardingTexts = listOf(
        "NOTE: THIS MONITORING SYSTEM ONLY MONITORING THE VEGETABLE, SOIL,LEAVES, STEM AND ENVIRONMENT IS NOT PART OF THE SCOPE OF THIS SYSTEM...",
        "SELECT THE PROPER CROP ON SELECTION. SO THE SYSTEM WILL NOT BE CONFUSED...",
        "FOLLOW THE DEVICE MINIMUM REQUIREMENTS TO GET THE BETTER RESULT...."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOnboarding()
        setupListeners()
    }

    private fun setupOnboarding() {
        onboardingAdapter = OnboardingAdapter(onboardingTexts)
        binding.viewPager.adapter = onboardingAdapter

        // Setup dot indicators
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, _ ->
            tab.view.isClickable = false
        }.attach()
    }

    private fun setupListeners() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.skipButton.visibility = if (position == onboardingTexts.size - 1) View.GONE else View.VISIBLE
                binding.nextButton.text = if (position == onboardingTexts.size - 1) "Start" else "Next"
            }
        })

        binding.skipButton.setOnClickListener {
            navigateToVegetableSelection()
        }

        binding.nextButton.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            if (currentPosition < onboardingTexts.size - 1) {
                binding.viewPager.currentItem = currentPosition + 1
            } else {
                navigateToVegetableSelection()
            }
        }
    }

    private fun navigateToVegetableSelection() {
        startActivity(Intent(this, VegetableSelectionActivity::class.java))
        finish()
    }
}
