# NovaIDE M8 — Plugins & Productivity Guide

## Command Palette

Open **More → Command Palette** or press **Ctrl+Shift+P**. Search by title, category, description, keyword, or an ordered fuzzy abbreviation such as `prj hlth`.

The palette contains:

- core NovaIDE commands,
- enabled extension commands,
- built-in and user-created tasks.

## Install an extension

Open **More → Extensions** and choose **Import manifest** or **Paste manifest**. NovaIDE parses the manifest locally and displays every requested permission before installation.

Example `.nova-plugin.json`:

```json
{
  "id": "dev.example.todo-tools",
  "name": "TODO Tools",
  "version": "1.0.0",
  "author": "Example Developer",
  "description": "Find TODOs and insert a review marker.",
  "permissions": ["READ_WORKSPACE", "EDITOR_WRITE"],
  "commands": [
    {
      "id": "find-todos",
      "title": "Find TODOs",
      "description": "Search indexed text files for TODO markers.",
      "action": "CONSOLE",
      "value": "grep -i TODO",
      "keywords": ["todo", "scan"]
    },
    {
      "id": "insert-review",
      "title": "Insert review marker",
      "action": "INSERT",
      "value": "// REVIEW: "
    }
  ]
}
```

### Permissions

- `READ_WORKSPACE` — run a safe console command against the bounded workspace snapshot.
- `EDITOR_WRITE` — insert declared text into the active writable editor tab.
- `OPEN_EXTERNAL` — open an explicitly declared HTTPS page.
- `CLIPBOARD_WRITE` — copy declared text after the user runs the command.

### Actions

- `CONSOLE`
- `INSERT`
- `OPEN_URL`
- `MESSAGE`
- `COPY`

A command must declare the matching permission. HTTP links, credential-bearing URLs, duplicate command IDs, unknown permissions/actions, oversized payloads, and malformed JSON are rejected.

## Nova Console

Open **More → Tasks & Nova Console → Open Nova Console**.

Supported commands:

```text
help
pwd
ls [path]
find <name>
grep [-i] <text> [path]
cat <path>
head <path> [lines]
tail <path> [lines]
wc <path>
hash <path>
base64 <text>
project-info
echo <text>
clear
```

Variables:

```text
${project}
${file}
${selection}
```

Nova Console is intentionally not a Termux shell. It cannot execute binaries, pipes, redirects, package managers, `rm`, or arbitrary system commands.

## Tasks and workflows

Open **Tasks & Nova Console → Create saved task / workflow**. Enter:

- a lowercase ID such as `my.precommit.check`,
- a name and description,
- one supported Nova Console command per line.

A task may contain up to 20 commands. It stops immediately when a command fails. Long-press a custom task to delete it. Built-in tasks are immutable.
