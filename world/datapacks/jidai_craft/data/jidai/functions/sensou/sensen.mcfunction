# =============================================================
# jidai:sensou/sensen ── 宣戦する
#   プラグインが呼ぶ(販売所GUI → 戦争宣誓 → 相手を選ぶ)。運営が手で打ってもよい。
#
#   ★★ 呼ぶ前に、この3つのスコアを置くこと ★★
#     #w_kuni sagyou … 宣戦する側の勢力の番号 (1〜5)
#     #w_aite sagyou … 宣戦される側の番号
#     #w_soku sagyou … 0=ふつう(準備5分から) / 1=即交戦(下剋上のときだけ)
#
#     例)
#       scoreboard players set #w_kuni sagyou 1
#       scoreboard players set #w_aite sagyou 2
#       scoreboard players set #w_soku sagyou 0
#       function jidai:sensou/sensen
#
#   ★ 1.21 ではマクロで {kuni:"丘陵",aite:"森林",soku:0} と渡していたが、
#     1.20.1 にマクロは無い(実測)。スコアで渡す形に変えた。
# =============================================================
# ★★ 戦争1件 = マーカー1体 ★★
#   状態(進み具合・残り秒・略奪の累計)は、そのマーカー自身のスコアに持つ。
#   保持者名を "丘陵>森林" のように組み立てる必要が無くなるので、
#   マクロが要らず、勢力が何個に増えても同じコードで動く。
#
#   マーカーが持つスコア:
#     sensou            0=なし 1=準備 2=交戦 3=再戦禁止
#     sensou_byou       今の状態の残り秒
#     sensou_bai        略奪の上限の倍率 (1 / 下剋上は 2)
#     w_kuni / w_aite   関わっている2つの勢力の番号
#     ryakudatsu_kane_a / _b    a側(=w_kuni側)とb側が奪った金の累計
#     ryakudatsu_sekiyu_a / _b  同じく石油
#
#   マーカーのタグ:
#     jidai_sensou … 戦争であることの印
#     w_1 〜 w_5    … 関わっている勢力の番号。片方だけを引く時に使う

# --- 0) 打ち間違いを弾く --------------------------------------
execute unless score #w_kuni sagyou matches 1..5 run return 0
execute unless score #w_aite sagyou matches 1..5 run return 0
execute if score #w_kuni sagyou = #w_aite sagyou run return 0

# --- 1) すでに関係があるなら断る ------------------------------
# この2勢力の間に戦争(準備・交戦・再戦禁止のどれか)が残っていれば、
# 新しく宣戦できない。
# ★下剋上(soku=1)だけは再戦禁止を無視して開戦できる。
#   占領から抜ける唯一の手段なので、ここで止めると詰んでしまう。
scoreboard players set #aru sagyou 0
execute as @e[type=marker,tag=jidai_sensou] if score @s w_kuni = #w_kuni sagyou if score @s w_aite = #w_aite sagyou run scoreboard players set #aru sagyou 1
execute as @e[type=marker,tag=jidai_sensou] if score @s w_kuni = #w_aite sagyou if score @s w_aite = #w_kuni sagyou run scoreboard players set #aru sagyou 1
execute if score #aru sagyou matches 1 if score #w_soku sagyou matches 0 run return 0

# 下剋上の時は、古い関係を消してから始める
execute as @e[type=marker,tag=jidai_sensou] if score @s w_kuni = #w_kuni sagyou if score @s w_aite = #w_aite sagyou run kill @s
execute as @e[type=marker,tag=jidai_sensou] if score @s w_kuni = #w_aite sagyou if score @s w_aite = #w_kuni sagyou run kill @s

# --- 2) 始まりの状態を決める ----------------------------------
# ふつうは【準備】から。下剋上は準備をとばして【交戦】から始まり、
# 略奪の上限が2倍になる。
scoreboard players set #jotai sagyou 1
scoreboard players operation #byou sagyou = 戦争_準備秒 settei
scoreboard players set #bai sagyou 1
execute if score #w_soku sagyou matches 1 run scoreboard players set #jotai sagyou 2
execute if score #w_soku sagyou matches 1 run scoreboard players operation #byou sagyou = 戦争_交戦秒 settei
execute if score #w_soku sagyou matches 1 run scoreboard players set #bai sagyou 2

# --- 3) 戦争のマーカーを1体置く -------------------------------
# ★ 置く場所は中央の上空。forceload の中なので必ず読み込まれている。
# ★ 目印の新入りタグを付けておき、次の行で「今置いた1体」を確実に掴む。
summon minecraft:marker 0 200 0 {Tags:["jidai_sensou","w_new"]}

execute as @e[type=marker,tag=w_new] run scoreboard players operation @s w_kuni = #w_kuni sagyou
execute as @e[type=marker,tag=w_new] run scoreboard players operation @s w_aite = #w_aite sagyou
execute as @e[type=marker,tag=w_new] run scoreboard players operation @s sensou = #jotai sagyou
execute as @e[type=marker,tag=w_new] run scoreboard players operation @s sensou_byou = #byou sagyou
execute as @e[type=marker,tag=w_new] run scoreboard players operation @s sensou_bai = #bai sagyou
# 略奪の累計は【1戦争ごと】。始める時に必ず0にする。
execute as @e[type=marker,tag=w_new] run scoreboard players set @s ryakudatsu_kane_a 0
execute as @e[type=marker,tag=w_new] run scoreboard players set @s ryakudatsu_kane_b 0
execute as @e[type=marker,tag=w_new] run scoreboard players set @s ryakudatsu_sekiyu_a 0
execute as @e[type=marker,tag=w_new] run scoreboard players set @s ryakudatsu_sekiyu_b 0

# どの勢力が関わっているかをタグでも持たせる(片側だけを引く時に使う)
# ★ 勢力を増やす時は、ここに2行足す
execute as @e[type=marker,tag=w_new] if score @s w_kuni matches 1 run tag @s add w_1
execute as @e[type=marker,tag=w_new] if score @s w_aite matches 1 run tag @s add w_1
execute as @e[type=marker,tag=w_new] if score @s w_kuni matches 2 run tag @s add w_2
execute as @e[type=marker,tag=w_new] if score @s w_aite matches 2 run tag @s add w_2
execute as @e[type=marker,tag=w_new] if score @s w_kuni matches 3 run tag @s add w_3
execute as @e[type=marker,tag=w_new] if score @s w_aite matches 3 run tag @s add w_3
execute as @e[type=marker,tag=w_new] if score @s w_kuni matches 4 run tag @s add w_4
execute as @e[type=marker,tag=w_new] if score @s w_aite matches 4 run tag @s add w_4
execute as @e[type=marker,tag=w_new] if score @s w_kuni matches 5 run tag @s add w_5
execute as @e[type=marker,tag=w_new] if score @s w_aite matches 5 run tag @s add w_5

tag @e[type=marker,tag=w_new] remove w_new

# --- 4) 全体へ知らせる ----------------------------------------
scoreboard players operation #mei_no sagyou = #w_kuni sagyou
scoreboard players operation #mei_no2 sagyou = #w_aite sagyou
function jidai:sensou/mei
function jidai:sensou/mei2
execute if score #w_soku sagyou matches 0 run tellraw @a [{"text":"[宣戦] ","color":"red","bold":true},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" が ","color":"white"},{"storage":"jidai:kari","nbt":"mei2","color":"white"},{"text":" に宣戦した。5分後に交戦が始まる","color":"white"}]
execute if score #w_soku sagyou matches 1 run tellraw @a [{"text":"[下剋上] ","color":"light_purple","bold":true},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" が ","color":"white"},{"storage":"jidai:kari","nbt":"mei2","color":"white"},{"text":" の占領を跳ね返した。ただちに交戦、略奪の上限は2倍","color":"white"}]
execute as @a run playsound minecraft:entity.wither.spawn player @s
