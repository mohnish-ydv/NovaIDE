# NovaIDE M10 Architecture

## Retained runtime layers

M1–M7 remain intact: Storage Access Framework workspaces, serialized I/O executors, professional editor engines, project indexing, ZIP safety, GitHub/GitLab clients, Android tooling, Android Keystore credentials, provider-neutral AI, deterministic offline intelligence, and local diagnostics.

## M8 extension layer

The `plugins` package contains:

- `MiniJson` — a bounded JSON reader with 128 KB input, depth/item limits, duplicate-key rejection, escape validation, and object-root enforcement.
- `PluginManifestParser` — validates identifiers, metadata, command counts, payload sizes, unique command IDs, known permissions, and supported action types.
- `PluginPolicy` — maps each action to an explicit permission and blocks undeclared capabilities, insecure links, credential-bearing URLs, oversized values, and disabled plugins.
- `PluginStore` — keeps reviewed manifest text and enabled state in app-private preferences. It does not store or expose Credentials Center secrets.

Extensions are declarative. No class loading, reflection-based plugin execution, DEX/JAR/APK loading, shell execution, or credential APIs are exposed.

## M8 productivity layer

The `productivity` package contains:

- `CommandPaletteEngine` — bounded exact, prefix, substring, keyword, and ordered fuzzy matching.
- `NovaConsoleEngine` — a pure Kotlin safe command interpreter over an immutable `ConsoleContext` snapshot.
- `TaskRunner` — validates an allow-listed sequence of up to 20 commands and stops on failure.
- `ProductivityStore` — app-private persistence for user-created tasks/workflows.

`MainActivity` builds console snapshots on the serialized project executor. It indexes no more than 2,000 files, reads at most 700 likely-text files, caps each read at 128 KB, and enforces a 6 MB total text budget. Only the immutable result returns to the UI.

## UI orchestration

- **Command Palette** merges built-in actions, enabled extension commands, and saved tasks.
- **Extensions Center** imports or accepts pasted manifests, shows requested permissions, and requires explicit installation.
- **Tasks & Nova Console** exposes built-in tasks, custom workflows, output copy, and the interactive safe console.
- Plugin actions run only after a direct user tap or command-palette selection.

## Safety boundaries

- Only HTTPS external links are accepted.
- Parent path segments are rejected by the console.
- Console commands are allow-listed; `rm`, shell pipes, redirects, package installers, subprocesses, and native executables are unsupported.
- Extension permission checks occur both at parse time and execution-plan time.
- Editor insertion is limited to the active writable tab.
- Console output, matches, input, manifest size, command count, payload size, file count, and text budgets are bounded.
- Credentials remain in the separate Android Keystore-backed vault and are not part of plugin or console contexts.

## Deliberate M8 boundaries

M8 does not claim VS Code binary-extension compatibility, arbitrary language-server hosting, an unrestricted terminal, background scheduled workflows, or native debugger plugins. These are deferred because they require stronger process isolation and a larger Android attack surface.


## M9 Web Preview runtime

`WebPreviewEngine` is a pure Kotlin planner and safety layer. It ranks HTML entry points, distinguishes static/build-output/tooling-source workspaces, normalizes URL paths, identifies sensitive files, decides safe SPA fallbacks, maps browser MIME types, constructs the synthetic `https://nova.local/` URL, and injects bounded runtime diagnostics.

`WorkspaceWebServer` is an in-process WebView request interceptor rather than a listening localhost socket. It maps normalized workspace-relative paths to SAF document URIs, serves open-editor text overrides, streams bounded binary resources, injects HTML diagnostics, rejects traversal/sensitive paths, and blocks external network requests unless the user explicitly enables them.

`WebPreviewSettingsStore` keeps non-secret preview preferences in app-private storage. `WebConsoleBuffer` caps and deduplicates browser/runtime diagnostics. `MainActivity` owns the WebView lifecycle, split/fullscreen layout, entry selection, viewport profiles, reload debounce, external handoff, console UI and workspace-buffer synchronization.

### WebView hardening

- synthetic HTTPS origin; no localhost port or broad filesystem mapping
- `allowFileAccess = false` and `allowContentAccess = false`
- no JavaScript interface bridge
- WebView debugging disabled
- mixed HTTP content blocked
- cookies and third-party cookies disabled
- external resources disabled by default
- `.env`, credentials, keys, keystores and internal tool folders blocked
- 6 MB text and 100 MB individual resource limits

### Deliberate M9 boundary

The runtime serves browser-ready output. It does not execute Node/npm, language servers, TypeScript/JSX transforms or framework build pipelines. Generated `dist`, `build` and `out` entries are preferred; unbuilt tool projects receive an explicit warning.

## M10 Universal Runtime

`runtime/UniversalRuntimeEngine` is a pure deterministic classifier. It reads a bounded subset of `package.json`, lockfiles and project paths, then returns a `RuntimeProject`; it never executes code. Generated browser output is separated from tooling source so a Vite/React root `index.html` is not mistaken for a completed build.

`runtime/TermuxCommandPolicy` provides the executable allowlist, argument bounds, shell quoting and port validation. `runtime/TermuxBridge` is the only Android integration point and sends a command through Termux's `RUN_COMMAND` service after explicit confirmation. It accepts only a real shared-storage path resolved by `SharedWorkspacePathResolver` and returns output through the private `TermuxResultReceiver`.

`DocumentPreviewGenerator` creates escaped local Markdown pages and strict Mermaid pages. `WorkspaceWebServer` retains the M9 synthetic origin and additionally permits only one explicitly selected loopback origin for development servers. No direct native process execution is present in NovaIDE.
