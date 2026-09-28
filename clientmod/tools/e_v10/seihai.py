# -*- coding: utf-8 -*-
"""
seihai.py -- 聖杯の欠片（8202）v10・512×512 の案。

  いまの絵（v8 で確定・64×64）= C2「聖杯の全体＋不規則な欠け4か所＋ひび3本」（tools/e_v8/seihai.py の an_c）。
  金の杯・中は暗い・青い宝石3つの帯・節のある脚・広い台座。上縁の左右と器の右と台座の右が欠けている。
  ★ 案A は、an_c() の座標をそのまま 2 倍にして 128 マスで描く（形を変えない）。宝石の台座と粒だけ足す。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/seihai.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
PX, PY = XS + 0.5, YS + 0.5
TOUMEI = D.dotto.TOUMEI


def houseki(k, cx, cy, r, dai=True):
    """青い宝石（左上に小さな光）。dai=True なら金の台座（左上が明るい輪）も付ける。"""
    if dai:
        k.nuru(k.daen_m(cx, cy, r + 2.5, r + 2.5), 'brass_deep')
        k.nuru(k.daen_m(cx - 0.8, cy - 0.8, r + 2, r + 2) & ~k.daen_m(cx, cy, r, r), 'brass_light')
    k.daen(cx, cy, r, r, 'jewel_blue')
    k.daen(cx + r * 0.35, cy + r * 0.35, r * 0.75, r * 0.75, 'enamel_blue')
    k.daen(cx - r * 0.35, cy - r * 0.35, max(1, r * 0.35), max(1, r * 0.35), 'glass_light')


def hai(k, kakeru=True):
    """聖杯（v8 の an_c を 2 倍）。kakeru=False なら欠けとひびを入れない。"""
    # 器（楕円の下半分）
    m = k.daen_m(64, 20, 48, 60) & (YS >= 20)
    k.nuru(m, 'brass_dark')
    k.nuru(k.daen_m(58, 16, 40, 54) & (YS >= 20), 'brass')
    k.nuru(k.daen_m(50, 12, 24, 44) & (YS >= 28), 'brass_light')
    # 上縁の楕円（中は暗い）
    k.daen(64, 20, 48, 12, 'brass_light')
    k.daen(64, 20, 40, 8, 'brass_deep')
    k.daen(66, 22, 34, 5, 'wood_deep')
    # 節と脚
    k.daen(64, 80, 16, 8, 'brass_dark')
    k.shikaku(54, 80, 75, 105, 'brass_dark')
    k.shikaku(56, 80, 63, 105, 'brass')
    k.shikaku(70, 80, 75, 105, 'brass_deep')
    k.daen(64, 92, 14, 6, 'brass')
    k.daen(60, 90, 6, 3, 'brass_light')
    for x in (54, 60, 66, 72):                                             # 節の粒
        k.shikaku(x, 95, x + 1, 96, 'brass_deep')
    # 台座
    k.daen(64, 112, 44, 12, 'brass_dark')
    k.daen(60, 108, 36, 8, 'brass')
    k.daen(52, 106, 16, 4, 'brass_light')
    k.daen(64, 116, 44, 8, 'brass_deep')
    k.daen(64, 114, 40, 6, 'brass_dark')
    # 宝石の帯（宝石のあいだに金の粒）
    k.nuru(k.takaku_m([(0, 40), (128, 40), (128, 56), (0, 56)]) & k.daen_m(64, 20, 48, 60), 'brass_deep')
    k.nuru(k.takaku_m([(0, 42), (128, 42), (128, 54), (0, 54)]) & k.daen_m(64, 20, 44, 56), 'brass_dark')
    for cx in (40, 64, 88):
        houseki(k, cx, 48, 7)
    for cx in (52, 76):
        k.shikaku(cx - 1, 47, cx, 48, 'brass_light')
    if kakeru:
        # 欠け（不規則に 4 か所）: 上縁の右（大）・上縁の左（小）・器の右側面・台座の右端
        k.takaku([(80, 4), (116, 4), (116, 28), (108, 22), (104, 34), (96, 20), (90, 28), (84, 14)], TOUMEI)
        k.takaku([(18, 10), (36, 4), (34, 18), (28, 16), (24, 26)], TOUMEI)
        k.takaku([(116, 50), (94, 58), (102, 66), (90, 74), (116, 78)], TOUMEI)
        k.takaku([(84, 126), (94, 116), (100, 120), (106, 110), (116, 114), (116, 126)], TOUMEI)
        # ひび（欠けから走る）
        k.orisen([(92, 30), (88, 42), (92, 54), (84, 66), (86, 74)], 'brass_deep', 1)
        k.orisen([(26, 26), (32, 36), (28, 48)], 'brass_deep', 1)
        k.orisen([(36, 112), (46, 116), (42, 122)], 'brass_deep', 1)
    return m


def an_a():
    """案A: いまの形を起こす（v8 の C2 を 2 倍）。金の杯・青い宝石の帯・節のある脚・欠け4か所とひび。"""
    k = D.K128()
    hai(k)
    k.daen(44, 30, 5, 3.5, 'highlight')                                   # 反射
    return k.shiage128()


def an_b():
    """案B: ステンドグラスの窓の聖杯。尖ったアーチの窓・青いガラス・赤い縁取り・杯の後ろに光の輪。
    小さくしても「青い窓に金の杯」と読める。"""
    k = D.K128()
    # 尖ったアーチ（2 つの弧が頂点で出会う）＋ 下の四角
    ys = 66
    r = 56.7
    mado = ((PX >= 16) & (PX <= 112) & (PY >= ys) & (PY <= 122)) | \
        (((PX - 72.7) ** 2 + (PY - ys) ** 2 <= r * r) & ((PX - 55.3) ** 2 + (PY - ys) ** 2 <= r * r) & (PY < ys + 1))
    k.nuru(mado, 'enamel_blue')
    # 赤い縁取りのガラス（縁の内側）
    sou = D.dotto.sou_kazoeru(mado)
    k.nuru(mado & (sou > 8) & (sou <= 15), 'red')
    k.nuru(mado & (sou > 8) & (sou <= 15) & (PX + PY > 140), 'red_deep')
    k.nuru(mado & (sou == 16), 'black_mid')                                # 鉛の線（縁取りの内側）
    # 青いガラスを鉛の線で分ける（斜めの格子）
    naka = mado & (sou > 16)
    k.nuru(naka & ((PX + PY) % 22 < 11) & ((PX - PY) % 26 < 13), 'jewel_blue')
    k.nuru(naka & ((np.abs((PX + PY) % 22) < 1) | (np.abs((PX - PY) % 26) < 1)), 'black_mid')
    # 杯の後ろの光の輪（淡いガラスの円盤。鉛の線で囲む）
    k.nuru(k.daen_m(64, 46, 27, 27) & naka, 'black_mid')
    k.nuru(k.daen_m(64, 46, 26, 26) & naka, 'hada_light')
    k.nuru(k.daen_m(64, 46, 26, 26) & ~k.daen_m(62, 44, 24, 24) & naka, 'paper_light')
    # 聖杯（窓の中に小さく。鉛の線で縁取る）
    ue, hx, hy = 40, 18, 20
    hai_m = (k.daen_m(64, ue, hx, hy) & (PY >= ue)) | k.daen_m(64, ue, hx, 4.5)
    hai_m |= (PX >= 60) & (PX < 68) & (PY >= ue + hy - 2) & (PY < 90)
    hai_m |= k.daen_m(64, 92, 15, 5)
    k.nuru(hai_m | k.zurasu(hai_m, 1, 0) | k.zurasu(hai_m, -1, 0) | k.zurasu(hai_m, 0, 1) |
           k.zurasu(hai_m, 0, -1), 'black_mid')
    k.nuru(hai_m, 'brass')
    k.nuru(hai_m & (PX < 58), 'brass_light')
    k.nuru(hai_m & (PX > 72), 'brass_dark')
    k.nuru(k.daen_m(64, ue, hx - 3, 3), 'wood_deep')                       # 杯の中
    k.nuru(hai_m & (PY >= ue + 8) & (PY < ue + 13), 'brass_deep')          # 宝石の帯
    for cx in (56, 64, 72):
        k.daen(cx, ue + 10.5, 2.2, 2.2, 'jewel_blue')
    k.nuru(k.daen_m(64, 76, 6, 2.5) & hai_m, 'brass_dark')                 # 節
    k.nuru(k.daen_m(54, ue + 5, 2.2, 1.8), 'highlight')
    # 青いガラスの中に、赤いかけらを少し（窓らしく）
    for (x, y) in ((34, 100), (92, 78), (40, 76), (88, 104)):
        k.nuru(naka & k.daen_m(x, y, 3.5, 3.5) & ~hai_m, 'jewel_red')
    # 経年は 1 か所（ガラスのひび）
    k.orisen([(96, 90), (90, 98), (94, 106)], 'black_mid', 1)
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('seihai', {'A': an_a, 'B': an_b}, '聖杯の欠片 ── 512×512 の案')
