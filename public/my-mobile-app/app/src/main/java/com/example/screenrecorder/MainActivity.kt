package com.example.screenrecorder

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var projectionManager: MediaProjectionManager

    private val captureIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(serviceIntent)
            finish() // Minimize app like iOS
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsRepository = SettingsRepository(this)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val txtSummary = findViewById<TextView>(R.id.txt_summary)
        txtSummary.text = "Last used: " + settingsRepository.getSummary()

        findViewById<Button>(R.id.btn_record).setOnClickListener {
            // Check if already recording...
            if (RecordingManager.isRecording) {
                stopRecording()
            } else {
                startRecordingFlow()
            }
        }

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.txt_summary).text = "Last used: " + settingsRepository.getSummary()
        updateRecordUI()
    }

    private fun updateRecordUI() {
        val btn = findViewById<Button>(R.id.btn_record)
        if (RecordingManager.isRecording) {
            btn.text = "STOP RECORDING"
            // Set red pulsing shape here
        } else {
            btn.text = "START RECORDING"
            // Set standard iOS style gray/red circle
        }
    }

    private fun startRecordingFlow() {
        // Request MediaProjection token
        val intent = projectionManager.createScreenCaptureIntent()
        captureIntentLauncher.launch(intent)
    }

    private fun stopRecording() {
        val serviceIntent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        startService(serviceIntent)
        updateRecordUI()
    }
}
