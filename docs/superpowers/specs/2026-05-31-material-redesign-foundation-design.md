# Buddhawajana Android — Material Redesign Foundation (Design Spec)

**Date:** 2026-05-31
**Status:** Approved (brainstorming) → ready for implementation plan
**Scope:** Foundation slice only. Feature parity with the iOS app (`~/Works/watnapahpong/WatnaMediaApp`) is delivered in later per-feature specs that build on this foundation.

---

## 1. Background & Goal

The Android app (`buddhawajana-android-compose`) is a Thai Theravada Buddhist media app ("พุทธวจน" / Buddhawajana) with three features today: **Books** (PDF list → external/Pdf-Viewer), **Audio** (albums → tracks → ArgPlayer full-screen activity), and **YouTube** (redirect link). It is built with Jetpack Compose 1.3.3, a Material 3 theme hybridized with a Material 2 bottom nav, MVVM + Koin + Room + Retrofit, and a RxJava2/coroutines mix.

The iOS counterpart is far richer: a full-featured audio player (speed, sleep timer, skip ±15s, lock-screen/now-playing, background audio, prev/next + auto-advance, share), Favorites, a Downloads manager (pause/resume/retry/cancel + completed list + background engine), search on every list, an in-app PDF reader with bookmarks + reading-progress resume, a Settings/About screen, and cache-first loading with loading/empty/error states. It is heavily modular (11 SPM packages) and uses SwiftData.

**Overall objective:** redesign the Android app to Material Design (Material 3) and reach feature parity with iOS.

**This spec's objective:** build the **foundation** every feature will reuse — design system, theming, navigation shell, data/state plumbing, modernized toolchain, and module structure — without yet rebuilding the features themselves. The app must compile and run end-to-end (the existing 3 screens) on the new foundation at every step.

### Why foundation-first
Full parity spans ~6–7 subsystems (design system + nav shell, audio catalog + search + favorites, rich player with background/now-playing, downloads engine + manager, books grid + in-app PDF reader with bookmarks/progress, settings). Designing them in one spec is too large and risky. The foundation is the lowest-risk slice that unblocks all of them and is reused by each.

---

## 2. Decisions (locked during brainstorming)

| # | Decision | Choice |
|---|----------|--------|
| D1 | Scope of this spec | Foundation only; features in later specs |
| D2 | Theming | Light + Dark + Material You dynamic color (Android 12+), with a fixed **teal** brand palette as the fallback below Android 12 |
| D3 | Brand seed color | **Teal** (mirrors the iOS accent) |
| D4 | Navigation | Mirror iOS: 3 top-level tabs (Audio · Books · YouTube); Settings = top-app-bar gear; Favorites + Downloads = pinned rows inside Audio (not tabs); adaptive rail on tablets/foldables |
| D5 | Modernization | Upgrade toolchain to Compose/Material3 stable + adaptive **and** migrate the data layer from RxJava2 to coroutines + Flow |
| D6 | Module structure | Multi-module Gradle (`:core:*` + later `:feature:*`), mirroring the iOS package split |
| D7 | Thai typography | Thai-optimized font with raised line-height; default **Noto Sans Thai** (alts: IBM Plex Sans Thai, Sarabun) |

---

## 3. Module Architecture (D6)

Dependencies point downward; features never depend on sibling features.

```
:app                      → thin host: Application, MainActivity, DI startup, nav-host wiring
  │
  ├─ :core:designsystem   → BuddhawajanaTheme, color/type/shape/spacing tokens,
  │                          M3 component wrappers, state views, snackbar, CachedAsyncImage (Coil)
  ├─ :core:ui             → shared Compose utils, UiState<T>, adaptive nav scaffold,
  │                          BaseViewModel, route contracts, WindowSize plumbing
  ├─ :core:data           → repositories (cache-first), Room DB + DAOs, mappers
  ├─ :core:network        → Retrofit/OkHttp, coroutine services, DTOs
  ├─ :core:model          → pure-Kotlin domain models (Book, Album, Audio, …) — no Android deps
  └─ :core:common         → coroutine dispatchers, Result types, constants, extension fns

(later, per feature-spec)
  :feature:audio  :feature:player  :feature:books  :feature:downloads  :feature:settings
        └── each depends on :core:* only, never on another :feature
```

**Rules**
- `:core:model` and `:core:common` are pure Kotlin (fast, testable, no Android deps).
- Features depend only on `:core:*`. Cross-feature navigation goes through route contracts exposed in `:core:ui`.
- `:app` knows every feature (to wire the nav graph); features never know `:app`.
- Koin: one module per `:core:*` and per `:feature:*`, aggregated in `:app`.

**Foundation migration of existing code:** today's `ui/`, `vm/`, `repository/`, `api/`, `entity/` move into the matching `:core:*` modules. The 3 current screens (Books/Audio/YouTube) get a temporary home in `:app` and are rebuilt vertical-by-vertical in later specs, so the foundation compiles and runs end-to-end throughout.

---

## 4. Theming & Design System (D2, D3, D7)

Shipped by `:core:designsystem`.

- **Color:** full M3 light + dark schemes generated from the teal seed; Material You dynamic color on Android 12+, falling back to the teal scheme below 12.
- **Type:** the M3 type scale on a Thai-optimized font (**Noto Sans Thai** by default) with raised line-height — the default Roboto clips Thai stacked diacritics.
- **Shape:** M3 shape scale (e.g., cards ~12dp, sheets ~28dp).
- **Spacing:** 4dp-grid tokens (xs 4 / s 8 / m 16 / l 24 / xl 32), replacing the scattered hardcoded paddings across current screens.

**Shared components shipped now**
- `LoadingView`, `EmptyStateView`, `ErrorView` (with retry) — the cache-first states.
- Snackbar host (Android's answer to iOS's "toast").
- Material3 `PullToRefresh` wrapper.
- `CachedAsyncImage` on Coil (covers/thumbnails).
- `DownloadButton` (state placeholder; engine arrives in the downloads feature spec).
- `BuddhawajanaTheme { }`, `BuddhawajanaTopBar`, and the adaptive navigation scaffold.

**Reference color roles (teal seed → M3):** primary `#006A6A`, primaryContainer `#6FF7F6`, secondary `#4A6363`, tertiary `#4B607C`, error `#BA1A1A`, surface `#F4FBFA` (light). Exact tones to be finalized with the M3 theme builder during implementation.

---

## 5. Navigation Shell (D4)

`NavigationSuiteScaffold` (from `material3-adaptive`) is the entire shell — one composable that auto-switches bottom bar ↔ navigation rail ↔ drawer by window size (replaces today's manual `WindowSize` branching).

**Top-level destinations** (contract exposed by `:core:ui`): `Audio` (start) · `Books` · `YouTube`.

```
MainActivity
└─ BuddhawajanaTheme
   └─ NavigationSuiteScaffold(destinations = [Audio, Books, YouTube])
      └─ Scaffold(topBar = BuddhawajanaTopBar(title, actions = [⚙️ Settings]))
         └─ NavHost(startDestination = Audio)
              audioGraph()    // nested graph → catalog, favorites, downloads, player (later specs)
              booksGraph()    // → list, reader (later spec)
              youtube()       // redirect screen
              settings()      // pushed from top-bar gear, not a tab
```

- **Settings** = top-app-bar action (gear), pushed as a normal destination — mirrors iOS, not a tab.
- **Favorites + Downloads** = routes *inside* `audioGraph`, reached from pinned rows in the Audio catalog. Not top-level.
- **Type-safe routes** — Navigation-Compose with `kotlinx-serialization` route objects (replaces today's string routes).
- **Per-tab back stack** preserved across tab switches (`saveState`/`restoreState`).
- **Deferred to feature specs:** nav-state persistence across cold launch (iOS restores reader page / category path) and deep links — foundation leaves the seams only.

---

## 6. Data Foundation & State (D5)

Shipped by `:core:network`, `:core:data`, `:core:model`, `:core:common`, with state types in `:core:ui`.

**Rx → coroutines/Flow migration (drop RxJava2 entirely)**
- Retrofit services → `suspend fun` returning DTOs (remove the rxjava2 call adapter).
- Room DAOs → `suspend` writes + `Flow<List<Entity>>` reads (remove rxjava2 Room integration).
- `viewModelScope` + Flow throughout; OkHttp file download → `Flow<Progress>` via coroutines.

**Cache-first repository pattern** (matches iOS; Room is the single source of truth):

```kotlin
interface Repository<T> {
    fun stream(): Flow<List<T>>         // Room-backed; emits instantly + on every change
    suspend fun refresh(): Result<Unit> // fetch API → upsert Room → stream re-emits
}
```

- Render cache immediately → refresh in background → upsert → Flow re-emits.
- Refresh fails **with** cache present → silent; fire a Snackbar event.
- Refresh fails with **empty** cache → `UiState.Error(retry)`.

**Shared UI state** (`:core:ui`):

```kotlin
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    object Empty : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String, val retry: () -> Unit) : UiState<Nothing>
}

abstract class BaseViewModel   // replaces today's BaseViewModel + DownloadableViewModel
```

ViewModels expose `StateFlow<UiState<T>>`; screens `when` over it → `LoadingView` / `EmptyStateView` / `ErrorView` / content.

**Data flow:** `DTO (:core:network) → Entity (:core:data/Room) → Model (:core:model)` via mappers. Models are pure Kotlin.

**Preserve user data:** keep the `watna-compose.db` database and its existing migrations (including the recent 1→2). This migration changes the **DAO API only** (Rx→Flow), not the schema — installed users keep their downloads and progress.

**Dispatchers** centralized in `:core:common` (injectable → testable). **Koin** module per `:core:*`.

**Deferred to feature specs:** the actual download *engine* (WorkManager) and the favorites / bookmarks / reading-progress tables. The foundation defines only the cache-first contract and state types they plug into.

---

## 7. Build / Toolchain & Migration Order (D5)

**Version catalog as single source of truth.** Today's build is inconsistent — `build.gradle` carries a Kotlin 1.8.10 fallback + Compose compiler 1.4.3 while `libs.versions.toml` says Kotlin 2.0. Consolidate everything into `libs.versions.toml`.

**Target versions**
- Compose **BOM** (latest stable) + Material3 **stable** + `material3-adaptive-navigation-suite`.
- Kotlin 2.0 + Compose compiler plugin (already present); AGP 8.9.1 (keep).
- Navigation-Compose + `kotlinx-serialization` (type-safe routes); coroutines; Koin (latest); Coil; Room (latest).
- **Remove:** RxJava2, the rxjava2 Retrofit adapter, and rxjava2 Room/WorkManager integrations.
- Keep **minSdk 24**, compile/target **36**.
- **`build-logic` convention plugins** — shared module config (android library, compose, kotlin) so the modules don't copy-paste Gradle setup.

**Migration order — each step keeps the app compiling and runnable (tracer-bullet):**
1. Version catalog cleanup + toolchain bump (still single module) → app builds on M3 stable.
2. Extract `:core:model` + `:core:common` (pure Kotlin).
3. `:core:network` — services → `suspend`, drop Rx.
4. `:core:data` — DAOs → Flow, repositories cache-first, drop Rx (DB schema untouched).
5. `:core:designsystem` — theme, tokens, components.
6. `:core:ui` — `UiState`, `BaseViewModel`, `NavigationSuiteScaffold` shell.
7. `:app` — rewire nav shell + Koin; adapt the existing 3 screens to the new theme/state so it runs end-to-end.

**Testing**
- Unit tests for repositories (fake DAO + fake service) covering cache-first behavior (instant cache emit, background refresh, silent-fail-with-cache, error-on-empty).
- Optional iOS-parity: **Paparazzi/Roborazzi** screenshot tests for design-system components (iOS uses snapshot tests).

---

## 8. Definition of Done

The foundation spec is complete when:
- The app **builds** on the modern M3 stable stack (Compose BOM, Material3 + adaptive, Kotlin 2.0), with RxJava2 fully removed.
- The app **runs** the 3 existing screens (Books, Audio, YouTube) through the new teal `BuddhawajanaTheme`, the adaptive `NavigationSuiteScaffold` shell (with top-bar Settings gear), and the cache-first / `UiState` plumbing.
- Code is organized into the `:core:*` modules with the dependency rules enforced.
- Existing user data (`watna-compose.db`) survives the upgrade.
- Repository cache-first behavior is covered by unit tests.
- The codebase is ready for `:feature:*` specs to be authored and dropped in.

---

## 9. Out of Scope (handled by later specs)

- Rich audio player (Media3/ExoPlayer + MediaSessionService): speed, sleep timer, skip ±15s, lock-screen/now-playing, background audio, prev/next, auto-advance, share.
- Favorites screen and its persistence.
- Downloads manager + background download engine (WorkManager): pause/resume/retry/cancel, completed list, resume data.
- Search on lists.
- Books: cover grid, in-app M3 PDF reader, bookmarks, reading-progress save/restore.
- Settings/About content beyond the shell route.
- Nav-state persistence across cold launch; deep links.
- Duration·size probing (HTTP HEAD).

---

## 10. Open Questions / To confirm during planning

- Exact M3 tonal values for the teal scheme (finalize with the Material Theme Builder).
- Whether to keep Gson/Moshi for JSON or move to `kotlinx-serialization` for consistency with the route serialization (lean: consolidate on `kotlinx-serialization`, but the current DTO mappings work — decide in the plan).
- `build-logic` convention plugin vs. a simpler shared Gradle approach if module count stays small.
