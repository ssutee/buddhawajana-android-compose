package com.watnapp.buddhawajana.navigation

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.runtime.Composable
import com.watnapp.buddhawajana.feature.books.reader.ReaderScreen
import com.watnapp.buddhawajana.feature.books.reader.ReaderViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable private data object HomeRoute
@Serializable private data class ReaderRoute(val bookId: Long)

@Composable
fun BuddhawajanaNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScaffold(onOpenBook = { id -> nav.navigate(ReaderRoute(id)) })
        }
        composable<ReaderRoute> { entry ->
            val bookId = entry.toRoute<ReaderRoute>().bookId
            val vm: ReaderViewModel = koinViewModel { parametersOf(bookId) }
            ReaderScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onShare = { /* BT11 wires FileProvider share */ },
            )
        }
    }
}
