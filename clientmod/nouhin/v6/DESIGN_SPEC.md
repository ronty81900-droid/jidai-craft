# 時代クラフト 遺物アイテム v6 — 3案 DESIGN_SPEC

必須11種について、A/B/Cの**完全な3組（合計33枚）**を用意しました。いずれも同じ正式ファイル名を保持しているため、採用する1組の `assets/` をそのままMOD側へコピーできます。Aは即読性、Bは機構・奥行き、Cは角度・使用状態の差を優先し、単純な左右反転は使っていません。

## 納品構造と選び方

- 案A: `proposal_a/assets/jidaiui/textures/item/`
- 案B: `proposal_b/assets/jidaiui/textures/item/`
- 案C: `proposal_c/assets/jidaiui/textures/item/`
- 4倍確認画像: `preview/proposal_a/`、`preview/proposal_b/`、`preview/proposal_c/`
- 採用時は1案の `assets/` 以下をコピーします。3案を同時にコピーして上書きしないでください。

## 共通作図規約

- すべて最初から64×64の整数ピクセル形状として構成し、写真・生成画像・高解像度原画を縮小していません。
- 背景は `#00000000`、不透明度は0か255だけ。ぼかし、発光、半透明、グラデーション、アンチエイリアスはありません。
- 完成シルエットを外から2px剥がした層は `#102329`、続く2pxは `#C4934B`。完成PNGから全画素を再検査しています。
- 各画像は上下左右2〜4pxの透明余白、実描画範囲56〜60pxです。大半は3pxへ揃えています。
- 共通光源は左上。最高輝度色 `#F5DE9B` は各画像1つの連結クラスタだけです。右下側には各素材の暗色面を置きます。
- 経年傷・欠け・煤は各画像1つの連結箇所だけ。リベット、縫い目、木目は素材構造として別扱いですが、必要最小限に抑えています。
- 暗い共通輪郭色は素材面の内部へ使っていません。内部は色面境界だけです。
- 読める文字、数字、疑似文字、ロゴ、メーカー固有UIは描いていません。
- `preview/` は完成64×64をnearest-neighborで厳密に4倍した256×256です。

## 共有パレット

真鍮・鉄・革・羊皮紙は全33枚で同一値です。各画像が実際に使った色は後段の個別表へpixel数付きで再掲します。

| role | RGBA |
|---|---|
| `transparent` | `#00000000` |
| `outline` | `#102329FF` |
| `brass_deep` | `#6F4E28FF` |
| `brass_dark` | `#9B6D32FF` |
| `brass` | `#C4934BFF` |
| `brass_light` | `#D6B06BFF` |
| `highlight` | `#F5DE9BFF` |
| `iron_deep` | `#303B3FFF` |
| `iron` | `#586368FF` |
| `iron_light` | `#7D8889FF` |
| `steel_light` | `#A8B0ADFF` |
| `steel_bright` | `#C9D0CCFF` |
| `leather_deep` | `#4A2922FF` |
| `leather` | `#784337FF` |
| `leather_light` | `#A5684EFF` |
| `paper_deep` | `#6E5A3DFF` |
| `paper_dark` | `#A98D61FF` |
| `paper` | `#D1B684FF` |
| `paper_light` | `#E4CC9AFF` |
| `wood_deep` | `#4B3325FF` |
| `wood` | `#765039FF` |
| `wood_light` | `#A47A50FF` |
| `white_deep` | `#817C71FF` |
| `white` | `#B9B09BFF` |
| `white_light` | `#D8D0B8FF` |
| `red_deep` | `#5B1F22FF` |
| `red` | `#8E2D2DFF` |
| `red_light` | `#B34A3DFF` |
| `enamel_blue` | `#213A56FF` |
| `enamel_green` | `#3C6A55FF` |
| `enamel_brown` | `#7F3C32FF` |
| `jewel_blue` | `#376B83FF` |
| `jewel_red` | `#8F3A3FFF` |
| `hot_deep` | `#721F18FF` |
| `hot` | `#B83B24FF` |
| `hot_light` | `#E86932FF` |
| `soot` | `#0A1012FF` |
| `black` | `#11171AFF` |
| `black_mid` | `#222B2FFF` |
| `black_light` | `#374247FF` |
| `glass_deep` | `#426064FF` |
| `glass` | `#789295FF` |
| `glass_light` | `#AFC4BEFF` |
| `liquid_deep` | `#667548FF` |
| `liquid` | `#9DAE68FF` |
| `liquid_light` | `#C5D58AFF` |
| `lens_deep` | `#1F4A3AFF` |
| `lens` | `#39725AFF` |
| `lens_light` | `#66A277FF` |
| `screen` | `#152B38FF` |
| `screen_light` | `#244553FF` |
| `scuff` | `#6A4B3DFF` |

## 史料の扱い

- 実物写真・図版は形状と構造の観察だけに使い、画像は同梱・トレース・縮小していません。
- ロンバルディアの鉄王冠は伝承名に合わせ暗い内輪を描きますが、モンツァ大聖堂博物館が紹介する科学調査では内輪は銀とされています。
- エクスカリバーと特定の現存『テンプル騎士団の盾』は実物資料がないため、同時代の直剣・カイト盾の構造資料と発注意匠を組み合わせています。
- 現行penicillin G製品は乾燥粉末として供給されるものがあります。本作の淡黄緑液は医療情報ではなく、発注指定に基づくゲーム内識別色です。
- 参照ページの確認日: 2026-08-22。

## 正本対応表

| # | 時代 | 正式名 | 各案の正式ファイル名 |
|---:|---|---|---|
| 1 | 鉄器 | **ヒッタイトの戦車** | `sensha.png` |
| 2 | 鉄器 | **ロンバルディアの鉄王冠** | `oukan.png` |
| 3 | 中世 | **聖剣エクスカリバー** | `excalibur.png` |
| 4 | 中世 | **テンプル騎士団の盾** | `tate.png` |
| 5 | 中世 | **武器商人の手形** | `tegata.png` |
| 6 | 近代 | **ベッセマー転炉** | `tenro.png` |
| 7 | 近代 | **ロスチャイルドの金庫** | `kinko.png` |
| 8 | 近代 | **死の商人の手引書** | `tebiki.png` |
| 9 | 現代 | **ペニシリン** | `penicillin.png` |
| 10 | 現代 | **暗視ゴーグル** | `goggle.png` |
| 11 | 現代 | **スマートフォン** | `sumaho.png` |

## 1. ヒッタイトの戦車 — `sensha.png`

代表素材・識別色: **木・革・青銅**

参照資料:

- https://www.metmuseum.org/art/collection/search/324008
- https://belleten.gov.tr/eng/abstarct/3592/eng

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/sensha.png`
- 構図: 大車輪の側面図。革張りの車台、木の車輪と轅、青銅の轂を最短距離で読ませる。
- 唯一のハイライト: 近側の青銅製轂の左上
- 唯一の経年箇所: 革張り車台右下の一続きの擦れ
- 使用RGBA色数: **12**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 2134 |
| `outline` | `#102329FF` | 880 |
| `leather_deep` | `#4A2922FF` | 2 |
| `wood_deep` | `#4B3325FF` | 11 |
| `wood` | `#765039FF` | 191 |
| `leather` | `#784337FF` | 111 |
| `brass_dark` | `#9B6D32FF` | 5 |
| `wood_light` | `#A47A50FF` | 19 |
| `leather_light` | `#A5684EFF` | 94 |
| `brass` | `#C4934BFF` | 626 |
| `paper_light` | `#E4CC9AFF` | 22 |
| `highlight` | `#F5DE9BFF` | 1 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/sensha.png`
- 構図: 後方3/4。大小の二輪と奥側の暗い面で、二輪戦車の奥行きを見せる。
- 唯一のハイライト: 近側の青銅製軸受の左上
- 唯一の経年箇所: 革箱の後上角に一続きの裂け
- 使用RGBA色数: **12**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1901 |
| `outline` | `#102329FF` | 892 |
| `leather_deep` | `#4A2922FF` | 81 |
| `wood_deep` | `#4B3325FF` | 16 |
| `wood` | `#765039FF` | 195 |
| `leather` | `#784337FF` | 219 |
| `brass_dark` | `#9B6D32FF` | 8 |
| `wood_light` | `#A47A50FF` | 13 |
| `leather_light` | `#A5684EFF` | 97 |
| `brass` | `#C4934BFF` | 647 |
| `paper_light` | `#E4CC9AFF` | 26 |
| `highlight` | `#F5DE9BFF` | 1 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/sensha.png`
- 構図: 後方3/4の二輪構図。左右に離した二輪と上方へ伸びる二本の轅でA/Bと輪郭を変える。
- 唯一のハイライト: 左側の青銅製轂の左上
- 唯一の経年箇所: 革張り車台右下の一続きの擦り跡
- 使用RGBA色数: **12**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1605 |
| `outline` | `#102329FF` | 1039 |
| `leather_deep` | `#4A2922FF` | 84 |
| `wood_deep` | `#4B3325FF` | 8 |
| `wood` | `#765039FF` | 207 |
| `leather` | `#784337FF` | 178 |
| `brass_dark` | `#9B6D32FF` | 14 |
| `wood_light` | `#A47A50FF` | 21 |
| `leather_light` | `#A5684EFF` | 148 |
| `brass` | `#C4934BFF` | 767 |
| `paper_light` | `#E4CC9AFF` | 24 |
| `highlight` | `#F5DE9BFF` | 1 |

## 2. ロンバルディアの鉄王冠 — `oukan.png`

代表素材・識別色: **金・多色エナメル・暗い内輪**

参照資料:

- https://www.museoduomomonza.it/en/iron-crown/
- https://www.museoduomomonza.it/en/iron-crown/the-iron-crown-as-a-work-of-art/

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/oukan.png`
- 構図: 低い正面楕円。六枚の金板、宝石、エナメル、内輪を王冠らしい帯形へ整理する。
- 唯一のハイライト: 左上の宝石
- 唯一の経年箇所: 右前板の下縁に一続きの擦れ
- 使用RGBA色数: **13**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1306 |
| `outline` | `#102329FF` | 548 |
| `enamel_blue` | `#213A56FF` | 139 |
| `iron_deep` | `#303B3FFF` | 364 |
| `enamel_green` | `#3C6A55FF` | 67 |
| `brass_deep` | `#6F4E28FF` | 15 |
| `enamel_brown` | `#7F3C32FF` | 175 |
| `jewel_red` | `#8F3A3FFF` | 39 |
| `brass_dark` | `#9B6D32FF` | 637 |
| `brass` | `#C4934BFF` | 548 |
| `brass_light` | `#D6B06BFF` | 181 |
| `white_light` | `#D8D0B8FF` | 71 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/oukan.png`
- 構図: 上からの3/4傾斜。暗い内輪を大きく見せ、金外装との素材差を強調する。
- 唯一のハイライト: 左前の宝石
- 唯一の経年箇所: 奥右エナメル角の一続きの欠け
- 使用RGBA色数: **13**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1746 |
| `outline` | `#102329FF` | 612 |
| `enamel_blue` | `#213A56FF` | 9 |
| `iron_deep` | `#303B3FFF` | 100 |
| `enamel_green` | `#3C6A55FF` | 108 |
| `brass_deep` | `#6F4E28FF` | 20 |
| `enamel_brown` | `#7F3C32FF` | 39 |
| `jewel_red` | `#8F3A3FFF` | 34 |
| `brass_dark` | `#9B6D32FF` | 616 |
| `brass` | `#C4934BFF` | 612 |
| `brass_light` | `#D6B06BFF` | 120 |
| `white_light` | `#D8D0B8FF` | 71 |
| `highlight` | `#F5DE9BFF` | 9 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/oukan.png`
- 構図: 真上寄りの六分割。板の反復と継ぎ目を色面だけで説明する。
- 唯一のハイライト: 前中央ロゼットの左上
- 唯一の経年箇所: 前中央金縁の一続きの凹み
- 使用RGBA色数: **13**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1332 |
| `outline` | `#102329FF` | 568 |
| `enamel_blue` | `#213A56FF` | 124 |
| `iron_deep` | `#303B3FFF` | 126 |
| `enamel_green` | `#3C6A55FF` | 111 |
| `brass_deep` | `#6F4E28FF` | 22 |
| `enamel_brown` | `#7F3C32FF` | 110 |
| `jewel_red` | `#8F3A3FFF` | 80 |
| `brass_dark` | `#9B6D32FF` | 836 |
| `brass` | `#C4934BFF` | 568 |
| `brass_light` | `#D6B06BFF` | 81 |
| `white_light` | `#D8D0B8FF` | 129 |
| `highlight` | `#F5DE9BFF` | 9 |

## 3. 聖剣エクスカリバー — `excalibur.png`

代表素材・識別色: **研磨鋼・濃紺革・青石**

参照資料:

- https://www.britishmuseum.org/collection/object/H_ML-4035
- https://www.metmuseum.org/art/collection/search/27458

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/excalibur.png`
- 構図: 左下の柄から右上の切先へ伸びる定番構図。両刃、フラー、十字鍔、円盤柄頭を見せる。
- 唯一のハイライト: 刃上部の左上側切刃
- 唯一の経年箇所: 刃中腹右縁の一続きの刃こぼれ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 2554 |
| `outline` | `#102329FF` | 448 |
| `iron_deep` | `#303B3FFF` | 99 |
| `jewel_blue` | `#376B83FF` | 66 |
| `leather_deep` | `#4A2922FF` | 73 |
| `iron` | `#586368FF` | 193 |
| `brass_dark` | `#9B6D32FF` | 131 |
| `leather_light` | `#A5684EFF` | 19 |
| `steel_light` | `#A8B0ADFF` | 89 |
| `brass` | `#C4934BFF` | 416 |
| `highlight` | `#F5DE9BFF` | 8 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/excalibur.png`
- 構図: ほぼ垂直で鍔を主役にする構図。片側の鍔端を下げ、機械的な左右対称を避ける。
- 唯一のハイライト: 刃上部の左上側切刃
- 唯一の経年箇所: 刃中腹の一続きの研ぎ痕
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 2299 |
| `outline` | `#102329FF` | 468 |
| `iron_deep` | `#303B3FFF` | 93 |
| `jewel_blue` | `#376B83FF` | 66 |
| `leather_deep` | `#4A2922FF` | 66 |
| `iron` | `#586368FF` | 245 |
| `brass_dark` | `#9B6D32FF` | 282 |
| `leather_light` | `#A5684EFF` | 10 |
| `steel_light` | `#A8B0ADFF` | 124 |
| `brass` | `#C4934BFF` | 436 |
| `highlight` | `#F5DE9BFF` | 7 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/excalibur.png`
- 構図: 水平に近い抜き身。太い刃と緩く下反りした鍔を大きく読ませる。
- 唯一のハイライト: 切先寄りの左上側切刃
- 唯一の経年箇所: 刃元片側の一続きの黒ずみ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 2324 |
| `outline` | `#102329FF` | 476 |
| `iron_deep` | `#303B3FFF` | 113 |
| `jewel_blue` | `#376B83FF` | 99 |
| `leather_deep` | `#4A2922FF` | 105 |
| `iron` | `#586368FF` | 241 |
| `brass_dark` | `#9B6D32FF` | 156 |
| `leather_light` | `#A5684EFF` | 26 |
| `steel_light` | `#A8B0ADFF` | 100 |
| `brass` | `#C4934BFF` | 444 |
| `highlight` | `#F5DE9BFF` | 12 |

## 4. テンプル騎士団の盾 — `tate.png`

代表素材・識別色: **白地・赤十字・木裏・鉄鋲**

参照資料:

- https://royalarmouries.org/objects-and-stories/stories/the-hundred-years-war-1337-1453
- https://www.britishmuseum.org/collection/object/H_1831-1101-110

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/tate.png`
- 構図: 正面カイト盾。白地と赤十字を主面にし、右縁だけ木口を見せる。
- 唯一のハイライト: 左上の鉄鋲
- 唯一の経年箇所: 赤十字右腕外側の半月状へこみ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1490 |
| `outline` | `#102329FF` | 448 |
| `red_deep` | `#5B1F22FF` | 34 |
| `wood` | `#765039FF` | 208 |
| `iron_light` | `#7D8889FF` | 82 |
| `white_deep` | `#817C71FF` | 287 |
| `red` | `#8E2D2DFF` | 551 |
| `white` | `#B9B09BFF` | 345 |
| `brass` | `#C4934BFF` | 416 |
| `white_light` | `#D8D0B8FF` | 229 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/tate.png`
- 構図: 前面3/4。右側へ木裏と革の把手を見せ、木の裏打ちを最も明瞭にする。
- 唯一のハイライト: 前面左上の鉄鋲
- 唯一の経年箇所: 下左白面の打痕一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1392 |
| `outline` | `#102329FF` | 452 |
| `red_deep` | `#5B1F22FF` | 20 |
| `wood` | `#765039FF` | 499 |
| `iron_light` | `#7D8889FF` | 55 |
| `white_deep` | `#817C71FF` | 254 |
| `red` | `#8E2D2DFF` | 540 |
| `white` | `#B9B09BFF` | 290 |
| `brass` | `#C4934BFF` | 420 |
| `white_light` | `#D8D0B8FF` | 165 |
| `highlight` | `#F5DE9BFF` | 9 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/tate.png`
- 構図: 反時計回りの前面。左木口と右背後から覗く革帯で構造を示す。
- 唯一のハイライト: 上左の鉄鋲
- 唯一の経年箇所: 右下斜面のへこみ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1527 |
| `outline` | `#102329FF` | 460 |
| `red_deep` | `#5B1F22FF` | 31 |
| `wood` | `#765039FF` | 234 |
| `iron_light` | `#7D8889FF` | 54 |
| `white_deep` | `#817C71FF` | 406 |
| `red` | `#8E2D2DFF` | 478 |
| `white` | `#B9B09BFF` | 353 |
| `brass` | `#C4934BFF` | 428 |
| `white_light` | `#D8D0B8FF` | 121 |
| `highlight` | `#F5DE9BFF` | 4 |

## 5. 武器商人の手形 — `tegata.png`

代表素材・識別色: **羊皮紙・赤蝋・赤紐**

参照資料:

- https://searcharchives.bl.uk/catalog/032-002142948
- https://www.nationalarchives.gov.uk/visit/researching-here/handling-documents/handling-seals/

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/tegata.png`
- 構図: 折り証文の右下へ赤蝋封を付ける。読める文字は置かず、不揃いな短線だけを使う。
- 唯一のハイライト: 蝋封の左上
- 唯一の経年箇所: 右上角の折れ欠け一つ
- 使用RGBA色数: **10**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1561 |
| `outline` | `#102329FF` | 460 |
| `red_deep` | `#5B1F22FF` | 60 |
| `paper_deep` | `#6E5A3DFF` | 109 |
| `red` | `#8E2D2DFF` | 188 |
| `paper_dark` | `#A98D61FF` | 555 |
| `brass` | `#C4934BFF` | 428 |
| `paper` | `#D1B684FF` | 430 |
| `paper_light` | `#E4CC9AFF` | 299 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/tegata.png`
- 構図: 三つ折りを半開きにした証文。赤紐が紙を斜めに回り、蝋封を下中央へ置く。
- 唯一のハイライト: 左上の紙折り山
- 唯一の経年箇所: 蝋封右下の欠け一つ
- 使用RGBA色数: **10**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1486 |
| `outline` | `#102329FF` | 444 |
| `red_deep` | `#5B1F22FF` | 126 |
| `paper_deep` | `#6E5A3DFF` | 189 |
| `red` | `#8E2D2DFF` | 171 |
| `paper_dark` | `#A98D61FF` | 598 |
| `brass` | `#C4934BFF` | 412 |
| `paper` | `#D1B684FF` | 389 |
| `paper_light` | `#E4CC9AFF` | 272 |
| `highlight` | `#F5DE9BFF` | 9 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/tegata.png`
- 構図: 下端plicaと吊り印章。二本の紐で下がる封印により中世文書らしさを出す。
- 唯一のハイライト: 吊り印章の左上
- 唯一の経年箇所: 中央右の折り擦れ一つ
- 使用RGBA色数: **10**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1654 |
| `outline` | `#102329FF` | 456 |
| `red_deep` | `#5B1F22FF` | 82 |
| `paper_deep` | `#6E5A3DFF` | 113 |
| `red` | `#8E2D2DFF` | 161 |
| `paper_dark` | `#A98D61FF` | 549 |
| `brass` | `#C4934BFF` | 424 |
| `paper` | `#D1B684FF` | 394 |
| `paper_light` | `#E4CC9AFF` | 254 |
| `highlight` | `#F5DE9BFF` | 9 |

## 6. ベッセマー転炉 — `tenro.png`

代表素材・識別色: **黒鉄・リベット・赤熱口**

参照資料:

- https://collection.sciencemuseumgroup.org.uk/objects/co19762/model-of-modern-bessemer-converter
- https://www.chiba-muse.or.jp/SCIENCE/topics/page-1614921541499/
- https://www.sheffieldmuseums.org.uk/whats-on/bessemer-converter/

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/tenro.png`
- 構図: 右へ傾けた注湯姿勢。洋梨胴、斜めの開口、トラニオン、支持台を一体で見せる。
- 唯一のハイライト: 赤熱口内の左上
- 唯一の経年箇所: 口下面の煤三日月一つ
- 使用RGBA色数: **13**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1152 |
| `soot` | `#0A1012FF` | 25 |
| `outline` | `#102329FF` | 468 |
| `black` | `#11171AFF` | 723 |
| `iron_deep` | `#303B3FFF` | 321 |
| `iron` | `#586368FF` | 249 |
| `hot_deep` | `#721F18FF` | 82 |
| `iron_light` | `#7D8889FF` | 242 |
| `brass_dark` | `#9B6D32FF` | 281 |
| `steel_light` | `#A8B0ADFF` | 34 |
| `brass` | `#C4934BFF` | 436 |
| `hot_light` | `#E86932FF` | 77 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/tenro.png`
- 構図: 左へ傾けた機構重視構図。右側だけ大きい回転機構を出し、輪郭差を作る。
- 唯一のハイライト: 赤熱口内の左上
- 唯一の経年箇所: 左口縁下端の煤一つ
- 使用RGBA色数: **13**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1186 |
| `soot` | `#0A1012FF` | 25 |
| `outline` | `#102329FF` | 476 |
| `black` | `#11171AFF` | 690 |
| `iron_deep` | `#303B3FFF` | 268 |
| `iron` | `#586368FF` | 219 |
| `hot_deep` | `#721F18FF` | 97 |
| `iron_light` | `#7D8889FF` | 289 |
| `brass_dark` | `#9B6D32FF` | 295 |
| `steel_light` | `#A8B0ADFF` | 23 |
| `brass` | `#C4934BFF` | 444 |
| `hot_light` | `#E86932FF` | 78 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/tenro.png`
- 構図: 口が読める正面3/4。楕円口の赤熱面を色面だけで描き、発光は使わない。
- 唯一のハイライト: 赤熱口内の左上
- 唯一の経年箇所: 右肩の煤パッチ一つ
- 使用RGBA色数: **13**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1248 |
| `soot` | `#0A1012FF` | 31 |
| `outline` | `#102329FF` | 460 |
| `black` | `#11171AFF` | 706 |
| `iron_deep` | `#303B3FFF` | 274 |
| `iron` | `#586368FF` | 190 |
| `hot_deep` | `#721F18FF` | 131 |
| `iron_light` | `#7D8889FF` | 182 |
| `brass_dark` | `#9B6D32FF` | 285 |
| `steel_light` | `#A8B0ADFF` | 36 |
| `brass` | `#C4934BFF` | 428 |
| `hot_light` | `#E86932FF` | 119 |
| `highlight` | `#F5DE9BFF` | 6 |

## 7. ロスチャイルドの金庫 — `kinko.png`

代表素材・識別色: **黒鉄・真鍮ダイヤル・金線**

参照資料:

- https://www.rothschildandco.com/siteassets/publications/rothschildandco/wealth_management/2017/randcowm---why-wealth-preservation.pdf
- https://catalogue.etoncollege.com/object-fda-a-2007-2020
- https://museum.lhc.gov.pk/node/49

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/kinko.png`
- 構図: 実物寄せの正面3/4。金線の扉縁、真鍮ダイヤル、三叉ハンドルをまとめる。
- 唯一のハイライト: 真鍮ダイヤルの左上
- 唯一の経年箇所: 扉右下の斜め擦り傷一本
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1002 |
| `outline` | `#102329FF` | 444 |
| `black` | `#11171AFF` | 273 |
| `black_mid` | `#222B2FFF` | 331 |
| `iron_deep` | `#303B3FFF` | 809 |
| `black_light` | `#374247FF` | 294 |
| `scuff` | `#6A4B3DFF` | 20 |
| `brass_dark` | `#9B6D32FF` | 290 |
| `brass` | `#C4934BFF` | 530 |
| `brass_light` | `#D6B06BFF` | 97 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/kinko.png`
- 構図: 扉をわずかに開いた構図。空の暗い内部と扉厚で金庫の機構を説明する。
- 唯一のハイライト: 開いた扉のダイヤル左上
- 唯一の経年箇所: 扉左下角の擦れ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1050 |
| `outline` | `#102329FF` | 452 |
| `black` | `#11171AFF` | 1008 |
| `black_mid` | `#222B2FFF` | 373 |
| `iron_deep` | `#303B3FFF` | 299 |
| `black_light` | `#374247FF` | 140 |
| `scuff` | `#6A4B3DFF` | 20 |
| `brass_dark` | `#9B6D32FF` | 208 |
| `brass` | `#C4934BFF` | 499 |
| `brass_light` | `#D6B06BFF` | 38 |
| `highlight` | `#F5DE9BFF` | 9 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/kinko.png`
- 構図: 旧式リベット金庫。台脚、輪ハンドル、太い蝶番帯を正面中心に配置する。
- 唯一のハイライト: 輪ハンドル上のダイヤル左上
- 唯一の経年箇所: 右側面下の縦擦れ一つ
- 使用RGBA色数: **12**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 921 |
| `outline` | `#102329FF` | 456 |
| `black` | `#11171AFF` | 205 |
| `black_mid` | `#222B2FFF` | 536 |
| `iron_deep` | `#303B3FFF` | 683 |
| `black_light` | `#374247FF` | 172 |
| `scuff` | `#6A4B3DFF` | 21 |
| `brass_dark` | `#9B6D32FF` | 260 |
| `steel_light` | `#A8B0ADFF` | 35 |
| `brass` | `#C4934BFF` | 553 |
| `brass_light` | `#D6B06BFF` | 248 |
| `highlight` | `#F5DE9BFF` | 6 |

## 8. 死の商人の手引書 — `tebiki.png`

代表素材・識別色: **黒革・生成り小口・銃の金箔**

参照資料:

- https://collection.sciencemuseumgroup.org.uk/objects/co179115/leather-bound-notebook
- https://www.metmuseum.org/art/collection/search/24855

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/tebiki.png`
- 構図: 閉じた縦手帳。黒革表紙へ長銃身の拳銃シルエットを金箔で置く。
- 唯一のハイライト: 銃箔の左上
- 唯一の経年箇所: 右下角の革擦れ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1044 |
| `outline` | `#102329FF` | 448 |
| `black` | `#11171AFF` | 841 |
| `black_mid` | `#222B2FFF` | 119 |
| `black_light` | `#374247FF` | 397 |
| `leather_deep` | `#4A2922FF` | 385 |
| `scuff` | `#6A4B3DFF` | 27 |
| `leather_light` | `#A5684EFF` | 51 |
| `brass` | `#C4934BFF` | 651 |
| `paper_light` | `#E4CC9AFF` | 127 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/tebiki.png`
- 構図: 斜め配置の閉本。長軸と箔押しを平行にしてAと大きく輪郭を変える。
- 唯一のハイライト: 斜め銃身の左上
- 唯一の経年箇所: 上右角の革擦れ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1727 |
| `outline` | `#102329FF` | 448 |
| `black` | `#11171AFF` | 531 |
| `black_mid` | `#222B2FFF` | 93 |
| `black_light` | `#374247FF` | 294 |
| `leather_deep` | `#4A2922FF` | 225 |
| `scuff` | `#6A4B3DFF` | 21 |
| `leather_light` | `#A5684EFF` | 34 |
| `brass` | `#C4934BFF` | 583 |
| `paper_light` | `#E4CC9AFF` | 134 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/tebiki.png`
- 構図: 横長の現場用ポケットブック。ずれた前表紙、紙束、革留め具を見せる。
- 唯一のハイライト: 横向き銃箔の左上
- 唯一の経年箇所: 背中央の革擦れ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1218 |
| `outline` | `#102329FF` | 448 |
| `black` | `#11171AFF` | 734 |
| `black_mid` | `#222B2FFF` | 174 |
| `black_light` | `#374247FF` | 382 |
| `leather_deep` | `#4A2922FF` | 249 |
| `scuff` | `#6A4B3DFF` | 35 |
| `leather_light` | `#A5684EFF` | 27 |
| `brass` | `#C4934BFF` | 634 |
| `paper_light` | `#E4CC9AFF` | 186 |
| `highlight` | `#F5DE9BFF` | 9 |

## 9. ペニシリン — `penicillin.png`

代表素材・識別色: **ガラス・淡黄緑液・注射器**

参照資料:

- https://www.si.edu/object/penicillin%3Anmah_1912343
- https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=969a421e-37d8-499e-9e89-67e210bd5d9d

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/penicillin.png`
- 構図: 直立バイアルの背面を注射器が交差する、最も即読性の高い構図。
- 唯一のハイライト: バイアル左肩の反射
- 唯一の経年箇所: アルミ縁右下の擦れ一つ
- 使用RGBA色数: **12**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1645 |
| `outline` | `#102329FF` | 484 |
| `iron_deep` | `#303B3FFF` | 20 |
| `black_light` | `#374247FF` | 81 |
| `glass_deep` | `#426064FF` | 181 |
| `glass` | `#789295FF` | 385 |
| `iron_light` | `#7D8889FF` | 159 |
| `liquid` | `#9DAE68FF` | 280 |
| `steel_light` | `#A8B0ADFF` | 224 |
| `glass_light` | `#AFC4BEFF` | 177 |
| `brass` | `#C4934BFF` | 452 |
| `highlight` | `#F5DE9BFF` | 8 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/penicillin.png`
- 構図: 太いバイアルとほぼ縦の注射器を並置し、栓とプランジャーを読みやすくする。
- 唯一のハイライト: 注射筒左上の反射
- 唯一の経年箇所: 栓の縁の擦れ一つ
- 使用RGBA色数: **12**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1239 |
| `outline` | `#102329FF` | 544 |
| `iron_deep` | `#303B3FFF` | 15 |
| `black_light` | `#374247FF` | 126 |
| `glass_deep` | `#426064FF` | 158 |
| `glass` | `#789295FF` | 589 |
| `iron_light` | `#7D8889FF` | 151 |
| `liquid` | `#9DAE68FF` | 299 |
| `steel_light` | `#A8B0ADFF` | 243 |
| `glass_light` | `#AFC4BEFF` | 171 |
| `brass` | `#C4934BFF` | 552 |
| `highlight` | `#F5DE9BFF` | 9 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/penicillin.png`
- 構図: 逆さバイアルへ針を入れた吸引構図。手は描かず、動作だけを示す。
- 唯一のハイライト: 逆さ瓶左上の反射
- 唯一の経年箇所: アルミ縁の傷一つ
- 使用RGBA色数: **12**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 2247 |
| `outline` | `#102329FF` | 452 |
| `iron_deep` | `#303B3FFF` | 15 |
| `black_light` | `#374247FF` | 132 |
| `glass_deep` | `#426064FF` | 34 |
| `glass` | `#789295FF` | 182 |
| `iron_light` | `#7D8889FF` | 163 |
| `liquid` | `#9DAE68FF` | 197 |
| `steel_light` | `#A8B0ADFF` | 194 |
| `glass_light` | `#AFC4BEFF` | 54 |
| `brass` | `#C4934BFF` | 420 |
| `highlight` | `#F5DE9BFF` | 6 |

## 10. 暗視ゴーグル — `goggle.png`

代表素材・識別色: **黒筐体・彩度を抑えた緑レンズ**

参照資料:

- https://www.l3harris.com/all-capabilities/binocular-night-vision-device-bnvd-1531
- https://www.l3harris.com/sites/default/files/2025-01/l3harris-binocular-night-vision-device-bnvd-1531-ps-cs-ivs.pdf

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/goggle.png`
- 構図: 二眼正面。レンズを上下にずらし、後方アーチと右上バックルを付ける。
- 唯一のハイライト: 左レンズだけの反射一点
- 唯一の経年箇所: 右筒の擦れ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1184 |
| `outline` | `#102329FF` | 520 |
| `black` | `#11171AFF` | 158 |
| `lens_deep` | `#1F4A3AFF` | 308 |
| `black_mid` | `#222B2FFF` | 682 |
| `black_light` | `#374247FF` | 269 |
| `lens` | `#39725AFF` | 323 |
| `scuff` | `#6A4B3DFF` | 16 |
| `brass_dark` | `#9B6D32FF` | 110 |
| `brass` | `#C4934BFF` | 520 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/goggle.png`
- 構図: 左前3/4。近側レンズを大きく、奥側を小さくして筒の奥行きを示す。
- 唯一のハイライト: 近側レンズだけの反射一点
- 唯一の経年箇所: 遠側筐体の擦れ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1650 |
| `outline` | `#102329FF` | 448 |
| `black` | `#11171AFF` | 192 |
| `lens_deep` | `#1F4A3AFF` | 236 |
| `black_mid` | `#222B2FFF` | 464 |
| `black_light` | `#374247FF` | 147 |
| `lens` | `#39725AFF` | 412 |
| `scuff` | `#6A4B3DFF` | 15 |
| `brass_dark` | `#9B6D32FF` | 110 |
| `brass` | `#C4934BFF` | 416 |
| `highlight` | `#F5DE9BFF` | 6 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/goggle.png`
- 構図: 片眼跳ね上げ。二本の筒を異なる角度にし、独立回転機構を読ませる。
- 唯一のハイライト: 下側レンズだけの反射一点
- 唯一の経年箇所: 上側筒の擦れ一つ
- 使用RGBA色数: **11**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1582 |
| `outline` | `#102329FF` | 500 |
| `black` | `#11171AFF` | 176 |
| `lens_deep` | `#1F4A3AFF` | 199 |
| `black_mid` | `#222B2FFF` | 485 |
| `black_light` | `#374247FF` | 266 |
| `lens` | `#39725AFF` | 257 |
| `scuff` | `#6A4B3DFF` | 15 |
| `brass_dark` | `#9B6D32FF` | 110 |
| `brass` | `#C4934BFF` | 500 |
| `highlight` | `#F5DE9BFF` | 6 |

## 11. スマートフォン — `sumaho.png`

代表素材・識別色: **黒い端末・消灯した濃紺画面**

参照資料:

- https://developer.apple.com/download/files/accessories/dimensional-drawings/iphone-16-pro-max.pdf
- https://support.apple.com/en-us/121029

### 案A

- ファイル: `proposal_a/assets/jidaiui/textures/item/sumaho.png`
- 構図: 斜め正面。消灯画面と薄い縁だけで汎用スマートフォンとして描く。
- 唯一のハイライト: 左上縁の反射
- 唯一の経年箇所: 右下角の擦れ一つ
- 使用RGBA色数: **8**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1907 |
| `outline` | `#102329FF` | 448 |
| `screen` | `#152B38FF` | 319 |
| `screen_light` | `#244553FF` | 863 |
| `black_light` | `#374247FF` | 85 |
| `scuff` | `#6A4B3DFF` | 20 |
| `brass` | `#C4934BFF` | 416 |
| `highlight` | `#F5DE9BFF` | 38 |

### 案B

- ファイル: `proposal_b/assets/jidaiui/textures/item/sumaho.png`
- 構図: 立体3/4。前面四辺形と右側面、無地の側面ボタンを色面で示す。
- 唯一のハイライト: 前面左上角の反射
- 唯一の経年箇所: 下側面の擦れ一つ
- 使用RGBA色数: **8**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1222 |
| `outline` | `#102329FF` | 448 |
| `screen` | `#152B38FF` | 389 |
| `screen_light` | `#244553FF` | 1332 |
| `black_light` | `#374247FF` | 242 |
| `scuff` | `#6A4B3DFF` | 20 |
| `brass` | `#C4934BFF` | 416 |
| `highlight` | `#F5DE9BFF` | 27 |

### 案C

- ファイル: `proposal_c/assets/jidaiui/textures/item/sumaho.png`
- 構図: 横置き3/4。下側面と無地のポート切欠きだけを加え、ロゴやUIは置かない。
- 唯一のハイライト: 横画面左上縁の反射
- 唯一の経年箇所: 右下角の擦れ一つ
- 使用RGBA色数: **8**（透明を含む）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 1483 |
| `outline` | `#102329FF` | 448 |
| `screen` | `#152B38FF` | 387 |
| `screen_light` | `#244553FF` | 1121 |
| `black_light` | `#374247FF` | 191 |
| `scuff` | `#6A4B3DFF` | 20 |
| `brass` | `#C4934BFF` | 416 |
| `highlight` | `#F5DE9BFF` | 30 |

## manifest と機械検査

- `manifest.json` の `files` は本体33枚とpreview 33枚の計66 PNGをすべて登録しています。
- `dimensions / byte_size / sha256 / rgba_color_count / alpha_values / contains_baked_text` は保存後の実ファイルから再測定しています。
- SHA-256はPNGのraw byte列から計算した大文字64桁です。
- 3案の同名PNGはディレクトリで分離し、採用後のResourceLocationはすべて `jidaiui:textures/item/<filename>` です。
- モデルJSON、言語ファイル、MOD登録コード、旧v5の任意描き直しは含めていません。

検査集計:

- 本体最大色数: **13 / 64**
- PNG登録数: **66**（本体33 + preview 33）
- 33本体はすべて異なるpixel arrayで、同一アイテムのA/B/Cも十分な差分があります。
- ZIPはrootに親フォルダを置かず、安全な相対パス、重複なし、暗号化なし、symlinkなし、CRC正常を再検査します。
