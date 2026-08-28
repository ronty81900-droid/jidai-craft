# -*- coding: utf-8 -*-
"""
sozai_kakunin.py -- Codex 納品の UI 素材と設計書を、機械で点検する。

  ★★ いちばん大事な検査 ★★
    「合成図（page_*.png）は、部品を並べたものと画素まで一致するか」。
    ここが一致すれば、次の2つが同時に証明できる。
      1. 設計書に書かれた座標が、実物と合っている
      2. 合成図に【余計なものが焼かれていない】（＝文字を焼いていない証明）
    manifest の自己申告 baked_text_used:false を、実物で裏取りする検査。

  動かし方:  python tests/sozai_kakunin.py [素材の根]
"""
import hashlib
import io
import json
import os
import sys

from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
NE_SOZAI = sys.argv[1] if len(sys.argv) > 1 else os.path.join(NE, 'ui')

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


def hiraku(rel):
    return Image.open(os.path.join(NE_SOZAI, rel)).convert('RGBA')


def kasanaru(a, b):
    """2つの矩形 (x,y,w,h) が重なるか"""
    return (a[0] < b[0] + b[2] and b[0] < a[0] + a[2]
            and a[1] < b[1] + b[3] and b[1] < a[1] + a[3])


def kazoku(c, buhin):
    """カードの family（大きさの種類）を、素材のファイル名から読む。

    ★ manifest に family を書いていないページ（銀行）と、
      state_assets が null のカード（銀行の「残高」＝押せない）があるため、
      【合成図に置かれている部品の素材名】を正本にする。
      family の申告がある時は、一致するかを別に確かめる。"""
    a = buhin.get(c['id'])
    if a is None and c.get('state_assets'):
        a = c['state_assets']['normal']
    if a is None:
        return None
    bu = os.path.basename(a)[:-4].split('_')
    return bu[1] if len(bu) >= 3 and bu[0] == 'card' else None


def main():
    print('=' * 62)
    print('UI 素材の点検')
    print('=' * 62)

    mp = os.path.join(NE_SOZAI, 'manifest.json')
    if not os.path.exists(mp):
        print('[SKIP] manifest.json がありません: %s' % mp)
        return 0
    m = json.loads(io.open(mp, encoding='utf-8').read())

    # -- 1) 基本 --
    check('論理キャンバスが 320x240', m['package']['logical_canvas'] == [320, 240],
          m['package']['logical_canvas'])
    check('名前空間が jidaiui', m['package']['namespace'] == 'jidaiui')
    check('1.20.1 向け', m['package']['minecraft'] == '1.20.1')

    # -- 2) ファイルがそろっているか --
    nai = [f['file'] for f in m['files']
           if not os.path.exists(os.path.join(NE_SOZAI, f['file']))]
    check('manifest の全ファイルが実在する (%d本)' % len(m['files']), not nai, nai)

    # -- 3) 全 PNG の寸法・色数・不透明度・中身のハッシュ --
    #    ★ sha256 まで見るのは、manifest と実物が別物にすり替わっていないかの確認。
    zuresun, iroooi, hantomei, hash_zure, iro_zure = [], [], [], [], []
    for f in m['files']:
        p = os.path.join(NE_SOZAI, f['file'])
        if not p.endswith('.png') or not os.path.exists(p):
            continue
        nakami = open(p, 'rb').read()
        if 'sha256' in f and hashlib.sha256(nakami).hexdigest().upper() != f['sha256'].upper():
            hash_zure.append(f['file'])
        if 'byte_size' in f and len(nakami) != f['byte_size']:
            hash_zure.append('%s の大きさ' % f['file'])
        im = Image.open(p).convert('RGBA')
        if list(im.size) != list(f['dimensions']):
            zuresun.append('%s=%s (%s のはず)' % (f['file'], list(im.size), f['dimensions']))
        px = list(im.getdata())
        if len(set(px)) > 256:
            iroooi.append('%s=%d色' % (f['file'], len(set(px))))
        if 'rgba_color_count' in f and len(set(px)) != f['rgba_color_count']:
            iro_zure.append('%s=%d色 (%d と申告)' % (f['file'], len(set(px)), f['rgba_color_count']))
        if any(q[3] not in (0, 255) for q in px):
            hantomei.append(f['file'])
    check('全 PNG の実寸が manifest どおり', not zuresun, zuresun)
    check('★全 PNG の中身が manifest の sha256 と一致', not hash_zure, hash_zure)
    check('全 PNG が 256色以下', not iroooi, iroooi)
    check('全 PNG の色数が manifest の申告どおり', not iro_zure, iro_zure)
    check('全 PNG に半透明が無い（0 か 255 のみ）', not hantomei, hantomei)

    # -- 3-2) ★★ 文字を置く場所が、本当に無地か ★★
    #    manifest は1枚ごとに「ここに文字やアイコンを描く」領域を申告している。
    #    そこに絵が入っていたら、実行時に描いた文字と重なる。
    #    無地（1色）であることを画素で確かめる＝「文字を焼いていない」の実物での裏取り。
    muji_zure = []
    for f in m['files']:
        p = os.path.join(NE_SOZAI, f['file'])
        if not os.path.exists(p) or not f.get('blank_regions'):
            continue
        im = Image.open(p).convert('RGBA')
        for br in f['blank_regions']:
            x, y, w, h = br['rect']
            iro = set(im.crop((x, y, x + w, y + h)).getdata())
            if len(iro) != 1:
                muji_zure.append('%s の %s が %d色' % (f['file'], br['label'], len(iro)))
    check('★★文字とアイコンを置く領域が、すべて無地（＝焼き込みが無い）',
          not muji_zure, muji_zure)

    # -- 4) 9分割 --
    for na, ns in m['nine_slice'].items():
        im = hiraku(ns['file'])
        check('%s が 24x24' % na, list(im.size) == [24, 24], im.size)
        check('%s のマスが 8x8' % na, ns['cell_size'] == [8, 8], ns['cell_size'])
        # 四隅の鏡像一致（納品側の申告を裏取りする）
        lt = im.crop((0, 0, 8, 8))
        rt = im.crop((16, 0, 24, 8)).transpose(Image.FLIP_LEFT_RIGHT)
        check('%s の左上と右上が鏡像' % na, list(lt.getdata()) == list(rt.getdata()))

    # -- 5) ページごと --
    for pid, p in m['pages'].items():
        check('%s: キャンバスが 320x240' % pid, p['canvas'] == [320, 240], p['canvas'])
        base = hiraku(p['base_asset'])
        check('%s: 合成図が 320x240' % pid, base.size == (320, 240), base.size)
        buhin = {c['id']: c['asset'] for c in p['components']}

        # 5-1) カードの寸法が family と合うか / 画面内 / 重ならない
        warui, hamidashi, fam_zure = [], [], []
        for c in p['cards']:
            kf = kazoku(c, buhin)
            if 'family' in c and c['family'] != kf:
                fam_zure.append('%s: 申告=%s 素材=%s' % (c['id'], c['family'], kf))
            fam = m['card_families'][kf]
            if c['rect'][2:] != fam['size']:
                warui.append('%s=%s (%s のはず)' % (c['id'], c['rect'][2:], fam['size']))
            x, y, w, h = c['rect']
            if x < 0 or y < 0 or x + w > 320 or y + h > 240:
                hamidashi.append(c['id'])
        check('%s: カードの寸法が family どおり' % pid, not warui, warui)
        check('%s: 申告した family と素材の名前が一致' % pid, not fam_zure, fam_zure)
        check('%s: カードが 320x240 に収まる' % pid, not hamidashi, hamidashi)

        kasa = []
        cs = p['cards']
        for i in range(len(cs)):
            for j in range(i + 1, len(cs)):
                if kasanaru(cs[i]['rect'], cs[j]['rect']):
                    kasa.append('%s と %s' % (cs[i]['id'], cs[j]['id']))
        check('%s: カード同士が重ならない' % pid, not kasa, kasa)

        # 5-2) ★算術: カード座標 + family の相対座標 == 設計書の文字座標
        moji = {t['id']: t['rect'] for t in p['runtime_text']}
        zure = []
        for c in p['cards']:
            fam = m['card_families'][kazoku(c, buhin)]
            for suffix, key in (('_name', 'name'), ('_price', 'price'), ('_unlock', 'meta')):
                t = moji.get(c['id'] + suffix)
                if t is None:
                    continue
                kitai = [c['rect'][0] + fam[key][0], c['rect'][1] + fam[key][1],
                         fam[key][2], fam[key][3]]
                if t != kitai:
                    zure.append('%s%s=%s (%s のはず)' % (c['id'], suffix, t, kitai))
            kitai = [c['rect'][0] + fam['item'][0], c['rect'][1] + fam['item'][1],
                     fam['item'][2], fam['item'][3]]
            if c.get('game_icon_rect') and c['game_icon_rect'] != kitai:
                zure.append('%s のアイコン=%s (%s のはず)' % (c['id'], c['game_icon_rect'], kitai))
        check('%s: 文字とアイコンの座標が「カード＋相対」と一致' % pid, not zure, zure)

        # 5-3) 文字領域がカードからはみ出していないか
        deru = []
        for c in p['cards']:
            for suffix in ('_name', '_price', '_unlock'):
                t = moji.get(c['id'] + suffix)
                if t is None:
                    continue
                if (t[0] < c['rect'][0] or t[1] < c['rect'][1]
                        or t[0] + t[2] > c['rect'][0] + c['rect'][2]
                        or t[1] + t[3] > c['rect'][1] + c['rect'][3]):
                    deru.append(c['id'] + suffix)
        check('%s: 文字がカードの外へ出ていない' % pid, not deru, deru)

        # 5-4) ★★ 合成図を部品から再現する ★★
        tsukutta = Image.new('RGBA', (320, 240), (0, 0, 0, 0))
        for comp in p['components']:
            tsukutta.alpha_composite(hiraku(comp['asset']),
                                     (comp['rect'][0], comp['rect'][1]))
        chigau = sum(1 for a, b in zip(list(base.getdata()), list(tsukutta.getdata()))
                     if a != b)
        check('★★%s: 合成図が部品の重ね合わせと画素まで一致' % pid,
              chigau == 0, '違う画素 %d 個 / 76800' % chigau)

        # 5-5) 状態素材が5種そろい、寸法が family と一致
        tarinai = []
        for c in p['cards']:
            if not c.get('state_assets'):
                continue          # 押せないカード（銀行の「残高」）は状態を持たない
            fam = m['card_families'][kazoku(c, buhin)]
            iru = c.get('states_used') or ('normal', 'hover', 'pressed',
                                           'locked', 'insufficient')
            for st in iru:
                a = c['state_assets'].get(st)
                if a is None:
                    tarinai.append('%s の %s' % (c['id'], st))
                elif list(hiraku(a).size) != fam['size']:
                    tarinai.append('%s の %s が %s' % (c['id'], st, list(hiraku(a).size)))
        check('%s: 使う状態がそろい、寸法も一致' % pid, not tarinai, tarinai)

        # 5-6) カードの「ふつう」の素材が、合成図に置かれている素材と同じか
        #      ★ ずれると、カーソルを外した瞬間に絵が変わる
        chigau_moto = []
        for c in p['cards']:
            if not c.get('state_assets'):
                continue
            if buhin.get(c['id']) != c['state_assets']['normal']:
                chigau_moto.append('%s: 部品=%s 状態=%s'
                                   % (c['id'], buhin.get(c['id']),
                                      c['state_assets']['normal']))
        check('%s: カードの「ふつう」の絵が合成図と同じ' % pid, not chigau_moto, chigau_moto)

        # 5-7) カードの絵を使う部品が、cards にも載っているか（宣言漏れの検出）
        card_id = set(c['id'] for c in p['cards'])
        more = [c['id'] for c in p['components']
                if os.path.basename(c['asset']).startswith('card_')
                and c['id'] not in card_id]
        check('%s: カードの絵を使う部品はすべて cards に載っている' % pid, not more, more)

    # -- 6) 申告の確認 --
    check('manifest が「文字を焼いていない」と申告している',
          m['provenance']['baked_text_used'] is False)
    check('manifest が「画像生成モデルを使っていない」と申告している',
          m['provenance']['image_generation_model_used'] is False)

    print('')
    print('=' * 62)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    print('')
    print('★ ここが全部通っても「ゲームで見た」ことにはならない。')
    print('  文字が枠に収まるかは、実機の書体で測るまで分からない。')
    return 1 if fail_ else 0


sys.exit(main())
