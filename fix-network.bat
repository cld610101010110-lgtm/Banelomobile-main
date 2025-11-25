@echo off
REM ============================================
REM Banelo Mobile App - Network Fix Launcher
REM ============================================
REM This batch file launches the PowerShell
REM script with Administrator privileges
REM ============================================

echo ============================================
echo Banelo Network Fix - Administrator Launcher
echo ============================================
echo.

REM Check if already running as admin
net session >nul 2>&1
if %errorLevel% == 0 (
    echo Running with Administrator privileges...
    echo.
    echo Launching PowerShell script...
    echo.
    powershell.exe -ExecutionPolicy Bypass -File "%~dp0fix-network.ps1"
) else (
    echo This script requires Administrator privileges.
    echo.
    echo Requesting elevation...
    echo.

    REM Request elevation and run PowerShell script
    powershell.exe -Command "Start-Process powershell.exe -ArgumentList '-ExecutionPolicy Bypass -File \"%~dp0fix-network.ps1\"' -Verb RunAs"
)

pause
