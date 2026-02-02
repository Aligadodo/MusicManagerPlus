@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================================
echo       FileManager Plus 构建脚本 v2.0
echo       (JDK 21 版本 - Windows)
echo ==========================================

REM =================配置区域=================
REM 请根据你的实际情况修改这些路径！！
REM JDK 21 路径 (指向包含 bin 和 lib 文件夹的 jdk 目录)
set "SOURCE_JDK=C:\Program Files\Java\jdk-21"

REM Flutter SDK 路径
set "FLUTTER_SDK=C:\flutter"

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"

REM 设置项目根目录（bin目录的父目录）
for %%i in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fi"

REM 设置构建输出目录
set "BUILD_OUTPUT=%PROJECT_ROOT%\dist"
REM ==========================================

REM --- 1. 检查环境 ---
echo [1/7] 正在检查构建环境...

REM 检查 JDK 环境
if not exist "%SOURCE_JDK%\bin\java.exe" (
    echo [错误] JDK 路径不存在: "%SOURCE_JDK%\bin\java.exe"
    echo 请修改脚本中的 SOURCE_JDK 变量。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM 检查 Flutter 环境
if not exist "%FLUTTER_SDK%\bin\flutter.bat" (
    echo [错误] Flutter SDK 路径不存在: "%FLUTTER_SDK%\bin\flutter.bat"
    echo 请修改脚本中的 FLUTTER_SDK 变量。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM 检查 Maven 环境
where mvn >nul 2>&1
if !errorlevel! neq 0 (
    echo [错误] Maven 未找到，请确保 Maven 已添加到 PATH。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM 检查 Python 环境
where python >nul 2>&1
if !errorlevel! neq 0 (
    echo [错误] Python 未找到，请确保 Python 已安装。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

echo 构建环境验证通过。

REM --- 2. 清理目录 ---
echo [2/7] 清理旧文件...
if exist "%BUILD_OUTPUT%" (
    rmdir /s /q "%BUILD_OUTPUT%"
)

mkdir "%BUILD_OUTPUT%\backend"
mkdir "%BUILD_OUTPUT%\frontend"
mkdir "%BUILD_OUTPUT%\jdk"

REM --- 3. 构建后端 ---
echo [3/7] 构建后端服务...
cd "%PROJECT_ROOT%"
call mvn clean package -DskipTests
if !errorlevel! neq 0 (
    echo [错误] Maven 打包失败！
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM --- 4. 构建前端 ---
echo [4/7] 构建前端应用...
set "PATH=%FLUTTER_SDK%\bin;%PATH%"
cd "%PROJECT_ROOT%\clients\flutter-web-cli"
call flutter build web --release
if !errorlevel! neq 0 (
    echo [错误] Flutter 构建失败！
    cd "%PROJECT_ROOT%"
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)
cd "%PROJECT_ROOT%"

REM --- 5. 复制后端文件 ---
echo [5/7] 复制后端文件...
if not exist "%PROJECT_ROOT%\backend\target\backend-1.0.0.jar" (
    echo [错误] backend 目录下没找到 jar 包。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

copy "%PROJECT_ROOT%\backend\target\backend-1.0.0.jar" "%BUILD_OUTPUT%\backend\backend.jar"

REM --- 6. 复制前端文件 ---
echo [6/7] 复制前端文件...
xcopy /e /i /y "%PROJECT_ROOT%\clients\flutter-web-cli\build\web\*" "%BUILD_OUTPUT%\frontend\"

REM --- 7. 复制 JDK ---
echo [7/7] 复制 JDK 运行时...
if exist "%SOURCE_JDK%\jre" (
    xcopy /e /i /y "%SOURCE_JDK%\jre\*" "%BUILD_OUTPUT%\jdk\"
) else if exist "%SOURCE_JDK%" (
    REM 对于没有单独 jre 目录的 JDK 版本
    xcopy /e /i /y "%SOURCE_JDK%\*" "%BUILD_OUTPUT%\jdk\"
) else (
    echo [错误] 无法找到 JDK 运行时目录。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM 二次检查复制结果
if not exist "%BUILD_OUTPUT%\jdk\bin\java.exe" (
    echo [致命错误] JDK 复制失败！%BUILD_OUTPUT%\jdk\bin\java.exe 不存在。
    echo 请检查是否有权限读取源目录。
    echo 将在 10 秒后自动退出...
    timeout /t 10 /nobreak >nul
    exit /b 1
)

REM --- 8. 复制启动和管理脚本 ---
echo [8/8] 复制启动和管理脚本...

REM 创建 macOS 脚本目录
mkdir "%BUILD_OUTPUT%\bin\macos"

REM 复制 macOS 脚本
copy "%SCRIPT_DIR%\macos\start-backend.sh" "%BUILD_OUTPUT%\bin\macos\"
copy "%SCRIPT_DIR%\macos\start-frontend.sh" "%BUILD_OUTPUT%\bin\macos\"
copy "%SCRIPT_DIR%\macos\stop-all.sh" "%BUILD_OUTPUT%\bin\macos\"
copy "%SCRIPT_DIR%\macos\start-all.sh" "%BUILD_OUTPUT%\bin\macos\"
copy "%SCRIPT_DIR%\macos\restart-backend.sh" "%BUILD_OUTPUT%\bin\macos\"
copy "%SCRIPT_DIR%\macos\restart-frontend.sh" "%BUILD_OUTPUT%\bin\macos\"
copy "%SCRIPT_DIR%\macos\restart-all.sh" "%BUILD_OUTPUT%\bin\macos\"

echo macOS 脚本复制完成

REM --- 9. 创建 Windows 脚本目录 ---
echo [9/9] 创建 Windows 脚本目录...

REM 创建 Windows 脚本目录
mkdir "%BUILD_OUTPUT%\bin\windows"

REM 复制 Windows 脚本
copy "%SCRIPT_DIR%\windows\start-backend.bat" "%BUILD_OUTPUT%\bin\windows\"
copy "%SCRIPT_DIR%\windows\start-frontend.bat" "%BUILD_OUTPUT%\bin\windows\"
copy "%SCRIPT_DIR%\windows\stop-all.bat" "%BUILD_OUTPUT%\bin\windows\"
copy "%SCRIPT_DIR%\windows\start-all.bat" "%BUILD_OUTPUT%\bin\windows\"
copy "%SCRIPT_DIR%\windows\restart-backend.bat" "%BUILD_OUTPUT%\bin\windows\"
copy "%SCRIPT_DIR%\windows\restart-frontend.bat" "%BUILD_OUTPUT%\bin\windows\"
copy "%SCRIPT_DIR%\windows\restart-all.bat" "%BUILD_OUTPUT%\bin\windows\"

echo Windows 脚本复制完成

echo.
echo ==========================================
echo 构建成功！
echo ==========================================
echo 输出目录: %BUILD_OUTPUT%
echo.
echo macOS 脚本位置: %BUILD_OUTPUT%\bin\macos\
echo   - start-backend.sh - 启动后端服务
echo   - start-frontend.sh - 启动前端服务
echo   - start-all.sh - 一键启动所有服务
echo   - stop-all.sh - 停止所有服务
echo   - restart-backend.sh - 重启后端服务
echo   - restart-frontend.sh - 重启前端服务
echo   - restart-all.sh - 重启所有服务
echo.
echo Windows 脚本位置: %BUILD_OUTPUT%\bin\windows\
echo   - start-backend.bat - 启动后端服务
echo   - start-frontend.bat - 启动前端服务
echo   - start-all.bat - 一键启动所有服务
echo   - stop-all.bat - 停止所有服务
echo   - restart-backend.bat - 重启后端服务
echo   - restart-frontend.bat - 重启前端服务
echo   - restart-all.bat - 重启所有服务
echo.
echo 请进入 %BUILD_OUTPUT% 文件夹，根据您的操作系统选择对应的脚本运行
echo ==========================================
echo 将在 10 秒后自动退出...
timeout /t 10 /nobreak >nul
