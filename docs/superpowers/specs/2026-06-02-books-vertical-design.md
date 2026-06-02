# Books Feature Vertical (Design Spec)

**Date:** 2026-06-02
**Status:** Approved (brainstorming) → ready for implementation plan
**Builds on:** the Material foundation (`docs/superpowers/specs/2026-05-31-material-redesign-foundation-design.md`), now merged to `main`.
**Goal:** Deliver the Books experience on the new foundation — an M3 cover grid + search and an in-app PDF reader with bookmarks and reading-progress resume — matching the iOS app, and migrate Books off RxJava onto `:core:data`.

---

## 1. Background

Today's Android Books tab is a **list** (thumbnail + title + HTML detail + download %), opens PDFs via an intent / the `Pdf-Viewer` library (separate Activity), downloads through the legacy RxJava `FileService`, and has **no** search, bookmarks, or reading progress. The iOS app presents a **2-column cover grid** with a category badge + search, and an in-app **PDFKit reader** with a thumbnail strip, page navigation, share, bookmarks, and reading-progress resume.

The foundation already provides: `:core:data` `BookRepository` (cache-first, coroutines/Flow, preserves state on refresh), `Book` model, `BookEntity`/`BookDao`, `BookService` (suspend Retrofit), the teal M3 design system + state views, `UiState`, and an adaptive navigation shell. The API (`etipitaka.org/.../api?method=getitem`) returns `category` and `description` (verified) — Android's DTO currently drops them.

---

## 2. Decisions (locked during brainstorming)

| # | Decision | Choice |
|---|----------|--------|
| D1 | Module | New `:feature:books` (depends on `:core:*` only) |
| D2 | PDF rendering | Native `android.graphics.pdf.PdfRenderer` + Compose; **page-flip** `HorizontalPager`; per-page pinch-zoom |
| D3 | Download | **Tap-to-read** (foreground, in reader); `FileDownloader` in `:core:data` returning `Flow<DownloadProgress>`; reuse on-disk path `…/buddhawajana/books/{bookId}.pdf`; long-press cover → re-download/delete; **downloaded-state derived from disk file existence** |
| D4 | Category badge | Add `category` to `BookDto` / `Book` / `BookEntity`; show on grid |
| D5 | Bookmarks + reading progress | New Room tables + entities/DAOs/repositories + pure domain models (`Long` epoch-millis timestamps) |
| D6 | DB strategy | New `:core:data` DB uses a **separate file `buddhawajana.db`**; legacy `watna-compose.db` stays untouched for Audio/YouTube; consolidate when the Audio vertical migrates |
| D7 | Grid | Adaptive columns (min cell width); cover + title + category badge + download indicator; search (title substring); pull-to-refresh; `UiState` states; snackbar on silent-refresh failure |
| D8 | Reader paging | Page-flip; full-screen route |
| D9 | Nav | App-level `NavHost`: `Home` (tab scaffold) + full-screen `Reader(bookId)` route |
| D10 | DI / legacy | Register `dataModule` + `booksModule`; remove legacy Book repo/VM/`BookScreen`; Audio/YouTube legacy screens untouched |

---

## 3. Module Architecture (D1, D9, D10)

```
:feature:books            (buddhawajana.android.compose + kotlin.serialization; deps :core:{ui,designsystem,data,model,common})
├─ BooksModule.kt          Koin: BookListViewModel, ReaderViewModel
├─ navigation/
│   ├─ BookRoutes.kt       @Serializable BooksList ; @Serializable Reader(bookId: Long)
│   └─ booksNavGraph()      NavGraphBuilder ext contributing the two destinations
├─ list/
│   ├─ BookListScreen.kt
│   └─ BookListViewModel.kt
└─ reader/
    ├─ ReaderScreen.kt
    ├─ ReaderViewModel.kt
    └─ PdfDocument.kt       PdfRenderer wrapper: open(File), pageCount, renderPage(index, targetWidth): Bitmap, close()
```

**App-shell change (small):** today's `BuddhawajanaNavHost` switches tab content with `when(selected)`. Replace with an **app-level `NavHost`**:
- `Home` destination = the `NavigationSuiteScaffold` with the three tabs (Audio/Books/YouTube via `when(selected)` still fine for the not-yet-migrated tabs).
- `Reader(bookId)` destination = full-screen (covers the navigation bar) for distraction-free reading (iOS-style).
- Books tab renders `BookListScreen(onOpenBook = { id -> navController.navigate(Reader(id)) })`.

ViewModels extend `:core:ui` `BaseViewModel` (snackbar events). Screens consume `StateFlow<UiState<…>>`.

---

## 4. Data Layer (D3, D4, D5, D6)

### 4.1 Category (server-owned)
- `BookDto` `+ category: String?` (JSON `"category"`).
- `Book` model `+ category: String?`.
- `BookEntity` `+ @ColumnInfo("category") category: String = ""`; mappers updated. On `BookRepository.refresh()` merge, `category` is applied from the server (it is not a preserved user column).

### 4.2 New domain models (`:core:model`, pure Kotlin)
```
Bookmark(id: Long, bookId: Long, page: Int, note: String, addedAt: Long)
ReadingProgress(bookId: Long, page: Int, updatedAt: Long)
```
(`Long` epoch-millis to avoid Date/Converter coupling.)

### 4.3 New Room tables (`:core:data`)
- `bookmark`: autoincrement `id` PK, `book_id` (indexed), `page`, `note`, `added_at` → `BookmarkDao.stream(bookId): Flow<List<BookmarkEntity>>` (ORDER BY page), `insert(entity)`, `delete(entity)` (suspend) → `BookmarkRepository`.
- `reading_progress`: `book_id` PK, `page`, `updated_at` → `ReadingProgressDao.get(bookId): suspend …?`, `upsert(entity)` (REPLACE) → `ReadingProgressRepository`.

### 4.4 Database strategy (D6) — separate coexisting file
- The `:core:data` `AppDatabase` is renamed to a **new file `buddhawajana.db`**, schema **version 1** (fresh — no prior copies in the field), containing `book` (with `category`), `album`, `audio`, `bookmark`, `reading_progress`. The previously-built `MIGRATION_1_2` (which targeted the legacy `watna-compose.db` schema) is **removed** from the new DB — it has no role on a fresh file.
- The legacy `watna-compose.db` (v2) and legacy `AppDatabase` remain **untouched**, owned solely by the still-legacy Audio/YouTube screens.
- Two distinct files → no Room dual-owner conflict.
- **No user-data loss:** audio data stays in the legacy file; downloaded book PDFs on disk are reused via existence check (below); the book list is refetched from the API into the new DB; bookmarks/progress/category are new.
- **Future consolidation:** when the Audio vertical migrates, fold Audio onto `buddhawajana.db` and retire `watna-compose.db` (optionally a one-time import). Out of scope here.

### 4.5 Downloading (D3)
- `FileDownloader` (`:core:data`): `fun download(url: String, dest: File): Flow<DownloadProgress>` — OkHttp streaming, emits `Progress(bytesDownloaded, bytesTotal)` then terminal `Done`/`Failed`; writes to a temp file then atomically renames to `dest`.
- `BookFileStore` (`:core:data`): resolves `context.getExternalFilesDir(...)/buddhawajana/books/{bookId}.pdf`, `exists(bookId)`, `file(bookId)`, `delete(bookId)`.
- **Downloaded-state = `BookFileStore.exists(bookId)`** (self-healing; survives DB resets). The grid indicator and the reader open-flow both read disk existence; transient download progress is **UI state only** (not persisted). The `BookEntity.status/progress` columns are left unused by Books.
- Provided via `dataModule`. Reuses the OkHttp client from `networkModule`.

---

## 5. Books Grid Screen (D7)

`BookListScreen` + `BookListViewModel`.
- **ViewModel:** exposes `StateFlow<UiState<List<Book>>>` derived from `BookRepository.stream()` combined with the search query and per-book download presence; calls `BookRepository.refresh()` on init + pull-to-refresh; emits a snackbar message on refresh failure when cache is present (silent), or surfaces `UiState.Error` when cache is empty.
- **UI:** adaptive `LazyVerticalGrid` (min cell width, ~2 cols phone / more on tablet). Cell = `CachedAsyncImage` cover + title + **category badge** + download indicator (not-downloaded ⬇ / downloading ring% / on-device ✓). M3 search field filters by title (case-insensitive substring, iOS parity). Pull-to-refresh. `LoadingView`/`EmptyStateView`/`ErrorView(retry)` from `UiState`.
- **Interactions:** tap → `navigate(Reader(bookId))`; long-press → context menu (re-download / delete via `BookFileStore`).

---

## 6. PDF Reader (D2, D8)

`ReaderScreen` + `ReaderViewModel` + `PdfDocument`.
- **Open flow / state machine:** `ReaderState = Loading | Downloading(progress) | Ready(pdf, pageCount) | Error(message, retry)`.
  1. On enter, resolve `BookFileStore`. If file exists → open `PdfDocument` → `Ready`.
  2. Else collect `FileDownloader.download(book.fileUrl, dest)` → `Downloading(%)` → on `Done` open → `Ready`; on `Failed` → `Error(retry)`.
  3. On `Ready`, load `ReadingProgressRepository.get(bookId)` and set the pager's initial page.
- **Rendering:** `PdfDocument` wraps `PdfRenderer` (open the `File` via `ParcelFileDescriptor`); `renderPage(index, targetWidth)` → `Bitmap` with a small windowed bitmap cache (current ± N) and recycle on eviction; `HorizontalPager(pageCount)`; each page is a zoomable `Image` (pinch/pan via `transformable`/`graphicsLayer`). `PdfDocument.close()` on dispose.
- **Chrome (center-tap toggles visibility):**
  - Top app bar: back · book title · bookmark-toggle (adds/removes a bookmark at the current page) · share · overflow.
  - Bottom bar: page scrubber (`Slider` over pages) + "หน้า N / M" + prev/next + goto-page (number → input dialog).
  - Thumbnail strip: `LazyRow` of low-res page thumbnails (rendered via `PdfDocument` at small scale, cached) → tap to jump; current page highlighted.
- **Bookmarks:** add at current page with auto-note "หน้า N" (`BookmarkRepository.insert`); a bottom-sheet lists `BookmarkRepository.stream(bookId)` (tap → jump, swipe/delete); pages with a bookmark show an indicator.
- **Reading progress:** persist the current page to `ReadingProgressRepository` (debounced on page change + on `onDispose`); restored on reopen (step 3).
- **Share:** share the downloaded PDF via a correctly-configured `FileProvider` (verify/define the authority in the app manifest; the legacy authority string has a typo — use the correct one).

---

## 7. Error Handling

- **List:** refresh failure with cache → snackbar (content stays); with empty cache → `ErrorView(retry)`. Empty result → `EmptyStateView`.
- **Reader:** download failure → `Error(retry)`; corrupt/unopenable PDF (PdfRenderer throws) → `Error` with a "file may be corrupt; re-download" action (deletes the file, allows retry).
- **Coroutines:** repository calls use the foundation's `runCatchingCancellable` (cancellation-safe).

---

## 8. Testing

- **Unit (`:core:data`):** `BookmarkRepository` (insert/stream/delete), `ReadingProgressRepository` (get/upsert), `FileDownloader` progress emissions + atomic rename (fake/local server or a fed `InputStream`), updated book mappers including `category`. Fakes for DAOs (in-memory `Flow`), per the foundation pattern.
- **Unit (`:feature:books`):** `BookListViewModel` (`UiState` transitions, search filter, refresh-failure snackbar), `ReaderViewModel` (state machine: exists→Ready, missing→Downloading→Ready, failure→Error, progress restore). `PdfDocument` is thin over the platform; cover its index/cache logic where feasible, otherwise exercise via an instrumented test.
- **No DB migration test needed** for the new `buddhawajana.db` (fresh file, version 1, no prior schema).
- **Device smoke (manual / instrumented):** open a book → downloads → renders → flip/zoom → bookmark add/jump/delete → close & reopen resumes page → share. (Runtime-only; cannot be verified in a headless environment.)

---

## 9. Definition of Done

- New `:feature:books` builds; Books tab shows the adaptive M3 cover grid (cover + title + category badge) with search, pull-to-refresh, and `UiState` states, backed by `BookRepository` (no RxJava in the Books path).
- Tapping a book opens the full-screen reader; first open downloads (progress) then renders; subsequent opens render from disk; reading position resumes.
- Bookmarks add/list/jump/delete; thumbnail strip + scrubber + goto navigate; share works.
- New `buddhawajana.db` (fresh schema, version 1) holds books/bookmarks/progress/category; legacy `watna-compose.db` untouched and Audio/YouTube still work.
- Unit tests for the new repositories + ViewModels pass; whole project builds.

---

## 10. Out of Scope (later specs)

- Audio vertical (catalog/player/favorites/downloads) and the eventual DB consolidation onto `buddhawajana.db`.
- The shared WorkManager background-download engine + Downloads manager UI (Books uses a foreground downloader only).
- PDF text selection / search-within-document, annotations, night-mode PDF inversion.
- Cross-device sync of bookmarks/progress.

---

## 11. Open Questions / To confirm during planning

- Exact `FileProvider` authority + `file_paths.xml` (reuse the app's existing provider; fix the typo'd authority).
- Bitmap cache window size + max render dimension (memory vs sharpness) for large dharma PDFs — tune during implementation.
- Whether to keep `album`/`audio` entities in the new `buddhawajana.db` now (they're already defined; harmless) or omit until the Audio vertical — lean: keep, unused, to avoid a later schema bump.
