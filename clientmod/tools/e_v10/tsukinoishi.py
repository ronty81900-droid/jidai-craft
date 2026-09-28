# -*- coding: utf-8 -*-
"""
tsukinoishi.py -- 月の石（8205）v10・512×512 の案。

  いまの絵（v5・16×16）: ガラスの覆いの中に灰色の石・青緑の台・真鍮の札。
  ★ ガラスは半透明にできない（不透明度は 0 か 255 だけ）。
    覆いの中を暗い色で塗り、縁と反射の筋でガラスに見せる。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/tsukinoishi.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]


def ishi_katachi(k, cx, cy, rx, ry, seed=0.0):
    """ごつごつした石の外形（楕円の半径を角度ごとに揺らす。乱数は使わず式で決める）。"""
    th = np.arctan2(YS + 0.5 - cy, XS + 0.5 - cx)
    yure = (1 + 0.10 * np.sin(3 * th + seed) + 0.06 * np.sin(7 * th + 2 * seed)
            + 0.04 * np.sin(11 * th + 1.3 * seed))
    d = np.sqrt(((XS + 0.5 - cx) / rx) ** 2 + ((YS + 0.5 - cy) / ry) ** 2)
    return d <= yure


def kureta(k, m, cx, cy, r):
    """クレーター。中は暗く、光（左上）の反対側＝右下の内側の縁が明るい。"""
    ana = k.daen_m(cx, cy, r, r * 0.8) & m
    k.nuru(ana, 'iron')
    k.nuru(ana & k.daen_m(cx - r * 0.25, cy - r * 0.2, r * 0.8, r * 0.6), 'iron_deep')
    k.nuru(ana & ~k.daen_m(cx - r * 0.35, cy - r * 0.3, r, r * 0.8), 'steel_light')


def ishi(k, cx, cy, rx, ry, seed=0.0):
    m = ishi_katachi(k, cx, cy, rx, ry, seed)
    k.kyuu(m, cx, cy, rx * 1.1, ry * 1.2, ['iron', 'iron_light', 'steel_light', 'steel_bright'])
    for (dx, dy, r) in ((-0.35, -0.2, 0.22), (0.3, 0.1, 0.28), (-0.05, 0.4, 0.16), (0.45, -0.35, 0.13),
                        (-0.5, 0.3, 0.12)):
        kureta(k, m, cx + dx * rx, cy + dy * ry, r * min(rx, ry) * 1.3)
    return m


def an_a():
    """案A: いまの形を起こす。ガラスの覆いの中に月の石・青緑の台・真鍮の名札。"""
    k = D.K128()
    # ガラスの覆い（上が丸い筒）。★ 壁（ガラスの厚み）を太い青緑にして、ガラスに見せる（いまの絵と同じ）
    #   外側 8 マスは縁になるので、壁は 22 マスの厚みを取る（見えるのは内側の 14 マス）
    ooi = k.marukaku_m(10, 6, 118, 104, 28)
    k.nuru(ooi, 'jewel_blue')
    naka = k.marukaku_m(32, 28, 96, 104, 16)                      # 覆いの中（暗い展示の奥）
    k.nuru(naka, 'screen')
    k.nuru(naka & (XS > 84), 'black_mid')
    # ガラスの壁の陰影（左上は明るく、右は暗い）
    kabe = ooi & ~naka
    k.nuru(kabe & (XS + YS < 76), 'glass_light')
    k.nuru(kabe & (XS > 100), 'screen_light')
    k.nuru(kabe & naka.__invert__() & k.zurasu(naka, -2, -2) & ~naka, 'screen_light')   # 内側の縁に落ちる影
    # 反射（左の縦の筋2本・上の弧）
    k.nuru(kabe & (XS >= 20) & (XS <= 22) & (YS >= 34) & (YS <= 92), 'white_light')
    k.nuru(kabe & (XS >= 25) & (XS <= 25) & (YS >= 44) & (YS <= 80), 'white_light')
    yumi = kabe & k.daen_m(64, 44, 44, 30) & ~k.daen_m(64, 46, 41, 28) & (YS < 24) & (XS < 62)
    k.nuru(yumi, 'white_light')
    # 石を載せる小さな台（黒い布）
    k.nuru(k.marukaku_m(38, 88, 90, 100, 3), 'black_light')
    k.nuru(k.marukaku_m(38, 88, 90, 91, 2), 'white_deep')
    # 月の石
    ishi(k, 64, 72, 26, 18, seed=0.7)
    # 台（覆いより広い・青緑）と真鍮の名札
    dai = k.marukaku_m(8, 100, 120, 121, 4)
    k.men(dai, 'glass', 'jewel_blue', 'screen_light', haba=2)
    fuda = k.marukaku_m(48, 107, 80, 115, 1)
    k.men(fuda, 'brass_light', 'brass', 'brass_dark', haba=1)
    k.shikaku(49, 108, 50, 108, 'highlight')
    for x in range(54, 76, 3):                                    # 名札の刻み（文字の代わり）
        k.shikaku(x, 111, x + 1, 111, 'brass_deep')
    # 経年は 1 か所（台の右の擦り傷）
    k.sen(102, 113, 107, 110, 'scuff')
    return k.shiage128()


def an_b():
    """案B: 大きな月の石と、その後ろの真鍮の三日月。小さくしても「月」と「石」が読める。"""
    k = D.K128()
    # 三日月（左上・真鍮）。★ 太くする（細いと縁の色だけになり、角に見えた。1回目の失敗）
    tsuki = k.daen_m(54, 52, 44, 44) & ~k.daen_m(80, 36, 36, 36)
    k.kyuu(tsuki, 54, 52, 44, 44, ['brass_dark', 'brass', 'brass_light', 'brass_light'])
    # 月の海（うすい模様）
    k.nuru(tsuki & k.daen_m(26, 62, 5, 8), 'brass_dark')
    k.nuru(tsuki & k.daen_m(40, 84, 6, 4), 'brass_dark')
    k.nuru(tsuki & k.daen_m(22, 40, 3, 3), 'brass')
    # 月の石（右下・大きく。三日月の下の先に重なる）
    ishi(k, 80, 88, 32, 24, seed=2.1)
    m = ishi_katachi(k, 80, 88, 32, 24, seed=2.1)
    k.nuru(m & ~k.zurasu(m, 0, -3), 'iron_deep')                   # 石の下側を締める
    k.daen(68, 76, 1.6, 1.2, 'highlight')
    # 経年は 1 か所（三日月の欠け）
    k.nuru(tsuki & k.daen_m(30, 26, 3, 3), 'brass_dark')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('tsukinoishi', {'A': an_a, 'B': an_b}, '月の石 ── 512×512 の案')
