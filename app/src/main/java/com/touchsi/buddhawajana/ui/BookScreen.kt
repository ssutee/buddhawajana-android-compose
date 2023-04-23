package com.touchsi.buddhawajana.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.ireward.htmlcompose.HtmlText
import com.rajat.pdfviewer.PdfViewerActivity
import com.touchsi.buddhawajana.R
import com.touchsi.buddhawajana.entity.BookEntity
import com.touchsi.buddhawajana.entity.getFile
import com.touchsi.buddhawajana.vm.BookViewModel
import com.touchsi.buddhawajana.vm.DownloadableViewModel.Status
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@Composable
fun BookScreen(vm: BookViewModel = koinViewModel()) {
    val owner = LocalLifecycleOwner.current
    val books by vm.getItems().collectAsState(initial = emptyList())
    val firstLoading by vm.firstLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (firstLoading) {
            vm.refresh(owner)
            vm.firstLoading.update { false }
        }
    }

    Scaffold(
        topBar = { BookTopBar() },
        content = {padding ->
            Box(modifier = Modifier.padding(padding)) {
                BookList(vm, books, owner)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookTopBar() {
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
}

private fun openPDF(book: BookEntity, context: Context) {
    val pdfFile = book.getFile(context)
    val pdfUri = FileProvider
        .getUriForFile(context, "com.touchsi.buddhawajana.provider", pdfFile)
    val pdfOpenIntent = Intent(Intent.ACTION_VIEW)
    pdfOpenIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    pdfOpenIntent.clipData = ClipData.newRawUri("", pdfUri)
    pdfOpenIntent.setDataAndType(pdfUri, "application/pdf")
    pdfOpenIntent.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    try {
        context.startActivity(pdfOpenIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            PdfViewerActivity.launchPdfFromPath(
                context,
                pdfUri.toString(),
                book.title,
                "",
                enableDownload = false
            )
        )
    }
}

private fun handleClickItem(vm: BookViewModel, book: BookEntity, context: Context) {
    val bookFile = book.getFile(context)
    if (bookFile.exists() && Status[book.status] == Status.FINISHED) {
        openPDF(book, context)
    } else if (!bookFile.exists() || Status[book.status] != Status.DOWNLOADING) {
        vm.downloadFile(book, context)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class,
    ExperimentalMaterialApi::class
)
@Composable
private fun BookList(vm: BookViewModel, books: List<BookEntity>, owner: LifecycleOwner) {
    var selected by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    val isRefreshing by vm.isRefreshing.collectAsState()
    val refreshScope = rememberCoroutineScope()
    fun refresh() = refreshScope.launch { vm.refresh(owner) }
    val state = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = ::refresh)
    Column(modifier = Modifier.padding(8.dp)) {
        Box(Modifier.pullRefresh(state)) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(books) { book ->
                    Column {
                        Row(
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        handleClickItem(vm, book, context)
                                    },
                                    onLongClick = {
                                        selected = book.bookId
                                    }
                                )
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center                             
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(book.coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    placeholder = painterResource(R.drawable.book_placeholder),
                                    contentDescription = book.title,
                                    contentScale = ContentScale.FillWidth
                                )
                                if (book.progress != 100 && book.progress != 0) {
                                    CircularProgressIndicator(book.progress.toFloat() / 100)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp, 0.dp, 16.dp, 0.dp)
                            ) {
                                Text(
                                    book.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                HtmlText(
                                    Html.fromHtml(book.detail, Html.FROM_HTML_MODE_COMPACT)
                                        .toString().trim(),
                                    fontSize = 12.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Clip
                                )
                                if (book.progress != 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "(${stringResource(R.string.downloaded)} ${book.progress}%)",
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                                DropdownMenu(
                                    expanded = book.bookId == selected,
                                    onDismissRequest = { selected = 0L }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.download_again))},
                                        onClick = {
                                            vm.downloadFile(book, context)
                                            selected = 0
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        onClick = {
                                            vm.deleteFile(book, context)
                                            selected = 0
                                        }
                                    )
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
            PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
        }
    }
}

