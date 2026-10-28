package com.xnotes.core.tools

/** Apply an explicit resize delta in dp, converting automatic dimensions to stable manual bounds. */
fun resizeToolbarGeometry(
    geometry: ToolbarGeometry,
    deltaWidthDp: Float,
    deltaHeightDp: Float,
    defaultWidthDp: Int = 200,
    defaultHeightDp: Int = 64,
): ToolbarGeometry = geometry.copy(
    widthDp = (geometry.widthDp ?: defaultWidthDp) + deltaWidthDp.toInt(),
    heightDp = (geometry.heightDp ?: defaultHeightDp) + deltaHeightDp.toInt(),
).constrained()
