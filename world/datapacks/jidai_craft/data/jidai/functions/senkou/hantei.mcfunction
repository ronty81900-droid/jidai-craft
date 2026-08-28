# =============================================================
# jidai:senkou/hantei ── 先行ペナルティの判定 (1秒ごと)
#   中央の時代より先へ進んだ勢力を「先行」とし、
#     ・メンバー全員を光らせる      (位置が全員に見える)
#     ・中央の石油の取得効率を落とす (実際に減らすのは sekiyu/azukeru)
#       ★ 2026-08-20 に「拾った時」から「銀行へ預けた時」へ移った。
#         拾っただけでは数に入らなくなったため。掛かる本数は同じ。
#   中央が追いつくと自動で解除される。
# =============================================================
# ★先行になるのは【独走している1勢力だけ】。
#   中央は「2勢力が到達したら上がる」ので、2つめの勢力が着いた瞬間に
#   中央も上がり、両方とも先行でなくなる。
#   つまり「単独で先頭に立った勢力」だけが対象になる。
#   追加の判定基準を作らなくてよく、全員が追いつけば勝手に解除される。
#
#   光らせないし、石油の目減りもしない。

# --- 1) 勢力ごとに、先行かどうかを調べる ----------------------
# 前回の値を #mae に取っておき、変わった時だけ通知する。
# 気づかないまま光っていると理不尽なので、発動も解除も知らせる。
# 勢力を増やす時は、この5行のかたまりを複製する

# 丘陵
scoreboard players operation #mae sagyou = 丘陵 senkou
scoreboard players set 丘陵 senkou 0
execute if score 丘陵 jidai > 世界 chuo run scoreboard players set 丘陵 senkou 1
execute if score 丘陵 senkou matches 1 if score #mae sagyou matches 0 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"丘陵 が中央より先へ進んだ。位置が全員に見え、中央の石油が取りにくくなる","color":"white"}]
execute if score 丘陵 senkou matches 0 if score #mae sagyou matches 1 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"丘陵 の先行状態が解除された (中央が追いついた)","color":"white"}]

# 森林
scoreboard players operation #mae sagyou = 森林 senkou
scoreboard players set 森林 senkou 0
execute if score 森林 jidai > 世界 chuo run scoreboard players set 森林 senkou 1
execute if score 森林 senkou matches 1 if score #mae sagyou matches 0 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"森林 が中央より先へ進んだ。位置が全員に見え、中央の石油が取りにくくなる","color":"white"}]
execute if score 森林 senkou matches 0 if score #mae sagyou matches 1 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"森林 の先行状態が解除された (中央が追いついた)","color":"white"}]

# 川
scoreboard players operation #mae sagyou = 川 senkou
scoreboard players set 川 senkou 0
execute if score 川 jidai > 世界 chuo run scoreboard players set 川 senkou 1
execute if score 川 senkou matches 1 if score #mae sagyou matches 0 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"川 が中央より先へ進んだ。位置が全員に見え、中央の石油が取りにくくなる","color":"white"}]
execute if score 川 senkou matches 0 if score #mae sagyou matches 1 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"川 の先行状態が解除された (中央が追いついた)","color":"white"}]

# 内海
scoreboard players operation #mae sagyou = 内海 senkou
scoreboard players set 内海 senkou 0
execute if score 内海 jidai > 世界 chuo run scoreboard players set 内海 senkou 1
execute if score 内海 senkou matches 1 if score #mae sagyou matches 0 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"内海 が中央より先へ進んだ。位置が全員に見え、中央の石油が取りにくくなる","color":"white"}]
execute if score 内海 senkou matches 0 if score #mae sagyou matches 1 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"内海 の先行状態が解除された (中央が追いついた)","color":"white"}]

# 岩場
scoreboard players operation #mae sagyou = 岩場 senkou
scoreboard players set 岩場 senkou 0
execute if score 岩場 jidai > 世界 chuo run scoreboard players set 岩場 senkou 1
execute if score 岩場 senkou matches 1 if score #mae sagyou matches 0 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"岩場 が中央より先へ進んだ。位置が全員に見え、中央の石油が取りにくくなる","color":"white"}]
execute if score 岩場 senkou matches 0 if score #mae sagyou matches 1 run tellraw @a [{"text":"[先行] ","color":"gold","bold":true},{"text":"岩場 の先行状態が解除された (中央が追いついた)","color":"white"}]

# --- 2) 今回 光るべき人に、一時的な印を付ける -----------------
# 発光を選んだ理由: 遠くからでも分かり、【チームの色で光る】ので
# どの勢力かも同時に分かる。数字を読ませずに情報を渡せる。
# 地形を透過するので地下に隠れても見えるが、これは
# 「隠れられない」というペナルティとして意図どおり。
# 勢力を増やす時は、ここに1行足す
execute if score 丘陵 senkou matches 1 as @a[team=kyuryo] run tag @s add jidai_hikaru_ima
execute if score 森林 senkou matches 1 as @a[team=shinrin] run tag @s add jidai_hikaru_ima
execute if score 川 senkou matches 1 as @a[team=kawa] run tag @s add jidai_hikaru_ima
execute if score 内海 senkou matches 1 as @a[team=naikai] run tag @s add jidai_hikaru_ima
execute if score 岩場 senkou matches 1 as @a[team=iwaba] run tag @s add jidai_hikaru_ima

# --- 3) 印が付いた人を光らせる --------------------------------
# ★毎秒かけ直す。こうしておくと、ログアウト中に状態が変わった人にも、
#   次にログインした1秒後に正しい状態が入る。
#   infinite でかけ直しても効果は1つのまま(1.21.10 で実測)。
# jidai_hikari は「こちらが光らせた」という記録。
execute as @a[tag=jidai_hikaru_ima] run effect give @s minecraft:glowing infinite 0 true
execute as @a[tag=jidai_hikaru_ima] run tag @s add jidai_hikari

# --- 4) 対象から外れた人の発光を消す --------------------------
# 「光らせた記録はあるが、今回は対象でない」人だけを消す。
# ★効果は死んでもログアウトしても残るので、消す側も必要。
#   記録が無い人には撃たないので、無駄な失敗が出ない。
execute as @a[tag=jidai_hikari,tag=!jidai_hikaru_ima] run effect clear @s minecraft:glowing
execute as @a[tag=jidai_hikari,tag=!jidai_hikaru_ima] run tag @s remove jidai_hikari

# --- 5) 一時的な印を片付ける ----------------------------------
execute as @a[tag=jidai_hikaru_ima] run tag @s remove jidai_hikaru_ima
