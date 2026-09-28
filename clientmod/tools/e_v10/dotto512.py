# -*- coding: utf-8 -*-
"""
dotto512.py -- 特殊アイテムの絵（v10・512×512）を描く土台（2026-09-28）。

  ご指示「いくつか一からここで作りなおしてもいいですか？512x512で」。
  作り直すのは 海図・蒸気機関の歯車・ベッセマー転炉・ロスチャイルドの金庫 の4枚。
  絵の感じは「いまと同じドット絵のまま細かくする」（案A）。

  ★★ 決まりは dotto.py（v8）と同じ。大きさだけ 512 に広げる ★★
    ・描くのは 128 マス（1マス = 4×4px）。512 には最寄りの点で4倍して出す。
      手に持った時の大きさ（画面で 300px 前後）では、4px より細かい点は見分けられないため。
    ・外周 4マス（16px）= outline、続く 4マス（16px）= brass。穴の縁も同じ（8近傍で数える）。
      v6 の 64×64 の「2px＋2px」と同じ比率なので、持ち物の大きさで ほかの絵と縁の太さがそろう。
    ・色は v6 の共有パレット＋追加色（dotto.TSUIKA）。追加する時は理由を書く。
    ・光は左上。highlight は1枚に1まとまり。素材の内部に outline 色を使わない。
    ・不透明度は 0 か 255 だけ。透明の画素の RGB は 0。

  ★ 縁の規則から来る形の決まり（v8 の実測）
    幅が 16マス より細い部分は、中が outline と brass だけになる（素材の色が出ない）。
    細い物（歯・取っ手・紐）は「真鍮の芯」に見えるので、それを生かすか、太くする。
    穴を透明にすると、穴のまわりにも縁が付く。付けたくない穴は、暗い色の窪みで描く。
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

KOKO = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(KOKO)
sys.path.insert(0, TOOLS)
import dotto  # noqa: E402

N = 128          # 描くマス目
BAI = 4          # 512 = 128 × 4
FUCHI = 4        # outline のマス数（= 16px）
SHINCHU = 4      # brass のマス数（= 16px）
PAL = dotto.PAL
iro = dotto.iro


class K128(dotto.Kyanbasu):
    """128 マスで描く画布。座標は 128 単位（画素 i は [i, i+1)）。"""

    def __init__(self):
        super().__init__(n=N, bai=1)

    # ---- 陰影 ----
    def zurasu(self, m, dx, dy):
        """m を dx, dy だけずらした bool 配列（はみ出しは False）。"""
        out = np.zeros_like(m)
        h, w = m.shape
        ys = slice(max(dy, 0), h + min(dy, 0))
        xs = slice(max(dx, 0), w + min(dx, 0))
        ys2 = slice(max(-dy, 0), h + min(-dy, 0))
        xs2 = slice(max(-dx, 0), w + min(-dx, 0))
        out[ys, xs] = m[ys2, xs2]
        return out

    def men(self, m, hikari, moto, kage, haba=2, kage2=None):
        """面取りの陰影。m の左上の縁 haba マスを hikari、右下の縁を kage で塗る（中は moto）。
        kage2 を渡すと、右下のいちばん外側 1 マスをさらに暗くする。"""
        self.nuru(m, moto)
        for k in range(1, haba + 1):
            self.nuru(m & ~self.zurasu(m, k, k), hikari)
        for k in range(1, haba + 1):
            self.nuru(m & ~self.zurasu(m, -k, -k), kage)
        if kage2 is not None:
            self.nuru(m & ~self.zurasu(m, -1, -1), kage2)

    def kyuu(self, m, cx, cy, rx, ry, dan):
        """丸い物の陰影。dan = 暗→明の色の並び。光は左上から。
        楕円の中心からの向きで明るさを決める（ドット絵なので段は色の数だけ）。"""
        ys, xs = np.mgrid[0:self.n, 0:self.n]
        nx = (xs + 0.5 - cx) / rx
        ny = (ys + 0.5 - cy) / ry
        nz = np.sqrt(np.clip(1 - nx * nx - ny * ny, 0, 1))
        L = np.array([-0.55, -0.6, 0.58])
        L = L / np.linalg.norm(L)
        v = nx * L[0] + ny * L[1] + nz * L[2]
        v = np.clip((v + 0.25) / 1.25, 0, 0.999)
        idx = (v * len(dan)).astype(int)
        for i, c in enumerate(dan):
            self.nuru(m & (idx == i), c)

    def tate_dan(self, m, x0, x1, dan):
        """円柱（縦）の陰影。x0..x1 の幅を dan（暗→明→暗の並びでもよい）で縦に塗り分ける。"""
        xs = np.arange(self.n)
        for i, c in enumerate(dan):
            a = x0 + (x1 - x0) * i / len(dan)
            b = x0 + (x1 - x0) * (i + 1) / len(dan)
            col = (xs + 0.5 >= a) & (xs + 0.5 < b)
            self.nuru(m & col[None, :], c)

    def ana_ume(self, c='black_mid'):
        """閉じた透明の隙間（外とつながっていない穴）を c で埋める。
        ★ 残すと、穴の内側にも縁（outline＋brass 8マス）が付いて、変な輪に見える。"""
        toumei = ~self.masuku()
        soto = np.zeros_like(toumei)
        soto[0, :] = toumei[0, :]
        soto[-1, :] = toumei[-1, :]
        soto[:, 0] = toumei[:, 0]
        soto[:, -1] = toumei[:, -1]
        while True:
            hiro = soto.copy()
            hiro[1:, :] |= soto[:-1, :]
            hiro[:-1, :] |= soto[1:, :]
            hiro[:, 1:] |= soto[:, :-1]
            hiro[:, :-1] |= soto[:, 1:]
            hiro &= toumei
            if (hiro == soto).all():
                break
            soto = hiro
        self.nuru(toumei & ~soto, c)

    # ---- 仕上げ ----
    def shiage128(self):
        """外周 FUCHI マス → outline、続く SHINCHU マス → brass。128×128 の PIL.Image を返す。"""
        s = self.sou()
        self.a[(s >= 1) & (s <= FUCHI)] = iro('outline')
        self.a[(s > FUCHI) & (s <= FUCHI + SHINCHU)] = iro('brass')
        m = self.masuku()
        self.a[~m] = dotto.TOUMEI
        self.a[m, 3] = 255
        return Image.fromarray(self.a.copy(), 'RGBA')


def dasu(im128):
    """128 → 512（最寄りの点で4倍）。"""
    return im128.resize((N * BAI, N * BAI), Image.NEAREST)


def tenken512(im, mei=''):
    """v6 の受け入れ規約を 512 に広げて点検する。[(合否, 文)]。"""
    a = np.array(im.convert('RGBA'))
    n = a.shape[0]
    k = [(a.shape[:2] == (512, 512), '%s: 512×512（%d×%d）' % (mei, a.shape[1], a.shape[0]))]
    al = a[:, :, 3]
    k.append((set(np.unique(al).tolist()) <= {0, 255}, '%s: 不透明度が 0/255 だけ' % mei))
    k.append((not (a[al == 0][:, :3] != 0).any(), '%s: 透明画素の RGB が 0' % mei))
    shu = set(map(tuple, a.reshape(-1, 4).tolist()))
    k.append((len(shu) <= 64, '%s: 色数 %d（64以下）' % (mei, len(shu))))
    pal = set(PAL.values()) | {dotto.TOUMEI}
    hazure = sorted(c for c in shu if c not in pal)
    k.append((not hazure, '%s: 全色がパレット＋追加色 %s' % (mei, hazure[:3] if hazure else '')))
    s = dotto.sou_kazoeru(al > 0)
    w1, w2 = FUCHI * BAI, (FUCHI + SHINCHU) * BAI

    def zenbu(m, c):
        return m.any() and not (a[m] != np.array(iro(c), dtype=np.uint8)).any()
    k.append((zenbu((s >= 1) & (s <= w1), 'outline'), '%s: 外周 %dpx が outline' % (mei, w1)))
    k.append((zenbu((s > w1) & (s <= w2), 'brass'), '%s: 続く %dpx が brass' % (mei, w2 - w1)))
    naibu = s > w2
    k.append((not (a[naibu][:, :3] == np.array(iro('outline')[:3], dtype=np.uint8)).all(axis=1).any()
              if naibu.any() else True, '%s: 内部に outline 色が無い' % mei))
    ys, xs = np.where(al > 0)
    yo = (int(xs.min()), int(ys.min()), n - 1 - int(xs.max()), n - 1 - int(ys.max()))
    k.append((min(yo) >= 16 and max(yo) <= 64, '%s: 透明余白 %s（16〜64px）' % (mei, yo)))
    hl = (a == np.array(iro('highlight'), dtype=np.uint8)).all(axis=2)
    k.append((dotto.renketsu(hl[::BAI, ::BAI]) == 1, '%s: highlight のまとまりが 1（%d）'
              % (mei, dotto.renketsu(hl[::BAI, ::BAI]))))
    k.append((dotto.renketsu((al > 0)[::BAI, ::BAI]) == 1, '%s: シルエットが 1 つ' % mei))
    return k


def ima_no_e(fai):
    """組んだ jar（clientmod/JidaiUI-0.1.0.jar）から、いまの絵を読む（RGBA の PIL.Image）。"""
    import io
    import zipfile
    z = zipfile.ZipFile(os.path.join(os.path.dirname(TOOLS), 'JidaiUI-0.1.0.jar'))
    return Image.open(io.BytesIO(z.read('assets/jidaiui/textures/item/%s.png' % fai))).convert('RGBA')


def ima_katachi(fai, bokashi=1.2):
    """いまの絵の外形を 128 マスに、なめらかに起こした bool 配列（案A で外形を変えないため）。
    ★ 64 の絵を最寄りの点で 2 倍にすると、2 マスの段が残って粗い。ぼかしてから半分で切ると、段が丸まる。
      小さな穴（1〜2 マス）はぼかしで埋まるので、要る穴は描く側で開け直す。"""
    from PIL import ImageFilter
    a = ima_no_e(fai).split()[3]
    a = a.resize((N, N), Image.BILINEAR).filter(ImageFilter.GaussianBlur(bokashi))
    return np.array(a) >= 128


def hashiru(fai, an, midashi):
    """案を描いて nouhin/v10_an/<fai>/ に出し、点検して、いまの絵と並べた見比べ表を作る。
    an = {'A': 関数, 'B': 関数}。いまの絵は実機に入っている jar から取る。"""
    import io
    import zipfile
    ne = os.path.dirname(TOOLS)
    saki = os.path.join(ne, 'nouhin', 'v10_an', fai)
    os.makedirs(saki, exist_ok=True)
    z = zipfile.ZipFile(os.path.join(ne, 'JidaiUI-0.1.0.jar'))
    ima = Image.open(io.BytesIO(z.read('assets/jidaiui/textures/item/%s.png' % fai))).convert('RGBA')
    kumi = [('いま（%d×%d）' % ima.size, ima)]
    for mei, f in an.items():
        e = dasu(f())
        e.save(os.path.join(saki, '%s.png' % mei))
        ng = [s for ok, s in tenken512(e, mei) if not ok]
        print('%s 案%s: %s' % (fai, mei, '点検 すべて合格' if not ng else '★ ' + ' / '.join(ng)))
        kumi.append(('案' + mei, e))
    print(hikaku(kumi, os.path.join(saki, 'hikaku.png'), midashi))


# ---- 見比べ表 ----
SLOT = (139, 139, 139, 255)          # 持ち物のマスの灰色
MOD_HAIKEI = (58, 52, 44, 255)       # MOD の古地図の画面の暗い茶


def _font(px):
    for p in ('C:/Windows/Fonts/BIZ-UDGothicB.ttc', 'C:/Windows/Fonts/meiryo.ttc'):
        if os.path.exists(p):
            from PIL import ImageFont
            return ImageFont.truetype(p, px)
    from PIL import ImageFont
    return ImageFont.load_default()


def hikaku(kumi, saki, midashi):
    """kumi = [(名札, 絵), ...]。上に 512 の実寸、下に持ち物の大きさ（48平均・48最寄り・32・16）を並べる。
    ★ 48平均 = 持ち物のマス（バニラはミップマップで縮める）に近い。
      48最寄り = いまの MOD の画面（最寄りの点で縮める）。"""
    f, f2 = _font(22), _font(15)
    haba = 512 + 40
    W = 30 + len(kumi) * haba
    H = 70 + 512 + 30 + 2 * 60 + 60
    im = Image.new('RGBA', (W, H), (242, 238, 228, 255))
    d = ImageDraw.Draw(im)
    d.text((30, 20), midashi, font=f, fill=(40, 36, 32, 255))
    for i, (fuda, e) in enumerate(kumi):
        x = 30 + i * haba
        y = 70
        d.text((x, y - 28 + 4), fuda, font=f2, fill=(40, 36, 32, 255))
        big = e.resize((512, 512), Image.NEAREST)
        d.rectangle([x, y, x + 511, y + 511], fill=SLOT)
        im.alpha_composite(big, (x, y))
        yy = y + 512 + 20
        chiisai = [('持ち物 48', e.resize((48, 48), Image.BOX)),
                   ('MOD画面 48', e.resize((48, 48), Image.NEAREST)),
                   ('32', e.resize((32, 32), Image.BOX)), ('16', e.resize((16, 16), Image.BOX))]
        xx = x
        for fuda2, s in chiisai:
            for j, bg in enumerate((SLOT, MOD_HAIKEI)):
                sl = Image.new('RGBA', (56, 56), bg)
                sz = s.size[0]
                sl.alpha_composite(s, ((56 - sz) // 2, (56 - sz) // 2))
                im.alpha_composite(sl, (xx, yy + j * 60))
            d.text((xx, yy + 122), fuda2, font=_font(12), fill=(90, 84, 76, 255))
            xx += 110
    os.makedirs(os.path.dirname(saki), exist_ok=True)
    im.convert('RGB').save(saki)
    return saki
