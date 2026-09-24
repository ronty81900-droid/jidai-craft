# -*- coding: utf-8 -*-
"""
tokushu_moderu.py -- 特殊アイテムの「絵の差し替え」一式を作る。

  ★★ 仕組み ★★
    サーバーが渡すのは【ただの紙】のまま。名前と CustomModelData だけ付いている。
    クライアント MOD が `minecraft/models/item/paper.json` を上書きして、
    「この番号の紙は、この絵で描く」という対応表を持ち込む。

      ・サーバーに MOD を入れる必要がない
      ・独自アイテムを登録しなくていい（Arclight と喧嘩しない）
      ・MOD を入れていない人には紙のまま見えるが、名前で分かるので困らない

    Forge の mod の assets は vanilla より後に読まれる（＝勝つ）。
    実機のクラッシュ報告にも `Resource Packs: vanilla, tacz_resources, mod_resources`
    と並んでいて、mod_resources が後ろに居ることが確かめられる。

  ★★ 対応表は Shouri.java から読む ★★
    名前・ファイル名・番号を手で写すと、片方だけ直した時に黙って食い違う。
    （食い違うと「絵が出ない」だけで、サーバーは正しく動くので気づけない）

  動かし方:  python tools/tokushu_moderu.py
"""
import hashlib
import io
import json
import os
import re
import shutil
import struct

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
KIKAKU = os.path.dirname(NE)

SHOURI = os.path.join(KIKAKU, 'plugin', 'src', 'main', 'java', 'jidai', 'Shouri.java')
# ★★ 2026-09-09: Codex の絵に戻した ★★
#   v7（自前生成の高解像度 512/256）は2度作り直したが、
#   「粗い・古地図風のクオリティが出ていない」というご判断で採用しない。
#   **実機に出すのは Codex 納品の v5（集める5種 16×16）と v6（遺物11種 64×64）**。
#   v7 と v7_an は記録として残すが、ここからは見ない。
NOUHIN = os.path.join(NE, 'nouhin', 'v5')
NOUHIN_IBUTSU = os.path.join(NE, 'nouhin', 'v6')

# ★★ 2026-09-18: 確定した2枚だけ v8 から取る（ご指示）★★
#   「アイテムは確定した二枚以外は以前のものを一旦使用します」。
#   護符と聖杯は作り直しが決まっている（自前・64×64）。
#   残り14枚は Codex 第7回がそろうまで v5/v6 のまま。
NOUHIN_V8 = os.path.join(NE, 'nouhin', 'v8')
KOBETSU = ('gofu', 'seihai')
SAKI = os.path.join(NE, 'tokushu', 'assets')

# ★★ 2026-09-20: 石油の絵（ユーザーからいただいたドット絵）★★
#   石油は特殊アイテムではない（Shouri.java に無い）。
#   正体は **黒い染料に名前と目印を付けた物**なので、
#   紙ではなく **black_dye.json** の方へ対応表を足す。
SEKIYU = os.path.join(NE, 'nouhin', 'sekiyu')
SEKIYU_DP = os.path.join(KIKAKU, 'datapacks', 'jidai_craft', 'data', 'jidai',
                         'functions', 'sekiyu')
# 黒い染料そのもの（バニラの black_dye.json と同じ中身）。
# ★ ここを間違えると、ふつうの黒い染料まで見た目が壊れる。
SUMI_MOTO = {
    'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'minecraft:item/black_dye'},
}

# 紙の見た目そのもの（バニラの paper.json と同じ中身）。
# ★ ここを間違えると、ふつうの紙まで見た目が壊れる。
KAMI_MOTO = {
    'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'minecraft:item/paper'},
}


def hyou():
    """Shouri.java の TOKUSHU_HYOU（集める5種）を読む。"""
    s = io.open(SHOURI, encoding='utf-8').read()
    m = re.search(r'TOKUSHU_HYOU = \{(.*?)\n    \};', s, re.S)
    if not m:
        raise SystemExit('[中止] Shouri.java に TOKUSHU_HYOU が見つかりません')
    kumi = re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)"\}', m.group(1))
    if len(kumi) < 1:
        raise SystemExit('[中止] TOKUSHU_HYOU を読み取れませんでした')
    return [(a, b, int(c)) for a, b, c in kumi]


def ibutsu_hyou():
    """Shouri.java の IBUTSU_HYOU（遺物8種）を読む。{名前, 絵, 番号, 時代, 効果, 説明}。"""
    s = io.open(SHOURI, encoding='utf-8').read()
    m = re.search(r'IBUTSU_HYOU = \{(.*?)\n    \};', s, re.S)
    if not m:
        return []
    kumi = re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)",\s*"\d",\s*"[A-Z]+",\s*"[^"]*"\}', m.group(1))
    return [(a, b, int(c)) for a, b, c in kumi]


def kaku(p, nakami):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    io.open(p, 'w', encoding='utf-8', newline='\n').write(
        json.dumps(nakami, ensure_ascii=False, indent=2) + '\n')


def sekiyu_bangou():
    """石油の CustomModelData を【データパックから】読む。

    ★★ 数字を写さない ★★
      ここに 8301 と書くと、データパックを直した時に黙って絵が出なくなる
      （サーバーは正しく動くので、実機で見るまで気づけない）。
    ★ 石油を作る所が複数あるので、**全部 同じ番号か**も確かめる。
      1か所でも漏れると、そこで湧いた石油だけ黒い染料のまま出る。
    """
    ban = set()
    for fai in ('dashi_give.mcfunction', 'waku.mcfunction'):
        michi = os.path.join(SEKIYU_DP, fai)
        if not os.path.exists(michi):
            continue
        t = io.open(michi, encoding='utf-8').read()
        for gyou in t.splitlines():
            if 'jidai_sekiyu:"oil"' not in gyou:
                continue
            if not ('give ' in gyou or 'summon ' in gyou):
                continue            # clear（消す側）は番号を見ない。部分一致なので要らない
            m = re.search(r'CustomModelData:(\d+)', gyou)
            ban.add(int(m.group(1)) if m else None)
    if not ban:
        return None
    if len(ban) > 1 or None in ban:
        raise SystemExit('[中止] 石油の CustomModelData が揃っていません: %s'
                         % sorted(x for x in ban if x is not None))
    return ban.pop()


def main():
    kumi = hyou()
    print('Shouri.java から 集める %d 件 読みました' % len(kumi))

    # ── 遺物: 絵が納品されている物だけ対応表に入れる ──
    #   ★ 絵の無い番号を対応表に書くと、紙が「欠けた絵（紫黒）」になる。
    #     無い間は素の紙のまま出す方がよい。納品が来たら自動で入る。
    nai = []
    dokoni = {}                      # 絵のファイル名 → どの納品から取るか
    for mei, fai, ban in ibutsu_hyou():
        moto = os.path.join(NOUHIN_IBUTSU, 'assets', 'jidaiui', 'textures', 'item', fai + '.png')
        if os.path.exists(moto):
            kumi.append((mei, fai, ban))
            dokoni[fai] = NOUHIN_IBUTSU
        else:
            nai.append('%s (%s.png)' % (mei, fai))
    if nai:
        print('  ★ 絵がまだ無い遺物（紙のまま出る）: ' + ' / '.join(nai))

    # ── 確定した絵だけ v8 から取る（2026-09-18 のご指示）──────
    for fai in KOBETSU:
        moto = os.path.join(NOUHIN_V8, 'assets', 'jidaiui', 'textures', 'item', fai + '.png')
        if os.path.exists(moto):
            dokoni[fai] = NOUHIN_V8
            print('  ★ %s.png は v8（確定した絵）から取る' % fai)
        else:
            print('  ★ v8 に %s.png が無いので、以前の絵のまま' % fai)

    # ── 1) 絵を納品から持ってくる（sha256 で照合しながら）──
    # ★ 鍵を【納品ごと】にする。前は名前だけだったので、別の納品から取った絵を
    #   前の納品の sha256 と突き合わせて誤って止まる（v8 の護符 vs v5 の sha）。
    sha = {}
    for nouhin in (NOUHIN, NOUHIN_IBUTSU, NOUHIN_V8):
        mp = os.path.join(nouhin, 'manifest.json')
        if not os.path.exists(mp):
            continue
        man = json.loads(io.open(mp, encoding='utf-8').read())
        for f in man['files']:
            if f.get('category') == 'item_texture':
                sha[(nouhin, os.path.basename(f['file']))] = f['sha256'].upper()

    e_saki = os.path.join(SAKI, 'jidaiui', 'textures', 'item')
    os.makedirs(e_saki, exist_ok=True)
    for _mei, fai, _ban in kumi:
        moto = os.path.join(dokoni.get(fai, NOUHIN), 'assets', 'jidaiui', 'textures', 'item', fai + '.png')
        if not os.path.exists(moto):
            raise SystemExit('[中止] 納品に %s.png がありません' % fai)
        shutil.copy2(moto, os.path.join(e_saki, fai + '.png'))
        h = hashlib.sha256(open(os.path.join(e_saki, fai + '.png'), 'rb').read()) \
            .hexdigest().upper()
        kagi = (dokoni.get(fai, NOUHIN), fai + '.png')
        if sha.get(kagi) and sha[kagi] != h:
            raise SystemExit('[中止] %s.png が納品と違います' % fai)
        print('  絵: %s.png (%s)' % (fai, h[:12]))

    # ── 2) 1枚ごとの模型 ──
    for _mei, fai, _ban in kumi:
        kaku(os.path.join(SAKI, 'jidaiui', 'models', 'item', fai + '.json'),
             {'parent': 'minecraft:item/generated',
              'textures': {'layer0': 'jidaiui:item/' + fai}})

    # ── 3) 紙の対応表（バニラ上書き）──
    #   ★ 番号の小さい順に並べる。Minecraft は上から順に見て
    #     「predicate 以上」で採るので、並びが崩れると別の絵になる。
    kami = dict(KAMI_MOTO)
    kami['overrides'] = [
        {'predicate': {'custom_model_data': ban},
         'model': 'jidaiui:item/' + fai}
        for _mei, fai, ban in sorted(kumi, key=lambda r: r[2])
    ]
    kaku(os.path.join(SAKI, 'minecraft', 'models', 'item', 'paper.json'), kami)

    print('  模型: %d 枚 ＋ 紙の対応表' % len(kumi))

    # ── 3b) 石油（黒い染料の対応表）──────────────────────
    sekiyu_ban = sekiyu_bangou()
    if sekiyu_ban is None:
        print('  ★ 石油: データパックに CustomModelData が無いので、絵を入れない')
    else:
        moto = os.path.join(SEKIYU, 'assets', 'jidaiui', 'textures', 'item', 'sekiyu.png')
        if not os.path.exists(moto):
            raise SystemExit('[中止] 石油の絵がありません（tools/sekiyu_e.py を先に動かす）')
        # ★ 納品の控えと sha256 を突き合わせる（特殊アイテムと同じやり方）
        man = json.loads(io.open(os.path.join(SEKIYU, 'manifest.json'),
                                 encoding='utf-8').read())
        hazu = man['files'][0]['sha256'].upper()
        shutil.copy2(moto, os.path.join(e_saki, 'sekiyu.png'))
        h = hashlib.sha256(open(os.path.join(e_saki, 'sekiyu.png'), 'rb').read()) \
            .hexdigest().upper()
        if h != hazu:
            raise SystemExit('[中止] sekiyu.png が納品と違います')
        kaku(os.path.join(SAKI, 'jidaiui', 'models', 'item', 'sekiyu.json'),
             {'parent': 'minecraft:item/generated',
              'textures': {'layer0': 'jidaiui:item/sekiyu'}})
        sumi = dict(SUMI_MOTO)
        sumi['overrides'] = [{'predicate': {'custom_model_data': sekiyu_ban},
                              'model': 'jidaiui:item/sekiyu'}]
        kaku(os.path.join(SAKI, 'minecraft', 'models', 'item', 'black_dye.json'), sumi)
        print('  石油: sekiyu.png (%s) ＋ 黒い染料の対応表 番号%d'
              % (h[:12], sekiyu_ban))

    # ── 4) MOD が【名前で】絵を引けるように、Java の表も作る ──
    #
    # ★★ なぜ名前でも引くか ★★
    #   紙の対応表（CustomModelData）は、バニラの描画にそのまま効く。
    #   ただ、こちらの古地図の画面は自前で描いているので、
    #   名前が分かれば対応表に頼らず絵を貼れる。
    #   対応表が効かない環境（別のパックが paper.json を上書きしている等）でも
    #   少なくとも こちらの画面だけは正しく出る。
    #   ★ この表は Shouri.java から作る。手で写さない。
    gyou = []
    ookisa = []
    for mei, fai, ban in kumi:
        gyou.append('            {"%s", "%s", "%d"},' % (mei, fai, ban))
        # ★ 絵の実寸（PNG の IHDR）。遺物は 64×64、集める物は 16×16。MOD はこれで縮めて貼る
        png = open(os.path.join(e_saki, fai + '.png'), 'rb').read()
        w, h = struct.unpack('>II', png[16:24])
        ookisa.append('            {%d, %d},' % (w, h))
    java = (
        'package jidai.ui;\n'
        '\n'
        '/**\n'
        ' * 特殊アイテムの表。★★ tools/tokushu_moderu.py が Shouri.java から作る。手で直さない ★★\n'
        ' *   { 名前, 絵のファイル名, 見た目の番号 }\n'
        ' */\n'
        'public final class Tokushu {\n'
        '\n'
        '    private Tokushu() {\n'
        '        // 表を置くだけなので、実体は作らない\n'
        '    }\n'
        '\n'
        '    public static final String[][] HYOU = {\n'
        + '\n'.join(gyou) + '\n'
        '    };\n'
        '\n'
        '    /** 絵の実寸（HYOU と同じ並び）。MOD はこれを 16×16 に縮めて貼る。 */\n'
        '    public static final int[][] OOKISA = {\n'
        + '\n'.join(ookisa) + '\n'
        '    };\n'
        '\n'
        '    /** 印と色の記号を取り除いた名前。 */\n'
        '    private static String souji(String namae) {\n'
        '        String s = namae;\n'
        '        while (!s.isEmpty() && (s.charAt(0) == \'★\' || s.charAt(0) == \'◆\')) {\n'
        '            s = s.substring(1);\n'
        '        }\n'
        '        return s.replaceAll("§.", "").trim();\n'
        '    }\n'
        '\n'
        '    /**\n'
        '     * 名前から絵の道筋を引く。特殊アイテムでなければ null。\n'
        '     * ★ 頭の等級の印（★ ◆）と色の記号は取り除いてから比べる。\n'
        '     */\n'
        '    public static String e(String namae) {\n'
        '        if (namae == null) {\n'
        '            return null;\n'
        '        }\n'
        '        String s = souji(namae);\n'
        '        for (String[] r : HYOU) {\n'
        '            if (r[0].equals(s)) {\n'
        '                return "textures/item/" + r[1] + ".png";\n'
        '            }\n'
        '        }\n'
        '        return null;\n'
        '    }\n'
        '\n'
        '    /** 名前から絵の実寸を引く。知らなければ 16×16。 */\n'
        '    public static int[] ookisa(String namae) {\n'
        '        if (namae == null) {\n'
        '            return new int[]{16, 16};\n'
        '        }\n'
        '        String s = souji(namae);\n'
        '        for (int i = 0; i < HYOU.length; i++) {\n'
        '            if (HYOU[i][0].equals(s)) {\n'
        '                return OOKISA[i];\n'
        '            }\n'
        '        }\n'
        '        return new int[]{16, 16};\n'
        '    }\n'
        '}\n'
    )
    jp = os.path.join(NE, 'src', 'main', 'java', 'jidai', 'ui', 'Tokushu.java')
    io.open(jp, 'w', encoding='utf-8', newline='\n').write(java)
    print('  Java の表: %s' % jp)
    print('できました: %s' % SAKI)
    for mei, fai, ban in kumi:
        print('    %-16s → %s.png (番号 %d)' % (mei, fai, ban))


main()
