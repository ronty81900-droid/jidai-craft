# -*- coding: utf-8 -*-
"""
ui_kakunin.py ── プラグインの見出しと、リソースパックの絵が一致しているかを確かめる

  ★★ なぜ要るか ★★
    GUI の見出しはただの文字列で、リソースパックの側と番号が1つでも
    ずれると「別の画面の絵が出る」「□に化ける」という形で壊れる。
    どちらもコンパイルは通るので、動かすまで気付けない。
    その突き合わせを、起動前に機械でやるのがこの検査。

  見に行く実物:
    resourcepack/codex/packs/*/title_map.json     … Codex の納品
    resourcepack/codex/packs/*/assets/.../*.png    … 絵の実寸
    plugin/src/main/java/jidai/Enshutsu.java       … プラグインの見出し

  動かし方:  python tests/ui_kakunin.py
"""
import io
import json
import re
import struct
import sys
from pathlib import Path


def yomu_png(b):
    """PNG を展開して (幅, 高さ, 1画素の byte 数, 行の並び) を返す。
    ★ 外部の道具を足さないため、自前で展開する。"""
    import zlib
    pos, idat = 8, b""
    w = h = depth = ctype = None
    while pos < len(b):
        ln = struct.unpack(">I", b[pos:pos + 4])[0]
        typ = b[pos + 4:pos + 8]
        if typ == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", b[pos + 8:pos + 18])
        elif typ == b"IDAT":
            idat += b[pos + 8:pos + 8 + ln]
        pos += 12 + ln
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    bpp = ch * depth // 8
    stride = w * bpp
    out, mae = [], bytearray(stride)
    q = 0
    for _ in range(h):
        f = raw[q]
        q += 1
        line = bytearray(raw[q:q + stride])
        q += stride
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            bb = mae[i]
            cc = mae[i - bpp] if i >= bpp else 0
            if f == 1:
                line[i] = (line[i] + a) & 255
            elif f == 2:
                line[i] = (line[i] + bb) & 255
            elif f == 3:
                line[i] = (line[i] + (a + bb) // 2) & 255
            elif f == 4:
                pa, pb, pc = abs(bb - cc), abs(a - cc), abs(a + bb - 2 * cc)
                pr = a if (pa <= pb and pa <= pc) else (bb if pb <= pc else cc)
                line[i] = (line[i] + pr) & 255
        out.append(bytes(line))
        mae = line
    return w, h, ch, out

NE = Path(__file__).resolve().parent.parent
PACKS = NE / "resourcepack" / "codex" / "packs"
ENSHUTSU = NE / "plugin" / "src" / "main" / "java" / "jidai" / "Enshutsu.java"

# 画面の名前 → (プラグインの定数名, 論理サイズ, 段数)
#
# ★★ 2026-08-21 に段数を増やした ★★
#   商品やボタンを1段おきに置き、空いた段（帯）へ
#   リソースパックの絵で「名前と価格」を焼き込むため。
#   高さは 114 + 18 × 段数。
#
# ★ 2026-08-21: 販売所と銃器専門店に【タブ】が付いた。
#   タブごとに見出しの絵が違うので、絵も2枚ずつ要る。
GAMEN = {
    "gacha":   ("UI_GACHA",   (176, 150), 2),
    "shop":    ("UI_MISE",    (176, 222), 6),
    "shop2":   ("UI_MISE_2",  (176, 222), 6),
    "advance": ("UI_SHINKO",  (176, 150), 2),
    "bank":    ("UI_GINKO",   (176, 150), 2),
    "juki":    ("UI_JUKI",    (176, 186), 4),
    "juki2":   ("UI_JUKI_2",  (176, 186), 4),
    "sensen":  ("UI_SENSEN",  (176, 150), 2),
}

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


def main():
    print("=" * 60)
    print("GUI の見出しが、プラグインとリソースパックで一致するか")
    print("=" * 60)

    if not PACKS.exists():
        print("[SKIP] リソースパックがまだ置かれていません: %s" % PACKS)
        return 0

    # --- プラグインの定数を読む ---
    src = io.open(ENSHUTSU, encoding="utf-8").read()
    # ★ Java のソースには、こう書かれている（§ は生の文字）:
    #     private static final String UI_MODOSU = "§f";
    #     public static final String UI_GACHA = UI_MODOSU + "";
    #   Python 側では「バックスラッシュ1個 + u + 4桁」を探す。
    modosu = re.search(r'UI_MODOSU = "§f\\u([0-9a-fA-F]{4})"', src)
    check("プラグインに左へ戻す文字がある", modosu is not None,
          "UI_MODOSU が読めない")
    plug = {}
    for na, (teisu, _, _) in GAMEN.items():
        m = re.search(teisu + r'\s*=\s*UI_MODOSU \+ "\\u([0-9a-fA-F]{4})"', src)
        if m:
            plug[na] = int(m.group(1), 16)
    check("プラグインに8枚ぶんの見出しがある", len(plug) == 8,
          "読めたのは %d 個: %s" % (len(plug), sorted(plug)))

    # --- 案ごとに突き合わせる ---
    an = sorted(p for p in PACKS.iterdir() if p.is_dir())
    check("リソースパックの案が4つある", len(an) == 4, [p.name for p in an])

    for p in an:
        t = json.loads(io.open(p / "title_map.json", encoding="utf-8").read())
        na = p.name

        # 1) 文字の割り当てが同じか
        zure = []
        for gamen, teisu in ((g, v[0]) for g, v in GAMEN.items()):
            pk = t["glyphs"].get(gamen)
            if pk is None:
                zure.append(gamen + "=パック側に無い")
            elif gamen not in plug:
                zure.append(gamen + "=プラグイン側に無い")
            elif ord(pk) != plug[gamen]:
                zure.append("%s パック=%s プラグイン=%s"
                            % (gamen, hex(ord(pk)), hex(plug[gamen])))
        check("★★" + na + ": 8枚すべて文字が一致", not zure, zure)

        # 2) 左へ戻す文字も同じか
        if modosu:
            check(na + ": 左へ戻す文字が一致",
                  ord(t["negative_offset"]) == int(modosu.group(1), 16),
                  "パック=%s プラグイン=%s"
                  % (hex(ord(t["negative_offset"])), "0x" + modosu.group(1)))

        # 3) 絵の実寸が、その画面の段数と合っているか
        #    ★ ここがずれると、絵が縦にはみ出す・足りない
        warui = []
        for gamen, (_, size, _dan) in GAMEN.items():
            f = p / "assets" / "jidai" / "textures" / "gui" / (gamen + ".png")
            if not f.exists():
                warui.append(gamen + "=画像が無い")
                continue
            b = io.open(f, "rb").read(24)
            w, h = struct.unpack(">II", b[16:24])
            if (w, h) != size:
                warui.append("%s=%dx%d (%dx%d のはず)" % (gamen, w, h, size[0], size[1]))
        check("★★" + na + ": 8枚とも実寸が正しい", not warui, warui)

        # 4) 画面全体が塗られているか
        #
        # ★★ ここが「バニラっぽさ」の正体 ★★
        #   チェスト画面の高さ(114 + 18 x 段数)には【持ち物欄も含まれる】。
        #   つまり1枚の絵で画面全体を覆えるのに、下を透明にすると
        #   バニラの灰色パネルがそのまま見える。
        #   （第2回の納品がそうなっていた。指示書の書き方が原因）
        usui = []
        for gamen, (_, size, dan) in GAMEN.items():
            f = p / "assets" / "jidai" / "textures" / "gui" / (gamen + ".png")
            if not f.exists():
                continue
            w, h, ch, rows = yomu_png(f.read_bytes())
            if ch != 4:
                continue
            mochi = 32 + 18 * dan          # 持ち物3段の1行目
            n = t = 0
            for y in range(mochi, min(h, 114 + 18 * dan)):
                r = rows[y]
                for x in range(0, w, 4):
                    t += r[x * ch + 3]
                    n += 1
            fuka = (t / n / 255 * 100) if n else 0
            if fuka < 90:
                usui.append("%s=%.0f%%" % (gamen, fuka))
        check("★★" + na + ": 持ち物欄の背景も塗られている", not usui,
              "透けている: " + str(usui))

        # 5) フォント定義に6つそろっているか
        fj = json.loads(io.open(
            p / "assets" / "minecraft" / "font" / "default.json", encoding="utf-8").read())
        bm = [x for x in fj["providers"] if x["type"] == "bitmap"]
        check(na + ": フォントに8つのグリフがある", len(bm) == 8, "実際=%d" % len(bm))

        # 5.5) 色数（AI が描いた絵を弾く）
        #
        # ★★ ドット絵は数十〜数百色。AI の画像は数千〜数万色になる ★★
        #   前回の納品は 84〜307色（手で描いた／プログラムで描いた範囲）。
        #   ここが桁で増えていたら、作り方が変わったということ。
        ooi = []
        for gamen in GAMEN:
            f = p / "assets" / "jidai" / "textures" / "gui" / (gamen + ".png")
            if not f.exists():
                continue
            w, h, ch, rows = yomu_png(f.read_bytes())
            iro = set()
            for y in range(h):
                r = rows[y]
                for x in range(w):
                    i = x * ch
                    if ch == 4 and r[i + 3] == 0:
                        continue
                    iro.add(bytes(r[i:i + ch]))
            if len(iro) > 1500:
                ooi.append("%s=%d色" % (gamen, len(iro)))
        check("★★" + na + ": 色数が 1500 以下（AI の絵ではない）", not ooi, ooi)

        # 6) サイドバーの行名に空白が無いか
        #    ★ 行名はコマンドの引数になるので、空白が入ると命令が壊れる（実測）
        sb = io.open(p / "SCOREBOARD_TEXT.txt", encoding="utf-8").read()
        kuhaku = [l for l in sb.splitlines() if l.strip() and " " in l]
        check("★★" + na + ": サイドバーの行名に空白が無い", not kuhaku, kuhaku)

    # --- 既定は「絵を使わない」か ---
    # ★ パックを配り終える前に絵を出すと、入れていない人の画面が □ に化ける
    check("★プラグインの既定は「ふつうの文字」",
          "private static boolean uiPack = false;" in src, "既定が true になっている")

    print()
    print("=" * 60)
    print("結果: PASS %d / FAIL %d" % (pass_, fail_))
    return 1 if fail_ else 0


if __name__ == "__main__":
    sys.exit(main())
