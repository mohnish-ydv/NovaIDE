package com.mohnishraj.novaide.editor.search

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.mohnishraj.novaide.core.Ui
import com.mohnishraj.novaide.theme.NovaPalette

class SearchReplacePanel(context: Context) : LinearLayout(context) {
    var onSearchChanged: ((String, SearchOptions) -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onReplace: ((String) -> Unit)? = null
    var onReplaceAll: ((String) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private var palette = NovaPalette.MIDNIGHT
    private val queryInput: EditText
    private val replacementInput: EditText
    private val resultText: TextView
    private val caseToggle: TextView
    private val wordToggle: TextView
    private val regexToggle: TextView
    private var matchCase = false
    private var wholeWord = false
    private var regex = false
    private var suppressCallbacks = false

    init {
        orientation = VERTICAL
        setPadding(Ui.dp(context, 6), Ui.dp(context, 5), Ui.dp(context, 6), Ui.dp(context, 5))

        val searchRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        queryInput = input("Find")
        resultText = label("0/0", 10.5f).apply {
            gravity = Gravity.CENTER
            minWidth = Ui.dp(context, 54)
        }
        val previous = action("↑", "Previous match") { onPrevious?.invoke() }
        val next = action("↓", "Next match") { onNext?.invoke() }
        val close = action("×", "Close search") { onClose?.invoke() }
        searchRow.addView(queryInput, LayoutParams(0, Ui.dp(context, 38), 1f))
        searchRow.addView(resultText, LayoutParams(Ui.dp(context, 58), Ui.dp(context, 38)))
        searchRow.addView(previous, LayoutParams(Ui.dp(context, 38), Ui.dp(context, 38)))
        searchRow.addView(next, LayoutParams(Ui.dp(context, 38), Ui.dp(context, 38)))
        searchRow.addView(close, LayoutParams(Ui.dp(context, 38), Ui.dp(context, 38)))
        addView(searchRow, LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(context, 38)))

        val replaceScroller = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        val replaceRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, Ui.dp(context, 4), 0, 0)
        }
        replacementInput = input("Replace").apply { minWidth = Ui.dp(context, 150) }
        val replace = action("Replace", "Replace current match") { onReplace?.invoke(replacementInput.text.toString()) }
        val replaceAll = action("All", "Replace all matches") { onReplaceAll?.invoke(replacementInput.text.toString()) }
        caseToggle = toggle("Aa", "Match case") {
            matchCase = !matchCase
            refreshToggleState()
            notifySearchChanged()
        }
        wordToggle = toggle("W", "Whole word") {
            wholeWord = !wholeWord
            refreshToggleState()
            notifySearchChanged()
        }
        regexToggle = toggle(".*", "Regular expression") {
            regex = !regex
            refreshToggleState()
            notifySearchChanged()
        }
        replaceRow.addView(replacementInput, LayoutParams(Ui.dp(context, 170), Ui.dp(context, 38)))
        replaceRow.addView(replace, LayoutParams(Ui.dp(context, 74), Ui.dp(context, 38)))
        replaceRow.addView(replaceAll, LayoutParams(Ui.dp(context, 52), Ui.dp(context, 38)))
        replaceRow.addView(caseToggle, LayoutParams(Ui.dp(context, 44), Ui.dp(context, 38)))
        replaceRow.addView(wordToggle, LayoutParams(Ui.dp(context, 42), Ui.dp(context, 38)))
        replaceRow.addView(regexToggle, LayoutParams(Ui.dp(context, 44), Ui.dp(context, 38)))
        replaceScroller.addView(replaceRow, android.widget.FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        addView(replaceScroller, LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(context, 42)))

        queryInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!suppressCallbacks) notifySearchChanged()
            }
        })
        applyPalette(palette)
    }

    fun applyPalette(value: NovaPalette) {
        palette = value
        setBackgroundColor(value.surfaceRaised)
        listOf(queryInput, replacementInput).forEach { input ->
            input.setTextColor(value.textPrimary)
            input.setHintTextColor(value.textSecondary)
            input.background = Ui.rounded(value.editorBackground, 7, context, value.divider)
        }
        resultText.setTextColor(value.textSecondary)
        refreshChildColors(this)
        refreshToggleState()
    }

    fun open(initialQuery: String? = null) {
        visibility = View.VISIBLE
        if (!initialQuery.isNullOrEmpty()) {
            suppressCallbacks = true
            queryInput.setText(initialQuery)
            queryInput.setSelection(queryInput.text.length)
            suppressCallbacks = false
        }
        queryInput.requestFocus()
        queryInput.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(queryInput, InputMethodManager.SHOW_IMPLICIT)
        }
        notifySearchChanged()
    }

    fun closeKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

    fun setResult(current: Int, total: Int, message: String? = null) {
        resultText.text = message ?: if (total <= 0) "0/0" else "${current.coerceIn(1, total)}/$total"
        resultText.setTextColor(if (message != null) palette.danger else palette.textSecondary)
    }

    fun query(): String = queryInput.text.toString()

    fun options(): SearchOptions = SearchOptions(matchCase, wholeWord, regex)

    private fun notifySearchChanged() {
        onSearchChanged?.invoke(queryInput.text.toString(), options())
    }

    private fun input(hintText: String): EditText = EditText(context).apply {
        hint = hintText
        setSingleLine(true)
        textSize = 13f
        setPadding(Ui.dp(context, 10), 0, Ui.dp(context, 10), 0)
        includeFontPadding = false
    }

    private fun label(value: String, size: Float): TextView = TextView(context).apply {
        text = value
        textSize = size
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun action(value: String, description: String, click: () -> Unit): TextView = label(value, if (value.length > 2) 11.5f else 18f).apply {
        gravity = Gravity.CENTER
        contentDescription = description
        setTypeface(typeface, Typeface.BOLD)
        setOnClickListener { click() }
    }

    private fun toggle(value: String, description: String, click: () -> Unit): TextView = action(value, description, click)

    private fun refreshChildColors(view: View) {
        if (view is TextView && view !== queryInput && view !== replacementInput && view !== resultText) {
            view.setTextColor(palette.textPrimary)
        }
        if (view is LinearLayout) {
            for (index in 0 until view.childCount) refreshChildColors(view.getChildAt(index))
        }
        if (view is HorizontalScrollView && view.childCount > 0) refreshChildColors(view.getChildAt(0))
    }

    private fun refreshToggleState() {
        styleToggle(caseToggle, matchCase)
        styleToggle(wordToggle, wholeWord)
        styleToggle(regexToggle, regex)
    }

    private fun styleToggle(view: TextView, active: Boolean) {
        view.background = Ui.rounded(
            if (active) palette.surfaceActive else palette.surfaceRaised,
            7,
            context,
            if (active) palette.accent else palette.divider
        )
        view.setTextColor(if (active) palette.accent else palette.textSecondary)
    }
}
