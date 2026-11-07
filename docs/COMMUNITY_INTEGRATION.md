# Community integration

This n-notes community branch includes upstream `3f7d7c2f` (0.9.7-0.53), the
existing community features, and `feat/customizable-toolbars` at `5e786579`.
Document tabs use an independent movable navigation bar; see [Document tabs](DOCUMENT_TABS_TOOLBAR.md).

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

## Validation — 2026-11-06

- 2,729 JVM tests passed, zero failures/errors/skips; `assembleCommunity` and
  `assembleDebug` passed.
  The concurrent build exhausted its previous heap; sequential compilation with
  an in-process 4 GB heap passed both debug and community Kotlin compilation.
- Community APK signed with the same local Android Debug certificate already on
  Redmi Pad 2. `adb install -r` succeeded; the installed version is
  `0.9.7-0.53-community`, versionCode 106. App launched with a live PID and no
  fatal exception in its startup logs. Package ID and signing certificate match
  the previous installation; no uninstall or app-data reset was performed.
- The emulator was closed at the user's request. No completed animation/gesture
  smoke test or stylus QA is claimed. Existing animations and gestures are reused
  by the toolbar element, and their existing model/drop tests remain enabled.

## Navigation update — 2026-11-07

The historical toolbar element has been replaced by independent floating/docked
navigation with shared runtime/preview rendering. The initial 21 interface tests passed
for XNotes on the tablet emulator; the n-notes invocation initially used the
wrong runner and does not establish n-notes UI coverage; JVM totals were 2,625 (XNotes) and 2,730
(n-notes), without failures. Earlier element-specific validation above is
historical. See [LLM handoff](TABS_TOOLBAR_LLM_HANDOFF.md) for the architecture,
migration, fork differences, branch map and remaining device QA.

## Validação final do tamanho adaptável — 2026-11-07

- XNotes: 2.628 testes JVM sem falhas; 22 testes de interface no Redmi.
- n-notes: 2.733 testes JVM sem falhas; 22 testes de interface no Small_Tablet,
  usando o runner correto `com.neddy.nnotes.debug.test`.
- Ambas as variantes community compilaram e foram atualizadas no Redmi por
  `adb install -r`, preservando dados e certificado. O hash SHA-256 do APK
  instalado foi comparado ao APK assinado local em cada app; ambos coincidiram.
  Os dois apps abriram sem FATAL EXCEPTION no smoke test de lançamento.
- A validação de gestos via Activity não substitui QA físico de caneta e áreas
  reservadas a gestos do Android. O debug n-notes foi recusado pelo instalador
  do Redmi; por isso sua bateria de interface final foi executada no emulador,
  enquanto a instalação community e o lançamento foram verificados no Redmi.

## Stable categorized preferences on tablets

The initial category index used 16 sp titles and full-width summary cards. Picking
a category replaced it with a 224 dp sidebar using 12 sp titles and resized the
content, which looked like the settings suddenly shrank. Categorized tablet
preferences now show that same sidebar from the first frame, defaulting to
General when there is no saved selection. Selecting a page updates its content
and selection without replacing category navigation. The index and back-to-index
behavior remain on phones; continuous settings and search remain supported.
The dedicated toolbar configurator is unchanged by this fork-specific fix.
