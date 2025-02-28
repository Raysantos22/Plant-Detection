package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.PlantDetection.MainActivity
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

        // Simulate loading delay and then start main camera activity
        findViewById<TextView>(R.id.loadingText).text = "LOADING....."

        android.os.Handler(mainLooper).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SELECTED_VEGETABLE", selectedVegetable)
            startActivity(intent)
            finish()
        }, 2000) // 2 seconds delay
    }
}
