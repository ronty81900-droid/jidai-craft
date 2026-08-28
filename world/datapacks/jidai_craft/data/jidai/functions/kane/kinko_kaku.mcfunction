# =============================================================
# jidai:kane/kinko_kaku ── 所属勢力の貯金へ書き戻す
#   作業用 #kinko の値を、実行者(@s)の所属勢力の貯金へ写す。
#   jidai:kane/kinko_yomu と対になっている。
# =============================================================

#   読む側と書く側で、必ず同じ条件にすること。
# 勢力を増やす時は、ここに1行足す
execute if entity @s[team=kyuryo] run scoreboard players operation 丘陵 chokin = #kinko sagyou
execute if entity @s[team=shinrin] run scoreboard players operation 森林 chokin = #kinko sagyou
execute if entity @s[team=kawa] run scoreboard players operation 川 chokin = #kinko sagyou
execute if entity @s[team=naikai] run scoreboard players operation 内海 chokin = #kinko sagyou
execute if entity @s[team=iwaba] run scoreboard players operation 岩場 chokin = #kinko sagyou
