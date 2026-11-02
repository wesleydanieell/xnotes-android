# Playbook de QA por ADB — XNotes Toolbars

Leia docs/toolbar-implementation-guide.md e docs/toolbar-task-state.md antes
de executar. Objetivo: o Codex deve COMPILAR, INSTALAR, ABRIR, TESTAR e
DIAGNOSTICAR a versão de toolbars sem tocar no XNotes normal do usuário.

## 1. Alvos e segurança

- Diretório: /home/wesley/Projetos/xnotes-android-toolbars
- Android SDK: /home/wesley/Android/Sdk
- APK esperado: app/build/outputs/apk/debug/app-debug.apk
- Application ID isolado: com.xnotes.toolbarsdebug
- Activity: com.xnotes.MainActivity
- Emulador conhecido: emulator-5554 (Small_Tablet). Não presumir que continuará
  igual: SEMPRE consultar adb devices -l e escolher explicitamente o serial.
- Redmi Pad 2 pode aparecer por USB/Wi-Fi; só instalar nele quando estiver
  autorizado e confirmar o serial. Nunca confundir com o outro dispositivo.
- Não usar adb uninstall, pm clear, apagar pastas de notas ou trocar chave de
  assinatura automaticamente. Isso apaga estado ou quebra upgrades.
- Não executar adb logcat -c (limpa logs úteis de outros processos).
- Não alterar regras de Wacom/OpenTabletDriver: não fazem parte desta feature.

## 2. Pré-flight (rodar em cada sessão)

No terminal Linux:

    cd /home/wesley/Projetos/xnotes-android-toolbars
    git status --short --branch
    git remote -v
    git diff --check
    command -v java
    /home/wesley/Android/Sdk/platform-tools/adb devices -l
    /home/wesley/Android/Sdk/emulator/emulator -list-avds

Se não houver serial online, pare antes de instalar. Emulador opcional,
somente se não houver um processo equivalente ativo:

    QT_QPA_PLATFORM=xcb /home/wesley/Android/Sdk/emulator/emulator @Small_Tablet -gpu swiftshader_indirect -no-snapshot -no-boot-anim -no-audio

A janela deve abrir no computador CachyOS. Não matar outra sessão do usuário.

## 3. Compilar e testar

    cd /home/wesley/Projetos/xnotes-android-toolbars
    ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain

Registrar exit code, falhas e testes executados no task-state. Atenção:
BUILD SUCCESSFUL não garante gestos e aparência. Se houver testes
instrumentados para esta feature, executar também:

    ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:connectedDebugAndroidTest --console=plain

Só contar um teste instrumentado se a task realmente existir e concluir.

## 4. Instalar e abrir (emulador)

Com adb devices -l confirmando emulator-5554:

    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell am start -n com.xnotes.toolbarsdebug/com.xnotes.MainActivity
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell pidof com.xnotes.toolbarsdebug

No tablet físico, substituir SOMENTE o serial depois de validá-lo. Instalar
a versão Toolbars não deve substituir apps com IDs com.xnotes,
com.xnotes.community ou com.xnotes.debug.

Falha INSTALL_FAILED_UPDATE_INCOMPATIBLE significa certificado diferente.
NÃO desinstalar automaticamente; localizar a chave original ou perguntar
como preservar os dados dessa variante de debug.

## 5. Coletar evidência sem destruir dados

Conferir se está ativo:

    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell dumpsys activity activities

Captura visual (arquivos temporários, nunca commitá-los sem necessidade):

    mkdir -p app/build/qa
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell screencap -p /sdcard/Download/xnotes-toolbar-qa.png
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 pull /sdcard/Download/xnotes-toolbar-qa.png app/build/qa/screen.png

Árvore acessível, se UIAutomator estiver disponível:

    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell uiautomator dump /sdcard/window.xml
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 pull /sdcard/window.xml app/build/qa/window.xml

Logs recentes, primeiro identificar PID do app e buscar exceções relevantes:

    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 logcat -d -t 800
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell dumpsys gfxinfo com.xnotes.toolbarsdebug framestats

Logs podem conter nomes de notas e dados do usuário: não publicar dumps
brutos em issue ou PR sem revisão/redação. Para um bug intermitente,
coletar timestamps + sequência exata de interação e stack trace.

## 6. Interações automatizáveis via input

Emulador: usar coordenadas determinadas pela screenshot ou XML, nunca
codificar posições absolutas permanentes. Exemplos ilustrativos:

    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell input tap X Y
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell input swipe X1 Y1 X2 Y2 750
    /home/wesley/Android/Sdk/platform-tools/adb -s emulator-5554 shell input keyevent KEYCODE_BACK

ADB input em geral injeta toque/dedo, NÃO se torna automaticamente S Pen
ou Xiaomi Smart Pen com pressão, hover e button state. Use essas ações
para smoke test de UI, alternar perfis, navegar, selecionar e arrastar.
Testes de pressão, palma, latência e primeiro traço precisam de caneta
real no Redmi Pad 2 ou harness de stylus explícito e validado.

## 7. Script manual de cenários para Codex/QA

Registrar PASS/FAIL/BLOCKED e screenshot quando visual.

A. Padrão legado:
   - Instalar/abrir configuração limpa e com dados legados (quando seguro);
   - uma barra principal, controles intactos e documento acessível.
B. Barra principal fixa:
   - trocar topo, baixo, esquerda e direita pelo configurador;
   - segurar e arrastar entre bordas; verificar que continua fixa;
   - página deve reservar área correspondente e manter escrita.
C. Flutuantes:
   - converter principal para flutuante, percorrer nove âncoras;
   - criar duas secundárias, colocar ambas na mesma âncora;
   - sem sobreposição crítica, scroll, clipping ou controles invisíveis.
D. Personalização:
   - duplicar, renomear, esconder/exibir, mover barras;
   - tornar uma imóvel; conferir que não reage ao gesto;
   - alterar tamanho e seleção na prévia; comparar com canvas real.
E. Perfis:
   - configurar xnote e xcanvas independentemente;
   - alternar shared sem destruir o perfil escondido;
   - trocar nota/canvas e reiniciar app, validar persistência.
F. Input:
   - toque em cada ferramenta, segurar no contêiner, arrastar;
   - menus de ferramentas e swatches devem continuar utilizáveis;
   - trocar foco em tela dividida e desenhar IMEDIATAMENTE (quando o
     split-view estiver presente na branch em teste).
G. Visual:
   - claro/OLED; nota branca/escura; retrato/paisagem;
   - cada preset compact/regular/comfortable, texto e borda visíveis.
H. Futuras fases:
   - largura/altura auto/fixa, blur fallback e padding;
   - cores 3x2, grupos 3x2, caneta unificada, context menu e resize edit;
   - undo/cancel e restauração após crash/restart.

Os cenários ainda não implementados DEVEM ser marcados BLOCKED/NOT IMPLEMENTED,
nunca PASS.

## 8. Perfis de performance (se ambiente tiver suporte)

Medir baseline master e branch no mesmo dispositivo/configuração, preferir:
- taxa de frames/jank e p95 frame time; alvo inicial <= 16,7ms em 60Hz
  para o cenário comum, sujeito ao hardware;
- latência até primeira tinta (observar traços não registrados);
- pen-down->ink durante escrita longa e após mudar pane;
- custo de recomposição com 1, 3 e 5 barras;
- memória antes/depois de abrir/fechar customizador repetidamente.
Não afirmar equivalência entre eventos input swipe por ADB e stylus físico.

## 9. Ciclo de depuração Codex

1. Reproduzir com passos e versão/commit exatos.
2. Capturar log filtrado e, se visual, screenshot.
3. Localizar causa em pointer dispatch, layout, persistência ou renderer.
4. Escrever teste/regressão reproduzível antes da correção quando viável.
5. Corrigir no menor domínio e reexecutar a matriz relevante.
6. Só então marcar tarefa concluída; criar commit focado e atualizar
   docs/toolbar-task-state.md com evidências.
7. Após três hipóteses/testes malsucedidos, documentar e pedir direção
   se depender de hardware/decisão. Não continuar com hacks aleatórios.

Não criar, atualizar, comentar ou mesclar PRs automaticamente. Preparar
branches e descrições; o usuário fará abertura e revisão manual.
