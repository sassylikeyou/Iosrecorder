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

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import android.widget.TableRow
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var projectionManager: MediaProjectionManager

    private val PERMISSION_REQ_CODE = 1001

    private val captureIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(serviceIntent)
            moveTaskToBack(true) // Minimize app like iOS
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsRepository = SettingsRepository(this)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        updateSummary()

        findViewById<android.widget.FrameLayout>(R.id.btn_record_container).setOnClickListener {
            checkPermissionsAndStart()
        }

        setupPreferenceClicks()
    }

    private fun setupPreferenceClicks() {
        findViewById<android.widget.LinearLayout>(R.id.row_resolution).setOnClickListener {
            val options = arrayOf("720p", "1080p", "1440p")
            val values = intArrayOf(720, 1080, 1440)
            val currentIndex = values.indexOf(settingsRepository.resolutionHeight).takeIf { it >= 0 } ?: 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.resolutionHeight = values[which]
                updateSummary()
                dialog.dismiss()
            }.show()
        }

        findViewById<android.widget.LinearLayout>(R.id.row_fps).setOnClickListener {
            val options = arrayOf("30fps", "60fps", "90fps")
            val values = intArrayOf(30, 60, 90)
            val currentIndex = values.indexOf(settingsRepository.frameRate).takeIf { it >= 0 } ?: 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.frameRate = values[which]
                updateSummary()
                dialog.dismiss()
            }.show()
        }

        findViewById<android.widget.LinearLayout>(R.id.row_bitrate).setOnClickListener {
            val options = arrayOf("5 Mbps", "8 Mbps", "12 Mbps", "16 Mbps")
            val values = intArrayOf(5000000, 8000000, 12000000, 16000000)
            val currentIndex = values.indexOf(settingsRepository.videoBitrate).takeIf { it >= 0 } ?: 2
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.videoBitrate = values[which]
                updateSummary()
                dialog.dismiss()
            }.show()
        }

        findViewById<android.widget.LinearLayout>(R.id.row_orientation).setOnClickListener {
            val options = arrayOf("Auto", "Portrait", "Landscape")
            val currentIndex = settingsRepository.orientationMode
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.orientationMode = which
                updateSummary()
                dialog.dismiss()
            }.show()
        }

        findViewById<android.widget.LinearLayout>(R.id.row_advanced).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateSummary() {
        val txtSummary = findViewById<TextView>(R.id.txt_summary)
        txtSummary.text = settingsRepository.getSummary()

        findViewById<TextView>(R.id.val_resolution).text = "${settingsRepository.resolutionHeight}p"
        findViewById<TextView>(R.id.val_fps).text = "${settingsRepository.frameRate}fps"
        findViewById<TextView>(R.id.val_bitrate).text = "${settingsRepository.videoBitrate / 1000000} Mbps"
        findViewById<TextView>(R.id.val_orientation).text = arrayOf("Auto", "Portrait", "Landscape")[settingsRepository.orientationMode]
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        if (settingsRepository.audioSourceMode != 0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQ_CODE)
        } else {
            proceedRecording()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_CODE) {
            proceedRecording()
        }
    }

    private fun proceedRecording() {
        if (RecordingManager.isRecording) {
            stopRecording()
        } else {
            startRecordingFlow()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSummary()
        updateRecordUI()
    }

    private fun updateRecordUI() {
        val innerView = findViewById<android.view.View>(R.id.btn_record_inner)
        val txtStatus = findViewById<TextView>(R.id.txt_status)
        if (RecordingManager.isRecording) {
            txtStatus.text = "RECORDING"
            txtStatus.setTextColor(ContextCompat.getColor(this, R.color.red_record))
            // Make the inner square to indicate stop
            val shape = android.graphics.drawable.GradientDrawable()
            shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            shape.cornerRadius = 16f
            shape.setColor(ContextCompat.getColor(this, R.color.red_record))
            innerView.background = shape
            
            val params = innerView.layoutParams as android.widget.FrameLayout.LayoutParams
            params.width = 64
            params.height = 64
            innerView.layoutParams = params
        } else {
            txtStatus.text = "READY TO CAPTURE"
            txtStatus.setTextColor(ContextCompat.getColor(this, R.color.gray_light))
            // Make inner circle
            val shape = android.graphics.drawable.GradientDrawable()
            shape.shape = android.graphics.drawable.GradientDrawable.OVAL
            shape.setColor(ContextCompat.getColor(this, R.color.red_record))
            innerView.background = shape
            
            val params = innerView.layoutParams as android.widget.FrameLayout.LayoutParams
            val size = (140 * resources.displayMetrics.density).toInt()
            params.width = size
            params.height = size
            innerView.layoutParams = params
        }
    }

    private fun startRecordingFlow() {
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
