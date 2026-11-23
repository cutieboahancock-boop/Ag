package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.screens.ClipsLibraryScreen
import com.example.ui.screens.StudioScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipForgeApp(
    viewModel: ClipForgeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportedClips by viewModel.exportedClips.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    if (uiState.activeTab == ClipForgeTab.MY_CLIPS && uiState.previewingClip == null) {
        BackHandler {
            viewModel.selectTab(ClipForgeTab.STUDIO)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = uiState.activeTab == ClipForgeTab.STUDIO,
                            onClick = { viewModel.selectTab(ClipForgeTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.activeTab == ClipForgeTab.STUDIO) {
                                        Icons.Filled.ContentCut
                                    } else {
                                        Icons.Outlined.ContentCut
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            modifier = Modifier.testTag("nav_tab_studio")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == ClipForgeTab.MY_CLIPS,
                            onClick = { viewModel.selectTab(ClipForgeTab.MY_CLIPS) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (exportedClips.isNotEmpty()) {
                                            Badge { Text("${exportedClips.size}") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (uiState.activeTab == ClipForgeTab.MY_CLIPS) {
                                            Icons.Filled.Movie
                                        } else {
                                            Icons.Outlined.Movie
                                        },
                                        contentDescription = null
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.nav_my_clips)) },
                            modifier = Modifier.testTag("nav_tab_my_clips")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.weight(0.1f))
                        NavigationRailItem(
                            selected = uiState.activeTab == ClipForgeTab.STUDIO,
                            onClick = { viewModel.selectTab(ClipForgeTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.activeTab == ClipForgeTab.STUDIO) {
                                        Icons.Filled.ContentCut
                                    } else {
                                        Icons.Outlined.ContentCut
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            modifier = Modifier.testTag("rail_tab_studio")
                        )
                        NavigationRailItem(
                            selected = uiState.activeTab == ClipForgeTab.MY_CLIPS,
                            onClick = { viewModel.selectTab(ClipForgeTab.MY_CLIPS) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (exportedClips.isNotEmpty()) {
                                            Badge { Text("${exportedClips.size}") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (uiState.activeTab == ClipForgeTab.MY_CLIPS) {
                                            Icons.Filled.Movie
                                        } else {
                                            Icons.Outlined.Movie
                                        },
                                        contentDescription = null
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.nav_my_clips)) },
                            modifier = Modifier.testTag("rail_tab_my_clips")
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (uiState.activeTab) {
                        ClipForgeTab.STUDIO -> StudioScreen(
                            uiState = uiState,
                            player = viewModel.studioPlayer,
                            onVideoPicked = viewModel::onVideoSelected,
                            onClearVideo = viewModel::clearSourceVideo,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onToggleMute = viewModel::toggleMute,
                            onToggleLoop = viewModel::toggleLoopSegment,
                            onSeek = viewModel::seekTo,
                            onTrimChanged = viewModel::updateTrimRange,
                            onApplyDurationPreset = viewModel::applyDurationPreset,
                            onSelectAspect = viewModel::selectAspectPreset,
                            onAddToQueue = viewModel::addCurrentRangeToQueue,
                            onAutoSplit = viewModel::autoSplitIntoShorts,
                            onSelectQueuedSegment = viewModel::selectQueuedSegment,
                            onRemoveQueuedSegment = viewModel::removeQueuedSegment,
                            onClearQueue = viewModel::clearQueue,
                            onExportCurrent = { viewModel.exportCurrentSelection() },
                            onExportAllQueued = viewModel::exportAllQueued,
                            onCancelExport = viewModel::cancelExport,
                            onDismissExportError = viewModel::dismissExportError
                        )

                        ClipForgeTab.MY_CLIPS -> ClipsLibraryScreen(
                            clips = exportedClips,
                            thumbnails = uiState.clipThumbnails,
                            previewingClip = uiState.previewingClip,
                            resolvePlayableUri = viewModel::resolvePlayableUri,
                            onPlayClip = viewModel::openClipPreview,
                            onClosePreview = viewModel::closeClipPreview,
                            onShareClip = viewModel::shareClip,
                            onDeleteClip = viewModel::deleteClip,
                            onOpenStudio = { viewModel.selectTab(ClipForgeTab.STUDIO) }
                        )
                    }
                }
            }
        }
    }
}
