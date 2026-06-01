# Material Redesign Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-platform the Buddhawajana Android app onto a modern, multi-module Material 3 foundation (teal theming, adaptive nav shell, cache-first repositories + UiState, coroutines/Flow) while keeping the existing 3 screens building and running at every step.

**Architecture:** Multi-module Gradle (`:core:model`, `:core:common`, `:core:network`, `:core:data`, `:core:designsystem`, `:core:ui`, `:app`) with a `build-logic` convention plugin. Room is the single source of truth; repositories expose `Flow` for cache and a `suspend refresh()` that upserts from Retrofit. UI renders a `UiState<T>` sealed type. RxJava2 is removed.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (BOM), Material3 + `material3-adaptive-navigation-suite`, Navigation-Compose + kotlinx-serialization, Coroutines/Flow, Room (KSP), Retrofit + Moshi, Koin, Coil. Kotlin DSL Gradle (`.kts`) + version catalog.

**Spec:** `docs/superpowers/specs/2026-05-31-material-redesign-foundation-design.md`

**Branch:** `redesign/material-foundation` (already created).

---

## Conventions used by every task

- **Pinned versions** live in `gradle/libs.versions.toml` (Task 1). All later modules reference `libs.*` aliases — never hardcode versions.
- **Package root:** `com.watnapp.buddhawajana` (confirmed from `app/build.gradle` namespace/applicationId). JDK confirmed 21 (≥17 OK).
- **Build check command** (used as the "test" for Gradle-structural tasks): `./gradlew :MODULE:assembleDebug` or `./gradlew build` as noted.
- **Unit test command:** `./gradlew :MODULE:testDebugUnitTest --tests "FQN"`.
- Commit after every task with the exact message shown.

---

## File Structure (what gets created/modified)

```
gradle/libs.versions.toml            (modify) single source of versions
settings.gradle.kts                  (modify) include all modules + build-logic
build.gradle.kts                     (modify) root: plugin aliases apply false
build-logic/                         (create) convention plugins
  settings.gradle.kts
  build.gradle.kts
  src/main/kotlin/
    AndroidLibraryConventionPlugin.kt
    AndroidComposeConventionPlugin.kt
    KotlinLibraryConventionPlugin.kt

core/model/        (create)  pure-Kotlin domain models: Book, Album, Audio
core/common/       (create)  DispatcherProvider, ext fns
core/network/      (create)  Retrofit suspend services, DTOs, Koin networkModule
core/data/         (create)  Room DB+DAOs(Flow), entities, mappers, repositories, dataModule
core/designsystem/ (create)  Color/Type/Shape/Spacing, BuddhawajanaTheme, components
core/ui/           (create)  UiState, BaseViewModel, TopDestination, nav scaffold
app/               (modify)  Application+Koin start, MainActivity, NavHost, adapt 3 screens

app/src/main/java/.../ (existing ui/ vm/ repository/ api/ entity/ — moved into core modules)
```

---

## Task 0: Snapshot current state & confirm package root

**Files:** none (read-only + branch hygiene)

- [ ] **Step 1: Confirm the app package root**

Run: `grep -m1 'namespace' app/build.gradle` and `grep -m1 'applicationId' app/build.gradle`
Expected: prints the namespace/applicationId. Record it. This plan uses `com.watnapp.buddhawajana` as a placeholder — replace with the real value everywhere below.

- [ ] **Step 2: Record the current dependency list and baseline build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (baseline before changes). If it fails, stop and fix the baseline first.

- [ ] **Step 3: Commit a checkpoint marker (no code change)**

```bash
git commit --allow-empty -m "chore: checkpoint before foundation re-platform"
```

---

## Task 1: Version catalog + root build files (Kotlin DSL)

**Files:**
- Modify/Create: `gradle/libs.versions.toml`
- Create: `settings.gradle.kts` (replacing `settings.gradle`)
- Create: `build.gradle.kts` (replacing root `build.gradle`)

- [ ] **Step 1: Write the version catalog**

Create/overwrite `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.9.1"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
coreKtx = "1.13.1"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
material3Adaptive = "1.3.1"
navigationCompose = "2.8.5"
serialization = "1.7.3"
coroutines = "1.9.0"
room = "2.6.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
moshi = "1.15.1"
koin = "3.5.6"
coil = "2.7.0"
junit = "4.13.2"
turbine = "1.2.0"
mockk = "1.13.13"
androidxTestExt = "1.2.1"
espresso = "3.6.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
material3-adaptive-navigation-suite = { module = "androidx.compose.material3:material3-adaptive-navigation-suite", version.ref = "material3Adaptive" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-moshi = { module = "com.squareup.retrofit2:converter-moshi", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
moshi-kotlin = { module = "com.squareup.moshi:moshi-kotlin", version.ref = "moshi" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
junit = { module = "junit:junit", version.ref = "junit" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestExt" }
androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Convert settings to Kotlin DSL**

Delete `settings.gradle` and create `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // ArgPlayer / Pdf-Viewer (used by :app legacy screens)
    }
}
rootProject.name = "Buddhawajana"
include(":app")
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
```

- [ ] **Step 3: Convert root build to Kotlin DSL**

Delete root `build.gradle` and create `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 4: Verify Gradle still configures**

Run: `./gradlew projects`
Expected: BUILD SUCCESSFUL, lists root project `Buddhawajana` and `:app`, `:core:model`, … (subprojects will error on build until their build files exist — that's expected; `projects` only needs settings to parse). If `projects` fails on missing module build files, proceed to Task 2 (which creates them) before re-running.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts build.gradle.kts
git rm settings.gradle build.gradle
git commit -m "build: version catalog + Kotlin DSL root + module includes"
```

---

## Task 2: build-logic convention plugins

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/KotlinLibraryConventionPlugin.kt`
- Create: `build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- Create: `build-logic/src/main/kotlin/AndroidComposeConventionPlugin.kt`

- [ ] **Step 1: build-logic settings**

Create `build-logic/settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
rootProject.name = "build-logic"
```

- [ ] **Step 2: build-logic build file (register plugins)**

Create `build-logic/build.gradle.kts`:

```kotlin
plugins {
    `kotlin-dsl`
}
dependencies {
    compileOnly(libs.plugins.android.library.toDep())
    compileOnly(libs.plugins.kotlin.android.toDep())
    compileOnly(libs.plugins.kotlin.compose.toDep())
}
fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "buddhawajana.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "buddhawajana.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "buddhawajana.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
```

- [ ] **Step 3: Kotlin (pure JVM) library convention**

Create `build-logic/src/main/kotlin/KotlinLibraryConventionPlugin.kt`:

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        val libs = extensions.getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java).named("libs")
        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
        }
    }
}
```

- [ ] **Step 4: Android library convention**

Create `build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt`:

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

        extensions.configure<LibraryExtension> {
            compileSdk = 36
            defaultConfig { minSdk = 24 }
            compileOptions {
                sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
            }
        }
        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
        dependencies {
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("mockk").get())
        }
    }
}
```

> Note: JVM target 17 (was 1.8). AGP 8.9 + Kotlin 2.0 require JDK 17 toolchain. Ensure the Gradle JDK is 17+.

- [ ] **Step 5: Android Compose convention**

Create `build-logic/src/main/kotlin/AndroidComposeConventionPlugin.kt`:

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("buddhawajana.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

        extensions.configure<LibraryExtension> {
            buildFeatures { compose = true }
        }
        dependencies {
            val bom = libs.findLibrary("compose-bom").get()
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.findLibrary("compose-ui").get())
            add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
            add("implementation", libs.findLibrary("compose-material3").get())
            add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
        }
    }
}
```

- [ ] **Step 6: Verify build-logic compiles**

Run: `./gradlew :app:help` (forces build-logic to compile as an included build)
Expected: BUILD SUCCESSFUL (build-logic Kotlin compiles). If plugin classpath errors appear, re-check Step 2's `toDep()` aliases match the catalog plugin ids.

- [ ] **Step 7: Commit**

```bash
git add build-logic
git commit -m "build: add convention plugins (kotlin/android library, compose)"
```

---

## Task 3: `:core:model` (pure-Kotlin domain models)

**Files:**
- Create: `core/model/build.gradle.kts`
- Create: `core/model/src/main/kotlin/com/watna/buddhawajana/core/model/Book.kt`
- Create: `core/model/src/main/kotlin/com/watna/buddhawajana/core/model/Album.kt`
- Create: `core/model/src/main/kotlin/com/watna/buddhawajana/core/model/Audio.kt`

- [ ] **Step 1: Module build file**

Create `core/model/build.gradle.kts`:

```kotlin
plugins { id("buddhawajana.kotlin.library") }
```

- [ ] **Step 2: Domain models**

Create `core/model/src/main/kotlin/com/watna/buddhawajana/core/model/Book.kt`:

```kotlin
package com.watnapp.buddhawajana.core.model

data class Book(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val fileUrl: String?,
    val totalPage: Int?,
    val producer: String?,
    val orderNumber: Int,
)
```

Create `core/model/src/main/kotlin/com/watna/buddhawajana/core/model/Album.kt`:

```kotlin
package com.watnapp.buddhawajana.core.model

data class Album(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val itemCount: Int,
    val position: Int,
)
```

Create `core/model/src/main/kotlin/com/watna/buddhawajana/core/model/Audio.kt`:

```kotlin
package com.watnapp.buddhawajana.core.model

data class Audio(
    val id: String,
    val albumId: String,
    val title: String,
    val url: String,
)
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :core:model:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/model
git commit -m "feat(core:model): pure-Kotlin domain models Book/Album/Audio"
```

---

## Task 4: `:core:common` (dispatchers + Result helper) — TDD

**Files:**
- Create: `core/common/build.gradle.kts`
- Create: `core/common/src/main/kotlin/com/watna/buddhawajana/core/common/DispatcherProvider.kt`
- Test: `core/common/src/test/kotlin/com/watna/buddhawajana/core/common/DispatcherProviderTest.kt`

- [ ] **Step 1: Module build file**

Create `core/common/build.gradle.kts`:

```kotlin
plugins { id("buddhawajana.kotlin.library") }
dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 2: Write the failing test**

Create `core/common/src/test/kotlin/com/watna/buddhawajana/core/common/DispatcherProviderTest.kt`:

```kotlin
package com.watnapp.buddhawajana.core.common

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatcherProviderTest {
    @Test
    fun `default provider exposes standard dispatchers`() {
        val provider = DefaultDispatcherProvider()
        assertEquals(Dispatchers.IO, provider.io)
        assertEquals(Dispatchers.Default, provider.default)
        assertEquals(Dispatchers.Main, provider.main)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :core:common:test --tests "*DispatcherProviderTest*"`
Expected: FAIL — `DefaultDispatcherProvider` / `DispatcherProvider` unresolved.

- [ ] **Step 4: Write minimal implementation**

Create `core/common/src/main/kotlin/com/watna/buddhawajana/core/common/DispatcherProvider.kt`:

```kotlin
package com.watnapp.buddhawajana.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :core:common:test --tests "*DispatcherProviderTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/common
git commit -m "feat(core:common): DispatcherProvider with TDD"
```

---

## Task 5: `:core:network` (Retrofit suspend services + DTOs)

**Files:**
- Create: `core/network/build.gradle.kts`
- Create: `core/network/src/main/AndroidManifest.xml`
- Create: DTOs `BookDto.kt`, `AlbumDto.kt`, `AudioDto.kt`
- Create: services `BookService.kt`, `AudioService.kt`
- Create: `NetworkModule.kt` (Koin)
- Test: `BookServiceContractTest.kt`

> Source of truth for endpoints (from current `api/`): Books `http://etipitaka.org/ebookshop/oauth/` `GET api?token,method`; Audio categories `http://watnapahpong.com/api/category`; audios `http://watnapahpong.com/api/category/{id}/`. Keep these URLs.

- [ ] **Step 1: Module build file**

Create `core/network/build.gradle.kts`:

```kotlin
plugins {
    id("buddhawajana.android.library")
}
android { namespace = "com.watnapp.buddhawajana.core.network" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.android)
}
```

- [ ] **Step 2: Manifest (library, internet not declared here — app declares it)**

Create `core/network/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 3: DTOs**

Create `core/network/src/main/java/com/watna/buddhawajana/core/network/dto/BookDto.kt`:

```kotlin
package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class BookDto(
    val id: String,
    val name: String?,
    @Json(name = "sort_order") val sortOrder: Int?,
    val totalpage: Int?,
    val producer: String?,
    val file: String?,
    val cover: String?,
)
```

Create `AlbumDto.kt`:

```kotlin
package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class AlbumDto(
    val id: String,
    @Json(name = "album_name") val albumName: String?,
    @Json(name = "album_cover") val albumCover: String?,
    val count: Int?,
)
```

Create `AudioDto.kt`:

```kotlin
package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class AudioDto(
    val id: String,
    val name: String?,
    @Json(name = "file_url") val fileUrl: String?,
)
```

- [ ] **Step 4: Suspend services**

Create `core/network/src/main/java/com/watna/buddhawajana/core/network/BookService.kt`:

```kotlin
package com.watnapp.buddhawajana.core.network

import com.watnapp.buddhawajana.core.network.dto.BookDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BookService {
    @GET("api")
    suspend fun getBooks(
        @Query("token") token: String = "",
        @Query("method") method: String = "getitem",
    ): List<BookDto>
}
```

Create `AudioService.kt`:

```kotlin
package com.watnapp.buddhawajana.core.network

import com.watnapp.buddhawajana.core.network.dto.AlbumDto
import com.watnapp.buddhawajana.core.network.dto.AudioDto
import retrofit2.http.GET
import retrofit2.http.Path

interface AudioService {
    @GET("category")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("category/{id}/")
    suspend fun getAudios(@Path("id") albumId: String): List<AudioDto>
}
```

- [ ] **Step 5: Koin network module**

Create `NetworkModule.kt`:

```kotlin
package com.watnapp.buddhawajana.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
    single {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        moshi to client
    }
    single<BookService> {
        val (moshi, client) = get<Pair<Moshi, OkHttpClient>>()
        Retrofit.Builder()
            .baseUrl("http://etipitaka.org/ebookshop/oauth/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BookService::class.java)
    }
    single<AudioService> {
        val (moshi, client) = get<Pair<Moshi, OkHttpClient>>()
        Retrofit.Builder()
            .baseUrl("http://watnapahpong.com/api/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AudioService::class.java)
    }
}
```

- [ ] **Step 6: Contract test (Retrofit parses JSON via MockWebServer-free fake)**

Create `core/network/src/test/java/com/watna/buddhawajana/core/network/BookServiceContractTest.kt`:

```kotlin
package com.watnapp.buddhawajana.core.network

import com.watnapp.buddhawajana.core.network.dto.BookDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class BookServiceContractTest {
    @Test
    fun `book json decodes into BookDto with snake_case mapping`() {
        val json = """[{"id":"1","name":"Test","sort_order":3,"totalpage":120,"producer":"X","file":"f.pdf","cover":"c.png"}]"""
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, BookDto::class.java)
        val list: List<BookDto> = moshi.adapter<List<BookDto>>(type).fromJson(json)!!

        assertEquals("1", list[0].id)
        assertEquals(3, list[0].sortOrder)
        assertEquals(120, list[0].totalpage)
    }
}
```

- [ ] **Step 7: Run the test**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*BookServiceContractTest*"`
Expected: PASS (proves DTO + Moshi mapping correct).

- [ ] **Step 8: Commit**

```bash
git add core/network
git commit -m "feat(core:network): suspend Retrofit services + DTOs + Koin module"
```

---

## Task 6: `:core:data` entities, Room DB (Flow DAOs), mappers

**Files:**
- Create: `core/data/build.gradle.kts`, manifest
- Create: entities `BookEntity.kt`, `AlbumEntity.kt`, `AudioEntity.kt`
- Create: DAOs `BookDao.kt`, `AlbumDao.kt`, `AudioDao.kt`
- Create: `AppDatabase.kt` (preserve `watna-compose.db`, version 2 + existing migration)
- Create: mappers `Mappers.kt`
- Test: `MappersTest.kt`

> The DB name and schema must stay `watna-compose.db` v2 with the existing `Migration1To2` so installed users keep data. Copy the migration SQL verbatim from the current `entity/`/`AppDatabase.kt` (commit 53d6275). Below, the DAOs change from RxJava to Flow/suspend.

- [ ] **Step 1: Module build file**

Create `core/data/build.gradle.kts`:

```kotlin
plugins {
    id("buddhawajana.android.library")
    alias(libs.plugins.ksp)
}
android { namespace = "com.watnapp.buddhawajana.core.data" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.android)
}
```

Create `core/data/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 2: Entities (port from current `entity/`, drop Rx)**

Create `core/data/src/main/java/com/watna/buddhawajana/core/data/db/BookEntity.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book")
data class BookEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: String,
    val title: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "book_url") val bookUrl: String?,
    @ColumnInfo(name = "total_page") val totalPage: Int?,
    val producer: String?,
    @ColumnInfo(name = "order_number") val orderNumber: Int,
)
```

> Port `AlbumEntity` (table `album`, PK `album_id`, fields title/cover_url/item_count/position) and `AudioEntity` (table `audio`, PK `audio_id`, FK `album_id` CASCADE, fields title/url) the same way, matching the existing column names from commit 53d6275 exactly so the migration aligns. Keep the existing indices (`audio_id`, `album_id`).

- [ ] **Step 3: DAOs (Flow + suspend)**

Create `BookDao.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM book ORDER BY order_number")
    fun stream(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<BookEntity>)

    @Query("SELECT COUNT(*) FROM book")
    suspend fun count(): Int
}
```

> Create `AlbumDao` (`stream()` ordered by `position`, `upsertAll`, `count`) and `AudioDao` (`stream(albumId)` filtered by `album_id`, `upsertAll`, `count`) following the same shape.

- [ ] **Step 4: AppDatabase preserving name + migration**

Create `AppDatabase.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookEntity::class, AlbumEntity::class, AudioEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun albumDao(): AlbumDao
    abstract fun audioDao(): AudioDao

    companion object {
        const val NAME = "watna-compose.db"

        // PASTE the exact SQL from the current Migration1To2 (commit 53d6275).
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // <<< copy verbatim from existing AppDatabase migration >>>
            }
        }
    }
}
```

> **Action:** open the current `AppDatabase.kt`, copy the `Migration1To2` body verbatim into `MIGRATION_1_2`. Do not invent SQL.

- [ ] **Step 5: Mappers**

Create `core/data/src/main/java/com/watna/buddhawajana/core/data/mapper/Mappers.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.mapper

import com.watnapp.buddhawajana.core.data.db.AlbumEntity
import com.watnapp.buddhawajana.core.data.db.AudioEntity
import com.watnapp.buddhawajana.core.data.db.BookEntity
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.network.dto.AlbumDto
import com.watnapp.buddhawajana.core.network.dto.AudioDto
import com.watnapp.buddhawajana.core.network.dto.BookDto

fun BookDto.toEntity() = BookEntity(
    bookId = id,
    title = name.orEmpty(),
    coverUrl = cover,
    bookUrl = file,
    totalPage = totalpage,
    producer = producer,
    orderNumber = sortOrder ?: 0,
)

fun BookEntity.toModel() = Book(id = bookId, title = title, coverUrl = coverUrl, fileUrl = bookUrl, totalPage = totalPage, producer = producer, orderNumber = orderNumber)

fun AlbumDto.toEntity() = AlbumEntity(albumId = id, title = albumName.orEmpty(), coverUrl = albumCover, itemCount = count ?: 0, position = 0)
fun AlbumEntity.toModel() = Album(id = albumId, title = title, coverUrl = coverUrl, itemCount = itemCount, position = position)

fun AudioDto.toEntity(albumId: String) = AudioEntity(audioId = id, albumId = albumId, title = name.orEmpty(), url = fileUrl.orEmpty())
fun AudioEntity.toModel() = Audio(id = audioId, albumId = albumId, title = title, url = url)
```

> Adjust `AlbumEntity`/`AudioEntity` constructor params to match the exact fields you ported in Step 2.

- [ ] **Step 6: Mapper test (TDD-style, run after writing)**

Create `core/data/src/test/java/com/watna/buddhawajana/core/data/mapper/MappersTest.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.mapper

import com.watnapp.buddhawajana.core.network.dto.BookDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test
    fun `BookDto maps to entity then model preserving fields`() {
        val dto = BookDto(id = "7", name = "ตถาคต", sortOrder = 2, totalpage = 50, producer = "P", file = "b.pdf", cover = "c.png")
        val model = dto.toEntity().toModel()
        assertEquals("7", model.id)
        assertEquals("ตถาคต", model.title)
        assertEquals(2, model.orderNumber)
        assertEquals("b.pdf", model.fileUrl)
        assertEquals(50, model.totalPage)
    }
}
```

- [ ] **Step 7: Run the test**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MappersTest*"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add core/data
git commit -m "feat(core:data): Room entities/DAOs (Flow) + migration-preserving DB + mappers"
```

---

## Task 7: `:core:data` cache-first repositories — TDD

**Files:**
- Create: `Repository.kt` (interface)
- Create: `BookRepository.kt`, `AlbumRepository.kt`, `AudioRepository.kt`
- Create: `DataModule.kt` (Koin)
- Test: `BookRepositoryTest.kt`

- [ ] **Step 1: Repository interface**

Create `core/data/src/main/java/com/watna/buddhawajana/core/data/repo/Repository.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.repo

import kotlinx.coroutines.flow.Flow

interface Repository<T> {
    fun stream(): Flow<List<T>>
    suspend fun refresh(): Result<Unit>
}
```

- [ ] **Step 2: Write the failing test for BookRepository**

Create `core/data/src/test/java/com/watna/buddhawajana/core/data/repo/BookRepositoryTest.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.repo

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.db.BookDao
import com.watnapp.buddhawajana.core.data.db.BookEntity
import com.watnapp.buddhawajana.core.network.BookService
import com.watnapp.buddhawajana.core.network.dto.BookDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookRepositoryTest {

    private val store = MutableStateFlow<List<BookEntity>>(emptyList())

    private val fakeDao = object : BookDao {
        override fun stream() = store
        override suspend fun upsertAll(books: List<BookEntity>) = store.update { books }
        override suspend fun count() = store.value.size
    }

    @Test
    fun `refresh fetches from service and upserts into dao, stream re-emits`() = runTest {
        val service = object : BookService {
            override suspend fun getBooks(token: String, method: String) =
                listOf(BookDto(id = "1", name = "A", sortOrder = 0, totalpage = 1, producer = null, file = null, cover = null))
        }
        val repo = BookRepository(fakeDao, service)

        repo.stream().test {
            assertEquals(emptyList<Any>(), awaitItem())   // cache empty first
            val result = repo.refresh()
            assertTrue(result.isSuccess)
            assertEquals("A", awaitItem().first().title)   // re-emits after upsert
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh returns failure when service throws`() = runTest {
        val service = object : BookService {
            override suspend fun getBooks(token: String, method: String): List<BookDto> = throw RuntimeException("network")
        }
        val repo = BookRepository(fakeDao, service)
        val result = repo.refresh()
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*BookRepositoryTest*"`
Expected: FAIL — `BookRepository` unresolved.

- [ ] **Step 4: Implement BookRepository**

Create `core/data/src/main/java/com/watna/buddhawajana/core/data/repo/BookRepository.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.BookDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.network.BookService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepository(
    private val dao: BookDao,
    private val service: BookService,
) : Repository<Book> {

    override fun stream(): Flow<List<Book>> = dao.stream().map { list -> list.map { it.toModel() } }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val books = service.getBooks().map { it.toEntity() }
        dao.upsertAll(books)
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*BookRepositoryTest*"`
Expected: PASS (both tests).

- [ ] **Step 6: Implement AlbumRepository and AudioRepository**

Create `AlbumRepository.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.AlbumDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.network.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlbumRepository(
    private val dao: AlbumDao,
    private val service: AudioService,
) : Repository<Album> {
    override fun stream(): Flow<List<Album>> = dao.stream().map { list -> list.map { it.toModel() } }
    override suspend fun refresh(): Result<Unit> = runCatching {
        dao.upsertAll(service.getAlbums().map { it.toEntity() })
    }
}
```

Create `AudioRepository.kt` (audios are per-album, so `refresh` takes an albumId):

```kotlin
package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.AudioDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.network.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AudioRepository(
    private val dao: AudioDao,
    private val service: AudioService,
) {
    fun stream(albumId: String): Flow<List<Audio>> = dao.stream(albumId).map { list -> list.map { it.toModel() } }
    suspend fun refresh(albumId: String): Result<Unit> = runCatching {
        dao.upsertAll(service.getAudios(albumId).map { it.toEntity(albumId) })
    }
}
```

- [ ] **Step 7: Koin data module**

Create `DataModule.kt`:

```kotlin
package com.watnapp.buddhawajana.core.data

import androidx.room.Room
import com.watnapp.buddhawajana.core.common.DefaultDispatcherProvider
import com.watnapp.buddhawajana.core.common.DispatcherProvider
import com.watnapp.buddhawajana.core.data.db.AppDatabase
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.data.repo.AudioRepository
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    single { get<AppDatabase>().bookDao() }
    single { get<AppDatabase>().albumDao() }
    single { get<AppDatabase>().audioDao() }
    single { BookRepository(get(), get()) }
    single { AlbumRepository(get(), get()) }
    single { AudioRepository(get(), get()) }
}
```

- [ ] **Step 8: Run all core:data tests**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add core/data
git commit -m "feat(core:data): cache-first repositories + Koin module (TDD)"
```

---

## Task 8: `:core:designsystem` (theme + components)

**Files:**
- Create: `core/designsystem/build.gradle.kts`, manifest
- Create: `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt`
- Create: components `StateViews.kt`, `BuddhawajanaTopBar.kt`, `CachedAsyncImage.kt`
- Create: font resources (Noto Sans Thai)

- [ ] **Step 1: Module build file**

Create `core/designsystem/build.gradle.kts`:

```kotlin
plugins { id("buddhawajana.android.compose") }
android {
    namespace = "com.watnapp.buddhawajana.core.designsystem"
}
dependencies {
    implementation(libs.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
}
```

Create `core/designsystem/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 2: Add the Thai font**

Download Noto Sans Thai (OFL) regular + medium + bold and place at:
`core/designsystem/src/main/res/font/noto_sans_thai_regular.ttf`, `..._medium.ttf`, `..._bold.ttf`.
(Source: Google Fonts, SIL Open Font License. Commit the .ttf files.)

- [ ] **Step 3: Color schemes (teal seed)**

Create `core/designsystem/src/main/java/com/watna/buddhawajana/core/designsystem/theme/Color.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6FF7F6),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363),
    tertiary = Color(0xFF4B607C),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF4FBFA),
    surface = Color(0xFFF4FBFA),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E3),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF4CDADA),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF00504F),
    onPrimaryContainer = Color(0xFF6FF7F6),
    secondary = Color(0xFFB0CCCB),
    tertiary = Color(0xFFB3C8E8),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0E1514),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDEE4E3),
    surfaceVariant = Color(0xFF3F4948),
)
```

- [ ] **Step 4: Typography with Thai font**

Create `Type.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.watnapp.buddhawajana.core.designsystem.R

val NotoSansThai = FontFamily(
    Font(R.font.noto_sans_thai_regular, FontWeight.Normal),
    Font(R.font.noto_sans_thai_medium, FontWeight.Medium),
    Font(R.font.noto_sans_thai_bold, FontWeight.Bold),
)

// Base M3 typography, retargeted to the Thai family with raised line height for diacritics.
val BuddhawajanaTypography = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(fontFamily = NotoSansThai, lineHeight = 26.sp),
        bodyMedium = bodyMedium.copy(fontFamily = NotoSansThai, lineHeight = 24.sp),
        titleLarge = titleLarge.copy(fontFamily = NotoSansThai),
        titleMedium = titleMedium.copy(fontFamily = NotoSansThai),
        labelLarge = labelLarge.copy(fontFamily = NotoSansThai),
        headlineSmall = headlineSmall.copy(fontFamily = NotoSansThai),
    )
}
```

- [ ] **Step 5: Shape + spacing tokens**

Create `Shape.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BuddhawajanaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)
```

Create `Spacing.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.theme

import androidx.compose.ui.unit.dp

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
}
```

- [ ] **Step 6: Theme composable (light/dark/dynamic)**

Create `Theme.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun BuddhawajanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BuddhawajanaTypography,
        shapes = BuddhawajanaShapes,
        content = content,
    )
}
```

- [ ] **Step 7: State views + top bar + cached image**

Create `core/designsystem/src/main/java/com/watna/buddhawajana/core/designsystem/component/StateViews.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.watnapp.buddhawajana.core.designsystem.theme.Spacing

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyStateView(message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(Spacing.l), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(Spacing.l), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
        Button(onClick = onRetry, modifier = Modifier.padding(top = Spacing.m)) { Text("ลองใหม่") }
    }
}
```

Create `BuddhawajanaTopBar.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddhawajanaTopBar(title: String, onSettingsClick: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
    )
}
```

Create `CachedAsyncImage.kt`:

```kotlin
package com.watnapp.buddhawajana.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun CachedAsyncImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    AsyncImage(model = url, contentDescription = contentDescription, modifier = modifier, contentScale = ContentScale.Crop)
}
```

- [ ] **Step 8: Verify the module builds**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL. (If font resource ids are unresolved, confirm the .ttf filenames match Step 2 exactly.)

- [ ] **Step 9: Commit**

```bash
git add core/designsystem
git commit -m "feat(core:designsystem): teal M3 theme, Noto Sans Thai, state views, components"
```

---

## Task 9: `:core:ui` (UiState, BaseViewModel, nav scaffold) — TDD on UiState helper

**Files:**
- Create: `core/ui/build.gradle.kts`, manifest
- Create: `UiState.kt`, `UiStateExt.kt`
- Create: `BaseViewModel.kt`
- Create: `TopDestination.kt`, `Routes.kt`
- Create: `BuddhawajanaNavSuite.kt`
- Test: `UiStateExtTest.kt`

- [ ] **Step 1: Module build file**

Create `core/ui/build.gradle.kts`:

```kotlin
plugins {
    id("buddhawajana.android.compose")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.watnapp.buddhawajana.core.ui" }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.material3.adaptive.navigation.suite)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.material.icons.extended)
}
```

Create `core/ui/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 2: UiState type**

Create `core/ui/src/main/java/com/watna/buddhawajana/core/ui/state/UiState.kt`:

```kotlin
package com.watnapp.buddhawajana.core.ui.state

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

- [ ] **Step 3: Write the failing test for the list→UiState helper**

Create `core/ui/src/test/java/com/watna/buddhawajana/core/ui/state/UiStateExtTest.kt`:

```kotlin
package com.watnapp.buddhawajana.core.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateExtTest {
    @Test
    fun `non-empty list becomes Content`() {
        val state = listOf("a", "b").toListUiState()
        assertTrue(state is UiState.Content)
        assertEquals(listOf("a", "b"), (state as UiState.Content).data)
    }

    @Test
    fun `empty list becomes Empty`() {
        assertEquals(UiState.Empty, emptyList<String>().toListUiState())
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "*UiStateExtTest*"`
Expected: FAIL — `toListUiState` unresolved.

- [ ] **Step 5: Implement the helper**

Create `core/ui/src/main/java/com/watna/buddhawajana/core/ui/state/UiStateExt.kt`:

```kotlin
package com.watnapp.buddhawajana.core.ui.state

fun <T> List<T>.toListUiState(): UiState<List<T>> =
    if (isEmpty()) UiState.Empty else UiState.Content(this)
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "*UiStateExtTest*"`
Expected: PASS.

- [ ] **Step 7: BaseViewModel**

Create `BaseViewModel.kt`:

```kotlin
package com.watnapp.buddhawajana.core.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

abstract class BaseViewModel : ViewModel() {
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()   // one-shot snackbar events (silent refresh failures)

    protected suspend fun emitMessage(text: String) = _messages.send(text)
}
```

- [ ] **Step 8: Destinations + routes**

Create `TopDestination.kt`:

```kotlin
package com.watnapp.buddhawajana.core.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(val label: String, val icon: ImageVector) {
    AUDIO("หลวงพ่อ", Icons.Default.Headphones),
    BOOKS("หนังสือ", Icons.Default.MenuBook),
    YOUTUBE("ยูทูบ", Icons.Default.PlayCircle),
}
```

Create `Routes.kt`:

```kotlin
package com.watnapp.buddhawajana.core.ui.nav

import kotlinx.serialization.Serializable

@Serializable object AudioGraph
@Serializable object BooksGraph
@Serializable object YoutubeRoute
@Serializable object SettingsRoute
```

- [ ] **Step 9: Adaptive navigation scaffold**

Create `BuddhawajanaNavSuite.kt`:

```kotlin
package com.watnapp.buddhawajana.core.ui.nav

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BuddhawajanaNavSuite(
    selected: TopDestination,
    onSelect: (TopDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopDestination.entries.forEach { dest ->
                item(
                    selected = dest == selected,
                    onClick = { onSelect(dest) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                )
            }
        },
        content = content,
    )
}
```

- [ ] **Step 10: Build the module**

Run: `./gradlew :core:ui:assembleDebug && ./gradlew :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL + tests PASS.

- [ ] **Step 11: Commit**

```bash
git add core/ui
git commit -m "feat(core:ui): UiState (TDD), BaseViewModel, adaptive nav scaffold + routes"
```

---

## Task 10: `:app` — convert to Kotlin DSL, wire Koin + theme + nav shell

**Files:**
- Modify: `app/build.gradle` → `app/build.gradle.kts`
- Create: `app/src/main/java/.../BuddhawajanaApp.kt` (Application)
- Modify: `app/src/main/java/.../MainActivity.kt`
- Create: `app/src/main/java/.../navigation/BuddhawajanaNavHost.kt`
- Modify: `AndroidManifest.xml` (register Application)

- [ ] **Step 1: Convert app build file to Kotlin DSL**

Delete `app/build.gradle`, create `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "com.watnapp.buddhawajana"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.watnapp.buddhawajana"
        minSdk = 24
        targetSdk = 36
        versionCode = 14004
        versionName = "1.4.3"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)

    // Legacy deps still used by the 3 existing screens until they are rebuilt in feature specs:
    // ArgPlayer, Pdf-Viewer, Accompanist, compose-html, etc. Keep their existing coordinates,
    // moved into libs.versions.toml as new aliases, OR temporarily hardcoded here with a TODO
    // to migrate them in the relevant feature spec.

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

> **Action:** carry over the legacy dependencies (`ArgPlayer v3.1.1`, `Pdf-Viewer v1.0.7`, Accompanist, compose-html, KProgressHUD, androidx.window) from the old `app/build.gradle` so the existing screens compile. Add them as catalog aliases. They get removed when their feature is rebuilt.

- [ ] **Step 2: Application class with Koin**

Create `app/src/main/java/com/watna/buddhawajana/BuddhawajanaApp.kt`:

```kotlin
package com.watnapp.buddhawajana

import android.app.Application
import com.watnapp.buddhawajana.core.data.dataModule
import com.watnapp.buddhawajana.core.network.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BuddhawajanaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BuddhawajanaApp)
            modules(networkModule, dataModule)
        }
    }
}
```

- [ ] **Step 3: Register Application + INTERNET permission in manifest**

Modify `app/src/main/AndroidManifest.xml` — set `android:name=".BuddhawajanaApp"` on `<application>` and ensure `<uses-permission android:name="android.permission.INTERNET" />` is present.

- [ ] **Step 4: NavHost wiring the 3 existing screens**

Create `app/src/main/java/com/watna/buddhawajana/navigation/BuddhawajanaNavHost.kt`:

```kotlin
package com.watnapp.buddhawajana.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.watnapp.buddhawajana.core.designsystem.component.BuddhawajanaTopBar
import com.watnapp.buddhawajana.core.ui.nav.BuddhawajanaNavSuite
import com.watnapp.buddhawajana.core.ui.nav.TopDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddhawajanaNavHost() {
    var selected by remember { mutableStateOf(TopDestination.AUDIO) }
    BuddhawajanaNavSuite(selected = selected, onSelect = { selected = it }) {
        Scaffold(topBar = { BuddhawajanaTopBar(title = "พุทธวจน", onSettingsClick = { /* settings route — feature spec */ }) }) { padding ->
            // Existing screens, restyled by BuddhawajanaTheme:
            when (selected) {
                TopDestination.AUDIO -> LegacyAudioScreen(Modifier.padding(padding))
                TopDestination.BOOKS -> LegacyBookScreen(Modifier.padding(padding))
                TopDestination.YOUTUBE -> LegacyYoutubeScreen(Modifier.padding(padding))
            }
        }
    }
}
```

> **Action:** `LegacyAudioScreen`/`LegacyBookScreen`/`LegacyYoutubeScreen` are the current `AudioScreen`/`BookScreen`/`YoutubeScreen` composables, moved into `app` and updated to (a) take a `Modifier`, (b) get their ViewModels via `koinViewModel()`, and (c) use repositories from `:core:data` instead of the old Rx repositories. If a screen still references Rx, wrap its existing logic minimally to compile — full rebuild happens in its feature spec.

- [ ] **Step 5: MainActivity uses theme + nav host**

Modify `MainActivity.kt` to:

```kotlin
package com.watnapp.buddhawajana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.watnapp.buddhawajana.core.designsystem.theme.BuddhawajanaTheme
import com.watnapp.buddhawajana.navigation.BuddhawajanaNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuddhawajanaTheme {
                BuddhawajanaNavHost()
            }
        }
    }
}
```

- [ ] **Step 6: Build and install**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app
git rm app/build.gradle
git commit -m "feat(app): Kotlin DSL, Koin start, teal theme + adaptive nav shell over existing screens"
```

---

## Task 11: Remove RxJava2 and verify whole-project build

**Files:** any remaining references to RxJava across `:app` and moved code.

- [ ] **Step 1: Find remaining Rx usages**

Run: `grep -rn "io.reactivex\|rxjava\|Observable\|Single<\|Flowable\|Disposable" app/src core/*/src`
Expected: ideally no matches. For each match, replace with the coroutine/Flow equivalent (the screen calls `repository.stream()` collected in a `LaunchedEffect`/`collectAsStateWithLifecycle`, and `repository.refresh()` in `viewModelScope.launch`).

- [ ] **Step 2: Remove Rx dependencies from the catalog and any build files**

Confirm `libs.versions.toml` and all `build.gradle.kts` contain no rxjava/rxandroid/rxjava-adapter/room-rxjava2 entries.

- [ ] **Step 3: Full build + all unit tests**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL; all `:core:*` unit tests pass.

- [ ] **Step 4: Manual smoke test (real device/emulator)**

Run: `./gradlew :app:installDebug`, launch the app. Verify:
- App opens on the Audio tab with the teal theme (light + dark by toggling system theme).
- Bottom nav shows 3 destinations; switching tabs works; rail appears in landscape on a tablet/foldable emulator.
- Existing Books list loads (cache-first: shows instantly if previously cached, refreshes from network), Audio albums load, YouTube opens the channel.
- Top-bar Settings gear is present (no-op for now).
- An upgrade install over a v2 database keeps existing downloaded books/audio (install the old build first, then `installDebug` the new one without uninstalling).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove RxJava2; project builds on coroutines/Flow foundation"
```

---

## Task 12: Optional — Roborazzi screenshot tests for design-system parity

**Files:**
- Modify: `gradle/libs.versions.toml` (+ roborazzi)
- Modify: `core/designsystem/build.gradle.kts`
- Test: `core/designsystem/src/test/java/.../ComponentScreenshotTest.kt`

- [ ] **Step 1: Add Roborazzi + Robolectric aliases to the catalog**

Add `roborazzi = "1.26.0"` and `robolectric = "4.13"` to `[versions]`, with libraries `roborazzi`, `roborazzi-compose`, `roborazzi-rule`, `robolectric`. (Verify current versions before pinning.)

- [ ] **Step 2: Enable in designsystem build**

Add the Roborazzi plugin + `testImplementation` deps and `testOptions { unitTests.isIncludeAndroidResources = true }` to `core/designsystem/build.gradle.kts`.

- [ ] **Step 3: Write a screenshot test for ErrorView (light + dark)**

```kotlin
// captures ErrorView under BuddhawajanaTheme in light and dark; baselines committed
```

- [ ] **Step 4: Record baselines**

Run: `./gradlew :core:designsystem:recordRoborazziDebug`
Expected: PNG baselines generated under `core/designsystem/src/test/.../__snapshots__`.

- [ ] **Step 5: Verify compare passes**

Run: `./gradlew :core:designsystem:verifyRoborazziDebug`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/designsystem gradle/libs.versions.toml
git commit -m "test(core:designsystem): Roborazzi screenshot baselines for components"
```

---

## Self-Review

**Spec coverage:**
- D1 foundation-only scope → Tasks 3–11 build foundation; features explicitly deferred (spec §9). ✓
- D2/D3 teal light/dark/dynamic → Task 8 (Color/Theme). ✓
- D4 mirror-iOS nav (3 tabs, top-bar settings, adaptive) → Task 9 (TopDestination, NavSuite) + Task 10 (NavHost, top bar). Favorites/Downloads pinned-in-Audio = deferred to feature specs (spec §9). ✓
- D5 toolchain + Rx→coroutines/Flow → Tasks 1,2 (toolchain), 5–7 (suspend/Flow), 11 (Rx removal). ✓
- D6 multi-module → Tasks 1–10 create the module graph. ✓
- D7 Thai font → Task 8 Steps 2,4. ✓
- Cache-first + UiState (spec §6) → Task 7 (repos, TDD) + Task 9 (UiState, TDD). ✓
- Preserve `watna-compose.db` (spec §6) → Task 6 Step 4 + Task 11 Step 4 upgrade test. ✓
- Done criteria (spec §8) → Task 11 (build + run + data survives + tests). ✓

**Placeholder scan:** The few "Action" notes (copy migration SQL verbatim; port AlbumEntity/AudioEntity exactly; carry legacy deps; convert legacy screens) are deliberate — they reference *existing* code that must be copied unchanged rather than invented. They are not unfilled requirements; each names the exact source. Task 10's `LegacyAudioScreen` etc. are the current screens relocated, not new undefined symbols.

**Type consistency:** `Repository<T>.stream()/refresh()` consistent across Tasks 7. `BookDao.stream()/upsertAll()/count()` identical in Task 6 (definition), Task 7 (fake), Task 9 unaffected. `UiState` variants used consistently. `TopDestination` (AUDIO/BOOKS/YOUTUBE) consistent across Tasks 9–10. `BuddhawajanaTheme`, `BuddhawajanaTopBar`, `BuddhawajanaNavSuite`, `toListUiState` names consistent across definition and use.

**Known follow-ups recorded for feature specs:** AudioRepository is not `Repository<T>` (per-album signature) — intentional; the audio feature spec defines its own state wiring. Legacy screens compile against new repos minimally; full rebuild per feature.
