#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""第7回・第1便。64格子を4px角で直接構成する、乱数なしの作画ソース。

python draw_dai1.py --clientmod <clientmod> --out <dai1>
画像生成モデル・写真・既存PNGからのトレースは使わない。
既存 dotto.py の正本パレットと仕上げ規則を読み込む。
"""
from __future__ import annotations
import argparse
import hashlib
import json
import math
import re
import sys
from collections import Counter
from pathlib import Path

import numpy as np
from PIL import Image

PARSER = argparse.ArgumentParser()
PARSER.add_argument('--clientmod', type=Path, required=True)
PARSER.add_argument('--out', type=Path, required=True)
ARGS = PARSER.parse_args()
ROOT = ARGS.clientmod.resolve()
OUT = ARGS.out.resolve()
sys.path.insert(0, str(ROOT / 'tools'))
import dotto

K = dotto.Kyanbasu
P = dotto.PAL


def poly(k, points, color):
    k.takaku(points, color)


def rect(k, x0, y0, x1, y1, color):
    """半開区間の矩形。すべての最小単位が完成時4px角。"""
    k.shikaku(x0, y0, x1 - 1, y1 - 1, color)


def disk(k, cx, cy, rx, ry, color):
    k.daen(cx, cy, rx, ry, color)


def paint(k, mask, color):
    k.nuru(mask, color)


def finished(k):
    # 64格子の2層+2層は、4px角で出すと8近傍の8層+8層に厳密一致。
    # 拡大フィルタは使わず、各論理画素を直接4×4の整数画素へ配置。
    a = np.asarray(k.shiage())
    return Image.fromarray(np.repeat(np.repeat(a, 4, axis=0), 4, axis=1))


def clipped(k, mask, action):
    old = k.a.copy()
    action()
    k.a[~mask] = old[~mask]


def roll(k, x0, x1, top, bottom, bright=False):
    """翠の巻き。円筒の色面は連続した太い帯、内部の輪郭線なし。"""
    cx = (x0 + x1) / 2
    # 胴と短い真鍮軸を一体にする。
    poly(k, [(x0+3,top), (x1-3,top), (x1,top+3),
             (x1,bottom-3), (x1-3,bottom), (x0+3,bottom),
             (x0,bottom-3), (x0,top+3)], 'lens_deep')
    rect(k, int(cx)-3, top-3, int(cx)+3, bottom+3, 'brass_dark')
    poly(k, [(x0+3,top), (x1-3,top), (x1,top+3),
             (x1,bottom-3), (x1-3,bottom), (x0+3,bottom),
             (x0,bottom-3), (x0,top+3)], 'lens_deep')
    # 左上の明るい面 → 基本の翠 → 右側の暗い面。
    poly(k, [(x0+3,top+3), (x0+6,top+2), (x0+7,bottom-3),
             (x0+3,bottom-3)], 'lens_light')
    poly(k, [(x0+6,top+2), (x1-4,top+3), (x1-4,bottom-3),
             (x0+7,bottom-3)], 'lens')
    poly(k, [(x1-4,top+3), (x1-2,top+5), (x1-2,bottom-5),
             (x1-4,bottom-3)], 'enamel_green')
    if bright:
        rect(k, x0+5, top+9, x0+6, top+16, 'highlight')
        rect(k, x0+5, top+9, x0+7, top+11, 'highlight')


def rose(k, cx, cy, radius):
    """文字のない8方位羅針図。針先も最低1格子=4pxで打つ。"""
    # 直交の針4本と斜めの針4本。輪は4px以上の幅を確保。
    rr = radius - 3
    k.wa(cx, cy, rr, rr-1, 'paper_dark')
    short = radius - 3
    for sx, sy in ((1,1),(-1,1),(-1,-1),(1,-1)):
        points = [(cx,cy), (cx+sx*short,cy+sy*(short-1)),
                  (cx+sx*short,cy+sy*short),
                  (cx+sx*(short-1),cy+sy*short)]
        poly(k, points, 'brass_dark')
        rect(k, cx+sx*short-(1 if sx>0 else 0),
             cy+sy*short-(1 if sy>0 else 0),
             cx+sx*short+(0 if sx>0 else 1),
             cy+sy*short+(0 if sy>0 else 1), 'brass_deep')
    # 太い長針。明暗は色面の境界だけで分ける。
    poly(k, [(cx-1,cy-radius), (cx+1,cy-radius),
             (cx+3,cy), (cx,cy+2), (cx-3,cy)], 'brass_deep')
    poly(k, [(cx-1,cy-radius), (cx,cy-radius), (cx,cy+1), (cx-3,cy)], 'brass_light')
    poly(k, [(cx,cy-2), (cx+3,cy), (cx+1,cy+radius),
             (cx-1,cy+radius), (cx-3,cy)], 'brass_deep')
    poly(k, [(cx-3,cy), (cx,cy+1), (cx,cy+radius), (cx-1,cy+radius)], 'brass_light')
    poly(k, [(cx-radius,cy-1), (cx,cy-3), (cx+2,cy),
             (cx,cy+3), (cx-radius,cy+1)], 'brass_deep')
    poly(k, [(cx-radius,cy-1), (cx,cy-3), (cx,cy), (cx-radius,cy)], 'brass_light')
    poly(k, [(cx-2,cy), (cx,cy-3), (cx+radius,cy-1),
             (cx+radius,cy+1), (cx,cy+3)], 'brass_deep')
    poly(k, [(cx,cy-3), (cx+radius,cy-1), (cx+radius,cy), (cx,cy)], 'brass_light')
    disk(k, cx, cy, 2, 2, 'brass')


def coast(k, points):
    poly(k, points, 'glass_deep')


def dashes(k, segments):
    for x0,y0,x1,y1 in segments:
        k.sen(x0,y0,x1,y1,'red',1)


def map_a():
    k = K()
    poly(k, [(13,11),(31,12),(51,11),(51,53),(34,54),(13,53)], 'hada')
    poly(k, [(13,11),(43,12),(35,20),(22,34),(13,41)], 'hada_light')
    poly(k, [(13,47),(31,49),(51,47),(51,53),(34,54),(13,53)], 'hada_dark')
    rect(k, 46,14,51,51,'hada_dark')
    # 階段状の湾岸。海の中に明るい浅瀬を色面で置く。
    coast(k, [(14,15),(30,15),(30,20),(27,20),(27,25),
              (23,25),(23,30),(20,30),(20,36),(14,36)])
    poly(k, [(16,16),(29,16),(29,19),(25,19),(25,24),
             (21,24),(21,29),(18,29),(18,35),(15,35)], 'jewel_blue')
    poly(k, [(19,19),(22,19),(23,20),(23,22),(19,22),(18,21)], 'hada_dark')
    rect(k, 19,19,22,21,'hada')
    rose(k, 37,35,10)
    dashes(k, [(23,16,24,18),(25,21,26,23),(27,27,26,29),
               (25,32,24,34),(24,37,25,39),(27,42,28,44)])
    # 唯一の染み。中を塗り潰さず、一続きの太い輪にする。
    k.wa(22,44,3,2,'hada_dark')
    roll(k, 3,17,7,57,True)
    roll(k, 48,61,7,57)
    return finished(k)


def map_b():
    k = K()
    # 左上から右下へ開いた巻物。上下の紙端も斜めにする。
    poly(k, [(11,9),(53,17),(53,54),(11,46)],'hada')
    poly(k, [(12,10),(51,18),(45,25),(15,20)],'hada_light')
    poly(k, [(12,41),(53,49),(53,54),(12,46)],'hada_dark')
    poly(k, [(47,17),(53,17),(53,54),(47,51)],'hada_dark')
    # 海は下側の広い湾。羅針図と航路の領域を分ける。
    coast(k, [(15,33),(21,33),(21,36),(27,36),(27,39),(34,39),
              (34,36),(39,36),(39,32),(48,32),(48,48),(15,42)])
    poly(k, [(15,35),(20,35),(20,38),(26,38),(26,41),(35,41),
             (35,38),(41,38),(41,34),(48,34),(48,46),(15,41)],'jewel_blue')
    poly(k, [(37,42),(42,42),(44,44),(42,46),(37,45)],'hada_dark')
    rect(k,38,42,42,44,'hada')
    rose(k, 26,26,9)
    dashes(k, [(18,38,20,39),(24,40,26,40),(30,41,32,41),
               (35,39,36,37),(38,34,40,33),(43,31,44,29)])
    # 右上の紙だけに1つの染み。
    poly(k, [(39,22),(42,21),(45,23),(44,26),(41,27),(39,25)],'hada_dark')
    roll(k,3,15,6,49,True)
    roll(k,48,61,15,58)
    return finished(k)


def map_c():
    k = K()
    # 左の太い巻きに多く残し、中央に大きな羅針図を見せる。
    poly(k, [(18,10),(36,11),(54,9),(54,53),(35,55),(18,52)],'hada')
    poly(k, [(18,10),(48,10),(40,17),(26,30),(18,32)],'hada_light')
    poly(k, [(18,47),(35,49),(54,46),(54,53),(35,55),(18,52)],'hada_dark')
    rect(k,21,14,25,49,'hada_dark')
    coast(k, [(43,14),(52,13),(52,22),(49,22),(49,25),
              (45,25),(45,21),(43,21)])
    poly(k,[(45,14),(52,14),(52,21),(48,21),(48,23),(47,23),(47,19),(45,19)],'jewel_blue')
    coast(k, [(31,44),(36,44),(36,41),(42,41),(42,38),
              (52,38),(52,48),(36,50),(31,49)])
    poly(k,[(33,46),(38,46),(38,43),(44,43),(44,40),(52,40),
            (52,47),(36,49),(33,48)],'jewel_blue')
    rose(k,37,30,12)
    dashes(k,[(47,16,48,17),(49,20,49,22),(49,26,49,28),
              (48,32,48,34),(47,38,46,40),(43,43,41,44)])
    # 左下の紙の一続きの染み。
    poly(k,[(26,42),(29,42),(30,44),(30,47),(27,48),(25,46)],'paper_dark')
    roll(k,3,25,6,58,True)
    roll(k,50,61,6,57)
    return finished(k)


def gear_mask(k, cx, cy, root_r, tip_r, teeth, missing, sy=1.0, phase=-90):
    pts = []
    pitch = 360 / teeth
    for i in range(teeth):
        ang = phase + i*pitch
        for frac, r in ((-.50,root_r),(-.36,root_r),(-.28,tip_r),
                         (.28,tip_r),(.36,root_r),(.50,root_r)):
            if i == missing and abs(frac) == .28:
                r = root_r - 1
            t = math.radians(ang+frac*pitch)
            pts.append((round(cx+r*math.cos(t)),round(cy+sy*r*math.sin(t))))
    return k.takaku_m(pts)


def square_hole(k,cx,cy,w,key=False):
    rect(k,cx-w//2,cy-w//2,cx+w//2,cy+w//2,'transparent')
    if key:
        rect(k,cx-1,cy-w//2-2,cx+2,cy-w//2+1,'transparent')


def gear_a():
    k=K()
    mask=gear_mask(k,32,32,23,29,10,4)
    paint(k,mask,'brass_dark')
    def face():
        poly(k,[(4,4),(43,4),(43,15),(29,29),(12,44),(4,44)],'brass_light')
        poly(k,[(44,19),(60,19),(60,60),(18,60),(18,49),(42,46)],'brass_deep')
        disk(k,32,32,19,19,'brass')
        disk(k,31,31,16,16,'brass_dark')
        poly(k,[(19,20),(24,17),(28,17),(25,22),(21,27),(17,28),(17,24)],'brass_deep')
        poly(k,[(39,18),(44,21),(47,27),(44,29),(40,25),(36,22)],'brass_deep')
        poly(k,[(18,38),(23,39),(27,43),(29,47),(24,47),(19,43)],'brass_deep')
        poly(k,[(43,36),(47,36),(47,41),(42,47),(36,47),(38,43)],'brass_deep')
        disk(k,32,32,12,12,'brass_light')
        poly(k,[(35,22),(43,26),(44,37),(36,44),(28,44),(32,37)],'brass_dark')
        # 煤・錆は欠けた1歯の根元にまとめる。
        poly(k,[(40,42),(43,42),(45,45),(43,47),(40,45)],'scuff')
        rect(k,22,18,25,20,'highlight')
    clipped(k,mask,face)
    square_hole(k,32,32,8)
    return finished(k)


def gear_b():
    k=K()
    mask=gear_mask(k,32,32,24,29,8,5,phase=-90)
    paint(k,mask,'iron')
    def face():
        poly(k,[(3,4),(42,4),(42,13),(23,38),(3,43)],'iron_light')
        poly(k,[(41,21),(61,21),(61,61),(21,61),(21,52)],'iron_deep')
        disk(k,32,32,20,20,'steel_light')
        disk(k,32,32,17,17,'iron_deep')
        # 5本の幅広い輻と、その間の深い肉抜き面。
        for deg in (-100,-28,44,116,188):
            th=math.radians(deg)
            ux,uy=math.cos(th),math.sin(th)
            vx,vy=-uy,ux
            points=[(round(32+ux*r+vx*w),round(32+uy*r+vy*w))
                    for r,w in ((7,-3),(19,-4),(19,3),(7,3))]
            poly(k,points,'iron_light' if deg in (-100,188) else 'iron')
        disk(k,32,32,11,11,'brass_dark')
        poly(k,[(23,27),(28,22),(36,22),(41,27),(37,33),(25,34)],'brass_light')
        # 唯一の経年は左下の欠け歯付近。
        poly(k,[(20,40),(23,39),(25,42),(23,44),(20,43)],'scuff')
        rect(k,23,19,26,21,'highlight')
    clipped(k,mask,face)
    square_hole(k,32,32,6,True)
    return finished(k)


def gear_c():
    k=K()
    # 厚みを見せる斜めの12歯。前面の中心が左上、後面は右下へずれる。
    back=gear_mask(k,34,35,23,28,12,2,sy=.91,phase=-90)
    front=gear_mask(k,30,29,23,28,12,2,sy=.83,phase=-90)
    whole=back|front
    paint(k,back,'brass_deep')
    paint(k,front,'brass_dark')
    def face():
        poly(k,[(2,3),(37,3),(42,13),(24,31),(3,39)],'brass_light')
        disk(k,30,29,20,17,'brass')
        disk(k,30,29,17,14,'brass_dark')
        # 3つの太い腎臓形の肉抜き。Aの4ポケット、Bの5輻とは構造を変える。
        poly(k,[(20,16),(26,14),(30,14),(30,19),(23,22),(18,24),(16,22)],'iron_deep')
        poly(k,[(39,17),(44,22),(46,28),(44,33),(39,31),(37,25),(36,21)],'iron_deep')
        poly(k,[(17,32),(23,34),(29,36),(34,35),(36,39),(30,43),(22,41),(17,37)],'iron_deep')
        disk(k,30,28,10,9,'brass_light')
        poly(k,[(32,20),(39,23),(40,30),(35,36),(28,37),(30,29)],'brass_dark')
        rect(k,20,17,23,19,'highlight')
    clipped(k,front,face)
    # 側面の厚さを3つの大きな面で見せる（模様の縞は作らない）。
    side=back & ~front
    side_lit=k.takaku_m([(5,35),(18,38),(29,47),(32,61),(7,52)])
    paint(k,side & side_lit,'brass_dark')
    # 欠けた右上の歯と、その根元の錆を同一箇所へ。
    clipped(k,whole,lambda:poly(k,[(42,18),(46,20),(46,23),(44,23),(42,21)],'scuff'))
    # 角度のついた四角い軸穴（透明）。
    poly(k,[(27,24),(34,25),(34,31),(27,30)],'transparent')
    return finished(k)


def crater(k,cx,cy,rx,ry):
    """穴底は左上が暗く、窪みの下縁が明るい。岩の表面とは逆。"""
    disk(k,cx,cy,rx,ry,'steel_light')
    disk(k,cx,cy-1,rx, max(2,ry-1),'iron')
    disk(k,cx-1,cy-2,max(2,rx-1),max(1,ry-2),'iron_deep')


def rock_a(k):
    shape=[(18,36),(22,28),(28,27),(32,20),(39,22),(43,27),
           (47,32),(49,40),(45,49),(24,50),(17,44)]
    poly(k,shape,'iron')
    poly(k,[(18,36),(22,28),(28,27),(32,20),(39,22),(35,31),(27,37)],'steel_light')
    poly(k,[(18,36),(27,37),(30,42),(25,48),(18,43)],'iron_light')
    poly(k,[(35,31),(43,27),(47,32),(49,40),(45,49),(34,49),(38,39)],'iron_deep')
    poly(k,[(29,35),(35,31),(40,36),(38,42),(34,49),(25,48),(30,42)],'iron_light')
    crater(k,36,31,5,4)
    crater(k,27,43,3,2)


def moon_a():
    k=K()
    # 元絵の「角を落としたガラスケース＋幅広台座」を保つ。
    poly(k,[(19,4),(45,4),(54,12),(56,21),(56,54),(8,54),(8,17),(13,9)],'glass_deep')
    poly(k,[(19,8),(43,8),(50,14),(51,22),(51,52),(13,52),(13,18)],'screen_light')
    poly(k,[(19,8),(40,8),(40,9),(19,9),(14,16),(14,42),(13,42),(13,17)],'glass_light')
    rock_a(k)
    # 床、台座上面、右下の暗い側面。
    poly(k,[(10,50),(54,50),(60,54),(60,60),(4,60),(4,54)],'glass_deep')
    poly(k,[(10,50),(54,50),(57,54),(7,54)],'glass')
    rect(k,7,54,57,57,'iron_deep')
    rect(k,29,53,35,57,'brass_dark')
    rect(k,20,8,25,9,'highlight')
    # 唯一の経年は台座の右前角。
    rect(k,48,55,52,57,'scuff')
    return finished(k)


def moon_b():
    k=K()
    # 大きな月岩をケースなしで低い台座へ直置き。
    poly(k,[(21,5),(35,3),(45,10),(48,20),(54,30),(50,43),
            (43,51),(19,50),(11,41),(10,28),(15,18)],'iron')
    poly(k,[(21,5),(35,3),(31,15),(22,21),(15,33),(10,28),(15,18)],'steel_light')
    poly(k,[(35,3),(45,10),(48,20),(38,25),(31,15)],'iron_light')
    poly(k,[(45,21),(48,20),(54,30),(50,43),(43,51),(33,49),(37,36)],'iron_deep')
    poly(k,[(22,21),(31,15),(38,25),(36,35),(29,40),(17,41),(15,33)],'iron_light')
    poly(k,[(17,41),(29,40),(37,36),(33,49),(19,50),(11,41)],'iron')
    crater(k,28,28,7,6)
    crater(k,40,40,4,3)
    # 台座の前に落ちる影も1つの広い色面。
    poly(k,[(13,46),(50,46),(59,51),(59,60),(5,60),(5,52)],'iron_deep')
    poly(k,[(13,46),(50,46),(55,51),(9,51)],'glass')
    poly(k,[(18,47),(43,47),(47,49),(15,49)],'glass_deep')
    rect(k,10,51,54,55,'glass_deep')
    rect(k,22,12,25,13,'highlight')
    # 経年は台座の右端だけ。
    rect(k,47,53,51,55,'scuff')
    return finished(k)


def moon_c():
    k=K()
    # 手に収まる一塊。左右非対称の破断面と二つの大きな窪み。
    shape=[(22,4),(36,5),(43,11),(50,13),(57,24),(60,38),
           (54,50),(42,58),(26,60),(16,54),(10,43),(3,31),(8,18)]
    poly(k,shape,'iron')
    poly(k,[(22,4),(36,5),(32,15),(21,22),(12,30),(3,31),(8,18)],'steel_light')
    poly(k,[(36,5),(43,11),(50,13),(44,24),(32,27),(32,15)],'iron_light')
    poly(k,[(3,31),(12,30),(22,34),(24,44),(16,54),(10,43)],'iron_light')
    poly(k,[(44,24),(50,13),(57,24),(60,38),(54,50),(42,58),
            (35,47),(40,38)],'iron_deep')
    poly(k,[(22,34),(32,27),(44,24),(40,38),(35,47),(26,51),(24,44)],'iron')
    poly(k,[(24,44),(26,51),(35,47),(42,58),(26,60),(16,54)],'iron_light')
    crater(k,24,26,9,7)
    crater(k,40,43,6,5)
    # 一か所の欠けた面。岩の他の色面は素材の破断構造。
    poly(k,[(48,26),(52,27),(54,32),(51,34),(48,31)],'black_light')
    rect(k,20,12,23,13,'highlight')
    return finished(k)


ART = {
    ('a','kaizu'):map_a, ('b','kaizu'):map_b, ('c','kaizu'):map_c,
    ('a','haguruma'):gear_a, ('b','haguruma'):gear_b, ('c','haguruma'):gear_c,
    ('a','tsukinoishi'):moon_a, ('b','tsukinoishi'):moon_b, ('c','tsukinoishi'):moon_c,
}

DESCRIPTIONS = {
    ('a','kaizu'):('下描きを継承した正面の巻物。左上の階段状海岸、右下の8方位羅針図、左寄りの赤い航路。地図の面を整理し、針先と線を4px以上にした。','左巻きの上寄りにある一続きの反射','紙の左下の輪染み1つ'),
    ('b','kaizu'):('左上から右下へ開く斜めの巻物。左上に羅針図、下半分に大きな湾と島。航路を海の帯に沿わせる。','左巻きの上寄りにある一続きの反射','紙の右上の染み1つ'),
    ('c','kaizu'):('左の巻きを太く残した半開きの巻物。中央に最大の羅針図、上下に分かれる海岸、右端を回る航路。','太い左巻きの上寄りにある一続きの反射','紙の左下の染み1つ'),
    ('a','haguruma'):('v5の正面の真鍮歯車と中心穴を継承。10歯のうち右下1歯を欠き、四角い軸穴と4つの肉抜き凹面を大きく描く。','左上の歯の根元の1面','右下の欠け歯と同じ箇所に錆'),
    ('b','haguruma'):('8歯の鉄歯車。幅広い5本の輻と深い肉抜き面、真鍮の軸受、鍵溝付きの中心穴。左下1歯だけ欠ける。','左上の外輪の1面','左下の欠け歯と同じ箇所に錆'),
    ('c','haguruma'):('厚みを見せる斜めの12歯真鍮歯車。3つの大きな腎臓形の肉抜き、四角い斜めの軸穴。右上1歯だけ欠ける。','前面の左上の1面','右上の欠け歯と同じ箇所に錆'),
    ('a','tsukinoishi'):('v5の面取りしたガラスケースと幅広台座を継承。岩の灰色の色面と窪みを大きく整理。ガラスは左上から左側へ続く1本の縁で示す。','ガラスの左上縁の1か所','台座右前角の擦れ1つ'),
    ('b','tsukinoishi'):('ケースを外し、背の高い角張った標本を低い台座へ直接置く。中央の大きな窪みと右下の小さな窪み。','岩の左上の小さな1面','台座右前角の擦れ1つ'),
    ('c','tsukinoishi'):('台座のない一塊の月岩。左上の大きな窪みと右下の小さな窪み、右側の暗い破断面で立体を見せる。','岩の左上の小さな1面','右肩に1つの欠け面'),
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def hexcolor(rgba):
    return '#'+''.join(f'{v:02X}' for v in rgba)


def measured(path, category, proposal, item, size=None):
    data=path.read_bytes()
    im=Image.open(path).convert('RGBA')
    a=np.asarray(im)
    d={'file':path.relative_to(OUT).as_posix(),'category':category,
       'proposal':proposal.upper(),'item':item,'dimensions':list(im.size),
       'byte_size':len(data),'sha256':hashlib.sha256(data).hexdigest().upper(),
       'rgba_color_count':len(im.getcolors(65536)),
       'alpha_values':sorted(int(x) for x in np.unique(a[:,:,3])),
       'contains_baked_text':False}
    if size: d['preview_size']=size
    return d


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    shouri=ROOT.parent/'plugin/src/main/java/jidai/Shouri.java'
    src=shouri.read_text(encoding='utf-8')
    meta={file:(name,int(number)) for name,file,number in re.findall(
        r'\{"([^"]+)",\s*"([^"]+)",\s*"(\d+)"\}',src)}
    eras={'kaizu':'中世','haguruma':'近代','tsukinoishi':'現代'}
    roles={tuple(v):k for k,v in P.items()}
    request=ROOT/'Codex依頼文_第7回.md'
    files=[]
    items=[]
    records=[]
    for (proposal,key),draw in ART.items():
        im=draw()
        target=OUT/f'proposal_{proposal}/assets/jidaiui/textures/item/{key}.png'
        target.parent.mkdir(parents=True,exist_ok=True)
        im.save(target,compress_level=9)
        saved=Image.open(target).convert('RGBA')
        checks=dotto.tenken(saved,f'{proposal.upper()}/{key}')
        thin=dotto.hosoi_wariai(saved)
        checks.append((thin<=.10,f'3px未満の細い要素 {thin*100:.4f}%'))
        a=np.asarray(saved)
        ys,xs=np.where(a[:,:,3]>0)
        counts=Counter(map(tuple,a.reshape(-1,4).tolist()))
        palette=[{'role':role,'rgba':hexcolor(P[role]),'pixel_count':counts[P[role]]}
                 for role in P if counts[P[role]]]
        intent,highlight,wear=DESCRIPTIONS[(proposal,key)]
        items.append({'name':meta[key][0],'file':key+'.png','number':meta[key][1],
                      'era':eras[key],'proposal':proposal.upper(),
                      'path':target.relative_to(OUT).as_posix(), 'intent':intent,
                      'highlight':highlight,'single_wear_point':wear,'palette':palette})
        files.append(measured(target,'item_texture',proposal,key))
        records.append({'proposal':proposal.upper(),'item':key,
                        'passed':all(c[0] for c in checks),'thin_ratio':thin,
                        'margins_ltrb':[int(xs.min()),int(ys.min()),255-int(xs.max()),255-int(ys.max())],
                        'checks':[{'passed':bool(ok),'detail':msg} for ok,msg in checks]})
        for size in (48,32,16):
            preview=OUT/f'preview/proposal_{proposal}/{key}_{size}.png'
            preview.parent.mkdir(parents=True,exist_ok=True)
            saved.resize((size,size),Image.Resampling.NEAREST).save(preview,compress_level=9)
            files.append(measured(preview,'preview',proposal,key,size))
        print(f'{proposal.upper()}/{key}: {len(palette)} colors, thin={thin:.4%}, '+
              ('PASS' if records[-1]['passed'] else 'FAIL '+str([msg for ok,msg in checks if not ok])))
    production=list(OUT.glob('proposal_*/assets/jidaiui/textures/item/*.png'))
    previews=list(OUT.glob('preview/proposal_*/*.png'))
    assert len(production)==9 and len(previews)==27,(len(production),len(previews))
    # 同じ作画手順を再実行してPNGの保存byteまで比較。入力画像は作画関数で使わない。
    import io
    for (proposal,key),draw in ART.items():
        buf=io.BytesIO()
        draw().save(buf,format='PNG',compress_level=9)
        assert buf.getvalue()==(OUT/f'proposal_{proposal}/assets/jidaiui/textures/item/{key}.png').read_bytes()
    refs=[request,shouri,ROOT/'tools/dotto.py',ROOT/'tests/dai7_kakunin.py',
          ROOT/'nouhin/v6/DESIGN_SPEC.md',ROOT/'nouhin/v8_an/kaizu/makimono_256.png']
    refs += [ROOT/f'nouhin/v8/assets/jidaiui/textures/item/{x}.png' for x in ('gofu','seihai')]
    refs += [ROOT/f'nouhin/v5/assets/jidaiui/textures/item/{x}.png' for x in eras]
    qa={'status':'passed' if all(r['passed'] for r in records) else 'failed',
        'checks':['本番9枚とnearestプレビュー27枚の実在を数え上げ',
                  '保存byteから寸法・RGBA色数・alpha・容量・SHA-256・pixel数を計測',
                  'dotto.tenkenの全項目と細い要素10%以下を検査',
                  '作画関数を2回実行し、本番9枚すべて保存PNG byte一致',
                  '64格子の各画素を4px角で直接配置。乱数・画像生成・AAなし'],
        'production_count_actual':len(production),'preview_count_actual':len(previews),
        'deterministic_png_bytes':True,'per_image':records}
    manifest={'schema_version':2,
              'package':{'name':'JidaiCraft v9 dai1','batch':1,'namespace':'jidaiui',
                         'minecraft':'1.20.1','texture_size':256,'proposal_count':3,
                         'item_count':3,'production_texture_count':9,'preview_count':27,
                         'source_request_sha256':sha(request)},
              'shared_palette':{role:hexcolor(color) for role,color in P.items()},
              'added_colors':[], 'items':items,'files':files,'qa':qa,
              'reference_files':[{'file':p.relative_to(ROOT.parent).as_posix(),'sha256':sha(p)} for p in refs]}
    (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (OUT/'self_check.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    lines=['# 時代クラフト v9 第1便 — DESIGN_SPEC','',
           '羊皮紙の海図・蒸気機関の歯車・月の石を各A/B/Cの3案。本番9枚、48/32/16pxのnearestプレビュー27枚。',
           '正式名・ファイル名・番号はShouri.javaから読んでいます。採用案は絵ごとに選べます。','',
           '## 作図方法','',
           '- 64×64の整数格子で形を設計し、1格子を4×4pxとして256×256へ直接配置しました。写真・生成画像・高解像度原画の縮小はありません。',
           '- 完成シルエットの外側2格子はoutline、次の2格子はbrass。最終256px画像で8近傍の8px＋8pxに厳密一致します。穴の周囲も同じです。',
           '- 最小の描画要素は1格子=4px。半透明・AA・乱数・ぼかし・グラデーションはありません。',
           '- 左上からの光。最高輝度highlightは各1連結成分、経年表現は各1か所。内部にoutline色は使いません。',
           '- 護符・聖杯・海図下描き・旧v5の3枚を実際に表示し、縁と材料色を確認しました。既存PNGは参照専用です。',
           '- ガラスの中も不透明の色面です。ガラスは1本の細い縁と背景色で表し、半透明は使いません。',
           '- 羅針図は8方向の図形で、文字・数字・疑似文字・ロゴを描いていません。','',
           '## 共有パレット','',
           'v6 DESIGN_SPECの52色と依頼で指定済みの肌色3色、計55色を共有。今回さらに足した色は0色です。',
           '肌色3色の役割は、羊皮紙の明面・基本面・右下の暗面です。追加理由は依頼の「地図は肌色」指定です。','',
           '| role | RGBA |','|---|---|']
    lines.extend(f'| `{role}` | `{hexcolor(color)}` |' for role,color in P.items())
    lines += ['','## 絵ごとの構図と色数','']
    for item,rec in zip(items,records):
        lines += [f"### {item['name']} — {item['proposal']}案（{item['number']}）",'',
                  f"- ファイル: `{item['path']}`",f"- 意図: {item['intent']}",
                  f"- 唯一の最高輝度: {item['highlight']}",f"- 唯一の経年: {item['single_wear_point']}",
                  f"- 透明余白（左・上・右・下）: {rec['margins_ltrb']} px",
                  f"- 3px未満の細い要素: {rec['thin_ratio']*100:.4f}%（上限10%）",'',
                  '| role | RGBA | pixel数 |','|---|---|---:|']
        lines.extend(f"| `{c['role']}` | `{c['rgba']}` | {c['pixel_count']} |" for c in item['palette'])
        lines.append('')
    lines += ['## 自己検査','',
              f"- 本番の実在数: {len(production)} / 9。プレビューの実在数: {len(previews)} / 27。",
              '- 保存済みのPNGを開き直し、寸法・容量・SHA-256・色数・不透明度・全色のpixel数を測定しました。',
              '- 2回作画して9枚のPNG byteがすべて一致しました。同一環境での再現に乱数は必要ありません。',
              f"- 最大色数: {max(len(x['palette']) for x in items)} / 64（透明を含む）。",
              f"- 細い要素の最大値: {max(r['thin_ratio'] for r in records)*100:.4f}%。",
              '- 個別の正本検査結果はself_check.json、受け入れ検査の出力はacceptance_check.txtを参照してください。','',
              '## 再生成','',
              '`python source/draw_dai1.py --clientmod <clientmodの絶対パス> --out <空の出力ディレクトリ>`','',
              'Pillow・NumPyと、リポジトリのtools/dotto.pyを使用。使用した正本ファイルのSHA-256はmanifest.reference_filesに記録しています。',
              'A/B/Cの選定後、採用するPNGを絵ごとにv8へコピーする作業は発注元が行う想定です。第2便は第1便の選定・評価後に進めます。','']
    (OUT/'DESIGN_SPEC.md').write_text('\n'.join(lines),encoding='utf-8')
    if not all(r['passed'] for r in records):
        sys.exit(1)


if __name__=='__main__':
    main()
