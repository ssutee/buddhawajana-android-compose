package com.touchsi.buddhawajana.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.touchsi.buddhawajana.R
import com.touchsi.buddhawajana.ui.theme.BuddhawajanaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuddhawajanaTheme {
                val windowSize = rememberWindowSizeClass()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(windowSize)
                }
            }
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, windowSize: WindowSize) {
    NavHost(navController, startDestination = NavigationItem.Books.route) {
        composable(NavigationItem.Books.route) {
            BookScreen()
        }
        composable(NavigationItem.Audio.route) {
            AudioScreen(windowSize =  windowSize)
        }
        composable(NavigationItem.Youtube.route) {
            YoutubeScreen()
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem.Books,
        NavigationItem.Audio,
        NavigationItem.Youtube
    )
    BottomNavigation(
        backgroundColor = colorResource(id = R.color.topbar_bg),
        contentColor = Color.Black
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            BottomNavigationItem(
                icon = { Icon(painterResource(id = item.icon),
                    contentDescription = stringResource(item.title)) },
                label = {
                    Text(text = stringResource(item.title),
                        color = Color.DarkGray, fontSize = 13.sp) },
                selectedContentColor = Color.Black,
                unselectedContentColor = Color.Black.copy(0.4f),
                alwaysShowLabel = true,
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { screen_route ->
                            popUpTo(screen_route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(windowSize: WindowSize) {
    val navController = rememberNavController()
    val permissionState = rememberMultiplePermissionsState(permissions = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ))

    LaunchedEffect(Unit) {
        when (Build.VERSION.SDK_INT) {
            in 1..29 -> {
                if (!permissionState.allPermissionsGranted) {
                    permissionState.launchMultiplePermissionRequest()
                }
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavigationGraph(navController = navController, windowSize)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BuddhawajanaTheme {
        MainScreen(WindowSize.Expanded)
    }
}