# XNotes — Toolbars V2: guia de produto, arquitetura e implementação

**Estado:** especificação para implementação; não representa funcionalidades já concluídas.  
**Data:** 28/10/2026  
**Upstream:** `shardulvs/xnotes-android`  
**Fork:** `wesleydanieell/xnotes-android`  
**Branch recomendada:** `feat/toolbar-editor-v2` (criada da `upstream/master` em um *worktree* separado).  
**Regra de colaboração:** commits organizados são permitidos; **nunca criar, comentar, alterar ou fazer merge de PR automaticamente**.

## 1. Visão e objetivo

Implementar no XNotes um **editor visual de HUD para barras de ferramentas**: a manipulação direta de um editor de HUD de jogo, a flexibilidade de containers/widgets dos painéis do KDE Plasma e a praticidade das ferramentas de escrita do Samsung Notes.

O usuário deve conseguir, sem escrever JSON nem coordenadas, criar várias barras e montar composições como a referência fornecida na conversa:

- Um botão grande à esquerda ocupando duas linhas.
- Oito cores num grid de quatro colunas e duas linhas, com separador horizontal.
- Quatro ferramentas num grid 2×2.
- Dois botões altos à direita, cada um ocupando duas linhas.
- Tudo no mesmo container, com bordas arredondadas, padding e espaçamento consistentes.

**Princípio:** a experiência principal é manipular a própria barra. Os painéis mostram propriedades contextuais. O motor deve garantir encaixe, dimensões e persistência.

### Fora de escopo da V2 inicial

- Desenhar, navegar ou editar conteúdo da nota que aparece atrás das barras no editor.
- Redimensionar barras por gesto **na nota normal** (somente no editor de HUD).
- Configurações persistentes específicas de cada nota (avaliar futuramente).
- Layout completamente livre por coordenadas para cada ferramenta.
- Publicação automática de PR ou merge no upstream.

## 2. Experiência de uso

### 2.1 Na nota normal

1. A barra executa suas funções normalmente.
2. Pressionar e segurar **a área não interativa do container** e mover inicia o arraste, caso a barra seja móvel.
3. Pressionar e segurar sem arrastar abre um menu contextual.
4. O menu oferece: **Personalizar barra**, Bloquear/Desbloquear posição, Duplicar, Ocultar e Excluir (excluir somente secundárias).
5. **Personalizar barra** abre diretamente o editor V2 com a barra e o contexto xnote/xcanvas corretos selecionados.
6. Barras bloqueadas continuam acessando o menu; apenas a movimentação é desabilitada.
7. Soltar a barra valida sua posição e a persiste. Feedback tátil/visual discreto, se disponível.
8. Toques em ferramentas e traços da caneta não podem ser capturados acidentalmente como arraste da barra.

**Resolução de conflito do gesto:** após o tempo de long press, entrar em estado `ARMED`; movimento além do *touch slop* inicia `DRAGGING`; liberação ainda imóvel abre `MENU`. Ao liberar após arraste, confirmar posição. Cancelamento do ponteiro restaura o estado anterior. Testar dedo, mouse e stylus.

### 2.2 No editor de HUD

O editor ocupa uma tela dedicada com:

- **Centro:** fundo estático que simula uma nota infinita e exibe as barras reais em modo editável. Ele não recebe escrita, pan, zoom ou interação de documentos.
- **Painel de componentes:** catálogo pesquisável de ferramentas, cores, containers, grupos, grids, separadores e espaçadores; recolhível.
- **Painel de propriedades:** exclusivamente do objeto selecionado; recolhível.
- **Barra superior:** voltar, perfil xnote/xcanvas, perfil compartilhado/independente, desfazer/refazer, Cancelar e Aplicar.
- **Lista compacta de barras:** selecionar, criar, duplicar, ocultar, excluir e renomear; não duplicar uma segunda árvore de ferramentas em texto.

O fundo pode alternar entre branco, escuro, pautado e quadriculado para examinar contraste. O fundo continua **não interativo**. As barras são *overlays* do viewport, nunca objetos pertencentes ao documento.

### 2.3 Hierarquia de seleção

- Toque no espaço vazio da barra: seleciona a **barra**.
- Toque em uma ferramenta: seleciona a **instância da ferramenta**.
- Toque na moldura de grupo/grid: seleciona **grupo/grid**.
- Breadcrumb contextual: `Barra / Grid de cores / Cor vermelha` permite subir pela hierarquia.
- Contornos discretos e alças surgem apenas em edição; não devem esconder ícones.
- **Escape/voltar** recua um nível de seleção antes de abandonar a tela.

Não exibir simultaneamente os controles de barra, grupo e ferramenta. Propriedades inexistentes não aparecem.

### 2.4 Catálogo e ações rápidas

- Abrir por `+` numa célula vazia, por botão de catálogo ou por arrastar um item da biblioteca.
- Pesquisa, categorias e ícones com nomes legíveis.
- Clicar adiciona à posição selecionada; arrastar adiciona ao destino destacado.
- Ação direta **Duplicar**, **Remover**, **Agrupar**, **Desagrupar** quando pertinente.
- A mesma ferramenta pode ser adicionada repetidamente; cada instância mantém ID próprio.
- Não depender de setas ↑/↓ para ordenar e não exibir duas interfaces concorrentes para a mesma composição.

### 2.5 Alterações, testes e confirmação

- Editor trabalha com **rascunho isolado** da configuração persistida.
- `Undo/Redo` cobre adicionar/remover/mover/redimensionar/editar propriedades.
- `Aplicar` persiste atomicamente; `Cancelar` descarta o rascunho.
- Sair com alterações pendentes oferece aplicar, descartar ou continuar editando.
- A verificação funcional das ferramentas acontece **depois de aplicar, em uma nota real de teste**. A tela de HUD só configura; não executa ações de escrita ou edição de documento.

## 3. Regras de layout: encaixe automático

### 3.1 Regra central

**Ferramentas e grupos não têm posição absoluta arbitrária.** A posição é determinada pela ordem e ocupação de slots dos containers. Arrastar mostra uma prévia exata do destino; soltar produz um layout válido e determinístico.

- Um container `Row` organiza filhos horizontalmente.
- Um container `Column` organiza filhos verticalmente.
- Um container `Grid` organiza filhos por linhas/colunas, em ordem **row-major** quando inserção automática.
- `Group` é um container com layout selecionável; pode conter outros containers.
- `Spacer` cria espaço fixo ou flexível; `Divider` cria separações visuais.
- Não permitir ciclos, IDs duplicados, spans inválidos ou filhos infinitamente aninhados.

### 3.2 Ocupação de grid

- `Grid(rows, columns)` tem células identificáveis por `(row,column)`.
- `rowSpan` e `columnSpan` permitem ferramentas altas, largas ou grandes.
- Uma ferramenta 1×2 ocupa duas linhas; a 2×2 ocupa quatro células.
- Não permitir sobreposição silenciosa, perda de elementos ou índices fora do grid.
- Antes de soltar, mostrar *ghost* no tamanho real e um destino válido/inválido.
- Quando um destino está ocupado, especificar e testar política determinística: inserir deslocando elementos compatíveis; se impossível, indicar bloqueio e não alterar o rascunho.
- Dimensões reduzidas não podem excluir filhos; apresentar erro ou manter overflow recuperável/visível.
- Células vazias exibem botão `+` **somente no modo edição**, com ação funcional.
- Reordenar não altera automaticamente o ID ou propriedades da instância.

### 3.3 Arraste de grupos

- Grupo se move como uma unidade, preservando seu layout interno.
- Aceitar mover entre barra, grupo e grid quando as restrições permitirem.
- Impedir soltar um container dentro de si mesmo ou de descendentes.
- Destaques distintos para **inserir antes/depois**, **inserir dentro** e **substituir posição**.
- Cancelar o gesto não modifica o modelo nem gera evento de Undo.

### 3.4 Dimensões e espaçamento

Separar rigorosamente:

- **Padding:** espaço entre contorno interno do container e conteúdo.
- **Gap:** espaço entre filhos/trilhas.
- **Tamanho visual:** largura/altura ou preset do elemento.
- **Área de toque:** pode exceder o tamanho do ícone; alvo adequado sem introduzir sobreposição de hitboxes.

Limites *iniciais propostos*, a validar em tablet e telas menores:

| Parâmetro | Faixa inicial | Observação |
| --- | --- | --- |
| Padding | 0–32 dp por eixo/lado | Padrão coerente com tema |
| Gap | 0–24 dp | Separado por eixo quando necessário |
| Ícone/ferramenta visual | 24–96 dp | Hit target deve continuar utilizável |
| Corner radius | 0–50% do menor lado | Respeitar limites geométricos |
| Linhas/colunas | 1–8 cada na primeira versão | Validar performance e overflow |

Esses limites não são verdades universais. Ajustar conforme ergonomia e configuração do app. `Auto`, `Compact`, `Regular` e `Comfortable` devem coexistir com dimensões personalizadas e regras de medida previsíveis.

### 3.5 Posicionamento das barras no workspace

Nove âncoras: superior-esquerda, superior-centro, superior-direita, centro-esquerda, centro, centro-direita, inferior-esquerda, inferior-centro e inferior-direita.

- Barras **fixas** ancoram na região escolhida e respeitam margens.
- Barras **flutuantes** inicialmente usam encaixe visual nessas regiões, com deslocamentos limitados se necessário. Posição totalmente livre pode ser opção avançada posterior.
- Várias barras podem compartilhar uma região; o resolvedor distribui/empilha sem criar sobreposições inacessíveis.
- Respeitar recortes, teclado, orientação, insets e divisões de tela.
- Arrastar perto da região ativa realça a âncora antes de soltar.

## 4. Modelo de dados proposto

Adaptar os nomes à arquitetura existente; não duplicar modelos desnecessariamente.

```kotlin
data class ToolbarWorkspaceConfig(
    val schemaVersion: Int,
    val bars: List<ToolbarBarConfig>,
    val profileMode: ProfileMode,
)

data class ToolbarBarConfig(
    val id: String,
    val name: String,
    val visible: Boolean,
    val movable: Boolean,
    val presentation: Presentation, // docked ou floating
    val geometry: BarGeometry,
    val appearance: BarAppearance,
    val root: LayoutNode,
)

sealed interface LayoutNode {
    val instanceId: String

    data class Tool(
        override val instanceId: String,
        val toolType: String,
        val properties: Map<String, ToolProperty>,
        val rowSpan: Int = 1,
        val columnSpan: Int = 1,
    ) : LayoutNode

    data class Container(
        override val instanceId: String,
        val kind: LayoutKind, // row, column, grid
        val children: List<LayoutNode>,
        val rows: Int = 1,
        val columns: Int = 1,
        val padding: Insets,
        val gap: Spacing,
    ) : LayoutNode

    // Spacer e Divider também são nodes.
}
```

**Isto é um modelo conceitual, não código pronto para colar.** Preferir os tipos do upstream quando já adequados. Definir propriedades tipadas/validadas; evitar um `Map<String, Any>` indiscriminado.

Separar módulos/responsabilidades:

1. **Model + validação:** invariantes, IDs, constraints, conversão legado.
2. **Layout resolver:** cálculo puro de medidas, trilhas, spans, slots e pontos de drop.
3. **Renderer:** único mecanismo de layout para barra real e preview, com camadas adicionais de decoração para edição.
4. **Interaction controller:** seleção, drag/drop, hover, long press, resize no editor, undo/redo.
5. **Persistence:** armazenamento com versão e migrações.
6. **Screens:** editor HUD, catálogo, propriedades, gerenciador de barras.

**Proibição arquitetural:** não manter um segundo algoritmo de medidas para o preview. Apenas decoração/controles de edição podem divergir.

## 5. Personalização contextual

| Seleção | Propriedades |
| --- | --- |
| Workspace | Perfis xnote/xcanvas, compartilhamento, fundo de teste, barras existentes |
| Barra | Nome, presença, âncora, fixa/flutuante, móvel, largura/altura/auto, padding, aparência |
| Ferramenta | Tipo, tamanho, comportamento e propriedades realmente suportadas por ela |
| Paleta | Cores, quantidade, linhas/colunas, tamanho dos swatches e separadores |
| Grid | Linhas, colunas, gap, padding, spans/ocupação |
| Grupo | Layout row/column/grid, gap, padding, alinhamento, filhos |
| Separador | Direção, comprimento, espessura, cor e margens |
| Espaçador | Tamanho fixo/flexível, mínimos/máximos |

Não exibir propriedades que não alterem o funcionamento real. Não criar controles visuais com callbacks vazios.

## 6. Animações e acessibilidade

- Seleção: transição sutil do contorno e alças.
- Arraste: *ghost* do item e indicação do slot de destino.
- Soltar: reposicionamento curto, preservando resposta imediata.
- Inserção/remoção: transições suaves e previsíveis; não animar o layout inteiro se prejudicar FPS.
- Menus, painéis, snackbar e estados vazios acessíveis.
- Respeitar redução de movimento, foco/teclado, TalkBack e contraste.
- Testar toque, mouse e stylus separadamente. Animação nunca deve bloquear a primeira escrita após mudança de ferramenta.

## 7. Plano de execução com gates

**Não avançar de fase se o resultado visual ou funcional estiver ruim.** Cada etapa deve produzir código, evidência de testes e commit focado.

| Fase | Entrega | Gate de conclusão |
| --- | --- | --- |
| M0 — Diagnóstico | Inspeção do upstream, estado antigo, APIs e testes | Inventário do que reutilizar, de riscos e baseline compilando |
| M1 — UX/protótipo | Editor HUD de uma tela, painéis recolhíveis e seleção | Capturas de tela avaliadas e fluxo de adição compreensível |
| M2 — Engine | Containers, grids, spans, validação e cálculo puro | Testes determinísticos para a barra de referência |
| M3 — Renderer unificado | Render real + preview compartilhando medidas | Comparação visual entre editor e nota equivalente |
| M4 — Gestos de composição | Drag/drop, slots, grupos, seleção, undo/redo | Sem colisões, perdas, setas obrigatórias ou ações falsas |
| M5 — Propriedades | Estilo, tamanhos, cores, espaços, barras, perfis | Alteração visível e persistida; Cancelar restaura estado |
| M6 — Integração | Menu long press, barras múltiplas/principal, xnote/xcanvas | Fluxo ponta a ponta no AVD sem conflito com stylus |
| M7 — QA | Orientação, split, migração, stress, acessibilidade/performance | Testes automatizados e roteiro ADB documentados |
| M8 — Finalização | Limpeza de legado, documentação e commits | Diff revisável, sem bugs conhecidos P0/P1, PR manual posterior |

### Gate obrigatório de M2: fixture da imagem

Implementar um teste/fixture que produza exatamente, em ordem:

1. Ferramenta grande ocupando duas linhas.
2. Paleta 4×2 com oito cores.
3. Separador horizontal dentro da seção da paleta.
4. Grupo/grid de quatro ferramentas 2×2.
5. Dois botões altos ocupando duas linhas cada.

Validar *bounds*, ausência de sobreposições, ordem de elementos, preservação após JSON round-trip, manutenção de instâncias repetidas e paridade entre renderer real e preview. Não confiar apenas em snapshots visuais.

## 8. QA e testes obrigatórios

### Unitários

- Grids 1×1, 3×2, incompletos e lotados; spans 1×2/2×1/2×2.
- Inserção com reordenação determinística; destino inválido; exclusão sem desaparecimentos silenciosos.
- Múltiplas instâncias do mesmo tipo com IDs diferentes.
- Mover entre containers; bloquear ciclos; cancelar arraste.
- Padding/gap no limite mínimo/máximo; dimensionamento automático.
- Serialização, migrações, undo/redo, cancel/apply e perfis compartilhados/independentes.

### UI/ADB

- Abrir editor a partir da barra principal e secundária.
- Criar duas barras, duplicar, ocultar, restaurar, renomear.
- Construir a barra da imagem inteiramente pela UI.
- Inserir duplicatas de caneta; criar 3×2 e mover entre células.
- Alterar aparência e verificar imediatamente no preview e na nota real.
- Mover barras com gestos; menus acessíveis mesmo bloqueadas.
- Fechar/reabrir app e comprovar persistência.
- Rodar em landscape e portrait, tablet/telefone, xnote/xcanvas e split-view.
- ADB taps não substituem **inspeção das screenshots**.

### Performance

- Instrumentar FPS/jank em arraste de layouts complexos.
- Evitar cálculo pesado e serialização a cada frame de movimento.
- Usar projeção/preview de arraste no estado transitório; confirmar no modelo ao soltar.
- Monitorar recomposição de Compose, estabilidade do primeiro input da caneta e vazamentos de estado.

## 9. Processo Git seguro

O trabalho anterior contém alterações úteis e possivelmente alterações não commitadas. **Nunca apagar ou resetar para recomeçar.**

1. Registrar `git status`, `git branch -vv`, `git log`, `git diff --stat` e arquivos não rastreados.
2. Fazer backup recuperável do estado antigo se necessário, sem forçar commits de alterações desconhecidas.
3. `git fetch upstream` e criar novo *worktree* separado em `~/Projetos/xnotes-android-toolbars-v2`, branch `feat/toolbar-editor-v2` baseada em `upstream/master`.
4. Reaproveitar código antigo **seletivamente**, após testes, por commits focados ou cópia manual revisada; não portar o editor antigo inteiro.
5. Commits pequenos com Conventional Commits (`feat(toolbar): ...`, `fix(toolbar): ...`, `test(toolbar): ...`, `docs(toolbar): ...`).
6. Nunca usar `reset --hard`, `clean -fd`, force-push ou alterar branches não relacionadas.
7. Não fazer push de mudanças para `upstream/master`.
8. **Não criar, comentar, modificar, fechar ou fazer merge de PRs automaticamente.**

Se a nova branch já existir, investigá-la antes de escolher outra; nunca sobrescrever trabalho.

## 10. Build e ADB no computador do usuário

Ambiente conhecido (confirmar a existência antes de usar):

```bash
cd /home/wesley/Projetos/xnotes-android-toolbars-v2
git status --short
ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
/home/wesley/Android/Sdk/platform-tools/adb devices -l
/home/wesley/Android/Sdk/emulator/emulator -list-avds
```

AVD utilizado anteriormente: `Small_Tablet`. **Não iniciar segunda instância do mesmo AVD se já houver uma em execução.** Se houver dificuldade com a janela gráfica no Niri/Wayland, investigar e usar o AVD headless com screenshot via ADB como fallback; não afirmar que houve inspeção visual sem inspecionar imagens reais.

Build de desenvolvimento isolada do XNotes comum: manter um `applicationId` exclusivo, por exemplo `com.xnotes.toolbarsdebug` ou um sufixo V2 distinto. **A coexistência depende do applicationId, não apenas da assinatura.** A assinatura debug normal pode ser utilizada se o identificador for diferente. Não sobrescrever a instalação que o usuário utiliza para estudar.

Instalação somente após conferir o pacote destino e dispositivo selecionado. Evitar `-wipe-data`, uninstall e exclusão de notas.

## 11. Critérios de aceite finais

- [ ] Crio duas ou mais barras e edito cada uma separadamente.
- [ ] A barra principal e as secundárias usam o mesmo motor configurável.
- [ ] Consigo construir a barra de referência visualmente, sem editar JSON.
- [ ] Grids e grupos encaixam seus filhos automaticamente por slots, com spans.
- [ ] Consigo usar a mesma caneta várias vezes com propriedades/IDs independentes.
- [ ] Não preciso de setas ↑/↓ para reordenar.
- [ ] Painéis exibem propriedades apenas do objeto selecionado.
- [ ] O fundo simulado não permite escrita/navegação e não altera notas reais.
- [ ] O preview é fiel à barra real; não há uma segunda implementação de layout.
- [ ] Long press estável abre menu; long press + arraste move quando permitido.
- [ ] O menu da barra abre diretamente a edição da mesma barra.
- [ ] Não é possível redimensionar por gesto na nota normal.
- [ ] Undo/Redo, Aplicar e Cancelar preservam consistência e persistência.
- [ ] xnote/xcanvas compartilhados ou independentes funcionam.
- [ ] Build e testes passam e foram realizados testes ADB reais.
- [ ] Capturas de tela de landscape/portrait foram revisadas.
- [ ] Documentação e commits estão organizados, sem PR criado automaticamente.

## 12. Registro consolidado das decisões

Esta seção registra as conclusões alcançadas durante a discussão e deve prevalecer quando documentação antiga sugerir comportamento conflitante.

### Decisões fechadas

1. **Reinício controlado:** refazer a experiência de edição; não apagar a branch antiga nem descartar automaticamente modelos, resolvedores ou testes válidos.
2. **Referências:** ergonomia das ferramentas do Samsung Notes, composição de painéis/widgets do KDE Plasma e manipulação de um editor de HUD do Free Fire. Goodnotes, Freenotes e Starnote são referências comparativas, não especificações obrigatórias.
3. **Editor HUD dedicado:** fundo que lembra nota infinita somente como cenário. **Sem navegação, desenho, zoom ou interação direta com a nota.** Apenas barras e seus componentes são editáveis.
4. **Edição contextual:** selecionar o container mostra propriedades da barra; selecionar ferramenta/grupo/grid mostra apenas propriedades daquele objeto. Catálogo em painel separado, recolhível.
5. **WYSIWYG real:** preview de barra + preview de posicionamento devem compartilhar o resolvedor e renderer das barras reais. Não manter cópia simplificada ou mentirosa.
6. **Múltiplas barras:** inclusive barra principal configurável e reposicionável; duplicação, ocultação e exclusão segura de secundárias.
7. **Gestos na nota:** long press no container sem mover abre menu; long press seguido de arraste move barras desbloqueadas. Menu oferece entrada direta para configurar aquela barra. Sem resize na nota.
8. **Layout encaixável:** ferramentas e grupos não se posicionam em X/Y livres; ocupam slots e são redistribuídos pelo motor conforme padding, gap e dimensões.
9. **Composição heterogênea:** a barra da imagem, com um botão grande, paleta 4×2, separador, grid 2×2 e dois botões altos, é teste obrigatório. Suportar spans, subcontainers e tamanhos diferentes.
10. **Duplicatas de ferramentas:** várias instâncias do mesmo tipo com IDs distintos; duplicar não compartilha identidade ou altera outra instância acidentalmente.
11. **Grupos como containers:** row, column ou grid; arrastar o grupo move seu conteúdo junto, mantendo ordem/medidas. Podem existir grupos aninhados dentro de limites seguros.
12. **Edição visual direta:** adicionar/remover/reorganizar por catálogo, botões `+`, menus e drag/drop. Sem setas como mecanismo principal e sem listas redundantes.
13. **Aparência completa:** presets Compact/Regular/Comfortable/Auto, width/height, padding/gap, ícones, paleta, cor/fundo/blur, borda, cor da borda e round corners; efeitos pesados condicionados a performance.
14. **Posicionamento:** nove âncoras para barras fixas/flutuantes; snap visual durante arraste; configurações de margens. Posição livre absoluta não é necessária para a primeira versão.
15. **Perfis:** xnote e xcanvas podem compartilhar ou possuir configurações independentes. Ajuste persistente por nota individual fica para estudo futuro.
16. **Qualidade:** não avançar com UI confusa apenas porque compilou; validar screenshots, toque/stylus, legibilidade, animações, persistência e performance em cada fase.
17. **Versionamento:** nova worktree limpa em branch separada baseada na master upstream; commits focados; nenhum PR automático.

### Decisões de projeto ainda ajustáveis

- Intervalos exatos de padding, gap, dimensões mínimas e limites de grids: calibrar empiricamente em tablet e telefone.
- Disponibilidade e custo do blur no Android: oferecer fallback sólido/translúcido quando necessário.
- Posição livre avançada para barras flutuantes: fora do MVP, reconsiderar depois de validar ancoragem.
- Caneta unificada que incorpora estilos e outras canetas: realizar apenas após verificar semântica e APIs das ferramentas existentes.
- Regras de conversão entre grupo e grid: permitir quando forem reversíveis e sem perda de conteúdo.
- Comportamento ao faltar espaço durante inserção: impedir mudança destrutiva, oferecer feedback claro e opção de aumentar o grid.

### Regras explicitamente rejeitadas

- Editor de listas intermináveis com setinhas como interface principal.
- Preview que não reflita tamanho/organização das barras reais.
- Fundo do editor funcionando como uma nota navegável/editável.
- Toolbars em que qualquer ferramenta exige coordenadas manuais para posicionamento.
- Redimensionamento de barra via gesto dentro da nota real.
- Criar funcionalidades cosméticas que não alterem o app.
- Recomeçar usando `git reset --hard` ou sobrescrevendo a branch antiga.

## 13. Prompt de execução para o Codex

Cole isto ao iniciar uma sessão no diretório do projeto:

> Você é responsável por implementar a V2 do editor de toolbars do XNotes. Leia **integralmente** `XNotes-Toolbars-V2-Guia.md` e o `AGENTS.md` do repositório, inspecione o Git e o código atual. A versão antiga possui alterações não commitadas: preserve tudo, não use comandos destrutivos e estabeleça uma nova branch/worktree limpa baseada em `upstream/master`. Implemente em ordem as fases M0–M8. Priorize primeiro um editor de HUD visual, com fundo estático não interativo e painéis contextuais, e valide a experiência em capturas reais do AVD antes de adicionar mais recursos. O motor deve suportar containers, grids com spans, encaixe automático, ferramentas repetidas e renderização compartilhada entre editor e nota. Faça pequenos commits Conventional Commits e mantenha `docs/toolbar-task-state.md` atualizado com testes, erros, decisões e próxima ação. Continue iterando enquanto houver tempo e ferramentas, sem prometer conclusão sem evidência. **É terminantemente proibido criar, modificar, comentar ou fazer merge de pull requests.**

Se a sessão encerrar, o prompt de retomada é:

> Leia `AGENTS.md`, `XNotes-Toolbars-V2-Guia.md` e `docs/toolbar-task-state.md`; confira `git status` e os commits mais recentes; continue exatamente do último checkpoint e da próxima fase pendente. Preserve trabalho existente, compile/teste/inspecione por ADB e registre o próximo checkpoint. Não crie PRs.
