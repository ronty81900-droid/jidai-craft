# -*- coding: utf-8 -*-
"""古びた護符（鉄器・8201）第2回: 今の v5 の形（卵形・紐穴・紐・右下の土）を 64×64 に起こし、A（青銅・渦巻き）を混ぜる。
  python tools/e_v8/gofu2.py
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
from dotto import Kyanbasu, hikaku, tenken  # noqa: E402
from PIL import Image  # noqa: E402

NE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SAKI = os.path.join(NE, 'nouhin', 'v8_an', 'gofu')
IMA = os.path.join(NE, 'nouhin', 'v5', 'assets', 'jidaiui', 'textures', 'item', 'gofu.png')

TOUMEI = (0, 0, 0, 0)


def karada(k, c, dx=0, dy=0, s=0):
    """v5 の形を 4 倍に起こした卵形（下が丸く、上がすぼんで紐の結びへ）。dx,dy でずらし s で縮める（陰影用）。"""
    k.daen(32 + dx, 42 + dy, 24 - s, 19 - s, c)                                   # 下の丸（x 8..56, y 23..61）
    k.takaku([(12 + s + dx, 34 + dy), (52 - s + dx, 34 + dy),
              (40 - s + dx, 5 + s + dy), (24 + s + dx, 5 + s + dy)], c)          # 首（台形）
    k.daen(32 + dx, 7 + s + dy, 8 - s, 5 - s, c)                                  # 首の上の丸み（y 2..12）


def kage(k, kage_iro, moto, hikari):
    """左上が明るく右下が暗い 3 段。"""
    karada(k, kage_iro)
    karada(k, moto, -2, -2, 2)
    karada(k, hikari, -5, -5, 7)


def himo(k):
    """紐穴（透明・縁が自動で金具になる）と、穴の上で結んだ革紐。"""
    # 結び目（大きめ）と、左右へ出る紐の端
    k.daen(32.0, 11.0, 8, 4.5, 'leather')
    k.daen(30.0, 9.5, 4, 2, 'leather_light')
    k.shikaku(26, 14, 38, 15, 'leather_deep')
    k.orisen([(25, 10), (22, 7)], 'leather_deep', 3)
    k.orisen([(39, 10), (42, 7)], 'leather_deep', 3)
    # 穴へ入る 2 本の紐
    k.orisen([(28, 15), (29, 18)], 'leather_deep', 3)
    k.orisen([(36, 15), (35, 18)], 'leather_deep', 3)
    k.orisen([(28, 15), (29, 17)], 'leather', 1)
    k.orisen([(36, 15), (35, 17)], 'leather', 1)
    # 紐穴
    k.daen(32.0, 23.0, 3.5, 3.5, TOUMEI)


def hanten(k, iro_kuro, iro_shiro):
    """石らしい斑点（位置は固定）。渦巻きの外側だけ。"""
    for x, y in ((14, 40), (19, 50), (24, 57), (47, 36), (51, 44), (44, 30), (20, 36), (49, 54), (28, 30), (38, 28)):
        k.shikaku(x, y, x + 1, y, iro_kuro)
    for x, y in ((16, 45), (50, 40), (26, 54), (42, 33)):
        k.ten(x, y, iro_shiro)


def uzumaki(k, mizo, fuchi, cx=32.0, cy=43.0, r=13):
    """渦巻きの彫り（A から）。右下側に明るい縁を 1px 見せて彫り込みに見せる。"""
    k.uzumaki(cx + 1, cy + 1, 1, r, 2.0, fuchi, futosa=3, kaishi=2.4)
    k.uzumaki(cx, cy, 1, r, 2.0, mizo, futosa=3, kaishi=2.4)


def tsuchi(k):
    """右下の土汚れ（経年 1 か所・v5 から）。"""
    k.daen(41.0, 51.0, 6, 4.5, 'scuff')
    k.daen(46.0, 47.5, 3, 2.5, 'scuff')
    k.daen(37.0, 55.0, 2.5, 2, 'scuff')
    k.daen(43.0, 53.0, 2.5, 2, 'leather_deep')


def ishi():
    """① 今の形 ×64・土色の石のまま ＋ 渦巻きの彫り。"""
    k = Kyanbasu()
    kage(k, 'paper_deep', 'wood_light', 'paper_dark')
    hanten(k, 'paper_deep', 'paper')
    uzumaki(k, 'paper_deep', 'paper')
    tsuchi(k)
    himo(k)
    k.daen(17.0, 33.0, 3, 2, 'highlight')
    return k.shiage()


def seidou():
    """② 今の形 ×64・青銅（A の色）＋ 渦巻き ＋ 緑青。"""
    k = Kyanbasu()
    kage(k, 'brass_deep', 'brass_dark', 'brass')
    uzumaki(k, 'brass_deep', 'brass_light')
    # 緑青（右下 1 か所）
    k.daen(43.0, 51.0, 5.5, 4, 'enamel_green')
    k.daen(47.0, 47.5, 2.5, 2.5, 'lens')
    k.daen(39.0, 55.0, 2.5, 1.5, 'lens_deep')
    himo(k)
    k.daen(17.0, 33.0, 3, 2, 'highlight')
    return k.shiage()


def hamekomi():
    """③ 今の形 ×64・土色の石に、青銅の渦巻き円盤をはめ込む。"""
    k = Kyanbasu()
    kage(k, 'paper_deep', 'wood_light', 'paper_dark')
    hanten(k, 'paper_deep', 'paper')
    # はめ込みの円盤（周りに暗い溝）
    k.daen(32.0, 43.0, 16, 16, 'paper_deep')
    k.kage_daen(32.0, 43.0, 14, 'brass_light', 'brass', 'brass_dark', zure=2)
    uzumaki(k, 'brass_deep', 'brass_light', r=10)
    tsuchi(k)
    himo(k)
    k.daen(17.0, 33.0, 3, 2, 'highlight')
    return k.shiage()


def main():
    os.makedirs(SAKI, exist_ok=True)
    ima = Image.open(IMA).convert('RGBA').resize((64, 64), Image.NEAREST)
    kumi = [('① 石のまま＋渦巻き', ishi(), 'ima_ishi'),
            ('② 青銅＋渦巻き＋緑青', seidou(), 'ima_seidou'),
            ('③ 石に青銅の円盤をはめ込む', hamekomi(), 'ima_hamekomi')]
    ok = True
    for fuda, im, fn in kumi:
        im.save(os.path.join(SAKI, fn + '.png'))
        for gou, bun in tenken(im, fn):
            print(('[PASS] ' if gou else '[FAIL] ') + bun)
            ok &= gou
    p = hikaku([('今（v5・16×16）', ima)] + [(a, b) for a, b, _ in kumi],
               os.path.join(SAKI, 'hikaku2.png'), '古びた護符 第2回: 今の形を 64×64 に ＋ A の要素')
    print(p)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
