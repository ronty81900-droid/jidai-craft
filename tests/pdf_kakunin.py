# -*- coding: utf-8 -*-
"""納品された説明書の PDF が、実装と合っているかを機械で突き合わせる。

  ★★ なぜ要るか（2026-08-23）★★
    説明書A・B は Codex へ発注済みで、**発注後に仕様が2回 変わった**
    （案C＝5種は現代限定 / 案D＝略奪で奪う / 時代5「未来」）。
    PDF は 遊び方12ページ・詳細版24ページ。目で読み比べると必ず取りこぼす。
    実装から数字を読み、PDF の文字と突き合わせる。

  ★ PDF の文字は「文字」として埋め込まれている前提（依頼文で指定済み）。
    絵に焼き込まれていたら、この道具は何も読めないので、そう言う。

  使い方:
    python tests/pdf_kakunin.py <PDFの入ったフォルダ>
    （省略すると Documents/Codex/.../時代クラフト_ルールブック を見る）
"""
import io
import os
import re
import sys

NE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DP = os.path.join(NE, 'datapacks', 'jidai_craft', 'data', 'jidai', 'functions')
SRC = os.path.join(NE, 'plugin', 'src', 'main', 'java', 'jidai')

KITEI = os.path.join(os.path.expanduser('~'), 'Documents', 'Codex', '2026-08-23',
                     '2-markdown-2-jidaicraft-manual-a-2', 'outputs', '時代クラフト_ルールブック')

pass_ = 0
fail_ = 0
chui = []


def check(namae, jouken, shousai=''):
    global pass_, fail_
    if jouken:
        pass_ += 1
        print('[PASS] ' + namae)
    else:
        fail_ += 1
        print('[FAIL] ' + namae + '  ' + str(shousai))


def yomu(p):
    return io.open(p, encoding='utf-8').read()


def pdf_moji(p):
    """PDF の文字を1本につなげて返す。改行と空白は畳む。"""
    import fitz
    d = fitz.open(p)
    de = []
    for page in d:
        de.append(page.get_text())
    d.close()
    # 折り返しで語が割れるので、空白と改行を全部 落とした版も作る
    nama = '\n'.join(de)
    kara = sum(1 for t in de if not t.strip())
    return nama, re.sub(r'\s+', '', nama), len(de), kara


def settei(kagi):
    load = yomu(os.path.join(DP, 'load.mcfunction'))
    m = re.search(r'scoreboard players set %s settei (-?\d+)' % re.escape(kagi), load)
    return int(m.group(1)) if m else None


def keihin():
    """Gacha.KEIHIN を (名前, 重み, 時代, 等級) で読む。"""
    g = yomu(os.path.join(SRC, 'Gacha.java'))
    return [(na, int(om), int(ji), tou) for na, _m, _k, _kane, om, ji, tou in re.findall(
        r'new Keihin\("([^"]+)",\s*Material\.(\w+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\w+)\)', g)]


def atsumeru5():
    sh = yomu(os.path.join(SRC, 'Shouri.java'))
    m = re.search(r'TOKUSHU_HYOU\s*=\s*\{(.*?)\n    \};', sh, re.S)
    return re.findall(r'\{"([^"]+)"', m.group(1))


def main():
    ne = sys.argv[1] if len(sys.argv) > 1 else KITEI
    if not os.path.isdir(ne):
        print('PDF のフォルダが無い: ' + ne)
        return 1

    # 依頼文が決めたページ数（docs/Codex依頼文_説明書A / B）
    #   A「遊び方」… ほぼ全ページ イラスト・12ページ
    #   B「詳細版」… イラスト半々・24ページ
    #   ★ 文字量を一律の下限で見てはいけない。A は絵の本なので、
    #     950字でも正しい（一度これで誤って落とした）。
    # ★ 2026-08-26: 詳細版は傭兵の章を落として 24 → 22 ページ。
    #   遊び方は 12 のまま（傭兵の1ページが未来の説明に置き換わった）。
    # ★ 2026-08-26(v9): 詳細版は章の分割をやめて 22 → 13 ページ。
    KITAI = {'遊び方.pdf': 12, '詳細版.pdf': 13}

    hon = {}
    for f in sorted(os.listdir(ne)):
        if f.lower().endswith('.pdf'):
            nama, tsume, pages, kara = pdf_moji(os.path.join(ne, f))
            hon[f] = (nama, tsume)
            print('読んだ: %-16s %2dページ / 文字 %d（1ページ %d字）'
                  % (f, pages, len(nama), len(nama) // max(1, pages)))
            if f in KITAI:
                check('%s のページ数が依頼どおり（%dページ）' % (f, KITAI[f]),
                      pages == KITAI[f], '実際=%dページ' % pages)
            check('%s: 文字が1つも無いページが無い（絵に焼かれていない）' % f,
                  kara == 0, '%dページが空' % kara)
    check('PDF を2冊 読めた', len(hon) >= 2, '実際=%d 冊' % len(hon))
    if not hon:
        return 1
    print()

    zenbu = ''.join(t for _n, t in hon.values())

    # ── 1) 集める5種が出る時代（案C）──────────────────
    print('-- 1) 集める5種の出る時代（案C: 歯車と月の石は現代だけ）--')
    kei = keihin()
    for mei in atsumeru5():
        deru = sorted({ji for na, _om, ji, _t in kei if na == mei})
        jidai_mei = {1: '鉄器', 2: '中世', 3: '近代', 4: '現代'}
        if mei in ('蒸気機関の歯車', '月の石'):
            check('★★「%s」は実装では現代だけ' % mei, deru == [4], '実装=%s' % deru)
            # PDF が「近代から」と言っていないか
            warui = mei.replace(' ', '') + '近代から'
            check('★★PDF が「%s」を『近代から』と書いていない' % mei,
                  warui not in zenbu, '書いている＝現代限定になった仕様と食い違う')
        else:
            check('「%s」の出る時代 %s' % (mei, [jidai_mei[d] for d in deru]), len(deru) > 0, '出ない')

    # ── 2) 発注後に変わった文言 ──────────────────────
    print()
    print('-- 2) 発注後に変わった所が、PDF に入っているか --')
    for moji, setsumei in (
            ('現代のガチャにしか出ません', '案C: 残り2種は現代限定'),
            # ★ 2026-08-26: 「およそ300回」を外した。
            #   今の仕様書（docs/Codex依頼文_説明書B_詳細版.md）は
            #   ガチャの回数そのものを載せていない。無い物を探していた。
            # ★ 「奪われます」→「奪われて」。PDF は
            #   「略奪で奪われても効果は残ります」と書いてある。
            #   語尾まで決め打ちすると、正しい文でも落ちる。
            ('奪われて', '案D: 略奪で紙を奪われる'),
            ('未来', '時代5「未来」'),
            ('未来到達勝利', '未来へ進めたら勝ち'),
    ):
        check('PDF に「%s」がある（%s）' % (moji, setsumei),
              moji.replace(' ', '') in zenbu, '無い＝差し替えが要る')

    check('PDF に古い「およそ450回」が残っていない',
          'およそ450回' not in zenbu and '450回' not in zenbu, '残っている')
    check('PDF に古い「近代に入ると、5種すべてが出る」が残っていない',
          '近代に入ると、5種すべてが出る'.replace(' ', '') not in zenbu, '残っている')

    # ── 3) ガチャの割合（近代の行）──────────────────
    print()
    print('-- 3) ガチャの割合（近代は 案C で変わった）--')
    for j, mei in ((1, '鉄器'), (2, '中世'), (3, '近代'), (4, '現代')):
        gyou = [(na, om, tou) for na, om, ji, tou in kei if ji == j]
        zen = sum(om for _n, om, _t in gyou)
        hazu = sum(om for _n, om, t in gyou if t == 'HAZURE')
        toku = sum(om for na, om, t in gyou
                   if t != 'HAZURE' and (na in atsumeru5() or '遺物' in na))
        check('時代%d の重みの合計が 10000' % j, zen == 10000, zen)
        # 特殊の割合（遺物を含む）は Gacha 側で数え直す
    # 近代の特殊は 3.0%（案C 後）
    kindai_toku = sum(om for na, om, ji, t in kei
                      if ji == 3 and t != 'HAZURE' and om == 50)
    check('★近代の特殊の重みは 300（＝3.0%%）（実測 %d）' % kindai_toku,
          kindai_toku == 300, '実装=%d' % kindai_toku)
    check('★★PDF に古い「4.0%」の行（近代）が残っていない',
          '22.0%51.3%22.7%4.0%' not in zenbu, '残っている＝案C 前の表')

    # ── 4) 時代と勝ち方 ────────────────────────────
    print()
    print('-- 4) 時代と勝ち方 --')
    check('★★PDF が「現代へ到達する」を勝ち方に挙げていない',
          '現代へ到達する' not in zenbu, '挙げている＝現代到達は勝ちではない')
    # ★ 数字だけで見ない。500 は 500,000 や「石油 500」にも当たって素通りする
    #   （実際に一度 素通りした）。**表の行**として見る。
    #
    # ★★ 2026-08-26: 「現代→未来」という字を探すのをやめた ★★
    #   仕様書では表の【列】なので、PDF から取り出すと
    #   「現代 未来 500 本 0 円 2,400 個」のように**空白で並ぶ**。
    #   矢印は文字として入らない。正しい納品を落としてしまった。
    #   → 「現代 と 未来 が並び、そのあとに実装の3つの数字が来る行」で見る。
    sekiyu = settei('進行_石油_現代')
    chokin = settei('進行_貯金_現代')
    kenchiku = settei('進行_建築_現代')
    gyou = [g for g in zenbu.split('\n')
            if '現代' in g and '未来' in g
            and str(sekiyu) in g and '{:,}'.format(kenchiku) in g]
    check('★★PDF に「現代 → 未来」の行がある',
          len(gyou) >= 1, '無い＝時代5がまるごと入っていない')
    if gyou:
        moji = gyou[0]
        for v, yobi in ((sekiyu, '未来に要る石油'),
                        (chokin, '未来に要る貯金'),
                        (kenchiku, '未来に要る建材')):
            check('PDF の「現代 → 未来」の行に %s %s がある' % (yobi, '{:,}'.format(v)),
                  '{:,}'.format(v) in moji or str(v) in moji,
                  '行=%s' % moji.strip()[:60])

    print('')
    print('=' * 62)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    print('★ 絵・レイアウト・読みやすさは機械では見られない。人の目で見ること。')
    return 1 if fail_ else 0


sys.exit(main())
