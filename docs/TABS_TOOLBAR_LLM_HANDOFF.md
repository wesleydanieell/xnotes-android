# Integração de abas e toolbars: contexto para uma futura LLM

Registro de 2026-11-07. Leia este documento antes de modificar a integração.

## Decisão vigente do usuário

As abas são navegação do aplicativo, em uma barra própria. Existem apenas os
modos **floating** e **docked**. O usuário pode reposicionar essa barra, inclusive
nas laterais, e vê-la no preview do configurador. **Não pode inserir tabs numa
section ou acoplá-las como elemento a uma toolbar de ferramentas.** Essa última
possibilidade existiu num protótipo e foi explicitamente abandonada pelo usuário.
Preservar animações de seleção, cores e expansão/recolhimento, fechamento,
rolagem, reordenação por toque longo e arraste para split.

## Mapa de branches e commits

Os caminhos abaixo são os worktrees desta máquina; confirme-os com `git worktree list`.

| Branch | Worktree | Papel |
| --- | --- | --- |
| `feat/customizable-toolbars` | `../xnotes-customizable-toolbars` | Fonte original das toolbars, preservada em `5e786579` |
| `feat/toolbar-restart` | `../xnotes-android-toolbars` | Outro experimento; não era a implementação pedida |
| `feat/document-tabs-toolbar-element` | `../xnotes-document-tabs-toolbar-element` | Protótipo histórico com tabs como elemento, código em `c198c064` |
| `feat/standalone-document-tabs-bar` | consultar worktrees/refs | Solução atual compartilhável, implementação em `3df8faed` |
| `community/xnotes` | `../xnotes-community` | XNotes community, inclui a solução atual |
| `community/n-notes` | `../n-notes-community` | n-notes community, implementação atual em `42d34068` |

Este documento também foi colocado na branch histórica do elemento, **sem
substituir seu código pelo desenho atual**. Nela, os arquivos da solução atual
citados abaixo não estarão necessariamente presentes: consulte `3df8faed`.
Não reescreva a branch original das toolbars para portar correções.

Bases upstream usadas nesta integração: XNotes `3926304a` (0.9.7) e n-notes
`3f7d7c2f` (0.9.7-0.53). Isso registra a base integrada, não afirma que continuará
sendo o upstream mais recente. Investigue atualizações futuras separadamente.

## Por que um merge simples não bastou

O histórico community já continha o merge do protótipo de toolbars, mas parte
visível da implementação havia sido removida depois. Git considerar uma branch
merged não significava que sua funcionalidade ainda existia. Foi necessário
reaplicar as mudanças da feature original em relação à base `26f42b29`, conciliando
as funcionalidades community: tabs, split view, drag/drop externo, carregamento
incremental e otimizações de texto. Veja também `COMMUNITY_INTEGRATION.md`.

O configurador foi mantido em `ToolbarPreferencesPane`, separado das preferências
gerais. Substituir a tela de preferências do n-notes pela do XNotes apagaria suas
categorias, busca, opções de escrita e sincronização. Os dois MainActivity também
não são intercambiáveis: n-notes possui integração de reader e panes próprios.
Faça port por comportamento, preservando essas diferenças.

## Protótipo histórico: tabs como elemento

Em `c198c064`, `ToolbarItem.DOCUMENT_TABS` podia ser colocado em sections de
workspaces note/canvas. `ToolbarDocumentTabs` adaptava o renderer existente;
`TabBarPlacement` e a detecção de itens visíveis decidiam quando ocultar a barra
legada. O catálogo, labels, sizing, serialização e áreas excluídas da caneta
precisaram reconhecer o elemento. Laterais usavam um lançador com popup.

Os problemas apareceram nas combinações: floating/docked, topo/base/laterais,
section com múltiplas linhas, expansão que mudava o tamanho ocupado, animações
reiniciadas pela troca de renderer e preview diferente do documento real.
Esse código é referência histórica, não a especificação atual. Não restaure o
elemento apenas porque o encontrar nessa branch ou em um preset antigo.

## Arquitetura atual e pontos de manutenção

Todos os caminhos são relativos a `app/src/main/java/com/xnotes/`.

- `settings/DocumentTabsBarConfig.kt`: configuração independente de `ToolbarLook`:
  floating, posição docked e âncora. Normaliza CENTER/valores inválidos e mantém
  coerência ao mudar modo/posição. `Preferences.kt` persiste `document_tabs_bar`.
- `ui/DocumentTabsState.kt`: contrato de navegação, callbacks e área de drop.
  O conteúdo dos documentos não pertence a configurações ou presets de toolbar.
- `ui/DocumentTabsHost.kt`: usado tanto no app quanto no preview. Docked reserva
  espaço fora do workspace das ferramentas; floating mede a superfície e evita
  os retângulos reais das toolbars. Topo/base usam linha horizontal; laterais usam
  strip vertical. A alça usa `ToolbarDragSurface` e salva a âncora pelo chamador.
- `ui/DocumentTabsPlacement.kt`: posicionamento floating com limites da janela e
  busca por posição livre entre obstáculos. Não basta somar um inset de topo:
  barras podem existir simultaneamente em bordas perpendiculares e nos cantos.
  Se a janela não comportar tudo, não existe garantia matemática de não sobrepor.
- `ui/DocumentTabsSelectionState.kt`: posição do indicador e pesos de cor têm
  estado no workspace, fora do renderer de um pane. Trocar o documento/renderer
  não deve recriar esse estado e interromper a animação.
- `ui/FloatingDocumentTabs.kt` e `ui/NoteTabBar.kt`: renderers compartilhados,
  AnimatedContent/SizeTransform para recolhimento e animação de seleção/cores.
  Floating realmente reduz sua extensão. Docked horizontal conserva a largura
  da borda; docked lateral anima a largura. Não congele toda a área da barra
  para tentar corrigir colisões: isso destrói o recolhimento visível.
- `core/workspace/TabDrop.kt`: geometria orientada do strip. Reordenação lateral
  usa Y e scroll vertical; split pode partir de qualquer borda em direção ao
  documento. Use limites reais do workspace após reservar barras docked.
- `ui/ToolbarWorkspacePreview.kt`: usa o mesmo host de navegação e ToolbarFrame
  das ferramentas, com densidade escalada. Calcula o workspace restante depois
  da reserva docked. Estado local de seleção/expansão/reordenação impede o preview
  de fechar ou modificar notas reais. Não volte a desenhar uma imitação estática.
- `ui/DocumentTabsBarControls.kt` e `ToolbarPreferencesPane.kt`: apenas modo,
  posição e expansão inicial para a navegação; nenhum botão de adicionar tabs
  ao catálogo de ferramentas.
- `ui/ToolbarHitAreas.kt` e MainActivity: retângulos de ferramentas e navegação
  alimentam o encaminhamento de eventos da caneta. Excluir só a área real,
  liberando os intervalos entre barras para desenhar. Remover retângulos ao sair
  da composição; não deixar regiões fantasmas após mover/fechar uma barra.

MainActivity instancia a navegação no workspace, fornece contexto ao backstage,
envolve SplitHost no host independente e persiste reposicionamento por
`applyHomePreferences`. Preserve os callbacks de abrir/fechar/ativar/split e o
comportamento específico do reader no n-notes.

## Migração do elemento removido

`ToolbarWorkspaceJson.kt` reconhece o ID bruto `document_tabs` antes de consultar
o enum, que já não tem DOCUMENT_TABS. Remove apenas o elemento legado, compacta
as lanes ocupadas nas sections afetadas e descarta barras que só tinham tabs.
Se um perfil inteiro ficar vazio por essa migração, restaura suas ferramentas
padrão. Mantém outras ferramentas, barras, geometria manual e perfis. Uma barra
vazia que não veio desse elemento não deve ser apagada indiscriminadamente.
Não escreva configuração de toolbars nos arquivos de notas.

## Validação reproduzível

No worktree de cada community, com ANDROID_HOME configurado:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --no-parallel --max-workers=1
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# XNotes
adb -s SERIAL shell am instrument -w -r -e class com.xnotes.ui.DocumentTabsBarAnimationTest com.xnotes.debug.test/android.test.InstrumentationTestRunner
# n-notes
adb -s SERIAL shell am instrument -w -r -e class com.xnotes.ui.DocumentTabsBarAnimationTest com.neddy.nnotes.debug.test/android.test.InstrumentationTestRunner
./gradlew :app:assembleCommunity --no-parallel --max-workers=1
```

As variantes debug têm IDs diferentes: XNotes `com.xnotes.debug` e n-notes
`com.neddy.nnotes.debug`; os testes acrescentam `.test`. Confirme o package ID
no APK e use o runner correspondente. Ambos têm classes no namespace `com.xnotes`,
mas isso **não** torna seus application IDs iguais. Executar o runner XNotes
depois de instalar n-notes testa novamente XNotes. Use documentos sintéticos.

Resultados antes do tamanho dinâmico: XNotes 2.625 e n-notes 2.730 testes JVM
sem falhas. Os 21 testes iniciais de interface passaram no XNotes no Small_Tablet,
1920×1200. O registro inicial de interface do n-notes usou o runner XNotes por
engano; ele não comprovava n-notes e é substituído pela validação posterior com
o pacote correto. A bateria cobre recolhimento nos dois modos nas quatro bordas,
oito casos no preview reduzido, quatro arrastes long-press (app/preview × modos)
e seleção contínua. O tamanho dinâmico acrescenta persistência/limites, espaços
livres entre vizinhos e um teste instrumentado com barra na mesma fileira.

Uma falha inicial de arraste do teste veio das coordenadas: no preview reduzido,
o toque atingia a área de status ou não acompanhava a escala. O fixture agora
tem margem superior e calcula o toque da alça na escala correta. Faça assertions
fora de `runOnMainSync`: uma AssertionFailedError na main thread derruba o
processo em vez de ser reportada como falha normal do teste.

Ainda precisam de QA dirigido: latência da caneta física, todas as rotações,
teclado/janela pequena, mais tabs que o viewport, múltiplos splits e todas as
combinações possíveis de barras de ferramentas. Testes verdes não provam essa
matriz completa; confirme também visibilidade da aba ativa ao expandir/rolar.

## Atualização segura no tablet e limites de publicação

Pacotes instalados: `com.xnotes.community` e `com.neddy.nnotes.community`.
Use sempre o serial explícito do Redmi, nunca o default com emulador conectado.
Confira versão instalada e certificado antes de atualizar. Assine com a mesma
chave usada pela instalação existente; nunca contorne incompatibilidade com
uninstall, clear-data ou nova chave. Use `adb install -r` e confira versão,
lançamento e erros de runtime. Não coloque senhas ou keystores no Git.

A chave local usada nas instalações anteriores tem SHA-256 do certificado
`6f97ab0c98008f4ccbcac526dc48a61f2c1c347a6f0e37406d51e0589d8dc360`.
Esse valor identifica a chave; não é o checksum do APK nem uma assinatura de release
upstream. Não assuma que uma chave debug gerada em outra máquina será a mesma.

As branches community desta sessão são locais. n-notes tem `origin` apontando
para `NeDDy3z/n-notes`, enquanto XNotes aponta para o fork `wesleydanieell`.
Atualizar branches locais não autoriza trocar remotes, force-push, publicar PR,
tag ou release. Inspecione o destino e a autorização antes de publicar.

## Tamanho adaptável e barras vizinhas

O tamanho expandido floating deixou de ser fixo em 360 dp. `DocumentTabsBarConfig`
persiste `sizing` (DYNAMIC/MANUAL), `manual_extent_dp`, `min_extent_dp` e
`max_extent_dp` (zero = espaço livre). Configurações antigas assumem DYNAMIC.
Dinâmico usa o intervalo livre na mesma fileira até a próxima toolbar; manual
escolhe uma extensão, sempre limitada ao espaço disponível. Há presets mínimo,
360 dp e máximo, além de limites mínimo/máximo. Na lateral, extensão significa
altura; as abas individuais mantêm altura de 42 dp. Docked conserva a extensão
da borda reservada e não usa o limite floating.

`documentTabsFreeStrip` mede obstáculos reais, troca os eixos para laterais e
procura um intervalo próximo da âncora, com margem de segurança. Não basta
expandir até a borda da tela: outra barra pode ocupar a mesma fileira. O host
limita também a medição do AnimatedContent ao intervalo livre, para que o tamanho
manual e os frames da animação não invadam a próxima barra. Se a fileira inteira
estiver ocupada, procura outra faixa; se nenhuma faixa comportar tudo, não há
garantia absoluta de não sobreposição. Mínimos e tamanhos manuais cedem aos
limites físicos. Runtime e preview continuam compartilhando o mesmo cálculo.

O fixture de arraste aplica MotionEvents pela Activity para testar os handlers
Compose sem direcionar eventos a janelas transitórias do sistema. Isso verifica
a lógica de gestos do app, não arbitragem do Android em bordas/gestos do sistema.
Antes desse ajuste, uma rodada completa do XNotes com injeção de toque do sistema
passou no Redmi; outras rodadas tiveram erros de janela-alvo. O preview do fixture
tem margens superior e esquerda para não confundir barras com áreas do sistema.

## Validação final do tamanho adaptável — 2026-11-07

- XNotes: 2.628 testes JVM sem falhas; 22 testes de interface no Redmi.
- n-notes: 2.733 testes JVM sem falhas; 22 testes de interface no Small_Tablet,
  usando o runner correto `com.neddy.nnotes.debug.test`.
- Ambas as variantes community compilaram e foram atualizadas no Redmi por
  `adb install -r`, preservando dados e certificado. O hash SHA-256 do APK
  instalado foi comparado ao APK assinado local em cada app; ambos coincidiram.
  Os dois apps abriram sem FATAL EXCEPTION no smoke test de lançamento.
- A validação de gestos via Activity não substitui QA físico de caneta e áreas
  reservadas a gestos do Android. O debug n-notes foi recusado pelo instalador
  do Redmi; por isso sua bateria de interface final foi executada no emulador,
  enquanto a instalação community e o lançamento foram verificados no Redmi.
