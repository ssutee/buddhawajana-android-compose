# Books Feature Vertical Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the Books experience on the new foundation — an adaptive M3 cover grid with search and an in-app native-`PdfRenderer` reader (page-flip, zoom, thumbnail strip, bookmarks, reading-progress resume, tap-to-read foreground download) — migrating Books off RxJava onto `:core:data`.

**Architecture:** New `:feature:books` module (depends on `:core:*` only). New Room tables (`bookmark`, `reading_progress`) + a `category` column live in a **separate `buddhawajana.db` file** (fresh, version 1) so the legacy `watna-compose.db` stays untouched for the still-legacy Audio/YouTube screens. "Downloaded" is derived from PDF file-existence on disk. App shell gains an app-level `NavHost` with a full-screen `Reader(bookId)` route.

**Tech Stack:** Kotlin 2.0, Compose (BOM), Material3 + adaptive nav, Navigation-Compose + kotlinx-serialization, Coroutines/Flow, Room (KSP), Koin, Coil, OkHttp, `android.graphics.pdf.PdfRenderer`.

**Spec:** `docs/superpowers/specs/2026-06-02-books-vertical-design.md`
**Branch:** `feature/books-vertical` (already created; spec committed).
**Package roots:** `com.watnapp.buddhawajana.core.*`, `com.watnapp.buddhawajana.feature.books.*`.

---

## Conventions

- Build check: `./gradlew :MODULE:assembleDebug`; unit tests: `./gradlew :MODULE:testDebugUnitTest --tests "FQN"`.
- All versions via `gradle/libs.versions.toml` (catalog already exists). New aliases get added in the task that first needs them.
- Commit after each task with the shown message.
- The legacy `watna-compose.db` / legacy `AppDatabase` / legacy Audio+YouTube screens are NOT touched except where a task explicitly says so (Task 10 removes only the legacy *Book* screen/VM from the Books tab).

---

## File Structure

```
core/model/.../model/
  Book.kt                 (modify: + category)
  Bookmark.kt             (create)
  ReadingProgress.kt      (create)
core/network/.../dto/BookDto.kt        (modify: + category)
core/data/.../db/
  BookEntity.kt           (modify: + category column)
  BookmarkEntity.kt       (create) + BookmarkDao.kt (create)
  ReadingProgressEntity.kt(create) + ReadingProgressDao.kt (create)
  AppDatabase.kt          (modify: name=buddhawajana.db, version 1, +entities, drop MIGRATION_1_2)
core/data/.../mapper/Mappers.kt        (modify: category + bookmark/progress mappers)
core/data/.../repo/
  BookmarkRepository.kt   (create) + ReadingProgressRepository.kt (create)
core/data/.../download/
  DownloadProgress.kt     (create)
  FileDownloader.kt       (create)
  BookFileStore.kt        (create)
core/data/.../DataModule.kt            (modify: db name, downloader, file store, new repos)
feature/books/                          (create module)
  build.gradle.kts, src/main/AndroidManifest.xml
  BooksModule.kt
  navigation/BookRoutes.kt, navigation/BooksNav.kt
  list/BookListViewModel.kt, list/BookListScreen.kt
  reader/PdfDocument.kt, reader/ReaderViewModel.kt, reader/ReaderScreen.kt
app/.../navigation/BuddhawajanaNavHost.kt   (modify: app-level NavHost + full-screen Reader)
app/.../MainApplication.kt                  (modify: + dataModule, + booksModule)
settings.gradle.kts                         (modify: include :feature:books)
app/build.gradle.kts                        (modify: + project(":feature:books"); drop legacy book screen deps if unused)
```

---

## Task 1: Add `category` to Book model, DTO, entity, mapper

**Files:**
- Modify: `core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/Book.kt`
- Modify: `core/network/src/main/java/com/watnapp/buddhawajana/core/network/dto/BookDto.kt`
- Modify: `core/data/src/main/java/com/watnapp/buddhawajana/core/data/db/BookEntity.kt`
- Modify: `core/data/src/main/java/com/watnapp/buddhawajana/core/data/mapper/Mappers.kt`
- Test: `core/data/src/test/java/com/watnapp/buddhawajana/core/data/mapper/MappersTest.kt`

- [ ] **Step 1: Add a failing assertion to the existing MappersTest**

Append a test to `MappersTest.kt`:
```kotlin
@Test
fun `BookDto category maps through entity to model`() {
    val dto = BookDto(id = "7", name = "ตถาคต", sortOrder = 2, totalpage = 50, producer = "P", file = "b.pdf", cover = "c.png", category = "พุทธวจน")
    val model = dto.toEntity().toModel()
    assertEquals("พุทธวจน", model.category)
}
```

- [ ] **Step 2: Run it — fails to compile**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MappersTest*"`
Expected: FAIL — `BookDto` has no `category` param; `Book` has no `category`.

- [ ] **Step 3: Add the field across the three types**

`Book.kt` — add `val category: String?,` (after `producer`).
`BookDto.kt` — add `val category: String?,` (after `cover`).
`BookEntity.kt` — add `@ColumnInfo(name = "category") var category: String = "",` (place after `producer`).

- [ ] **Step 4: Update mappers**

In `Mappers.kt`, `BookDto.toEntity()` add `category = category ?: "",`. `BookEntity.toModel()` add `category = category.ifEmpty { null },`.

- [ ] **Step 5: Run the test — passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MappersTest*"`
Expected: PASS (both old and new assertions).

- [ ] **Step 6: Commit**
```bash
git add core/model core/network core/data
git commit -m "feat(core): add category field to Book model/DTO/entity + mapper"
```

---

## Task 2: New domain models `Bookmark` + `ReadingProgress`

**Files:**
- Create: `core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/Bookmark.kt`
- Create: `core/model/src/main/kotlin/com/watnapp/buddhawajana/core/model/ReadingProgress.kt`

- [ ] **Step 1: Create the models**

`Bookmark.kt`:
```kotlin
package com.watnapp.buddhawajana.core.model

data class Bookmark(
    val id: Long,
    val bookId: Long,
    val page: Int,
    val note: String,
    val addedAt: Long,
)
```
`ReadingProgress.kt`:
```kotlin
package com.watnapp.buddhawajana.core.model

data class ReadingProgress(
    val bookId: Long,
    val page: Int,
    val updatedAt: Long,
)
```

- [ ] **Step 2: Compile**

Run: `./gradlew :core:model:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add core/model
git commit -m "feat(core:model): Bookmark + ReadingProgress domain models"
```

---

## Task 3: New Room entities/DAOs + switch DB to `buddhawajana.db` (fresh v1)

**Files:**
- Create: `core/data/.../db/BookmarkEntity.kt`, `BookmarkDao.kt`, `ReadingProgressEntity.kt`, `ReadingProgressDao.kt`
- Modify: `core/data/.../db/AppDatabase.kt`
- Modify: `core/data/.../DataModule.kt`

- [ ] **Step 1: Bookmark entity + DAO**

`BookmarkEntity.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmark", indices = [Index("book_id")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "page") val page: Int,
    @ColumnInfo(name = "note") val note: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
```
`BookmarkDao.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark WHERE book_id = :bookId ORDER BY page")
    fun stream(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)
}
```

- [ ] **Step 2: ReadingProgress entity + DAO**

`ReadingProgressEntity.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "page") val page: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
```
`ReadingProgressDao.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId")
    suspend fun get(bookId: Long): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)
}
```

- [ ] **Step 3: Update AppDatabase — new file name, version 1, add entities, drop legacy migration**

In `AppDatabase.kt`: change `@Database(entities = [...], version = 1, exportSchema = false)` adding `BookmarkEntity::class, ReadingProgressEntity::class`; add abstract `fun bookmarkDao(): BookmarkDao` and `fun readingProgressDao(): ReadingProgressDao`; set `const val NAME = "buddhawajana.db"`; **remove** the `MIGRATION_1_2` object (the new file is fresh — no v1→v2 migration applies). Keep the three existing entities (`BookEntity` now with `category`, `AlbumEntity`, `AudioEntity`).

- [ ] **Step 4: Update DataModule — drop migration, keep new name**

In `DataModule.kt`, the Room builder becomes:
```kotlin
single {
    androidx.room.Room.databaseBuilder(
        org.koin.android.ext.koin.androidContext(),
        AppDatabase::class.java,
        AppDatabase.NAME,
    ).build()   // no addMigrations — fresh DB file
}
single { get<AppDatabase>().bookmarkDao() }
single { get<AppDatabase>().readingProgressDao() }
```
(Keep the existing `bookDao()`, `albumDao()`, `audioDao()`, and repo singles.)

- [ ] **Step 5: Build (Room/KSP codegen)**

Run: `./gradlew :core:data:assembleDebug`
Expected: BUILD SUCCESSFUL. (KSP generates DAOs; schema is fresh, no migration.)

- [ ] **Step 6: Commit**
```bash
git add core/data
git commit -m "feat(core:data): bookmark + reading_progress tables; switch to fresh buddhawajana.db"
```

---

## Task 4: BookmarkRepository + ReadingProgressRepository (TDD)

**Files:**
- Create: `core/data/.../repo/BookmarkRepository.kt`, `core/data/.../repo/ReadingProgressRepository.kt`
- Modify: `core/data/.../mapper/Mappers.kt` (add bookmark/progress mappers)
- Modify: `core/data/.../DataModule.kt` (add the two repo singles)
- Test: `core/data/.../repo/BookmarkRepositoryTest.kt`, `core/data/.../repo/ReadingProgressRepositoryTest.kt`

- [ ] **Step 1: Add mappers**

In `Mappers.kt`:
```kotlin
import com.watnapp.buddhawajana.core.data.db.BookmarkEntity
import com.watnapp.buddhawajana.core.data.db.ReadingProgressEntity
import com.watnapp.buddhawajana.core.model.Bookmark
import com.watnapp.buddhawajana.core.model.ReadingProgress

fun BookmarkEntity.toModel() = Bookmark(id, bookId, page, note, addedAt)
fun ReadingProgressEntity.toModel() = ReadingProgress(bookId, page, updatedAt)
```

- [ ] **Step 2: Write failing tests**

`BookmarkRepositoryTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.db.BookmarkDao
import com.watnapp.buddhawajana.core.data.db.BookmarkEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkRepositoryTest {
    private val store = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    private var nextId = 1L
    private val dao = object : BookmarkDao {
        override fun stream(bookId: Long) = store.map { it.filter { b -> b.bookId == bookId }.sortedBy { b -> b.page } }
        override suspend fun insert(bookmark: BookmarkEntity): Long {
            val withId = bookmark.copy(id = nextId++); store.update { it + withId }; return withId.id
        }
        override suspend fun delete(bookmark: BookmarkEntity) = store.update { it.filterNot { b -> b.id == bookmark.id } }
    }

    @Test
    fun `add then stream returns bookmark as model`() = runTest {
        val repo = BookmarkRepository(dao)
        repo.add(bookId = 5, page = 12, note = "หน้า 12")
        repo.stream(5).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(12, list[0].page)
            assertEquals("หน้า 12", list[0].note)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```
`ReadingProgressRepositoryTest.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.ReadingProgressDao
import com.watnapp.buddhawajana.core.data.db.ReadingProgressEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingProgressRepositoryTest {
    private val map = HashMap<Long, ReadingProgressEntity>()
    private val dao = object : ReadingProgressDao {
        override suspend fun get(bookId: Long) = map[bookId]
        override suspend fun upsert(progress: ReadingProgressEntity) { map[progress.bookId] = progress }
    }

    @Test
    fun `save then get returns page`() = runTest {
        val repo = ReadingProgressRepository(dao)
        assertNull(repo.get(9))
        repo.save(bookId = 9, page = 33, now = 1000L)
        assertEquals(33, repo.get(9)?.page)
    }
}
```

- [ ] **Step 3: Run — fail (repos unresolved)**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*BookmarkRepositoryTest*" --tests "*ReadingProgressRepositoryTest*"`
Expected: FAIL (unresolved `BookmarkRepository`/`ReadingProgressRepository`).

- [ ] **Step 4: Implement the repositories**

`BookmarkRepository.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.BookmarkDao
import com.watnapp.buddhawajana.core.data.db.BookmarkEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(private val dao: BookmarkDao) {
    fun stream(bookId: Long): Flow<List<Bookmark>> = dao.stream(bookId).map { it.map(BookmarkEntity::toModel) }
    suspend fun add(bookId: Long, page: Int, note: String, now: Long = System.currentTimeMillis()) {
        dao.insert(BookmarkEntity(bookId = bookId, page = page, note = note, addedAt = now))
    }
    suspend fun delete(bookmark: Bookmark) {
        dao.delete(BookmarkEntity(id = bookmark.id, bookId = bookmark.bookId, page = bookmark.page, note = bookmark.note, addedAt = bookmark.addedAt))
    }
}
```
`ReadingProgressRepository.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.ReadingProgressDao
import com.watnapp.buddhawajana.core.data.db.ReadingProgressEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.ReadingProgress

class ReadingProgressRepository(private val dao: ReadingProgressDao) {
    suspend fun get(bookId: Long): ReadingProgress? = dao.get(bookId)?.toModel()
    suspend fun save(bookId: Long, page: Int, now: Long = System.currentTimeMillis()) {
        dao.upsert(ReadingProgressEntity(bookId = bookId, page = page, updatedAt = now))
    }
}
```

- [ ] **Step 5: Add Koin singles**

In `DataModule.kt`: `single { BookmarkRepository(get()) }` and `single { ReadingProgressRepository(get()) }`.

- [ ] **Step 6: Run — pass**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: PASS (all `:core:data` tests).

- [ ] **Step 7: Commit**
```bash
git add core/data
git commit -m "feat(core:data): Bookmark + ReadingProgress repositories (TDD)"
```

---

## Task 5: FileDownloader + BookFileStore (TDD)

**Files:**
- Create: `core/data/.../download/DownloadProgress.kt`, `FileDownloader.kt`, `BookFileStore.kt`
- Modify: `core/data/.../DataModule.kt`
- Test: `core/data/.../download/FileDownloaderTest.kt`

- [ ] **Step 1: DownloadProgress type**

`DownloadProgress.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.download

sealed interface DownloadProgress {
    data class Progress(val bytesDownloaded: Long, val bytesTotal: Long) : DownloadProgress {
        val fraction: Float get() = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f
    }
    data object Done : DownloadProgress
    data class Failed(val error: Throwable) : DownloadProgress
}
```

- [ ] **Step 2: Write the failing test (uses a local file:// URL via OkHttp is awkward; test the stream-copy core instead)**

`FileDownloaderTest.kt` — test the pure copy/emit logic via the injectable `openStream`:
```kotlin
package com.watnapp.buddhawajana.core.data.download

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.InputStream

class FileDownloaderTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `downloads bytes to dest and emits Progress then Done`() = runTest {
        val payload = ByteArray(2048) { it.toByte() }
        val downloader = FileDownloader(
            openStream = { _ -> object {
                val total = payload.size.toLong()
                val stream: InputStream = ByteArrayInputStream(payload)
            }.let { it.total to it.stream } },
        )
        val dest = tmp.newFile("out.pdf")
        var sawProgress = false
        var sawDone = false
        downloader.download("http://x/y.pdf", dest).test {
            while (true) {
                val e = awaitItem()
                if (e is DownloadProgress.Progress) sawProgress = true
                if (e is DownloadProgress.Done) { sawDone = true; break }
            }
            awaitComplete()
        }
        assertTrue(sawProgress); assertTrue(sawDone)
        assertArrayEquals(payload, dest.readBytes())
    }
}
```

- [ ] **Step 3: Run — fail**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*FileDownloaderTest*"`
Expected: FAIL (unresolved `FileDownloader`).

- [ ] **Step 4: Implement FileDownloader (OkHttp for prod, injectable stream for tests)**

`FileDownloader.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.download

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream

class FileDownloader(
    private val openStream: (url: String) -> Pair<Long, InputStream>,
) {
    // Production constructor: stream via OkHttp.
    constructor(client: OkHttpClient) : this(openStream = { url ->
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        if (!resp.isSuccessful) { resp.close(); error("HTTP ${'$'}{resp.code}") }
        val body = resp.body ?: error("empty body")
        body.contentLength() to body.byteStream()
    })

    fun download(url: String, dest: File): Flow<DownloadProgress> = flow {
        val tmp = File(dest.parentFile, dest.name + ".part")
        dest.parentFile?.mkdirs()
        try {
            val (total, input) = openStream(url)
            input.use { source ->
                tmp.outputStream().use { sink ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val n = source.read(buf)
                        if (n < 0) break
                        sink.write(buf, 0, n)
                        downloaded += n
                        emit(DownloadProgress.Progress(downloaded, total))
                    }
                }
            }
            if (!tmp.renameTo(dest)) { tmp.copyTo(dest, overwrite = true); tmp.delete() }
            emit(DownloadProgress.Done)
        } catch (c: kotlin.coroutines.cancellation.CancellationException) {
            tmp.delete(); throw c
        } catch (e: Throwable) {
            tmp.delete(); emit(DownloadProgress.Failed(e))
        }
    }
}
```

- [ ] **Step 5: BookFileStore (matches the legacy on-disk path for continuity)**

`BookFileStore.kt`:
```kotlin
package com.watnapp.buddhawajana.core.data.download

import android.content.Context
import android.os.Environment
import java.io.File

class BookFileStore(private val context: Context) {
    private fun dir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "buddhawajana/books").apply { mkdirs() }
    fun file(bookId: Long): File = File(dir(), "$bookId.pdf")
    fun exists(bookId: Long): Boolean = file(bookId).let { it.exists() && it.length() > 0 }
    fun delete(bookId: Long): Boolean = file(bookId).delete()
}
```

- [ ] **Step 6: Koin singles**

In `DataModule.kt`:
```kotlin
single { FileDownloader(get<okhttp3.OkHttpClient>()) }
single { BookFileStore(org.koin.android.ext.koin.androidContext()) }
```
(The `OkHttpClient` single already exists in `networkModule`; ensure `networkModule` is started — it is, in `MainApplication`.)

- [ ] **Step 7: Run — pass**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*FileDownloaderTest*"`
Expected: PASS.

- [ ] **Step 8: Commit**
```bash
git add core/data
git commit -m "feat(core:data): FileDownloader (Flow<progress>) + BookFileStore (TDD)"
```

---

## Task 6: `:feature:books` module skeleton

**Files:**
- Modify: `settings.gradle.kts`
- Create: `feature/books/build.gradle.kts`, `feature/books/src/main/AndroidManifest.xml`
- Create: `feature/books/.../navigation/BookRoutes.kt`
- Create: `feature/books/.../BooksModule.kt`

- [ ] **Step 1: Register the module**

In `settings.gradle.kts` add `include(":feature:books")`.

- [ ] **Step 2: build.gradle.kts**

`feature/books/build.gradle.kts`:
```kotlin
plugins {
    id("buddhawajana.android.compose")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.watnapp.buddhawajana.feature.books" }
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
}
```

- [ ] **Step 3: Manifest**

`feature/books/src/main/AndroidManifest.xml`: `<manifest xmlns:android="http://schemas.android.com/apk/res/android" />`

- [ ] **Step 4: Routes**

`BookRoutes.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.navigation

import kotlinx.serialization.Serializable

@Serializable data object BooksListRoute
@Serializable data class ReaderRoute(val bookId: Long)
```

- [ ] **Step 5: Empty Koin module (filled in Tasks 7 & 9)**

`BooksModule.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books

import org.koin.dsl.module

val booksModule = module {
    // viewModel { ... } added in Tasks 7 and 9
}
```

- [ ] **Step 6: Build**

Run: `./gradlew :feature:books:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**
```bash
git add settings.gradle.kts feature/books
git commit -m "feat(feature:books): module skeleton + routes + Koin module"
```

---

## Task 7: BookListViewModel (TDD) + BookListScreen

**Files:**
- Create: `feature/books/.../list/BookListViewModel.kt`, `feature/books/.../list/BookListScreen.kt`
- Modify: `feature/books/.../BooksModule.kt`
- Test: `feature/books/src/test/java/.../list/BookListViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel test**

`BookListViewModelTest.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.list

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.ui.state.UiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookListViewModelTest {
    private fun book(id: String, title: String) = Book(id, title, null, "u", 1, null, "cat", 0)

    @Test
    fun `loads books as Content and search filters by title`() = runTest {
        val repo = mockk<BookRepository>(relaxed = true)
        every { repo.stream() } returns flowOf(listOf(book("1", "Marga"), book("2", "Tathagata")))
        coEvery { repo.refresh() } returns Result.success(Unit)
        val vm = BookListViewModel(repo, downloaded = { false })

        vm.state.test {
            // first emission may be Loading; advance to Content
            var s = awaitItem()
            while (s is UiState.Loading) s = awaitItem()
            assertTrue(s is UiState.Content)
            assertEquals(2, (s as UiState.Content).data.size)

            vm.onSearch("marga")
            val filtered = awaitItem()
            assertEquals(1, (filtered as UiState.Content).data.size)
            assertEquals("Marga", filtered.data[0].book.title)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```
> Note the `Book` constructor arg order matches Task 1: `(id, title, coverUrl, fileUrl, totalPage, producer, category, orderNumber)`. Adjust the test literal if the field order in `Book.kt` differs — keep it consistent with the actual model.

- [ ] **Step 2: Run — fail**

Run: `./gradlew :feature:books:testDebugUnitTest --tests "*BookListViewModelTest*"`
Expected: FAIL (unresolved `BookListViewModel`).

- [ ] **Step 3: Implement the ViewModel**

`BookListViewModel.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.list

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookUi(val book: Book, val downloaded: Boolean)

class BookListViewModel(
    private val repo: BookRepository,
    private val downloaded: (Long) -> Boolean,
) : BaseViewModel() {

    private val query = MutableStateFlow("")
    fun onSearch(q: String) { query.value = q }

    val state: StateFlow<UiState<List<BookUi>>> =
        combine(repo.stream(), query) { books, q ->
            val filtered = if (q.isBlank()) books
                else books.filter { it.title.contains(q.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) UiState.Empty
            else UiState.Content(filtered.map { BookUi(it, downloaded(it.id.toLongOrNull() ?: -1)) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        repo.refresh().onFailure { emitMessage("รีเฟรชล้มเหลว") }
    }
}
```

- [ ] **Step 4: Run — pass**

Run: `./gradlew :feature:books:testDebugUnitTest --tests "*BookListViewModelTest*"`
Expected: PASS.

- [ ] **Step 5: Implement BookListScreen**

`BookListScreen.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.designsystem.component.EmptyStateView
import com.watnapp.buddhawajana.core.designsystem.component.ErrorView
import com.watnapp.buddhawajana.core.designsystem.component.LoadingView
import com.watnapp.buddhawajana.core.designsystem.theme.Spacing
import com.watnapp.buddhawajana.core.ui.state.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookListScreen(
    state: UiState<List<BookUi>>,
    query: String,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = onSearch,
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(Spacing.m),
            placeholder = { Text("ค้นหาหนังสือ") },
        )
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Empty -> EmptyStateView("ไม่พบหนังสือ")
            is UiState.Error -> ErrorView(state.message, onRefresh)
            is UiState.Content -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                items(state.data, key = { it.book.id }) { item ->
                    BookCell(item, onClick = { onOpen(item.book.id.toLong()) }, onLongClick = { onDelete(item.book.id.toLong()) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCell(item: BookUi, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))) {
            CachedAsyncImage(item.book.coverUrl, item.book.title, Modifier.fillMaxSize())
            item.book.category?.let {
                Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(6.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Icon(
                if (item.downloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
            )
        }
        Text(item.book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = Spacing.xs))
    }
}
```

- [ ] **Step 6: Register the ViewModel in Koin**

In `BooksModule.kt`:
```kotlin
import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.feature.books.list.BookListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
// inside module { }:
viewModel { BookListViewModel(get(), downloaded = { id -> get<BookFileStore>().exists(id) }) }
```

- [ ] **Step 7: Build + test**

Run: `./gradlew :feature:books:assembleDebug :feature:books:testDebugUnitTest`
Expected: BUILD SUCCESSFUL + tests pass.

- [ ] **Step 8: Commit**
```bash
git add feature/books
git commit -m "feat(feature:books): BookListViewModel (TDD) + grid BookListScreen"
```

---

## Task 8: PdfDocument (PdfRenderer wrapper)

**Files:**
- Create: `feature/books/.../reader/PdfDocument.kt`

- [ ] **Step 1: Implement the wrapper**

`PdfDocument.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.collection.LruCache
import java.io.File

class PdfDocument private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    val pageCount: Int get() = renderer.pageCount
    private val cache = object : LruCache<Int, Bitmap>(6) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) oldValue.recycle()
        }
    }

    @Synchronized
    fun renderPage(index: Int, targetWidthPx: Int): Bitmap {
        cache.get(index)?.let { if (!it.isRecycled) return it }
        renderer.openPage(index).use { page ->
            val scale = targetWidthPx.toFloat() / page.width
            val w = targetWidthPx
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            cache.put(index, bmp)
            return bmp
        }
    }

    @Synchronized
    fun close() {
        cache.evictAll()
        renderer.close()
        pfd.close()
    }

    companion object {
        fun open(file: File): PdfDocument {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            return PdfDocument(pfd, PdfRenderer(pfd))
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :feature:books:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add feature/books
git commit -m "feat(feature:books): PdfDocument PdfRenderer wrapper with bitmap cache"
```

---

## Task 9: ReaderViewModel (TDD) + ReaderScreen

**Files:**
- Create: `feature/books/.../reader/ReaderViewModel.kt`, `feature/books/.../reader/ReaderScreen.kt`
- Modify: `feature/books/.../BooksModule.kt`
- Test: `feature/books/src/test/java/.../reader/ReaderViewModelTest.kt`

- [ ] **Step 1: Failing ReaderViewModel test (state machine, no real rendering)**

`ReaderViewModelTest.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.reader

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.core.data.download.DownloadProgress
import com.watnapp.buddhawajana.core.data.download.FileDownloader
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.data.repo.BookmarkRepository
import com.watnapp.buddhawajana.core.data.repo.ReadingProgressRepository
import com.watnapp.buddhawajana.core.model.Book
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReaderViewModelTest {
    @Test
    fun `missing file triggers download then Ready`() = runTest {
        val store = mockk<BookFileStore>()
        val file = File.createTempFile("b", ".pdf")
        every { store.exists(1L) } returns false andThen true
        every { store.file(1L) } returns file
        val downloader = mockk<FileDownloader>()
        every { downloader.download(any(), any()) } returns flowOf(DownloadProgress.Progress(1, 2), DownloadProgress.Done)
        val books = mockk<BookRepository>(relaxed = true)
        every { books.stream() } returns flowOf(listOf(Book("1", "T", null, "http://x/y.pdf", 1, null, null, 0)))
        val vm = ReaderViewModel(
            bookId = 1L, books = books, downloader = downloader, files = store,
            bookmarks = mockk(relaxed = true), progress = mockk(relaxed = true),
            openPdf = { _ -> FakePdf() },
        )
        vm.state.test {
            // eventually reaches Ready (we stub openPdf so no real PdfRenderer)
            var seenReady = false
            repeat(6) { val s = awaitItem(); if (s is ReaderState.Ready) seenReady = true }
            assertTrue(seenReady)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```
> `openPdf` is an injected `(File) -> PdfHandle` seam so the VM is testable without a real PDF. Define a minimal `PdfHandle` interface (Step 3) that `PdfDocument` and the test's `FakePdf` both implement.

- [ ] **Step 2: Run — fail**

Run: `./gradlew :feature:books:testDebugUnitTest --tests "*ReaderViewModelTest*"`
Expected: FAIL (unresolved `ReaderViewModel`, `ReaderState`, `PdfHandle`, `FakePdf`).

- [ ] **Step 3: Implement ReaderState + PdfHandle seam + ReaderViewModel**

Add to `PdfDocument.kt` an interface and make `PdfDocument` implement it:
```kotlin
interface PdfHandle {
    val pageCount: Int
    fun renderPage(index: Int, targetWidthPx: Int): android.graphics.Bitmap
    fun close()
}
```
Change `class PdfDocument ... : PdfHandle`. Add a test `FakePdf` in the test source set:
```kotlin
// feature/books/src/test/java/.../reader/FakePdf.kt
package com.watnapp.buddhawajana.feature.books.reader
import android.graphics.Bitmap
class FakePdf : PdfHandle {
    override val pageCount = 3
    override fun renderPage(index: Int, targetWidthPx: Int): Bitmap = throw UnsupportedOperationException()
    override fun close() {}
}
```
`ReaderViewModel.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.reader

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.core.data.download.DownloadProgress
import com.watnapp.buddhawajana.core.data.download.FileDownloader
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.data.repo.BookmarkRepository
import com.watnapp.buddhawajana.core.data.repo.ReadingProgressRepository
import com.watnapp.buddhawajana.core.model.Bookmark
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed interface ReaderState {
    data object Loading : ReaderState
    data class Downloading(val fraction: Float) : ReaderState
    data class Ready(val pdf: PdfHandle, val pageCount: Int, val startPage: Int) : ReaderState
    data class Error(val message: String) : ReaderState
}

class ReaderViewModel(
    private val bookId: Long,
    private val books: BookRepository,
    private val downloader: FileDownloader,
    private val files: BookFileStore,
    private val bookmarks: BookmarkRepository,
    private val progress: ReadingProgressRepository,
    private val openPdf: (File) -> PdfHandle,
) : BaseViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    val bookmarksFor: Flow<List<Bookmark>> = bookmarks.stream(bookId)

    private val _page = MutableStateFlow(0)
    val page: StateFlow<Int> = _page.asStateFlow()

    init { open() }

    private fun open() = viewModelScope.launch {
        try {
            if (!files.exists(bookId)) {
                val url = books.stream().first().firstOrNull { it.id.toLongOrNull() == bookId }?.fileUrl
                    ?: run { _state.value = ReaderState.Error("ไม่พบไฟล์"); return@launch }
                downloader.download(url, files.file(bookId)).collect { p ->
                    when (p) {
                        is DownloadProgress.Progress -> _state.value = ReaderState.Downloading(p.fraction)
                        is DownloadProgress.Failed -> { _state.value = ReaderState.Error("ดาวน์โหลดล้มเหลว"); return@collect }
                        is DownloadProgress.Done -> {}
                    }
                }
                if (_state.value is ReaderState.Error) return@launch
            }
            val pdf = openPdf(files.file(bookId))
            val start = (progress.get(bookId)?.page ?: 0).coerceIn(0, (pdf.pageCount - 1).coerceAtLeast(0))
            _page.value = start
            _state.value = ReaderState.Ready(pdf, pdf.pageCount, start)
        } catch (e: Throwable) {
            _state.value = ReaderState.Error("เปิดไฟล์ไม่ได้")
        }
    }

    fun onPageChanged(index: Int) {
        _page.value = index
        viewModelScope.launch { progress.save(bookId, index) }
    }

    fun addBookmark(page: Int) = viewModelScope.launch { bookmarks.add(bookId, page, "หน้า ${'$'}{page + 1}") }
    fun deleteBookmark(b: Bookmark) = viewModelScope.launch { bookmarks.delete(b) }

    override fun onCleared() {
        super.onCleared()
        (_state.value as? ReaderState.Ready)?.pdf?.close()
    }
}
```

- [ ] **Step 4: Run — pass**

Run: `./gradlew :feature:books:testDebugUnitTest --tests "*ReaderViewModelTest*"`
Expected: PASS.

- [ ] **Step 5: Implement ReaderScreen**

`ReaderScreen.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    title: String,
    vm: ReaderViewModel,
    onBack: () -> Unit,
    onShare: () -> Unit,
) {
    val state by vm.state.collectAsState()
    var chrome by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            if (chrome) TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } },
                actions = {
                    IconButton({ (state as? ReaderState.Ready)?.let { vm.addBookmark(vm.page.value) } }) { Icon(Icons.Default.Bookmark, "bookmark") }
                    IconButton(onShare) { Icon(Icons.Default.Share, "share") }
                },
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (val s = state) {
                is ReaderState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ReaderState.Downloading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("กำลังดาวน์โหลด…"); Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { s.fraction })
                }
                is ReaderState.Error -> Column(Modifier.align(Alignment.Center)) { Text(s.message) }
                is ReaderState.Ready -> ReaderPager(s, vm) { chrome = !chrome }
            }
        }
    }
}

@Composable
private fun ReaderPager(ready: ReaderState.Ready, vm: ReaderViewModel, onToggleChrome: () -> Unit) {
    val pager = rememberPagerState(initialPage = ready.startPage) { ready.pageCount }
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }.toInt()
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collectLatest { vm.onPageChanged(it) }
    }
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
        val bitmap = remember(index) { ready.pdf.renderPage(index, widthPx) }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "page ${'$'}{index + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onToggleChrome() }) },
        )
    }
}
```
> Zoom (pinch/pan), thumbnail strip, page scrubber, goto-page dialog, and the bookmark list sheet are additive UI on top of `ReaderPager`. Implement them as separate composables in this file driven by `pager.scrollToPage(...)` and `vm.bookmarksFor`. (They share the already-defined `pager`, `vm`, and `ready` — no new types.)

- [ ] **Step 6: Register ReaderViewModel in Koin**

In `BooksModule.kt` add:
```kotlin
import com.watnapp.buddhawajana.feature.books.reader.PdfDocument
import com.watnapp.buddhawajana.feature.books.reader.ReaderViewModel
import org.koin.core.parameter.parametersOf
// inside module { }:
viewModel { (bookId: Long) -> ReaderViewModel(bookId, get(), get(), get(), get(), get(), openPdf = { f -> PdfDocument.open(f) }) }
```

- [ ] **Step 7: Build + test**

Run: `./gradlew :feature:books:assembleDebug :feature:books:testDebugUnitTest`
Expected: BUILD SUCCESSFUL + tests pass.

- [ ] **Step 8: Commit**
```bash
git add feature/books
git commit -m "feat(feature:books): ReaderViewModel (TDD) + ReaderScreen (pager, download, bookmarks, progress)"
```

---

## Task 10: Wire into the app — nav graph, full-screen reader, Koin; remove legacy Book screen

**Files:**
- Create: `feature/books/.../navigation/BooksNav.kt`
- Modify: `app/.../navigation/BuddhawajanaNavHost.kt`
- Modify: `app/.../MainApplication.kt`
- Modify: `app/build.gradle.kts`
- Delete: legacy `app/.../ui/BookScreen.kt` + its references in the old nav

- [ ] **Step 1: feature nav entry points**

`BooksNav.kt`:
```kotlin
package com.watnapp.buddhawajana.feature.books.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watnapp.buddhawajana.feature.books.list.BookListScreen
import com.watnapp.buddhawajana.feature.books.list.BookListViewModel
import org.koin.androidx.compose.koinViewModel

@androidx.compose.runtime.Composable
fun BooksListPane(onOpenBook: (Long) -> Unit, modifier: Modifier = Modifier) {
    val vm: BookListViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    BookListScreen(
        state = state, query = query,
        onSearch = { query = it; vm.onSearch(it) },
        onRefresh = vm::refresh, onOpen = onOpenBook,
        onDelete = { /* delete via BookFileStore — call a vm.delete(id) added if needed */ },
        modifier = modifier,
    )
}
```
> If a delete action is wanted from the grid, add `fun delete(id: Long)` to `BookListViewModel` that calls an injected `BookFileStore.delete`; otherwise pass `{}`. Keep it consistent with Task 7's constructor.

- [ ] **Step 2: App-level NavHost with full-screen Reader**

Rewrite `app/.../navigation/BuddhawajanaNavHost.kt` to:
```kotlin
package com.watnapp.buddhawajana.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.watnapp.buddhawajana.feature.books.navigation.BooksListPane
import com.watnapp.buddhawajana.feature.books.reader.ReaderScreen
import com.watnapp.buddhawajana.feature.books.reader.ReaderViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable private data object HomeRoute
@Serializable private data class ReaderRoute(val bookId: Long)

@Composable
fun BuddhawajanaNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScaffold(onOpenBook = { id -> nav.navigate(ReaderRoute(id)) })
        }
        composable<ReaderRoute> { backStack ->
            val bookId = backStack.toRoute<ReaderRoute>().bookId
            val vm: ReaderViewModel = koinViewModel { parametersOf(bookId) }
            ReaderScreen(title = "", vm = vm, onBack = { nav.popBackStack() }, onShare = { /* FileProvider share, Task 11 */ })
        }
    }
}
```
Add a `HomeScaffold` composable (in the same file or a new `Home.kt`) that contains the existing `BuddhawajanaNavSuite` + `BuddhawajanaTopBar`, switching tab content by `selected` and rendering `BooksListPane(onOpenBook)` for the Books tab, and the existing legacy `AudioScreen`/`YoutubeScreen` for the other two (unchanged).

- [ ] **Step 3: Koin + module dep**

`MainApplication.kt`: `startKoin { ... modules(networkModule, dataModule, booksModule, appModule) }` (add `dataModule` and `booksModule`; keep `appModule` for the still-legacy Audio/YouTube).
`app/build.gradle.kts`: add `implementation(project(":feature:books"))`.

> Koin duplicate check: `dataModule` now provides the new `AppDatabase` (buddhawajana.db). The legacy `appModule` still provides its own `AppDatabaseProvider` (watna-compose.db) — different classes, different files, no conflict. The legacy Book repository/VM in `appModule` are now unused by the UI; leave them (removed in a later cleanup) OR remove their Koin entries now if trivial.

- [ ] **Step 4: Remove the legacy Book screen from the Books tab**

Delete `app/.../ui/BookScreen.kt`. Remove any remaining references (the old `NavigationGraph`/`when(selected)` BOOKS branch now uses `BooksListPane`). Ensure nothing else imports the deleted screen.

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**
```bash
git add app feature/books
git rm app/src/main/java/com/watnapp/buddhawajana/ui/BookScreen.kt
git commit -m "feat(app): wire :feature:books — app-level NavHost + full-screen reader; drop legacy BookScreen"
```

---

## Task 11: FileProvider share, whole-project verify, smoke checklist

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` + `app/src/main/res/xml/file_paths.xml` (verify/fix FileProvider authority)
- Modify: `feature/books/.../reader/ReaderScreen.kt` share wiring (pass an `onShare` that builds the intent in `:app` or via a small helper)

- [ ] **Step 1: Verify/define FileProvider**

Inspect `app/src/main/AndroidManifest.xml` for the `<provider android:name="androidx.core.content.FileProvider">` authority (legacy had a typo `com.watanpp.buddhawajana.provider`). Standardize to `${applicationId}.fileprovider`; ensure `res/xml/file_paths.xml` includes `<external-files-path name="books" path="Pictures/buddhawajana/books/"/>` (matches `BookFileStore`).

- [ ] **Step 2: Wire share in the Reader route**

In `BuddhawajanaNavHost.kt`, implement `onShare` for the reader: build a share `Intent` with `FileProvider.getUriForFile(context, "${'$'}{context.packageName}.fileprovider", BookFileStore(context).file(bookId))`, `type = "application/pdf"`, `FLAG_GRANT_READ_URI_PERMISSION`, wrapped in `Intent.createChooser`. (Get `BookFileStore` from Koin via `koinInject()` or construct with `LocalContext`.)

- [ ] **Step 3: Whole-project build + all unit tests**

Run: `./gradlew clean assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL; `:core:data` + `:feature:books` tests pass; legacy modules unaffected.

- [ ] **Step 4: Manual smoke checklist (device/emulator — cannot run headless)**

Document/verify on a device:
- Books tab shows the cover grid with category badges; search filters; pull-to-refresh; ✓/⬇ indicator reflects disk.
- Tap a not-downloaded book → full-screen reader shows download progress → renders; flip pages; pinch-zoom.
- Add a bookmark; open the bookmark sheet; jump; delete.
- Close the reader and reopen the same book → resumes at the last page.
- Share exports the PDF.
- Audio + YouTube tabs still work (legacy, unaffected); legacy `watna-compose.db` intact.

- [ ] **Step 5: Commit**
```bash
git add app feature/books
git commit -m "feat(books): PDF share via FileProvider; project build + tests green"
```

---

## Self-Review

**Spec coverage:**
- §3 module `:feature:books` + app-level NavHost full-screen reader → Tasks 6, 10. ✓
- §4.1 category → Task 1. §4.2 models → Task 2. §4.3 tables/DAOs → Task 3. §4.4 fresh `buddhawajana.db` (drop migration) → Task 3. §4.5 FileDownloader + BookFileStore + disk-derived downloaded-state → Task 5 (+ used in 7/9). ✓
- §5 grid (adaptive, badge, search, states, pull-to-refresh, tap/long-press) → Task 7. ✓
- §6 reader (open flow/state machine, PdfRenderer page-flip+zoom, chrome, thumbnail strip/scrubber/goto, bookmarks, progress, share) → Tasks 8, 9, 11. (Zoom/thumbnail/scrubber/goto/bookmark-sheet are additive UI noted in Task 9 Step 5 on the defined `pager`/`vm`.) ✓
- §7 error handling → Task 7 (Error/Empty), Task 9 (Downloading/Error states), `runCatchingCancellable` in repos. ✓
- §8 testing → TDD tests in Tasks 1,4,5,7,9; smoke checklist Task 11. ✓
- §9 DoD → Task 11 verify. ✓

**Placeholder scan:** Task 9 Step 5 and Task 10 Step 1 note additive UI / optional delete with explicit guidance, not unspecified work; types referenced (`pager`, `vm`, `ready`, `BookFileStore.delete`) are all defined. No "TBD/implement later".

**Type consistency:** `Book(id,title,coverUrl,fileUrl,totalPage,producer,category,orderNumber)` used consistently (Task 1 defines order; Tasks 7/9 test literals call it out to match). `BookListViewModel(repo, downloaded)` consistent Tasks 7/10. `ReaderViewModel(bookId, books, downloader, files, bookmarks, progress, openPdf)` consistent Tasks 9/10. `PdfHandle` (pageCount/renderPage/close) implemented by `PdfDocument` + `FakePdf`. `DownloadProgress` (Progress/Done/Failed) consistent Tasks 5/9. `BookFileStore` (file/exists/delete) consistent Tasks 5/7/11. `UiState` (Loading/Empty/Content/Error) from `:core:ui`.

**Known runtime-only gaps:** reader rendering/zoom/share verified only on-device (Task 11 Step 4); unit tests cover VM/repo/downloader logic with a `PdfHandle` seam so `PdfRenderer` isn't needed in tests.
