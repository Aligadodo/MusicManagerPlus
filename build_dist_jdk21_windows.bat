@echo off
setlocal enabledelayedexpansion
REM 切换编码以支持中文显示
chcp 65001 >nul

REM 设置输出文件编码为 UTF-8
set "OUTPUT_ENCODING=UTF-8"

echo ==========================================
echo      FileManager Plus 构建脚本 v1.0
echo      (JDK 21 版本 - Windows)
echo ==========================================

REM =================配置区域=================
REM 请根据你的实际情况修改这些路径！！
REM JDK 21 路径 (指向包含 bin 和 lib 文件夹的 jdk 目录)
set "SOURCE_JDK=C:\Program Files\Java\jdk-21.0.10"

REM Flutter SDK 路径
set "FLUTTER_SDK=C:\Program Files\flutter_windows_3.38.4-stable\flutter"

REM 工具文件路径
set "SOURCE_TOOLS=D:\projects\pack\tools"

REM 设置构建输出目录
set "BUILD_OUTPUT=dist"
REM ==========================================

REM --- 1. 检查环境 ---
echo [1/8] 正在检查构建环境...

REM 检查 JDK 环境
if not exist "%SOURCE_JDK%\bin\java.exe" (
    echo [错误] JDK 路径不存在 java.exe: "%SOURCE_JDK%\bin\java.exe"
    echo 请修改脚本中的 SOURCE_JDK 变量。
    pause
    exit /b 1
)

REM 检查 Flutter 环境
if not exist "%FLUTTER_SDK%\bin\flutter.bat" (
    echo [错误] Flutter SDK 路径不存在: "%FLUTTER_SDK%\bin\flutter.bat"
    echo 请修改脚本中的 FLUTTER_SDK 变量。
    pause
    exit /b 1
)

REM 检查 Maven 环境
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] Maven 未找到，请确保 Maven 已添加到 PATH。
    pause
    exit /b 1
)

echo 构建环境验证通过。

REM --- 2. 清理目录 ---
echo [2/8] 清理旧文件...
if exist "%BUILD_OUTPUT%" rd /s /q "%BUILD_OUTPUT%"
mkdir "%BUILD_OUTPUT%"
mkdir "%BUILD_OUTPUT%\backend"
mkdir "%BUILD_OUTPUT%\frontend"
mkdir "%BUILD_OUTPUT%\jdk"

REM --- 3. 构建后端 ---
echo [3/8] 构建后端服务...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [错误] Maven 打包失败！
    pause
    exit /b %errorlevel%
)

REM --- 4. 构建前端 ---
echo [4/8] 构建前端应用...
set "PATH=%FLUTTER_SDK%\bin;%PATH%"
cd clients\flutter-web-cli
call flutter build web
if %errorlevel% neq 0 (
    echo [错误] Flutter 构建失败！
    cd ..\..
    pause
    exit /b %errorlevel%
)
cd ..\..

REM --- 5. 复制后端文件 ---
echo [5/8] 复制后端文件...
if not exist "backend\target\backend-1.0.0.jar" (
    echo [错误] backend 目录下没找到 jar 包。
    pause
    exit /b 1
)
copy "backend\target\backend-1.0.0.jar" "%BUILD_OUTPUT%\backend\" >nul

REM --- 6. 复制前端文件 ---
echo [6/8] 复制前端文件...
xcopy /E /I /Q /Y "clients\flutter-web-cli\build\web" "%BUILD_OUTPUT%\frontend" >nul

REM --- 7. 复制 JDK ---
echo [7/8] 复制 JDK 运行时...
xcopy /E /I /Q /Y "%SOURCE_JDK%" "%BUILD_OUTPUT%\jdk" >nul

REM 二次检查复制结果
if not exist "%BUILD_OUTPUT%\jdk\bin\java.exe" (
    echo [致命错误] JDK 复制失败！%BUILD_OUTPUT%\jdk\bin\java.exe 不存在。
    echo 请检查是否有权限读取源目录，或者尝试以管理员身份运行此脚本。
    pause
    exit /b 1
)

REM --- 8. 复制工具文件 ---
echo [8/9] 复制工具文件...
mkdir "%BUILD_OUTPUT%\tools" >nul 2>nul
if exist "%SOURCE_TOOLS%" (
    xcopy /E /I /Q /Y "%SOURCE_TOOLS%" "%BUILD_OUTPUT%\tools" >nul
    echo [成功] 工具文件复制完成
) else (
    echo [警告] 工具文件目录不存在: %SOURCE_TOOLS%
    echo 请检查工具文件路径是否正确
)

REM --- 9. 生成启动和管理脚本 ---
echo [9/9] 生成启动和管理脚本...

REM 生成后端启动脚本
echo 正在生成 start-backend.bat...
type nul > "%BUILD_OUTPUT%\start-backend.bat"
echo @echo off >> "%BUILD_OUTPUT%\start-backend.bat"
echo chcp 65001 ^>nul >> "%BUILD_OUTPUT%\start-backend.bat"
echo cd /d "%%~dp0" >> "%BUILD_OUTPUT%\start-backend.bat"
echo echo 正在启动 FileManager Plus 后端服务... >> "%BUILD_OUTPUT%\start-backend.bat"
echo echo 服务地址: http://localhost:8080 >> "%BUILD_OUTPUT%\start-backend.bat"
echo echo 按 Ctrl+C 停止服务 >> "%BUILD_OUTPUT%\start-backend.bat"
echo echo. >> "%BUILD_OUTPUT%\start-backend.bat"
echo "jdk\bin\java.exe" -Xms512m -Xmx1g -jar "backend\backend-1.0.0.jar" >> "%BUILD_OUTPUT%\start-backend.bat"

REM 生成前端启动脚本
echo 正在生成 start-frontend.bat...
type nul > "%BUILD_OUTPUT%\start-frontend.bat"
echo @echo off >> "%BUILD_OUTPUT%\start-frontend.bat"
echo chcp 65001 ^>nul >> "%BUILD_OUTPUT%\start-frontend.bat"
echo cd /d "%%~dp0" >> "%BUILD_OUTPUT%\start-frontend.bat"
echo echo 正在启动 FileManager Plus 前端服务... >> "%BUILD_OUTPUT%\start-frontend.bat"
echo echo 访问地址: http://localhost:8081 >> "%BUILD_OUTPUT%\start-frontend.bat"
echo echo 按 Ctrl+C 停止服务 >> "%BUILD_OUTPUT%\start-frontend.bat"
echo echo. >> "%BUILD_OUTPUT%\start-frontend.bat"
echo python -m http.server 8081 --directory "frontend" >> "%BUILD_OUTPUT%\start-frontend.bat"

REM 生成一键启动脚本
echo 正在生成 start-all.bat...
type nul > "%BUILD_OUTPUT%\start-all.bat"
echo @echo off >> "%BUILD_OUTPUT%\start-all.bat"
echo chcp 65001 ^>nul >> "%BUILD_OUTPUT%\start-all.bat"
echo cd /d "%%~dp0" >> "%BUILD_OUTPUT%\start-all.bat"
echo echo 正在启动 FileManager Plus 服务... >> "%BUILD_OUTPUT%\start-all.bat"
echo echo 后端服务地址: http://localhost:8080 >> "%BUILD_OUTPUT%\start-all.bat"
echo echo 前端访问地址: http://localhost:8081 >> "%BUILD_OUTPUT%\start-all.bat"
echo echo 按 Ctrl+C 停止服务 >> "%BUILD_OUTPUT%\start-all.bat"
echo echo. >> "%BUILD_OUTPUT%\start-all.bat"
echo start "FileManager Backend" cmd /c "start-backend.bat" >> "%BUILD_OUTPUT%\start-all.bat"
echo timeout /t 3 >> "%BUILD_OUTPUT%\start-all.bat"
echo start "FileManager Frontend" cmd /c "start-frontend.bat" >> "%BUILD_OUTPUT%\start-all.bat"
echo echo 服务已启动，请在浏览器中访问 http://localhost:8081 >> "%BUILD_OUTPUT%\start-all.bat"
echo echo 如果需要停止服务，请关闭对应的命令窗口 >> "%BUILD_OUTPUT%\start-all.bat"
echo pause >> "%BUILD_OUTPUT%\start-all.bat"

REM 生成停止脚本
echo 正在生成 stop-all.bat...
type nul > "%BUILD_OUTPUT%\stop-all.bat"
echo @echo off >> "%BUILD_OUTPUT%\stop-all.bat"
echo setlocal enabledelayedexpansion >> "%BUILD_OUTPUT%\stop-all.bat"
echo chcp 65001 ^>nul >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo      FileManager Plus 停止脚本 v1.0 >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo      (Windows) >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM --- 1. 检查服务状态 --- >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo [1/2] 检查服务运行状态... >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM 检查后端服务 (8080 端口) >> "%BUILD_OUTPUT%\stop-all.bat"
echo netstat -ano ^| findstr :8080 ^>nul >> "%BUILD_OUTPUT%\stop-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [发现] 后端服务正在运行 (端口 8080) >> "%BUILD_OUTPUT%\stop-all.bat"
echo     set "BACKEND_RUNNING=true" >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [未发现] 后端服务未运行 (端口 8080) >> "%BUILD_OUTPUT%\stop-all.bat"
echo     set "BACKEND_RUNNING=false" >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM 检查前端服务 (8081 端口) >> "%BUILD_OUTPUT%\stop-all.bat"
echo netstat -ano ^| findstr :8081 ^>nul >> "%BUILD_OUTPUT%\stop-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [发现] 前端服务正在运行 (端口 8081) >> "%BUILD_OUTPUT%\stop-all.bat"
echo     set "FRONTEND_RUNNING=true" >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [未发现] 前端服务未运行 (端口 8081) >> "%BUILD_OUTPUT%\stop-all.bat"
echo     set "FRONTEND_RUNNING=false" >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo if "!BACKEND_RUNNING!" equ "false" if "!FRONTEND_RUNNING!" equ "false" ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [信息] 所有服务均未运行，无需停止。 >> "%BUILD_OUTPUT%\stop-all.bat"
echo     pause >> "%BUILD_OUTPUT%\stop-all.bat"
echo     exit /b 0 >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM --- 2. 停止服务 --- >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo [2/2] 正在停止服务... >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM 停止后端服务 >> "%BUILD_OUTPUT%\stop-all.bat"
echo if "!BACKEND_RUNNING!" equ "true" ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo 正在停止后端服务... >> "%BUILD_OUTPUT%\stop-all.bat"
echo     REM 获取占用 8080 端口的进程 ID >> "%BUILD_OUTPUT%\stop-all.bat"
echo     for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo         set "PID=%%a" >> "%BUILD_OUTPUT%\stop-all.bat"
echo         echo 发现进程 ID: !PID! >> "%BUILD_OUTPUT%\stop-all.bat"
echo         taskkill /PID !PID! /F ^>nul 2^>nul >> "%BUILD_OUTPUT%\stop-all.bat"
echo         if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo             echo 后端服务已停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo         ) else ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo             echo 停止后端服务失败，请手动停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo         ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo     ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM 停止前端服务 >> "%BUILD_OUTPUT%\stop-all.bat"
echo if "!FRONTEND_RUNNING!" equ "true" ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo 正在停止前端服务... >> "%BUILD_OUTPUT%\stop-all.bat"
echo     REM 获取占用 8081 端口的进程 ID >> "%BUILD_OUTPUT%\stop-all.bat"
echo     for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081') do ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo         set "PID=%%a" >> "%BUILD_OUTPUT%\stop-all.bat"
echo         echo 发现进程 ID: !PID! >> "%BUILD_OUTPUT%\stop-all.bat"
echo         taskkill /PID !PID! /F ^>nul 2^>nul >> "%BUILD_OUTPUT%\stop-all.bat"
echo         if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo             echo 前端服务已停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo         ) else ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo             echo 停止前端服务失败，请手动停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo         ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo     ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM --- 3. 验证停止结果 --- >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo [验证] 验证服务停止状态... >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM 检查后端服务是否停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo netstat -ano ^| findstr :8080 ^>nul >> "%BUILD_OUTPUT%\stop-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [警告] 后端服务可能未完全停止，请手动检查 >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [成功] 后端服务已停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo REM 检查前端服务是否停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo netstat -ano ^| findstr :8081 ^>nul >> "%BUILD_OUTPUT%\stop-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [警告] 前端服务可能未完全停止，请手动检查 >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\stop-all.bat"
echo     echo [成功] 前端服务已停止 >> "%BUILD_OUTPUT%\stop-all.bat"
echo ) >> "%BUILD_OUTPUT%\stop-all.bat"
echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo. >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo 停止操作完成！ >> "%BUILD_OUTPUT%\stop-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\stop-all.bat"
echo pause >> "%BUILD_OUTPUT%\stop-all.bat"

REM 生成重启脚本
echo 正在生成 restart-all.bat...
type nul > "%BUILD_OUTPUT%\restart-all.bat"
echo @echo off >> "%BUILD_OUTPUT%\restart-all.bat"
echo setlocal enabledelayedexpansion >> "%BUILD_OUTPUT%\restart-all.bat"
echo chcp 65001 ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo      FileManager Plus 重启脚本 v1.0 >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo      (Windows) >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM --- 1. 停止服务 --- >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo [1/3] 停止现有服务... >> "%BUILD_OUTPUT%\restart-all.bat"
echo call "%%~dp0stop-all.bat" >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM --- 2. 检查停止结果 --- >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo [2/3] 检查服务停止状态... >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 等待 2 秒让服务完全停止 >> "%BUILD_OUTPUT%\restart-all.bat"
echo timeout /t 2 /nobreak ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 检查后端服务是否停止 >> "%BUILD_OUTPUT%\restart-all.bat"
echo netstat -ano ^| findstr :8080 ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [警告] 后端服务可能未完全停止，继续重启操作 >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [成功] 后端服务已停止 >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 检查前端服务是否停止 >> "%BUILD_OUTPUT%\restart-all.bat"
echo netstat -ano ^| findstr :8081 ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [警告] 前端服务可能未完全停止，继续重启操作 >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [成功] 前端服务已停止 >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM --- 3. 启动服务 --- >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo [3/3] 启动服务... >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo 正在启动 FileManager Plus 服务... >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo 服务地址: http://localhost:8080 >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo 访问地址: http://localhost:8081 >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo 按 Ctrl+C 停止服务 >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 启动后端服务 >> "%BUILD_OUTPUT%\restart-all.bat"
echo start "FileManager Backend" cmd /c "%%~dp0start-backend.bat" >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 等待 3 秒让后端服务启动 >> "%BUILD_OUTPUT%\restart-all.bat"
echo timeout /t 3 /nobreak ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 检查后端服务是否启动成功 >> "%BUILD_OUTPUT%\restart-all.bat"
echo netstat -ano ^| findstr :8080 ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [成功] 后端服务启动成功 (端口 8080) >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [警告] 后端服务可能启动失败，请检查日志 >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 启动前端服务 >> "%BUILD_OUTPUT%\restart-all.bat"
echo start "FileManager Frontend" cmd /c "%%~dp0start-frontend.bat" >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 等待 2 秒让前端服务启动 >> "%BUILD_OUTPUT%\restart-all.bat"
echo timeout /t 2 /nobreak ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo REM 检查前端服务是否启动成功 >> "%BUILD_OUTPUT%\restart-all.bat"
echo netstat -ano ^| findstr :8081 ^>nul >> "%BUILD_OUTPUT%\restart-all.bat"
echo if %%errorlevel%% equ 0 ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [成功] 前端服务启动成功 (端口 8081) >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) else ( >> "%BUILD_OUTPUT%\restart-all.bat"
echo     echo [警告] 前端服务可能启动失败，请检查日志 >> "%BUILD_OUTPUT%\restart-all.bat"
echo ) >> "%BUILD_OUTPUT%\restart-all.bat"
echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo. >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo 重启操作完成！ >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo 服务已启动，请在浏览器中访问 http://localhost:8081 >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo 如果需要停止服务，请关闭对应的命令窗口 >> "%BUILD_OUTPUT%\restart-all.bat"
echo echo ========================================== >> "%BUILD_OUTPUT%\restart-all.bat"
echo pause >> "%BUILD_OUTPUT%\restart-all.bat"

echo.
echo ==========================================
echo 构建成功！
echo 请进入 %BUILD_OUTPUT% 文件夹运行启动脚本测试
echo 脚本列表：
echo 1. start-backend.bat - 启动后端服务
echo 2. start-frontend.bat - 启动前端服务
echo 3. start-all.bat - 一键启动所有服务
echo 4. stop-all.bat - 停止所有服务
echo 5. restart-all.bat - 重启所有服务
echo ==========================================
pause