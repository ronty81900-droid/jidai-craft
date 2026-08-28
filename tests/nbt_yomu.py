# -*- coding: utf-8 -*-
"""
ワールドセーブの NBT を読むだけの小さな道具 (Minecraft Java 1.21.10)

level.dat / scoreboard.dat は gzip で固めた NBT。外部ライブラリを入れずに読む。
書き込みは一切しない（読むだけ）。
"""
import gzip
import struct
import sys

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE = 0, 1, 2, 3, 4, 5, 6
BYTES, STRING, LIST, COMPOUND, INTS, LONGS = 7, 8, 9, 10, 11, 12


class Yomi:
    def __init__(self, b):
        self.b = b
        self.i = 0

    def take(self, n):
        v = self.b[self.i:self.i + n]
        self.i += n
        return v

    def num(self, fmt, n):
        return struct.unpack(">" + fmt, self.take(n))[0]

    def moji(self):
        n = self.num("H", 2)
        return self.take(n).decode("utf-8", "replace")

    def atai(self, t, michi=""):
        # ★ 空の TAG_List は要素の種別が 0(END) で入っている。
        #   ここを弾かないと「知らないタグ種別」で落ちる。
        if t == END:
            return None
        if t == BYTE:
            return self.num("b", 1)
        if t == SHORT:
            return self.num("h", 2)
        if t == INT:
            return self.num("i", 4)
        if t == LONG:
            return self.num("q", 8)
        if t == FLOAT:
            return self.num("f", 4)
        if t == DOUBLE:
            return self.num("d", 8)
        if t == BYTES:
            return self.take(self.num("i", 4))
        if t == STRING:
            return self.moji()
        if t == LIST:
            it = self.num("b", 1)
            n = self.num("i", 4)
            return [self.atai(it, michi + "[]") for _ in range(n)]
        if t == COMPOUND:
            d = {}
            while True:
                tt = self.num("b", 1)
                if tt == END:
                    return d
                na = self.moji()
                d[na] = self.atai(tt, michi + "." + na)
        if t == INTS:
            n = self.num("i", 4)
            return [self.num("i", 4) for _ in range(n)]
        if t == LONGS:
            n = self.num("i", 4)
            return [self.num("q", 8) for _ in range(n)]
        raise ValueError("知らないタグ種別: " + str(t) + "  場所=" + michi
                         + "  位置=" + str(self.i))


def yomu(path):
    """gzip でも生でも読めるようにしておく。"""
    raw = open(path, "rb").read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    y = Yomi(raw)
    t = y.num("b", 1)
    y.moji()          # ルートの名前（空文字）
    return y.atai(t)


if __name__ == "__main__":
    import json
    d = yomu(sys.argv[1])

    def keru(o):
        if isinstance(o, bytes):
            return "<" + str(len(o)) + " バイト>"
        return o
    print(json.dumps(d, ensure_ascii=False, indent=1, default=keru)[:6000])
