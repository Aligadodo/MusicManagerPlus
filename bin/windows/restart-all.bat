@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================================
echo       FileManager Plus 重启脚本
echo       (Windows)
echo ==========================================

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"

REM --- 1. 停止服务 ---
echo [1/3] 停止现有服务...
if exist "%SCRIPT_DIR%\stop-all.bat" (
    call "%SCRIPT_DIR%\stop-all.bat"
) else (
    echo [警告] 未找到 stop-all.bat 脚本，尝试手动停止服务
    REM 手动停止后端服务
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
        taskkill /f /pid %%a >nul 2>&1
        echo 后端服务已停止
        goto :backend_stopped
    )
    :backend_stopped
    REM 手动停止前端服务
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
        taskkill /f /pid %%a >nul 2>&1
        echo 前端服务已停止
        goto :frontend_stopped
    )
    :frontend_stopped
)

REM --- 2. 检查停止结果 ---
echo [2/3] 检查服务停止状态...

REM 等待 2 秒让服务完全停止
timeout /t 2 /nobreak >nul

REM 检查后端服务是否停止
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
    echo [警告] 后端服务可能未完全停止，继续重启操作
    goto :backend_check_done
)
echo [成功] 后端服务已停止
:backend_check_done

REM 检查前端服务是否停止
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
    echo [警告] 前端服务可能未完全停止，继续重启操作
    goto :frontend_check_done
)
echo [成功] 前端服务已停止
:frontend_check_done

REM --- 3. 启动服务 ---
echo [3/3] 启动服务...
echo 正在启动 FileManager Plus 服务...
echo 服务地址: http://localhost:8080
echo 访问地址: http://localhost:8081
echo 按 Ctrl+C 停止服务
echo.

REM 启动后端服务
if exist "%SCRIPT_DIR%\start-backend.bat" (
    start "FileManager Plus Backend" cmd /k "" "%SCRIPT_DIR%\start-backend.bat"
) else (
    echo [错误] 未找到 start-backend.bat 脚本
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM 等待 3 秒让后端服务启动
timeout /t 3 /nobreak >nul

REM 检查后端服务是否启动成功
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
    echo [成功] 后端服务启动成功 (端口 8080)
    goto :backend_started
)
echo [警告] 后端服务可能启动失败，请检查日志
:backend_started

REM 启动前端服务
if exist "%SCRIPT_DIR%\start-frontend.bat" (
    start "FileManager Plus Frontend" cmd /k "" "%SCRIPT_DIR%\start-frontend.bat"
) else (
    echo [错误] 未找到 start-frontend.bat 脚本
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM 等待 2 秒让前端服务启动
timeout /t 2 /nobreak >nul

REM 检查前端服务是否启动成功
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do (
    echo [成功] 前端服务启动成功 (端口 8081)
    goto :frontend_started
)
echo [警告] 前端服务可能启动失败，请检查日志
:frontend_started

echo.
echo ==========================================
echo 重启操作完成！
echo 服务已启动，请在浏览器中访问 http://localhost:8081
echo 如果需要停止服务，请关闭对应的终端窗口
echo ==========================================
echo 将在 10 秒后自动退出...
timeout /t 10 /nobreak >nul
