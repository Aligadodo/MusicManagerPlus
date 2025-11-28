@echo off
setlocal

echo ==========================================
echo      Echo Music Manager Build Script
echo ==========================================

REM --- 配置区域 (请仔细核对) ---
REM 1. 设置你的 JRE 路径。
REM    注意：该路径下必须直接包含 bin 文件夹。
REM    例如：C:\Program Files\Java\jdk1.8.0_202\jre
set "SOURCE_JRE=C:\Users\28667\Desktop\projects\pack\jre"

REM 2. 设置 FFmpeg 所在目录 (包含 ffmpeg.exe 的文件夹)
set "SOURCE_FFMPEG=C:\Users\28667\Desktop\projects\pack\ffmpeg-tools"

REM --- 目录准备 ---
if exist "dist" rd /s /q "dist"
mkdir "dist"
mkdir "dist\bin"
mkdir "dist\jre"

echo [1/5] Cleaning and Building with Maven...
call mvn clean package
if %errorlevel% neq 0 (
    echo [ERROR] Maven Build Failed!
    pause
    exit /b %errorlevel%
)

echo [2/5] Copying Application Jar...
if not exist "target\EchoMusicManager.jar" (
    echo [ERROR] Jar file not found in target folder. Check Maven output.
    pause
    exit /b 1
)
copy "target\EchoMusicManager.jar" "dist\bin\" >nul

echo [3/5] Bundling JRE from: "%SOURCE_JRE%"
if not exist "%SOURCE_JRE%\bin\java.exe" (
    echo [ERROR] Invalid SOURCE_JRE path!
    echo [ERROR] Could not find bin\java.exe in "%SOURCE_JRE%"
    echo [ERROR] Please edit build_dist.bat and correct SOURCE_JRE.
    pause
    exit /b 1
)
REM 使用 xcopy 复制 JRE 到 dist/jre
xcopy /E /I /Q /Y "%SOURCE_JRE%" "dist\jre" >nul

echo [4/5] Bundling FFmpeg tools...
if exist "%SOURCE_FFMPEG%\ffmpeg.exe" (
    copy "%SOURCE_FFMPEG%\ffmpeg.exe" "dist\" >nul
    echo FFmpeg bundled.
) else (
    echo [WARNING] ffmpeg.exe not found in %SOURCE_FFMPEG%. User will need to install it manually.
)

echo [5/5] Creating robust launcher...
REM 使用 ^ 转义特殊字符，生成更健壮的启动脚本
(
    echo @echo off
    echo REM 切换到脚本所在目录，确保相对路径正确
    echo cd /d "%%~dp0"
    echo.
    echo if not exist "jre\bin\javaw.exe" ^(
    echo     echo [ERROR] JRE not found! Please ensure dist/jre folder exists.
    echo     pause
    echo     exit
    echo ^)
    echo.
    echo start "" "jre\bin\javaw.exe" -jar "bin\EchoMusicManager.jar"
) > "dist\EchoMusicManager.bat"

echo.
echo ==========================================
echo Build Complete!
echo.
echo Please verify the folder structure:
echo   dist/
echo    |-- EchoMusicManager.bat
echo    |-- jre/
echo         |-- bin/  (Make sure javaw.exe is here!)
echo ==========================================
pause