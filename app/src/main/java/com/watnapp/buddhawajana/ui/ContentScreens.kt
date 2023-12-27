package com.watnapp.buddhawajana.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watnapp.buddhawajana.R

@Composable
fun BooksScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
//            .background(colorResource(id = R.color.teal_700))
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = "Books View",
            fontWeight = FontWeight.Bold,
//            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            fontSize = 25.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BooksScreenPreview() {
    BooksScreen()
}


@Preview(showBackground = true)
@Composable
fun AudioScreenPreview() {
    AudioScreen(windowSize = WindowSize.Expanded)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeScreen() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@BuddhawajanaReal")))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title =
                { Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "logo",
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(30.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = stringResource(R.string.app_name), fontSize = 18.sp)
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(id = R.color.topbar_bg))
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@BuddhawajanaReal")))
                    }
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.real),
                    contentDescription = "Real",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.buddhawajana_real),
                    fontWeight = FontWeight.Bold,
//                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                    fontSize = 50.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Buddhawajana Real",
                    fontWeight = FontWeight.Bold,
//                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                    fontSize = 25.sp
                )
            }
        }
    )


}

@Preview(showBackground = true)
@Composable
fun YoutubeScreenPreview() {
    YoutubeScreen()
}