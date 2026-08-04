# Pursivo

**A private, expressive command center for moving job applications forward.**

Pursivo is an open-source, Android-first job application tracker. It works without an account, a backend, AI, or an internet connection. When you choose to use AI, you bring your own provider key, inspect the exact context first, and decide what to keep.

> Current milestone: `v0.1.0-alpha.1` foundation. The data model and backup format may evolve before the stable release.

## What is already implemented

- Today dashboard with a guilt-free pursuit path, next actions, seven-day activity metrics, and an on-demand AI brief.
- Searchable application list and horizontal stage board, archive/restore, stage changes, next actions, and immutable timeline events.
- Application Studio with explicit context preview, evidence-grounded fit analysis, gaps, next action, resume bullets, and cover-letter drafts.
- Career evidence profile and an app-private PDF, DOCX, and TXT document vault with editable extracted text.
- Customizable stages whose user-facing labels are separated from fixed analytics semantics.
- BYOK adapters for OpenAI, Anthropic, Gemini, and HTTPS OpenAI-compatible endpoints. Credentials are wrapped by Android Keystore.
- Versioned ZIP backup/restore that excludes provider keys.
- Stable/prerelease GitHub update discovery with a 24-hour foreground throttle and manual check.
- Native `MaterialExpressiveTheme` motion, dynamic Material You color on Android 12+, light/dark/system modes, Roboto Flex, morphing shapes, an animated pursuit path, and the floating action dock.

## Product promises

- No account, telemetry, ads, crash reporting, cloud sync, or hidden network work.
- Core tracking remains available offline and without AI.
- AI runs only in the foreground after a deliberate action.
- Unknown career facts become explicit placeholders, never invented achievements.
- Provider keys are never put in the Room database, logs, backups, or saved UI state.

## Build locally

Requirements: JDK 17 and Android SDK Platform 37. The app targets Android 16/API 36 and supports Android 8.0/API 26 and newer.

```bash
./gradlew test lintDebug detekt assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
With an emulator or device connected, run the first-launch interaction test with `./gradlew :app:connectedDebugAndroidTest`.

## Project structure

```text
app/                Compose screens, Android entry points, state and user flows
core:model/         Serializable domain contracts
core:data/          Room, DataStore, vault, backup, secrets and update discovery
core:ai/            Provider adapters and grounded analysis/drafting engines
core:designsystem/  Pursivo theme, typography and expressive UI wrappers
docs/               Architecture, privacy and release documentation
```

The architecture intentionally isolates experimental Material 3 APIs behind `core:designsystem`, and provider-specific payloads behind `AiProvider`. See [architecture.md](docs/architecture.md).

## Releases

Tags are SemVer:

- `v0.1.0-alpha.1` and `v0.1.0-beta.1` publish GitHub prereleases.
- `v0.1.0` publishes a stable release.

The release workflow builds a signed universal APK, SHA-256 file, SBOM, update manifest, provenance attestation, and generated release notes. Maintainers must configure the signing secrets described in [releasing.md](docs/releasing.md).

## Contributing and security

Read [CONTRIBUTING.md](CONTRIBUTING.md), the [code of conduct](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md). Please do not report suspected key-handling or backup vulnerabilities in a public issue.

## Roadmap

The delivery sequence is foundation, identity/vault, tracking, Today/reminders, AI foundation, Studio, updater/beta hardening, then public v0.1.0. Accounts, cloud sync, automatic job-board ingestion, auto-apply, background paid AI, and telemetry are intentionally outside v1.

## License

Pursivo is licensed under [Apache License 2.0](LICENSE). Roboto Flex is distributed under the SIL Open Font License; see [NOTICE](NOTICE).
