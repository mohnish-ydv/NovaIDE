package com.mohnishraj.novaide.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LineBackgroundSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.EditText
import com.mohnishraj.novaide.core.Ui
import com.mohnishraj.novaide.editor.folding.FoldController
import com.mohnishraj.novaide.editor.search.TextRange
import com.mohnishraj.novaide.editor.syntax.CodeLanguage
import com.mohnishraj.novaide.editor.syntax.HighlightToken
import com.mohnishraj.novaide.editor.syntax.LanguageDetector
import com.mohnishraj.novaide.editor.syntax.SyntaxKind
import com.mohnishraj.novaide.editor.syntax.SyntaxTokenizer
import com.mohnishraj.novaide.theme.NovaPalette
import java.util.concurrent.Executors
import kotlin.math.max

private interface SyntaxMarker
private interface SearchMarker
private interface MultiCursorMarker
private interface BracketMarker
private interface CurrentLineMarker

private class SyntaxColorSpan(color: Int) : ForegroundColorSpan(color), SyntaxMarker
private class SyntaxBoldSpan : StyleSpan(Typeface.BOLD), SyntaxMarker
private class SearchBackgroundSpan(color: Int) : BackgroundColorSpan(color), SearchMarker
private class MultiCursorBackgroundSpan(color: Int) : BackgroundColorSpan(color), MultiCursorMarker
private class BracketBackgroundSpan(color: Int) : BackgroundColorSpan(color), BracketMarker
private class CurrentLineSpan(private val color: Int) : LineBackgroundSpan, CurrentLineMarker {
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val original = paint.color
        paint.color = color
        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        paint.color = original
    }
}

class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditText(context, attrs) {

    companion object {
        private const val HIGHLIGHT_DELAY_MS = 170L
        private const val MAX_MULTI_OCCURRENCES = 500
    }

    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lineBounds = Rect()
    private var gutterWidth = Ui.dp(context, 48)
    private var palette = NovaPalette.MIDNIGHT
    private var lastLineCount = 1
    private var currentFileName = "untitled.txt"
    private var highlightGeneration = 0
    private var pendingHighlight: Runnable? = null
    private val highlightExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "nova-syntax").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val foldController = FoldController()
    private var multiCursorState: MultiCursorState? = null
    private var suppressTextCallbacks = false
    private var programmaticMultiEdit = false
    private var changeStart = 0
    private var changeBefore = 0
    private var changeCount = 0

    var onSelectionChangedListener: (() -> Unit)? = null
    var onUserContentChanged: ((String) -> Unit)? = null
    var onVisualContentChanged: ((String) -> Unit)? = null
    var onMultiCursorStateChanged: ((Int) -> Unit)? = null

    val language: CodeLanguage
        get() = LanguageDetector.fromFileName(currentFileName)

    val isLargeFileMode: Boolean
        get() = length() > SyntaxTokenizer.MAX_HIGHLIGHT_CHARS

    val multiCursorCount: Int
        get() = multiCursorState?.occurrences?.size ?: 0

    init {
        gravity = Gravity.TOP or Gravity.START
        typeface = Typeface.MONOSPACE
        textSize = 14f
        setTextIsSelectable(true)
        setHorizontallyScrolling(true)
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = true
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        setLineSpacing(Ui.dp(context, 2).toFloat(), 1.0f)
        setPadding(gutterWidth + Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 64), Ui.dp(context, 24))

        gutterPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        gutterPaint.textAlign = Paint.Align.RIGHT
        gutterPaint.textSize = Ui.dp(context, 11).toFloat()
        dividerPaint.strokeWidth = Ui.dp(context, 1).toFloat()

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!suppressTextCallbacks && !programmaticMultiEdit) {
                    changeStart = start
                    changeBefore = count
                    changeCount = after
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!suppressTextCallbacks && !programmaticMultiEdit) {
                    changeStart = start
                    changeBefore = before
                    changeCount = count
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val editable = s ?: return
                val count = max(1, lineCount)
                if (count != lastLineCount) {
                    lastLineCount = count
                    updateGutterWidth(count)
                }
                invalidate()
                if (suppressTextCallbacks || programmaticMultiEdit) return

                foldController.clear(editable)
                clearSearchHighlights()
                val propagated = propagateMultiCursorEdit(editable)
                refreshCurrentLine()
                refreshBracketMatch()
                scheduleSyntaxHighlight()
                val updated = editable.toString()
                onVisualContentChanged?.invoke(updated)
                onUserContentChanged?.invoke(updated)
                if (!propagated && multiCursorState != null) applyMultiCursorHighlights()
            }
        })
    }

    fun applyPalette(value: NovaPalette) {
        palette = value
        setBackgroundColor(value.editorBackground)
        setTextColor(value.textPrimary)
        setHintTextColor(value.textSecondary)
        gutterPaint.color = value.gutterText
        dividerPaint.color = value.divider
        refreshCurrentLine()
        refreshBracketMatch()
        applyMultiCursorHighlights()
        scheduleSyntaxHighlight(immediate = true)
        invalidate()
    }

    fun setDocument(fileName: String, value: String) {
        currentFileName = fileName
        highlightGeneration++
        suppressTextCallbacks = true
        multiCursorState = null
        foldController.clear(text ?: return)
        setText(value)
        suppressTextCallbacks = false
        lastLineCount = max(1, lineCount)
        updateGutterWidth(lastLineCount)
        onMultiCursorStateChanged?.invoke(0)
        onVisualContentChanged?.invoke(value)
        refreshCurrentLine()
        refreshBracketMatch()
        scheduleSyntaxHighlight(immediate = true)
    }

    fun setDocumentText(value: String) = setDocument(currentFileName, value)

    fun replaceRange(start: Int, endExclusive: Int, replacement: String) {
        val editable = text ?: return
        val safeStart = start.coerceIn(0, editable.length)
        val safeEnd = endExclusive.coerceIn(safeStart, editable.length)
        editable.replace(safeStart, safeEnd, replacement)
        val cursor = (safeStart + replacement.length).coerceIn(0, editable.length)
        setSelection(cursor)
    }

    fun replaceAllUserText(value: String) {
        val editable = text ?: return
        editable.replace(0, editable.length, value)
        setSelection(value.length.coerceIn(0, length()))
    }

    fun showSearchMatches(ranges: List<TextRange>, currentIndex: Int) {
        val editable = text ?: return
        clearSearchHighlights()
        ranges.forEachIndexed { index, range ->
            if (range.start < 0 || range.endExclusive > editable.length || range.start >= range.endExclusive) return@forEachIndexed
            val color = if (index == currentIndex) palette.searchCurrent else palette.searchMatch
            editable.setSpan(SearchBackgroundSpan(color), range.start, range.endExclusive, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun clearSearchHighlights() {
        val editable = text ?: return
        editable.getSpans(0, editable.length, SearchMarker::class.java).forEach(editable::removeSpan)
    }

    fun toggleFoldAtCursor(): Boolean? {
        val editable = text ?: return null
        val folded = foldController.toggleAtCursor(editable, selectionStart.coerceAtLeast(0), palette)
        invalidate()
        return folded
    }

    fun unfoldAll(): Boolean {
        val editable = text ?: return false
        if (!foldController.hasFolds()) return false
        foldController.clear(editable)
        invalidate()
        return true
    }

    fun enableMultiOccurrenceEdit(): Int {
        val editable = text ?: return 0
        val selected = selectionStart.coerceAtLeast(0) until selectionEnd.coerceAtLeast(0)
        val range = if (!selected.isEmpty()) {
            TextRange(selected.first, selected.last + 1)
        } else {
            wordRangeAtCursor(editable.toString(), selectionStart.coerceAtLeast(0)) ?: return 0
        }
        val token = editable.substring(range.start, range.endExclusive)
        if (token.isBlank() || token.length > 80 || token.contains('\n')) return 0
        val occurrences = findOccurrences(editable.toString(), token)
        if (occurrences.size < 2) return 0
        val primary = occurrences.indexOfFirst { range.start >= it.start && range.endExclusive <= it.endExclusive }
            .takeIf { it >= 0 } ?: 0
        multiCursorState = MultiCursorState(token, occurrences, primary)
        val primaryRange = occurrences[primary]
        setSelection(primaryRange.start, primaryRange.endExclusive)
        applyMultiCursorHighlights()
        onMultiCursorStateChanged?.invoke(occurrences.size)
        return occurrences.size
    }

    fun disableMultiOccurrenceEdit() {
        multiCursorState = null
        val editable = text ?: return
        editable.getSpans(0, editable.length, MultiCursorMarker::class.java).forEach(editable::removeSpan)
        onMultiCursorStateChanged?.invoke(0)
    }

    fun selectedTextOrWord(): String? {
        val source = text?.toString().orEmpty()
        if (source.isEmpty()) return null
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(start)
        if (end > start) return source.substring(start, end)
        val range = wordRangeAtCursor(source, start) ?: return null
        return source.substring(range.start, range.endExclusive)
    }

    override fun onDraw(canvas: Canvas) {
        val currentLayout = layout
        if (currentLayout != null) {
            val translatedTop = scrollY
            canvas.save()
            canvas.clipRect(scrollX, translatedTop, scrollX + gutterWidth, translatedTop + height)
            canvas.drawColor(palette.gutterBackground)
            canvas.restore()

            val firstLine = currentLayout.getLineForVertical(scrollY)
            val lastLine = currentLayout.getLineForVertical(scrollY + height)
            for (line in firstLine..lastLine) {
                val baseline = currentLayout.getLineBaseline(line)
                val number = (line + 1).toString()
                gutterPaint.getTextBounds(number, 0, number.length, lineBounds)
                canvas.drawText(
                    number,
                    (scrollX + gutterWidth - Ui.dp(context, 10)).toFloat(),
                    baseline.toFloat(),
                    gutterPaint
                )
            }
            val dividerX = (scrollX + gutterWidth).toFloat()
            canvas.drawLine(dividerX, scrollY.toFloat(), dividerX, (scrollY + height).toFloat(), dividerPaint)
        }
        super.onDraw(canvas)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!suppressTextCallbacks) {
            refreshCurrentLine()
            refreshBracketMatch()
            onSelectionChangedListener?.invoke()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && event.x + scrollX < gutterWidth) {
            requestFocus()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        highlightGeneration++
        pendingHighlight?.let(::removeCallbacks)
        pendingHighlight = null
        highlightExecutor.shutdownNow()
        super.onDetachedFromWindow()
    }

    private fun scheduleSyntaxHighlight(immediate: Boolean = false) {
        val editable = text ?: return
        val generation = ++highlightGeneration
        val snapshot = editable.toString()
        if (isLargeFileMode || language == CodeLanguage.PLAIN_TEXT) {
            removeSyntaxSpans(editable)
            return
        }
        val work = Runnable {
            val result = SyntaxTokenizer.tokenize(snapshot, language)
            post {
                if (generation != highlightGeneration || text?.toString() != snapshot) return@post
                applySyntaxTokens(result.tokens)
            }
        }
        pendingHighlight?.let(::removeCallbacks)
        pendingHighlight = null
        if (immediate) {
            runCatching { highlightExecutor.submit(work) }
        } else {
            val delayed = Runnable { runCatching { highlightExecutor.submit(work) } }
            pendingHighlight = delayed
            postDelayed(delayed, HIGHLIGHT_DELAY_MS)
        }
    }

    private fun applySyntaxTokens(tokens: List<HighlightToken>) {
        val editable = text ?: return
        removeSyntaxSpans(editable)
        tokens.forEach { token ->
            if (token.start < 0 || token.endExclusive > editable.length || token.start >= token.endExclusive) return@forEach
            editable.setSpan(
                SyntaxColorSpan(colorFor(token.kind)),
                token.start,
                token.endExclusive,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (token.kind == SyntaxKind.KEYWORD || token.kind == SyntaxKind.HEADING) {
                editable.setSpan(SyntaxBoldSpan(), token.start, token.endExclusive, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        invalidate()
    }

    private fun removeSyntaxSpans(editable: Editable) {
        editable.getSpans(0, editable.length, SyntaxMarker::class.java).forEach(editable::removeSpan)
    }

    private fun colorFor(kind: SyntaxKind): Int = when (kind) {
        SyntaxKind.COMMENT -> palette.syntaxComment
        SyntaxKind.STRING -> palette.syntaxString
        SyntaxKind.NUMBER -> palette.syntaxNumber
        SyntaxKind.KEYWORD -> palette.syntaxKeyword
        SyntaxKind.TYPE -> palette.syntaxType
        SyntaxKind.FUNCTION -> palette.syntaxFunction
        SyntaxKind.ANNOTATION -> palette.syntaxAnnotation
        SyntaxKind.TAG -> palette.syntaxTag
        SyntaxKind.ATTRIBUTE -> palette.syntaxAttribute
        SyntaxKind.HEADING -> palette.syntaxHeading
    }

    private fun refreshCurrentLine() {
        val editable = text ?: return
        editable.getSpans(0, editable.length, CurrentLineMarker::class.java).forEach(editable::removeSpan)
        if (editable.isEmpty()) return
        val cursor = selectionStart.coerceIn(0, editable.length)
        val source = editable.toString()
        val previousLineBreak = if (cursor <= 0) -1 else source.lastIndexOf('\n', cursor - 1)
        val start = if (previousLineBreak < 0) 0 else previousLineBreak + 1
        val end = source.indexOf('\n', cursor).let { if (it < 0) source.length else it + 1 }
        if (start <= end) editable.setSpan(CurrentLineSpan(palette.currentLine), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun refreshBracketMatch() {
        val editable = text ?: return
        editable.getSpans(0, editable.length, BracketMarker::class.java).forEach(editable::removeSpan)
        if (editable.isEmpty()) return
        val source = editable.toString()
        val cursor = selectionStart.coerceIn(0, source.length)
        val candidate = when {
            cursor > 0 && source[cursor - 1] in "()[]{}" -> cursor - 1
            cursor < source.length && source[cursor] in "()[]{}" -> cursor
            else -> return
        }
        val pair = findMatchingBracket(source, candidate) ?: return
        editable.setSpan(BracketBackgroundSpan(palette.bracketMatch), candidate, candidate + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        editable.setSpan(BracketBackgroundSpan(palette.bracketMatch), pair, pair + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun findMatchingBracket(source: String, position: Int): Int? {
        val char = source.getOrNull(position) ?: return null
        val pairs = mapOf('(' to ')', '[' to ']', '{' to '}', ')' to '(', ']' to '[', '}' to '{')
        val target = pairs[char] ?: return null
        val forward = char in "([{"
        var depth = 0
        var index = position
        while (true) {
            index += if (forward) 1 else -1
            if (index !in source.indices) return null
            val current = source[index]
            if (current == char) depth++
            if (current == target) {
                if (depth == 0) return index
                depth--
            }
        }
    }

    private fun propagateMultiCursorEdit(editable: Editable): Boolean {
        val state = multiCursorState ?: return false
        val primary = state.occurrences.getOrNull(state.primaryIndex) ?: run {
            disableMultiOccurrenceEdit()
            return false
        }
        if (changeStart < primary.start || changeStart + changeBefore > primary.endExclusive) {
            disableMultiOccurrenceEdit()
            return false
        }
        val insertedEnd = (changeStart + changeCount).coerceIn(changeStart, editable.length)
        val inserted = editable.substring(changeStart, insertedEnd)
        val relativeStart = changeStart - primary.start
        val delta = changeCount - changeBefore
        val newLength = primary.length + delta
        if (newLength <= 0) {
            disableMultiOccurrenceEdit()
            return false
        }
        val newPrimaryEndBeforePropagation = primary.start + newLength
        if (newPrimaryEndBeforePropagation > editable.length) {
            disableMultiOccurrenceEdit()
            return false
        }
        val newToken = editable.substring(primary.start, newPrimaryEndBeforePropagation)
        val selectionStartBefore = selectionStart.coerceAtLeast(0)
        val selectionEndBefore = selectionEnd.coerceAtLeast(selectionStartBefore)
        val countBeforePrimary = state.occurrences.count { it.start < primary.start }

        programmaticMultiEdit = true
        try {
            state.occurrences
                .withIndex()
                .filter { it.index != state.primaryIndex }
                .sortedByDescending { it.value.start }
                .forEach { indexed ->
                    val occurrence = indexed.value
                    val currentStart = occurrence.start + if (occurrence.start > primary.start) delta else 0
                    val replaceStart = currentStart + relativeStart
                    val replaceEnd = replaceStart + changeBefore
                    if (replaceStart >= 0 && replaceEnd <= editable.length && replaceStart <= replaceEnd) {
                        editable.replace(replaceStart, replaceEnd, inserted)
                    }
                }
        } finally {
            programmaticMultiEdit = false
        }

        val updatedRanges = state.occurrences.mapIndexed { index, occurrence ->
            val shiftsBefore = index * delta
            TextRange(occurrence.start + shiftsBefore, occurrence.endExclusive + shiftsBefore + delta)
        }
        val newPrimary = updatedRanges[state.primaryIndex]
        multiCursorState = MultiCursorState(newToken, updatedRanges, state.primaryIndex)
        val selectionShift = countBeforePrimary * delta
        val safeStart = (selectionStartBefore + selectionShift).coerceIn(newPrimary.start, newPrimary.endExclusive)
        val safeEnd = (selectionEndBefore + selectionShift).coerceIn(safeStart, newPrimary.endExclusive)
        setSelection(safeStart, safeEnd)
        applyMultiCursorHighlights()
        return true
    }

    private fun applyMultiCursorHighlights() {
        val editable = text ?: return
        editable.getSpans(0, editable.length, MultiCursorMarker::class.java).forEach(editable::removeSpan)
        val state = multiCursorState ?: return
        state.occurrences.forEachIndexed { index, range ->
            if (range.start >= 0 && range.endExclusive <= editable.length && range.start < range.endExclusive) {
                val color = if (index == state.primaryIndex) palette.searchCurrent else palette.multiCursor
                editable.setSpan(MultiCursorBackgroundSpan(color), range.start, range.endExclusive, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun findOccurrences(source: String, token: String): List<TextRange> {
        val identifier = token.all { it.isLetterOrDigit() || it == '_' || it == '$' }
        val ranges = ArrayList<TextRange>()
        var from = 0
        while (from <= source.length - token.length && ranges.size < MAX_MULTI_OCCURRENCES) {
            val index = source.indexOf(token, from)
            if (index < 0) break
            val end = index + token.length
            val boundaryOkay = !identifier || (
                (index == 0 || !source[index - 1].isIdentifierPart()) &&
                    (end == source.length || !source[end].isIdentifierPart())
                )
            if (boundaryOkay) ranges += TextRange(index, end)
            from = end.coerceAtLeast(index + 1)
        }
        return ranges
    }

    private fun wordRangeAtCursor(source: String, cursor: Int): TextRange? {
        if (source.isEmpty()) return null
        var start = cursor.coerceIn(0, source.length)
        if (start == source.length || (start < source.length && !source[start].isIdentifierPart())) {
            if (start > 0 && source[start - 1].isIdentifierPart()) start-- else return null
        }
        while (start > 0 && source[start - 1].isIdentifierPart()) start--
        var end = start
        while (end < source.length && source[end].isIdentifierPart()) end++
        return if (end > start) TextRange(start, end) else null
    }

    private fun Char.isIdentifierPart(): Boolean = isLetterOrDigit() || this == '_' || this == '$'

    private fun updateGutterWidth(lines: Int) {
        val digits = lines.toString().length
        val next = max(Ui.dp(context, 48), Ui.dp(context, 22 + digits * 9))
        if (next != gutterWidth) {
            gutterWidth = next
            setPadding(gutterWidth + Ui.dp(context, 12), paddingTop, paddingRight, paddingBottom)
        }
    }

    private data class MultiCursorState(
        val token: String,
        val occurrences: List<TextRange>,
        val primaryIndex: Int
    )
}
