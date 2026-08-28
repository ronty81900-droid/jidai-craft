# -*- coding: utf-8 -*-
"""
haichi.py -- 組んだ MOD を、実際に遊んでいる mods フォルダへ入れる。

  ★★ なぜ道具にしたか（2026-08-22）★★
    「直したのに直っていない」が2回 起きた。原因はどちらも
    【実機の mods に古い jar が残っていた】こと。
    しかも案内していた cp のパスが `.minecraft/mods` で、
    実際に遊んでいるのは専用フォルダ（Desktop/実機演習 1.20.1）だった。

    入れたあと、置いた物と組んだ物が同じかを sha256 で必ず照合する。
    「入れたつもり」を残さないため。

  使い方:  python haichi.py
           python haichi.py --muri   … 動作中でも無理やり入れる（勧めない）
"""
import hashlib
import os
import shutil
import subprocess
import sys

KOKO = os.path.dirname(os.path.abspath(__file__))
JAR = os.path.join(KOKO, 'JidaiUI-0.1.0.jar')

# 遊んでいる場所の候補。上から順に、実在する物すべてへ入れる。
# ★ tacz が入っているかで「本当に遊んでいる mods か」を見分ける。
# ★ 家の場所は【パソコンから読む】。ユーザー名を書かない。
#   こうすると、もらった人のパソコンでもそのまま動く。
IE = os.path.expanduser('~')
SAKI = [
    os.path.join(IE, 'Desktop', '実機演習 1.20.1', 'mods'),
    os.path.join(IE, 'AppData', 'Roaming', '.minecraft', 'mods'),
]


def sha(p):
    return hashlib.sha256(open(p, 'rb').read()).hexdigest()


def ugoiteruKa():
    """Minecraft が動いていれば、その手がかりを返す。動いていなければ None。

    ★★ なぜ要るか（2026-08-22 のクラッシュ）★★
      遊んでいる最中に jar を差し替えたら、73秒後に落ちた。
        NoClassDefFoundError: jidai/ui/SokuteiGamen$Kekka

      Forge は起動時に jar の目次（どの class がどこにあるか）を覚える。
      中身を入れ替えると、その目次が実物とずれる。
      **まだ一度も読んでいない class** を読もうとした時に初めて
      「無い」と言われて落ちる。既に読んだ class は平気なので、
      入れ替えた直後は何ごともなく動いて見える。これが厄介な所。

      → 動いている間は入れない。これしか確実な手が無い。
    """
    ps = ("Get-CimInstance Win32_Process"
          " -Filter \"Name='javaw.exe' or Name='java.exe'\""
          " | Where-Object { $_.CommandLine -like '*minecraft*' }"
          " | Select-Object -First 1 -ExpandProperty ProcessId")
    try:
        r = subprocess.run(
            ['powershell', '-NoProfile', '-NonInteractive', '-Command', ps],
            capture_output=True, timeout=60)
    except Exception:
        return None            # 調べられない時は止めない（作業が止まるため）
    pid = r.stdout.decode('cp932', 'replace').strip()
    return pid if pid else None


TOMERU = [
    '[中止] Minecraft が動いています (プロセス %s)。',
    '       動いている間に jar を差し替えると、',
    '       まだ一度も読んでいない class を読んだ時に落ちます。',
    '       （2026-08-22 の NoClassDefFoundError がこれ）',
    '       Minecraft を閉じてから、もう一度 実行してください。',
]


def main():
    if not os.path.exists(JAR):
        raise SystemExit('[中止] 先に python build.py で組んでください')

    # ★ 遊んでいる最中に入れ替えると、あとで必ず落ちる（上の説明）
    pid = ugoiteruKa()
    if pid and '--muri' not in sys.argv:
        raise SystemExit(os.linesep.join(TOMERU) % pid)

    moto = sha(JAR)
    print('組んだ物: %s (%d バイト)' % (moto[:16], os.path.getsize(JAR)))

    ireta = 0
    for d in SAKI:
        if not os.path.isdir(d):
            continue
        tacz = any(f.startswith('tacz-1.20.1') for f in os.listdir(d))
        if not tacz:
            print('  とばす（TaCZ が無いので、遊んでいる場所ではない）: %s' % d)
            continue
        saki = os.path.join(d, os.path.basename(JAR))
        shutil.copy2(JAR, saki)
        ato = sha(saki)
        if ato != moto:
            raise SystemExit('[中止] 置いた物が違います: %s' % saki)
        print('  入れて照合しました: %s' % saki)
        ireta += 1

    if ireta == 0:
        raise SystemExit('[中止] 入れ先が1つも見つかりませんでした。'
                         'SAKI に遊んでいる mods フォルダを足してください')
    print('%d か所に入れました。中身は組んだ物と一致しています。' % ireta)


main()
