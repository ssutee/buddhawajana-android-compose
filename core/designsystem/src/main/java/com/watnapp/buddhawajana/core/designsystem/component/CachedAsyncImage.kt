package com.watnapp.buddhawajana.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun CachedAsyncImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    AsyncImage(model = url, contentDescription = contentDescription, modifier = modifier, contentScale = ContentScale.Crop)
}
