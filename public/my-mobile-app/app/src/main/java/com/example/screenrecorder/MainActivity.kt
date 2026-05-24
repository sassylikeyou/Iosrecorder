package com.example.screenrecorder

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator

class MainActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var projectionManager: MediaProjectionManager

    private val PERMISSION_REQ_CODE = 1001

    private lateinit var container: FrameLayout
    private lateinit var tabRecord: LinearLayout
    private lateinit var tabLibrary: LinearLayout
    private lateinit var tabSettings: LinearLayout

    private lateinit var viewRecord: View
    private lateinit var viewLibrary: View
    private lateinit var viewSettings: View

    private var currentTabIndex = -1
    private var pulseAnimator: ObjectAnimator? = null
    private var btnAnimator: ObjectAnimator? = null
    private var bgAnimator: ValueAnimator? = null
    private var rippleAnimator: ObjectAnimator? = null

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isCountingDown = false
    private val liveTimerRunnable = object : Runnable {
        override fun run() {
            if (RecordingManager.isRecording) {
                val duration = System.currentTimeMillis() - RecordingManager.recordingStartTime
                val seconds = (duration / 1000).toInt() % 60
                val minutes = ((duration / (1000 * 60)) % 60).toInt()
                val hours = (duration / (1000 * 60 * 60)).toInt()
                val timeString = if (hours > 0) {
                    String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
                }
                viewRecord.findViewById<TextView>(R.id.txt_status)?.text = "Recording in progress... $timeString"
                handler.postDelayed(this, 1000)
            }
        }
    }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentTabIndex", currentTabIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        
        val mode = when (settingsRepository.themeMode) {
            1 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            2 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
        
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        container = findViewById(R.id.fragment_container)
        tabRecord = findViewById(R.id.tab_record)
        tabLibrary = findViewById(R.id.tab_library)
        tabSettings = findViewById(R.id.tab_settings)

        viewRecord = layoutInflater.inflate(R.layout.fragment_record, container, false)
        viewLibrary = layoutInflater.inflate(R.layout.fragment_library, container, false)
        viewSettings = layoutInflater.inflate(R.layout.fragment_settings, container, false)

        container.addView(viewRecord)
        container.addView(viewLibrary)
        container.addView(viewSettings)

        tabRecord.setOnClickListener { switchTab(0) }
        tabLibrary.setOnClickListener { switchTab(1) }
        tabSettings.setOnClickListener { switchTab(2) }

        setupRecordView()
        setupSettingsView()
        setupLibraryView()

        startBackgroundAnimation()

        val initialTab = savedInstanceState?.getInt("currentTabIndex", 0) ?: 0
        switchTab(initialTab)
    }

    private fun switchTab(index: Int) {
        if (currentTabIndex == index) return
        val isForward = index > currentTabIndex
        val previousIndex = currentTabIndex
        currentTabIndex = index
        
        val targetView = when (index) {
            0 -> viewRecord
            1 -> viewLibrary
            else -> viewSettings
        }

        listOf(viewRecord, viewLibrary, viewSettings).forEach { view ->
            if (view == targetView) {
                if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    view.alpha = 0f
                    view.translationX = if (isForward) 150f else -150f
                    view.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
            } else {
                if (view.visibility == View.VISIBLE) {
                    view.animate()
                        .alpha(0f)
                        .translationX(if (isForward) -150f else 150f)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.AccelerateInterpolator())
                        .withEndAction { view.visibility = View.GONE }
                        .start()
                }
            }
        }

        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val navBar = findViewById<LinearLayout>(R.id.tab_bar)
        // Set an elevation depending on theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            navBar.elevation = if (isNightMode) 0f else 24f
            val divider = findViewById<View>(android.R.id.custom) // using custom ID because R.id isn't dynamically registered without xml.
            if (divider == null) {
                val newDivider = View(this).apply {
                    id = android.R.id.custom
                    layoutParams = android.view.ViewGroup.MarginLayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
                        val lp = navBar.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                        if (lp != null) {
                            // Can't reliably insert constraint layout divider linearly inside constraint layout without matching constraints
                        }
                    }
                    setBackgroundColor(if (isNightMode) Color.parseColor("#1C1C1E") else Color.parseColor("#F3F4F6"))
                }
                // Skip injecting programmatically if constraint layout is too complex, let's just make the navBar have a top border by itself.
                navBar.setBackgroundColor(if (isNightMode) Color.parseColor("#0A0A14") else Color.parseColor("#FFFFFF"))
            }
        }
        
        val colorActive = Color.parseColor("#8B5CF6")
        val colorInactive = if (isNightMode) Color.parseColor("#8E8E93") else Color.parseColor("#6B7280")
        
        if (isNightMode) {
            navBar.setBackgroundColor(Color.parseColor("#0A0A14"))
            container.setBackgroundColor(Color.parseColor("#0A0A14"))
        } else {
            navBar.setBackgroundColor(Color.parseColor("#FFFFFF"))
            container.setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val ivRecord = findViewById<ImageView>(R.id.icon_record)
        val ivLibrary = findViewById<ImageView>(R.id.icon_library)
        val ivSettings = findViewById<ImageView>(R.id.icon_settings)

        if (previousIndex != -1) {
            animateTabIcon(ivRecord, index == 0)
            animateTabIcon(ivLibrary, index == 1)
            animateTabIcon(ivSettings, index == 2)
        } else {
            ivRecord.scaleX = if (index == 0) 1.2f else 1.0f
            ivRecord.scaleY = if (index == 0) 1.2f else 1.0f
            ivLibrary.scaleX = if (index == 1) 1.2f else 1.0f
            ivLibrary.scaleY = if (index == 1) 1.2f else 1.0f
            ivSettings.scaleX = if (index == 2) 1.2f else 1.0f
            ivSettings.scaleY = if (index == 2) 1.2f else 1.0f
        }

        ivRecord.setColorFilter(if (index == 0) colorActive else colorInactive)
        findViewById<TextView>(R.id.text_record).setTextColor(if (index == 0) colorActive else colorInactive)
        findViewById<TextView>(R.id.text_record).typeface = if (index == 0) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        ivLibrary.setColorFilter(if (index == 1) colorActive else colorInactive)
        findViewById<TextView>(R.id.text_library).setTextColor(if (index == 1) colorActive else colorInactive)
        findViewById<TextView>(R.id.text_library).typeface = if (index == 1) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        ivSettings.setColorFilter(if (index == 2) colorActive else colorInactive)
        findViewById<TextView>(R.id.text_settings).setTextColor(if (index == 2) colorActive else colorInactive)
        findViewById<TextView>(R.id.text_settings).typeface = if (index == 2) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        if (index == 0) updateRecordUI()
        if (index == 1) loadLibrary()
        if (index == 2) updateSettingsUI()
    }

    private fun animateTabIcon(icon: ImageView, isSelected: Boolean) {
        val targetScale = if (isSelected) 1.2f else 1.0f
        icon.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    private fun startBackgroundAnimation() {
        if (bgAnimator == null) {
            val color1 = Color.parseColor("#CC150030") // Purpleish dark
            val color2 = Color.parseColor("#E6000000") // Blackish
            
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(color1, color2)
            )
            gradient.shape = GradientDrawable.RECTANGLE
            container.background = gradient

            bgAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
                duration = 10000
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    // Interpolate orientation or simply colors (hack: adjust center)
                    gradient.setGradientCenter(0.5f + fraction * 0.5f, 0.5f + fraction * 0.5f)
                }
            }
        }
        bgAnimator?.start()
    }

    private fun setupRecordView() {
        viewRecord.findViewById<FrameLayout>(R.id.btn_record).setOnClickListener {
            checkPermissionsAndStart()
        }
        val btnMic = viewRecord.findViewById<LinearLayout>(R.id.btn_mic)
        btnMic.setOnClickListener {
            settingsRepository.audioSourceMode = if (settingsRepository.audioSourceMode == 0) 1 else 0
            updateRecordUI()
        }
        
        startRippleAnimation()
    }

    private fun startRippleAnimation() {
        val ripple = viewRecord.findViewById<View>(R.id.rec_ripple)
        if (rippleAnimator == null) {
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.5f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.5f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f)
            rippleAnimator = ObjectAnimator.ofPropertyValuesHolder(ripple, scaleX, scaleY, alpha).apply {
                duration = 1200
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = android.view.animation.DecelerateInterpolator()
            }
        }
        rippleAnimator?.start()
    }

    private fun setupSettingsView() {
        viewSettings.findViewById<View>(R.id.row_theme)?.setOnClickListener {
            val options = arrayOf("System Default", "Light Theme", "Dark Theme")
            val values = intArrayOf(-1, 1, 2)
            val currentIndex = values.indexOf(settingsRepository.themeMode).takeIf { it >= 0 } ?: 0
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.themeMode = values[which]
                val mode = when (values[which]) {
                    1 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                    2 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                    else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
                updateSettingsUI()
                dialog.dismiss()
                recreate()
            }.show()
        }

        viewSettings.findViewById<View>(R.id.row_resolution).setOnClickListener {
            val options = arrayOf("720p HD", "1080p HD", "1440p HD")
            val values = intArrayOf(720, 1080, 1440)
            val currentIndex = values.indexOf(settingsRepository.resolutionHeight).takeIf { it >= 0 } ?: 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.resolutionHeight = values[which]
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }
        viewSettings.findViewById<View>(R.id.row_fps).setOnClickListener {
            val options = arrayOf("30 FPS", "60 FPS", "90 FPS", "120 FPS")
            val values = intArrayOf(30, 60, 90, 120)
            val currentIndex = values.indexOf(settingsRepository.frameRate).takeIf { it >= 0 } ?: 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.frameRate = values[which]
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }
        viewSettings.findViewById<View>(R.id.row_countdown)?.setOnClickListener {
            val options = arrayOf("0s (Off)", "3s", "5s")
            val values = intArrayOf(0, 3, 5)
            val currentIndex = values.indexOf(settingsRepository.countdownDuration).takeIf { it >= 0 } ?: 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.countdownDuration = values[which]
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }
        viewSettings.findViewById<View>(R.id.row_bitrate).setOnClickListener {
            val options = arrayOf("5 Mbps", "8 Mbps", "12 Mbps", "16 Mbps")
            val values = intArrayOf(5000000, 8000000, 12000000, 16000000)
            val currentIndex = values.indexOf(settingsRepository.videoBitrate).takeIf { it >= 0 } ?: 2
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.videoBitrate = values[which]
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }

        viewSettings.findViewById<View>(R.id.row_encoder)?.setOnClickListener {
            val options = arrayOf("H.264 / AVC", "H.265 / HEVC")
            val currentIndex = if (settingsRepository.videoEncoder == "H265") 1 else 0
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.videoEncoder = if (which == 1) "H265" else "H264"
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }

        viewSettings.findViewById<View>(R.id.row_max_file_size)?.setOnClickListener {
            val options = arrayOf("No Limit", "1 GB", "2 GB", "4 GB", "Custom Size")
            // default indices
            val limit = settingsRepository.maxFileSize
            val currentIndex = when (limit) {
                0L -> 0
                1024L * 1024L * 1024L -> 1
                2048L * 1024L * 1024L -> 2
                4096L * 1024L * 1024L -> 3
                else -> 4
            }
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                when (which) {
                    0 -> settingsRepository.maxFileSize = 0L
                    1 -> settingsRepository.maxFileSize = 1024L * 1024L * 1024L
                    2 -> settingsRepository.maxFileSize = 2048L * 1024L * 1024L
                    3 -> settingsRepository.maxFileSize = 4096L * 1024L * 1024L
                    4 -> {
                        val container = android.widget.LinearLayout(this).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            setPadding(50, 20, 50, 0)
                        }
                        val editText = android.widget.EditText(this).apply {
                            inputType = android.text.InputType.TYPE_CLASS_NUMBER
                            hint = "Size value"
                            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        val spinner = android.widget.Spinner(this).apply {
                            val adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("MB", "GB"))
                            this.adapter = adapter
                            layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                        }
                        container.addView(editText)
                        container.addView(spinner)
                        
                        AlertDialog.Builder(this)
                            .setTitle("Custom File Size Limit")
                            .setView(container)
                            .setPositiveButton("Set") { _, _ ->
                                val num = editText.text.toString().toLongOrNull()
                                if (num != null && num > 0) {
                                    val isGb = spinner.selectedItem.toString() == "GB"
                                    val multiplier = if (isGb) 1024L * 1024L * 1024L else 1024L * 1024L
                                    settingsRepository.maxFileSize = num * multiplier
                                    updateSettingsUI()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                        dialog.dismiss()
                        return@setSingleChoiceItems
                    }
                }
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }
        viewSettings.findViewById<View>(R.id.row_format).setOnClickListener {
            val options = arrayOf("MP4", "MKV")
            val currentIndex = if (settingsRepository.outputFormat == "MP4") 0 else 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.outputFormat = options[which]
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }

        viewSettings.findViewById<View>(R.id.row_export_code)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Export Codebase")
                .setMessage("To export the codebase as a ZIP file or to GitHub, please use the platform Settings menu (gear icon) located outside of the app preview.")
                .setPositiveButton("OK", null)
                .show()
        }

        viewSettings.findViewById<View>(R.id.row_reset_defaults)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset to Defaults")
                .setMessage("Are you sure you want to revert all settings to their default values?")
                .setPositiveButton("Reset") { dialog, _ ->
                    settingsRepository.resetToDefaults()
                    val mode = when (settingsRepository.themeMode) {
                        1 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                        2 -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
                    updateSettingsUI()
                    dialog.dismiss()
                    android.widget.Toast.makeText(this, "Settings reset to defaults", android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewSettings.findViewById<View>(R.id.row_mic)?.setOnClickListener {
            val options = arrayOf("Mute", "Microphone", "Internal Audio", "Internal & Mic")
            val values = intArrayOf(0, 1, 2, 3)
            val currentIndex = values.indexOf(settingsRepository.audioSourceMode).takeIf { it >= 0 } ?: 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.audioSourceMode = values[which]
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }
        
        val switchShowTouches = viewSettings.findViewById<Switch>(R.id.switch_show_touches)
        switchShowTouches?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !android.provider.Settings.System.canWrite(this)) {
                switchShowTouches.isChecked = false
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                android.widget.Toast.makeText(this, "Please grant permission to modify system settings", android.widget.Toast.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }
            settingsRepository.showTouches = isChecked
            updateSettingsUI()
        }
    }

    private fun setupLibraryView() {
        val recyclerView = viewLibrary.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
    }

    private fun loadLibrary() {
        val txtEmpty = viewLibrary.findViewById<View>(R.id.empty_state_container)
        val resolver = contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            android.provider.MediaStore.Video.Media._ID,
            android.provider.MediaStore.Video.Media.DISPLAY_NAME,
            android.provider.MediaStore.Video.Media.DURATION,
            android.provider.MediaStore.Video.Media.SIZE
        )
        val query = resolver.query(
            collection,
            projection,
            android.provider.MediaStore.Video.Media.DATA + " like ? ",
            arrayOf("%ScreenRecorder%"),
            android.provider.MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        val files = mutableListOf<VideoFile>()
        val retriever = android.media.MediaMetadataRetriever()
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val contentUri: Uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                
                var thumbnail: android.graphics.Bitmap? = null
                try {
                    retriever.setDataSource(this, contentUri)
                    thumbnail = retriever.getFrameAtTime(1000000) // 1 second in microseconds
                    thumbnail?.let {
                        val scaled = android.graphics.Bitmap.createScaledBitmap(it, 120, 120, true)
                        if (scaled != it) {
                            it.recycle()
                        }
                        thumbnail = scaled
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                files.add(VideoFile(contentUri, name, size, thumbnail))
            }
        }
        try { retriever.release() } catch (e: Exception) {}
        if (files.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
        } else {
            txtEmpty.visibility = View.GONE
        }
        val recyclerView = viewLibrary.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        
        fun updateSelectionUI(adapter: LibraryAdapter) {
            val selectionToolbar = viewLibrary.findViewById<LinearLayout>(R.id.selection_toolbar)
            val txtSelectionCount = viewLibrary.findViewById<TextView>(R.id.txt_selection_count)
            val txtTitle = viewLibrary.findViewById<TextView>(R.id.txt_title)
            
            if (adapter.isSelectionMode) {
                selectionToolbar.visibility = View.VISIBLE
                txtTitle.visibility = View.INVISIBLE
                txtSelectionCount.text = "${adapter.selectedFiles.size} selected"
            } else {
                selectionToolbar.visibility = View.GONE
                txtTitle.visibility = View.VISIBLE
            }
            
            val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isNightMode) {
                selectionToolbar.setBackgroundColor(Color.parseColor("#1C1C1E"))
                txtSelectionCount.setTextColor(Color.WHITE)
            } else {
                selectionToolbar.setBackgroundColor(Color.parseColor("#F2F2F7"))
                txtSelectionCount.setTextColor(Color.BLACK)
            }
        }
        
        val adapter = LibraryAdapter(
            files,
            { file ->
                val currentAdapter = recyclerView.adapter as LibraryAdapter
                if (currentAdapter.isSelectionMode) {
                    updateSelectionUI(currentAdapter)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, file.uri)
                    intent.setDataAndType(file.uri, "video/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
            },
            { file ->
                val currentAdapter = recyclerView.adapter as LibraryAdapter
                if (!currentAdapter.isSelectionMode) {
                    currentAdapter.toggleSelection(file)
                    updateSelectionUI(currentAdapter)
                }
            },
            { file ->
                deleteVideo(file)
            },
            { file ->
                renameVideo(file)
            },
            { file ->
                shareVideo(file)
            }
        )
        recyclerView.adapter = adapter
        
        val controller = android.view.animation.AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
        
        viewLibrary.findViewById<View>(R.id.btn_cancel_selection).setOnClickListener {
            adapter.clearSelection()
            updateSelectionUI(adapter)
        }
        
        viewLibrary.findViewById<View>(R.id.btn_delete_selected).setOnClickListener {
            if (adapter.selectedFiles.isNotEmpty()) {
                val selectedFilesList = adapter.selectedFiles.toList()
                val alert = android.app.AlertDialog.Builder(this)
                alert.setTitle("Delete Videos")
                alert.setMessage("Are you sure you want to delete ${selectedFilesList.size} selected video(s)?")
                alert.setPositiveButton("Delete") { _, _ ->
                    selectedFilesList.forEach { file ->
                        try {
                            contentResolver.delete(file.uri, null, null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    adapter.clearSelection()
                    updateSelectionUI(adapter)
                    loadLibrary()
                }
                alert.setNegativeButton("Cancel", null)
                alert.show()
            }
        }
    }

    private fun startPulseAnimation() {
        val halo = viewRecord.findViewById<View>(R.id.rec_halo)
        val btn = viewRecord.findViewById<View>(R.id.btn_record)
        
        if (pulseAnimator == null) {
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 0f)
            pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(halo, scaleX, scaleY, alpha).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
            }
        }
        
        if (btnAnimator == null) {
            val btnScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.05f)
            val btnScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.05f)
            btnAnimator = ObjectAnimator.ofPropertyValuesHolder(btn, btnScaleX, btnScaleY).apply {
                duration = 1000
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
        }
        
        pulseAnimator?.start()
        btnAnimator?.start()
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        btnAnimator?.cancel()
        val halo = viewRecord.findViewById<View>(R.id.rec_halo)
        val btn = viewRecord.findViewById<View>(R.id.btn_record)
        halo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).start()
        btn.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
    }

    private fun updateRecordUI() {
        val txtStatus = viewRecord.findViewById<TextView>(R.id.txt_status)
        val circle = viewRecord.findViewById<View>(R.id.rec_circle)
        val txtRec = viewRecord.findViewById<TextView>(R.id.txt_rec)
        
        // stats
        val resHeight = settingsRepository.resolutionHeight
        val badgeRes = when (resHeight) {
            720 -> "HD"
            1080 -> "FHD"
            1440 -> "QHD"
            else -> "FHD"
        }
        viewRecord.findViewById<TextView>(R.id.stat_res).text = "${resHeight}p"
        viewRecord.findViewById<TextView>(R.id.stat_res_badge).text = badgeRes

        val frameRate = settingsRepository.frameRate
        viewRecord.findViewById<TextView>(R.id.stat_fps).text = "$frameRate"
        viewRecord.findViewById<TextView>(R.id.stat_fps_badge).text = "FPS"

        val bitrate = settingsRepository.videoBitrate
        val bitMbps = bitrate / 1000000
        val badgeBit = when (bitMbps) {
            5 -> "LOW"
            8 -> "MED"
            12 -> "HIGH"
            16 -> "ULTRA"
            else -> "HIGH"
        }
        viewRecord.findViewById<TextView>(R.id.stat_bit).text = "$bitMbps Mbps"
        viewRecord.findViewById<TextView>(R.id.stat_bit_badge).text = badgeBit

        val statsContainer = viewRecord.findViewById<LinearLayout>(R.id.stats_row_container)

        if (RecordingManager.isRecording) {
            handler.removeCallbacks(liveTimerRunnable)
            handler.post(liveTimerRunnable)
            txtRec?.text = "STOP"
            startPulseAnimation()
            
            for (i in 0 until statsContainer.childCount) {
                val child = statsContainer.getChildAt(i)
                child.animate()
                    .alpha(0f)
                    .translationY(30f)
                    .setStartDelay((i * 100).toLong())
                    .setDuration(300)
                    .start()
            }
        } else {
            handler.removeCallbacks(liveTimerRunnable)
            txtStatus.text = "✦ Tap the button to start recording"
            txtRec?.text = "REC"
            stopPulseAnimation()
            
            for (i in 0 until statsContainer.childCount) {
                val child = statsContainer.getChildAt(i)
                child.translationY = 30f
                child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((i * 200).toLong())
                    .setDuration(500)
                    .setInterpolator(android.view.animation.OvershootInterpolator())
                    .start()
            }
        }
    }
    
    private fun updateSettingsUI() {
        val themeString = when(settingsRepository.themeMode) {
            1 -> "Light Theme"
            2 -> "Dark Theme"
            else -> "System Default"
        }
        viewSettings.findViewById<TextView>(R.id.val_theme)?.text = themeString

        val cdLength = settingsRepository.countdownDuration
        viewSettings.findViewById<TextView>(R.id.val_countdown)?.text = if (cdLength == 0) "Off" else "${cdLength}s"

        val resString = when(settingsRepository.resolutionHeight) {
            720 -> "720p HD"
            1080 -> "1080p HD"
            1440 -> "1440p HD"
            else -> "${settingsRepository.resolutionHeight}p (FHD)"
        }
        viewSettings.findViewById<TextView>(R.id.val_resolution).text = resString
        viewSettings.findViewById<TextView>(R.id.val_fps).text = "${settingsRepository.frameRate} FPS"
        viewSettings.findViewById<TextView>(R.id.val_bitrate).text = "${settingsRepository.videoBitrate / 1000000} Mbps"

        val encoderString = if (settingsRepository.videoEncoder == "H265") "H.265 / HEVC" else "H.264 / AVC"
        viewSettings.findViewById<TextView>(R.id.val_encoder)?.text = encoderString
        
        val encoderRow = viewSettings.findViewById<LinearLayout>(R.id.row_encoder)
        val encoderDesc = encoderRow?.getChildAt(1) as? TextView
        if (settingsRepository.videoEncoder == "H265") {
            encoderDesc?.text = "HEVC reduces file size but may not be compatible with older devices."
        } else {
            encoderDesc?.text = "H.264 is highly compatible with most standard video players."
        }

        val mbSize = settingsRepository.maxFileSize / (1024 * 1024)
        val limitString = if (mbSize <= 0) "No Limit" else if (mbSize >= 1024 && mbSize % 1024 == 0L) "${mbSize / 1024} GB" else "$mbSize MB"
        viewSettings.findViewById<TextView>(R.id.val_max_file_size)?.text = limitString

        val source = settingsRepository.audioSourceMode
        val sourceString = when(source) {
            0 -> "Mute"
            1 -> "Microphone"
            2 -> "Internal Audio"
            3 -> "Internal & Mic"
            else -> "Microphone"
        }
        viewSettings.findViewById<TextView>(R.id.val_audio_source)?.text = sourceString

        val switchShowTouches = viewSettings.findViewById<Switch>(R.id.switch_show_touches)
        switchShowTouches?.setOnCheckedChangeListener(null)
        switchShowTouches?.isChecked = settingsRepository.showTouches
        
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            viewSettings.setBackgroundColor(android.graphics.Color.parseColor("#0A0A14"))
            val settingsCards = arrayOf(R.id.row_theme, R.id.row_resolution, R.id.row_fps, R.id.row_countdown, R.id.row_bitrate, R.id.row_encoder, R.id.row_max_file_size, R.id.row_format, R.id.row_export_code, R.id.row_reset_defaults)
            val cardColor = android.graphics.Color.parseColor("#1C1C1E")
            for (id in settingsCards) {
                viewSettings.findViewById<View>(id)?.backgroundTintList = android.content.res.ColorStateList.valueOf(cardColor)
            }
            
            // Audio & Touches container
            (viewSettings.findViewById<View>(R.id.row_mic)?.parent as? View)?.backgroundTintList = android.content.res.ColorStateList.valueOf(cardColor)
        } else {
            viewSettings.setBackgroundColor(android.graphics.Color.parseColor("#F8FAFC"))
            val settingsCards = arrayOf(R.id.row_theme, R.id.row_resolution, R.id.row_fps, R.id.row_countdown, R.id.row_bitrate, R.id.row_encoder, R.id.row_max_file_size, R.id.row_format, R.id.row_export_code, R.id.row_reset_defaults)
            val cardColor = android.graphics.Color.parseColor("#FFFFFF")
            for (id in settingsCards) {
                viewSettings.findViewById<View>(id)?.backgroundTintList = android.content.res.ColorStateList.valueOf(cardColor)
            }
            (viewSettings.findViewById<View>(R.id.row_mic)?.parent as? View)?.backgroundTintList = android.content.res.ColorStateList.valueOf(cardColor)
        }
        
        setupSettingsView() // reattach listeners
    }

    override fun onResume() {
        super.onResume()
        updateRecordUI()
        updateSettingsUI()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun checkPermissionsAndStart() {
        if (isCountingDown) {
            cancelCountdown()
            return
        }
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
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                proceedRecording()
            } else {
                android.widget.Toast.makeText(this, "Permissions required to record.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun proceedRecording() {
        if (RecordingManager.isRecording) {
            stopRecording()
        } else {
            val duration = settingsRepository.countdownDuration
            if (duration <= 0) {
                startRecordingFlow()
            } else {
                startCountdown(duration)
            }
        }
    }

    private var countdownRunnable: Runnable? = null

    private fun startCountdown(seconds: Int) {
        val txtCountdown = viewRecord.findViewById<TextView>(R.id.txt_countdown)
        txtCountdown.visibility = View.VISIBLE
        isCountingDown = true
        var remaining = seconds
        
        countdownRunnable = object : Runnable {
            override fun run() {
                if (remaining > 0) {
                    txtCountdown.text = remaining.toString()
                    remaining--
                    handler.postDelayed(this, 1000)
                } else {
                    txtCountdown.visibility = View.GONE
                    isCountingDown = false
                    startRecordingFlow()
                }
            }
        }
        handler.post(countdownRunnable!!)
    }

    private fun cancelCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        isCountingDown = false
        viewRecord.findViewById<TextView>(R.id.txt_countdown).visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            // Delete action was granted by the user, we can try to reload the library
            loadLibrary()
        }
    }

    private fun shareVideo(file: VideoFile) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, file.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Video"))
    }

    private fun deleteVideo(file: VideoFile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Video")
            .setMessage("Are you sure you want to delete this video?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    contentResolver.delete(file.uri, null, null)
                    loadLibrary()
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val recoverableSecurityException = e as? android.app.RecoverableSecurityException
                        val intentSender = recoverableSecurityException?.userAction?.actionIntent?.intentSender
                        if (intentSender != null) {
                            startIntentSenderForResult(intentSender, 100, null, 0, 0, 0, null)
                        }
                    } else {
                        android.widget.Toast.makeText(this, "Permission denied to delete video", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Failed to delete video", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameVideo(file: VideoFile) {
        val editText = android.widget.EditText(this).apply {
            setText(file.name)
            setSelection(file.name.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Rename Video")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank() && newName != file.name) {
                    try {
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, newName)
                        }
                        contentResolver.update(file.uri, values, null, null)
                        loadLibrary()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this, "Failed to rename video", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        android.widget.Toast.makeText(this, "Recording finished", android.widget.Toast.LENGTH_LONG).show()
    }
}

data class VideoFile(val uri: Uri, val name: String, val size: Long, val thumbnail: android.graphics.Bitmap?)

class LibraryAdapter(
    private val files: List<VideoFile>,
    private val onClick: (VideoFile) -> Unit,
    private val onLongClick: (VideoFile) -> Unit,
    private val onDelete: (VideoFile) -> Unit,
    private val onRename: (VideoFile) -> Unit,
    private val onShare: (VideoFile) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    val selectedFiles = mutableSetOf<VideoFile>()
    var isSelectionMode = false

    fun toggleSelection(file: VideoFile) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file)
            if (selectedFiles.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedFiles.add(file)
            isSelectionMode = true
        }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedFiles.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.img_thumbnail)
        val imgCheck: ImageView? = view.findViewById(R.id.img_check)
        val title: TextView = view.findViewById(R.id.txt_title)
        val subtitle: TextView = view.findViewById(R.id.txt_subtitle)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
        val btnMore: ImageView = view.findViewById(R.id.btn_more)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.title.text = file.name
        holder.subtitle.text = "${file.size / (1024 * 1024)} MB"
        
        if (file.thumbnail != null) {
            holder.thumbnail.setImageBitmap(file.thumbnail)
            holder.thumbnail.setPadding(0, 0, 0, 0)
        } else {
            holder.thumbnail.setImageResource(android.R.drawable.ic_media_play)
            holder.thumbnail.setPadding(8, 8, 8, 8)
        }
        
        val isSelected = selectedFiles.contains(file)
        if (isSelectionMode) {
            holder.btnDelete.visibility = View.GONE
            holder.btnMore.visibility = View.GONE
            holder.imgCheck?.visibility = View.VISIBLE
            holder.imgCheck?.setImageResource(if (isSelected) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background)
            val isNightMode = (holder.itemView.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val tintColor = if (isSelected) {
                if (isNightMode) android.graphics.Color.parseColor("#1C1C3E") else android.graphics.Color.parseColor("#E0E7FF")
            } else {
                if (isNightMode) android.graphics.Color.parseColor("#1C1C1E") else android.graphics.Color.parseColor("#FFFFFF")
            }
            holder.itemView.backgroundTintList = android.content.res.ColorStateList.valueOf(tintColor)
        } else {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnMore.visibility = View.VISIBLE
            holder.imgCheck?.visibility = View.GONE
            val isNightMode = (holder.itemView.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            holder.itemView.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isNightMode) android.graphics.Color.parseColor("#1C1C1E") else android.graphics.Color.parseColor("#FFFFFF"))
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(file)
                onClick(file) // we'll handle selection inside the activity as well to update UI
            } else {
                onClick(file)
            }
        }
        
        holder.itemView.setOnLongClickListener {
            onLongClick(file)
            true
        }
        
        holder.btnDelete.setOnClickListener { onDelete(file) }
        
        holder.btnMore.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(view.context, holder.btnMore)
            popup.menu.add(android.view.Menu.NONE, 1, 1, "Rename")
            popup.menu.add(android.view.Menu.NONE, 2, 2, "Share")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onRename(file)
                    2 -> onShare(file)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = files.size
}
