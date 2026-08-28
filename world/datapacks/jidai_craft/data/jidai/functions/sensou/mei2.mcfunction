# =============================================================
# jidai:sensou/mei2 ── 2つ目の勢力名（相手側）
#   入力  #mei_no2 sagyou   出力  storage jidai:kari mei2
# =============================================================
# ★ 「丘陵 が 森林 に宣戦した」のように2つ出す通知が多いので、
#   別の入れ物をもう1つ用意してある。中身は jidai:sensou/mei と同じ。
# ★ 勢力を増やす時は、ここに1行足す。

data modify storage jidai:kari mei2 set value ""
execute if score #mei_no2 sagyou matches 1 run data modify storage jidai:kari mei2 set value "丘陵"
execute if score #mei_no2 sagyou matches 2 run data modify storage jidai:kari mei2 set value "森林"
execute if score #mei_no2 sagyou matches 3 run data modify storage jidai:kari mei2 set value "川"
execute if score #mei_no2 sagyou matches 4 run data modify storage jidai:kari mei2 set value "内海"
execute if score #mei_no2 sagyou matches 5 run data modify storage jidai:kari mei2 set value "岩場"
