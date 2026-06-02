package com.watnapp.buddhawajana.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.feature.audio.player.PlayerScreen
import com.watnapp.buddhawajana.feature.audio.player.PlayerViewModel
import com.watnapp.buddhawajana.feature.books.reader.ReaderScreen
import com.watnapp.buddhawajana.feature.books.reader.ReaderViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Serializable private data object HomeRoute
@Serializable private data class ReaderRoute(val bookId: Long)
@Serializable private data object PlayerRoute

@Composable
fun BuddhawajanaNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScaffold(
                onOpenBook = { id -> nav.navigate(ReaderRoute(id)) },
                onOpenPlayer = { nav.navigate(PlayerRoute) },
            )
        }
        composable<ReaderRoute> { entry ->
            val bookId = entry.toRoute<ReaderRoute>().bookId
            val vm: ReaderViewModel = koinViewModel { parametersOf(bookId) }
            val context = LocalContext.current
            val fileStore: BookFileStore = koinInject()
            ReaderScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onShare = {
                    val file = fileStore.file(bookId)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
            )
        }
        composable<PlayerRoute> {
            val playerVm: PlayerViewModel = koinViewModel()
            PlayerScreen(vm = playerVm, onBack = { nav.popBackStack() })
        }
    }
}
