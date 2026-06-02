# Downloads Vertical Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Download audio for offline listening — per-track (player + list rows) and per-album batch — via WorkManager, browse in a Downloads screen, with offline playback preferring the local file.

**Architecture:** A `DownloadWorker` (CoroutineWorker, deps via Koin GlobalContext) streams to an `AudioFileStore` file and writes a denormalized `DownloadEntity` on success. `DownloadRepository` merges the persistent entity with live WorkManager `WorkInfo` into one `DownloadState` (via a pure `mapDownloadState`). `MediaPlaybackController` plays the local file when present. UI lives in `:feature:audio`.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), AndroidX WorkManager 2.9.1, Room (KSP) + migration, Media3, Koin, Coroutines/Flow, OkHttp (reuses `FileDownloader`). JVM tests JUnit4 + Turbine + MockK.

**Conventions:** Package root `com.watnapp.buddhawajana`. All commits end with:
`Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`. WorkManager/Worker/foreground/MediaController are **device-verified**, not unit-tested; pure logic (`mapDownloadState`), VMs, mappers, screens-VM are unit-tested.

---

## File Structure

**New:** `core/model/Download.kt`; `core/data/download/{AudioFileStore,DownloadEntity,DownloadDao,DownloadState,DownloadWorker}.kt`, `core/data/repo/DownloadRepository.kt`; `feature/audio/download/DownloadButton.kt`, `feature/audio/downloads/{DownloadsViewModel,DownloadsScreen}.kt`; tests.

**Modified:** `core/data/build.gradle.kts`, `db/AppDatabase.kt` (v4), `DataModule.kt`, `mapper/Mappers.kt`; `core/player/PlaybackController.kt` (NowPlaying.isLocal), `MediaPlaybackController.kt` (local uri), `PlayerModule.kt`; `feature/audio` PlayerVM/Screen, AudioListVM/Screen, AlbumsVM/Screen, AudioPane, AudioModule; `app` Manifest + MainApplication.

---

## Task DT1: `:core:model` — Download

**Files:** Create `core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/Download.kt`

- [ ] **Step 1: Create model**
```kotlin
package com.watnapp.buddhawajana.core.model

data class Download(
    val audioId: String,
    val title: String,
    val url: String,
    val albumId: String,
    val albumTitle: String,
    val coverUrl: String?,
    val sizeBytes: Long,
    val completedAt: Long,
)
```

- [ ] **Step 2: Compile** — `./gradlew :core:model:compileReleaseKotlin -q` → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add core/model
git commit -m "feat(core:model): Download

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT2: `:core:data` — file store, entity/dao, DownloadState, mapDownloadState (TDD), mapper, WorkManager dep

**Files:**
- Modify `core/data/build.gradle.kts`
- Create `core/data/src/main/java/com/watnapp/buddhawajana/core/data/download/AudioFileStore.kt`
- Create `.../download/DownloadEntity.kt`, `.../download/DownloadDao.kt`, `.../download/DownloadState.kt`
- Modify `.../mapper/Mappers.kt`
- Test `core/data/src/test/java/com/watnapp/buddhawajana/core/data/download/DownloadStateTest.kt`

- [ ] **Step 1: Add WorkManager dep** — in `core/data/build.gradle.kts` `dependencies {}`, after `implementation(libs.okhttp)`:
```kotlin
    implementation(libs.work.runtime.ktx)
```

- [ ] **Step 2: Write the failing pure-mapper test** `DownloadStateTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.download

import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStateTest {
    @Test fun `downloaded when row and file present`() =
        assertEquals(DownloadState.Downloaded, mapDownloadState(true, true, WorkInfo.State.RUNNING, 0.5f))

    @Test fun `row without file falls through to work state`() =
        assertEquals(DownloadState.Downloading(0.5f), mapDownloadState(true, false, WorkInfo.State.RUNNING, 0.5f))

    @Test fun `enqueued is Queued`() =
        assertEquals(DownloadState.Queued, mapDownloadState(false, false, WorkInfo.State.ENQUEUED, 0f))

    @Test fun `running is Downloading with fraction`() =
        assertEquals(DownloadState.Downloading(0.3f), mapDownloadState(false, false, WorkInfo.State.RUNNING, 0.3f))

    @Test fun `failed is Failed`() =
        assertEquals(DownloadState.Failed, mapDownloadState(false, false, WorkInfo.State.FAILED, 0f))

    @Test fun `no work and no file is NotDownloaded`() =
        assertEquals(DownloadState.NotDownloaded, mapDownloadState(false, false, null, 0f))

    @Test fun `succeeded work but no row yet is NotDownloaded`() =
        assertEquals(DownloadState.NotDownloaded, mapDownloadState(false, false, WorkInfo.State.SUCCEEDED, 0f))
}
```

- [ ] **Step 3: Run, verify FAIL** — `./gradlew :core:data:testDebugUnitTest --tests "*DownloadStateTest*" -q` → FAIL (unresolved).

- [ ] **Step 4: Create DownloadState.kt (state + pure mapper)**
```kotlin
package com.watnapp.buddhawajana.core.data.download

import androidx.work.WorkInfo

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data object Queued : DownloadState
    data class Downloading(val fraction: Float) : DownloadState
    data object Downloaded : DownloadState
    data object Failed : DownloadState
}

/** Merge persistent (entity+file) and transient (WorkInfo) signals into one state. Pure. */
fun mapDownloadState(
    downloaded: Boolean,
    fileExists: Boolean,
    infoState: WorkInfo.State?,
    fraction: Float,
): DownloadState =
    if (downloaded && fileExists) DownloadState.Downloaded
    else when (infoState) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Queued
        WorkInfo.State.RUNNING -> DownloadState.Downloading(fraction)
        WorkInfo.State.FAILED -> DownloadState.Failed
        else -> DownloadState.NotDownloaded
    }
```

- [ ] **Step 5: Create AudioFileStore.kt**
```kotlin
package com.watnapp.buddhawajana.core.data.download

import android.content.Context
import android.os.Environment
import java.io.File

class AudioFileStore(private val context: Context) {
    private fun dir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "buddhawajana/audios").apply { mkdirs() }
    fun file(audioId: String): File = File(dir(), "$audioId.mp3")
    fun exists(audioId: String): Boolean = file(audioId).let { it.exists() && it.length() > 0 }
    fun delete(audioId: String): Boolean = file(audioId).delete()
}
```

- [ ] **Step 6: Create DownloadEntity.kt**
```kotlin
package com.watnapp.buddhawajana.core.data.download

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download")
data class DownloadEntity(
    @PrimaryKey @ColumnInfo(name = "audio_id") val audioId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "album_id") val albumId: String,
    @ColumnInfo(name = "album_title") val albumTitle: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
)
```

- [ ] **Step 7: Create DownloadDao.kt**
```kotlin
package com.watnapp.buddhawajana.core.data.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download ORDER BY completed_at DESC")
    fun stream(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download WHERE audio_id = :id")
    suspend fun get(id: String): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM download WHERE audio_id = :id)")
    fun existsFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM download WHERE audio_id = :id)")
    suspend fun existsOnce(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: DownloadEntity)

    @Query("DELETE FROM download WHERE audio_id = :id")
    suspend fun deleteById(id: String)
}
```

- [ ] **Step 8: Add mapper** — append to `Mappers.kt` (imports `com.watnapp.buddhawajana.core.data.download.DownloadEntity`, `com.watnapp.buddhawajana.core.model.Download`):
```kotlin
// ---- Download ----

fun DownloadEntity.toModel() = Download(audioId, title, url, albumId, albumTitle, coverUrl, sizeBytes, completedAt)
```

- [ ] **Step 9: Run, verify PASS** — `./gradlew :core:data:testDebugUnitTest --tests "*DownloadStateTest*" -q` → PASS. Then `./gradlew :core:data:compileDebugKotlin -q` → BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**
```bash
git add core/data
git commit -m "feat(core:data): AudioFileStore + Download entity/dao + DownloadState mapper (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT3: `:core:data` — DownloadWorker + DownloadRepository (device-verified)

**Files:**
- Create `core/data/src/main/java/com/watnapp/buddhawajana/core/data/download/DownloadWorker.kt`
- Create `core/data/src/main/java/com/watnapp/buddhawajana/core/data/repo/DownloadRepository.kt`

- [ ] **Step 1: Create DownloadWorker.kt**
```kotlin
package com.watnapp.buddhawajana.core.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import okhttp3.OkHttpClient
import org.koin.core.context.GlobalContext
import kotlin.coroutines.cancellation.CancellationException

/** Streams one audio to AudioFileStore and records a DownloadEntity on success. */
class DownloadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val audioId = inputData.getString(KEY_AUDIO_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val koin = GlobalContext.get()
        val client = koin.get<OkHttpClient>()
        val dao = koin.get<DownloadDao>()
        val files = koin.get<AudioFileStore>()
        val title = inputData.getString(KEY_TITLE) ?: ""
        return try {
            setForeground(foregroundInfo(audioId, title))
            FileDownloader(client).download(url, files.file(audioId)).collect { p ->
                when (p) {
                    is DownloadProgress.Progress -> setProgress(workDataOf(KEY_FRACTION to p.fraction))
                    is DownloadProgress.Failed -> throw p.error
                    DownloadProgress.Done -> {}
                }
            }
            val f = files.file(audioId)
            dao.insert(
                DownloadEntity(
                    audioId = audioId, title = title, url = url,
                    albumId = inputData.getString(KEY_ALBUM_ID) ?: "",
                    albumTitle = inputData.getString(KEY_ALBUM_TITLE) ?: "",
                    coverUrl = inputData.getString(KEY_COVER),
                    fileName = f.name, sizeBytes = f.length(),
                    completedAt = System.currentTimeMillis(),
                ),
            )
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun foregroundInfo(audioId: String, title: String): ForegroundInfo {
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setContentTitle("กำลังดาวน์โหลด")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        val id = audioId.hashCode()
        return if (Build.VERSION.SDK_INT >= 34) {
            ForegroundInfo(id, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notif)
        }
    }

    companion object {
        const val CHANNEL = "downloads"
        const val KEY_AUDIO_ID = "audioId"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_ALBUM_ID = "albumId"
        const val KEY_ALBUM_TITLE = "albumTitle"
        const val KEY_COVER = "cover"
        const val KEY_FRACTION = "fraction"

        fun inputData(audio: Audio, album: Album) = workDataOf(
            KEY_AUDIO_ID to audio.id,
            KEY_URL to audio.url,
            KEY_TITLE to audio.title,
            KEY_ALBUM_ID to album.id,
            KEY_ALBUM_TITLE to album.title,
            KEY_COVER to album.coverUrl,
        )

        fun workName(audioId: String) = "download_$audioId"
    }
}
```

- [ ] **Step 2: Create DownloadRepository.kt**
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.watnapp.buddhawajana.core.data.download.AudioFileStore
import com.watnapp.buddhawajana.core.data.download.DownloadDao
import com.watnapp.buddhawajana.core.data.download.DownloadState
import com.watnapp.buddhawajana.core.data.download.DownloadWorker
import com.watnapp.buddhawajana.core.data.download.mapDownloadState
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Download
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
    private val files: AudioFileStore,
) {
    private val wm get() = WorkManager.getInstance(context)

    val downloads: Flow<List<Download>> = dao.stream().map { list -> list.map { it.toModel() } }

    fun enqueue(audio: Audio, album: Album) {
        val req = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(DownloadWorker.inputData(audio, album))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        wm.enqueueUniqueWork(DownloadWorker.workName(audio.id), ExistingWorkPolicy.KEEP, req)
    }

    suspend fun enqueueAll(album: Album, audios: List<Audio>) {
        audios.forEach { if (!dao.existsOnce(it.id)) enqueue(it, album) }
    }

    fun cancel(audioId: String) { wm.cancelUniqueWork(DownloadWorker.workName(audioId)) }

    suspend fun delete(audioId: String) {
        cancel(audioId)
        files.delete(audioId)
        dao.deleteById(audioId)
    }

    fun state(audioId: String): Flow<DownloadState> =
        combine(
            dao.existsFlow(audioId),
            wm.getWorkInfosForUniqueWorkFlow(DownloadWorker.workName(audioId)),
        ) { downloaded, infos ->
            val info = infos.firstOrNull()
            mapDownloadState(
                downloaded = downloaded,
                fileExists = files.exists(audioId),
                infoState = info?.state,
                fraction = info?.progress?.getFloat(DownloadWorker.KEY_FRACTION, 0f) ?: 0f,
            )
        }
}
```

- [ ] **Step 3: Compile** — `./gradlew :core:data:compileDebugKotlin -q` → BUILD SUCCESSFUL. (If `NotificationCompat` unresolved, confirm `androidx.core:core-ktx` is on the convention plugin's classpath — it is for android libraries; report if not.)

- [ ] **Step 4: Commit**
```bash
git add core/data
git commit -m "feat(core:data): DownloadWorker + DownloadRepository (WorkManager engine)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT4: `:core:data` — DB v4 + DI

**Files:** Modify `core/data/.../db/AppDatabase.kt`, `core/data/.../DataModule.kt`

- [ ] **Step 1: AppDatabase v4** — change `version = 3` → `version = 4`; add `DownloadEntity::class` to entities (import `com.watnapp.buddhawajana.core.data.download.DownloadEntity`); add `abstract fun downloadDao(): com.watnapp.buddhawajana.core.data.download.DownloadDao`; add in companion:
```kotlin
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS download (" +
                        "audio_id TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, url TEXT NOT NULL, " +
                        "album_id TEXT NOT NULL, album_title TEXT NOT NULL, cover_url TEXT, " +
                        "file_name TEXT NOT NULL, size_bytes INTEGER NOT NULL, completed_at INTEGER NOT NULL)"
                )
            }
        }
```

- [ ] **Step 2: DataModule** — change `.addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)` to `.addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)`; add singles:
```kotlin
    single { get<AppDatabase>().downloadDao() }
    single { com.watnapp.buddhawajana.core.data.download.AudioFileStore(androidContext()) }
    single { com.watnapp.buddhawajana.core.data.repo.DownloadRepository(androidContext(), get(), get()) }
```

- [ ] **Step 3: Compile (KSP)** — `./gradlew :core:data:assembleDebug -q` → BUILD SUCCESSFUL. Then `./gradlew :core:data:testDebugUnitTest -q` → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add core/data
git commit -m "feat(core:data): DB v4 download table migration + DI

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT5: `:core:player` — offline playback (local file + NowPlaying.isLocal)

**Files:** Modify `core/player/.../PlaybackController.kt`, `core/player/.../MediaPlaybackController.kt`, `core/player/.../PlayerModule.kt`, and the test fixture in `feature/audio/.../player/PlayerViewModelTest.kt`.

- [ ] **Step 1: NowPlaying.isLocal** — in `PlaybackController.kt`, add `val isLocal: Boolean` as the last field of `NowPlaying`:
```kotlin
data class NowPlaying(
    val audioId: String,
    val albumId: String,
    val title: String,
    val album: String,
    val artworkUrl: String?,
    val url: String,
    val isLocal: Boolean,
)
```

- [ ] **Step 2: MediaPlaybackController** —
  (a) add ctor param `private val localFile: (audioId: String) -> java.io.File?` (after `prefs`).
  (b) in `setQueue`, replace `.setUri(Uri.parse(audio.url))` with:
```kotlin
                .setUri(localFile(audio.id)?.let(Uri::fromFile) ?: Uri.parse(audio.url))
```
  (c) in `updateNowPlaying()`, add `isLocal` to the `NowPlaying(...)` construction:
```kotlin
            url = item.localConfiguration?.uri?.toString() ?: "",
            isLocal = item.localConfiguration?.uri?.scheme == "file",
```

- [ ] **Step 3: PlayerModule** — build the controller with the resolver:
```kotlin
val playerModule = module {
    single { PlaybackPrefs(androidContext()) }
    single<PlaybackController> {
        MediaPlaybackController(
            androidContext(), get(), get(),
            localFile = { id ->
                val files = get<com.watnapp.buddhawajana.core.data.download.AudioFileStore>()
                if (files.exists(id)) files.file(id) else null
            },
        )
    }
}
```

- [ ] **Step 4: Fix the NowPlaying test fixture** — in `feature/audio/.../player/PlayerViewModelTest.kt`, the `NowPlaying("7", "9", "Talk", "Album", "cov", "http://x/7.mp3")` construction now needs the `isLocal` arg. Change it to:
```kotlin
        val now = NowPlaying("7", "9", "Talk", "Album", "cov", "http://x/7.mp3", isLocal = false)
```

- [ ] **Step 5: Compile + existing player tests** — `./gradlew :core:player:assembleDebug -q` → SUCCESSFUL; `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q` → PASS.

- [ ] **Step 6: Commit**
```bash
git add core/player feature/audio
git commit -m "feat(core:player): offline playback prefers local file; NowPlaying.isLocal

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT6: `:feature:audio` — DownloadButton + player download action (TDD)

**Files:**
- Create `feature/audio/.../download/DownloadButton.kt`
- Modify `feature/audio/.../player/PlayerViewModel.kt`, `player/PlayerScreen.kt`, `AudioModule.kt`
- Modify `feature/audio/src/test/.../player/PlayerViewModelTest.kt`

- [ ] **Step 1: Update PlayerViewModelTest** — add a DownloadRepository mock and a download-forwarding test. At the top of the class add:
```kotlin
    private val downloads: com.watnapp.buddhawajana.core.data.repo.DownloadRepository =
        io.mockk.mockk(relaxed = true) {
            io.mockk.every { state(any()) } returns kotlinx.coroutines.flow.flowOf(
                com.watnapp.buddhawajana.core.data.download.DownloadState.NotDownloaded
            )
        }
```
Change every `PlayerViewModel(c, favorites)` / `PlayerViewModel(controller(now), favorites)` construction to add `, downloads` as the 3rd arg. Add this test:
```kotlin
    @Test fun `download enqueues current track`() = runTest {
        val now = NowPlaying("7", "9", "Talk", "Album", "cov", "http://x/7.mp3", isLocal = false)
        val vm = PlayerViewModel(controller(now), favorites, downloads)
        vm.download()
        advanceUntilIdle()
        io.mockk.verify {
            downloads.enqueue(
                com.watnapp.buddhawajana.core.model.Audio("7", "9", "Talk", "http://x/7.mp3"),
                com.watnapp.buddhawajana.core.model.Album("9", "Album", "cov", 0, 0),
            )
        }
    }
```

- [ ] **Step 2: Run, verify FAIL** — `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q` → FAIL.

- [ ] **Step 3: Update PlayerViewModel** — add `downloads` ctor param + state/actions. New imports: `com.watnapp.buddhawajana.core.data.download.DownloadState`, `com.watnapp.buddhawajana.core.data.repo.DownloadRepository`, `com.watnapp.buddhawajana.core.model.Album`, `com.watnapp.buddhawajana.core.model.Audio`. Change the constructor to:
```kotlin
class PlayerViewModel(
    private val controller: PlaybackController,
    private val favorites: FavoriteRepository,
    private val downloads: DownloadRepository,
) : BaseViewModel() {
```
Add (next to `isFavorite`):
```kotlin
    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadState: StateFlow<DownloadState> =
        controller.nowPlaying.flatMapLatest { np ->
            if (np == null) flowOf(DownloadState.NotDownloaded) else downloads.state(np.audioId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadState.NotDownloaded)

    fun download() {
        val np = controller.nowPlaying.value ?: return
        if (np.url.isEmpty()) return
        downloads.enqueue(
            Audio(np.audioId, np.albumId, np.title, np.url),
            Album(np.albumId, np.album, np.artworkUrl, 0, 0),
        )
    }
    fun cancelDownload() { controller.nowPlaying.value?.let { downloads.cancel(it.audioId) } }
    fun deleteDownload() = viewModelScope.launch {
        controller.nowPlaying.value?.let { downloads.delete(it.audioId) }
    }
```
(`flowOf` import already present from `isFavorite`.)

- [ ] **Step 4: Run, verify PASS** — `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q` → PASS.

- [ ] **Step 5: Create DownloadButton.kt**
```kotlin
package com.watnapp.buddhawajana.feature.audio.download

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.data.download.DownloadState

@Composable
fun DownloadButton(
    state: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        DownloadState.NotDownloaded ->
            IconButton(onClick = onDownload, modifier = modifier) {
                Icon(Icons.Default.Download, contentDescription = "ดาวน์โหลด")
            }
        DownloadState.Queued ->
            IconButton(onClick = onCancel, modifier = modifier) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        is DownloadState.Downloading ->
            IconButton(onClick = onCancel, modifier = modifier) {
                CircularProgressIndicator(progress = { state.fraction }, modifier = Modifier.size(20.dp))
            }
        DownloadState.Downloaded ->
            IconButton(onClick = onDelete, modifier = modifier) {
                Icon(Icons.Default.CheckCircle, contentDescription = "ลบไฟล์", tint = MaterialTheme.colorScheme.primary)
            }
        DownloadState.Failed ->
            IconButton(onClick = onDownload, modifier = modifier) {
                Icon(Icons.Default.ErrorOutline, contentDescription = "ลองใหม่", tint = MaterialTheme.colorScheme.error)
            }
    }
}
```
(Add `import androidx.compose.foundation.layout.size`.)

- [ ] **Step 6: Wire DownloadButton into the player ActionRow** — in `PlayerScreen.kt`, inside `ActionRow(vm)` (which already shows the heart), collect download state and add the button after the heart `IconButton`:
```kotlin
    val downloadState by vm.downloadState.collectAsState()
```
and after the heart IconButton, before the closing of the Row:
```kotlin
        com.watnapp.buddhawajana.feature.audio.download.DownloadButton(
            state = downloadState,
            onDownload = vm::download,
            onCancel = vm::cancelDownload,
            onDelete = vm::deleteDownload,
        )
```

- [ ] **Step 7: audioModule** — change `viewModel { PlayerViewModel(get(), get()) }` to `viewModel { PlayerViewModel(get(), get(), get()) }`.

- [ ] **Step 8: Verify** — `./gradlew :feature:audio:assembleDebug -q` + `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q`. Both SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add feature/audio
git commit -m "feat(feature:audio): DownloadButton + player download action (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT7: `:feature:audio` — audio-list per-row download + batch (TDD)

**Files:**
- Modify `feature/audio/.../list/AudioListViewModel.kt`, `list/AudioListScreen.kt`, `navigation/AudioPane.kt`, `AudioModule.kt`
- Modify `feature/audio/src/test/.../list/AudioListViewModelTest.kt`

- [ ] **Step 1: Update AudioListViewModelTest** — add a DownloadRepository mock to the existing `controller()`-based tests. Add a class field:
```kotlin
    private val downloads: com.watnapp.buddhawajana.core.data.repo.DownloadRepository =
        io.mockk.mockk(relaxed = true)
```
Change both existing `AudioListViewModel("9", repo, controller())` constructions to `AudioListViewModel("9", repo, controller(), downloads)`. Add:
```kotlin
    @Test fun `downloadAll enqueues whole album`() = runTest {
        val repo: AudioRepository = mockk(relaxed = true) {
            every { stream("9") } returns flowOf(emptyList())
            coEvery { refresh("9") } returns Result.success(Unit)
        }
        val vm = AudioListViewModel("9", repo, controller(), downloads)
        val album = com.watnapp.buddhawajana.core.model.Album("9", "A", null, 0, 0)
        val list = listOf(com.watnapp.buddhawajana.core.model.Audio("1", "9", "T", "u"))
        vm.downloadAll(album, list)
        advanceUntilIdle()
        io.mockk.coVerify { downloads.enqueueAll(album, list) }
    }
```

- [ ] **Step 2: Run, verify FAIL** — `./gradlew :feature:audio:testDebugUnitTest --tests "*AudioListViewModelTest*" -q` → FAIL.

- [ ] **Step 3: Update AudioListViewModel** — add `downloads` ctor param + actions + state provider. Add import `com.watnapp.buddhawajana.core.data.download.DownloadState`, `com.watnapp.buddhawajana.core.data.repo.DownloadRepository`, `kotlinx.coroutines.flow.Flow`. Change constructor:
```kotlin
class AudioListViewModel(
    private val albumId: String,
    private val repo: AudioRepository,
    private val controller: PlaybackController,
    private val downloads: DownloadRepository,
) : BaseViewModel() {
```
Add methods (after `play`):
```kotlin
    fun downloadState(audioId: String): Flow<DownloadState> = downloads.state(audioId)
    fun download(audio: Audio, album: Album) = downloads.enqueue(audio, album)
    fun cancelDownload(audioId: String) = downloads.cancel(audioId)
    fun deleteDownload(audioId: String) = viewModelScope.launch { downloads.delete(audioId) }
    fun downloadAll(album: Album, audios: List<Audio>) = viewModelScope.launch { downloads.enqueueAll(album, audios) }
```

- [ ] **Step 4: Run, verify PASS** — `./gradlew :feature:audio:testDebugUnitTest --tests "*AudioListViewModelTest*" -q` → PASS.

- [ ] **Step 5: AudioListScreen — per-row DownloadButton + batch button.** Update the signature to accept a download-state provider, per-row actions, and a download-all action:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioListScreen(
    state: UiState<List<Audio>>,
    query: String,
    playingAudioId: String?,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onRowVisible: (Audio) -> Unit,
    onPlay: (Int) -> Unit,
    downloadStateFor: (String) -> kotlinx.coroutines.flow.Flow<com.watnapp.buddhawajana.core.data.download.DownloadState>,
    onDownload: (Audio) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onDownloadAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
```
Add a "ดาวน์โหลดทั้งหมด" `TextButton` row above the list inside the `Content` branch (before `LazyColumn`), and give each `ListItem` a `trailingContent` with a `DownloadButton`. Replace the `Content` branch body with:
```kotlin
            is UiState.Content -> Column(Modifier.fillMaxSize()) {
                androidx.compose.material3.TextButton(
                    onClick = onDownloadAll,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) { Text("ดาวน์โหลดทั้งหมด") }
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(state.data, key = { _, a -> a.id }) { index, audio ->
                        LaunchedEffect(audio.id) { onRowVisible(audio) }
                        val dl by remember(audio.id) { downloadStateFor(audio.id) }
                            .collectAsState(initial = com.watnapp.buddhawajana.core.data.download.DownloadState.NotDownloaded)
                        ListItem(
                            headlineContent = {
                                Text(
                                    audio.title,
                                    color = if (audio.id == playingAudioId) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            supportingContent = { Text(formatRowMeta(audio.durationMs, audio.sizeBytes)) },
                            trailingContent = {
                                com.watnapp.buddhawajana.feature.audio.download.DownloadButton(
                                    state = dl,
                                    onDownload = { onDownload(audio) },
                                    onCancel = { onCancelDownload(audio.id) },
                                    onDelete = { onDeleteDownload(audio.id) },
                                )
                            },
                            modifier = Modifier.clickable { onPlay(index) },
                        )
                    }
                }
            }
```
Add imports: `androidx.compose.foundation.layout.Column` (if missing), `androidx.compose.runtime.collectAsState`, `androidx.compose.runtime.getValue`, `androidx.compose.runtime.remember`.

- [ ] **Step 6: AudioPane — wire AudioListScreen.** In the `composable<AudioListRoute>` block, build the route album once and pass the new params:
```kotlin
            val routeAlbum = Album(route.albumId, route.albumTitle, route.albumCoverUrl, 0, 0)
            AudioListScreen(
                state = state,
                query = query,
                playingAudioId = now?.audioId,
                onSearch = vm::onSearch,
                onRefresh = vm::refresh,
                onRowVisible = vm::onRowVisible,
                onPlay = { index ->
                    if (audios.isNotEmpty()) {
                        vm.play(routeAlbum.copy(itemCount = audios.size), audios, index)
                        onOpenPlayer()
                    }
                },
                downloadStateFor = vm::downloadState,
                onDownload = { audio -> vm.download(audio, routeAlbum) },
                onCancelDownload = vm::cancelDownload,
                onDeleteDownload = vm::deleteDownload,
                onDownloadAll = { vm.downloadAll(routeAlbum, audios) },
            )
```
(`Album` is already imported in AudioPane. `routeAlbum.copy(itemCount = audios.size)` keeps the prior play() behavior of passing the count.)

- [ ] **Step 7: audioModule** — change `viewModel { (albumId: String) -> AudioListViewModel(albumId, get(), get()) }` to `viewModel { (albumId: String) -> AudioListViewModel(albumId, get(), get(), get()) }`.

- [ ] **Step 8: Verify** — `./gradlew :feature:audio:assembleDebug -q` + `./gradlew :feature:audio:testDebugUnitTest -q`. Both SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add feature/audio
git commit -m "feat(feature:audio): per-row + batch downloads in the audio list (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT8: `:feature:audio` — DownloadsViewModel + DownloadsScreen + nav (TDD)

**Files:**
- Create `feature/audio/.../downloads/DownloadsViewModel.kt`, `downloads/DownloadsScreen.kt`
- Modify `AudioModule.kt`, `navigation/AudioPane.kt`
- Test `feature/audio/src/test/.../downloads/DownloadsViewModelTest.kt`

- [ ] **Step 1: Failing VM test** `DownloadsViewModelTest.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.downloads

import com.watnapp.buddhawajana.core.data.repo.DownloadRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Download
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.state.UiState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun dl(id: String) = Download(id, "T$id", "u$id", "9", "Album", "cov", 1_000L, id.toLong())

    @Test fun `empty downloads yields Empty`() = runTest {
        val repo: DownloadRepository = mockk(relaxed = true) { every { downloads } returns flowOf(emptyList()) }
        val vm = DownloadsViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Empty)
    }

    @Test fun `play builds single-track queue`() = runTest {
        val repo: DownloadRepository = mockk(relaxed = true) { every { downloads } returns flowOf(listOf(dl("7"))) }
        val controller: PlaybackController = mockk(relaxed = true)
        val vm = DownloadsViewModel(repo, controller)
        advanceUntilIdle()
        vm.play(dl("7"))
        verify { controller.setQueue(Album("9", "Album", "cov", 0, 0), listOf(Audio("7", "9", "T7", "u7")), 0) }
    }

    @Test fun `delete forwards`() = runTest {
        val repo: DownloadRepository = mockk(relaxed = true) { every { downloads } returns flowOf(emptyList()) }
        val vm = DownloadsViewModel(repo, mockk(relaxed = true))
        vm.delete("7")
        advanceUntilIdle()
        coVerify { repo.delete("7") }
    }
}
```

- [ ] **Step 2: Run, verify FAIL** — `./gradlew :feature:audio:testDebugUnitTest --tests "*DownloadsViewModelTest*" -q` → FAIL.

- [ ] **Step 3: Create DownloadsViewModel.kt**
```kotlin
package com.watnapp.buddhawajana.feature.audio.downloads

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.DownloadRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Download
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloads: DownloadRepository,
    private val controller: PlaybackController,
) : BaseViewModel() {

    val state: StateFlow<UiState<List<Download>>> =
        downloads.downloads
            .map { if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun play(d: Download) {
        controller.setQueue(
            Album(d.albumId, d.albumTitle, d.coverUrl, 0, 0),
            listOf(Audio(d.audioId, d.albumId, d.title, d.url)),
            0,
        )
    }

    fun delete(audioId: String) = viewModelScope.launch { downloads.delete(audioId) }
}
```

- [ ] **Step 4: Run, verify PASS** — `./gradlew :feature:audio:testDebugUnitTest --tests "*DownloadsViewModelTest*" -q` → PASS.

- [ ] **Step 5: Create DownloadsScreen.kt** (iOS-style list, swipe-to-remove; mirrors FavoritesScreen)
```kotlin
package com.watnapp.buddhawajana.feature.audio.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.designsystem.component.EmptyStateView
import com.watnapp.buddhawajana.core.designsystem.component.ErrorView
import com.watnapp.buddhawajana.core.designsystem.component.LoadingView
import com.watnapp.buddhawajana.core.model.Download
import com.watnapp.buddhawajana.core.ui.state.UiState
import com.watnapp.buddhawajana.feature.audio.format.formatRowMeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    state: UiState<List<Download>>,
    onPlay: (Download) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Empty -> EmptyStateView("ยังไม่มีรายการดาวน์โหลด")
        is UiState.Error -> ErrorView(message = state.message, onRetry = { })
        is UiState.Content -> LazyColumn(modifier.fillMaxSize()) {
            items(state.data, key = { it.audioId }) { d ->
                val dismiss = rememberSwipeToDismissBoxState(
                    confirmValueChange = { v ->
                        if (v != SwipeToDismissBoxValue.Settled) { onRemove(d.audioId); true } else false
                    },
                )
                SwipeToDismissBox(
                    state = dismiss,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) { Icon(Icons.Default.Delete, contentDescription = null) }
                    },
                ) {
                    ListItem(
                        leadingContent = {
                            CachedAsyncImage(d.coverUrl, d.title, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                        },
                        headlineContent = { Text(d.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(formatRowMeta(null, d.sizeBytes)) },
                        modifier = Modifier.clickable { onPlay(d) },
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 6: audioModule** — add import `com.watnapp.buddhawajana.feature.audio.downloads.DownloadsViewModel` and inside the module: `viewModel { DownloadsViewModel(get(), get()) }`.

- [ ] **Step 7: AudioPane — DownloadsRoute.** Add imports `com.watnapp.buddhawajana.feature.audio.downloads.DownloadsScreen`, `DownloadsViewModel`. Add `@Serializable private data object DownloadsRoute`. Add a composable after `FavoritesRoute`:
```kotlin
        composable<DownloadsRoute> {
            val vm: DownloadsViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            DownloadsScreen(
                state = state,
                onPlay = { d -> vm.play(d); onOpenPlayer() },
                onRemove = vm::delete,
            )
        }
```

- [ ] **Step 8: Verify** — `./gradlew :feature:audio:assembleDebug -q` + `./gradlew :feature:audio:testDebugUnitTest --tests "*DownloadsViewModelTest*" -q`. Both SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add feature/audio
git commit -m "feat(feature:audio): DownloadsViewModel + DownloadsScreen + nav route (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT9: `:feature:audio` — pinned downloads card on AlbumsScreen (TDD)

**Files:**
- Modify `feature/audio/.../albums/AlbumsViewModel.kt`, `albums/AlbumsScreen.kt`, `navigation/AudioPane.kt`, `AudioModule.kt`
- Modify `feature/audio/src/test/.../albums/AlbumsViewModelTest.kt`

- [ ] **Step 1: Update AlbumsViewModelTest** — add a DownloadRepository mock and a `downloadCount` test. Add class field:
```kotlin
    private val downloadsRepo: com.watnapp.buddhawajana.core.data.repo.DownloadRepository =
        io.mockk.mockk(relaxed = true) {
            io.mockk.every { downloads } returns kotlinx.coroutines.flow.flowOf(emptyList())
        }
```
Change both existing `AlbumsViewModel(repo(...), favorites)` constructions to `AlbumsViewModel(repo(...), favorites, downloadsRepo)`. Update the `favoriteCount reflects repo` test's `AlbumsViewModel(repo(emptyList()), favs)` to `AlbumsViewModel(repo(emptyList()), favs, downloadsRepo)`. Add:
```kotlin
    @Test fun `downloadCount reflects repo`() = runTest {
        val dls: com.watnapp.buddhawajana.core.data.repo.DownloadRepository =
            io.mockk.mockk(relaxed = true) {
                io.mockk.every { downloads } returns kotlinx.coroutines.flow.flowOf(
                    listOf(
                        com.watnapp.buddhawajana.core.model.Download("1", "T", "u", "9", "A", null, 1, 1),
                        com.watnapp.buddhawajana.core.model.Download("2", "T", "u", "9", "A", null, 1, 2),
                    )
                )
            }
        val vm = AlbumsViewModel(repo(emptyList()), favorites, dls)
        vm.downloadCount.test {
            var last = 0
            repeat(2) { last = awaitItem() }
            org.junit.Assert.assertEquals(2, last)
            cancelAndIgnoreRemainingEvents()
        }
    }
```

- [ ] **Step 2: Run, verify FAIL** — `./gradlew :feature:audio:testDebugUnitTest --tests "*AlbumsViewModelTest*" -q` → FAIL.

- [ ] **Step 3: Update AlbumsViewModel** — add `downloads` ctor param + `downloadCount`. Add import `com.watnapp.buddhawajana.core.data.repo.DownloadRepository`. Change ctor:
```kotlin
class AlbumsViewModel(
    private val repo: AlbumRepository,
    favorites: FavoriteRepository,
    downloads: DownloadRepository,
) : BaseViewModel() {
```
Add after `favoriteCount`:
```kotlin
    val downloadCount: StateFlow<Int> =
        downloads.downloads.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
```

- [ ] **Step 4: Run, verify PASS** — `./gradlew :feature:audio:testDebugUnitTest --tests "*AlbumsViewModelTest*" -q` → PASS.

- [ ] **Step 5: AlbumsScreen — downloads card.** Add params `downloadCount: Int` and `onOpenDownloads: () -> Unit` to `AlbumsScreen`. In the `if (query.isBlank()) { ... }` block, after the `FavoritesCard` + its `HorizontalDivider`, add:
```kotlin
            DownloadsCard(count = downloadCount, onClick = onOpenDownloads)
            HorizontalDivider()
```
Add the card composable at the bottom of the file (import `androidx.compose.material.icons.filled.Download`):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsCard(count: Int, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        headlineContent = { Text("ดาวน์โหลด") },
        trailingContent = {
            if (count > 0) Text("$count", style = MaterialTheme.typography.bodyMedium)
            else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
```

- [ ] **Step 6: AudioPane — pass downloads card params.** In `composable<AlbumsRoute>`, collect `downloadCount` and pass:
```kotlin
            val downloadCount by vm.downloadCount.collectAsState()
            AlbumsScreen(
                state = state,
                query = query,
                favoriteCount = favoriteCount,
                downloadCount = downloadCount,
                onSearch = vm::onSearch,
                onRefresh = vm::refresh,
                onOpenFavorites = { nav.navigate(FavoritesRoute) },
                onOpenDownloads = { nav.navigate(DownloadsRoute) },
                onOpenAlbum = { album -> nav.navigate(AudioListRoute(album.id, album.title, album.coverUrl)) },
            )
```
(Keep the existing `favoriteCount` collection line.)

- [ ] **Step 7: audioModule** — change `viewModel { AlbumsViewModel(get(), get()) }` to `viewModel { AlbumsViewModel(get(), get(), get()) }`.

- [ ] **Step 8: Verify** — `./gradlew :feature:audio:assembleDebug -q` + `./gradlew :feature:audio:testDebugUnitTest -q` (ALL). Both SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add feature/audio
git commit -m "feat(feature:audio): pinned downloads card on album list (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task DT10: `:app` — foreground perm + notification channel; full build + device smoke

**Files:** Modify `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/watnapp/buddhawajana/MainApplication.kt`

- [ ] **Step 1: Manifest perm** — in `AndroidManifest.xml`, after the existing `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission, add:
```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

- [ ] **Step 2: Notification channel** — in `MainApplication.onCreate()`, after `startKoin { ... }`, create the downloads channel (API 26+):
```kotlin
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(android.app.NotificationManager::class.java)
            mgr.createNotificationChannel(
                android.app.NotificationChannel(
                    "downloads",
                    "ดาวน์โหลด",
                    android.app.NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
```

- [ ] **Step 3: Full build + all tests** — Run:
`./gradlew :app:assembleDebug testDebugUnitTest :core:common:test :core:player:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all green.

- [ ] **Step 4: Commit**
```bash
git add app
git commit -m "feat(app): downloads foreground-service perm + notification channel

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 5: Device smoke (manual)**
1. Player → tap download → spinner → progress → check; file lands in audios dir.
2. Kill app mid-download → reopens, WorkManager retries/resumes, completes.
3. Audio rows show per-track download state; tap downloads one.
4. Album "ดาวน์โหลดทั้งหมด" → confirm → all queue + complete.
5. Albums screen → pinned ดาวน์โหลด card + count.
6. Downloads screen → list with sizes; tap → plays.
7. Airplane mode → play a downloaded track → plays from disk.
8. Swipe a download → file + row removed; count updates.

---

## Self-Review (author checklist — completed)

- **Spec coverage:** Download model (DT1); AudioFileStore + entity/dao + DownloadState + mapDownloadState + mapper + WM dep (DT2); DownloadWorker + DownloadRepository (DT3); DB v4 + DI (DT4); offline playback + NowPlaying.isLocal (DT5); DownloadButton + player action (DT6); list rows + batch (DT7); DownloadsScreen/VM + nav (DT8); pinned card + count (DT9); perm + channel + build (DT10). WorkManager ✔, batch ✔, denormalized+WorkInfo merge ✔, GlobalContext worker ✔, offline-local ✔. ✓
- **Placeholder scan:** none; every step has full code/commands. ✓
- **Type consistency:** `DownloadState` (5 variants) consistent DT2→DT6/7. `DownloadRepository` surface (downloads/enqueue/enqueueAll/cancel/delete/state) consistent DT3→DT6/7/8/9. `DownloadEntity`/`Download` fields ↔ migration SQL ↔ worker insert ↔ mapper. `Download(audioId,title,url,albumId,albumTitle,coverUrl,sizeBytes,completedAt)` consistent DT1→DT8/9 tests. `NowPlaying` 7-arg (isLocal) — constructed in MediaPlaybackController (DT5) + PlayerViewModelTest fixture (DT5/DT6). VM ctor arities matched in audioModule: PlayerVM 3 (DT6), AudioListVM 4 (DT7), AlbumsVM 3 (DT9), DownloadsVM 2 (DT8). `setQueue(Album, List<Audio>, Int)` unchanged. `mapDownloadState(Boolean,Boolean,WorkInfo.State?,Float)` consistent DT2↔DT3. ✓
```
