# =============================================================
# jidai:sensou/shokuminchi ── 拠点のビーコンが壊され、植民地になる
#
#   入力  #s_kuni sagyou … 壊した側（宗主国）の勢力番号 1〜5
#         #s_aite sagyou … 壊された側（植民地になる）の勢力番号 1〜5
#
#   ★★ 呼ぶのはプラグイン ★★
#     ビーコンを壊せるかどうかの判定（交戦中か・相手の貯金が0か）は
#     プラグイン側が持っている。ここは「壊れたあと」だけを引き受ける。
#
#   ★ 勝利条件その1「戦争勝利」
#     自勢力を除く4勢力すべてを植民地にしたら勝ち。
#     判定は jidai:sensou/shouri。
# =============================================================

# --- 植民地の印を付ける ---------------------------------------
# shokuminchi の値 = 宗主国の勢力番号。0 なら独立している。
# ★ 既に他国の植民地だった場合も、上書きする（乗っ取り）。
execute if score #s_aite sagyou matches 1 run scoreboard players operation 丘陵 shokuminchi = #s_kuni sagyou
execute if score #s_aite sagyou matches 2 run scoreboard players operation 森林 shokuminchi = #s_kuni sagyou
execute if score #s_aite sagyou matches 3 run scoreboard players operation 川 shokuminchi = #s_kuni sagyou
execute if score #s_aite sagyou matches 4 run scoreboard players operation 内海 shokuminchi = #s_kuni sagyou
execute if score #s_aite sagyou matches 5 run scoreboard players operation 岩場 shokuminchi = #s_kuni sagyou

# --- 全体へ知らせる -------------------------------------------
# ★ 隠さない。誰が誰を落としたかは、全員が知るべき情報。
scoreboard players operation #mei_no sagyou = #s_kuni sagyou
function jidai:sensou/mei
scoreboard players operation #mei_no2 sagyou = #s_aite sagyou
function jidai:sensou/mei2

tellraw @a [{"text":"","color":"white"}]
tellraw @a [{"text":"═══════════════════════════","color":"dark_red"}]
tellraw @a [{"text":"[占領] ","color":"dark_red","bold":true},{"nbt":"mei","storage":"jidai:kari","color":"yellow"},{"text":" が ","color":"white"},{"nbt":"mei2","storage":"jidai:kari","color":"red"},{"text":" の拠点を落とした","color":"white"}]
tellraw @a [{"text":"       ","color":"white"},{"nbt":"mei2","storage":"jidai:kari","color":"red"},{"text":" は ","color":"white"},{"nbt":"mei","storage":"jidai:kari","color":"yellow"},{"text":" の植民地になった","color":"white"}]
tellraw @a [{"text":"═══════════════════════════","color":"dark_red"}]
execute as @a run playsound minecraft:entity.wither.spawn player @s

# --- 勝ったかどうかを調べる -----------------------------------
function jidai:sensou/shouri
