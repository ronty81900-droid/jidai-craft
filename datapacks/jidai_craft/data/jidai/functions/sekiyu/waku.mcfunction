# =============================================================
# jidai:sekiyu/waku ── 中央プラントが石油を1本吐き出す
#   jidai:clock から、設定した間隔ごとに呼ばれる。
#   その時代の上限に達していたら何も落とさない。
# =============================================================

# 秒数の数えを戻す
scoreboard players set #plant_t sagyou 0

# --- 上限に達していたら止める ---------------------------------
# 中央の時代が上がるとリセットされ、また湧き始める
execute unless score 世界 wakidashi < 石油_時代上限 settei run return 0

# --- プラントのマーカーが無ければ何もしない -------------------
execute unless entity @e[type=marker,tag=jidai_plant] run return 0

# --- 4か所すべてに1本ずつ落とす -------------------------------
# ★★ 2026-08-20 に変更: 出口を順番に回す形をやめた ★★
#   前は1回につき1か所だけに落としていたので、
#   同じ場所に次が来るまで【間隔 x 4】かかっていた。
#   (鉄器の間隔5秒なら1か所20秒。実機で「大体15秒」と観測された)
#   今は1回で4か所すべてに落とす。つまり
#   【1か所あたりの間隔 = 石油_間隔秒】がそのまま効く。
#
# ★プラントの「中」ではなく「周囲」に落とす。
#   中に落とすと、外観の差し替えでアイテムが埋まるうえ、
#   拾うために中へ入る必要が出てしまう。
#   footprint は 12x12 なので、そこから外れた4方向の出口を使う。
#
# 消えない・燃えないための指定 (すべて実測して確認した)
#   Age:-32768      … 通常は 6000tick(5分)で消えるが、これで消えなくなる
#   Invulnerable:1b … 溶岩・火で壊れなくなる
#   PickupDelay:0   … すぐ拾える
# 判定は必ず custom_data で行う。素の黒色の染料とは別物として扱う。
#
# ★moto:"chuo" は「中央プラントが出した石油」の印。
#   先行ペナルティ(取得コスト増)の対象を、これだけに限るために付ける。
execute as @e[type=marker,tag=jidai_plant,limit=1] at @s run summon minecraft:item ~10 ~ ~ {Item:{id:"minecraft:black_dye",Count:1b,tag:{jidai_sekiyu:"oil",CustomModelData:8301,moto:"chuo",display:{Name:'{"text":"石油","color":"black"}'}}},Tags:["jidai_sekiyu"],Age:-32768,Invulnerable:1b,PickupDelay:0}
execute as @e[type=marker,tag=jidai_plant,limit=1] at @s run summon minecraft:item ~-10 ~ ~ {Item:{id:"minecraft:black_dye",Count:1b,tag:{jidai_sekiyu:"oil",CustomModelData:8301,moto:"chuo",display:{Name:'{"text":"石油","color":"black"}'}}},Tags:["jidai_sekiyu"],Age:-32768,Invulnerable:1b,PickupDelay:0}
execute as @e[type=marker,tag=jidai_plant,limit=1] at @s run summon minecraft:item ~ ~ ~10 {Item:{id:"minecraft:black_dye",Count:1b,tag:{jidai_sekiyu:"oil",CustomModelData:8301,moto:"chuo",display:{Name:'{"text":"石油","color":"black"}'}}},Tags:["jidai_sekiyu"],Age:-32768,Invulnerable:1b,PickupDelay:0}
execute as @e[type=marker,tag=jidai_plant,limit=1] at @s run summon minecraft:item ~ ~ ~-10 {Item:{id:"minecraft:black_dye",Count:1b,tag:{jidai_sekiyu:"oil",CustomModelData:8301,moto:"chuo",display:{Name:'{"text":"石油","color":"black"}'}}},Tags:["jidai_sekiyu"],Age:-32768,Invulnerable:1b,PickupDelay:0}

# 湧いた総本数を数える
# 1回で4本 落ちるので4つ数える。
# ★上限の判定はこの関数の頭で1回だけなので、上限を最大3本ぶん
#   超えることがある。750本に対して3本なので、そのままにしている。
scoreboard players add 世界 wakidashi 4

# 中央に居る人に音で知らせる(誰も居なければ何も起きない)
execute as @e[type=marker,tag=jidai_plant,limit=1] at @s as @a[distance=..40] run playsound minecraft:block.large_amethyst_bud.break player @s

# 上限に達したらその旨を全体へ知らせる
execute unless score 世界 wakidashi < 石油_時代上限 settei run tellraw @a [{"text":"[中央プラント] ","color":"dark_purple"},{"text":"この時代の石油は尽きた。中央の時代が変わるまで湧かない","color":"gray"}]
