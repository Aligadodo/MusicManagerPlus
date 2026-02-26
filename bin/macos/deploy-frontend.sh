#!/bin/bash

# FileManager Plus 前端部署脚本 (macOS)

set -e

echo "==========================================="
echo "      FileManager Plus 前端部署脚本"
echo "      (macOS)"
echo "==========================================="

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# 获取项目根目录
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "[1/4] 切换到前端目录..."
echo "Project root: $PROJECT_ROOT"
cd "$PROJECT_ROOT/clients/flutter-web-cli"

echo "[2/4] 构建前端应用..."
FLUTTER_WEB_RENDERER=html flutter build web --release --no-wasm-dry-run

echo "[3/4] 部署到前端目录..."
cd "$PROJECT_ROOT"

# 确保frontend目录存在
if [ ! -d "frontend" ]; then
    echo "Creating frontend directory..."
    mkdir -p frontend
fi

rm -rf frontend/* 2>/dev/null || true
cp -r clients/flutter-web-cli/build/web/* frontend/

echo "[4/4] 重启前端服务..."
cd "$SCRIPT_DIR"
./restart-frontend.sh

echo "==========================================="
echo "前端部署完成！"
echo "访问地址: http://localhost:8081"
echo "==========================================="
