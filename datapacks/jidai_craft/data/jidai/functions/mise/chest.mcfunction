# =============================================================
# jidai:mise/chest ── チェスト式販売所から商品が持ち出された(暫定UI)
#   マーカー(=容器と同じ座標)として走る。マーカーのタグで品を見分ける。
#   プラグイン完成後はマーカーを置かなければ無効化できる。
# =============================================================

# 在庫は無限。取られた分をすぐ補充する
execute if entity @s[tag=jidai_chest_kojin] run item replace block ~ ~ ~ container.0 with minecraft:iron_sword 1
execute if entity @s[tag=jidai_chest_seiryoku] run item replace block ~ ~ ~ container.0 with minecraft:iron_pickaxe 1

# 購入者は「自分で樽を開けた本人」に限る(印は jidai:mise/aketa が付ける)
tag @p[tag=jidai_kaimono,distance=..6] add jidai_chest_kai
execute if entity @s[tag=jidai_chest_kojin] as @p[tag=jidai_kaimono,distance=..6] run function jidai:mise/kau_kojin
execute if entity @s[tag=jidai_chest_seiryoku] as @p[tag=jidai_kaimono,distance=..6] run function jidai:mise/kau_seiryoku
