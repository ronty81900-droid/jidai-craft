# =============================================================
# jidai:setup/kyoten ── 施設ブロックを5拠点と中央に置く
#   運営が1回だけ打つ。ブロックを置き、反応する目印(マーカー)も付ける。
#   もう一度打つと置き直しになる(マーカーは重複しない)。
# =============================================================
# 拠点パッドは120x120・高さ100で完全平坦。施設はその上(Y=101)に置く。
# 5拠点とも【拠点の中心から見て同じ相対位置】に並べる。
#
#   相対位置 (拠点の中心を 0,0 とする)
#     金ブロック(10預ける)   (-6, +6)
#     金ブロック(50預ける)   (-3, +6)
#     金ブロック(全部預ける) ( 0, +6)
#     エメラルドブロック     (+4, +6)   … 売却所(貴金属を売る)
#     ダイヤブロック         (+7, +6)   … ガチャ(プラグイン側が使う)

# --- 拠点の区画を読み込ませる ---------------------------------
# ★★ 読み込まれていない区画には、setblock も summon も届かない ★★
#   しかも【黙って失敗する】。エラーは出ない。
#   forceload は「次の tick から読み込む」ので、この関数の中では
#   まだ間に合わない。だから最後に数を数えて、足りなければ知らせる。
function jidai:setup/forceload

# 置き直しに備えて、古いマーカーを消す
execute as @e[type=marker,tag=jidai_shisetsu] run kill @s

# 1500版の拠点中心 (r=410 / 北から時計回りに 0,72,144,216,288度)
#   #1 丘陵 ( 0,-410) / #2 森林 (390,-127) / #3 川 (241,332)
#   #4 内海 (-241,332) / #5 岩場 (-390,-127)
# ★拠点ごとに【その拠点を持つ勢力のチーム名】を渡す。
#   略奪は「押した金ブロックが誰の拠点か」で決まるので、この印が要る。
#   5拠点すべてに勢力が決まっている(2026-08-18 に3〜5を割り当てた)
#   (誰の拠点でもないので、今までどおり預金として動く)。
#   勢力を入れ替える時は、この5行のチーム名を書き換える。
# ★★ 1.20.1 にはマクロが無いので、勢力名を関数へ渡せない ★★
#   代わりに「置く → まだ印の無い施設マーカーへ印を付ける」を拠点ごとに繰り返す。
#   置いた直後に付けるので、他の拠点のマーカーと混ざらない。
execute positioned 0 101 -410 run function jidai:setup/kyoten_1
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_kyuryo
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_zumi

execute positioned 390 101 -127 run function jidai:setup/kyoten_1
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_shinrin
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_zumi

execute positioned 241 101 332 run function jidai:setup/kyoten_1
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_kawa
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_zumi

execute positioned -241 101 332 run function jidai:setup/kyoten_1
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_naikai
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_zumi

execute positioned -390 101 -127 run function jidai:setup/kyoten_1
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_iwaba
tag @e[type=marker,tag=jidai_shisetsu,tag=!jidai_kyoten_zumi] add jidai_kyoten_zumi

# 印を付け終わったので、作業用の印は外す(次に走らせた時のため)
tag @e[type=marker,tag=jidai_kyoten_zumi] remove jidai_kyoten_zumi

# 中央エリア(高さ96)にも、買う用のエメラルドブロックを置く。
# ここでは売れない(売却所は拠点だけ)。押すと断りのメッセージが出る。
setblock 0 97 6 minecraft:emerald_block replace
summon minecraft:marker 0 97 6 {Tags:["jidai_shisetsu","jidai_uru_chuo"]}

# 中央プラント(石油が湧く場所)の目印と、今の時代の外観
summon minecraft:marker 0 97 0 {Tags:["jidai_shisetsu","jidai_plant"]}
function jidai:shinko/plant

# --- 本当に置けたかを数える ------------------------------------
# ★★ ここが無いと「打ったのに何も建っていない」に気づけない ★★
#   置く命令が黙って失敗するので、成功したように見えてしまう。
#   正しい数は 5拠点x7 + 中央2 = 37個。(2026-08-20 に銃器専門店を追加)
#
# ★★ 打った人がコンソール(RCON含む)だと、下の tellraw は届きません ★★
#   コンソールは entity ではないので @s が誰にも当たらず、黙って消えます。
#   そのため、置けた数は必ず【スコアにも残す】。コンソールからは
#     scoreboard players get #shisetsu sagyou
#   で読めます。37 なら成功、それ未満なら区画がまだ読み込まれていません。
#
# ★ ついでの注意: サーバーの黒い画面から打つ時は【スラッシュを付けない】。
#   `/function ...` と打つと "Unknown command" になる(実機で確認)。
#   ゲーム内のチャットからは今までどおりスラッシュを付ける。
scoreboard players set #shisetsu sagyou 0
execute as @e[type=marker,tag=jidai_shisetsu] run scoreboard players add #shisetsu sagyou 1

execute if score #shisetsu sagyou matches 37.. run tellraw @s [{"text":"[設置] ","color":"green"},{"text":"5拠点と中央に施設を置いた (施設 ","color":"white"},{"score":{"name":"#shisetsu","objective":"sagyou"},"color":"yellow"},{"text":" 個 / 37)。次に /jidai scan を打つとプラグインが覚えます","color":"white"}]
execute if score #shisetsu sagyou matches 37.. run tellraw @s [{"text":"      ","color":"gray"},{"text":"座標の一覧は /function jidai:setup/zahyou","color":"gray"}]

execute if score #shisetsu sagyou matches ..36 run tellraw @s [{"text":"[設置] ","color":"red","bold":true},{"text":"置けたのは ","color":"white"},{"score":{"name":"#shisetsu","objective":"sagyou"},"color":"yellow"},{"text":" 個だけです (37個のはず)","color":"white"}]
execute if score #shisetsu sagyou matches ..36 run tellraw @s [{"text":"      ","color":"gray"},{"text":"拠点の区画がまだ読み込まれていません。","color":"gray"}]
execute if score #shisetsu sagyou matches ..36 run tellraw @s [{"text":"      ","color":"gray"},{"text":"★ 5秒ほど待って、もう一度 /function jidai:setup/kyoten を打ってください","color":"yellow"}]
execute if score #shisetsu sagyou matches ..36 run tellraw @s [{"text":"      ","color":"gray"},{"text":"(この関数が forceload を済ませたので、2回目は通ります)","color":"gray"}]
