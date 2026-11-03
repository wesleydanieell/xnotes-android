package com.xnotes.core.tools

/**
 * User-customisable toolbar layout: ordered [sections], each an ordered list of [ToolbarEntry].
 * Pure model (no Android, no JSON) so the add/remove/move/migration logic is unit-testable on
 * the JVM; the settings layer persists it and the UI renders from it.
 */

/** Every atomic, movable toolbar element. The three block ids each render a fixed multi-control
 *  cluster but move and hide as one unit. [id] is the persistence key (never rename without a
 *  migration); the tool ids match the matching [Tool.id]. */
enum class ToolbarItem(val id: String) {
    HOME("home"),
    TITLE("title"),
    SIDEBAR("sidebar"),
    PEN("pen"),
    DASHED("dashed"),
    CALLIGRAPHY("calligraphy"),
    SPEED("speed"),
    TAPER("taper"),
    HIGHLIGHTER("highlighter"),
    /** Optional group launcher. It is hidden by default so individual pens remain available. */
    UNIFIED_PEN("unified_pen"),
    ERASER("eraser"),
    PAN("pan"),
    SELECT("select"),
    LASSO("lasso"),
    SCREENSHOT("screenshot"),
    WAND("wand"),
    TAPE("tape"),
    SHAPE("shape"),
    RULER("ruler"),
    TEXT("text"),
    TEXT_BOX("text_box"),

    /** The text markup tool; only notes with a PDF show it. */
    MARKUP("markup"),
    IMAGE("image"),
    UNDO("undo"),
    REDO("redo"),
    PAGE_NAV("page_nav"),
    STYLES("styles"),
    MARGINS("margins"),
    VIEW("view"),
    ZOOM("zoom"),
    FIT("fit"),
    ZOOM_LOCK("zoom_lock"),
    FULLSCREEN("fullscreen"),
    COLORS("colors"),

    /** Canvas only: saved views, which are what page numbers are on an unbounded surface. */
    WAYPOINTS("waypoints"),

    /** Canvas only: the overview map in the corner. */
    MINIMAP("minimap");

    companion object {
        fun fromId(id: String?): ToolbarItem? = entries.firstOrNull { it.id == id }
    }
}

data class ToolbarEntry(val item: ToolbarItem, val visible: Boolean = true)

/** A section is either linear (0), or a compact group with 1–3 columns and at most 2 rows.
 * Only single-button tools can occupy a grid cell: composite controls need their own section. */
data class ToolbarSection(val entries: List<ToolbarEntry>, val gridColumns: Int = 0) {
    val visibleEntries: List<ToolbarEntry> get() = entries.filter { it.visible }
    val hasVisible: Boolean get() = entries.any { it.visible }
    val gridCapacity: Int get() = if (gridColumns in 1..3) gridColumns * 2 else Int.MAX_VALUE
    val crossCells: Int get() = if (gridColumns in 1..3)
        ((visibleEntries.size + gridColumns - 1) / gridColumns).coerceIn(1, 2) else 1

    fun supportsGrid(columns: Int): Boolean = columns in 1..3 &&
        entries.size <= columns * 2 && entries.all { it.item.supportsGridCell }

    fun normalized(): ToolbarSection = if (gridColumns == 0 || supportsGrid(gridColumns)) this
        else copy(gridColumns = 0)
}

/** Labels and multi-control clusters cannot be shrunk into one grid button. */
val ToolbarItem.supportsGridCell: Boolean get() = this !in setOf(
    ToolbarItem.TITLE, ToolbarItem.COLORS, ToolbarItem.PAGE_NAV, ToolbarItem.ZOOM,
)

data class ToolbarLayout(val sections: List<ToolbarSection>) {

    /** Sections that paint on the real bar; a separator goes between consecutive ones. */
    val visibleSections: List<ToolbarSection> get() = sections.filter { it.hasVisible }

    fun addSection(): ToolbarLayout = copy(sections = sections + ToolbarSection(emptyList()))

    /** Invalid size/content combinations are ignored, so existing tools are never lost. */
    fun setSectionGrid(sectionIndex: Int, columns: Int): ToolbarLayout {
        if (columns !in 0..3) return this
        val section = sections.getOrNull(sectionIndex) ?: return this
        if (columns != 0 && !section.supportsGrid(columns)) return this
        return copy(sections = sections.toMutableList().also { it[sectionIndex] = section.copy(gridColumns = columns) })
    }

    /**
     * Opt in to the unified pen without changing standalone pen visibility or introducing
     * unrelated tools into a deliberately minimal secondary toolbar.
     */
    fun withUnifiedPenVisible(): ToolbarLayout {
        for ((sectionIndex, section) in sections.withIndex()) {
            val entryIndex = section.entries.indexOfFirst { it.item == ToolbarItem.UNIFIED_PEN }
            if (entryIndex >= 0) {
                if (section.entries[entryIndex].visible) return this
                val entries = section.entries.toMutableList()
                entries[entryIndex] = entries[entryIndex].copy(visible = true)
                val updated = sections.toMutableList()
                updated[sectionIndex] = section.copy(entries = entries)
                return copy(sections = updated)
            }
        }
        val anchorCandidates = listOf(
            ToolbarItem.HIGHLIGHTER,
            ToolbarItem.TAPER,
            ToolbarItem.SPEED,
            ToolbarItem.CALLIGRAPHY,
            ToolbarItem.DASHED,
            ToolbarItem.PEN,
        )
        for (anchor in anchorCandidates) {
            for ((sectionIndex, section) in sections.withIndex()) {
                val position = section.entries.indexOfFirst { it.item == anchor }
                if (position >= 0) {
                    val entries = section.entries.toMutableList()
                    entries.add(position + 1, ToolbarEntry(ToolbarItem.UNIFIED_PEN))
                    val updated = sections.toMutableList()
                    updated[sectionIndex] = section.copy(entries = entries).normalized()
                    return copy(sections = updated)
                }
            }
        }
        if (sections.isEmpty()) return ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(ToolbarItem.UNIFIED_PEN)))))
        val last = sections.last()
        return copy(sections = sections.dropLast(1) + last.copy(entries = last.entries + ToolbarEntry(ToolbarItem.UNIFIED_PEN)).normalized())
    }

    /** Hiding the group never hides any standalone pen or removes its saved placement. */
    fun withUnifiedPenHidden(): ToolbarLayout {
        sections.forEachIndexed { sectionIndex, section ->
            val entryIndex = section.entries.indexOfFirst { it.item == ToolbarItem.UNIFIED_PEN }
            if (entryIndex >= 0) {
                return if (section.entries[entryIndex].visible) toggleVisible(sectionIndex, entryIndex) else this
            }
        }
        return this
    }

    /**
     * Remove section [i], merging its entries into the previous section, or the next one when [i]
     * is the first. Order and visibility are preserved. No-op when only one section remains.
     */
    fun removeSection(i: Int): ToolbarLayout {
        if (sections.size <= 1 || i !in sections.indices) return this
        val target = if (i == 0) i + 1 else i - 1
        val moved = sections[i].entries
        val rebuilt = sections.mapIndexedNotNull { idx, sec ->
            when (idx) {
                i -> null
                target -> sec.copy(entries = if (target < i) sec.entries + moved else moved + sec.entries).normalized()
                else -> sec
            }
        }
        return copy(sections = rebuilt)
    }

    fun moveSection(from: Int, to: Int): ToolbarLayout {
        if (from !in sections.indices || to !in sections.indices || from == to) return this
        val list = sections.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(sections = list)
    }

    fun toggleVisible(sectionIndex: Int, entryIndex: Int): ToolbarLayout {
        val sec = sections.getOrNull(sectionIndex) ?: return this
        val e = sec.entries.getOrNull(entryIndex) ?: return this
        val entries = sec.entries.toMutableList().also { it[entryIndex] = e.copy(visible = !e.visible) }
        return copy(sections = sections.toMutableList().also { it[sectionIndex] = sec.copy(entries = entries) })
    }

    /**
     * Move the entry at ([fromSection], [fromIndex]) to ([toSection], [toIndex]), keeping its
     * visibility. The target index is interpreted against the layout after the source is removed.
     */
    fun moveItem(fromSection: Int, fromIndex: Int, toSection: Int, toIndex: Int): ToolbarLayout {
        val src = sections.getOrNull(fromSection) ?: return this
        val entry = src.entries.getOrNull(fromIndex) ?: return this
        if (toSection !in sections.indices) return this
        val destination = sections[toSection]
        if (fromSection != toSection && destination.gridColumns != 0 &&
            (destination.entries.size >= destination.gridCapacity || !entry.item.supportsGridCell)
        ) return this
        val mutable = sections.map { it.entries.toMutableList() }.toMutableList()
        mutable[fromSection].removeAt(fromIndex)
        val adjusted = if (toSection == fromSection && toIndex > fromIndex) toIndex - 1 else toIndex
        mutable[toSection].add(adjusted.coerceIn(0, mutable[toSection].size), entry)
        return copy(sections = mutable.mapIndexed { idx, e -> sections[idx].copy(entries = e) })
    }

    /** Add any of [among] missing from this layout, visible, so the bar never goes stale
     *  across versions: an item with a designated neighbour ([INSERT_AFTER]) slots in right
     *  after it; anything else is appended to the last section. */
    fun withMissingItemsAppended(among: Set<ToolbarItem> = NOTE_ITEMS): ToolbarLayout {
        var layout = this
        for (item in ToolbarItem.entries) {
            if (item !in among) continue
            if (layout.sections.any { s -> s.entries.any { it.item == item } }) continue
            layout = layout.insertMissing(item)
        }
        return layout
    }

    private fun insertMissing(item: ToolbarItem): ToolbarLayout {
        val anchor = INSERT_AFTER[item]
        val entry = ToolbarEntry(item, visible = item != ToolbarItem.UNIFIED_PEN)
        if (anchor != null) {
            sections.forEachIndexed { si, sec ->
                val i = sec.entries.indexOfFirst { it.item == anchor }
                if (i >= 0) {
                    val entries = sec.entries.toMutableList().also { it.add(i + 1, entry) }
                    return copy(sections = sections.toMutableList().also { it[si] = sec.copy(entries = entries).normalized() })
                }
            }
        }
        val last = sections.last()
        return copy(sections = sections.dropLast(1) + last.copy(entries = last.entries + entry).normalized())
    }

    companion object {
        /** Later-added items that belong beside an existing one in stored layouts. */
        private val INSERT_AFTER = mapOf(
            ToolbarItem.UNIFIED_PEN to ToolbarItem.HIGHLIGHTER,
            ToolbarItem.TEXT_BOX to ToolbarItem.TEXT,
            ToolbarItem.VIEW to ToolbarItem.STYLES,
            ToolbarItem.MARGINS to ToolbarItem.STYLES,
            ToolbarItem.WAND to ToolbarItem.LASSO,
            ToolbarItem.TAPE to ToolbarItem.WAND,
            ToolbarItem.MARKUP to ToolbarItem.TEXT_BOX,
        )

        /**
         * The two bars hold different things, so each has its own set and neither can be handed the
         * other's. Page navigation means nothing on an unbounded canvas, and a waypoint means
         * nothing in a paged note; an item outside a layout's set is dropped on load rather than
         * drawn as a dead chip.
         */
        val CANVAS_ITEMS: Set<ToolbarItem> = setOf(
            ToolbarItem.HOME, ToolbarItem.TITLE,
            ToolbarItem.PEN, ToolbarItem.DASHED, ToolbarItem.CALLIGRAPHY, ToolbarItem.SPEED,
            ToolbarItem.TAPER, ToolbarItem.HIGHLIGHTER, ToolbarItem.UNIFIED_PEN, ToolbarItem.ERASER,
            ToolbarItem.PAN, ToolbarItem.SELECT, ToolbarItem.LASSO, ToolbarItem.WAND,
            ToolbarItem.TAPE, ToolbarItem.SHAPE,
            ToolbarItem.IMAGE, ToolbarItem.COLORS, ToolbarItem.UNDO, ToolbarItem.REDO,
            ToolbarItem.STYLES, ToolbarItem.WAYPOINTS, ToolbarItem.MINIMAP,
            ToolbarItem.ZOOM, ToolbarItem.FIT, ToolbarItem.ZOOM_LOCK,
        )

        /** Everything the paged bar can hold: the whole enum bar the two canvas-only additions. */
        val NOTE_ITEMS: Set<ToolbarItem> =
            ToolbarItem.entries.toSet() - setOf(ToolbarItem.WAYPOINTS, ToolbarItem.MINIMAP)

        /** Mirrors the hardcoded bar exactly (see Toolbar.kt) so existing users see no change. */
        val DEFAULT: ToolbarLayout = of(
            listOf(ToolbarItem.HOME, ToolbarItem.TITLE),
            listOf(ToolbarItem.SIDEBAR),
            listOf(
                ToolbarItem.PEN, ToolbarItem.DASHED, ToolbarItem.CALLIGRAPHY, ToolbarItem.SPEED,
                ToolbarItem.TAPER, ToolbarItem.HIGHLIGHTER, ToolbarItem.UNIFIED_PEN, ToolbarItem.ERASER,
            ),
            listOf(ToolbarItem.PAN, ToolbarItem.SELECT, ToolbarItem.LASSO, ToolbarItem.SCREENSHOT),
            listOf(ToolbarItem.WAND, ToolbarItem.TAPE, ToolbarItem.SHAPE, ToolbarItem.RULER, ToolbarItem.TEXT, ToolbarItem.TEXT_BOX, ToolbarItem.MARKUP),
            listOf(ToolbarItem.IMAGE),
            listOf(ToolbarItem.COLORS),
            listOf(ToolbarItem.UNDO, ToolbarItem.REDO),
            listOf(ToolbarItem.PAGE_NAV, ToolbarItem.STYLES, ToolbarItem.MARGINS, ToolbarItem.VIEW),
            listOf(ToolbarItem.ZOOM, ToolbarItem.FIT, ToolbarItem.ZOOM_LOCK),
            listOf(ToolbarItem.FULLSCREEN),
        )

        /** Mirrors the hardcoded canvas bar (see InfiniteToolbar.kt). */
        val CANVAS_DEFAULT: ToolbarLayout = of(
            listOf(ToolbarItem.HOME, ToolbarItem.TITLE),
            listOf(
                ToolbarItem.PEN, ToolbarItem.DASHED, ToolbarItem.CALLIGRAPHY, ToolbarItem.SPEED,
                ToolbarItem.TAPER, ToolbarItem.HIGHLIGHTER, ToolbarItem.UNIFIED_PEN, ToolbarItem.ERASER,
            ),
            listOf(ToolbarItem.PAN, ToolbarItem.SELECT, ToolbarItem.LASSO),
            listOf(ToolbarItem.WAND, ToolbarItem.TAPE, ToolbarItem.SHAPE),
            listOf(ToolbarItem.IMAGE),
            listOf(ToolbarItem.COLORS),
            listOf(ToolbarItem.UNDO, ToolbarItem.REDO),
            listOf(ToolbarItem.STYLES, ToolbarItem.WAYPOINTS, ToolbarItem.MINIMAP),
            listOf(ToolbarItem.ZOOM, ToolbarItem.FIT, ToolbarItem.ZOOM_LOCK),
        )

        private fun of(vararg groups: List<ToolbarItem>): ToolbarLayout =
            ToolbarLayout(groups.map { g -> ToolbarSection(g.map { ToolbarEntry(it, visible = it != ToolbarItem.UNIFIED_PEN) }) })

        /**
         * Build from raw (id, visible) pairs per section as read from storage: ids outside [among]
         * are dropped, duplicates keep their first occurrence, empty input falls back to [fallback],
         * and any item of [among] not present is appended so the bar never goes stale across
         * versions.
         */
        fun fromRaw(
            rawSections: List<List<Pair<String, Boolean>>>,
            among: Set<ToolbarItem> = NOTE_ITEMS,
            fallback: ToolbarLayout = DEFAULT,
        ): ToolbarLayout {
            if (rawSections.isEmpty()) return fallback
            val seen = LinkedHashSet<ToolbarItem>()
            val sections = rawSections.map { raw ->
                ToolbarSection(
                    raw.mapNotNull { (id, visible) ->
                        val item = ToolbarItem.fromId(id) ?: return@mapNotNull null
                        if (item !in among) return@mapNotNull null
                        if (!seen.add(item)) return@mapNotNull null
                        ToolbarEntry(item, visible)
                    },
                )
            }
            if (sections.all { it.entries.isEmpty() }) return fallback
            return ToolbarLayout(sections).withMissingItemsAppended(among)
        }
    }
}
