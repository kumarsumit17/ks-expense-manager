# ks-expense-manager

Minimalist, privacy-first personal finance and expense tracker.

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
