# Integration handoff for future upstream ports

The review branches deliberately differ from the community integration. Do not open a PR with the aggregate community diff.

## Working baseline

`xnotes/integration/toolbars-tabs` retains community `4266dfe4` with standalone tabs, customizable toolbars and the existing split-view corrections. It also contains other community features. `nnotes/integration/toolbars-tabs` tracks the independently updated n-notes baseline. Those are preserved integrations, not newly extracted PRs.

Tabs are a separate docked/floating navigation bar; the abandoned tabs-as-toolbar-item prototype must not be restored. The existing integration keeps expand/collapse transitions, width settings and the dedicated preview controls. In per-pane mode navigation is docked above both viewers; tools belong to their own pane. In shared mode the chosen interface and its workspace are shared, while commands target the active editor. Moving a bar must preserve both interface choice and shared/per-pane mode.

## Review order

1. Existing persistent-tabs PR #359 and dependent split-view PR #2, including dragging a tab into a second pane. The latter now merges the current tabs base without rewriting its history.
2. Independent native shared-toolbar PR (`xnotes/pr/shared-split-toolbars`), which does not depend on tabs or multi-bar workspaces.
3. Workspace model → persistence → runtime → dedicated customizer. Pen Group is a separate upstream-based PR.
4. Extract a small adapter once the prerequisites and maintainer choices are settled. The current tab workspace selector and multi-bar split adapter are **preserved in community, not claimed to be an independent ready PR**.

## Adapter boundaries

- `MainActivity.kt`: editor/pane ownership, active command target and shared/per-pane host composition. Do not copy the complete community activity over a PR base: it also includes file tree, drops and other pending PRs.
- `DocumentTabsHost.kt`, `FloatingDocumentTabs.kt`, `DocumentTabsPlacement.kt`, `DocumentTabsSelectionState.kt`: standalone navigation and mixed-interface selector. Separate navigation state from toolbar element models.
- `SplitToolbarPresentation.kt`, `SplitPreviewGeometry.kt`, `SharedSplitToolbar.kt`, `SharedSplitTools.kt`: presentation planning, preview geometry and active-editor tool routing.
- `ToolbarDockWorkspace.kt`, `WorkspaceFloatingBars.kt`: pass pane-local bounds and obstacles in per-pane mode. Shared mode uses the shared host bounds. Overflow menus and navigation count as obstacles only when they actually occupy the same host.
- `ToolbarPreferencesPane.kt` and `DocumentTabsBarControls.kt`: keep standalone tabs settings separate from tool-bar editing.

Useful community references: commits `0254ece4` (tabs selector), `b4044901` (shared width and pane grips), `25e83e7f` (mode preservation and per-pane navigation), `4266dfe4` (overflow/obstacle isolation). Existing integration docs: `TABS_TOOLBAR_LLM_HANDOFF.md`, `COMMUNITY_SHARED_SPLIT_TOOLBARS.md`, `UPSTREAM_PORT_SELECTION_SPLIT_TOOLBARS.md` under `docs/` in that branch.

## Compatibility and duplication

- Original clipboard is superseded by upstream ItemClipboard.
- Native canvas TextItem codec/mesh is extracted separately from external drops and experimental editing/performance work. Older readers skip the new item kind.
- PR #333 was closed after the original problem proved device/configuration-related; do not revive its performance claim without a new reproducible measurement.
- n-notes now has native grouped pen/selection controls. Its small legacy-ID compatibility PR preserves saved layouts without pretending that the fork lacks grouped tools.
- Experimental generic nested groups, lab signing/labels, profiling and agent instructions stay out of product PRs.

## Validation before changing the adapter

Exercise notes/notes, canvases/canvases and mixed documents; shared and per-pane mode; top/bottom/left/right; docked/floating; navigation adjacent to tools; resizing and rotation; moving each bar while a mixed interface is selected; switching and closing panes. Check the same placement in preview and actual documents. Selection deletion on PDFs must still target the owning editor.

JVM compilation and geometry tests do not establish animation, pointer routing or device layout correctness. Run isolated APK/device validation before describing a newly extracted adapter as production-equivalent. Never run destructive test harnesses in the user's community package.
