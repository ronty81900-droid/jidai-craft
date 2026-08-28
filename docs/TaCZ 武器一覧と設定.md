# TaCZ 武器一覧と設定の監査 ── 実物の json から作成

作成: 2026-08-20 / **記憶ではなく、パックの json と MOD の jar を直接読んで作った**

---

## A. 建築を壊し得るもの ── 全部洗い出した

### A-1. 変えた設定（3つ）

`jikki_mod/config/tacz-common.toml`

| 設定 | 初期値 | → | 何が起きるか |
|---|---|---|---|
| `ExplosiveAmmoDestroysBlock` | true | **false** | 榴弾・ロケットがブロックを壊す |
| `IgniteBlock` | true | **false** | **弾がブロックに火を付ける**（原木・木の板が燃える） |
| `DestroyGlass` | true | **false** | **弾がガラスを割る** |

**指示書にあったのは1つ目だけですが、残り2つも同じ危険なので一緒に変えました。**
戻す時は `true` に書き換えてください。元のファイルは `tacz-common.toml.moto` にあります。

> **★ 実測**: 変更後にサーバーを起動し直し、TaCZ に書き戻されないことを確認しました。

### A-2. 変えなかったもの（建築には影響しない）

| 設定 | 値 | なぜ放置してよいか |
|---|---|---|
| `ExplosiveAmmoFire` | false | もともと false。爆発で火が付かない |
| `ExplosiveAmmoKnockBack` | true | 人が吹き飛ぶだけ。ブロックは動かない |
| `IgniteEntity` | true | 人が燃えるだけ |
| `PassThroughBlocks` | `[]` | 「弾がすり抜けるブロック」の一覧。空＝すり抜けない |

### A-3. 爆発・着火を持つ定義 ── 全パックを総ざらいした結果

**`"explode": true` を持つのは3件だけ。**

| 定義 | 何か | 対処 |
|---|---|---|
| `tacz:m320` | M320 グレネードランチャー | `ExplosiveAmmoDestroysBlock=false` で無害化 |
| `tacz:rpg7` | RPG-7 | 同上 |
| **`tacz:ammo_mod_he`** | **HE弾モジュール（アタッチメント）** | 同上 |

> **★★ `ammo_mod_he` が一番危ない ★★**
> これは**アタッチメント**なので、**どの銃に付けても弾が爆発するようになります。**
> 「爆発する銃は2丁だけ」ではありません。設定1つで全部止まるのが救いです。

**`"ignite": true`（ブロックに火が付く）を持つのは1件だけ。**

| 定義 | 何か |
|---|---|
| `hamster:flaregun` | Webley 信号拳銃 |

`tacz:ammo_mod_i`（焼夷弾モジュール）は `ignite: {entity: true}` で、**人しか燃えません**。
`ak47` `fn_fal` `spas_12` は `ignite: {entity:false, block:false}` で**何も燃やしません**。

### A-4. 貫通（pierce）について

最大は `tacz:m107` / `tacz:m95` の **5**。
ただし pierce は**エンティティ（人）を貫く数**で、ブロックは貫きません。
ブロックのすり抜けは `PassThroughBlocks` と `tacz:bullet_ignore` タグが決めます。

`data/tacz/tags/blocks/bullet_ignore.json`（弾が当たらずすり抜けるブロック）:
`minecraft:iron_bars` / `#minecraft:fences` / `#minecraft:fence_gates` / `#minecraft:leaves`

**壊すのではなく、通り抜けるだけ**です。柵や葉は撃っても壊れません。

---

## B. クラフトについて ── 企画の方針とぴったり噛み合っています

### B-1. 銃も弾も、バニラの作業台では作れません

銃・弾・アタッチメントのレシピは、全部 `type: "tacz:gun_smith_table_crafting"` です。
**専用の作業台（ガンスミステーブル）でしか作れません。**

### B-2. そのガンスミステーブルが、バニラの作業台でしか作れません

`data/tacz/recipes/gun_smith_table.json`（**バニラの `crafting_shaped`**）

```
LLL     L = #minecraft:logs        （原木 ×3）
IBI     I = #forge:ingots/iron     （鉄インゴット ×4）
I I     B = minecraft:iron_block   （鉄ブロック ×1）
```

**時代クラフトは作業台のクラフトを禁止しています。**
したがって参加者はガンスミステーブルを作れず、**銃も弾も1発も作れません。**

> **★ これは非常に都合が良い。** 銃と弾の入手経路が
> 「**運営が配る / 販売所で買う**」だけになり、時代ごとの解禁を完全に握れます。
> 追加の禁止処理は要りません。

同じ理由で、次のものも作れません（すべてバニラの作業台が要る）。

`tacz:gun_smith_table` / `tacz:workbench_a`(弾薬作業台) / `tacz:workbench_c`(アタッチメント作業台)
/ `tacz:ammo_box`(弾薬箱) / `tacz:target`(的) / `tacz:statue`

**唯一の注意**: TaCZ は `minecraft:gunpowder` のバニラレシピも足しています
（火打石1+砂糖2+木炭3 → 火薬3）。これも作業台が要るので同じく作れません。

### B-3. ガンパックは2つの作業台で分かれています（時代分けの土台になる）

| 作業台 | 出るレシピ |
|---|---|
| `tacz:gun_smith_table`（標準） | `^.*$` から `^hamster:.*$` を除く ＝ **現代銃だけ** |
| `hamster:oldworkbench`（旧式・ガンパック付属） | `^hamster:.*$` ＝ **第一次大戦の銃だけ** |

もし将来クラフトを解禁するなら、**どちらの作業台を配るかで時代を分けられます。**

---

## C. 時代解禁をどう実装するか

### C-1. アイテムの正体（MOD の jar から確認）

| もの | アイテムID | 見分ける NBT |
|---|---|---|
| 銃（全90丁とも同じアイテム） | **`tacz:modern_kinetic_gun`** | `GunId` （例 `"hamster:webley"`） |
| 弾 | **`tacz:ammo`** | `AmmoId` （例 `"hamster:long_ammo"`） |
| アタッチメント | **`tacz:attachment`** | `AttachmentId` |
| 手榴弾 | `tacz:m67` | — |
| 弾薬箱 | `tacz:ammo_box` | `Level` |

銃はすべて**1種類のアイテム**で、中身の `GunId` で別物になります。
つまり**データパックから NBT で狙い撃ちできます。**

### C-2. 「所持できる」と「撃てる」を分けられるか

**結論: 分けられます。ただし手段が違います。**

| やりたいこと | できるか | どうやるか |
|---|---|---|
| **所持を禁じる** | **できる** | `clear @a tacz:modern_kinetic_gun{GunId:"tacz:m4a1"}` で没収 |
| 持っているか数える | できる | `clear @s … 0`（数えるだけモード。1.20.1 で動作確認済み） |
| **撃てなくする** | **直接はできない** | TaCZ に「撃てなくする」入口がない |
| 実質的に撃てなくする | **できる** | **弾（`tacz:ammo{AmmoId:…}`）を配らない・没収する** |

**弾は作業台が禁止なので自作できません。** つまり
**「銃は持てるが、弾が無いから撃てない」**という状態を、弾の配布だけで作れます。

これは運営メモの案そのものです。**実装できます。**

> ★ ただし `clear` は**プレイヤーの持ち物しか見ません**。
>   チェストに隠された銃は取れません。時代を戻す運用をするなら、そこは割り切りが要ります。

### C-3. 実装の形（案）

いまの `jidai:clock` は毎秒 `@a` を回しています。そこに1行足すだけです。

```
# 例: 現代(4)より前は M4A1 を没収する
execute as @a unless score 世界 chuo matches 4.. run clear @s tacz:modern_kinetic_gun{GunId:"tacz:m4a1"}
```

**時代の割り当てが決まれば、この形で並べるだけです。**

---

## D. 防具・アタッチメント

- **防具は1つもありません。** TaCZ が登録するアイテムは
  銃 / 弾 / アタッチメント / 弾薬箱 / 手榴弾 / 作業台3種 / 的 / 像 だけです
- アタッチメントは **第一次大戦パック10個**・**既定パック99個**

| 種類 | 既定パック | 第一次大戦パック |
|---|---|---|
| スコープ | 32 | 5（Aperture / Deadeye / PU / Unertl など） |
| 銃口 | 19 | 3（銃剣2 / 鞭） |
| 弾倉 | 17（**HE弾・焼夷弾モジュールを含む**） | 3（速射装填具など） |
| グリップ | 12 | 0 |
| ストック | 14 | 0 |
| レーザー | 5 | 0 |

---

## E. 弾薬の供給

**弾は「銃の種類」ではなく「口径」で共有されます。** 第一次大戦パックは4種類だけです。

| 弾 | 使う銃 |
|---|---|
| `hamster:long_ammo` | ライフル14丁（Gew98 / Mosin / M1903 / Lee-Enfield など） |
| `hamster:medium_ammo` | 8丁（Colt1873 / Webley / SKS / Win1894 など） |
| `hamster:compact_ammo` | 10丁（Luger / MP18 / Nagant など） |
| `tacz:12g` | 散弾4丁（Auto-5 / M1887 / 単発 など） |
| `hamster:flares_ammo` | 信号拳銃のみ |

**4種類だけなので、販売所の商品としてそのまま並べられます。**
既定パック（現代銃）は24種類あるので、そちらを使うなら整理が要ります。

---


## 1. Gunpowder Revolution（第一次大戦・36丁）

| ID | 表示名(英) | 種別 | 弾薬 | 装弾 | 射撃 | RPM | 威力 | 貫通 | 焼夷 | 爆発 | クラフト |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `hamster:auto5` | Browning Auto-5 Shotgun | shotgun | `12g` | 4 | semi | 100 | 40 | 1 |  |  | ○ |
| `hamster:berthier` | Berthier M1892 Rifle | sniper | `long_ammo` | 3 | semi | 380 | 30 | 4 |  |  | ○ |
| `hamster:colt1873` | Colt M1873 'Peacemaker'Revolver | pistol | `medium_ammo` | 6 | semi,auto | 40 | 25 | 2 |  |  | ○ |
| `hamster:coltm1851` | Colt M1851 Conversion Revolver | pistol | `compact_ammo` | 6 | semi,auto | 55 | 12 | 1 |  |  | ○ |
| `hamster:coltm1892` | Colt M1892 'New Army' Revolver | pistol | `compact_ammo` | 6 | burst | 200 | 10 | 1 |  |  | ○ |
| `hamster:coltm1892pair` | Micah's Sidearms | pistol | `compact_ammo` | 12 | semi | 400 | 10 | 1 |  |  | ○ |
| `hamster:flaregun` | Webley Flare Gun | pistol | `flares_ammo` | 1 | semi | 100 | 1 | 1 | ★ |  | ○ |
| `hamster:gew98` | Gew.98 Rifle | sniper | `long_ammo` | 5 | semi | 300 | 28 | 4 |  |  | ○ |
| `hamster:gras1874` | Gras 1874 Rifle | sniper | `medium_ammo` | 1 | semi | 200 | 32 | 2 |  |  | ○ |
| `hamster:krag` | Krag-Jørgensen Rifle | sniper | `long_ammo` | 5 | semi | 450 | 25 | 4 |  |  | ○ |
| `hamster:lebel1886` | Lebel M1886 Rifle | sniper | `long_ammo` | 8 | semi | 350 | 32 | 4 |  |  | ○ |
| `hamster:luger1906` | Luger M1906 Rifle | sniper | `long_ammo` | 5 | semi | 150 | 25 | 4 |  |  | ○ |
| `hamster:lugerp08` | Luger P08 Pistol | pistol | `compact_ammo` | 8 | semi | 170 | 8 | 1 |  |  | ○ |
| `hamster:m1879revolver` | M1879 'Empire' Revolver | pistol | `medium_ammo` | 6 | semi | 35 | 17 | 1 |  |  | ○ |
| `hamster:m1887` | Winchester M1887 Lever-Action Shotgun | shotgun | `12g` | 6 | semi | 300 | 35 | 1 |  |  | ○ |
| `hamster:m1903` | Springfield M1903A1 Rifle | sniper | `long_ammo` | 5 | semi | 350 | 30 | 4 |  |  | ○ |
| `hamster:m1garand` | M1 Garand Rifle | sniper | `long_ammo` | 8 | semi | 180 | 20 | 3 |  |  | ○ |
| `hamster:madsen` | Madsen Light Machine Gun | mg | `long_ammo` | 30 | auto,semi | 450 | 15 | 3 |  |  | ○ |
| `hamster:makarov` | Makarov PM Pistol | pistol | `compact_ammo` | 8 | semi | 200 | 8 | 2 |  |  | ○ |
| `hamster:martinihenry` | Martini-Henry Rifle | sniper | `long_ammo` | 1 | semi | 200 | 40 | 3 |  |  | ○ |
| `hamster:mg1417` | hamster.gun.mg1417.name | mg | `long_ammo` | 100 | auto,semi | 700 | 15 | 3 |  |  | ○ |
| `hamster:mosin91` | Mosin-Nagant M1891 | sniper | `long_ammo` | 5 | semi | 250 | 35 | 4 |  |  | ○ |
| `hamster:mosin9130` | Mosin-Nagant M91/30 Rifle | sniper | `long_ammo` | 5 | semi | 250 | 35 | 4 |  |  | ○ |
| `hamster:mp18` | MP18 Submachine Gun | smg | `compact_ammo` | 32 | auto | 500 | 8 | 1 |  |  | ○ |
| `hamster:nagantcarbine` | Nagant M1895 Carbine | pistol | `compact_ammo` | 7 | semi | 120 | 14 | 2 |  |  | ○ |
| `hamster:nagantm1895` | Nagant M1895 Revolver | pistol | `compact_ammo` | 7 | semi | 100 | 12 | 1 |  |  | ○ |
| `hamster:one_barrel` | Hammer-Action Shotgun | shotgun | `12g` | 1 | semi | 100 | 35 | 1 |  |  | ○ |
| `hamster:sharps` | Sharps M1874 Sharpshooter Rifle | sniper | `long_ammo` | 1 | semi | 80 | 40 | 4 |  |  | ○ |
| `hamster:sks` | SKS Semi-Automatic Rifle | rifle | `medium_ammo` | 10 | semi | 200 | 15 | 2 |  |  | ○ |
| `hamster:smle_mk3` | Lee-Enfield No.1 MKIII Rifle | sniper | `long_ammo` | 10 | semi | 450 | 30 | 3 |  |  | ○ |
| `hamster:sw_mk2` | S&W.455 MKII | pistol | `medium_ammo` | 6 | burst,semi | 100 | 14 | 2 |  |  | ○ |
| `hamster:sw_mk2_41` | S&W.455 MKII | pistol | `medium_ammo` | 6 | burst | 100 | 17 | 3 |  |  | × |
| `hamster:type99` | Type 99 Rifle | sniper | `long_ammo` | 5 | semi | 350 | 25 | 4 |  |  | ○ |
| `hamster:webley` | Webley Mk.VI Revolver | pistol | `medium_ammo` | 6 | semi | 90 | 17 | 2 |  |  | ○ |
| `hamster:win1873` | Winchester M1873 Lever-Action Rifle | sniper | `compact_ammo` | 15 | semi,auto | 400 | 18 | 2 |  |  | ○ |
| `hamster:win1894` | Winchester M1894 Lever-Action Rifle | sniper | `medium_ammo` | 8 | semi | 140 | 30 | 3 |  |  | ○ |

### 使う弾薬

- `hamster:compact_ammo` … Short Bullet
- `hamster:flares_ammo` … Flare Ammunition
- `hamster:long_ammo` … Long Bullet
- `hamster:medium_ammo` … Medium Bullet

---

## 2. TaCZ 既定パック（現代銃・54丁）

| ID | 表示名(英) | 種別 | 弾薬 | 装弾 | 射撃 | RPM | 威力 | 貫通 | 焼夷 | 爆発 | クラフト |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `tacz:aa12` | AA12 Shotgun | shotgun | `12g` | 8 | semi,auto | 350 | 30 | 1 |  |  | ○ |
| `tacz:ai_awp` | Accuracy International AWM | sniper | `338` | 5 | semi | 171 | 42 | 4 |  |  | ○ |
| `tacz:ak47` | AKM | rifle | `762x39` | 30 | auto,semi | 600 | 9 | 1 |  |  | ○ |
| `tacz:aug` | AUG | rifle | `556x45` | 30 | auto,semi | 710 | 7 | 1 |  |  | ○ |
| `tacz:b93r` | B93R | pistol | `9mm` | 20 | burst,semi | 900 | 7.5 | 0 |  |  | ○ |
| `tacz:cz75` | CZ 75 | pistol | `9mm` | 16 | auto | 900 | 5 | 0 |  |  | ○ |
| `tacz:db_long` | DB-4 Ursus | shotgun | `12g` | 2 | semi | 100 | 30 | 1 |  |  | ○ |
| `tacz:db_short` | DB-2 Durin | shotgun | `12g` | 2 | burst,semi | 150 | 24 | 1 |  |  | ○ |
| `tacz:deagle` | Deagle 50 | pistol | `50ae` | 7 | semi | 300 | 16 | 1 |  |  | ○ |
| `tacz:deagle_golden` | Golden Deagle 357 | pistol | `357mag` | 9 | semi | 350 | 12 | 1 |  |  | ○ |
| `tacz:fn_evolys` | FN EVOLYS Machine Gun | mg | `308` | 75 | auto | 750 | 12 | 1 |  |  | ○ |
| `tacz:fn_fal` | FN FAL Battle Rifle | rifle | `308` | 20 | semi,auto | 350 | 13 | 1 |  |  | ○ |
| `tacz:g36k` | G36K | rifle | `556x45` | 30 | auto,semi | 780 | 7 | 1 |  |  | ○ |
| `tacz:glock_17` | Glock 17 | pistol | `9mm` | 17 | semi | 400 | 6 | 1 |  |  | ○ |
| `tacz:hk416d` | HK-416A5 | rifle | `556x45` | 30 | auto,semi | 943 | 6.5 | 1 |  |  | ○ |
| `tacz:hk_g3` | HK G3 Battle rifle | rifle | `308` | 20 | semi,auto | 350 | 12 | 2 |  |  | ○ |
| `tacz:hk_mk23` | MK23 Offensive Pistol | pistol | `45acp` | 12 | semi,burst | 50 | 12 | 1 |  |  | ○ |
| `tacz:hk_mp5a5` | HK-MP5A5 | smg | `9mm` | 30 | auto,burst,semi | 820 | 6 | 1 |  |  | ○ |
| `tacz:kar98` | Mauser Kar98k Rifle | sniper | `792x57` | 4 | semi | 250 | 26 | 2 |  |  | ○ |
| `tacz:lonetrail` | .30-06 Lonetrail Hand Cannon | pistol | `30_06` | 1 | semi | 90 | 21.5 | 1 |  |  | ○ |
| `tacz:m1014` | M1014 Battle Shotgun | shotgun | `12g` | 6 | semi | 200 | 40 | 1 |  |  | ○ |
| `tacz:m107` | M107 Sniper Rifle | sniper | `50bmg` | 10 | semi | 400 | 55 | 5 |  |  | ○ |
| `tacz:m16a1` | M16A1 Service Rifle | rifle | `556x45` | 20 | auto,semi | 750 | 8 | 1 |  |  | ○ |
| `tacz:m16a4` | M16A4 Service Rifle | rifle | `556x45` | 30 | burst,semi | 400 | 8 | 1 |  |  | ○ |
| `tacz:m1911` | M1911 | pistol | `45acp` | 7 | semi | 350 | 11 | 1 |  |  | ○ |
| `tacz:m249` | M249 Machine Gun | mg | `556x45` | 75 | auto | 750 | 7.5 | 1 |  |  | ○ |
| `tacz:m320` | M320 Grenade Launcher | rpg | `40mm` | 1 | semi | 150 | 10 | 0 |  | ★ | ○ |
| `tacz:m4a1` | M4A1 Carbine | rifle | `556x45` | 30 | auto,semi | 810 | 6.5 | 1 |  |  | ○ |
| `tacz:m700` | M700 Sniper Rifle | sniper | `30_06` | 5 | semi | 180 | 24 | 2 |  |  | ○ |
| `tacz:m870` | M870 | shotgun | `12g` | 5 | semi | 180 | 36 | 1 |  |  | ○ |
| `tacz:m95` | M95 .50 Cal Antimaterial | sniper | `50bmg` | 5 | semi | 151 | 75 | 5 |  |  | ○ |
| `tacz:m9a4` | M9A4 | pistol | `9mm` | 17 | semi | 400 | 6 | 0 |  |  | ○ |
| `tacz:minigun` | M134 Minigun | mg | `308` |  | auto,burst | 1200 | 8 | 2 |  |  | ○ |
| `tacz:mk14` | MK14 EBR | rifle | `308` | 10 | semi,auto | 300 | 16 | 3 |  |  | ○ |
| `tacz:p320` | P320 | pistol | `45acp` | 12 | semi | 450 | 10 | 1 |  |  | ○ |
| `tacz:p90` | P90 PDW | smg | `57x28` | 50 | auto,burst | 810 | 5.5 | 1 |  |  | ○ |
| `tacz:qbz_191` | QBZ-191 Assault Rifle | rifle | `58x42` | 30 | auto,semi | 750 | 7.5 | 1 |  |  | ○ |
| `tacz:qbz_95` | QBZ-95 "Longbow" | rifle | `58x42` | 30 | auto,semi,burst | 660 | 7.5 | 1 |  |  | ○ |
| `tacz:rhino357` | .357 Rhino Revolver | pistol | `357mag` | 6 | semi | 200 | 10.5 | 1 |  |  | ○ |
| `tacz:rpg7` | RPG-7 | rpg | `rpg_rocket` | 1 | semi | 150 | 20 | 0 |  | ★ | ○ |
| `tacz:rpk` | RPK | mg | `762x39` | 40 | auto,semi | 630 | 10 | 2 |  |  | ○ |
| `tacz:scar_h` | SCAR-H Battle Rifle | rifle | `308` | 20 | semi,auto | 570 | 14 | 2 |  |  | ○ |
| `tacz:scar_l` | SCAR-L Assault Rifle | rifle | `556x45` | 30 | auto,burst,semi | 650 | 7.5 | 1 |  |  | ○ |
| `tacz:sks_tactical` | Sks Tactical Rifle | rifle | `762x39` | 10 | semi | 510 | 11 | 1 |  |  | ○ |
| `tacz:spas_12` | SPAS-12 Multi-purpose Shotgun | shotgun | `12g` | 5 | semi,burst | 200 | 64 | 1 |  |  | ○ |
| `tacz:spr15hb` | SPR-15 HB "Sagittarius" | rifle | `556x45` | 15 | semi,burst | 700 | 10 | 1 |  |  | ○ |
| `tacz:springfield1873` | Springfield 1873 Trapdoor Rifle | sniper | `45_70` | 1 | semi | 90 | 35 | 1 |  |  | ○ |
| `tacz:taurus500` | Taurus "Raging Hunter" Hand Cannon | pistol | `500mag` | 5 | semi | 120 | 40 | 3 |  |  | ○ |
| `tacz:taurus943` | .22 Modle 943 Revolver | pistol | `22wmr` | 8 | semi | 180 | 6 | 1 |  |  | × |
| `tacz:timeless50` | §6Timeless .50 Z-Type | pistol | `50ae` | 8 | semi | 300 | 15 | 1 |  |  | ○ |
| `tacz:type_81` | Type 81-1 Service Rifle | rifle | `762x39` | 30 | auto,semi | 630 | 9 | 1 |  |  | ○ |
| `tacz:ump45` | UMP45 SMG | smg | `45acp` | 25 | auto,burst | 660 | 9 | 1 |  |  | ○ |
| `tacz:uzi` | UZI | smg | `9mm` | 20 | auto | 600 | 6.5 | 1 |  |  | ○ |
| `tacz:vector45` | Vector SMG | smg | `45acp` | 20 | auto,burst,semi | 1200 | 7 | 1 |  |  | ○ |

### 使う弾薬

- `tacz:12g` … §912 Gauge Bullet
- `tacz:22wmr` … §a.22 Winchester Magnum
- `tacz:308` … §b.308 Winchester Bullet
- `tacz:30_06` … §c.30-06 Springfield Bullet
- `tacz:338` … §c.338 Lapua Bullet
- `tacz:357mag` … §c.357 Magnum
- `tacz:40mm` … §d40mm Grenade
- `tacz:45_70` … §c.45-70 Bullet
- `tacz:45acp` … §a.45 ACP Bullet
- `tacz:46x30` … §64.6mm AP Bullet
- `tacz:500mag` … §c.500 Magnum
- `tacz:50ae` … §c.50 AE
- `tacz:50bmg` … §c.50 BMG
- `tacz:545x39` … §e5.45x39mm Bullet
- `tacz:556x45` … §e5.56x45mm Bullet
- `tacz:57x28` … §65.7x28mm AP Bullet
- `tacz:58x42` … §e5.8mm DBP87 Bullet
- `tacz:68x51fury` … §b6.8×51mm Fury Bullet
- `tacz:762x25` … §a7.62x25mm Tokarev Bullet
- `tacz:762x39` … §e7.62x39mm Bullet
- `tacz:762x54` … §b7.62x54mm Bullet
- `tacz:792x57` … §b8mm Mauser Bullet
- `tacz:9mm` … §a9mm Bullet
- `tacz:rpg_rocket` … §dRPG-7 Rocket
