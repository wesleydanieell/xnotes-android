package com.xnotes.settings

import com.xnotes.core.tools.ToolbarAnchor
import org.json.JSONObject

/** App navigation is independent of editable toolbars and has exactly two modes. */
data class DocumentTabsBarConfig(
    val floating: Boolean = true,
    val position: ToolbarPosition = ToolbarPosition.TOP,
    val anchor: ToolbarAnchor = ToolbarAnchor.TOP_CENTER,
) {
    fun movedTo(target: ToolbarAnchor): DocumentTabsBarConfig {
        val safe = target.takeUnless { it == ToolbarAnchor.CENTER } ?: ToolbarAnchor.TOP_CENTER
        return copy(anchor = safe, position = edgeOf(safe))
    }

    fun withFloating(value: Boolean): DocumentTabsBarConfig = if (value == floating) this else {
        if (value) copy(floating = true, anchor = anchorOf(position))
        else copy(floating = false, position = edgeOf(anchor))
    }

    fun toJson(): JSONObject = JSONObject().put("floating", floating)
        .put("position", position.id).put("anchor", anchor.name)

    companion object {
        fun fromJson(raw: JSONObject?): DocumentTabsBarConfig {
            if (raw == null) return DocumentTabsBarConfig()
            val anchor = ToolbarAnchor.entries.firstOrNull { it.name == raw.optString("anchor") }
                ?.takeUnless { it == ToolbarAnchor.CENTER } ?: ToolbarAnchor.TOP_CENTER
            return DocumentTabsBarConfig(raw.optBoolean("floating", true),
                ToolbarPosition.fromId(raw.optString("position")), anchor)
        }
        fun anchorOf(position: ToolbarPosition): ToolbarAnchor = when (position) {
            ToolbarPosition.TOP -> ToolbarAnchor.TOP_CENTER
            ToolbarPosition.BOTTOM -> ToolbarAnchor.BOTTOM_CENTER
            ToolbarPosition.LEFT -> ToolbarAnchor.CENTER_LEFT
            ToolbarPosition.RIGHT -> ToolbarAnchor.CENTER_RIGHT
        }
        fun edgeOf(anchor: ToolbarAnchor): ToolbarPosition = when (anchor) {
            ToolbarAnchor.BOTTOM_LEFT, ToolbarAnchor.BOTTOM_CENTER, ToolbarAnchor.BOTTOM_RIGHT -> ToolbarPosition.BOTTOM
            ToolbarAnchor.CENTER_LEFT -> ToolbarPosition.LEFT
            ToolbarAnchor.CENTER_RIGHT -> ToolbarPosition.RIGHT
            else -> ToolbarPosition.TOP
        }
    }
}
