"""Monta uma folha de contato com prints lado a lado, para revisão.

    python scripts/folha.py <pasta> <saida.png> <print1> <print2> ...   (nomes sem .png)
"""
import sys
from PIL import Image

folder, out, names = sys.argv[1], sys.argv[2], sys.argv[3:]
scale, gap = 0.3, 10
images = [Image.open(f"{folder}/{n}.png").convert("RGB") for n in names]
w, h = images[0].size
tw, th = int(w * scale), int(h * scale)
sheet = Image.new("RGB", (tw * len(images) + gap * (len(images) - 1), th), (128, 128, 128))
for i, im in enumerate(images):
    sheet.paste(im.resize((tw, th)), (i * (tw + gap), 0))
sheet.save(f"{folder}/{out}")
