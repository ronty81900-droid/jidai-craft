# -*- coding: utf-8 -*-
"""
mise_kumikae.py -- 販売所の3ページ（生活・防具・武器）を組み直す。

  ★★ なぜ要るか ★★
    2026-08-22 に剣を専用のタブ「武器」へ移した（ご指示）。
    タブが2枚から3枚に増えたので、
    販売所の3ページとも【タブの帯を描き直す】必要がある。
    「武器」ページ自体も新しく作る。

  ★★ なぜ Codex に頼み直さないか ★★
    合成図（page_*.png）は「下地 + タブ + ふつうのカード」を
    宣言された座標に重ねただけのもの。使う部品はすべて納品済み・検査済み。
    新しい絵は1枚も要らないので、こちらで組み直せる。
    結果は tests/sozai_kakunin.py が画素単位で確かめる。

  ★ このファイルが販売所ページの正本。manifest.json を手で書き換えない。

  使い方:  python tools/mise_kumikae.py
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

# タブは3枚。左から 62px 幅で 2px 空けて並べる。
TABU = [(20, 38), (84, 38), (148, 38)]
TABU_MEI = ['生活', '防具', '武器']
TABU_HABA = 62
TABU_TAKASA = 18

# ページごとのカードの種類。
#   compact … 94x52 を 3列 x 2行（6枚まで）
#   micro   … 69x52 を 4列 x 3行（12枚まで。防具ページと同じ）
# ★ 2026-08-22: 生活が 10品 になったので、生活だけ micro へ。
KAZOKU = {'shop_life': 'micro', 'shop_weapon': 'compact'}

# 商品カードを置く場所（種類ごと）
BASHO = {
    'compact': [(16, 60), (113, 60), (210, 60), (16, 114), (113, 114), (210, 114)],
    'micro': [(16, 60), (89, 60), (162, 60), (235, 60),
              (16, 114), (89, 114), (162, 114), (235, 114),
              (16, 168), (89, 168), (162, 168), (235, 168)],
}

# 並びは plugin の Shop.java の枠と 1対1。
#   (名前, 財布, 値段, 解禁)
# ★ 名前は hyou_tsukuru.py が Shop.java の名前と突き合わせる（「×N」を除いて一致すること）。
SHINAMONO = {
    'shop_life': [
        ('パン ×3',          '個人', '2',   '鉄器'),
        ('石炭',             '個人', '5',   '鉄器'),
        ('オークの原木 ×2',  '個人', '10',  '鉄器'),
        ('石レンガ ×5',      '個人', '12',  '鉄器'),
        ('松明 ×16',         '個人', '5',   '鉄器'),
        ('焼肉 ×5',          '個人', '30',  '中世'),
        ('ガラス ×5',        '個人', '25',  '中世'),
        ('戦争宣誓',         '勢力', '100', '中世'),
        ('金リンゴ',         '個人', '100', '近代'),
        ('下剋上',           '勢力', '150', '近代'),
    ],
    'shop_weapon': [
        ('鉄の剣',     '勢力', '100', '鉄器'),
        ('弓',         '勢力', '150', '鉄器'),
        ('矢 ×10',     '個人', '30',  '鉄器'),
        ('ダイヤの剣', '勢力', '500', '中世'),
        ('クロスボウ', '勢力', '150', '中世'),
    ],
}

# ページ → 何枚目のタブが選ばれているか
ERABU = {'shop_life': 0, 'shop_armor': 1, 'shop_weapon': 2}


def hiraku(rel):
    return Image.open(os.path.join(UI, rel)).convert('RGBA')


def tabu_moji(pid):
    """3枚ぶんのタブの文字。選ばれている物だけ明るい色。"""
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
    """商品カードと、その文字を作る。"""
    kazoku = KAZOKU[pid]
    fam = m['card_families'][kazoku]
    w, h = fam['size']
    if len(SHINAMONO[pid]) > len(BASHO[kazoku]):
        raise SystemExit('[中止] %s: 品が %d あるのに、%s の置き場は %d しか無い'
                         % (pid, len(SHINAMONO[pid]), kazoku, len(BASHO[kazoku])))
    kado, moji = [], []
    for i, ((namae, saifu, nedan, kaikin), (x, y)) in enumerate(
            zip(SHINAMONO[pid], BASHO[kazoku]), start=1):
        cid = 'item_%d' % i
        kado.append({
            'id': cid, 'rect': [x, y, w, h], 'family': kazoku,
            'state_assets': {st: E + 'card_%s_%s.png' % (kazoku, st)
                             for st in ('normal', 'hover', 'pressed',
                                        'locked', 'insufficient')},
            'game_icon_rect': [x + fam['item'][0], y + fam['item'][1],
                               fam['item'][2], fam['item'][3]],
            'name': namae, 'price': nedan, 'wallet': saifu,
            'display_price': saifu + ' ' + nedan, 'unlock': kaikin,
            'tooltip': None, 'pressed_content_offset': [1, 1],
        })
        for suffix, key, iro in (('_name', 'name', '#F5E8C8'),
                                 ('_price', 'price', '#C4934B'),
                                 ('_unlock', 'meta', '#AEB8AE')):
            r = fam[key]
            moji.append({
                'id': cid + suffix,
                'content': {'_name': namae, '_price': saifu + ' ' + nedan,
                            '_unlock': kaikin}[suffix],
                'rect': [x + r[0], y + r[1], r[2], r[3]],
                'font_px': 8, 'line_height_px': 10,
                'max_lines': 2 if suffix == '_name' else 1,
                'color': iro, 'align': 'left', 'dynamic': False, 'notes': '',
            })
    return kado, moji


def main():
    m = json.loads(io.open(MANIFEST, encoding='utf-8').read())

    # 「武器」ページは、まだ無ければ「生活」を写して作る
    if 'shop_weapon' not in m['pages']:
        moto = json.loads(json.dumps(m['pages']['shop_life'], ensure_ascii=False))
        moto['id'] = 'shop_weapon'
        moto['base_asset'] = E + 'page_shop_weapon.png'
        m['pages']['shop_weapon'] = moto

    for pid in ('shop_life', 'shop_armor', 'shop_weapon'):
        p = m['pages'][pid]

        # 見出しと財布はそのまま。タブの文字だけ3枚ぶんに入れ替える。
        nokosu = ('title', 'wallet_personal', 'wallet_faction')
        moji = [t for t in p['runtime_text'] if t['id'] in nokosu]
        moji += tabu_moji(pid)

        buhin = [c for c in p['components'] if c['id'] == 'screen_chrome']
        buhin += tabu_buhin(pid)

        if pid == 'shop_armor':
            # 防具は12枠のまま。カードと文字はそのまま使う。
            kado = p['cards']
            moji += [t for t in p['runtime_text'] if t['id'].startswith('item_')]
            for c in kado:
                buhin.append({'id': c['id'], 'asset': c['state_assets']['normal'],
                              'rect': c['rect']})
        else:
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
        kiroku = None
        for f in m['files']:
            if f['file'] == rel:
                kiroku = f
                break
        if kiroku is None:
            kiroku = {'file': rel,
                      'resource_location': 'jidaiui:' + rel.split('assets/jidaiui/', 1)[-1],
                      'purpose': '販売所「武器」タブの合成図', 'category': 'page'}
            m['files'].append(kiroku)
        kiroku['dimensions'] = [320, 240]
        kiroku['blank_regions'] = kara
        kiroku['byte_size'] = len(nakami)
        kiroku['sha256'] = hashlib.sha256(nakami).hexdigest().upper()
        kiroku['rgba_color_count'] = len(set(px))
        kiroku['alpha_values'] = sorted(set(q[3] for q in px))
        kiroku['transparent_rgb_zero'] = True
        kiroku['blank_regions_verified_flat'] = len(kara)
        kiroku['contains_baked_text'] = False

        print('  %-12s カード %2d / 文字 %2d / %d色' % (pid, len(kado), len(moji), len(set(px))))

    m['package']['effective_page_count'] = len(m['pages'])
    m['provenance']['modified_after_delivery'] = (
        '2026-08-22: 剣を「武器」タブへ移したため、販売所を3タブ・3ページへ組み直した。'
        '新しい絵は作っていない。納品・検査済みの部品を並べ直しただけ'
        '（tools/mise_kumikae.py）。'
    )

    io.open(MANIFEST, 'w', encoding='utf-8', newline='\n').write(
        json.dumps(m, ensure_ascii=False, indent=1))
    print('組み直しました: 画面 %d 枚' % len(m['pages']))


main()
