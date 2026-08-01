package com.mohnishraj.novaide.androidsuite

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

object ManifestInspector {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    data class Result(
        val packageName: String?,
        val permissions: List<String>,
        val components: List<AndroidComponent>,
        val issues: List<AndroidProjectIssue>
    )

    fun inspect(source: String, path: String = "AndroidManifest.xml"): Result {
        val issues = mutableListOf<AndroidProjectIssue>()
        if (source.isBlank()) return Result(null, emptyList(), emptyList(), listOf(AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Empty manifest", "$path is empty.", path)))
        val document = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(source)))
        }.getOrElse {
            return Result(null, emptyList(), emptyList(), listOf(AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Invalid manifest XML", it.message ?: "The manifest could not be parsed.", path)))
        }
        val root = document.documentElement
        if (root?.tagName != "manifest") {
            return Result(null, emptyList(), emptyList(), listOf(AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Invalid manifest root", "Expected a <manifest> root element.", path)))
        }
        val permissions = buildList {
            val tags = listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")
            tags.forEach { tag ->
                val nodes = document.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? Element ?: continue
                    element.getAttributeNS(ANDROID_NS, "name").takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.distinct().sorted()
        val components = buildList {
            listOf("activity", "activity-alias", "service", "receiver", "provider").forEach { type ->
                val nodes = document.getElementsByTagName(type)
                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? Element ?: continue
                    val name = element.getAttributeNS(ANDROID_NS, "name").ifBlank { "<unnamed>" }
                    val exported = element.getAttributeNS(ANDROID_NS, "exported").ifBlank { null }
                    val hasIntentFilter = element.getElementsByTagName("intent-filter").length > 0
                    add(AndroidComponent(type, name, exported, hasIntentFilter))
                    if (hasIntentFilter && exported == null) {
                        issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Missing android:exported", "$type $name has an intent-filter but no explicit android:exported value.", path)
                    }
                    if (type == "provider") {
                        val authorities = element.getAttributeNS(ANDROID_NS, "authorities")
                        if (authorities.isBlank()) issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Provider authority missing", "Provider $name has no android:authorities value.", path)
                    }
                }
            }
        }
        val applications = document.getElementsByTagName("application")
        if (applications.length == 0) {
            issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Application element missing", "$path has no <application> element.", path)
        } else {
            val app = applications.item(0) as? Element
            if (app?.getAttributeNS(ANDROID_NS, "usesCleartextTraffic") == "true") {
                issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Cleartext traffic enabled", "android:usesCleartextTraffic=\"true\" allows unencrypted HTTP traffic.", path)
            }
            if (app?.getAttributeNS(ANDROID_NS, "debuggable") == "true") {
                issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Application marked debuggable", "Remove android:debuggable from the manifest and control it through build types.", path)
            }
            if (app?.getAttributeNS(ANDROID_NS, "allowBackup") == "true") {
                issues += AndroidProjectIssue(AndroidIssueSeverity.INFO, "Backups enabled", "Review whether application data should be included in device/cloud backups.", path)
            }
        }
        permissions.filter { it in sensitivePermissions }.forEach {
            issues += AndroidProjectIssue(AndroidIssueSeverity.INFO, "Sensitive permission", "$it requires a clear product need and, for dangerous permissions, runtime handling.", path)
        }
        return Result(root.getAttribute("package").ifBlank { null }, permissions, components, issues)
    }

    private val sensitivePermissions = setOf(
        "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION", "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS",
        "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE", "android.permission.READ_SMS",
        "android.permission.POST_NOTIFICATIONS", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"
    )
}
