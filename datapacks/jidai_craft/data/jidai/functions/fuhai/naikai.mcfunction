# =============================================================
# jidai:fuhai/naikai ── 内海の石油の腐敗
#   勢力名とチーム名だけを置いて、共通の判定へ渡す。
#   勢力を増やす時は、このファイルを複製して
#   「内海」と「naikai」を書き換えるだけでよい。
# =============================================================

# --- 1) 今の総量を数える --------------------------------------
# スコアの合計に、引き出して手に持っている分を足す。
# ★sekiyu_gokei は【オンラインの人だけ】の合計。
#   誰かがログアウトすると総量が減り、時計が戻る (既知の抜け道)。
scoreboard players operation #gokei sagyou = 内海 sekiyu_gokei
execute as @a[team=naikai] run function jidai:fuhai/mochi

# --- 2) 基準値・経過秒・猶予を作業用へ写す --------------------
scoreboard players operation #kijun sagyou = 内海 fuhai_kijun
scoreboard players operation #byou sagyou = 内海 fuhai_byou
scoreboard players operation #yuyo sagyou = 腐敗_猶予秒 settei

# --- 3) 共通の判定 (#heru に減らす本数が入る) -----------------
function jidai:fuhai/kyotsu

# --- 4) 結果を内海へ書き戻す ----------------------------------
scoreboard players operation 内海 fuhai_kijun = #kijun sagyou
scoreboard players operation 内海 fuhai_byou = #byou sagyou

# 腐らないならここで終わり
execute if score #heru sagyou matches ..0 run return 0

# --- 5) 最も多く持っている個人から引く ------------------------
# ★溜め込んだ本人が罰を受ける形にする。
#   バニラには「スコア順に並べる」機能が無いので、
#   一度なめて最大値を求め、その値と一致する人を1人だけ選ぶ。
#   ( > は「大きい方を入れる」= 最大値を求める書き方)
scoreboard players set #max sagyou 0
execute as @a[team=naikai] run scoreboard players operation #max sagyou > @s sekiyu

# 誰も持っていなければ引けない (全員オフラインの時など)。
# 手持ちのアイテム分も総量に入っているので、スコアより多く引かない。
# ( < は「小さい方を入れる」= 上限で頭打ちにする書き方)
scoreboard players operation #heru sagyou < #max sagyou
execute if score #heru sagyou matches ..0 run return 0

# 同点が居ても1人だけにするため、#zumi で見張る
scoreboard players set #zumi sagyou 0
execute as @a[team=naikai] if score @s sekiyu = #max sagyou run function jidai:fuhai/hiku_hitori

tellraw @a [{"text":"[腐敗] ","color":"dark_red","bold":true},{"text":"内海 の石油が腐った (","color":"white"},{"score":{"name":"#heru","objective":"sagyou"},"color":"red"},{"text":" 本)","color":"white"}]
