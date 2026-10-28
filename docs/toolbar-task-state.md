# Estado executável — XNotes Toolbars

Atualizado em 28/10/2026. Este arquivo é o checkpoint obrigatório para qualquer sessão Codex.
Guia completo: docs/toolbar-implementation-guide.md. Roteiro ADB:
docs/toolbar-adb-test-playbook.md.

## Projeto e ambiente verificados

- Diretório ativo: /home/wesley/Projetos/xnotes-android-toolbars
- Clone separado do upstream shardulvs/xnotes-android; fork configurado como origin.
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
| M3 Editor/preview | PARCIAL | Config view, CRUD, grade 3x3, preview, undo/redo/apply/cancel; QA completa pendente |
| M4 Aparência/tamanho | PARCIAL | Resolver, dimensões, aparência e controles implementados; blur real/snapshots/QA visual pendentes |
| M5 Elementos/cores | PARCIAL | Contagem por barra, colunas e tokens individuais básicos; hitbox por elemento e grid dedicado pendentes |
| M6 Grupos/grades | PARCIAL | Schema v3 Group/Grid/ColorPalette e projeção implementados; editor direto/drag-drop/auto-fit pendentes |
| M7 Caneta unificada | PARCIAL | UnifiedPen persistente e launcher compatível; menu/último estilo/QA física pendentes |
| M8 Contextual/resize | PARCIAL | Menu contextual e Lock/Unlock implementados; resize transacional pendente |
| M9 Perfis/QA/performance | PARCIAL | Perfis Shared/independent e labels implementados; TalkBack/performance/Redmi Pad pendentes |
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

## Fila priorizada por dependências

### P0 — Confiabilidade antes de adicionar complexidade

- [x] [M0] Compilar/testar NESTE clone; verificar origem da branch (28/10/2026).
      Gradle unit tests + assembleDebug passaram e APK instalou no emulator-5554.
- [ ] [M2] Testar toque e long-press em ferramentas reais. Resolver conflitos
      entre drag do contêiner e long-press próprios de ferramentas/cores.
- [ ] [M2] Testar dock principal topo/baixo/esquerda/direita com mover por hold;
      garantir que esquerda/direita não são convertidas inadvertidamente em float.
- [ ] [M2] Testar 9 posições float note/canvas em retrato, landscape e split.
- [ ] [M2] Verificar suporte a insets, barra fora da tela e overlap com duas barras.
- [ ] [M3] Abrir configurador no emulator e validar visualmente preview e grade,
      com seleção por toque, arraste, duplicação, exclusão e restauração.

### P1 — Bases para toda a personalização

- [x] [M1] Revisar persistência versionada, invalidação de IDs e migração legado
      (28/10/2026; c09da34a; codec limitado e testes de fallback/IDs/schema).
- [ ] [M3] Criar view-model de edição em rascunho com undo/redo, aplicar/cancelar.
- [x] [M4] Definir GeometryResolver puro e testes; width/height auto/manual (20a4be4d).
- [x] [M4] Adicionar cores, fundo, opacidade, borda, raio, padding, blur/fallback (81aeba23, 8286ae38, dc52b5f1).
- [x] [M3/M4] Fazer preview utilizar o MESMO resolvedor de geometria do renderer (20a4be4d).

### P2 — Customização estrutural e ações

- [x] [M5] Tamanho/espaço/hitbox por elemento (fb50bae7; nós explícitos aplicados ao renderer).
- [x] [M5] Número de cores por barra e grade de swatches (2595956a; QA visual pendente).
- [x] [M6] Schema ToolbarNode v3 com Group/Grid e migração + testes (fd9c8a89, aee8a604).
- [ ] [M6] Grid 2x2, 3x2, auto fit e drag/drop entre grupos.
- [x] [M7] Caneta unificada usando tipos existentes (aee8a604; QA física pendente).
- [x] [M8] Menu de contexto em longpress sem deslocamento (53bfc890, 607341a0).
- [ ] [M8] Lock/Unlock e resize on-canvas em modo edit transacional.
- [x] [M9] UI de compartilhamento/independência xnote/xcanvas (7cd3305d).

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

- 27/10/2026: preflight nesta sessão em 6a5d6d64: worktree limpo,
  emulator-5554 online; Gradle testDebugUnitTest + assembleDebug passou
  (50 tasks, 6 executadas, 44 up-to-date; aviso CXX5304 sobre versão XML do SDK).
  install -r retornou Success. Criada nota de teste untitled_2 no app isolado;
  toque no primeiro ícone de caneta selecionou-o. Pressão de 1000 ms no mesmo
  ícone não mostrou menu visual; comportamento esperado do controle e eventual
  conflito com detector do contêiner ainda precisam ser investigados. Nenhum
  cenário P0 completo; nenhum teste de stylus físico foi executado.

- 27/10/2026: em 6a5d6d64, conferido ToolButton: opções de caneta
  abrem ao tocar novamente na ferramenta selecionada, e foram observadas no
  emulador. O hold sem menu não comprova falha desse controle. Com eventos
  ADB motionevent DOWN, espera de 1s, MOVE e UP, arrastar o separador da
  barra principal do topo para o centro a tornou flutuante; houve sobreposição
  com desfazer/refazer. Outro arraste levou-a ao topo ainda flutuante. Esses
  são testes parciais de dedo, não validam os quatro docks, demais controles,
  conflito de long-press ou stylus. A configuração de teste foi alterada no
  app isolado; a nota de teste untitled_2 permanece nele.

- 27/10/2026: no emulator-5554, após retornar a barra flutuante ao topo,
  DOWN no ícone da caneta, espera de 1s, MOVE até o centro e UP moveu a
  barra ao centro; os controles laterais de desfazer/refazer ficaram cobertos.
  ToolbarAround usa cobertura por borda e, com primary flutuante do workspace,
  zera onCover; uma região central não é representada por esse contrato.
  Falta projetar e testar a gestão de colisões/oclusão, inclusive secundárias,
  antes de afirmar que a interação ou a geometria passou. Não houve teste
  físico de pressão, hover, latência ou primeiro traço nesta rodada.

- 27/10/2026: commit 088b2a3d calcula a âncora pela posição de soltura
  em vez do centro da barra; testes JVM cobrem as nove regiões e entradas
  inválidas. Gradle testDebugUnitTest + assembleDebug: BUILD SUCCESSFUL
  (16 executadas, 34 up-to-date); git diff --check passou; install -r no
  emulator-5554 retornou Success após reiniciar o AVD desconectado. Na nota
  de teste, hold seguido de MOVE vertical e depois MOVE para x=100, y=600
  posicionou a barra flutuante na lateral esquerda. Duas tentativas de MOVE
  diagonal direto rolaram o conteúdo horizontal em vez de mover a barra:
  conflito com scroll segue pendente. Persistência após restart e demais
  posições não foram verificadas nesta rodada; não marcar P0 concluído.

- 27/10/2026: commit eea7815d dá prioridade ao drag após hold no passe
  Initial, antes do scroll interno. testDebugUnitTest + assembleDebug passou
  (14 executadas, 36 up-to-date); diff --check passou e install -r no
  emulator-5554 retornou Success. Hold e MOVE diagonal da barra vertical
  esquerda para o rodapé e do rodapé para a esquerda moveram a barra;
  dois toques na caneta selecionaram-na e abriram suas opções. Não há teste
  instrumentado automatizado desse gesto; cancelamento, outros controles,
  scroll normal, secundárias e stylus permanecem por validar. Não marcar P0.

- 27/10/2026: no emulator-5554, swipe curto na barra vertical rolou os
  itens; DOWN/hold/MOVE/CANCEL deixou a barra na âncora esquerda. ADB não
  comprova todos os tipos de cancelamento. A prévia mostrava duas barras na
  mesma âncora sobrepostas; commit b42734c2 agrupa-as em pilha rolável.
  Gradle testDebugUnitTest + assembleDebug passou (13 executadas, 37
  up-to-date), diff --check e install -r passaram. Prévia com duas barras
  mostrou-as separadas e toque selecionou a segunda; duplicação até quatro e
  swipe alcançaram as barras inferiores. As duas duplicatas de teste foram
  removidas. Faltam indicação explícita de overflow, comparação geométrica
  com renderer real, testes automatizados de UI e demais âncoras; M3 segue
  parcial. A sobreposição real da barra longa com secundárias continua aberta.

- 27/10/2026: commit 5ddbd25d limita a altura das barras que compartilham
  âncora e torna a pilha rolável; testes JVM cobrem limites para uma, duas e
  várias barras e espaço inválido. testDebugUnitTest + assembleDebug passou
  (16 executadas, 34 up-to-date), diff --check e install -r passaram. No
  emulator-5554, duas barras verticais na esquerda ficaram visíveis sem se
  sobrepor; a principal rolou, a secundária foi arrastada para a direita e
  de volta à esquerda, e ambas voltaram separadas após reiniciar o app.
  Faltam QA de pilhas maiores, indicação explícita de overflow, âncoras
  distintas, telas pequenas, canvas e testes instrumentados; não marcar M2.

- 27/10/2026: commit be3ca074 compartilha controles de rolagem de pilha
  entre preview e renderer, com ícones contrastantes e labels acessíveis.
  Primeira compilação falhou por import ausente de animateScrollBy; corrigida.
  testDebugUnitTest + assembleDebug final passou (14 executadas, 36
  up-to-date), diff --check e install -r passaram. No emulator-5554, duas
  barras na prévia mostraram seta e o toque revelou a segunda; com quatro
  barras, a seta avançou a pilha na prévia e na nota. Duas duplicatas criadas
  para QA foram removidas. Faltam testes instrumentados, confirmação de
  acessibilidade por leitor, orientação, canvas e telas pequenas; o resolvedor
  de geometria e colisões entre âncoras seguem pendentes.

- 27/10/2026: commit d36b9f77 introduz regiões puras para âncoras ocupadas,
  usadas no renderer e na prévia; divide em terços quando há linhas ou colunas
  distintas, mantendo rolagem em barras restringidas. Testes JVM verificam as
  nove regiões sem interseção, eixo único e dimensões inválidas. Gradle
  testDebugUnitTest + assembleDebug passou (última execução 13 executadas,
  37 up-to-date), diff --check e install -r passaram. No emulator-5554,
  barra principal centro-esquerda e secundária topo-centro, depois topo-esquerda,
  ficaram separadas; secundária foi devolvida ao centro-esquerda. Este método
  conservador pode reduzir significativamente o espaço das ferramentas; faltam
  QA de regiões pequenas, nove âncoras, preview, canvas, split e oclusões de
  outros overlays. Não equivale ao GeometryResolver completo nem conclui M2/M4.

- 27/10/2026: no emulator-5554 foi criado o canvas de teste untitled_3.
  Hold-drag da barra fixa do topo para a esquerda manteve-a vertical e reservou
  espaço; após rotacionar para retrato ela permaneceu à esquerda. Configurações
  do emulador restauradas: user_rotation=0 e accelerometer_rotation=1.
- 27/10/2026: commit ac4ef5ee usa posicionamento absoluto físico para regiões
  e arraste em RTL. Gradle testDebugUnitTest + assembleDebug passou (13
  executadas, 37 up-to-date), diff --check e install -r passaram. Com locale
  temporário ar no app isolado, uma secundária arrastada da esquerda para a
  direita apareceu na borda física direita; devolvida à esquerda e locale
  restaurado para lista vazia. A tela inicial em RTL apresentou sobreposição
  de sidebar e conteúdo, fora desta correção; prévia RTL, teste automatizado,
  demais âncoras e gestos continuam pendentes. Canvas multi-barra, 9 âncoras,
  split e teste físico com stylus não foram validados nesta rodada.

- 28/10/2026: commit af83f40d isola a aparência efetiva da barra primária por
  contexto. `ToolbarLook` agora é projetado da geometria primária da nota/canvas
  dentro de cada pane; mover a barra do canvas não altera mais a preferência
  legada global nem reposiciona implicitamente a nota. `moveToolbarBar` mantém a
  operação no workspace escolhido. Testes JVM cobrem fallback legado,
  posição/tamanho/float por geometria e âncoras de canto. `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest`
  passou; `git diff --check`, assembleDebug e install -r no emulator-5554
  passaram. Activity reiniciada e captura `/tmp/xnotes-toolbar-dimensions.png`
  feita sem crash. Ainda não há teste instrumentado de split/contexto.

- 28/10/2026: commit 20a4be4d adiciona `ToolbarGeometryResolver` puro para
  dimensões manuais opcionais, preservando auto/wrap-content e limitando valores
  a 48..2048 dp. O frame real recebe a geometria primária/extra e a prévia usa
  o mesmo resolvedor. `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
  passou (50 tasks, 10 executadas, 40 up-to-date), `git diff --check` passou e
  install -r no emulator-5554 retornou Success. Não marcar M4: aparência,
  edição de dimensões na UI, snapshots instrumentados e comparação visual ainda
  faltam.

- 28/10/2026: commit 81aeba23 adiciona `ToolbarAppearance` imutável por barra,
  com fundo RGBA opcional, opacidade, borda, raio, padding, espaçamento e
  solicitação de blur limitada com fallback seguro sem blur. O codec de
  workspaces faz round-trip e aplica clamps (opacidade 0..1, dimensões de
  aparência e blur); o frame e a prévia usam os mesmos tokens. Teste de
  persistência/clamp foi adicionado. `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
  passou e install -r no emulator-5554 retornou Success. Os controles de
  edição desses tokens ainda não estão expostos na PreferencesPane; não marcar
  M4 concluído.

- 28/10/2026: commit 8286ae38 expõe no editor de barras controles para opacidade,
  borda, largura/altura manual ou automática, raio e padding, aplicando-os ao
  workspace selecionado. O fluxo continua gravação imediata (rascunho/undo/
  cancelar ainda pendentes). `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
  passou e install -r no emulator-5554 retornou Success. Não foi possível
  validar toque visual no configurador nesta rodada; a Activity foi instalada
  sem limpar os dados isolados.

- 28/10/2026: commit 612e4296 adiciona histórico limitado de undo/redo para
  alterações de workspaces e botões Cancel/Apply no editor de barras. Cancel
  restaura o snapshot capturado ao abrir o configurador; Apply fecha mantendo
  as alterações persistidas. A gravação ainda é imediata durante a edição, mas
  o histórico permite reversão. `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
  passou e install -r retornou Success no emulator-5554. Falta QA de toque
  efetivo dos botões e teste de reinício/cancelamento pelo ADB.

- 28/10/2026: commit 77470460 usa `geometry.columns` (limitado a 1..6) para
  renderizar uma grade horizontal no `ToolbarFrame`, com espaçamento de
  aparência; rails verticais permanecem lineares para preservar rolagem e
  orientação. O editor expõe o número de colunas. Testes JVM, assembleDebug e
  install -r passaram no emulator-5554. Ainda faltam grid de cores dedicado,
  grupos/nós v3, validação visual de capacidade e drag/drop entre grupos.

- 28/10/2026: commit 91fcb058 adiciona `colorCount` opcional por barra, com
  fallback ao contador global e codec/clamp de 1..15. Note e canvas recebem o
  valor via CompositionLocal sem copiar a paleta mutável; o editor expõe slider
  e retorno ao modo Shared. Teste JVM de round-trip/clamp, assembleDebug e
  install -r passaram no emulator-5554. Grade NxM de swatches e arraste entre
  grupos continuam pendentes.

- 28/10/2026: QA ADB após `91fcb058`/`612e4296`: Preferences abriu no
  emulator-5554, o configurador exibiu preview com duas barras empilhadas e
  controles Undo/Redo/Cancel/Apply. Toque em Duplicate criou temporariamente
  `3. Toolbar 3`; toque em Cancel retornou à Preferences e, ao reabrir o
  configurador, apenas `2. Toolbar 2` permaneceu, comprovando restauração do
  snapshot sem apagar dados do app isolado. Screenshot `/tmp/toolbar-config-qa.png`.

- 28/10/2026: commit 53bfc890 adiciona menu contextual em long-press da barra
  com Lock/Unlock, Duplicate e Hide para secundárias, além de operações de
  atualização/duplicação/ocultação persistidas no workspace correto. Commit
  607341a0 corrige o caso bloqueado: o movimento fica desabilitado, mas o
  long-press continua acessível para Unlock. JVM/assembleDebug/install -r
  passaram. No emulator-5554, long-press abriu `Lock`/`Duplicate`, Lock foi
  acionado, reinício preservou o estado e novo long-press exibiu `Unlock` e
  `Duplicate`. A interação com botão de ferramenta foi preservada no teste
  de toque curto; pressão/hover de stylus continuam dependentes do Redmi Pad 2.

- 28/10/2026: commit dc52b5f1 adiciona picker RGBA de fundo por barra, com
  opção de voltar ao tema. O valor usa os recents existentes e é persistido no
  codec de aparência, sem duplicar estado de tinta. JVM/assembleDebug/install -r
  passaram no emulator-5554; interação do picker e teste visual de opacidade
  ainda precisam de uma rodada dedicada.

- 28/10/2026: commit c3522fb3 adiciona content descriptions estáveis (IDs dos
  itens) aos controles da prévia, incluindo swatches, para leitores de tela e
  navegação sem toque visual. `testDebugUnitTest`, assembleDebug e install -r
  passaram; validação com TalkBack/leitor real permanece pendente.

- 28/10/2026: commit 7cd3305d expõe o toggle Shared/independent no editor de
  workspaces. A alternância usa o campo persistido existente e não substitui o
  perfil canvas oculto; o filtro por contexto continua aplicado no renderer.
  JVM/assembleDebug/install -r passaram no emulator-5554. QA visual do toggle
  e alternância com perfis distintos ainda pendentes.

- 28/10/2026: commit fd9c8a89 adiciona schema v3 finito de `ToolbarNode`, com
  Tool, ColorPalette, Group, Grid, Spacer e Separator, limites de profundidade
  3/filhos 64, sanitização de IDs/dimensões e codec versionado que aceita v1–v3
  e grava v3. Barras legadas continuam usando `ToolbarLayout`; barras com nós
  explícitos projetam uma lista segura no renderer de barras secundárias.
  Testes JVM cobrem round-trip, flatten e rejeição de profundidade insegura;
  assembleDebug/install -r passaram no emulator-5554. Editor visual de nós,
  drag/drop entre grupos e grids de cores dedicados ainda pendentes.

- 28/10/2026: commit 1f8e7692 faz a projeção v3 ser usada também pela barra
  primária de note/canvas (secundárias já usavam `effectiveLayout`). Assim uma
  barra com nós explícitos não fica limitada ao layout plano apenas por ser a
  principal. Testes JVM, assembleDebug e install -r passaram no emulator-5554;
  falta QA visual com uma árvore v3 efetivamente criada na UI.

- 28/10/2026: commit 362774ef adiciona ação `Compose v3`/`Use flat layout` no
  editor. A conversão gera IDs estáveis por seção/item, trata `COLORS` como
  ColorPalette e sincroniza alterações posteriores do layout com a árvore v3;
  a barra primária e secundária usam a mesma projeção. Testes JVM, assembleDebug
  e install -r passaram no emulator-5554. Ainda falta drag/drop visual entre
  grupos e edição direta de propriedades de cada nó.

- 28/10/2026: commit aee8a604 adiciona `UnifiedPen` ao schema v3. O nó
  persiste estilos stroke válidos e `lastStyle`, projeta um único item PEN no
  renderer e reutiliza o popup/configuração existente sem inventar novos IDs de
  ferramenta. Round-trip/sanitização JVM, assembleDebug e install -r passaram
  no emulator-5554. Alternância visual de estilos e latência com caneta física
  continuam pendentes.

- 28/10/2026: commit 77f0276d propaga as colunas do primeiro `Grid` válido da
  árvore v3 para o `ToolbarFrame`, com fallback para `geometry.columns`; note,
  canvas, barras primárias e secundárias usam o mesmo valor. Teste puro de
  resolução, JVM/assembleDebug e install -r passaram no emulator-5554.
  Capacidade de grids mistos e validação visual de overflow ainda pendentes.

- 28/10/2026: commit fb50bae7 liga `visualDp` e `hitboxDp` dos nós Tool ao
  `ToolbarIcon` real via CompositionLocal, com fallback para os presets. Teste
  JVM verifica métricas por Tool; assembleDebug/install -r passaram no
  emulator-5554. A edição direta desses campos ainda não está exposta no
  painel, e QA de hitbox com caneta física permanece pendente.

- 28/10/2026: commit 4cf0b826 expõe `Visual dp` e `Hitbox dp` no editor para o
  primeiro item Tool da composição v3 e atualiza a árvore recursivamente. Teste
  JVM de atualização, assembleDebug e install -r passaram no emulator-5554.
  Edição em lote de todos os nós e QA de acessibilidade/stylus ainda pendentes.

- 28/10/2026: commit c1477cc2 adiciona edição persistente das colunas do primeiro
  `ColorPalette` v3 (`Swatch columns`), usa essa grade na resolução comum da barra
  e cobre a atualização recursiva com teste JVM. `testDebugUnitTest`,
  `assembleDebug` e `install -r` passaram no emulator-5554. Edição de múltiplos
  palettes/grids mistos e validação visual 3×2 continuam pendentes.

- 28/10/2026: `connectedDebugAndroidTest` também concluiu `BUILD SUCCESSFUL`
  no emulator-5554 após c1477cc2 (70 tasks; sem testes instrumentados
  específicos reportados). Isso não substitui a matriz manual de gestos nem a
  validação de pressão/hover com caneta.

- 28/10/2026: commit fd14ddae expõe `Grid columns` e `Grid rows` no editor
  quando a barra usa composição v3, com atualização recursiva e teste JVM.
  `testDebugUnitTest`, `assembleDebug`, install -r e reinício da Activity no
  emulator-5554 passaram. Ainda falta auto-fit, capacidade/overflow de grids
  mistos e drag/drop entre grupos.

- 28/10/2026: commit fea94063 propaga `Grid.rows` para o `FlowRow` real das
  barras primárias e secundárias, limitando linhas além das colunas configuradas.
  O teste JVM de resolução de linhas passou; `testDebugUnitTest`, assembleDebug
  e install -r passaram no emulator-5554. Auto-fit e grids mistos seguem
  pendentes.

- 28/10/2026: após fea94063, `connectedDebugAndroidTest` concluiu novamente
  `BUILD SUCCESSFUL` no emulator-5554 (70 tasks; sem testes instrumentados
  específicos). O APK já havia sido instalado com `install -r` e a Activity
  reiniciada sem limpar dados.

- 28/10/2026: commit 9d2f33d0 adiciona `moveNode` puro para drag/drop v3 entre
  `Group` e `Grid`, com extração/inserção limitada, preservação de IDs e
  rejeição de destino inexistente/auto-destino. Testes JVM, assembleDebug e
  install -r passaram. A UI ainda usa o editor plano; integração visual do
  arraste entre grupos permanece pendente.

- 28/10/2026: commit bca0e63f adiciona handle de resize na prévia para a barra
  selecionada. O gesto acumula o delta até soltar, aplica limites 48–2048 dp e
  passa pelo histórico de rascunho do Editor (Undo/Cancel/Apply); o handle tem
  descrição semântica `Resize toolbar <id>`. `testDebugUnitTest`, assembleDebug
  e install -r passaram no emulator-5554. Resize on-canvas fora do configurador
  e validação visual do gesto ainda pendentes.

- 28/10/2026: `connectedDebugAndroidTest` após bca0e63f concluiu
  `BUILD SUCCESSFUL` no emulator-5554; a tentativa de navegação automatizada
  não alcançou o configurador nesta rodada, portanto o handle não foi marcado
  como validado visualmente. Nenhum dado foi limpo.

- 28/10/2026: commit 501fd0ba adiciona `Restore hidden (N)` ao configurador,
  reexibindo barras ocultas no perfil ativo sem apagar o perfil canvas quando
  `Shared` está ligado. `testDebugUnitTest`, assembleDebug e install -r
  passaram no emulator-5554; teste visual do fluxo de recuperação permanece
  pendente.

- 28/10/2026: após 501fd0ba, `connectedDebugAndroidTest` concluiu
  `BUILD SUCCESSFUL` no emulator-5554 (70 tasks). O fluxo foi instalado sem
  limpar dados; a recuperação visual ainda precisa de uma barra ocultada
  intencionalmente para validação manual dedicada.

- 28/10/2026: QA ADB manual completou a recuperação: no configurador, a barra
  `Toolbar 2` foi selecionada, `Visible` desmarcado fez aparecer
  `Restore hidden (1)`, e tocar nessa ação restaurou o estado para
  `Restore hidden` sem contador. A alteração foi cancelada antes de sair; não
  houve limpeza de dados nem alteração do app original.

- 28/10/2026: commit ac3a30b4 adiciona `autoFit` ao nó `Grid` v3, persiste o
  campo no JSON, calcula colunas pela capacidade dos filhos (ou pelas linhas
  explícitas) e expõe `Grid auto-fit` no editor. Testes JVM, assembleDebug,
  install -r e `connectedDebugAndroidTest` passaram no emulator-5554. Grids
  mistos e validação visual específica do auto-fit continuam pendentes.

- 28/10/2026: commit a6781832 conecta a composição v3 ao editor com uma lista
  `Composition nodes`; cada nó de primeiro nível mostra tipo/ID e pode subir ou
  descer preservando a árvore e o histórico de rascunho. `testDebugUnitTest`,
  assembleDebug e install -r passaram no emulator-5554. Reordenação aninhada,
  drag/drop visual entre grupos e QA da tela ainda pendentes.

- 28/10/2026: commit 13ee0200 estende o editor de composição para mostrar nós
  aninhados indentados e reordená-los dentro de `Group`/`Grid`; o teste JVM
  cobre a movimentação de irmãos aninhados. `testDebugUnitTest`, assembleDebug
  (build anterior sem alteração de produção adicional) e install -r passaram.
  Drag/drop por gesto e movimentação entre pais diferentes ainda pendentes.

- 28/10/2026: `connectedDebugAndroidTest` após 13ee0200 concluiu
  `BUILD SUCCESSFUL` no emulator-5554 (70 tasks; sem testes instrumentados
  específicos). A árvore aninhada ainda requer QA visual dedicada.

- 28/10/2026: commit 7fbf72c9 adiciona long-press + drag no editor de nós v3.
  Os limites medidos escolhem o alvo mais próximo e chamam `moveNodeBefore`,
  permitindo reordenação entre pais sem duplicar IDs; o item arrastado fica
  atenuado durante o gesto. `testDebugUnitTest`, assembleDebug, install -r e
  `connectedDebugAndroidTest` passaram no emulator-5554. QA manual do gesto,
  feedback visual de destino e cancelamento por stylus ainda pendentes.

- 28/10/2026: a regressão encontrada no cenário nó aninhado → alvo na raiz foi
  corrigida em 83a3d952; a extração agora é recursiva também para destinos da
  raiz, com teste JVM dedicado. Após a correção, `testDebugUnitTest`,
  `assembleDebug` e install -r passaram no emulator-5554.

- 28/10/2026: `connectedDebugAndroidTest` após 83a3d952 concluiu
  `BUILD SUCCESSFUL` no emulator-5554; nenhuma suíte instrumentada específica
  foi reportada.

- 28/10/2026: QA ADB reabriu `Configure toolbars` e confirmou a tela de
  composição plana (`Tools and groups`, chips e `Add section`). Nesta rodada o
  gesto de scroll não alcançou a seção inferior do editor v3; portanto a árvore
  `Composition nodes` e o drag visual não foram marcados como PASS.

- 28/10/2026: QA ADB no emulator-5554 abriu `Preferences > Configure toolbars`
  e confirmou a prévia com overflow explícito (`+28`), os perfis `xnote`,
  `xcanvas` e `Shared`, além dos botões `Undo`, `Redo`, `Cancel` e `Apply`.
  `Add Toolbar` criou `Toolbar 3`; `Undo` removeu-a e `Redo` restaurou-a.
  Uma criação seguida de `Cancel` retornou ao explorador sem alterar a lista
  persistida. Nenhum dado do app original foi tocado; validação de gestos em
  ferramenta, posições/docks e stylus físico permanece pendente.

- 28/10/2026: após a instalação de `77f0276d`/`aee8a604`, Activity isolada foi
  reiniciada no emulator-5554; após o loader, `topResumedActivity` confirmou
  `com.xnotes.toolbarsdebug/com.xnotes.MainActivity` e `uiautomator dump` foi
  gerado sem crash. Captura inicial `/tmp/xnotes-latest.png`; não houve limpeza
  de dados nem alteração do app original.

- 28/10/2026: commit c09da34a limita a leitura de árvores v3 malformadas no
  codec de settings a 128 nós e profundidade 3 antes da sanitização, evitando
  recursão ilimitada em JSON importado. Foi adicionada regressão JVM com árvore
  de 256 níveis; `:app:testDebugUnitTest --tests
  com.xnotes.settings.ToolbarWorkspaceJsonTest` passou. O gate completo
  `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest
  :app:assembleDebug --console=plain` passou (50 tasks; 10 executadas, 40
  up-to-date), e `git diff --check` passou. Nesta rodada `adb devices -l` não
  listou nenhum dispositivo online; instalação, reinício e QA ADB ficaram
  pendentes. A alteração local não commitada em `PreferencesPane.kt` foi
  preservada fora do commit.

- 28/10/2026: commit 2595956a conecta `ColorPalette.columns` ao renderer real
  de note e canvas por uma grade dedicada, horizontal ou vertical conforme a
  orientação. A grade geral da barra deixou de interpretar colunas da paleta,
  evitando transformar ferramentas não relacionadas em uma grade. O contador
  por barra continua limitado pela paleta compartilhada e os índices de cor,
  popup e drag permanecem os mesmos. `git diff --check` e o gate completo
  `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest
  :app:assembleDebug --console=plain` passaram (50 tasks; 14 executadas, 36
  up-to-date). Não houve ADB nesta rodada: `adb devices -l` segue sem serial
  online, portanto 3×2 visual, orientação e acionamento em dispositivo ainda
  não são PASS.

- 28/10/2026: commit befa68c1 corrige a resolução de capacidade de `Grid`:
  `effectiveRows()` agora calcula as linhas mínimas para os filhos visíveis,
  respeitando colunas explícitas e `autoFit` até o limite 1..6, evitando que
  `maxLines` descarte itens quando `columns × rows` era insuficiente. O caso
  de seis itens em grade 2×1 está coberto por teste JVM. `git diff --check` e
  `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest
  :app:assembleDebug --console=plain` passaram (50 tasks; 14 executadas, 36
  up-to-date). Overflow acima de 6 linhas e validação visual no emulador ainda
  ficam pendentes; não houve serial ADB online.

## Próxima ação (uma por vez)

Validar no ADB a grade 3×2 de cores, a persistência após reinício e a matriz de
gestos/docks assim que um dispositivo autorizado estiver online. Em paralelo,
implementar o próximo item funcional desbloqueado: validação de capacidade e
auto-fit para grids mistos, com overflow explícito e sem clipping. Reservar
pressão, hover, latência e primeiro traço para o Redmi Pad 2.
