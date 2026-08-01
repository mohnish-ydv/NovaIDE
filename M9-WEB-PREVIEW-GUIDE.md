# NovaIDE M9 — Web Preview Guide

## Starting a preview

1. Open a workspace containing an HTML file.
2. Open the HTML file when you want that exact file to be the entry point.
3. Tap **▶** in the main toolbar or press **Ctrl+Shift+R**.
4. NovaIDE opens editor and preview together. On a small portrait screen they are stacked; on wider screens they are side by side.

You can also long-press an HTML file and choose **Run in Web Preview**, or open **More → Web Preview → Choose HTML entry point**.

## Files that run directly

M9 serves browser-ready:

- HTML and HTM
- CSS
- JavaScript, MJS and CJS when the browser can execute them
- JSON, source maps and web manifests
- SVG, PNG, JPEG, GIF, WebP and icons
- WOFF/WOFF2/TTF/OTF fonts
- MP3, WAV, OGG, MP4 and WebM media

Relative references such as `./styles.css`, `../assets/logo.png`, ES-module imports and root references such as `/assets/app.js` resolve inside the selected workspace.

## Live reload

Open web files are served from NovaIDE's current editor buffers. When live reload is enabled, editing an open HTML/CSS/JS/SVG/JSON file schedules one bounded refresh after 650 ms. This means the preview can show unsaved changes before the normal file autosave completes.

## Console

Tap **≡** in the preview toolbar or open **More → Web Preview → View console**. NovaIDE records:

- `console.log`, warning and error messages
- uncaught JavaScript errors
- unhandled promise rejections
- missing local resources
- blocked external requests
- HTTP and WebView loading errors

The buffer keeps at most 300 deduplicated entries and truncates oversized messages.

## Viewport modes

Tap **▣**:

- **Responsive** — normal mobile user agent with wide-page fitting.
- **Mobile** — phone-oriented layout without desktop overview scaling.
- **Desktop** — desktop Chromium-style user agent and wide-page fitting.

This is a practical responsive-testing switch, not full Chrome DevTools device emulation.

## Security settings

The Web Preview Center contains four controls:

- **Enable JavaScript** — on by default; turn off to inspect static HTML safely.
- **Live reload** — on by default.
- **Allow external CDN/API resources** — off by default. When enabled, HTTPS subresources may access the internet. Top-level external navigation still opens outside NovaIDE.
- **SPA fallback** — sends safe extensionless in-app routes back to the selected HTML entry.

NovaIDE never serves `.env`, credentials, keystores, private keys, `.git`, `.gradle`, `.idea`, secrets folders or parent-traversal paths. File/content WebView access, mixed HTTP content, cookies, JavaScript bridges and WebView debugging remain disabled.

## React, Vite, Next and other tool projects

M9 is not a Node/npm runtime. It cannot transpile TypeScript/JSX or execute a bundler. NovaIDE:

1. prioritizes generated `dist/index.html`, `build/index.html` or `out/index.html`;
2. warns when a source project appears to require Vite/Next/Angular/Webpack/Astro/Svelte/Nuxt;
3. allows an existing browser-ready HTML entry to be tried manually;
4. clearly reports when no runnable HTML output exists.

Use GitHub Actions, another build environment, or a generated static export, then preview the output folder in NovaIDE.

## External browser button

The **↗** button passes the selected HTML document URI to an installed external app with temporary read permission. Some browsers cannot resolve sibling SAF files, so NovaIDE's internal preview is the authoritative full-project preview.
