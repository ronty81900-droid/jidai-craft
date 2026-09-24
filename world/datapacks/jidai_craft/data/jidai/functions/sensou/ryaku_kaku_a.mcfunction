# =============================================================
# jidai:sensou/ryaku_kaku_a ── a側が奪った分を書き足す
#   jidai:sensou/ryakudatsu から【戦争のマーカーとして】呼ばれる。
# =============================================================
# ★ マーカーのスコアは、そのマーカーとしてでないと読めない。
#   だから「見つけて → 作業用へ写す」を小さな関数に分けてある。
#   上限は向きごとなので、a側の欄にだけ足す。

scoreboard players operation @s ryakudatsu_kane_a += #gaku sagyou
scoreboard players operation @s ryakudatsu_sekiyu_a += #hon sagyou
# ★ 2026-09-09: 壊した回数。占領_必要回数 に届くとビーコンが壊せる。
scoreboard players add @s ryakudatsu_kai_a 1
scoreboard players operation #r_kai sagyou = @s ryakudatsu_kai_a
