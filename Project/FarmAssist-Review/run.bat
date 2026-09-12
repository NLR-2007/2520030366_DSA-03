@echo off
REM ------------------------------------------------------------------
REM  FarmAssist - Review 1 build
REM  Compiles everything under src into out, then starts the console.
REM  Double click this file, or run  run.bat  from this folder.
REM
REM  Anything typed after run.bat is passed on to the program, so
REM    run.bat --no-color    plain text, no ANSI colour
REM    run.bat --ascii       no box drawing characters
REM ------------------------------------------------------------------
setlocal enabledelayedexpansion
cd /d "%~dp0"

title FarmAssist - Review 1

REM The console is switched to UTF-8 so the box drawing characters come out
REM as drawings and not as mojibake. If the console cannot do it, the program
REM notices (stdout.encoding is no longer UTF-8) and falls back to plain ASCII.
chcp 65001 >nul

if not exist out mkdir out

echo Compiling ...

REM The folder path has spaces in it ("2ND YEAR FILES"), so every source
REM file is quoted and passed to javac directly. A javac @argfile is not
REM used here because a backslash counts as an escape inside one.
set SOURCES=
for /r "%~dp0src" %%f in (*.java) do set SOURCES=!SOURCES! "%%f"

javac -encoding UTF-8 -d out !SOURCES!
if errorlevel 1 (
    echo.
    echo Compilation failed.
    pause
    exit /b 1
)

cls
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp out app.Main data %*
pause
