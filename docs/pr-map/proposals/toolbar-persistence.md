# Persist toolbar workspaces with backward-compatible layout migration

Published as a review phase within [upstream PR #431](https://github.com/shardulvs/xnotes-android/pull/431). Intermediate branch/base names below remain useful for phase-specific diffs; no separate upstream PR was opened for each internal layer.

Branch: `xnotes/pr/toolbar-persistence`

Proposed base: `xnotes/pr/toolbar-workspace-model` in shardulvs/xnotes-android (use a stacked PR while prerequisites remain unmerged).

## Description

Toolbar workspaces need stable settings and transferable presets. Extend layouts with lanes, regions, sizing and spacers, then serialize workspaces and named presets.

Versioned codecs; old single-bar settings migration; profile/context serialization; preset import/export and templates. Legacy unified_pen entries map to a visible ordinary PEN without a Pen Group dependency. Minimal Spacer render/icon cases keep enum handling exhaustive.

## Validation

JVM suite and debug Kotlin compilation passed.

Run `./gradlew :app:testDebugUnitTest` with JDK 17 and the Android SDK configured. See [validation](../validation.json) for pinned commits, merge checks and test status. JVM success does not certify APK packaging or device behavior.
