# Validação das branches mantidas

Upstream verificado em 2026-11-10: `199918c5`.

| Branch | Commit | Merge com a base | Testes JVM |
|---|---|---|---|
| `nnotes/pr/legacy-toolbar-compatibility` | `90305af6` | limpo | passed |
| `community/n-notes` | `716af312` | limpo | passed |
| `nnotes/community` | `716af312` | limpo | not rerun; branch source unchanged during audit |
| `nnotes/integration/toolbars-tabs` | `716af312` | limpo | not rerun; branch source unchanged during audit |

Todas as branches acima contêm o upstream próprio auditado. As fases de toolbar customization são PRs empilhados: comparar cada uma com a base declarada, não com master, enquanto as dependências não forem incorporadas.

Compilação e testes passaram nas extrações novas; a limpeza posterior de imports do configurador foi verificada por compilação. Branches existentes sem alterações de código tiveram a compatibilidade de merge conferida; os testes não repetidos e aliases estão identificados na matriz.

Os aliases de integração usam exatamente o mesmo commit da community correspondente. Arquivos históricos ficam fora desta matriz. Não foram concluídos empacotamento APK nem QA visual dos cortes novos; nenhum app pessoal foi alterado.

Detalhes e contagens disponíveis: [validation.json](validation.json). Resumos dos logs: `validation-logs/`. Reexecutar os merges: `python tools/check-pr-bases.py` a partir da raiz.
