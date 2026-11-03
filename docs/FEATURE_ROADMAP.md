# XNotes — roadmap de ferramentas, workspaces e camadas

Atualizado: 03/11/2026. Branch de integração: `feat/customizable-toolbars`.
Este documento é o índice de produto; o detalhamento técnico das toolbars está em
[toolbar-implementation-guide.md](toolbar-implementation-guide.md), as tarefas executáveis
em [toolbar-task-state.md](toolbar-task-state.md) e os testes no
[toolbar-adb-test-playbook.md](toolbar-adb-test-playbook.md).

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
- [ ] Aparência individual: padding, fundo, borda, raio, opacidade, blur com fallback.
- [ ] Ancoragem 4 docks/9 posições flutuantes, pilhas e áreas seguras.
- [ ] Slots/grades 2×2 e 3×2 com restrições de capacidade e hitbox.
- [ ] Testar migração JSON com dados legados, inválidos e versões futuras.
**Gate:** renderer e preview compartilham modelo/resolvedor; persistência
reversível e desenho fluido.

## P3 — Editor de workspaces

- [ ] Tela dedicada com nota ilustrativa não editável e preview real.
- [ ] Drag-and-drop visual de barras, ferramentas e grupos com indicação de destino.
- [ ] Perfis xnote/xcanvas compartilhados ou independentes sem sobrescrever o inativo.
- [ ] Presets Escrita, Estudo/PDF e Desenho; import/export com schema validado.
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
