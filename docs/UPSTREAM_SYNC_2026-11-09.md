# Atualização dos community com upstream — 2026-11-09

## Bases verificadas

- `community/xnotes`: `shardulvs/xnotes-android`, `master` em `3926304a`
  (0.9.7), já ancestral do HEAD community `25e83e7f`. Não havia commits novos.
- `community/n-notes`: `NeDDy3z/n-notes`, `master` em `293d4aec`
  (0.9.7-1.1). Merge de 23 commits sobre o community `fd501407`.
- Backup local do n-notes: `backup/community-nnotes-before-upstream-20261109-fd501407`.

## Resolução de conflitos

1. `app/build.gradle.kts`: versão upstream 108 / 0.9.7-1.1, mantendo o runner de
   instrumentação e a variante/ID community já existentes.
2. `Backstage.kt`: tanto PREFERENCES quanto TOOLBARS continuam acessíveis; ambos
   recebem o novo estado/callback do log de sincronização. TOOLBARS conserva o
   encaminhamento ao editor dedicado de barras.
3. `PreferencesPane.kt`: mantido o layout estável de categorias em tablets
   (`activeCategory`, navegação compacta só no telefone), adaptando os títulos e
   resumos aos novos recursos traduzidos do upstream. Integrada a nova SyncLogPage.
   Não reintroduzido o antigo editor de toolbars embutido nessa tela; suas variáveis
   e drag state pertencem ao editor dedicado do community.
4. `values/strings.xml` e `values-b+zh+Hans/strings.xml`: preservados os textos
   community e adicionados os recursos upstream, sem nomes duplicados.

As mudanças de sincronização, armazenamento atômico, conflitos, logs e traduções
espanhol/checo entram do upstream. Os adaptadores de abas, foco e barras
compartilhadas/por painel permanecem. A branch independente de compartilhamento
não foi alterada por este merge.

Não enviar `community/n-notes` ao origin de NeDDy3z. A branch community é publicada
no fork `wesleydanieell/xnotes-android`.

## Validação

- 2.771 testes JVM concluídos, zero falhas, erros ou testes ignorados.
- Resources XML validados; não há chaves duplicadas nos blocos combinados.
- A variante mantém `com.neddy.nnotes.community`; a versão resultante é
  `0.9.7-1.1-community`, versionCode 108.

A regressão completa detectou também um ciclo pré-existente no overflow do modo
por painel. Foi corrigido nos dois community separando hit exclusions de obstáculos
de posicionamento e excluindo o próprio overflow de seu cálculo; detalhes em
`COMMUNITY_SHARED_SPLIT_TOOLBARS.md`. Não é uma alteração na lógica de sync upstream.

- Regressão final: 55 testes de dispositivo aprovados no pacote isolado n-notes,
  incluindo 25 casos de split, exclusão em PDF, barras por painel, menus de seleção
  com caneta e 22 casos de animação das abas.
- Correção comum de overflow: cinco testes direcionados aprovados no XNotes.
- `assembleCommunity` com otimização R8 e lint vital passou nos dois forks.
- Nenhuma conta real de nuvem foi usada para executar sincronização: as mudanças
  de sync foram verificadas pelos testes JVM e pela compilação do código upstream.
- Conferência visual/funcional no pacote isolado: o editor dedicado abre por
  Preferences → Toolbar → Configure toolbars; a categoria Cloud sync traduzida
  abre com o serviço ainda em None/off. Nenhum login ou sync real foi realizado.
- Restaurado para `0` o ajuste temporário de manter a tela acesa por USB.
