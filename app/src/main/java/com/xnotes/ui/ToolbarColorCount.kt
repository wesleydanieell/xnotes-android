package com.xnotes.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolbarNode

/** Optional per-bar swatch count; null retains the shared ink palette count. */
internal val LocalToolbarColorCount = compositionLocalOf<Int?> { null }
internal val LocalToolbarColorColumns = compositionLocalOf { 1 }

internal val LocalToolbarNodeMetrics = compositionLocalOf<Map<Tool, ToolbarNode.Tool>> { emptyMap() }

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ToolbarColorGrid(
    count: Int,
    columns: Int,
    vertical: Boolean,
    content: @Composable (Int) -> Unit,
) {
    val safeCount = count.coerceAtLeast(0)
    val safeColumns = columns.coerceIn(1, 6)
    if (safeColumns == 1) {
        for (index in 0 until safeCount) content(index)
    } else if (vertical) {
        FlowColumn(maxItemsInEachColumn = safeColumns) {
            for (index in 0 until safeCount) content(index)
        }
    } else {
        FlowRow(maxItemsInEachRow = safeColumns) {
            for (index in 0 until safeCount) content(index)
        }
    }
}
