# ks-expense-manager

Minimalist, privacy-first personal finance and expense tracker.

## Specs
- All specs for this project live in `specs/` in the repository root, as an Open Knowledge Format (OKF) v0.2 bundle. Never create spec files anywhere else. Start at `specs/index.md`.
- One topic per file. Every file except `index.md` and `log.md` starts with YAML frontmatter: `type` (required: `Specification`, `Policy`, `Plan`, `Playbook` or `Reference`), `title`, `description` (one sentence), `tags`.
- When a spec is added: list it in `specs/index.md` with its description. When a spec is added or changed: add a line under today's date (`# YYYY-MM-DD`, newest first) in `specs/log.md`.
- Link between specs with relative markdown links (`[Dependencies](dependencies.md)`), so they also work on GitHub.

## Stack
- Kotlin, Jetpack Compose (Material 3), Room, Vico (charts), DataStore
- Package: `com.ks.expense`. Features: `transactions/`, `budgets/`, `reports/`
- minSdk 26 · targetSdk latest stable

## Constraints
- **Offline only.** Don't declare the `INTERNET` permission. No bank sync, no cloud accounts, no analytics.
- **Money is never floating point.** Store amounts as `Long` in minor units (paise or cents) plus an ISO-4217 currency code. Format them with `NumberFormat.getCurrencyInstance`. Never use `Double` or `Float` for money.
- **Dates.** Store `Instant` (UTC epoch millis). Group by month and day in the user's `ZoneId` at query time.
- **Schema changes** need a Room `Migration` plus an exported schema in `app/schemas/`. Data loss on upgrade is a release blocker.
- **Minimalism.** Adding an expense takes three taps or fewer from launch. Avoid new screens or settings when an existing one can absorb the feature.
- Import and export use the user's own CSV through the Storage Access Framework. Set `android:allowBackup="false"`. The user's own CSV export is the backup.
- **Optional import from `ks-tv-launcher` subscriptions:** a user-started CSV/JSON share intent, which creates recurring expenses. There is no background link between the apps.

## Commands
```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest             # money/date math must have unit tests
./gradlew connectedDebugAndroidTest     # Room migration tests
./gradlew lintDebug
```
