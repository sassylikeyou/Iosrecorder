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
        setContentView(R.layout.activity_settings)
        
        settingsRepository = SettingsRepository(this)

        updateFolderUI()

        findViewById<Button>(R.id.btn_choose_folder).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            folderPickerLauncher.launch(intent)
        }

        val rgAudio = findViewById<android.widget.RadioGroup>(R.id.rg_audio_source)
        when (settingsRepository.audioSourceMode) {
            0 -> rgAudio.check(R.id.rb_audio_none)
            1 -> rgAudio.check(R.id.rb_audio_mic)
            2 -> rgAudio.check(R.id.rb_audio_internal)
            3 -> rgAudio.check(R.id.rb_audio_both)
        }

        rgAudio.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_audio_none -> 0
                R.id.rb_audio_mic -> 1
                R.id.rb_audio_internal -> 2
                R.id.rb_audio_both -> 3
                else -> 1
            }
            settingsRepository.audioSourceMode = mode
        }
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
