# =============================================================
# jidai:setup/tetsu ── 全員へ鉄装備を配る
#   石器時代を廃止したので、開始時にこれを1回打つ。
#   助走が無い代わりに、鉄器の進行条件を軽くしてある。
# =============================================================

execute as @a run give @s minecraft:iron_helmet
execute as @a run give @s minecraft:iron_chestplate
execute as @a run give @s minecraft:iron_leggings
execute as @a run give @s minecraft:iron_boots
execute as @a run give @s minecraft:iron_sword
# ★★ ツルハシはここでは配らない ★★
#   プラグインが「時代のツルハシ」を、初めて入った人へ自動で配る。
#   壊れず、死んでも落とさない専用の1本で、素の鉄のツルハシとは別物。
#   ここで素のツルハシも配ると、壊れる方が混ざって紛らわしい。
#   無くした人には  jidai tsuruhashi <名前>  で渡す。
execute as @a run give @s minecraft:iron_axe
execute as @a run give @s minecraft:iron_shovel

execute as @a run tellraw @s [{"text":"[配布] ","color":"green"},{"text":"鉄装備一式を受け取った","color":"white"}]
