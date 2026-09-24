# BOSSBAR_SPEC ── 世界の時代バー（ボスバー）

`clientmod/tools/bar_tsukuru.py` が生成。手で編集しない。

## 1. 使った palette 色

| 名前 | 16進 | 用途 |
|---|---|---|
| `outer` | `#102329` | 枠の最外周 1px |
| `light` | `#B2A577` | 枠の凸ベベル上/左・凹ベベル下/右 |
| `dark` | `#1B282B` | 枠の凸ベベル下/右・凹ベベル上/左・目盛り（溝と fill の各120pxタイル先頭4列） |
| `panel` | `#685E45` | 枠の本体 |
| `slot` | `#38474A` | 空の溝 |
| `muted` | `#AEB8AE` | `bar_fill_tekki.png` の基色（鉄器） |
| `brass` | `#C4934B` | `bar_fill_chusei.png` の基色（中世） |
| `info` | `#4FA1B2` | `bar_fill_kindai.png` の基色（近代） |
| `era_gendai` | `#B9A7D0` | `bar_fill_gendai.png` の基色（現代） |

## 2. 派生色

基色の各チャンネルに差分を足して 0〜255 に丸めた物。基色ごと2つまで、差分は ±24 以内。
`tests/bar_kakunin.py` の検査9はこの表を読む。

| 基色名 | 差分 | 16進 | 用途 |
|---|---|---|---|
| `slot` | -12 | `#2C3B3E` | 溝の繊維ノイズ |
| `muted` | +20 | `#C2CCC2` | fill 上端2行 |
| `muted` | -20 | `#9AA49A` | fill 下端2行と繊維ノイズ |
| `brass` | +20 | `#D8A75F` | fill 上端2行 |
| `brass` | -20 | `#B07F37` | fill 下端2行と繊維ノイズ |
| `info` | +20 | `#63B5C6` | fill 上端2行 |
| `info` | -20 | `#3B8D9E` | fill 下端2行と繊維ノイズ |
| `era_gendai` | +20 | `#CDBBE4` | fill 上端2行 |
| `era_gendai` | -20 | `#A593BC` | fill 下端2行と繊維ノイズ |

## 3. レンダラ契約（クライアントMOD側が守る物）

| bossbar color | ファイル | 時代 |
|---|---|---|
| `white` | `bar_fill_tekki.png` | 鉄器 |
| `yellow` | `bar_fill_chusei.png` | 中世 |
| `blue` | `bar_fill_kindai.png` | 近代 |
| `purple` | `bar_fill_gendai.png` | 現代 |

- 枠 `bar_frame.png` は 728×32 テクスチャpx を **182×8 論理px** に描く。9分割しない
- 角丸: 枠の外周は半径 8（2論理px）、溝と fill は半径 4（1論理px）。角の外は透明。fill は各角3画素が透明
- 目盛り: `dark` 色・幅 4px・全高。溝と fill の x mod 120 < 4 の列。切り出し位置と一致する
- fill はアルファ 0 の画素を描かない（角の3画素。通常の RGBA ブレンドで良い）
- fill は枠の **(4,4)** に置く（テクスチャpx。論理pxなら (1,1)）
- fill の切り出し幅（テクスチャpx）= **720 × value ÷ max**、左端から。max=6 なら 120px 刻み
- 溝の論理幅は **180**。バニラの 182 を使うと右端が枠にかかる
- 時代の名前は Minecraft が実行時に描く。テクスチャに文字は無い
- 他の色（例: `red`/`green`/`pink`）が来た時の扱いは未定義。MOD側で `white` に倒すか非表示にするかを決める

## 4. 生成条件

- 乱数の種: `20260903`（`numpy.random.default_rng`）
- 繊維ノイズは 120×24 のタイルを 6 回並べた物。繊維はタイルの端をまたがないので 120px のどの境目で切っても繊維が途中で切れない
- 4枚の fill は同じタイル・同じ形・同じ目盛り位置。色だけ違う
- プレビューは本番画像を最近傍で4倍にした物（別に描いていない）

## 5. 指示文

- `docs/Fable依頼文_ボスバー_v3.md` sha256: `32cd16bb319f41894923a63de33e8737223019870930df2123d60f00331c5ad4`
