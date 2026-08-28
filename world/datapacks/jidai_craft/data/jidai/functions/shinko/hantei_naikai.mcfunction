# =============================================================
# jidai:shinko/hantei_naikai ── 内海の3条件を調べる
#   勢力名と拠点座標だけを置いて、共通の判定へ渡す。
#   勢力を増やす時は、このファイルを複製して2箇所だけ書き換える。
# =============================================================

# 判定対象の勢力の値を作業用へ写す
scoreboard players operation #sekiyu sagyou = 内海 sekiyu_gokei
scoreboard players operation #chokin sagyou = 内海 chokin
scoreboard players operation #jidai sagyou = 内海 jidai

# 拠点4(内海)の中心。1500版の座標
# 建築を数える区画は、この中心から 12ブロック四方 / 高さ 101〜112
execute positioned -241 101 332 run function jidai:shinko/kenchiku

# 共通の判定へ
function jidai:shinko/hantei

# 結果を内海へ書き戻す
scoreboard players operation 内海 kenchiku = #kenchiku sagyou
scoreboard players operation 内海 jouken_a = #a sagyou
scoreboard players operation 内海 jouken_b = #b sagyou
scoreboard players operation 内海 jouken_c = #c sagyou

# 3つ揃ったら運営へ知らせる (毎回出ると煩いので、初めて揃った時だけ)
execute if score #zenbu sagyou matches 1 unless score 内海 sagyou matches 1 run tellraw @a[tag=jidai_unei] [{"text":"[進行] ","color":"gold","bold":true},{"text":"内海 が3条件を満たした。承認するなら ","color":"white"},{"text":"/function jidai:shinko/shounin_naikai","color":"aqua"}]
scoreboard players operation 内海 sagyou = #zenbu sagyou
