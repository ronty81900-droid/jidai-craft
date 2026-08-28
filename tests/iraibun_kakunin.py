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
for mei, kagi in (("略奪の金", "略奪_金"), ("略奪の石油", "略奪_石油"),
                  ("上限の金", "略奪_上限金"), ("上限の石油", "略奪_上限石油")):
    check("B: %s %d" % (mei, settei(kagi)), str(settei(kagi)) in B, "無い")
check("B: ビーコンは貯金 %d円以下" % teisuu("JidaiCraft.java", "BEACON_KOWASERU"),
      ("%d円以下" % teisuu("JidaiCraft.java", "BEACON_KOWASERU")) in B, "無い")
check("B: 下剋上 %d円" % settei("下剋上_値段"), ("%d円" % settei("下剋上_値段")) in B, "無い")

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
check("送り状: A は12ページ", "12ページ" in OKURI, "無い")
check("送り状: B は22ページ", "22ページ" in OKURI, "無い")
check("送り状: 生成イラストを使うと書いている", "生成イラストを多用" in OKURI, "無い")
check("送り状: 100人と書いている（125人ではない）",
      "100人規模" in OKURI and "125人" not in OKURI, "古い人数が残っている")

print()
print("=" * 60)
print("結果: PASS %d / FAIL %d" % (pass_, fail_))
sys.exit(1 if fail_ else 0)
