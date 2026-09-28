# -*- coding: utf-8 -*-
"""
tenro.py -- ベッセマー転炉（8216）v10・512×512 の案。

  いまの絵（v6・64×64）: 灰色の卵形の炉・右上に赤く光る口・左右に真鍮の軸受け車・暗い台。
  ★ 縁（外側 8 マス）にかかる所は outline と brass に塗りつぶされる。
    ・光る口は、まわりに炉の唇を残して内側に置く
    ・台座と車は 16 マスより太く・大きくする（細いと縁の色だけになる。1回目の失敗）

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/tenro.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]
BRASS = ('brass_light', 'brass', 'brass_dark', 'brass_deep')


def kuruma(k, cx, cy, r, ude=6, zai=BRASS):
    """軸受けの車（真鍮）。腕のあいだは暗い窪み。"""
    hikari, ji, kage, fukai = zai
    m = k.daen_m(cx, cy, r, r)
    k.men(m, hikari, ji, kage, haba=2, kage2=fukai)
    rr = np.hypot(XS + 0.5 - cx, YS + 0.5 - cy)
    th = np.arctan2(-(YS + 0.5 - cy), XS + 0.5 - cx)
    f = (th / (2 * math.pi) * ude) % 1.0
    mado = (rr < r - 4.5) & (rr > 5.5) & (f > 0.22) & (f < 0.78)
    k.nuru(mado, 'black_mid')
    k.nuru(mado & ~k.zurasu(mado, -1, -1), kage)
    k.nuru(mado & ~k.zurasu(mado, 1, 1), 'black')
    k.daen(cx, cy, 5, 5, kage)
    k.daen(cx - 0.8, cy - 0.8, 3, 3, ji)
    k.daen(cx - 1.4, cy - 1.4, 1.4, 1.4, hikari)


def kuchi(k, cx, cy, rx, ry):
    """光る口（上から覗く楕円）。鉄の唇 → hot_deep → hot → hot_light → highlight。"""
    k.daen(cx, cy, rx + 3, ry + 2.5, 'iron_light')
    k.daen(cx + 1, cy + 1, rx + 2, ry + 1.5, 'iron_deep')
    k.daen(cx, cy, rx, ry, 'hot_deep')
    k.daen(cx - 0.5, cy + 0.3, rx - 2, ry - 1.6, 'hot')
    k.daen(cx - 1.2, cy + 0.2, rx - 5, ry - 3.4, 'hot_light')
    k.daen(cx - 3, cy - 0.3, max(1.6, rx - 10.5), max(1.1, ry - 6), 'highlight')


def ro(k, cx, cy, rx, ry, kx, ky, kr):
    """卵形の炉。胴（楕円）＋首（台形）＋頭（口のまわりの楕円）。"""
    dou = k.daen_m(cx, cy, rx, ry)
    kubi = k.takaku_m([(cx - rx * 0.9, cy - ry * 0.25), (cx + rx * 0.92, cy - ry * 0.25),
                       (kx + kr + 4, ky + 2), (kx - kr - 3, ky + 1)])
    atama = k.daen_m(kx, ky, kr + 4, kr * 0.6 + 6)
    return dou | kubi | atama


def byou_obi(k, m, y0, kankaku=7):
    """鋲を打った帯（横）。"""
    obi = m & (YS >= y0) & (YS <= y0 + 3)
    k.nuru(obi, 'iron_deep')
    k.nuru(obi & (YS == y0), 'iron')
    for x in range(0, D.N - 1, kankaku):
        if m[y0 + 1, x] and m[y0 + 1, x + 1] and m[y0 + 1, max(x - 5, 0)] and m[y0 + 1, min(x + 6, D.N - 1)]:
            k.shikaku(x, y0 + 1, x + 1, y0 + 2, 'steel_light')


def dai_to_ro(k, kuchi_x, kuchi_y, kuchi_r, ro_cy=62):
    """暗い台（炉の下を埋める）と、卵形の炉。★ 透明の穴を作らない（穴の内側にも縁が付くため）。"""
    dai = k.marukaku_m(8, 92, 120, 121, 4)
    k.men(dai, 'black_light', 'black_mid', 'black', haba=2)
    # 台の煉瓦の目地（横2本・縦は互い違い）
    for y in (101, 110):
        k.nuru(dai & (YS == y) & (XS > 14) & (XS < 114), 'black')
    for y0, y1, x0 in ((93, 100, 20), (102, 109, 26), (111, 118, 20)):
        for x in range(x0, 112, 13):
            k.nuru(dai & (XS == x) & (YS >= y0) & (YS <= y1), 'black')
    m = ro(k, 64, ro_cy, 36, 36, kuchi_x, kuchi_y, kuchi_r)
    m &= YS < 96
    k.kyuu(m, 56, ro_cy - 16, 46, 54, ['iron_deep', 'iron', 'iron', 'iron_light', 'steel_light'])
    for x in (50, 78):
        k.nuru(m & (XS == x) & (YS > ro_cy - 24) & (YS < 94), 'iron_deep')
    byou_obi(k, m, ro_cy - 14)
    byou_obi(k, m, ro_cy + 18)
    # 炉の下の影（台に落ちる）
    k.nuru(dai & (YS >= 92) & (YS <= 95) & (np.abs(XS + 0.5 - 64) < 30), 'black')
    return m


def an_a():
    """案A: いまの構図を起こす。大きな卵形の鉄の炉・右上の光る口・左右の真鍮の車・暗い台。"""
    k = D.K128()
    m = dai_to_ro(k, 80, 28, 15)
    # 口の下の壁が熱で赤らむ
    k.nuru(m & k.daen_m(88, 40, 9, 5) & ~k.daen_m(80, 28, 19, 11), 'red_deep')
    kuchi(k, 80, 29, 15, 8)
    # 左右の車（炉の脇腹と台に重なる）
    kuruma(k, 26, 72, 17)
    kuruma(k, 102, 72, 17)
    # 経年は 1 か所（胴の煤）
    k.nuru(m & k.daen_m(52, 88, 7, 3) & ~k.daen_m(26, 72, 18, 18), 'soot')
    return k.shiage128()


def honoo(k, x0, y0, haba, takasa, yure=0.0):
    """炎（口から上へ）。外から hot_deep → hot → hot_light → brass_light。
    ★ 外側 8 マスは縁になるので、太めに描く。"""
    m = np.zeros((D.N, D.N), dtype=bool)
    for i in range(takasa):
        t = i / takasa
        w = haba / 2 * (1 - t) ** 0.7 + 1.5
        cx = x0 + math.sin(t * 5.5 + yure) * 3.5 * t
        y = y0 - i
        m |= (YS == y) & (np.abs(XS + 0.5 - cx) <= w)
    k.nuru(m, 'hot_deep')
    naka = m & k.zurasu(m, 2, 0) & k.zurasu(m, -2, 0) & k.zurasu(m, 0, 2)
    k.nuru(naka, 'hot')
    naka2 = naka & k.zurasu(naka, 3, 0) & k.zurasu(naka, -3, 0) & k.zurasu(naka, 0, 3)
    k.nuru(naka2, 'hot_light')
    naka3 = naka2 & k.zurasu(naka2, 3, 0) & k.zurasu(naka2, -3, 0) & k.zurasu(naka2, 0, 4)
    k.nuru(naka3, 'brass_light')
    return m


def honoo2(k, cx, y_ne):
    """ふくらんだ炎（丸い房を重ねる）。外側 8 マスは縁になるので、房は大きく。
    中へ向かって hot_deep → hot → hot_light → brass_light。"""
    fusa = [(cx, y_ne - 14, 24, 13), (cx - 15, y_ne - 8, 13, 9), (cx + 15, y_ne - 9, 14, 10),
            (cx - 3, y_ne - 27, 15, 11), (cx + 7, y_ne - 36, 8, 7)]
    m = np.zeros((D.N, D.N), dtype=bool)
    for (x, y, rx, ry) in fusa:
        m |= k.daen_m(x, y, rx, ry)
    # 上へ尖った炎の舌（3本）
    for (x, y, w, h) in ((cx - 16, y_ne - 14, 7, 20), (cx + 18, y_ne - 16, 7, 18), (cx + 2, y_ne - 38, 6, 16)):
        m |= k.takaku_m([(x - w, y), (x + w, y), (x + 1, y - h)])
    k.nuru(m, 'hot_deep')
    for (x, y, rx, ry) in fusa:
        k.nuru(m & k.daen_m(x - 1, y + 1, rx - 5, ry - 4), 'hot')
    for (x, y, rx, ry) in fusa[:4]:
        k.nuru(m & k.daen_m(x - 1, y + 2, rx - 9, ry - 7), 'hot_light')
    k.nuru(m & k.daen_m(cx - 1, y_ne - 12, 9, 5), 'brass_light')
    return m


def an_b():
    """案B: ベッセマー法の「吹き」。暗い煉瓦の壁の前で、炉の口から炎と火の粉が吹き上がる。
    ★ 炎と火の粉は壁の内側に描く（シルエットの縁にかかると縁の色に塗りつぶされるため）。"""
    k = D.K128()
    # 煉瓦の壁（シルエット全体の地。上は丸いアーチ）
    kabe = k.marukaku_m(7, 10, 121, 96, 16)
    k.nuru(kabe, 'black_mid')
    for y in range(18, 96, 8):
        k.nuru(kabe & (YS == y), 'black')
        zure = 0 if (y // 8) % 2 == 0 else 6
        for x in range(zure + 8, 121, 12):
            k.nuru(kabe & (XS == x) & (YS > y) & (YS < y + 8), 'black')
    # 壁に映る炎の照り返し（真ん中が赤い）
    k.nuru(kabe & k.daen_m(64, 40, 34, 26) & ~k.daen_m(64, 40, 26, 18), 'red_deep')
    k.nuru(kabe & k.daen_m(64, 40, 26, 18), 'hot_deep')
    # 火の粉（壁の上に散る）
    for (x, y, c) in ((34, 26, 'hot_light'), (40, 16, 'brass_light'), (92, 22, 'hot_light'),
                      (86, 32, 'hot'), (30, 44, 'hot'), (98, 44, 'brass_light'), (50, 14, 'hot'),
                      (78, 12, 'hot_light'), (24, 34, 'brass_light'), (104, 30, 'hot')):
        k.shikaku(x, y, x + 1, y + 1, c)
    m = dai_to_ro(k, 64, 56, 15, ro_cy=74)
    k.nuru(m & k.daen_m(64, 64, 20, 5) & ~k.daen_m(64, 56, 19, 10), 'red_deep')
    # 炎は炉の口から上へ（炉より前に描く。口の線より上だけ）
    ue = np.zeros((D.N, D.N), dtype=bool)
    ue[:57, :] = True
    mae = k.a.copy()
    honoo2(k, 64, 58)
    k.a[~ue] = mae[~ue]
    k.a[ue & ~kabe] = 0                       # 炎の先は壁の内側に収める（外へ出ると縁の尖りになる）
    # 口（炎の根元がいちばん明るい）
    k.daen(64, 57, 18, 8, 'iron_light')
    k.daen(65, 58, 17, 7, 'iron_deep')
    k.daen(64, 57, 14, 5.5, 'hot_light')
    k.daen(62, 56.5, 9, 3, 'highlight')
    kuruma(k, 26, 78, 16)
    kuruma(k, 102, 78, 16)
    k.nuru(m & k.daen_m(76, 90, 6, 2.5) & ~k.daen_m(102, 78, 17, 17), 'soot')
    return k.shiage128()


AN = {'A': an_a, 'B': an_b}


def main():
    import io
    import zipfile
    from PIL import Image
    ne = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    saki = os.path.join(ne, 'nouhin', 'v10_an', 'tenro')
    os.makedirs(saki, exist_ok=True)
    z = zipfile.ZipFile(os.path.join(ne, 'JidaiUI-0.1.0.jar'))
    kumi = [('いま（v6・64×64）',
             Image.open(io.BytesIO(z.read('assets/jidaiui/textures/item/tenro.png'))).convert('RGBA'))]
    for mei, f in AN.items():
        e = D.dasu(f())
        e.save(os.path.join(saki, '%s.png' % mei))
        ng = [s for ok, s in D.tenken512(e, mei) if not ok]
        print('案%s: %s' % (mei, '点検 すべて合格' if not ng else '★ ' + ' / '.join(ng)))
        kumi.append(('案' + mei, e))
    print(D.hikaku(kumi, os.path.join(saki, 'hikaku.png'), 'ベッセマー転炉 ── 512×512 の案'))


if __name__ == '__main__':
    main()
