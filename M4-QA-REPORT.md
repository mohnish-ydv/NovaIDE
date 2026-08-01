# NovaIDE M4 QA Report

**Release:** `0.4.0-M4` (`versionCode 4`)  
**Scope:** Git + GitHub integration over the complete M1–M3 source tree.

## Result

Release-candidate repository QA passed.

## Automated verification

- Repository static QA: **passed**
- Kotlin source inventory: **40 files / 6,599 lines**
- XML parsing: **5/5 files passed**
- GitHub Actions workflow YAML syntax: **passed**
- M2 editor regression tests: **7/7 passed**
- M3 project/workspace regression tests: **5/5 passed**
- M4 Git core regression tests: **5/5 passed**
- Total executed regression tests: **17/17 passed**
- M4 Git/GitHub production source type-check against Android/JVM API stubs: **passed with zero source errors**
- MainActivity M4 integration-region type-check: **zero M4 diagnostics**
- Secret/token literal scan: **passed**
- Unfinished implementation marker scan: **passed**
- Generated/local build-file scan: **passed**
- Android permission audit: only `android.permission.INTERNET`

## M4 safety gates verified

- GitHub token encryption uses Android Keystore with AES-GCM.
- Authorization is attached only to `api.github.com`; redirected downloads do not receive the token.
- Commit & Push checks the expected remote head and never force-updates a branch.
- Commit limits: 200 changed files, 25 MB per file, 50 MB total uploaded content.
- Pull archive limits: 12,000 entries, 40 MB per file, 700 MB expanded data, 300 MB compressed download.
- Artifact download limit: 1 GB.
- ZIP traversal, absolute path, control-character, duplicate-path, multiple-root and file/folder type-conflict checks are active.
- Workspace switching is blocked while a mutating Git operation is running.
- Pull and baseline creation wait for dirty editor files to finish saving.
- Existing `.git` metadata is never modified and generated dependency/build folders are excluded from NovaIDE snapshots.

## Packaging verification

The final archive is re-extracted before delivery. Static QA, ZIP integrity, top-level folder structure and per-file SHA-256 equality are checked against the release source tree.

## Environment note

A complete Android SDK is not installed in this execution environment, so a genuine local `assembleDebug` and Android Lint run could not be performed here. The included GitHub Actions workflow is the authoritative Android gate and runs, in order:

1. repository static QA;
2. all JVM unit tests;
3. clean debug APK assembly;
4. Android Lint;
5. APK and lint-report artifact upload.
