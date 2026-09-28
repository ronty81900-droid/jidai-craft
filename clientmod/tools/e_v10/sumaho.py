# -*- coding: utf-8 -*-
"""
sumaho.py -- スマートフォン（8221）v10・512×512 の案。

  いまの絵（v6・64×64）: 右へ約 28 度傾いた横長の板。暗い縁（screen）の中に画面（screen_light）、
  画面の上の縁に沿って光の筋（highlight）、右の短い辺に本体の厚み（black_light）、右下の角に擦り傷。
  ★ 案A は、いまの絵に当てはめた傾いた四角（29 度・中心 64.5,64）で描き込む。
    いまの絵は板が大きく、左右の角が絵の端（外から 6 マス）で平らに切られている。
    角丸の四角だけで小さく描いたら重なりが 0.81（1回目）、いまの大きさで角を切ったら平らな所が目立った（2回目）。
    → 半幅 54・半高 41・角の丸み 18 にして、はみ出す所だけ端で切る（重なり 0.948）。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/sumaho.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
PX, PY = XS + 0.5, YS + 0.5


def ita_uv(cx, cy, kakudo):
    """傾いた板の中の座標。u = 長い辺の向き（右が正）、v = 短い辺の向き（下が正）。"""
    th = math.radians(kakudo)
    u = (PX - cx) * math.cos(th) + (PY - cy) * math.sin(th)
    v = -(PX - cx) * math.sin(th) + (PY - cy) * math.cos(th)
    return u, v


def kakumaru(u, v, hw, hh, r):
    """板の中の角丸長方形（半幅 hw・半高 hh・角の丸み r）。"""
    qx = np.maximum(np.abs(u) - (hw - r), 0)
    qy = np.maximum(np.abs(v) - (hh - r), 0)
    return qx * qx + qy * qy <= r * r


def uv_xy(cx, cy, kakudo, u, v):
    th = math.radians(kakudo)
    return cx + u * math.cos(th) - v * math.sin(th), cy + u * math.sin(th) + v * math.cos(th)


def an_a():
    """案A: いまの形を起こす。傾いた横長の板・暗い画面・上の縁の光の筋・右の辺の厚み・前のカメラ・画面の角のひび。"""
    k = D.K128()
    cx, cy, kaku = 64.5, 64, 29
    u, v = ita_uv(cx, cy, kaku)
    m = kakumaru(u, v, 54, 41, 18) & (PX >= 6) & (PX <= 122) & (PY >= 6) & (PY <= 122)
    k.nuru(m, 'screen')
    # 右の短い辺に本体の厚み（光の当たらない側）
    k.nuru(m & (u > 40), 'black_light')
    k.nuru(m & (u > 40) & (u < 42), 'black_mid')
    # 画面（縁の内側）。ガラスの映り込みを斜めの帯で
    gamen = kakumaru(u, v, 39, 26, 5)
    k.nuru(gamen, 'screen_light')
    k.nuru(gamen & (np.abs(u * 0.5 + v + 4) < 7), 'glass_deep')
    k.nuru(gamen & (np.abs(u * 0.5 + v + 4) < 7) & (np.abs(u * 0.5 + v + 4) >= 5), 'screen_light')
    k.nuru(gamen & (u + v * 0.3 > 29), 'screen')                          # 画面の右の奥は暗い
    # 上の縁に沿った光の筋（照り）
    k.nuru(gamen & (v > -25) & (v < -22) & (u > -32) & (u < -10), 'highlight')
    # 前のカメラ（左の短い辺の縁の真ん中）と、受話口
    kx, ky = uv_xy(cx, cy, kaku, -45, 0)
    k.nuru(k.daen_m(kx, ky, 2.6, 2.6), 'black')
    k.nuru(k.daen_m(kx - 0.8, ky - 0.8, 1, 1), 'glass_light')
    k.nuru((np.abs(u + 45) < 1) & (np.abs(v - 12) < 4.5), 'black_mid')
    # 経年は 1 か所（画面の右下の角のひび）
    hx, hy = uv_xy(cx, cy, kaku, 35, 21)
    for (du, dv) in ((-8, -2), (-5, -7), (-2, -9)):
        ex, ey = uv_xy(cx, cy, kaku, 35 + du, 21 + dv)
        k.sen(int(hx), int(hy), int(ex), int(ey), 'steel_light')
    return k.shiage128()


def an_b():
    """案B: 画面の点いたスマートフォン（縦向き・左へ傾ける）。壁紙の上にアプリの絵が並び、下に並びの台。"""
    k = D.K128()
    cx, cy, kaku = 64, 64, -28
    u, v = ita_uv(cx, cy, kaku)
    m = kakumaru(u, v, 34, 52, 9)
    k.nuru(m, 'screen')
    k.nuru(m & (u > 24), 'black_light')                                    # 右の辺の厚み
    gamen = kakumaru(u, v, 22, 39, 3)
    # 壁紙（上は明るい青、下は暗い青）
    k.nuru(gamen, 'jewel_blue')
    k.nuru(gamen & (v > 4), 'glass_deep')
    k.nuru(gamen & (v > 22), 'enamel_blue')
    # 上の帯（時刻と電池）
    k.nuru(gamen & (v < -32) & (np.abs(u + 12) < 5), 'white_light')
    k.nuru(gamen & (v < -32) & (np.abs(u - 14) < 3), 'white_light')
    k.nuru(gamen & (np.abs(v + 34.5) < 1.5) & (np.abs(u - 14) < 1.5), 'lens_light')
    # アプリの絵（3 列 × 3 段）と、下の並びの台（3 つ）
    iro = ['red_light', 'brass_light', 'lens_light', 'jewel_red', 'white_light', 'hot_light',
           'enamel_green', 'liquid_light', 'steel_bright']
    for i, c in enumerate(iro):
        au, av = -13 + (i % 3) * 13, -21 + (i // 3) * 14
        k.nuru(kakumaru(u - au, v - av, 5, 5, 1.5), c)
    k.nuru(gamen & (v > 25) & (v < 36) & (np.abs(u) < 19), 'screen_light')
    for i, c in enumerate(('lens', 'red', 'brass')):
        k.nuru(kakumaru(u - (-13 + i * 13), v - 30.5, 4.5, 4.5, 1.5), c)
    # 画面の上の穴のカメラ
    kx, ky = uv_xy(cx, cy, kaku, 0, -45)
    k.nuru(k.daen_m(kx, ky, 2.4, 2.4), 'black')
    # 照り（画面の左上のガラス）
    hx, hy = uv_xy(cx, cy, kaku, -17, -27)
    k.nuru(k.daen_m(hx, hy, 1.6, 1.6), 'highlight')
    # 経年は 1 か所（縁の角の擦れ）
    ex, ey = uv_xy(cx, cy, kaku, 28, 46)
    k.nuru(m & k.daen_m(ex, ey, 2.5, 2), 'iron')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('sumaho', {'A': an_a, 'B': an_b}, 'スマートフォン ── 512×512 の案')
