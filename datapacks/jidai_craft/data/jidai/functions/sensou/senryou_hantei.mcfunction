# =============================================================
# jidai:sensou/senryou_hantei ── 交戦終了時に占領が起きるかを決める
#   jidai:sensou/susumu から、戦争のマーカーとして呼ばれる(@s = マーカー)。
#     入力  #ryaku_a / #ryaku_b … a側/b側が奪った量の合計
#          #mei_no / #mei_no2  … a側/b側の番号(名前は storage に入っている)
# =============================================================
# ★ 勢力の貯金は「勢力名」が保持者なので、番号からは直接読めない。
#   番号で場合分けして作業用へ写す。
#   ★ 勢力を増やす時は、下の4つのかたまりに1行ずつ足す。

# --- a側の貯金を読む ------------------------------------------
scoreboard players set #chokin_a sagyou 0
execute if score @s w_kuni matches 1 run scoreboard players operation #chokin_a sagyou = 丘陵 chokin
execute if score @s w_kuni matches 2 run scoreboard players operation #chokin_a sagyou = 森林 chokin
execute if score @s w_kuni matches 3 run scoreboard players operation #chokin_a sagyou = 川 chokin
execute if score @s w_kuni matches 4 run scoreboard players operation #chokin_a sagyou = 内海 chokin
execute if score @s w_kuni matches 5 run scoreboard players operation #chokin_a sagyou = 岩場 chokin

# --- b側の貯金を読む ------------------------------------------
scoreboard players set #chokin_b sagyou 0
execute if score @s w_aite matches 1 run scoreboard players operation #chokin_b sagyou = 丘陵 chokin
execute if score @s w_aite matches 2 run scoreboard players operation #chokin_b sagyou = 森林 chokin
execute if score @s w_aite matches 3 run scoreboard players operation #chokin_b sagyou = 川 chokin
execute if score @s w_aite matches 4 run scoreboard players operation #chokin_b sagyou = 内海 chokin
execute if score @s w_aite matches 5 run scoreboard players operation #chokin_b sagyou = 岩場 chokin

# --- a側が占領されるか（貯金0 かつ b側が略奪していた）---------
scoreboard players set #seme sagyou 0
execute if score #chokin_a sagyou matches ..0 if score #ryaku_b sagyou matches 1.. run scoreboard players set #seme sagyou 1
scoreboard players operation #senryou_no sagyou = @s w_aite
execute if score #seme sagyou matches 1 if score @s w_kuni matches 1 run scoreboard players operation 丘陵 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_kuni matches 2 run scoreboard players operation 森林 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_kuni matches 3 run scoreboard players operation 川 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_kuni matches 4 run scoreboard players operation 内海 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_kuni matches 5 run scoreboard players operation 岩場 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 run tellraw @a [{"text":"[占領] ","color":"dark_red","bold":true},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" は貯金が尽きた。","color":"white"},{"storage":"jidai:kari","nbt":"mei2","color":"white"},{"text":" に占領された","color":"white"}]
execute if score #chokin_a sagyou matches ..0 if score #ryaku_b sagyou matches ..0 run tellraw @a [{"text":"[占領なし] ","color":"gray"},{"storage":"jidai:kari","nbt":"mei","color":"gray"},{"text":" は貯金が尽きていたが、","color":"gray"},{"storage":"jidai:kari","nbt":"mei2","color":"gray"},{"text":" は一度も略奪していないので占領されない","color":"gray"}]

# --- b側が占領されるか（貯金0 かつ a側が略奪していた）---------
scoreboard players set #seme sagyou 0
execute if score #chokin_b sagyou matches ..0 if score #ryaku_a sagyou matches 1.. run scoreboard players set #seme sagyou 1
scoreboard players operation #senryou_no sagyou = @s w_kuni
execute if score #seme sagyou matches 1 if score @s w_aite matches 1 run scoreboard players operation 丘陵 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_aite matches 2 run scoreboard players operation 森林 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_aite matches 3 run scoreboard players operation 川 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_aite matches 4 run scoreboard players operation 内海 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 if score @s w_aite matches 5 run scoreboard players operation 岩場 senryou = #senryou_no sagyou
execute if score #seme sagyou matches 1 run tellraw @a [{"text":"[占領] ","color":"dark_red","bold":true},{"storage":"jidai:kari","nbt":"mei2","color":"white"},{"text":" は貯金が尽きた。","color":"white"},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" に占領された","color":"white"}]
execute if score #chokin_b sagyou matches ..0 if score #ryaku_a sagyou matches ..0 run tellraw @a [{"text":"[占領なし] ","color":"gray"},{"storage":"jidai:kari","nbt":"mei2","color":"gray"},{"text":" は貯金が尽きていたが、","color":"gray"},{"storage":"jidai:kari","nbt":"mei","color":"gray"},{"text":" は一度も略奪していないので占領されない","color":"gray"}]
