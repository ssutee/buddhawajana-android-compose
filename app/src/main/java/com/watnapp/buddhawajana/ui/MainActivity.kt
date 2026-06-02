package com.watnapp.buddhawajana.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.watnapp.buddhawajana.core.designsystem.theme.BuddhawajanaTheme
import com.watnapp.buddhawajana.navigation.BuddhawajanaNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // POST_NOTIFICATIONS is requested in-context when the player first opens
        // (see BuddhawajanaNavHost.PlayerRoute), not at cold launch.
        setContent {
            BuddhawajanaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BuddhawajanaNavHost()
                }
            }
        }
    }
}
