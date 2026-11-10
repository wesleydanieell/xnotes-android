# Share toolbars across split documents with configurable mixed interfaces

Branch: `xnotes/pr/shared-split-toolbars`

Proposed base: `master` in shardulvs/xnotes-android (use a stacked PR while prerequisites remain unmerged).

## Description

Split documents can share one toolbar; mixed note/canvas pairs choose the interface used by both editors. Per-pane tools remain available.

Independent native-toolbar implementation, preferences and in-note controls; active-editor routing and geometry helpers. No dependency on customizable workspaces or document tabs. Includes an isolated Android device harness. The newer tabs selector in community is an adapter, not part of this independent PR.

## Validation

See validation.json for current JVM results. The device harness is present; it has not been rerun during this audit.

Run `./gradlew :app:testDebugUnitTest` with JDK 17 and the Android SDK configured. See [validation](../validation.json) for pinned commits, merge checks and test status. JVM success does not certify APK packaging or device behavior.
