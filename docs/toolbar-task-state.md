# XNotes Toolbars V2 — estado da implementação

## Checkpoint M0 — diagnóstico inicial

Data: 28/10/2026  
Branch: `feat/toolbar-restart`  
Base: `upstream/master` / `master` em `26f42b29` (`v0.9.6`)

### Estado do Git

- A branch atual foi criada limpa a partir de `master`.
- O único arquivo novo neste worktree é `XNotes-Toolbars-V2-Guia.md`.
- A branch anterior `feat/toolbar-composer-instances` continua preservada no worktree
  `/home/wesley/Projetos/xnotes-toolbar-composer-instances`, em `84e74a27`.
- Não houve reset, limpeza destrutiva, alteração da branch antiga, push ou operação de PR.
- O repositório não possui `AGENTS.md` nesta branch. A branch antiga possui instruções
  específicas do protótipo; elas foram consultadas apenas para entender o histórico e os
  gates, não serão copiadas automaticamente para a V2.

### Baseline verificado

Comando executado:

```bash
ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Resultado: `BUILD SUCCESSFUL` em aproximadamente 40 s; 50 tarefas, com testes unitários e APK
debug gerados. Permanecem somente warnings existentes de APIs/depreciações em `AboutPane.kt` e
`TextFormatBar.kt`.

Ambiente Android confirmado:

- SDK: `/home/wesley/Android/Sdk`
- Dispositivo conectado: `emulator-5554`
- AVDs disponíveis: `Small_Tablet`, `Medium_Phone_API_37.0`
- Nenhum AVD foi iniciado ou instalado durante o diagnóstico.

### Inventário do upstream reutilizável

O upstream já possui uma base sólida, mas de barra linear:

- `core/tools/ToolbarLayout.kt`: itens enumerados, seções, visibilidade, separadores e
  reordenação por índice; serve como fonte de compatibilidade/migração, não como modelo final de
  composição.
- `core/tools/Tool.kt` e `InkPalette`: catálogo e semântica das ferramentas existentes.
- `settings/Settings.kt`, `Preferences.kt` e codecs JSON: persistência global de preferências,
  que deve receber uma configuração versionada de workspace sem gravar estado em documentos.
- `ui/Toolbar.kt` e `ui/InfiniteToolbar.kt`: renderização e callbacks funcionais das barras de
  nota e canvas; os controles de ferramenta devem ser reaproveitados no renderer unificado.
- `ui/ToolbarFrame.kt`: contêiner visual, orientação, scrolling e aparência básica existentes.
- `ui/ToolbarCustomizer.kt` e `ui/PreferencesPane.kt`: pontos de entrada da customização atual,
  mas a UX V2 deve evoluir para um editor HUD dedicado com seleção contextual.
- Histórico/estado Compose do `Editor` e `InfiniteEditor`: integrações funcionais de ferramentas,
  undo/redo e contexto xnote/xcanvas que não devem ser duplicadas pelo editor HUD.
- Testes existentes em `core/tools`, `settings` e `ui`: baseline de regressão e referência para
  novos testes puros.

### O que existe na branch antiga

`feat/toolbar-composer-instances` contém 38 arquivos alterados, aproximadamente 5.032 linhas
adicionadas e uma sequência extensa de commits de protótipo. Entre os componentes úteis estão:

- workspaces com barras múltiplas e perfis note/canvas;
- migração/persistência JSON versionada;
- `ToolbarNode` com grupos, grids, paleta, spacer e separador;
- âncoras, pilhas, geometry resolver, resize e drop targets;
- preview, barras flutuantes, ações contextuais e testes unitários;
- documentação de QA/ADB e checkpoints de decisões.

Essa branch é referência e fonte seletiva de ideias/testes. Não portar o editor inteiro: o guia V2
exige modelo de slots com spans, renderer e resolver compartilhados entre preview e barra real,
rascunho isolado, undo/redo transacional e uma tela HUD estática. A implementação antiga usa
conceitos de layout linear/legacy e deve ser auditada antes de qualquer reaproveitamento.

### Riscos e decisões de arquitetura

1. **Modelo legado:** `ToolbarLayout` identifica ferramentas por tipo e limita a uma ocorrência
   por layout. A V2 precisa de `instanceId` distinto para duplicatas e nós aninhados.
2. **Layout:** não reutilizar coordenadas absolutas ou um segundo algoritmo de preview. O núcleo
   novo deve calcular Row/Column/Grid, spans, padding, gap, overflow e destinos de drop de forma
   pura e determinística.
3. **Renderização:** `Toolbar.kt` e `InfiniteToolbar.kt` hoje têm ramos paralelos. A próxima
   arquitetura deve extrair uma projeção/renderização compartilhada, mantendo callbacks reais
   das ferramentas.
4. **Persistência:** preservar JSON legado e adicionar versão/migração para workspace V2. Nunca
   escrever toolbar em `.xnote` ou `.xcanvas`.
5. **Gestos:** long press/drag do container precisa coexistir com toque de ferramenta, desenho,
   mouse e stylus; esta área exige testes próprios antes da integração.
6. **Editor:** a tela HUD deve ser não interativa como documento; somente barras e componentes
   recebem eventos. Cancelar não pode tocar o estado persistido.
7. **Build isolado:** antes de instalar em ADB, conferir `applicationId` de debug exclusivo para
   não substituir a instalação normal do XNotes.

### Próxima ação — M1

Definir e implementar o esqueleto visual do editor HUD dedicado: fundo estático, barra superior,
catálogo recolhível, painel de propriedades recolhível, lista compacta de barras e seleção de
workspace/barra. Antes de adicionar engine de grid, validar a hierarquia de seleção e o fluxo de
entrada/saída com screenshots do emulador.

### Evidência do checkpoint

- `git status --short --branch`: branch limpa exceto pelo guia e este checkpoint.
- `git worktree list`: branch antiga preservada em worktree separado.
- Build/teste unitário baseline: passou.
- ADB: dispositivo conectado; nenhum teste visual ou instalação foi declarado neste checkpoint.
