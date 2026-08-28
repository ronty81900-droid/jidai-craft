# -*- coding: utf-8 -*-
"""
namae.py ── Minecraft の「本来の名前」と「SRG 名」の対応表を作り、引く。

  ★★ なぜ要るか ★★
    Forge の本番環境では、Minecraft のメソッド名は m_280488_ のような
    SRG 名になっている。名前を見ても何のメソッドか分からないので、
    そのまま書くと【読めないコード】になる（作法3に反する）。

    そこで2つの実物を突き合わせて、対応表を自分で作る。

      client.txt      … Mojang 公式。本来の名前 → 難読名
      mappings-merged … Forge 同梱。難読名 → SRG 名

    どちらも「難読名」を持っているので、そこで繋げば
    本来の名前 ⇔ SRG 名 が出る。推測は一切していない。

  ★ client.txt は再配布禁止（Mojang のライセンス）。MOD には同梱しない。

  使い方:
    python namae.py 作る                       … 対応表を作って cache へ書く
    python namae.py 引く <クラス> <メソッド名>   … SRG 名を引く
    python namae.py 逆引き <クラス> <SRG名>     … SRG 名から本来の名前を引く
"""
import io
import json
import os
import sys

KOKO = os.path.dirname(os.path.abspath(__file__))
CLIENT_TXT = os.path.join(KOKO, 'client.txt')
# ★ 家の場所は【パソコンから読む】。ユーザー名を書かない。
MERGED = (os.path.expanduser('~').replace('\\', '/')
          + '/AppData/Roaming/.minecraft/libraries/de/oceanlabs'
          + '/mcp/mcp_config/1.20.1-20230612.114412'
          + '/mcp_config-1.20.1-20230612.114412-mappings-merged.txt')
CACHE = os.path.join(KOKO, 'namae_cache.json')

GENSHI = {'int': 'I', 'void': 'V', 'boolean': 'Z', 'byte': 'B', 'char': 'C',
          'short': 'S', 'long': 'J', 'float': 'F', 'double': 'D'}


def kata_to_desc(kata, obf_no):
    """本来の型名（例 net.minecraft.client.gui.Font）を、
    難読名の descriptor（例 Lfyc;）へ変換する。配列は [ を付ける。"""
    haisuu = 0
    while kata.endswith('[]'):
        haisuu += 1
        kata = kata[:-2]
    if kata in GENSHI:
        moto = GENSHI[kata]
    else:
        uchi = kata.replace('.', '/')
        moto = 'L' + obf_no.get(uchi, uchi) + ';'
    return '[' * haisuu + moto


def yomu_client_txt():
    """Mojang 公式を読む。
    返り値: (本来クラス→難読クラス,
             {(難読クラス, 難読メソッド, 難読desc): 本来メソッド名},
             {(難読クラス, 難読フィールド): 本来フィールド名})"""
    obf_no = {}
    gyou = []                       # (本来クラス, 本来メソッド, 戻り型, 引数, 難読メソッド)
    fields = []                     # (本来クラス, 本来フィールド, 難読フィールド)
    ima = None
    for line in io.open(CLIENT_TXT, encoding='utf-8'):
        if line.startswith('#'):
            continue
        if not line.startswith(' '):
            # "com.mojang.blaze3d.Blaze3D -> ega:"
            hidari, migi = line.rstrip().split(' -> ')
            ima = hidari.replace('.', '/')
            obf_no[ima] = migi.rstrip(':').replace('.', '/')
            continue
        t = line.strip()
        if ' -> ' not in t:
            continue
        naka, nanmoku = t.rsplit(' -> ', 1)
        if '(' not in naka:
            # フィールド: "型 名前 -> 難読名"
            bu = naka.split(' ')
            if len(bu) >= 2:
                fields.append((ima, bu[-1], nanmoku))
            continue
        atama, hikisuu = naka.split('(', 1)
        hikisuu = hikisuu.rstrip(')')
        bu = atama.split(' ')
        modori, mei = bu[-2], bu[-1]
        if ':' in modori:
            modori = modori.split(':')[-1]
        gyou.append((ima, mei, modori, hikisuu, nanmoku))

    hyo = {}
    for cls, mei, modori, hikisuu, nanmoku in gyou:
        hiki = [x for x in hikisuu.split(',') if x]
        desc = '(' + ''.join(kata_to_desc(x, obf_no) for x in hiki) + ')' \
               + kata_to_desc(modori, obf_no)
        hyo[(obf_no.get(cls, cls), nanmoku, desc)] = mei
    fhyo = {}
    for cls, mei, nanmoku in fields:
        fhyo[(obf_no.get(cls, cls), nanmoku)] = mei
    return obf_no, hyo, fhyo


def yomu_merged():
    """Forge 同梱の対応表を読む。
    返り値: {(難読クラス, 難読メソッド, 難読desc): (本来クラス, SRG名)}"""
    hyo = {}
    fhyo = {}
    ima_obf = ima_hon = None
    for line in io.open(MERGED, encoding='utf-8'):
        if line.startswith('tsrg2'):
            continue
        if not line.startswith('\t'):
            bu = line.rstrip().split(' ')
            if len(bu) >= 2:
                ima_obf, ima_hon = bu[0], bu[1]
            continue
        bu = line.strip().split(' ')
        if len(bu) == 3:            # メソッド: 難読名 desc SRG名
            hyo[(ima_obf, bu[0], bu[1])] = (ima_hon, bu[2])
        elif len(bu) == 2:          # フィールド: 難読名 SRG名
            fhyo[(ima_obf, bu[0])] = (ima_hon, bu[1])
    return hyo, fhyo


def tsukuru():
    print('client.txt を読みます…')
    _, mojang, mojang_f = yomu_client_txt()
    print('  メソッド %d 件 / フィールド %d 件' % (len(mojang), len(mojang_f)))
    print('mappings-merged を読みます…')
    forge, forge_f = yomu_merged()
    print('  メソッド %d 件 / フィールド %d 件' % (len(forge), len(forge_f)))

    dekita = {}
    for kagi, srg_kumi in forge.items():
        hon_mei = mojang.get(kagi)
        if hon_mei is None:
            continue
        hon_cls, srg = srg_kumi
        dekita.setdefault(hon_cls, {}).setdefault(hon_mei, []).append([srg, kagi[2]])
    dekita_f = {}
    for kagi, srg_kumi in forge_f.items():
        hon_mei = mojang_f.get(kagi)
        if hon_mei is None:
            continue
        hon_cls, srg = srg_kumi
        dekita_f.setdefault(hon_cls, {})[hon_mei] = srg
    kazu = sum(len(v) for v in dekita.values())
    kazu_f = sum(len(v) for v in dekita_f.values())
    print('突き合わせできた: クラス %d / メソッド名 %d / フィールド %d'
          % (len(dekita), kazu, kazu_f))
    io.open(CACHE, 'w', encoding='utf-8').write(json.dumps(
        {'method': dekita, 'field': dekita_f}, ensure_ascii=False, separators=(',', ':')))
    print('書きました: %s' % CACHE)


def yomu_cache():
    if not os.path.exists(CACHE):
        raise SystemExit('先に「python namae.py 作る」を実行してください')
    return json.loads(io.open(CACHE, encoding='utf-8').read())


def hiku(cls, mei):
    zen = yomu_cache()
    d, df = zen['method'], zen['field']
    uchi = cls.replace('.', '/')
    if uchi in df and mei in df[uchi]:
        print('%s # %s  ← フィールド' % (cls, mei))
        print('    %s' % df[uchi][mei])
        return
    c = d.get(uchi)
    if c is None:
        raise SystemExit('そのクラスは対応表にありません: ' + cls)
    v = c.get(mei)
    if v is None:
        niteru = [k for k in c if mei.lower() in k.lower()]
        niteru += ['(フィールド)' + k for k in df.get(uchi, {}) if mei.lower() in k.lower()]
        raise SystemExit('ありません。似た名前: %s' % (niteru[:12] or '(無し)'))
    print('%s # %s' % (cls, mei))
    for srg, desc in v:
        print('    %-14s %s' % (srg, desc))
    if len(v) == 1:
        print('    ★ 1件だけ = そのまま使える')


def gyakubiki(cls, srg):
    zen = yomu_cache()
    d, df = zen['method'], zen['field']
    uchi = cls.replace('.', '/')
    for mei, s in df.get(uchi, {}).items():
        if s == srg:
            print('%s # %s  →  本来の名前は【%s】（フィールド）' % (cls, srg, mei))
            return
    c = d.get(uchi, {})
    for mei, v in c.items():
        for s, desc in v:
            if s == srg:
                print('%s # %s  →  本来の名前は【%s】' % (cls, srg, mei))
                print('    %s' % desc)
                return
    raise SystemExit('見つかりません: %s # %s' % (cls, srg))


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    w = sys.argv[1]
    if w == '作る':
        tsukuru()
    elif w == '引く':
        hiku(sys.argv[2], sys.argv[3])
    elif w == '逆引き':
        gyakubiki(sys.argv[2], sys.argv[3])
    else:
        raise SystemExit(__doc__)
