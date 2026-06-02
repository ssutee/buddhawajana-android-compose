# Audio Vertical (V1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a Material 3 Audio vertical — browse (albums→audios) + streaming Media3 player with background playback, lock-screen controls, resume position, a persistent mini-player, and per-row duration/size metadata.

**Architecture:** A new `:core:player` module owns the Media3 engine: a `PlaybackService` (ExoPlayer + MediaSession) plus a `PlaybackController` interface (Media3-backed impl) exposing playback state as Flows and accepting commands. The app scaffold renders the mini-player and `:feature:audio` renders browse + full player, both consuming the same `PlaybackController`. Small additions to `:core:data` (PlaybackProgress, audio metadata columns, MetadataProber) and `:core:common` (https helper).

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (Material 3), AndroidX Media3 1.5.1 (ExoPlayer + Session), Navigation-Compose + kotlinx-serialization, Room (KSP), Koin, Coil, OkHttp, DataStore Preferences, Coroutines/Flow. JVM unit tests with JUnit4 + Turbine + MockK.

**Conventions:**
- Package root `com.watnapp.buddhawajana`. Convention plugins: `buddhawajana.android.library` (android libs), `buddhawajana.android.compose` (compose libs), `buddhawajana.kotlin.library` (pure kotlin).
- All git commits end with the trailer:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`
- Media3 engine / service / MediaController are **device-verified, not unit-tested** (per the three reader device bugs — runtime gesture/threading/service issues don't surface in JVM tests). Those tasks build + compile in CI and are checked against the device smoke checklist at the end.

---

## File Structure

**New module `:core:player`** (`core/player/`):
- `build.gradle.kts`, `src/main/AndroidManifest.xml`
- `player/PlaybackService.kt` — MediaSessionService (ExoPlayer + MediaSession)
- `player/PlaybackController.kt` — interface + `NowPlaying`, `SleepTimerState`, `PlaybackSpeed`, sleep-tick helpers
- `player/MediaPlaybackController.kt` — Media3-backed impl (MediaController + listeners + ticker + resume)
- `player/PlaybackPrefs.kt` — DataStore speed persistence
- `player/PlayerModule.kt` — Koin `playerModule`
- `src/test/.../SleepTimerTest.kt`, `PlaybackSpeedTest.kt`

**New module `:feature:audio`** (`feature/audio/`):
- `build.gradle.kts`, `src/main/AndroidManifest.xml`
- `AudioModule.kt` — Koin `audioModule`
- `albums/AlbumsViewModel.kt`, `albums/AlbumsScreen.kt`
- `list/AudioListViewModel.kt`, `list/AudioListScreen.kt`
- `player/PlayerViewModel.kt`, `player/PlayerScreen.kt`
- `format/AudioFormat.kt` — `formatTime`, `formatRowMeta`
- `navigation/AudioPane.kt` — nested NavHost (albums↔audio list)
- `src/test/...` — VM + formatter tests

**Modified:**
- `gradle/libs.versions.toml` — Media3 + DataStore deps
- `settings.gradle.kts` — include `:core:player`, `:feature:audio`
- `core/model/.../Audio.kt`, new `core/model/.../PlaybackProgress.kt`
- `core/common/.../HttpsUrl.kt` (+ test)
- `core/data`: `AudioEntity.kt`, `AudioDao.kt`, `Mappers.kt`, `AudioRepository.kt`, new `PlaybackProgressEntity.kt`/`PlaybackProgressDao.kt`/`PlaybackProgressRepository.kt`, new `MetadataProber.kt`, `AppDatabase.kt` (v2 + migration), `DataModule.kt`
- `app`: `AndroidManifest.xml`, `MainApplication.kt`, `navigation/BuddhawajanaNavHost.kt`, `navigation/HomeScaffold.kt`, new `navigation/MiniPlayer.kt`, `app/build.gradle.kts`; remove legacy audio (`ui/AudioScreen.kt`, `ui/Mp3PlayerActivity.kt`, `vm/AudioViewModel.kt`, `vm/AlbumViewModel.kt`, legacy audio/album repos, arg-player dep) in AT14

---

## Task AT1: Add Media3 + DataStore to version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add versions**

In `[versions]`, add:
```toml
media3 = "1.5.1"
datastore = "1.1.1"
```

- [ ] **Step 2: Add libraries**

In `[libraries]` (near the other AndroidX entries):
```toml
media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
media3-session = { module = "androidx.media3:media3-session", version.ref = "media3" }
media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

- [ ] **Step 3: Verify catalog parses**

Run: `./gradlew help -q`
Expected: BUILD SUCCESSFUL (no catalog errors).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build(audio): add Media3 + DataStore to version catalog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT2: `:core:model` — PlaybackProgress + Audio metadata fields

**Files:**
- Create: `core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/PlaybackProgress.kt`
- Modify: `core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/Audio.kt`

- [ ] **Step 1: Create PlaybackProgress model**

`core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/PlaybackProgress.kt`:
```kotlin
package com.watnapp.buddhawajana.core.model

data class PlaybackProgress(
    val audioId: String,
    val positionMs: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 2: Add nullable metadata to Audio (default null → existing call sites unaffected)**

Replace the body of `core/model/.../Audio.kt` with:
```kotlin
package com.watnapp.buddhawajana.core.model

data class Audio(
    val id: String,
    val albumId: String,
    val title: String,
    val url: String,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
)
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :core:model:compileReleaseKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/model
git commit -m "feat(core:model): PlaybackProgress + Audio duration/size fields

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT3: `:core:common` — https-upgrade helper (TDD)

**Files:**
- Create: `core/common/src/main/kotlin/com/watnapp/buddhawajana/core/common/HttpsUrl.kt`
- Test: `core/common/src/test/kotlin/com/watnapp/buddhawajana/core/common/HttpsUrlTest.kt`

- [ ] **Step 1: Write the failing test**

`core/common/src/test/kotlin/com/watnapp/buddhawajana/core/common/HttpsUrlTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class HttpsUrlTest {
    @Test fun `upgrades http to https`() =
        assertEquals("https://x.com/a.mp3", "http://x.com/a.mp3".toHttpsOrSelf())

    @Test fun `trims surrounding whitespace`() =
        assertEquals("https://x.com/a.mp3", "  http://x.com/a.mp3 ".toHttpsOrSelf())

    @Test fun `leaves https untouched`() =
        assertEquals("https://x.com/a.mp3", "https://x.com/a.mp3".toHttpsOrSelf())

    @Test fun `leaves non-http scheme untouched`() =
        assertEquals("file:///a.mp3", "file:///a.mp3".toHttpsOrSelf())
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :core:common:test --tests "*HttpsUrlTest*" -q`
Expected: FAIL (unresolved reference `toHttpsOrSelf`).

- [ ] **Step 3: Implement**

`core/common/src/main/kotlin/com/watnapp/buddhawajana/core/common/HttpsUrl.kt`:
```kotlin
package com.watnapp.buddhawajana.core.common

/** Upgrade a plain `http://` URL to `https://` and trim whitespace; leave other schemes as-is. */
fun String.toHttpsOrSelf(): String {
    val t = trim()
    return if (t.startsWith("http://")) "https://" + t.removePrefix("http://") else t
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :core:common:test --tests "*HttpsUrlTest*" -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/common
git commit -m "feat(core:common): toHttpsOrSelf url helper (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT4: `:core:data` — PlaybackProgress entity/dao/repo (TDD)

**Files:**
- Create: `core/data/src/main/java/com/watnapp/buddhawajana/core/data/db/PlaybackProgressEntity.kt`
- Create: `core/data/src/main/java/com/watnapp/buddhawajana/core/data/db/PlaybackProgressDao.kt`
- Create: `core/data/src/main/java/com/watnapp/buddhawajana/core/data/repo/PlaybackProgressRepository.kt`
- Modify: `core/data/src/main/java/com/watnapp/buddhawajana/core/data/mapper/Mappers.kt`
- Test: `core/data/src/test/java/com/watnapp/buddhawajana/core/data/repo/PlaybackProgressRepositoryTest.kt`

- [ ] **Step 1: Write the failing test (fake in-memory dao + real repo)**

`PlaybackProgressRepositoryTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.PlaybackProgressDao
import com.watnapp.buddhawajana.core.data.db.PlaybackProgressEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackProgressRepositoryTest {
    private val store = HashMap<String, PlaybackProgressEntity>()
    private val dao = object : PlaybackProgressDao {
        override suspend fun get(audioId: String) = store[audioId]
        override suspend fun upsert(progress: PlaybackProgressEntity) { store[progress.audioId] = progress }
    }

    @Test fun `save then get round-trips position`() = runTest {
        val repo = PlaybackProgressRepository(dao)
        assertNull(repo.get("5"))
        repo.save("5", 42_000L, now = 100L)
        val p = repo.get("5")!!
        assertEquals(42_000L, p.positionMs)
        assertEquals(100L, p.updatedAt)
    }

    @Test fun `save overwrites prior position`() = runTest {
        val repo = PlaybackProgressRepository(dao)
        repo.save("5", 1_000L, now = 1L)
        repo.save("5", 9_000L, now = 2L)
        assertEquals(9_000L, repo.get("5")!!.positionMs)
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*PlaybackProgressRepositoryTest*" -q`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Create entity**

`PlaybackProgressEntity.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey @ColumnInfo(name = "audio_id") val audioId: String,
    @ColumnInfo(name = "position_ms") val positionMs: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
```

- [ ] **Step 4: Create dao**

`PlaybackProgressDao.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE audio_id = :audioId")
    suspend fun get(audioId: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity)
}
```

- [ ] **Step 5: Create repo**

`PlaybackProgressRepository.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.PlaybackProgressDao
import com.watnapp.buddhawajana.core.data.db.PlaybackProgressEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.PlaybackProgress

class PlaybackProgressRepository(private val dao: PlaybackProgressDao) {
    suspend fun get(audioId: String): PlaybackProgress? = dao.get(audioId)?.toModel()
    suspend fun save(audioId: String, positionMs: Long, now: Long = System.currentTimeMillis()) {
        dao.upsert(PlaybackProgressEntity(audioId = audioId, positionMs = positionMs, updatedAt = now))
    }
}
```

- [ ] **Step 6: Add mapper**

In `Mappers.kt`, add `PlaybackProgress` import and, at the end:
```kotlin
// ---- PlaybackProgress ----

fun PlaybackProgressEntity.toModel() = PlaybackProgress(audioId, positionMs, updatedAt)
```
(Add imports: `com.watnapp.buddhawajana.core.data.db.PlaybackProgressEntity`, `com.watnapp.buddhawajana.core.model.PlaybackProgress`.)

- [ ] **Step 7: Run test, verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*PlaybackProgressRepositoryTest*" -q`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add core/data
git commit -m "feat(core:data): PlaybackProgress entity/dao/repo (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT5: `:core:data` — audio metadata columns + MetadataProber + refresh preserves metadata (TDD)

**Files:**
- Modify: `core/data/.../db/AudioEntity.kt`
- Modify: `core/data/.../db/AudioDao.kt`
- Modify: `core/data/.../mapper/Mappers.kt`
- Modify: `core/data/.../repo/AudioRepository.kt`
- Create: `core/data/.../download/MetadataProber.kt`
- Test: `core/data/src/test/java/com/watnapp/buddhawajana/core/data/download/MetadataProberTest.kt`
- Test: `core/data/src/test/java/com/watnapp/buddhawajana/core/data/repo/AudioRepositoryMetadataTest.kt`

- [ ] **Step 1: Write failing MetadataProber test (injected probe lambdas)**

`MetadataProberTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.download

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataProberTest {
    @Test fun `returns duration and size from probes`() = runTest {
        val prober = MetadataProber(
            headSize = { 12_000_000L },
            readDuration = { 323_000L },
        )
        val (dur, size) = prober.probe("http://x/a.mp3")
        assertEquals(323_000L, dur)
        assertEquals(12_000_000L, size)
    }

    @Test fun `null when probes throw`() = runTest {
        val prober = MetadataProber(
            headSize = { error("network") },
            readDuration = { error("decode") },
        )
        val (dur, size) = prober.probe("http://x/a.mp3")
        assertNull(dur)
        assertNull(size)
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MetadataProberTest*" -q`
Expected: FAIL (unresolved `MetadataProber`).

- [ ] **Step 3: Implement MetadataProber**

`core/data/.../download/MetadataProber.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.download

import android.media.MediaMetadataRetriever
import com.watnapp.buddhawajana.core.common.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Probes an audio URL for duration (ms) and size (bytes). Each probe is best-effort:
 * a failure yields null for that field. Heavy work runs on Dispatchers.IO.
 *
 * Primary constructor takes lambdas for testability; the OkHttpClient secondary
 * constructor wires the real HEAD + MediaMetadataRetriever implementations.
 */
class MetadataProber(
    private val headSize: suspend (url: String) -> Long?,
    private val readDuration: suspend (url: String) -> Long?,
) {
    suspend fun probe(url: String): Pair<Long?, Long?> = withContext(Dispatchers.IO) {
        val size = runCatchingCancellable { headSize(url) }.getOrNull()
        val dur = runCatchingCancellable { readDuration(url) }.getOrNull()
        dur to size
    }

    constructor(client: OkHttpClient) : this(
        headSize = { url ->
            client.newCall(Request.Builder().url(url).head().build()).execute().use { resp ->
                resp.header("Content-Length")?.toLongOrNull()
            }
        },
        readDuration = { url ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(url, HashMap())
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        },
    )
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MetadataProberTest*" -q`
Expected: PASS.

- [ ] **Step 5: Add metadata columns to AudioEntity**

In `AudioEntity.kt`, add two nullable columns inside the data class (after `progress`):
```kotlin
    @ColumnInfo(name = "duration_ms") var durationMs: Long? = null,
    @ColumnInfo(name = "size_bytes") var sizeBytes: Long? = null,
```

- [ ] **Step 6: Add AudioDao.updateMetadata**

In `AudioDao.kt`, add:
```kotlin
    @Query("UPDATE audio SET duration_ms = :durationMs, size_bytes = :sizeBytes WHERE audio_id = :audioId")
    suspend fun updateMetadata(audioId: Long, durationMs: Long?, sizeBytes: Long?)
```

- [ ] **Step 7: Update Audio mappers (https upgrade + carry metadata)**

In `Mappers.kt`, add import `com.watnapp.buddhawajana.core.common.toHttpsOrSelf`, then replace the Audio mapper block:
```kotlin
// ---- Audio ----

fun AudioDto.toEntity(albumId: String): AudioEntity = AudioEntity(
    audioId = id.toLong(),
    albumId = albumId.toLong(),
    title = name ?: "",
    url = fileUrl?.toHttpsOrSelf() ?: "",
    status = 0,
    requestId = 0,
    progress = 0,
    // duration_ms / size_bytes keep entity defaults (null) — populated later by MetadataProber
)

fun AudioEntity.toModel(): Audio = Audio(
    id = audioId.toString(),
    albumId = albumId.toString(),
    title = title,
    url = url,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
)
```
Also upgrade album cover — in the Album mapper change `coverUrl = albumCover ?: ""` to `coverUrl = albumCover?.toHttpsOrSelf() ?: ""`.

- [ ] **Step 8: Write failing test — refresh preserves probed metadata**

`AudioRepositoryMetadataTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.AudioDao
import com.watnapp.buddhawajana.core.data.db.AudioEntity
import com.watnapp.buddhawajana.core.network.AudioService
import com.watnapp.buddhawajana.core.network.dto.AlbumDto
import com.watnapp.buddhawajana.core.network.dto.AudioDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioRepositoryMetadataTest {
    private val stored = mutableListOf<AudioEntity>()
    private val dao = object : AudioDao {
        override fun stream(albumId: Long): Flow<List<AudioEntity>> = flowOf(stored.filter { it.albumId == albumId })
        override suspend fun getAllOnce(albumId: Long) = stored.filter { it.albumId == albumId }
        override suspend fun upsertAll(items: List<AudioEntity>) {
            items.forEach { e -> stored.removeAll { it.audioId == e.audioId }; stored.add(e) }
        }
        override suspend fun count() = stored.size
        override suspend fun updateMetadata(audioId: Long, durationMs: Long?, sizeBytes: Long?) {
            stored.replaceAll { if (it.audioId == audioId) it.copy(durationMs = durationMs, sizeBytes = sizeBytes) else it }
        }
    }
    private val service = object : AudioService {
        override suspend fun getAlbums(): List<AlbumDto> = emptyList()
        override suspend fun getAudios(albumId: String) = listOf(AudioDto(id = "1", name = "T2", fileUrl = "http://x/1.mp3"))
    }

    @Test fun `refresh keeps probed duration and size`() = runTest {
        // Pre-existing row with probed metadata.
        stored.add(AudioEntity(audioId = 1, albumId = 9, title = "T1", url = "http://x/1.mp3", durationMs = 323_000, sizeBytes = 12_000_000))
        val repo = AudioRepository(dao, service)
        repo.refresh("9")
        val row = dao.getAllOnce(9).single()
        assertEquals("T2", row.title)               // server content applied
        assertEquals(323_000L, row.durationMs)        // probed metadata preserved
        assertEquals(12_000_000L, row.sizeBytes)
    }
}
```

- [ ] **Step 9: Run test, verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*AudioRepositoryMetadataTest*" -q`
Expected: FAIL — refresh's merge `copy(...)` does not carry `durationMs`/`sizeBytes`, so they reset to null.

- [ ] **Step 10: Preserve metadata in AudioRepository.refresh + add ensureMetadata**

In `AudioRepository.kt`: (a) add a `prober: MetadataProber` constructor param; (b) extend the merge `copy(...)`; (c) add `ensureMetadata`. New file body:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.common.runCatchingCancellable
import com.watnapp.buddhawajana.core.data.db.AudioDao
import com.watnapp.buddhawajana.core.data.download.MetadataProber
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.network.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cache-first repository for audios (per-album).
 *
 * Merge strategy (read-before-write): apply server content fields while KEEPING
 * user/device-owned columns — download state (status, progress, requestId) AND
 * probed metadata (durationMs, sizeBytes).
 */
class AudioRepository(
    private val dao: AudioDao,
    private val service: AudioService,
    private val prober: MetadataProber? = null,
) {
    fun stream(albumId: String): Flow<List<Audio>> =
        dao.stream(albumId.toLong()).map { entities -> entities.map { it.toModel() } }

    suspend fun refresh(albumId: String): Result<Unit> = runCatchingCancellable {
        val dtos = service.getAudios(albumId)
        val albumIdLong = albumId.toLong()
        val existing = dao.getAllOnce(albumIdLong).associateBy { it.audioId }
        val merged = dtos.map { dto ->
            val fresh = dto.toEntity(albumId)
            val current = existing[dto.id.toLong()]
            if (current != null) {
                fresh.copy(
                    status = current.status,
                    progress = current.progress,
                    requestId = current.requestId,
                    durationMs = current.durationMs,
                    sizeBytes = current.sizeBytes,
                )
            } else fresh
        }
        dao.upsertAll(merged)
    }

    /** Probe + persist duration/size for [audio] if not already known. No-op if probed or no prober. */
    suspend fun ensureMetadata(audio: Audio) {
        if (audio.durationMs != null && audio.sizeBytes != null) return
        val p = prober ?: return
        val (dur, size) = p.probe(audio.url)
        if (dur != null || size != null) {
            dao.updateMetadata(audio.id.toLong(), dur ?: audio.durationMs, size ?: audio.sizeBytes)
        }
    }
}
```

- [ ] **Step 11: Run the metadata + existing repo tests, verify pass**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*AudioRepository*" --tests "*MapperTest*" --tests "*MappersTest*" -q`
Expected: PASS. (The `prober` param defaults to null, so any existing `AudioRepository(dao, service)` construction still compiles.)

- [ ] **Step 12: Bump DB to v2 with migration**

In `AppDatabase.kt`: change `version = 1` to `version = 2`, add the dao accessor `abstract fun playbackProgressDao(): PlaybackProgressDao`, add `PlaybackProgressEntity::class` to `entities`, and add a migration companion constant:
```kotlin
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audio ADD COLUMN duration_ms INTEGER")
                db.execSQL("ALTER TABLE audio ADD COLUMN size_bytes INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS playback_progress (" +
                        "audio_id TEXT NOT NULL PRIMARY KEY, " +
                        "position_ms INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL)"
                )
            }
        }
```

- [ ] **Step 13: Verify compile (KSP regenerates Room)**

Run: `./gradlew :core:data:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 14: Commit**

```bash
git add core/data
git commit -m "feat(core:data): audio metadata columns + MetadataProber + DB v2 (TDD)

refresh merge now preserves probed duration/size alongside download state.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT6: `:core:data` — wire migration + new DI (DataModule)

**Files:**
- Modify: `core/data/.../DataModule.kt`

- [ ] **Step 1: Register migration on the database builder + new singletons**

In `DataModule.kt`, update the database `single { ... }` to add the migration and register the new dao/repo/prober:
```kotlin
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    // ... existing dao singles ...
    single { get<AppDatabase>().playbackProgressDao() }
    single { MetadataProber(get<okhttp3.OkHttpClient>()) }
    single { PlaybackProgressRepository(get()) }
```
Change `single { AudioRepository(get(), get()) }` to `single { AudioRepository(get(), get(), get()) }` (dao, service, prober). Add imports: `com.watnapp.buddhawajana.core.data.download.MetadataProber`, `com.watnapp.buddhawajana.core.data.repo.PlaybackProgressRepository`.

- [ ] **Step 2: Verify compile**

Run: `./gradlew :core:data:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/data
git commit -m "feat(core:data): wire DB v2 migration + PlaybackProgress/MetadataProber DI

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT7: `:core:player` module skeleton + SleepTimer/Speed helpers (TDD)

**Files:**
- Create: `core/player/build.gradle.kts`
- Create: `core/player/src/main/AndroidManifest.xml`
- Create: `core/player/.../player/PlaybackController.kt`
- Test: `core/player/src/test/java/com/watnapp/buddhawajana/core/player/SleepTimerTest.kt`
- Test: `core/player/src/test/java/com/watnapp/buddhawajana/core/player/PlaybackSpeedTest.kt`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Register module in settings**

In `settings.gradle.kts`, after `include(":core:ui")`:
```kotlin
include(":core:player")
```

- [ ] **Step 2: Create build.gradle.kts**

`core/player/build.gradle.kts`:
```kotlin
plugins {
    id("buddhawajana.android.library")
}
android { namespace = "com.watnapp.buddhawajana.core.player" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 3: Create AndroidManifest with the service declaration**

`core/player/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application>
        <service
            android:name=".player.PlaybackService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

- [ ] **Step 4: Write failing tests for pure helpers**

`PlaybackSpeedTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTest {
    @Test fun `presets are the iOS set`() =
        assertEquals(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f), PlaybackSpeed.PRESETS)

    @Test fun `clamp snaps to nearest preset`() {
        assertEquals(1.0f, PlaybackSpeed.clamp(0.9f))
        assertEquals(1.5f, PlaybackSpeed.clamp(1.6f))
        assertEquals(2.0f, PlaybackSpeed.clamp(5f))
    }
}
```
`SleepTimerTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    @Test fun `duration ticks down and clamps at zero`() {
        assertEquals(4_000L, tickRemaining(5_000L, 1_000L))
        assertEquals(0L, tickRemaining(500L, 1_000L))
    }

    @Test fun `expired only at or below zero`() {
        assertFalse(isExpired(1L))
        assertTrue(isExpired(0L))
    }
}
```

- [ ] **Step 5: Run tests, verify they fail**

Run: `./gradlew :core:player:testDebugUnitTest -q`
Expected: FAIL (unresolved references).

- [ ] **Step 6: Implement PlaybackController interface + state types + helpers**

`core/player/.../player/PlaybackController.kt`:
```kotlin
package com.watnapp.buddhawajana.core.player

import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import kotlinx.coroutines.flow.StateFlow

const val SKIP_DELTA_MS = 15_000L

data class NowPlaying(
    val audioId: String,
    val albumId: String,
    val title: String,
    val album: String,
    val artworkUrl: String?,
)

sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Duration(val remainingMs: Long) : SleepTimerState
    data object EndOfTrack : SleepTimerState
}

/** Decrement a sleep-timer remaining value, clamped at zero. Pure. */
fun tickRemaining(remainingMs: Long, deltaMs: Long): Long = (remainingMs - deltaMs).coerceAtLeast(0L)

/** True once a countdown has reached/passed zero. Pure. */
fun isExpired(remainingMs: Long): Boolean = remainingMs <= 0L

object PlaybackSpeed {
    val PRESETS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    fun clamp(rate: Float): Float = PRESETS.minByOrNull { kotlin.math.abs(it - rate) } ?: 1.0f
}

/** Playback surface consumed by ViewModels and the mini-player. Media3-backed in production, fakeable in tests. */
interface PlaybackController {
    val nowPlaying: StateFlow<NowPlaying?>
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val speed: StateFlow<Float>
    val sleepTimer: StateFlow<SleepTimerState>

    fun setQueue(album: Album, audios: List<Audio>, startIndex: Int)
    fun playPause()
    fun seekTo(ms: Long)
    fun skip(deltaMs: Long)
    fun next()
    fun prev()
    fun setSpeed(rate: Float)
    fun setSleepTimer(state: SleepTimerState)
    fun stop()
}
```

- [ ] **Step 7: Run tests, verify they pass**

Run: `./gradlew :core:player:testDebugUnitTest -q`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts core/player
git commit -m "feat(core:player): module skeleton + PlaybackController API + sleep/speed helpers (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT8: `:core:player` — PlaybackService (device-verified)

**Files:**
- Create: `core/player/.../player/PlaybackService.kt`

- [ ] **Step 1: Implement the MediaSessionService**

`core/player/.../player/PlaybackService.kt`:
```kotlin
package com.watnapp.buddhawajana.core.player

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Background audio host. Media3 auto-publishes the MediaStyle notification + lock-screen controls. */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && (!player.playWhenReady || player.mediaItemCount == 0)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :core:player:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/player
git commit -m "feat(core:player): PlaybackService (MediaSessionService + ExoPlayer)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT9: `:core:player` — PlaybackPrefs (speed persistence)

**Files:**
- Create: `core/player/.../player/PlaybackPrefs.kt`

- [ ] **Step 1: Implement DataStore-backed speed prefs**

`core/player/.../player/PlaybackPrefs.kt`:
```kotlin
package com.watnapp.buddhawajana.core.player

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore("playback_prefs")

class PlaybackPrefs(private val context: Context) {
    private val speedKey = floatPreferencesKey("speed")

    val speed: Flow<Float> = context.playbackDataStore.data.map { it[speedKey] ?: 1.0f }

    suspend fun setSpeed(rate: Float) {
        context.playbackDataStore.edit { it[speedKey] = PlaybackSpeed.clamp(rate) }
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :core:player:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/player
git commit -m "feat(core:player): PlaybackPrefs DataStore speed persistence

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT10: `:core:player` — MediaPlaybackController + Koin (device-verified)

**Files:**
- Create: `core/player/.../player/MediaPlaybackController.kt`
- Create: `core/player/.../player/PlayerModule.kt`

- [ ] **Step 1: Implement the Media3-backed controller**

`core/player/.../player/MediaPlaybackController.kt`:
```kotlin
package com.watnapp.buddhawajana.core.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.watnapp.buddhawajana.core.data.repo.PlaybackProgressRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Binds a MediaController to PlaybackService, exposes player state as StateFlows, and
 * forwards commands. Persists/restores playback position via PlaybackProgressRepository.
 *
 * Device-verified (MediaController requires the running service); not unit-tested.
 */
class MediaPlaybackController(
    private val context: Context,
    private val progress: PlaybackProgressRepository,
    private val prefs: PlaybackPrefs,
) : PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var sleepJob: kotlinx.coroutines.Job? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    private val _speed = MutableStateFlow(1.0f)
    override val speed: StateFlow<Float> = _speed.asStateFlow()
    private val _sleepTimer = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    override val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    init { connect() }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            scope.launch { prefs.speed.collect { rate -> _speed.value = rate; c.setPlaybackSpeed(rate) } }
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (!isPlaying) saveProgress()
                }
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    saveProgress()
                    updateNowPlaying()
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED && _sleepTimer.value is SleepTimerState.EndOfTrack) {
                        c.pause(); _sleepTimer.value = SleepTimerState.Off
                    }
                }
            })
            updateNowPlaying()
            startTicker()
        }, MoreExecutors.directExecutor())
    }

    private fun startTicker() = scope.launch {
        var sinceSaveMs = 0L
        while (true) {
            val c = controller
            if (c != null && c.isPlaying) {
                _positionMs.value = c.currentPosition
                _durationMs.value = c.duration.coerceAtLeast(0L)
                sinceSaveMs += 500
                if (sinceSaveMs >= 10_000) { saveProgress(); sinceSaveMs = 0 }
            }
            delay(500)
        }
    }

    private fun updateNowPlaying() {
        val c = controller ?: return
        val item = c.currentMediaItem
        if (item == null) { _nowPlaying.value = null; return }
        val md = item.mediaMetadata
        _nowPlaying.value = NowPlaying(
            audioId = item.mediaId,
            albumId = md.extras?.getString("albumId") ?: "",
            title = md.title?.toString() ?: "",
            album = md.albumTitle?.toString() ?: "",
            artworkUrl = md.artworkUri?.toString(),
        )
    }

    private fun saveProgress() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId ?: return
        val pos = c.currentPosition
        scope.launch { progress.save(id, pos) }
    }

    override fun setQueue(album: Album, audios: List<Audio>, startIndex: Int) {
        val c = controller ?: return
        val items = audios.map { audio ->
            MediaItem.Builder()
                .setMediaId(audio.id)
                .setUri(Uri.parse(audio.url))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audio.title)
                        .setAlbumTitle(album.title)
                        .setArtworkUri(album.coverUrl?.let(Uri::parse))
                        .setExtras(android.os.Bundle().apply { putString("albumId", album.id) })
                        .build(),
                )
                .build()
        }
        scope.launch {
            val resumeMs = progress.get(audios.getOrNull(startIndex)?.id ?: "")?.positionMs ?: 0L
            c.setMediaItems(items, startIndex, resumeMs)
            c.setPlaybackSpeed(_speed.value)
            c.prepare()
            c.play()
            updateNowPlaying()
        }
    }

    override fun playPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    override fun seekTo(ms: Long) { controller?.seekTo(ms.coerceAtLeast(0)) }
    override fun skip(deltaMs: Long) {
        val c = controller ?: return
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, c.duration.coerceAtLeast(0)))
    }
    override fun next() { controller?.seekToNextMediaItem() }
    override fun prev() { controller?.seekToPreviousMediaItem() }
    override fun setSpeed(rate: Float) {
        val r = PlaybackSpeed.clamp(rate)
        controller?.setPlaybackSpeed(r)
        _speed.value = r
        scope.launch { prefs.setSpeed(r) }
    }
    override fun setSleepTimer(state: SleepTimerState) {
        sleepJob?.cancel()
        _sleepTimer.value = state
        if (state is SleepTimerState.Duration) {
            sleepJob = scope.launch {
                var remaining = state.remainingMs
                while (!isExpired(remaining)) {
                    delay(1_000)
                    remaining = tickRemaining(remaining, 1_000)
                    _sleepTimer.value = SleepTimerState.Duration(remaining)
                }
                controller?.pause()
                _sleepTimer.value = SleepTimerState.Off
            }
        }
    }
    override fun stop() {
        saveProgress()
        sleepJob?.cancel()
        controller?.run { pause(); clearMediaItems() }
        _nowPlaying.value = null
        _sleepTimer.value = SleepTimerState.Off
    }
}
```

- [ ] **Step 2: Create Koin module**

`core/player/.../player/PlayerModule.kt`:
```kotlin
package com.watnapp.buddhawajana.core.player

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playerModule = module {
    single { PlaybackPrefs(androidContext()) }
    single<PlaybackController> { MediaPlaybackController(androidContext(), get(), get()) }
}
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :core:player:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/player
git commit -m "feat(core:player): MediaPlaybackController + playerModule (resume, sleep, speed)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT11: `:feature:audio` module skeleton + formatters (TDD)

**Files:**
- Create: `feature/audio/build.gradle.kts`
- Create: `feature/audio/src/main/AndroidManifest.xml`
- Create: `feature/audio/.../format/AudioFormat.kt`
- Test: `feature/audio/src/test/java/com/watnapp/buddhawajana/feature/audio/format/AudioFormatTest.kt`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Register module**

In `settings.gradle.kts`, after `include(":feature:books")`:
```kotlin
include(":feature:audio")
```

- [ ] **Step 2: Create build.gradle.kts (mirror :feature:books + add :core:player)**

`feature/audio/build.gradle.kts`:
```kotlin
plugins {
    id("buddhawajana.android.compose")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.watnapp.buddhawajana.feature.audio" }
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:player"))
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 3: Create AndroidManifest**

`feature/audio/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 4: Write failing formatter tests**

`AudioFormatTest.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.format

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFormatTest {
    @Test fun `formatTime under an hour is M colon SS`() {
        assertEquals("5:23", formatTime(323_000))
        assertEquals("0:05", formatTime(5_000))
        assertEquals("0:00", formatTime(-10))
    }

    @Test fun `formatTime over an hour is H colon MM colon SS`() =
        assertEquals("1:02:03", formatTime(3_723_000))

    @Test fun `formatRowMeta joins duration and size`() =
        assertEquals("5:23 · 12 MB", formatRowMeta(323_000, 12_000_000))

    @Test fun `formatRowMeta shows dash when nothing known`() =
        assertEquals("—", formatRowMeta(null, null))

    @Test fun `formatRowMeta tolerates partial metadata`() {
        assertEquals("5:23", formatRowMeta(323_000, null))
        assertEquals("12 MB", formatRowMeta(null, 12_000_000))
    }
}
```

- [ ] **Step 5: Run tests, verify they fail**

Run: `./gradlew :feature:audio:testDebugUnitTest -q`
Expected: FAIL (unresolved references).

- [ ] **Step 6: Implement formatters**

`feature/audio/.../format/AudioFormat.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.format

/** Milliseconds → "M:SS" (or "H:MM:SS" past an hour). Negatives clamp to "0:00". */
fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Row subtitle: "M:SS · X MB", any subset, or "—" when nothing is known yet. */
fun formatRowMeta(durationMs: Long?, sizeBytes: Long?): String {
    val parts = listOfNotNull(
        durationMs?.let { formatTime(it) },
        sizeBytes?.let { "%d MB".format((it / 1_000_000.0).toLong().coerceAtLeast(0)) },
    )
    return if (parts.isEmpty()) "—" else parts.joinToString(" · ")
}
```

- [ ] **Step 7: Run tests, verify they pass**

Run: `./gradlew :feature:audio:testDebugUnitTest -q`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts feature/audio
git commit -m "feat(feature:audio): module skeleton + time/size formatters (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT12: `:feature:audio` — AlbumsViewModel + AlbumsScreen (TDD)

**Files:**
- Create: `feature/audio/.../albums/AlbumsViewModel.kt`
- Create: `feature/audio/.../albums/AlbumsScreen.kt`
- Test: `feature/audio/src/test/java/com/watnapp/buddhawajana/feature/audio/albums/AlbumsViewModelTest.kt`

- [ ] **Step 1: Write failing VM test**

`AlbumsViewModelTest.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.albums

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.ui.state.UiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun repo(albums: List<Album>): AlbumRepository = mockk(relaxed = true) {
        every { stream() } returns flowOf(albums)
        coEvery { refresh() } returns Result.success(Unit)
    }

    @Test fun `emits content from stream`() = runTest {
        val vm = AlbumsViewModel(repo(listOf(Album("1", "ชุดA", null, 3, 0))))
        vm.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = awaitItem()
            assertTrue(content is UiState.Content && content.data.single().title == "ชุดA")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `search filters by title case-insensitively`() = runTest {
        val vm = AlbumsViewModel(repo(listOf(
            Album("1", "ตถาคต", null, 1, 0),
            Album("2", "ภิกษุ", null, 1, 0),
        )))
        vm.onSearch("ตถา")
        vm.state.test {
            var last: UiState<List<Album>>? = null
            repeat(3) { last = awaitItem() }
            assertTrue(last is UiState.Content && (last as UiState.Content).data.single().id == "1")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*AlbumsViewModelTest*" -q`
Expected: FAIL (unresolved `AlbumsViewModel`).

- [ ] **Step 3: Implement AlbumsViewModel (mirror BookListViewModel)**

`feature/audio/.../albums/AlbumsViewModel.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.albums

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumsViewModel(private val repo: AlbumRepository) : BaseViewModel() {
    private val query = MutableStateFlow("")
    val queryState: StateFlow<String> = query.asStateFlow()
    fun onSearch(q: String) { query.value = q }

    val state: StateFlow<UiState<List<Album>>> =
        combine(repo.stream(), query) { albums, q ->
            val filtered = if (q.isBlank()) albums
                else albums.filter { it.title.contains(q.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) UiState.Empty else UiState.Content(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        repo.refresh().onFailure { emitMessage("รีเฟรชล้มเหลว") }
    }
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*AlbumsViewModelTest*" -q`
Expected: PASS.

- [ ] **Step 5: Implement AlbumsScreen**

`feature/audio/.../albums/AlbumsScreen.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.designsystem.component.EmptyView
import com.watnapp.buddhawajana.core.designsystem.component.ErrorView
import com.watnapp.buddhawajana.core.designsystem.component.LoadingView
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.ui.state.UiState

@Composable
fun AlbumsScreen(
    state: UiState<List<Album>>,
    query: String,
    onSearch: (String) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onSearch,
            singleLine = true,
            label = { Text("ค้นหาชุดเสียง") },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Empty -> EmptyView("ไม่พบชุดเสียง")
            is UiState.Error -> ErrorView(state.message)
            is UiState.Content -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.data, key = { it.id }) { album ->
                    AlbumCard(album, onClick = { onOpenAlbum(album) })
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(Modifier.padding(8.dp).clickable(onClick = onClick)) {
        CachedAsyncImage(
            url = album.coverUrl,
            contentDescription = album.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Text(album.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        Text("${album.itemCount} ตอน", style = MaterialTheme.typography.bodySmall)
    }
}
```
> NOTE: Confirm the exact composable names/signatures in `:core:designsystem` (`CachedAsyncImage`, `LoadingView`, `EmptyView`, `ErrorView` in `component/StateViews.kt` / `component/CachedAsyncImage.kt`) and adapt the calls to match (e.g. parameter name for the image URL). Mirror how `:feature:books` `BookListScreen.kt` uses them.

- [ ] **Step 6: Verify compile**

Run: `./gradlew :feature:audio:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add feature/audio
git commit -m "feat(feature:audio): AlbumsViewModel + AlbumsScreen (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT13: `:feature:audio` — AudioListViewModel + AudioListScreen + lazy probe (TDD)

**Files:**
- Create: `feature/audio/.../list/AudioListViewModel.kt`
- Create: `feature/audio/.../list/AudioListScreen.kt`
- Test: `feature/audio/src/test/java/com/watnapp/buddhawajana/feature/audio/list/AudioListViewModelTest.kt`

- [ ] **Step 1: Write failing VM test (stream + search + probe trigger + playing highlight)**

`AudioListViewModelTest.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.list

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.repo.AudioRepository
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.state.UiState
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun controller(): PlaybackController = mockk(relaxed = true) {
        every { nowPlaying } returns MutableStateFlow(null)
    }

    @Test fun `content streamed and search filters`() = runTest {
        val repo: AudioRepository = mockk(relaxed = true) {
            every { stream("9") } returns flowOf(listOf(
                Audio("1", "9", "ตอนหนึ่ง", "http://x/1.mp3"),
                Audio("2", "9", "ตอนสอง", "http://x/2.mp3"),
            ))
            coEvery { refresh("9") } returns Result.success(Unit)
        }
        val vm = AudioListViewModel("9", repo, controller())
        vm.onSearch("สอง")
        vm.state.test {
            var last: UiState<List<Audio>>? = null
            repeat(3) { last = awaitItem() }
            assertTrue(last is UiState.Content && (last as UiState.Content).data.single().id == "2")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `onRowVisible probes metadata for unprobed rows`() = runTest {
        val audio = Audio("1", "9", "ตอนหนึ่ง", "http://x/1.mp3", durationMs = null, sizeBytes = null)
        val repo: AudioRepository = mockk(relaxed = true) {
            every { stream("9") } returns flowOf(listOf(audio))
            coEvery { refresh("9") } returns Result.success(Unit)
            coEvery { ensureMetadata(any()) } returns Unit
        }
        val vm = AudioListViewModel("9", repo, controller())
        vm.onRowVisible(audio)
        advanceUntilIdle()
        coVerify { repo.ensureMetadata(audio) }
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*AudioListViewModelTest*" -q`
Expected: FAIL (unresolved `AudioListViewModel`).

- [ ] **Step 3: Implement AudioListViewModel**

`feature/audio/.../list/AudioListViewModel.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.list

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.AudioRepository
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioListViewModel(
    private val albumId: String,
    private val repo: AudioRepository,
    val controller: PlaybackController,
) : BaseViewModel() {

    private val query = MutableStateFlow("")
    val queryState: StateFlow<String> = query.asStateFlow()
    fun onSearch(q: String) { query.value = q }

    val nowPlaying = controller.nowPlaying

    val state: StateFlow<UiState<List<Audio>>> =
        combine(repo.stream(albumId), query) { audios, q ->
            val filtered = if (q.isBlank()) audios
                else audios.filter { it.title.contains(q.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) UiState.Empty else UiState.Content(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val probing = mutableSetOf<String>()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        repo.refresh(albumId).onFailure { emitMessage("รีเฟรชล้มเหลว") }
    }

    /** Called when a row first appears; probes duration/size once per audio. */
    fun onRowVisible(audio: Audio) {
        if (audio.durationMs != null && audio.sizeBytes != null) return
        if (!probing.add(audio.id)) return
        viewModelScope.launch { repo.ensureMetadata(audio) }
    }

    /** Start the album as a queue at [index] and signal the host to open the player. */
    fun play(album: com.watnapp.buddhawajana.core.model.Album, audios: List<Audio>, index: Int) {
        controller.setQueue(album, audios, index)
    }
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*AudioListViewModelTest*" -q`
Expected: PASS.

- [ ] **Step 5: Implement AudioListScreen**

`feature/audio/.../list/AudioListScreen.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.EmptyView
import com.watnapp.buddhawajana.core.designsystem.component.ErrorView
import com.watnapp.buddhawajana.core.designsystem.component.LoadingView
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.ui.state.UiState
import com.watnapp.buddhawajana.feature.audio.format.formatRowMeta

@Composable
fun AudioListScreen(
    state: UiState<List<Audio>>,
    query: String,
    playingAudioId: String?,
    onSearch: (String) -> Unit,
    onRowVisible: (Audio) -> Unit,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onSearch,
            singleLine = true,
            label = { Text("ค้นหาเสียง") },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Empty -> EmptyView("ไม่พบไฟล์เสียง")
            is UiState.Error -> ErrorView(state.message)
            is UiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.data, key = { it.id }) { audio ->
                    LaunchedEffect(audio.id) { onRowVisible(audio) }
                    val index = state.data.indexOf(audio)
                    ListItem(
                        headlineContent = {
                            Text(
                                audio.title,
                                color = if (audio.id == playingAudioId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = { Text(formatRowMeta(audio.durationMs, audio.sizeBytes)) },
                        modifier = Modifier.clickable { onPlay(index) },
                    )
                }
            }
        }
    }
}
```
> NOTE: Match `LoadingView`/`EmptyView`/`ErrorView` to the real signatures in `:core:designsystem` (see how `:feature:books` `BookListScreen` invokes them) and adjust if needed.

- [ ] **Step 6: Verify compile**

Run: `./gradlew :feature:audio:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add feature/audio
git commit -m "feat(feature:audio): AudioListViewModel + screen + lazy metadata probe (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT14: `:feature:audio` — PlayerViewModel + PlayerScreen (TDD for command forwarding)

**Files:**
- Create: `feature/audio/.../player/PlayerViewModel.kt`
- Create: `feature/audio/.../player/PlayerScreen.kt`
- Test: `feature/audio/src/test/java/com/watnapp/buddhawajana/feature/audio/player/PlayerViewModelTest.kt`

- [ ] **Step 1: Write failing VM test (forwards commands to a fake controller)**

`PlayerViewModelTest.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.player

import com.watnapp.buddhawajana.core.player.NowPlaying
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class PlayerViewModelTest {
    private val controller: PlaybackController = mockk(relaxed = true) {
        io.mockk.every { nowPlaying } returns MutableStateFlow<NowPlaying?>(null)
        io.mockk.every { isPlaying } returns MutableStateFlow(false)
        io.mockk.every { positionMs } returns MutableStateFlow(0L)
        io.mockk.every { durationMs } returns MutableStateFlow(0L)
        io.mockk.every { speed } returns MutableStateFlow(1.0f)
        io.mockk.every { sleepTimer } returns MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    }

    @Test fun `skip forward and back use the 15s delta`() {
        val vm = PlayerViewModel(controller)
        vm.skipForward(); verify { controller.skip(SKIP_DELTA_MS) }
        vm.skipBack(); verify { controller.skip(-SKIP_DELTA_MS) }
    }

    @Test fun `playPause and seek forward to controller`() {
        val vm = PlayerViewModel(controller)
        vm.playPause(); verify { controller.playPause() }
        vm.seekTo(1234L); verify { controller.seekTo(1234L) }
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q`
Expected: FAIL (unresolved `PlayerViewModel`).

- [ ] **Step 3: Implement PlayerViewModel (thin forwarding over the controller)**

`feature/audio/.../player/PlayerViewModel.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.player

import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import com.watnapp.buddhawajana.core.ui.BaseViewModel

class PlayerViewModel(private val controller: PlaybackController) : BaseViewModel() {
    val nowPlaying = controller.nowPlaying
    val isPlaying = controller.isPlaying
    val positionMs = controller.positionMs
    val durationMs = controller.durationMs
    val speed = controller.speed
    val sleepTimer = controller.sleepTimer

    fun playPause() = controller.playPause()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun skipForward() = controller.skip(SKIP_DELTA_MS)
    fun skipBack() = controller.skip(-SKIP_DELTA_MS)
    fun next() = controller.next()
    fun prev() = controller.prev()
    fun setSpeed(rate: Float) = controller.setSpeed(rate)
    fun setSleepTimer(state: SleepTimerState) = controller.setSleepTimer(state)
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :feature:audio:testDebugUnitTest --tests "*PlayerViewModelTest*" -q`
Expected: PASS.

- [ ] **Step 5: Implement PlayerScreen**

`feature/audio/.../player/PlayerScreen.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.feature.audio.format.formatTime

@Composable
fun PlayerScreen(vm: PlayerViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val now by vm.nowPlaying.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val position by vm.positionMs.collectAsState()
    val duration by vm.durationMs.collectAsState()

    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        CachedAsyncImage(
            url = now?.artworkUrl,
            contentDescription = now?.title,
            modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now?.title ?: "", style = MaterialTheme.typography.titleLarge)
            Text(now?.album ?: "", style = MaterialTheme.typography.bodyMedium)
        }

        // Draft-state scrubber: follows playback unless the user is dragging.
        var dragging by remember { mutableStateOf(false) }
        var draft by remember { mutableStateOf(0f) }
        val shown = if (dragging) draft else position.toFloat()
        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = shown,
                valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                onValueChange = { dragging = true; draft = it },
                onValueChangeFinished = { vm.seekTo(draft.toLong()); dragging = false },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(shown.toLong()))
                Text(formatTime(duration))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = vm::prev) { Icon(Icons.Default.SkipPrevious, "ก่อนหน้า") }
            IconButton(onClick = vm::skipBack) { Icon(Icons.Default.Replay30, "ถอย 15 วิ") }
            IconButton(onClick = vm::playPause) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "เล่น/หยุด")
            }
            IconButton(onClick = vm::skipForward) { Icon(Icons.Default.Forward30, "เดิน 15 วิ") }
            IconButton(onClick = vm::next) { Icon(Icons.Default.SkipNext, "ถัดไป") }
        }

        SpeedAndSleepRow(vm)
    }
}
```
Add `SpeedAndSleepRow` in the same file:
```kotlin
@Composable
private fun SpeedAndSleepRow(vm: PlayerViewModel) {
    val speed by vm.speed.collectAsState()
    val sleep by vm.sleepTimer.collectAsState()
    var speedOpen by remember { mutableStateOf(false) }
    var sleepOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        androidx.compose.material3.Box {
            androidx.compose.material3.TextButton(onClick = { speedOpen = true }) { Text("${speed}x") }
            androidx.compose.material3.DropdownMenu(expanded = speedOpen, onDismissRequest = { speedOpen = false }) {
                com.watnapp.buddhawajana.core.player.PlaybackSpeed.PRESETS.forEach { r ->
                    androidx.compose.material3.DropdownMenuItem(text = { Text("${r}x") }, onClick = { vm.setSpeed(r); speedOpen = false })
                }
            }
        }
        androidx.compose.material3.Box {
            val label = when (val s = sleep) {
                is com.watnapp.buddhawajana.core.player.SleepTimerState.Duration -> formatTime(s.remainingMs)
                com.watnapp.buddhawajana.core.player.SleepTimerState.EndOfTrack -> "จบตอน"
                com.watnapp.buddhawajana.core.player.SleepTimerState.Off -> "ตั้งเวลา"
            }
            androidx.compose.material3.TextButton(onClick = { sleepOpen = true }) { Text(label) }
            androidx.compose.material3.DropdownMenu(expanded = sleepOpen, onDismissRequest = { sleepOpen = false }) {
                listOf(15, 30, 45, 60).forEach { min ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("$min นาที") },
                        onClick = { vm.setSleepTimer(com.watnapp.buddhawajana.core.player.SleepTimerState.Duration(min * 60_000L)); sleepOpen = false },
                    )
                }
                androidx.compose.material3.DropdownMenuItem(text = { Text("จบตอนนี้") }, onClick = { vm.setSleepTimer(com.watnapp.buddhawajana.core.player.SleepTimerState.EndOfTrack); sleepOpen = false })
                androidx.compose.material3.DropdownMenuItem(text = { Text("ปิด") }, onClick = { vm.setSleepTimer(com.watnapp.buddhawajana.core.player.SleepTimerState.Off); sleepOpen = false })
            }
        }
    }
}
```
> NOTE: imports for `Box`, `TextButton`, `DropdownMenu`, `DropdownMenuItem` are fully-qualified above for clarity — convert to top-of-file imports. Verify `material-icons-extended` provides `Forward30`/`Replay30`/`Pause`/`SkipNext`/`SkipPrevious` (it does); if a 15s-specific glyph is preferred, the label/`contentDescription` already says 15.

- [ ] **Step 6: Verify compile**

Run: `./gradlew :feature:audio:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add feature/audio
git commit -m "feat(feature:audio): PlayerViewModel + PlayerScreen (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT15: `:feature:audio` — audioModule (Koin) + AudioPane (nested nav)

**Files:**
- Create: `feature/audio/.../AudioModule.kt`
- Create: `feature/audio/.../navigation/AudioPane.kt`

- [ ] **Step 1: Create Koin module**

`feature/audio/.../AudioModule.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio

import com.watnapp.buddhawajana.feature.audio.albums.AlbumsViewModel
import com.watnapp.buddhawajana.feature.audio.list.AudioListViewModel
import com.watnapp.buddhawajana.feature.audio.player.PlayerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val audioModule = module {
    viewModel { AlbumsViewModel(get()) }
    viewModel { (albumId: String) -> AudioListViewModel(albumId, get(), get()) }
    viewModel { PlayerViewModel(get()) }
}
```

- [ ] **Step 2: Create AudioPane (nested NavHost: albums ↔ audio list)**

`feature/audio/.../navigation/AudioPane.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.audio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.ui.state.UiState
import com.watnapp.buddhawajana.feature.audio.albums.AlbumsScreen
import com.watnapp.buddhawajana.feature.audio.albums.AlbumsViewModel
import com.watnapp.buddhawajana.feature.audio.list.AudioListScreen
import com.watnapp.buddhawajana.feature.audio.list.AudioListViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable private data object AlbumsRoute
@Serializable private data class AudioListRoute(val albumId: String, val albumTitle: String)

/**
 * Browse pane for the AUDIO tab. Hosts albums → audio-list internally so the bottom
 * nav + mini-player stay visible. [onOpenPlayer] asks the app NavHost to push the
 * full-screen player after a queue has been set.
 */
@Composable
fun AudioPane(onOpenPlayer: () -> Unit, modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = AlbumsRoute, modifier = modifier) {
        composable<AlbumsRoute> {
            val vm: AlbumsViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            val query by vm.queryState.collectAsState()
            AlbumsScreen(
                state = state, query = query,
                onSearch = vm::onSearch,
                onOpenAlbum = { album -> nav.navigate(AudioListRoute(album.id, album.title)) },
            )
        }
        composable<AudioListRoute> { entry ->
            val route = entry.toRoute<AudioListRoute>()
            val vm: AudioListViewModel = koinViewModel { parametersOf(route.albumId) }
            val state by vm.state.collectAsState()
            val query by vm.queryState.collectAsState()
            val now by vm.nowPlaying.collectAsState()
            AudioListScreen(
                state = state, query = query,
                playingAudioId = now?.audioId,
                onSearch = vm::onSearch,
                onRowVisible = vm::onRowVisible,
                onPlay = { index ->
                    val list = (state as? UiState.Content)?.data ?: return@AudioListScreen
                    vm.play(Album(route.albumId, route.albumTitle, now?.artworkUrl, list.size, 0), list, index)
                    onOpenPlayer()
                },
            )
        }
    }
}
```
> NOTE: the `Album(...)` constructed for `play` only needs `id`/`title`/`coverUrl` for metadata; `coverUrl` here falls back to the now-playing artwork. If the albums list already carries the cover, thread the real `Album` through instead (e.g. stash the selected `Album` in the VM). Acceptable simplification for V1.

- [ ] **Step 3: Verify compile**

Run: `./gradlew :feature:audio:assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add feature/audio
git commit -m "feat(feature:audio): audioModule + AudioPane nested navigation

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT16: `:app` — integrate Audio (mini-player, nav, manifest, Koin) + retire legacy audio

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/watnapp/buddhawajana/MainApplication.kt`
- Modify: `app/src/main/java/com/watnapp/buddhawajana/navigation/BuddhawajanaNavHost.kt`
- Modify: `app/src/main/java/com/watnapp/buddhawajana/navigation/HomeScaffold.kt`
- Create: `app/src/main/java/com/watnapp/buddhawajana/navigation/MiniPlayer.kt`
- Delete (after reference check): `app/.../ui/AudioScreen.kt`, `app/.../ui/Mp3PlayerActivity.kt`, `app/.../vm/AudioViewModel.kt`, `app/.../vm/AlbumViewModel.kt`, legacy `repository/AudioRepository.kt` + `repository/AlbumRepository.kt` (only if unreferenced elsewhere)

- [ ] **Step 1: Add module deps**

In `app/build.gradle.kts` `dependencies {}`, under the foundation modules:
```kotlin
    implementation(project(":core:player"))
    implementation(project(":feature:audio"))
    implementation(libs.media3.session)
```

- [ ] **Step 2: Manifest — perms + service merge**

In `app/src/main/AndroidManifest.xml`, add below the existing INTERNET permission:
```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
The `<service>` is declared in `:core:player`'s manifest and merges automatically — do **not** redeclare it here. Remove the legacy `<activity ... .ui.Mp3PlayerActivity .../>` entry (deleted in Step 8).

- [ ] **Step 3: Register Koin modules**

In `MainApplication.kt`, add imports for `com.watnapp.buddhawajana.core.player.playerModule` and `com.watnapp.buddhawajana.feature.audio.audioModule`, and change the `modules(...)` line to:
```kotlin
            modules(networkModule, dataModule, playerModule, audioModule, booksModule, appModule)
```
In `appModule`, remove the legacy audio/album DI lines (`AlbumRepository`, `AlbumViewModel`, `AudioRepository`, `AudioViewModel`) and their imports.

- [ ] **Step 4: Create the MiniPlayer composable**

`app/src/main/java/com/watnapp/buddhawajana/navigation/MiniPlayer.kt`:
```kotlin
package com.watnapp.buddhawajana.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.player.PlaybackController

@Composable
fun MiniPlayer(controller: PlaybackController, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val now by controller.nowPlaying.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val position by controller.positionMs.collectAsState()
    val duration by controller.durationMs.collectAsState()
    val track = now ?: return

    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand).padding(8.dp)) {
                CachedAsyncImage(url = track.artworkUrl, contentDescription = track.title, modifier = Modifier.size(40.dp))
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                IconButton(onClick = controller::playPause) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "เล่น/หยุด")
                }
                IconButton(onClick = controller::stop) { Icon(Icons.Default.Close, "ปิด") }
            }
            LinearProgressIndicator(
                progress = { if (duration > 0) position.toFloat() / duration else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

- [ ] **Step 5: Render MiniPlayer in HomeScaffold + swap AUDIO tab to AudioPane**

In `HomeScaffold.kt`: add `onOpenPlayer: () -> Unit` param; inject the controller via `org.koin.compose.koinInject`; render `MiniPlayer` in the `Scaffold`'s `bottomBar` (above the nav suite's bottom bar it will appear stacked — acceptable for V1) OR at the bottom of the content `Box`. Use the content approach for simplicity:
```kotlin
@Composable
fun HomeScaffold(onOpenBook: (Long) -> Unit, onOpenPlayer: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(TopDestination.AUDIO) }
    val controller: com.watnapp.buddhawajana.core.player.PlaybackController = org.koin.compose.koinInject()
    Scaffold(
        topBar = { BuddhawajanaTopBar(title = "พุทธวจน", onSettingsClick = { }) }
    ) { innerPadding ->
        BuddhawajanaNavSuite(selected = selected, onSelect = { selected = it }) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(innerPadding)) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selected) {
                        TopDestination.AUDIO -> com.watnapp.buddhawajana.feature.audio.navigation.AudioPane(onOpenPlayer = onOpenPlayer)
                        TopDestination.BOOKS -> BooksListPane(onOpenBook = onOpenBook)
                        TopDestination.YOUTUBE -> YoutubeScreen()
                    }
                }
                MiniPlayer(controller = controller, onExpand = onOpenPlayer)
            }
        }
    }
}
```
Remove the now-unused `AudioScreen`/`WindowSize`/`rememberWindowSizeClass` imports and the `windowSize` plumbing (the legacy AudioScreen is being deleted). Keep `YoutubeScreen` import.

- [ ] **Step 6: Add PlayerRoute to the app NavHost**

In `BuddhawajanaNavHost.kt`: add `@Serializable private data object PlayerRoute`. Pass `onOpenPlayer = { nav.navigate(PlayerRoute) }` to `HomeScaffold`. Add the composable:
```kotlin
        composable<PlayerRoute> {
            val vm: com.watnapp.buddhawajana.feature.audio.player.PlayerViewModel = koinViewModel()
            com.watnapp.buddhawajana.feature.audio.player.PlayerScreen(vm = vm, onBack = { nav.popBackStack() })
        }
```

- [ ] **Step 7: Request POST_NOTIFICATIONS at runtime (API 33+)**

In `MainActivity.kt`, register a permission launcher and request on first resume (so Media3's notification can show):
```kotlin
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
```
Place this in `onCreate` before `setContent`. (If denied, playback still works without a notification.)

- [ ] **Step 8: Reference-check then delete legacy audio classes**

Run reference checks; delete only files with no remaining references:
```bash
grep -rn "AudioScreen\|Mp3PlayerActivity\|AudioViewModel\|AlbumViewModel" app/src/main --include=*.kt | grep -v "core.player\|feature.audio"
```
For each legacy class with no non-legacy references, delete the file. Expected deletions: `ui/AudioScreen.kt`, `ui/Mp3PlayerActivity.kt`, `vm/AudioViewModel.kt`, `vm/AlbumViewModel.kt`. Check `repository/AudioRepository.kt` + `repository/AlbumRepository.kt` and `vm/DownloadableViewModel.kt` references — delete the audio/album legacy repos only if nothing else (e.g. YouTube/Book legacy) uses them; otherwise leave them. Do **not** remove `arg.player`, `accompanist`, etc. from the catalog yet — only drop `implementation(libs.arg.player)` from `app/build.gradle.kts` if `Mp3PlayerActivity` (its only user) is deleted.

- [ ] **Step 9: Build the whole app**

Run: `./gradlew :app:assembleDebug -q`
Expected: BUILD SUCCESSFUL. If unresolved-reference errors point at a legacy class still in use, restore that file (revert its deletion) and adjust.

- [ ] **Step 10: Run all unit tests**

Run: `./gradlew testDebugUnitTest :core:common:test :core:player:testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL (all green).

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat(app): integrate Audio vertical — mini-player, nav, MediaSession service; retire legacy audio

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task AT17: Device smoke verification (manual)

No code. Install on a device and walk the checklist; file any failures as systematic-debugging fixes (the engine/service/gesture layer is where runtime-only bugs live).

- [ ] Browse albums → open album → rows appear; duration/size fill in as rows scroll into view.
- [ ] Tap an audio → full player opens, streams, plays.
- [ ] Background app / lock screen → playback continues; notification + lock-screen transport controls work (play/pause, ±15s, next/prev).
- [ ] Headset/Bluetooth buttons work; unplugging headphones pauses.
- [ ] Change speed → persists across track changes and an app restart.
- [ ] Sleep timer (a duration + "จบตอนนี้") pauses correctly; countdown label updates.
- [ ] Mini-player appears while playing, persists across Books↔Audio tabs, tap expands to player, ✕ stops.
- [ ] Close + reopen an audio → resumes near the saved position.
- [ ] Incoming call pauses; afterwards playback resumes/holds correctly.
- [ ] Empty/failed album shows error + retry; mid-stream network drop surfaces an error.

---

## Self-Review (author checklist — completed)

- **Spec coverage:** browse+search (AT12/AT13), streaming player + controls + speed + sleep + ±15s + auto-advance (AT8/AT10/AT14), background + lock-screen (AT8/AT10 + manifest AT16), resume position (AT4/AT10), mini-player (AT16), per-row metadata (AT5/AT13), https/whitespace quirk (AT3/AT5), edge cases (AT8 audio focus/becoming-noisy; AT10 throttled save; AT13 error states). Downloads/favorites correctly absent (deferred). ✔
- **Placeholder scan:** no TBD/TODO; every code step has full code. Two `> NOTE:` callouts ask the implementer to match real `:core:designsystem` signatures and thread the real `Album` — these are verification reminders, not missing code. ✔
- **Type consistency:** `PlaybackController` surface (flows + `setQueue`/`playPause`/`seekTo`/`skip`/`next`/`prev`/`setSpeed`/`setSleepTimer`/`stop`) is identical across AT7 (interface), AT10 (impl), AT13/AT14 (consumers), AT16 (mini-player). `SKIP_DELTA_MS`, `SleepTimerState`, `PlaybackSpeed`, `NowPlaying` defined once in AT7. `AudioRepository` 3-arg ctor (AT5) matches DI (AT6). `Audio` nullable metadata (AT2) matches mapper (AT5) + formatters (AT11). ✔
```
