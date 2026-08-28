# =============================================================
# jidai:sensou/youyaku_b ── b側の勢力の欄へ書く
# =============================================================
# ★ 勢力名が保持者のスコアには、番号から直接書けない。
#   番号で場合分けして、勢力ごとの小さな関数へ渡す。
# ★ 勢力を増やす時は、ここに1行足す（と、下の小さな関数も1つ）。

execute if score #w_b sagyou matches 1 unless score 丘陵 sensou_tsuyosa >= #tsuyosa sagyou run function jidai:sensou/youyaku_b_1
execute if score #w_b sagyou matches 2 unless score 森林 sensou_tsuyosa >= #tsuyosa sagyou run function jidai:sensou/youyaku_b_2
execute if score #w_b sagyou matches 3 unless score 川 sensou_tsuyosa >= #tsuyosa sagyou run function jidai:sensou/youyaku_b_3
execute if score #w_b sagyou matches 4 unless score 内海 sensou_tsuyosa >= #tsuyosa sagyou run function jidai:sensou/youyaku_b_4
execute if score #w_b sagyou matches 5 unless score 岩場 sensou_tsuyosa >= #tsuyosa sagyou run function jidai:sensou/youyaku_b_5
