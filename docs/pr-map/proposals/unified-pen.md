# Add an optional Pen Group launcher independent of toolbar customization

Branch: `xnotes/pr/unified-pen`

Proposed base: `master` in shardulvs/xnotes-android (use a stacked PR while prerequisites remain unmerged).

## Description

Allow the existing toolbar to expose a grouped pen launcher while retaining standalone tools and the default visible layout.

Tool family model; launcher/details and pen-fan gesture; existing toolbar and infinite-toolbar bindings; labels and optional hidden layout entry. Does not import ToolbarWorkspace or multi-bar configuration.

## Validation

2429 JVM tests passed, including grouping and gesture tests; debug Kotlin compilation passed. Device gesture QA remains pending.

Run `./gradlew :app:testDebugUnitTest` with JDK 17 and the Android SDK configured. See [validation](../validation.json) for pinned commits, merge checks and test status. JVM success does not certify APK packaging or device behavior.
