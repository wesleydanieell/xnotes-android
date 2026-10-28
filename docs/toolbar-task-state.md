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

## Checkpoint M1 — esqueleto visual do editor HUD

Implementado em `ui/ToolbarHud.kt` e integrado ao backstage por `BackstageView.TOOLBAR_HUD`.

- A entrada fica disponível em Preferências → Toolbar → “Abrir editor visual de barras”.
- O HUD ocupa a janela inteira e tem retorno por Back/voltar.
- O fundo central é explicitamente não interativo e separado do documento.
- A barra superior mostra voltar, perfil `xnote`/`xcanvas`, compartilhamento, undo/redo e
  Cancelar/Aplicar como affordances visuais do protótipo.
- O catálogo é pesquisável, recolhível e adiciona itens ao preview transitório.
- O preview mostra duas barras, seleção de barra e seleção de instância de item.
- O painel de propriedades é contextual e recolhível; as propriedades ainda são somente protótipo,
  sem persistência ou alteração do documento.
- O debug desta branch usa `com.xnotes.toolbarv2debug`, coexistindo com `com.xnotes.debug` e
  `com.xnotes.toolbarsdebug`.

### Evidência M1

- APK debug compilado e instalado no `emulator-5554`.
- Capturas reais revisadas em `app/build/qa/m1-ready-fullscreen.png` e
  `app/build/qa/m1-hud-full.png`.
- A captura final mostra o editor em tela cheia, catálogo à esquerda, preview no centro e
  propriedades à direita com uma instância selecionada.
- Não foi executada escrita, pan ou zoom no fundo do preview.

### Refinamento visual M1

Após a primeira inspeção, o preview foi remodelado para se comportar visualmente como um painel
encaixável sobre uma nota:

- papel simulado com linhas e grade, texto fantasma e área de conteúdo não interativa;
- barra principal sobreposta no topo da nota;
- segunda barra como painel flutuante no rodapé;
- composição interna explícita com botão alto, paleta 4×2, separador, grid 2×2 e botões de ação;
- seleção continua contextual sem transformar o fundo em uma tela de documento.

Screenshot revisado: `app/build/qa/m1-nav2.png`.

### Correção de composição e interação

Também foram corrigidos os problemas apontados na revisão seguinte:

- widgets reduzidos para uma escala mais próxima da barra vista dentro da nota;
- itens adicionados passam a aparecer em uma faixa de slots reais abaixo da composição;
- catálogo passou a mostrar ícones do sistema para ferramentas e ícones próprios para grupo, grid,
  separador e espaçador;
- toque longo com arraste horizontal reordena slots adjacentes;
- propriedades da barra permitem editar nome, padding e gap no rascunho;
- item selecionado pode ser removido pelo painel de propriedades;
- `+ Barra` cria e seleciona uma nova barra; barras secundárias são empilhadas com espaçamento no
  preview para não se esconderem.
- catálogo condensado em `+ Componente` dentro de um painel central superior;
- painel superior possui alça visível e pode ser deslocado sobre a nota, com escala/feedback durante
  o arraste, mantendo juntos perfil, adicionar componente, nova barra e undo/redo.
- painel superior é expansível: no modo aberto atua como inspector contextual da barra/instância
  selecionada, com edição de nome, padding, gap e remoção do item; o painel lateral duplicado é
  ocultado enquanto esse inspector está aberto.
- a viewport agora funciona como uma simulação visual de nota infinita: não possui borda de página
  visível e mantém grade, conteúdo e barras na mesma proporção responsiva;
- a nota não é interativa: os controles de zoom alteram somente a escala do preview, enquanto
  seleção, arraste e edição ficam restritos às barras e seus componentes;
- o padrão visual da barra principal usa posição, tamanho e Docked/Floating configurados no app;
  uma nova barra nasce vazia, em tamanho Regular e com largura total do mundo da nota.
- o preview ocupa a camada de fundo do editor e o painel de edição superior é desenhado como
  overlay central acima da nota, evitando que o canvas passe por cima dele;
- o inspector de propriedades deixou de ser lateral independente: quando expandido, fica acoplado
  ao mesmo painel flutuante central.
- o painel central fica fixo no topo e oferece fechamento explícito do catálogo e das propriedades;
- barras podem alternar entre orientação horizontal e vertical; a orientação vertical usa coluna,
  rolagem e indicador de reordenação no eixo correto.

Essas alterações ainda são estado local do protótipo M1. A identidade persistente das instâncias,
destinos de drop por célula, undo/redo e Aplicar/Cancelar transacional continuam sendo escopo do
M2/M4/M5.

### Próxima ação — M2

Extrair o modelo puro de composição e o layout resolver determinístico: Row/Column/Grid, slots,
spans, padding/gap, validação de IDs/ciclos e fixture da barra de referência do guia. O HUD M1
deve continuar sendo apenas uma camada visual até esse motor existir.

### Evidência do checkpoint

- `git status --short --branch`: branch limpa exceto pelo guia e este checkpoint.
- `git worktree list`: branch antiga preservada em worktree separado.
- Build/teste unitário baseline: passou.
- ADB: dispositivo conectado; nenhum teste visual ou instalação foi declarado neste checkpoint.
