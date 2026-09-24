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

# ★ 2026-09-20 r2 が最新の納品（50 PASS / 0 FAIL）。
#   前は 2026-08-23 の古い納品を指していて、引数を忘れると
#   **古い PDF を測って大量に赤が出る**（測り間違いだと気づきにくい）。
KITEI = os.path.join(os.path.expanduser('~'), 'Documents', 'ChatGPT', 'New project',
                     'output', 'pdf', 'jidaicraft_20260920_r2', '時代クラフト_ルールブック')

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


def tsuzuku(bun, moji, tomo, haba=22):
    """手がかりの語 tomo の【すぐ後ろ haba 文字】に moji が来るか。

    ★★ なぜ向きまで決めるか ★★
      最初は「前後 45 文字の中にあれば OK」にしていたが、
      **隣の文の語に当たって素通りした**（掘りの表の「ダイヤ 1%」が、
      1つ前の文の「略奪」を手がかりに緑になった。わざと壊して発覚）。
      日本語は「銀行を100回」「貯金と石油を1%」のように
      **手がかりの語 → 数字**の順に並ぶので、後ろだけを見る。
    """
    for t in tomo:
        for m in re.finditer(re.escape(t), bun):
            i = m.end()
            if moji in bun[i:i + haba]:
                return True
    return False


def gacha_gokei(jidai):
    """ガチャの「時代ごとの合計」を実装から数える。

    ★ 数え方は tests/shiryou_gacha.py の kazoeru() と同じにしてある。
      別々に数えると、資料と PDF で違う答えが出る（正本が2つになる）。
    """
    kei = keihin()          # (名前, 重み, 時代, 等級)
    g = yomu(os.path.join(SRC, 'Gacha.java'))
    kane_hyou = {(na, int(ji)): int(kn) for na, _m, _k, kn, _om, ji, _t in re.findall(
        r'new Keihin\("([^"]+)",\s*Material\.(\w+),\s*(\d+),\s*(\d+),\s*(\d+),'
        r'\s*(\d+),\s*(\w+)\)', g)}
    sh = yomu(os.path.join(SRC, 'Shouri.java'))
    mei = set()
    for kagi in ('TOKUSHU_HYOU', 'IBUTSU_HYOU'):
        m = re.search(kagi + r'\s*=\s*\{(.*?)\n    \};', sh, re.S)
        if m:
            mei |= set(re.findall(r'\{"([^"]+)"', m.group(1)))
    # ★ NEDAN は {0, 5, 10, 20, 50}。**先頭の 0 は詰め物**で、添字が時代そのもの。
    #   ここを 0 から数えると値段が1つずれる（実際ずらして 0除算で落ちた）。
    m = re.search(r'NEDAN\s*=\s*\{([^}]+)\}', g)
    atai = [int(x.strip()) for x in m.group(1).split(',')]
    ne = {j: atai[j] for j in (1, 2, 3, 4)}
    gyou = [(na, om, tou) for na, om, ji, tou in kei if ji == jidai]
    zen = sum(om for _n, om, _t in gyou)
    if not zen:
        return {'nashi': True}
    hazu = sum(om for _n, om, t in gyou if t == 'HAZURE')
    kane = sum(om for na, om, t in gyou
               if t != 'HAZURE' and kane_hyou.get((na, jidai), 0) > 0)
    dai = sum(om for na, om, t in gyou
              if t == 'DAI' and kane_hyou.get((na, jidai), 0) > 0)
    toku = sum(om for na, om, t in gyou
               if t != 'HAZURE' and kane_hyou.get((na, jidai), 0) == 0 and na in mei)
    kitai = sum(kane_hyou.get((na, jidai), 0) * om for na, om, _t in gyou) / float(zen)
    return {'nashi': False, 'ne': ne[jidai], 'kazu': len(gyou),
            'hazu': hazu / 100.0, 'futsu': (zen - hazu - kane - toku) / 100.0,
            'kane': kane / 100.0, 'dai': dai / 100.0, 'toku': toku / 100.0,
            'kitai': kitai}


def iraibun_page(kumi):
    """依頼文A/B が申告しているページ数を読む。★ 手で写さない。"""
    fai = {'A': 'Codex依頼文_説明書A_遊び方.md',
           'B': 'Codex依頼文_説明書B_詳細版.md'}[kumi]
    t = yomu(os.path.join(NE, 'docs', fai))
    m = re.search(r'\*\*(\d+)ページ\*\*', t)
    assert m, '%s にページ数が無い' % fai
    return int(m.group(1))


def teisuu(fai, mei):
    """プラグインの定数を1つ読む。無ければ落とす（黙って0にしない）。"""
    m = re.search(r'%s\s*=\s*(\d+)' % mei, yomu(os.path.join(SRC, fai)))
    assert m, '%s に %s が無い' % (fai, mei)
    return int(m.group(1))


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
    # ★★ 2026-09-20: 数を写すのをやめた ★★
    #   ここに 13 と書いてあり、送り状には 22 と書いてあった。
    #   **どちらが正本か分からない状態**だったので、依頼文から読む。
    KITAI = {'遊び方.pdf': iraibun_page('A'), '詳細版.pdf': iraibun_page('B')}

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
            # ★★ ここは一度 間違っていた（2026-09-20）★★
            #   前は「奪われて」で探していたが、その字は【遺物の一文】
            #   「略奪で奪われても効果は残ります」にしか無かった。
            #   その一文は監査 #06 で誤りと分かって消した物なので、
            #   **この検査はずっと間違った文で緑になっていた**。
            #   案D の中身そのもの（誰から・何枚）で見る。下の chikaku で。
            ('未来', '時代5「未来」'),
            ('未来到達勝利', '未来へ進めたら勝ち'),
    ):
        check('PDF に「%s」がある（%s）' % (moji, setsumei),
              moji.replace(' ', '') in zenbu, '無い＝差し替えが要る')

    # ── 案D: 集める5種は略奪で奪われる ─────────────────
    #   ★ 「誰から」「何を」で見る。語尾は見ない。
    check('★★PDF に案D（リーダーから特殊アイテムを奪う）がある',
          tsuzuku(zenbu, '奪え', ('相手のリーダーから', 'リーダーから'), haba=20),
          '無い＝案D が入っていない')

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
    # ★ 昔の書き方（依頼文Bが自分で表を持っていた頃の形）
    check('★★PDF に古い「4.0%」の行（近代）が残っていない',
          '22.0%51.3%22.7%4.0%' not in zenbu, '残っている＝案C 前の表')

    # ★★ 書き方ではなく【行そのもの】を突き合わせる（2026-09-20）★★
    #   上の1行だけでは、資料の書式（22.00 / 51.30 / …）で刷られた
    #   古い表を素通りさせた。実際 それで「近代 23件・特殊4.00%」が
    #   0 FAIL のまま刷られた（Codex の監査が見つけた）。
    #   実装から行を組み立てて、そのまま入っているかを見る。
    for j, mei in ((1, '鉄器'), (2, '中世'), (3, '近代'), (4, '現代')):
        a = gacha_gokei(j)
        gyou = ('%s(%d)%d%d%.2f%.2f%.2f%.2f%.2f%.2f%.2f%%'
                % (mei, j, a['ne'], a['kazu'], a['hazu'], a['futsu'], a['kane'],
                   a['dai'], a['toku'], a['kitai'], a['kitai'] / a['ne'] * 100))
        check('★★PDF のガチャの表 %s の行が実装と一致' % mei,
              gyou in zenbu or a['nashi'],
              '実装=%s（資料 §5 を作り直して送り直す）' % gyou)

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

    # ── 5) 戦争の作り直し（2026-09-09）────────────────
    #   ★★ ここが長らく空だった ★★
    #     戦争を作り直したのに、PDF 側の見張りを足していなかった。
    #     「0 FAIL なのに古い戦争が刷られている」が起きる形だった。
    print()
    print('-- 5) 戦争の作り直し（1回1% ／ 100回でビーコン）--')
    #   ★ 割合は「割る数」から出す。100 と書くと、設定を変えた時に取り残される。
    wari = 100 // settei('略奪_割る数')
    kai = settei('占領_必要回数')
    check('★★PDF に「%d%%」の略奪がある（掘りの表ではなく略奪の所で）' % wari,
          tsuzuku(zenbu, '%d%%' % wari, ('貯金と石油', '相手の貯金', '略奪で')),
          '無い＝古い「1回いくら」のまま')
    check('★★PDF にビーコンの「%d回」がある（銀行を壊す回数として）' % kai,
          tsuzuku(zenbu, '%d回' % kai, ('銀行', 'ビーコン')),
          '無い＝古い「貯金が尽きたら」のまま')
    for furui, riyuu in (('100円以下', '古いビーコン条件（貯金が尽きたら壊せる）'),
                         ('300円', '廃止した略奪の上限'),
                         ('石油2本', '廃止した固定の奪取量')):
        check('★★PDF に古い「%s」が残っていない' % furui,
              furui not in zenbu, riyuu)

    # ── 6) 銀行の守り（2026-09-18）──────────────────
    #   ★ 参加者が必ずぶつかる。説明書に無いと当日の問い合わせになる。
    print()
    print('-- 6) 銀行の守り（3秒で戻る ／ まわりは掘れない）--')
    modoru = teisuu('JidaiCraft.java', 'GINKO_MODORU') // 20     # 20tick = 1秒
    mamoru = teisuu('JidaiCraft.java', 'GINKO_MAMORU')
    check('PDF に「%d秒で」戻ると書いてある（銀行の話として）' % modoru,
          tsuzuku(zenbu, '%d秒で' % modoru, ('銀行', '金ブロック')),
          '無い＝壊れっぱなしだと誤解される')
    check('PDF に「%dマス」の守りが書いてある（銀行の話として）' % mamoru,
          tsuzuku(zenbu, '%dマス' % mamoru, ('銀行', '金ブロック', 'まわり')),
          '無い')

    # ── 6b) r2 で入れた補足のうち、実装から出る数字 ──────
    #   ★ 2026-09-20 の監査で「分からない」と指摘され、
    #     実装を確かめて本文に入れた物。消えたら気づけるようにする。
    print()
    print('-- 6b) 補足（実装から出る数字）--')
    #   ★ 守る範囲は 7×7×7。半径から出す（3 → 2*3+1 = 7）。
    hen = mamoru * 2 + 1
    check('PDF に守りの立方体 %d×%d×%d がある' % (hen, hen, hen),
          ('%d×%d×%d' % (hen, hen, hen)) in zenbu,
          '無い＝上下も守られることが伝わらない')
    #   ★ 1人が次に壊せるまでの秒数。
    kan = settei('略奪_間隔秒')
    check('PDF に「%d秒」の間隔がある（次に壊せるまで）' % kan,
          tsuzuku(zenbu, '%d秒' % kan, ('次に壊せるのは', '壊せるのは', '同じ人が')),
          '無い＝人数が速さになることが伝わらない')
    #   ★ 「押す」が右クリックだと書いてあるか。実装は RIGHT_CLICK_BLOCK。
    jc = yomu(os.path.join(SRC, 'JidaiCraft.java'))
    check('実装: 押す＝右クリック（Action.RIGHT_CLICK_BLOCK）',
          'RIGHT_CLICK_BLOCK' in jc, '実装が変わった。文書も見直すこと')
    check('PDF に「押す＝右クリック」がある',
          '押す＝右クリック' in zenbu, '無い＝左クリックと取り違える')

    # ── 7) 止めた仕組みが消えているか ────────────────
    #   ★ 「削除してください」と頼んだのに、消えたかを誰も測っていなかった。
    print()
    print('-- 7) 止めた仕組みが残っていないか --')
    for furui in ('傭兵', '蛮族', '人望勝利', 'Uキー', '/hire', '/headhunt'):
        check('PDF に「%s」が残っていない' % furui,
              furui not in zenbu, '2026-08-26 に仕組みごと止めた')

    print('')
    print('=' * 62)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    print('★ 絵・レイアウト・読みやすさは機械では見られない。人の目で見ること。')
    return 1 if fail_ else 0


sys.exit(main())
