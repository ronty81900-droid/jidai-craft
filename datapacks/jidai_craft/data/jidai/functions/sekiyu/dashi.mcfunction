# =============================================================
# jidai:sekiyu/dashi ── 石油をアイテムとして引き出す
#   /trigger sekiyu_dashi set 5 を打った本人として走る。
#   バニラには相手を指定した送金が無いので、これが取引の手段になる。
# =============================================================

# 打たれた本数を作業用へ取り出し、トリガはすぐ 0 に戻す
scoreboard players operation #hon sagyou = @s sekiyu_dashi
scoreboard players set @s sekiyu_dashi 0

# --- 0以下は弾く ----------------------------------------------
# 負の数を通すと、引き算がマイナスになって無限に増やせてしまう
execute if score #hon sagyou matches ..0 run tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"text":"1本以上を指定してください","color":"red"}]
execute if score #hon sagyou matches ..0 run return 0

# --- 一度に出せるのは64本まで (1スタック) ---------------------
execute if score #hon sagyou matches 65.. run tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"text":"一度に引き出せるのは64本まで","color":"red"}]
execute if score #hon sagyou matches 65.. run return 0

# --- 残高を超える指定を弾く -----------------------------------
execute unless score @s sekiyu >= #hon sagyou run tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"text":"石油が足りません (指定 ","color":"red"},{"score":{"name":"#hon","objective":"sagyou"}},{"text":" 本 / 所持 ","color":"red"},{"score":{"name":"@s","objective":"sekiyu"}},{"text":" 本)","color":"red"}]
execute unless score @s sekiyu >= #hon sagyou run playsound minecraft:entity.villager.no player @s
execute unless score @s sekiyu >= #hon sagyou run return 0

# --- 先にスコアを減らしてから、アイテムを渡す ------------------
# 逆にすると、渡した後に減らし損ねた時アイテムだけ増える
scoreboard players operation @s sekiyu -= #hon sagyou

# give に渡す個数は「その場に書いた数字」でないと通らない。
# 本数は毎回変わるので、storage へ入れてマクロで渡す。
# (マクロを使うのはここだけ。他は全部そのまま書いている)
# ★ マクロが無いので storage で渡さない。#hon sagyou をそのまま見せる。
#   ★ dashi_give は #hon を 0 まで減らすので、
#     このあとの通知で本数を出したい分を先に控えておく。
scoreboard players operation #dashita sagyou = #hon sagyou
function jidai:sekiyu/dashi_give

tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"score":{"name":"#dashita","objective":"sagyou"},"color":"light_purple"},{"text":" 本をアイテムにした (残り ","color":"white"},{"score":{"name":"@s","objective":"sekiyu"},"color":"white"},{"text":" 本)","color":"white"}]
tellraw @s [{"text":"  手持ちの石油は死ぬと落ちる。銀行で預け直すと数に入る","color":"dark_gray"}]
playsound minecraft:item.bucket.fill player @s
