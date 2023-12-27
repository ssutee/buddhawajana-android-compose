package com.watnapp.buddhawajana.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator

enum class WindowSize {
    Compact, Medium, Expanded
}

@Composable
fun Activity.rememberWindowSizeClass(): WindowSize {
    val windowSize = rememberWindowSize()
    val windowDpSize = with(LocalDensity.current) {
        windowSize.toDpSize()
    }
    return getWindowSizeClass(windowSize = windowDpSize)
}

@Composable
fun Activity.rememberWindowSize(): Size {
    val configuration = LocalConfiguration.current
    val windowMetric = remember(configuration) {
        WindowMetricsCalculator
            .getOrCreate()
            .computeCurrentWindowMetrics(this)
    }
    return windowMetric.bounds.toComposeRect().size
}

@Composable
fun getWindowSizeClass(windowSize: DpSize) = when {
    windowSize.width < 0.dp ->
        throw IllegalArgumentException("Window size can not be negative")
    windowSize.width < 600.dp ->
        WindowSize.Compact
    windowSize.width < 840.dp ->
        WindowSize.Medium
    else ->
        WindowSize.Expanded
}