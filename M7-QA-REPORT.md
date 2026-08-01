# NovaIDE M7 QA Report

**Release:** `0.7.0-M7`  
**Milestone:** Debug & Analysis  
**Date:** 1 August 2026

## Delivered systems

- Local crash and ANR trace analysis with deepest-cause selection, stable fingerprints, secret redaction, project-frame resolution, and file/line navigation.
- Bounded whole-project health audit with severity totals and a 0–100 triage score.
- Exact-file and normalized repeated-code detection.
- Conservative unused private symbol, import, unreachable-statement, and Android resource candidates.
- Kotlin/Java, JavaScript/TypeScript, and Python dependency graph analysis with hubs, orphans, and cycle detection.
- Performance heuristics for large assets/sources, long functions, deep nesting, blocking calls, whole-file I/O, nested loops, repeated allocations, list refreshes, bitmap decoding, and unstructured coroutine scopes.
- Security heuristics for sensitive files, possible embedded secrets, cleartext endpoints, weak crypto, AES/ECB, weak randomness, risky WebViews, TLS bypass, wildcard CORS, dynamic execution, exported Android components, and unpinned dependencies.
- A mobile-first Debug & Analysis Center with background execution, copyable reports, confidence labels, and exact source navigation.

## Executed regression tests

A fresh Kotlin/JVM test build executed the complete retained suite after the final M7 hardening patch:

| Suite | Result |
|---|---:|
| M2 editor core | 7/7 |
| M3 project/workspace core | 5/5 |
| M4/M6 Git core | 6/6 |
| M5 Android Suite core | 8/8 |
| M6 AI/local intelligence | 8/8 |
| M7 diagnostics | 9/9 |
| **Total** | **43/43 passed** |

The M7 tests cover root-cause/frame resolution, secret redaction, exact and block duplicates, dead-code candidates, dependency cycles/orphans, performance findings, security findings, health scoring, and truncation propagation.

## Repository and source checks

- Repository static QA: **passed**.
- Kotlin production/test sources counted: **80 files / 11,231 lines**.
- Registered JVM tests: **43**.
- M7 pure diagnostic engines compiled in the JVM regression build.
- MainActivity M7 parser scan: **no Kotlin syntax diagnostics**.
- Android XML parsing: **5/5 passed**.
- GitHub Actions YAML parsing: **1/1 passed**.
- Android permissions: only `android.permission.INTERNET`.
- No broad storage permission added; workspaces remain SAF-based.
- Static gates verify M7 UI entry points, engine files, limits, privacy wording, tests, version, and workflow artifact names.

## Safety limits verified in source

- 8,000 project files per audit.
- 700 KB maximum source read per eligible text file.
- 2,000,000 crash-log characters.
- 8,000 dependency edges.
- 600 combined findings.
- 120 duplicate groups.
- Generated/vendor folders excluded.
- Sensitive paths excluded from project-content reads.
- Heuristic dead-code findings never automatically delete code.

## Build limitation

The execution environment does not contain a complete Android SDK/Gradle installation and cannot fetch it, so a genuine local `assembleDebug`/Android Lint run is **not claimed**. The included GitHub Actions workflow runs repository QA, all unit tests, clean APK assembly, and `lintDebug`; it uploads an APK only after those build steps succeed.

## Release archive verification

The final release ZIP is re-extracted into a clean directory. Static QA, the 43-test JVM suite, XML/YAML validation, ZIP CRC verification, and per-file SHA-256 comparison are repeated against the packaged copy before handoff.
