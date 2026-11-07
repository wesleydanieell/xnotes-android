# Estado executável — XNotes Toolbars

## Community document-tabs integration — 06/11/2026

- Updated n-notes through upstream `3f7d7c2f` (0.9.7-0.53) and restored the
  complete customizable-toolbar changes at `5e786579`.
- Adapted `feat/document-tabs-toolbar-element` (XNotes commit `c198c064`) to
  n-notes: reader actions, image-as-page, preference categories/search, OCR and
  cloud-sync UI are retained. The same animated switcher is an optional element
  for any note/canvas toolbar, including a separate movable tabs-only bar.
- 2,729 JVM tests passed without failures/errors/skips. Both debug and community
  Kotlin compilation passed. Final APK packaging/install evidence is recorded
  in `COMMUNITY_INTEGRATION.md`.
- Builds are run sequentially on this machine; concurrent variant compilation
  exhausted the old heap. Reproducible local command:
  `./gradlew :app:testDebugUnitTest :app:assembleCommunity :app:assembleDebug
  --no-parallel --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
  '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8'`.
- Emulator closed at the user's request. Full animation/gesture/stylus QA is
  not claimed; next action is manual validation on a separate test note.


## Presets aplicam também o modo da principal — 06/11/2026

- Relato: aplicar workspace mudava elementos, mas podia manter o modo docked/
  floating anterior. Aplicação a um só contexto preserva a aparência global;
  por isso a Main importada ainda herdava floating/posição do estado atual.
- `applyToolbarPresetToContext` resolve a posição herdada usando a aparência do
  preset e grava a geometria explícita na Main importada. Perfil inativo, cores
  e aparência global são preservados; compartilhamento é desligado como antes.
- `toolbarDockEdge` passa a priorizar dock explícito sobre aparência global
  floating. Inspetores e cópia usam a mesma regra, alinhando modos exibidos e
  resolvedores. `geometry.floating = true` continua prevalecendo sobre dock salvo.
- Três regressões cobrem aplicação aos dois contextos em modos conflitantes,
  preset compartilhado/legado, round-trip JSON e cópia da Main já importada.
- Gate offline: 2.479 testes sem falhas/erros/skips; assembleDebug e diff-check
  passaram. APK debug instalado no emulador isolado.
- Smoke UI passou: com aparência global floating, aplicar Original só ao XNotes
  gravou Main docked TOP; aplicar Escrita sem distrações em seguida gravou Main
  floating BOTTOM_CENTER. Em ambos, canvas inativo e aparência global ficaram
  iguais. Settings originais do emulador restaurados e comparados ao backup.
- Não é QA completo de stylus/rotação. A aplicação aos dois perfis continua
  restaurando a aparência/paleta global como antes; o ajuste é no destino único.

## Conversão de modo preservando a borda — 06/11/2026

- Relatos: barra docked não virava floating; floating na direita voltava ao topo
  ao encaixar. Os controles usavam âncora flutuante antiga ou TOP como fallback,
  mesmo quando a borda efetiva era lateral. Uma altura lateral explícita podia
  virar profundidade horizontal e causar rejeição falsa por capacidade.
- `toolbarFloatingGeometry` deriva a âncora da borda efetiva e limpa `dockEdge`.
  `toolbarDockedGeometry` deriva o encaixe da âncora flutuante atual, respeitando
  também o modo legado da principal. Main e secundárias usam os mesmos helpers;
  dimensões e faixa são preservadas, sem largura de 200 dp ou TOP forçados.
- Na Main, a posição global é sincronizada com a borda escolhida ao encaixar;
  o seletor usa a borda explícita quando ela existe.
- Três regressões percorrem Main/secundárias, quatro bordas, ida/volta sem
  modificar dimensões, altura lateral rejeitada pela âncora antiga e a principal
  legada flutuante cuja posição vem da aparência global.
- Gate offline: 2.476 testes sem falhas/erros/skips, assembleDebug e diff-check
  passaram. APK debug instalado no emulador isolado.
- Smoke com fixture temporária passou para Main e secundária: docked direita →
  floating direita → docked direita, verificando geometria persistida. A primeira
  tentativa passou na Main, mas parou no seletor fora da tela; o script passou
  completo após rolar ao seletor. Settings originais restaurados e comparados
  ao backup no final de ambas as tentativas. Sem alteração nas notas.
- Não substitui QA com stylus nem a matriz visual completa de rotação/encaixes.

## Reset sem ocultar outras barras na prévia — 06/11/2026

- Relato: outra barra desaparecia da prévia ao resetar Main nas configurações.
  Reproduzido no resolvedor: Main flutuante compacta e oito secundárias cabem;
  resetar a principal aumenta seu comprimento natural e rejeita uma secundária.
  A barra permanece salva, mas a prévia não renderiza barras sem placement.
- `planToolbarReset` prepara somente o layout padrão da barra selecionada e
  valida o conjunto completo com os mesmos filtros e medidas da prévia antes
  de aplicar. Se não couber, mantém a configuração anterior e informa como
  liberar espaço. Não move/oculta outras barras nem altera dimensões salvas.
- Erros de capacidade agora aparecem perto dos controles superiores, em vez
  de ficarem no fim do inspetor. Reset aceito limpa seleção/medições de arraste.
- Duas regressões, cobrindo XNotes/XCanvas: rejeição do reset que expulsaria
  secundárias e reset com espaço preservando secundárias/geometria/origem.
- Gate offline: 2.473 testes sem falhas/erros/skips; assembleDebug e diff-check
  passaram. Build instalado no emulador isolado.
- Smoke com fixture temporária de nove barras confirmou aviso visível, nove
  barras no seletor e workspace salvo intacto após a recusa. Settings originais
  do emulador restaurados e comparados ao backup ao final. Não toca nas notas.
- Não é QA completo de stylus/rotação; reset sem espaço agora exige liberar
  espaço antes de aplicar, sem reposicionamento automático das demais barras.

## Sincronização da persistência legada — 06/11/2026

- Extraídas operações puras `Settings.withToolbarLayout` e
  `Settings.withToolbarWorkspaces`. Centralizam a sincronização entre layouts
  primários legados e perfis completos; `Editor` conserva atualização do runtime
  e gravação. Edição de layout em settings antigos mantém `toolbarWorkspaces`
  nulo, sem materialização antecipada da migração.
- Auditoria dos pontos de atribuição confirmou que salvar/aplicar presets passa
  pelos mesmos métodos de `Editor`; o leitor JSON continua responsável por
  carregar/migrar. Schema e codec não foram alterados.
- Três regressões de round-trip verificam settings legados, preservação de
  secundárias/perfil inativo e sincronização das duas principais após reordenação.
  O fallback sem `toolbar_workspaces` recupera os layouts primários legados.
- Gate offline passou: 2.471 testes, sem falhas/erros/skips; assembleDebug e
  `git diff --check` passaram. APK instalado no `emulator-5554` com Success.
- UIAutomator alterou e restaurou Movable nos dois perfis; após cada gravação,
  conferiu IDs/visibilidade/ordem de ambos os campos legados contra as principais,
  preservação do perfil inativo e restauração dos layouts/workspaces originais.
  Passou. Não cobre stylus nem a matriz completa de encaixes/rotação.
- Próximo passo: mapear dependências concretas entre modelo, migrações e medidas
  para a primeira branch de revisão extraída do upstream, sem abrir PR automático.

## Atualização imutável dos perfis — 06/11/2026

- `ToolbarWorkspaces.updateWorkspace(context, transform)` centraliza a edição de
  um perfil salvo, preservando o outro perfil e a opção `shared`. Não filtra
  ferramentas nem escolhe implicitamente o perfil de renderização.
- Configurador usa o contexto da aba em edição. O movimento em runtime continua
  direcionado a NOTE quando compartilhado; persistência e sincronização dos
  layouts primários legados permanecem em `Editor`. Copiar para XCanvas mantém
  sua transição explícita para `shared = false`.
- Duas regressões cobrem os dois contextos com compartilhamento ligado/desligado,
  preservação de ferramentas específicas, imutabilidade da origem e edição da
  fonte compartilhada sem sobrescrever o canvas independente.
- Gate offline: 2.468 testes sem falhas/erros/skips; assembleDebug passou e APK
  instalado no `emulator-5554` com Success. `git diff --check` passou.
- UIAutomator alterou Movable em xnote e xcanvas, verificou no JSON que apenas
  o perfil selecionado mudou e que `shared` foi preservado; restaurou a opção em
  cada contexto e comparou os workspaces completos com o estado inicial. Passou.
- Este smoke não percorre docks/âncoras nem mede escrita com stylus. Não houve
  alteração no desenho ou no reconhecimento do gesto de arraste.
- Próximo corte: auditar persistência e sincronização dos campos legados para
  preparar a separação das branches de Modelo/Persistência.

## Planejamento de cópia e capacidade — 06/11/2026

- Extraído `ToolbarCopyPlanner`: preserva o encaixe herdado da principal quando
  vira secundária e tenta as mesmas âncoras flutuantes, na ordem anterior, se
  a posição original não couber. Não grava configurações nem acessa `Editor`.
- `toolbarPlacementFits` compartilha a validação usada pela edição de geometria,
  cópia entre contextos e duplicação. Primeiro reserva docks, depois valida as
  flutuantes na área restante. A projeção de ferramentas usada apenas pelos
  docks na edição continua explícita no chamador; comportamento anterior mantido.
- A criação de barra conserva sua validação específica do ID recém-criado;
  não foi convertida em rejeição de todo o workspace durante esta refatoração.
- Três regressões cobrem dock herdado, fallback para flutuante sem perder
  dimensões e capacidade flutuante após reservar espaço do documento.
- Gate offline passou: 2.466 testes, sem falhas/erros/skips; assembleDebug e
  diff-check passaram. APK debug instalado no emulador com Success.
- Smoke abriu a prévia de cópia para XCanvas, exibiu compatíveis/omitidos e
  cancelou sem aplicar. Não valida a matriz completa de posições nem stylus.
  A navegação inicial precisou ajustar o script de scroll/seletor; não foi
  identificada falha do aplicativo nesses passos.
- Próximo corte: operações imutáveis e integração de atualização de perfis,
  com regressões de preservação do contexto inativo e compartilhamento.

## Extração da biblioteca e validação — 06/11/2026

- `ToolbarWorkspaceLibrary` concentra lista, prévia, estado transitório e launchers
  de importação/exportação. Persistência e aplicação continuam em callbacks do
  chamador; codecs, mensagens e formato JSON foram preservados.
- A biblioteca permanece composta com o configurador, preservando o ciclo de
  vida dos launchers. A aba selecionada persiste ao voltar da prévia.
- `testDebugUnitTest assembleDebug --offline` passou: 2.463 testes, sem falhas,
  erros ou skips. APK debug instalado no `emulator-5554` com Success.
- UIAutomator confirmou o inspetor de Elementos e o percurso Modelos → Original
  → prévia → voltar à mesma aba → fechar. O hash dos layouts/workspaces salvos
  permaneceu igual. Importação/exportação completa via seletor Android e stylus
  não foram exercitados nesta rodada.
- `PreferencesPane.kt`: 1.578 → 1.320 linhas nas duas extrações desta rodada.
  Nenhuma alteração no desenho das barras em runtime ou alegação de ganho de FPS.
- Próxima revisão: duplicações comprovadas nos planejadores de posicionamento e
  cópia, mantendo compatibilidade e regressões de geometria.

## Extração de Elementos — 06/11/2026

- `ToolbarElementsEditor` concentra tamanho individual, paleta/blocos, distribuição,
  Pen Group e a ligação com o editor de sections. Recebe o layout, um acesso ao
  layout atual, estado transitório e callbacks; não acessa `Editor` nem settings.
- Ghost e auto-rolagem continuam no painel externo. Ambos e os gestos usam os
  mesmos métodos de retarget em `ToolbarEditorInteractionState`.
- Regressão adicional verifica o destino antes/depois da atualização dos bounds
  por scroll e a remoção do destaque quando o layout fica sem medições válidas.
- `testDebugUnitTest assembleDebug --offline` passou. Esta rodada conserva a
  mesma ordem de controles e persistência imediata; smoke visual será registrado
  junto da extração da biblioteca.

## Refatoração incremental para revisão upstream — 06/11/2026

- Após `d52d4fc9`, o usuário confirmou que a principal do XCanvas move melhor e
  aceitou o espaço temporário deixado na origem durante o arraste, que some ao
  posicionar. Esse efeito não é bloqueador informado pelo usuário.
- Extraídos os controles de barra (`ToolbarBarSettings`), incluindo o seletor
  de âncoras, sem referência a `Editor`. Validação de encaixe, perfis e gravação
  continuam no chamador; o layout e os callbacks anteriores foram preservados.
- Extraído `ToolbarEditorInteractionState`: seleção e estados de arraste ficam
  em um objeto por configurador; as cinco tabelas de bounds continuam mapas comuns.
  Trocar barra ou perfil usa a mesma rotina de cancelamento/limpeza.
- Duas regressões verificam que medições do layout anterior não resolvem um drop
  após a troca e que limpar um configurador não altera o arraste de outro.
- Verificação: `testDebugUnitTest assembleDebug --offline` passou; 2.462 testes,
  sem falhas, erros ou skips. APK debug instalado com Success no `emulator-5554`.
  Smoke por UIAutomator confirmou abertura de Aparência, alternância xcanvas/xnote
  e acesso aos inspetores Elementos/Barras. Não é teste de stylus nem da matriz
  completa de gestos. Primeira tentativa de navegação parou por seletor fora da
  área visível; a repetição rolou até o controle e passou.
- `PreferencesPane.kt`: 1.817 → 1.578 linhas nesta rodada. O corte visual é
  registrado no commit `8d653861`; o estado transitório possui commit próprio.
- Guia de preparação/candidatos a PR em `docs/toolbar-pr-preparation.md`.
  Próximo corte: apresentação de Elementos e integração da biblioteca. Nenhuma
  alegação de ganho de FPS; performance ainda requer medição.

## Retomada de revisão e refatoração — 06/11/2026

- Base confirmada: `932756cc` restaura exatamente a árvore de `0e667a1d`. Worktree
  inicialmente limpa em `feat/customizable-toolbars`, sincronizada com origin.
- Diff local contra `upstream/master`: 58 arquivos, 8.306 inserções e 282 remoções.
  O baseline `testDebugUnitTest assembleDebug --offline` passou; a primeira execução
  reutilizou o resultado dos testes. Após alterações, a suíte foi executada novamente.
- Prioridades de revisão: separar UI do estado/persistência em `PreferencesPane.kt`,
  auditar geometria compartilhada e depois remover redundâncias comprovadas. Não há
  medição de cobertura percentual nem evidência nova de ganho de desempenho.
- Cobertura existente: domínio de layouts/workspaces, codecs/migração, troca de
  perfis/presets, capacidade de docks, medidas e distribuição de sections. Lacunas
  permanecem nos gestos reais, SAF, transações e QA com stylus; ver fila acima.
- Primeira extração: `ToolbarAppearanceSettings` recebe geometria e callbacks;
  preserva as validações e a atualização do tamanho global apenas na principal.
  `PreferenceChoiceChip` compartilha o desenho/semântica do chip existente.
  Não foram alterados renderer, sombras, persistência ou geometria nesta extração.
- Emulador Small_Tablet aberto com janela visível; debug `com.xnotes.toolbarsdebug`
  instalado e iniciado. A comparação visual controlada antes/depois ainda não foi
  concluída. Não considerar a antiga mancha preta resolvida.

### Correção intercalada — arraste da principal do XCanvas

- Configuração observada no emulador: `canvas-primary` com `movable=true` e
  `dock_edge=TOP` explícito. O movimento alterava âncora/preferência global, mas
  preservava a borda explícita anterior, priorizada por `toolbarDockPlan`.
- `ToolbarGeometry.movedTo` sincroniza a borda explícita nos destinos encaixados,
  preservando as dimensões e o comportamento legado sem borda explícita.
- O gesto delega a detecção de operação sem mudança ao controlador, pois a âncora
  salva pode divergir do encaixe efetivo da barra principal.
- Três regressões cobrem posição efetiva nas quatro bordas, drop flutuante e
  compatibilidade legada. Suíte completa: 2.460 testes, zero falhas/erros/skips;
  `assembleDebug` e `git diff --check` passaram. APK reinstalado com Success.
- Validação visual do arraste e da mancha no dispositivo continua pendente.
  Próxima ação: reproduzir o gesto no XCanvas e confirmar a borda efetiva antes
  de extrair o estado transitório do configurador.

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
| M8 Atalho in-note | PARCIAL | Atalho adicionado aos menus de nota e canvas, abrindo o editor dedicado; QA com caneta no Redmi pendente |
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
- [x] [M8] Atalho nos menus de nota/canvas para abrir a configuração dedicada, sem resize in-note (04/11). QA físico pendente.
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

## Checkpoint M4.2 — navegação de propriedades acima da prévia (03/11/2026)

- As categorias `Barras / Dimensões / Elementos / Cores` passaram para uma faixa horizontal acima do preview, sem consumir o espaço da coluna de propriedades.
- O cabeçalho concentra perfil xnote/xcanvas, escolha da barra em menu suspenso e ação de restauração. O painel de propriedades continua ao lado da prévia em janelas largas e abaixo em janelas estreitas.
- Selecionar uma ferramenta na prévia abre a categoria Elementos e seus controles individuais.
- `git diff --check` e `:app:testDebugUnitTest :app:assembleDebug --offline` passaram no CachyOS (`BUILD SUCCESSFUL`, 50 tarefas). QA físico no Redmi Pad 2 ainda pendente.
- Próxima fase: faixas de encaixe por borda e ocupação sem colisões, com testes de capacidade e comparação preview/nota/canvas.

## Checkpoint M5 — distribuição por bordas (04/11/2026)

Implementado nesta branch: até três faixas por borda, encaixadas com reserva real de espaço e flutuantes sobre canvas; barras de uma mesma faixa respeitam comprimento, ordem e alinhamento; fallback automático para outra faixa da mesma borda quando a solicitada não comporta a barra. Preferências e preview reutilizam a mesma base de cálculo; JSON v5 migra layouts anteriores.

A navegação Barras/Dimensões/Elementos/Cores foi movida para cima do preview (90b625e8). O Pen Group agora participa da animação de destaque circular da toolbar, sem fundo quadrado próprio.

Validação local: testes JVM do resolvedor e do codec, assembleDebug, edição/configuração no emulador. Somente o emulador apareceu no ADB, não o Redmi Pad 2; teste manual no dispositivo real, escrita, canvas, split view e rotação continuam pendentes. Com viewport excepcionalmente pequena e faixas lotadas, uma barra pode ser rejeitada e não renderizada; definir UX de recuperação antes do DONE.

Próxima ação: instalar build atualizada no tablet quando reconectar e percorrer cenários de capacidade/gestos; revisar medição intrínseca de grupos de tamanho variável e tornar rejeição explícita ao usuário se a viewport encolher.

## Ajuste visual do Pen Group e seleção proporcional (04/11/2026)

O destaque deslizando agora recebe a posição e o raio reais de cada elemento, incluindo sizeDp individual, e anima alterações de diâmetro e deslocamento transversal entre sections. O Pen Group usa esse mesmo sistema; não mantém fundo quadrado independente.

A tinta selecionada colore somente os acentos dentro dos pictogramas das canetas e as amostras reais da paleta. Bordas de seleção do radial e do painel usam a cor de destaque do tema; a linha do controle de espessura é neutra. Teste JVM incluído para as dimensões proporcionais de destaque; build debug e testes verdes. QA manual de transição entre elementos de 28dp/70dp no Redmi Pad 2 pendente.

## Checkpoint — distribuição e editor de camadas (04/11/2026)

- `ToolbarLayout.distribution` agora alterna entre **CONTINUOUS** (uma lista de sections) e **REGIONS** (Start/Center/End); cada section tem região própria e conserva suas 1–3 linhas. Persistência JSON v6 lê layouts anteriores como CONTINUOUS/START.
- Editor Elementos em tela dedicada: controles acima, preview centralizado imediatamente antes das colunas. Cada seção tem alça de arraste, menu de 1/2/3 linhas e remoção; ferramentas aparecem como linhas/camadas com visibilidade, remoção, alça e botão de adicionar. Colunas vazias oferecem `Add section` e sections têm catálogo com busca (`id` e nomes, inclusive Colors/Colours).
- Sections inteiras podem ser arrastadas entre colunas ou reordenadas, incluindo coluna vazia. Itens podem ser movidos entre sections e linhas. Ferramentas comuns não são duplicadas; Colors permite múltiplas ocorrências com recalculo proporcional dos blocos.
- A distribuição é reutilizada pelo renderer das notas, canvas e preview. Quando a largura/altura é pequena, o conteúdo rola sem sobrepor as regiões; cálculo de posições testado em JVM.
- Menus existentes de notas e canvas receberam atalho **Configure toolbars**, que abre o editor dedicado (sem edição in-note).
- `git diff --check` + `:app:testDebugUnitTest :app:assembleDebug --offline` concluíram com BUILD SUCCESSFUL. APK `com.xnotes.toolbarsdebug` instalado em `emulator-5554` com Success. QA visual da tela de colunas e preview executado no Small_Tablet: criar uma section de duas linhas no Center, arrastar uma section Start→Center e mover Undo entre sections funcionaram.
- **Pendente**: Redmi Pad 2 ausente do ADB; validar com Xiaomi Smart Pen, gestos nas laterais, xcanvas real, rotação, persistência após reiniciar, overflow em largura reduzida e retorno do atalho in-note. Sem PR ou push nesta sessão até aqui.

## Correções — regiões e hierarquia do editor (04/11/2026)

- Corrigida a faixa de regiões no renderer compartilhado: Start, Center e End têm visores próprios, alinhados fisicamente nas quatro orientações. Overflow de uma região rola localmente, sem deslocar as outras. O cálculo legado de largura expandida foi removido.
- **Comportamento revogado no ajuste posterior abaixo:** a conversão automática dividia as sections de barras antigas em três regiões e alterava o layout padrão, motivo da regressão relatada pelo usuário.
- As abas `Barras / Dimensões / Elementos / Cores` **continuam acima do preview**, conforme correção do usuário. No editor de elementos, controles de tamanho, modo de distribuição e Pen Group ficam abaixo da prévia, junto das colunas de sections; textos redundantes foram removidos.
- `git diff --check`, `:app:testDebugUnitTest :app:assembleDebug --offline` passaram (`BUILD SUCCESSFUL`); debug instalado em `emulator-5554`. QA funcional completo das faixas na barra real, retrato/paisagem e Redmi Pad 2 continua pendente.

## Correção de regressão — alternância de distribuição (04/11/2026)

- Revisão do usuário identificou que a distribuição automática anterior alterava a main toolbar. **Ela foi removida**: `setDistribution` agora troca somente o modo, sem ordenar, mover ou alterar sections/itens/linhas/tamanhos. Início/Centro/Fim só mudam ao arrastar uma section explicitamente.
- O layout padrão de `ToolbarLayout.DEFAULT` se mantém idêntico ao alternar Contínuo → Regiões → Contínuo. Testes adicionados para preservação da ordem mesmo com regiões intercaladas.
- Para configurações **já salvas** pela versão problemática, a tela Elementos mostra a ação contextual `Reunir sections no início` quando há itens em Centro/Fim. Ela reagrupa as sections sem apagar ferramentas, tamanho individual ou linhas; só deve ser usada se o usuário desejar limpar os alinhamentos anteriores.
- `git diff --check` + `:app:testDebugUnitTest :app:assembleDebug --offline` passaram, `BUILD SUCCESSFUL`; APK reinstalado no `emulator-5554` com `Success`. Falta QA manual no tablet, incluindo barras vertical e principal com regiões definidas pelo usuário.

### Próxima ação

1. Conectar Redmi Pad 2 via ADB e instalar apenas `com.xnotes.toolbarsdebug`.
2. Validar os arrastes de ferramenta/section e alinhamentos Start/Center/End na barra real (topo, base e laterais), depois em xcanvas.
3. Testar persistência/recuperação de barras e sections após reiniciar, além de janelas pequenas/split; registrar divergências entre preview e runtime.

## Ajuste — arraste entre regiões e centro físico (04/11/2026)

- Referência de UX: Noctalia (configuração de widgets em Start/Center/End, suporte ao arraste entre faixas e centro ancorado ao meio físico da barra), documentação: https://docs.noctalia.dev/noctalia/v5.2.1/bar/ . Código de distribuição: https://github.com/noctalia-dev/noctalia/blob/main/src/shell/bar/bar.cpp .
- O alvo do arraste de uma **ferramenta individual** agora é primeiro determinado pela coluna sob o dedo. Entre as sections dessa região escolhe a posição mais próxima; em região vazia, soltar cria automaticamente uma section de uma linha. A ferramenta mantém suas propriedades e o rateio de amostras `Colors` é recalculado.
- Na barra real e na prévia, as regiões ocupam faixas que não se sobrepõem: início e fim dividem igualmente o espaço restante em torno do conteúdo central, que tem largura/altura natural limitada a metade da barra e fica ancorado ao centro físico. Com conteúdo excedente, as regiões rolam de modo independente; laterais usam a mesma regra no eixo vertical.
- Não há reorganização automática quando se alterna **Contínuo** ↔ **Início/Centro/Fim**; a ordem original da main toolbar é preservada. As abas `Barras / Dimensões / Elementos / Cores` permanecem acima da prévia.
- Casos JVM novos: arraste para Centro vazio, seleção somente de sections dentro da região, nova section com preservação de ferramentas e redivisão de `Colors` duplicadas. `git diff --check` e `:app:testDebugUnitTest :app:assembleDebug --offline` passaram (`BUILD SUCCESSFUL`). APK debug instalado em `emulator-5554` (`Success`).
- **Pendente**: conferir manualmente no editor e em nota/canvas a posição central após arraste, em barra horizontal e lateral, com janela compacta, retrato/paisagem e depois no Redmi Pad 2 com Xiaomi Smart Pen. Sem push nesta etapa.

## Fix — regiões eram descartadas ao entrar na nota/canvas (04/11/2026)

- Causa confirmada por instrumentação temporária: `ToolbarWorkspaces.forContext()` filtra itens pelo contexto com `onlyItems()`, mas este recriava cada `ToolbarLayout` sem copiar `distribution`. A configuração persistida tinha REGIONS e sections em START/END/CENTER; o runtime recebia CONTINUOUS e enfileirava tudo por ordem de origem.
- O filtro agora usa `bar.layout.copy(sections = ...)`, preservando modo de distribuição, regiões, tamanho individual e outras propriedades. Cobertura JVM nova para perfil compartilhado em nota e canvas.
- A prévia e as barras customizadas flutuantes medem as regiões dentro do comprimento alocado; o preenchimento do conteúdo respeita a largura/altura disponível. O planejamento automático de barras flutuantes com REGIONS considera a maior ocupação entre Início e Fim dos dois lados do Centro, em vez de só somar tudo e acabar ocultando cores.
- Reproduzido no emulador em nota real: antes, Sidebar aparecia no fim; depois, a medição do Centro foi `Rect(924..996)` em barra `Rect(540..1380)`, cujo centro físico é x=960 em ambos. Na build final, a barra flutuante estendeu-se e as cores voltaram a ficar visíveis em Start, Sidebar ao centro e Pen Group ao final.
- Testes `:app:testDebugUnitTest :app:assembleDebug --offline`, `BUILD SUCCESSFUL`; APK instalado no `emulator-5554`. Removidas as mensagens temporárias de medição. Ainda é necessário QA físico no Redmi Pad 2 (laterais, xcanvas, gestos, rotação, split e diferentes tamanhos), sem push nesta etapa.

## Espaçamento manual entre elementos (04/11/2026)

- Incluído `ToolbarItem.SPACER`: elemento invisível e repetível, com tamanho principal independente de **4 a 320 dp** (24 dp por padrão); horizontal em barras superior/inferior e vertical nas laterais. Não deve aumentar a espessura da barra.
- Adicionar via catálogo das sections, mover como uma camada comum, selecionar o espaço na prévia e modificar `Espaço entre elementos`. Suporte em barras de notas, canvas, prévia, medição de barras flutuantes e persistência JSON.
- O modelo mantém `Spacer` opcional: barras antigas e o layout padrão da main toolbar não recebem nenhum espaço extra automaticamente. Ocorrências repetidas preservam ordem e medidas ao salvar/reabrir.
- Distinção de UX: **REGIONS** mantém o Centro ancorado ao meio físico, criando espaços quando as regiões têm tamanhos desiguais; **CONTINUOUS + Spacer** permite especificar manualmente as distâncias desejadas. Não alterar a geometria automática de REGIONS ao inserir spacers.
- `git diff --check`, `:app:testDebugUnitTest` (2445+ testes) e `:app:assembleDebug` passaram; APK instalado e inicializado em `emulator-5554`. Tablet real não conectado; QA manual dos espaçamentos pendente.

## Fix — lacuna artificial entre Center e End (04/11/2026)

- Causa: `estimatedFloatingLength` reservava `2 × max(Start,End) + Center` para alinhar o Centro ao meio físico, mesmo quando o comprimento da barra era automático. Com Start mais longo que End, a área simétrica criava um vão enorme no final.
- **REGIONS + flutuante + dimensão automática** agora usa comprimento natural das sections, com grupos visualmente ordenados Start → Center → End, sem alterar a ordem salva nem suas linhas internas. O conteúdo pode rolar quando excede o espaço alocado.
- **REGIONS + barra docked ou com dimensão principal explícita** mantém alinhamento físico Start/Center/End para respeitar a área que o usuário escolheu. `CONTINUOUS` não mudou.
- Renderer compartilhado de notas/canvas e preview usam a mesma seleção de modo compacto; nenhum elemento é removido, nem redistribuído de uma coluna para outra. Cobertura JVM verifica o comprimento natural horizontal/vertical, as condições do modo compacto e a ordem visual versus persistida.
- Gradle `:app:testDebugUnitTest :app:assembleDebug --offline` passou (`BUILD SUCCESSFUL`), APK reinstalado no `emulator-5554` e Activity iniciada. Validação visual manual com grupos assimétricos no Redmi Pad 2 permanece pendente; sem push.

## Checkpoint — workspaces portáteis e revisão de pendências (04/11/2026)

- Cópia de toolbar entre **XNotes e XCanvas** com prévia do destino, relatório de elementos incompatíveis, filtragem por contexto e preservação de sections, distribuição, dimensões e parâmetros suportados. A origem não é alterada. Planejadores verificam posições viáveis e oferecem fallback de âncora.
- Workspaces nomeados salvam e restauram **ambos os perfis** (nota/canvas), aparência comum e paleta. A biblioteca diferencia `Meus salvos` e `Modelos`, pede confirmação para exclusão, permite prévia antes de aplicar e oferece importação/exportação de JSON versionado pelo seletor de arquivos do Android. Importação não substitui automaticamente a configuração ativa e evita sobrescrita de nomes/IDs; limite validado de 20 presets salvos.
- Modelos incluídos, sem referência a aplicativos de terceiros: `Original`, `Escrita sem distrações`, `Caderno de estudos`, `Revisão de documentos`, `Desenho livre`, `Marcação rápida`, `Barra completa` e `Duas linhas`. Modelos cobrem nota e canvas com elementos compatíveis; testes verificam encaixe e capacidade em viewport de tablet.
- UX do configurador: `Workspaces ▾` fica junto à seleção `Main toolbar ▾` no cabeçalho; **Cores** deixa de ser aba independente e passa a ser `Paleta de cores` dentro de **Elementos**. As abas permanecem acima da prévia: Barras / Dimensões / Elementos.
- Evidências: `git diff --check` limpo, `:app:testDebugUnitTest :app:assembleDebug --offline` com `BUILD SUCCESSFUL`, debug instalado/iniciado em `emulator-5554` e navegação de cabeçalho/paleta/biblioteca observada no UIAutomator. Tests JVM adicionados para transferências, JSON, importação e presets. **Não houve validação completa dos fluxos de SAF (importar/exportar) nem QA físico com Xiaomi Smart Pen; não declarar DONE.**
- A tentativa de grupos genéricos/pastas além do Pen Group **foi rejeitada e revertida a pedido do usuário**. Não reabrir implementação de grupos compactos nesta fase; sections já são unidades organizacionais. Também fica fora do escopo a antiga personalização de blur e bordas individuais, conforme decisão de simplificação.

### Auditoria priorizada após esta entrega

1. **P0 — QA no Redmi Pad 2:** gestos de arrastar a barra versus toque/long-press nas ferramentas, primeiro traço, troca de notas/canvas, 4 docks, posições flutuantes, 1/2/5 barras, sections REGIONS e CONTINUOUS, split view e rotação. Conferir prévia vs toolbar real, tamanho individual, hitboxes e rejeição/overflow em telas pequenas. Registrar reprodução e corrigir regressões antes de expandir recursos.
2. **P0 — Persistência/importação:** teste Android end-to-end de salvar/aplicar preset, duplicar, copiar XNotes↔XCanvas, importar/exportar JSON por SAF, fechar/reabrir o app, migração de dados anteriores, conteúdo inválido, trocas de contexto e modelo original de recuperação.
3. **P1 — Edição segura:** modo rascunho com Aplicar/Cancelar, Desfazer/Refazer e recuperação explícita para barra inviável após redimensionar viewport. No estado atual, grande parte das mudanças é persistida imediatamente.
4. **P1 — UX do editor:** distinguir perfis compartilhados/independentes no controle, concluir ajuste de espaçamento e alvos de toque por ocorrência, checar arraste com scroll/colunas estreitas, estados vazios e feedback de destino sem sobreposição.
5. **P2 — Polimento/documentação:** acessibilidade e contraste claro/escuro, textos longos, toque mínimo, teclado, desempenho sob várias barras; consolidar roadmap, atualizar playbook ADB, preparar commits/PRs somente depois da matriz de QA.

**Próxima ação recomendada:** conectar Redmi Pad 2 e executar P0 nas barras reais, com prioridade para conflito de gestos e divergência de geometria; depois testar todos os fluxos de portabilidade/persistência com seletor de arquivos do Android.

## Build release isolada no Redmi Pad 2 (04/11/2026)

- Gradle `:app:assembleRelease -PtoolbarIsolatedRelease=true --offline --console=plain` passou com `BUILD SUCCESSFUL` (R8 `isMinifyEnabled` e resource shrinking habilitados).
- A propriedade opt-in `toolbarIsolatedRelease` aplica `applicationIdSuffix = .toolbarsrelease`, nome `0.9.6-toolbars-release` e assina com a chave local de **teste**, sem mudar a assinatura/ID da release normal `com.xnotes`. Nenhuma chave foi incluída no Git.
- APK `app/build/outputs/apk/release/app-release.apk` de 35 MiB: `aapt` confirmou `com.xnotes.toolbarsrelease`, `apksigner verify` validou a assinatura v2. ADB no Redmi Pad 2 `pfcmu4dutsylceus`: `install -r` → `Success`; `monkey -p com.xnotes.toolbarsrelease 1` executou e `pidof` retornou processo ativo; sem crash fatal nas últimas 200 linhas de logcat.
- XNotes normal `com.xnotes`, `com.xnotes.community` e build debug `com.xnotes.toolbarsdebug` não foram substituídos. QA de gestos, stylus, ações, import/export e produção de documentos ainda é manual e **não** está concluído.

## Correção — alinhamento de cores e aplicação de workspaces por perfil (04/11/2026)

- `ToolbarSectionGrid` centraliza cada bloco `COLORS` em um espaço transversal do tamanho de botão, tanto horizontal quanto vertical; suas linhas/colunas e os grupos de regiões Start/Center/End agora centralizam conteúdo no eixo cruzado. O mesmo componente atende notas, canvas e preview.
- O diálogo de prévia de workspace aplica **somente no contexto selecionado** (XNotes ou XCanvas) por padrão. A opção explícita `Aplicar também ao outro editor` restaura ambos. Ao aplicar um perfil, o perfil inativo é preservado, `shared` é desativado e apenas elementos compatíveis ficam no destino. Quando aplicado a um só, aparência/paleta globais permanecem como estavam; aplicar ambos restaura também essas preferências.
- Testes JVM adicionados para impedir que restaurar um preset de nota altere canvas, inclusive se o perfil era compartilhado, e para aplicar no canvas um preset compartilhado filtrando ferramentas exclusivas de notas.
- `git diff --check`, `:app:testDebugUnitTest :app:assembleDebug --offline` e `:app:assembleRelease -PtoolbarIsolatedRelease=true --offline` passaram (`BUILD SUCCESSFUL`). Release `com.xnotes.toolbarsrelease` atualizada no Redmi Pad 2 serial `pfcmu4dutsylceus` com `adb install -r` → `Success`; iniciada e com PID ativo. Nenhuma mudança de dados ou pacote do XNotes normal.
- **QA visual do alinhamento vertical com configurações reais do usuário ainda depende de confirmação no tablet**, em barras de uma/três faixas, regiões e lateral. Validar também reabertura do app com perfis independentes e opção de aplicação dupla.

## Refinamento visual — primeira etapa (04/11/2026)

- Pen Group: painel reorganizado em seleção, traço, cores e controles avançados; tipografia do `MaterialTheme`, cores semânticas de `LocalPalette`, botões de caneta e amostras maiores, controles de toque com dimensões regulares, divisores e espaçamentos consistentes com XNotes.
- Editor de sections e elementos: cabeçalhos, chips e linhas usam a tipografia do app e áreas de toque mais confortáveis, sem alterar a semântica de arrastar, categorias ou alinhamentos.
- Mantidos os gestos e o seletor radial já existentes; nenhuma tentativa de reintroduzir grupos genéricos.
- Verificação inicial: `git diff --check`, testes JVM e montagem debug passaram. Interface de nota aberta e inspecionada em `emulator-5554`; comportamento com caneta real e inspeção do popup em todas as densidades ainda requerem QA.

## Usabilidade — preview e gesto de reorganização (04/11/2026)

- `Live preview` passa a ter um único cabeçalho estável acima da área de prévia nas três abas. Não existem mais dois caminhos de renderização do label (um dentro do inspector e outro fora dele).
- Ghost de arraste de elemento agora usa um deslocamento constante próximo ao dedo, calculado em pixels conforme a densidade, independentemente da largura da linha de origem. O cálculo do alvo continua usando a coordenada real do dedo; o arraste de sections mantém a posição relativa original.
- Reorganização isolada em callbacks e componente do ghost para permitir ajustes futuros sem alterar o modelo persistido ou o layout da barra.
- `:app:testDebugUnitTest :app:assembleDebug --offline --quiet` e `git diff --check` passaram. Inspeção física com Smart Pen ainda necessária para avaliar a percepção do ghost e seus destinos.

## Arraste de container completo (04/11/2026)

- O ghost de elementos usa o mesmo componente `LayerFace` do editor, com largura medida da linha e deslocamento original do ponto de toque. Ícone, nome, grip e visibilidade continuam representados ao arrastar.
- A cópia mantém apenas o destaque visual da seleção; remoção e alteração de visibilidade continuam disponíveis exclusivamente no editor normal.
- A lógica de destino e de sections não foi alterada; o renderer compartilhado reduz a manutenção quando surgirem novos elementos ou novos controles de linha.
- `:app:testDebugUnitTest :app:assembleDebug --offline` e `git diff --check` passaram; QA físico do gesto com Smart Pen ainda pendente.

## Navegação de prévia de workspace (05/11/2026)

- Fechar/cancelar a prévia de um workspace agora retorna à biblioteca, sem fechar o painel de workspaces. O botão foi renomeado para `Voltar à lista`.
- A aba da biblioteca (`Meus salvos` ou `Modelos`) é estado do configurador, preservada durante a transição lista → prévia → lista. O botão Voltar do Android usa o mesmo callback.
- `Aplicar` ainda confirma a substituição e fecha o fluxo. `Fechar` na própria biblioteca continua fechando o painel.
- Testes JVM e compilação debug passaram (`:app:testDebugUnitTest :app:assembleDebug --offline`).

## Simplificação da biblioteca de workspaces (05/11/2026)

- Biblioteca em duas abas preservadas: `Meus salvos` e `Modelos`. Cartões agora abrem diretamente a prévia ao toque, dispensando botões repetidos de visualizar.
- `Salvar atual` permanece visível. Importar, exportar atual e exportar todos ficam no menu superior de opções; exportar ou excluir um preset salvo ficam no menu de opções do cartão. Exclusão exige confirmação.
- Modelos ficam em cartões compactos com nome e breve descrição. Usa `MaterialTheme.shapes.medium`, tipografia e componentes Material já empregados no app.
- Funcionalidades de salvamento, arquivos e aplicação foram mantidas; alterou-se apenas sua apresentação. `:app:testDebugUnitTest :app:assembleDebug --offline` e `git diff --check` passaram.

## Editor visual contextual — primeira implementação (05/11/2026)

- Preview fica acima das abas de propriedades, com posição única ao alternar Barras/Aparência/Elementos. `Dimensões` passou a `Aparência`; escala da barra foi movida de `Barras` para `Aparência`, junto dos ajustes de geometria.
- Tocar no container de elemento seleciona o item no inspetor, sem alternar visibilidade. O contorno de destaque indica a seleção; tamanho individual continua contextual no inspetor.
- Visibilidade foi separada em um botão de olho na linha. Itens ocultos continuam na lista, com estilo atenuado e rótulo `Oculto`. A remoção continua em ação separada e o arraste completo permanece.
- O estado é transitório no editor e os ajustes de aparência/elementos continuam persistidos pelo modelo existente. `:app:testDebugUnitTest :app:assembleDebug --offline` e `git diff --check` passaram.
- Pendências de UX de uma próxima etapa: inspeção física de seleção vs arraste/caneta, redução de texto explicativo redundante, seções avançadas recolhíveis e eventual preview fixo durante rolagem longa.

## Ajustes de usabilidade — preview e organização (05/11/2026)

- Preview do editor agora tem largura compacta por padrão (`390dp`) e botão `Ampliar/Reduzir` para inspeção eventual (`680dp`). Assim seu tamanho não é definido pela largura toda das preferências.
- `Elementos` possui um viewport de até `380dp`, com scroll próprio, independente do scroll de configurações; a auto-rolagem do drag próximo das bordas atua nesse viewport e mantém os destinos de inserção e regiões alcançáveis sem perder o preview.
- Ações de criar/duplicar/remover/copiar toolbar saíram do conteúdo da aba `Barras` e migraram para menu de opções próximo ao seletor da toolbar, disponível em qualquer aba; lógica original de validação de encaixe foi preservada.
- `Barras` concentra as propriedades de posicionamento e visibilidade; seleção por container e indicação de itens ocultos permanecem em `Elementos`; `Aparência` contém as medidas/escala. Conferir com Xiaomi Smart Pen a sensação de rolagem aninhada, arraste entre regiões e expansão do preview.

## Densidade visual dos elementos (05/11/2026)

- Containers de elementos mais compactos: padding vertical 10→4dp, padding horizontal 10→8dp, ícones 16/18→14/16dp, espaçamento horizontal 7→6dp, controles laterais de olho/remover em caixas de 30dp sem mínimo implícito de IconButton.
- Header de section 40→34dp, espaço entre sections 8→6dp e entre linhas/elementos 5/4→3dp. Preservados títulos, contorno da seleção, indicação `Oculto` e gesto de arrastar container inteiro.
- O ghost reutiliza `LayerFace` e herda automaticamente a mesma densidade; hitboxes das sections e elementos continuam medidas em tempo real.
- Testes JVM e build debug passaram (`:app:testDebugUnitTest :app:assembleDebug --offline`). QA com Smart Pen ainda necessário para validar conforto dos controles compactos.

## Compactação das propriedades de barra (05/11/2026)

- `Barras`: nome e opção `Movível` compartilham a linha quando houver largura; faixa de encaixe e alinhamento passam a formar dois blocos lado a lado, ajustando-se automaticamente em telas estreitas.
- `Aparência`: controles de medidas em cartões de largura reduzida, com menor distância vertical/horizontal; removidos título e descrição duplicados.
- Redução do espaçamento do inspetor e dos textos explicativos que não acrescentavam uma ação; sem alterar a lógica de encaixe, seleção ou persistência.
- Direção de UX inspirada na densidade e no acesso contextual de editores de notas como Samsung Notes e Notein, preservando componentes do XNotes.
