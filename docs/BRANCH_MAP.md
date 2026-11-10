# Mapa de branches e preparação de PRs — n-notes

Auditoria: 2026-11-10. Upstream: `NeDDy3z/n-notes/master` em `199918c5`.

## Organização

Este repositório tem `.git` próprio, separado de `~/Projetos/xnotes-upstream/`.
O origin continua sendo o fork `wesleydanieell/xnotes-android`, com fetch limitado
às branches `nnotes/*` e `community/n-notes`. Nenhuma branch XNotes é usada como
base direta de um novo PR para n-notes.

## Contexto dos PRs

Os PRs #4 a #9 no n-notes foram fechados. O mantenedor pediu aguardar as contribuições
no XNotes original para facilitar a sincronização; o autor concordou:
https://github.com/NeDDy3z/n-notes/pull/4#issuecomment-6591203089

O draft no fork #3 (`nnotes/split-document-view` sobre `nnotes/document-tabs`) é
histórico e depende de um PR-base fechado; não reutilizar seu diff de dezenas de
commits sem uma extração limpa. Nenhum desses PRs será reaberto automaticamente.

## Branches de origem

| Branch | Classificação |
|---|---|
| `community/n-notes` | integração mantida, atualizada com o upstream e compatibilidade de layouts |
| `nnotes/document-tabs` | port histórico fechado; aguardar upstream, preservar para referência |
| `nnotes/split-document-view` | draft histórico dependente de tabs; revisão futura da pilha |
| `nnotes/hold-line-endpoint` | port fechado; equivalente do PR XNotes #326 |
| `nnotes/file-tree-sidebar` | port fechado; equivalente do PR XNotes #285 |
| `nnotes/external-drag-and-drop` | port fechado; equivalente do PR XNotes #332 |
| `nnotes/editable-text-rendering` | port fechado; não ressuscitar sem repro independente |
| `nnotes/incremental-canvas-loading` | port fechado; equivalente do PR XNotes #358 |
| `archive/local-master-20261110` | master local divergente preservado; não é o novo upstream |
| `work/community-nnotes-latest-toolbars` | snapshot de integração anterior, não usar como PR |

## Novas mudanças upstream

`199918c5` agrupa as canetas em PEN e seleção/lasso em SELECT e adiciona filtros
por tipo de item. O community conserva IDs antigos de configurações como opções
legadas ocultas por padrão, mantendo layouts salvos legíveis. Novos layouts usam
os controles agrupados nativos do fork. Não importar cegamente o Pen Group do
XNotes como se o n-notes não tivesse implementação própria.

Os detalhes de cada mudança e os resultados atuais de testes serão registrados
em `docs/pr-map/`; preservam-se as exceções de ReaderScreen, matemática e seleção.

## Branches organizadas

- `nnotes/upstream`: upstream próprio usado na auditoria.
- `nnotes/community`: integração atualizada; `community/n-notes` é mantida como nome legado.
- `nnotes/integration/toolbars-tabs`: integração completa preservada, sem promessa de PR monolítico.
- `nnotes/pr/legacy-toolbar-compatibility`: correção pequena baseada diretamente no upstream próprio. Título, descrição e validação em `docs/pr-map/proposals/legacy-toolbar-compatibility.md`.
- `nnotes/docs/branch-map`: este mapa, snapshot de PRs e matriz `docs/pr-map/validation.json`.

Não foram reabertos PRs fechados nem reescritas suas branches. A pilha XNotes de toolbar customization deve aguardar o alinhamento com o mantenedor; não enviar os antigos ports agregados como se fossem PRs limpos. As integrações conservam o código específico do fork e as correções upstream de paginação/seleção.

Validação final: [matriz de resultados](pr-map/VALIDATION.md). Comando reproduzível dos testes: `./gradlew --max-workers=2 -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process -I tools/pr-validation.gradle :app:testDebugUnitTest` (o script está na branch de documentação; pode ser passado por caminho absoluto ao testar outra worktree).
