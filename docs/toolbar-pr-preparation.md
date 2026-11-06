# Preparação das contribuições de toolbars

Checkpoint: 06/11/2026, integração em `feat/customizable-toolbars`.

## Base e critérios

A árvore restaurada de `0e667a1d` é a referência funcional aprovada. O commit
`932756cc` preserva essa mesma árvore no histórico publicado. As correções e
refatorações posteriores devem ser commits adicionais, sem reescrever esse histórico.

O usuário confirmou melhora no movimento da principal do XCanvas. Durante o
arraste, o espaço reservado à barra na origem pode permanecer vazio até soltar;
o conteúdo se recompõe ao posicionar. Esse comportamento temporário foi aceito.

As extrações de configuração preservam o fluxo atual de persistência imediata,
as medidas compartilhadas e a compatibilidade JSON. Ganho de desempenho exige
medição; redução de acoplamento e tamanho de arquivo não prova ganho de FPS.

## Separação atual de responsabilidades

| Componente | Responsabilidade |
| --- | --- |
| `ToolbarAppearanceSettings` | Controles de dimensões e presets de tamanho |
| `ToolbarBarSettings` | Nome, visibilidade, ordem, movimento, encaixe, faixas e alinhamento |
| `ToolbarEditorInteractionState` | Seleção, arraste e medições transitórias; limpeza na troca de barra/perfil |
| `ToolbarElementsEditor` | Inspetor de elementos, paleta, distribuição e callbacks de reorganização |
| `ToolbarWorkspaceLibrary` | Estado da biblioteca, navegação de prévia e seletores Android de arquivos |
| `ToolbarCopyPlanner` | Posição da cópia e validação compartilhada de capacidade dock/flutuante |
| `PreferencesPane` | Integração com Editor, seleção de perfil, validação de espaço e aplicação |
| `ToolbarWorkspaceDialogs` | Biblioteca, cópia e prévias de workspaces |

A extração de estado mantém os mapas de bounds não observáveis. Atualizações de
medição não devem disparar recomposição; apenas seleção, ponteiro e destinos
visuais precisam ser observados. Dois configuradores não compartilham esse estado.

`ToolbarWorkspaces.updateWorkspace` altera um perfil salvo sem aplicar o filtro
de renderização nem redirecionar implicitamente o contexto compartilhado. O
configurador escolhe o perfil da aba; o movimento em runtime escolhe NOTE quando
o canvas exibe o perfil compartilhado. O JSON e a sincronização dos campos
legados continuam nos pontos de persistência existentes em `Editor`.

## Próximas revisões

1. Auditar os pontos de persistência e sincronização com os campos legados antes
   de extrair as branches de Modelo/Persistência para revisão independente.
2. Conferir funções sem referências e imports redundantes nos arquivos alterados.
   Entradas de JSON e migração não podem ser removidas só por não terem chamadas diretas.
3. Medir recomposições e tempo de frame durante arraste antes de introduzir caches.

## Candidatos a PR e dependências

Esta divisão é um plano de revisão. Cada branch precisa ser extraída e compilada
isoladamente para confirmar os cortes e o volume; não há PR aberto por automação.

| Lote | Conteúdo principal | Dependência |
| --- | --- | --- |
| Modelo | Workspaces, sections/faixas, geometria, distribuição, operações e testes puros | upstream/master |
| Persistência | Codec versionado, migrações e compatibilidade dos campos legados | Modelo |
| Layout | Resolvedores de docks, medidas e organização compartilhada | Modelo |
| Runtime | Frames e bindings de nota/canvas, múltiplas barras e movimento | Persistência + Layout |
| Configurador | Componentes de configuração, preview e interação | Runtime |
| Portabilidade | Presets, templates, cópia e importação/exportação | Configurador |

O Pen Group pode exigir um lote próprio, mas seus IDs e migrações precisam estar
coerentes com Modelo/Persistência. Não omitir IDs já persistidos de uma branch
dependente sem revisar a compatibilidade. Se Configurador exceder o volume
revisável, separar apresentação e gestos em commits/branches compiláveis.

## Evidência e limites

Testes JVM, build debug e `git diff --check` são o gate automatizado atual.
O smoke test do emulador cobre abertura do configurador, troca de abas e perfis;
não substitui QA de stylus, latência, importação/exportação pelo SAF ou caneta
no Redmi Pad 2. A release física deve ser gerada quando uma mudança justificar
essa rodada de validação.
