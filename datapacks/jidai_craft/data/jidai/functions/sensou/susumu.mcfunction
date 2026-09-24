# =============================================================
# jidai:sensou/susumu ── 戦争の時計を1秒ぶん進める
#   jidai:clock から【戦争のマーカーとして】呼ばれる(@s = そのマーカー)。
#     execute as @e[type=marker,tag=jidai_sensou] run function jidai:sensou/susumu
#
#   状態は3つ。順に移っていき、最後は関係なしに戻る。
#     1 = 準備     … 宣戦から5分。まだ略奪できない
#     2 = 交戦     … 10分。相手拠点の金ブロックで略奪できる
#     3 = 再戦禁止 … 30分。同じ組み合わせでは宣戦できない
# =============================================================
# ★ 1.21 では保持者 "丘陵>森林" のスコアを読み書きしていたが、
#   1.20.1 にマクロが無いのでその名前を作れない。
#   マーカー自身のスコアに持たせる形へ変えた。
#   おかげで storage への写し(旧 susumu_1)も要らなくなった。

# --- 1) 残り秒を1つ減らす ------------------------------------
scoreboard players remove @s sensou_byou 1

# まだ時間が残っていれば、ここで終わり
execute if score @s sensou_byou matches 1.. run return 0

# --- 2) 今の状態を読む ---------------------------------------
scoreboard players operation #jotai sagyou = @s sensou

# 次の状態と秒数。何も当てはまらなければ「関係なし」に落とす
scoreboard players set #tsugi sagyou 0
scoreboard players set #byou sagyou 0

# 通知に名前を出すため、関わっている2勢力を控える
scoreboard players operation #mei_no sagyou = @s w_kuni
scoreboard players operation #mei_no2 sagyou = @s w_aite
function jidai:sensou/mei
function jidai:sensou/mei2

# --- 3) 準備 → 交戦 ------------------------------------------
execute if score #jotai sagyou matches 1 run scoreboard players set #tsugi sagyou 2
execute if score #jotai sagyou matches 1 run scoreboard players operation #byou sagyou = 戦争_交戦秒 settei
execute if score #jotai sagyou matches 1 run tellraw @a [{"text":"[交戦] ","color":"red","bold":true},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" と ","color":"white"},{"storage":"jidai:kari","nbt":"mei2","color":"white"},{"text":" の交戦が始まった。相手拠点の金ブロックを押すと略奪できる","color":"white"}]

# --- 4) 交戦 → 再戦禁止。ここで占領が決まる ------------------
# ★交戦が終わった時点で貯金が0の勢力は、相手に占領される。
#   占領そのものに不利益は無い(貢納も制限も付けない)。状態と表示だけ。
#   時間では解けない。解除は【下剋上】を行使した時だけ。
#
# ★★ 一度も略奪していない相手は占領できない ★★
#   これが無いと、時代進行で貯金を使った直後に、指一本触れていない
#   相手から占領されてしまう。「殴った者だけが取れる」ようにしてある。
execute if score #jotai sagyou matches 2 run scoreboard players set #tsugi sagyou 3
execute if score #jotai sagyou matches 2 run scoreboard players operation #byou sagyou = 戦争_禁止秒 settei
execute if score #jotai sagyou matches 2 run tellraw @a [{"text":"[交戦終了] ","color":"gold","bold":true},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" と ","color":"white"},{"storage":"jidai:kari","nbt":"mei2","color":"white"},{"text":" の交戦が終わった。30分は再戦できない","color":"white"}]

# それぞれが「相手からいくら奪ったか」。金と石油を足して1以上なら略奪あり。
scoreboard players operation #ryaku_a sagyou = @s ryakudatsu_kane_a
scoreboard players operation #ryaku_a sagyou += @s ryakudatsu_sekiyu_a
scoreboard players operation #ryaku_b sagyou = @s ryakudatsu_kane_b
scoreboard players operation #ryaku_b sagyou += @s ryakudatsu_sekiyu_b

# ★★ 2026-09-09: 「交戦終了時に貯金が0なら占領」は廃止した ★★
#   占領の入口は【ビーコンを壊す】1本だけにする（ご指示）。
#   1回1%の設計では貯金は 0 にならない（100回でも約37%残る）ので、
#   この判定はどのみちほぼ発動しなかった。
#   判定の中身だった jidai:sensou/senryou_hantei は消してある。

# --- 5) 再戦禁止 → 関係なし ----------------------------------
execute if score #jotai sagyou matches 3 run tellraw @a [{"text":"[戦争] ","color":"gray"},{"storage":"jidai:kari","nbt":"mei","color":"gray"},{"text":" と ","color":"gray"},{"storage":"jidai:kari","nbt":"mei2","color":"gray"},{"text":" は再び宣戦できるようになった","color":"gray"}]

# --- 6) 新しい状態を書く -------------------------------------
scoreboard players operation @s sensou = #tsugi sagyou
scoreboard players operation @s sensou_byou = #byou sagyou

# --- 7) 戦争が終わったらマーカーを消す ------------------------
# マーカーが消えると時計も回らなくなる。これが戦争の終わり。
execute if score #tsugi sagyou matches 0 run kill @s
