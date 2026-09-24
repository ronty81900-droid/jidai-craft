# =============================================================
# jidai:shinko/junban ── 時代進行の条件を、勢力を順番に1つずつ調べる
#   5秒ごとに1勢力だけ見る。勢力が5つでも25秒で一巡する。
#   50人×5勢力を毎tick調べると重いので、こうして散らしている。
# =============================================================

# 数えを戻す
scoreboard players set #hantei_t sagyou 0

# 順番を1つ進める。勢力の数で折り返す。
# 勢力を増やす時は、ここの上限と下の行を増やす
scoreboard players add #junban sagyou 1
execute if score #junban sagyou matches 6.. run scoreboard players set #junban sagyou 1

# 今回の担当勢力を作業用へ置く。
# 勢力を増やす時は、ここに2行足す
execute if score #junban sagyou matches 1 run function jidai:shinko/hantei_kyuryo
execute if score #junban sagyou matches 2 run function jidai:shinko/hantei_shinrin
execute if score #junban sagyou matches 3 run function jidai:shinko/hantei_kawa
execute if score #junban sagyou matches 4 run function jidai:shinko/hantei_naikai
execute if score #junban sagyou matches 5 run function jidai:shinko/hantei_iwaba

# ★ 判定の直後にボスバーを書き直す。jouken_a/b/c が最新になっているのはここだけ。
function jidai:shinko/bar
