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
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
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
