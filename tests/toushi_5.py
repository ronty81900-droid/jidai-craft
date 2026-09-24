# -*- coding: utf-8 -*-
"""
5勢力の通し稼働 (対象: Java Edition 1.20.1 / データパック jidai_craft)

verify_jidai.py が「部品ごとの検証」なのに対し、こちらは
**5勢力を同時に動かして、最初から最後まで1回通す**ことだけを見る。

  配置 → 金庫 → 石油 → 時代進行 → 同時に2つの戦争 → 略奪
  → 終戦と占領 → 下剋上 → サイドバー

★ 偽のプレイヤーは armor stand。@a には入らないので、
  「@a を対象にする処理」(石油を奪う相手の選定など) はここでは動かない。
  そこは verify_jidai.py が別途見ている。

使い方:  python tests/toushi_5.py [--keep]
終了コード: 0=全部通った / 1=どこかで落ちた / 2=環境要因で実行不可
"""
import re
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_jidai as V

GO = [("kyuryo", "丘陵", 1), ("shinrin", "森林", 2), ("kawa", "川", 3),
      ("naikai", "内海", 4), ("iwaba", "岩場", 5)]

passed, failed = [], []

def check(label, cond, detail=""):
    (passed if cond else failed).append(label)
    print("  [" + ("PASS" if cond else "FAIL") + "] " + label
          + ("  " + detail if detail else ""))

def main():
    keep = "--keep" in sys.argv
    print("=== 5勢力の通し稼働 (1.20.1) ===")
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

    def score0(h, o):
        # ★ 一度も書かれていない保持者は None が返る。平時(0)と同じ扱いにする
        v = score(h, o)
        return 0 if v is None else v

    def n_ent(sel):
        # ★版差: 1.20.1 は "Test passed, count: 3" / 1.21 は "Test passed. Count: 3"
        m = re.search(r"[Cc]ount:\s*(\d+)", c("execute if entity " + sel))
        return int(m.group(1)) if m else 0

    # ★★ 戦争はマーカー1体で1件 (1.20.1 移行で変わった) ★★
    #   マクロが無いので保持者 "丘陵>森林" が作れない。マーカー自身の
    #   スコアに持たせている。読み書きは下の3つを通す。
    SEN = "@e[type=marker,tag=jidai_sensou]"

    def sensen(a, b, soku=0):
        c("scoreboard players set #w_kuni sagyou " + str(a))
        c("scoreboard players set #w_aite sagyou " + str(b))
        c("scoreboard players set #w_soku sagyou " + str(soku))
        return c("function jidai:sensou/sensen")

    def war(a, b, obj):
        c("scoreboard players set #yomu sagyou -999")
        c("execute as " + SEN[:-1] + ",scores={w_kuni=" + str(a) + ",w_aite=" + str(b)
          + "}] run scoreboard players operation #yomu sagyou = @s " + obj)
        v = score("#yomu", "sagyou")
        return None if v == -999 else v

    def war_set(a, b, obj, v):
        c("execute as " + SEN[:-1] + ",scores={w_kuni=" + str(a) + ",w_aite=" + str(b)
          + "}] run scoreboard players set @s " + obj + " " + str(v))

    def susumu():
        return c("execute as " + SEN + " run function jidai:sensou/susumu")

    try:
        r.connect()
        c("function jidai:load")
        c("function jidai:setup/kyoten")

        # ---------- 1) 5勢力に3人ずつ置く ----------
        print("\n[1] 5勢力に3人ずつ配置する")
        for gi, (team, mei, ban) in enumerate(GO):
            for k in range(3):
                tag = "p_" + team + "_" + str(k)
                # ★★ 置く場所は x,z とも 0〜15 の中に収める ★★
                #   人が誰も居ないサーバーでは、読み込まれている区画が
                #   ごく狭い。summon は成功を返すのに、@e で数えると
                #   居ないことになる(読み込まれていない区画の entity は
                #   セレクタに一致しないため)。
                #   実測: x=-4/-2 は消え、x=0/2/4 は残った。z=30台は全滅。
                #   ここは通し稼働で見たい所ではないので、確実な場所を使う。
                # ★ NoGravity:1b … Marker の armor stand は当たり判定が無く、
                #   地面をすり抜けて落ちてしまうため。
                cmd = ('summon minecraft:armor_stand ' + str(1 + gi * 2) + ' 100 '
                       + str(1 + k * 2) + ' {Tags:["' + tag + '","toushi"],'
                       + 'Invisible:1b,Marker:1b,NoGravity:1b}')
                res = c(cmd)
                if "Summoned" not in res:
                    print("      summon の返事: " + cmd + "  ->  " + res[:90])
                c("team join " + team + " @e[tag=" + tag + "]")
                c("execute as @e[tag=" + tag + "] run scoreboard players set @s bangou "
                  + str(ban))
        uchiwake = [(t, n_ent("@e[tag=toushi,team=" + t + "]"),
                     n_ent("@e[tag=p_" + t + "_0]")) for t, _, _ in GO]
        print("      内訳 (チーム所属数, 0番の有無): " + str(uchiwake))
        check("15人ぶんの居場所ができた", n_ent("@e[tag=toushi]") == 15,
              "実測=" + str(n_ent("@e[tag=toushi]")) + " " + str(uchiwake))
        check("5勢力すべてにチームがある",
              all(n_ent("@e[tag=toushi,team=" + t + "]") == 3 for t, _, _ in GO))

        # ---------- 2) 5勢力の金庫 ----------
        print("\n[2] 5勢力すべての金庫を立てる")
        for i, (team, mei, ban) in enumerate(GO):
            c("scoreboard players set " + mei + " chokin " + str(200 + i * 50))
        got = [score(m, "chokin") for _, m, _ in GO]
        check("5勢力の金庫がそれぞれ立っている", got == [200, 250, 300, 350, 400], str(got))

        c("execute as @e[tag=p_iwaba_0] run scoreboard players set @s kane_kojin 100")
        c("execute as @e[tag=p_iwaba_0] run function jidai:kane/kinko_yomu")
        yomi = score("#kinko", "sagyou")
        c("scoreboard players set #kinko sagyou 430")
        c("execute as @e[tag=p_iwaba_0] run function jidai:kane/kinko_kaku")
        check("★岩場(5勢力目)の人が金庫を読み書きできる",
              yomi == 400 and score("岩場", "chokin") == 430,
              "読み=" + str(yomi) + " 書き後=" + str(score("岩場", "chokin")))

        # ---------- 3) 石油 ----------
        print("\n[3] 石油の合計が5勢力ぶん作られる")
        for gi, (team, mei, ban) in enumerate(GO):
            for k in range(3):
                c("execute as @e[tag=p_" + team + "_" + str(k)
                  + "] run scoreboard players set @s sekiyu " + str(gi + k))
        c("function jidai:clock")
        got = [score(m, "sekiyu_gokei") for _, m, _ in GO]
        check("★石油の合計欄が5勢力ぶんある", all(g is not None for g in got), str(got))

        # ---------- 4) 時代進行 ----------
        print("\n[4] 時代進行を5勢力ぶん回す")
        for _ in range(6):
            c("function jidai:shinko/junban")
        mawatta = [score(m, "jouken_a") is not None for _, m, _ in GO]
        check("★6回まわすと5勢力すべてに判定が届く", all(mawatta), str(mawatta))
        for _, mei, _ in GO:
            c("scoreboard players set " + mei + " jidai 2")
        c("function jidai:shinko/chuo")
        check("★5勢力そろって2になれば中央も2へ上がる", score("世界", "chuo") == 2,
              "実測=" + str(score("世界", "chuo")))

        # ---------- 5) 同時に2つの戦争 ----------
        print("\n[5] 2つの戦争を同時に走らせる (丘陵vs森林 / 川vs内海)")
        sensen(1, 2)
        sensen(3, 4)
        check("★戦争マーカーが2体ある", n_ent(SEN) == 2, "実測=" + str(n_ent(SEN)))
        check("両方とも準備(1)から始まる",
              war(1, 2, "sensou") == 1 and war(3, 4, "sensou") == 1,
              "丘陵→森林=" + str(war(1, 2, "sensou"))
              + " 川→内海=" + str(war(3, 4, "sensou")))
        check("★巻き込まれていない岩場の印(w_5)は付いていない",
              n_ent(SEN[:-1] + ",tag=w_5]") == 0,
              "実測=" + str(n_ent(SEN[:-1] + ",tag=w_5]")))
        c("function jidai:sensou/youyaku")
        check("★要約でも岩場は平時のまま", score("岩場", "sensou") == 0,
              "実測=" + str(score("岩場", "sensou")))

        war_set(1, 2, "sensou_byou", 1)
        war_set(3, 4, "sensou_byou", 1)
        susumu()
        check("★★1回の時計で2つの戦争がどちらも交戦へ進む",
              war(1, 2, "sensou") == 2 and war(3, 4, "sensou") == 2,
              "丘陵→森林=" + str(war(1, 2, "sensou"))
              + " 川→内海=" + str(war(3, 4, "sensou")))

        # ---------- 6) 略奪 ----------
        print("\n[6] 略奪する")
        # ★ 2026-09-09: 1回で相手の総量の 1%。100 だと 1 しか動かず目盛りが粗いので 500 で見る
        c("scoreboard players set 森林 chokin 500")
        c("scoreboard players set 丘陵 chokin 0")
        c("execute as @e[tag=p_kyuryo_0] run scoreboard players set @s ryakudatsu_kan 0")
        # ★1.20.1 では関数に引数を渡せない。番号をスコアに置いてから呼ぶ。
        #   (本番では jidai:kane/azukeru が同じ形で置いてから呼んでいる)
        c("scoreboard players set #jibun_no sagyou 1")
        c("scoreboard players set #aite_no sagyou 2")
        c("execute as @e[tag=p_kyuryo_0] run function jidai:sensou/ryakudatsu")
        check("★略奪で相手の総量の1%が動く (森林500→495 / 丘陵0→5)",
              score("森林", "chokin") == 495 and score("丘陵", "chokin") == 5,
              "森林=" + str(score("森林", "chokin")) + " 丘陵=" + str(score("丘陵", "chokin")))
        check("奪った累計が向きごとに記録される (a側だけ増えて b側は0)",
              war(1, 2, "ryakudatsu_kane_a") == 5
              and war(1, 2, "ryakudatsu_kane_b") == 0,
              "a=" + str(war(1, 2, "ryakudatsu_kane_a"))
              + " b=" + str(war(1, 2, "ryakudatsu_kane_b")))
        check("クールダウンが置かれる",
              score("@e[tag=p_kyuryo_0,limit=1]", "ryakudatsu_kan") == 10,
              "実測=" + str(score("@e[tag=p_kyuryo_0,limit=1]", "ryakudatsu_kan")))
        check("★通知の名前用の印は、使い終わったら残らない",
              n_ent("@e[tag=jidai_ubawareta]") == 0,
              "実測=" + str(n_ent("@e[tag=jidai_ubawareta]")))

        # ---------- 7) 終戦と占領 ----------
        print("\n[7] 終戦。奪った側だけが占領できる")
        c("scoreboard players set 森林 chokin 0")
        c("scoreboard players set 内海 chokin 0")
        war_set(1, 2, "sensou_byou", 1)
        war_set(3, 4, "sensou_byou", 1)
        susumu()
        # ★★ 2026-09-09: 貯金0での占領は廃止。占領の入口はビーコン1本 ★★
        check("★貯金0で終わっても占領は付かない（2026-09-09 で廃止）",
              score("森林", "senryou") == 0, "実測=" + str(score("森林", "senryou")))
        check("★一度も略奪していない川も、内海を占領できない",
              score("内海", "senryou") == 0, "実測=" + str(score("内海", "senryou")))
        # ★ ビーコンを壊した時の道。プラグインは #s_kuni / #s_aite を置いて
        #   jidai:sensou/shokuminchi を呼ぶ。ここでは同じ形で呼ぶ。
        c("scoreboard players set #s_kuni sagyou 1")
        c("scoreboard players set #s_aite sagyou 2")
        c("function jidai:sensou/shokuminchi")
        check("★★ビーコンを壊すと 丘陵 が 森林 を占領する",
              score("森林", "senryou") == 1, "実測=" + str(score("森林", "senryou")))
        check("★★植民地の印も同時に付く（勝利条件が数える方）",
              score("森林", "shokuminchi") == 1,
              "実測=" + str(score("森林", "shokuminchi")))
        check("2つとも再戦禁止(3)になる",
              war(1, 2, "sensou") == 3 and war(3, 4, "sensou") == 3,
              "丘陵→森林=" + str(war(1, 2, "sensou"))
              + " 川→内海=" + str(war(3, 4, "sensou")))

        # ---------- 8) 下剋上 ----------
        print("\n[8] 下剋上で占領を跳ね返す")
        c("scoreboard players set 森林 gekokujo 1")
        c("tag @e[tag=p_shinrin_0] add jidai_leader")
        c("execute as @e[tag=p_shinrin_0] run function jidai:sensou/gekokujo_tsukau")
        check("★★下剋上で占領が解ける", score("森林", "senryou") == 0,
              "実測=" + str(score("森林", "senryou")))
        check("権利は消費される", score("森林", "gekokujo") == 0,
              "実測=" + str(score("森林", "gekokujo")))
        check("★★再戦禁止を無視して、ただちに交戦(2)から始まる",
              war(2, 1, "sensou") == 2, "実測=" + str(war(2, 1, "sensou")))
        check("略奪の上限が2倍になる", war(2, 1, "sensou_bai") == 2,
              "実測=" + str(war(2, 1, "sensou_bai")))
        check("★古い戦争(丘陵→森林)は消えて、川→内海と合わせて2件になる",
              n_ent(SEN) == 2 and war(1, 2, "sensou") is None,
              "件数=" + str(n_ent(SEN)) + " 古い方=" + str(war(1, 2, "sensou")))

        # ---------- 9) サイドバー ----------
        print("\n[9] サイドバーが5勢力ぶん出る")
        c("function jidai:hyouji/kousin")
        # ★サイドバーは勢力ごとに別の目的。自分の勢力の3行しか見えない。
        hyo = [score("貯金", "hyouji_" + str(b)) for _, _, b in GO]
        sek = [score("石油", "hyouji_" + str(b)) for _, _, b in GO]
        check("★5勢力すべての枠に貯金が入る", all(h is not None for h in hyo), str(hyo))
        check("★5勢力すべての枠に石油が入る", all(h is not None for h in sek), str(sek))
        check("★1人が見る行は常に3行 (貯金・石油・中央の時代)",
              len([1 for h in ("貯金", "石油", "中央の時代")
                   if score(h, "hyouji_1") is not None]) == 3)
        c("scoreboard players set 岩場 senryou 3")
        out = c("function jidai:hyouji/kousin")
        check("占領された勢力があっても行を作り直せる",
              "Unknown" not in out and "Invalid" not in out, out[:60])

        # ---------- 10) ログ ----------
        print("\n[10] ログにエラーが出ていない")
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
