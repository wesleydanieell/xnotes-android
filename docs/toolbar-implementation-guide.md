# Plano mestre — barras personalizáveis do XNotes

> Guia de execução para humanos e agentes Codex. Versão 1.0 — 28/10/2026.
> Estado granular: docs/toolbar-task-state.md. Instruções ao agente: AGENTS.md.
> Código: branch feat/customizable-toolbars, fork wesleydanieell/xnotes-android.
> Fonte de verdade upstream: shardulvs/xnotes-android, branch master.

## 1. Objetivo e limites

Criar um sistema de múltiplas barras independentes, totalmente configuráveis, disponível
no editor de cadernos (xnote) e no canvas infinito (xcanvas). A personalização deve competir
em ergonomia com Samsung Notes, Freenotes, StarNote e Goodnotes, especialmente no tablet
com caneta: acesso rápido, baixa latência, pouco espaço ocupado, grande legibilidade,
gestos previsíveis e configuração reversível. "Melhor" é uma meta mensurável, não uma
alegação de superioridade a priori.

O projeto não deve reescrever o motor de desenho, modificar os formatos de documentos,
incluir mudanças de abas/split-view de outros PRs, desabilitar segurança, tocar nas notas
existentes do usuário ou exigir configuração especial de uma Wacom no computador.

## 2. Pesquisa e princípios competitivos

Referências consultadas em 27–28/10/2026 (funcionalidades podem evoluir):

| Referência | Evidência publicada | Direção para o XNotes |
| --- | --- | --- |
| Samsung Notes | Comunidade Samsung descreve pressionar e arrastar a barra entre posições, além de personalização; anúncio oficial de posicionamento à esquerda/direita no PC. | Arrastar intuitivo do contêiner, sem alça obrigatória; preservar encaixe fixo. |
| Goodnotes | Suporte oficial descreve seleção/ocultação/ordem de ferramentas e posição de desfazer/refazer; blog confirma personalização multiplataforma. | Preferências rápidas, ferramentas comuns ao alcance e barra legível. |
| Freenotes | Descrição oficial na App Store destaca canetas personalizadas e fixação de barras/atalhos usados com frequência. | Favoritos de canetas e cores num espaço reduzido. |
| StarNote | Listagens oficiais descrevem diversos tipos de caneta, configurações de cor/espessura e barra personalizável. | Grupos de ferramentas e comportamento familiar ao estudante. |

Fontes para revisão futura:
- Samsung: https://eu.community.samsung.com/t5/mobile-apps-services/how-do-i-rearrange-the-toolbar-in-samsung-notes/td-p/7453762
- Samsung PC: https://eu.community.samsung.com/t5/tips-how-to/notes-pc-announcement-02-04-move-the-toolbar-left-or-right/ba-p/12659807
- Goodnotes: https://support.goodnotes.com/hc/en-us/articles/8900755183631-Customize-the-toolbar
- Goodnotes novidades: https://www.goodnotes.com/blog/features-fixes-updates-december-2025
- Freenotes: https://apps.apple.com/us/app/freenotes-ai-notes-taking/id6464237904
- StarNote: https://starnote.ai/ e https://play.google.com/store/apps/details?id=com.onyx.galaxy.global.note

A grade de ferramentas arbitrária 3×2, a barra múltipla por contexto e a aparência
profundamente configurável são propostas NOSSA equipe — não atribuir esses recursos
aos concorrentes sem evidência. Inspiração visual não é cópia de código, ícones ou marca.

## 3. Contrato funcional completo

Cada item abaixo é parte do escopo; não descartar silenciosamente:

- [ ] Criar várias barras (nenhum máximo artificial baixo); duplicar, renomear, remover
      as secundárias, ocultar/exibir, reordenar, recuperar/resetar por padrão.
- [ ] Barra principal também configurável e móvel; sempre haver uma rota para
      recuperar comandos essenciais. Nenhum estado deve prender o usuário sem acesso.
- [ ] Tamanho: largura/altura automática pelo conteúdo OU manual em dp, mínimo/máximo
      documentados; presets Compact, Regular, Comfortable; orientação horizontal/vertical.
- [ ] Fixa/encaixada versus flutuante. Modo fixo: topo, baixo, esquerda, direita.
      Modo flutuante: NOVE âncoras — quatro cantos, quatro centros das bordas e
      centro absoluto. São TREZE combinações de modo+posição.
- [ ] Aparência individual: fundo sólido/transparência; blur opcional com fallback
      seguro; borda habilitada/desabilitada, espessura e cor; cantos arredondados;
      padding horizontal/vertical, espaçamento e alinhamento interno.
- [ ] Comportamento móvel/travado por barra. Travada conserva âncora e tamanho e
      não abre modo de arraste acidental; configuração ainda pode alterar posição.
- [ ] Pressionar e segurar no CONTÊINER para iniciar movimento, sem puxador visível
      permanente; gesto não deve impedir taps comuns e opções das ferramentas.
- [ ] Menu contextual da própria barra (Configurar, Mover/Travar, Redimensionar,
      Duplicar, Ocultar e Restaurar). Só ações relevantes são mostradas.
- [ ] Editor dedicado: lista de barras, seleção clara, painel de propriedades
      progressivo, ferramenta de composição, prévia visual responsiva de uma nota,
      seleção e arraste dentro da prévia, desfazer/refazer e cancelamento/aplicação.
- [ ] Elementos individualizados: tamanho visual, área de toque, espaçamento,
      rótulo/ícone opcional, prioridade, estado ativo/selecionado e visibilidade.
- [ ] Quantidade de cores por barra e disposição personalizável de swatches,
      incluindo linhas/colunas como 3×2; acesso a paletas existentes sem duplicar
      estado global de tinta.
- [ ] Grupos de elementos: grupos recolhíveis/expansíveis e "Caneta unificada"
      como lançador de caneta, dashed, caligrafia, marcadores etc., com último
      tipo ativo preservado.
- [ ] Layout empilhado: ferramenta A sobre B, grade de ferramentas 2×2, 3×2,
      grupos mistos de ícones de tamanhos diferentes quando couberem, alinhamento
      e validação de capacidade. Sem sobreposição ou controles ilegíveis.
- [ ] Reordenação drag-and-drop entre seções e barras, com indicação visual de
      destino e sem duplicatas acidentais dentro da mesma barra.
- [ ] Perfis xnote/xcanvas compartilhados OU independentes; ferramentas indisponíveis
      são filtradas, sem eliminar as configurações guardadas no perfil inativo.
- [ ] Redimensionamento persistente dentro da nota/canvas apenas em modo explícito
      de edição, com handles acessíveis, limites e Reverter. Por padrão a configuração
      pertence ao perfil da barra, NÃO ao arquivo .xnote/.xcanvas. Overrides por nota
      exigem decisão separada de produto e migração futura.

## 4. UX e decisões estabelecidas

### 4.1 Gestos e conflitos

Fluxo desejado: toque curto continua executando a ferramenta; pressionar e manter
sem mover abre o menu contextual ao soltar; pressionar e ultrapassar touch slop
entra em Dragging com prévia; soltar confirma âncora e persiste; cancelar restaura
a origem. A prioridade de gesto da caneta deve respeitar os comandos existentes
de pressionar longamente sobre ferramentas. É necessário resolver explicitamente
a distinção "contêiner/fundo" x "ícone com ação própria"; o protótipo atual
ainda exige testes reais desse conflito. Não bloquear ACTION_DOWN no canvas.
Durante desenho, navegação, pan e split-view, arraste em barra não deve causar
o bug de perder o primeiro traço no painel recém-focado.

Uma barra travada não deve aceitar drag de posição, nem editar tamanho por
gesto. O menu continua acessível por opção explícita. Dispositivo mouse,
dedo e stylus precisam funcionar com clique, toque e long-press adequados.

### 4.2 Posicionamento e colisões

Guardar âncora relativa à região editável de cada pane, respeitando insets,
barra de sistema, teclado e orientação. Se duas barras partilharem âncora,
empilhar com espaçamento e ordem estável; se não couber, oferecer overflow
ou deslocamento controlado, nunca esconder ações fora de tela. A barra fixa
altera o espaço reservado ao documento; a flutuante o sobrepõe e informa
insets/oclusão aos overlays dependentes. Em split-view, cada pane recebe sua
própria composição e suas áreas seguras.

### 4.3 Preview

O mockup deve renderizar a disposição real das barras sem carregar um documento.
Mostrar itens visíveis, ordem, cores, escala, orientação, estado selecionado,
borda, raio e fundo; ao mudar uma propriedade, atualizar imediatamente. Arrastar
na prévia altera a mesma geometria persistida; tocar escolhe barra. Para layouts
muito grandes, mostrar overflow explicitamente. Especificar se mudanças são
imediatas ou editadas em rascunho antes de "Aplicar"; alvo final é rascunho
com Desfazer/Cancelar para evitar gravação indesejada. Prévia nunca escreve tinta.

### 4.4 Editor de configuração

Hierarquia sugerida:
1) perfis xnote/xcanvas e modo compartilhar;
2) prévia de documento no topo;
3) lista de barras com criar/duplicar/excluir;
4) Propriedades > Layout, Aparência, Comportamento, Elementos;
5) catálogo de ferramentas e árvore de grupos;
6) prévia e controles de aplicação/reverter.
Evitar dezenas de chips soltos e formulários gigantes. Para posição,
usar grade 3×3 contextual; modo fixo habilita apenas quatro lados.

## 5. Arquitetura, persistência e compatibilidade

Arquivos atuais: core/tools/ToolbarWorkspace.kt; ToolbarLayout.kt;
settings/Settings.kt e serialização de workspaces; ui/Editor.kt;
ToolbarFrame.kt, WorkspaceFloatingBars.kt, PreferencesPane.kt,
ToolbarCustomizer.kt e ToolbarWorkspacePreview.kt.

Não duplicar cálculo de geometria, dispatch de ferramentas, swatches ou
renderização de prévia. Separar:
- DOMÍNIO PURO: ToolbarWorkspace, ToolbarBar, ToolbarGeometry, ToolbarNode.
- CONFIGURAÇÃO: versão do JSON, migração, validação e persistência atômica.
- MEDIÇÃO: algoritmo determinístico dp/pixel, grades, orientação,
  encaixe, safe bounds e colisões, com testes sem Android.
- RENDERIZAÇÃO: frame visual reutilizável para prévia e barras reais.
- INPUT: máquina de estados de gesto sem efeitos colaterais até commit do drag.
- CONFIG UI: view-model de rascunho/undo e editor de propriedades.
- INTEGRAÇÃO: binding com ToolbarItem e engines de xnote/xcanvas.

O ToolbarLayout atual modela seções planas. Evolução recomendada: introduzir
uma árvore finita de ToolbarNode com nós Tool, ColorPalette, Group, Grid,
Spacer/Separator. NÃO usar JSON arbitrário sem schema, nem aninhar infinitamente.
Limites inicialmente: profundidade 3, linhas/colunas 1–6, IDs estáveis para
nós e barras, tamanho mínimo da hitbox, overflow explícito. Implementar
migração unidirecional v2 -> v3 sem perder seções, visibilidade ou ordem.
Versão futura desconhecida e JSON inválido retornam fallback seguro, com
backup do estado antigo e sem apagar outros settings.

Contextos: "shared" projeta um perfil comum filtrando itens incompatíveis;
"independent" preserva ambos. Não alterar o perfil escondido ao alternar
modo. Canvas e note usam as mesmas definições de tipo de ferramenta, mas
mantêm bindings próprios de engine/canvas. O estado de tinta/caneta deve
continuar compartilhado apenas onde a aplicação já compartilha.

Aparência: usar configurações em dados imutáveis e tokens de tema,
armazenar cores como ARGB/RGBA estáveis, converter dp com LocalDensity.
Blur não pode causar falhas em dispositivos sem suporte; fallback para
superfície sem blur com cor e opacidade adequadas. Preferir renderizado
nativo e recomposições mínimas; não recalcular medidas ao mover caneta.

## 6. Sequência de implementação com gates

Cada milestone termina com tests, documentação, commit e revisão de diff.
Milestones dependentes não devem ser dados como concluídos sem QA.

### M0 — Organização e linha de base [FEITO]
- Clone isolado do upstream; origem/fork configurados; branch da feature
  preservada; debug applicationId exclusivo e chave de debug local.
- Compilar testes existentes e registrar commit-base.
- Gate: source/fork claros, nenhuma alteração em outro clone.

### M1 — Modelo + migração [IMPLEMENTADO; REVALIDAR]
- Várias barras, IDs persistentes, operações imutáveis, note/canvas,
  JSON versionado, fallback de layout legado.
- Completar testes de ordenação, conversão, input malformado, ID repetido,
  schema futuro, round-trip e compatibilidade ao mudar toolbar principal.
- Gate: abrir settings legados sem perder barra e rollback possível.

### M2 — Renderização e input [PARCIAL]
- Renderizar secundárias e principal relocada, exibir/ocultar, mover com
  long-press em contêiner, 4 encaixes e 9 âncoras flutuantes.
- Corrigir semântica e direção de barras verticais, colisões, stack,
  safe areas, reservação de espaço, first-stroke, z-order, focus, split.
- Gate: interação com ferramentas antigas continua funcional em 3 panes
  de tamanho/orientação diferentes, sem clipping.

### M3 — Configuração, preview e rascunho [PARCIAL]
- Tela exclusiva já existe com criar, duplicar, selecionar, propriedades
  básicas, grade 3×3 e preview simples interativo.
- Refinar preview para fidelidade de tamanho/aparência, posicionamento
  das barras fixas e empilhamento; adicionar undo/redo, cancelar/aplicar,
  preset restaurar e criação guiada.
- Gate: prévia e tela real concordam nos testes de referência.

### M4 — Aparência e geometria [PENDENTE]
- Modelo/persistência de width/height auto/manual, escala, padding,
  fundo, alfa, borda, cor, raio, blur com fallback, alinhamento.
- Um GeometryResolver calcula limites, orientação, largura e altura
  antes da composição; snapshots para tamanhos extremos.
- Gate: ícones não somem nem sobrepõem; conf. sobrevive restart.

### M5 — Elementos individuais e cores [PENDENTE]
- Painel de tamanho visual/hitbox/espaço de cada elemento.
- Controle do número de cores por barra e grade NxM, preservando paleta
  de tinta real sem cópias mutáveis.
- Gate: cores atuais mudam sem atrasos; espaços não quebram popup.

### M6 — Grupos e grades aninhadas [PENDENTE]
- Schema v3 de nós, renderer recursivo limitado e validação de capacidade.
- Grid 3×2 de ferramentas e de cores; tamanho misto e auto-fit previsível;
  drag/drop entre nós/barras com highlight de destino.
- Gate: reversibilidade, upgrade v2->v3, export/import round-trip.

### M7 — Caneta unificada [PENDENTE]
- Grupo acionador com último tipo ativo, menu de estilos, presets
  independentes; nenhum novo ID de Tool inválido.
- Gate: alternar caneta/borracha/estilos preserva config e lag baixo.

### M8 — Menu contextual, mover e redimensionar [PENDENTE]
- Integrar máquina de estados press/drag/menu para fundo e ferramentas,
  Lock/Unlock, Configure, Resize e Hide/Restore.
- Modo Resize explícito com handles, transação persistente, Cancel/Undo;
  desabilitar em modo de escrita e barra travada.
- Gate: testes de concorrência de gestos, sem clicks fantasma.

### M9 — Perfis, acessibilidade e performance [PENDENTE]
- Compartilhado/independente em GUI, seleção de perfil e preservação
  dos dois originais. Leitores de tela, foco teclado, labels, hitboxes.
- Benchmarks em Redmi Pad 2 para toque, 60fps, p95 frame, latência de
  traço, memórias em três barras e editor split.
- Gate: regressões de primeira escrita e layouts resolvidas.

### M10 — Qualidade, documentação, PRs [PENDENTE]
- Rodar testes unitários, migração, instrumentados, snapshots, QA
  manual tablet, emulator, modo claro/escuro, paisagem/retrato.
- Extrair PRs pequenos e independentes em branches novas baseadas em
  upstream/master; escrever vídeos/screenshots e descrições humanas.
- Gate: nenhum conflito aberto, docs de uso e matriz de testes verificada.

## 7. Matriz mínima de testes de aceitação

| Cenário | Esperado |
| --- | --- |
| Instalação legado sem configuração | Uma barra original, sem crash/perda de dados |
| 1/2/5 barras | Ordem e visibilidade persistem após restart |
| Principal fixa 4 lados | Mantém o modo fixo e reajusta espaço do documento |
| Flutuante 9 âncoras | Encaixe correto e persistente em nota/canvas |
| Movable OFF | Arrastar não move, taps executam ferramenta |
| Longpress sem arrastar | Menu; sem disparar ferramenta acidental |
| Longpress + arrastar | Preview, snap e persistência na posição escolhida |
| Alternar shared/independent | Preserva os perfis originais e filtros |
| Grade 3×2 com 6 cores/ferramentas | Sem sobreposição e com acionamento correto |
| Horizontal/vertical/landscape/split | Dentro dos limites visíveis; sem controles perdidos |
| Barra flutuante sobre nota branca/escura | Bordas e contraste legíveis |
| Drag com caneta, dedo e mouse | Sem lag, sem perda do primeiro stroke |
| 2 apps XNotes debug simultâneos | IDs/assinaturas independentes; sem sobrepor dados |
| Upgrade e settings inválidos | Migração ou fallback seguro, sem apagar dados |
| Change appearance/padding/blur | Prévia e real concordam e persistem |
| Undo/Cancel/Reset | Resultado reversível, histórico consistente |

Automatizar casos puros com testes Kotlin/JVM. Usar UI tests para configuração,
semântica, docking e preview; instrumentados para stylus/motion apenas quando
o harness tiver suporte. O simulador não equivale ao Redmi Pad 2: executar
QA manual com caneta antes de declarar milestone final.

Comandos locais sugeridos:
- git diff --check
- ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
- adb devices
- adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk

## 8. Divisão recomendada de PRs

A branch feat/customizable-toolbars é de INTEGRAÇÃO, não deve virar um PR
monolítico. Recriar PRs a partir de upstream/master:

1. chore/toolbar-workspaces-schema — modelos puros e testes.
2. feat/toolbar-workspaces-persistence — codec versionado, migração e testes.
3. feat/toolbar-multiple-bars — render básico + compatibilidade.
4. feat/toolbar-manager — CRUD de barras e perfil xnote/xcanvas.
5. feat/toolbar-live-preview — preview/configuração e testes UI.
6. feat/toolbar-docking-gestures — press/drag, âncoras e safe areas.
7. feat/toolbar-appearance — tamanho/padding/cores/bordas/blur.
8. feat/toolbar-tool-grids — elemento individual + grupos/grade + migração v3.
9. feat/toolbar-color-grids — quantidade/posicionamento de cores.
10. feat/toolbar-unified-pen — agrupador de ferramentas de escrita.
11. feat/toolbar-context-actions — menu, lock e resize edit mode.
12. feat/toolbar-profiles-polish — shared/independent, acessibilidade, QA.

Conventional Commits, sem mistura de funções; commits e testes atômicos.
Não reescrever histórico já publicado. O agente pode preparar branches e commits,
mas NUNCA abrir, editar, comentar ou mesclar PRs automaticamente. O usuário
fará o envio e a manutenção manual dos PRs. Push para o próprio fork depende
da autorização do usuário; nunca enviar para upstream. Visar PRs legíveis (centenas de linhas, e não milhares),
mas dividir por domínio sem quebra artificial de compilação; justificar
exceções. Documentar dependências explícitas e ordem de merge.
Se master avançar, atualizar primeiro branches ainda não publicadas com
fetch+rebase seguro; em branches publicadas priorizar merge ou combinar
estratégia com o mantenedor.

## 9. Fechamento do projeto (Definition of Done)

Só marcar "DONE" quando: todos os itens da seção 3 entregues; testes e
migrações verdes; preview próximo ao renderizador real; performance e
primeiro traço verificados no tablet; acessibilidade e recuperação garantidas;
sem duplicação relevante de código e sem mudanças fora do escopo;
documentação e release notes completas; PRs isolados e revisáveis.
Se uma melhoria desejada implicar alteração de formatos .xnote/.xcanvas ou
per-note override, registrar ADR, proposta de migração e pedir decisão
antes de expandir escopo. O agente não deve fingir trabalho em segundo plano
quando sua sessão terminou.

## 10. Lista de riscos que exigem revisão constante

- Conflito do longpress do contêiner com controles de cor e menus da caneta.
- Dimensões e hitboxes de 48dp versus barras compactas.
- Viewport cover / perda de tinta em split-view e redimensionamento.
- Alterações no upstream durante desenvolvimento longo; cherry-picks/PRs.
- Blur caro/indisponível em GPU Android de entrada.
- Dados legados, versão do codec e uso de IDs estáveis.
- Desaparecimento de todas as ferramentas por visibilidade inválida.
- Stacks/grades maiores que viewport, cutouts ou teclado.
- Profiles shared alterando o perfil que deveria permanecer independente.
- Protótipo de preview não idêntico ao layout real: converge para o mesmo resolver.

Consulte docs/toolbar-task-state.md para o próximo item e registre descobertas
lá em cada rodada. Nunca declarar essas fases concluídas apenas porque uma
versão debug instala no emulador.
