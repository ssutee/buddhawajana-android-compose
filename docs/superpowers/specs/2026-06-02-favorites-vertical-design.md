# Favorites Vertical — Design

Date: 2026-06-02
Status: Approved (brainstorming)
Branch: `feature/favorites-vertical`

## Goal

Let users favorite audio tracks and browse them in a dedicated Favorites screen, reaching
parity with the iOS app's heart-on-player + Favorites list + pinned "รายการโปรด" card.

## Scope decisions (locked in brainstorming)

- **Audio only.** Favorite individual audio tracks. Books keep their own bookmarks. Model is
  audio-specific (no polymorphic content type).
- **Tap a favorite → plays just that track** (single-item queue, no auto-advance through favorites).
- **Heart on the player only** (plus swipe-to-remove in the Favorites screen). No row-level hearts.
- **Denormalized snapshot** storage (chosen over id-only + join): a favorite is self-contained so
  it renders and plays even if the source album's cache was cleared.

Out of scope: book favorites, favorites reordering, favorites-as-continuous-queue, row-level hearts.

## Architecture

A favorite must (a) render in the Favorites list (title + cover) and (b) play standalone
(`setQueue` needs album id/title/cover + audio id/title/url). Since audio rows live per-album in
Room and may not all be cached, the favorite row stores a **denormalized snapshot** of everything
needed — no joins, never a dead row.

### Module deltas

| Module | Change |
|--------|--------|
| `:core:model` | new `Favorite` |
| `:core:data` | `FavoriteEntity`/`FavoriteDao`/`FavoriteRepository`; DB v2→v3 + migration; DI |
| `:core:player` | `NowPlaying` gains `url` (so the player heart can snapshot the track) |
| `:feature:audio` | heart in `PlayerScreen`/`PlayerViewModel`; `FavoritesScreen`/`FavoritesViewModel`; pinned card on `AlbumsScreen`; `FavoritesRoute` in `AudioPane`; `audioModule` wiring |
| `:app` | none (AudioPane owns the internal nav; player route already exists) |

## Section 1 — data layer + NowPlaying.url

**`:core:model`**
```kotlin
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

**`:core:data`**
- `FavoriteEntity` table `favorite`, `@PrimaryKey @ColumnInfo("audio_id") audioId: String`, cols
  `title`, `url`, `album_id`, `album_title`, `cover_url` (nullable), `added_at: Long`.
- `FavoriteDao`:
  - `@Query("SELECT * FROM favorite ORDER BY added_at DESC") fun stream(): Flow<List<FavoriteEntity>>`
  - `@Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE audio_id = :audioId)") fun isFavorite(audioId: String): Flow<Boolean>`
  - `@Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE audio_id = :audioId)") suspend fun exists(audioId: String): Boolean`
  - `@Insert(onConflict = REPLACE) suspend fun insert(e: FavoriteEntity)`
  - `@Query("DELETE FROM favorite WHERE audio_id = :audioId") suspend fun deleteById(audioId: String)`
- `FavoriteRepository`:
  - `val favorites: Flow<List<Favorite>> = dao.stream().map { it.map(::toModel) }`
  - `fun isFavorite(audioId: String): Flow<Boolean> = dao.isFavorite(audioId)`
  - `suspend fun add(f: Favorite)`, `suspend fun remove(audioId: String)`
  - `suspend fun toggle(f: Favorite)` — if `dao.isFavorite(f.audioId).first()` remove else add
    (use a one-shot existence read via a `suspend fun exists(audioId): Boolean` dao query to avoid
    collecting the Flow inside toggle).
- Mapper `FavoriteEntity.toModel()` / `Favorite.toEntity()`.
- `AppDatabase` v2→v3: add `FavoriteEntity::class` to entities, `abstract fun favoriteDao()`,
  `MIGRATION_2_3`:
  ```sql
  CREATE TABLE IF NOT EXISTS favorite (
      audio_id TEXT NOT NULL PRIMARY KEY,
      title TEXT NOT NULL, url TEXT NOT NULL,
      album_id TEXT NOT NULL, album_title TEXT NOT NULL,
      cover_url TEXT, added_at INTEGER NOT NULL)
  ```
- `DataModule`: register `MIGRATION_2_3` on the builder + `favoriteDao()` single + `FavoriteRepository` single.

**`:core:player`** — `NowPlaying` gains `val url: String`. `MediaPlaybackController.updateNowPlaying()`
populates it from `currentMediaItem.localConfiguration?.uri?.toString() ?: ""`
(setQueue already sets `setUri(audio.url)`). No other engine change. Fakes/tests for `NowPlaying`
elsewhere must add the new field (default it where constructed in tests).

## Section 2 — UI + navigation (`:feature:audio`)

**Player heart.** `PlayerViewModel` ctor adds `FavoriteRepository`.
- `isFavorite: StateFlow<Boolean>` = `nowPlaying.flatMapLatest { if (it == null) flowOf(false) else favorites.isFavorite(it.audioId) }.stateIn(...)`.
- `toggleFavorite()` = `viewModelScope.launch { nowPlaying.value?.let { favorites.toggle(it.toFavorite(now())) } }` where
  `NowPlaying.toFavorite(addedAt)` builds the snapshot.
- `PlayerScreen`: heart `IconButton` (`Icons.Default.Favorite` when favorited else `Icons.Default.FavoriteBorder`)
  in the secondary row; hidden/disabled when `nowPlaying == null`.
- `audioModule`: `viewModel { PlayerViewModel(get(), get()) }`.

**FavoritesScreen + FavoritesViewModel.**
- `FavoritesViewModel(favorites: FavoriteRepository, controller: PlaybackController)`:
  - `state: StateFlow<UiState<List<Favorite>>>` from `favorites.favorites` (Empty when none, else Content).
  - `fun play(f: Favorite)` = `controller.setQueue(Album(f.albumId, f.albumTitle, f.coverUrl, 0, 0), listOf(Audio(f.audioId, f.albumId, f.title, f.url)), 0)`.
  - `fun remove(audioId: String)` = `viewModelScope.launch { favorites.remove(audioId) }`.
- `FavoritesScreen`: `LazyColumn` of iOS-style rows (`ListItem`: 48dp cover thumb + title + album subtitle),
  newest-first. Tap → `onPlay(f)`. Swipe-to-remove via `SwipeToDismissBox` → `onRemove(audioId)`.
  Empty → `EmptyStateView("ยังไม่มีรายการโปรด")`. Loading → `LoadingView`.

**Pinned card on AlbumsScreen.**
- `AlbumsViewModel` exposes `favoriteCount: StateFlow<Int>` = `favorites.favorites.map { it.size }`.
  (ctor adds `FavoriteRepository`; `audioModule` → `AlbumsViewModel(get(), get())`.)
- `AlbumsScreen` gains `onOpenFavorites: () -> Unit` + `favoriteCount: Int`. Renders a pinned
  `รายการโปรด` card (❤️ leading + label + trailing count when >0 + chevron) above the album list,
  shown only when the search query is blank. Tap → `onOpenFavorites`.

**Navigation (AudioPane).**
- Add `@Serializable private data object FavoritesRoute`.
- Albums composable: pass `onOpenFavorites = { nav.navigate(FavoritesRoute) }` + `favoriteCount`.
- New `composable<FavoritesRoute>`: hosts `FavoritesScreen`; `onPlay = { f -> vm.play(f); onOpenPlayer() }`
  (reuses AudioPane's existing `onOpenPlayer` → app-level full-screen player route); `onRemove = vm::remove`.

## Section 3 — testing + edge cases

**JVM unit tests:**
- `FavoriteRepository` (fake in-memory dao): add/remove/toggle round-trip; `isFavorite` reflects
  state; `favorites` ordered newest-first.
- `FavoritesViewModel` (mockk repo + controller): stream → Content/Empty; `play(f)` calls
  `controller.setQueue` with a 1-item queue built from the favorite; `remove` calls repo.
- `PlayerViewModel`: `toggleFavorite()` forwards a `Favorite` snapshot built from current
  `NowPlaying`; `isFavorite` tracks the repo flow.
- `AlbumsViewModel`: `favoriteCount` reflects repo flow.

**Device-only:** heart fills/outlines live; swipe-to-remove; tap favorite plays standalone (even
after album cache cleared); pinned-card count updates.

**Edge cases:**
- `NowPlaying.url` empty (nothing playing) → heart disabled, no-op.
- Favorited audio whose album cache was cleared still plays (denormalized snapshot, no join).
- Duplicate add → PK `audio_id` REPLACE (idempotent); toggle removes when present.
- `coverUrl` null → CachedAsyncImage placeholder.
- Removing the currently-playing favorite → playback continues (queue already set); row vanishes.
- Empty favorites → Empty state; pinned card shows count only when >0.

## Device smoke checklist

1. Play a track → tap heart → fills; reopen player → still filled.
2. Albums screen → pinned รายการโปรด card shows, count matches.
3. Open Favorites → newest-first list; tap a row → plays that track + opens player.
4. Swipe a favorite → removed; count + list update.
5. Favorite a track, kill+reopen app → favorite persists and still plays.
6. Unheart from player → disappears from Favorites.
