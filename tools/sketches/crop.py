"""Cuts the empty background off the bottom of each sketch PNG (keeps a 24 px margin)."""
import sys
from pathlib import Path

from PIL import Image

folder, stamp = Path(sys.argv[1]), sys.argv[2]
for png in sorted(folder.glob(f'*_{stamp}.png')):
    img = Image.open(png).convert('RGB')
    w, h = img.size
    bg = img.getpixel((w - 2, h - 2))
    last = 0
    for y in range(h):
        row = [img.getpixel((x, y)) for x in range(0, w, 4)]
        if any(abs(sum(p) - sum(bg)) > 12 for p in row):
            last = y
    img.crop((0, 0, w, min(h, last + 24))).save(png)
    print(png.name, w, 'x', min(h, last + 24))
