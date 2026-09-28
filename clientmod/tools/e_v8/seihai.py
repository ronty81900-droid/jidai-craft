# -*- coding: utf-8 -*-
"""聖杯の欠片（中世・8202）の 3 案。  python tools/e_v8/seihai.py
  A: 今の v5 の形（上縁の帯＋左下へ下がる湾曲した破片・青い宝石 1 つ）を 64 に起こす
  B: 杯の上半分（器の部分）が残り、下が割れている
  C: 聖杯の全体の形に、上縁の欠けとひび
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
from dotto import Kyanbasu, hikaku, tenken  # noqa: E402
from PIL import Image  # noqa: E402

NE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SAKI = os.path.join(NE, 'nouhin', 'v8_an', 'seihai')
IMA = os.path.join(NE, 'nouhin', 'v5', 'assets', 'jidaiui', 'textures', 'item', 'seihai.png')
TOUMEI = (0, 0, 0, 0)


def houseki(k, cx, cy, r):
    """青い宝石（左上に小さな光）。"""
    k.daen(cx, cy, r, r, 'jewel_blue')
    k.daen(cx + r * 0.35, cy + r * 0.35, r * 0.75, r * 0.75, 'enamel_blue')
    k.daen(cx - r * 0.35, cy - r * 0.35, max(1, r * 0.35), max(1, r * 0.35), 'glass_light')


def an_a():
    """A: 今の形 ×4。上縁の帯（右に宝石）から左下へ、内側の面を見せながら細る破片。"""
    k = Kyanbasu()

    def PX(x):          # 幅 44 → 48 に広げる（x 12..56 → 8..56）
        return round(8 + (x - 12) * 48 / 44)

    def P(pts):
        return [(PX(x), y) for x, y in pts]

    # 破片の外形（左辺はほぼ垂直・右辺は割れ目でギザギザに細る）
    soto = [(12, 10), (26, 6), (44, 6), (54, 10), (56, 18), (50, 22), (46, 28), (42, 30), (40, 36), (34, 40),
            (32, 46), (28, 50), (26, 56), (20, 60), (12, 58)]
    k.takaku(P(soto), 'brass_dark')
    # 明るい面（左上寄り）と暗い面（右下寄り）
    k.takaku(P([(14, 12), (26, 8), (44, 8), (50, 11), (46, 16), (34, 18), (26, 24), (20, 36), (18, 50), (14, 52)]), 'brass')
    k.takaku(P([(16, 14), (26, 10), (40, 10), (36, 14), (26, 20), (22, 30), (18, 44), (16, 48)]), 'brass_light')
    # 上縁の帯（縁飾りの溝 2 本）
    k.orisen(P([(14, 14), (26, 11), (44, 11), (52, 14)]), 'brass_deep', 2)
    k.orisen(P([(14, 20), (28, 17), (44, 17), (52, 20)]), 'brass_deep', 2)
    # 器の内側（湾曲した面。帯の下・右側）
    k.takaku(P([(30, 20), (48, 20), (50, 22), (46, 28), (42, 30), (40, 36), (34, 40), (32, 46), (28, 48), (26, 40),
              (28, 30)]), 'brass_deep')
    k.takaku(P([(32, 22), (46, 22), (44, 27), (40, 30), (36, 36), (32, 40), (30, 34)]), 'wood_deep')
    # 宝石（上縁の右）
    houseki(k, PX(46.0), 14.0, 5)
    # 反射
    k.daen(PX(22.0), 14.0, 3, 2, 'highlight')
    return k.shiage()


def an_b():
    """B: 杯の器の部分。上縁の楕円で内側が見え、宝石の帯、下が割れて尖る。"""
    k = Kyanbasu()
    # 器の外形（楕円の下半分）
    m = k.daen_m(32.0, 14.0, 26, 40)
    ys = k._grid()[1]
    m &= ys >= 14
    # 割れ目（ギザギザより下を消す）
    ware = k.takaku_m([(0, 40), (10, 40), (16, 50), (22, 44), (27, 54), (33, 46), (39, 55), (45, 46), (51, 50), (56, 42), (64, 40), (64, 64), (0, 64)])
    m &= ~ware
    k.nuru(m, 'brass_dark')
    # 明るい面（左）と暗い面（右）
    m2 = k.daen_m(29.0, 12.0, 21, 36) & (ys >= 14) & ~ware
    k.nuru(m2, 'brass')
    m3 = k.daen_m(24.0, 10.0, 12, 30) & (ys >= 18) & ~ware
    k.nuru(m3, 'brass_light')
    # 上縁（楕円）: 外が明るい真鍮、内側が暗い（器の中）
    k.daen(32.0, 14.0, 26, 7, 'brass_light')
    k.daen(32.0, 14.0, 22, 4.5, 'brass_deep')
    k.daen(33.0, 15.0, 19, 3, 'wood_deep')
    # 宝石の帯
    k.takaku([(6, 26), (58, 26), (58, 34), (6, 34)], 'brass_deep')
    m4 = k.takaku_m([(0, 27), (64, 27), (64, 33), (0, 33)]) & k.daen_m(32.0, 14.0, 24, 38)
    k.nuru(m4, 'brass_dark')
    for cx in (18.0, 32.0, 46.0):
        houseki(k, cx, 30.0, 4)
    # 割れ目のすぐ上を暗く（厚み）
    k.orisen([(10, 40), (16, 50), (22, 44), (27, 54), (33, 46), (39, 55), (45, 46), (51, 50), (56, 42)], 'brass_deep', 2)
    # ひび
    k.orisen([(40, 46), (42, 40), (41, 36)], 'brass_deep', 1)
    # 反射
    k.daen(18.0, 20.0, 4, 2, 'highlight')
    return k.shiage()


def an_c():
    """C: 聖杯の全体。上縁の右に欠け、そこからひび。宝石の帯、脚と台座。"""
    k = Kyanbasu()
    ys = k._grid()[1]
    # 器（楕円の下半分・y 10..40）
    m = k.daen_m(32.0, 10.0, 24, 30) & (ys >= 10)
    k.nuru(m, 'brass_dark')
    k.nuru(k.daen_m(29.0, 8.0, 20, 27) & (ys >= 10), 'brass')
    k.nuru(k.daen_m(25.0, 6.0, 12, 22) & (ys >= 14), 'brass_light')
    # 上縁の楕円
    k.daen(32.0, 10.0, 24, 6, 'brass_light')
    k.daen(32.0, 10.0, 20, 4, 'brass_deep')
    k.daen(33.0, 11.0, 17, 2.5, 'wood_deep')
    # 節と脚
    k.daen(32.0, 40.0, 8, 4, 'brass_dark')
    k.shikaku(27, 40, 37, 52, 'brass_dark')
    k.shikaku(28, 40, 31, 52, 'brass')
    k.shikaku(35, 40, 37, 52, 'brass_deep')
    k.daen(32.0, 46.0, 7, 3, 'brass')
    k.daen(30.0, 45.0, 3, 1.5, 'brass_light')
    # 台座
    k.daen(32.0, 56.0, 22, 6, 'brass_dark')
    k.daen(30.0, 54.0, 18, 4, 'brass')
    k.daen(26.0, 53.0, 8, 2, 'brass_light')
    k.daen(32.0, 58.0, 22, 4, 'brass_deep')
    k.daen(32.0, 57.0, 20, 3, 'brass_dark')
    # 宝石の帯
    k.nuru(k.takaku_m([(0, 20), (64, 20), (64, 28), (0, 28)]) & k.daen_m(32.0, 10.0, 24, 30), 'brass_deep')
    k.nuru(k.takaku_m([(0, 21), (64, 21), (64, 27), (0, 27)]) & k.daen_m(32.0, 10.0, 22, 28), 'brass_dark')
    for cx in (20.0, 32.0, 44.0):
        houseki(k, cx, 24.0, 3.5)
    # 欠け（不規則に 4 か所）: 上縁の右（大）・上縁の左（小）・器の右側面・台座の右端
    k.takaku([(40, 2), (58, 2), (58, 14), (54, 11), (52, 17), (48, 10), (45, 14), (42, 7)], TOUMEI)
    k.takaku([(9, 5), (18, 2), (17, 9), (14, 8), (12, 13)], TOUMEI)
    k.takaku([(58, 25), (47, 29), (51, 33), (45, 37), (58, 39)], TOUMEI)
    k.takaku([(42, 63), (47, 58), (50, 60), (53, 55), (58, 57), (58, 63)], TOUMEI)
    # ひび（欠けから走る）
    k.orisen([(46, 15), (44, 21), (46, 27), (42, 33), (43, 37)], 'brass_deep', 1)
    k.orisen([(13, 13), (16, 18), (14, 24)], 'brass_deep', 1)
    k.orisen([(18, 56), (23, 58), (21, 61)], 'brass_deep', 1)
    # 反射
    k.daen(22.0, 15.0, 3, 2, 'highlight')
    return k.shiage()


def main():
    os.makedirs(SAKI, exist_ok=True)
    ima = Image.open(IMA).convert('RGBA').resize((64, 64), Image.NEAREST)
    kumi = [('A 今の形 ×4（上縁の破片）', an_a(), 'A'), ('B 器だけ残った杯', an_b(), 'B'), ('C 全体＋欠けとひび', an_c(), 'C')]
    ok = True
    for fuda, im, fn in kumi:
        im.save(os.path.join(SAKI, fn + '.png'))
        for gou, bun in tenken(im, fn):
            print(('[PASS] ' if gou else '[FAIL] ') + bun)
            ok &= gou
    p = hikaku([('今（v5・16×16）', ima)] + [(a, b) for a, b, _ in kumi],
               os.path.join(SAKI, 'hikaku.png'), '聖杯の欠片（中世・8202）')
    print(p)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
