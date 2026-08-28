# =============================================================
# jidai:sekiyu/jougen ── 勢力が石油をためられる上限を決める
#   入力  #kuni_jidai sagyou … その勢力の今の時代 (1〜4)
#   出力  #jougen     sagyou … ためられる本数の上限
#
#   ★★ 上限 = 今の時代を進めるのに要る石油 × 1.2 ★★
#     鉄器 100 → 120 / 中世 250 → 300 / 近代 500 → 600 / 現代 500 → 600
#     ★ 2026-08-23: 現代にも進む先（未来）ができたので、現代は
#       進行_石油_現代 から引く。未来（5以上）は進む先が無いので現代と同じ。
#
#   なぜ上限を付けるか:
#     必要量の何倍もためておくと「先に進める権利」を買い占められる。
#     2割の余裕だけ残して、それ以上は意味を持たせない。
#     余った石油は取引にも使えないわけではない(手元には残る)。
#
#   ★ 進行_石油_* を変えれば、上限も自動でついてくる。
#     数字を2か所に書かないため、ここでは掛け算だけしている。
# =============================================================

# --- その時代を進めるのに要る本数を引く -----------------------
execute if score #kuni_jidai sagyou matches 1 run scoreboard players operation #jougen sagyou = 進行_石油_鉄器 settei
execute if score #kuni_jidai sagyou matches 2 run scoreboard players operation #jougen sagyou = 進行_石油_中世 settei
execute if score #kuni_jidai sagyou matches 3 run scoreboard players operation #jougen sagyou = 進行_石油_近代 settei
# 現代は「未来へ進む」ぶんを持てる必要がある。
execute if score #kuni_jidai sagyou matches 4 run scoreboard players operation #jougen sagyou = 進行_石油_現代 settei
# 未来(5以上)は進む先が無い。現代と同じ量を上限にする。
execute if score #kuni_jidai sagyou matches 5.. run scoreboard players operation #jougen sagyou = 進行_石油_現代 settei

# --- 2割の余裕を足す (× 120 ÷ 100) ----------------------------
# ★ 先に掛けてから割ること。逆にすると整数の割り算で端数が消える。
scoreboard players operation #jougen sagyou *= #hyaku20 sagyou
scoreboard players operation #jougen sagyou /= #hyaku sagyou
