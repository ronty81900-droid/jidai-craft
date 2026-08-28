# -*- coding: utf-8 -*-
"""
horu_kakunin.py -- 掘りのルートテーブルが、正本（horu_tsukuru.HYOU）どおりか・実機にも入っているか。

  動かし方:  python tests/horu_kakunin.py
"""
import io
import json
import os
import sys

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
sys.path.insert(0, KOKO)

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


def main():
    # 正本を「実行せずに」読む（main() が走らないよう、定義だけ取り出す）
    src = io.open(os.path.join(KOKO, 'horu_tsukuru.py'), encoding='utf-8').read()
    ns = {'__file__': os.path.join(KOKO, 'horu_tsukuru.py')}
    exec(src.split('\ndef main():')[0], ns)
    HYOU, ISHI, SHINSO_BAI, SAKI = ns['HYOU'], ns['ISHI'], ns['SHINSO_BAI'], ns['SAKI']

    check('正本に 鉄・ラピス・金・ダイヤ の4種がある', len(HYOU) == 4, HYOU)
    kitai = dict(HYOU)
    check('ご指示の確率（鉄15 / ラピス8 / 金4 / ダイヤ1 %）',
          kitai['minecraft:iron_ingot'] == 0.15 and kitai['minecraft:lapis_lazuli'] == 0.08
          and kitai['minecraft:gold_ingot'] == 0.04 and kitai['minecraft:diamond'] == 0.01, kitai)
    check('石は5種（石・花崗岩・閃緑岩・安山岩・深層岩）', set(ISHI) == {'stone', 'granite', 'diorite', 'andesite', 'deepslate'}, sorted(ISHI))
    check('深層岩は出やすい（倍率 > 1）', SHINSO_BAI > 1.0, SHINSO_BAI)

    for saki in SAKI:
        mei = os.path.relpath(saki, NE)
        if not os.path.isdir(saki):
            print('  （無いのでとばす: %s）' % mei)
            continue
        d = os.path.join(saki, 'data', 'minecraft', 'loot_tables', 'blocks')
        for ishi in ISHI:
            p = os.path.join(d, ishi + '.json')
            if not os.path.exists(p):
                check('%s: %s.json がある' % (mei, ishi), False, p)
                continue
            j = json.loads(io.open(p, encoding='utf-8').read())
            bai = SHINSO_BAI if ishi == 'deepslate' else 1.0
            mita = {}
            for pool in j['pools']:
                for c in pool.get('conditions', []):
                    if c.get('condition') == 'minecraft:random_chance':
                        mita[pool['entries'][0]['name']] = c['chance']
            warui = [m for m, p0 in HYOU if abs(mita.get(m, -1) - round(p0 * bai, 4)) > 1e-9]
            check('%s: %s.json の確率が正本どおり' % (mei, ishi), not warui,
                  '違う: %s / 実物=%s' % (warui, mita))
            # 掘った本体が出る（石→丸石 など）
            hon = j['pools'][0]['entries'][0]
            namae = hon.get('name') or [c['name'] for c in hon['children']]
            check('%s: %s.json で本体が出る' % (mei, ishi), bool(namae), hon)

    print('')
    print('=' * 60)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    return 1 if fail_ else 0


sys.exit(main())
