#!/usr/bin/env bash
# Tira prints do app no aparelho conectado, nos temas escuro e claro.
#
#   scripts/prints.sh <pasta> <tela> [<tela>...]
#   ex.: scripts/prints.sh fase-0 hoje agenda tarefas foco
#
# <tela> é a chave de TopLevel (hoje, agenda, tarefas, foco), passada à MainActivity
# pelo extra de debug "-e tela". Os arquivos vão para prints/<pasta>/<tela>_<tema>.png.
# O modo noturno original do aparelho é restaurado no fim, mesmo se o script falhar.
set -euo pipefail

PKG="com.organizador.agenda"
ACTIVITY="$PKG/.MainActivity"
OUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/prints/${1:?pasta de saída}"
shift
SCREENS=("$@")
[ ${#SCREENS[@]} -gt 0 ] || { echo "informe ao menos uma tela" >&2; exit 1; }
mkdir -p "$OUT_DIR"

# "Night mode: yes" -> "yes" (yes | no | auto | custom...)
ORIGINAL_NIGHT="$(adb shell cmd uimode night | tr -d '\r' | sed 's/^Night mode: //')"
restore() { adb shell cmd uimode night "$ORIGINAL_NIGHT" >/dev/null; echo "modo noturno restaurado: $ORIGINAL_NIGHT"; }
trap restore EXIT

adb shell input keyevent KEYCODE_WAKEUP

for mode in yes no; do
  theme=$([ "$mode" = yes ] && echo escuro || echo claro)
  adb shell cmd uimode night "$mode" >/dev/null
  for screen in "${SCREENS[@]}"; do
    # 0x10008000 = NEW_TASK | CLEAR_TASK: abre a atividade do zero já na tela pedida.
    adb shell am start -n "$ACTIVITY" -f 0x10008000 -e tela "$screen" >/dev/null
    sleep "${PRINT_DELAY:-2.5}"
    adb exec-out screencap -p > "$OUT_DIR/${screen}_${theme}.png"
    echo "ok: $OUT_DIR/${screen}_${theme}.png"
  done
done
