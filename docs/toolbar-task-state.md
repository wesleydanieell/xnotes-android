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

## Próxima ação (uma por vez)

Reproduzir e corrigir o fluxo segurar/arrastar o contêiner da barra principal
e secundárias (touch/stylus), antes de expandir renderização de grupos.
Inspecionar a tela de preview no emulador, testar a grade 3×3 e comparar
com a barra real. Usar o playbook ADB e documentar evidências.
