@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo      Echo Music Manager Build Script V5
echo      (Fixes Manifest & Startup Args)
echo ==========================================

REM ================= CONFIGURATION =================
REM Set JDK 17 path
set "SOURCE_JDK=C:\Program Files\Java\jdk-17"
REM Set FFmpeg path
set "SOURCE_FFMPEG=C:\Users\28667\Desktop\projects\pack\ffmpeg-tools"
REM =================================================

REM --- 1. Setup JAVA_HOME ---
echo [1/7] Setting up build environment...
if not exist "%SOURCE_JDK%\bin\javac.exe" goto ErrorJdk
set "JAVA_HOME=%SOURCE_JDK%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
if %errorlevel% neq 0 goto ErrorJavaVer
echo.

REM --- 2. Clean ---
echo [2/7] Cleaning workspace...
if exist "dist" rd /s /q "dist"
mkdir "dist"
mkdir "dist\bin"

REM --- 3. Maven Build ---
echo [3/7] Running Maven Clean Package...
call mvn clean package
if %errorlevel% neq 0 goto ErrorMaven

REM --- 4. Copy Jar ---
echo [4/7] Copying Jar...
REM Try encrypted jar first, then shade jar, then original jar
if exist "target\EchoMusicManager-encrypted.jar" (
    echo Using Encrypted Jar.
    copy "target\EchoMusicManager-encrypted.jar" "dist\bin\EchoMusicManager.jar" >nul
) else (
    REM Fallback to the standard shade jar (usually named EchoMusicManager-1.0-SNAPSHOT.jar or defined by finalName)
    if exist "target\EchoMusicManager.jar" (
        echo Using Standard Jar.
        copy "target\EchoMusicManager.jar" "dist\bin\EchoMusicManager.jar" >nul
    ) else (
        goto ErrorJar
    )
)

REM --- 5. Generate JRE ---
echo [5/7] Generating JRE...
"%SOURCE_JDK%\bin\jlink.exe" --module-path "%SOURCE_JDK%\jmods" --add-modules java.base,java.desktop,java.logging,java.naming,java.sql,java.xml,jdk.unsupported,java.management,java.instrument,java.scripting,jdk.crypto.ec --output "dist\jre" --compress=2 --no-header-files --no-man-pages --strip-debug
if %errorlevel% neq 0 goto ErrorJre

REM Copy FFmpeg
if exist "%SOURCE_FFMPEG%\ffmpeg.exe" copy "%SOURCE_FFMPEG%\ffmpeg.exe" "dist\" >nul

REM --- 6. Create Launcher ---
echo [6/7] Creating Launcher Script...
set "BAT_FILE=dist\EchoMusicManager.bat"
echo @echo off > "%BAT_FILE%"
echo cd /d "%%~dp0" >> "%BAT_FILE%"
echo echo Starting App... >> "%BAT_FILE%"
REM JVM Args fix: Only open JDK internal modules, do not open javafx modules (they are on classpath now)
echo "jre\bin\java.exe" --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED -jar "bin\EchoMusicManager.jar" >> "%BAT_FILE%"
echo if %%errorlevel%% neq 0 pause >> "%BAT_FILE%"

echo.
echo ==========================================
echo Build Success!
echo Run dist\EchoMusicManager.bat to test.
echo ==========================================
pause
exit /b 0

:ErrorJdk
echo [ERROR] Invalid JDK path.
pause
exit /b 1
:ErrorJavaVer
echo [ERROR] Java check failed.
pause
exit /b 1
:ErrorMaven
echo [ERROR] Maven build failed.
pause
exit /b 1
:ErrorJar
echo [ERROR] Jar file missing in target/.
pause
exit /b 1
:ErrorJre
echo [ERROR] JRE generation failed.
pause
exit /b 1