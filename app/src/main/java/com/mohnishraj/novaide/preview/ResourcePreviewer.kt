package com.mohnishraj.novaide.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.mohnishraj.novaide.files.FileRepository
import com.mohnishraj.novaide.model.DocumentNode
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

sealed class ResourcePreview {
    data class Image(val bitmap: Bitmap, val details: String) : ResourcePreview()
    data class Details(val title: String, val lines: List<String>) : ResourcePreview()
}

class ResourcePreviewer(
    private val context: Context,
    private val repository: FileRepository
) {
    fun load(node: DocumentNode): ResourcePreview {
        return when {
            node.mimeType.startsWith("image/") && node.mimeType != "image/svg+xml" -> loadImage(node)
            node.mimeType.startsWith("audio/") || node.mimeType.startsWith("video/") -> loadMedia(node)
            node.name.endsWith(".zip", ignoreCase = true) || node.name.endsWith(".apk", ignoreCase = true) -> loadArchive(node)
            else -> loadBinary(node)
        }
    }

    private fun loadImage(node: DocumentNode): ResourcePreview {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        repository.openInput(node.uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        val bitmap = repository.openInput(node.uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return ResourcePreview.Details(node.name, listOf("Image could not be decoded"))
        return ResourcePreview.Image(bitmap, "${bounds.outWidth} × ${bounds.outHeight} • ${formatBytes(node.size)}")
    }

    private fun loadMedia(node: DocumentNode): ResourcePreview {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, node.uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ResourcePreview.Details(node.name, buildList {
                add("Type: ${node.mimeType}")
                add("Size: ${formatBytes(node.size)}")
                if (duration > 0) add("Duration: ${formatDuration(duration)}")
                if (!width.isNullOrBlank() && !height.isNullOrBlank()) add("Resolution: $width × $height")
                if (bitrate != null && bitrate > 0) add("Bitrate: ${bitrate / 1000} kbps")
                if (!title.isNullOrBlank()) add("Title: $title")
                if (!artist.isNullOrBlank()) add("Artist: $artist")
            })
        } catch (error: Exception) {
            ResourcePreview.Details(node.name, listOf("Type: ${node.mimeType}", "Size: ${formatBytes(node.size)}", error.message ?: "Metadata unavailable"))
        } finally {
            retriever.release()
        }
    }

    private fun loadArchive(node: DocumentNode): ResourcePreview {
        var files = 0
        var folders = 0
        var expanded = 0L
        val examples = mutableListOf<String>()
        repository.openInput(node.uri)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                while (files + folders < 5000) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) folders++ else {
                        files++
                        if (entry.size > 0) expanded += entry.size
                    }
                    if (examples.size < 8) examples += entry.name.take(100)
                    zip.closeEntry()
                }
            }
        }
        return ResourcePreview.Details(node.name, buildList {
            add("Archive size: ${formatBytes(node.size)}")
            add("Entries scanned: ${files + folders}")
            add("Files: $files • Folders: $folders")
            if (expanded > 0) add("Declared unpacked size: ${formatBytes(expanded)}")
            if (examples.isNotEmpty()) {
                add("")
                add("Contents preview:")
                addAll(examples)
            }
        })
    }

    private fun loadBinary(node: DocumentNode): ResourcePreview {
        val output = ByteArrayOutputStream()
        repository.openInput(node.uri)?.use { input ->
            val buffer = ByteArray(64)
            val read = input.read(buffer)
            if (read > 0) output.write(buffer, 0, read)
        }
        val hex = output.toByteArray().joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
        return ResourcePreview.Details(node.name, listOf(
            "Type: ${node.mimeType}",
            "Size: ${formatBytes(node.size)}",
            "Modified: ${if (node.lastModified > 0) java.text.DateFormat.getDateTimeInstance().format(node.lastModified) else "Unknown"}",
            "",
            "First bytes:",
            hex.ifBlank { "Empty file" }
        ))
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
