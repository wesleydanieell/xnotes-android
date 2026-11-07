package com.xnotes.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.xnotes.core.workspace.NoteTabs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Workspace-owned motion survives a toolbar renderer being replaced (for example note/canvas).
 * Positions use tab slots rather than pixels so multiple bars can share motion at different widths. */
class DocumentTabsSelectionState(tabs: NoteTabs, activeUri: String?) {
    internal val position = Animatable(tabs.tabs.indexOfFirst { it.uri == activeUri }.coerceAtLeast(0).toFloat())
    private val weights = mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>().apply {
        tabs.tabs.forEach { put(it.uri, Animatable(if (it.uri == activeUri) 1f else 0f)) }
    }

    internal fun weight(uri: String): Float = weights[uri]?.value ?: 0f

    internal suspend fun select(tabs: NoteTabs, activeUri: String?) = coroutineScope {
        val ids = tabs.tabs.map { it.uri }.toSet()
        weights.keys.toList().filterNot { it in ids }.forEach { weights.remove(it) }
        launch {
            position.animateTo(tabs.tabs.indexOfFirst { it.uri == activeUri }.coerceAtLeast(0).toFloat(),
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh))
        }
        tabs.tabs.forEach { tab ->
            val weight = weights.getOrPut(tab.uri) { Animatable(0f) }
            launch { weight.animateTo(if (tab.uri == activeUri) 1f else 0f, tween(300)) }
        }
    }
}

@Composable
internal fun rememberDocumentTabsSelection(tabs: NoteTabs, activeUri: String?): DocumentTabsSelectionState {
    val state = remember { DocumentTabsSelectionState(tabs, activeUri) }
    LaunchedEffect(state, tabs.tabs.map { it.uri }, activeUri) { state.select(tabs, activeUri) }
    return state
}
