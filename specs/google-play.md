---
type: Playbook
title: Google Play
description: What KS Expense Manager needs on Google Play, what is ready in the repository, and the steps the owner does in the Play Console.
tags: [google-play, release, android, listing, privacy]
---

# Google Play

## Ready in the repository

- **Bundle.** Every release publishes `ks-expense-manager-v<version>.aab` built for Play. It comes from the `play` build type (the release workflow's `play-build-type`): the release build without the **Get the new version** link to GitHub, because Play updates the app itself and its policy does not allow pointing users to another way of updating ([App updates](app-updates.md)). The APK on GitHub keeps the link. The AAB is signed with the suite keystore, which serves as the Play upload key; `versionCode` comes from the tag (`android-v0.5.0` → 500) and always rises.
- **Target.** `targetSdk` 37, the newest; `minSdk` 26. No `INTERNET` permission (the build fails if it appears, `checkNoInternetPlay`) and no runtime or sensitive permission; the only manifest permission is AndroidX's internal `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which users never see.
- **Listing.** `mobile/android/play-listing/metadata/android/` in the layout Play upload tools read (fastlane `supply`): title, short and full description in English (`en-US`) and Hindi (`hi-IN`); the 512 × 512 icon and the 1024 × 500 feature graphic (drawn by `mobile/android/play-listing/PlayImages.java` from the launcher icon's shapes); phone screenshots at 1080 × 2160, taken from the emulator with sample data.
- **Privacy policy.** [`PRIVACY.md`](../PRIVACY.md), public at https://github.com/kumasumit/ks-expense-manager/blob/main/PRIVACY.md.

## Answers for the Play Console

| Question | Answer |
|---|---|
| App name | KS Expense Manager (Hindi: KS खर्च मैनेजर) |
| Category | Finance |
| Free or paid | Free |
| Data safety | No data collected, no data shared. Data is not encrypted in transit because nothing is transmitted. Users can delete their data by uninstalling the app or clearing its storage. |
| Ads | No ads |
| Content rating | Fill in the questionnaire as a reference/utility app with no user interaction, no sharing and no purchases: everyone |
| Target audience | 18 and over (the app does not target children) |
| Financial features declaration | A personal expense tracker only: no banking, loans, payments, trading or crypto |
| Government app | No |
| Sensitive permissions | None |
| Form factors | Phone only (no Android TV, Wear OS or Chromebook build) |

## Steps for the owner (to do)

1. Create the app in the Play Console (package `com.ks.expense`, free) and turn on Play App Signing with the suite key (`ks-release`) as the upload key.
2. Fill in the store listing from `mobile/android/play-listing/` and the answers above; paste the privacy policy URL.
3. Upload the AAB of the newest `android-v` release. A new personal developer account must first run a closed test with at least 12 testers for 14 days before production.
4. Later: automate uploads from the release workflow with a Play service account (a ks-utility change); not needed for the first release.

## To do, not blocking releases

- The Play Console steps above.
- The pre-launch report once Play has processed the bundle.
