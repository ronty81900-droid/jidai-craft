# =============================================================
# jidai:sensou/shouri ── 戦争勝利になったかを調べる
#
#   入力  #s_kuni sagyou … 今 拠点を落とした勢力の番号 1〜5
#
#   ★★ 勝利条件その1「戦争勝利」 ★★
#     自勢力を除く4勢力すべてを植民地にしたら勝ち。
#     植民地の印は shokuminchi（保持者＝勢力名 / 値＝宗主国の番号）。
#
#   ★ 呼ばれるのは jidai:sensou/shokuminchi の最後だけ。
#     毎秒 数えると、5勢力ぶんの比較が常時 走ることになる。
#     拠点が落ちた時にだけ数えれば足りる。
# =============================================================

# --- 自分が宗主国になっている勢力を数える ---------------------
scoreboard players set #shoku sagyou 0
execute if score 丘陵 shokuminchi = #s_kuni sagyou run scoreboard players add #shoku sagyou 1
execute if score 森林 shokuminchi = #s_kuni sagyou run scoreboard players add #shoku sagyou 1
execute if score 川 shokuminchi = #s_kuni sagyou run scoreboard players add #shoku sagyou 1
execute if score 内海 shokuminchi = #s_kuni sagyou run scoreboard players add #shoku sagyou 1
execute if score 岩場 shokuminchi = #s_kuni sagyou run scoreboard players add #shoku sagyou 1

# --- あと何勢力かを知らせる（4に届くまで） --------------------
# ★ 進み具合が見えないと、あと1つなのか3つなのかが分からず
#   終盤の緊張が生まれない。落ちるたびに全員へ出す。
scoreboard players operation #nokori sagyou = #yon sagyou
scoreboard players operation #nokori sagyou -= #shoku sagyou
execute if score #shoku sagyou matches ..3 run tellraw @a [{"text":"       あと ","color":"gray"},{"score":{"name":"#nokori","objective":"sagyou"},"color":"yellow"},{"text":" 勢力で戦争勝利","color":"gray"}]

# --- 4勢力そろったら勝ち --------------------------------------
scoreboard players set #shouri_shu sagyou 1
execute if score #shoku sagyou matches 4.. run function jidai:sensou/shouri_kakutei
