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

        switchTab(0)
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

        val colorActive = Color.parseColor("#8B5CF6")
        val colorInactive = Color.parseColor("#8E8E93")

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
        viewSettings.findViewById<View>(R.id.row_format).setOnClickListener {
            val options = arrayOf("MP4", "MKV")
            val currentIndex = if (settingsRepository.outputFormat == "MP4") 0 else 1
            AlertDialog.Builder(this).setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.outputFormat = options[which]
                updateSettingsUI()
                dialog.dismiss()
            }.show()
        }

        val switchMic = viewSettings.findViewById<Switch>(R.id.switch_mic)
        switchMic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settingsRepository.audioSourceMode = if (viewSettings.findViewById<Switch>(R.id.switch_sys_audio).isChecked) 3 else 1
            } else {
                settingsRepository.audioSourceMode = if (viewSettings.findViewById<Switch>(R.id.switch_sys_audio).isChecked) 2 else 0
            }
            updateSettingsUI()
        }
        
        val switchSys = viewSettings.findViewById<Switch>(R.id.switch_sys_audio)
        switchSys.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settingsRepository.audioSourceMode = if (viewSettings.findViewById<Switch>(R.id.switch_mic).isChecked) 3 else 2
            } else {
                settingsRepository.audioSourceMode = if (viewSettings.findViewById<Switch>(R.id.switch_mic).isChecked) 1 else 0
            }
            updateSettingsUI()
        }
    }

    private fun setupLibraryView() {
        val recyclerView = viewLibrary.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
    }

    private fun loadLibrary() {
        val txtEmpty = viewLibrary.findViewById<TextView>(R.id.txt_empty)
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
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val contentUri: Uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                files.add(VideoFile(contentUri, name, size))
            }
        }
        if (files.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
        } else {
            txtEmpty.visibility = View.GONE
        }
        val recyclerView = viewLibrary.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = LibraryAdapter(
            files,
            onClick = { file ->
                val intent = Intent(Intent.ACTION_VIEW, file.uri)
                intent.setDataAndType(file.uri, "video/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            },
            onDelete = { file ->
                deleteVideo(file)
            },
            onRename = { file ->
                renameVideo(file)
            },
            onShare = { file ->
                shareVideo(file)
            }
        )
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
            txtStatus.text = "Recording in progress..."
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

        val resString = when(settingsRepository.resolutionHeight) {
            720 -> "720p HD"
            1080 -> "1080p HD"
            1440 -> "1440p HD"
            else -> "${settingsRepository.resolutionHeight}p (FHD)"
        }
        viewSettings.findViewById<TextView>(R.id.val_resolution).text = resString
        viewSettings.findViewById<TextView>(R.id.val_fps).text = "${settingsRepository.frameRate} FPS"
        viewSettings.findViewById<TextView>(R.id.val_bitrate).text = "${settingsRepository.videoBitrate / 1000000} Mbps"

        val source = settingsRepository.audioSourceMode
        viewSettings.findViewById<Switch>(R.id.switch_mic).setOnCheckedChangeListener(null)
        
        viewSettings.findViewById<Switch>(R.id.switch_mic).isChecked = (source == 1 || source == 3)
        
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
            startRecordingFlow()
        }
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

data class VideoFile(val uri: Uri, val name: String, val size: Long)

class LibraryAdapter(
    private val files: List<VideoFile>,
    private val onClick: (VideoFile) -> Unit,
    private val onDelete: (VideoFile) -> Unit,
    private val onRename: (VideoFile) -> Unit,
    private val onShare: (VideoFile) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
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
        
        holder.itemView.setOnClickListener { onClick(file) }
        
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
