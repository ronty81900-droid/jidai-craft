# =============================================================
# jidai:fuhai/hiku_hitori ── 腐敗ぶんを1人から引く
#   その勢力で最も多く石油を持っている人として走る。
#   呼び出し元が #heru (引く本数) と #zumi (0) を用意している。
# =============================================================

# --- 同点が居ても1人だけにする --------------------------------
# 最大値と一致する人が2人以上いると全員から引いてしまうので、
# 一度引いたら以降は素通りする。
execute if score #zumi sagyou matches 1.. run return 0
scoreboard players set #zumi sagyou 1

# 呼び出し元で #heru は「その人の所持」以下に収めてあるので、
# マイナスにはならない。
scoreboard players operation @s sekiyu -= #heru sagyou

tellraw @s [{"text":"[腐敗] ","color":"dark_red"},{"text":"溜め込んだ石油が ","color":"gray"},{"score":{"name":"#heru","objective":"sagyou"},"color":"red"},{"text":" 本 腐った (残り ","color":"gray"},{"score":{"name":"@s","objective":"sekiyu"},"color":"white"},{"text":" 本)","color":"gray"}]
tellraw @s [{"text":"  勢力の石油が減っていないと腐る。使うか、売るか、払うこと","color":"dark_gray"}]
playsound minecraft:block.fire.extinguish player @s
