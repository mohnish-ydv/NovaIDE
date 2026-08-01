# NovaIDE M4 — Git + GitHub Guide

## Open GitHub Center

Open a workspace, then use **⋮ → Git & GitHub** or a hardware keyboard with **Ctrl + Shift + G**.

## Connect a repository

Enter one of:

- `owner/repository`
- `https://github.com/owner/repository.git`
- `git@github.com:owner/repository.git`

Set the branch. A token is optional for public read operations but required for push and artifact downloads. NovaIDE encrypts it through Android Keystore; clearing app data removes it.

When first connected:

- Choose **Pull / Clone** for the safest, accurate remote baseline.
- Choose **Trust Current** only when the workspace already exactly matches the branch, such as a repository previously cloned and synchronized in Termux.

## Status and diff

**Status & Diff** saves open dirty buffers, scans the workspace, then reports:

- `A` — added
- `M` — modified
- `D` — deleted

Tap a text change to view a unified diff. Baseline text is capped at 512 KB per file and 12 MB total; larger or binary files show metadata only.

## Commit and push

1. Review Status & Diff.
2. Choose **Commit & Push**.
3. Enter a commit message.
4. NovaIDE checks the remote head, uploads changed blobs, creates a tree/commit and advances the branch.

Push is blocked when the remote head changed since the baseline. Pull first, resolve any conflicts and retry. The branch update is non-force.

## Pull / clone

Pull requires saved editor buffers. When local tracked changes exist, NovaIDE warns before overwrite. Untracked unrelated files are preserved. The remote archive is validated before application.

Safety limits:

- 12,000 archive entries.
- 40 MB per archive file.
- 700 MB total uncompressed archive data.
- Workspace scan depth 60 for Git operations.

A failed apply can leave some files updated because Android SAF does not provide atomic folder transactions. The downloaded remote can be pulled again after fixing storage access or space problems.

## Branches

Open **Branches**, tap a branch and NovaIDE downloads it. A switch is blocked when tracked local changes exist. Protected branches are labeled.

M4 lists and switches existing branches; branch creation, rebase and cherry-pick remain outside this milestone.

## History

History shows the latest commits for the active branch. Tap one for author/date details or to open the commit in a browser.

## GitHub Actions

Actions lists recent runs for the active branch. Tap a run to view its status and artifacts. Artifact ZIPs require a token and are saved to a location selected through Android's document picker.

## Merge helper

Use **Merge Helper** to scan text files for standard conflict markers. For each block select:

- **Use Current**
- **Use Incoming**
- **Keep Both**

NovaIDE resolves one block at a time and continues until the file is clean.

## Security model

- No broad external-storage permission.
- `INTERNET` is the only new Android permission in M4.
- Tokens are AES-GCM encrypted with a non-exportable Android Keystore key.
- Auth headers are sent only to `api.github.com`; signed artifact/archive redirects do not receive the token.
- Tokens are never exported with workspace ZIPs.

## Known boundaries

M4 is a GitHub-native mobile workflow, not a bundled native `git` executable. It does not create local `.git` objects, run arbitrary Git hooks, support submodules, preserve executable file modes during API commits, or perform interactive rebases. Existing `.git` folders remain untouched and are excluded from NovaIDE snapshots.
