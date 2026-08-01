# NovaIDE M10 QA Report

## Release

- Version: `1.0.0-M10`
- Version code: `10`
- Milestone: Universal Runtime and final release hardening

## Implemented verification targets

- Universal runtime classification and package-manager detection
- Generated build-output discovery
- Vite root `index.html` false-positive regression
- Bounded duplicate-key-rejecting `package.json` parser
- Termux executable/argument allowlist and shell quoting
- Shared-storage SAF path resolution
- Markdown HTML escaping and Mermaid strict mode
- Explicit loopback-origin validation
- Private Termux result receiver and package visibility
- Existing M1–M9 regression suite

## Automated results

- JVM regression tests: **71/71 passed**
- M10 tests: **10/10 passed**
- M1–M9 retained tests: **61/61 passed**
- M10 pure/runtime Android-stub compile: passed
- Whole-project Kotlin parser scan: zero syntax diagnostics
- Kotlin files: **106**
- Kotlin lines: **14,680**
- XML parsing: passed
- GitHub workflow YAML parsing: passed
- Repository static/security QA: passed
- Secret-literal scan: passed
- Unfinished implementation scan: passed
- ZIP CRC test: passed
- Fresh-extraction regression and static QA: passed
- File-by-file SHA-256 packaging comparison: passed

## Security boundaries

- No direct `Runtime.exec` or `ProcessBuilder` use
- No unrestricted shell API
- No silent command execution
- No arbitrary executable names
- No path guessing for cloud/virtual SAF providers
- Only the exact user-confirmed loopback origin is allowed
- Termux result receiver is not exported
- Workspaces remain accessed through SAF inside NovaIDE

## Environment limitation

The execution environment did not provide the full Android SDK/Gradle toolchain, so local `assembleDebug` and Android Lint are not represented as completed here. The included GitHub Actions workflow runs static QA, all unit tests, clean debug APK assembly, and `lintDebug` before publishing artifacts.
