# =============================================================
# jidai:shinko/choshu ── 石油の4割を徴収する(1人分)
#   まだ今回の徴収を受けていない人に対して、jidai:clock から走る。
#   端数は切り捨て。整数の割り算なので持っている側が少し得をする。
# =============================================================
# ★中央の時代が上がった瞬間に @a へ配ると、その時ログインしていない人が
#   徴収を逃れてしまう(ログアウトして待てば得をする抜け穴になる)。
#   そこで「世代番号」を持たせ、番号が古い人を見つけ次第その場で徴収する。
#   オフラインでもスコアは残るので、次にログインした時に必ず徴収される。

# 徴収する本数 = 所持 × 徴収率 ÷ 100
scoreboard players operation #hiku sagyou = @s sekiyu
scoreboard players operation #hiku sagyou *= リセット_徴収率 settei
scoreboard players set #hyaku sagyou 100
scoreboard players operation #hiku sagyou /= #hyaku sagyou

scoreboard players operation @s sekiyu -= #hiku sagyou

# この人は今回の徴収を受けた、と記録する
scoreboard players operation @s choshu = 世界 choshu

# 1本以上取られた人にだけ知らせる
execute if score #hiku sagyou matches 1.. run tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"text":"時代の移り変わりで ","color":"gray"},{"score":{"name":"#hiku","objective":"sagyou"},"color":"red"},{"text":" 本が徴収された (残り ","color":"gray"},{"score":{"name":"@s","objective":"sekiyu"},"color":"white"},{"text":" 本)","color":"gray"}]
