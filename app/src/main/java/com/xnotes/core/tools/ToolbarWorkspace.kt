package com.xnotes.core.tools

/**
 * An interface workspace contains multiple independently configured toolbars. The existing
 * [ToolbarLayout] still describes the tools *within* each bar, so old layouts can be carried
 * forward without changing the tool-dispatch or drawing models.
 *
 * Bar IDs are stable persistence keys and must not be derived from display names.
 */
enum class ToolbarAnchor {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
}

enum class ToolbarContext { NOTE, CANVAS }

/** Geometry is kept in logical dp; null dimensions mean wrap-content/automatic size. */
data class ToolbarGeometry(
    val anchor: ToolbarAnchor = ToolbarAnchor.TOP_CENTER,
    val floating: Boolean = false,
    val movable: Boolean = true,
    val widthDp: Int? = null,
    val heightDp: Int? = null,
    val columns: Int = 1,
) {
    /** Clamp untrusted persisted dimensions before rendering; do not force an exact size on auto bars. */
    fun constrained(): ToolbarGeometry = copy(
        widthDp = widthDp?.coerceIn(48, 2048),
        heightDp = heightDp?.coerceIn(48, 2048),
        columns = columns.coerceIn(1, 6),
    )
}

data class ToolbarBar(
    val id: String,
    val name: String,
    val layout: ToolbarLayout,
    val visible: Boolean = true,
    val geometry: ToolbarGeometry = ToolbarGeometry(),
)

/**
 * Pure, immutable multi-bar operations. Tools may appear on different bars simultaneously:
 * uniqueness across an entire workspace would make quick tool and color palettes impossible.
 * Each bar's existing [ToolbarLayout] remains independently editable.
 */
data class ToolbarWorkspace(val bars: List<ToolbarBar>) {
    fun addBar(bar: ToolbarBar): ToolbarWorkspace =
        if (bar.id.isBlank() || bars.any { it.id == bar.id }) this
        else copy(bars = bars + bar)

    fun duplicateBar(id: String, newId: String, name: String): ToolbarWorkspace {
        val source = bars.firstOrNull { it.id == id } ?: return this
        return addBar(source.copy(id = newId, name = name))
    }

    /** Keep a primary bar as a recovery path, even if all its items are hidden. */
    fun removeBar(id: String): ToolbarWorkspace =
        if (bars.size <= 1 || bars.none { it.id == id }) this
        else copy(bars = bars.filterNot { it.id == id })

    fun replaceBar(bar: ToolbarBar): ToolbarWorkspace {
        val index = bars.indexOfFirst { it.id == bar.id }
        if (index < 0) return this
        return copy(bars = bars.toMutableList().also { it[index] = bar })
    }

    fun moveBar(id: String, toIndex: Int): ToolbarWorkspace {
        val index = bars.indexOfFirst { it.id == id }
        if (index < 0 || toIndex !in bars.indices || index == toIndex) return this
        return copy(bars = bars.toMutableList().also { it.add(toIndex, it.removeAt(index)) })
    }

    /**
     * Filter context-specific tools without appending every "missing" tool to every bar.
     * [ToolbarLayout.fromRaw] does append them for legacy *single* bars, but that behavior
     * would produce duplicates when loading an intentionally minimal secondary bar.
     */
    fun onlyItems(allowed: Set<ToolbarItem>): ToolbarWorkspace = copy(
        bars = bars.map { bar ->
            bar.copy(layout = ToolbarLayout(bar.layout.sections.map { section ->
                section.copy(entries = section.entries.filter { it.item in allowed })
            }))
        },
    )

    companion object {
        fun fromLegacy(id: String, name: String, layout: ToolbarLayout): ToolbarWorkspace =
            ToolbarWorkspace(listOf(ToolbarBar(id = id, name = name, layout = layout)))
    }
}

/**
 * Two persisted profiles, with a switch to reuse the note profile on the canvas. Canvas-only
 * tools are filtered when sharing; the independent canvas profile stays intact for later reuse.
 */
data class ToolbarWorkspaces(
    val note: ToolbarWorkspace,
    val canvas: ToolbarWorkspace,
    val shared: Boolean = false,
) {
    fun forContext(context: ToolbarContext): ToolbarWorkspace = when (context) {
        ToolbarContext.NOTE ->
            note.onlyItems(ToolbarLayout.NOTE_ITEMS)
        ToolbarContext.CANVAS ->
            (if (shared) note else canvas).onlyItems(ToolbarLayout.CANVAS_ITEMS)
    }

    companion object {
        fun fromLegacy(
            noteLayout: ToolbarLayout,
            canvasLayout: ToolbarLayout,
        ) = ToolbarWorkspaces(
            note = ToolbarWorkspace.fromLegacy("note-primary", "Main toolbar", noteLayout),
            canvas = ToolbarWorkspace.fromLegacy("canvas-primary", "Main toolbar", canvasLayout),
        )
    }
}
