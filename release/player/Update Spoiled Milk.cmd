@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0update-spoiled-milk.ps1"
if errorlevel 1 pause
