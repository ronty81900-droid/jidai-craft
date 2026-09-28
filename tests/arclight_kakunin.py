# -*- coding: utf-8 -*-
"""
本番（jikki_mod・Arclight）の【写し】を立てて、配ったプラグインとデータパックが
Arclight でも読み込まれ、動くかを確かめる。人は入れない（RCON だけ）。

★ なぜ要るか
  ボットの検査（tests/bot_kakunin.js）は Paper の写しで走る。ボットは MOD 入りの
  Arclight に入れないため。だから「新しい jar が Arclight で起動するか」は別に見る。

★ 本物の jikki_mod は【読むだけ】。写し（tests/arclightserver）を作って、終わったら消す。
  写しは server-ip=127.0.0.1 にして、このパソコンの外からは入れない。
  RCON のパスワードは毎回その場で作り、写しごと消える。
★ 起動は WMI 経由（Claude の下から java を立てると NIO が死ぬ）。

見る物:
  1. プラグインがいまの版（plugin.yml）で有効になる・データパックが読まれる・自動走査が走る
  2. 戻し待ちの印（jidai_ginko_modori）が付いた銀行は、データパックの clock が直さない
  3. 印が残ったまま止めて起動し直すと、起動の5秒後に印が外れて銀行が戻る（落ちた時の保険）
  4. ログに JidaiCraft の例外が出ていない

使い方:  python tests/arclight_kakunin.py [--nokosu]
終了コード: 0=全部通った / 1=どこかで落ちた / 2=環境要因で実行不可
"""
import os
import re
import secrets
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path

NE = Path(__file__).resolve().parent.parent
MOTO = NE / "jikki_mod"                    # 本物（読むだけ）
SABA = NE / "tests" / "arclightserver"     # 写し（作って消す）
PORT, RPORT = 25661, 25662                 # 本物（25565）・他の検査と重ならない番号
PW = secrets.token_hex(12)
HOST = "127.0.0.1"
# 銀行の印は拠点1（丘陵）のもので見る。印はブロックの底の真ん中に立っている
#   （setup/kyoten の positioned が整数の x と z を真ん中へずらすため）
GINKO = (0, 101, -404)
SHIRUSHI = "@e[type=marker,tag=jidai_ginko,x=0.5,y=101.5,z=-403.5,distance=..1]"

passed, failed = [], []


def check(label, cond, detail=""):
    (passed if cond else failed).append(label)
    print("  [%s] %s%s" % ("PASS" if cond else "FAIL", label, ("  " + detail) if detail else ""))


def tsunagaru(port):
    try:
        with socket.create_connection((HOST, port), timeout=1.5):
            return True
    except OSError:
        return False


# ── RCON（1回の受信で包みを1つしか読まないので、返事が来てから次を送る）──
class Rcon:
    def __init__(self):
        self.s = socket.create_connection((HOST, RPORT), timeout=30)
        self.id = 1
        self._okuru(3, PW)
        if self._uke()[0] == -1:
            raise RuntimeError("RCON の認証に失敗")

    def _okuru(self, t, body):
        self.id += 1
        b = body.encode("utf-8")
        self.s.sendall((10 + len(b)).to_bytes(4, "little", signed=True)
                       + self.id.to_bytes(4, "little", signed=True)
                       + t.to_bytes(4, "little", signed=True) + b + b"\x00\x00")

    def _uke(self):
        def n(k):
            d = b""
            while len(d) < k:
                c = self.s.recv(k - len(d))
                if not c:
                    raise RuntimeError("RCON が切れた")
                d += c
            return d
        ln = int.from_bytes(n(4), "little", signed=True)
        d = n(ln)
        return int.from_bytes(d[:4], "little", signed=True), d[8:-2].decode("utf-8", "replace")

    def c(self, cmd):
        self._okuru(2, cmd)
        return self._uke()[1]

    def close(self):
        try:
            self.s.close()
        except OSError:
            pass


def utsusu():
    if tsunagaru(25565):
        raise RuntimeError("ポート 25565 でサーバーが動いている。止めてからやり直すこと（動いている世界は写せない）")
    if tsunagaru(PORT):
        raise RuntimeError("ポート %d を別のサーバーが使っている（前回の写しが残っているかもしれない）" % PORT)
    if SABA.exists():
        shutil.rmtree(SABA)                  # 前回の写し（このファイルが作った物）
    shutil.copytree(MOTO, SABA, ignore=lambda d, fs: ["logs"] if Path(d) == MOTO else [])
    kae = {"server-ip": HOST, "server-port": str(PORT), "query.port": str(PORT),
           "enable-rcon": "true", "rcon.port": str(RPORT), "rcon.password": PW,
           "motd": "arclight kensa (copy of jikki_mod)"}
    p = SABA / "server.properties"
    gyou, mita = [], set()
    for l in p.read_text(encoding="utf-8").splitlines():
        k = l.split("=", 1)[0]
        if k in kae:
            gyou.append(k + "=" + kae[k])
            mita.add(k)
        else:
            gyou.append(l)
    gyou += [k + "=" + v for k, v in kae.items() if k not in mita]
    p.write_text("\n".join(gyou) + "\n", encoding="utf-8")
    # java は控えの start.bat と同じ物（本物の start.bat は探し方の手順なので、写しでは使わない）
    s = (NE / "jikki_1201" / "start.bat").read_bytes().decode("latin1")
    m = re.search(r'"([^"]*java\.exe)"', s, re.I)
    java = m.group(1) if m else "java"
    jar = next(f.name for f in MOTO.iterdir() if f.name.startswith("arclight-") and f.suffix == ".jar")
    (SABA / "kidou.bat").write_bytes(
        ('@echo off\r\ncd /d "%%~dp0"\r\n"%s" -Xms2G -Xmx4G -jar %s --nogui\r\n' % (java, jar)).encode("ascii"))
    print("写しを作った: tests/arclightserver（%s）" % jar)


def tateru():
    out = SABA / "server_out.txt"
    out.write_text("", encoding="ascii")
    cl = 'cmd /c "cd /d "%s" && "%s" > "%s" 2>&1"' % (SABA, SABA / "kidou.bat", out)
    ps = ("$r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = '"
          + cl.replace("'", "''") + "' }; Write-Output $r.ReturnValue")
    rv = subprocess.run(["powershell", "-NoProfile", "-NonInteractive", "-Command", ps],
                        capture_output=True).stdout.decode("utf-8", "replace").strip()
    if rv != "0":
        raise RuntimeError("WMI で起動できない（ReturnValue=%s）" % rv)
    for i in range(150):
        time.sleep(2)
        t = out.read_bytes().decode("latin1")
        if "Done (" in t:
            print("サーバーが立った（%d秒）" % ((i + 1) * 2))
            return
        if re.search(r"FAILED TO BIND|Exception in thread \"main\"|Mod loading has failed", t):
            raise RuntimeError("起動に失敗した（%s を見ること）" % out)
    raise RuntimeError("5分 待っても立たない")


def rcon_tsunagu():
    for _ in range(20):
        try:
            return Rcon()
        except OSError:
            time.sleep(2)
    raise RuntimeError("RCON に繋がらない")


def tomeru(r):
    try:
        r.c("stop")
    except (OSError, RuntimeError):
        pass
    r.close()
    for _ in range(90):
        if not tsunagaru(PORT):
            break
        time.sleep(1)
    time.sleep(4)


def log_yomu():
    b = (SABA / "logs" / "latest.log").read_bytes()
    try:
        return b.decode("utf-8")
    except UnicodeDecodeError:
        return b.decode("cp932", "replace")


def block_aru(r, b):
    return "passed" in r.c("execute if block %d %d %d minecraft:%s" % (GINKO + (b,)))


def main():
    nokosu = "--nokosu" in sys.argv
    sys.stdout.reconfigure(encoding="utf-8")
    ban = re.search(r"^version:\s*(\S+)", (NE / "plugin" / "src" / "main" / "resources" / "plugin.yml")
                    .read_text(encoding="utf-8"), re.M).group(1)
    print("=== 本番（Arclight）の写しで、プラグイン v%s とデータパックを確かめる ===" % ban)
    tatta = False
    r = None
    try:
        utsusu()
        tateru()
        tatta = True
        r = rcon_tsunagu()
        time.sleep(8)                      # 起動の5秒後に走る自動走査（jidoScan）を待つ

        print("\n[1] 読み込み")
        log = log_yomu()
        check("プラグインが v%s で有効になった" % ban, ("Enabling JidaiCraft v" + ban) in log)
        check("データパックを確かめた（プラグインの起動時の点検）", "データパック jidai_craft を確認しました" in log)
        check("起動時の自動走査が走った", "自動走査:" in log)
        shippai0 = [l for l in log.splitlines()
                    if re.search(r"No entity was found|Unknown or incomplete command|Incorrect argument", l)]
        check("印が無い時、起動時の印外しは何も言わない", not shippai0, " | ".join(shippai0[:3])[:300])
        dp = r.c("datapack list enabled")
        check("データパック jidai_craft が有効", "jidai_craft" in dp, dp.strip()[:120])
        n = re.search(r"[Cc]ount:\s*(\d+)", r.c("execute if entity " + SHIRUSHI))
        check("拠点1の銀行の印（マーカー）がある", n is not None and int(n.group(1)) == 1,
              r.c("data get entity %s Pos" % SHIRUSHI.replace("]", ",limit=1]")).strip()[:100])

        print("\n[2] 戻し待ちの銀行は、データパックが直さない")
        check("(前) 銀行は金ブロック", block_aru(r, "gold_block"))
        r.c("tag %s add jidai_ginko_modori" % SHIRUSHI)
        r.c("setblock %d %d %d minecraft:air replace" % GINKO)
        time.sleep(2.5)
        check("★★ 印が付いている間、clock は銀行を直さない（2.5秒 待っても空気）", block_aru(r, "air"))

        print("\n[3] 印が残ったまま止めて、起動し直す（落ちた時の保険）")
        tomeru(r)
        r = None
        tateru()
        r = rcon_tsunagu()
        naotta = False
        for _ in range(30):
            time.sleep(1)
            if block_aru(r, "gold_block"):
                naotta = True
                break
        nokoru = "passed" in r.c("execute if entity @e[type=marker,tag=jidai_ginko_modori]")
        check("★★ 起動し直すと、残った印が外れて銀行が戻る", naotta and not nokoru,
              "戻った=%s / 印が残る=%s" % (naotta, nokoru))

        print("\n[4] ログ")
        log = log_yomu()
        reigai = [l for l in log.splitlines()
                  if ("Exception" in l or "ERROR" in l) and "JidaiCraft" in l]
        check("JidaiCraft の例外・エラーがログに無い", not reigai, " | ".join(reigai[:3])[:300])
        shippai = [l for l in log.splitlines()
                   if re.search(r"No entity was found|Unknown or incomplete command|Incorrect argument", l)]
        check("起動時の印外しの命令が、失敗を出していない", not shippai, " | ".join(shippai[:3])[:300])
    except RuntimeError as e:
        print("[環境エラー] " + str(e))
        return 2
    finally:
        if tatta and not nokosu:
            if r is None:
                try:
                    r = rcon_tsunagu()
                except RuntimeError:
                    r = None
            if r is not None:
                tomeru(r)
            for _ in range(10):
                try:
                    shutil.rmtree(SABA)
                    print("写しを消した")
                    break
                except OSError:
                    time.sleep(2)
            else:
                print("★ 写しを消せなかった（まだ java が掴んでいるかもしれない）: %s" % SABA)
    print("\n結果: PASS %d / FAIL %d" % (len(passed), len(failed)))
    for f in failed:
        print("  FAIL: " + f)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
