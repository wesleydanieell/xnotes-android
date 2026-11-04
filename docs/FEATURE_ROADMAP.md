# XNotes — roadmap de ferramentas, workspaces e camadas

Atualizado: 04/11/2026. Branch de integração: `feat/customizable-toolbars`.
Este documento é o índice de produto; o detalhamento técnico das toolbars está em
[toolbar-implementation-guide.md](toolbar-implementation-guide.md), as tarefas executáveis
em [toolbar-task-state.md](toolbar-task-state.md) e os testes no
[toolbar-adb-test-playbook.md](toolbar-adb-test-playbook.md).

## Situação atual em 04/11/2026

- **Implementado, mas sem QA físico completo:** barras múltiplas, REGIONS/CONTINUOUS,
  sections e spacers, edição por arraste, dimensões individuais básicas, cópia
  XNotes↔XCanvas com prévia e filtro, oito workspaces neutros, salvar/restaurar
  ambos os perfis, importação/exportação JSON pelo seletor Android. Cores agora
  ficam dentro de Elementos e Workspaces ao lado do seletor Main toolbar.
- **Bloqueadores para considerar concluído:** testes reais com caneta e primeira
  escrita no Redmi, alinhamento prévia/runtime, 4 docks, 9 posições, rotação,
  split-view, barras numerosas, persistência e fluxo Android SAF ponta a ponta.
- **Próxima engenharia:** edição transacional Aplicar/Cancelar + Desfazer/Refazer,
  fallback recuperável quando o layout não cabe, refinamento de hitboxes e UX
  de perfis compartilhados/independentes. Detalhes em `toolbar-task-state.md`.
- **Decisão posterior à antiga P1:** grupos compactos genéricos/pastas foram
  testados, rejeitados pelo usuário e revertidos. Não reintroduzir no escopo
  vigente. Preservar Pen Group e sections existentes. Itens P1 de ToolGroup
  abaixo são históricos, não o próximo trabalho aprovado.

## Decisão consolidada em 03/11/2026

- **Toda edição de toolbars fica na tela dedicada de configuração.** A nota terá apenas
  um atalho direto para abrir essa tela; sem painel de propriedades ou resize in-note.
  O gesto já existente de mover uma barra continua sujeito a QA, não vira editor visual.
- **Aparência individual simplificada:** largura, altura, padding, espaçamento e
  tamanho dos elementos. Manter cores e cantos coerentes com o tema do aplicativo;
  remover do escopo futuro blur/opacidade/borda/cor/raio por barra.
- **Grupos e grades:** primeira etapa reutiliza seções como grupos com layouts linear,
  1×2, 2×2 e 3×2; no máximo duas fileiras e somente controles de célula única.
  O schema de grupos aninhados fica fora do MVP. Editor, toolbar real e preview devem
  usar a mesma organização e validar capacidade antes de aceitar arrastes.
- **Pen Group:** grupo opcional, separado das canetas individuais. Primeiro toque arma
  a ferramenta anterior se não estiver ativa; tocar quando já está ativa abre opções.
  Segurar + arrastar escolhe pelo círculo exibido; ícones devem ser reconhecíveis e
  visualmente coerentes com os demais do app.

## Decisão de produto

O problema inicial é a saturação da barra atual. A solução não deve exigir que
o usuário crie várias barras para encontrar uma caneta. Entregar progressivamente:
(1) grupos inteligentes e paleta rápida; (2) múltiplas barras e geometria robusta;
(3) editor visual e perfis completos; (4) camadas como projeto separado.

A barra principal também é configurável; manter sempre uma saída de recuperação
e um preset padrão. Nunca quebrar ferramentas antigas, latência de tinta ou dados.
O protótipo existente é base de engenharia, NÃO UI aprovada.

## P0 — Confiabilidade do protótipo atual (bloqueia expansão)

- [ ] Reproduzir e resolver conflitos entre long-press do contêiner e da ferramenta.
- [ ] Validar 4 docks, 9 âncoras, 1/2/5 barras, insets, colisões e split-view.
- [ ] Validar preservação do primeiro traço com caneta no Redmi Pad 2.
- [ ] Criar resolvedor único de geometria compartilhado entre preview e interface real.
- [ ] Criar modo rascunho com Aplicar, Cancelar, Desfazer, Refazer e Reset.
**Gate:** matriz M2/M3 do guia, sem regressão de escrita ou clipping.

## P1 — Menos botões, acesso mais rápido

- [ ] Definir `ToolGroup` com ID estável, itens permitidos e último item ativado.
- [ ] Implementar grupo recolhível: toque curto ativa último, toque prolongado abre
      popover; gesto de seleção rápida é opcional e deve passar teste de conflito.
- [ ] Paleta rápida configurável de favoritos (canetas, cores e ferramentas).
- [ ] Exibir estado ativo fiel, sem duplicar o estado da caneta/tinta do editor.
- [ ] Permitir grupo expandido ou compacto; uso por dedo, mouse e stylus.
- [ ] Testar seleção, restauração e migração de toolbar plana para grupo.
**Gate:** o usuário acessa ferramentas frequentes em até duas ações; tap na
caneta nunca inicia drag; não perder acesso às ferramentas ocultas.

## P2 — Motor de múltiplas barras completo

- [ ] Configurar barra principal e secundárias: criar, duplicar, remover, ocultar.
- [ ] Layout horizontal/vertical, linhas/colunas, overflow, dimensões auto/manuais.
- [ ] Refinar padding, espaçamento, tamanho individual e hitboxes por elemento; fundo, borda, opacidade e blur independentes foram removidos do escopo.
- [ ] Ancoragem 4 docks/9 posições flutuantes, pilhas e áreas seguras.
- [ ] Slots/grades 2×2 e 3×2 com restrições de capacidade e hitbox.
- [ ] Testar migração JSON com dados legados, inválidos e versões futuras.
**Gate:** renderer e preview compartilham modelo/resolvedor; persistência
reversível e desenho fluido.

## P3 — Editor de workspaces

- [ ] Tela dedicada com nota ilustrativa não editável e preview real.
- [ ] Drag-and-drop visual de barras, ferramentas e grupos com indicação de destino.
- [ ] Perfis xnote/xcanvas compartilhados ou independentes sem sobrescrever o inativo.
- [x] Modelos neutros de Escrita, Estudos/PDF e Desenho; import/export JSON com schema validado (implementação e testes JVM; QA Android SAF pendente).
- [ ] Acessibilidade, teclado, área de toque e modo compacto.
**Gate:** edição transacional, preview fiel, recuperação de configuração inviável.

## P4 — Camadas (trilha independente)

Consultar [layers-roadmap.md](layers-roadmap.md). Não alterar arquivos de nota
ou motor de desenho nesta branch para antecipar camadas.

## Ordem de execução e disciplina

P0 deve terminar antes de P1 estrutural; P1 pode começar por modelo puro e testes
em paralelo ao desenho de P2, mas o renderer único é dependência de ambas.
Milestone só é FEITO com testes executados e evidência registrada.
Trabalhar em commits pequenos; futuras PRs extraídas de upstream/master,
sem publicar PR automaticamente. Preservar worktrees, branches e dados locais.

## Como retomar em outra sessão

1. Abrir `AGENTS.md` e os três documentos de toolbars citados acima.
2. `git status -sb`, `git log -5 --oneline`, comparar `origin` e `upstream`.
3. Ler `toolbar-task-state.md` e escolher a primeira tarefa desbloqueada.
4. Teste pequeno -> implementação -> testes JVM/Android -> QA -> documentação.
5. Registrar resultado real, SHA, limitações e **uma próxima ação** no checkpoint.
