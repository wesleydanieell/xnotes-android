# Add split-screen editing and drag-and-drop pane management

Existing PR: https://github.com/wesleydanieell/xnotes-android/pull/2
Branch: `feat/split-document-view`; target: `feat/document-tabs`.

The split feature stays stacked on persistent document tabs. Dragging a tab opens or targets the secondary pane. Merge the updated tabs base without rewriting the PR head; retain secondary-pane routing and the base branch autosave behavior.

JVM suite and debug Kotlin compilation passed after resolving MainActivity.activateTab. This branch deliberately excludes customizable toolbars and the shared-toolbar preference. The tab/workspace selector adapter remains in the integration branch until its prerequisites can be reviewed independently.
