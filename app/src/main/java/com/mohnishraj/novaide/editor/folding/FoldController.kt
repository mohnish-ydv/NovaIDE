package com.mohnishraj.novaide.editor.folding

import android.graphics.Color
import android.text.Editable
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LineHeightSpan
import android.text.style.RelativeSizeSpan
import com.mohnishraj.novaide.theme.NovaPalette

private interface FoldMarker
private class FoldForegroundSpan : ForegroundColorSpan(Color.TRANSPARENT), FoldMarker
private class FoldSizeSpan : RelativeSizeSpan(0.01f), FoldMarker
private class FoldBraceSpan(color: Int) : BackgroundColorSpan(color), FoldMarker
private class FoldHeightSpan : LineHeightSpan, FoldMarker {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        v: Int,
        fm: android.graphics.Paint.FontMetricsInt
    ) {
        fm.ascent = 0
        fm.descent = 0
        fm.top = 0
        fm.bottom = 0
        fm.leading = 0
    }
}

data class FoldRecord(
    val openingBrace: Int,
    val hiddenStart: Int,
    val hiddenEnd: Int,
    val closingBrace: Int
)

class FoldController {
    private val records = mutableListOf<FoldRecord>()

    fun toggleAtCursor(editable: Editable, cursor: Int, palette: NovaPalette): Boolean {
        val existing = records.firstOrNull { cursor in it.openingBrace..it.closingBrace }
        if (existing != null) {
            removeRecord(editable, existing)
            return false
        }
        val block = findBraceBlock(editable.toString(), cursor) ?: return false
        if (block.hiddenStart >= block.hiddenEnd) return false
        records += block
        editable.setSpan(FoldForegroundSpan(), block.hiddenStart, block.hiddenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        editable.setSpan(FoldSizeSpan(), block.hiddenStart, block.hiddenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        editable.setSpan(FoldHeightSpan(), block.hiddenStart, block.hiddenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        editable.setSpan(FoldBraceSpan(palette.surfaceActive), block.openingBrace, block.openingBrace + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return true
    }

    fun clear(editable: Editable) {
        editable.getSpans(0, editable.length, FoldMarker::class.java).forEach(editable::removeSpan)
        records.clear()
    }

    fun hasFolds(): Boolean = records.isNotEmpty()

    private fun removeRecord(editable: Editable, record: FoldRecord) {
        editable.getSpans(record.openingBrace, record.closingBrace + 1, FoldMarker::class.java).forEach(editable::removeSpan)
        records.remove(record)
    }

    private fun findBraceBlock(source: String, cursor: Int): FoldRecord? {
        if (source.isEmpty()) return null
        val safeCursor = cursor.coerceIn(0, source.length)
        var opening = -1
        var depth = 0
        var index = (safeCursor - 1).coerceAtLeast(0)
        while (index >= 0) {
            when (source[index]) {
                '}' -> depth++
                '{' -> if (depth == 0) {
                    opening = index
                    break
                } else depth--
            }
            index--
        }
        if (opening < 0) {
            opening = source.indexOf('{', safeCursor)
            if (opening < 0) return null
        }

        var closing = -1
        depth = 0
        index = opening
        var quote: Char? = null
        var escaped = false
        while (index < source.length) {
            val char = source[index]
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else {
                when (char) {
                    '\'', '"', '`' -> quote = char
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            closing = index
                            break
                        }
                    }
                }
            }
            index++
        }
        if (closing <= opening) return null

        val firstNewline = source.indexOf('\n', opening)
        if (firstNewline < 0 || firstNewline >= closing) return null
        val closingLineStart = source.lastIndexOf('\n', closing).let { if (it < 0) closing else it + 1 }
        val hiddenStart = firstNewline + 1
        val hiddenEnd = closingLineStart.coerceAtLeast(hiddenStart)
        if (hiddenEnd <= hiddenStart) return null
        return FoldRecord(opening, hiddenStart, hiddenEnd, closing)
    }
}
