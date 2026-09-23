package com.mousecontrol.remote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A vertical 0-100 slider for the far right edge. Fades out while the user
 * is actively clicking/dragging on the trackpad, and back in when the user
 * touches this edge strip again — the touch target itself stays live even
 * while fully faded, so the very first tap on the edge both reveals it and
 * starts adjusting the value.
 */
@Composable
fun VerticalEdgeSlider(
    value: Int,
    icon: ImageVector,
    visible: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "sliderFade")

    Box(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight(0.65f)
            .alpha(alpha)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val ratio = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange((ratio * 100).roundToInt().coerceIn(0, 100))
                }
            }
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.5f)
                .fillMaxHeight(value / 100f)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null)
            Text("$value", style = MaterialTheme.typography.labelSmall)
        }
    }
}
