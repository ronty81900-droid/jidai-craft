# =============================================================
# jidai:fuhai/youhei ── 傭兵1人ぶんの腐敗
#   傭兵(youhei=1)として走る。
#   傭兵は1人を1つの単位として、勢力と同じ論理を使う。
# =============================================================
# ★傭兵も腐らせる理由 (指示書 §8):
#   1. 腐らないと、勢力が傭兵に預けて時間を稼げてしまう。
#      勢力単位の判定に穴があく (最強の貯蔵庫になる)
#   2. 傭兵は石油を換金できないので、溜め込めば減るだけになる。
#      使うか売るかしかなくなり、動く動機になる
#
# ★規模が違う(勢力は50〜100本・傭兵は数本)ので、
#   減り方は割合、猶予は別の設定(既定で勢力の2倍)にしてある。


# --- 1) 今の総量 = 自分のスコア + 手に持っている分 ------------
scoreboard players operation #gokei sagyou = @s sekiyu
function jidai:fuhai/mochi


# --- 2) 基準値・経過秒・猶予を作業用へ写す --------------------
scoreboard players operation #kijun sagyou = @s fuhai_kijun
scoreboard players operation #byou sagyou = @s fuhai_byou
scoreboard players operation #yuyo sagyou = 腐敗_傭兵猶予秒 settei


# --- 3) 共通の判定 (#heru に減らす本数が入る) -----------------
function jidai:fuhai/kyotsu


# --- 4) 結果を本人へ書き戻す ----------------------------------
scoreboard players operation @s fuhai_kijun = #kijun sagyou
scoreboard players operation @s fuhai_byou = #byou sagyou


# --- 5) 腐るなら自分から引く ----------------------------------
# 手持ちのアイテム分も総量に入っているので、スコアより多くは引かない
# ( < は「小さい方を入れる」= 上限で頭打ちにする書き方)
scoreboard players operation #heru sagyou < @s sekiyu
execute if score #heru sagyou matches ..0 run return 0

scoreboard players operation @s sekiyu -= #heru sagyou

tellraw @s [{"text":"[腐敗] ","color":"dark_red"},{"text":"溜め込んだ石油が ","color":"gray"},{"score":{"name":"#heru","objective":"sagyou"},"color":"red"},{"text":" 本 腐った (残り ","color":"gray"},{"score":{"name":"@s","objective":"sekiyu"},"color":"white"},{"text":" 本)","color":"gray"}]
tellraw @s [{"text":"  石油は腐る。U キーの蛮族の店「売る」で金に換えること (勢力より高く売れる)","color":"dark_gray"}]
playsound minecraft:block.fire.extinguish player @s
