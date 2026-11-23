package com.example.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.media.AspectPreset
import com.example.media.formatTimecode
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TimecodeTextStyle

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerPreview(
    player: ExoPlayer,
    sourceAspectRatio: Float,
    selectedAspect: AspectPreset,
    isPlaying: Boolean,
    isMuted: Boolean,
    loopSegment: Boolean,
    playheadMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetRatio = (selectedAspect.ratio ?: sourceAspectRatio).coerceIn(0.45f, 2.2f)
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 220),
        label = "preview_aspect_ratio"
    )
    val clipDuration = (trimEndMs - trimStartMs).coerceAtLeast(0L)
    val relativePos = (playheadMs - trimStartMs).coerceIn(0L, clipDuration)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 210.dp, max = 320.dp)
            .clip(MaterialTheme.shapes.large)
            .background(StudioObsidian)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.large
            ),
        contentAlignment = Alignment.Center
    ) {
        // Live cropped preview viewport matching selected Shorts/Feed aspect ratio
        Box(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 14.dp)
                .aspectRatio(animatedRatio, matchHeightConstraintsFirst = animatedRatio < 1f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onTogglePlayPause)
                .testTag("video_preview_surface"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        this.player = player
                    }
                },
                update = { view ->
                    if (view.player !== player) {
                        view.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Center Play/Pause Affordance
            Surface(
                shape = CircleShape,
                color = StudioObsidian.copy(alpha = if (isPlaying) 0.35f else 0.72f),
                modifier = Modifier
                    .size(52.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Top-left live framing tag
        Surface(
            color = StudioObsidian.copy(alpha = 0.78f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "${selectedAspect.label} Frame",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Top-right Loop & Mute controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (loopSegment) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                } else {
                    StudioObsidian.copy(alpha = 0.75f)
                },
                shape = CircleShape,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onToggleLoop)
                    .testTag("loop_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = stringResource(R.string.cd_loop_toggle),
                        tint = if (loopSegment) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (loopSegment) "Loop" else "Once",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (loopSegment) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .background(StudioObsidian.copy(alpha = 0.75f), CircleShape)
                    .testTag("mute_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.cd_mute_toggle),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom-left clip timecode readout
        Surface(
            color = StudioObsidian.copy(alpha = 0.82f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = "${formatTimecode(relativePos)} / ${formatTimecode(clipDuration)}",
                style = TimecodeTextStyle,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}
