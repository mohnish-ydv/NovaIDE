#!/usr/bin/env python3
"""NovaIDE M10 repository-level completeness, privacy and Universal Runtime security gates."""
from __future__ import annotations

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
failures: list[str] = []


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        failures.append(f"Missing required file: {relative}")
        return ""
    return path.read_text(encoding="utf-8")


def require_text(relative: str, snippets: tuple[str, ...], label: str) -> str:
    text = read(relative)
    for snippet in snippets:
        if snippet not in text:
            failures.append(f"{label} missing in {relative}: {snippet}")
    return text


required = [
    "settings.gradle.kts",
    "build.gradle.kts",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/mohnishraj/novaide/MainActivity.kt",
    "app/src/main/java/com/mohnishraj/novaide/files/FileRepository.kt",
    "app/src/main/java/com/mohnishraj/novaide/files/WorkspaceStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/archive/ZipSafety.kt",
    "app/src/main/java/com/mohnishraj/novaide/archive/ZipWorkspaceManager.kt",
    "app/src/main/java/com/mohnishraj/novaide/git/GitModels.kt",
    "app/src/main/java/com/mohnishraj/novaide/git/GitCommitPlanner.kt",
    "app/src/main/java/com/mohnishraj/novaide/git/GitUrlParser.kt",
    "app/src/main/java/com/mohnishraj/novaide/git/GitSnapshotStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/git/GitStatusEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/git/UnifiedDiff.kt",
    "app/src/main/java/com/mohnishraj/novaide/git/ConflictParser.kt",
    "app/src/main/java/com/mohnishraj/novaide/github/GitHubStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/github/GitHubTokenNormalizer.kt",
    "app/src/main/java/com/mohnishraj/novaide/github/GitHubApiClient.kt",
    "app/src/main/java/com/mohnishraj/novaide/github/GitHubArchiveApplier.kt",
    "app/src/main/java/com/mohnishraj/novaide/credentials/CredentialModels.kt",
    "app/src/main/java/com/mohnishraj/novaide/credentials/CredentialCatalog.kt",
    "app/src/main/java/com/mohnishraj/novaide/credentials/CredentialSettingsStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/credentials/SecureCredentialStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/gitlab/GitLabTokenNormalizer.kt",
    "app/src/main/java/com/mohnishraj/novaide/gitlab/GitLabApiClient.kt",
    "app/src/main/java/com/mohnishraj/novaide/ai/AiModels.kt",
    "app/src/main/java/com/mohnishraj/novaide/ai/AiApiClient.kt",
    "app/src/main/java/com/mohnishraj/novaide/ai/AiContextBuilder.kt",
    "app/src/main/java/com/mohnishraj/novaide/ai/AiPromptBuilder.kt",
    "app/src/main/java/com/mohnishraj/novaide/ai/AiResponseParser.kt",
    "app/src/main/java/com/mohnishraj/novaide/ai/AiWorkspacePatcher.kt",
    "app/src/main/java/com/mohnishraj/novaide/ai/SecretRedactor.kt",
    "app/src/main/java/com/mohnishraj/novaide/localintel/AutocompleteEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/localintel/SnippetCatalog.kt",
    "app/src/main/java/com/mohnishraj/novaide/localintel/LocalLintEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/localintel/RegexFixEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/localintel/StaticAnalysisEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/AndroidProjectAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/ManifestInspector.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/ManifestEditor.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/ResourceAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/GradleBuildAssistant.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/ApkInspector.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/BuildLogReader.kt",
    "app/src/main/java/com/mohnishraj/novaide/androidsuite/BuildLogAnalyzer.kt",
    "app/src/test/java/com/mohnishraj/novaide/editor/EditorCoreTest.kt",
    "app/src/test/java/com/mohnishraj/novaide/editor/M3ProjectCoreTest.kt",
    "app/src/test/java/com/mohnishraj/novaide/git/M4GitCoreTest.kt",
    "app/src/test/java/com/mohnishraj/novaide/editor/M5AndroidSuiteTest.kt",
    "app/src/test/java/com/mohnishraj/novaide/m6/M6AiLocalCoreTest.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/DiagnosticModels.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/CrashTraceAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/DuplicateCodeAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/DeadCodeAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/DependencyGraphAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/PerformanceAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/SecurityAnalyzer.kt",
    "app/src/main/java/com/mohnishraj/novaide/diagnostics/ProjectAuditEngine.kt",
    "app/src/test/java/com/mohnishraj/novaide/m7/M7DiagnosticsTest.kt",
    "app/src/main/java/com/mohnishraj/novaide/plugins/MiniJson.kt",
    "app/src/main/java/com/mohnishraj/novaide/plugins/PluginModels.kt",
    "app/src/main/java/com/mohnishraj/novaide/plugins/PluginManifestParser.kt",
    "app/src/main/java/com/mohnishraj/novaide/plugins/PluginPolicy.kt",
    "app/src/main/java/com/mohnishraj/novaide/plugins/PluginStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/productivity/CommandPaletteEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/productivity/NovaConsoleEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/productivity/TaskRunner.kt",
    "app/src/main/java/com/mohnishraj/novaide/productivity/ProductivityStore.kt",
    "app/src/test/java/com/mohnishraj/novaide/m8/M8ProductivityTest.kt",
    "app/src/main/java/com/mohnishraj/novaide/webpreview/WebPreviewEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/webpreview/WebPreviewSettingsStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/webpreview/WebConsoleBuffer.kt",
    "app/src/main/java/com/mohnishraj/novaide/webpreview/WorkspaceWebServer.kt",
    "app/src/test/java/com/mohnishraj/novaide/m9/M9WebPreviewTest.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/RuntimeModels.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/PackageJsonReader.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/UniversalRuntimeEngine.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/TermuxCommandPolicy.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/SharedWorkspacePathResolver.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/RuntimeSettingsStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/TermuxBridge.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/TermuxResultReceiver.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/TermuxRunResultStore.kt",
    "app/src/main/java/com/mohnishraj/novaide/runtime/DocumentPreviewGenerator.kt",
    "app/src/test/java/com/mohnishraj/novaide/m10/M10UniversalRuntimeTest.kt",
    ".github/workflows/build-apk.yml",
    "README.md",
    "ARCHITECTURE.md",
    "CHANGELOG.md",
    "M4-GIT-GITHUB-GUIDE.md",
    "M5-ANDROID-SUITE-GUIDE.md",
    "M6-AI-CREDENTIALS-GUIDE.md",
    "M7-DEBUG-ANALYSIS-GUIDE.md",
    "M8-PLUGINS-PRODUCTIVITY-GUIDE.md",
    "M9-WEB-PREVIEW-GUIDE.md",
    "M9-QA-REPORT.md",
    "M10-UNIVERSAL-RUNTIME-GUIDE.md",
    "M10-QA-REPORT.md",
    "MILESTONES.md",
    "TERMUX-COMMANDS.md",
]
for item in required:
    read(item)

# XML must be structurally valid.
xml_files = sorted(ROOT.rglob("*.xml"))
for xml_file in xml_files:
    try:
        ET.parse(xml_file)
    except ET.ParseError as exc:
        failures.append(f"Invalid XML {xml_file.relative_to(ROOT)}: {exc}")

# The IDE needs network only; workspaces use SAF.
manifest = read("app/src/main/AndroidManifest.xml")
for forbidden in (
    "MANAGE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
    "REQUEST_INSTALL_PACKAGES", "QUERY_ALL_PACKAGES", "READ_LOGS", "PACKAGE_USAGE_STATS",
):
    if forbidden in manifest:
        failures.append(f"Forbidden or unnecessary permission: {forbidden}")
permissions = re.findall(r'<uses-permission\s+android:name="([^"]+)"', manifest)
if permissions != ["android.permission.INTERNET", "com.termux.permission.RUN_COMMAND"]:
    failures.append(f"Unexpected permission set: {permissions}")

require_text("app/build.gradle.kts", (
    "versionCode = 10", 'versionName = "1.0.0-M10"', "compileSdk = 35", "targetSdk = 35",
    'testImplementation("junit:junit:4.13.2")',
), "Build configuration")
require_text(".github/workflows/build-apk.yml", (
    "python3 tools/static_qa.py", ":app:testDebugUnitTest", ":app:assembleDebug", ":app:lintDebug",
    "NovaIDE-M10-Debug-APK", "NovaIDE-M10-Lint-Report",
), "Workflow")

main_activity = require_text("app/src/main/java/com/mohnishraj/novaide/MainActivity.kt", (
    "showCredentialsCenter()", "showGitHubCredentialEditor()", "showGitLabCredentialEditor()",
    "showAiCredentialEditor(provider", "showAiCenter()", "runAiTask(", "showAiResponse(",
    "confirmAiFilePatches(", "showLocalIntelligenceCenter()", "showAutocomplete()", "showSnippets()",
    "showLocalLint()", "showRegexFixes()", "showStaticAnalysis()", "checkRemoteInitialization(",
    "Create Initial Commit", "Initial commit / initialize sync", "KeyEvent.KEYCODE_SPACE",
    "GitLabApiClient", "CredentialCatalog", "SecureCredentialStore",
    "showDiagnosticsCenter()", "runFullProjectAudit()", "promptCrashTrace()", "analyzeDiagnosticLog(uri)",
    "runDuplicateAnalysis()", "runDependencyAnalysis()", "runFindingAnalysis(", "diagnosticBadge(",
    "showCommandPalette()", "buildPaletteCommands()", "showExtensionsCenter()", "confirmPluginInstall(",
    "executePluginCommand(", "showProductivityCenter()", "showNovaConsole()", "buildConsoleContext(",
    "PluginStore", "ProductivityStore", "KeyEvent.KEYCODE_P -> if (event.shiftPressedCompat()) showCommandPalette()",
    "runWebPreview(", "showWebPreviewCenter()", "createWebPreviewPane()", "ensurePreviewWebView()",
    "WorkspaceWebServer", "WebPreviewSettingsStore", "syncWebPreviewOverrides()", "scheduleWebPreviewReload()",
    "WebView.setWebContentsDebuggingEnabled(false)", "allowFileAccess = false", "allowContentAccess = false",
    "mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW", "KeyEvent.KEYCODE_R",
    'scheme in setOf("https", "http", "mailto", "tel")', "Blocked unsafe navigation scheme",
), "M1-M9 UI integration")
if '@SuppressLint("WrongConstant")' in main_activity:
    failures.append("WrongConstant must be fixed, not suppressed")
if re.search(r"takePersistableUriPermission\(\s*uri\s*,\s*flags\s*\)", main_activity):
    failures.append("Dynamic flags are still passed to takePersistableUriPermission")
if "if (remove.isChecked) credentialVault.delete(provider.credentialId)\n                val rawKey" in main_activity:
    failures.append("AI credential is deleted before provider verification")

# Shared secret vault and endpoint policy.
require_text("app/src/main/java/com/mohnishraj/novaide/credentials/SecureCredentialStore.kt", (
    "AndroidKeyStore", "AES/GCM/NoPadding", "GCMParameterSpec(128, iv)",
    "setRandomizedEncryptionRequired(true)", "MAX_SECRET_LENGTH", "cipher.iv",
), "Credential encryption")
require_text("app/src/main/java/com/mohnishraj/novaide/credentials/CredentialSettingsStore.kt", (
    "validatedAiConfig", "normalizeGitLabBaseUrl", 'uri.scheme.equals("https"',
    "uri.userInfo == null", "validateHttpsOrigin",
), "Credential endpoint validation")
require_text("app/src/main/java/com/mohnishraj/novaide/credentials/CredentialCatalog.kt", (
    "https://github.com/settings/personal-access-tokens/new",
    "https://gitlab.com/-/user_settings/personal_access_tokens",
    "https://platform.openai.com/api-keys", "https://aistudio.google.com/app/apikey",
    "https://console.groq.com/keys", "https://openrouter.ai/settings/keys",
    "Contents read/write", "read_api",
), "Credential catalog")
require_text("app/src/main/java/com/mohnishraj/novaide/credentials/CredentialModels.kt", (
    "Android Keystore", "stored only on this device", "OPENAI", "GEMINI", "GROQ", "OPENROUTER", "CUSTOM",
), "Credential privacy/provider models")

# GitHub empty repository, stale-head and no-force safeguards.
require_text("app/src/main/java/com/mohnishraj/novaide/git/GitCommitPlanner.kt", (
    "GitCommitMode.INITIAL", "GitCommitMode.UPDATE", "Remote branch changed",
    "Initial commit cannot contain deleted paths", "changes.any",
), "Commit planner")
require_text("app/src/main/java/com/mohnishraj/novaide/github/GitHubApiClient.kt", (
    "fun branchHeadOrNull", "error.statusCode == 409", "GitCommitPlanner.plan", "base_tree",
    'put("parents", JSONArray().put(plan.parent))', 'put("ref", "refs/heads/${repo.branch}")',
    'put("force", false)', "Contents read/write", "MAX_COMMIT_FILES = 200",
    "MAX_COMMIT_FILE_BYTES = 25L * 1024L * 1024L", "MAX_COMMIT_TOTAL_BYTES = 50L * 1024L * 1024L",
    'url.host.equals("api.github.com", ignoreCase = true)', "UnknownHostException", "SSLException",
), "GitHub writer")
require_text("app/src/main/java/com/mohnishraj/novaide/git/GitStatusEngine.kt", (
    "if (baseline == null)", "GitChangeKind.ADDED",
), "Empty-repository status")
require_text("app/src/main/java/com/mohnishraj/novaide/github/GitHubStore.kt", (
    "SecureCredentialStore", "migrateLegacyToken", "GitHubTokenNormalizer.normalize",
), "GitHub credential migration")

# GitLab scope-aware verification.
require_text("app/src/main/java/com/mohnishraj/novaide/gitlab/GitLabApiClient.kt", (
    "/api/v4/user", "/api/v4/personal_access_tokens/self", "read_api", "api scope",
    "requireRead", "requireWrite", 'url.host.equals(origin.host, ignoreCase = true)',
    "PRIVATE-TOKEN", "UnknownHostException", "SSLException",
), "GitLab API")
require_text("app/src/main/java/com/mohnishraj/novaide/gitlab/GitLabTokenNormalizer.kt", (
    "private-token", "authorization", "bearer", "none { it.isWhitespace() }",
), "GitLab token normalization")

# AI context, protocol adapters and patch boundaries.
require_text("app/src/main/java/com/mohnishraj/novaide/ai/AiApiClient.kt", (
    "MAX_RESPONSE_BYTES", "MAX_PROMPT_CHARS", 'endpoint("/responses")',
    'endpoint("/chat/completions")', "generateContent", "x-goog-api-key",
    'url.host.equals(baseHost, ignoreCase = true)', "Provider quota", "TLS connection",
), "AI provider adapter")
require_text("app/src/main/java/com/mohnishraj/novaide/ai/AiContextBuilder.kt", (
    "MAX_CONTEXT_CHARS", "MAX_RELEVANT_FILES", "SecretRedactor::isSensitivePath",
    "SecretRedactor.redact", "MAX_TREE_PATHS",
), "AI context safety")
require_text("app/src/main/java/com/mohnishraj/novaide/ai/SecretRedactor.kt", (
    "github_pat_", "glpat-", "AIza", "local.properties", "google-services.json",
    'name.endsWith(".jks")', 'lower.contains("/credentials/")',
), "Secret redaction")
require_text("app/src/main/java/com/mohnishraj/novaide/ai/AiResponseParser.kt", (
    "MAX_PATCH_FILES", "MAX_PATCH_TOTAL_CHARS", "duplicate path", 'it == ".."',
    "AI patch path is unsafe",
), "AI patch parser")
require_text("app/src/main/java/com/mohnishraj/novaide/ai/AiWorkspacePatcher.kt", (
    "dirtyPaths", "Save or close unsaved files before applying", "repository.createFile",
), "AI workspace patcher")

# Offline tools must remain deterministic local engines.
require_text("app/src/main/java/com/mohnishraj/novaide/localintel/AutocompleteEngine.kt", (
    "MAX_ITEMS", "LanguageDetector", "SymbolExtractor", "keywords(language)",
), "Autocomplete")
require_text("app/src/main/java/com/mohnishraj/novaide/localintel/SnippetCatalog.kt", (
    "__NOVA_CURSOR__", "fun forFile", "fun expand",
), "Snippets")
require_text("app/src/main/java/com/mohnishraj/novaide/localintel/LocalLintEngine.kt", (
    "MAX_ISSUES", "duplicate-import", "unclosed-delimiter", "trailing-whitespace",
), "Local lint")
require_text("app/src/main/java/com/mohnishraj/novaide/localintel/RegexFixEngine.kt", (
    "trim-trailing", "dedupe-imports", "differenceEstimate",
), "Regex fixes")
require_text("app/src/main/java/com/mohnishraj/novaide/localintel/StaticAnalysisEngine.kt", (
    "MAX_FINDINGS", "SecretRedactor.isSensitivePath", "Unresolved merge conflict",
    'MessageDigest.getInstance("SHA-256")',
), "Static analysis")

# M7 local diagnostics and non-destructive boundaries.
require_text("app/src/main/java/com/mohnishraj/novaide/diagnostics/CrashTraceAnalyzer.kt", (
    "MAX_LOG_CHARS = 2_000_000", "Caused by", "resolveProjectPath", "SecretRedactor.redact",
    "fingerprint", "Start from the earliest project-owned frame",
), "Crash trace analyzer")
require_text("app/src/main/java/com/mohnishraj/novaide/diagnostics/DuplicateCodeAnalyzer.kt", (
    "MAX_GROUPS = 120", "WINDOW_LINES = 8", "MIN_NORMALIZED_CHARS = 120", "collapseOverlaps",
), "Duplicate analyzer")
require_text("app/src/main/java/com/mohnishraj/novaide/diagnostics/DeadCodeAnalyzer.kt", (
    "Possibly unused", "Unused import", "Possibly unreachable statement", "before deleting",
), "Dead-code analyzer")
require_text("app/src/main/java/com/mohnishraj/novaide/diagnostics/DependencyGraphAnalyzer.kt", (
    "MAX_EDGES = 8_000", "strongConnect", "orphanSources", "DEPENDENCY MAP",
), "Dependency analyzer")
require_text("app/src/main/java/com/mohnishraj/novaide/diagnostics/PerformanceAnalyzer.kt", (
    "Long function", "Blocking call", "Whole-file I/O", "Large image asset", "Deep nesting",
), "Performance analyzer")
require_text("app/src/main/java/com/mohnishraj/novaide/diagnostics/SecurityAnalyzer.kt", (
    "Possible embedded secret", "Cleartext network endpoint", "TLS verification bypass",
    "WebView JavaScript bridge", "Unpinned dependency version",
), "Security analyzer")
require_text("app/src/main/java/com/mohnishraj/novaide/diagnostics/ProjectAuditEngine.kt", (
    "MAX_FILES = 8_000", "qualityScore", "DiagnosticCategory.DUPLICATION", "Dependency cycle",
    "renderFindings", "confidence",
), "Project audit engine")
if "auto-delete" not in read("README.md") and "never automatically delete" not in read("README.md"):
    failures.append("M7 non-destructive heuristic boundary is not documented")

# M8 declarative extension and productivity sandbox.
require_text("app/src/main/java/com/mohnishraj/novaide/plugins/MiniJson.kt", (
    "MAX_INPUT = 128_000", "MAX_DEPTH = 24", "MAX_ITEMS = 1_000", "Duplicate JSON key",
), "Bounded plugin JSON")
require_text("app/src/main/java/com/mohnishraj/novaide/plugins/PluginManifestParser.kt", (
    "MAX_COMMANDS = 24", "Duplicate plugin command id", "PluginPolicy.validateCommand", "Unknown plugin permission",
), "Plugin manifest parser")
require_text("app/src/main/java/com/mohnishraj/novaide/plugins/PluginPolicy.kt", (
    "READ_WORKSPACE", "EDITOR_WRITE", "OPEN_EXTERNAL", "CLIPBOARD_WRITE", "Plugin links must use HTTPS",
    "Plugin links cannot include credentials", "Plugin is disabled",
), "Plugin permission policy")
require_text("app/src/main/java/com/mohnishraj/novaide/plugins/PluginStore.kt", (
    'getSharedPreferences("nova_plugins"', "PluginManifestParser.parse", "setEnabled", "uninstall",
), "Plugin store")
require_text("app/src/main/java/com/mohnishraj/novaide/productivity/CommandPaletteEngine.kt", (
    "MAX_RESULTS = 60", "fuzzyScore", "keywords.joinToString",
), "Command palette")
require_text("app/src/main/java/com/mohnishraj/novaide/productivity/NovaConsoleEngine.kt", (
    "MAX_OUTPUT_CHARS = 80_000", "MAX_MATCHES = 2_000", "Parent path segments are not allowed",
    "safe built-in commands", "project-info", 'MessageDigest.getInstance("SHA-256")',
), "Nova Console")
require_text("app/src/main/java/com/mohnishraj/novaide/productivity/TaskRunner.kt", (
    "MAX_COMMANDS = 20", "supportedCommands", "Unsupported safe command", "TaskRunResult", "builtIns",
), "Task runner")
require_text("app/src/main/java/com/mohnishraj/novaide/productivity/ProductivityStore.kt", (
    'getSharedPreferences("nova_productivity"', "TaskRunner.validate", "saveTask", "deleteTask",
), "Productivity store")
for forbidden_plugin_api in ("DexClassLoader", "PathClassLoader", "Runtime.getRuntime().exec", "ProcessBuilder("):
    for plugin_file in sorted((ROOT / "app/src/main/java/com/mohnishraj/novaide/plugins").rglob("*.kt")):
        if forbidden_plugin_api in plugin_file.read_text(encoding="utf-8"):
            failures.append(f"Forbidden extension runtime API {forbidden_plugin_api}: {plugin_file.relative_to(ROOT)}")

# M9 secure local Web Preview runtime.
require_text("app/src/main/java/com/mohnishraj/novaide/webpreview/WebPreviewEngine.kt", (
    'const val HOST = "nova.local"', "normalizePath", 'part == ".."', "isSensitive", "blockedFileNames",
    "injectDiagnostics", "unhandledrejection", "BUILD_OUTPUT", "TOOLING_SOURCE", "localUrl",
), "Web Preview planning and path safety")
require_text("app/src/main/java/com/mohnishraj/novaide/webpreview/WorkspaceWebServer.kt", (
    "MAX_TEXT_BYTES = 6L * 1024L * 1024L", "MAX_RESOURCE_BYTES = 100L * 1024L * 1024L",
    "allowExternalNetwork", "spaFallback", "WebPreviewEngine.isSensitive", "request.isForMainFrame",
    'url.scheme.equals("https", ignoreCase = true)', '"Cache-Control" to "no-store, max-age=0"', '"X-Content-Type-Options" to "nosniff"',
), "Workspace WebView server")
require_text("app/src/main/java/com/mohnishraj/novaide/webpreview/WebPreviewSettingsStore.kt", (
    'getSharedPreferences("nova_web_preview"', 'prefs.getBoolean("external_network", false)',
    'prefs.getBoolean("live_reload", true)', "PreviewViewport.RESPONSIVE",
), "Web Preview settings")
require_text("app/src/main/java/com/mohnishraj/novaide/webpreview/WebConsoleBuffer.kt", (
    "capacity: Int = 300", "message.take(8_000)", "while (entries.size >", "entries.lastOrNull()",
), "Web console bounds")
if "addJavascriptInterface" in main_activity:
    failures.append("M9 must not expose an addJavascriptInterface bridge")
for required_security in (
    "setAcceptCookie(false)", "setAcceptThirdPartyCookies(currentWebView, false)",
    "WebView.setWebContentsDebuggingEnabled(false)", "allowFileAccess = false", "allowContentAccess = false",
    "mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW",
):
    if required_security not in main_activity:
        failures.append(f"Web Preview hardening missing: {required_security}")

# M10 universal runtime and explicit Termux security boundary.
require_text("app/src/main/java/com/mohnishraj/novaide/runtime/PackageJsonReader.kt", (
    "MAX_INPUT = 512_000", "MAX_DEPTH = 32", "MAX_ITEMS = 8_000", "Duplicate package.json key",
), "Bounded package.json reader")
require_text("app/src/main/java/com/mohnishraj/novaide/runtime/UniversalRuntimeEngine.kt", (
    "RuntimeKind.VITE", "RuntimeKind.REACT", "RuntimeKind.NEXT_JS", "RuntimeKind.ANGULAR",
    "RuntimeKind.PYTHON", "RuntimeKind.PHP", "RuntimeKind.HUGO", "RuntimeKind.JEKYLL",
    "generatedOnly = kind in browserBuildKinds", "Source project detected, but no generated browser output exists yet",
), "Universal runtime detection")
require_text("app/src/main/java/com/mohnishraj/novaide/runtime/TermuxCommandPolicy.kt", (
    "allowedExecutables", "Destructive runtime commands are blocked", "Runtime arguments cannot contain control lines",
    "safePort", "shellQuote",
), "Termux command policy")
require_text("app/src/main/java/com/mohnishraj/novaide/runtime/SharedWorkspacePathResolver.kt", (
    "EXTERNAL_STORAGE_AUTHORITY", "treeDocumentId.indexOf(':')",
    '"/storage/emulated/0"', "char.code < 32",
), "Shared-storage resolver")
require_text("app/src/main/java/com/mohnishraj/novaide/runtime/TermuxBridge.kt", (
    'const val RUN_PERMISSION = "com.termux.permission.RUN_COMMAND"',
    'const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"',
    'const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"',
    'require(workDir.startsWith("/storage/"', "PendingIntent.FLAG_ONE_SHOT", "TermuxCommandPolicy.validate(command)",
), "Termux bridge")
require_text("app/src/main/java/com/mohnishraj/novaide/runtime/DocumentPreviewGenerator.kt", (
    "MAX_SOURCE_CHARS", "private fun escape", "securityLevel: 'strict'", "https://cdn.jsdelivr.net/npm/mermaid",
), "Document preview generator")
require_text("app/src/main/java/com/mohnishraj/novaide/webpreview/WebPreviewEngine.kt", (
    "runtimeOrigin", "isAllowedRuntimeUrl", 'host !in setOf("127.0.0.1", "localhost", "::1")',
), "Loopback runtime isolation")
for forbidden_runtime_api in ("Runtime.getRuntime().exec", "ProcessBuilder(", "su -c", "sh -c"):
    for runtime_file in sorted((ROOT / "app/src/main/java/com/mohnishraj/novaide/runtime").rglob("*.kt")):
        if forbidden_runtime_api in runtime_file.read_text(encoding="utf-8"):
            failures.append(f"Forbidden direct runtime API {forbidden_runtime_api}: {runtime_file.relative_to(ROOT)}")
for required_runtime_ui in (
    "runUniversalProject()", "showRuntimeCenter()", "showTermuxSetup()", "confirmTermuxCommand(",
    "promptLoopbackPreview(", "startGeneratedDocumentPreview(", "ServiceWorkerController.getInstance()",
    "NovaIDE never runs a Termux command silently", "Open local development server preview",
):
    if required_runtime_ui not in main_activity:
        failures.append(f"M10 runtime UI integration missing: {required_runtime_ui}")
if '<package android:name="com.termux"' not in manifest:
    failures.append("Termux package visibility declaration is missing")
if 'android:name=".runtime.TermuxResultReceiver"' not in manifest or 'android:exported="false"' not in manifest:
    failures.append("Private Termux result receiver is missing or exported")

# Retain M3-M5 archive/Android size and parser limits.
require_text("app/src/main/java/com/mohnishraj/novaide/github/GitHubArchiveApplier.kt", (
    "MAX_ENTRIES = 12_000", "MAX_TOTAL_BYTES = 700L * 1024L * 1024L",
    "ZipSafety.safeSegments(entry.name)", "duplicate path",
), "GitHub archive safety")
require_text("app/src/main/java/com/mohnishraj/novaide/androidsuite/ApkInspector.kt", (
    "MAX_ENTRIES = 80_000", "MAX_TOTAL_BYTES = 2L * 1024L * 1024L * 1024L", "Unsafe APK entry path",
), "APK limits")
require_text("app/src/main/java/com/mohnishraj/novaide/androidsuite/BuildLogReader.kt", (
    "MAX_TEXT_BYTES = 8L * 1024L * 1024L", "MAX_ZIP_ENTRIES = 500", "collectedBytes",
), "Build-log limits")

# Unit-test inventory should not silently shrink.
test_files = sorted((ROOT / "app/src/test/java").rglob("*.kt"))
test_count = sum(path.read_text(encoding="utf-8").count("@Test") for path in test_files)
if test_count < 71:
    failures.append(f"Expected at least 71 JVM tests, found {test_count}")

# Catch accidental real credentials. Examples in tests are intentionally shorter than these thresholds.
secret_patterns = (
    re.compile(r"github_pat_[A-Za-z0-9_]{30,}"),
    re.compile(r"gh[pousr]_[A-Za-z0-9]{30,}"),
    re.compile(r"glpat-[A-Za-z0-9_-]{30,}"),
    re.compile(r"\bsk-[A-Za-z0-9_-]{32,}\b"),
    re.compile(r"\bAIza[A-Za-z0-9_-]{32,}\b"),
)
for candidate in sorted(ROOT.rglob("*")):
    if candidate.is_file() and candidate.suffix.lower() in {".kt", ".kts", ".md", ".yml", ".yaml", ".py", ".xml", ".properties"}:
        text = candidate.read_text(encoding="utf-8", errors="ignore")
        if any(pattern.search(text) for pattern in secret_patterns):
            failures.append(f"Possible embedded credential literal: {candidate.relative_to(ROOT)}")

unfinished_patterns = (
    re.compile(r"\bTODO\s*\("), re.compile(r"\bNotImplementedError\b"),
    re.compile(r"\berror\(\s*[\"']Not implemented", re.IGNORECASE),
)
for kotlin_file in sorted(ROOT.rglob("*.kt")):
    text = kotlin_file.read_text(encoding="utf-8")
    if any(pattern.search(text) for pattern in unfinished_patterns):
        failures.append(f"Unfinished implementation marker: {kotlin_file.relative_to(ROOT)}")

for generated in (".gradle", "build", ".idea", "local.properties", "__pycache__"):
    for path in ROOT.rglob(generated):
        failures.append(f"Generated/local file must not ship: {path.relative_to(ROOT)}")

if failures:
    print("NovaIDE M10 static QA failed:")
    for failure in failures:
        print(f"- {failure}")
    sys.exit(1)

kotlin_files = sorted(ROOT.rglob("*.kt"))
source_lines = sum(len(path.read_text(encoding="utf-8").splitlines()) for path in kotlin_files)
print("NovaIDE M10 static QA passed")
print(f"XML files: {len(xml_files)}")
print(f"Kotlin files: {len(kotlin_files)}")
print(f"Kotlin lines: {source_lines}")
print(f"JVM tests: {test_count}")
print(f"Permissions: {', '.join(permissions)}")
