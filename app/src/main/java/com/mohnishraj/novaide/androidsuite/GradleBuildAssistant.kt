package com.mohnishraj.novaide.androidsuite

data class GradleBuildCommand(val title: String, val command: String, val description: String)

object GradleBuildAssistant {
    fun commands(report: AndroidProjectReport, hasWrapper: Boolean): List<GradleBuildCommand> {
        val runner = if (hasWrapper) "./gradlew" else "gradle"
        val applicationModule = report.modules.firstOrNull { it.isApplication }?.name
        val taskPrefix = when {
            applicationModule.isNullOrBlank() || applicationModule == "root" || applicationModule == "." -> ""
            else -> ":${applicationModule.trim(':').replace('/', ':')}:"
        }
        fun task(name: String): String = "$taskPrefix$name"
        return listOf(
            GradleBuildCommand("Debug APK", "$runner --no-daemon ${task("assembleDebug")}", "Build an installable debug APK."),
            GradleBuildCommand("Unit tests", "$runner --no-daemon ${task("testDebugUnitTest")}", "Run local JVM unit tests."),
            GradleBuildCommand("Android Lint", "$runner --no-daemon ${task("lintDebug")}", "Run static Android correctness checks."),
            GradleBuildCommand("Clean verification", "$runner --no-daemon clean ${task("testDebugUnitTest")} ${task("assembleDebug")} ${task("lintDebug")}", "Clean, test, compile and lint in one CI-style pass."),
            GradleBuildCommand("Dependency tree", "$runner --no-daemon ${task("dependencies")}", "Inspect direct and transitive dependencies."),
            GradleBuildCommand("Signing report", "$runner --no-daemon ${task("signingReport")}", "Display debug/release signing configuration metadata."),
            GradleBuildCommand("Available tasks", "$runner --no-daemon tasks --all", "List tasks exposed by every module and plugin."),
            GradleBuildCommand("Stop daemons", "$runner --stop", "Release Gradle daemon memory after phone-side builds.")
        )
    }
}
