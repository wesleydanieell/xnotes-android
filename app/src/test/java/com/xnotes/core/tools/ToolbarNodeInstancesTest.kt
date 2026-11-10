package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarNodeInstancesTest {
    @Test fun twoRowsCanContainSixInstancesIncludingRepeatedTools() {
        val tools = listOf(
            ToolbarItem.PEN, ToolbarItem.ERASER, ToolbarItem.PEN,
            ToolbarItem.HIGHLIGHTER, ToolbarItem.PEN, ToolbarItem.LASSO,
        )
        val grid = ToolbarNode.Grid(
            id = "grid", columns = 3, rows = 2,
            children = tools.mapIndexed { i, item -> ToolbarNode.Tool("tool-$i", item) },
        )
        val bar = ToolbarBar(
            id = "main", name = "Main",
            layout = ToolbarLayout.DEFAULT,
            nodes = listOf(grid),
        )
        assertEquals(3, bar.effectiveColumns())
        assertEquals(2, bar.effectiveRows())
        assertEquals(0, bar.effectiveGridOverflowCount())
        assertEquals(tools, bar.effectiveLayout().sections.single().entries.map { it.item })
    }

    @Test fun addAndRemoveOperateOnInstancesNotOnToolTypes() {
        val first = ToolbarNode.Tool("pen-1", ToolbarItem.PEN)
        val grid = ToolbarNode.Grid("grid", listOf(first), columns = 3, rows = 2)
        val original = listOf<ToolbarNode>(grid)
        val added = original.addNodeInstance(ToolbarNode.Tool("pen-2", ToolbarItem.PEN), "grid")
        val children = (added.single() as ToolbarNode.Grid).children
        assertEquals(listOf("pen-1", "pen-2"), children.map { it.id })
        assertEquals(listOf(ToolbarItem.PEN, ToolbarItem.PEN), children.map { (it as ToolbarNode.Tool).item })
        assertSame(added, added.addNodeInstance(ToolbarNode.Tool("pen-2", ToolbarItem.PEN), "grid"))
        assertSame(added, added.addNodeInstance(ToolbarNode.Tool("pen-3", ToolbarItem.PEN), "missing"))
        val removed = added.removeNode("pen-1")
        assertEquals(listOf("pen-2"), (removed.single() as ToolbarNode.Grid).children.map { it.id })
    }

    @Test fun duplicateGeneratesNewIdsWithoutChangingToolKindOrOriginal() {
        val grid = ToolbarNode.Grid("g0", listOf(
            ToolbarNode.Tool("t0", ToolbarItem.PEN, visualDp = 32),
            ToolbarNode.Tool("t1", ToolbarItem.ERASER),
        ), columns = 3, rows = 2)
        var counter = 0
        val duplicated = listOf<ToolbarNode>(grid).duplicateNodeInstance("t0") { "copy-${counter++}" }
        val children = (duplicated.single() as ToolbarNode.Grid).children
        assertEquals(listOf("t0", "copy-0", "t1"), children.map { it.id })
        assertEquals(listOf(ToolbarItem.PEN, ToolbarItem.PEN, ToolbarItem.ERASER),
            children.map { (it as ToolbarNode.Tool).item })
        assertEquals(32, (children[1] as ToolbarNode.Tool).visualDp)
        assertEquals(2, grid.children.size)
        assertEquals(listOf("t1"), (duplicated.removeNode("copy-0").single() as ToolbarNode.Grid)
            .children.filter { it.id != "t0" }.map { it.id })
    }

    @Test fun duplicatingGroupsProducesDistinctNestedIds() {
        val source = listOf<ToolbarNode>(ToolbarNode.Group("group", listOf(
            ToolbarNode.Grid("grid", listOf(ToolbarNode.Tool("pen", ToolbarItem.PEN)), 3, 2),
        )))
        var next = 0
        val duplicate = source.duplicateNodeInstance("group") { "new-${next++}" }
        assertEquals(2, duplicate.size)
        val copied = duplicate[1] as ToolbarNode.Group
        assertEquals("new-0", copied.id)
        val copiedGrid = copied.children.single() as ToolbarNode.Grid
        assertEquals("new-1", copiedGrid.id)
        assertEquals("new-2", copiedGrid.children.single().id)
        assertEquals(ToolbarItem.PEN, (copiedGrid.children.single() as ToolbarNode.Tool).item)
        assertTrue(duplicate.all { it.sanitized() != null })
        assertFalse(copied.id == source.single().id)
    }
}
