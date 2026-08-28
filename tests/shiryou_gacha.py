# -*- coding: utf-8 -*-
"""docs/資料_商品一覧.md のガチャ景品表を、Gacha.java から作り直す。

  ★★ なぜ道具にしたか（2026-08-23）★★
    この資料の景品表は【手で写した表】だった。
    2026-08-23 の「集める5種を 0.5% にそろえる」変更のあと、
    護符 100（実装は 50）・原木 980（実装は 1030）のように
    黙って食い違ったまま残っていた。
    説明書の検査（bunsho_kakunin.py）はこの資料を見ていなかったので、
    全部 緑のまま通り抜けていた。

    → 表は正本（Gacha.KEIHIN）から作る。手で写さない。
    → bunsho_kakunin.py にも突き合わせを足したので、次からは落ちる。

  使い方:
    python tests/shiryou_gacha.py          … 作り直す
    python tests/shiryou_gacha.py --miru   … 差分を出すだけ（書かない）
"""
import io
import os
import re
import sys

NE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GACHA = os.path.join(NE, 'plugin', 'src', 'main', 'java', 'jidai', 'Gacha.java')
SHOURI = os.path.join(NE, 'plugin', 'src', 'main', 'java', 'jidai', 'Shouri.java')
SHIRYOU = os.path.join(NE, 'docs', '資料_商品一覧.md')

JIDAI_MEI = {1: '鉄器', 2: '中世', 3: '近代', 4: '現代'}
TOU_MEI = {'HAZURE': 'ハズレ', 'SHO': '小', 'CHU': '中', 'DAI': '大'}


def yomu(p):
    return io.open(p, encoding='utf-8').read()


def deru(moji):
    """端末が cp932 でも落ちないように出す。

    ★ 資料には en ダッシュ（–）が入っている。そのまま print すると
      この環境の端末（cp932）で UnicodeEncodeError になり、
      差分を見ようとしただけで道具が止まる。
    """
    kodo = getattr(sys.stdout, 'encoding', None) or 'utf-8'
    print(moji.encode(kodo, 'replace').decode(kodo))


def keihin():
    """Gacha.KEIHIN を (名前, 個数, 金, 重み, 時代, 等級) の並びで読む。"""
    s = yomu(GACHA)
    m = re.search(r'NEDAN\s*=\s*\{([^}]+)\}', s)
    nedan = [int(x.strip()) for x in m.group(1).split(',')]
    hyou = []
    for na, _mat, kazu, kane, om, ji, tou in re.findall(
            r'new Keihin\("([^"]+)",\s*Material\.(\w+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\w+)\)', s):
        hyou.append((na, int(kazu), int(kane), int(om), int(ji), tou))
    return nedan, hyou


def tokushu_mei():
    """集める5種と遺物の名前。Shouri が正本。"""
    s = yomu(SHOURI)
    atsu = re.search(r'TOKUSHU_HYOU\s*=\s*\{(.*?)\n    \};', s, re.S)
    ibu = re.search(r'IBUTSU_HYOU\s*=\s*\{(.*?)\n    \};', s, re.S)
    return (set(re.findall(r'\{"([^"]+)"', atsu.group(1))),
            set(re.findall(r'\{"([^"]+)"', ibu.group(1))))


def kubun(na, kane, tou, atsumeru, ibutsu):
    if tou == 'HAZURE':
        return 'はずれ'
    if kane > 0:
        return 'お金(特賞)' if tou == 'DAI' else 'お金'
    if na in atsumeru:
        return '特殊(集める5種)'
    if na in ibutsu:
        return '特殊(遺物)'
    return 'ふつう'


def setsu(jidai):
    """1つの時代ぶんの節を組み立てる。"""
    nedan, hyou = keihin()
    atsumeru, ibutsu = tokushu_mei()
    gyou = [r for r in hyou if r[4] == jidai]
    ne = nedan[jidai]

    zen = sum(r[3] for r in gyou)
    hazu = sum(r[3] for r in gyou if r[5] == 'HAZURE')
    kane_om = sum(r[3] for r in gyou if r[5] != 'HAZURE' and r[2] > 0)
    dai_om = sum(r[3] for r in gyou if r[5] == 'DAI' and r[2] > 0)
    toku = sum(r[3] for r in gyou
               if r[5] != 'HAZURE' and r[2] == 0 and (r[0] in atsumeru or r[0] in ibutsu))
    futsu = zen - hazu - kane_om - toku
    kitai = sum(r[2] * r[3] for r in gyou) / float(zen)

    de = []
    de.append('### 景品表 ── %s（%d 件 / 値段 %d）' % (JIDAI_MEI[jidai], len(gyou), ne))
    de.append('')
    de.append('| 品名 | 個数 | 金 | 重み | 出現率% | 等級 | 区分 |')
    de.append('|---|---:|---:|---:|---:|---|---|')
    for na, kazu, kane, om, _ji, tou in gyou:
        de.append('| %s | %d | %s | %d | %s | %s | %s |'
                  % (na, kazu, (str(kane) if kane > 0 else '–'), om,
                     ('%g' % (om / 100.0)), TOU_MEI[tou],
                     kubun(na, kane, tou, atsumeru, ibutsu)))
    de.append('')
    de.append('合計重み %d（=100%%）／ はずれ %g%% ・ ふつう %g%% ・ お金 %g%%（うち特賞 %g%%）・ 特殊 %g%%'
              % (zen, hazu / 100.0, futsu / 100.0, kane_om / 100.0, dai_om / 100.0, toku / 100.0))
    de.append('お金の期待値 %.2f（値段 %d の %.2f%%）' % (kitai, ne, kitai / ne * 100))
    return '\n'.join(de)


def atsumeru_setsu():
    """「集める5種」の出現率の表を組み立てる。

    ★ 名前・絵・番号・時代は Shouri.TOKUSHU_HYOU（行末コメントに時代がある）。
      出現率は Gacha.KEIHIN の重み。どちらも正本から読む。
    """
    _nedan, hyou = keihin()
    ss = yomu(SHOURI)
    m = re.search(r'TOKUSHU_HYOU\s*=\s*\{(.*?)\n    \};', ss, re.S)
    gyou = re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)"\},\s*//\s*(\S+)', m.group(1))

    de = []
    de.append('### 集める5種（`Shouri.TOKUSHU_HYOU` ─ **%d 件**）' % len(gyou))
    de.append('')
    de.append('勝利条件その4「超特殊勝利」＝どこかの勢力の**リーダー1人**が5種すべてを持っていれば勝ち。')
    de.append('')
    de.append('| 名前 | 時代 | 絵のファイル名 | 見た目の番号 | 出現率%（時代別） |')
    de.append('|---|---|---|---:|---|')
    for na, e, ban, ji in gyou:
        deru_ji = []
        for j in (1, 2, 3, 4):
            om = [r[3] for r in hyou if r[0] == na and r[4] == j]
            if om:
                deru_ji.append('%s %g%%' % (JIDAI_MEI[j], om[0] / 100.0))
        de.append('| %s | %s | %s | %s | %s |'
                  % (na, ji, e, ban, (' / '.join(deru_ji) if deru_ji else '**出ない**')))
    de.append('')
    de.append('- 「時代」欄は `TOKUSHU_HYOU` の行末コメント（`// 鉄器` など）。表の列としては持っていない。')
    de.append('- 出現率は `Gacha.KEIHIN` の重み（1万分率 ÷100）。**この表は `tests/shiryou_gacha.py` が正本から作る。手で直さない。**')
    de.append('- ★ **2026-08-23（案C）: 「蒸気機関の歯車」「月の石」は現代のガチャにしか出ません。**')
    de.append('  近代までに集められるのは 3種までで、5種そろえるには現代へ着く必要があります。')
    de.append('- ★ **2026-08-23（案D）: 略奪が成立すると、相手勢力のリーダーが持つこの5種の紙を1枚 奪います。**')
    de.append('- **勢力ごとに各1個まで**。すでにその勢力へ出ている物・傭兵/無所属には出ず、ふつうの当たりに引き直される。')
    return '\n'.join(de)


def main():
    miru = '--miru' in sys.argv
    s = yomu(SHIRYOU)
    kazu = 0
    for jidai in (1, 2, 3, 4):
        atama = '### 景品表 ── %s（' % JIDAI_MEI[jidai]
        i = s.find(atama)
        if i < 0:
            raise SystemExit('%s の節が見つからない' % JIDAI_MEI[jidai])
        # 節の終わりは「お金の期待値 …」の行末
        j = s.index('お金の期待値', i)
        j = s.index('\n', j)
        atarashii = setsu(jidai)
        if s[i:j] != atarashii:
            kazu += 1
            print('--- %s: 書き換える' % JIDAI_MEI[jidai])
            if miru:
                for a, b in zip(s[i:j].split('\n'), atarashii.split('\n')):
                    if a != b:
                        deru('    旧 ' + a)
                        deru('    新 ' + b)
        s = s[:i] + atarashii + s[j:]
    # 「集める5種」の別表も同じ正本から作り直す
    _a = '### 集める5種（'
    _i = s.index(_a)
    _j = s.index('### 遺物11種', _i)
    _atarashii = atsumeru_setsu() + '\n\n'
    if s[_i:_j] != _atarashii:
        kazu += 1
        print('--- 集める5種の表: 書き換える')
        if miru:
            for a, b in zip(s[_i:_j].split('\n'), _atarashii.split('\n')):
                if a != b:
                    deru('    旧 ' + a)
                    deru('    新 ' + b)
    s = s[:_i] + _atarashii + s[_j:]

    if miru:
        print('（--miru なので書いていない）')
        return
    io.open(SHIRYOU, 'w', encoding='utf-8', newline='\n').write(s)
    print('OK: docs/資料_商品一覧.md の景品表 %d 節を作り直した' % kazu)


main()
