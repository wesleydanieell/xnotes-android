# Portar a correção de seleção e a UX de barras em split para a master

> Atualização de 2026-11-09: o padrão passou a ser workspace compartilhado entre
> os painéis, com opção por painel. Consulte [o port community](COMMUNITY_SHARED_SPLIT_TOOLBARS.md)
> para os contratos atuais; as correções anteriores de seleção e navegação continuam válidas.

Registro: 2026-11-07. Este guia registra contratos, causas e validação, não uma
instrução para fazer merge de toda a community na master. O upstream poderá
incorporar partes das features por outros commits ou refatorar os mesmos hosts.

## Escopo e referências de UX

O usuário relatou, nos dois community, que os traços sobre PDF eram selecionados,
mas a lixeira da seleção às vezes não apagava. Também pediu uma interação mais
previsível entre tela dividida e barras personalizadas, em xnote e xcanvas.

A organização escolhida mantém ferramentas por painel e navegação de documentos
única. Painéis pequenos adaptam a apresentação das barras, com opção para manter
o tamanho configurado. Barras que não cabem continuam disponíveis em um menu.
Não mover barras automaticamente para o painel ativo: isso poderia trocar o alvo
ou o layout durante uma escrita. Não recolocar tabs como elemento de toolbar.

Referências primárias consultadas:

- [Goodnotes: organização de navegação, ferramentas, overflow e menus contextuais](https://support.goodnotes.com/hc/en-us/articles/13682253498767-Improved-User-Interface).
- [Goodnotes: documentos em janelas distintas e rolagem independente](https://support.goodnotes.com/hc/en-us/articles/7353757107215-Open-Goodnotes-in-multiple-windows-on-iPadOS).
  Esse recurso documentado é de iPadOS; não implica uma API equivalente no Android.
- [Concepts: ferramentas configuráveis e reorganização do workspace](https://concepts.app/en/manual/workspace).

A política de adaptação abaixo é uma decisão deste projeto inspirada nesses
padrões; não é uma afirmação de que esses apps usam os mesmos limites ou algoritmo.

## Causa reproduzida da lixeira

O menu era um Row Compose sobre o canvas nativo. Os retângulos de toolbars eram
excluídos do encaminhamento de caneta, mas o menu de seleção não era registrado.
Depois do dispatch Compose de DOWN, `SplitStylusRouter.dispatchFallback` podia
encaminhar o mesmo evento para a superfície nativa. O desenho interpretava esse
DOWN como um toque no canvas e desfazia a seleção antes do UP/click da lixeira.
A operação `deleteSelection()` nem chegava a ser chamada.

O teste `SelectionMenuStylusTest` reproduziu isso antes da correção: dedo passou;
os dois casos com caneta falharam com `Delete must execute once ... was 0`.
Depois de proteger a superfície real do menu, os três passaram. Isso é uma falha
de roteamento/UI, não uma razão para substituir o algoritmo de apagar traços.

## Bloco independente: ações de seleção

Caminhos relativos a `app/src/main/java/com/xnotes/`:

- `ui/CanvasMenus.kt`: preserva ações e peculiaridades de cada fork; o conteúdo
  da seleção passa pelo host compartilhado `CanvasSelectionActionBar`.
- `ui/CanvasSelectionActionBar.kt`: mede a superfície real, permite rolagem quando
  as ações excedem a largura, mantém a camada acima das barras dentro do painel
  e usa `ToolbarHitArea` para excluir apenas o menu do roteamento nativo.
- `ui/CanvasActionBarGeometry.kt`: posiciona em coordenadas do viewport do painel,
  limita às bordas, tenta evitar seleção e obstáculos medidos (ferramentas/abas),
  e usa uma posição limitada à janela quando não existe espaço livre suficiente.

Não registrar o Box transparente de tela inteira como área de ferramenta: isso
impediria desenhar no resto do canvas. Não incluir o menu na lista global de
obstáculos das abas, ou a navegação pode começar a fugir da seleção. A exclusão
local de input e os obstáculos de layout global são papéis distintos.

Em `canvas/InteractionController.kt`, `selectAll()` também publica o menu após
atualizar a seleção. Antes, selecionar tudo alterava o modelo mas podia deixar
as ações ausentes; o teste com PDF real detectou isso. Se a master já chamar
`refreshSelectionMenu()` nessa operação, não duplicar a chamada.

Esse bloco serve a xnote e xcanvas. No n-notes, preservar comandos extras de
seleção (texto, matemática, funções, links, tamanho, etc.). Não sobrescrever
CanvasMenus inteiro com a versão de outro fork.

## Bloco de UX: barras personalizadas em painéis

- `ui/SplitToolbarPresentation.kt`: cria uma apresentação temporária quando a
  opção está ativa e a menor dimensão do painel está abaixo de 600 dp. Usa o
  preset compacto, limita controles individuais a 36 dp, espaçamento/padding e
  dimensões manuais ao painel. Não modifica o workspace salvo. Um painel largo
  pode manter o tamanho configurado enquanto o vizinho estreito compacta.
- `settings/Preferences.kt`: `adaptiveSplitToolbars`, padrão true, chave JSON
  `adaptive_split_toolbars`. A configuração desativada persiste explicitamente;
  configurações antigas assumem a adaptação. Não guardar isso no documento.
- `ui/ToolbarPreferencesPane.kt`: opção em Barras para adaptar painéis pequenos.
- `MainActivity.kt` / `EditorPane`: usa constraints reais de cada painel, fornece
  a apresentação ao renderizador de notas/canvas e às barras primária/secundárias.
  Passar o layout adaptado também à barra primária; mudar só as secundárias deixa
  os botões personalizados grandes na principal. Preserve reader/split do n-notes.
- `ui/ToolbarDockWorkspace.kt`: `fitToViewport` é opt-in. O runtime pode limitar
  o comprimento manual à borda disponível; validadores do configurador continuam
  estritos. Não tornar a validação global permissiva para esconder overflow.
- `ui/ToolbarFrame.kt`: fornece a lista de barras docked rejeitadas ao conteúdo.
- `ui/WorkspaceFloatingBars.kt`: reúne rejeitadas docked/floating e mostra recuperação.
- `ui/ToolbarOverflowMenu.kt`: “Outras barras” dá acesso às ferramentas que não
  cabem. Não as apaga, oculta no preset ou salva posições de fallback. O popup usa
  o editor do painel correspondente. Como é outra janela, suas áreas não devem
  ser registradas como exclusões no canvas da janela principal.

A alça de mover painel também passa por hit-area protegida. Seu ponto inicial é
medido na composição real; não reconstruir a posição só a partir da toolbar salva,
pois a apresentação do painel pode estar compactada. O foco continua sendo
transferido depois do último pointer-up, nunca no meio de um traço. A indicação
visual de painel ativo foi reforçada sem mudar suas dimensões ao trocar foco.

## Coordenadas e compatibilidade com refactors

- A seleção está em pixels locais do viewport nativo; use o host desse viewport.
- Toolbars/abas são medidas em `boundsInWindow`; subtraia a origem do host para
  obter obstáculos locais. O fallback Android compara áreas em coordenadas de tela.
- Constraints de um painel não são `LocalConfiguration.screenWidthDp` da janela
  inteira. Use a largura/altura efetivamente atribuídas após barras docked/sidebar.
- Garanta remoção das áreas quando o menu/barra sai da composição, evitando zonas
  fantasmas que continuam bloqueando a caneta.
- Mantenha IDs estáveis e a separação entre apresentação temporária e persistência.
  Redimensionar o divisor ou trocar foco não é uma edição do preset do usuário.

## Verificação obrigatória antes de portar para outra master

1. Em checkout isolado, procure `SelectionMenuHost`, `SelectionMenu`,
   `SplitStylusRouter`, `forwardStylusToNativeCanvas`, `ToolbarHitAreas`,
   `ToolbarAround`, `toolbarDockPlan` e `WorkspaceFloatingBars`.
2. Consulte conteúdo e histórico (`git log -S ... -- caminho`), não apenas se um
   commit/branch está merged. Uma feature pode ter sido removida após o merge.
3. Reproduza a falha nessa master. Se o menu já estiver protegido ou não houver
   o fallback nativo que causa o problema, não introduza o router community só
   para aplicar um patch. Reaproveite o mecanismo de exclusão que já existir.
4. Porte primeiro a correção mínima do menu e seus testes. Se não houver abas ou
   workspace global nessa master, adapte a fonte de obstáculos ao host existente;
   não force dependências das features de navegação para corrigir uma lixeira.
5. Só porte adaptação/overflow quando o modelo de múltiplas barras e o host de
   split compatíveis já estiverem presentes. Caso já existam implementações
   equivalentes, ajuste o contrato delas; não crie um segundo host concorrente.
6. Resolva conflitos preservando interfaces e callbacks da master atual. Nunca
   substitua MainActivity ou a tela de preferências inteira por uma community.

## Testes e comandos

Os testes de UI são em pacotes debug isolados, com documentos sintéticos.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --no-parallel --max-workers=1
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# XNotes
adb -s SERIAL shell am instrument -w -r -e class com.xnotes.ui.SelectionMenuStylusTest,com.xnotes.ui.PdfSelectionDeleteTest,com.xnotes.ui.SplitCustomToolbarTest com.xnotes.debug.test/android.test.InstrumentationTestRunner
# n-notes: mesmo namespace de classes, outro application ID/runner
adb -s SERIAL shell am instrument -w -r -e class com.xnotes.ui.SelectionMenuStylusTest,com.xnotes.ui.PdfSelectionDeleteTest,com.xnotes.ui.SplitCustomToolbarTest com.neddy.nnotes.debug.test/android.test.InstrumentationTestRunner
./gradlew :app:assembleCommunity --no-parallel --max-workers=1
```

- `SelectionMenuStylusTest`: dispatch real da Activity e menu real, superfície
  sintética que revela DOWN indevido no desenho. Dedo, caneta e painel estreito.
- `PdfSelectionDeleteTest`: PDF real gerado em cache, importado; traço sobre esse
  PDF apagado pela lixeira com evento de caneta, undo/redo e bytes do PDF intactos.
  O xnote do teste também fica no cache, sem autosave numa pasta de notas pessoais.
- `SplitCustomToolbarTest`: host real da Activity, xnote/xnote, xcanvas/xcanvas,
  combinação mista com troca de posições, proporções 25/50/75% e recuperação de
  barras rejeitadas. Controles compactam apenas onde o painel requer isso.
- `CanvasActionBarGeometryTest`: bordas, seleção perto do fim, obstáculos e janelas pequenas.
- `SelectionCacheRepairTest`: apagar ink em página PDF, fundo mantido e undo/redo.
- `SplitToolbarPresentationTest`: geometria por eixo, presets intactos, opção
  persistida e diferença entre fitting de runtime e validação estrita.

As cópias de testes usam o mesmo namespace `com.xnotes`, mas seus APKs/IDs são
diferentes. Confirme o runner correto. Faça asserts fora de runOnMainSync para
que a falha seja reportada em vez de derrubar a main thread. Modais do sistema,
incluindo avisos de compatibilidade 16 KB no emulador, podem impedir testes de
input destinados à janela da Activity; não confunda isso com falha do modelo.

Ainda testar manualmente com caneta física: latência, popup/IME, bordas reservadas
a gestos do Android, rotação, mais de duas interações simultâneas e workspaces
extremos. Não prometer que esta matriz cobre toda configuração possível.

Atualizações no tablet usam `adb install -r` com o mesmo certificado e o serial
explícito. Não desinstalar/limpar dados para contornar assinatura. Não publicar
PR, tag, release ou reescrever branches originais como parte de um port local.

## Commits locais de implementação

Os commits estão nas branches community locais; não foram publicados como PR.
Se a master já tiver partes equivalentes, usar os diffs como referência em vez
de aplicar tudo cegamente. A correção da seleção é separada da adaptação de split.

| Fork | Seleção/caneta | Adaptação de split |
| --- | --- | --- |
| XNotes | `f0593800f55ac4866fd7e7c30c587e551d297438` | `8322324a05dcefc6ac57d6feb16ebd50b4e3fd90` |
| n-notes | `0e9c650cfab3692ad1a7fe7b4b0dceda2be4108b` | `09bbd4b294fe5903cf5af393064531a6847e799e` |

O fixture do menu de overflow também deve esperar a janela do popup aparecer e
marcar MotionEvent como SOURCE_TOUCHSCREEN. Uma checagem imediatamente após UP
pode ler a janela anterior no Redmi. Usar a versão mais recente dos testes ao
portar (`git log -- app/src/androidTest/java/com/xnotes/ui/SplitCustomToolbarTest.kt`).

Commits de sincronização do teste de overflow (aplicar junto à feature quando portar os testes):

- XNotes: `202aa4f21309d280bc609630582ea5a2f8f8b219`.
- n-notes: `ef5b901c8599d75882f5cc191ed4caf63d86149d`.

## Resultado final desta integração

- XNotes: 2.637 testes JVM, zero falhas/erros; os oito testes de interface deste
  guia passaram no emulador e no Redmi Pad 2.
- n-notes: 2.742 testes JVM, zero falhas/erros; os oito testes de interface passaram
  no emulador usando o runner `com.neddy.nnotes.debug.test`.
- O teste com PDF real confirmou exclusão da tinta pela lixeira com evento de
  caneta, undo/redo e preservação dos bytes do PDF nos dois forks.
- APKs community otimizados foram empacotados e o lint vital foi concluído
  separadamente, sem falhas. Os dois apps foram atualizados no Redmi por
  `adb install -r`; os hashes dos APKs instalados coincidiram com os locais e
  os dois lançaram sem FATAL EXCEPTION no smoke test.
- O teste de overflow passou após aguardar a janela do popup e especificar o
  source do toque; a primeira checagem imediata no Redmi tinha falhado.

Os testes com MotionEvents verificam o roteamento do app; não substituem QA de
latência da caneta física. As configurações e notas community não foram usadas
como documentos de teste. Os PDFs/xnotes sintéticos ficam no cache debug.

Este guia também foi copiado para a branch histórica da integração de tabs como
elemento. Nela, os helpers descritos pertencem aos commits community da tabela,
não ao código histórico do protótipo. A branch original das toolbars foi preservada.
