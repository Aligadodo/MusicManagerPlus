@echo off
setlocal enabledelayedexpansion
REM 切换编码为 UTF-8 以支持中文显示
chcp 65001 >nul

echo ==========================================
echo      Echo Music Manager 构建脚本 (JDK 17+)
echo      (包含完整的反射权限修复)
echo ==========================================

REM =================配置区域=================
REM 1. 设置你的 JDK 17 根目录路径
set "SOURCE_JDK=C:\Program Files\Java\jdk-17"

REM 2. 设置 FFmpeg 所在目录
set "SOURCE_FFMPEG=C:\Users\28667\Desktop\projects\pack\ffmpeg-tools"
REM ==========================================

REM --- 1. 环境自检 ---
echo [1/6] 正在检查 JDK 17 环境...
if not exist "%SOURCE_JDK%\bin\jlink.exe" (
    echo [错误] 找不到 jlink.exe。
    echo 请确认 SOURCE_JDK 指向的是完整的 JDK 17 安装目录。
    pause
    exit /b 1
)
if not exist "%SOURCE_JDK%\jmods" (
    echo [错误] 找不到 jmods 文件夹。
    pause
    exit /b 1
)

REM --- 2. 清理目录 ---
echo [2/6] 清理旧文件...
if exist "dist" rd /s /q "dist"
mkdir "dist"
mkdir "dist\bin"

REM --- 3. Maven 打包 ---
echo [3/6] 执行 Maven 打包...
call mvn clean package
if %errorlevel% neq 0 (
    echo [错误] Maven 打包失败！
    pause
    exit /b %errorlevel%
)

REM --- 4. 复制程序 ---
echo [4/6] 复制应用程序 Jar...
if not exist "target\EchoMusicManager-encrypted.jar" (
    echo [警告] 未找到加密包，使用普通包...
    copy "target\EchoMusicManager.jar" "dist\bin\EchoMusicManager.jar" >nul
) else (
    copy "target\EchoMusicManager-encrypted.jar" "dist\bin\EchoMusicManager.jar" >nul
)

REM --- 5. 生成精简版 JRE ---
echo [5/6] 正在生成定制 JRE (包含 java.desktop, java.logging 等)...
"%SOURCE_JDK%\bin\jlink.exe" ^
    --module-path "%SOURCE_JDK%\jmods" ^
    --add-modules java.base,java.desktop,java.logging,java.naming,java.sql,java.xml,jdk.unsupported,java.management,java.instrument,java.scripting,jdk.crypto.ec ^
    --output "dist\jre" ^
    --compress=2 ^
    --no-header-files ^
    --no-man-pages ^
    --strip-debug

if %errorlevel% neq 0 (
    echo [错误] JRE 生成失败！
    pause
    exit /b %errorlevel%
)

REM 复制 FFmpeg
if exist "%SOURCE_FFMPEG%\ffmpeg.exe" (
    copy "%SOURCE_FFMPEG%\ffmpeg.exe" "dist\" >nul
)

REM --- 6. 生成启动脚本 (核心修复) ---
echo [6/6] 生成启动脚本 (添加 --add-opens 参数)...
(
    echo @echo off
    echo chcp 65001 ^>nul
    echo cd /d "%%~dp0"
    echo.
    echo 正在启动 Echo Music Manager...
    echo.
    echo REM ========================================================
    echo REM JDK 17+ 模块化权限补丁
    echo REM 解决 InaccessibleObjectException 报错
    echo REM ========================================================
    echo "jre\bin\java.exe" ^
    --add-opens java.base/java.lang=ALL-UNNAMED ^
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
    --add-opens java.base/java.io=ALL-UNNAMED ^
    --add-opens java.base/java.util=ALL-UNNAMED ^
    --add-opens java.base/java.text=ALL-UNNAMED ^
    --add-opens java.desktop/java.awt.font=ALL-UNNAMED ^
    --add-opens java.desktop/sun.awt=ALL-UNNAMED ^
    --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED ^
    --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED ^
    --add-opens javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED ^
    --add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED ^
    --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED ^
    -jar "bin\EchoMusicManager.jar"
    echo.
    echo if %%errorlevel%% neq 0 ^(
    echo     echo 程序异常退出，错误码: %%errorlevel%%
    echo     pause
    echo ^)
) > "dist\EchoMusicManager.bat"

echo.
echo ==========================================
echo 构建成功！
echo 请运行 dist\EchoMusicManager.bat 进行测试。
echo ==========================================
pause