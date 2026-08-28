# =============================================================
# jidai:sensou/aite_yomu ── 近くの銀行が「どの勢力の拠点か」を storage へ
#   金ブロックを押した本人として走る。
#   拠点の施設マーカーには jidai_kyoten_<チーム名> が付いている
#   (jidai:setup/kyoten が拠点ごとに後付けする)。
# =============================================================
# ★v5 までは、どの拠点の銀行でも「押した人の勢力」へ預かっていた。
#   略奪には「どの勢力の拠点か」が要るので、拠点ごとの印を足した。

# 前回の値が残らないよう、先に空にする。
# 名前(マクロへ渡す用)と番号(自分の勢力と比べる用)の両方を出す。
# 番号があると「自分の拠点かどうか」をスコアの比較1行で判定できる。
data modify storage jidai:kari aite set value ""
scoreboard players set #aite_no sagyou 0

# 勢力を増やす時は、この2行のかたまりを複製する
execute if entity @e[type=marker,tag=jidai_kyoten_kyuryo,distance=..4] run data modify storage jidai:kari aite set value "丘陵"
execute if entity @e[type=marker,tag=jidai_kyoten_kyuryo,distance=..4] run scoreboard players set #aite_no sagyou 1
execute if entity @e[type=marker,tag=jidai_kyoten_shinrin,distance=..4] run data modify storage jidai:kari aite set value "森林"
execute if entity @e[type=marker,tag=jidai_kyoten_shinrin,distance=..4] run scoreboard players set #aite_no sagyou 2
execute if entity @e[type=marker,tag=jidai_kyoten_kawa,distance=..4] run data modify storage jidai:kari aite set value "川"
execute if entity @e[type=marker,tag=jidai_kyoten_kawa,distance=..4] run scoreboard players set #aite_no sagyou 3
execute if entity @e[type=marker,tag=jidai_kyoten_naikai,distance=..4] run data modify storage jidai:kari aite set value "内海"
execute if entity @e[type=marker,tag=jidai_kyoten_naikai,distance=..4] run scoreboard players set #aite_no sagyou 4
execute if entity @e[type=marker,tag=jidai_kyoten_iwaba,distance=..4] run data modify storage jidai:kari aite set value "岩場"
execute if entity @e[type=marker,tag=jidai_kyoten_iwaba,distance=..4] run scoreboard players set #aite_no sagyou 5
