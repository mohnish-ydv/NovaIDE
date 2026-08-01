package com.mohnishraj.novaide.core

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.mohnishraj.novaide.theme.NovaPalette

object Ui {
    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radiusDp: Int, context: Context, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(context, radiusDp).toFloat()
            strokeColor?.let { setStroke(dp(context, 1), it) }
        }

    fun text(
        context: Context,
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.CENTER_VERTICAL
    ): TextView = TextView(context).apply {
        this.text = value
        textSize = sizeSp
        setTextColor(color)
        this.gravity = gravity
        includeFontPadding = false
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    fun divider(context: Context, palette: NovaPalette, vertical: Boolean = false): View =
        View(context).apply {
            setBackgroundColor(palette.divider)
            layoutParams = if (vertical) {
                LinearLayout.LayoutParams(dp(context, 1), LinearLayout.LayoutParams.MATCH_PARENT)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1))
            }
        }
}
