package com.PlantDetection

import Plantdetection.PlantConditionData
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


/**
 * Activity for displaying detailed information about a detected plant condition
 * and managing monitoring tasks for the condition
 */
class ConditionDetailActivity : AppCompatActivity() {

    private lateinit var monitoringManager: PlantMonitoringManager

    private var detectionId: String? = null
    private var conditionName: String? = null
    private var vegetableType: String? = null

    private lateinit var conditionTitle: TextView
    private lateinit var conditionDescription: TextView
    private lateinit var statusText: TextView
    private lateinit var conditionImage: ImageView
    private lateinit var preventionContainer: LinearLayout
    private lateinit var treatmentContainer: LinearLayout
    private lateinit var setReminderButton: Button
    private lateinit var taskCompleteButton: Button
    private lateinit var nextCheckText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_condition_detail)

        // Initialize monitoring manager
        monitoringManager = PlantMonitoringManager(this)

        // Get data from intent
        detectionId = intent.getStringExtra(PlantMonitoringManager.EXTRA_DETECTION_ID)
        conditionName = intent.getStringExtra(PlantMonitoringManager.EXTRA_CONDITION_NAME)
        vegetableType = intent.getStringExtra(PlantMonitoringManager.EXTRA_VEGETABLE_TYPE)

        if (detectionId == null || conditionName == null || vegetableType == null) {
            Toast.makeText(this, "Error: Missing condition data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initViews()

        // Display condition data
        displayConditionData()

        // Set up listeners
        setupListeners()
    }

    private fun initViews() {
        conditionTitle = findViewById(R.id.conditionTitle)
        conditionDescription = findViewById(R.id.conditionDescription)
        statusText = findViewById(R.id.statusText)
        conditionImage = findViewById(R.id.conditionImage)
        preventionContainer = findViewById(R.id.preventionContainer)
        treatmentContainer = findViewById(R.id.treatmentContainer)
        setReminderButton = findViewById(R.id.setReminderButton)
        taskCompleteButton = findViewById(R.id.taskCompleteButton)
        nextCheckText = findViewById(R.id.nextCheckText)

        // Setup back button
        findViewById<View>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun displayConditionData() {
        // Get condition data
        val condition = PlantConditionData.conditions[conditionName] ?: return

        // Set condition details
        conditionTitle.text = condition.name
        conditionDescription.text = condition.description

        // Set appropriate status
        val isHealthy = conditionName?.startsWith("Healthy") == true
        statusText.text = if (isHealthy) "Healthy" else "Needs Treatment"
        statusText.setTextColor(getColor(if (isHealthy) R.color.app_dark_green else R.color.orange))

        // Set condition image (based on condition name)
        val conditionImageResId = when {
            conditionName?.contains("Tomato") == true -> R.drawable.tomato
            conditionName?.contains("Eggplant") == true -> R.drawable.eggplant
            else -> {
                // Default image or specific condition images could be added here
                when (conditionName) {
                    "Anthracnose" -> R.drawable.tomato // Replace with actual disease images
                    "Blossom End Rot" -> R.drawable.tomato
                    "Leaf Caterpillar" -> R.drawable.tomato
                    "Leaf Roller" -> R.drawable.tomato
                    "Melon thrips" -> R.drawable.tomato
                    "White Fly" -> R.drawable.tomato
                    else -> R.drawable.tomato
                }
            }
        }
        conditionImage.setImageResource(conditionImageResId)

        // Display prevention tips
        preventionContainer.removeAllViews()
        condition.preventionTips.forEach { tip ->
            val tipView = LayoutInflater.from(this)
                .inflate(R.layout.item_prevention_tip, preventionContainer, false)
            tipView.findViewById<TextView>(R.id.tipText).text = tip
            preventionContainer.addView(tipView)
        }

        // Display treatment tips
        treatmentContainer.removeAllViews()
        condition.treatmentTips.forEach { tip ->
            val tipView = LayoutInflater.from(this)
                .inflate(R.layout.item_treatment_tip, treatmentContainer, false)
            tipView.findViewById<TextView>(R.id.tipText).text = tip
            treatmentContainer.addView(tipView)
        }

        // Check if the condition is already resolved
        val sharedPrefs = getSharedPreferences(PlantMonitoringManager.COMPANION_OBJECT_NAME, Context.MODE_PRIVATE)
        val isResolved = sharedPrefs.getBoolean("${detectionId}_resolved", false)

        if (isResolved) {
            statusText.text = "Resolved"
            setReminderButton.isEnabled = false
            taskCompleteButton.isEnabled = false
        }

        // Get next scheduled check time and display it
        val nextCheckTime = sharedPrefs.getLong("${detectionId}_next_check", 0)
        if (nextCheckTime > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            nextCheckText.text = "Next check: ${dateFormat.format(nextCheckTime)}"
        }
    }

    private fun setupListeners() {
        // Set reminder button
        setReminderButton.setOnClickListener {
            showDateTimePicker()
        }

        // Task complete button
        taskCompleteButton.setOnClickListener {
            completeTask()
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        // Date picker
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Time picker
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Set the reminder
                        setReminder(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun setReminder(reminderTimeMillis: Long) {
        detectionId?.let { id ->
            conditionName?.let { condition ->
                vegetableType?.let { vegetable ->
                    // Set custom reminder in preferences
                    monitoringManager.setCustomReminder(id, reminderTimeMillis)

                    // Schedule notification
                    monitoringManager.scheduleCheckNotification(id, condition, vegetable)

                    // Update UI to show next check time
                    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    nextCheckText.text = "Next check: ${dateFormat.format(reminderTimeMillis)}"

                    Toast.makeText(
                        this,
                        "Reminder set for ${dateFormat.format(reminderTimeMillis)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun completeTask() {
        detectionId?.let { id ->
            conditionName?.let { condition ->
                // Mark the task as complete
                monitoringManager.markTaskComplete(id, condition)

                Toast.makeText(this, "Task marked as complete", Toast.LENGTH_SHORT).show()

                // If this was a disease/pest condition, ask if it's resolved
                if (condition.startsWith("Healthy").not()) {
                    showResolutionConfirmationDialog()
                } else {
                    // Update next check time display
                    val sharedPrefs = getSharedPreferences(PlantMonitoringManager.COMPANION_OBJECT_NAME, Context.MODE_PRIVATE)
                    val nextCheckTime = sharedPrefs.getLong("${id}_next_check", 0)
                    if (nextCheckTime > 0) {
                        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        nextCheckText.text = "Next check: ${dateFormat.format(nextCheckTime)}"
                    }
                }
            }
        }
    }

    private fun showResolutionConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Issue Resolved?")
            .setMessage("Has the plant condition been resolved?")
            .setPositiveButton("Yes") { dialog, _ ->
                markConditionResolved()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                Toast.makeText(this, "Continue following the treatment plan.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun markConditionResolved() {
        detectionId?.let { id ->
            // Mark as resolved in the monitoring manager
            monitoringManager.markConditionResolved(id)

            // Update UI
            statusText.text = "Resolved"
            statusText.setTextColor(getColor(R.color.app_dark_green))
            setReminderButton.isEnabled = false
            taskCompleteButton.isEnabled = false

            Toast.makeText(this, "Great! Plant condition marked as resolved.", Toast.LENGTH_SHORT).show()
        }
    }
}