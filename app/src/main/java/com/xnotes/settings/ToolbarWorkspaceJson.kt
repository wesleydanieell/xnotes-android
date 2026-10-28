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
import com.xnotes.core.tools.sanitized
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned settings-only codec. Never write toolbar workspace state into document files.
 * Deliberately does not use ToolbarLayout.fromRaw(): it appends all missing tools, which is
 * correct for migrating a legacy single bar but wrong for a secondary/compact bar.
 */
internal object ToolbarWorkspaceJson {
    private const val VERSION = 3
    private const val MAX_BARS = 32
    private const val MAX_SECTIONS = 32
    private const val MAX_ENTRIES = 128

    fun write(value: ToolbarWorkspaces): JSONObject = JSONObject()
        .put("version", VERSION)
        .put("shared", value.shared)
        .put("note", writeWorkspace(value.note))
        .put("canvas", writeWorkspace(value.canvas))

    fun read(value: JSONObject?): ToolbarWorkspaces? {
        val version = value?.optInt("version", -1) ?: return null
        if (version !in 1..VERSION) return null
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
                .apply { bar.nodes?.let { put("nodes", JSONArray().apply { it.forEach { node -> put(writeNode(node)) } }) } }
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
                nodes = readNodes(raw.optJSONArray("nodes")),
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

    private fun writeNode(node: com.xnotes.core.tools.ToolbarNode): JSONObject = JSONObject().apply {
        put("id", node.id)
        put("visible", node.visible)
        when (node) {
            is com.xnotes.core.tools.ToolbarNode.Tool -> {
                put("type", "tool").put("item", node.item.id).put("visual_dp", node.visualDp)
                    .put("hitbox_dp", node.hitboxDp).put("spacing_dp", node.spacingDp).put("priority", node.priority)
            }
            is com.xnotes.core.tools.ToolbarNode.ColorPalette -> put("type", "colors").put("count", node.count).put("columns", node.columns)
            is com.xnotes.core.tools.ToolbarNode.Group -> {
                put("type", "group").put("collapsible", node.collapsible).put("expanded", node.expanded)
                put("children", JSONArray().apply { node.children.forEach { put(writeNode(it)) } })
            }
            is com.xnotes.core.tools.ToolbarNode.Grid -> {
                put("type", "grid").put("columns", node.columns).put("rows", node.rows)
                put("children", JSONArray().apply { node.children.forEach { put(writeNode(it)) } })
            }
            is com.xnotes.core.tools.ToolbarNode.Spacer -> put("type", "spacer").put("size_dp", node.sizeDp)
            is com.xnotes.core.tools.ToolbarNode.Separator -> put("type", "separator")
        }
    }

    private fun readNodes(raw: JSONArray?): List<com.xnotes.core.tools.ToolbarNode>? {
        if (raw == null) return null
        val ids = HashSet<String>()
        return (0 until minOf(raw.length(), 128)).mapNotNull { readNode(raw.optJSONObject(it))?.sanitized(ids = ids) }
            .takeIf { it.isNotEmpty() }
    }

    private fun readNode(raw: JSONObject?): com.xnotes.core.tools.ToolbarNode? {
        if (raw == null) return null
        val id = raw.optString("id").trim().takeIf { it.isNotEmpty() } ?: return null
        val visible = raw.optBoolean("visible", true)
        val type = raw.optString("type")
        fun children(): List<com.xnotes.core.tools.ToolbarNode> = raw.optJSONArray("children")?.let { array ->
            (0 until minOf(array.length(), 64)).mapNotNull { readNode(array.optJSONObject(it)) }
        } ?: emptyList()
        return when (type) {
            "tool" -> com.xnotes.core.tools.ToolbarNode.Tool(id, ToolbarItem.fromId(raw.optString("item")) ?: return null, visible, raw.optInt("visual_dp").takeIf { raw.has("visual_dp") }, raw.optInt("hitbox_dp").takeIf { raw.has("hitbox_dp") }, raw.optInt("spacing_dp", 0), raw.optInt("priority", 0))
            "colors" -> com.xnotes.core.tools.ToolbarNode.ColorPalette(id, raw.optInt("count").takeIf { raw.has("count") }, raw.optInt("columns", 1), visible)
            "group" -> com.xnotes.core.tools.ToolbarNode.Group(id, children(), raw.optBoolean("collapsible", false), raw.optBoolean("expanded", true), visible)
            "grid" -> com.xnotes.core.tools.ToolbarNode.Grid(id, children(), raw.optInt("columns", 2), raw.optInt("rows").takeIf { raw.has("rows") }, visible)
            "spacer" -> com.xnotes.core.tools.ToolbarNode.Spacer(id, raw.optInt("size_dp", 8), visible)
            "separator" -> com.xnotes.core.tools.ToolbarNode.Separator(id, visible)
            else -> null
        }
    }
}
