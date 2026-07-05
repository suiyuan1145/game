@echo off
chcp 65001 >nul
title Nebula Game Store - JavaFX

echo =======================================
echo     Nebula Game Store - JavaFX
echo =======================================
echo.

set "JFX_DIR=%~dp0jfx-lib"
set "SRC_DIR=%~dp0src\main\java"
set "RES_DIR=%~dp0src\main\resources"
set "OUT_DIR=%~dp0target\classes"
set "MAIN_CLASS=com.shooter.Main"

java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo 未找到 Java，请安装 JDK 21 或 JDK 17+
    pause
    exit /b 1
)

if not exist "%JFX_DIR%\javafx-controls.jar" (
    echo 未找到 JavaFX 本地库: %JFX_DIR%
    echo 请确认 jfx-lib 目录中存在 javafx-base.jar、javafx-graphics.jar、javafx-controls.jar。
    pause
    exit /b 1
)

echo [1/3] JavaFX 本地库已就绪
echo [2/3] 编译商城代码 ...

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%" >nul 2>&1
mkdir "%OUT_DIR%" >nul 2>&1

dir /s /b "%SRC_DIR%\*.java" > "%TEMP%\nebula-javafiles.txt" 2>nul

javac -encoding UTF-8 -d "%OUT_DIR%" --module-path "%JFX_DIR%" --add-modules javafx.controls,javafx.media @"%TEMP%\nebula-javafiles.txt"
if %ERRORLEVEL% neq 0 (
    echo 编译失败。
    pause
    exit /b 1
)

if exist "%RES_DIR%" xcopy /E /I /Y "%RES_DIR%\*" "%OUT_DIR%\" >nul 2>&1

echo 编译成功
echo [3/3] 启动商城 ...
echo.

java --module-path "%JFX_DIR%" --add-modules javafx.controls,javafx.media -cp "%OUT_DIR%" %MAIN_CLASS%

if %ERRORLEVEL% neq 0 (
    echo 程序已退出，代码: %ERRORLEVEL%
    pause
)
