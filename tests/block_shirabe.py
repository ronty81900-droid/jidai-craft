# -*- coding: utf-8 -*-
"""ワールド全体を1回だけ舐めて、気になるブロックが在るチャンク数を数える。読むだけ。"""
import glob, os, struct, sys, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import chikei_shiraberu as C

def main():
    w = sys.argv[1]
    kazu = collections.Counter()
    n_chunk = 0
    for path in sorted(glob.glob(os.path.join(w, "region", "*.mca"))):
        b = os.path.basename(path)[2:-4].split(".")
        rx, rz = int(b[0]), int(b[1])
        with open(path, "rb") as f:
            head = f.read(4096)
        for i in range(1024):
            off = struct.unpack(">I", b"\x00" + head[i*4:i*4+3])[0]
            if off == 0:
                continue
            cx, cz = rx*32 + (i % 32), rz*32 + (i // 32)
            ch = C.chunk_yomu(w, cx, cz)
            if ch is None:
                continue
            n_chunk += 1
            mita = set()
            for sec in ch.get("sections", []):
                bs = sec.get("block_states") or {}
                for p in bs.get("palette", []):
                    mita.add(p.get("Name"))
            for na in mita:
                kazu[na] += 1
    print("チャンク数 =", n_chunk)
    print()
    print("--- 鉱石（0でなければならない） ---")
    ore = [k for k in kazu if k and ("_ore" in k or k.endswith("ancient_debris"))]
    if not ore:
        print("  鉱石は1つも無い")
    for k in sorted(ore):
        print("  %-40s %d チャンク" % (k, kazu[k]))
    print()
    print("--- 木・目印になるもの ---")
    for k in ("minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
              "minecraft:gold_block", "minecraft:emerald_block", "minecraft:iron_block",
              "minecraft:diamond_block", "minecraft:furnace", "minecraft:water",
              "minecraft:lava", "minecraft:grass_block", "minecraft:sand"):
        print("  %-32s %d チャンク" % (k, kazu.get(k, 0)))
    print()
    print("--- 出てくるブロックの種類 = %d ---" % len([k for k in kazu if k]))

main()
