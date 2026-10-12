package com.huimao.ghosttrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.huimao.ghosttrack.ui.GhostTrackApp
import com.huimao.ghosttrack.ui.about.LanguageSetting
import com.huimao.ghosttrack.ui.theme.GhostTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 先应用用户选择的语言，再绘制界面
        LanguageSetting.apply(this, LanguageSetting.current(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhostTrackTheme {
                GhostTrackApp()
            }
        }
    }
}
