package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.ClipEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object VideoClipExporter {

    @OptIn(UnstableApi::class)
    suspend fun exportClip(
        context: Context,
        sourceUri: Uri,
        sourceTitle: String,
        segment: QueuedSegment,
        onProgress: (Int) -> Unit
    ): ClipEntity = withContext(Dispatchers.Main.immediate) {
        val clipsDir = File(context.filesDir, "clips").apply { mkdirs() }
        val safeBase = segment.label
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "short_clip" }
        val fileName = "${safeBase}_${System.currentTimeMillis()}.mp4"
        val outputFile = File(clipsDir, fileName)

        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(segment.startMs.coerceAtLeast(0L))
                    .setEndPositionMs(segment.endMs.coerceAtLeast(segment.startMs + 500L))
                    .build()
            )
            .build()

        val videoEffects: List<Effect> = segment.aspectPreset.ratio?.let { targetRatio ->
            listOf(
                Presentation.createForAspectRatio(
                    targetRatio,
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                )
            )
        } ?: emptyList()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        var activeTransformer: Transformer? = null

        val exportJob = launch {
            val holder = ProgressHolder()
            while (isActive) {
                val t = activeTransformer
                if (t != null) {
                    val state = t.getProgress(holder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(holder.progress.coerceIn(0, 100))
                    }
                }
                delay(150L)
            }
        }

        try {
            suspendCancellableCoroutine<ExportResult> { continuation ->
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            onProgress(100)
                            if (continuation.isActive) {
                                continuation.resume(exportResult)
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(exportException)
                            }
                        }
                    })
                    .build()

                activeTransformer = transformer
                continuation.invokeOnCancellation {
                    transformer.cancel()
                }
                transformer.start(editedMediaItem, outputFile.absolutePath)
            }
        } finally {
            exportJob.cancel()
        }

        val galleryUri = withContext(Dispatchers.IO) {
            saveToMediaStore(context, outputFile, fileName)
        }

        val sizeBytes = outputFile.length().coerceAtLeast(0L)
        ClipEntity(
            title = segment.label,
            sourceTitle = sourceTitle,
            filePath = outputFile.absolutePath,
            contentUri = galleryUri?.toString() ?: Uri.fromFile(outputFile).toString(),
            startMs = segment.startMs,
            endMs = segment.endMs,
            durationMs = segment.durationMs,
            aspectRatioLabel = segment.aspectPreset.label,
            fileSizeBytes = sizeBytes,
            createdAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun saveToMediaStore(context: Context, file: File, displayName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/ClipForge"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrNull()
    }

    fun launchShareIntent(context: Context, shareUri: Uri, clipTitle: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_TEXT, clipTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Short Clip")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
