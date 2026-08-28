# =============================================================
# jidai:kane/kinko_yomu ── 所属勢力の貯金を読み出す
#   実行者(@s)の所属チームを見て、その勢力の貯金を
#   作業用の #kinko へ写す。どの勢力でも同じ書き方で扱えるようにする。
# =============================================================

# 勢力に入っていない人が呼んだ時に前回の値が残らないよう、先に 0 にする
scoreboard players set #kinko sagyou 0

#   買い物できてしまっていた。
# 勢力を増やす時は、ここに1行足す
execute if entity @s[team=kyuryo] run scoreboard players operation #kinko sagyou = 丘陵 chokin
execute if entity @s[team=shinrin] run scoreboard players operation #kinko sagyou = 森林 chokin
execute if entity @s[team=kawa] run scoreboard players operation #kinko sagyou = 川 chokin
execute if entity @s[team=naikai] run scoreboard players operation #kinko sagyou = 内海 chokin
execute if entity @s[team=iwaba] run scoreboard players operation #kinko sagyou = 岩場 chokin
