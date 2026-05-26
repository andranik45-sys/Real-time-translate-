package com.example

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TranslationScreen
import com.example.ui.TranslationViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: TranslationViewModel = viewModel()

        // Handle dynamic audio recording runtime permissions safely
        val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
          if (isGranted) {
            viewModel.startListening()
          } else {
            viewModel.onPermissionDenied()
          }
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          TranslationScreen(
            viewModel = viewModel,
            onRequestPermission = { language ->
              viewModel.prepareToListen(language)
              val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
              )
              if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                viewModel.startListening(language)
              } else {
                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
              }
            },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}
