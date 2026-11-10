# Add a dedicated toolbar editor with a live workspace preview

Branch: `xnotes/pr/toolbar-customizer`

Proposed base: `xnotes/pr/toolbar-runtime` in shardulvs/xnotes-android (use a stacked PR while prerequisites remain unmerged).

## Description

Move toolbar configuration into its own screen so general preferences remain readable. Configure bars, appearance and elements beside a workspace preview.

Separate bar, geometry, appearance and element components; editor gesture state; preset library and transfer dialogs; copy/reset placement planners. Remove the superseded drag/ghost implementation from general preferences. Tabs, split-view settings and Pen Group are excluded from this stack.

## Validation

See validation.json for the final JVM result. Visual device QA of this extracted branch remains pending.

Run `./gradlew :app:testDebugUnitTest` with JDK 17 and the Android SDK configured. See [validation](../validation.json) for pinned commits, merge checks and test status. JVM success does not certify APK packaging or device behavior.
