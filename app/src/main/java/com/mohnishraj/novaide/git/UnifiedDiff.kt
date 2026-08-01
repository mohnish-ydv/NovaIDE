package com.mohnishraj.novaide.git

object UnifiedDiff {
    private const val MAX_LINES = 1_500
    private const val MAX_CELLS = 700_000
    private const val MAX_OUTPUT_CHARS = 180_000

    private sealed class Op(val line: String) {
        class Same(line: String) : Op(line)
        class Add(line: String) : Op(line)
        class Delete(line: String) : Op(line)
    }

    fun create(oldText: String, newText: String, oldName: String = "baseline", newName: String = "workspace"): UnifiedDiffResult {
        if (oldText == newText) return UnifiedDiffResult("No differences.", 0, 0, false)
        val oldLines = splitLines(oldText)
        val newLines = splitLines(newText)
        if (oldLines.size > MAX_LINES || newLines.size > MAX_LINES || oldLines.size.toLong() * newLines.size > MAX_CELLS) {
            return fallback(oldText, newText, oldName, newName)
        }
        val table = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }
        for (i in oldLines.indices.reversed()) {
            for (j in newLines.indices.reversed()) {
                table[i][j] = if (oldLines[i] == newLines[j]) table[i + 1][j + 1] + 1
                else maxOf(table[i + 1][j], table[i][j + 1])
            }
        }
        val ops = mutableListOf<Op>()
        var i = 0
        var j = 0
        while (i < oldLines.size || j < newLines.size) {
            when {
                i < oldLines.size && j < newLines.size && oldLines[i] == newLines[j] -> {
                    ops += Op.Same(oldLines[i]); i++; j++
                }
                j < newLines.size && (i == oldLines.size || table[i][j + 1] >= table[i + 1][j]) -> {
                    ops += Op.Add(newLines[j]); j++
                }
                i < oldLines.size -> {
                    ops += Op.Delete(oldLines[i]); i++
                }
            }
        }
        val builder = StringBuilder("--- $oldName\n+++ $newName\n")
        var additions = 0
        var deletions = 0
        var truncated = false
        ops.forEach { op ->
            val prefix = when (op) {
                is Op.Same -> " "
                is Op.Add -> "+"
                is Op.Delete -> "-"
            }
            if (op is Op.Add) additions++
            if (op is Op.Delete) deletions++
            if (!truncated) {
                if (builder.length + op.line.length + 2 > MAX_OUTPUT_CHARS) {
                    builder.append("\n… diff output truncated …\n")
                    truncated = true
                } else builder.append(prefix).append(op.line).append('\n')
            }
        }
        return UnifiedDiffResult(builder.toString(), additions, deletions, truncated)
    }

    private fun splitLines(text: String): List<String> = if (text.isEmpty()) emptyList() else text.split('\n')

    private fun fallback(oldText: String, newText: String, oldName: String, newName: String): UnifiedDiffResult {
        val oldLines = oldText.count { it == '\n' } + if (oldText.isNotEmpty()) 1 else 0
        val newLines = newText.count { it == '\n' } + if (newText.isNotEmpty()) 1 else 0
        val text = "--- $oldName\n+++ $newName\nLarge-file diff was summarized for mobile safety.\nOld: $oldLines lines, ${oldText.length} chars\nNew: $newLines lines, ${newText.length} chars\n"
        return UnifiedDiffResult(text, maxOf(0, newLines - oldLines), maxOf(0, oldLines - newLines), true)
    }
}
