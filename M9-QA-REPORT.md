# NovaIDE M9 QA Report

**Release:** `0.9.0-M9`  
**Milestone:** Web Preview Runtime  
**Verification date:** 2026-08-02

## Delivered scope

M9 adds an in-app static web runtime with direct Run controls, HTML entry detection, an isolated `https://nova.local/` origin, relative project assets, split/fullscreen preview, unsaved-buffer live reload, console/runtime diagnostics, viewport profiles, JavaScript and external-resource controls, SPA fallback, build-tool warnings and external document handoff.

## Regression matrix

A fresh Kotlin/JVM build executes the complete retained matrix:

| Area | Passed |
|---|---:|
| M2 editor core | 7/7 |
| M3 project workspace | 5/5 |
| M4 Git core | 6/6 |
| M5 Android Suite | 8/8 |
| M6 AI/local intelligence | 8/8 |
| M7 diagnostics | 9/9 |
| M8 plugins/productivity | 9/9 |
| M9 Web Preview | 9/9 |
| **Total** | **61/61** |

M9 tests cover traversal rejection, active-entry priority, generated-output selection, unbuilt-tool diagnostics, sensitive-file blocking, browser MIME mapping, idempotent runtime injection, safe SPA fallback and bounded/deduplicated console storage.

## Compile-oriented verification

- M9 pure Kotlin runtime engines compiled and executed in the JVM regression build.
- `WorkspaceWebServer` compiled against an Android/WebView API-surface stub, including request interception, status responses and bounded streams.
- `WebPreviewSettingsStore` compiled against an Android preferences API-surface stub.
- `MainActivity.kt` completed a Kotlin parser scan with zero syntax/parser diagnostics.
- All XML files parsed successfully.
- The GitHub Actions YAML parsed successfully and retains static QA, unit tests, `assembleDebug` and `lintDebug` gates.

## Static and security gates

- 95 Kotlin files and 13,401 Kotlin lines checked.
- 61 JVM test methods inventoried.
- Android permission set is exactly `android.permission.INTERNET`.
- No broad storage, package-install, log-reading or usage-access permission is declared.
- WebView file access and content access are disabled.
- Mixed HTTP content is blocked.
- Cookies and third-party cookies are disabled.
- WebView debugging is disabled.
- No `addJavascriptInterface` bridge is present.
- External subresources are disabled by default and restricted to HTTPS when enabled.
- Unsafe top-level schemes are blocked from preview navigation.
- `.env`, credentials, keystores, keys and internal tool folders are not served.
- Traversal/control-character paths, oversized text and oversized/unknown-size binary streams are bounded.

## Packaging verification

The final release ZIP is created from a clean source tree, CRC-tested, freshly extracted, and then subjected again to repository static QA and the 61-test regression matrix. Every packaged file is compared to the source tree using SHA-256.

## Environment limitation

The execution environment does not contain the complete Android SDK/Gradle toolchain and cannot resolve external SDK downloads, so a genuine local `assembleDebug` and Android Lint run are **not claimed**. The included GitHub Actions workflow is the authoritative Android build gate and uploads `NovaIDE-M9-Debug-APK` only after tests, APK assembly and `lintDebug` complete.
