# -*- coding: utf-8 -*-
"""
build.py ── Gradle を使わずに Forge の MOD を組む。

  ★★ なぜ Gradle を使わないか ★★
    この環境では Gradle が動かない（Java の NIO Selector が塞がれている）。
    そこで javac と zipfile だけで組む。プラグイン側と同じやり方。

  ★★ なぜこれで本番に載るか ★★
    Forge の本番環境では Minecraft のメソッド名は「SRG 名」になっている。
    ForgeGradle は「本来の名前で書く → SRG 名へ変換して jar にする」
    という二段構えだが、こちらは【最初から SRG 名で書いている】ので
    変換が要らない。だから素の javac で足りる。

  使い方:  python build.py
"""
import os
import shutil
import struct
import subprocess
import sys
import zipfile

KOKO = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(KOKO, 'src', 'main', 'java')
RES = os.path.join(KOKO, 'src', 'main', 'resources')
# ★ Codex 納品の絵。jar の中では assets/jidaiui/... に置く必要があるので、
#   詰める時の起点は ui/ にする（ui/ 直下の設計書などは入れない）。
UI = os.path.join(KOKO, 'ui')
UI_E = os.path.join(UI, 'assets')
# 特殊アイテムの絵と模型（tools/tokushu_moderu.py が作る）。
# ★ ui/ とは分けてある。ui/ は manifest と1対1で点検しているので、
#   別系統の物を混ぜると そちらの検査が誤って赤くなる。
TOKU_E = os.path.join(KOKO, 'tokushu', 'assets')
OUT = os.path.join(KOKO, 'out')
JAR = os.path.join(KOKO, 'JidaiUI-0.1.0.jar')

LIB = os.path.expandvars(r'%APPDATA%\.minecraft\libraries')

# ★ 並び順が大事。Forge が手を入れた Minecraft のクラスを先に見せる。
#   後ろの素の SRG jar が先に来ると、パッチ前の版でコンパイルしてしまう。
JARS = [
    'net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-client.jar',
    'net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-srg.jar',
    'net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-universal.jar',
    'net/minecraftforge/javafmllanguage/1.20.1-47.4.10/javafmllanguage-1.20.1-47.4.10.jar',
    'net/minecraftforge/eventbus/6.2.27/eventbus-6.2.27.jar',
    'net/minecraftforge/mergetool-api/1.0/mergetool-api-1.0.jar',
]

# 上のものが内部で参照している外部の部品。無いと javac が止まる。
YOSO = ['com/google/guava', 'com/google/code/gson', 'org/joml',
        'org/apache/logging/log4j', 'com/mojang/datafixerupper',
        'com/mojang/brigadier', 'it/unimi/dsi', 'org/apache/commons',
        'net/minecraftforge/unsafe', 'net/minecraftforge/JarJarSelector',
        'net/minecraftforge/JarJarMetadata', 'net/minecraftforge/coremods',
        'org/lwjgl', 'com/mojang/authlib', 'com/mojang/logging',
        'com/mojang/blocklist', 'net/minecraftforge/forgespi',
        'net/minecraftforge/securemodules', 'cpw/mods']


def classpath():
    """使う jar の一覧を作る。無いものは早めに気付けるよう、その場で止める。"""
    michi = []
    for rel in JARS:
        p = os.path.join(LIB, rel.replace('/', os.sep))
        if not os.path.exists(p):
            raise SystemExit('[中止] jar がありません: %s' % p)
        michi.append(p)
    for ne, _, fs in os.walk(LIB):
        rel = os.path.relpath(ne, LIB).replace(os.sep, '/')
        if not any(rel.startswith(y) for y in YOSO):
            continue
        if any(x in rel for x in ('1.20.4', '1.21', '1.12', '/1.5')):
            continue
        for f in fs:
            if f.endswith('.jar') and 'sources' not in f:
                michi.append(os.path.join(ne, f))
    return michi


def java_files():
    a = []
    for ne, _, fs in os.walk(SRC):
        for f in fs:
            if f.endswith('.java'):
                a.append(os.path.join(ne, f))
    return sorted(a)


def main():
    print('[1] 前の出力を消します')
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)

    cp = classpath()
    src = java_files()
    print('[2] コンパイルします (java %d 本 / classpath %d 本)' % (len(src), len(cp)))
    ku = os.pathsep.join(cp)
    cmd = ['javac', '--release', '17', '-encoding', 'UTF-8',
           '-nowarn', '-cp', ku, '-d', OUT] + src
    # ★ この環境の javac は日本語のエラーを cp932 で出す。
    #   utf-8 で読むと全部 文字化けして、何が悪いのか分からなくなる（実際に困った）。
    r = subprocess.run(cmd, capture_output=True)
    if r.returncode != 0:
        for b in (r.stdout, r.stderr):
            print(b.decode('cp932', 'replace'))
        raise SystemExit('[中止] コンパイルが通りませんでした')
    print('    OK')

    print('[3] class file version を確かめます（本番は Java 17 = 61）')
    for ne, _, fs in os.walk(OUT):
        for f in fs:
            if not f.endswith('.class'):
                continue
            with open(os.path.join(ne, f), 'rb') as h:
                b = h.read(8)
            v = struct.unpack('>H', b[6:8])[0]
            if v != 61:
                raise SystemExit('[中止] %s が version %d（61 のはず）' % (f, v))
    print('    OK（全部 61）')

    print('[4] jar に詰めます')
    if os.path.exists(JAR):
        os.remove(JAR)
    with zipfile.ZipFile(JAR, 'w', zipfile.ZIP_DEFLATED) as z:
        kazu = 0
        # (歩く場所, 相対パスの起点) の組。起点がずれると jar の中の並びが崩れる。
        for aruku, kiten in ((OUT, OUT), (RES, RES), (UI_E, UI),
                             (TOKU_E, os.path.dirname(TOKU_E))):
            if not os.path.isdir(aruku):
                raise SystemExit('[中止] 詰めるものがありません: %s' % aruku)
            for ne, _, fs in os.walk(aruku):
                for f in fs:
                    p = os.path.join(ne, f)
                    z.write(p, os.path.relpath(p, kiten).replace(os.sep, '/'))
                    kazu += 1
    print('    %s (%d バイト / %d 本)' % (os.path.basename(JAR), os.path.getsize(JAR), kazu))

    print('[5] 中身を点検します')
    with zipfile.ZipFile(JAR) as z:
        naka = z.namelist()
    iru = ['META-INF/mods.toml', 'pack.mcmeta',
           'jidai/ui/JidaiUi.class', 'jidai/ui/Mc.class',
           'jidai/ui/Hyou.class', 'jidai/ui/SokuteiGamen.class',
           'jidai/ui/UiGamen.class', 'jidai/ui/Tsunagu.class',
           'jidai/ui/Keiji.class',
           'assets/jidaiui/textures/gui/page_gunshop_guns.png',
           'assets/jidaiui/textures/gui/card_compact_hover.png']
    warui = [x for x in iru if x not in naka]
    if warui:
        raise SystemExit('[中止] jar に入っていません: %s' % warui)
    print('    OK（要るものは全部入っています）')
    print('')
    print('できました: %s' % JAR)


main()
