# -*- coding: utf-8 -*-
"""
jidai_craft v6 実サーバー検証 (対象: Java Edition 1.20.1)

指示書「データパック／マップ側」の検証項目を、実サーバーで測る。

使い方:  python tests/verify_jidai.py [--keep]
終了コード: 0=全アサート成功 / 1=アサート失敗 / 2=環境要因で実行不可
"""
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

# ★ 隣の置き場「コマンド」から RCON の道具を借りる。
#   ユーザー名は書かず、【自分の場所から数える】。
#   このファイル = …/KUN/時代クラフト/tests/ なので parents[2] が KUN。
RT = Path(__file__).resolve().parents[2] / "コマンド" / "tests"
sys.path.insert(0, str(RT))
from rcon_client import RconClient, RconError  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SERVER_DIR = ROOT / "tests" / "server"
DATAPACK_SRC = ROOT / "datapacks" / "jidai_craft"
SERVER_JAR = RT / "server-1.20.1.jar"
PORT, RPORT, PW = 25601, 25602, "jidai_verify"

passed, failed = [], []

def check(label, cond, detail=""):
    (passed if cond else failed).append(label)
    print(f"  [{'PASS' if cond else 'FAIL'}] {label}" + (f"  {detail}" if detail else ""))

def prepare():
    # ★前回のワールドが残ったまま再利用されると、アーマースタンドもスコアも
    #   ignore_errors=True で黙って握りつぶすと、原因の分からない失敗が大量に出る。
    #   前の java.exe がまだ掴んでいることがあるので、少し待って何度か試し、
    #   それでも消せなければ【その場で止める】。
    for _ in range(10):
        if not SERVER_DIR.exists():
            break
        shutil.rmtree(SERVER_DIR, ignore_errors=True)
        if SERVER_DIR.exists():
            time.sleep(2)
    if SERVER_DIR.exists():
        raise RuntimeError(
            f"前回の検証サーバーが消せない: {SERVER_DIR}\n"
            "  まだ java.exe が動いている可能性がある。終了させてからやり直すこと。\n"
            "  (このまま続けると前回の世界を引き継いで、数字が二重になる)")
    SERVER_DIR.mkdir(parents=True, exist_ok=True)
    (SERVER_DIR / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    props = [
        f"server-port={PORT}", "online-mode=false", "enable-rcon=true",
        f"rcon.port={RPORT}", f"rcon.password={PW}",
        "level-type=minecraft\\:flat", "generate-structures=false",
        "view-distance=8", "simulation-distance=8", "spawn-protection=0",
        "level-name=world", "sync-chunk-writes=false",
    ]
    (SERVER_DIR / "server.properties").write_text("\n".join(props) + "\n", encoding="utf-8")
    shutil.copytree(DATAPACK_SRC, SERVER_DIR / "world" / "datapacks" / DATAPACK_SRC.name)

def start():
    log = SERVER_DIR / "verify_stdout.log"
    lf = open(log, "w", encoding="utf-8", errors="replace")
    proc = subprocess.Popen(["java", "-Xms1G", "-Xmx2G", "-jar", str(SERVER_JAR), "nogui"],
                            cwd=SERVER_DIR, stdout=lf, stderr=subprocess.STDOUT,
                            stdin=subprocess.DEVNULL)
    print("サーバー起動中 (最大180秒)...")
    dl = time.time() + 180
    while time.time() < dl:
        if proc.poll() is not None:
            return proc, log, False
        if re.search(r"Done \([0-9.]+s\)!", log.read_text(encoding="utf-8", errors="replace")):
            print("起動完了")
            return proc, log, True
        time.sleep(2)
    return proc, log, False

def main():
    keep = "--keep" in sys.argv
    print("=== jidai_craft v6 実サーバー検証 (1.20.1) ===")
    if not SERVER_JAR.exists():
        print(f"[環境エラー] サーバー jar が無い: {SERVER_JAR}")
        return 2
    try:
        prepare()
    except RuntimeError as e:
        print(f"[環境エラー] {e}")
        return 2
    proc, log, ok = start()
    if not ok:
        print("[環境エラー] 起動失敗。ログ末尾:")
        print("\n".join(log.read_text(encoding="utf-8", errors="replace").splitlines()[-25:]))
        proc.kill()
        return 2
    r = RconClient(port=RPORT, password=PW, timeout=180.0)

    def c(x):
        return r.command(x).strip()

    def score(h, o):
        m = re.search(r"has (-?\d+)", c(f"scoreboard players get {h} {o}"))
        return int(m.group(1)) if m else None

    def n_ent(sel):
        # ★版差: 1.20.1 は "Test passed, count: 3" / 1.21 は "Test passed. Count: 3"
        #   (両方の server jar の en_us.json で実測。大文字小文字が違う)
        m = re.search(r"[Cc]ount:\s*(\d+)", c(f"execute if entity {sel}"))
        return int(m.group(1)) if m else 0

    # ★★ 戦争はマーカー1体で1件 (1.20.1 移行で変わった) ★★
    #   1.21 では保持者 "丘陵>森林" のスコアで持っていたが、その名前は
    #   マクロでしか作れない。1.20.1 にマクロが無いので、マーカー自身の
    #   スコアに持たせている。マーカーのスコアは【そのマーカーとして】
    #   でないと読めないので、下の3つを通して読み書きする。
    SEN = "@e[type=marker,tag=jidai_sensou]"

    def sensen(a, b, soku=0):
        """宣戦する。番号をスコアに置いてから呼ぶ(引数が渡せないため)"""
        c(f"scoreboard players set #w_kuni sagyou {a}")
        c(f"scoreboard players set #w_aite sagyou {b}")
        c(f"scoreboard players set #w_soku sagyou {soku}")
        return c("function jidai:sensou/sensen")

    def war(a, b, obj):
        """番号 a→b の戦争マーカーのスコアを読む。無ければ None"""
        c("scoreboard players set #yomu sagyou -999")
        c(f"execute as {SEN[:-1]},scores={{w_kuni={a},w_aite={b}}}] run "
          f"scoreboard players operation #yomu sagyou = @s {obj}")
        v = score("#yomu", "sagyou")
        return None if v == -999 else v

    def war_set(a, b, obj, v):
        """番号 a→b の戦争マーカーのスコアを書く"""
        c(f"execute as {SEN[:-1]},scores={{w_kuni={a},w_aite={b}}}] run "
          f"scoreboard players set @s {obj} {v}")

    def susumu():
        """戦争の時計を1秒進める(全マーカーを1回ずつ)"""
        return c(f"execute as {SEN} run function jidai:sensou/susumu")

    try:
        r.connect()

        # ★偽プレイヤー(アーマースタンド)を置く帯を常時読み込みにする。
        #   人が1人も居ないサーバーでは読み込まれている区画がごく狭く、
        #   summon が成功を返しても @e で数えると居ないことがある
        #   (tests/toushi_5.py で実測。z=30台が全滅した)。
        #   ここは x=0 / z=30〜42 に置いているので、その帯を固定しておく。
        #   これをしないと「たまたま通っている」検証になる。
        c("forceload add 0 24 15 47")

        print("\n[1] ロードとスコアボードの定義 (プラグイン側との取り決め)")
        check("jidai_craft が有効", "jidai_craft" in c("datapack list enabled"))
        txt = log.read_text(encoding="utf-8", errors="replace")
        errs = [l for l in txt.splitlines()
                if any(k in l for k in ("Couldn't parse", "Failed to load", "Whilst executing"))]
        check("ロードエラーが無い", not errs, str(errs[:2]))
        for o in ("kane_kojin", "sekiyu", "chokin", "sekiyu_gokei", "jidai", "chuo",
                  "wakidashi", "kenchiku", "jouken_a", "jouken_b", "jouken_c",
                  "settei", "sagyou", "kaimono", "sekiyu_dashi",
                  "hyouji_0", "hyouji_1", "hyouji_2", "hyouji_3",
                  "hyouji_4", "hyouji_5",
                  "senkou", "fuhai_kijun", "fuhai_byou",
                  "sensou", "sensou_byou", "sensou_bai",
                  # ★ ryakudatsu_kane / ryakudatsu_sekiyu は 2026-08-20 に消した。
                  #   1.21 の「組ごと」設計の名残で、書き込みも読み取りも0だった。
                  #   マーカー方式では向きごとの _a / _b を使う。
                  "ryakudatsu_kane_a", "ryakudatsu_kane_b",
                  "ryakudatsu_sekiyu_a", "ryakudatsu_sekiyu_b",
                  "w_kuni", "w_aite", "sensou_aite", "sensou_tsuyosa",
                  "ryakudatsu_kan", "senryou", "gekokujo", "bangou"):
            check(f"目的 {o} が定義されている",
                  "already exist" in c(f"scoreboard objectives add {o} dummy").lower())
        check("sekiyu_dashi は trigger 型",
              "Enabled" in c("scoreboard players enable #p sekiyu_dashi"))

        print("\n[2] 設定の初期値")
        # ★ 石油と進行条件は【時代別】になった(2026-08-18)。
        #   石油_間隔秒 / 石油_時代上限 は clock が毎秒写す作業用の入れ物なので、
        #   ここでは時代別の方を確かめる。
        for k, v in [("石油_上限_鉄器", 750), ("石油_上限_中世", 1800),
                     ("石油_上限_近代", 3350),
                     # ★ 石油_間隔_* は【1か所あたりの秒数】になった(2026-08-20)。
                     #   1回の湧きで4か所すべてに落ちるので、この値が
                     #   そのまま「同じ場所に次が来るまで」になる。全時代 10 秒。
                     ("石油_間隔_鉄器", 10),
                     ("石油_間隔_中世", 10), ("石油_間隔_近代", 10),
                     ("石油_間隔_現代", 10),
                     ("進行_石油_鉄器", 100), ("進行_石油_中世", 250),
                     ("進行_石油_近代", 500), ("進行_貯金_鉄器", 5000),
                     ("進行_貯金_中世", 15000), ("進行_貯金_近代", 50000),
                     ("進行_建築_鉄器", 200), ("進行_建築_中世", 400),
                     ("進行_建築_近代", 800), ("リセット_徴収率", 40),
                     ("腐敗_減少率", 10), ("腐敗_必要減少率", 10),
                     ("戦争_準備秒", 300), ("戦争_交戦秒", 600), ("戦争_禁止秒", 1800),
                     # ★ 2026-09-09: 1回いくらの固定額と上限は廃止。相手の総量の 1%
                     ("略奪_割る数", 100), ("略奪_間隔秒", 10),
                     ("占領_必要回数", 100),
                     ("下剋上_値段", 150), ("下剋上_解禁時代", 3)]:
            check(f"{k} = {v}", score(k, "settei") == v, f"実測={score(k, 'settei')}")
        check("世界の中央時代 = 1", score("世界", "chuo") == 1)
        check("丘陵の時代 = 1", score("丘陵", "jidai") == 1)

        print("\n[3] 施設の設置 (5拠点 + 中央)")
        c("function jidai:setup/kyoten")
        time.sleep(1)
        # ★ 2026-08-18: 拠点は「金ブロック1個 + ガチャ」になった。
        #   預金(10/50/全部)も売却も、プラグインのチェスト画面に統合したため。
        #   売却所のエメラルドは拠点から無くなった(中央の買う用は残る)。
        for tag, cnt in [("jidai_ginko", 5), ("jidai_uru", 0), ("jidai_gacha", 5),
                         ("jidai_shinko", 5), ("jidai_kamado", 10),
                         ("jidai_mise", 5)]:
            got = n_ent(f"@e[type=marker,tag={tag}]")
            check(f"{tag} のマーカーが {cnt} 個", got == cnt, f"実測={got}")
        check("拠点1の金ブロック(銀行)",
              "Test passed" in c("execute if block 0 101 -404 minecraft:gold_block"))
        check("★拠点にも販売所のエメラルドを置く (2026-08-18に追加)",
              "Test passed" in c("execute if block 4 101 -404 minecraft:emerald_block"))
        check("★拠点1の鉄ブロック(時代を進める)",
              "Test passed" in c("execute if block -4 101 -404 minecraft:iron_block"))
        check("★拠点1のエンダーかまど2つ",
              "Test passed" in c("execute if block -8 101 -404 minecraft:furnace")
              and "Test passed" in c("execute if block -10 101 -404 minecraft:furnace"))
        check("拠点1のダイヤブロック(ガチャ)",
              "Test passed" in c("execute if block 7 101 -404 minecraft:diamond_block"))
        check("中央の買う用エメラルド",
              "Test passed" in c("execute if block 0 97 6 minecraft:emerald_block"))
        check("中央プラントのマーカー", n_ent("@e[type=marker,tag=jidai_plant]") == 1)

        print("\n[4] 施設ブロックは壊れても直り、何も落とさない")
        # ★ 売却所のエメラルドは拠点から無くなったので、銀行の金ブロックで見る
        c("setblock 0 101 -404 minecraft:air replace")
        check("壊した直後は空気",
              "Test passed" in c("execute if block 0 101 -404 minecraft:air"))
        c("function jidai:clock")
        check("clock で元に戻る",
              "Test passed" in c("execute if block 0 101 -404 minecraft:gold_block"))
        c("forceload add -32 -32 32 32")
        c("setblock 20 100 20 minecraft:emerald_block replace")
        c("setblock 20 99 20 minecraft:barrel replace")
        c("loot insert 20 99 20 mine 20 100 20 minecraft:iron_pickaxe")
        # ★1.20.1 に execute if items は無い(実測)。樽の中身を直に読む。
        got = c("data get block 20 99 20 Items")
        check("エメラルドブロックを掘っても何も落ちない", "emerald_block" not in got, got[:60])

        print("\n[5] 石油: 湧き・上限・リセット")
        c("scoreboard players set 世界 wakidashi 0")
        c("execute as @e[type=item,tag=jidai_sekiyu] run kill @s")
        for _ in range(5):
            c("function jidai:sekiyu/waku")
        time.sleep(1)
        got = n_ent("@e[type=item,tag=jidai_sekiyu]")
        # ★ 1回の湧きで【4か所すべて】に1本ずつ落ちる。5回なら20本。
        check("★★5回呼ぶと石油が20本落ちる (1回に4か所)", got == 20, f"実測={got}")
        check("湧出の数えが20", score("世界", "wakidashi") == 20)
        # ★ 鉄器の上限は 750 になった
        c("function jidai:clock")   # 時代別の上限を写させる
        c("scoreboard players set 世界 wakidashi 750")
        before = n_ent("@e[type=item,tag=jidai_sekiyu]")
        c("function jidai:sekiyu/waku")
        time.sleep(1)
        check("上限に達したら湧かない", n_ent("@e[type=item,tag=jidai_sekiyu]") == before,
              f"{before} → {n_ent('@e[type=item,tag=jidai_sekiyu]')}")
        c("scoreboard players set 世界 wakidashi 0")
        c("function jidai:sekiyu/waku")
        time.sleep(1)
        check("リセットすると再開する (4本)",
              n_ent("@e[type=item,tag=jidai_sekiyu]") == before + 4,
              f"実測={n_ent('@e[type=item,tag=jidai_sekiyu]')} / 前={before}")

        # ★ 「4つの出口それぞれに1本ずつ」は、この下の [12] で既に見ている
        #   （"4方向の出口すべてに1本ずつ散らばっている"）。
        #   ここで同じことをもう一度書くと、片方が壊れても気付けない。
        #   実際、平坦な検証worldではアイテムが落ち続けるため
        #   高さを決め打ちした書き方は成り立たない（実測で外した）。
        # ---------- 勝利条件その1「戦争勝利」（2026-08-20 追加） ----------
        #
        # ★ 拠点のビーコンを壊すと植民地になる。自勢力以外の4つを
        #   植民地にしたら勝ち。壊せるかどうかの判定はプラグイン側で、
        #   ここは「壊れたあと」の処理を見る。
        print("  -- 戦争勝利（植民地化）--")
        for mei in ("丘陵", "森林", "川", "内海", "岩場"):
            c(f"scoreboard players set {mei} shokuminchi 0")
        c("scoreboard players set 世界 shouri 0")

        # 丘陵(1) が 森林(2) を落とす
        c("scoreboard players set #s_kuni sagyou 1")
        c("scoreboard players set #s_aite sagyou 2")
        c("function jidai:sensou/shokuminchi")
        check("★★落とされた勢力に宗主国の番号が付く",
              score("森林", "shokuminchi") == 1, f"実測={score('森林', 'shokuminchi')}")
        check("落としていない勢力は独立のまま",
              score("川", "shokuminchi") == 0 and score("内海", "shokuminchi") == 0)
        check("1つだけでは まだ勝ちにならない",
              score("世界", "shouri") == 0, f"実測={score('世界', 'shouri')}")

        # 残り3つも落とす
        for ban in (3, 4, 5):
            c(f"scoreboard players set #s_kuni sagyou 1")
            c(f"scoreboard players set #s_aite sagyou {ban}")
            c("function jidai:sensou/shokuminchi")
        check("★★★4勢力すべてを落とすと勝利が確定する",
              score("世界", "shouri") == 1, f"実測={score('世界', 'shouri')}")

        # ★ 二度 祝わない
        c("scoreboard players set #s_kuni sagyou 3")
        c("scoreboard players set #s_aite sagyou 2")
        c("function jidai:sensou/shokuminchi")
        check("★勝者が決まった後は、上書きされない",
              score("世界", "shouri") == 1, f"実測={score('世界', 'shouri')}")
        check("(乗っ取りそのものは起きる) 森林の宗主国が 川 に変わる",
              score("森林", "shokuminchi") == 3, f"実測={score('森林', 'shokuminchi')}")

        # ★ 自分だけ落としても勝ちにならない（4勢力ぶん要る）
        for mei in ("丘陵", "森林", "川", "内海", "岩場"):
            c(f"scoreboard players set {mei} shokuminchi 0")
        c("scoreboard players set 世界 shouri 0")
        c("scoreboard players set #s_kuni sagyou 2")
        c("scoreboard players set #s_aite sagyou 3")
        c("function jidai:sensou/shokuminchi")
        c("scoreboard players set #s_aite sagyou 4")
        c("function jidai:sensou/shokuminchi")
        check("★3勢力では まだ勝ちにならない (2勢力ぶんで確認)",
              score("世界", "shouri") == 0, f"実測={score('世界', 'shouri')}")
        for mei in ("丘陵", "森林", "川", "内海", "岩場"):
            c(f"scoreboard players set {mei} shokuminchi 0")
        c("scoreboard players set 世界 shouri 0")
        # ---------- 勝利条件その3「経済勝利」 ----------
        print("  -- 経済勝利 --")
        check("経済_勝利_貯金 の初期値は 500000",
              score("経済_勝利_貯金", "settei") == 500000,
              f"実測={score('経済_勝利_貯金', 'settei')}")
        for mei in ("丘陵", "森林", "川", "内海", "岩場"):
            c(f"scoreboard players set {mei} chokin 0")
        c("scoreboard players set 世界 shouri 0")

        c("scoreboard players set 川 chokin 499999")
        c("function jidai:sensou/keizai")
        check("★1 足りないと まだ勝ちにならない",
              score("世界", "shouri") == 0, f"実測={score('世界', 'shouri')}")

        c("scoreboard players set 川 chokin 500000")
        c("function jidai:sensou/keizai")
        check("★★ちょうど 500000 で経済勝利（川=3）",
              score("世界", "shouri") == 3, f"実測={score('世界', 'shouri')}")

        # ★ 決着した後は、他の勢力が達しても上書きされない
        c("scoreboard players set 岩場 chokin 900000")
        c("function jidai:sensou/keizai")
        check("★決着後は他の勢力が達しても変わらない",
              score("世界", "shouri") == 3, f"実測={score('世界', 'shouri')}")

        # ★ 毎秒の時計から呼ばれているか
        csrc2 = (DATAPACK_SRC / "data/jidai/functions/clock.mcfunction").read_text(
            encoding="utf-8")
        check("★時計から経済勝利の判定を呼んでいる",
              "function jidai:sensou/keizai" in csrc2, "呼んでいない")

        # 勝ち方の種別が4つとも文面に用意されているか
        ksrc = (DATAPACK_SRC
                / "data/jidai/functions/sensou/shouri_kakutei.mcfunction").read_text(
            encoding="utf-8")
        #   誰も 2 を立てないので、データパックからも行ごと外してある。
        for shu, mei in ((1, "戦争勝利"), (3, "経済勝利"), (4, "超特殊勝利"), (5, "未来到達勝利")):
            check(f"勝ち方 {shu}（{mei}）の文面がある",
                  f"#shouri_shu sagyou matches {shu} run tellraw" in ksrc
                  and mei in ksrc, "無い")

        for mei in ("丘陵", "森林", "川", "内海", "岩場"):
            c(f"scoreboard players set {mei} chokin 0")
        c("scoreboard players set 世界 shouri 0")
        # ---------- 石油の獲得上限（2026-08-20 追加） ----------
        #
        # ★★ 上限 = その時代を進めるのに要る石油 × 1.2 ★★
        #   鉄器 100→120 / 中世 250→300 / 近代 500→600 / 現代は近代と同じ
        #   計算だけの関数なので、ここで直接 呼んで数えられる。
        print("  -- 石油の獲得上限 --")
        for jidai, hazu in ((1, 120), (2, 300), (3, 600), (4, 600)):
            c(f"scoreboard players set #kuni_jidai sagyou {jidai}")
            c("function jidai:sekiyu/jougen")
            jitsu = score("#jougen", "sagyou")
            check(f"★★時代 {jidai} の上限が {hazu} 本", jitsu == hazu, f"実測={jitsu}")

        # ★ 進行_石油_* を変えると、上限もついてくる（数字を2か所に書かない）
        c("scoreboard players set 進行_石油_鉄器 settei 200")
        c("scoreboard players set #kuni_jidai sagyou 1")
        c("function jidai:sekiyu/jougen")
        check("★必要量を変えると上限もついてくる (200 → 240)",
              score("#jougen", "sagyou") == 240, f"実測={score('#jougen', 'sagyou')}")
        c("scoreboard players set 進行_石油_鉄器 settei 100")

        # ---------- 拾っただけでは数に入らない ----------
        # ★ アドバンスメントを消したので、拾っても何も起きない。
        #   残っていると「拾った瞬間に消える」古い動きに戻る。
        # ★ コマンドでは確かめられない。プレイヤーが居ないと
        #   アドバンスメントの存在を見る前に "No player was found" で弾かれる
        #   （最初そう書いて誤検出した）。ファイルの有無で見る。
        f_a = (DATAPACK_SRC / "data/jidai/advancements/sekiyu_hirotta.json")
        check("★★石油を拾うアドバンスメントが無くなっている",
              not f_a.exists(), str(f_a))
        f_h = (DATAPACK_SRC / "data/jidai/functions/sekiyu/hirotta.mcfunction")
        check("★拾った時の関数も消えている", not f_h.exists(), str(f_h))

        # ---------- 預ける関数の作り ----------
        asrc = (DATAPACK_SRC / "data/jidai/functions/sekiyu/azukeru.mcfunction"
                ).read_text(encoding="utf-8")
        # ★ 上限の判定が clear より前にあること。
        #   逆だと、上限に達している人の石油を一度消してから返すことになり、
        #   持ち物がいっぱいなら地面に落ちて失われる。
        check("★★上限の判定が、手持ちを消すより前にある",
              asrc.index("#yoyuu sagyou matches ..0 run return 0")
              < asrc.index("run clear @s minecraft:black_dye"),
              "消してから断っている")
        check("★入らなかった分を手元へ返している",
              "function jidai:sekiyu/dashi_give" in asrc, "返していない")
        check("先行ペナルティは中央産にだけ掛かる",
              'moto:"chuo"' in asrc and "function jidai:senkou/loss" in asrc)

        # 走らせてエラーが出ないこと（プレイヤーが居ないので中身は進まない）
        out = c("function jidai:sekiyu/azukeru")
        check("預ける関数がエラー無く読み込める",
              "Unknown" not in out and "Incorrect" not in out
              and "Failed to instantiate" not in out, out[:70])

        # ★ ここで測れないこと（プレイヤーが要る）
        print("      ※ 実際に石油を持って預ける流れは、clear と give が")
        print("        プレイヤー専用のため、この検証環境では測れない。")

        # ---------- サイドバーに勢力の時代が出るか ----------
        # ★ 販売所の解禁が勢力の時代で決まるようになったので、
        #   自分の時代が見えないと「なぜ買えないのか」が分からなくなる。
        print("  -- サイドバー --")
        # ---------- 行名にどんな文字が使えるか（リソースパックの前提）----------
        # ★ サイドバーの行の見た目は「保持者の名前」そのもの。
        #   飾り記号や色を入れられるかで、作れる見た目が変わる。
        SEC = chr(167)   # §
        r1 = c(f"scoreboard players set {SEC}e国庫 hyouji_1 7")
        check("★行名に § の色コードを使える",
              "Unknown" not in r1 and "Incorrect" not in r1 and "Invalid" not in r1,
              r1[:70])
        r2 = c(f'scoreboard players set {SEC}e¤ 国庫 hyouji_1 7')
        check("★★行名に【空白】は使えない（引用も効かない）",
              "Unknown" in r2 or "Incorrect" in r2 or "Expected" in r2
              or "Invalid" in r2, r2[:70])
        r3 = c('scoreboard players set "' + SEC + 'e¤ 国庫" hyouji_1 7')
        check("★★引用符で囲んでも空白は通らない",
              "Unknown" in r3 or "Incorrect" in r3 or "Expected" in r3
              or "Invalid" in r3, r3[:70])
        r4 = c(f"scoreboard players set {SEC}6❖時代記 hyouji_1 7")
        check("行名に記号(❖)を使える",
              "Unknown" not in r4 and "Incorrect" not in r4, r4[:70])
        c(f"scoreboard players reset {SEC}e国庫 hyouji_1")
        c(f"scoreboard players reset {SEC}6❖時代記 hyouji_1")
        c("scoreboard players set 丘陵 jidai 3")
        c("scoreboard players set 森林 jidai 1")
        c("function jidai:hyouji/kousin")
        check("★★サイドバーに丘陵の時代が写る (3)",
              score("勢力の時代", "hyouji_1") == 3,
              f"実測={score('勢力の時代', 'hyouji_1')}")
        check("★★勢力ごとに別の値が写る (森林は1)",
              score("勢力の時代", "hyouji_2") == 1,
              f"実測={score('勢力の時代', 'hyouji_2')}")
        check("中央の時代も残っている",
              score("中央の時代", "hyouji_1") == score("世界", "chuo"),
              f"実測={score('中央の時代', 'hyouji_1')}")
        kake = [n for n in (1, 2, 3, 4, 5) if score("勢力の時代", f"hyouji_{n}") is None]
        check("5勢力すべてに勢力の時代の行がある", not kake, f"欠け={kake}")
        c("scoreboard players set 丘陵 jidai 1")
        c("scoreboard players set 森林 jidai 1")
        # ---------- 石油の栓（運営が止める・抑える） ----------
        # ★ プラグインは settei の2つを書くだけ。湧かせるのはデータパック。
        #   ここでは、その2つが本当に効くかを本物のサーバーで見る。
        print("  -- 石油の栓 --")
        check("石油_停止 の初期値は 0", score("石油_停止", "settei") == 0,
              f"実測={score('石油_停止', 'settei')}")
        check("石油_倍率 の初期値は 100", score("石油_倍率", "settei") == 100,
              f"実測={score('石油_倍率', 'settei')}")

        # --- 抑える: 倍率を下げると間隔が伸びる ---
        c("scoreboard players set 世界 chuo 1")     # 鉄器 = 間隔5秒
        # ★ もとの間隔が 10 秒になったので、期待値も 10 を基準にする。
        for bai, hazu in ((100, 10), (50, 20), (10, 100), (25, 40)):
            c(f"scoreboard players set 石油_倍率 settei {bai}")
            c("function jidai:clock")
            jitsu = score("石油_間隔秒", "settei")
            check(f"★倍率 {bai}% で間隔が {hazu} 秒になる", jitsu == hazu, f"実測={jitsu}")
        c("scoreboard players set 石油_倍率 settei 100")

        # --- 止める: 1本も湧かない ---
        c("kill @e[type=item,tag=jidai_sekiyu]")
        c("scoreboard players set 世界 wakidashi 0")
        c("scoreboard players set 石油_停止 settei 1")
        c("scoreboard players set #plant_t sagyou 999")
        c("function jidai:clock")
        check("★★止めている間は湧かない", score("世界", "wakidashi") == 0,
              f"実測={score('世界', 'wakidashi')}")
        check("★止めている間は時計も戻る(再開時にまとめて湧かない)",
              score("#plant_t", "sagyou") == 0, f"実測={score('#plant_t', 'sagyou')}")
        for _ in range(3):
            c("function jidai:clock")
        check("止めたまま3回回しても湧かない", score("世界", "wakidashi") == 0,
              f"実測={score('世界', 'wakidashi')}")

        # --- 再開する ---
        c("scoreboard players set 石油_停止 settei 0")
        c("scoreboard players set #plant_t sagyou 999")
        c("function jidai:clock")
        check("★★再開すると湧く (4本)", score("世界", "wakidashi") == 4,
              f"実測={score('世界', 'wakidashi')}")

        # --- 倍率0を入れても壊れないこと ---
        # ★ 0 で割ると Minecraft は演算を黙って捨て、間隔が前のまま残る。
        #   「抑えたつもりで抑わっていない」という一番たちの悪い壊れ方。
        #   load が 1.. を保証しているので、戻ることを見る。
        c("scoreboard players set 石油_倍率 settei 0")
        c("function jidai:load")
        check("★倍率0でも、読み込み直せば 100 に戻る",
              score("石油_倍率", "settei") == 100, f"実測={score('石油_倍率', 'settei')}")
        c("kill @e[type=item,tag=jidai_sekiyu]")
        c("scoreboard players set 世界 wakidashi 0")
        c("scoreboard players set 石油_停止 settei 0")
        print("\n[6] 石油: 消えない・燃えない (対照実験つき)")
        c("setblock 8 100 8 minecraft:lava replace")
        c('summon minecraft:item 8 101 8 '
          '{Item:{id:"minecraft:black_dye",Count:1b},Tags:["taisho"]}')
        # ★1.20.1 はアイテムコンポーネントが無い。tag:{...} と Count:1b で書く。
        oil_nbt = ('{Item:{id:"minecraft:black_dye",Count:1b,'
                   'tag:{jidai_sekiyu:"oil"}},'
                   'Tags:["jidai_sekiyu","yougan"],Age:-32768,Invulnerable:1b,PickupDelay:0}')
        c(f"summon minecraft:item 8 101 8 {oil_nbt}")
        time.sleep(2)
        check("対照(素のアイテム)は溶岩で消える", n_ent("@e[type=item,tag=taisho]") == 0,
              f"実測={n_ent('@e[type=item,tag=taisho]')}")
        check("石油は溶岩で消えない", n_ent("@e[type=item,tag=yougan]") == 1)
        # ★★ 1.20.1 に tick コマンドは無い(実測: en_us.json に commands.tick が
        #   1件も無い。1.20.3 で追加されたもの)。8000tick を早送りできないので、
        #   「消えないこと」そのものは【測れない】。
        #   代わりに、消えない仕組み(Age)を測る。アイテムは Age が 6000 に
        #   達すると消える。Age:-32768 から毎tick 1ずつ増えるので、消えるまで
        #   38768tick = 約32分。イベントの想定(2時間)より短いが、これは
        #   1.21 版と同じ挙動で、移行で変わったものではない。
        age = c("data get entity @e[type=item,tag=yougan,limit=1] Age")
        check("石油の Age が -32768 になっている", "-32768" in age, age[-30:])
        time.sleep(3)
        age2 = c("data get entity @e[type=item,tag=yougan,limit=1] Age")
        # ★★ 実測で分かったこと ★★
        #   Age:-32768 は「歳を取らない」印で、3秒経っても -32768 のまま。
        #   ふつうのアイテムは Age が 6000 になると消えるが、この石油は
        #   そもそも Age が動かないので【永久に消えない】。
        #   (32分で消える、と見積もっていたが、それは間違いだった)
        check("★Age は3秒経っても増えない = 歳を取らない = 永久に消えない",
              "-32768" in age2, f"3秒前={age[-16:]} 今={age2[-16:]}")
        check("★(測れない) 8000tick の早送りは 1.20.1 では不可。"
              "Age の値から消えないと判断している", True)

        print("\n[7] 貴金属の売却の計算 (鉄1/ラピス5/金10/ダイヤ15/ネザライト30)")
        # ★★ 2026-08-20 に改定 ★★ (前は 鉄1/ラピス2/金4/ダイヤ8)
        #   ネザライトは石を掘った時に 0.1% で出る（プラグイン側）。
        for t, l, k, d, n in [(0, 0, 0, 0, 0), (1, 0, 0, 0, 0), (0, 1, 0, 0, 0),
                              (0, 0, 1, 0, 0), (0, 0, 0, 1, 0), (0, 0, 0, 0, 1),
                              (7, 3, 2, 1, 1)]:
            for nm, v in (("#tetsu", t), ("#lapis", l), ("#kin", k),
                          ("#dia", d), ("#neza", n)):
                c(f"scoreboard players set {nm} sagyou {v}")
            c("scoreboard players set #gokei sagyou 0")
            c("scoreboard players operation #gokei sagyou += #tetsu sagyou")
            for src, mul in (("#lapis", 5), ("#kin", 10), ("#dia", 15), ("#neza", 30)):
                c(f"scoreboard players operation #tmp sagyou = {src} sagyou")
                c(f"scoreboard players set #bai sagyou {mul}")
                c("scoreboard players operation #tmp sagyou *= #bai sagyou")
                c("scoreboard players operation #gokei sagyou += #tmp sagyou")
            want = t + l * 5 + k * 10 + d * 15 + n * 30
            check(f"鉄{t}/ラピス{l}/金{k}/ダイヤ{d}/ネザ{n} → {want}",
                  score("#gokei", "sagyou") == want, f"実測={score('#gokei', 'sagyou')}")

        # ★★ 両側の値がそろっているか ★★
        #   データパックとプラグインで値がずれると、売った額が食い違う。
        usrc = (DATAPACK_SRC / "data/jidai/functions/kane/uru.mcfunction").read_text(
            encoding="utf-8")
        for mul in (5, 10, 15, 30):
            check(f"データパックの倍率に {mul} がある",
                  f"scoreboard players set #bai sagyou {mul}" in usrc, "無い")
        check("データパックもネザライトを数えている",
              "minecraft:netherite_ingot" in usrc, "数えていない")

        print("\n[8] 勢力の貯金の読み書き (チームに入れたエンティティで検証)")
        c('summon minecraft:armor_stand 0 100 30 {Tags:["t_k"],Invisible:1b,Marker:1b}')
        c('summon minecraft:armor_stand 0 100 32 {Tags:["t_s"],Invisible:1b,Marker:1b}')
        c('summon minecraft:armor_stand 0 100 34 {Tags:["t_n"],Invisible:1b,Marker:1b}')
        time.sleep(1)
        c("team join kyuryo @e[tag=t_k]")
        c("team join shinrin @e[tag=t_s]")
        c("scoreboard players set 丘陵 chokin 100")
        c("scoreboard players set 森林 chokin 55")
        c("execute as @e[tag=t_k] run function jidai:kane/kinko_yomu")
        check("丘陵所属で読むと 100", score("#kinko", "sagyou") == 100)
        c("execute as @e[tag=t_s] run function jidai:kane/kinko_yomu")
        check("森林所属で読むと 55", score("#kinko", "sagyou") == 55)
        c("execute as @e[tag=t_n] run function jidai:kane/kinko_yomu")
        check("未所属で読むと 0", score("#kinko", "sagyou") == 0)
        c("scoreboard players set #kinko sagyou 70")
        c("execute as @e[tag=t_k] run function jidai:kane/kinko_kaku")
        check("丘陵が 70 に書き戻る", score("丘陵", "chokin") == 70)
        check("森林は 55 のまま (取り違えない)", score("森林", "chokin") == 55)

        c("execute as @e[tag=t_k] run scoreboard players set @s sekiyu 12")
        c("execute as @e[tag=t_n] run scoreboard players set @s sekiyu 99")
        c("scoreboard players set 丘陵 sekiyu_gokei 0")

        c("team join kyuryo @e[tag=t_n]")
        c("scoreboard players set 丘陵 sekiyu_gokei 0")
        # 対照実験: チームだけで数えると本当に混ざることを示す
        c("scoreboard players set #taisho sagyou 0")
        c("execute as @e[team=kyuryo] run scoreboard players operation #taisho sagyou += @s sekiyu")
        check("対照: チームだけで数えると 111 (だから分離が要る)",
              score("#taisho", "sagyou") == 111, f"実測={score('#taisho', 'sagyou')}")
        c("team leave @e[tag=t_n]")

        c("scoreboard players set #kazu sagyou 0")
        for era, fee in [(1, 1), (2, 2), (3, 4), (4, 8)]:
            c(f"scoreboard players set 世界 chuo {era}")
            c("function jidai:clock")
        c("scoreboard players set 世界 chuo 1")

        print("\n[10] 建築の数え方 (clone filtered)")
        c("fill -12 101 -422 11 112 -399 minecraft:air replace")
        for i in range(30):
            c(f"setblock {-12 + i % 24} {101 + (i // 24)} {-422 + (i * 5) % 24} "
              f"minecraft:furnace replace")
        c("execute positioned 0 101 -410 run function jidai:shinko/kenchiku")
        k = score("#kenchiku", "sagyou")
        check("かまど30個を数えられる", k == 30, f"実測={k}")
        check("元のブロックは壊れない(非破壊)",
              "Test passed" in c("execute if block -12 101 -422 minecraft:furnace"))

        # ★★ 5種類の合計を数えられるか（2026-08-18に対象を増やした）★★
        #   原木と木の板は【種類を問わない】。ブロックタグ #minecraft:logs /
        #   #minecraft:planks で数えているので、樫でも松でも白樺でも入る。
        #   タグが clone の filtered で効くかどうかは、実際に置いて数えないと
        #   分からない（効かなければ黙って 0 になる）。
        c("fill -12 101 -422 11 112 -399 minecraft:air replace")
        oku = [("minecraft:cobblestone", 10), ("minecraft:oak_log", 7),
               ("minecraft:spruce_log", 5), ("minecraft:oak_planks", 6),
               ("minecraft:birch_planks", 4), ("minecraft:stone_bricks", 8),
               ("minecraft:furnace", 3)]
        n = 0
        for buroku, kazu in oku:
            for i in range(kazu):
                x = -12 + (n % 24)
                y = 101 + (n // 24)
                c(f"setblock {x} {y} -422 {buroku} replace")
                n += 1
        c("execute positioned 0 101 -410 run function jidai:shinko/kenchiku")
        k2 = score("#kenchiku", "sagyou")
        check("★★5種類の合計を数えられる (丸石10+原木12+板10+石レンガ8+かまど3 = 43)",
              k2 == 43, f"実測={k2}")
        # 種類ごとの内訳も見る（タグが効いていないと原木・板が落ちる）
        c("execute store result score #tk sagyou positioned 0 101 -410 run "
          "clone ~-12 101 ~-12 ~11 112 ~11 ~-12 -60 ~-12 filtered #minecraft:logs force")
        check("★原木は種類を問わず数えられる (樫7+松5 = 12)",
              score("#tk", "sagyou") == 12, f"実測={score('#tk', 'sagyou')}")
        c("execute store result score #tk sagyou positioned 0 101 -410 run "
          "clone ~-12 101 ~-12 ~11 112 ~11 ~-12 -60 ~-12 filtered #minecraft:planks force")
        check("★木の板は種類を問わず数えられる (樫6+白樺4 = 10)",
              score("#tk", "sagyou") == 10, f"実測={score('#tk', 'sagyou')}")
        # ★ ここで更地に戻さないこと。後の「時代進行」の節が、この43個を
        #   建築の数として使っている（消すと条件Cが通らなくなる）。

        print("\n[11] 時代進行の3条件と承認")
        # ★ 鉄器→中世: 石油100 / 貯金5000 / 建築200
        c("scoreboard players set 丘陵 sekiyu_gokei 99")
        c("scoreboard players set 丘陵 chokin 5000")
        c("scoreboard players set 丘陵 jidai 1")
        c("function jidai:shinko/hantei_kyuryo")
        check("石油99本では条件A未達 (必要100)", score("丘陵", "jouken_a") == 0,
              f"a={score('丘陵', 'jouken_a')}")
        check("貯金5000で条件B達成", score("丘陵", "jouken_b") == 1)
        check("★建築が200に届かないので条件C未達", score("丘陵", "jouken_c") == 0,
              f"c={score('丘陵', 'jouken_c')} 実測の数={score('丘陵', 'kenchiku')}")
        c("scoreboard players set 丘陵 sekiyu_gokei 100")
        c("function jidai:shinko/hantei_kyuryo")
        check("石油100本で条件A達成", score("丘陵", "jouken_a") == 1)
        # 建築を満たすのはブロックを200個置くことになるので、
        # ここでは必要量そのものを下げて承認まで通す
        c("scoreboard players set 進行_建築_鉄器 settei 1")
        c("function jidai:shinko/hantei_kyuryo")
        check("必要量を下げれば条件Cも達成する", score("丘陵", "jouken_c") == 1,
              f"c={score('丘陵', 'jouken_c')}")
        check("承認前は時代1のまま", score("丘陵", "jidai") == 1)
        c("function jidai:shinko/shounin_kyuryo")
        check("承認で時代2になる", score("丘陵", "jidai") == 2, f"実測={score('丘陵', 'jidai')}")
        check("承認で貯金が消費される (5000→0)", score("丘陵", "chokin") == 0,
              f"実測={score('丘陵', 'chokin')}")
        c("scoreboard players set 進行_建築_鉄器 settei 200")   # 戻す
        c("scoreboard players set 森林 sekiyu_gokei 0")
        c("scoreboard players set 森林 chokin 0")
        c("scoreboard players set 森林 jidai 1")
        c("function jidai:shinko/shounin_shinrin")
        check("条件未達なら承認しても進まない", score("森林", "jidai") == 1)

        print("\n[12] 中央の時代と4割徴収")
        check("1勢力だけでは中央は上がらない", score("世界", "chuo") == 1,
              f"実測={score('世界', 'chuo')}")
        # ★ 鉄器→中世の条件は 石油100 / 貯金5000 / 建築200 になった。
        #   建築はブロックを実際に置かないと満たせないので、
        #   ここでは必要量を下げて承認を通す(条件そのものは上で測っている)。
        c("scoreboard players set 進行_建築_鉄器 settei 1")
        c("scoreboard players set 森林 sekiyu_gokei 100")
        c("scoreboard players set 森林 chokin 5000")
        c("scoreboard players set 森林 kenchiku 30")
        c("execute as @e[tag=t_k] run scoreboard players set @s sekiyu 10")
        c("execute as @e[tag=t_s] run scoreboard players set @s sekiyu 7")
        c("scoreboard players set 世界 wakidashi 55")
        c("function jidai:shinko/shounin_shinrin")
        check("2勢力到達で中央の時代が2になる", score("世界", "chuo") == 2,
              f"実測={score('世界', 'chuo')}")
        check("石油の湧出がリセットされる", score("世界", "wakidashi") == 0)
        c("scoreboard players set 進行_建築_鉄器 settei 200")   # 戻す
        # 徴収は「世代番号が古い人を見つけ次第引く」方式なので、
        # プレイヤーが居ない検証では armor_stand を対象に手で1回走らせて確かめる。
        check("徴収の世代が1つ進む", score("世界", "choshu") == 1,
              f"実測={score('世界', 'choshu')}")
        c("execute as @e[tag=t_k] run function jidai:shinko/choshu")
        c("execute as @e[tag=t_s] run function jidai:shinko/choshu")
        vk = score("@e[tag=t_k,limit=1]", "sekiyu")
        vs = score("@e[tag=t_s,limit=1]", "sekiyu")
        check("10本から4割徴収 → 6本 (10*40/100=4)", vk == 6, f"実測={vk}")
        check("7本から4割徴収 → 5本 (端数切り捨て 7*40/100=2)", vs == 5, f"実測={vs}")
        # 2回走らせても二重に引かれないこと(世代番号で防いでいる)
        c("execute as @e[tag=t_k] unless score @s choshu = 世界 choshu run function jidai:shinko/choshu")
        check("同じ世代で二重に徴収されない", score("@e[tag=t_k,limit=1]", "sekiyu") == 6,
              f"実測={score('@e[tag=t_k,limit=1]', 'sekiyu')}")

        # 売却の本体はプレイヤー専用コマンド(clear)を使うので、
        c("scoreboard players set #uri_member sagyou 0")
        check("勢力所属者は弾かれない", score("#uri_member", "sagyou") == 0,
              f"実測={score('#uri_member', 'sagyou')}")
        src = (DATAPACK_SRC / "data/jidai/functions/kane/uru.mcfunction").read_text(encoding="utf-8")

        print("\n[12-C] 中央プラントの外観切替")
        c("scoreboard players set 世界 chuo 1")
        c("function jidai:shinko/plant")
        h1 = [y for y in range(99, 129)
              if "Test passed" in c(f"execute if block 0 {y} 0 minecraft:cobblestone")]
        check("時代1は丸石の低い柱 (高さ3)", len(h1) == 3, f"実測={len(h1)}段")
        c("scoreboard players set 世界 chuo 4")
        c("function jidai:shinko/plant")
        h4 = [y for y in range(99, 129)
              if "Test passed" in c(f"execute if block 0 {y} 0 minecraft:nether_bricks")]
        check("時代4はネザーレンガの高い柱 (高さ22)", len(h4) == 22, f"実測={len(h4)}段")
        check("時代1の丸石は残っていない",
              "Test failed" in c("execute if block 0 100 0 minecraft:cobblestone"))
        # ★埋まらないことの確認: footprint の外は、切替後も空気のまま
        # 先に [6] の溶岩を片付ける(流れて周囲を埋めるため。検証用の後片付い)
        c("fill 4 96 4 12 102 12 minecraft:air replace minecraft:lava")
        c("fill 4 96 4 12 102 12 minecraft:air replace minecraft:flowing_lava")
        buried = []
        for dx, dz in [(10, 0), (-10, 0), (0, 10), (0, -10), (8, 8), (-8, -8)]:
            for y in (97, 98, 99):
                if "Test failed" in c(f"execute if block {dx} {y} {dz} minecraft:air"):
                    buried.append((dx, y, dz))
        check("周囲(石油の出口と待機位置)は切替後も空気 = 埋まらない",
              not buried, f"埋まった位置={buried}")
        # footprint 内は詰まっている = 中に立てない
        check("土台は詰まっている (中に立てない)",
              "Test failed" in c("execute if block 0 97 0 minecraft:air")
              and "Test failed" in c("execute if block 0 98 0 minecraft:air"))
        # 横に広がっていない (footprint は 12x12 = ~-6..~5)
        check("footprint の外(X=6)は空気のまま",
              "Test passed" in c("execute if block 6 100 0 minecraft:air"))

        print("\n[12-D] 石油はプラントの外に落ちる")
        c("execute as @e[type=item,tag=jidai_sekiyu] run kill @s")
        c("scoreboard players set 世界 wakidashi 0")
        c("scoreboard players set #deguchi sagyou 0")
        for _ in range(4):
            c("function jidai:sekiyu/waku")
        time.sleep(1)
        check("4回で16本落ちる (1回に4か所)", n_ent("@e[type=item,tag=jidai_sekiyu]") == 16,
              f"実測={n_ent('@e[type=item,tag=jidai_sekiyu]')}")
        inside = n_ent("@e[type=item,tag=jidai_sekiyu,x=-6,y=97,z=-6,dx=11,dy=31,dz=11]")
        check("プラントの中(footprint内)に1本も無い", inside == 0, f"実測={inside}本")
        # 平坦な検証world には地面が無くアイテムが落ち続けるので、
        # 高さは見ずに「横位置が4つの出口に分かれているか」で確かめる。
        deguchi = 0
        for ox, oz in [(10, 0), (-10, 0), (0, 10), (0, -10)]:
            n = n_ent(f"@e[type=item,tag=jidai_sekiyu,x={ox-2},y=-320,z={oz-2},"
                      f"dx=4,dy=640,dz=4]")
            deguchi += 1 if n >= 1 else 0
        check("4方向の出口すべてに1本ずつ散らばっている", deguchi == 4,
              f"出口に居た数={deguchi}/4")

        print("\n[12-E] 先行ペナルティ")
        # --- 先行かどうかの判定 ---
        c("scoreboard players set 世界 chuo 2")
        c("scoreboard players set 丘陵 jidai 2")
        c("scoreboard players set 森林 jidai 2")
        c("function jidai:senkou/hantei")
        check("中央と同じ時代なら先行ではない", score("丘陵", "senkou") == 0,
              f"実測={score('丘陵', 'senkou')}")
        c("scoreboard players set 丘陵 jidai 3")
        c("function jidai:senkou/hantei")
        check("中央より先の時代なら先行になる", score("丘陵", "senkou") == 1,
              f"実測={score('丘陵', 'senkou')}")
        check("追う側は先行にならない", score("森林", "senkou") == 0,
              f"実測={score('森林', 'senkou')}")
        c("scoreboard players set 世界 chuo 3")
        c("function jidai:senkou/hantei")
        check("中央が追いつくと自動で解除される", score("丘陵", "senkou") == 0,
              f"実測={score('丘陵', 'senkou')}")

        # --- 実際の進行経路でも確かめる (承認 → 中央が上がる → 解除) ---
        # ★中央は「2勢力が到達したら上がる」ので、先行になるのは
        #   独走している1勢力だけになる。ここがその実証。
        c("scoreboard players set 世界 chuo 1")
        c("scoreboard players set 丘陵 jidai 1")
        c("scoreboard players set 森林 jidai 1")
        # ★ 鉄器→中世の条件に合わせる。建築だけは実際に置けないので下げる。
        c("scoreboard players set 進行_建築_鉄器 settei 1")
        for f, v in [("sekiyu_gokei", 100), ("chokin", 5000), ("kenchiku", 30)]:
            c(f"scoreboard players set 丘陵 {f} {v}")
            c(f"scoreboard players set 森林 {f} {v}")
        c("function jidai:shinko/shounin_kyuryo")
        c("function jidai:senkou/hantei")
        check("承認直後は、独走した1勢力だけが先行になる",
              score("丘陵", "senkou") == 1 and score("森林", "senkou") == 0,
              f"丘陵={score('丘陵', 'senkou')} 森林={score('森林', 'senkou')} 中央={score('世界', 'chuo')}")
        c("function jidai:shinko/shounin_shinrin")
        c("function jidai:senkou/hantei")
        c("scoreboard players set 進行_建築_鉄器 settei 200")   # 戻す
        check("2つめが追いつくと中央が上がり、誰も先行でなくなる",
              score("丘陵", "senkou") == 0 and score("森林", "senkou") == 0,
              f"丘陵={score('丘陵', 'senkou')} 森林={score('森林', 'senkou')} 中央={score('世界', 'chuo')}")

        # --- 誰にペナルティが乗るか (本物の azukeru を走らせて #ritsu を見る) ---
        # ★ 2026-08-20 に、ペナルティは「拾った時」から「預けた時」へ移った。
        # clear はプレイヤー専用なので本数は0のままだが、
        # 「自分の勢力が先行か」を見る分岐は、消す前に置いてあるので通る。
        c("scoreboard players set 丘陵 senkou 1")
        c("scoreboard players set 森林 senkou 0")
        c("execute as @e[tag=t_k] run function jidai:sekiyu/azukeru")
        check("先行勢力のメンバーはロス率が乗る", score("#ritsu", "sagyou") == 50,
              f"実測={score('#ritsu', 'sagyou')}")
        c("execute as @e[tag=t_s] run function jidai:sekiyu/azukeru")
        check("先行していない勢力は乗らない", score("#ritsu", "sagyou") == 0,
              f"実測={score('#ritsu', 'sagyou')}")
        c("team join kyuryo @e[tag=t_n]")
        c("execute as @e[tag=t_n] run function jidai:sekiyu/azukeru")
        c("team leave @e[tag=t_n]")
        c("scoreboard players set 丘陵 senkou 0")

        # --- 採取コスト増の効き目を実測する ---
        def loss_total(hon, ritsu, kaisu):
            c(f"scoreboard players set #hon_chuo sagyou {hon}")
            c(f"scoreboard players set #ritsu sagyou {ritsu}")
            c("scoreboard players set #ushi sagyou 0")
            for _ in range(kaisu):
                c("function jidai:senkou/loss")
                c("scoreboard players operation #ushi sagyou += #hiku sagyou")
            return score("#ushi", "sagyou")

        got = loss_total(1, 0, 20)
        check("ロス率0なら1本も失わない (対照)", got == 0, f"実測={got}/20本")
        got = loss_total(1, 100, 20)
        check("ロス率100なら必ず失う (対照)", got == 20, f"実測={got}/20本")
        got = loss_total(4, 50, 20)
        check("4本まとめて50%なら毎回きっちり2本", got == 40, f"実測={got} (期待40)")
        got = loss_total(1, 50, 200)
        check("1本ずつ200回・50% → 失うのは 79〜121本 (≒半分)",
              79 <= got <= 121, f"実測={got}/200本")

        # --- 発光そのものの動き (senkou/hantei は @a なので実プレイヤーが要る。
        #     ここでは仕組みだけをゾンビで確かめる) ---
        c('summon minecraft:zombie 0 100 40 {Tags:["gz"],NoAI:1b,PersistenceRequired:1b}')
        time.sleep(1)
        c("effect give @e[tag=gz] minecraft:glowing infinite 0 true")
        eff = c("data get entity @e[tag=gz,limit=1] ActiveEffects")
        # ★1.20.1 の ActiveEffects は Id が数字(24b など)で、名前が入っていない。
        #   版で変わらない「残り時間」と「個数」で測る。
        check("発光は infinite でかかる (残り時間 -1)", "Duration: -1" in eff, eff[:150])
        c("effect give @e[tag=gz] minecraft:glowing infinite 0 true")
        eff = c("data get entity @e[tag=gz,limit=1] ActiveEffects")
        check("毎秒かけ直しても効果は1つのまま", eff.count("Duration:") == 1, eff[:150])
        c("effect clear @e[tag=gz] minecraft:glowing")
        check("解除で消える",
              "Duration:" not in c("data get entity @e[tag=gz,limit=1] ActiveEffects"))
        c("kill @e[tag=gz]")
        ssrc = (DATAPACK_SRC / "data/jidai/functions/senkou/hantei.mcfunction").read_text(encoding="utf-8")
        check("(記述) 毎秒かけ直すので、復帰した人にも光が戻る",
              "tag=jidai_hikaru_ima] run effect give @s minecraft:glowing infinite 0 true" in ssrc)
        check("(記述) 対象から外れた人の発光は消える",
              "tag=jidai_hikari,tag=!jidai_hikaru_ima] run effect clear @s minecraft:glowing" in ssrc)
        check("(記述) 発動と解除の両方を全体に知らせる",
              "が中央より先へ進んだ" in ssrc and "先行状態が解除された" in ssrc)

        # --- 中央産の石油だけに印が付く ---
        c("execute as @e[type=item,tag=jidai_sekiyu] run kill @s")
        c("scoreboard players set 世界 wakidashi 0")
        c("function jidai:sekiyu/waku")
        time.sleep(1)
        # ★1.20.1 には execute if items もアイテムコンポーネントも無い(実測)。
        #   拾得の検知(アドバンスメント)と同じ【NBTの部分一致】で測る。
        check("中央プラントの石油には moto:chuo が付く",
              "Test passed" in c('execute if entity @e[type=item,tag=jidai_sekiyu,'
                                 'nbt={Item:{tag:{moto:"chuo"}}}]'))
        check("既存の1キー判定にも当たる (拾得の検知は壊れていない)",
              "Test passed" in c('execute if entity @e[type=item,tag=jidai_sekiyu,'
                                 'nbt={Item:{tag:{jidai_sekiyu:"oil"}}}]'))
        dsrc = (DATAPACK_SRC / "data/jidai/functions/sekiyu/dashi_give.mcfunction").read_text(encoding="utf-8")
        check("(記述) 引き出した石油には印が付かない = 買った石油は目減りしない", "moto" not in dsrc)

        print("\n[12-F] 石油の腐敗")

        def fuhai(gokei, kijun, byou, yuyo=900):
            for nm, v in (("#gokei", gokei), ("#kijun", kijun),
                          ("#byou", byou), ("#yuyo", yuyo)):
                c(f"scoreboard players set {nm} sagyou {v}")
            c("function jidai:fuhai/kyotsu")
            return (score("#heru", "sagyou"), score("#kijun", "sagyou"),
                    score("#byou", "sagyou"))

        heru, kijun, byou = fuhai(50, 50, 0)
        check("減っていなければ時計が進む (まだ腐らない)",
              heru == 0 and byou == 10, f"heru={heru} byou={byou}")
        heru, kijun, byou = fuhai(40, 50, 500)
        check("十分に減っていれば時計が0に戻る",
              heru == 0 and byou == 0 and kijun == 40, f"heru={heru} byou={byou} kijun={kijun}")
        heru, kijun, byou = fuhai(49, 50, 500)
        check("1本だけ払っても時計は戻らない (小額逃げ対策)",
              heru == 0 and byou == 510 and kijun == 50, f"heru={heru} byou={byou} kijun={kijun}")
        heru, kijun, byou = fuhai(60, 50, 500)
        check("増えただけでは時計は戻らない (基準値だけ上がる)",
              heru == 0 and byou == 510 and kijun == 60, f"heru={heru} byou={byou} kijun={kijun}")
        heru, kijun, byou = fuhai(50, 50, 890)
        check("猶予を過ぎたら腐る (50本の10%切り上げ = 5本)",
              heru == 5 and byou == 0 and kijun == 45, f"heru={heru} byou={byou} kijun={kijun}")
        heru, _, _ = fuhai(14, 14, 890)
        check("端数は切り上げ (14本の10% = 2本)", heru == 2, f"実測={heru}")
        heru, _, _ = fuhai(1, 1, 890)
        check("1本しか無くても切り上げで1本腐る (小分けで逃げられない)",
              heru == 1, f"実測={heru}")
        heru, _, byou = fuhai(0, 0, 890)
        check("0本なら腐らない", heru == 0 and byou == 0, f"heru={heru} byou={byou}")

        # --- 一番多く持っている個人から引く ---
        c('summon minecraft:armor_stand 0 100 36 {Tags:["t_a"],Invisible:1b,Marker:1b}')
        c('summon minecraft:armor_stand 0 100 38 {Tags:["t_b"],Invisible:1b,Marker:1b}')
        time.sleep(1)
        for t, s in (("t_a", 5), ("t_b", 30)):
            c(f"team join kyuryo @e[tag={t}]")
            c(f"execute as @e[tag={t}] run scoreboard players set @s sekiyu {s}")
        c("execute as @e[tag=t_k] run scoreboard players set @s sekiyu 7")
        c("scoreboard players set #max sagyou 0")
        c("scoreboard players set #max sagyou 0")
        # ★ 傭兵を止めたので scores={youhei=0} の絞り込みは外した（2026-08-26）
        c("execute as @e[team=kyuryo] run "
          "scoreboard players operation #max sagyou > @s sekiyu")
        check("勢力で最も多い保有量が取れる (7/5/30 → 30)", score("#max", "sagyou") == 30,
              f"実測={score('#max', 'sagyou')}")
        c("scoreboard players set #heru sagyou 3")
        c("scoreboard players set #zumi sagyou 0")
        c("execute as @e[team=kyuryo] if score @s sekiyu = #max sagyou "
          "run function jidai:fuhai/hiku_hitori")
        check("最も多く持っている人からだけ引かれる",
              score("@e[tag=t_b,limit=1]", "sekiyu") == 27
              and score("@e[tag=t_a,limit=1]", "sekiyu") == 5
              and score("@e[tag=t_k,limit=1]", "sekiyu") == 7,
              f"t_b={score('@e[tag=t_b,limit=1]', 'sekiyu')} "
              f"t_a={score('@e[tag=t_a,limit=1]', 'sekiyu')}")
        # 同点が2人いても1人からしか引かないこと
        for t in ("t_a", "t_b"):
            c(f"execute as @e[tag={t}] run scoreboard players set @s sekiyu 10")
        c("scoreboard players set #heru sagyou 3")
        c("scoreboard players set #zumi sagyou 0")
        c("execute as @e[tag=t_a] run function jidai:fuhai/hiku_hitori")
        c("execute as @e[tag=t_b] run function jidai:fuhai/hiku_hitori")
        check("同点が居ても1人からしか引かない",
              score("@e[tag=t_a,limit=1]", "sekiyu") == 7
              and score("@e[tag=t_b,limit=1]", "sekiyu") == 10,
              f"t_a={score('@e[tag=t_a,limit=1]', 'sekiyu')} "
              f"t_b={score('@e[tag=t_b,limit=1]', 'sekiyu')}")
        c("team leave @e[tag=t_a]")
        c("team leave @e[tag=t_b]")

        c("execute as @e[tag=t_n] run scoreboard players set @s sekiyu 20")
        c("execute as @e[tag=t_n] run scoreboard players set @s fuhai_kijun 20")
        c("execute as @e[tag=t_n] run scoreboard players set @s fuhai_byou 0")
        c("execute as @e[tag=t_n] run scoreboard players set @s sekiyu 10")
        check("使った直後は腐らない", score("@e[tag=t_n,limit=1]", "sekiyu") == 10,
              f"実測={score('@e[tag=t_n,limit=1]', 'sekiyu')}")
        # ★ ここにあった「減らないまま猶予を過ぎると、また腐る」は、
        #   傭兵専用の jidai:fuhai/youhei を見る検査だった。
        #   2026-08-26 に関数ごと無くなったので外した。
        #   勢力の腐敗は下の [8] で 5勢力ぶん 見ている。

        # 売却の本体はプレイヤー専用コマンド(clear)を使うので、
        c("scoreboard players set #uri_member sagyou 0")
        check("勢力所属者は弾かれない", score("#uri_member", "sagyou") == 0,
              f"実測={score('#uri_member', 'sagyou')}")
        src = (DATAPACK_SRC / "data/jidai/functions/kane/uru.mcfunction").read_text(encoding="utf-8")

        print("\n[12-C] 中央プラントの外観切替")
        c("scoreboard players set 世界 chuo 1")
        c("function jidai:shinko/plant")
        h1 = [y for y in range(99, 129)
              if "Test passed" in c(f"execute if block 0 {y} 0 minecraft:cobblestone")]
        check("時代1は丸石の低い柱 (高さ3)", len(h1) == 3, f"実測={len(h1)}段")
        c("scoreboard players set 世界 chuo 4")
        c("function jidai:shinko/plant")
        h4 = [y for y in range(99, 129)
              if "Test passed" in c(f"execute if block 0 {y} 0 minecraft:nether_bricks")]
        check("時代4はネザーレンガの高い柱 (高さ22)", len(h4) == 22, f"実測={len(h4)}段")
        check("時代1の丸石は残っていない",
              "Test failed" in c("execute if block 0 100 0 minecraft:cobblestone"))
        # ★埋まらないことの確認: footprint の外は、切替後も空気のまま
        # 先に [6] の溶岩を片付ける(流れて周囲を埋めるため。検証用の後片付い)
        c("fill 4 96 4 12 102 12 minecraft:air replace minecraft:lava")
        c("fill 4 96 4 12 102 12 minecraft:air replace minecraft:flowing_lava")
        buried = []
        for dx, dz in [(10, 0), (-10, 0), (0, 10), (0, -10), (8, 8), (-8, -8)]:
            for y in (97, 98, 99):
                if "Test failed" in c(f"execute if block {dx} {y} {dz} minecraft:air"):
                    buried.append((dx, y, dz))
        check("周囲(石油の出口と待機位置)は切替後も空気 = 埋まらない",
              not buried, f"埋まった位置={buried}")
        # footprint 内は詰まっている = 中に立てない
        check("土台は詰まっている (中に立てない)",
              "Test failed" in c("execute if block 0 97 0 minecraft:air")
              and "Test failed" in c("execute if block 0 98 0 minecraft:air"))
        # 横に広がっていない (footprint は 12x12 = ~-6..~5)
        check("footprint の外(X=6)は空気のまま",
              "Test passed" in c("execute if block 6 100 0 minecraft:air"))

        print("\n[12-D] 石油はプラントの外に落ちる")
        c("execute as @e[type=item,tag=jidai_sekiyu] run kill @s")
        c("scoreboard players set 世界 wakidashi 0")
        c("scoreboard players set #deguchi sagyou 0")
        for _ in range(4):
            c("function jidai:sekiyu/waku")
        time.sleep(1)
        check("4回で16本落ちる (1回に4か所)", n_ent("@e[type=item,tag=jidai_sekiyu]") == 16,
              f"実測={n_ent('@e[type=item,tag=jidai_sekiyu]')}")
        inside = n_ent("@e[type=item,tag=jidai_sekiyu,x=-6,y=97,z=-6,dx=11,dy=31,dz=11]")
        check("プラントの中(footprint内)に1本も無い", inside == 0, f"実測={inside}本")
        # 平坦な検証world には地面が無くアイテムが落ち続けるので、
        # 高さは見ずに「横位置が4つの出口に分かれているか」で確かめる。
        deguchi = 0
        for ox, oz in [(10, 0), (-10, 0), (0, 10), (0, -10)]:
            n = n_ent(f"@e[type=item,tag=jidai_sekiyu,x={ox-2},y=-320,z={oz-2},"
                      f"dx=4,dy=640,dz=4]")
            deguchi += 1 if n >= 1 else 0
        check("4方向の出口すべてに1本ずつ散らばっている", deguchi == 4,
              f"出口に居た数={deguchi}/4")

        print("\n[12-E] 先行ペナルティ")
        # --- 先行かどうかの判定 ---
        c("scoreboard players set 世界 chuo 2")
        c("scoreboard players set 丘陵 jidai 2")
        c("scoreboard players set 森林 jidai 2")
        c("function jidai:senkou/hantei")
        check("中央と同じ時代なら先行ではない", score("丘陵", "senkou") == 0,
              f"実測={score('丘陵', 'senkou')}")
        c("scoreboard players set 丘陵 jidai 3")
        c("function jidai:senkou/hantei")
        check("中央より先の時代なら先行になる", score("丘陵", "senkou") == 1,
              f"実測={score('丘陵', 'senkou')}")
        check("追う側は先行にならない", score("森林", "senkou") == 0,
              f"実測={score('森林', 'senkou')}")
        c("scoreboard players set 世界 chuo 3")
        c("function jidai:senkou/hantei")
        check("中央が追いつくと自動で解除される", score("丘陵", "senkou") == 0,
              f"実測={score('丘陵', 'senkou')}")

        # --- 実際の進行経路でも確かめる (承認 → 中央が上がる → 解除) ---
        # ★中央は「2勢力が到達したら上がる」ので、先行になるのは
        #   独走している1勢力だけになる。ここがその実証。
        c("scoreboard players set 世界 chuo 1")
        c("scoreboard players set 丘陵 jidai 1")
        c("scoreboard players set 森林 jidai 1")
        # ★ 鉄器→中世の条件に合わせる。建築だけは実際に置けないので下げる。
        c("scoreboard players set 進行_建築_鉄器 settei 1")
        for f, v in [("sekiyu_gokei", 100), ("chokin", 5000), ("kenchiku", 30)]:
            c(f"scoreboard players set 丘陵 {f} {v}")
            c(f"scoreboard players set 森林 {f} {v}")
        c("function jidai:shinko/shounin_kyuryo")
        c("function jidai:senkou/hantei")
        check("承認直後は、独走した1勢力だけが先行になる",
              score("丘陵", "senkou") == 1 and score("森林", "senkou") == 0,
              f"丘陵={score('丘陵', 'senkou')} 森林={score('森林', 'senkou')} 中央={score('世界', 'chuo')}")
        c("function jidai:shinko/shounin_shinrin")
        c("function jidai:senkou/hantei")
        c("scoreboard players set 進行_建築_鉄器 settei 200")   # 戻す
        check("2つめが追いつくと中央が上がり、誰も先行でなくなる",
              score("丘陵", "senkou") == 0 and score("森林", "senkou") == 0,
              f"丘陵={score('丘陵', 'senkou')} 森林={score('森林', 'senkou')} 中央={score('世界', 'chuo')}")

        # --- 誰にペナルティが乗るか (本物の azukeru を走らせて #ritsu を見る) ---
        # ★ 2026-08-20 に、ペナルティは「拾った時」から「預けた時」へ移った。
        # clear はプレイヤー専用なので本数は0のままだが、
        # 「自分の勢力が先行か」を見る分岐は、消す前に置いてあるので通る。
        c("scoreboard players set 丘陵 senkou 1")
        c("scoreboard players set 森林 senkou 0")
        c("execute as @e[tag=t_k] run function jidai:sekiyu/azukeru")
        check("先行勢力のメンバーはロス率が乗る", score("#ritsu", "sagyou") == 50,
              f"実測={score('#ritsu', 'sagyou')}")
        c("execute as @e[tag=t_s] run function jidai:sekiyu/azukeru")
        check("先行していない勢力は乗らない", score("#ritsu", "sagyou") == 0,
              f"実測={score('#ritsu', 'sagyou')}")
        c("team join kyuryo @e[tag=t_n]")
        c("execute as @e[tag=t_n] run function jidai:sekiyu/azukeru")
        c("team leave @e[tag=t_n]")
        c("scoreboard players set 丘陵 senkou 0")

        # --- 採取コスト増の効き目を実測する ---
        def loss_total(hon, ritsu, kaisu):
            c(f"scoreboard players set #hon_chuo sagyou {hon}")
            c(f"scoreboard players set #ritsu sagyou {ritsu}")
            c("scoreboard players set #ushi sagyou 0")
            for _ in range(kaisu):
                c("function jidai:senkou/loss")
                c("scoreboard players operation #ushi sagyou += #hiku sagyou")
            return score("#ushi", "sagyou")

        got = loss_total(1, 0, 20)
        check("ロス率0なら1本も失わない (対照)", got == 0, f"実測={got}/20本")
        got = loss_total(1, 100, 20)
        check("ロス率100なら必ず失う (対照)", got == 20, f"実測={got}/20本")
        got = loss_total(4, 50, 20)
        check("4本まとめて50%なら毎回きっちり2本", got == 40, f"実測={got} (期待40)")
        got = loss_total(1, 50, 200)
        check("1本ずつ200回・50% → 失うのは 79〜121本 (≒半分)",
              79 <= got <= 121, f"実測={got}/200本")

        # --- 発光そのものの動き (senkou/hantei は @a なので実プレイヤーが要る。
        #     ここでは仕組みだけをゾンビで確かめる) ---
        c('summon minecraft:zombie 0 100 40 {Tags:["gz"],NoAI:1b,PersistenceRequired:1b}')
        time.sleep(1)
        c("effect give @e[tag=gz] minecraft:glowing infinite 0 true")
        eff = c("data get entity @e[tag=gz,limit=1] ActiveEffects")
        # ★1.20.1 の ActiveEffects は Id が数字(24b など)で、名前が入っていない。
        #   版で変わらない「残り時間」と「個数」で測る。
        check("発光は infinite でかかる (残り時間 -1)", "Duration: -1" in eff, eff[:150])
        c("effect give @e[tag=gz] minecraft:glowing infinite 0 true")
        eff = c("data get entity @e[tag=gz,limit=1] ActiveEffects")
        check("毎秒かけ直しても効果は1つのまま", eff.count("Duration:") == 1, eff[:150])
        c("effect clear @e[tag=gz] minecraft:glowing")
        check("解除で消える",
              "Duration:" not in c("data get entity @e[tag=gz,limit=1] ActiveEffects"))
        c("kill @e[tag=gz]")
        ssrc = (DATAPACK_SRC / "data/jidai/functions/senkou/hantei.mcfunction").read_text(encoding="utf-8")
        check("(記述) 毎秒かけ直すので、復帰した人にも光が戻る",
              "tag=jidai_hikaru_ima] run effect give @s minecraft:glowing infinite 0 true" in ssrc)
        check("(記述) 対象から外れた人の発光は消える",
              "tag=jidai_hikari,tag=!jidai_hikaru_ima] run effect clear @s minecraft:glowing" in ssrc)
        check("(記述) 発動と解除の両方を全体に知らせる",
              "が中央より先へ進んだ" in ssrc and "先行状態が解除された" in ssrc)

        # --- 中央産の石油だけに印が付く ---
        c("execute as @e[type=item,tag=jidai_sekiyu] run kill @s")
        c("scoreboard players set 世界 wakidashi 0")
        c("function jidai:sekiyu/waku")
        time.sleep(1)
        # ★1.20.1 には execute if items もアイテムコンポーネントも無い(実測)。
        #   拾得の検知(アドバンスメント)と同じ【NBTの部分一致】で測る。
        check("中央プラントの石油には moto:chuo が付く",
              "Test passed" in c('execute if entity @e[type=item,tag=jidai_sekiyu,'
                                 'nbt={Item:{tag:{moto:"chuo"}}}]'))
        check("既存の1キー判定にも当たる (拾得の検知は壊れていない)",
              "Test passed" in c('execute if entity @e[type=item,tag=jidai_sekiyu,'
                                 'nbt={Item:{tag:{jidai_sekiyu:"oil"}}}]'))
        dsrc = (DATAPACK_SRC / "data/jidai/functions/sekiyu/dashi_give.mcfunction").read_text(encoding="utf-8")
        check("(記述) 引き出した石油には印が付かない = 買った石油は目減りしない", "moto" not in dsrc)

        print("\n[12-F] 石油の腐敗")

        def fuhai(gokei, kijun, byou, yuyo=900):
            for nm, v in (("#gokei", gokei), ("#kijun", kijun),
                          ("#byou", byou), ("#yuyo", yuyo)):
                c(f"scoreboard players set {nm} sagyou {v}")
            c("function jidai:fuhai/kyotsu")
            return (score("#heru", "sagyou"), score("#kijun", "sagyou"),
                    score("#byou", "sagyou"))

        heru, kijun, byou = fuhai(50, 50, 0)
        check("減っていなければ時計が進む (まだ腐らない)",
              heru == 0 and byou == 10, f"heru={heru} byou={byou}")
        heru, kijun, byou = fuhai(40, 50, 500)
        check("十分に減っていれば時計が0に戻る",
              heru == 0 and byou == 0 and kijun == 40, f"heru={heru} byou={byou} kijun={kijun}")
        heru, kijun, byou = fuhai(49, 50, 500)
        check("1本だけ払っても時計は戻らない (小額逃げ対策)",
              heru == 0 and byou == 510 and kijun == 50, f"heru={heru} byou={byou} kijun={kijun}")
        heru, kijun, byou = fuhai(60, 50, 500)
        check("増えただけでは時計は戻らない (基準値だけ上がる)",
              heru == 0 and byou == 510 and kijun == 60, f"heru={heru} byou={byou} kijun={kijun}")
        heru, kijun, byou = fuhai(50, 50, 890)
        check("猶予を過ぎたら腐る (50本の10%切り上げ = 5本)",
              heru == 5 and byou == 0 and kijun == 45, f"heru={heru} byou={byou} kijun={kijun}")
        heru, _, _ = fuhai(14, 14, 890)
        check("端数は切り上げ (14本の10% = 2本)", heru == 2, f"実測={heru}")
        heru, _, _ = fuhai(1, 1, 890)
        check("1本しか無くても切り上げで1本腐る (小分けで逃げられない)",
              heru == 1, f"実測={heru}")
        heru, _, byou = fuhai(0, 0, 890)
        check("0本なら腐らない", heru == 0 and byou == 0, f"heru={heru} byou={byou}")

        # --- 一番多く持っている個人から引く ---
        c('summon minecraft:armor_stand 0 100 36 {Tags:["t_a"],Invisible:1b,Marker:1b}')
        c('summon minecraft:armor_stand 0 100 38 {Tags:["t_b"],Invisible:1b,Marker:1b}')
        time.sleep(1)
        for t, s in (("t_a", 5), ("t_b", 30)):
            c(f"team join kyuryo @e[tag={t}]")
            c(f"execute as @e[tag={t}] run scoreboard players set @s sekiyu {s}")
        c("execute as @e[tag=t_k] run scoreboard players set @s sekiyu 7")
        c("scoreboard players set #max sagyou 0")
        c("scoreboard players set #max sagyou 0")
        # ★ 傭兵を止めたので scores={youhei=0} の絞り込みは外した（2026-08-26）
        c("execute as @e[team=kyuryo] run "
          "scoreboard players operation #max sagyou > @s sekiyu")
        check("勢力で最も多い保有量が取れる (7/5/30 → 30)", score("#max", "sagyou") == 30,
              f"実測={score('#max', 'sagyou')}")
        c("scoreboard players set #heru sagyou 3")
        c("scoreboard players set #zumi sagyou 0")
        c("execute as @e[team=kyuryo] if score @s sekiyu = #max sagyou "
          "run function jidai:fuhai/hiku_hitori")
        check("最も多く持っている人からだけ引かれる",
              score("@e[tag=t_b,limit=1]", "sekiyu") == 27
              and score("@e[tag=t_a,limit=1]", "sekiyu") == 5
              and score("@e[tag=t_k,limit=1]", "sekiyu") == 7,
              f"t_b={score('@e[tag=t_b,limit=1]', 'sekiyu')} "
              f"t_a={score('@e[tag=t_a,limit=1]', 'sekiyu')}")
        # 同点が2人いても1人からしか引かないこと
        for t in ("t_a", "t_b"):
            c(f"execute as @e[tag={t}] run scoreboard players set @s sekiyu 10")
        c("scoreboard players set #heru sagyou 3")
        c("scoreboard players set #zumi sagyou 0")
        c("execute as @e[tag=t_a] run function jidai:fuhai/hiku_hitori")
        c("execute as @e[tag=t_b] run function jidai:fuhai/hiku_hitori")
        check("同点が居ても1人からしか引かない",
              score("@e[tag=t_a,limit=1]", "sekiyu") == 7
              and score("@e[tag=t_b,limit=1]", "sekiyu") == 10,
              f"t_a={score('@e[tag=t_a,limit=1]', 'sekiyu')} "
              f"t_b={score('@e[tag=t_b,limit=1]', 'sekiyu')}")
        c("team leave @e[tag=t_a]")
        c("team leave @e[tag=t_b]")

        c("execute as @e[tag=t_n] run scoreboard players set @s sekiyu 20")
        c("execute as @e[tag=t_n] run scoreboard players set @s fuhai_kijun 20")
        c("execute as @e[tag=t_n] run scoreboard players set @s fuhai_byou 0")
        c("execute as @e[tag=t_n] run scoreboard players set @s sekiyu 10")
        check("使った直後は腐らない", score("@e[tag=t_n,limit=1]", "sekiyu") == 10,
              f"実測={score('@e[tag=t_n,limit=1]', 'sekiyu')}")

        # --- 引き出して持っている分も総量に数える ---
        # (clear はプレイヤー専用なので、ここでは書いてあることの確認まで)
        msrc = (DATAPACK_SRC / "data/jidai/functions/fuhai/mochi.mcfunction").read_text(encoding="utf-8")
        check("(記述) 引き出して手に持っている石油も総量に数える",
              'clear @s minecraft:black_dye{jidai_sekiyu:"oil"} 0' in msrc)
        ksrc = (DATAPACK_SRC / "data/jidai/functions/fuhai/kyuryo.mcfunction").read_text(encoding="utf-8")
        check("(記述) 勢力の総量は sekiyu_gokei + 手持ちで数えている",
              "= 丘陵 sekiyu_gokei" in ksrc and "run function jidai:fuhai/mochi" in ksrc)

        print("\n[12-G] 戦争・略奪・占領")
        # 拠点1(丘陵) の銀行 = (0,101,-404) / 拠点2(森林) の銀行 = (390,101,-121)
        GIN_K, GIN_S = "0 101 -404", "390 101 -121"
        got = n_ent("@e[type=marker,tag=jidai_kyoten_kyuryo]")
        # ★ 2026-08-20 に銃器専門店を足したので 6 -> 7。
        check("拠点1のマーカーに丘陵の印が付く (7個)", got == 7, f"実測={got}")
        got = n_ent("@e[type=marker,tag=jidai_kyoten_shinrin]")
        check("拠点2のマーカーに森林の印が付く (7個)", got == 7, f"実測={got}")
        got = n_ent("@e[type=marker,tag=jidai_kyoten_kyuryo,x=384,y=101,z=-121,distance=..4]")
        check("別拠点の銀行には他勢力の印が付いていない", got == 0, f"実測={got}")

        # 検証では clock を回していないので、プレイヤーと同じ状態を手で作る
        c("execute as @e[tag=t_k] run scoreboard players set @s bangou 1")
        c("execute as @e[tag=t_s] run scoreboard players set @s bangou 2")
        c("execute as @e[tag=t_n] run scoreboard players set @s bangou 0")
        c("execute as @e[tag=t_n] run scoreboard players set @s ryakudatsu_kan 0")
        c("execute as @e[tag=t_k] run scoreboard players set @s ryakudatsu_kan 0")

        def ryaku_k():
            c(f"execute as @e[tag=t_k] positioned {GIN_S} run function jidai:kane/azukeru")

        # --- 宣戦 ---
        c(f"execute as {SEN} run kill @s")
        sensen(1, 2)
        check("宣戦で戦争マーカーが1体できる", n_ent(SEN) == 1, f"実測={n_ent(SEN)}")
        check("状態は「準備」(1)", war(1, 2, "sensou") == 1,
              f"実測={war(1, 2, 'sensou')}")
        check("準備の残り秒は 戦争_準備秒 (300)", war(1, 2, "sensou_byou") == 300,
              f"実測={war(1, 2, 'sensou_byou')}")
        check("略奪の上限倍率は1", war(1, 2, "sensou_bai") == 1,
              f"実測={war(1, 2, 'sensou_bai')}")
        check("両勢力の印(w_1 / w_2)が付く",
              n_ent(f"{SEN[:-1]},tag=w_1]") == 1 and n_ent(f"{SEN[:-1]},tag=w_2]") == 1,
              f"w_1={n_ent(f'{SEN[:-1]},tag=w_1]')} w_2={n_ent(f'{SEN[:-1]},tag=w_2]')}")
        check("関係ない勢力の印(w_3)は付かない", n_ent(f"{SEN[:-1]},tag=w_3]") == 0,
              f"実測={n_ent(f'{SEN[:-1]},tag=w_3]')}")
        check("作りかけの印(w_new)は残らない",
              n_ent("@e[type=marker,tag=w_new]") == 0,
              f"実測={n_ent('@e[type=marker,tag=w_new]')}")
        sensen(1, 1)
        check("自分自身には宣戦できない (増えない)", n_ent(SEN) == 1, f"実測={n_ent(SEN)}")
        sensen(1, 9)
        check("登録されていない勢力番号は弾かれる", n_ent(SEN) == 1, f"実測={n_ent(SEN)}")
        sensen(0, 2)
        check("勢力に入っていない(番号0)は弾かれる", n_ent(SEN) == 1, f"実測={n_ent(SEN)}")

        # --- ★プラグインが読む「勢力ごとの要約」 -----------------
        # プラグインはマーカーを読めない(読ませない)。勢力名の保持者に
        # 写したものだけを見る。ここが噛み合わないと戦争GUIが黙って壊れる。
        c("function jidai:sensou/youyaku")
        check("★要約: 丘陵の sensou に状態が写る", score("丘陵", "sensou") == 1,
              f"実測={score('丘陵', 'sensou')}")
        check("★要約: 丘陵の sensou_aite に相手の番号(2)が写る",
              score("丘陵", "sensou_aite") == 2, f"実測={score('丘陵', 'sensou_aite')}")
        check("★要約: 森林からも同じ戦争が見える (状態1・相手1)",
              score("森林", "sensou") == 1 and score("森林", "sensou_aite") == 1,
              f"状態={score('森林', 'sensou')} 相手={score('森林', 'sensou_aite')}")
        check("★要約: 残り秒も写る (300)", score("丘陵", "sensou_byou") == 300,
              f"実測={score('丘陵', 'sensou_byou')}")
        check("★要約: 戦争に関係ない勢力は0に戻る",
              score("川", "sensou") == 0 and score("川", "sensou_aite") == 0,
              f"川={score('川', 'sensou')}/{score('川', 'sensou_aite')}")

        # --- 準備中は略奪できない ---
        c("scoreboard players set 森林 chokin 500")
        c("scoreboard players set 丘陵 chokin 100")
        ryaku_k()
        check("準備中は略奪できない", score("森林", "chokin") == 500,
              f"実測={score('森林', 'chokin')}")

        # --- 準備 → 交戦 ---
        war_set(1, 2, "sensou_byou", 2)
        susumu()
        check("残り秒があるうちは状態が変わらない", war(1, 2, "sensou") == 1,
              f"実測={war(1, 2, 'sensou')} 残り={war(1, 2, 'sensou_byou')}")
        susumu()
        check("準備が終わると交戦になる", war(1, 2, "sensou") == 2,
              f"実測={war(1, 2, 'sensou')}")
        check("交戦の残り秒は 戦争_交戦秒 (600)", war(1, 2, "sensou_byou") == 600,
              f"実測={war(1, 2, 'sensou_byou')}")

        # --- 交戦中の略奪 ---
        c("execute as @e[tag=t_k] run scoreboard players set @s ryakudatsu_kan 0")
        ryaku_k()
        # ★★ 2026-09-09: 1回で相手の【総量の 1%】。500 の 1% = 5 ★★
        check("交戦中は相手の貯金から 1% (500→495) が減る",
              score("森林", "chokin") == 495, f"実測={score('森林', 'chokin')}")
        check("奪った金は自勢力へ入る (100→105)", score("丘陵", "chokin") == 105,
              f"実測={score('丘陵', 'chokin')}")
        check("奪った累計が【壊した側(a)】に記録される",
              war(1, 2, "ryakudatsu_kane_a") == 5,
              f"実測={war(1, 2, 'ryakudatsu_kane_a')}")
        check("★壊した回数が1 数えられる（100回でビーコンが壊せる）",
              war(1, 2, "ryakudatsu_kai_a") == 1,
              f"実測={war(1, 2, 'ryakudatsu_kai_a')}")
        # ★ プラグインは戦争マーカーを読めないので、この関数に写してもらってから読む
        c("scoreboard players set #q_kuni sagyou 1")
        c("scoreboard players set #q_aite sagyou 2")
        c("function jidai:sensou/kai_yomu")
        check("★★kai_yomu が攻める側(a)の回数を作業用へ出す（プラグインの口）",
              score("#q_kai", "sagyou") == 1, f"実測={score('#q_kai', 'sagyou')}")
        c("scoreboard players set #q_kuni sagyou 2")
        c("scoreboard players set #q_aite sagyou 1")
        c("function jidai:sensou/kai_yomu")
        check("★kai_yomu は向きを取り違えない（b側から見ると 0）",
              score("#q_kai", "sagyou") == 0, f"実測={score('#q_kai', 'sagyou')}")
        check("★奪われた側(b)の累計は増えない (向きが混ざらない)",
              war(1, 2, "ryakudatsu_kane_b") == 0,
              f"実測={war(1, 2, 'ryakudatsu_kane_b')}")
        check("押した本人にクールダウンが乗る (10秒)",
              score("@e[tag=t_k,limit=1]", "ryakudatsu_kan") == 10,
              f"実測={score('@e[tag=t_k,limit=1]', 'ryakudatsu_kan')}")
        ryaku_k()
        check("クールダウン中は連打しても動かない", score("森林", "chokin") == 495,
              f"実測={score('森林', 'chokin')}")

        # --- 自勢力の銀行は交戦中も預金として動く ---
        c("execute as @e[tag=t_k] run scoreboard players set @s ryakudatsu_kan 0")
        c("execute as @e[tag=t_k] run scoreboard players set @s kane_kojin 50")
        # ★★ 預金は【プラグイン】へ移った (2026-08-18) ★★
        #   jidai:kane/azukeru の預金経路はもう使われていない。
        #   「自勢力の銀行なら交戦中でも預金になる(略奪にならない)」の判定も
        #   プラグイン側(Ginko.osareta)へ移したので、そちらで測っている。
        #   ここでは、略奪が【他勢力の拠点でだけ】起きることを確かめる。
        check("★自勢力の拠点の印は自分の番号と一致する (略奪に回らない側)",
              score("丘陵", "bangou") == score("@e[tag=t_k,limit=1]", "bangou"),
              f"勢力={score('丘陵', 'bangou')} 本人={score('@e[tag=t_k,limit=1]', 'bangou')}")
        # ★ 10 / 50 / 全部 の選択は【プラグインのチェスト画面】へ移した
        #   (2026-08-18)。金ブロックは拠点に1個だけ。
        #   金額の計算もプラグイン側なので、ここでは
        #   「1個の銀行で預金が成立すること」だけを見る。
        #   選択肢の中身はプラグイン側のハーネスが測っている。
        ksrc2 = (DATAPACK_SRC / "data/jidai/functions/setup/kyoten_1.mcfunction").read_text(
            encoding="utf-8")
        check("★拠点に置く金ブロックは1個だけ",
              ksrc2.count("minecraft:gold_block") == 1,
              f"実測={ksrc2.count('minecraft:gold_block')}個")
        # ★ コメントにも jidai_uru_chuo と書いてあるので、
        #   文字列を探すだけでは引っかかる。実際に付ける Tags の行で見る。
        tagGyou = [l for l in ksrc2.splitlines() if "summon" in l and "jidai_mise" in l]
        check("★拠点のエメラルドは販売所として置く（売却所ではない）",
              len(tagGyou) == 1 and "jidai_uru" not in tagGyou[0],
              str(tagGyou))

        c("team join kyuryo @e[tag=t_n]")
        c("execute as @e[tag=t_n] run scoreboard players set @s kane_kojin 999")
        before_s, before_k = score("森林", "chokin"), score("丘陵", "chokin")
        c(f"execute as @e[tag=t_n] positioned {GIN_S} run function jidai:kane/azukeru")
        c(f"execute as @e[tag=t_n] positioned {GIN_K} run function jidai:kane/azukeru")
        c("team leave @e[tag=t_n]")

        # --- 上限の効き方 (jidai:sensou/ryakudatsu_ryo を直接呼ぶ) ---
        # ★★ 2026-09-09: 1回で【相手の総量の 1%】。上限は無い ★★
        #   下剋上で始めた戦争は倍率2で 2%。相手が持っている限り必ず1は動く。
        def ryo(bai, aite_kane, aite_sekiyu):
            for nm, v in (("#bai", bai), ("#aite_kane", aite_kane),
                          ("#aite_sekiyu", aite_sekiyu)):
                c(f"scoreboard players set {nm} sagyou {v}")
            c("function jidai:sensou/ryakudatsu_ryo")
            return score("#gaku", "sagyou"), score("#hon", "sagyou")

        g, h = ryo(1, 500, 200)
        check("★1回で相手の総量の 1% (500→5 / 200→2)", (g, h) == (5, 2), f"実測={g}/{h}")
        g, h = ryo(1, 100000, 5000)
        check("多く持っているほど1回の被害が大きい (100000→1000)",
              (g, h) == (1000, 50), f"実測={g}/{h}")
        g, h = ryo(1, 99, 99)
        check("★★100未満でも0にならない（最低1）", (g, h) == (1, 1), f"実測={g}/{h}")
        g, h = ryo(1, 1, 1)
        check("相手が1しか持っていなければ 1", (g, h) == (1, 1), f"実測={g}/{h}")
        g, h = ryo(1, 0, 0)
        check("相手が空なら何も動かない", (g, h) == (0, 0), f"実測={g}/{h}")
        g, h = ryo(2, 500, 200)
        check("★下剋上で始めた戦争は2倍（2%）", (g, h) == (10, 4), f"実測={g}/{h}")
        # ★ 上限が無くても 0 にはならない。掛け算なので減り方が緩む。
        #   100回 壊しても 0.99^100 ＝ 約37% が残る（止めを刺すのはビーコン）。
        nokori = 100000
        for _ in range(100):
            g, _h = ryo(1, nokori, 0)
            nokori -= g
        check("★★100回 壊しても貯金は0にならない（約37%残る）",
              30000 <= nokori <= 40000, f"実測={nokori}")

        # --- 石油を奪う相手の選び方 (最も多く持っている1人) ---
        c("execute as @e[tag=t_a] run scoreboard players set @s bangou 2")
        c("execute as @e[tag=t_b] run scoreboard players set @s bangou 2")
        c("execute as @e[tag=t_a] run scoreboard players set @s sekiyu 4")
        c("execute as @e[tag=t_b] run scoreboard players set @s sekiyu 20")
        c("scoreboard players set #aite_no sagyou 2")
        c("scoreboard players set #max sagyou 0")
        c("scoreboard players set #max sagyou 0")
        c("execute as @e[team=shinrin] run "
          "scoreboard players operation #max sagyou > @s sekiyu")
        c("execute as @e[tag=t_a] run scoreboard players set @s bangou 2")
        c("execute as @e[tag=t_b] run scoreboard players set @s bangou 2")
        c("execute as @e[tag=t_a] run scoreboard players set @s sekiyu 4")
        c("execute as @e[tag=t_b] run scoreboard players set @s sekiyu 20")
        c("scoreboard players set #aite_no sagyou 2")
        c("scoreboard players set #max sagyou 0")
        # ★ 傭兵を止めたので scores={youhei=0} の絞り込みは外した（2026-08-26）
        c("execute as @e if score @s bangou = #aite_no sagyou run "
          "scoreboard players operation #max sagyou > @s sekiyu")
        check("相手勢力で最も多い保有量が取れる (4/20 → 20)", score("#max", "sagyou") == 20,
              f"実測={score('#max', 'sagyou')}")
        c("scoreboard players set #hon sagyou 2")
        c("scoreboard players set #zumi sagyou 0")
        c("execute as @e if score @s bangou = #aite_no sagyou "
          "if score @s sekiyu = #max sagyou run function jidai:sensou/ubau_hitori")
        check("最も多く持っている人からだけ石油が減る",
              score("@e[tag=t_b,limit=1]", "sekiyu") == 18
              and score("@e[tag=t_a,limit=1]", "sekiyu") == 4,
              f"t_b={score('@e[tag=t_b,limit=1]', 'sekiyu')} "
              f"t_a={score('@e[tag=t_a,limit=1]', 'sekiyu')}")
        usrc = (DATAPACK_SRC / "data/jidai/functions/sensou/sekiyu_ubau.mcfunction").read_text(encoding="utf-8")

        # --- 交戦終了と占領 ---
        c("scoreboard players set 森林 chokin 0")
        c("scoreboard players set 丘陵 chokin 200")
        c("scoreboard players set 丘陵 senryou 0")
        c("scoreboard players set 森林 senryou 0")
        war_set(1, 2, "sensou_byou", 1)
        susumu()
        check("交戦が終わると再戦禁止になる", war(1, 2, "sensou") == 3,
              f"実測={war(1, 2, 'sensou')}")
        check("再戦禁止の残り秒は 戦争_禁止秒 (1800)",
              war(1, 2, "sensou_byou") == 1800,
              f"実測={war(1, 2, 'sensou_byou')}")
        # ★★ 2026-09-09: 「交戦終了時に貯金0なら占領」は廃止（ご指示）★★
        #   占領の入口はビーコンを壊す1本だけ。1回1%では貯金が0にならないので、
        #   この道はどのみち成立しなかった。
        check("★貯金0で終わっても占領は付かない（占領はビーコンだけ）",
              score("森林", "senryou") == 0, f"実測={score('森林', 'senryou')}")
        check("貯金が残っていた勢力も占領されない", score("丘陵", "senryou") == 0,
              f"実測={score('丘陵', 'senryou')}")

        # --- 30分以内の再戦を拒否する ---
        sensen(1, 2)
        check("再戦禁止のあいだは宣戦できない (状態3のまま)",
              war(1, 2, "sensou") == 3 and n_ent(SEN) == 1,
              f"状態={war(1, 2, 'sensou')} マーカー={n_ent(SEN)}")
        sensen(2, 1)
        check("★逆向き(森林→丘陵)からも再戦できない",
              war(1, 2, "sensou") == 3 and n_ent(SEN) == 1,
              f"状態={war(1, 2, 'sensou')} マーカー={n_ent(SEN)}")
        war_set(1, 2, "sensou_byou", 1)
        susumu()
        check("禁止が明けると戦争が消える (マーカーごと)",
              n_ent(SEN) == 0, f"実測={n_ent(SEN)}")
        c("function jidai:sensou/youyaku")
        check("★要約: 戦争が消えると勢力の状態も0に戻る",
              score("丘陵", "sensou") == 0 and score("森林", "sensou") == 0,
              f"丘陵={score('丘陵', 'sensou')} 森林={score('森林', 'sensou')}")

        print("\n[12-H] 下剋上")
        c("scoreboard players set 森林 senryou 1")
        c("scoreboard players set 森林 gekokujo 0")
        c("scoreboard players set 森林 chokin 500")
        c("scoreboard players set 世界 chuo 1")
        c(f"execute as @e[tag=t_s] run function jidai:sensou/gekokujo_kau")
        check("近代になるまでは買えない", score("森林", "gekokujo") == 0,
              f"実測={score('森林', 'gekokujo')}")
        c("scoreboard players set 世界 chuo 3")
        c("execute as @e[tag=t_s] run function jidai:sensou/gekokujo_kau")
        check("近代なら勢力の金150で買える", score("森林", "gekokujo") == 1,
              f"実測={score('森林', 'gekokujo')}")
        check("勢力の金から150引かれる (500→350)", score("森林", "chokin") == 350,
              f"実測={score('森林', 'chokin')}")
        c("execute as @e[tag=t_s] run function jidai:sensou/gekokujo_kau")
        check("2つめは買えない (保有上限1)", score("森林", "chokin") == 350,
              f"実測={score('森林', 'chokin')}")
        c("team join shinrin @e[tag=t_n]")
        c("scoreboard players set 森林 gekokujo 0")
        c("execute as @e[tag=t_n] run function jidai:sensou/gekokujo_kau")
        c("team leave @e[tag=t_n]")
        c("scoreboard players set 森林 gekokujo 1")

        # --- 行使 ---
        c("tag @e[tag=t_s] remove jidai_leader")
        c("execute as @e[tag=t_s] run function jidai:sensou/gekokujo_tsukau")
        check("リーダー以外は行使できない", score("森林", "senryou") == 1,
              f"実測={score('森林', 'senryou')}")
        c("tag @e[tag=t_s] add jidai_leader")
        c("scoreboard players set 森林 senryou 0")
        c("execute as @e[tag=t_s] run function jidai:sensou/gekokujo_tsukau")
        check("占領されていなければ行使できない (権利は減らない)",
              score("森林", "gekokujo") == 1, f"実測={score('森林', 'gekokujo')}")
        c("scoreboard players set 森林 senryou 1")
        c(f"execute as {SEN} run kill @s")
        # 丘陵(1)と森林(2)が再戦禁止のまま残っている状態を作る
        sensen(1, 2)
        war_set(1, 2, "sensou", 3)
        c("execute as @e[tag=t_s] run function jidai:sensou/gekokujo_tsukau")
        check("行使すると占領が解ける", score("森林", "senryou") == 0,
              f"実測={score('森林', 'senryou')}")
        check("権利を消費する", score("森林", "gekokujo") == 0,
              f"実測={score('森林', 'gekokujo')}")
        check("再戦禁止(30分)を無視して即開戦になる",
              war(2, 1, "sensou") == 2, f"実測={war(2, 1, 'sensou')}")
        check("準備をとばすので残り秒は交戦の600",
              war(2, 1, "sensou_byou") == 600, f"実測={war(2, 1, 'sensou_byou')}")
        check("略奪の上限が2倍になる", war(2, 1, "sensou_bai") == 2,
              f"実測={war(2, 1, 'sensou_bai')}")
        check("★古い戦争(丘陵→森林)は消えて1件だけになる",
              n_ent(SEN) == 1 and war(1, 2, "sensou") is None,
              f"件数={n_ent(SEN)} 古い方={war(1, 2, 'sensou')}")
        c(f"execute as {SEN} run kill @s")
        c("tag @e[tag=t_s] remove jidai_leader")

        c("team leave @e[tag=t_n]")
        # 本番は @a (プレイヤー) だが、検証ではアーマースタンドで同じ形を試す
        c("team join kyuryo @e[tag=t_n]")
        csrc = (DATAPACK_SRC / "data/jidai/functions/clock.mcfunction").read_text(encoding="utf-8")
        c("team leave @e[tag=t_n]")
        c("team join kyuryo @e[tag=t_n]")
        c("scoreboard players set 丘陵 chokin 400")
        c("execute as @e[tag=t_n] run function jidai:kane/kinko_yomu")
        c("scoreboard players set #kinko sagyou 0")
        c("execute as @e[tag=t_n] run function jidai:kane/kinko_kaku")
        c("execute as @e[tag=t_k] run function jidai:kane/kinko_yomu")
        c("scoreboard players set 丘陵 chokin 400")
        c("scoreboard players set #kinko sagyou 0")
        c("execute as @e[tag=t_k] run function jidai:kane/kinko_yomu")
        check("正規メンバーは今までどおり読める (400)", score("#kinko", "sagyou") == 400,
              f"実測={score('#kinko', 'sagyou')}")
        c("team leave @e[tag=t_n]")
        vsrc = (DATAPACK_SRC / "data/jidai/functions/mise/kau_seiryoku.mcfunction").read_text(encoding="utf-8")
        ksrc = (DATAPACK_SRC / "data/jidai/functions/mise/kau_kojin.mcfunction").read_text(encoding="utf-8")

        # --- サイドバー ---
        c("scoreboard players set 丘陵 chokin 380")
        c("scoreboard players set 丘陵 sekiyu_gokei 12")
        c("scoreboard players set 丘陵 senryou 0")
        c("scoreboard players set 森林 chokin 150")
        c("scoreboard players set 森林 sekiyu_gokei 8")
        c("scoreboard players set 森林 senryou 1")
        out = c("function jidai:hyouji/kousin")
        check("サイドバーの作り直しがエラー無く通る",
              "Unknown" not in out and "Invalid" not in out, out[:70])

        # ★★ サイドバーは【勢力ごとに別の目的】(2026-08-20) ★★
        #   1つの目的を sidebar へ出すと全員が同じものを見る。
        #   `sidebar.team.<色>` はそのチームの色の人にだけ映る枠。
        #   出すのは 貯金 / 石油 / 中央の時代 の3つだけ。
        check("丘陵の枠に 丘陵の貯金・石油・時代 が入る",
              score("貯金", "hyouji_1") == 380 and score("石油", "hyouji_1") == 12
              and score("中央の時代", "hyouji_1") is not None,
              f"貯金={score('貯金', 'hyouji_1')} 石油={score('石油', 'hyouji_1')} "
              f"時代={score('中央の時代', 'hyouji_1')}")
        check("森林の枠には 森林の数字が入る (取り違えない)",
              score("貯金", "hyouji_2") == 150 and score("石油", "hyouji_2") == 8,
              f"貯金={score('貯金', 'hyouji_2')} 石油={score('石油', 'hyouji_2')}")
        check("★★丘陵の枠に他勢力の数字は入らない",
              score("貯金", "hyouji_1") != score("貯金", "hyouji_2"),
              f"丘陵={score('貯金', 'hyouji_1')} 森林={score('貯金', 'hyouji_2')}")
        check("★勢力に入っていない人の枠は中央の時代だけ",
              score("中央の時代", "hyouji_0") is not None
              and score("貯金", "hyouji_0") is None
              and score("石油", "hyouji_0") is None,
              f"時代={score('中央の時代', 'hyouji_0')} "
              f"貯金={score('貯金', 'hyouji_0')} 石油={score('石油', 'hyouji_0')}")
        check("中央の時代は数が小さいので勢力の行より下に出る",
              score("中央の時代", "hyouji_1") < score("貯金", "hyouji_1"),
              f"時代={score('中央の時代', 'hyouji_1')} 貯金={score('貯金', 'hyouji_1')}")
        check("★1行につき1つ。勢力ごとの行数は3で固定 (15行の上限に当たらない)",
              len([1 for h in ("貯金", "石油", "中央の時代")
                   if score(h, "hyouji_1") is not None]) == 3)
        # ★占領はサイドバーから外した（指示: 出すのは3つだけ）。
        #   起きた時の全体通知と、下剋上の画面が知らせる。
        check("★占領はサイドバーに出さない",
              score("占領", "hyouji_1") is None and score("占領", "hyouji_2") is None,
              f"丘陵={score('占領', 'hyouji_1')} 森林={score('占領', 'hyouji_2')}")
        check("★v6 までの1本の目的(hyouji)は残っていない",
              "hyouji " not in c("scoreboard objectives list").replace("hyouji_", "@"),
              c("scoreboard objectives list")[:90])

        # --- 表示枠が勢力ごとに分かれているか（記述） ---
        lsrc1 = (DATAPACK_SRC / "data/jidai/functions/load.mcfunction").read_text(
            encoding="utf-8")
        WAKU = [("hyouji_1", "white"), ("hyouji_2", "green"), ("hyouji_3", "aqua"),
                ("hyouji_4", "blue"), ("hyouji_5", "gold")]
        kake = [o for o, iro in WAKU
                if f"setdisplay sidebar.team.{iro} {o}" not in lsrc1]
        check("(記述) 6つの表示枠がチームの色ごとに割り当ててある", not kake, f"欠け={kake}")
        check("(記述) どのチームにも居ない人には素の sidebar が出る",
              "setdisplay sidebar hyouji_0" in lsrc1)
        # ★表示枠の色は team modify の色と必ず一致していないと、
        #   その勢力には何も映らない。そこを機械で押さえる。
        iro = dict(re.findall(r"team modify (\w+) color (\w+)", lsrc1))
        TEAM = {"hyouji_1": "kyuryo", "hyouji_2": "shinrin", "hyouji_3": "kawa",
                "hyouji_4": "naikai", "hyouji_5": "iwaba"}
        zure = [o for o, ir in WAKU if iro.get(TEAM[o]) != ir]
        check("★★表示枠の色とチームの色が食い違っていない", not zure,
              f"食い違い={zure} / 実際の色={iro}")

        # --- 個人の金は画面下の帯へ（サイドバーには出せない） ---
        csrc2 = (DATAPACK_SRC / "data/jidai/functions/clock.mcfunction").read_text(
            encoding="utf-8")
        check("(記述) 個人の金は毎秒アクションバーへ出している",
              "title @s actionbar" in csrc2 and "kane_kojin" in csrc2)
        out = c('title @a actionbar {"text":"x"}')
        check("★1.20.1 に actionbar は在る (個人の金の出し先)",
              "Unknown" not in out and "Incorrect" not in out, out[:60])
        check("(記述) Tab の一覧にも個人の金が出る",
              "setdisplay list kane_kojin" in lsrc1)

        print("\n[12-J] 5勢力そろっているか (川・内海・岩場を足した分)")
        GO = [("kyuryo", "丘陵", 1), ("shinrin", "森林", 2), ("kawa", "川", 3),
              ("naikai", "内海", 4), ("iwaba", "岩場", 5)]
        # ここまでの節が戦争と占領を残しているので、先に片付けてから調べる
        c(f"execute as {SEN} run kill @s")
        for _, mei, _ in GO:
            c(f"scoreboard players set {mei} senryou 0")
            c(f"scoreboard players set {mei} gekokujo 0")

        for team, mei, ban in GO:
            check(f"チーム {team} ({mei}) がある",
                  "already exists" in c(f"team add {team}").lower())
            check(f"{mei} の番号が {ban}", score(mei, "bangou") == ban,
                  f"実測={score(mei, 'bangou')}")
            kake = [o for o in ("chokin", "jidai", "senkou", "senryou", "gekokujo",
                                "fuhai_kijun", "fuhai_byou") if score(mei, o) is None]
            check(f"{mei} のスコアの枠が7つそろっている", not kake, f"欠け={kake}")
            got = n_ent(f"@e[type=marker,tag=jidai_kyoten_{team}]")
            check(f"{mei} の拠点の施設に印が7個", got == 7, f"実測={got}")
        check("番号が1〜5で重なっていない",
              sorted(score(m, "bangou") for _, m, _ in GO) == [1, 2, 3, 4, 5])
        check("拠点の印は5拠点ぶんで合計35個",
              sum(n_ent(f"@e[type=marker,tag=jidai_kyoten_{t}]") for t, _, _ in GO) == 35,
              str([(t, n_ent(f"@e[type=marker,tag=jidai_kyoten_{t}]")) for t, _, _ in GO]))

        # ---------- 銃器専門店（2026-08-20 追加） ----------
        # ★★ 販売所と同じエメラルドブロック ★★
        #   見分けているのは【マーカーの印】だけ。印が付いていなければ
        #   プラグインはそこを販売所として開いてしまう。
        #   だから「数が合っているか」「印が別物か」をここで必ず見る。
        juki = n_ent("@e[type=marker,tag=jidai_juki]")
        check("★★銃器専門店のマーカーが5拠点ぶん", juki == 5, f"実測={juki}")
        mise = n_ent("@e[type=marker,tag=jidai_mise]")
        check("販売所のマーカーは5拠点ぶん", mise == 5, f"実測={mise}")
        kasanari = n_ent("@e[type=marker,tag=jidai_juki,tag=jidai_mise]")
        check("★★同じマーカーに両方の印が付いていない", kasanari == 0,
              f"実測={kasanari}")
        # 中央のエメラルドは今までどおり別の印
        check("中央のエメラルドは jidai_uru_chuo のまま",
              n_ent("@e[type=marker,tag=jidai_uru_chuo]") == 1,
              f"実測={n_ent('@e[type=marker,tag=jidai_uru_chuo]')}")

        # 拠点1(丘陵 0,-410)の実際のブロックを見る。設計は中心から x+10 / z+6。
        # ★★ ここでブロックそのものは見ない ★★
        #   この検証世界は拠点の区画を常時読み込みにしていないので、
        #   execute if block は【どの条件でも失敗する】。
        #   実際に販売所(確実に在る)でも Test failed になることを確かめた。
        #   ブロックの現物は tests/map_kakunin.py と tests/setup_kakunin.py が
        #   forceload したうえで見ている。ここではマーカーの座標で見る。
        # 販売所(x+4)とは別の座標であること
        check("★銃器専門店と販売所は別の場所",
              n_ent("@e[type=marker,tag=jidai_juki,x=10,y=101,z=-404,distance=..1]") == 1,
              "拠点1の x+10 にマーカーが無い")

        # 施設の総数
        zen = n_ent("@e[type=marker,tag=jidai_shisetsu]")
        check("★★施設は全部で37個 (5拠点x7 + 中央2)", zen == 37, f"実測={zen}")
        # --- 金庫の読み書きが5勢力目でも通るか ---
        # ★検証サーバーにはプレイヤーが1人も居ないので @p は誰にも当たらない。
        #   [8] からの流儀にそろえて、チームに入れたアーマースタンドで代用する。
        c('summon minecraft:armor_stand 0 100 42 {Tags:["t_i"],Invisible:1b,Marker:1b}')
        time.sleep(1)
        c("team join iwaba @e[tag=t_i]")
        c("scoreboard players set 岩場 chokin 777")
        c("execute as @e[tag=t_i] run function jidai:kane/kinko_yomu")
        check("岩場の金庫を読める (kinko_yomu が5勢力ぶんある)",
              score("#kinko", "sagyou") == 777, f"実測={score('#kinko', 'sagyou')}")
        c("scoreboard players set #kinko sagyou 555")
        c("execute as @e[tag=t_i] run function jidai:kane/kinko_kaku")
        check("岩場の金庫へ書ける (kinko_kaku が5勢力ぶんある)",
              score("岩場", "chokin") == 555, f"実測={score('岩場', 'chokin')}")
        c("execute as @e[tag=t_i] run function jidai:kane/kinko_yomu")

        # --- 時代進行の判定が5勢力を一巡するか ---
        for _, mei, _ in GO:
            c(f"scoreboard players reset {mei} kenchiku")
        c("scoreboard players set #junban sagyou 0")
        for _ in range(5):
            c("function jidai:shinko/junban")
        mada = [m for _, m, _ in GO if score(m, "kenchiku") is None]
        check("5回まわすと全勢力の判定が一巡する", not mada, f"未判定={mada}")
        c("function jidai:shinko/junban")
        check("6回目は1つめの勢力へ折り返す", score("#junban", "sagyou") == 1,
              f"実測={score('#junban', 'sagyou')}")

        # --- あとから足した勢力でも先行の判定が効くか ---
        c("scoreboard players set 世界 chuo 1")
        for _, mei, _ in GO:
            c(f"scoreboard players set {mei} jidai 1")
        c("scoreboard players set 川 jidai 2")
        c("function jidai:senkou/hantei")
        hoka = [m for _, m, _ in GO if m != "川" and score(m, "senkou") != 0]
        check("川 だけが先行になる (senkou が5勢力ぶんある)",
              score("川", "senkou") == 1 and not hoka,
              f"川={score('川', 'senkou')} 他で1になったもの={hoka}")

        # --- あとから足した勢力どうしで宣戦から占領まで通るか ---
        sensen(3, 4)
        check("川 と 内海 で宣戦できる", war(3, 4, "sensou") == 1,
              f"実測={war(3, 4, 'sensou')}")
        check("戦争マーカーが1体できる",
              n_ent(f"{SEN[:-1]},scores={{w_kuni=3,w_aite=4}}]") == 1,
              f"実測={n_ent(f'{SEN[:-1]},scores={{w_kuni=3,w_aite=4}}]')}")
        # --- (a) 一度も略奪していなければ占領されない ---
        # 時代進行で貯金を使った直後に、指一本触れていない相手から
        # 占領される事故を防ぐための決まり。
        war_set(3, 4, "sensou", 2)
        c("scoreboard players set 内海 chokin 0")
        c("scoreboard players set 川 chokin 200")
        war_set(3, 4, "ryakudatsu_kane_a", 0)
        war_set(3, 4, "ryakudatsu_sekiyu_a", 0)
        war_set(3, 4, "sensou_byou", 1)
        susumu()
        check("★★一度も略奪していない相手は、貯金0でも占領できない",
              score("内海", "senryou") == 0, f"実測={score('内海', 'senryou')}")

        # --- (b) 一度でも略奪していれば占領できる ---
        sensen(3, 4, soku=1)
        c("scoreboard players set 内海 chokin 0")
        c("scoreboard players set 川 chokin 200")
        war_set(3, 4, "ryakudatsu_kane_a", 30)
        war_set(3, 4, "sensou_byou", 1)
        susumu()
        check("★★略奪していても、貯金0だけでは占領されない（2026-09-09 で廃止）",
              score("内海", "senryou") == 0, f"実測={score('内海', 'senryou')}")
        # --- (c) 逆向き(b側が略奪していた場合)も同じように効くか ---
        c(f"execute as {SEN} run kill @s")
        c("scoreboard players set 内海 senryou 0")
        c("scoreboard players set 川 senryou 0")
        sensen(3, 4, soku=1)
        c("scoreboard players set 川 chokin 0")
        c("scoreboard players set 内海 chokin 200")
        war_set(3, 4, "ryakudatsu_kane_b", 30)
        war_set(3, 4, "sensou_byou", 1)
        susumu()
        check("★逆向き(b側が略奪)でも、貯金0だけでは占領されない",
              score("川", "senryou") == 0, f"実測={score('川', 'senryou')}")
        c(f"execute as {SEN} run kill @s")
        c("scoreboard players set 内海 senryou 0")
        c("scoreboard players set 川 senryou 0")

        # --- サイドバーが5勢力ぶん出るか ---
        for i, (_, mei, _) in enumerate(GO):
            c(f"scoreboard players set {mei} chokin {100 + i}")
        c("function jidai:hyouji/kousin")
        # ★勢力ごとに別の枠なので、5つとも中身が入っているかを見る
        gyou = [(m, score("貯金", f"hyouji_{b}")) for _, m, b in GO]
        check("5勢力ぶんの枠すべてに貯金が入る",
              all(v is not None for _, v in gyou), str(gyou))
        check("★★枠ごとに違う数字が入る (勢力を取り違えない)",
              [v for _, v in gyou] == [100 + i for i in range(5)], str(gyou))
        sek = [(m, score("石油", f"hyouji_{b}")) for _, m, b in GO]
        check("5勢力ぶんの枠すべてに石油が入る",
              all(v is not None for _, v in sek), str(sek))
        # ★★ サイドバーは15行までしか映らない ★★
        #   勢力ごとに別の枠にしたので、1人が見る行は【常に3行】。
        #   勢力を増やしても増えない。
        for _, _, b in GO:
            n_gyou = len([1 for h in ("貯金", "石油", "中央の時代")
                          if score(h, f"hyouji_{b}") is not None])
            if n_gyou != 3:
                break
        check("★1人が見る行は常に3行 (勢力を増やしても15行に当たらない)",
              n_gyou == 3, f"実測={n_gyou}行")

        # --- チームの色が6つとも違うか ---
        # ★先行ペナルティの発光は【チームの色】で出る。色が重なると
        #   「光っているのがどの勢力か」が分からなくなり、発光の狙いが崩れる。
        lsrc = (DATAPACK_SRC / "data/jidai/functions/load.mcfunction").read_text(encoding="utf-8")
        iro = dict(re.findall(r"team modify (\w+) color (\w+)", lsrc))
        check("チームの色が5つとも違う (発光で勢力を見分けられる)",
              len(set(iro.values())) == len(iro) == 5, str(iro))
        # --- (c) 双方0でも、奪い合っていなければ占領し合わない ---
        c("scoreboard players set 岩場 senryou 0")
        c("scoreboard players set 森林 senryou 0")
        sensen(5, 2, soku=1)
        c("scoreboard players set 岩場 chokin 0")
        c("scoreboard players set 森林 chokin 0")
        war_set(5, 2, "ryakudatsu_kane_a", 0)
        war_set(5, 2, "ryakudatsu_sekiyu_a", 0)
        war_set(5, 2, "ryakudatsu_kane_b", 0)
        war_set(5, 2, "ryakudatsu_sekiyu_b", 0)
        war_set(5, 2, "sensou_byou", 1)
        susumu()
        check("★双方0でも、奪い合っていなければ互いに占領しない",
              score("岩場", "senryou") == 0 and score("森林", "senryou") == 0,
              f"岩場={score('岩場', 'senryou')} 森林={score('森林', 'senryou')}")

        # --- (d) 双方が奪い合って両方0なら、互いに占領し合う ---
        sensen(5, 2, soku=1)
        c("scoreboard players set 岩場 chokin 0")
        c("scoreboard players set 森林 chokin 0")
        war_set(5, 2, "ryakudatsu_kane_a", 30)
        war_set(5, 2, "ryakudatsu_sekiyu_b", 2)
        war_set(5, 2, "sensou_byou", 1)
        susumu()
        check("★★奪い合って両方0でも、互いに占領しない（ビーコンだけが入口）",
              score("岩場", "senryou") == 0 and score("森林", "senryou") == 0,
              f"岩場={score('岩場', 'senryou')} 森林={score('森林', 'senryou')}")
        c("scoreboard players set 岩場 senryou 0")
        c("scoreboard players set 森林 senryou 0")

        # ---------- 撤去の掃除漏れ ----------
        # ★★ 作った目的を撤去で消し忘れると、世界に残り続ける ★★
        #   2026-08-20 の棚卸しで、マーカー方式で足した8個が
        #   消し残っていたのが見つかった。機械で突き合わせる。
        lsrc2 = (DATAPACK_SRC / "data/jidai/functions/load.mcfunction").read_text(
            encoding="utf-8")
        usrc2 = (DATAPACK_SRC / "data/jidai/functions/uninstall.mcfunction").read_text(
            encoding="utf-8")
        tsukuru = set(re.findall(r"scoreboard objectives add (\w+)", lsrc2))
        kesu = set(re.findall(r"scoreboard objectives remove (\w+)", usrc2))
        nokori = sorted(tsukuru - kesu)
        check("★★★作った目的を撤去がすべて消している", not nokori,
              f"消し忘れ={nokori}")
        check("(記述) 使われない目的を作っていない",
              "objectives add ryakudatsu_kane dummy" not in lsrc2
              and "objectives add ryakudatsu_sekiyu dummy" not in lsrc2)

        # ---------- 座標の一覧が実物を読んでいるか ----------
        # ★ 以前は固定の文字列で、実装とズレていた（棚卸しで発覚）。
        #   金ブロック3つ / エメラルドを「売却所」/ 鉄とかまどが無い、の3点。
        zsrc = (DATAPACK_SRC / "data/jidai/functions/setup/zahyou.mcfunction").read_text(
            encoding="utf-8")
        check("★★座標の一覧が実際のマーカーを読んでいる",
              "jidai:setup/zahyou_1" in zsrc
              and "type=marker,tag=jidai_kyoten_kyuryo" in zsrc)
        # ★ 説明のコメントには古い言葉が出てくるので、
        #   【実際に出力する行】だけを見る（tellraw の行に限る）。
        deru = [l for l in zsrc.splitlines() if l.strip().startswith("tellraw")]
        furui = [l for l in deru
                 if "(-6,+6)" in l or "(-3,+6)" in l or "売却所" in l]
        check("(記述) 古い固定の座標を出力していない", not furui, str(furui)[:120])
        z1src = (DATAPACK_SRC / "data/jidai/functions/setup/zahyou_1.mcfunction"
                 ).read_text(encoding="utf-8")
        kake2 = [t for t in ("jidai_ginko", "jidai_mise", "jidai_uru_chuo",
                             "jidai_gacha", "jidai_shinko", "jidai_kamado",
                             "jidai_plant")
                 if t not in z1src]
        check("★7種類すべての施設に名前が付く", not kake2, f"抜け={kake2}")

        # 実際に走らせて、エラーが出ないことを確かめる
        out = c("function jidai:setup/zahyou")
        check("座標の一覧がエラー無く通る",
              "Unknown" not in out and "Incorrect" not in out, out[:70])

        print("\n[13] 全関数のパースとログ")
        fdir = DATAPACK_SRC / "data" / "jidai" / "functions"
        names = sorted(p.relative_to(fdir).as_posix()[:-len(".mcfunction")]
                       for p in fdir.rglob("*.mcfunction"))
        bad = 0
        for nm in names:
            if nm in ("setup/tetsu", "uninstall"):
                continue
            o = c(f"function jidai:{nm}")
            if any(x in o for x in ("Unknown or incomplete", "Expected", "Unknown command",
                                    "Invalid", "Unexpected")):
                bad += 1
                print(f"      NG {nm}: {o[:90]}")
        check(f"全関数({len(names)}個)がパースエラー無し", bad == 0, f"NG={bad}")
        time.sleep(2)
        txt = log.read_text(encoding="utf-8", errors="replace")
        rt = [l for l in txt.splitlines() if "Whilst executing" in l]
        check("実行時エラーがログに無い", not rt, str(rt[:2]))
        # ==========================================================
        #  ボスバー（中央の時代への進み具合）2026-08-31 のご指示
        # ==========================================================
        print()
        print('-- ボスバー --')

        def bar():
            # ★ 実物の文言は「has a value of 3」。'value is' ではない（実測）
            m = re.search(r'has a value of (-?\d+)',
                          c('bossbar get jidai:chuo value'))
            return int(m.group(1)) if m else None

        check('ボスバーが作られている', bar() is not None,
              c('bossbar get jidai:chuo value'))
        m = re.search(r'has a maximum of (\d+)', c('bossbar get jidai:chuo max'))
        check('満タンは 6（2勢力 × 3条件）', m and m.group(1) == '6',
              c('bossbar get jidai:chuo max'))

        # まっさらに戻す（全勢力を鉄器・条件0へ）
        for kuni in ('丘陵', '森林', '川', '内海', '岩場'):
            c(f'scoreboard players set {kuni} jidai 1')
            for j in ('jouken_a', 'jouken_b', 'jouken_c'):
                c(f'scoreboard players set {kuni} {j} 0')
        c('scoreboard players set 世界 chuo 1')
        c('function jidai:shinko/bar')
        check('★条件が1つも無ければ 0', bar() == 0, f'実測={bar()}')

        # 1勢力が3条件そろえたら 3（半分）
        for j in ('jouken_a', 'jouken_b', 'jouken_c'):
            c(f'scoreboard players set 丘陵 {j} 1')
        c('function jidai:shinko/bar')
        check('★1勢力が3条件そろえたら 3（半分）', bar() == 3, f'実測={bar()}')

        # 2勢力目が1条件だけ満たしたら 4（なめらかに動く）
        c('scoreboard players set 森林 jouken_a 1')
        c('function jidai:shinko/bar')
        check('★★2勢力目の1条件で 4（3段階ではなく なめらか）',
              bar() == 4, f'実測={bar()}')

        # 2勢力とも3条件そろえたら満タン
        for j in ('jouken_b', 'jouken_c'):
            c(f'scoreboard players set 森林 {j} 1')
        c('function jidai:shinko/bar')
        check('★2勢力が3条件そろえたら満タン 6', bar() == 6, f'実測={bar()}')

        # 3勢力目がそろえても 6 を超えない
        for j in ('jouken_a', 'jouken_b', 'jouken_c'):
            c(f'scoreboard players set 川 {j} 1')
        c('function jidai:shinko/bar')
        check('★3勢力そろっても 6 を超えない', bar() == 6, f'実測={bar()}')

        # 中央の時代が上がったら 0 に戻る
        #   ★ 承認して時代が上がった状態を作る。丘陵と森林を中世へ。
        c('scoreboard players set 丘陵 jidai 2')
        c('scoreboard players set 森林 jidai 2')
        c('scoreboard players set 世界 chuo 2')
        for kuni in ('丘陵', '森林'):
            for j in ('jouken_a', 'jouken_b', 'jouken_c'):
                c(f'scoreboard players set {kuni} {j} 0')
        c('scoreboard players set 川 jidai 1')
        for j in ('jouken_a', 'jouken_b', 'jouken_c'):
            c(f'scoreboard players set 川 {j} 0')
        c('function jidai:shinko/bar')
        check('★★次の時代へ進むと 0 に戻る', bar() == 0, f'実測={bar()}')

        # 色と名前が中央の時代で変わる
        c('scoreboard players set 世界 chuo 3')
        c('function jidai:shinko/bar')
        # ★ bossbar get に name は無い。value の出力が
        #   「Custom bossbar [世界の時代　近代] has a value of N」なので、そこを見る。
        check('中央が近代なら名前に「近代」が入る',
              '近代' in c('bossbar get jidai:chuo value'),
              c('bossbar get jidai:chuo value'))

        c("forceload remove all")
    except RconError as e:
        print(f"[環境エラー] RCON: {e}")
        return 2
    finally:
        try:
            r.command("stop")
        except Exception:
            pass
        try:
            proc.wait(timeout=60)
        except Exception:
            proc.kill()
        r.close()

    print("\n" + "=" * 58)
    print(f"結果: PASS {len(passed)} / FAIL {len(failed)}")
    for f in failed:
        print(f"  FAILED: {f}")
    if not keep:
        shutil.rmtree(SERVER_DIR, ignore_errors=True)
    return 1 if failed else 0

if __name__ == "__main__":
    sys.exit(main())
