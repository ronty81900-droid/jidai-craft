# -*- coding: utf-8 -*-
"""
800版の生成結果が変わっていないことを検査する。

サイズ切り替え(プリセット化)を入れた時に、800版の出力が1バイトも変わって
いないことを保証するための検査。指示書の必須条件。

基準は tests/baseline_800.json (プリセット化する前に取ったハッシュ)。

使い方:  python tests/check_baseline_800.py
終了コード: 0=一致 / 1=不一致 / 2=ファイルが無い
"""

import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASELINE = ROOT / "tests" / "baseline_800.json"
OUT = ROOT / "map" / "800"

if not BASELINE.exists():
    print(f"[環境エラー] 基準が無い: {BASELINE}")
    sys.exit(2)

base = json.loads(BASELINE.read_text(encoding="utf-8"))
print("=== 800版の生成結果が変わっていないか ===")
print(f"基準: {BASELINE.relative_to(ROOT)}")
print(f"対象: {OUT.relative_to(ROOT)}\n")

ng = 0
for name, want in sorted(base.items()):
    p = OUT / name
    if not p.exists():
        print(f"  [NG ] {name:26s} ファイルが無い")
        ng += 1
        continue
    got = hashlib.sha256(p.read_bytes()).hexdigest()
    same = got == want
    ng += 0 if same else 1
    print(f"  [{'OK ' if same else 'NG '}] {name:26s} {got[:16]}"
          + ("" if same else f"  ≠ 基準 {want[:16]}"))

print()
if ng:
    print(f"NG: {ng} 件が基準と違う。800版の生成結果が変わってしまっている。")
    sys.exit(1)
print("OK: 800版の生成結果は1バイトも変わっていない。")
sys.exit(0)
