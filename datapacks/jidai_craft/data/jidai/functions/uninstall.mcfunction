# =============================================================
# jidai:uninstall ── 作った物を全部消す(テストのやり直し用)
#   スコアボード・チーム・時計・施設のマーカーを撤去する。
#   もう一度使うときは /reload すれば load が走って作り直される。
# =============================================================

# 先に時計を止める。忘れると、消した後も clock が動いて赤字が出る
schedule clear jidai:clock

scoreboard objectives remove kane_kojin
scoreboard objectives remove sekiyu
scoreboard objectives remove chokin
scoreboard objectives remove sekiyu_gokei
scoreboard objectives remove jidai
scoreboard objectives remove kenchiku
scoreboard objectives remove jouken_a
scoreboard objectives remove jouken_b
scoreboard objectives remove jouken_c
scoreboard objectives remove chuo
scoreboard objectives remove wakidashi
scoreboard objectives remove choshu
scoreboard objectives remove senkou
scoreboard objectives remove fuhai_kijun
scoreboard objectives remove fuhai_byou
scoreboard objectives remove sensou
scoreboard objectives remove sensou_byou
# ★ ここから下は 1.20.1 のマーカー方式で足したもの。
#   2026-08-20 まで消し忘れていた（棚卸しで発覚）。
#   戦争のマーカーが持つスコア
scoreboard objectives remove w_kuni
scoreboard objectives remove w_aite
scoreboard objectives remove ryakudatsu_kane_a
scoreboard objectives remove ryakudatsu_kane_b
scoreboard objectives remove ryakudatsu_sekiyu_a
scoreboard objectives remove ryakudatsu_sekiyu_b
#   プラグインが読む「勢力ごとの要約」と、その作業用
scoreboard objectives remove sensou_aite
scoreboard objectives remove sensou_tsuyosa
scoreboard objectives remove sensou_bai
scoreboard objectives remove ryakudatsu_kan
scoreboard objectives remove senryou
scoreboard objectives remove gekokujo
scoreboard objectives remove shokuminchi
scoreboard objectives remove shouri
scoreboard objectives remove bangou
scoreboard objectives remove settei
# ★ hyouji は v6 の途中まで使っていた1本の目的。もう作っていないが、
#   古い世界に残っている可能性があるので消す指示は残す。
#   （存在しないものを消しても、黙って何も起きないだけ）
scoreboard objectives remove hyouji
scoreboard objectives remove hyouji_0
scoreboard objectives remove hyouji_1
scoreboard objectives remove hyouji_2
scoreboard objectives remove hyouji_3
scoreboard objectives remove hyouji_4
scoreboard objectives remove hyouji_5
scoreboard objectives remove sagyou
scoreboard objectives remove kaimono
scoreboard objectives remove sekiyu_dashi

team remove kyuryo
team remove shinrin
team remove kawa
team remove naikai
team remove iwaba

# 戦争そのもの(マーカー1体 = 戦争1つ)を消す
execute as @e[type=marker,tag=jidai_sensou] run kill @s

# 施設と暫定UIのマーカーを消す
execute as @e[type=marker,tag=jidai_shisetsu] run kill @s
execute as @e[type=marker,tag=jidai_mise_kojin] run kill @s
execute as @e[type=marker,tag=jidai_mise_seiryoku] run kill @s
execute as @e[type=marker,tag=jidai_chest_kojin] run kill @s
execute as @e[type=marker,tag=jidai_chest_seiryoku] run kill @s

# 地面に残った石油アイテムも消す
execute as @e[type=item,tag=jidai_sekiyu] run kill @s

# プレイヤーに付いた印も外す
execute as @a[tag=jidai_kaimono] run tag @s remove jidai_kaimono
execute as @a[tag=jidai_chest_kai] run tag @s remove jidai_chest_kai

# 先行ペナルティの発光を消す。効果はログアウトしても残るので、
# 印(jidai_hikari)が付いている人から確実に外す。
execute as @a[tag=jidai_hikari] run effect clear @s minecraft:glowing
execute as @a[tag=jidai_hikari] run tag @s remove jidai_hikari

# 略奪の通知に名前を出すための一時的な印。
# ふだんは jidai:sensou/ryakudatsu が通知の直後に外すが、
# 途中で止まった時に残らないよう、撤去でも外しておく。
execute as @a[tag=jidai_ubawareta] run tag @s remove jidai_ubawareta
execute as @a[tag=jidai_hikaru_ima] run tag @s remove jidai_hikaru_ima

forceload remove all

execute as @a run tellraw @s [{"text":"[時代クラフト] ","color":"gold"},{"text":"撤去しました","color":"gray"}]
