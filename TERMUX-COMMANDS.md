# NovaIDE M10 — Phone-only GitHub and Termux commands

## Upload the project to GitHub

```bash
termux-setup-storage
cd ~/storage/downloads
unzip NovaIDE-M10-Universal-Runtime-GitHub-Ready.zip
cd NovaIDE-M10-Universal-Runtime-GitHub-Ready
git init
git branch -M main
git add .
git commit -m "NovaIDE 1.0.0 M10 universal runtime"
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin main
```

For an existing checkout, replace the remote-add line with the repository's existing remote and use `git push`.

## Build APK

Open the repository's **Actions** tab and run **Build NovaIDE APK**. The workflow runs static QA, 71+ JVM tests, `assembleDebug`, and `lintDebug`. Download `NovaIDE-M10-Debug-APK` from the completed run.

## Prepare Termux for NovaIDE runtime commands

Run inside Termux:

```bash
termux-setup-storage
mkdir -p ~/.termux
grep -q '^allow-external-apps=true$' ~/.termux/termux.properties 2>/dev/null || printf '\nallow-external-apps=true\n' >> ~/.termux/termux.properties
```

Then fully restart Termux. Install only the runtimes you need, for example:

```bash
pkg update
pkg install nodejs python php
```

Inside Android App Info for NovaIDE, grant the additional **Run commands in Termux environment** permission. Choose NovaIDE workspaces from shared storage such as Download/Documents so both apps can access the same project path.

NovaIDE always displays and confirms the exact command and working directory. It does not provide an unrestricted shell or silently install packages.
