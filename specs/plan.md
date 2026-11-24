---
type: Plan
title: Build plan
description: What KS Expense Manager does, how it stores and formats money, the libraries it uses, and the staged roadmap from a minimal first version.
tags: [plan, roadmap, features, storage, money, languages, dependencies]
status: proposed
---

# Build plan

A small, private Android phone app for tracking personal expenses. Everything stays on the phone. It keeps every rule in [Constraints](constraints.md) and [Dependencies](dependencies.md). Status: **proposed**, waiting for the owner's approval; no app code is written before that.

## Features

| Feature | What the user gets | Stage |
|---|---|---|
| Quick add | Open the app, tap **+**, type the amount, tap a category, tap **Save**. Today's date, the default currency and an empty note are filled in. Three taps from launch. | 1 |
| Categories | A starting set (Food, Transport, Shopping, Bills, Health, Entertainment, Other) with an icon each. The user can add, rename and hide categories. A hidden category keeps its expenses. | 1 |
| Monthly view and totals | The home screen is one month: its total, a bar per category (share of the month), and the expenses grouped by day, newest first. Swipe or arrows for other months. Tap an expense to edit or delete it. | 1 |
| Export to CSV | Writes the expenses to a CSV file the user chooses (Storage Access Framework), for a spreadsheet. | 1 |
| Backup and restore | **Back up** writes everything (expenses, categories, budgets, recurring rules, settings) to one file the user chooses. **Restore** reads it back after a clear warning, replacing what is on the phone. | 1 |
| English and Hindi | All text in both languages from the start. | 1 |
| Budgets | An optional monthly limit, overall and per category. The month view shows spent and left, and turns the bar amber at 80 % and red over 100 %. No notifications. | 2 |
| Search and filters | A search field on the month view (note text and amount) and filter chips (category, date range). It finds across all months. | 2 |
| Other currencies | Pick the default currency in Settings and, optionally, another currency for one expense. Totals are shown per currency; there is no conversion (no exchange rates offline). | 2 |
| Recurring expenses | A rule (amount, category, note, monthly or yearly or weekly, next date). When the app opens, due ones are added and shown in a short list ("3 added"). No background work. | 3 |
| Subscriptions from ks-tv-launcher | The user shares ks-tv-launcher's subscription CSV to this app; it shows the rows and creates recurring rules for the ones the user ticks ([contract](https://github.com/kumarsumit17/KS-Suite/blob/main/specs/cross-repo.md): `name,amount_minor,currency,cycle,next_due`). | 3 |

Not planned: income tracking, bank sync, cloud sync, accounts, receipts or photos, notifications, widgets, charts beyond the category bars. Each would be a new decision.

## Screens

Three screens, no navigation library (one state value picks the screen):

1. **Month** (home): total, category bars, the day-grouped list, the **+** button; in stage 2 also budgets and search.
2. **Add or edit**: a bottom sheet over Month, with the amount field focused and the number keyboard open, category chips, date, note, Save, and Delete when editing.
3. **Settings**: language, default currency, categories, export, back up, restore, App updates, and in stage 3 recurring rules.

## Money

- Amounts are a `Long` in minor units (paise, cents) plus an ISO 4217 code, as [Constraints](constraints.md) require. Typed text is parsed straight to minor units with `BigDecimal`, never `Double`. Sums are `Long` sums in SQL per currency.
- **Rupees by default**: the default currency is `INR` on first start (whatever the phone's region; the user can change it in stage 2).
- **Indian digit grouping**: amounts are shown with `NumberFormat.getCurrencyInstance` for `en-IN` (or `hi-IN` in Hindi) whenever the currency is `INR`: `₹1,23,456.50`. Other currencies use the app language with the phone's region. Latin digits always, as the suite's [language rule](https://github.com/kumarsumit17/ks-utility/blob/main/specs/languages.md#formatting) says.
- ks-utility `v0.6.0` has no Kotlin money or number formatter (its Kotlin `Translator` groups numbers inside translated sentences only). The plan uses Android's own formatter in this app, which already gives Indian grouping; a ks-utility helper is added only if a second app needs one. Owner decision 2.
- Unit tests cover parsing ("1,23,456.5", "0.05", too many decimals, empty, too large), zero-decimal currencies (`JPY`), three-decimal ones (`KWD`), sums, and formatting in both languages.

## Storage

- **Room** database (`expenses.db`): tables `expense` (id, amount_minor, currency, category_id, note, occurred_at epoch millis, created_at), `category` (id, name key or custom name, icon, hidden, sort), and from later stages `budget` and `recurring_rule`. Months and days are grouped in the phone's `ZoneId` at query time. Schemas are exported to `mobile/android/schemas/`; every change has a `Migration` and a migration test ([Constraints](constraints.md)). Stage 2 and 3 tables are added by migrations, not created early.
- **DataStore** (Preferences) for settings: default currency, first-start done.
- `android:allowBackup="false"`, and no `INTERNET` permission.
- **Backup file**: one JSON file, `ks-expense-backup-YYYY-MM-DD.json`, with a format version, written and read with Android's built-in `org.json` (no new library). Restore checks the whole file first and changes nothing if any part is wrong. The database is not encrypted; the phone's own lock and storage encryption protect it.
- **CSV export**: `date,amount,currency,category,note` with the amount as a plain decimal (`123456.50`), UTF-8 with a byte-order mark so Excel reads Hindi text, RFC 4180 quoting. CSV is for spreadsheets; the JSON file is the backup. This changes the line in [Constraints](constraints.md) that says the CSV export is the backup (owner decision 3).

## Languages

- The suite rule through ks-utility ([Languages](https://github.com/kumarsumit17/ks-utility/blob/main/specs/languages.md)): text in `mobile/android/src/main/lang/en.json` and `hi.json` with the shared catalogue, turned into Android string resources by `lang.mjs android` before each build, checked by `lang.mjs check` in `check`. Hindi follows the [Hindi style guide](https://github.com/kumarsumit17/ks-utility/blob/main/specs/hindi-style.md).
- Follow the system, allow a choice, remember it: `AppCompatDelegate.setApplicationLocales` with `en` and `hi` in `locales_config.xml`. Settings offers "Same as the system", English, हिन्दी.
- Category names of the starting set are language keys, so they change with the language; names the user types stay as typed.

## App updates

As [App updates](app-updates.md) says: Settings shows "This is version X" and **Get the new version**, which opens the releases page in the browser. No update check, so no `INTERNET` permission. The ks-utility check is left out (owner decision 1).

## Build layout

- One Android application module at `mobile/android/` (Gradle path `:mobile:android`), package and application id `com.ks.expense`, debug build `com.ks.expense.debug`. Packages `transactions/`, `budgets/`, `reports/`, plus `data/`, `settings/`, `backup/`, `ui/`. The empty root `app/` skeleton is removed.
- Kotlin 2.4.21, AGP 9.5.2, Gradle 9.9.1 (as the other suite apps), JDK 17, minSdk 26, compile and target SDK 37.
- `ks-utility` moves from `v0.3.0` to `v0.6.0` in its own commit; `includeBuild("ks-utility/kotlin")`.
- Version from `-PversionName` / `-PversionCode`; `0.0.0` / `1` for local builds.
- Release: `.github/workflows/release.yml` calls `kotlin-app-release.yml@v0.6.0` with `android-module: ":mobile:android"` and no `tv-module`.

## Libraries

All from the version catalog; only the artifacts used.

| Library | Why | Licence |
|---|---|---|
| ks-utility (`com.ks:utility`) and its `lang.mjs` | Languages, `GitHubRepo` link | MIT (first-party) |
| Kotlin standard library, kotlinx-coroutines-android | Language, background work for the database | Apache 2.0 |
| AndroidX core-ktx, activity-compose | App entry, file pickers (Storage Access Framework) | Apache 2.0 |
| AndroidX appcompat | Per-app language on Android 8 to 12 (`setApplicationLocales`) | Apache 2.0 |
| AndroidX lifecycle (viewmodel-compose, runtime-compose) | Screen state that survives rotation | Apache 2.0 |
| Jetpack Compose (BOM; ui, foundation, material3) | Interface | Apache 2.0 |
| AndroidX Room (runtime, ktx, compiler) + KSP plugin | Database, migrations, exported schemas | Apache 2.0 |
| AndroidX DataStore Preferences | Settings | Apache 2.0 |
| Tests: JUnit 4, AndroidX Test (runner, ext-junit), room-testing, compose ui-test-junit4, kotlinx-coroutines-test | Unit, migration and screen tests | EPL 1.0 (JUnit 4), Apache 2.0 (the rest) |

**Vico is dropped** from the stack (owner decision 4): the category bars are plain Compose rows, so a chart library adds weight for nothing. It can come back with a real chart need. No dependency injection, navigation, JSON or CSV library: these are small and written by hand.

## Testing

- `./gradlew testDebugUnitTest`: money parsing and formatting, month and day grouping across time zones and month ends, CSV writing and quoting, backup write and read (round trip, wrong version, broken file), recurring due dates (stage 3), language files (`check`).
- `./gradlew connectedDebugAndroidTest` on `ks-phone-36`: Room queries, every migration from the first exported schema, and a Compose test of quick add in three taps.
- `lintDebug`, plus a check in the build that the merged manifest has no `INTERNET` permission.
- By hand on the phone emulator, each stage: add, edit, delete, month switch, export and open the CSV, back up and restore, both languages, dark theme, a large font size.

## Roadmap

| Stage | Version | Holds |
|---|---|---|
| 1, minimal first version | `v0.1.0` | Gradle build, quick add, categories, month view with totals and category bars, edit and delete, CSV export, backup and restore, English and Hindi, Settings with App updates, release workflow |
| 2 | `v0.2.0` | Budgets, search and filters, other currencies |
| 3 | `v0.3.0` | Recurring expenses, subscriptions from ks-tv-launcher |
| 1.0 | `v1.0.0` | After the owner has used it on a real phone; Google Play readiness is planned separately |

Each stage is built and tested on `dev`. The owner is asked before any tag; the repository's four signing secrets must exist first ([signing first](https://github.com/kumarsumit17/KS-Suite/blob/main/specs/versioning-signing.md#a-new-repository-signing-first)), and a rehearsal run must pass.

## Owner decisions

1. **Updates:** stay fully offline, with only the **Get the new version** link (recommended), or allow ks-utility's daily update check, which adds the `INTERNET` permission.
2. **Money formatting:** Android's formatter in this app (recommended), or a new Kotlin money helper in ks-utility first.
3. **Backup:** a JSON backup file next to the CSV export (recommended; the constraint line changes), or CSV only, as the constraints say now.
4. **Charts:** drop Vico and draw the category bars in Compose (recommended), or keep Vico.
5. **Stage 1 scope:** as in the roadmap (recommended), or a smaller first version without backup and restore.
