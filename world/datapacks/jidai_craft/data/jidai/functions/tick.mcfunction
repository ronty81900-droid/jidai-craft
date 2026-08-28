# =============================================================
# jidai:tick ── 毎tick走る
#   暫定UI(チェスト式販売所)の棚だけを見張る。
#   施設ブロックの修復は 20tick の clock 側でやっている。
# =============================================================

# 棚から商品が持ち出されていたら購入処理へ送る。
# マーカーを置かなければ何も動かない(プラグイン完成後はマーカーを消すだけでよい)。
# ★ 1.20.1 に `execute ... items block` は無い(1.20.5で追加)。
#   同じことを `data block` で書く。棚の0番に目印の品が無ければ持ち出された、と見る。
execute as @e[type=marker,tag=jidai_chest_kojin] at @s unless data block ~ ~ ~ {Items:[{Slot:0b,id:"minecraft:iron_sword"}]} run function jidai:mise/chest
execute as @e[type=marker,tag=jidai_chest_seiryoku] at @s unless data block ~ ~ ~ {Items:[{Slot:0b,id:"minecraft:iron_pickaxe"}]} run function jidai:mise/chest
