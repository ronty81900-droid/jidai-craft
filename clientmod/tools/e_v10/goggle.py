# -*- coding: utf-8 -*-
"""
goggle.py -- 暗視ゴーグル（8220）v10・512×512 の案。

  いまの絵（v6・64×64）: 正面から見た黒い箱。下に緑のレンズ2つ（左は明るい lens・右は暗い lens_deep）、
  上の真ん中に真鍮の枠の細い窓（透明の穴）、右上に真鍮の箱（電池の蓋）、右のレンズに擦り傷。
  ★ 案A は、いまの絵の外形（dotto512.ima_katachi）と、レンズ・窓・電池の蓋の位置を 2 倍にして描き込む。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/goggle.py
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


def renzu(k, cx, cy, r, akarui=True):
    """対物レンズ。黒い筒（右下が影）の中に緑のガラス、奥にもう1枚のレンズの輪、左上に白い反射の弧。"""
    tsutsu = k.daen_m(cx, cy, r + 5, r + 5)
    k.nuru(tsutsu, 'black_mid')
    k.nuru(tsutsu & ~k.daen_m(cx - 2, cy - 2, r + 5, r + 5), 'black')
    k.nuru(tsutsu & ~k.daen_m(cx + 1, cy + 1, r + 5, r + 5), 'black_light')
    garasu = k.daen_m(cx, cy, r, r)
    if akarui:
        k.kyuu(garasu, cx, cy, r, r, ['lens_deep', 'lens', 'lens', 'lens_light'])
    else:
        k.kyuu(garasu, cx, cy, r, r, ['black', 'lens_deep', 'lens_deep', 'lens'])
    oku = k.daen_m(cx + 1, cy + 1, r * 0.55, r * 0.55) & ~k.daen_m(cx + 1, cy + 1, r * 0.55 - 1.2, r * 0.55 - 1.2)
    k.nuru(oku, 'lens_deep' if akarui else 'black')                       # 奥のレンズの縁
    yumi = garasu & k.daen_m(cx + 2, cy + 2, r - 3, r - 3) & ~k.daen_m(cx + 4, cy + 4, r - 3, r - 3) & \
        (PX < cx) & (PY < cy)
    k.nuru(yumi, 'white_light' if akarui else 'lens')                     # 反射の弧


def an_a():
    """案A: いまの形を起こす。黒い箱・緑のレンズ2つ・真鍮の枠の窓・右上の電池の蓋・右のレンズの擦り傷。"""
    k = D.K128()
    m = D.ima_katachi('goggle')
    # 箱（上の面は光が当たって明るい。前の面は暗い）
    k.nuru(m, 'black_mid')
    k.nuru(m & (PY < 50) & (PX < 96), 'black_light')
    k.nuru(m & (PX < 30) & (PY < 70), 'black_light')
    k.nuru(m & (PY >= 50) & (PY < 53) & (PX > 24) & (PX < 96), 'black')    # 上の面と前の面の境
    for x in range(30, 92, 6):                                            # 上の面の滑り止めの溝
        k.nuru(m & (np.abs(PX - x) < 0.6) & (PY > 22) & (PY < 30), 'black_mid')
    # 真鍮の箱（電池の蓋）: 丸い蓋と、硬貨で回す溝
    hako = k.marukaku_m(92, 26, 112, 48, 3)
    k.men(hako, 'brass', 'brass_dark', 'brass_deep', haba=2)
    futa = k.daen_m(102, 37, 7, 7)
    k.kyuu(futa, 102, 37, 7, 7, ['brass_deep', 'brass_dark', 'brass', 'brass_light'])
    k.sen(98, 37, 106, 37, 'brass_deep', futosa=2)
    # 上の真ん中の細い窓（透明の穴。縁は自動で真鍮の枠になる）
    # ★ 仕上げの ana_ume() はこの穴も埋めてしまう（1回目）。穴は ana_ume() の後で開ける
    k.ana_ume('black')
    k.shikaku(48, 40, 65, 43, TOUMEI)
    # レンズ（左は明るく、右は影で暗い）
    renzu(k, 41, 83, 19, akarui=True)
    renzu(k, 89, 76, 18, akarui=False)
    k.nuru(k.marukaku_m(26, 64, 30, 69, 1), 'highlight')                 # 照り（左のレンズの左上）
    # 経年は 1 か所（右のレンズの擦り傷）
    k.sen(94, 86, 101, 80, 'scuff', futosa=2)
    return k.shiage128()


def an_b():
    """案B: 電源の入った暗視ゴーグルを、目に当てる側から見る。ゴムの目当ての中で、接眼レンズが緑に光る。"""
    k = D.K128()
    hon = k.marukaku_m(10, 22, 118, 86, 18)
    me_h = k.daen_m(40, 82, 32, 32)
    me_m = k.daen_m(88, 82, 32, 32)
    kanagu = k.takaku_m([(46, 26), (82, 26), (76, 8), (52, 8)])            # 頭へ付ける金具
    m = hon | me_h | me_m | kanagu
    # 箱と金具
    k.nuru(m, 'black_mid')
    k.nuru(hon & (PY < 44), 'black_light')
    k.nuru(hon & (PY >= 44) & (PY < 47) & ~me_h & ~me_m, 'black')
    k.men(kanagu, 'steel_light', 'iron', 'iron_deep', haba=2)
    k.kyuu(k.daen_m(64, 16, 4, 4), 64, 16, 4, 4, ['iron_deep', 'iron', 'iron_light', 'steel_light'])   # 金具のねじ
    # 電源のつまみ（左の上）: 真鍮の丸いつまみと、向きの印
    tsumami = k.daen_m(26, 36, 7, 7)
    k.kyuu(tsumami, 26, 36, 7, 7, ['brass_deep', 'brass_dark', 'brass', 'brass_light'])
    k.sen(26, 36, 30, 32, 'brass_deep', futosa=2)
    # ゴムの目当て（黒。上の縁に光、下に影）と、光る接眼レンズ
    # ★ ひだを放射の線で描いたら、虫の脚に見えた（1回目）
    for cx in (40, 88):
        me = k.daen_m(cx, 82, 32, 32)
        k.nuru(me, 'black_mid')
        k.nuru(me & ~k.daen_m(cx + 2, 84, 32, 32), 'black_light')
        k.nuru(me & ~k.daen_m(cx - 2, 80, 32, 32), 'black')
        k.nuru(k.daen_m(cx, 82, 21, 21) & ~k.daen_m(cx, 82, 19, 19), 'black')   # 目当ての口の段
        k.nuru(k.daen_m(cx, 82, 16, 16), 'black')
        k.nuru(k.daen_m(cx, 82, 14, 14), 'lens_deep')
        k.nuru(k.daen_m(cx, 82, 11, 11), 'lens')
        k.nuru(k.daen_m(cx, 82, 7.5, 7.5), 'lens_light')
        k.nuru(k.daen_m(cx, 82, 4, 4), 'liquid_light')
    k.nuru(k.marukaku_m(31, 73, 34, 76, 1), 'highlight')                  # 照り（左のレンズ）
    # 経年は 1 か所（箱の角の擦れ）
    k.sen(100, 30, 106, 34, 'iron', futosa=1)
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('goggle', {'A': an_a, 'B': an_b}, '暗視ゴーグル ── 512×512 の案')
