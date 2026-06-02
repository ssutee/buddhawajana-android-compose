# Favorites Vertical Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Favorite audio tracks (heart on the player), browse them in a Favorites screen reached from a pinned card on the album list, with swipe-to-remove.

**Architecture:** A denormalized `Favorite` snapshot (audioId + title + url + album id/title/cover + addedAt) stored in Room so favorites render and play standalone without joins. The player heart toggles favorites off `NowPlaying` (extended with `url`); the Favorites screen plays a tapped favorite as a single-track queue. All new UI lives in `:feature:audio`; nav stays inside `AudioPane`.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3, SwipeToDismissBox), Room (KSP) + migration, Koin, Coroutines/Flow, Coil. JVM unit tests (JUnit4 + Turbine + MockK).

**Conventions:** Package root `com.watnapp.buddhawajana`. All commits end with:
`Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`

---

## File Structure

**New:**
- `core/model/.../Favorite.kt`
- `core/data/.../db/FavoriteEntity.kt`, `db/FavoriteDao.kt`, `repo/FavoriteRepository.kt`
- `feature/audio/.../favorites/FavoritesViewModel.kt`, `favorites/FavoritesScreen.kt`
- tests for FavoriteRepository, FavoritesViewModel, PlayerViewModel (favorites), AlbumsViewModel (count)

**Modified:**
- `core/data/.../mapper/Mappers.kt`, `db/AppDatabase.kt` (v3 + migration), `DataModule.kt`
- `core/player/.../PlaybackController.kt` (NowPlaying.url), `MediaPlaybackController.kt` (populate url)
- `feature/audio/.../player/PlayerViewModel.kt` + `player/PlayerScreen.kt` (heart)
- `feature/audio/.../albums/AlbumsViewModel.kt` + `albums/AlbumsScreen.kt` (pinned card + count)
- `feature/audio/.../navigation/AudioPane.kt` (FavoritesRoute + wiring)
- `feature/audio/.../AudioModule.kt` (VM DI updates)

---

## Task FT1: `:core:model` — Favorite

**Files:** Create `core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/Favorite.kt`

- [ ] **Step 1: Create model**
```kotlin
package com.watnapp.buddhawajana.core.model

data class Favorite(
    val audioId: String,
    val title: String,
    val url: String,
    val albumId: String,
    val albumTitle: String,
    val coverUrl: String?,
    val addedAt: Long,
)
```

- [ ] **Step 2: Compile** — Run: `./gradlew :core:model:compileReleaseKotlin -q` → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add core/model
git commit -m "feat(core:model): Favorite

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task FT2: `:core:data` — FavoriteEntity/Dao/Repository + mapper (TDD)

**Files:**
- Create `core/data/src/main/java/com/watnapp/buddhawajana/core/data/db/FavoriteEntity.kt`
- Create `core/data/src/main/java/com/watnapp/buddhawajana/core/data/db/FavoriteDao.kt`
- Create `core/data/src/main/java/com/watnapp/buddhawajana/core/data/repo/FavoriteRepository.kt`
- Modify `core/data/src/main/java/com/watnapp/buddhawajana/core/data/mapper/Mappers.kt`
- Test `core/data/src/test/java/com/watnapp/buddhawajana/core/data/repo/FavoriteRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.db.FavoriteDao
import com.watnapp.buddhawajana.core.data.db.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteRepositoryTest {
    private val backing = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    private val dao = object : FavoriteDao {
        override fun stream(): Flow<List<FavoriteEntity>> =
            backing.map { it.sortedByDescending { e -> e.addedAt } }
        override fun isFavorite(audioId: String): Flow<Boolean> =
            backing.map { list -> list.any { it.audioId == audioId } }
        override suspend fun exists(audioId: String): Boolean = backing.value.any { it.audioId == audioId }
        override suspend fun insert(e: FavoriteEntity) =
            backing.update { it.filterNot { x -> x.audioId == e.audioId } + e }
        override suspend fun deleteById(audioId: String) =
            backing.update { it.filterNot { x -> x.audioId == audioId } }
    }
    private fun fav(id: String, addedAt: Long) =
        com.watnapp.buddhawajana.core.model.Favorite(id, "T$id", "u$id", "9", "Album", null, addedAt)

    @Test fun `add then favorites contains it newest-first`() = runTest {
        val repo = FavoriteRepository(dao)
        repo.add(fav("1", 100))
        repo.add(fav("2", 200))
        repo.favorites.test {
            assertEquals(listOf("2", "1"), awaitItem().map { it.audioId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `toggle adds when absent and removes when present`() = runTest {
        val repo = FavoriteRepository(dao)
        repo.toggle(fav("1", 1))
        repo.isFavorite("1").test { assertTrue(awaitItem()); cancelAndIgnoreRemainingEvents() }
        repo.toggle(fav("1", 2))
        repo.isFavorite("1").test { assertFalse(awaitItem()); cancelAndIgnoreRemainingEvents() }
    }

    @Test fun `remove deletes`() = runTest {
        val repo = FavoriteRepository(dao)
        repo.add(fav("1", 1))
        repo.remove("1")
        repo.isFavorite("1").test { assertFalse(awaitItem()); cancelAndIgnoreRemainingEvents() }
    }
}
```

- [ ] **Step 2: Run, verify FAIL** — Run: `./gradlew :core:data:testDebugUnitTest --tests "*FavoriteRepositoryTest*" -q` → FAIL (unresolved refs).

- [ ] **Step 3: Create FavoriteEntity**
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "audio_id") val audioId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "album_id") val albumId: String,
    @ColumnInfo(name = "album_title") val albumTitle: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
```

- [ ] **Step 4: Create FavoriteDao**
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY added_at DESC")
    fun stream(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE audio_id = :audioId)")
    fun isFavorite(audioId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE audio_id = :audioId)")
    suspend fun exists(audioId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE audio_id = :audioId")
    suspend fun deleteById(audioId: String)
}
```

- [ ] **Step 5: Create FavoriteRepository**
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.FavoriteDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Favorite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepository(private val dao: FavoriteDao) {
    val favorites: Flow<List<Favorite>> = dao.stream().map { list -> list.map { it.toModel() } }
    fun isFavorite(audioId: String): Flow<Boolean> = dao.isFavorite(audioId)
    suspend fun add(f: Favorite) = dao.insert(f.toEntity())
    suspend fun remove(audioId: String) = dao.deleteById(audioId)
    suspend fun toggle(f: Favorite) {
        if (dao.exists(f.audioId)) dao.deleteById(f.audioId) else dao.insert(f.toEntity())
    }
}
```

- [ ] **Step 6: Add mappers** — append to `Mappers.kt` (add imports `com.watnapp.buddhawajana.core.data.db.FavoriteEntity`, `com.watnapp.buddhawajana.core.model.Favorite`):
```kotlin
// ---- Favorite ----

fun FavoriteEntity.toModel() = Favorite(audioId, title, url, albumId, albumTitle, coverUrl, addedAt)
fun Favorite.toEntity() = FavoriteEntity(audioId, title, url, albumId, albumTitle, coverUrl, addedAt)
```

- [ ] **Step 7: Run, verify PASS** — Run: `./gradlew :core:data:testDebugUnitTest --tests "*FavoriteRepositoryTest*" -q` → PASS.

- [ ] **Step 8: Commit**
```bash
git add core/data
git commit -m "feat(core:data): Favorite entity/dao/repository + mapper (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task FT3: `:core:data` — DB v3 migration + DI

**Files:** Modify `core/data/.../db/AppDatabase.kt`, `core/data/.../DataModule.kt`

- [ ] **Step 1: AppDatabase v3**
In `AppDatabase.kt`: bump `version = 2` → `version = 3`; add `FavoriteEntity::class` to the `entities` array; add `abstract fun favoriteDao(): FavoriteDao`; add inside the companion object:
```kotlin
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS favorite (" +
                        "audio_id TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, url TEXT NOT NULL, " +
                        "album_id TEXT NOT NULL, album_title TEXT NOT NULL, " +
                        "cover_url TEXT, added_at INTEGER NOT NULL)"
                )
            }
        }
```

- [ ] **Step 2: DataModule wiring**
In `DataModule.kt`: add `.addMigrations(AppDatabase.MIGRATION_2_3)` to the existing `addMigrations(...)` call (so it reads `.addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)`); add `single { get<AppDatabase>().favoriteDao() }`; add `single { FavoriteRepository(get()) }` (import `com.watnapp.buddhawajana.core.data.repo.FavoriteRepository`).

- [ ] **Step 3: Compile (KSP regenerates Room)** — Run: `./gradlew :core:data:assembleDebug -q` → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add core/data
git commit -m "feat(core:data): DB v3 favorite table migration + DI

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task FT4: `:core:player` — NowPlaying.url

**Files:** Modify `core/player/.../PlaybackController.kt`, `core/player/.../MediaPlaybackController.kt`

- [ ] **Step 1: Add url to NowPlaying**
In `PlaybackController.kt`, change the data class to:
```kotlin
data class NowPlaying(
    val audioId: String,
    val albumId: String,
    val title: String,
    val album: String,
    val artworkUrl: String?,
    val url: String,
)
```

- [ ] **Step 2: Populate url in MediaPlaybackController.updateNowPlaying**
In `MediaPlaybackController.kt`, the `_nowPlaying.value = NowPlaying(...)` block (inside `updateNowPlaying()`) — add the `url` arg:
```kotlin
        _nowPlaying.value = NowPlaying(
            audioId = item.mediaId,
            albumId = md.extras?.getString("albumId") ?: "",
            title = md.title?.toString() ?: "",
            album = md.albumTitle?.toString() ?: "",
            artworkUrl = md.artworkUri?.toString(),
            url = item.localConfiguration?.uri?.toString() ?: "",
        )
```

- [ ] **Step 3: Compile** — Run: `./gradlew :core:player:assembleDebug -q` → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add core/player
git commit -m "feat(core:player): NowPlaying carries stream url (for favorite snapshot)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task FT5: `:feature:audio` — player heart (TDD)

**Files:**
- Modify `feature/audio/.../player/PlayerViewModel.kt`
- Modify `feature/audio/.../player/PlayerScreen.kt`
- Modify `feature/audio/.../AudioModule.kt`
- Modify `feature/audio/src/test/.../player/PlayerViewModelTest.kt`

- [ ] **Step 1: Update the failing test**
Replace `PlayerViewModelTest.kt` with (adds a FavoriteRepository mock to the existing two tests + a toggleFavorite test):
```kotlin
package com.watnapp.buddhawajana.feature.audio.player

import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.NowPlaying
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val favorites: FavoriteRepository = mockk(relaxed = true) {
        every { isFavorite(any()) } returns flowOf(false)
    }
    private fun controller(now: NowPlaying? = null): PlaybackController = mockk(relaxed = true) {
        every { nowPlaying } returns MutableStateFlow(now)
        every { isPlaying } returns MutableStateFlow(false)
        every { positionMs } returns MutableStateFlow(0L)
        every { durationMs } returns MutableStateFlow(0L)
        every { speed } returns MutableStateFlow(1.0f)
        every { sleepTimer } returns MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    }

    @Test fun `skip forward and back use the 15s delta`() {
        val c = controller()
        val vm = PlayerViewModel(c, favorites)
        vm.skipForward(); coVerify { c.skip(SKIP_DELTA_MS) }
        vm.skipBack(); coVerify { c.skip(-SKIP_DELTA_MS) }
    }

    @Test fun `playPause and seek forward to controller`() {
        val c = controller()
        val vm = PlayerViewModel(c, favorites)
        vm.playPause(); coVerify { c.playPause() }
        vm.seekTo(1234L); coVerify { c.seekTo(1234L) }
    }

    @Test fun `toggleFavorite snapshots current track`() = runTest {
        val now = NowPlaying("7", "9", "Talk", "Album", "cov", "http://x/7.mp3")
        val vm = PlayerViewModel(controller(now), favorites)
        vm.toggleFavorite()
        advanceUntilIdle()
        coVerify { favorites.toggle(match<Favorite> { it.audioId == "7" && it.url == "http://x/7.mp3" && it.albumTitle == "Album" }) }
    }

    @Test fun `toggleFavorite no-ops when nothing playing`() = runTest {
        val vm = PlayerViewModel(controller(null), favorites)
        vm.toggleFavorite()
        advanceUntilIdle()
        coVerify(exactly = 0) { favorites.toggle(any()) }
    }
}
```

- [ ] **Step 2: Run, verify FAIL** — Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q` → FAIL (ctor arity / toggleFavorite missing).

- [ ] **Step 3: Update PlayerViewModel**
Replace `PlayerViewModel.kt` with:
```kotlin
package com.watnapp.buddhawajana.feature.audio.player

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.NowPlaying
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val controller: PlaybackController,
    private val favorites: FavoriteRepository,
) : BaseViewModel() {
    val nowPlaying = controller.nowPlaying
    val isPlaying = controller.isPlaying
    val positionMs = controller.positionMs
    val durationMs = controller.durationMs
    val speed = controller.speed
    val sleepTimer = controller.sleepTimer

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> =
        controller.nowPlaying.flatMapLatest { np ->
            if (np == null) flowOf(false) else favorites.isFavorite(np.audioId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun playPause() = controller.playPause()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun skipForward() = controller.skip(SKIP_DELTA_MS)
    fun skipBack() = controller.skip(-SKIP_DELTA_MS)
    fun next() = controller.next()
    fun prev() = controller.prev()
    fun setSpeed(rate: Float) = controller.setSpeed(rate)
    fun setSleepTimer(state: SleepTimerState) = controller.setSleepTimer(state)

    fun toggleFavorite() = viewModelScope.launch {
        val np = controller.nowPlaying.value ?: return@launch
        if (np.url.isEmpty()) return@launch
        favorites.toggle(np.toFavorite(System.currentTimeMillis()))
    }
}

internal fun NowPlaying.toFavorite(addedAt: Long) =
    Favorite(audioId, title, url, albumId, album, artworkUrl, addedAt)
```

- [ ] **Step 4: Run, verify PASS** — Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q` → PASS.

- [ ] **Step 5: audioModule — 2-arg PlayerViewModel**
In `AudioModule.kt`, change `viewModel { PlayerViewModel(get()) }` to `viewModel { PlayerViewModel(get(), get()) }`.

- [ ] **Step 6: Heart in PlayerScreen**
In `PlayerScreen.kt`: collect favorite state and add a heart button. Add these imports:
```kotlin
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
```
At the top of `PlayerScreen` (next to the other `collectAsState()` calls) add:
```kotlin
    val isFavorite by vm.isFavorite.collectAsState()
```
Then change the controls `Row` to include a heart at the end. Locate the controls row (`Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { ... prev/skipBack/playPause/skipForward/next ... }`) and add, after the `next` IconButton and before the row closes:
```kotlin
            IconButton(onClick = vm::toggleFavorite, enabled = now != null) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "รายการโปรด",
                )
            }
```
(`now` is the existing `val now by vm.nowPlaying.collectAsState()`.)

- [ ] **Step 7: Verify** — Run: `./gradlew :feature:audio:assembleDebug -q` and `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q`. Both BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**
```bash
git add feature/audio
git commit -m "feat(feature:audio): player favorite heart toggle (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task FT6: `:feature:audio` — FavoritesViewModel + FavoritesScreen + nav (TDD)

**Files:**
- Create `feature/audio/.../favorites/FavoritesViewModel.kt`
- Create `feature/audio/.../favorites/FavoritesScreen.kt`
- Modify `feature/audio/.../AudioModule.kt`
- Modify `feature/audio/.../navigation/AudioPane.kt`
- Test `feature/audio/src/test/.../favorites/FavoritesViewModelTest.kt`

- [ ] **Step 1: Write the failing VM test**
```kotlin
package com.watnapp.buddhawajana.feature.audio.favorites

import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.state.UiState
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
class FavoritesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun fav(id: String) = Favorite(id, "T$id", "u$id", "9", "Album", "cov", id.toLong())

    @Test fun `empty favorites yields Empty state`() = runTest {
        val repo: FavoriteRepository = mockk(relaxed = true) { every { favorites } returns flowOf(emptyList()) }
        val vm = FavoritesViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Empty)
    }

    @Test fun `content then play builds single-track queue`() = runTest {
        val repo: FavoriteRepository = mockk(relaxed = true) { every { favorites } returns flowOf(listOf(fav("7"))) }
        val controller: PlaybackController = mockk(relaxed = true)
        val vm = FavoritesViewModel(repo, controller)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Content)
        vm.play(fav("7"))
        verify {
            controller.setQueue(
                Album("9", "Album", "cov", 0, 0),
                listOf(Audio("7", "9", "T7", "u7")),
                0,
            )
        }
    }

    @Test fun `remove forwards to repo`() = runTest {
        val repo: FavoriteRepository = mockk(relaxed = true) { every { favorites } returns flowOf(emptyList()) }
        val vm = FavoritesViewModel(repo, mockk(relaxed = true))
        vm.remove("7")
        advanceUntilIdle()
        io.mockk.coVerify { repo.remove("7") }
    }
}
```

- [ ] **Step 2: Run, verify FAIL** — Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*FavoritesViewModelTest*" -q` → FAIL.

- [ ] **Step 3: Create FavoritesViewModel**
```kotlin
package com.watnapp.buddhawajana.feature.audio.favorites

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favorites: FavoriteRepository,
    private val controller: PlaybackController,
) : BaseViewModel() {

    val state: StateFlow<UiState<List<Favorite>>> =
        favorites.favorites
            .map { if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun play(f: Favorite) {
        controller.setQueue(
            Album(f.albumId, f.albumTitle, f.coverUrl, 0, 0),
            listOf(Audio(f.audioId, f.albumId, f.title, f.url)),
            0,
        )
    }

    fun remove(audioId: String) = viewModelScope.launch { favorites.remove(audioId) }
}
```

- [ ] **Step 4: Run, verify PASS** — Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*FavoritesViewModelTest*" -q` → PASS.

- [ ] **Step 5: Create FavoritesScreen**
```kotlin
package com.watnapp.buddhawajana.feature.audio.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.ui.state.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: UiState<List<Favorite>>,
    onPlay: (Favorite) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Empty -> EmptyStateView("ยังไม่มีรายการโปรด")
        is UiState.Error -> ErrorView(state.message) { }
        is UiState.Content -> LazyColumn(modifier.fillMaxSize()) {
            items(state.data, key = { it.audioId }) { fav ->
                val dismiss = rememberSwipeToDismissBoxState(
                    confirmValueChange = { v ->
                        if (v != SwipeToDismissBoxValue.Settled) { onRemove(fav.audioId); true } else false
                    },
                )
                SwipeToDismissBox(
                    state = dismiss,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) { Icon(Icons.Default.Delete, contentDescription = null) }
                    },
                ) {
                    FavoriteRow(fav, onClick = { onPlay(fav) })
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteRow(fav: Favorite, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            CachedAsyncImage(fav.coverUrl, fav.title, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
        },
        headlineContent = { Text(fav.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(fav.albumTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
```

- [ ] **Step 6: audioModule — register FavoritesViewModel**
In `AudioModule.kt` add import `com.watnapp.buddhawajana.feature.audio.favorites.FavoritesViewModel` and inside the module:
```kotlin
    viewModel { FavoritesViewModel(get(), get()) }
```

- [ ] **Step 7: Add FavoritesRoute to AudioPane**
In `AudioPane.kt`:
1. Add import `com.watnapp.buddhawajana.feature.audio.favorites.FavoritesScreen` and `com.watnapp.buddhawajana.feature.audio.favorites.FavoritesViewModel`.
2. Add route: `@Serializable private data object FavoritesRoute`.
3. Add a composable inside the `NavHost` (after the `AudioListRoute` block):
```kotlin
        composable<FavoritesRoute> {
            val vm: FavoritesViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            FavoritesScreen(
                state = state,
                onPlay = { f -> vm.play(f); onOpenPlayer() },
                onRemove = vm::remove,
            )
        }
```

- [ ] **Step 8: Verify** — Run: `./gradlew :feature:audio:assembleDebug -q` and `./gradlew :feature:audio:testDebugUnitTest --tests "*FavoritesViewModelTest*" -q`. Both BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add feature/audio
git commit -m "feat(feature:audio): FavoritesViewModel + FavoritesScreen + nav route (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task FT7: `:feature:audio` — pinned favorites card on AlbumsScreen (TDD)

**Files:**
- Modify `feature/audio/.../albums/AlbumsViewModel.kt`
- Modify `feature/audio/.../albums/AlbumsScreen.kt`
- Modify `feature/audio/.../navigation/AudioPane.kt`
- Modify `feature/audio/.../AudioModule.kt`
- Modify `feature/audio/src/test/.../albums/AlbumsViewModelTest.kt`

- [ ] **Step 1: Update the AlbumsViewModel test**
In `AlbumsViewModelTest.kt`, the existing `repo(albums)` helper builds an `AlbumsViewModel(repo(...))`. Update both existing tests to construct `AlbumsViewModel(repo(...), favorites)` where `favorites` is:
```kotlin
    private val favorites: com.watnapp.buddhawajana.core.data.repo.FavoriteRepository =
        io.mockk.mockk(relaxed = true) {
            io.mockk.every { favorites } returns kotlinx.coroutines.flow.flowOf(emptyList())
        }
```
Then add a new test:
```kotlin
    @Test fun `favoriteCount reflects repo`() = runTest {
        val favs: com.watnapp.buddhawajana.core.data.repo.FavoriteRepository =
            io.mockk.mockk(relaxed = true) {
                io.mockk.every { favorites } returns kotlinx.coroutines.flow.flowOf(
                    listOf(
                        com.watnapp.buddhawajana.core.model.Favorite("1", "T", "u", "9", "A", null, 1),
                        com.watnapp.buddhawajana.core.model.Favorite("2", "T", "u", "9", "A", null, 2),
                    )
                )
            }
        val vm = AlbumsViewModel(repo(emptyList()), favs)
        vm.favoriteCount.test {
            var last = 0
            repeat(2) { last = awaitItem() }
            org.junit.Assert.assertEquals(2, last)
            cancelAndIgnoreRemainingEvents()
        }
    }
```
(Ensure `import app.cash.turbine.test` and `import kotlinx.coroutines.test.runTest` are present.)

- [ ] **Step 2: Run, verify FAIL** — Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*AlbumsViewModelTest*" -q` → FAIL (ctor arity / favoriteCount missing).

- [ ] **Step 3: Update AlbumsViewModel**
Replace `AlbumsViewModel.kt` with:
```kotlin
package com.watnapp.buddhawajana.feature.audio.albums

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumsViewModel(
    private val repo: AlbumRepository,
    favorites: FavoriteRepository,
) : BaseViewModel() {
    private val query = MutableStateFlow("")
    val queryState: StateFlow<String> = query.asStateFlow()
    fun onSearch(q: String) { query.value = q }

    val state: StateFlow<UiState<List<Album>>> =
        combine(repo.stream(), query) { albums, q ->
            val filtered = if (q.isBlank()) albums
                else albums.filter { it.title.contains(q.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) UiState.Empty else UiState.Content(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val favoriteCount: StateFlow<Int> =
        favorites.favorites.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        repo.refresh().onFailure { emitMessage("รีเฟรชล้มเหลว") }
    }
}
```

- [ ] **Step 4: Run, verify PASS** — Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*AlbumsViewModelTest*" -q` → PASS.

- [ ] **Step 5: Pinned card in AlbumsScreen**
In `AlbumsScreen.kt`:
1. Update the signature to add `onOpenFavorites: () -> Unit` and `favoriteCount: Int`:
```kotlin
@Composable
fun AlbumsScreen(
    state: UiState<List<Album>>,
    query: String,
    favoriteCount: Int,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
```
2. Add these imports:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
```
3. Inside the root `Column`, immediately AFTER the `OutlinedTextField {...}` and BEFORE the `when (state)` block, add the pinned card (only when not searching):
```kotlin
        if (query.isBlank()) {
            FavoritesCard(count = favoriteCount, onClick = onOpenFavorites)
            HorizontalDivider()
        }
```
4. Add `import androidx.compose.material3.HorizontalDivider` and the card composable at the bottom of the file:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesCard(count: Int, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        headlineContent = { Text("รายการโปรด") },
        trailingContent = {
            if (count > 0) {
                Text("$count", style = MaterialTheme.typography.bodyMedium)
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
```
(`MaterialTheme`, `Text`, `clickable`, `Modifier` are already imported from the existing album-list code; if `MaterialTheme` is not imported, add `import androidx.compose.material3.MaterialTheme`.)

- [ ] **Step 6: Wire AudioPane Albums composable**
In `AudioPane.kt`, the `composable<AlbumsRoute>` block: collect favoriteCount and pass the new params:
```kotlin
        composable<AlbumsRoute> {
            val vm: AlbumsViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            val query by vm.queryState.collectAsState()
            val favoriteCount by vm.favoriteCount.collectAsState()
            AlbumsScreen(
                state = state,
                query = query,
                favoriteCount = favoriteCount,
                onSearch = vm::onSearch,
                onRefresh = vm::refresh,
                onOpenFavorites = { nav.navigate(FavoritesRoute) },
                onOpenAlbum = { album -> nav.navigate(AudioListRoute(album.id, album.title, album.coverUrl)) },
            )
        }
```

- [ ] **Step 7: audioModule — 2-arg AlbumsViewModel**
In `AudioModule.kt`, change `viewModel { AlbumsViewModel(get()) }` to `viewModel { AlbumsViewModel(get(), get()) }`.

- [ ] **Step 8: Verify** — Run: `./gradlew :feature:audio:assembleDebug -q` and `./gradlew :feature:audio:testDebugUnitTest -q`. Both BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add feature/audio
git commit -m "feat(feature:audio): pinned favorites card on album list (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task FT8: Whole-project build + device smoke (manual)

- [ ] **Step 1: Full build + all tests**
Run: `./gradlew :app:assembleDebug testDebugUnitTest :core:common:test :core:player:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all green.

- [ ] **Step 2: Device smoke checklist (manual)**
1. Play a track → tap heart → fills; reopen player → still filled.
2. Albums screen → pinned รายการโปรด card shows, count matches.
3. Open Favorites → newest-first list; tap a row → plays that track + opens player.
4. Swipe a favorite → removed; count + list update.
5. Favorite a track, kill+reopen app → favorite persists and still plays.
6. Unheart from player → disappears from Favorites.

---

## Self-Review (author checklist — completed)

- **Spec coverage:** Favorite model (FT1); entity/dao/repo/mapper (FT2); DB v3 migration + DI (FT3); NowPlaying.url (FT4); player heart + isFavorite + toggle (FT5); FavoritesScreen/VM + single-track play + swipe-remove + nav (FT6); pinned card + count (FT7). Audio-only ✔, single-track play ✔, player-only heart ✔, denormalized snapshot ✔. ✓
- **Placeholder scan:** none — every step has full code/commands. ✓
- **Type consistency:** `Favorite(audioId, title, url, albumId, albumTitle, coverUrl, addedAt)` identical across model (FT1), entity/mapper (FT2), `toFavorite` (FT5), VM play (FT6), tests. `FavoriteRepository` surface (favorites/isFavorite/add/remove/toggle/exists) consistent FT2→FT5→FT6→FT7. `NowPlaying` 6-arg (with url) — only constructed in MediaPlaybackController (FT4) + tests (FT5). `AlbumsViewModel`/`PlayerViewModel` 2-arg ctors matched in audioModule (FT5/FT7). `setQueue(Album, List<Audio>, Int)` matches existing PlaybackController. ✓
```
