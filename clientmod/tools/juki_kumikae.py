# -*- coding: utf-8 -*-
"""juki_kumikae.py -- 銃器専門店の2ページ（銃・弾）を組み直す。

  ★★ なぜ要るか（2026-08-30 に実際に困った）★★
    プラグイン側（Shop.java）で銃を 9丁6種 → 5丁1種 に直したのに、
    **画面は 9丁のまま**だった。理由は2つ。
      ① 画面の文字の正本は ui/manifest.json で、プラグインとは別物
      ② カードの枠は下地の絵（page_gunshop_*.png）に焼き込まれている
    どちらも直さないと、実機の見た目は変わらない。

  ★ このファイルが銃器専門店ページの正本。manifest.json を手で書き換えない。
    直したら次を順に走らせる:
        python tools/juki_kumikae.py     … ここ（manifest と 絵）
        python tools/hyou_tsukuru.py     … Hyou.java を作り直す
        python build.py                  … MOD の jar を組む

  ★ 品揃えは plugin/src/main/java/jidai/Shop.java の JUKIHIN と 1対1。
    tests/buki_kakunin.py が実物のパックと突き合わせている。

  使い方:  python tools/juki_kumikae.py
"""
import hashlib
import io
import json
import os

from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
UI = os.path.join(NE, 'ui')
MANIFEST = os.path.join(UI, 'manifest.json')

E = 'assets/jidaiui/textures/gui/'

# タブは2枚。左から 62px 幅で 2px 空けて並べる。
TABU = [(20, 38), (84, 38)]
TABU_MEI = ['銃', '弾']
TABU_HABA = 62
TABU_TAKASA = 18

# カードを置く場所（種類ごと）。今の manifest の並びと同じ。
BASHO = {
    'compact': [(16, 60), (113, 60), (210, 60),
                (16, 114), (113, 114), (210, 114),
                (16, 168), (113, 168), (210, 168)],
    'tall': [(16, 60), (113, 60), (210, 60),
             (16, 142), (113, 142), (210, 142)],
}

KAZOKU = {'gunshop_guns': 'compact', 'gunshop_ammo': 'tall'}
ERABU = {'gunshop_guns': 0, 'gunshop_ammo': 1}

# ══════════════════════════════════════════════════════════════
#  正本 ── (名前, 財布, 値段, 解禁, 説明)
#    ★ 2026-08-30: 9丁6種 → 5丁1種。
#      呼び名を役割にして、実銃名は説明の行へ回した。
#      並びは【毎秒の威力】を実物のパックから測って決めた
#      （18 → 45 → 60 → 79 → 108 と必ず上がる）。
# ══════════════════════════════════════════════════════════════
SHINAMONO = {
    'gunshop_guns': [
        ('拳銃',     '勢力', '800',    '中世', 'ナガン M1895 / 装弾数 7'),
        ('小銃',     '勢力', '3,000',  '近代', 'SKS / 装弾数 10'),
        ('連射銃',   '勢力', '5,000',  '近代', 'MP18 / 装弾数 32'),
        ('自動小銃', '勢力', '8,000',  '現代', 'M4A1 / 装弾数 30'),
        ('狙撃銃',   '勢力', '12,000', '現代', 'AWP / 装弾数 5'),
    ],
    'gunshop_ammo': [
        ('弾 30発', '個人', '20', '中世', 'どの銃にも使えます'),
    ],
}


def hiraku(rel):
    return Image.open(os.path.join(UI, rel)).convert('RGBA')


def tabu_moji(pid):
    de = []
    for i, (x, y) in enumerate(TABU):
        erabu = (i == ERABU[pid])
        de.append({
            'id': 'tab_%d_label' % (i + 1), 'content': TABU_MEI[i],
            'rect': [x + 6, y + 4, TABU_HABA - 12, 10],
            'font_px': 8, 'line_height_px': 10, 'max_lines': 1,
            'color': '#F5E8C8' if erabu else '#AEB8AE',
            'align': 'center', 'dynamic': False, 'notes': '',
        })
    return de


def tabu_buhin(pid):
    de = []
    for i, (x, y) in enumerate(TABU):
        erabu = (i == ERABU[pid])
        de.append({
            'id': 'tab_%d' % (i + 1),
            'asset': E + ('tab_selected.png' if erabu else 'tab_unselected.png'),
            'rect': [x, y, TABU_HABA, TABU_TAKASA], 'selected': erabu,
        })
    return de


def kado_tsukuru(m, pid):
    kazoku = KAZOKU[pid]
    fam = m['card_families'][kazoku]
    w, h = fam['size']
    if len(SHINAMONO[pid]) > len(BASHO[kazoku]):
        raise SystemExit('[中止] %s: 品が %d あるのに置き場は %d しか無い'
                         % (pid, len(SHINAMONO[pid]), len(BASHO[kazoku])))
    kado, moji = [], []
    for i, ((namae, saifu, nedan, kaikin, setsumei), (x, y)) in enumerate(
            zip(SHINAMONO[pid], BASHO[kazoku]), start=1):
        cid = 'item_%d' % i
        display = saifu + nedan + '円'
        kado.append({
            'id': cid, 'rect': [x, y, w, h], 'family': kazoku,
            'state_assets': {st: E + 'card_%s_%s.png' % (kazoku, st)
                             for st in ('normal', 'hover', 'pressed',
                                        'locked', 'insufficient')},
            'game_icon_rect': [x + fam['item'][0], y + fam['item'][1],
                               fam['item'][2], fam['item'][3]],
            'name': namae, 'price': nedan, 'wallet': saifu,
            'display_price': display, 'unlock': kaikin,
            'tooltip': setsumei, 'pressed_content_offset': [1, 1],
        })
        for suffix, key, iro, naka in (
                ('_name', 'name', '#F5E8C8', namae),
                ('_price', 'price', '#C4934B', display),
                ('_unlock', 'meta', '#AEB8AE', kaikin)):
            r = fam[key]
            moji.append({
                'id': cid + suffix, 'content': naka,
                'rect': [x + r[0], y + r[1], r[2], r[3]],
                'font_px': 8, 'line_height_px': 10,
                'max_lines': 2 if suffix == '_name' else 1,
                'color': iro, 'align': 'left', 'dynamic': False, 'notes': '',
            })
    return kado, moji


def main():
    m = json.loads(io.open(MANIFEST, encoding='utf-8').read())

    for pid in ('gunshop_guns', 'gunshop_ammo'):
        p = m['pages'][pid]

        nokosu = ('title', 'wallet_personal', 'wallet_faction')
        moji = [t for t in p['runtime_text'] if t['id'] in nokosu]
        moji += tabu_moji(pid)

        buhin = [c for c in p['components'] if c['id'] == 'screen_chrome']
        buhin += tabu_buhin(pid)

        kado, item_moji = kado_tsukuru(m, pid)
        moji += item_moji
        for c in kado:
            buhin.append({'id': c['id'], 'asset': c['state_assets']['normal'],
                          'rect': c['rect']})

        p['components'] = buhin
        p['cards'] = kado
        p['runtime_text'] = moji

        # ── 合成図を部品から作り直す ──
        e = Image.new('RGBA', (320, 240), (0, 0, 0, 0))
        for c in buhin:
            e.alpha_composite(hiraku(c['asset']), (c['rect'][0], c['rect'][1]))
        rel = p['base_asset']
        e.save(os.path.join(UI, rel))

        # ── 文字を置く場所が無地か、その場で確かめる ──
        kara = []
        for t in moji:
            x, y, tw, th = t['rect']
            iro = set(e.crop((x, y, x + tw, y + th)).getdata())
            if len(iro) != 1:
                raise SystemExit('[中止] %s の %s が無地ではありません（%d色）'
                                 % (pid, t['id'], len(iro)))
            kara.append({'label': 'runtime_text:' + t['id'], 'rect': [x, y, tw, th],
                         'reason': '文字はPNGへ焼かず実行時描画'})

        # ── manifest のファイル記述を実物に合わせる ──
        nakami = open(os.path.join(UI, rel), 'rb').read()
        px = list(e.getdata())
        kiroku = next((f for f in m['files'] if f['file'] == rel), None)
        if kiroku is None:
            raise SystemExit('[中止] manifest の files に %s が無い' % rel)
        kiroku['dimensions'] = [320, 240]
        kiroku['blank_regions'] = kara
        kiroku['byte_size'] = len(nakami)
        kiroku['sha256'] = hashlib.sha256(nakami).hexdigest().upper()
        kiroku['rgba_color_count'] = len(set(px))
        kiroku['alpha_values'] = sorted(set(q[3] for q in px))
        kiroku['transparent_rgb_zero'] = True

        print('  %-14s カード %d 枚 / 文字 %d 件 / %s'
              % (pid, len(kado), len(moji), os.path.basename(rel)))

    io.open(MANIFEST, 'w', encoding='utf-8', newline='\n').write(
        json.dumps(m, ensure_ascii=False, indent=2) + '\n')
    print()
    print('manifest.json と 合成図2枚を作り直しました。')
    print('★ 続けて走らせること:')
    print('    python tools/hyou_tsukuru.py    … Hyou.java を作り直す')
    print('    python build.py                 … MOD の jar を組む')


if __name__ == '__main__':
    main()
