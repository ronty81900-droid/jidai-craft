# -*- coding: utf-8 -*-
"""
katachi_hikaku.py -- 案A の外形が、いまの絵（jar の v6・64×64 を 2 倍）とどれだけ重なるかを見る。

  ★ なぜ要るか（2026-09-28）
    2回目で「戦車今」「エクスカリバー今」になった。形を変えた案A は、いまの絵に負けた。
    3回目からは、案A を「いまの絵の外形を 2 倍の 128 マスに写して、中を描き込む」にした。
    外形がずれていないかを、数（IoU）と重ねた絵で確かめる。3回目の案A は 0.949〜0.976。

  出すもの: nouhin/v10_an/<絵>/kasane.png（灰 = 両方・赤 = 案A だけ・青 = いまだけ）

  実行: cd clientmod && PYTHONIOENCODING=utf-8 python tools/e_v10/katachi_hikaku.py <絵> [<絵> ...]
"""
import importlib
import io
import os
import sys
import zipfile

import numpy as np
from PIL import Image

KOKO = os.path.dirname(os.path.abspath(__file__))
NE = os.path.dirname(os.path.dirname(KOKO))
sys.path.insert(0, KOKO)


def hakaru(fai):
    an_a = importlib.import_module(fai).an_a
    a = np.array(an_a())[:, :, 3] > 0
    z = zipfile.ZipFile(os.path.join(NE, 'JidaiUI-0.1.0.jar'))
    ima = Image.open(io.BytesIO(z.read('assets/jidaiui/textures/item/%s.png' % fai))).convert('RGBA')
    bai = a.shape[0] // ima.size[0]
    v6 = np.array(ima)[:, :, 3] > 0
    v6 = v6.repeat(bai, 0).repeat(bai, 1)
    iou = (a & v6).sum() / (a | v6).sum()
    e = np.full(a.shape + (3,), 255, np.uint8)
    e[a & v6] = (160, 160, 160)
    e[a & ~v6] = (220, 60, 60)
    e[~a & v6] = (60, 60, 220)
    saki = os.path.join(NE, 'nouhin', 'v10_an', fai, 'kasane.png')
    os.makedirs(os.path.dirname(saki), exist_ok=True)
    Image.fromarray(e).resize((384, 384), Image.NEAREST).save(saki)
    print('%s: IoU %.3f（案A だけ %d マス・いまだけ %d マス）→ %s'
          % (fai, iou, (a & ~v6).sum(), (~a & v6).sum(), saki))
    return iou


if __name__ == '__main__':
    for fai in sys.argv[1:]:
        hakaru(fai)
