# =============================================================
# jidai:sekiyu/dashi_give ── 石油を1本ずつ渡す（再帰）
#   jidai:sekiyu/dashi から呼ばれる。#hon sagyou に残りの本数が入っている。
# =============================================================
# ★★ 1.20.1 にはマクロが無い ★★
#   1.21 では `$give ... $(hon)` の1行で可変個数を渡していたが、
#   1.20.1 では個数を命令に埋め込めない。
#   そこで「1本渡して、残りを1減らして、自分をもう一度呼ぶ」形にする。
#   石油の引き出しは最大64本なので、深さは最大64。負荷は問題ない。
#
# ★ 渡す物には必ず custom_data(1.20.1 では tag)を付ける。
#   素の黒色の染料と見分けるため。

# 残りが無ければ終わり
execute unless score #hon sagyou matches 1.. run return 0

# 1本渡す
give @s minecraft:black_dye{jidai_sekiyu:"oil",display:{Name:'{"text":"石油","color":"black"}'}} 1

# 残りを1減らして、もう一度自分を呼ぶ
scoreboard players remove #hon sagyou 1
function jidai:sekiyu/dashi_give
