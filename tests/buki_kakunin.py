# -*- coding: utf-8 -*-
"""
buki_kakunin.py ── 販売所に書いた銃と弾の ID が、実物の MOD に在るかを確かめる

  ★★ なぜ要るか ★★
    Shop.java に書く GunId / AmmoId はただの文字列で、綴りを1文字
    間違えてもコンパイルは通る。実機で「give が黙って失敗し、
    買えたつもりで何も来ない」という形で初めて分かる。
    それを起動前に捕まえるのがこの検査。

  見に行く実物:
    jikki_mod/mods/tacz-*.jar                      … TaCZ 本体の既定パック
    jikki_mod/tacz/GunpowderRevolution_*.zip        … 銃パック(第一次大戦)

  動かし方:  python tests/buki_kakunin.py
"""
import io
import re
import sys
import zipfile
from pathlib import Path

NE = Path(__file__).resolve().parent.parent
MOD = NE / "jikki_mod"
SHOP = NE / "plugin" / "src" / "main" / "java" / "jidai" / "Shop.java"

pass_ = 0
fail_ = 0


def check(namae, jouken, shousai=""):
    global pass_, fail_
    if jouken:
        pass_ += 1
        print("[PASS] " + namae)
    else:
        fail_ += 1
        print("[FAIL] " + namae + "  " + str(shousai))


def hitotsu(oya, katachi):
    """その形に合うファイルを1つ返す。無ければ None。"""
    machi = sorted(oya.glob(katachi))
    return machi[0] if machi else None


def naka(zpath):
    """zip の中の「登録されている名前」を集める。

    ★ index/guns/○○.json と index/ammo/○○.json が登録簿。
      名前空間(hamster / tacz)は data/<名前空間>/ の部分から取る。
    """
    juu, tama = set(), set()
    with zipfile.ZipFile(zpath) as z:
        for n in z.namelist():
            # ★ TaCZ 本体の既定パックは jar の中で
            #   assets/tacz/custom/tacz_default_gun/data/... と入れ子になっている。
            #   頭を固定すると本体側が1丁も見つからない(実際に外して気付いた)。
            m = re.search(r"(?:^|/)data/([^/]+)/index/(guns|ammo)/([^/]+)\.json$", n)
            if not m:
                continue
            ns, shu, mei = m.groups()
            (juu if shu == "guns" else tama).add(ns + ":" + mei)
    return juu, tama


def main():
    print("=" * 60)
    print("販売所の銃と弾の ID を、実物の MOD と突き合わせる")
    print("=" * 60)

    jar = hitotsu(MOD / "mods", "tacz-*.jar")
    pack = hitotsu(MOD / "tacz", "*naoshi*.zip")
    check("TaCZ 本体の jar がある", jar is not None, MOD / "mods")
    check("銃パックの zip がある", pack is not None, MOD / "tacz")
    if jar is None or pack is None:
        print("\n結果: PASS %d / FAIL %d" % (pass_, fail_))
        return 1

    juu, tama = set(), set()
    for z in (jar, pack):
        a, b = naka(z)
        juu |= a
        tama |= b
    print("  実物にある銃: %d 丁 / 弾: %d 種" % (len(juu), len(tama)))

    src = io.open(SHOP, encoding="utf-8").read()

    # ★ 銃器専門店の棚だけを切り出して数える。
    #   2026-08-26 に傭兵の店が無くなったので、店は1つだけになった。
    def tana(mei):
        i = src.index(mei + " = {")
        return src[i:src.index("\n    };", i)]

    juki_src = tana("Shohin[] JUKIHIN")

    def hirou(bun, kagi):
        return re.findall(kagi + r':\\"([^"\\]+)\\"', bun)

    juki_juu = hirou(juki_src, "GunId")
    juki_tama = hirou(juki_src, "AmmoId")

    kaku_juu = juki_juu
    kaku_tama = juki_tama

    check("銃器専門店に銃が 9 丁書いてある", len(juki_juu) == 9, juki_juu)
    check("銃器専門店に弾が 6 種書いてある", len(juki_tama) == 6, juki_tama)

    # --- 1丁ずつ、実物に在るかを見る -------------------------------
    for g in kaku_juu:
        check("銃 " + g + " が実物にある", g in juu, "MOD側の登録簿に無い")
    for t in kaku_tama:
        check("弾 " + t + " が実物にある", t in tama, "MOD側の登録簿に無い")

    # --- 同じ物を二重に並べていないか ------------------------------
    check("銃器専門店で同じ銃を2枠に置いていない",
          len(set(juki_juu)) == len(juki_juu), juki_juu)
    check("銃器専門店で同じ弾を2枠に置いていない",
          len(set(juki_tama)) == len(juki_tama), juki_tama)

    # --- 銃が使う弾が、ちゃんと売られているか ----------------------
    # ★ 実物の _data ファイルから「この銃はどの弾を使うか」を読む。
    #   Shop.java のコメントを読むのではない。実装が正しいかを実物で見る。
    iru = {}
    for z in (jar, pack):
        with zipfile.ZipFile(z) as zz:
            for n in zz.namelist():
                m = re.search(r"(?:^|/)data/([^/]+)/data/guns/([^/]+)\.json$", n)
                if not m:
                    continue
                ns, mei = m.groups()
                mei = ns + ":" + mei.replace("_data", "")
                if mei not in kaku_juu:
                    continue
                s = zz.read(n).decode("utf-8")
                a = re.search(r'"ammo"\s*:\s*"([^"]+)"', s)
                if a:
                    iru[mei] = a.group(1)

    check("売っている9丁すべての「使う弾」を実物から読めた",
          len(iru) == len(kaku_juu), "読めたのは %d 丁: %s" % (len(iru), sorted(iru)))
    tarinai = sorted(v for v in iru.values() if v not in kaku_tama)
    check("★★売っている銃が使う弾は、すべて販売所にある",
          not tarinai, "売られていない弾=" + str(tarinai))

    # --- 逆に、どの銃も使わない弾を売っていないか ------------------
    amari = sorted(t for t in kaku_tama if t not in set(iru.values()))
    check("どの銃も使わない弾を売っていない", not amari, "余っている=" + str(amari))

    print()
    print("=" * 60)
    print("結果: PASS %d / FAIL %d" % (pass_, fail_))
    return 1 if fail_ else 0


if __name__ == "__main__":
    sys.exit(main())
