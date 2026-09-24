# -*- coding: utf-8 -*-
"""データパックの mcfunction を、実機2台（jikki_mod / jikki_1201）へ配る。

  ★★ なぜ道具にしたか（2026-08-23）★★
    データパックを直しても、実機の world/datapacks にある実物を差し替えないと
    何も変わらない。手で cp すると「入れたつもり」が残る
    （クライアント MOD で2回 起きた。clientmod/haichi.py と同じ考え方）。

  やること:
    1. サーバーが動いていないか見る（動いていたら中止）
    2. 指定したファイルを配る
    3. 配ったあと、1バイトも違わないか照合する

  使い方:
    python tests/dp_haichi.py                 … 変更のあるファイルを全部 配る
    python tests/dp_haichi.py --miru          … 差があるファイルを並べるだけ
"""
import filecmp
import os
import shutil
import subprocess
import sys

NE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOTO = os.path.join(NE, 'datapacks', 'jidai_craft')
SAKI = [
    os.path.join(NE, 'jikki_mod', 'world', 'datapacks', 'jidai_craft'),
    os.path.join(NE, 'jikki_1201', 'world', 'datapacks', 'jidai_craft'),
]


def ugoiteruka():
    """25565 が LISTENING なら、サーバーが動いている。

    ★ TIME_WAIT は閉じた接続の残骸。LISTENING の行だけを見る。
    """
    try:
        r = subprocess.run(['netstat', '-ano'], capture_output=True, timeout=30)
    except Exception:
        return False
    moji = (r.stdout or b'').decode('utf-8', 'replace')
    for gyou in moji.splitlines():
        if '25565' in gyou and 'LISTENING' in gyou:
            return True
    return False


def chigau():
    """元と実機で中身が違うファイルを (相対パス, 配り先) で並べる。"""
    de = []
    for ne, _dirs, files in os.walk(MOTO):
        for f in files:
            p = os.path.join(ne, f)
            sou = os.path.relpath(p, MOTO)
            for s in SAKI:
                q = os.path.join(s, sou)
                if not os.path.exists(q) or not filecmp.cmp(p, q, shallow=False):
                    de.append((sou, s))
    return de


def yobun():
    """配り先にあって、元にはもう無いファイルを (相対パス, 配り先) で並べる。

    ★★ 2026-09-10 に足した ★★
      差し替えだけだと、【消した関数が実機に残り続ける】。
      呼ばれないので黙っているが、次に開いた人は「まだこの決まりがある」と読む。
      実際に senryou_hantei.mcfunction で起きた。
    """
    de = []
    for s in SAKI:
        if not os.path.isdir(s):
            continue
        for ne, _dirs, files in os.walk(s):
            for f in files:
                q = os.path.join(ne, f)
                sou = os.path.relpath(q, s)
                if not os.path.exists(os.path.join(MOTO, sou)):
                    de.append((sou, s))
    return de


def main():
    miru = '--miru' in sys.argv
    kumi = chigau()
    keru = yobun()
    if not kumi and not keru:
        print('OK: 実機2台とも元と同じ。配るものは無い')
        return 0

    if kumi:
        print('差のあるファイル %d 件:' % len(kumi))
        for sou, s in kumi:
            print('  %-52s → %s' % (sou, os.path.relpath(s, NE).split(os.sep)[0]))
    if keru:
        print('元から消えたので、実機からも消すファイル %d 件:' % len(keru))
        for sou, s in keru:
            print('  %-52s ← %s' % (sou, os.path.relpath(s, NE).split(os.sep)[0]))
    if miru:
        print('（--miru なので配っていない）')
        return 0

    if ugoiteruka():
        print('')
        print('★ サーバーが動いている（25565 が LISTENING）。中止した。')
        print('  遊んでいる最中に差し替えると、途中から別のルールになる。')
        return 1

    for sou, s in kumi:
        moto = os.path.join(MOTO, sou)
        saki = os.path.join(s, sou)
        os.makedirs(os.path.dirname(saki), exist_ok=True)
        shutil.copyfile(moto, saki)
    for sou, s in keru:
        os.remove(os.path.join(s, sou))

    # --- 配ったあと照合する（「入れたつもり」を残さない）---------
    nokori = chigau()
    nokori2 = yobun()
    if nokori or nokori2:
        print('')
        print('★ 配ったのに違うファイルが %d 件 / 消し残りが %d 件 ある:'
              % (len(nokori), len(nokori2)))
        for sou, _s in nokori + nokori2:
            print('  ' + sou)
        return 1
    print('')
    print('OK: %d 件 配り、%d 件 消して、実機2台とも元と1バイトも違わないことを照合した'
          % (len(kumi), len(keru)))
    return 0


sys.exit(main())
