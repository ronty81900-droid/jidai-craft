# =============================================================
# jidai:sensou/kai_yomu ── 攻める側が銀行を壊した回数を作業用へ出す
#   入力  #q_kuni … 攻める側の勢力の番号
#         #q_aite … 守る側の勢力の番号
#   出力  #q_kai  … 攻める側が壊した回数（戦争が無ければ 0）
#
# ★ なぜ要るか
#   回数は【戦争のマーカー】が持っている。マーカーのスコアは、そのマーカーとして
#   でないと読めない。プラグインはマーカーを直接読めないので、
#   ここで固定の作業用へ写してから読んでもらう。
#   （jidai:sensou/aite_yomu と同じ型。プラグインは「呼んでから読む」）
# =============================================================
scoreboard players set #q_kai sagyou 0
execute as @e[type=marker,tag=jidai_sensou] if score @s w_kuni = #q_kuni sagyou if score @s w_aite = #q_aite sagyou run scoreboard players operation #q_kai sagyou = @s ryakudatsu_kai_a
execute as @e[type=marker,tag=jidai_sensou] if score @s w_aite = #q_kuni sagyou if score @s w_kuni = #q_aite sagyou run scoreboard players operation #q_kai sagyou = @s ryakudatsu_kai_b
