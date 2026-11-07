package com.xnotes.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.*
import com.xnotes.settings.Settings
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ToolbarDocumentTabsTest {
    private fun layout(visible: Boolean = true) = ToolbarLayout(listOf(
        ToolbarSection(listOf(ToolbarEntry(ToolbarItem.DOCUMENT_TABS, visible)), lanes = 2,
            region = ToolbarRegion.END),
    ), distribution = ToolbarDistribution.REGIONS)

    @Test fun existingProfilesDoNotGrowNavigationOrChangeTheirVisibleTools() {
        for ((default, allowed) in listOf(ToolbarLayout.DEFAULT to ToolbarLayout.NOTE_ITEMS,
            ToolbarLayout.CANVAS_DEFAULT to ToolbarLayout.CANVAS_ITEMS)) {
            assertFalse(default.visibleSections.any { section ->
                section.visibleEntries.any { it.item == ToolbarItem.DOCUMENT_TABS }
            })
            assertEquals(default, default.withMissingItemsAppended(allowed))
            val raw = default.sections.map { s -> s.entries.map { it.item.id to it.visible } }
            assertEquals(default, ToolbarLayout.fromRaw(raw, allowed, default))
        }
    }

    @Test fun tabsSurvivePersistenceCopyingAndSharedProjectionInBothEditors() {
        val bar = ToolbarBar("navigation", "Documents", layout(),
            geometry = ToolbarGeometry(anchor = ToolbarAnchor.BOTTOM_RIGHT, floating = true))
        val initial = ToolbarWorkspaces(ToolbarWorkspace(listOf(bar)),
            ToolbarWorkspace(listOf(bar.copy(id = "canvas-navigation"))), shared = true)
        val settings = Settings(toolbarWorkspaces = initial)
        val restored = Settings.fromJson(JSONObject(settings.toJson().toString())).effectiveToolbarWorkspaces()
        assertEquals(initial, restored)
        for (context in ToolbarContext.entries) {
            assertEquals(layout(), restored.forContext(context).bars.single().layout)
            val destination = ToolbarWorkspace.fromLegacy("primary", "Main", ToolbarLayout.CANVAS_DEFAULT)
            val copied = prepareToolbarTransfer(bar, destination, context)
            assertTrue(copied.omitted.isEmpty())
            assertEquals(layout(), copied.bar.layout)
        }
    }

    @Test fun hiddenNavigationDoesNotSuppressLegacyTabs() {
        val main = ToolbarBar("main", "Main", ToolbarLayout.DEFAULT)
        val navigation = ToolbarBar("tabs", "Tabs", layout())
        val workspace = ToolbarWorkspace(listOf(main, navigation))
        assertTrue(workspace.hasVisibleDocumentTabs())
        assertFalse(workspace.replaceBar(navigation.copy(visible = false)).hasVisibleDocumentTabs())
        assertFalse(workspace.replaceBar(navigation.copy(layout = layout(false))).hasVisibleDocumentTabs())
        assertFalse(workspace.removeBar("tabs").hasVisibleDocumentTabs())
    }

    @Test fun compositeUsesSameFootprintInPreviewAndFloatingPlan() {
        val bar = ToolbarBar("tabs", "Tabs", layout(), geometry = ToolbarGeometry(floating = true))
        val workspace = ToolbarWorkspace(listOf(bar))
        assertEquals(2, visibleTabSlots(DOCUMENT_TABS_WIDTH_DP - 36f, 2))
        val look = ToolbarLook(size = ToolbarSize.COMPACT, floating = true)
        val metrics = barMetrics(look.size)
        assertTrue(toolbarDepth(metrics, layout()) >= 42.dp + metrics.padding * 2)
        val plan = toolbarFloatingPlan(workspace, look, 800.dp, 600.dp, 5)
        val placement = requireNotNull(plan.placement("tabs"))
        assertTrue(placement.width >= DOCUMENT_TABS_WIDTH_DP)
        // At either side, only a launcher occupies the rail; the existing switcher stays upright.
        for (position in listOf(ToolbarPosition.LEFT, ToolbarPosition.RIGHT)) {
            val anchor = if (position == ToolbarPosition.LEFT) ToolbarAnchor.CENTER_LEFT else ToolbarAnchor.CENTER_RIGHT
            val vertical = toolbarFloatingPlan(ToolbarWorkspace(listOf(bar.copy(geometry = bar.geometry.copy(anchor = anchor)))),
                look.copy(position = position), 800.dp, 600.dp, 5)
            assertNotNull(vertical.placement("tabs"))
        }
    }

    @Test fun rejectedToolbarKeepsStandaloneNavigationAvailable() {
        val primary = ToolbarBar("main", "Main", ToolbarLayout(listOf(
            ToolbarSection(listOf(ToolbarEntry(ToolbarItem.PEN))),
        )))
        val tabs = ToolbarBar("tabs", "Tabs", layout(), geometry = ToolbarGeometry(
            anchor = ToolbarAnchor.BOTTOM_CENTER, floating = true, widthDp = 600))
        val blockers = (1..3).map { index -> primary.copy(id = "blocker-$index",
            geometry = ToolbarGeometry(anchor = ToolbarAnchor.BOTTOM_CENTER, floating = true, widthDp = 250)) }
        val workspace = ToolbarWorkspace(listOf(primary) + blockers + tabs)
        assertTrue(workspace.hasVisibleDocumentTabs())
        assertFalse(toolbarHasPlacedDocumentTabs(workspace, ToolbarLook(), 250.dp, 600.dp, 5))
        assertTrue(toolbarHasPlacedDocumentTabs(workspace, ToolbarLook(), 1200.dp, 800.dp, 5))
    }

    @Test fun stylusExclusionTracksEveryBarAndReleasesRemovedSurfaces() {
        val areas = ToolbarHitAreas()
        val first = Any()
        val second = Any()
        val left = Rect(0f, 0f, 100f, 40f)
        val right = Rect(200f, 0f, 300f, 40f)
        areas.update(first, left)
        areas.update(second, right)
        assertEquals(setOf(left, right), areas.bounds().toSet())
        assertFalse(areas.bounds().any { it.contains(androidx.compose.ui.geometry.Offset(150f, 20f)) })
        areas.remove(first)
        assertEquals(listOf(right), areas.bounds())
    }
}
