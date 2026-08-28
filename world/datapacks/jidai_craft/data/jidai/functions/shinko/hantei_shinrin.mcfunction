# =============================================================
# jidai:shinko/hantei_shinrin ── 森林の3条件を調べる
#   勢力名と拠点座標だけを置いて、共通の判定へ渡す。
#   勢力を増やす時は、このファイルを複製して2箇所だけ書き換える。
# =============================================================

# 判定対象の勢力の値を作業用へ写す
scoreboard players operation #sekiyu sagyou = 森林 sekiyu_gokei
scoreboard players operation #chokin sagyou = 森林 chokin
scoreboard players operation #jidai sagyou = 森林 jidai

# 拠点2(森林)の中心。1500版の座標
# 建築を数える区画は、この中心から 12ブロック四方 / 高さ 101〜112
execute positioned 390 101 -127 run function jidai:shinko/kenchiku

# 共通の判定へ
function jidai:shinko/hantei

# 結果を森林へ書き戻す
scoreboard players operation 森林 kenchiku = #kenchiku sagyou
scoreboard players operation 森林 jouken_a = #a sagyou
scoreboard players operation 森林 jouken_b = #b sagyou
scoreboard players operation 森林 jouken_c = #c sagyou

# 3つ揃ったら運営へ知らせる (毎回出ると煩いので、初めて揃った時だけ)
execute if score #zenbu sagyou matches 1 unless score 森林 sagyou matches 1 run tellraw @a[tag=jidai_unei] [{"text":"[進行] ","color":"gold","bold":true},{"text":"森林 が3条件を満たした。承認するなら ","color":"white"},{"text":"/function jidai:shinko/shounin_shinrin","color":"aqua"}]
scoreboard players operation 森林 sagyou = #zenbu sagyou
