---
type: Plan
title: Per-platform optimization plan
description: How the one KS Expense Manager build, the Android phone app, is made the best it can be on Android, with an audit of the build today and what changes, keeping dependencies to the minimum.
tags: [plan, platforms, android, performance, accessibility, privacy]
status: proposed
---

# Per-platform optimization plan

**Status: proposed** (2026-11-25), waiting for the owner's approval. Nothing here is built before that.

The suite rule ([Platform folders](https://github.com/kumarsumit17/KS-Suite/blob/main/specs/platform-folders.md), "Each platform build is optimized for its OS") says each platform build is its own app, tuned to its OS. KS Expense Manager has **one target, the Android phone** (`mobile/android/`). The other four folders stay empty: a TV interface is unusable for typing amounts, and macOS, Windows and iOS are not targets (adding one is the owner's decision and would start its own section here). So this plan is about one build, made as good as it can be on Android phones. It sits next to the feature roadmap in the [Build plan](plan.md), which it does not change.

## 1. Audit: the Android phone build today (stage 1, 2026-11-25)

| Area | Today |
|---|---|
| Entry point | `MainActivity` (AppCompat, for the per-app language on Android 8–12), edge-to-edge, launcher icon only. No app shortcuts. |
| Look | Material 3, the phone's own colours (dynamic colour) on Android 12+, green before; dark theme follows the system; adaptive icon with a monochrome layer (themed icons). No splash-screen customisation (Android 12+ shows the icon on its own). |
| Input | Touch. Quick add in three taps with the decimal keyboard opening on the amount; Save sits in the sheet's title row so the keyboard never covers it. Back closes the sheet and Settings (`BackHandler`). Predictive back: on by default for apps targeting API 36+, not checked by hand. No haptics. |
| Performance | R8 and resource shrinking on `release`. Room opens on first use, off the main thread; the starting categories are written when the database is created. **Cold start not measured** (the shared emulators were overloaded). No baseline profile; Compose's own profiles come through `profileinstaller`. |
| Stability | 18 unit tests, 13 device tests. No StrictMode in debug builds, so accidental disk work on the main thread would go unnoticed. |
| Storage and privacy | Room database in app storage, not encrypted (the phone's own lock and file encryption protect it); `allowBackup="false"` and data-extraction rules exclude everything; no `INTERNET` permission (build check). **The app's screen shows in Recents** (amounts visible in the app switcher). |
| Packaging | Release APK **2.1 MB** (`classes.dex` 2.8 MB uncompressed, `resources.arsc` 0.44 MB). `resources.arsc` carries the strings of AppCompat and Material in every language they ship, though the app has only English and Hindi. One native library comes with Compose (`libandroidx.graphics.path.so`, about 11 KB per ABI). AAB from the release workflow. |
| Dependencies | AndroidX core-ktx, activity-compose, appcompat, Compose (ui, foundation, material3, material-icons-core), Room with KSP, ks-utility. AppCompat is there only for the per-app language below Android 13. |
| OS integrations | Per-app language (`locales_config.xml`, AppCompat stores the choice); file export and import through the Storage Access Framework. No share target yet (planned in stage 3 for ks-tv-launcher's subscriptions). |
| Accessibility | Labels on the icon buttons (+, Settings, month arrows, Back) and on each category switch. Checked by hand at 130 % font size in light and dark. **Not checked with TalkBack**; the emoji icons are read out next to the category name ("curry rice, Food"); the category bars are separate nodes, not one sentence; 200 % font size not checked. |
| Version | One tag `vX.Y.Z` builds the one APK and AAB. |

## 2. The plan for Android (`mobile/android/`)

No new runtime dependency is needed for anything below. Items that would need one are only in section 3.

### 2.1 Look and input

- **Launcher shortcut "Add expense"** (static `res/xml/shortcuts.xml`, no library): a long press on the icon opens the add sheet straight away, so adding takes two taps from the home screen. Uses the existing language keys.
- **Predictive back**: check the back-to-home and sheet-closing animations on Android 16 (they are on by default for this target); fix anything that animates wrongly.
- **Haptic confirmation** on Save and Delete (`HapticFeedbackType.Confirm` / `Reject`, part of Compose).
- Landscape and split-screen checked: the month list and the sheet scroll and nothing is cut off. A phone app stays a single column; no tablet layout.

### 2.2 Performance and startup

- **Measure cold start** (first start after install, and a normal cold start) with `am start -W` on the phone emulator when it is not shared, and on a real phone by the owner. Target: first frame under 500 ms on the emulator, under 800 ms on a mid-range phone.
- **Open the database early**: start opening Room on a background thread in `ExpenseApp.onCreate`, so the month's data is ready when the first frame is drawn.
- **Baseline profile only if the target is missed**: it needs the `androidx.baselineprofile` Gradle plugin and a macrobenchmark test module (build-time only, nothing extra ships). Section 3, decision 3.
- **StrictMode in debug builds** (disk and network on the main thread, leaked closeables), logged, never in release.

### 2.3 Packaging and size

- **Keep only English and Hindi resources**: `androidResources { localeFilters += listOf("en", "hi") }` drops the other languages that AppCompat and Material bring. Expected to shrink `resources.arsc` by most of its 0.44 MB.
- Report the APK and AAB size in each release's notes; check the R8 keep rules stay empty (Room and Compose bring their own).

### 2.4 Privacy and OS integrations

- **Hide the app's content in Recents**: `setRecentsScreenshotEnabled(false)` on Android 13+ (and the same effect on 8–12 with a blank view while the activity is in the background). Screenshots taken by the user keep working; only the app-switcher thumbnail is hidden. Section 3, decision 2.
- **Share target** for ks-tv-launcher's subscription CSV: as already planned in stage 3 of the [Build plan](plan.md).
- No widget, no Quick Settings tile, no notifications: each needs either a library (Glance) or a new screen or permission, and the launcher shortcut covers the fast path.

### 2.5 Accessibility

- **TalkBack pass** on every screen: the emoji icon is decoration (cleared from semantics where the name is shown), each category bar reads as one sentence ("Food, ₹450, 80 percent of this month"), each day header reads its date and total, the amount field announces its error.
- **200 % font size** and display size at maximum: nothing clipped, chips wrap, the total wraps rather than shrinking.
- Touch targets at least 48 dp (Material 3 defaults; checked on the chips and switches) and contrast of the bar colours in both themes.
- Interface text for any new label in English and Hindi (the language-file check).

### 2.6 Dependencies

- **AppCompat**: used only for the per-app language on Android 8–12. Section 3, decision 1 asks whether to keep it.
- Nothing else is added. Charts, view-models, navigation, DataStore (until stage 2 needs it) stay out, as in the [Build plan](plan.md).

## 3. Decisions for the owner

1. **AppCompat** (its size after R8 is measured in step 1):
   - **(a) Keep it** (recommended): it gives the suite's language rule on Android 8–12 with no code of our own; R8 removes most of it.
   - (b) Replace it with the platform's `LocaleManager` on Android 13+ and a small locale wrapper of our own for 8–12 (`attachBaseContext` plus a saved choice): one dependency less, more code to keep.
   - (c) Raise `minSdk` to 33 (Android 13) and drop it: simplest code, but phones on Android 8–12 can no longer install the app.
2. **Hide the app in Recents** (2.4): yes (recommended), or keep the thumbnail.
3. **Baseline profile** (2.2): add it only if cold start misses the target (recommended), or add it now.
4. **Version tags**: keep plain `vX.Y.Z` (recommended; there is only one platform). If the suite moves every app to per-platform tags (as ks-jaiplayer did with `android-vX.Y.Z`), this app follows in the same change as the shared workflow.

## 4. Build order and testing

Each step is one or more commits on `dev`, tested before the next. Nothing is pushed or tagged without the owner's OK.

| Step | What | Tested on |
|---|---|---|
| 1 | Measure cold start and size; locale filter; early database open; StrictMode in debug | Phone emulator (not shared at the time); `am start -W` before and after |
| 2 | Launcher shortcut, haptics, predictive back and landscape checks, hide in Recents | Phone emulator; Compose test for the shortcut's intent |
| 3 | Accessibility pass (TalkBack, 200 % font, contrast) | Phone emulator with TalkBack; Accessibility Scanner by hand |
| 4 | Decision 1 (b or c), or the baseline profile, if chosen | Phone emulator; unit and device tests; language switch in both directions |

A real phone check by the owner follows each release ([Release cycle](https://github.com/kumarsumit17/KS-Suite/blob/main/specs/release-cycle.md), step 5).
