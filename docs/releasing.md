# Releasing

The tag workflow expects four encrypted repository secrets:

- `PURSIVO_KEYSTORE_BASE64`
- `PURSIVO_KEYSTORE_PASSWORD`
- `PURSIVO_KEY_ALIAS`
- `PURSIVO_KEY_PASSWORD`

Generate and preserve one release keystore for the lifetime of the application. Never commit it. Before tagging, install the candidate APK over the preceding signed version and verify database, documents, preferences and provider aliases survive.

Tags must be valid SemVer prefixed with `v`. A suffix (`-alpha.N`, `-beta.N`, `-rc.N`) makes the GitHub release a prerelease. A tag without a suffix is stable.

The workflow verifies tests and lint, assembles the signed universal APK, writes SHA-256 and update-manifest artifacts, generates an SBOM and provenance attestation, and creates the GitHub release. Stable-channel clients ignore every prerelease even if its numeric version is newer.
