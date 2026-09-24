# -*- coding: utf-8 -*-
"""クリア想定時間を、実装の数字から見積もる。

  ★ ここに書いた定数は【すべて実装から読んだ値】。推測は「仮定」の節に分けて置く。

  出典:
    datapacks/.../functions/load.mcfunction   … settei の既定値
    datapacks/.../functions/sekiyu/waku.mcfunction … 1回に4か所へ1本ずつ
    datapacks/.../functions/shinko/hantei.mcfunction / shounin_*.mcfunction
    datapacks/.../functions/shinko/chuo.mcfunction … 2勢力で中央が上がる・4割徴収
    datapacks/.../functions/fuhai/kyotsu.mcfunction … 腐敗
    plugin/.../Ginko.java                      … 貴金属の売値
    tests/horu_tsukuru.py                      … 掘りの確率
"""

import io
import os
import re
import sys

# ── 実装から読んだ値（settei / ソース）───────────────────
SEKIYU_KANKAKU = 10          # 石油_間隔_* = 10秒（全時代）
SEKIYU_DEGUCHI = 4           # waku.mcfunction が1回に落とす本数（4か所×1本）
SEKIYU_JOUGEN = {1: 750, 2: 1800, 3: 3350, 4: 3350}   # 石油_上限_*（中央時代ごとの総本数）

def shinko_yomu():
    """その勢力の今の時代 → (石油の保有, 貯金, 建築)。load.mcfunction から読む。

    ★★ ここは手で写さない（2026-08-23）★★
      写した表は、片方だけ直した時に黙って食い違う。
      2026-08-23 に時代5「未来」を足した時、写しのままなら
      この見積りだけが「現代が終わり」と言い続けた。

    ★ 「その時代から次へ進むのに要る量」なので、
      現代(4)の欄は【現代 → 未来】の必要量になる。
    """
    ne = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    load = io.open(os.path.join(ne, 'datapacks', 'jidai_craft', 'data', 'jidai',
                                'functions', 'load.mcfunction'), encoding='utf-8').read()

    def hiku(kagi):
        m = re.search(r'scoreboard players set %s settei (-?\d+)' % re.escape(kagi), load)
        return int(m.group(1)) if m else None

    de = {}
    for ban, mei in ((1, '鉄器'), (2, '中世'), (3, '近代'), (4, '現代')):
        o, k, c = hiku('進行_石油_' + mei), hiku('進行_貯金_' + mei), hiku('進行_建築_' + mei)
        if o is None:
            continue           # その時代から先は無い
        de[ban] = (o, k or 0, c or 0)
    return de


SHINKO = shinko_yomu()
SAIGO = max(SHINKO) + 1      # 最後の時代（＝ここへ着いたら勝ち）
JOUGEN_BAI = 1.2             # 石油の保有上限 = 必要量 × 1.2（jidai:sekiyu/jougen）
CHUO_HITSUYO = 2             # chuo.mcfunction: 2勢力が到達すると中央が上がる
CHOSHU_RITSU = 40            # リセット_徴収率 = 40（中央が上がった時に4割 徴収）
SENKO_LOSS = 50              # 先行_ロス率 = 50（中央より先の勢力は拾う時に半分 失う）
FUHAI_YUYO = 900             # 腐敗_猶予秒 = 900（15分）
FUHAI_RITSU = 10             # 腐敗_減少率 = 10（%）
KEIZAI_SHOURI = 500000       # 経済_勝利_貯金


def bubble_yomu():
    """貴金属のバブル（中央が近代に入ると売値が倍）を実装から読む。

    ★ 2026-08-26 に足した。それまで模型に入っておらず、
      道具（652分）と文書（5.8時間）が食い違っていた。
    """
    g = io.open(os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), 'plugin', 'src', 'main', 'java',
        'jidai', 'Ginko.java'), encoding='utf-8').read()
    jidai = re.search(r'BUBBLE_JIDAI = (\d+);', g)
    bai = re.search(r'BUBBLE_BAI = (\d+);', g)
    assert jidai and bai, 'Ginko.java にバブルの定数が無い'
    return int(jidai.group(1)), int(bai.group(1))

SENSOU_JUNBI = 300           # 戦争_準備秒
SENSOU_KOUSEN = 600          # 戦争_交戦秒
SENSOU_KINSHI = 1800         # 戦争_禁止秒
# ★ 2026-09-09: 1回いくらの上限は廃止。1回で相手の総量の 1%（略奪_割る数=100）。
#   100回 壊すと 0.99^100 ＝ 約37% が残る。空にはならない。
RYAKUDATSU_WARU = 100          # 略奪_割る数（100 なら 1%）
SENRYOU_KAISU = 100            # 占領_必要回数（銀行を壊す回数）
RYAKUDATSU_KANKAKU = 10        # 略奪_間隔秒（壊した本人ごとのクールダウン）

# 掘り: 石・花崗岩・閃緑岩・安山岩（深層岩は 1.5倍）
HORI = [(0.15, 1), (0.08, 5), (0.04, 10), (0.01, 15)]   # (確率, 売値)
HORI_NEZA = (0.001, 30)      # ネザライト（プラグイン。深層岩は 1/500）
SHINSO_BAI = 1.5

# ── 仮定（実装から読めない。ここだけが見積もりの幅）─────────
SEIRYOKU_KAZU = 5

# ★ 人数は【仮定】の側。コマンドで変えられる（実装は直さない）。
#     python tests/clear_time.py              … 既定（1勢力20人＝合計100人）
#     python tests/clear_time.py --ninzu 12   … 1勢力12人＝合計60人
#
# ★★ 2026-08-26: 傭兵を止めたので、傭兵の取り分を外した ★★
#   前は 25/125＝20% を引いていた。今は湧いた石油が【全部 勢力へ回る】。
#   そのぶん時代の到達が速くなる。経緯は docs/蛮族と傭兵（休止中）.md。
SEIRYOKU_NINZU = 20          # 1勢力あたりの人数
for _i, _a in enumerate(sys.argv):
    if _a == '--ninzu' and _i + 1 < len(sys.argv):
        SEIRYOKU_NINZU = int(sys.argv[_i + 1])

HORU_HITO = 0.5              # 勢力のうち、掘りに回る割合
# ★★ 2026-08-31 に実機で測った値 ★★
#   1分間で 71ブロック。出た物は ラピス6 / 鉄9 / 金4 / ダイヤ0。
#   その19個は「石だけを71ブロック掘った時の期待値（20個・78円）」と
#   ほぼ一致した（実測79円 = 101%）。**確率表は正しい。**
#   ズレていたのは、ここの速さの想定（90）の方だった。
HORU_SOKUDO = 71             # 1人が1分に掘るブロック数（2026-08-31 実測）
SHINSO_WARIAI = 0.5          # 掘る場所のうち深層岩の割合
CHOKIN_HIRITSU = 0.6         # 稼いだ金のうち、勢力の貯金へ回る割合（残りは装備・ガチャ）

# 石油の取り分（プラントは4か所。先頭の勢力が押さえやすい）
TORIBUN = {'先頭': 0.28, '2番手': 0.24, '中位': 0.18, '下位': 0.15, '最下位': 0.15}


def hori_kitaichi():
    """石1個あたりの期待金額（円）。"""
    futsu = sum(p * n for p, n in HORI) + HORI_NEZA[0] * HORI_NEZA[1]
    shinso = sum(p * SHINSO_BAI * n for p, n in HORI) + HORI_NEZA[0] * 2 * HORI_NEZA[1]
    return futsu, shinso, futsu * (1 - SHINSO_WARIAI) + shinso * SHINSO_WARIAI


def sekiyu_bunpun():
    """プラント全体の湧き（本/分）。"""
    return SEKIYU_DEGUCHI * 60.0 / SEKIYU_KANKAKU


def kane_bunpun():
    """1勢力あたりの、貯金に回る金（円/分）。"""
    _f, _s, kitai = hori_kitaichi()
    hito = SEIRYOKU_NINZU * HORU_HITO
    return hito * HORU_SOKUDO * kitai * CHOKIN_HIRITSU


def sim(toribun, senko_penalty=True, fuhai=True, choshu=True):
    """1分きざみで進める。返り値: {時代: 到達した分}"""
    zen = sekiyu_bunpun()
    kane_r = kane_bunpun()
    kuni = [{'jidai': 1, 'oil': 0.0, 'kane': 0.0, 'kijun': 0.0, 'byou': 0,
             'wake': w} for w in toribun]
    chuo = 1
    waki = 0.0                    # 今の中央時代で湧いた本数
    tassei = {}
    for t in range(1, 60 * 12):   # 12時間まで
        # --- 湧き（上限に達したら止まる）---
        nokori = max(0.0, SEKIYU_JOUGEN.get(chuo, 0) - waki)
        deta = min(zen, nokori)
        waki += deta
        for k in kuni:
            eta = deta * k['wake']
            if senko_penalty and k['jidai'] > chuo:
                eta *= (100 - SENKO_LOSS) / 100.0
            k['oil'] += eta
            k['kane'] += kane_r
            # ★ 石油の保有上限（jidai:sekiyu/jougen）。
            #   これを入れないと「いくらでも溜まる」ことになり、
            #   未来の必要量を決める時に「届く」と嘘をつく。
            need = SHINKO.get(k['jidai'], (0, 0, 0))[0]
            if need > 0:
                k['oil'] = min(k['oil'], need * JOUGEN_BAI)
        # --- 腐敗（15分ごとに 10%）---
        if fuhai and t % (FUHAI_YUYO // 60) == 0:
            for k in kuni:
                k['oil'] *= (100 - FUHAI_RITSU) / 100.0
        # --- 時代進行（条件が揃った勢力は、その分に承認されるとする）---
        agatta = False
        for k in kuni:
            if k['jidai'] >= SAIGO:
                continue
            need_oil, need_kane, _ = SHINKO[k['jidai']]
            if k['oil'] >= need_oil and k['kane'] >= need_kane:
                k['kane'] -= need_kane          # 貯金だけ消費（shounin_*）
                k['jidai'] += 1
                agatta = True
        if agatta:
            # --- 中央が上がるか（2勢力が到達）---
            tsugi = chuo + 1
            if chuo < 4 and sum(1 for k in kuni if k['jidai'] >= tsugi) >= CHUO_HITSUYO:
                chuo = tsugi
                waki = 0.0
                if choshu:
                    for k in kuni:
                        k['oil'] *= (100 - CHOSHU_RITSU) / 100.0
            saikou = max(k['jidai'] for k in kuni)
            if saikou not in tassei:
                tassei[saikou] = t
            # ★ 最後の時代（未来）へ着いたら、そこで決着（未来到達勝利）
            if saikou >= SAIGO:
                return tassei, kuni, chuo, t
    return tassei, kuni, chuo, None


def fun(m):
    if m is None:
        return '（12時間内に届かない）'
    return '%d分（%d時間%02d分）' % (m, m // 60, m % 60)


if __name__ == '__main__':
    futsu, shinso, kitai = hori_kitaichi()
    print('=' * 64)
    print('前提（実装から読んだ値）')
    print('=' * 64)
    print('石油の湧き        : %d本/%d秒 = %.1f本/分 = %d本/時'
          % (SEKIYU_DEGUCHI, SEKIYU_KANKAKU, sekiyu_bunpun(), sekiyu_bunpun() * 60))
    print('中央時代ごとの総量: 鉄器%d / 中世%d / 近代%d 本'
          % (SEKIYU_JOUGEN[1], SEKIYU_JOUGEN[2], SEKIYU_JOUGEN[3]))
    print('  → 湧き切るまで  : 鉄器%.0f分 / 中世%.0f分 / 近代%.0f分'
          % (SEKIYU_JOUGEN[1] / sekiyu_bunpun(), SEKIYU_JOUGEN[2] / sekiyu_bunpun(),
             SEKIYU_JOUGEN[3] / sekiyu_bunpun()))
    print('掘りの期待金額    : 石 %.3f円/個 / 深層岩 %.3f円/個 → 平均 %.3f円/個'
          % (futsu, shinso, kitai))
    print('1勢力の稼ぎ       : %.0f人×%d個/分×%.3f円×貯金へ%.0f%% = %.0f円/分'
          % (SEIRYOKU_NINZU * HORU_HITO, HORU_SOKUDO, kitai, CHOKIN_HIRITSU * 100,
             kane_bunpun()))
    print('  → 進行に要る金  : 5,000+15,000+50,000 = 70,000円 → 掘りだけで %.0f分'
          % (70000 / kane_bunpun()))
    print()

    print('=' * 64)
    print('時代の到達 ── ★ 未来(時代5)へ着いた勢力が勝ち（2026-08-23 のご指示）')
    print('=' * 64)
    for na, tb in (('均等に分け合う', [0.2] * 5),
                   ('ふつう（先頭が押さえる）', [0.28, 0.24, 0.18, 0.15, 0.15]),
                   ('先頭が強い', [0.40, 0.25, 0.15, 0.10, 0.10])):
        tassei, kuni, chuo, owari = sim(tb)
        print('%-24s 中世 %-20s 近代 %-20s 現代 %-20s 未来 %s'
              % (na, fun(tassei.get(2)), fun(tassei.get(3)),
                 fun(tassei.get(4)), fun(tassei.get(5))))
    print()

    print('-- 内訳（ふつう・先頭の勢力）--')
    tassei, kuni, chuo, owari = sim([0.28, 0.24, 0.18, 0.15, 0.15])
    print('   中世 %s / 近代 %s / 現代 %s'
          % (fun(tassei.get(2)), fun(tassei.get(3)), fun(tassei.get(4))))
    print('   ★ 未来（勝ち） %s   ← 現代の %s 後'
          % (fun(tassei.get(5)),
             ('%d分' % (tassei[5] - tassei[4])) if (tassei.get(5) and tassei.get(4)) else '？'))
    print('     現代 → 未来 の条件: 石油 %d本 / 建築 %d個 / 貯金 %d円'
          % (SHINKO[4][0], SHINKO[4][2], SHINKO[4][1]))
    print('     ★ 石油は「まだ持っているか」の壁。腐敗（15分で10%）と略奪で下回る。')
    print('       建築は「時間」の壁。数える区画は 24×12×24 = 6912 個までしか入らない。')
    print('   ★★ この見積りは【建築を数えていない】★★')
    print('     模型が見ているのは石油と貯金だけなので、上の「未来」の分は')
    print('     「石油の条件だけなら いつ揃うか」でしかない。')
    print('     実際の壁は建築 %d 個（近代の %d から +%d）で、'
          % (SHINKO[4][2], SHINKO[3][2], SHINKO[4][2] - SHINKO[3][2]))
    # ★ 置く速さは人数に比例すると見る（20人で15〜25分を基準にする）
    _t1 = 15 * 20.0 / SEIRYOKU_NINZU
    _t2 = 25 * 20.0 / SEIRYOKU_NINZU
    print('     %d人で %.0f〜%.0f分ぶんの作業と見ている。ここは実機で1回まわさないと分からない。'
          % (SEIRYOKU_NINZU, _t1, _t2))
    print('   ★ 効いてくる駆け引き: 2つめの勢力が現代に着くと中央が現代になり、')
    print('     全勢力から石油を40%徴収する。先頭は約300本まで落ちて、')
    print('     500本へ積み直すのに40分ほどかかる。')
    print('     → 「2番手が現代に着く前に建て切る」かどうかが分かれ目になる。')
    print('   12時間後の各勢力: ' + ' / '.join(
        '時代%d(石油%.0f 貯金%.0f)' % (k['jidai'], k['oil'], k['kane']) for k in kuni))
    print()

    print('-- 効きを確かめる（1つずつ外す）--')
    for na, kw in (('腐敗が無ければ', {'fuhai': False}),
                   ('4割徴収が無ければ', {'choshu': False}),
                   ('先行ペナルティが無ければ', {'senko_penalty': False})):
        tassei, _k, _c, _o = sim([0.28, 0.24, 0.18, 0.15, 0.15], **kw)
        print('%-24s 現代 %s' % (na, fun(tassei.get(4))))
    print()

    print('=' * 64)
    print('ほかの勝ち方')
    print('=' * 64)
    kr = kane_bunpun()
    bj, bb = bubble_yomu()
    print('経済勝利（貯金 %d）'  % KEIZAI_SHOURI)
    print('  バブル無し（鉄器〜中世の稼ぎのまま）: %.0f分（%.1f時間）'
          % (KEIZAI_SHOURI / kr, KEIZAI_SHOURI / kr / 60))
    # ★ 中央が近代(BUBBLE_JIDAI)へ入ると貴金属が BUBBLE_BAI 倍で売れる。
    #   近代へ着くまでは素の稼ぎ、その後は倍、として積む。
    # ★ 近代へ着く時刻を、上と同じ取り分でもう一度 出す（模型は軽いので測り直す）
    _t2, _k2, _c2, _o2 = sim([0.28, 0.24, 0.18, 0.15, 0.15])
    kindai_fun = _t2.get(3)
    if kindai_fun:
        tamatta = kr * kindai_fun
        nokori = max(0.0, KEIZAI_SHOURI - tamatta)
        zenbu = kindai_fun + nokori / (kr * bb)
        print('  バブル有り（中央が近代で貴金属 ×%d・実装のとおり）: %.0f分（%.1f時間）'
              % (bb, zenbu, zenbu / 60))
    print('  ★ ただし時代進行で 70,000 を使うので、実際はさらに %.0f分 増える'
          % (70000 / kr))
    print('戦争勝利: 1回の戦争 = 準備%d分 + 交戦%d分 = %d分。再戦禁止 %d分。'
          % (SENSOU_JUNBI / 60, SENSOU_KOUSEN / 60,
             (SENSOU_JUNBI + SENSOU_KOUSEN) / 60, SENSOU_KINSHI / 60))
    print('  4勢力を落とすので、最短でも %d分（同時に複数へ宣戦できる場合）'
          % ((SENSOU_JUNBI + SENSOU_KOUSEN) / 60 * 4))
    # ★ 2026-09-09: 1回で相手の総量の 1%。上限は無いが、掛け算なので 0 にはならない。
    nokori = (1 - 1.0 / RYAKUDATSU_WARU) ** SENRYOU_KAISU
    print('  ★ 略奪は1回で相手の総量の %.0f%%。%d回 壊しても %.0f%% は残る（0 にはならない）。'
          % (100.0 / RYAKUDATSU_WARU, SENRYOU_KAISU, nokori * 100))
    print('    止めを刺すのはビーコン。銀行を %d回 壊すと壊せるようになる。'
          % SENRYOU_KAISU)
    # ★ 1人あたり RYAKUDATSU_KANKAKU 秒に1回しか壊せない。
    #   交戦は SENSOU_KOUSEN 秒しかないので、**必要な人数**が決まる。
    hitsuyou_byou = SENRYOU_KAISU * RYAKUDATSU_KANKAKU
    for nin in (1, 3, 5, 10, 20):
        byou = hitsuyou_byou / nin
        ma = '交戦(%d分)に間に合う' % (SENSOU_KOUSEN / 60) if byou <= SENSOU_KOUSEN else '★間に合わない'
        print('      %2d人で %4.0f秒（%.1f分） … %s' % (nin, byou, byou / 60, ma))
    print('    ★ 1人では %d秒 かかり、交戦 %d秒 に収まらない。**攻めるには人数が要る**。'
          % (hitsuyou_byou, SENSOU_KOUSEN))

# =========================================================
#  超特殊勝利（特殊5種）の見積り ── 追記
# =========================================================
# ガチャの値段（Gacha.NEDAN）と、各時代の特殊の重み（Gacha.KEIHIN・1万分率）
GACHA_NEDAN = {1: 5, 2: 10, 3: 20, 4: 50}


def tokushu_omomi_yomu():
    """時代 -> {集める5種の品名: 重み} を Gacha.java / Shouri.java から読む。

    ★★ ここは手で写さない（2026-08-23）★★
      もとは時代と品名の対応をこのファイルに書き写していた。
      案C（近代の景品表から「蒸気機関の歯車」「月の石」を外す）を入れても、
      この見積りだけが「近代で5種そろう」と言い続けた。
      写した表は、片方だけ直した時に黙って食い違う。正本から読む。

      正本: Shouri.TOKUSHU_HYOU（集める5種の名前。遺物 IBUTSU_HYOU は入れない）
            Gacha.KEIHIN      （どの時代に、どの重みで出るか）
    """
    ne = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    jd = os.path.join(ne, 'plugin', 'src', 'main', 'java', 'jidai')
    gacha = io.open(os.path.join(jd, 'Gacha.java'), encoding='utf-8').read()
    shouri = io.open(os.path.join(jd, 'Shouri.java'), encoding='utf-8').read()

    hyou = re.search(r'TOKUSHU_HYOU\s*=\s*\{(.*?)\n    \};', shouri, re.S)
    if hyou is None:
        raise SystemExit('Shouri.TOKUSHU_HYOU が読めない')
    atsumeru = set(re.findall(r'\{"([^"]+)"', hyou.group(1)))

    omomi = {1: {}, 2: {}, 3: {}, 4: {}}
    for na, om, ji in re.findall(
            r'new Keihin\("([^"]+)",\s*Material\.\w+,\s*\d+,\s*\d+,\s*(\d+),\s*(\d+),', gacha):
        if na in atsumeru:
            omomi[int(ji)][na] = int(om)
    return atsumeru, omomi


GACHA_MODORI = 0.60        # 金の戻り（値段のおよそ6割。GachaTest が上限を見張っている）


def kuupon(nokori, kakuritsu):
    """残り nokori 種を、1回あたり各 kakuritsu で引き当てるまでの平均回数。"""
    return sum(1.0 / (kakuritsu * k) for k in range(1, nokori + 1))


def tokushu_mitsumori():
    """超特殊勝利（リーダーが集める5種を持つ）までの見積り。

    ★ どの時代で何種 引けるかは【景品表から読む】。ここに書かない。
      「最後の1種が初めて出る時代」に着くまでが、この勝ち筋の壁になる。
    """
    atsumeru, omomi = tokushu_omomi_yomu()
    zen = len(atsumeru)

    print()
    print('=' * 64)
    print('超特殊勝利（リーダーが集める%d種）' % zen)
    print('=' * 64)

    # 各時代で「引ける種類」と「そこまでに集めきれる種類」を出す。
    # ★ 一度 手に入れた物は残るので、集まり具合は時代をまたいだ足し算になる。
    tamaru = set()
    saigo = None            # 5種そろえられる最初の時代
    hatsu = {}              # 時代 → その時代に初めて出るようになる種類
    for jidai in (1, 2, 3, 4):
        ima = set(omomi[jidai])
        hatsu[jidai] = ima - tamaru
        tamaru |= ima
        p_ima = (min(omomi[jidai].values()) / 10000.0) if ima else 0.0
        print('  時代%d: 引ける %d種 / ここまでで %d/%d種 / 各 %.2f%% / 1回 %d円'
              % (jidai, len(ima), len(tamaru), zen, p_ima * 100, GACHA_NEDAN[jidai]))
        if saigo is None and len(tamaru) == zen:
            saigo = jidai

    if saigo is None:
        print()
        print('  ★★ どの時代まで進めても %d種 そろわない（景品表を見直すこと）★★' % zen)
        return

    p_saigo = min(omomi[saigo].values()) / 10000.0
    nokori = len(hatsu[saigo])          # 最後の時代で初めて引けるようになる種類
    kaisuu = kuupon(nokori, p_saigo)    # そこから何回まわすか
    kakari = kaisuu * GACHA_NEDAN[saigo]

    print()
    print('  最後の1種が初めて出るのは【時代%d】。ここへ着くまでが壁。' % saigo)
    print('  時代%d で新しく狙うのは %d種 → 平均 %.0f回 / 代金 %.0f円 / 差し引き %.0f円（戻り6割）'
          % (saigo, nokori, kaisuu, kakari, kakari * (1 - GACHA_MODORI)))

    # 途中の時代で拾っておく分（着いてから慌てないための前払い）
    mae_kane = 0.0
    for jidai in (1, 2, 3, 4):
        if jidai >= saigo or not hatsu[jidai]:
            continue
        p_j = min(omomi[jidai].values()) / 10000.0
        mae_kane += kuupon(len(hatsu[jidai]), p_j) * GACHA_NEDAN[jidai]
    if mae_kane > 0:
        print('  それまでの時代で %d種 を拾っておく代金 %.0f円（差し引き %.0f円）'
              % (zen - nokori, mae_kane, mae_kane * (1 - GACHA_MODORI)))

    print()
    kane_r = kane_bunpun()
    print('  勢力の稼ぎ %.0f円/分 から見ると、最後の代金は %.1f分ぶん。'
          % (kane_r, kakari / kane_r))
    print('  ★ 特殊は【勢力ごとに各1個まで】なので、%d人が同時にまわしてよい。' % SEIRYOKU_NINZU)
    print('    1人あたり %.0f回 = 十連 %.1f回。十連は約5秒なので、実時間は 5〜15分。'
          % (kaisuu / SEIRYOKU_NINZU, kaisuu / SEIRYOKU_NINZU / 10))

    tassei, _k, _c, _o = sim([0.28, 0.24, 0.18, 0.15, 0.15])
    tsuku = tassei.get(saigo)
    gendai = tassei.get(4)
    print()
    if tsuku is None:
        print('  → 時代%d に着けないので、この勝ち筋は成立しない。' % saigo)
        return
    print('  → 時代%d に着くのが %s。そこから 5〜15分。' % (saigo, fun(tsuku)))
    print('    **超特殊勝利の想定 = %d〜%d分（%d時間%d分〜%d時間%d分）**'
          % (tsuku + 5, tsuku + 15,
             (tsuku + 5) // 60, (tsuku + 5) % 60, (tsuku + 15) // 60, (tsuku + 15) % 60))
    sa = (tsuku + 10) - gendai
    if sa < 0:
        print('    （現代到達は %s。超特殊の方が %d分ほど速い）' % (fun(gendai), -sa))
    else:
        print('    （現代到達は %s。超特殊はそれより %d分ほど遅い＝最速の勝ち筋ではない）'
              % (fun(gendai), sa))



tokushu_mitsumori()
