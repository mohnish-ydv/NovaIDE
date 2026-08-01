# NovaIDE M5 QA Report

## Release

- Version: `0.5.0-M5`
- Version code: `5`
- Package: `com.mohnishraj.novaide`
- Target SDK: 35
- Minimum SDK: 26

## GitHub connection repair

The M4 setup flow prefilled and validated `main` before reading repository metadata. Repositories whose default branch was `master`, `develop`, or another name could therefore fail connection despite a valid URL/token.

M5 changes the connection sequence to:

1. Parse and validate the repository URL.
2. Normalize a newly supplied or previously encrypted token.
3. Validate an authenticated identity when a token is present.
4. Read repository metadata.
5. Resolve the real default branch when Branch is blank, or verify the exact explicit branch.
6. Save token/repository settings only after successful verification.

Additional compatibility and safety work:

- Supports HTTPS, SSH, scheme-less GitHub URLs, `www.github.com`, and copied `/tree/<branch>` URLs.
- Trims surrounding whitespace and strips copied `Authorization:`, `Bearer`, and `token` prefixes.
- Normalizes compatible M4-saved prefixed tokens during decryption.
- Keeps the previous repository connection when verification fails.
- Separates invalid-token, repository-access, DNS, timeout, and TLS errors.
- Never silently falls back from an explicitly entered branch.
- Authorization remains restricted to `api.github.com`; force push remains disabled.

## M5 feature verification

Implemented and wired through **More → Android Tools**:

- Android project/module/SDK/dependency inspector.
- Secure manifest XML inspection.
- Manifest permission list/add/remove actions with dirty-buffer protection.
- Android resource health and size report.
- Wrapper-aware, nested-module-aware Gradle command generator.
- Bounded APK/ZIP structure analyzer.
- Bounded plain/ZIP build-log reader.
- Build, compiler, Lint, GitHub Actions, crash, ANR, and runtime-signature analyzer.

## Automated and compile-oriented checks

- Repository static QA: passed.
- Kotlin source inventory: 51 files, 7,984 lines.
- XML validation: 5/5 files passed.
- GitHub Actions workflow YAML parsing: passed.
- Android permission policy: passed; only `android.permission.INTERNET` is declared.
- Embedded GitHub token/secret literal scan: passed.
- Unfinished implementation marker scan: passed.
- Generated/local build-file exclusion scan: passed.
- M2 editor regressions: 7/7 passed.
- M3 project regressions: 5/5 passed.
- M4/M5 Git and connection-core regressions: 6/6 passed.
- M5 Android Suite regressions: 8/8 passed.
- Total pure-engine regressions: 26/26 passed.
- `GitHubStore`, `GitHubApiClient`, and token migration code type-checked against Android/API stubs with zero source errors.
- `MainActivity` plus the complete M5 Android Suite integration type-checked against Android API stubs with zero source errors.

## Hardening fixes found during QA

- Prevented compatible M4 encrypted tokens with copied prefixes from remaining unusable after upgrade.
- Prevented an explicit invalid branch from silently switching to the repository default.
- Preserved branch selection from copied `/tree/<branch>` URLs.
- Corrected Gradle task paths for nested modules such as `:features:mobile`.
- Corrected Gradle task paths for root application modules.
- Added an 8 MB aggregate text budget to ZIP log analysis, not only a per-entry cap.
- Retained ZIP path, entry-count, APK expansion, Git upload, pull/archive, and artifact limits from earlier milestones.

## Packaging checks

- Final archive integrity test: passed.
- Final archive was extracted into a clean directory and repository static QA passed again.
- Re-extracted XML and workflow YAML validation passed.
- Re-extracted M2–M5 pure-engine regression suites passed 26/26.
- Re-extracted `MainActivity` + M5 integration type-check passed.
- Re-extracted real GitHub Store/API connection-layer type-check passed.
- Re-extracted source tree matched the packaged source file hashes.
- SHA-256 checksum generated separately.

## Environment limitation

A complete Android SDK/Gradle toolchain is not installed in this execution sandbox, so a genuine local `assembleDebug` and Android Lint run could not be executed here. The included GitHub Actions workflow runs static QA, all JVM tests, clean APK assembly, and `lintDebug`; it uploads the APK only after the build step succeeds. No claim is made that a local APK was assembled in this sandbox.
