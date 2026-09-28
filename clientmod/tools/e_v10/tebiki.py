# -*- coding: utf-8 -*-
"""
tebiki.py -- 死の商人の手引書（8218）v10・512×512 の案。

  いまの絵（v6・64×64）: 黒い革の本（少し左へ回っている）。左に茶色の背と 5 本の帯、
  表紙の真ん中に金の短銃（右向き）、下に小口（紙の束）、右下の角は擦り切れている。
  ★ 案A は、いまの絵の形（外形・背・短銃・小口の位置）を 2 倍の 128 マスに写して、中を描き込む。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/tebiki.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
PX, PY = XS + 0.5, YS + 0.5
KIN = ('brass_light', 'brass', 'brass_dark')


def bezier(p0, p1, p2, p3, n=16):
    ten = []
    for i in range(n + 1):
        t = i / n
        a, b, c, d = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t ** 2, t ** 3
        ten.append((a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0],
                    a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1]))
    return ten


def tanjuu(k, dx=0, dy=0, iro=KIN, teri=True):
    """金の短銃（火打ち石式・右向き）。いまの絵の位置を 2 倍にした所に描く。
    部品ごとに面取り（左上が光・右下が影）して、黒い表紙の上で浮き上がらせる。"""
    def P(ten):
        return k.takaku_m([(x + dx, y + dy) for x, y in ten])
    hikari, moto, kage = iro
    buhin = [
        P([(72, 59), (97, 59), (97, 64), (76, 65)]),                      # 銃床の先（銃身の下）
        P([(48, 50), (95, 50), (95, 59), (48, 59)]),                      # 銃身
        P([(93, 48), (98, 48), (98, 61), (93, 61)]),                      # 銃口の輪
        P([(38, 48), (60, 48), (63, 52), (63, 62), (41, 64), (37, 58)]),  # 機関部
        P([(46, 61), (63, 61), (61, 70), (59, 78), (50, 81), (43, 78), (45, 69)]),   # 握り
        P([(41, 49), (40, 43), (43, 39), (48, 39), (49, 42), (45, 43), (45, 49)]),   # 撃鉄
        P([(52, 43), (55, 43), (56, 49), (52, 49)]),                      # 当たり金
    ]
    for m in buhin:
        k.men(m, hikari, moto, kage, haba=1)
    k.nuru(k.daen_m(51 + dx, 78 + dy, 8, 4.5), moto)                     # 床尾の金具
    k.nuru(k.daen_m(51 + dx, 78 + dy, 8, 4.5) & ~k.daen_m(50 + dx, 77 + dy, 7, 3.5), kage)
    # 引き金の囲い（下向きの U）と引き金
    wa = k.daen_m(68 + dx, 63 + dy, 7, 7) & ~k.daen_m(68 + dx, 63 + dy, 4.5, 4.5) & (PY > 63 + dy)
    k.nuru(wa, moto)
    k.nuru(wa & (PY > 68 + dy), kage)
    k.sen(66 + dx, 63 + dy, 65 + dx, 67 + dy, moto, futosa=2)
    # 銃身の帯と、機関部の板の線
    for x in (70, 84):
        k.nuru((np.abs(PX - x - dx) < 1) & (PY >= 50 + dy) & (PY < 59 + dy), kage)
    k.sen(42 + dx, 55 + dy, 60 + dx, 55 + dy, kage)
    if teri:
        k.shikaku(44 + dx, 51 + dy, 45 + dx, 52 + dy, 'highlight')


def an_a():
    """案A: いまの形を起こす。黒い革の表紙・茶色の背と 5 本の帯・金の短銃・下の小口・擦り切れた右下の角。"""
    k = D.K128()
    soto = [(10, 10), (114, 6), (118, 40), (122, 90), (122, 116), (18, 122)]
    soto += bezier((18, 122), (10, 122), (6, 116), (6, 108))[1:]
    soto += [(6, 74), (8, 40)]
    m = k.takaku_m(soto)
    uchi = D.dotto.sou_kazoeru(m) > D.FUCHI + D.SHINCHU
    # 表紙（光は左上。左は明るく、右へ暗く）
    k.nuru(m, 'black')
    k.nuru(m & (PX < 62), 'black_light')
    k.nuru(m & (PX >= 62) & (PX < 70), 'black_mid')
    k.nuru(uchi & ~k.zurasu(uchi, 0, 3), 'black_mid')                 # 上の辺の厚み
    k.nuru(uchi & ~k.zurasu(uchi, -3, 0), 'black_mid')                # 右の辺の厚み
    # 空押しの枠（表紙に押した2本の線）
    for d, c1, c2 in ((0, 'black_mid', 'black_mid'), (3, 'black_light', 'black_mid')):
        x0, x1, y0, y1 = 44 + d, 102 - d, 24 + d, 92 - d
        k.orisen([(x0, y0), (x1, y0 - 2), (x1 + 1, y1 - 2), (x0 + 1, y1), (x0, y0)],
                 c1 if d == 0 else c2)
    # 背（茶色の革）と 5 本の帯
    se = uchi & (PX < 38)
    k.nuru(se, 'leather_deep')
    k.nuru(se & (PX >= 34), 'black_mid')                               # 背と表紙の間の溝
    k.nuru(se & (PX < 22), 'leather')                                  # 背の丸み（光の当たる所）
    for y0 in (32, 46, 60, 74, 88):
        obi = se & (PY >= y0) & (PY < y0 + 6) & (PX < 34)
        k.nuru(obi, 'leather')
        k.nuru(obi & (PY < y0 + 2), 'leather_light')
        k.nuru(se & (PY >= y0 + 6) & (PY < y0 + 7) & (PX < 34), 'black_mid')
    # 小口（下の紙の束）
    kuchi = uchi & (PY >= 102 - (PX - 30) * 0.03) & (PX >= 26)
    k.nuru(kuchi, 'paper_light')
    for i, y in enumerate((105, 108, 111)):
        k.nuru(kuchi & (np.abs(PY - y + (PX - 30) * 0.03) < 0.6), 'paper' if i % 2 else 'paper_dark')
    k.nuru(kuchi & ~k.zurasu(kuchi, 0, 1), 'black_mid')                # 表紙の下の影
    # 金の短銃
    tanjuu(k)
    # 経年は 1 か所（右下の角の擦り切れ。革が剥げて芯が見える）
    kado = uchi & (PX + PY * 0.9 > 196) & (PY < 101)
    k.nuru(kado, 'scuff')
    k.nuru(kado & ~k.zurasu(kado, 1, 1), 'leather')
    return k.shiage128()


def an_b():
    """案B: 開いた手引書。左の頁に短銃の図面（寸法の線つき）、右の頁に文字と表、赤い栞。"""
    k = D.K128()
    # 表紙（開いた本の下に、黒い革が少しはみ出す）
    hyoushi = k.takaku_m([(6, 26), (40, 20), (64, 30), (88, 20), (122, 26), (122, 108), (88, 104),
                          (64, 118), (40, 104), (6, 108)])
    k.nuru(hyoushi, 'black_mid')
    # 頁（上の辺は背へ向かって沈む。下の辺も背で沈む）
    ue_h = bezier((12, 18), (26, 8), (48, 10), (64, 26))
    ue_m = bezier((64, 26), (80, 10), (102, 8), (116, 18))
    shita_m = bezier((116, 100), (100, 96), (80, 98), (64, 110))
    shita_h = bezier((64, 110), (48, 98), (28, 96), (12, 100))
    hidari = k.takaku_m(ue_h + shita_h)                                # 上の辺 → 背の谷 → 下の辺
    migi = k.takaku_m(ue_m + shita_m)
    # 左の頁は明るく、右の頁は少し暗い。背の近くは影
    k.nuru(hidari, 'paper_light')
    k.nuru(migi, 'paper')
    k.nuru(hidari & (PX > 56), 'paper')
    k.nuru(migi & (PX < 72), 'paper_dark')
    k.nuru((hidari | migi) & (np.abs(PX - 64) < 1.5), 'paper_deep')    # 背の谷
    # 頁の束（外側の端に、重なった頁の線）
    for i in range(3):
        k.nuru(hidari & (PX < 16 + i * 2) & (PX >= 15 + i * 2), 'paper_dark')
        k.nuru(migi & (PX >= 112 - i * 2) & (PX < 113 - i * 2), 'paper_dark')
    # 左の頁: 短銃の図面（線だけ）と寸法の線
    zu = [(22, 44), (52, 44), (52, 49), (34, 49), (32, 56), (26, 58), (24, 50), (22, 49), (22, 44)]
    k.orisen(zu, 'paper_deep')
    k.orisen([(30, 49), (31, 53), (34, 53)], 'paper_deep')              # 引き金の囲い
    k.orisen([(24, 44), (25, 40), (28, 40)], 'paper_deep')              # 撃鉄
    k.sen(22, 38, 52, 38, 'red_deep')                                    # 寸法の線（赤）
    k.sen(22, 36, 22, 40, 'red_deep')
    k.sen(52, 36, 52, 40, 'red_deep')
    for y in (68, 74, 80, 86):
        k.sen(20, y, 50 - (y % 3) * 3, y + 1, 'paper_deep')
    # 右の頁: 見出し・文字の行・小さな表
    k.sen(72, 36, 104, 34, 'paper_deep')
    k.sen(72, 37, 100, 35, 'paper_deep')
    for y in (46, 52, 58):
        k.sen(72, y, 106, y - 2, 'paper_deep')
    hyou = [(74, 68), (104, 66), (104, 88), (74, 90), (74, 68)]
    k.orisen(hyou, 'paper_deep')
    k.sen(74, 75, 104, 73, 'paper_deep')
    k.sen(74, 82, 104, 80, 'paper_deep')
    k.sen(88, 67, 88, 89, 'paper_deep')
    # 赤い栞（背の上から右の頁へ垂れる）
    shiori = k.takaku_m([(66, 30), (72, 27), (84, 100), (82, 106), (78, 101)])
    k.nuru(shiori & migi, 'red')
    k.nuru(shiori & migi & (PX > 77 + (PY - 30) * 0.08), 'red_deep')
    # 照りは左の頁の角（紙の光）
    k.shikaku(28, 26, 29, 27, 'highlight')
    # 経年は 1 か所（右の頁の染み）
    k.nuru(migi & k.daen_m(100, 94, 4, 2.5), 'paper_dark')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('tebiki', {'A': an_a, 'B': an_b}, '死の商人の手引書 ── 512×512 の案')
