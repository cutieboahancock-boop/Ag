package com.example.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ClipRepository(
    private val context: Context,
    private val clipDao: ClipDao
) {
    val exportedClips: Flow<List<ClipEntity>> = clipDao.observeAllClips()

    suspend fun saveClip(clip: ClipEntity): Long {
        return clipDao.insertClip(clip)
    }

    suspend fun deleteClip(clip: ClipEntity) {
        withContext(Dispatchers.IO) {
            val file = File(clip.filePath)
            if (file.exists()) {
                file.delete()
            }
            if (clip.contentUri.startsWith("content://media/")) {
                runCatching {
                    context.contentResolver.delete(Uri.parse(clip.contentUri), null, null)
                }
            }
        }
        clipDao.deleteClipById(clip.id)
    }

    fun resolvePlayableUri(clip: ClipEntity): Uri {
        val file = File(clip.filePath)
        return if (file.exists()) {
            Uri.fromFile(file)
        } else {
            Uri.parse(clip.contentUri)
        }
    }

    fun resolveShareUri(clip: ClipEntity): Uri {
        val file = File(clip.filePath)
        return if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.parse(clip.contentUri)
        }
    }
}
