from PIL import Image, ImageDraw, ImageFont
import os

SIZE = 512
BG = (30, 30, 35)
ORANGE = (220, 160, 40)
WHITE = (255, 255, 255)


def dashed_circle(draw, cx, cy, r, width, on_deg=14, off_deg=10):
    a = 0
    while a < 360:
        draw.arc(
            [cx - r, cy - r, cx + r, cy + r],
            start=a,
            end=min(a + on_deg, 360),
            fill=ORANGE,
            width=width,
        )
        a += on_deg + off_deg


def house(draw, x0, y0, x1, y1, apex_y, stroke):
    mid = (x0 + x1) / 2
    overhang = 14
    draw.line(
        [(x0, y0), (x0, y1), (x1, y1), (x1, y0)],
        fill=ORANGE,
        width=stroke,
        joint="curve",
    )
    draw.line(
        [(x0 - overhang, y0), (mid, apex_y), (x1 + overhang, y0)],
        fill=ORANGE,
        width=stroke,
        joint="curve",
    )


# --- Poster ---
img = Image.new("RGB", (SIZE, SIZE), BG)
draw = ImageDraw.Draw(img)

# House center-right, lit window = the room you can finally see.
house(draw, 265, 245, 415, 380, 172, 10)
draw.rectangle([315, 285, 365, 335], fill=WHITE)

# Tree front-left; solid trunk, dashed crown overlapping the house.
CROWN_CX, CROWN_CY, CROWN_R = 195, 215, 100
draw.line([(178, 395), (178, 290)], fill=ORANGE, width=14)
dashed_circle(draw, CROWN_CX, CROWN_CY, CROWN_R, 10)

# --- Text ---
try:
    font = ImageFont.truetype("arial.ttf", 44)
except Exception:
    font = ImageFont.load_default()

text1 = "Clear"
text2 = "Canopy"
bbox1 = draw.textbbox((0, 0), text1, font=font)
bbox2 = draw.textbbox((0, 0), text2, font=font)
tw1 = bbox1[2] - bbox1[0]
tw2 = bbox2[2] - bbox2[0]
th = bbox1[3] - bbox1[1]

text_y = 408
draw.text(((SIZE - tw1) / 2, text_y), text1, fill=WHITE, font=font)
draw.text(((SIZE - tw2) / 2, text_y + th + 12), text2, fill=WHITE, font=font)

out = os.path.join(os.path.dirname(__file__), "poster.png")
img.save(out)
print(f"Saved to {out}")


# --- Icon: tree with dashed crown, no house/text. Rendered at 256 and
# downscaled to 32 with LANCZOS so the dashes stay readable. ---
ICON_RENDER = 256
ICON_OUT = 32

icon_img = Image.new("RGB", (ICON_RENDER, ICON_RENDER), BG)
icon_draw = ImageDraw.Draw(icon_img)

ICX, ICY, IR = 128, 104, 84
icon_draw.line([(128, 240), (128, 150)], fill=ORANGE, width=26)
dashed_circle(icon_draw, ICX, ICY, IR, 20, on_deg=22, off_deg=14)

icon_small = icon_img.resize((ICON_OUT, ICON_OUT), Image.LANCZOS)
icon_path = os.path.join(os.path.dirname(__file__), "icon.png")
icon_small.save(icon_path)
print(f"Saved to {icon_path}")
