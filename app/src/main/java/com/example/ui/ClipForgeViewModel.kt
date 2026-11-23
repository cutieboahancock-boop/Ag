package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.ClipEntity
import com.example.data.ClipForgeDatabase
import com.example.data.ClipRepository
import com.example.media.AspectPreset
import com.example.media.ExportProgressState
import com.example.media.QueuedSegment
import com.example.media.SourceVideoInfo
import com.example.media.VideoClipExporter
import com.example.media.VideoMetadataHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ClipForgeTab {
    STUDIO,
    MY_CLIPS
}

data class StudioUiState(
    val activeTab: ClipForgeTab = ClipForgeTab.STUDIO,
    val sourceVideo: SourceVideoInfo? = null,
    val isLoadingVideo: Boolean = false,
    val loadError: String? = null,
    val filmstrip: List<Bitmap> = emptyList(),
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val loopSegment: Boolean = true,
    val selectedAspect: AspectPreset = AspectPreset.SHORTS_9_16,
    val queuedSegments: List<QueuedSegment> = emptyList(),
    val selectedSegmentId: String? = null,
    val exportState: ExportProgressState = ExportProgressState(),
    val previewingClip: ClipEntity? = null,
    val clipThumbnails: Map<Long, Bitmap> = emptyMap(),
    val snackbarMessage: String? = null
)

class ClipForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = ClipRepository(
        context = appContext,
        clipDao = ClipForgeDatabase.getInstance(appContext).clipDao()
    )

    val exportedClips: StateFlow<List<ClipEntity>> = repository.exportedClips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val studioPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        })
    }

    private var exportJob: Job? = null

    init {
        startPlayheadTracker()
        observeClipThumbnails()
    }

    private fun startPlayheadTracker() {
        viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                if (state.sourceVideo != null && studioPlayer.playbackState != Player.STATE_IDLE) {
                    val currentPos = studioPlayer.currentPosition.coerceAtLeast(0L)
                    if (state.loopSegment && state.isPlaying && currentPos >= state.trimEndMs && state.trimEndMs > state.trimStartMs) {
                        studioPlayer.seekTo(state.trimStartMs)
                        _uiState.update { it.copy(playheadMs = state.trimStartMs) }
                    } else {
                        _uiState.update { it.copy(playheadMs = currentPos) }
                    }
                }
                delay(50L)
            }
        }
    }

    private fun observeClipThumbnails() {
        viewModelScope.launch {
            exportedClips.collect { clips ->
                val existing = _uiState.value.clipThumbnails
                val missing = clips.filter { !existing.containsKey(it.id) }
                if (missing.isNotEmpty()) {
                    val loaded = mutableMapOf<Long, Bitmap>()
                    for (clip in missing) {
                        VideoMetadataHelper.extractThumbnailForFile(
                            appContext,
                            clip.filePath,
                            clip.contentUri
                        )?.let { bmp ->
                            loaded[clip.id] = bmp
                        }
                    }
                    if (loaded.isNotEmpty()) {
                        _uiState.update { current ->
                            current.copy(clipThumbnails = current.clipThumbnails + loaded)
                        }
                    }
                }
            }
        }
    }

    fun selectTab(tab: ClipForgeTab) {
        if (tab != ClipForgeTab.STUDIO && studioPlayer.isPlaying) {
            studioPlayer.pause()
        }
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingVideo = true,
                    loadError = null,
                    queuedSegments = emptyList(),
                    selectedSegmentId = null
                )
            }
            val info = VideoMetadataHelper.loadVideoInfo(appContext, uri)
            if (info == null) {
                _uiState.update {
                    it.copy(
                        isLoadingVideo = false,
                        loadError = "Unable to read this video file. Please try another MP4 or MOV video."
                    )
                }
                return@launch
            }

            val defaultEnd = minOf(info.durationMs, 30_000L).coerceAtLeast(
                minOf(info.durationMs, 1_000L)
            )

            studioPlayer.setMediaItem(MediaItem.fromUri(uri))
            studioPlayer.prepare()
            studioPlayer.seekTo(0L)
            studioPlayer.playWhenReady = false

            _uiState.update {
                it.copy(
                    sourceVideo = info,
                    isLoadingVideo = false,
                    trimStartMs = 0L,
                    trimEndMs = defaultEnd,
                    playheadMs = 0L,
                    filmstrip = emptyList()
                )
            }

            val frames = VideoMetadataHelper.extractFilmstrip(appContext, uri, info.durationMs, 12)
            _uiState.update { it.copy(filmstrip = frames) }
        }
    }

    fun clearSourceVideo() {
        studioPlayer.stop()
        studioPlayer.clearMediaItems()
        _uiState.update {
            it.copy(
                sourceVideo = null,
                filmstrip = emptyList(),
                queuedSegments = emptyList(),
                selectedSegmentId = null,
                trimStartMs = 0L,
                trimEndMs = 0L,
                playheadMs = 0L
            )
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.sourceVideo == null) return
        if (studioPlayer.isPlaying) {
            studioPlayer.pause()
        } else {
            if (studioPlayer.currentPosition < state.trimStartMs ||
                studioPlayer.currentPosition >= state.trimEndMs
            ) {
                studioPlayer.seekTo(state.trimStartMs)
            }
            studioPlayer.play()
        }
    }

    fun pauseStudioPlayer() {
        if (studioPlayer.isPlaying) {
            studioPlayer.pause()
        }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.isMuted
        studioPlayer.volume = if (nextMuted) 0f else 1f
        _uiState.update { it.copy(isMuted = nextMuted) }
    }

    fun toggleLoopSegment() {
        _uiState.update { it.copy(loopSegment = !it.loopSegment) }
    }

    fun seekTo(positionMs: Long) {
        val duration = _uiState.value.sourceVideo?.durationMs ?: return
        val clamped = positionMs.coerceIn(0L, duration)
        studioPlayer.seekTo(clamped)
        _uiState.update { it.copy(playheadMs = clamped) }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val duration = _uiState.value.sourceVideo?.durationMs ?: return
        val minGap = minOf(1_000L, duration)
        val clampedStart = startMs.coerceIn(0L, (duration - minGap).coerceAtLeast(0L))
        val clampedEnd = endMs.coerceIn(clampedStart + minGap, duration)
        val newPlayhead = _uiState.value.playheadMs.coerceIn(clampedStart, clampedEnd)

        _uiState.update {
            it.copy(
                trimStartMs = clampedStart,
                trimEndMs = clampedEnd,
                playheadMs = newPlayhead,
                selectedSegmentId = null
            )
        }
    }

    fun applyDurationPreset(seconds: Int) {
        val duration = _uiState.value.sourceVideo?.durationMs ?: return
        val targetLen = (seconds * 1000L).coerceAtMost(duration)
        var start = _uiState.value.trimStartMs
        var end = start + targetLen
        if (end > duration) {
            end = duration
            start = (duration - targetLen).coerceAtLeast(0L)
        }
        studioPlayer.seekTo(start)
        _uiState.update {
            it.copy(
                trimStartMs = start,
                trimEndMs = end,
                playheadMs = start,
                selectedSegmentId = null
            )
        }
    }

    fun selectAspectPreset(preset: AspectPreset) {
        _uiState.update { it.copy(selectedAspect = preset) }
    }

    fun addCurrentRangeToQueue(customLabel: String? = null) {
        val state = _uiState.value
        if (state.sourceVideo == null) return
        val nextIndex = state.queuedSegments.size + 1
        val label = customLabel?.trim()?.takeIf { it.isNotEmpty() } ?: "Clip $nextIndex"
        val segment = QueuedSegment(
            label = label,
            startMs = state.trimStartMs,
            endMs = state.trimEndMs,
            aspectPreset = state.selectedAspect
        )
        _uiState.update {
            it.copy(
                queuedSegments = it.queuedSegments + segment,
                selectedSegmentId = segment.id,
                snackbarMessage = "Added \"$label\" (${(segment.durationMs / 1000.0).let { s -> String.format("%.1fs", s) }}) to queue"
            )
        }
    }

    fun autoSplitIntoShorts(chunkSeconds: Int) {
        val state = _uiState.value
        val source = state.sourceVideo ?: return
        val chunkMs = (chunkSeconds * 1000L).coerceAtLeast(3_000L)
        val generated = mutableListOf<QueuedSegment>()
        var cursor = 0L
        var part = 1
        while (cursor < source.durationMs) {
            val end = minOf(cursor + chunkMs, source.durationMs)
            val remainingAfter = source.durationMs - end
            // If trailing remainder is < 2 seconds, merge it into this last part
            val actualEnd = if (remainingAfter in 1L..1_999L) source.durationMs else end
            if (actualEnd - cursor >= 1_000L) {
                generated.add(
                    QueuedSegment(
                        label = "Part $part",
                        startMs = cursor,
                        endMs = actualEnd,
                        aspectPreset = state.selectedAspect
                    )
                )
            }
            cursor = actualEnd
            part++
        }

        if (generated.isNotEmpty()) {
            val first = generated.first()
            studioPlayer.seekTo(first.startMs)
            _uiState.update {
                it.copy(
                    queuedSegments = generated,
                    selectedSegmentId = first.id,
                    trimStartMs = first.startMs,
                    trimEndMs = first.endMs,
                    playheadMs = first.startMs,
                    snackbarMessage = "Auto-split into ${generated.size} ${chunkSeconds}s shorts"
                )
            }
        }
    }

    fun selectQueuedSegment(segment: QueuedSegment) {
        studioPlayer.seekTo(segment.startMs)
        _uiState.update {
            it.copy(
                selectedSegmentId = segment.id,
                trimStartMs = segment.startMs,
                trimEndMs = segment.endMs,
                playheadMs = segment.startMs,
                selectedAspect = segment.aspectPreset
            )
        }
    }

    fun removeQueuedSegment(segmentId: String) {
        _uiState.update { state ->
            state.copy(
                queuedSegments = state.queuedSegments.filterNot { it.id == segmentId },
                selectedSegmentId = if (state.selectedSegmentId == segmentId) null else state.selectedSegmentId
            )
        }
    }

    fun clearQueue() {
        _uiState.update { it.copy(queuedSegments = emptyList(), selectedSegmentId = null) }
    }

    fun exportCurrentSelection(customTitle: String? = null) {
        val state = _uiState.value
        val source = state.sourceVideo ?: return
        val label = customTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: "${source.title.take(18)} Short"
        val singleSegment = QueuedSegment(
            label = label,
            startMs = state.trimStartMs,
            endMs = state.trimEndMs,
            aspectPreset = state.selectedAspect
        )
        runExportBatch(source, listOf(singleSegment))
    }

    fun exportAllQueued() {
        val state = _uiState.value
        val source = state.sourceVideo ?: return
        if (state.queuedSegments.isEmpty()) {
            exportCurrentSelection()
            return
        }
        runExportBatch(source, state.queuedSegments)
    }

    private fun runExportBatch(source: SourceVideoInfo, segments: List<QueuedSegment>) {
        if (_uiState.value.exportState.isExporting) return
        pauseStudioPlayer()

        exportJob = viewModelScope.launch {
            var exportedCount = 0
            try {
                segments.forEachIndexed { index, segment ->
                    _uiState.update {
                        it.copy(
                            exportState = ExportProgressState(
                                isExporting = true,
                                currentIndex = index + 1,
                                totalCount = segments.size,
                                currentLabel = segment.label,
                                progressPercent = 0
                            )
                        )
                    }

                    val clipEntity = VideoClipExporter.exportClip(
                        context = appContext,
                        sourceUri = source.uri,
                        sourceTitle = source.title,
                        segment = segment,
                        onProgress = { pct ->
                            _uiState.update { state ->
                                state.copy(
                                    exportState = state.exportState.copy(progressPercent = pct)
                                )
                            }
                        }
                    )
                    repository.saveClip(clipEntity)
                    exportedCount++
                }

                _uiState.update {
                    it.copy(
                        exportState = ExportProgressState(),
                        activeTab = ClipForgeTab.MY_CLIPS,
                        snackbarMessage = if (exportedCount == 1) {
                            "Clip exported to Movies/ClipForge & ready to share!"
                        } else {
                            "Exported $exportedCount shorts to Movies/ClipForge!"
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                _uiState.update {
                    it.copy(
                        exportState = ExportProgressState(),
                        snackbarMessage = "Export cancelled"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        exportState = ExportProgressState(
                            isExporting = false,
                            errorMessage = e.localizedMessage ?: "Export failed for this video codec."
                        )
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
    }

    fun dismissExportError() {
        _uiState.update { it.copy(exportState = ExportProgressState()) }
    }

    fun openClipPreview(clip: ClipEntity) {
        pauseStudioPlayer()
        _uiState.update { it.copy(previewingClip = clip) }
    }

    fun closeClipPreview() {
        _uiState.update { it.copy(previewingClip = null) }
    }

    fun shareClip(clip: ClipEntity) {
        val shareUri = repository.resolveShareUri(clip)
        VideoClipExporter.launchShareIntent(appContext, shareUri, clip.title)
    }

    fun resolvePlayableUri(clip: ClipEntity): Uri = repository.resolvePlayableUri(clip)

    fun deleteClip(clip: ClipEntity) {
        viewModelScope.launch {
            repository.deleteClip(clip)
            _uiState.update { state ->
                state.copy(
                    previewingClip = if (state.previewingClip?.id == clip.id) null else state.previewingClip,
                    clipThumbnails = state.clipThumbnails - clip.id,
                    snackbarMessage = "Deleted \"${clip.title}\""
                )
            }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        exportJob?.cancel()
        studioPlayer.release()
    }
}
