package com.watnapp.buddhawajana.navigation

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.watnapp.buddhawajana.core.designsystem.component.BuddhawajanaTopBar
import com.watnapp.buddhawajana.core.ui.nav.BuddhawajanaNavSuite
import com.watnapp.buddhawajana.core.ui.nav.TopDestination
import com.watnapp.buddhawajana.ui.AudioScreen
import com.watnapp.buddhawajana.ui.BookScreen
import com.watnapp.buddhawajana.ui.WindowSize
import com.watnapp.buddhawajana.ui.YoutubeScreen
import com.watnapp.buddhawajana.ui.rememberWindowSizeClass

@Composable
fun BuddhawajanaNavHost() {
    var selected by rememberSaveable { mutableStateOf(TopDestination.AUDIO) }

    // Retrieve window size for screens that require it (AudioScreen).
    // rememberWindowSizeClass is an extension on Activity so we resolve it here.
    val activity = LocalContext.current as? Activity
    val windowSize: WindowSize = if (activity != null) {
        activity.rememberWindowSizeClass()
    } else {
        WindowSize.Compact
    }

    Scaffold(
        topBar = {
            BuddhawajanaTopBar(
                title = "พุทธวจน",
                onSettingsClick = { /* no-op for now */ }
            )
        }
    ) { innerPadding ->
        BuddhawajanaNavSuite(
            selected = selected,
            onSelect = { selected = it },
        ) {
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selected) {
                    TopDestination.AUDIO -> AudioScreen(windowSize = windowSize)
                    TopDestination.BOOKS -> BookScreen()
                    TopDestination.YOUTUBE -> YoutubeScreen()
                }
            }
        }
    }
}
