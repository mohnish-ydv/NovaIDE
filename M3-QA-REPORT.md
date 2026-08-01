# NovaIDE M3 QA Report

**Release:** `0.3.0-M3` (`versionCode 3`)  
**Milestone:** Project Workspace  
**Audit date:** July 31, 2026

## Result

Repository-level QA passed. M3 project-management code and all retained M1/M2 Kotlin sources type-check successfully against the local Android API contract stubs. The pure editor and project engines passed executable regression suites. XML, workflow YAML, tests, permissions, versioning and archive structure were rechecked.

## M3 functional coverage

- Project Hub with recent and favorite workspaces.
- Per-workspace open-tab/cursor/scroll sessions.
- Save/discard/cancel protection during workspace switching.
- Bounded recursive project index with source-first generated-folder ordering.
- Detection for Android/Gradle, Flutter, React Native, Next.js, React, Vue, Svelte, Phaser, Node.js, Godot, Python, Rust, Go and static web projects.
- Streamed ZIP import and export.
- ZIP-slip, absolute-path, control-character, path-length, entry-count, per-file and total-size protections.
- Failed-import cleanup request.
- Export self-exclusion when the output ZIP is created inside the workspace.
- Sanitized export entry segments.
- Workspace-wide file-name/content search with case, regex and generated-folder controls.
- Exact result navigation to line and column.
- Four valid project templates with real multiline content.
- Image, media, archive/APK and binary previews.

## Checks performed

### Repository static QA

- Required M1/M2/M3 modules: passed.
- Version code/name and SDK configuration: passed.
- GitHub workflow command/artifact checks: passed.
- Broad/legacy storage permission check: passed.
- Persistable URI flag regression check: passed.
- ZIP safety/limit/cleanup/self-exclusion rules: passed.
- Template completeness and escaped-content regression check: passed.
- Unfinished implementation marker check: passed.
- Generated/local artifact exclusion check: passed.

Static QA result:

- XML files: 5
- Kotlin files: 30
- Kotlin lines: 4,685

### Kotlin source checks

- Full application source type-check with local Android API stubs: passed with zero errors.
- Final changed-source classic compiler pass: passed with zero errors.
- JVM unit-test source type-check: passed with zero errors.

### Executable engine regression

M2 editor engine suite passed:

- normal, whole-word and regex search;
- regex error handling;
- contextual/capture-group replacement;
- Replace All mutation limit;
- symbol extraction;
- syntax tokenization and large-source truncation.

M3 project engine suite passed:

- Android, Flutter, Godot, Next.js, Phaser and Python detection;
- ZIP traversal, absolute-path, control-character and safe-export rules;
- regex workspace search and generated-directory filtering;
- query-length limit;
- template count and real-newline validation.

### Format and packaging checks

- All XML parsed successfully.
- GitHub Actions YAML parsed successfully.
- Static QA Python script compiled successfully.
- Final ZIP CRC/integrity and top-level-folder checks are performed during packaging.

## Build note

A complete Android SDK/Gradle environment is not installed in this sandbox, so a real `assembleDebug` and Android Lint execution could not be run locally. The included GitHub workflow remains the authoritative Android build gate and runs static QA, JVM tests, APK assembly and `lintDebug` before uploading the APK artifact.
