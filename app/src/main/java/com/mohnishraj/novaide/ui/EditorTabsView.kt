package com.mohnishraj.novaide.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.mohnishraj.novaide.core.Ui
import com.mohnishraj.novaide.model.EditorTab
import com.mohnishraj.novaide.theme.NovaPalette

class EditorTabsView(context: Context) : HorizontalScrollView(context) {
    private val strip = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private var palette = NovaPalette.MIDNIGHT
    private var onSelect: ((EditorTab) -> Unit)? = null
    private var onClose: ((EditorTab) -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        addView(strip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(
        tabs: List<EditorTab>,
        active: EditorTab?,
        palette: NovaPalette,
        onSelect: (EditorTab) -> Unit,
        onClose: (EditorTab) -> Unit
    ) {
        this.palette = palette
        this.onSelect = onSelect
        this.onClose = onClose
        strip.removeAllViews()
        tabs.forEach { tab -> strip.addView(createTab(tab, tab === active)) }
        post {
            val activeIndex = tabs.indexOf(active)
            if (activeIndex >= 0 && activeIndex < strip.childCount) {
                val child = strip.getChildAt(activeIndex)
                smoothScrollTo(child.left - Ui.dp(context, 24), 0)
            }
        }
    }

    private fun createTab(tab: EditorTab, active: Boolean): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumWidth = Ui.dp(context, 108)
        background = Ui.rounded(
            if (active) palette.surfaceActive else palette.surface,
            7,
            context,
            if (active) palette.accent else palette.divider
        )
        val label = TextView(context).apply {
            text = (if (tab.isDirty) "● " else "") + tab.name
            setTextColor(if (active) palette.textPrimary else palette.textSecondary)
            textSize = 12.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
            setPadding(Ui.dp(context, 12), 0, Ui.dp(context, 6), 0)
            setOnClickListener { onSelect?.invoke(tab) }
        }
        val close = TextView(context).apply {
            text = "×"
            setTextColor(palette.textSecondary)
            textSize = 17f
            gravity = Gravity.CENTER
            contentDescription = "Close ${tab.name}"
            setOnClickListener { onClose?.invoke(tab) }
        }
        addView(label, LinearLayout.LayoutParams(Ui.dp(context, 112), LayoutParams.MATCH_PARENT))
        addView(close, LinearLayout.LayoutParams(Ui.dp(context, 34), LayoutParams.MATCH_PARENT))
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(context, 38)).apply {
            setMargins(Ui.dp(context, 4), Ui.dp(context, 4), 0, Ui.dp(context, 4))
        }
    }
}
