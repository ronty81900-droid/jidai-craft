# =============================================================
# jidai:shinko/chuo ── 中央(世界)の時代が上がるかを調べる
#   2勢力が次の時代へ到達した時点で、中央の姿が変わる。
#   変わったら石油の湧出をリセットし、持ち越し分から4割を徴収する。
# =============================================================
# 勢力の時代と、中央の姿は別物。
# 視聴者は中央を見れば世界の時代が分かる(数字を出さずに進捗を見せる)。

# 中央の次の時代 = 今の中央 + 1
scoreboard players operation #tsugi sagyou = 世界 chuo
scoreboard players add #tsugi sagyou 1

# 次の時代に到達している勢力を数える
# 勢力を増やす時は、ここに1行足す
scoreboard players set #tassei sagyou 0
execute if score 丘陵 jidai >= #tsugi sagyou run scoreboard players add #tassei sagyou 1
execute if score 森林 jidai >= #tsugi sagyou run scoreboard players add #tassei sagyou 1
execute if score 川 jidai >= #tsugi sagyou run scoreboard players add #tassei sagyou 1
execute if score 内海 jidai >= #tsugi sagyou run scoreboard players add #tassei sagyou 1
execute if score 岩場 jidai >= #tsugi sagyou run scoreboard players add #tassei sagyou 1

# 2勢力に満たなければ何もしない
execute if score #tassei sagyou matches ..1 run return 0
# 時代4が上限
execute if score 世界 chuo matches 4.. run return 0

# --- 中央の時代を上げる ---------------------------------------
scoreboard players add 世界 chuo 1

# --- 石油のリセット -------------------------------------------
# 湧出量の数えを戻して、また湧き始めるようにする
scoreboard players set 世界 wakidashi 0
scoreboard players set #plant_t sagyou 0

# --- 持ち越し分から4割を徴収する ------------------------------
# 時代を強制的に進められた代償。遅れた勢力に希望を残しつつ追いつきすぎない。
# 端数は切り捨て(整数の割り算なので、残る側が得をする)。
# 例: 10本持っていたら 10*40/100 = 4本 徴収して 6本 残る
# 徴収の世代を1つ進める。実際に引くのは jidai:clock が
# 「まだ受けていない人」を見つけた時に行う(ログアウトで逃げられない)。
scoreboard players add 世界 choshu 1

# --- 中央プラントの外観を差し替える ---------------------------
# footprint が全段階で同じで、中にも上にも人が居ないので、
# 退避処理は要らない(詳しくは jidai:shinko/plant の頭)。
function jidai:shinko/plant

tellraw @a [{"text":"[中央] ","color":"red","bold":true},{"text":"世界が次の時代へ移った (中央の時代 ","color":"white"},{"score":{"name":"世界","objective":"chuo"},"color":"yellow"},{"text":")","color":"white"}]
tellraw @a [{"text":"  石油の湧出がリセットされた。持ち越した石油から4割が徴収された","color":"gray"}]
tellraw @a [{"text":"  この時代の装備が全勢力に解禁された","color":"gray"}]
execute as @a run playsound minecraft:entity.wither.spawn player @s
