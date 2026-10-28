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
| M3 Editor/preview | PARCIAL | Config view, CRUD, grade 3x3 e live preview simples; sem undo/apply |
| M4 Aparência/tamanho | PENDENTE | Só presets básicos; width/height e aparência precisam implementação |
| M5 Elementos/cores | PENDENTE | Contagem global legado existe; quantidade/layout por barra não |
| M6 Grupos/grades | PENDENTE | Seções planas; schema de Group/Grid ainda não implementado |
| M7 Caneta unificada | PENDENTE | Não implementada |
| M8 Contextual/resize | PENDENTE | Sem menu de barra e modo resize transacional |
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

- [ ] [M1] Revisar persistência versionada, invalidação de IDs e migração legado.
- [ ] [M3] Criar view-model de edição em rascunho com undo/redo, aplicar/cancelar.
- [ ] [M4] Definir GeometryResolver puro e testes; width/height auto/manual.
- [ ] [M4] Adicionar cores, fundo, opacidade, borda, raio, padding, blur/fallback.
- [ ] [M3/M4] Fazer preview utilizar o MESMO resolvedor de geometria do renderer.

### P2 — Customização estrutural e ações

- [ ] [M5] Tamanho/espaço/hitbox por elemento.
- [ ] [M5] Número de cores por barra e grade de swatches.
- [ ] [M6] Schema ToolbarNode v3 com Group/Grid e migração + testes.
- [ ] [M6] Grid 2x2, 3x2, auto fit e drag/drop entre grupos.
- [ ] [M7] Caneta unificada usando tipos existentes.
- [ ] [M8] Menu de contexto em longpress sem deslocamento.
- [ ] [M8] Lock/Unlock e resize on-canvas em modo edit transacional.
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

## Próxima ação (uma por vez)

Validar no ADB o configurador, os botões Undo/Redo/Cancel/Apply e a persistência
de dimensões/aparência após reinício. Em seguida concluir preview/grade 3×3,
overflow e matriz de gestos/docks no canvas; implementar grupos/grades de
elementos, cores por barra e caneta unificada. Reservar pressão, hover, latência
e primeiro traço para o Redmi Pad 2.
