package com.mohnishraj.novaide.model

import android.net.Uri

data class DocumentNode(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val isDirectory: Boolean,
    val depth: Int,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    var isExpanded: Boolean = false,
    var isLoading: Boolean = false,
    var childrenLoaded: Boolean = false,
    val parentUri: Uri? = null
)
