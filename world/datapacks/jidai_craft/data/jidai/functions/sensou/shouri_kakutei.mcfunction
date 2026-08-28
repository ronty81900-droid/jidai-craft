# =============================================================
# jidai:sensou/shouri_kakutei ── 戦争勝利が確定した
#   入力  #s_kuni    sagyou … 勝った勢力の番号
#         #shouri_shu sagyou … 勝ち方
#                                1=戦争 / 2=人望 / 3=経済 / 4=超特殊
#                                5=未来到達（2026-08-23 に足した）
#
#   ★ ここでは【知らせるだけ】で、ゲームは止めない。
#     止めるかどうかは運営が決めることで、
#     データパックが勝手にサーバーを終わらせるべきではない。
#     終わりの合図を派手に出し、あとは人が締める。
#
#   ★ 勝った印は shouri（保持者＝世界 / 値＝勝った勢力の番号）。
#     もう1回 起きても二重に祝わないよう、ここで見張る。
# =============================================================

# 既に誰かが勝っていれば、二度目は出さない
execute unless score 世界 shouri matches 0 run return 0
scoreboard players operation 世界 shouri = #s_kuni sagyou

scoreboard players operation #mei_no sagyou = #s_kuni sagyou
function jidai:sensou/mei

# ★★ 2026-08-26 のご指示: タイトルで大きく出す ★★
#   〇〇勝利（大きい字） / 勢力[丘陵]（小さい字）。
#   ★ 順番が要る。subtitle は【先に置いてから】title を出さないと、
#     その回の表示に乗らない（title が表示の合図を兼ねているため）。
#   ★ 出しっぱなしにしない。fadeIn 10 / stay 100(5秒) / fadeOut 20。
title @a times 10 100 20
title @a subtitle [{"text":"勢力[","color":"white"},{"nbt":"mei","storage":"jidai:kari","color":"yellow","bold":true},{"text":"]","color":"white"}]
execute if score #shouri_shu sagyou matches 1 run title @a title {"text":"戦争勝利","color":"gold","bold":true}
execute if score #shouri_shu sagyou matches 3 run title @a title {"text":"経済勝利","color":"gold","bold":true}
execute if score #shouri_shu sagyou matches 4 run title @a title {"text":"超特殊勝利","color":"light_purple","bold":true}
execute if score #shouri_shu sagyou matches 5 run title @a title {"text":"未来到達勝利","color":"aqua","bold":true}

tellraw @a [{"text":"","color":"white"}]
tellraw @a [{"text":"███████████████████████████","color":"gold"}]
execute if score #shouri_shu sagyou matches 1 run tellraw @a [{"text":"   ★ 戦争勝利 ★","color":"gold","bold":true}]
execute if score #shouri_shu sagyou matches 3 run tellraw @a [{"text":"   ★ 経済勝利 ★","color":"gold","bold":true}]
execute if score #shouri_shu sagyou matches 4 run tellraw @a [{"text":"   ★ 超特殊勝利 ★","color":"light_purple","bold":true}]
execute if score #shouri_shu sagyou matches 5 run tellraw @a [{"text":"   ★ 未来到達勝利 ★","color":"aqua","bold":true}]
execute if score #shouri_shu sagyou matches 1 run tellraw @a [{"text":"   ","color":"white"},{"nbt":"mei","storage":"jidai:kari","color":"yellow","bold":true},{"text":" が すべての勢力を植民地にした","color":"white"}]
execute if score #shouri_shu sagyou matches 3 run tellraw @a [{"text":"   ","color":"white"},{"nbt":"mei","storage":"jidai:kari","color":"yellow","bold":true},{"text":" の貯金が ","color":"white"},{"score":{"name":"経済_勝利_貯金","objective":"settei"},"color":"yellow"},{"text":" に達した","color":"white"}]
execute if score #shouri_shu sagyou matches 4 run tellraw @a [{"text":"   ","color":"white"},{"nbt":"mei","storage":"jidai:kari","color":"yellow","bold":true},{"text":" のリーダーが 特殊アイテムを5種すべて集めた","color":"white"}]
execute if score #shouri_shu sagyou matches 5 run tellraw @a [{"text":"   ","color":"white"},{"nbt":"mei","storage":"jidai:kari","color":"yellow","bold":true},{"text":" が 現代から【未来】へ進んだ","color":"white"}]
tellraw @a [{"text":"███████████████████████████","color":"gold"}]
tellraw @a [{"text":"","color":"white"}]

# ★★ 2026-08-26 のご指示: 勝利の音は【かなとこ】★★
#   3回 鳴らして音程を上げ、「カン・カン・カーン」と打ち終える形にする。
#   ★ 同じ tick に重ねるので、ずれずに和音のように聞こえる。
#   ★ master で鳴らす。player の音量を下げている人にも必ず届かせるため
#     （勝利は聞き逃してはいけない合図）。
execute as @a run playsound minecraft:block.anvil.land master @s ~ ~ ~ 1 0.8
execute as @a run playsound minecraft:block.anvil.land master @s ~ ~ ~ 1 1.2
execute as @a run playsound minecraft:block.anvil.use master @s ~ ~ ~ 1 1.6
# 余韻。かなとこの下に敷く（これが無いと打撃音だけで終わって軽い）
execute as @a run playsound minecraft:entity.ender_dragon.death player @s ~ ~ ~ 1 1

# 全員の頭上に花火。★ 勝った勢力だけでなく全員に上げる（終わりの合図）
execute as @a at @s run summon minecraft:firework_rocket ~ ~1 ~ {LifeTime:20,FireworksItem:{id:"minecraft:firework_rocket",Count:1b,tag:{Fireworks:{Flight:1b,Explosions:[{Type:1b,Colors:[I;16766720],FadeColors:[I;16777215]}]}}}}
