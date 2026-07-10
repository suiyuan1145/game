@echo off
cd /d "%~dp0"
mvn javafx:run
if errorlevel 1 pause
