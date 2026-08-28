# -*- coding: utf-8 -*-
"""
書き出した地図を、本物の 1.20.1 サーバーに読ませて確かめる。

  ・1.20.1 が地図を読めるか（マップ形式が合っているか）
  ・拠点5つの地表が Y=100、中央が Y=96 か
  ・その地図の上でデータパックが拠点を組み立てられるか

使い方:
    python tests/map_kakunin.py                 ← 1500 を見る
    python tests/map_kakunin.py 800             ← 800 を見る
    python tests/map_kakunin.py --dir "C:/…"    ← 書き出し先を直に指す

終了コード: 0=全部通った / 1=どこかで落ちた / 2=環境要因で実行不可
"""
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# ★ 隣の置き場「コマンド」から借りる。ユーザー名は書かない。
RT = ROOT.parent / "コマンド" / "tests"
sys.path.insert(0, str(RT))
from rcon_client import RconClient  # noqa: E402

SERVER_JAR = RT / "server-1.20.1.jar"
SERVER_DIR = ROOT / "tests" / "mapserver"
PORT, RPORT, PW = 25621, 25622, "mapchk"

# 拠点の中心。すべて地表 Y=100 の平坦になっているはず
KYOTEN = [("拠点1 丘陵", 0, -410), ("拠点2 森林", 390, -127), ("拠点3 川", 241, 332),
          ("拠点4 内海", -241, 332), ("拠点5 岩場", -390, -127)]
# 銀行は拠点の中心から少しずれた所に建つ（jidai:setup/kyoten が置く）
GINKO = [("拠点1 丘陵", 0, 101, -404), ("拠点2 森林", 390, 101, -121)]

ok, ng = [], []


def check(namae, jouken, shousai=""):
    (ok if jouken else ng).append(namae)
    print("  [" + ("PASS" if jouken else "FAIL") + "] " + namae
          + ("  " + shousai if shousai else ""))


def main():
    size = 1500
    export_dir = None
    args = sys.argv[1:]
    if "--dir" in args:
        export_dir = Path(args[args.index("--dir") + 1])
    elif args and args[0].isdigit():
        size = int(args[0])
    if export_dir is None:
        oya = ROOT / "map" / str(size) / "export_1201"
        naka = [p for p in oya.iterdir() if p.is_dir()] if oya.exists() else []
        if not naka:
            print("[環境エラー] 書き出したワールドが無い: " + str(oya))
            print("  先に:  wpscript map/export.js " + str(size) + " \"" + str(oya) + "\"")
            return 2
        export_dir = naka[0]

    if not SERVER_JAR.exists():
        print("[環境エラー] サーバー jar が無い: " + str(SERVER_JAR))
        return 2

    print("=== 書き出した地図を 1.20.1 で読ませる (" + str(size) + ") ===")
    print("地図: " + str(export_dir))

    # ★ 前回のワールドを消してから入れ直す。
    #   残っていると、前回 setup/kyoten が積んだ地形をそのまま測ってしまい、
    #   「中央の地表が Y=101」のような有り得ない答えになる（実際にやった）。
    for _ in range(10):
        if not SERVER_DIR.exists():
            break
        shutil.rmtree(SERVER_DIR, ignore_errors=True)
        if SERVER_DIR.exists():
            time.sleep(2)
    if SERVER_DIR.exists():
        print("[環境エラー] 前回の検証サーバーが消せない: " + str(SERVER_DIR))
        print("  まだ java.exe が動いている可能性がある。終了させてからやり直すこと。")
        return 2
    SERVER_DIR.mkdir(parents=True)
    (SERVER_DIR / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (SERVER_DIR / "server.properties").write_text("\n".join([
        "server-port=" + str(PORT), "online-mode=false", "enable-rcon=true",
        "rcon.port=" + str(RPORT), "rcon.password=" + PW,
        "level-name=world", "view-distance=6", "max-players=2",
        "difficulty=peaceful",
    ]) + "\n", encoding="utf-8")
    shutil.copytree(export_dir, SERVER_DIR / "world")
    lock = SERVER_DIR / "world" / "session.lock"
    if lock.exists():
        lock.unlink()
    shutil.copytree(ROOT / "datapacks" / "jidai_craft",
                    SERVER_DIR / "world" / "datapacks" / "jidai_craft")

    log = SERVER_DIR / "out.log"
    with open(log, "wb") as f:
        proc = subprocess.Popen(["java", "-Xmx2G", "-jar", str(SERVER_JAR), "--nogui"],
                                cwd=str(SERVER_DIR), stdout=f, stderr=subprocess.STDOUT,
                                stdin=subprocess.DEVNULL)
    print("サーバーを起動中 (最大300秒)...")
    kidou = False
    for _ in range(300):
        time.sleep(1)
        if log.exists() and "Done (" in log.read_text(encoding="utf-8", errors="replace"):
            kidou = True
            break
        if proc.poll() is not None:
            break
    if not kidou:
        print("[環境エラー] 起動できなかった。ログ末尾:")
        print("\n".join(log.read_text(encoding="utf-8", errors="replace").splitlines()[-25:]))
        proc.kill()
        return 2
    print("起動完了\n")

    r = RconClient(port=RPORT, password=PW, timeout=180.0)
    try:
        r.connect()

        def c(x):
            return r.command(x).strip()

        print("[1] 地図が読めているか")
        check("1.20.1 のサーバーが起動した（＝マップ形式が合っている）", True)
        txt = log.read_text(encoding="utf-8", errors="replace")
        warui = [l for l in txt.splitlines() if "ERROR" in l]
        check("読み込み時のエラーが無い", not warui, str(warui[:2]))

        print("\n[2] 拠点と中央の高さ")
        # ★★ forceload したあと、区画が本当に載るまで待つこと ★★
        #   載っていない区画では `execute if block` が【どの条件でも失敗】する。
        #   「空気でもない、中身でもない」という有り得ない答えになるので、
        #   地図が壊れているように見えてしまう（実際に1回それで騙された）。
        for _, x, z in KYOTEN:
            c("forceload add %d %d %d %d" % (x - 16, z - 16, x + 16, z + 16))
        c("forceload add -16 -16 16 16")
        time.sleep(8)

        # ★「地表が Y=100」＝ Y=100 が中身のあるブロックで、Y=101 が空気。
        #   ここを取り違えると、正しい地図なのに全部 FAIL になる（実際にやった）。
        for na, x, z in KYOTEN:
            ue = c("execute if block %d 101 %d minecraft:air" % (x, z))
            ji = c("execute if block %d 100 %d minecraft:air" % (x, z))
            check(na + " (%d,%d) の地表が Y=100" % (x, z),
                  "Test passed" in ue and "Test failed" in ji,
                  "Y101=" + ue[:18] + " / Y100=" + ji[:18])
        ue = c("execute if block 0 97 0 minecraft:air")
        ji = c("execute if block 0 96 0 minecraft:air")
        check("中央 (0,0) の地表が Y=96",
              "Test passed" in ue and "Test failed" in ji,
              "Y97=" + ue[:18] + " / Y96=" + ji[:18])

        print("\n[3] その地図の上でデータパックが動くか")
        check("jidai_craft が有効", "jidai_craft" in c("datapack list enabled"))
        c("function jidai:load")
        c("function jidai:setup/forceload")
        time.sleep(3)
        c("function jidai:setup/kyoten")
        time.sleep(2)

        def kazoeru(sel):
            # ★版差: 1.20.1 は "Test passed, count: 3" / 1.21 は "Test passed. Count: 3"
            m = re.search(r"[Cc]ount:\s*(\d+)", c("execute if entity " + sel))
            return int(m.group(1)) if m else 0

        uchi = [(t, kazoeru("@e[type=marker,tag=jidai_kyoten_%s]" % t))
                for t in ("kyuryo", "shinrin", "kawa", "naikai", "iwaba")]
        # ★ 銃器専門店を足したので 6個x5 -> 7個x5。
        check("拠点の印が5拠点ぶんで合計35個", sum(n for _, n in uchi) == 35, str(uchi))
        for na, x, y, z in GINKO:
            res = c("execute if block %d %d %d minecraft:gold_block" % (x, y, z))
            check(na + " に銀行(金ブロック)が置かれた", "Test passed" in res, res[:30])

        # ★★ 販売所と銃器専門店は、どちらもエメラルドブロック ★★
        #   離れた場所に2つ建つことを、実際のブロックで確かめる。
        #   銀行(中心)からの相対位置は 販売所 x+4 / 銃器専門店 x+10、どちらも z+6。
        #   ここは区画を常時読み込みにしてあるので execute if block が効く
        #   (読み込んでいない世界では、どの条件でも Test failed になる)。
        for na, x, y, z in GINKO:
            m4 = c("execute if block %d %d %d minecraft:emerald_block" % (x + 4, y, z))
            m10 = c("execute if block %d %d %d minecraft:emerald_block" % (x + 10, y, z))
            check(na + " に販売所(x+4)がある", "Test passed" in m4, m4[:30])
            check(na + " に銃器専門店(x+10)がある", "Test passed" in m10, m10[:30])
        juki = kazoeru("@e[type=marker,tag=jidai_juki]")
        check("★銃器専門店のマーカーが5つ", juki == 5, "実測=%d" % juki)

        print("\n[4] ログにエラーが出ていない")
        txt = log.read_text(encoding="utf-8", errors="replace")
        e = [l for l in txt.splitlines()
             if "Failed to load" in l or "Whilst executing" in l or "Couldn't load" in l]
        check("実行時エラーが無い", not e, str(e[:2]))
    finally:
        try:
            r.command("stop")
            proc.wait(timeout=120)
        except Exception:
            proc.kill()
        finally:
            try:
                r.close()
            except Exception:
                pass

    print()
    print("=" * 50)
    print("結果: PASS %d / FAIL %d" % (len(ok), len(ng)))
    for n in ng:
        print("  FAILED: " + n)
    return 1 if ng else 0


if __name__ == "__main__":
    sys.exit(main())
