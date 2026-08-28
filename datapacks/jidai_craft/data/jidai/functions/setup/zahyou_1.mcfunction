# =============================================================
# jidai:setup/zahyou_1 ── 施設1つぶんの行を出す
#   jidai:setup/zahyou から【施設のマーカーとして】呼ばれる(@s = マーカー)。
#   見せる相手は jidai_zahyou_miru の印が付いている人。
# =============================================================
# ★ Pos は小数（例 3.5d）なので、そのまま出すと読みにくい。
#   スコアへ入れると整数に切り捨てられるので、そちらを使う。
#   store result は「命令の答えをスコアに入れる」書き方。
execute store result score #z_x sagyou run data get entity @s Pos[0]
execute store result score #z_y sagyou run data get entity @s Pos[1]
execute store result score #z_z sagyou run data get entity @s Pos[2]

# --- 種別の名前を決める ---------------------------------------
# ★ 施設を増やす時は、ここに1行足すこと。
#   足し忘れると「(不明)」と出るので、黙って消えることはない。
data modify storage jidai:kari shisetsu set value "(不明)"
execute if entity @s[tag=jidai_ginko] run data modify storage jidai:kari shisetsu set value "銀行     (金ブロック)"
execute if entity @s[tag=jidai_mise] run data modify storage jidai:kari shisetsu set value "販売所   (エメラルド)"
execute if entity @s[tag=jidai_uru_chuo] run data modify storage jidai:kari shisetsu set value "販売所   (エメラルド・中央)"
execute if entity @s[tag=jidai_gacha] run data modify storage jidai:kari shisetsu set value "ガチャ   (ダイヤ)"
execute if entity @s[tag=jidai_juki] run data modify storage jidai:kari shisetsu set value "銃器専門店 (エメラルド)"
execute if entity @s[tag=jidai_shinko] run data modify storage jidai:kari shisetsu set value "時代進行 (鉄ブロック)"
execute if entity @s[tag=jidai_kamado] run data modify storage jidai:kari shisetsu set value "エンダーかまど"
execute if entity @s[tag=jidai_plant] run data modify storage jidai:kari shisetsu set value "石油プラント (押せない)"

# --- 1行出す -------------------------------------------------
tellraw @a[tag=jidai_zahyou_miru] [{"text":"  "},{"storage":"jidai:kari","nbt":"shisetsu","color":"white"},{"text":"  (","color":"gray"},{"score":{"name":"#z_x","objective":"sagyou"},"color":"yellow"},{"text":", ","color":"gray"},{"score":{"name":"#z_y","objective":"sagyou"},"color":"yellow"},{"text":", ","color":"gray"},{"score":{"name":"#z_z","objective":"sagyou"},"color":"yellow"},{"text":")","color":"gray"}]
