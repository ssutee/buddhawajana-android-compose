package com.watnapp.buddhawajana.feature.books.reader

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.watnapp.buddhawajana.core.model.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    vm: ReaderViewModel,
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val page by vm.page.collectAsState()
    val bookmarks by vm.bookmarksFor.collectAsState(initial = emptyList())
    val bookmarkedPages by vm.bookmarkedPages.collectAsState()

    var chrome by remember { mutableStateOf(true) }
    var showBookmarkSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (chrome) {
                ReaderTopBar(
                    title = when (val s = state) {
                        is ReaderState.Ready -> "หน้า ${page + 1} / ${s.pageCount}"
                        else -> "กำลังโหลด…"
                    },
                    onBack = onBack,
                    isBookmarked = page in bookmarkedPages,
                    onToggleBookmark = { vm.toggleBookmark(page) },
                    onShowBookmarks = { showBookmarkSheet = true },
                    onShare = onShare,
                )
            }
        },
        bottomBar = {
            if (chrome && state is ReaderState.Ready) {
                val ready = state as ReaderState.Ready
                ReaderBottomBar(
                    currentPage = page,
                    pageCount = ready.pageCount,
                    onScrollToPage = { vm.onPageChanged(it) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                is ReaderState.Loading -> CircularProgressIndicator()
                is ReaderState.Downloading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("กำลังดาวน์โหลด…", style = MaterialTheme.typography.bodyLarge)
                        LinearProgressIndicator(progress = { s.fraction }, modifier = Modifier.fillMaxWidth(0.7f))
                    }
                }
                is ReaderState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(s.message, style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = onBack) { Text("กลับ") }
                    }
                }
                is ReaderState.Ready -> {
                    PdfPager(
                        ready = s,
                        currentPage = page,
                        onPageChanged = { vm.onPageChanged(it) },
                        onToggleChrome = { chrome = !chrome },
                    )
                }
            }
        }
    }

    if (showBookmarkSheet) {
        BookmarkSheet(
            bookmarks = bookmarks,
            onDismiss = { showBookmarkSheet = false },
            onJump = { bm ->
                vm.onPageChanged(bm.page)
                showBookmarkSheet = false
            },
            onDelete = { vm.deleteBookmark(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    onBack: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShare: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "กลับ")
            }
        },
        actions = {
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "ลบบุ๊กมาร์กหน้านี้" else "บุ๊กมาร์กหน้านี้",
                )
            }
            IconButton(onClick = onShowBookmarks) {
                Icon(Icons.Default.Bookmarks, contentDescription = "รายการบุ๊กมาร์ก")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "แชร์")
            }
        },
    )
}

@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    pageCount: Int,
    onScrollToPage: (Int) -> Unit,
) {
    var showGotoDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(currentPage.toFloat()) }
    LaunchedEffect(currentPage) { draft = currentPage.toFloat() }

    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Slider(
                value = draft,
                onValueChange = { draft = it },
                onValueChangeFinished = { onScrollToPage(draft.roundToInt()) },
                valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { if (currentPage > 0) onScrollToPage(currentPage - 1) },
                    enabled = currentPage > 0,
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "หน้าก่อน")
                }
                TextButton(onClick = { showGotoDialog = true }) {
                    Text("หน้า ${currentPage + 1} / $pageCount")
                }
                IconButton(
                    onClick = { if (currentPage < pageCount - 1) onScrollToPage(currentPage + 1) },
                    enabled = currentPage < pageCount - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "หน้าถัดไป")
                }
            }
        }
    }

    if (showGotoDialog) {
        GotoPageDialog(
            currentPage = currentPage,
            pageCount = pageCount,
            onConfirm = { target ->
                onScrollToPage(target)
                showGotoDialog = false
            },
            onDismiss = { showGotoDialog = false },
        )
    }
}

@Composable
private fun GotoPageDialog(
    currentPage: Int,
    pageCount: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("${currentPage + 1}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ไปที่หน้า") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                label = { Text("หมายเลขหน้า (1–$pageCount)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val page = text.toIntOrNull()?.minus(1)
                    if (page != null && page in 0 until pageCount) onConfirm(page)
                }),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val page = text.toIntOrNull()?.minus(1)
                if (page != null && page in 0 until pageCount) onConfirm(page)
            }) { Text("ไป") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ยกเลิก") }
        },
    )
}

@Composable
private fun PdfPager(
    ready: ReaderState.Ready,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onToggleChrome: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = ready.startPage) { ready.pageCount }

    // Persist page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { onPageChanged(it) }
    }

    // Sync external page changes (prev/next buttons, bookmarks) into pager
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.scrollToPage(currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        PdfPage(
            pdf = ready.pdf,
            index = index,
            onToggleChrome = onToggleChrome,
        )
    }
}

@Composable
private fun PdfPage(
    pdf: PdfHandle,
    index: Int,
    onToggleChrome: () -> Unit,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val widthPx = remember(configuration.screenWidthDp) {
        with(density) {
            minOf((configuration.screenWidthDp.dp.toPx()).toInt() * 2, 2048)
        }
    }

    val bitmap by produceState<android.graphics.Bitmap?>(null, index, widthPx) {
        value = withContext(Dispatchers.IO) { pdf.renderPage(index, widthPx) }
    }

    var scale by remember(index) { mutableFloatStateOf(1f) }
    var offsetX by remember(index) { mutableFloatStateOf(0f) }
    var offsetY by remember(index) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(index) {
                detectTapGestures(
                    onTap = { onToggleChrome() },
                )
            }
            .pointerInput(index) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "หน้า ${index + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkSheet(
    bookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onJump: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("ยังไม่มีบุ๊กมาร์ก", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(bookmarks, key = { it.id }) { bm ->
                    ListItem(
                        headlineContent = { Text(bm.note) },
                        supportingContent = { Text("หน้า ${bm.page + 1}") },
                        trailingContent = {
                            IconButton(onClick = { onDelete(bm) }) {
                                Icon(Icons.Default.Delete, contentDescription = "ลบบุ๊กมาร์ก")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(bm.id) {
                                detectTapGestures(onTap = { onJump(bm) })
                            },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
