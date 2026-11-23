package com.example.media

import android.net.Uri
import java.util.UUID

enum class AspectPreset(
    val label: String,
    val shortHint: String,
    val ratio: Float?
) {
    SHORTS_9_16("9:16", "Shorts / Reels", 9f / 16f),
    PORTRAIT_4_5("4:5", "Portrait Feed", 4f / 5f),
    SQUARE_1_1("1:1", "Square", 1f),
    ORIGINAL("Original", "Source Ratio", null)
}

data class SourceVideoInfo(
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long
) {
    val sourceAspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 16f / 9f
}

data class QueuedSegment(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val startMs: Long,
    val endMs: Long,
    val aspectPreset: AspectPreset
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

data class ExportProgressState(
    val isExporting: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentLabel: String = "",
    val progressPercent: Int = 0,
    val errorMessage: String? = null
)

fun formatTimecode(ms: Long, includeTenths: Boolean = true): String {
    val clamped = ms.coerceAtLeast(0L)
    val totalSeconds = clamped / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val tenths = (clamped % 1000L) / 100L
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        includeTenths -> String.format("%02d:%02d.%d", minutes, seconds, tenths)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.1f MB", mb)
    } else {
        String.format("%.0f KB", kb.coerceAtLeast(1.0))
    }
}
