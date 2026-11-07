package com.xnotes.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.settings.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DocumentTabsBarTest {
    @Test fun exactlyTwoModesRetainIndependentPositionAndRoundTripPreferences() {
        for (floating in listOf(false, true)) for (position in ToolbarPosition.entries) {
            val bar = DocumentTabsBarConfig(floating, position, DocumentTabsBarConfig.anchorOf(position))
            val original = Preferences(documentTabsBar = bar,
                toolbarLook = ToolbarLook(position = ToolbarPosition.BOTTOM, floating = false))
            val restored = Preferences.fromJson(JSONObject(original.toJson().toString()))
            assertEquals(original, restored)
            val moved = bar.movedTo(ToolbarAnchor.CENTER_LEFT)
            assertEquals(floating, moved.floating)
            assertEquals(ToolbarPosition.LEFT, moved.position)
            assertEquals(ToolbarAnchor.CENTER_LEFT, moved.anchor)
            assertEquals(ToolbarPosition.LEFT, moved.withFloating(!floating).position)
        }
    }

    @Test fun oldAndMalformedPreferencesHaveSafeIndependentDefaults() {
        val old = Preferences.fromJson(JSONObject().put("toolbar_position", "bottom").put("toolbar_floating", false))
        assertEquals(DocumentTabsBarConfig(), old.documentTabsBar)
        val malformed = DocumentTabsBarConfig.fromJson(JSONObject().put("anchor", "CENTER").put("position", "missing"))
        assertEquals(DocumentTabsBarConfig(), malformed)
        assertNull(ToolbarItem.fromId("document_tabs"))
    }

    private fun entry(id: String, lane: Int = 0) = JSONObject().put("id", id).put("lane", lane).put("visible", true)
    private fun bar(id: String, entries: JSONArray) = JSONObject().put("id", id).put("name", id)
        .put("geometry", JSONObject().put("width_dp", 300)).put("sections", JSONArray().put(
            JSONObject().put("lanes", 2).put("entries", entries)))

    @Test fun removedElementsMigrateWithoutDeletingToolsOrKeepingEmptyTabsBars() {
        val workspace = JSONObject().put("version", 6)
            .put("note", JSONObject().put("bars", JSONArray()
                .put(bar("main", JSONArray().put(entry("document_tabs")).put(entry("pen", 1))))
                .put(bar("old-tabs", JSONArray().put(entry("document_tabs"))))))
            .put("canvas", JSONObject().put("bars", JSONArray().put(bar("old-tabs-only", JSONArray().put(entry("document_tabs"))))))
        val loaded = Settings.fromJson(JSONObject().put("toolbar_workspaces", workspace)).effectiveToolbarWorkspaces()
        assertEquals(listOf("main"), loaded.note.bars.map { it.id })
        assertEquals(listOf(ToolbarItem.PEN), loaded.note.bars.single().layout.sections.single().entries.map { it.item })
        assertEquals(1, loaded.note.bars.single().layout.sections.single().lanes)
        assertEquals(0, loaded.note.bars.single().layout.sections.single().entries.single().lane)
        assertEquals(300, loaded.note.bars.single().geometry.widthDp)
        assertTrue(loaded.canvas.bars.single().layout.sections.any { s -> s.entries.any { it.item == ToolbarItem.HOME } })
        assertEquals(loaded, Settings.fromJson(JSONObject(Settings(toolbarWorkspaces = loaded).toJson().toString())).effectiveToolbarWorkspaces())
    }

    @Test fun allFloatingAnchorsStayVisibleAndAvoidToolbarsAtEveryEdgeAndCorner() {
        val host = Size(1200f, 800f); val card = Size(300f, 100f)
        val tools = listOf(Rect(0f, 0f, 1200f, 50f), Rect(0f, 750f, 1200f, 800f),
            Rect(0f, 50f, 50f, 750f), Rect(1150f, 50f, 1200f, 750f))
        for (anchor in ToolbarAnchor.entries.filterNot { it == ToolbarAnchor.CENTER }) {
            val offset = documentTabsOffset(host, card, anchor, tools, 8f)
            val placed = Rect(offset, card)
            assertTrue("Outside host at $anchor: $placed", placed.left >= 0 && placed.top >= 0 && placed.right <= host.width && placed.bottom <= host.height)
            assertFalse("Overlaps a tool bar at $anchor: $placed", tools.any { it.overlaps(placed) })
        }
    }

    @Test fun smallWindowsClampInsteadOfPlacingNavigationOffScreen() {
        val placed = documentTabsOffset(Size(100f, 50f), Size(360f, 120f), ToolbarAnchor.BOTTOM_RIGHT, emptyList(), 8f)
        assertEquals(0f, placed.x, 0f); assertEquals(0f, placed.y, 0f)
    }
    @Test fun expandedSizingFillsAvailableSpaceOrClampsManualSizeAndLimits() {
        val dynamic = DocumentTabsBarConfig()
        assertEquals(1000f, dynamic.expandedExtent(1000f), 0f)
        assertEquals(480f, dynamic.copy(maxExtentDp = 480).expandedExtent(1000f), 0f)
        val manual = dynamic.copy(sizing = DocumentTabsSizing.MANUAL, manualExtentDp = 600, minExtentDp = 240, maxExtentDp = 720)
        assertEquals(600f, manual.expandedExtent(1000f), 0f)
        assertEquals(180f, manual.expandedExtent(180f), 0f)
        assertEquals(240f, manual.copy(manualExtentDp = 80).expandedExtent(1000f), 0f)
        assertEquals(manual, DocumentTabsBarConfig.fromJson(JSONObject(manual.toJson().toString())))
        assertEquals(DocumentTabsSizing.DYNAMIC, DocumentTabsBarConfig.fromJson(JSONObject().put("sizing", "unknown")).sizing)
    }

    @Test fun expandedStripStopsBeforeNeighborOnTheSameRowAndHandlesSideRails() {
        val host = Size(1200f, 800f)
        val neighbors = listOf(Rect(800f, 0f, 1200f, 70f))
        val strip = documentTabsFreeStrip(host, ToolbarAnchor.TOP_LEFT, false, 50f, neighbors, 8f, 80f)
        assertEquals(8f, strip.left, 0f)
        assertEquals(792f, strip.right, 0f)
        assertEquals(8f, strip.top, 0f)
        assertFalse(neighbors.any { it.overlaps(strip) })
        val rail = documentTabsFreeStrip(host, ToolbarAnchor.CENTER_RIGHT, true, 168f,
            listOf(Rect(1000f, 600f, 1200f, 800f)), 8f, 80f)
        assertEquals(592f, rail.bottom, 0f)
        assertEquals(1192f, rail.right, 0f)
    }

    @Test fun expandedStripUsesWholeWindowAndMovesPastFullEdgeObstructions() {
        val host = Size(1200f, 800f)
        val open = documentTabsFreeStrip(host, ToolbarAnchor.TOP_CENTER, false, 50f, emptyList(), 8f, 80f)
        assertEquals(1184f, open.width, 0f)
        val obstacles = listOf(Rect(0f, 0f, 1200f, 60f), Rect(0f, 60f, 80f, 800f), Rect(1100f, 60f, 1200f, 800f))
        val strip = documentTabsFreeStrip(host, ToolbarAnchor.TOP_CENTER, false, 50f, obstacles, 8f, 80f)
        assertEquals(68f, strip.top, 0f)
        assertEquals(88f, strip.left, 0f)
        assertEquals(1092f, strip.right, 0f)
        assertFalse(obstacles.any { it.overlaps(strip) })
    }

}
