package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.xnotes.core.tools.*

/** Shared bar hit exclusions and measured floating covers, in window coordinates. */
internal class SharedCommunityToolbarState {
    val hitAreas = ToolbarHitAreas()
    private data class Cover(val bounds: Rect, val edge: String)
    private val covers = mutableStateMapOf<Any, Cover>()

    fun update(key: Any, bounds: Rect, floating: Boolean, edge: String) {
        val next = Cover(bounds, edge)
        if (!floating || bounds == Rect.Zero) covers.remove(key)
        else if (covers[key] != next) covers[key] = next
    }

    fun remove(key: Any) { covers.remove(key) }

    fun insets(pane: Rect): ToolbarPaneInsets {
        val p = ToolbarPaneBounds(pane.left.toDouble(), pane.top.toDouble(), pane.right.toDouble(), pane.bottom.toDouble())
        return covers.values.fold(ToolbarPaneInsets()) { total, cover ->
            val b = cover.bounds
            val inset = sharedToolbarInsets(ToolbarPaneBounds(b.left.toDouble(), b.top.toDouble(), b.right.toDouble(), b.bottom.toDouble()), p, cover.edge)
            ToolbarPaneInsets(maxOf(total.left, inset.left), maxOf(total.top, inset.top),
                maxOf(total.right, inset.right), maxOf(total.bottom, inset.bottom))
        }
    }
}

/** Every visible floating bar, including overflow, contributes only its actual footprint. */
@Composable
internal fun MeasuredSharedToolbar(state: SharedCommunityToolbarState, content: @Composable () -> Unit) {
    val key = remember { Any() }
    val look = LocalToolbarLook.current
    var bounds by remember { mutableStateOf(Rect.Zero) }
    DisposableEffect(state, key) { onDispose { state.remove(key) } }
    SideEffect { state.update(key, bounds, look.floating, look.position.id) }
    Box(Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }) { content() }
}
