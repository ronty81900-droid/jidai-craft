# =============================================================
# jidai:sekiyu/azukeru ── 手持ちの石油を銀行へ預ける
#   銀行の画面から、預けた本人として呼ばれる。
#     execute as <名前> run function jidai:sekiyu/azukeru
#
#   ★★ 2026-08-20 に、石油の扱いを変えた ★★
#     前 … 拾った瞬間にスコアへ入り、アイテムは消えていた
#     今 … 拾ってもアイテムのまま。銀行で預けて初めて数に入る
#
#   持ち歩いている石油 … 死ねば落ちる。他人に直接渡せる。
#                        略奪と徴収の対象にならない
#   預けた石油         … 死んでも減らない。時代進行の条件に入る。
#                        略奪と徴収の対象になる
#   どちらで持つかを選べるようにするのが、この変更の狙い。
#
#   ★ 勢力ごとに【獲得の上限】がある。
#     上限 = 今の時代を進めるのに要る石油 × 1.2 (jidai:sekiyu/jougen)
#     上限を超える分は数に入らないので、手元へ返す。
# =============================================================

# --- 1) 自分の勢力の「今の時代」と「今の合計」を取る ----------
# ★★ 手持ちを触る前に、上限を先に見る ★★
#   上限に達している時に消してから返すと、
#   返す途中で持ち物がいっぱいだった人の石油が地面に落ちる。
#   先に断れば、そもそも手を触れずに済む。
scoreboard players set #kuni_jidai sagyou 0
scoreboard players set #ima sagyou 0
scoreboard players set #jougen sagyou 1000000

execute if entity @s[team=kyuryo] run scoreboard players operation #kuni_jidai sagyou = 丘陵 jidai
execute if entity @s[team=kyuryo] run scoreboard players operation #ima sagyou = 丘陵 sekiyu_gokei
execute if entity @s[team=shinrin] run scoreboard players operation #kuni_jidai sagyou = 森林 jidai
execute if entity @s[team=shinrin] run scoreboard players operation #ima sagyou = 森林 sekiyu_gokei
execute if entity @s[team=kawa] run scoreboard players operation #kuni_jidai sagyou = 川 jidai
execute if entity @s[team=kawa] run scoreboard players operation #ima sagyou = 川 sekiyu_gokei
execute if entity @s[team=naikai] run scoreboard players operation #kuni_jidai sagyou = 内海 jidai
execute if entity @s[team=naikai] run scoreboard players operation #ima sagyou = 内海 sekiyu_gokei
execute if entity @s[team=iwaba] run scoreboard players operation #kuni_jidai sagyou = 岩場 jidai
execute if entity @s[team=iwaba] run scoreboard players operation #ima sagyou = 岩場 sekiyu_gokei

# 勢力に属している人だけ、上限を計算する
execute if score #kuni_jidai sagyou matches 1.. run function jidai:sekiyu/jougen

# --- 2) 自分の勢力が先行しているかを見る ----------------------
# ★ 中央より先へ進んでいる勢力は、中央産の石油が目減りする。
#   以前は「拾った時」に引いていたが、拾っても数に入らなくなったので
#   預けた時へ移した。掛かる本数は変わらない。
# ★★ 判定は【いちばん先】に置く ★★
#   上限で断る手前に置かないと、断られた人は #ritsu が前の値のまま残り、
#   次に見た時に他人の判定を拾ってしまう（実測でそうなった）。
scoreboard players set #ritsu sagyou 0
execute if entity @s[team=kyuryo] if score 丘陵 senkou matches 1 run scoreboard players operation #ritsu sagyou = 先行_ロス率 settei
execute if entity @s[team=shinrin] if score 森林 senkou matches 1 run scoreboard players operation #ritsu sagyou = 先行_ロス率 settei
execute if entity @s[team=kawa] if score 川 senkou matches 1 run scoreboard players operation #ritsu sagyou = 先行_ロス率 settei
execute if entity @s[team=naikai] if score 内海 senkou matches 1 run scoreboard players operation #ritsu sagyou = 先行_ロス率 settei
execute if entity @s[team=iwaba] if score 岩場 senkou matches 1 run scoreboard players operation #ritsu sagyou = 先行_ロス率 settei

# --- 3) 上限までの余裕。無ければ、手を触れずに断る ------------
scoreboard players operation #yoyuu sagyou = #jougen sagyou
scoreboard players operation #yoyuu sagyou -= #ima sagyou

execute if score #yoyuu sagyou matches ..0 run tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"text":"勢力の石油が上限に達しています (上限 ","color":"red"},{"score":{"name":"#jougen","objective":"sagyou"},"color":"yellow"},{"text":" 本)。これ以上は預けても数に入りません","color":"red"}]
execute if score #yoyuu sagyou matches ..0 run tellraw @s [{"text":"  時代を進めると上限も上がります","color":"dark_gray"}]
execute if score #yoyuu sagyou matches ..0 run playsound minecraft:entity.villager.no player @s
execute if score #yoyuu sagyou matches ..0 run return 0

# --- 4) 手持ちを全部 消して数える ------------------------------
# ★ 中央プラント産には moto:"chuo" が付いている(jidai:sekiyu/waku で付ける)。
#   先行ペナルティの対象はこれだけなので、先に分けて数える。
# ★ 個数を指定していないので、どちらも「全部 消して、消した数を返す」。
execute store result score #hon_chuo sagyou run clear @s minecraft:black_dye{jidai_sekiyu:"oil",moto:"chuo"}
execute store result score #hon_hoka sagyou run clear @s minecraft:black_dye{jidai_sekiyu:"oil"}

# --- 5) 何も持っていなければ、ここで終わり --------------------
scoreboard players operation #mochi sagyou = #hon_chuo sagyou
scoreboard players operation #mochi sagyou += #hon_hoka sagyou
execute if score #mochi sagyou matches ..0 run tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"text":"預ける石油を持っていません","color":"red"}]
execute if score #mochi sagyou matches ..0 run playsound minecraft:entity.villager.no player @s
execute if score #mochi sagyou matches ..0 run return 0

# --- 6) 先行ペナルティの本数を出す ----------------------------
function jidai:senkou/loss

# --- 7) 数に入る本数を決める ----------------------------------
# 実質 = 消した全部 - ペナルティ
scoreboard players operation #jisshitsu sagyou = #hon_chuo sagyou
scoreboard players operation #jisshitsu sagyou += #hon_hoka sagyou
scoreboard players operation #jisshitsu sagyou -= #hiku sagyou

# 入る = 実質 と 余裕 の少ない方
scoreboard players operation #haitta sagyou = #jisshitsu sagyou
execute if score #haitta sagyou > #yoyuu sagyou run scoreboard players operation #haitta sagyou = #yoyuu sagyou

scoreboard players operation @s sekiyu += #haitta sagyou

# --- 8) 上限で入らなかった分は手元へ返す ----------------------
# ★ 返す石油に moto:"chuo" は付けない。引き出した石油と同じ扱いになる。
#   (上限に達している時は、そもそも預けられないので影響は無い)
scoreboard players operation #hon sagyou = #jisshitsu sagyou
scoreboard players operation #hon sagyou -= #haitta sagyou
scoreboard players operation #kaeshita sagyou = #hon sagyou
execute if score #hon sagyou matches 1.. run function jidai:sekiyu/dashi_give

# --- 9) 本人に知らせる ----------------------------------------
tellraw @s [{"text":"[石油] ","color":"dark_purple"},{"text":"+","color":"light_purple"},{"score":{"name":"#haitta","objective":"sagyou"},"color":"light_purple"},{"text":" 本を預けた (所持 ","color":"gray"},{"score":{"name":"@s","objective":"sekiyu"},"color":"white"},{"text":" 本)","color":"gray"}]
execute if score #hiku sagyou matches 1.. run tellraw @s [{"text":"  先行ペナルティ ","color":"gold"},{"text":"-","color":"red"},{"score":{"name":"#hiku","objective":"sagyou"},"color":"red"},{"text":" 本 (中央より先へ進んでいる間だけ)","color":"gray"}]
execute if score #kaeshita sagyou matches 1.. run tellraw @s [{"text":"  上限に達したので ","color":"gold"},{"score":{"name":"#kaeshita","objective":"sagyou"},"color":"yellow"},{"text":" 本は手元へ返しました","color":"gray"}]
playsound minecraft:entity.experience_orb.pickup player @s
