# Mapa de branches e preparação de PRs — XNotes

Auditoria: 2026-11-10. Upstream: `shardulvs/xnotes-android/master` (`3926304a`, 0.9.7).

## Organização

- Pasta canônica: `~/Projetos/xnotes-upstream/`, com `.git` próprio.
- n-notes: `~/Projetos/n-notes-upstream/`, outro `.git`, outro upstream.
- Publicação compartilhada: `wesleydanieell/xnotes-android`; novas branches usam `xnotes/...` ou `nnotes/...`.
- Nomes das branches de PRs abertos são preservados. PRs serão abertos/retargetados manualmente pelo usuário.
- Históricos antigos, backups e worktrees com mudanças locais não foram apagados.

## PRs existentes

| PR | Branch | Estado | Escopo |
|---|---|---|---|
| [#359](https://github.com/shardulvs/xnotes-android/pull/359) | `feat/document-tabs` | aberto | feat(tabs): add persistent tabs for notes and canvases |
| [#358](https://github.com/shardulvs/xnotes-android/pull/358) | `perf/incremental-canvas-loading` | aberto | perf(canvas): improve loading performance with incremental scene building |
| [#332](https://github.com/shardulvs/xnotes-android/pull/332) | `feat/external-drag-and-drop` | aberto | support drag-and-drop from other apps |
| [#326](https://github.com/shardulvs/xnotes-android/pull/326) | `feat/hold-line-endpoint` | aberto | fix(gesture): allow dragging recognized line endpoints before pen lift |
| [#285](https://github.com/shardulvs/xnotes-android/pull/285) | `pr/note-file-tree-sidebar` | aberto | Add folder tree sidebar and improve drag-and-drop behavior |
| [#2](https://github.com/wesleydanieell/xnotes-android/pull/2) | `feat/split-document-view` | aberto | feat(split-view): add split-screen editing and drag-and-drop pane management |

## Novos cortes para revisão

| Branch | Base pretendida | Conteúdo |
|---|---|---|
| `xnotes/pr/toolbar-workspace-model` | upstream/master | domínio imutável, âncoras, capacidade de docks |
| `xnotes/pr/toolbar-persistence` | toolbar-workspace-model | layout/faixas, codec versionado e migrações |
| `xnotes/pr/toolbar-runtime` | toolbar-persistence | frames, renderização e movimento das barras |
| `xnotes/pr/toolbar-customizer` | toolbar-runtime | configurador dedicado, preview e biblioteca |
| `xnotes/pr/unified-pen` | upstream/master | Pen Group opcional sem dependência de workspaces |
| `xnotes/pr/shared-split-toolbars` | upstream/master | compartilhamento da toolbar nativa e interface mista |
| `xnotes/pr/native-canvas-text` | upstream/master | codec e mesh de TextItem no canvas, sem UI experimental |
| `xnotes/integration/toolbars-tabs` | community/xnotes | integração funcional; não enviar como PR monolítico |

Os títulos, descrições e resultados efetivamente executados ficam em `docs/pr-map/proposals/`. A matriz em `docs/pr-map/validation.json` distingue compilação/testes JVM de QA visual. O adapter novo do seletor de workspace nas tabs continua na integração; não está apresentado como PR independente pronto. Veja `docs/pr-map/INTEGRATION_HANDOFF.md`.

## Inventário original

| Branch de origem | Commits atrás/à frente do upstream | Arquivos | Merge de diagnóstico | Destino/observação |
|---|---:|---:|---|---|
| `backup/community-xnotes-before-shared-split-20261109` | 0 / 225 | 171 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/community-xnotes-pre-toolbars-20261106` | 0 / 141 | 84 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/document-tabs-toolbar-before-rollback-20261107` | 0 / 114 | 85 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/external-drop-before-pr-split` | 59 / 1 | 17 | conflitos históricos | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/external-drop-before-removing-text-optimizations` | 59 / 6 | 28 | conflitos históricos | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/hold-line-endpoint-before-cleanup` | 128 / 29 | 24 | conflitos históricos | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/pre-096-clipboard` | 59 / 1 | 4 | conflitos históricos | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/pre-096-perf` | 59 / 3 | 4 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/pre-096-split` | 59 / 36 | 11 | conflitos históricos | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/pre-096-tabs` | 59 / 19 | 7 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `backup/toolbars-before-rollback-b0451c94` | 12 / 71 | 60 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `community/xnotes` | 0 / 229 | 185 | limpo | Fonte histórica; revisar commits exclusivos antes de reaproveitar. |
| `develop` | 59 / 42 | 60 | conflitos históricos | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `docs/pr-branch-map-20261110` | 0 / 0 | 0 | limpo | Fonte histórica; revisar commits exclusivos antes de reaproveitar. |
| `feat/adaptive-document-tabs` | 12 / 53 | 25 | limpo | Árvore idêntica ao split (#2); alias histórico, não criar PR duplicado. |
| `feat/customizable-toolbars` | 12 / 85 | 74 | limpo | Protótipo de origem; extrair PRs por domínio, sem reescrever histórico. |
| `feat/document-tabs` | 0 / 24 | 17 | limpo | Preservar branch e escopo do PR aberto; atualizar por merge, sem force-push. |
| `feat/document-tabs-toolbar-element` | 0 / 221 | 154 | limpo | Integração histórica: tabs como elemento foi substituído por barra própria. |
| `feat/document-tabs-toolbar-item` | 12 / 85 | 74 | limpo | Alias exato de customizable-toolbars; preservar como histórico. |
| `feat/external-drag-and-drop` | 0 / 8 | 8 | limpo | Preservar branch e escopo do PR aberto; atualizar por merge, sem force-push. |
| `feat/hold-line-endpoint` | 0 / 3 | 2 | limpo | Preservar branch e escopo do PR aberto; atualizar por merge, sem force-push. |
| `feat/note-tabs-upstream` | 59 / 39 | 18 | conflitos históricos | Protótipo anterior ao PR #359; fonte histórica, não usar como base. |
| `feat/shared-document-clipboard` | 59 / 1 | 4 | conflitos históricos | Superada pelo ItemClipboard compartilhado do upstream; não reintroduzir WorkspaceItemClipboard. |
| `feat/shared-split-toolbars` | 0 / 1 | 20 | limpo | Fonte do PR independente de compartilhamento; preparar adapter de tabs separadamente. |
| `feat/split-document-view` | 12 / 42 | 25 | limpo | Preservar branch e escopo do PR aberto; atualizar por merge, sem force-push. |
| `feat/standalone-document-tabs-bar` | 0 / 225 | 171 | limpo | Snapshot community amplo; usar apenas como integração/fonte de correções. |
| `feat/xcanvas-native-textitem` | 0 / 12 | 13 | limpo | Candidato experimental: separar codec/render de drop externo e edição; QA de formato necessário. |
| `integration/community-097` | 0 / 121 | 80 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `master` | 128 / 28 | 23 | conflitos históricos | Master local antigo preservado como archive/local-master-20261110; master canônico é upstream. |
| `perf/editable-text-rendering` | 0 / 8 | 34 | limpo | PR #333 fechado pelo autor: ausência de repro independente; não reabrir sem novo caso medido. |
| `perf/incremental-canvas-loading` | 0 / 4 | 4 | limpo | Preservar branch e escopo do PR aberto; atualizar por merge, sem force-push. |
| `pr/note-file-tree-sidebar` | 0 / 4 | 9 | limpo | Preservar branch e escopo do PR aberto; atualizar por merge, sem force-push. |
| `pr285-screenshots` | 1155 / 1 | 0 | conflitos históricos | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `test/pr332-no-canvas-textbox` | 0 / 7 | 11 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `test/pr332-no-compose-drop` | 0 / 7 | 12 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `test/pr332-with-text-optimization` | 0 / 15 | 44 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `test/xnotes-096-integrated` | 12 / 41 | 17 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `work/community-pr-only` | 0 / 78 | 82 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |
| `work/community-xnotes-latest-toolbars` | 0 / 216 | 152 | limpo | Arquivo/diagnóstico; preservar commits. Não propor o diff agregado como PR. |

## Alterações sem commit preservadas

- `toolbar-composer-instances`: quatro arquivos preservados em `xnotes/archive/toolbar-composer-wip` (`0eb9399c`); original continua com o mesmo estado local. Não é uma implementação aprovada do modelo de grupos aninhados.
- Worktrees de APKs demo e testes PR332: mudanças locais de build/label continuam nos diretórios originais e não entram nos PRs de produto.

## Política para órfãos e código desnecessário

- Não transportar profiling, builds de laboratório, docs de agentes ou outros PRs junto de uma feature.
- Não excluir codecs/migrações só por falta de chamadas diretas: preservam dados antigos.
- Não reintroduzir grupos genéricos revertidos nem o antigo tabs como elemento.
- Não ressuscitar o PR #333 apenas porque seu código aparece no community.
- Clipboard antigo é redundante com o upstream atual.

## Validação

O inventário JSON preserva o estado anterior à organização. Os conflitos históricos são diagnósticos dos protótipos, não promessa de compatibilidade. A matriz final registra separadamente merge contra a base de cada PR, compilação, testes JVM e QA de dispositivo.

## Nomes e publicação

- `xnotes/upstream`: referência publicada do upstream usado na auditoria; o master remoto antigo não é sobrescrito.
- `xnotes/community` e `xnotes/integration/toolbars-tabs`: aliases da integração preservada. O nome antigo `community/xnotes` continua disponível.
- `xnotes/archive/*`: fontes importantes e WIP preservados, sem incluir esses diffs nos novos PRs.
- `xnotes/docs/branch-map`: este mapa e propostas de PR. Os documentos ficam nesta branch para não poluir PRs de código.
- Não usar `git push --all`: publicar somente refs revisadas. Nenhuma branch antiga foi apagada ou recebeu force-push.

## Limites desta organização

As branches antigas fechadas/experimentais são arquivos históricos, não branches de produto mantidas. Seus conflitos de diagnóstico não foram escondidos com merges artificiais. As branches mantidas têm verificações de merge separadas contra suas bases declaradas.

O configurador foi modularizado e removido da tela geral. A pilha de extração exclui o Pen Group e a integração de tabs/split; a integração community completa continua disponível. A branch de texto nativo é um candidato de formato/render, com revisão de compatibilidade pendente. Localização das strings e QA visual do configurador extraído ainda precisam de revisão antes da submissão.

Validação final: [matriz de resultados](pr-map/VALIDATION.md). Comando reproduzível dos testes: `./gradlew --max-workers=2 -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process -I tools/pr-validation.gradle :app:testDebugUnitTest` (o script está na branch de documentação; pode ser passado por caminho absoluto ao testar outra worktree).
