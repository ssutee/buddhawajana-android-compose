package com.touchsi.buddhawajana.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.touchsi.buddhawajana.R
import com.touchsi.buddhawajana.entity.AlbumEntity
import com.touchsi.buddhawajana.entity.AudioEntity
import com.touchsi.buddhawajana.vm.AlbumViewModel
import com.touchsi.buddhawajana.vm.AudioViewModel
import com.touchsi.buddhawajana.vm.DownloadableViewModel.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

fun play(context: Context, index: Int, albumId: Long) {
    context.startActivity(Intent(context, Mp3PlayerActivity::class.java).apply {
        putExtra("ALBUM_ID", albumId)
        putExtra("START_INDEX", index)
    })
}

fun downloadAll(
    playlist: List<AudioEntity>,
    vm: AudioViewModel,
    context: Context,
    onFinished: () -> Unit
): Flow<Pair<Int,AudioEntity>> = flow {
    playlist.forEachIndexed { index,audio ->
        if (audio.status == Status.IDLE.value) {
            vm.downloadFileFlow(audio, context).collect {
                emit(Pair(index, audio))
            }
        }
    }
    onFinished.invoke()
}

@Composable
fun AudioScreen(
    albumViewModel: AlbumViewModel = koinViewModel(),
    audioViewModel: AudioViewModel = koinViewModel(),
    windowSize: WindowSize
) {
    val selected by albumViewModel.selectedId.collectAsState()
    var selectedAlbum by remember { mutableStateOf(AlbumEntity()) }
    var isPlaylistOpened by remember { mutableStateOf(false) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadingProgress by remember { mutableStateOf(0) }
    var downloadingTitle by remember { mutableStateOf("") }
    var totalDownloading by remember { mutableStateOf(0) }
    var currentDownloading by remember { mutableStateOf(0) }

    val albums by albumViewModel.getItems().collectAsState(initial = emptyList())
    val playlist by audioViewModel.getItems(selected).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val isExpanded = windowSize == WindowSize.Expanded

    fun handleDownloadFile(audio: AudioEntity) {
        isDownloading = true
        totalDownloading = 1
        currentDownloading = 1
        downloadingProgress = 0
        downloadingTitle = audio.title
        scope.launch(Dispatchers.IO) {
            audioViewModel.downloadFileFlow(audio, context).collect {
                downloadingProgress = audio.progress
            }
            isDownloading = false
        }
    }

    fun handleAudioItemSelected(index: Int) {
        audioViewModel.selectedIndex.update { index }
    }

    fun handleAlbumItemSelected(index: Int, album: AlbumEntity) {
        albumViewModel.selectedIndex.update { index }
        albumViewModel.selectedId.update { album.albumId }
        audioViewModel.selectedIndex.update { -1 }
        selectedAlbum = album
        isPlaylistOpened = true
        if (isExpanded) {
            scope.launch(Dispatchers.IO) {
                audioViewModel.refresh(owner, selected)
            }
        }
    }

    Scaffold(
        content = { padding -> Box(
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading) {
                DownloadingScreen(
                    currentDownloading,
                    totalDownloading,
                    downloadingProgress,
                    downloadingTitle
                )
            } else if (isExpanded) {
                AlbumWithPlaylistScreen(
                    albumViewModel = albumViewModel,
                    audioViewModel = audioViewModel,
                    albums = albums,
                    albumId = selected,
                    onAlbumItemSelected = { index, album ->
                        handleAlbumItemSelected(index, album)
                    },
                    onAudioItemSelected = { index, _ ->
                        handleAudioItemSelected(index)
                    },
                    onDownloadSelected = { audio ->
                        handleDownloadFile(audio)
                    }
                )
            } else if (isPlaylistOpened) {
                PlaylistScreen(audioViewModel, selected, playlist, {
                    isPlaylistOpened = false
                    audioViewModel.selectedIndex.update { -1 }
                }, { index, _ ->
                    handleAudioItemSelected(index)
                }) { audio ->
                    handleDownloadFile(audio)
                }
            } else {
                AlbumScreen(albumViewModel, albums) { index, album ->
                    handleAlbumItemSelected(index, album)
                }
            }
        }},
        topBar = {
            AudioTopBar(
                if (isPlaylistOpened)
                    selectedAlbum.title
                else
                    stringResource(R.string.app_name),
                isPlaylistOpened,
                isDownloading,
            ) {
                isDownloading = true
                totalDownloading = playlist.count()
                scope.launch(Dispatchers.IO) {
                    downloadAll(playlist, audioViewModel, context) {
                        isDownloading = false
                    }.collect { pair ->
                        currentDownloading = pair.first+1
                        downloadingTitle = pair.second.title
                        downloadingProgress = pair.second.progress
                    }
                }
            }
        }
    )
}

@Composable
private fun DownloadingScreen(
    currentDownloading: Int,
    totalDownloading: Int,
    downloadingProgress: Int,
    downloadingTitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text("${currentDownloading}/${totalDownloading}")
                CircularProgressIndicator(
                    progress = downloadingProgress.toFloat() / 100,
                    modifier = Modifier.size(96.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                downloadingTitle,
                modifier = Modifier.padding(48.dp, 0.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaylistScreen(
    vm: AudioViewModel,
    albumId: Long,
    playlist: List<AudioEntity>,
    onBackPressed:() -> Unit,
    onItemSelected: (index: Int, audio: AudioEntity) -> Unit,
    onDownloadSelected: (audio: AudioEntity) -> Unit
) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        if (albumId > 0L) {
            vm.refresh(owner, albumId)
        }
    }

    Scaffold(
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AudioList(vm, albumId, playlist, onItemSelected, onDownloadSelected)
            }
            BackHandler {
                onBackPressed.invoke()
            }
        }
    )
}

@Composable
fun OverflowMenu(onActionSelected: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = {
        showMenu = !showMenu
    }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.more),
        )
    }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = {Text(stringResource(R.string.download_all))},
            onClick = {
                showMenu = false
                onActionSelected.invoke()
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioTopBar(
    title: String, showMenu: Boolean,
    isDownloading: Boolean,
    onActionSelected: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "logo",
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(30.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(id = R.color.topbar_bg)),
        actions = {
            if (showMenu && !isDownloading) {
                OverflowMenu(onActionSelected)
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@Composable
fun AudioList(
    vm: AudioViewModel,
    albumId: Long,
    playlist: List<AudioEntity>,
    onItemSelected: (index: Int, audio: AudioEntity) -> Unit,
    onDownloadSelected: (audio: AudioEntity) -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val selected by vm.selectedId.collectAsState()
    val selectedIndex by vm.selectedIndex.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()
    fun refresh() = scope.launch(Dispatchers.IO) { vm.refresh(owner, albumId) }
    val state = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = ::refresh)
    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxSize()) {
        Box(
            Modifier
                .pullRefresh(state)
                .fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                state = rememberForeverLazyListState("playlist:$selected")
            ) {
                itemsIndexed(playlist) {index, audio ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (selectedIndex == index)
                                    Color.LightGray
                                else
                                    Color.White
                            )
                            .clickable {
                                vm.selectedId.update { audio.audioId }
                                onItemSelected.invoke(index, audio)
                            }
                            .height(60.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(
                                if (audio.status == Status.FINISHED.value)
                                    R.drawable.download_flat
                                else
                                    R.drawable.cloud
                            ),
                            contentDescription = audio.title,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${index+1}.) ${audio.title}",
                            fontSize = 16.sp,
                            color = if (selectedIndex == index && audio.status != Status.FINISHED.value)
                                Color.White
                            else if (audio.status == Status.FINISHED.value)
                                Color.Black
                            else
                                Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        DropdownMenu(
                            expanded = audio.audioId == selected,
                            onDismissRequest = {
                                vm.selectedId.update { 0L }
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.play_audio))},
                                onClick = {
                                    vm.selectedId.update { 0L }
                                    play(context, index, albumId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.download))},
                                onClick = {
                                    vm.selectedId.update { 0L }
                                    onDownloadSelected.invoke(audio)
                                }
                            )
                        }
                    }
                    Divider()
                }
            }
            PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
fun AlbumScreen(
    vm: AlbumViewModel,
    albums: List<AlbumEntity>,
    modifier: Modifier = Modifier,
    onItemSelected: (index: Int, album: AlbumEntity) -> Unit
) {
    val owner = LocalLifecycleOwner.current
    val firstLoading by vm.firstLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (firstLoading) {
            vm.refresh(owner)
            vm.firstLoading.update { false }
        }
    }
    Scaffold(
        content = { padding ->
            Box(modifier = modifier.padding(padding)) {
                AlbumList(vm, albums, modifier, onItemSelected)
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlbumList(
    vm: AlbumViewModel,
    albums: List<AlbumEntity>,
    modifier: Modifier = Modifier,
    onItemSelected: (index: Int, album: AlbumEntity) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val isRefreshing by vm.isRefreshing.collectAsState()
    val refreshScope = rememberCoroutineScope()
    fun refresh() = refreshScope.launch(Dispatchers.IO) { vm.refresh(owner) }
    val state = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = ::refresh)
    Column(modifier = modifier.padding(8.dp)) {
        Box(Modifier.pullRefresh(state)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight(),
                state = rememberForeverLazyListState("album")
            ) {
                itemsIndexed(albums) {index, album ->
                    AlbumItem(vm, index, album,  onItemSelected)
                }
            }
            PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
fun AlbumItem(
    vm: AlbumViewModel,
    index: Int,
    album: AlbumEntity,
    onItemSelected: (index: Int, album: AlbumEntity) -> Unit
) {
    val selectedIndex by vm.selectedIndex.collectAsState()
    Column(
        modifier = Modifier
            .background(
                if (selectedIndex == index)
                    Color.LightGray
                else
                    Color.White
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onItemSelected.invoke(index, album) },
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.title,
                    contentScale = ContentScale.FillWidth
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 16.dp, 0.dp)
            ) {
                Text(
                    album.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.view_count, album.viewCount),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Divider()
    }
}

@Composable
fun AlbumWithPlaylistScreen(
    albumViewModel: AlbumViewModel,
    audioViewModel: AudioViewModel,
    albums: List<AlbumEntity>,
    albumId: Long,
    onAlbumItemSelected: (index: Int, album: AlbumEntity) -> Unit,
    onAudioItemSelected: (index: Int, audio: AudioEntity) -> Unit,
    onDownloadSelected: (audio: AudioEntity) -> Unit
) {
    val owner = LocalLifecycleOwner.current
    val firstLoading by albumViewModel.firstLoading.collectAsState()
    val playlist by audioViewModel.getItems(albumId).collectAsState(initial = emptyList())

    Log.d("AlbumWithPlaylistScreen", "$albumId")

    LaunchedEffect(Unit) {
        if (firstLoading) {
            albumViewModel.refresh(owner)
            albumViewModel.firstLoading.update { false }
        }
    }

    Scaffold(
        content = { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AlbumList(
                    vm = albumViewModel,
                    albums = albums,
                    modifier = Modifier.width(334.dp),
                    onItemSelected = onAlbumItemSelected)
                AudioList(
                    vm = audioViewModel,
                    albumId = albumId,
                    playlist = playlist,
                    onItemSelected = onAudioItemSelected,
                    onDownloadSelected = onDownloadSelected
                )
            }
        }
    )
}

/**
 * Static field, contains all scroll values
 */
private val SaveMap = mutableMapOf<String, KeyParams>()

private data class KeyParams(
    val params: String = "",
    val index: Int,
    val scrollOffset: Int
)

/**
 * Save scroll state on all time.
 * @param key value for comparing screen
 * @param params arguments for find different between equals screen
 * @param initialFirstVisibleItemIndex see [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset see [LazyListState.firstVisibleItemScrollOffset]
 */
@Composable
fun rememberForeverLazyListState(
    key: String,
    params: String = "",
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyListState {
    val scrollState = rememberSaveable(saver = LazyListState.Saver) {
        var savedValue = SaveMap[key]
        if (savedValue?.params != params) savedValue = null
        val savedIndex = savedValue?.index ?: initialFirstVisibleItemIndex
        val savedOffset = savedValue?.scrollOffset ?: initialFirstVisibleItemScrollOffset
        LazyListState(
            savedIndex,
            savedOffset
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            val lastIndex = scrollState.firstVisibleItemIndex
            val lastOffset = scrollState.firstVisibleItemScrollOffset
            SaveMap[key] = KeyParams(params, lastIndex, lastOffset)
        }
    }
    return scrollState
}