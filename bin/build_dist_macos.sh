#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 构建脚本 v2.0"
echo "      (JDK 21 版本 - macOS)"
echo "==========================================="

# =================配置区域=================
# 请根据你的实际情况修改这些路径！！
# JDK 21 路径 (指向包含 bin 和 lib 文件夹的 jdk 目录)
SOURCE_JDK="/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home"

# Flutter SDK 路径
FLUTTER_SDK="/Users/hrcao/Documents/flutter"

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 设置项目根目录（bin目录的父目录）
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 设置构建输出目录
BUILD_OUTPUT="$PROJECT_ROOT/dist"
# ==========================================

# --- 1. 检查环境 ---
echo "[1/7] 正在检查构建环境..."

# 检查 JDK 环境
if [ ! -f "$SOURCE_JDK/bin/java" ]; then
    echo "[错误] JDK 路径不存在: \"$SOURCE_JDK/bin/java\""
    echo "请修改脚本中的 SOURCE_JDK 变量。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 检查 Flutter 环境
if [ ! -f "$FLUTTER_SDK/bin/flutter" ]; then
    echo "[错误] Flutter SDK 路径不存在: \"$FLUTTER_SDK/bin/flutter\""
    echo "请修改脚本中的 FLUTTER_SDK 变量。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 检查 Maven 环境
if ! command -v mvn &> /dev/null; then
    echo "[错误] Maven 未找到，请确保 Maven 已添加到 PATH。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 检查 Python 环境
if ! command -v python3 &> /dev/null; then
    echo "[错误] Python 3 未找到，请确保 Python 3 已安装。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

echo "构建环境验证通过。"

# --- 2. 清理目录 ---
echo "[2/7] 清理旧文件..."
if [ -d "$BUILD_OUTPUT" ]; then
    rm -rf "$BUILD_OUTPUT"
fi

mkdir -p "$BUILD_OUTPUT/backend"
mkdir -p "$BUILD_OUTPUT/frontend"
mkdir -p "$BUILD_OUTPUT/jdk"

# --- 3. 构建后端 ---
echo "[3/7] 构建后端服务..."
cd "$PROJECT_ROOT"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "[错误] Maven 打包失败！"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# --- 4. 构建前端 ---
echo "[4/7] 构建前端应用..."
export PATH="$FLUTTER_SDK/bin:$PATH"
cd "$PROJECT_ROOT/clients/flutter-web-cli"
flutter build web --release
if [ $? -ne 0 ]; then
    echo "[错误] Flutter 构建失败！"
    cd "$PROJECT_ROOT"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi
cd "$PROJECT_ROOT"

# --- 5. 复制后端文件 ---
echo "[5/7] 复制后端文件..."
if [ ! -f "$PROJECT_ROOT/backend/target/backend-1.0.0.jar" ]; then
    echo "[错误] backend 目录下没找到 jar 包。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

cp "$PROJECT_ROOT/backend/target/backend-1.0.0.jar" "$BUILD_OUTPUT/backend/backend.jar"

# --- 6. 复制前端文件 ---
echo "[6/7] 复制前端文件..."
cp -r "$PROJECT_ROOT/clients/flutter-web-cli/build/web"/* "$BUILD_OUTPUT/frontend/"

# --- 7. 复制 JDK ---
echo "[7/7] 复制 JDK 运行时..."
if [ -d "$SOURCE_JDK/jre" ]; then
    cp -r "$SOURCE_JDK/jre"/* "$BUILD_OUTPUT/jdk/"
elif [ -d "$SOURCE_JDK" ]; then
    # 对于没有单独 jre 目录的 JDK 版本
    cp -r "$SOURCE_JDK"/* "$BUILD_OUTPUT/jdk/"
else
    echo "[错误] 无法找到 JDK 运行时目录。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 二次检查复制结果
if [ ! -f "$BUILD_OUTPUT/jdk/bin/java" ]; then
    echo "[致命错误] JDK 复制失败！$BUILD_OUTPUT/jdk/bin/java 不存在。"
    echo "请检查是否有权限读取源目录。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# --- 8. 复制启动和管理脚本 ---
echo "[8/8] 复制启动和管理脚本..."

# 创建 macOS 脚本目录
mkdir -p "$BUILD_OUTPUT/bin/macos"

# 复制 macOS 脚本
cp "$SCRIPT_DIR/macos/start-backend.sh" "$BUILD_OUTPUT/bin/macos/"
cp "$SCRIPT_DIR/macos/start-frontend.sh" "$BUILD_OUTPUT/bin/macos/"
cp "$SCRIPT_DIR/macos/stop-all.sh" "$BUILD_OUTPUT/bin/macos/"
cp "$SCRIPT_DIR/macos/start-all.sh" "$BUILD_OUTPUT/bin/macos/"
cp "$SCRIPT_DIR/macos/restart-backend.sh" "$BUILD_OUTPUT/bin/macos/"
cp "$SCRIPT_DIR/macos/restart-frontend.sh" "$BUILD_OUTPUT/bin/macos/"
cp "$SCRIPT_DIR/macos/restart-all.sh" "$BUILD_OUTPUT/bin/macos/"

# 设置脚本执行权限
chmod +x "$BUILD_OUTPUT/bin/macos/"*.sh

echo "macOS 脚本复制完成"

# --- 9. 创建 Windows 脚本目录 ---
echo "[9/9] 创建 Windows 脚本目录..."

# 创建 Windows 脚本目录
mkdir -p "$BUILD_OUTPUT/bin/windows"

# 复制 Windows 脚本
cp "$SCRIPT_DIR/windows/start-backend.bat" "$BUILD_OUTPUT/bin/windows/"
cp "$SCRIPT_DIR/windows/start-frontend.bat" "$BUILD_OUTPUT/bin/windows/"
cp "$SCRIPT_DIR/windows/stop-all.bat" "$BUILD_OUTPUT/bin/windows/"
cp "$SCRIPT_DIR/windows/start-all.bat" "$BUILD_OUTPUT/bin/windows/"
cp "$SCRIPT_DIR/windows/restart-backend.bat" "$BUILD_OUTPUT/bin/windows/"
cp "$SCRIPT_DIR/windows/restart-frontend.bat" "$BUILD_OUTPUT/bin/windows/"
cp "$SCRIPT_DIR/windows/restart-all.bat" "$BUILD_OUTPUT/bin/windows/"

echo "Windows 脚本复制完成"

echo ""
echo "==========================================="
echo "构建成功！"
echo "==========================================="
echo "输出目录: $BUILD_OUTPUT"
echo ""
echo "macOS 脚本位置: $BUILD_OUTPUT/bin/macos/"
echo "  - start-backend.sh - 启动后端服务"
echo "  - start-frontend.sh - 启动前端服务"
echo "  - start-all.sh - 一键启动所有服务"
echo "  - stop-all.sh - 停止所有服务"
echo ""
echo "Windows 脚本位置: $BUILD_OUTPUT/bin/windows/"
echo "  - start-backend.bat - 启动后端服务"
echo "  - start-frontend.bat - 启动前端服务"
echo "  - start-all.bat - 一键启动所有服务"
echo "  - stop-all.bat - 停止所有服务"
echo ""
echo "请进入 $BUILD_OUTPUT 文件夹，根据您的操作系统选择对应的脚本运行"
echo "==========================================="
echo "将在 10 秒后自动退出..."
sleep 10
