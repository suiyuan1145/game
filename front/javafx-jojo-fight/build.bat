@echo off
chcp 65001 >nul
echo ========================================
echo Anime Super Battle Stars XXV - Build
echo ========================================

set "JAVA_HOME=C:\Users\hjk07\.jdks\temurin-24"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using JDK:
"%JAVA_HOME%\bin\java" -version

echo.
echo Step 1: Checking for Maven...
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Maven not found. Downloading Maven...
    set "MAVEN_HOME=%USERPROFILE%\.maven\apache-maven-3.9.9"
    if not exist "%MAVEN_HOME%" (
        mkdir "%USERPROFILE%\.maven" 2>nul
        powershell -Command "& {try { $wc = New-Object System.Net.WebClient; $wc.DownloadFile('https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip', '%TEMP%\maven.zip'); Write-Output 'Downloaded Maven' } catch { Write-Output 'Failed: ' + $_.Exception.Message }}"
        if exist "%TEMP%\maven.zip" (
            powershell -Command "& {Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%USERPROFILE%\.maven' -Force; Write-Output 'Extracted Maven'}"
        ) else (
            echo WARNING: Could not download Maven. Trying alternative approach...
            goto :direct_compile
        )
    )
    if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        set "PATH=%MAVEN_HOME%\bin;%PATH%"
        echo Maven ready.
    ) else (
        echo Maven setup failed.
        goto :direct_compile
    )
)

echo.
echo Step 2: Compiling with Maven...
call mvn clean compile -q
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
    goto :run
) else (
    echo Compilation failed. Trying direct compile...
    goto :direct_compile
)

:direct_compile
echo.
echo Step 2 (alternative): Direct compilation...
set "SRC_DIR=src\main\java"
set "OUT_DIR=target\classes"

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

dir /s /B "%SRC_DIR%\*.java" > sources.txt

"%JAVA_HOME%\bin\javac" --release 21 -d "%OUT_DIR%" @sources.txt 2>compile_errors.txt
if %ERRORLEVEL% EQU 0 (
    echo Direct compilation successful!
    del sources.txt
    goto :run_direct
) else (
    echo Direct compilation failed. Check compile_errors.txt
    type compile_errors.txt
    del sources.txt
    pause
    exit /b 1
)

:run
echo.
echo Step 3: Running the game...
call mvn javafx:run
pause
exit /b 0

:run_direct
echo.
echo Step 3: Running the game (direct)...
echo NOTE: JavaFX runtime is needed. Trying to locate JavaFX...
set "FX_PATH=C:\Users\hjk07\.jdks\temurin-24\lib"
if exist "%FX_PATH%\javafx.base.jar" (
    "%JAVA_HOME%\bin\java" --module-path "%FX_PATH%" --add-modules javafx.controls,javafx.media -cp "%OUT_DIR%" com.fighter.App
) else (
    "%JAVA_HOME%\bin\java" -cp "%OUT_DIR%" com.fighter.App
)
pause
exit /b 0
