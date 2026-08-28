# =============================================================
# jidai:mise/kau_kojin ── 個人の金で 鉄の剣 を買う (値段3)
#   買う本人として走る。呼ばれ方が2通りある。
#     ボタン式  : 石のボタンを押した → アドバンスメントから直接
#     チェスト式: 棚から取った → jidai:chest_kojin から
# =============================================================

# ボタン式で次も押せるように、達成を取り消す。
# (チェスト式では達成していないので、取り消しても何も起きない)
# ★ ここに `advancement revoke @s only jidai:oshita_kojin` があったが、
#   1.20.1 に minecraft:default_block_use の起動条件が無い(実測)ため、
#   そのアドバンスメント自体を消した。存在しないものを revoke すると
#   【黙って失敗するだけ】で気づけないので、行ごと外してある。

# 値段はここ1行。変えたい時はこの数字だけ直す
scoreboard players set #nedan sagyou 3

# --- 呼ばれ方の判定 -------------------------------------------
# jidai_chest_kai の印はチェスト式のときだけ付いている。
# ボタン式のときは、押した石ボタンが本当に販売所のものか確認する。
# (どこかに自分で石ボタンを置いて押しても買えてしまうのを防ぐ)
execute unless entity @s[tag=jidai_chest_kai] unless entity @e[type=marker,tag=jidai_mise_kojin,distance=..5] run return 0

# --- 払えない場合 ---------------------------------------------
execute unless score @s kane_kojin >= #nedan sagyou run tellraw @s [{"text":"[販売所] ","color":"gold"},{"text":"個人の金が足りません (必要 ","color":"red"},{"score":{"name":"#nedan","objective":"sagyou"}},{"text":" / 所持 ","color":"red"},{"score":{"name":"@s","objective":"kane_kojin"}},{"text":")","color":"red"}]
execute unless score @s kane_kojin >= #nedan sagyou run playsound minecraft:entity.villager.no player @s
# チェスト式なら、棚から取られた商品を取り上げて返す
execute unless score @s kane_kojin >= #nedan sagyou if entity @s[tag=jidai_chest_kai] run clear @s minecraft:iron_sword 1
execute unless score @s kane_kojin >= #nedan sagyou run tag @s remove jidai_chest_kai
execute unless score @s kane_kojin >= #nedan sagyou run return 0

# --- 購入成立 -------------------------------------------------
# 通知に「いくらから いくらへ」を出すため、購入前の残高を控えておく
scoreboard players operation #mae sagyou = @s kane_kojin
scoreboard players operation @s kane_kojin -= #nedan sagyou

# ボタン式なら商品を渡す。チェスト式は本人がもう棚から取っている
execute unless entity @s[tag=jidai_chest_kai] run give @s minecraft:iron_sword

# ★ サイドバーへ個人の金を書く行はやめた (2026-08-20)。
#   個人の金はアクションバーへ移した(jidai:clock が毎秒書き直す)。

tellraw @a [{"text":"[販売所] ","color":"gold"},{"selector":"@s"},{"text":" が 個人の金 で 鉄の剣 を購入 (自分の残高 ","color":"white"},{"score":{"name":"#mae","objective":"sagyou"},"color":"gray"},{"text":" → ","color":"gray"},{"score":{"name":"@s","objective":"kane_kojin"},"color":"gray"},{"text":")","color":"gray"}]

playsound minecraft:entity.player.levelup player @s
tag @s remove jidai_chest_kai
