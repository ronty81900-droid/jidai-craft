# =============================================================
# jidai:kane/uru ── 売却所。手持ちの貴金属をまとめて全部売る
#
# ★★ 本番では、この関数はもう呼ばれていません（2026-08-20 時点） ★★
#   売却はプラグインの銀行の画面（Ginko）に統合しました。
#   それでも消していないのは、次の2つの理由です。
#     2. 売却の単価（鉄1/ラピス2/金4/ダイヤ8）の正本がここにある。
#        プラグインの Ginko.NEDAN と食い違っていないかを確かめる基準
#   拠点のエメラルドブロックを押した本人として走る。
#   鉄1 / ラピス2 / 金4 / ダイヤ8 で個人の金にする。
# =============================================================

# 次も押せるように達成を取り消す
# ★ ここに `advancement revoke @s only jidai:oshita_emerald` があったが、
#   1.20.1 に minecraft:default_block_use の起動条件が無い(実測)ため、
#   そのアドバンスメント自体を消した。存在しないものを revoke すると
#   【黙って失敗するだけ】で気づけないので、行ごと外してある。

# --- 中央では売らせない ---------------------------------------
# 中央のエメラルドは「買う」ためだけ。持ち帰らせるのが設計の要。
execute if entity @e[type=marker,tag=jidai_uru_chuo,distance=..4] run tellraw @s [{"text":"[売却所] ","color":"green"},{"text":"中央では売れません。拠点へ持ち帰ってください","color":"red"}]
execute if entity @e[type=marker,tag=jidai_uru_chuo,distance=..4] run playsound minecraft:entity.villager.no player @s
execute if entity @e[type=marker,tag=jidai_uru_chuo,distance=..4] run return 0

#
# ★掘ること自体は禁止しない。売れないだけ。

# 拠点の売却所の近くでなければ何もしない
execute unless entity @e[type=marker,tag=jidai_uru,distance=..4] run return 0

# =============================================================
# 順番が命。「数える → 金を足す → 実物を消す」の順で書く。
# 先に消してから足すと、途中で止まった時に
# 「アイテムだけ消えて金が入らない」事故になる。
# =============================================================

# --- 1) 数える ------------------------------------------------
# clear の最後の 0 = 1個も消さずに個数だけ返す「数えるだけモード」
execute store result score #tetsu sagyou run clear @s minecraft:iron_ingot 0
execute store result score #lapis sagyou run clear @s minecraft:lapis_lazuli 0
execute store result score #kin sagyou run clear @s minecraft:gold_ingot 0
execute store result score #dia sagyou run clear @s minecraft:diamond 0
# ★ ネザライトは石を掘った時に 0.1% で出る(プラグイン側)。
#   この世界ではネザーへ行かないので、石掘りだけが入手経路。
execute store result score #neza sagyou run clear @s minecraft:netherite_ingot 0

# --- 2) 合計額を計算する --------------------------------------
# ★★ 2026-08-20 に改定: 鉄1 / ラピス5 / 金10 / ダイヤ15 / ネザライト30 ★★
#   (前は 鉄1 / ラピス2 / 金4 / ダイヤ8)
#   ★ プラグインの Ginko.NEDAN と必ず同じ値にすること。
#     tests/verify_jidai.py が両方を突き合わせている。
scoreboard players set #gokei sagyou 0
scoreboard players operation #gokei sagyou += #tetsu sagyou

scoreboard players operation #tmp sagyou = #lapis sagyou
scoreboard players set #bai sagyou 5
scoreboard players operation #tmp sagyou *= #bai sagyou
scoreboard players operation #gokei sagyou += #tmp sagyou

scoreboard players operation #tmp sagyou = #kin sagyou
scoreboard players set #bai sagyou 10
scoreboard players operation #tmp sagyou *= #bai sagyou
scoreboard players operation #gokei sagyou += #tmp sagyou

scoreboard players operation #tmp sagyou = #dia sagyou
scoreboard players set #bai sagyou 15
scoreboard players operation #tmp sagyou *= #bai sagyou
scoreboard players operation #gokei sagyou += #tmp sagyou

scoreboard players operation #tmp sagyou = #neza sagyou
scoreboard players set #bai sagyou 30
scoreboard players operation #tmp sagyou *= #bai sagyou
scoreboard players operation #gokei sagyou += #tmp sagyou

# --- 3) 何も持っていなければ何も起きない ----------------------
execute if score #gokei sagyou matches 0 run tellraw @s [{"text":"[売却所] ","color":"green"},{"text":"売れる貴金属を持っていません (鉄・ラピス・金・ダイヤ・ネザライト)","color":"red"}]
execute if score #gokei sagyou matches 0 run playsound minecraft:entity.villager.no player @s
execute if score #gokei sagyou matches 0 run return 0

# --- 4) 先に金を足す ------------------------------------------
scoreboard players operation @s kane_kojin += #gokei sagyou

# --- 5) 金が入ってから、最後に実物を消す ----------------------
# 個数を省くと全部消す
clear @s minecraft:iron_ingot
clear @s minecraft:lapis_lazuli
clear @s minecraft:gold_ingot
clear @s minecraft:diamond
clear @s minecraft:netherite_ingot

# --- 6) 何を何個、いくらになったかを本人に伝える --------------
tellraw @s [{"text":"[売却所] ","color":"green"},{"text":"鉄 ","color":"white"},{"score":{"name":"#tetsu","objective":"sagyou"}},{"text":" / ラピス ","color":"blue"},{"score":{"name":"#lapis","objective":"sagyou"}},{"text":" / 金 ","color":"gold"},{"score":{"name":"#kin","objective":"sagyou"}},{"text":" / ダイヤ ","color":"aqua"},{"score":{"name":"#dia","objective":"sagyou"}},{"text":" / ネザライト ","color":"dark_red"},{"score":{"name":"#neza","objective":"sagyou"}}]
tellraw @s [{"text":"        → 個人の金 +","color":"yellow"},{"score":{"name":"#gokei","objective":"sagyou"},"color":"yellow"},{"text":" (所持 ","color":"gray"},{"score":{"name":"@s","objective":"kane_kojin"},"color":"gray"},{"text":")","color":"gray"}]

playsound minecraft:block.note_block.bell player @s
