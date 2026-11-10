Preserve native `TextItem` data when reading and writing an XCanvas file, and mesh its glyph contours through the existing rendering pipeline. This extracts the format/render support independently of the experimental canvas text editor.

- Serialize and parse text content, style and geometry; preserve lock/group metadata.
- Reject non-finite geometry when loading text items.
- Route native text contours through `ItemMesher`.
- Add codec round-trip and mesh regression tests.

### Validation

Debug Kotlin compilation and **2,423 JVM tests passed**, including the added canvas text round-trip and meshing tests.

### Compatibility review

Draft because this introduces another canvas item kind: older readers skip native text items, which means reopening and saving such a file with an older version can lose that content. This needs an explicit format compatibility decision before merging.

This PR does not expose a new text editing UI, add external text drops, alter resize gestures, or claim the performance fix from the withdrawn #333. Screenshots would not demonstrate this codec-only workflow; the regression tests are the evidence for this change.

AI tools assisted with extraction, documentation and validation.
