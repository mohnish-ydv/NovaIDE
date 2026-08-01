package com.mohnishraj.novaide.androidsuite

data class AndroidSourceFile(
    val path: String,
    val content: String? = null,
    val sizeBytes: Long = 0L
)

enum class AndroidIssueSeverity { ERROR, WARNING, INFO }

data class AndroidProjectIssue(
    val severity: AndroidIssueSeverity,
    val title: String,
    val detail: String,
    val path: String? = null,
    val line: Int? = null
)

data class AndroidComponent(
    val type: String,
    val name: String,
    val exported: String?,
    val hasIntentFilter: Boolean
)

data class AndroidDependency(
    val configuration: String,
    val notation: String,
    val sourcePath: String
)

data class AndroidModuleReport(
    val name: String,
    val buildFile: String,
    val namespace: String?,
    val applicationId: String?,
    val compileSdk: Int?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val versionCode: Int?,
    val versionName: String?,
    val isApplication: Boolean,
    val buildTypes: List<String>,
    val dependencies: List<AndroidDependency>
)

data class AndroidProjectReport(
    val isAndroidProject: Boolean,
    val projectName: String,
    val modules: List<AndroidModuleReport>,
    val permissions: List<String>,
    val components: List<AndroidComponent>,
    val manifestPath: String?,
    val sourceFiles: Int,
    val testFiles: Int,
    val resourceFiles: Int,
    val assetFiles: Int,
    val nativeLibraries: Int,
    val issues: List<AndroidProjectIssue>
) {
    val applicationModules: Int get() = modules.count { it.isApplication }
    val dependencyCount: Int get() = modules.sumOf { it.dependencies.size }
}

data class ResourceItem(
    val type: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val qualifier: String
)

data class ResourceReport(
    val totalFiles: Int,
    val totalBytes: Long,
    val byType: Map<String, Int>,
    val byQualifier: Map<String, Int>,
    val largest: List<ResourceItem>,
    val duplicateNames: Map<String, List<String>>,
    val invalidNames: List<ResourceItem>,
    val issues: List<AndroidProjectIssue>
)

data class ApkReport(
    val fileName: String,
    val entryCount: Int,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
    val dexFiles: Int,
    val nativeAbis: List<String>,
    val nativeLibraries: Int,
    val resourceEntries: Int,
    val assetEntries: Int,
    val hasManifest: Boolean,
    val hasResourcesTable: Boolean,
    val hasV1Signature: Boolean,
    val hasV2OrNewerSignatureBlockHint: Boolean,
    val largestEntries: List<Pair<String, Long>>,
    val warnings: List<String>
)

data class BuildLogFinding(
    val severity: AndroidIssueSeverity,
    val title: String,
    val evidence: String,
    val suggestion: String
)

data class BuildLogReport(
    val findings: List<BuildLogFinding>,
    val errorLines: Int,
    val warningLines: Int,
    val probableRootCause: BuildLogFinding?
)
