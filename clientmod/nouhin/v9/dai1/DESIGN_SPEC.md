# 時代クラフト v9 第1便 — DESIGN_SPEC

羊皮紙の海図・蒸気機関の歯車・月の石を各A/B/Cの3案。本番9枚、48/32/16pxのnearestプレビュー27枚。
正式名・ファイル名・番号はShouri.javaから読んでいます。採用案は絵ごとに選べます。

## 作図方法

- 64×64の整数格子で形を設計し、1格子を4×4pxとして256×256へ直接配置しました。写真・生成画像・高解像度原画の縮小はありません。
- 完成シルエットの外側2格子はoutline、次の2格子はbrass。最終256px画像で8近傍の8px＋8pxに厳密一致します。穴の周囲も同じです。
- 最小の描画要素は1格子=4px。半透明・AA・乱数・ぼかし・グラデーションはありません。
- 左上からの光。最高輝度highlightは各1連結成分、経年表現は各1か所。内部にoutline色は使いません。
- 護符・聖杯・海図下描き・旧v5の3枚を実際に表示し、縁と材料色を確認しました。既存PNGは参照専用です。
- ガラスの中も不透明の色面です。ガラスは1本の細い縁と背景色で表し、半透明は使いません。
- 羅針図は8方向の図形で、文字・数字・疑似文字・ロゴを描いていません。

## 共有パレット

v6 DESIGN_SPECの52色と依頼で指定済みの肌色3色、計55色を共有。今回さらに足した色は0色です。
肌色3色の役割は、羊皮紙の明面・基本面・右下の暗面です。追加理由は依頼の「地図は肌色」指定です。

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
| `hada_light` | `#F2DCBAFF` |
| `hada` | `#E4C39AFF` |
| `hada_dark` | `#C69E70FF` |

## 絵ごとの構図と色数

### 羊皮紙の海図 — A案（8203）

- ファイル: `proposal_a/assets/jidaiui/textures/item/kaizu.png`
- 意図: 下描きを継承した正面の巻物。左上の階段状海岸、右下の8方位羅針図、左寄りの赤い航路。地図の面を整理し、針先と線を4px以上にした。
- 唯一の最高輝度: 左巻きの上寄りにある一続きの反射
- 唯一の経年: 紙の左下の輪染み1つ
- 透明余白（左・上・右・下）: [12, 16, 12, 16] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 22432 |
| `outline` | `#102329FF` | 8064 |
| `brass_deep` | `#6F4E28FF` | 960 |
| `brass_dark` | `#9B6D32FF` | 224 |
| `brass` | `#C4934BFF` | 7360 |
| `brass_light` | `#D6B06BFF` | 928 |
| `highlight` | `#F5DE9BFF` | 144 |
| `paper_dark` | `#A98D61FF` | 384 |
| `red` | `#8E2D2DFF` | 288 |
| `enamel_green` | `#3C6A55FF` | 1088 |
| `jewel_blue` | `#376B83FF` | 1376 |
| `glass_deep` | `#426064FF` | 832 |
| `lens_deep` | `#1F4A3AFF` | 2720 |
| `lens` | `#39725AFF` | 3632 |
| `lens_light` | `#66A277FF` | 3584 |
| `hada_light` | `#F2DCBAFF` | 1552 |
| `hada` | `#E4C39AFF` | 8000 |
| `hada_dark` | `#C69E70FF` | 1968 |

### 羊皮紙の海図 — B案（8203）

- ファイル: `proposal_b/assets/jidaiui/textures/item/kaizu.png`
- 意図: 左上から右下へ開く斜めの巻物。左上に羅針図、下半分に大きな湾と島。航路を海の帯に沿わせる。
- 唯一の最高輝度: 左巻きの上寄りにある一続きの反射
- 唯一の経年: 紙の右上の染み1つ
- 透明余白（左・上・右・下）: [12, 12, 12, 12] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 28192 |
| `outline` | `#102329FF` | 7744 |
| `brass_deep` | `#6F4E28FF` | 864 |
| `brass_dark` | `#9B6D32FF` | 192 |
| `brass` | `#C4934BFF` | 7008 |
| `brass_light` | `#D6B06BFF` | 800 |
| `highlight` | `#F5DE9BFF` | 144 |
| `paper_dark` | `#A98D61FF` | 256 |
| `red` | `#8E2D2DFF` | 288 |
| `enamel_green` | `#3C6A55FF` | 928 |
| `jewel_blue` | `#376B83FF` | 2752 |
| `glass_deep` | `#426064FF` | 1984 |
| `lens_deep` | `#1F4A3AFF` | 2304 |
| `lens` | `#39725AFF` | 1904 |
| `lens_light` | `#66A277FF` | 2944 |
| `hada_light` | `#F2DCBAFF` | 1952 |
| `hada` | `#E4C39AFF` | 4368 |
| `hada_dark` | `#C69E70FF` | 912 |

### 羊皮紙の海図 — C案（8203）

- ファイル: `proposal_c/assets/jidaiui/textures/item/kaizu.png`
- 意図: 左の巻きを太く残した半開きの巻物。中央に最大の羅針図、上下に分かれる海岸、右端を回る航路。
- 唯一の最高輝度: 太い左巻きの上寄りにある一続きの反射
- 唯一の経年: 紙の左下の染み1つ
- 透明余白（左・上・右・下）: [12, 12, 12, 12] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 19968 |
| `outline` | `#102329FF` | 8192 |
| `brass_deep` | `#6F4E28FF` | 1216 |
| `brass_dark` | `#9B6D32FF` | 320 |
| `brass` | `#C4934BFF` | 7424 |
| `brass_light` | `#D6B06BFF` | 1152 |
| `highlight` | `#F5DE9BFF` | 144 |
| `paper_dark` | `#A98D61FF` | 896 |
| `red` | `#8E2D2DFF` | 272 |
| `enamel_green` | `#3C6A55FF` | 1120 |
| `jewel_blue` | `#376B83FF` | 1888 |
| `glass_deep` | `#426064FF` | 1600 |
| `lens_deep` | `#1F4A3AFF` | 2800 |
| `lens` | `#39725AFF` | 8160 |
| `lens_light` | `#66A277FF` | 3680 |
| `hada_light` | `#F2DCBAFF` | 2208 |
| `hada` | `#E4C39AFF` | 4240 |
| `hada_dark` | `#C69E70FF` | 256 |

### 蒸気機関の歯車 — A案（8204）

- ファイル: `proposal_a/assets/jidaiui/textures/item/haguruma.png`
- 意図: v5の正面の真鍮歯車と中心穴を継承。10歯のうち右下1歯を欠き、四角い軸穴と4つの肉抜き凹面を大きく描く。
- 唯一の最高輝度: 左上の歯の根元の1面
- 唯一の経年: 右下の欠け歯と同じ箇所に錆
- 透明余白（左・上・右・下）: [12, 12, 12, 12] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 31232 |
| `outline` | `#102329FF` | 10784 |
| `brass_deep` | `#6F4E28FF` | 3792 |
| `brass_dark` | `#9B6D32FF` | 4464 |
| `brass` | `#C4934BFF` | 12624 |
| `brass_light` | `#D6B06BFF` | 2416 |
| `highlight` | `#F5DE9BFF` | 96 |
| `scuff` | `#6A4B3DFF` | 128 |

### 蒸気機関の歯車 — B案（8204）

- ファイル: `proposal_b/assets/jidaiui/textures/item/haguruma.png`
- 意図: 8歯の鉄歯車。幅広い5本の輻と深い肉抜き面、真鍮の軸受、鍵溝付きの中心穴。左下1歯だけ欠ける。
- 唯一の最高輝度: 左上の外輪の1面
- 唯一の経年: 左下の欠け歯と同じ箇所に錆
- 透明余白（左・上・右・下）: [16, 16, 16, 16] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 31456 |
| `outline` | `#102329FF` | 8896 |
| `brass_dark` | `#9B6D32FF` | 2240 |
| `brass` | `#C4934BFF` | 8192 |
| `brass_light` | `#D6B06BFF` | 416 |
| `highlight` | `#F5DE9BFF` | 96 |
| `iron_deep` | `#303B3FFF` | 5792 |
| `iron` | `#586368FF` | 2528 |
| `iron_light` | `#7D8889FF` | 2592 |
| `steel_light` | `#A8B0ADFF` | 3056 |
| `scuff` | `#6A4B3DFF` | 272 |

### 蒸気機関の歯車 — C案（8204）

- ファイル: `proposal_c/assets/jidaiui/textures/item/haguruma.png`
- 意図: 厚みを見せる斜めの12歯真鍮歯車。3つの大きな腎臓形の肉抜き、四角い斜めの軸穴。右上1歯だけ欠ける。
- 唯一の最高輝度: 前面の左上の1面
- 唯一の経年: 右上の欠け歯と同じ箇所に錆
- 透明余白（左・上・右・下）: [8, 24, 8, 16] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 30752 |
| `outline` | `#102329FF` | 10144 |
| `brass_deep` | `#6F4E28FF` | 832 |
| `brass_dark` | `#9B6D32FF` | 6976 |
| `brass` | `#C4934BFF` | 11856 |
| `brass_light` | `#D6B06BFF` | 864 |
| `highlight` | `#F5DE9BFF` | 96 |
| `iron_deep` | `#303B3FFF` | 3824 |
| `scuff` | `#6A4B3DFF` | 192 |

### 月の石 — A案（8205）

- ファイル: `proposal_a/assets/jidaiui/textures/item/tsukinoishi.png`
- 意図: v5の面取りしたガラスケースと幅広台座を継承。岩の灰色の色面と窪みを大きく整理。ガラスは左上から左側へ続く1本の縁で示す。
- 唯一の最高輝度: ガラスの左上縁の1か所
- 唯一の経年: 台座右前角の擦れ1つ
- 透明余白（左・上・右・下）: [16, 16, 16, 16] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 23536 |
| `outline` | `#102329FF` | 6912 |
| `brass_dark` | `#9B6D32FF` | 288 |
| `brass` | `#C4934BFF` | 6304 |
| `highlight` | `#F5DE9BFF` | 48 |
| `iron_deep` | `#303B3FFF` | 4544 |
| `iron` | `#586368FF` | 1504 |
| `iron_light` | `#7D8889FF` | 3168 |
| `steel_light` | `#A8B0ADFF` | 2752 |
| `glass_deep` | `#426064FF` | 912 |
| `glass` | `#789295FF` | 2464 |
| `glass_light` | `#AFC4BEFF` | 608 |
| `screen_light` | `#244553FF` | 12432 |
| `scuff` | `#6A4B3DFF` | 64 |

### 月の石 — B案（8205）

- ファイル: `proposal_b/assets/jidaiui/textures/item/tsukinoishi.png`
- 意図: ケースを外し、背の高い角張った標本を低い台座へ直接置く。中央の大きな窪みと右下の小さな窪み。
- 唯一の最高輝度: 岩の左上の小さな1面
- 唯一の経年: 台座右前角の擦れ1つ
- 透明余白（左・上・右・下）: [20, 12, 20, 16] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 30800 |
| `outline` | `#102329FF` | 7536 |
| `brass` | `#C4934BFF` | 6912 |
| `highlight` | `#F5DE9BFF` | 32 |
| `iron_deep` | `#303B3FFF` | 4320 |
| `iron` | `#586368FF` | 3312 |
| `iron_light` | `#7D8889FF` | 5744 |
| `steel_light` | `#A8B0ADFF` | 2336 |
| `glass_deep` | `#426064FF` | 3120 |
| `glass` | `#789295FF` | 1296 |
| `scuff` | `#6A4B3DFF` | 128 |

### 月の石 — C案（8205）

- ファイル: `proposal_c/assets/jidaiui/textures/item/tsukinoishi.png`
- 意図: 台座のない一塊の月岩。左上の大きな窪みと右下の小さな窪み、右側の暗い破断面で立体を見せる。
- 唯一の最高輝度: 岩の左上の小さな1面
- 唯一の経年: 右肩に1つの欠け面
- 透明余白（左・上・右・下）: [12, 16, 16, 16] px
- 3px未満の細い要素: 0.0000%（上限10%）

| role | RGBA | pixel数 |
|---|---|---:|
| `transparent` | `#00000000` | 29024 |
| `outline` | `#102329FF` | 6928 |
| `brass` | `#C4934BFF` | 6272 |
| `highlight` | `#F5DE9BFF` | 32 |
| `iron_deep` | `#303B3FFF` | 6656 |
| `iron` | `#586368FF` | 6576 |
| `iron_light` | `#7D8889FF` | 6064 |
| `steel_light` | `#A8B0ADFF` | 3456 |
| `black_light` | `#374247FF` | 528 |

## 自己検査

- 本番の実在数: 9 / 9。プレビューの実在数: 27 / 27。
- 保存済みのPNGを開き直し、寸法・容量・SHA-256・色数・不透明度・全色のpixel数を測定しました。
- 2回作画して9枚のPNG byteがすべて一致しました。同一環境での再現に乱数は必要ありません。
- 最大色数: 18 / 64（透明を含む）。
- 細い要素の最大値: 0.0000%。
- 個別の正本検査結果はself_check.json、受け入れ検査の出力はacceptance_check.txtを参照してください。

## 再生成

`python source/draw_dai1.py --clientmod <clientmodの絶対パス> --out <空の出力ディレクトリ>`

Pillow・NumPyと、リポジトリのtools/dotto.pyを使用。使用した正本ファイルのSHA-256はmanifest.reference_filesに記録しています。
A/B/Cの選定後、採用するPNGを絵ごとにv8へコピーする作業は発注元が行う想定です。第2便は第1便の選定・評価後に進めます。

## 目視確認と選定用資料

- `hikaku/overview.png` に9案と確定済み2枚の見比べ表を収録。
- `hikaku/compare_*.png` は各3案を256px、48px平均、48/32/16px nearestで比較。暗色・羊皮紙色の両背景を確認しました。
- 16pxの実寸と整数倍拡大を見て、巻物・歯車・ケース入り標本／台座／岩の塊の違いを確認しました。細かい航路・肉抜き面の説明は48px以上でより明瞭です。最終的な意匠の選定は発注元が行います。
- 比較用PNG4枚は本番9枚・preview27枚とは別です。比較用だけに名札と背景を置き、本番と縮小PNGは透明背景・無文字です。
- 実行環境: Python 3.14 / Pillow 12.2.0 / NumPy 2.4.6。

## 正本の受け入れ検査

`python clientmod/tests/dai7_kakunin.py clientmod/nouhin/v9/dai1 --hikaku-nashi`

納品先の実ファイルで **PASS 246 / FAIL 0**、終了コード0。比較表の自動生成だけを省略するオプションで、検査項目はすべて実行しました。
納品前に本番9枚・縮小27枚を再度数え、manifest全40 PNGのハッシュ・容量・寸法・色数・alphaと9枚の色別pixel数を照合しました。
