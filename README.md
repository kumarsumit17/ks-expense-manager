# KS Expense Manager

> Part of the [KS-Suite](https://github.com/kumarsumit17/KS-Suite).

A minimalist, privacy-first personal finance and expense tracker. All data stays on the device.

| | |
|---|---|
| **Platform** | Android |
| **Tech stack** | Kotlin, Jetpack Compose, Room, DataStore |

## Getting started

```bash
git clone --recurse-submodules <repo-url> ks-expense-manager
cd ks-expense-manager
./gradlew assembleDebug
```

What it does and the roadmap: [specs/plan.md](specs/plan.md).

## Release

Releases are cut by pushing an annotated Android tag on `main` (`git tag -a android-v1.2.3 -m android-v1.2.3 && git push origin android-v1.2.3`).
See the suite-level [CI/CD guide](https://github.com/kumarsumit17/KS-Suite#-cicd-semantic-versioning--automated-signing).

## License

MIT. See [LICENSE](LICENSE).
