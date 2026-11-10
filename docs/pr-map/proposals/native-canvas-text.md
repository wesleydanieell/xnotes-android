# Preserve and mesh native text items in XCanvas files

Branch: `xnotes/pr/native-canvas-text`

Proposed base: `master` in shardulvs/xnotes-android (use a stacked PR while prerequisites remain unmerged).

## Description

Recover the independent format/render portion of the experimental canvas text work. Native TextItem data can round-trip through the canvas codec and reach the existing mesh pipeline.

TextItem glyph contours; ItemMesher text support; canvas text serialization with finite geometry checks; round-trip and mesh regression tests. No external text-drop UI, resize gesture or claimed text-editing performance improvement.

## Validation

See validation.json for current JVM results. Older readers skip the new item kind; cross-version format review is required before proposing this PR.

Run `./gradlew :app:testDebugUnitTest` with JDK 17 and the Android SDK configured. See [validation](../validation.json) for pinned commits, merge checks and test status. JVM success does not certify APK packaging or device behavior.
