# ks-expense-manager

Minimalist, privacy-first personal finance and expense tracker.

## Specs
- All specs for this project live in `specs/` in the repository root, as an Open Knowledge Format (OKF) v0.2 bundle. Never create spec files anywhere else. Start at `specs/index.md`.
- One topic per file. Every file except `index.md` and `log.md` starts with YAML frontmatter: `type` (required: `Specification`, `Policy`, `Plan`, `Playbook` or `Reference`), `title`, `description` (one sentence), `tags`.
- When a spec is added: list it in `specs/index.md` with its description. When a spec is added or changed: add a line under today's date (`# YYYY-MM-DD`, newest first) in `specs/log.md`.
- Link between specs with relative markdown links (`[Dependencies](dependencies.md)`), so they also work on GitHub.

## Stack
- Kotlin, Jetpack Compose (Material 3), Room, SharedPreferences for the one setting (no DataStore, no chart library)
- Package: `com.ks.expense`. Features: `transactions/`, `budgets/`, `reports/`
- minSdk 26 · targetSdk latest stable

## Constraints
- **Offline only.** Don't declare the `INTERNET` permission. No bank sync, no cloud accounts, no analytics.
- **Money is never floating point.** Store amounts as `Long` in minor units (paise or cents) plus an ISO-4217 currency code. Format them with `NumberFormat.getCurrencyInstance`. Never use `Double` or `Float` for money.
- **Dates.** Store `Instant` (UTC epoch millis). Group by month and day in the user's `ZoneId` at query time.
- **Schema changes** need a Room `Migration` plus an exported schema in `mobile/android/schemas/`. Data loss on upgrade is a release blocker.
- **Minimalism.** Adding an expense takes three taps or fewer from launch. Avoid new screens or settings when an existing one can absorb the feature.
- Files go through the Storage Access Framework only. Set `android:allowBackup="false"`. The backup is a JSON file the user saves and restores ([Build plan](specs/plan.md#storage)); the CSV export is for spreadsheets.
- **Optional import from `ks-tv-launcher` subscriptions:** a user-started CSV/JSON share intent, which creates recurring expenses. There is no background link between the apps.

## Commands
```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest             # money/date math must have unit tests
./gradlew connectedDebugAndroidTest     # Room migration tests
./gradlew lintDebug
./gradlew check                         # unit tests, language files, no-INTERNET check
```
- Gradle tasks run from the root apply to the one module, `:mobile:android`. The debug build is `com.ks.expense.debug`.
- Releases: tag `android-vX.Y.Z` (annotated, on `main`; the suite's platform tags, used by one-platform apps too). v0.1.0 and v0.1.1 (the bridge release) have plain tags; no plain tag after that. Rehearse with Actions › Release › Run workflow (`platform: android`).
- Interface text is in `mobile/android/src/main/lang/{en,hi}.json`; `lang.mjs android` turns it into resources under `build/` before every build.

Emulators (from the KS-Suite workspace; suite spec `specs/testing.md`): `../scripts/emulators.sh create` once, then `../scripts/emulators.sh start phone` boots the phone AVD (`ks-phone-36`, API 36). Then:
```bash
export ANDROID_SERIAL=$(../scripts/emulators.sh serial phone)
./gradlew installDebug connectedDebugAndroidTest
adb shell monkey -p com.ks.expense.debug -c android.intent.category.LAUNCHER 1   # open the debug build
```

## Platform folders
- Every KS-Suite app repo has the same top-level platform folders: `tv/google-tv/`, `desktop/mac/`, `desktop/windows/`, `mobile/android/` and `mobile/ios/`. All five exist in every app repo, whether or not the app supports that platform yet.
- This app targets `mobile/android`. The other folders are empty on purpose (a `.gitkeep` only): they mark where that platform's code would go, not missing work. Do not add code for a platform that is not a target without the owner asking.
- Three layers, narrowest first: a platform folder holds code for one platform of this app; `shared/` in the repository root holds code used by two or more platforms of this app, written once and never copied between platform folders; `ks-utility` holds generic code used by several apps. This app has one target, so it has no `shared/` folder.
- The app module is `mobile/android/` (Gradle path `:mobile:android`).
- Never rename, merge or remove these folders, and never add another top-level platform folder; the layout is a suite-wide rule.

## Shared code (`ks-utility`)
- Generic utility code shared across the KS-Suite apps lives in the `ks-utility` repository. It is a dependency of this app: a git submodule at `ks-utility/`, pinned to a commit. After cloning, run `git submodule update --init`.
- Before writing a generic helper (one that knows nothing about this app), look in `ks-utility/kotlin` and use it from there. If it is missing and another app would need it too, add it in the `ks-utility` repository, not here. Application-specific logic stays in this repo.
- Never edit files under `ks-utility/` from this repo. Change them in the `ks-utility` repository, then move the pin: `git -C ks-utility fetch && git -C ks-utility checkout <commit>`, and commit the changed `ks-utility` entry.
- Gradle wiring, to add when this repo's Gradle build is created: `includeBuild("ks-utility/kotlin")` in `settings.gradle.kts`, `ks-utility = { module = "com.ks:utility" }` in `gradle/libs.versions.toml`, and `implementation(libs.ks.utility)` in the module that uses it. It is a plain Kotlin/JVM library with no Android SDK.
- `ks-utility` has no network code and no third-party dependencies, so it does not add the `INTERNET` permission. Check that again whenever the pin moves.
- CI checks out with `submodules: true` (`.github/workflows/release.yml`).

## Working efficiently (keep token use low)
- Use the knowledge graph first: follow the rules in the `graphify` section at the end of this file. [graphify](https://github.com/Graphify-Labs/graphify) is a development tool only; it is never a dependency of the code and is never bundled or shipped.
- One-time setup on a new machine (owner): `uv tool install graphifyy` (the package name has two y's), then `/graphify .` in Claude Code. The setup in `.claude/` is committed; `graphify-out/` is generated and git-ignored.
- Read only the specs the task needs, and do not re-read a file that has not changed in this session.
- Never read generated files: `build/`, `.gradle/`, `graphify-out/graph.json`.
- Keep command output short (show only failures), and keep summaries brief.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. When the answer is under a Markdown heading, `graphify section "<heading>"` prints that section's text, so the .md need not be opened. These return a scoped subgraph, usually much smaller than GRAPH_REPORT
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- `graphify-out/` is generated and git-ignored: never commit it.
