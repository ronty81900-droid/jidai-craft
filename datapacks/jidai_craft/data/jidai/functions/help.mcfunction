# =============================================================
# jidai:help ── 使い方と自分の残高をチャットに出す
#   /function jidai:help で誰でも見られる。
# =============================================================

tellraw @s [{"text":"── 時代クラフト v6 ──","color":"gold","bold":true}]
tellraw @s [{"text":"あなたの残高: 金 ","color":"white"},{"score":{"name":"@s","objective":"kane_kojin"},"color":"gold"},{"text":" / 石油 ","color":"white"},{"score":{"name":"@s","objective":"sekiyu"},"color":"dark_purple"},{"text":" 本","color":"white"}]
tellraw @s [{"text":"1. 掘る","color":"aqua"},{"text":" 石をピッケルで掘ると 鉄15%/ラピス5%/金2%/ダイヤ0.5%","color":"white"}]
tellraw @s [{"text":"2. 売る","color":"aqua"},{"text":" 拠点の【エメラルドブロック】を押すと全部売れる","color":"white"}]
tellraw @s [{"text":"  鉄1 / ラピス2 / 金4 / ダイヤ8。中央では売れない","color":"dark_gray"}]
tellraw @s [{"text":"3. 預ける","color":"aqua"},{"text":" 拠点の【金ブロック】3つ = 10 / 50 / 全部","color":"white"}]
tellraw @s [{"text":"  勢力の貯金は時代を進めるのに要る","color":"dark_gray"}]
tellraw @s [{"text":"石油","color":"light_purple"},{"text":" 中央プラントが落とす。拾うと自分の石油になる","color":"white"}]
tellraw @s [{"text":"  /trigger sekiyu_dashi set 5","color":"aqua"},{"text":" で5本アイテム化(取引用)","color":"white"}]
tellraw @s [{"text":"  持っているだけなら死んでも失わない。出した分だけ危険","color":"dark_gray"}]
tellraw @s [{"text":"先行","color":"gold"},{"text":" 中央より先の時代へ進むと、勢力全員が光り、中央の石油が減って拾える","color":"white"}]
tellraw @s [{"text":"腐敗","color":"dark_red"},{"text":" 勢力の石油が一定時間 減らないと腐る。一番多く持つ人から減る","color":"white"}]
tellraw @s [{"text":"  使うか、売るか、払うこと。仲間内で渡し合っても総量は変わらない","color":"dark_gray"}]
tellraw @s [{"text":"戦争","color":"red"},{"text":" 宣戦から準備5分 → 交戦10分 → 30分は再戦できない","color":"white"}]
tellraw @s [{"text":"略奪","color":"dark_red"},{"text":" 交戦中に【相手拠点の金ブロック】を押すと 金30+石油2 を奪える","color":"white"}]
tellraw @s [{"text":"  10秒に1回。1戦争で 金300/石油10 まで。自分の拠点は今までどおり預金","color":"dark_gray"}]
tellraw @s [{"text":"占領","color":"dark_red"},{"text":" 交戦が終わった時に貯金0だと占領される。不利益は無いが時間では解けない","color":"white"}]
tellraw @s [{"text":"下剋上","color":"light_purple"},{"text":" 販売所の商品(勢力の金150・近代解禁・1つまで)","color":"white"}]
tellraw @s [{"text":"  リーダーが占領中に使うと、占領が解けて相手へ即開戦(略奪の上限2倍)","color":"dark_gray"}]
tellraw @s [{"text":"/team join kyuryo @s","color":"aqua"},{"text":" 丘陵","color":"white"}]
tellraw @s [{"text":"  他は shinrin(森林) kawa(川) naikai(内海) iwaba(岩場)","color":"gray"}]
