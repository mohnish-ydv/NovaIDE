package com.mohnishraj.novaide.model

import android.net.Uri

data class EditorTab(
    val uri: Uri,
    var name: String,
    var content: String,
    var savedContentHash: Int,
    var cursorStart: Int = 0,
    var cursorEnd: Int = 0,
    var scrollX: Int = 0,
    var scrollY: Int = 0,
    var isReadOnly: Boolean = false,
    var lastKnownModified: Long = 0L
) {
    val isDirty: Boolean
        get() = content.hashCode() != savedContentHash
}
