package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.PlantDetection.R

class LoadingActivity : AppCompatActivity() {
    private var selectedVegetable: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Get selected vegetable
        selectedVegetable = intent.getStringExtra("SELECTED_VEGETABLE")

        // Display appropriate message
        val noteText = findViewById<TextView>(R.id.noteText)
        noteText.text = "NOTE: MAKE SURE YOU CHOSE THE RIGHT VEGETABLE TO MONITOR. RESULT WILL BE ERROR IF YOU CHOSE WRONG VEGETABLE."

        // Loading text
        val loadingText = findViewById<TextView>(R.id.loadingText)
        loadingText.text = "LOADING....."

        // Find the start scanning button
        val startScanningButton = findViewById<Button>(R.id.startScanningButton)
        startScanningButton.visibility = View.GONE // Hide button initially

        // Simulate loading delay
        android.os.Handler(mainLooper).postDelayed({
            // Hide loading text and show start button
            loadingText.visibility = View.GONE
            startScanningButton.visibility = View.VISIBLE
        }, 2000) // 2 seconds delay

        // Set click listener for the start scanning button
        startScanningButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SELECTED_VEGETABLE", selectedVegetable)
            startActivity(intent)
            finish()
        }
    }
}