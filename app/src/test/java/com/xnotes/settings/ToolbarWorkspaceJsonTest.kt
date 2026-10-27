package com.xnotes.settings

import com.xnotes.core.tools.ToolbarAnchor
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
}
