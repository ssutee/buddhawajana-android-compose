package com.watnapp.buddhawajana.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.watnapp.buddhawajana.core.designsystem.theme.BuddhawajanaTheme
import com.watnapp.buddhawajana.navigation.BuddhawajanaNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge (required for apps targeting SDK 35+; replaces the deprecated
        // Window.setStatusBarColor / setDecorFitsSystemWindows APIs). The system bars become
        // transparent and draw over the window.
        enableEdgeToEdge()
        setContent {
            BuddhawajanaTheme {
                // Surface fills the whole window (so the transparent bars sit over the app
                // background), and safeDrawingPadding insets — and consumes — the system-bar
                // insets once at the root, so child Scaffolds/screens lay out inside the safe
                // area without extra per-screen inset handling.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        BuddhawajanaNavHost()
                    }
                }
            }
        }
    }
}
