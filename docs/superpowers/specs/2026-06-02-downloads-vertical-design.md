# Downloads Vertical — Design

Date: 2026-06-02
Status: Approved (brainstorming)
Branch: `feature/downloads-vertical`

## Goal

Let users download audio tracks for offline listening — per-track (player + list rows) and
per-album batch — browse them in a Downloads screen reached from a pinned card, with offline
playback preferring the local file. Reaches parity with the iOS DownloadEngine + DownloadButton +
Downloads screen.

## Scope decisions (locked in brainstorming)

- **Engine: WorkManager.** Downloads run as background work that survives app-kill, auto-retries,
  and respects a network constraint. (Chosen over an in-app coroutine queue for robustness.)
- **Batch included.** Per-track download PLUS "download all" for an album (with a count/size confirm).
- **State model:** a denormalized `DownloadEntity` (persistent, written on completion) merged with
  live WorkManager `WorkInfo` (transient queued/running/failed) into one `DownloadState` the UI observes.
- **Worker DI:** `DownloadWorker` resolves its dependencies via Koin `GlobalContext.get()` inside
  `doWork()` (no custom `WorkerFactory`).
- **Offline playback:** `MediaPlaybackController.setQueue` plays from the local file when downloaded.

Out of scope: pause/resume mid-file, wifi-only setting, auto-download-on-favorite, book downloads.

## Architecture

Two state layers merged:
- **Transient** (queued / downloading% / failed) from WorkManager `WorkInfo` — not persisted.
- **Persistent** `DownloadEntity` (denormalized snapshot, like `Favorite`) written by the worker on
  success — powers the Downloads screen + offline resolution without joins.

`DownloadRepository.state(audioId): Flow<DownloadState>` merges them. Engine code lives in
`:core:data` (it already owns `FileDownloader`, OkHttp, Room); UI in `:feature:audio`; offline
file resolution in `:core:player`.

### Module deltas

| Module | Change |
|--------|--------|
| `:core:model` | new `Download` |
| `:core:data` | WorkManager dep; `AudioFileStore`; `DownloadEntity`/`Dao`; `DownloadWorker`; `DownloadRepository`; `DownloadState`; mapper; DB v3→v4 + migration; DI |
| `:core:player` | `setQueue` plays local file when present; `NowPlaying.isLocal`; resolver wiring |
| `:feature:audio` | `DownloadButton`; player action-row + audio-row buttons; album batch; `DownloadsScreen`/VM; pinned ⬇ card; `DownloadsRoute` |
| `:app` | `FOREGROUND_SERVICE_DATA_SYNC` perm; "downloads" notification channel |

## Section 1 — data layer (`:core:data`)

Add dep `libs.work.runtime.ktx` (catalog already defines it).

**`AudioFileStore`** (mirror `BookFileStore`):
```kotlin
class AudioFileStore(private val context: Context) {
    private fun dir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "buddhawajana/audios").apply { mkdirs() }
    fun file(audioId: String): File = File(dir(), "$audioId.mp3")
    fun exists(audioId: String): Boolean = file(audioId).let { it.exists() && it.length() > 0 }
    fun delete(audioId: String): Boolean = file(audioId).delete()
}
```

**`DownloadEntity`** table `download`, PK `audio_id` (TEXT), cols `title`, `url`, `album_id`,
`album_title`, `cover_url` (nullable), `file_name`, `size_bytes` (Long), `completed_at` (Long).

**`DownloadDao`:**
- `@Query("SELECT * FROM download ORDER BY completed_at DESC") fun stream(): Flow<List<DownloadEntity>>`
- `@Query("SELECT * FROM download WHERE audio_id = :id") suspend fun get(id: String): DownloadEntity?`
- `@Query("SELECT EXISTS(SELECT 1 FROM download WHERE audio_id = :id)") fun existsFlow(id: String): Flow<Boolean>`
- `@Insert(onConflict = REPLACE) suspend fun insert(e: DownloadEntity)`
- `@Query("DELETE FROM download WHERE audio_id = :id") suspend fun deleteById(id: String)`

**`DownloadState`:**
```kotlin
sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data object Queued : DownloadState
    data class Downloading(val fraction: Float) : DownloadState
    data object Downloaded : DownloadState
    data object Failed : DownloadState
}
```

**`Download`** model (`:core:model`): `audioId, title, url, albumId, albumTitle, coverUrl: String?, sizeBytes: Long, completedAt: Long`. Mapper `DownloadEntity.toModel()` only (the worker writes `DownloadEntity` directly; UI only reads — no `Download.toEntity()` needed).

**DB v3→v4:** add `DownloadEntity::class`, `downloadDao()`, `MIGRATION_3_4`:
```sql
CREATE TABLE IF NOT EXISTS download (
    audio_id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL, url TEXT NOT NULL,
    album_id TEXT NOT NULL, album_title TEXT NOT NULL, cover_url TEXT,
    file_name TEXT NOT NULL, size_bytes INTEGER NOT NULL, completed_at INTEGER NOT NULL)
```
`DataModule`: `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`; singles for `downloadDao()`, `AudioFileStore`, `DownloadRepository`.

## Section 2 — download engine (`:core:data`)

**`DownloadWorker : CoroutineWorker(appContext, params)`** — input keys: `audioId, url, title, albumId, albumTitle, coverUrl`. Companion: `KEY_FRACTION = "fraction"`, plus the input keys + a builder `inputData(audio, album)`.
`doWork()`:
1. Resolve via `org.koin.core.context.GlobalContext.get()`: `OkHttpClient`, `DownloadDao`, `AudioFileStore`.
2. `setForeground(foregroundInfo())` — notification on channel `"downloads"` ("กำลังดาวน์โหลด").
3. `FileDownloader(client).download(url, files.file(audioId)).collect { p -> when (p) { is Progress -> setProgress(workDataOf(KEY_FRACTION to p.fraction)); is Failed -> throw p.error; Done -> {} } }`.
4. Success → `dao.insert(DownloadEntity(audioId, title, url, albumId, albumTitle, coverUrl, fileName = file.name, sizeBytes = file.length(), completedAt = System.currentTimeMillis()))` → `Result.success()`.
5. Catch → if `runAttemptCount < 3` `Result.retry()` else `Result.failure()`. (FileDownloader already deletes the `.part` tmp on failure.)

**`DownloadRepository(context, dao, files)`:**
- `fun enqueue(audio: Audio, album: Album)`: `OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(DownloadWorker.inputData(audio, album)).setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED)).build()` → `WorkManager.getInstance(context).enqueueUniqueWork("download_${audio.id}", ExistingWorkPolicy.KEEP, req)`.
- `fun enqueueAll(album: Album, audios: List<Audio>)`: enqueue each not-already-downloaded audio (KEEP).
- `fun cancel(audioId: String)`: `cancelUniqueWork("download_$audioId")`.
- `suspend fun delete(audioId: String)`: cancel + `files.delete(audioId)` + `dao.deleteById(audioId)`.
- `val downloads: Flow<List<Download>>` = `dao.stream().map { it.map(::toModel) }`.
- `fun state(audioId: String): Flow<DownloadState>` =
  `combine(dao.existsFlow(audioId), WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("download_$audioId")) { downloaded, infos ->`
  - `if (downloaded && files.exists(audioId)) DownloadState.Downloaded`
  - else from `infos.firstOrNull()?.state`: `ENQUEUED|BLOCKED → Queued`; `RUNNING → Downloading(info.progress.getFloat(KEY_FRACTION, 0f))`; `FAILED → Failed`; else `NotDownloaded`.
  `}`

**Koin:** `single { AudioFileStore(androidContext()) }`, `single { DownloadRepository(androidContext(), get(), get()) }`. Worker resolves deps via GlobalContext (no Koin worker factory).

> Testability seam: `DownloadRepository.state` merge logic depends on WorkManager. To unit-test the
> mapping, extract a pure `fun mapDownloadState(downloaded: Boolean, fileExists: Boolean, infoState: WorkInfo.State?, fraction: Float): DownloadState`
> and have `state(...)` call it. The pure function is unit-tested; the WorkManager plumbing is device-verified.

## Section 3 — offline playback (`:core:player`)

`MediaPlaybackController` constructor adds `private val localFile: (audioId: String) -> File?`.
In `setQueue`, per audio:
```kotlin
val local = localFile(audio.id)
val uri = if (local != null) Uri.fromFile(local) else Uri.parse(audio.url)
```
build the `MediaItem` with `uri`. Everything else (resume, metadata, queue, speed) unchanged.

`NowPlaying` gains `isLocal: Boolean`, set in `updateNowPlaying` from
`item.localConfiguration?.uri?.scheme == "file"`. One new construction-site arg (+ test fakes).

`playerModule`: build controller with
`localFile = { id -> get<AudioFileStore>().let { if (it.exists(id)) it.file(id) else null } }`
(`:core:player` already depends on `:core:data`).

Resume/saveProgress still key on `audio.id` (mediaId) — unaffected by local vs stream uri.

## Section 4 — UI (`:feature:audio`) + app

**`DownloadButton(state: DownloadState, onDownload, onCancel, onDelete)`:**
- `NotDownloaded` → download icon → `onDownload`
- `Queued` → small indeterminate spinner → `onCancel`
- `Downloading(f)` → determinate circular progress `f` → `onCancel`
- `Downloaded` → check icon → `onDelete` (confirm dialog)
- `Failed` → error/retry icon → `onDownload`

**Player action row:** add `DownloadButton` beside ❤️ (the reserved slot). `PlayerViewModel` ctor
gains `DownloadRepository`; adds `downloadState: StateFlow<DownloadState>` (flatMapLatest on
`nowPlaying`, NotDownloaded when null) and `download()/cancelDownload()/deleteDownload()` (build
`Audio`+`Album` from `NowPlaying`, like `toFavorite`).

**Audio list rows:** trailing `DownloadButton` per row. `AudioListViewModel` ctor gains
`DownloadRepository`; exposes `fun downloadState(audioId): Flow<DownloadState>` (collected per row)
+ `download(audio)/cancel(audioId)/delete(audioId)`. (Album for enqueue built from the route's
album id/title/cover, same as `play`.)

**Album batch:** an action on `AudioListScreen` (e.g. a top "ดาวน์โหลดทั้งหมด" button) → confirm
dialog (item count) → `vm.downloadAll()` → `repo.enqueueAll(album, audios)`.

**Downloads screen:** `DownloadsViewModel(downloads: DownloadRepository, controller: PlaybackController)`:
- `state: StateFlow<UiState<List<Download>>>` from `downloads.downloads` (Empty/Content; initial Loading; `Eagerly` so screen-scoped reads settle — consistent with FavoritesViewModel).
- `play(d)` = `controller.setQueue(Album(d.albumId, d.albumTitle, d.coverUrl, 0, 0), listOf(Audio(d.audioId, d.albumId, d.title, d.url)), 0)` (controller resolves to local file).
- `delete(audioId)` = launch `downloads.delete(audioId)`.
`DownloadsScreen` = iOS-style list (48dp cover + title + size subtitle via `formatRowMeta(null, sizeBytes)` or a size formatter), tap → play + open player, swipe-to-remove (end→start) → delete. Empty → "ยังไม่มีรายการดาวน์โหลด".

**Pinned ⬇ card on AlbumsScreen:** `AlbumsViewModel` adds `downloadCount: StateFlow<Int>` (ctor gains
`DownloadRepository`); `AlbumsScreen` gains `downloadCount` + `onOpenDownloads`; render a
`ดาวน์โหลด` card (⬇ + count when >0 else chevron) directly under the favorites card when query blank.

**Nav (AudioPane):** add `@Serializable data object DownloadsRoute` + `composable<DownloadsRoute>`
hosting `DownloadsScreen` (`onPlay = { vm.play(it); onOpenPlayer() }`, `onRemove = vm::delete`).
Albums composable passes `onOpenDownloads = { nav.navigate(DownloadsRoute) }` + `downloadCount`.

**`audioModule`:** `PlayerViewModel(get(), get(), get())`,
`AudioListViewModel(albumId, get(), get(), get())`, `AlbumsViewModel(get(), get(), get())`,
`viewModel { DownloadsViewModel(get(), get()) }`.

**App:** add `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />`.
Create the `"downloads"` `NotificationChannel` in `MainApplication.onCreate` (API 26+). WorkManager
auto-initializes via its manifest provider; no extra init.

## Testing

**JVM unit:**
- Pure `mapDownloadState(downloaded, fileExists, infoState, fraction)` → all five outcomes.
- `DownloadsViewModel`: stream→Content/Empty; `play` builds single-track queue; `delete` forwards.
- `PlayerViewModel`/`AudioListViewModel` download-action forwarding (mock repo); `downloadState` wiring.
- `AlbumsViewModel.downloadCount` reflects repo.
- Mappers (`DownloadEntity` ↔ `Download`).

**Device-only:** real WorkManager enqueue + foreground notification + progress + retry; offline
playback from disk; batch "download all"; delete removes file + row; survives app-kill mid-download.

## Edge cases

- Duplicate tap → unique work `KEEP` (no double download).
- Cancel mid-download → `.part` cleaned (FileDownloader), no DB row inserted.
- Delete a downloaded track while it's playing → playback continues (ExoPlayer holds the item);
  row vanishes; a later play streams (no local file).
- Not downloaded → streams as before (offline resolver returns null).
- Failure (network) → `Failed` state, retry ≤3 attempts, then surfaced via button.
- Storage in app-scoped external files dir → no runtime storage permission.

## Device smoke checklist

1. Player → tap download → spinner → progress → check; file lands in audios dir.
2. Kill app mid-download → reopens, WorkManager resumes/retries, completes.
3. Audio list rows show per-track download state; tap downloads.
4. Album "ดาวน์โหลดทั้งหมด" → confirm → all tracks queue + complete.
5. Albums screen → pinned ดาวน์โหลด card + count.
6. Downloads screen → list with sizes; tap → plays from disk (airplane mode proves offline).
7. Swipe a download → file + row removed; count updates.
8. Play a downloaded track in airplane mode → plays.
