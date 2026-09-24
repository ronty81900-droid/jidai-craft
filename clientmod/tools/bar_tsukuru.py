# -*- coding: utf-8 -*-
"""世界の時代バー（ボスバー）の絵を吐く。

    python clientmod/tools/bar_tsukuru.py

- 色は全部 clientmod/ui/manifest.json の palette から読む。16進はこのファイルに書かない
- 派生色は基色 ±DELTA で計算し BOSSBAR_SPEC.md に書き出す（基色ごと2つまで・±24以内）
- 乱数は numpy.random.default_rng(固定の種)。2回動かせば同じ byte が出る
- フォントは読まない。文字は描かない
- 同じ実行の中で preview_bossbar/ と BOSSBAR_SPEC.md と manifest.json の files 5件を更新する

出力:
  clientmod/ui/assets/jidaiui/textures/gui/bar_frame.png       728x32
  clientmod/ui/assets/jidaiui/textures/gui/bar_fill_<era>.png  720x24 x4
  clientmod/ui/preview_bossbar/*.png                            4倍・最近傍
  clientmod/ui/BOSSBAR_SPEC.md
  clientmod/ui/manifest.json  (files に5件、既にあれば置き換え)
"""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
UI_DIR = ROOT / "clientmod" / "ui"
MANIFEST = UI_DIR / "manifest.json"
GUI_DIR = UI_DIR / "assets" / "jidaiui" / "textures" / "gui"
PREVIEW_DIR = UI_DIR / "preview_bossbar"
SPEC_MD = UI_DIR / "BOSSBAR_SPEC.md"
REQUEST_MD = ROOT / "docs" / "Fable依頼文_ボスバー_v3.md"
RESOURCE_PREFIX = "jidaiui:textures/gui/"
MANIFEST_FILE_PREFIX = "assets/jidaiui/textures/gui/"

# ---- 寸法（依頼文 §2・§3）---------------------------------------------------
FRAME_W, FRAME_H = 728, 32
FILL_W, FILL_H = 720, 24
BORDER = 4                      # 上下左右の縁
STEPS = 6                       # 満タン = 6 → 切る位置は 120px 刻み
PERIOD = FILL_W // STEPS        # 120
PREVIEW_SCALE = 4
SEED = 20260903
OUTER_RADIUS = 8                # 枠の外周の角丸（2論理px）。内側へ1pxごとに1減る
GROOVE_RADIUS = OUTER_RADIUS - BORDER   # 溝と fill の角丸（1論理px）= 4
TICK_W = 4                      # 目盛りの幅（1論理px）。各120pxタイルの先頭 0..3 列

# ---- 時代 → palette 名（依頼文 §2 の表。順番は時代順）------------------------
ERAS = [
    # (ファイル接尾, 時代名, bossbar color, palette 名)
    ("tekki", "鉄器", "white", "muted"),
    ("chusei", "中世", "yellow", "brass"),
    ("kindai", "近代", "blue", "info"),
    ("gendai", "現代", "purple", "era_gendai"),
]
FRAME_ROLES = ["outer", "light", "dark", "panel", "slot"]

# ---- 派生色の規則（依頼文 §4）。基色ごと2つまで・各チャンネル ±24 以内 --------
FILL_LIGHT_DELTA = +20      # fill 上端2行
FILL_DARK_DELTA = -20       # fill 下端2行と繊維ノイズ
SLOT_DARK_DELTA = -12       # 溝の繊維ノイズ
DELTA_LIMIT = 24


def hex_to_rgb(s: str) -> tuple[int, int, int]:
    s = s.lstrip("#")
    return int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)


def rgb_to_hex(c) -> str:
    return "#%02X%02X%02X" % tuple(int(v) for v in c)


def derive(base: tuple[int, int, int], delta: int) -> tuple[int, int, int]:
    assert abs(delta) <= DELTA_LIMIT, delta
    return tuple(int(min(255, max(0, v + delta))) for v in base)


def load_manifest() -> dict:
    return json.loads(MANIFEST.read_bytes().decode("utf-8"))


def save_manifest(m: dict) -> None:
    # 既存ファイルと同じ整形（2スペース・非ASCIIそのまま・末尾改行1つ・LF）
    MANIFEST.write_bytes((json.dumps(m, ensure_ascii=False, indent=2) + "\n").encode("utf-8"))


def fiber_mask(rng: np.random.Generator, width: int, height: int, count: int,
               y0: int, y1: int, min_len: int, max_len: int) -> np.ndarray:
    """横向きの短い線（紙の繊維）。タイルの端をまたがない（切れ目に繊維が掛からない）。"""
    mask = np.zeros((height, width), dtype=bool)
    for _ in range(count):
        n = int(rng.integers(min_len, max_len + 1))
        x = int(rng.integers(0, width - n + 1))
        y = int(rng.integers(y0, y1))
        mask[y, x:x + n] = True
    return mask


def rounded_mask(w: int, h: int, inset: int, radius: int) -> np.ndarray:
    """inset だけ内側に寄せた角丸長方形。画素中心が角の円の内側なら True。アンチエイリアスなし。"""
    ys, xs = np.mgrid[0:h, 0:w]
    inside = (xs >= inset) & (xs < w - inset) & (ys >= inset) & (ys < h - inset)
    if radius <= 0:
        return inside
    cx, cy = xs + 0.5, ys + 0.5
    x0, y0, x1, y1 = inset, inset, w - inset, h - inset
    for ccx, ccy in ((x0 + radius, y0 + radius), (x1 - radius, y0 + radius),
                     (x0 + radius, y1 - radius), (x1 - radius, y1 - radius)):
        corner = ((cx < ccx) if ccx == x0 + radius else (cx > ccx)) & ((cy < ccy) if ccy == y0 + radius else (cy > ccy))
        inside &= ~corner | ((cx - ccx) ** 2 + (cy - ccy) ** 2 <= radius ** 2)
    return inside


def tick_columns(w: int) -> np.ndarray:
    xs = np.arange(w)
    return (xs % PERIOD) < TICK_W


def build_fill(base: tuple[int, int, int], tick: tuple[int, int, int], tile_mask: np.ndarray) -> np.ndarray:
    light = derive(base, FILL_LIGHT_DELTA)
    dark = derive(base, FILL_DARK_DELTA)
    img = np.zeros((FILL_H, FILL_W, 4), dtype=np.uint8)
    img[:, :, :3] = base
    img[:, :, 3] = 255
    img[0:2, :, :3] = light                 # 上の縁取り（2px = 0.5論理px）
    img[FILL_H - 2:FILL_H, :, :3] = dark    # 下の影
    full_mask = np.tile(tile_mask, (1, STEPS))   # 120px タイルを6回
    img[full_mask, :3] = dark
    img[:, tick_columns(FILL_W), :3] = tick     # 目盛り（各タイル先頭4列・全高）
    outside = ~rounded_mask(FILL_W, FILL_H, 0, GROOVE_RADIUS)
    img[outside] = (0, 0, 0, 0)                 # 角丸。透明画素は RGB 0
    return img


def build_frame(pal: dict, slot_mask_tile: np.ndarray) -> np.ndarray:
    outer, light, dark, panel, slot = (hex_to_rgb(pal[k]) for k in FRAME_ROLES)
    slot_dark = derive(slot, SLOT_DARK_DELTA)
    img = np.zeros((FRAME_H, FRAME_W, 4), dtype=np.uint8)   # 角の外は透明・RGB 0
    ys, xs = np.mgrid[0:FRAME_H, 0:FRAME_W]
    d_l, d_t, d_r, d_b = xs, ys, FRAME_W - 1 - xs, FRAME_H - 1 - ys
    top_left = np.minimum(d_l, d_t) <= np.minimum(d_r, d_b)   # 角は対角線で分ける
    # 外から内へ 1px ずつ: outer / 凸ベベル(light|dark) / panel / 凹ベベル(dark|light)。角丸は1pxごとに半径を1減らす
    shells = [rounded_mask(FRAME_W, FRAME_H, r, OUTER_RADIUS - r) for r in range(BORDER + 1)]
    rings = [shells[r] & ~shells[r + 1] for r in range(BORDER)]
    img[rings[0], :3] = outer
    img[rings[1] & top_left, :3] = light
    img[rings[1] & ~top_left, :3] = dark
    img[rings[2], :3] = panel
    img[rings[3] & top_left, :3] = dark
    img[rings[3] & ~top_left, :3] = light
    for r in rings:
        img[r, 3] = 255
    # 溝（角丸半径 4 = fill と同じ形）
    groove = shells[BORDER]
    img[groove, :3] = slot
    img[groove, 3] = 255
    fiber = np.zeros((FRAME_H, FRAME_W), dtype=bool)
    fiber[BORDER:BORDER + FILL_H, BORDER:BORDER + FILL_W] = np.tile(slot_mask_tile, (1, STEPS))
    img[fiber & groove, :3] = slot_dark
    tick = np.zeros((FRAME_H, FRAME_W), dtype=bool)
    tick[BORDER:BORDER + FILL_H, BORDER:BORDER + FILL_W] = tick_columns(FILL_W)[None, :]
    img[tick & groove, :3] = dark                # 目盛り。fill と同じ列
    return img


def save_png(arr: np.ndarray, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(arr, "RGBA").save(path, format="PNG", optimize=False, compress_level=9)


def compose(frame: np.ndarray, fill: np.ndarray | None, value: int) -> np.ndarray:
    out = frame.copy()
    if fill is not None and value > 0:
        w = FILL_W * value // STEPS          # 左端から幅×進捗
        part = fill[:, :w]
        opaque = part[:, :, 3] == 255
        region = out[BORDER:BORDER + FILL_H, BORDER:BORDER + w]
        region[opaque] = part[opaque]
    return out


def upscale(arr: np.ndarray) -> np.ndarray:
    return np.repeat(np.repeat(arr, PREVIEW_SCALE, axis=0), PREVIEW_SCALE, axis=1)


def png_stats(path: Path) -> dict:
    data = path.read_bytes()
    arr = np.asarray(Image.open(path).convert("RGBA"))
    colors = np.unique(arr.reshape(-1, 4), axis=0)
    alphas = sorted(int(a) for a in np.unique(arr[:, :, 3]))
    transparent = arr[arr[:, :, 3] == 0]
    return {
        "dimensions": [int(arr.shape[1]), int(arr.shape[0])],
        "byte_size": len(data),
        "sha256": hashlib.sha256(data).hexdigest().upper(),
        "rgba_color_count": int(len(colors)),
        "alpha_values": alphas,
        "transparent_rgb_zero": bool(transparent.size == 0 or not np.any(transparent[:, :3])),
    }


def manifest_entry(name: str, purpose: str) -> dict:
    st = png_stats(GUI_DIR / name)
    return {
        "file": MANIFEST_FILE_PREFIX + name,
        "resource_location": RESOURCE_PREFIX + name,
        "purpose": purpose,
        "category": "bossbar",
        "dimensions": st["dimensions"],
        "blank_regions": [],
        "byte_size": st["byte_size"],
        "sha256": st["sha256"],
        "rgba_color_count": st["rgba_color_count"],
        "alpha_values": st["alpha_values"],
        "transparent_rgb_zero": st["transparent_rgb_zero"],
        "blank_regions_verified_flat": 0,
        "contains_baked_text": False,
    }


def main() -> int:
    m = load_manifest()
    pal = m["palette"]
    missing = [k for k in FRAME_ROLES + [e[3] for e in ERAS] if k not in pal]
    if missing:
        print("[STOP] manifest.json の palette に無い名前:", ", ".join(missing))
        print("       依頼文 §7-1 のとおり era_gendai を palette に足してから動かしてください")
        return 2

    rng = np.random.default_rng(SEED)
    fill_tile = fiber_mask(rng, PERIOD, FILL_H, count=42, y0=3, y1=FILL_H - 3, min_len=2, max_len=5)
    slot_tile = fiber_mask(rng, PERIOD, FILL_H, count=30, y0=1, y1=FILL_H - 1, min_len=2, max_len=6)

    # ---- 本番5枚 ----
    frame = build_frame(pal, slot_tile)
    save_png(frame, GUI_DIR / "bar_frame.png")
    fills: dict[str, np.ndarray] = {}
    for suffix, _era, _color, pname in ERAS:
        fills[suffix] = build_fill(hex_to_rgb(pal[pname]), hex_to_rgb(pal["dark"]), fill_tile)
        save_png(fills[suffix], GUI_DIR / f"bar_fill_{suffix}.png")

    # ---- プレビュー（本番画像を最近傍で4倍）----
    save_png(upscale(compose(frame, None, 0)), PREVIEW_DIR / "preview_empty.png")
    for suffix, _era, _color, _p in ERAS:
        save_png(upscale(compose(frame, fills[suffix], STEPS // 2)), PREVIEW_DIR / f"preview_half_{suffix}.png")
        save_png(upscale(compose(frame, fills[suffix], STEPS)), PREVIEW_DIR / f"preview_full_{suffix}.png")

    # ---- manifest.json の files（既存の同名は置き換え、無ければ末尾に追加）----
    new_entries = [manifest_entry("bar_frame.png", "世界の時代バーの枠（空の状態）")]
    for suffix, era, color, _p in ERAS:
        new_entries.append(manifest_entry(f"bar_fill_{suffix}.png", f"世界の時代バーの中身（{era}・bossbar color {color}）"))
    by_file = {e["file"]: e for e in new_entries}
    files = [e for e in m["files"] if e["file"] not in by_file]
    files.extend(new_entries)
    m["files"] = files
    save_manifest(m)

    # ---- BOSSBAR_SPEC.md ----
    derived = [("slot", SLOT_DARK_DELTA, rgb_to_hex(derive(hex_to_rgb(pal["slot"]), SLOT_DARK_DELTA)))]
    for _s, _e, _c, pname in ERAS:
        base = hex_to_rgb(pal[pname])
        derived.append((pname, FILL_LIGHT_DELTA, rgb_to_hex(derive(base, FILL_LIGHT_DELTA))))
        derived.append((pname, FILL_DARK_DELTA, rgb_to_hex(derive(base, FILL_DARK_DELTA))))
    req_sha = hashlib.sha256(REQUEST_MD.read_bytes()).hexdigest() if REQUEST_MD.exists() else "(docs/Fable依頼文_ボスバー_v3.md が見つからない)"

    use_note = {"outer": "枠の最外周 1px", "light": "枠の凸ベベル上/左・凹ベベル下/右", "dark": "枠の凸ベベル下/右・凹ベベル上/左",
                "panel": "枠の本体", "slot": "空の溝"}
    use_note["dark"] += "・目盛り（溝と fill の各120pxタイル先頭4列）"
    dnote = {SLOT_DARK_DELTA: "溝の繊維ノイズ", FILL_LIGHT_DELTA: "fill 上端2行", FILL_DARK_DELTA: "fill 下端2行と繊維ノイズ"}
    L = []
    L += ["# BOSSBAR_SPEC ── 世界の時代バー（ボスバー）", "",
          "`clientmod/tools/bar_tsukuru.py` が生成。手で編集しない。", "",
          "## 1. 使った palette 色", "", "| 名前 | 16進 | 用途 |", "|---|---|---|"]
    for k in FRAME_ROLES:
        L.append(f"| `{k}` | `{pal[k]}` | {use_note[k]} |")
    for suffix, era, _c, pname in ERAS:
        L.append(f"| `{pname}` | `{pal[pname]}` | `bar_fill_{suffix}.png` の基色（{era}） |")
    L += ["", "## 2. 派生色", "",
          f"基色の各チャンネルに差分を足して 0〜255 に丸めた物。基色ごと2つまで、差分は ±{DELTA_LIMIT} 以内。",
          "`tests/bar_kakunin.py` の検査9はこの表を読む。", "",
          "| 基色名 | 差分 | 16進 | 用途 |", "|---|---|---|---|"]
    for name, delta, hx in derived:
        L.append(f"| `{name}` | {delta:+d} | `{hx}` | {dnote[delta]} |")
    L += ["", "## 3. レンダラ契約（クライアントMOD側が守る物）", "",
          "| bossbar color | ファイル | 時代 |", "|---|---|---|"]
    for suffix, era, color, _p in ERAS:
        L.append(f"| `{color}` | `bar_fill_{suffix}.png` | {era} |")
    L += ["",
          f"- 枠 `bar_frame.png` は {FRAME_W}×{FRAME_H} テクスチャpx を **182×8 論理px** に描く。9分割しない",
          f"- 角丸: 枠の外周は半径 {OUTER_RADIUS}（2論理px）、溝と fill は半径 {GROOVE_RADIUS}（1論理px）。角の外は透明。fill は各角3画素が透明",
          f"- 目盛り: `dark` 色・幅 {TICK_W}px・全高。溝と fill の x mod {PERIOD} < {TICK_W} の列。切り出し位置と一致する",
          "- fill はアルファ 0 の画素を描かない（角の3画素。通常の RGBA ブレンドで良い）",
          f"- fill は枠の **({BORDER},{BORDER})** に置く（テクスチャpx。論理pxなら (1,1)）",
          f"- fill の切り出し幅（テクスチャpx）= **{FILL_W} × value ÷ max**、左端から。max={STEPS} なら {PERIOD}px 刻み",
          f"- 溝の論理幅は **{FILL_W // PREVIEW_SCALE}**。バニラの 182 を使うと右端が枠にかかる",
          "- 時代の名前は Minecraft が実行時に描く。テクスチャに文字は無い",
          f"- 他の色（例: `red`/`green`/`pink`）が来た時の扱いは未定義。MOD側で `{ERAS[0][2]}` に倒すか非表示にするかを決める",
          "", "## 4. 生成条件", "",
          f"- 乱数の種: `{SEED}`（`numpy.random.default_rng`）",
          f"- 繊維ノイズは {PERIOD}×{FILL_H} のタイルを {STEPS} 回並べた物。繊維はタイルの端をまたがないので {PERIOD}px のどの境目で切っても繊維が途中で切れない",
          "- 4枚の fill は同じタイル・同じ形・同じ目盛り位置。色だけ違う",
          "- プレビューは本番画像を最近傍で4倍にした物（別に描いていない）",
          "", "## 5. 指示文", "",
          f"- `docs/Fable依頼文_ボスバー_v3.md` sha256: `{req_sha}`", ""]
    SPEC_MD.write_bytes(("\n".join(L)).encode("utf-8"))

    print("wrote:", ", ".join(p.name for p in sorted(GUI_DIR.glob("bar_*.png"))))
    print("wrote:", str(PREVIEW_DIR.relative_to(ROOT)) + "/ (9 files)")
    print("wrote:", SPEC_MD.relative_to(ROOT), "/", MANIFEST.relative_to(ROOT), f"(files={len(files)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
