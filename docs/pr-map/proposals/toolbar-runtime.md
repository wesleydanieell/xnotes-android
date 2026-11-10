# Render and move docked and floating toolbar workspaces

Published as a review phase within [upstream PR #431](https://github.com/shardulvs/xnotes-android/pull/431). Intermediate branch/base names below remain useful for phase-specific diffs; no separate upstream PR was opened for each internal layer.

Branch: `xnotes/pr/toolbar-runtime`

Proposed base: `xnotes/pr/toolbar-persistence` in shardulvs/xnotes-android (use a stacked PR while prerequisites remain unmerged).

## Description

Render the saved workspace in paged notes and infinite canvases, with consistent dock sizing and floating placements.

Shared frames and section grids; dock layout and floating planning; editor workspace application; mode conversion and preset placement. Gesture callbacks observe the latest editor state. Uses the existing general preferences screen until the dedicated editor lands.

## Validation

JVM suite and debug Kotlin compilation passed.

Run `./gradlew :app:testDebugUnitTest` with JDK 17 and the Android SDK configured. See [validation](../validation.json) for pinned commits, merge checks and test status. JVM success does not certify APK packaging or device behavior.
