package com.mohnishraj.novaide.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.mohnishraj.novaide.core.Ui
import com.mohnishraj.novaide.theme.NovaPalette
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MinimapView(context: Context) : View(context) {
    private val codePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val viewportPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var palette = NovaPalette.MIDNIGHT
    private var source = ""
    private var editor: CodeEditorView? = null
    private var lineLengths = IntArray(0)
    private var maximumLineLength = 1

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        codePaint.strokeWidth = Ui.dp(context, 1).coerceAtLeast(1).toFloat()
        viewportPaint.style = Paint.Style.FILL
        backgroundPaint.style = Paint.Style.FILL
    }

    fun bind(editorView: CodeEditorView) {
        editor = editorView
        editorView.setOnScrollChangeListener { _, _, _, _, _ -> invalidate() }
    }

    fun applyPalette(value: NovaPalette) {
        palette = value
        backgroundPaint.color = value.gutterBackground
        codePaint.color = value.gutterText
        viewportPaint.color = value.surfaceActive
        invalidate()
    }

    fun setSource(value: String) {
        source = value
        rebuildLineLengths()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (lineLengths.isEmpty() || width <= 0 || height <= 0) return

        val lineHeight = max(1f, height.toFloat() / max(lineLengths.size, 1))
        val sampleStride = max(1, ceil(lineLengths.size / max(height.toFloat(), 1f)).toInt())
        val usableWidth = max(1f, width - Ui.dp(context, 8).toFloat())
        var line = 0
        while (line < lineLengths.size) {
            val y = min(height - 1f, line * lineHeight)
            val ratio = lineLengths[line].coerceAtMost(160).toFloat() / maximumLineLength.coerceAtMost(160).toFloat()
            val endX = Ui.dp(context, 3) + usableWidth * ratio
            canvas.drawLine(Ui.dp(context, 3).toFloat(), y, endX, y, codePaint)
            line += sampleStride
        }

        val editorView = editor ?: return
        val contentHeight = max(editorView.height, editorView.layout?.height ?: editorView.height)
        if (contentHeight <= 0) return
        val topRatio = editorView.scrollY.toFloat() / contentHeight
        val viewportRatio = editorView.height.toFloat() / contentHeight
        val top = (topRatio * height).coerceIn(0f, height.toFloat())
        val minimumBottom = (top + Ui.dp(context, 8)).coerceAtMost(height.toFloat())
        val bottom = ((topRatio + viewportRatio) * height).coerceIn(minimumBottom, height.toFloat())
        canvas.drawRoundRect(
            RectF(0f, top, width.toFloat(), bottom),
            Ui.dp(context, 3).toFloat(),
            Ui.dp(context, 3).toFloat(),
            viewportPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
            return true
        }
        if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE) return true
        val editorView = editor ?: return true
        val contentHeight = max(editorView.height, editorView.layout?.height ?: editorView.height)
        val ratio = (event.y / height.coerceAtLeast(1)).coerceIn(0f, 1f)
        val target = (ratio * contentHeight - editorView.height / 2f).toInt().coerceAtLeast(0)
        editorView.scrollTo(editorView.scrollX, target)
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun rebuildLineLengths() {
        if (source.isEmpty()) {
            lineLengths = intArrayOf(0)
            maximumLineLength = 1
            return
        }
        val lengths = ArrayList<Int>()
        var current = 0
        var maximum = 1
        source.forEach { char ->
            if (char == '\n') {
                lengths += current
                maximum = max(maximum, current)
                current = 0
            } else {
                current++
            }
        }
        lengths += current
        maximum = max(maximum, current)
        lineLengths = lengths.toIntArray()
        maximumLineLength = maximum
    }
}
