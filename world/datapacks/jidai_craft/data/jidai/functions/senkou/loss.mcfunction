# =============================================================
# jidai:senkou/loss ── 先行ペナルティで失う本数を決める
#   入力  #hon_chuo … 中央プラント産の石油を何本拾ったか
#         #ritsu    … 失う割合(%)。先行していなければ 0
#   出力  #hiku     … 失う本数
# =============================================================
# ★別の関数に切り出してあるのは、確率を実測できるようにするため。
#   tests/verify_jidai.py がここを何百回も呼んで、
#   本当にその割合で減るかを数えている。

# --- 失う本数 = 本数 × ロス率 ÷ 100 (切り捨て) ----------------
scoreboard players set #hyaku sagyou 100
scoreboard players operation #hiku sagyou = #hon_chuo sagyou
scoreboard players operation #hiku sagyou *= #ritsu sagyou
scoreboard players operation #hiku sagyou /= #hyaku sagyou

# --- 切り捨てた端数は「その確率でもう1本失う」で埋める --------
#   例) 1本 × 50% = 0本 + 端数50 → 50%でもう1本 =「2本拾って1本ぶん」
#   例) 4本 × 50% = 2本 + 端数 0 → 必ず2本
# ★これが無いと、中央では1本ずつ湧くので
#   「1本ずつ拾えばペナルティを完全に回避できる」ことになってしまう。
scoreboard players operation #amari sagyou = #hon_chuo sagyou
scoreboard players operation #amari sagyou *= #ritsu sagyou
scoreboard players operation #amari sagyou %= #hyaku sagyou

# ★★ 1.20.1 に random value は無い ★★
#   代わりに、10%刻みの述語(jidai:ran10〜ran90)を引く。
#   #amari(0〜99) を 10 で割って「段」に直し、その段の述語で判定する。
#
# ★ 確率は変わらない。
#   既定のロス率は 50 なので、#amari は 0 か 50 にしかならない
#   (本数×50 を 100 で割った余りは、本数が偶数なら0・奇数なら50)。
#   50 は 10 で割り切れるため、丸めが起きない。
#   ★ ロス率を 10 の倍数でない値に変えると、10%刻みに丸められる。
scoreboard players operation #dan sagyou = #amari sagyou
scoreboard players set #juu sagyou 10
scoreboard players operation #dan sagyou /= #juu sagyou
execute if score #dan sagyou matches 1 if predicate jidai:ran10 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 2 if predicate jidai:ran20 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 3 if predicate jidai:ran30 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 4 if predicate jidai:ran40 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 5 if predicate jidai:ran50 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 6 if predicate jidai:ran60 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 7 if predicate jidai:ran70 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 8 if predicate jidai:ran80 run scoreboard players add #hiku sagyou 1
execute if score #dan sagyou matches 9.. if predicate jidai:ran90 run scoreboard players add #hiku sagyou 1
