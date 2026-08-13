@echo off
REM ------------------------------------------------------------------
REM  FarmAssist - compile and run
REM  Just double click this file, or run  run.bat  from the FarmAssist folder
REM ------------------------------------------------------------------
setlocal enabledelayedexpansion
cd /d "%~dp0"

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
    pause
    exit /b 1
)

echo.
echo Starting FarmAssist ...
echo.
java -cp out ui.ConsoleChat data
pause
