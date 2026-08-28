# -*- coding: utf-8 -*-
"""
ibutsu_kakunin.py -- Codex 納品（第6回: 遺物 11種・64×64）を機械で点検する。

  第5回（tokushu_kakunin.py）と同じ考え方。違いは:
    ・64×64 / 64色以下 / 外周 2px の暗い輪郭 ＋ 内側 2px の真鍮の縁（8近傍）/ プレビューは最近傍【4倍】（256×256）
    ・枚数と名前は plugin の Shouri.IBUTSU_HYOU から読む（手で写さない）

  動かし方:  python tests/ibutsu_kakunin.py [納品の根]     （既定: nouhin/v6）
"""
import hashlib
import io
import json
import os
import re
import sys
from collections import deque

from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
KIKAKU = os.path.dirname(NE)
MOTO = sys.argv[1] if len(sys.argv) > 1 else os.path.join(NE, 'nouhin', 'v6')
SHOURI = os.path.join(KIKAKU, 'plugin', 'src', 'main', 'java', 'jidai', 'Shouri.java')

OOKISA = 64
IRO_JOUGEN = 64
RINKAKU = '#102329FF'
SHINCHU = '#C4934BFF'
BAI = 4

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
    return '#%02X%02X%02X%02X' % px


def kazoeru(im):
    kazu = {}
    for px in im.getdata():
        kazu[iro(px)] = kazu.get(iro(px), 0) + 1
    return kazu


def kyori_soto(im):
    """各画素の「絵の外（透明 or 画像の外）からの距離」。透明は 0、外周の不透明は 1。8近傍。"""
    w, h = im.size
    px = im.load()
    d = [[None] * w for _ in range(h)]
    q = deque()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                d[y][x] = 0
                q.append((x, y))
            elif x == 0 or y == 0 or x == w - 1 or y == h - 1:
                d[y][x] = 1
                q.append((x, y))
    while q:
        x, y = q.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
            tx, ty = x + dx, y + dy
            if 0 <= tx < w and 0 <= ty < h and d[ty][tx] is None:
                d[ty][tx] = d[y][x] + 1
                q.append((tx, ty))
    return d


def ibutsu():
    s = io.open(SHOURI, encoding='utf-8').read()
    m = re.search(r'IBUTSU_HYOU = \{(.*?)\n    \};', s, re.S)
    return re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)",\s*"\d",\s*"[A-Z]+",\s*"[^"]*"\}', m.group(1))


def main():
    mp = os.path.join(MOTO, 'manifest.json')
    if not os.path.exists(mp):
        print('[SKIP] manifest.json がありません: ' + mp)
        return 0
    man = json.loads(io.open(mp, encoding='utf-8').read())
    files = [f for f in man['files'] if f.get('category') == 'item_texture']
    iru = {fai + '.png' for _m, fai, _b in ibutsu()}
    check('本番の絵が Shouri.java の遺物と同じ枚数（%d）' % len(iru), len(files) == len(iru), '実際=%d' % len(files))

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
        check('%s: sha256 が実物と一致' % mei, hashlib.sha256(nama).hexdigest().upper() == f['sha256'].upper(), '違う')
        check('%s: %dx%d' % (mei, OOKISA, OOKISA), im.size == (OOKISA, OOKISA), '実際=%s' % (im.size,))
        kazu = kazoeru(im)
        check('%s: 色数が申告どおり (%s)' % (mei, f.get('rgba_color_count')), len(kazu) == f.get('rgba_color_count'), '実物=%d' % len(kazu))
        check('%s: 色数が %d 以下' % (mei, IRO_JOUGEN), len(kazu) <= IRO_JOUGEN, '実物=%d' % len(kazu))
        alpha = sorted(set(px[3] for px in im.getdata()))
        check('%s: 不透明度が 0 か 255 だけ' % mei, alpha == [0, 255] or alpha == [255], '実物=%s' % alpha)
        sumi = [im.getpixel(xy)[3] for xy in ((0, 0), (OOKISA - 1, 0), (0, OOKISA - 1), (OOKISA - 1, OOKISA - 1))]
        check('%s: 四隅が透明' % mei, set(sumi) == {0}, sumi)

        # ★ 外周 2px の暗い輪郭 ＋ その内側 2px の真鍮の縁（依頼文 §2 の決まり）
        #   「絵の外からの距離」を 8近傍（斜めも隣）で数える。Codex の「1px ずつ剥がす」は
        #   この定義で、4近傍で数えると斜めの角の画素が別の層に入って全枚 落ちる（2026-08-23 実測）。
        d = kyori_soto(im)
        for hajime, owari, iro_kitai, yobi in ((1, 2, RINKAKU, '外周 2px が暗い輪郭'),
                                               (3, 4, SHINCHU, '内側 2px が真鍮の縁')):
            zen = chigau = 0
            for y in range(OOKISA):
                for x in range(OOKISA):
                    if d[y][x] is not None and hajime <= d[y][x] <= owari:
                        zen += 1
                        if iro(im.getpixel((x, y))) != iro_kitai:
                            chigau += 1
            check('%s: %s %s' % (mei, yobi, iro_kitai), zen > 0 and chigau == 0,
                  '%d 画素のうち %d が別の色' % (zen, chigau))

        # 本体の大きさ（小さく描いていないか）
        bb = im.getbbox()
        haba = (bb[2] - bb[0]) if bb else 0
        check('%s: 本体が 56px 以上（小さく描いていない）' % mei, haba >= 56, '実際=%d' % haba)

        pv = os.path.join(MOTO, 'preview', mei)
        if not os.path.exists(pv):
            check('%s: プレビューがある' % mei, False, pv)
        else:
            pim = Image.open(pv).convert('RGBA')
            check('%s: プレビューが %dx%d' % (mei, OOKISA * BAI, OOKISA * BAI), pim.size == (OOKISA * BAI, OOKISA * BAI), '%s' % (pim.size,))
            if pim.size == (OOKISA * BAI, OOKISA * BAI):
                hirogeta = im.resize((OOKISA * BAI, OOKISA * BAI), Image.NEAREST)
                check('%s: ★★プレビューは本番の最近傍%d倍と1画素も違わない' % (mei, BAI),
                      list(pim.getdata()) == list(hirogeta.getdata()), 'プレビュー用に別の絵を描いている疑い')

    check('依頼した %d 枚がそろっている' % len(iru), set(gazou) == iru, sorted(set(gazou) ^ iru))
    nakami = {}
    for mei, im in gazou.items():
        nakami.setdefault(bytes(im.tobytes()), []).append(mei)
    check('全部 別々の絵', not [v for v in nakami.values() if len(v) > 1], '使い回し')

    print('')
    print('=' * 60)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    return 1 if fail_ else 0


sys.exit(main())
