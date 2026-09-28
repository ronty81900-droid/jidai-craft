# -*- coding: utf-8 -*-
"""
kinko.py -- ロスチャイルドの金庫（8217）v10・512×512 の案。

  いまの絵（v6・64×64）: 斜めから見た暗い金庫・真鍮の縁取り・ダイヤルと取っ手。
  効果は「貴金属の売値が 1.2倍」。お金の匂いがする方が伝わる。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/kinko.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

YS, XS = np.mgrid[0:D.N, 0:D.N]


def dial(k, cx, cy, r):
    """ダイヤル（真鍮の円盤・目盛り・つまみ）。"""
    k.kyuu(k.daen_m(cx, cy, r, r), cx, cy, r, r, ['brass_deep', 'brass_dark', 'brass', 'brass_light'])
    k.wa(cx, cy, r - 2.2, r - 3.4, 'brass_deep')
    for i in range(12):
        a = math.radians(i * 30)
        x0, y0 = cx + (r - 3) * math.cos(a), cy - (r - 3) * math.sin(a)
        x1, y1 = cx + (r - 5.5) * math.cos(a), cy - (r - 5.5) * math.sin(a)
        k.sen(x0, y0, x1, y1, 'brass_deep')
    k.kyuu(k.daen_m(cx, cy, r * 0.45, r * 0.45), cx, cy, r * 0.45, r * 0.45,
           ['iron_deep', 'iron', 'iron_light', 'steel_light'])
    k.takaku([(cx - 1, cy - r + 1.5), (cx + 1, cy - r + 1.5), (cx, cy - r + 4.5)], 'red')  # 合わせの印


def totte(k, cx, cy, r):
    """三本腕の取っ手（真鍮の輪と腕）。"""
    for i in range(3):
        a = math.radians(90 + i * 120)
        k.sen(cx, cy, cx + r * math.cos(a), cy - r * math.sin(a), 'brass_dark', futosa=2)
        k.sen(cx - 0.5, cy - 0.5, cx + r * math.cos(a) - 0.5, cy - r * math.sin(a) - 0.5, 'brass', futosa=1)
        k.daen(cx + r * math.cos(a), cy - r * math.sin(a), 2.2, 2.2, 'brass_light')
    k.daen(cx, cy, 3, 3, 'brass_dark')
    k.daen(cx - 0.6, cy - 0.6, 1.6, 1.6, 'brass_light')


def byou(k, x, y):
    k.shikaku(x, y, x + 1, y + 1, 'steel_light')
    k.ten(x + 1, y + 1, 'iron_deep')


def an_a():
    """案A: いまの構図を起こす。斜めから見た重い金庫。正面・右の側面・天板の3面。"""
    k = D.K128()
    # 3面の外形（正面の左上 (12,30)〜右下 (94,114)、奥行きは右上へ 20）
    shomen = k.takaku_m([(8, 30), (98, 30), (98, 120), (8, 120)])
    sokumen = k.takaku_m([(98, 30), (120, 10), (120, 100), (98, 120)])
    tenban = k.takaku_m([(8, 30), (30, 10), (120, 10), (98, 30)])
    k.men(shomen, 'black_light', 'black_mid', 'black', haba=2)
    k.nuru(sokumen, 'black')
    k.nuru(sokumen & ~k.zurasu(sokumen, 2, 0), 'black_mid')
    k.nuru(tenban, 'black_light')
    k.nuru(tenban & (YS >= 27), 'black_mid')
    # 天板と側面の真鍮の角金具
    k.nuru(sokumen & (XS >= 112), 'brass_dark')
    # 正面の扉（一段くぼんだ板・真鍮の縁取り）
    tobira = k.takaku_m([(20, 40), (88, 40), (88, 110), (20, 110)])
    k.nuru(tobira, 'brass_dark')
    naka = k.takaku_m([(23, 43), (85, 43), (85, 107), (23, 107)])
    k.men(naka, 'black_light', 'black_mid', 'black', haba=1)
    k.nuru(tobira & ~naka & (YS <= 42), 'brass')
    k.nuru(tobira & ~naka & (XS <= 21), 'brass')
    # 蝶番（左）
    for y in (50, 92):
        k.shikaku(16, y, 22, y + 8, 'iron_deep')
        k.shikaku(17, y + 1, 20, y + 7, 'iron_light')
    # ダイヤル・取っ手
    dial(k, 54, 64, 15)
    totte(k, 54, 94, 9)
    # 鋲（四隅）
    for (x, y) in ((12, 34), (92, 34), (12, 114), (92, 114), (34, 114), (72, 114)):
        byou(k, x, y)
    # 経年は 1 か所（正面の右下の擦り傷）
    k.sen(74, 104, 80, 101, 'scuff')
    # highlight は 1 まとまり（ダイヤルの左上の照り）
    k.daen(49.5, 59.5, 2.4, 1.7, 'highlight')
    return k.shiage128()


def kinka(k, x, y):
    """金貨を横から見た1枚（楕円）。"""
    k.daen(x, y, 6, 2.2, 'brass_dark')
    k.daen(x - 0.5, y - 0.6, 5, 1.5, 'brass')
    k.shikaku(int(x) - 3, int(y) - 1, int(x) - 2, int(y) - 1, 'brass_light')


def nobebou(k, x, y, w=16, h=7):
    """金の延べ棒（台形）。"""
    k.takaku([(x, y + h), (x + w, y + h), (x + w - 3, y), (x + 3, y)], 'brass')
    k.takaku([(x + 3, y), (x + w - 3, y), (x + w - 4, y + 2), (x + 4, y + 2)], 'brass_light')
    k.nuru(k.takaku_m([(x, y + h), (x + w, y + h), (x + w - 1, y + h - 2), (x + 1, y + h - 2)]), 'brass_dark')


def an_b():
    """案B: 正面から見た金庫。扉が左へ開き、中の金の延べ棒と金貨が見える。"""
    k = D.K128()
    # 本体（正面）
    hontai = k.marukaku_m(34, 12, 118, 118, 4)
    k.men(hontai, 'black_light', 'black_mid', 'black', haba=2)
    # 中（奥の暗い箱・棚）
    naka = k.marukaku_m(46, 24, 107, 107, 2)
    k.nuru(naka, 'black')
    k.nuru(naka & (YS >= 66) & (YS <= 68), 'iron_deep')              # 棚板
    k.nuru(naka & (YS == 66), 'iron')
    # 延べ棒（上の段に3本を積む）
    nobebou(k, 52, 57)
    nobebou(k, 70, 57)
    nobebou(k, 61, 49)
    nobebou(k, 88, 57, 14)
    # 金貨（下の段に積む）
    for i, x in enumerate((56, 70, 84, 96)):
        for j in range(3 - (i % 2)):
            kinka(k, x, 99 - j * 4)
    # 袋（下の段の左奥）
    k.daen(62, 88, 7, 8, 'leather')
    k.daen(61, 86, 5, 5, 'leather_light')
    k.shikaku(59, 79, 64, 81, 'leather_deep')
    # 開いた扉（左へ開いて、手前に厚みが見える）
    tobira = k.takaku_m([(8, 20), (36, 12), (36, 118), (8, 110)])
    k.nuru(tobira, 'black_mid')
    k.nuru(tobira & (XS >= 30), 'iron_deep')                        # 扉の厚み
    k.nuru(tobira & (XS >= 30) & (XS <= 31), 'iron')
    # 扉の裏のかんぬき（鋼の棒3本）
    for y in (34, 64, 94):
        k.shikaku(12, y, 30, y + 3, 'iron_light')
        k.shikaku(12, y + 3, 30, y + 3, 'iron_deep')
        k.shikaku(28, y - 1, 33, y + 4, 'steel_light')
    # 本体の真鍮の縁取り（中の口のまわり）
    kuchi_fuchi = k.marukaku_m(43, 21, 110, 110, 3) & ~k.marukaku_m(46, 24, 107, 107, 2)
    k.nuru(kuchi_fuchi, 'brass_dark')
    k.nuru(kuchi_fuchi & (YS <= 22), 'brass')
    # 経年は 1 か所（本体の右下の擦り傷）
    k.sen(104, 113, 109, 111, 'scuff')
    # highlight は 1 まとまり（延べ棒のいちばん上の照り）
    k.shikaku(65, 49, 69, 49, 'highlight')
    return k.shiage128()


AN = {'A': an_a, 'B': an_b}


def main():
    import io
    import zipfile
    from PIL import Image
    ne = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    saki = os.path.join(ne, 'nouhin', 'v10_an', 'kinko')
    os.makedirs(saki, exist_ok=True)
    z = zipfile.ZipFile(os.path.join(ne, 'JidaiUI-0.1.0.jar'))
    kumi = [('いま（v6・64×64）',
             Image.open(io.BytesIO(z.read('assets/jidaiui/textures/item/kinko.png'))).convert('RGBA'))]
    for mei, f in AN.items():
        e = D.dasu(f())
        e.save(os.path.join(saki, '%s.png' % mei))
        ng = [s for ok, s in D.tenken512(e, mei) if not ok]
        print('案%s: %s' % (mei, '点検 すべて合格' if not ng else '★ ' + ' / '.join(ng)))
        kumi.append(('案' + mei, e))
    print(D.hikaku(kumi, os.path.join(saki, 'hikaku.png'), 'ロスチャイルドの金庫 ── 512×512 の案'))


if __name__ == '__main__':
    main()
