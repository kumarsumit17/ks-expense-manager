# 2026-11-27

* **Update**: `android-v0.5.2` was never released: ks-utility `v0.18.2` came out between the rehearsal and the tag, and the tag run's ks-suite-check refused the older pin. The bump workflow moved the pin to `v0.18.2` by itself; the release is `android-v0.5.3`.
* **Update**: `ks-utility` moved to `v0.18.1` (a bump-workflow fix).
* **Update**: `ks-utility` moved to `v0.18.0`; `release.yml` and `ks-utility-bump.yml` call ks-utility's shared workflows at `@main` (owner's decision: `@main` always equals the newest release, so only the code pin moves). No `KS_WORKFLOWS_TOKEN`.
* **Update**: `ks-utility` moved from `v0.16.0` to `v0.17.0`, with the shared-workflow calls (by hand: the bump workflow's commit step failed on a missing `*.yaml` glob, reported to ks-utility).
* **Update**: Release workflow at `kotlin-app-release.yml@v0.16.0`, which runs ks-suite-check before building; `.github/workflows/ks-utility-bump.yml` moves the pin to each new ks-utility tag by itself (tests first). `ks-suite-check.json` declares the one allowed exception, `no-updater`: the app is offline by design ([App updates](app-updates.md)).
* **Creation**: [Google Play](google-play.md): the `play` build type for the AAB (no releases link), store listing in English and Hindi with icon, feature graphic and screenshots, `PRIVACY.md`, and the Play Console answers and owner steps. Release workflow on `kotlin-app-release.yml@v0.14.0` with `play-build-type: play`.

# 2026-11-26

* **Update**: `ks-utility` moved from `v0.13.2` to `v0.14.0` (shared Tauri release workflows; no change to the Kotlin code or the language files).
* **Update**: Stage 3: recurring expenses (schema 3 with `MIGRATION_2_3`; a Repeat chip in the add sheet; a Recurring expenses section in Settings; due ones added when the app opens) and the share target for ks-tv-launcher's subscription CSV; backup format version 4. [Build plan](plan.md) "Stage 3 design".
* **Update**: `CLAUDE.md`: the "build, test, release, no waiting" rule and the newest-ks-utility-pin rule (owner's OK).
* **Update**: `ks-utility` moved from `v0.12.0` to `v0.13.2` (suite rule: the newest tag before each release). No network code in its Kotlin part; language check passes.
* **Update**: [Build plan](plan.md): releases follow the suite rule "Build, test, release, no waiting": tested work is released with the next tag without asking; hand checks never block a release.
* **Update**: `ks-utility` moved from `v0.10.0` to `v0.12.0` (the owner's approved Hindi terms). Hindi texts follow them: राशि (amount), यातायात, खरीदारी, स्वास्थ्य, "बजट से ज़्यादा". No network code in ks-utility's Kotlin part.
* **Update**: Stage 2, second part: other currencies. Default currency in Settings (SharedPreferences, no new library; DataStore dropped from the plan), a Currency chip in the add sheet, totals per currency with the default first, backup format version 3 with the setting. [Build plan](plan.md) "Stage 2 design".
* **Update**: Stage 2, first part: monthly budgets (overall and per category; schema 2 with `MIGRATION_1_2`) and search with category and date filters across all months; backup format version 2 with budgets. [Build plan](plan.md) "Stage 2 design". Other currencies still to come.
* **Update**: The repository moved from the GitHub account `kumarsumit17` to `kumasumit`: release workflow (`kumasumit/ks-utility/...@v0.10.0`), submodule URL, the App updates link and every doc link changed. The old address redirects, so copies of v0.1.0 still reach the releases.

# 2026-11-25

* **Update**: Platform tags (suite rule, 2026-11-25): releases are tagged `android-vX.Y.Z` from the release after v0.1.1. The release workflow calls `kotlin-app-release.yml@v0.10.0`, takes `*-v*.*.*` tags and a rehearsal `platform` input. [App updates](app-updates.md): **Get the new version** opens the releases list, because Android releases are not marked latest; v0.1.1 is a plain-tag bridge release so copies of v0.1.0 reach it. [Per-platform plan](plan-per-platform.md) decision 4 and the [Build plan](plan.md) roadmap updated.
* **Update**: `ks-utility` moved from `v0.9.0` to `v0.10.0` (platform tags in the shared release workflow and the update helpers). Its Kotlin code still has no network code, so no `INTERNET` permission comes with it.
* **Update**: [Per-platform optimization plan](plan-per-platform.md) steps 1–3 built: English and Hindi locale filter (APK 2.14 → 1.81 MB), early database open, StrictMode in debug, the "Add expense" launcher shortcut, haptics, Recents hidden on Android 13+ (cover on 8–12), accessibility changes. Results and what is not settled (cold start) are in its section 4. New language key `month.categoryShare`.
* **Update**: [Per-platform optimization plan](plan-per-platform.md) approved: keep AppCompat, hide in Recents, baseline profile only if cold start is slow, plain `vX.Y.Z` tags.
* **Creation**: [Per-platform optimization plan](plan-per-platform.md) added as a proposal, for the suite rule that each platform build is optimized for its OS: an audit of the Android phone build and the changes (launcher shortcut, cold start, locale filter, hide in Recents, accessibility pass), with four owner decisions.
* **Update**: `ks-utility` moved from `v0.6.0` to `v0.9.0` (more languages, the translation tools; [ks-utility translation tools](https://github.com/kumasumit/ks-utility/blob/main/specs/translating.md)).

# 2026-11-24

* **Update**: Stage 1 built on `dev`: Gradle build in `mobile/android/` (`:mobile:android`, `com.ks.expense`), quick add, categories, month view with totals and category bars, edit and delete, CSV export, JSON backup and restore, English and Hindi, Settings with App updates. Room schema 1 exported to `mobile/android/schemas/`. [Build plan](plan.md): Save moved to the sheet's title row; DataStore and lifecycle libraries left for later stages; kotlinx-serialization-core raised to 1.8.1 by a constraint (room-testing needs it). New dependencies, all Apache 2.0: AndroidX core-ktx, activity-compose, appcompat, Compose (ui, foundation, material3, material-icons-core), Room 2.8.5 with KSP; tests: JUnit 4 (EPL 1.0), AndroidX Test, room-testing, compose ui-test.
* **Update**: Release workflow calls `kotlin-app-release.yml@v0.6.0` with `android-module: ":mobile:android"` and no `tv-module`; its tests run the unit tests, the language-file check and the no-`INTERNET` check.
* **Update**: [Build plan](plan.md) approved by the owner: offline updates link only, Android's formatter for money, JSON backup next to the CSV export, no Vico, stage 1 as planned. [Constraints](constraints.md): the JSON file is the backup; schemas in `mobile/android/schemas/`. [Overview](overview.md): Vico dropped.
* **Creation**: [Build plan](plan.md) added as a proposal: features, money, storage with backup and restore, languages, libraries with licences, staged roadmap and five owner decisions.

# 2026-11-23

* **Update**: Release: `.github/workflows/release.yml` now calls ks-utility's shared release workflow (`kotlin-app-release.yml@v0.4.0`, [spec](https://github.com/kumasumit/ks-utility/blob/main/specs/release-workflow.md)): one workflow for every Kotlin app, with a rehearsal input, pinned actions, signature checks, build-provenance attestations and `gh` publishing.
* **Creation**: [App updates](app-updates.md) added: the App updates section shows the version and a Get the new version link to the GitHub releases page; no update check or download, because of the offline constraint.
* **Update**: `ks-utility` moved to `v0.3.0`.

# 2026-11-21

* **Update**: [Dependencies](dependencies.md): `ks-utility` added as a git submodule for shared generic utilities.
* **Initialization**: `specs/` created as an Open Knowledge Format (OKF) v0.2 bundle with this log and `index.md`.
* **Creation**: [Overview](overview.md), [Constraints](constraints.md) added as drafts, from `README.md` and `CLAUDE.md`.
* **Creation**: [Dependencies](dependencies.md) added: third-party dependencies are kept to the minimum.
