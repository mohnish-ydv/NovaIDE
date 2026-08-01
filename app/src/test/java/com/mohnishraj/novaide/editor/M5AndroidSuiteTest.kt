package com.mohnishraj.novaide.editor

import com.mohnishraj.novaide.androidsuite.AndroidIssueSeverity
import com.mohnishraj.novaide.androidsuite.AndroidProjectAnalyzer
import com.mohnishraj.novaide.androidsuite.AndroidSourceFile
import com.mohnishraj.novaide.androidsuite.ApkInspector
import com.mohnishraj.novaide.androidsuite.BuildLogAnalyzer
import com.mohnishraj.novaide.androidsuite.BuildLogReader
import com.mohnishraj.novaide.androidsuite.GradleBuildAssistant
import com.mohnishraj.novaide.androidsuite.ManifestEditor
import com.mohnishraj.novaide.androidsuite.ResourceAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class M5AndroidSuiteTest {
    @Test fun androidProjectInspectionFindsModulesAndManifestProblems() {
        val files = listOf(
            AndroidSourceFile("settings.gradle.kts", "rootProject.name = \"Demo\"\ninclude(\":app\")"),
            AndroidSourceFile("app/build.gradle.kts", """
                plugins { id("com.android.application") }
                android {
                    namespace = "demo.app"
                    compileSdk = 35
                    defaultConfig {
                        applicationId = "demo.app"
                        minSdk = 26
                        targetSdk = 35
                        versionCode = 7
                        versionName = "1.2"
                    }
                }
                dependencies { implementation("com.example:lib:1.+") }
            """.trimIndent()),
            AndroidSourceFile("app/src/main/AndroidManifest.xml", """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <uses-permission android:name="android.permission.INTERNET" />
                    <application android:usesCleartextTraffic="true">
                        <activity android:name=".MainActivity">
                            <intent-filter><action android:name="android.intent.action.MAIN" /></intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()),
            AndroidSourceFile("app/src/main/java/demo/MainActivity.kt", sizeBytes = 1200)
        )
        val report = AndroidProjectAnalyzer.analyze(files, "Demo")
        assertTrue(report.isAndroidProject)
        assertEquals(1, report.modules.size)
        assertEquals(35, report.modules.first().targetSdk)
        assertEquals(listOf("android.permission.INTERNET"), report.permissions)
        assertTrue(report.issues.any { it.title == "Missing android:exported" && it.severity == AndroidIssueSeverity.ERROR })
        assertTrue(report.issues.any { it.title == "Dynamic dependency version" })
        assertTrue(report.issues.any { it.title == "Cleartext traffic enabled" })
    }

    @Test fun manifestPermissionEditorIsIdempotentAndReversible() {
        val source = """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application />
</manifest>"""
        val added = ManifestEditor.addPermission(source, "android.permission.INTERNET")
        assertTrue(added.contains("android.permission.INTERNET"))
        assertEquals(added, ManifestEditor.addPermission(added, "android.permission.INTERNET"))
        val removed = ManifestEditor.removePermission(added, "android.permission.INTERNET")
        assertFalse(removed.contains("android.permission.INTERNET"))
    }

    @Test fun resourcesDetectInvalidLargeAndDuplicateEntries() {
        val files = listOf(
            AndroidSourceFile("app/src/main/res/drawable/icon.png", sizeBytes = 3L * 1024L * 1024L),
            AndroidSourceFile("app/src/main/res/drawable/icon.webp", sizeBytes = 20),
            AndroidSourceFile("app/src/main/res/layout/Bad-Name.xml", sizeBytes = 100)
        )
        val report = ResourceAnalyzer.analyze(files)
        assertEquals(3, report.totalFiles)
        assertTrue(report.issues.any { it.title == "Large packaged resource" })
        assertTrue(report.issues.any { it.title == "Invalid resource filename" })
        assertTrue(report.issues.any { it.title == "Duplicate resource in same qualifier" })
    }

    @Test fun apkInspectorReportsDexAbiAndSignatureEntries() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun add(name: String, bytes: ByteArray = byteArrayOf(1, 2, 3)) {
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
            add("AndroidManifest.xml")
            add("resources.arsc")
            add("classes.dex")
            add("classes2.dex")
            add("lib/arm64-v8a/libdemo.so")
            add("META-INF/CERT.RSA")
            add("res/layout/main.xml")
        }
        val report = ApkInspector.inspect("demo.apk", ByteArrayInputStream(output.toByteArray()))
        assertEquals(2, report.dexFiles)
        assertEquals(listOf("arm64-v8a"), report.nativeAbis)
        assertTrue(report.hasManifest)
        assertTrue(report.hasV1Signature)
    }

    @Test fun buildLogsIdentifyCrashAndLintRootCauses() {
        val report = BuildLogAnalyzer.analyze("""
            FATAL EXCEPTION: main
            java.lang.SecurityException: Permission Denial
            Lint found 1 errors, 0 warnings. [WrongConstant]
        """.trimIndent())
        assertTrue(report.findings.any { it.title == "Android app crash" })
        assertTrue(report.findings.any { it.title == "Android SecurityException" })
        assertTrue(report.findings.any { it.title == "Android Lint failure" })
        assertEquals(AndroidIssueSeverity.ERROR, report.probableRootCause?.severity)
    }

    @Test fun zippedLogReaderCollectsTextFilesOnly() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("build.log")); zip.write("BUILD FAILED".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("image.png")); zip.write(byteArrayOf(0, 1)); zip.closeEntry()
        }
        val text = BuildLogReader.read("logs.zip", ByteArrayInputStream(output.toByteArray()))
        assertTrue(text.contains("build.log"))
        assertTrue(text.contains("BUILD FAILED"))
        assertFalse(text.contains("image.png"))
    }

    @Test fun gradleAssistantUsesWrapperAndDetectedModule() {
        val report = AndroidProjectAnalyzer.analyze(listOf(
            AndroidSourceFile("settings.gradle.kts", "include(\":features:mobile\")"),
            AndroidSourceFile("features/mobile/build.gradle.kts", "plugins { id(\"com.android.application\") }\nandroid { compileSdk = 35 }")
        ))
        val commands = GradleBuildAssistant.commands(report, hasWrapper = true)
        assertTrue(commands.first().command.startsWith("./gradlew"))
        assertTrue(commands.first().command.contains(":features:mobile:assembleDebug"))
    }

    @Test fun rootAndroidModuleUsesRootGradleTasks() {
        val report = AndroidProjectAnalyzer.analyze(listOf(
            AndroidSourceFile("build.gradle.kts", "plugins { id(\"com.android.application\") }\nandroid { compileSdk = 35 }")
        ))
        val command = GradleBuildAssistant.commands(report, hasWrapper = false).first().command
        assertEquals("gradle --no-daemon assembleDebug", command)
    }

}
