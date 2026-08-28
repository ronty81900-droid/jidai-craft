# -*- coding: utf-8 -*-
"""説明書3本に書いた数字が、実装と合っているかを突き合わせる。

  ★ 説明書は「実装から読んだ実測値」と名乗っている。名乗る以上、機械で確かめる。
  走らせ方: python bunsho_kakunin.py
"""
import io
import os
import re
import sys

# ★ 置き場は【自分の場所から数える】。ユーザー名を書かない。
#   このファイルは 置き場/tests/ にあるので 2つ上。
NE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(NE, 'docs')
SRC = os.path.join(NE, 'plugin', 'src', 'main', 'java', 'jidai')
DP = os.path.join(NE, 'datapacks', 'jidai_craft', 'data', 'jidai', 'functions')

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


def yomu(p):
    return io.open(p, encoding='utf-8').read()


LOAD = yomu(os.path.join(DP, 'load.mcfunction'))
GACHA = yomu(os.path.join(SRC, 'Gacha.java'))
SHOP = yomu(os.path.join(SRC, 'Shop.java'))
GINKO = yomu(os.path.join(SRC, 'Ginko.java'))
SHOURI = yomu(os.path.join(SRC, 'Shouri.java'))
IBUTSU = yomu(os.path.join(SRC, 'Ibutsu.java'))
KANRI = yomu(os.path.join(SRC, 'Kanri.java'))
JC = yomu(os.path.join(SRC, 'JidaiCraft.java'))

A = yomu(os.path.join(DOCS, 'Codex依頼文_説明書A_遊び方.md'))
B = yomu(os.path.join(DOCS, 'Codex依頼文_説明書B_詳細版.md'))
G = yomu(os.path.join(DOCS, 'ゲームマスター説明書.md'))
T = yomu(os.path.join(DOCS, 'クリア想定時間.md'))
ZEN = A + B + G + T


def settei(mei):
    m = re.search(r'scoreboard players set %s settei (-?\d+)' % re.escape(mei), LOAD)
    return int(m.group(1)) if m else None


print('=' * 62)
print('説明書に書いた数字を、実装と突き合わせる')
print('=' * 62)

# ── 1) settei の値 ──
for mei, kitai in (('進行_石油_鉄器', 100), ('進行_石油_中世', 250), ('進行_石油_近代', 500),
                   ('進行_貯金_鉄器', 5000), ('進行_貯金_中世', 15000), ('進行_貯金_近代', 50000),
                   ('進行_建築_鉄器', 200), ('進行_建築_中世', 400), ('進行_建築_近代', 800),
                   ('石油_上限_鉄器', 750), ('石油_上限_中世', 1800), ('石油_上限_近代', 3350),
                   ('石油_間隔_鉄器', 10), ('リセット_徴収率', 40), ('先行_ロス率', 50),
                   ('腐敗_猶予秒', 900), ('腐敗_減少率', 10),
                   ('戦争_準備秒', 300), ('戦争_交戦秒', 600), ('戦争_禁止秒', 1800),
                   ('略奪_金', 30), ('略奪_石油', 2), ('略奪_上限金', 300), ('略奪_上限石油', 10),
                   ('下剋上_値段', 150), ('経済_勝利_貯金', 500000)):
    check('settei %s = %s' % (mei, kitai), settei(mei) == kitai, '実装=%s' % settei(mei))

# ── 2) プラグインの定数 ──
for src, mei, kitai, na in ((SHOP, 'JUKI_KAIHOU', 10000, '銃器専門店の解放'),
                            (IBUTSU, 'JUKI_SHIKII_TEGATA', 5500, '手形の閾値'),
                            (JC, 'BEACON_KOWASERU', 100, 'ビーコンを壊せる貯金'),
                            (JC, 'SEIATSU_HANABI', 5, '制圧の花火'),
                            (KANRI, 'BEACON_USHIRO', 16, 'ビーコンの後方')):
    m = re.search(r'%s\s*=\s*(\d+)' % mei, src)
    check('%s（%s）= %d' % (na, mei, kitai), m and int(m.group(1)) == kitai,
          '実装=%s' % (m.group(1) if m else 'なし'))

m = re.search(r'HAJIME_NO_RISU\s*=\s*"([^"]+)"', KANRI)
check('初期リスポーンの既定 = 40 176 -927', m and m.group(1) == '40 176 -927',
      '実装=%s' % (m.group(1) if m else 'なし'))

# ── 3) ガチャの値段と割合 ──
m = re.search(r'NEDAN\s*=\s*\{([^}]+)\}', GACHA)
nedan = [int(x.strip()) for x in m.group(1).split(',')]
check('ガチャの値段 = 0,5,10,20,50', nedan == [0, 5, 10, 20, 50], nedan)

# 景品表から時代ごとの割合を計算し、説明書の表と突き合わせる
kei = re.findall(r'new Keihin\("([^"]+)",\s*Material\.(\w+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\w+)\)', GACHA)
# ★ 件数は焼き込まない（表を1行 直すたびに、意味のない FAIL が出るため）。
#   読み落としは、下の【時代ごとの重みの合計が 10000】が捕まえる。
#   ここでは「4時代ぶん 読めたか」だけを見る。
_ji = set(int(r[5]) for r in kei)
check('景品表を読めた（4時代ぶん・%d 件）' % len(kei), _ji == {1, 2, 3, 4},
      '実際=%d 件 / 時代=%s' % (len(kei), sorted(_ji)))
tokushu_mei = set(re.findall(r'\{"([^"]+)",\s*"[^"]+",\s*"\d+"', SHOURI)) | \
              set(re.findall(r'\{"([^"]+)",\s*"[^"]+",\s*"\d+",\s*"\d"', SHOURI))
# ★ 割合の期待値はここに書かない（第3の正本になってしまう）。
#   説明書の本文の表と実装を直に突き合わせる（下の「本文 vs 実装」）。
for j in (1, 2, 3, 4):
    hazu = kane = toku = zen = 0
    for na, _mat, _kazu, k, om, jd, tou in kei:
        if int(jd) != j:
            continue
        om = int(om)
        zen += om
        if tou == 'HAZURE':
            hazu += om
        elif int(k) > 0:
            kane += om
        elif na in tokushu_mei:
            toku += om
    futsu = zen - hazu - kane - toku
    check('時代%d の重みの合計が 10000' % j, zen == 10000, zen)

# ── 4) 貴金属の売値 ──
m = re.search(r'NEDAN\s*=\s*\{([^}]+)\}', GINKO)
check('貴金属の売値 = 1,5,10,15,30',
      [int(x.strip()) for x in m.group(1).split(',')] == [1, 5, 10, 15, 30], m.group(1))

# ── 5) 掘りの確率 ──
horu = yomu(os.path.join(NE, 'tests', 'horu_tsukuru.py'))
for mono, kitai in (('iron_ingot', 0.15), ('lapis_lazuli', 0.08),
                    ('gold_ingot', 0.04), ('diamond', 0.01)):
    m = re.search(r"'minecraft:%s',\s*([\d.]+)" % mono, horu)
    check('掘り %s = %s' % (mono, kitai), m and float(m.group(1)) == kitai,
          '実装=%s' % (m.group(1) if m else 'なし'))
m = re.search(r'SHINSO_BAI\s*=\s*([\d.]+)', horu)
check('深層岩の倍率 = 1.5', m and float(m.group(1)) == 1.5, m.group(1) if m else 'なし')

# ── 6) 承認のコマンドが実在するか ──
for kuni in ('kyuryo', 'shinrin', 'kawa', 'naikai', 'iwaba'):
    p = os.path.join(DP, 'shinko', 'shounin_%s.mcfunction' % kuni)
    check('承認 jidai:shinko/shounin_%s がある' % kuni, os.path.exists(p), p)
    check('GM 説明書が shounin_%s を載せている' % kuni, 'shounin_%s' % kuni in G)

# ── 7) 遺物11種の名前と効果が、説明書A/B/GM と一致するか ──
ib = re.findall(r'\{"([^"]+)",\s*"[^"]+",\s*"(\d+)",\s*"(\d)",\s*"[A-Z]+",\s*"([^"]*)"\}', SHOURI)
check('遺物を 11 種 読めた', len(ib) == 11, len(ib))
for mei, ban, _jd, setsu in ib:
    check('説明書B に遺物「%s」がある' % mei, mei in B)
    check('GM 説明書に遺物「%s」と番号 %s がある' % (mei, ban), mei in G and ban in G)

# ── 8) 集める5種 ──
tok = re.findall(r'\{"([^"]+)",\s*"[^"]+",\s*"(\d+)"\}', SHOURI)
check('集める5種を読めた', len(tok) == 5, len(tok))
for mei, ban in tok:
    check('説明書B に「%s」がある' % mei, mei in B)
    check('GM 説明書に「%s」と番号 %s がある' % (mei, ban), mei in G and ban in G)

# ── 9) 説明書のページ数の宣言と、中身の見出しの数 ──
# ★ 2026-08-26 に書き方を変えた。B は「4〜5ページ目」のようにまとめる節があるので、
#   節の数ではなく【宣言したページ数】と【最後の節が指すページ】を見る。
# ★ 2026-08-26(v9): 詳細版は章の分割をやめて 22 → 13 ページ
for na, bun, kazu in (('A', A, 12), ('B', B, 13)):
    check('説明書%s の依頼文が %dページと宣言している' % (na, kazu),
          ('**%dページ**' % kazu) in bun, '無い')
    saigo = re.findall(r'^### (?:\d+〜)?(\d+)ページ目', bun, re.M)
    check('説明書%s の最後の節が %dページ目' % (na, kazu),
          saigo and int(saigo[-1]) == kazu, '実際=%s' % (saigo[-1] if saigo else 'なし'))


# =========================================================
#  ★★ ここからが本丸: 説明書の【本文の数字】を実装と突き合わせる ★★
# =========================================================
#   上の検査は「実装 vs この検査に書いた期待値」なので、
#   説明書の本文がずれても気づけない。本文から数字を抜いて比べる。

def hyou_gyou(bun, midashi, retsu):
    """markdown の表から、見出しの直後の表の行を (列, ...) の並びで返す。"""
    i = bun.index(midashi)
    gyou = []
    for g in bun[i:].splitlines():
        if g.startswith('|') and not set(g) <= set('|-: '):
            bu = [x.strip() for x in g.strip('|').split('|')]
            if len(bu) == retsu:
                gyou.append(bu)
        elif gyou and not g.startswith('|'):
            break
    return gyou[1:]          # 見出し行を落とす


def suji(s):
    """表の升目から数字だけを取り出す（「5分」「1,800本」「約31分」などに対応）。"""
    m = re.search(r'-?[\d,]+', s)
    if not m:
        raise ValueError('数字が無い: ' + s)
    return int(m.group(0).replace(',', ''))


print()
print('-- 説明書Bの本文 vs 実装 --')

# ★★ 2026-08-26: 依頼文の表の照合は tests/iraibun_kakunin.py へ移した ★★
#   依頼文を書き直した時に、表の見出しが変わってここが落ちた。
#   向こうは【表を行ごとに読む】ので、こちらより厳しい
#   （「文書のどこかに数字があればOK」では、別ページの同じ数字で
#     通ってしまうことを実測で確かめた）。
#   ★ 依頼文を送る前に、必ず走らせること:
#       python tests/iraibun_kakunin.py
check('★依頼文の検査が置いてある（tests/iraibun_kakunin.py）',
      os.path.exists(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                  'iraibun_kakunin.py')), '無い')

# 10) バブルと石油の換金（2026-08-23）
GINKO = yomu(os.path.join(SRC, 'Ginko.java'))
for mei, kitai, yobi in (('SEKIYU_NEDAN', 8, '石油の売値'), ('SEKIYU_URU_JIDAI', 2, '石油を売れる時代'),
                         ('BUBBLE_JIDAI', 3, 'バブルの時代'), ('BUBBLE_BAI', 2, 'バブルの倍率')):
    mm = re.search(r'%s = (\d+);' % mei, GINKO)
    check('%s（%s）= %d' % (yobi, mei, kitai), mm and int(mm.group(1)) == kitai,
          '実装=%s' % (mm.group(1) if mm else 'なし'))
for na, bun in (('B', B), ('GM', G)):
    check('説明書%s: 石油の売値 8 が書いてある' % na, '8円' in bun or '1本 8' in bun)
    check('説明書%s: バブルが書いてある' % na, 'バブル' in bun)

# ── 11) 資料_商品一覧.md の景品表（2026-08-23 に足した）──
#
# ★★ なぜ足したか ★★
#   この資料の景品表は手で写した表で、2026-08-23 の「集める5種を 0.5% に
#   そろえる」変更のあと、護符 100（実装は 50）・原木 980（実装は 1030）の
#   ように黙って食い違ったまま残っていた。
#   この検査は資料を1つも見ていなかったので、全部 緑のまま通り抜けていた。
#   → 資料は tests/shiryou_gacha.py が Gacha.java から作る。
#     ここでは「作り直した物と今の中身が同じか」を見る。
SHIRYOU = yomu(os.path.join(DOCS, '資料_商品一覧.md'))
_jidai_mei = {1: '鉄器', 2: '中世', 3: '近代', 4: '現代'}
for _j in (1, 2, 3, 4):
    _atama = '### 景品表 ── %s（' % _jidai_mei[_j]
    _i = SHIRYOU.find(_atama)
    check('資料_商品一覧: %s の景品表がある' % _jidai_mei[_j], _i >= 0)
    if _i < 0:
        continue
    _owari = SHIRYOU.index('お金の期待値', _i)
    _honbun = SHIRYOU[_i:_owari]
    # 実装のその時代の行と、資料の行が【同じ順で・同じ値で】並んでいるか
    _jissou = [(na, int(kazu), int(k), int(om))
               for na, _m, kazu, k, om, jd, _t in kei if int(jd) == _j]
    _shiryou = [(g[0].strip(), int(g[1]), (0 if g[2].strip() == '–' else int(g[2])), int(g[3]))
                for g in re.findall(r'\n\| ([^|]+) \| (\d+) \| ([^|]+) \| (\d+) \|', _honbun)]
    check('★★資料_商品一覧: %s の景品表が実装と一致（実装 %d 行 / 資料 %d 行）'
          % (_jidai_mei[_j], len(_jissou), len(_shiryou)),
          _jissou == _shiryou,
          '食い違い=' + str([(a, b) for a, b in zip(_jissou, _shiryou) if a != b][:3]))
    _m = re.search(r'（(\d+) 件 / 値段 (\d+)）', SHIRYOU[_i:_i + 80])
    check('資料_商品一覧: %s の件数が実装と一致' % _jidai_mei[_j],
          _m and int(_m.group(1)) == len(_jissou),
          '資料=%s / 実装=%d' % (_m.group(1) if _m else 'なし', len(_jissou)))

# 「集める5種」の出現率の表（景品表とは別にもう1つある）
#   ★ ここも 2026-08-23 の 0.5% 変更で止まっていた（護符「鉄器 1%」など）。
# ★ tokushu_mei は遺物も含む。この表は「集める5種」だけなので、TOKUSHU_HYOU から取り直す。
_m = re.search(r'TOKUSHU_HYOU\s*=\s*\{(.*?)\n    \};', SHOURI, re.S)
_atsumeru = re.findall(r'\{"([^"]+)"', _m.group(1)) if _m else []
check('集める5種を TOKUSHU_HYOU から読めた（%d 種）' % len(_atsumeru), len(_atsumeru) == 5,
      '実際=%d' % len(_atsumeru))
for _na in _atsumeru:
    _deru = []
    for _j in (1, 2, 3, 4):
        _om = [int(r[4]) for r in kei if r[0] == _na and int(r[5]) == _j]
        if _om:
            _deru.append('%s %g%%' % (_jidai_mei[_j], _om[0] / 100.0))
    _kitai = ' / '.join(_deru) if _deru else '**出ない**'
    check('★資料_商品一覧: 「%s」の出現率が実装と一致（%s）' % (_na, _kitai),
          ('| %s |' % _na) in SHIRYOU and _kitai in SHIRYOU, '資料に無い')

# ── 12) ルールブックのプロンプト（2026-08-23・案2「実装どおり」）──
#
# ★ この文書は「数字はすべて実装から読んだ確定値」と名乗っている。
#   名乗る以上、機械で確かめる（資料_商品一覧.md が黙ってズレた前例があるため）。
RULE = yomu(os.path.join(DOCS, 'ルールブック_プロンプト_生成AI絵版.md'))

check('ルールブック: 企画案ではなく実装だと明記している',
      'いま動いている実装' in RULE)
check('ルールブック: 時代は5つ（石器は廃止・未来を追加）',
      '鉄器 → 中世 → 近代 → 現代 → 未来' in RULE and '石器は廃止' in RULE)
check('ルールブック: 勢力5つの名前', all(k in RULE for k in
      ('丘陵', '森林', '川', '内海', '岩場')))
check('ルールブック: 人数 125人', '125人' in RULE)

# 時代を進める条件 ── データパックの settei と突き合わせる
for kagi, kitai, yobi in (('進行_石油_鉄器', 100, '石油(鉄器)'), ('進行_石油_中世', 250, '石油(中世)'),
                          ('進行_石油_近代', 500, '石油(近代)'),
                          ('進行_貯金_鉄器', 5000, '貯金(鉄器)'), ('進行_貯金_中世', 15000, '貯金(中世)'),
                          ('進行_貯金_近代', 50000, '貯金(近代)'),
                          ('進行_建築_鉄器', 200, '建築(鉄器)'), ('進行_建築_中世', 400, '建築(中世)'),
                          ('進行_建築_近代', 800, '建築(近代)')):
    mm = re.search(r'%s settei (\d+)$' % kagi, LOAD, re.M)
    jissou = int(mm.group(1)) if mm else -1
    check('ルールブック: 進行の %s が実装(%d)と一致' % (yobi, jissou),
          jissou == kitai and ('{:,}'.format(kitai) in RULE or str(kitai) in RULE),
          '実装=%d / 期待=%d' % (jissou, kitai))

# 勝ち方4つ
# ★★ 2026-08-29: '人望勝利' → '未来到達勝利' に入れ替えた ★★
#   人望は傭兵ごと止めたのに、この検査が【あること】を要求していた。
#   ＝ 止めた仕組みを検査が守っていた。正しく消すと落ちる状態だった。
for na in ('戦争勝利', '経済勝利', '超特殊勝利', '未来到達勝利'):
    check('ルールブック: 勝ち方に %s がある' % na, na in RULE)
check('★ルールブック: 現代へ着くことは勝ちではないと書いてある',
      '「現代へ到達したこと」自体は勝ちではありません' in RULE)
check('★★ルールブック: 未来到達勝利が勝ち方に入っている',
      '未来到達勝利' in RULE and '現代から「未来」へ進む' in RULE)

# 集める5種の出る時代（案C）
check('★★ルールブック: 歯車と月の石は現代だけ',
      '| 蒸気機関の歯車 / 月の石 | **現代だけ** |' in RULE)
check('★ルールブック: 近代までに集められるのは3種まで',
      '近代までに集められるのは3種まで' in RULE)
# 案D
check('★★ルールブック: 略奪で紙を奪われると書いてある',
      '略奪されると、リーダーが持っている紙を1枚 奪われます' in RULE)
check('★ルールブック: 遺物は紙を奪われても効果が移らない',
      '紙を奪われても効果は移りません' in RULE)

# 実装の定数と突き合わせる数字
_m = re.search(r'JUKI_KAIHOU = (\d+);', SHOP)
check('ルールブック: 銃器専門店 %s 超で開く が実装と一致' % (_m.group(1) if _m else '?'),
      _m and ('{:,}'.format(int(_m.group(1))) in RULE), '実装=%s' % (_m.group(1) if _m else 'なし'))
check('ルールブック: 下剋上150 / 戦争宣誓100',
      '下剋上(150)' in RULE and '戦争宣誓(100)' in RULE)
check('ルールブック: 経済勝利 500,000', '**500,000**' in RULE)
check('ルールブック: 石油は1本8円（中世から）', '中世から1本8円' in RULE or '1本 8円' in RULE)
check('ルールブック: 未実装の「裏切りの色」を外した',
      'ルールブックからは外しました' in RULE and '名前の色が変わる' not in RULE)
check('★ルールブック: 数字を作るなと指示している',
      '書いていない数字は作らない' in RULE and '(調整中)' not in RULE)

# ── 13) 時代5「未来」（2026-08-23 のご指示）──
#
# ★ 「現代へ到達は勝利条件にしません。現代から未来に到達したら勝利とします
#    （建築ブロック数と石油の数で勝利みたいな）」
GM = G
for kagi, yobi, bun in (('進行_石油_現代', '未来に要る石油', True),
                        ('進行_建築_現代', '未来に要る建材', True),
                        ('進行_貯金_現代', '未来に要る貯金', False)):
    _m = re.search(r'scoreboard players set %s settei (-?\d+)' % re.escape(kagi), LOAD)
    _v = int(_m.group(1)) if _m else None
    check('%s（%s）を load が持っている' % (yobi, kagi), _v is not None, 'なし')
    if _v is None or not bun:
        continue
    _kaki = '{:,}'.format(_v)
    for _na, _b in (('README', yomu(os.path.join(NE, 'README.md'))),
                    ('GM説明書', GM), ('ルールブック', RULE)):
        check('%s: %s %s が実装と一致' % (_na, yobi, _kaki),
              (_kaki in _b) or (str(_v) in _b), '書いていない')

# ★★ 建材が数える区画に収まるか（超えたら永久に勝てない）★★
_ken = yomu(os.path.join(DP, 'shinko', 'kenchiku.mcfunction'))
_m = re.search(r'clone ~(-?\d+) (\d+) ~(-?\d+) ~(-?\d+) (\d+) ~(-?\d+)', _ken)
_taiseki = 0
if _m:
    _taiseki = ((abs(int(_m.group(4)) - int(_m.group(1))) + 1)
                * (abs(int(_m.group(5)) - int(_m.group(2))) + 1)
                * (abs(int(_m.group(6)) - int(_m.group(3))) + 1))
_m2 = re.search(r'scoreboard players set 進行_建築_現代 settei (\d+)', LOAD)
_need = int(_m2.group(1)) if _m2 else 0
check('★★未来に要る建材 %d が、数える区画 %d に収まる' % (_need, _taiseki),
      0 < _need < _taiseki, '超えている＝永久に勝てない')
check('文書が「6,912」（数える区画の広さ）を書いている',
      '6,912' in yomu(os.path.join(NE, 'README.md')), '書いていない')

# 勝ち方は4つ（戦争 / 経済 / 超特殊 / 未来到達）
#   ★★ 2026-08-29: ここも「5つ」を要求していた ★★
#     2（人望）は傭兵ごと止めたのに、検査が5つのままだったので、
#     正しく直した README が落ちた。**検査の方が古かった。**
for _na, _b in (('README', yomu(os.path.join(NE, 'README.md'))), ('GM説明書', GM), ('ルールブック', RULE)):
    check('%s: 勝ち方に未来到達勝利がある' % _na, '未来到達勝利' in _b, '無い')
check('README: 勝利条件が4つだと書いてある', '勝利条件（4つ' in yomu(os.path.join(NE, 'README.md')))
check('★README: 番号が 1・3・4・5 と飛ぶ理由を書いてある',
      '番号が 1・3・4・5 と飛びます' in yomu(os.path.join(NE, 'README.md')),
      '無い＝読んだ人が「2 はどこ？」で止まる')

# ══════════════════════════════════════════════════════════
#  ★★ KUN 勢力が戻ってきていないか（2026-08-29 のご指示）★★
#    「KUN勢力は必要ない。KUNにまつわるものは消してほしい。」
#
#    ★ KUN という字には3つの別物が混ざる。ここで見るのは①だけ。
#        ① 勢力・仕組みとしての KUN  → **あってはいけない**
#        ② 主催者としての KUN（「KUN の50人クラフト」）→ 企画の相手。良い
#        ③ フォルダ名の KUN（Desktop/KUN）→ ただの道順。良い
#      ②③まで禁じると、企画の文書が書けなくなる。
# ══════════════════════════════════════════════════════════
#    ★★ 「今もある」と「もう無い」を分ける ★★
#      「人望勝利は無くなりました」という説明まで禁じると、
#      なぜ番号が飛ぶのかを書けなくなる（実際に一度 落ちた）。
#      → **行ごと**に見て、その行に「止めた/外した/廃止/でした」等の
#        済んだ印が無い時だけ、生き残りとみなす。
_IKENAI = ('roadhog', '人望勝利', 'KUN雇用', 'KUN を含む', 'KUN が自勢力',
           'KUN の店', 'KUN の棚', '蛮族王・傭兵王 KUN')
_SUNDA = ('止め', '外し', '廃止', '無くなり', '使っていません', 'でした',
          '作りません', '空振り', '入れ替え', '休止', 'ごと止めた')


def _ikinokori(bun):
    """「今もある」ように読める行だけを返す。"""
    deta = []
    for g in bun.split(chr(10)):
        if not any(k in g for k in _IKENAI):
            continue
        if any(k in g for k in _SUNDA):
            continue          # 「もう無い」と書いてある行は良い
        deta.append(g.strip()[:60])
    return deta


for _mei, _p in (('README', 'README.md'),
                 ('GM説明書', 'docs/ゲームマスター説明書.md'),
                 ('ルールブック', 'docs/ルールブック_プロンプト_生成AI絵版.md'),
                 ('実機確認リスト', 'docs/実機確認リスト.md'),
                 ('ソロ検証の順路', 'docs/ソロ検証の順路.md'),
                 ('休止中の記録', 'docs/蛮族と傭兵（休止中）.md')):
    _deta = _ikinokori(yomu(os.path.join(NE, _p)))
    check('★★%s に KUN 勢力が生き残っていない' % _mei, not _deta,
          '生きている行: %s' % _deta)

# ★ コードの側にも、名前が焼き付いていないか
for _f in ('Shouri.java', 'Shop.java', 'JidaiCraft.java', 'Kane.java'):
    _src = yomu(os.path.join(SRC, _f))
    check('★%s に roadhog / KUN の定数が無い' % _f,
          'roadhog' not in _src and 'KUN =' not in _src and 'KUN_JIDAI' not in _src,
          'KUN 勢力の残骸')

# ── 14) 蛮族／傭兵（2026-08-23 のご指示）──
#
# ── 16) 同士討ちの禁止と、キルで10%奪取（2026-08-24 のご指示）──
_TATAKAI = yomu(os.path.join(SRC, 'Tatakai.java'))
_m = re.search(r'UBAU_WARIAI = (\d+);', _TATAKAI)
_ubau = int(_m.group(1)) if _m else -1
check('奪う割合を読めた（%d%%）' % _ubau, _ubau > 0, 'なし')
_README3 = yomu(os.path.join(NE, 'README.md'))
for _na, _b in (('GM説明書', G), ('README', _README3), ('ルールブック', RULE)):
    check('%s: 同じ勢力は傷つけ合えないと書いている' % _na,
          '同じ勢力の' in _b and '傷つけ' in _b, '無い')
    check('%s: 奪う割合 %d%% が実装と一致' % (_na, _ubau), ('%d%%' % _ubau) in _b, '書いていない')
    check('%s: 勢力の貯金からは取らないと書いている' % _na,
          '勢力の貯金からは' in _b, '無い')
check('★GM説明書: hire の実態（勢力のみに帰属）を書いている',
      'しっかりその勢力のみに帰属します' in G, '無い')

print('')
print('=' * 62)
print('結果: PASS %d / FAIL %d' % (pass_, fail_))
sys.exit(1 if fail_ else 0)
