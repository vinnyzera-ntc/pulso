#!/usr/bin/env bash
# Toca no centro do primeiro elemento da tela cujo text ou content-desc contém "$1".
#   scripts/tap_text.sh "Permitir acesso"
set -euo pipefail
export MSYS_NO_PATHCONV=1  # Git Bash no Windows: não converter /sdcard em caminho local
adb shell uiautomator dump /sdcard/pulso_ui.xml >/dev/null
adb shell cat /sdcard/pulso_ui.xml | python -c '
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
sys.exit("não encontrado: " + needle)
' "$1" | { read -r x y; adb shell input tap "$x" "$y"; echo "tap $x,$y ($1)"; }
adb shell rm /sdcard/pulso_ui.xml
