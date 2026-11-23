package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exported_clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val sourceTitle: String,
    val filePath: String,
    val contentUri: String,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val aspectRatioLabel: String,
    val fileSizeBytes: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
