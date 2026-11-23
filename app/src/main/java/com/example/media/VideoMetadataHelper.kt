package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoMetadataHelper {

    suspend fun loadVideoInfo(context: Context, uri: Uri): SourceVideoInfo? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val rawWidth = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 1920
                val rawHeight = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 1080
                val rotation = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0

                val (width, height) = if (rotation == 90 || rotation == 270) {
                    rawHeight to rawWidth
                } else {
                    rawWidth to rawHeight
                }

                var displayName = "Source Video"
                var fileSize = 0L
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) {
                            displayName = cursor.getString(nameIdx) ?: displayName
                        }
                        if (sizeIdx >= 0) {
                            fileSize = cursor.getLong(sizeIdx)
                        }
                    }
                }

                if (durationMs <= 0L) return@withContext null

                SourceVideoInfo(
                    uri = uri,
                    title = displayName.substringBeforeLast('.'),
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    fileSizeBytes = fileSize
                )
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

    suspend fun extractFilmstrip(
        context: Context,
        uri: Uri,
        durationMs: Long,
        frameCount: Int = 10
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        if (durationMs <= 0L || frameCount <= 0) return@withContext emptyList()
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, uri)
            for (i in 0 until frameCount) {
                val fraction = if (frameCount == 1) 0.5f else i.toFloat() / (frameCount - 1).toFloat()
                val timeUs = (durationMs * fraction * 1000L).toLong().coerceAtLeast(0L)
                val frame = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (frame != null) {
                    val scaled = scaleBitmapHeight(frame, 120)
                    if (scaled !== frame) frame.recycle()
                    frames.add(scaled)
                }
            }
        } catch (_: Exception) {
            // Return whatever frames were extracted
        } finally {
            runCatching { retriever.release() }
        }
        frames
    }

    suspend fun extractThumbnailForFile(
        context: Context,
        filePath: String,
        contentUri: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                retriever.setDataSource(file.absolutePath)
            } else {
                retriever.setDataSource(context, Uri.parse(contentUri))
            }
            val frame = retriever.getFrameAtTime(
                200_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.getFrameAtTime(0L)
            frame?.let {
                val scaled = scaleBitmapHeight(it, 240)
                if (scaled !== it) it.recycle()
                scaled
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleBitmapHeight(source: Bitmap, targetHeight: Int): Bitmap {
        if (source.height <= targetHeight) return source
        val aspect = source.width.toFloat() / source.height.toFloat()
        val targetWidth = (targetHeight * aspect).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
