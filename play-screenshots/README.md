# Google Play Store Screenshots — พุทธวจน (v2.0.0 / build 14005)

Captured from `app-debug.apk` (current `main`) on Android emulators. Status bar cleaned
via SystemUI demo mode (12:00, full battery/signal, no notification clutter).

## Folders

| Folder | Play device type | Dimensions | Notes |
|--------|------------------|-----------|-------|
| `phone/`  | Phone        | 1080×1920 (9:16) | 6 shots, portrait |
| `tab7/`   | 7-inch tablet | portrait 1200×1800, landscape 1920×1080 | 6 portrait (`p-*`) + 6 landscape (`l-*`); system taskbar cropped off bottom |
| `tab10/`  | 10-inch tablet | portrait 1600×2560, landscape 2560×1600 | 6 portrait (`p-*`) + 6 landscape (`l-*`) |

Shots per flow: `01 home → 02 audio album/tracks → 03 player → 04 books → 05 PDF reader → 06 YouTube`.

## Play Console upload notes

- **One screenshot set per device type** (Phone, 7", 10"), **max 8 each**. The tablet
  folders contain both orientations — pick up to 8 per type (e.g. all portrait + 2 landscape,
  or any mix). Play allows mixing portrait and landscape in the same set.
- All files: 24-bit PNG, no alpha, < 1 MB each (Play limit 8 MB). Each side 320–3840 px,
  longer side ≤ 2× shorter — all compliant.

## Issue found while capturing — FIXED

- **Audio player had no transport controls in landscape** (affected all landscape, not just
  10"): the square album art was sized by width and overflowed the short landscape height in a
  non-scrolling Column, pushing the controls off-screen. Fixed in `PlayerScreen.kt` with an
  adaptive layout (`BoxWithConstraints` → landscape = art-left / scrollable-controls-right;
  portrait unchanged). The `*/l-03-player.png` landscape player shots were re-captured after
  the fix and now show full controls.

## Reproduce

AVDs created for this: `PlayPhone` (pixel, 1080×1920), `PlayTab7` (Nexus 7 2013, 1200×1920).
`Pixel_Tablet` (2560×1600) used for 10". Demo mode: `adb shell am broadcast -a com.android.systemui.demo ...`.
