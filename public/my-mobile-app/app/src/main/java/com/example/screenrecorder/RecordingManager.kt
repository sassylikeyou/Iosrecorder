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

        try {
            setupMediaRecorder(context, settings)
            mediaRecorder?.prepare()
            virtualDisplay = createVirtualDisplay(settings)
            mediaRecorder?.start()
            isRecording = true
        } catch (e: Exception) {
            android.util.Log.e("ScreenRecorder", "Failed to start recording", e)
            // Try fallback: lower resolution or simpler settings
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
        var hasAudio = false
        if (settings.audioSourceMode != 0) {
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                hasAudio = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val outputFile = getOutputFile(context, settings)
        recorder.setOutputFile(outputFile.absolutePath)

        val encoder = if (settings.videoEncoder == "H265") MediaRecorder.VideoEncoder.HEVC else MediaRecorder.VideoEncoder.H264
        recorder.setVideoEncoder(encoder)
        
        if (hasAudio) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
        }

        recorder.setVideoSize(getVideoWidth(settings), getVideoHeight(settings))
        recorder.setVideoEncodingBitRate(settings.videoBitrate)
        recorder.setVideoFrameRate(settings.frameRate)
    }

    private fun createVirtualDisplay(settings: SettingsRepository): VirtualDisplay? {
        val width = getVideoWidth(settings)
        val height = getVideoHeight(settings)
        val dpi = 300 // Standard fallback DPI

        return mediaProjection?.createVirtualDisplay(
            "ScreenRecorder",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )
    }

    private fun getVideoWidth(settings: SettingsRepository): Int {
        val baseShort = settings.resolutionHeight
        val baseLong = (baseShort * 16) / 9
        val width = if (settings.orientationMode == 2) baseLong else baseShort
        return width - (width % 2) // Ensure even
    }

    private fun getVideoHeight(settings: SettingsRepository): Int {
        val baseShort = settings.resolutionHeight
        val baseLong = (baseShort * 16) / 9
        val height = if (settings.orientationMode == 2) baseShort else baseLong
        return height - (height % 2) // Ensure even
    }

    private fun getOutputFile(context: Context, settings: SettingsRepository): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (settings.outputFormat == "MKV") ".mkv" else ".mp4"
        val fileName = "Screen_$timestamp$extension"

        var dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (dir == null) {
            dir = context.filesDir
        }
        dir?.mkdirs()
        
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
