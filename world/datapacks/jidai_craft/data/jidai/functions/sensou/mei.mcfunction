# =============================================================
# jidai:sensou/mei ── 勢力の番号を名前に直す
#   入力  #mei_no sagyou … 1〜5
#   出力  storage jidai:kari mei … "丘陵" など
# =============================================================
# ★★ なぜ storage を経由するか ★★
#   1.20.1 にはマクロが無いので、命令の文字列に名前を埋め込めない。
#   だが tellraw は storage の中身を読める({"storage":..,"nbt":..})ので、
#   ここで名前を storage に置いておけば、通知の文面に出せる。
#   これをやらないと、通知のたびに5勢力ぶんの tellraw を並べることになる。
#
# ★ 勢力を増やす時は、ここに1行足す。

data modify storage jidai:kari mei set value ""
execute if score #mei_no sagyou matches 1 run data modify storage jidai:kari mei set value "丘陵"
execute if score #mei_no sagyou matches 2 run data modify storage jidai:kari mei set value "森林"
execute if score #mei_no sagyou matches 3 run data modify storage jidai:kari mei set value "川"
execute if score #mei_no sagyou matches 4 run data modify storage jidai:kari mei set value "内海"
execute if score #mei_no sagyou matches 5 run data modify storage jidai:kari mei set value "岩場"
