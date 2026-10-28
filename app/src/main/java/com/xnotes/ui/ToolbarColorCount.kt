package com.xnotes.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarNode

/** Optional per-bar swatch count; null retains the shared ink palette count. */
internal val LocalToolbarColorCount = compositionLocalOf<Int?> { null }
internal val LocalToolbarColorColumns = compositionLocalOf { 1 }
internal val LocalToolbarGridOverflow = compositionLocalOf { 0 }
internal val LocalToolbarGridOverflowItems = compositionLocalOf<List<ToolbarItem>> { emptyList() }

internal val LocalToolbarNodeMetrics = compositionLocalOf<Map<Tool, ToolbarNode.Tool>> { emptyMap() }

internal fun ToolbarItem.overflowTool(): Tool? = when (this) {
    ToolbarItem.PEN -> Tool.PEN
    ToolbarItem.DASHED -> Tool.DASHED
    ToolbarItem.CALLIGRAPHY -> Tool.CALLIGRAPHY
    ToolbarItem.SPEED -> Tool.SPEED
    ToolbarItem.TAPER -> Tool.TAPER
    ToolbarItem.HIGHLIGHTER -> Tool.HIGHLIGHTER
    ToolbarItem.ERASER -> Tool.ERASER
    ToolbarItem.PAN -> Tool.PAN
    ToolbarItem.SELECT -> Tool.SELECT
    ToolbarItem.LASSO -> Tool.LASSO
    ToolbarItem.SCREENSHOT -> Tool.SCREENSHOT
    ToolbarItem.SHAPE -> Tool.SHAPE
    ToolbarItem.TEXT -> Tool.TEXT
    ToolbarItem.TEXT_BOX -> Tool.TEXT_BOX
    ToolbarItem.MARKUP -> Tool.MARKUP
    ToolbarItem.TAPE -> Tool.TAPE
    else -> null
}

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
