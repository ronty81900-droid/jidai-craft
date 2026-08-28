# -*- coding: utf-8 -*-
"""
horu_tsukuru.py -- 「石を掘ると貴金属が出る」ルートテーブルを5種の石ぶん作る。

  ★★ 正本はこのファイルの HYOU ★★
    鉄 / ラピス / 金 / ダイヤ の確率を変えるならここだけ。
    5つの JSON を手で直すと、1つだけ古いまま残る。

  ★ 2026-08-22 のご指示
    ・石・花崗岩・閃緑岩・安山岩・深層岩 の全部から出る
    ・深層岩は出やすい（1.5倍）
    ・ラピス 8% / 金 4% / ダイヤ 1%（鉄は 15% のまま）
  ★ ネザライトはプラグイン（JidaiCraft.NEZA_KAKURITSU: 石 1/1000・深層岩 1/500）。ここには無い。

  使い方:  python tests/horu_tsukuru.py
           → datapacks/jidai_craft と、実機（jikki_mod / jikki_1201）の world/datapacks に書く
"""
import io
import json
import os

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)

# (品, 確率)。それぞれ独立に判定される（1ブロックから複数 出ることもある）
HYOU = [
    ('minecraft:iron_ingot',   0.15),
    ('minecraft:lapis_lazuli', 0.08),
    ('minecraft:gold_ingot',   0.04),
    ('minecraft:diamond',      0.01),
]

# 深層岩の倍率
SHINSO_BAI = 1.5

# 石の種類 → (シルクタッチで出る物, ふつうに出る物)。同じなら1つ
ISHI = {
    'stone':     ('minecraft:stone',     'minecraft:cobblestone'),
    'granite':   ('minecraft:granite',   'minecraft:granite'),
    'diorite':   ('minecraft:diorite',   'minecraft:diorite'),
    'andesite':  ('minecraft:andesite',  'minecraft:andesite'),
    'deepslate': ('minecraft:deepslate', 'minecraft:cobbled_deepslate'),
}

SAKI = [
    os.path.join(NE, 'datapacks', 'jidai_craft'),
    os.path.join(NE, 'jikki_mod', 'world', 'datapacks', 'jidai_craft'),
    os.path.join(NE, 'jikki_1201', 'world', 'datapacks', 'jidai_craft'),
]


def kakuritsu(ishi):
    bai = SHINSO_BAI if ishi == 'deepslate' else 1.0
    return [(mono, round(p * bai, 4)) for mono, p in HYOU]


def hontai(ishi):
    silk, futsuu = ISHI[ishi]
    if silk == futsuu:
        moto = {'type': 'minecraft:item', 'name': futsuu,
                'conditions': [{'condition': 'minecraft:survives_explosion'}]}
    else:
        moto = {'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': silk,
             'conditions': [{'condition': 'minecraft:match_tool',
                             'predicate': {'enchantments': [
                                 {'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}]},
            {'type': 'minecraft:item', 'name': futsuu,
             'conditions': [{'condition': 'minecraft:survives_explosion'}]},
        ]}
    pools = [{'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [moto]}]
    for mono, p in kakuritsu(ishi):
        pools.append({
            'rolls': 1.0, 'bonus_rolls': 0.0,
            'conditions': [
                {'condition': 'minecraft:random_chance', 'chance': p},
                {'condition': 'minecraft:match_tool', 'predicate': {'tag': 'minecraft:pickaxes'}},
            ],
            'entries': [{'type': 'minecraft:item', 'name': mono}],
        })
    return {'type': 'minecraft:block', 'random_sequence': 'minecraft:blocks/' + ishi, 'pools': pools}


def main():
    for saki in SAKI:
        if not os.path.isdir(saki):
            print('  とばす（無い）: %s' % saki)
            continue
        d = os.path.join(saki, 'data', 'minecraft', 'loot_tables', 'blocks')
        os.makedirs(d, exist_ok=True)
        for ishi in ISHI:
            p = os.path.join(d, ishi + '.json')
            io.open(p, 'w', encoding='utf-8', newline='\n').write(
                json.dumps(hontai(ishi), ensure_ascii=False, indent=2) + '\n')
        print('  書いた: %s (%d 種)' % (saki, len(ISHI)))
    print('')
    print('確率（ピッケルで掘った時・それぞれ独立）:')
    for ishi in ISHI:
        print('  %-9s ' % ishi + ' / '.join('%s %.1f%%' % (m.split(':')[1], p * 100) for m, p in kakuritsu(ishi)))


main()
