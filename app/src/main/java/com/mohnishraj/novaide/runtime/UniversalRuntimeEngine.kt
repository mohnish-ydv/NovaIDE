package com.mohnishraj.novaide.runtime

import java.util.Locale

object UniversalRuntimeEngine {
    private val ignoredOutputSegments = setOf("node_modules", ".git", ".gradle", ".idea", ".next", ".nuxt", ".svelte-kit")

    fun detect(paths: Collection<String>, textFiles: Map<String, String> = emptyMap()): RuntimeProject {
        val normalized = paths.map { it.replace('\\', '/').trimStart('/') }.filter { it.isNotBlank() }.distinct()
        val lowerPaths = normalized.associateBy { it.lowercase(Locale.ROOT) }
        fun has(path: String) = lowerPaths.containsKey(path.lowercase(Locale.ROOT))
        fun anyName(name: String) = normalized.any { it.substringAfterLast('/').equals(name, ignoreCase = true) }
        fun content(path: String): String? = textFiles.entries.firstOrNull { it.key.equals(path, ignoreCase = true) }?.value
        val packageInfo = content("package.json")?.let { runCatching { PackageJsonReader.parse(it) }.getOrNull() }
        val dependencies = (packageInfo?.dependencies.orEmpty() + packageInfo?.devDependencies.orEmpty()).map { it.lowercase(Locale.ROOT) }.toSet()
        val scripts = packageInfo?.scripts.orEmpty()
        val packageManager = detectPackageManager(normalized)
        val evidence = mutableListOf<String>()

        val kind = when {
            has("angular.json") || "@angular/core" in dependencies -> RuntimeKind.ANGULAR.also { evidence += "Angular workspace marker" }
            has("next.config.js") || has("next.config.mjs") || has("next.config.ts") || "next" in dependencies -> RuntimeKind.NEXT_JS.also { evidence += "Next.js configuration/dependency" }
            has("nuxt.config.ts") || has("nuxt.config.js") || "nuxt" in dependencies -> RuntimeKind.NUXT.also { evidence += "Nuxt configuration/dependency" }
            has("astro.config.mjs") || has("astro.config.ts") || "astro" in dependencies -> RuntimeKind.ASTRO.also { evidence += "Astro configuration/dependency" }
            has("svelte.config.js") || has("svelte.config.ts") || "svelte" in dependencies || "@sveltejs/kit" in dependencies -> RuntimeKind.SVELTE.also { evidence += "Svelte configuration/dependency" }
            "vue" in dependencies || has("vue.config.js") -> RuntimeKind.VUE.also { evidence += "Vue dependency/configuration" }
            has("vite.config.js") || has("vite.config.ts") || has("vite.config.mjs") || "vite" in dependencies -> RuntimeKind.VITE.also { evidence += "Vite configuration/dependency" }
            "react" in dependencies || "react-scripts" in dependencies -> RuntimeKind.REACT.also { evidence += "React dependency" }
            has("package.json") -> RuntimeKind.NODE.also { evidence += "package.json" }
            has("pyproject.toml") || has("requirements.txt") || has("setup.py") || normalized.any { it.endsWith(".py", true) } -> RuntimeKind.PYTHON.also { evidence += "Python project marker" }
            has("composer.json") || normalized.any { it.endsWith(".php", true) } -> RuntimeKind.PHP.also { evidence += "PHP source/Composer marker" }
            has("config.toml") && normalized.any { it.startsWith("content/", true) } || has("hugo.toml") -> RuntimeKind.HUGO.also { evidence += "Hugo configuration" }
            has("_config.yml") && (has("gemfile") || normalized.any { it.startsWith("_posts/", true) }) -> RuntimeKind.JEKYLL.also { evidence += "Jekyll configuration" }
            normalized.any { it.endsWith(".mmd", true) || it.endsWith(".mermaid", true) } -> RuntimeKind.MERMAID.also { evidence += "Mermaid document" }
            normalized.any { it.endsWith(".md", true) } && normalized.none { it.endsWith(".html", true) } -> RuntimeKind.MARKDOWN.also { evidence += "Markdown document" }
            normalized.any { it.endsWith(".html", true) } -> RuntimeKind.STATIC_WEB.also { evidence += "Browser-ready HTML" }
            else -> RuntimeKind.GENERIC.also { evidence += "No supported runtime marker" }
        }

        val outputs = outputCandidates(kind, packageInfo?.name, normalized)
        val detectedOutput = outputs.firstOrNull { candidate ->
            has(candidate) || normalized.any {
                it.equals(candidate, true) ||
                    it.startsWith(candidate.substringBeforeLast('/', candidate) + "/", true) && it.endsWith("/index.html", true)
            }
        } ?: findGeneratedIndex(normalized, generatedOnly = kind in browserBuildKinds)

        val commands = buildCommands(kind, scripts, packageManager, normalized)
        val confidence = when {
            evidence.any { it.contains("configuration", true) } -> 96
            packageInfo != null -> 91
            kind in setOf(RuntimeKind.PYTHON, RuntimeKind.PHP, RuntimeKind.HUGO, RuntimeKind.JEKYLL) -> 88
            kind in setOf(RuntimeKind.STATIC_WEB, RuntimeKind.MARKDOWN, RuntimeKind.MERMAID) -> 85
            else -> 50
        }
        val warning = when {
            kind == RuntimeKind.GENERIC -> "No executable runtime was detected. You can still edit files or create a custom safe task."
            kind == RuntimeKind.NEXT_JS && detectedOutput == null -> "Next.js preview requires a static export in out/ or a running local development server."
            kind in browserBuildKinds && detectedOutput == null -> "Source project detected, but no generated browser output exists yet. Run Install and Build through the Termux bridge."
            else -> null
        }
        return RuntimeProject(kind, confidence, packageManager, scripts, commands, outputs, detectedOutput, evidence.distinct(), warning)
    }

    private val browserBuildKinds = setOf(
        RuntimeKind.VITE, RuntimeKind.REACT, RuntimeKind.NEXT_JS, RuntimeKind.VUE,
        RuntimeKind.SVELTE, RuntimeKind.ASTRO, RuntimeKind.ANGULAR, RuntimeKind.NUXT
    )

    private fun detectPackageManager(paths: Collection<String>): String? = when {
        paths.any { it.equals("bun.lock", true) || it.equals("bun.lockb", true) } -> "bun"
        paths.any { it.equals("pnpm-lock.yaml", true) } -> "pnpm"
        paths.any { it.equals("yarn.lock", true) } -> "yarn"
        paths.any { it.equals("package-lock.json", true) || it.equals("npm-shrinkwrap.json", true) } -> "npm"
        paths.any { it.equals("package.json", true) } -> "npm"
        else -> null
    }

    private fun outputCandidates(kind: RuntimeKind, packageName: String?, paths: Collection<String>): List<String> {
        val angularName = packageName?.replace(Regex("[^A-Za-z0-9_-]"), "-")?.takeIf { it.isNotBlank() }
        val base = when (kind) {
            RuntimeKind.REACT -> listOf("build/index.html", "dist/index.html")
            RuntimeKind.NEXT_JS -> listOf("out/index.html")
            RuntimeKind.NUXT -> listOf(".output/public/index.html", "dist/index.html")
            RuntimeKind.SVELTE -> listOf("build/index.html", "dist/index.html")
            RuntimeKind.ANGULAR -> buildList {
                if (angularName != null) {
                    add("dist/$angularName/browser/index.html")
                    add("dist/$angularName/index.html")
                }
                add("dist/browser/index.html")
                add("dist/index.html")
            }
            RuntimeKind.VITE, RuntimeKind.VUE, RuntimeKind.ASTRO -> listOf("dist/index.html")
            RuntimeKind.HUGO, RuntimeKind.JEKYLL -> listOf("public/index.html", "_site/index.html")
            else -> listOf("index.html")
        }
        return (base + paths.filter { path ->
            val lower = path.lowercase(Locale.ROOT)
            lower.endsWith("/index.html") && lower.split('/').none { it in ignoredOutputSegments } &&
                (lower.startsWith("dist/") || lower.startsWith("build/") || lower.startsWith("out/") || lower.startsWith("public/") || lower.startsWith("_site/") || lower.startsWith(".output/public/"))
        }).distinct()
    }

    private fun findGeneratedIndex(paths: Collection<String>, generatedOnly: Boolean): String? = paths
        .filter { it.endsWith("index.html", true) }
        .filter { path -> path.split('/').none { it.lowercase(Locale.ROOT) in ignoredOutputSegments } }
        .filter { path ->
            if (!generatedOnly) true else {
                val lower = path.lowercase(Locale.ROOT)
                lower.startsWith("dist/") || lower.startsWith("build/") || lower.startsWith("out/") ||
                    lower.startsWith(".output/public/") || lower.startsWith("public/") || lower.startsWith("_site/")
            }
        }
        .sortedWith(compareBy<String> {
            val lower = it.lowercase(Locale.ROOT)
            when {
                lower == "index.html" -> 0
                lower.startsWith("dist/") -> 1
                lower.startsWith("build/") -> 2
                lower.startsWith("out/") -> 3
                lower.startsWith(".output/public/") -> 4
                lower.startsWith("public/") -> 5
                lower.startsWith("_site/") -> 6
                else -> 20
            }
        }.thenBy { it.count { char -> char == '/' } })
        .firstOrNull()

    private fun buildCommands(
        kind: RuntimeKind,
        scripts: Map<String, String>,
        packageManager: String?,
        paths: Collection<String>
    ): List<RuntimeCommand> {
        if (packageManager != null) {
            val commands = mutableListOf<RuntimeCommand>()
            commands += installCommand(packageManager, paths)
            chooseScript(scripts, listOf("build", "export", "generate"))?.let { commands += scriptCommand(packageManager, RuntimeAction.BUILD, "Build project", it.first, it.second) }
            chooseScript(scripts, listOf("dev", "start", "serve"))?.let { (name, raw) ->
                val port = defaultPort(kind, raw)
                commands += scriptCommand(packageManager, RuntimeAction.DEVELOP, "Start development server", name, raw, opensServer = true, port = port)
            }
            chooseScript(scripts, listOf("preview"))?.let { (name, raw) ->
                commands += scriptCommand(packageManager, RuntimeAction.RUN, "Preview production build", name, raw, opensServer = true, port = defaultPort(kind, raw))
            }
            chooseScript(scripts, listOf("test", "check", "lint"))?.let { commands += scriptCommand(packageManager, RuntimeAction.TEST, "Run project checks", it.first, it.second) }
            if (commands.none { it.action == RuntimeAction.RUN } && kind == RuntimeKind.NODE) {
                chooseScript(scripts, listOf("start", "serve"))?.let { commands += scriptCommand(packageManager, RuntimeAction.RUN, "Run project", it.first, it.second, true, defaultPort(kind, it.second)) }
            }
            return commands.distinctBy { it.action to it.display() }
        }
        return when (kind) {
            RuntimeKind.PYTHON -> {
                val entry = listOf("main.py", "app.py", "manage.py").firstOrNull { candidate -> paths.any { it.equals(candidate, true) } }
                    ?: paths.firstOrNull { it.endsWith(".py", true) }
                buildList {
                    if (paths.any { it.equals("requirements.txt", true) }) add(RuntimeCommand(RuntimeAction.INSTALL, "Install Python dependencies", "python", listOf("-m", "pip", "install", "-r", "requirements.txt"), "Install requirements.txt dependencies."))
                    if (entry != null) add(RuntimeCommand(RuntimeAction.RUN, "Run Python", "python", listOf(entry), "Run $entry in Termux."))
                }
            }
            RuntimeKind.PHP -> listOf(RuntimeCommand(RuntimeAction.DEVELOP, "Start PHP server", "php", listOf("-S", "127.0.0.1:8000", "-t", "."), "Serve this workspace with PHP's development server.", true, 8000))
            RuntimeKind.HUGO -> listOf(
                RuntimeCommand(RuntimeAction.BUILD, "Build Hugo site", "hugo", emptyList(), "Generate the static site into public/."),
                RuntimeCommand(RuntimeAction.DEVELOP, "Start Hugo server", "hugo", listOf("server", "--bind", "127.0.0.1", "--port", "1313"), "Start Hugo's local development server.", true, 1313)
            )
            RuntimeKind.JEKYLL -> listOf(
                RuntimeCommand(RuntimeAction.INSTALL, "Install Jekyll gems", "bundle", listOf("install"), "Install Gemfile dependencies."),
                RuntimeCommand(RuntimeAction.BUILD, "Build Jekyll site", "bundle", listOf("exec", "jekyll", "build"), "Generate the static site into _site/."),
                RuntimeCommand(RuntimeAction.DEVELOP, "Start Jekyll server", "bundle", listOf("exec", "jekyll", "serve", "--host", "127.0.0.1", "--port", "4000"), "Start Jekyll's local server.", true, 4000)
            )
            else -> emptyList()
        }
    }

    private fun installCommand(manager: String, paths: Collection<String>): RuntimeCommand = when (manager) {
        "pnpm" -> RuntimeCommand(RuntimeAction.INSTALL, "Install dependencies", "pnpm", listOf("install", "--frozen-lockfile"), "Install the exact pnpm lockfile dependencies.")
        "yarn" -> RuntimeCommand(RuntimeAction.INSTALL, "Install dependencies", "yarn", listOf("install", "--frozen-lockfile"), "Install the exact Yarn lockfile dependencies.")
        "bun" -> RuntimeCommand(RuntimeAction.INSTALL, "Install dependencies", "bun", listOf("install", "--frozen-lockfile"), "Install the exact Bun lockfile dependencies.")
        else -> if (paths.any { it.equals("package-lock.json", true) || it.equals("npm-shrinkwrap.json", true) })
            RuntimeCommand(RuntimeAction.INSTALL, "Install dependencies", "npm", listOf("ci"), "Cleanly install package-lock dependencies.")
        else RuntimeCommand(RuntimeAction.INSTALL, "Install dependencies", "npm", listOf("install"), "Install package.json dependencies.")
    }

    private fun scriptCommand(
        manager: String,
        action: RuntimeAction,
        label: String,
        script: String,
        raw: String,
        opensServer: Boolean = false,
        port: Int? = null
    ): RuntimeCommand {
        val args = when (manager) {
            "yarn" -> listOf(script)
            else -> listOf("run", script)
        }
        return RuntimeCommand(action, label, manager, args, "package.json script '$script': $raw", opensServer, port)
    }

    private fun chooseScript(scripts: Map<String, String>, names: List<String>): Pair<String, String>? =
        names.firstNotNullOfOrNull { name -> scripts.entries.firstOrNull { it.key.equals(name, true) }?.let { it.key to it.value } }

    private fun defaultPort(kind: RuntimeKind, raw: String): Int = Regex("(?:--port(?:=|\\s+)|-p\\s+)(\\d{2,5})")
        .find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1024..65535 }
        ?: when (kind) {
            RuntimeKind.VITE, RuntimeKind.VUE, RuntimeKind.SVELTE -> 5173
            RuntimeKind.NEXT_JS, RuntimeKind.NUXT, RuntimeKind.REACT, RuntimeKind.NODE -> 3000
            RuntimeKind.ASTRO -> 4321
            RuntimeKind.ANGULAR -> 4200
            else -> 3000
        }
}
