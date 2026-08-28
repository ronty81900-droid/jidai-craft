# -*- coding: utf-8 -*-
"""
tokushu_kakunin.py -- Codex 納品の特殊アイテムの絵を、機械で点検する。

  ★★ 自己申告を実物で裏取りする ★★
    manifest.json には「16x16」「色は10種」「sha256 は…」と書いてある。
    書いてあることを信じずに、**保存された画素から数え直して**突き合わせる。
    ここが一致すれば、少なくとも manifest と実物は同じ物を指している。

  ★★ 8倍のプレビューが「拡大しただけ」か ★★
    これが今回いちばん効く検査。
    プレビューが本番の絵の【最近傍8倍】と1画素も違わないなら、
      1. プレビュー用に別の絵を描いていない
      2. 本番の絵に文字や効果を後から足していない
    が同時に言える。（UI 素材の「合成図＝部品の並び」と同じ考え方）

  動かし方:  python tests/tokushu_kakunin.py [納品の根]
"""
import hashlib
import io
import json
import os
import sys

from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
MOTO = sys.argv[1] if len(sys.argv) > 1 else os.path.join(NE, 'nouhin', 'v5')

pass_ = 0
fail_ = 0


def check(namae, jouken, shousai=''):
    global pass_, fail_
    if jouken:
        pass_ += 1
        print('[PASS] ' + namae)
    else:
        fail_ += 1
        print('[FAIL] ' + namae + '  ' + str(shousai))


def iro(px):
    """(r,g,b,a) → #RRGGBBAA。manifest と同じ書き方に揃える。"""
    return '#%02X%02X%02X%02X' % px


def kazoeru(im):
    """色ごとの画素数。"""
    kazu = {}
    for px in im.getdata():
        kazu[iro(px)] = kazu.get(iro(px), 0) + 1
    return kazu


def main():
    mp = os.path.join(MOTO, 'manifest.json')
    if not os.path.exists(mp):
        print('[SKIP] manifest.json がありません: ' + mp)
        return 0
    man = json.loads(io.open(mp, encoding='utf-8').read())

    files = [f for f in man['files'] if f.get('category') == 'item_texture']
    check('本番の絵が 5 枚ある', len(files) == 5, '実際=%d' % len(files))

    gazou = {}

    for f in files:
        mei = os.path.basename(f['file'])
        p = os.path.join(MOTO, f['file'].replace('/', os.sep))
        if not os.path.exists(p):
            check('%s がある' % mei, False, p)
            continue

        nama = open(p, 'rb').read()
        im = Image.open(io.BytesIO(nama)).convert('RGBA')
        gazou[mei] = im

        # ── 1) manifest の自己申告と実物 ──
        check('%s: sha256 が実物と一致' % mei,
              hashlib.sha256(nama).hexdigest().upper() == f['sha256'].upper(),
              '実物=%s' % hashlib.sha256(nama).hexdigest().upper()[:16])
        check('%s: byte_size が実物と一致' % mei,
              len(nama) == f['byte_size'], '実物=%d 申告=%d' % (len(nama), f['byte_size']))
        check('%s: 16x16' % mei, im.size == (16, 16), '実際=%s' % (im.size,))

        kazu = kazoeru(im)
        check('%s: 色数が申告どおり (%d)' % (mei, f['rgba_color_count']),
              len(kazu) == f['rgba_color_count'], '実物=%d' % len(kazu))
        check('%s: 色数が 32 以下' % mei, len(kazu) <= 32, '実物=%d' % len(kazu))

        # ★ 色ごとの画素数まで突き合わせる。ここが合えば「同じ絵」と言い切れる
        chigau = []
        for c in f['rgba_colors']:
            if kazu.get(c['rgba'].upper(), 0) != c['pixel_count']:
                chigau.append('%s 申告%d 実物%d'
                              % (c['rgba'], c['pixel_count'], kazu.get(c['rgba'].upper(), 0)))
        check('%s: 色ごとの画素数まで申告どおり' % mei, not chigau, chigau[:3])

        alpha = sorted(set(px[3] for px in im.getdata()))
        check('%s: 不透明度が 0 か 255 だけ' % mei, alpha == [0, 255] or alpha == [255],
              '実物=%s' % alpha)
        check('%s: 申告の alpha_values と一致' % mei,
              alpha == sorted(f['alpha_values']), '実物=%s 申告=%s' % (alpha, f['alpha_values']))

        # ── 2) 背景が透明か（四隅） ──
        sumi = [im.getpixel(xy)[3] for xy in ((0, 0), (15, 0), (0, 15), (15, 15))]
        check('%s: 四隅が透明（背景が塗られていない）' % mei, set(sumi) == {0}, sumi)

        # ── 3) 1px の暗い輪郭 ──
        #   絵の外側に接している画素が、共通の暗い色になっているか。
        rinkaku = f.get('outline_rgba') or '#102329FF'
        soto = 0
        chigau2 = 0
        for y in range(16):
            for x in range(16):
                if im.getpixel((x, y))[3] == 0:
                    continue
                # 上下左右に透明（＝絵の外）が触れていれば、そこは輪郭
                tonari = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
                soto_ni_sesshite = False
                for tx, ty in tonari:
                    if tx < 0 or ty < 0 or tx > 15 or ty > 15:
                        soto_ni_sesshite = True
                    elif im.getpixel((tx, ty))[3] == 0:
                        soto_ni_sesshite = True
                if soto_ni_sesshite:
                    soto += 1
                    if iro(im.getpixel((x, y))) != rinkaku:
                        chigau2 += 1
        check('%s: 外周がすべて共通の暗い輪郭 %s' % (mei, rinkaku),
              soto > 0 and chigau2 == 0, '外周%d画素のうち %d が別の色' % (soto, chigau2))

        # ── 4) プレビューが「8倍に拡大しただけ」か ──
        pv = os.path.join(MOTO, 'preview', mei)
        if not os.path.exists(pv):
            check('%s: プレビューがある' % mei, False, pv)
        else:
            pim = Image.open(pv).convert('RGBA')
            check('%s: プレビューが 128x128' % mei, pim.size == (128, 128), '%s' % (pim.size,))
            if pim.size == (128, 128):
                # ★★ ここが本丸 ★★
                hirogeta = im.resize((128, 128), Image.NEAREST)
                check('%s: ★★プレビューは本番の絵の最近傍8倍と1画素も違わない' % mei,
                      list(pim.getdata()) == list(hirogeta.getdata()),
                      'プレビュー用に別の絵を描いている疑い')

    # ── 5) 5枚が別々の絵か ──
    nakami = {}
    for mei, im in gazou.items():
        nakami.setdefault(bytes(im.tobytes()), []).append(mei)
    kasanari = [v for v in nakami.values() if len(v) > 1]
    check('5枚が別々の絵（使い回していない）', not kasanari, kasanari)

    # ── 6) 遠目で見分けられるか（いちばん多い「絵の色」が離れているか） ──
    #   輪郭と透明を除いた、いちばん面積の広い色で比べる。
    daihyou = {}
    for mei, im in gazou.items():
        kazu = kazoeru(im)
        kouho = [(n, c) for c, n in kazu.items()
                 if c != '#00000000' and c != '#102329FF']
        if kouho:
            daihyou[mei] = max(kouho)[1]
    chikai = []
    mei_list = sorted(daihyou)
    for i in range(len(mei_list)):
        for j in range(i + 1, len(mei_list)):
            a = daihyou[mei_list[i]]
            b = daihyou[mei_list[j]]
            sa = sum(abs(int(a[1 + k * 2:3 + k * 2], 16) - int(b[1 + k * 2:3 + k * 2], 16))
                     for k in range(3))
            if sa < 60:
                chikai.append('%s(%s) と %s(%s) の差=%d'
                              % (mei_list[i], a, mei_list[j], b, sa))
    check('5枚が色で見分けられる（代表色が離れている）', not chikai, chikai)
    for m in mei_list:
        print('      %-18s 代表色 %s' % (m, daihyou[m]))

    # ── 7) 依頼文と同じ 5 枚か ──
    iru = {'gofu.png', 'seihai.png', 'kaizu.png', 'haguruma.png', 'tsukinoishi.png'}
    check('依頼した 5 枚がそろっている', set(gazou) == iru, sorted(set(gazou) ^ iru))

    print('')
    print('=' * 60)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    print('')
    return 1 if fail_ else 0


sys.exit(main())
