# NovaIDE M2 QA Report

## Supplied build-log diagnosis

The M1 GitHub run completed `:app:assembleDebug` successfully. The run failed afterward in `:app:lintDebug` with one blocking `WrongConstant` error at `takePersistableUriPermission()`. A dynamically masked integer was being passed where Android Lint requires explicit read/write grant constants.

## Fix applied

Workspace permission persistence now checks the returned read/write grants and passes only one of these explicit values:

- `Intent.FLAG_GRANT_READ_URI_PERMISSION`
- `Intent.FLAG_GRANT_WRITE_URI_PERMISSION`
- their explicit read/write combination

The error is fixed in code and is not hidden with `@SuppressLint`.

## Additional correctness fixes found during M2 audit

- Save All + Close waits for actual write completion instead of a fixed timer.
- Queued file writes are allowed to finish during activity shutdown.
- Restored sessions activate the last valid tab when the old active document is unavailable.
- Stale syntax and search results are rejected after tab/content changes.
- Regex replacement supports contextual patterns and capture groups.
- Replace All aborts without modifying the document when the 5,000-match safety limit is exceeded.
- Minimap bounds and accessibility click handling were hardened.
- Strings/comments receive lexical priority so comment markers inside strings are not misclassified.

## Validation completed

- Repository static QA: passed
- Android XML parsing: 5/5 files passed
- Workflow YAML parse and required-step inspection: passed
- Main Android-facing Kotlin source type-check against local API stubs: passed with zero diagnostics
- JUnit test-source type-check: passed
- Executed pure editor-core regression runner: passed
- Search, whole-word, invalid-regex and replacement tests: passed
- Contextual regex and capture-group replacement tests: passed
- Replace All safety-limit test: passed
- Syntax token, language and symbol extraction tests: passed
- Large-file tokenizer truncation test: passed
- Broad/unnecessary storage-permission check: passed
- Unfinished `TODO()`/`NotImplementedError` marker check: passed
- Generated/local build-file exclusion check: passed

## Repository totals

- Files: 41
- Kotlin files: 20
- Kotlin lines: 3419
- App version: `0.2.0-M2` (version code 2)

## Build note

This sandbox does not contain a complete Android SDK/Gradle installation, so a genuine local APK assembly and Android Lint execution could not be rerun here. The supplied M1 log proves the original source reached successful APK assembly before the isolated Lint failure. The included GitHub workflow is the authoritative final check and runs static QA, JVM unit tests, clean APK assembly and Android Lint before uploading the APK artifact.
