# -*- coding: utf-8 -*-
"""羊皮紙の海図（中世・8203）の 3 案。  python tools/e_v8/kaizu.py
  A: 今の v5 の形（左端が巻かれた 1 枚の羊皮紙・段状の海岸線・5 点の方位印）を 64 に起こす
  B: 巻物（左右の巻きの間に海図が開いている）
  C: 広げた海図（角がめくれ、破れ・焼け跡・大きな羅針図・点線の航路）
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
from dotto import Kyanbasu, hikaku, tenken  # noqa: E402
from PIL import Image  # noqa: E402

NE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SAKI = os.path.join(NE, 'nouhin', 'v8_an', 'kaizu')
IMA = os.path.join(NE, 'nouhin', 'v5', 'assets', 'jidaiui', 'textures', 'item', 'kaizu.png')
TOUMEI = (0, 0, 0, 0)


def umi(k, poly, kishi):
    """海（青）と段状の海岸線。poly = 海の多角形、kishi = 海岸線として描く折れ線。"""
    k.takaku(poly, 'jewel_blue')
    k.orisen(kishi, 'enamel_blue', 2)


def rashin(k, cx, cy, R, r):
    """8 方位の羅針図（真鍮）。北東南西が長く、斜めが短い。長い針は左半分が明るく右半分が暗い。"""
    import math

    def hari(th, nagasa, haba, hidari, migi):
        tx, ty = cx + nagasa * math.cos(th), cy + nagasa * math.sin(th)
        px_, py_ = -math.sin(th) * haba, math.cos(th) * haba
        k.takaku([(cx, cy), (tx, ty), (cx - px_, cy - py_)], hidari)
        k.takaku([(cx, cy), (tx, ty), (cx + px_, cy + py_)], migi)

    for i in range(1, 8, 2):                       # 斜めの短い針（下に敷く）
        hari(-math.pi / 2 + i * math.pi / 4, r, r * 0.35, 'brass_dark', 'brass_deep')
    for i in range(0, 8, 2):                       # 縦横の長い針
        hari(-math.pi / 2 + i * math.pi / 4, R, r * 0.5, 'brass_light', 'brass_deep')
    k.daen(cx, cy, max(1.5, r * 0.3), max(1.5, r * 0.3), 'brass')
    k.shikaku(int(cx) - 1, int(cy - R) - 3, int(cx), int(cy - R) - 2, 'red')       # 北の印


def kouro(k, ten):
    """点線の航路（赤）。"""
    import math
    for (x0, y0), (x1, y1) in zip(ten, ten[1:]):
        L = math.hypot(x1 - x0, y1 - y0)
        n = max(1, int(L / 4))
        for i in range(n):
            t0 = i / n
            t1 = (i + 0.5) / n
            k.sen(int(x0 + (x1 - x0) * t0), int(y0 + (y1 - y0) * t0), int(x0 + (x1 - x0) * t1), int(y0 + (y1 - y0) * t1), 'red', 1)


def maki(k, x0, x1, y0, y1):
    """縦の巻き（円筒）。x0..x1 の帯を左から 明→暗 に。上端に渦の小口。"""
    w = x1 - x0
    k.shikaku(x0, y0, x1, y1, 'paper_deep')
    k.shikaku(x0 + 1, y0, x0 + w * 3 // 4, y1, 'paper_dark')
    k.shikaku(x0 + 2, y0, x0 + w // 2, y1, 'paper')
    k.shikaku(x0 + 3, y0, x0 + w // 3, y1, 'paper_light')
    cx = (x0 + x1 + 1) / 2
    k.daen(cx, y0 + 0.5, w / 2, 3, 'paper_light')
    k.daen(cx, y0 + 0.5, w / 2 - 2, 1.8, 'paper_deep')
    k.daen(cx + 1, y0 + 0.5, w / 2 - 4, 0.9, 'paper')
    k.daen(cx, y1 + 0.5, w / 2, 3, 'paper_dark')
    k.daen(cx, y1 + 0.5, w / 2 - 2, 1.8, 'paper_deep')


def an_a():
    """A: 今の形 ×4。左端が巻かれた 1 枚。右下へ影。段状の海岸線と 5 点の方位印。"""
    k = Kyanbasu()
    # 紙の本体（右へ広がる台形・下辺が少し傾く）
    hon = [(12, 10), (58, 14), (58, 52), (30, 52), (26, 58), (6, 56), (6, 12)]
    k.takaku(hon, 'paper_dark')
    k.takaku([(12, 10), (54, 14), (54, 48), (28, 48), (24, 54), (8, 52), (8, 12)], 'paper')
    k.takaku([(14, 12), (40, 14), (36, 22), (20, 30), (10, 44), (10, 14)], 'paper_light')
    # 左端の巻き
    maki(k, 6, 20, 8, 56)
    # 海（左下）と段状の海岸線
    umi(k, [(22, 30), (28, 30), (28, 34), (32, 34), (32, 38), (36, 38), (36, 42), (40, 42), (40, 46), (44, 46), (44, 52), (22, 52)],
        [(22, 30), (28, 30), (28, 34), (32, 34), (32, 38), (36, 38), (36, 42), (40, 42), (40, 46), (44, 46), (44, 52)])
    # 小島
    k.daen(30.0, 45.0, 3, 2, 'paper_dark')
    # 5 点の方位印（v5 から）を真鍮で
    cx, cy = 46, 28
    for dx, dy in ((0, 0), (0, -7), (0, 7), (-7, 0), (7, 0)):
        k.shikaku(cx + dx - 1, cy + dy - 1, cx + dx + 1, cy + dy + 1, 'brass_dark')
        k.ten(cx + dx - 1, cy + dy - 1, 'brass_light')
    k.orisen([(cx, cy - 5), (cx, cy + 5)], 'brass_deep', 1)
    k.orisen([(cx - 5, cy), (cx + 5, cy)], 'brass_deep', 1)
    # 点線の航路
    kouro(k, [(46, 34), (44, 42), (40, 50)])
    # 破れ（右上の角・経年 1 か所）
    k.takaku([(50, 10), (58, 10), (58, 20), (55, 16), (52, 18)], TOUMEI)
    # 反射
    k.daen(16.0, 14.0, 2, 4, 'highlight')
    return k.shiage()


def an_b():
    """B: 巻物。左右の巻きの間に、海岸線と羅針図と航路が見える。"""
    k = Kyanbasu()
    # 開いた紙（中央・上下が少しカーブ）
    k.takaku([(14, 12), (50, 12), (50, 52), (14, 52)], 'paper_dark')
    k.takaku([(14, 12), (48, 12), (48, 48), (14, 48)], 'paper')
    k.takaku([(14, 12), (34, 12), (24, 24), (14, 40)], 'paper_light')
    # 海（左上）と段状の海岸線
    umi(k, [(14, 12), (30, 12), (30, 16), (26, 16), (26, 22), (22, 22), (22, 28), (18, 28), (18, 34), (14, 34)],
        [(30, 12), (30, 16), (26, 16), (26, 22), (22, 22), (22, 28), (18, 28), (18, 34), (14, 34)])
    # 羅針図（右下）
    rashin(k, 38.0, 36.0, 10, 5)
    # 航路
    kouro(k, [(28, 20), (34, 24), (32, 32), (36, 44)])
    # 左右の巻き
    maki(k, 4, 16, 8, 56)
    maki(k, 48, 60, 8, 56)
    # 反射
    k.daen(9.0, 20.0, 1.5, 4, 'highlight')
    return k.shiage()


def an_c():
    """C: 広げた海図。右下の角がめくれ、左上は焼け跡、縁は破れ。大きな羅針図と航路。"""
    k = Kyanbasu()
    # 紙（縁はわざと不揃い）
    hon = [(6, 6), (20, 4), (36, 7), (56, 4), (58, 22), (56, 40), (58, 58), (42, 60), (24, 57), (6, 60), (8, 40), (5, 22)]
    k.takaku(hon, 'paper_dark')
    k.takaku([(8, 8), (20, 6), (36, 9), (54, 6), (55, 22), (53, 40), (54, 52), (42, 54), (24, 52), (9, 56), (10, 40), (8, 22)], 'paper')
    k.takaku([(10, 10), (30, 8), (26, 18), (16, 26), (11, 40), (10, 12)], 'paper_light')
    # 海（左・段状の海岸線）と小島
    umi(k, [(10, 20), (22, 20), (22, 26), (26, 26), (26, 32), (30, 32), (30, 40), (26, 40), (26, 48), (10, 48)],
        [(22, 20), (22, 26), (26, 26), (26, 32), (30, 32), (30, 40), (26, 40), (26, 48)])
    k.daen(16.0, 38.0, 3, 2, 'paper_dark')
    k.daen(15.0, 37.5, 1.5, 1, 'paper')
    # 大きな羅針図（右）
    rashin(k, 43.0, 30.0, 12, 6)
    # 航路（海の中を点線で）
    kouro(k, [(12, 24), (18, 30), (14, 44)])
    # 焼け跡（左上・経年 1 か所）: 焦げの茶 → 黒
    k.takaku([(6, 6), (18, 4), (16, 10), (12, 12), (8, 18), (5, 22)], TOUMEI)
    k.orisen([(11, 24), (15, 17), (19, 14), (22, 10)], 'black_mid', 3)
    k.orisen([(13, 26), (17, 19), (21, 16), (24, 12)], 'scuff', 3)
    # 右下の角がめくれる（裏面は暗い紙・めくれの下は透明）
    k.takaku([(46, 60), (58, 46), (58, 58)], TOUMEI)
    k.takaku([(44, 56), (52, 46), (57, 52), (50, 58)], 'paper_dark')
    k.takaku([(46, 55), (52, 48), (55, 52), (50, 56)], 'paper_deep')
    # 反射
    k.daen(30.0, 13.0, 3, 2, 'highlight')
    return k.shiage()


def main():
    os.makedirs(SAKI, exist_ok=True)
    ima = Image.open(IMA).convert('RGBA').resize((64, 64), Image.NEAREST)
    kumi = [('A 今の形 ×4（左が巻かれた紙）', an_a(), 'A'), ('B 巻物', an_b(), 'B'), ('C 広げた海図（めくれ・焼け）', an_c(), 'C')]
    ok = True
    for fuda, im, fn in kumi:
        im.save(os.path.join(SAKI, fn + '.png'))
        for gou, bun in tenken(im, fn):
            print(('[PASS] ' if gou else '[FAIL] ') + bun)
            ok &= gou
    p = hikaku([('今（v5・16×16）', ima)] + [(a, b) for a, b, _ in kumi],
               os.path.join(SAKI, 'hikaku.png'), '羊皮紙の海図（中世・8203）')
    print(p)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
