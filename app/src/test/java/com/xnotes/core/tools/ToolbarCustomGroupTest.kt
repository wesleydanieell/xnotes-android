package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarCustomGroupTest {
    @Test fun customGroupsAreOptionalAndDoNotChangeThePrimaryLayout() {
        assertFalse(ToolbarItem.TOOL_GROUP in ToolbarLayout.DEFAULT.sections.flatMap { it.entries }.map { it.item })
        assertEquals(ToolbarLayout.DEFAULT, ToolbarLayout.DEFAULT.withMissingItemsAppended())
        val layout = ToolbarLayout.DEFAULT.addItem(0, 0, ToolbarItem.TOOL_GROUP)
        val group = layout.sections[0].entries.last().group
        assertNotNull(group)
        assertEquals(listOf(ToolbarItem.PEN, ToolbarItem.ERASER), group!!.tools)
        assertEquals(ToolbarGroupIcon.FOLDER, group.icon)
        assertEquals(ToolbarLayout.DEFAULT, layout.removeItem(0, layout.sections[0].entries.lastIndex))
    }

    @Test fun customGroupCanBeExpandedReorderedAndHaveMultipleOccurrences() {
        val layout = ToolbarLayout(listOf(ToolbarSection(emptyList())))
            .addItem(0, 0, ToolbarItem.TOOL_GROUP)
            .addItem(0, 0, ToolbarItem.TOOL_GROUP)
            .updateGroup(0, 0) { it.copy(display = ToolbarGroupDisplay.EXPANDED) }
            .moveGroupTool(0, 0, 0, 1)
        val groups = layout.sections[0].entries.mapNotNull { it.group }
        assertEquals(2, groups.size)
        assertTrue(groups[0].id != groups[1].id)
        assertEquals(ToolbarGroupDisplay.EXPANDED, groups[0].display)
        assertEquals(listOf(ToolbarItem.ERASER, ToolbarItem.PEN), groups[0].tools)
    }

    @Test fun normalEntryCanBeDraggedIntoGroupThenReleased() {
        val layout = ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(ToolbarItem.SHAPE)))))
            .addItem(0, 0, ToolbarItem.TOOL_GROUP)
        val packed = layout.moveItemIntoGroup(0, 0, 0, 1)
        assertEquals(1, packed.sections[0].entries.size)
        assertEquals(listOf(ToolbarItem.PEN, ToolbarItem.ERASER, ToolbarItem.SHAPE),
            packed.sections[0].entries[0].group!!.tools)
        val released = packed.extractGroupTool(0, 0, 2)
        assertEquals(ToolbarItem.SHAPE, released.sections[0].entries.last().item)
        assertFalse(ToolbarItem.SHAPE in released.sections[0].entries[0].group!!.tools)
    }

    @Test fun groupMembersCanTransferWithoutLeavingAnEmptyGroup() {
        val layout = ToolbarLayout(listOf(ToolbarSection(emptyList())))
            .addItem(0, 0, ToolbarItem.TOOL_GROUP)
            .addItem(0, 0, ToolbarItem.TOOL_GROUP)
            .updateGroup(0, 1) { it.copy(tools = listOf(ToolbarItem.SHAPE)) }
        val moved = layout.transferGroupTool(0, 0, 1, 0, 1)
        assertEquals(listOf(ToolbarItem.PEN), moved.sections[0].entries[0].group!!.tools)
        assertEquals(listOf(ToolbarItem.SHAPE, ToolbarItem.ERASER), moved.sections[0].entries[1].group!!.tools)
        assertEquals(moved, moved.transferGroupTool(0, 0, 0, 0, 1))
    }

    @Test fun canvasFilterKeepsOnlyCompatibleGroupMembers() {
        val note = ToolbarLayout(listOf(ToolbarSection(emptyList())))
            .addItem(0, 0, ToolbarItem.TOOL_GROUP)
            .updateGroup(0, 0) { it.copy(tools = listOf(ToolbarItem.TEXT, ToolbarItem.PEN)) }
        val workspace = ToolbarWorkspace.fromLegacy("main", "Main", note)
        val canvas = workspace.onlyItems(ToolbarLayout.CANVAS_ITEMS)
        assertEquals(listOf(ToolbarItem.PEN),
            canvas.bars.single().layout.sections.single().entries.single().group!!.tools)
    }
}
