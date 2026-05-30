@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0update-spoiled-milk.ps1"
if errorlevel 1 pause & exit /b 1
runtime\bin\java.exe -jar Spoiled_Milk_Client.jar
if errorlevel 1 pause
