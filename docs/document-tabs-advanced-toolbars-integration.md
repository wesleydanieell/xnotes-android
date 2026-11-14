# Optional integration: persistent document tabs + configurable toolbar workspaces

This is the optional **stage 8** after [toolbar stabilization (#11)](https://github.com/wesleydanieell/xnotes-android/pull/11). It combines the persistent tab lifecycle originating in [upstream #359](https://github.com/shardulvs/xnotes-android/pull/359) with the independently configurable XNote/XCanvas toolbars from [upstream #431](https://github.com/shardulvs/xnotes-android/pull/431). It is an **add-on**: the toolbar-only PR remains reviewable and mergeable without adopting persistent tabs.

## Architecture

- `NoteTabs` and `NoteTabsStore`: independent tab state and Android persistence. Navigation actions are routed to the editor, including select, close, reorder and split opening.
- `DocumentTabsHost`: independent dock/floating navigation surface; not a `ToolbarBar`, `ToolbarEntry` or a pen group. Docked mode reserves a dedicated edge slot, floating mode overlays the editor without reducing document area.
- `documentTabsToolObstacles`: uses the existing toolbar dock/floating layout planners to avoid real toolbar rectangles. `documentTabsFreeStrip` determines the usable space for expanded navigation.
- `DocumentTabsBarConfig` and `Preferences`: startup state, anchor, edge, dynamic/manual extent and floating/docked mode persist separately from tool workspace JSON. Old XNotes preferences use safe defaults.
- `DocumentTabsEditor` and `ToolbarWorkspacePreview`: navigation is configured in the same editor as toolbars, with an interactive two-document preview, but does not enter the toolbar element palette.
- `ToolbarDragSurface`: the same long-press gesture engine is shared. Corner drops are disambiguated by actual pointer position, since an anchor like TOP_RIGHT represents both TOP/END and RIGHT/START.

## Test matrix

1. Open an `.xnote` and `.xcanvas` in separate tabs; select and close each, then reopen the app to verify persistence.
2. Expand/collapse tabs, reorder them, and open split view through the tab menu. Ensure the active pane and selected tab remain synchronized.
3. Float the navigation bar at each edge/corner; place independently sized 1-, 2- and 3-row toolbars in the same area. Navigation must avoid the toolbar bounds and remain inside the viewport.
4. Dock navigation on all four edges; resize and move the existing toolbars. Verify the document pane still has valid dimensions.
5. Use the toolbar editor's Navigation section and drag the preview navigation independently. Check Start/Center/End and the visible selected state.
6. Exercise the earlier toolbar drag regressions RIGHT/START to TOP/CENTER and TOP/CENTER to RIGHT/CENTER. Navigation must not reintroduce bar jumps, ghost indicators or disappearing peers.

## Review and release

- Run `./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleRelease -PtoolbarTabsReviewBuild=true`.
- A signed opt-in review APK uses **`com.xnotes.pr431tabsreview`**, not the official XNotes package or the toolbar-only `com.xnotes.pr431review`. This lets testers compare without overwriting either.
- The proposed integration is not a claim that #359 or #431 has been upstream-merged. Do not alter either upstream PR's source tree to include this optional stage.
- Emulator/JVM tests are not a substitute for physical stylus verification; attach a real tablet video only when the contributor supplies and approves one.
