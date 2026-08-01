# NovaIDE M6 QA Report

## Release identity

- Version: `0.6.0-M6`
- Version code: `6`
- Package: `com.mohnishraj.novaide`
- Compile/target SDK: 35
- Minimum SDK: 26
- Project files: 100

## Implemented M6 scope

- Shared Android-Keystore/AES-GCM credential vault for GitHub, GitLab and five AI configurations.
- Official credential creation links, permission/privacy guidance, verify-before-save behavior, model discovery and safe removal.
- OpenAI, Gemini, Groq, OpenRouter and custom HTTPS OpenAI-compatible AI adapters.
- Project context selection, sensitive-path exclusion, credential redaction, bounded request/response sizes and safe preview/apply flows.
- Offline autocomplete, snippets, local lint, regex quick fixes and static project analysis.
- Empty GitHub repository first-commit support through root tree + parentless commit + new branch reference.
- Read/write permission diagnostics for GitHub and GitLab; stale-head protection and no force-push.

## Executed verification

### Repository static/security QA

- Result: **passed**
- XML files: 5
- Kotlin files: 71
- Kotlin lines: 10,120
- Registered JVM tests: 34
- Android permissions: only `android.permission.INTERNET`
- No forbidden broad-storage, package-install, all-packages, Logcat or usage-access permission.
- Required M1–M6 engines, guides, workflow gates, size limits, encryption primitives and safe-write checks present.
- Real-looking GitHub, GitLab, OpenAI and Google API credential literal scan passed.
- No `TODO(...)`, `NotImplementedError` or generated/local build directories shipped.

### JVM regressions

Result: **34/34 passed**.

- M2 editor: 7/7
- M3 workspace/project: 5/5
- M4 Git: 6/6
- M5 Android Suite: 8/8
- M6 AI, credentials, local intelligence and empty-repository planning: 8/8

M6 coverage includes token normalization, secret redaction, sensitive-path exclusion, AI traversal/duplicate-patch blocking, autocomplete, snippet cursor stability, deterministic lint/fixes, static conflict/secret-path findings, initial/update commit planning, stale-head rejection and initial deletion rejection.

### Kotlin compile-oriented checks

- Non-UI production sources compiled against an Android API contract jar with **zero diagnostics**.
- `MainActivity` M6 integration compiled against the production-engine jar and isolated UI contracts with **zero diagnostics**.
- Test sources compiled against a JUnit contract and the production-engine jar before execution.

### Format and packaging checks

- All XML files parsed successfully.
- GitHub Actions YAML parsed successfully.
- Workflow order verified: static QA → JVM tests → clean debug APK assembly → Android Lint → artifacts.
- Final archive is re-extracted and all checks are repeated before handoff.
- Source/package SHA-256 comparison and ZIP CRC integrity are performed after final packaging.

## Important environment limitation

The sandbox does not contain a complete Android SDK/Gradle distribution and direct network installation is unavailable, so a genuine local `:app:assembleDebug` and `:app:lintDebug` run is **not claimed**. The included GitHub Actions workflow installs Android 35 and Gradle 8.9, then runs static QA, all unit tests, clean APK assembly and Android Lint before publishing `NovaIDE-M6-Debug-APK`.
