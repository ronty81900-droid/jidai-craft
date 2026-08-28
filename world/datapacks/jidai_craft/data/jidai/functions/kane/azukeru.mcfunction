# =============================================================
# jidai:kane/azukeru ── 銀行。個人の金を勢力の貯金へ預ける
#
# ★★ 本番では、この関数はもう呼ばれていません（2026-08-20 時点） ★★
#   預金も売却も略奪も、プラグインの Ginko が受け持っています。
#   それでも消していないのは、次の2つの理由です。
#     1. tests/verify_jidai.py が【略奪の入口】としてこれを呼んでいる。
#        消すと戦争まわりの検査が丸ごと落ちる
#     2. 「他勢力の拠点なら略奪へ回す」判断の正本がここに書いてある。
#        プラグイン側の実装と突き合わせる時の基準になる
#   拠点の金ブロックを押した本人として走る。
#   どの金ブロックを押したかで金額が決まる (10 / 50 / 全部)。
# =============================================================

# ★ ここに `advancement revoke @s only jidai:oshita_gold` があったが、
#   1.20.1 に minecraft:default_block_use の起動条件が無い(実測)ため、
#   そのアドバンスメント自体を消した。存在しないものを revoke すると
#   【黙って失敗するだけ】で気づけないので、行ごと外してある。

# --- 銀行の近くでなければ何もしない ---------------------------
execute unless entity @e[type=marker,tag=jidai_ginko,distance=..4] run return 0

#   預けさせると勢力の金庫を書き換えることになるし、略奪もできてしまう。

# --- 勢力に入っていない人は預ける先が無い ---------------------
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] run tellraw @s [{"text":"[銀行] ","color":"aqua"},{"text":"勢力に入っていません。先に /team join kyuryo @s","color":"red"}]
execute unless entity @s[team=kyuryo] unless entity @s[team=shinrin] unless entity @s[team=kawa] unless entity @s[team=naikai] unless entity @s[team=iwaba] run return 0

# --- ここが誰の拠点かを見る (別勢力なら略奪へ) ----------------
# ★これが戦争の入口。相手勢力の拠点の金ブロックを押した時だけ、
#   預金ではなく略奪になる。自勢力の銀行は交戦中でも預金のまま。
#   #aite_no は「この拠点を持つ勢力の番号」、@s bangou は「自分の番号」。
# ★ 1.20.1 にマクロが無いので storage では渡さない。
#   押した人の番号を作業用へ置いてから略奪を呼ぶ。
function jidai:sensou/aite_yomu
scoreboard players operation #jibun_no sagyou = @s bangou
execute if score #aite_no sagyou matches 1.. unless score @s bangou = #aite_no sagyou run function jidai:sensou/ryakudatsu
execute if score #aite_no sagyou matches 1.. unless score @s bangou = #aite_no sagyou run return 0

# --- 預ける額を決める -----------------------------------------
# 3つの金ブロックはマーカーのタグで見分ける。
# ★3つは3ブロックしか離れていないので、「4以内にあるか」で見ると
#   隣のブロックにも当たってしまい、後の行が勝つ。
#   (10を押したのに50が預けられ、50を押すと全額入る。v5 までの不具合)
#   一番近いマーカー【1体だけ】を見て、そのタグで額を決める。
scoreboard players set #gaku sagyou 0
execute as @e[type=marker,tag=jidai_ginko,distance=..4,sort=nearest,limit=1] if entity @s[tag=jidai_ginko_10] run scoreboard players set #gaku sagyou 10
execute as @e[type=marker,tag=jidai_ginko,distance=..4,sort=nearest,limit=1] if entity @s[tag=jidai_ginko_50] run scoreboard players set #gaku sagyou 50
# 全部預ける場合は、いったん目印の -1 を置く。
# (この行の中では @s がマーカーになっているので、本人の所持金を読めない)
execute as @e[type=marker,tag=jidai_ginko,distance=..4,sort=nearest,limit=1] if entity @s[tag=jidai_ginko_zenbu] run scoreboard players set #gaku sagyou -1
execute if score #gaku sagyou matches -1 run scoreboard players operation #gaku sagyou = @s kane_kojin

# --- 額が0以下なら何もしない ----------------------------------
execute if score #gaku sagyou matches ..0 run tellraw @s [{"text":"[銀行] ","color":"aqua"},{"text":"預ける金がありません","color":"red"}]
execute if score #gaku sagyou matches ..0 run playsound minecraft:entity.villager.no player @s
execute if score #gaku sagyou matches ..0 run return 0

# --- 所持金が足りるか -----------------------------------------
execute unless score @s kane_kojin >= #gaku sagyou run tellraw @s [{"text":"[銀行] ","color":"aqua"},{"text":"個人の金が足りません (預ける額 ","color":"red"},{"score":{"name":"#gaku","objective":"sagyou"}},{"text":" / 所持 ","color":"red"},{"score":{"name":"@s","objective":"kane_kojin"}},{"text":")","color":"red"}]
execute unless score @s kane_kojin >= #gaku sagyou run playsound minecraft:entity.villager.no player @s
execute unless score @s kane_kojin >= #gaku sagyou run return 0

# --- 預ける ---------------------------------------------------
function jidai:kane/kinko_yomu
scoreboard players operation #mae sagyou = #kinko sagyou
scoreboard players operation @s kane_kojin -= #gaku sagyou
scoreboard players operation #kinko sagyou += #gaku sagyou
function jidai:kane/kinko_kaku

# --- 全体通知(必須) -------------------------------------------
# 誰がいくら預けたかを全員に見せる。
# 預けない人間が可視化されることが、この通知の狙い。
tellraw @a [{"text":"[銀行] ","color":"aqua"},{"selector":"@s"},{"text":" が 勢力へ ","color":"white"},{"score":{"name":"#gaku","objective":"sagyou"},"color":"yellow"},{"text":" 預けた (勢力の貯金 ","color":"gray"},{"score":{"name":"#mae","objective":"sagyou"},"color":"gray"},{"text":" → ","color":"gray"},{"score":{"name":"#kinko","objective":"sagyou"},"color":"gray"},{"text":")","color":"gray"}]

playsound minecraft:block.note_block.bell player @s
