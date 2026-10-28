package com.xnotes.core.tools

/** Typed spacing used by the V2 composition engine. Values are logical dp, not screen pixels. */
data class ToolbarInsets(val start: Int = 0, val top: Int = 0, val end: Int = 0, val bottom: Int = 0)

data class ToolbarSpacing(val horizontal: Int = 0, val vertical: Int = 0)

enum class ToolbarLayoutKind { ROW, COLUMN, GRID }

/** The persistent composition tree. A node ID identifies an instance, not a tool type. */
sealed interface ToolbarLayoutNode {
    val instanceId: String

    data class Tool(
        override val instanceId: String,
        val toolType: String,
        val rowSpan: Int = 1,
        val columnSpan: Int = 1,
    ) : ToolbarLayoutNode

    data class Container(
        override val instanceId: String,
        val kind: ToolbarLayoutKind,
        val children: List<ToolbarLayoutNode>,
        val rows: Int = 1,
        val columns: Int = 1,
        val padding: ToolbarInsets = ToolbarInsets(),
        val gap: ToolbarSpacing = ToolbarSpacing(),
    ) : ToolbarLayoutNode

    data class Spacer(override val instanceId: String, val flexible: Boolean = false) : ToolbarLayoutNode

    data class Divider(override val instanceId: String) : ToolbarLayoutNode
}

data class ToolbarBarComposition(
    val id: String,
    val name: String,
    val root: ToolbarLayoutNode.Container,
)

data class ToolbarNodeSlot(
    val instanceId: String,
    val parentId: String,
    val order: Int,
    val row: Int,
    val column: Int,
    val rowSpan: Int,
    val columnSpan: Int,
    val node: ToolbarLayoutNode,
    val children: List<ToolbarNodeSlot> = emptyList(),
)

data class ToolbarResolvedLayout(
    val root: ToolbarNodeSlot,
    val rows: Int,
    val columns: Int,
)

object ToolbarCompositionValidator {
    fun validate(root: ToolbarLayoutNode): List<String> {
        val errors = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        fun visit(node: ToolbarLayoutNode, depth: Int) {
            if (depth > 32) errors += "layout depth exceeds 32"
            if (node.instanceId.isBlank()) errors += "node has a blank instanceId"
            if (!ids.add(node.instanceId)) errors += "duplicate instanceId: ${node.instanceId}"
            when (node) {
                is ToolbarLayoutNode.Tool -> {
                    if (node.rowSpan !in 1..8) errors += "invalid rowSpan for ${node.instanceId}"
                    if (node.columnSpan !in 1..8) errors += "invalid columnSpan for ${node.instanceId}"
                }
                is ToolbarLayoutNode.Container -> {
                    if (node.rows !in 1..8) errors += "invalid rows for ${node.instanceId}"
                    if (node.columns !in 1..8) errors += "invalid columns for ${node.instanceId}"
                    node.children.forEach { visit(it, depth + 1) }
                }
                is ToolbarLayoutNode.Spacer, is ToolbarLayoutNode.Divider -> Unit
            }
        }
        visit(root, 0)
        return errors
    }
}

/** Pure row/column/grid resolver. It never mutates the composition and always scans row-major. */
object ToolbarLayoutResolver {
    fun resolve(root: ToolbarLayoutNode.Container): ToolbarResolvedLayout {
        val errors = ToolbarCompositionValidator.validate(root)
        require(errors.isEmpty()) { errors.joinToString("; ") }
        val resolved = resolveNode(root, parentId = "workspace", order = 0, row = 0, column = 0, rowSpan = 1, columnSpan = 1)
        return ToolbarResolvedLayout(resolved, resolved.childrenRows(), resolved.childrenColumns())
    }

    private fun resolveNode(
        node: ToolbarLayoutNode,
        parentId: String,
        order: Int,
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int,
    ): ToolbarNodeSlot {
        val children = if (node is ToolbarLayoutNode.Container) resolveChildren(node) else emptyList()
        return ToolbarNodeSlot(node.instanceId, parentId, order, row, column, rowSpan, columnSpan, node, children)
    }

    private fun resolveChildren(container: ToolbarLayoutNode.Container): List<ToolbarNodeSlot> {
        if (container.children.isEmpty()) return emptyList()
        val (rows, columns) = dimensions(container)
        val occupied = Array(rows) { BooleanArray(columns) }
        val out = mutableListOf<ToolbarNodeSlot>()
        container.children.forEachIndexed { index, child ->
            val rowSpan = childSpanRows(child)
            val columnSpan = childSpanColumns(child)
            val position = when (container.kind) {
                ToolbarLayoutKind.ROW -> 0 to index
                ToolbarLayoutKind.COLUMN -> index to 0
                ToolbarLayoutKind.GRID -> findFirstFree(occupied, rowSpan, columnSpan)
            }
            val row = position.first
            val column = position.second
            require(row + rowSpan <= rows && column + columnSpan <= columns) {
                "${child.instanceId} does not fit in ${container.instanceId}"
            }
            for (r in row until row + rowSpan) for (c in column until column + columnSpan) {
                require(!occupied[r][c]) { "${child.instanceId} overlaps in ${container.instanceId}" }
                occupied[r][c] = true
            }
            out += resolveNode(child, container.instanceId, index, row, column, rowSpan, columnSpan)
        }
        return out
    }

    private fun dimensions(container: ToolbarLayoutNode.Container): Pair<Int, Int> = when (container.kind) {
        ToolbarLayoutKind.ROW -> maxOf(1, container.children.maxOf { childSpanRows(it) }) to
            maxOf(1, container.children.sumOf { childSpanColumns(it) })
        ToolbarLayoutKind.COLUMN -> maxOf(1, container.children.sumOf { childSpanRows(it) }) to
            maxOf(1, container.children.maxOf { childSpanColumns(it) })
        ToolbarLayoutKind.GRID -> container.rows to container.columns
    }

    private fun childSpanRows(node: ToolbarLayoutNode): Int =
        if (node is ToolbarLayoutNode.Tool) node.rowSpan else 1

    private fun childSpanColumns(node: ToolbarLayoutNode): Int =
        if (node is ToolbarLayoutNode.Tool) node.columnSpan else 1

    private fun findFirstFree(occupied: Array<BooleanArray>, rowSpan: Int, columnSpan: Int): Pair<Int, Int> {
        for (row in occupied.indices) for (column in occupied[row].indices) {
            if (row + rowSpan <= occupied.size && column + columnSpan <= occupied[row].size &&
                (row until row + rowSpan).all { r -> (column until column + columnSpan).all { c -> !occupied[r][c] } }
            ) return row to column
        }
        error("no free slot for span ${rowSpan}x$columnSpan")
    }

    private fun ToolbarNodeSlot.childrenRows(): Int = children.maxOfOrNull { it.row + it.rowSpan } ?: 1
    private fun ToolbarNodeSlot.childrenColumns(): Int = children.maxOfOrNull { it.column + it.columnSpan } ?: 1
}
