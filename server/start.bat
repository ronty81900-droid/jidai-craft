@echo off
rem =============================================================
rem  時代クラフト MODサーバー (Arclight 1.20.1 / Forge 47.3.22)
rem
rem  このファイルをダブルクリックすると起動します。
rem  止める時は、黒い画面に  stop  と打って Enter。
rem  ★ 右上の × で閉じないでください。書きかけの世界が壊れます。
rem
rem  ★★ コンソールではスラッシュを付けません ★★
rem    黒い画面:  jidai setup
rem    ゲーム内:  /jidai setup
rem
rem  ★ このファイルは Shift-JIS(cp932) + CRLF で保存すること。
rem    UTF-8 で保存すると、コマンドプロンプトが日本語のコメントを
rem    命令と読み違えて 'er...' のようなエラーを出します(実機で発生)。
rem =============================================================
cd /d "%~dp0"

rem --- メモリ ---
rem  このPCは 32GB。50人ぶんで 8GB にしています。
rem  Xms と Xmx を同じにするのは、途中で広げ直す間の引っかかりを無くすため。
rem  足りなければ両方の数字を大きくしてください (12G など)。
set MEM=8G

rem --- ゴミ集めの設定 (Aikar's flags) ---
rem  人が多いサーバーで「たまにカクつく」のを減らす、よく使われる指定です。
rem  意味が分からなくても消さないでください。
rem  ★ 行を分ける(^)と壊れやすいので、1行のままにしてあります。
set FLAGS=-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1

"C:\Program Files\Java\jdk-21.0.10\bin\java.exe" -Xms%MEM% -Xmx%MEM% %FLAGS% -jar arclight-forge-1.20.1-1.0.6.jar --nogui

echo.
echo サーバーが止まりました。何かキーを押すと閉じます。
pause > nul
