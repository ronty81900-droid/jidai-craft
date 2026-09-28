# -*- coding: utf-8 -*-
"""羊皮紙の海図（中世・8203）第2回: 巻物風＋羅針図を 256×256 で。地図は肌色、巻きは翠（緑）。
  python tools/e_v8/kaizu2.py
  座標は 64 単位、bai=4 で 256 に出す（dotto.Kyanbasu）。
"""
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
from dotto import Kyanbasu, hikaku, tenken  # noqa: E402
from PIL import Image  # noqa: E402

NE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SAKI = os.path.join(NE, 'nouhin', 'v8_an', 'kaizu')
IMA = os.path.join(NE, 'nouhin', 'v5', 'assets', 'jidaiui', 'textures', 'item', 'kaizu.png')
TOUMEI = (0, 0, 0, 0)


def maki_midori(k, x0, x1, y0, y1):
    """翠の巻き（縦の円筒）。左から 暗→明→暗。上下の端は丸い。"""
    w = x1 - x0
    obi = [('lens_deep', 1.5), ('lens', 3), ('lens_light', 3), ('lens', 3), ('enamel_green', 2), ('lens_deep', w - 12.5)]
    x = x0
    for c, ww in obi:
        k.takaku([(x, y0), (x + ww, y0), (x + ww, y1), (x, y1)], c)
        x += ww
    cx = (x0 + x1) / 2
    # 端の丸み（上は明るく、下は暗く）
    k.daen(cx, y0, w / 2, 2.5, 'lens_light')
    k.daen(cx, y1, w / 2, 2.5, 'lens_deep')


def jikusaki(k, cx, y0, y1):
    """軸先（真鍮の短い円筒）。"""
    k.takaku([(cx - 3.5, y0), (cx + 3.5, y0), (cx + 3.5, y1), (cx - 3.5, y1)], 'brass_dark')
    k.takaku([(cx - 2.5, y0), (cx + 0.5, y0), (cx + 0.5, y1), (cx - 2.5, y1)], 'brass')
    k.daen(cx, y0, 3.5, 1.2, 'brass_light')
    k.daen(cx, y1, 3.5, 1.2, 'brass_deep')


def rashin(k, cx, cy, R, r):
    """羅針図。斜めの短い針 → 輪 → 縦横の長い針（左半分 明・右半分 暗）→ 中心 → 北の印。"""
    def hari(th, nagasa, haba, hidari, migi):
        tx, ty = cx + nagasa * math.cos(th), cy + nagasa * math.sin(th)
        px_, py_ = -math.sin(th) * haba, math.cos(th) * haba
        k.takaku([(cx, cy), (tx, ty), (cx - px_, cy - py_)], hidari)
        k.takaku([(cx, cy), (tx, ty), (cx + px_, cy + py_)], migi)

    for i in range(1, 8, 2):
        hari(-math.pi / 2 + i * math.pi / 4, r, r * 0.35, 'brass_dark', 'brass_deep')
    k.wa(cx, cy, R * 0.72, R * 0.72 - 0.7, 'brass_deep')
    for i in range(0, 8, 2):
        hari(-math.pi / 2 + i * math.pi / 4, R, r * 0.5, 'brass_light', 'brass_deep')
    k.daen(cx, cy, r * 0.32, r * 0.32, 'brass')
    k.daen(cx, cy - R - 1.6, 1.0, 1.0, 'red')      # 北の印


def kouro(k, ten):
    """点線の航路（赤）。1 単位の太さ、2 単位の破線。"""
    for (x0, y0), (x1, y1) in zip(ten, ten[1:]):
        L = math.hypot(x1 - x0, y1 - y0)
        n = max(1, int(L / 4))
        for i in range(n):
            t0, t1 = i / n, (i + 0.5) / n
            k.sen(x0 + (x1 - x0) * t0, y0 + (y1 - y0) * t0, x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1, 'red', 1)


def makimono_256():
    k = Kyanbasu(bai=4)
    # 開いた地図（肌色）。上下の縁が少したわむ
    k.takaku([(14, 11), (32, 12), (50, 11), (50, 53), (32, 54), (14, 53)], 'hada')
    k.takaku([(14, 11), (40, 11.5), (30, 22), (20, 30), (14, 40)], 'hada_light')
    # 巻きへ入る所は暗く（紙が曲がる）
    k.takaku([(14, 11), (17.5, 11), (17.5, 53), (14, 53)], 'hada_dark')
    k.takaku([(46.5, 11), (50, 11), (50, 53), (46.5, 53)], 'hada_dark')
    k.takaku([(14, 47.5), (50, 47.5), (50, 53), (14, 53)], 'hada_dark')
    # 海（左上）と段状の海岸線。海側に浅瀬の明るい帯
    umi = [(16, 15), (30, 15), (30, 19.5), (26.5, 19.5), (26.5, 24.5), (23, 24.5), (23, 29.5), (19.5, 29.5), (19.5, 34.5), (16, 34.5)]
    kishi = umi[1:]
    k.takaku(umi, 'jewel_blue')
    k.orisen([(x - 1, y - 1) for x, y in kishi], 'glass', 1)
    k.orisen(kishi, 'enamel_blue', 1)
    # 小島
    k.daen(20.5, 21.5, 2.2, 1.5, 'hada_dark')
    k.daen(20.2, 21.2, 1.4, 0.9, 'hada')
    # 羅針図（右下寄り）
    rashin(k, 37.5, 35.0, 9.5, 5.0)
    # 航路（海から陸へ）
    kouro(k, [(21, 17), (27, 27), (24, 37), (29, 45)])
    # 経年（1 か所）: 茶の染みの輪
    k.wa(24.5, 44.0, 3.4, 2.7, 'hada_dark')
    # 左右の翠の巻き（紙の端に被せる）と軸先
    maki_midori(k, 2, 16, 7, 57)
    maki_midori(k, 48, 62, 7, 57)
    for cx in (9.0, 55.0):
        jikusaki(k, cx, 3.2, 7.5)
        jikusaki(k, cx, 56.5, 60.8)
    # 反射（左の巻きの明るい帯の上に 1 か所）
    k.daen(8.5, 20.0, 0.9, 5.0, 'highlight')
    return k.shiage()


def main():
    os.makedirs(SAKI, exist_ok=True)
    ima = Image.open(IMA).convert('RGBA').resize((64, 64), Image.NEAREST)
    mae = Image.open(os.path.join(SAKI, 'B.png')).convert('RGBA')
    e = makimono_256()
    e.save(os.path.join(SAKI, 'makimono_256.png'))
    ok = True
    for gou, bun in tenken(e, 'makimono_256'):
        print(('[PASS] ' if gou else '[FAIL] ') + bun)
        ok &= gou
    p = hikaku([('今（v5・16×16）', ima), ('B 巻物（前回・64）', mae), ('翠の巻物＋羅針図（256×256）', e)],
               os.path.join(SAKI, 'hikaku2.png'), '羊皮紙の海図 第2回: 巻物風＋羅針図・256')
    print(p)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
