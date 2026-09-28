# -*- coding: utf-8 -*-
"""
kaizu.py -- 羊皮紙の海図（8203）v10・512×512 の案。

  ご指定（2026-09-13）: 巻物風＋羅針図・地図は肌色・巻きは翠。
  下描きは nouhin/v8_an/kaizu/makimono_256.png（左右に翠の軸・肌色の紙・左上に海・赤い航路・羅針図）。

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/kaizu.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402


def jiku(k, x0, x1, y0, y1, hidari):
    """翠の軸（縦の円柱）。hidari=True なら左の軸（光は左上なので、明るい側は左）。"""
    m = np.zeros((D.N, D.N), dtype=bool)
    m[y0:y1 + 1, x0:x1 + 1] = True
    k.tate_dan(m, x0, x1 + 1, ['lens_light', 'lens_light', 'lens', 'lens', 'lens', 'lens_deep', 'lens_deep'])
    # 真鍮の帯（紙の上下の端のところ）
    for yb in (y0 + 10, y1 - 12):
        b = np.zeros_like(m)
        b[yb:yb + 3, x0:x1 + 1] = True
        k.tate_dan(b, x0, x1 + 1, ['brass_light', 'brass', 'brass', 'brass_dark', 'brass_deep'])


def rashinzu(k, cx, cy, r):
    """羅針図（8方位の星）。上下左右の大きい針は、光の当たる半分を明るく塗って立体に見せる。"""
    # 外の輪
    k.wa(cx, cy, r - 2, r - 3, 'brass_deep')
    # 斜めの小さい針
    for ang in (45, 135, 225, 315):
        t = math.radians(ang)
        tip = (cx + (r - 5) * math.cos(t), cy - (r - 5) * math.sin(t))
        l = (cx + 3 * math.cos(t + math.pi / 2), cy - 3 * math.sin(t + math.pi / 2))
        rr = (cx + 3 * math.cos(t - math.pi / 2), cy - 3 * math.sin(t - math.pi / 2))
        k.takaku([tip, l, (cx, cy)], 'brass_dark')
        k.takaku([tip, (cx, cy), rr], 'brass_deep')
    # 上下左右の大きい針（北は赤い先）
    for ang in (90, 0, 270, 180):
        t = math.radians(ang)
        tip = (cx + r * math.cos(t), cy - r * math.sin(t))
        l = (cx + 4 * math.cos(t + math.pi / 2), cy - 4 * math.sin(t + math.pi / 2))
        rr = (cx + 4 * math.cos(t - math.pi / 2), cy - 4 * math.sin(t - math.pi / 2))
        akarui = 'brass_light' if ang in (90, 180) else 'brass'
        kurai = 'brass_dark'
        k.takaku([tip, l, (cx, cy)], akarui)
        k.takaku([tip, (cx, cy), rr], kurai)
    # 北の先だけ赤
    k.takaku([(cx, cy - r), (cx - 2, cy - r + 6), (cx + 2, cy - r + 6)], 'red')
    # 真ん中
    k.daen(cx, cy, 2.6, 2.6, 'brass_deep')
    k.daen(cx - 0.4, cy - 0.4, 1.4, 1.4, 'highlight')


def jiku2(k, x0, x1, y0, y1, hidari):
    """翠の軸（太い円柱）と、上下に突き出た真鍮の頭。
    ★ 外側の 8 マスは縁（outline＋brass）になるので、翠が見えるのは内側だけ。
      左の軸は x0+8 から、右の軸は x1-8 まで。そこに円柱の陰影を付ける。"""
    m = k.marukaku_m(x0, y0, x1 + 1, y1 + 1, 5)
    k.nuru(m, 'lens')
    if hidari:
        v0, v1 = x0 + 8, x1
        dan = ['lens_light', 'lens_light', 'lens', 'lens', 'lens', 'lens', 'lens', 'lens_deep', 'lens_deep',
               'lens_deep', 'enamel_green', 'enamel_green', 'enamel_green', 'enamel_green']
    else:
        v0, v1 = x0, x1 - 8
        dan = ['lens_light', 'lens_light', 'lens', 'lens', 'lens', 'lens', 'lens', 'lens', 'lens_deep',
               'lens_deep', 'lens_deep', 'enamel_green', 'enamel_green', 'enamel_green']
    for i, c in enumerate(dan):
        x = v0 + i
        if x <= v1:
            k.nuru(m & (np.arange(D.N)[None, :] == x), c)
    # 上下の頭（細いので縁の色だけになる＝真鍮の頭に見える）
    cx = (x0 + x1 + 1) / 2
    k.marukaku(cx - 6, y0 - 6, cx + 6, y0 + 2, 3, 'brass')
    k.marukaku(cx - 6, y1 - 1, cx + 6, y1 + 7, 3, 'brass')
    k.daen(cx, y0 - 7, 3.5, 2.5, 'brass')
    k.daen(cx, y1 + 8, 3.5, 2.5, 'brass')


def tensen(k, ten, c, naga=4, aki=3, futosa=2):
    """点線。折れ線 ten に沿って naga マス塗り、aki マス空ける。"""
    zenbu = []
    for (x0, y0), (x1, y1) in zip(ten, ten[1:]):
        L = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(int(L)):
            zenbu.append((x0 + (x1 - x0) * i / L, y0 + (y1 - y0) * i / L))
    for i, (x, y) in enumerate(zenbu):
        if i % (naga + aki) < naga:
            k.shikaku(int(x), int(y), int(x) + futosa - 1, int(y) + futosa - 1, c)


def an_a():
    """案A: ご指定の下描き（makimono_256）を 128 マスで起こす。
    太い翠の軸と真鍮の頭・肌色の紙・左上に段々の海と島・赤い点線の航路・大きな羅針図・左下の丸。"""
    k = D.K128()
    ys, xs = np.mgrid[0:D.N, 0:D.N]
    # 紙（外周の上下 8 マスは縁になる）
    kami = np.zeros((D.N, D.N), dtype=bool)
    kami[24:104, 26:102] = True
    k.nuru(kami, 'hada')
    k.nuru(kami & (ys < 36), 'hada_light')                       # 上の縁のそばは明るい
    k.nuru(kami & (xs - ys > 44) & (ys < 50), 'hada_light')      # 右上の明るい面（下描きの三角）
    k.nuru(kami & (ys > 91), 'hada_dark')                        # 下の縁のそばは影
    k.nuru(kami & ((xs < 30) | (xs > 97)), 'hada_dark')          # 軸に巻き込む影
    # 繊維（まばらな短い線）
    for (x, y) in ((60, 42), (84, 50), (66, 84), (88, 80), (36, 90), (62, 72), (92, 60)):
        k.sen(x, y, x + 2, y, 'hada_dark')

    # 海（左上・段々の海岸）
    umi = k.takaku_m([(28, 30), (68, 30), (68, 40), (64, 40), (64, 48), (60, 48), (60, 56),
                      (54, 56), (54, 64), (48, 64), (48, 72), (40, 72), (40, 76), (28, 76)])
    k.nuru(umi, 'jewel_blue')
    k.nuru(umi & (xs + ys < 66), 'enamel_blue')                  # 沖は深い色
    asase = umi & ~k.zurasu(umi, -2, -2)                         # 岸に沿った浅瀬
    k.nuru(asase, 'glass')
    k.nuru(kami & ~umi & k.zurasu(umi, 1, 1) & ~k.zurasu(umi, -1, -1), 'hada_light')
    for (x, y) in ((33, 36), (46, 34), (38, 58), (31, 66), (52, 41)):
        k.sen(x, y, x + 1, y - 1, 'steel_light')
        k.sen(x + 1, y - 1, x + 3, y, 'steel_light')
    # 海の中の島
    k.daen(40, 46, 5.5, 3.5, 'hada_dark')
    k.daen(39.5, 45.3, 4.6, 2.6, 'hada')
    k.daen(38.5, 44.6, 2.4, 1.3, 'hada_light')

    # 赤い点線の航路（島のそばから、海岸に沿って左下の丸へ）
    tensen(k, [(48, 38), (54, 46), (58, 54), (56, 62), (52, 70), (49, 78), (46, 84)], 'red')
    k.daen(76, 38, 3, 3, 'red_deep')                             # 目当ての印（羅針図の上）
    k.daen(75.5, 37.5, 1.5, 1.5, 'red')
    # 左下の丸（港）
    k.wa(44, 90, 6.5, 5, 'hada_dark')

    # 羅針図（右・大きく）
    rashinzu2(k, 76, 66, 18)

    # 経年は 1 か所（右下の染み。形を不規則に）
    k.nuru(k.daen_m(92, 89, 3.5, 2.2) | k.daen_m(89.5, 91, 2.2, 1.6) | k.daen_m(95, 87.5, 1.4, 1.2), 'hada_dark')

    # 左右の翠の軸
    jiku2(k, 5, 28, 18, 109, True)
    jiku2(k, 99, 122, 18, 109, False)
    # 光の筋（highlight は 1 まとまり）
    k.shikaku(15, 28, 16, 44, 'highlight')
    return k.shiage128()


def rashinzu2(k, cx, cy, r):
    """羅針図（大きい版）。外の輪・8方位の針・真ん中の円。光の当たる側の半分を明るく。"""
    k.wa(cx, cy, r - 3, r - 4.5, 'brass_deep')
    k.wa(cx, cy, r - 8, r - 9, 'brass_dark')
    for ang in (45, 135, 225, 315):
        t = math.radians(ang)
        tip = (cx + (r - 6) * math.cos(t), cy - (r - 6) * math.sin(t))
        l = (cx + 3.5 * math.cos(t + math.pi / 2), cy - 3.5 * math.sin(t + math.pi / 2))
        rr = (cx + 3.5 * math.cos(t - math.pi / 2), cy - 3.5 * math.sin(t - math.pi / 2))
        k.takaku([tip, l, (cx, cy)], 'brass')
        k.takaku([tip, (cx, cy), rr], 'brass_dark')
    for ang in (90, 0, 270, 180):
        t = math.radians(ang)
        tip = (cx + r * math.cos(t), cy - r * math.sin(t))
        l = (cx + 5 * math.cos(t + math.pi / 2), cy - 5 * math.sin(t + math.pi / 2))
        rr = (cx + 5 * math.cos(t - math.pi / 2), cy - 5 * math.sin(t - math.pi / 2))
        k.takaku([tip, l, (cx, cy)], 'brass_light')
        k.takaku([tip, (cx, cy), rr], 'brass_dark')
    k.daen(cx, cy, 4, 4, 'brass_deep')
    k.daen(cx - 0.6, cy - 0.6, 2.6, 2.6, 'brass')
    k.daen(cx - 1.2, cy - 1.2, 1.2, 1.2, 'brass_light')


def fune(k, x, y):
    """小さな帆船（左向き・x,y は船体の左下）。"""
    # 帆柱と帆
    k.shikaku(x + 7, y - 14, x + 7, y - 1, 'wood_deep')
    k.takaku([(x + 8, y - 14), (x + 8, y - 3), (x + 15, y - 4)], 'white_light')
    k.takaku([(x + 8, y - 14), (x + 8, y - 3), (x + 11, y - 3.5)], 'white')
    k.takaku([(x + 7, y - 12), (x + 7, y - 4), (x + 1, y - 4)], 'white')
    k.shikaku(x + 7, y - 15, x + 9, y - 14, 'red')               # 旗
    # 船体
    k.takaku([(x, y - 3), (x + 16, y - 3), (x + 13, y + 1), (x + 3, y + 1)], 'wood')
    k.shikaku(x + 1, y - 3, x + 15, y - 3, 'wood_light')
    k.takaku([(x + 3, y), (x + 13, y), (x + 13, y + 1), (x + 3, y + 1)], 'wood_deep')


def an_b():
    """案B: 中世の航海図（ポルトラノ海図）。羅針図から方位線が放射状に伸び、帆船が浮かぶ。
    巻物の形と色（翠の軸・肌色の紙）はご指定のまま。"""
    k = D.K128()
    ys, xs = np.mgrid[0:D.N, 0:D.N]
    kami = np.zeros((D.N, D.N), dtype=bool)
    kami[24:104, 26:102] = True
    k.nuru(kami, 'hada')
    k.nuru(kami & (ys < 36), 'hada_light')
    k.nuru(kami & (ys > 91), 'hada_dark')
    k.nuru(kami & ((xs < 30) | (xs > 97)), 'hada_dark')

    # 海（紙の大半）。陸は右と左下に張り出す
    umi = kami & (xs >= 30) & (xs <= 97) & (ys >= 32) & (ys <= 95)
    riku_migi = k.takaku_m([(98, 32), (84, 32), (80, 38), (83, 44), (78, 50), (81, 58), (76, 64),
                            (79, 72), (74, 80), (78, 88), (72, 96), (98, 96)])
    riku_hidari = k.takaku_m([(30, 96), (30, 78), (36, 80), (40, 76), (46, 82), (52, 84), (56, 90),
                              (60, 96)])
    riku = (riku_migi | riku_hidari) & kami
    umi &= ~riku
    k.nuru(umi, 'jewel_blue')
    # 陸: 明るい肌色＋海岸の縁取り（暗い線と、海側の浅瀬）
    k.nuru(riku, 'hada_light')
    k.nuru(riku & ~k.zurasu(riku, 2, 0) & ~k.zurasu(riku, -2, 0) | (riku & ~k.zurasu(riku, 0, 2)),
           'hada')
    kishi = riku & (k.zurasu(umi, 1, 0) | k.zurasu(umi, -1, 0) | k.zurasu(umi, 0, 1) | k.zurasu(umi, 0, -1))
    k.nuru(kishi, 'hada_dark')
    asase = umi & (k.zurasu(riku, 1, 0) | k.zurasu(riku, -1, 0) | k.zurasu(riku, 0, 1) | k.zurasu(riku, 0, -1))
    k.nuru(asase, 'glass')
    # 陸の上の小さな町（赤い点）と旗
    for (x, y) in ((88, 44), (86, 66), (40, 88)):
        k.shikaku(x, y, x + 1, y + 1, 'red_deep')

    # 方位線（羅針図の真ん中から放射状に。海の上だけ）
    cx, cy = 52, 58
    for i in range(16):
        t = math.radians(i * 22.5)
        c = ('red' if i % 4 == 0 else 'lens_deep' if i % 2 == 0 else 'brass_dark')
        ex, ey = cx + 90 * math.cos(t), cy - 90 * math.sin(t)
        sen = np.zeros((D.N, D.N), dtype=bool)
        L = 90
        for s in range(L):
            px_, py_ = int(cx + s * math.cos(t)), int(cy - s * math.sin(t))
            if 0 <= px_ < D.N and 0 <= py_ < D.N:
                sen[py_, px_] = True
        k.nuru(sen & umi, c)

    # 羅針図（海の真ん中）
    rashinzu2(k, cx, cy, 16)
    # 帆船（右上の海）
    fune(k, 60, 44)
    # 経年は 1 か所（右の陸の染み）
    k.nuru((k.daen_m(90, 82, 3.2, 2.2) | k.daen_m(92.5, 84, 2, 1.5)) & riku, 'hada_dark')

    jiku2(k, 5, 28, 18, 109, True)
    jiku2(k, 99, 122, 18, 109, False)
    k.shikaku(14, 28, 15, 44, 'highlight')
    return k.shiage128()


def an_a_mae():
    """（最初の試し。残しておくだけ）"""
    k = D.K128()
    X0, X1, Y0, Y1 = 22, 105, 20, 107          # 紙（外周の 8 マスは縁になる）
    # 紙: 肌色。左上が明るく、軸のそばは巻き癖の影
    kami = np.zeros((D.N, D.N), dtype=bool)
    kami[Y0:Y1 + 1, X0:X1 + 1] = True
    k.nuru(kami, 'hada')
    ys, xs = np.mgrid[0:D.N, 0:D.N]
    k.nuru(kami & (xs + ys < 88), 'hada_light')
    k.nuru(kami & ((xs < 32) | (xs > 95)), 'hada_dark')        # 軸のそばの影
    k.nuru(kami & ((xs == 32) | (xs == 95)), 'hada')

    # 海（左上）。海岸線は不規則に
    umi = k.takaku_m([(28, 26), (74, 26), (72, 31), (66, 33), (64, 38), (57, 41), (55, 46),
                      (48, 48), (45, 53), (38, 55), (34, 60), (28, 61)])
    k.nuru(umi, 'jewel_blue')
    # 岸の暗い線と、陸側の明るい線
    kishi = umi & ~k.zurasu(umi, -1, -1) & ~k.zurasu(umi, 0, -1)
    k.nuru(umi & ~k.zurasu(umi, -2, -2), 'enamel_blue')
    riku_kishi = kami & ~umi & k.zurasu(umi, 2, 2)
    k.nuru(riku_kishi, 'hada_light')
    # 波
    for (x, y) in ((36, 34), (50, 31), (40, 44), (60, 36), (32, 50)):
        k.sen(x, y, x + 2, y - 1, 'glass_light')
        k.sen(x + 2, y - 1, x + 4, y, 'glass_light')

    # 島（右上）と山の印
    shima = k.daen_m(88, 40, 7, 5)
    k.nuru(shima & kami, 'hada_dark')
    k.nuru(k.daen_m(87, 39, 5, 3.4), 'hada')
    for (x, y) in ((44, 76), (50, 72), (56, 77)):
        k.takaku([(x, y - 4), (x - 3, y + 1), (x + 3, y + 1)], 'hada_dark')
        k.takaku([(x, y - 4), (x - 3, y + 1), (x, y + 1)], 'paper_dark')

    # 赤い航路（点線）と宝の印
    michi = [(34, 94), (40, 90), (46, 88), (52, 84), (58, 80), (66, 76), (72, 70), (78, 62),
             (82, 54), (86, 48)]
    for i, (x, y) in enumerate(michi[:-1]):
        if i % 2 == 0:
            k.sen(x, y, michi[i + 1][0], michi[i + 1][1], 'red', futosa=1)
    k.sen(84, 44, 88, 48, 'red_deep')
    k.sen(88, 44, 84, 48, 'red_deep')
    k.wa(34, 94, 3, 2, 'red_deep')

    # 羅針図（右下）
    rashinzu(k, 78, 84, 13)

    # 経年は 1 か所（右下の染み）
    k.daen(96, 98, 3, 2, 'hada_dark')

    # 左右の翠の軸（紙より上下に長い）
    jiku(k, 10, 27, 12, 115, True)
    jiku(k, 100, 117, 12, 115, False)
    return k.shiage128()


AN = {'A': an_a, 'B': an_b}


def main():
    ne = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    saki = os.path.join(ne, 'nouhin', 'v10_an', 'kaizu')
    os.makedirs(saki, exist_ok=True)
    kumi = []
    from PIL import Image
    import zipfile
    import io
    z = zipfile.ZipFile(os.path.join(ne, 'JidaiUI-0.1.0.jar'))
    ima = Image.open(io.BytesIO(z.read('assets/jidaiui/textures/item/kaizu.png'))).convert('RGBA')
    kumi.append(('いま（v5・16×16）', ima))
    for mei, f in AN.items():
        e = D.dasu(f())
        e.save(os.path.join(saki, '%s.png' % mei))
        ng = [s for ok, s in D.tenken512(e, mei) if not ok]
        print('案%s: %s' % (mei, '点検 すべて合格' if not ng else '★ ' + ' / '.join(ng)))
        kumi.append(('案' + mei, e))
    print(D.hikaku(kumi, os.path.join(saki, 'hikaku.png'), '羊皮紙の海図 ── 512×512 の案'))


if __name__ == '__main__':
    main()
