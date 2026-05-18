package com.example.screenrecorder

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class RecorderTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val isRecording = RecordingManager.isRecording
        if (isRecording) {
            // Stop recording
            val intent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            startService(intent)
        } else {
            // Start recording (needs activity to get projection token if Android 14+)
            // For older versions or if token cached, we could start directly
            // But Android 14+ requires intent prompt per session.
            val intent = Intent(this, RequestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivityAndCollapse(intent)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (RecordingManager.isRecording) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Stop Recording"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Recording..."
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Screen Record"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap to start"
            }
        }
        tile.updateTile()
    }
}
