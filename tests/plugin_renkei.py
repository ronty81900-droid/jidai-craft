# -*- coding: utf-8 -*-
"""
プラグイン ⇄ データパック の連携検証 (Java Edition 1.20.1)

**プラグインが実際に送った命令を、本物のデータパックへ流して確かめる。**

verify_jidai.py も toushi_5.py も、データパックだけを動かしている。
プラグイン側のハーネスは、データパックの真似(スタブ)を相手にしている。
つまり「プラグインの出す文字列が、本物に受理されるか」は
どちらも見ていない。そこだけを見るのがこのファイル。

命令は手で写さない。プラグインのハーネスが書き出した
tests/plugin_cmds.txt をそのまま読む。写し間違いを起こさないため。

  1. プラグイン側で SeiryokuTest を走らせて plugin_cmds.txt を作る
  2. python tests/plugin_renkei.py

★ プレイヤーが要る命令 (execute as <名前>) は、本物の人でしか完全には
  試せない。ここでは「命令として成り立つか」と「関数の中身が正しく効くか」
  を分けて見ている。前者は名前を出したまま、後者は試験用の entity に
  置き換えて確かめる。

終了コード: 0=全部通った / 1=どこかで落ちた / 2=環境要因で実行不可
"""
import re
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_jidai as V

CMDS_FILE = Path(__file__).resolve().parent / "plugin_cmds.txt"

# 「命令として成り立っていない」ことを示す返事
NG = ("Unknown or incomplete command", "Unknown function", "Failed to instantiate",
      "Expected ", "Incorrect argument", "Unknown command", "Invalid or unknown")

passed, failed = [], []

def check(label, cond, detail=""):
    (passed if cond else failed).append(label)
    print("  [" + ("PASS" if cond else "FAIL") + "] " + label
          + ("  " + detail if detail else ""))

def main():
    keep = "--keep" in sys.argv
    print("=== プラグインの命令を本物のデータパックへ流す (1.20.1) ===")
    if not CMDS_FILE.exists():
        print("[環境エラー] " + str(CMDS_FILE) + " が無い。")
        print("  先にプラグイン側の SeiryokuTest を、書き出し先を引数にして走らせること。")
        return 2
    cmds = [l.strip() for l in CMDS_FILE.read_text(encoding="utf-8").splitlines()
            if l.strip()]
    print("読み込んだ命令: " + str(len(cmds)) + " 種類")
    if not V.SERVER_JAR.exists():
        print("[環境エラー] サーバー jar が無い: " + str(V.SERVER_JAR))
        return 2
    V.prepare()
    proc, log, ok = V.start()
    if not ok:
        print("[環境エラー] 起動失敗")
        proc.kill()
        return 2
    r = V.RconClient(port=V.RPORT, password=V.PW, timeout=180.0)

    def c(x):
        return r.command(x).strip()

    def score(h, o):
        m = re.search(r"has (-?\d+)", c("scoreboard players get " + h + " " + o))
        return int(m.group(1)) if m else None

    # ★★ 戦争はマーカー1体で1件（1.20.1 移行）★★
    #   プラグインは勢力名の保持者（要約）しか読まないが、
    #   ここでは「命令が本当にマーカーを立てたか」まで見たいので、
    #   マーカーのスコアも読めるようにしておく。
    SEN = "@e[type=marker,tag=jidai_sensou]"

    def war(a, b, obj):
        c("scoreboard players set #yomu sagyou -999")
        c("execute as " + SEN[:-1] + ",scores={w_kuni=" + str(a) + ",w_aite=" + str(b)
          + "}] run scoreboard players operation #yomu sagyou = @s " + obj)
        v = score("#yomu", "sagyou")
        return None if v == -999 else v

    def war_set(a, b, obj, v):
        c("execute as " + SEN[:-1] + ",scores={w_kuni=" + str(a) + ",w_aite=" + str(b)
          + "}] run scoreboard players set @s " + obj + " " + str(v))

    try:
        r.connect()
        c("function jidai:load")
        c("function jidai:setup/kyoten")
        # 試験用の「人」。x,z とも 0〜15 に置く (読み込まれている区画の中)
        c('summon minecraft:armor_stand 1 100 1 '
          '{Tags:["t_p"],Invisible:1b,Marker:1b,NoGravity:1b}')
        c("team join shinrin @e[tag=t_p]")

        # ---------- 1) 命令として成り立つか ----------
        # ★ ここが要。soku を書き忘れると Failed to instantiate function で
        #   止まるが、プラグイン側のスタブでは絶対に気づけない。
        print("\n[1] プラグインの命令が、本物のサーバーで命令として成り立つか")
        for cmd in cmds:
            res = c(cmd)
            warui = [n for n in NG if n in res]
            check("命令が成り立つ: " + cmd[:58], not warui, res[:80])

        # ---------- 2) 宣戦がちゃんと効くか ----------
        print("\n[2] 宣戦の命令が、状態を本当に動かすか")
        # ★★ 1.20.1 では関数に引数を渡せない ★★
        #   プラグインは「スコアを3つ置いてから関数を呼ぶ」形で送る。
        #   その【並び】が崩れると、前回の値が残ったまま別の戦争が始まる。
        #   ここでは書き出された命令をその順に流して、本当に効くかを見る。
        # ★ plugin_cmds.txt は【重複を除いた一覧】なので、並び順は当てにしない。
        #   種類がそろっていることを確かめ、送る順はここで組み立てる。
        kuni = [x for x in cmds if x.startswith("scoreboard players set #w_kuni")]
        aite = [x for x in cmds if x.startswith("scoreboard players set #w_aite")]
        soku_l = [x for x in cmds if x.startswith("scoreboard players set #w_soku")]
        hon = [x for x in cmds if x == "function jidai:sensou/sensen"]
        check("宣戦の命令が書き出されている（スコア3種＋関数）",
              kuni and aite and soku_l and hon,
              f"kuni={len(kuni)} aite={len(aite)} soku={len(soku_l)} 関数={len(hon)}")
        check("★soku は 0 と 1 の両方が書き出されている（下剋上の即開戦ぶん）",
              any(x.endswith(" 0") for x in soku_l)
              and any(x.endswith(" 1") for x in soku_l), str(soku_l))
        okuru = [kuni[0], aite[0], soku_l[0], hon[0]]

        def sensen_okuru(soku):
            """書き出された命令を、soku だけ差し替えて、送る順に流す"""
            c(f"execute as {SEN} run kill @s")
            for x in okuru:
                if x.startswith("scoreboard players set #w_soku"):
                    c("scoreboard players set #w_soku sagyou " + str(soku))
                else:
                    c(x)

        sensen_okuru(0)
        check("★★#w_soku 0 で【準備(1)】から始まる", war(1, 2, "sensou") == 1,
              "実測=" + str(war(1, 2, "sensou")))
        check("準備の残り秒が 戦争_準備秒 (300)", war(1, 2, "sensou_byou") == 300,
              "実測=" + str(war(1, 2, "sensou_byou")))
        check("戦争マーカーがちょうど1体できる",
              "count: 1" in c("execute if entity " + SEN).lower(),
              c("execute if entity " + SEN)[:50])

        # ★プラグインが読むのは【要約】のほう。そこまで届いて初めて意味がある。
        c("function jidai:sensou/youyaku")
        check("★★★プラグインが読む要約まで届く（丘陵 sensou=1 / 相手=2）",
              score("丘陵", "sensou") == 1 and score("丘陵", "sensou_aite") == 2,
              "状態=" + str(score("丘陵", "sensou"))
              + " 相手=" + str(score("丘陵", "sensou_aite")))
        check("相手側(森林)からも同じ戦争が見える",
              score("森林", "sensou") == 1 and score("森林", "sensou_aite") == 1,
              "状態=" + str(score("森林", "sensou"))
              + " 相手=" + str(score("森林", "sensou_aite")))

        sensen_okuru(1)
        check("★★#w_soku 1 で【交戦(2)】から始まり、略奪の割合が2倍になる",
              war(1, 2, "sensou") == 2 and war(1, 2, "sensou_bai") == 2,
              "状態=" + str(war(1, 2, "sensou"))
              + " 倍率=" + str(war(1, 2, "sensou_bai")))

        # ★ プラグインは「呼んだあと要約を作り直して読み直し、
        #   立っていなければ返金」する。その読み直しが本物でも効くか。
        sensen_okuru(0)
        war_set(1, 2, "sensou", 3)   # 再戦禁止にしておく
        c("function jidai:sensou/youyaku")
        for x in okuru:
            c(x)
        c("function jidai:sensou/youyaku")
        check("★★★再戦禁止中は、命令が通っても状態が変わらない"
              "（プラグインはこれを見て返金する）",
              score("丘陵", "sensou") == 3, "実測=" + str(score("丘陵", "sensou")))
        c(f"execute as {SEN} run kill @s")
        c("function jidai:sensou/youyaku")
        check("★戦争が消えると要約も 0 に戻る（返金の判定がこれに懸かる）",
              score("丘陵", "sensou") == 0, "実測=" + str(score("丘陵", "sensou")))

        # ---------- 2b) 略奪の3本 ----------
        print("\n[2b] 略奪の命令が、番号でちゃんと届くか")
        ryaku = [x for x in cmds if "sensou/ryakudatsu" in x or "#jibun_no" in x
                 or "sensou/aite_yomu" in x]
        check("略奪の3本が書き出されている（with storage は使っていない）",
              len(ryaku) == 3 and not any("with storage" in x for x in ryaku),
              str(len(ryaku)))
        # 交戦中の状態を作り、拠点の番号を手で置いてから本体だけ流す
        sensen_okuru(1)
        c("scoreboard players set 森林 chokin 500")
        c("scoreboard players set 丘陵 chokin 0")
        c("execute as @e[tag=t_p] run scoreboard players set @s ryakudatsu_kan 0")
        c("scoreboard players set #jibun_no sagyou 1")
        c("scoreboard players set #aite_no sagyou 2")
        naka = [x for x in ryaku if x.endswith("sensou/ryakudatsu")][0]
        c("execute as @e[tag=t_p] at @e[tag=t_p] run "
          + naka.split(" run ", 1)[1])
        # ★★ 2026-09-09: 1回で相手の総量の 1% ★★
        #   ★ 期待値を直書きしない。この戦争は #w_soku 1（下剋上の形）で始めたので
        #     倍率が2＝2% になっている。**実際の倍率から出す**
        #     （直書きすると「実装は正しいのに検査だけ落ちる」ことになる。実際に一度そうなった）。
        bai = war(1, 2, "sensou_bai") or 1
        ugoku = 500 * bai // 100
        check("★★略奪が効く（森林 500→%d / 丘陵 0→%d ・倍率%d）"
              % (500 - ugoku, ugoku, bai),
              score("森林", "chokin") == 500 - ugoku
              and score("丘陵", "chokin") == ugoku,
              "森林=" + str(score("森林", "chokin"))
              + " 丘陵=" + str(score("丘陵", "chokin")))
        c(f"execute as {SEN} run kill @s")
        c("function jidai:sensou/youyaku")

        # ---------- 3) リーダーの目印 ----------
        print("\n[3] リーダーの目印を付ける命令")
        tags = [x for x in cmds if x.startswith("tag ")]
        check("目印を付ける命令と外す命令の両方がある",
              any(" add jidai_leader" in x for x in tags)
              and any(" remove jidai_leader" in x for x in tags),
              str(len(tags)) + " 種類")
        # 中身が効くかは、試験用の entity で確かめる
        c("tag @e[tag=t_p] add jidai_leader")
        n = c("execute if entity @e[tag=t_p,tag=jidai_leader]")
        check("★jidai_leader が実際に付く（データパックはこれを見る）",
              "Test passed" in n or "count: 1" in n, n[:60])

        # ---------- 4) 下剋上の関数 ----------
        print("\n[4] 下剋上の関数が、本人として走ると効くか")
        geko = [x for x in cmds if "gekokujo_tsukau" in x]
        check("下剋上の命令が書き出されている", len(geko) >= 1, str(len(geko)))
        if geko:
            # execute as <名前> の <名前> を、試験用の entity に差し替える
            naka = geko[0].split(" run ", 1)[1]
            c("scoreboard players set 森林 gekokujo 1")
            c("scoreboard players set 森林 senryou 1")
            c("scoreboard players set 丘陵 bangou 1")
            c("execute as @e[tag=t_p] at @e[tag=t_p] run " + naka)
            check("★★目印つきのリーダーが走らせると、占領が解ける",
                  score("森林", "senryou") == 0,
                  "実測=" + str(score("森林", "senryou")))
            check("権利が消費される", score("森林", "gekokujo") == 0,
                  "実測=" + str(score("森林", "gekokujo")))

            # ★ 目印が無いと断られること = プラグインが目印を付ける義務の裏取り
            c("tag @e[tag=t_p] remove jidai_leader")
            c("scoreboard players set 森林 gekokujo 1")
            c("scoreboard players set 森林 senryou 1")
            c("execute as @e[tag=t_p] at @e[tag=t_p] run " + naka)
            check("★★★目印が無いと断られる（プラグインが付け忘れたら死ぬ）",
                  score("森林", "senryou") == 1 and score("森林", "gekokujo") == 1,
                  "占領=" + str(score("森林", "senryou"))
                  + " 権利=" + str(score("森林", "gekokujo")))

        # ---------- 5) 下剋上を買う関数 ----------
        print("\n[5] 下剋上を買う関数")
        kau = [x for x in cmds if "gekokujo_kau" in x]
        check("購入の命令が書き出されている", len(kau) >= 1, str(len(kau)))
        if kau:
            naka = kau[0].split(" run ", 1)[1]
            c("tag @e[tag=t_p] add jidai_leader")
            c("scoreboard players set 森林 gekokujo 0")
            c("scoreboard players set 森林 chokin 1000")
            c("scoreboard players set 世界 chuo 3")
            c("execute as @e[tag=t_p] at @e[tag=t_p] run " + naka)
            check("★★データパック側が 150 を引いて権利を渡す（プラグインは引かない）",
                  score("森林", "chokin") == 850 and score("森林", "gekokujo") == 1,
                  "貯金=" + str(score("森林", "chokin"))
                  + " 権利=" + str(score("森林", "gekokujo")))

        # ---------- 5.5) 販売所が出す give の書式 ----------
        # ★★ ここが今回いちばん危ない所 ★★
        #   銃と弾は Bukkit の Material で表せないので、give コマンドで渡す。
        #   1.20.1 は「アイテム名{NBT}」、1.21 は「アイテム名[部品]」で
        #   書式が違う。書式を間違えると、実機で黙って何も渡らない。
        #   ここでは【バニラのアイテムに差し替えて】書式だけを確かめる。
        #   TaCZ のアイテムはバニラのサーバーに存在しないため、
        #   ID そのものの正しさは tests/buki_kakunin.py が実物の jar で見る。
        print(chr(10) + "[5.5] 販売所が出す give の書式が 1.20.1 で通るか")
        shop = (V.ROOT / "plugin/src/main/java/jidai/Shop.java").read_text(encoding="utf-8")
        # ★ Java のソース上では \" と書かれている。そこまで含めて拾う。
        #   単純に .*? にすると \" の手前で切れ、途中までしか取れない。
        katachi = re.findall(r'(tacz:[a-z_]+\{[A-Za-z]+Id:\\"[a-z_0-9:]+\\"\})', shop)
        # ★ 銃を 5丁1種 に整理した時（v0.56.0）から 15 → 6 になっていた。
        #   この検査だけ 15 のまま取り残されていた（2026-09-09 に気付いた）。
        check("販売所に give 用の文字列が 6 個ある（銃5丁＋弾1種）",
              len(katachi) == 6, str(len(katachi)))

        # 書式だけを見る。中カッコの NBT が 1.20.1 で受理されるか。
        res = c('give @p minecraft:stone{jidai_kakunin:"ok"} 1')
        check("★★1.20.1 は アイテム名{NBT} の書式を受け付ける",
              not any(n in res for n in NG), res[:90])
        # 1.21 の書式は【通らない】ことも確かめる（逆戻り防止）
        res2 = c('give @p minecraft:stone[custom_name="x"] 1')
        check("★1.21 の [部品] 書式は 1.20.1 では通らない",
              any(n in res2 for n in NG), res2[:90])

        # 組み立てた命令の形そのもの（アイテム名だけ差し替え）
        for k in katachi[:3]:
            # Java の \" を、実際の命令の " に戻す
            nbt = k[k.index("{"):].replace('\\"', '"')
            r2 = c("give @p minecraft:stone" + nbt + " 1")
            check("give の形が成り立つ: " + k[:44],
                  not any(n in r2 for n in NG), r2[:80])

        # ---------- 5.55) 銀行が出す「石油を預ける」の命令 ----------
        # ★ 画面のボタンは、データパックの関数を呼ぶだけ。
        #   その命令が本物のサーバーで通るかをここで見る。
        print(chr(10) + "[5.55] 銀行の石油預けの命令が通るか")
        ginko = (V.ROOT / "plugin/src/main/java/jidai/Ginko.java").read_text(
            encoding="utf-8")
        check("銀行が jidai:sekiyu/azukeru を呼んでいる",
              "jidai:sekiyu/azukeru" in ginko, "呼んでいない")
        r3 = c("execute as @e[tag=t_p] run function jidai:sekiyu/azukeru")
        check("★★預ける関数が本物のサーバーで通る",
              not any(n in r3 for n in NG), r3[:90])

        # ---------- 5.6) 石油の栓: 名前が両側で一致しているか ----------
        # ★ プラグインが書く保持者名と、データパックが読む名前がズレると、
        #   コマンドは成功するのに何も起きない。文字列なので誰も気付けない。
        print(chr(10) + "[5.6] 石油の栓の名前が、プラグインとデータパックで一致するか")
        kane = (V.ROOT / "plugin/src/main/java/jidai/Kane.java").read_text(encoding="utf-8")
        load = (V.DATAPACK_SRC / "data/jidai/functions/load.mcfunction").read_text(
            encoding="utf-8")
        for na in ("石油_停止", "石油_倍率"):
            check("プラグインが " + na + " を書いている", '"' + na + '"' in kane)
            check("データパックが " + na + " を用意している", na + " settei" in load)
        clock = (V.DATAPACK_SRC / "data/jidai/functions/clock.mcfunction").read_text(
            encoding="utf-8")
        check("★止めている時に湧きを飛ばしている",
              "石油_停止 settei matches 0" in clock, "clock が停止を見ていない")
        check("★倍率を間隔に掛けている",
              "石油_間隔秒 settei /= 石油_倍率 settei" in clock, "掛けていない")
        # ---------- 6) ログ ----------
        print("\n[6] ログにエラーが出ていない")
        txt = log.read_text(encoding="utf-8", errors="replace")
        ng = [l for l in txt.splitlines()
              if ("Failed to instantiate" in l or "Unknown function" in l
                  or "Whilst executing" in l)]
        check("実行時エラーがログに無い", not ng, str(ng[:3]))

    finally:
        try:
            c("stop")
            proc.wait(timeout=60)
        except Exception:
            proc.kill()
        finally:
            r.close()

    print("\n" + "=" * 58)
    print("結果: PASS " + str(len(passed)) + " / FAIL " + str(len(failed)))
    for f in failed:
        print("  FAILED: " + f)
    if not keep:
        shutil.rmtree(V.SERVER_DIR, ignore_errors=True)
    return 1 if failed else 0

if __name__ == "__main__":
    sys.exit(main())
