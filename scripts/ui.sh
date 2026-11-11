#!/usr/bin/env bash
# Funções para dirigir o Pulso no aparelho durante a verificação. Use com `source scripts/ui.sh`.
#
# Trava de segurança: toda ação (toque, arrastar, voltar, print) confere antes se o Pulso está
# em primeiro plano. Se outro app estiver na frente (por exemplo, porque um "voltar" saiu do
# app), a função falha em vez de agir no app errado ou tirar print dele.
export MSYS_NO_PATHCONV=1  # Git Bash no Windows: não converter /sdcard em caminho local
PKG="com.organizador.agenda"
ACTIVITY="$PKG/.MainActivity"

# Falha se o Pulso não estiver em primeiro plano.
fg() {
  local top
  top="$(adb shell dumpsys activity activities | grep -m1 topResumedActivity | tr -d '\r')"
  case "$top" in
    *" $PKG/"*) return 0 ;;
    *) echo "ABORTADO: o Pulso não está em primeiro plano ($top)" >&2; return 1 ;;
  esac
}

# open <tela> [seed]: abre o app do zero numa tela (hoje, agenda, tarefas, foco, ajustes).
open() {
  if [ -n "${2:-}" ]; then adb shell am start -n "$ACTIVITY" -f 0x10008000 -e tela "$1" -e seed "$2" >/dev/null
  else adb shell am start -n "$ACTIVITY" -f 0x10008000 -e tela "$1" >/dev/null; fi
  sleep "${OPEN_DELAY:-3}"
}

# dump: XML da tela atual (UTF-8) na saída padrão.
dump() { fg || return 1; adb shell uiautomator dump /sdcard/pulso_ui.xml >/dev/null; adb shell cat /sdcard/pulso_ui.xml; }

# center <trecho>: "x y" do centro do primeiro elemento cujo text ou content-desc contém o trecho.
center() {
  dump | python -c '
import re, sys
needle = sys.argv[1]
xml = sys.stdin.buffer.read().decode("utf-8")
for node in re.finditer(r"<node [^>]*>", xml):
    n = node.group(0)
    t = re.search(r"text=\"([^\"]*)\"", n).group(1)
    d = re.search(r"content-desc=\"([^\"]*)\"", n).group(1)
    if needle in t or needle in d:
        x1, y1, x2, y2 = map(int, re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n).groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        sys.exit(0)
sys.exit(1)
' "$1" | tr -d '\r'
}

# texts: todos os textos visíveis, um por linha.
texts() { dump | python -c '
import re, sys
for t in re.findall(r"text=\"([^\"]+)\"", sys.stdin.buffer.read().decode("utf-8")): sys.stdout.buffer.write((t + "\n").encode("utf-8"))'; }

# tap <trecho>: toca no elemento; falha se não achar.
tap() {
  local xy; xy="$(center "$1")" || { echo "não encontrado: $1" >&2; return 1; }
  [ -n "$xy" ] || { echo "não encontrado: $1" >&2; return 1; }
  # shellcheck disable=SC2086
  adb shell input tap $xy; sleep "${TAP_DELAY:-1.2}"
}

tapxy() { fg || return 1; adb shell input tap "$1" "$2"; sleep "${TAP_DELAY:-1.2}"; }
swipe() { fg || return 1; adb shell input swipe "$1" "$2" "$3" "$4" "${5:-300}"; sleep 0.8; }
drag() { fg || return 1; adb shell input draganddrop "$1" "$2" "$3" "$4" "${5:-2500}"; sleep 1.2; }
typetext() { fg || return 1; adb shell input text "$1"; sleep 0.6; }

# back: "voltar", só se o Pulso estiver na frente.
back() { fg || return 1; adb shell input keyevent KEYCODE_BACK; sleep 0.8; }

# shot <arquivo.png>: print, só se o Pulso estiver na frente.
shot() { fg || return 1; adb exec-out screencap -p > "$1"; }
