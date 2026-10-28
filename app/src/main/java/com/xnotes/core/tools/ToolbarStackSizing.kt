package com.xnotes.core.tools

/** Upper bound for each bar in a scrollable anchor stack, in the caller's units. */
internal fun toolbarStackBarLimit(available: Float, count: Int, gap: Float): Float {
    if (!available.isFinite() || available <= 0f || count <= 0) return 0f
    if (count == 1) return available
    val safeGap = if (gap.isFinite()) gap.coerceAtLeast(0f) else 0f
    return (available - safeGap).coerceAtLeast(0f) / 2f
}
