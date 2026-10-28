package com.xnotes.core.tools

/** Finite v3 composition tree for a bar. Flat v2 layouts remain valid when [ToolbarBar.nodes] is null. */
sealed interface ToolbarNode {
    val id: String
    val visible: Boolean

    data class Tool(
        override val id: String,
        val item: ToolbarItem,
        override val visible: Boolean = true,
        val visualDp: Int? = null,
        val hitboxDp: Int? = null,
        val spacingDp: Int = 0,
        val priority: Int = 0,
    ) : ToolbarNode

    data class ColorPalette(
        override val id: String,
        val count: Int? = null,
        val columns: Int = 1,
        override val visible: Boolean = true,
    ) : ToolbarNode

    data class UnifiedPen(
        override val id: String,
        val styles: List<com.xnotes.core.tools.Tool> = listOf(com.xnotes.core.tools.Tool.PEN, com.xnotes.core.tools.Tool.DASHED, com.xnotes.core.tools.Tool.CALLIGRAPHY, com.xnotes.core.tools.Tool.HIGHLIGHTER),
        val lastStyle: com.xnotes.core.tools.Tool? = null,
        override val visible: Boolean = true,
    ) : ToolbarNode

    data class Group(
        override val id: String,
        val children: List<ToolbarNode>,
        val collapsible: Boolean = false,
        val expanded: Boolean = true,
        override val visible: Boolean = true,
    ) : ToolbarNode

    data class Grid(
        override val id: String,
        val children: List<ToolbarNode>,
        val columns: Int = 2,
        val rows: Int? = null,
        override val visible: Boolean = true,
    ) : ToolbarNode

    data class Spacer(
        override val id: String,
        val sizeDp: Int = 8,
        override val visible: Boolean = true,
    ) : ToolbarNode

    data class Separator(
        override val id: String,
        override val visible: Boolean = true,
    ) : ToolbarNode
}

/** Remove malformed IDs, duplicate IDs, unsupported depth and unsafe dimensions deterministically. */
fun ToolbarNode.sanitized(depth: Int = 0, ids: MutableSet<String> = HashSet()): ToolbarNode? {
    if (depth > 3 || id.isBlank() || id.length > 64 || !ids.add(id)) return null
    fun dim(v: Int?, fallback: Int? = null): Int? = v?.coerceIn(1, 256) ?: fallback
    return when (this) {
        is ToolbarNode.Tool -> copy(visualDp = dim(visualDp), hitboxDp = dim(hitboxDp), spacingDp = spacingDp.coerceIn(0, 48))
        is ToolbarNode.ColorPalette -> copy(count = count?.coerceIn(1, InkPalette.MAX_SWATCHES), columns = columns.coerceIn(1, 6))
        is ToolbarNode.UnifiedPen -> copy(styles = styles.distinct().filter { it.isStroke }.take(8), lastStyle = lastStyle?.takeIf { it in styles && it.isStroke })
        is ToolbarNode.Group -> copy(children = children.take(64).mapNotNull { it.sanitized(depth + 1, ids) })
        is ToolbarNode.Grid -> copy(children = children.take(64).mapNotNull { it.sanitized(depth + 1, ids) }, columns = columns.coerceIn(1, 6), rows = rows?.coerceIn(1, 6))
        is ToolbarNode.Spacer -> copy(sizeDp = sizeDp.coerceIn(1, 256))
        is ToolbarNode.Separator -> this
    }
}

fun ToolbarNode.flattenItems(out: MutableList<ToolbarItem> = ArrayList()): List<ToolbarItem> {
    if (!visible) return out
    when (this) {
        is ToolbarNode.Tool -> out += item
        is ToolbarNode.ColorPalette -> out += ToolbarItem.COLORS
        is ToolbarNode.UnifiedPen -> out += ToolbarItem.PEN
        is ToolbarNode.Group -> if (expanded) children.forEach { it.flattenItems(out) }
        is ToolbarNode.Grid -> children.forEach { it.flattenItems(out) }
        is ToolbarNode.Spacer, is ToolbarNode.Separator -> Unit
    }
    return out
}

fun ToolbarNode.toolMetrics(out: MutableMap<Tool, ToolbarNode.Tool> = LinkedHashMap()): Map<Tool, ToolbarNode.Tool> {
    when (this) {
        is ToolbarNode.Tool -> item.toTool()?.let { out[it] = this }
        is ToolbarNode.UnifiedPen -> Unit
        is ToolbarNode.Group -> children.forEach { it.toolMetrics(out) }
        is ToolbarNode.Grid -> children.forEach { it.toolMetrics(out) }
        is ToolbarNode.ColorPalette, is ToolbarNode.Spacer, is ToolbarNode.Separator -> Unit
    }
    return out
}

fun ToolbarNode.updateTool(item: ToolbarItem, transform: (ToolbarNode.Tool) -> ToolbarNode.Tool): ToolbarNode = when (this) {
    is ToolbarNode.Tool -> if (this.item == item) transform(this) else this
    is ToolbarNode.Group -> copy(children = children.map { it.updateTool(item, transform) })
    is ToolbarNode.Grid -> copy(children = children.map { it.updateTool(item, transform) })
    else -> this
}

fun ToolbarNode.updateColorPalette(transform: (ToolbarNode.ColorPalette) -> ToolbarNode.ColorPalette): ToolbarNode = when (this) {
    is ToolbarNode.ColorPalette -> transform(this)
    is ToolbarNode.Group -> copy(children = children.map { it.updateColorPalette(transform) })
    is ToolbarNode.Grid -> copy(children = children.map { it.updateColorPalette(transform) })
    else -> this
}

fun ToolbarNode.findColorPalette(): ToolbarNode.ColorPalette? = when (this) {
    is ToolbarNode.ColorPalette -> this
    is ToolbarNode.Group -> children.firstNotNullOfOrNull { it.findColorPalette() }
    is ToolbarNode.Grid -> children.firstNotNullOfOrNull { it.findColorPalette() }
    else -> null
}

fun ToolbarNode.updateGrid(transform: (ToolbarNode.Grid) -> ToolbarNode.Grid): ToolbarNode = when (this) {
    is ToolbarNode.Grid -> transform(this)
    is ToolbarNode.Group -> copy(children = children.map { it.updateGrid(transform) })
    else -> this
}

fun ToolbarNode.findGrid(): ToolbarNode.Grid? = when (this) {
    is ToolbarNode.Grid -> this
    is ToolbarNode.Group -> children.firstNotNullOfOrNull { it.findGrid() }
    else -> null
}

fun ToolbarNode.findTool(item: ToolbarItem): ToolbarNode.Tool? = when (this) {
    is ToolbarNode.Tool -> takeIf { it.item == item }
    is ToolbarNode.Group -> children.firstNotNullOfOrNull { it.findTool(item) }
    is ToolbarNode.Grid -> children.firstNotNullOfOrNull { it.findTool(item) }
    else -> null
}

private fun ToolbarItem.toTool(): Tool? = when (this) {
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

fun toolbarNodesFromLayout(layout: ToolbarLayout): List<ToolbarNode> = layout.sections.mapIndexed { sectionIndex, section ->
    ToolbarNode.Group(
        id = "section-$sectionIndex",
        children = section.entries.mapIndexed { entryIndex, entry ->
            if (entry.item == ToolbarItem.COLORS) {
                ToolbarNode.ColorPalette("section-$sectionIndex-colors-$entryIndex", visible = entry.visible)
            } else if (entry.item == ToolbarItem.PEN) {
                ToolbarNode.UnifiedPen("section-$sectionIndex-pen-$entryIndex", visible = entry.visible)
            } else {
                ToolbarNode.Tool("section-$sectionIndex-${entry.item.id}-$entryIndex", entry.item, entry.visible)
            }
        },
    )
}
