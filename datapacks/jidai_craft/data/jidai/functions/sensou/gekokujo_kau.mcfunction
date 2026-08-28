# =============================================================
# jidai:sensou/gekokujo_kau ── 下剋上の権利を買う
#   販売所の商品。買う本人として走る(プラグインのGUIから呼ばれる)。
#     値段   … 勢力の金 150
#     解禁   … 中央の時代が近代(3)以上
#     上限   … 1勢力につき1つまで
#   ★占領されていなくても買える。先に買って備えておける。
# =============================================================
# 解禁を【中央の時代】で見るのは、銃の解禁や最低雇用料と揃えるため。
# 勢力の時代で見ると、遅れている勢力ほど買えなくなり、
# 占領から抜け出す手段が届かなくなる。

# --- 勢力に入っていない人は買えない ---------------------------
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"勢力に入っていません","color":"red"}]
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] run return 0

# --- 近代になるまで売っていない -------------------------------
execute unless score 世界 chuo >= 下剋上_解禁時代 settei run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"まだ解禁されていない (中央が近代になると売り出す)","color":"red"}]
execute unless score 世界 chuo >= 下剋上_解禁時代 settei run return 0

# --- すでに1つ持っていたら買えない ----------------------------
# 勢力を増やす時は、ここに1行足す
scoreboard players set #geko sagyou 0
execute if entity @s[team=kyuryo] run scoreboard players operation #geko sagyou = 丘陵 gekokujo
execute if entity @s[team=shinrin] run scoreboard players operation #geko sagyou = 森林 gekokujo
execute if entity @s[team=kawa] run scoreboard players operation #geko sagyou = 川 gekokujo
execute if entity @s[team=naikai] run scoreboard players operation #geko sagyou = 内海 gekokujo
execute if entity @s[team=iwaba] run scoreboard players operation #geko sagyou = 岩場 gekokujo
execute if score #geko sagyou matches 1.. run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"すでに1つ持っています (保有できるのは1つまで)","color":"red"}]
execute if score #geko sagyou matches 1.. run return 0

# --- 勢力の金で払う -------------------------------------------
function jidai:kane/kinko_yomu
execute unless score #kinko sagyou >= 下剋上_値段 settei run tellraw @s [{"text":"[下剋上] ","color":"light_purple"},{"text":"勢力の金が足りません (必要 ","color":"red"},{"score":{"name":"下剋上_値段","objective":"settei"}},{"text":" / 金庫 ","color":"red"},{"score":{"name":"#kinko","objective":"sagyou"}},{"text":")","color":"red"}]
execute unless score #kinko sagyou >= 下剋上_値段 settei run playsound minecraft:entity.villager.no player @s
execute unless score #kinko sagyou >= 下剋上_値段 settei run return 0

scoreboard players operation #mae sagyou = #kinko sagyou
scoreboard players operation #kinko sagyou -= 下剋上_値段 settei
function jidai:kane/kinko_kaku

# --- 権利を渡す -----------------------------------------------
# 勢力を増やす時は、ここに1行足す
execute if entity @s[team=kyuryo] run scoreboard players set 丘陵 gekokujo 1
execute if entity @s[team=shinrin] run scoreboard players set 森林 gekokujo 1
execute if entity @s[team=kawa] run scoreboard players set 川 gekokujo 1
execute if entity @s[team=naikai] run scoreboard players set 内海 gekokujo 1
execute if entity @s[team=iwaba] run scoreboard players set 岩場 gekokujo 1

# --- 全体通知(勢力の金を使ったので必ず出す) -------------------
tellraw @a [{"text":"[下剋上] ","color":"light_purple","bold":true},{"selector":"@s"},{"text":" が 勢力の金 で 下剋上の権利 を買った (勢力残高 ","color":"white"},{"score":{"name":"#mae","objective":"sagyou"},"color":"gray"},{"text":" → ","color":"gray"},{"score":{"name":"#kinko","objective":"sagyou"},"color":"gray"},{"text":")","color":"gray"}]
tellraw @a [{"text":"  行使できるのはリーダーだけ。占領されている時だけ使える","color":"dark_gray"}]
execute as @a run playsound minecraft:block.beacon.activate player @s
