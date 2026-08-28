# =============================================================
# jidai:mise/kau_seiryoku ── 勢力の金で 鉄のツルハシ を買う (値段30)
#   買う本人として走る。ボタン式(黒石のボタン)とチェスト式の両方から。
#   勢力の金は全員が使えるので、必ず全体へ通知する。
# =============================================================

# ★ ここに `advancement revoke @s only jidai:oshita_seiryoku` があったが、
#   1.20.1 に minecraft:default_block_use の起動条件が無い(実測)ため、
#   そのアドバンスメント自体を消した。存在しないものを revoke すると
#   【黙って失敗するだけ】で気づけないので、行ごと外してある。

# 値段はここ1行
scoreboard players set #nedan sagyou 30

# ボタン式のときは、押した黒石ボタンが本当に販売所のものか確認する
execute unless entity @s[tag=jidai_chest_kai] unless entity @e[type=marker,tag=jidai_mise_seiryoku,distance=..5] run return 0

#   買い物できてしまっていた(個人の金の販売所は今までどおり使える)。

# --- 勢力に入っていない人は勢力の金を使えない ------------------
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] run tellraw @s [{"text":"[販売所] ","color":"gold"},{"text":"勢力に入っていません。先に /team join kyuryo @s","color":"red"}]
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] if entity @s[tag=jidai_chest_kai] run clear @s minecraft:iron_pickaxe 1
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] run tag @s remove jidai_chest_kai
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] run return 0

# 所属勢力の金庫の残高を #kinko へ読み出す
function jidai:kane/kinko_yomu

# --- 金庫が足りない場合 ---------------------------------------
execute unless score #kinko sagyou >= #nedan sagyou run tellraw @s [{"text":"[販売所] ","color":"gold"},{"text":"勢力の金が足りません (必要 ","color":"red"},{"score":{"name":"#nedan","objective":"sagyou"}},{"text":" / 金庫 ","color":"red"},{"score":{"name":"#kinko","objective":"sagyou"}},{"text":")","color":"red"}]
execute unless score #kinko sagyou >= #nedan sagyou run playsound minecraft:entity.villager.no player @s
execute unless score #kinko sagyou >= #nedan sagyou if entity @s[tag=jidai_chest_kai] run clear @s minecraft:iron_pickaxe 1
execute unless score #kinko sagyou >= #nedan sagyou run tag @s remove jidai_chest_kai
execute unless score #kinko sagyou >= #nedan sagyou run return 0

# --- 購入成立 -------------------------------------------------
scoreboard players operation #mae sagyou = #kinko sagyou
scoreboard players operation #kinko sagyou -= #nedan sagyou

# 減らした値を金庫へ書き戻す。これを忘れると金が減らない
function jidai:kane/kinko_kaku

# ボタン式なら商品を渡す。チェスト式は本人がもう棚から取っている
execute unless entity @s[tag=jidai_chest_kai] run give @s minecraft:iron_pickaxe

# --- 全体通知(必須) -------------------------------------------
# 勢力の金は全員が使えるので、記録が無いと「金庫が減った事実」だけが残る。
# 誰が独断で使ったのかを分かるようにしておく。追及は人間がやる。
tellraw @a [{"text":"[販売所] ","color":"gold"},{"selector":"@s"},{"text":" が 勢力の金 で 鉄のツルハシ を購入 (勢力残高 ","color":"white"},{"score":{"name":"#mae","objective":"sagyou"},"color":"gray"},{"text":" → ","color":"gray"},{"score":{"name":"#kinko","objective":"sagyou"},"color":"gray"},{"text":")","color":"gray"}]

playsound minecraft:entity.player.levelup player @s
tag @s remove jidai_chest_kai
