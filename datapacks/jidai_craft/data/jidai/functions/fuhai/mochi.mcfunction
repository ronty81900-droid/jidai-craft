# =============================================================
# jidai:fuhai/mochi ── 引き出して手に持っている石油を総量に足す
#   実行者(@s)の持ち物を数えて #gokei に加える。1本も消さない。
# =============================================================
# ★引き出してアイテムにしただけでは「使った」ことにならない
#   (指示書 §9)。数えないと、腐る直前に全部引き出して逃げられる。
#   clear の最後の 0 が「1個も消さずに個数だけ返す」数えるだけモード。

# 数えられなかった時に前回の値が残らないよう、先に 0 にする
# (clear はプレイヤー専用。検証用のアーマースタンドでは失敗する)
scoreboard players set #mochi sagyou 0
execute store result score #mochi sagyou run clear @s minecraft:black_dye{jidai_sekiyu:"oil"} 0

scoreboard players operation #gokei sagyou += #mochi sagyou
