# Community integration

The XNotes community integration contains upstream `3926304a` (0.9.7), the existing
community PR features, and `feat/customizable-toolbars` at `5e786579`. Document tabs
use an independent movable navigation bar; see [Document tabs](DOCUMENT_TABS_TOOLBAR.md).

The original toolbar prototype had been removed while its history remained
merged. The new integration explicitly reapplies its complete changes from the
original upstream base `26f42b29`, preserving community tabs, split view, external
drops, incremental loading, and text rendering optimizations.

To update, merge `upstream/master`, review conflicts, and run
`:app:testDebugUnitTest :app:assembleCommunity`. Exercise notes, canvases, tabs,
split view, toolbar customization, stylus and file interactions on test documents.

Package ID stays `com.xnotes.community`. Updating an installed release requires
its original signing key. Local APKs are unsigned when `keystore.properties` is
absent; the community release workflow signs using repository secrets.

## Validation — 2026-11-06

- XNotes: 2,624 JVM tests, zero failures/errors/skips; `assembleCommunity` and
  `assembleDebug` passed. Community APK signed with the existing local Android
  Debug key and updated with `adb install -r` on Redmi Pad 2; app launched without
  a fatal exception. Existing package ID and signing certificate were preserved.
- New regressions cover optional legacy migration, workspace JSON/copy/shared
  profiles, hidden and capacity-rejected bars, composite sizing and stylus
  exclusion areas. Original tab/drop/split regression suites remain enabled.
- The emulator was closed at the user's request. No animation/gesture smoke run
  is claimed; physical stylus latency, rotation and the full docking matrix remain
  pending. `FloatingDocumentTabs` and `NoteTabBar` retain their existing animations
  and input handling; the new element mounts those components.

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

## Selection actions and custom toolbars in split — 2026-11-07

Selection menus now exclude their actual surfaces from native stylus forwarding,
fit their pane and scroll when necessary. Select-all publishes its action menu.
Split toolbars can adapt temporarily to small panes without rewriting presets;
rejected bars remain accessible in an overflow menu. Pane grips are protected
from native forwarding and use measured drag origins.

See [upstream port guide](UPSTREAM_PORT_SELECTION_SPLIT_TOOLBARS.md) for separated
commits, dependency checks for an already partly integrated master, UX sources,
regression commands, test results and device update evidence.
