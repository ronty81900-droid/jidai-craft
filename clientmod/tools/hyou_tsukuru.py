# -*- coding: utf-8 -*-
"""
hyou_tsukuru.py -- ui/manifest.json から、Java の表 Hyou.java を作る。

  ★★ なぜ生成するか ★★
    画面に描く文字は135件あり、1件ずつ x/y/幅/高さ/大きさ/色/揃え を持つ。
    これを手で Java へ写すと、必ずどこかで1桁間違える。
    しかもコンパイルは通るので、実機で見るまで気付けない。
    正本は ui/manifest.json だけにして、Java は【毎回ここから作り直す】。

  ★ Hyou.java は手で直さない。直すなら manifest か、この道具を直す。

  使い方:  python tools/hyou_tsukuru.py
"""
import io
import json
import os
import re

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
MANIFEST = os.path.join(NE, 'ui', 'manifest.json')
DE = os.path.join(NE, 'src', 'main', 'java', 'jidai', 'ui', 'Hyou.java')

# ★ 商品とアイテムの対応は、プラグインの Shop.java が正本。
#   ここで並べ直すと、片方だけ変えた時に「絵と中身が違う店」になる。
SHOP = os.path.join(os.path.dirname(NE), 'plugin', 'src', 'main', 'java', 'jidai', 'Shop.java')

YOSE = {'left': 'L', 'center': 'C', 'right': 'R'}

# UI の画面 → Shop.java のどの棚のどのページか
TANA = {
    'shop_life':     ('IPPAN', 1),
    'shop_armor':    ('IPPAN', 2),
    'shop_weapon':   ('IPPAN', 3),
    'gunshop_guns':  ('JUKIHIN', 1),
    'gunshop_ammo':  ('JUKIHIN', 2),
}


def java_moji(s):
    """Java の文字列リテラルにする。日本語はそのまま（ファイルは UTF-8）。"""
    return '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'


def moto(p):
    """assets/jidaiui/textures/gui/x.png → textures/gui/x.png（MOD が読む形）"""
    return p.split('assets/jidaiui/', 1)[-1] if p else None


def java_arui(s):
    """null かもしれない文字列を Java にする。"""
    return 'null' if s is None else java_moji(s)


def shohin_yomu():
    """
    Shop.java の IPPAN / JUKIHIN を読み、(棚, ページ) ごとに
    枠の小さい順で [(アイテムid, 個数, 名前), ...] を返す。

    ★ 引用符の中に括弧やカンマが入る行がある
      （例: "tacz:modern_kinetic_gun{GunId:\\"hamster:sks\\"}"）ので、
      正規表現1本では切れない。括弧の深さと引用符を見ながら1件ずつ切り出す。
    """
    s = io.open(SHOP, encoding='utf-8').read()
    hyou = {}
    for tana in ('IPPAN', 'JUKIHIN'):
        i = s.index('Shohin[] ' + tana + ' = {')
        owari = s.index('\n    };', i)
        naka = s[i:owari]
        p = 0
        while True:
            p = naka.find('new Shohin(', p)
            if p < 0:
                break
            p += len('new Shohin(')
            fukasa = 1
            kagi = False
            nige = False
            hajime = p
            while fukasa > 0:
                c = naka[p]
                if nige:
                    nige = False
                elif c == '\\':
                    nige = True
                elif c == '"':
                    kagi = not kagi
                elif not kagi and c == '(':
                    fukasa += 1
                elif not kagi and c == ')':
                    fukasa -= 1
                p += 1
            hikisuu = kugiru(naka[hajime:p - 1])
            waku = int(hikisuu[0])
            material = hikisuu[1].split('.')[-1].strip()
            kazu = int(hikisuu[2])
            # ★ MOD のアイテム指定（銃と弾）。Java の文字列なので、
            #   囲みの " を外し、中の \" を " に戻す。
            mod = hikisuu[8].strip()
            mod = None if mod == 'null' else mod.strip('"').replace('\\"', '"')
            page = int(hikisuu[-1])
            namae = hikisuu[3].strip().strip('"')
            hyou.setdefault((tana, page), []).append(
                (waku, 'minecraft:' + material.lower(), kazu, mod, namae))
    return {k: [(x[0], x[1], x[2], x[3], x[4]) for x in sorted(v)] for k, v in hyou.items()}


def kugiru(s):
    """いちばん外側のカンマだけで切る（引用符と括弧の中は無視する）。"""
    de = []
    ima = ''
    fukasa = 0
    kagi = False
    nige = False
    for c in s:
        if nige:
            ima += c
            nige = False
            continue
        if c == '\\':
            ima += c
            nige = True
            continue
        if c == '"':
            kagi = not kagi
        elif not kagi and c in '({':
            fukasa += 1
        elif not kagi and c in ')}':
            fukasa -= 1
        if c == ',' and not kagi and fukasa == 0:
            de.append(ima.strip())
            ima = ''
        else:
            ima += c
    de.append(ima.strip())
    return de


def main():
    m = json.loads(io.open(MANIFEST, encoding='utf-8').read())
    shohin = shohin_yomu()
    aitem_ari = [0]
    tabu = []
    tabu_kazu = 0

    gyou = []
    kazu = 0
    kado = []
    kado_kazu = 0
    gamen = []
    for pid, p in m['pages'].items():
        gyou.append('')
        gyou.append('        // ---- %s ----' % pid)
        for t in p['runtime_text']:
            x, y, w, h = t['rect']
            gyou.append('        new Moji(%s, %s, %s, %d, %d, %d, %d, %d, %d, 0x%s, \'%s\', %s),'
                        % (java_moji(pid), java_moji(t['id']), java_moji(t['content']),
                           x, y, w, h, t['font_px'], t['max_lines'],
                           t['color'].lstrip('#'), YOSE[t['align']],
                           'true' if t.get('dynamic') else 'false'))
            kazu += 1

        if not gamen:
            gamen.append('')
        gamen.append('        new Gamen(%s, %s, %s),'
                     % (java_moji(pid), java_moji(moto(p['base_asset'])),
                        java_moji(p.get('title', pid))))

        # ── その画面に並ぶ商品（プラグインの Shop.java から）──
        shina = shohin.get(TANA.get(pid), []) if pid in TANA else []
        if pid in TANA and len(shina) != len(p['cards']):
            raise SystemExit(
                '[中止] %s: UI のカードは %d 枚なのに、Shop.java の %s ページ%d は %d 品です。\n'
                '  どちらかを直してから作り直してください（絵と中身がずれた店になります）。'
                % (pid, len(p['cards']), TANA[pid][0], TANA[pid][1], len(shina)))

        for c in p['components']:
            if not c['id'].startswith('tab_'):
                continue
            if not tabu:
                tabu.append('')
            r = c['rect']
            tabu.append('        new Tabu(%s, %d, %d, %d, %d, %d),'
                        % (java_moji(pid), int(c['id'].split('_')[1]) - 1,
                           r[0], r[1], r[2], r[3]))
            tabu_kazu += 1

        kado.append('')
        kado.append('        // ---- %s ----' % pid)
        for ban, c in enumerate(p['cards']):
            st = c.get('state_assets') or {}
            x, y, w, h = c['rect']
            a = c.get('game_icon_rect') or [0, 0, 0, 0]
            # 商品のある画面だけ、アイテムの絵を置く。
            # 銀行・時代を進める・宣戦は「どのアイテムを置くか」がまだ決まっていない。
            waku, aitem, aitem_kazu, mod, namae = shina[ban] if shina else (-1, None, 0, None, None)
            if aitem:
                aitem_ari[0] += 1
            # ★★ カードの名前と Shop.java の名前を突き合わせる（2026-08-22）★★
            #   数だけ合っていて順番がずれると「パンを押したら石炭が来る」店になる。
            #   コンパイルも通り、実機で押すまで分からない。名前で必ず止める。
            if namae is not None:
                ui_mei = re.sub(r'\s*×\s*\d+\s*$', '', c.get('name', '')).strip()
                if ui_mei != namae:
                    raise SystemExit(
                        '[中止] %s の %s: UI の名前「%s」と Shop.java の名前「%s」が違います。\n'
                        '  順番がずれています。mise_kumikae.py の SHINAMONO と Shop.java を見比べてください。'
                        % (pid, c['id'], c.get('name', ''), namae))
            kado.append('        new Kado(%s, %s, %d, %d, %d, %d, %s, %s, %s, %s, '
                        '%d, %d, %d, %d, %s, %d, %d, %s),'
                        % (java_moji(pid), java_moji(c['id']), x, y, w, h,
                           java_arui(moto(st.get('hover'))),
                           java_arui(moto(st.get('pressed'))),
                           java_arui(moto(st.get('locked'))),
                           java_arui(moto(st.get('insufficient'))),
                           a[0], a[1], a[2], a[3],
                           java_arui(aitem), aitem_kazu, waku, java_arui(mod)))
            kado_kazu += 1

    honbun = '''package jidai.ui;

/**
 * 画面に描く文字の表。
 *
 * ★★ このファイルは手で直さない ★★
 *   `ui/manifest.json` から `python tools/hyou_tsukuru.py` で作り直す。
 *   文字は %d 件あり、1件ずつ座標・大きさ・色・揃えを持つ。
 *   手で写すと必ずどこかで1桁間違えるが、コンパイルは通ってしまうので、
 *   実機で開くまで気付けない。だから正本を manifest.json ひとつにしている。
 *
 *   直したい時は manifest.json を直して、この道具を走らせ直すこと。
 */
public final class Hyou {

    private Hyou() {
        // 表だけの置き場なので、実体は作らない
    }

    /** 画面に描く文字ひとつぶん。 */
    public static final class Moji {

        /** どの画面か（gunshop_guns など） */
        public final String gamen;
        /** 画面の中での名前（item_1_name など） */
        public final String id;
        /** 出す文字。{...} の部分は、その場で変わる値に差し替える */
        public final String moji;
        /** 文字を置く枠。320x240 の中の座標 */
        public final int x;
        public final int y;
        public final int w;
        public final int h;
        /** 文字の大きさ。8 が素の大きさ / 12 は1.5倍 / 16 は2倍 */
        public final int px;
        /** 折り返してよい行数の上限 */
        public final int gyouSuu;
        /** 色。0xRRGGBB */
        public final int iro;
        /** 寄せ方。L=左 C=中央 R=右 */
        public final char yose;
        /** その場で変わる値を含むか */
        public final boolean ugoku;

        Moji(String gamen, String id, String moji, int x, int y, int w, int h,
             int px, int gyouSuu, int iro, char yose, boolean ugoku) {
            this.gamen = gamen;
            this.id = id;
            this.moji = moji;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.px = px;
            this.gyouSuu = gyouSuu;
            this.iro = iro;
            this.yose = yose;
            this.ugoku = ugoku;
        }

        /** 文字の大きさから、拡大率を出す。8px が 1.0 倍。 */
        public float bairitsu() {
            return px / 8.0F;
        }
    }

    /** 全部の文字。%d 件。 */
    public static final Moji[] ZENBU = {%s
    };

    /**
     * {value} のような差し込みを、仮の値へ置き換える。
     *
     * ★ まだサーバーと繋いでいないので、【いちばん長くなる形】を入れる。
     *   短い値で測ると「本番で桁が増えた時だけはみ出す」を見逃す。
     *   繋いだら、ここを本物の値に差し替える。
     */
    public static String atehameru(String s) {
        if (s.indexOf('{') < 0) {
            return s;
        }
        return s.replaceAll("[{]faction_name[^}]*[}]", "内海")
                .replaceAll("[{][^}]*[}]", "999,999");
    }

    /** 画面ひとつぶん。 */
    public static final class Gamen {

        /** 画面の名前（gunshop_guns など） */
        public final String id;
        /** 下地の絵。これ1枚に、ふつうの状態のカードまで描かれている */
        public final String shita;
        /** 人が読む見出し */
        public final String midashi;

        Gamen(String id, String shita, String midashi) {
            this.id = id;
            this.shita = shita;
            this.midashi = midashi;
        }
    }

    /** 画面は8つ。 */
    public static final Gamen[] GAMEN = {%s
    };

    /**
     * カードひとつぶん。
     *
     * ★ ふつうの状態は【下地の絵にもう描かれている】ので、素材を持たない。
     *   カーソルが乗った時などに、その状態の絵をこの位置へ上から貼る。
     */
    public static final class Kado {

        public final String gamen;
        public final String id;
        public final int x;
        public final int y;
        public final int w;
        public final int h;
        /** カーソルが乗っている時の絵。無ければ null（押せないカード） */
        public final String kasoru;
        /** 押している時 */
        public final String oshita;
        /** まだ買えない時 */
        public final String mikaikin;
        /** お金が足りない時 */
        public final String fusoku;
        /** ゲームがアイテムの絵を置く場所 */
        public final int ax;
        public final int ay;
        public final int aw;
        public final int ah;
        /**
         * そこに置くアイテム（minecraft:bread など）。無ければ null。
         * ★ プラグインの Shop.java が正本。ここで並べ直すと
         *   「絵と中身が違う店」になるので、必ず生成し直すこと。
         */
        public final String aitem;
        /** そのアイテムを何個ぶんとして見せるか */
        public final int aitemKazu;
        /**
         * サーバーが開いているチェスト画面の【何番目の枠】か。
         *
         * ★★ ここが通信の要 ★★
         *   この MOD はサーバーと独自のやり取りをしない。
         *   プラグインが今までどおりチェスト画面を開き、その上に絵を被せる。
         *   カードを押したら、この枠を左クリックしたことにして送る。
         *   買う処理はプラグイン側のまま（655項目で確かめてあるもの）が動く。
         *
         *   商品の無いカード（銀行など）は -1。
         */
        public final int waku;
        /**
         * MOD のアイテム指定。銃と弾だけ。無ければ null。
         *
         * ★★ これがあると、代用品ではなく【本物の銃の見た目】が出る ★★
         *   サーバー（Bukkit）は MOD のアイテムを作れないので、
         *   チェスト画面にはクロスボウなどの代用品しか置けない。
         *   クライアント側には TaCZ が入っているので、
         *   こちらで組み立てれば本物の絵が描ける。
         *   正本は plugin の Shop.java の modItem 欄。
         */
        public final String modAitem;

        Kado(String gamen, String id, int x, int y, int w, int h,
             String kasoru, String oshita, String mikaikin, String fusoku,
             int ax, int ay, int aw, int ah, String aitem, int aitemKazu, int waku,
             String modAitem) {
            this.gamen = gamen;
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.kasoru = kasoru;
            this.oshita = oshita;
            this.mikaikin = mikaikin;
            this.fusoku = fusoku;
            this.ax = ax;
            this.ay = ay;
            this.aw = aw;
            this.ah = ah;
            this.aitem = aitem;
            this.aitemKazu = aitemKazu;
            this.waku = waku;
            this.modAitem = modAitem;
        }

        /** 中に点が入っているか（カーソル判定） */
        public boolean naka(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    /** 全部のカード。%d 件。 */
    public static final Kado[] KADO = {%s
    };

    /**
     * タブひとつぶん。
     *
     * ★ タブはチェスト画面の 1段目（枠0・枠1）に置かれている。
     *   押したらその枠を左クリックしたことにして送ると、
     *   プラグインが画面を開き直してページが変わる。
     */
    public static final class Tabu {

        public final String gamen;
        /** 何枚目のタブか（0 から） */
        public final int ban;
        public final int x;
        public final int y;
        public final int w;
        public final int h;

        Tabu(String gamen, int ban, int x, int y, int w, int h) {
            this.gamen = gamen;
            this.ban = ban;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean naka(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        /** チェスト画面での枠番号。タブは 1段目に左から並ぶ。 */
        public int waku() {
            return ban;
        }
    }

    /** 全部のタブ。%d 件。 */
    public static final Tabu[] TABU = {%s
    };
}
''' % (kazu, kazu, '\n'.join(gyou), '\n'.join(gamen), kado_kazu, '\n'.join(kado),
        tabu_kazu, '\n'.join(tabu))

    io.open(DE, 'w', encoding='utf-8', newline='\n').write(honbun)
    print('作りました: %s' % DE)
    print('  文字 %d 件 / カード %d 件 / タブ %d 件 / 画面 %d 枚 / アイテムの絵 %d 件'
          % (kazu, kado_kazu, tabu_kazu, len(m['pages']), aitem_ari[0]))
    nashi = kado_kazu - aitem_ari[0]
    if nashi:
        print('  ★ アイテムの決まっていないカードが %d 件あります'
              '（銀行・時代を進める・宣戦。何を置くかは未定）' % nashi)


main()
