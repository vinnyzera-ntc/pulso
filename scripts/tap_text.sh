#!/usr/bin/env bash
# Toca no centro do primeiro elemento da tela cujo text (ou, se nenhum, content-desc) contém "$1".
#   scripts/tap_text.sh "Permitir acesso"
set -euo pipefail
export MSYS_NO_PATHCONV=1  # Git Bash no Windows: não converter /sdcard em caminho local
adb shell uiautomator dump /sdcard/pulso_ui.xml >/dev/null
adb shell cat /sdcard/pulso_ui.xml | python -c '
import re, sys
needle = sys.argv[1]
xml = sys.stdin.buffer.read().decode("utf-8")
nodes = [m.group(0) for m in re.finditer(r"<node [^>]*>", xml)]
# Texto visível primeiro: o círculo de concluir tem content-desc "Concluir: <título>".
for attr in ("text", "content-desc"):
    for n in nodes:
        if needle in re.search(attr + r"=\"([^\"]*)\"", n).group(1):
            x1, y1, x2, y2 = map(int, re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n).groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            sys.exit(0)
sys.exit("não encontrado: " + needle)
' "$1" | { read -r x y; adb shell input tap "$x" "$y"; echo "tap $x,$y ($1)"; }
adb shell rm /sdcard/pulso_ui.xml
