# =============================================================
# jidai:hyouji/kousin ── サイドバーを作り直す(1秒ごと)
#
#   ★★ 見えるのは【自分の勢力の情報だけ】(2026-08-20) ★★
#
#     丘陵          ← 見出しが自分の勢力名
#     貯金      380
#     石油       12
#     勢力の時代   2   ← ★ 販売所の解禁はこちらで決まる(2026-08-20)
#     中央の時代   1
#
#   勢力ごとに別の目的を作り、`sidebar.team.<色>` へ出している。
#   その枠は【そのチームの色の人にだけ】映るので、
#   丘陵の人には丘陵の数字しか見えない。
# =============================================================
# ★ 並びはスコアの大きい順。貯金 > 石油 > 時代 になることが多い。
#   貯金が0の時だけ石油が上に来るが、行の名前が付いているので読み違えない。
#
# ★ 個人の金はここに出せない。保持者ごとに1行なので、出すと勢力の全員が
#   並んでしまう。本人だけに見せるため、画面下の帯へ回した(jidai:clock)。
#
# ★ 占領されているかは、ここには出していない(指示どおり3つだけ)。
#   占領は起きた時に全体へ知らせているほか、下剋上の画面が教える。
#
# ★ 勢力を増やす時:
#     1. 下の3行のかたまりを複製する
#     2. jidai:load に objectives add hyouji_<番号> と
#        setdisplay sidebar.team.<色> を足す

# --- 丘陵 (番号 1 / チームの色 white) ---
scoreboard players operation 貯金 hyouji_1 = 丘陵 chokin
scoreboard players operation 石油 hyouji_1 = 丘陵 sekiyu_gokei
scoreboard players operation 勢力の時代 hyouji_1 = 丘陵 jidai
scoreboard players operation 中央の時代 hyouji_1 = 世界 chuo

# --- 森林 (番号 2 / チームの色 green) ---
scoreboard players operation 貯金 hyouji_2 = 森林 chokin
scoreboard players operation 石油 hyouji_2 = 森林 sekiyu_gokei
scoreboard players operation 勢力の時代 hyouji_2 = 森林 jidai
scoreboard players operation 中央の時代 hyouji_2 = 世界 chuo

# --- 川 (番号 3 / チームの色 aqua) ---
scoreboard players operation 貯金 hyouji_3 = 川 chokin
scoreboard players operation 石油 hyouji_3 = 川 sekiyu_gokei
scoreboard players operation 勢力の時代 hyouji_3 = 川 jidai
scoreboard players operation 中央の時代 hyouji_3 = 世界 chuo

# --- 内海 (番号 4 / チームの色 blue) ---
scoreboard players operation 貯金 hyouji_4 = 内海 chokin
scoreboard players operation 石油 hyouji_4 = 内海 sekiyu_gokei
scoreboard players operation 勢力の時代 hyouji_4 = 内海 jidai
scoreboard players operation 中央の時代 hyouji_4 = 世界 chuo

# --- 岩場 (番号 5 / チームの色 gold) ---
scoreboard players operation 貯金 hyouji_5 = 岩場 chokin
scoreboard players operation 石油 hyouji_5 = 岩場 sekiyu_gokei
scoreboard players operation 勢力の時代 hyouji_5 = 岩場 jidai
scoreboard players operation 中央の時代 hyouji_5 = 世界 chuo

# --- どの勢力にも入っていない人 --------------------------------
# ここが映るのは次の2組:
#   ・チームに入っていない人（運営・見学者）… 素の sidebar
#
#   その勢力のサイドバーが見えます。 ★★
#   表示枠はチームの色で分かれるため、これは避けられません。
scoreboard players operation 中央の時代 hyouji_0 = 世界 chuo
