@echo off
setlocal
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

set HONTAI=arclight-forge-1.20.1-1.0.6.jar
set IRUBAN=17

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

rem =============================================================
rem  サーバー本体があるか
rem =============================================================
if not exist "%HONTAI%" goto HONTAI_NASHI

rem =============================================================
rem  Java を探す
rem    ★ 絶対パスで書かないこと。版を上げたり、別のパソコンへ渡した
rem      とたんに動かなくなります(2026-08-30 に実際に起きました)。
rem    ★ 「最初に見つかった物」ではなく「17以上だった物」を採ります。
rem      PATH の java が Oracle の中継で、中身が Java 8 のことがあるためです。
rem =============================================================
set JAVA=
set JAVABAN=
call :TAMESU "%JAVA_HOME%\bin\java.exe"
for %%J in (java.exe) do if not "%%~$PATH:J"=="" call :TAMESU "%%~$PATH:J"
call :SAGASU "C:\Program Files\Java"
call :SAGASU "C:\Program Files\Eclipse Adoptium"
call :SAGASU "C:\Program Files\Microsoft\jdk"
call :SAGASU "C:\Program Files\Amazon Corretto"
call :SAGASU "C:\Program Files\Zulu"
call :SAGASU "C:\Program Files\BellSoft"
call :SAGASU "%LOCALAPPDATA%\Programs\Eclipse Adoptium"
if not defined JAVA goto JAVA_NASHI

echo 使う Java: %JAVA%
echo その版  : %JAVABAN%
echo.
"%JAVA%" -Xms%MEM% -Xmx%MEM% %FLAGS% -jar "%HONTAI%" --nogui

echo.
echo サーバーが止まりました。何かキーを押すと閉じます。
pause > nul
exit /b 0

rem =============================================================
rem  :SAGASU <フォルダ>  … その下の各フォルダの bin\java.exe を順に試す
rem =============================================================
:SAGASU
if defined JAVA goto :eof
if not exist "%~1" goto :eof
for /d %%D in ("%~1\*") do call :TAMESU "%%D\bin\java.exe"
goto :eof

rem =============================================================
rem  :TAMESU <java.exe>  … 版が %IRUBAN% 以上なら JAVA に採る
rem    ★ -version の出力は【標準エラー】に出る。
rem    ★ 出力を for /f の中で直接 呼ばない。道に空白があると引用が壊れる。
rem      一度 ファイルへ落としてから読む(実測でここに引っかかった)。
rem =============================================================
:TAMESU
if defined JAVA goto :eof
if "%~1"=="" goto :eof
if not exist "%~1" goto :eof
set BAN=
set SHIRABE=%TEMP%\jidai_java_ban.txt
"%~1" -version 2>"%SHIRABE%" >nul
if not exist "%SHIRABE%" goto :eof
for /f "tokens=3" %%v in ('findstr /i "version" "%SHIRABE%" 2^>nul') do if not defined BAN set BAN=%%v
del "%SHIRABE%" >nul 2>&1
if not defined BAN goto :eof
set BAN=%BAN:"=%
for /f "delims=.-+_ tokens=1" %%a in ("%BAN%") do set OYA=%%a
rem  Java 8 以前は 1.8.0_xxx の形。親が 1 なら古い。
if "%OYA%"=="1" goto :eof
rem  数字でなければ判定できないので使わない
echo %OYA%| findstr /r "^[0-9][0-9]*$" >nul || goto :eof
if %OYA% LSS %IRUBAN% goto :eof
set "JAVA=%~1"
set "JAVABAN=%BAN%"
goto :eof

rem =============================================================
:HONTAI_NASHI
echo.
echo  === サーバー本体が見つかりません ===
echo.
echo  探した物: %HONTAI%
echo  探した所: %~dp0
echo.
echo  このフォルダは「配る人に渡すためのひな型」です。設定だけが入っています。
echo  次の物を自分で用意して、このフォルダに置いてください。
echo.
echo    arclight-forge-1.20.1-1.0.6.jar   サーバー本体
echo    mods\                             TaCZ と Simple Voice Chat
echo    tacz\                             銃のパック
echo    plugins\JidaiCraft-*.jar          時代クラフトのプラグイン
echo    world\                            遊ぶ舞台
echo.
echo  入手先は  外部ファイル.md  に、組み立て方は  README.md  に書いてあります。
echo.
pause > nul
exit /b 1

rem =============================================================
:JAVA_NASHI
echo.
echo  === 使える Java が見つかりません ===
echo.
echo  このサーバーには Java %IRUBAN% 以上が要ります(21 を勧めます)。
echo  古い Java しか入っていない場合も、ここに来ます。
echo.
echo  https://adoptium.net/ から Temurin 21 の JDK を入れてください。
echo  入れたあと、このファイルをもう一度 開けば動きます。
echo.
echo  すでに入れてあるのにここへ来る時は、環境変数 JAVA_HOME に
echo  その置き場(bin の1つ上)を指してから、もう一度 開いてください。
echo.
pause > nul
exit /b 1
