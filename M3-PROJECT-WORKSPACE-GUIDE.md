# NovaIDE M3 Project Workspace Guide

## Project Hub

Tap the workspace name in the top bar or choose **Project hub** from the menu. The hub shows the detected project type, index statistics, recent projects and favorite controls. Tap a recent project to reopen it. Long-press or tap the star to change favorite status.

NovaIDE keeps open-tab, cursor and scroll state separately for each workspace. A switch cannot silently discard dirty files: choose **Save & switch**, **Discard & switch** or **Cancel**.

## ZIP import

1. Open the folder that should receive the project.
2. Choose **Import ZIP**.
3. Select an archive.

NovaIDE creates a new subfolder from the archive name. It rejects absolute/traversal paths, more than 10,000 entries, more than 128 MB in one extracted file or more than 512 MB total. A failed extraction removes its partial project folder.

## ZIP export

Choose **Export workspace ZIP**, select a destination and confirm the suggested file name. NovaIDE saves current dirty editor buffers before the archive is streamed. If the destination is inside the current workspace, the output ZIP excludes itself. Exported entry names are sanitized against traversal-style segments.

## Workspace search

Choose **Search workspace** or press `Ctrl+Shift+F` on a hardware keyboard. Available options:

- **Match case**
- **Regular expression**
- **Include generated folders**

By default, expensive generated directories are skipped. Tap a result to open its file at the reported line and column.

## Templates

Open **New project from template**, select a starter and enter the destination folder name. Current starters are Modern Web App, Phaser Game, Python Utility and Node.js CLI.

## Resource previews

Tap a non-text file in the file tree. Images are sampled for memory safety. Audio/video files show metadata, ZIP/APK files show a bounded contents summary, and other binaries show metadata plus their first bytes.

## Current boundaries

- Workspace index: 8,000 entries.
- Editable text file: 2 MB.
- Search content read: 768 KB per file.
- Workspace search results: 500.
- ZIP streamed content: 512 MB.
