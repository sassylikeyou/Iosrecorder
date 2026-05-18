package com.example.screenrecorder

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RecordingManager {
    var isRecording = false
        private set

    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    private var currentFileDescriptor: android.os.ParcelFileDescriptor? = null
    private var currentOutputUri: Uri? = null
    private var currentFallbackFile: File? = null

    fun startRecording(context: Context, projection: MediaProjection): Boolean {
        if (isRecording) return true

        mediaProjection = projection
        val settings = SettingsRepository(context)
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    val stopIntent = android.content.Intent(context, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
            
            setupMediaRecorder(context, settings)
            mediaRecorder?.prepare()
            virtualDisplay = createVirtualDisplay(context, settings)
            mediaRecorder?.start()
            isRecording = true
            true
        } catch (e: Exception) {
            android.util.Log.e("ScreenRecorder", "Failed to start recording", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "Recording Error: ${e.message ?: e.javaClass.simpleName}", android.widget.Toast.LENGTH_LONG).show()
            }
            deleteFailedFile(context)
            releaseResources()
            false
        }
    }

    private fun deleteFailedFile(context: Context) {
        try {
            currentOutputUri?.let { context.contentResolver.delete(it, null, null) }
            currentFallbackFile?.let { if (it.exists() && it.length() == 0L) it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentOutputUri = null
        currentFallbackFile = null
    }

    fun stopRecording() {
        if (!isRecording) return
        
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            releaseResources()
            isRecording = false
        }
    }

    private fun setupMediaRecorder(context: Context, settings: SettingsRepository) {
        val recorder = mediaRecorder ?: return

        var hasAudio = false
        if (settings.audioSourceMode != 0) {
            val audioPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            if (audioPerm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                hasAudio = true
            }
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (settings.outputFormat == "MKV") ".mkv" else ".mp4"
        val fileName = "Screen_$timestamp$extension"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ScreenRecorder")
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                currentOutputUri = uri
                currentFileDescriptor = context.contentResolver.openFileDescriptor(uri, "rw")
                recorder.setOutputFile(currentFileDescriptor?.fileDescriptor)
            } else {
                val fallbackFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)
                currentFallbackFile = fallbackFile
                recorder.setOutputFile(fallbackFile.absolutePath)
            }
        } else {
            val fallbackFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)
            currentFallbackFile = fallbackFile
            recorder.setOutputFile(fallbackFile.absolutePath)
        }

        val encoder = if (settings.videoEncoder == "H265") MediaRecorder.VideoEncoder.HEVC else MediaRecorder.VideoEncoder.H264
        recorder.setVideoEncoder(encoder)
        
        if (hasAudio) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
        }

        recorder.setVideoSize(getVideoWidth(context, settings), getVideoHeight(context, settings))
        recorder.setVideoEncodingBitRate(settings.videoBitrate)
        recorder.setVideoFrameRate(settings.frameRate)
    }

    private fun createVirtualDisplay(context: Context, settings: SettingsRepository): VirtualDisplay? {
        val width = getVideoWidth(context, settings)
        val height = getVideoHeight(context, settings)
        val dpi = context.resources.displayMetrics.densityDpi

        return mediaProjection?.createVirtualDisplay(
            "ScreenRecorder",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )
    }

    private fun getVideoWidth(context: Context, settings: SettingsRepository): Int {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val isPortraitDevice = w < h
        
        val mWidth = Math.min(w, h)
        val mHeight = Math.max(w, h)
        val aspectRatio = mHeight.toFloat() / mWidth.toFloat()
        
        val baseShort = settings.resolutionHeight
        val baseLong = (baseShort * aspectRatio).toInt()
        
        val isPortrait = when (settings.orientationMode) {
            1 -> true
            2 -> false
            else -> isPortraitDevice
        }
        
        var width = if (isPortrait) baseShort else baseLong
        return width - (width % 2) // Ensure even
    }

    private fun getVideoHeight(context: Context, settings: SettingsRepository): Int {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val isPortraitDevice = w < h
        
        val mWidth = Math.min(w, h)
        val mHeight = Math.max(w, h)
        val aspectRatio = mHeight.toFloat() / mWidth.toFloat()
        
        val baseShort = settings.resolutionHeight
        val baseLong = (baseShort * aspectRatio).toInt()
        
        val isPortrait = when (settings.orientationMode) {
            1 -> true
            2 -> false
            else -> isPortraitDevice
        }
        
        var height = if (isPortrait) baseLong else baseShort
        return height - (height % 2) // Ensure even
    }

    private fun releaseResources() {
        try {
            currentFileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentFileDescriptor = null
        currentOutputUri = null
        currentFallbackFile = null

        virtualDisplay?.release()
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaProjection?.stop()
        
        virtualDisplay = null
        mediaRecorder = null
        mediaProjection = null
    }
}
