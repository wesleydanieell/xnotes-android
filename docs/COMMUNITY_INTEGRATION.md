# Community integration

This n-notes community branch includes upstream `3f7d7c2f` (0.9.7-0.53), the
existing community features, and `feat/customizable-toolbars` at `5e786579`.
Document tabs are optional toolbar elements; see [Document tabs](DOCUMENT_TABS_TOOLBAR.md).

The upstream history was reorganized, so the update uses already integrated
n-notes `a164adda` as the content merge base. It preserves community tab dragging,
split readers, external drops, incremental canvas loading and text optimizations,
while adopting upstream sync, document links, OCR and preference improvements.
The configurator is separate from the n-notes preference categories/search and
reader toolbar. Original IDs, version name and community signing remain intact.

To update, fetch and merge `origin/master`, review conflicts and run
`:app:testDebugUnitTest :app:assembleCommunity`. Exercise notes, canvases, tabs,
split readers, sync, navigation, toolbar customizations and stylus on test files.

Package ID stays `com.neddy.nnotes.community`. Updating an existing installation
requires its original signing key. Local community APKs are unsigned when
`keystore.properties` is absent; do not uninstall to bypass a signature mismatch.
