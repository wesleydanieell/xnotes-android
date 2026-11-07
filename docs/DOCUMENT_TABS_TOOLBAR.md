# Independent document navigation bar

Document tabs belong to the app navigation, independently of customizable toolbars.
Preferences → Toolbar shows the tabs in the live preview and provides two modes:
**Floating** and **Docked**. Choose a position in the grid, or long-press the grip
and drag the navigation bar. Docked navigation reserves its own edge outside the
tool workspace; floating navigation uses eight anchors and avoids measured tool
surfaces. It cannot be inserted into a tool section or attached to another bar.

Runtime and preview use `DocumentTabsHost` and the same animated tab renderer.
The preview maintains its own selection, expansion and ordering without changing
open documents. Horizontal navigation displays a row; left/right navigation uses
a vertical strip. Floating navigation shrinks when collapsed, while docked side
navigation reduces its width and horizontal docked navigation retains the edge.
Selection position and tint animation belong to the document workspace, so replacing
the active pane does not reset them. Tab closing, scrolling, long-press reordering
and dragging toward a document split remain supported.

The navigation configuration is saved in preferences as `document_tabs_bar`.
Documents are never serialized into tool presets. Legacy `document_tabs` elements
are removed on workspace loading, preserving other tools and compacting affected
section lanes. Bars that contained only that element are removed; an otherwise
empty migrated profile regains its default tool workspace.

A shareable local branch is `feat/standalone-document-tabs-bar`.
The older element implementation is preserved on
`feat/document-tabs-toolbar-element`; the original `feat/customizable-toolbars`
branch is unchanged. No branch or release is published automatically.

Emulator verification uses synthetic documents in the isolated debug package.
Physical pen latency and the complete device rotation matrix require device QA.

Validation on 2026-11-07: 2625 JVM tests passed; debug app and instrumented
test APKs built successfully. All 21 interface tests passed on the Small_Tablet
emulator (1920×1200): collapse animation for both modes on all four edges,
the same eight cases in a reduced preview, four long-press drag cases
(runtime/preview × floating/docked), and continuous selection animation when
the document renderer changes. Emulator success does not cover every device
or every possible toolbar arrangement.
