package com.xnotes.settings

import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.model.Rgba
import com.xnotes.core.tools.ToolbarBar
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

/**
 * Versioned settings-only codec. Never write toolbar workspace state into document files.
 * Deliberately does not use ToolbarLayout.fromRaw(): it appends all missing tools, which is
 * correct for migrating a legacy single bar but wrong for a secondary/compact bar.
 */
internal object ToolbarWorkspaceJson {
    private const val VERSION = 1
    private const val MAX_BARS = 32
    private const val MAX_SECTIONS = 32
    private const val MAX_ENTRIES = 128

    fun write(value: ToolbarWorkspaces): JSONObject = JSONObject()
        .put("version", VERSION)
        .put("shared", value.shared)
        .put("note", writeWorkspace(value.note))
        .put("canvas", writeWorkspace(value.canvas))

    fun read(value: JSONObject?): ToolbarWorkspaces? {
        if (value?.optInt("version", -1) != VERSION) return null
        val note = readWorkspace(value.optJSONObject("note"), ToolbarLayout.NOTE_ITEMS) ?: return null
        val canvas = readWorkspace(value.optJSONObject("canvas"), ToolbarLayout.CANVAS_ITEMS) ?: return null
        return ToolbarWorkspaces(note, canvas, shared = value.optBoolean("shared", false))
    }

    private fun writeWorkspace(workspace: ToolbarWorkspace): JSONObject =
        JSONObject().put("bars", JSONArray().apply {
            for (bar in workspace.bars) put(JSONObject()
                .put("id", bar.id)
                .put("name", bar.name)
                .put("visible", bar.visible)
                .put("color_count", bar.colorCount)
        .put("geometry", writeGeometry(bar.geometry))
                .put("sections", JSONArray().apply {
                    for (section in bar.layout.sections) put(JSONArray().apply {
                        for (entry in section.entries) put(JSONObject()
                            .put("id", entry.item.id)
                            .put("visible", entry.visible))
                    })
                }))
        })

    private fun writeGeometry(g: ToolbarGeometry): JSONObject = JSONObject()
        .put("anchor", g.anchor.name)
        .put("scale", g.scale.name)
        .put("floating", g.floating)
        .put("movable", g.movable)
        .put("columns", g.columns)
        .put("appearance", writeAppearance(g.appearance))
        .apply {
            g.widthDp?.let { put("width_dp", it) }
            g.heightDp?.let { put("height_dp", it) }
        }

    private fun readWorkspace(value: JSONObject?, available: Set<ToolbarItem>): ToolbarWorkspace? {
        val rawBars = value?.optJSONArray("bars") ?: return null
        if (rawBars.length() == 0) return null
        val ids = HashSet<String>()
        val bars = ArrayList<ToolbarBar>()
        for (index in 0 until minOf(rawBars.length(), MAX_BARS)) {
            val raw = rawBars.optJSONObject(index) ?: continue
            val id = raw.optString("id").trim()
            if (id.isEmpty() || id.length > 64 || !ids.add(id)) continue
            val rawSections = raw.optJSONArray("sections") ?: continue
            val seen = HashSet<ToolbarItem>()
            val sections = ArrayList<ToolbarSection>()
            for (i in 0 until minOf(rawSections.length(), MAX_SECTIONS)) {
                val entries = ArrayList<ToolbarEntry>()
                val rawEntries = rawSections.optJSONArray(i) ?: continue
                for (j in 0 until minOf(rawEntries.length(), MAX_ENTRIES)) {
                    val entry = rawEntries.optJSONObject(j) ?: continue
                    val item = ToolbarItem.fromId(entry.optString("id")) ?: continue
                    if (item !in available || !seen.add(item)) continue
                    entries.add(ToolbarEntry(item, entry.optBoolean("visible", true)))
                }
                sections.add(ToolbarSection(entries))
            }
            bars.add(ToolbarBar(
                id = id,
                name = raw.optString("name", "Toolbar").take(64),
                layout = ToolbarLayout(sections.ifEmpty { listOf(ToolbarSection(emptyList())) }),
                visible = raw.optBoolean("visible", true),
                geometry = readGeometry(raw.optJSONObject("geometry")),
                colorCount = raw.optInt("color_count").takeIf { raw.has("color_count") }?.coerceIn(1, com.xnotes.core.tools.InkPalette.MAX_SWATCHES),
            ))
        }
        return bars.takeIf { it.isNotEmpty() }?.let(::ToolbarWorkspace)
    }

    private fun readGeometry(raw: JSONObject?): ToolbarGeometry {
        if (raw == null) return ToolbarGeometry()
        val anchor = ToolbarAnchor.entries.firstOrNull { it.name == raw.optString("anchor") }
            ?: ToolbarAnchor.TOP_CENTER
        val scale = ToolbarScale.entries.firstOrNull { it.name == raw.optString("scale") }
            ?: ToolbarScale.REGULAR
        return ToolbarGeometry(
            anchor = anchor,
            scale = scale,
            floating = raw.optBoolean("floating", false),
            movable = raw.optBoolean("movable", true),
            widthDp = raw.optInt("width_dp").takeIf { raw.has("width_dp") },
            heightDp = raw.optInt("height_dp").takeIf { raw.has("height_dp") },
            columns = raw.optInt("columns", 1),
            appearance = readAppearance(raw.optJSONObject("appearance")),
        ).constrained()
    }

    private fun writeAppearance(a: com.xnotes.core.tools.ToolbarAppearance): JSONObject = JSONObject()
        .put("background", a.background?.let(::writeColor))
        .put("opacity", a.opacity.toDouble())
        .put("border_enabled", a.borderEnabled)
        .put("border_width_dp", a.borderWidthDp)
        .put("border_color", a.borderColor?.let(::writeColor))
        .put("corner_radius_dp", a.cornerRadiusDp)
        .put("padding_horizontal_dp", a.paddingHorizontalDp)
        .put("padding_vertical_dp", a.paddingVerticalDp)
        .put("spacing_dp", a.spacingDp)
        .put("blur_radius_dp", a.blurRadiusDp)

    private fun readAppearance(raw: JSONObject?): com.xnotes.core.tools.ToolbarAppearance {
        if (raw == null) return com.xnotes.core.tools.ToolbarAppearance()
        return com.xnotes.core.tools.ToolbarAppearance(
            background = readColor(raw.optJSONArray("background")),
            opacity = raw.optDouble("opacity", 1.0).toFloat(),
            borderEnabled = raw.optBoolean("border_enabled", true),
            borderWidthDp = raw.optInt("border_width_dp", 1),
            borderColor = readColor(raw.optJSONArray("border_color")),
            cornerRadiusDp = raw.optInt("corner_radius_dp", 24),
            paddingHorizontalDp = raw.optInt("padding_horizontal_dp", 4),
            paddingVerticalDp = raw.optInt("padding_vertical_dp", 4),
            spacingDp = raw.optInt("spacing_dp", 0),
            blurRadiusDp = raw.optInt("blur_radius_dp", 0),
        )
    }

    private fun writeColor(c: Rgba): JSONArray = JSONArray(c.toList())

    private fun readColor(a: JSONArray?): Rgba? = Rgba.fromList(a?.let { list ->
        (0 until list.length()).mapNotNull { index -> list.optInt(index).takeIf { !list.isNull(index) } }
    })
}
