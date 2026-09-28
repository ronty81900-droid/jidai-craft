# -*- coding: utf-8 -*-
"""
excalibur.py -- 聖剣エクスカリバー（8213）v10・512×512 の案。

  いまの絵（v6・64×64）: 左下の柄から右上の切っ先へ斜めの剣。刃は灰色、柄は革、柄頭に青い玉。
  ★ 刃は細いと縁（外側 8 マス）の色だけになる。刃の幅は 20 マス以上取る。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/excalibur.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
GOLD = ['brass_dark', 'brass', 'brass_light', 'brass_light']


def uv(cx, cy, th):
    """剣の向き（th）に沿った座標。u = 剣の長さの向き、v = 幅の向き。"""
    dx, dy = XS + 0.5 - cx, YS + 0.5 - cy
    u = dx * math.cos(th) - dy * math.sin(th)
    v = dx * math.sin(th) + dy * math.cos(th)
    return u, v


def ken(k, cx, cy, th, ha_naga, ha_haba, tsuka_naga):
    """剣1本。(cx,cy) は鍔の中心、th は切っ先の向き（ラジアン・右が 0・上が正）。"""
    u, v = uv(cx, cy, th)
    # 刃（先へ向かって細くなり、切っ先で尖る）
    w = ha_haba / 2 * np.clip(1 - np.maximum(u - ha_naga * 0.78, 0) / (ha_naga * 0.22), 0, 1)
    ha = (u > 2) & (u < ha_naga) & (np.abs(v) <= w)
    k.nuru(ha, 'iron_light')
    k.nuru(ha & (v < 0), 'steel_bright')                           # 光の当たる側の刃
    k.nuru(ha & (v > w * 0.35), 'iron')                            # 影の側
    k.nuru(ha & (np.abs(v) < 1.6) & (u > 6) & (u < ha_naga * 0.8), 'steel_light')   # 樋（中央の溝）
    k.nuru(ha & (np.abs(v + 0.6) < 0.6) & (u > 6) & (u < ha_naga * 0.8), 'iron')
    # 鍔（横に広い真鍮）
    # ★ 厚みは 18 マス（薄いと縁の暗い色だけの棒になった）
    tsuba = (np.abs(u) <= 9) & (np.abs(v) <= ha_haba * 1.0)
    k.kyuu(tsuba, cx, cy, ha_haba, ha_haba, GOLD)
    # 柄（革巻き）
    tsuka = (u < -4) & (u > -tsuka_naga) & (np.abs(v) <= ha_haba * 0.32)
    k.nuru(tsuka, 'leather')
    k.nuru(tsuka & (np.mod(u, 5) < 1.6), 'leather_deep')          # 巻きの筋
    k.nuru(tsuka & (v < -1.5), 'leather_light')
    # 柄頭（青い玉と金の座）
    ku, kv = cx - (tsuka_naga + 4) * math.cos(th), cy + (tsuka_naga + 4) * math.sin(th)
    k.kyuu(k.daen_m(ku, kv, ha_haba * 0.5, ha_haba * 0.5), ku, kv, ha_haba * 0.5, ha_haba * 0.5, GOLD)
    k.kyuu(k.daen_m(ku, kv, ha_haba * 0.3, ha_haba * 0.3), ku, kv, ha_haba * 0.3, ha_haba * 0.3,
           ['enamel_blue', 'jewel_blue', 'jewel_blue', 'glass_light'])
    return ha


def an_a():
    """案A: いまの形を起こす。左下の柄から右上の切っ先へ、太い刃・真鍮の鍔・革の柄・青い玉の柄頭。"""
    k = D.K128()
    th = math.radians(45)
    ha = ken(k, 40, 88, th, 104, 36, 18)
    # 聖剣の光（刃の上の小さな光の点）
    for (du, dv) in ((36, -3), (58, 2), (80, -2)):
        x = 40 + du * math.cos(th) + dv * math.sin(th)
        y = 88 - du * math.sin(th) + dv * math.cos(th)
        k.shikaku(int(x), int(y), int(x) + 1, int(y) + 1, 'white_light')
    x = 40 + 70 * math.cos(th)
    y = 88 - 70 * math.sin(th)
    k.daen(x - 2, y + 2, 1.6, 1.6, 'highlight')                   # 刃の照り（切っ先寄り）
    k.sen(62, 70, 66, 66, 'scuff')                                 # 経年は 1 か所（刃こぼれ）
    return k.shiage128()


def an_b():
    """案B: 岩に刺さった剣。下に大きな灰色の岩、剣はまっすぐ上に柄が出る。"""
    k = D.K128()
    # 岩（ごつごつ）
    th = np.arctan2(YS + 0.5 - 94, XS + 0.5 - 64)
    yure = 1 + 0.08 * np.sin(4 * th + 0.4) + 0.05 * np.sin(9 * th + 1.1)
    iwa = ((((XS + 0.5 - 64) / 56) ** 2 + ((YS + 0.5 - 96) / 26) ** 2) <= yure) & (YS <= 120)
    k.kyuu(iwa, 50, 84, 64, 34, ['iron_deep', 'iron', 'iron_light', 'steel_light'])
    # 岩の割れ目と苔
    k.sen(40, 92, 48, 100, 'iron_deep')
    k.sen(48, 100, 46, 108, 'iron_deep')
    k.sen(84, 96, 92, 104, 'iron_deep')
    k.nuru(iwa & (YS < 82) & ((XS < 34) | (XS > 96)), 'lens_deep')
    # 剣（まっすぐ下向きに刺さる。岩の中は見えない）
    mae = k.a.copy()
    ken(k, 64, 52, math.radians(-90), 60, 32, 20)
    k.a[iwa & (YS > 78)] = mae[iwa & (YS > 78)]                     # 岩に埋まった所は岩のまま
    # 刺さり口の影
    k.nuru(k.daen_m(64, 80, 11, 3) & iwa, 'black_mid')
    # 聖剣の光（鍔のまわり）
    for (x, y) in ((61, 64), (66, 72), (40, 96), (88, 92)):
        k.shikaku(x, y, x + 1, y + 1, 'white_light')
    k.daen(60, 60, 1.4, 1.4, 'highlight')
    k.sen(70, 106, 76, 104, 'scuff')                                # 経年は 1 か所
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('excalibur', {'A': an_a, 'B': an_b}, '聖剣エクスカリバー ── 512×512 の案')
