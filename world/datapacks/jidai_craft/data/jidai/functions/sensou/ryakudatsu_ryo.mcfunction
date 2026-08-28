# =============================================================
# jidai:sensou/ryakudatsu_ryo ── 今回の略奪で動かせる量を決める
#   入力  #bai       … 上限の倍率(ふつう1 / 下剋上で始めた戦争は2)
#         #r_kane    … この戦争でこれまでに奪った金の累計
#         #r_sekiyu  … 同じく石油の累計
#         #aite_kane … 相手の貯金
#   出力  #gaku … 動かす金の額   #hon … 動かす石油の本数
# =============================================================
# ★別の関数に切り出してあるのは、上限の効き方を実測できるようにするため。
#   tests/verify_jidai.py がここへ直接いろいろな値を入れて確かめている。

# --- 金 -------------------------------------------------------
# 「1回の額」「上限までの残り」「相手の貯金」の、いちばん小さい値まで。
# ( < は「小さい方を入れる」= 上限で頭打ちにする書き方)
scoreboard players operation #jougen sagyou = 略奪_上限金 settei
scoreboard players operation #jougen sagyou *= #bai sagyou
scoreboard players operation #jougen sagyou -= #r_kane sagyou
scoreboard players operation #gaku sagyou = 略奪_金 settei
scoreboard players operation #gaku sagyou < #jougen sagyou
scoreboard players operation #gaku sagyou < #aite_kane sagyou
execute if score #gaku sagyou matches ..0 run scoreboard players set #gaku sagyou 0

# --- 石油 -----------------------------------------------------
# 上限までの残りまで。相手が実際に持っているかは奪う側で見る。
# ★上限10本 ÷ 1回2本 = 5回で打ち止め。
#   「5回目以降は金のみ」は上限から自然に出るので、
#   回数を数える仕組みは要らない。
scoreboard players operation #jougen sagyou = 略奪_上限石油 settei
scoreboard players operation #jougen sagyou *= #bai sagyou
scoreboard players operation #jougen sagyou -= #r_sekiyu sagyou
scoreboard players operation #hon sagyou = 略奪_石油 settei
scoreboard players operation #hon sagyou < #jougen sagyou
execute if score #hon sagyou matches ..0 run scoreboard players set #hon sagyou 0
