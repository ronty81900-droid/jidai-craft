# -*- coding: utf-8 -*-
"""
oukan.py -- ロンバルディアの鉄王冠（8212）v10・512×512 の案。

  本物: 金の板 6 枚を蝶番でつないだ低い輪。板ごとに七宝の花と宝石。内側に細い鉄の帯
  （キリストの磔刑の釘から作られたと伝わる）。背の高い尖った冠ではない。
  いまの絵（v6・64×64）は、丸い真鍮の枠の中に色の板が並び、記章に見えていた。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/oukan.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
GOLD = ['brass_dark', 'brass', 'brass_light', 'brass_light']


def zabuton(k, x0, x1, y0, y1):
    """赤い天鵞絨の台座と、四隅の房。"""
    m = k.marukaku_m(x0, y0, x1, y1, 8)
    k.kyuu(m, (x0 + x1) / 2 - 10, y0, (x1 - x0) / 2 + 10, (y1 - y0) + 6, ['red_deep', 'red', 'red', 'red_light'])
    k.nuru(m & (YS >= y1 - 4), 'red_deep')
    for x in (x0 + 3, x1 - 7):
        k.kyuu(k.daen_m(x + 2, y1 - 2, 4, 4), x + 2, y1 - 2, 4, 4, ['brass_dark', 'brass', 'brass_light', 'brass_light'])
    return m


def hana(k, cx, cy, r, c1, c2):
    """七宝の花（5弁）と、真ん中の宝石。"""
    for i in range(5):
        a = math.radians(90 + i * 72)
        k.daen(cx + r * 0.55 * math.cos(a), cy - r * 0.55 * math.sin(a), r * 0.45, r * 0.45, c1)
    k.daen(cx, cy, r * 0.42, r * 0.42, c2)


def an_a():
    """案A: 本物に沿わせる。金の板をつないだ低い輪（斜め上から）・七宝の花と宝石・内側の鉄の帯・赤い台座。"""
    k = D.K128()
    zabuton(k, 8, 120, 80, 116)
    cx = 64
    # 輪の外形: 上の縁の楕円・前の面・下の丸み（背を高く）
    ue = k.daen_m(cx, 30, 52, 18)
    mae = (XS + 0.5 >= cx - 52) & (XS + 0.5 <= cx + 52) & (YS >= 30) & (YS <= 78)
    shita = k.daen_m(cx, 78, 52, 18)
    wa = ue | (mae & ~(YS > 78)) | (shita & (YS >= 78))
    # 前の面（金。横に丸く陰影）
    # 前の面は縦の筋で丸みを出す（円柱）。斜めの帯にしない
    k.tate_dan(wa, cx - 52, cx + 53, ['brass', 'brass_light', 'brass_light', 'brass', 'brass', 'brass',
                                    'brass', 'brass_dark', 'brass_dark', 'brass_deep'])
    # 輪の中（上から覗く）: 奥の内側に鉄の帯、その下は暗い
    naka = k.daen_m(cx, 30, 44, 12)
    k.nuru(naka, 'black_mid')
    k.nuru(naka & ~k.daen_m(cx, 35, 44, 11), 'iron')              # 奥の内側の鉄の帯
    k.nuru(naka & ~k.daen_m(cx, 34, 44, 11) & k.daen_m(cx, 33, 44, 11), 'iron_light')
    k.nuru(naka & ~k.zurasu(naka, 0, 2), 'black')
    # 上の縁（金の細い縁）
    fuchi = ue & ~naka & (YS < 44)
    k.nuru(fuchi & (YS < 30), 'brass_light')
    # 上の縁の小さな真珠
    for i in range(9):
        a = math.radians(200 + i * 17.5)
        x, y = cx + 48 * math.cos(a), 30 - 15 * math.sin(a)
        k.daen(x, y, 1.8, 1.6, 'white_light')
    # 板のつなぎ目（蝶番）: 前の面を 5 枚に分ける
    for x in (cx - 34, cx - 13, cx + 13, cx + 34):
        k.nuru(wa & ~naka & (np.abs(XS + 0.5 - x) < 1.2) & (YS > 40), 'brass_deep')
        for y in (48, 60, 72):
            k.shikaku(x - 1, y, x, y + 1, 'brass_light')
    # 板ごとの七宝の花と宝石
    hana(k, cx, 60, 11, 'enamel_green', 'jewel_red')
    hana(k, cx - 24, 59, 9, 'jewel_blue', 'jewel_red')
    hana(k, cx + 24, 59, 9, 'jewel_blue', 'jewel_red')
    for x in (cx - 43, cx + 43):
        k.daen(x, 58, 3.5, 6, 'enamel_green')
        k.daen(x, 58, 1.6, 2.8, 'jewel_red')
    # 宝石の照り
    k.daen(cx - 1.4, 58.6, 1.3, 1.1, 'highlight')
    k.sen(cx + 30, 82, cx + 36, 80, 'scuff')                       # 経年は 1 か所
    return k.shiage128()


def an_b():
    """案B: だれが見ても王冠と分かる形。金の冠（上の縁はひと続きのギザギザ）・内側の鉄の帯・宝石・赤い帽子。
    ★ 尖りを1本ずつ離すと、細い所が縁の暗い色だけになり、城の塔に見えた（1回目の失敗）。
      縁をひと続きのギザギザにして、尖りの根元を太くする。"""
    k = D.K128()
    cx = 64
    # 冠の外形: 下の帯＋上のギザギザ（山 5 つ・谷 4 つ）
    yama = [(8, 34), (26, 22), (45, 16), (64, 10), (83, 16), (102, 22), (120, 34)]
    tani = [(17, 44), (36, 38), (55, 36), (73, 36), (92, 38), (111, 44)]
    ten = [(8, 116), (8, 58)]
    for a, b in zip(yama, tani + [None]):
        ten.append(a)
        if b is not None:
            ten.append(b)
    ten += [(120, 58), (120, 116)]
    kan = k.takaku_m([(x, y) for (x, y) in ten])
    k.tate_dan(kan, 8, 121, ['brass', 'brass_light', 'brass_light', 'brass', 'brass', 'brass', 'brass',
                             'brass_dark', 'brass_dark', 'brass_deep'])
    # 内側の赤い帽子（谷の奥に見える）
    naka = kan & (YS < 66) & (YS > 30)
    k.nuru(naka & ~k.zurasu(kan, 0, 8), 'red')
    # 下の帯（鉄の帯＝この冠の名の由来）と宝石
    obi = kan & (YS >= 80) & (YS <= 108)
    k.men(obi, 'steel_light', 'iron_light', 'iron', haba=2, kage2='iron_deep')
    for i, x in enumerate((24, 44, 64, 84, 104)):
        c = ('jewel_red', 'red_deep') if i % 2 == 0 else ('jewel_blue', 'enamel_blue')
        k.kyuu(k.daen_m(x, 94, 6.5, 7.5), x, 94, 6.5, 7.5, [c[1], c[0], c[0], 'white_light'])
        k.wa(x, 94, 8.5, 6.8, 'brass')
    # 山ごとの宝石（小さく）
    for (x, y) in yama[1:-1]:
        k.kyuu(k.daen_m(x, y + 20, 4.5, 4.5), x, y + 20, 4.5, 4.5, ['red_deep', 'jewel_red', 'jewel_red', 'red_light'])
    k.daen(62, 91, 1.4, 1.2, 'highlight')
    k.sen(96, 104, 102, 102, 'scuff')                                 # 経年は 1 か所
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('oukan', {'A': an_a, 'B': an_b}, 'ロンバルディアの鉄王冠 ── 512×512 の案')
