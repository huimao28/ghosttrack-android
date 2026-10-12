package com.huimao.ghosttrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.huimao.ghosttrack.ui.GhostTrackApp
import com.huimao.ghosttrack.ui.theme.GhostTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhostTrackTheme {
                GhostTrackApp()
            }
        }
    }
}
