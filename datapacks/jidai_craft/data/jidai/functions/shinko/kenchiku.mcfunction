# =============================================================
# jidai:shinko/kenchiku ── 拠点の建築を数える
#   呼び出し元が execute positioned で拠点の中心に位置を合わせて呼ぶ。
#   中心から 12ブロック四方・高さ 101〜112 の区画を数える。
# =============================================================
# 厳密なテンプレート照合はしない。全員が同じ建物を建てることになり、
# 建築の楽しさが死ぬため。「指定ブロックが N 個以上」の緩い判定にする。

# 数え方: clone の filtered は【一致したブロックの数】を返す。
#   ・元のブロックは壊れない(非破壊。実測で確認済み)
#   ・退避先は同じ場所の真下 Y=-60。同じチャンクなので必ず読み込まれている
#   ・一度に扱えるのは 32768 ブロックまで。ここは 24x12x24 = 6912 で収まる
# バニラには「範囲内の指定ブロックを数える」命令が無いので、この形になる。
# ★ 数える対象は5種類。clone の filtered は1種類しか数えられないので、
#   5回まわして足す。範囲は 24x12x24 = 6912 で、1回の上限 32768 に収まる。
#   ★ 作業台は【クラフト禁止】なので数えない(そもそも作れない)。
scoreboard players set #kenchiku sagyou 0
execute store result score #tmp_k sagyou run clone ~-12 101 ~-12 ~11 112 ~11 ~-12 -60 ~-12 filtered minecraft:cobblestone force
scoreboard players operation #kenchiku sagyou += #tmp_k sagyou
execute store result score #tmp_k sagyou run clone ~-12 101 ~-12 ~11 112 ~11 ~-12 -60 ~-12 filtered #minecraft:logs force
scoreboard players operation #kenchiku sagyou += #tmp_k sagyou
execute store result score #tmp_k sagyou run clone ~-12 101 ~-12 ~11 112 ~11 ~-12 -60 ~-12 filtered #minecraft:planks force
scoreboard players operation #kenchiku sagyou += #tmp_k sagyou
execute store result score #tmp_k sagyou run clone ~-12 101 ~-12 ~11 112 ~11 ~-12 -60 ~-12 filtered minecraft:stone_bricks force
scoreboard players operation #kenchiku sagyou += #tmp_k sagyou
execute store result score #tmp_k sagyou run clone ~-12 101 ~-12 ~11 112 ~11 ~-12 -60 ~-12 filtered minecraft:furnace force
scoreboard players operation #kenchiku sagyou += #tmp_k sagyou
