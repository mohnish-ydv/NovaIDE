package com.mohnishraj.novaide.git

object ConflictParser {
    private const val START = "<<<<<<<"
    private const val MIDDLE = "======="
    private const val END = ">>>>>>>"

    fun find(source: String): List<ConflictBlock> {
        val blocks = mutableListOf<ConflictBlock>()
        var cursor = 0
        while (cursor < source.length) {
            val start = lineMarker(source, START, cursor) ?: break
            val startLineEnd = lineEnd(source, start)
            val middle = lineMarker(source, MIDDLE, startLineEnd) ?: break
            val middleLineEnd = lineEnd(source, middle)
            val end = lineMarker(source, END, middleLineEnd) ?: break
            val endLineEnd = lineEnd(source, end)
            val oursLabel = source.substring(start + START.length, startLineEnd).trim()
            val theirsLabel = source.substring(end + END.length, endLineEnd).trim()
            blocks += ConflictBlock(
                start = start,
                separator = middle,
                endExclusive = lineEndIncludingNewline(source, end),
                ours = source.substring(lineEndIncludingNewline(source, start), middle),
                theirs = source.substring(lineEndIncludingNewline(source, middle), end),
                oursLabel = oursLabel.ifBlank { "Current" },
                theirsLabel = theirsLabel.ifBlank { "Incoming" }
            )
            cursor = lineEndIncludingNewline(source, end)
        }
        return blocks
    }

    fun resolve(source: String, index: Int, resolution: ConflictResolution): String {
        val block = find(source).getOrNull(index) ?: return source
        val replacement = when (resolution) {
            ConflictResolution.OURS -> block.ours
            ConflictResolution.THEIRS -> block.theirs
            ConflictResolution.BOTH -> block.ours + block.theirs
        }
        return source.replaceRange(block.start, block.endExclusive, replacement)
    }

    fun resolveAll(source: String, resolution: ConflictResolution): String {
        var output = source
        while (true) {
            val block = find(output).firstOrNull() ?: return output
            val replacement = when (resolution) {
                ConflictResolution.OURS -> block.ours
                ConflictResolution.THEIRS -> block.theirs
                ConflictResolution.BOTH -> block.ours + block.theirs
            }
            output = output.replaceRange(block.start, block.endExclusive, replacement)
        }
    }

    private fun lineMarker(source: String, marker: String, from: Int): Int? {
        var cursor = from.coerceAtLeast(0)
        while (cursor < source.length) {
            val found = source.indexOf(marker, cursor)
            if (found < 0) return null
            if (found == 0 || source[found - 1] == '\n') return found
            cursor = found + marker.length
        }
        return null
    }

    private fun lineEnd(source: String, offset: Int): Int =
        source.indexOf('\n', offset).let { if (it < 0) source.length else it }

    private fun lineEndIncludingNewline(source: String, offset: Int): Int {
        val end = lineEnd(source, offset)
        return if (end < source.length) end + 1 else end
    }
}
