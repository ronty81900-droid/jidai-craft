# -*- coding: utf-8 -*-
"""
拠点の設置が「黙って失敗しないか」を実測する。

★★ なぜこれが要るのか ★★
  読み込まれていない区画には setblock も summon も届かない。
  しかも【エラーが出ない】。打った人は成功したと思い込む。
  実際に、起動直後に打つと 32個のうち 26個しか置けなかった(実測)。
  ★ 2026-08-20 に銃器専門店を各拠点へ足したので、正しい数は 37 になった。

  そこで jidai:setup/kyoten は最後に自分で数を数え、
  足りなければ理由と対処を出すようにしてある。
  このテストは【わざと失敗させて】その知らせが出ることを確かめる。

★ tellraw @s はコンソール(RCON含む)には届かない。@s が誰にも当たらないため。
  だから置けた数は #shisetsu sagyou にも残してある。ここではそれを読む。

使い方:  python tests/setup_kakunin.py
終了コード: 0=全部通った / 1=どこかで落ちた / 2=環境要因で実行不可
"""
import re, shutil, subprocess, sys, time
from pathlib import Path
R = Path(__file__).resolve().parent.parent
# ★ 隣の置き場「コマンド」から借りる。ユーザー名は書かない。
#   R = …/KUN/時代クラフト なので、その親が KUN。
RT = R.parent / "コマンド" / "tests"
sys.path.insert(0, str(RT))
from rcon_client import RconClient
D = R / "tests" / "setupserver"
JAR = str(RT / "server-1.20.1.jar")
PORT, RPORT, PW = 25641, 25642, "setupchk"

if D.exists():
    shutil.rmtree(D, ignore_errors=True)
D.mkdir(parents=True)
(D / "eula.txt").write_text("eula=true\n", encoding="utf-8")
(D / "server.properties").write_text("\n".join([
    "server-port=%d" % PORT, "online-mode=false", "enable-rcon=true",
    "rcon.port=%d" % RPORT, "rcon.password=" + PW,
    "level-name=world", "view-distance=6", "max-players=2",
]) + "\n", encoding="utf-8")
# ★実物の地図を使う（拠点が遠いので、区画の読み込み待ちが本当に起きる）
shutil.copytree(R / "map" / "1500" / "export_1201" / "heightmap_1500_8bit", D / "world")
(D / "world" / "session.lock").unlink(missing_ok=True)
shutil.copytree(R / "datapacks" / "jidai_craft", D / "world" / "datapacks" / "jidai_craft")

log = D / "out.log"
with open(log, "wb") as f:
    proc = subprocess.Popen(["java", "-Xmx2G", "-jar", JAR, "--nogui"],
                            cwd=str(D), stdout=f, stderr=subprocess.STDOUT,
                            stdin=subprocess.DEVNULL)
for _ in range(300):
    time.sleep(1)
    if log.exists() and "Done (" in log.read_text(encoding="utf-8", errors="replace"):
        break
    if proc.poll() is not None:
        print("起動失敗"); sys.exit(2)
r = RconClient(port=RPORT, password=PW, timeout=180.0)
ok, ng = [], []
def check(na, j, sh=""):
    (ok if j else ng).append(na)
    print("  [%s] %s%s" % ("PASS" if j else "FAIL", na, "  " + sh if sh else ""))
try:
    r.connect()
    c = lambda x: r.command(x).strip()
    def kazu():
        m = re.search(r"[Cc]ount:\s*(\d+)",
                      c("execute if entity @e[type=marker,tag=jidai_shisetsu]"))
        return int(m.group(1)) if m else 0

    print("=== (1) わざと失敗させる: 起動直後、区画が読まれる前に打つ ===")
    out = c("function jidai:setup/kyoten")
    n1 = kazu()
    print("    施設マーカー: %d 個" % n1)
    print("    返事の中身:")
    for l in out.splitlines()[:8]:
        print("      " + l)
    # ★ tellraw @s はコンソール(RCON)には届かない(@s が誰にも当たらない)。
    #   そこで【スコアに残した数】で確かめる。コンソールからも読める正本。
    import re as _re
    m = _re.search(r"has (-?\d+)", c("scoreboard players get #shisetsu sagyou"))
    kiroku = int(m.group(1)) if m else None
    check("★★置けた数がスコアに残る (コンソールからも読める)",
          kiroku == n1, "スコア=%s 実際=%d" % (kiroku, n1))
    if n1 < 37:
        check("★★失敗の道が本当に通った (区画が読まれる前は足りない)",
              True, "1回目=%d 個" % n1)
    else:
        check("(この環境では1回目から37個置けた。失敗の道は通っていない)", True,
              "実測=%d 個" % n1)
    src = (R / "datapacks/jidai_craft/data/jidai/functions/setup/kyoten.mcfunction"
           ).read_text(encoding="utf-8")
    check("(記述) 足りない時に理由と対処を出す行がある",
          "個だけです" in src and "もう一度" in src)
    check("(記述) 先に forceload を呼んでいる",
          "function jidai:setup/forceload" in src)

    print()
    print("=== (2) 待ってから、もう一度打つ ===")
    time.sleep(8)
    out2 = c("function jidai:setup/kyoten")
    n2 = kazu()
    print("    施設マーカー: %d 個" % n2)
    for l in out2.splitlines()[:4]:
        print("      " + l)
    check("2回目で 37個 そろう", n2 == 37, "実測=%d 個" % n2)
    m = _re.search(r"has (-?\d+)", c("scoreboard players get #shisetsu sagyou"))
    check("成功した時もスコアが 37 になる", (int(m.group(1)) if m else None) == 37,
          "スコア=%s" % (m.group(1) if m else None))
    uchi = []
    for t in ("kyuryo", "shinrin", "kawa", "naikai", "iwaba"):
        m = re.search(r"[Cc]ount:\s*(\d+)",
                      c("execute if entity @e[type=marker,tag=jidai_kyoten_%s]" % t))
        uchi.append((t, int(m.group(1)) if m else 0))
    check("5拠点とも7個ずつ印が付く", all(n == 7 for _, n in uchi), str(uchi))
finally:
    try:
        r.command("stop"); proc.wait(timeout=120)
    except Exception:
        proc.kill()
    finally:
        try: r.close()
        except Exception: pass
print()
print("結果: PASS %d / FAIL %d" % (len(ok), len(ng)))
sys.exit(1 if ng else 0)
