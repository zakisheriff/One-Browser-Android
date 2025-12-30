package com.oneatom.onebrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerControls(
        modifier: Modifier = Modifier,
        isPlaying: Boolean,
        currentTime: Float, // In seconds
        duration: Float, // In seconds
        title: String? = null,
        onPlayPause: () -> Unit,
        onSeek: (Float) -> Unit,
        onClose: () -> Unit,
        onRotate: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    // Auto-hide controls
    LaunchedEffect(isVisible, isPlaying, isSeeking) {
        if (isVisible && isPlaying && !isSeeking) {
            delay(3000)
            isVisible = false
        }
    }

    // Sync seek value with current time when not seeking
    LaunchedEffect(currentTime) {
        if (!isSeeking) {
            seekValue = currentTime
        }
    }

    Box(
            modifier =
                    modifier.fillMaxSize().clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                            ) { isVisible = !isVisible }
    ) {
        AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                        modifier =
                                Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = title ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onClose) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                        )
                    }
                }

                // Center (Play/Pause)
                Box(modifier = Modifier.align(Alignment.Center)) {
                    IconButton(
                            onClick = onPlayPause,
                            modifier =
                                    Modifier.size(64.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                                imageVector =
                                        if (isPlaying) Icons.Default.Pause
                                        else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Bottom Bar
                Column(
                        modifier =
                                Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    // Time Labels & Slider
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                text = formatTime(if (isSeeking) seekValue else currentTime),
                                color = Color.White,
                                fontSize = 12.sp
                        )

                        Slider(
                                value = if (isSeeking) seekValue else currentTime,
                                onValueChange = {
                                    isSeeking = true
                                    seekValue = it
                                    isVisible = true // vector interaction keeps controls alive
                                },
                                onValueChangeFinished = {
                                    isSeeking = false
                                    onSeek(seekValue)
                                },
                                valueRange =
                                        0f..(if (duration > 0) duration
                                                else 100f), // Fallback to 100 if unknown
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors =
                                        SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color.Red,
                                                inactiveTrackColor = Color.Gray
                                        )
                        )

                        Text(text = formatTime(duration), color = Color.White, fontSize = 12.sp)
                    }

                    // Bottom Actions (Rotate)
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onRotate) {
                            Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate",
                                    tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    if (seconds.isNaN() || seconds < 0) return "00:00"
    val minutes = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return String.format("%02d:%02d", minutes, secs)
}
