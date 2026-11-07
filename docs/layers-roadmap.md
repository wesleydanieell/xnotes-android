# XNotes — estudo e roadmap de camadas

Status: proposta de produto, **nenhuma implementação autorizada nesta branch**.
Data: 03/11/2026. Dependência: estabilização da experiência de ferramentas.

## Objetivo

Permitir organizar conteúdo escrito e desenhado em camadas, sem transformar
o XNotes em um editor gráfico pesado. Casos primários: anotação sobre PDFs,
rascunho e resposta separados, esconder marcações, comparar soluções e
bloquear um fundo sem impedir escrita sobre ele.

## Decisões a validar antes de código

- Camadas por página, por nota/canvas ou ambas? Em notas paginadas, camadas
  por página são mais previsíveis; no canvas infinito, avaliar escopo global.
- Itens existentes pertencem a uma camada padrão implicitamente; abrir notas
  legadas deve produzir o mesmo resultado visual.
- PDF incorporado/externo permanece base imutável; camadas organizam apenas
  anotações, nunca reescrevem bytes do PDF original.
- Documentos `.xnote` antigos devem continuar abrindo; exportar e importar
  sem perda exige versionamento do manifesto e testes de round-trip.
- Definir comportamento ao compartilhar com clientes antigos (flatten de
  exportação, leitura degradada ou bloqueio com explicação). Nunca assumir
  compatibilidade silenciosa.

## Roadmap

### L0 — Levantamento de arquitetura [ ]
- [ ] Mapear `ShapeItem`/`TextItem`, páginas, ordenação, seleção, histórico,
      serialização `.xnote`, PDF e exportação.
- [ ] Documentar formatos e exemplos de notas legadas.
- [ ] ADR com estratégia de compatibilidade e rollback.
**Gate:** contrato persistente aprovado, sem alteração no formato.

### L1 — Modelo puro [ ]
- [ ] IDs estáveis de camadas, ordem, nome, visível e bloqueado.
- [ ] Política para mover itens entre camadas e camada ativa.
- [ ] Regras de hit-test: não selecionar invisíveis/bloqueados; preservar undo.
- [ ] Testes unitários com páginas vazias, remoção/reordenação e migração.
**Gate:** dados e operações determinísticos sem Android.

### L2 — Renderer e interação [ ]
- [ ] Respeitar ordem de renderização, seleção e cache por camada.
- [ ] Tornar ocultar/exibir barato sem remeshing de todo o documento.
- [ ] Medir memória e latência da caneta em documentos grandes.
**Gate:** comportamento equivalente quando há uma camada só.

### L3 — Interface simples [ ]
- [ ] Painel recolhível: criar, renomear, mover, bloquear, ocultar e selecionar.
- [ ] Mover seleção para outra camada com undo/redo.
- [ ] Acesso contextual sem lotar toolbar (futura integração com P1/P3).
**Gate:** toque/caneta, retrato/paisagem e split não perdem primeiro traço.

### L4 — Persistência, interoperabilidade e release [ ]
- [ ] Migração de documentos antigos, round-trip e estratégia de exportação.
- [ ] PDF exportado preserva composição final de camadas visíveis.
- [ ] Testes de recuperação após crash, backups, notas grandes e compatibilidade.
**Gate:** zero perda de conteúdo e nenhuma corrupção de arquivos reais.

## Fora de escopo inicialmente

Máscaras, blend modes, filtros, clipping groups e árvore recursiva de camadas.
Não desenvolver sem caso de uso e benchmark que justifiquem complexidade.

## Branches e PRs

Criar worktree e branch próprios a partir de upstream/master apenas após ADR.
Separar PRs de modelo, renderer, UI e persistência com dependências explícitas.
Não misturar mudanças de camadas na branch `feat/customizable-toolbars`.
