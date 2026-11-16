package com.xnotes.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.settings.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DocumentTabsBarTest {
    @Test fun legacyLanePreferencesPreserveEdgeAndAlignment() {
        val config = DocumentTabsBarConfig(floating = false)
            .movedTo(ToolbarPosition.RIGHT, com.xnotes.core.tools.ToolbarDockAlignment.END)
        val legacy = config.toJson().put("dock_lane", 2).put("after_toolbar_id", "tools")
        assertEquals(config, DocumentTabsBarConfig.fromJson(legacy))
    }

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

    @Test fun sideRailStartAndEndDoNotTurnIntoTopOrBottomEdges() {
        for (edge in listOf(ToolbarPosition.LEFT, ToolbarPosition.RIGHT)) {
            val start = if (edge == ToolbarPosition.LEFT) ToolbarAnchor.TOP_LEFT else ToolbarAnchor.TOP_RIGHT
            val end = if (edge == ToolbarPosition.LEFT) ToolbarAnchor.BOTTOM_LEFT else ToolbarAnchor.BOTTOM_RIGHT
            for (anchor in listOf(start, end)) {
                val config = DocumentTabsBarConfig(position = edge, anchor = anchor)
                val restored = DocumentTabsBarConfig.fromJson(config.toJson())
                assertEquals(edge, restored.position)
                assertEquals(anchor, restored.anchor)
                assertTrue(restored.position.vertical)
            }
        }
    }

    @Test fun edgesAndAlignmentSurviveTwoWayPlacementChanges() {
        for (edge in ToolbarPosition.entries)
            for (alignment in com.xnotes.core.tools.ToolbarDockAlignment.entries) {
                val next = DocumentTabsBarConfig().movedTo(edge, alignment)
                assertEquals(edge, next.position)
                assertEquals(alignment, next.alignment())
                assertEquals(next, DocumentTabsBarConfig.fromJson(next.toJson()))
                assertEquals(next.copy(floating = false), next.withFloating(false))
                assertEquals(next, next.withFloating(false).withFloating(true))
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

    @Test fun unknownOnlyNavigationBarsAreIgnoredSafely() {
        val workspace = JSONObject().put("version", 6)
            .put("note", JSONObject().put("bars", JSONArray()
                .put(bar("main", JSONArray().put(entry("document_tabs")).put(entry("pen", 1))))
                .put(bar("old-tabs", JSONArray().put(entry("document_tabs"))))))
            .put("canvas", JSONObject().put("bars", JSONArray()
                .put(bar("canvas-main", JSONArray().put(entry("home"))))
                .put(bar("old-tabs-only", JSONArray().put(entry("document_tabs"))))))
        val loaded = Settings.fromJson(JSONObject().put("toolbar_workspaces", workspace)).effectiveToolbarWorkspaces()
        assertEquals(listOf("main"), loaded.note.bars.map { it.id })
        assertEquals(listOf(ToolbarItem.PEN), loaded.note.bars.single().layout.sections.single().entries.map { it.item })
        // Unreleased tab-bar aliases are ignored; valid current-schema lane
        // metadata is preserved instead of applying an experimental migration.
        assertEquals(2, loaded.note.bars.single().layout.sections.single().lanes)
        assertEquals(1, loaded.note.bars.single().layout.sections.single().entries.single().lane)
        assertEquals(300, loaded.note.bars.single().geometry.widthDp)
        assertEquals(listOf("canvas-main"), loaded.canvas.bars.map { it.id })
        assertTrue(loaded.canvas.bars.single().layout.sections.any { s -> s.entries.any { it.item == ToolbarItem.HOME } })
        assertEquals(loaded, Settings.fromJson(JSONObject(Settings(toolbarWorkspaces = loaded).toJson().toString())).effectiveToolbarWorkspaces())
    }

    @Test fun unknownOnlyWorkspaceFallsBackToOriginalToolbars() {
        val invalid = JSONObject().put("version", 6)
            .put("note", JSONObject().put("bars", JSONArray().put(
                bar("old-tabs", JSONArray().put(entry("document_tabs"))))))
            .put("canvas", JSONObject().put("bars", JSONArray().put(
                bar("old-tabs", JSONArray().put(entry("document_tabs"))))))
        val loaded = Settings.fromJson(JSONObject().put("toolbar_workspaces", invalid))
            .effectiveToolbarWorkspaces()
        assertEquals(ToolbarLayout.DEFAULT, loaded.note.bars.single().layout)
        assertEquals(ToolbarLayout.CANVAS_DEFAULT, loaded.canvas.bars.single().layout)
    }

    @Test fun smallWindowsClampInsteadOfPlacingNavigationOffScreen() {
        val placed = documentTabsOffset(Size(100f, 50f), Size(360f, 120f), ToolbarAnchor.BOTTOM_RIGHT)
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
        val strip = documentTabsFloatingStrip(host, ToolbarAnchor.TOP_LEFT, ToolbarPosition.TOP, neighbors, 1f)
        assertEquals(8f, strip.left, 0f)
        assertEquals(792f, strip.right, 0f)
        assertEquals(8f, strip.top, 0f)
        assertFalse(neighbors.any { it.overlaps(strip) })
        val rail = documentTabsFloatingStrip(host, ToolbarAnchor.CENTER_RIGHT, ToolbarPosition.RIGHT,
            listOf(Rect(1000f, 600f, 1200f, 800f)), 1f)
        assertEquals(592f, rail.bottom, 0f)
        assertEquals(1192f, rail.right, 0f)
    }

    @Test fun fullyOccupiedEdgeUsesTheFirstClearBand() {
        val host = Size(1200f, 800f)
        val obstacles = listOf(Rect(0f, 0f, 1200f, 60f))
        val strip = documentTabsFloatingStrip(host, ToolbarAnchor.TOP_CENTER, ToolbarPosition.TOP, obstacles, 1f)
        assertEquals(Rect(8f, 68f, 1192f, 128f), strip)
        assertFalse(obstacles.any { it.overlaps(strip) })
    }

    @Test fun floatingStripSkipsACloserGapThatIsTooSmall() {
        val obstacles = listOf(Rect(60f, 8f, 420f, 70f))
        val strip = documentTabsFloatingStrip(
            Size(700f, 440f), ToolbarAnchor.TOP_LEFT, ToolbarPosition.TOP, obstacles, 1f,
        )
        assertEquals(Rect(428f, 8f, 692f, 68f), strip)
    }

}