# -*- coding: utf-8 -*-
"""
mod_kakunin.py ── 組んだ MOD を、ゲームを起動せずに機械で点検する。

  ★★ なぜ要るか ★★
    javac が通っても「意図した別のメソッドを呼んでいる」事故は防げない。
    たとえば GuiGraphics には (IIIII)V のメソッドが2つあり、
    片方は fill（四角を塗る）、もう片方は renderOutline（枠を描く）。
    名前が m_280509_ と m_280637_ なので、取り違えてもコンパイルは通る。
    そこで【SRG 名 → 本来の名前】を逆に引いて、意図と合っているかを見る。

  動かし方:  python tests/mod_kakunin.py
"""
import io
import json
import os
import sys
import struct as _struct
import zipfile

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
CACHE = os.path.join(NE, 'tools', 'namae_cache.json')
JAR = os.path.join(NE, 'JidaiUI-0.1.0.jar')

# (どのファイルに書いてあるか, クラス, 本来の名前, 使っている SRG 名)
KITAI = [
    ('Mc.java', 'net/minecraft/client/Minecraft',            'getInstance',    'm_91087_'),
    ('Mc.java', 'net/minecraft/client/Minecraft',            'font',           'f_91062_'),
    ('Mc.java', 'net/minecraft/client/Minecraft',            'screen',         'f_91080_'),
    ('Mc.java', 'net/minecraft/client/Minecraft',            'setScreen',      'm_91152_'),
    ('Mc.java', 'net/minecraft/network/chat/Component',      'literal',        'm_237113_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics',      'fill',           'm_280509_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics',      'drawString',     'm_280488_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics',      'pose',           'm_280168_'),
    ('Mc.java', 'net/minecraft/client/gui/Font',             'width',          'm_92895_'),
    ('Mc.java', 'net/minecraft/client/gui/Font',             'split',          'm_92923_'),
    ('Mc.java', 'net/minecraft/client/gui/Font',             'width',          'm_92724_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics',      'drawString',     'm_280649_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics',      'blit',           'm_280163_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics',      'renderItem',     'm_280480_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics', 'renderItemDecorations', 'm_280370_'),
    ('Mc.java', 'net/minecraft/core/registries/BuiltInRegistries', 'ITEM',      'f_257033_'),
    ('Mc.java', 'net/minecraft/core/Registry',                'get',           'm_7745_'),
    ('Mc.java', 'net/minecraft/world/inventory/AbstractContainerMenu', 'getItems',   'm_38927_'),
    ('Mc.java', 'net/minecraft/world/inventory/AbstractContainerMenu', 'containerId', 'f_38840_'),
    ('Mc.java', 'net/minecraft/client/multiplayer/MultiPlayerGameMode',
     'handleInventoryMouseClick', 'm_171799_'),
    ('Mc.java', 'net/minecraft/client/Minecraft',            'gameMode',       'f_91072_'),
    ('Mc.java', 'net/minecraft/client/Minecraft',            'player',         'f_91074_'),
    ('Mc.java', 'net/minecraft/world/entity/player/Player',  'closeContainer', 'm_6915_'),
    ('Mc.java', 'net/minecraft/world/item/ItemStack',        'isEmpty',        'm_41619_'),
    ('Mc.java', 'net/minecraft/world/item/ItemStack',        'getItem',        'm_41720_'),
    ('Mc.java', 'net/minecraft/world/item/ItemStack',        'setTag',         'm_41751_'),
    ('Mc.java', 'net/minecraft/nbt/TagParser',               'parseTag',       'm_129359_'),
    ('Mc.java', 'net/minecraft/world/level/Level',           'getScoreboard',  'm_6188_'),
    ('Mc.java', 'net/minecraft/world/scores/Scoreboard',     'getObjective',   'm_83477_'),
    ('Mc.java', 'net/minecraft/world/scores/Scoreboard', 'getOrCreatePlayerScore', 'm_83471_'),
    ('Mc.java', 'net/minecraft/world/scores/Score',          'getScore',       'm_83400_'),
    ('Mc.java', 'net/minecraft/client/Minecraft',            'level',          'f_91073_'),
    ('Mc.java', 'net/minecraft/world/entity/Entity',    'getScoreboardName',   'm_6302_'),
    ('Mc.java', 'net/minecraft/client/Minecraft',            'options',        'f_91066_'),
    ('Mc.java', 'net/minecraft/client/Options',              'keyInventory',   'f_92092_'),
    ('Mc.java', 'net/minecraft/client/KeyMapping',           'matches',        'm_90832_'),
    ('Mc.java', 'net/minecraft/world/scores/Scoreboard', 'getDisplayObjective', 'm_83416_'),
    ('Mc.java', 'net/minecraft/world/scores/Scoreboard',     'getPlayerScores', 'm_83498_'),
    ('Mc.java', 'net/minecraft/world/scores/Objective',      'getDisplayName', 'm_83322_'),
    ('Mc.java', 'net/minecraft/world/scores/Score',          'getOwner',       'm_83405_'),
    ('Mc.java', 'net/minecraft/world/entity/Entity',         'getTeam',        'm_5647_'),
    ('Mc.java', 'net/minecraft/world/scores/PlayerTeam',     'getColor',       'm_7414_'),
    ('Mc.java', 'net/minecraft/ChatFormatting',              'getId',          'm_126656_'),
    ('Keiji.java', 'com/mojang/blaze3d/platform/Window',  'getGuiScaledWidth', 'm_85445_'),
    ('Keiji.java', 'com/mojang/blaze3d/platform/Window', 'getGuiScaledHeight', 'm_85446_'),
    ('UiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'onClose',     'm_7379_'),
    ('Tsunagu.java', 'net/minecraft/client/gui/screens/Screen', 'getTitle',    'm_96636_'),
    ('Tsunagu.java', 'net/minecraft/client/gui/screens/inventory/MenuAccess',
     'getMenu', 'm_6262_'),
    ('Tsunagu.java', 'net/minecraft/world/item/ItemStack',   'isEmpty',        'm_41619_'),
    ('Tsunagu.java', 'net/minecraft/world/item/ItemStack',   'getHoverName',   'm_41786_'),
    ('Mc.java', 'com/mojang/blaze3d/vertex/PoseStack',       'pushPose',       'm_85836_'),
    ('Mc.java', 'com/mojang/blaze3d/vertex/PoseStack',       'popPose',        'm_85849_'),
    ('Mc.java', 'com/mojang/blaze3d/vertex/PoseStack',       'scale',          'm_85841_'),
    ('Mc.java', 'com/mojang/blaze3d/vertex/PoseStack',       'translate',      'm_252880_'),
    ('Mc.java', 'net/minecraft/client/gui/screens/Screen',   'width',          'f_96543_'),
    ('Mc.java', 'net/minecraft/client/gui/screens/Screen',   'height',         'f_96544_'),
    ('SokuteiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'render',           'm_88315_'),
    ('SokuteiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'isPauseScreen',    'm_7043_'),
    ('SokuteiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'renderBackground', 'm_280273_'),
    ('UiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'render',           'm_88315_'),
    ('UiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'isPauseScreen',    'm_7043_'),
    ('UiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'renderBackground', 'm_280273_'),
    ('UiGamen.java', 'net/minecraft/client/gui/screens/Screen', 'keyPressed',       'm_7933_'),
    ('UiGamen.java', 'net/minecraft/client/gui/components/events/GuiEventListener',
     'mouseClicked', 'm_6375_'),
    ('JurenGamen.java', 'net/minecraft/client/gui/screens/Screen', 'render',           'm_88315_'),
    ('JurenGamen.java', 'net/minecraft/client/gui/screens/Screen', 'isPauseScreen',    'm_7043_'),
    ('JurenGamen.java', 'net/minecraft/client/gui/screens/Screen', 'renderBackground', 'm_280273_'),
    ('JurenGamen.java', 'net/minecraft/client/gui/screens/Screen', 'keyPressed',       'm_7933_'),
    ('JurenGamen.java', 'net/minecraft/client/gui/screens/Screen', 'onClose',          'm_7379_'),
    ('JurenGamen.java', 'net/minecraft/client/gui/components/events/GuiEventListener',
     'mouseClicked', 'm_6375_'),
    ('JurenGamen.java', 'net/minecraft/world/item/ItemStack',   'isEmpty',        'm_41619_'),
    ('JurenGamen.java', 'net/minecraft/world/item/ItemStack',   'getHoverName',   'm_41786_'),
    ('KyotsuGamen.java', 'net/minecraft/client/gui/screens/Screen', 'render',           'm_88315_'),
    ('KyotsuGamen.java', 'net/minecraft/client/gui/screens/Screen', 'isPauseScreen',    'm_7043_'),
    ('KyotsuGamen.java', 'net/minecraft/client/gui/screens/Screen', 'renderBackground', 'm_280273_'),
    ('KyotsuGamen.java', 'net/minecraft/client/gui/screens/Screen', 'keyPressed',       'm_7933_'),
    ('KyotsuGamen.java', 'net/minecraft/client/gui/screens/Screen', 'onClose',          'm_7379_'),
    ('KyotsuGamen.java', 'net/minecraft/client/gui/components/events/GuiEventListener',
     'mouseClicked', 'm_6375_'),
    ('KyotsuGamen.java', 'net/minecraft/world/item/ItemStack',   'isEmpty',        'm_41619_'),
    ('KyotsuGamen.java', 'net/minecraft/world/item/ItemStack',   'getHoverName',   'm_41786_'),
    ('KyotsuGamen.java', 'net/minecraft/world/inventory/AbstractContainerMenu',
     'getItems', 'm_38927_'),
    ('Mc.java', 'net/minecraft/world/item/ItemStack',   'getTooltipLines', 'm_41651_'),
    ('Mc.java', 'net/minecraft/world/item/TooltipFlag', 'NORMAL',          'f_256752_'),
    ('Kazari.java', 'net/minecraft/world/item/ItemStack', 'isEmpty',        'm_41619_'),
    ('Kazari.java', 'net/minecraft/world/item/ItemStack', 'getHoverName',   'm_41786_'),
    ('Mc.java', 'net/minecraft/client/player/LocalPlayer', 'connection',      'f_108617_'),
    ('Mc.java', 'net/minecraft/client/gui/GuiGraphics',     'blit',            'm_280411_'),
    ('Mc.java', 'net/minecraft/client/multiplayer/ClientPacketListener', 'sendCommand', 'm_246623_'),
    # ── ボスバーの描き直し（2026-09-09）──
    ('Mc.java', 'net/minecraft/client/gui/Font', 'lineHeight',        'f_92710_'),
    ('Bosubaa.java', 'net/minecraft/world/BossEvent', 'getName',      'm_18861_'),
    ('Bosubaa.java', 'net/minecraft/world/BossEvent', 'getColor',     'm_18862_'),
    ('Bosubaa.java', 'net/minecraft/world/BossEvent', 'getProgress',  'm_142717_'),
]

pass_ = 0
fail_ = 0


def check(namae, jouken, shousai=''):
    global pass_, fail_
    if jouken:
        pass_ += 1
        print('[PASS] ' + namae)
    else:
        fail_ += 1
        print('[FAIL] ' + namae + '  ' + str(shousai))


def main():
    print('=' * 60)
    print('MOD を、ゲームを起動せずに 点検する')
    print('=' * 60)

    if not os.path.exists(CACHE):
        print('[SKIP] 対応表がありません。先に python tools/namae.py 作る')
        return 0
    zen = json.loads(io.open(CACHE, encoding='utf-8').read())
    hou, fld = zen['method'], zen['field']

    # ── 1) SRG 名が、本当にその名前のメソッド／フィールドか ──
    for f, cls, hon, srg in KITAI:
        if srg.startswith('f_'):
            jitsu = fld.get(cls, {}).get(hon)
            check('%s: %s#%s は %s' % (f, cls.split('/')[-1], hon, srg),
                  jitsu == srg, '実際は %s' % jitsu)
        else:
            kouho = [s for s, _d in hou.get(cls, {}).get(hon, [])]
            check('%s: %s#%s は %s' % (f, cls.split('/')[-1], hon, srg),
                  srg in kouho, '%s の候補は %s' % (hon, kouho))

    # ── 2) その SRG 名が、実際にソースに書かれているか ──
    #    表だけ直してソースを直し忘れる、を防ぐ
    for f in ('Mc.java', 'SokuteiGamen.java', 'UiGamen.java', 'Tsunagu.java',
              'Keiji.java', 'JurenGamen.java', 'KyotsuGamen.java', 'Kazari.java'):
        p = os.path.join(NE, 'src', 'main', 'java', 'jidai', 'ui', f)
        s = io.open(p, encoding='utf-8').read()
        nai = [srg for ff, _c, _h, srg in KITAI if ff == f and srg not in s]
        check('%s: 表に載せた SRG 名がすべてソースにある' % f, not nai, nai)

    # ── 3) ソースに、表に載っていない SRG 名が紛れていないか ──
    import re
    shitteru = set(srg for _f, _c, _h, srg in KITAI)
    for f in ('Mc.java', 'SokuteiGamen.java', 'UiGamen.java', 'Tsunagu.java',
              'Keiji.java', 'JidaiUi.java', 'Hyou.java', 'JurenGamen.java',
              'KyotsuGamen.java', 'Kazari.java', 'Tokushu.java'):
        p = os.path.join(NE, 'src', 'main', 'java', 'jidai', 'ui', f)
        s = io.open(p, encoding='utf-8').read()
        mitsuketa = set(re.findall(r'\b[mf]_\d+_\b', s))
        hoka = sorted(mitsuketa - shitteru)
        check('%s: 素性の分からない SRG 名が無い' % f, not hoka, hoka)

    # ── 3.5) 共通画面が、いちばん狭い画面でも溢れないか ──
    #
    # ★★ なぜ計算するか ★★
    #   「はみ出している」は実機でしか見えない、と思い込んで
    #   2度 やり直した（掲示板のサイドバー）。
    #   板と枠の大きさは ただの算数なので、ここで確かめられる。
    #   Minecraft の自動倍率では論理幅が 320 まで下がる。そこで検算する。
    import re as _re2
    kp = os.path.join(NE, 'src', 'main', 'java', 'jidai', 'ui', 'KyotsuGamen.java')
    ks = io.open(kp, encoding='utf-8').read()

    def teisuu(mei):
        m = _re2.search(r'%s = (\d+);' % mei, ks)
        return int(m.group(1)) if m else None

    dan = teisuu('DAN')
    hiroi = teisuu('MASU_HIROI')
    sebai = teisuu('MASU_SEBAI')
    takasaM = teisuu('MASU_TAKASA')
    check('共通画面の定数が読めた',
          None not in (dan, hiroi, sebai, takasaM),
          'DAN=%s HIROI=%s SEBAI=%s TAKASA=%s' % (dan, hiroi, sebai, takasaM))

    if None not in (dan, hiroi, sebai, takasaM):
        # 一番きつい形: 9枠 × 2段（ガチャが回っている時）
        for gh, gt, mei in ((320, 240, '自動倍率のいちばん狭い画面'),
                            (480, 253, '実機（1920x1009 / 倍率4）')):
            masu = max(sebai, min(hiroi, (gh - 40) // dan))
            haba = masu * dan + 16
            takasa = 24 + 2 * takasaM + 10
            check('%s（%dx%d）で 横に溢れない' % (mei, gh, gt), haba <= gh,
                  '板 %d > 画面 %d' % (haba, gh))
            check('%s（%dx%d）で 縦に溢れない' % (mei, gh, gt), takasa <= gt,
                  '板 %d > 画面 %d' % (takasa, gt))
            print('      %s: 板 %dx%d（ひと枠 %d）' % (mei, haba, takasa, masu))

    # ── 4) jar の中身 ──
    check('jar ができている', os.path.exists(JAR), JAR)
    if os.path.exists(JAR):
        with zipfile.ZipFile(JAR) as z:
            naka = z.namelist()
            toml = z.read('META-INF/mods.toml').decode('utf-8')
        for iru in ('META-INF/mods.toml', 'pack.mcmeta',
                    'jidai/ui/JidaiUi.class', 'jidai/ui/Mc.class',
                    'jidai/ui/Hyou.class', 'jidai/ui/SokuteiGamen.class',
                    'jidai/ui/UiGamen.class', 'jidai/ui/Tsunagu.class',
                    'jidai/ui/Keiji.class', 'jidai/ui/JurenGamen.class',
                    'jidai/ui/KyotsuGamen.class', 'jidai/ui/Kazari.class',
                    'assets/minecraft/models/item/paper.json',
                    'assets/jidaiui/textures/gui/page_gunshop_guns.png'):
            check('jar に %s が入っている' % iru, iru in naka)
        check('mods.toml の modId が jidaiui', 'modId = "jidaiui"' in toml)
        check('mods.toml の modLoader が javafml', 'modLoader = "javafml"' in toml)
        check('mods.toml が 1.20.1 向け', '[1.20.1,1.20.2)' in toml)
        # ★ @Mod の値と mods.toml の modId がずれると、黙って読み込まれない
        with zipfile.ZipFile(JAR) as z:
            b = z.read('jidai/ui/JidaiUi.class')
        check('@Mod の値が jidaiui（mods.toml と一致）', b'jidaiui' in b)

        # ── 5) 組んだ class が【1本残らず】jar に入っているか ──
        #
        # ★★ なぜ足したか（2026-08-22 のクラッシュ）★★
        #   NoClassDefFoundError: jidai/ui/SokuteiGamen$Kekka で落ちた。
        #   あの時の原因は入れ替えの手順（動作中に差し替えた）だったが、
        #   詰め残しでも まったく同じ落ち方をする。しかも
        #   **その class を初めて読む場面に入るまで気づけない**。
        #   入れ子の class は名前に $ が付くので、うっかり弾きやすい。
        out = os.path.join(NE, 'out')
        kunda = set()
        for ne2, _d, fs in os.walk(out):
            for f2 in fs:
                if f2.endswith('.class'):
                    michi = os.path.relpath(os.path.join(ne2, f2), out)
                    kunda.add(michi.replace(os.sep, '/'))
        nokori = sorted(kunda - set(naka))
        check('組んだ class が1本残らず jar に入っている（入れ子も含む）',
              not nokori, '入っていない: %s' % nokori)
        check('入れ子の class（$ つき）も入っている',
              any('$' in n for n in naka if n.endswith('.class')),
              '$ つきが1本も無い＝詰め残しの疑い')

        # ── 6) 特殊アイテムの絵と、紙の対応表 ──
        #
        # ★ 中身は【ただの紙】のまま。CustomModelData で絵だけ差し替える。
        #   番号が食い違うと **別の絵が出る**のに、サーバーは正しく動くので
        #   実機で見るまで気づけない。番号の正本は plugin の Shouri.java。
        import re as _re
        shouri = os.path.join(os.path.dirname(os.path.dirname(NE)),
                              '時代クラフト', 'plugin', 'src', 'main', 'java',
                              'jidai', 'Shouri.java')
        if not os.path.exists(shouri):
            shouri = os.path.join(os.path.dirname(NE), 'plugin', 'src', 'main',
                                  'java', 'jidai', 'Shouri.java')
        check('plugin の Shouri.java が読める', os.path.exists(shouri), shouri)
        if os.path.exists(shouri):
            ss = io.open(shouri, encoding='utf-8').read()
            m = _re.search(r'TOKUSHU_HYOU = \{(.*?)\n    \};', ss, _re.S)
            kumi = _re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)"\}',
                               m.group(1) if m else '')
            check('特殊アイテムが 5 種 読み取れた', len(kumi) == 5, '実際=%d' % len(kumi))
            with zipfile.ZipFile(JAR) as z:
                kami = z.read('assets/minecraft/models/item/paper.json').decode('utf-8')
            for mei, fai, ban in kumi:
                check('%s の絵が jar にある' % mei,
                      'assets/jidaiui/textures/item/%s.png' % fai in naka, fai)
                check('%s の模型が jar にある' % mei,
                      'assets/jidaiui/models/item/%s.json' % fai in naka, fai)
                check('紙の対応表の番号が %s と一致 (%s)' % (mei, ban),
                      ('"custom_model_data": %s' % ban) in kami
                      and ('jidaiui:item/%s' % fai) in kami,
                      '番号か絵の名前が食い違っている')
            check('ふつうの紙の見た目を壊していない',
                  '"layer0": "minecraft:item/paper"' in kami, 'layer0 が違う')

            # ── Java の表（名前→絵）も Shouri と同じか ──
            #   ★ 食い違うと、こちらの画面だけ紙のままになる
            tp = os.path.join(NE, 'src', 'main', 'java', 'jidai', 'ui', 'Tokushu.java')
            check('MOD に Tokushu.java がある（名前→絵の表）', os.path.exists(tp), tp)
            if os.path.exists(tp):
                ts = io.open(tp, encoding='utf-8').read()
                for mei, fai, ban in kumi:
                    check('Tokushu.java に %s → %s (%s) がある' % (mei, fai, ban),
                          ('{"%s", "%s", "%s"}' % (mei, fai, ban)) in ts,
                          'Shouri.java と食い違っている。tools/tokushu_moderu.py を走らせる')
                check('Tokushu.java は生成物の印がある（手で直していない）',
                      'tools/tokushu_moderu.py が Shouri.java から作る' in ts, '印が無い')

                # ── 7) 遺物 11種も、絵・模型・対応表・Java の表が通しで揃っているか ──
                #
                # ★★ なぜ足したか（2026-08-23 のご指摘）★★
                #   「ガチャにアイコンが出ていない」。原因は絵がまだ無かったこと。
                #   ところが検査は【集める5種だけ】を見ていて、遺物は1件も見ていなかった。
                #   絵が無くてもサーバーは正しく動く（紙のまま出る）ので、
                #   実機で見るまで誰も気づけない。遺物も同じ通しで見る。
                m2 = _re.search(r'IBUTSU_HYOU = \{(.*?)\n    \};', ss, _re.S)
                ib = _re.findall(
                    r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)",\s*"\d",\s*"[A-Z]+",\s*"[^"]*"\}',
                    m2.group(1) if m2 else '')
                check('遺物が 11 種 読み取れた', len(ib) == 11, '実際=%d' % len(ib))
                for mei, fai, ban in ib:
                    check('遺物 %s の絵が jar にある' % mei,
                          'assets/jidaiui/textures/item/%s.png' % fai in naka, fai)
                    check('遺物 %s の模型が jar にある' % mei,
                          'assets/jidaiui/models/item/%s.json' % fai in naka, fai)
                    check('紙の対応表の番号が %s と一致 (%s)' % (mei, ban),
                          ('"custom_model_data": %s' % ban) in kami
                          and ('jidaiui:item/%s' % fai) in kami,
                          '番号か絵の名前が食い違っている＝別の絵が出る')
                    check('Tokushu.java に %s → %s (%s) がある' % (mei, fai, ban),
                          ('{"%s", "%s", "%s"}' % (mei, fai, ban)) in ts,
                          'MOD の画面だけ紙のままになる。tools/tokushu_moderu.py を走らせる')

                # ★ 番号が重なっていないか（重なると別の絵が出る）
                ban_zen = [b for _m, _f, b in kumi] + [b for _m, _f, b in ib]
                check('見た目の番号が 16 種すべて別（集める5＋遺物11）',
                      len(set(ban_zen)) == len(ban_zen) == 16,
                      '重なりか数の違い: %s' % sorted(ban_zen))

    # ── 8) 石油の絵（2026-09-20・ユーザーからいただいたドット絵）────
    #
    # ★★ 石油は特殊アイテムではない ★★
    #   正体は **黒い染料に名前と目印を付けた物**（独自アイテムを登録していない）。
    #   だから紙ではなく **black_dye.json** の方に対応表が要る。
    #
    # ★★ ここが「黙って壊れる」形が4つある ★★
    #   ① 石油を作る所は5つ。1か所でも番号が漏れると、そこの石油だけ染料のまま
    #   ② 番号の正本が3つに散っている（データパック・プラグイン・対応表）
    #   ③ black_dye.json の親を間違えると、ふつうの黒い染料まで樽になる
    #   ④ 絵の背景が不透明だと、灰色の四角が出る
    print('')
    print('-- 8) 石油の絵 --')
    KIKAKU = os.path.dirname(NE)
    DPS = os.path.join(KIKAKU, 'datapacks', 'jidai_craft', 'data', 'jidai',
                       'functions', 'sekiyu')

    # ① 石油を【作る】行を全部 集め、番号が揃っているか
    tsukuru = []
    for fai in ('dashi_give.mcfunction', 'waku.mcfunction'):
        michi = os.path.join(DPS, fai)
        if not os.path.exists(michi):
            continue
        for gyou in io.open(michi, encoding='utf-8').read().splitlines():
            if 'jidai_sekiyu:"oil"' in gyou and ('give ' in gyou or 'summon ' in gyou):
                m = _re.search(r'CustomModelData:(\d+)', gyou)
                tsukuru.append((fai, int(m.group(1)) if m else None))
    check('データパックで石油を作っている所が 5 つ', len(tsukuru) == 5,
          '実際=%d か所' % len(tsukuru))
    nashi = [f for f, b in tsukuru if b is None]
    check('★石油を作る所すべてに見た目の番号がある', not nashi,
          '番号が無い: %s（そこで湧いた石油だけ黒い染料のまま出る）' % sorted(set(nashi)))
    ban_dp = sorted({b for _f, b in tsukuru if b is not None})
    check('★石油の番号がどこも同じ', len(ban_dp) == 1, '食い違い: %s' % ban_dp)

    if len(ban_dp) == 1:
        sban = ban_dp[0]
        # ② プラグインの写しが同じか（正本はデータパック）
        gp = os.path.join(KIKAKU, 'plugin', 'src', 'main', 'java', 'jidai', 'Ginko.java')
        gs = io.open(gp, encoding='utf-8').read() if os.path.exists(gp) else ''
        check('★プラグインの石油の番号がデータパックと同じ (%d)' % sban,
              ('SEKIYU_MITAME = %d;' % sban) in gs,
              '銀行のボタンだけ黒い染料のままになる')

        if os.path.exists(JAR):
            with zipfile.ZipFile(JAR) as z:
                naka2 = z.namelist()
                check('石油の絵が jar にある',
                      'assets/jidaiui/textures/item/sekiyu.png' in naka2, '無い')
                check('石油の模型が jar にある',
                      'assets/jidaiui/models/item/sekiyu.json' in naka2, '無い')
                aru = 'assets/minecraft/models/item/black_dye.json' in naka2
                check('黒い染料の対応表が jar にある', aru, '無い＝絵が出ない')
                if aru:
                    sumi = json.loads(
                        z.read('assets/minecraft/models/item/black_dye.json').decode('utf-8'))
                    # ③ ふつうの黒い染料を壊していないか
                    check('★対応表の地はバニラの黒い染料のまま',
                          sumi.get('textures', {}).get('layer0') == 'minecraft:item/black_dye',
                          'ふつうの黒い染料まで見た目が変わる')
                    ov = sumi.get('overrides', [])
                    check('★対応表の番号がデータパックと同じ (%d)' % sban,
                          any(o.get('predicate', {}).get('custom_model_data') == sban
                              and o.get('model') == 'jidaiui:item/sekiyu' for o in ov),
                          '番号か絵の名前が食い違っている＝絵が出ない')
                    # ★ 特殊アイテム16種と番号が重なっていないか
                    check('★石油の番号が特殊アイテム16種と重なっていない',
                          str(sban) not in kami,
                          '重なると別の絵が出る')
                if 'assets/jidaiui/textures/item/sekiyu.png' in naka2:
                    png = z.read('assets/jidaiui/textures/item/sekiyu.png')
                    w, h = _struct.unpack('>II', png[16:24])
                    iro = png[25]          # IHDR の色の種類。6 = RGBA
                    check('石油の絵が 64×64', (w, h) == (64, 64), '実際=%dx%d' % (w, h))
                    # ④ 背景が【本当に】透明か
                    #
                    # ★★ ここは一度 穴が開いていた ★★
                    #   最初は「アルファの層があるか」だけ見ていた。
                    #   背景を消し忘れた絵（全部 不透明）でも層はあるので、
                    #   **灰色の四角が出る絵を緑で通した**（わざと壊して発覚）。
                    #   層の有無ではなく、**透明な画素が実際にあるか**を数える。
                    check('★石油の絵にアルファの層がある', iro == 6,
                          '色の種類=%d（6=RGBA でないと透明にできない）' % iro)
                    if iro == 6:
                        from PIL import Image as _Im
                        import io as _io
                        _a = _Im.open(_io.BytesIO(png)).convert('RGBA')
                        _p = list(_a.getdata())
                        _suke = sum(1 for _x in _p if _x[3] == 0) / float(len(_p))
                        # ★ 樽は縦長。四角に収めると 3割ほどは必ず余る。
                        #   2割を下回ったら「背景を消し忘れた」と見てよい。
                        check('★石油の絵の背景が透明（透明な画素 %.0f%%）' % (_suke * 100),
                              _suke >= 0.20,
                              '背景を消し忘れている＝灰色の四角が出る')

    print('')
    print('=' * 60)
    print('結果: PASS %d / FAIL %d' % (pass_, fail_))
    print('')
    print('★ ここが全部通っても「ゲームで見た」ことにはならない。')
    print('  実際に J を押して画面が出るかは、人の目で見るしかない。')
    return 1 if fail_ else 0


sys.exit(main())
