package com.mohnishraj.novaide.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.mohnishraj.novaide.core.Ui
import com.mohnishraj.novaide.model.DocumentNode
import com.mohnishraj.novaide.theme.NovaPalette

class FileTreeAdapter(
    private val context: Context,
    private var palette: NovaPalette,
    private val onClick: (DocumentNode) -> Unit,
    private val onLongClick: (DocumentNode, View) -> Unit
) : BaseAdapter() {

    private val items = mutableListOf<DocumentNode>()

    fun submit(nodes: List<DocumentNode>) {
        items.clear()
        items.addAll(nodes)
        notifyDataSetChanged()
    }

    fun applyPalette(value: NovaPalette) {
        palette = value
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): DocumentNode = items[position]
    override fun getItemId(position: Int): Long = items[position].uri.toString().hashCode().toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = (convertView as? LinearLayout) ?: createRow()
        val node = getItem(position)
        val icon = row.getChildAt(0) as TextView
        val title = row.getChildAt(1) as TextView
        val detail = row.getChildAt(2) as TextView

        row.setPadding(Ui.dp(context, 8 + node.depth * 14), Ui.dp(context, 3), Ui.dp(context, 8), Ui.dp(context, 3))
        row.setBackgroundColor(palette.surface)
        icon.text = when {
            node.isLoading -> "…"
            node.isDirectory && node.isExpanded -> "▾"
            node.isDirectory -> "▸"
            else -> fileGlyph(node.name)
        }
        icon.setTextColor(if (node.isDirectory) palette.accent else palette.textSecondary)
        title.text = node.name
        title.setTextColor(palette.textPrimary)
        title.setTypeface(Typeface.DEFAULT, if (node.isDirectory) Typeface.BOLD else Typeface.NORMAL)
        detail.text = if (!node.isDirectory && node.size > 0L) compactSize(node.size) else ""
        detail.setTextColor(palette.textSecondary)

        row.setOnClickListener { onClick(node) }
        row.setOnLongClickListener {
            onLongClick(node, row)
            true
        }
        return row
    }

    private fun createRow(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = Ui.dp(context, 42)
        addView(TextView(context).apply {
            gravity = Gravity.CENTER
            textSize = 16f
        }, LinearLayout.LayoutParams(Ui.dp(context, 30), LinearLayout.LayoutParams.MATCH_PARENT))
        addView(TextView(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            textSize = 13.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        addView(TextView(context).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            textSize = 10f
        }, LinearLayout.LayoutParams(Ui.dp(context, 46), LinearLayout.LayoutParams.MATCH_PARENT))
    }

    private fun fileGlyph(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts", "java" -> "K"
        "html", "htm" -> "H"
        "css", "scss", "sass" -> "#"
        "js", "ts", "jsx", "tsx" -> "J"
        "json" -> "{}"
        "xml" -> "<>"
        "md", "markdown" -> "M"
        "py" -> "P"
        else -> "·"
    }

    private fun compactSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> "%.1fM".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.0fK".format(bytes / 1024.0)
        else -> "${bytes}B"
    }
}
