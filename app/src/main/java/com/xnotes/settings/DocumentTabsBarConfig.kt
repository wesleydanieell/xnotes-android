package com.xnotes.settings

import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarDockAlignment
import org.json.JSONObject

enum class DocumentTabsSizing { DYNAMIC, MANUAL }

/** App navigation is independent of editable toolbars and has exactly two modes. */
data class DocumentTabsBarConfig(
    val floating: Boolean = true,
    val position: ToolbarPosition = ToolbarPosition.TOP,
    val anchor: ToolbarAnchor = ToolbarAnchor.TOP_CENTER,
    val sizing: DocumentTabsSizing = DocumentTabsSizing.DYNAMIC,
    val manualExtentDp: Int = 360,
    val minExtentDp: Int = 160,
    val maxExtentDp: Int = 0, // Zero means the available window extent.

) {
    fun movedTo(target: ToolbarAnchor): DocumentTabsBarConfig {
        val safe = target.takeUnless { it == ToolbarAnchor.CENTER } ?: ToolbarAnchor.TOP_CENTER
        return copy(anchor = safe, position = edgeOf(safe))
    }

    fun alignment(): ToolbarDockAlignment = when (position) {
        ToolbarPosition.TOP, ToolbarPosition.BOTTOM -> when (anchor) {
            ToolbarAnchor.TOP_LEFT, ToolbarAnchor.BOTTOM_LEFT -> ToolbarDockAlignment.START
            ToolbarAnchor.TOP_RIGHT, ToolbarAnchor.BOTTOM_RIGHT -> ToolbarDockAlignment.END
            else -> ToolbarDockAlignment.CENTER
        }
        ToolbarPosition.LEFT, ToolbarPosition.RIGHT -> when (anchor) {
            ToolbarAnchor.TOP_LEFT, ToolbarAnchor.TOP_CENTER, ToolbarAnchor.TOP_RIGHT -> ToolbarDockAlignment.START
            ToolbarAnchor.BOTTOM_LEFT, ToolbarAnchor.BOTTOM_CENTER, ToolbarAnchor.BOTTOM_RIGHT -> ToolbarDockAlignment.END
            else -> ToolbarDockAlignment.CENTER
        }
    }

    fun movedTo(position: ToolbarPosition, alignment: ToolbarDockAlignment): DocumentTabsBarConfig {
        val anchor = when (position) {
            ToolbarPosition.TOP -> when (alignment) {
                ToolbarDockAlignment.START -> ToolbarAnchor.TOP_LEFT
                ToolbarDockAlignment.CENTER -> ToolbarAnchor.TOP_CENTER
                ToolbarDockAlignment.END -> ToolbarAnchor.TOP_RIGHT
            }
            ToolbarPosition.BOTTOM -> when (alignment) {
                ToolbarDockAlignment.START -> ToolbarAnchor.BOTTOM_LEFT
                ToolbarDockAlignment.CENTER -> ToolbarAnchor.BOTTOM_CENTER
                ToolbarDockAlignment.END -> ToolbarAnchor.BOTTOM_RIGHT
            }
            ToolbarPosition.LEFT -> when (alignment) {
                ToolbarDockAlignment.START -> ToolbarAnchor.TOP_LEFT
                ToolbarDockAlignment.CENTER -> ToolbarAnchor.CENTER_LEFT
                ToolbarDockAlignment.END -> ToolbarAnchor.BOTTOM_LEFT
            }
            ToolbarPosition.RIGHT -> when (alignment) {
                ToolbarDockAlignment.START -> ToolbarAnchor.TOP_RIGHT
                ToolbarDockAlignment.CENTER -> ToolbarAnchor.CENTER_RIGHT
                ToolbarDockAlignment.END -> ToolbarAnchor.BOTTOM_RIGHT
            }
        }
        return copy(position = position, anchor = anchor)
    }

    fun withFloating(value: Boolean): DocumentTabsBarConfig = if (value == floating) this else {
        if (value) copy(floating = true, anchor = anchorOf(position))
        else copy(floating = false, position = edgeOf(anchor))
    }

    fun expandedExtent(availableDp: Float): Float {
        val available = availableDp.coerceAtLeast(0f)
        val minimum = minExtentDp.coerceIn(80, 2000).toFloat().coerceAtMost(available)
        val maximum = (if (maxExtentDp <= 0) available else maxExtentDp.toFloat())
            .coerceIn(minimum, available)
        return if (sizing == DocumentTabsSizing.DYNAMIC) maximum
            else manualExtentDp.toFloat().coerceIn(minimum, maximum)
    }

    fun toJson(): JSONObject = JSONObject().put("floating", floating)
        .put("position", position.id).put("anchor", anchor.name)
        .put("sizing", sizing.name).put("manual_extent_dp", manualExtentDp)
        .put("min_extent_dp", minExtentDp).put("max_extent_dp", maxExtentDp)

    companion object {
        fun fromJson(raw: JSONObject?): DocumentTabsBarConfig {
            if (raw == null) return DocumentTabsBarConfig()
            val anchor = ToolbarAnchor.entries.firstOrNull { it.name == raw.optString("anchor") }
                ?.takeUnless { it == ToolbarAnchor.CENTER } ?: ToolbarAnchor.TOP_CENTER
            return DocumentTabsBarConfig(raw.optBoolean("floating", true),
                ToolbarPosition.fromId(raw.optString("position")), anchor,
                DocumentTabsSizing.entries.firstOrNull { it.name == raw.optString("sizing") } ?: DocumentTabsSizing.DYNAMIC,
                raw.optInt("manual_extent_dp", 360).coerceIn(80, 4000),
                raw.optInt("min_extent_dp", 160).coerceIn(80, 2000),
                raw.optInt("max_extent_dp", 0).coerceIn(0, 4000))
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
