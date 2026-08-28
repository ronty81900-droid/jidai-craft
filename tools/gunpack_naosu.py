# -*- coding: utf-8 -*-
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
