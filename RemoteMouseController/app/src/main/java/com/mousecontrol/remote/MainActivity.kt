package com.mousecontrol.remote

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.mousecontrol.remote.ui.RemoteMouseNavHost
import com.mousecontrol.remote.ui.theme.RemoteMouseTheme
import com.mousecontrol.remote.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Strict landscape lock, per spec.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            RemoteMouseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RemoteMouseNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.disconnect()
        super.onDestroy()
    }
}
