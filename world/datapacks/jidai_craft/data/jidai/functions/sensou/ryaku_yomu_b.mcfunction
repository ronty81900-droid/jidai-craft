# =============================================================
# jidai:sensou/ryaku_yomu_b ── 押した人が b側 だった場合
#   jidai:sensou/ryakudatsu から【戦争のマーカーとして】呼ばれる。
# =============================================================
# ★ マーカーのスコアは、そのマーカーとしてでないと読めない。
#   だから「見つけて → 作業用へ写す」を小さな関数に分けてある。
#   b側から見た累計(ryakudatsu_*_b)を読む。

scoreboard players set #w_mitsuke sagyou 1
scoreboard players set #jibun_a sagyou 0
scoreboard players operation #w_jotai sagyou = @s sensou
scoreboard players operation #bai sagyou = @s sensou_bai
scoreboard players operation #r_kane sagyou = @s ryakudatsu_kane_b
scoreboard players operation #r_sekiyu sagyou = @s ryakudatsu_sekiyu_b
