# YouTube Vertical Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the auto-redirecting legacy YouTube tab with an iOS-parity branded landing screen + explicit "open in YouTube" button, in a new `:feature:youtube` module; retire the legacy `YoutubeScreen`.

**Architecture:** A dataless module — one Compose screen (`YouTubeChannelScreen`) + one `YouTubeLauncher` (app-then-web `ACTION_VIEW`). No ViewModel, Koin, network, or DB. `:app` renders it in the YOUTUBE tab and depends on the module.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3) via the `buddhawajana.android.compose` convention plugin. No tests (pure Android Intent plumbing; device-verified).

---

## File Structure

**New module `:feature:youtube`** (`feature/youtube/`):
- `build.gradle.kts`, `src/main/AndroidManifest.xml`
- `src/main/java/com/watnapp/buddhawajana/feature/youtube/YouTubeChannelScreen.kt`
- `src/main/java/com/watnapp/buddhawajana/feature/youtube/YouTubeLauncher.kt`
- `src/main/res/drawable/real.jpg` (moved from `:app`)
- `src/main/res/values/strings.xml` (`buddhawajana_real`, moved from `:app`)

**Modified:** `settings.gradle.kts`; `app/build.gradle.kts`; `app/.../navigation/HomeScaffold.kt`; `app/.../ui/ContentScreens.kt` (delete `YoutubeScreen` + preview); `app/src/main/res/values/strings.xml` (remove moved string); delete `app/src/main/res/drawable/real.jpg` (moved).

---

## Task YT1: Create the `:feature:youtube` module

**Files:**
- Modify: `settings.gradle.kts`
- Create: `feature/youtube/build.gradle.kts`, `feature/youtube/src/main/AndroidManifest.xml`
- Create: `feature/youtube/src/main/res/values/strings.xml`
- Move: `app/src/main/res/drawable/real.jpg` → `feature/youtube/src/main/res/drawable/real.jpg`
- Create: `YouTubeLauncher.kt`, `YouTubeChannelScreen.kt`

- [ ] **Step 1: Register module** — in `settings.gradle.kts`, after `include(":feature:audio")`:
```kotlin
include(":feature:youtube")
```

- [ ] **Step 2: build.gradle.kts** — `feature/youtube/build.gradle.kts`:
```kotlin
plugins {
    id("buddhawajana.android.compose")
}
android { namespace = "com.watnapp.buddhawajana.feature.youtube" }
dependencies {
}
```
(The convention plugin provides Compose BOM + ui + material3 + foundation; no other deps needed.)

- [ ] **Step 3: AndroidManifest** — `feature/youtube/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 4: Move the channel logo** —
```bash
mkdir -p feature/youtube/src/main/res/drawable
git mv app/src/main/res/drawable/real.jpg feature/youtube/src/main/res/drawable/real.jpg
```

- [ ] **Step 5: Module strings** — `feature/youtube/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="buddhawajana_real">พุทธวจนเรียล</string>
</resources>
```

- [ ] **Step 6: YouTubeLauncher.kt**
```kotlin
package com.watnapp.buddhawajana.feature.youtube

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens the Buddhawajana YouTube channel — the YouTube app if installed, else a browser. */
object YouTubeLauncher {
    const val CHANNEL_URL = "https://www.youtube.com/@BuddhawajanaReal"
    private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    fun open(context: Context) {
        val uri = Uri.parse(CHANNEL_URL)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(YOUTUBE_PACKAGE))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
```

- [ ] **Step 7: YouTubeChannelScreen.kt**
```kotlin
package com.watnapp.buddhawajana.feature.youtube

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Branded landing for the YouTube tab. No auto-redirect — the user taps to leave the app. */
@Composable
fun YouTubeChannelScreen(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.real),
            contentDescription = stringResource(R.string.buddhawajana_real),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(128.dp).clip(CircleShape).border(2.dp, Color.Gray, CircleShape),
        )
        Text(
            text = stringResource(R.string.buddhawajana_real),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Buddhawajana Real",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpen, modifier = Modifier.padding(top = 24.dp)) {
            Text("เปิดใน YouTube")
        }
    }
}
```

- [ ] **Step 8: Verify** — `./gradlew :feature:youtube:assembleDebug -q` → BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add settings.gradle.kts feature/youtube app/src/main/res/drawable/real.jpg
git commit -m "feat(feature:youtube): channel landing screen + launcher module

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```
(The `git mv` stages both the add and the delete of `real.jpg`.)

---

## Task YT2: Wire into `:app` + retire legacy YoutubeScreen

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/watnapp/buddhawajana/navigation/HomeScaffold.kt`
- Modify: `app/src/main/java/com/watnapp/buddhawajana/ui/ContentScreens.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: app dep** — in `app/build.gradle.kts` `dependencies {}`, after `implementation(project(":feature:audio"))`:
```kotlin
    implementation(project(":feature:youtube"))
```

- [ ] **Step 2: HomeScaffold — render the new screen.**
In `HomeScaffold.kt`:
1. Add imports:
```kotlin
import androidx.compose.ui.platform.LocalContext
import com.watnapp.buddhawajana.feature.youtube.YouTubeChannelScreen
import com.watnapp.buddhawajana.feature.youtube.YouTubeLauncher
```
2. Remove the import `import com.watnapp.buddhawajana.ui.YoutubeScreen`.
3. Inside `HomeScaffold`, after `val controller: PlaybackController = koinInject()`, add:
```kotlin
    val context = LocalContext.current
```
4. Change the YOUTUBE branch in the `when (selected)` from `TopDestination.YOUTUBE -> YoutubeScreen()` to:
```kotlin
                        TopDestination.YOUTUBE -> YouTubeChannelScreen(
                            onOpen = { YouTubeLauncher.open(context) },
                        )
```

- [ ] **Step 3: Delete legacy YoutubeScreen.**
In `app/src/main/java/com/watnapp/buddhawajana/ui/ContentScreens.kt`, delete the `@Composable fun YoutubeScreen() { ... }` function (≈ lines 54–112, including its `@Composable` annotation) AND the `@Composable fun YoutubeScreenPreview() { YoutubeScreen() }` function (≈ lines 115–118, with its `@Preview`/`@Composable` annotations). Leave `BooksScreen`/`BooksScreenPreview` and everything else intact. Do not worry about now-unused imports (Kotlin treats unused imports as warnings, not errors).

- [ ] **Step 4: Remove the moved string from `:app`.**
In `app/src/main/res/values/strings.xml`, delete the line:
```xml
    <string name="buddhawajana_real">พุทธวจนเรียล</string>
```
(It now lives in `:feature:youtube`.)

- [ ] **Step 5: Verify build + tests** —
`./gradlew :app:assembleDebug testDebugUnitTest -q` → BUILD SUCCESSFUL.
(If `:app:assembleDebug` fails with an unresolved `R.drawable.real` or `R.string.buddhawajana_real`, it means a legacy reference remains in `:app` — grep `grep -rn "drawable.real\|buddhawajana_real" app/src/main` and remove it.)

- [ ] **Step 6: Commit**
```bash
git add app
git commit -m "feat(app): YouTube tab uses :feature:youtube landing; retire legacy YoutubeScreen

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 7: Device smoke (manual)**
1. Open the YouTube tab → branded landing shows; **no auto-redirect**.
2. Tap "เปิดใน YouTube" → YouTube app opens the @BuddhawajanaReal channel.
3. Device without the YouTube app → opens the channel in a browser.
4. Back returns to the app (still on the YouTube tab).
5. Phone (bottom bar) + tablet (rail) both render the landing.

---

## Self-Review (author checklist — completed)

- **Spec coverage:** landing screen + button (YT1 Step 7), no auto-redirect (no `LaunchedEffect`), app-then-web launcher (YT1 Step 6), new module (YT1), HomeScaffold wiring (YT2 Step 2), retire legacy + move asset/string (YT1 Step 4–5, YT2 Step 3–4). ✓
- **Placeholder scan:** none; full code + commands throughout. ✓
- **Type consistency:** `YouTubeChannelScreen(onOpen: () -> Unit, modifier)` and `YouTubeLauncher.open(context)` referenced identically in YT2. `R.drawable.real` + `R.string.buddhawajana_real` resolve from the module's own R (assets moved in YT1). No project deps added to the module (convention plugin supplies Compose). ✓
```
