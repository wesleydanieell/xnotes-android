# Integração de barras compartilhadas nos community

Registro: 2026-11-09. Este documento complementa
[SHARED_SPLIT_TOOLBARS.md](SHARED_SPLIT_TOOLBARS.md) e substitui, quanto ao modo
padrão de organização, a decisão antiga de manter barras duplicadas por painel.
As correções de seleção, roteamento de caneta e modo por painel continuam válidas.

## Base independente e escopo

A implementação independente está em `feat/shared-split-toolbars`, commit
`115571bb2f5f76c70927c2ed740097f24883afb9`, diretamente sobre upstream `3926304a`
(0.9.7). Não foi feito merge de toda a master, de outros PRs ou de toda uma
community na outra. O port adapta os contratos aos dois forks:
`community/xnotes` e `community/n-notes`.

O comportamento padrão é uma única instância do workspace de ferramentas para os
dois documentos. Isso significa todas as barras configuradas, não apenas a barra
primária. O usuário pode optar por barras por painel. Em split misto, escolhe o
workspace xnote ou xcanvas; trocar o painel ativo não troca esse layout. A opção
existente de vincular os perfis xnote/xcanvas (`ToolbarWorkspaces.shared`) mantém
seu significado: ela não é a nova preferência de compartilhamento entre painéis.

As abas continuam na barra independente, floating ou docked, com dimensionamento
e animações existentes. Não voltam a ser elementos de uma toolbar. Cada documento
mantém seleção, zoom, rolagem, histórico, PDF, edição de texto e autosave próprios.

## Onde foi necessário adaptar

### Workspace fora dos painéis

`SharedCommunityWorkspace`, em `MainActivity`, recebe o host de split existente
como conteúdo. Resolve o perfil uma vez e aplica `ToolbarAround` e
`WorkspaceFloatingBars` ao workspace inteiro. Usa os mesmos planejadores,
geometria, ordem, lanes, distribuição, controles individuais e overflow que o
configurador e o modo por painel já utilizam.

`PaneToolbarsAround` preserva o caminho antigo quando o usuário escolhe barras por
painel. No modo compartilhado, o painel fornece somente o canvas, sidebar, menus e
overlays. Os antigos renderizadores locais não são desenhados novamente.

`SharedCommunityBar` renderiza o layout escolhido contra o editor realmente ativo.
Não encaminha comandos de página ao xnote oculto quando um xcanvas está ativo.
Itens exclusivos do outro formato ficam desabilitados e expõem esse estado à
acessibilidade. Atalhos e pickers continuam com os contratos de alvo existentes.

`SharedToolbarFrame` mantém scroll e animação por barra fora da chave do editor
ativo. Somente o conteúdo/popups nativos são reiniciados ao trocar de documento.
Não envolver esse conteúdo com um `Box` dentro do frame: ao suprimir o frame
nativo, isso pode sobrepor as células em vez de distribuí-las.

### Primeiro DOWN sem trocar foco durante o traço

Os community já adiam o foco até o release nativo para evitar cancelamento de
traços/recriação de toolbars. Esse contrato foi preservado, em vez de copiar o
foco imediato da base independente.

Cada superfície expõe `beforeSurfaceDown`, chamado antes de `super.dispatchTouchEvent`.
O `EditorPane` configura esse callback somente enquanto usa ferramentas
compartilhadas. Assim, o primeiro DOWN na outra superfície já recebe a ferramenta,
cor, configurações de traço e shape compartilhadas, mesmo nos caminhos de interop
ou fallback do stylus.

`PaneFocusDelivery` continua ativando o painel depois do dispatch de UP. Usa
`focusPane(..., adoptSharedTools = false)` para não copiar novamente a ferramenta
de saída e desfazer, por exemplo, um retorno automático da borracha para a caneta.
Cliques nos títulos continuam usando a ativação normal.

Não substituir a cor com `pickColor`, que pode recolorir seleção/texto e produzir
histórico apenas por mudar o foco. `adoptSharedInk` altera somente a tinta futura.
Uma seleção ou sessão de texto existente conserva sua ferramenta ao ser ativada,
para não invalidar um menu contextual antes de a ação tocada executar.

### Hit areas e covers por superfície

`SharedCommunityToolbarState` mantém hit areas compartilhadas e os retângulos
medidos de cada barra floating em coordenadas **da janela**. Todos os painéis os
somam às suas exclusões locais e às da navegação, tanto em `forwardStylusToNativeCanvas`
quanto em `SplitStylusRouter`. Não remover as proteções anteriores de seleção e
caneta: tocar uma barra que fica sobre o outro documento não deve desenhar nele.

`MeasuredSharedToolbar` mede cada barra visível. O overflow já apresenta controles
como docked em outra janela, com hit areas locais/globais desabilitadas; esse
popup não acrescenta covers nem reflui o canvas. `insets` intersecta cada retângulo
floating com o viewport nativo depois de
sidebar/docking. Combina covers pelo máximo por borda, não pela soma. Uma barra
floating à direita não pode reduzir a área útil da superfície à esquerda; duas
lanes no topo não devem somar a distância à origem duas vezes.

Barras docked já reduzem o viewport de conteúdo pelo planejador. Seus retângulos
não acrescentam insets. Os covers somem quando a barra sai ou vira docked. A alça
de reposicionamento do painel usa a cobertura floating efetiva no topo.

### Preferências e recursos de cada fork

- `prefs.shared_split_toolbars`: true por padrão; false mantém ferramentas por painel.
- `prefs.mixed_split_toolbar`: `note` por padrão ou `canvas`. Persistido mesmo com
  ferramentas independentes, sem reescrever os perfis salvos.
- Aba própria **Tela dividida** no configurador (`ToolbarPropertyTab.SPLIT_VIEW`).
  Também contém a opção existente de compactar barras em espaços pequenos.
- Ajustes dentro da divisão ficam na própria barra de abas, sem outra faixa
  obrigatória. Em split misto compartilhado, o botão indica xnote/xcanvas e abre
  os mesmos cartões visuais. Nas barras laterais usa um botão compacto. O espaço
  reservado ao switcher desconta o tamanho medido do controle adicional.
- O host de navegação em runtime consome o inset do IME antes de distribuir abas,
  toolkit e documentos. As barras inferiores e o formatador de texto ficam acima
  do teclado; os `imePadding` descendentes recebem o inset já consumido.
- As zonas de drop de tabs usam os bounds medidos de `SplitHost` depois do docking
  compartilhado, não o retângulo anterior que ainda inclui barras docked. O host de
  navegação preserva esses bounds fornecidos pelo app; o preview mantém seu fallback local.
- `DocumentTabsState.splitControls` é um slot opcional, preenchido pelo app somente
  durante um split. A barra e o preview usam a mesma superfície. Fechar e ativar
  documentos continua sendo responsabilidade das tabs/Back, sem duplicar títulos
  e botões de fechar num cabeçalho obrigatório.
- No modo compartilhado, a adaptação usa o viewport do workspace inteiro; no modo
  por painel continua usando cada painel. Nenhuma adaptação grava novos presets.

O novo compartilhamento se aplica aos dois editores de notas/canvas. No n-notes,
`ReaderScreen` ao lado de uma nota ou cobrindo as duas posições mantém o host e
seus controles anteriores; não é tratado como um segundo xcanvas/xnote.

O port de n-notes conserva seus menus de reader/PDF, links, math e caixas de texto.
`MathKeyboardPanel` e `TextBoxFormatBar` acompanham o editor ativo no modo
compartilhado; continuam no caminho local no modo por painel. Não copiar toda a
`MainActivity` ou todo o `Editor` entre os forks para atualizar esta feature.

## Validação e atualização

Os testes da base independente foram adaptados aos perfis multi-bar e a pacotes
isolados terminados em `.sharedsplitcommunity`. O script
`tools/shared-split-validation.gradle` só muda o ID de debug; os IDs community e
suas assinaturas continuam os mesmos. `-PsharedSplitAbi=arm64-v8a` limita o build
local ao Redmi, sem modificar o padrão de ABIs do repositório.

A suíte cobre modo compartilhado/por painel, xnote/xnote, xcanvas/xcanvas, mistos
nas duas ordens, escolha persistente, quatro bordas, floating/docked, ratios,
rotação, fechamento, seleção e histórico. Casos adicionais exercitam um workspace
com barras docked em cima/baixo e floating lateral, troca dos painéis, preservação
dos perfis e exclusão de DOWN da caneta em ambos os documentos.

`SplitCustomToolbarTest` continua verificando o modo por painel explicitamente.
Os testes anteriores de PDF, menus da seleção e animações das abas permanecem na
validação. No pacote isolado do n-notes a checagem remota de atualização é
desativada antes de `ready`, para que um modal de versão não cubra a UI do teste;
a preferência do pacote community do usuário não é alterada. Fixtures aguardam
`sessionLoaded` antes de criar documentos e aguardam o fechamento/flush antes de
restaurar preferências, evitando que o restore assíncrono substitua uma fixture.
Antes de rodar a suíte, acordar/desbloquear o tablet e evitar o timeout da tela.
Capturas de falha identificaram keyguard no lugar do app durante testes longos.
Se usar `svc power stayon usb` na bancada, salvar e restaurar o valor original de
`stay_on_while_plugged_in` ao terminar; não mudar a preferência do app do usuário.
A trava dos testes anteriores aceita debug ou o novo pacote isolado; a nova suíte
exige o pacote isolado. Não executar fixtures sobre o community do usuário.
O teste de teclado encerra a sessão e oculta o IME; cada fixture arma PEN para não
herdar TEXT. Ao medir um botão após mover a mesma barra entre pais docked/floating,
limpar o cache de acessibilidade (API 33+): o Android pode devolver o retângulo da
posição anterior, fazendo o teste tocar o canvas apesar de o botão estar visível.

Para o build otimizado usa-se `:app:assembleCommunity`. A instalação é uma
atualização com `adb install -r`, mantendo ID, certificado e dados. Validar o
certificado contra o APK atualmente instalado antes de atualizar; nunca
substituir uma incompatibilidade de assinatura por desinstalação.

### Registro desta integração (2026-11-09)

- XNotes: 2.649 testes JVM, sem falhas ou testes ignorados.
- n-notes: 2.754 testes JVM, sem falhas ou testes ignorados.
- Redmi Pad 2: 46 testes de dispositivo por app, todos aprovados (92 no total),
  incluindo 22 casos de animação das abas por app e exclusão em PDF.
- `assembleCommunity` concluído nos dois forks, incluindo `lintVitalCommunity`.
- Os dois community foram atualizados no Redmi por `adb install -r`, abertos com
  sucesso e seus APKs instalados conferidos por SHA-256 contra os artefatos preparados.
  Mantidos applicationId, certificado e dados; nenhuma desinstalação.
- O ajuste temporário de manter a tela acesa por USB foi restaurado para `0`.
- A branch base `feat/shared-split-toolbars` permanece independente. Este port
  fica em `community/xnotes` e `community/n-notes`, no fork de wesleydanieell;
  não enviar o port diretamente ao `origin` do n-notes (repositório de NeDDy3z).

## Futuras integrações

Se o upstream incorporar as barras configuráveis antes deste port, verificar os
contratos de renderização, hit areas, covers, foco e estado atual dos arquivos,
não somente se um commit já aparece no histórico. Os dois guias antigos continuam
úteis para entender os bugs de seleção, mas o padrão agora é compartilhado.
Aplicar o adaptador somente aos blocos ainda necessários, preservando recursos
novos do upstream e do fork. A branch independente continua sem dependência deste
adaptador community.

## Correção: comprimento das barras, abas adjacentes e puxadores

O workspace compartilhado usa as dimensões e distribuição normais do perfil,
sem passar por `splitToolbarPresentation`: a compactação adaptativa destina-se
aos painéis individuais. A área compartilhada pode diminuir com abas docked ou
IME; isso não deve trocar automaticamente os tamanhos dos controles ou comprimentos
salvos. Os limites reais da janela e o scroll da barra continuam sendo respeitados.

O registro global `LocalGlobalToolHitAreas` é usado pelo host das abas para
calcular a faixa livre. Os três pontinhos de cada painel estavam entrando nesse
registro como se fossem barras de ferramentas. Assim, sua posição alterava o
comprimento/posicionamento das abas; reposicionar as abas também podia exigir mover
o puxador, formando uma dependência circular de layout. `ToolbarHitArea` agora
permite `toolObstacle = false`: o puxador continua excluído do roteamento nativo
da caneta no painel, mas não reserva espaço como uma toolbar.

`paneGripClearance` calcula a posição vertical do puxador a partir dos retângulos
reais das ferramentas e da barra de abas, em coordenadas da janela. Só considera
obstáculos que cruzam a largura do puxador; percorre linhas sobrepostas sem depender
da ordem do registro. Barras docked fora da área do painel e barras do outro painel
não acrescentam um inset fictício. O preview de arraste usa a mesma folga medida.

Regressões adicionais alternam shared/per-pane nos pares xnote/xnote,
xcanvas/xcanvas e mistos, com abas expandidas flutuantes junto das barras, nas
quatro bordas e nos modos floating/docked. Conferir que existe uma toolbar em cada
lado, que suas ferramentas estão dentro da janela e que abas/puxadores não as
cobrem. O teste com IME também compara largura e distância entre controles antes
e depois de abrir o teclado. Quatro testes JVM cobrem a folga dos puxadores.

Validação JVM desta correção: 2.653 testes no XNotes e 2.758 no n-notes,
sem falhas. `assembleCommunity` passou nos dois, incluindo a verificação vital
do lint e otimização R8. Os casos de dispositivo acima são executados em pacotes
isolados; os arquivos do usuário não são usados como fixtures.

Redmi Pad 2: 49 testes de dispositivo aprovados em cada fork (98 no total),
incluindo as animações das abas, exclusão em PDF e regressões de barras por painel.
Os dois APKs corrigidos foram instalados por atualização no Redmi e abertos com
sucesso. Os hashes dos APKs instalados coincidem com os artefatos assinados.
Restaurado para `0` o ajuste temporário de manter a tela acesa por USB.

## Correção: arraste, escolhas atuais e isolamento por painel

`ToolbarDragSurface` tem um detector `pointerInput(id, anchor, paneBounds)`. Esses
parâmetros podem permanecer iguais quando o usuário troca xnote/xcanvas ou o modo
da divisão. O detector antes conservava o callback da composição inicial. Agora
`rememberUpdatedState(onMoveBar)` entrega o callback atual no fim do gesto.
O movimento da navegação também faz um patch sobre `editor.preferences` no momento
da ação; nunca copiar `tabsPreferences` capturado para salvar uma nova âncora.
Isso evita restaurar `mixedSplitToolbar`/`sharedSplitToolbars` antigos ao arrastar.

A política de mudança de geometria foi extraída para `movedToolbarBar`. No modo
compartilhado/single, `moveToolbarBar` continua persistindo o perfil apropriado.
No modo por painel, `movePaneToolbarBar` muda apenas `paneToolbarWorkspaces` e
`paneToolbarLook` do editor daquela visualização. São posições da sessão da
divisão: não são gravadas por cima do perfil normal/compartilhado. Fechar a divisão
limpa os overrides; editar explicitamente os perfis também os invalida. Alterar o
foco ou mover a outra toolbar não deve mexer nesses overrides.

Cada painel é recortado pelos próprios limites e fornece seus próprios obstáculos
para o overflow de ferramentas. Continua publicando seus retângulos no registro
global, pois a navegação e os puxadores precisam enxergar as posições reais, mas
não usa as ferramentas do outro painel para escolher sua própria posição.

No modo por painel há uma única barra de abas docked em cima, fora e acima das
duas visualizações. `DocumentTabsState.dockAboveSplit` aplica essa apresentação no
host e no preview, retira a alça e desativa o arraste. A configuração de abas salva
não é sobrescrita; voltar ao modo compartilhado restaura a posição anterior.
ReaderScreen do n-notes mantém a exceção anterior.

Regressões usam arrastes reais com long press: mudança da interface antes de mover
a toolbar compartilhada, mudança de preferências antes de mover as abas e arraste
separado nas duas visualizações com dois xnote ou dois xcanvas. Verificar os
retângulos da outra toolbar, os perfis salvos e as escolhas de compartilhamento e
interface antes/depois. Outro teste verifica a linha superior acima dos dois
viewports e a restauração da posição floating ao voltar ao modo compartilhado.

Validação desta etapa: 25 testes da suíte de tela dividida em cada app, todos
aprovados no Redmi (50 no total); 2.653 testes JVM no XNotes e 2.758 no n-notes,
sem falhas. Builds `assembleCommunity`, R8 e lint vital concluídos nos dois forks.
Atualização instalada nos dois community por `adb install -r`, mantendo os dados
e o certificado. Abertura dos dois apps concluída e APKs instalados conferidos por
SHA-256. Restaurado o ajuste temporário de tela acesa por USB para `0`.
