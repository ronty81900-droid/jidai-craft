# =============================================================
# jidai:sensou/ryaku_kaku_a ── a側が奪った分を書き足す
#   jidai:sensou/ryakudatsu から【戦争のマーカーとして】呼ばれる。
# =============================================================
# ★ マーカーのスコアは、そのマーカーとしてでないと読めない。
#   だから「見つけて → 作業用へ写す」を小さな関数に分けてある。
#   上限は向きごとなので、a側の欄にだけ足す。

scoreboard players operation @s ryakudatsu_kane_a += #gaku sagyou
scoreboard players operation @s ryakudatsu_sekiyu_a += #hon sagyou
