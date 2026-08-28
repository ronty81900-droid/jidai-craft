# =============================================================
# jidai:sensou/youyaku ── 勢力ごとの「今の戦争」をスコアにまとめる
#   jidai:clock から毎秒呼ばれる。
# =============================================================
# ★★ なぜ要るか ★★
#   戦争の本体はマーカーが持っているが、マーカーのスコアの保持者は
#   その entity の UUID なので、**プラグインから読めない**。
#   そこで「勢力ごとの要約」を勢力名の保持者に毎秒書き出す。
#   プラグインは今までどおり勢力名だけを見ればよい。
#
#   sensou       保持者=勢力名  0=平時 1=準備 2=交戦 3=再戦禁止
#   sensou_aite  保持者=勢力名  相手の勢力の番号（0=相手なし）
#   sensou_byou  保持者=勢力名  今の状態の残り秒
#
# ★ 同時に2つ以上の戦争に関わっている場合は、
#   「一番進んでいるもの（交戦 > 準備 > 再戦禁止）」を代表として出す。
#   プラグインが見るのは「戦えるかどうか」なので、これで足りる。
# ★ 勢力を増やす時は、この3行のかたまりを複製する。

# ★★ ここを消すと、プラグインから呼んだ時だけ黙って効かなくなる ★★
#   youyaku_a / youyaku_b は「今より強い時だけ上書き」する作りで、
#   その比較に sensou_tsuyosa を使う。前回の値が残っていると
#   上書きが起きない。以前は jidai:clock が呼ぶ直前に消していたが、
#   それだと【この関数を単体で呼べない】。
#   プラグインは宣戦の直後にここを直接呼んで状態を読み直すので、
#   後始末はこの関数が自分で持つ。
scoreboard players reset * sensou_tsuyosa

scoreboard players set 丘陵 sensou 0
scoreboard players set 丘陵 sensou_aite 0
scoreboard players set 丘陵 sensou_byou 0
scoreboard players set 森林 sensou 0
scoreboard players set 森林 sensou_aite 0
scoreboard players set 森林 sensou_byou 0
scoreboard players set 川 sensou 0
scoreboard players set 川 sensou_aite 0
scoreboard players set 川 sensou_byou 0
scoreboard players set 内海 sensou 0
scoreboard players set 内海 sensou_aite 0
scoreboard players set 内海 sensou_byou 0
scoreboard players set 岩場 sensou 0
scoreboard players set 岩場 sensou_aite 0
scoreboard players set 岩場 sensou_byou 0

# マーカーを1体ずつ見て、関わっている2勢力の欄へ書き込む
execute as @e[type=marker,tag=jidai_sensou] run function jidai:sensou/youyaku_1
