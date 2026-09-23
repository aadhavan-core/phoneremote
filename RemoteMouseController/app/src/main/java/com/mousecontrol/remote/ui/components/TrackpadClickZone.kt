package com.mousecontrol.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private const val LONG_PRESS_MS = 280L
private const val DOUBLE_TAP_MS = 260L

/**
 * A click zone that mirrors a physical trackpad button: a quick tap sends a
 * click, a quick second tap sends a double-click, and a press-and-hold lets
 * the user drag the cursor while the button stays down (drag-lock) — the
 * same "exact standard actions" a laptop trackpad's corner button supports.
 */
@Composable
fun TrackpadClickZone(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .pointerInput(Unit) {
                var lastTapTime = 0L
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val downTime = System.currentTimeMillis()
                    var dragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val elapsed = System.currentTimeMillis() - downTime

                        if (!dragging && elapsed > LONG_PRESS_MS) {
                            dragging = true
                            onDragStart()
                        }
                        if (dragging) {
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            if (dx != 0f || dy != 0f) onDrag(dx, dy)
                        }
                        if (!change.pressed) {
                            if (dragging) {
                                onDragEnd()
                            } else {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime < DOUBLE_TAP_MS) {
                                    onDoubleClick()
                                    lastTapTime = 0L
                                } else {
                                    onClick()
                                    lastTapTime = now
                                }
                            }
                            break
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}
