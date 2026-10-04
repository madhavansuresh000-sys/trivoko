"""Draws a simple, made-up product picture for the live Cloudinary upload test.

No real photo needed: run `python tools/test-photo/make_test_photo.py` and it writes
tools/test-photo/trivoko_test_phone.jpg (800x800, well under the 5 MB limit).
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

TEAL, TEAL_DARK, SAFFRON, WHITE = (13, 148, 136), (17, 94, 89), (249, 115, 22), (255, 255, 255)

img = Image.new("RGB", (800, 800), (240, 253, 250))
d = ImageDraw.Draw(img)


def font(size):
    try:
        return ImageFont.truetype("arialbd.ttf", size)
    except OSError:
        return ImageFont.load_default()


# the "phone": rounded body, screen, camera dot
d.rounded_rectangle((270, 110, 530, 640), radius=40, fill=TEAL_DARK)
d.rounded_rectangle((290, 150, 510, 590), radius=18, fill=TEAL)
d.ellipse((392, 122, 408, 138), fill=WHITE)
d.text((400, 330), "TriVoKo", fill=WHITE, font=font(44), anchor="mm")
d.text((400, 390), "K5", fill=SAFFRON, font=font(60), anchor="mm")

# label under the phone
d.text((400, 700), "TEST PHOTO - Phase 1 step 9", fill=TEAL_DARK, font=font(32), anchor="mm")

out = Path(__file__).with_name("trivoko_test_phone.jpg")
img.save(out, quality=88)
print(out, out.stat().st_size, "bytes")
