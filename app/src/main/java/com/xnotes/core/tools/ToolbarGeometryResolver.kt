package com.xnotes.core.tools

/** Resolved logical dimensions for one toolbar before it is composed. */
data class ToolbarResolvedSize(
    val widthDp: Int?,
    val heightDp: Int?,
)

/**
 * Resolve optional manual dimensions without allowing persisted values to create an unusable
 * surface. Null remains automatic/wrap-content. The same clamp is used by renderer and preview.
 */
fun resolveToolbarSize(
    geometry: ToolbarGeometry,
    minDp: Int = 48,
    maxDp: Int = 2048,
): ToolbarResolvedSize {
    val min = minDp.coerceAtLeast(1)
    val max = maxDp.coerceAtLeast(min)
    fun clamp(value: Int?): Int? = value?.coerceIn(min, max)
    return ToolbarResolvedSize(clamp(geometry.widthDp), clamp(geometry.heightDp))
}
