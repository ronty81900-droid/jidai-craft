# =============================================================
# jidai:sensou/kuni_yomu ── 実行者(@s)の勢力名を storage へ入れる
#   「勢力の正規メンバーかどうか」を調べるために使う。
#   呼んだ側は if data storage jidai:kari {kuni:""} で弾ける。
#   ★1.21 ではマクロへ名前を渡す用途もあったが、1.20.1 では判定専用。
# =============================================================

# 前回の値が残らないよう、先に空にする
data modify storage jidai:kari kuni set value ""

# 勢力を増やす時は、ここに1行足す
execute if entity @s[team=kyuryo] run data modify storage jidai:kari kuni set value "丘陵"
execute if entity @s[team=shinrin] run data modify storage jidai:kari kuni set value "森林"
execute if entity @s[team=kawa] run data modify storage jidai:kari kuni set value "川"
execute if entity @s[team=naikai] run data modify storage jidai:kari kuni set value "内海"
execute if entity @s[team=iwaba] run data modify storage jidai:kari kuni set value "岩場"
