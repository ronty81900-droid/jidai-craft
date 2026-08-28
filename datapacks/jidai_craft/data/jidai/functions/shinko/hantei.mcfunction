# =============================================================
# jidai:shinko/hantei ── 3条件を判定する(勢力共通)
#   呼び出し元が #sekiyu / #chokin / #jidai / #kenchiku を用意して呼ぶ。
#   結果を #a #b #c と、3つ揃ったかの #zenbu に入れる。
# =============================================================

# --- その勢力の【今の時代】に応じた必要量を選ぶ ---------------
# ★ 進むほど重くなる。時代5(未来)が上限。★ 現代→未来 は 2026-08-23 に足した。
scoreboard players set #hitsuyo_sekiyu sagyou 0
scoreboard players set #hitsuyo_chokin sagyou 0
scoreboard players set #hitsuyo_kenchiku sagyou 0
execute if score #jidai sagyou matches 1 run scoreboard players operation #hitsuyo_sekiyu sagyou = 進行_石油_鉄器 settei
execute if score #jidai sagyou matches 1 run scoreboard players operation #hitsuyo_chokin sagyou = 進行_貯金_鉄器 settei
execute if score #jidai sagyou matches 1 run scoreboard players operation #hitsuyo_kenchiku sagyou = 進行_建築_鉄器 settei
execute if score #jidai sagyou matches 2 run scoreboard players operation #hitsuyo_sekiyu sagyou = 進行_石油_中世 settei
execute if score #jidai sagyou matches 2 run scoreboard players operation #hitsuyo_chokin sagyou = 進行_貯金_中世 settei
execute if score #jidai sagyou matches 2 run scoreboard players operation #hitsuyo_kenchiku sagyou = 進行_建築_中世 settei
execute if score #jidai sagyou matches 3 run scoreboard players operation #hitsuyo_sekiyu sagyou = 進行_石油_近代 settei
execute if score #jidai sagyou matches 3 run scoreboard players operation #hitsuyo_chokin sagyou = 進行_貯金_近代 settei
execute if score #jidai sagyou matches 3 run scoreboard players operation #hitsuyo_kenchiku sagyou = 進行_建築_近代 settei
# ★★ 現代 → 未来（2026-08-23 のご指示）★★
#   ここへ進めた勢力が勝つ。条件は石油と建築の2つで、貯金は既定 0（要らない）。
execute if score #jidai sagyou matches 4 run scoreboard players operation #hitsuyo_sekiyu sagyou = 進行_石油_現代 settei
execute if score #jidai sagyou matches 4 run scoreboard players operation #hitsuyo_chokin sagyou = 進行_貯金_現代 settei
execute if score #jidai sagyou matches 4 run scoreboard players operation #hitsuyo_kenchiku sagyou = 進行_建築_現代 settei

# --- 条件A: 石油の【本数】 ------------------------------------
# ★ v6 までは「その時代に湧く量の20%」という割合だったが、
#   絶対の本数に変えた(2026-08-18)。湧く総量の方を
#   必要量×5勢力より多くしてあるので、取り合いは残る。
scoreboard players set #a sagyou 0
execute if score #sekiyu sagyou >= #hitsuyo_sekiyu sagyou run scoreboard players set #a sagyou 1

# --- 条件B: 勢力の貯金 ----------------------------------------
# 進行時に消費する(累積にしない)。消費は承認の側で行う。
scoreboard players set #b sagyou 0
execute if score #chokin sagyou >= #hitsuyo_chokin sagyou run scoreboard players set #b sagyou 1

# --- 条件C: 建築 ----------------------------------------------
# 拠点の中に置かれた 丸石・原木・木の板・石レンガ・かまど の合計。
scoreboard players set #c sagyou 0
execute if score #kenchiku sagyou >= #hitsuyo_kenchiku sagyou run scoreboard players set #c sagyou 1

# --- 3つ揃ったか ----------------------------------------------
scoreboard players set #zenbu sagyou 0
execute if score #a sagyou matches 1 if score #b sagyou matches 1 if score #c sagyou matches 1 run scoreboard players set #zenbu sagyou 1

# 時代5(未来)が上限。すでに未来なら進めない
execute if score #jidai sagyou matches 5.. run scoreboard players set #zenbu sagyou 0
