@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================================
echo       FileManager Plus 前端重启脚本
echo       (Windows)
echo ==========================================

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"

REM --- 1. 停止前端服务 ---
echo [1/3] 停止前端服务...

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
    echo 发现前端服务正在运行 (端口 8081)
    taskkill /f /pid %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo 前端服务已停止
    ) else (
        echo 停止前端服务失败，请手动停止
    )
    goto :frontend_stopped
)

echo 前端服务未运行
:frontend_stopped

REM --- 2. 检查停止结果 ---
echo [2/3] 检查前端服务停止状态...

REM 等待 2 秒让服务完全停止
timeout /t 2 /nobreak >nul

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
    echo [警告] 前端服务可能未完全停止，继续重启操作
    goto :frontend_check_done
)
echo [成功] 前端服务已停止
:frontend_check_done

REM --- 3. 启动前端服务 ---
echo [3/3] 启动前端服务...
echo 访问地址: http://localhost:8081
echo 按 Ctrl+C 停止服务
echo.

cd "%SCRIPT_DIR%\frontend"
python -m http.server 8081

pause
