# Document tabs as a toolbar element

Branch: `feat/document-tabs-toolbar-element`.

This branch combines `feat/customizable-toolbars` at `5e786579` with the existing
community document tabs and split-view integration. The element commit can be
ported separately to a fork with those dependencies; the branch is not an
independent upstream PR. No PR or release is published automatically.

Open Preferences → Toolbar → Configure → Elements, add **Document tabs** from a
section's catalog, and move it between sections or bars as with any other element.
It works in note and canvas profiles and survives workspace JSON export/import,
presets, and copying between profiles. Existing layouts do not gain a new item
automatically. Hiding/removing the configured item restores legacy navigation.

Horizontal bars contain the existing animated document switcher in a bounded
360 dp slot. A fixed footprint prevents expansion from moving unrelated bars.
Side rails show a document-tabs button opening the same switcher upright in a
popup. Selection highlights, tint/width animations, collapsing/expanding, closing,
scrolling, long-press reordering and dragging to split reuse `FloatingDocumentTabs`
and `NoteTabBar`; there is no second navigation model. Each split pane routes tab
selection and split actions through its own focus. Documents are not serialized
inside toolbar presets.

The toolbar configurator is a separate `ToolbarPreferencesPane`, so n-notes keeps
its general preference categories, search, handwriting settings and cloud sync.
All toolbar surfaces register their actual rectangles with stylus routing; gaps
between bars remain drawable and closing a bar removes its exclusion rectangle.

Validation: see `COMMUNITY_INTEGRATION.md` for each fork's recorded test/build
results. Physical Xiaomi Smart Pen latency and the complete rotation/docking
matrix still require device QA; emulator interaction does not establish those.
