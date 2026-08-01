# NovaIDE M8 QA Report

**Release:** `0.8.0-M8`  
**Milestone:** Plugins & Productivity  
**Package:** `com.mohnishraj.novaide`

## Delivered scope

- Fuzzy command palette with core, task and enabled-extension commands.
- Declarative `.nova-plugin.json` extension runtime.
- Explicit `READ_WORKSPACE`, `EDITOR_WRITE`, `OPEN_EXTERNAL`, and `CLIPBOARD_WRITE` permissions.
- Local manifest review, enable/disable, uninstall, and command execution UI.
- Safe Nova Console with bounded workspace reads and no arbitrary shell execution.
- Built-in and custom multi-step tasks/workflows with stop-on-failure behavior.
- App-private persistence for extension manifests and saved tasks.
- Existing Android Keystore credential isolation retained.
- All M1–M7 systems retained.

## Regression tests

A fresh Kotlin/JVM build executed the complete retained test matrix after the final M8 hardening changes:

| Area | Result |
|---|---:|
| M2 editor engines | 7/7 |
| M3 project/workspace engines | 5/5 |
| M4 Git engines | 6/6 |
| M5 Android Suite | 8/8 |
| M6 AI/local intelligence | 8/8 |
| M7 diagnostics | 9/9 |
| M8 plugins/productivity | 9/9 |
| **Total** | **52/52 passed** |

M8 tests cover manifest parsing, permission enforcement, insecure-link rejection, fuzzy command search, path-traversal rejection, bounded grep/variables, safe built-in tasks, shell-command rejection, and stop-on-failure workflows.

## Compile-oriented checks

- M8 pure Kotlin production engines compiled in the JVM regression build.
- Android-dependent plugin/task persistence stores previously compiled against minimal Android API stubs.
- `MainActivity.kt` was scanned by the Kotlin parser after final integration; no Kotlin syntax diagnostics were found. Unresolved Android/project symbols are expected without an Android SDK classpath.
- XML resource validation passed.
- GitHub Actions YAML validation passed.

## Static and security QA

- Static QA: **passed**.
- Kotlin files: **90**.
- Kotlin lines: **12,408**.
- Registered JVM tests: **52**.
- Android permissions: only `android.permission.INTERNET`.
- No arbitrary APK/JAR/DEX plugin loading.
- No unrestricted shell, binary execution, pipes, redirects, package-manager commands, or parent traversal in Nova Console.
- HTTP extension links and credential-bearing URLs are rejected.
- Extension capabilities require declared permissions and explicit user execution.
- Credentials remain isolated from plugins and tasks.

## Packaging verification

The final archive is verified by:

- ZIP CRC/integrity test.
- Fresh extraction into a clean directory.
- Static QA rerun against the extracted project.
- Complete 52-test regression rerun against extracted source.
- SHA-256 comparison of every packaged file against the release source tree.

## Environment limitation

The execution environment does not include a complete Android SDK/Gradle toolchain, so a genuine local `assembleDebug` and Android Lint run are not claimed. The included GitHub Actions workflow runs repository QA, all unit tests, clean APK assembly, and `lintDebug` before uploading the M8 APK artifact.
