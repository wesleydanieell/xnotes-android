# Add an immutable model for configurable toolbar workspaces

Published as a review phase within [upstream PR #431](https://github.com/shardulvs/xnotes-android/pull/431). Intermediate branch/base names below remain useful for phase-specific diffs; no separate upstream PR was opened for each internal layer.

Branch: `xnotes/pr/toolbar-workspace-model`

Proposed base: `master` in shardulvs/xnotes-android (use a stacked PR while prerequisites remain unmerged).

## Description

Existing toolbar settings describe one layout. Introduce a UI-independent workspace model for multiple bars, their dock/floating anchors and their geometry.

Immutable bar/workspace transformations; deterministic anchor resolution; dock capacity and collision helpers. No settings migration or UI changes.

## Validation

2437 JVM tests passed, including workspace and anchor geometry suites.

Run `./gradlew :app:testDebugUnitTest` with JDK 17 and the Android SDK configured. See [validation](../validation.json) for pinned commits, merge checks and test status. JVM success does not certify APK packaging or device behavior.
