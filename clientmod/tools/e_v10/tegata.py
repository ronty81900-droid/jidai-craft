# -*- coding: utf-8 -*-
"""
tegata.py -- 武器商人の手形（8215）v10・512×512 の案。

  いまの絵（v6・64×64）: 四つ折りの跡のある紙（少し左へ回っている）。左上は明るく、右半分は暗い。
  文字の行が2本、横の折り目、右下に赤い封蝋（右下は紙からはみ出す）とリボン、右上の角に小さな赤い印。
  ★ 案A は、いまの絵の形（紙の四隅・封蝋の位置と大きさ）を 2 倍の 128 マスに写して、中を描き込む。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/tegata.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
PX, PY = XS + 0.5, YS + 0.5


def kami_uv(o, U, V):
    """平行四辺形の紙の中の座標 (u, v)。o = 左上の角、U = 上の辺、V = 左の辺（どれも 128 マス）。"""
    det = U[0] * V[1] - U[1] * V[0]
    dx, dy = PX - o[0], PY - o[1]
    u = (dx * V[1] - dy * V[0]) / det
    v = (U[0] * dy - U[1] * dx) / det
    return u, v


def uv_xy(o, U, V, u, v):
    return o[0] + u * U[0] + v * V[0], o[1] + u * U[1] + v * V[1]


def moji_gyou(k, o, U, V, u0, u1, v, c='paper_deep', seed=0):
    """手書きの 1 行（語ごとに切れる波線）。紙の傾きに沿わせる。"""
    u = u0
    i = 0
    while u < u1:
        naga = 0.05 + 0.06 * ((math.sin(seed * 7.1 + i * 2.3) + 1) / 2)
        e = min(u + naga, u1)
        ten = []
        n = max(2, int((e - u) * 60))
        for j in range(n + 1):
            uu = u + (e - u) * j / n
            vv = v - 0.008 * math.sin(uu * 90 + seed + i)        # 字の上下の揺れ
            ten.append(uv_xy(o, U, V, uu, vv))
        for a, b in zip(ten, ten[1:]):
            k.sen(int(a[0]), int(a[1]), int(b[0]), int(b[1]), c)
        u = e + 0.03
        i += 1


def fuuro(k, cx, cy, r, emblem=True):
    """赤い封蝋。縁は蝋が垂れたように揺らす。平らな面に、縁だけ光（左上）と影（右下）。
    中に押した輪と、交差した2本の剣（武器商人の印）。"""
    th = np.arctan2(PY - cy, PX - cx)
    d = np.hypot(PX - cx, PY - cy)
    rr = r * (1 + 0.06 * np.sin(5 * th + 0.5) + 0.04 * np.sin(9 * th + 1.7))
    m = d <= rr
    k.men(m, 'red_light', 'red', 'red_deep', haba=2)
    wa = (d <= r * 0.86) & (d > r * 0.78)                        # 押した輪（右下が影、左上が光）
    k.nuru(wa, 'red_deep')
    k.nuru(wa & (PX - cx + PY - cy < -r * 0.3), 'red_light')
    if emblem:
        # 交差した2本の剣。★ 2マスの太さ・ずらした影・長い鍔で描いたら格子模様に崩れ、
        #   縦の短剣にしたら逆さの十字に見えた。→ 1マスの斜めの線（45度はドットがきれいに並ぶ）と短い鍔
        s = round(r * 0.45)
        x0, y0 = int(cx), int(cy)
        k.sen(x0 - s, y0 + s, x0 + s, y0 - s, 'red_deep')              # 剣1（左下の柄 → 右上の切っ先）
        k.sen(x0 + s, y0 + s, x0 - s, y0 - s, 'red_deep')              # 剣2（右下の柄 → 左上の切っ先）
        g = max(2, round(s * 0.3))
        k.sen(x0 - s + g - 2, y0 + s - g - 2, x0 - s + g + 2, y0 + s - g + 2, 'red_deep')   # 剣1の鍔
        k.sen(x0 + s - g - 2, y0 + s - g + 2, x0 + s - g + 2, y0 + s - g - 2, 'red_deep')   # 剣2の鍔
    return m


def an_a():
    """案A: いまの形を起こす。四つ折りの跡・手書きの行・右下の赤い封蝋とリボン・右上の赤い印。"""
    k = D.K128()
    o, U, V = (5, 13), (96, -8), (10, 92)
    u, v = kami_uv(o, U, V)
    kami = (u >= 0) & (u <= 1) & (v >= 0) & (v <= 1)
    # 封蝋のはみ出す所（紙の外。外側は縁の真鍮と outline になる）
    fx, fy = 96, 97
    soto = np.hypot(PX - fx, PY - fy) <= 25
    k.nuru(soto, 'brass')
    # 四つ折りの面（光は左上。右半分は向こうへ傾いて暗い）
    k.nuru(kami & (u < 0.5) & (v < 0.47), 'paper_light')
    k.nuru(kami & (u >= 0.5), 'paper_dark')
    k.nuru(kami & (u < 0.5) & (v >= 0.47), 'paper')
    k.nuru(kami & ((u < 0.05) | (v < 0.06)) & (u < 0.5), 'paper')      # 左と上の端の反り
    # 折り目（縦と横。山の片側に光、片側に影）
    k.nuru(kami & (np.abs(u - 0.5) < 0.012), 'paper_deep')
    k.nuru(kami & (u >= 0.512) & (u < 0.53), 'paper')
    k.nuru(kami & (np.abs(v - 0.47) < 0.012), 'paper_deep')
    k.nuru(kami & (v >= 0.482) & (v < 0.5) & (u < 0.5), 'paper_light')
    # 手書きの行（左上に見出し2行、右上に3行、左下に金額と署名）
    moji_gyou(k, o, U, V, 0.1, 0.44, 0.2, seed=1)
    moji_gyou(k, o, U, V, 0.1, 0.40, 0.29, seed=2)
    moji_gyou(k, o, U, V, 0.1, 0.36, 0.38, seed=3)
    moji_gyou(k, o, U, V, 0.58, 0.8, 0.29, seed=4)
    moji_gyou(k, o, U, V, 0.58, 0.9, 0.38, seed=5)
    moji_gyou(k, o, U, V, 0.1, 0.3, 0.6, seed=6)
    # 署名（輪を3つ続けた筆記体と、下の払い）
    sx, sy = uv_xy(o, U, V, 0.12, 0.8)
    ten = [(sx + 26 * t - 3 * math.sin(t * 6 * math.pi), sy - 4 * math.cos(t * 6 * math.pi) * (1 - 0.4 * t) - 3 * t)
           for t in np.linspace(0, 1, 40)]
    k.orisen([(int(round(a)), int(round(b))) for a, b in ten], 'paper_deep')
    k.orisen([(int(sx - 2), int(sy + 6)), (int(sx + 14), int(sy + 5)), (int(sx + 30), int(sy + 1))], 'paper_deep')
    # 右上の角の小さな赤い印（印紙）
    ix, iy = uv_xy(o, U, V, 0.84, 0.1)
    inshi = k.marukaku_m(ix - 5, iy - 5, ix + 5, iy + 5, 1)
    k.nuru(inshi & kami, 'red_deep')
    k.nuru(inshi & kami & k.marukaku_m(ix - 3, iy - 3, ix + 3, iy + 3, 1), 'red')
    k.nuru(inshi & kami & (np.abs(PX - ix) < 0.8), 'red_deep')
    # リボン（封蝋の下から左上の切り込みへ）
    for w, c in ((3.2, 'red_deep'), (1.2, 'red')):
        riboon = (np.abs((PX - 72) * 0.8 - (PY - 58) * 0.6) <= w) & (PY >= 58) & (PY <= 92)
        k.nuru(riboon & kami, c)
    k.sen(66, 56, 76, 58, 'paper_deep')                             # 切り込み
    # 封蝋
    fuuro(k, fx, fy, 17)
    k.shikaku(86, 88, 87, 89, 'highlight')
    # 経年は 1 か所（左の辺の小さな破れ）
    k.nuru(kami & k.daen_m(*uv_xy(o, U, V, 0.09, 0.66), 3, 2), 'paper_deep')
    return k.shiage128()


def an_b():
    """案B: 封をした手形（折りたたんで蝋で閉じた書状）。真ん中に赤い封蝋、左右に赤い紐。"""
    k = D.K128()
    o, U, V = (10, 13), (104, 6), (-4, 95)
    u, v = kami_uv(o, U, V)
    kami = (u >= 0) & (u <= 1) & (v >= 0) & (v <= 1)
    k.nuru(kami, 'paper')
    # 折り返し（上のかぶせ・左右の三角・下）。光は左上
    ue = kami & (v < 0.55 - np.abs(u - 0.5) * 0.9)
    hidari = kami & (u < 0.5) & ~ue & (v < 0.5 + (0.5 - u) * 0.8) & (v > 0.5 - (0.5 - u) * 0.8)
    migi = kami & (u >= 0.5) & ~ue & (v < 0.5 + (u - 0.5) * 0.8) & (v > 0.5 - (u - 0.5) * 0.8)
    shita = kami & ~ue & ~hidari & ~migi
    k.nuru(shita, 'paper')
    k.nuru(hidari, 'paper_light')
    k.nuru(migi, 'paper_dark')
    k.nuru(ue, 'paper_light')
    k.nuru(ue & ~k.zurasu(ue, 0, -2) & ~(v < 0.02), 'paper_deep')      # かぶせの下の影
    for m, c in ((hidari, 'paper'), (migi, 'paper_deep')):
        k.nuru(m & ~k.zurasu(m, 0, -1), c)
    # 赤い紐（横に1本）
    himo = kami & (np.abs(v - 0.55) < 0.05)
    k.nuru(himo, 'red')
    k.nuru(himo & (v > 0.57), 'red_deep')
    # 封蝋（真ん中）
    cx, cy = uv_xy(o, U, V, 0.5, 0.55)
    fuuro(k, cx, cy, 19)
    k.shikaku(int(cx - 10), int(cy - 10), int(cx - 9), int(cy - 9), 'highlight')
    # 経年は 1 か所（右下の角の折れ）
    kx, ky = uv_xy(o, U, V, 0.97, 0.95)
    k.nuru(kami & k.daen_m(kx, ky, 5, 4), 'paper_deep')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('tegata', {'A': an_a, 'B': an_b}, '武器商人の手形 ── 512×512 の案')
