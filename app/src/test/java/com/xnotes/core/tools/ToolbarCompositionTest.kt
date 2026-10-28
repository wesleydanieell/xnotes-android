package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarCompositionTest {
    private fun tool(id: String, type: String = id, rows: Int = 1, columns: Int = 1) =
        ToolbarLayoutNode.Tool(id, type, rows, columns)

    @Test
    fun referenceFixtureIsResolvedInStableOrder() {
        val palette = ToolbarLayoutNode.Container(
            instanceId = "palette",
            kind = ToolbarLayoutKind.GRID,
            rows = 2,
            columns = 4,
            children = (1..8).map { tool("color-$it", "color") },
        )
        val toolGrid = ToolbarLayoutNode.Container(
            instanceId = "tool-grid",
            kind = ToolbarLayoutKind.GRID,
            rows = 2,
            columns = 2,
            children = listOf(tool("pen"), tool("shape"), tool("text"), tool("select")),
        )
        val root = ToolbarLayoutNode.Container(
            instanceId = "reference",
            kind = ToolbarLayoutKind.ROW,
            children = listOf(
                tool("home", rows = 2),
                palette,
                ToolbarLayoutNode.Divider("palette-divider"),
                toolGrid,
                tool("undo", rows = 2),
                tool("redo", rows = 2),
            ),
        )

        val result = ToolbarLayoutResolver.resolve(root)

        assertEquals(
            listOf("home", "palette", "palette-divider", "tool-grid", "undo", "redo"),
            result.root.children.map { it.instanceId },
        )
        assertEquals(2, result.root.children.first().rowSpan)
        assertEquals(8, result.root.children[1].children.size)
        assertEquals((0..7).toList(), result.root.children[1].children.map { it.row * 4 + it.column })
        assertEquals(listOf("pen", "shape", "text", "select"), result.root.children[3].children.map { it.instanceId })
        assertTrue(result.root.children.zipWithNext().all { (a, b) -> a.column + a.columnSpan <= b.column })
    }

    @Test
    fun gridPlacesSpansRowMajorWithoutOverlap() {
        val root = ToolbarLayoutNode.Container(
            "grid",
            ToolbarLayoutKind.GRID,
            rows = 3,
            columns = 2,
            children = listOf(
                tool("wide", rows = 1, columns = 2),
                tool("tall", rows = 2),
                tool("last"),
            ),
        )

        val children = ToolbarLayoutResolver.resolve(root).root.children

        assertEquals(listOf("wide", "tall", "last"), children.map { it.instanceId })
        assertEquals(0 to 0, children[0].row to children[0].column)
        assertEquals(1 to 0, children[1].row to children[1].column)
        assertEquals(1 to 1, children[2].row to children[2].column)
    }

    @Test
    fun validatorRejectsDuplicateIdsAndInvalidSpans() {
        val root = ToolbarLayoutNode.Container(
            "root",
            ToolbarLayoutKind.ROW,
            children = listOf(tool("same", rows = 0), tool("same")),
        )

        val errors = ToolbarCompositionValidator.validate(root)

        assertTrue(errors.any { it.contains("duplicate instanceId") })
        assertTrue(errors.any { it.contains("invalid rowSpan") })
    }
}
