# Nebula Game Store - 一键运行脚本 (PowerShell)
# 使用本地 jfx-lib 编译运行 JavaFX 桌面商城

Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "    Nebula Game Store - JavaFX" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

$JFX_DIR = "$PSScriptRoot\jfx-lib"
$SRC_DIR = "$PSScriptRoot\src\main\java"
$RES_DIR = "$PSScriptRoot\src\main\resources"
$OUT_DIR = "$PSScriptRoot\target\classes"
$MAIN_CLASS = "com.shooter.Main"

if (-not (Test-Path "$JFX_DIR\javafx-controls.jar")) {
    Write-Host "[1/3] 未找到 JavaFX 本地库: $JFX_DIR" -ForegroundColor Red
    Write-Host "请确认 jfx-lib 目录中存在 javafx-base.jar、javafx-graphics.jar、javafx-controls.jar。" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/3] JavaFX 本地库已就绪" -ForegroundColor Green
Write-Host "[2/3] 编译商城代码 ..." -ForegroundColor Yellow

Remove-Item $OUT_DIR -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $OUT_DIR -Force | Out-Null

$javaFiles = Get-ChildItem -Path $SRC_DIR -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

if (-not $javaFiles -or $javaFiles.Count -eq 0) {
    Write-Host "未找到 Java 源文件。" -ForegroundColor Red
    exit 1
}

javac -encoding UTF-8 -d "$OUT_DIR" --module-path "$JFX_DIR" --add-modules javafx.controls,javafx.media $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败。" -ForegroundColor Red
    exit 1
}

if (Test-Path $RES_DIR) {
    Copy-Item "$RES_DIR\*" $OUT_DIR -Recurse -Force
}

Write-Host "编译成功" -ForegroundColor Green
Write-Host "[3/3] 启动商城 ..." -ForegroundColor Yellow
Write-Host ""

java --module-path "$JFX_DIR" --add-modules javafx.controls,javafx.media -cp "$OUT_DIR" $MAIN_CLASS

if ($LASTEXITCODE -ne 0) {
    Write-Host "程序已退出，代码: $LASTEXITCODE" -ForegroundColor Gray
}
