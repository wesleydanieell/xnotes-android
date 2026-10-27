# Customizable Toolbars — Implementation Plan

Baseline: upstream `master` (0.9.6). Feature branch: `feat/customizable-toolbars`.

## Isolated tablet debug build

- This branch's debug application ID is `com.xnotes.toolbarsdebug`, with launcher label
  `XNotes Toolbars`. It coexists with `com.xnotes`, `com.xnotes.community` and
  `com.xnotes.debug` without touching their installed data.
- Locally signed debug APKs use `~/.android/xnotes-toolbars-debug.keystore` when present,
  with alias `xnotes-toolbars-debug`. This key is deliberately outside Git. Preserve the same
  keystore for future updates; losing it requires reinstalling this *debug variant*.
- Without that local keystore, Gradle falls back to the usual Android debug signing so CI
  and contributors can build; that fallback APK cannot update an APK signed by the separate
  local key without reinstalling.
- This change applies only to `debug`; do not install this branch's `release` on top of
  the regular XNotes installation. Before opening an upstream PR, review whether to keep
  these branch-specific packaging details.

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

## Progress — 2026-10-27

- Phase 1: pure multi-bar workspace model and JVM tests complete.
- Phase 2: versioned JSON codec, lazy migration from legacy layouts/look, and
  compatibility with existing toolbar editor writes complete; JSON fallback and
  round-trip tests pass.
- Phase 3 MVP: note and canvas renderers accept per-bar layouts; additional bars
  render as floating overlays in each pane and stack when assigned the same anchor.
- First preferences UI: add, duplicate, remove, select, show/hide tools, change bar
  anchor and size. The existing primary bar and tool dispatch remain unchanged.
- Dedicated toolbar configuration view accessible from Preferences, with profile/bar selection
  before properties, rename, per-bar visibility, secondary-bar ordering, and the existing
  tool/section drag editor. Main-toolbar legacy appearance stays in its own inspector.
- Desktop QA: Small_Tablet Android emulator boots in the user's Niri session via
  `QT_QPA_PLATFORM=xcb`; the debug APK installs and launches there. The Wacom CTL-472
  uses OpenTabletDriver Artist Mode with a pressure-capable virtual tablet. Direct Android
  stylus forwarding is not yet implemented; emulator gRPC requires authenticated access.
- Next: movable handles, robust overlap/viewport-cover handling, docking, and
  explicit edit mode; currently additional bars are positioned via Preferences.

## Scope boundaries

Do not add toolbar state to .xnotes/.xcanvas files. Do not merge unrelated branches or
rework the current drawing engine in this feature. Avoid nested arbitrary layout trees in
the first iteration; a controlled grid/stack element is easier to keep accessible.
