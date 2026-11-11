"""Monta uma folha de contato com prints, para revisão.

    python scripts/folha.py <pasta> <saida.png> <print1> <print2> ...   (nomes sem .png)

Prints em retrato ficam lado a lado numa linha; em paisagem, em grade de 2 colunas.
"""
import sys
from PIL import Image

folder, out, names = sys.argv[1], sys.argv[2], sys.argv[3:]
gap = 10
images = [Image.open(f"{folder}/{n}.png").convert("RGB") for n in names]
w, h = images[0].size
landscape = w > h
scale = 0.32 if landscape else 0.3
cols = 2 if landscape else len(images)
rows = (len(images) + cols - 1) // cols
tw, th = int(w * scale), int(h * scale)
sheet = Image.new("RGB", (tw * cols + gap * (cols - 1), th * rows + gap * (rows - 1)), (128, 128, 128))
for i, im in enumerate(images):
    sheet.paste(im.resize((tw, th)), ((i % cols) * (tw + gap), (i // cols) * (th + gap)))
sheet.save(f"{folder}/{out}")
