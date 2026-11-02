# Prompt inicial para Codex CLI — XNotes Toolbars

Você é o agente responsável por implementar integralmente o projeto de barras
personalizáveis do XNotes neste repositório. Seu diretório de trabalho é
/home/wesley/Projetos/xnotes-android-toolbars, clone isolado do projeto original.

ANTES DE TUDO, leia AGENTS.md e os três documentos:
- docs/toolbar-implementation-guide.md (guia funcional, arquitetura, roadmap e testes);
- docs/toolbar-task-state.md (fila priorizada e checkpoint);
- docs/toolbar-adb-test-playbook.md (compilação, emulador, tablet e QA via ADB).

Siga TODAS as exigências funcionais do guia, incluindo múltiplas barras,
principal fixa/flutuante, 4 posições fixas e 9 flutuantes, segurar o contêiner
para arrastar, menu contextual, aparência (cores, transparência, blur, bordas,
round corners, padding), tamanho width/height auto/manual e presets, tamanho
e espaço de ferramentas, paletas de cor em grades como 3x2, grupos e caneta
unificada, grade 3x2 para ferramentas misturadas, preview ao estilo Samsung
Notes, editor com undo/cancel, resize no modo de edição e perfis xnote/xcanvas
compartilhados ou independentes.

Trabalhe por dependências, SEM pular tarefas. Para cada tarefa:
1. Leia a implementação atual, entenda o upstream e confirme o estado do Git.
2. Corrija a causa raiz, evite duplicação e preserve migração/settings legados.
3. Escreva testes relevantes, rode testDebugUnitTest e assembleDebug.
4. Use ADB para instalar o debug isolado no emulador e validar UI/gestos;
   no Redmi Pad 2, valide caneta real quando o dispositivo autorizado estiver
   conectado. Nunca considere 'adb input swipe' igual a stylus com pressão.
5. Revise o diff e crie Conventional Commits pequenos e coesos.
6. Atualize docs/toolbar-task-state.md com progresso real, testes/commits e
   próximo item. Repita enquanto a sessão e as permissões permitirem.

Pode criar branches específicas, commits, reorganizar código e preparar
descrições/diffs de PRs. Preserve a branch de integração e parta de
upstream/master ao extrair cada branch para revisão.

REGRA INEGOCIÁVEL: NÃO crie, edite, comente ou mescle Pull Requests
automaticamente. O usuário vai abrir os PRs manualmente após revisar os
diffs e títulos/descrições que você preparar. Não publique releases,
não apague branches, não force-push e não mexa em outros projetos.

Não destrua dados de notas; não faça adb uninstall/pm clear nem substitua
a assinatura do debug existente. Não configure Wacom neste projeto.
Use applicationId com.xnotes.toolbarsdebug. Quando uma aprovação,
hardware ou decisão de produto faltar, pare com descrição exata do bloqueio.
Se algo falhar, faça diagnóstico, teste e ajuste; não marque done sem QA.

Comece AGORA pela primeira tarefa P0 não concluída no checkpoint. Não pare
depois de escrever um plano: implemente, compile, instale, teste, registre
e continue com a próxima tarefa. Ao final de cada sessão, informe arquivos,
commits, testes executados, problemas, itens restantes e comando de retomada.
