@echo off
setlocal enabledelayedexpansion
REM 切换编码以支持中文显示
chcp 65001 >nul

echo ==========================================
echo      Echo Music Manager 构建脚本 v2.0
echo      (带环境自检与核心库验证)
echo ==========================================

REM =================配置区域=================
REM 请根据你的实际情况修改这个路径！！
REM 必须指向包含 bin 和 lib 文件夹的 jre 目录
set "SOURCE_JRE=D:\projects\pack\jre"

REM 设置 FFmpeg 所在目录
set "SOURCE_FFMPEG=D:\projects\pack\ffmpeg-tools"
REM ==========================================

REM --- 1. 检查源环境 ---
echo [1/6] 正在检查源 JRE 环境...
if not exist "%SOURCE_JRE%\bin\java.exe" (
    echo [错误] 源路径不存在 java.exe: "%SOURCE_JRE%\bin\java.exe"
    echo 请修改脚本中的 SOURCE_JRE 变量。
    pause
    exit /b 1
)
if not exist "%SOURCE_JRE%\lib\rt.jar" (
    echo [错误] 源路径缺少核心库: "%SOURCE_JRE%\lib\rt.jar"
    echo 导致 'NoClassDefFoundError: java/lang/Object' 的原因就是缺这个文件。
    echo 请确保 SOURCE_JRE 指向的是 JDK 目录下的 jre 文件夹。
    pause
    exit /b 1
)
echo 源 JRE 环境验证通过。

REM --- 2. 清理目录 ---
echo [2/6] 清理旧文件...
if exist "dist" rd /s /q "dist"
mkdir "dist"
mkdir "dist\bin"
mkdir "dist\jre"

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
if not exist "target\EchoMusicManager.jar" (
    echo [错误] target 目录下没找到 jar 包。
    pause
    exit /b 1
)
copy "target\EchoMusicManager.jar" "dist\bin\" >nul

REM --- 5. 复制 JRE (关键步骤) ---
echo [5/6] 正在捆绑 JRE (正在复制核心库，请稍候)...
REM /E:复制目录和子目录 /I:如果目标不存在则假定为目录 /Q:静默 /Y:覆盖
xcopy /E /I /Q /Y "%SOURCE_JRE%" "dist\jre" >nul

REM 二次检查复制结果
if not exist "dist\jre\lib\rt.jar" (
    echo [致命错误] JRE 复制失败！dist\jre\lib\rt.jar 不存在。
    echo 请检查是否有权限读取源目录，或者尝试以管理员身份运行此脚本。
    pause
    exit /b 1
)

REM 复制 FFmpeg
if exist "%SOURCE_FFMPEG%\ffmpeg.exe" (
    copy "%SOURCE_FFMPEG%\ffmpeg.exe" "dist\" >nul
)

REM 复制主题文件
echo [6/7] 正在复制主题文件...
if exist "style\themes" (
    xcopy /E /I /Q /Y "style\themes" "dist\style\themes" >nul
    echo 主题文件复制成功！
)

REM --- 7. 生成启动脚本 ---
echo [7/7] 生成启动脚本...
(
    echo @echo off
    echo chcp 65001 ^>nul
    echo cd /d "%%~dp0"
    echo 正在启动 Echo Music Manager...
    echo.
    echo REM 启动命令
    echo "jre\bin\java.exe" -Xms1g -Xmx2g -XX:+HeapDumpOnOutOfMemoryError -jar "bin\EchoMusicManager.jar"
    echo.
    echo if %%errorlevel%% neq 0 ^(
    echo     echo 程序异常退出，错误码: %%errorlevel%%
    echo     pause
    echo ^)
) > "dist\点此启动软件.bat"

echo.
echo ==========================================
echo 构建成功！
echo 请进入 dist 文件夹运行 EchoMusicManager.bat 测试
echo ==========================================
pause