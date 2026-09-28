# -*- coding: utf-8 -*-
"""
sensha.py -- ヒッタイトの戦車（8211）v10・512×512 の案。

  いまの絵（v6・64×64）: 大きな車輪・革の車体・右上へ伸びる棒。
  ★ v7 のご指摘「戦車が大砲にしか見えない」。棒を斜め上へ伸ばすと砲身に見える。
    轅（ながえ）は水平に伸ばし、先に軛（くびき）を付ける。
  ★ ヒッタイトの戦車の車輪は輻（や）が 6 本。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/sensha.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]


def sharin(k, cx, cy, r):
    """6本輻の車輪。木の輪・真鍮の轂（こしき）・輻のあいだは暗い窪み。"""
    m = k.daen_m(cx, cy, r, r)
    k.men(m, 'wood_light', 'wood', 'wood_deep', haba=2)
    rr = np.hypot(XS + 0.5 - cx, YS + 0.5 - cy)
    th = np.arctan2(-(YS + 0.5 - cy), XS + 0.5 - cx)
    f = (th / (2 * math.pi) * 6 + 0.25) % 1.0
    mado = (rr < r - 5) & (rr > 6.5) & (f > 0.2) & (f < 0.8)
    k.nuru(mado, 'black_mid')
    k.nuru(mado & ~k.zurasu(mado, -1, -1), 'wood_deep')
    k.nuru(mado & ~k.zurasu(mado, 1, 1), 'black')
    # 輪の鋲
    for i in range(12):
        a = math.radians(i * 30 + 15)
        x, y = cx + (r - 2.5) * math.cos(a), cy - (r - 2.5) * math.sin(a)
        k.shikaku(int(x), int(y), int(x), int(y), 'brass')
    k.kyuu(k.daen_m(cx, cy, 6, 6), cx, cy, 6, 6, ['brass_deep', 'brass_dark', 'brass', 'brass_light'])
    k.daen(cx, cy, 1.6, 1.6, 'brass_deep')


def shatai(k, x0, x1, y0, y1):
    """車体（革を張った箱・前が丸く立ち上がる手すり・真鍮の鋲）。"""
    m = k.takaku_m([(x0, y1), (x1, y1), (x1 + 7, y0 + 14), (x1 + 1, y0), (x0 + 5, y0)])
    k.men(m, 'leather_light', 'leather', 'leather_deep', haba=2)
    # 木の枠（上の縁と、前の手すり）
    k.nuru(m & (YS >= y0) & (YS <= y0 + 2), 'wood_light')
    k.nuru(m & (YS >= y1 - 2), 'wood_deep')
    # 模様（赤い帯と真鍮の鋲）
    k.nuru(m & (YS >= y0 + 9) & (YS <= y0 + 11), 'red')
    for x in range(x0 + 6, x1 + 2, 6):
        if m[y0 + 5, x]:
            k.shikaku(x, y0 + 5, x + 1, y0 + 6, 'brass_light')
            k.ten(x + 1, y0 + 6, 'brass_dark')
    return m


def ya_zutsu(k, x, y):
    """背の矢筒（革）と矢羽。"""
    k.nuru(k.takaku_m([(x, y + 34), (x + 10, y + 36), (x + 14, y + 4), (x + 5, y + 2)]), 'leather_deep')
    k.nuru(k.takaku_m([(x + 2, y + 32), (x + 7, y + 33), (x + 11, y + 6), (x + 6, y + 5)]), 'leather')
    for (dx, c) in ((3, 'red_light'), (7, 'white'), (11, 'red_light')):
        k.nuru(k.takaku_m([(x + dx + 2, y + 4), (x + dx + 6, y + 4), (x + dx + 6, y - 6), (x + dx + 3, y - 3)]), c)


def an_a():
    """案A: いまの形を起こす。大きな6本輻の車輪・前が丸く立ち上がる胸板（D字）の車体・前やや下へ伸びる轅と軛。
    ★ 部分はどれも 16 マス以上の太さにする（細いと縁の暗い色だけになる。1回目の失敗）。
    ★ 上を向いた棒は砲身に見える（v7 のご指摘と、ここでの2回目の失敗）。轅は前やや下へ。"""
    k = D.K128()
    # 轅（ながえ）: 車体の前から水平に右へ、太い板。先に軛（くびき）の山
    naga = k.takaku_m([(70, 44), (112, 40), (112, 60), (70, 64)])
    k.men(naga, 'wood_light', 'wood', 'wood_deep', haba=2)
    k.nuru(naga & (XS >= 84) & (XS <= 86), 'brass_dark')           # 轅の金具（帯）
    k.nuru(naga & (XS >= 98) & (XS <= 100), 'brass_dark')
    kubiki = k.takaku_m([(100, 14), (122, 14), (122, 30), (116, 40), (106, 40), (100, 30)])
    k.men(kubiki, 'wood_light', 'wood', 'wood_deep', haba=2)
    # 車体（前が丸く立ち上がる。縦に大きく）
    shatai(k, 12, 80, 14, 62)
    # 車体の真鍮の飾り金具（前の丸い立ち上がりに）
    k.kyuu(k.daen_m(74, 34, 6, 6), 74, 34, 6, 6, ['brass_deep', 'brass_dark', 'brass', 'brass_light'])
    # 車輪（大きく・下に）
    sharin(k, 44, 82, 34)
    # 経年は 1 か所（車体の擦れ）
    k.sen(58, 40, 64, 37, 'scuff')
    k.daen(38, 74, 1.6, 1.2, 'highlight')
    return k.shiage128()


def uma(k, cx, cy):
    """走る馬（横向き・右へ）。★ 縁の決まりに合わせて、玩具のように太く描く。
    脚は前と後ろを1本ずつの太い塊にし（16 マス前後）、首と頭も太く。"""
    kage, ji, hikari = 'wood_deep', 'wood', 'wood_light'
    dou = k.marukaku_m(cx - 26, cy - 14, cx + 22, cy + 14, 13)
    kubi = k.takaku_m([(cx + 6, cy - 10), (cx + 22, cy + 2), (cx + 36, cy - 26), (cx + 22, cy - 38)])
    atama = k.marukaku_m(cx + 22, cy - 46, cx + 50, cy - 26, 8)
    mae_ashi = k.takaku_m([(cx + 6, cy + 6), (cx + 22, cy + 6), (cx + 28, cy + 40), (cx + 12, cy + 40)])
    ato_ashi = k.takaku_m([(cx - 24, cy + 4), (cx - 8, cy + 8), (cx - 18, cy + 40), (cx - 34, cy + 38)])
    shippo = k.takaku_m([(cx - 22, cy - 10), (cx - 40, cy - 6), (cx - 42, cy + 12), (cx - 26, cy + 6)])
    m = dou | kubi | atama | mae_ashi | ato_ashi | shippo
    k.nuru(m, ji)
    k.kyuu(dou | kubi | atama, cx, cy - 12, 44, 34, [kage, ji, ji, hikari])
    k.men(mae_ashi, ji, kage, 'leather_deep', haba=1)
    k.men(ato_ashi, ji, kage, 'leather_deep', haba=1)
    k.nuru(shippo, 'black_mid')
    tategami = k.takaku_m([(cx + 20, cy - 40), (cx + 28, cy - 44), (cx + 16, cy - 6), (cx + 8, cy - 8)])
    k.nuru(tategami, 'black_mid')
    k.nuru(tategami & ~k.zurasu(tategami, 1, 1), 'black_light')
    k.shikaku(cx + 36, cy - 40, cx + 38, cy - 38, 'black')        # 目
    k.shikaku(cx + 46, cy - 32, cx + 47, cy - 31, 'black_mid')    # 鼻
    # 馬具（赤い胸帯と頭絡）
    k.nuru(m & (np.abs((XS + 0.5 - (cx + 14)) - (YS + 0.5 - cy) * 0.55) < 2.5)
           & (YS > cy - 20) & (YS < cy + 10), 'red')
    k.nuru(atama & (np.abs(XS + 0.5 - (cx + 32)) < 1.6), 'red_deep')
    return m


def an_b():
    """案B: 丸い青銅の円盤に、馬が引く戦車を浮き彫りにした物（ヒッタイトの浮き彫り風）。
    ★ 絵柄は円盤の内側に描くので、細い馬の脚も縁の色に埋まらない。"""
    k = D.K128()
    cx, cy, r = 64, 64, 58
    enban = k.daen_m(cx, cy, r, r)
    k.kyuu(enban, cx, cy, r, r, ['brass_deep', 'brass_dark', 'brass_dark', 'brass'])
    # 内側の地（一段くぼんだ暗い青銅）と、その縁の刻み
    ji = k.daen_m(cx, cy, r - 13, r - 13)
    k.nuru(ji, 'leather_deep')
    k.nuru(ji & ~k.zurasu(ji, 2, 2), 'black_mid')                 # 左上の内壁は影
    k.nuru(ji & ~k.zurasu(ji, -2, -2), 'brass_deep')              # 右下の内壁は光
    for i in range(24):
        a = math.radians(i * 15)
        x, y = cx + (r - 10.5) * math.cos(a), cy - (r - 10.5) * math.sin(a)
        k.shikaku(int(x), int(y), int(x) + 1, int(y) + 1, 'brass_light' if i % 2 == 0 else 'brass_deep')
    # 浮き彫り（明るい真鍮）: 馬（右）と戦車（左）と御者
    e = 'brass'
    hi = 'brass_light'
    # 馬の胴・首・頭
    k.nuru(k.marukaku_m(62, 58, 88, 70, 6), e)
    k.takaku([(80, 60), (88, 64), (96, 48), (90, 44)], e)
    k.nuru(k.marukaku_m(88, 40, 102, 50, 3), e)
    k.takaku([(90, 41), (93, 34), (95, 41)], e)                  # 耳
    # 馬の脚（走る。細くてよい）
    for (x0, y0, x1, y1) in ((84, 68, 92, 82), (80, 69, 80, 84), (66, 68, 60, 82), (70, 69, 72, 84)):
        k.sen(x0, y0, x1, y1, e, futosa=2)
    k.takaku([(62, 60), (54, 62), (52, 70), (60, 66)], e)        # 尾
    # 戦車の車輪（6本輻）と車体
    k.wa(44, 76, 11, 8.5, e)
    for i in range(6):
        a = math.radians(i * 60)
        k.sen(44, 76, 44 + 9 * math.cos(a), 76 - 9 * math.sin(a), e)
    k.daen(44, 76, 2.2, 2.2, hi)
    k.takaku([(34, 56), (56, 58), (54, 70), (36, 70)], e)
    k.sen(54, 66, 66, 64, e, futosa=2)                           # 轅
    # 御者（弓を引く）
    k.daen(46, 44, 3.2, 3.2, e)
    k.takaku([(42, 48), (50, 48), (52, 58), (40, 58)], e)
    k.sen(50, 50, 58, 44, e, futosa=2)
    k.wa(58, 46, 7, 5.8, e)                                        # 弓
    # 浮き彫りの光（左上の縁を明るく）
    rel = (k.a[:, :, :3] == np.array(D.iro(e)[:3])).all(axis=2)
    k.nuru(rel & ~k.zurasu(rel, 1, 1), hi)
    k.nuru(rel & ~k.zurasu(rel, -1, -1), 'brass_dark')
    k.sen(84, 84, 90, 81, 'scuff')                                 # 経年は 1 か所
    k.daen(95, 44, 1.2, 1.0, 'highlight')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('sensha', {'A': an_a, 'B': an_b}, 'ヒッタイトの戦車 ── 512×512 の案')
