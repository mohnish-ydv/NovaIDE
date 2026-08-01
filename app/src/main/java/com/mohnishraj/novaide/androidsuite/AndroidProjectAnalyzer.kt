package com.mohnishraj.novaide.androidsuite

import java.util.Locale

object AndroidProjectAnalyzer {
    private val moduleInclude = Regex("""include\s*\(?\s*([^\n]+)""")
    private val dependencyLine = Regex("""(?m)(?:^|[;{])\s*(implementation|api|compileOnly|runtimeOnly|kapt|ksp|annotationProcessor|testImplementation|androidTestImplementation|debugImplementation|releaseImplementation)\s*\(?\s*[\"']([^\"']+)[\"']""")
    private val buildTypeBlock = Regex("""(?m)^\s*(debug|release|benchmark|staging|qa|demo|production)\s*\{""")

    fun analyze(files: List<AndroidSourceFile>, projectName: String = "Android project"): AndroidProjectReport {
        val normalized = files.associateBy { it.path.replace('\\', '/').trimStart('/') }
        val settings = normalized.entries.firstOrNull { it.key == "settings.gradle.kts" || it.key == "settings.gradle" }?.value?.content.orEmpty()
        val buildFiles = normalized.values.filter {
            it.path.endsWith("build.gradle.kts") || it.path.endsWith("build.gradle")
        }
        val manifests = normalized.values.filter { it.path.endsWith("src/main/AndroidManifest.xml") || it.path == "AndroidManifest.xml" }
        val isAndroid = buildFiles.any { looksAndroidGradle(it.content.orEmpty()) } || manifests.isNotEmpty()
        if (!isAndroid) {
            return AndroidProjectReport(false, projectName, emptyList(), emptyList(), emptyList(), null, 0, 0, 0, 0, 0,
                listOf(AndroidProjectIssue(AndroidIssueSeverity.INFO, "Not an Android project", "No Android Gradle plugin or app manifest was detected.")))
        }

        val moduleNames = parseIncludedModules(settings).toMutableSet()
        buildFiles.forEach { file ->
            val parent = file.path.substringBeforeLast('/', "")
            if (parent.isNotEmpty() && looksAndroidGradle(file.content.orEmpty())) moduleNames += parent
        }
        if (moduleNames.isEmpty() && buildFiles.any { looksAndroidGradle(it.content.orEmpty()) }) moduleNames += "."

        val issues = mutableListOf<AndroidProjectIssue>()
        val modules = moduleNames.mapNotNull { module ->
            val prefix = if (module == ".") "" else "${module.trim(':').replace(':', '/')}/"
            val build = buildFiles.firstOrNull { it.path == "${prefix}build.gradle.kts" || it.path == "${prefix}build.gradle" }
                ?: return@mapNotNull null
            parseModule(module.trim(':').ifBlank { "root" }, build, issues)
        }.sortedBy { it.name }

        val primaryManifest = manifests.firstOrNull { pathBelongsToApplicationModule(it.path, modules) } ?: manifests.firstOrNull()
        val manifestAnalysis = primaryManifest?.let { ManifestInspector.inspect(it.content.orEmpty(), it.path) }
        if (primaryManifest == null) {
            issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Missing AndroidManifest.xml", "No src/main/AndroidManifest.xml was found in the detected modules.")
        } else {
            issues += manifestAnalysis?.issues.orEmpty()
        }

        buildFiles.forEach { file -> analyzeBuildFile(file, issues) }
        modules.forEach { module ->
            if (module.compileSdk != null && module.targetSdk != null && module.targetSdk > module.compileSdk) {
                issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "targetSdk exceeds compileSdk", "${module.name}: targetSdk ${module.targetSdk} cannot be higher than compileSdk ${module.compileSdk}.", module.buildFile)
            }
            if (module.minSdk != null && module.targetSdk != null && module.minSdk > module.targetSdk) {
                issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "minSdk exceeds targetSdk", "${module.name}: minSdk ${module.minSdk} is higher than targetSdk ${module.targetSdk}.", module.buildFile)
            }
            if (module.isApplication && module.applicationId.isNullOrBlank()) {
                issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Application ID not resolved", "NovaIDE could not find a literal applicationId in ${module.buildFile}.", module.buildFile)
            }
        }

        val sourceFiles = files.count { path ->
            val p = path.path.lowercase(Locale.US)
            (p.contains("/src/") || p.startsWith("src/")) && (p.endsWith(".kt") || p.endsWith(".java")) && !p.contains("/test/") && !p.contains("/androidtest/")
        }
        val testFiles = files.count {
            val p = it.path.lowercase(Locale.US)
            (p.contains("/test/") || p.contains("/androidtest/")) && (p.endsWith(".kt") || p.endsWith(".java"))
        }
        val resources = files.count { it.path.replace('\\', '/').contains("/src/main/res/") }
        val assets = files.count { it.path.replace('\\', '/').contains("/src/main/assets/") }
        val native = files.count { it.path.replace('\\', '/').contains("/src/main/jnilibs/") && it.path.endsWith(".so", true) }

        return AndroidProjectReport(
            isAndroidProject = true,
            projectName = projectName,
            modules = modules,
            permissions = manifestAnalysis?.permissions.orEmpty(),
            components = manifestAnalysis?.components.orEmpty(),
            manifestPath = primaryManifest?.path,
            sourceFiles = sourceFiles,
            testFiles = testFiles,
            resourceFiles = resources,
            assetFiles = assets,
            nativeLibraries = native,
            issues = issues.distinctBy { listOf(it.severity, it.title, it.detail, it.path, it.line) }
                .sortedWith(compareBy<AndroidProjectIssue> { it.severity.ordinal }.thenBy { it.title })
        )
    }

    private fun parseIncludedModules(settings: String): Set<String> {
        val modules = linkedSetOf<String>()
        moduleInclude.findAll(settings).forEach { match ->
            Regex("""[\"'](:[^\"']+)[\"']""").findAll(match.groupValues[1]).forEach {
                modules += it.groupValues[1].trim(':').replace(':', '/')
            }
        }
        return modules
    }

    private fun parseModule(name: String, file: AndroidSourceFile, issues: MutableList<AndroidProjectIssue>): AndroidModuleReport {
        val source = stripComments(file.content.orEmpty())
        val plugins = source.lowercase(Locale.US)
        val isApplication = plugins.contains("com.android.application")
        val namespace = literal(source, "namespace")
        val applicationId = literal(source, "applicationId")
        val compileSdk = integer(source, "compileSdk") ?: integer(source, "compileSdkVersion")
        val minSdk = integer(source, "minSdk") ?: integer(source, "minSdkVersion")
        val targetSdk = integer(source, "targetSdk") ?: integer(source, "targetSdkVersion")
        val versionCode = integer(source, "versionCode")
        val versionName = literal(source, "versionName")
        val deps = dependencyLine.findAll(source).map {
            AndroidDependency(it.groupValues[1], it.groupValues[2], file.path)
        }.toList()
        val buildTypes = buildTypeBlock.findAll(source.substringAfter("buildTypes", "")).map { it.groupValues[1] }.distinct().toList()
        if (!plugins.contains("com.android.application") && !plugins.contains("com.android.library") && !plugins.contains("com.android.dynamic-feature")) {
            issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Android plugin not resolved", "${file.path} was treated as a module but no literal Android plugin ID was found.", file.path)
        }
        return AndroidModuleReport(name, file.path, namespace, applicationId, compileSdk, minSdk, targetSdk, versionCode, versionName, isApplication, buildTypes, deps)
    }

    private fun analyzeBuildFile(file: AndroidSourceFile, issues: MutableList<AndroidProjectIssue>) {
        val source = file.content.orEmpty()
        if (Regex("""(?i)\bjcenter\s*\(""").containsMatchIn(source)) {
            issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Deprecated jcenter repository", "Replace jcenter() with maintained repositories such as google() and mavenCentral().", file.path)
        }
        if (Regex("""maven\s*\{[^}]*url\s*=?.*http://""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(source)) {
            issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Insecure Maven repository", "An HTTP Maven repository was found. Prefer HTTPS to prevent dependency tampering.", file.path)
        }
        dependencyLine.findAll(source).forEach { match ->
            val notation = match.groupValues[2]
            if (notation.contains('+') || notation.contains("latest.release", true) || notation.contains("latest.integration", true)) {
                issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Dynamic dependency version", "$notation is not reproducible. Pin an exact version.", file.path, lineOf(source, match.range.first))
            }
        }
        if (Regex("""(?i)debuggable\s*[= ]\s*true""").containsMatchIn(source.substringAfter("release", ""))) {
            issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Release build is debuggable", "A release build block appears to enable debuggable=true.", file.path)
        }
        if (Regex("""(?i)(storePassword|keyPassword)\s*[= ]\s*[\"'][^$][^\"']+[\"']""").containsMatchIn(source)) {
            issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Signing secret in source", "A signing password appears to be hardcoded in ${file.path}. Move secrets to protected environment variables or an untracked properties file.", file.path)
        }
    }

    private fun looksAndroidGradle(source: String): Boolean =
        source.contains("com.android.application") || source.contains("com.android.library") || source.contains("com.android.dynamic-feature") || source.contains("android {")

    private fun pathBelongsToApplicationModule(path: String, modules: List<AndroidModuleReport>): Boolean =
        modules.filter { it.isApplication }.any { module ->
            val prefix = module.buildFile.substringBeforeLast("build.gradle", "")
            path.startsWith(prefix)
        }

    private fun literal(source: String, key: String): String? {
        val patterns = listOf(
            Regex("""(?m)\b${Regex.escape(key)}\s*=\s*[\"']([^\"']+)[\"']"""),
            Regex("""(?m)\b${Regex.escape(key)}\s+[\"']([^\"']+)[\"']""")
        )
        return patterns.firstNotNullOfOrNull { it.find(source)?.groupValues?.getOrNull(1) }
    }

    private fun integer(source: String, key: String): Int? {
        val patterns = listOf(
            Regex("""(?m)\b${Regex.escape(key)}\s*=\s*(\d+)"""),
            Regex("""(?m)\b${Regex.escape(key)}\s+(\d+)""")
        )
        return patterns.firstNotNullOfOrNull { it.find(source)?.groupValues?.getOrNull(1)?.toIntOrNull() }
    }

    private fun stripComments(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""(?m)//.*$"""), "")

    private fun lineOf(source: String, offset: Int): Int = source.take(offset.coerceIn(0, source.length)).count { it == '\n' } + 1
}
