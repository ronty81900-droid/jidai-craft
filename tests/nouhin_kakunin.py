# -*- coding: utf-8 -*-
"""説明書の納品を受け入れる検査。

  ★★ pdf_kakunin.py との違い ★★
    pdf_kakunin.py … 実装の数字が PDF に入っているか（29件）
    こちら         … **仕様書に書いた文章と表が、そのまま入っているか**

    送り状で「PDF から文字を取り出し、仕様書の文章・表と1文字ずつ照合します」と
    約束している。その約束を果たすのがこの道具。

  見る物:
    ① 仕様書の【本文ブロック】が PDF にそのまま入っているか
    ② 仕様書の【表の各セル】が PDF に入っているか
    ③ illust/ に e01〜e24 が揃っているか（B が番号で指すため）
    ④ manifest.json の sha256 と寸法が実物と合っているか
    ⑤ 絵の色が §2 のパレットに寄っているか

  使い方:
      python tests/nouhin_kakunin.py <納品を展開したフォルダ>
"""
import hashlib
import io
import json
import os
import re
import sys

NE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(NE, "docs")

pass_ = fail_ = 0


def check(mei, jouken, riyuu=""):
    global pass_, fail_
    if jouken:
        pass_ += 1
    else:
        fail_ += 1
        print("[FAIL] " + mei + "  " + str(riyuu))


def yomu(p):
    return io.open(p, encoding="utf-8").read()


def tsumeru(s):
    """空白・改行を落とす。PDF は行の折り返しが仕様書と違うため。"""
    return re.sub(r"[\s　]+", "", s)


def pdf_moji(p):
    try:
        import pypdf
    except ImportError:
        try:
            import PyPDF2 as pypdf
        except ImportError:
            print("★ pypdf が要ります: pip install pypdf")
            sys.exit(2)
    r = pypdf.PdfReader(p)
    return "\n".join((pg.extract_text() or "") for pg in r.pages), len(r.pages)


def honbun_toru(shiyou):
    """仕様書の ```…``` で囲んだ本文ブロックを全部 取る。"""
    return re.findall(r"\*\*本文[^*]*\*\*:\s*\n```\n(.*?)\n```", shiyou, re.S)


def hyou_cell(shiyou):
    """仕様書の表の、意味のあるセルを取る。

      ★★ 見るのは【ページごとの中身】の節だけ ★★
        §1 仕様（ページ数・解像度）や §2 パレットの表は、
        説明書に載る物ではない。全部の表を見ると、正しい納品でも落ちる
        （実際に 38 個が「入っていない」と出た）。
    """
    i = shiyou.find("ページごとの中身")
    if i < 0:
        i = 0
    j = shiyou.find("## 4. 納品", i)
    if j < 0:
        j = shiyou.find("## 5. 納品", i)
    if j < 0:
        j = len(shiyou)
    shiyou = shiyou[i:j]

    cells = []
    for g in shiyou.split("\n"):
        g = g.strip()
        if not g.startswith("|") or re.match(r"^\|[\s:\-\|]*-[\s:\-\|]*\|$", g):
            continue
        for c in g.strip("|").split("|"):
            c = c.strip().replace("**", "").replace("`", "")
            # 短すぎる物・記号だけは見ない（誤検知になる）
            if len(tsumeru(c)) >= 4 and not c.startswith("★"):
                cells.append(c)
    return cells


def main():
    ne = sys.argv[1] if len(sys.argv) > 1 else None
    if not ne or not os.path.isdir(ne):
        print("使い方: python tests/nouhin_kakunin.py <納品を展開したフォルダ>")
        return 2

    print("=" * 62)
    print("説明書の納品を受け入れる（仕様書と1文字ずつ照合）")
    print("=" * 62)

    KUMI = [
        ("A 遊び方", "Codex依頼文_説明書A_遊び方.md",
         "JidaiCraft_Manual_A_v8", "JidaiCraft_遊び方.pdf", 12),
        ("B 詳細版", "Codex依頼文_説明書B_詳細版.md",
         "JidaiCraft_Manual_B_v9", "JidaiCraft_詳細版.pdf", 13),
    ]

    for mei, shiyou_f, fol, pdf_f, page in KUMI:
        print()
        print("-- %s --" % mei)
        hako = os.path.join(ne, fol)
        if not os.path.isdir(hako):
            check("%s の箱がある" % mei, False, hako)
            continue
        p = os.path.join(hako, pdf_f)
        if not os.path.exists(p):
            check("%s の PDF がある" % mei, False, p)
            continue

        moji, pages = pdf_moji(p)
        tsume = tsumeru(moji)
        check("%s: %dページ" % (mei, page), pages == page, "実際=%dページ" % pages)

        shiyou = yomu(os.path.join(DOCS, shiyou_f))

        # ── ① 本文ブロック ──
        honbun = honbun_toru(shiyou)
        nai = [h for h in honbun if tsumeru(h) not in tsume]
        check("%s: 本文 %d ブロックがそのまま入っている" % (mei, len(honbun)),
              not nai, "入っていない: %s" % [h.split("\n")[0][:30] for h in nai])
        print("    本文 %d ブロック / 入っていない %d" % (len(honbun), len(nai)))

        # ── ② 表のセル ──
        cells = hyou_cell(shiyou)
        nai2 = [c for c in cells if tsumeru(c) not in tsume]
        check("%s: 表のセル %d 個がそのまま入っている" % (mei, len(cells)),
              len(nai2) == 0, "入っていない %d 個: %s" % (len(nai2), nai2[:6]))
        print("    表のセル %d 個 / 入っていない %d" % (len(cells), len(nai2)))

        # ── ④ manifest ──
        mf = os.path.join(hako, "manifest.json")
        check("%s: manifest.json がある" % mei, os.path.exists(mf), "無い")
        if os.path.exists(mf):
            m = json.load(io.open(mf, encoding="utf-8"))
            chigau = []
            for kumi in [m.get("pdf")] + list(m.get("pages", [])) \
                    + list(m.get("illustrations", [])):
                if not isinstance(kumi, dict) or "file" not in kumi:
                    continue
                q = os.path.join(hako, kumi["file"])
                if not os.path.exists(q):
                    chigau.append(kumi["file"] + "（物が無い）")
                    continue
                if "sha256" in kumi:
                    h = hashlib.sha256(open(q, "rb").read()).hexdigest()
                    if h != kumi["sha256"]:
                        chigau.append(kumi["file"] + "（sha256 が違う）")
            check("%s: manifest の sha256 が実物と全部 合っている" % mei,
                  not chigau, chigau[:5])

    # ── ★ 納品が【どの版の仕様書】で作られたか ──
    #
    #   ★★ 2026-08-29 に、これが無くて丸1往復 無駄にした ★★
    #     こちらが仕様書を書き直したのに、外注は【古い写し】で作っていた。
    #     見た目も中身も違う物が届き、原因が分かるまで検査を何本も回した。
    #     納品の DESIGN_SPEC.md に「元仕様」として sha256 が書いてある。
    #     **最初にここを見れば1秒で分かる。**
    print()
    print("-- 元にした仕様書 --")
    for fol in ("JidaiCraft_Manual_A_v8", "JidaiCraft_Manual_B_v9",
                "JidaiCraft_Manual_B_v8"):
        ds = os.path.join(ne, fol, "DESIGN_SPEC.md")
        if not os.path.exists(ds):
            continue
        moji = yomu(ds)
        for f in ("Codex依頼文_説明書A_遊び方.md", "Codex依頼文_説明書B_詳細版.md"):
            m = re.search(re.escape(f) + r"`?\s*sha256\s*`?([0-9a-f]{64})", moji)
            if not m:
                continue
            q = os.path.join(DOCS, f)
            ima = hashlib.sha256(io.open(q, "rb").read()).hexdigest()
            check("%s: %s が今の版で作られている" % (fol, f), m.group(1) == ima,
                  "外注が使った %s… / 今ある物 %s…（古い写しで作っている）"
                  % (m.group(1)[:12], ima[:12]))

    # ── ③ 絵が 24枚 そろっているか（B は A から流用する）──
    print()
    print("-- 絵 --")
    isho = os.path.join(ne, "JidaiCraft_Manual_A_v8", "illust")
    check("illust/ がある", os.path.isdir(isho), isho)
    if os.path.isdir(isho):
        aru = sorted(os.listdir(isho))
        ban = set(re.findall(r"e(\d\d)_", " ".join(aru)))
        iru = set("%02d" % n for n in range(1, 25))
        check("絵が e01〜e24 の 24枚 そろっている", ban == iru,
              "足りない: %s" % sorted(iru - ban))
        print("    %d 枚" % len(aru))

        # ── ⑤ 色がパレットに寄っているか ──
        try:
            from PIL import Image
        except ImportError:
            print("    （PIL が無いので色は見ない）")
        else:
            PALETTE = ["#102329", "#17313A", "#C4934B", "#F5E8C8",
                       "#AEB8AE", "#4FA1B2", "#E8B44A", "#5FA05F", "#3A6FA8"]
            pal = [tuple(int(x[i:i + 2], 16) for i in (1, 3, 5)) for x in PALETTE]

            def chikai(c):
                return min(sum((a - b) ** 2 for a, b in zip(c, p)) for p in pal) ** 0.5

            tooi = []
            for f in sorted(os.listdir(isho)):
                if not f.endswith(".png"):
                    continue
                im = Image.open(os.path.join(isho, f)).convert("RGB")
                im = im.resize((48, 48))
                iro = im.getdata()
                # ★ 生成イラストなので完全一致は求めない。
                #   「パレットからの距離」の平均で見る（0〜441）。
                heikin = sum(chikai(c) for c in iro) / len(iro)
                if heikin > 70:
                    tooi.append("%s（平均のずれ %.0f）" % (f, heikin))
            check("絵の色がパレットに寄っている（平均のずれ 70 未満）",
                  not tooi, tooi[:5])
            print("    パレットから遠い絵 %d 枚" % len(tooi))

    # ── ⑥ 下の余白（組版が薄すぎないか）──
    #   ★ 中身が正しくても、1ページの下半分が真っ白なら組版として未完成。
    #     機械で測れる数少ない「見た目」の指標なので入れておく。
    print()
    print("-- 下の余白 --")
    try:
        from PIL import Image
    except ImportError:
        print("    （PIL が無いので測らない）")
    else:
        # ★ 地の色は本ごとに違う。A は暗い地、B は明るい地（2026-08-26）。
        #   同じ色で測ると、B は「1画素も空いていない」と出て素通りする。
        JI_A = (0x10, 0x23, 0x29)
        JI_B = (0xF5, 0xE8, 0xC8)
        for mei, fol, yoko in (("A 遊び方", "JidaiCraft_Manual_A_v8", True),
                               ("B 詳細版", "JidaiCraft_Manual_B_v9", False)):
            pg = os.path.join(ne, fol, "pages")
            if not os.path.isdir(pg):
                continue
            aki = []
            for f in sorted(os.listdir(pg)):
                if not f.endswith(".png"):
                    continue
                im = Image.open(os.path.join(pg, f)).convert("RGB")
                im = im.resize((140, 100) if yoko else (100, 140))
                px = list(im.getdata())
                W, H = im.size
                # ★★ ページ番号と足元の罫を飛ばしてから数える ★★
                #   下から順に見ると、いちばん下の「12」や罫線で止まってしまい、
                #   目で見て3割 空いているページを「2%」と報せた（2026-08-29）。
                #   下の 8% は飾りの帯とみなして、その上から数える。
                JI = JI_A if yoko else JI_B

                def ji_dake(y):
                    return all(sum((a2 - b2) ** 2
                                   for a2, b2 in zip(px[y * W + x], JI)) < 900
                               for x in range(W))

                obi = max(1, H * 8 // 100)
                kara = 0
                for y in range(H - 1 - obi, -1, -1):
                    if ji_dake(y):
                        kara += 1
                    else:
                        break
                aki.append(kara * 100 // H)
            heikin = sum(aki) // max(1, len(aki))
            warui = [x for x in aki if x >= 25]
            print("    %s: 平均 %d%% / 25%%以上 空いたページ %d 枚 / %d 枚"
                  % (mei, heikin, len(warui), len(aki)))
            check("%s: 下の余白が平均 25%% 未満" % mei, heikin < 25,
                  "平均 %d%%（中身は正しいが、組版が薄い）" % heikin)

    print()
    print("=" * 62)
    print("結果: PASS %d / FAIL %d" % (pass_, fail_))
    print("★ 絵の中身・読みやすさは機械では見られない。人の目で見ること。")
    return 1 if fail_ else 0


if __name__ == "__main__":
    sys.exit(main())
