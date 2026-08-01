# Changelog

## 1.0.0-M10 — Universal Runtime and Final Release

- Added framework detection for Vite, React, Vue, Svelte, Astro, Angular, Next.js and Nuxt.
- Added generated output discovery without treating tooling source `index.html` as a build.
- Added Node, npm, pnpm, Yarn, Bun, Python, PHP, Hugo and Jekyll command planning.
- Added user-confirmed Termux `RUN_COMMAND` integration and bounded result capture.
- Added shared-storage path validation, private result receiver and package visibility.
- Added explicit loopback development-server preview.
- Added Markdown and strict Mermaid preview.
- Added PWA service-worker interception and retained WebAssembly MIME support.
- Added M10 regression tests and final repository security gates.
- Updated version to `1.0.0-M10` / code 10.

## 0.9.0-M9

- Added a direct toolbar Run button and `Ctrl+Shift+R` shortcut for browser-ready HTML workspaces.
- Added an isolated in-app WebView runtime served from the synthetic HTTPS origin `nova.local` with relative CSS, JavaScript, image, font, media, module and root-absolute asset resolution.
- Added split editor/preview layout with portrait vertical split, landscape/tablet side-by-side split, and content-area fullscreen preview.
- Added live reload from unsaved open editor buffers with a bounded debounce, so static web changes can be reviewed before disk autosave completes.
- Added responsive, mobile and desktop viewport profiles, navigation controls, reload, entry-point selection and external document handoff.
- Added JavaScript console capture, uncaught error and promise-rejection instrumentation, HTTP/resource diagnostics, and a bounded 300-entry console buffer.
- Added Web Preview settings for JavaScript, live reload, external HTTPS resources and SPA fallback.
- Added build-tool awareness for Vite, React, Next, Angular, Webpack, Astro, Svelte and Nuxt source projects, with clear boundaries when transpilation/build output is required.
- Added preview security gates: no file/content WebView access, no JavaScript bridge, no WebView debugging, no cookies, no mixed HTTP content, traversal rejection, sensitive-file blocking, resource-size limits and external network disabled by default.
- Added 9 M9 JVM regression tests and expanded repository static/security QA to 61 tests.

## 0.8.0-M8

- Added a fuzzy mobile command palette with built-in, task and extension commands plus `Ctrl+Shift+P`.
- Added a declarative extension manifest parser with bounded JSON input, duplicate-key rejection, command limits and versioned metadata.
- Added explicit extension permissions for workspace reads, editor writes, HTTPS links and clipboard writes.
- Added a sandbox policy that blocks undeclared capabilities, insecure URLs, oversized payloads and arbitrary APK/JAR execution.
- Added extension import, paste-review, permission confirmation, enable/disable, uninstall and command execution UI.
- Added Nova Console, a safe workspace-aware console with bounded `ls`, `find`, `grep`, `cat`, `head`, `tail`, `wc`, `hash`, `base64`, `project-info`, `echo`, `pwd`, `help` and `clear` commands.
- Added project, active-file and selection variables without exposing an unrestricted Android shell.
- Added built-in tasks and user-created multi-command tasks/workflows with allow-list validation and stop-on-failure behavior.
- Added app-private preference storage for installed manifest text and saved task definitions; credentials remain isolated in the existing Android Keystore vault.
- Added 9 M8 JVM regression tests and expanded static/security QA.

## 0.7.0-M7

- Added the local Debug & Analysis Center with a full project health score and category reports.
- Added deep crash/ANR trace parsing, caused-by root-cause selection, stable fingerprints, secret redaction, project-frame resolution, and direct file/line navigation.
- Added imported text/JSON/HTML/binary/ZIP diagnostic-log support using bounded existing log readers.
- Added exact duplicate-file and normalized repeated-code-block detection.
- Added conservative unused private symbol, unused import, unreachable statement, and Android resource checks.
- Added project dependency graph mapping, hub/orphan reporting, and strongly connected cycle detection.
- Added mobile performance diagnostics for long files/functions, deep nesting, blocking calls, whole-file I/O, nested loops, repeated allocation patterns, and large image assets.
- Added security diagnostics for embedded secrets, cleartext endpoints, weak crypto, AES/ECB, TLS bypass, unsafe WebViews, wildcard CORS, dynamic execution, exported Android components, and dynamic dependencies.
- Added confidence labels, non-destructive review guidance, report copy, generated/vendor folder exclusions, and mobile analysis caps.
- Added 9 M7 JVM regression tests and expanded static/security QA.

## 0.6.0-M6

- Added one encrypted Credentials Center for GitHub, GitLab, OpenAI, Gemini, Groq, OpenRouter and custom HTTPS OpenAI-compatible providers.
- Added direct official token/API-key creation links, permission guidance, test-before-save, model discovery, masked state and safe removal.
- Added provider-neutral AI project chat, explain, generate, fix, refactor, error analysis, security review and performance review.
- Added bounded project context, sensitive-path exclusion, secret redaction, stale-file checks and previewable safe multi-file AI patches.
- Added fully offline autocomplete, snippets, local lint, regex quick fixes and static project analysis.
- Fixed commits to brand-new empty GitHub repositories by creating an orphan tree, first commit and branch ref without requiring a nonexistent HEAD/base tree.
- Added permission-aware GitHub and GitLab errors and preserved prior working credentials when verification fails.
- Added M6 tests and expanded repository security/static QA.

## 0.5.0-M5

- Added the Android Tools center with project, module, SDK, dependency and manifest inspection.
- Added safe manifest permission listing, insertion and removal with unsaved-buffer protection.
- Added Android resource health reports for qualifiers, invalid names, duplicates, size and bitmap density risks.
- Added module-aware Gradle command generation for APKs, tests, Lint, verification, dependencies and signing.
- Added bounded APK structure inspection and imported build/crash log diagnosis.
- Fixed GitHub setup for repositories whose default branch is not `main`.
- Added actual default-branch discovery, explicit-branch validation and save-after-verification behavior.
- Added copied-token normalization and migration compatibility for M4 tokens containing `Bearer`, `token` or `Authorization:` prefixes.
- Added clearer GitHub authentication, repository access, DNS, timeout and TLS diagnostics.
- Added common copied GitHub URL support including `www.github.com`, scheme-less URLs and `/tree/<branch>` links.
- Hardened ZIP log aggregate memory, nested/root Gradle task paths and explicit-branch behavior.
- Added M5 Android Suite tests and expanded repository static QA.
## 0.4.0-M4

- Added encrypted GitHub repository connection with Android Keystore protected fine-grained tokens.
- Added local snapshot-based status with added/modified/deleted tracking and bounded unified text diffs.
- Added real GitHub Git Data API commit-and-push with remote-head divergence protection.
- Added clone/pull archive application, tracked-file deletion handling and branch switching.
- Added commit history, workflow-run status and authenticated artifact download to user-selected storage.
- Added merge-marker scanning and Current/Incoming/Both conflict resolution.
- Added automatic `.git/config` remote detection for existing Termux/Git repositories.
- Added archive, file-count and upload safety limits for phone memory/storage protection.
- Added M4 pure-Kotlin regression tests and expanded repository static QA.

## 0.3.0-M3

- Added recent/favorite Project Hub with per-workspace sessions and unsaved-switch protection.
- Added bounded recursive workspace indexing and multi-framework project detection.
- Added streamed ZIP import/export with traversal, entry and size protections.
- Added workspace-wide file/content search with regex, case and generated-folder controls.
- Added Modern Web, Phaser, Python and Node project templates.
- Added sampled image, media, archive/APK and binary previews.
- Added exact search-result navigation without fixed timing delays.
- Hardened failed-import cleanup, dirty-buffer export ordering, export self-exclusion, safe archive names and inaccessible-folder scanning.
- Retained all M1 and M2 editor functionality.

## 0.2.0-M2

- Fixed Android Lint `WrongConstant` failure in persisted workspace permission handling.
- Added professional editing systems: syntax highlighting, find/replace, minimap, symbols, folding, bracket matching and synchronized multi-occurrence editing.
- Added automatic large-file performance mode and bounded editor-analysis work.
- Added context-aware regex replacement and atomic Replace All safety above 5,000 matches.
- Fixed session restore fallback when the previously active document is no longer accessible.
- Increased restored tab limit from 12 to 20.
- Replaced timer-based Save All + Close behavior with write-completion tracking.
- Added JVM unit tests and made GitHub Actions run tests before APK assembly and lint.

## 0.1.0-M1

- Initial workspace, file explorer, tabs, editor, autosave, session restore and themes.
