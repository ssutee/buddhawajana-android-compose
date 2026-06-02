# Audio Vertical — Design (Vertical 1)

Date: 2026-06-02
Status: Approved (brainstorming)
Branch: `feature/audio-vertical`

## Goal

Rebuild the **Audio** feature as a modern Material 3 vertical, reaching parity with the
iOS app (`~/Works/watnapahpong/WatnaMediaApp`) audio experience — delivered in phases.
This document covers **Vertical 1**: browse + streaming player + background/lock-screen +
resume position + mini-player + per-row metadata. Downloads/offline and favorites are
explicitly deferred to follow-up verticals.

## Context: what already exists

The multi-module foundation already pre-built the audio **data spine**:

- `core/model`: `Album(id, title, coverUrl, itemCount, position)`, `Audio(id, albumId, title, url)`
- `core/network`: `AudioService` (`getAlbums()` → `GET category`, `getAudios(id)` → `GET category/{id}/`),
  `AlbumDto`, `AudioDto`
- `core/data`: `AlbumEntity`/`AlbumDao`/`AlbumRepository`, `AudioEntity`/`AudioDao`/`AudioRepository`
  (cache-first: Flow stream + suspend refresh, read-before-write merge), Room DB
  `buddhawajana.db` (class `com.watnapp.buddhawajana.core.data.db.AppDatabase`),
  reusable `FileDownloader`.

So this vertical is **UI + player engine + small data additions**, not a data rebuild.

Legacy audio (to be retired): `:app` `AudioScreen`, `Mp3PlayerActivity` using the 3rd-party
`ArgMusicPlayer`, RxJava2 `AudioService`, generic `DownloadableViewModel`. None reused.

## Scope decisions (locked in brainstorming)

- **Phased.** Vertical 1 = browse + streaming player + background + lock-screen. Later
  verticals: downloads/offline + batch; favorites.
- **Player engine: Media3 ExoPlayer + `MediaSessionService`** (background + lock-screen +
  headset/Bluetooth/Android Auto for free; replaces ArgMusicPlayer).
- **Resume position: yes** (Android improvement over iOS, which is session-only). New
  `PlaybackProgress`, mirroring Books' `ReadingProgress`.
- **Mini-player: yes** — persistent bar above the bottom nav, app-wide.
- **Per-row duration + size: yes now** — lazy metadata probing, cached in DB.

Out of scope V1: downloads/offline, batch download, favorites, repeat/shuffle, Android Auto polish.

## Architecture

Chosen approach: a dedicated **`:core:player`** module owns the Media3 engine; the app
scaffold and `:feature:audio` both consume its state. (Rejected: player-in-feature with no
service — fails background/lock-screen; bound service + manual `MediaSessionCompat` — obsolete
boilerplate.)

### Module deltas

| Module | Change |
|--------|--------|
| `:core:player` (**new**) | Media3 engine: `PlaybackService`, `PlaybackConnection`, `SleepTimerState`, speed prefs |
| `:core:data` | `PlaybackProgress` entity/dao/repo; `durationMs`/`sizeBytes` on audio; `MetadataProber`; DB v1→v2 migration |
| `:core:model` | `PlaybackProgress` model; `Audio` gains `durationMs`/`sizeBytes` (nullable) |
| `:feature:audio` (**new**) | Albums + AudioList + Player screens + VMs + `audioModule` + nav graph |
| `:app` | Mini-player in scaffold; Audio nav destination; `PlaybackService` manifest + perms; register `playerModule` + `audioModule` |

## Section 1 — `:core:player` engine

Deps (version catalog): `androidx.media3:media3-exoplayer`, `media3-session`, `media3-ui` (minimal).

**`PlaybackService : MediaSessionService`** — owns one `ExoPlayer` + one `MediaSession`.
Media3 auto-publishes the MediaStyle notification + lock-screen controls + headset/Bluetooth/
Android-Auto. Audio focus via `setAudioAttributes(..., handleAudioFocus = true)` and
`setHandleAudioBecomingNoisy(true)`. Runs foreground while playing.

**`PlaybackConnection`** (app-singleton) — binds a `MediaController` via `SessionToken`
(async future). Exposes hot `StateFlow`s driven by a `Player.Listener` + a ~500 ms position
ticker:

```kotlin
data class NowPlaying(
    val audioId: String, val albumId: String, val title: String,
    val album: String, val artworkUrl: String?, val isLocal: Boolean = false,
)
val nowPlaying: StateFlow<NowPlaying?>
val isPlaying: StateFlow<Boolean>
val positionMs: StateFlow<Long>
val durationMs: StateFlow<Long>
val speed: StateFlow<Float>
val sleepTimer: StateFlow<SleepTimerState>
```

Commands: `setQueue(album, audios, startIndex)`, `playPause()`, `seekTo(ms)`,
`skip(deltaMs)` (±15_000), `next()`, `prev()`, `setSpeed(rate)`, `setSleepTimer(state)`.

Artwork: `Audio` has no per-track art, so `NowPlaying.artworkUrl` = the album's `coverUrl`
(passed via `setQueue(album, ...)`). Player and mini-player both render album cover.

- **Queue / auto-advance:** load the whole album as `List<MediaItem>`; ExoPlayer's built-in
  `seekToNext/Previous` + auto-advance; stop on last (no repeat/shuffle).
- **Speed:** presets 0.75 / 1.0 / 1.25 / 1.5 / 2.0×; persisted in DataStore Preferences
  (owned by `:core:player`); re-applied on each prepare.
- **Sleep timer:** `SleepTimerState = Off | Duration(remainingMs) | EndOfTrack`. Coroutine
  countdown in the connection → pause at zero; `EndOfTrack` pauses on `STATE_ENDED` of the
  current item. Session-only (not persisted).
- **Resume position:** on item transition + on pause + every ~10 s, the connection calls
  `PlaybackProgressRepository.save(audioId, positionMs)`; on `setQueue`, seek the start item
  to the saved position.

## Section 2 — data layer (`:core:data`)

**`PlaybackProgress`** (mirror `ReadingProgress`):
- model `PlaybackProgress(audioId: String, positionMs: Long, updatedAt: Long)`
- `PlaybackProgressEntity` table `playback_progress`, PK `audioId`
- `PlaybackProgressDao`: `suspend get(audioId): PlaybackProgressEntity?`,
  `suspend upsert(entity)`, optional `stream(audioId)`
- `PlaybackProgressRepository`: `get(audioId)`, `save(audioId, positionMs)`

**Audio metadata** (per-row duration + size):
- add nullable cols to `AudioEntity`: `durationMs: Long?`, `sizeBytes: Long?` (null = not probed)
- `Audio` model gains nullable `durationMs`, `sizeBytes`; mapper updated
- `AudioDao`: `suspend updateMetadata(audioId, durationMs, sizeBytes)`
- **`MetadataProber`**: `suspend probe(url): Pair<Long?, Long?>` — OkHttp HEAD →
  `Content-Length` (size); duration via `MediaMetadataRetriever.setDataSource(url, emptyMap())`
  → `METADATA_KEY_DURATION`; on `Dispatchers.IO`, wrapped in `runCatchingCancellable` (null on
  failure). Probe once, persist, never re-probe when already non-null.

**DB migration:** `buddhawajana.db` v1 → v2 — `CREATE TABLE playback_progress` +
`ALTER TABLE audio ADD COLUMN duration_ms INTEGER` + `ADD COLUMN size_bytes INTEGER`.
Proper `Migration` object (non-destructive).

**Koin:** `dataModule` += `PlaybackProgressDao`, `PlaybackProgressRepository`, `MetadataProber`.

`AlbumRepository`/`AudioRepository` (cache-first) reused as-is.

## Section 3 — `:feature:audio` UI

Mirror `:feature:books` (Koin module, type-safe nav routes via kotlinx-serialization,
`UiState<T>`, `BaseViewModel`).

**Routes:** `AudioAlbums`, `AudioList(albumId, albumTitle)`, `Player`. (Mini-player is scaffold
chrome, not a route.)

1. **AlbumsScreen / AlbumsViewModel** — `albums: StateFlow<UiState<List<Album>>>` from
   `AlbumRepository` (cache-first stream + refresh). Grid of album cards (Coil cover, title,
   itemCount). Search: case-insensitive substring on title; query held in VM (survives
   rotation). Tap → `AudioList`.
2. **AudioListScreen / AudioListViewModel(albumId)** — `audios: StateFlow<UiState<List<Audio>>>`
   from `AudioRepository.stream(albumId)` + refresh. Rows show title + "M:SS · X MB"
   (from `durationMs`/`sizeBytes`, "—" until probed). Lazy probe: on first compose of a row
   with null metadata, VM launches `MetadataProber` → persists → row updates via Flow. Search
   substring on title. Tap row → `connection.setQueue(album, audios, index)` + nav to `Player`.
   Highlight the currently-playing row from `connection.nowPlaying`.
3. **PlayerScreen / PlayerViewModel** — reads `PlaybackConnection` Flows. Layout (mirror iOS):
   large artwork, title, album, scrubber (draft-state slider, commit on release) with
   M:SS/total, main controls (prev | −15 s | play/pause | +15 s | next), secondary
   (speed menu, sleep-timer menu with countdown label), share (stream URL link). Back pops to
   browse; playback continues via service + mini-player. PlayerViewModel mostly maps Flows +
   forwards commands; no per-screen ExoPlayer.

## Section 4 — mini-player, app wiring, manifest, testing

**Mini-player (app scaffold):** rendered above the bottom nav. Observes
`PlaybackConnection.nowPlaying` — null → hidden; non-null → artwork thumb + title + play/pause +
thin progress line. Tap → `Player` route. Swipe/✕ → stop + clear. Persists across all
top-level destinations (Audio, Books).

**Navigation:** add **Audio** top-level destination to the adaptive navigation suite. App-level
NavHost gets the audio graph via a `NavGraphBuilder.audioGraph()` extension exposed by
`:feature:audio` (Books pattern). `:app` depends on `:core:player` + `:feature:audio`.

**Manifest / perms (`:app`):**
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<service
    android:name="com.watnapp.buddhawajana.core.player.PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter><action android:name="androidx.media3.session.MediaSessionService"/></intent-filter>
</service>
```
`POST_NOTIFICATIONS` requested at runtime on first play (API 33+); if denied, playback still
works without a notification.

**Koin:** new `playerModule` (singleton `PlaybackConnection`, speed prefs) + `audioModule`
(3 VMs). Registered in `MainApplication` alongside existing modules. `PlaybackConnection` is an
app-singleton, binds the controller on first use, releases on app teardown.

**Testing:**
- JVM unit: `PlaybackProgressRepository` (fake dao); Albums/AudioList VMs (cache-first +
  search + lazy-probe trigger with faked `MetadataProber`); `SleepTimerState` countdown logic;
  speed-prefs persistence; `MetadataProber` HEAD parse (fake OkHttp).
- Player engine / service / `MediaController`: **device/instrumented only** — not meaningfully
  unit-testable (per the three reader device bugs). Ship a device smoke checklist.

**Edge cases:**
- Stream URL http→https upgrade + trim leading whitespace (iOS quirk) — applied in mapper/DTO.
- Audio-focus loss (call/other app) → ExoPlayer auto-pauses/ducks.
- Headset unplug → pause (`setHandleAudioBecomingNoisy(true)`).
- Empty/failed album → `UiState.Error` + retry.
- Network loss mid-stream → ExoPlayer error surfaced + retry.
- Position save throttled (~10 s + on pause/transition) to avoid DB spam.

## Device smoke checklist (V1)

1. Browse albums → open album → list shows rows; duration/size fill in as rows appear.
2. Tap audio → player opens, streams, plays.
3. Background the app / lock screen → playback continues; notification + lock-screen controls
   work (play/pause, ±15 s, next/prev).
4. Headset buttons + unplug behave (pause on noisy).
5. Speed change persists across tracks and app restart.
6. Sleep timer (duration + end-of-track) pauses correctly; countdown shows.
7. Mini-player appears while playing, persists across Books/Audio nav, tap expands, ✕ stops.
8. Close + reopen an audio → resumes near last position.
9. Incoming call pauses, then resumes/holds correctly.
