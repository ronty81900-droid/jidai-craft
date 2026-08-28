# =============================================================
# jidai:sensou/ryakudatsu ── 略奪する
#   相手勢力の拠点の金ブロックを押した本人として走る(@s = その人)。
#   jidai:kane/azukeru から、預金ではなく略奪として分岐して呼ばれる。
#
#   ★ 呼ぶ前に置いておくもの
#     @s bangou      … 押した人の勢力の番号（jidai:clock が毎秒入れている）
#     #aite_no sagyou … その拠点を持つ勢力の番号（jidai:sensou/aite_yomu が入れる）
#
#   ★ 1.21 ではマクロで勢力名を受けていたが、1.20.1 には無い。番号だけで足りる。
# =============================================================
# 略奪は双方向。宣戦された側も、相手の銀行を叩ける。

# --- 1) この2勢力の戦争を探す --------------------------------
# ★ マーカー1体が戦争1件。自分と相手の番号が両方入っているものを探す。
#   見つけたら、その状態と倍率と累計を作業用へ写す。
#   （マーカーのスコアは、そのマーカーとしてでないと読めないため）
scoreboard players set #w_mitsuke sagyou 0
scoreboard players set #w_jotai sagyou 0
scoreboard players set #bai sagyou 1
scoreboard players set #r_kane sagyou 0
scoreboard players set #r_sekiyu sagyou 0
# 自分が a側 か b側 かで、見る累計の向きが変わる
scoreboard players set #jibun_a sagyou 0

execute as @e[type=marker,tag=jidai_sensou] if score @s w_kuni = #jibun_no sagyou if score @s w_aite = #aite_no sagyou run function jidai:sensou/ryaku_yomu_a
execute as @e[type=marker,tag=jidai_sensou] if score @s w_aite = #jibun_no sagyou if score @s w_kuni = #aite_no sagyou run function jidai:sensou/ryaku_yomu_b

# --- 2) 交戦中でなければ何も起きない --------------------------
execute if score #w_jotai sagyou matches 1 run tellraw @s [{"text":"[略奪] ","color":"red"},{"text":"まだ準備中。交戦が始まるまで略奪できない","color":"gray"}]
execute if score #w_jotai sagyou matches 3 run tellraw @s [{"text":"[略奪] ","color":"red"},{"text":"この相手との交戦はもう終わっている","color":"gray"}]
execute if score #w_mitsuke sagyou matches 0 run tellraw @s [{"text":"[銀行] ","color":"aqua"},{"text":"ここは他の勢力の銀行。戦争していないので何もできない","color":"red"}]
execute unless score #w_jotai sagyou matches 2 run playsound minecraft:entity.villager.no player @s
execute unless score #w_jotai sagyou matches 2 run return 0

# --- 3) 押した本人ごとのクールダウン --------------------------
# 連打で一気に上限まで持っていけないようにする。
execute if score @s ryakudatsu_kan matches 1.. run tellraw @s [{"text":"[略奪] ","color":"red"},{"text":"まだ手が離せない (あと ","color":"gray"},{"score":{"name":"@s","objective":"ryakudatsu_kan"},"color":"white"},{"text":" 秒)","color":"gray"}]
execute if score @s ryakudatsu_kan matches 1.. run return 0

# --- 4) 今回いくら動かせるかを決める --------------------------
# 上限は【1戦争・1勢力あたり】。下剋上で始めた戦争は倍率2になる。
# 計算そのものは jidai:sensou/ryakudatsu_ryo に切り出してある。
scoreboard players set #aite_kane sagyou 0
execute if score #aite_no sagyou matches 1 run scoreboard players operation #aite_kane sagyou = 丘陵 chokin
execute if score #aite_no sagyou matches 2 run scoreboard players operation #aite_kane sagyou = 森林 chokin
execute if score #aite_no sagyou matches 3 run scoreboard players operation #aite_kane sagyou = 川 chokin
execute if score #aite_no sagyou matches 4 run scoreboard players operation #aite_kane sagyou = 内海 chokin
execute if score #aite_no sagyou matches 5 run scoreboard players operation #aite_kane sagyou = 岩場 chokin
function jidai:sensou/ryakudatsu_ryo

# どちらも動かせないなら、押しても何も起きない
execute if score #gaku sagyou matches ..0 if score #hon sagyou matches ..0 run tellraw @s [{"text":"[略奪] ","color":"red"},{"text":"これ以上は奪えない (上限に達したか、相手が空)","color":"gray"}]
execute if score #gaku sagyou matches ..0 if score #hon sagyou matches ..0 run playsound minecraft:entity.villager.no player @s
execute if score #gaku sagyou matches ..0 if score #hon sagyou matches ..0 run return 0

# --- 5) 金を移す ----------------------------------------------
# 相手から引いて、自分の勢力へ足す。どちらも番号で場合分けする。
scoreboard players operation #mae sagyou = #aite_kane sagyou
execute if score #aite_no sagyou matches 1 run scoreboard players operation 丘陵 chokin -= #gaku sagyou
execute if score #aite_no sagyou matches 2 run scoreboard players operation 森林 chokin -= #gaku sagyou
execute if score #aite_no sagyou matches 3 run scoreboard players operation 川 chokin -= #gaku sagyou
execute if score #aite_no sagyou matches 4 run scoreboard players operation 内海 chokin -= #gaku sagyou
execute if score #aite_no sagyou matches 5 run scoreboard players operation 岩場 chokin -= #gaku sagyou
execute if score #jibun_no sagyou matches 1 run scoreboard players operation 丘陵 chokin += #gaku sagyou
execute if score #jibun_no sagyou matches 2 run scoreboard players operation 森林 chokin += #gaku sagyou
execute if score #jibun_no sagyou matches 3 run scoreboard players operation 川 chokin += #gaku sagyou
execute if score #jibun_no sagyou matches 4 run scoreboard players operation 内海 chokin += #gaku sagyou
execute if score #jibun_no sagyou matches 5 run scoreboard players operation 岩場 chokin += #gaku sagyou

# --- 6) 石油を移す --------------------------------------------
# ★石油は【個人スコア】。勢力の合計は毎秒作り直される派生値なので、
#   そこから引いても1秒で戻ってしまう。
#   相手勢力で最も多く持っている個人から奪い、押した本人が受け取る。
tag @a remove jidai_ubawareta
execute if score #hon sagyou matches 1.. run function jidai:sensou/sekiyu_ubau
scoreboard players operation @s sekiyu += #hon sagyou

# --- 7) 奪った累計をマーカーへ書き戻す ------------------------
# ★ 自分が a側 か b側 かで、書き込む先が変わる。
execute as @e[type=marker,tag=jidai_sensou] if score @s w_kuni = #jibun_no sagyou if score @s w_aite = #aite_no sagyou run function jidai:sensou/ryaku_kaku_a
execute as @e[type=marker,tag=jidai_sensou] if score @s w_aite = #jibun_no sagyou if score @s w_kuni = #aite_no sagyou run function jidai:sensou/ryaku_kaku_b

# --- 8) クールダウンを置く ------------------------------------
scoreboard players operation @s ryakudatsu_kan = 略奪_間隔秒 settei

# 通知に出す「奪ったあとの相手の貯金」を控えておく
scoreboard players set #ato sagyou 0
execute if score #aite_no sagyou matches 1 run scoreboard players operation #ato sagyou = 丘陵 chokin
execute if score #aite_no sagyou matches 2 run scoreboard players operation #ato sagyou = 森林 chokin
execute if score #aite_no sagyou matches 3 run scoreboard players operation #ato sagyou = 川 chokin
execute if score #aite_no sagyou matches 4 run scoreboard players operation #ato sagyou = 内海 chokin
execute if score #aite_no sagyou matches 5 run scoreboard players operation #ato sagyou = 岩場 chokin

# --- 9) 全体へ事実を出す --------------------------------------
# ★石油を奪った時は【誰から奪ったか】を必ず名指しで出す。
#   石油を個人のスコアにした設計が効くのはここ。溜め込んでいた人が
#   全員の前で晒され、次の標的になる。溜め込みへの一番強い圧力。
scoreboard players operation #mei_no sagyou = #aite_no sagyou
function jidai:sensou/mei
execute if score #hon sagyou matches 1.. run tellraw @a [{"text":"[略奪] ","color":"dark_red","bold":true},{"selector":"@s"},{"text":" が ","color":"white"},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" から 金","color":"white"},{"score":{"name":"#gaku","objective":"sagyou"},"color":"yellow"},{"text":" 石油","color":"white"},{"score":{"name":"#hon","objective":"sagyou"},"color":"light_purple"},{"text":" を奪った (石油は ","color":"gray"},{"selector":"@a[tag=jidai_ubawareta]","color":"white"},{"text":" から / 相手の貯金 ","color":"gray"},{"score":{"name":"#mae","objective":"sagyou"},"color":"gray"},{"text":" → ","color":"gray"},{"score":{"name":"#ato","objective":"sagyou"},"color":"gray"},{"text":")","color":"gray"}]
execute if score #hon sagyou matches ..0 run tellraw @a [{"text":"[略奪] ","color":"dark_red","bold":true},{"selector":"@s"},{"text":" が ","color":"white"},{"storage":"jidai:kari","nbt":"mei","color":"white"},{"text":" から 金","color":"white"},{"score":{"name":"#gaku","objective":"sagyou"},"color":"yellow"},{"text":" を奪った (相手の貯金 ","color":"gray"},{"score":{"name":"#mae","objective":"sagyou"},"color":"gray"},{"text":" → ","color":"gray"},{"score":{"name":"#ato","objective":"sagyou"},"color":"gray"},{"text":")","color":"gray"}]

tag @a remove jidai_ubawareta
playsound minecraft:entity.player.attack.crit player @s
