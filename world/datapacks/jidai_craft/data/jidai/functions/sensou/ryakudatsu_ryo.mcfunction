# =============================================================
# jidai:sensou/ryakudatsu_ryo ── 今回の略奪で動かす量を決める
#   入力  #bai         … 倍率(ふつう1 / 下剋上で始めた戦争は2)
#         #aite_kane   … 相手勢力の貯金
#         #aite_sekiyu … 相手勢力の石油の合計
#   出力  #gaku … 動かす金の額   #hon … 動かす石油の本数
#
# ★★ 2026-09-09 のご指示で作り直した ★★
#   前: 1回で決まった額(金30・石油2本)。1戦争あたりの上限まで。
#   今: **相手の総量の 1%**。上限は無い。だから相手が多く持っているほど
#       1回の被害が大きく、減るほど1回の被害も小さくなる。
#       100回 壊しても 0.99^100 ＝ 約37% は残る。**空にはならない**。
#       止めを刺すのはビーコンの方（占領_必要回数）。
#
# ★ 割り算にしているのは、1.20.1 のスコアボードに小数が無いため。
#   略奪_割る数 が 100 なら 1%。50 にすれば 2%。
#   倍率は先に掛ける（下剋上で始めた戦争は 2%）。
#
# ★ 最低1を入れている理由:
#   相手の貯金が 100 未満だと 1% が 0 になり、いくら壊しても何も動かない。
#   相手が1でも持っている限り、必ず1は動く形にする。
# =============================================================
# ★別の関数に切り出してあるのは、効き方を実測できるようにするため。
#   tests/verify_jidai.py がここへ直接いろいろな値を入れて確かめている。

# --- 金 -------------------------------------------------------
scoreboard players operation #gaku sagyou = #aite_kane sagyou
scoreboard players operation #gaku sagyou *= #bai sagyou
scoreboard players operation #gaku sagyou /= 略奪_割る数 settei
# 相手が持っているのに 0 になったら 1 にする
execute if score #aite_kane sagyou matches 1.. if score #gaku sagyou matches ..0 run scoreboard players set #gaku sagyou 1
# 相手の持ち以上は動かせない ( < は「小さい方を入れる」)
scoreboard players operation #gaku sagyou < #aite_kane sagyou
execute if score #gaku sagyou matches ..0 run scoreboard players set #gaku sagyou 0

# --- 石油 -----------------------------------------------------
# ★ #aite_sekiyu は【勢力の合計】(sekiyu_gokei)。1秒に1回 作り直される派生値なので、
#   最大1秒 古いことがある。1% の計算なら誤差は1本未満で影響しない。
scoreboard players operation #hon sagyou = #aite_sekiyu sagyou
scoreboard players operation #hon sagyou *= #bai sagyou
scoreboard players operation #hon sagyou /= 略奪_割る数 settei
execute if score #aite_sekiyu sagyou matches 1.. if score #hon sagyou matches ..0 run scoreboard players set #hon sagyou 1
scoreboard players operation #hon sagyou < #aite_sekiyu sagyou
execute if score #hon sagyou matches ..0 run scoreboard players set #hon sagyou 0
