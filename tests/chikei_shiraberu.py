# -*- coding: utf-8 -*-
"""
ワールドの地形を座標で確かめる (Minecraft Java 1.21.10 / Anvil)

  ・指定した座標の地表の高さを出す
  ・指定したブロックが、どのチャンクに在るかを出す

「拠点のパッド(Y=100 で平坦)が本当にそこに在るか」を測るための道具。
読むだけ。書き込みは一切しない。
"""
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nbt_yomu import Yomi

KUUKI = ("minecraft:air", "minecraft:cave_air", "minecraft:void_air",
         "minecraft:water")


def chunk_yomu(world, cx, cz):
    """チャンク1つの NBT を返す。無ければ None。"""
    rx, rz = cx >> 5, cz >> 5
    path = os.path.join(world, "region", "r.%d.%d.mca" % (rx, rz))
    if not os.path.exists(path):
        return None
    with open(path, "rb") as f:
        head = f.read(4096)
        i = ((cx & 31) + (cz & 31) * 32) * 4
        off = struct.unpack(">I", b"\x00" + head[i:i + 3])[0]
        num = head[i + 3]
        if off == 0 or num == 0:
            return None
        f.seek(off * 4096)
        n = struct.unpack(">I", f.read(4))[0]
        comp = f.read(1)[0]
        data = f.read(n - 1)
    if comp == 1:
        import gzip
        data = gzip.decompress(data)
    elif comp == 2:
        data = zlib.decompress(data)
    y = Yomi(data)
    t = y.num("b", 1)
    y.moji()
    return y.atai(t)


def takasa(world, x, z):
    """(x,z) の地表の高さ。見つからなければ None。"""
    ch = chunk_yomu(world, x >> 4, z >> 4)
    if ch is None:
        return None
    lx, lz = x & 15, z & 15
    best = None
    for sec in ch.get("sections", []):
        bs = sec.get("block_states")
        if not bs:
            continue
        pal = bs.get("palette", [])
        if not pal or all(p.get("Name") in KUUKI for p in pal):
            continue
        y0 = sec.get("Y", 0) * 16
        dat = bs.get("data")
        bits = max(4, (len(pal) - 1).bit_length())
        for ly in range(15, -1, -1):
            idx = (ly * 16 + lz) * 16 + lx
            if dat is None:
                pi = 0
            else:
                per = 64 // bits
                w = dat[idx // per]
                pi = (w >> ((idx % per) * bits)) & ((1 << bits) - 1)
            if pi < len(pal) and pal[pi].get("Name") not in KUUKI:
                yy = y0 + ly
                if best is None or yy > best:
                    best = yy
                break
    return best


def sagasu(world, name):
    """そのブロックを持つチャンクの座標を返す。"""
    out = []
    import glob
    for path in glob.glob(os.path.join(world, "region", "*.mca")):
        b = os.path.basename(path)[2:-4].split(".")
        rx, rz = int(b[0]), int(b[1])
        with open(path, "rb") as f:
            head = f.read(4096)
        for i in range(1024):
            off = struct.unpack(">I", b"\x00" + head[i * 4:i * 4 + 3])[0]
            if off == 0:
                continue
            cx, cz = rx * 32 + (i % 32), rz * 32 + (i // 32)
            ch = chunk_yomu(world, cx, cz)
            if ch is None:
                continue
            for sec in ch.get("sections", []):
                bs = sec.get("block_states") or {}
                if any(p.get("Name") == name for p in bs.get("palette", [])):
                    out.append((cx * 16, cz * 16))
                    break
    return out


if __name__ == "__main__":
    w = sys.argv[1]
    if sys.argv[2] == "sagasu":
        for xz in sagasu(w, sys.argv[3]):
            print("  %s が在るチャンク: ブロック座標 x=%d z=%d" % (sys.argv[3], xz[0], xz[1]))
    else:
        for arg in sys.argv[2:]:
            x, z = [int(v) for v in arg.split(",")]
            print("  (%5d,%5d) の地表 Y = %s" % (x, z, takasa(w, x, z)))
