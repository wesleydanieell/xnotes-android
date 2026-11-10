# Validação das branches mantidas

Upstream verificado em 2026-11-10: `3926304a`.

| Branch | Commit | Merge com a base | Testes JVM |
|---|---|---|---|
| `xnotes/pr/toolbar-workspace-model` | `9ae8d7e2` | limpo | passed |
| `xnotes/pr/toolbar-persistence` | `5eeb6fa3` | limpo | passed |
| `xnotes/pr/toolbar-runtime` | `93c780bb` | limpo | passed |
| `xnotes/pr/toolbar-customizer` | `c0e43cfd` | limpo | passed |
| `xnotes/pr/unified-pen` | `26d18773` | limpo | passed |
| `xnotes/pr/native-canvas-text` | `4f573105` | limpo | passed |
| `xnotes/pr/shared-split-toolbars` | `115571bb` | limpo | passed |
| `feat/split-document-view` | `24677475` | limpo | passed |
| `xnotes/community` | `4266dfe4` | limpo | passed |
| `xnotes/integration/toolbars-tabs` | `4266dfe4` | limpo | not rerun; branch source unchanged during audit |
| `community/xnotes` | `4266dfe4` | limpo | not rerun; branch source unchanged during audit |
| `feat/document-tabs` | `6382e18d` | limpo | not rerun; branch source unchanged during audit |
| `perf/incremental-canvas-loading` | `0ace9951` | limpo | not rerun; branch source unchanged during audit |
| `feat/external-drag-and-drop` | `235ec1ca` | limpo | not rerun; branch source unchanged during audit |
| `feat/hold-line-endpoint` | `7b45505b` | limpo | not rerun; branch source unchanged during audit |
| `pr/note-file-tree-sidebar` | `ae115482` | limpo | not rerun; branch source unchanged during audit |

Todas as branches acima contêm o upstream próprio auditado. As fases de toolbar customization são PRs empilhados: comparar cada uma com a base declarada, não com master, enquanto as dependências não forem incorporadas.

Compilação e testes passaram nas extrações novas; a limpeza posterior de imports do configurador foi verificada por compilação. Branches existentes sem alterações de código tiveram a compatibilidade de merge conferida; os testes não repetidos e aliases estão identificados na matriz.

Os aliases de integração usam exatamente o mesmo commit da community correspondente. Arquivos históricos ficam fora desta matriz. Não foram concluídos empacotamento APK nem QA visual dos cortes novos; nenhum app pessoal foi alterado.

Detalhes e contagens disponíveis: [validation.json](validation.json). Resumos dos logs: `validation-logs/`. Reexecutar os merges: `python tools/check-pr-bases.py` a partir da raiz.
