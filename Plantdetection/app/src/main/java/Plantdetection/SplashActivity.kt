package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.PlantDetection.R

// Splash Screen Activity with Logo
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Logo and start button are in the layout
        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
}
