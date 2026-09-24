# -*- coding: utf-8 -*-
"""
dotto.py -- 特殊アイテムの絵（v8・ドット絵）を Python だけで描く土台。

  ★★ 作法は v6（Codex 納品・受け入れ済み）に合わせる ★★
    ・不透明度は 0 か 255 だけ、透明画素の RGB は 0
    ・完成シルエットを外から 2px 剥がした層 = outline(#102329)、続く 2px = brass(#C4934B)
      （穴の縁も同じ。8近傍で数える。clientmod/tests/ibutsu_kakunin.py と同じ定義）
      ★ 256×256 で出す時は 8px ＋ 8px（比例）
    ・色は v6 の共有パレット 52 色（clientmod/nouhin/v6/DESIGN_SPEC.md を読む。手で写さない）
      ＋ 理由を書いて足した色（TSUIKA）
    ・光源は左上。highlight(#F5DE9B) は 1 枚に 1 か所のまとまりだけ
    ・素材面の内部に outline 色を使わない

  ★★ 座標は常に 64 単位 ★★
    Kyanbasu(bai=1) → 64×64、Kyanbasu(bai=4) → 256×256。同じ描画コードで両方出せる。
    図形（円・多角形）は出力解像度で滑らかに切られ、線の太さ・点は bai 倍の塊になる。
    画素 i は [i, i+1) を占める。円の中心 32.0 は 64 幅のちょうど真ん中。

  使い方（絵 1 枚 = 関数 1 つ）:
      k = Kyanbasu()
      k.daen(32, 37, 24, 24, 'brass_dark')      # 円盤（シルエットごと描く）
      ...
      im = k.shiage()                            # 外周を剥がして縁を付け、PIL.Image で返す
"""
import io
import os
import re

import numpy as np
from PIL import Image, ImageDraw, ImageFont

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)
V6_SPEC = os.path.join(NE, 'nouhin', 'v6', 'DESIGN_SPEC.md')

N = 64


def paretto():
    """v6 の共有パレット {役割: (r,g,b,a)}。"""
    s = io.open(V6_SPEC, encoding='utf-8').read()
    m = re.search(r'## 共有パレット(.*?)\n## ', s, re.S)
    if not m:
        raise SystemExit('[中止] v6 DESIGN_SPEC に共有パレットが無い')
    p = {}
    for yaku, hx in re.findall(r'\| `([a-z_]+)` \| `#([0-9A-F]{8})` \|', m.group(1)):
        p[yaku] = tuple(int(hx[i:i + 2], 16) for i in (0, 2, 4, 6))
    if len(p) < 50:
        raise SystemExit('[中止] パレットが %d 色しか読めない' % len(p))
    return p


PAL = paretto()
TOUMEI = (0, 0, 0, 0)

# 追加した色（v6 に無い役割）。理由を書いてから足す。tenken はこれも許す。
TSUIKA = {
    'hada_light': (0xF2, 0xDC, 0xBA, 255),   # 肌色の羊皮紙（明）。ご指示「地図は肌色」（2026-09-13 海図）
    'hada': (0xE4, 0xC3, 0x9A, 255),         # 肌色の羊皮紙（基本）
    'hada_dark': (0xC6, 0x9E, 0x70, 255),    # 肌色の羊皮紙（影）
}
for _k, _v in TSUIKA.items():
    PAL.setdefault(_k, _v)


def iro(x):
    """役割名 → RGBA。タプルならそのまま。"""
    if isinstance(x, str):
        if x not in PAL:
            raise KeyError('パレットに無い色: ' + x)
        return PAL[x]
    return tuple(x)


class Kyanbasu:
    """64 単位の座標で描き、bai 倍の大きさで出す。"""

    def __init__(self, n=N, bai=1):
        self.bai = bai
        self.n = n * bai
        self.a = np.zeros((self.n, self.n, 4), dtype=np.uint8)

    # ---- 画素の読み書き（64 単位の 1 点 = bai×bai の塊）----
    def ten(self, x, y, c):
        b = self.bai
        self._shikaku_px(x * b, y * b, (x + 1) * b - 1, (y + 1) * b - 1, c)

    def yomu(self, x, y):
        return tuple(int(v) for v in self.a[y * self.bai, x * self.bai])

    def masuku(self):
        """不透明な画素の bool 配列。"""
        return self.a[:, :, 3] > 0

    # ---- 図形 ----
    def _grid(self):
        ys, xs = np.mgrid[0:self.n, 0:self.n]
        return xs + 0.5, ys + 0.5

    def nuru(self, m, c):
        """bool 配列 m の所を色 c で塗る。"""
        self.a[m] = iro(c)

    def _shikaku_px(self, x0, y0, x1, y1, c):
        x0, x1 = sorted((int(x0), int(x1)))
        y0, y1 = sorted((int(y0), int(y1)))
        self.a[max(y0, 0):min(y1, self.n - 1) + 1, max(x0, 0):min(x1, self.n - 1) + 1] = iro(c)

    def shikaku(self, x0, y0, x1, y1, c):
        """長方形（両端を含む・64 単位）。"""
        b = self.bai
        x0, x1 = sorted((x0, x1))
        y0, y1 = sorted((y0, y1))
        self._shikaku_px(x0 * b, y0 * b, (x1 + 1) * b - 1, (y1 + 1) * b - 1, c)

    def daen_m(self, cx, cy, rx, ry):
        b = self.bai
        px, py = self._grid()
        return ((px - cx * b) / (rx * b)) ** 2 + ((py - cy * b) / (ry * b)) ** 2 <= 1.0

    def daen(self, cx, cy, rx, ry, c):
        self.nuru(self.daen_m(cx, cy, rx, ry), c)

    def wa(self, cx, cy, r_soto, r_uchi, c):
        """輪（外径 r_soto・内径 r_uchi）。"""
        self.nuru(self.daen_m(cx, cy, r_soto, r_soto) & ~self.daen_m(cx, cy, r_uchi, r_uchi), c)

    def marukaku_m(self, x0, y0, x1, y1, r):
        """角丸長方形。x0..x1, y0..y1 は画素の端（右下は含まない端）。"""
        b = self.bai
        x0, y0, x1, y1, r = x0 * b, y0 * b, x1 * b, y1 * b, r * b
        px, py = self._grid()
        qx = np.maximum(np.maximum(x0 + r - px, px - (x1 - r)), 0)
        qy = np.maximum(np.maximum(y0 + r - py, py - (y1 - r)), 0)
        return (qx ** 2 + qy ** 2) <= r * r

    def marukaku(self, x0, y0, x1, y1, r, c):
        self.nuru(self.marukaku_m(x0, y0, x1, y1, r), c)

    def takaku_m(self, ten):
        """多角形（画素の中心が内側なら塗る）。ten = [(x,y), ...] 画素の端の座標（64 単位）。"""
        b = self.bai
        px, py = self._grid()
        m = np.zeros((self.n, self.n), dtype=bool)
        k = len(ten)
        for i in range(k):
            x0, y0 = ten[i][0] * b, ten[i][1] * b
            x1, y1 = ten[(i + 1) % k][0] * b, ten[(i + 1) % k][1] * b
            if y0 == y1:
                continue
            cond = ((py >= min(y0, y1)) & (py < max(y0, y1)))
            xi = x0 + (py - y0) * (x1 - x0) / (y1 - y0)
            m ^= cond & (px < xi)
        return m

    def takaku(self, ten, c):
        self.nuru(self.takaku_m(ten), c)

    def _sen_px(self, x0, y0, x1, y1, c, futosa):
        """線（Bresenham・画素単位）。futosa は正方形ブラシの一辺。"""
        dx, dy = abs(x1 - x0), abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx - dy
        x, y = x0, y0
        h = futosa // 2
        while True:
            self._shikaku_px(x - h, y - h, x - h + futosa - 1, y - h + futosa - 1, c)
            if x == x1 and y == y1:
                break
            e2 = 2 * err
            if e2 > -dy:
                err -= dy
                x += sx
            if e2 < dx:
                err += dx
                y += sy

    def sen(self, x0, y0, x1, y1, c, futosa=1):
        """線（64 単位）。bai 倍では端点を塊の中心に置き、太さも bai 倍。"""
        b = self.bai
        o = b // 2
        self._sen_px(int(x0 * b) + o, int(y0 * b) + o, int(x1 * b) + o, int(y1 * b) + o, c, futosa * b)

    def orisen(self, ten, c, futosa=1):
        """折れ線。"""
        for (x0, y0), (x1, y1) in zip(ten, ten[1:]):
            self.sen(x0, y0, x1, y1, c, futosa)

    def uzumaki(self, cx, cy, r0, r1, mawari, c, futosa=2, kaishi=0.0):
        """渦巻き。半径 r0 → r1 を mawari 周で。"""
        import math
        ten = []
        k = int(mawari * 48)
        for i in range(k + 1):
            t = i / k
            th = kaishi + t * mawari * 2 * math.pi
            r = r0 + (r1 - r0) * t
            ten.append((int(cx + r * math.cos(th)), int(cy + r * math.sin(th))))
        self.orisen(ten, c, futosa)

    # ---- 陰影の道具 ----
    def kage_daen(self, cx, cy, r, hikari, moto, kage, zure=None):
        """円盤を「左上が明るく右下が暗い」3 段で塗る（三日月方式）。"""
        z = zure if zure is not None else max(2, r // 6)
        self.daen(cx, cy, r, r, kage)
        self.daen(cx - z, cy - z, r - z, r - z, moto)
        self.daen(cx - 2 * z, cy - 2 * z, r - 3 * z, r - 3 * z, hikari)

    # ---- 仕上げ ----
    def sou(self):
        """外（透明）から数えた層番号。透明=0、透明に8近傍で接する不透明=1、以降 2,3,...。"""
        return sou_kazoeru(self.masuku())

    def shiage(self, fuchi=True):
        """外周 2×bai px → outline、続く 2×bai px → brass。透明の RGB を 0 に。PIL.Image を返す。"""
        if fuchi:
            s = self.sou()
            w = 2 * self.bai
            self.a[(s >= 1) & (s <= w)] = iro('outline')
            self.a[(s > w) & (s <= 2 * w)] = iro('brass')
        m = self.masuku()
        self.a[~m] = TOUMEI
        self.a[m, 3] = 255
        return Image.fromarray(self.a.copy(), 'RGBA')


def sou_kazoeru(m):
    """bool 配列の不透明部分に、外から数えた層番号を付ける（8近傍）。"""
    h, w = m.shape
    sou = np.zeros(m.shape, dtype=np.int32)
    ima = m.copy()
    k = 0
    while ima.any():
        k += 1
        pad = np.pad(ima, 1, constant_values=False)
        soto = np.zeros_like(ima)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                soto |= ~pad[1 + dy:1 + dy + h, 1 + dx:1 + dx + w]
        fuchi = ima & soto
        sou[fuchi] = k
        ima &= ~fuchi
    return sou


def hosoi_wariai(a, k=3):
    """「k px 未満の細い要素」の割合。色ごとに k×k で開き（収縮→膨張）、消える画素を不透明画素で割る。
    実測（256）: きれいな絵 0〜8% / 1px の市松・2px の縞 22〜60%。Codex依頼文_第7回 §4 の検査（10% 以下）。"""
    a = np.asarray(a)
    op = a[:, :, 3] > 0
    if not op.any():
        return 0.0
    key = a[:, :, 0].astype(np.int64) * 65536 + a[:, :, 1].astype(np.int64) * 256 + a[:, :, 2]
    r = k // 2

    def zurashi(m, f):
        out = None
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                s = np.roll(np.roll(m, dy, 0), dx, 1)
                out = s if out is None else f(out, s)
        return out

    kie = np.zeros(op.shape, dtype=bool)
    for c in np.unique(key[op]):
        m = (key == c) & op
        hiraku = zurashi(zurashi(m, np.logical_and), np.logical_or)
        kie |= m & ~hiraku
    return float(kie[op].mean())


# ---- 点検 ----
def tenken(im, mei=''):
    """v6 の受け入れ規約で 1 枚を点検し、[(合否, 文)] を返す。64 か 256（縁と余白は比例）。"""
    a = np.array(im.convert('RGBA'))
    k = []
    n = a.shape[0]
    bai = max(1, n // N)
    k.append((a.shape[:2] in ((N, N), (N * 4, N * 4)), '%s: %d×%d（64 か 256）' % (mei, a.shape[1], a.shape[0])))
    al = a[:, :, 3]
    k.append((set(np.unique(al).tolist()) <= {0, 255}, '%s: 不透明度が 0/255 だけ' % mei))
    k.append((not (a[al == 0][:, :3] != 0).any(), '%s: 透明画素の RGB が 0' % mei))
    iro_shu = set(map(tuple, a.reshape(-1, 4).tolist()))
    k.append((len(iro_shu) <= 64, '%s: 色数 %d (64以下)' % (mei, len(iro_shu))))
    pal = set(PAL.values()) | {TOUMEI}
    hazure = sorted(c for c in iro_shu if c not in pal)
    k.append((not hazure, '%s: 全色が v6 パレット＋追加色 %s' % (mei, hazure[:4] if hazure else '')))
    s = sou_kazoeru(al > 0)
    out = iro('outline')
    br = iro('brass')
    w = 2 * bai

    def zenbu(m, c):
        return m.any() and not (a[m] != np.array(c, dtype=np.uint8)).any()
    k.append((zenbu((s >= 1) & (s <= w), out), '%s: 外周 %dpx が outline' % (mei, w)))
    k.append((zenbu((s > w) & (s <= 2 * w), br), '%s: 続く %dpx が brass' % (mei, w)))
    naibu = s > 2 * w
    k.append((not ((a[naibu] == np.array(out, dtype=np.uint8)).all(axis=1)).any() if naibu.any() else True,
              '%s: 内部に outline 色が無い' % mei))
    ys, xs = np.where(al > 0)
    yohaku = (xs.min(), ys.min(), n - 1 - xs.max(), n - 1 - ys.max()) if len(xs) else (0, 0, 0, 0)
    k.append((min(yohaku) >= 2 * bai and max(yohaku) <= 8 * bai,
              '%s: 透明余白 %s（%d〜%d）' % (mei, tuple(int(v) for v in yohaku), 2 * bai, 8 * bai)))
    hl = (a == np.array(iro('highlight'), dtype=np.uint8)).all(axis=2)
    k.append((renketsu(hl) == 1, '%s: highlight のまとまりが 1（%d）' % (mei, renketsu(hl))))
    k.append((renketsu(al > 0) == 1, '%s: シルエットが 1 つ（%d）' % (mei, renketsu(al > 0))))
    return k


def renketsu(m):
    """8近傍の連結成分の数。"""
    m = m.copy()
    n = 0
    ys, xs = np.where(m)
    while len(ys):
        n += 1
        stack = [(ys[0], xs[0])]
        m[ys[0], xs[0]] = False
        while stack:
            y, x = stack.pop()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    yy, xx = y + dy, x + dx
                    if 0 <= yy < m.shape[0] and 0 <= xx < m.shape[1] and m[yy, xx]:
                        m[yy, xx] = False
                        stack.append((yy, xx))
        ys, xs = np.where(m)
    return n


# ---- 見比べ用の一覧 ----
def _font(px):
    for p in ('C:/Windows/Fonts/meiryo.ttc', 'C:/Windows/Fonts/msgothic.ttc', 'C:/Windows/Fonts/YuGothM.ttc'):
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, px)
            except Exception:
                pass
    return ImageFont.load_default()


def hikaku(kumi, saki, midashi=''):
    """kumi = [(名札, PIL.Image), ...]。各案を 256 表示・持ち物欄風(48 平均)・48 nearest・GUI2(32)・GUI1(16) で並べる。"""
    haikei = (58, 52, 44, 255)          # 古地図の画面に近い暗い茶
    haikei2 = (198, 176, 132, 255)      # 明るい羊皮紙
    f = _font(14)
    f2 = _font(11)
    W = 16 + len(kumi) * (256 + 24)
    H = 16 + 22 + 256 + 8 + 110 + 20 + 8 + 20
    im = Image.new('RGBA', (W, H), haikei)
    d = ImageDraw.Draw(im)
    if midashi:
        d.text((16, 4), midashi, font=f, fill=(230, 220, 200, 255))
    for i, (fuda, e) in enumerate(kumi):
        x = 16 + i * (256 + 24)
        y = 16 + 22
        d.text((x, y - 18), fuda, font=f, fill=(230, 220, 200, 255))
        big = e.resize((256, 256), Image.NEAREST)
        im.paste(big, (x, y), big)
        yy = y + 256 + 8
        heikin = e.resize((48, 48), Image.BOX)
        chiisai = [('48 平均', heikin), ('48 nearest', e.resize((48, 48), Image.NEAREST)),
                   ('32', e.resize((32, 32), Image.NEAREST)), ('16', e.resize((16, 16), Image.NEAREST))]
        xx = x
        for fuda2, s in chiisai:
            for j, bg in enumerate((haikei, haikei2)):
                sl = Image.new('RGBA', (52, 52), bg)
                sz = s.size[0]
                sl.paste(s, ((52 - sz) // 2, (52 - sz) // 2), s)
                im.paste(sl, (xx, yy + j * 54))
            d.text((xx, yy + 108), fuda2, font=f2, fill=(230, 220, 200, 255))
            xx += 60
    os.makedirs(os.path.dirname(saki), exist_ok=True)
    im.save(saki)
    return saki
