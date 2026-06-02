# YouTube Vertical — Design

Date: 2026-06-02
Status: Approved (brainstorming)
Branch: `feature/youtube-vertical`

## Goal

Replace the legacy YouTube tab (which auto-redirects out of the app on every tab tap) with an
iOS-parity branded landing screen + an explicit "open in YouTube" button, housed in a new
`:feature:youtube` module so the legacy `ContentScreens.kt` `YoutubeScreen` can be retired.

## Context

YouTube has **no data layer** on either platform — no API, no DB, no in-app playback. Both iOS
and the current Android app are pure external launchers to the channel `@BuddhawajanaReal`.

- iOS: a landing screen (channel logo + name + "Open in YouTube" button) that deep-links
  (`youtube://…`) with a web fallback (`https://…`). No auto-redirect.
- Current Android (`app/.../ui/ContentScreens.kt` `YoutubeScreen`): a branded screen that
  **auto-opens** the channel via `Intent.ACTION_VIEW(https://www.youtube.com/@BuddhawajanaReal)`
  in a `LaunchedEffect` — so the user can never sit on the tab. Web-only, in the legacy `:app`.

## Scope decisions (locked in brainstorming)

- **Landing screen + explicit button** (no auto-redirect). Matches iOS.
- **New `:feature:youtube` module** (consistency with `:feature:audio`/`:feature:books`; retires
  the legacy `YoutubeScreen`).
- **App-then-web launch**: prefer the installed YouTube app, fall back to a browser.

Out of scope: video/playlist lists, in-app playback, search, favorites/downloads — neither iOS
nor the backend exposes YouTube data.

## Architecture

A tiny, dataless module: one composable screen + one launcher helper. No ViewModel, no Koin
binding, no network/DB.

### Module deltas

| Module | Change |
|--------|--------|
| `:feature:youtube` (**new**) | `YouTubeChannelScreen`, `YouTubeLauncher`; channel logo + strings. No project deps beyond the `buddhawajana.android.compose` convention plugin (plain Compose UI). |
| `:app` | HomeScaffold YOUTUBE branch → `YouTubeChannelScreen`; depend on `:feature:youtube`; remove legacy `YoutubeScreen` + its `real` asset/string usage |
| `settings.gradle.kts` | `include(":feature:youtube")` |

## Components

**Module setup** — `feature/youtube/build.gradle.kts` (plugin `buddhawajana.android.compose`,
namespace `com.watnapp.buddhawajana.feature.youtube`), minimal `AndroidManifest.xml`. No project
deps needed (plain Compose UI from the convention plugin; the logo is a local drawable). Move
`app/src/main/res/drawable/real.jpg` → `feature/youtube/src/main/res/drawable/` and the
`buddhawajana_real` string → the module's `res/values/strings.xml`.

**`YouTubeChannelScreen(onOpen: () -> Unit, modifier: Modifier = Modifier)`** — centered column:
circular channel logo (`painterResource(R.drawable.real)`, ~128dp, gray border), title
`พุทธวจนเรียล` (large bold), caption "Buddhawajana Real", and a `Button` labelled
**"เปิดใน YouTube"** → `onOpen`. No `LaunchedEffect`, no auto navigation.

**`YouTubeLauncher`** —
```kotlin
object YouTubeLauncher {
    const val CHANNEL_URL = "https://www.youtube.com/@BuddhawajanaReal"
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    fun open(context: Context) {
        val uri = Uri.parse(CHANNEL_URL)
        val appIntent = Intent(Intent.ACTION_VIEW, uri).setPackage(YOUTUBE_PACKAGE)
        try {
            context.startActivity(appIntent)            // YouTube app if installed
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri)) // browser fallback
        }
    }
}
```
(`ACTION_VIEW` on the https URL with the YouTube package forces the app; the catch handles the
app-not-installed case. Equivalent to iOS deep-link→web.)

**HomeScaffold wiring** — replace `TopDestination.YOUTUBE -> YoutubeScreen()` with:
```kotlin
TopDestination.YOUTUBE -> YouTubeChannelScreen(
    onOpen = { YouTubeLauncher.open(context) },
)
```
(`context = LocalContext.current` in HomeScaffold.) `:app` `build.gradle.kts` adds
`implementation(project(":feature:youtube"))`.

**Retire legacy** — delete `YoutubeScreen` from `app/.../ui/ContentScreens.kt` (and its now-unused
`real` drawable + `buddhawajana_real` string in `:app`, moved to the module). Remove the
`com.watnapp.buddhawajana.ui.YoutubeScreen` import in HomeScaffold. Leave the rest of
`ContentScreens.kt` (Books/Audio previews) untouched.

## Error handling

`YouTubeLauncher.open` catches `ActivityNotFoundException` (YouTube app absent) and retries with a
plain web intent. If even that fails (no browser at all — practically impossible), the exception
propagates; acceptable for this scope.

## Testing

No data/VM ⇒ no JVM unit test of value (the launcher is pure Android `Intent` plumbing). Verify on
device:
1. Select the YouTube tab → branded landing shows, **no auto-redirect**.
2. Tap "เปิดใน YouTube" → opens the YouTube app on the @BuddhawajanaReal channel.
3. On a device without the YouTube app → opens the channel in a browser.
4. Back returns to the app (tab still on YouTube).
5. Phone (bottom bar) + tablet (rail) both show the landing correctly.

## Device smoke checklist

- YouTube tab no longer yanks you out on selection.
- Button opens YouTube app; browser fallback when app absent.
- Logo + Thai title render.
