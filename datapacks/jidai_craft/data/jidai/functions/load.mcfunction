# =============================================================
# jidai:load ── データパックの初期化
#   スコアボードの定義・チーム・設定値・サイドバーを用意する。
#   最後に 20tick の時計 jidai:clock を回し始める。
#   ワールドを開いた時と /reload した時に自動で走る。
# =============================================================
# ★スコアボードの目的名はプラグイン側との取り決め。README に一覧がある。
#   ここの名前を変えるとプラグインが読めなくなる。

# --- 個人が持つもの -------------------------------------------
# 2回目以降の /reload では「already exists」と赤字が出るが無害。
scoreboard objectives add kane_kojin dummy {"text":"個人の金","color":"gold"}
scoreboard objectives add sekiyu dummy {"text":"石油(本)","color":"dark_purple"}

# --- 勢力が持つもの (保持者は 丘陵 / 森林 の勢力名) -----------
# ★ダミープレイヤー名に引用符を付けてはいけない。
#   "丘陵" と書くと引用符まで含んだ別人になる (1.21.10 で実測)
scoreboard objectives add chokin dummy {"text":"勢力の貯金","color":"aqua"}
scoreboard objectives add sekiyu_gokei dummy {"text":"勢力の石油","color":"dark_purple"}
scoreboard objectives add jidai dummy {"text":"勢力の時代","color":"yellow"}
scoreboard objectives add kenchiku dummy {"text":"建築の数","color":"green"}
scoreboard objectives add jouken_a dummy
scoreboard objectives add jouken_b dummy
scoreboard objectives add jouken_c dummy

# ボスバー（中央の時代への進み具合。2026-08-31 のご指示）
#   ★ add は既にあると失敗して以降が止まる。必ず remove してから作る。
#   ★ 満タン=6（2勢力 × 3条件）。中身は jidai:shinko/bar が毎回 書く。
bossbar remove jidai:chuo
bossbar add jidai:chuo {"text":"世界の時代"}
bossbar set jidai:chuo max 6
bossbar set jidai:chuo players @a
bossbar set jidai:chuo visible true

# --- 世界(中央)が持つもの -------------------------------------
scoreboard objectives add chuo dummy {"text":"中央の時代","color":"red"}
scoreboard objectives add wakidashi dummy
# 石油の徴収の世代番号。世界の値と個人の値が違う人が徴収の対象
scoreboard objectives add choshu dummy

# --- 先行ペナルティと腐敗が持つもの ---------------------------
# senkou: 中央より先へ進んでいるか。1=先行(データパックが毎秒書く)
scoreboard objectives add senkou dummy {"text":"先行","color":"gold"}
#   fuhai_kijun … 前回 時計を戻した時の石油の総量
#   fuhai_byou  … 総量が減っていない秒数
scoreboard objectives add fuhai_kijun dummy
scoreboard objectives add fuhai_byou dummy

# --- 戦争が持つもの (保持者は「丘陵>森林」のような組み合わせ) -
# ★両方向ぶん書く。どちらの側から見ても同じ1行で読めるようにするため。
#   sensou … 0=なし / 1=準備 / 2=交戦 / 3=再戦禁止
scoreboard objectives add sensou dummy
scoreboard objectives add sensou_byou dummy
# 略奪の上限の倍率。ふつうは1、下剋上で始めた戦争は2
scoreboard objectives add sensou_bai dummy
# 1戦争のあいだに奪った累計(上限の判定に使う)
# ★ ryakudatsu_kane / ryakudatsu_sekiyu は 2026-08-20 に消した。
#   1.21 の「組ごと」設計で使っていたもの。マーカー方式では
#   ryakudatsu_kane_a / _b（向きごと）を使うので、こちらは
#   作られるだけで一度も読み書きされていなかった（実測）。
# 略奪のクールダウン(押した本人ごとの残り秒数)
scoreboard objectives add ryakudatsu_kan dummy

# --- 戦争のマーカーが持つスコア（保持者＝マーカー自身）--------
# ★ 1.20.1 にマクロが無いので "丘陵>森林" のような保持者を作れない。
#   戦争1件をマーカー1体で表し、状態をそのマーカーに持たせている。
scoreboard objectives add w_kuni dummy
scoreboard objectives add w_aite dummy
scoreboard objectives add ryakudatsu_kane_a dummy
scoreboard objectives add ryakudatsu_kane_b dummy
scoreboard objectives add ryakudatsu_sekiyu_a dummy
scoreboard objectives add ryakudatsu_sekiyu_b dummy
# 銀行を壊した回数（向きごと）。★ 2026-09-09: これが 占領_必要回数 に届くと
#   ビーコンが壊せるようになる。占領の入口はここ1本だけ。
scoreboard objectives add ryakudatsu_kai_a dummy
scoreboard objectives add ryakudatsu_kai_b dummy
# 勢力ごとの要約（プラグインが読む）
scoreboard objectives add sensou_aite dummy
scoreboard objectives add sensou_tsuyosa dummy

# --- 占領と下剋上 (保持者は勢力名) ----------------------------
# senryou: 0=占領されていない / それ以外=占領している勢力の番号
scoreboard objectives add senryou dummy {"text":"占領","color":"dark_red"}
# gekokujo: 下剋上の権利を持っているか 0/1 (1勢力につき1つまで)
scoreboard objectives add gekokujo dummy

# --- 勝利条件その1「戦争勝利」(2026-08-20 追加) ----------------
# shokuminchi … 保持者＝勢力名 / 値＝宗主国の勢力番号(0 なら独立)
#   交戦中に相手の貯金を0にすると、その拠点のビーコンが壊せるようになる。
#   壊されると植民地になり、自勢力以外の4つを植民地にした勢力が勝つ。
#   ★ senryou(占領) とは別物。senryou は戦争1回ぶんの一時的な状態で、
#     shokuminchi は企画の終わりまで戻らない。
scoreboard objectives add shokuminchi dummy {"text":"宗主国","color":"dark_red"}

# shouri … 保持者＝世界 / 値＝勝った勢力の番号(0 なら まだ)
#   二度 祝わないための見張りも兼ねる。
scoreboard objectives add shouri dummy {"text":"勝利","color":"gold"}
scoreboard players add 世界 shouri 0

# bangou: 勢力の番号。【勢力名とプレイヤーの両方】に入る。
#   略奪で「相手勢力の人」をセレクタで選ぶのに使う(マクロを使わずに済む)
scoreboard objectives add bangou dummy

# --- 調整できる設定値 -----------------------------------------
# 保持者の名前がそのまま設定項目名。プラグインからも読み書きできる。
scoreboard objectives add settei dummy

# --- 内部で使うもの -------------------------------------------
# ★★ サイドバーは【勢力ごとに別のもの】を出す (2026-08-20) ★★
#   1つの目的を sidebar へ出すと、全員が同じものを見る。
#   「自分の勢力の情報だけ見せる」には、表示枠を分けるしかない。
#   `sidebar.team.<色>` は【そのチームの色の人にだけ】映る枠。
#   1.20.1 に在ることは実測済み（無い色を指定すると
#   "Unknown display slot" が返る＝対照も取ってある）。
#
#   色は load の下の方で team modify … color で決めているものと必ず揃える。
#   ★勢力の色を変える時は、下の setdisplay も一緒に直すこと。
scoreboard objectives add hyouji_1 dummy {"text":"丘陵","color":"white","bold":true}
scoreboard objectives add hyouji_2 dummy {"text":"森林","color":"green","bold":true}
scoreboard objectives add hyouji_3 dummy {"text":"川","color":"aqua","bold":true}
scoreboard objectives add hyouji_4 dummy {"text":"内海","color":"blue","bold":true}
scoreboard objectives add hyouji_5 dummy {"text":"岩場","color":"gold","bold":true}
scoreboard objectives add hyouji_0 dummy {"text":"時代クラフト","color":"gold","bold":true}
scoreboard objectives add sagyou dummy
scoreboard objectives add kaimono dummy
scoreboard objectives add sekiyu_dashi trigger

# --- 設定の初期値 ---------------------------------------------
# 「add 0」は無ければ作る・あれば触らない。運営が変えた値を消さない。
# 石油_時代上限 / 石油_間隔秒 は【中央の時代ごと】に切り替わる。
# jidai:clock が毎秒、下の時代別の値をここへ写している。
# ★ 直すのは時代別の方。この2つは作業用の入れ物。
# --- 運営が動かす石油の栓 (2026-08-20) ---------------------------
# ★ この2つだけは【プラグインが書く】。他の settei はここで決めたまま。
#   石油_停止 … 1 なら中央プラントが1本も出さない (jidai sekiyu tomeru)
#   石油_倍率 … 湧く速さ。100 が通常、50 で半分の速さ (jidai sekiyu osaeru 50)
#   ★ add ... 0 は「無ければ0で作る・あればそのまま」。世界をまたいで値が残る。
scoreboard players add 石油_停止 settei 0
execute unless score 石油_倍率 settei matches 1.. run scoreboard players set 石油_倍率 settei 100
# 割り算に使う定数。倍率は百分率なので 100 を掛けてから割る。
scoreboard players set #hyaku sagyou 100
# 石油の獲得上限に使う「1.2倍」。jidai:sekiyu/jougen が読む。
scoreboard players set #hyaku20 sagyou 120
# 勝利判定で「あと何勢力か」を出すのに使う。
scoreboard players set #yon sagyou 4

# 勝利条件その3「経済勝利」。勢力の貯金がこの額に達したら勝ち。
# ★ 時代進行に要る貯金は 近代で 50000。その10倍を目安に置いた。
execute unless score 経済_勝利_貯金 settei matches 1.. run scoreboard players set 経済_勝利_貯金 settei 500000

scoreboard players add 石油_時代上限 settei 0
scoreboard players add 石油_間隔秒 settei 0

# 1つの中央時代に湧く総本数。
#   総量 × 間隔 = 湧き切るまでの秒数
#     鉄器 750×5=3750秒(62分) / 中世 1800×3=5400秒(90分) / 近代 3350×2=6700秒(112分)
execute unless score 石油_上限_鉄器 settei matches 1.. run scoreboard players set 石油_上限_鉄器 settei 750
execute unless score 石油_上限_中世 settei matches 1.. run scoreboard players set 石油_上限_中世 settei 1800
execute unless score 石油_上限_近代 settei matches 1.. run scoreboard players set 石油_上限_近代 settei 3350
execute unless score 石油_上限_現代 settei matches 1.. run scoreboard players set 石油_上限_現代 settei 3350

# 中央プラントが1本落とす間隔(秒)。時代が進むほど速くなる。
# ★★ 石油_間隔_* は【1か所あたりの秒数】(2026-08-20 に変更) ★★
#   1回の湧きで4か所すべてに1本ずつ落ちるので、
#   ここに書いた秒数が、そのまま「同じ場所に次が来るまで」になる。
#   ご指示どおり全時代 10秒。時代で速さを変えたければ、下の数字を変える。
#
# ★ ここだけ unless を付けず【毎回 上書き】している。
#   付けたままだと、既に 5/3/2/2 が入っている世界では新しい値に
#   ならない(実際、値は世界に保存されて残る)。
#   その代わり、遊んでいる最中の調整は jidai sekiyu osaeru <1〜100> で行う。
scoreboard players set 石油_間隔_鉄器 settei 10
scoreboard players set 石油_間隔_中世 settei 10
scoreboard players set 石油_間隔_近代 settei 10
scoreboard players set 石油_間隔_現代 settei 10

# --- 時代進行に必要な3条件。【その勢力の今の時代】で切り替わる ---
# ★ 石油は「勢力の合計本数」、貯金は「勢力の貯金」、建築は
#   拠点の中に置かれた 丸石・原木・木の板・石レンガ・かまど の合計個数。
execute unless score 進行_石油_鉄器 settei matches 1.. run scoreboard players set 進行_石油_鉄器 settei 100
execute unless score 進行_石油_中世 settei matches 1.. run scoreboard players set 進行_石油_中世 settei 250
execute unless score 進行_石油_近代 settei matches 1.. run scoreboard players set 進行_石油_近代 settei 500
execute unless score 進行_貯金_鉄器 settei matches 1.. run scoreboard players set 進行_貯金_鉄器 settei 5000
execute unless score 進行_貯金_中世 settei matches 1.. run scoreboard players set 進行_貯金_中世 settei 15000
execute unless score 進行_貯金_近代 settei matches 1.. run scoreboard players set 進行_貯金_近代 settei 50000
execute unless score 進行_建築_鉄器 settei matches 1.. run scoreboard players set 進行_建築_鉄器 settei 200
execute unless score 進行_建築_中世 settei matches 1.. run scoreboard players set 進行_建築_中世 settei 400
execute unless score 進行_建築_近代 settei matches 1.. run scoreboard players set 進行_建築_近代 settei 800
# ★★ 2026-08-23: 時代5「未来」を足した（ご指示）★★
#   「現代へ着くこと」は勝ちではない。現代から【未来】へ進めた勢力が勝つ。
#   ご指示のとおり、条件は【石油の本数】と【建築ブロック数】の2つ。
#
#   ★ 石油 500 … 現代へ着いた勢力がだいたい持っている量。時間の壁ではなく
#     「まだ持っているか」の壁。腐敗（15分で10%）と略奪で下回るので、
#     建てているあいだ守り切る必要がある。
#     ★ ここを 550 以上にすると届かなくなる: 中央が現代になった時の4割徴収で
#       約300本まで落ち、そこから積み直すと 90分ほど かかる（tests/clear_time.py）。
#   ★ 建築 2400 … 数える区画は 24×12×24 = 6912 ブロックが上限なので、その約35%。
#     近代の 800 から +1600。20人で 15〜25分ぶんの作業。ここが実質の時間の壁。
#   ★ 貯金 0 … ご指示に貯金が無いので要らない。0 のままなら Shinko の画面に
#     「この時代では要りません」と出る。要るようにするならここを上げるだけ。
execute unless score 進行_石油_現代 settei matches 1.. run scoreboard players set 進行_石油_現代 settei 500
execute unless score 進行_建築_現代 settei matches 1.. run scoreboard players set 進行_建築_現代 settei 2400
# ★ 貯金だけ「matches 0..」で見る。0 を既定にしたいので、
#   1.. で見ると /reload のたびに 0 へ戻ってしまい、運営が上げても効かない。
#   スコアがまだ無い時だけ 0 を入れる。
execute unless score 進行_貯金_現代 settei matches 0.. run scoreboard players set 進行_貯金_現代 settei 0
# ★ 進行_石油率 / 進行_必要貯金 / 進行_必要建築 は廃止した(2026-08-18)。
#   割合ではなく【時代別の絶対値】に変えたため。上の 進行_石油_鉄器 などを見る。
scoreboard players reset 進行_石油率 settei
scoreboard players reset 進行_必要貯金 settei
scoreboard players reset 進行_必要建築 settei
# 今の中央の時代における最低額。データパックが毎秒書く(運営は触らなくてよい)

# リセット_徴収率: 中央の時代が上がった時に石油から徴収する割合(%)
scoreboard players add リセット_徴収率 settei 0
execute unless score リセット_徴収率 settei matches 1.. run scoreboard players set リセット_徴収率 settei 40

# --- 先行ペナルティの設定 -------------------------------------
# ★ここから下は「matches 0..」で書いてある。上の設定と書き方が違う。
#   理由: これらは 0 が意味のある値(ペナルティ無し・即座に腐る)なので、
#   「matches 1..」で書くと運営が 0 にしても /reload で既定値へ戻ってしまう。
#   スコアがまだ無い時だけ既定値を置く、が正しい形。
#
# 先行_ロス率: 先行勢力が中央の石油を拾う時に失う割合(%)
#   50 なら「2本拾って1本ぶん」。0 にすると目減りしない。
execute unless score 先行_ロス率 settei matches 0.. run scoreboard players set 先行_ロス率 settei 50

# --- 石油の腐敗の設定 -----------------------------------------
# 腐敗_猶予秒: 勢力の石油総量がこの秒数のあいだ減らなければ腐る(900=15分)
execute unless score 腐敗_猶予秒 settei matches 0.. run scoreboard players set 腐敗_猶予秒 settei 900
# 腐敗_減少率: 腐る時に減る割合(%)。切り上げなので1本でも持っていれば必ず減る
execute unless score 腐敗_減少率 settei matches 0.. run scoreboard players set 腐敗_減少率 settei 10
# 腐敗_必要減少率: 腐敗の時計を戻すのに必要な減り幅(%)
#   腐敗_減少率 と同じ値にしておくと
#   「腐敗で取られるより多く使えば腐らない」という1行の説明になる。
#   1 にすると「1本でも減れば腐らない」(素直だが、1本払って逃げられる)。
execute unless score 腐敗_必要減少率 settei matches 0.. run scoreboard players set 腐敗_必要減少率 settei 10

# --- 戦争の設定 -----------------------------------------------
# 準備(略奪できない) → 交戦(略奪できる) → 再戦禁止 の順に進む
execute unless score 戦争_準備秒 settei matches 0.. run scoreboard players set 戦争_準備秒 settei 300
execute unless score 戦争_交戦秒 settei matches 0.. run scoreboard players set 戦争_交戦秒 settei 600
execute unless score 戦争_禁止秒 settei matches 0.. run scoreboard players set 戦争_禁止秒 settei 1800

# --- 略奪の設定 -----------------------------------------------
# ★★ 2026-09-09 のご指示で作り直した ★★
#   前: 銀行の画面から1回 押すと 金30・石油2本。1戦争・1勢力あたり 金300・石油10本まで。
#   今: **相手の銀行のブロックを1回壊すごとに、相手の総量の 1% を奪う**（石油も）。上限は無い。
#
# ★ 1% を「割る数」で持つ理由: 1.20.1 のスコアボードに小数が無い。
#   100 なら 1%、50 なら 2%。下剋上で始めた戦争は倍率2で 2% になる。
execute unless score 略奪_割る数 settei matches 1.. run scoreboard players set 略奪_割る数 settei 100
# 壊した本人ごとのクールダウン（秒）。100回を何分で貯めるかは、ここで決まる。
#   例: 20人が同時に殴れば 100回 ÷ 20人 × 10秒 ＝ 約50秒
execute unless score 略奪_間隔秒 settei matches 0.. run scoreboard players set 略奪_間隔秒 settei 10
# ビーコンが壊せるようになるまでに、攻める側が銀行を壊す回数
execute unless score 占領_必要回数 settei matches 1.. run scoreboard players set 占領_必要回数 settei 100

# --- 下剋上の設定 ---------------------------------------------
# 販売所の商品。占領を解く唯一の手段
execute unless score 下剋上_値段 settei matches 0.. run scoreboard players set 下剋上_値段 settei 150
execute unless score 下剋上_解禁時代 settei matches 0.. run scoreboard players set 下剋上_解禁時代 settei 3

# --- チーム(勢力) ---------------------------------------------
# prefix を付けると通知の名前が「[丘陵] 名前」になり、どの勢力かが分かる
team add kyuryo
team modify kyuryo displayName {"text":"丘陵","color":"white"}
team modify kyuryo color white
team modify kyuryo prefix {"text":"[丘陵] ","color":"white"}

team add shinrin
team modify shinrin displayName {"text":"森林","color":"green"}
team modify shinrin color green
team modify shinrin prefix {"text":"[森林] ","color":"green"}

team add kawa
team modify kawa displayName {"text":"川","color":"aqua"}
team modify kawa color aqua
team modify kawa prefix {"text":"[川] ","color":"aqua"}

team add naikai
team modify naikai displayName {"text":"内海","color":"blue"}
team modify naikai color blue
team modify naikai prefix {"text":"[内海] ","color":"blue"}

team add iwaba
team modify iwaba displayName {"text":"岩場","color":"gold"}
team modify iwaba color gold
team modify iwaba prefix {"text":"[岩場] ","color":"gold"}

# ★★ 2026-08-24: 同じ勢力の人間は傷つけ合えない（ご指示）★★
#   本体はプラグイン（Tatakai）が EntityDamageByEntityEvent で止めているが、
#   MOD の弾がそのイベントに来るかは実機でしか分からない。
#   バニラのチームの friendlyFire は【本体が直接】見るので、こちらは保険になる。
team modify kyuryo friendlyFire false
team modify shinrin friendlyFire false
team modify kawa friendlyFire false
team modify naikai friendlyFire false
team modify iwaba friendlyFire false

# --- 勢力と世界の初期値 ---------------------------------------
# 時代は 1(鉄器) から始まる。石器は廃止した
scoreboard players add 丘陵 chokin 0
scoreboard players add 森林 chokin 0
scoreboard players add 川 chokin 0
scoreboard players add 内海 chokin 0
scoreboard players add 岩場 chokin 0
execute unless score 丘陵 jidai matches 1.. run scoreboard players set 丘陵 jidai 1
execute unless score 森林 jidai matches 1.. run scoreboard players set 森林 jidai 1
execute unless score 川 jidai matches 1.. run scoreboard players set 川 jidai 1
execute unless score 内海 jidai matches 1.. run scoreboard players set 内海 jidai 1
execute unless score 岩場 jidai matches 1.. run scoreboard players set 岩場 jidai 1
execute unless score 世界 chuo matches 1.. run scoreboard players set 世界 chuo 1
scoreboard players add 世界 wakidashi 0
scoreboard players add 世界 choshu 0

# 先行と腐敗の枠を作る。「add 0」は無ければ作る・あれば触らない。
# 枠が無いと senkou を読む比較が空振りするので、必ずここで作る。
# 勢力を増やす時は、この3行のかたまりを複製する
scoreboard players add 丘陵 senkou 0
scoreboard players add 丘陵 fuhai_kijun 0
scoreboard players add 丘陵 fuhai_byou 0
scoreboard players add 森林 senkou 0
scoreboard players add 森林 fuhai_kijun 0
scoreboard players add 森林 fuhai_byou 0
scoreboard players add 川 senkou 0
scoreboard players add 川 fuhai_kijun 0
scoreboard players add 川 fuhai_byou 0
scoreboard players add 内海 senkou 0
scoreboard players add 内海 fuhai_kijun 0
scoreboard players add 内海 fuhai_byou 0
scoreboard players add 岩場 senkou 0
scoreboard players add 岩場 fuhai_kijun 0
scoreboard players add 岩場 fuhai_byou 0

# 勢力の番号。1から順に振る(0は「勢力なし」の意味で使うので空ける)。
# 占領・略奪でこの番号を使うので、飛び番にしないこと。
# 勢力を増やす時は、この3行のかたまりを複製する
scoreboard players set 丘陵 bangou 1
scoreboard players add 丘陵 senryou 0
scoreboard players add 丘陵 gekokujo 0
scoreboard players set 森林 bangou 2
scoreboard players add 森林 senryou 0
scoreboard players add 森林 gekokujo 0
scoreboard players set 川 bangou 3
scoreboard players add 川 senryou 0
scoreboard players add 川 gekokujo 0
scoreboard players set 内海 bangou 4
scoreboard players add 内海 senryou 0
scoreboard players add 内海 gekokujo 0
scoreboard players set 岩場 bangou 5
scoreboard players add 岩場 senryou 0
scoreboard players add 岩場 gekokujo 0

# --- サイドバー -----------------------------------------------
# ★★ 自分の勢力の情報だけを見せる (2026-08-20) ★★
#   出すのは3つだけ:  貯金 / 石油 / 中央の時代
#   個人の金は【全員ぶんが並んでしまう】のでサイドバーには出せない。
#   代わりに画面下の帯(アクションバー)へ毎秒出している(jidai:clock)。
#   Tab を押した時の一覧にも今までどおり出る。
#
# ★ どのチームにも入っていない人には、素の sidebar が映る。
#   運営・見学者はここを見る。中央の時代だけが出る。
scoreboard objectives setdisplay sidebar.team.white hyouji_1
scoreboard objectives setdisplay sidebar.team.green hyouji_2
scoreboard objectives setdisplay sidebar.team.aqua hyouji_3
scoreboard objectives setdisplay sidebar.team.blue hyouji_4
scoreboard objectives setdisplay sidebar.team.gold hyouji_5
scoreboard objectives setdisplay sidebar.team.gray hyouji_0
scoreboard objectives setdisplay sidebar hyouji_0
scoreboard objectives setdisplay list kane_kojin

# ★ v6 までの1本の目的(hyouji)は使わない。作らない。
#   古い世界に残っていると行が居座るので、ここで消しておく。
scoreboard objectives remove hyouji

# ★★ 1.20.1 では「1行にまとめる」ができない ★★
#   表示名に数字を埋めるには `scoreboard players display name` が要るが、
#   1.20.1 には無い(実測)。`display numberformat` も無い。
#   そこで v5 までの【勢力ごとに2行】へ戻す。
#     丘陵:貯金 380
#     丘陵:石油  12
#   占領されている勢力だけ「丘陵:占領」の行が増える(最大17行になるが、
#   占領は同時に何件も起きないので実際は収まる)。

# --- 建築判定の区画を常時読み込みにする -----------------------
# clone で数えるには、その場所が読み込まれている必要がある。
# 拠点ごとに 24x24 (2x2チャンク) だけ固定する。5拠点で約20チャンク。
function jidai:setup/forceload

# --- 時計を始動 -----------------------------------------------
# replace = 予約済みの同じ関数を置き換える。
# これが無いと /reload のたびに時計が増えて二重三重に走る。
schedule function jidai:clock 20t replace

execute as @a run tellraw @s [{"text":"[時代クラフト] ","color":"gold"},{"text":"v6 読み込み完了。/function jidai:help","color":"gray"}]
