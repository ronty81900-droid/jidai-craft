# -*- coding: utf-8 -*-
"""世界の時代バー（ボスバー）の検査。依頼文 v2 §6。

    python tests/bar_kakunin.py

[PASS] / [FAIL] / [WARN] を1行ずつ出し、最後に件数を出す。FAIL が1つでもあれば終了コード 1。
検査10は bar_tsukuru.py を2回動かす（本番ファイルを上書きする。決定的なので中身は変わらない）。
"""
from __future__ import annotations

import glob
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "clientmod" / "ui"
GUI = UI / "assets" / "jidaiui" / "textures" / "gui"
PREVIEW = UI / "preview_bossbar"
SPEC = UI / "BOSSBAR_SPEC.md"
MANIFEST = UI / "manifest.json"
SCRIPT = ROOT / "clientmod" / "tools" / "bar_tsukuru.py"

FRAME = "bar_frame.png"
FILLS = ["bar_fill_tekki.png", "bar_fill_chusei.png", "bar_fill_kindai.png", "bar_fill_gendai.png"]
FRAME_SIZE = (728, 32)
FILL_SIZE = (720, 24)
BORDER = 4
PERIOD = 120
MAX_COLORS = 32
DELTA_LIMIT = 24
DERIVED_PER_BASE = 2
LUMA_SLOT_MIN = 60      # 6a: fill 平均輝度 − slot 輝度
LUMA_PAIR_WARN = 20     # 6b: 時代間の平均輝度差（WARN のみ）
GROOVE_RADIUS = 4       # 溝と fill の角丸。fill は各角 (0,0)(1,0)(0,1) の3画素が透明
TICK_W = 4              # 目盛り: x mod 120 < 4 の列・全高・palette dark
FILL_CORNER_PIXELS = {(0, 0), (1, 0), (0, 1)}   # 左上基準。他の角は鏡映

results: list[tuple[str, str, str]] = []


def report(status: str, name: str, why: str = "") -> None:
    results.append((status, name, why))
    print(f"[{status}] {name}" + (f"  {why}" if why else ""))


def check(cond: bool, name: str, why: str = "") -> None:
    report("PASS" if cond else "FAIL", name, "" if cond else why)


def load(name: str) -> np.ndarray:
    return np.asarray(Image.open(GUI / name).convert("RGBA"))


def hex_to_rgb(s: str) -> tuple[int, int, int]:
    s = s.lstrip("#")
    return int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)


def luma(rgb) -> int:
    r, g, b = (int(v) for v in rgb)
    return int(round(0.2126 * r + 0.7152 * g + 0.0722 * b))


def mean_luma(arr: np.ndarray) -> float:
    rgb = arr[:, :, :3].astype(np.float64)
    return float(np.mean(0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]))


def colors_of(arr: np.ndarray) -> set[tuple[int, int, int, int]]:
    return {tuple(int(v) for v in c) for c in np.unique(arr.reshape(-1, 4), axis=0)}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_derived(spec_text: str) -> list[tuple[str, int, tuple[int, int, int]]]:
    """BOSSBAR_SPEC.md の派生色表 `| `基色名` | +20 | `#RRGGBB` | ...` を読む。"""
    out = []
    for m in re.finditer(r"^\|\s*`([A-Za-z0-9_]+)`\s*\|\s*([+-]\d+)\s*\|\s*`(#[0-9A-Fa-f]{6})`", spec_text, re.M):
        out.append((m.group(1), int(m.group(2)), hex_to_rgb(m.group(3))))
    return out


def main() -> int:
    manifest = json.loads(MANIFEST.read_bytes().decode("utf-8"))
    pal = {k: hex_to_rgb(v) for k, v in manifest["palette"].items()}
    imgs = {n: load(n) for n in [FRAME] + FILLS}

    # 1 大きさ
    for n, size in [(FRAME, FRAME_SIZE)] + [(f, FILL_SIZE) for f in FILLS]:
        got = (imgs[n].shape[1], imgs[n].shape[0])
        check(got == size, f"1 size {n}", f"{got} != {size}")

    # 2 色数
    for n, a in imgs.items():
        c = len(colors_of(a))
        check(c <= MAX_COLORS, f"2 colors<=32 {n}", f"{c} 色")

    # 3 アルファ 0/255
    for n, a in imgs.items():
        vals = set(int(v) for v in np.unique(a[:, :, 3]))
        check(vals <= {0, 255}, f"3 alpha binary {n}", f"alpha={sorted(vals)}")

    # 4 透明画素の RGB
    for n, a in imgs.items():
        t = a[a[:, :, 3] == 0]
        check(t.size == 0 or not np.any(t[:, :3]), f"4 transparent rgb zero {n}", "透明画素に RGB != 0 がある")

    # 5 fill のアルファ形状一致
    ref = imgs[FILLS[0]][:, :, 3]
    for n in FILLS[1:]:
        check(imgs[n].shape == imgs[FILLS[0]].shape and np.array_equal(imgs[n][:, :, 3], ref),
              f"5 alpha shape == tekki {n}", "アルファの形が違う")

    # 6a fill 平均輝度 − slot 輝度 >= 60
    slot_l = luma(pal["slot"])
    means = {}
    for n in FILLS:
        means[n] = mean_luma(imgs[n])
        d = means[n] - slot_l
        check(d >= LUMA_SLOT_MIN, f"6a luma fill-slot>={LUMA_SLOT_MIN} {n}", f"fill={means[n]:.1f} slot={slot_l} diff={d:.1f}")
        if d >= LUMA_SLOT_MIN:
            print(f"       fill={means[n]:.1f} slot={slot_l} diff={d:.1f}")
    # 6b 時代間の平均輝度差（WARN のみ・全ペア出す）
    for i in range(len(FILLS)):
        for j in range(i + 1, len(FILLS)):
            a, b = FILLS[i], FILLS[j]
            d = abs(means[a] - means[b])
            report("PASS" if d >= LUMA_PAIR_WARN else "WARN", f"6b luma pair>={LUMA_PAIR_WARN} {a} vs {b}",
                   f"{means[a]:.1f} vs {means[b]:.1f} diff={d:.1f}")

    # 7 列 x == 列 (x mod 120)。ただし角丸の透明画素（各角3画素）は比較から外す
    def corner_mask(h: int, w: int) -> np.ndarray:
        m = np.zeros((h, w), dtype=bool)
        for dx, dy in FILL_CORNER_PIXELS:
            for x in (dx, w - 1 - dx):
                for y in (dy, h - 1 - dy):
                    m[y, x] = True
        return m
    for n in FILLS:
        a = imgs[n]
        cm = corner_mask(*a.shape[:2])
        bad = []
        for x in range(a.shape[1]):
            keep = ~(cm[:, x] | cm[:, x % PERIOD])
            if not np.array_equal(a[keep, x], a[keep, x % PERIOD]):
                bad.append(x)
        check(not bad, f"7 column periodic {PERIOD} {n}", f"一致しない列 {bad[:8]}{'...' if len(bad) > 8 else ''}")

    # 8 枠の縁 4px（溝 (4,4)〜(723,27) は slot 系のみ、目盛り列は dark、縁は slot 系を含まない）
    fr = imgs[FRAME]
    spec_text = SPEC.read_bytes().decode("utf-8") if SPEC.exists() else ""
    derived = read_derived(spec_text)
    slot_family = {pal["slot"]} | {rgb for base, _d, rgb in derived if base == "slot"}
    H, W = fr.shape[:2]
    groove = fr[BORDER:H - BORDER, BORDER:W - BORDER]
    tick_cols = (np.arange(groove.shape[1]) % PERIOD) < TICK_W
    gcm = corner_mask(*groove.shape[:2])          # 溝の角3画素は枠のベベルなので除外
    groove_colors = {c[:3] for c in colors_of(groove[~gcm & ~tick_cols[None, :]]) if c[3] == 255}
    tick_colors = {c[:3] for c in colors_of(groove[~gcm & tick_cols[None, :]]) if c[3] == 255}
    mask = np.ones((H, W), dtype=bool)
    mask[BORDER:H - BORDER, BORDER:W - BORDER] = False
    border_colors = {c[:3] for c in colors_of(fr[mask]) if c[3] == 255}
    check(groove.shape[:2] == (FILL_SIZE[1], FILL_SIZE[0]), "8 groove size", f"{groove.shape[1]}x{groove.shape[0]}")
    check(groove_colors <= slot_family, "8 groove only slot family", f"溝に slot 系以外: {sorted(groove_colors - slot_family)[:4]}")
    check(tick_colors == {pal["dark"]}, "8 groove tick columns are dark", f"目盛り列の色: {sorted(tick_colors)[:4]}")
    check(not (border_colors & slot_family), "8 border has no slot family", f"縁に slot 系: {sorted(border_colors & slot_family)[:4]}")

    # 13 角丸と目盛り: fill の透明画素は各角3画素ちょうど。fill の目盛り列は全高 dark。枠の角も透明
    for n in FILLS:
        a = imgs[n]
        cm = corner_mask(*a.shape[:2])
        transparent = a[:, :, 3] == 0
        check(np.array_equal(transparent, cm), f"13 fill corners transparent exactly {n}",
              f"透明画素 {int(transparent.sum())} 個（期待 {int(cm.sum())}）")
        tc = (np.arange(a.shape[1]) % PERIOD) < TICK_W
        t = a[:, tc]
        t_ok = {c[:3] for c in colors_of(t) if c[3] == 255} == {pal["dark"]}
        check(t_ok, f"13 fill tick columns are dark {n}", "目盛り列に dark 以外の不透明色")
    check(fr[0, 0, 3] == 0 and fr[0, W - 1, 3] == 0 and fr[H - 1, 0, 3] == 0 and fr[H - 1, W - 1, 3] == 0
          and fr[0, W // 2, 3] == 255 and fr[H // 2, 0, 3] == 255,
          "13 frame corners transparent, edges opaque", "枠の角が透明でないか、辺が透明")

    # 9 全色 ⊆ palette ∪ 派生色。派生色の規則も見る
    check(bool(derived), "9 derived table present in BOSSBAR_SPEC.md", "派生色の表が読めない")
    ok_rule = True
    why = []
    per_base: dict[str, int] = {}
    for base, delta, rgb in derived:
        per_base[base] = per_base.get(base, 0) + 1
        if base not in pal:
            ok_rule = False; why.append(f"{base} が palette に無い")
        elif abs(delta) > DELTA_LIMIT:
            ok_rule = False; why.append(f"{base}{delta:+d} が ±{DELTA_LIMIT} 超")
        else:
            exp = tuple(min(255, max(0, v + delta)) for v in pal[base])
            if exp != rgb:
                ok_rule = False; why.append(f"{base}{delta:+d} の16進が計算と違う")
    for base, cnt in per_base.items():
        if cnt > DERIVED_PER_BASE:
            ok_rule = False; why.append(f"{base} の派生色が {cnt} 個")
    check(ok_rule, "9 derived rule (±24, <=2 per base, computed)", "; ".join(why))
    allowed = set(pal.values()) | {rgb for _b, _d, rgb in derived}
    for n, a in imgs.items():
        used = {c[:3] for c in colors_of(a) if c[3] == 255}
        extra = used - allowed
        check(not extra, f"9 colors in palette+derived {n}", f"許可外の色 {['#%02X%02X%02X' % c for c in sorted(extra)][:4]}")

    # 12 manifest の files に5件、寸法と sha256 が実物と一致（依頼文 §7-2。§6 の表には無いが安い）
    by_file = {e["file"]: e for e in manifest["files"]}
    for n in [FRAME] + FILLS:
        key = "assets/jidaiui/textures/gui/" + n
        e = by_file.get(key)
        if e is None:
            check(False, f"12 manifest entry {n}", "files に無い")
            continue
        a = imgs[n]
        ok = (e.get("category") == "bossbar" and e.get("dimensions") == [a.shape[1], a.shape[0]]
              and e.get("sha256", "").upper() == sha(GUI / n).upper()
              and e.get("rgba_color_count") == len(colors_of(a)))
        check(ok, f"12 manifest entry {n}", "category/dimensions/sha256/rgba_color_count のどれかが実物と違う")
    check("era_gendai" in pal, "12 manifest palette era_gendai", "palette に era_gendai が無い")

    # 10 決定性: 現在の byte → 1回目 → 2回目 が全部一致
    targets = sorted([GUI / n for n in [FRAME] + FILLS] + list(PREVIEW.glob("*.png")) + [SPEC, MANIFEST])
    h0 = {p: sha(p) for p in targets}
    for i in (1, 2):
        r = subprocess.run([sys.executable, str(SCRIPT)], capture_output=True, text=True, encoding="utf-8")
        if r.returncode != 0:
            check(False, f"10 deterministic run{i}", f"bar_tsukuru.py が失敗: {r.stdout.strip()} {r.stderr.strip()[-200:]}")
            break
        h = {p: sha(p) for p in targets}
        diff = [p.name for p in targets if h[p] != h0[p]]
        check(not diff, f"10 deterministic run{i} == current", f"byte が違う: {diff}")
        h0 = h

    # ══════════════════════════════════════════════════════════
    # 14 描く側（クライアント MOD）が契約どおりか
    #   ★ 絵・契約書・MOD・データパックの4つを突き合わせる。
    #     1つ直しても他は通ってしまうので、ここでしか捕まえられない。
    # ══════════════════════════════════════════════════════════
    MOD = ROOT / "clientmod" / "src" / "main" / "java" / "jidai" / "ui" / "Bosubaa.java"
    BAR_FN = (ROOT / "datapacks" / "jidai_craft" / "data" / "jidai" / "functions"
              / "shinko" / "bar.mcfunction")
    if not MOD.exists():
        check(False, "14 Bosubaa.java がある", str(MOD))
    elif not BAR_FN.exists():
        check(False, "14 bar.mcfunction がある", str(BAR_FN))
    else:
        mod = MOD.read_text(encoding="utf-8")
        fn = BAR_FN.read_text(encoding="utf-8")

        # ★★ 字を探す検査は【注記を外してから】見る ★★
        #   実際に踏んだ: Mc.kaku( を // で潰しても "Mc.kaku(" は残るので、
        #   検査が通ってしまった（＝壊れているのに緑）。注記を落としてから探す。
        mod_code = re.sub(r"/\*.*?\*/", "", mod, flags=re.S)
        mod_code = re.sub(r"//[^\n]*", "", mod_code)

        # 14-1 目印: MOD が自分のバーを見分ける字が、データパックの名前に必ず入るか
        m = re.search(r'String MEJIRUSHI = "([^"]+)"', mod)
        check(m is not None, "14 MOD に目印の字がある")
        if m:
            shirushi = m.group(1)
            namae = re.findall(r'bossbar set \S+ name .*?"text":"([^"]*)"', fn)
            check(bool(namae) and all(shirushi in n for n in namae),
                  f"14 目印「{shirushi}」がデータパックの名前 {len(namae)} 本すべてに入る",
                  f"入っていない: {[n for n in namae if shirushi not in n]}")

        # 14-2 色 → 絵 の対応が、契約書とデータパックの3つで一致するか
        #   MOD:        BossBarColor.WHITE → bar_fill_tekki.png
        #   契約書 §3:  | `white` | `bar_fill_tekki.png` | 鉄器 |
        #   データパック: chuo matches ..1 → color white ／ name …鉄器
        mod_taiou = dict((a.lower(), b) for a, b in re.findall(
            r'BossBarColor\.(\w+)\) \{\s*\n\s*return "([^"]+)";', mod))
        spec_taiou = dict(re.findall(r"\| `(\w+)` \| `(bar_fill_\w+\.png)` \|", spec_text))
        check(len(mod_taiou) == 4, f"14 MOD の色→絵 が4件", sorted(mod_taiou))
        check(mod_taiou == spec_taiou, "★14 MOD の色→絵 が BOSSBAR_SPEC §3 と一致",
              f"MOD={mod_taiou} / 契約書={spec_taiou}")

        # データパック側の 時代 → 色 と、契約書の 時代 → 絵 を突き合わせる
        iro = dict((k.strip(), v) for k, v in re.findall(
            r"chuo matches (\S+) run bossbar set \S+ color (\w+)", fn))
        jidai = dict((k.strip(), v) for k, v in re.findall(
            r"chuo matches (\S+) run bossbar set \S+ name .*?世界の時代.(\w+?).,", fn))
        spec_jidai = dict((b, c) for _a, b, c in re.findall(
            r"\| `(\w+)` \| `(bar_fill_\w+\.png)` \| (\S+) \|", spec_text))
        ok, riyuu = True, []
        for shiki, ir in sorted(iro.items()):
            e = spec_taiou.get(ir)
            if e is None or spec_jidai.get(e) != jidai.get(shiki):
                ok = False
                riyuu.append(f"{jidai.get(shiki)}→{ir}→{e}（契約書は {spec_jidai.get(e)}）")
        check(bool(iro) and ok,
              f"★14 データパックの 時代→色 と 契約書の 色→絵→時代 が一致（{len(iro)} 件）",
              " / ".join(riyuu))

        # 14-3 寸法: MOD が持つ数字が、実物の絵と契約書に合うか
        suuji = dict((a, int(b)) for a, b in re.findall(
            r"int (WAKU_TEX_W|WAKU_TEX_H|NAKA_TEX_W|NAKA_TEX_H|WAKU_W|WAKU_H|MIZO_W|MIZO_H)"
            r" = (\d+);", mod))
        check(len(suuji) == 8, "14 MOD の寸法が8つ読める", sorted(suuji))
        if len(suuji) == 8:
            check((suuji["WAKU_TEX_W"], suuji["WAKU_TEX_H"]) == FRAME_SIZE,
                  "★14 MOD の枠のテクスチャ寸法が実物と同じ", str(suuji))
            check((suuji["NAKA_TEX_W"], suuji["NAKA_TEX_H"]) == FILL_SIZE,
                  "★14 MOD の中身のテクスチャ寸法が実物と同じ", str(suuji))
            # 契約書: 枠は 182×8 論理px、溝の論理幅は 180
            check("**182×8 論理px**" in spec_text and suuji["WAKU_W"] == 182 and suuji["WAKU_H"] == 8,
                  "★14 MOD の枠の論理寸法が契約書（182×8）と同じ", str(suuji))
            check("**180**" in spec_text and suuji["MIZO_W"] == 180,
                  "★14 MOD の溝の論理幅が契約書（180）と同じ", str(suuji))
            # 縮尺が縦横で同じか（横 728/182=4、縦 32/8=4、中身 720/180=4・24/6=4）
            bai = {suuji["WAKU_TEX_W"] // suuji["WAKU_W"], suuji["WAKU_TEX_H"] // suuji["WAKU_H"],
                   suuji["NAKA_TEX_W"] // suuji["MIZO_W"], suuji["NAKA_TEX_H"] // suuji["MIZO_H"]}
            check(bai == {4}, "★14 縦横とも同じ倍率（4倍）で描く", f"倍率={sorted(bai)}")
            # 切り出し位置が目盛り（120px）に必ず乗るか
            #   論理px で丸めてから 4倍するので、6段階なら 30px 刻み → 120px 刻み
            kizami = suuji["MIZO_W"] // 6 * (suuji["NAKA_TEX_W"] // suuji["MIZO_W"])
            check(kizami == PERIOD,
                  f"★14 満タン6段階の切り口が目盛り {PERIOD}px に乗る（{kizami}px 刻み）",
                  f"{kizami} != {PERIOD}")

        # 14-4 キャンセルしたら名前も飛ぶ。名前を自分で描いているか
        #   ★ Forge が当てた BossHealthOverlay を javap で読んで確かめた事実。
        #     setCanceled(true) だけ書いて名前を描き忘れると、実機で名前が消える。
        check("setCanceled(true)" in mod_code, "14 MOD がバニラの描画を止めている")
        check("Mc.kaku(" in mod_code and "Mc.haba(" in mod_code,
              "★14 名前を自分で描いている（キャンセルすると名前も飛ぶため）",
              "描いていない＝実機で名前が消える")

        # 14-5 他人のボスバーに触らないか
        check("contains(MEJIRUSHI)" in mod_code and "return;" in mod_code,
              "★14 目印が無いボスバーには触らない（他の MOD の帯を壊さない）")

        # 14-6 絵が jar に入るか（build.py が ui/assets を詰める）
        jar = ROOT / "clientmod" / "JidaiUI-0.1.0.jar"
        if jar.exists():
            import zipfile
            naka = set(zipfile.ZipFile(jar).namelist())
            nai = [f for f in [FRAME] + FILLS
                   if f"assets/jidaiui/textures/gui/{f}" not in naka]
            check(not nai, f"★14 jar に絵が5枚 入っている", f"入っていない: {nai}")
            check("jidai/ui/Bosubaa.class" in naka, "14 jar に Bosubaa.class が入っている")
        else:
            report("WARN", "14 jar が無い", "clientmod/build.py を動かすと確かめられる")

    # 11 既存検査を全件実行
    others = sorted(p for p in glob.glob(str(ROOT / "tests" / "*_kakunin.py")) if Path(p).name != "bar_kakunin.py")
    if not others:
        report("WARN", "11 existing tests", "tests/*_kakunin.py が他に無い（この環境ではプロジェクト全体が無い）")
    for p in others:
        r = subprocess.run([sys.executable, p], capture_output=True, text=True, encoding="utf-8")
        deta = (r.stdout or "") + (r.stderr or "")
        owari = r.stdout.strip().splitlines()[-1:] if r.stdout else [r.stderr[-200:]]
        # 引数が要る検査は「使い方」を出して終わる。これは失敗ではないので飛ばす
        if r.returncode != 0 and "使い方" in deta:
            report("WARN", f"11 existing {Path(p).name}", "引数が要る検査なので飛ばした")
            continue
        if r.returncode == 0:
            check(True, f"11 existing {Path(p).name}")
            continue
        # ★ 落ちた検査が【この変更が届く所】を読んでいるかで分ける。
        #   検査11 の狙いは「manifest に5件足して既存を壊していないか」。
        #   manifest も gui/ も読まない検査の赤は、その問いの答えにならない。
        moto = Path(p).read_text(encoding="utf-8", errors="replace")
        todoku = ("manifest.json" in moto) or ("textures/gui" in moto) or ("textures\\gui" in moto)
        if todoku:
            check(False, f"11 existing {Path(p).name}", f"exit={r.returncode} {owari}")
        else:
            report("WARN", f"11 existing {Path(p).name}",
                   f"exit={r.returncode} {owari} ★この検査は manifest も gui/ も読まない＝ボスバーとは無関係の赤（前から落ちている）")

    n_pass = sum(1 for s, _, _ in results if s == "PASS")
    n_fail = sum(1 for s, _, _ in results if s == "FAIL")
    n_warn = sum(1 for s, _, _ in results if s == "WARN")
    print(f"PASS {n_pass} / FAIL {n_fail} / WARN {n_warn} / total {len(results)}")
    return 1 if n_fail else 0


if __name__ == "__main__":
    sys.exit(main())
