# -*- coding: utf-8 -*-
"""
saiyou.py -- 選んでいただいた案を v10 の納品（nouhin/v10）へ写し、manifest を書く。

  ★ どの絵のどの案を採ったかは、ここ（SAIYOU）だけに書く。
    tools/tokushu_moderu.py は nouhin/v10 にある絵を見て、その絵だけ v10 に差し替える。

  流れ: 案を描く（tools/e_v10/<絵>.py → nouhin/v10_an/<絵>/A.png …）
        → ユーザーが選ぶ → SAIYOU に書く → このファイルを動かす
        → python tools/tokushu_moderu.py → python build.py → python haichi.py

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/saiyou.py
"""
import hashlib
import io
import json
import os
import shutil
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dotto512 as D  # noqa: E402

NE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
AN = os.path.join(NE, 'nouhin', 'v10_an')
SAKI = os.path.join(NE, 'nouhin', 'v10')
E_SAKI = os.path.join(SAKI, 'assets', 'jidaiui', 'textures', 'item')

# 絵 → (採った案, 選んだ日, ご指示の言葉)
SAIYOU = {
    'kaizu': ('A', '2026-09-28', '海図A'),
    'haguruma': ('A', '2026-09-28', '歯車A'),
    'tenro': ('A', '2026-09-28', 'ベッセマーA'),
    'kinko': ('A', '2026-09-28', '金庫A'),
    'tsukinoishi': ('A', '2026-09-28', '月の石A'),
    'oukan': ('A', '2026-09-28', '王冠A'),
    'tate': ('B', '2026-09-28', '盾B'),
    'tegata': ('A', '2026-09-28', '手紙A'),          # 「手紙」= 武器商人の手形（3回目で手紙の形の絵はこれだけ）
    'tebiki': ('A', '2026-09-28', '手引書A'),
    'penicillin': ('A', '2026-09-28', 'ペニシリンA'),
    'goggle': ('B', '2026-09-28', 'ゴーグルB'),
    'sumaho': ('B', '2026-09-28', 'スマホB'),
}

# 案を見ていただいたうえで「いまのまま」と決まった絵 → (決めた日, ご指示の言葉)
# ★ ここにある絵は v10 に入れない（jar の v6 の絵をそのまま使う）
IMA_NO_MAMA = {
    'sensha': ('2026-09-28', '戦車今'),
    'excalibur': ('2026-09-28', 'エクスカリバー今'),
    'gofu': ('2026-09-28', '護符今'),          # v8 で確定した③（e_v8/gofu2.py）のまま
    'seihai': ('2026-09-28', '聖杯今'),        # v8 で確定した C2（e_v8/seihai.py の an_c）のまま
}


def main():
    ryouhou = set(SAIYOU) & set(IMA_NO_MAMA)
    if ryouhou:
        raise SystemExit('[中止] 採用と「いまのまま」の両方にある: ' + ', '.join(sorted(ryouhou)))
    os.makedirs(E_SAKI, exist_ok=True)
    # nouhin/v10 に SAIYOU に無い絵が残っていると、tokushu_moderu.py がそれも差し替えてしまう
    nokori = sorted(f[:-4] for f in os.listdir(E_SAKI) if f.endswith('.png') and f[:-4] not in SAIYOU)
    if nokori:
        raise SystemExit('[中止] SAIYOU に無い絵が nouhin/v10 にある: ' + ', '.join(nokori))
    files = []
    for fai, (an, hi, kotoba) in SAIYOU.items():
        moto = os.path.join(AN, fai, an + '.png')
        if not os.path.exists(moto):
            raise SystemExit('[中止] 案の絵が無い: ' + moto)
        im = Image.open(moto)
        ng = [s for ok, s in D.tenken512(im, fai) if not ok]
        if ng:
            raise SystemExit('[中止] %s の案%s が点検に落ちた: %s' % (fai, an, ' / '.join(ng)))
        saki = os.path.join(E_SAKI, fai + '.png')
        shutil.copy2(moto, saki)
        h = hashlib.sha256(open(saki, 'rb').read()).hexdigest().upper()
        files.append({'category': 'item_texture',
                      'file': 'assets/jidaiui/textures/item/%s.png' % fai,
                      'sha256': h, 'size': list(im.size), 'proposal': an,
                      'chosen_on': hi, 'user_words': kotoba,
                      'drawn_by': 'tools/e_v10/%s.py の an_%s()' % (fai, an.lower())})
        print('  %s.png ← 案%s（%s・%s）%s' % (fai, an, kotoba, hi, h[:12]))
    ima = [{'file': 'assets/jidaiui/textures/item/%s.png' % fai, 'decided_on': hi, 'user_words': kotoba,
            'note': '案を見たうえで いまの絵（v6）のまま'} for fai, (hi, kotoba) in IMA_NO_MAMA.items()]
    man = {'schema_version': 1, 'package': 'JidaiCraft 特殊アイテムの絵 v10（512×512・自前）',
           'rules': '128 マスで描いて 4 倍・外周 16px outline＋16px brass・v6 共有パレット＋追加色（dotto.TSUIKA）',
           'files': files, 'kept_current': ima}
    io.open(os.path.join(SAKI, 'manifest.json'), 'w', encoding='utf-8', newline='\n').write(
        json.dumps(man, ensure_ascii=False, indent=1) + '\n')
    print('manifest.json: %d 枚（いまのまま %d 枚）' % (len(files), len(ima)))


if __name__ == '__main__':
    main()
