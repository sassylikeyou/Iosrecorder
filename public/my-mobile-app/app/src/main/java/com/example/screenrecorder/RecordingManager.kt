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

    fun startRecording(context: Context, projection: MediaProjection) {
        if (isRecording) return

        mediaProjection = projection
        val settings = SettingsRepository(context)
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        setupMediaRecorder(context, settings)

        try {
            mediaRecorder?.prepare()
            virtualDisplay = createVirtualDisplay(settings)
            mediaRecorder?.start()
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseResources()
        }
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

        // Audio setup
        if (settings.audioSourceMode == 1 || settings.audioSourceMode == 3) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        
        val format = if (settings.outputFormat == "MKV" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaRecorder.OutputFormat.WEBM
        } else {
            MediaRecorder.OutputFormat.MPEG_4
        }
        recorder.setOutputFormat(format)

        val outputFile = getOutputFile(context, settings)
        recorder.setOutputFile(outputFile.absolutePath)

        val encoder = if (settings.videoEncoder == "H265") MediaRecorder.VideoEncoder.HEVC else MediaRecorder.VideoEncoder.H264
        recorder.setVideoEncoder(encoder)
        
        if (settings.audioSourceMode == 1 || settings.audioSourceMode == 3) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }

        // Use standard 16:9 ratio, handled by orientation settings
        recorder.setVideoSize(getScreenWidth(settings), settings.resolutionHeight)
        recorder.setVideoEncodingBitRate(settings.videoBitrate)
        recorder.setVideoFrameRate(settings.frameRate)
    }

    private fun createVirtualDisplay(settings: SettingsRepository): VirtualDisplay? {
        val width = getScreenWidth(settings)
        val height = settings.resolutionHeight
        val dpi = 300 // Standard fallback DPI

        return mediaProjection?.createVirtualDisplay(
            "ScreenRecorder",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )
    }

    private fun getScreenWidth(settings: SettingsRepository): Int {
        // Simple 16:9 aspect ratio calculation 
        // In a real app, use WindowManager to get exact screen bounds based on orientation
        return (settings.resolutionHeight * 9) / 16
    }

    private fun getOutputFile(context: Context, settings: SettingsRepository): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (settings.outputFormat == "MKV") ".mkv" else ".mp4"
        val fileName = "Screen_$timestamp$extension"

        val dir = if (settings.outputFolderUri != null) {
            // Simplified: In a full app, we use ContentResolver to write to the SAF DocumentFile
            // Here we fallback to Movies directory if SAF logic is omitted for brevity
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        }
        
        return File(dir, fileName)
    }

    private fun releaseResources() {
        virtualDisplay?.release()
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaProjection?.stop()
        
        virtualDisplay = null
        mediaRecorder = null
        mediaProjection = null
    }
}
