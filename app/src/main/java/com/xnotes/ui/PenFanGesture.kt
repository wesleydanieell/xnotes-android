package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import com.xnotes.settings.ToolbarPosition
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Semicircular quick-select layout, opening away from the toolbar's docked edge. */
internal enum class PenFanDirection { DOWN, UP, RIGHT, LEFT }

internal fun ToolbarPosition.penFanDirection(): PenFanDirection = when (this) {
    ToolbarPosition.TOP -> PenFanDirection.DOWN
    ToolbarPosition.BOTTOM -> PenFanDirection.UP
    ToolbarPosition.LEFT -> PenFanDirection.RIGHT
    ToolbarPosition.RIGHT -> PenFanDirection.LEFT
}

/** Position of an item relative to the launcher, in the same units as [radius]. */
internal fun penFanOffset(index: Int, count: Int, direction: PenFanDirection, radius: Float): Offset {
    if (count <= 0) return Offset.Zero
    val angle = (-75.0 + 150.0 * index.coerceIn(0, count - 1) / (count - 1).coerceAtLeast(1)) * PI / 180.0
    val sideways = (sin(angle) * radius).toFloat()
    val forward = (cos(angle) * radius).toFloat()
    return when (direction) {
        PenFanDirection.DOWN -> Offset(sideways, forward)
        PenFanDirection.UP -> Offset(sideways, -forward)
        PenFanDirection.RIGHT -> Offset(forward, sideways)
        PenFanDirection.LEFT -> Offset(-forward, sideways)
    }
}

/**
 * Press, drag in the direction of a pen, release to activate it. No target while
 * still at the launcher or moving away; releasing there leaves the palette open.
 */
internal fun penFanTarget(
    displacement: Offset,
    direction: PenFanDirection,
    count: Int,
    minForward: Float,
): Int? {
    if (count <= 0) return null
    val (forward, sideways) = when (direction) {
        PenFanDirection.DOWN -> displacement.y to displacement.x
        PenFanDirection.UP -> -displacement.y to displacement.x
        PenFanDirection.RIGHT -> displacement.x to displacement.y
        PenFanDirection.LEFT -> -displacement.x to displacement.y
    }
    if (!forward.isFinite() || !sideways.isFinite() || forward < minForward) return null
    val angle = atan2(sideways.toDouble(), forward.toDouble()) * 180.0 / PI
    if (angle < -88.0 || angle > 88.0) return null
    return (((angle + 75.0) / 150.0) * (count - 1))
        .roundToInt().coerceIn(0, count - 1)
}
