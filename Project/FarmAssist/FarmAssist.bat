@echo off
REM ==================================================================
REM  FarmAssist / Sophie  -  double click launcher
REM
REM  Opens a real terminal window first, then compiles and starts the
REM  chat inside it. Windows Terminal is used when it is installed,
REM  because it gives UTF-8 and 24 bit colour with no setup at all.
REM  A plain console window is used when it is not.
REM
REM  Double click this file. Nothing else is needed.
REM ==================================================================
setlocal enabledelayedexpansion

REM The argument "run" is only present once we are already inside the
REM window we opened for ourselves, which stops this looping forever.
if /i "%~1"=="run" goto run

where wt.exe >nul 2>&1
if not errorlevel 1 (
    start "" wt.exe -d "%~dp0." --title "Sophie - FarmAssist" cmd /c call "%~f0" run
    exit /b
)

start "Sophie - FarmAssist" cmd /c call "%~f0" run
exit /b


:run
cd /d "%~dp0"
title Sophie - FarmAssist

REM A legacy console opens at 80 columns, which is too narrow for the
REM 78 column page plus its margin. Windows Terminal is already wide.
if not defined WT_SESSION mode con: cols=110 >nul 2>&1

REM The screen is drawn with box characters, so the console has to be
REM in UTF-8. The old code page is put back on the way out.
for /f "tokens=2 delims=:" %%c in ('chcp') do set OLDCP=%%c
chcp 65001 >nul

if not exist out mkdir out

echo.
echo   Compiling ...

REM Collect every .java file into one quoted list and hand it to javac
REM directly. The quotes are needed because the project folder may
REM contain spaces (for example "2ND YEAR FILES"). We do NOT use a
REM javac @argfile here, because inside an argfile a backslash counts
REM as an escape character and the Windows path would be destroyed.
set SOURCES=
for /r "%~dp0src" %%f in (*.java) do set SOURCES=!SOURCES! "%%f"

javac -encoding UTF-8 -d out !SOURCES!
if errorlevel 1 (
    echo.
    echo   Compilation failed. The errors are listed above.
    chcp !OLDCP! >nul
    echo.
    pause
    exit /b 1
)

cls
REM -Dstdout.encoding tells Java to write UTF-8 to the console, which is
REM what makes the borders, the meters and the logo come out as real
REM characters instead of question marks.
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dfile.encoding=UTF-8 ^
     -cp out ui.ConsoleChat data

chcp !OLDCP! >nul
echo.
pause
