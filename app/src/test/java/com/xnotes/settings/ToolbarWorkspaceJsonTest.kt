package com.xnotes.settings

import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.model.Rgba
import com.xnotes.core.tools.ToolbarAppearance
import com.xnotes.core.tools.ToolbarNode
import com.xnotes.core.tools.flattenItems
import com.xnotes.core.tools.sanitized
import com.xnotes.core.tools.toolbarNodesFromLayout
import com.xnotes.core.tools.updateTool
import com.xnotes.core.tools.updateColorPalette
import com.xnotes.core.tools.updateGrid
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarContext
import com.xnotes.core.tools.ToolbarEntry
import com.xnotes.core.tools.ToolbarGeometry
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.core.tools.ToolbarSection
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.tools.ToolbarWorkspaces
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarWorkspaceJsonTest {
    private fun compactLayout(vararg items: ToolbarItem): ToolbarLayout =
        ToolbarLayout(listOf(ToolbarSection(items.map { ToolbarEntry(it) })))

    @Test fun oldSettingsMigrateWithoutChangingOriginalLayoutOrPlacement() {
        val legacy = Settings(
            toolbarLayout = ToolbarLayout.DEFAULT.moveSection(0, 2),
            canvasToolbarLayout = ToolbarLayout.CANVAS_DEFAULT.moveSection(0, 2),
            prefs = Preferences(toolbarLook = ToolbarLook(
                position = ToolbarPosition.RIGHT,
                size = ToolbarSize.COMPACT,
                floating = true,
            )),
        )
        val restored = Settings.fromJson(legacy.toJson())
        assertNull(restored.toolbarWorkspaces)
        assertEquals(legacy.toolbarLayout, restored.toolbarLayout)
        assertEquals(legacy.canvasToolbarLayout, restored.canvasToolbarLayout)
        val primary = restored.effectiveToolbarWorkspaces().note.bars.single()
        assertEquals(ToolbarAnchor.CENTER_RIGHT, primary.geometry.anchor)
        assertEquals(ToolbarScale.COMPACT, primary.geometry.scale)
        assertTrue(primary.geometry.floating)
        assertEquals(legacy.toolbarLayout, primary.layout)
    }

    @Test fun workspaceSettingsRoundTripKeepsSecondaryBarsAndIndependentProfiles() {
        val legacy = ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT)
        val second = ToolbarBar(
            id = "colors-1",
            name = "Quick colors",
            layout = compactLayout(ToolbarItem.COLORS, ToolbarItem.PEN),
            geometry = ToolbarGeometry(
                anchor = ToolbarAnchor.BOTTOM_RIGHT,
                floating = true,
                widthDp = 245,
                columns = 3,
            ),
        )
        val profiles = legacy.copy(note = legacy.note.addBar(second), shared = true)
        val persisted = Settings(toolbarWorkspaces = profiles)
        val restored = Settings.fromJson(JSONObject(persisted.toJson().toString()))
        assertEquals(profiles, restored.toolbarWorkspaces)
        assertEquals(2, restored.effectiveToolbarWorkspaces().note.bars.size)
        assertEquals(2, restored.effectiveToolbarWorkspaces().forContext(ToolbarContext.CANVAS).bars.size)
        assertEquals(245, restored.toolbarWorkspaces!!.note.bars[1].geometry.widthDp)
        assertEquals(null, restored.toolbarWorkspaces!!.note.bars[0].colorCount)
    }

    @Test fun perBarColourCountRoundTripsAndClamps() {
        val profiles = ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT)
            .copy(note = ToolbarWorkspace(listOf(
                ToolbarBar("primary", "Primary", ToolbarLayout.DEFAULT, colorCount = 99),
            )))
        val json = Settings(toolbarWorkspaces = profiles).toJson()
        val raw = json.getJSONObject("toolbar_workspaces").getJSONObject("note")
            .getJSONArray("bars").getJSONObject(0)
        raw.put("color_count", 99)
        val parsed = Settings.fromJson(json).toolbarWorkspaces!!.note.bars.single()
        assertEquals(com.xnotes.core.tools.InkPalette.MAX_SWATCHES, parsed.colorCount)
    }

    @Test fun unsupportedSchemaFallsBackToLegacySettingsSafely() {
        val json = Settings().toJson()
        json.put("toolbar_workspaces", ToolbarWorkspaceJson.write(
            ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT),
        ).put("version", 99))
        val restored = Settings.fromJson(json)
        assertNull(restored.toolbarWorkspaces)
        assertEquals(ToolbarLayout.DEFAULT, restored.effectiveToolbarWorkspaces().note.bars.single().layout)
    }

    @Test fun malformedAndWrongContextEntriesAreFilteredWithoutAppendingMissingTools() {
        val legacy = ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT)
        val json = ToolbarWorkspaceJson.write(legacy)
        val noteBars = json.getJSONObject("note").getJSONArray("bars")
        val duplicate = JSONObject(noteBars.getJSONObject(0).toString()).put("name", "collision")
        noteBars.put(duplicate)
        val canvas = json.getJSONObject("canvas").getJSONArray("bars").getJSONObject(0)
        canvas.put("sections", JSONArray().put(JSONArray()
            .put(JSONObject().put("id", "pen").put("visible", true))
            .put(JSONObject().put("id", "page_nav").put("visible", true))
            .put(JSONObject().put("id", "unrecognized").put("visible", true))
            .put(JSONObject().put("id", "pen").put("visible", true))))
        val parsed = ToolbarWorkspaceJson.read(json)!!
        assertEquals(1, parsed.note.bars.size)
        assertEquals(listOf(ToolbarItem.PEN), parsed.canvas.bars.single()
            .layout.sections.single().entries.map { it.item })
        assertFalse(parsed.shared)
    }

    @Test fun invalidDimensionValuesAreClampedDuringLoading() {
        val json = ToolbarWorkspaceJson.write(
            ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT),
        )
        val geometry = json.getJSONObject("note").getJSONArray("bars")
            .getJSONObject(0).getJSONObject("geometry")
        geometry.put("width_dp", -80)
        geometry.put("height_dp", 99999)
        geometry.put("columns", 999)
        val dimensions = ToolbarWorkspaceJson.read(json)!!.note.bars.single().geometry
        assertEquals(48, dimensions.widthDp)
        assertEquals(2048, dimensions.heightDp)
        assertEquals(6, dimensions.columns)
    }

    @Test fun appearanceRoundTripAndClampsUnsafeValues() {
        val appearance = ToolbarAppearance(
            background = Rgba(12, 34, 56, 220),
            opacity = 2f,
            borderEnabled = false,
            borderWidthDp = 99,
            borderColor = Rgba(1, 2, 3),
            cornerRadiusDp = -4,
            paddingHorizontalDp = 100,
            paddingVerticalDp = 7,
            spacingDp = 80,
            blurRadiusDp = 999,
        )
        val profiles = ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT)
            .copy(note = ToolbarWorkspace(listOf(
                ToolbarBar("primary", "Primary", ToolbarLayout.DEFAULT, geometry = ToolbarGeometry(appearance = appearance)),
            )))
        val restored = Settings.fromJson(Settings(toolbarWorkspaces = profiles).toJson()).toolbarWorkspaces!!
            .note.bars.single().geometry.appearance
        assertEquals(Rgba(12, 34, 56, 220), restored.background)
        assertEquals(1f, restored.opacity)
        assertEquals(false, restored.borderEnabled)
        assertEquals(24, restored.borderWidthDp)
        assertEquals(0, restored.cornerRadiusDp)
        assertEquals(96, restored.paddingHorizontalDp)
        assertEquals(48, restored.spacingDp)
        assertEquals(96, restored.blurRadiusDp)
    }

    @Test fun v3NodeTreeRoundTripsAndRejectsUnsafeDepth() {
        val nodes = listOf<ToolbarNode>(
            ToolbarNode.Group("tools", listOf(
                ToolbarNode.Tool("pen-node", ToolbarItem.PEN, visualDp = 28, hitboxDp = 48),
                ToolbarNode.Grid("grid", listOf(
                    ToolbarNode.Tool("colors-tool", ToolbarItem.COLORS),
                    ToolbarNode.Spacer("gap", 6),
                ), columns = 2),
            ), collapsible = true, expanded = true),
        )
        val profiles = ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT)
            .copy(note = ToolbarWorkspace(listOf(ToolbarBar("primary", "Primary", ToolbarLayout.DEFAULT, nodes = nodes))))
        val restored = Settings.fromJson(Settings(toolbarWorkspaces = profiles).toJson()).toolbarWorkspaces!!
            .note.bars.single().nodes!!
        assertEquals(nodes, restored)
        assertEquals(listOf(ToolbarItem.PEN, ToolbarItem.COLORS), restored.flatMap { it.flattenItems() })
        assertEquals(
            listOf(ToolbarItem.PEN, ToolbarItem.COLORS),
            ToolbarBar("primary", "Primary", ToolbarLayout.DEFAULT, nodes = restored).effectiveLayout()
                .sections.single().entries.map { it.item },
        )
        assertEquals(
            listOf(ToolbarItem.PEN, ToolbarItem.COLORS),
            toolbarNodesFromLayout(ToolbarLayout(listOf(ToolbarSection(listOf(
                ToolbarEntry(ToolbarItem.PEN), ToolbarEntry(ToolbarItem.COLORS),
            ))))).flatMap { it.flattenItems() },
        )
        val tooDeep = ToolbarNode.Group("d0", listOf(
            ToolbarNode.Group("d1", listOf(
                ToolbarNode.Group("d2", listOf(
                    ToolbarNode.Group("d3", listOf(
                        ToolbarNode.Group("d4", listOf(ToolbarNode.Tool("tool", ToolbarItem.PEN)))
                    ))
                ))
            ))
        ))
        val sanitized = tooDeep.sanitized()
        assertTrue((sanitized as ToolbarNode.Group).flattenItems().isEmpty())
        val penTree = listOf<ToolbarNode>(ToolbarNode.UnifiedPen("unified"))
        val penProfiles = ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT)
            .copy(note = ToolbarWorkspace(listOf(ToolbarBar("primary", "Primary", ToolbarLayout.DEFAULT, nodes = penTree))))
        val penRestored = Settings.fromJson(Settings(toolbarWorkspaces = penProfiles).toJson()).toolbarWorkspaces!!
            .note.bars.single().nodes!!
        assertEquals(ToolbarItem.PEN, penRestored.single().flattenItems().single())
        assertTrue(penRestored.single() is ToolbarNode.UnifiedPen)
        val gridBar = ToolbarBar(
            "grid", "Grid", ToolbarLayout.DEFAULT,
            geometry = ToolbarGeometry(columns = 1),
            nodes = listOf(ToolbarNode.Group("g", listOf(ToolbarNode.Grid("grid-node", emptyList(), columns = 3)))),
        )
        assertEquals(3, gridBar.effectiveColumns())
        val metrics = ToolbarBar(
            "metric", "Metric", ToolbarLayout.DEFAULT,
            nodes = listOf(ToolbarNode.Tool("pen-metric", ToolbarItem.PEN, visualDp = 30, hitboxDp = 56)),
        ).toolMetrics()
        assertEquals(30, metrics[com.xnotes.core.tools.Tool.PEN]?.visualDp)
        assertEquals(56, metrics[com.xnotes.core.tools.Tool.PEN]?.hitboxDp)
        val updatedNode = ToolbarNode.Tool("pen", ToolbarItem.PEN).updateTool(ToolbarItem.PEN) { it.copy(hitboxDp = 64) }
        assertEquals(64, (updatedNode as ToolbarNode.Tool).hitboxDp)
        val palette = ToolbarNode.Group("palette-group", listOf(ToolbarNode.ColorPalette("palette", columns = 2)))
        val updatedPalette = palette.updateColorPalette { it.copy(columns = 3) }
        assertEquals(3, (updatedPalette as ToolbarNode.Group).children.single().let { (it as ToolbarNode.ColorPalette).columns })
        assertEquals(3, ToolbarBar("palette", "Palette", ToolbarLayout.DEFAULT, nodes = listOf(updatedPalette)).effectiveColumns())
        val grid = ToolbarNode.Grid("grid", emptyList(), columns = 2, rows = 2)
        val updatedGrid = grid.updateGrid { it.copy(columns = 3, rows = 2) }
        assertEquals(3, (updatedGrid as ToolbarNode.Grid).columns)
        assertEquals(2, updatedGrid.rows)
    }
}
