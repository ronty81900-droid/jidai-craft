# =============================================================
# jidai:shinko/plant ── 中央プラントの外観を、今の中央の時代に合わせる
#   中央の時代が上がった時と、設置の時に呼ばれる。
#   仮のブロックで段階が変わることを確かめる形。実物は別工程。
# =============================================================
# ★「切替時にプレイヤーが埋まる」問題は、コードではなく形で解いてある。
#
#   1. 4段階すべて X/Z の占有範囲(footprint)が完全に同じ  … 12x12
#   2. 変化は上方向だけ。横に広がらない
#   3. 土台は全段階で埋まっている                        … Y97〜98 が中身の詰まった石
#   4. 登れる経路を作らない                              … 側面が平らな柱だけ
#   5. 石油はプラントの中ではなく周囲に落ちる            … jidai:sekiyu/waku
#
#   プレイヤーはプラントの「周り」に集まる。中にも上にもいないので、
#   差し替えても誰も埋まらない。退避処理が要らない。
#   石油の取り合いも中断されない。
#
# 実物のストラクチャーに差し替える時も、この5つを守れば
# ここの fill を place structure に置き換えるだけで済む。
# (ストラクチャーブロックは 32x32x32 が上限。footprint 12x12 なら
#  高さ32まで1つに収まる。それを超える段階は縦に分割する)

# --- 古い外観を消す -------------------------------------------
# footprint は常に同じなので、この範囲だけ消せばよい
execute as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-6 97 ~-6 ~5 128 ~5 minecraft:air replace

# --- 土台 (全段階で共通。中に立てないよう中身を詰める) --------
execute as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-6 97 ~-6 ~5 98 ~5 minecraft:deepslate_tiles replace

# --- 時代1 鉄器: 手押しポンプ (低い) --------------------------
execute if score 世界 chuo matches 1 as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-3 99 ~-3 ~2 101 ~2 minecraft:cobblestone replace

# --- 時代2 中世: 石造りの塔 -----------------------------------
execute if score 世界 chuo matches 2 as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-3 99 ~-3 ~2 108 ~2 minecraft:stone_bricks replace

# --- 時代3 近代: 蒸気機関と煙突 -------------------------------
execute if score 世界 chuo matches 3 as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-3 99 ~-3 ~2 112 ~2 minecraft:polished_deepslate replace
execute if score 世界 chuo matches 3 as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-1 113 ~-1 ~0 118 ~0 minecraft:polished_blackstone replace

# --- 時代4 現代: 巨大プラント ---------------------------------
execute if score 世界 chuo matches 4.. as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-3 99 ~-3 ~2 120 ~2 minecraft:nether_bricks replace
execute if score 世界 chuo matches 4.. as @e[type=marker,tag=jidai_plant,limit=1] at @s run fill ~-1 121 ~-1 ~0 128 ~0 minecraft:polished_blackstone replace
