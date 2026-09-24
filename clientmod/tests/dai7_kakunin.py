# -*- coding: utf-8 -*-
"""
dai7_kakunin.py -- Codex 第7回（v9・便ごと・各3案・256×256）の納品を機械で点検する。

  依頼文 = clientmod/Codex依頼文_第7回.md §7。規則の正本は tools/dotto.py の tenken()。

  動かし方:
    python tests/dai7_kakunin.py nouhin/v9/dai1              … 第1便（便の番号は manifest の package.batch）
    python tests/dai7_kakunin.py <根> --mono kaizu             … 絵を絞る
    python tests/dai7_kakunin.py <根> --iraibun-nashi          … 依頼文の sha256 照合を飛ばす
    python tests/dai7_kakunin.py <根> --hikaku-nashi           … 見比べ表（hikaku/<絵>.png）を作らない

  ★ 第6回の教訓: manifest が「3案」を申告しても zip に A しか無いことがあった。
    ここでは「A/B/C × 便の枚数」が全部 実在するかを最初に数える。
"""
import hashlib
import io
import json
import os
import re
import sys

import numpy as np
from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)                     # clientmod
KIKAKU = os.path.dirname(NE)
sys.path.insert(0, os.path.join(NE, 'tools'))
import dotto  # noqa: E402

SHOURI = os.path.join(KIKAKU, 'plugin', 'src', 'main', 'java', 'jidai', 'Shouri.java')
IRAIBUN = os.path.join(NE, 'Codex依頼文_第7回.md')
V5 = os.path.join(NE, 'nouhin', 'v5', 'assets', 'jidaiui', 'textures', 'item')
V6 = os.path.join(NE, 'nouhin', 'v6', 'assets', 'jidaiui', 'textures', 'item')

KAKUTEI = ('gofu', 'seihai')                   # 確定済み（第7回の対象外）
AN = ('a', 'b', 'c')
PREVIEW_SIZES = (48, 32, 16)
TSUIKA_JOUGEN = 4
HOSOI_JOUGEN = 0.10                            # 3px 未満の細い要素の上限（実測: きれいな絵 0〜8%・1〜2px の模様 22〜60%）
OOKISA = 256

pass_ = fail_ = 0


def check(mei, jouken, riyuu=''):
    global pass_, fail_
    if jouken:
        pass_ += 1
        print('[PASS] ' + mei)
    else:
        fail_ += 1
        print('[FAIL] ' + mei + '  ' + str(riyuu))


def shouri_hyou():
    """Shouri.java から (名前, ファイル名, 番号) を表の順に。"""
    s = io.open(SHOURI, encoding='utf-8').read()
    kekka = []
    m = re.search(r'TOKUSHU_HYOU = \{(.*?)\n    \};', s, re.S)
    kekka += [(a, b, int(c)) for a, b, c in re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)"\}', m.group(1))]
    m = re.search(r'IBUTSU_HYOU = \{(.*?)\n    \};', s, re.S)
    kekka += [(a, b, int(c)) for a, b, c in
              re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)",\s*"\d",\s*"[A-Z]+",\s*"[^"]*"\}', m.group(1))]
    return kekka


def bin_hyou():
    """便 → [(名前, ファイル名, 番号)]。確定済みを除いて 3 枚ずつ。"""
    nokori = [k for k in shouri_hyou() if k[1] not in KAKUTEI]
    return {i + 1: nokori[i * 3:(i + 1) * 3] for i in range((len(nokori) + 2) // 3)}


def png_arr(p):
    return np.array(Image.open(p).convert('RGBA'))


def hanten_onaji(a, b):
    """b が a の 同じ・左右反転・上下反転・90/180/270 回転 のどれかなら True。"""
    if a.shape != b.shape:
        return False
    kouho = [a, a[:, ::-1], a[::-1, :], np.rot90(a, 1), np.rot90(a, 2), np.rot90(a, 3),
             np.rot90(a[:, ::-1], 1), np.rot90(a[:, ::-1], 3)]
    return any(np.array_equal(k, b) for k in kouho)


def main():
    argv = sys.argv[1:]
    if not argv:
        print(__doc__)
        return 2
    ne = argv[0]
    mono = None
    for i, a in enumerate(argv):
        if a == '--mono':
            mono = set(argv[i + 1].split(','))
    iraibun_nashi = '--iraibun-nashi' in argv
    hikaku_nashi = '--hikaku-nashi' in argv

    mp = os.path.join(ne, 'manifest.json')
    check('manifest.json がある', os.path.exists(mp), mp)
    if not os.path.exists(mp):
        return owari()
    man = json.loads(io.open(mp, encoding='utf-8').read())
    pkg = man.get('package', {})
    bin_ban = pkg.get('batch')
    hyou = bin_hyou()
    check('manifest.package.batch が 1〜%d' % len(hyou), bin_ban in hyou, bin_ban)
    if bin_ban not in hyou:
        return owari()
    kitai = [k for k in hyou[bin_ban] if not mono or k[1] in mono]
    check('第%d便の絵 = %s' % (bin_ban, [k[1] for k in hyou[bin_ban]]), True)

    # 依頼文の sha256
    if not iraibun_nashi:
        ira = hashlib.sha256(open(IRAIBUN, 'rb').read()).hexdigest().upper()
        check('manifest の source_request_sha256 が依頼文と一致',
              str(pkg.get('source_request_sha256', '')).upper() == ira, '%s / %s' % (str(pkg.get('source_request_sha256', ''))[:12], ira[:12]))

    # 追加色
    tsuika = man.get('added_colors', []) or []
    check('追加色が %d 色以内（%d）' % (TSUIKA_JOUGEN, len(tsuika)), len(tsuika) <= TSUIKA_JOUGEN)
    for c in tsuika:
        check('追加色 %s に役割・RGBA・理由がある' % c.get('role'),
              bool(c.get('role')) and re.fullmatch(r'#[0-9A-Fa-f]{8}', str(c.get('rgba', ''))) and len(str(c.get('reason', ''))) >= 4)
    yurusu = set(dotto.PAL.values()) | {dotto.TOUMEI}
    for c in tsuika:
        hx = str(c.get('rgba', '')).lstrip('#')
        if len(hx) == 8:
            yurusu.add(tuple(int(hx[i:i + 2], 16) for i in (0, 2, 4, 6)))

    # ★ 実在の数え上げ（A/B/C × 枚数）
    gazou = {}       # (an, fai) -> array
    for an in AN:
        for _mei, fai, _ban in kitai:
            p = os.path.join(ne, 'proposal_' + an, 'assets', 'jidaiui', 'textures', 'item', fai + '.png')
            aru = os.path.exists(p)
            check('案%s %s.png が実在する' % (an.upper(), fai), aru, p)
            if aru:
                gazou[(an, fai)] = png_arr(p)
    check('本番 %d 枚がすべて実在（%d）' % (len(AN) * len(kitai), len(gazou)), len(gazou) == len(AN) * len(kitai))

    # manifest の files を引く
    files = {f['file'].replace('\\', '/'): f for f in man.get('files', [])}

    # 1 枚ずつ
    for (an, fai), a in sorted(gazou.items()):
        rel = 'proposal_%s/assets/jidaiui/textures/item/%s.png' % (an, fai)
        na = '案%s %s' % (an.upper(), fai)
        im = Image.fromarray(a, 'RGBA')
        for gou, bun in dotto.tenken(im, na):
            # パレットは追加色も許す（tenken は v6＋hada だけ見る）
            if 'パレット' in bun:
                iro_shu = set(map(tuple, a.reshape(-1, 4).tolist()))
                hazure = sorted(c for c in iro_shu if c not in yurusu)
                check('%s: 全色が共有パレット＋追加色 %s' % (na, hazure[:4] if hazure else ''), not hazure)
            else:
                check(bun, gou)
        check('%s: 256×256' % na, a.shape[:2] == (OOKISA, OOKISA), a.shape[:2])
        if a.shape[:2] != (OOKISA, OOKISA):
            continue
        # §4 細かすぎない: 3px 未満の細い要素が不透明画素の 10% 以下
        hw = dotto.hosoi_wariai(a)
        check('%s: 3px 未満の細い要素が %.1f%%（%d%% 以下）' % (na, hw * 100, HOSOI_JOUGEN * 100), hw <= HOSOI_JOUGEN)
        # manifest の申告
        f = files.get(rel)
        check('%s: manifest に載っている' % na, f is not None)
        if f is not None:
            b = open(os.path.join(ne, rel), 'rb').read()
            h = hashlib.sha256(b).hexdigest().upper()
            iro_kazu = len(set(map(tuple, a.reshape(-1, 4).tolist())))
            check('%s: manifest の sha256（大文字）・byte 数・寸法・色数が実物と一致' % na,
                  f.get('sha256') == h and f.get('byte_size') == len(b) and list(f.get('dimensions', [])) == [OOKISA, OOKISA]
                  and f.get('rgba_color_count') == iro_kazu,
                  'sha=%s/%s size=%s/%s dim=%s iro=%s/%s' % (str(f.get('sha256'))[:8], h[:8], f.get('byte_size'), len(b), f.get('dimensions'), f.get('rgba_color_count'), iro_kazu))
        # preview 48/32/16 = nearest 縮小
        for sz in PREVIEW_SIZES:
            pv = os.path.join(ne, 'preview', 'proposal_' + an, '%s_%d.png' % (fai, sz))
            aru = os.path.exists(pv)
            check('%s: preview %d がある' % (na, sz), aru, pv)
            if aru:
                pa = png_arr(pv)
                kitai_pa = np.array(im.resize((sz, sz), Image.NEAREST))
                check('%s: preview %d は本番の nearest 縮小と 1 画素も違わない' % (na, sz),
                      pa.shape == kitai_pa.shape and np.array_equal(pa, kitai_pa))
        # 海図の色
        if fai == 'kaizu':
            iro_shu = set(map(tuple, a.reshape(-1, 4).tolist()))
            midori = {dotto.PAL[k] for k in ('lens_deep', 'lens', 'lens_light', 'enamel_green')}
            hada = {dotto.PAL[k] for k in ('hada_light', 'hada', 'hada_dark')}
            check('%s: 翠（lens 系）を使っている' % na, bool(iro_shu & midori))
            check('%s: 肌色（hada 系）を使っている' % na, bool(iro_shu & hada))

    # 3 案が互いに違う・他の絵の使い回しでない
    for _mei, fai, _ban in kitai:
        ans = [(an, gazou[(an, fai)]) for an in AN if (an, fai) in gazou]
        for i in range(len(ans)):
            for j in range(i + 1, len(ans)):
                check('%s: 案%s と 案%s が別の絵（反転・回転のコピーでない）' % (fai, ans[i][0].upper(), ans[j][0].upper()),
                      not hanten_onaji(ans[i][1], ans[j][1]))
    keys = sorted(gazou)
    for i in range(len(keys)):
        for j in range(i + 1, len(keys)):
            if keys[i][1] != keys[j][1]:
                check('%s/%s と %s/%s が別の絵' % (keys[i][0], keys[i][1], keys[j][0], keys[j][1]),
                      not np.array_equal(gazou[keys[i]], gazou[keys[j]]))

    # 見比べ表（発注元が選ぶ用）
    if not hikaku_nashi and gazou:
        for mei, fai, ban in kitai:
            kumi = []
            ima = os.path.join(V5, fai + '.png')
            if not os.path.exists(ima):
                ima = os.path.join(V6, fai + '.png')
            if os.path.exists(ima):
                kumi.append(('今（v5/v6）', Image.open(ima).convert('RGBA')))
            for an in AN:
                if (an, fai) in gazou:
                    kumi.append(('案 ' + an.upper(), Image.fromarray(gazou[(an, fai)], 'RGBA')))
            saki = os.path.join(ne, 'hikaku', fai + '.png')
            dotto.hikaku(kumi, saki, '%s（%d・第%d便）' % (mei, ban, bin_ban))
            print('[INFO] 見比べ表 ' + saki)
    return owari()


def owari():
    print('')
    print('=' * 60)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    return 1 if fail_ else 0


if __name__ == '__main__':
    sys.exit(main())
