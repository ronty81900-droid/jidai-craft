# =============================================================
# jidai:shinko/bar ── 中央の時代への進み具合をボスバーに出す
#   2026-08-31 のご指示。
#
#   ★★ 何を出しているか ★★
#     中央の時代は「2勢力が次の時代へ到達したら上がる」。
#     そのまま出すと 0% / 50% / 100% の3段階しか動かないので、
#     勢力ごとの【3条件の達成度】を混ぜてなめらかにする。
#
#       満タン = 6  （2勢力 × 3条件）
#       各勢力の寄与:
#         次の時代へ到達済み        → 3（満額）
#         中央と同じ時代で条件を満たしている数 → 0〜3
#         それより遅れている        → 0
#
#     6 を超えることがある（3勢力が条件をそろえた等）。上限で止める。
#     承認は運営の手打ちなので「条件はそろっているが時代は上がっていない」
#     状態が普通に起きる。その時 バーは満タンのまま待つ。
#
#   ★ 中央が上がると #tsugi が変わるので、寄与が計算し直されて 0 に戻る。
#     わざわざ 0 を書く処理は要らない。
#
#   ★ 呼ばれる場所: jidai:shinko/junban（勢力の判定を1つずつ回している所）
#     の最後。判定の直後なので jouken_a/b/c が最新になっている。
# =============================================================

# 次の中央時代
scoreboard players operation #bar_tsugi sagyou = 世界 chuo
scoreboard players add #bar_tsugi sagyou 1

scoreboard players set #bar sagyou 0

# --- 丘陵 ---------------------------------------------------
scoreboard players set #bar_k sagyou 0
execute if score 丘陵 jidai >= #bar_tsugi sagyou run scoreboard players set #bar_k sagyou 3
execute if score 丘陵 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou = 丘陵 jouken_a
execute if score 丘陵 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 丘陵 jouken_b
execute if score 丘陵 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 丘陵 jouken_c
scoreboard players operation #bar sagyou += #bar_k sagyou

# --- 森林 ---------------------------------------------------
scoreboard players set #bar_k sagyou 0
execute if score 森林 jidai >= #bar_tsugi sagyou run scoreboard players set #bar_k sagyou 3
execute if score 森林 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou = 森林 jouken_a
execute if score 森林 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 森林 jouken_b
execute if score 森林 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 森林 jouken_c
scoreboard players operation #bar sagyou += #bar_k sagyou

# --- 川 -----------------------------------------------------
scoreboard players set #bar_k sagyou 0
execute if score 川 jidai >= #bar_tsugi sagyou run scoreboard players set #bar_k sagyou 3
execute if score 川 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou = 川 jouken_a
execute if score 川 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 川 jouken_b
execute if score 川 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 川 jouken_c
scoreboard players operation #bar sagyou += #bar_k sagyou

# --- 内海 ---------------------------------------------------
scoreboard players set #bar_k sagyou 0
execute if score 内海 jidai >= #bar_tsugi sagyou run scoreboard players set #bar_k sagyou 3
execute if score 内海 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou = 内海 jouken_a
execute if score 内海 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 内海 jouken_b
execute if score 内海 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 内海 jouken_c
scoreboard players operation #bar sagyou += #bar_k sagyou

# --- 岩場 ---------------------------------------------------
scoreboard players set #bar_k sagyou 0
execute if score 岩場 jidai >= #bar_tsugi sagyou run scoreboard players set #bar_k sagyou 3
execute if score 岩場 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou = 岩場 jouken_a
execute if score 岩場 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 岩場 jouken_b
execute if score 岩場 jidai = 世界 chuo run scoreboard players operation #bar_k sagyou += 岩場 jouken_c
scoreboard players operation #bar sagyou += #bar_k sagyou

# --- 上限で止める（3勢力そろうと 6 を超える）-----------------
execute if score #bar sagyou matches 7.. run scoreboard players set #bar sagyou 6

# --- 見せる相手を配り直す ------------------------------------
#   ★ load だけで @a を入れると、あとから入った人に出ない。
#     ここで毎回 入れ直せば、参加も退出も自動で追いつく。
bossbar set jidai:chuo players @a

# --- ボスバーへ反映 -----------------------------------------
bossbar set jidai:chuo value 6
execute store result bossbar jidai:chuo value run scoreboard players get #bar sagyou

# --- 色は中央の時代で変える（数字を出さずに時代を見せる）-----
#   鉄器=白 / 中世=黄 / 近代=青 / 現代=紫
execute if score 世界 chuo matches ..1 run bossbar set jidai:chuo color white
execute if score 世界 chuo matches 2 run bossbar set jidai:chuo color yellow
execute if score 世界 chuo matches 3 run bossbar set jidai:chuo color blue
execute if score 世界 chuo matches 4.. run bossbar set jidai:chuo color purple

execute if score 世界 chuo matches ..1 run bossbar set jidai:chuo name {"text":"世界の時代　鉄器","color":"white"}
execute if score 世界 chuo matches 2 run bossbar set jidai:chuo name {"text":"世界の時代　中世","color":"yellow"}
execute if score 世界 chuo matches 3 run bossbar set jidai:chuo name {"text":"世界の時代　近代","color":"aqua"}
execute if score 世界 chuo matches 4.. run bossbar set jidai:chuo name {"text":"世界の時代　現代","color":"light_purple"}
