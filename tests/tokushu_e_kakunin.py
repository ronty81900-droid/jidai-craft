# -*- coding: utf-8 -*-
"""特殊アイテムの絵（v7・高解像度）を機械で点検する。docs/Fable依頼文_特殊アイテム.md §6。

  ★★ 自己申告を実物で裏取りする ★★
    manifest.json や DESIGN_SPEC.md に書いてある事を信じず、保存された画素から数え直す。

  ★★ いちばん効く2つ ★★
    ① 本番を nearest で 16×16 に縮めた物が、下絵と1画素も違わない
       （MOD の画面 Kazari.aitem は nearest で縮めて貼る。ここがずれると小さい画面で崩れる）
    ② 各格子の平均色が下絵に近い（RGB 各成分 40 以内・アルファ 25% 以内）
       （バニラの描画は平均で縮める＝ミップマップ。ここがずれると手持ち・地面で別の色になる）

  見る物（§6 の 1〜14）:
     1 枚数とファイル名が Shouri.java と一致       8 使った色が v6 の共有パレット（＋断った新色）にある
     2 大きさが 512 / 256                           9 highlight が1つの連結したまとまり
     3 ① nearest 縮小 ＝ 下絵                      10 素材面の内部に outline が無い（下絵＝透明に接する格子だけ／本番＝透明から 1.75 格子以内）
     4 ② 格子の平均色                              11 外周の透明余白（512: 24〜64px / 256: 12〜32px。下限は指示文 §3、上限は2格子）
     5 アルファが 0 か 255                          12 プレビュー ＝ 本番の nearest 1/2、preview_shitae ＝ 下絵の 8倍
     6 透明画素の RGB が 0                          13 manifest の sha256・寸法・byte 数が実物と一致
     7 色数 64 以内                                 14 スクリプトを2回動かして byte が一致

  動かし方:
    python tests/tokushu_e_kakunin.py                                   … clientmod/nouhin/v7（16枚）
    python tests/tokushu_e_kakunin.py clientmod/nouhin/v7/an/A --mono tsukinoishi,sensha
    python tests/tokushu_e_kakunin.py <根> --nidome-nashi                … 14（2回動かす）を飛ばす

  ★ 書いた検査は、わざと壊して落ちることを確かめてある（1画素・manifest の1文字・プレビュー差し替え）。
"""
import hashlib
import io
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image

NE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOURI = os.path.join(NE, 'plugin', 'src', 'main', 'java', 'jidai', 'Shouri.java')
V6_SPEC = os.path.join(NE, 'clientmod', 'nouhin', 'v6', 'DESIGN_SPEC.md')
IRAIBUN = os.path.join(NE, 'docs', 'Fable依頼文_特殊アイテム.md')
TSUKURU = os.path.join(NE, 'clientmod', 'tools', 'tokushu_e_tsukuru.py')

OUTLINE = '#102329FF'
HIGHLIGHT = '#F5DE9BFF'
IRO_JOUGEN = 64
RGB_SA = 40
ALPHA_SA = 255 * 0.25
YOHAKU = {512: (24, 64), 256: (12, 32)}      # 指示文 §3「512なら 24〜32px」を下限に、上限は2格子（絵が小さくならないように）

pass_ = fail_ = 0


def check(mei, jouken, riyuu=''):
    global pass_, fail_
    if jouken:
        pass_ += 1
        print('[PASS] ' + mei)
    else:
        fail_ += 1
        print('[FAIL] ' + mei + '  ' + str(riyuu))


def yomu(p):
    return io.open(p, encoding='utf-8').read()


def iro(px):
    return '#%02X%02X%02X%02X' % tuple(int(v) for v in px)


def hex_rgba(hx):
    hx = hx.lstrip('#')
    return tuple(int(hx[i:i + 2], 16) for i in (0, 2, 4, 6))


def shouri_hyou():
    """Shouri.java の正本。[(名前, ファイル名, 番号, 大きさ)]"""
    s = yomu(SHOURI)
    kekka = []
    m = re.search(r'TOKUSHU_HYOU = \{(.*?)\n    \};', s, re.S)
    for a, b, c in re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)"\}', m.group(1)):
        kekka.append((a, b, int(c), 512))
    m = re.search(r'IBUTSU_HYOU = \{(.*?)\n    \};', s, re.S)
    for a, b, c in re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)",\s*"\d",\s*"[A-Z]+",\s*"[^"]*"\}', m.group(1)):
        kekka.append((a, b, int(c), 256))
    return kekka


def v6_paretto():
    s = yomu(V6_SPEC)
    setsu = re.search(r'## 共有パレット(.*?)\r?\n## ', s, re.S)
    return {hex_rgba(hx) for _na, hx in re.findall(r'\| `([a-z_]+)` \| `(#[0-9A-F]{8})` \|', setsu.group(1))}


def shinshoku(spec):
    """DESIGN_SPEC の「足した色:」の行から新色を読む。"""
    m = re.search(r'足した色: (.*)', spec)
    if not m or '無し' in m.group(1):
        return {}
    return {na: hex_rgba(hx) for na, hx in re.findall(r'`([a-z_]+)` (#[0-9A-F]{8})', m.group(1))}


def hirogeru(mask, d):
    """True を チェビシェフ距離 d まで広げる（3×3 の最大を d 回）。"""
    m = mask.copy()
    for _ in range(d):
        p = np.pad(m, 1)
        n = m.copy()
        for dy in (0, 1, 2):
            for dx in (0, 1, 2):
                n |= p[dy:dy + m.shape[0], dx:dx + m.shape[1]]
        m = n
    return m


def renketsu_kazu(mask):
    """True の 8連結のまとまりの数。"""
    mask = mask.copy()
    h, w = mask.shape
    kazu = 0
    ys, xs = np.nonzero(mask)
    for y0, x0 in zip(ys, xs):
        if not mask[y0, x0]:
            continue
        kazu += 1
        stack = [(y0, x0)]
        mask[y0, x0] = False
        while stack:
            y, x = stack.pop()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < h and 0 <= nx < w and mask[ny, nx]:
                        mask[ny, nx] = False
                        stack.append((ny, nx))
    return kazu


def png_arr(p):
    return np.array(Image.open(p).convert('RGBA'))


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    flags = [a for a in sys.argv[1:] if a.startswith('--')]
    root = os.path.abspath(args[0]) if args else os.path.join(NE, 'clientmod', 'nouhin', 'v7')
    mono = None
    for f in flags:
        if f.startswith('--mono'):
            mono = set((f.split('=', 1)[1] if '=' in f else sys.argv[sys.argv.index(f) + 1]).split(','))
    nidome = '--nidome-nashi' not in flags

    print('=' * 62)
    print('特殊アイテムの絵（v7）を点検: %s' % root)
    print('=' * 62)

    if not os.path.isdir(root):
        check('納品の根がある', False, root)
        return owari()

    hyou = shouri_hyou()
    check('Shouri.java から 16件 読めた', len(hyou) == 16, len(hyou))
    kitai = [h for h in hyou if mono is None or h[1] in mono]
    if mono is not None:
        check('--mono の名前が Shouri.java にある', len(kitai) == len(mono), sorted(mono - {h[1] for h in hyou}))

    # ── manifest / DESIGN_SPEC ──
    mp = os.path.join(root, 'manifest.json')
    check('manifest.json がある', os.path.exists(mp))
    man = json.loads(yomu(mp)) if os.path.exists(mp) else {'files': [], 'items': []}
    sp = os.path.join(root, 'DESIGN_SPEC.md')
    if not os.path.exists(sp):
        sp = os.path.join(os.path.dirname(root), 'DESIGN_SPEC.md')
    check('DESIGN_SPEC.md がある（根か、その1つ上）', os.path.exists(sp), sp)
    spec = yomu(sp) if os.path.exists(sp) else ''
    ira_sha = hashlib.sha256(open(IRAIBUN, 'rb').read()).hexdigest().upper()
    check('DESIGN_SPEC に指示文の sha256 が書いてある', ira_sha in spec.upper(), ira_sha[:12])
    check('manifest の source_request_sha256 が指示文と一致',
          man.get('package', {}).get('source_request_sha256', '').upper() == ira_sha)

    pal = v6_paretto()
    check('v6 の共有パレットを読めた（52色）', len(pal) == 52, len(pal))
    atarashii = shinshoku(spec)
    yurusu = pal | set(atarashii.values())
    if atarashii:
        print('    断った新色: %s' % ', '.join('%s=%s' % (k, iro(v)) for k, v in atarashii.items()))

    # ── 1. 枚数とファイル名 ──
    e_dir = os.path.join(root, 'assets', 'jidaiui', 'textures', 'item')
    aru = sorted(os.listdir(e_dir)) if os.path.isdir(e_dir) else []
    kitai_mei = sorted(h[1] + '.png' for h in kitai)
    check('本番の絵が %d 枚あり、名前が Shouri.java と1つも違わない' % len(kitai_mei),
          aru == kitai_mei, '実物=%s' % aru)
    man_hon = {os.path.basename(f['file']): f for f in man['files'] if f.get('category') == 'item_texture'}
    check('manifest の item_texture が %d 件' % len(kitai_mei), sorted(man_hon) == kitai_mei, sorted(man_hon))

    # ── 13. manifest と実物（全 PNG） ──
    man_files = {f['file']: f for f in man['files']}
    jitsu = []
    for dp, _dn, fn in os.walk(root):
        for f in fn:
            if f.lower().endswith('.png') and not f.startswith('hikaku_'):
                jitsu.append(os.path.relpath(os.path.join(dp, f), root).replace(os.sep, '/'))
    check('manifest の files と実物の PNG が同じ集合（%d）' % len(jitsu),
          set(jitsu) == set(man_files), 'manifest だけ=%s 実物だけ=%s' % (
              sorted(set(man_files) - set(jitsu))[:4], sorted(set(jitsu) - set(man_files))[:4]))
    for rel, f in sorted(man_files.items()):
        p = os.path.join(root, rel)
        if not os.path.exists(p):
            check('%s がある' % rel, False)
            continue
        b = open(p, 'rb').read()
        arr = png_arr(p)
        h = hashlib.sha256(b).hexdigest().upper()
        check('%s: manifest の sha256（大文字）・byte 数・寸法が実物と一致' % rel,
              f['sha256'] == h and f['sha256'] == f['sha256'].upper() and f['byte_size'] == len(b)
              and list(f['dimensions']) == [arr.shape[1], arr.shape[0]],
              'sha=%s/%s size=%s/%s dim=%s/%s' % (f['sha256'][:8], h[:8], f['byte_size'], len(b),
                                                 f['dimensions'], [arr.shape[1], arr.shape[0]]))
        iro_k = np.unique(arr.reshape(-1, 4), axis=0)
        check('%s: manifest の rgba_color_count が実物と一致（%d）' % (rel, len(iro_k)),
              f.get('rgba_color_count') == len(iro_k), f.get('rgba_color_count'))

    # ── 1枚ごと ──
    for mei, fai, ban, ookisa in kitai:
        na = fai + '.png'
        p = os.path.join(e_dir, na)
        if not os.path.exists(p):
            check('%s がある' % na, False)
            continue
        im = png_arr(p)
        S = ookisa // 16
        # 2. 大きさ
        check('%s: %d×%d' % (na, ookisa, ookisa), im.shape[:2] == (ookisa, ookisa), im.shape[:2])
        if im.shape[:2] != (ookisa, ookisa):
            continue
        # 5. 6.
        a = im[:, :, 3]
        check('%s: アルファが 0 か 255 だけ' % na, bool(np.all((a == 0) | (a == 255))),
              sorted(int(v) for v in np.unique(a))[:6])
        check('%s: 透明画素の RGB が 0,0,0' % na, bool(np.all(im[a == 0][:, :3] == 0)))
        # 7. 8.
        iro_k = np.unique(im.reshape(-1, 4), axis=0)
        check('%s: 色数 %d（%d 以内）' % (na, len(iro_k), IRO_JOUGEN), len(iro_k) <= IRO_JOUGEN)
        nai = [iro(c) for c in iro_k if tuple(int(v) for v in c) not in yurusu]
        check('%s: 使った色がすべて v6 の共有パレット（＋断った新色）にある' % na, not nai, nai[:5])
        # 3. ① 下絵
        sp_ = os.path.join(root, 'shitae', na)
        if not os.path.exists(sp_):
            check('%s: 下絵 shitae/%s がある' % (na, na), False)
            continue
        shitae = png_arr(sp_)
        check('%s: 下絵が 16×16' % na, shitae.shape[:2] == (16, 16), shitae.shape[:2])
        if shitae.shape[:2] != (16, 16):
            continue
        chijimi = np.array(Image.open(p).convert('RGBA').resize((16, 16), Image.NEAREST))
        naka = im[S // 2::S, S // 2::S]
        chigai = int(np.sum(np.any(chijimi != shitae, axis=2)))
        check('★%s: ① nearest で 16×16 に縮めた物が下絵と1画素も違わない' % na, chigai == 0, '%d 画素 違う' % chigai)
        check('%s: 格子の中心の画素 ＝ 下絵（Pillow に依らない定義）' % na, bool(np.array_equal(naka, shitae)))
        # 4. ② 平均
        warui = []
        for cy in range(16):
            for cx in range(16):
                blk = im[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S].astype(np.int64)
                sa = shitae[cy, cx].astype(np.int64)
                al = blk[:, :, 3].mean()
                if abs(al - sa[3]) > ALPHA_SA:
                    warui.append('(%d,%d)a%.0f/%d' % (cx, cy, al, sa[3]))
                    continue
                if sa[3] == 255:
                    op = blk[blk[:, :, 3] == 255][:, :3]
                    d = np.abs(op.mean(axis=0) - sa[:3]) if len(op) else np.array([999])
                    if d.max() > RGB_SA:
                        warui.append('(%d,%d)rgb%.0f' % (cx, cy, d.max()))
        check('★%s: ② 各格子の平均色が下絵に近い（RGB %d 以内・アルファ %.0f%% 以内）' % (na, RGB_SA, 25),
              not warui, '%d 格子: %s' % (len(warui), warui[:5]))
        # 9. highlight
        hl = renketsu_kazu(np.all(im == np.array(hex_rgba(HIGHLIGHT)), axis=2))
        check('%s: highlight (%s) が1つの連結したまとまり' % (na, HIGHLIGHT), hl == 1, '%d 個' % hl)
        # 10. outline
        rin = np.all(im == np.array(hex_rgba(OUTLINE)), axis=2)
        toumei = a == 0
        toumei_hen = toumei.copy()
        toumei_hen[0, :] = toumei_hen[-1, :] = toumei_hen[:, 0] = toumei_hen[:, -1] = True
        chikai = hirogeru(toumei_hen, int(1.75 * S))
        oku = int(np.sum(rin & ~chikai))
        check('%s: 本番の outline 画素がすべて透明から %.2f 格子以内（素材面の内部に線が無い）' % (na, 1.75),
              oku == 0, '%d 画素 が奥にある' % oku)
        sh_rin = np.all(shitae == np.array(hex_rgba(OUTLINE)), axis=2)
        sh_tou = shitae[:, :, 3] == 0
        sh_tou_hen = np.pad(sh_tou, 1, constant_values=True)
        sh_tonari = np.zeros((16, 16), dtype=bool)
        for dy in (0, 1, 2):
            for dx in (0, 1, 2):
                if dy == 1 and dx == 1:
                    continue
                sh_tonari |= sh_tou_hen[dy:dy + 16, dx:dx + 16]
        oku2 = int(np.sum(sh_rin & ~sh_tonari))
        check('%s: 下絵の outline が透明に 8近傍で接する格子だけ' % na, oku2 == 0, '%d 格子' % oku2)
        # 11. 余白
        ys, xs = np.nonzero(~toumei)
        yohaku = (int(xs.min()), int(ys.min()), int(ookisa - 1 - xs.max()), int(ookisa - 1 - ys.max()))
        lo, hi = YOHAKU[ookisa]
        check('%s: 外周の透明余白 %s が %d〜%d px' % (na, yohaku, lo, hi), all(lo <= v <= hi for v in yohaku))
        # 12. プレビュー
        pv = os.path.join(root, 'preview', na)
        if os.path.exists(pv):
            pva = png_arr(pv)
            check('★%s: プレビュー ＝ 本番の nearest 1/2 と1画素も違わない' % na,
                  pva.shape == im[1::2, 1::2].shape and bool(np.array_equal(pva, im[1::2, 1::2])),
                  pva.shape[:2])
        else:
            check('%s: preview/%s がある' % (na, na), False)
        pvs = os.path.join(root, 'preview_shitae', na)
        if os.path.exists(pvs):
            pvsa = png_arr(pvs)
            kitai8 = np.repeat(np.repeat(shitae, 8, axis=0), 8, axis=1)
            check('%s: preview_shitae ＝ 下絵の nearest 8倍と1画素も違わない' % na,
                  pvsa.shape == kitai8.shape and bool(np.array_equal(pvsa, kitai8)), pvsa.shape[:2])
        else:
            check('%s: preview_shitae/%s がある' % (na, na), False)

    # 別々の絵か
    if len(kitai) >= 2:
        arrs = [png_arr(os.path.join(e_dir, h[1] + '.png')).tobytes() for h in kitai
                if os.path.exists(os.path.join(e_dir, h[1] + '.png'))]
        check('本番の絵がすべて別々', len(set(arrs)) == len(arrs))

    # ── 14. 2回動かして byte が一致 ──
    if nidome:
        an_mode = os.path.basename(os.path.dirname(root)) == 'an'
        tmp = tempfile.mkdtemp(prefix='tokushu_e_')
        try:
            outs = []
            for k in (1, 2):
                o = os.path.join(tmp, str(k))
                cmd = [sys.executable, TSUKURU, '--out', o]
                if an_mode:
                    cmd.append('--an')
                r = subprocess.run(cmd, capture_output=True)
                check('生成 %d 回目が通る' % k, r.returncode == 0, r.stderr.decode('utf-8', 'replace')[-300:])
                outs.append(o)
            chigau = []
            kazu = 0
            for dp, _dn, fn in os.walk(outs[0]):
                for f in fn:
                    p1 = os.path.join(dp, f)
                    p2 = os.path.join(outs[1], os.path.relpath(p1, outs[0]))
                    kazu += 1
                    if not os.path.exists(p2) or open(p1, 'rb').read() != open(p2, 'rb').read():
                        chigau.append(os.path.relpath(p1, outs[0]))
            check('★2回動かして %d ファイルの byte が全部 一致' % kazu, kazu > 0 and not chigau, chigau[:5])
            # 納品と同じか（納品が手で触られていないか）
            sub = os.path.relpath(root, os.path.join(NE, 'clientmod', 'nouhin', 'v7'))
            if os.path.basename(root) in ('A', 'B', 'C') or sub == '.':
                moto = os.path.join(outs[0], sub) if sub != '.' else outs[0]
                chigau2 = []
                for rel in sorted(man_files):
                    p1 = os.path.join(root, rel)
                    p2 = os.path.join(moto, rel)
                    if not os.path.exists(p2) or open(p1, 'rb').read() != open(p2, 'rb').read():
                        chigau2.append(rel)
                check('納品の PNG が、いま吐き直した物と byte まで同じ（手で触っていない）', not chigau2, chigau2[:5])
        finally:
            shutil.rmtree(tmp, ignore_errors=True)

    return owari()


def owari():
    print()
    print('=' * 60)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    return 1 if fail_ else 0


if __name__ == '__main__':
    sys.exit(main())
