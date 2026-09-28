# -*- coding: utf-8 -*-
"""
penicillin.py -- ペニシリン（8219）v10・512×512 の案。

  いまの絵（v6・64×64）: 左にガラスの瓶（金属の蓋・下に薄い緑の液）、右上から斜めにスポイト
  （右上に黒いゴムの球、ガラスの管の先は瓶の液の中）。瓶と管は1つの形につながっている。
  ★ 案A は、いまの絵の形（瓶・蓋・管・ゴムの球の位置）を 2 倍の 128 マスに写して、中を描き込む。
  ★ 斜めの管は、縁（8 マス）が斜めに食い込むので、横から見た幅より細く見える。管は太く取る。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/penicillin.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
PX, PY = XS + 0.5, YS + 0.5


def kuda_uv(ax, ay, bx, by):
    """a → b の線に沿った座標。t = 0（a）〜 1（b）、s = 線からの距離（右手が正）。"""
    L = math.hypot(bx - ax, by - ay)
    ex, ey = (bx - ax) / L, (by - ay) / L
    t = ((PX - ax) * ex + (PY - ay) * ey) / L
    s = (PX - ax) * (-ey) + (PY - ay) * ex
    return t, s, L


def an_a():
    """案A: いまの形を起こす。ガラスの瓶（金属の蓋・薄い緑の液）と、右上から斜めに入るスポイト。
    ★ 1回目は管を細い帯にしたら、縁（8マス）に両側から削られて灰色の棒になった。
      いまの絵と同じく、瓶の右と管のあいだはガラスで埋めて、1つの大きな形にする。"""
    k = D.K128()
    # ---- 形（いまの絵の外形を 2 倍）----
    bin_ = k.marukaku_m(6, 22, 72, 122, 16)                             # 瓶の胴（下の角が丸い）
    futa = k.marukaku_m(14, 10, 66, 38, 3)                               # 蓋
    tama = k.marukaku_m(84, 6, 122, 44, 10)                              # ゴムの球
    garasu = k.takaku_m([(68, 26), (88, 26), (120, 40), (72, 94), (68, 94)])   # 瓶と管のあいだのガラス
    t, s, L = kuda_uv(97, 32, 46, 99)                                    # 管の芯（ゴムの球の下 → 先）
    haba = 7.5 * np.clip((1 - t) / 0.18, 0.2, 1)                         # 先の 18% で細くなる
    kuda = (t >= 0) & (t <= 1) & (np.abs(s) <= haba)
    # ---- 瓶のガラス（縦の帯。光は左上）----
    k.nuru(bin_ | garasu, 'glass')
    k.nuru(bin_ & (PX >= 18) & (PX < 36), 'glass_light')
    k.nuru(bin_ & (PX >= 44) & (PX < 64), 'glass_deep')
    # 液（下の 4 割）。上の面は明るい線、右は暗い
    eki = bin_ & (PY >= 78) & k.marukaku_m(14, 0, 64, 114, 10)
    k.nuru(eki, 'liquid')
    k.nuru(eki & (PX >= 48), 'liquid_deep')
    k.nuru(eki & (PY < 80), 'liquid_light')
    # ---- 蓋（金属の締め金。縦の刻み）----
    k.nuru(futa, 'iron_light')
    k.nuru(futa & (PX < 26), 'steel_light')
    k.nuru(futa & (PX >= 52), 'iron')
    for x in range(28, 52, 4):
        k.nuru(futa & (PY > 24) & (np.abs(PX - x) < 0.6), 'iron')
    k.nuru(futa & (PY > 34), 'iron')                                     # 蓋の下の縁の影
    k.nuru(futa & k.marukaku_m(46, 20, 56, 27, 1), 'iron_deep')          # 蓋の切り欠き（ゴムの栓が見える）
    # ---- 管（ガラスの管。左の縁が光り、右の縁は暗い。目盛りつき）----
    k.nuru(kuda, 'steel_light')
    k.nuru(kuda & (s < -haba + 2), 'glass_light')
    k.nuru(kuda & (s > haba - 2), 'iron_light')
    for tt in np.arange(0.12, 0.62, 0.1):                                # 目盛り（右の縁から短い線）
        k.nuru(kuda & (np.abs(t - tt) < 0.008) & (s > haba * 0.1), 'iron')
    suu = kuda & (t > 0.66) & (np.abs(s) < haba - 2)                     # 吸い上げた液
    k.nuru(suu, 'liquid_light')
    # ---- ゴムの球（黒）と、管とつなぐ金属の輪 ----
    k.nuru(tama, 'black_light')
    k.nuru(tama & (PX + PY > 150), 'black_mid')
    k.nuru(tama & k.marukaku_m(94, 16, 99, 30, 2), 'iron_light')        # ゴムの照り（縦の筋）
    wa = (t >= -0.03) & (t < 0.03) & (np.abs(s) <= 10) & ~(PY < 30)
    k.nuru(wa, 'iron_light')
    k.nuru(wa & (s > 4), 'iron')
    # 照りは瓶の左上（いまの絵と同じ所）
    k.nuru(k.marukaku_m(20, 42, 24, 52, 1), 'highlight')
    # 経年は 1 か所（蓋の擦り傷）
    k.sen(30, 30, 36, 28, 'iron')
    k.ana_ume('glass_deep')
    return k.shiage128()


def an_b():
    """案B: フレミングの培養皿。寒天の上に青かびが生え、そのまわりだけ菌が育たない（輪の跡）。"""
    k = D.K128()
    cx, cy, rx, ry, atsu = 64, 60, 58, 44, 10
    sara = k.daen_m(cx, cy, rx, ry) | k.daen_m(cx, cy + atsu, rx, ry) | \
        ((np.abs(PX - cx) <= rx) & (PY >= cy) & (PY <= cy + atsu))
    # ガラスの壁（前の側面は下に見える）
    k.nuru(sara, 'glass')
    k.nuru(sara & (PY > cy + 6) & (PX > cx), 'glass_deep')
    naka = k.daen_m(cx, cy, rx - 16, ry - 13)
    k.nuru(k.daen_m(cx, cy, rx - 8, ry - 7) & ~naka, 'glass_light')     # 上の縁のガラス
    k.nuru(k.daen_m(cx, cy, rx - 8, ry - 7) & ~naka & (PX + PY > 150), 'glass')
    # 寒天（琥珀色）。右下の縁だけ暗い
    k.nuru(naka, 'hada')
    k.nuru(naka & ~k.daen_m(cx - 5, cy - 4, rx - 16, ry - 13), 'hada_dark')
    # 菌の小さな群れ（青かびから遠い所だけ）
    kx, ky = 52, 56
    for (x, y) in ((82, 44), (90, 52), (96, 62), (86, 72), (76, 80), (94, 76), (70, 88), (100, 56), (60, 90),
                   (84, 60), (40, 84), (28, 70)):
        if math.hypot(x - kx, (y - ky) * 1.3) > 30:
            k.nuru(naka & k.daen_m(x, y, 2.5, 2), 'paper_light')
            k.nuru(naka & k.daen_m(x + 0.8, y + 0.8, 1.2, 1), 'paper')
    # 菌の育たない輪（寒天が澄んで明るい）
    k.nuru(naka & k.daen_m(kx, ky, 26, 20) & ~k.daen_m(kx, ky, 17, 13), 'hada_light')
    # 青かびの群れ（ふちは白い綿毛、中は青緑、放射の筋）
    th = np.arctan2(PY - ky, PX - kx)
    yure = 1 + 0.08 * np.sin(7 * th) + 0.05 * np.sin(13 * th + 1)
    kabi = naka & (np.hypot((PX - kx) / 16, (PY - ky) / 12.5) <= yure)
    k.nuru(kabi, 'white_light')
    shin = naka & (np.hypot((PX - kx) / 13, (PY - ky) / 10) <= yure)
    k.nuru(shin, 'lens_light')
    k.nuru(shin & (np.hypot((PX - kx) / 9, (PY - ky) / 7) <= yure), 'lens')
    k.nuru(shin & (np.hypot((PX - kx) / 4, (PY - ky) / 3) <= 1), 'lens_deep')
    for i in range(10):                                                  # 放射の筋
        a = i * 2 * math.pi / 10 + 0.3
        k.sen(int(kx + 4 * math.cos(a)), int(ky + 3 * math.sin(a)),
              int(kx + 11 * math.cos(a)), int(ky + 8.5 * math.sin(a)), 'lens_deep')
    # 照り（ガラスの左上の縁）
    k.nuru(k.marukaku_m(31, 37, 35, 39, 1), 'highlight')
    # 経年は 1 か所（ガラスの縁の欠け）
    k.nuru(k.daen_m(104, 52, 2.5, 2) & sara, 'glass_deep')
    return k.shiage128()


if __name__ == '__main__':
    D.hashiru('penicillin', {'A': an_a, 'B': an_b}, 'ペニシリン ── 512×512 の案')
