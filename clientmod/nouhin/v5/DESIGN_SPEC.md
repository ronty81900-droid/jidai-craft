# 時代クラフト 特殊アイテム v5 — DESIGN_SPEC

勝利条件となる特殊アイテム5種の16×16テクスチャです。5種すべてを同じ `#102329` の1px輪郭と左上光源で統一し、素材固有色で遠目の識別性を分けています。

## 共通作図規約

- 本体は最初から `16×16` RGBAキャンバスへ整数ピクセルで直接描画しています。大きな絵からの縮小ではありません。
- 背景は `#00000000`。不透明度は0または255だけです。半透明グラデーションはありません。
- 共通外周は `#102329` の1px。透明画素へ8近傍で接する全不透明画素がこの色であることを検査しています。
- 光源は左上。各画像でハイライト画素の重心が影画素より左かつ上にあることを検査しています。
- 画像生成モデル、写真質感、ぼかし、レンズフレア、文字、疑似文字、アンチエイリアス、乱数は使用していません。
- `preview/` は完成16×16をnearest-neighborで厳密に8倍した128×128確認画像です。

## 正本対応表

| # | 時代 | 正式名 | 本体ファイル | 識別色 |
|---:|---|---|---|---|
| 1 | 鉄器 | **古びた護符** | `assets/jidaiui/textures/item/gofu.png` | 土色 |
| 2 | 中世 | **聖杯の欠片** | `assets/jidaiui/textures/item/seihai.png` | 金＋青い宝石 |
| 3 | 中世 | **羊皮紙の海図** | `assets/jidaiui/textures/item/kaizu.png` | 生成り＋Navigation青緑 |
| 4 | 近代 | **蒸気機関の歯車** | `assets/jidaiui/textures/item/haguruma.png` | 真鍮＋煤 |
| 5 | 現代 | **月の石** | `assets/jidaiui/textures/item/tsukinoishi.png` | 灰＋青緑ケース |

## 1. 古びた護符 — `gofu.png`

- 時代: **鉄器**
- 意図: 石か骨の小さな護符。紐を通した透明穴と、右下に残る土汚れで埋土品と分かる。
- 光源: 左上。右下へ素材影を集めています。
- 使用色: **10色**（透明色を含む）

| 役割 | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 131 |
| `outline` | `#102329FF` | 62 |
| `cord_shadow` | `#3C3025FF` | 2 |
| `cord_base` | `#5B4632FF` | 2 |
| `cord_light` | `#806548FF` | 2 |
| `stone_shadow` | `#514235FF` | 8 |
| `soil` | `#69452FFF` | 4 |
| `stone_base` | `#8B7250FF` | 34 |
| `stone_light` | `#BDA77AFF` | 5 |
| `bone_highlight` | `#D7C69AFF` | 6 |

## 2. 聖杯の欠片 — `seihai.png`

- 時代: **中世**
- 意図: 杯の上縁から片側だけ残った湾曲破片。脚や台座は描かず、青い宝石を1つだけ残す。
- 光源: 左上。右下へ素材影を集めています。
- 使用色: **10色**（透明色を含む）

| 役割 | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 157 |
| `outline` | `#102329FF` | 51 |
| `gold_deep` | `#5E3E1FFF` | 5 |
| `gold_shadow` | `#8A5E27FF` | 9 |
| `gold_base` | `#C4934BFF` | 17 |
| `gold_light` | `#E1B95FFF` | 6 |
| `gold_highlight` | `#FFE29AFF` | 8 |
| `jewel_shadow` | `#24545EFF` | 1 |
| `jewel_base` | `#4FA1B2FF` | 1 |
| `jewel_highlight` | `#A5D6D8FF` | 1 |

## 3. 羊皮紙の海図 — `kaizu.png`

- 時代: **中世**
- 意図: 巻いた羊皮紙を少し開き、段状の海岸線と5点の方位記号を見せる。文字や疑似文字は置かない。
- 光源: 左上。右下へ素材影を集めています。
- 使用色: **10色**（透明色を含む）

| 役割 | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 100 |
| `outline` | `#102329FF` | 52 |
| `parchment_deep` | `#6C583BFF` | 10 |
| `parchment_shadow` | `#9A7D50FF` | 12 |
| `parchment_base` | `#C9AE76FF` | 52 |
| `parchment_light` | `#E5D09CFF` | 13 |
| `parchment_highlight` | `#F5E8C8FF` | 6 |
| `coast_deep` | `#17313AFF` | 5 |
| `coast_light` | `#4FA1B2FF` | 2 |
| `bearing_brass` | `#C4934BFF` | 4 |

## 4. 蒸気機関の歯車 — `haguruma.png`

- 時代: **近代**
- 意図: 真鍮リングと8方向の歯。右上の歯を欠けさせ、右下へ煤と赤錆を限定して使い込みを示す。
- 光源: 左上。右下へ素材影を集めています。
- 使用色: **9色**（透明色を含む）

| 役割 | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 102 |
| `outline` | `#102329FF` | 72 |
| `metal_deep` | `#3E3527FF` | 11 |
| `brass_shadow` | `#5D482CFF` | 25 |
| `brass_base` | `#C4934BFF` | 15 |
| `brass_light` | `#D8AD63FF` | 9 |
| `brass_highlight` | `#F1D48BFF` | 17 |
| `soot` | `#242527FF` | 2 |
| `rust` | `#824B32FF` | 3 |

## 5. 月の石 — `tsukinoishi.png`

- 時代: **現代**
- 意図: 不透明の青緑ガラス表現と台座で囲った灰色岩石標本。中央の小さなクレーターで月岩らしさを出す。
- 光源: 左上。右下へ素材影を集めています。
- 使用色: **13色**（透明色を含む）

| 役割 | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 88 |
| `outline` | `#102329FF` | 52 |
| `glass_deep` | `#203A40FF` | 1 |
| `glass_shadow` | `#24545EFF` | 13 |
| `glass_base` | `#4FA1B2FF` | 24 |
| `glass_highlight` | `#A5D6D8FF` | 7 |
| `rock_deep` | `#3A4347FF` | 28 |
| `rock_shadow` | `#596268FF` | 5 |
| `rock_base` | `#81898BFF` | 17 |
| `rock_light` | `#B4BCB8FF` | 3 |
| `rock_highlight` | `#E1E5DEFF` | 6 |
| `base_shadow` | `#1B282BFF` | 10 |
| `bearing_brass` | `#C4934BFF` | 2 |

## manifest と実装

- ResourceLocationは `jidaiui:textures/item/<filename>`。モデルJSON・言語ファイル・MOD登録コードは納品に含めていません。
- `manifest.json` の `files` は、本体5枚とpreview 5枚の計10 PNGをすべて登録しています。
- `rgba_color_count` は透明 `#00000000` を含むRGBA組の数です。
- SHA-256はPNGのraw byte列から計算し、大文字64桁で記録しています。

## 検査結果

- PASS: no font import, raster text call, random drawing, blur or image-generation input
- PASS: gofu.png: 16x16 RGBA drawn at native resolution
- PASS: gofu.png: transparent background with alpha exactly 0/255
- PASS: gofu.png: 10 RGBA colors including transparent (<=32)
- PASS: gofu.png: common #102329 boundary verified on 56 pixels
- PASS: gofu.png: single connected silhouette with transparent canvas margin
- PASS: gofu.png: highlight centroid is left and above shadow centroid
- PASS: preview/gofu.png: exact nearest-neighbor 8x copy
- PASS: seihai.png: 16x16 RGBA drawn at native resolution
- PASS: seihai.png: transparent background with alpha exactly 0/255
- PASS: seihai.png: 10 RGBA colors including transparent (<=32)
- PASS: seihai.png: common #102329 boundary verified on 44 pixels
- PASS: seihai.png: single connected silhouette with transparent canvas margin
- PASS: seihai.png: highlight centroid is left and above shadow centroid
- PASS: preview/seihai.png: exact nearest-neighbor 8x copy
- PASS: kaizu.png: 16x16 RGBA drawn at native resolution
- PASS: kaizu.png: transparent background with alpha exactly 0/255
- PASS: kaizu.png: 10 RGBA colors including transparent (<=32)
- PASS: kaizu.png: common #102329 boundary verified on 52 pixels
- PASS: kaizu.png: single connected silhouette with transparent canvas margin
- PASS: kaizu.png: highlight centroid is left and above shadow centroid
- PASS: preview/kaizu.png: exact nearest-neighbor 8x copy
- PASS: haguruma.png: 16x16 RGBA drawn at native resolution
- PASS: haguruma.png: transparent background with alpha exactly 0/255
- PASS: haguruma.png: 9 RGBA colors including transparent (<=32)
- PASS: haguruma.png: common #102329 boundary verified on 72 pixels
- PASS: haguruma.png: single connected silhouette with transparent canvas margin
- PASS: haguruma.png: highlight centroid is left and above shadow centroid
- PASS: preview/haguruma.png: exact nearest-neighbor 8x copy
- PASS: tsukinoishi.png: 16x16 RGBA drawn at native resolution
- PASS: tsukinoishi.png: transparent background with alpha exactly 0/255
- PASS: tsukinoishi.png: 13 RGBA colors including transparent (<=32)
- PASS: tsukinoishi.png: common #102329 boundary verified on 52 pixels
- PASS: tsukinoishi.png: single connected silhouette with transparent canvas margin
- PASS: tsukinoishi.png: highlight centroid is left and above shadow centroid
- PASS: preview/tsukinoishi.png: exact nearest-neighbor 8x copy
- PASS: all five production textures have different pixel arrays
- PASS: all 10 saved PNGs re-open as RGBA with alpha exactly [0,255]
- PASS: maximum production texture color count is 13 including transparency
- PASS: manifest byte sizes, SHA-256, dimensions, RGBA colors and alpha values are measured from saved bytes
- 本体最大色数: 13 / 32
- PNG登録数: 10（本体5 + preview 5）
- ZIPには本書1本、manifest 1本、PNG 10枚だけを入れます。
