# Optional integration: persistent document tabs + configurable toolbar workspaces

This is the optional **stage 8** after [toolbar stabilization (#11)](https://github.com/wesleydanieell/xnotes-android/pull/11). It combines the persistent tab lifecycle originating in [upstream #359](https://github.com/shardulvs/xnotes-android/pull/359) with the independently configurable XNote/XCanvas toolbars from [upstream #431](https://github.com/shardulvs/xnotes-android/pull/431). It is an **add-on**: the toolbar-only PR remains reviewable and mergeable without adopting persistent tabs.

## Architecture

- `NoteTabs` and `NoteTabsStore`: independent tab state and Android persistence. Navigation actions are routed to the editor, including select, close, reorder and split opening.
- `DocumentTabsHost` positions navigation independently. Docked tabs reserve a full edge strip and stay expanded. Floating tabs use the selected edge and alignment. Legacy lane preferences are ignored when loading the saved edge and size.
- `documentTabsToolObstacles` resolves existing toolbars as obstacles for a floating drop. The tab bar follows the pointer within the editor and settles into the outlined destination on a valid drop. Pane layout only updates when the move commits. Floating tabs overlay both split panes without resizing them, and Center uses the full editor bounds.
- `DocumentTabsBarConfig` and `Preferences`: startup state, anchor, edge, dynamic/manual extent and floating/docked mode persist separately from tool workspace JSON. Old XNotes preferences use safe defaults.
- `DocumentTabsEditor` and `ToolbarWorkspacePreview`: navigation is configured in the same editor as toolbars, with an interactive two-document preview, but does not enter the toolbar element palette.
- `ToolbarDragSurface`: the same long-press gesture engine is shared. Corner drops are disambiguated by actual pointer position, since an anchor like TOP_RIGHT represents both TOP/END and RIGHT/START.

## Test matrix

1. Open an `.xnote` and `.xcanvas` in separate tabs; select and close each, then reopen the app to verify persistence.
2. Expand/collapse tabs, reorder them, and open split view through the tab menu. Ensure the active pane and selected tab remain synchronized.
3. Float the navigation bar at each edge/corner; place independently sized 1-, 2- and 3-row toolbars in the same area. Verify that navigation stays usable near the chosen edge, including when three toolbars occupy that edge. A conflicting drop leaves the current placement intact.
4. Dock navigation on all four edges; resize and move the existing toolbars. Verify the document pane still has valid dimensions.
5. Use the toolbar editor's Document tabs section and drag the preview navigation independently. Check Start/Center/End and the visible selected state.
6. Exercise the earlier toolbar drag regressions RIGHT/START to TOP/CENTER and TOP/CENTER to RIGHT/CENTER. Navigation must not reintroduce bar jumps, ghost indicators or disappearing peers.

## Review and release

For layout or gesture changes, run the Compose suite on Small Tablet before marking a PR ready. Include both the Workspace Preview and the live editor in regression coverage. In XNote and XCanvas, check placement, drag and release with both docked and floating bars. Recheck a saturated edge, the compact/expanded states and split view where applicable.

Run `ANDROID_SERIAL=<emulator-serial> ./gradlew :app:connectedDebugAndroidTest`. The integration regression includes the 144-placement matrix and the outline, drop, cancel and collision checks in the preview and the live editor containers. Record the tested revision, device and results in the PR.


- Run `./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleRelease -PtoolbarTabsReviewBuild=true`.
- A signed opt-in review APK uses **`com.xnotes.pr431tabsreview`**, not the official XNotes package or the toolbar-only `com.xnotes.pr431review`. This lets testers compare without overwriting either.
- The proposed integration is not a claim that #359 or #431 has been upstream-merged. Do not alter either upstream PR's source tree to include this optional stage.
- Emulator/JVM tests are not a substitute for physical stylus verification; attach a real tablet video only when the contributor supplies and approves one.

## Navigation appearance and drag interaction

- The tabbar has no dedicated grip or reserved grip spacing. Hold and drag the navigation surface itself to reposition it; a held tab still supports document reordering.
- Floating navigation uses the same Material extraLarge shape role as floating drawing toolbars. The selected-tab highlight uses the Material small shape. Both follow the global Settings > Corners preference (Sharp, Rounded, Soft), including when it changes while the editor is open.
- Docked navigation stays flush with its dock edge. Verify all four edges and the floating editor preview, plus collapsed and expanded modes, when reviewing this optional integration.