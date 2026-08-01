# NovaIDE

NovaIDE is a native, mobile-first Android IDE designed for serious development directly from a phone. **M10** completes the ten-milestone roadmap with a Universal Runtime Engine that combines secure in-app browser preview, framework/build-output detection, Markdown/Mermaid rendering, and an explicit user-approved Termux bridge.

**Package:** `com.mohnishraj.novaide`  
**Version:** `1.0.0-M10` (`versionCode 10`)  
**Minimum Android:** 8.0 / API 26  
**Target / compile SDK:** 35  
**Developer:** Mohnish Raj

## Universal Run

Press **▶ Run** or `Ctrl+Shift+R`. NovaIDE can directly preview:

- HTML, CSS and browser JavaScript
- Static Phaser, Three.js, PixiJS and Babylon.js projects
- PWA resources and WebAssembly files
- Generated `dist/`, `build/`, `out/`, `public/`, `_site/` and `.output/public/` output
- Markdown and Mermaid documents

NovaIDE detects Vite, React, Vue, Svelte/SvelteKit, Astro, Angular, Next.js, Nuxt, Node.js, Python, PHP, Hugo and Jekyll. Source projects that still need compilation are sent to the Universal Runtime Center instead of being falsely presented as runnable static output.

## Termux bridge

For Node/npm/pnpm/Yarn/Bun, Python, PHP, Hugo and Jekyll workflows, NovaIDE can send a bounded command to the official Termux `RUN_COMMAND` service. This requires:

- Termux installed
- NovaIDE's additional `com.termux.permission.RUN_COMMAND` permission granted
- `allow-external-apps=true` enabled in Termux
- A workspace under real shared device storage
- The needed runtime/packages already installed in Termux

Every exact executable, argument list and working directory is shown before execution. NovaIDE has no unrestricted shell button, does not silently install dependencies, and does not execute commands through `Runtime.exec` or `ProcessBuilder`.

## Web Preview

- Isolated `https://nova.local/` project origin
- Split editor/preview and fullscreen content view
- Unsaved HTML/CSS/JS buffer overrides
- Live reload
- Console, runtime and failed-resource diagnostics
- Responsive, mobile and desktop viewports
- SPA fallback
- Service-worker request interception for PWA testing
- Explicit loopback preview for Termux servers

Only the approved `localhost`, `127.0.0.1` or `::1` origin and port may be opened for a local runtime server.

## Security boundaries

- Workspaces use Android Storage Access Framework.
- No broad storage, package-install, log-reading or all-package query permission.
- WebView file/content access, debugging, cookies and mixed content are disabled.
- No JavaScript bridge.
- Sensitive files, traversal and oversized resources are blocked.
- Termux commands are allowlisted, size-bounded and individually confirmed.
- The Termux result receiver is private and non-exported.
- Git and AI secrets remain encrypted with Android Keystore.
- Plugins remain declarative and cannot load arbitrary executable code.
- Diagnostic dead-code findings remain advisory and never auto-delete code.

## Retained M1–M9 systems

NovaIDE includes the professional editor, ZIP/project workspace tools, GitHub/GitLab workflows, empty-repository initial commits, Android project suite, multi-provider AI, offline autocomplete/snippets/lint/fixes, crash and project analysis, permission-sandboxed extensions, command palette, Nova Console, saved tasks, and secure Web Preview developed across M1–M9.

## Build and verification

GitHub Actions runs static/security QA, all JVM tests, clean debug APK assembly and Android Lint before uploading:

- **NovaIDE-M10-Debug-APK**
- **NovaIDE-M10-Lint-Report**

See [`M10-UNIVERSAL-RUNTIME-GUIDE.md`](M10-UNIVERSAL-RUNTIME-GUIDE.md), [`M10-QA-REPORT.md`](M10-QA-REPORT.md), and [`TERMUX-COMMANDS.md`](TERMUX-COMMANDS.md).
