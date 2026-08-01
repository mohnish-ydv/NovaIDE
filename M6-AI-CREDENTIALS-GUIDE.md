# NovaIDE M6 — AI, Credentials and Local Intelligence Guide

## 1. Credentials Center

Open the main menu and choose **Credentials Center**. Every secret is handled by a shared encrypted vault backed by Android Keystore and AES-GCM.

### GitHub

1. Tap **Create GitHub API token** to open GitHub's official fine-grained-token page.
2. Limit repository access to only the repositories NovaIDE should manage.
3. For the full M4/M6 workflow, enable:
   - Metadata: read
   - Contents: read and write
   - Actions: read
   - Workflows: write only when NovaIDE must modify files under `.github/workflows`
4. Paste the token and use **Save & Verify**.

NovaIDE saves the token only after `/user` verification succeeds. Repository-specific write access is checked again before commit/push. A token can be valid but still lack access to a particular private repository or lack push rights.

### GitLab

1. Choose GitLab in Credentials Center.
2. Keep `https://gitlab.com` or enter the clean HTTPS origin of a self-managed instance.
3. Tap **Create token on GitLab**.
4. Choose `read_api` for inspection-only access or `api` for complete repository API read/write access.
5. Paste the token and use **Save & Verify**.

NovaIDE verifies the authenticated identity and asks GitLab for the token's own scope metadata when supported. If the server does not report scope metadata, later operations still fail with a specific permission message instead of a generic connection failure.

### AI providers

Choose OpenAI, Gemini, Groq, OpenRouter, or Custom AI:

1. Tap the provider's official **Create API key** button.
2. Enter or retain the HTTPS API base URL.
3. Tap **Fetch available models** to verify the key and choose a model.
4. Enable **Use as active AI provider** when appropriate.
5. Use **Save & Test**.

A failed test does not overwrite the previous working secret or settings. Custom endpoints must use HTTPS and the key is optional.

## 2. AI Assistant

Open **AI Assistant** or press `Ctrl + Shift + I` with a hardware keyboard.

Available tasks include project questions, explanation, generation, bug fixing, refactoring, error analysis, security review, and performance review. For code-focused actions, NovaIDE captures the active file, selection, cursor context, and a bounded project snapshot.

### Context privacy

NovaIDE excludes or redacts likely secrets before transmitting context. Excluded paths include common environment files, local Gradle properties, signing keys, private keys, cloud service credential files, `.git`, credential folders, and other high-risk paths. This is a safety layer, not a guarantee that every custom secret format can be recognized; review provider-bound context and never intentionally place secrets in source code.

### Applying AI output

- **Copy:** copies the response without modifying the project.
- **Apply code:** previews and replaces the captured selection or current file only if it has not changed since the request began.
- **Apply files:** parses `:::nova-file` blocks, previews every target, then applies only safe workspace-relative paths.

Dirty open target files, traversal paths, absolute paths, duplicate paths, and malformed blocks are rejected.

## 3. Offline Local Intelligence

Open **Local Intelligence** for:

- **Autocomplete:** language keywords, project symbols, and nearby identifiers. Shortcut: `Ctrl + Space`.
- **Insert snippet:** language-specific templates with stable cursor placement.
- **Local lint:** deterministic file checks with severity, rule, line, and navigation.
- **Regex quick fixes:** previews transformations such as trailing-whitespace cleanup and duplicate-import removal before applying.
- **Static project analysis:** bounded project checks that do not read sensitive file contents.

No AI credential is needed for these tools.

## 4. Initial commit to an empty GitHub repository

Connect the repository normally and leave Branch blank to discover its configured default branch. When GitHub reports that the repository has no commits, NovaIDE offers **Create Initial Commit**.

M6 does not request a nonexistent HEAD or base tree. It uploads local file blobs, creates a root tree without `base_tree`, creates a commit without parents, then creates `refs/heads/<branch>`. The local baseline is recorded only after GitHub confirms the commit.

An entirely fileless workspace still cannot be committed because Git stores files, not empty directory objects. Add a README or `.gitkeep` first.

## 5. Error behavior

NovaIDE distinguishes:

- Missing key/token
- Invalid or expired credential
- Valid credential without model/repository permission
- Read-only source-control scope
- Repository role without push access
- Missing/empty branch
- Stale remote head requiring pull
- Provider quota or rate limit
- DNS failure
- TLS/certificate failure
- Network timeout
- Invalid custom endpoint

No write action falls back to force-push or silently broadens permissions.
