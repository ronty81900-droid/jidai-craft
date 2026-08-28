# =============================================================
# jidai:sensou/youyaku_1 ── 戦争1件ぶんを勢力の欄へ写す
#   jidai:sensou/youyaku から【戦争のマーカーとして】呼ばれる。
# =============================================================
# ★ 「一番進んでいるもの」を残すため、今入っている値より
#   状態が大きい時だけ上書きする（交戦2 > 準備1 だが、
#   再戦禁止3 は数字が大きいので、そこだけ別に扱う）。
#   ★ 実際には「交戦(2)を最優先、次に準備(1)、最後に再戦禁止(3)」。
#     並べ替えるために、書き込む時だけ 交戦=3 準備=2 再戦禁止=1 の
#     「強さ」に直して比べる。
scoreboard players set #tsuyosa sagyou 0
execute if score @s sensou matches 1 run scoreboard players set #tsuyosa sagyou 2
execute if score @s sensou matches 2 run scoreboard players set #tsuyosa sagyou 3
execute if score @s sensou matches 3 run scoreboard players set #tsuyosa sagyou 1

scoreboard players operation #w_jotai sagyou = @s sensou
scoreboard players operation #w_byou sagyou = @s sensou_byou
scoreboard players operation #w_a sagyou = @s w_kuni
scoreboard players operation #w_b sagyou = @s w_aite

# a側の欄へ（今より強ければ上書き）
function jidai:sensou/youyaku_a
# b側の欄へ
function jidai:sensou/youyaku_b
