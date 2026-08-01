# NovaIDE M5 — Android Development Suite

M5 adds phone-first Android project diagnostics and build helpers without requesting broad storage, package-install, or device-log permissions.

## Android Tools Center

Open a workspace, then choose **More → Android Tools**.

### Project Inspector

NovaIDE scans Android Gradle modules, literal SDK values, application IDs, build types, dependencies, manifest permissions/components, source/test counts, assets, native libraries, and common correctness/security problems.

Checks include missing `android:exported`, malformed manifests, cleartext traffic, dynamic dependency versions, `jcenter()`, insecure HTTP repositories, invalid SDK relationships, hardcoded signing secrets, and debuggable release configuration.

### Manifest & Permissions

- Open the detected app manifest directly in the editor.
- Add a curated common Android permission.
- Remove existing `<uses-permission>` declarations.
- Refuse automated writes while the manifest has unsaved editor changes.

The editor is intentionally narrow: it does not rewrite unrelated XML or silently add runtime permission code.

### Resource Manager

Shows resource counts, qualifiers, total packaged size, largest resources, invalid filenames, same-qualifier duplicates, unqualified bitmaps, and oversized packaged assets.

### Gradle Build Center

Generates copyable commands for debug APKs, unit tests, Android Lint, clean CI verification, dependency trees, signing reports, task discovery, and daemon cleanup. It automatically prefers `./gradlew` when the wrapper is present and targets the detected application module.

### APK Analyzer

Pick any APK/ZIP through Android's document picker. NovaIDE safely reports DEX count, native ABIs/libraries, resource and asset entries, manifest/resources table presence, JAR/v1 signature entries, expanded size, and largest packaged files. Inspection is bounded against malicious or oversized archives.

### Build & Crash Analyzer

Import `.txt`, `.log`, `.json`, `.html`, or a logs ZIP. NovaIDE recognizes common Kotlin/Java compiler, AAPT, manifest merger, dependency, Java/Gradle, SDK, duplicate-class, memory, Lint, signing, GitHub Actions, timeout, Android crash, ANR, `SecurityException`, missing-class, null-reference, and resource-inflation signatures.

Direct device-wide Logcat access is intentionally not requested because modern Android restricts it to privileged/debug environments. Imported Logcat/crash files are supported.

## GitHub Connection Fix

M4 prefilled and validated `main`, so repositories using `master`, `develop`, or another default branch could fail even when the URL and token were valid. M5:

- lets Branch stay blank;
- reads the repository's real default branch first;
- validates the resolved branch before saving the connection;
- trims surrounding whitespace and normalizes copied `Authorization:`, `Bearer …` and `token …` prefixes while rejecting embedded whitespace;
- validates authenticated tokens through GitHub before persistence;
- normalizes compatible prefixed tokens previously encrypted by M4;
- preserves the previous connection when verification fails;
- gives separate messages for invalid tokens, inaccessible private repositories, DNS failure, timeout, and TLS failure.

## Security Boundaries

- GitHub tokens remain AES-GCM encrypted with Android Keystore.
- Authorization is sent only to `api.github.com`.
- No force push.
- No broad storage permission.
- No APK installation permission.
- No privileged Logcat permission.
