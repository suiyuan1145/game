@echo off
echo ========================================
echo   星穹游戏商城 - 启动所有服务
echo ========================================
echo.
echo 启动主商城 (端口 5173)...
start "星穹游戏商城" cmd /c "npm run dev"
echo 启动修仙模拟器 (端口 5174)...
start "修仙模拟器" cmd /c "cd games\cultivation && npm run dev"
echo 启动星河射击战 (端口 5175)...
start "星河射击战" cmd /c "cd games\shooting && npm run dev"
echo.
echo 所有服务已启动！
echo   主商城:     http://127.0.0.1:5173
echo   修仙模拟器: http://127.0.0.1:5174
echo   星河射击战: http://127.0.0.1:5175
echo.
echo 关闭此窗口不会停止游戏服务。
echo 请在各游戏窗口中按 Ctrl+C 停止对应服务。
echo ========================================
pause