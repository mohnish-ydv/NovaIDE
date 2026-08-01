package com.mohnishraj.novaide

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mohnishraj.novaide.androidsuite.AndroidIssueSeverity
import com.mohnishraj.novaide.androidsuite.AndroidProjectAnalyzer
import com.mohnishraj.novaide.androidsuite.AndroidProjectReport
import com.mohnishraj.novaide.androidsuite.AndroidSourceFile
import com.mohnishraj.novaide.androidsuite.ApkInspector
import com.mohnishraj.novaide.androidsuite.BuildLogAnalyzer
import com.mohnishraj.novaide.androidsuite.BuildLogReader
import com.mohnishraj.novaide.androidsuite.GradleBuildAssistant
import com.mohnishraj.novaide.androidsuite.ManifestEditor
import com.mohnishraj.novaide.androidsuite.ResourceAnalyzer
import com.mohnishraj.novaide.ai.AiApiClient
import com.mohnishraj.novaide.ai.AiContextBuilder
import com.mohnishraj.novaide.ai.AiRequest
import com.mohnishraj.novaide.ai.AiResponse
import com.mohnishraj.novaide.ai.AiResponseParser
import com.mohnishraj.novaide.ai.AiRuntime
import com.mohnishraj.novaide.ai.AiTask
import com.mohnishraj.novaide.ai.AiWorkspacePatcher
import com.mohnishraj.novaide.credentials.AiProviderId
import com.mohnishraj.novaide.credentials.CredentialCatalog
import com.mohnishraj.novaide.credentials.CredentialId
import com.mohnishraj.novaide.credentials.CredentialSettingsStore
import com.mohnishraj.novaide.credentials.SecureCredentialStore
import com.mohnishraj.novaide.diagnostics.CrashTraceAnalyzer
import com.mohnishraj.novaide.diagnostics.DeadCodeAnalyzer
import com.mohnishraj.novaide.diagnostics.DependencyGraphAnalyzer
import com.mohnishraj.novaide.diagnostics.DiagnosticCategory
import com.mohnishraj.novaide.diagnostics.DiagnosticFile
import com.mohnishraj.novaide.diagnostics.DiagnosticFinding
import com.mohnishraj.novaide.diagnostics.DiagnosticSeverity
import com.mohnishraj.novaide.diagnostics.DuplicateCodeAnalyzer
import com.mohnishraj.novaide.diagnostics.PerformanceAnalyzer
import com.mohnishraj.novaide.diagnostics.ProjectAuditEngine
import com.mohnishraj.novaide.diagnostics.SecurityAnalyzer
import com.mohnishraj.novaide.archive.ZipWorkspaceManager
import com.mohnishraj.novaide.core.TextFileClassifier
import com.mohnishraj.novaide.core.Ui
import com.mohnishraj.novaide.editor.CodeEditorView
import com.mohnishraj.novaide.editor.MinimapView
import com.mohnishraj.novaide.editor.navigation.SymbolExtractor
import com.mohnishraj.novaide.editor.search.SearchEngine
import com.mohnishraj.novaide.editor.search.SearchOptions
import com.mohnishraj.novaide.editor.search.SearchReplacePanel
import com.mohnishraj.novaide.editor.search.TextRange
import com.mohnishraj.novaide.files.FileRepository
import com.mohnishraj.novaide.files.WorkspaceStore
import com.mohnishraj.novaide.git.ConflictParser
import com.mohnishraj.novaide.git.ConflictResolution
import com.mohnishraj.novaide.git.GitChange
import com.mohnishraj.novaide.git.GitChangeKind
import com.mohnishraj.novaide.git.GitHubRepository
import com.mohnishraj.novaide.git.GitSnapshotStore
import com.mohnishraj.novaide.git.GitStatus
import com.mohnishraj.novaide.git.GitStatusEngine
import com.mohnishraj.novaide.git.GitUrlParser
import com.mohnishraj.novaide.git.UnifiedDiff
import com.mohnishraj.novaide.git.WorkflowArtifact
import com.mohnishraj.novaide.github.GitHubApiClient
import com.mohnishraj.novaide.github.GitHubArchiveApplier
import com.mohnishraj.novaide.github.GitHubStore
import com.mohnishraj.novaide.gitlab.GitLabApiClient
import com.mohnishraj.novaide.gitlab.GitLabTokenNormalizer
import com.mohnishraj.novaide.localintel.AutocompleteEngine
import com.mohnishraj.novaide.localintel.LintSeverity
import com.mohnishraj.novaide.localintel.LocalLintEngine
import com.mohnishraj.novaide.localintel.RegexFixEngine
import com.mohnishraj.novaide.localintel.SnippetCatalog
import com.mohnishraj.novaide.localintel.StaticAnalysisEngine
import com.mohnishraj.novaide.localintel.StaticFile
import com.mohnishraj.novaide.model.DocumentNode
import com.mohnishraj.novaide.model.EditorTab
import com.mohnishraj.novaide.preview.ResourcePreview
import com.mohnishraj.novaide.preview.ResourcePreviewer
import com.mohnishraj.novaide.plugins.InstalledPlugin
import com.mohnishraj.novaide.plugins.PluginActionType
import com.mohnishraj.novaide.plugins.PluginManifest
import com.mohnishraj.novaide.plugins.PluginManifestParser
import com.mohnishraj.novaide.plugins.PluginPermission
import com.mohnishraj.novaide.plugins.PluginPolicy
import com.mohnishraj.novaide.plugins.PluginStore
import com.mohnishraj.novaide.productivity.CommandPaletteEngine
import com.mohnishraj.novaide.productivity.ConsoleContext
import com.mohnishraj.novaide.productivity.ConsoleFile
import com.mohnishraj.novaide.productivity.NovaConsoleEngine
import com.mohnishraj.novaide.productivity.NovaTask
import com.mohnishraj.novaide.productivity.PaletteCommand
import com.mohnishraj.novaide.productivity.ProductivityStore
import com.mohnishraj.novaide.productivity.TaskRunner
import com.mohnishraj.novaide.project.ProjectDetector
import com.mohnishraj.novaide.project.ProjectReport
import com.mohnishraj.novaide.templates.TemplateCatalog
import com.mohnishraj.novaide.templates.TemplateInstaller
import com.mohnishraj.novaide.theme.NovaPalette
import com.mohnishraj.novaide.theme.ThemeManager
import com.mohnishraj.novaide.ui.EditorTabsView
import com.mohnishraj.novaide.ui.FileTreeAdapter
import com.mohnishraj.novaide.workspace.search.WorkspaceSearchEngine
import com.mohnishraj.novaide.workspace.search.WorkspaceSearchHit
import com.mohnishraj.novaide.workspace.search.WorkspaceSearchOptions
import com.mohnishraj.novaide.runtime.DocumentPreviewGenerator
import com.mohnishraj.novaide.runtime.RuntimeAction
import com.mohnishraj.novaide.runtime.RuntimeKind
import com.mohnishraj.novaide.runtime.RuntimeProject
import com.mohnishraj.novaide.runtime.RuntimeSettingsStore
import com.mohnishraj.novaide.runtime.TermuxBridge
import com.mohnishraj.novaide.runtime.TermuxCommandPolicy
import com.mohnishraj.novaide.runtime.TermuxRunResultStore
import com.mohnishraj.novaide.runtime.UniversalRuntimeEngine
import com.mohnishraj.novaide.webpreview.PreviewViewport
import com.mohnishraj.novaide.webpreview.WebConsoleBuffer
import com.mohnishraj.novaide.webpreview.WebConsoleEntry
import com.mohnishraj.novaide.webpreview.WebPreviewEngine
import com.mohnishraj.novaide.webpreview.WebPreviewKind
import com.mohnishraj.novaide.webpreview.WebPreviewSettings
import com.mohnishraj.novaide.webpreview.WebPreviewSettingsStore
import com.mohnishraj.novaide.webpreview.WorkspaceWebServer
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.max

class MainActivity : Activity() {
    companion object {
        private const val PICK_WORKSPACE = 41
        private const val PICK_ZIP = 42
        private const val CREATE_ZIP = 43
        private const val CREATE_GITHUB_ARTIFACT = 44
        private const val PICK_APK = 45
        private const val PICK_BUILD_LOG = 46
        private const val PICK_DIAGNOSTIC_LOG = 47
        private const val PICK_PLUGIN_MANIFEST = 48
        private const val REQUEST_TERMUX_PERMISSION = 49
        private const val AUTOSAVE_DELAY_MS = 900L
        private const val MAX_OPEN_TABS = 20
        private const val PREVIEW_RELOAD_DELAY_MS = 650L
    }

    private data class AiApplyTarget(
        val uri: Uri?,
        val fileName: String?,
        val selectionStart: Int,
        val selectionEnd: Int,
        val contentHash: Int
    )

    private lateinit var repository: FileRepository
    private lateinit var store: WorkspaceStore
    private lateinit var themeManager: ThemeManager
    private lateinit var gitHubStore: GitHubStore
    private lateinit var gitSnapshots: GitSnapshotStore
    private lateinit var credentialVault: SecureCredentialStore
    private lateinit var credentialSettings: CredentialSettingsStore
    private lateinit var pluginStore: PluginStore
    private lateinit var productivityStore: ProductivityStore
    private lateinit var webPreviewSettingsStore: WebPreviewSettingsStore
    private lateinit var webPreviewServer: WorkspaceWebServer
    private lateinit var runtimeSettings: RuntimeSettingsStore
    private lateinit var termuxBridge: TermuxBridge
    private lateinit var termuxResults: TermuxRunResultStore
    private var palette: NovaPalette = NovaPalette.MIDNIGHT

    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "nova-file-io").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val projectIo = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "nova-project-io").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val editorAnalysis = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "nova-editor-analysis").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val aiIo = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "nova-ai-network").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private var treeLoadTask: Future<*>? = null
    private var searchTask: Future<*>? = null
    private val pendingAutosaves = mutableMapOf<String, Runnable>()

    private lateinit var root: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var workspaceTitle: TextView
    private lateinit var filePane: LinearLayout
    private lateinit var fileList: ListView
    private lateinit var fileAdapter: FileTreeAdapter
    private lateinit var editorTabs: EditorTabsView
    private lateinit var editor: CodeEditorView
    private lateinit var minimap: MinimapView
    private lateinit var searchPanel: SearchReplacePanel
    private lateinit var editorHost: FrameLayout
    private lateinit var editorPreviewHost: LinearLayout
    private lateinit var previewPane: LinearLayout
    private lateinit var previewCanvas: FrameLayout
    private lateinit var previewStatusText: TextView
    private lateinit var welcome: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var saveStateText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var fileToggle: TextView

    private val visibleNodes = mutableListOf<DocumentNode>()
    private val childrenByParent = mutableMapOf<String, List<DocumentNode>>()
    private val openTabs = mutableListOf<EditorTab>()
    private var activeTab: EditorTab? = null
    private var workspaceRoot: DocumentNode? = null
    private var filePaneVisible = true
    private var minimapVisible = true
    private var searchMatches: List<TextRange> = emptyList()
    private var currentSearchIndex = -1
    private var currentSearchQuery = ""
    private var currentSearchOptions = SearchOptions()
    private var searchGeneration = 0
    private var workspaceIndex: List<FileRepository.WorkspaceEntry> = emptyList()
    private var projectReport: ProjectReport? = null
    private var pendingExportName: String = "NovaIDE-project.zip"
    private var pendingArtifact: WorkflowArtifact? = null
    private var gitMutationWorkspace: Uri? = null
    private var previewWebView: WebView? = null
    private var previewVisible = false
    private var previewFullscreen = false
    private var previewEntryPath: String? = null
    private var previewReloadTask: Runnable? = null
    private var filePaneBeforePreviewFullscreen = true
    private val webConsole = WebConsoleBuffer()
    private var defaultWebUserAgent: String? = null
    private var previewDocumentKind: RuntimeKind? = null
    private var pendingTermuxExecutionId: Int? = null
    private var pendingTermuxAction: RuntimeAction? = null
    private var pendingRuntimePort: Int? = null
    private var termuxResultPoll: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FileRepository(this)
        store = WorkspaceStore(this)
        themeManager = ThemeManager(this)
        credentialVault = SecureCredentialStore(this)
        credentialSettings = CredentialSettingsStore(this, credentialVault)
        pluginStore = PluginStore(this)
        productivityStore = ProductivityStore(this)
        webPreviewSettingsStore = WebPreviewSettingsStore(this)
        webPreviewServer = WorkspaceWebServer(repository)
        runtimeSettings = RuntimeSettingsStore(this)
        termuxBridge = TermuxBridge(this)
        termuxResults = TermuxRunResultStore(this)
        gitHubStore = GitHubStore(this)
        gitSnapshots = GitSnapshotStore(this, repository)
        palette = themeManager.palette
        minimapVisible = getSharedPreferences("nova_editor", MODE_PRIVATE)
            .getBoolean("minimap_visible", true)

        window.statusBarColor = palette.window
        window.navigationBarColor = palette.window
        buildUi()
        applyTheme()

        store.workspaceUri?.let { uri ->
            openWorkspace(uri, restoreTabs = true)
        } ?: showWelcome()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            if (requestCode == CREATE_GITHUB_ARTIFACT) pendingArtifact = null
            return
        }
        val resultData = data ?: return
        val uri = resultData.data ?: return
        when (requestCode) {
            PICK_WORKSPACE -> {
                persistUriPermission(uri, resultData.flags)
                requestWorkspaceSwitch(uri)
            }
            PICK_ZIP -> importZip(uri)
            CREATE_ZIP -> exportWorkspace(uri)
            CREATE_GITHUB_ARTIFACT -> downloadPendingArtifact(uri)
            PICK_APK -> inspectPickedApk(uri)
            PICK_BUILD_LOG -> analyzePickedBuildLog(uri)
            PICK_DIAGNOSTIC_LOG -> analyzeDiagnosticLog(uri)
            PICK_PLUGIN_MANIFEST -> installPluginFromUri(uri)
        }
    }

    override fun onPause() {
        super.onPause()
        snapshotActiveEditorState()
        persistSession()
        saveAllDirty(silent = true)
        previewWebView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        previewWebView?.onResume()
        consumeTermuxResult()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_TERMUX_PERMISSION) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) toast("Termux command permission granted")
            else showError("Permission not granted", "NovaIDE cannot run framework or language commands until the Termux RUN_COMMAND permission is granted in App Info → Permissions → Additional permissions.")
        }
    }

    override fun onDestroy() {
        treeLoadTask?.cancel(true)
        searchTask?.cancel(true)
        io.shutdown()
        editorAnalysis.shutdownNow()
        aiIo.shutdownNow()
        projectIo.shutdownNow()
        previewReloadTask?.let { mainHandler.removeCallbacks(it) }
        termuxResultPoll?.let { mainHandler.removeCallbacks(it) }
        previewWebView?.apply { stopLoading(); loadUrl("about:blank"); clearHistory(); removeAllViews(); destroy() }
        previewWebView = null
        super.onDestroy()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
        }

        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(this@MainActivity, 8), 0, Ui.dp(this@MainActivity, 8), 0)
        }
        fileToggle = toolbarButton("☰", "Toggle files") { toggleFilePane() }
        workspaceTitle = Ui.text(this, "NovaIDE", 15f, palette.textPrimary, bold = true).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            setPadding(Ui.dp(this@MainActivity, 8), 0, Ui.dp(this@MainActivity, 8), 0)
            setOnClickListener { showProjectHub() }
        }
        val themeButton = toolbarButton("◐", "Change theme") {
            snapshotActiveEditorState()
            persistSession()
            themeManager.next()
            recreate()
        }
        val searchButton = toolbarButton("⌕", "Find and replace") { openSearch() }
        val runButton = toolbarButton("▶", "Run project") { runUniversalProject() }
        val saveButton = toolbarButton("✓", "Save file") { saveActive(silent = false) }
        val menuButton = toolbarButton("⋮", "More actions") { showAppMenu(it) }
        toolbar.addView(fileToggle, sizeParams(44, 52))
        toolbar.addView(workspaceTitle, LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f))
        toolbar.addView(themeButton, sizeParams(42, 52))
        toolbar.addView(searchButton, sizeParams(40, 52))
        toolbar.addView(runButton, sizeParams(40, 52))
        toolbar.addView(saveButton, sizeParams(42, 52))
        toolbar.addView(menuButton, sizeParams(42, 52))
        root.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)))
        root.addView(Ui.divider(this, palette))

        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        filePane = createFilePane()
        body.addView(filePane, LinearLayout.LayoutParams(filePaneWidth(), ViewGroup.LayoutParams.MATCH_PARENT))
        body.addView(Ui.divider(this, palette, vertical = true))
        body.addView(createEditorPane(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val statusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(this@MainActivity, 10), 0, Ui.dp(this@MainActivity, 10), 0)
        }
        statusText = Ui.text(this, "Ready", 10.5f, palette.textSecondary)
        saveStateText = Ui.text(this, "", 10.5f, palette.textSecondary, gravity = Gravity.CENTER_VERTICAL or Gravity.END)
        statusBar.addView(statusText, LinearLayout.LayoutParams(0, Ui.dp(this, 28), 1f))
        statusBar.addView(saveStateText, LinearLayout.LayoutParams(Ui.dp(this, 110), Ui.dp(this, 28)))
        root.addView(Ui.divider(this, palette))
        root.addView(statusBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 28)))

        setContentView(root)
    }

    private fun createFilePane(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val header = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(this@MainActivity, 10), 0, Ui.dp(this@MainActivity, 6), 0)
        }
        val title = Ui.text(this@MainActivity, "FILES", 11f, palette.textSecondary, bold = true)
        val refresh = toolbarButton("↻", "Refresh files") { reloadWorkspaceTree() }
        val add = toolbarButton("+", "New file or folder") { showCreateMenu(it) }
        header.addView(title, LinearLayout.LayoutParams(0, Ui.dp(this@MainActivity, 44), 1f))
        header.addView(refresh, sizeParams(38, 44))
        header.addView(add, sizeParams(38, 44))
        addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 44)))
        addView(Ui.divider(this@MainActivity, palette))

        fileAdapter = FileTreeAdapter(
            this@MainActivity,
            palette,
            onClick = ::onFileNodeClick,
            onLongClick = ::showNodeMenu
        )
        fileList = ListView(this@MainActivity).apply {
            adapter = fileAdapter
            divider = null
            isFastScrollEnabled = true
            setOnItemClickListener(null)
        }
        addView(fileList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun createEditorPane(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        editorTabs = EditorTabsView(this@MainActivity)
        addView(editorTabs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 46)))
        addView(Ui.divider(this@MainActivity, palette))

        searchPanel = SearchReplacePanel(this@MainActivity).apply {
            visibility = View.GONE
            onSearchChanged = ::scheduleSearch
            onPrevious = { moveSearch(-1) }
            onNext = { moveSearch(1) }
            onReplace = ::replaceCurrentSearchMatch
            onReplaceAll = ::replaceAllSearchMatches
            onClose = ::closeSearch
        }
        addView(searchPanel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 90)))

        editorHost = FrameLayout(this@MainActivity)
        editor = CodeEditorView(this@MainActivity).apply {
            hint = "Open a text file from the workspace"
            onUserContentChanged = { updated ->
                activeTab?.let { tab ->
                    tab.content = updated
                    renderTabs()
                    updateStatus()
                    scheduleAutosave(tab)
                    syncWebPreviewOverrides()
                    scheduleWebPreviewReload()
                    if (searchPanel.visibility == View.VISIBLE) {
                        scheduleSearch(currentSearchQuery, currentSearchOptions)
                    }
                }
            }
            onVisualContentChanged = { updated ->
                if (::minimap.isInitialized) minimap.setSource(updated)
            }
            onSelectionChangedListener = {
                snapshotCursorOnly()
                updateStatus()
            }
            onMultiCursorStateChanged = { updateStatus() }
        }
        editorHost.addView(editor, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        minimap = MinimapView(this@MainActivity).apply {
            bind(editor)
            visibility = if (minimapVisible) View.VISIBLE else View.GONE
        }
        editorHost.addView(
            minimap,
            FrameLayout.LayoutParams(Ui.dp(this@MainActivity, 50), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END)
        )

        welcome = createWelcome()
        editorHost.addView(welcome, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        progress = ProgressBar(this@MainActivity).apply { visibility = View.GONE }
        editorHost.addView(progress, FrameLayout.LayoutParams(Ui.dp(this@MainActivity, 44), Ui.dp(this@MainActivity, 44), Gravity.CENTER))

        editorPreviewHost = LinearLayout(this@MainActivity)
        previewPane = createWebPreviewPane().apply { visibility = View.GONE }
        editorPreviewHost.addView(editorHost)
        editorPreviewHost.addView(previewPane)
        applyPreviewLayout()
        addView(editorPreviewHost, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }


    private fun createWebPreviewPane(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.WHITE)
        val bar = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(this@MainActivity, 4), 0, Ui.dp(this@MainActivity, 4), 0)
            setBackgroundColor(palette.surface)
        }
        fun control(symbol: String, description: String, action: (View) -> Unit): TextView =
            toolbarButton(symbol, description, action)
        bar.addView(control("‹", "Preview back") { previewWebView?.takeIf { it.canGoBack() }?.goBack() }, sizeParams(36, 42))
        bar.addView(control("›", "Preview forward") { previewWebView?.takeIf { it.canGoForward() }?.goForward() }, sizeParams(36, 42))
        bar.addView(control("↻", "Reload preview") { reloadWebPreview() }, sizeParams(36, 42))
        previewStatusText = Ui.text(this@MainActivity, "Web Preview", 11f, palette.textPrimary, bold = true).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            setPadding(Ui.dp(this@MainActivity, 6), 0, Ui.dp(this@MainActivity, 6), 0)
        }
        bar.addView(previewStatusText, LinearLayout.LayoutParams(0, Ui.dp(this@MainActivity, 42), 1f))
        bar.addView(control("▣", "Preview viewport") { showPreviewViewportMenu(it) }, sizeParams(36, 42))
        bar.addView(control("≡", "Web console") { showWebConsole() }, sizeParams(36, 42))
        bar.addView(control("↗", "Open HTML externally") { openPreviewExternally() }, sizeParams(36, 42))
        bar.addView(control("⛶", "Toggle preview fullscreen") { togglePreviewFullscreen() }, sizeParams(36, 42))
        bar.addView(control("×", "Close preview") { closeWebPreview() }, sizeParams(36, 42))
        addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 42)))
        addView(Ui.divider(this@MainActivity, palette))
        previewCanvas = FrameLayout(this@MainActivity).apply {
            setBackgroundColor(Color.WHITE)
            addView(Ui.text(this@MainActivity, "Press ▶ to run a browser-ready HTML file.", 13f, 0xff5f6470.toInt(), gravity = Gravity.CENTER),
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        addView(previewCanvas, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun applyPreviewLayout() {
        if (!::editorPreviewHost.isInitialized) return
        val wide = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || resources.configuration.screenWidthDp >= 700
        editorPreviewHost.orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        previewPane.visibility = if (previewVisible) View.VISIBLE else View.GONE
        editorHost.visibility = if (previewFullscreen) View.GONE else View.VISIBLE
        if (!previewVisible) {
            editorHost.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return
        }
        if (previewFullscreen) {
            previewPane.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return
        }
        if (wide) {
            editorHost.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            previewPane.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        } else {
            editorHost.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            previewPane.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
    }

    private fun runWebPreview(preferredPath: String? = null) {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        snapshotActiveEditorState()
        val preferred = preferredPath ?: activeTab?.uri?.let(::pathForUri)
        if (workspaceIndex.isEmpty()) {
            showProgress(true)
            statusText.text = "Indexing web workspace…"
            projectIo.submit {
                val scan = runCatching { repository.scan(rootNode) }
                mainHandler.post {
                    showProgress(false)
                    scan.onSuccess {
                        workspaceIndex = it.entries
                        webPreviewServer.updateWorkspace(workspaceIndex)
                        syncWebPreviewOverrides()
                        launchWebPreview(preferred)
                    }.onFailure { showError("Web Preview unavailable", it.message ?: "Could not index this workspace") }
                }
            }
        } else launchWebPreview(preferred)
    }

    private fun launchWebPreview(preferredPath: String?, ignoreToolingWarning: Boolean = false) {
        val plan = WebPreviewEngine.plan(workspaceIndex.filterNot { it.node.isDirectory }.map { it.relativePath }, preferredPath)
        val entry = plan.entryPath
        if (entry == null) {
            showError("No runnable web entry", plan.warning ?: "Add an index.html file and try again.")
            return
        }
        if (!ignoreToolingWarning && plan.kind == WebPreviewKind.TOOLING_SOURCE && plan.warning != null) {
            AlertDialog.Builder(this)
                .setTitle("Source project detected")
                .setMessage("${plan.warning}\n\nPreview $entry anyway?")
                .setPositiveButton("Preview anyway") { _, _ -> launchWebPreview(entry, true) }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        startWebPreview(entry)
    }

    private fun startWebPreview(entryPath: String) {
        if (WebPreviewEngine.isSensitive(entryPath)) {
            showError("Preview blocked", "Sensitive files cannot be served inside Web Preview.")
            return
        }
        val webView = ensurePreviewWebView() ?: return
        webPreviewServer.allowedRuntimeOrigin = null
        previewDocumentKind = null
        webPreviewServer.updateWorkspace(workspaceIndex)
        webPreviewServer.entryPath = entryPath
        syncWebPreviewOverrides()
        applyWebPreviewSettings(reload = false)
        previewEntryPath = entryPath
        previewVisible = true
        previewFullscreen = false
        webConsole.clear()
        applyPreviewLayout()
        previewStatusText.text = entryPath
        statusText.text = "Web Preview • $entryPath"
        webView.loadUrl(WebPreviewEngine.localUrl(entryPath))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensurePreviewWebView(): WebView? {
        previewWebView?.let { return it }
        return runCatching {
            ServiceWorkerController.getInstance().apply {
                serviceWorkerWebSettings.apply {
                    allowContentAccess = false
                    allowFileAccess = false
                    blockNetworkLoads = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                setServiceWorkerClient(object : ServiceWorkerClient() {
                    override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? =
                        webPreviewServer.intercept(request)
                })
            }
            val view = WebView(this).apply {
                setBackgroundColor(Color.WHITE)
                isFocusable = true
                isFocusableInTouchMode = true
                defaultWebUserAgent = WebSettings.getDefaultUserAgent(this@MainActivity)
                settings.apply {
                    domStorageEnabled = true
                    databaseEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    mediaPlaybackRequiresUserGesture = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    safeBrowsingEnabled = true
                }
                WebView.setWebContentsDebuggingEnabled(false)
                val currentWebView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(false)
                    setAcceptThirdPartyCookies(currentWebView, false)
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
                        request?.let(webPreviewServer::intercept)

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val target = request?.url ?: return true
                        if (target.host.equals(WebPreviewEngine.HOST, ignoreCase = true)) return false
                        if (WebPreviewEngine.isAllowedRuntimeUrl(target.toString(), webPreviewServer.allowedRuntimeOrigin)) return false
                        if (request.isForMainFrame) {
                            mainHandler.post {
                                val scheme = target.scheme.orEmpty().lowercase()
                                if (scheme in setOf("https", "http", "mailto", "tel")) {
                                    webConsole.add(WebConsoleEntry("navigation", "External navigation opened outside NovaIDE", target.toString()))
                                    openExternalUrl(target.toString())
                                } else {
                                    webConsole.add(WebConsoleEntry("blocked", "Blocked unsafe navigation scheme: $scheme", target.toString()))
                                    toast("Preview blocked an unsafe link")
                                }
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (!previewVisible) return
                        previewStatusText.text = previewEntryPath ?: "Web Preview"
                        statusText.text = "Preview ready • ${previewEntryPath.orEmpty()}"
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        val message = error?.description?.toString().orEmpty().ifBlank { "Web resource failed" }
                        webConsole.add(WebConsoleEntry("error", message, request?.url?.toString().orEmpty()))
                        if (request?.isForMainFrame == true) mainHandler.post { statusText.text = "Preview error • open console" }
                    }

                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                        val code = errorResponse?.statusCode ?: 0
                        if (code >= 400) {
                            webConsole.add(WebConsoleEntry("http", "HTTP $code ${errorResponse?.reasonPhrase.orEmpty()}", request?.url?.toString().orEmpty()))
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage ?: return false
                        webConsole.add(WebConsoleEntry(
                            consoleMessage.messageLevel().name.lowercase(),
                            consoleMessage.message(),
                            consoleMessage.sourceId().orEmpty(),
                            consoleMessage.lineNumber()
                        ))
                        if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                            mainHandler.post { statusText.text = "Preview runtime error • open console" }
                        }
                        return true
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (previewVisible && newProgress in 1..99) statusText.text = "Loading preview… $newProgress%"
                    }
                }
                setDownloadListener { url, _, _, _, _ ->
                    val target = url.orEmpty()
                    val parsed = Uri.parse(target)
                    if (parsed.scheme.equals("https", ignoreCase = true) && !parsed.host.equals(WebPreviewEngine.HOST, ignoreCase = true)) {
                        openExternalUrl(target)
                    } else toast("Workspace preview downloads are not exported automatically")
                }
            }
            previewCanvas.removeAllViews()
            previewCanvas.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            previewWebView = view
            applyWebPreviewSettings(reload = false)
            view
        }.getOrElse {
            showError("Android WebView unavailable", "Install or update Android System WebView/Chrome, then reopen NovaIDE.\n\n${it.message.orEmpty()}")
            null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applyWebPreviewSettings(reload: Boolean = true) {
        val config = webPreviewSettingsStore.load()
        webPreviewServer.allowExternalNetwork = config.allowExternalNetwork
        webPreviewServer.spaFallback = config.spaFallback
        previewWebView?.settings?.apply {
            javaScriptEnabled = config.javaScriptEnabled
            when (config.viewport) {
                PreviewViewport.RESPONSIVE -> {
                    userAgentString = defaultWebUserAgent
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    textZoom = 100
                }
                PreviewViewport.MOBILE -> {
                    userAgentString = defaultWebUserAgent
                    useWideViewPort = false
                    loadWithOverviewMode = false
                    textZoom = 100
                }
                PreviewViewport.DESKTOP -> {
                    userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 NovaIDE/1.0"
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    textZoom = 90
                }
            }
        }
        if (reload && previewVisible) reloadWebPreview()
    }

    private fun reloadWebPreview() {
        if (!previewVisible) { runUniversalProject(); return }
        val documentKind = previewDocumentKind
        val tab = activeTab
        if (documentKind != null && tab != null) {
            startGeneratedDocumentPreview(documentKind, tab.name, tab.content)
            return
        }
        syncWebPreviewOverrides()
        previewWebView?.reload()
    }

    private fun scheduleWebPreviewReload() {
        if (!previewVisible || !webPreviewSettingsStore.load().liveReload) return
        previewReloadTask?.let { mainHandler.removeCallbacks(it) }
        termuxResultPoll?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable {
            previewReloadTask = null
            syncWebPreviewOverrides()
            previewWebView?.reload()
        }
        previewReloadTask = task
        mainHandler.postDelayed(task, PREVIEW_RELOAD_DELAY_MS)
    }

    private fun syncWebPreviewOverrides() {
        if (!::webPreviewServer.isInitialized) return
        val overrides = openTabs.mapNotNull { tab ->
            val path = pathForUri(tab.uri) ?: return@mapNotNull null
            if (!WebPreviewEngine.isPreviewText(path)) return@mapNotNull null
            path to tab.content
        }.toMap()
        webPreviewServer.updateOverrides(overrides)
    }

    private fun pathForUri(uri: Uri): String? = workspaceIndex.firstOrNull { it.node.uri.toString() == uri.toString() }?.relativePath

    private fun showWebPreviewCenter() {
        val paths = workspaceIndex.filterNot { it.node.isDirectory }.map { it.relativePath }
        val activePath = activeTab?.uri?.let(::pathForUri)
        val plan = WebPreviewEngine.plan(paths, activePath)
        var settings = webPreviewSettingsStore.load()
        var dialog: AlertDialog? = null
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8))
            addView(Ui.text(this@MainActivity,
                "Static HTML/CSS/JavaScript runs from an isolated https://${WebPreviewEngine.HOST} origin. Unsaved web editor buffers can live-reload without being written first.",
                12f, palette.textSecondary).apply { setPadding(0, 4, 0, 10) })
            addView(Ui.text(this@MainActivity,
                plan.entryPath?.let { "Suggested entry: $it" } ?: (plan.warning ?: "No HTML entry detected"),
                13f, palette.textPrimary, bold = true).apply { setPadding(0, 4, 0, 10) })
        }
        fun checkbox(label: String, checked: Boolean, change: (Boolean) -> Unit): CheckBox = CheckBox(this).apply {
            text = label
            isChecked = checked
            setTextColor(palette.textPrimary)
            setOnCheckedChangeListener { _, value -> change(value) }
        }
        fun persist() {
            webPreviewSettingsStore.save(settings)
            applyWebPreviewSettings(reload = previewVisible)
        }
        container.addView(checkbox("Enable JavaScript", settings.javaScriptEnabled) { settings = settings.copy(javaScriptEnabled = it); persist() })
        container.addView(checkbox("Live reload while editing", settings.liveReload) { settings = settings.copy(liveReload = it); persist() })
        container.addView(checkbox("Allow external CDN/API resources", settings.allowExternalNetwork) { settings = settings.copy(allowExternalNetwork = it); persist() })
        container.addView(checkbox("SPA route fallback to entry HTML", settings.spaFallback) { settings = settings.copy(spaFallback = it); persist() })
        container.addView(Ui.text(this,
            "External network access is off by default. Sensitive files such as .env, credentials, keystores and .git content are never served.",
            11f, palette.textSecondary).apply { setPadding(0, 8, 0, 12) })
        container.addView(Button(this).apply { text = "▶ Run suggested entry"; isAllCaps = false; setOnClickListener { dialog?.dismissCompat(); runWebPreview(plan.entryPath) } })
        container.addView(Button(this).apply { text = "Choose HTML entry point"; isAllCaps = false; setOnClickListener { dialog?.dismissCompat(); chooseWebPreviewEntry() } })
        container.addView(Button(this).apply { text = "View console & runtime errors"; isAllCaps = false; setOnClickListener { showWebConsole() } })
        container.addView(Button(this).apply {
            text = "Clear preview cache, cookies & storage"
            isAllCaps = false
            setOnClickListener {
                previewWebView?.apply { clearCache(true); clearHistory(); clearFormData() }
                CookieManager.getInstance().removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
                webConsole.clear()
                toast("Web Preview data cleared")
            }
        })
        dialog = AlertDialog.Builder(this).setTitle("Web Preview · M10").setView(ScrollView(this).apply { addView(container) })
            .setNegativeButton("Close", null).create()
        dialog.show()
    }

    private fun chooseWebPreviewEntry() {
        val plan = WebPreviewEngine.plan(workspaceIndex.filterNot { it.node.isDirectory }.map { it.relativePath }, activeTab?.uri?.let(::pathForUri))
        if (plan.candidates.isEmpty()) {
            showError("No HTML entry", plan.warning ?: "Create an index.html file first.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Choose web entry")
            .setItems(plan.candidates.toTypedArray()) { _, which -> launchWebPreview(plan.candidates[which]) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPreviewViewportMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            PreviewViewport.entries.forEach { mode -> menu.add(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
            setOnMenuItemClickListener { item ->
                val mode = runCatching { PreviewViewport.valueOf(item.title.toString().uppercase()) }.getOrDefault(PreviewViewport.RESPONSIVE)
                val updated = webPreviewSettingsStore.load().copy(viewport = mode)
                webPreviewSettingsStore.save(updated)
                applyWebPreviewSettings(reload = previewVisible)
                true
            }
            show()
        }
    }

    private fun showWebConsole() {
        showSelectableTextDialog("Web Console", webConsole.render(), "Clear") {
            webConsole.clear()
            toast("Web console cleared")
        }
    }

    private fun openPreviewExternally() {
        val path = previewEntryPath ?: run { toast("Run a preview first"); return }
        if (WebPreviewEngine.isAllowedRuntimeUrl(path, webPreviewServer.allowedRuntimeOrigin)) {
            openExternalUrl(path)
            return
        }
        if (previewDocumentKind != null) {
            toast("Generated document previews stay inside NovaIDE")
            return
        }
        val resource = webPreviewServer.resource(path) ?: run { toast("Preview entry is unavailable"); return }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(resource.uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("NovaIDE web entry", resource.uri)
        }
        runCatching { startActivity(Intent.createChooser(intent, "Open HTML with…")) }
            .onFailure { toast("No external browser could open this document") }
    }

    private fun togglePreviewFullscreen() {
        if (!previewVisible) { runUniversalProject(); return }
        previewFullscreen = !previewFullscreen
        if (previewFullscreen) {
            filePaneBeforePreviewFullscreen = filePaneVisible
            if (filePaneVisible) toggleFilePane()
        } else if (filePaneBeforePreviewFullscreen && !filePaneVisible) {
            toggleFilePane()
        }
        applyPreviewLayout()
    }

    private fun closeWebPreview() {
        previewReloadTask?.let { mainHandler.removeCallbacks(it) }
        termuxResultPoll?.let { mainHandler.removeCallbacks(it) }
        previewReloadTask = null
        previewWebView?.stopLoading()
        previewWebView?.loadUrl("about:blank")
        if (previewFullscreen && filePaneBeforePreviewFullscreen && !filePaneVisible) toggleFilePane()
        previewFullscreen = false
        previewVisible = false
        previewEntryPath = null
        previewDocumentKind = null
        webPreviewServer.allowedRuntimeOrigin = null
        applyPreviewLayout()
        updateStatus()
    }

    private fun createWelcome(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(Ui.dp(this@MainActivity, 28), Ui.dp(this@MainActivity, 28), Ui.dp(this@MainActivity, 28), Ui.dp(this@MainActivity, 28))
        val logo = Ui.text(this@MainActivity, "N", 38f, palette.accent, bold = true, gravity = Gravity.CENTER).apply {
            background = Ui.rounded(palette.surfaceRaised, 18, this@MainActivity, palette.accent)
        }
        addView(logo, LinearLayout.LayoutParams(Ui.dp(this@MainActivity, 76), Ui.dp(this@MainActivity, 76)))
        addView(Ui.text(this@MainActivity, "NovaIDE", 27f, palette.textPrimary, bold = true, gravity = Gravity.CENTER).apply {
            setPadding(0, Ui.dp(this@MainActivity, 18), 0, 0)
        })
        addView(Ui.text(this@MainActivity, "A mobile-first workspace for serious projects.", 13f, palette.textSecondary, gravity = Gravity.CENTER).apply {
            setPadding(0, Ui.dp(this@MainActivity, 8), 0, Ui.dp(this@MainActivity, 22))
        })
        addView(Button(this@MainActivity).apply {
            text = "Open workspace"
            setTextColor(palette.textPrimary)
            background = Ui.rounded(palette.accent, 10, this@MainActivity)
            setOnClickListener { chooseWorkspace() }
        }, LinearLayout.LayoutParams(Ui.dp(this@MainActivity, 190), Ui.dp(this@MainActivity, 50)))
        addView(Ui.text(this@MainActivity, "No broad storage permission. Your folder stays under Android's secure access framework.", 11f, palette.textSecondary, gravity = Gravity.CENTER).apply {
            setPadding(Ui.dp(this@MainActivity, 20), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 20), 0)
        })
    }

    private fun toolbarButton(symbol: String, description: String, action: (View) -> Unit): TextView =
        Ui.text(this, symbol, 20f, palette.textPrimary, gravity = Gravity.CENTER).apply {
            contentDescription = description
            setOnClickListener(action)
        }

    private fun chooseWorkspace() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            store.workspaceUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }
        startActivityForResult(intent, PICK_WORKSPACE)
    }

    private fun openWorkspace(treeUri: Uri, restoreTabs: Boolean) {
        showProgress(true)
        treeLoadTask?.cancel(true)
        treeLoadTask = io.submit {
            try {
                val rootNode = repository.rootNode(treeUri)
                val firstLevel = repository.listChildren(rootNode)
                mainHandler.post {
                    workspaceRoot = rootNode
                    childrenByParent.clear()
                    childrenByParent[rootNode.uri.toString()] = firstLevel
                    rebuildVisibleTree()
                    workspaceTitle.text = rootNode.name
                    showProgress(false)
                    showEditorOrWelcome()
                    store.recordWorkspace(treeUri, rootNode.name, projectReport?.detection?.kind?.label ?: "Analyzing…")
                    if (restoreTabs) restoreSessionTabs()
                    buildProjectIndex(rootNode, treeUri)
                }
            } catch (error: Exception) {
                mainHandler.post {
                    showProgress(false)
                    store.workspaceUri = null
                    showError("Workspace unavailable", error.message ?: "Access may have been revoked.")
                    showWelcome()
                }
            }
        }
    }

    private fun reloadWorkspaceTree() {
        store.workspaceUri?.let { openWorkspace(it, restoreTabs = false) } ?: chooseWorkspace()
    }

    private fun rebuildVisibleTree() {
        val rootNode = workspaceRoot ?: return
        visibleNodes.clear()
        fun append(parent: DocumentNode) {
            val children = childrenByParent[parent.uri.toString()].orEmpty()
            children.forEach { child ->
                visibleNodes += child
                if (child.isDirectory && child.isExpanded) append(child)
            }
        }
        append(rootNode)
        fileAdapter.submit(visibleNodes.toList())
        statusText.text = "${visibleNodes.size} items"
    }

    private fun onFileNodeClick(node: DocumentNode) {
        if (node.isDirectory) toggleDirectory(node) else openDocument(node)
    }

    private fun toggleDirectory(node: DocumentNode) {
        node.isExpanded = !node.isExpanded
        if (node.isExpanded && !node.childrenLoaded) {
            node.isLoading = true
            rebuildVisibleTree()
            io.submit {
                val children = runCatching { repository.listChildren(node) }.getOrElse { emptyList() }
                mainHandler.post {
                    node.isLoading = false
                    node.childrenLoaded = true
                    childrenByParent[node.uri.toString()] = children
                    rebuildVisibleTree()
                }
            }
        } else {
            rebuildVisibleTree()
        }
    }

    private fun openDocument(node: DocumentNode, restoredState: WorkspaceStore.TabState? = null, activate: Boolean = true, afterOpen: ((EditorTab) -> Unit)? = null) {
        openTabs.firstOrNull { it.uri == node.uri }?.let {
            if (activate) activateTab(it)
            afterOpen?.invoke(it)
            return
        }
        if (openTabs.size >= MAX_OPEN_TABS) {
            toast("Close a tab first. NovaIDE keeps up to $MAX_OPEN_TABS active tabs.")
            return
        }
        if (!TextFileClassifier.isProbablyText(node.name, node.mimeType)) {
            showResourcePreview(node)
            return
        }
        showProgress(true)
        io.submit {
            try {
                val content = repository.readText(node.uri)
                val tab = EditorTab(
                    uri = node.uri,
                    name = node.name,
                    content = content,
                    savedContentHash = content.hashCode(),
                    cursorStart = restoredState?.cursorStart ?: 0,
                    cursorEnd = restoredState?.cursorEnd ?: 0,
                    scrollX = restoredState?.scrollX ?: 0,
                    scrollY = restoredState?.scrollY ?: 0,
                    lastKnownModified = node.lastModified
                )
                mainHandler.post {
                    openTabs += tab
                    showProgress(false)
                    if (activate) activateTab(tab) else renderTabs()
                    afterOpen?.invoke(tab)
                }
            } catch (error: Exception) {
                mainHandler.post {
                    showProgress(false)
                    showError("Cannot open ${node.name}", error.message ?: "Unknown file error")
                }
            }
        }
    }

    private fun activateTab(tab: EditorTab) {
        if (activeTab === tab) return
        snapshotActiveEditorState()
        val reopenSearch = searchPanel.visibility == View.VISIBLE
        activeTab = tab
        clearSearchState()
        editor.setDocument(tab.name, tab.content)
        val safeStart = tab.cursorStart.coerceIn(0, tab.content.length)
        val safeEnd = tab.cursorEnd.coerceIn(safeStart, tab.content.length)
        editor.setSelection(safeStart, safeEnd)
        editor.post { editor.scrollTo(tab.scrollX, tab.scrollY) }
        editor.isEnabled = !tab.isReadOnly
        showEditorOrWelcome()
        renderTabs()
        updateStatus()
        syncWebPreviewOverrides()
        persistSession()
        if (reopenSearch && searchPanel.query().isNotEmpty()) {
            scheduleSearch(searchPanel.query(), searchPanel.options())
        }
    }

    private fun requestCloseTab(tab: EditorTab) {
        if (tab.isDirty) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved changes")
                .setMessage("Save ${tab.name} before closing?")
                .setPositiveButton("Save") { _, _ -> saveTab(tab, silent = false) { closeTab(tab) } }
                .setNegativeButton("Discard") { _, _ -> closeTab(tab) }
                .setNeutralButton("Cancel", null)
                .show()
        } else closeTab(tab)
    }

    private fun closeTab(tab: EditorTab) {
        val index = openTabs.indexOf(tab)
        pendingAutosaves.remove(tab.uri.toString())?.let(mainHandler::removeCallbacks)
        openTabs.remove(tab)
        if (activeTab === tab) {
            activeTab = openTabs.getOrNull(index.coerceAtMost(openTabs.lastIndex))
            activeTab?.let(::activateTabAfterClose) ?: run {
                editor.setDocument("untitled.txt", "")
                clearSearchState()
                showEditorOrWelcome()
                renderTabs()
                updateStatus()
            }
        } else renderTabs()
        syncWebPreviewOverrides()
        persistSession()
    }

    private fun activateTabAfterClose(tab: EditorTab) {
        activeTab = null
        activateTab(tab)
    }

    private fun closeAllTabsWithoutPrompt() {
        pendingAutosaves.values.forEach(mainHandler::removeCallbacks)
        pendingAutosaves.clear()
        openTabs.clear()
        activeTab = null
        if (::editor.isInitialized) editor.setDocument("untitled.txt", "")
        if (::searchPanel.isInitialized) closeSearch()
        if (::webPreviewServer.isInitialized) webPreviewServer.updateOverrides(emptyMap())
    }

    private fun scheduleAutosave(tab: EditorTab) {
        val key = tab.uri.toString()
        pendingAutosaves.remove(key)?.let(mainHandler::removeCallbacks)
        if (!tab.isDirty) {
            saveStateText.text = "Saved"
            return
        }
        saveStateText.text = "Autosave…"
        val task = Runnable {
            pendingAutosaves.remove(key)
            saveTab(tab, silent = true)
        }
        pendingAutosaves[key] = task
        mainHandler.postDelayed(task, AUTOSAVE_DELAY_MS)
    }

    private fun saveActive(silent: Boolean) {
        snapshotActiveEditorState()
        val tab = activeTab
        if (tab != null) {
            saveTab(tab, silent)
        } else if (!silent) {
            toast("No file is open")
        }
    }

    private fun saveAllDirty(silent: Boolean) {
        snapshotActiveEditorState()
        openTabs.filter { it.isDirty }.forEach { saveTab(it, silent) }
    }

    private fun saveTab(tab: EditorTab, silent: Boolean, onSaved: (() -> Unit)? = null) {
        pendingAutosaves.remove(tab.uri.toString())?.let(mainHandler::removeCallbacks)
        if (tab.isReadOnly || !tab.isDirty) {
            onSaved?.invoke()
            return
        }
        val snapshot = tab.content
        if (activeTab === tab) saveStateText.text = "Saving…"
        io.submit {
            try {
                repository.writeText(tab.uri, snapshot)
                mainHandler.post {
                    if (tab.content == snapshot) tab.savedContentHash = snapshot.hashCode()
                    if (activeTab === tab) saveStateText.text = if (tab.isDirty) "Changed" else "Saved"
                    renderTabs()
                    syncWebPreviewOverrides()
                    scheduleWebPreviewReload()
                    if (!silent) toast("Saved ${tab.name}")
                    onSaved?.invoke()
                }
            } catch (error: Exception) {
                mainHandler.post {
                    if (activeTab === tab) saveStateText.text = "Save failed"
                    if (!silent) showError("Save failed", error.message ?: "Could not write file")
                }
            }
        }
    }

    private fun snapshotActiveEditorState() {
        val tab = activeTab ?: return
        tab.content = editor.text?.toString().orEmpty()
        tab.cursorStart = max(0, editor.selectionStart)
        tab.cursorEnd = max(tab.cursorStart, editor.selectionEnd)
        tab.scrollX = editor.scrollX
        tab.scrollY = editor.scrollY
    }

    private fun snapshotCursorOnly() {
        val tab = activeTab ?: return
        tab.cursorStart = max(0, editor.selectionStart)
        tab.cursorEnd = max(tab.cursorStart, editor.selectionEnd)
    }

    private fun renderTabs() {
        editorTabs.bind(openTabs, activeTab, palette, ::activateTab, ::requestCloseTab)
    }

    private fun updateStatus() {
        val tab = activeTab
        if (tab == null) {
            statusText.text = workspaceRoot?.let { "${visibleNodes.size} items" } ?: "Ready"
            saveStateText.text = ""
            return
        }
        val cursor = editor.selectionStart.coerceAtLeast(0)
        val source = editor.text?.toString().orEmpty()
        var line = 1
        var lineStart = 0
        for (index in 0 until cursor.coerceAtMost(source.length)) {
            if (source[index] == '\n') {
                line++
                lineStart = index + 1
            }
        }
        val column = cursor - lineStart + 1
        val mode = if (editor.isLargeFileMode) " • Performance mode" else ""
        val multi = editor.multiCursorCount.takeIf { it > 0 }?.let { " • $it cursors" }.orEmpty()
        statusText.text = "Ln $line, Col $column • ${editor.language.label} • ${source.length} chars$mode$multi"
        saveStateText.text = if (tab.isDirty) "Changed" else "Saved"
    }

    private fun persistSession() {
        snapshotActiveEditorState()
        store.saveSession(
            activeTab?.uri,
            openTabs.map {
                WorkspaceStore.TabState(it.uri, it.cursorStart, it.cursorEnd, it.scrollX, it.scrollY)
            }
        )
    }

    private fun restoreSessionTabs() {
        val state = store.restoreSession()
        if (state.tabs.isEmpty()) return
        showProgress(true)
        io.submit {
            val restored = state.tabs.take(MAX_OPEN_TABS).mapNotNull { saved ->
                val metadata = runCatching { repository.metadata(saved.uri) }.getOrNull() ?: return@mapNotNull null
                val node = DocumentNode(
                    uri = saved.uri,
                    name = metadata.name,
                    mimeType = metadata.mimeType,
                    isDirectory = false,
                    depth = 0,
                    size = metadata.size,
                    lastModified = metadata.lastModified
                )
                Triple(node, saved, saved.uri == state.activeUri)
            }
            mainHandler.post {
                showProgress(false)
                val hasRestorableActiveTab = restored.any { it.third }
                restored.forEachIndexed { index, (node, saved, wasActive) ->
                    val activate = wasActive || (!hasRestorableActiveTab && index == restored.lastIndex)
                    openDocument(node, saved, activate)
                }
            }
        }
    }

    private fun showCreateMenu(anchor: View) {
        val parent = selectedFolderUri()
        if (parent == null) {
            toast("Open a workspace first")
            return
        }
        PopupMenu(this, anchor).apply {
            menu.add("New file")
            menu.add("New folder")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "New file" -> promptCreate(parent, folder = false)
                    "New folder" -> promptCreate(parent, folder = true)
                }
                true
            }
            show()
        }
    }

    private fun selectedFolderUri(): Uri? = workspaceRoot?.uri

    private fun promptCreate(parent: Uri, folder: Boolean) {
        val input = EditText(this).apply {
            hint = if (folder) "Folder name" else "index.html"
            setSingleLine(true)
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8))
        }
        AlertDialog.Builder(this)
            .setTitle(if (folder) "New folder" else "New file")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty() || name.contains('/')) {
                    toast("Enter a valid name")
                    return@setPositiveButton
                }
                io.submit {
                    val created = if (folder) repository.createFolder(parent, name) else repository.createFile(parent, name, mimeForName(name))
                    mainHandler.post {
                        if (created == null) toast("Could not create $name")
                        else {
                            reloadWorkspaceTree()
                            if (!folder) {
                                val node = DocumentNode(created, name, mimeForName(name), false, 0)
                                openDocument(node)
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNodeMenu(node: DocumentNode, anchor: View) {
        PopupMenu(this, anchor).apply {
            if (node.isDirectory) {
                menu.add("New file here")
                menu.add("New folder here")
            } else if (WebPreviewEngine.isHtml(node.name)) {
                menu.add("Run in Web Preview")
            } else if (node.name.substringAfterLast('.', "").lowercase() in setOf("md", "markdown", "mmd", "mermaid")) {
                menu.add("Run document preview")
            }
            menu.add("Rename")
            menu.add("Delete")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "New file here" -> promptCreate(node.uri, false)
                    "New folder here" -> promptCreate(node.uri, true)
                    "Run in Web Preview" -> runWebPreview(pathForUri(node.uri) ?: node.name)
                    "Run document preview" -> previewDocumentNode(node)
                    "Rename" -> promptRename(node)
                    "Delete" -> confirmDelete(node)
                }
                true
            }
            show()
        }
    }

    private fun previewDocumentNode(node: DocumentNode) {
        val extension = node.name.substringAfterLast('.', "").lowercase()
        val kind = if (extension in setOf("mmd", "mermaid")) RuntimeKind.MERMAID else RuntimeKind.MARKDOWN
        val open = openTabs.firstOrNull { it.uri == node.uri }
        if (open != null) {
            if (activeTab?.uri != open.uri) activateTab(open)
            startGeneratedDocumentPreview(kind, open.name, open.content)
            return
        }
        showProgress(true)
        io.submit {
            val content = runCatching { repository.readText(node.uri, DocumentPreviewGenerator.MAX_SOURCE_CHARS.toLong()) }
            mainHandler.post {
                showProgress(false)
                content.onSuccess { startGeneratedDocumentPreview(kind, node.name, it) }
                    .onFailure { showError("Document preview failed", it.message ?: "Could not read ${node.name}") }
            }
        }
    }

    private fun promptRename(node: DocumentNode) {
        val openTab = openTabs.firstOrNull { it.uri == node.uri }
        if (openTab?.isDirty == true) {
            toast("Save or close ${node.name} before renaming")
            return
        }
        val input = EditText(this).apply {
            setText(node.name)
            setSelection(text.length)
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isBlank() || newName.contains('/')) return@setPositiveButton
                io.submit {
                    val updated = repository.rename(node.uri, newName)
                    mainHandler.post {
                        if (updated == null) toast("Rename failed")
                        else {
                            openTabs.firstOrNull { it.uri == node.uri }?.let(::closeTab)
                            reloadWorkspaceTree()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(node: DocumentNode) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${node.name}?")
            .setMessage(if (node.isDirectory) "This folder and everything inside it will be removed." else "This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                io.submit {
                    val deleted = repository.delete(node.uri)
                    mainHandler.post {
                        if (!deleted) toast("Delete failed")
                        else {
                            openTabs.firstOrNull { it.uri == node.uri }?.let(::closeTab)
                            reloadWorkspaceTree()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAppMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Project hub")
            menu.add("Open workspace")
            menu.add("Search workspace")
            menu.add("Universal Runtime")
            menu.add("Web Preview")
            menu.add("Git & GitHub")
            menu.add("Credentials Center")
            menu.add("AI Assistant")
            menu.add("Local Intelligence")
            menu.add("Debug & Analysis")
            menu.add("Command Palette")
            menu.add("Extensions")
            menu.add("Tasks & Nova Console")
            menu.add("Android Tools")
            menu.add("Import ZIP")
            menu.add("Export workspace ZIP")
            menu.add("New project from template")
            menu.add("Find and replace")
            menu.add("Go to line")
            menu.add("Go to symbol")
            menu.add("Autocomplete")
            menu.add("Insert snippet")
            menu.add(if (editor.multiCursorCount > 0) "Stop multi-edit" else "Edit all occurrences")
            menu.add("Fold block")
            menu.add("Unfold all")
            menu.add(if (minimapVisible) "Hide minimap" else "Show minimap")
            menu.add("Save all")
            menu.add("Close all tabs")
            menu.add("About M10")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Project hub" -> showProjectHub()
                    "Open workspace" -> chooseWorkspace()
                    "Search workspace" -> showWorkspaceSearch()
                    "Universal Runtime" -> showRuntimeCenter()
                    "Web Preview" -> showWebPreviewCenter()
                    "Git & GitHub" -> showGitHubCenter()
                    "Credentials Center" -> showCredentialsCenter()
                    "AI Assistant" -> showAiCenter()
                    "Local Intelligence" -> showLocalIntelligenceCenter()
                    "Debug & Analysis" -> showDiagnosticsCenter()
                    "Command Palette" -> showCommandPalette()
                    "Extensions" -> showExtensionsCenter()
                    "Tasks & Nova Console" -> showProductivityCenter()
                    "Android Tools" -> showAndroidCenter()
                    "Import ZIP" -> chooseZipImport()
                    "Export workspace ZIP" -> chooseZipExport()
                    "New project from template" -> showTemplatePicker()
                    "Find and replace" -> openSearch()
                    "Go to line" -> showGoToLine()
                    "Go to symbol" -> showSymbols()
                    "Autocomplete" -> showAutocomplete()
                    "Insert snippet" -> showSnippets()
                    "Edit all occurrences", "Stop multi-edit" -> toggleMultiOccurrenceEdit()
                    "Fold block" -> toggleFoldAtCursor()
                    "Unfold all" -> if (!editor.unfoldAll()) toast("No folded blocks")
                    "Hide minimap", "Show minimap" -> toggleMinimap()
                    "Save all" -> saveAllDirty(silent = false)
                    "Close all tabs" -> requestCloseAllTabs()
                    "About M10" -> showAbout()
                }
                true
            }
            show()
        }
    }

    private fun requestCloseAllTabs() {
        if (openTabs.any { it.isDirty }) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved tabs")
                .setMessage("Save all changed files before closing?")
                .setPositiveButton("Save all") { _, _ -> saveDirtyTabsAndClose() }
                .setNegativeButton("Discard") { _, _ -> finishCloseAllTabs() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finishCloseAllTabs()
        }
    }

    private fun saveDirtyTabsAndClose() {
        snapshotActiveEditorState()
        val snapshots = openTabs
            .filter { it.isDirty && !it.isReadOnly }
            .map { it to it.content }
        if (snapshots.isEmpty()) {
            finishCloseAllTabs()
            return
        }
        showProgress(true)
        saveStateText.text = "Saving…"
        io.submit {
            val saved = mutableListOf<Pair<EditorTab, String>>()
            var failure: Exception? = null
            for ((tab, content) in snapshots) {
                try {
                    repository.writeText(tab.uri, content)
                    saved += tab to content
                } catch (error: Exception) {
                    failure = error
                    break
                }
            }
            mainHandler.post {
                showProgress(false)
                saved.forEach { (tab, content) ->
                    if (tab.content == content) tab.savedContentHash = content.hashCode()
                }
                if (failure != null) {
                    renderTabs()
                    updateStatus()
                    showError("Save failed", failure.message ?: "Could not save every open file")
                } else {
                    finishCloseAllTabs()
                }
            }
        }
    }

    private fun finishCloseAllTabs() {
        closeAllTabsWithoutPrompt()
        showEditorOrWelcome()
        renderTabs()
        updateStatus()
        persistSession()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("NovaIDE 1.0.0 — M10")
            .setMessage(
                "Universal Runtime final release\n\n" +
                    "• In-app static HTML/CSS/JavaScript, PWA and WebAssembly preview\n" +
                    "• React, Vite, Vue, Svelte, Astro, Angular, Next and Nuxt detection\n" +
                    "• Generated dist/build/out output discovery\n" +
                    "• User-approved Termux bridge for Node, npm, pnpm, Yarn, Bun, Python, PHP, Hugo and Jekyll\n" +
                    "• Markdown and Mermaid document preview\n" +
                    "• Localhost development-server preview with explicit port isolation\n" +
                    "• Command output, exit-code and runtime error reporting\n" +
                    "• No silent shell execution; every command and working directory require confirmation\n" +
                    "• All M1–M9 editor, Git, Android, AI, diagnostics, plugins and Web Preview features retained\n\n" +
                    "External runtimes require Termux, its RUN_COMMAND permission, allow-external-apps=true, and a workspace stored in shared device storage.\n\n" +
                    "Developer: Mohnish Raj"
            )
            .setPositiveButton("Done", null)
            .show()
    }

    private fun toggleFilePane() {
        filePaneVisible = !filePaneVisible
        filePane.visibility = if (filePaneVisible) View.VISIBLE else View.GONE
        fileToggle.text = if (filePaneVisible) "☰" else "▤"
    }

    private fun showEditorOrWelcome() {
        val hasTab = activeTab != null
        editor.visibility = if (hasTab) View.VISIBLE else View.GONE
        welcome.visibility = if (hasTab) View.GONE else View.VISIBLE
    }

    private fun showWelcome() {
        showProgress(false)
        workspaceTitle.text = "NovaIDE"
        showEditorOrWelcome()
    }

    private fun showProgress(show: Boolean) {
        if (::progress.isInitialized) progress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun applyTheme() {
        palette = themeManager.palette
        window.statusBarColor = palette.window
        window.navigationBarColor = palette.window
        root.setBackgroundColor(palette.window)
        toolbar.setBackgroundColor(palette.surface)
        workspaceTitle.setTextColor(palette.textPrimary)
        filePane.setBackgroundColor(palette.surface)
        fileAdapter.applyPalette(palette)
        editorTabs.setBackgroundColor(palette.surface)
        searchPanel.applyPalette(palette)
        editor.applyPalette(palette)
        minimap.applyPalette(palette)
        welcome.setBackgroundColor(palette.editorBackground)
        updateWelcomeColors(welcome)
        statusText.setTextColor(palette.textSecondary)
        saveStateText.setTextColor(palette.textSecondary)
        renderTabs()
    }

    private fun updateWelcomeColors(view: View) {
        if (view is TextView) {
            when {
                view.text == "N" -> view.setTextColor(palette.accent)
                view.text == "NovaIDE" -> view.setTextColor(palette.textPrimary)
                view is Button -> view.setTextColor(palette.textPrimary)
                else -> view.setTextColor(palette.textSecondary)
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) updateWelcomeColors(view.getChildAt(i))
    }

    private fun openSearch() {
        if (activeTab == null) {
            toast("Open a text file first")
            return
        }
        if (resources.configuration.screenWidthDp < 600 && filePaneVisible) toggleFilePane()
        val initial = editor.selectedTextOrWord()
        searchPanel.open(initial)
    }

    private fun closeSearch() {
        if (!::searchPanel.isInitialized) return
        searchPanel.closeKeyboard()
        searchPanel.visibility = View.GONE
        clearSearchState()
    }

    private fun clearSearchState() {
        searchGeneration++
        searchTask?.cancel(true)
        searchMatches = emptyList()
        currentSearchIndex = -1
        currentSearchQuery = ""
        editor.clearSearchHighlights()
        if (::searchPanel.isInitialized) searchPanel.setResult(0, 0)
    }

    private fun scheduleSearch(query: String, options: SearchOptions) {
        currentSearchQuery = query
        currentSearchOptions = options
        val tab = activeTab
        if (tab == null || query.isEmpty()) {
            searchMatches = emptyList()
            currentSearchIndex = -1
            editor.clearSearchHighlights()
            searchPanel.setResult(0, 0)
            return
        }
        val snapshot = editor.text?.toString().orEmpty()
        val generation = ++searchGeneration
        searchTask?.cancel(true)
        searchPanel.setResult(0, 0, "…")
        searchTask = editorAnalysis.submit {
            val result = SearchEngine.findAll(snapshot, query, options)
            mainHandler.post {
                if (generation != searchGeneration || activeTab !== tab || editor.text?.toString() != snapshot) return@post
                if (result.error != null) {
                    searchMatches = emptyList()
                    currentSearchIndex = -1
                    editor.clearSearchHighlights()
                    searchPanel.setResult(0, 0, "Regex")
                    return@post
                }
                searchMatches = result.matches
                currentSearchIndex = if (searchMatches.isEmpty()) -1 else {
                    val cursor = editor.selectionStart.coerceAtLeast(0)
                    searchMatches.indexOfFirst { it.start >= cursor }.takeIf { it >= 0 } ?: 0
                }
                editor.showSearchMatches(searchMatches, currentSearchIndex)
                if (currentSearchIndex >= 0) searchPanel.setResult(currentSearchIndex + 1, searchMatches.size)
                else searchPanel.setResult(0, 0, if (result.truncated) "5000+" else null)
            }
        }
    }

    private fun moveSearch(direction: Int) {
        if (searchMatches.isEmpty()) {
            scheduleSearch(searchPanel.query(), searchPanel.options())
            return
        }
        currentSearchIndex = (currentSearchIndex + direction + searchMatches.size) % searchMatches.size
        selectSearchMatch(currentSearchIndex)
    }

    private fun selectSearchMatch(index: Int) {
        val range = searchMatches.getOrNull(index) ?: return
        currentSearchIndex = index
        editor.showSearchMatches(searchMatches, currentSearchIndex)
        editor.requestFocus()
        editor.setSelection(range.start.coerceAtMost(editor.length()), range.endExclusive.coerceAtMost(editor.length()))
        editor.bringPointIntoView(range.start)
        searchPanel.setResult(index + 1, searchMatches.size)
    }

    private fun replaceCurrentSearchMatch(replacement: String) {
        val range = searchMatches.getOrNull(currentSearchIndex) ?: run {
            toast("No match selected")
            return
        }
        val source = editor.text?.toString().orEmpty()
        val value = SearchEngine.replacementForMatch(
            source,
            range,
            currentSearchQuery,
            replacement,
            currentSearchOptions
        )
        editor.replaceRange(range.start, range.endExclusive, value)
    }

    private fun replaceAllSearchMatches(replacement: String) {
        val tab = activeTab ?: return
        val source = editor.text?.toString().orEmpty()
        val query = currentSearchQuery
        val options = currentSearchOptions
        if (query.isEmpty()) return
        val generation = ++searchGeneration
        searchPanel.setResult(0, 0, "…")
        searchTask?.cancel(true)
        searchTask = editorAnalysis.submit {
            val result = SearchEngine.replaceAll(source, query, replacement, options)
            mainHandler.post {
                if (generation != searchGeneration || activeTab !== tab || editor.text?.toString() != source) return@post
                val replaceError = result.error
                if (replaceError != null) {
                    searchPanel.setResult(0, 0, if (replaceError.startsWith("Replace All")) "Limit" else "Regex")
                    toast(replaceError)
                } else if (result.replacementCount == 0) {
                    toast("No matches to replace")
                    scheduleSearch(query, options)
                } else {
                    editor.replaceAllUserText(result.text)
                    toast("Replaced ${result.replacementCount} matches")
                }
            }
        }
    }

    private fun showGoToLine() {
        if (activeTab == null) {
            toast("Open a file first")
            return
        }
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Line number"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Go to line")
            .setView(input)
            .setPositiveButton("Go") { _, _ ->
                val requested = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                val source = editor.text?.toString().orEmpty()
                val targetLine = requested.coerceAtLeast(1)
                var line = 1
                var offset = 0
                while (line < targetLine && offset < source.length) {
                    val next = source.indexOf('\n', offset)
                    if (next < 0) break
                    offset = next + 1
                    line++
                }
                editor.requestFocus()
                editor.setSelection(offset.coerceIn(0, source.length))
                editor.bringPointIntoView(offset)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSymbols() {
        val tab = activeTab ?: run {
            toast("Open a file first")
            return
        }
        val source = editor.text?.toString().orEmpty()
        showProgress(true)
        editorAnalysis.submit {
            val symbols = SymbolExtractor.extract(tab.name, source)
            mainHandler.post {
                showProgress(false)
                if (activeTab !== tab) return@post
                if (symbols.isEmpty()) {
                    toast("No navigable symbols found")
                    return@post
                }
                val labels = symbols.map { "${it.kind.uppercase()}  ${it.name}   · L${it.line}" }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Symbols in ${tab.name}")
                    .setItems(labels) { _, which ->
                        val symbol = symbols[which]
                        editor.requestFocus()
                        editor.setSelection(symbol.offset.coerceIn(0, editor.length()))
                        editor.bringPointIntoView(symbol.offset)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun toggleMultiOccurrenceEdit() {
        if (activeTab == null) {
            toast("Open a file first")
            return
        }
        if (editor.multiCursorCount > 0) {
            editor.disableMultiOccurrenceEdit()
            toast("Multi-edit stopped")
        } else {
            val count = editor.enableMultiOccurrenceEdit()
            if (count < 2) toast("Select a repeated word or symbol first")
            else toast("Editing $count occurrences together")
        }
        updateStatus()
    }

    private fun toggleFoldAtCursor() {
        if (activeTab == null) {
            toast("Open a file first")
            return
        }
        when (editor.toggleFoldAtCursor()) {
            true -> toast("Block folded")
            false -> toast("Block unfolded")
            null -> toast("Place cursor inside a multi-line brace block")
        }
    }

    private fun toggleMinimap() {
        minimapVisible = !minimapVisible
        minimap.visibility = if (minimapVisible) View.VISIBLE else View.GONE
        getSharedPreferences("nova_editor", MODE_PRIVATE)
            .edit()
            .putBoolean("minimap_visible", minimapVisible)
            .apply()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_S -> saveActive(silent = false)
                KeyEvent.KEYCODE_F -> if (event.shiftPressedCompat()) showWorkspaceSearch() else openSearch()
                43 /* KEYCODE_O */ -> showProjectHub()
                KeyEvent.KEYCODE_G -> if (event.shiftPressedCompat()) showGitHubCenter() else showGoToLine()
                KeyEvent.KEYCODE_P -> if (event.shiftPressedCompat()) showCommandPalette() else showSymbols()
                KeyEvent.KEYCODE_SPACE -> showAutocomplete()
                KeyEvent.KEYCODE_R -> if (event.shiftPressedCompat()) runUniversalProject() else if (previewVisible) reloadWebPreview() else return super.dispatchKeyEvent(event)
                KeyEvent.KEYCODE_I -> if (event.shiftPressedCompat()) showAiCenter() else return super.dispatchKeyEvent(event)
                KeyEvent.KEYCODE_D -> toggleMultiOccurrenceEdit()
                KeyEvent.KEYCODE_W -> activeTab?.let(::requestCloseTab)
                else -> return super.dispatchKeyEvent(event)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }


    private fun persistUriPermission(uri: Uri, flags: Int) {
        val read = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        val write = flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0
        runCatching {
            when {
                read && write -> contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                read -> contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                write -> contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    }

    private fun requestWorkspaceSwitch(uri: Uri) {
        if (gitMutationWorkspace != null) {
            toast("Finish the active GitHub write operation before switching workspace")
            return
        }
        snapshotActiveEditorState()
        val dirty = openTabs.filter { it.isDirty && !it.isReadOnly }
        if (dirty.isEmpty()) {
            switchWorkspaceNow(uri)
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Unsaved changes")
            .setMessage("Save ${dirty.size} changed file${if (dirty.size == 1) "" else "s"} before switching workspace?")
            .setPositiveButton("Save & switch") { _, _ -> saveDirtyTabsThen(dirty) { switchWorkspaceNow(uri) } }
            .setNegativeButton("Discard & switch") { _, _ -> switchWorkspaceNow(uri) }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun saveDirtyTabsThen(tabs: List<EditorTab>, onComplete: () -> Unit) {
        val snapshots = tabs.map { it to it.content }
        showProgress(true)
        io.submit {
            var failure: Exception? = null
            val saved = mutableListOf<Pair<EditorTab, String>>()
            for ((tab, content) in snapshots) {
                try {
                    repository.writeText(tab.uri, content)
                    saved += tab to content
                } catch (error: Exception) {
                    failure = error
                    break
                }
            }
            mainHandler.post {
                showProgress(false)
                saved.forEach { (tab, content) -> if (tab.content == content) tab.savedContentHash = content.hashCode() }
                if (failure == null) onComplete() else showError("Save failed", failure.message ?: "Could not save every file")
            }
        }
    }

    private fun switchWorkspaceNow(uri: Uri) {
        snapshotActiveEditorState()
        persistSession()
        closeWebPreview()
        closeAllTabsWithoutPrompt()
        workspaceIndex = emptyList()
        projectReport = null
        store.workspaceUri = uri
        openWorkspace(uri, restoreTabs = true)
    }

    private fun buildProjectIndex(rootNode: DocumentNode, treeUri: Uri) {
        projectIo.submit {
            val scan = runCatching { repository.scan(rootNode) }.getOrNull() ?: return@submit
            val hints = mutableMapOf<String, String>()
            for (entry in scan.entries) {
                val normalized = entry.relativePath.lowercase()
                if (normalized in setOf("package.json", "pubspec.yaml", "build.gradle.kts", "build.gradle")) {
                    runCatching { repository.readText(entry.node.uri, 128 * 1024L) }.getOrNull()?.let { hints[normalized] = it }
                }
            }
            val gitConfig = scan.entries.firstOrNull { it.relativePath.equals(".git/config", ignoreCase = true) }
                ?.let { runCatching { repository.readText(it.node.uri, 128 * 1024L) }.getOrNull() }
            val gitHead = scan.entries.firstOrNull { it.relativePath.equals(".git/HEAD", ignoreCase = true) }
                ?.let { runCatching { repository.readText(it.node.uri, 16 * 1024L) }.getOrNull() }
            val detectedRemote = gitConfig?.let(GitUrlParser::parseRemoteConfig)?.let { parsed ->
                val branch = gitHead?.trim()?.removePrefix("ref: refs/heads/")
                    ?.takeIf(GitUrlParser::isValidRef)
                if (branch == null) parsed else parsed.copy(branch = branch)
            }
            val detection = ProjectDetector.detect(scan.entries.map { it.relativePath }, hints)
            val report = ProjectReport(
                detection,
                scan.entries.count { !it.node.isDirectory },
                scan.folderCount,
                scan.totalBytes,
                scan.truncated
            )
            mainHandler.post {
                if (store.workspaceUri != treeUri) return@post
                workspaceIndex = scan.entries
                webPreviewServer.updateWorkspace(workspaceIndex)
                syncWebPreviewOverrides()
                projectReport = report
                if (gitHubStore.repository(treeUri) == null && detectedRemote != null) {
                    gitHubStore.saveRepository(treeUri, detectedRemote)
                }
                store.recordWorkspace(treeUri, rootNode.name, detection.kind.label)
                statusText.text = "${detection.kind.label} • ${report.fileCount} files"
            }
        }
    }

    private fun showProjectHub() {
        var hubDialog: AlertDialog? = null
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8))
        }
        projectReport?.let { report ->
            container.addView(Ui.text(this, "${report.detection.kind.badge}  ${report.detection.kind.label}", 17f, palette.textPrimary, bold = true))
            container.addView(Ui.text(this, "${report.fileCount} files • ${report.folderCount} folders • ${report.formattedSize}${if (report.truncated) " • index capped" else ""}", 12f, palette.textSecondary).apply { setPadding(0, 6, 0, 12) })
            container.addView(Ui.text(this, "Detected from: ${report.detection.evidence.joinToString()}", 11f, palette.textSecondary).apply { setPadding(0, 0, 0, 14) })
        } ?: container.addView(Ui.text(this, if (workspaceRoot == null) "No workspace open" else "Analyzing current workspace…", 14f, palette.textSecondary).apply { setPadding(0, 8, 0, 12) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun action(label: String, block: () -> Unit): Button = Button(this).apply { text = label; setOnClickListener { hubDialog?.dismissCompat(); block() } }
        actions.addView(action("Open") { chooseWorkspace() }, LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f))
        actions.addView(action("Search") { showWorkspaceSearch() }, LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f))
        actions.addView(action("Git") { showGitHubCenter() }, LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f))
        actions.addView(action("Template") { showTemplatePicker() }, LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f))
        container.addView(actions)
        container.addView(Ui.text(this, "RECENT WORKSPACES", 11f, palette.textSecondary, bold = true).apply { setPadding(0, Ui.dp(this@MainActivity, 18), 0, Ui.dp(this@MainActivity, 6)) })
        val recents = store.recentWorkspaces()
        if (recents.isEmpty()) container.addView(Ui.text(this, "No recent workspaces yet.", 12f, palette.textSecondary))
        recents.forEach { recent ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val label = Ui.text(this, "${if (recent.favorite) "★" else "☆"}  ${recent.name}\n${recent.kind}", 13f, palette.textPrimary).apply {
                setPadding(0, Ui.dp(this@MainActivity, 8), 0, Ui.dp(this@MainActivity, 8))
                setOnClickListener { hubDialog?.dismissCompat(); requestWorkspaceSwitch(recent.uri) }
                setOnLongClickListener { toast(if (store.toggleFavorite(recent.uri)) "Added to favorites" else "Removed from favorites"); true }
            }
            row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val star = toolbarButton(if (recent.favorite) "★" else "☆", "Favorite workspace") { view ->
                val favorite = store.toggleFavorite(recent.uri)
                (view as TextView).text = if (favorite) "★" else "☆"
                label.text = "${if (favorite) "★" else "☆"}  ${recent.name}\n${recent.kind}"
                toast(if (favorite) "Added to favorites" else "Removed from favorites")
            }
            row.addView(star, sizeParams(44, 44))
            container.addView(row)
            container.addView(Ui.divider(this, palette))
        }
        val scroll = ScrollView(this).apply { addView(container) }
        hubDialog = AlertDialog.Builder(this).setTitle("Project Hub").setView(scroll).setNegativeButton("Close", null).show()
    }

    private fun chooseZipImport() {
        if (workspaceRoot == null) { toast("Open a destination workspace first"); return }
        startActivityForResult(Intent("android.intent.action.OPEN_DOCUMENT").apply {
            addOpenableCategoryCompat()
            setMimeTypeCompat("application/zip")
            putExtra("android.intent.extra.MIME_TYPES", arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, PICK_ZIP)
    }

    private fun importZip(zipUri: Uri) {
        val rootNode = workspaceRoot ?: return
        val name = runCatching { repository.metadata(zipUri).name }.getOrDefault("ImportedProject.zip")
        showProgress(true)
        statusText.text = "Importing ZIP…"
        projectIo.submit {
            val result = runCatching { ZipWorkspaceManager(repository).importZip(zipUri, rootNode.uri, name) }
            mainHandler.post {
                showProgress(false)
                result.onSuccess {
                    toast("Imported ${it.files} files into ${it.folderName}")
                    reloadWorkspaceTree()
                }.onFailure { showError("ZIP import failed", it.message ?: "Could not import archive") }
            }
        }
    }

    private fun chooseZipExport() {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        pendingExportName = "${rootNode.name.replace(Regex("[^A-Za-z0-9._-]"), "-")}-NovaIDE.zip"
        startActivityForResult(Intent("android.intent.action.CREATE_DOCUMENT").apply {
            addOpenableCategoryCompat()
            setMimeTypeCompat("application/zip")
            putExtra("android.intent.extra.TITLE", pendingExportName)
        }, CREATE_ZIP)
    }

    private fun exportWorkspace(destination: Uri) {
        val rootNode = workspaceRoot ?: return
        snapshotActiveEditorState()
        pendingAutosaves.values.forEach(mainHandler::removeCallbacks)
        pendingAutosaves.clear()
        val dirtySnapshots = openTabs.filter { it.isDirty && !it.isReadOnly }.map { it to it.content }
        showProgress(true)
        statusText.text = "Saving and exporting workspace…"
        io.submit {
            val result = runCatching {
                for ((tab, content) in dirtySnapshots) repository.writeText(tab.uri, content)
                ZipWorkspaceManager(repository).exportWorkspace(rootNode, destination)
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess {
                    dirtySnapshots.forEach { (tab, content) -> if (tab.content == content) tab.savedContentHash = content.hashCode() }
                    renderTabs(); updateStatus()
                    toast("Exported ${it.files} files")
                }.onFailure { showError("ZIP export failed", it.message ?: "Could not export workspace") }
            }
        }
    }

    private fun showTemplatePicker() {
        val rootNode = workspaceRoot ?: run { toast("Open a destination workspace first"); return }
        val labels = TemplateCatalog.all.map { "${it.name}\n${it.description}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("New project from template").setItems(labels) { _, which ->
            val template = TemplateCatalog.all[which]
            val input = EditText(this).apply { hint = "Project folder name"; setText(template.name.replace(" ", "-")) }
            AlertDialog.Builder(this).setTitle(template.name).setView(input)
                .setPositiveButton("Create") { _, _ ->
                    val folder = input.text.toString()
                    showProgress(true)
                    projectIo.submit {
                        val result = runCatching { TemplateInstaller(repository).install(rootNode.uri, folder, template) }
                        mainHandler.post {
                            showProgress(false)
                            result.onSuccess { toast("Created ${it.projectFolder} with ${it.filesCreated} files"); reloadWorkspaceTree() }
                                .onFailure { showError("Template failed", it.message ?: "Could not create project") }
                        }
                    }
                }.setNegativeButton("Cancel", null).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showWorkspaceSearch() {
        if (workspaceRoot == null) { toast("Open a workspace first"); return }
        if (workspaceIndex.isEmpty()) { toast("Project index is still building"); return }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 8, 28, 0) }
        val query = EditText(this).apply { hint = "Search file names and contents"; setSingleLine(true) }
        val caseSensitive = CheckBox(this).apply { text = "Match case" }
        val regex = CheckBox(this).apply { text = "Regular expression" }
        val generated = CheckBox(this).apply { text = "Include generated folders" }
        box.addView(query); box.addView(caseSensitive); box.addView(regex); box.addView(generated)
        AlertDialog.Builder(this).setTitle("Search workspace").setView(box)
            .setPositiveButton("Search") { _, _ ->
                val text = query.text.toString()
                if (text.isBlank()) toast("Enter a search query")
                else runWorkspaceSearch(text, WorkspaceSearchOptions(caseSensitive = caseSensitive.isChecked, regex = regex.isChecked, includeGenerated = generated.isChecked))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runWorkspaceSearch(query: String, options: WorkspaceSearchOptions) {
        showProgress(true)
        statusText.text = "Searching workspace…"
        val snapshot = workspaceIndex
        projectIo.submit {
            val result = WorkspaceSearchEngine(repository).search(snapshot, query, options)
            mainHandler.post {
                showProgress(false)
                result.error?.let { searchError -> showError("Search error", searchError); return@post }
                if (result.hits.isEmpty()) { toast("No workspace matches"); return@post }
                val labels = result.hits.map {
                    val where = if (it.fileNameMatch) "file name" else "L${it.line}:${it.column}"
                    "${it.entry.relativePath}  •  $where\n${it.preview}"
                }.toTypedArray()
                AlertDialog.Builder(this).setTitle("${result.hits.size}${if (result.truncated) "+" else ""} results")
                    .setItems(labels) { _, which -> openWorkspaceSearchHit(result.hits[which]) }
                    .setNegativeButton("Close", null).show()
            }
        }
    }

    private fun openWorkspaceSearchHit(hit: WorkspaceSearchHit) {
        openDocument(hit.entry.node, activate = true) { tab ->
            activateTab(tab)
            if (hit.line > 0) goToLine(hit.line, hit.column)
        }
    }

    private fun goToLine(lineNumber: Int, columnNumber: Int = 1) {
        val source = editor.text?.toString().orEmpty()
        var line = 1
        var offset = 0
        while (line < lineNumber && offset < source.length) {
            val next = source.indexOf('\n', offset)
            if (next < 0) break
            offset = next + 1
            line++
        }
        val lineEnd = source.indexOf('\n', offset).let { if (it < 0) source.length else it }
        offset = (offset + columnNumber - 1).coerceIn(offset, lineEnd)
        editor.requestFocus(); editor.setSelection(offset); editor.bringPointIntoView(offset)
    }

    private fun showResourcePreview(node: DocumentNode) {
        showProgress(true)
        projectIo.submit {
            val preview = runCatching { ResourcePreviewer(this, repository).load(node) }
            mainHandler.post {
                showProgress(false)
                preview.onSuccess { showLoadedPreview(node.name, it) }
                    .onFailure { showError("Preview failed", it.message ?: "Unsupported resource") }
            }
        }
    }

    private fun showLoadedPreview(name: String, preview: ResourcePreview) {
        when (preview) {
            is ResourcePreview.Image -> {
                val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 8) }
                column.addView(ImageView(this).apply {
                    setImageBitmap(preview.bitmap)
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 360)))
                column.addView(Ui.text(this, preview.details, 12f, palette.textSecondary, gravity = Gravity.CENTER).apply { setPadding(0, 12, 0, 4) })
                AlertDialog.Builder(this).setTitle(name).setView(column).setPositiveButton("Close", null).show()
            }
            is ResourcePreview.Details -> {
                val text = Ui.text(this, preview.lines.joinToString("\n"), 12f, palette.textPrimary, gravity = Gravity.START).apply {
                    setPadding(28, 16, 28, 16); typeface = Typeface.MONOSPACE
                }
                AlertDialog.Builder(this).setTitle(preview.title).setView(ScrollView(this).apply { addView(text) }).setPositiveButton("Close", null).show()
            }
        }
    }

    private fun showGitHubCenter() {
        val workspaceUri = store.workspaceUri ?: run { toast("Open a workspace first"); return }
        val remote = gitHubStore.repository(workspaceUri)
        if (remote == null) {
            showGitHubSetup(null)
            return
        }
        var dialog: AlertDialog? = null
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8))
        }
        box.addView(Ui.text(this, "◉  ${remote.slug}", 17f, palette.textPrimary, bold = true))
        box.addView(Ui.text(this, "Branch: ${remote.branch}  •  ${if (gitHubStore.hasToken()) "Token protected by Android Keystore" else "Public read-only mode"}", 11.5f, palette.textSecondary).apply {
            setPadding(0, Ui.dp(this@MainActivity, 5), 0, Ui.dp(this@MainActivity, 12))
        })
        val baseline = gitSnapshots.load(workspaceUri)
        box.addView(Ui.text(
            this,
            if (baseline == null) "No synchronization baseline yet" else "Baseline: ${baseline.commitSha?.take(8) ?: "local"} • ${baseline.entries.size} tracked files",
            11.5f,
            if (baseline == null) palette.danger else palette.textSecondary
        ).apply { setPadding(0, 0, 0, Ui.dp(this@MainActivity, 12)) })

        fun actionRow(left: String, leftAction: () -> Unit, right: String, rightAction: () -> Unit) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fun button(label: String, action: () -> Unit) = Button(this).apply {
                text = label
                setOnClickListener { dialog?.dismiss(); action() }
            }
            row.addView(button(left, leftAction), LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1f))
            row.addView(button(right, rightAction), LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1f))
            box.addView(row)
        }
        actionRow("Status & Diff", { showGitStatus() }, "Commit & Push", { beginCommitAndPush() })
        actionRow("Pull / Clone", { requestPull(remote) }, "Branches", { showGitBranches() })
        actionRow("History", { showGitHistory() }, "Actions", { showGitHubActions() })
        actionRow("Merge Helper", { showMergeConflicts() }, "Settings", { showGitHubSetup(remote) })
        if (baseline == null) {
            box.addView(Button(this).apply {
                text = "Initial commit / initialize sync"
                setOnClickListener {
                    dialog?.dismiss()
                    checkRemoteInitialization(remote)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 52)))
        }
        dialog = AlertDialog.Builder(this)
            .setTitle("Git & GitHub")
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showGitHubSetup(existing: GitHubRepository?) {
        val workspaceUri = store.workspaceUri ?: run { toast("Open a workspace first"); return }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 24), Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 24), 0)
        }
        val url = EditText(this).apply {
            hint = "https://github.com/owner/repository"
            setSingleLine(true)
            setText(existing?.webUrl.orEmpty())
        }
        val branch = EditText(this).apply {
            hint = "Branch (blank = repository default)"
            setSingleLine(true)
            setText(existing?.branch.orEmpty())
        }
        val token = EditText(this).apply {
            hint = if (gitHubStore.hasToken()) "New token (blank keeps saved token)" else "Fine-grained GitHub token (optional for public read)"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val clearToken = CheckBox(this).apply {
            text = "Remove saved GitHub token"
            visibility = if (gitHubStore.hasToken()) View.VISIBLE else View.GONE
        }
        box.addView(url)
        box.addView(branch)
        box.addView(token)
        box.addView(clearToken)
        box.addView(Button(this).apply {
            text = "Create GitHub API token"
            setOnClickListener { openExternalUrl(CredentialCatalog.github.createUrl.orEmpty()) }
        })
        box.addView(Button(this).apply {
            text = "Open Credentials Center"
            setOnClickListener { showCredentialsCenter() }
        })
        box.addView(Ui.text(this,
            "Tip: choose only the repositories NovaIDE should manage, then enable Metadata read, Contents read/write and Actions read. Add Workflows write only when you plan to edit workflow files.\n\nDon't worry: your token is encrypted with Android Keystore and stored only on this device. It is sent over HTTPS only to GitHub when you request a GitHub action.\n\nLeave Branch blank to auto-detect the repository's real default branch.",
            11f, palette.textSecondary
        ).apply { setPadding(0, Ui.dp(this@MainActivity, 10), 0, 0) })
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Connect GitHub repository" else "GitHub settings")
            .setView(box)
            .setPositiveButton("Connect") { _, _ ->
                val requestedBranch = branch.text.toString().trim()
                if (requestedBranch.isNotBlank() && !GitUrlParser.isValidRef(requestedBranch)) {
                    showError("Invalid branch", "Enter a valid Git branch name or leave it blank for automatic default-branch detection.")
                    return@setPositiveButton
                }
                val inferredTreeBranch = if (requestedBranch.isBlank()) GitUrlParser.branchFromTreeUrl(url.text.toString()) else null
                val effectiveRequestedBranch = requestedBranch.ifBlank { inferredTreeBranch.orEmpty() }
                val parsed = GitUrlParser.parse(url.text.toString(), effectiveRequestedBranch.ifBlank { "main" })
                if (parsed == null) {
                    showError("Invalid repository", "Enter an owner/repository name or a valid GitHub HTTPS/SSH URL.")
                    return@setPositiveButton
                }
                val rawToken = token.text.toString()
                val candidateToken = try {
                    when {
                        clearToken.isChecked -> null
                        rawToken.isNotBlank() -> gitHubStore.normalizedToken(rawToken)
                        else -> gitHubStore.token()
                    }
                } catch (error: Exception) {
                    showError("Invalid GitHub token", error.message ?: "The token format is invalid")
                    return@setPositiveButton
                }
                verifyGitHubConnection(
                    remote = parsed,
                    requestedBranch = effectiveRequestedBranch.ifBlank { null },
                    candidateToken = candidateToken,
                    saveNewToken = rawToken.isNotBlank(),
                    clearSavedToken = clearToken.isChecked,
                    previous = existing,
                    promptSync = existing == null || existing.owner != parsed.owner || existing.name != parsed.name
                )
            }
            .setNeutralButton(if (existing == null) "Cancel" else "Disconnect") { _, _ ->
                if (existing != null) {
                    gitHubStore.clearRepository(workspaceUri)
                    gitSnapshots.clear(workspaceUri)
                    toast("GitHub repository disconnected")
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun verifyGitHubConnection(
        remote: GitHubRepository,
        requestedBranch: String?,
        candidateToken: String?,
        saveNewToken: Boolean,
        clearSavedToken: Boolean,
        previous: GitHubRepository?,
        promptSync: Boolean
    ) {
        val workspaceUri = store.workspaceUri ?: return
        showProgress(true)
        statusText.text = "Checking ${remote.slug}…"
        projectIo.submit {
            val result = runCatching {
                val client = GitHubApiClient(candidateToken)
                val user = client.authenticatedUser()
                val (info, resolved) = client.resolveBranch(remote, requestedBranch)
                Triple(info, resolved, user)
            }
            mainHandler.post {
                showProgress(false)
                if (store.workspaceUri != workspaceUri) return@post
                result.onSuccess { (info, resolved, user) ->
                    try {
                        when {
                            clearSavedToken -> gitHubStore.clearToken()
                            saveNewToken && candidateToken != null -> gitHubStore.saveToken(candidateToken)
                        }
                    } catch (error: Exception) {
                        showError("Token storage failed", error.message ?: "Android Keystore could not protect the token")
                        return@onSuccess
                    }
                    if (previous != null && previous != resolved) gitSnapshots.clear(workspaceUri)
                    gitHubStore.saveRepository(workspaceUri, resolved)
                    val identity = user?.login?.let { " as @$it" }.orEmpty()
                    statusText.text = "GitHub connected • ${info.fullName} • ${resolved.branch}"
                    toast("Connected to ${info.fullName}$identity")
                    if (candidateToken != null && !info.canPush) {
                        showError(
                            "GitHub connected read-only",
                            "NovaIDE can read this repository, but GitHub did not grant push access. Commit, delete, branch-write and similar requests will be blocked clearly. Recreate the fine-grained token with Contents read/write for this repository and confirm your account has push permission."
                        )
                    }
                    if (requestedBranch.isNullOrBlank() && resolved.branch == info.defaultBranch) {
                        toast("Using default branch: ${resolved.branch}")
                    }
                    if (promptSync || previous != resolved) promptInitialGitSync(resolved, info.isEmpty)
                }.onFailure { error ->
                    statusText.text = "GitHub connection failed"
                    showError("GitHub connection failed", error.message ?: "Could not access the repository")
                }
            }
        }
    }

    private fun promptInitialGitSync(remote: GitHubRepository, isEmpty: Boolean) {
        if (isEmpty) {
            AlertDialog.Builder(this)
                .setTitle("Empty repository detected")
                .setMessage(
                    "This GitHub repository has no branch or commit yet. NovaIDE can create the first commit directly from the current workspace.\n\n" +
                        "Git does not store empty folders, so add at least one file such as README.md before committing."
                )
                .setPositiveButton("Create Initial Commit") { _, _ -> beginCommitAndPush() }
                .setNegativeButton("Later", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Initialize synchronization")
            .setMessage(
                "Pull / Clone downloads the selected branch into this workspace and creates an accurate baseline.\n\n" +
                    "Trust Current is intended only when these files already exactly match the GitHub branch."
            )
            .setPositiveButton("Pull / Clone") { _, _ -> requestPull(remote) }
            .setNegativeButton("Trust Current") { _, _ -> confirmCreateBaseline(remote) }
            .setNeutralButton("Later", null)
            .show()
    }

    private fun confirmCreateBaseline(remote: GitHubRepository) {
        AlertDialog.Builder(this)
            .setTitle("Trust current workspace?")
            .setMessage("NovaIDE will mark the current files as synchronized with ${remote.slug}:${remote.branch}. Incorrect use can hide existing differences until the next pull.")
            .setPositiveButton("Create baseline") { _, _ -> createCurrentBaseline(remote) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCurrentBaseline(remote: GitHubRepository) {
        val workspaceUri = store.workspaceUri ?: return
        val rootNode = workspaceRoot ?: return
        withSavedWorkspace {
            if (!beginGitMutation(workspaceUri)) return@withSavedWorkspace
            showProgress(true)
            statusText.text = "Creating Git baseline…"
            projectIo.submit {
                val result = runCatching {
                    val client = GitHubApiClient(gitHubStore.token())
                    val head = client.branchHeadOrNull(remote)
                        ?: throw IOException("This repository is empty. Create an initial commit instead of trusting a baseline.")
                    val scan = repository.scan(rootNode, maxEntries = 8_000, maxDepth = 60)
                    gitSnapshots.capture(workspaceUri, remote, head, scan.entries)
                }
                mainHandler.post {
                    endGitMutation(workspaceUri)
                    showProgress(false)
                    if (store.workspaceUri != workspaceUri) return@post
                    result.onSuccess {
                        statusText.text = "Git baseline ready • ${it.files} files"
                        toast("Baseline created for ${remote.branch}")
                    }.onFailure { showError("Baseline failed", it.message ?: "Could not create the snapshot") }
                }
            }
        }
    }

    private fun withSavedWorkspace(action: () -> Unit) {
        snapshotActiveEditorState()
        val dirty = openTabs.filter { it.isDirty && !it.isReadOnly }
        if (dirty.isEmpty()) action() else saveDirtyTabsThen(dirty, action)
    }

    private fun computeGitStatus(onReady: (GitStatus, List<FileRepository.WorkspaceEntry>) -> Unit) {
        val workspaceUri = store.workspaceUri ?: run { toast("Open a workspace first"); return }
        val rootNode = workspaceRoot ?: return
        withSavedWorkspace {
            showProgress(true)
            statusText.text = "Scanning local changes…"
            projectIo.submit {
                val result = runCatching {
                    val scan = repository.scan(rootNode, maxEntries = 8_000, maxDepth = 60)
                    GitStatusEngine(repository, gitSnapshots).status(workspaceUri, scan.entries) to scan.entries
                }
                mainHandler.post {
                    showProgress(false)
                    if (store.workspaceUri != workspaceUri) return@post
                    result.onSuccess { onReady(it.first, it.second) }
                        .onFailure { showError("Git status failed", it.message ?: "Could not scan the workspace") }
                }
            }
        }
    }

    private fun showGitStatus() {
        val workspaceUri = store.workspaceUri ?: return
        val remote = gitHubStore.repository(workspaceUri) ?: run { showGitHubSetup(null); return }
        computeGitStatus { status, _ ->
            if (status.changes.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Working tree clean")
                    .setMessage("${status.scannedFiles} tracked files match ${remote.branch}.${if (status.skippedFiles > 0) " ${status.skippedFiles} oversized/unreadable files were skipped." else ""}")
                    .setPositiveButton("Done", null)
                    .show()
                return@computeGitStatus
            }
            val labels = status.changes.map { change ->
                val badge = when (change.kind) {
                    GitChangeKind.ADDED -> "A"
                    GitChangeKind.MODIFIED -> "M"
                    GitChangeKind.DELETED -> "D"
                }
                "$badge   ${change.path}${if (change.size > 0) "  •  ${formatBytes(change.size)}" else ""}"
            }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle((if (status.baselineExists) "${status.changes.size} changes" else "Initial commit • ${status.changes.size} files") + " • +${status.added} ~${status.modified} -${status.deleted}")
                .setItems(labels) { _, which -> showGitDiff(workspaceUri, status.changes[which]) }
                .setPositiveButton("Commit & Push") { _, _ -> showCommitMessage(remote, status) }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showGitDiff(workspaceUri: Uri, change: GitChange) {
        showProgress(true)
        projectIo.submit {
            val result = runCatching {
                val baseline = gitSnapshots.load(workspaceUri)
                val oldEntry = baseline?.entries?.get(change.path)
                val oldText = oldEntry?.let { gitSnapshots.readBaselineText(workspaceUri, it) }
                val currentText = if (change.kind == GitChangeKind.DELETED) "" else {
                    val uri = change.uri ?: throw IOException("Current file is unavailable")
                    val metadata = repository.metadata(uri)
                    if (!TextFileClassifier.isProbablyText(metadata.name, metadata.mimeType) || metadata.size > 512L * 1024L) null
                    else repository.readText(uri, 512L * 1024L)
                }
                if (oldText == null && change.kind != GitChangeKind.ADDED) {
                    "Binary or large-file change\n\n${change.path}\nType: ${change.kind.name.lowercase()}\nSize: ${formatBytes(change.size)}\nText diff is available for UTF-8 files up to 512 KB."
                } else if (currentText == null && change.kind != GitChangeKind.DELETED) {
                    "Binary or large-file change\n\n${change.path}\nType: ${change.kind.name.lowercase()}\nSize: ${formatBytes(change.size)}\nText diff is available for UTF-8 files up to 512 KB."
                } else {
                    UnifiedDiff.create(oldText.orEmpty(), currentText.orEmpty(), "baseline/${change.path}", "workspace/${change.path}").text
                }
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { diff ->
                    val text = Ui.text(this, diff, 11.5f, palette.textPrimary, gravity = Gravity.START).apply {
                        typeface = Typeface.MONOSPACE
                        setTextIsSelectable(true)
                        setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 12))
                    }
                    AlertDialog.Builder(this)
                        .setTitle(change.path)
                        .setView(ScrollView(this).apply { addView(text) })
                        .setPositiveButton("Close", null)
                        .show()
                }.onFailure { showError("Diff failed", it.message ?: "Could not create a diff") }
            }
        }
    }

    private fun beginCommitAndPush() {
        val workspaceUri = store.workspaceUri ?: return
        val remote = gitHubStore.repository(workspaceUri) ?: run { showGitHubSetup(null); return }
        if (!gitHubStore.hasToken()) {
            showError("GitHub token required", "Commit & Push requires a fine-grained token with repository Contents read/write permission. Add it in GitHub Settings.")
            return
        }
        computeGitStatus { status, _ ->
            when {
                status.changes.isEmpty() && !status.baselineExists -> showError(
                    "Nothing to commit",
                    "The repository and workspace contain no trackable files. Git does not store empty folders; create README.md or another file first."
                )
                status.changes.isEmpty() -> toast("Working tree is already clean")
                else -> showCommitMessage(remote, status)
            }
        }
    }

    private fun showCommitMessage(remote: GitHubRepository, status: GitStatus) {
        val input = EditText(this).apply {
            hint = "Describe this change"
            setSingleLine(false)
            minLines = 2
            maxLines = 5
        }
        AlertDialog.Builder(this)
            .setTitle(if (status.baselineExists) "Commit ${status.changes.size} changes to ${remote.branch}" else "Create initial commit on ${remote.branch}")
            .setView(input)
            .setPositiveButton("Commit & Push") { _, _ ->
                val message = input.text.toString().trim()
                if (message.isBlank()) {
                    toast("Commit message is required")
                } else performCommit(remote, status, message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCommit(remote: GitHubRepository, status: GitStatus, message: String) {
        val workspaceUri = store.workspaceUri ?: return
        val rootNode = workspaceRoot ?: return
        val baseline = gitSnapshots.load(workspaceUri)
        if (!beginGitMutation(workspaceUri)) return
        showProgress(true)
        statusText.text = "Uploading ${status.changes.size} changes…"
        projectIo.submit {
            val result = runCatching {
                val client = GitHubApiClient(gitHubStore.token())
                val commit = client.createCommit(remote, message, status.changes, repository, baseline?.commitSha)
                val scan = repository.scan(rootNode, maxEntries = 8_000, maxDepth = 60)
                gitSnapshots.capture(workspaceUri, remote, commit.sha, scan.entries)
                commit
            }
            mainHandler.post {
                endGitMutation(workspaceUri)
                showProgress(false)
                if (store.workspaceUri != workspaceUri) return@post
                result.onSuccess { commit ->
                    statusText.text = "Pushed ${commit.sha.take(8)} to ${remote.branch}"
                    AlertDialog.Builder(this)
                        .setTitle("Push complete")
                        .setMessage("Commit ${commit.sha.take(12)} was created on ${remote.slug}:${remote.branch}.")
                        .setPositiveButton("Done", null)
                        .setNeutralButton("View on GitHub") { _, _ -> openExternalUrl(commit.webUrl) }
                        .show()
                }.onFailure { showError("Commit & Push failed", it.message ?: "GitHub rejected the commit") }
            }
        }
    }

    private fun requestPull(remote: GitHubRepository) {
        workspaceRoot ?: run { toast("Open a workspace first"); return }
        computeGitStatus { status, entries ->
            val trackedFiles = entries.count { !it.node.isDirectory && gitSnapshots.shouldTrack(it.relativePath) }
            when {
                status.baselineExists && status.changes.isNotEmpty() -> {
                    AlertDialog.Builder(this)
                        .setTitle("Local changes detected")
                        .setMessage("Pull will overwrite matching tracked files. Untracked files are preserved. Commit first whenever these changes matter.")
                        .setPositiveButton("Discard tracked changes & Pull") { _, _ -> performPull(remote) }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                !status.baselineExists && trackedFiles > 0 -> {
                    AlertDialog.Builder(this)
                        .setTitle("Pull into non-empty workspace?")
                        .setMessage("Remote files may overwrite same-path files. Existing unrelated files are preserved.")
                        .setPositiveButton("Pull / Clone") { _, _ -> performPull(remote) }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                else -> performPull(remote)
            }
        }
    }

    private fun performPull(remote: GitHubRepository) {
        val workspaceUri = store.workspaceUri ?: return
        val rootNode = workspaceRoot ?: return
        if (!beginGitMutation(workspaceUri)) return
        closeAllTabsWithoutPrompt()
        persistSession()
        showEditorOrWelcome()
        showProgress(true)
        statusText.text = "Downloading ${remote.slug}:${remote.branch}…"
        projectIo.submit {
            val archive = File(cacheDir, "nova-github-${System.nanoTime()}.zip")
            val result = runCatching {
                val client = GitHubApiClient(gitHubStore.token())
                val head = client.downloadArchive(remote, archive)
                val previousBaseline = gitSnapshots.load(workspaceUri)
                val applied = GitHubArchiveApplier(repository, gitSnapshots).apply(archive, rootNode, previousBaseline)
                val scan = repository.scan(rootNode, maxEntries = 8_000, maxDepth = 60)
                val remoteEntries = scan.entries.filter { it.node.isDirectory || it.relativePath in applied.remotePaths }
                gitSnapshots.capture(workspaceUri, remote, head, remoteEntries)
                gitHubStore.saveRepository(workspaceUri, remote)
                applied to head
            }
            archive.delete()
            mainHandler.post {
                endGitMutation(workspaceUri)
                showProgress(false)
                if (store.workspaceUri != workspaceUri) return@post
                result.onSuccess { (applied, head) ->
                    statusText.text = "Pulled ${head.take(8)} • ${applied.filesWritten} files"
                    toast("Pulled ${applied.filesWritten} files from ${remote.branch}")
                    reloadWorkspaceTree()
                }.onFailure { showError("Pull failed", it.message ?: "Could not apply the GitHub archive") }
            }
        }
    }

    private fun showGitBranches() {
        val workspaceUri = store.workspaceUri ?: return
        val remote = gitHubStore.repository(workspaceUri) ?: run { showGitHubSetup(null); return }
        showProgress(true)
        statusText.text = "Loading branches…"
        projectIo.submit {
            val result = runCatching { GitHubApiClient(gitHubStore.token()).branches(remote) }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { branches ->
                    if (branches.isEmpty()) { toast("No branches found"); return@onSuccess }
                    val labels = branches.map { "${if (it.name == remote.branch) "✓" else " "}  ${it.name}${if (it.protected) "  • protected" else ""}\n${it.sha.take(12)}" }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Branches • ${remote.slug}")
                        .setItems(labels) { _, which ->
                            val selected = branches[which]
                            if (selected.name == remote.branch) toast("${selected.name} is already active")
                            else requestBranchSwitch(remote.copy(branch = selected.name))
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }.onFailure { showError("Branches failed", it.message ?: "Could not load branches") }
            }
        }
    }

    private fun requestBranchSwitch(target: GitHubRepository) {
        computeGitStatus { status, entries ->
            val hasWorkspaceFiles = entries.any { !it.node.isDirectory && gitSnapshots.shouldTrack(it.relativePath) }
            if (status.baselineExists && status.changes.isNotEmpty()) {
                showError("Branch switch blocked", "Commit or discard local changes before switching branches.")
            } else if (!status.baselineExists && hasWorkspaceFiles) {
                AlertDialog.Builder(this)
                    .setTitle("Switch to ${target.branch}?")
                    .setMessage("No baseline exists. Same-path workspace files may be overwritten by this branch.")
                    .setPositiveButton("Switch & Pull") { _, _ -> performPull(target) }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else performPull(target)
        }
    }

    private fun showGitHistory() {
        val workspaceUri = store.workspaceUri ?: return
        val remote = gitHubStore.repository(workspaceUri) ?: run { showGitHubSetup(null); return }
        showProgress(true)
        statusText.text = "Loading commit history…"
        projectIo.submit {
            val result = runCatching { GitHubApiClient(gitHubStore.token()).commits(remote) }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { commits ->
                    if (commits.isEmpty()) { toast("No commits found"); return@onSuccess }
                    val labels = commits.map { "${it.sha.take(8)}  ${it.message}\n${it.author} • ${it.date}" }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("History • ${remote.branch}")
                        .setItems(labels) { _, which ->
                            val commit = commits[which]
                            AlertDialog.Builder(this)
                                .setTitle(commit.sha.take(12))
                                .setMessage("${commit.message}\n\n${commit.author}\n${commit.date}")
                                .setPositiveButton("View on GitHub") { _, _ -> openExternalUrl(commit.webUrl) }
                                .setNegativeButton("Close", null)
                                .show()
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }.onFailure { showError("History failed", it.message ?: "Could not load commits") }
            }
        }
    }

    private fun showGitHubActions() {
        val workspaceUri = store.workspaceUri ?: return
        val remote = gitHubStore.repository(workspaceUri) ?: run { showGitHubSetup(null); return }
        showProgress(true)
        statusText.text = "Loading GitHub Actions…"
        projectIo.submit {
            val result = runCatching { GitHubApiClient(gitHubStore.token()).workflowRuns(remote) }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { runs ->
                    if (runs.isEmpty()) { toast("No workflow runs found for ${remote.branch}"); return@onSuccess }
                    val labels = runs.map {
                        val state = it.conclusion ?: it.status
                        "${actionBadge(state)}  ${it.name}\n$state • ${it.event} • ${it.createdAt}"
                    }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("GitHub Actions • ${remote.branch}")
                        .setItems(labels) { _, which -> showWorkflowRun(remote, runs[which]) }
                        .setNegativeButton("Close", null)
                        .show()
                }.onFailure { showError("Actions failed", it.message ?: "Could not load workflow runs") }
            }
        }
    }

    private fun showWorkflowRun(remote: GitHubRepository, run: com.mohnishraj.novaide.git.WorkflowRun) {
        showProgress(true)
        statusText.text = "Loading workflow artifacts…"
        projectIo.submit {
            val result = runCatching { GitHubApiClient(gitHubStore.token()).artifacts(remote, run.id) }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { artifacts ->
                    if (artifacts.isEmpty()) {
                        AlertDialog.Builder(this)
                            .setTitle(run.name)
                            .setMessage("Status: ${run.conclusion ?: run.status}\nBranch: ${run.branch}\nEvent: ${run.event}\nCreated: ${run.createdAt}\n\nNo downloadable artifacts were found.")
                            .setPositiveButton("View run") { _, _ -> openExternalUrl(run.webUrl) }
                            .setNegativeButton("Close", null)
                            .show()
                    } else {
                        val labels = artifacts.map { "${if (it.expired) "Expired" else "Download"}  ${it.name}\n${formatBytes(it.sizeBytes)}" }.toTypedArray()
                        AlertDialog.Builder(this)
                            .setTitle("${run.name} • ${run.conclusion ?: run.status}")
                            .setItems(labels) { _, which -> chooseArtifactDestination(artifacts[which]) }
                            .setPositiveButton("View run") { _, _ -> openExternalUrl(run.webUrl) }
                            .setNegativeButton("Close", null)
                            .show()
                    }
                }.onFailure { showError("Artifacts failed", it.message ?: "Could not load workflow artifacts") }
            }
        }
    }

    private fun chooseArtifactDestination(artifact: WorkflowArtifact) {
        if (artifact.expired) { toast("This artifact has expired"); return }
        if (!gitHubStore.hasToken()) {
            showError("GitHub token required", "GitHub requires authentication to download workflow artifacts.")
            return
        }
        pendingArtifact = artifact
        startActivityForResult(Intent("android.intent.action.CREATE_DOCUMENT").apply {
            addOpenableCategoryCompat()
            setMimeTypeCompat("application/zip")
            putExtra("android.intent.extra.TITLE", "${artifact.name.replace(Regex("[^A-Za-z0-9._-]"), "-")}.zip")
        }, CREATE_GITHUB_ARTIFACT)
    }

    private fun downloadPendingArtifact(destination: Uri) {
        val artifact = pendingArtifact ?: return
        pendingArtifact = null
        showProgress(true)
        statusText.text = "Downloading ${artifact.name}…"
        projectIo.submit {
            val result = runCatching {
                contentResolver.openOutputStream(destination, "w")?.use { output ->
                    GitHubApiClient(gitHubStore.token()).downloadArtifact(artifact, output)
                } ?: throw IOException("Could not create the destination file")
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { toast("Downloaded ${artifact.name}.zip") }
                    .onFailure { showError("Artifact download failed", it.message ?: "Could not download the artifact") }
            }
        }
    }

    private fun showMergeConflicts() {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        withSavedWorkspace {
            showProgress(true)
            statusText.text = "Scanning conflict markers…"
            projectIo.submit {
                val result = runCatching {
                    val scan = repository.scan(rootNode, maxEntries = 8_000, maxDepth = 60)
                    scan.entries.asSequence()
                        .filter { !it.node.isDirectory && it.node.size <= 2L * 1024L * 1024L }
                        .filter { TextFileClassifier.isProbablyText(it.node.name, it.node.mimeType) }
                        .mapNotNull { entry ->
                            val source = runCatching { repository.readText(entry.node.uri) }.getOrNull() ?: return@mapNotNull null
                            val count = ConflictParser.find(source).size
                            if (count == 0) null else Triple(entry, source, count)
                        }
                        .take(200)
                        .toList()
                }
                mainHandler.post {
                    showProgress(false)
                    result.onSuccess { conflicts ->
                        if (conflicts.isEmpty()) { toast("No merge conflict markers found"); return@onSuccess }
                        val labels = conflicts.map { "${it.first.relativePath}\n${it.third} conflict block${if (it.third == 1) "" else "s"}" }.toTypedArray()
                        AlertDialog.Builder(this)
                            .setTitle("Merge conflicts • ${conflicts.sumOf { it.third }} blocks")
                            .setItems(labels) { _, which -> showConflictResolver(conflicts[which].first, conflicts[which].second) }
                            .setNegativeButton("Close", null)
                            .show()
                    }.onFailure { showError("Conflict scan failed", it.message ?: "Could not scan text files") }
                }
            }
        }
    }

    private fun showConflictResolver(entry: FileRepository.WorkspaceEntry, source: String) {
        val blocks = ConflictParser.find(source)
        val block = blocks.firstOrNull() ?: run {
            toast("All conflicts in ${entry.node.name} are resolved")
            return
        }
        val preview = buildString {
            append("CURRENT — ").append(block.oursLabel).append("\n")
            append(block.ours.take(3_000)).append("\n")
            append("────────────────\n")
            append("INCOMING — ").append(block.theirsLabel).append("\n")
            append(block.theirs.take(3_000))
            if (blocks.size > 1) append("\n\n${blocks.size - 1} more conflict block(s) remain in this file.")
        }
        val text = Ui.text(this, preview, 11.5f, palette.textPrimary, gravity = Gravity.START).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 12))
        }
        AlertDialog.Builder(this)
            .setTitle(entry.relativePath)
            .setView(ScrollView(this).apply { addView(text) })
            .setPositiveButton("Use Current") { _, _ -> resolveConflict(entry, source, ConflictResolution.OURS) }
            .setNegativeButton("Use Incoming") { _, _ -> resolveConflict(entry, source, ConflictResolution.THEIRS) }
            .setNeutralButton("Keep Both") { _, _ -> resolveConflict(entry, source, ConflictResolution.BOTH) }
            .show()
    }

    private fun resolveConflict(entry: FileRepository.WorkspaceEntry, source: String, resolution: ConflictResolution) {
        val workspaceUri = store.workspaceUri ?: return
        if (!beginGitMutation(workspaceUri)) return
        val resolved = ConflictParser.resolve(source, 0, resolution)
        showProgress(true)
        projectIo.submit {
            val result = runCatching { repository.writeText(entry.node.uri, resolved) }
            mainHandler.post {
                endGitMutation(workspaceUri)
                showProgress(false)
                if (store.workspaceUri != workspaceUri) return@post
                result.onSuccess {
                    openTabs.firstOrNull { it.uri == entry.node.uri }?.let { tab ->
                        tab.content = resolved
                        tab.savedContentHash = resolved.hashCode()
                        if (activeTab === tab) editor.setDocument(tab.name, resolved)
                    }
                    renderTabs()
                    if (ConflictParser.find(resolved).isEmpty()) toast("Resolved all conflicts in ${entry.node.name}")
                    else showConflictResolver(entry, resolved)
                }.onFailure { showError("Conflict resolution failed", it.message ?: "Could not write the resolved file") }
            }
        }
    }

    private fun checkRemoteInitialization(remote: GitHubRepository) {
        showProgress(true)
        statusText.text = "Checking repository state…"
        projectIo.submit {
            val result = runCatching {
                val client = GitHubApiClient(gitHubStore.token())
                val info = client.repositoryInfo(remote)
                val empty = client.branchHeadOrNull(remote) == null
                info to empty
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { (_, empty) -> promptInitialGitSync(remote, empty) }
                    .onFailure { showError("GitHub check failed", it.message ?: "Could not inspect repository state") }
            }
        }
    }

    private fun showCredentialsCenter() {
        val configured = credentialVault.configured()
        val active = credentialSettings.activeAiProvider
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 12))
        }
        box.addView(Ui.text(this, "Credentials stay under your control", 18f, palette.textPrimary, bold = true))
        box.addView(Ui.text(this,
            "Secrets are encrypted with Android Keystore and stored only on this device. NovaIDE never displays a saved secret again and sends it only over HTTPS to the selected provider when you request an authenticated action.",
            11.5f, palette.textSecondary
        ).apply { setPadding(0, Ui.dp(this@MainActivity, 6), 0, Ui.dp(this@MainActivity, 14)) })

        fun card(title: String, subtitle: String, configuredNow: Boolean, activeNow: Boolean = false, action: () -> Unit) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 10), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 10))
                setBackgroundColor(palette.surface)
            }
            row.addView(Ui.text(this, title + if (activeNow) "  • ACTIVE" else "", 15f, palette.textPrimary, bold = true))
            row.addView(Ui.text(this, (if (configuredNow) "Configured • " else "Not configured • ") + subtitle, 11f,
                if (configuredNow) palette.accent else palette.textSecondary))
            row.setOnClickListener { action() }
            box.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = Ui.dp(this@MainActivity, 8)
            })
        }
        card("GitHub", "repositories, commits, branches and Actions", configured.contains(CredentialId.GITHUB)) { showGitHubCredentialEditor() }
        card("GitLab", "GitLab.com or self-managed API access", configured.contains(CredentialId.GITLAB)) { showGitLabCredentialEditor() }
        CredentialCatalog.aiProviders.forEach { provider ->
            val cfg = credentialSettings.aiConfig(provider)
            card(provider.label, "${cfg.model.ifBlank { "model not selected" }} • ${cfg.baseUrl}", cfg.hasKey || !provider.keyRequired, provider == active) {
                showAiCredentialEditor(provider)
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Credentials Center")
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showGitHubCredentialEditor() {
        val descriptor = CredentialCatalog.github
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 22), 0, Ui.dp(this@MainActivity, 22), 0)
        }
        val token = EditText(this).apply {
            hint = if (credentialVault.has(CredentialId.GITHUB)) "New token (blank keeps saved token)" else "Fine-grained GitHub token"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val remove = CheckBox(this).apply {
            text = "Remove saved GitHub token"
            visibility = if (credentialVault.has(CredentialId.GITHUB)) View.VISIBLE else View.GONE
        }
        box.addView(token)
        box.addView(remove)
        box.addView(Button(this).apply {
            text = "Create token on GitHub"
            setOnClickListener { openExternalUrl(descriptor.createUrl.orEmpty()) }
        })
        box.addView(Ui.text(this, descriptor.permissionTip + "\n\n" + descriptor.privacyNote, 11f, palette.textSecondary).apply {
            setPadding(0, Ui.dp(this@MainActivity, 8), 0, 0)
        })
        AlertDialog.Builder(this)
            .setTitle("Manage GitHub credential")
            .setView(box)
            .setPositiveButton("Save & Verify") { _, _ ->
                if (remove.isChecked) {
                    credentialVault.delete(CredentialId.GITHUB)
                    toast("GitHub token removed")
                    return@setPositiveButton
                }
                val raw = token.text.toString()
                val candidate = try {
                    if (raw.isBlank()) credentialVault.read(CredentialId.GITHUB)
                    else gitHubStore.normalizedToken(raw)
                } catch (error: Exception) {
                    showError("Invalid GitHub token", error.message ?: "Token format is invalid")
                    return@setPositiveButton
                }
                if (candidate.isNullOrBlank()) {
                    showError("GitHub token required", "Paste a fine-grained token or use public read-only mode from GitHub repository settings.")
                    return@setPositiveButton
                }
                showProgress(true)
                aiIo.submit {
                    val result = runCatching { GitHubApiClient(candidate).authenticatedUser() ?: throw IOException("GitHub did not return an authenticated user") }
                    mainHandler.post {
                        showProgress(false)
                        result.onSuccess { user ->
                            credentialVault.save(CredentialId.GITHUB, candidate)
                            toast("GitHub verified as @${user.login}")
                        }.onFailure { showError("GitHub verification failed", it.message ?: "Check the token and permissions") }
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showGitLabCredentialEditor() {
        val descriptor = CredentialCatalog.gitlab
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 22), 0, Ui.dp(this@MainActivity, 22), 0)
        }
        val base = EditText(this).apply {
            hint = "https://gitlab.com"
            setSingleLine(true)
            setText(credentialSettings.gitLabBaseUrl)
        }
        val token = EditText(this).apply {
            hint = if (credentialVault.has(CredentialId.GITLAB)) "New token (blank keeps saved token)" else "GitLab personal access token"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val remove = CheckBox(this).apply {
            text = "Remove saved GitLab token"
            visibility = if (credentialVault.has(CredentialId.GITLAB)) View.VISIBLE else View.GONE
        }
        box.addView(base); box.addView(token); box.addView(remove)
        box.addView(Button(this).apply {
            text = "Create token on GitLab"
            setOnClickListener { openExternalUrl(descriptor.createUrl.orEmpty()) }
        })
        box.addView(Ui.text(this, descriptor.permissionTip + "\n\n" + descriptor.privacyNote, 11f, palette.textSecondary).apply {
            setPadding(0, Ui.dp(this@MainActivity, 8), 0, 0)
        })
        AlertDialog.Builder(this)
            .setTitle("Manage GitLab credential")
            .setView(box)
            .setPositiveButton("Save & Verify") { _, _ ->
                if (remove.isChecked) {
                    credentialVault.delete(CredentialId.GITLAB)
                    toast("GitLab token removed")
                    return@setPositiveButton
                }
                val server = base.text.toString().trim()
                val raw = token.text.toString()
                val candidate = try {
                    if (raw.isBlank()) credentialVault.read(CredentialId.GITLAB) else GitLabTokenNormalizer.normalize(raw)
                } catch (error: Exception) {
                    showError("Invalid GitLab token", error.message ?: "Token format is invalid")
                    return@setPositiveButton
                }
                if (candidate.isNullOrBlank()) {
                    showError("GitLab token required", "Create a token with read_api or api scope and paste it here.")
                    return@setPositiveButton
                }
                val normalizedServer = try {
                    credentialSettings.normalizeGitLabBaseUrl(server)
                } catch (error: Exception) {
                    showError("Invalid GitLab URL", error.message ?: "Use a clean HTTPS server URL")
                    return@setPositiveButton
                }
                showProgress(true)
                aiIo.submit {
                    val result = runCatching {
                        val client = GitLabApiClient(normalizedServer, candidate)
                        val identity = client.verify()
                        client.requireRead(identity)
                        identity
                    }
                    mainHandler.post {
                        showProgress(false)
                        result.onSuccess { identity ->
                            credentialVault.save(CredentialId.GITLAB, candidate)
                            credentialSettings.gitLabBaseUrl = normalizedServer
                            val warning = if (identity.canWriteApi) "Read/write API access available." else "Read-only API access. Write requests will show a clear api-scope error."
                            showSelectableTextDialog("GitLab connected", "@${identity.username} • ${identity.name}\nScopes: ${identity.scopes.ifEmpty { setOf("not reported") }.joinToString()}\n$warning")
                        }.onFailure { showError("GitLab verification failed", it.message ?: "Check server, token and scopes") }
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAiCredentialEditor(provider: AiProviderId) {
        val descriptor = CredentialCatalog.ai(provider)
        val current = credentialSettings.aiConfig(provider)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 22), 0, Ui.dp(this@MainActivity, 22), 0)
        }
        val base = EditText(this).apply { hint = "HTTPS API base URL"; setSingleLine(true); setText(current.baseUrl) }
        val model = EditText(this).apply { hint = "Model ID"; setSingleLine(true); setText(current.model) }
        val key = EditText(this).apply {
            hint = if (current.hasKey) "New API key (blank keeps saved key)" else if (provider.keyRequired) "API key" else "API key (optional)"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val active = CheckBox(this).apply { text = "Use as active AI provider"; isChecked = credentialSettings.activeAiProvider == provider }
        val remove = CheckBox(this).apply { text = "Remove saved API key"; visibility = if (current.hasKey) View.VISIBLE else View.GONE }
        box.addView(base); box.addView(model); box.addView(key); box.addView(active); box.addView(remove)
        descriptor.createUrl?.let { url -> box.addView(Button(this).apply { text = "Create API key"; setOnClickListener { openExternalUrl(url) } }) }
        box.addView(Button(this).apply {
            text = "Fetch available models"
            setOnClickListener {
                val candidate = key.text.toString().trim().ifBlank { credentialVault.read(provider.credentialId).orEmpty() }
                val cfg = try { credentialSettings.validatedAiConfig(provider, base.text.toString(), model.text.toString()) }
                catch (error: Exception) { showError("Invalid provider settings", error.message ?: "Check the HTTPS endpoint"); return@setOnClickListener }
                showProgress(true)
                aiIo.submit {
                    val result = runCatching { AiApiClient(AiRuntime(cfg, candidate.ifBlank { null })).testConnection() }
                    mainHandler.post {
                        showProgress(false)
                        result.onSuccess { models ->
                            if (models.isEmpty()) toast("Connected; enter a model ID manually")
                            else AlertDialog.Builder(this@MainActivity)
                                .setTitle("Choose ${provider.label} model")
                                .setItems(models.take(200).map { "${it.label}\n${it.id}" }.toTypedArray()) { _, which -> model.setText(models[which].id) }
                                .setNegativeButton("Close", null).show()
                        }.onFailure { showError("Model discovery failed", it.message ?: "Check key, endpoint and permissions") }
                    }
                }
            }
        })
        box.addView(Ui.text(this, descriptor.permissionTip + "\n\n" + descriptor.privacyNote, 11f, palette.textSecondary).apply { setPadding(0, 8, 0, 0) })
        AlertDialog.Builder(this)
            .setTitle("Manage ${provider.label}")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Save & Test") { _, _ ->
                if (remove.isChecked) {
                    credentialVault.delete(provider.credentialId)
                    if (credentialSettings.activeAiProvider == provider && provider.keyRequired) {
                        toast("${provider.label} key removed. Choose or configure another provider before using AI.")
                    } else {
                        toast("${provider.label} API key removed from this device")
                    }
                    return@setPositiveButton
                }
                val rawKey = key.text.toString().trim()
                val candidate = rawKey.ifBlank { credentialVault.read(provider.credentialId).orEmpty() }.ifBlank { null }
                val cfg = try {
                    credentialSettings.validatedAiConfig(provider, base.text.toString(), model.text.toString())
                } catch (error: Exception) {
                    showError("Invalid provider settings", error.message ?: "Check the HTTPS endpoint")
                    return@setPositiveButton
                }
                if (provider.keyRequired && candidate.isNullOrBlank()) {
                    showError("API key required", "${provider.label} requires a key. Use the Create API key link and paste it here.")
                    return@setPositiveButton
                }
                showProgress(true)
                aiIo.submit {
                    val result = runCatching { AiApiClient(AiRuntime(cfg.copy(hasKey = !candidate.isNullOrBlank()), candidate)).testConnection() }
                    mainHandler.post {
                        showProgress(false)
                        result.onSuccess { models ->
                            if (!rawKey.isBlank()) credentialVault.save(provider.credentialId, rawKey)
                            credentialSettings.saveAiConfig(provider, cfg.baseUrl, cfg.model)
                            if (active.isChecked) credentialSettings.activeAiProvider = provider
                            toast("${provider.label} connected • ${models.size} models found")
                        }.onFailure { showError("AI connection failed", it.message ?: "Check API key, model access and endpoint") }
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAiCenter() {
        val provider = credentialSettings.activeAiProvider
        val config = credentialSettings.aiConfig(provider)
        val labels = AiTask.entries.map { it.label }.toMutableList().apply { add("Credentials & models") }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("AI Assistant • ${provider.label}\n${config.model.ifBlank { "No model selected" }}")
            .setItems(labels) { _, which ->
                if (which == AiTask.entries.size) showCredentialsCenter() else promptAiTask(AiTask.entries[which])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun promptAiTask(task: AiTask) {
        val input = EditText(this).apply {
            hint = when (task) {
                AiTask.FIX -> "Describe the bug or paste the error"
                AiTask.GENERATE -> "Describe what to build"
                AiTask.ERROR_TRACE -> "Paste the stack trace or failing behavior"
                else -> "What should NovaIDE analyze or do?"
            }
            minLines = 3; maxLines = 8; gravity = Gravity.TOP
        }
        AlertDialog.Builder(this)
            .setTitle(task.label)
            .setView(input)
            .setPositiveButton("Run") { _, _ -> runAiTask(task, input.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runAiTask(task: AiTask, prompt: String) {
        if (prompt.isBlank()) { toast("Enter an AI request"); return }
        val provider = credentialSettings.activeAiProvider
        val config = credentialSettings.aiConfig(provider)
        val key = credentialVault.read(provider.credentialId)
        if (provider.keyRequired && key.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("${provider.label} key required")
                .setMessage("This request needs a configured API key. NovaIDE cannot perform a provider action without the required permission or credential.")
                .setPositiveButton("Open Credentials") { _, _ -> showAiCredentialEditor(provider) }
                .setNegativeButton("Cancel", null).show()
            return
        }
        val tab = activeTab
        val content = editor.text?.toString().orEmpty()
        val start = minOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
        val end = maxOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(start)
        val selection = content.substring(start.coerceAtMost(content.length), end.coerceAtMost(content.length)).takeIf { it.isNotBlank() }
        val path = tab?.let { current -> workspaceIndex.firstOrNull { it.node.uri == current.uri }?.relativePath ?: current.name }
        val target = AiApplyTarget(tab?.uri, path, start, end, content.hashCode())
        val entries = workspaceIndex.toList()
        showProgress(true)
        statusText.text = "Building safe AI context…"
        aiIo.submit {
            val result = runCatching {
                val context = AiContextBuilder.build(prompt, path, content.takeIf { tab != null }, selection, entries, repository)
                val request = AiRequest(task, prompt, context, path, selection != null)
                AiApiClient(AiRuntime(config, key)).complete(request) to context
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { (response, context) -> showAiResponse(response, target, context.includedFiles.size, context.redactions) }
                    .onFailure { showError("AI request failed", it.message ?: "The provider rejected the request") }
            }
        }
    }

    private fun showAiResponse(response: AiResponse, target: AiApplyTarget, contextFiles: Int, redactions: Int) {
        val patches = runCatching { AiResponseParser.filePatches(response.text) }.getOrDefault(emptyList())
        val code = AiResponseParser.extractCode(response.text)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Ui.text(this, "${response.provider} • ${response.model} • $contextFiles context files • $redactions secret redactions", 10.5f, palette.textSecondary).apply {
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 4))
        })
        box.addView(ScrollView(this).apply {
            addView(Ui.text(this@MainActivity, response.text, 11.5f, palette.textPrimary, gravity = Gravity.START).apply {
                typeface = Typeface.MONOSPACE; setTextIsSelectable(true); setPadding(16, 8, 16, 16)
            })
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 390)))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }
        row.addView(button("Copy") { copyText("AI response", response.text) }, LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1f))
        if (code != null) row.addView(button("Apply code") { applyAiCode(target, code) }, LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1f))
        if (patches.isNotEmpty()) row.addView(button("Apply ${patches.size} files") { confirmAiFilePatches(patches) }, LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1f))
        box.addView(row)
        AlertDialog.Builder(this).setTitle("AI result").setView(box).setNegativeButton("Close", null).show()
    }

    private fun applyAiCode(target: AiApplyTarget, code: String) {
        val tab = activeTab
        val current = editor.text?.toString().orEmpty()
        if (tab == null || tab.uri != target.uri || current.hashCode() != target.contentHash) {
            showError("File changed", "The active file changed after this AI request. Review the response and run it again before applying.")
            return
        }
        if (target.selectionEnd > target.selectionStart) {
            editor.replaceRange(target.selectionStart, target.selectionEnd, code)
            toast("AI code applied to selection")
        } else {
            AlertDialog.Builder(this)
                .setTitle("Replace entire active file?")
                .setMessage("No code selection was captured when the AI request started. Replacing the whole file is reversible only through your own save/Git history.")
                .setPositiveButton("Replace file") { _, _ -> editor.replaceAllUserText(code) }
                .setNegativeButton("Cancel", null).show()
        }
    }

    private fun confirmAiFilePatches(patches: List<com.mohnishraj.novaide.ai.NovaFilePatch>) {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        val paths = patches.joinToString("\n") { "• ${it.path}" }
        AlertDialog.Builder(this)
            .setTitle("Apply ${patches.size} AI file changes?")
            .setMessage("NovaIDE validated all relative paths. Unsaved open files are protected.\n\n$paths")
            .setPositiveButton("Apply") { _, _ ->
                val dirtyPaths = openTabs.filter { it.isDirty }.mapNotNull { tab -> workspaceIndex.firstOrNull { it.node.uri == tab.uri }?.relativePath }.toSet()
                showProgress(true)
                projectIo.submit {
                    val result = runCatching { AiWorkspacePatcher(repository).apply(rootNode, patches, dirtyPaths) }
                    mainHandler.post {
                        showProgress(false)
                        result.onSuccess { applied ->
                            toast("AI patch applied • ${applied.created} created, ${applied.updated} updated")
                            reloadWorkspaceTree()
                        }.onFailure { showError("AI patch blocked", it.message ?: "Could not apply the patch safely") }
                    }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showLocalIntelligenceCenter() {
        val options = arrayOf("Autocomplete", "Insert snippet", "Local lint", "Regex quick fixes", "Static project analysis")
        AlertDialog.Builder(this).setTitle("Local Intelligence • offline")
            .setItems(options) { _, which -> when (which) {
                0 -> showAutocomplete(); 1 -> showSnippets(); 2 -> showLocalLint(); 3 -> showRegexFixes(); else -> showStaticAnalysis()
            }}.setNegativeButton("Close", null).show()
    }

    private fun showAutocomplete() {
        val tab = activeTab ?: run { toast("Open a text file first"); return }
        val source = editor.text?.toString().orEmpty()
        val suggestions = AutocompleteEngine.suggest(tab.name, source, editor.selectionStart)
        if (suggestions.isEmpty()) { toast("No completion suggestions for this prefix"); return }
        AlertDialog.Builder(this)
            .setTitle("Autocomplete • ${suggestions.size} suggestions")
            .setItems(suggestions.map { "${it.label}\n${it.detail}" }.toTypedArray()) { _, which ->
                val item = suggestions[which]
                editor.replaceRange(item.replaceStart, item.replaceEnd, item.insertText)
            }
            .setNegativeButton("Close", null).show()
    }

    private fun showSnippets() {
        val tab = activeTab ?: run { toast("Open a text file first"); return }
        val snippets = SnippetCatalog.forFile(tab.name)
        if (snippets.isEmpty()) { toast("No snippets available for ${tab.name}"); return }
        AlertDialog.Builder(this).setTitle("Insert snippet")
            .setItems(snippets.map { "${it.title}\nTrigger: ${it.trigger}" }.toTypedArray()) { _, which ->
                val source = editor.text?.toString().orEmpty()
                val cursor = editor.selectionStart.coerceIn(0, source.length)
                val lineStart = source.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
                val indent = source.substring(lineStart, cursor).takeWhile { it == ' ' || it == '\t' }
                val expanded = SnippetCatalog.expand(snippets[which], indent)
                val start = minOf(editor.selectionStart, editor.selectionEnd)
                val end = maxOf(editor.selectionStart, editor.selectionEnd)
                editor.replaceRange(start, end, expanded.text)
                editor.setSelection((start + expanded.cursorOffset).coerceIn(0, editor.length()))
            }.setNegativeButton("Close", null).show()
    }

    private fun showLocalLint() {
        val tab = activeTab ?: run { toast("Open a text file first"); return }
        val source = editor.text?.toString().orEmpty()
        showProgress(true)
        editorAnalysis.submit {
            val issues = LocalLintEngine.analyze(tab.name, source)
            mainHandler.post {
                showProgress(false)
                if (activeTab !== tab || editor.text?.toString() != source) { toast("File changed; run lint again"); return@post }
                if (issues.isEmpty()) { toast("Local lint found no issues"); return@post }
                AlertDialog.Builder(this)
                    .setTitle("Local lint • ${issues.size} issues")
                    .setItems(issues.map { "${when(it.severity){LintSeverity.ERROR->"✕";LintSeverity.WARNING->"⚠";LintSeverity.INFO->"•"}} line ${it.line}:${it.column}\n${it.message}" }.toTypedArray()) { _, which ->
                        goToLine(issues[which].line)
                    }.setPositiveButton("Quick fixes") { _, _ -> showRegexFixes() }
                    .setNegativeButton("Close", null).show()
            }
        }
    }

    private fun showRegexFixes() {
        val tab = activeTab ?: run { toast("Open a text file first"); return }
        val source = editor.text?.toString().orEmpty()
        val proposals = RegexFixEngine.proposals(tab.name, source)
        if (proposals.isEmpty()) { toast("No safe regex fixes are available"); return }
        AlertDialog.Builder(this).setTitle("Regex quick fixes")
            .setItems(proposals.map { "${it.title}\n${it.description} • ~${it.replacements} lines" }.toTypedArray()) { _, which ->
                val proposal = proposals[which]
                val diff = UnifiedDiff.create(source, proposal.output, tab.name, "fixed/${tab.name}").text
                showSelectableTextDialog(proposal.title, diff, "Apply") {
                    if (activeTab === tab && editor.text?.toString() == source) editor.replaceAllUserText(proposal.output)
                    else showError("File changed", "Run the quick fix again before applying.")
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun showStaticAnalysis() {
        if (workspaceIndex.isEmpty()) { toast("Project index is still building"); return }
        val snapshot = workspaceIndex.filter { !it.node.isDirectory }
        showProgress(true)
        statusText.text = "Running local static analysis…"
        projectIo.submit {
            val files = snapshot.take(8_000).map { entry ->
                val text = if (entry.node.size <= 512L * 1024L && !com.mohnishraj.novaide.ai.SecretRedactor.isSensitivePath(entry.relativePath) && TextFileClassifier.isProbablyText(entry.node.name, entry.node.mimeType)) {
                    runCatching { repository.readText(entry.node.uri, 512L * 1024L) }.getOrNull()
                } else null
                StaticFile(entry.relativePath, entry.node.size, text)
            }
            val report = StaticAnalysisEngine.analyze(files)
            mainHandler.post {
                showProgress(false)
                if (report.findings.isEmpty()) { toast("Static analysis found no notable issues"); return@post }
                AlertDialog.Builder(this)
                    .setTitle("Static analysis • ${report.findings.size} findings")
                    .setItems(report.findings.map { "${it.severity} • ${it.title}\n${it.path ?: it.details}" }.toTypedArray()) { _, which ->
                        val finding = report.findings[which]
                        val findingPath = finding.path
                        if (findingPath != null && findWorkspaceEntry(findingPath) != null) {
                            AlertDialog.Builder(this).setTitle(finding.title).setMessage(finding.details)
                                .setPositiveButton("Open file") { _, _ -> openWorkspacePath(findingPath) }
                                .setNegativeButton("Close", null).show()
                        } else showError(finding.title, finding.details)
                    }.setNegativeButton("Close", null).show()
            }
        }
    }


    private fun showDiagnosticsCenter() {
        if (workspaceRoot == null) { toast("Open a workspace first"); return }
        val options = arrayOf(
            "Full project health audit",
            "Analyze pasted crash / ANR trace",
            "Import crash, log or ZIP",
            "Duplicate code and files",
            "Dead code and unused symbols",
            "Dependency map and cycles",
            "Performance report",
            "Security report"
        )
        AlertDialog.Builder(this)
            .setTitle("Debug & Analysis • local")
            .setMessage("No project source or log is uploaded. Findings are confidence-labeled and require review before code removal.")
            .setItems(options) { _, which -> when (which) {
                0 -> runFullProjectAudit()
                1 -> promptCrashTrace()
                2 -> chooseDiagnosticLog()
                3 -> runDuplicateAnalysis()
                4 -> runFindingAnalysis("Dead code", DiagnosticCategory.DEAD_CODE) { DeadCodeAnalyzer.analyze(it) }
                5 -> runDependencyAnalysis()
                6 -> runFindingAnalysis("Performance", DiagnosticCategory.PERFORMANCE) { PerformanceAnalyzer.analyze(it) }
                else -> runFindingAnalysis("Security", DiagnosticCategory.SECURITY) { SecurityAnalyzer.analyze(it) }
            }}
            .setNegativeButton("Close", null)
            .show()
    }

    private fun promptCrashTrace() {
        val input = EditText(this).apply {
            hint = "Paste FATAL EXCEPTION, Caused by chain, ANR trace, or build crash output"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = Gravity.TOP or Gravity.START
            minLines = 10
            maxLines = 18
            setHorizontallyScrolling(false)
            setPadding(Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 12))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Analyze crash / ANR trace")
            .setView(input)
            .setPositiveButton("Analyze", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text?.toString().orEmpty()
                if (value.isBlank()) { input.error = "Paste a trace or log first"; return@setOnClickListener }
                dialog.dismissCompat()
                analyzeCrashText("Pasted trace", value)
            }
        }
        dialog.show()
    }

    private fun chooseDiagnosticLog() {
        startActivityForResult(Intent("android.intent.action.OPEN_DOCUMENT").apply {
            addOpenableCategoryCompat()
            setMimeTypeCompat("*/*")
            putExtra("android.intent.extra.MIME_TYPES", arrayOf("text/plain", "application/zip", "application/json", "text/html", "application/octet-stream"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, PICK_DIAGNOSTIC_LOG)
    }

    private fun analyzeDiagnosticLog(uri: Uri) {
        showProgress(true)
        statusText.text = "Tracing crash log…"
        projectIo.submit {
            val result = runCatching {
                val name = repository.metadata(uri).name
                val input = repository.openInput(uri) ?: throw IOException("Could not open diagnostic log")
                name to input.use { BuildLogReader.read(name, it) }
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { (name, text) -> analyzeCrashText(name, text) }
                    .onFailure { showError("Diagnostic log failed", it.message ?: "Could not read this log") }
            }
        }
    }

    private fun analyzeCrashText(sourceName: String, text: String) {
        showProgress(true)
        statusText.text = "Resolving project stack frames…"
        val paths = workspaceIndex.filter { !it.node.isDirectory }.map { it.relativePath }
        projectIo.submit {
            val report = CrashTraceAnalyzer.analyze(text, paths)
            val rendered = buildString {
                append("CRASH TRACE REPORT\n")
                append("Source: $sourceName\n")
                append("Kind: ${report.kind}\n")
                append("Fingerprint: ${report.fingerprint}\n")
                append("Root cause: ${report.rootCause}\n")
                append("Secrets redacted: ${report.redactions}")
                if (report.truncated) append(" • log capped at ${CrashTraceAnalyzer.MAX_LOG_CHARS} characters")
                append("\n")
                report.suspectedPath?.let { append("Suspected project frame: $it${report.suspectedLine?.let { line -> ":$line" } ?: ""}\n") }
                append("\nRECOMMENDED NEXT STEPS\n")
                report.recommendations.forEach { append("• $it\n") }
                append("\nSTACK FRAMES\n")
                if (report.frames.isEmpty()) append("No Java/Kotlin stack frames were parsed. Keep the full caused-by chain or ANR main-thread stack.\n")
                report.frames.take(80).forEach { frame ->
                    append(if (frame.projectPath != null) "→ " else "  ")
                    append("${frame.className}.${frame.methodName}(${frame.fileName ?: "Unknown"}${frame.line?.let { ":$it" } ?: ""})")
                    frame.projectPath?.let { append("  [$it]") }
                    append("\n")
                }
            }
            mainHandler.post {
                showProgress(false)
                val path = report.suspectedPath
                if (path != null) showSelectableTextDialog("Crash & ANR Trace", rendered, "Open suspected file") {
                    openWorkspacePath(path, report.suspectedLine ?: 1)
                } else showSelectableTextDialog("Crash & ANR Trace", rendered)
            }
        }
    }

    private fun runFullProjectAudit() {
        runWithDiagnosticFiles("Running full project audit…") { files, truncated ->
            val report = ProjectAuditEngine.analyze(files, truncated)
            mainHandler.post {
                showProgress(false)
                showAuditReport(report)
            }
        }
    }

    private fun showAuditReport(report: com.mohnishraj.novaide.diagnostics.ProjectAuditReport) {
        val categoryCounts = DiagnosticCategory.entries.associateWith { category -> report.findings.count { it.category == category } }
        val options = arrayOf(
            "All findings (${report.findings.size})",
            "Security (${categoryCounts[DiagnosticCategory.SECURITY]})",
            "Performance (${categoryCounts[DiagnosticCategory.PERFORMANCE]})",
            "Dead code (${categoryCounts[DiagnosticCategory.DEAD_CODE]})",
            "Duplication (${categoryCounts[DiagnosticCategory.DUPLICATION]})",
            "Dependencies (${categoryCounts[DiagnosticCategory.DEPENDENCY]})",
            "Copy complete text report"
        )
        AlertDialog.Builder(this)
            .setTitle("Project health • ${report.qualityScore}/100")
            .setMessage(report.summary())
            .setItems(options) { _, which -> when (which) {
                0 -> showDiagnosticFindings("All findings", report.findings, report.summary())
                1 -> showDiagnosticFindings("Security report", report.findings.filter { it.category == DiagnosticCategory.SECURITY }, report.summary())
                2 -> showDiagnosticFindings("Performance report", report.findings.filter { it.category == DiagnosticCategory.PERFORMANCE }, report.summary())
                3 -> showDiagnosticFindings("Dead-code report", report.findings.filter { it.category == DiagnosticCategory.DEAD_CODE }, report.summary())
                4 -> showDiagnosticFindings("Duplication report", report.findings.filter { it.category == DiagnosticCategory.DUPLICATION }, report.summary())
                5 -> showSelectableTextDialog("Dependency map", DependencyGraphAnalyzer.render(report.dependencyGraph))
                else -> copyText("NovaIDE project audit", ProjectAuditEngine.renderFindings(report))
            }}
            .setNegativeButton("Close", null)
            .show()
    }

    private fun runDuplicateAnalysis() {
        runWithDiagnosticFiles("Finding repeated code…") { files, _ ->
            val groups = DuplicateCodeAnalyzer.analyze(files)
            val findings = groups.map { group ->
                DiagnosticFinding(
                    DiagnosticCategory.DUPLICATION,
                    if (group.exactFile) DiagnosticSeverity.MEDIUM else DiagnosticSeverity.LOW,
                    if (group.exactFile) "Duplicate file content" else "Repeated ${group.normalizedLines}-line code block",
                    group.occurrences.joinToString("\n") { "${it.path}:${it.startLine}-${it.endLine}" },
                    "Consolidate only if the copies implement the same responsibility; keep intentional platform-specific variants.",
                    group.occurrences.firstOrNull()?.path,
                    group.occurrences.firstOrNull()?.startLine,
                    confidence = if (group.exactFile) 99 else 82
                )
            }
            mainHandler.post {
                showProgress(false)
                showDiagnosticFindings("Duplicate code • ${groups.size} groups", findings, "Exact files and normalized code windows; generated folders are excluded.")
            }
        }
    }

    private fun runDependencyAnalysis() {
        runWithDiagnosticFiles("Mapping dependencies…") { files, _ ->
            val report = DependencyGraphAnalyzer.analyze(files)
            mainHandler.post {
                showProgress(false)
                showSelectableTextDialog("Dependency map", DependencyGraphAnalyzer.render(report))
            }
        }
    }

    private fun runFindingAnalysis(
        title: String,
        category: DiagnosticCategory,
        analyzer: (List<DiagnosticFile>) -> List<DiagnosticFinding>
    ) {
        runWithDiagnosticFiles("Running ${title.lowercase()} analysis…") { files, _ ->
            val findings = analyzer(files).filter { it.category == category }
            mainHandler.post {
                showProgress(false)
                showDiagnosticFindings("$title • ${findings.size} findings", findings, "Local heuristic analysis. Review confidence and framework/reflection use before changing code.")
            }
        }
    }

    private fun runWithDiagnosticFiles(
        status: String,
        action: (List<DiagnosticFile>, Boolean) -> Unit
    ) {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        showProgress(true)
        statusText.text = status
        projectIo.submit {
            val result = runCatching {
                val scan = if (workspaceIndex.isEmpty()) repository.scan(rootNode, maxEntries = ProjectAuditEngine.MAX_FILES, maxDepth = 60) else null
                val entries = scan?.entries ?: workspaceIndex
                val generated = setOf(".git", ".gradle", "build", "dist", "node_modules", ".idea", "target", "vendor", ".dart_tool")
                val relevant = entries.asSequence().filter { !it.node.isDirectory }
                    .filter { entry -> entry.relativePath.replace('\\', '/').split('/').none { it.lowercase() in generated } }
                    .take(ProjectAuditEngine.MAX_FILES).toList()
                val files = relevant.map { entry ->
                    val canRead = entry.node.size <= 700L * 1024L &&
                        TextFileClassifier.isProbablyText(entry.node.name, entry.node.mimeType) &&
                        !com.mohnishraj.novaide.ai.SecretRedactor.isSensitivePath(entry.relativePath)
                    val text = if (canRead) runCatching { repository.readText(entry.node.uri, 700L * 1024L) }.getOrNull() else null
                    DiagnosticFile(entry.relativePath, entry.node.size, text)
                }
                val truncated = scan?.truncated == true || projectReport?.truncated == true || relevant.size >= ProjectAuditEngine.MAX_FILES
                files to truncated
            }
            result.onSuccess { (files, truncated) -> action(files, truncated) }
                .onFailure { error -> mainHandler.post {
                    showProgress(false)
                    showError("Analysis failed", error.message ?: "Could not analyze this workspace")
                } }
        }
    }

    private fun showDiagnosticFindings(title: String, findings: List<DiagnosticFinding>, header: String) {
        if (findings.isEmpty()) {
            showSelectableTextDialog(title, "$header\n\nNo findings detected in this category.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(header)
            .setItems(findings.take(300).map { finding ->
                "${diagnosticBadge(finding.severity)} ${finding.title}\n${finding.path ?: finding.details}${finding.line?.let { ":$it" } ?: ""} • ${finding.confidence}%"
            }.toTypedArray()) { _, which ->
                val finding = findings[which]
                val details = buildString {
                    append("[${finding.severity}] ${finding.title}\n")
                    finding.path?.let { append("$it${finding.line?.let { line -> ":$line" } ?: ""}\n") }
                    append("Confidence: ${finding.confidence}%\n\n")
                    append(finding.details).append("\n\nRECOMMENDATION\n").append(finding.recommendation)
                    finding.evidence?.let { append("\n\nEVIDENCE\n").append(it) }
                }
                val path = finding.path
                if (path != null && findWorkspaceEntry(path) != null) showSelectableTextDialog(finding.title, details, "Open file") {
                    openWorkspacePath(path, finding.line ?: 1)
                } else showSelectableTextDialog(finding.title, details)
            }
            .setPositiveButton("Copy text report") { _, _ ->
                copyText(title, findings.joinToString("\n\n") { finding ->
                    "[${finding.severity}] ${finding.title}\n${finding.path ?: "Project"}${finding.line?.let { ":$it" } ?: ""}\n${finding.details}\nFix: ${finding.recommendation}\nConfidence: ${finding.confidence}%"
                })
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun diagnosticBadge(severity: DiagnosticSeverity): String = when (severity) {
        DiagnosticSeverity.CRITICAL -> "‼"
        DiagnosticSeverity.HIGH -> "✕"
        DiagnosticSeverity.MEDIUM -> "⚠"
        DiagnosticSeverity.LOW -> "△"
        DiagnosticSeverity.INFO -> "•"
    }



    private fun runUniversalProject() {
        val tab = activeTab
        val extension = tab?.name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (tab != null && extension in setOf("md", "markdown")) {
            startGeneratedDocumentPreview(RuntimeKind.MARKDOWN, tab.name, tab.content)
            return
        }
        if (tab != null && extension in setOf("mmd", "mermaid")) {
            startGeneratedDocumentPreview(RuntimeKind.MERMAID, tab.name, tab.content)
            return
        }
        loadRuntimeProject { project ->
            when {
                project.detectedOutput != null -> runWebPreview(project.detectedOutput)
                project.kind == RuntimeKind.STATIC_WEB -> runWebPreview()
                project.kind in setOf(RuntimeKind.MARKDOWN, RuntimeKind.MERMAID) -> showRuntimeCenter(project)
                project.commands.isNotEmpty() -> showRuntimeCenter(project)
                else -> showError("No runnable project detected", project.warning ?: "Open a browser-ready HTML, Markdown, Mermaid, Node, Python, PHP, Hugo or Jekyll project.")
            }
        }
    }

    private fun loadRuntimeProject(onReady: (RuntimeProject) -> Unit) {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        fun analyze(entries: List<FileRepository.WorkspaceEntry>) {
            showProgress(true)
            statusText.text = "Detecting project runtime…"
            projectIo.submit {
                val packageEntry = entries.firstOrNull { !it.node.isDirectory && it.relativePath.equals("package.json", ignoreCase = true) }
                val packageText = packageEntry?.let { runCatching { repository.readText(it.node.uri, 512_000) }.getOrNull() }
                val text = if (packageText == null) emptyMap() else mapOf("package.json" to packageText)
                val result = runCatching { UniversalRuntimeEngine.detect(entries.map { it.relativePath }, text) }
                mainHandler.post {
                    showProgress(false)
                    result.onSuccess(onReady).onFailure { showError("Runtime detection failed", it.message ?: "Could not inspect this project") }
                }
            }
        }
        if (workspaceIndex.isNotEmpty()) analyze(workspaceIndex)
        else {
            showProgress(true)
            projectIo.submit {
                val scan = runCatching { repository.scan(rootNode) }
                mainHandler.post {
                    showProgress(false)
                    scan.onSuccess {
                        workspaceIndex = it.entries
                        webPreviewServer.updateWorkspace(workspaceIndex)
                        analyze(workspaceIndex)
                    }.onFailure { showError("Runtime detection failed", it.message ?: "Could not index workspace") }
                }
            }
        }
    }

    private fun showRuntimeCenter(projectOverride: RuntimeProject? = null) {
        if (projectOverride == null) {
            loadRuntimeProject { showRuntimeCenter(it) }
            return
        }
        val project = projectOverride
        val environment = termuxBridge.environment(store.workspaceUri, runtimeSettings.allowExternalAppsConfirmed)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 12))
            addView(Ui.text(this@MainActivity, "${project.kind.label} · ${project.confidence}% confidence", 16f, palette.textPrimary, bold = true).apply { setPadding(0, 4, 0, 4) })
            addView(Ui.text(this@MainActivity,
                buildString {
                    project.packageManager?.let { append("Package manager: $it\n") }
                    project.detectedOutput?.let { append("Runnable output: $it\n") }
                    append("Evidence: ").append(project.evidence.joinToString())
                    project.warning?.let { append("\n\n⚠ ").append(it) }
                }, 12f, palette.textSecondary).apply { setPadding(0, 0, 0, 12) })
        }
        project.detectedOutput?.let { output ->
            container.addView(Button(this).apply {
                text = "▶ Preview $output"; isAllCaps = false
                setOnClickListener { runWebPreview(output) }
            })
        }
        if (project.kind == RuntimeKind.STATIC_WEB) {
            container.addView(Button(this).apply { text = "▶ Run Web Preview"; isAllCaps = false; setOnClickListener { runWebPreview() } })
        }
        container.addView(Ui.text(this, "Termux execution", 14f, palette.textPrimary, bold = true).apply { setPadding(0, 16, 0, 4) })
        val status = buildString {
            append(if (environment.termuxInstalled) "✓" else "✕").append(" Termux installed\n")
            append(if (environment.runPermissionGranted) "✓" else "✕").append(" RUN_COMMAND permission\n")
            append(if (environment.sharedWorkspacePath != null) "✓" else "✕").append(" Shared-storage workspace path")
            environment.sharedWorkspacePath?.let { append(": $it") }
            append('\n').append(if (environment.allowExternalAppsConfirmed) "✓" else "✕").append(" allow-external-apps confirmed")
        }
        container.addView(Ui.text(this, status, 12f, if (environment.canExecute) palette.textPrimary else palette.danger).apply { setPadding(0, 0, 0, 8) })
        project.commands.forEach { command ->
            container.addView(Button(this).apply {
                text = "${runtimeActionIcon(command.action)} ${command.label}\n${command.display()}"
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                isEnabled = environment.canExecute
                setOnClickListener { confirmTermuxCommand(command) }
            })
        }
        if (project.commands.isEmpty()) {
            container.addView(Ui.text(this, "This project does not need an external runtime command for its detected preview.", 12f, palette.textSecondary).apply { setPadding(0, 8, 0, 8) })
        }
        container.addView(Button(this).apply {
            text = "Open local development server preview"; isAllCaps = false
            setOnClickListener { promptLoopbackPreview(project.command(RuntimeAction.DEVELOP)?.defaultPort ?: runtimeSettings.lastServerPort) }
        })
        container.addView(Button(this).apply {
            text = "Termux setup & permissions"; isAllCaps = false
            setOnClickListener { showTermuxSetup() }
        })
        termuxResults.latest()?.let { latest ->
            container.addView(Button(this).apply {
                text = "Latest command result: ${if (latest.succeeded) "Success" else "Failed"} · ${latest.label}"; isAllCaps = false
                setOnClickListener { showSelectableTextDialog("Termux result", latest.render()) }
            })
        }
        AlertDialog.Builder(this).setTitle("Universal Runtime · M10")
            .setView(ScrollView(this).apply { addView(container) })
            .setPositiveButton("Refresh") { _, _ -> showRuntimeCenter() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun runtimeActionIcon(action: RuntimeAction): String = when (action) {
        RuntimeAction.INSTALL -> "↓"
        RuntimeAction.BUILD -> "⚙"
        RuntimeAction.DEVELOP -> "▶"
        RuntimeAction.RUN -> "▷"
        RuntimeAction.TEST -> "✓"
    }

    private fun showTermuxSetup() {
        val environment = termuxBridge.environment(store.workspaceUri, runtimeSettings.allowExternalAppsConfirmed)
        val setupCommand = "mkdir -p ~/.termux && (grep -q '^allow-external-apps=true$' ~/.termux/termux.properties 2>/dev/null || printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties)"
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8))
            addView(Ui.text(this@MainActivity,
                "NovaIDE never runs a Termux command silently. Every exact executable, arguments and working directory are shown for confirmation first. Command results stay on this device.",
                12f, palette.textSecondary).apply { setPadding(0, 2, 0, 12) })
            addView(Ui.text(this@MainActivity, environment.blockers().ifEmpty { listOf("Termux bridge is ready.") }.joinToString("\n• ", prefix = "• "), 12f, palette.textPrimary).apply { setPadding(0, 0, 0, 12) })
        }
        container.addView(Button(this).apply {
            text = if (environment.termuxInstalled) "Open Termux" else "Install Termux from official releases"
            isAllCaps = false
            setOnClickListener {
                if (environment.termuxInstalled) {
                    val launch = packageManager.getLaunchIntentForPackage(TermuxBridge.TERMUX_PACKAGE)
                    if (launch != null) startActivity(launch) else toast("Could not open Termux")
                } else openExternalUrl("https://github.com/termux/termux-app/releases")
            }
        })
        container.addView(Button(this).apply {
            text = "Grant RUN_COMMAND permission"; isAllCaps = false
            setOnClickListener {
                if (termuxBridge.hasPermission()) toast("Permission already granted")
                else requestPermissions(arrayOf(TermuxBridge.RUN_PERMISSION), REQUEST_TERMUX_PERMISSION)
            }
        })
        container.addView(Button(this).apply {
            text = "Open NovaIDE App Info"; isAllCaps = false
            setOnClickListener { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        })
        container.addView(Button(this).apply {
            text = "Copy allow-external-apps setup command"; isAllCaps = false
            setOnClickListener { copyText("Termux setup", setupCommand); toast("Setup command copied") }
        })
        container.addView(CheckBox(this).apply {
            text = "I enabled allow-external-apps=true and restarted Termux"
            setTextColor(palette.textPrimary)
            isChecked = runtimeSettings.allowExternalAppsConfirmed
            setOnCheckedChangeListener { _, checked -> runtimeSettings.allowExternalAppsConfirmed = checked }
        })
        container.addView(Ui.text(this,
            "Workspace requirement: choose a project under shared internal storage, such as Download or Documents. NovaIDE intentionally refuses to invent filesystem paths for cloud providers or virtual SAF folders.",
            11f, palette.textSecondary).apply { setPadding(0, 10, 0, 4) })
        AlertDialog.Builder(this).setTitle("Termux Bridge setup")
            .setView(ScrollView(this).apply { addView(container) })
            .setNegativeButton("Close", null).show()
    }

    private fun confirmTermuxCommand(command: com.mohnishraj.novaide.runtime.RuntimeCommand) {
        val environment = termuxBridge.environment(store.workspaceUri, runtimeSettings.allowExternalAppsConfirmed)
        if (!environment.canExecute) {
            showError("Termux bridge is not ready", environment.blockers().joinToString("\n\n"))
            return
        }
        val workDir = environment.sharedWorkspacePath ?: return
        val message = buildString {
            append(command.description).append("\n\nCommand\n").append(command.display())
            append("\n\nWorking directory\n").append(workDir)
            append("\n\nNovaIDE will send this exact command to Termux only after you approve it.")
        }
        AlertDialog.Builder(this).setTitle(command.label).setMessage(message)
            .setPositiveButton("Run in Termux") { _, _ ->
                runCatching {
                    val id = termuxBridge.run(command, workDir, foreground = command.opensServer)
                    pendingTermuxExecutionId = id
                    pendingTermuxAction = command.action
                    pendingRuntimePort = command.defaultPort
                    statusText.text = "Sent to Termux • ${command.label}"
                    scheduleTermuxResultPoll()
                    if (command.opensServer) {
                        val port = command.defaultPort ?: runtimeSettings.lastServerPort
                        runtimeSettings.lastServerPort = port
                        mainHandler.postDelayed({ startLoopbackPreview(port) }, 1400L)
                    } else toast("Command sent to Termux")
                }.onFailure { error -> showError("Could not start Termux command", error.message ?: "Termux rejected the request") }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun scheduleTermuxResultPoll() {
        termuxResultPoll?.let { mainHandler.removeCallbacks(it) }
        var attempts = 0
        val task = object : Runnable {
            override fun run() {
                if (consumeTermuxResult()) return
                attempts++
                if (pendingTermuxExecutionId != null && attempts < 600) mainHandler.postDelayed(this, 1_000L)
                else termuxResultPoll = null
            }
        }
        termuxResultPoll = task
        mainHandler.postDelayed(task, 1_000L)
    }

    private fun consumeTermuxResult(): Boolean {
        val result = termuxResults.takeUnread() ?: return false
        val matching = pendingTermuxExecutionId == null || pendingTermuxExecutionId == result.executionId
        if (matching) {
            val action = pendingTermuxAction
            pendingTermuxExecutionId = null
            pendingTermuxAction = null
            pendingRuntimePort = null
            termuxResultPoll?.let { mainHandler.removeCallbacks(it) }
            termuxResultPoll = null
            statusText.text = if (result.succeeded) "Runtime command completed" else "Runtime command failed"
            AlertDialog.Builder(this)
                .setTitle(if (result.succeeded) "Command completed" else "Command failed")
                .setMessage(result.render())
                .setPositiveButton("Copy output") { _, _ -> copyText("Termux result", result.render()) }
                .setNeutralButton(if (result.succeeded && action == RuntimeAction.BUILD) "Refresh & Run" else "Runtime Center") { _, _ ->
                    if (result.succeeded && action == RuntimeAction.BUILD) {
                        workspaceIndex = emptyList()
                        reloadWorkspaceTree()
                        mainHandler.postDelayed({ runUniversalProject() }, 1_000L)
                    } else showRuntimeCenter()
                }
                .setNegativeButton("Close", null).show()
        }
        return matching
    }

    private fun promptLoopbackPreview(defaultPort: Int) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(defaultPort.coerceIn(1024, 65535).toString())
            selectAll()
        }
        AlertDialog.Builder(this).setTitle("Open local server")
            .setMessage("Enter the port used by a server running in Termux. NovaIDE permits only this explicit loopback origin.")
            .setView(input)
            .setPositiveButton("Open") { _, _ ->
                val port = input.text.toString().toIntOrNull()
                runCatching { TermuxCommandPolicy.safePort(port ?: -1) }
                    .onSuccess { runtimeSettings.lastServerPort = it; startLoopbackPreview(it) }
                    .onFailure { showError("Invalid port", it.message ?: "Use a port from 1024 to 65535") }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun startLoopbackPreview(port: Int) {
        val safePort = runCatching { TermuxCommandPolicy.safePort(port) }.getOrElse { showError("Invalid runtime port", it.message.orEmpty()); return }
        val origin = "http://127.0.0.1:$safePort"
        val webView = ensurePreviewWebView() ?: return
        webPreviewServer.allowedRuntimeOrigin = origin
        previewDocumentKind = null
        previewEntryPath = "$origin/"
        previewVisible = true
        previewFullscreen = false
        webConsole.clear()
        applyPreviewLayout()
        previewStatusText.text = "Local server · $safePort"
        statusText.text = "Connecting to Termux server on port $safePort…"
        webView.loadUrl("$origin/")
    }

    private fun startGeneratedDocumentPreview(kind: RuntimeKind, title: String, source: String) {
        val html = runCatching {
            when (kind) {
                RuntimeKind.MARKDOWN -> DocumentPreviewGenerator.markdown(title, source)
                RuntimeKind.MERMAID -> DocumentPreviewGenerator.mermaid(title, source)
                else -> error("Unsupported document preview")
            }
        }.getOrElse { showError("Document preview failed", it.message.orEmpty()); return }
        val webView = ensurePreviewWebView() ?: return
        webPreviewServer.allowedRuntimeOrigin = null
        previewDocumentKind = kind
        previewEntryPath = "${kind.label} · $title"
        previewVisible = true
        previewFullscreen = false
        webConsole.clear()
        applyPreviewLayout()
        previewStatusText.text = previewEntryPath
        statusText.text = "Rendering ${kind.label}…"
        webView.loadDataWithBaseURL("https://${WebPreviewEngine.HOST}/__document__/", WebPreviewEngine.injectDiagnostics(html), "text/html", "UTF-8", null)
    }

    private fun showCommandPalette() {
        val query = EditText(this).apply {
            hint = "Type a command…"
            setSingleLine(true)
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 8))
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(query, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 52)))
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 430)))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Command Palette · Ctrl+Shift+P")
            .setView(container).setNegativeButton("Close", null).create()

        fun render(value: String) {
            list.removeAllViews()
            val commands = CommandPaletteEngine.search(buildPaletteCommands(), value)
            if (commands.isEmpty()) {
                list.addView(Ui.text(this, "No matching command", 13f, palette.textSecondary).apply { setPadding(16, 18, 16, 18) })
            } else commands.forEach { command ->
                val shortcut = command.shortcut.takeIf { it.isNotBlank() }?.let { "  ·  $it" }.orEmpty()
                val button = Button(this).apply {
                    text = "${command.title}$shortcut\n${command.category} · ${command.source}${command.description.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()}"
                    isAllCaps = false
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setOnClickListener {
                        dialog.dismissCompat()
                        runPaletteCommand(command.id)
                    }
                }
                list.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        query.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render(s?.toString().orEmpty())
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        render("")
        dialog.show()
        query.requestFocus()
    }

    private fun buildPaletteCommands(): List<PaletteCommand> {
        val builtIns = listOf(
            PaletteCommand("file.save", "Save current file", "Write the active editor tab.", "File", shortcut = "Ctrl+S"),
            PaletteCommand("file.saveAll", "Save all files", "Write every changed tab.", "File"),
            PaletteCommand("workspace.open", "Open workspace", "Choose an Android SAF project folder.", "Workspace", shortcut = "Ctrl+O"),
            PaletteCommand("workspace.search", "Search workspace", "Find file names and content.", "Workspace", shortcut = "Ctrl+Shift+F"),
            PaletteCommand("runtime.run", "Run project", "Preview browser-ready files or open the Universal Runtime Center.", "Run", shortcut = "Ctrl+Shift+R", keywords = listOf("runtime", "termux", "build", "serve", "preview")),
            PaletteCommand("runtime.center", "Universal Runtime Center", "Detect frameworks, build outputs and Termux commands.", "Run", keywords = listOf("react", "vite", "python", "php", "node")),
            PaletteCommand("web.previewCenter", "Web Preview settings", "Choose entry point, viewport and security controls.", "Web"),
            PaletteCommand("editor.find", "Find and replace", "Search the active file.", "Editor", shortcut = "Ctrl+F"),
            PaletteCommand("editor.symbol", "Go to symbol", "Navigate classes and functions.", "Editor", shortcut = "Ctrl+P"),
            PaletteCommand("editor.autocomplete", "Autocomplete", "Show local code suggestions.", "Editor", shortcut = "Ctrl+Space"),
            PaletteCommand("git.center", "Git & GitHub", "Open repository status and remote tools.", "Source Control", shortcut = "Ctrl+Shift+G"),
            PaletteCommand("ai.center", "AI Assistant", "Use the configured AI provider.", "AI", shortcut = "Ctrl+Shift+I"),
            PaletteCommand("diagnostics.audit", "Run project health audit", "Security, performance, duplicates and dead-code analysis.", "Analyze", keywords = listOf("health", "security", "performance")),
            PaletteCommand("android.center", "Android Tools", "Inspect manifests, resources, Gradle and APKs.", "Android"),
            PaletteCommand("extensions.center", "Extensions", "Manage permission-sandboxed plugins.", "Extensions"),
            PaletteCommand("tasks.center", "Tasks & Nova Console", "Run safe commands and saved workflows.", "Productivity"),
            PaletteCommand("credentials.center", "Credentials Center", "Manage Git and AI credentials.", "Account")
        )
        val plugins = pluginStore.installed().filter { it.enabled }.flatMap { installed ->
            installed.manifest.commands.map { command ->
                PaletteCommand(
                    id = "plugin:${installed.manifest.id}:${command.id}",
                    title = command.title,
                    description = command.description,
                    category = "Extension",
                    keywords = command.keywords,
                    source = installed.manifest.name
                )
            }
        }
        val tasks = (TaskRunner.builtIns() + productivityStore.customTasks()).map { task ->
            PaletteCommand("task:${task.id}", task.name, task.description, "Task", source = "Nova Task Runner")
        }
        return builtIns + plugins + tasks
    }

    private fun runPaletteCommand(id: String) {
        when {
            id.startsWith("plugin:") -> {
                val parts = id.split(':', limit = 3)
                val plugin = pluginStore.installed().firstOrNull { it.manifest.id == parts.getOrNull(1) }
                if (plugin == null) showError("Extension unavailable", "The selected extension is no longer installed.")
                else executePluginCommand(plugin, parts.getOrNull(2).orEmpty())
            }
            id.startsWith("task:") -> {
                val taskId = id.removePrefix("task:")
                val task = (TaskRunner.builtIns() + productivityStore.customTasks()).firstOrNull { it.id == taskId }
                if (task == null) showError("Task unavailable", "The selected task no longer exists.") else runNovaTask(task)
            }
            id == "file.save" -> saveActive(false)
            id == "file.saveAll" -> saveAllDirty(false)
            id == "workspace.open" -> chooseWorkspace()
            id == "workspace.search" -> showWorkspaceSearch()
            id == "runtime.run" -> runUniversalProject()
            id == "runtime.center" -> showRuntimeCenter()
            id == "web.previewCenter" -> showWebPreviewCenter()
            id == "editor.find" -> openSearch()
            id == "editor.symbol" -> showSymbols()
            id == "editor.autocomplete" -> showAutocomplete()
            id == "git.center" -> showGitHubCenter()
            id == "ai.center" -> showAiCenter()
            id == "diagnostics.audit" -> runFullProjectAudit()
            id == "android.center" -> showAndroidCenter()
            id == "extensions.center" -> showExtensionsCenter()
            id == "tasks.center" -> showProductivityCenter()
            id == "credentials.center" -> showCredentialsCenter()
        }
    }

    private fun showExtensionsCenter() {
        val installed = pluginStore.installed()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8))
            addView(Ui.text(this@MainActivity,
                "Nova extensions are declarative and permission-sandboxed. They cannot execute arbitrary APK/JAR code or read credentials.",
                12f, palette.textSecondary).apply { setPadding(4, 4, 4, 12) })
        }
        if (installed.isEmpty()) {
            container.addView(Ui.text(this, "No extensions installed.", 13f, palette.textSecondary).apply { setPadding(4, 14, 4, 14) })
        } else installed.forEach { plugin ->
            container.addView(Button(this).apply {
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                text = "${if (plugin.enabled) "●" else "○"} ${plugin.manifest.name}  v${plugin.manifest.version}\n${plugin.manifest.commands.size} commands · ${plugin.manifest.permissions.size} permissions"
                setOnClickListener { showPluginDetails(plugin) }
            })
        }
        val dialog = AlertDialog.Builder(this).setTitle("Extensions")
            .setView(ScrollView(this).apply { addView(container) })
            .setPositiveButton("Import manifest") { _, _ -> choosePluginManifest() }
            .setNeutralButton("Paste manifest") { _, _ -> promptPastePluginManifest() }
            .setNegativeButton("Close", null).create()
        dialog.show()
    }

    private fun choosePluginManifest() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            setMimeTypeCompat("application/json")
            addOpenableCategoryCompat()
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, PICK_PLUGIN_MANIFEST)
    }

    private fun installPluginFromUri(uri: Uri) {
        projectIo.submit {
            val raw = runCatching { repository.readText(uri, 128_000L) }
            mainHandler.post {
                raw.onSuccess(::confirmPluginInstall).onFailure { showError("Extension import failed", it.message ?: "Could not read manifest") }
            }
        }
    }

    private fun promptPastePluginManifest() {
        val input = EditText(this).apply {
            hint = "Paste .nova-plugin.json manifest"
            gravity = Gravity.TOP or Gravity.START
            minLines = 10
            typeface = Typeface.MONOSPACE
        }
        AlertDialog.Builder(this).setTitle("Paste extension manifest").setView(input)
            .setPositiveButton("Review") { _, _ -> confirmPluginInstall(input.text.toString()) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmPluginInstall(raw: String) {
        val manifest = runCatching { PluginManifestParser.parse(raw) }.getOrElse {
            showError("Invalid extension manifest", it.message ?: "Manifest could not be parsed")
            return
        }
        val permissionText = if (manifest.permissions.isEmpty()) "No permissions requested." else manifest.permissions.joinToString("\n") {
            "• ${it.label}: ${it.explanation}"
        }
        AlertDialog.Builder(this)
            .setTitle("Install ${manifest.name}?")
            .setMessage("${manifest.description}\n\nAuthor: ${manifest.author.ifBlank { "Not provided" }}\nVersion: ${manifest.version}\n\nPermissions\n$permissionText\n\nExtensions only act after you explicitly run a command.")
            .setPositiveButton("Install") { _, _ ->
                runCatching { pluginStore.install(raw) }
                    .onSuccess { toast("Installed ${it.manifest.name}") }
                    .onFailure { showError("Install failed", it.message ?: "Unknown manifest error") }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showPluginDetails(plugin: InstalledPlugin) {
        val manifest = plugin.manifest
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8))
            addView(Ui.text(this@MainActivity, manifest.description.ifBlank { "No description" }, 12f, palette.textSecondary).apply { setPadding(4, 4, 4, 12) })
        }
        manifest.commands.forEach { command ->
            content.addView(Button(this).apply {
                isEnabled = plugin.enabled
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                text = "${command.title}\n${command.description.ifBlank { command.action.name.lowercase() }}"
                setOnClickListener { executePluginCommand(plugin, command.id) }
            })
        }
        AlertDialog.Builder(this).setTitle("${manifest.name} · v${manifest.version}")
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton(if (plugin.enabled) "Disable" else "Enable") { _, _ ->
                pluginStore.setEnabled(manifest.id, !plugin.enabled)
                toast(if (plugin.enabled) "Extension disabled" else "Extension enabled")
            }
            .setNeutralButton("Uninstall") { _, _ ->
                AlertDialog.Builder(this).setTitle("Uninstall ${manifest.name}?")
                    .setPositiveButton("Uninstall") { _, _ -> pluginStore.uninstall(manifest.id); toast("Extension uninstalled") }
                    .setNegativeButton("Cancel", null).show()
            }
            .setNegativeButton("Close", null).show()
    }

    private fun executePluginCommand(plugin: InstalledPlugin, commandId: String) {
        val plan = runCatching { PluginPolicy.plan(plugin, commandId) }.getOrElse {
            showError("Extension blocked", it.message ?: "Command violates the extension sandbox")
            return
        }
        when (plan.action) {
            PluginActionType.CONSOLE -> buildConsoleContext { context ->
                showSelectableTextDialog("${plugin.manifest.name} · Console", NovaConsoleEngine.execute(plan.value, context).output, "Copy") {
                    copyText("extension output", NovaConsoleEngine.execute(plan.value, context).output)
                }
            }
            PluginActionType.INSERT -> {
                val tab = activeTab ?: run { showError("No active editor", "Open a writable text file before running this command."); return }
                if (tab.isReadOnly) { showError("Read-only file", "This extension cannot edit the active file."); return }
                val start = editor.selectionStart.coerceAtLeast(0)
                val end = editor.selectionEnd.coerceAtLeast(start)
                editor.replaceRange(start, end, plan.value)
                toast("${plugin.manifest.name} inserted text")
            }
            PluginActionType.OPEN_URL -> openExternalUrl(plan.value)
            PluginActionType.MESSAGE -> showError(plugin.manifest.name, plan.value)
            PluginActionType.COPY -> copyText(plugin.manifest.name, plan.value)
        }
    }

    private fun showProductivityCenter() {
        val tasks = TaskRunner.builtIns() + productivityStore.customTasks()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8))
            addView(Ui.text(this@MainActivity,
                "Nova Console is a workspace-aware safe console, not an unrestricted Android shell. Saved tasks can chain up to 20 approved commands.",
                12f, palette.textSecondary).apply { setPadding(4, 4, 4, 12) })
            addView(Button(this@MainActivity).apply { text = "Open Nova Console"; isAllCaps = false; setOnClickListener { showNovaConsole() } })
            addView(Button(this@MainActivity).apply { text = "Create saved task / workflow"; isAllCaps = false; setOnClickListener { promptCreateTask() } })
        }
        tasks.forEach { task ->
            content.addView(Button(this).apply {
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                text = "▶ ${task.name}\n${task.description.ifBlank { task.commands.joinToString(" → ") }}"
                setOnClickListener { runNovaTask(task) }
                setOnLongClickListener {
                    if (task.id.startsWith("nova.")) toast("Built-in tasks cannot be deleted")
                    else AlertDialog.Builder(this@MainActivity).setTitle("Delete ${task.name}?")
                        .setPositiveButton("Delete") { _, _ -> productivityStore.deleteTask(task.id); toast("Task deleted") }
                        .setNegativeButton("Cancel", null).show()
                    true
                }
            })
        }
        AlertDialog.Builder(this).setTitle("Tasks & Nova Console")
            .setView(ScrollView(this).apply { addView(content) }).setNegativeButton("Close", null).show()
    }

    private fun promptCreateTask() {
        val name = EditText(this).apply { hint = "Task name"; setSingleLine(true) }
        val id = EditText(this).apply { hint = "id, e.g. my.precommit.check"; setSingleLine(true) }
        val description = EditText(this).apply { hint = "Description"; setSingleLine(true) }
        val commands = EditText(this).apply {
            hint = "One safe command per line\nproject-info\ngrep -i TODO"
            minLines = 8
            gravity = Gravity.TOP or Gravity.START
            typeface = Typeface.MONOSPACE
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 8)
            addView(name); addView(id); addView(description); addView(commands)
        }
        AlertDialog.Builder(this).setTitle("Create task / workflow").setView(form)
            .setPositiveButton("Save") { _, _ ->
                val task = NovaTask(
                    id.text.toString().trim().lowercase(), name.text.toString().trim(), description.text.toString().trim(),
                    commands.text.toString().lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                )
                runCatching { productivityStore.saveTask(task) }
                    .onSuccess { toast("Saved ${task.name}") }
                    .onFailure { showError("Task not saved", it.message ?: "Invalid task") }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun runNovaTask(task: NovaTask) {
        buildConsoleContext { context ->
            val result = TaskRunner.run(task, context)
            showSelectableTextDialog(
                "${if (result.success) "✓" else "✕"} ${task.name}",
                "Completed ${result.completedCommands}/${task.commands.size} commands\n\n${result.output}",
                "Copy"
            ) { copyText("task output", result.output) }
        }
    }

    private fun showNovaConsole() {
        buildConsoleContext { context ->
            val output = Ui.text(this, "Nova Console ready. Type 'help'.", 11.5f, palette.textPrimary, gravity = Gravity.TOP or Gravity.START).apply {
                typeface = Typeface.MONOSPACE
                setTextIsSelectable(true)
                setPadding(12, 12, 12, 12)
            }
            val input = EditText(this).apply {
                hint = "project-info"
                setSingleLine(true)
                typeface = Typeface.MONOSPACE
            }
            val run = Button(this).apply { text = "Run"; isAllCaps = false }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(input, LinearLayout.LayoutParams(0, Ui.dp(this@MainActivity, 52), 1f))
                addView(run, LinearLayout.LayoutParams(Ui.dp(this@MainActivity, 78), Ui.dp(this@MainActivity, 52)))
            }
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(ScrollView(this@MainActivity).apply { addView(output) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this@MainActivity, 390)))
                addView(row)
            }
            val dialog = AlertDialog.Builder(this).setTitle("Nova Console · Safe workspace tools")
                .setView(container).setNegativeButton("Close", null).create()
            fun execute() {
                val command = input.text.toString().trim()
                if (command.isBlank()) return
                val result = NovaConsoleEngine.execute(command, context)
                if (result.output == "__NOVA_CLEAR__") output.text = ""
                else output.append("\n\n$ $command\n${result.output}")
                input.setText("")
            }
            run.setOnClickListener { execute() }
            input.setOnEditorActionListener { _, _, _ -> execute(); true }
            dialog.show()
            input.requestFocus()
        }
    }

    private fun buildConsoleContext(onReady: (ConsoleContext) -> Unit) {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        val workspaceTree = store.workspaceUri ?: run { toast("Open a workspace first"); return }
        if (workspaceIndex.isEmpty()) { toast("Workspace index is still loading"); return }
        showProgress(true)
        val snapshot = workspaceIndex.toList()
        val active = activeTab
        val activePath = active?.let { tab -> snapshot.firstOrNull { it.node.uri == tab.uri }?.relativePath }
        val selection = editor.selectedTextOrWord().orEmpty()
        projectIo.submit {
            val files = mutableListOf<ConsoleFile>()
            var textBudget = 6L * 1024L * 1024L
            var indexedTextFiles = 0
            for (entry in snapshot.asSequence().filter { !it.node.isDirectory }.take(2_000)) {
                if (Thread.currentThread().isInterrupted) break
                var content: String? = null
                if (TextFileClassifier.isProbablyText(entry.node.name, entry.node.mimeType) && textBudget > 0L && indexedTextFiles < 700) {
                    content = if (active?.uri == entry.node.uri) active.content else runCatching {
                        repository.readText(entry.node.uri, minOf(128L * 1024L, textBudget))
                    }.getOrNull()
                    if (content != null) {
                        indexedTextFiles++
                        textBudget -= content.toByteArray(Charsets.UTF_8).size.toLong()
                    }
                }
                files += ConsoleFile(entry.relativePath, entry.node.size, content)
            }
            val context = ConsoleContext(files, rootNode.name, activePath, selection)
            mainHandler.post {
                showProgress(false)
                if (store.workspaceUri == workspaceTree) onReady(context)
            }
        }
    }

    private fun showAndroidCenter() {
        val rootNode = workspaceRoot ?: run { toast("Open a workspace first"); return }
        showProgress(true)
        statusText.text = "Inspecting Android project…"
        projectIo.submit {
            val result = runCatching { buildAndroidProjectReport(rootNode) }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { report ->
                    if (!report.isAndroidProject) {
                        showError("Android project not detected", "NovaIDE did not find an Android Gradle module or src/main/AndroidManifest.xml in this workspace.")
                    } else showAndroidCenterDialog(report)
                }.onFailure { showError("Android inspection failed", it.message ?: "Could not inspect this workspace") }
            }
        }
    }

    private fun buildAndroidProjectReport(rootNode: DocumentNode): AndroidProjectReport {
        val entries = workspaceIndex.ifEmpty {
            repository.scan(rootNode, maxEntries = 12_000, maxDepth = 60).entries
        }
        val sourceFiles = entries.filter { !it.node.isDirectory }.map { entry ->
            val lower = entry.relativePath.lowercase()
            val shouldRead = lower.endsWith("build.gradle") || lower.endsWith("build.gradle.kts") ||
                lower.endsWith("settings.gradle") || lower.endsWith("settings.gradle.kts") ||
                lower.endsWith("androidmanifest.xml") || lower.endsWith("gradle.properties") ||
                lower.endsWith("libs.versions.toml")
            val content = if (shouldRead && entry.node.size <= 768L * 1024L) {
                runCatching { repository.readText(entry.node.uri, 768L * 1024L) }.getOrNull()
            } else null
            AndroidSourceFile(entry.relativePath, content, entry.node.size)
        }
        return AndroidProjectAnalyzer.analyze(sourceFiles, rootNode.name)
    }

    private fun showAndroidCenterDialog(report: AndroidProjectReport) {
        var dialog: AlertDialog? = null
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8))
        }
        val errorCount = report.issues.count { it.severity == AndroidIssueSeverity.ERROR }
        val warningCount = report.issues.count { it.severity == AndroidIssueSeverity.WARNING }
        container.addView(Ui.text(this, report.projectName, 17f, palette.textPrimary, bold = true))
        container.addView(Ui.text(this,
            "${report.modules.size} modules • ${report.applicationModules} app • ${report.sourceFiles} source • ${report.resourceFiles} resources",
            11.5f, palette.textSecondary
        ).apply { setPadding(0, Ui.dp(this@MainActivity, 4), 0, Ui.dp(this@MainActivity, 4)) })
        container.addView(Ui.text(this,
            when {
                errorCount > 0 -> "✕ $errorCount errors • $warningCount warnings"
                warningCount > 0 -> "⚠ $warningCount warnings • no blocking errors"
                else -> "✓ No blocking Android project issues detected"
            }, 12f, if (errorCount > 0) palette.danger else palette.textSecondary, bold = errorCount > 0
        ).apply { setPadding(0, 0, 0, Ui.dp(this@MainActivity, 12)) })

        fun row(left: String, leftAction: () -> Unit, right: String, rightAction: () -> Unit) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fun button(label: String, action: () -> Unit) = Button(this).apply {
                text = label
                setOnClickListener { dialog?.dismiss(); action() }
            }
            line.addView(button(left, leftAction), LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f))
            line.addView(button(right, rightAction), LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f))
            container.addView(line)
        }
        row("Project Inspector", { showAndroidProjectInspector(report) }, "Manifest", { showManifestCenter(report) })
        row("Resources", { showResourceCenter() }, "Gradle Builds", { showGradleBuildCenter(report) })
        row("APK Analyzer", { chooseApkForInspection() }, "Build Logs", { chooseBuildLog() })
        dialog = AlertDialog.Builder(this)
            .setTitle("Android Tools")
            .setView(ScrollView(this).apply { addView(container) })
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAndroidProjectInspector(report: AndroidProjectReport) {
        val text = buildString {
            append("PROJECT\n")
            append("Modules: ${report.modules.size} (${report.applicationModules} application)\n")
            append("Sources: ${report.sourceFiles} • Tests: ${report.testFiles}\n")
            append("Resources: ${report.resourceFiles} • Assets: ${report.assetFiles} • Native libs: ${report.nativeLibraries}\n")
            append("Permissions: ${report.permissions.size} • Components: ${report.components.size}\n\n")
            append("MODULES\n")
            report.modules.forEach { module ->
                append("\n${if (module.isApplication) "APP" else "LIB"}  ${module.name}\n")
                append("  namespace: ${module.namespace ?: "not resolved"}\n")
                if (module.isApplication) append("  applicationId: ${module.applicationId ?: "not resolved"}\n")
                append("  SDK: min ${module.minSdk ?: "?"} • target ${module.targetSdk ?: "?"} • compile ${module.compileSdk ?: "?"}\n")
                if (module.versionName != null || module.versionCode != null) append("  version: ${module.versionName ?: "?"} (${module.versionCode ?: "?"})\n")
                append("  dependencies: ${module.dependencies.size}\n")
                if (module.buildTypes.isNotEmpty()) append("  build types: ${module.buildTypes.joinToString()}\n")
            }
            append("\nDEPENDENCIES (${report.dependencyCount})\n")
            report.modules.flatMap { it.dependencies }.take(100).forEach { append("${it.configuration}: ${it.notation}\n") }
            if (report.dependencyCount > 100) append("… ${report.dependencyCount - 100} more\n")
            append("\nISSUES (${report.issues.size})\n")
            report.issues.forEach { issue ->
                append("${androidIssueBadge(issue.severity)} ${issue.title}\n")
                append("${issue.detail}\n")
                issue.path?.let { append("${it}${issue.line?.let { line -> ":$line" }.orEmpty()}\n") }
                append('\n')
            }
        }
        showSelectableTextDialog("Android Project Inspector", text, "Issues") {
            showAndroidIssueList(report)
        }
    }

    private fun showAndroidIssueList(report: AndroidProjectReport) {
        if (report.issues.isEmpty()) { toast("No Android project issues detected"); return }
        val labels = report.issues.map { issue ->
            "${androidIssueBadge(issue.severity)} ${issue.title}\n${issue.detail.take(180)}${issue.path?.let { "\n$it" }.orEmpty()}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Android issues • ${report.issues.size}")
            .setItems(labels) { _, which ->
                val issue = report.issues[which]
                val path = issue.path
                if (path != null) openWorkspacePath(path, issue.line ?: 1)
                else showError(issue.title, issue.detail)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showManifestCenter(report: AndroidProjectReport) {
        val manifestPath = report.manifestPath ?: run { toast("No app manifest found"); return }
        val entry = findWorkspaceEntry(manifestPath) ?: run { toast("Manifest index is stale; reopen Android Tools"); return }
        projectIo.submit {
            val result = runCatching { repository.readText(entry.node.uri, 2L * 1024L * 1024L) }
            mainHandler.post {
                result.onSuccess { source -> showManifestActions(entry, source) }
                    .onFailure { showError("Manifest unavailable", it.message ?: "Could not read AndroidManifest.xml") }
            }
        }
    }

    private fun showManifestActions(entry: FileRepository.WorkspaceEntry, source: String) {
        var dialog: AlertDialog? = null
        val permissions = ManifestEditor.listPermissions(source)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 8))
        }
        box.addView(Ui.text(this, entry.relativePath, 12f, palette.textSecondary))
        box.addView(Ui.text(this,
            if (permissions.isEmpty()) "No uses-permission entries" else permissions.joinToString("\n"),
            11.5f, palette.textPrimary, gravity = Gravity.START
        ).apply { typeface = Typeface.MONOSPACE; setPadding(0, Ui.dp(this@MainActivity, 10), 0, Ui.dp(this@MainActivity, 10)) })
        fun button(label: String, action: () -> Unit) {
            box.addView(Button(this).apply { text = label; setOnClickListener { dialog?.dismiss(); action() } },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)))
        }
        button("Open manifest in editor") { openDocument(entry.node) }
        button("Add common permission") { showAddManifestPermission(entry, source) }
        button("Remove permission") { showRemoveManifestPermission(entry, source) }
        dialog = AlertDialog.Builder(this).setTitle("Manifest & Permissions").setView(box).setNegativeButton("Close", null).show()
    }

    private fun showAddManifestPermission(entry: FileRepository.WorkspaceEntry, source: String) {
        val common = arrayOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.FOREGROUND_SERVICE"
        ).filterNot { it in ManifestEditor.listPermissions(source) }.toTypedArray()
        if (common.isEmpty()) { toast("All common permissions are already present"); return }
        AlertDialog.Builder(this).setTitle("Add permission").setItems(common) { _, which ->
            val updated = runCatching { ManifestEditor.addPermission(source, common[which]) }
            updated.onSuccess { writeAndroidToolFile(entry, it, "Added ${common[which]}") }
                .onFailure { showError("Manifest edit failed", it.message ?: "Could not add permission") }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showRemoveManifestPermission(entry: FileRepository.WorkspaceEntry, source: String) {
        val permissions = ManifestEditor.listPermissions(source).toTypedArray()
        if (permissions.isEmpty()) { toast("No manifest permissions to remove"); return }
        AlertDialog.Builder(this).setTitle("Remove permission").setItems(permissions) { _, which ->
            AlertDialog.Builder(this).setTitle("Remove permission?")
                .setMessage(permissions[which])
                .setPositiveButton("Remove") { _, _ ->
                    writeAndroidToolFile(entry, ManifestEditor.removePermission(source, permissions[which]), "Removed ${permissions[which]}")
                }.setNegativeButton("Cancel", null).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun writeAndroidToolFile(entry: FileRepository.WorkspaceEntry, content: String, success: String) {
        val open = openTabs.firstOrNull { it.uri == entry.node.uri }
        if (open?.isDirty == true) {
            showError("Unsaved manifest", "Save or discard the open ${entry.node.name} changes before using the permission editor.")
            return
        }
        showProgress(true)
        io.submit {
            val result = runCatching { repository.writeText(entry.node.uri, content) }
            mainHandler.post {
                showProgress(false)
                result.onSuccess {
                    open?.let { tab ->
                        tab.content = content
                        tab.savedContentHash = content.hashCode()
                        if (activeTab === tab) editor.setDocument(tab.name, content)
                    }
                    renderTabs()
                    toast(success)
                    reloadWorkspaceTree()
                }.onFailure { showError("Write failed", it.message ?: "Could not update ${entry.node.name}") }
            }
        }
    }

    private fun showResourceCenter() {
        val files = workspaceIndex.filter { !it.node.isDirectory }.map { AndroidSourceFile(it.relativePath, sizeBytes = it.node.size) }
        val report = ResourceAnalyzer.analyze(files)
        val text = buildString {
            append("RESOURCE HEALTH\n")
            append("Files: ${report.totalFiles} • Size: ${formatBytes(report.totalBytes)}\n\n")
            append("BY TYPE\n")
            report.byType.forEach { (type, count) -> append("$type: $count\n") }
            append("\nQUALIFIERS\n")
            report.byQualifier.forEach { (qualifier, count) -> append("$qualifier: $count\n") }
            append("\nLARGEST\n")
            report.largest.forEach { append("${formatBytes(it.sizeBytes).padStart(9)}  ${it.path}\n") }
            append("\nISSUES (${report.issues.size})\n")
            report.issues.forEach { append("${androidIssueBadge(it.severity)} ${it.title}\n${it.detail}\n\n") }
        }
        showSelectableTextDialog("Android Resource Manager", text)
    }

    private fun showGradleBuildCenter(report: AndroidProjectReport) {
        val hasWrapper = workspaceIndex.any { it.relativePath == "gradlew" }
        val commands = GradleBuildAssistant.commands(report, hasWrapper)
        val labels = commands.map { "${it.title}\n${it.description}\n${it.command}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Gradle Build Center")
            .setItems(labels) { _, which ->
                val selected = commands[which]
                AlertDialog.Builder(this).setTitle(selected.title)
                    .setMessage("${selected.description}\n\n${selected.command}")
                    .setPositiveButton("Copy command") { _, _ -> copyText(selected.title, selected.command) }
                    .setNegativeButton("Close", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun chooseApkForInspection() {
        startActivityForResult(Intent("android.intent.action.OPEN_DOCUMENT").apply {
            addOpenableCategoryCompat()
            setMimeTypeCompat("application/vnd.android.package-archive")
            putExtra("android.intent.extra.MIME_TYPES", arrayOf("application/vnd.android.package-archive", "application/zip", "application/octet-stream"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, PICK_APK)
    }

    private fun inspectPickedApk(uri: Uri) {
        showProgress(true)
        statusText.text = "Inspecting APK…"
        projectIo.submit {
            val result = runCatching {
                val name = repository.metadata(uri).name
                val input = repository.openInput(uri) ?: throw IOException("Could not open APK")
                ApkInspector.inspect(name, input)
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { report ->
                    val text = buildString {
                        append("APK STRUCTURE\n")
                        append("File: ${report.fileName}\n")
                        append("Entries: ${report.entryCount}\n")
                        append("Expanded size: ${formatBytes(report.uncompressedBytes)}\n")
                        append("DEX files: ${report.dexFiles}\n")
                        append("Native ABIs: ${report.nativeAbis.ifEmpty { listOf("none") }.joinToString()}\n")
                        append("Native libraries: ${report.nativeLibraries}\n")
                        append("Resources: ${report.resourceEntries} • Assets: ${report.assetEntries}\n")
                        append("Manifest: ${if (report.hasManifest) "present" else "missing"}\n")
                        append("resources.arsc: ${if (report.hasResourcesTable) "present" else "missing"}\n")
                        append("JAR/v1 signature: ${if (report.hasV1Signature) "present" else "not detected"}\n\n")
                        append("LARGEST ENTRIES\n")
                        report.largestEntries.forEach { append("${formatBytes(it.second).padStart(9)}  ${it.first}\n") }
                        if (report.warnings.isNotEmpty()) {
                            append("\nWARNINGS\n")
                            report.warnings.forEach { append("⚠ $it\n") }
                        }
                    }
                    showSelectableTextDialog("APK Analyzer", text)
                }.onFailure { showError("APK inspection failed", it.message ?: "Could not inspect this APK") }
            }
        }
    }

    private fun chooseBuildLog() {
        startActivityForResult(Intent("android.intent.action.OPEN_DOCUMENT").apply {
            addOpenableCategoryCompat()
            setMimeTypeCompat("*/*")
            putExtra("android.intent.extra.MIME_TYPES", arrayOf("text/plain", "application/zip", "application/json", "text/html", "application/octet-stream"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, PICK_BUILD_LOG)
    }

    private fun analyzePickedBuildLog(uri: Uri) {
        showProgress(true)
        statusText.text = "Analyzing build logs…"
        projectIo.submit {
            val result = runCatching {
                val name = repository.metadata(uri).name
                val input = repository.openInput(uri) ?: throw IOException("Could not open build log")
                val text = input.use { BuildLogReader.read(name, it) }
                name to BuildLogAnalyzer.analyze(text)
            }
            mainHandler.post {
                showProgress(false)
                result.onSuccess { (name, report) ->
                    val text = buildString {
                        append("BUILD LOG ANALYSIS\n")
                        append("Source: $name\n")
                        append("Error-like lines: ${report.errorLines} • Warning-like lines: ${report.warningLines}\n")
                        report.probableRootCause?.let { append("Probable root cause: ${it.title}\n") }
                        append("\nFINDINGS\n")
                        report.findings.forEachIndexed { index, finding ->
                            append("${index + 1}. ${androidIssueBadge(finding.severity)} ${finding.title}\n")
                            append("Evidence: ${finding.evidence}\n")
                            append("Fix: ${finding.suggestion}\n\n")
                        }
                    }
                    showSelectableTextDialog("Build & Crash Analyzer", text)
                }.onFailure { showError("Log analysis failed", it.message ?: "Could not analyze this log") }
            }
        }
    }

    private fun findWorkspaceEntry(path: String): FileRepository.WorkspaceEntry? =
        workspaceIndex.firstOrNull { it.relativePath.equals(path, ignoreCase = true) }

    private fun openWorkspacePath(path: String, line: Int = 1) {
        val entry = findWorkspaceEntry(path) ?: run { toast("File not found in current index: $path"); return }
        openDocument(entry.node, activate = true) { tab ->
            activateTab(tab)
            if (line > 0) goToLine(line)
        }
    }

    private fun showSelectableTextDialog(title: String, content: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        val text = Ui.text(this, content, 11.5f, palette.textPrimary, gravity = Gravity.START).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 16), Ui.dp(this@MainActivity, 12))
        }
        val builder = AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply { addView(text) })
            .setNegativeButton("Close", null)
        if (actionLabel != null && action != null) builder.setPositiveButton(actionLabel) { _, _ -> action() }
        builder.show()
    }

    private fun copyText(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        toast("Copied $label")
    }

    private fun androidIssueBadge(severity: AndroidIssueSeverity): String = when (severity) {
        AndroidIssueSeverity.ERROR -> "✕"
        AndroidIssueSeverity.WARNING -> "⚠"
        AndroidIssueSeverity.INFO -> "•"
    }

    private fun beginGitMutation(workspaceUri: Uri): Boolean {
        if (gitMutationWorkspace != null) {
            toast("Another GitHub write operation is already running")
            return false
        }
        gitMutationWorkspace = workspaceUri
        return true
    }

    private fun endGitMutation(workspaceUri: Uri) {
        if (gitMutationWorkspace == workspaceUri) gitMutationWorkspace = null
    }

    private fun actionBadge(state: String): String = when (state.lowercase()) {
        "success" -> "✓"
        "failure", "cancelled", "timed_out" -> "✕"
        "in_progress", "queued", "waiting", "requested" -> "◷"
        else -> "•"
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    private fun openExternalUrl(url: String) {
        if (url.isBlank()) { toast("GitHub URL is unavailable"); return }
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { toast("No browser could open this link") }
    }

    private fun AlertDialog.dismissCompat() {
        runCatching { javaClass.getMethod("dismiss").invoke(this) }
    }

    private fun Intent.setMimeTypeCompat(value: String): Intent = apply {
        runCatching { javaClass.getMethod("setType", String::class.java).invoke(this, value) }
    }

    private fun Intent.addOpenableCategoryCompat(): Intent = apply {
        runCatching { javaClass.getMethod("addCategory", String::class.java).invoke(this, "android.intent.category.OPENABLE") }
    }

    private fun KeyEvent.shiftPressedCompat(): Boolean = runCatching {
        javaClass.getMethod("isShiftPressed").invoke(this) as? Boolean ?: false
    }.getOrDefault(false)

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun sizeParams(widthDp: Int, heightDp: Int) =
        LinearLayout.LayoutParams(Ui.dp(this, widthDp), Ui.dp(this, heightDp))

    private fun filePaneWidth(): Int {
        val screenDp = resources.configuration.screenWidthDp
        return Ui.dp(this, when {
            screenDp >= 900 -> 300
            screenDp >= 600 -> 260
            else -> 190
        })
    }

    private fun mimeForName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs", "cjs" -> "application/javascript"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "md", "markdown" -> "text/markdown"
        "kt", "kts", "java", "py", "txt", "gradle" -> "text/plain"
        else -> "text/plain"
    }
}
