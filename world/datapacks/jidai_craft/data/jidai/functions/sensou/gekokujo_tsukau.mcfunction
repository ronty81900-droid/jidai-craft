# =============================================================
# jidai:sensou/gekokujo_tsukau ── 下剋上を行使する
#   リーダーが打つ(プラグインのGUIから呼ばれる)。
#     条件 … 自勢力が占領されている / 権利を持っている / 本人がリーダー
#     効果 … 占領が解ける + 占領していた勢力へ即時開戦
#            (準備5分なし・略奪の上限2倍) + 権利を消費する
# =============================================================
# ★占領は時間では解けない。これが唯一の解除手段。
#   だから再戦禁止(30分)も無視して開戦できるようにしてある
#   (jidai:sensou/sensen の soku=1)。ここで止めると詰んでしまう。
#
# ★リーダーは jidai_leader タグで見分ける。付けるのはプラグイン。

# --- リーダーだけが行使できる ---------------------------------
execute unless entity @s[tag=jidai_leader] run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"行使できるのは勢力のリーダーだけです","color":"red"}]
execute unless entity @s[tag=jidai_leader] run return 0

function jidai:sensou/kuni_yomu
execute if data storage jidai:kari {kuni:""} run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"勢力に入っていません","color":"red"}]
execute if data storage jidai:kari {kuni:""} run return 0

# --- 権利と占領の状態を読む -----------------------------------
# 勢力を増やす時は、ここに2行足す
scoreboard players set #geko sagyou 0
scoreboard players set #senryou sagyou 0
execute if entity @s[team=kyuryo] run scoreboard players operation #geko sagyou = 丘陵 gekokujo
execute if entity @s[team=kyuryo] run scoreboard players operation #senryou sagyou = 丘陵 senryou
execute if entity @s[team=shinrin] run scoreboard players operation #geko sagyou = 森林 gekokujo
execute if entity @s[team=shinrin] run scoreboard players operation #senryou sagyou = 森林 senryou
execute if entity @s[team=kawa] run scoreboard players operation #geko sagyou = 川 gekokujo
execute if entity @s[team=kawa] run scoreboard players operation #senryou sagyou = 川 senryou
execute if entity @s[team=naikai] run scoreboard players operation #geko sagyou = 内海 gekokujo
execute if entity @s[team=naikai] run scoreboard players operation #senryou sagyou = 内海 senryou
execute if entity @s[team=iwaba] run scoreboard players operation #geko sagyou = 岩場 gekokujo
execute if entity @s[team=iwaba] run scoreboard players operation #senryou sagyou = 岩場 senryou

execute if score #geko sagyou matches ..0 run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"権利を持っていません (販売所で買えます)","color":"red"}]
execute if score #geko sagyou matches ..0 run return 0

execute if score #senryou sagyou matches ..0 run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"占領されていません。使えるのは占領されている時だけです","color":"red"}]
execute if score #senryou sagyou matches ..0 run return 0

# --- 占領している勢力の名前を引く(番号 → 名前) ----------------
# senryou には占領している勢力の番号が入っている。
# 勢力を増やす時は、ここに1行足す
data modify storage jidai:kari aite set value ""
execute if score #senryou sagyou matches 1 run data modify storage jidai:kari aite set value "丘陵"
execute if score #senryou sagyou matches 2 run data modify storage jidai:kari aite set value "森林"
execute if score #senryou sagyou matches 3 run data modify storage jidai:kari aite set value "川"
execute if score #senryou sagyou matches 4 run data modify storage jidai:kari aite set value "内海"
execute if score #senryou sagyou matches 5 run data modify storage jidai:kari aite set value "岩場"

# --- 権利を消費し、占領を解く ---------------------------------
# 勢力を増やす時は、ここに2行足す
execute if entity @s[team=kyuryo] run scoreboard players set 丘陵 gekokujo 0
execute if entity @s[team=kyuryo] run scoreboard players set 丘陵 senryou 0
execute if entity @s[team=shinrin] run scoreboard players set 森林 gekokujo 0
execute if entity @s[team=shinrin] run scoreboard players set 森林 senryou 0
execute if entity @s[team=kawa] run scoreboard players set 川 gekokujo 0
execute if entity @s[team=kawa] run scoreboard players set 川 senryou 0
execute if entity @s[team=naikai] run scoreboard players set 内海 gekokujo 0
execute if entity @s[team=naikai] run scoreboard players set 内海 senryou 0
execute if entity @s[team=iwaba] run scoreboard players set 岩場 gekokujo 0
execute if entity @s[team=iwaba] run scoreboard players set 岩場 senryou 0

# --- 占領していた勢力へ即時開戦 -------------------------------
# soku=1 = 準備をとばして交戦から始め、略奪の上限を2倍にする。
# 開戦の通知は jidai:sensou/sensen が出す。
# ★ 1.20.1 にマクロが無いので、スコアで渡す。
#   #senryou sagyou には占領している勢力の番号が入っている。
scoreboard players operation #w_kuni sagyou = @s bangou
scoreboard players operation #w_aite sagyou = #senryou sagyou
scoreboard players set #w_soku sagyou 1
function jidai:sensou/sensen
