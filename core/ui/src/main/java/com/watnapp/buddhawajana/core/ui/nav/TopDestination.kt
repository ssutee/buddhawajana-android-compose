package com.watnapp.buddhawajana.core.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(val label: String, val icon: ImageVector) {
    AUDIO("หลวงพ่อ", Icons.Default.Headphones),
    BOOKS("หนังสือ", Icons.AutoMirrored.Filled.MenuBook),
    YOUTUBE("ยูทูบ", Icons.Default.PlayCircle),
}
