# -*- coding: utf-8 -*-
"""
tokushu_e_tsukuru.py -- 特殊アイテム16種の絵（高解像度・古地図風）を吐く。第2版。

  ★★ 作り（docs/Fable依頼文_特殊アイテム.md §2 の条件①②を守る）★★
    ・形は 円・多角形・帯 などの図形（符号付き距離）で本番の解像度に描く
    ・面は 球・円柱・面 の陰影を段階色で付け、素材ごとの模様を画素単位で入れる
    ・そのままでは条件②（格子の平均が下絵に近い）を破るので、
      形の縁を【格子の境目に寄せる】（格子ごとに 78% 以上／22% 以下 に揃える）
    ・輪郭は 0.65 格子の帯、その内側は素材の暗い色の縁
    ・下絵（16×16）は本番の【格子の中心の画素】を拾った物 ＝ 条件①の定義そのもの
    ・吐いた直後に自分で①②ほかを確かめ、外れていたら止まる（黙って出さない）

  ★★ 正本 ★★
    名前・ファイル名・番号 … plugin/.../Shouri.java（TOKUSHU_HYOU / IBUTSU_HYOU）
    色                     … clientmod/nouhin/v6/DESIGN_SPEC.md の「共有パレット」52色
    足す色は ATARASHII_IRO の1か所だけ（今は無し）

  ★★ 乱数 ★★
    模様の置き場所は座標の hash から決める（状態を持たない）。2回動かしたら同じ byte。

  動かし方:
    python clientmod/tools/tokushu_e_tsukuru.py                 … v7 を吐く
    python clientmod/tools/tokushu_e_tsukuru.py --out <所>      … 吐き先を変える
    python clientmod/tools/tokushu_e_tsukuru.py --mono a,b      … 一部だけ

  依存: Pillow / numpy
"""
import argparse
import hashlib
import io
import json
import math
import os
import re
import sys

import numpy as np
from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)                       # clientmod
KIKAKU = os.path.dirname(NE)                     # 時代クラフト

SHOURI = os.path.join(KIKAKU, 'plugin', 'src', 'main', 'java', 'jidai', 'Shouri.java')
V6_SPEC = os.path.join(NE, 'nouhin', 'v6', 'DESIGN_SPEC.md')
IRAIBUN = os.path.join(KIKAKU, 'docs', 'Fable依頼文_特殊アイテム.md')
OUT_KITEI = os.path.join(NE, 'nouhin', 'v7')

# 足す色（v6 の共有パレットに無い色だけ。名前: ('#RRGGBBAA', '理由')）
# ★ ここ以外で 16進を書かない。
ATARASHII_IRO = {}

JIDAI_MEI = {'1': '鉄器', '2': '中世', '3': '近代', '4': '現代'}

# 条件②の余裕（検査は 40 / 25%。生成は少し内側で止める）
RGB_SA = 38
ALPHA_WARI = 0.22          # 格子の中で「入る」側は 78% 以上、「出る」側は 22% 以下
OBI = 0.5                  # 輪郭の帯の幅（格子の一辺に対する割合）。素材によって obi_haba() が太くする
OBI_HOSOI = 0.25           # 細い部品の輪郭の帯

# 光。左上の手前から
HIKARI = np.array([-0.55, -0.65, 0.53])
HIKARI /= np.linalg.norm(HIKARI)


# ══════════════════════════════════════════════════════════════
#  正本を読む
# ══════════════════════════════════════════════════════════════

def hyou():
    s = io.open(SHOURI, encoding='utf-8').read()
    kekka = []
    m = re.search(r'TOKUSHU_HYOU = \{(.*?)\n    \};', s, re.S)
    if not m:
        raise SystemExit('[中止] Shouri.java に TOKUSHU_HYOU が見つかりません')
    for gyou in m.group(1).splitlines():
        k = re.search(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)"\}\s*,?\s*//\s*(\S+)', gyou)
        if k:
            kekka.append(dict(mei=k.group(1), fai=k.group(2), ban=int(k.group(3)), jidai=k.group(4), ookisa=512))
    if len(kekka) != 5:
        raise SystemExit('[中止] TOKUSHU_HYOU を5件 読めませんでした: %d' % len(kekka))
    m = re.search(r'IBUTSU_HYOU = \{(.*?)\n    \};', s, re.S)
    if not m:
        raise SystemExit('[中止] Shouri.java に IBUTSU_HYOU が見つかりません')
    for a, b, c, d in re.findall(r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)",\s*"(\d)",\s*"[A-Z]+",\s*"[^"]*"\}', m.group(1)):
        kekka.append(dict(mei=a, fai=b, ban=int(c), jidai=JIDAI_MEI[d], ookisa=256))
    if len(kekka) != 16:
        raise SystemExit('[中止] 16件 読めませんでした: %d' % len(kekka))
    return kekka


def hex_rgba(hx):
    hx = hx.lstrip('#')
    return tuple(int(hx[i:i + 2], 16) for i in (0, 2, 4, 6))


def paretto():
    s = io.open(V6_SPEC, encoding='utf-8').read()
    setsu = re.search(r'## 共有パレット(.*?)\r?\n## ', s, re.S)
    if not setsu:
        raise SystemExit('[中止] v6 DESIGN_SPEC に「共有パレット」が見つかりません')
    pal = {}
    for na, hx in re.findall(r'\| `([a-z_]+)` \| `(#[0-9A-F]{8})` \|', setsu.group(1)):
        pal[na] = hex_rgba(hx)
    if len(pal) < 50:
        raise SystemExit('[中止] 共有パレットを読めていません: %d 色' % len(pal))
    for na, (hx, _r) in ATARASHII_IRO.items():
        if na in pal:
            raise SystemExit('[中止] 足す色 %s は v6 に既にあります' % na)
        pal[na] = hex_rgba(hx)
    return pal


PAL = None
ROLE = []          # 番号 → 色の名前
ROLE_NO = {}       # 色の名前 → 番号
RGBA = None        # 番号 → RGBA (N,4)


def paretto_junbi():
    global PAL, ROLE, ROLE_NO, RGBA
    PAL = paretto()
    ROLE = sorted(PAL)
    ROLE_NO = {n: i for i, n in enumerate(ROLE)}
    RGBA = np.array([PAL[n] for n in ROLE], dtype=np.uint8)


# 素材 ＝ 暗い → 明るい の段階色
SOZAI = {
    'stone':   ['white_deep', 'white', 'white_light'],
    'paper':   ['paper_deep', 'paper_dark', 'paper', 'paper_light'],
    'wood':    ['wood_deep', 'wood', 'wood_light'],
    'leather': ['leather_deep', 'leather', 'leather_light'],
    'brass':   ['brass_deep', 'brass_dark', 'brass', 'brass_light'],
    'iron':    ['iron_deep', 'iron', 'iron_light', 'steel_light', 'steel_bright'],
    'steel':   ['iron_light', 'steel_light', 'steel_bright'],
    'black':   ['black', 'black_mid', 'black_light'],
    'soot':    ['soot', 'black', 'black_mid'],
    'glass':   ['glass_deep', 'glass', 'glass_light'],
    'lens':    ['lens_deep', 'lens', 'lens_light'],
    'liquid':  ['liquid_deep', 'liquid', 'liquid_light'],
    'hot':     ['hot_deep', 'hot', 'hot_light'],
    'red':     ['red_deep', 'red', 'red_light'],
    'screen':  ['screen', 'screen_light'],
    'gem_blue': ['enamel_blue', 'jewel_blue'],
    'gem_red': ['red_deep', 'jewel_red'],
    'enamel_green': ['enamel_green'],
    'enamel_blue': ['enamel_blue'],
    'enamel_brown': ['enamel_brown'],
    'scuff':   ['scuff'],
    'highlight': ['highlight'],
    'outline': ['outline'],
}

# 素材ごとの模様の種類
MOYOU = {
    'stone': 'madara', 'paper': 'kami', 'wood': 'mokume', 'leather': 'tsubu',
    'iron': 'hake', 'steel': 'hake',
}


# ══════════════════════════════════════════════════════════════
#  形（符号付き距離。単位は格子。負が内側）
# ══════════════════════════════════════════════════════════════

class Katachi(object):
    def sdf(self, X, Y):
        raise NotImplementedError


class En(Katachi):
    def __init__(self, cx, cy, r):
        self.cx, self.cy, self.r = cx, cy, r

    def sdf(self, X, Y):
        return np.hypot(X - self.cx, Y - self.cy) - self.r


class Daen(Katachi):
    def __init__(self, cx, cy, rx, ry):
        self.cx, self.cy, self.rx, self.ry = cx, cy, rx, ry

    def sdf(self, X, Y):
        return (np.hypot((X - self.cx) / self.rx, (Y - self.cy) / self.ry) - 1.0) * min(self.rx, self.ry)


class Hako(Katachi):
    def __init__(self, x0, y0, x1, y1, maru=0.0):
        self.cx, self.cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
        self.hx, self.hy = abs(x1 - x0) / 2.0 - maru, abs(y1 - y0) / 2.0 - maru
        self.maru = maru

    def sdf(self, X, Y):
        dx = np.abs(X - self.cx) - self.hx
        dy = np.abs(Y - self.cy) - self.hy
        soto = np.hypot(np.maximum(dx, 0), np.maximum(dy, 0))
        uchi = np.minimum(np.maximum(dx, dy), 0)
        return soto + uchi - self.maru


class Obi(Katachi):
    """線分を太らせた形（両端は丸い）。w は太さ。"""

    def __init__(self, x0, y0, x1, y1, w):
        self.x0, self.y0, self.x1, self.y1, self.w = x0, y0, x1, y1, w

    def sdf(self, X, Y):
        ex, ey = self.x1 - self.x0, self.y1 - self.y0
        px, py = X - self.x0, Y - self.y0
        l2 = ex * ex + ey * ey
        t = np.clip((px * ex + py * ey) / l2, 0, 1) if l2 > 0 else 0
        return np.hypot(px - t * ex, py - t * ey) - self.w / 2.0


class Takaku(Katachi):
    """多角形（頂点の並び。単純な多角形）。"""

    def __init__(self, ten):
        self.ten = [(float(x), float(y)) for x, y in ten]

    def sdf(self, X, Y):
        n = len(self.ten)
        d = np.full(X.shape, np.inf)
        naka = np.zeros(X.shape, dtype=bool)
        for i in range(n):
            ax, ay = self.ten[i]
            bx, by = self.ten[(i + 1) % n]
            ex, ey = bx - ax, by - ay
            px, py = X - ax, Y - ay
            l2 = ex * ex + ey * ey
            t = np.clip((px * ex + py * ey) / l2, 0, 1) if l2 > 0 else 0
            d = np.minimum(d, np.hypot(px - t * ex, py - t * ey))
            if by != ay:
                cond = ((ay > Y) != (by > Y)) & (X < ex * (Y - ay) / ey + ax)
                naka ^= cond
        return np.where(naka, -d, d)


class Awase(Katachi):
    def __init__(self, *k):
        self.k = k

    def sdf(self, X, Y):
        d = self.k[0].sdf(X, Y)
        for k in self.k[1:]:
            d = np.minimum(d, k.sdf(X, Y))
        return d


class Hiku(Katachi):
    def __init__(self, a, *b):
        self.a, self.b = a, b

    def sdf(self, X, Y):
        d = self.a.sdf(X, Y)
        for b in self.b:
            d = np.maximum(d, -b.sdf(X, Y))
        return d


class Kasane(Katachi):
    def __init__(self, *k):
        self.k = k

    def sdf(self, X, Y):
        d = self.k[0].sdf(X, Y)
        for k in self.k[1:]:
            d = np.maximum(d, k.sdf(X, Y))
        return d


class Michi(Katachi):
    """細い部品。格子を1つずつ辿る道（8近傍）。太さは 1 格子。
    細い物は形のまま描くと格子の寄せで消えるので、最初から格子で持つ。"""

    def __init__(self, ten):
        self.masu = []
        for i in range(len(ten) - 1):
            x0, y0 = ten[i]
            x1, y1 = ten[i + 1]
            n = max(1, int(math.ceil(max(abs(x1 - x0), abs(y1 - y0)) * 4)))
            for k in range(n + 1):
                t = k / float(n)
                c = (int(math.floor(x0 + (x1 - x0) * t)), int(math.floor(y0 + (y1 - y0) * t)))
                if not self.masu or self.masu[-1] != c:
                    self.masu.append(c)
        self.masu = list(dict.fromkeys(self.masu))

    def sdf(self, X, Y):
        d = np.full(X.shape, np.inf)
        for cx, cy in self.masu:
            dx = np.abs(X - (cx + 0.5)) - 0.5
            dy = np.abs(Y - (cy + 0.5)) - 0.5
            dd = np.hypot(np.maximum(dx, 0), np.maximum(dy, 0)) + np.minimum(np.maximum(dx, dy), 0)
            d = np.minimum(d, dd)
        return d


# ══════════════════════════════════════════════════════════════
#  部品
# ══════════════════════════════════════════════════════════════

class Buhin(object):
    """
      katachi … 形
      sozai   … 素材（SOZAI の名前）
      kage    … 陰影。('flat', None) / ('sphere', cx, cy, r) / ('cyl_v', cx, r) / ('cyl_h', cy, r)
                 / ('plane', cx, cy, haba) / ('ring', cx, cy) / ('concave', cx, cy, r)
      z       … 重なり順（大きいほど手前）
      yose    … 縁を格子に寄せる割合（隣との色差が大きい部品に付ける）。False で寄せない
      dan     … 使う段の範囲 (lo, hi)（含む）
      fuchi   … 部品の縁に一番暗い段の縁を付けるか
      obi     … 輪郭の帯の幅（格子に対する割合）。None なら既定
      moyou   … 素材の模様を入れるか
      bias/gain … 陰影の調整
    """

    def __init__(self, katachi, sozai, kage=('flat', None), z=0, yose=False, dan=None,
                 fuchi=True, obi=None, moyou=True, bias=0.0, gain=1.0, na='', kiri=None):
        self.katachi, self.sozai, self.kage, self.z = katachi, sozai, kage, z
        self.yose = yose
        self.kiri = kiri            # この形の外には描かない（細い線を親の形で切る）
        self.dan = dan
        self.fuchi = fuchi
        self.obi = obi
        self.moyou = moyou
        self.bias, self.gain = bias, gain
        self.na = na
        self.hosoi = isinstance(katachi, Michi)


def hash01(X, Y, tane):
    """座標から決まる 0〜1 の値（状態を持たない乱数）。"""
    x = np.floor(X * 64.0).astype(np.int64)
    y = np.floor(Y * 64.0).astype(np.int64)
    h = (x * 374761393 + y * 668265263 + int(tane) * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFF) / 65535.0


def kaishu_noise(X, Y, tane, scale):
    """粗い連続ノイズ（格子の点の値を滑らかに補間）。"""
    gx, gy = X / scale, Y / scale
    x0, y0 = np.floor(gx), np.floor(gy)
    fx, fy = gx - x0, gy - y0
    fx = fx * fx * (3 - 2 * fx)
    fy = fy * fy * (3 - 2 * fy)
    a = hash01(x0 / 64.0, y0 / 64.0, tane)
    b = hash01((x0 + 1) / 64.0, y0 / 64.0, tane)
    c = hash01(x0 / 64.0, (y0 + 1) / 64.0, tane)
    d = hash01((x0 + 1) / 64.0, (y0 + 1) / 64.0, tane)
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


# ══════════════════════════════════════════════════════════════
#  描く
# ══════════════════════════════════════════════════════════════

def kage_s(b, X, Y):
    """陰影の明るさ 0〜1。"""
    k = b.kage
    t = k[0]
    if t == 'flat':
        s = np.full(X.shape, 0.5)
    elif t == 'sphere':
        _, cx, cy, r = k
        nx = np.clip((X - cx) / r, -1, 1)
        ny = np.clip((Y - cy) / r, -1, 1)
        nz = np.sqrt(np.clip(1 - nx * nx - ny * ny, 0, 1))
        s = 0.5 + 0.5 * (nx * HIKARI[0] + ny * HIKARI[1] + nz * HIKARI[2])
    elif t == 'concave':
        _, cx, cy, r = k
        nx = np.clip((X - cx) / r, -1, 1)
        ny = np.clip((Y - cy) / r, -1, 1)
        nz = np.sqrt(np.clip(1 - nx * nx - ny * ny, 0, 1))
        s = 0.5 + 0.5 * (-nx * HIKARI[0] - ny * HIKARI[1] + nz * HIKARI[2])
    elif t == 'cyl_v':
        _, cx, r = k
        u = np.clip((X - cx) / r, -1, 1)
        s = 0.5 + 0.5 * (u * HIKARI[0] + np.sqrt(1 - u * u) * HIKARI[2])
    elif t == 'cyl_h':
        _, cy, r = k
        v = np.clip((Y - cy) / r, -1, 1)
        s = 0.5 + 0.5 * (v * HIKARI[1] + np.sqrt(1 - v * v) * HIKARI[2])
    elif t == 'plane':
        _, cx, cy, haba = k
        s = 0.5 - ((X - cx) * 0.55 + (Y - cy) * 0.65) / haba
    elif t == 'ring':
        _, cx, cy = k
        th = np.arctan2(Y - cy, X - cx)
        thL = math.atan2(HIKARI[1], HIKARI[0])
        s = 0.5 + 0.5 * np.cos(th - thL)
    else:
        raise SystemExit('[中止] 知らない陰影 %s' % t)
    return np.clip(0.5 + (s - 0.5) * b.gain + b.bias, 0, 0.9999)


def moyou_delta(shu, X, Y, tane):
    """素材の模様。段を ±1 ずらす場所を返す。"""
    if shu == 'mokume':
        v = np.sin((Y * 2.2 + 0.5 * np.sin(X * 1.3 + tane)) * math.pi)
        return np.where(v > 0.86, -1, np.where(v < -0.93, 1, 0))
    if shu == 'tsubu':
        h = hash01(X, Y, tane)
        return np.where(h < 0.045, -1, np.where(h > 0.985, 1, 0))
    if shu == 'madara':
        n = kaishu_noise(X, Y, tane, 0.55) * 0.7 + kaishu_noise(X, Y, tane + 1, 0.2) * 0.3
        return np.where(n < 0.33, -1, np.where(n > 0.74, 1, 0))
    if shu == 'kami':
        n = kaishu_noise(X, Y, tane, 0.7)
        h = hash01(X, Y, tane + 3)
        return np.where((n < 0.3) & (h < 0.5), -1, 0)
    if shu == 'hake':
        v = np.sin(Y * 9.0 * math.pi)
        return np.where((v > 0.94) & (hash01(X, Y, tane) < 0.6), -1, 0)
    return np.zeros(X.shape, dtype=np.int64)


def obi_haba(b):
    """輪郭の帯の幅（格子に対する割合）。
    境の格子は「帯 ＋ 素材の一番暗い段」なので、暗い段と輪郭色の差が大きい素材ほど帯を太くする:
      (1 - 帯) × 差 <= 38  →  帯 >= 1 - 38/差"""
    if b.obi is not None:
        return b.obi
    if b.hosoi:
        return OBI_HOSOI
    fukai = PAL[SOZAI[b.sozai][0]]
    sa = max(abs(fukai[i] - PAL['outline'][i]) for i in range(3))
    saitei = 1.0 - RGB_SA / float(sa) if sa > 0 else 0.0
    return max(OBI, saitei + 0.03)


def yoseru(mask, sdf, S, wari, kitei_in=None, dame=None):
    """格子ごとに「入る側 (1-wari) 以上 / 出る側 wari 以下」に寄せる。
    mask: 元の形（sdf<=0）。sdf で近い画素から足し引きする。
    kitei_in: 強制的に入る格子（細い部品）。dame: 強制的に出る格子。
    戻り: 寄せた mask、格子の in/out (16×16)"""
    N = S * S
    out = np.zeros_like(mask)
    cell_in = np.zeros((16, 16), dtype=bool)
    k_in = int(math.ceil((1 - wari) * N))
    k_out = int(math.floor(wari * N))
    for cy in range(16):
        for cx in range(16):
            ys, xs = slice(cy * S, (cy + 1) * S), slice(cx * S, (cx + 1) * S)
            m = mask[ys, xs]
            d = sdf[ys, xs]
            cov = int(m.sum())
            iru = cov >= N * 0.5
            if kitei_in is not None and kitei_in[cy, cx]:
                iru = True
            if dame is not None and dame[cy, cx]:
                cell_in[cy, cx] = False
                out[ys, xs] = False
                continue
            cell_in[cy, cx] = iru
            if iru:
                if cov >= k_in:
                    o = m.copy()
                else:
                    # ★ 同じ距離の画素が並ぶ（長方形の角など）ので、並び順で切って個数をぴったり k_in にする
                    jun = np.argsort(d.ravel(), kind='stable')[:k_in]
                    o = np.zeros(N, dtype=bool)
                    o[jun] = True
                    o = o.reshape(S, S)
                o[S // 2, S // 2] = True            # 入る格子の中心は必ず不透明（下絵の定義）
            else:
                if cov <= k_out:
                    o = m.copy()
                elif k_out <= 0:
                    o = np.zeros((S, S), dtype=bool)
                else:
                    jun = np.argsort(np.where(m, d, np.inf).ravel(), kind='stable')[:k_out]
                    o = np.zeros(N, dtype=bool)
                    o[jun] = True
                    o = o.reshape(S, S) & m
                o[S // 2, S // 2] = False           # 出る格子の中心は必ず透明
            out[ys, xs] = o
    return out, cell_in


def kyori_soto(mask, dmax):
    """透明（mask が False。画像の外も透明）からの距離（八角形近似）。dmax より遠い所は dmax+1。"""
    R = mask.shape[0]
    d = np.full((R, R), dmax + 1, dtype=np.int32)
    soto = ~mask
    d[soto] = 0
    cur = np.pad(soto, 1, constant_values=True)
    for k in range(1, dmax + 1):
        n = cur[1:-1, 1:-1] | cur[:-2, 1:-1] | cur[2:, 1:-1] | cur[1:-1, :-2] | cur[1:-1, 2:]
        if k % 2 == 0:
            n = n | cur[:-2, :-2] | cur[:-2, 2:] | cur[2:, :-2] | cur[2:, 2:]
        d[n & (d > k)] = k
        cur = np.pad(n, 1, constant_values=True)
    return d


def egaku(buhin, R, tane=1):
    """部品の並びから 本番 (R,R,4) と 下絵 (16,16,4) を作る。"""
    S = R // 16
    p = (np.arange(R) + 0.5) / S
    X, Y = np.meshgrid(p, p)
    buhin = sorted(buhin, key=lambda b: b.z)
    sdfs = [b.katachi.sdf(X, Y) for b in buhin]
    masks = []
    kitei = np.zeros((16, 16), dtype=bool)
    for b, d in zip(buhin, sdfs):
        m = d <= 0
        if b.kiri is not None:
            m &= b.kiri.sdf(X, Y) <= 0
        if b.hosoi:
            for cx, cy in b.katachi.masu:
                if 0 <= cx < 16 and 0 <= cy < 16:
                    kitei[cy, cx] = True
        elif b.yose:
            m, _ = yoseru(m, d, S, float(b.yose))
        masks.append(m)
    # 重ねる（z の小さい順に塗る）
    pid = np.full((R, R), -1, dtype=np.int32)
    for i, m in enumerate(masks):
        pid[m] = i
    # 外周の格子は必ず透明
    dame = np.zeros((16, 16), dtype=bool)
    dame[0, :] = dame[15, :] = dame[:, 0] = dame[:, 15] = True
    sdf_u = np.min(np.stack(sdfs), axis=0)
    F, cell_in = yoseru(pid >= 0, sdf_u, S, ALPHA_WARI, kitei_in=kitei, dame=dame)
    tashita = F & (pid < 0)
    if tashita.any():
        chikai = np.argmin(np.stack(sdfs), axis=0)
        pid[tashita] = chikai[tashita]
    pid[~F] = -1
    # 境の格子（入る格子で、8近傍に出る格子がある）
    pad = np.pad(cell_in, 1, constant_values=False)
    tonari_out = np.zeros((16, 16), dtype=bool)
    for dy in (0, 1, 2):
        for dx in (0, 1, 2):
            if dy == 1 and dx == 1:
                continue
            tonari_out |= ~pad[dy:dy + 16, dx:dx + 16]
    sakai = cell_in & tonari_out
    sakai_px = np.repeat(np.repeat(sakai | ~cell_in, S, axis=0), S, axis=1)
    dmax = int(math.ceil(OBI * S)) + 1
    dout = kyori_soto(F, dmax)
    # 色の段
    tone = np.full((R, R), -1, dtype=np.int32)
    obi_px = np.zeros((R, R), dtype=np.int32)
    for i, b in enumerate(buhin):
        sel = pid == i
        if not sel.any():
            continue
        hashigo = SOZAI[b.sozai]
        K = len(hashigo)
        lo, hi = b.dan if b.dan else (0, K - 1)
        s = kage_s(b, X, Y)
        idx = lo + np.floor(s * (hi - lo + 1)).astype(np.int64)
        idx = np.clip(idx, lo, hi)
        if b.moyou and MOYOU.get(b.sozai):
            dl = moyou_delta(MOYOU[b.sozai], X, Y, tane * 31 + i)
            idx = np.clip(idx + dl, lo, hi)
        if b.fuchi and K > 1:
            d = sdfs[i]
            gy, gx = np.gradient(d)
            nrm = np.hypot(gx, gy) + 1e-9
            nl = (gx / nrm) * HIKARI[0] + (gy / nrm) * HIKARI[1]
            w = 0.10 + 0.16 * (1 - nl) / 2.0
            idx = np.where(d >= -w, lo, idx)
        tone[sel] = np.array([ROLE_NO[h] for h in hashigo])[idx[sel]]
        # ★ 境の格子では帯が格子の中心を含むこと（中心＝下絵が輪郭色になる）。細い部品は芯を残す
        haba = int(round(obi_haba(b) * S))
        obi_px[sel] = haba if b.hosoi else max(haba, S // 2 + 1)
    obi = F & sakai_px & (dout <= obi_px)
    tone[obi] = ROLE_NO['outline']
    nokori = F & sakai_px & ~obi
    for i, b in enumerate(buhin):
        sel = nokori & (pid == i)
        if sel.any() and b.sozai != 'highlight':
            tone[sel] = ROLE_NO[SOZAI[b.sozai][0]]
    im = np.zeros((R, R, 4), dtype=np.uint8)
    im[F] = RGBA[tone[F]]
    naoshi = nijou(im, tone, pid, buhin, S)
    shitae = im[S // 2::S, S // 2::S].copy()
    return im, shitae, naoshi


def nijou(im, tone, pid, buhin, S):
    """格子ごとに平均を見て、外れていたら【中心以外の画素の段】を、その素材の並びの中で
    平均が中心の色へ近づく向きに1段ずつ動かす。戻り: 直した格子の数。"""
    naoshi = 0
    for cy in range(16):
        for cx in range(16):
            ys, xs = slice(cy * S, (cy + 1) * S), slice(cx * S, (cx + 1) * S)
            naotta = False
            for _ in range(24):
                blk = im[ys, xs].astype(np.int64)
                op = blk[:, :, 3] == 255
                c = im[cy * S + S // 2, cx * S + S // 2].astype(np.int64)
                if c[3] != 255:
                    break
                mean = blk[op][:, :3].mean(axis=0)
                err = mean - c[:3]
                if np.abs(err).max() <= RGB_SA:
                    break
                # まず: 中心の1画素を、格子の中にある色のうち平均に一番近い物に替える
                #   （下絵は中心を拾うので、格子の多数派の色が下絵に出る。本番は1画素しか変わらない）
                t = tone[ys, xs]
                ci = tone[cy * S + S // 2, cx * S + S // 2]
                kouho = None
                for ti in np.unique(t[op]):
                    if ROLE[ti] in ('highlight', 'outline') and ti != ci:
                        continue
                    e2 = np.abs(mean - RGBA[ti][:3].astype(np.int64)).max()
                    if kouho is None or e2 < kouho[0]:
                        kouho = (e2, ti)
                if kouho is not None and kouho[1] != ci and kouho[0] <= RGB_SA:
                    tone[cy * S + S // 2, cx * S + S // 2] = kouho[1]
                    im[cy * S + S // 2, cx * S + S // 2] = RGBA[kouho[1]]
                    naotta = True
                    continue
                ch = int(np.argmax(np.abs(err)))
                muki = 1 if err[ch] > 0 else -1
                p = pid[ys, xs]
                best = None
                for ti in np.unique(t[op]):
                    if ti == ci:
                        continue
                    role = ROLE[ti]
                    for pi in np.unique(p[op & (t == ti)]):
                        hashigo = SOZAI[buhin[pi].sozai]
                        if role not in hashigo:
                            continue
                        k = hashigo.index(role)
                        for k2 in (k - 1, k + 1):
                            if not (0 <= k2 < len(hashigo)):
                                continue
                            heru = (PAL[role][ch] - PAL[hashigo[k2]][ch]) * muki
                            if heru <= 0:
                                continue
                            sel = op & (t == ti) & (p == pi)
                            score = heru * int(sel.sum())
                            if best is None or score > best[0]:
                                best = (score, sel, ROLE_NO[hashigo[k2]])
                if best is None:
                    break
                t[best[1]] = best[2]
                tone[ys, xs] = t
                blk2 = im[ys, xs]
                blk2[op] = RGBA[t[op]]
                naotta = True
            if naotta:
                naoshi += 1
    return naoshi


# ══════════════════════════════════════════════════════════════
#  確かめる（外れていたら止まる）
# ══════════════════════════════════════════════════════════════

def renketsu_kazu(mask):
    mask = mask.copy()
    h, w = mask.shape
    kazu = 0
    ys, xs = np.nonzero(mask)
    for y0, x0 in zip(ys, xs):
        if not mask[y0, x0]:
            continue
        kazu += 1
        stack = [(y0, x0)]
        mask[y0, x0] = False
        while stack:
            y, x = stack.pop()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < h and 0 <= nx < w and mask[ny, nx]:
                        mask[ny, nx] = False
                        stack.append((ny, nx))
    return kazu


def kakunin_jibun(fai, im, shitae):
    R = im.shape[0]
    S = R // 16
    a = im[:, :, 3]
    if not np.all((a == 0) | (a == 255)):
        raise SystemExit('[中止] %s: アルファが 0/255 以外' % fai)
    if np.any(im[a == 0][:, :3] != 0):
        raise SystemExit('[中止] %s: 透明画素の RGB が 0 でない' % fai)
    if not np.array_equal(im[S // 2::S, S // 2::S], shitae):
        raise SystemExit('[中止] %s: 中心の画素が下絵と違う' % fai)
    warui = []
    for cy in range(16):
        for cx in range(16):
            blk = im[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S].astype(np.int64)
            sa = shitae[cy, cx].astype(np.int64)
            al = blk[:, :, 3].mean()
            if abs(al - sa[3]) > 255 * 0.25:
                warui.append('(%d,%d) alpha %.0f/%d' % (cx, cy, al, sa[3]))
                continue
            if sa[3] == 255:
                op = blk[blk[:, :, 3] == 255][:, :3]
                d = np.abs(op.mean(axis=0) - sa[:3])
                if d.max() > 40:
                    warui.append('(%d,%d) rgb %s' % (cx, cy, d.round(0).astype(int).tolist()))
    if warui:
        raise SystemExit('[中止] %s: 条件② 外れ %d 格子: %s' % (fai, len(warui), warui[:6]))
    iro = np.unique(im.reshape(-1, 4), axis=0)
    if len(iro) > 64:
        raise SystemExit('[中止] %s: %d 色（64 まで）' % (fai, len(iro)))
    yurusu = set(PAL.values()) | {(0, 0, 0, 0)}
    for c in iro:
        if tuple(int(v) for v in c) not in yurusu:
            raise SystemExit('[中止] %s: パレットに無い色 %s' % (fai, c.tolist()))
    hl = renketsu_kazu(np.all(im == np.array(PAL['highlight']), axis=2))
    if hl != 1:
        raise SystemExit('[中止] %s: highlight のまとまりが %d 個' % (fai, hl))
    rin = np.all(shitae == np.array(PAL['outline']), axis=2)
    tou = np.pad(shitae[:, :, 3] == 0, 1, constant_values=True)
    tonari = np.zeros((16, 16), dtype=bool)
    for dy in (0, 1, 2):
        for dx in (0, 1, 2):
            if dy == 1 and dx == 1:
                continue
            tonari |= tou[dy:dy + 16, dx:dx + 16]
    if np.any(rin & ~tonari):
        ys, xs = np.nonzero(rin & ~tonari)
        raise SystemExit('[中止] %s: 下絵の輪郭が透明に接していない格子 %s' % (fai, list(zip(xs.tolist(), ys.tolist()))[:5]))
    ys, xs = np.nonzero(a)
    yohaku = (int(xs.min()), int(ys.min()), int(R - 1 - xs.max()), int(R - 1 - ys.max()))
    lo = S * 3 // 4
    if min(yohaku) < lo or max(yohaku) > 2 * S:
        raise SystemExit('[中止] %s: 余白 %s が %d〜%d px から外れている' % (fai, yohaku, lo, 2 * S))


# ══════════════════════════════════════════════════════════════
#  16枚の設計（座標は格子。0〜16。外周1格子は透明）
#  ★ 部品の置き方の決まり
#     ・隣と色差の大きい大きな部品には yose=0.22〜0.3 を付ける（縁を格子に寄せる）
#     ・同じ素材の段どうし・色差の小さい部品は yose=False で自由に描ける
#     ・細い物（轅・紐・線）は Michi（格子の道）で持つ。形のまま描くと寄せで消える
#     ・highlight は1部品だけ。格子の中心 (n+0.5) に置いて半径 0.45〜0.5 にすると下絵にも1画素 残る
# ══════════════════════════════════════════════════════════════

E = {}          # ファイル名 → (部品を返す関数, 意図, 経年箇所)


def flat(k, sozai, dan, z, **kw):
    kw.setdefault('fuchi', False)
    kw.setdefault('moyou', False)
    return Buhin(k, sozai, ('flat', None), dan=(dan, dan), z=z, **kw)


def hikari(cx, cy):
    """反射（highlight）。格子の中心 (n+0.5) に置き、格子の 85% ほどを占める角丸の四角。
    ★ 暗い素材の上では小さな反射だと格子の平均が highlight から離れすぎる（条件②）ので、この大きさにする。"""
    return flat(Hako(cx - 0.47, cy - 0.47, cx + 0.47, cy + 0.47, maru=0.2), 'highlight', 0, 9, na='反射')


def e_tsukinoishi():
    b = []
    b.append(Buhin(Hako(1, 11.5, 15, 15), 'black', ('plane', 8, 13, 14), z=1, yose=0.22, gain=0.6, na='台'))
    b.append(flat(Hako(1.3, 11.5, 14.7, 12.2), 'black', 2, 2, na='台の上面'))
    b.append(Buhin(Hako(5, 13, 11, 14), 'brass', ('cyl_h', 13.5, 0.7), z=3, yose=0.2, gain=1.2, na='銘板'))
    b.append(flat(En(5.45, 13.5, 0.17), 'brass', 0, 4, na='ねじ'))
    b.append(flat(En(10.55, 13.5, 0.17), 'brass', 0, 4, na='ねじ'))
    b.append(flat(Obi(12.3, 13.2, 13.4, 14.1, 0.14), 'black', 2, 4, na='擦れ'))
    b.append(Buhin(Hako(2, 1, 14, 11.5), 'glass', ('plane', 8, 6, 30), z=1, yose=0.22, gain=0.8, na='ガラス'))
    b.append(flat(Hako(12.9, 1.4, 13.9, 11.4), 'glass', 0, 2, na='右の厚み'))
    b.append(flat(Hako(2.1, 10.6, 13.9, 11.4), 'glass', 0, 2, na='下の厚み'))
    iwa = Awase(En(8.0, 6.4, 3.3), En(6.0, 7.3, 2.3), En(9.9, 5.4, 2.0), En(7.2, 4.6, 1.8))
    b.append(Buhin(iwa, 'iron', ('sphere', 7.8, 6.2, 4.2), z=5, dan=(0, 3), gain=1.3, bias=-0.05, na='岩'))
    for (cx, cy, r) in ((9.3, 7.2, 0.95), (6.5, 5.5, 0.6), (8.1, 4.4, 0.42), (6.2, 8.4, 0.38)):
        b.append(Buhin(En(cx, cy, r), 'iron', ('concave', cx, cy, r), z=6, dan=(0, 2), gain=1.4, moyou=False, na='クレーター'))
    b.append(flat(Obi(3.2, 1.6, 8.6, 7.0, 0.22), 'glass', 2, 7, na='筋'))
    b.append(flat(Obi(4.6, 1.6, 7.4, 4.4, 0.14), 'glass', 2, 7, na='筋'))
    b.append(hikari(3.5, 2.5))
    return b


E['tsukinoishi'] = (e_tsukinoishi, '黒い樹脂の台に載ったガラスの標本箱。中に灰色の月の岩（球の陰影とクレーター）。左上のガラスに反射が1か所', '台の右側の擦れ1か所')


def e_sensha():
    b = []
    b.append(Buhin(Michi([(9.5, 7.5), (14.5, 6.5)]), 'wood', ('flat', None), dan=(1, 1), z=1, fuchi=False, na='轅'))
    b.append(Buhin(Michi([(14.5, 4.5), (14.5, 8.5)]), 'wood', ('flat', None), dan=(1, 1), z=1, fuchi=False, na='軛'))
    kaba = Takaku([(4.0, 2.2), (9.6, 2.2), (9.6, 7.8), (3.4, 7.8), (2.5, 6.8), (2.1, 5.2), (2.4, 3.6), (3.1, 2.6)])
    b.append(Buhin(kaba, 'leather', ('plane', 6, 5, 10), z=2, yose=0.22, gain=0.9, na='車台'))
    b.append(Buhin(Hako(3.0, 2.2, 9.6, 2.9), 'wood', ('flat', None), dan=(1, 2), z=3, na='上の桟'))
    b.append(flat(Obi(2.3, 4.9, 9.6, 4.9, 0.28), 'wood', 1, 3, na='中の桟'))
    b.append(flat(Hako(2.6, 7.2, 9.7, 7.8), 'wood', 0, 3, na='床'))
    b.append(flat(Hako(7.4, 3.3, 8.6, 6.9), 'leather', 0, 3, na='矢筒'))
    b.append(flat(Hako(7.3, 4.2, 8.7, 4.5), 'brass', 1, 4, na='矢筒の帯'))
    b.append(flat(Hako(7.3, 6.1, 8.7, 6.4), 'brass', 1, 4, na='矢筒の帯'))
    # 車輪。輻の間は穴にせず暗い木にする（穴だと全部の格子が境になり、木の色が残らない）
    b.append(flat(En(5.5, 10.5, 4.5), 'wood', 0, 4, yose=0.22, na='車輪の奥'))
    b.append(Buhin(Hiku(En(5.5, 10.5, 4.5), En(5.5, 10.5, 2.9)), 'wood', ('ring', 5.5, 10.5), z=5, dan=(1, 2), gain=0.8, fuchi=False, na='輪'))
    b.append(Buhin(Obi(5.5, 6.6, 5.5, 14.4, 1.0), 'wood', ('cyl_v', 5.5, 0.5), z=5, dan=(1, 2), fuchi=False, na='輻'))
    b.append(Buhin(Obi(1.6, 10.5, 9.4, 10.5, 1.0), 'wood', ('cyl_h', 10.5, 0.5), z=5, dan=(1, 2), fuchi=False, na='輻'))
    b.append(Buhin(En(5.5, 10.5, 1.3), 'brass', ('sphere', 5.5, 10.5, 1.3), z=6, gain=1.1, na='轂'))
    b.append(hikari(5.5, 9.5))
    b.append(flat(Obi(5.0, 6.0, 6.3, 6.6, 0.35), 'leather', 2, 3, na='擦れ'))
    return b


E['sensha'] = (e_sensha, '右向きの側面図。前の丸い革張りの車台と木の桟、横に矢筒。左下に木の大車輪（十字の輻・青銅の轂）。車台の前下から右へ長く伸びる轅と、先端の軛', '車台の革の擦れ1か所')


def hoshi(cx, cy, r_out, r_in, n=8, kaiten=0.0):
    """星形の多角形（n つの角）。"""
    ten = []
    for i in range(2 * n):
        a = kaiten + math.pi * i / n
        r = r_out if i % 2 == 0 else r_in
        ten.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return Takaku(ten)


def chijimeru(ten, cx, cy, wari):
    return [(cx + (x - cx) * wari, cy + (y - cy) * wari) for x, y in ten]


def e_gofu():
    b = []
    ishi = Hiku(Awase(Daen(8, 9.4, 6.5, 5.6), En(8, 5.0, 3.8)),
                En(8.0, 4.0, 1.05),                                        # 紐を通す穴
                Takaku([(13.2, 11.0), (14.7, 12.2), (13.0, 13.2)]))       # 欠け（1か所）
    b.append(Buhin(ishi, 'stone', ('sphere', 8, 9.0, 7.2), z=2, yose=0.22, gain=1.0, bias=0.05, na='石'))
    b.append(flat(Hiku(En(8, 9.8, 2.9), En(8, 9.8, 2.4)), 'stone', 0, 3, na='刻み目の輪'))
    b.append(flat(En(8, 9.8, 0.75), 'stone', 0, 3, na='刻み目の芯'))
    b.append(flat(Obi(4.4, 7.6, 5.8, 6.6, 0.22), 'stone', 0, 3, na='刻み目'))
    b.append(flat(Obi(11.6, 7.6, 10.2, 6.6, 0.22), 'stone', 0, 3, na='刻み目'))
    b.append(Buhin(Michi([(7.5, 3.5), (6.5, 1.5)]), 'leather', ('flat', None), dan=(1, 1), z=6, fuchi=False, na='紐'))
    b.append(Buhin(Michi([(8.5, 3.5), (9.5, 1.5)]), 'leather', ('flat', None), dan=(1, 1), z=6, fuchi=False, na='紐'))
    b.append(hikari(5.5, 6.5))
    return b


E['gofu'] = (e_gofu, '革紐を通した石の護符。上に穴、面に輪と芯の刻み目（文字ではない）。球の陰影と石のまだら', '右下の欠け1か所')


def e_seihai():
    b = []
    sakazuki = Takaku([(3.6, 1.6), (14.5, 1.6), (14.3, 4.4), (13.2, 7.2), (11.4, 9.4), (9.9, 10.8),
                       (9.5, 11.6), (9.5, 13.2), (8.6, 14.6), (7.7, 13.0), (6.8, 14.3), (6.6, 11.6),
                       (5.9, 10.4), (4.8, 9.9), (3.6, 8.6), (2.0, 7.6), (1.5, 6.0), (3.2, 4.6), (2.4, 3.2)])
    b.append(Buhin(sakazuki, 'brass', ('cyl_v', 8.5, 6.8), z=2, yose=0.22, gain=1.1, na='杯'))
    b.append(Buhin(Hako(3.7, 1.6, 14.4, 2.7), 'brass', ('cyl_v', 8.5, 6.8), dan=(2, 3), z=3, fuchi=False, na='縁の帯'))
    b.append(flat(Obi(2.9, 4.6, 14.1, 4.6, 0.18), 'brass', 0, 3, na='彫り線'))
    b.append(flat(Obi(3.4, 8.0, 12.6, 8.0, 0.18), 'brass', 0, 3, na='彫り線'))
    b.append(Buhin(En(11.2, 6.4, 1.25), 'gem_blue', ('sphere', 11.2, 6.4, 1.25), z=4, yose=0.3, gain=1.2, na='宝石'))
    b.append(flat(Hiku(En(11.2, 6.4, 1.55), En(11.2, 6.4, 1.25)), 'brass', 0, 3, na='宝石の座'))
    b.append(hikari(11.5, 5.5))
    b.append(flat(Obi(12.9, 7.3, 13.5, 8.2, 0.3), 'brass', 0, 3, na='へこみ'))
    return b


E['seihai'] = (e_seihai, '割れた金の聖杯。左と脚が欠けた杯の右側の破片。縁の帯、2本の彫り線、青い宝石の座', '右下のへこみ1か所')


def e_kaizu():
    b = []
    kami = Hiku(Hako(1.5, 2.6, 14.5, 14.5), Takaku([(12.8, 14.6), (14.6, 12.6), (14.6, 14.6)]))
    b.append(Buhin(kami, 'paper', ('plane', 8, 8, 24), z=2, yose=0.22, gain=0.7, na='羊皮紙'))
    b.append(flat(Takaku([(12.8, 14.5), (14.5, 12.6), (13.2, 12.9)]), 'paper', 1, 3, na='めくれ'))
    b.append(Buhin(Hako(1.5, 1.5, 14.5, 3.3), 'paper', ('cyl_h', 2.4, 0.9), z=4, yose=0.22, gain=1.2, na='巻き'))
    # 海岸線（線）と、海側の斜線
    kaigan = [(1.6, 6.2), (3.4, 5.4), (5.0, 6.0), (6.4, 5.2), (8.2, 5.6), (9.6, 7.4), (9.2, 9.6), (10.8, 11.2), (12.6, 11.8), (14.4, 10.6)]
    for i in range(len(kaigan) - 1):
        (x0, y0), (x1, y1) = kaigan[i], kaigan[i + 1]
        b.append(flat(Obi(x0, y0, x1, y1, 0.17), 'paper', 0, 5, na='海岸線', kiri=kami))
        ex, ey = x1 - x0, y1 - y0
        L = math.hypot(ex, ey)
        nx, ny = -ey / L, ex / L                   # 海側（右下）へ
        k = int(L / 0.45)
        for j in range(k):
            t = (j + 0.5) / k
            px, py = x0 + ex * t, y0 + ey * t
            b.append(flat(Obi(px + nx * 0.12, py + ny * 0.12, px + nx * 0.5, py + ny * 0.5, 0.1), 'paper', 1, 5, na='斜線', kiri=kami))
    # 方位図（8つの角の星）と放射線
    b.append(flat(hoshi(5.0, 10.5, 1.9, 0.6, 8, -math.pi / 2), 'brass', 2, 6, na='方位図'))
    b.append(flat(hoshi(5.0, 10.5, 1.15, 0.45, 4, -math.pi / 2), 'brass', 0, 7, na='方位図の芯'))
    for a in (-0.35, 0.2, 0.75, 1.35):
        b.append(flat(Obi(5.0 + 2.1 * math.cos(a), 10.5 + 2.1 * math.sin(a), 5.0 + 9.5 * math.cos(a), 10.5 + 9.5 * math.sin(a), 0.09), 'paper', 1, 5, na='放射線', kiri=kami))
    b.append(flat(En(12.0, 5.0, 0.5), 'scuff', 0, 5, na='染み'))
    b.append(hikari(3.5, 2.5))
    return b


E['kaizu'] = (e_kaizu, '上を巻いた羊皮紙の海図。海岸線と海側の斜線、真鍮の方位図と放射線、右下のめくれ。文字は無い', '右上の染み1か所')


def e_haguruma():
    b = []
    ha = [En(8, 8, 5.6)]
    for i in range(10):
        a = math.pi * 2 * i / 10 + math.pi / 10
        c, s = math.cos(a), math.sin(a)
        n1, n2 = -s, c
        ha.append(Takaku([(8 + 5.3 * c + 0.85 * n1, 8 + 5.3 * s + 0.85 * n2), (8 + 7.0 * c + 0.6 * n1, 8 + 7.0 * s + 0.6 * n2),
                          (8 + 7.0 * c - 0.6 * n1, 8 + 7.0 * s - 0.6 * n2), (8 + 5.3 * c - 0.85 * n1, 8 + 5.3 * s - 0.85 * n2)]))
    karada = Hiku(Awase(*ha), Awase(En(8, 8, 1.55), Hako(7.5, 5.9, 8.5, 8.0)))
    b.append(Buhin(karada, 'iron', ('plane', 8, 8, 13), z=2, yose=0.22, gain=1.0, dan=(0, 3), na='歯車'))
    for i in range(4):
        # 肉抜きは穴にせず、暗い窪みにする（穴だらけにすると全部の格子が境になり、面の陰影が残らない）
        a = math.pi / 4 + math.pi / 2 * i
        cx, cy = 8 + 3.4 * math.cos(a), 8 + 3.4 * math.sin(a)
        b.append(Buhin(En(cx, cy, 1.0), 'iron', ('concave', cx, cy, 1.0), z=3, dan=(0, 1), gain=1.2, moyou=False, na='窪み'))
    b.append(flat(Hiku(En(8, 8, 2.4), En(8, 8, 1.95)), 'iron', 0, 3, na='轂の段'))
    b.append(Buhin(En(12.0, 11.0, 0.62), 'soot', ('flat', None), dan=(0, 1), z=4, fuchi=False, moyou=False, na='煤'))
    b.append(hikari(5.5, 5.5))
    return b


E['haguruma'] = (e_haguruma, '鉄の歯車。10枚の歯、鍵溝のある軸穴、4つの肉抜き穴、轂の段。鉄の刷毛目', '右下の煤1か所')


def e_oukan():
    b = []
    obi = Takaku([(1.5, 4.0), (14.5, 4.0), (14.5, 12.5), (8, 15.0), (1.5, 12.5)])
    b.append(Buhin(obi, 'brass', ('cyl_v', 8, 6.8), z=3, yose=0.22, gain=1.0, na='冠の帯'))
    b.append(Buhin(Daen(8, 4.0, 6.3, 1.9), 'iron', ('flat', None), dan=(0, 1), z=2, yose=0.22, fuchi=False, na='内側の鉄輪'))
    b.append(Buhin(Hiku(Daen(8, 4.0, 6.6, 2.1), Daen(8, 4.0, 6.0, 1.6)), 'brass', ('flat', None), dan=(1, 2), z=2, yose=False, fuchi=False, na='奥の縁'))
    for i, (x0, sozai, gem) in enumerate(((2.0, 'enamel_green', 'gem_red'), (5.0, 'enamel_blue', 'gem_blue'),
                                          (8.0, 'enamel_green', 'gem_red'), (11.0, 'enamel_blue', 'gem_blue'))):
        b.append(flat(Hako(x0 - 0.25, 5.75, x0 + 2.25, 10.75), 'brass', 1, 4, na='座'))
        b.append(Buhin(Hako(x0, 6.0, x0 + 2.0, 10.5), sozai, ('flat', None), z=5, yose=0.25, fuchi=False, moyou=False, na='七宝'))
        b.append(Buhin(En(x0 + 1.0, 8.25, 0.62), gem, ('sphere', x0 + 1.0, 8.25, 0.62), z=6, gain=1.2, na='宝石'))
    for x in (1.9, 3.7, 5.5, 7.3, 9.1, 10.9, 12.7, 14.1):
        b.append(flat(En(x, 4.9, 0.2), 'brass', 0, 5, na='鋲'))
        b.append(flat(En(x, 11.6 + (0.0 if abs(x - 8) > 3 else 0.6), 0.2), 'brass', 0, 5, na='鋲'))
    b.append(flat(Obi(12.6, 12.2, 13.6, 12.9, 0.22), 'brass', 0, 5, na='擦れ'))
    b.append(hikari(2.5, 5.5))
    return b


E['oukan'] = (e_oukan, '金の帯に七宝と宝石を並べた王冠。上の開口から内側の鉄の輪が見える。鋲の列', '右下の擦れ1か所')


def e_excalibur():
    b = []
    # ★ 横に寝かせた幅広の剣（切っ先が左）。縦だと余白が広すぎ、刃が細いと全部の格子が境になって鋼の明るさが出ない
    yaiba = Hiku(Takaku([(1.0, 8.0), (3.4, 6.0), (10.5, 6.0), (10.5, 10.0), (3.4, 10.0)]),
                 Takaku([(6.2, 10.05), (6.6, 9.55), (7.0, 10.05)]))
    b.append(Buhin(yaiba, 'steel', ('cyl_h', 8.0, 2.1), z=3, yose=0.22, gain=1.3, na='刃'))
    b.append(flat(Obi(3.6, 8.0, 10.0, 8.0, 0.3), 'steel', 0, 4, na='樋'))
    b.append(Buhin(Takaku([(10.5, 1.8), (13.5, 1.8), (13.5, 14.2), (10.5, 14.2), (10.9, 11.6), (10.9, 4.4)]), 'brass', ('cyl_v', 12.0, 1.5), z=5, yose=0.22, gain=1.1, na='鍔'))
    b.append(Buhin(Hako(13.5, 7.1, 14.3, 8.9), 'leather', ('cyl_h', 8.0, 0.9), z=4, yose=0.22, dan=(0, 1), na='柄'))
    for x in (13.7,):
        b.append(flat(Obi(x, 7.1, x + 0.3, 8.9, 0.18), 'leather', 0, 5, na='柄巻き'))
    b.append(Buhin(En(14.3, 8.0, 0.7), 'brass', ('sphere', 14.3, 8.0, 0.7), z=5, yose=0.22, gain=1.1, na='柄頭'))
    b.append(hikari(5.5, 7.5))
    return b


E['excalibur'] = (e_excalibur, '垂直に立てた直剣。鋼の刃と樋、真鍮の鍔と柄頭、革巻きの柄', '刃の右の刃こぼれ1か所')


def e_tate():
    b = []
    ten = [(1.5, 1.5), (14.5, 1.5), (14.5, 7.0), (13.2, 10.5), (10.5, 13.5), (8, 15.0), (5.5, 13.5), (2.8, 10.5), (1.5, 7.0)]
    b.append(Buhin(Takaku(ten), 'brass', ('plane', 8, 8, 18), z=2, yose=0.22, gain=0.8, na='縁金'))
    b.append(Buhin(Takaku(chijimeru(ten, 8, 7.8, 0.84)), 'stone', ('plane', 8, 8, 20), z=3, yose=0.25, gain=0.6, moyou=False, na='白い面'))
    b.append(Buhin(Awase(Hako(6.5, 3.0, 9.5, 11.5), Hako(3.5, 5.0, 12.5, 8.0)), 'red', ('plane', 8, 7, 14), z=4, yose=0.25, gain=0.6, na='十字'))
    for (x, y) in ((3.0, 2.4), (6.5, 2.4), (9.5, 2.4), (13.0, 2.4), (2.4, 6.5), (13.6, 6.5), (3.6, 10.5), (12.4, 10.5), (6.2, 13.0), (9.8, 13.0)):
        b.append(flat(En(x, y, 0.22), 'brass', 0, 5, na='鋲'))
    b.append(flat(Obi(10.6, 9.4, 11.7, 10.9, 0.18), 'stone', 0, 5, na='傷'))
    b.append(hikari(3.5, 3.5))
    return b


E['tate'] = (e_tate, '真鍮の縁金を打った騎士の盾。白い面に赤い十字、縁に鋲', '右下の白い面の傷1か所')


def kaiten_hako(cx, cy, w, h, do):
    """回した長方形（角度は度。正で時計回り）。"""
    a = math.radians(do)
    c, s = math.cos(a), math.sin(a)
    ten = []
    for px, py in ((-w / 2, -h / 2), (w / 2, -h / 2), (w / 2, h / 2), (-w / 2, h / 2)):
        ten.append((cx + px * c - py * s, cy + px * s + py * c))
    return Takaku(ten)


def kaiten_ten(cx, cy, px, py, do):
    a = math.radians(do)
    return cx + px * math.cos(a) - py * math.sin(a), cy + px * math.sin(a) + py * math.cos(a)


def e_tegata():
    b = []
    kami = Hiku(Hako(1.5, 1.5, 13.5, 14.5), Takaku([(1.4, 1.4), (2.7, 1.4), (1.4, 2.5)]))     # 左上の破れ（1か所）
    b.append(Buhin(kami, 'paper', ('plane', 7.5, 8, 22), z=2, yose=0.22, gain=0.7, na='証文'))
    b.append(flat(Obi(1.7, 8.0, 13.3, 8.0, 0.12), 'paper', 1, 3, na='折り目'))
    b.append(flat(Obi(2.6, 2.6, 6.4, 2.6, 0.36), 'paper', 0, 4, na='見出しの線'))
    # 書いてある風の線（文字ではない）
    k = 0
    for y in (3.9, 4.9, 5.9, 6.9, 9.4, 10.4, 11.4, 12.4):
        x = 2.6
        while x < 12.4:
            L = (0.7, 1.4, 1.0, 1.9, 0.6, 1.2)[k % 6]
            k += 1
            x1 = min(x + L, 12.6)
            if y > 9 and x1 > 9.8:
                break
            b.append(flat(Obi(x, y, x1, y, 0.2), 'paper', 0, 4, na='線', kiri=kami))
            x = x1 + 0.45
    b.append(Buhin(En(12.6, 11.4, 1.6), 'red', ('sphere', 12.6, 11.4, 1.6), z=6, yose=0.25, gain=0.9, na='封蝋'))
    b.append(flat(Hiku(En(12.6, 11.4, 1.05), En(12.6, 11.4, 0.75)), 'red', 0, 7, na='封蝋の型'))
    b.append(flat(Obi(12.0, 12.6, 10.6, 14.5, 0.55), 'red', 0, 5, na='紐'))
    b.append(flat(Obi(13.0, 12.7, 12.2, 14.6, 0.55), 'red', 0, 5, na='紐'))
    b.append(hikari(11.5, 10.5))
    return b


E['tegata'] = (e_tegata, '羊皮紙の証文。見出しの線と書き込み風の線（文字ではない）、折り目、赤い封蝋と紐', '左上の破れ1か所')


def e_tenro():
    b = []
    # 洋梨形の胴 ＋ 右上へ傾いた首。口に火。耳軸と脚と台
    kubi = kaiten_hako(10.4, 4.8, 3.4, 5.6, 38)
    karada = Awase(Daen(7.4, 9.8, 4.7, 4.3), En(7.6, 7.2, 3.4), kubi)
    b.append(Buhin(karada, 'iron', ('sphere', 7.4, 9.0, 5.6), z=3, yose=0.22, dan=(0, 3), gain=1.1, na='転炉'))
    mx, my = kaiten_ten(10.4, 4.8, 0, -2.8, 38)
    b.append(Buhin(Daen(mx, my, 1.25, 0.8), 'hot', ('sphere', mx, my, 1.1), z=5, yose=0.22, dan=(1, 2), gain=0.8, moyou=False, na='口の火'))
    b.append(flat(Obi(3.4, 9.8, 12.2, 9.8, 0.55), 'iron', 0, 4, na='耳軸の帯'))
    for x in (2.8, 12.6):
        b.append(flat(En(x, 9.8, 1.05), 'iron', 0, 4, na='耳軸'))
        b.append(flat(En(x, 9.8, 0.5), 'iron', 2, 5, na='耳軸の頭'))
    b.append(flat(Hako(2.2, 9.8, 3.4, 14.0), 'iron', 0, 2, na='脚'))
    b.append(flat(Hako(12.0, 9.8, 13.2, 14.0), 'iron', 0, 2, na='脚'))
    b.append(Buhin(Hako(1.5, 13.6, 14.4, 14.9), 'iron', ('flat', None), dan=(0, 1), z=2, yose=0.22, na='台'))
    for y in (7.9, 11.6):
        for x in (4.8, 6.2, 7.6, 9.0, 10.2):
            b.append(flat(En(x, y, 0.19), 'iron', 3, 5, na='鋲'))
    b.append(Buhin(En(10.0, 12.4, 0.62), 'soot', ('flat', None), dan=(0, 1), z=5, fuchi=False, moyou=False, na='煤'))
    b.append(hikari(5.5, 7.5))
    return b


E['tenro'] = (e_tenro, '鉄の転炉。右上に傾いた口から火が見える。耳軸と脚、台、鋲の列、鉄の刷毛目', '胴の右下の煤1か所')


def e_kinko():
    b = []
    b.append(Buhin(Hako(1.5, 1.5, 14.5, 14.5), 'iron', ('plane', 8, 8, 24), z=2, yose=0.22, dan=(0, 2), gain=0.6, na='金庫'))
    b.append(Buhin(Hako(2.7, 2.7, 13.3, 13.3), 'iron', ('plane', 8, 8, 20), z=3, dan=(1, 2), gain=0.5, na='扉'))
    for (x, y) in ((2.7, 2.7), (12.1, 2.7), (2.7, 12.1), (12.1, 12.1)):
        b.append(Buhin(Hako(x, y, x + 1.2, y + 1.2), 'brass', ('plane', x + 0.6, y + 0.6, 2), z=4, dan=(1, 2), gain=0.5, fuchi=False, na='角金'))
    b.append(Buhin(En(6.0, 8.0, 2.3), 'brass', ('sphere', 6.0, 8.0, 2.3), z=5, gain=1.0, na='ダイヤル'))
    b.append(flat(En(6.0, 8.0, 1.5), 'iron', 3, 6, na='ダイヤルの面'))
    for i in range(8):
        a = math.pi * 2 * i / 8
        b.append(flat(Obi(6.0 + 1.55 * math.cos(a), 8.0 + 1.55 * math.sin(a), 6.0 + 2.0 * math.cos(a), 8.0 + 2.0 * math.sin(a), 0.16), 'brass', 0, 7, na='目盛'))
    b.append(flat(Obi(6.0, 8.0, 6.0, 6.7, 0.24), 'iron', 0, 7, na='針'))
    b.append(Buhin(Obi(9.6, 8.0, 12.6, 8.0, 0.6), 'brass', ('cyl_h', 8.0, 0.3), z=5, gain=1.0, na='取っ手'))
    b.append(Buhin(En(9.8, 8.0, 0.75), 'brass', ('sphere', 9.8, 8.0, 0.75), z=6, gain=1.0, na='取っ手の軸'))
    for x in (2.1, 4.2, 6.3, 8.4, 10.5, 12.6, 13.9):
        for y in (2.1, 13.9):
            b.append(flat(En(x, y, 0.18), 'iron', 2, 4, na='鋲'))
    for y in (4.2, 6.3, 8.4, 10.5, 12.6):
        for x in (2.1, 13.9):
            b.append(flat(En(x, y, 0.18), 'iron', 2, 4, na='鋲'))
    b.append(flat(Obi(10.8, 11.0, 12.2, 12.3, 0.16), 'iron', 3, 6, na='傷'))
    b.append(hikari(5.5, 7.5))
    return b


E['kinko'] = (e_kinko, '鉄の金庫。扉の面、四隅の角金、真鍮のダイヤルと目盛、取っ手、鋲の縁', '扉の右下の傷1か所')


def e_tebiki():
    b = []
    b.append(Buhin(Hako(12.0, 2.2, 14.3, 14.0), 'paper', ('flat', None), dan=(1, 2), z=2, yose=0.22, moyou=False, na='小口'))
    for y in (3.0, 4.6, 6.2, 7.8, 9.4, 11.0, 12.6):
        b.append(flat(Obi(12.2, y, 14.1, y, 0.12), 'paper', 0, 3, na='紙の段'))
    b.append(Buhin(Hako(1.5, 1.5, 12.4, 14.5), 'leather', ('plane', 7, 8, 26), z=3, yose=0.22, dan=(0, 1), gain=0.6, na='表紙'))
    b.append(flat(Hako(1.5, 1.5, 3.0, 14.5), 'leather', 0, 4, na='背'))
    for y in (3.4, 7.7, 12.0):
        b.append(flat(Hako(1.5, y, 3.0, y + 0.6), 'leather', 1, 5, na='背の帯'))
    for (x, y, dx, dy) in ((3.0, 1.5, 1, 1), (12.4, 1.5, -1, 1), (3.0, 14.5, 1, -1), (12.4, 14.5, -1, -1)):
        b.append(flat(Takaku([(x, y), (x + dx * 2.0, y), (x, y + dy * 2.0)]), 'brass', 1, 5, na='角金'))
    b.append(Buhin(Hako(11.2, 7.1, 14.2, 8.9), 'brass', ('cyl_h', 8.0, 0.9), z=6, yose=0.25, gain=1.0, na='留め金'))
    b.append(flat(Hiku(En(7.5, 7.7, 1.9), En(7.5, 7.7, 1.5)), 'brass', 2, 5, na='紋の輪'))
    b.append(flat(hoshi(7.5, 7.7, 0.95, 0.4, 4, -math.pi / 2), 'brass', 2, 5, na='紋'))
    b.append(flat(Obi(9.4, 11.6, 10.8, 12.8, 0.22), 'leather', 2, 5, na='擦れ'))
    b.append(hikari(12.5, 7.5))
    return b


E['tebiki'] = (e_tebiki, '黒革の手引書。背の帯、四隅の角金、真鍮の留め金、表紙の紋（輪と菱。文字ではない）、右に紙の小口', '表紙の右下の擦れ1か所')


def e_penicillin():
    b = []
    b.append(Buhin(Hako(2.0, 3.0, 14.0, 14.5, maru=0.7), 'glass', ('cyl_v', 8.0, 6.0), z=2, yose=0.22, gain=1.0, na='瓶'))
    b.append(Buhin(Hako(2.4, 8.4, 13.6, 14.1, maru=0.5), 'liquid', ('cyl_v', 8.0, 5.6), z=3, yose=0.25, gain=1.0, na='薬液'))
    b.append(flat(Obi(2.5, 8.4, 13.5, 8.4, 0.2), 'liquid', 2, 4, na='液面'))
    b.append(Buhin(Hako(2.1, 5.0, 13.9, 7.6), 'stone', ('flat', None), dan=(1, 2), z=4, yose=0.25, moyou=False, fuchi=False, na='札'))
    b.append(flat(Hako(2.1, 5.0, 13.9, 5.25), 'red', 1, 5, na='札の帯'))
    b.append(Buhin(Hako(2.6, 1.5, 13.4, 3.3), 'steel', ('cyl_v', 8.0, 5.4), z=5, yose=0.22, gain=1.0, na='蓋'))
    b.append(flat(Hako(3.2, 2.7, 12.8, 3.6), 'black', 0, 6, na='栓'))
    b.append(flat(Obi(11.6, 1.9, 12.5, 2.6, 0.16), 'steel', 0, 7, na='傷'))
    b.append(hikari(5.5, 4.5))
    return b


E['penicillin'] = (e_penicillin, 'ガラスの薬瓶。淡い黄緑の薬液、白い札（文字は無い）、金属の蓋とゴムの栓', '蓋の右上の傷1か所')


def e_goggle():
    b = []
    b.append(Buhin(Hako(2.0, 1.5, 14.0, 8.2, maru=0.5), 'black', ('plane', 8, 4.8, 16), z=2, yose=0.22, dan=(0, 2), gain=0.6, na='本体'))
    b.append(flat(Hako(9.6, 1.5, 13.2, 3.6), 'black', 1, 3, na='電池'))
    b.append(flat(Hako(1.5, 3.0, 2.2, 6.6), 'black', 2, 3, na='帯の金具'))
    b.append(flat(Hako(13.8, 3.0, 14.5, 6.6), 'black', 2, 3, na='帯の金具'))
    for cx in (5.0, 11.0):
        b.append(Buhin(En(cx, 10.8, 3.6), 'black', ('sphere', cx, 10.8, 3.6), z=4, yose=0.22, dan=(0, 2), gain=0.9, na='筒'))
        b.append(Buhin(En(cx, 10.8, 2.5), 'lens', ('sphere', cx, 10.8, 2.5), z=5, yose=0.25, gain=1.1, na='レンズ'))
    b.append(flat(Hako(6.6, 8.2, 9.4, 11.2), 'black', 1, 3, na='つなぎ'))
    b.append(flat(En(12.4, 3.2, 0.42), 'scuff', 0, 5, na='擦れ'))
    b.append(hikari(4.5, 9.5))
    return b


E['goggle'] = (e_goggle, '双眼の暗視ゴーグル。黒い樹脂の本体と電池、2つの筒に緑のレンズ', '本体の右上の擦れ1か所')


def e_sumaho():
    b = []
    do = 20
    b.append(Buhin(kaiten_hako(8, 8, 8.5, 12, do), 'black', ('plane', 8, 8, 20), z=2, yose=0.22, dan=(0, 2), gain=0.6, na='本体'))
    b.append(flat(Hiku(kaiten_hako(8, 8, 8.2, 11.7, do), kaiten_hako(8, 8, 7.8, 11.3, do)), 'steel', 1, 3, na='縁'))
    gamen = kaiten_hako(8, 8.2, 7.3, 10.4, do)
    b.append(Buhin(gamen, 'screen', ('flat', None), dan=(0, 0), z=4, moyou=False, na='画面'))
    x0, y0 = kaiten_ten(8, 8.2, -3.0, -4.6, do)
    x1, y1 = kaiten_ten(8, 8.2, 3.0, 2.2, do)
    b.append(flat(Obi(x0, y0, x1, y1, 0.5), 'screen', 1, 5, na='映り込み', kiri=gamen))
    cx, cy = kaiten_ten(8, 8, 0, -5.55, do)
    b.append(flat(En(cx, cy, 0.22), 'black', 2, 5, na='カメラ'))
    kx, ky = kaiten_ten(8, 8.2, -3.4, 5.2, do)
    for (dx, dy) in ((1.6, -0.5), (1.9, -1.6), (0.9, -2.0)):
        b.append(flat(Obi(kx, ky, kx + dx, ky + dy, 0.12), 'screen', 1, 6, na='ひび', kiri=gamen))
    hx, hy = kaiten_ten(8, 8.2, -2.0, -3.6, do)
    b.append(hikari(round(hx - 0.5) + 0.5, round(hy - 0.5) + 0.5))
    return b


E['sumaho'] = (e_sumaho, '斜めに置いたスマートフォン。黒い本体、金属の縁、消灯した濃紺の画面に映り込み、上端のカメラ', '画面の左下のひび1か所')


# ══════════════════════════════════════════════════════════════
#  出力
# ══════════════════════════════════════════════════════════════

def png_bytes(arr):
    bio = io.BytesIO()
    Image.fromarray(arr, 'RGBA').save(bio, format='PNG', optimize=False, compress_level=9)
    return bio.getvalue()


def kaku_bytes(p, b):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'wb') as f:
        f.write(b)


def iro_mei(rgba):
    if rgba == (0, 0, 0, 0):
        return 'transparent'
    for na, v in PAL.items():
        if v == rgba:
            return na
    return '?'


def file_kiroku(rel, b, category, **sono):
    arr = np.array(Image.open(io.BytesIO(b)).convert('RGBA'))
    iro, kazu = np.unique(arr.reshape(-1, 4), axis=0, return_counts=True)
    d = dict(file=rel.replace(os.sep, '/'), category=category,
             dimensions=[int(arr.shape[1]), int(arr.shape[0])],
             byte_size=len(b), sha256=hashlib.sha256(b).hexdigest().upper(),
             rgba_color_count=int(len(iro)),
             alpha_values=sorted(int(v) for v in np.unique(arr[:, :, 3])),
             rgba_colors=[{'rgba': '#%02X%02X%02X%02X' % tuple(int(v) for v in c), 'pixel_count': int(k)}
                          for c, k in zip(iro, kazu)],
             contains_baked_text=False)
    d.update(sono)
    return d


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=OUT_KITEI)
    ap.add_argument('--mono', default='')
    args = ap.parse_args()
    paretto_junbi()
    zenbu = hyou()
    erabu = [m for m in zenbu if m['fai'] in E]
    if args.mono:
        mono = set(args.mono.split(','))
        erabu = [m for m in erabu if m['fai'] in mono]
    if not erabu:
        raise SystemExit('[中止] 描く物がありません')
    mada = [m['fai'] for m in zenbu if m['fai'] not in E]
    iraibun_sha = hashlib.sha256(open(IRAIBUN, 'rb').read()).hexdigest().upper()
    root = args.out
    files, items = [], []
    dame = []
    for m in erabu:
        fn, ito, kizu = E[m['fai']]
        im, shitae, naoshi = egaku(fn(), m['ookisa'], tane=m['ban'])
        try:
            kakunin_jibun(m['fai'], im, shitae)
        except SystemExit as e:
            # ★ 1枚で止めず、全枚の問題をまとめて出す（最後に止まる）
            print(str(e))
            dame.append(m['fai'])
        hon = os.path.join('assets', 'jidaiui', 'textures', 'item', m['fai'] + '.png')
        shi = os.path.join('shitae', m['fai'] + '.png')
        pre = os.path.join('preview', m['fai'] + '.png')
        pre_s = os.path.join('preview_shitae', m['fai'] + '.png')
        b_hon = png_bytes(im)
        b_shi = png_bytes(shitae)
        b_pre = png_bytes(np.ascontiguousarray(im[1::2, 1::2]))
        b_pre_s = png_bytes(np.repeat(np.repeat(shitae, 8, axis=0), 8, axis=1))
        for rel, b in ((hon, b_hon), (shi, b_shi), (pre, b_pre), (pre_s, b_pre_s)):
            kaku_bytes(os.path.join(root, rel), b)
        yomi = np.array(Image.open(io.BytesIO(b_hon)).convert('RGBA').resize((16, 16), Image.NEAREST))
        if not np.array_equal(yomi, shitae):
            raise SystemExit('[中止] %s: Pillow の nearest 縮小が下絵と違う' % m['fai'])
        kyoutsuu = dict(name=m['mei'], era=m['jidai'], custom_model_data=m['ban'])
        files.append(file_kiroku(hon, b_hon, 'item_texture', **kyoutsuu))
        files.append(file_kiroku(shi, b_shi, 'shitae', **kyoutsuu))
        files.append(file_kiroku(pre, b_pre, 'preview', **kyoutsuu))
        files.append(file_kiroku(pre_s, b_pre_s, 'preview_shitae', **kyoutsuu))
        iro, kazu = np.unique(im.reshape(-1, 4), axis=0, return_counts=True)
        items.append(dict(
            name=m['mei'], era=m['jidai'], custom_model_data=m['ban'],
            file=hon.replace(os.sep, '/'), shitae=shi.replace(os.sep, '/'),
            preview=pre.replace(os.sep, '/'), preview_shitae=pre_s.replace(os.sep, '/'),
            resource_location='jidaiui:textures/item/%s.png' % m['fai'],
            size=[m['ookisa'], m['ookisa']], cell=m['ookisa'] // 16,
            intent=ito, single_wear_point=kizu, mean_fix_cells=int(naoshi),
            palette=[{'role': iro_mei(tuple(int(v) for v in c)),
                      'rgba': '#%02X%02X%02X%02X' % tuple(int(v) for v in c), 'pixel_count': int(k)}
                     for c, k in zip(iro, kazu)]))
        print('  %-12s %s %dx%d %2d色  ②で寄せた格子 %d' % (m['fai'], m['mei'], m['ookisa'], m['ookisa'], len(iro), naoshi))
    man = {
        'schema_version': 1,
        'package': {
            'name': 'JidaiCraft Tokushu v7',
            'namespace': 'jidaiui', 'minecraft': '1.20.1',
            'canonical_item_count': 16, 'drawn_item_count': len(erabu), 'not_yet_drawn': mada,
            'production_texture_count': sum(1 for f in files if f['category'] == 'item_texture'),
            'source_request_sha256': iraibun_sha,
            'generator': 'clientmod/tools/tokushu_e_tsukuru.py',
        },
        'provenance': {
            'method': 'deterministic: signed-distance shapes rendered at full resolution, edges snapped to the 16-cell grid (>=78%/<=22% per cell), quantized shading + hash textures; shitae = cell-center samples',
            'image_generation_model_used': False, 'baked_text_used': False, 'anti_aliasing_used': False,
            'randomness_used': 'coordinate hash only (stateless)', 'light_direction': 'top_left',
            'common_outline_rgba': '#%02X%02X%02X%02X' % PAL['outline'],
            'outline_band': '%.2f cell (thin parts %.2f cell)' % (OBI, OBI_HOSOI),
            'shrink_rule_nearest': 'honban[S/2::S, S/2::S] == shitae (S = 32 for 512, 16 for 256)',
            'shrink_rule_mean': 'per cell: |mean alpha - shitae alpha| <= 25% of 255; for opaque shitae cells, |mean RGB over opaque pixels - shitae RGB| <= 40 per channel',
        },
        'shared_palette': {na: '#%02X%02X%02X%02X' % v for na, v in PAL.items()},
        'new_colors': {na: dict(rgba=hx, reason=r) for na, (hx, r) in ATARASHII_IRO.items()},
        'items': items, 'files': files,
    }
    io.open(os.path.join(root, 'manifest.json'), 'w', encoding='utf-8', newline='\n').write(
        json.dumps(man, ensure_ascii=False, indent=2) + '\n')
    design_spec_kaku(root, man)
    if dame:
        raise SystemExit('[中止] 自己確認で外れた絵: %s' % ' '.join(dame))
    print('できました: %s' % root)
    if mada:
        print('  ★ まだ描いていない: %s' % ' '.join(mada))


def design_spec_kaku(root, man):
    g = []
    g.append('# 時代クラフト 特殊アイテム v7 — DESIGN_SPEC')
    g.append('')
    g.append('- 指示文: `docs/Fable依頼文_特殊アイテム.md`  sha256 = `%s`' % man['package']['source_request_sha256'])
    g.append('- 吐いた道具: `%s`（2回動かして同じ byte）' % man['package']['generator'])
    g.append('- 正本: 名前・ファイル名・番号は `plugin/src/main/java/jidai/Shouri.java`、色は v6 `DESIGN_SPEC.md` の共有パレット %d 色' % len(man['shared_palette']))
    g.append('')
    g.append('## 作り方（指示文 §2 の条件①②を守る）')
    g.append('')
    g.append('1. 形を 円・多角形・帯 などの図形（符号付き距離）で本番の解像度に描く')
    g.append('2. 面は 球・円柱・面 の陰影を段階色で付け、素材ごとの模様（木目・革の粒・石のまだら・紙のむら・鉄の刷毛目）を画素単位で入れる')
    g.append('3. 形の縁を格子に寄せる: 格子ごとに「入る側 78% 以上／出る側 22% 以下」（アルファ 25% の内側）')
    g.append('4. 輪郭は透明に接する格子だけに %.2f 格子の帯。その内側は素材の一番暗い色' % OBI)
    g.append('5. 下絵（16×16）は本番の【格子の中心の画素】＝ 条件①の定義そのもの。格子の平均が外れた所は同じ素材の段を1つ寄せる')
    g.append('')
    g.append('| 条件 | 定義 | 確かめ方 |')
    g.append('|---|---|---|')
    g.append('| ① nearest | `本番[S/2::S, S/2::S] == 下絵`（S = 格子の一辺。Pillow の NEAREST と同じ「中心を拾う」） | 吐いた byte を読み直して照合 |')
    g.append('| ② 平均 | 格子ごとに アルファの平均が下絵から 25% 以内。下絵が不透明なら 不透明画素の RGB 平均が各成分 40 以内 | 全格子を numpy で数える |')
    g.append('')
    g.append('## 共通の作図規約（指示文 §3）')
    g.append('')
    g.append('- 背景 `#00000000`、不透明度は 0 か 255 だけ。ぼかし・発光・半透明・グラデーション・アンチエイリアス無し')
    g.append('- 完全に透明な画素の RGB は 0,0,0')
    g.append('- 輪郭は `outline` (#102329)。透明に接する格子にだけ置く。素材面の内側は色面の境界だけ')
    g.append('- 光源は左上。`highlight` (#F5DE9B) は1枚に1つの連結したまとまりだけ')
    g.append('- 経年の傷・欠け・煤は1枚に1か所')
    g.append('- 外周の透明余白は 1格子（512: 32px / 256: 16px）')
    g.append('- 読める文字・数字・ロゴ・実在メーカーの UI は無い。長方形の角は直角')
    g.append('- 色は v6 の共有パレット %d 色から。足した色: %s' % (
        len(man['shared_palette']),
        '無し' if not man['new_colors'] else ', '.join('`%s` %s（%s）' % (k, v['rgba'], v['reason']) for k, v in man['new_colors'].items())))
    g.append('')
    g.append('## 1枚ごと')
    g.append('')
    for it in man['items']:
        g.append('### %s — `%s`' % (it['name'], os.path.basename(it['file'])))
        g.append('')
        g.append('- 時代: **%s** / 番号: %d / 大きさ: %d×%d（格子 %dpx）' % (it['era'], it['custom_model_data'], it['size'][0], it['size'][1], it['cell']))
        g.append('- 意図: %s' % it['intent'])
        g.append('- 唯一の経年箇所: %s' % it['single_wear_point'])
        g.append('- 使用色: **%d**（透明を含む）。条件②で段を寄せた格子: %d' % (len(it['palette']), it['mean_fix_cells']))
        g.append('')
        g.append('| role | RGBA | pixel数 |')
        g.append('|---|---|---:|')
        for p in it['palette']:
            g.append('| `%s` | `%s` | %d |' % (p['role'], p['rgba'], p['pixel_count']))
        g.append('')
    g.append('## manifest')
    g.append('')
    g.append('- `manifest.json` は根の直下。`files` の各項目に `category`（`item_texture` / `shitae` / `preview` / `preview_shitae`）・`sha256`（大文字）・`dimensions` を入れている')
    g.append('- `preview/` は本番を nearest で 1/2 に縮めた物。`preview_shitae/` は下絵を nearest で 8倍にした物。どちらも同じ実行で吐く')
    g.append('- 検査: `python tests/tokushu_e_kakunin.py clientmod/nouhin/v7`')
    g.append('')
    io.open(os.path.join(root, 'DESIGN_SPEC.md'), 'w', encoding='utf-8', newline='\n').write('\n'.join(g))


if __name__ == '__main__':
    main()
