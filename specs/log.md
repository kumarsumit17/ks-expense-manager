# 2026-11-25

* **Update**: `ks-utility` moved from `v0.6.0` to `v0.9.0` (more languages, the translation tools; [ks-utility translation tools](https://github.com/kumarsumit17/ks-utility/blob/main/specs/translating.md)).

# 2026-11-24

* **Update**: Stage 1 built on `dev`: Gradle build in `mobile/android/` (`:mobile:android`, `com.ks.expense`), quick add, categories, month view with totals and category bars, edit and delete, CSV export, JSON backup and restore, English and Hindi, Settings with App updates. Room schema 1 exported to `mobile/android/schemas/`. [Build plan](plan.md): Save moved to the sheet's title row; DataStore and lifecycle libraries left for later stages; kotlinx-serialization-core raised to 1.8.1 by a constraint (room-testing needs it). New dependencies, all Apache 2.0: AndroidX core-ktx, activity-compose, appcompat, Compose (ui, foundation, material3, material-icons-core), Room 2.8.5 with KSP; tests: JUnit 4 (EPL 1.0), AndroidX Test, room-testing, compose ui-test.
* **Update**: Release workflow calls `kotlin-app-release.yml@v0.6.0` with `android-module: ":mobile:android"` and no `tv-module`; its tests run the unit tests, the language-file check and the no-`INTERNET` check.
* **Update**: [Build plan](plan.md) approved by the owner: offline updates link only, Android's formatter for money, JSON backup next to the CSV export, no Vico, stage 1 as planned. [Constraints](constraints.md): the JSON file is the backup; schemas in `mobile/android/schemas/`. [Overview](overview.md): Vico dropped.
* **Creation**: [Build plan](plan.md) added as a proposal: features, money, storage with backup and restore, languages, libraries with licences, staged roadmap and five owner decisions.

# 2026-11-23

* **Update**: Release: `.github/workflows/release.yml` now calls ks-utility's shared release workflow (`kotlin-app-release.yml@v0.4.0`, [spec](https://github.com/kumarsumit17/ks-utility/blob/main/specs/release-workflow.md)): one workflow for every Kotlin app, with a rehearsal input, pinned actions, signature checks, build-provenance attestations and `gh` publishing.
* **Creation**: [App updates](app-updates.md) added: the App updates section shows the version and a Get the new version link to the GitHub releases page; no update check or download, because of the offline constraint.
* **Update**: `ks-utility` moved to `v0.3.0`.

# 2026-11-21

* **Update**: [Dependencies](dependencies.md): `ks-utility` added as a git submodule for shared generic utilities.
* **Initialization**: `specs/` created as an Open Knowledge Format (OKF) v0.2 bundle with this log and `index.md`.
* **Creation**: [Overview](overview.md), [Constraints](constraints.md) added as drafts, from `README.md` and `CLAUDE.md`.
* **Creation**: [Dependencies](dependencies.md) added: third-party dependencies are kept to the minimum.
