@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================================
echo       FileManager Plus 停止脚本
echo       (Windows)
echo ==========================================

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"

REM --- 1. 检查服务状态 ---
echo [1/2] 检查服务运行状态...

REM 检查后端服务 (8080 端口)
set BACKEND_RUNNING=false
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
    echo [发现] 后端服务正在运行 (端口 8080)
    set BACKEND_RUNNING=true
    set BACKEND_PID=%%a
    echo [信息] 后端服务进程 ID: %%a
)

if "%BACKEND_RUNNING%"=="false" (
    echo [未发现] 后端服务未运行 (端口 8080)
)

REM 检查前端服务 (8081 端口)
set FRONTEND_RUNNING=false
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
    echo [发现] 前端服务正在运行 (端口 8081)
    set FRONTEND_RUNNING=true
    set FRONTEND_PID=%%a
    echo [信息] 前端服务进程 ID: %%a
)

if "%FRONTEND_RUNNING%"=="false" (
    echo [未发现] 前端服务未运行 (端口 8081)
)

if "%BACKEND_RUNNING%"=="false" if "%FRONTEND_RUNNING%"=="false" (
    echo [信息] 所有服务均未运行，无需停止。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 0
)

REM --- 2. 停止服务 ---
echo [2/2] 正在停止服务...

REM 停止后端服务
if "%BACKEND_RUNNING%"=="true" (
    echo 正在停止后端服务...
    if defined BACKEND_PID (
        taskkill /f /pid %BACKEND_PID% >nul 2>&1
        if !errorlevel! equ 0 (
            echo 后端服务已停止
        ) else (
            echo 停止后端服务失败，请手动停止
        )
    ) else (
        echo 无法获取后端服务进程 ID，请手动停止
    )
)

REM 停止前端服务
if "%FRONTEND_RUNNING%"=="true" (
    echo 正在停止前端服务...
    if defined FRONTEND_PID (
        taskkill /f /pid %FRONTEND_PID% >nul 2>&1
        if !errorlevel! equ 0 (
            echo 前端服务已停止
        ) else (
            echo 停止前端服务失败，请手动停止
        )
    ) else (
        echo 无法获取前端服务进程 ID，请手动停止
    )
)

REM --- 3. 验证停止结果 ---
echo [验证] 验证服务停止状态...

REM 检查后端服务是否停止
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
    echo [警告] 后端服务可能未完全停止，请手动检查
    goto :backend_check_done
)
echo [成功] 后端服务已停止
:backend_check_done

REM 检查前端服务是否停止
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
    echo [警告] 前端服务可能未完全停止，请手动检查
    goto :frontend_check_done
)
echo [成功] 前端服务已停止
:frontend_check_done

echo.
echo ==========================================
echo 停止操作完成！
echo ==========================================
echo 将在 10 秒后自动退出...
timeout /t 10 /nobreak >nul
