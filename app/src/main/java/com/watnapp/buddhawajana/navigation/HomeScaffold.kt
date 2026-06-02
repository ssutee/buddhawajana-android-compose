package com.watnapp.buddhawajana.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.watnapp.buddhawajana.core.designsystem.component.BuddhawajanaTopBar
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.nav.BuddhawajanaNavSuite
import com.watnapp.buddhawajana.core.ui.nav.TopDestination
import com.watnapp.buddhawajana.feature.audio.navigation.AudioPane
import com.watnapp.buddhawajana.feature.books.navigation.BooksListPane
import com.watnapp.buddhawajana.ui.YoutubeScreen
import org.koin.compose.koinInject

@Composable
fun HomeScaffold(onOpenBook: (Long) -> Unit, onOpenPlayer: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(TopDestination.AUDIO) }
    val controller: PlaybackController = koinInject()
    Scaffold(
        topBar = {
            BuddhawajanaTopBar(title = "พุทธวจน", onSettingsClick = { })
        }
    ) { innerPadding ->
        BuddhawajanaNavSuite(selected = selected, onSelect = { selected = it }) {
            Column(modifier = Modifier.padding(innerPadding)) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selected) {
                        TopDestination.AUDIO -> AudioPane(onOpenPlayer = onOpenPlayer)
                        TopDestination.BOOKS -> BooksListPane(onOpenBook = onOpenBook)
                        TopDestination.YOUTUBE -> YoutubeScreen()
                    }
                }
                MiniPlayer(controller = controller, onExpand = onOpenPlayer)
            }
        }
    }
}
