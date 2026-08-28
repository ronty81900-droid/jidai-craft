# =============================================================
# jidai:clock ── 20tick(1秒)ごとに走る時計
#   勢力の集計・サイドバー・石油の湧き・引き出しの受付をする。
#   最後で自分を再予約するので、ずっと回り続ける。
# =============================================================

# --- 0) 全員のスコア枠を先に作る ------------------------------
execute as @a run scoreboard players add @s kane_kojin 0
execute as @a run scoreboard players add @s sekiyu 0
execute as @a run scoreboard players add @s fuhai_kijun 0
execute as @a run scoreboard players add @s fuhai_byou 0
# 略奪のクールダウンの枠
execute as @a run scoreboard players add @s ryakudatsu_kan 0

# --- 0.5) 勢力の番号を個人にも持たせる ------------------------
# ★略奪で「相手勢力の人」をセレクタで選ぶのに使う。
#   番号で持つと、勢力名をマクロへ渡さずにセレクタが書ける。
# 勢力を増やす時は、ここに1行足す
execute as @a run scoreboard players set @s bangou 0
execute as @a[team=kyuryo] run scoreboard players set @s bangou 1
execute as @a[team=shinrin] run scoreboard players set @s bangou 2
execute as @a[team=kawa] run scoreboard players set @s bangou 3
execute as @a[team=naikai] run scoreboard players set @s bangou 4
execute as @a[team=iwaba] run scoreboard players set @s bangou 5

# --- 0.7) 略奪のクールダウンを1秒ずつ減らす -------------------
execute as @a[scores={ryakudatsu_kan=1..}] run scoreboard players remove @s ryakudatsu_kan 1

# --- 1) 勢力ごとの石油合計を数え直す --------------------------
# ★ その勢力のチームに居る人の石油を、毎秒 足し直す。
#   時代進行の条件A(石油)はこの合計で見る。
#   ★ 2026-08-26: 傭兵を止めたので、絞り込み(scores={youhei=0})を外した。
#     チームに居る人は全員 数に入る。経緯は docs/蛮族と傭兵（休止中）.md。
#
# 勢力を増やす時は、ここに2行足す
scoreboard players set 丘陵 sekiyu_gokei 0
execute as @a[team=kyuryo] run scoreboard players operation 丘陵 sekiyu_gokei += @s sekiyu
scoreboard players set 森林 sekiyu_gokei 0
execute as @a[team=shinrin] run scoreboard players operation 森林 sekiyu_gokei += @s sekiyu
scoreboard players set 川 sekiyu_gokei 0
execute as @a[team=kawa] run scoreboard players operation 川 sekiyu_gokei += @s sekiyu
scoreboard players set 内海 sekiyu_gokei 0
execute as @a[team=naikai] run scoreboard players operation 内海 sekiyu_gokei += @s sekiyu
scoreboard players set 岩場 sekiyu_gokei 0
execute as @a[team=iwaba] run scoreboard players operation 岩場 sekiyu_gokei += @s sekiyu

# --- 2) サイドバー --------------------------------------------
# 個人の金は Tab(list) に出しているので、ここには入れない。
# 勢力は1行にまとめて「石油と金と占領」を出す(中身は jidai:hyouji/kousin)。
function jidai:hyouji/kousin

# --- 個人の金を画面下の帯に出す --------------------------------
# ★★ 個人の金はサイドバーに出せない ★★
#   サイドバーは「保持者と数字」を並べるものなので、個人の金を出すと
#   その勢力の全員ぶんが並んでしまう(50人なら50行)。
#   本人にだけ見せられる場所は、画面下の帯(アクションバー)しかない。
#   毎秒書き直すので、金が動いたらすぐ変わる。
# ★ 末尾に「円」を付けた（2026-08-22 のご指示。サイドバーの貯金と表記をそろえる）
execute as @a run title @s actionbar [{"text":"所持金 ","color":"gray"},{"score":{"name":"@s","objective":"kane_kojin"},"color":"yellow","bold":true},{"text":" 円","color":"gray"}]

# --- 2.6) 石油の湧き方も中央の時代で切り替える ----------------
# 進むほど「たくさん・速く」出る。総量は必要量×5勢力より多い。
execute if score 世界 chuo matches 1 run scoreboard players operation 石油_時代上限 settei = 石油_上限_鉄器 settei
execute if score 世界 chuo matches 2 run scoreboard players operation 石油_時代上限 settei = 石油_上限_中世 settei
execute if score 世界 chuo matches 3 run scoreboard players operation 石油_時代上限 settei = 石油_上限_近代 settei
execute if score 世界 chuo matches 4.. run scoreboard players operation 石油_時代上限 settei = 石油_上限_現代 settei
execute if score 世界 chuo matches 1 run scoreboard players operation 石油_間隔秒 settei = 石油_間隔_鉄器 settei
execute if score 世界 chuo matches 2 run scoreboard players operation 石油_間隔秒 settei = 石油_間隔_中世 settei
execute if score 世界 chuo matches 3 run scoreboard players operation 石油_間隔秒 settei = 石油_間隔_近代 settei
execute if score 世界 chuo matches 4.. run scoreboard players operation 石油_間隔秒 settei = 石油_間隔_現代 settei

# --- 2.7) 運営の抑制を間隔に掛ける ----------------------------
# ★ 石油_倍率 は百分率。100=通常 / 50=半分の速さ(間隔2倍) / 10=十分の一。
#   「×100 してから ÷倍率」の順でないと、整数の割り算で0になる。
#   倍率が0だと割れないので、load で 1.. を保証している。
scoreboard players operation 石油_間隔秒 settei *= #hyaku sagyou
scoreboard players operation 石油_間隔秒 settei /= 石油_倍率 settei

# --- 2.9) 経済勝利になっていないかを見る ----------------------
# ★ 既に勝者が居れば、関数の頭ですぐ返る。
function jidai:sensou/keizai

# --- 3) 中央プラントが石油を吐き出す --------------------------
# 秒数を数え、間隔に達したら1本落とす。
scoreboard players add #plant_t sagyou 1
# ★ 止めている間は時計も戻す。再開した瞬間にまとめて湧く事故を防ぐ。
execute if score 石油_停止 settei matches 1.. run scoreboard players set #plant_t sagyou 0
execute if score 石油_停止 settei matches 0 if score #plant_t sagyou >= 石油_間隔秒 settei run function jidai:sekiyu/waku

# --- 4) 石油の引き出しの受付 ----------------------------------
# /trigger sekiyu_dashi set 5 を打った人を処理する
execute as @a[scores={sekiyu_dashi=1..}] run function jidai:sekiyu/dashi
# 0以下は不正(負の数で増やせてしまう)。黙って捨てる
execute as @a[scores={sekiyu_dashi=..0}] run scoreboard players set @s sekiyu_dashi 0
execute as @a run scoreboard players enable @s sekiyu_dashi

# --- 5) 施設ブロックを直す ------------------------------------
# 壊されても1秒以内に戻る。壊しても何も落ちない(ルートテーブルで空にした)。
execute as @e[type=marker,tag=jidai_uru] at @s unless block ~ ~ ~ minecraft:emerald_block run setblock ~ ~ ~ minecraft:emerald_block
execute as @e[type=marker,tag=jidai_ginko] at @s unless block ~ ~ ~ minecraft:gold_block run setblock ~ ~ ~ minecraft:gold_block
execute as @e[type=marker,tag=jidai_gacha] at @s unless block ~ ~ ~ minecraft:diamond_block run setblock ~ ~ ~ minecraft:diamond_block

# --- 6) 「買い物中」の印を時間切れにする(暫定UIのチェスト用) --
execute as @a[tag=jidai_kaimono] run scoreboard players remove @s kaimono 1
execute as @a[tag=jidai_kaimono,scores={kaimono=..0}] run tag @s remove jidai_kaimono

# --- 6.5) 石油の徴収をまだ受けていない人に適用する ------------
# 中央の時代が上がると世代番号が進む。番号が古い人をここで徴収する。
# オフラインだった人も、次にログインした時に必ず1回だけ徴収される。
execute as @a unless score @s choshu = 世界 choshu run function jidai:shinko/choshu

# --- 6.6) 先行ペナルティ --------------------------------------
# 中央より先へ進んだ勢力を光らせ、中央の石油を取りにくくする。
# 毎秒やり直すので、ログアウト中に状態が変わった人も
# 次にログインした1秒後には正しい状態になる。
function jidai:senkou/hantei

# --- 6.7) 石油の腐敗 (10秒に1回) ------------------------------
# 人数ぶんの持ち物を数えるので、毎秒はやらない。猶予は分の単位なので
# 10秒で足りる。間隔を変える時は jidai:fuhai/kyotsu の「10」も直すこと。
scoreboard players add #fuhai_t sagyou 1
execute if score #fuhai_t sagyou matches 10.. run function jidai:fuhai/hantei

# --- 6.8) 戦争の時計 ------------------------------------------
# ★戦争1つにつきマーカーが1体ある(jidai:sensou/sensen が置く)。
#   マーカーを回すだけなので、勢力が5つに増えてもここは変わらない。
#   バニラには「スコアの保持者を並べる」方法が無いので、
#   今ある戦争の一覧をエンティティで持っている。
execute as @e[type=marker,tag=jidai_sensou] run function jidai:sensou/susumu

# --- 戦争の要約を勢力ごとに書き出す --------------------------
# ★ 戦争の本体はマーカーが持っているが、その保持者は entity の UUID なので
#   プラグインから読めない。勢力名の保持者へ毎秒写しておく。
# ★ sensou_tsuyosa の後始末は jidai:sensou/youyaku が自分で行う。
#   (プラグインからも直接呼ばれるため。ここに置くと単体で呼べない)
function jidai:sensou/youyaku

# --- 7) 時代進行の条件を調べる --------------------------------
# 毎秒すべての勢力を調べると重いので、5秒に1回、1勢力ずつ順番に見る。
# 勢力が5つでも、全部を25秒で一巡する。
scoreboard players add #hantei_t sagyou 1
execute if score #hantei_t sagyou matches 5.. run function jidai:shinko/junban

# --- 8) 20tick後にまた自分を呼ぶ ------------------------------
schedule function jidai:clock 20t replace
