# -*- coding: utf-8 -*-
"""古びた護符（鉄器・8201）の 3 案。  python tools/e_v8/gofu.py"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
from dotto import Kyanbasu, hikaku, tenken  # noqa: E402
from PIL import Image  # noqa: E402

NE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SAKI = os.path.join(NE, 'nouhin', 'v8_an', 'gofu')
IMA = os.path.join(NE, 'nouhin', 'v5', 'assets', 'jidaiui', 'textures', 'item', 'gofu.png')


def an_a():
    """A: 青銅の円盤。渦巻きの刻印・縁に打ち出しの点・上に吊り環・右下に緑青。"""
    k = Kyanbasu()
    cx, cy, r = 32.0, 36.0, 26
    # 吊り環（輪）。円盤と繋がる
    k.wa(32.0, 9.0, 7, 2.5, 'brass_dark')
    # 円盤の陰影（左上 明・右下 暗）
    k.kage_daen(cx, cy, r, 'brass_light', 'brass', 'brass_dark', zure=3)
    # 盛り上がった縁取り（暗い溝 → その内側に明るい段）
    k.wa(cx, cy, 22, 20, 'brass_deep')
    k.wa(cx - 1, cy - 1, 20, 19, 'brass_light')
    # 打ち出しの点（縁の溝の上に 12 個）
    import math
    for i in range(12):
        th = i * math.pi / 6 + math.pi / 12
        x = int(cx + 21 * math.cos(th))
        y = int(cy + 21 * math.sin(th))
        k.shikaku(x - 1, y - 1, x, y, 'brass_light' if (math.cos(th) + math.sin(th)) < 0 else 'brass_deep')
    # 渦巻きの刻印（溝は暗い真鍮 3px・右下側に明るい縁 1px で彫り込みに見せる）
    k.uzumaki(cx + 1, cy + 1, 1, 13, 2.1, 'brass_light', futosa=3, kaishi=2.4)
    k.uzumaki(cx, cy, 1, 13, 2.1, 'brass_deep', futosa=3, kaishi=2.4)
    # 緑青（経年箇所は 1 か所・右下）
    k.daen(45.0, 49.0, 5, 4, 'enamel_green')
    k.daen(48.0, 46.0, 2.5, 2.5, 'lens')
    k.daen(43.0, 52.0, 2, 1.5, 'lens_deep')
    # 反射（1 か所）
    k.daen(20.0, 24.0, 3.5, 2.5, 'highlight')
    return k.shiage()


def an_b():
    """B: 石板の目。灰色の石の護符に彫った目（瞳は青）・上に紐穴・ひびと欠け。"""
    k = Kyanbasu()
    # 石板（角丸・縦長）: 右下が暗い
    k.marukaku(6, 3, 58, 62, 13, 'iron_deep')
    k.marukaku(6, 3, 56, 59, 13, 'iron')
    k.marukaku(8, 5, 50, 52, 11, 'iron_light')
    # 紐穴（透明）
    k.daen(32.0, 12.5, 3.5, 3.5, (0, 0, 0, 0))
    # 目（アーモンド形）: 白目 → 瞳 → 光
    k.takaku([(15, 36), (32, 24), (49, 36), (32, 48)], 'white_light')
    k.takaku([(17, 36), (32, 26), (47, 36), (32, 46)], 'white')
    k.daen(32.0, 36.0, 7, 7, 'jewel_blue')
    k.daen(33.0, 37.0, 5, 5, 'enamel_blue')
    k.daen(32.0, 36.0, 3, 3, 'black')
    k.shikaku(28, 32, 29, 33, 'highlight')
    # 目の周りの彫り線（まぶた）
    k.orisen([(14, 36), (22, 28), (32, 23), (42, 28), (50, 36)], 'iron_deep', 2)
    k.orisen([(14, 36), (22, 44), (32, 48), (42, 44), (50, 36)], 'iron_deep', 1)
    # ひび（右下 1 か所）
    k.orisen([(40, 51), (45, 55), (44, 58), (48, 60)], 'iron_deep', 1)
    # 欠け（右上の角を透明でえぐる）
    k.takaku([(48, 3), (58, 3), (58, 11)], (0, 0, 0, 0))
    return k.shiage()


def an_c():
    """C: 骨の札。骨色の板に赤い顔料で太陽十字・上に革紐を巻き付け・右下に欠け。"""
    k = Kyanbasu()
    # 骨の札（少しいびつな角丸）
    k.marukaku(7, 2, 57, 62, 10, 'white_deep')
    k.marukaku(7, 2, 55, 59, 10, 'white')
    k.marukaku(9, 4, 49, 52, 9, 'white_light')
    # 骨の筋（薄い線）
    k.sen(14, 20, 16, 56, 'white_deep', 1)
    k.sen(50, 16, 48, 50, 'white_deep', 1)
    # 革紐を 2 周 巻き付ける（札の上部・横帯）
    for y0 in (9, 15):
        k.shikaku(7, y0, 57, y0 + 3, 'leather')
        k.shikaku(7, y0, 57, y0, 'leather_light')
        k.shikaku(7, y0 + 3, 57, y0 + 3, 'leather_deep')
    # 結び目（右上）
    k.daen(46.0, 13.0, 4, 3, 'leather')
    k.daen(45.0, 12.0, 2, 1.5, 'leather_light')
    # 太陽十字（丸＋十字）を赤い顔料で。溝に見せるため下側に暗い赤
    k.wa(32.0, 40.0, 12, 8, 'red_deep')
    k.wa(32.0, 39.0, 12, 8, 'red')
    k.shikaku(30, 28, 33, 51, 'red')
    k.shikaku(20, 38, 44, 41, 'red')
    k.shikaku(30, 51, 33, 51, 'red_deep')
    k.shikaku(20, 41, 44, 41, 'red_deep')
    k.wa(32.0, 39.0, 11, 10, 'red_light')
    # 欠け（右下 1 か所）: 透明でえぐる
    k.takaku([(50, 55), (57, 55), (57, 63), (46, 63)], (0, 0, 0, 0))
    # 反射
    k.daen(16.0, 26.0, 2.5, 4, 'highlight')
    return k.shiage()


def main():
    os.makedirs(SAKI, exist_ok=True)
    ima = Image.open(IMA).convert('RGBA').resize((64, 64), Image.NEAREST)
    kumi = [('A 青銅の円盤（渦巻き）', an_a()), ('B 石板の目', an_b()), ('C 骨の札（赤い太陽十字）', an_c())]
    ok = True
    for fuda, im in kumi:
        im.save(os.path.join(SAKI, fuda[0] + '.png'))
        for gou, bun in tenken(im, fuda[0]):
            print(('[PASS] ' if gou else '[FAIL] ') + bun)
            ok &= gou
    p = hikaku([('今（v5・16×16）', ima)] + kumi, os.path.join(SAKI, 'hikaku.png'), '古びた護符（鉄器・8201）')
    print(p)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
