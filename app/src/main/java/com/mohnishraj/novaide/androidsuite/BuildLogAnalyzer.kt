package com.mohnishraj.novaide.androidsuite

object BuildLogAnalyzer {
    private data class Rule(val title: String, val pattern: Regex, val severity: AndroidIssueSeverity, val suggestion: String)

    private val rules = listOf(
        Rule("Android app crash", Regex("(?i)(fatal exception|androidruntime: fatal|process: .* pid:)") , AndroidIssueSeverity.ERROR, "Start at the first exception after FATAL EXCEPTION, then follow the first stack frame from your app package to the responsible source line."),
        Rule("Application Not Responding", Regex("(?i)(anr in |input dispatching timed out|executing service .* timed out|broadcast of intent .* timed out)"), AndroidIssueSeverity.ERROR, "Move blocking I/O, network, parsing, or long loops off the main thread and inspect the main-thread stack in the ANR trace."),
        Rule("Android SecurityException", Regex("""(?i)(java\.lang\.securityexception|permission denial|requires .* permission)"""), AndroidIssueSeverity.ERROR, "Check the exact denied API, manifest declaration, runtime permission state, target-SDK restrictions, and whether the API is privileged/system-only."),
        Rule("Missing class or dependency", Regex("(?i)(classnotfoundexception|noclassdeffounderror|didn.t find class|unable to instantiate activity)"), AndroidIssueSeverity.ERROR, "Verify the class/package name, dependency inclusion, R8 keep rules, manifest component name, and variant-specific source set."),
        Rule("Null reference crash", Regex("""(?i)(nullpointerexception|lateinit property .* has not been initialized|kotlin\.uninitializedpropertyaccessexception)"""), AndroidIssueSeverity.ERROR, "Inspect the first app-owned stack frame and enforce lifecycle/nullability checks before accessing the value."),
        Rule("Resource runtime failure", Regex("""(?i)(resources[$]notfoundexception|android\.view\.inflateexception|binary xml file line #)"""), AndroidIssueSeverity.ERROR, "Inspect the referenced layout/resource line, theme attributes, constructor inflation path, and resource qualifier compatibility."),
        Rule("Kotlin/Java compilation error", Regex("(?i)(e: .*\\.kt:|error: cannot find symbol|unresolved reference|type mismatch|compilation error)"), AndroidIssueSeverity.ERROR, "Open the first compiler error, fix it, then rebuild; later errors are often cascading."),
        Rule("Android resource linking failed", Regex("(?i)(android resource linking failed|aapt2? .* error|resource .* not found|duplicate resources)"), AndroidIssueSeverity.ERROR, "Inspect the first AAPT error and the referenced XML/resource name. Validate filenames and resource references."),
        Rule("Manifest merge failure", Regex("(?i)(manifest merger failed|uses-sdk:minSdkVersion|android:exported needs to be explicitly specified)"), AndroidIssueSeverity.ERROR, "Open the merged-manifest report, resolve the conflicting attribute, and explicitly set android:exported for intent-filter components."),
        Rule("Dependency resolution failure", Regex("(?i)(could not resolve all files|could not find .*:.*:|failed to transform|could not resolve project)"), AndroidIssueSeverity.ERROR, "Verify repository declarations, dependency coordinates, network access, and pinned versions."),
        Rule("Gradle/Java compatibility problem", Regex("(?i)(unsupported class file major version|invalid source release|requires java|jvm target compatibility|this version of the android gradle plugin requires java)"), AndroidIssueSeverity.ERROR, "Align the Java version, Android Gradle Plugin, Gradle wrapper, compileOptions, and Kotlin jvmTarget."),
        Rule("Android SDK component missing", Regex("(?i)(failed to find target with hash string|sdk location not found|platforms;android-|build-tools;|license for package .* not accepted)"), AndroidIssueSeverity.ERROR, "Install the requested SDK platform/build-tools and accept licenses. Ensure ANDROID_HOME or local.properties is configured in the build environment."),
        Rule("Duplicate class", Regex("(?i)(duplicate class .* found in modules|program type already present)"), AndroidIssueSeverity.ERROR, "Use the dependency tree to find duplicate libraries, then exclude one transitive dependency or align versions."),
        Rule("Out of memory", Regex("(?i)(java heap space|outofmemoryerror|gc overhead limit exceeded|daemon disappeared unexpectedly)"), AndroidIssueSeverity.ERROR, "Reduce parallelism/asset size or raise org.gradle.jvmargs within device/runner memory limits."),
        Rule("Android Lint failure", Regex("(?i)(lint found .* errors|lint vital|abortOnError|\\[WrongConstant\\]|\\[NewApi\\])"), AndroidIssueSeverity.ERROR, "Open the generated lint HTML/XML report and fix the first unsuppressed issue instead of disabling lint."),
        Rule("Signing configuration failure", Regex("(?i)(keystore .* not found|failed to read key|keystore was tampered|signingconfig|apksigner)"), AndroidIssueSeverity.ERROR, "Verify keystore path, alias and secrets. Keep secrets outside source control and pass them through protected CI variables."),
        Rule("GitHub Actions permission failure", Regex("(?i)(resource not accessible by integration|permission denied to github-actions|http 403|bad credentials)"), AndroidIssueSeverity.ERROR, "Check repository Actions permissions and token scopes. Use least-privilege Contents/Actions permissions."),
        Rule("Deprecated build behavior", Regex("(?i)(deprecated gradle features|has been deprecated|will be removed in gradle)"), AndroidIssueSeverity.WARNING, "Run with --warning-mode all, locate the responsible plugin/script, and migrate before the next Gradle upgrade."),
        Rule("Build cancelled or timed out", Regex("(?i)(process completed with exit code 143|cancelled|timed out|timeout-minutes)"), AndroidIssueSeverity.WARNING, "Check the final active task, network downloads, memory pressure and workflow timeout settings.")
    )

    fun analyze(log: String): BuildLogReport {
        val lines = log.lineSequence().toList()
        val findings = mutableListOf<BuildLogFinding>()
        rules.forEach { rule ->
            val evidence = lines.firstOrNull { rule.pattern.containsMatchIn(it) }
                ?: rule.pattern.find(log)?.value
            if (!evidence.isNullOrBlank()) findings += BuildLogFinding(rule.severity, rule.title, evidence.trim().take(500), rule.suggestion)
        }
        val explicitErrors = lines.count { line -> Regex("(?i)(^|\\s)(error|exception|failure|failed)(:|\\s)").containsMatchIn(line) }
        val warnings = lines.count { line -> Regex("(?i)(^|\\s)warning(:|\\s)").containsMatchIn(line) }
        if (findings.isEmpty() && log.isNotBlank()) {
            findings += BuildLogFinding(AndroidIssueSeverity.INFO, "No known signature detected", lines.takeLast(8).joinToString("\n").take(500), "Search upward from the final failure line for the first task or exception that failed.")
        }
        return BuildLogReport(findings, explicitErrors, warnings, findings.firstOrNull { it.severity == AndroidIssueSeverity.ERROR } ?: findings.firstOrNull())
    }
}
