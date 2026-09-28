# -*- coding: utf-8 -*-
"""
gofu.py -- 古びた護符（8201）v10・512×512 の案。

  いまの絵（v8 で確定・64×64）= ③「土色の石に、青銅の渦巻き円盤をはめ込む」（tools/e_v8/gofu2.py の hamekomi）。
  卵形の石・上に紐穴と革紐の結び目・真ん中に青銅の円盤（渦巻きの彫り）・右下に土汚れ。
  ★ 案A は、gofu2.py の座標をそのまま 2 倍にして 128 マスで描く（形を変えない）。
    64 マスでは紐穴の縁（真鍮）が首いっぱいに広がっていた。128 マスでは縁が相対的に細くなるので、
    穴のまわりに青銅の座金を描いて、同じ見え方に寄せる。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/gofu.py
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


# ---- 案A: gofu2.py の hamekomi() を 2 倍に ----
def karada(k, c, dx=0, dy=0, s=0):
    """卵形（下が丸く、上がすぼんで紐の結びへ）。dx,dy でずらし s で縮める（陰影用）。座標は 128 マス。"""
    k.daen(64 + dx, 84 + dy, 48 - s, 38 - s, c)
    k.takaku([(24 + s + dx, 68 + dy), (104 - s + dx, 68 + dy),
              (80 - s + dx, 10 + s + dy), (48 + s + dx, 10 + s + dy)], c)
    k.daen(64 + dx, 14 + s + dy, 16 - s, 10 - s, c)


def kage(k, kage_iro, moto, hikari):
    """左上が明るく右下が暗い 3 段。"""
    karada(k, kage_iro)
    karada(k, moto, -4, -4, 4)
    karada(k, hikari, -10, -10, 14)


def himo(k):
    """紐穴（透明・縁が自動で金具になる）と、穴の上で結んだ革紐。"""
    # 穴のまわりの青銅の座金（64 マスの時の見え方＝首の真ん中が真鍮、に寄せる）
    k.nuru(k.daen_m(64, 46, 19, 19) & ~k.daen_m(64, 46, 15, 15), 'brass_dark')
    k.nuru(k.daen_m(63, 45, 19, 19) & ~k.daen_m(64, 46, 15, 15) & (PX + PY < 110), 'brass')
    # 結び目（大きめ）と、左右へ出る紐の端
    k.daen(64, 22, 16, 9, 'leather')
    k.daen(60, 19, 8, 4, 'leather_light')
    k.nuru(k.daen_m(64, 22, 16, 9) & ~k.daen_m(63, 21, 15, 8), 'leather_deep')   # 結び目の右下の影
    k.shikaku(52, 28, 77, 31, 'leather_deep')
    for i in range(3):                                                  # 結び目の巻き（斜めの筋）
        k.sen(54 + i * 8, 26, 58 + i * 8, 16, 'leather_deep')
    k.orisen([(50, 20), (44, 14)], 'leather_deep', 6)
    k.orisen([(78, 20), (84, 14)], 'leather_deep', 6)
    # 穴へ入る 2 本の紐
    k.orisen([(56, 30), (58, 36)], 'leather_deep', 6)
    k.orisen([(72, 30), (70, 36)], 'leather_deep', 6)
    k.orisen([(56, 30), (58, 34)], 'leather', 2)
    k.orisen([(72, 30), (70, 34)], 'leather', 2)
    # 紐穴
    k.daen(64, 46, 7, 7, TOUMEI)


def hanten(k, iro_kuro, iro_shiro):
    """石らしい斑点（位置は固定）。渦巻きの外側だけ。"""
    for x, y in ((28, 80), (38, 100), (48, 114), (94, 72), (102, 88), (88, 60), (40, 72), (98, 108),
                 (56, 60), (76, 56), (30, 92), (106, 78), (70, 118)):
        k.shikaku(x, y, x + 3, y + 1, iro_kuro)
    for x, y in ((32, 90), (100, 80), (52, 108), (84, 66), (36, 64)):
        k.shikaku(x, y, x + 1, y + 1, iro_shiro)


def uzumaki(k, mizo, fuchi, cx=64.0, cy=86.0, r=20):
    """渦巻きの彫り。右下側に明るい縁を見せて彫り込みに見せる。"""
    k.uzumaki(cx + 2, cy + 2, 2, r, 2.0, fuchi, futosa=4, kaishi=2.4)
    k.uzumaki(cx, cy, 2, r, 2.0, mizo, futosa=4, kaishi=2.4)


def tsuchi(k):
    """右下の土汚れ（経年 1 か所）。"""
    k.daen(82, 102, 12, 9, 'scuff')
    k.daen(92, 95, 6, 5, 'scuff')
    k.daen(74, 110, 5, 4, 'scuff')
    k.daen(86, 106, 5, 4, 'leather_deep')
    for (x, y) in ((78, 98), (88, 104), (84, 96)):                     # 土の粒
        k.shikaku(x, y, x + 1, y + 1, 'paper_deep')


def an_a():
    """案A: いまの形を起こす（v8 の③を 2 倍）。土色の石・青銅の渦巻き円盤・革紐の結び目・右下の土汚れ。"""
    k = D.K128()
    kage(k, 'paper_deep', 'wood_light', 'paper_dark')
    hanten(k, 'paper_deep', 'paper')
    # はめ込みの円盤（周りに暗い溝。青銅は左上が明るい）
    k.daen(64, 86, 32, 32, 'paper_deep')
    k.nuru(k.daen_m(64, 86, 32, 32) & ~k.daen_m(63, 85, 31, 31), 'paper')        # 溝の右下の縁が光る
    k.kyuu(k.daen_m(64, 86, 28, 28), 64, 86, 28, 28, ['brass_dark', 'brass', 'brass', 'brass_light'])
    k.wa(64, 86, 28, 25.5, 'brass_dark')                                          # 円盤の縁の段
    k.nuru(k.daen_m(64, 86, 28, 28) & ~k.daen_m(64, 86, 25.5, 25.5) & (PX + PY < 140), 'brass_light')
    uzumaki(k, 'brass_deep', 'brass_light')
    tsuchi(k)
    himo(k)
    k.daen(34, 66, 5, 3.5, 'highlight')
    return k.shiage128()


# ---- 案B: 勾玉 ----
def magatama_m(k, O, rho, a0, a1, r0, r1, n=60):
    """勾玉の形: 弧（中心 O・半径 rho・角度 a0→a1）に沿って、半径 r0→r1 の円を並べた和。
    ★ 太極の半分（大円の半分＋小円−小円）で作ったら、尾が細くなりすぎて縁に削られ、先がちぎれた（1回目）。"""
    m = np.zeros((D.N, D.N), dtype=bool)
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0 + (a1 - a0) * t)
        x, y = O[0] + rho * math.cos(a), O[1] + rho * math.sin(a)
        r = r0 + (r1 - r0) * t ** 0.9
        m |= (PX - x) ** 2 + (PY - y) ** 2 <= r * r
    return m


def an_b():
    """案B: 勾玉（古墳の頃からの護符）。翡翠の緑・頭に紐穴・艶の光。頭は右上、尾は下を回って左へ。"""
    k = D.K128()
    O, rho = (58, 66), 28
    m = magatama_m(k, O, rho, -45, 165, 36, 16)
    k.nuru(m, 'lens')
    hx, hy = O[0] + rho * math.cos(math.radians(-45)), O[1] + rho * math.sin(math.radians(-45))
    # 翡翠（光は左上）。縁の近くは透けて明るい
    k.kyuu(m, 60, 56, 58, 58, ['lens_deep', 'lens', 'lens', 'lens_light'])
    sou = D.dotto.sou_kazoeru(m)
    k.nuru(m & (sou > 8) & (sou <= 11) & (PX + PY > 150), 'lens_light')
    # 頭の紐穴（透明・縁は自動で金具）と、穴のまわりの窪み
    ax, ay = hx + 4, hy - 4
    k.nuru(k.daen_m(ax, ay, 19, 19) & ~k.daen_m(ax, ay, 15, 15) & m, 'lens_deep')
    k.daen(ax, ay, 7, 7, TOUMEI)
    # 艶（左の腹に白い弧）と照り
    yumi = k.daen_m(58, 70, 34, 34) & ~k.daen_m(61, 70, 31, 32) & m & (PX < 40) & (PY > 52) & (PY < 96)
    k.nuru(yumi, 'white_light')
    k.nuru(k.daen_m(hx - 20, hy - 2, 2.6, 2), 'highlight')
    # 経年は 1 か所（尾の小さな欠け）
    k.nuru(m & k.daen_m(40, 100, 3, 2), 'lens_deep')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('gofu', {'A': an_a, 'B': an_b}, '古びた護符 ── 512×512 の案')
