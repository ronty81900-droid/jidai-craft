# -*- coding: utf-8 -*-
"""manifest の文字が、枠に収まるかを【全件】測る。

  ★ 「円」を足したせいで、どこか1つでも切れていないかを見る。
    MOD の中の測定画面(SokuteiGamen)と同じ計算を、外から回す。
      枠の幅は画面px。書体は拡大前で測るので、倍率で割って比べる。
    ★ 値段だけでなく【全部の文字】を見る。値段の枠を動かしたせいで
      隣が押し出されていないかも、ここで気付ける。
"""
import io, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ji_haba import haba          # 本物の 1.20.1 の資材で測る

# ★ 置き場は【自分の場所から数える】。ユーザー名を書かない。
#   このファイルは 置き場/clientmod/tests/ にあるので 3つ上。
NE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MANI = os.path.join(NE, "clientmod", "ui", "manifest.json")


def atehameru(s):
    """MOD の Hyou.atehameru と同じ。いちばん長くなる形へ差し替える。"""
    if "{" not in s:
        return s
    import re
    s = re.sub(r"[{]faction_name[^}]*[}]", "内海", s)
    return re.sub(r"[{][^}]*[}]", "999,999", s)


m = json.load(io.open(MANI, encoding="utf-8"))
zenbu = afureta = 0
warui = []
for page, d in m["pages"].items():
    for t in d.get("runtime_text", []):
        s = atehameru(t["content"])
        x, y, w, h = t["rect"]
        bai = t["font_px"] / 8.0
        hitsuyou = round(haba(s) * bai)
        zenbu += 1
        # ★ 品名の欄は2行に折り返してよい（max_lines）。1行の幅で測ると
        #   もともと折り返している物まで「あふれる」と出てしまう。
        #   日本語はどこでも折れるので、要る行数は「幅 ÷ 枠」の切り上げで足りる。
        iru_gyou = max(1, -(-hitsuyou // max(1, w)))
        if iru_gyou > t.get("max_lines", 1):
            afureta += 1
            warui.append((page, t["id"], s, hitsuyou, w))

print("全 %d 件 / あふれる %d 件" % (zenbu, afureta))
for p, i, s, n, w in warui:
    print("  ★あふれる %-14s %-16s %-14s 要 %3d > 枠 %3d" % (p, i, s, n, w))

# 値段だけ、余裕がどれだけ残ったかも出す
print()
print("=== 値段の余裕（枠 − 要る幅）===")
for page in ["shop_life", "shop_armor", "shop_weapon", "gunshop_guns",
             "gunshop_ammo", "gacha"]:
    d = m["pages"][page]
    nokori = []
    for t in d.get("runtime_text", []):
        if t["id"].endswith("_price"):
            s = atehameru(t["content"])
            nokori.append((t["rect"][2] - round(haba(s) * t["font_px"] / 8.0), s))
    if nokori:
        nokori.sort()
        print("  %-14s いちばん狭い %+3dpx (%s)" % (page, nokori[0][0], nokori[0][1]))

# カードからはみ出していないか（値段の枠がカードの中に収まるか）
print()
print("=== 値段の枠がカードの中に収まるか ===")
warui2 = 0
for page, d in m["pages"].items():
    kado = {c["id"]: c["rect"] for c in d.get("cards", [])}
    for t in d.get("runtime_text", []):
        if not t["id"].endswith("_price"):
            continue
        k = kado.get(t["id"][:-6])
        if not k:
            continue
        if t["rect"][0] < k[0] or t["rect"][0] + t["rect"][2] > k[0] + k[2]:
            print("  ★カードから出ている %s/%s 枠%s カード%s"
                  % (page, t["id"], t["rect"], k))
            warui2 += 1
print("  はみ出し %d 件" % warui2)

sys.exit(1 if (afureta or warui2) else 0)
