# 時代クラフト UI v4 — Navigation 設計書

この納品は、ForgeクライアントMODが `320×240` 論理px上で直接組み立てるための、Navigation（航海・測量）案1種です。旧リソースパック方式、チェスト固定幅、フォントグリフ、負の幅、文字入りPNGは使用しません。

PNGは固定パレット、整数座標の矩形、1px線、等間隔stamp、鏡像座標だけで決定的に生成しました。画像生成モデル、アンチエイリアス、半透明、ぼかし、疑似文字、焼き込み文字は使っていません。

> 依頼文が言及する実機スクリーンショットは添付データ内に存在しませんでした。試作コードは方式と寸法の確認にだけ使い、見た目の参照にはしていません。

## 座標・描画規約

- 全座標は左上原点の論理pxです。矩形は `(x, y, width, height)`、右端と下端を含まない半開区間です。
- 全8ページの合成ベースは `320×240`。どのページもスクロール不要です。スクロール棒素材も含めていません。
- 広い画面ではMOD側が全体を最大2倍まで拡大し、テクスチャフィルタは必ず `nearest` にします。
- 合成順は `ページベース → 現在状態のカード素材 → ゲーム側アイコン → 実行時文字 → 吹き出し` です。
- `pressed` だけ、ゲーム側アイコンと文字を `(1,1)` ずらします。
- ResourceLocation は `jidaiui:textures/gui/<filename>` です。

## 文字規約

文字はすべてMinecraft標準フォントをMODから実行時描画します。PNG内の空き領域は完全な単色です。

| 指定 | 実行倍率 | 行送り | 用途 |
|---:|---:|---:|---|
| 8px | 1.0 | 10px | 商品名、価格、解禁、所持金、補足 |
| 12px | 1.5 | 14px | 長い画面見出し、生活カード名、時代進行条件名、ガチャ価格 |
| 16px | 2.0 | 18px | 短い画面見出し |

名前は最大2行です。日本語は文字境界でも折り返し可能にし、英数字を含む最長名もカードから出さないでください。`locked` は文字色 `#AEB8AE`、`insufficient` は価格文字だけ `#C15A49` に上書きします。

## 固定パレット

| 名称 | 色 |
|---|---|
| `outer` | `#102329` |
| `background` | `#17313A` |
| `grid` | `#233F45` |
| `panel` | `#685E45` |
| `panel2` | `#49493B` |
| `slot` | `#38474A` |
| `light` | `#B2A577` |
| `dark` | `#1B282B` |
| `danger` | `#C15A49` |
| `brass` | `#C4934B` |
| `info` | `#4FA1B2` |
| `success` | `#77B789` |
| `text` | `#F5E8C8` |
| `muted` | `#AEB8AE` |

## 9分割枠

`outer_frame_9slice.png`、`panel_9slice.png`、`tooltip_9slice.png` はすべて24×24で、8×8の9マスです。

| マス | アトラス内 `(x,y,w,h)` | 配置 |
|---|---|---|
| 左上 | `(0,0,8,8)` | 原寸 |
| 上 | `(8,0,8,8)` | x反復 |
| 右上 | `(16,0,8,8)` | 原寸 |
| 左 | `(0,8,8,8)` | y反復 |
| 中 | `(8,8,8,8)` | x・y反復 |
| 右 | `(16,8,8,8)` | y反復 |
| 左下 | `(0,16,8,8)` | 原寸 |
| 下 | `(8,16,8,8)` | x反復 |
| 右下 | `(16,16,8,8)` | 原寸 |

引き伸ばしは禁止です。端数は最後の8pxタイルを切り取ります。四隅の鏡像一致、上下辺の横周期、左右辺の縦周期、中央の縦横周期を機械検査済みです。

## カード寸法

| family | 寸法 | アイコン井戸 | ゲームアイコン | 名前 | 価格/値 | 解禁/補助 |
|---|---|---|---|---|---|---|
| `micro` | `69×52` | `(4, 5, 18, 18)` | `(5, 6, 16, 16)` | `(4, 29, 61, 20)` | `(24, 4, 41, 10)` | `(24, 16, 41, 10)` |
| `compact` | `94×52` | `(5, 5, 18, 18)` | `(6, 6, 16, 16)` | `(27, 4, 62, 20)` | `(27, 27, 62, 10)` | `(5, 29, 18, 10)` |
| `tall` | `94×76` | `(5, 10, 34, 34)` | `(6, 11, 32, 32)` | `(43, 7, 46, 20)` | `(43, 31, 46, 10)` | `(43, 47, 46, 10)` |
| `large` | `142×76` | `(8, 12, 34, 34)` | `(9, 13, 32, 32)` | `(50, 8, 84, 28)` | `(50, 41, 84, 10)` | `(50, 57, 84, 10)` |
| `danger` | `142×76` | `(8, 8, 34, 34)` | `(9, 9, 32, 32)` | `(8, 48, 126, 20)` | `(50, 8, 84, 10)` | `(50, 23, 84, 10)` |

上表はカード左上を原点にした相対座標です。`micro` と `compact` は16×16、`tall`・`large`・`danger` は32×32のゲームアイコンを置きます。アイコン領域には格子も装飾もありません。

## 状態

| 素材suffix | 日本語 | 見え方と実装 |
|---|---|---|
| `normal` | ふつう | 深青緑面と1pxの測量枠 |
| `hover` | カーソルが乗っている | 真鍮の明枠と四隅の青い測点。ぼかしなし |
| `pressed` | 押している | 面を暗くしベベル反転。文字とアイコンを `+1,+1` |
| `locked` | まだ買えない（未解禁） | 暗い面。解禁/理由文字はMOD側で描画 |
| `insufficient` | お金が足りない | 価格横に赤い測量マーカー。価格文字も `#C15A49` |

優先順は `locked → pressed → hover → insufficient → normal`。不足中にhover/pressedなら枠はその状態、価格の赤だけ維持します。タブは `tab_selected.png` / `tab_unselected.png`、ラベル相対領域は `(6,4,50,10)`、8px中央揃えです。

## 吹き出し

`tooltip_9slice.png` を推奨176×36へタイル展開し、文字領域を相対 `(8,8,160,20)`、8px・最大2行とします。配置はカーソル `+(8,8)` を基本に、画面外へ出る場合は反転して320×240へクランプします。銃・弾・貴金属の補足は下記の文字列をそのまま表示します。

## 画面別仕様

### `shop_life` — 販売所

合成ベース: `assets/jidaiui/textures/gui/page_shop_life.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `tab_1` | `assets/jidaiui/textures/gui/tab_selected.png` | `(20, 38, 62, 18)` | — |
| `tab_2` | `assets/jidaiui/textures/gui/tab_unselected.png` | `(84, 38, 62, 18)` | — |
| `item_1` | `assets/jidaiui/textures/gui/card_large_normal.png` | `(16, 60, 142, 76)` | (25, 73, 32, 32) |
| `item_2` | `assets/jidaiui/textures/gui/card_large_normal.png` | `(162, 60, 142, 76)` | (171, 73, 32, 32) |
| `item_3` | `assets/jidaiui/textures/gui/card_large_normal.png` | `(16, 142, 142, 76)` | (25, 155, 32, 32) |
| `item_4` | `assets/jidaiui/textures/gui/card_large_normal.png` | `(162, 142, 142, 76)` | (171, 155, 32, 32) |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `販売所` | `(20, 14, 90, 18)` | 16 | `#F5E8C8` | 1行 / left |
| `tab_1_label` | `生活` | `(26, 42, 50, 10)` | 8 | `#F5E8C8` | 1行 / center |
| `tab_2_label` | `防具` | `(90, 42, 50, 10)` | 8 | `#AEB8AE` | 1行 / center |
| `wallet_personal` | `個人 {value}` | `(116, 17, 90, 10)` | 8 | `#4FA1B2` | 1行 / right |
| `wallet_faction` | `勢力 {value}` | `(210, 17, 90, 10)` | 8 | `#4FA1B2` | 1行 / right |
| `item_1_name` | `パン ×3` | `(66, 68, 84, 28)` | 12 | `#F5E8C8` | 2行 / left |
| `item_1_price` | `個人 2` | `(66, 101, 84, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_1_unlock` | `鉄器` | `(66, 117, 84, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_2_name` | `石炭` | `(212, 68, 84, 28)` | 12 | `#F5E8C8` | 2行 / left |
| `item_2_price` | `個人 5` | `(212, 101, 84, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_2_unlock` | `鉄器` | `(212, 117, 84, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_3_name` | `戦争宣誓` | `(66, 150, 84, 28)` | 12 | `#F5E8C8` | 2行 / left |
| `item_3_price` | `勢力 100` | `(66, 183, 84, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_3_unlock` | `中世` | `(66, 199, 84, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_4_name` | `下剋上` | `(212, 150, 84, 28)` | 12 | `#F5E8C8` | 2行 / left |
| `item_4_price` | `勢力 150` | `(212, 183, 84, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_4_unlock` | `近代` | `(212, 199, 84, 10)` | 8 | `#AEB8AE` | 1行 / left |

カード正本:

| id | 名前 | 価格/値 | 解禁 | カード座標 | 補足 |
|---|---|---|---|---|---|
| `item_1` | パン ×3 | 個人 2 | 鉄器 | `(16, 60, 142, 76)` | — |
| `item_2` | 石炭 | 個人 5 | 鉄器 | `(162, 60, 142, 76)` | — |
| `item_3` | 戦争宣誓 | 勢力 100 | 中世 | `(16, 142, 142, 76)` | — |
| `item_4` | 下剋上 | 勢力 150 | 近代 | `(162, 142, 142, 76)` | — |

- 全件を320×240内へ収めたため縦スクロールは使わない。
- locked状態では解禁欄を理由表示に使い、insufficient状態では価格文字だけを#C15A49へ変更する。

### `shop_armor` — 販売所

合成ベース: `assets/jidaiui/textures/gui/page_shop_armor.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `tab_1` | `assets/jidaiui/textures/gui/tab_unselected.png` | `(20, 38, 62, 18)` | — |
| `tab_2` | `assets/jidaiui/textures/gui/tab_selected.png` | `(84, 38, 62, 18)` | — |
| `item_1` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(16, 60, 69, 52)` | (21, 66, 16, 16) |
| `item_2` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(89, 60, 69, 52)` | (94, 66, 16, 16) |
| `item_3` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(162, 60, 69, 52)` | (167, 66, 16, 16) |
| `item_4` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(235, 60, 69, 52)` | (240, 66, 16, 16) |
| `item_5` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(16, 114, 69, 52)` | (21, 120, 16, 16) |
| `item_6` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(89, 114, 69, 52)` | (94, 120, 16, 16) |
| `item_7` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(162, 114, 69, 52)` | (167, 120, 16, 16) |
| `item_8` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(235, 114, 69, 52)` | (240, 120, 16, 16) |
| `item_9` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(16, 168, 69, 52)` | (21, 174, 16, 16) |
| `item_10` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(89, 168, 69, 52)` | (94, 174, 16, 16) |
| `item_11` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(162, 168, 69, 52)` | (167, 174, 16, 16) |
| `item_12` | `assets/jidaiui/textures/gui/card_micro_normal.png` | `(235, 168, 69, 52)` | (240, 174, 16, 16) |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `販売所` | `(20, 14, 90, 18)` | 16 | `#F5E8C8` | 1行 / left |
| `tab_1_label` | `生活` | `(26, 42, 50, 10)` | 8 | `#AEB8AE` | 1行 / center |
| `tab_2_label` | `防具` | `(90, 42, 50, 10)` | 8 | `#F5E8C8` | 1行 / center |
| `wallet_faction` | `勢力 {value}` | `(192, 17, 108, 10)` | 8 | `#4FA1B2` | 1行 / right |
| `item_1_name` | `チェーンの兜` | `(20, 89, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_1_price` | `勢力 15` | `(40, 64, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_1_unlock` | `鉄器` | `(40, 76, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_2_name` | `チェーンの胸当て` | `(93, 89, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_2_price` | `勢力 25` | `(113, 64, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_2_unlock` | `鉄器` | `(113, 76, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_3_name` | `チェーンの脚当て` | `(166, 89, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_3_price` | `勢力 20` | `(186, 64, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_3_unlock` | `鉄器` | `(186, 76, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_4_name` | `チェーンの靴` | `(239, 89, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_4_price` | `勢力 10` | `(259, 64, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_4_unlock` | `鉄器` | `(259, 76, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_5_name` | `鉄の兜` | `(20, 143, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_5_price` | `勢力 25` | `(40, 118, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_5_unlock` | `中世` | `(40, 130, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_6_name` | `鉄の胸当て` | `(93, 143, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_6_price` | `勢力 30` | `(113, 118, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_6_unlock` | `中世` | `(113, 130, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_7_name` | `鉄の脚当て` | `(166, 143, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_7_price` | `勢力 30` | `(186, 118, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_7_unlock` | `中世` | `(186, 130, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_8_name` | `鉄の靴` | `(239, 143, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_8_price` | `勢力 20` | `(259, 118, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_8_unlock` | `中世` | `(259, 130, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_9_name` | `ダイヤの兜` | `(20, 197, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_9_price` | `勢力 300` | `(40, 172, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_9_unlock` | `現代` | `(40, 184, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_10_name` | `ダイヤの胸当て` | `(93, 197, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_10_price` | `勢力 500` | `(113, 172, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_10_unlock` | `近代` | `(113, 184, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_11_name` | `ダイヤの脚当て` | `(166, 197, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_11_price` | `勢力 400` | `(186, 172, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_11_unlock` | `現代` | `(186, 184, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_12_name` | `ダイヤの靴` | `(239, 197, 61, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_12_price` | `勢力 200` | `(259, 172, 41, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_12_unlock` | `現代` | `(259, 184, 41, 10)` | 8 | `#AEB8AE` | 1行 / left |

カード正本:

| id | 名前 | 価格/値 | 解禁 | カード座標 | 補足 |
|---|---|---|---|---|---|
| `item_1` | チェーンの兜 | 勢力 15 | 鉄器 | `(16, 60, 69, 52)` | — |
| `item_2` | チェーンの胸当て | 勢力 25 | 鉄器 | `(89, 60, 69, 52)` | — |
| `item_3` | チェーンの脚当て | 勢力 20 | 鉄器 | `(162, 60, 69, 52)` | — |
| `item_4` | チェーンの靴 | 勢力 10 | 鉄器 | `(235, 60, 69, 52)` | — |
| `item_5` | 鉄の兜 | 勢力 25 | 中世 | `(16, 114, 69, 52)` | — |
| `item_6` | 鉄の胸当て | 勢力 30 | 中世 | `(89, 114, 69, 52)` | — |
| `item_7` | 鉄の脚当て | 勢力 30 | 中世 | `(162, 114, 69, 52)` | — |
| `item_8` | 鉄の靴 | 勢力 20 | 中世 | `(235, 114, 69, 52)` | — |
| `item_9` | ダイヤの兜 | 勢力 300 | 現代 | `(16, 168, 69, 52)` | — |
| `item_10` | ダイヤの胸当て | 勢力 500 | 近代 | `(89, 168, 69, 52)` | — |
| `item_11` | ダイヤの脚当て | 勢力 400 | 現代 | `(162, 168, 69, 52)` | — |
| `item_12` | ダイヤの靴 | 勢力 200 | 現代 | `(235, 168, 69, 52)` | — |

- 全件を320×240内へ収めたため縦スクロールは使わない。
- locked状態では解禁欄を理由表示に使い、insufficient状態では価格文字だけを#C15A49へ変更する。

### `gunshop_guns` — 銃器専門店

合成ベース: `assets/jidaiui/textures/gui/page_gunshop_guns.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `tab_1` | `assets/jidaiui/textures/gui/tab_selected.png` | `(20, 38, 62, 18)` | — |
| `tab_2` | `assets/jidaiui/textures/gui/tab_unselected.png` | `(84, 38, 62, 18)` | — |
| `item_1` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(16, 60, 94, 52)` | (22, 66, 16, 16) |
| `item_2` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(113, 60, 94, 52)` | (119, 66, 16, 16) |
| `item_3` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(210, 60, 94, 52)` | (216, 66, 16, 16) |
| `item_4` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(16, 114, 94, 52)` | (22, 120, 16, 16) |
| `item_5` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(113, 114, 94, 52)` | (119, 120, 16, 16) |
| `item_6` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(210, 114, 94, 52)` | (216, 120, 16, 16) |
| `item_7` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(16, 168, 94, 52)` | (22, 174, 16, 16) |
| `item_8` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(113, 168, 94, 52)` | (119, 174, 16, 16) |
| `item_9` | `assets/jidaiui/textures/gui/card_compact_normal.png` | `(210, 168, 94, 52)` | (216, 174, 16, 16) |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `銃器専門店` | `(20, 16, 132, 14)` | 12 | `#F5E8C8` | 1行 / left |
| `tab_1_label` | `銃` | `(26, 42, 50, 10)` | 8 | `#F5E8C8` | 1行 / center |
| `tab_2_label` | `弾` | `(90, 42, 50, 10)` | 8 | `#AEB8AE` | 1行 / center |
| `wallet_faction` | `勢力 {value}` | `(192, 17, 108, 10)` | 8 | `#4FA1B2` | 1行 / right |
| `item_1_name` | `コルト M1851 リボルバー` | `(43, 64, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_1_price` | `勢力 800` | `(43, 87, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_1_unlock` | `中世` | `(21, 89, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_2_name` | `コルト M1873 リボルバー` | `(140, 64, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_2_price` | `勢力 1,000` | `(140, 87, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_2_unlock` | `近代` | `(118, 89, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_3_name` | `SKS 半自動小銃` | `(237, 64, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_3_price` | `勢力 3,000` | `(237, 87, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_3_unlock` | `近代` | `(215, 89, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_4_name` | `M1 ガーランド` | `(43, 118, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_4_price` | `勢力 5,000` | `(43, 141, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_4_unlock` | `近代` | `(21, 143, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_5_name` | `マドセン軽機関銃` | `(140, 118, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_5_price` | `勢力 7,000` | `(140, 141, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_5_unlock` | `近代` | `(118, 143, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_6_name` | `グロック 17` | `(237, 118, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_6_price` | `勢力 1,200` | `(237, 141, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_6_unlock` | `現代` | `(215, 143, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_7_name` | `UZI` | `(43, 172, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_7_price` | `勢力 4,000` | `(43, 195, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_7_unlock` | `現代` | `(21, 197, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_8_name` | `M4A1 カービン` | `(140, 172, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_8_price` | `勢力 8,000` | `(140, 195, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_8_unlock` | `現代` | `(118, 197, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_9_name` | `M107 対物狙撃銃` | `(237, 172, 62, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_9_price` | `勢力 12,000` | `(237, 195, 62, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_9_unlock` | `現代` | `(215, 197, 18, 10)` | 8 | `#AEB8AE` | 1行 / left |

カード正本:

| id | 名前 | 価格/値 | 解禁 | カード座標 | 補足 |
|---|---|---|---|---|---|
| `item_1` | コルト M1851 リボルバー | 勢力 800 | 中世 | `(16, 60, 94, 52)` | 弾: 小口径 / 装弾数 6 |
| `item_2` | コルト M1873 リボルバー | 勢力 1,000 | 近代 | `(113, 60, 94, 52)` | 弾: 中口径 / 装弾数 6 |
| `item_3` | SKS 半自動小銃 | 勢力 3,000 | 近代 | `(210, 60, 94, 52)` | 弾: 中口径 / 装弾数 10 |
| `item_4` | M1 ガーランド | 勢力 5,000 | 近代 | `(16, 114, 94, 52)` | 弾: 大口径 / 装弾数 8 |
| `item_5` | マドセン軽機関銃 | 勢力 7,000 | 近代 | `(113, 114, 94, 52)` | 弾: 大口径 / 装弾数 30 |
| `item_6` | グロック 17 | 勢力 1,200 | 現代 | `(210, 114, 94, 52)` | 弾: 9mm / 装弾数 17 |
| `item_7` | UZI | 勢力 4,000 | 現代 | `(16, 168, 94, 52)` | 弾: 9mm / 装弾数 20 |
| `item_8` | M4A1 カービン | 勢力 8,000 | 現代 | `(113, 168, 94, 52)` | 弾: 5.56mm / 装弾数 30 |
| `item_9` | M107 対物狙撃銃 | 勢力 12,000 | 現代 | `(210, 168, 94, 52)` | 弾: 50BMG / 装弾数 10 |

- 全件を320×240内へ収めたため縦スクロールは使わない。
- locked状態では解禁欄を理由表示に使い、insufficient状態では価格文字だけを#C15A49へ変更する。

### `gunshop_ammo` — 銃器専門店

合成ベース: `assets/jidaiui/textures/gui/page_gunshop_ammo.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `tab_1` | `assets/jidaiui/textures/gui/tab_unselected.png` | `(20, 38, 62, 18)` | — |
| `tab_2` | `assets/jidaiui/textures/gui/tab_selected.png` | `(84, 38, 62, 18)` | — |
| `item_1` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(16, 60, 94, 76)` | (22, 71, 32, 32) |
| `item_2` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(113, 60, 94, 76)` | (119, 71, 32, 32) |
| `item_3` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(210, 60, 94, 76)` | (216, 71, 32, 32) |
| `item_4` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(16, 142, 94, 76)` | (22, 153, 32, 32) |
| `item_5` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(113, 142, 94, 76)` | (119, 153, 32, 32) |
| `item_6` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(210, 142, 94, 76)` | (216, 153, 32, 32) |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `銃器専門店` | `(20, 16, 132, 14)` | 12 | `#F5E8C8` | 1行 / left |
| `tab_1_label` | `銃` | `(26, 42, 50, 10)` | 8 | `#AEB8AE` | 1行 / center |
| `tab_2_label` | `弾` | `(90, 42, 50, 10)` | 8 | `#F5E8C8` | 1行 / center |
| `wallet_personal` | `個人 {value}` | `(192, 17, 108, 10)` | 8 | `#4FA1B2` | 1行 / right |
| `item_1_name` | `小口径弾 30発` | `(59, 67, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_1_price` | `個人 15` | `(59, 91, 46, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_1_unlock` | `中世` | `(59, 107, 46, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_2_name` | `中口径弾 30発` | `(156, 67, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_2_price` | `個人 20` | `(156, 91, 46, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_2_unlock` | `近代` | `(156, 107, 46, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_3_name` | `大口径弾 30発` | `(253, 67, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_3_price` | `個人 30` | `(253, 91, 46, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_3_unlock` | `近代` | `(253, 107, 46, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_4_name` | `9mm弾 30発` | `(59, 149, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_4_price` | `個人 25` | `(59, 173, 46, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_4_unlock` | `現代` | `(59, 189, 46, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_5_name` | `5.56mm弾 30発` | `(156, 149, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_5_price` | `個人 40` | `(156, 173, 46, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_5_unlock` | `現代` | `(156, 189, 46, 10)` | 8 | `#AEB8AE` | 1行 / left |
| `item_6_name` | `50BMG弾 10発` | `(253, 149, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `item_6_price` | `個人 60` | `(253, 173, 46, 10)` | 8 | `#C4934B` | 1行 / left |
| `item_6_unlock` | `現代` | `(253, 189, 46, 10)` | 8 | `#AEB8AE` | 1行 / left |

カード正本:

| id | 名前 | 価格/値 | 解禁 | カード座標 | 補足 |
|---|---|---|---|---|---|
| `item_1` | 小口径弾 30発 | 個人 15 | 中世 | `(16, 60, 94, 76)` | コルト M1851 用 |
| `item_2` | 中口径弾 30発 | 個人 20 | 近代 | `(113, 60, 94, 76)` | コルト M1873 / SKS 用 |
| `item_3` | 大口径弾 30発 | 個人 30 | 近代 | `(210, 60, 94, 76)` | M1 ガーランド / マドセン 用 |
| `item_4` | 9mm弾 30発 | 個人 25 | 現代 | `(16, 142, 94, 76)` | グロック 17 / UZI 用 |
| `item_5` | 5.56mm弾 30発 | 個人 40 | 現代 | `(113, 142, 94, 76)` | M4A1 用 |
| `item_6` | 50BMG弾 10発 | 個人 60 | 現代 | `(210, 142, 94, 76)` | M107 用 |

- 全件を320×240内へ収めたため縦スクロールは使わない。
- locked状態では解禁欄を理由表示に使い、insufficient状態では価格文字だけを#C15A49へ変更する。

### `gacha` — ガチャ

合成ベース: `assets/jidaiui/textures/gui/page_gacha.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `gacha_track` | `assets/jidaiui/textures/gui/gacha_track.png` | `(16, 44, 288, 96)` | 9箇所（manifest参照） |
| `center_stop_marker` | `assets/jidaiui/textures/gui/gacha_stop_marker.png` | `(142, 54, 36, 78)` | — |
| `dynamic_price_panel` | `assets/jidaiui/textures/gui/gacha_price_panel.png` | `(64, 152, 192, 28)` | — |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `ガチャ` | `(20, 14, 90, 18)` | 16 | `#F5E8C8` | 1行 / left |
| `gacha_price` | `個人 {price}` | `(72, 159, 176, 14)` | 12 | `#C4934B` | 1行 / center |

価格は `鉄器5 / 中世10 / 近代20 / 現代50`、財布は `個人`。景品は左から右へ流れ、中心x=160で停止します。依頼文に操作ボタンの固定文言が無いため、文字を勝手に追加せず航路 `(16,44,288,96)` 自体をクリック領域にしています。

- 操作ボタンの固定文言は依頼文に無いため追加しない。中央航路をクリック領域として扱う。
- 景品アイコンは実行時に左から右へ移動し、x=160の停止マーカー中央で止める。

### `bank` — 銀行

合成ベース: `assets/jidaiui/textures/gui/page_bank.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `bank_1` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(16, 48, 94, 76)` | (22, 59, 32, 32) |
| `bank_2` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(113, 48, 94, 76)` | (119, 59, 32, 32) |
| `bank_3` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(210, 48, 94, 76)` | (216, 59, 32, 32) |
| `bank_4` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(16, 132, 94, 76)` | (22, 143, 32, 32) |
| `bank_5` | `assets/jidaiui/textures/gui/card_tall_normal.png` | `(113, 132, 94, 76)` | (119, 143, 32, 32) |
| `bank_6` | `assets/jidaiui/textures/gui/card_tall_readonly.png` | `(210, 132, 94, 76)` | (216, 143, 32, 32) |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `銀行` | `(20, 14, 90, 18)` | 16 | `#F5E8C8` | 1行 / left |
| `bank_1_name` | `10 預ける` | `(59, 55, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `bank_2_name` | `50 預ける` | `(156, 55, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `bank_3_name` | `全部 預ける` | `(253, 55, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `bank_4_name` | `貴金属を売る` | `(59, 139, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `bank_5_name` | `石油を預ける` | `(156, 139, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `bank_6_name` | `残高` | `(253, 139, 46, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `bank_6_balance` | `{balance}` | `(253, 163, 46, 10)` | 8 | `#4FA1B2` | 1行 / left |

カード正本:

| id | 名前 | 価格/値 | 解禁 | カード座標 | 補足 |
|---|---|---|---|---|---|
| `bank_1` | 10 預ける | — | — | `(16, 48, 94, 76)` | — |
| `bank_2` | 50 預ける | — | — | `(113, 48, 94, 76)` | — |
| `bank_3` | 全部 預ける | — | — | `(210, 48, 94, 76)` | — |
| `bank_4` | 貴金属を売る | — | — | `(16, 132, 94, 76)` | 鉄1 ラピス5 金10 ダイヤ15 ネザライト30 |
| `bank_5` | 石油を預ける | — | — | `(113, 132, 94, 76)` | — |
| `bank_6` | 残高 | — | — | `(210, 132, 94, 76)` | {balance} |

- 貴金属を売るの補足は tooltip_9slice.png 上へ「鉄1 ラピス5 金10 ダイヤ15 ネザライト30」をそのまま描画する。
- 残高はlockedではなくreadonly専用素材。

### `advance` — 時代を進める

合成ベース: `assets/jidaiui/textures/gui/page_advance.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `condition_1` | `assets/jidaiui/textures/gui/advance_status_unmet.png` | `(16, 48, 142, 76)` | (25, 62, 32, 32) |
| `condition_2` | `assets/jidaiui/textures/gui/advance_status_unmet.png` | `(162, 48, 142, 76)` | (171, 62, 32, 32) |
| `condition_3` | `assets/jidaiui/textures/gui/advance_status_unmet.png` | `(16, 132, 142, 76)` | (25, 146, 32, 32) |
| `apply` | `assets/jidaiui/textures/gui/card_large_normal.png` | `(162, 132, 142, 76)` | (171, 145, 32, 32) |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `時代を進める` | `(20, 16, 160, 14)` | 12 | `#F5E8C8` | 1行 / left |
| `condition_1_name` | `石油` | `(66, 57, 84, 14)` | 12 | `#F5E8C8` | 1行 / left |
| `condition_1_value` | `{value}` | `(66, 79, 84, 20)` | 8 | `#4FA1B2` | 2行 / left |
| `condition_2_name` | `貯金` | `(212, 57, 84, 14)` | 12 | `#F5E8C8` | 1行 / left |
| `condition_2_value` | `{value}` | `(212, 79, 84, 20)` | 8 | `#4FA1B2` | 2行 / left |
| `condition_3_name` | `建築` | `(66, 141, 84, 14)` | 12 | `#F5E8C8` | 1行 / left |
| `condition_3_value` | `{value}` | `(66, 163, 84, 20)` | 8 | `#4FA1B2` | 2行 / left |
| `apply_name` | `申請` | `(212, 140, 84, 28)` | 12 | `#F5E8C8` | 1行 / left |
| `apply_locked_reason` | `{reason}` | `(212, 189, 84, 10)` | 8 | `#AEB8AE` | 1行 / left |

カード正本:

| id | 名前 | 価格/値 | 解禁 | カード座標 | 補足 |
|---|---|---|---|---|---|
| `apply` | 申請 | — | — | `(162, 132, 142, 76)` | — |

- 石油・貯金・建築の3条件がすべてmetになるまで申請カードはlocked。

### `declare_war` — 宣戦する相手を選ぶ

合成ベース: `assets/jidaiui/textures/gui/page_declare_war.png`。スクロール: **なし**。

部品:

| id | 素材 | 座標 `(x,y,w,h)` | アイコン領域 |
|---|---|---|---|
| `screen_chrome` | `assets/jidaiui/textures/gui/screen_chrome.png` | `(0, 0, 320, 240)` | — |
| `danger_route` | `assets/jidaiui/textures/gui/danger_route.png` | `(16, 38, 288, 10)` | — |
| `faction_1` | `assets/jidaiui/textures/gui/card_danger_normal.png` | `(16, 52, 142, 76)` | (25, 61, 32, 32) |
| `faction_2` | `assets/jidaiui/textures/gui/card_danger_normal.png` | `(162, 52, 142, 76)` | (171, 61, 32, 32) |
| `faction_3` | `assets/jidaiui/textures/gui/card_danger_normal.png` | `(16, 134, 142, 76)` | (25, 143, 32, 32) |
| `faction_4` | `assets/jidaiui/textures/gui/card_danger_normal.png` | `(162, 134, 142, 76)` | (171, 143, 32, 32) |
| `irreversible_warning` | `assets/jidaiui/textures/gui/war_warning_panel.png` | `(16, 212, 288, 18)` | — |

実行時文字:

| id | 表示文字 | 領域 `(x,y,w,h)` | px | 色 | 行/揃え |
|---|---|---|---:|---|---|
| `title` | `宣戦する相手を選ぶ` | `(20, 16, 172, 14)` | 12 | `#F5E8C8` | 1行 / left |
| `faction_1_name` | `{faction_name_1}` | `(24, 100, 126, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `faction_2_name` | `{faction_name_2}` | `(170, 100, 126, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `faction_3_name` | `{faction_name_3}` | `(24, 182, 126, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `faction_4_name` | `{faction_name_4}` | `(170, 182, 126, 20)` | 8 | `#F5E8C8` | 2行 / left |
| `irreversible_warning_text` | `取り返しがつかない操作` | `(22, 216, 276, 10)` | 8 | `#C15A49` | 1行 / center |

カード正本:

| id | 名前 | 価格/値 | 解禁 | カード座標 | 補足 |
|---|---|---|---|---|---|
| `faction_1` | {faction_name_1} | — | — | `(16, 52, 142, 76)` | — |
| `faction_2` | {faction_name_2} | — | — | `(162, 52, 142, 76)` | — |
| `faction_3` | {faction_name_3} | — | — | `(16, 134, 142, 76)` | — |
| `faction_4` | {faction_name_4} | — | — | `(162, 134, 142, 76)` | — |

- 自勢力を除いた4勢力だけを行優先で並べる。赤い一方向航路と警告帯は常時表示する。

## PNG対応表

| ファイル | 寸法 | 色数 | alpha | 用途 |
|---|---:|---:|---|---|
| `assets/jidaiui/textures/gui/advance_status_met.png` | 142×76 | 4 | 255 | 時代進行条件の達成表示 |
| `assets/jidaiui/textures/gui/advance_status_unmet.png` | 142×76 | 4 | 255 | 時代進行条件の未達表示 |
| `assets/jidaiui/textures/gui/card_compact_hover.png` | 94×52 | 5 | 255 | compactカードのhover状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_compact_insufficient.png` | 94×52 | 5 | 255 | compactカードのinsufficient状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_compact_locked.png` | 94×52 | 3 | 255 | compactカードのlocked状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_compact_normal.png` | 94×52 | 4 | 255 | compactカードのnormal状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_compact_pressed.png` | 94×52 | 3 | 255 | compactカードのpressed状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_danger_hover.png` | 142×76 | 6 | 255 | dangerカードのhover状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_danger_insufficient.png` | 142×76 | 4 | 255 | dangerカードのinsufficient状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_danger_locked.png` | 142×76 | 4 | 255 | dangerカードのlocked状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_danger_normal.png` | 142×76 | 5 | 255 | dangerカードのnormal状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_danger_pressed.png` | 142×76 | 4 | 255 | dangerカードのpressed状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_large_hover.png` | 142×76 | 5 | 255 | largeカードのhover状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_large_insufficient.png` | 142×76 | 5 | 255 | largeカードのinsufficient状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_large_locked.png` | 142×76 | 3 | 255 | largeカードのlocked状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_large_normal.png` | 142×76 | 4 | 255 | largeカードのnormal状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_large_pressed.png` | 142×76 | 3 | 255 | largeカードのpressed状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_micro_hover.png` | 69×52 | 5 | 255 | microカードのhover状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_micro_insufficient.png` | 69×52 | 5 | 255 | microカードのinsufficient状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_micro_locked.png` | 69×52 | 3 | 255 | microカードのlocked状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_micro_normal.png` | 69×52 | 4 | 255 | microカードのnormal状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_micro_pressed.png` | 69×52 | 3 | 255 | microカードのpressed状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_tall_hover.png` | 94×76 | 5 | 255 | tallカードのhover状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_tall_insufficient.png` | 94×76 | 5 | 255 | tallカードのinsufficient状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_tall_locked.png` | 94×76 | 3 | 255 | tallカードのlocked状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_tall_normal.png` | 94×76 | 4 | 255 | tallカードのnormal状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_tall_pressed.png` | 94×76 | 3 | 255 | tallカードのpressed状態。文字とゲームアイコンは含まない。 |
| `assets/jidaiui/textures/gui/card_tall_readonly.png` | 94×76 | 4 | 255 | 銀行の残高専用。クリック状態を持たない情報表示カード。 |
| `assets/jidaiui/textures/gui/danger_route.png` | 288×10 | 3 | 255 | 宣戦画面の一方向危険航路。矢印はすべて同方向・同間隔。 |
| `assets/jidaiui/textures/gui/gacha_price_panel.png` | 192×28 | 3 | 255 | 時代別の動的ガチャ価格表示枠。 |
| `assets/jidaiui/textures/gui/gacha_stop_marker.png` | 36×78 | 3 | 0,255 | 中央停止位置を示す真鍮針と赤い直角ブラケット。 |
| `assets/jidaiui/textures/gui/gacha_track.png` | 288×96 | 8 | 255 | 景品が左から右へ流れる288×96の航路。9個の16pxアイコン井戸を持つ。 |
| `assets/jidaiui/textures/gui/outer_frame_9slice.png` | 24×24 | 5 | 255 | 320×240基準の外枠。8px角・8px辺の9分割アトラス。 |
| `assets/jidaiui/textures/gui/page_advance.png` | 320×240 | 7 | 255 | 時代を進めるの320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/page_bank.png` | 320×240 | 8 | 255 | 銀行の320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/page_declare_war.png` | 320×240 | 8 | 255 | 宣戦する相手を選ぶの320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/page_gacha.png` | 320×240 | 10 | 255 | ガチャの320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/page_gunshop_ammo.png` | 320×240 | 9 | 255 | 銃器専門店の320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/page_gunshop_guns.png` | 320×240 | 9 | 255 | 銃器専門店の320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/page_shop_armor.png` | 320×240 | 9 | 255 | 販売所の320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/page_shop_life.png` | 320×240 | 9 | 255 | 販売所の320×240文字なし合成ベース。状態素材・ゲームアイコン・文字をこの上へ重ねる。 |
| `assets/jidaiui/textures/gui/panel_9slice.png` | 24×24 | 4 | 255 | 情報パネル用の9分割アトラス。 |
| `assets/jidaiui/textures/gui/screen_chrome.png` | 320×240 | 6 | 255 | 全8ページ共通の320×240航海測量背景。ヘッダー、16px格子、対称の真鍮目盛りを含む。 |
| `assets/jidaiui/textures/gui/tab_selected.png` | 62×18 | 4 | 255 | 選択中タブ |
| `assets/jidaiui/textures/gui/tab_unselected.png` | 62×18 | 3 | 255 | 非選択タブ |
| `assets/jidaiui/textures/gui/tooltip_9slice.png` | 24×24 | 4 | 255 | 銃・弾・貴金属の補足を実行時文字で出す吹き出し枠。 |
| `assets/jidaiui/textures/gui/war_warning_panel.png` | 288×18 | 3 | 255 | 取り返しのつかない操作であることを示す警告帯。 |

## 検査結果

- PASS: generator has no font import and no raster text drawing call
- PASS: eight effective pages present
- PASS: all component/text/icon rectangles inside 320x240
- PASS: no card overlaps
- PASS: all pages fit without scrolling
- PASS: font sizes restricted to 8/12/16
- PASS: card counts match 4/12/9/6/6/4 plus advance 3+1
- PASS: canonical product names preserved without abbreviation
- PASS: prices, wallets, unlock eras and tooltip strings preserved
- PASS: diamond chestplate early-unlock exception preserved
- PASS: gacha era prices preserved
- PASS: bank and irreversible-war wording preserved
- PASS: outer_frame_9slice.png: 24x24 atlas, mirrored corners and seamless edge/center repetition verified
- PASS: panel_9slice.png: 24x24 atlas, mirrored corners and seamless edge/center repetition verified
- PASS: tooltip_9slice.png: 24x24 atlas, mirrored corners and seamless edge/center repetition verified
- PASS: every PNG has declared dimensions
- PASS: every PNG uses only alpha 0 or 255
- PASS: every PNG has 256 or fewer RGBA colors
- PASS: all fully transparent pixels have RGB 0,0,0
- PASS: all declared text/icon regions are single-color and pattern-free
- PASS: no unregistered PNG and no unnecessary scrollbar asset
- PNG総数: 47
- 最大色数: 10（`page_gacha.png`）
- 全PNG: RGBA、alphaは0/255のみ、256色以下、宣言済み文字・アイコン領域は単色
- ZIP内Markdownは本書1本だけです。
