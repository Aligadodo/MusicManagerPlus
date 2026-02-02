@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================================
echo       FileManager Plus 一键启动脚本
echo       (Windows)
echo ==========================================

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"

REM 启动后端服务
echo [1/2] 启动后端服务...
start "FileManager Plus Backend" cmd /k "" "%SCRIPT_DIR%\start-backend.bat"

REM 等待 3 秒
timeout /t 3 /nobreak >nul

REM 启动前端服务
echo [2/2] 启动前端服务...
start "FileManager Plus Frontend" cmd /k "" "%SCRIPT_DIR%\start-frontend.bat"

echo.
echo ==========================================
echo 服务已启动！
echo 后端服务地址: http://localhost:8080
echo 前端访问地址: http://localhost:8081
echo 如果需要停止服务，请运行: %SCRIPT_DIR%\stop-all.bat
echo ==========================================
echo 将在 10 秒后自动退出...
timeout /t 10 /nobreak >nul
