# 2026-11-24

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
