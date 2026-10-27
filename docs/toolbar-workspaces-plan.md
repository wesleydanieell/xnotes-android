# Customizable Toolbars — Implementation Plan

Baseline: upstream `master` (0.9.6). Feature branch: `feat/customizable-toolbars`.

## Goals

Allow users to create several independently positioned and styled toolbars, build compact
multi-row tool/color groups, and choose either shared or independent configurations for notes
and the infinite canvas. Keep the existing toolbar layout and settings valid throughout the
migration. The ordinary drawing gesture must never be consumed by toolbar editing.

## Existing architecture

- `core/tools/ToolbarLayout.kt` models unique tool entries inside ordered sections.
- `settings/Settings.kt` persists separate note/canvas layouts as JSON.
- `ui/ToolbarFrame.kt` owns the current single-bar geometry, docking, and occlusion.
- `ui/ToolbarCustomizer.kt` edits sections and tools using touch drag-and-drop.
- `ui/PreferencesPane.kt` offers the note/canvas toolbar customization tabs.

The existing single-bar path stays operational until the multiple-bar renderer and editor
can replace it atomically.

## Phases / independently reviewable commits

1. **Pure workspace model (first milestone)**
   - Stable bar IDs; zero-to-many layouts per context; add, update, reorder and remove.
   - Explicit anchors and manual/automatic sizing data.
   - Preserve independent per-bar tool arrangements; filter unavailable tools by context.
   - JVM tests for operations, ID collisions and legacy-to-workspace conversion.
2. **Versioned persistence and migration**
   - Store a workspace schema with an explicit version, separate from document formats.
   - Migrate existing `toolbar_layout`, `canvas_toolbar_layout` and `ToolbarLook`.
   - Old settings remain readable; malformed or future schemas fall back safely.
   - Round-trip and upgrade tests.
3. **Multiple-bar rendering**
   - Render the current bar and new bars through a shared frame and one tool-dispatch path.
   - Independent placement, hide/show, docking, clipping/scrolling, stable focus.
   - Ensure viewport occlusion and safe areas work in landscape, portrait and split-view.
   - Maintain compatibility with existing tool popups and pen latency.
4. **Customizer screen**
   - Create, duplicate, rename, delete and reorder bars.
   - Add/reorder/remove tools; move tools between bars; restore the default.
   - Live preview with undo/cancel; no interactions reach the drawing canvas in edit mode.
5. **Appearance and responsive layout**
   - Auto/manual width and height; compact/regular/comfortable size presets.
   - Background, opacity, optional blur, border, radius, padding and alignment.
   - Adapt rows/columns, overflow and color-slot groups without overlapping targets.
6. **On-canvas interaction and mode profiles**
   - Press/drag a designated handle, tap it for the context menu.
   - Resize only in layout-edit mode; lock controls during ordinary writing.
   - Shared/independent note and canvas workspace profiles, with distinct tool state
     for each split-view pane. Export/import later if the first six milestones pass QA.

## Non-negotiable checks

- Start with a clean legacy install, a customized legacy install and a malformed settings file.
- No data loss on switching note/canvas or restoring defaults.
- Existing tool behaviors and long presses continue to work unchanged.
- No lost first ink stroke when opening a note or changing split-view focus.
- No off-screen controls at orientation changes or in the Android gesture insets.
- Compact icons retain practical touch targets (target >= 48 dp for finger use).
- Performance on Xiaomi Redmi Pad 2: compare pen latency and layout recompositions
  before/after enabling several floating bars.

## Scope boundaries

Do not add toolbar state to .xnotes/.xcanvas files. Do not merge unrelated branches or
rework the current drawing engine in this feature. Avoid nested arbitrary layout trees in
the first iteration; a controlled grid/stack element is easier to keep accessible.
