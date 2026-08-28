# =============================================================
# jidai:setup/zahyou ── 施設の座標一覧をチャットに出す
#
#   ★★ 固定の文字列ではなく、【実際に建っているマーカーを読んで】出す ★★
#   以前はここに座標を手で書いていた。施設が増えた時に直し忘れ、
#   実物と食い違っていた（2026-08-20 の棚卸しで発覚）。
#     ・金ブロック3つと書いてあったが実物は1つ
#     ・エメラルドを「売却所」と書いていたが実物は「販売所」
#     ・時代を進める鉄ブロックとエンダーかまど2つが載っていなかった
#   マーカーから読めば、二度とズレない。
#
#   ★ ゲーム内から打つこと。
#     コンソールから打つと @s が誰にも当たらず、1行も出ない。
#     コンソールからは  jidai list （プラグイン側）で同じものが見られる。
# =============================================================

# --- 打った人に印を付ける -------------------------------------
# ★ マーカーとして走らせる中では @s がマーカーになるので、
#   「誰に見せるか」を先に印で決めておく必要がある。
tag @s add jidai_zahyou_miru

tellraw @a[tag=jidai_zahyou_miru] [{"text":"── 施設の座標（実際に建っているもの）──","color":"gold","bold":true}]

# --- 数を先に出す ---------------------------------------------
# 建っていない時に「一覧が空」の理由が分かるようにする。
scoreboard players set #z_kazu sagyou 0
execute as @e[type=marker,tag=jidai_shisetsu] run scoreboard players add #z_kazu sagyou 1
tellraw @a[tag=jidai_zahyou_miru] [{"text":"施設の数: ","color":"gray"},{"score":{"name":"#z_kazu","objective":"sagyou"},"color":"yellow"},{"text":" / 32","color":"gray"}]
execute if score #z_kazu sagyou matches ..0 run tellraw @a[tag=jidai_zahyou_miru] [{"text":"まだ建っていません。","color":"red"},{"text":"/jidai setup","color":"yellow"},{"text":" を打ってください","color":"red"}]

# --- 1つずつ出す ----------------------------------------------
# ★ 拠点ごとにまとめたいので、拠点の印で順に回す。
#   最後に、どの拠点にも属さないもの（中央）を出す。
tellraw @a[tag=jidai_zahyou_miru] [{"text":"[拠点1 丘陵]","color":"white"}]
execute as @e[type=marker,tag=jidai_kyoten_kyuryo] run function jidai:setup/zahyou_1
tellraw @a[tag=jidai_zahyou_miru] [{"text":"[拠点2 森林]","color":"green"}]
execute as @e[type=marker,tag=jidai_kyoten_shinrin] run function jidai:setup/zahyou_1
tellraw @a[tag=jidai_zahyou_miru] [{"text":"[拠点3 川]","color":"aqua"}]
execute as @e[type=marker,tag=jidai_kyoten_kawa] run function jidai:setup/zahyou_1
tellraw @a[tag=jidai_zahyou_miru] [{"text":"[拠点4 内海]","color":"blue"}]
execute as @e[type=marker,tag=jidai_kyoten_naikai] run function jidai:setup/zahyou_1
tellraw @a[tag=jidai_zahyou_miru] [{"text":"[拠点5 岩場]","color":"gold"}]
execute as @e[type=marker,tag=jidai_kyoten_iwaba] run function jidai:setup/zahyou_1

tellraw @a[tag=jidai_zahyou_miru] [{"text":"[中央]","color":"light_purple"}]
execute as @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_kyuryo,tag=!jidai_kyoten_shinrin,tag=!jidai_kyoten_kawa,tag=!jidai_kyoten_naikai,tag=!jidai_kyoten_iwaba] run function jidai:setup/zahyou_1

# --- 拠点の中心も出す -----------------------------------------
# ★ ここだけは実物から読めない。拠点の中心にはマーカーを置いていないため。
#   jidai:setup/kyoten の execute positioned と必ず同じにすること。
tellraw @a[tag=jidai_zahyou_miru] [{"text":"拠点の中心（地表 Y=100）","color":"gray"}]
tellraw @a[tag=jidai_zahyou_miru] [{"text":"  丘陵 (0,-410) / 森林 (390,-127) / 川 (241,332)","color":"white"}]
tellraw @a[tag=jidai_zahyou_miru] [{"text":"  内海 (-241,332) / 岩場 (-390,-127)","color":"white"}]

# --- 印を外す -------------------------------------------------
tag @s remove jidai_zahyou_miru
