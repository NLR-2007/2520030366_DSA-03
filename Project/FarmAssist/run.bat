@echo off
REM ------------------------------------------------------------------
REM  Sophie / FarmAssist - compile and run
REM  Just double click this file, or run  run.bat  from the FarmAssist folder
REM ------------------------------------------------------------------
setlocal enabledelayedexpansion
cd /d "%~dp0"

title Sophie - FarmAssist

REM The console draws box characters, so switch it to UTF-8 first. The old
REM code page is put back on the way out. Without this, a classic cmd window
REM shows question marks instead of the borders.
for /f "tokens=2 delims=:" %%c in ('chcp') do set OLDCP=%%c
chcp 65001 >nul

if not exist out mkdir out

echo Compiling ...

REM Collect every .java file into one quoted list and hand it to javac
REM directly. The quotes are needed because the project folder may contain
REM spaces (for example "2ND YEAR FILES"). We do NOT use a javac @argfile
REM here, because inside an argfile a backslash counts as an escape
REM character and the Windows path would be destroyed.
set SOURCES=
for /r "%~dp0src" %%f in (*.java) do set SOURCES=!SOURCES! "%%f"

javac -encoding UTF-8 -d out !SOURCES!
if errorlevel 1 (
    echo.
    echo Compilation failed.
    chcp !OLDCP! >nul
    pause
    exit /b 1
)

cls
REM -Dstdout.encoding tells Java to write UTF-8 to the console, which is what
REM makes the borders, the meters and the logo come out as real characters.
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dfile.encoding=UTF-8 ^
     -cp out ui.ConsoleChat data

chcp !OLDCP! >nul
pause
