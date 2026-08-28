# =============================================================
# jidai:shinko/shounin_kawa ── 運営が川の時代進行を承認する
#   運営がコマンドで打つ。これを打つまで時代は進まない。
#   数えるのはシステム、開けるのは人間。
# =============================================================

# 対象の勢力を作業用へ置く
scoreboard players operation #sekiyu sagyou = 川 sekiyu_gokei
scoreboard players operation #chokin sagyou = 川 chokin
scoreboard players operation #jidai sagyou = 川 jidai
scoreboard players operation #kenchiku sagyou = 川 kenchiku

# 承認の時点でもう一度3条件を確かめる(条件が崩れていたら通さない)
function jidai:shinko/hantei
execute if score #zenbu sagyou matches 0 run tellraw @s [{"text":"[進行] ","color":"gold"},{"text":"川 は条件を満たしていない。承認しなかった","color":"red"}]
execute if score #zenbu sagyou matches 0 run return 0

# 貯金を消費する(累積にしない。後半の作業がゼロにならないようにする)
# ★ 引く額は【その時代に必要だった額】。すぐ上で呼んだ
#   jidai:shinko/hantei が #hitsuyo_chokin に入れている。
scoreboard players operation 川 chokin -= #hitsuyo_chokin sagyou

# 時代を1つ進める
scoreboard players add 川 jidai 1
scoreboard players set 川 sagyou 0

tellraw @a [{"text":"[時代進行] ","color":"gold","bold":true},{"text":"川 が次の時代へ進んだ (時代 ","color":"white"},{"score":{"name":"川","objective":"jidai"},"color":"yellow"},{"text":")","color":"white"}]
# ★ 販売所と銃器専門店の品揃えは【その勢力の時代】で決まる(2026-08-20)。
#   進んだ勢力の人にだけ、新しい物が並んだことを知らせる。
tellraw @a[team=kawa] [{"text":"[販売所] ","color":"green"},{"text":"新しい商品が並びました（販売所・銃器専門店を見てください）","color":"white"}]
execute as @a run playsound minecraft:ui.toast.challenge_complete player @s

# 2勢力が到達したら中央の姿が変わる
function jidai:shinko/chuo

# ★★ 未来(時代5)へ到達したら勝ち（2026-08-23 のご指示）★★
#   「現代へ着くこと」自体は勝ちではない。現代から未来へ進めた勢力が勝つ。
#   ★ 番号は決め打ちせず bangou から取る（番号を2か所に書かないため）。
execute if score 川 jidai matches 5.. run scoreboard players operation #s_kuni sagyou = 川 bangou
execute if score 川 jidai matches 5.. run scoreboard players set #shouri_shu sagyou 5
execute if score 川 jidai matches 5.. run function jidai:sensou/shouri_kakutei
