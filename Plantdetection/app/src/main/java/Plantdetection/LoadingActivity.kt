package com.PlantDetection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class LoadingActivity : AppCompatActivity() {
    private var selectedVegetable: String? = null
    private lateinit var loadingText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startScanningButton: Button
    private lateinit var retryButton: Button

    // These will be set after download
    private var modelPath: String? = null
    private var labelsPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        selectedVegetable = intent.getStringExtra("SELECTED_VEGETABLE")

        initViews()
        startModelDownload()
    }

    private fun initViews() {
        val noteText = findViewById<TextView>(R.id.noteText)
        noteText.text = "NOTE: MAKE SURE YOU CHOSE THE RIGHT VEGETABLE TO MONITOR. RESULT WILL BE ERROR IF YOU CHOSE WRONG VEGETABLE."

        loadingText = findViewById<TextView>(R.id.loadingText)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)
        startScanningButton = findViewById<Button>(R.id.startScanningButton)
        retryButton = findViewById<Button>(R.id.retryButton)

        startScanningButton.visibility = View.GONE
        retryButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        progressBar.max = 100

        // Set up retry button
        retryButton.setOnClickListener {
            retryButton.visibility = View.GONE
            startModelDownload()
        }

        // Set up start scanning button
        startScanningButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SELECTED_VEGETABLE", selectedVegetable)
            // No need to pass file paths as MainActivity will find them in internal storage
            startActivity(intent)
            finish()
        }
    }

    private fun debugFileInfo() {
        val modelFile = File(filesDir, "yolov11v8.tflite")
        val labelsFile = File(filesDir, "labels1.txt")

        Log.d("LoadingActivity", "=== FILE DEBUG INFO ===")
        Log.d("LoadingActivity", "Model file exists: ${modelFile.exists()}")
        Log.d("LoadingActivity", "Model file size: ${if (modelFile.exists()) modelFile.length() else "N/A"} bytes")
        Log.d("LoadingActivity", "Model file path: ${modelFile.absolutePath}")
        Log.d("LoadingActivity", "Labels file exists: ${labelsFile.exists()}")
        Log.d("LoadingActivity", "Labels file size: ${if (labelsFile.exists()) labelsFile.length() else "N/A"} bytes")
        Log.d("LoadingActivity", "Labels file path: ${labelsFile.absolutePath}")
        Log.d("LoadingActivity", "========================")
    }

    private fun startModelDownload() {
        loadingText.text = "Preparing to download model files..."
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        lifecycleScope.launch {
            SupabaseModelDownloader.downloadModelFiles(
                context = this@LoadingActivity,
                onProgress = { progress, message ->
                    runOnUiThread {
                        progressBar.progress = progress
                        loadingText.text = message
                    }
                },
                onComplete = { modelFilePath, labelsFilePath ->
                    runOnUiThread {
                        this@LoadingActivity.modelPath = modelFilePath
                        this@LoadingActivity.labelsPath = labelsFilePath

                        // Debug file information
                        debugFileInfo()

                        loadingText.text = "Files ready! You can start scanning."
                        progressBar.visibility = View.GONE
                        startScanningButton.visibility = View.VISIBLE

                        Log.d("LoadingActivity", "Download completed successfully")
                        Log.d("LoadingActivity", "Model path: $modelFilePath")
                        Log.d("LoadingActivity", "Labels path: $labelsFilePath")
                    }
                },
                onError = { exception ->
                    runOnUiThread {
                        loadingText.text = "Download failed: ${exception.message}"
                        progressBar.visibility = View.GONE
                        retryButton.visibility = View.VISIBLE

                        Log.e("LoadingActivity", "Download error: ${exception.message}", exception)
                    }
                }
            )
        }
    }
}