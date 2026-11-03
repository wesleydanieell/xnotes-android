# Estado executável — XNotes Toolbars

Atualizado em 03/11/2026. Este arquivo é o checkpoint obrigatório para qualquer sessão Codex.
Índice e prioridades atuais: docs/FEATURE_ROADMAP.md. Guia técnico:
docs/toolbar-implementation-guide.md. Roteiro ADB: docs/toolbar-adb-test-playbook.md.
Camadas são um projeto futuro separado: docs/layers-roadmap.md.

## Projeto e ambiente verificados

- Diretório ativo confirmado em 03/11: /home/wesley/Projetos/xnotes-customizable-toolbars
- Worktree separada do upstream shardulvs/xnotes-android; fork configurado como origin.
- Branch atual: feat/customizable-toolbars; histórico da prototipagem importado sem squash.
- Debug applicationId: com.xnotes.toolbarsdebug; build isolado das instalações normais.
- SDK Android conhecido: /home/wesley/Android/Sdk
- Emulador existente durante a prototipagem: emulator-5554, Small_Tablet.
- APK da branch antiga compilou/testou em 27/10/2026; o clone novo compilou,
  passou testes JVM e instalou no emulador em 28/10/2026. QA visual e de gestos
  continuam sem validação completa.
- Keystore de debug em /home/wesley/.android/xnotes-toolbars-debug.keystore:
  não copiar para Git nem alterar sua senha por automação.

## Status geral (não inflar resultados)

| Milestone | Estado | Evidência e pendências |
| --- | --- | --- |
| M0 Clone/branch/debug | FEITO | Clone isolado, commits importados e build/testes/instalação ADB aprovados em 28/10 |
| M1 Modelos/migração | PARCIAL | ToolbarWorkspace, codec JSON e testes JVM existem; rever casos extremos |
| M2 Renderer/gestos | PARCIAL | Barras extra; principal pode mover; container longpress; sem QA de conflitos |
| M3 Editor/preview | PARCIAL | Config view, CRUD, oito encaixes com centro vazio, live preview; sem undo/apply; QA físico pendente |
| M4 Dimensões essenciais | PARCIAL | Cinco controles por barra, overrides opcionais e resolvedor de métricas compartilhado; QA físico e edição por elemento individual ainda pendentes |
| M5 Elementos/cores | PARCIAL | Cores globais divididas dinamicamente em N blocos por barra, controle na tela dedicada; ainda faltam tamanho/spacing por elemento e QA |
| M6 Sections em faixas | PARCIAL | Sections de 1–3 linhas independentes (colunas em barras laterais), sem limite por linha; codec v4 migra v1/v2/v3; renderer/preview comuns, arraste por linha; QA físico pendente |
| M7 Caneta unificada | PARCIAL | Lançador opcional, leque radial com animação e painel único de configurações por tipo; QA de stylus e migração/schema avançado pendentes |
| M8 Atalho in-note | PENDENTE | A pedido do usuário, NÃO implementar edição/resize dentro da nota; apenas entrada rápida para configuração dedicada |
| M9 Perfis/QA/performance | PARCIAL | Dados note/canvas separados; UI de shared não terminada |
| M10 PRs upstream | PENDENTE | Só branch de integração no fork; nenhum PR desta feature aberto |

## Commits da integração já existentes

- 34089281 docs: planejamento inicial.
- a62e5f88 modelo de barras.
- d6568070 codec de persistência/migração.
- feb37019 layout explícito.
- 043c84f0 barras secundárias.
- 6781e61a controles básicos de perfil/barra.
- 60748fd3 debug isolado.
- 20efeb5d tela de configuração focada.
- 5a00c22c mover contêiner de barras e encaixes.
- 1f9c1e62 preview, grade de posições e refinamento visual.

Confira git log ao retomar; pode haver commits posteriores. Este checkpoint
não substitui o Git como fonte de verdade.

## Último checkpoint de execução — 03/11/2026

- Redmi Pad 2 confirmado em ADB: serial `pfcmu4dutsylceus`; APK isolado
  `com.xnotes.toolbarsdebug` instalado com `Success` nesta sessão (sem mexer no XNotes principal).
- Correção de empilhamento: barra arrastada recebe zIndex enquanto segura; camada de
  barras flutuantes fica acima do desenho do canvas. **Revalidar em split-view.**
- Pen Group: ícones vetoriais de contorno mais compactos, sem bolinha cortada no lançador;
  nomes visíveis no painel; toque na caneta já ativa abre painel imediatamente.
  Quick-select usa teste de proximidade aos centros reais dos ícones na tela, com
  setor angular como fallback. **Validar com dedo e Xiaomi Smart Pen nas quatro bordas.**
- Nova estrutura após revisão com o usuário: presets 1×2/2×2/3×2 substituídos
  por `ToolbarSection.lanes` (1–3), sem limite de itens por faixa. Na lateral,
  as faixas viram colunas. Criar section abre escolha de 1/2/3 linhas; o padrão
  é 1. Alteração posterior por menu no cabeçalho, sem botões repetidos em toda
  section. DnD identifica ocorrências por seção/índice. O seletor global de cores ganhou
  distribuição da barra em N blocos, sem botões ou números nos chips. Os intervalos
  são frações da paleta **atual** (5 cores/2 blocos → 3+2; 10/2 → 5+5).
  JSON v4 migra intervalos absolutos v3, grades v2 e arrays v1. Renderer idêntico
  na nota/canvas/preview. O snap exclui CENTER e resolve para o ponto mais próximo
  entre oito; barras legadas no centro migram para TOP_CENTER.
  **Exige QA físico de 3 faixas, cores e snap nas bordas no Redmi Pad 2.**
- Testes JVM adicionados para geometria do leque, seleção por posição real,
  capacidade de grupo e persistência v1/v2. O fato de compilar NÃO substitui QA visual.
- Decisão atual do usuário: personalização de barra SOMENTE na tela dedicada.
  Propriedades finais: largura, altura, padding, espaçamento e tamanho de elementos;
  acesso rápido pelo editor ainda precisa implementar.

## Fila priorizada por dependências

### P0 — Confiabilidade antes de adicionar complexidade

- [x] [M0] Compilar/testar NESTE clone; verificar origem da branch (28/10/2026).
      Gradle unit tests + assembleDebug passaram e APK instalou no emulator-5554.
- [ ] [M2] Testar toque e long-press em ferramentas reais. Resolver conflitos
      entre drag do contêiner e long-press próprios de ferramentas/cores.
- [ ] [M2] Testar dock principal topo/baixo/esquerda/direita com mover por hold;
      garantir que esquerda/direita não são convertidas inadvertidamente em float.
- [ ] [M2] Testar 8 posições float note/canvas em retrato, landscape e split.
- [ ] [M2] Verificar suporte a insets, barra fora da tela e overlap com duas barras.
- [ ] [M3] Abrir configurador no emulator e validar visualmente preview e grade,
      com seleção por toque, arraste, duplicação, exclusão e restauração.

### P1 — Bases para toda a personalização

- [ ] [M1] Revisar persistência versionada, invalidação de IDs e migração legado.
- [ ] [M3] Criar view-model de edição em rascunho com undo/redo, aplicar/cancelar.
- [x] [M4] Resolver métricas e dimensões auto/manuais de forma determinística, com testes JVM.
- [ ] [M4] Validar visualmente limites de largura, altura, padding, espaçamento e tamanho nas barras fixas, flutuantes e laterais.
- [ ] [M3/M4] Confirmar no Redmi Pad 2 equivalência geométrica completa do preview e renderer; ambos usam o mesmo resolvedor de métricas.

### P2 — Customização estrutural e ações

- [ ] [M5] Refinar tamanho/espaço/hitbox por elemento individual; tamanho geral por barra está disponível.
- [ ] [M5] Número de cores por barra e grade de swatches.
- [x] [M6] Sections com 1–2 faixas independentes e migração v1/v2 → v3, testes JVM.
- [ ] [M6] QA físico do arraste entre linhas/colunas, overflow e Colors duplicado;
      não reintroduzir presets fixos 2×2/3×2.
- [ ] [M7] Caneta unificada usando tipos existentes.
- [ ] [M8] Menu de contexto em longpress sem deslocamento.
- [ ] [M8] Atalho in-note para abrir a configuração dedicada, sem resize in-note (decisão do usuário).
- [ ] [M9] UI de compartilhamento/independência xnote/xcanvas.

### P3 — Lançamento e PRs

- [ ] [M9] Acessibilidade, contrastes e recuperação de barras ocultas.
- [ ] [M9] Instrumentar performance e primeiro traço no Redmi Pad 2.
- [ ] [M10] Matriz QA completa do guia, screenshots e documentar como usar.
- [ ] [M10] Planejar e extrair branches pequenas a partir de upstream/master;
      verificar diffs e entregar descrições para abertura MANUAL pelo usuário.
      O Codex nunca cria, edita, comenta ou mescla PRs automaticamente.

## Como atualizar este checkpoint

Ao terminar um item, marque [x], escreva data, hash do commit, comando de teste
e resultado real numa seção "Evidências" abaixo. Quando algo falhar, não
assinale como feito. Insira a reprodução, logs relevantes e próximo experimento.
Se o upstream mudar, registre o commit-base e a estratégia de sincronização.
Antes de encerrar uma sessão, atualize "Próxima ação". Preserve alterações do
usuário encontradas no worktree e não invente sucesso nem background jobs.

## Evidências

- 27/10/2026: Gradle testDebugUnitTest + assembleDebug concluídos no clone
  anterior /home/wesley/Projetos/xnotes-customizable-toolbars.
- 27/10/2026: APK com package com.xnotes.toolbarsdebug instalado via adb no
  emulator-5554 e Activity iniciada; QA de gestos/preview ainda NÃO validada.
- 28/10/2026: clone original em novo diretório e branch local importada,
  com origin=fork e upstream=original. Git history preservado.
- 28/10/2026: no clone NOVO, testDebugUnitTest e assembleDebug concluíram
  BUILD SUCCESSFUL (50 tasks, 25 executed e 25 from cache), em 2m12s;
  instalação adb -s emulator-5554 install -r retornou Success, e am start
  encaminhou a Activity já ativa. QA visual/gestual continua PENDENTE.

## Evidências de retomada — 03/11/2026

- O worktree local estava limpo em `feat/customizable-toolbars` (`1f9c1e62`),
  dois commits atrás do remoto. Foi atualizado exclusivamente por fast-forward
  até `6a5d6d64`, preservando o histórico anterior.
- Auditoria de código identificou `ToolbarWorkspace`, codec JSON versionado,
  renderer de barras secundárias, preview e testes existentes. **Não foram
  reexecutados builds ou QA no tablet nesta sessão.**
- Roadmap de produto atualizado em `FEATURE_ROADMAP.md`: priorizar a redução
  de sobrecarga com grupos/paleta rápida, mas **antes** validar os gestos e
  a geometria existentes. Camadas documentadas em trilha separada.

## Evidências de implementação — 03/11/2026

- Commit `b444d24d`: extraído `ToolbarAnchorResolver.nearest` como função pura para o grid 3×3,
  compartilhada pelo drag das barras reais e pela prévia que usa `ToolbarDragSurface`.
- Adicionados testes JVM para nove posições, coordenadas fora da área e entrada inválida.
- `:app:testDebugUnitTest :app:compileDebugKotlin` passou (25 tarefas, BUILD SUCCESSFUL,
  14 s). Não valida gestos de stylus ou aparência no dispositivo.
- `Small_Tablet` estava com override de celular (1080×2400/420 dpi); restaurados
  1920×1200/320 dpi. Build anterior instalou no emulador com sucesso.

## Evidências — grupo compacto (03/11/2026)

- Commit `b1054517`: criado `ToolbarToolGroup` imutável, com escolhas de escrita,
  deduplicação, última ferramenta selecionada e projeção não destrutiva por contexto.
- Testes JVM de seleção, fallback e validação; `:app:testDebugUnitTest
  :app:compileDebugKotlin` passou (25 tarefas, BUILD SUCCESSFUL em 7 s).
- **Ainda não visível na UI**: a próxima etapa de P1 é conectar um lançador
  compacto ao renderer compartilhado e ao schema versionado, sem alterar
  o layout legado automaticamente.

## Evidências — fidelidade do preview (03/11/2026)

- Commit `27fc3fba` (`fix(toolbar): match preview sizing and distinguish docked from floating`).
- Removidos os tamanhos fictícios de 21 dp, o limite artificial de 5/9 itens e as
  três cores de exemplo. A prévia usa `barMetrics` (36/42/50 dp), cores reais,
  múltiplos controles de paginação/zoom e rolagem para conteúdo excedente.
- Docked ocupa a borda e reserva visualmente espaço do papel; Floating usa
  cápsula com margem e sombra sobre o papel.
- `:app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug` passou
  (50 tarefas, BUILD SUCCESSFUL em 8 s), APK instalado com ADB no Small_Tablet.
- QA visual com capturas no emulador: xnote docked topo, xnote floating topo e
  xcanvas floating inferior renderizaram sem crash. Teste de gestos/caneta no
  Redmi Pad 2 e comparação pixel-perfect com barra real ainda PENDENTES.
- Esta foi uma aproximação inicial: título, página e zoom eram placeholders, e
  ainda faltava escala relativa à largura da tela e resolver colisões entre barras.

## Correção incremental — proporção real do preview (03/11/2026)

- Docked: removido o limite de largura que encolhia a barra e a deixava grudada
  à esquerda; largura/altura transversal seguem exatamente o contêiner e
  `BarMetrics.thickness`, como no `ToolbarFrame` real.
- A prévia agora reutiliza os composables efetivos `ToolbarIcon`, `Swatch`,
  `Separator`, `Label` e `PageCounter`: cores com anel/miolo corretos,
  paginação anterior + contador + próxima (sem três setas iguais),
  zoom menos + valor + mais, e seta de voltar no lugar de ícone de casa.
- `LocalConfiguration.screenWidthDp/screenHeightDp` fornecem dimensões da janela;
  `toolbarPreviewAspectRatio` preserva a forma da tela no palco e
  `toolbarPreviewScale(previewWidth, screenWidth)` + `LocalDensity` aplicam
  escala uniforme a botões, cores, textos, separadores e espaço do papel.
  Preservam-se os tamanhos reais da barra fora do preview, inclusive hit targets.
- O preview recebe título, número de página, zoom e cor ativa do `Editor` atual;
  para xcanvas, a representação de zoom ainda precisa receber o estado do canvas.
- `:app:testDebugUnitTest :app:assembleDebug` passou (50 tarefas); testes unitários
  da razão de escala cobrindo .75, .5, largura maior e dimensões inválidas.
  APK reinstalado com `adb` no `Small_Tablet`, capturas da barra real e do
  docked no editor mostram a mesma proporção da seta e dos demais controles.
- Pendente: comparar floating, barra lateral e contexto split-view no Redmi Pad 2;
  testar long-press/scroll sem colisões, além de alinhar o tamanho da viewport
  ao painel real quando houver sidebar e split.

## Implementação M7 — caneta unificada (03/11/2026)

- Commit de implementação `8313ec18` (`feat(toolbar): add unified pen fan and inline settings`).
  Última verificação: `:app:testDebugUnitTest :app:assembleDebug` com
  `BUILD SUCCESSFUL` (50 tarefas, 8 s), `git diff --check` sem erros e
  instalação `adb -s emulator-5554 install -r` com resposta `Success`.
- `ToolbarItem.UNIFIED_PEN` é opt-in, oculto por padrão. Cada caneta individual
  continua independente. O checkbox em `Toolbar > Tools and groups` adiciona,
  exibe ou oculta o grupo também em layouts persistidos antigos, sem acrescentar
  outras ferramentas a barras secundárias mínimas.
- Em `Toolbar.kt` e `InfiniteToolbar.kt`, toque curto seleciona a última caneta.
  O segundo toque na caneta ativa abre o painel completo. Segurar apresenta seis
  escolhas circulares, animadas em arco conforme a posição da toolbar, com
  feedback tátil. Arrastar e soltar sobre uma escolha ativa a ferramenta;
  soltar sem escolha deve fechar o leque.
- `UnifiedPenDetails.kt` mostra na MESMA janela a caneta selecionada, largura,
  amostra de traço e cores reais. `Advanced pen settings` expande abaixo,
  adaptando parâmetros a cada tipo (pressão, sensibilidade, caligrafia, speed,
  taper, tracejada, marca-texto e neon). Não há um segundo popup de ferramenta.
  O painel compacto tem cores de 22 dp em alvos de toque de 30 dp, instrumentos
  verticais sem rótulos sobrepostos e sliders visualmente reduzidos.
- Os desenhos das pontas são vetoriais Compose, verticais e com friso/bico de
  cor dinâmica. `PenFanGestureTest` cobre as quatro direções e o alvo de toque.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` passou nas iterações
  iniciais; a build foi instalada no `Small_Tablet`. Capturas ADB verificaram
  barra real e círculo radial; o usuário pediu refinamentos após testar.
  **Não confundir isso com QA de caneta real:** Redmi Pad 2 ainda não conectado.
- Ainda pendente: verificar que o Popup não perde o evento de UP no longpress;
  movimento contínuo com stylus, cancelamento, rotação, bordas da tela,
  split-view, persistência do último tipo entre relançamentos e configurações
  de cor individuais por caneta. A implementação atual usa `rememberSaveable`
  para a última caneta e não muda o schema para ToolbarNode v3.

## Próxima ação (uma por vez)

Validar no AVD a janela única com avançadas expandidas, o checkbox e o
cancelamento do leque ao levantar sem selecionar. Depois testar arrastar com
Xiaomi Smart Pen no Redmi Pad 2 (quando conectado), corrigir colisões com
drag de toolbar, registrar QA e planejar persistência por perfil/toolbar.
Camadas ficam fora desta branch.

## Checkpoint M4 — geometria essencial (03/11/2026)

- Introduzidos overrides opcionais por toolbar: `widthDp`, `heightDp`,
  `paddingDp`, `spacingDp` e `elementSizeDp`. Valores nulos continuam
  usando o preset original. Limites: dimensão 48–2048 dp (controle visual
  de largura até 1200 e altura até 800), padding/spacing 0–32 dp e elemento
  28–72 dp.
- `resolvedBarMetrics` e `toolbarDepth` alimentam barra real de notas,
  canvas e preview com as mesmas medidas. Geometria manual de docked é
  centralizada na borda; dimensões transversais respeitam o mínimo
  necessário para até três faixas. O controle fica somente no configurador.
- JSON v4 recebeu os campos opcionais sem quebrar layouts anteriores.
  Testes de clamp, round-trip e resolução de métricas adicionados.
- `:app:testDebugUnitTest :app:assembleDebug --console=plain --offline`
  passou no CachyOS, `BUILD SUCCESSFUL` (50 tarefas). `git diff --check`
  passou. APK isolado instalado em `emulator-5554` com `Success`.
- Emulador confirmado com `wm size 1920x1200`, `wm density 320`.
  Tela dedicada abriu; os cinco ajustes foram vistos; tocar no slider de
  padding mudou para 20 dp com opção Auto. Foi corrigido o salto de layout
  ao surgir essa opção. A build final após a correção passou novamente:
  `BUILD SUCCESSFUL` (50 tarefas, 7 s). O ajuste final do botão Auto
  ainda não foi comparado visualmente em nova instalação.
- O Redmi Pad 2 **não estava listado** por `adb devices -l` nesta
  execução. Não houve instalação nem teste físico desta versão nele.
  Comparação real/preview, orientação, toques com caneta e split-view
  continuam pendentes. Nenhum PR foi aberto ou alterado.

## Checkpoint M4.1 — tamanho individual junto à prévia (03/11/2026)

- `ToolbarEntry.sizeDp` permite override opcional de 28–72 dp para cada
  ocorrência de ferramenta. Mesmo tipo repetido (inclusive `Colours`) não
  compartilha override. `ToolbarLayout.setEntrySize(sectionIndex, entryIndex, sizeDp)`
  atualiza de forma imutável; JSON v4 registra `size_dp` apenas quando definido.
- `entryBarMetrics` aplica a escala relativa à métrica da barra no renderer
  compartilhado para xnote, xcanvas e preview. `toolbarDepth` considera o maior
  elemento em cada faixa de sections com 1–3 linhas.
- O configurador apresenta os cinco controles de dimensão ao lado da prévia
  no tablet e logo abaixo em janelas estreitas. Tocar um elemento da prévia
  seleciona sua ocorrência, mostra controle individual e botão para voltar às
  dimensões da barra. A prévia continua acessível durante o ajuste.
- `:app:testDebugUnitTest :app:assembleDebug --offline` e `git diff --check`
  passaram. Testes cobrem duas ocorrências de `Colours`, reset automático,
  round-trip JSON, escala individual e profundidade de barra com duas faixas.
- Debug instalado em `emulator-5554`. QA visual em 1920×1200/320 dpi:
  painel adjacente à prévia, seleção `Pen group`, ajuste de 58 dp refletido
  imediatamente na prévia, retorno a Auto e seleção de `Colours` confirmados.
- Não foi instalado no Redmi Pad 2 (não conectado nesta rodada). Persistência
  após reiniciar app, diferentes posições de dock, rotação, notas/canvas reais
  e gestos com Xiaomi Smart Pen ainda requerem QA físico.

### Próxima ação vigente

1. Com Redmi Pad 2 conectado, instalar só `com.xnotes.toolbarsdebug` e comparar
   tamanhos individuais entre prévia, nota e canvas (docked e floating).
2. Verificar persistência com ferramentas duplicadas/sections de várias linhas,
   reinício, retrato/paisagem e layout compacto; corrigir diferenças visuais.
3. Implementar atalho simples dentro da nota para abrir o configurador dedicado,
   sem reintroduzir editor ou redimensionamento in-note.
