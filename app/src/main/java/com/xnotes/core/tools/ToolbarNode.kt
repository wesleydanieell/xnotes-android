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
        is ToolbarNode.Group -> if (expanded) children.forEach { it.flattenItems(out) }
        is ToolbarNode.Grid -> children.forEach { it.flattenItems(out) }
        is ToolbarNode.Spacer, is ToolbarNode.Separator -> Unit
    }
    return out
}

fun toolbarNodesFromLayout(layout: ToolbarLayout): List<ToolbarNode> = layout.sections.mapIndexed { sectionIndex, section ->
    ToolbarNode.Group(
        id = "section-$sectionIndex",
        children = section.entries.mapIndexed { entryIndex, entry ->
            if (entry.item == ToolbarItem.COLORS) {
                ToolbarNode.ColorPalette("section-$sectionIndex-colors-$entryIndex", visible = entry.visible)
            } else {
                ToolbarNode.Tool("section-$sectionIndex-${entry.item.id}-$entryIndex", entry.item, entry.visible)
            }
        },
    )
}
