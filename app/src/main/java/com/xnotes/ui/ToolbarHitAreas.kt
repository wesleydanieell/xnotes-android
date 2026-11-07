package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/** Every bar excludes its own surface from native-canvas stylus forwarding, including secondary
 * floating bars. Separate rectangles preserve drawing in the gaps between bars. */
internal class ToolbarHitAreas {
    private val areas = mutableStateMapOf<Any, Rect>()
    fun update(key: Any, bounds: Rect) { if (areas[key] != bounds) areas[key] = bounds }
    fun remove(key: Any) { areas.remove(key) }
    fun bounds(): List<Rect> = areas.values.toList()
}

internal val LocalToolbarHitAreas = compositionLocalOf<ToolbarHitAreas?> { null }

@Composable
internal fun ToolbarHitArea(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val areas = LocalToolbarHitAreas.current
    val key = remember { Any() }
    DisposableEffect(areas, key) { onDispose { areas?.remove(key) } }
    Box(modifier.onGloballyPositioned { areas?.update(key, it.boundsInWindow()) }) { content() }
}
