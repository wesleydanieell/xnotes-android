package com.xnotes.ui

import androidx.compose.runtime.compositionLocalOf
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolbarNode

/** Optional per-bar swatch count; null retains the shared ink palette count. */
internal val LocalToolbarColorCount = compositionLocalOf<Int?> { null }

internal val LocalToolbarNodeMetrics = compositionLocalOf<Map<Tool, ToolbarNode.Tool>> { emptyMap() }
