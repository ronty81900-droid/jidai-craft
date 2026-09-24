# -*- coding: utf-8 -*-
"""
sekiyu_e.py -- いただいた石油のドット絵を、アイテムの絵に仕立てる。

  ★★ なぜ道具にするか ★★
    もらった絵をそのまま置くと、**どう加工したか**が誰にも分からなくなる。
    元の絵は `nouhin/sekiyu/moto/` に手つかずで残し、
    本番の PNG は【この道具が毎回 作り直す】形にする。
    2回 動かして同じ byte になることを、検査が見張る。

  ★★ もらった絵について（測った値・推測ではない）★★
    ・1254×1254 の WEBP、**アルファ無し**。背景は不透明な灰 RGB(48,48,48)
    ・格子は ざっと96相当だが **きれいな格子に乗っていない**
      （縮めて戻した時の誤差に段差が無い＝生成した絵をドット絵「風」にした物）
    ・色数 7219。webp の圧縮でにじんでいる
    ・樽は 644×1000 で、**画面の51%しか使っていない**

  ★★ やっていること（4つ）★★
    ① 背景を **縁からの塗りつぶし** で消す
       ── 色で抜かない。樽の黒(#1a〜#2a)と背景の灰(#303030)は近いので、
          色だけで抜くと **樽に穴があく**
    ② 中身で切り出して、真ん中へ入れ直す
       ── Minecraft のアイテムの絵は画面いっぱいに描く。
          51%のままだと 16画素に潰した時にただの塊になる
    ③ 64×64 へ縮める（BOX＝格子の平均）
       ── 16/32/48/64 を並べて目で見て決めた。
          16 は「OIL」が消えて塊になる。64 は樽と分かり、文字も読める。
          64 は護符・聖杯（確定した2枚）と同じ大きさでもある
    ④ 縁の半端なアルファを **0 か 255 に寄せる**
       ── item/generated は絵を押し出して立体にするので、
          半透明の縁が残ると ひげのような欠けが出る

  動かし方:  cd clientmod && python tools/sekiyu_e.py
"""
import hashlib
import io
import json
import os

import numpy as np
from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(KOKO)

NOUHIN = os.path.join(NE, 'nouhin', 'sekiyu')
MOTO = os.path.join(NOUHIN, 'moto', 'sekiyu_moto.webp')
SAKI = os.path.join(NOUHIN, 'assets', 'jidaiui', 'textures', 'item', 'sekiyu.png')

OOKISA = 64            # 出来上がりの一辺
YOHAKU = 1             # 端に空ける画素
HAIKEI_SHIKII = 26     # 背景と同じ色と見なす幅（0〜255）
ALPHA_SHIKII = 128     # これ未満を透明、以上を不透明にする


def haikei_nuku(im):
    """背景を透明にする。

    ★★ 元の絵にアルファがあれば、それをそのまま使う ★★
      2026-09-20 に踏んだ不具合: いただいた2枚目は**最初から背景が透明**だったのに、
      ここで convert('RGB') してアルファを捨てていた。
      透明な所の RGB は (0,0,0) なので、**四隅が黒**になり、
      「黒に近い所は背景」と塗りつぶして **樽の黒い部分まで削った**
      （見える面積が 50% → 31% に減って気づいた）。
      アルファがある絵は、描いた人が背景を決めている。こちらで決め直さない。

    ★ アルファが無い絵は、**縁からつながっているか**で決める。
      色で抜くと、樽の黒(#1a〜#2a)と背景の灰(#303030)が近すぎて樽に穴があく。
    """
    if im.mode in ('RGBA', 'LA') or 'transparency' in im.info:
        b = np.asarray(im.convert('RGBA')).astype(int)
        suke = (b[:, :, 3] == 0).mean()
        if suke >= 0.05:
            print('  元の絵にアルファあり（透明 %.0f%%）。そのまま使う' % (suke * 100))
            return Image.fromarray(b.astype(np.uint8), 'RGBA')
        print('  元の絵にアルファはあるが、透明な所がほとんど無い（%.1f%%）。'
              '縁から抜く' % (suke * 100))
    a = np.asarray(im.convert('RGB')).astype(int)
    h, w, _ = a.shape
    chikai = np.abs(a - a[0, 0]).max(axis=2) <= HAIKEI_SHIKII

    mita = np.zeros((h, w), bool)
    tsumi = []
    for y, x in ([(0, x) for x in range(w)] + [(h - 1, x) for x in range(w)]
                 + [(y, 0) for y in range(h)] + [(y, w - 1) for y in range(h)]):
        if chikai[y, x] and not mita[y, x]:
            mita[y, x] = True
            tsumi.append((y, x))
    while tsumi:
        y, x = tsumi.pop()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and not mita[ny, nx] and chikai[ny, nx]:
                mita[ny, nx] = True
                tsumi.append((ny, nx))
    return Image.fromarray(
        np.dstack([a, np.where(mita, 0, 255)]).astype(np.uint8), 'RGBA')


def shiageru(im):
    """切り出して真ん中へ入れ、縮めて、縁のアルファを寄せる。"""
    a = np.asarray(im)
    ys, xs = np.where(a[:, :, 3] > 0)
    kiri = im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))

    naka = OOKISA - YOHAKU * 2
    w, h = kiri.size
    bai = min(naka / w, naka / h)
    ko = kiri.resize((max(1, round(w * bai)), max(1, round(h * bai))), Image.BOX)

    dai = Image.new('RGBA', (OOKISA, OOKISA), (0, 0, 0, 0))
    dai.paste(ko, ((OOKISA - ko.width) // 2, (OOKISA - ko.height) // 2))

    # ★ 半端なアルファを 0 か 255 に寄せる（押し出しのひげを消す）
    b = np.asarray(dai).copy()
    b[:, :, 3] = np.where(b[:, :, 3] >= ALPHA_SHIKII, 255, 0)
    # ★ 透明にした画素の色は 0 にそろえる。残しておくと、
    #   PNG の圧縮のされ方が環境で変わり、2回 動かして byte が揃わないことがある。
    b[b[:, :, 3] == 0] = 0
    return Image.fromarray(b, 'RGBA')


def main():
    if not os.path.exists(MOTO):
        raise SystemExit('[中止] 元の絵がありません: %s' % MOTO)

    im = Image.open(MOTO)
    print('元: %s %s %s' % (im.format, im.size, im.mode))
    dekita = shiageru(haikei_nuku(im))

    os.makedirs(os.path.dirname(SAKI), exist_ok=True)
    dekita.save(SAKI, 'PNG', optimize=False)

    nama = open(SAKI, 'rb').read()
    h = hashlib.sha256(nama).hexdigest().upper()
    a = np.asarray(dekita)
    miru = a[:, :, 3] > 0
    iro = len(np.unique(a[miru][:, :3], axis=0)) if miru.any() else 0

    # ★ 納品の控え（sha256 を残す。組み込みの時に照合する）
    io.open(os.path.join(NOUHIN, 'manifest.json'), 'w', encoding='utf-8',
            newline='\n').write(json.dumps({
                'moto': os.path.relpath(MOTO, NOUHIN).replace('\\', '/'),
                'files': [{'file': 'assets/jidaiui/textures/item/sekiyu.png',
                           'category': 'item_texture',
                           'size': [OOKISA, OOKISA],
                           'sha256': h}],
            }, ensure_ascii=False, indent=2) + '\n')

    print('作りました: %s' % os.path.relpath(SAKI, NE))
    print('  %d×%d / 中身 %.0f%% / 色 %d / sha256 %s'
          % (OOKISA, OOKISA, miru.mean() * 100, iro, h[:16]))
    print('  ★ 半透明の画素: %d 個（0 であること）'
          % int(((a[:, :, 3] > 0) & (a[:, :, 3] < 255)).sum()))

    # ── ★★ 形が削れていないかを、自分で確かめる ★★ ──────────
    #   2026-09-20 に踏んだ不具合の見張り。
    #   元の絵にアルファがあるのに縁から塗りつぶすと、
    #   透明な所の RGB が (0,0,0) なので【樽の黒い部分まで消える】。
    #   その時は見える面積が 49% → 31% に落ちた。
    #   元の絵の影と出来上がりの影を、同じ大きさに縮めて突き合わせる。
    if im.mode in ('RGBA', 'LA') or 'transparency' in im.info:
        moto_a = np.asarray(im.convert('RGBA'))[:, :, 3] >= ALPHA_SHIKII
        ys, xs = np.where(moto_a)
        kiri = Image.fromarray(moto_a.astype(np.uint8) * 255).crop(
            (xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
        naka = OOKISA - YOHAKU * 2
        bai = min(naka / float(kiri.width), naka / float(kiri.height))
        ko = np.asarray(kiri.resize((max(1, round(kiri.width * bai)),
                                     max(1, round(kiri.height * bai))),
                                    Image.BOX)) >= 128
        chigai = abs(int(ko.sum()) - int(miru.sum())) / float(max(1, ko.sum()))
        print('  ★ 元の絵と形が合っているか: ずれ %.1f%%' % (chigai * 100))
        if chigai > 0.05:
            raise SystemExit(
                '[中止] 出来上がりの形が元の絵と %.0f%% 違います。'
                '  背景の抜き方が元の絵を削っている可能性があります'
                '（透明な所の RGB が黒だと、黒い部分ごと消える）。' % (chigai * 100))


if __name__ == '__main__':
    main()
