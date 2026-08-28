# =============================================================
# jidai:sensou/keizai ── 経済勝利になったかを調べる
#   jidai:clock から毎秒 呼ばれる。
#
#   ★★ 勝利条件その3「経済勝利」 ★★
#     勢力の貯金が 経済_勝利_貯金（既定 500000）に達したら勝ち。
#
#   ★ 毎秒 5回の比較しかしていない。
#     貯金は略奪でも時代進行でも動くので、
#     「動いた時にだけ調べる」形にすると呼び出し元が5か所に散る。
#     毎秒 数える方が、抜けが起きない。
# =============================================================

# 既に誰かが勝っていれば、何もしない
execute unless score 世界 shouri matches 0 run return 0

scoreboard players set #shouri_shu sagyou 3

execute if score 丘陵 chokin >= 経済_勝利_貯金 settei run scoreboard players set #s_kuni sagyou 1
execute if score 丘陵 chokin >= 経済_勝利_貯金 settei run function jidai:sensou/shouri_kakutei
execute if score 森林 chokin >= 経済_勝利_貯金 settei run scoreboard players set #s_kuni sagyou 2
execute if score 森林 chokin >= 経済_勝利_貯金 settei run function jidai:sensou/shouri_kakutei
execute if score 川 chokin >= 経済_勝利_貯金 settei run scoreboard players set #s_kuni sagyou 3
execute if score 川 chokin >= 経済_勝利_貯金 settei run function jidai:sensou/shouri_kakutei
execute if score 内海 chokin >= 経済_勝利_貯金 settei run scoreboard players set #s_kuni sagyou 4
execute if score 内海 chokin >= 経済_勝利_貯金 settei run function jidai:sensou/shouri_kakutei
execute if score 岩場 chokin >= 経済_勝利_貯金 settei run scoreboard players set #s_kuni sagyou 5
execute if score 岩場 chokin >= 経済_勝利_貯金 settei run function jidai:sensou/shouri_kakutei
