# =============================================================
# jidai:mise/aketa ── 販売所の樽を開けた
#
# ★★ この関数には、いま到達しません（2026-08-20 時点） ★★
#   起動元だったアドバンスメント jidai:aketa は、1.20.1 に
#   minecraft:default_block_use の起動条件が無いため削除しました（実測）。
#   さらに、必要なマーカー(jidai_chest_kojin 等)を setup が置きません。
#
#   それでも消していないのは、これが【暫定UI】の一式だからです。
#   プラグインのチェスト画面が使えない事態になった時の逃げ道として、
#   mise/chest / mise/kau_kojin / mise/kau_seiryoku ごと残してあります。
#   使うなら、マーカーを置く処理と起動の入口を作り直す必要があります。
#   開けた本人として走る(アドバンスメントの報酬なので確実に本人)。
#   「この人が今、棚を開けている」という印を付けるだけ。
#   実際の課金は jidai:tick が棚の減りを見て行う。
# =============================================================

# 次に開けた時もまた発火するよう、達成を取り消す
# ★ ここに `advancement revoke @s only jidai:aketa` があったが、
#   1.20.1 に minecraft:default_block_use の起動条件が無い(実測)ため、
#   そのアドバンスメント自体を消した。存在しないものを revoke すると
#   【黙って失敗するだけ】で気づけないので、行ごと外してある。

# 販売所のマーカーが近くに無ければ、ただの樽なので何もしない。
# return 0 = この関数をここで打ち切る。
execute unless entity @e[type=marker,tag=jidai_chest_kojin,distance=..6] unless entity @e[type=marker,tag=jidai_chest_seiryoku,distance=..6] run return 0

# 「買い物中」の印。5秒で自然に外れる(外すのは jidai:clock)。
# ★この印があるおかげで、棚が減った時の購入者を「近くの誰か」ではなく
#   「自分で樽を開けた本人」に絞れる。
tag @s add jidai_kaimono
scoreboard players set @s kaimono 5
