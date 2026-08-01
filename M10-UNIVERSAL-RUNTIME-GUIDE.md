# NovaIDE M10 Universal Runtime Guide

NovaIDE 1.0.0-M10 combines its secure in-app Web Preview with an explicit Termux bridge. It does **not** silently install packages, execute a shell, or invent access to private/cloud folders.

## What runs directly in NovaIDE

The Run button can directly preview browser-ready HTML/CSS/JavaScript, static Phaser/Three.js/PixiJS/Babylon.js builds, PWA assets, WebAssembly resources, Markdown, Mermaid, and generated framework output such as `dist/`, `build/`, `out/`, `public/`, `_site/`, or `.output/public/`.

Unsaved open HTML/CSS/JS buffers are supplied to the preview server. WebView file/content access, mixed content, cookies, debugging, unsafe navigation schemes, traversal, sensitive files, and oversized resources remain blocked.

## Framework detection

NovaIDE detects Vite, React, Vue, Svelte/SvelteKit, Astro, Angular, Next.js, Nuxt, Node.js, Python, PHP, Hugo, Jekyll, static websites, Markdown, and Mermaid. A build-tool source project is never mistaken for a generated static output merely because it contains a root `index.html`.

Package-manager selection follows lockfiles: Bun, pnpm, Yarn, then npm. Commands are derived from `package.json` scripts and displayed before execution.

## Termux bridge setup

1. Install the official Termux app.
2. Open NovaIDE **Universal Runtime → Termux setup & permissions**.
3. Grant NovaIDE the additional `RUN_COMMAND` permission in Android App Info.
4. In Termux, enable `allow-external-apps=true` in `~/.termux/termux.properties`, then restart Termux.
5. Run `termux-setup-storage` when Termux needs shared-storage access.
6. Open the project from shared device storage such as Download or Documents.

NovaIDE resolves only Android's external-storage document provider to a real `/storage/...` path. Cloud providers, virtual documents, and unknown SAF authorities are intentionally refused.

## Supported Termux commands

The allowlist supports npm, npx, pnpm, Yarn, Bun, Node.js, Python, PHP, Hugo, Bundler, and Jekyll. Maximum argument count and size are bounded; line-control characters and destructive commands are blocked. Every command, argument list, description, and working directory is shown in a confirmation dialog.

Command output is returned through a private, non-exported broadcast receiver and stored in app-private preferences with bounded stdout, stderr, and error sizes.

## Local development servers

For commands that start a server, NovaIDE can preview an explicitly selected loopback origin such as `http://127.0.0.1:5173`. Only localhost, `127.0.0.1`, or `::1` with the exact approved port are accepted. Other HTTP origins, credentials in URLs, fragments, and unsafe schemes are blocked.

## Important limits

- NovaIDE does not bundle Node, Python, PHP, or framework compilers.
- Termux must already have the requested runtime/packages installed.
- Next.js server-only features cannot become a static preview; use a local development server or static export.
- Mermaid loads its renderer from HTTPS only when external network access is enabled in Web Preview settings.
- A WebView preview is not a production hosting environment.
