# =============================================================
# jidai:shinko/hantei_kyuryo ── 丘陵の3条件を調べる
#   勢力名と拠点座標だけを置いて、共通の判定へ渡す。
#   勢力を増やす時は、このファイルを複製して2箇所だけ書き換える。
# =============================================================

# 判定対象の勢力の値を作業用へ写す
scoreboard players operation #sekiyu sagyou = 丘陵 sekiyu_gokei
scoreboard players operation #chokin sagyou = 丘陵 chokin
scoreboard players operation #jidai sagyou = 丘陵 jidai

# 拠点1(丘陵)の中心。1500版の座標
# 建築を数える区画は、この中心から 12ブロック四方 / 高さ 101〜112
execute positioned 0 101 -410 run function jidai:shinko/kenchiku

# 共通の判定へ
function jidai:shinko/hantei

# 結果を丘陵へ書き戻す
scoreboard players operation 丘陵 kenchiku = #kenchiku sagyou
scoreboard players operation 丘陵 jouken_a = #a sagyou
scoreboard players operation 丘陵 jouken_b = #b sagyou
scoreboard players operation 丘陵 jouken_c = #c sagyou

# 3つ揃ったら運営へ知らせる (毎回出ると煩いので、初めて揃った時だけ)
execute if score #zenbu sagyou matches 1 unless score 丘陵 sagyou matches 1 run tellraw @a[tag=jidai_unei] [{"text":"[進行] ","color":"gold","bold":true},{"text":"丘陵 が3条件を満たした。承認するなら ","color":"white"},{"text":"/function jidai:shinko/shounin_kyuryo","color":"aqua"}]
scoreboard players operation 丘陵 sagyou = #zenbu sagyou
