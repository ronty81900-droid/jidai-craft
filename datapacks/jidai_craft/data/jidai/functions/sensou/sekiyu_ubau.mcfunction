# =============================================================
# jidai:sensou/sekiyu_ubau ── 相手勢力から石油を奪う
#   入力  #aite_no … 奪われる勢力の番号
#         #hon     … 奪いたい本数
#   出力  #hon     … 実際に奪えた本数(相手が少なければ減る)
# =============================================================
# ★最も多く持っている個人から奪う。腐敗と同じ考え方で、
#   溜め込んでいる人が狙われる。
#
# ★勢力名ではなく【番号】で相手を選んでいるのは、マクロを使わずに
#   セレクタを書くため。bangou は jidai:clock が毎秒、
#
# ★オンラインの人からしか奪えない(@a はオンラインだけ)。
#   落ちている人の石油には手が届かない。既知の制約。

# --- 相手勢力で最も多く持っている量を求める -------------------
# ( > は「大きい方を入れる」= 最大値を求める書き方)
scoreboard players set #max sagyou 0
execute as @a if score @s bangou = #aite_no sagyou run scoreboard players operation #max sagyou > @s sekiyu

# --- 持っている以上は奪えない ---------------------------------
# ( < は「小さい方を入れる」= 上限で頭打ちにする書き方)
scoreboard players operation #hon sagyou < #max sagyou
execute if score #hon sagyou matches ..0 run scoreboard players set #hon sagyou 0
execute if score #hon sagyou matches ..0 run return 0

# --- 1人だけから引く ------------------------------------------
# 同点が居ると全員から引いてしまうので、#zumi で見張る
scoreboard players set #zumi sagyou 0
execute as @a if score @s bangou = #aite_no sagyou if score @s sekiyu = #max sagyou run function jidai:sensou/ubau_hitori
