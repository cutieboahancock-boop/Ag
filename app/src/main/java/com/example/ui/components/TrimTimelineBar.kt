package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.media.QueuedSegment
import com.example.media.formatTimecode
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TimecodeTextStyle
import kotlin.math.roundToInt

@Composable
fun TrimTimelineBar(
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    playheadMs: Long,
    filmstrip: List<Bitmap>,
    queuedSegments: List<QueuedSegment>,
    onTrimChanged: (Long, Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val currentStart by rememberUpdatedState(trimStartMs)
    val currentEnd by rememberUpdatedState(trimEndMs)
    val currentDuration by rememberUpdatedState(safeDuration)
    val onTrimUpdate by rememberUpdatedState(onTrimChanged)
    val onSeekUpdate by rememberUpdatedState(onSeek)

    val handleVisualWidth = 18.dp
    val handleTouchWidth = 48.dp

    Column(modifier = modifier.fillMaxWidth()) {
        // Timecodes header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IN ${formatTimecode(trimStartMs)}",
                style = TimecodeTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "CLIP ${formatTimecode(trimEndMs - trimStartMs)}",
                    style = TimecodeTextStyle,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "OUT ${formatTimecode(trimEndMs)}",
                style = TimecodeTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .testTag("trim_timeline_bar")
        ) {
            val density = LocalDensity.current
            val handleVisualPx = with(density) { handleVisualWidth.toPx() }
            val handleTouchPx = with(density) { handleTouchWidth.toPx() }
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val trackWidthPx = (totalWidthPx - handleVisualPx * 2f).coerceAtLeast(1f)

            val startFraction = (trimStartMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            val endFraction = (trimEndMs.toFloat() / safeDuration.toFloat()).coerceIn(startFraction, 1f)
            val playheadFraction = (playheadMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

            val startXPx = handleVisualPx + startFraction * trackWidthPx
            val endXPx = handleVisualPx + endFraction * trackWidthPx
            val playheadXPx = handleVisualPx + playheadFraction * trackWidthPx

            // Background filmstrip track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = handleVisualWidth)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(safeDuration, trackWidthPx) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            onSeekUpdate((frac * currentDuration).toLong())
                        }
                    }
                    .pointerInput(safeDuration, trackWidthPx) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                            onSeekUpdate((frac * currentDuration).toLong())
                        }
                    }
            ) {
                if (filmstrip.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        filmstrip.forEach { frame ->
                            Image(
                                bitmap = frame.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                // Queued clip markers strip at top edge of filmstrip
                queuedSegments.forEach { seg ->
                    val segStartFrac = (seg.startMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    val segEndFrac = (seg.endMs.toFloat() / safeDuration.toFloat()).coerceIn(segStartFrac, 1f)
                    val segStartDp = with(density) { (segStartFrac * trackWidthPx).toDp() }
                    val segWidthDp = with(density) { ((segEndFrac - segStartFrac) * trackWidthPx).coerceAtLeast(4f).toDp() }
                    Box(
                        modifier = Modifier
                            .offset(x = segStartDp)
                            .width(segWidthDp)
                            .height(5.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f))
                    )
                }

                // Dimmed Left Region
                val leftDimWidthDp = with(density) { (startFraction * trackWidthPx).toDp() }
                if (leftDimWidthDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .width(leftDimWidthDp)
                            .fillMaxHeight()
                            .background(StudioObsidian.copy(alpha = 0.68f))
                    )
                }

                // Dimmed Right Region
                val rightDimOffsetDp = with(density) { (endFraction * trackWidthPx).toDp() }
                val rightDimWidthDp = with(density) { ((1f - endFraction) * trackWidthPx).toDp() }
                if (rightDimWidthDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = rightDimOffsetDp)
                            .width(rightDimWidthDp)
                            .fillMaxHeight()
                            .background(StudioObsidian.copy(alpha = 0.68f))
                    )
                }
            }

            // Active Range Top/Bottom Highlight Frame
            val activeWidthDp = with(density) { (endXPx - startXPx).coerceAtLeast(0f).toDp() }
            val activeStartDp = with(density) { startXPx.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = activeStartDp)
                    .width(activeWidthDp)
                    .fillMaxHeight()
                    .border(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
            )

            // Playhead needle
            Box(
                modifier = Modifier
                    .offset { IntOffset((playheadXPx - 1.5.dp.toPx()).roundToInt(), 0) }
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )

            // Left Trim Handle (48dp wide interactive target)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (startXPx - handleVisualPx - (handleTouchPx - handleVisualPx) / 2f).roundToInt(),
                            0
                        )
                    }
                    .width(handleTouchWidth)
                    .fillMaxHeight()
                    .pointerInput(trackWidthPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = ((dragAmount.x / trackWidthPx) * currentDuration).toLong()
                            val nextStart = currentStart + deltaMs
                            onTrimUpdate(nextStart, currentEnd)
                            onSeekUpdate(nextStart.coerceIn(0L, currentEnd - 500L))
                        }
                    }
                    .testTag("trim_start_handle"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(handleVisualWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 22.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            }

            // Right Trim Handle (48dp wide interactive target)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (endXPx - (handleTouchPx - handleVisualPx) / 2f).roundToInt(),
                            0
                        )
                    }
                    .width(handleTouchWidth)
                    .fillMaxHeight()
                    .pointerInput(trackWidthPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = ((dragAmount.x / trackWidthPx) * currentDuration).toLong()
                            val nextEnd = currentEnd + deltaMs
                            onTrimUpdate(currentStart, nextEnd)
                            onSeekUpdate(nextEnd.coerceIn(currentStart + 500L, currentDuration))
                        }
                    }
                    .testTag("trim_end_handle"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(handleVisualWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 22.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
