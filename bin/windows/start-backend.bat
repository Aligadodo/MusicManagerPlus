@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================================
echo       FileManager Plus 后端启动脚本
echo       (Windows)
echo ==========================================

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"

REM --- 1. 停止已运行的后端服务 ---
echo [1/2] 停止已运行的后端服务...

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
    echo 发现后端服务正在运行 (端口 8080)
    taskkill /f /pid %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo 后端服务已停止
    ) else (
        echo 停止后端服务失败，请手动停止
    )
    goto :backend_stopped
)

echo 后端服务未运行
:backend_stopped

REM 等待 2 秒让服务完全停止
timeout /t 2 /nobreak >nul

REM --- 2. 启动后端服务 ---
echo.
echo [2/2] 启动 FileManager Plus 后端服务...
echo 服务地址: http://localhost:8080
echo 按 Ctrl+C 停止服务
echo.

"%SCRIPT_DIR%\jdk\bin\java.exe" -Xms512m -Xmx1g -jar "%SCRIPT_DIR%\backend\backend.jar"

pause
