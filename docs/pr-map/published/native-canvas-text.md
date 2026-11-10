Native text is a document item, but the XCanvas codec currently skips it and the canvas mesher has no text path. A text item therefore cannot survive a canvas save/load cycle or reach the native canvas renderer.

**Change.**
- Read and write text content, position, dimensions, font, size and color in the canvas manifest.
- Preserve item locking and grouping through the existing metadata handling.
- Expose glyph contours from `TextItem` and pass them through the existing text tessellator in `ItemMesher`.
- Validate text geometry when loading and add round-trip/meshing regression tests.

**Testing.** Debug Kotlin compilation and `testDebugUnitTest` passed: 2,423 tests, zero failures. Added tests cover text round trips, lock/group metadata and mesh creation.

**Compatibility.** Older XCanvas readers skip this item kind. Opening and saving a canvas containing native text in an older version can therefore discard that text. This remains a draft pending a format-compatibility decision.

This adds persistence and rendering support. Editing and resizing performance has not been benchmarked, and no performance improvement is claimed.

AI tools assisted with implementation, documentation and validation.
