package com.mohnishraj.novaide.project

enum class ProjectKind(val label: String, val badge: String) {
    ANDROID("Android / Gradle", "AND"),
    FLUTTER("Flutter", "FLT"),
    REACT_NATIVE("React Native", "RN"),
    NEXT_JS("Next.js", "NXT"),
    REACT("React", "RCT"),
    VUE("Vue", "VUE"),
    SVELTE("Svelte", "SVT"),
    PHASER("Phaser", "PHR"),
    NODE("Node.js", "NOD"),
    GODOT("Godot", "GDT"),
    PYTHON("Python", "PY"),
    RUST("Rust", "RS"),
    GO("Go", "GO"),
    STATIC_WEB("Static Web", "WEB"),
    GRADLE("Gradle", "GRD"),
    GENERIC("General Project", "GEN")
}

data class ProjectDetection(
    val kind: ProjectKind,
    val confidence: Int,
    val evidence: List<String>
)

object ProjectDetector {
    fun detect(paths: Collection<String>, textHints: Map<String, String> = emptyMap()): ProjectDetection {
        val normalized = paths.map { it.replace('\\', '/').trimStart('/').lowercase() }.toSet()
        val names = normalized.map { it.substringAfterLast('/') }.toSet()
        val hints = textHints.mapKeys { it.key.replace('\\', '/').trimStart('/').lowercase() }

        fun hasPath(path: String): Boolean = path.lowercase() in normalized
        fun hasName(name: String): Boolean = name.lowercase() in names
        fun hintContains(path: String, token: String): Boolean =
            hints[path.lowercase()]?.contains(token, ignoreCase = true) == true

        val scored = mutableListOf<Triple<ProjectKind, Int, MutableList<String>>>()
        fun score(kind: ProjectKind, points: Int, reason: String) {
            val item = scored.firstOrNull { it.first == kind }
            if (item == null) scored += Triple(kind, points, mutableListOf(reason))
            else {
                val index = scored.indexOf(item)
                scored[index] = Triple(item.first, item.second + points, (item.third + reason).toMutableList())
            }
        }

        if (hasName("settings.gradle") || hasName("settings.gradle.kts")) score(ProjectKind.GRADLE, 20, "Gradle settings")
        if (hasName("build.gradle") || hasName("build.gradle.kts")) score(ProjectKind.GRADLE, 15, "Gradle build")
        if (hasPath("app/src/main/androidmanifest.xml") || hasName("androidmanifest.xml")) score(ProjectKind.ANDROID, 70, "Android manifest")
        if (hasPath("app/build.gradle") || hasPath("app/build.gradle.kts")) score(ProjectKind.ANDROID, 25, "Android app module")
        if (hasName("gradlew")) score(ProjectKind.ANDROID, 5, "Gradle wrapper")

        if (hasName("pubspec.yaml")) score(ProjectKind.FLUTTER, 85, "Flutter pubspec")
        if (normalized.any { it.startsWith("lib/") && it.endsWith(".dart") }) score(ProjectKind.FLUTTER, 15, "Dart sources")

        if (hasName("project.godot")) score(ProjectKind.GODOT, 95, "Godot project file")
        if (normalized.any { it.endsWith(".gd") }) score(ProjectKind.GODOT, 10, "GDScript sources")

        if (hasName("cargo.toml")) score(ProjectKind.RUST, 95, "Cargo manifest")
        if (hasName("go.mod")) score(ProjectKind.GO, 95, "Go module")
        if (hasName("pyproject.toml") || hasName("requirements.txt") || hasName("setup.py")) score(ProjectKind.PYTHON, 80, "Python project marker")
        if (normalized.any { it.endsWith(".py") }) score(ProjectKind.PYTHON, 10, "Python sources")

        if (hasName("package.json")) score(ProjectKind.NODE, 45, "package.json")
        if (hasName("next.config.js") || hasName("next.config.mjs") || hasName("next.config.ts")) score(ProjectKind.NEXT_JS, 80, "Next.js config")
        if (hintContains("package.json", "\"next\"")) score(ProjectKind.NEXT_JS, 45, "Next.js dependency")
        if (hintContains("package.json", "react-native")) score(ProjectKind.REACT_NATIVE, 90, "React Native dependency")
        if (hintContains("package.json", "\"react\"")) score(ProjectKind.REACT, 45, "React dependency")
        if (hintContains("package.json", "\"vue\"")) score(ProjectKind.VUE, 70, "Vue dependency")
        if (hintContains("package.json", "\"svelte\"")) score(ProjectKind.SVELTE, 70, "Svelte dependency")
        if (hintContains("package.json", "phaser")) score(ProjectKind.PHASER, 90, "Phaser dependency")
        if (hasName("vite.config.js") || hasName("vite.config.ts")) score(ProjectKind.REACT, 10, "Vite config")

        if (hasName("index.html")) score(ProjectKind.STATIC_WEB, 55, "index.html")
        if (normalized.any { it.endsWith(".css") }) score(ProjectKind.STATIC_WEB, 10, "Stylesheets")
        if (normalized.any { it.endsWith(".js") }) score(ProjectKind.STATIC_WEB, 10, "JavaScript sources")

        val best = scored.maxWithOrNull(compareBy<Triple<ProjectKind, Int, MutableList<String>>> { it.second }
            .thenBy { priority(it.first) })
        if (best == null) return ProjectDetection(ProjectKind.GENERIC, 25, listOf("No framework marker found"))
        val confidence = when {
            best.second >= 95 -> 99
            best.second >= 80 -> 94
            best.second >= 60 -> 85
            best.second >= 40 -> 72
            else -> 55
        }
        return ProjectDetection(best.first, confidence, best.third.distinct().take(4))
    }

    private fun priority(kind: ProjectKind): Int = when (kind) {
        ProjectKind.ANDROID -> 100
        ProjectKind.FLUTTER -> 95
        ProjectKind.REACT_NATIVE -> 92
        ProjectKind.GODOT -> 90
        ProjectKind.NEXT_JS -> 88
        ProjectKind.PHASER -> 86
        ProjectKind.VUE, ProjectKind.SVELTE, ProjectKind.REACT -> 80
        ProjectKind.RUST, ProjectKind.GO, ProjectKind.PYTHON -> 75
        ProjectKind.NODE -> 60
        ProjectKind.GRADLE -> 50
        ProjectKind.STATIC_WEB -> 40
        ProjectKind.GENERIC -> 0
    }
}
