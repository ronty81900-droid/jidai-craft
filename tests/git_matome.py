# -*- coding: utf-8 -*-
"""GitHub へ上げる一式を、まっさらな別フォルダに組み立てる。

  ★★ なぜ「その場で git init」にしないか ★★
    この作業場は 1.7GB あり、その大半が【他人の物】と【試験用サーバー】。
      jikki_mod/mods      … TaCZ と Simple Voice Chat（他人の MOD）
      jikki_mod/tacz      … 銃パック（他人の作品・110MB）
      jikki_mod/libraries … Forge が自動で落とす物（179MB）
      plugin/tests/libs   … Paper の API 一式（115本・66MB）
      plugin/testserver / tests/*server … 試験用サーバー（450MB）
    ここで `git add -A` を一度でも打つと、他人の作品ごと公開してしまう。
    ★ だから「入れる物だけを名指しで写す」形にした（除外ではなく許可の一覧）。

  ★ 他人の物は1つも入れない。代わりに「外部ファイル.md」に版と入手先を書く。
    銃パックの直しは【6ファイルを消しただけ】なので、消す道具だけ置く。

  使い方:
      python tests/git_matome.py            … 組み立てる
      python tests/git_matome.py --git      … 組み立てて git init + 最初のコミット
"""
import io
import json
import os
import re
import shutil
import subprocess
import sys

MOTO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SAKI = os.path.join(os.path.dirname(MOTO), "jidai-craft")

# ============================================================
# 入れる物（許可の一覧）。ここに書いた物だけが写る。
#   (元の場所, 先の場所, 除外する名前…)
# ============================================================
UTSUSU = [
    # --- 遊ぶのに要る物 ---------------------------------------
    ("jikki_mod/world", "world",
     # 参加者ごとの記録。次の催しでは白紙から始めたいので入れない。
     ["playerdata", "stats", "advancements", "session.lock", "uid.dat"]),
    ("datapacks/jidai_craft", "datapacks/jidai_craft", []),
    ("resourcepack", "resourcepack", ["codex"]),

    # --- 作り直したい人向けの元 -------------------------------
    ("plugin/src", "plugin/src", []),
    ("plugin/tests/harness", "plugin/tests/harness", []),
    ("clientmod/src", "clientmod/src", []),
    ("clientmod/ui", "clientmod/ui", []),
    ("clientmod/tests", "clientmod/tests", []),
    ("clientmod/nouhin", "clientmod/nouhin", []),

    # --- 説明書 -----------------------------------------------
    ("docs", "docs", []),
]

# 1本ずつ写す物
def ichiban_atarashii_jar():
    """plugin/ の JidaiCraft-*.jar のうち、版がいちばん大きい物を返す。
       ★ 版を決め打ちすると、上げるたびに古い jar を配ってしまう。"""
    saidai, mei = None, None
    for f in os.listdir(os.path.join(MOTO, "plugin")):
        m = re.match(r"JidaiCraft-(\d+)\.(\d+)\.(\d+)\.jar$", f)
        if not m:
            continue
        ban = tuple(int(x) for x in m.groups())
        if saidai is None or ban > saidai:
            saidai, mei = ban, f
    assert mei, "plugin/ に JidaiCraft-*.jar が無い"
    return mei


FILE = [
    ("plugin/src/main/resources/plugin.yml", None),      # 目印。写しは上で済み
    ("clientmod/JidaiUI-0.1.0.jar", "clientmod/JidaiUI-0.1.0.jar"),
    ("clientmod/build.py", "clientmod/build.py"),
    ("clientmod/haichi.py", "clientmod/haichi.py"),
    ("plugin/tests/run_harness.py", "plugin/tests/run_harness.py"),
    ("README.md", "docs/もとの README.md"),
    ("TACZ_BUKI.md", "docs/TaCZ 武器一覧と設定.md"),
    ("次のチャットへ.md", "docs/開発の引き継ぎ書.md"),
]

# clientmod/tools と tests は .py だけ（client.txt 7.6MB と cache 4.6MB は入れない。
# どちらも Mojang の配布物から作り直せる）
PY_DAKE = [
    ("clientmod/tools", "clientmod/tools"),
    ("tests", "tests"),
]

# サーバーの設定。★ 個人情報を含む物は入れない
SERVER_OK = ["server.properties", "bukkit.yml", "spigot.yml", "commands.yml",
             "permissions.yml", "help.yml", "arclight.conf", "user_jvm_args.txt",
             "start.bat", "run.bat", "run.sh", "eula.txt", "手順.md"]
SERVER_NG = ["ops.json", "whitelist.json", "usercache.json", "usernamecache.json",
             "banned-players.json", "banned-ips.json"]

# 消した銃（6件）。銃パック本体は配れないので、消す道具だけ置く。
KESU_JUU = [
    "data/hamster/index/guns/colt1873_lb.json",
    "data/hamster/index/guns/coltm1851_chain.json",
    "data/hamster/index/guns/lebel1886_07c.json",
    "data/hamster/index/guns/lugerp08_artillerie.json",
    "data/hamster/index/guns/m1887_hc.json",
    "data/hamster/index/guns/uppercut.json",
]


def mkdir(p):
    if not os.path.isdir(p):
        os.makedirs(p)


def kaku(michi, naka):
    mkdir(os.path.dirname(michi))
    io.open(michi, "w", encoding="utf-8", newline="\n").write(naka)


def utsusu_ki(moto, saki, nozoku):
    """フォルダをまるごと写す。nozoku に載っている名前は飛ばす。"""
    n = 0
    for ne, dirs, files in os.walk(moto):
        dirs[:] = [d for d in dirs if d not in nozoku]
        for f in files:
            if f in nozoku:
                continue
            m = os.path.join(ne, f)
            s = os.path.join(saki, os.path.relpath(m, moto))
            mkdir(os.path.dirname(s))
            shutil.copy2(m, s)
            n += 1
    return n


def main():
    ue = "--ue" in sys.argv
    if os.path.exists(SAKI) and not ue:
        print("★ すでに %s があります。" % SAKI)
        print("  作り直すなら --ue を付けてください（.git は残したまま中身を入れ替えます）")
        return 1
    # ★★ ここで消してはいけない物 ★★
    #   .git   … 履歴とタグ
    #   下の5つ … この道具が作らない、手で書いた物。
    #            消すと毎回 git から戻す羽目になる（実際そうなっていた）。
    NOKOSU = (".git", "README.md", "LICENSE", ".gitignore",
              ".gitattributes", "外部ファイル.md")
    if ue and os.path.exists(SAKI):
        for na in sorted(os.listdir(SAKI)):
            if na in NOKOSU:
                continue
            michi = os.path.join(SAKI, na)
            if os.path.isdir(michi):
                shutil.rmtree(michi)
            else:
                os.remove(michi)
        print("中身を入れ替えました（.git と手書きの5本は残しています）")

    print("組み立て先: %s" % SAKI)
    mkdir(SAKI)
    kazu = 0

    for moto, saki, nozoku in UTSUSU:
        m = os.path.join(MOTO, moto)
        if not os.path.exists(m):
            print("  ★ 元が無い: %s" % moto)
            continue
        n = utsusu_ki(m, os.path.join(SAKI, saki), nozoku)
        kazu += n
        print("  %-34s %4d 本" % (moto + " →", n))

    jar = ichiban_atarashii_jar()
    FILE.append(("plugin/" + jar, "plugin/" + jar))
    print("  %-34s %s" % ("プラグインの jar →", jar))

    for moto, saki in FILE:
        if saki is None:
            continue
        m = os.path.join(MOTO, moto)
        if not os.path.exists(m):
            print("  ★ 元が無い: %s" % moto)
            continue
        s = os.path.join(SAKI, saki)
        mkdir(os.path.dirname(s))
        shutil.copy2(m, s)
        kazu += 1

    for moto, saki in PY_DAKE:
        m = os.path.join(MOTO, moto)
        for f in sorted(os.listdir(m)):
            if not f.endswith(".py"):
                continue
            s = os.path.join(SAKI, saki, f)
            mkdir(os.path.dirname(s))
            shutil.copy2(os.path.join(m, f), s)
            kazu += 1
        print("  %-34s .py だけ" % (moto + " →"))

    # --- サーバーの設定 -------------------------------------
    sv = os.path.join(SAKI, "server")
    mkdir(sv)
    for f in SERVER_OK:
        m = os.path.join(MOTO, "jikki_mod", f)
        if os.path.exists(m):
            shutil.copy2(m, os.path.join(sv, f))
            kazu += 1
    print("  %-34s %d 本" % ("jikki_mod の設定 →", len(SERVER_OK)))

    # --- 銃パックから6件を消す道具 --------------------------
    kaku(os.path.join(SAKI, "tools", "gunpack_naosu.py"), GUNPACK_TOOL)

    print()
    print("写した本数: %d" % kazu)
    print("大きさ: %.1f MB" % (ookisa(SAKI) / 1048576.0))

    # ── 表紙の README が、実物の jar を指しているか ──────────
    #   ★★ 2026-08-29 に見つかった事故 ★★
    #     README が JidaiCraft-0.54.0.jar と書いたまま残り、
    #     実物は 0.55.0 だった。README のとおりに置いた人は
    #     【存在しないファイル】を探すことになる。
    #     README は手で書く物なので、組み立てのたびにここで照合する。
    yomi = os.path.join(SAKI, "README.md")
    if os.path.exists(yomi):
        moji = io.open(yomi, encoding="utf-8").read()
        chigau = []
        for j in (jar, "JidaiUI-0.1.0.jar"):
            if j not in moji:
                chigau.append(j)
        # 古い版が残っていないかも見る
        for furui in re.findall(r"JidaiCraft-\d+\.\d+\.\d+\.jar", moji):
            if furui != jar:
                chigau.append(furui + "（実物は " + jar + "）")
        if chigau:
            print()
            print("★★ README.md が実物と食い違っています ★★")
            for x in chigau:
                print("   " + x)
            sys.exit(1)
        print("README.md は実物の jar を指しています")
    return 0


def ookisa(p):
    g = 0
    for ne, _, files in os.walk(p):
        for f in files:
            g += os.path.getsize(os.path.join(ne, f))
    return g


GUNPACK_TOOL = '''# -*- coding: utf-8 -*-
"""銃パックから、時代クラフトで使わない6丁を消す。

  ★ 銃パック本体（GunpowderRevolution v1.2.7）は他人の作品なので、
    この置き場には入っていません。配布元から自分で落としてください
    （入手先は「外部ファイル.md」）。

  この道具がやることは【6つの json を消すだけ】です。
  中身は1バイトも書き換えません（元と直した物を突き合わせて確認済み）。

  使い方:
      python tools/gunpack_naosu.py GunpowderRevolution_gunpack_v1.2.7.zip
  出来る物:
      GunpowderRevolution_gunpack_v1.2.7_naoshi.zip
"""
import os
import shutil
import sys
import zipfile

KESU = [
    "data/hamster/index/guns/colt1873_lb.json",
    "data/hamster/index/guns/coltm1851_chain.json",
    "data/hamster/index/guns/lebel1886_07c.json",
    "data/hamster/index/guns/lugerp08_artillerie.json",
    "data/hamster/index/guns/m1887_hc.json",
    "data/hamster/index/guns/uppercut.json",
]

if len(sys.argv) < 2:
    print(__doc__)
    raise SystemExit(1)

moto = sys.argv[1]
saki = os.path.splitext(moto)[0] + "_naoshi.zip"

a = zipfile.ZipFile(moto)
naka = {i.filename for i in a.infolist()}
nai = [k for k in KESU if k not in naka]
if nai:
    print("★ 消すはずの物が元に入っていません。パックの版が違います:")
    for k in nai:
        print("   ", k)
    raise SystemExit(1)

with zipfile.ZipFile(saki, "w", zipfile.ZIP_DEFLATED) as b:
    for i in a.infolist():
        if i.filename in KESU:
            continue
        b.writestr(i, a.read(i.filename))

print("できました: %s" % saki)
print("  %d 件 → %d 件（6件 消した）" % (len(naka), len(naka) - 6))
'''


if __name__ == "__main__":
    sys.exit(main())
