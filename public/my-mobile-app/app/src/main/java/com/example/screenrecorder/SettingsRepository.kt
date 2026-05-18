package com.example.screenrecorder

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("RecorderSettings", Context.MODE_PRIVATE)

    var resolutionHeight: Int
        get() = prefs.getInt("resolutionHeight", 1080)
        set(value) = prefs.edit().putInt("resolutionHeight", value).apply()

    var frameRate: Int
        get() = prefs.getInt("frameRate", 60)
        set(value) = prefs.edit().putInt("frameRate", value).apply()

    var videoBitrate: Int
        get() = prefs.getInt("videoBitrate", 5_000_000) // 5 Mbps default
        set(value) = prefs.edit().putInt("videoBitrate", value).apply()

    // 0 = AUTO, 1 = PORTRAIT, 2 = LANDSCAPE
    var orientationMode: Int
        get() = prefs.getInt("orientationMode", 0)
        set(value) = prefs.edit().putInt("orientationMode", value).apply()

    // 0 = NONE, 1 = MIC, 2 = INTERNAL, 3 = BOTH
    var audioSourceMode: Int
        get() = prefs.getInt("audioSourceMode", 1)
        set(value) = prefs.edit().putInt("audioSourceMode", value).apply()

    // "H264" or "H265"
    var videoEncoder: String
        get() = prefs.getString("videoEncoder", "H264") ?: "H264"
        set(value) = prefs.edit().putString("videoEncoder", value).apply()

    var outputFormat: String
        get() = prefs.getString("outputFormat", "MP4") ?: "MP4"
        set(value) = prefs.edit().putString("outputFormat", value).apply()

    var outputFolderUri: String?
        get() = prefs.getString("outputFolderUri", null)
        set(value) = prefs.edit().putString("outputFolderUri", value).apply()

    var themeMode: Int
        get() = prefs.getInt("themeMode", -1) // -1 SYSTEM, 1 LIGHT, 2 DARK
        set(value) = prefs.edit().putInt("themeMode", value).apply()

    var countdownDuration: Int
        get() = prefs.getInt("countdownDuration", 3) // default 3s
        set(value) = prefs.edit().putInt("countdownDuration", value).apply()

    var showTouches: Boolean
        get() = prefs.getBoolean("showTouches", false)
        set(value) = prefs.edit().putBoolean("showTouches", value).apply()

    var maxFileSize: Long
        get() = prefs.getLong("maxFileSize", 0L) // 0 = no limit
        set(value) = prefs.edit().putLong("maxFileSize", value).apply()

    fun getSummary(): String {
        return "${resolutionHeight}p · ${frameRate}fps · ${(videoBitrate / 1_000_000)}Mbps"
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
