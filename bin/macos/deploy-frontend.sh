#!/bin/bash

# FileManager Plus 前端部署脚本 (macOS)

set -e

echo "==========================================="
echo "      FileManager Plus 前端部署脚本"
echo "      (macOS)"
echo "==========================================="

echo "[1/4] 切换到前端目录..."
cd "$(dirname "$(dirname "$(dirname "$0")")")/clients/flutter-web-cli"

echo "[2/4] 构建前端应用..."
flutter build web --release --no-wasm-dry-run

echo "[3/4] 部署到前端目录..."
cd "$(dirname "$(dirname "$(dirname "$0")")")"
rm -rf frontend/* 2>/dev/null || true
cp -r clients/flutter-web-cli/build/web/* frontend/

echo "[4/4] 重启前端服务..."
cd bin/macos
./restart-frontend.sh

echo "==========================================="
echo "前端部署完成！"
echo "访问地址: http://localhost:8081"
echo "==========================================="
