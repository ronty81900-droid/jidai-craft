# -*- coding: utf-8 -*-
"""
プラグインの検証ハーネスを、まとめてコンパイルして走らせる (対象: 1.20.1 / Java 17)

**なぜ Gradle を使わないのか**
  この環境では Gradle が動かない（Java の NIO Selector が塞がれていて、
  デーモンが起動できない。実測）。そこで javac と jar を直に叩いている。
  やっていることは Gradle の build と同じ。

**やること**
  1. plugin/src/main/java を javac でコンパイルする（Java 17 で）
  2. src/main/resources と合わせて .jar を組み立てる
  3. tests/harness/*.java をコンパイルする
  4. 全部走らせて、PASS / FAIL を数える

**サーバー本体は要らない**
  ハーネスは Bukkit の API を Proxy と Unsafe で偽装している。
  だから起動しない。ただし「アイテムを実際に作る」所だけはどうしても
  本体が要るので、そこは各ハーネスが「壁に当たった」と記録して先へ進む。

使い方:  python plugin/tests/run_harness.py
終了コード: 0=全部通った / 1=どこかで落ちた / 2=環境要因で実行不可
"""
import os
import re
import shutil
import subprocess

def yomu(b):
    """外の道具の出力を、化けない方の文字コードで読む。

    ★★ なぜ決め打ちにしないか（2026-08-22）★★
      この環境では javac が cp932、java が UTF-8 で日本語を出す。
      片方に決め打ちすると、もう片方が必ず化ける。
      実際、化けたせいで「runTaskLaterの参照はあいまいです」という
      肝心のエラー文が読めず、原因を掴むのに遠回りした。
      UTF-8 は不正な並びを弾くので、通った方を採る形で見分けられる。
    """
    if not b:
        return ""
    for moji in ('utf-8', 'cp932'):
        try:
            return b.decode(moji)
        except UnicodeDecodeError:
            pass
    return b.decode('utf-8', 'replace')
import sys
import tempfile
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
PLUGIN = HERE.parent
SRC = PLUGIN / "src" / "main" / "java"
RES = PLUGIN / "src" / "main" / "resources"
LIBS = HERE / "libs"
HARNESS = HERE / "harness"

# 引数に .jar の場所が要るもの（プラグインの .jar そのものを調べる）
JAR_IRU = ["ClassCheck", "CmdCheck", "YmlCheck", "LoadCheck",
           "OnEnableTest", "Stage6Test", "Stage7Test"]
# ClassCheck だけ2つ目の引数（本体クラスの名前）も要る
HONTAI = "jidai.JidaiCraft"

# ★ SeiryokuTest は「プラグインが実際に送った命令」をここへ書き出す。
#   tests/plugin_renkei.py が、これを本物のサーバーへ流して受理されるか見る。
#   命令を手で写すと写し間違いが起きるので、必ず機械で受け渡す。
CMDS_SAKI = PLUGIN.parent / "tests" / "plugin_cmds.txt"


def javac(*hikisu):
    r = subprocess.run(["javac", *hikisu], capture_output=True)
    r.stdout = yomu(r.stdout)
    r.stderr = yomu(r.stderr)
    return r


def main():
    if not LIBS.exists() or not list(LIBS.glob("*.jar")):
        print("[環境エラー] " + str(LIBS) + " に jar がありません。")
        print("  paper-api-1.20.1.jar と Paper が使うライブラリ一式が要ります。")
        return 2
    if shutil.which("javac") is None:
        print("[環境エラー] javac が PATH にありません（JDK 17 以上が要ります）")
        return 2

    shigoto = Path(tempfile.mkdtemp(prefix="jidai_harness_"))
    try:
        cp = str(LIBS / "*")
        out = shigoto / "out"
        out.mkdir()

        # --- 1) プラグイン本体をコンパイル ---------------------
        print("[1] プラグイン本体をコンパイル中...")
        srcs = sorted(str(p) for p in SRC.rglob("*.java"))
        r = javac("--release", "17", "-encoding", "UTF-8", "-nowarn",
                  "-cp", cp, "-d", str(out), *srcs)
        # ★★ 失敗は【返り値】で見る（2026-08-22 に直した）★★
        #   前は stderr に "error" が入るかで見ていたが、この環境の javac は
        #   日本語で「エラー」と出すため、壊れていても素通りして
        #   【全体が緑のまま】になっていた（UneiTest が0件なのに 0 FAIL と出た。実際に起きた）。
        if r.returncode != 0:
            print("  コンパイル失敗:")
            for l in (r.stderr or "").splitlines()[:10]:
                print("    " + l)
            return 1
        print("  OK (" + str(len(list(out.rglob('*.class')))) + " クラス)")

        # --- 2) .jar を組み立てる ------------------------------
        print("[2] .jar を組み立て中...")
        tsumi = shigoto / "jarbuild"
        shutil.copytree(out, tsumi)
        shutil.copytree(RES, tsumi, dirs_exist_ok=True)
        jar_path = shigoto / "JidaiCraft-test.jar"
        # ★ jar コマンドは PATH に無いことがある（javac だけ通っている環境がある。実測）。
        #   .jar は中身が zip なので、Python の zipfile で同じものを組み立てる。
        #   Gradle が作るものと合わせて、空の MANIFEST.MF も入れておく。
        with zipfile.ZipFile(jar_path, "w", zipfile.ZIP_DEFLATED) as z:
            z.writestr("META-INF/MANIFEST.MF",
                       "Manifest-Version: 1.0" + chr(10) + chr(10))
            for f in sorted(tsumi.rglob("*")):
                if f.is_file():
                    z.write(f, f.relative_to(tsumi).as_posix())
        print("  OK (" + str(jar_path.stat().st_size) + " バイト)")

        # --- 3) ハーネスをコンパイル ---------------------------
        print("[3] ハーネスをコンパイル中...")
        hout = shigoto / "hout"
        hout.mkdir()
        hsrcs = sorted(str(p) for p in HARNESS.glob("*.java"))
        r = javac("--release", "17", "-encoding", "UTF-8", "-nowarn",
                  "-cp", cp + os.pathsep + str(out), "-d", str(hout), *hsrcs)
        # ★★ 失敗は【返り値】で見る（2026-08-22 に直した）★★
        #   前は stderr に "error" が入るかで見ていたが、この環境の javac は
        #   日本語で「エラー」と出すため、壊れていても素通りして
        #   【全体が緑のまま】になっていた（UneiTest が0件なのに 0 FAIL と出た。実際に起きた）。
        if r.returncode != 0:
            print("  コンパイル失敗:")
            for l in (r.stderr or "").splitlines()[:10]:
                print("    " + l)
            return 1
        print("  OK (" + str(len(hsrcs)) + " 本)")

        # --- 4) 走らせる ---------------------------------------
        print("[4] 走らせる")
        cpx = os.pathsep.join([cp, str(out), str(hout)])
        goukei_p, goukei_f, ochita = 0, 0, []
        for p in hsrcs:
            na = Path(p).stem
            hiki = [str(jar_path)] if na in JAR_IRU else []
            if na == "ClassCheck":
                hiki.append(HONTAI)
            if na == "SeiryokuTest":
                hiki.append(str(CMDS_SAKI))
            # ★ 置き場を Java へ渡す（-Djidai.root）。
            #   こうすると、検査の Java 側に絶対パスを書かなくて済む。
            r = subprocess.run(["java", "-Dfile.encoding=UTF-8",
                                "-Djidai.root=" + str(PLUGIN.parent).replace("\\", "/"),
                                "-cp", cpx, na, *hiki],
                               capture_output=True)
            moji = yomu(r.stdout) + yomu(r.stderr)
            np = len(re.findall(r"\[PASS\]", moji))
            nf = len(re.findall(r"\[FAIL\]", moji))
            goukei_p += np
            goukei_f += nf
            shirushi = ""
            if "Exception in thread" in moji:
                shirushi = "  ←落ちた"
                ochita.append(na)
                for l in moji.splitlines():
                    if "Exception" in l or "	at jidai" in l:
                        print("      >> " + l.strip()[:150])
            print("  " + na.ljust(14) + " PASS=" + str(np).ljust(4)
                  + " FAIL=" + str(nf) + shirushi)
            if nf:
                for l in moji.splitlines():
                    if "[FAIL]" in l:
                        print("      " + l.strip())

        print()
        print("=" * 50)
        print("結果: PASS " + str(goukei_p) + " / FAIL " + str(goukei_f))
        if ochita:
            print("落ちたハーネス: " + ", ".join(ochita))
        return 1 if (goukei_f or ochita) else 0
    finally:
        shutil.rmtree(shigoto, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
