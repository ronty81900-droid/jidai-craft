# -*- coding: utf-8 -*-
"""本物の Minecraft 1.20.1 から、字の横幅(px)を測る。

  ★ 「たぶん9px」で値段の枠を決めると、桁が増えた時だけ本番で切れる。
    Font#width と同じ計算を、本物の資材(ascii.png / unifont)から作る。

  Minecraft の測り方:
    space  … advances に書いてある値をそのまま
    bitmap … 絵の中で、字が入っている一番右の列 + 1 を、8px 高さへ縮めた値
    unihex … size_overrides の left/right から (right-left+1)/2 + 1
"""
import io, json, os, sys, zipfile

MC = os.path.expandvars(r"%APPDATA%\.minecraft")
VER = os.path.join(MC, "versions", "1.20.1", "1.20.1.jar")
VERJ = os.path.join(MC, "versions", "1.20.1", "1.20.1.json")

jar = zipfile.ZipFile(VER)

# ---- ① 資材置き場から、本物の unifont.json を取る -----------
idx = json.load(io.open(VERJ, encoding="utf-8"))["assetIndex"]["id"]
ichiran = json.load(io.open(os.path.join(MC, "assets", "indexes", idx + ".json"),
                            encoding="utf-8"))["objects"]
kagi = "minecraft/font/include/unifont.json"
uni = None
if kagi in ichiran:
    h = ichiran[kagi]["hash"]
    p = os.path.join(MC, "assets", "objects", h[:2], h)
    if os.path.exists(p):
        uni = json.load(io.open(p, encoding="utf-8"))
        print("unifont.json を資材置き場から読んだ (%s)" % kagi)
if uni is None:
    print("★ 資材置き場に unifont.json が無い。全角は9pxとして進める")

zenkaku = {}
if uni:
    for pr in uni.get("providers", []):
        for o in pr.get("size_overrides", []):
            hidari, migi = o["left"], o["right"]
            haba = (migi - hidari + 1) // 2 + 1     # 16px幅 → 8 + 1 = 9
            zenkaku[(ord(o["from"]), ord(o["to"]))] = haba
    for k, v in list(zenkaku.items())[:8]:
        print("   U+%04X..U+%04X → %dpx" % (k[0], k[1], v))

# ---- ② ascii.png から半角の幅を測る -------------------------
try:
    from PIL import Image
except ImportError:
    print("★ PIL が無いので半角は既定値を使う")
    Image = None

hankaku = {}
if Image:
    # include/default.json に、どの絵にどの字が並んでいるかが書いてある
    dj = json.loads(jar.read("assets/minecraft/font/include/default.json").decode("utf-8"))
    for pr in dj["providers"]:
        if pr.get("type") != "bitmap":
            continue
        namae = pr["file"].split(":")[-1]
        try:
            data = jar.read("assets/minecraft/textures/font/" + os.path.basename(namae))
        except KeyError:
            continue
        im = Image.open(io.BytesIO(data)).convert("RGBA")
        gyou = pr["chars"]
        takasa = pr.get("height", 8)
        masuW = im.width // max(len(r) for r in gyou)
        masuH = im.height // len(gyou)
        px = im.load()
        for gi, retsu in enumerate(gyou):
            for ci, ch in enumerate(retsu):
                if ch == "":
                    continue
                x0, y0 = ci * masuW, gi * masuH
                migi = -1
                for x in range(masuW):
                    for y in range(masuH):
                        if px[x0 + x, y0 + y][3] != 0:
                            migi = x
                            break
                if migi < 0:
                    hankaku.setdefault(ch, 4)          # 空白の字
                else:
                    # 8px 高さへ縮めてから +1 の隙間
                    nama = (migi + 1) * takasa / float(masuH)
                    hankaku.setdefault(ch, int(round(nama)) + 1)

for c in "0123456789, ":
    print("   '%s' = %s px" % (c, hankaku.get(c, "?")))


def haba(s):
    """Font#width(s) と同じ値を出す。"""
    g = 0
    for ch in s:
        if ch == " ":
            g += 4
            continue
        if ch in hankaku:
            g += hankaku[ch]
            continue
        k = ord(ch)
        for (a, b), w in zenkaku.items():
            if a <= k <= b:
                g += w
                break
        else:
            g += 9          # 見つからない全角は9
    return g


if __name__ == "__main__":
    print()
    print("=== 測る ===")
    for s in ["個人 2", "個人 100", "勢力 12,000",
              "個人 2円", "個人 12円", "個人 100円", "勢力 500円",
              "個人2円", "個人100円", "勢力12,000円", "個人999,999円"]:
        print("   %-14s %3d px" % (s, haba(s)))
