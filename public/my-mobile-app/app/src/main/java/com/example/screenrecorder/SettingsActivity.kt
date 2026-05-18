package com.example.screenrecorder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                // Persist permissions
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                settingsRepository.outputFolderUri = uri.toString()
                updateFolderUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings) // Assume basic linear layout exists
        
        settingsRepository = SettingsRepository(this)

        updateFolderUI()

        findViewById<Button>(R.id.btn_choose_folder).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            folderPickerLauncher.launch(intent)
        }

        // Implementation of dropdowns and sliders for other settings would go here,
        // wired directly to `settingsRepository` properties.
    }

    private fun updateFolderUI() {
        val txtFolder = findViewById<TextView>(R.id.txt_folder_path)
        val uriStr = settingsRepository.outputFolderUri
        if (uriStr != null) {
            txtFolder.text = "Save path: " + Uri.parse(uriStr).lastPathSegment
        } else {
            txtFolder.text = "Save path: Default App Movies Folder"
        }
    }
}
