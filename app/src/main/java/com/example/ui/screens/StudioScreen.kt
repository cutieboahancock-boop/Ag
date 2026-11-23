package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.example.R
import com.example.media.AspectPreset
import com.example.media.QueuedSegment
import com.example.media.formatFileSize
import com.example.media.formatTimecode
import com.example.ui.StudioUiState
import com.example.ui.components.TrimTimelineBar
import com.example.ui.components.VideoPlayerPreview
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TimecodeTextStyle
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    uiState: StudioUiState,
    player: ExoPlayer,
    onVideoPicked: (Uri) -> Unit,
    onClearVideo: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleLoop: () -> Unit,
    onSeek: (Long) -> Unit,
    onTrimChanged: (Long, Long) -> Unit,
    onApplyDurationPreset: (Int) -> Unit,
    onSelectAspect: (AspectPreset) -> Unit,
    onAddToQueue: (String?) -> Unit,
    onAutoSplit: (Int) -> Unit,
    onSelectQueuedSegment: (QueuedSegment) -> Unit,
    onRemoveQueuedSegment: (String) -> Unit,
    onClearQueue: () -> Unit,
    onExportCurrent: () -> Unit,
    onExportAllQueued: () -> Unit,
    onCancelExport: () -> Unit,
    onDismissExportError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onVideoPicked(uri)
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onVideoPicked(uri)
    }

    var showAutoSplitSheet by remember { mutableStateOf(false) }
    var showAddClipDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        val source = uiState.sourceVideo
        if (source == null) {
            StudioEmptyState(
                isLoading = uiState.isLoadingVideo,
                errorMessage = uiState.loadError,
                onPickFromGallery = {
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onBrowseFiles = {
                    documentPicker.launch(arrayOf("video/*"))
                }
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 640.dp
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.05f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SourceHeaderCard(
                                title = source.title,
                                resolution = "${source.width}×${source.height}",
                                durationText = formatTimecode(source.durationMs, includeTenths = false),
                                sizeText = formatFileSize(source.fileSizeBytes),
                                onChangeVideo = {
                                    galleryPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                onCloseVideo = onClearVideo
                            )
                            VideoPlayerPreview(
                                player = player,
                                sourceAspectRatio = source.sourceAspectRatio,
                                selectedAspect = uiState.selectedAspect,
                                isPlaying = uiState.isPlaying,
                                isMuted = uiState.isMuted,
                                loopSegment = uiState.loopSegment,
                                playheadMs = uiState.playheadMs,
                                trimStartMs = uiState.trimStartMs,
                                trimEndMs = uiState.trimEndMs,
                                onTogglePlayPause = onTogglePlayPause,
                                onToggleMute = onToggleMute,
                                onToggleLoop = onToggleLoop
                            )
                            AspectRatioSelectorRow(
                                selected = uiState.selectedAspect,
                                onSelect = onSelectAspect
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            EditorControlsSection(
                                uiState = uiState,
                                onTrimChanged = onTrimChanged,
                                onSeek = onSeek,
                                onApplyDurationPreset = onApplyDurationPreset,
                                onOpenAutoSplit = { showAutoSplitSheet = true },
                                onOpenAddClip = { showAddClipDialog = true },
                                onSelectQueuedSegment = onSelectQueuedSegment,
                                onRemoveQueuedSegment = onRemoveQueuedSegment,
                                onClearQueue = onClearQueue,
                                onExportCurrent = onExportCurrent,
                                onExportAllQueued = onExportAllQueued
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SourceHeaderCard(
                            title = source.title,
                            resolution = "${source.width}×${source.height}",
                            durationText = formatTimecode(source.durationMs, includeTenths = false),
                            sizeText = formatFileSize(source.fileSizeBytes),
                            onChangeVideo = {
                                galleryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onCloseVideo = onClearVideo
                        )

                        VideoPlayerPreview(
                            player = player,
                            sourceAspectRatio = source.sourceAspectRatio,
                            selectedAspect = uiState.selectedAspect,
                            isPlaying = uiState.isPlaying,
                            isMuted = uiState.isMuted,
                            loopSegment = uiState.loopSegment,
                            playheadMs = uiState.playheadMs,
                            trimStartMs = uiState.trimStartMs,
                            trimEndMs = uiState.trimEndMs,
                            onTogglePlayPause = onTogglePlayPause,
                            onToggleMute = onToggleMute,
                            onToggleLoop = onToggleLoop
                        )

                        AspectRatioSelectorRow(
                            selected = uiState.selectedAspect,
                            onSelect = onSelectAspect
                        )

                        EditorControlsSection(
                            uiState = uiState,
                            onTrimChanged = onTrimChanged,
                            onSeek = onSeek,
                            onApplyDurationPreset = onApplyDurationPreset,
                            onOpenAutoSplit = { showAutoSplitSheet = true },
                            onOpenAddClip = { showAddClipDialog = true },
                            onSelectQueuedSegment = onSelectQueuedSegment,
                            onRemoveQueuedSegment = onRemoveQueuedSegment,
                            onClearQueue = onClearQueue,
                            onExportCurrent = onExportCurrent,
                            onExportAllQueued = onExportAllQueued
                        )

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        // Auto-Split Modal Sheet
        if (showAutoSplitSheet && source != null) {
            AutoSplitBottomSheet(
                sourceDurationMs = source.durationMs,
                selectedAspect = uiState.selectedAspect,
                onConfirmSplit = { chunkSeconds ->
                    onAutoSplit(chunkSeconds)
                    showAutoSplitSheet = false
                },
                onDismiss = { showAutoSplitSheet = false }
            )
        }

        // Add Clip to Queue Dialog
        if (showAddClipDialog) {
            AddClipLabelDialog(
                defaultLabel = "Clip ${uiState.queuedSegments.size + 1}",
                durationMs = uiState.trimEndMs - uiState.trimStartMs,
                aspectLabel = uiState.selectedAspect.label,
                onConfirm = { label ->
                    onAddToQueue(label)
                    showAddClipDialog = false
                },
                onDismiss = { showAddClipDialog = false }
            )
        }

        // Active Export Progress Modal
        if (uiState.exportState.isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioObsidian.copy(alpha = 0.78f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 420.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (uiState.exportState.totalCount > 1) {
                                "Exporting Short ${uiState.exportState.currentIndex} of ${uiState.exportState.totalCount}"
                            } else {
                                "Rendering Shareable Short"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.exportState.currentLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { uiState.exportState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${uiState.exportState.progressPercent}%",
                            style = TimecodeTextStyle,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        TextButton(
                            onClick = onCancelExport,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cancel_export_button")
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }
        }

        // Export Error Dialog
        uiState.exportState.errorMessage?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = onDismissExportError,
                title = { Text("Export Issue") },
                text = { Text(errorMsg) },
                confirmButton = {
                    Button(onClick = onDismissExportError) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun StudioEmptyState(
    isLoading: Boolean,
    errorMessage: String?,
    onPickFromGallery: () -> Unit,
    onBrowseFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Art Card with Editorial Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.large)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.large
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = stringResource(R.string.cd_hero_illustration),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    StudioObsidian.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "SHORTS & REELS CLIPPER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }

            Text(
                text = stringResource(R.string.hero_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Inspecting video frames & duration...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPickFromGallery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("pick_gallery_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.action_pick_gallery),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onBrowseFiles,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("browse_files_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.action_browse_files),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // 3-Step Workflow Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorkflowMiniCard(
                    icon = Icons.Default.ContentCut,
                    title = "Frame Trim",
                    subtitle = "Drag handles & 9:16 crop",
                    modifier = Modifier.weight(1f)
                )
                WorkflowMiniCard(
                    icon = Icons.Default.AutoAwesomeMosaic,
                    title = "Auto-Split",
                    subtitle = "Slice into 15–60s parts",
                    modifier = Modifier.weight(1f)
                )
                WorkflowMiniCard(
                    icon = Icons.Default.Share,
                    title = "Share Fast",
                    subtitle = "Save MP4 & send anywhere",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WorkflowMiniCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SourceHeaderCard(
    title: String,
    resolution: String,
    durationText: String,
    sizeText: String,
    onChangeVideo: () -> Unit,
    onCloseVideo: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$durationText • $resolution • $sizeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onChangeVideo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("change_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.action_change_video)
                    )
                }
                IconButton(
                    onClick = onCloseVideo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close source video"
                    )
                }
            }
        }
    }
}

@Composable
private fun AspectRatioSelectorRow(
    selected: AspectPreset,
    onSelect: (AspectPreset) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(AspectPreset.entries) { preset ->
            val icon = when (preset) {
                AspectPreset.SHORTS_9_16, AspectPreset.PORTRAIT_4_5 -> Icons.Default.CropPortrait
                AspectPreset.SQUARE_1_1 -> Icons.Default.CropSquare
                AspectPreset.ORIGINAL -> Icons.Default.Crop169
            }
            val isSelected = selected == preset
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(preset) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = if (preset == AspectPreset.SHORTS_9_16) "9:16 Shorts" else preset.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("aspect_chip_${preset.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun EditorControlsSection(
    uiState: StudioUiState,
    onTrimChanged: (Long, Long) -> Unit,
    onSeek: (Long) -> Unit,
    onApplyDurationPreset: (Int) -> Unit,
    onOpenAutoSplit: () -> Unit,
    onOpenAddClip: () -> Unit,
    onSelectQueuedSegment: (QueuedSegment) -> Unit,
    onRemoveQueuedSegment: (String) -> Unit,
    onClearQueue: () -> Unit,
    onExportCurrent: () -> Unit,
    onExportAllQueued: () -> Unit
) {
    val source = uiState.sourceVideo ?: return

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrimTimelineBar(
                durationMs = source.durationMs,
                trimStartMs = uiState.trimStartMs,
                trimEndMs = uiState.trimEndMs,
                playheadMs = uiState.playheadMs,
                filmstrip = uiState.filmstrip,
                queuedSegments = uiState.queuedSegments,
                onTrimChanged = onTrimChanged,
                onSeek = onSeek
            )

            // Quick Length Presets + Auto-Split + Add Clip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(listOf(15, 30, 60)) { seconds ->
                    AssistChip(
                        onClick = { onApplyDurationPreset(seconds) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("${seconds}s") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("preset_${seconds}s_chip")
                    )
                }

                item {
                    FilledTonalButton(
                        onClick = onOpenAutoSplit,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("auto_split_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesomeMosaic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_auto_split))
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = onOpenAddClip,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_clip_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_add_to_queue))
                    }
                }
            }
        }
    }

    // Queued Shorts List
    AnimatedVisibility(visible = uiState.queuedSegments.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clip Queue (${uiState.queuedSegments.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = onClearQueue,
                        modifier = Modifier.testTag("clear_queue_button")
                    ) {
                        Text("Clear")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.queuedSegments, key = { it.id }) { segment ->
                        val isSelected = uiState.selectedSegmentId == segment.id
                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSelectQueuedSegment(segment) }
                                .testTag("queued_segment_${segment.label}")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = segment.label,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${formatTimecode(segment.startMs, false)}–${formatTimecode(segment.endMs, false)} • ${segment.aspectPreset.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveQueuedSegment(segment.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove ${segment.label}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Action Bar
    val currentClipSeconds = ((uiState.trimEndMs - uiState.trimStartMs) / 1000.0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (uiState.queuedSegments.isNotEmpty()) {
            OutlinedButton(
                onClick = onExportCurrent,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("export_current_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Export 1 (${String.format("%.0fs", currentClipSeconds)})")
            }
            Button(
                onClick = onExportAllQueued,
                modifier = Modifier
                    .weight(1.3f)
                    .height(52.dp)
                    .testTag("export_all_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.MovieCreation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_export_all, uiState.queuedSegments.size))
            }
        } else {
            Button(
                onClick = onExportCurrent,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("export_single_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.MovieCreation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Export Short (${uiState.selectedAspect.label} • ${String.format("%.1fs", currentClipSeconds)})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSplitBottomSheet(
    sourceDurationMs: Long,
    selectedAspect: AspectPreset,
    onConfirmSplit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedChunkSec by remember { mutableIntStateOf(30) }
    val options = listOf(15, 30, 45, 60)
    val estimatedParts = ceil(sourceDurationMs.toDouble() / (selectedChunkSec * 1000.0)).toInt().coerceAtLeast(1)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Auto-Split Video Into Shorts",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Automatically slice your ${formatTimecode(sourceDurationMs, false)} video into consecutive ${selectedAspect.label} clips.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { sec ->
                    val chosen = selectedChunkSec == sec
                    FilterChip(
                        selected = chosen,
                        onClick = { selectedChunkSec = sec },
                        label = { Text("${sec}s") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("split_option_${sec}s")
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Creates $estimatedParts ${if (estimatedParts == 1) "part" else "parts"} (${selectedChunkSec}s max each) in ${selectedAspect.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { onConfirmSplit(selectedChunkSec) },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("confirm_auto_split_button")
                ) {
                    Text("Create $estimatedParts Parts")
                }
            }
        }
    }
}

@Composable
private fun AddClipLabelDialog(
    defaultLabel: String,
    durationMs: Long,
    aspectLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(defaultLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Clip to Queue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Duration: ${formatTimecode(durationMs)} • Format: $aspectLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Clip Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clip_label_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label) },
                modifier = Modifier.testTag("confirm_add_clip_button")
            ) {
                Text("Queue Clip")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
