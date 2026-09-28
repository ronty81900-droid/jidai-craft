# -*- coding: utf-8 -*-
"""
haguruma.py -- 蒸気機関の歯車（8204）v10・512×512 の案。

  いまの絵（v5・16×16）は真鍮の輪で、歯が見えず「輪」に読めていた。
  ★ 窓（腕のあいだ）は透明にしない。透明の穴には内側にも縁（outline＋brass 8マス）が付き、
    歯車ぜんぶが縁の色だけになってしまう（v7 の教訓「穴だらけの形は穴でなく暗い窪みに」）。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/haguruma.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]


def kyori(cx, cy):
    return np.hypot(XS + 0.5 - cx, YS + 0.5 - cy)


def kakudo(cx, cy):
    return np.arctan2(-(YS + 0.5 - cy), XS + 0.5 - cx)


def ha_katachi(cx, cy, r_saki, r_ne, kazu, zure=0.0, haba=0.6):
    """歯車の外形。r_saki=歯先の半径、r_ne=歯元の半径、kazu=歯の数。
    haba は 1 ピッチのうち歯が占める割合（先へ行くほど少し細くする）。"""
    r = kyori(cx, cy)
    t = (kakudo(cx, cy) - zure) / (2 * math.pi) * kazu
    f = np.abs(t - np.round(t))                     # 歯の中心からのずれ（0〜0.5）
    # 歯元では haba、歯先では haba*0.7 の幅（台形の歯）
    k = np.clip((r - r_ne) / max(r_saki - r_ne, 1e-6), 0, 1)
    w = haba * (1 - 0.3 * k) / 2
    return (r <= r_ne) | ((r <= r_saki) & (f <= w))


def ude_katachi(cx, cy, r0, r1, kazu, futosa, zure=0.0):
    """腕（中心から放射状の帯）。"""
    r = kyori(cx, cy)
    th = kakudo(cx, cy)
    m = np.zeros_like(r, dtype=bool)
    for i in range(kazu):
        a = zure + i * 2 * math.pi / kazu
        # 帯までの距離 = 点と直線の距離
        d = np.abs(-(XS + 0.5 - cx) * math.sin(a) - (YS + 0.5 - cy) * math.cos(a))
        mae = ((XS + 0.5 - cx) * math.cos(a) - (YS + 0.5 - cy) * math.sin(a)) > 0
        m |= (d <= futosa / 2) & mae & (r >= r0) & (r <= r1)
    return m


def haguruma(k, cx, cy, r_saki, r_ne, r_wa, r_hub, kazu, ude, zai, zure=0.0, kubomi='black_mid'):
    """歯車1枚を描く。zai = (明, 地, 影, 深い影) の4色。窓は暗い窪み。"""
    hikari, ji, kage, fukai = zai
    soto = ha_katachi(cx, cy, r_saki, r_ne, kazu, zure)
    r = kyori(cx, cy)
    # 地（面取りの陰影）
    k.men(soto, hikari, ji, kage, haba=2, kage2=fukai)
    # 窓（腕のあいだの窪み）
    mado = (r < r_wa) & (r > r_hub) & ~ude_katachi(cx, cy, 0, r_wa + 1, ude, max(6, r_saki * 0.16),
                                                      zure + math.pi / ude)
    k.nuru(mado, kubomi)
    # 窪みの中: 右下の壁は光を受ける（明るい縁）、左上の壁は影（床より暗い）
    k.nuru(mado & ~k.zurasu(mado, -2, -2), kage)
    k.nuru(mado & ~k.zurasu(mado, 1, 1), 'black')
    # 輪の内側の縁取り（窓の外周に沿った一段暗い線）
    wa_fuchi = (r >= r_wa) & (r < r_wa + 1.6) & ~ude_katachi(cx, cy, 0, r_wa + 2, ude,
                                                             max(6, r_saki * 0.16), zure + math.pi / ude)
    k.nuru(wa_fuchi & soto, kage)
    # 中心の軸受け（盛り上がった円）
    hub = r <= r_hub
    k.kyuu(hub, cx, cy, r_hub, r_hub, [fukai, kage, ji, hikari])
    k.nuru(r <= r_hub * 0.42, 'iron_deep')                   # 軸の穴（暗い）
    k.nuru((r <= r_hub * 0.42) & (XS + 0.5 < cx) & (YS + 0.5 < cy), 'black_mid')
    return soto


def an_a():
    """案A: 真鍮の大きな歯車1枚。12枚歯・6本の腕・盛り上がった軸受け・止めネジ6本。"""
    k = D.K128()
    cx, cy = 64, 64
    zai = ('brass_light', 'brass', 'brass_dark', 'brass_deep')
    haguruma(k, cx, cy, 58, 43, 32, 13, 10, 6, zai, zure=math.radians(18))
    # 止めネジ（軸受けのまわり）
    for i in range(6):
        a = math.radians(30 + i * 60)
        x, y = cx + 18.5 * math.cos(a), cy - 18.5 * math.sin(a)
        k.daen(x, y, 1.9, 1.9, 'brass_deep')
        k.daen(x - 0.4, y - 0.4, 1.0, 1.0, 'brass_light')
    # 経年は 1 か所（輪の右下の擦り傷）
    k.sen(88, 86, 92, 83, 'scuff')
    k.sen(89, 87, 93, 84, 'brass_dark')
    # highlight は 1 まとまり（軸受けの左上の照り）
    k.daen(cx - 5, cy - 5, 2.2, 1.6, 'highlight')
    return k.shiage128()


def an_b():
    """案B: 真鍮の大歯車と鉄の小歯車が噛み合う。蒸気機関の中の一場面。"""
    k = D.K128()
    brass = ('brass_light', 'brass', 'brass_dark', 'brass_deep')
    tetsu = ('steel_light', 'iron_light', 'iron', 'iron_deep')
    # 小歯車（鉄・右下）を先に、大歯車（真鍮・左上）を上に重ねる
    # ★ 2枚の間に閉じた隙間ができないよう、少し重ねる（中心の間 58・大の歯元 33＋小の歯先 30）
    haguruma(k, 92, 90, 29, 20, 13, 8, 7, 4, tetsu, zure=math.radians(10), kubomi='black')
    haguruma(k, 50, 50, 44, 33, 23, 11, 11, 5, brass, zure=math.radians(2))
    for i in range(5):
        a = math.radians(90 + i * 72)
        x, y = 50 + 15.5 * math.cos(a), 50 - 15.5 * math.sin(a)
        k.daen(x, y, 1.7, 1.7, 'brass_deep')
        k.daen(x - 0.4, y - 0.4, 0.9, 0.9, 'brass_light')
    k.sen(30, 66, 33, 70, 'scuff')                               # 経年は 1 か所
    k.daen(45, 45, 2, 1.5, 'highlight')
    k.ana_ume('black')                                           # 残った閉じた隙間は暗く埋める
    return k.shiage128()


AN = {'A': an_a, 'B': an_b}


def main():
    import io
    import zipfile
    from PIL import Image
    ne = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    saki = os.path.join(ne, 'nouhin', 'v10_an', 'haguruma')
    os.makedirs(saki, exist_ok=True)
    z = zipfile.ZipFile(os.path.join(ne, 'JidaiUI-0.1.0.jar'))
    kumi = [('いま（v5・16×16）',
             Image.open(io.BytesIO(z.read('assets/jidaiui/textures/item/haguruma.png'))).convert('RGBA'))]
    for mei, f in AN.items():
        e = D.dasu(f())
        e.save(os.path.join(saki, '%s.png' % mei))
        ng = [s for ok, s in D.tenken512(e, mei) if not ok]
        print('案%s: %s' % (mei, '点検 すべて合格' if not ng else '★ ' + ' / '.join(ng)))
        kumi.append(('案' + mei, e))
    print(D.hikaku(kumi, os.path.join(saki, 'hikaku.png'), '蒸気機関の歯車 ── 512×512 の案'))


if __name__ == '__main__':
    main()
