# =============================================================
# jidai:sensou/ubau_hitori ── 略奪ぶんの石油を1人から引く
#   相手勢力で最も多く持っている人として走る。
#   呼び出し元が #hon(引く本数) と #zumi(0) を用意している。
# =============================================================

# もう誰かから引いていたら、この人からは引かない
execute if score #zumi sagyou matches 1.. run return 0
scoreboard players set #zumi sagyou 1

# ★全体通知に名前を出すための印。
#   バニラには「プレイヤー名を storage へ入れる」方法が無いので、
#   印を付けておいて、通知側で @a[tag=...] のセレクタとして出す。
#   呼び出し元(jidai:sensou/ryakudatsu)が通知の直後に外す。
tag @s add jidai_ubawareta

# 呼び出し元で #hon は「その人の所持」以下に収めてあるので、
# マイナスにはならない。
scoreboard players operation @s sekiyu -= #hon sagyou

tellraw @s [{"text":"[略奪] ","color":"dark_red"},{"text":"石油 ","color":"gray"},{"score":{"name":"#hon","objective":"sagyou"},"color":"red"},{"text":" 本を奪われた (残り ","color":"gray"},{"score":{"name":"@s","objective":"sekiyu"},"color":"white"},{"text":" 本)","color":"gray"}]
playsound minecraft:entity.item.break player @s
