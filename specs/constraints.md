---
type: Policy
title: Constraints
description: The constraints every change to KS Expense Manager must keep.
tags: [constraints, policy]
status: draft
---

# Constraints

- **Offline only.** Don't declare the `INTERNET` permission. No bank sync, no cloud accounts, no analytics.
- **Money is never floating point.** Store amounts as `Long` in minor units (paise or cents) plus an ISO-4217 currency code. Format them with `NumberFormat.getCurrencyInstance`. Never use `Double` or `Float` for money.
- **Dates.** Store `Instant` (UTC epoch millis). Group by month and day in the user's `ZoneId` at query time.
- **Schema changes** need a Room `Migration` plus an exported schema in `mobile/android/schemas/`. Data loss on upgrade is a release blocker.
- **Minimalism.** Adding an expense takes three taps or fewer from launch. Avoid new screens or settings when an existing one can absorb the feature.
- Files go through the Storage Access Framework only. Set `android:allowBackup="false"`. The backup is a JSON file the user saves and restores ([Build plan](plan.md#storage)); the CSV export is for spreadsheets.
- **Optional import from `ks-tv-launcher` subscriptions:** a user-started CSV/JSON share intent, which creates recurring expenses. There is no background link between the apps.

These rules are also in `CLAUDE.md`, where they are loaded into every AI-assisted session; change both together.
