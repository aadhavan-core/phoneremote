package com.mousecontrol.remote.ui.screens

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mousecontrol.remote.network.ConnectionStatus
import com.mousecontrol.remote.network.MouseButton
import com.mousecontrol.remote.ui.components.TrackpadClickZone
import com.mousecontrol.remote.ui.components.VerticalEdgeSlider
import com.mousecontrol.remote.viewmodel.AppViewModel
import com.mousecontrol.remote.viewmodel.ControlMode

private const val TRACKPAD_SENSITIVITY = 1.6f

@Composable
fun ControlScreen(viewModel: AppViewModel, onDisconnected: () -> Unit) {
    val mode by viewModel.mode.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    var slidersVisible by remember { mutableStateOf(true) }

    LaunchedEffect(status) {
        if (status == ConnectionStatus.DISCONNECTED || status == ConnectionStatus.FAILED) onDisconnected()
    }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            TrackpadClickZone(
                label = "LEFT CLICK",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.click(MouseButton.LEFT) },
                onDoubleClick = { viewModel.doubleClick(MouseButton.LEFT) },
                onDragStart = {
                    slidersVisible = false
                    viewModel.buttonDown(MouseButton.LEFT)
                },
                onDrag = { dx, dy ->
                    viewModel.sendTrackpadDelta(dx * TRACKPAD_SENSITIVITY, dy * TRACKPAD_SENSITIVITY)
                },
                onDragEnd = { viewModel.buttonUp(MouseButton.LEFT) }
            )

            // Center: kept empty per spec, apart from the mode toggle and,
            // in Trackpad Mode, the swipe surface for cursor movement.
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
                    .then(
                        if (mode == ControlMode.TRACKPAD) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { slidersVisible = false },
                                    onDragEnd = { slidersVisible = true },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        viewModel.sendTrackpadDelta(
                                            dragAmount.x * TRACKPAD_SENSITIVITY,
                                            dragAmount.y * TRACKPAD_SENSITIVITY
                                        )
                                    }
                                )
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    ModeToggle(mode = mode, onModeChange = { viewModel.setMode(it) })
                }
            }

            TrackpadClickZone(
                label = "RIGHT CLICK",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.click(MouseButton.RIGHT) },
                onDoubleClick = { viewModel.doubleClick(MouseButton.RIGHT) },
                onDragStart = {
                    slidersVisible = false
                    viewModel.buttonDown(MouseButton.RIGHT)
                },
                onDrag = { dx, dy ->
                    viewModel.sendTrackpadDelta(dx * TRACKPAD_SENSITIVITY, dy * TRACKPAD_SENSITIVITY)
                },
                onDragEnd = { viewModel.buttonUp(MouseButton.RIGHT) }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 8.dp)
                .pointerInput(Unit) {
                    // Touching this edge always reveals the sliders, even mid-fade.
                    detectDragGestures(
                        onDragStart = { slidersVisible = true },
                        onDrag = { change, _ -> change.consume() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerticalEdgeSlider(
                value = brightness,
                icon = Icons.Default.Brightness6,
                visible = slidersVisible,
                onValueChange = { viewModel.setBrightness(it) },
                modifier = Modifier.padding(end = 6.dp)
            )
            VerticalEdgeSlider(
                value = volume,
                icon = Icons.Default.VolumeUp,
                visible = slidersVisible,
                onValueChange = { viewModel.setVolume(it) }
            )
        }
    }
}

@Composable
private fun ModeToggle(mode: ControlMode, onModeChange: (ControlMode) -> Unit) {
    val options = ControlMode.entries
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(if (option == ControlMode.GYROSCOPE) "Gyroscopic" else "Trackpad") }
            )
        }
    }
}
