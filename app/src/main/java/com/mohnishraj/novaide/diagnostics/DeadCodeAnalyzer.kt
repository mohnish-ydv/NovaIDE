package com.mohnishraj.novaide.diagnostics

object DeadCodeAnalyzer {
    private const val MAX_FINDINGS = 250
    private val ignoredSymbols = setOf("main", "oncreate", "onstart", "onresume", "ondestroy", "render", "update", "init", "constructor")
    private val declarationPatterns = listOf(
        Regex("""(?m)^\s*private\s+(?:suspend\s+|inline\s+)?fun\s+([A-Za-z_][\w]*)\s*\(""") to "private function",
        Regex("""(?m)^\s*private\s+(?:data\s+|sealed\s+|enum\s+)?(?:class|object|interface)\s+([A-Za-z_][\w]*)""") to "private type",
        Regex("""(?m)^\s*private\s+(?:const\s+)?(?:val|var)\s+([A-Za-z_][\w]*)""") to "private property",
        Regex("""(?m)^\s*private\s+(?:static\s+)?[\w<>, ?\[\].]+\s+([A-Za-z_][\w]*)\s*\(""") to "private method",
        Regex("""(?m)^\s*def\s+(_[A-Za-z_][\w]*)\s*\(""") to "private-style Python function"
    )

    fun analyze(files: List<DiagnosticFile>): List<DiagnosticFinding> {
        val textFiles = files.filter { it.text != null }
        val corpus = textFiles.joinToString("\n") { it.text.orEmpty() }
        val findings = mutableListOf<DiagnosticFinding>()
        textFiles.forEach { file ->
            val source = file.text ?: return@forEach
            declarationPatterns.forEach { (pattern, kind) ->
                pattern.findAll(source).forEach declaration@{ match ->
                    if (findings.size >= MAX_FINDINGS) return@declaration
                    val symbol = match.groupValues[1]
                    if (symbol.lowercase() in ignoredSymbols || symbol.length < 3) return@declaration
                    val occurrences = Regex("\\b${Regex.escape(symbol)}\\b").findAll(corpus).take(3).count()
                    if (occurrences <= 1) {
                        findings += DiagnosticFinding(
                            DiagnosticCategory.DEAD_CODE, DiagnosticSeverity.LOW,
                            "Possibly unused $kind: $symbol",
                            "The declaration was found, but no second project reference was detected.",
                            "Confirm reflective/framework use before removing it. Delete only after tests and a clean build.",
                            file.path, lineOf(source, match.range.first), match.value.trim().take(180), 72
                        )
                    }
                }
            }
            unusedImports(file, source).forEach { findings += it }
            unreachableStatements(file, source).forEach { findings += it }
        }
        findings += unusedAndroidResources(files, corpus)
        return findings.take(MAX_FINDINGS)
    }

    private fun unusedImports(file: DiagnosticFile, source: String): List<DiagnosticFinding> {
        val results = mutableListOf<DiagnosticFinding>()
        Regex("""(?m)^\s*import\s+(?:static\s+)?([\w.]+)(?:\s+as\s+(\w+))?\s*;?\s*$""").findAll(source).forEach { match ->
            val imported = match.groupValues[2].ifBlank { match.groupValues[1].substringAfterLast('.') }
            if (imported == "*" || imported.length < 2) return@forEach
            val afterImport = source.removeRange(match.range)
            if (!Regex("\\b${Regex.escape(imported)}\\b").containsMatchIn(afterImport)) {
                results += DiagnosticFinding(
                    DiagnosticCategory.DEAD_CODE, DiagnosticSeverity.INFO, "Unused import: $imported",
                    "The imported symbol is not referenced elsewhere in this file.",
                    "Remove the import and run the compiler to confirm.", file.path,
                    lineOf(source, match.range.first), match.value.trim(), 90
                )
            }
        }
        return results
    }

    private fun unreachableStatements(file: DiagnosticFile, source: String): List<DiagnosticFinding> {
        val lines = source.lines()
        val results = mutableListOf<DiagnosticFinding>()
        for (index in 0 until lines.lastIndex) {
            val current = lines[index]
            if (!Regex("""\b(return|throw|continue|break)\b[^;{}]*;?\s*$""").containsMatchIn(current.trim())) continue
            val currentIndent = current.takeWhile { it == ' ' || it == '\t' }.length
            val nextIndex = (index + 1..minOf(index + 3, lines.lastIndex)).firstOrNull { lines[it].trim().isNotEmpty() } ?: continue
            val next = lines[nextIndex]
            val nextTrim = next.trim()
            val nextIndent = next.takeWhile { it == ' ' || it == '\t' }.length
            if (!nextTrim.startsWith("}") && nextIndent >= currentIndent && !nextTrim.startsWith("case ") && !nextTrim.startsWith("default:")) {
                results += DiagnosticFinding(
                    DiagnosticCategory.DEAD_CODE, DiagnosticSeverity.MEDIUM, "Possibly unreachable statement",
                    "A statement follows an unconditional control-flow exit at the same nesting level.",
                    "Review the branch and move or delete unreachable logic after confirming control flow.",
                    file.path, nextIndex + 1, nextTrim.take(180), 76
                )
            }
        }
        return results
    }

    private fun unusedAndroidResources(files: List<DiagnosticFile>, corpus: String): List<DiagnosticFinding> {
        val safeNames = setOf("ic_launcher", "ic_launcher_round", "colors", "strings", "themes", "styles", "backup_rules", "data_extraction_rules")
        return files.asSequence().filter { it.path.contains("/src/main/res/") && it.text == null }
            .mapNotNull { file ->
                val name = file.path.substringAfterLast('/').substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_]"), "_")
                if (name in safeNames || name.isBlank()) return@mapNotNull null
                val referenced = Regex("(?:@|R\\.[A-Za-z_]+\\.)${Regex.escape(name)}\\b").containsMatchIn(corpus)
                if (referenced) null else DiagnosticFinding(
                    DiagnosticCategory.DEAD_CODE, DiagnosticSeverity.LOW, "Possibly unused Android resource: $name",
                    "No XML or source reference was detected for this resource filename.",
                    "Check manifest, reflection, resource shrinking rules, and product flavors before deleting.",
                    file.path, null, null, 62
                )
            }.take(80).toList()
    }

    private fun lineOf(source: String, offset: Int): Int = source.take(offset.coerceAtLeast(0)).count { it == '\n' } + 1
}
