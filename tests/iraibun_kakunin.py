# -*- coding: utf-8 -*-
"""外注へ送る依頼文2本の数字が、実装と合っているかを見る。

  ★★ なぜ要るか ★★
    依頼文の数字がずれたまま送ると、【間違った説明書が刷られる】。
    PDF が届いてから pdf_kakunin.py で落ちても、刷り直しになる。
    **送る前に** 依頼文そのものを実装と突き合わせる。

  走らせ方: python tests/iraibun_kakunin.py
"""
import io
import os
import re
import sys

NE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DP = os.path.join(NE, "datapacks", "jidai_craft", "data", "jidai", "functions")
SRC = os.path.join(NE, "plugin", "src", "main", "java", "jidai")

pass_ = fail_ = 0


def check(mei, jouken, riyuu=""):
    global pass_, fail_
    if jouken:
        pass_ += 1
        print("[PASS] " + mei)
    else:
        fail_ += 1
        print("[FAIL] " + mei + "  " + str(riyuu))


def yomu(*p):
    return io.open(os.path.join(*p), encoding="utf-8").read()


LOAD = yomu(DP, "load.mcfunction")
A = yomu(NE, "docs", "Codex依頼文_説明書A_遊び方.md")
B = yomu(NE, "docs", "Codex依頼文_説明書B_詳細版.md")
SEIHON = yomu(NE, "docs", "説明書の中身（正本）.md")


def settei(kagi):
    m = re.search(r"scoreboard players set %s settei (-?\d+)" % re.escape(kagi), LOAD)
    assert m, "settei に %s が無い" % kagi
    return int(m.group(1))


def teisuu(f, mei):
    m = re.search(r"%s\s*=\s*(\d+)" % mei, yomu(SRC, f))
    assert m, "%s に %s が無い" % (f, mei)
    return int(m.group(1))


def kanma(n):
    return "{:,}".format(n)


print("=" * 60)
print("外注へ送る依頼文2本を、実装と突き合わせる")
print("=" * 60)

# ============================================================
#  1) 書いてはいけない物が、依頼文に残っていないか
# ============================================================
print()
print("-- 1. 止めた仕組みが残っていないか --")
for na, bun in (("A 遊び方", A), ("B 詳細版", B)):
    for kotoba in ("傭兵", "蛮族", "人望勝利", "/hire", "youheimise", "Uキー", "U キー"):
        check("%s に「%s」が無い" % (na, kotoba), kotoba not in bun,
              "残っている＝無い仕組みを説明書に載せてしまう")

# ============================================================
#  2) 時代進行の条件（4行 × 3つ）
# ============================================================
print()
print("-- 2. 時代を進める条件 --")
# ★★ 「どこかにあればOK」では捕まらない（実測で分かった）★★
#   建材 2,400 は勝ち方の表にも出るので、進行の表を 2,000 に壊しても
#   「文書のどこかにある」で通ってしまった。**表を行ごとに読む。**
TSUGI = {"鉄器": "中世", "中世": "近代", "近代": "現代", "現代": "未来"}
for mei in ("鉄器", "中世", "近代", "現代"):
    # 「| 鉄器 | 中世 | … |」の行を、表の中から1本だけ取る
    gyou = [g for g in B.split("\n")

            if g.count("|") == 6 and g.strip().startswith("|")
            and g.split("|")[1].strip().replace("*", "") == mei
            and "本" in g and "個" in g]
    if len(gyou) != 1:
        check("B: %s の進行の行が1本ある" % mei, False, "%d 本 見つかった" % len(gyou))
        continue
    hako = [x.strip().replace("*", "") for x in gyou[0].strip("|").split("|")]
    for i, (shu, kagi) in enumerate((("石油", "進行_石油_"), ("貯金", "進行_貯金_"),
                                     ("建築", "進行_建築_")), start=2):
        n = settei(kagi + mei)
        kaita = hako[i].replace(",", "").replace("本", "").replace("円", "").replace("個", "").strip()
        check("B: %s → %s の%s = %s" % (mei, TSUGI[mei], shu, kanma(n)),
              kaita == str(n), "表には %s と書いてある" % hako[i])

# ============================================================
#  3) 勝ち方は4つ・未来の条件
# ============================================================
print()
print("-- 3. 勝ち方 --")
check("A: 勝ち方は4つと書いている", "勝ち方は4つ" in A, "無い")
check("B: 勝ち方は4つと書いている", "勝ち方は4つ" in B, "無い")
check("A: 未来到達が本命だと書いている", "本命" in A and "未来" in A, "無い")
check("B: 経済勝利 %s" % kanma(settei("経済_勝利_貯金")),
      kanma(settei("経済_勝利_貯金")) in B, "無い")
check("A: 経済勝利 %s" % kanma(settei("経済_勝利_貯金")),
      kanma(settei("経済_勝利_貯金")) in A, "無い")

# ============================================================
#  4) 戦争の数字
# ============================================================
print()
print("-- 4. 戦争 --")
for mei, kagi, tan in (("準備", "戦争_準備秒", 60), ("交戦", "戦争_交戦秒", 60),
                       ("再戦禁止", "戦争_禁止秒", 60)):
    n = settei(kagi) // tan
    check("B: %s %d分" % (mei, n), ("**%d分**" % n) in B or ("%d分" % n) in B, "無い")
# ★ 2026-09-09: 1回いくらの固定額と上限は廃止。相手の総量の 1% を奪う形になった。
#   「1%」は 略奪_割る数(100) から出す。文書に数字を写さず、実装から作って照合する。
wari = 100 // settei("略奪_割る数")
check("B: 略奪は 1回 %d%%" % wari, ("**%d%% ずつ**" % wari) in B, "無い")
check("B: ビーコンは銀行を %d回" % settei("占領_必要回数"),
      ("**%d回**" % settei("占領_必要回数")) in B, "無い")
check("B: 下剋上 %d円" % settei("下剋上_値段"), ("%d円" % settei("下剋上_値段")) in B, "無い")

# ── A 側の戦争 ─────────────────────────────────────────
#   ★ A は「1ページ 60文字以内」なので、本文ではなく
#     【4コマのキャプション】が規則を運んでいる。そこを見る。
check("A: 略奪は「壊して」と書いている（押して ではない）",
      "金ブロックを**壊して**" in A, "無い＝古い動作のまま絵が描かれる")
check("A: 略奪は 1回 %d%%" % wari, ("**%d%% ずつ**" % wari) in A, "無い")
check("A: ビーコンは %d回" % settei("占領_必要回数"),
      ("**%d回 壊すと**" % settei("占領_必要回数")) in A, "無い")

# ── 銀行の守り（2026-09-18 のご指示）──────────────────
#   ★ 参加者が必ずぶつかる。説明書に無いと当日の問い合わせになる。
mamoru = teisuu("JidaiCraft.java", "GINKO_MAMORU")
modoru = teisuu("JidaiCraft.java", "GINKO_MODORU") // 20       # 20tick = 1秒
for na, bun in (("A", A), ("B", B)):
    check("%s: 銀行は %d秒で戻ると書いている" % (na, modoru),
          ("%d秒で" % modoru) in bun, "無い")
    check("%s: まわり %dマスは掘れない・置けないと書いている" % (na, mamoru),
          ("まわり%dマス" % mamoru) in bun or ("まわり %dマス" % mamoru) in bun, "無い")

# ── ★★ 古い規則が【残っていない】か ★★ ────────────────
#   ★ これが無かったせいで、B に新旧の規則が両方 並んだまま
#     133 PASS で通っていた。「ある」だけ見ても同居は捕まらない。
for na, bun in (("A", A), ("B", B)):
    for furui, riyuu in (
            ("金ブロックを押して、金と石油", "2026-09-09 より前の略奪（押す）"),
            ("100円以下", "2026-09-09 より前のビーコン条件（貯金が尽きたら）"),
            ("石油2本", "2026-09-09 より前の固定額"),
            ("上限 300", "2026-09-09 に廃止した上限"),
    ):
        check("%s: 古い「%s」が残っていない" % (na, furui),
              furui not in bun, riyuu)

# ============================================================
#  5) 同士討ちと奪う割合
# ============================================================
print()
print("-- 5. 同士討ち --")
ubau = teisuu("Tatakai.java", "UBAU_WARIAI")
for na, bun in (("A", A), ("B", B)):
    check("%s: 仲間は傷つけられないと書いている" % na,
          "傷つけられません" in bun or "傷つけられません" in bun, "無い")
    check("%s: 奪う割合 %d%%" % (na, ubau), ("%d%%" % ubau) in bun, "無い")
check("B: 勢力の貯金からは取らないと書いている", "勢力の貯金からは取れません" in B, "無い")

# ============================================================
#  6) 店とガチャ
# ============================================================
print()
print("-- 6. 店とガチャ --")
juki = teisuu("Shop.java", "JUKI_KAIHOU")
for na, bun in (("A", A), ("B", B)):
    check("%s: 銃器専門店は %s" % (na, kanma(juki)), kanma(juki) in bun, "無い")
check("B: 石油の売値 %d円" % teisuu("Ginko.java", "SEKIYU_NEDAN"),
      ("%d円" % teisuu("Ginko.java", "SEKIYU_NEDAN")) in B, "無い")
check("B: バブルは %d倍" % teisuu("Ginko.java", "BUBBLE_BAI"),
      ("%d倍" % teisuu("Ginko.java", "BUBBLE_BAI")) in B, "無い")
m = re.search(r"NEDAN = \{([^}]+)\}", yomu(SRC, "Gacha.java"))
atai = [x.strip() for x in m.group(1).split(",") if x.strip()]
for n, mei in ((1, "鉄器"), (2, "中世"), (3, "近代"), (4, "現代")):
    check("B: ガチャ %s %s円" % (mei, atai[n]), ("%s円" % atai[n]) in B, "無い")

# ============================================================
#  6b) Codex の監査（2026-09-20）で見つかった食い違い
#      ★ どれも「サーバーは正しく動くが、紙に嘘が刷られる」形。
#        実機で遊んでも気づけないので、ここで止める。
# ============================================================
print()
print("-- 6b. 監査で見つかった食い違い --")

# ── #07 銃と弾の件数 ──────────────────────────────────
#   ★ 2026-08-30 に 9丁6種 → 5丁1種 へ整理したのに、
#     資料の見出しと依頼文だけ 9件/6件 のまま残り、**説明書に刷られた**。
SHOP = yomu(SRC, "Shop.java")
_j = re.search(r"JUKIHIN\s*=\s*\{(.*?)\n    \};", SHOP, re.S).group(1)
_i = _j.index("弾1種") if "弾1種" in _j else len(_j)
JYU = len(re.findall(r"new Shohin\(", _j[:_i]))
TAMA = len(re.findall(r"new Shohin\(", _j[_i:]))
SHIRYOU = yomu(NE, "docs", "資料_商品一覧.md")
check("資料: 銃が %d丁 と書いている" % JYU,
      ("タブ1「銃」（%d 件）" % JYU) in SHIRYOU, "実装と食い違う＝説明書に古い件数が刷られる")
check("資料: 弾が %d種 と書いている" % TAMA,
      ("タブ2「弾」（%d 件）" % TAMA) in SHIRYOU, "同上")
check("資料: JUKIHIN の合計が %d 件" % (JYU + TAMA),
      ("**JUKIHIN は %d 件**" % (JYU + TAMA)) in SHIRYOU, "同上")
check("B: 表の見出しが 銃%d丁・弾%d種" % (JYU, TAMA),
      ('「銃%d丁」「弾%d種」' % (JYU, TAMA)) in B, "実装と食い違う")

# ── #06 遺物は略奪で奪われない ────────────────────────
#   ★ Shouri.atsumeruNoNamae は集める5種だけを拾い、遺物は continue する。
#     「略奪で奪われても効果は残ります」は【奪われる】と読めてしまう。
SHOURI_SRC = yomu(SRC, "Shouri.java")
check("実装: 奪うのは集める5種だけ（遺物は対象外）",
      "遺物や、ただの名前付きの紙" in SHOURI_SRC,
      "実装が変わった。文書の書き方も見直すこと")
check("B: 遺物は略奪で奪われないと書いている",
      "遺物は略奪では奪われません" in B, "無い＝奪われると誤解される")
check("B: 古い「略奪で奪われても効果は残ります」が残っていない",
      "略奪で奪われても効果は残ります" not in B, "残っている")

# ── #09 ツルハシは売っていない（全員に配る）────────────
check("実装: ツルハシは配る（売っていない）",
      "全員に1本ずつ配る" in SHOP, "実装が変わった")
for na, bun in (("A", A), ("B", B)):
    check("%s: ツルハシを「買う物」に挙げていない" % na,
          "・ツルハシ" not in bun and "ツルハシ |" not in bun,
          "販売所に無いので、参加者が探してしまう")

# ============================================================
#  7) 特殊5種と遺物11種の名前
# ============================================================
print()
print("-- 7. 特殊と遺物の名前 --")
SH = yomu(SRC, "Shouri.java")
tokushu = re.findall(r'\{"([^"]+)",\s*"\w+",\s*"82\d\d"\}', SH)
check("実装から特殊5種を読めた", len(tokushu) == 5, tokushu)
for mei in tokushu:
    check("B: 特殊「%s」が載っている" % mei, mei in B, "無い")
ibutsu = re.findall(r'\{"([^"]+)",\s*"\w+",\s*"\d+",\s*"\d",\s*"[A-Z_]+",\s*"([^"]+)"\}', SH)
check("実装から遺物11種を読めた", len(ibutsu) == 11, len(ibutsu))
for mei, kouka in ibutsu:
    check("B: 遺物「%s」が載っている" % mei, mei in B, "無い")

# ============================================================
#  8) ページ数の取り決め
# ============================================================
print()
print("-- 8. ページ数 --")
check("A: 12ページと書いている", "**12ページ**" in A, "無い")
check("B: 13ページと書いている", "**13ページ**" in B, "無い")
check("A: 生成イラストを使うと書いている", "生成イラストを多用" in A, "無い")
check("B: A の絵を流用すると書いている", "流用" in B, "無い")

# ── 絵の番号の受け渡し ──
#   ★ B は「絵: e07」と番号で指す。A がその番号を定めていないと、
#     外注はどの絵が e07 か分からない（実際に1度 抜けていた）。
a_e = set(re.findall(r"e(\d\d)", A))
b_e = set(re.findall(r"e(\d\d)", B))
seihon_e = set("%02d" % int(x) for x in re.findall(r"^\| (\d+) \| \*\*", SEIHON, re.M))
check("A が絵の番号を %d 枚 定めている" % len(seihon_e), a_e == seihon_e,
      "A=%s / 正本=%s" % (sorted(a_e), sorted(seihon_e)))
tarinai = sorted(b_e - a_e)
check("B が使う絵を A が全部 作る", not tarinai,
      "A が作らない: %s" % ["e" + x for x in tarinai])

# ★★ 正本の「詳」欄（○）が、依頼文B と一致しているか ★★
#   ○ は依頼文B から作っている（setsumei_seihon.py）。だからここが落ちるのは
#   **正本が手で直された／生成器を走らせ忘れた**時。
#   2026-09-09 に実際それが起きて、正本だけ手で直された跡が残っていた。
maru = set("%02d" % int(n) for n, sho in
           re.findall(r"^\| (\d+) \| \*\*.+?\| ([○－]) \|$", SEIHON, re.M) if sho == "○")
tsukawanai = sorted(maru - b_e)
check("正本の「詳」欄 %d 枚が依頼文B と一致（正本を手で直すとここが落ちる）" % len(maru),
      not tsukawanai, "B が使っていない: %s" % ["e" + x for x in tsukawanai])

# ── 送り状（そのまま貼る本文）も、同じ物差しで見る ──
OKURI = yomu(NE, "docs", "Codex送信用_依頼本文.md")
# ★ 「書いてはいけないこと」の節には、わざと止めた仕組みの名前が並ぶ。
#   そこを除いてから探す（除かないと、正しい文書が落ちる）。
MARK = "## ★ 書いてはいけないこと"
check("送り状に「書いてはいけないこと」の節がある", MARK in OKURI, "無い")
HONBUN = OKURI
if MARK in OKURI:
    i = OKURI.index(MARK)
    j = OKURI.index("\n## ", i + 1)
    HONBUN = OKURI[:i] + OKURI[j:]
for kotoba in ("傭兵 25人", "人望勝利", "/hire", "youheimise"):
    check("送り状の本文に「%s」が無い" % kotoba, kotoba not in HONBUN, "残っている")
# ★★ 数を写さない ★★
#   ここは長らく「B は22ページ」と書いてあり、依頼文B は 13 だった。
#   **検査が古い方を守っていた**ので、直すと落ちる状態になっていた。
#   依頼文から読んで突き合わせる形にすれば、二度と食い違わない。
def iraibun_page(bun, mei):
    m = re.search(r"\*\*(\d+)ページ\*\*", bun)
    assert m, "%s にページ数が無い" % mei
    return int(m.group(1))


for mei, bun in (("A", A), ("B", B)):
    n = iraibun_page(bun, mei)
    check("送り状: %s は%dページ（依頼文と同じ）" % (mei, n),
          ("%dページ" % n) in OKURI, "食い違い＝どちらが正本か分からなくなる")

# ★★ 「ある」だけでは足りない ★★
#   3か所あるうち1か所だけ古い数に戻しても、上の検査は緑のままだった
#   （わざと壊して発覚）。**古い数が残っていない**ことまで見る。
#   ★ 10 未満は地の文（「表が1ページに収まらない」）なので数えない。
tadashii = {iraibun_page(A, "A"), iraibun_page(B, "B")}
nokori = sorted({int(x) for x in re.findall(r"(\d+)ページ", OKURI)} - tadashii - set(range(10)))
check("送り状に古いページ数が残っていない", not nokori,
      "残っている: %s（正しいのは %s）" % (nokori, sorted(tadashii)))
check("送り状: 生成イラストを使うと書いている", "生成イラストを多用" in OKURI, "無い")
check("送り状: 100人と書いている（125人ではない）",
      "100人規模" in OKURI and "125人" not in OKURI, "古い人数が残っている")

# ============================================================
#  8b) 作り直し依頼（Codex へ送る送り状）
# ============================================================
print()
print("-- 8b. 作り直し依頼 --")
SASHIKAE = yomu(NE, "docs", "Codex送信用_説明書の差し替え.md")

# ★ 実装から拾った値が、そのまま書いてあるか。
for mei, moji in (
        ("略奪の %d%%" % wari, "%d%% ずつ" % wari),
        ("ビーコンの %d回" % settei("占領_必要回数"), "%d回 壊すと" % settei("占領_必要回数")),
        ("銀行が %d秒で戻る" % modoru, "%d秒で元に戻ります" % modoru),
        ("まわり %dマス" % mamoru, "まわり %dマス" % mamoru),
):
    check("送り状: %s が書いてある" % mei, moji in SASHIKAE, "無い")

# ★ ページ数は依頼文から。ここでも写さない。
#   ★★ 「どこかにあれば OK」では駄目 ★★
#     この文書は「24ページ → 13ページ に変えた」と経緯も書くので、
#     古い数がわざと入っている。**納品の形を書いた行**を名指しで見る
#     （そこだけ古いままにしても緑になった。わざと壊して発覚）。
for mei, bun, mukuri in (("A", A, "A4横"), ("B", B, "A4縦")):
    n = iraibun_page(bun, mei)
    check("送り状: 納品の形が %s %dページ" % (mukuri, n),
          ("%s・%dページ" % (mukuri, n)) in SASHIKAE,
          "無い＝作らせる冊子のページ数が依頼文と食い違う")

# ★ 新しく頼む絵の番号が、正本と食い違っていないか。
#   A が e25 を作る約束なので、送り状も e25 と呼んでいないと混乱する。
check("送り状: 新しく頼む絵を e25 と呼んでいる", "e25" in SASHIKAE,
      "無い＝依頼文A との呼び名が食い違う")

# ★ 画風の6色は、前回の納品の manifest が正本。写し間違えると絵柄が変わる。
IRO = ("#102329", "#F5E8C8", "#7D8889", "#C4934B", "#4FA1B2", "#3C6A55")
check("送り状: 前回の6色をすべて書いている",
      all(i in SASHIKAE for i in IRO),
      "足りない: %s" % [i for i in IRO if i not in SASHIKAE])

# ★ 「文章はこちらで直す」と明記しているか。
#   ここが抜けると、外注が善意で文章を書き換え、実装と食い違う。
check("送り状: 文章は外注に書き換えさせないと明記している",
      "書き換えないでください" in SASHIKAE, "無い")

# ── ボスバーの絵の指示文（Fable へ渡す物）──────────────────
#   ★ 説明書と同じ理由でここを見る。寸法や色の対応がずれたまま渡すと、
#     描き直しになる。実装（データパック・manifest）が正本。
import hashlib
import json

FB = yomu(NE, "docs", "Fable依頼文_ボスバー.md")
BAR = yomu(DP, "shinko", "bar.mcfunction")
MANI = json.load(io.open(os.path.join(NE, "clientmod", "ui", "manifest.json"),
                        encoding="utf-8"))

# 名前（jidai:chuo）が実装と合っているか
bar_id = re.search(r"bossbar add (\S+) ", LOAD)
check("指示文のボスバー名が load と同じ",
      bar_id and bar_id.group(1) in FB,
      "load=%s" % (bar_id.group(1) if bar_id else "見つからない"))

# 満タンと段数
mx = re.search(r"bossbar set \S+ max (\d+)", LOAD)
man = int(mx.group(1)) if mx else -1
check("指示文の満タンが load と同じ（%d）" % man,
      ("満タンは `%d`" % man) in FB, "load=%d" % man)
check("指示文の段数が 満タン+1（%d段階）" % (man + 1),
      ("**動きは%d段階**" % (man + 1)) in FB, "load=%d" % man)

# 寸法が算数として合っているか（Codex版はここが合っていなかった）
wk = re.search(r"\*\*(\d+) × (\d+) px\*\*", FB)
nk = re.search(r"溝」が (\d+) × (\d+) px。\*\*上下左右すべて (\d+)px", FB)
check("指示文に枠と溝の寸法がある", wk and nk, "%s / %s" % (wk, nk))
if wk and nk:
    fw, fh = int(wk.group(1)), int(wk.group(2))
    iw, ih, en = int(nk.group(1)), int(nk.group(2)), int(nk.group(3))
    check("★枠の横 = 溝の横 + 縁×2", fw == iw + en * 2,
          "%d != %d + %d*2" % (fw, iw, en))
    check("★枠の縦 = 溝の縦 + 縁×2", fh == ih + en * 2,
          "%d != %d + %d*2" % (fh, ih, en))
    check("枠の横がバニラ(182)の4倍", fw == 182 * 4, "%d" % fw)
    check("★溝の横が段数で割り切れる", man > 0 and iw % man == 0,
          "%d %% %d" % (iw, man))
    kiru = " / ".join(str(iw // man * i) for i in range(man + 1))
    check("指示文の切る位置が 溝÷段数 と合う（%s）" % kiru,
          kiru in FB, "計算では %s" % kiru)

# 時代と色の対応。bar.mcfunction の color 行と name 行を matches 式で結ぶ
iro = dict((m.group(1).strip(), m.group(2)) for m in re.finditer(
    r"chuo matches (\S+) run bossbar set \S+ color (\w+)", BAR))
mei = dict((m.group(1).strip(), m.group(2)) for m in re.finditer(
    r"chuo matches (\S+) run bossbar set \S+ name .*?世界の時代.(\w+?).,", BAR))
check("bar.mcfunction に時代が4つある", len(iro) == 4 and len(mei) == 4,
      "色=%d 名前=%d" % (len(iro), len(mei)))
hyou = dict((m.group(2), (m.group(1), m.group(3), m.group(4))) for m in re.finditer(
    r"\| `bar_fill_(\w+)\.png` \| (\S+) \| `(\w+)` \|[^|]*\| `(#[0-9A-Fa-f]{6})` \|", FB))
check("指示文の表に中身が4枚ある", len(hyou) == 4, "%d 枚" % len(hyou))
for shiki in sorted(iro):
    jidai_mei = mei.get(shiki)
    check("★時代『%s』の色が実装と同じ（%s）" % (jidai_mei, iro[shiki]),
          jidai_mei in hyou and hyou[jidai_mei][1] == iro[shiki],
          "実装=%s / 指示文=%s" % (iro[shiki],
                                 hyou.get(jidai_mei, ("?", "?", "?"))[1]))

# 色が manifest のパレットにあるか（新色だけは例外と、指示文が名指ししている）
PAL = set(v.upper() for v in MANI["palette"].values())
for _f, (_e, _c, hexiro) in sorted(hyou.items()):
    if hexiro.upper() in PAL:
        check("指示文の色 %s は palette にある" % hexiro, True)
    else:
        # ★ 断り書きは同じ行に無いと意味が無い。文書のどこかに両方あれば通る、
        #   では別の話題の行を拾ってしまう。行ごとに見る。
        dan = [ln for ln in FB.splitlines()
               if hexiro in ln and "既存パレットに無い新色" in ln]
        check("★palette に無い色 %s を『新色』と断ってある" % hexiro,
              len(dan) == 1, "その断り書きの行が %d 本" % len(dan))

# どの版の Codex 依頼文から書き直したかが合っているか
moto = io.open(os.path.join(NE, "docs", "Codex依頼文_ボスバー.md"), "rb").read()
check("指示文が引いている Codex版の sha256 が実物と合う",
      hashlib.sha256(moto).hexdigest() in FB, hashlib.sha256(moto).hexdigest())

# ── 特殊アイテム16種の絵の指示文（Fable へ渡す物）──────────
#   ★ 指示文の表は Shouri.java の写し。写しは黙って古くなるので突き合わせる。
TK = yomu(NE, "docs", "Fable依頼文_特殊アイテム.md")
SHOURI = yomu(SRC, "Shouri.java")

# 正本（Shouri.java）。集める5種は行末の注記から時代を取る
JIDAI_MEI = {"1": "鉄器", "2": "中世", "3": "近代", "4": "現代"}
seihon = {}
for m in re.finditer(r'\{"([^"]+)",\s*"(\w+)",\s*"(\d+)"\},\s*// (\S+)',
                     SHOURI):
    seihon[m.group(2)] = (m.group(1), m.group(3), m.group(4), 512)
for m in re.finditer(r'\{"([^"]+)",\s*"(\w+)",\s*"(\d+)",\s*"(\d)",',
                     SHOURI):
    seihon[m.group(2)] = (m.group(1), m.group(3),
                          JIDAI_MEI[m.group(4)], 256)
check("Shouri.java から16件 読める（集める5＋遺物11）", len(seihon) == 16,
      "%d 件" % len(seihon))

# 指示文の表
hyou = {}
for m in re.finditer(r"\| `(\w+)\.png` \| (\S+) \| (\d+) \| (\d+) \| (\S+?) \|", TK):
    hyou[m.group(1)] = (m.group(2), m.group(3), m.group(5), int(m.group(4)))
check("指示文の表も16件", len(hyou) == 16, "%d 件" % len(hyou))

for fai in sorted(seihon):
    mei, ban, jidai, ookisa = seihon[fai]
    check("★%s: 名前・番号・時代・大きさが正本と同じ" % fai,
          hyou.get(fai) == (mei, ban, jidai, ookisa),
          "正本=%s / 指示文=%s" % ((mei, ban, jidai, ookisa), hyou.get(fai)))

# 大きさの内訳（勝利5枚が512、遺物11枚が256）
check("512 が5枚・256 が11枚",
      sum(1 for v in hyou.values() if v[3] == 512) == 5
      and sum(1 for v in hyou.values() if v[3] == 256) == 11,
      str(sorted(v[3] for v in hyou.values())))

# §2 の算数（縮めた時に何画素拾うか）
for n in (512, 256):
    hiku = "%s を 16 に nearest で縮めると、%s 画素のうち 256 個（%.2f%%）" % (
        n, format(n * n, ","), 256.0 / (n * n) * 100)
    check("指示文の縮小の算数が合う（%d）" % n, hiku in TK, "計算では %s" % hiku)

# §2 が実測値として書いた、前回の絵の縮み方
def nearest16(px, w):
    """nearest で 16×16 に縮めた画素を返す。★ Minecraft の GL_NEAREST と
    同じ拾い方（出力画素の中心を写した所）にそろえてある。"""
    k = w // 16
    return [px[(y * k + k // 2) * w + (x * k + k // 2)]
            for y in range(16) for x in range(16)]

try:
    from PIL import Image
    ep = os.path.join(NE, "clientmod", "nouhin", "v6", "assets", "jidaiui",
                      "textures", "item", "excalibur.png")
    im = Image.open(ep).convert("RGBA")
    px = list(im.get_flattened_data() if hasattr(im, "get_flattened_data")
              else im.getdata())
    w = im.size[0]
    sm = nearest16(px, w)
    moto_men = round(sum(1 for p in px if p[3]) * 100.0 / len(px))
    sm_men = round(sum(1 for p in sm if p[3]) * 100.0 / len(sm))
    check("指示文の『面積 %d%%→%d%%』が実物と合う" % (moto_men, sm_men),
          ("面積 %d%%→%d%%" % (moto_men, sm_men)) in TK,
          "実測は %d%%→%d%%" % (moto_men, sm_men))
    check("指示文の『色数 %d→%d』が実物と合う" % (len(set(px)), len(set(sm))),
          ("色数 %d→%d" % (len(set(px)), len(set(sm)))) in TK,
          "実測は %d→%d" % (len(set(px)), len(set(sm)))) 
except ImportError:
    print("[SKIP] Pillow が無いので前回の絵の実測は見ない")

# v6 の共有パレットの色数
V6 = yomu(NE, "clientmod", "nouhin", "v6", "DESIGN_SPEC.md")
setsu = re.search(r"## 共有パレット(.*?)\n## ", V6, re.S)
pal6 = set(re.findall(r"\| `(#[0-9A-Fa-f]{8})` \|", setsu.group(1))) if setsu else set()
check("指示文の『共有パレット%d色』が v6 と合う" % len(pal6),
      ("共有パレット%d色" % len(pal6)) in TK, "v6 は %d 色" % len(pal6))

# 指示文が寄りかかっている仕組みが、本当にその名前で在るか
KAZARI = yomu(NE, "clientmod", "src", "main", "java", "jidai", "ui", "Kazari.java")
check("MOD が 16×16 に縮めて貼っている（指示文§2の前提）",
      "haruOokisa(g, e, x, y, 16, 16" in KAZARI, "呼び方が変わっている")
MODERU = yomu(NE, "clientmod", "tools", "tokushu_moderu.py")
for na in ("NOUHIN", "NOUHIN_IBUTSU"):
    check("tokushu_moderu.py に %s がある（指示文§7が指す物）" % na,
          re.search(r"^%s = " % na, MODERU, re.M) is not None, "無い")
check("tokushu_moderu.py が PNG の実寸から OOKISA を作る（指示文§7の前提）",
      "OOKISA" in MODERU and "IHDR" in MODERU, "作り方が変わっている")

# 今の大きさ（指示文が「今の大きさ」欄に書いた値）
try:
    from PIL import Image as _I
    ima = os.path.join(NE, "clientmod", "nouhin", "v5", "assets", "jidaiui",
                       "textures", "item", "gofu.png")
    imb = os.path.join(NE, "clientmod", "nouhin", "v6", "assets", "jidaiui",
                       "textures", "item", "sensha.png")
    a, b = _I.open(ima).size[0], _I.open(imb).size[0]
    check("指示文の「今の大きさ」が実物と合う（%d／%d）" % (a, b),
          ("| %d × %d |" % (a, a)) in TK and ("| %d × %d |" % (b, b)) in TK,
          "実測 %d と %d" % (a, b))
except ImportError:
    pass

# どの版の指示で作らせるか
check("指示文が自分の sha256 を書かせている",
      "Fable依頼文_特殊アイテム.md" in TK and "sha256" in TK, "無い")

print()
print("=" * 60)
print("結果: PASS %d / FAIL %d" % (pass_, fail_))
sys.exit(1 if fail_ else 0)
