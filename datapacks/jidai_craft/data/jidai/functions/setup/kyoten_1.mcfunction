# =============================================================
# jidai:setup/kyoten_1 ── 拠点1つぶんの施設を置く
#   呼び出し元が execute positioned で拠点の中心へ合わせ、
#   その拠点を持つ勢力のチーム名を引数で渡して呼ぶ。
#     execute positioned 0 101 -410 run function jidai:setup/kyoten_1 {kuni:"kyuryo"}
#   相対座標(~)で書いてあるので、5拠点すべて同じ並びになる。
# =============================================================
# ★マーカーに jidai_kyoten_<チーム名> の印を付けている。
#   略奪は「押した金ブロックが誰の拠点か」で決まるので、この印が要る。
#   置いたあとで tag を付ける形にすると、チャンクの読み込みが間に合わず
#   印が付かない拠点が出た(実測)。summon と同時に付けると必ず付く。
#
# ★★ 1.20.1 にはマクロが無い ★★
#   勢力名を summon の中に埋められないので、
#   「どの勢力の拠点か」の印は【呼び出し側】(jidai:setup/kyoten)で付ける。
#   置いた直後に、まだ印の付いていない施設マーカーへまとめて付ける形。
#   勢力が決まっていない拠点には kuni:"mitei" を渡す
#   (印は付くが誰の拠点でもないので、今までどおり預金として動く)。

# --- 銀行(金ブロック1つ) --------------------------------------
# ★★ 2026-08-18 に 3つ → 1つ へ変えた ★★
#   預金(10/50/全部)も貴金属の売却も、プラグインのチェスト画面の中で
#   完結するようになったため、ブロックを分ける必要が無くなった。
#   売却所のエメラルドも、この画面へ統合したので置かない。
#
# ★ jidai_ginko … プラグインが「ここは銀行だ」と見分けるための印
# ★ jidai_kyoten_<チーム名> … 誰の拠点かの印。略奪の判定に要る
setblock ~ ~ ~6 minecraft:gold_block replace
summon minecraft:marker ~ ~ ~6 {Tags:["jidai_shisetsu","jidai_ginko"]}

# --- 販売所(エメラルドブロック) -------------------------------
# ★ 中央にしか無いと、買い物のたびに中央まで往復することになる。
#   拠点にも置いて、拠点で完結できるようにした(2026-08-18)。
#   印は jidai_mise。中央の買う専用は jidai_uru_chuo のままで別物。
setblock ~4 ~ ~6 minecraft:emerald_block replace
summon minecraft:marker ~4 ~ ~6 {Tags:["jidai_shisetsu","jidai_mise"]}

# --- 銃器専門店(エメラルドブロック) ---------------------------
# ★★ 販売所と同じエメラルドブロックだが、別の店 ★★
#   見分けるのは【ブロックの種類ではなく登録】。マーカーの印が違う。
#     jidai_mise … 販売所(生活・ピッケル・防具)
#     jidai_juki … 銃器専門店(銃9丁・弾6種)
#   同じ石で2種類の店を出しているので、印を取り違えると
#   「押したら違う店が開く」という分かりにくい壊れ方をする。
# ★ 置く場所は列の一番はしの ~10。銀行(0)や販売所(4)から離してある。
setblock ~10 ~ ~6 minecraft:emerald_block replace
summon minecraft:marker ~10 ~ ~6 {Tags:["jidai_shisetsu","jidai_juki"]}

# --- ガチャ(ダイヤブロック。プラグイン側が使う) ---------------
setblock ~7 ~ ~6 minecraft:diamond_block replace
summon minecraft:marker ~7 ~ ~6 {Tags:["jidai_shisetsu","jidai_gacha"]}

# --- 時代を進める(鉄ブロック1つ) ------------------------------
# 押すとプラグインの画面が開き、3条件の達成状況が見える。
# 押せるのはリーダーと代行だけ。進めるのは運営。
setblock ~-4 ~ ~6 minecraft:iron_block replace
summon minecraft:marker ~-4 ~ ~6 {Tags:["jidai_shisetsu","jidai_shinko"]}

# --- エンダーかまど(2つ) --------------------------------------
# ★ 1人1枚の専用の精錬枠。味方にも中身を取られない。
#   クラフトが禁止なので、鉱石をインゴットに変える唯一の道になる。
setblock ~-8 ~ ~6 minecraft:furnace[facing=south] replace
summon minecraft:marker ~-8 ~ ~6 {Tags:["jidai_shisetsu","jidai_kamado"]}
setblock ~-10 ~ ~6 minecraft:furnace[facing=south] replace
summon minecraft:marker ~-10 ~ ~6 {Tags:["jidai_shisetsu","jidai_kamado"]}

# --- 看板で説明を出す -----------------------------------------
setblock ~ ~1 ~6 minecraft:oak_sign{front_text:{messages:['"銀行"','"預ける / 売る"','"押すと開く"','""']},is_waxed:1b} replace
setblock ~7 ~1 ~6 minecraft:oak_sign{front_text:{messages:['"ガチャ"','"ダイヤを押す"','""','""']},is_waxed:1b} replace
setblock ~-4 ~1 ~6 minecraft:oak_sign{front_text:{messages:['"時代を進める"','"リーダー専用"','""','""']},is_waxed:1b} replace
setblock ~4 ~1 ~6 minecraft:oak_sign{front_text:{messages:['"販売所"','"買う"','""','""']},is_waxed:1b} replace
setblock ~-9 ~1 ~6 minecraft:oak_sign{front_text:{messages:['"エンダーかまど"','"自分専用"','"味方にも"','"取られない"']},is_waxed:1b} replace
setblock ~10 ~1 ~6 minecraft:oak_sign{front_text:{messages:['"銃器専門店"','"銃と弾"','"中世から"','""']},is_waxed:1b} replace
