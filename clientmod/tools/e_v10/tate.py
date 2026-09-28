# -*- coding: utf-8 -*-
"""
tate.py -- テンプル騎士団の盾（8214）v10・512×512 の案。

  いまの絵（v6・64×64）: 少し傾いた凧形の盾。白い面に赤い十字、鉄の鋲が5つ。
  盾は少しこちらへ回っていて、右の端に木の厚みが見える。十字の右の端は影で暗い赤。
  ★ 案A は、いまの絵の形（外形・十字・鋲の位置）を 2 倍の 128 マスにそのまま写して、中を描き込む。
    （2回目で「戦車今」「エクスカリバー今」。形を変えた案は いまの絵に負けた）

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/tate.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
TETSU = ['iron_deep', 'iron', 'iron_light', 'steel_light']


def bezier(p0, p1, p2, p3, n=24):
    """3次ベジェ曲線の点（p0 から p3 まで）。"""
    ten = []
    for i in range(n + 1):
        t = i / n
        a, b, c, d = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t ** 2, t ** 3
        ten.append((a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0],
                    a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1]))
    return ten


def byou(k, cx, cy, r=4.5, teri=False):
    """鉄の鋲（丸い頭）。右下に小さな影を落とす。teri=True なら照り（highlight）を置く。"""
    k.nuru(k.daen_m(cx + 1.2, cy + 1.2, r, r) & ~k.daen_m(cx, cy, r, r), 'white_deep')
    k.kyuu(k.daen_m(cx, cy, r, r), cx, cy, r, r, TETSU)
    if teri:
        k.shikaku(int(cx - 2), int(cy - 2), int(cx - 1), int(cy - 1), 'highlight')


def migi_hashi(uchi, haba):
    """uchi の各行の右の端から haba(y) マスの帯（盾の厚みを描く所）。haba は y ごとの配列。"""
    x_migi = np.where(uchi.any(axis=1), D.N - 1 - np.argmax(uchi[:, ::-1], axis=1), -1)
    return uchi & (XS > (x_migi - haba)[:, None])


def tate_a_katachi(k):
    """いまの絵の外形を 2 倍にした凧形（上の辺は右が少し高い）。"""
    hidari = bezier((6, 34), (6, 70), (22, 110), (63, 123))       # 左の下り（下の尖りへ）
    migi = bezier((67, 123), (104, 110), (122, 70), (122, 34))    # 右の上り
    ten = [(14, 10), (114, 6), (118, 16), (122, 34)]
    ten += migi[::-1]                                               # 右の上から下へ
    ten += hidari[::-1]                                             # 下の尖りから左の上へ
    ten += [(6, 34), (9, 20)]
    return k.takaku_m(ten)


def an_a():
    """案A: いまの形を起こす。白い面・赤い十字・鉄の鋲5つ・右の端に木の厚み。"""
    k = D.K128()
    m = tate_a_katachi(k)
    # 面の明るさ: 光は左上。左の端（向こうへ回り込む所）と右の方は暗い
    t = 0.72 * (XS + 0.5 - 14) / 100 + 0.36 * (YS + 0.5 - 10) / 112
    k.nuru(m, 'white_light')
    k.nuru(m & (t > 0.42), 'white')
    k.nuru(m & (t > 0.60), 'white_deep')
    uchi = D.dotto.sou_kazoeru(m) > D.FUCHI + D.SHINCHU              # 縁（8マス）の内側
    k.nuru(uchi & ~k.zurasu(uchi, 7, 0) & (t < 0.60), 'white')      # 左の端の回り込み
    k.nuru(uchi & ~k.zurasu(uchi, 0, 4), 'white')                   # 上の端の厚み
    # 赤い十字（いまの絵と同じ位置・太さ）。右の端は影で暗い赤
    tate_bou = (XS >= 52) & (XS < 76) & (YS >= 28) & (YS < 90)
    yoko_bou = (XS >= 26) & (XS < 104) & (YS >= 48) & (YS < 68)
    juji = m & (tate_bou | yoko_bou)
    k.nuru(juji, 'red')
    k.nuru(juji & (XS >= 86), 'red_deep')
    # 右の端: 盾の厚み（木）。縁の内側の 16 マス。上と下の尖りへ向かって細くなる（いまの絵と同じ）
    y = np.arange(D.N)
    haba = 16 * np.clip((y - 14) / 10, 0, 1) * np.clip((114 - y) / 26, 0, 1)
    ki = migi_hashi(uchi, haba)
    k.nuru(ki, 'wood')
    k.nuru(ki & ~k.zurasu(ki, 2, 0), 'wood_deep')                   # 面との境の影（2マス）
    k.nuru(ki & ~k.zurasu(ki, 3, 0) & k.zurasu(ki, 2, 0), 'wood_light')   # 角の光
    for i, y0 in enumerate(range(22, 104, 7)):                      # 木目（縦に途切れる筋）
        naka = migi_hashi(uchi, haba * (0.45 + 0.2 * (i % 3)))
        k.nuru(ki & naka & ~k.zurasu(naka, 1, 0) & (YS >= y0) & (YS < y0 + 5), 'wood_deep')
    # 鉄の鋲（いまの絵と同じ 5 か所）。左上の鋲に照り
    byou(k, 32, 31, teri=True)
    byou(k, 88, 29)
    byou(k, 29, 69)
    byou(k, 92, 68)
    byou(k, 63, 96)
    # 経年は 1 か所（剣の当たった傷）
    k.sen(34, 84, 44, 76, 'scuff')
    k.sen(35, 84, 45, 76, 'white_deep')
    return k.shiage128()


def an_b():
    """案B: 騎士団の旗（上が黒・下が白の「ボーセアン」）を描いた盾に、先の広がった赤い十字（十字パテ）。
    正面から見た左右対称の凧形。小さくしても黒と白と赤の3色で見分けられる。"""
    k = D.K128()
    hidari = bezier((10, 50), (12, 88), (36, 112), (64, 124))
    migi = bezier((64, 124), (92, 112), (116, 88), (118, 50))
    ten = [(10, 8), (118, 8), (118, 50)] + migi[::-1] + hidari[::-1][1:] + [(10, 50)]
    m = k.takaku_m(ten)
    # 上 4 割は黒、下は白（光は左上）
    ue = m & (YS < 50)
    t = 0.7 * (XS + 0.5 - 10) / 108 + 0.3 * (YS + 0.5 - 8) / 116  # 光は左上
    k.nuru(m, 'white_light')
    k.nuru(m & (t > 0.5), 'white')
    k.nuru(m & (t > 0.72), 'white_deep')
    k.nuru(ue, 'black_mid')
    k.nuru(ue & (t < 0.3), 'black_light')
    k.nuru(ue & (t > 0.62), 'black')
    k.nuru(m & (YS >= 50) & (YS < 52), 'white_deep')                # 黒と白の境の影
    # 十字パテ（中心 64,56。腕は根元が細く、先が広い。辺は内側へ反る）
    cx, cy = 64, 58
    dx, dy = XS + 0.5 - cx, YS + 0.5 - cy
    ax, ay = np.abs(dx), np.abs(dy)
    ude_yoko = (ax <= 34) & (ay <= 5 + 9 * (ax / 34) ** 1.5)
    ude_tate_ue = (dy < 0) & (ay <= 30) & (ax <= 5 + 9 * (ay / 30) ** 1.5)
    ude_tate_shita = (dy >= 0) & (ay <= 40) & (ax <= 5 + 10 * (ay / 40) ** 1.5)
    juji = m & (ude_yoko | ude_tate_ue | ude_tate_shita)
    k.nuru(juji, 'red')
    k.nuru(juji & ~k.zurasu(juji, -2, -2), 'red_deep')               # 右下の縁に影
    k.nuru(juji & ~k.zurasu(juji, 2, 2), 'red_light')                # 左上の縁に光
    # 上の辺に真鍮の鋲 3 つ（左が照り）
    for i, x in enumerate((28, 64, 100)):
        k.kyuu(k.daen_m(x, 23, 4, 4), x, 23, 4, 4, ['brass_dark', 'brass', 'brass_light', 'brass_light'])
    k.shikaku(26, 21, 27, 22, 'highlight')
    # 経年は 1 か所（白い所の擦り傷）
    k.sen(34, 92, 42, 86, 'scuff')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('tate', {'A': an_a, 'B': an_b}, 'テンプル騎士団の盾 ── 512×512 の案')
