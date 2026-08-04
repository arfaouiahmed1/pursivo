# Contributing to Pursivo

Thank you for helping make job searching calmer and more private.

1. Start from an issue for behavior changes and security-sensitive work.
2. Create a short-lived branch from `main`.
3. Keep commits conventional (`feat:`, `fix:`, `docs:`, `test:`, `build:`, `ci:`, `refactor:`).
4. Run `./gradlew detekt test lint assembleDebug`.
5. Open a pull request describing user-visible behavior, privacy/network impact, and verification performed.

Feature code must not depend directly on experimental Material 3 components; add or change a Pursivo-owned wrapper in `core:designsystem`. New AI providers implement `AiProvider`, must redact secrets and payloads from logs, and need mock-server contract tests. Room schema changes require an exported schema and migration test. User-facing strings must remain localization-ready.

Do not add analytics, advertising, accounts, cloud sync, background AI, scraping, auto-apply behavior, or silent APK installation without an accepted project proposal that explicitly revisits the product boundaries.
