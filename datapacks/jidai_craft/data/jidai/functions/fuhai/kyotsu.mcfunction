# =============================================================
#   呼び出し元が次の4つを用意して呼ぶ。
#     #gokei … 今の石油の総量
#     #kijun … 基準値 (前回 時計を戻した時の総量)
#     #byou  … 経過秒
#     #yuyo  … 猶予秒 (これを超えたら腐る)
#   結果は #heru (減らす本数。0なら腐らない) と、
#   更新された #kijun #byou。書き戻すのは呼び出し元の仕事。
# =============================================================

scoreboard players set #heru sagyou 0
scoreboard players set #hyaku sagyou 100

# --- 1) 「使った」と認める減り幅を求める ----------------------
# 必要減少 = 基準値 × 腐敗_必要減少率 ÷ 100 (切り上げ)
#   回避できてしまう。腐敗で持っていかれる量以上を使った時だけ
#   「使った」と認めることで、逃げても得にならなくなる。
#   腐敗_必要減少率 を 腐敗_減少率 と同じ値にしておくと、
#   「腐敗で取られるより多く使えば腐らない」の1行で説明できる。
scoreboard players operation #hitsuyo sagyou = #kijun sagyou
scoreboard players operation #hitsuyo sagyou *= 腐敗_必要減少率 settei
# 切り上げ: 99 を足してから100で割る (1本でも使うなら1本を要求する)
scoreboard players add #hitsuyo sagyou 99
scoreboard players operation #hitsuyo sagyou /= #hyaku sagyou

# --- 2) 基準値からどれだけ減ったか ----------------------------
scoreboard players operation #sa sagyou = #kijun sagyou
scoreboard players operation #sa sagyou -= #gokei sagyou

# --- 3) 十分に減っていたら、時計を0に戻して終わり -------------
# 石油を持っていない相手は 必要減少=0 になるので、ここで必ず抜ける。
execute if score #sa sagyou >= #hitsuyo sagyou run scoreboard players set #byou sagyou 0
execute if score #sa sagyou >= #hitsuyo sagyou run scoreboard players operation #kijun sagyou = #gokei sagyou
execute if score #sa sagyou >= #hitsuyo sagyou run return 0

# --- 4) 増えていたら、基準値だけ上げる ------------------------
# ★時計は止めない。溜め込みを罰するのが目的なので、
#   増えただけでは「使った」ことにならない。
execute if score #gokei sagyou > #kijun sagyou run scoreboard players operation #kijun sagyou = #gokei sagyou

# --- 5) 時計を進める ------------------------------------------
# ★この 10 は jidai:fuhai/hantei が10秒ごとに呼ばれることに対応する。
#   間隔を変えたら、ここの数字も変える。
scoreboard players add #byou sagyou 10

# 猶予に達していなければ、まだ腐らない
execute unless score #byou sagyou >= #yuyo sagyou run return 0

# --- 6) 腐る --------------------------------------------------
# 減る本数 = 総量 × 腐敗_減少率 ÷ 100 (切り上げ)
# ★固定本数ではなく割合にした理由:
# ★切り上げにした理由:
#   腐らない」という抜け穴が復活する。
scoreboard players operation #heru sagyou = #gokei sagyou
scoreboard players operation #heru sagyou *= 腐敗_減少率 settei
scoreboard players add #heru sagyou 99
scoreboard players operation #heru sagyou /= #hyaku sagyou

# 次の猶予は「腐った後の値」を基準にする
scoreboard players set #byou sagyou 0
scoreboard players operation #kijun sagyou = #gokei sagyou
scoreboard players operation #kijun sagyou -= #heru sagyou
