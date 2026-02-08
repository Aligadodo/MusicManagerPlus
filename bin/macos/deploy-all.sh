#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 快速部署脚本"
echo "      (macOS)"
echo "==========================================="

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# --- 1. 编译后端 ---
echo "[1/4] 编译后端代码..."
cd "$SCRIPT_DIR/../backend"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "后端编译失败"
    exit 1
fi
echo "后端编译成功"

# --- 2. 编译前端 ---
echo ""
echo "[2/4] 编译前端代码..."
cd "$SCRIPT_DIR/../clients/flutter-web-cli"
flutter build web --release
if [ $? -ne 0 ]; then
    echo "前端编译失败"
    exit 1
fi
echo "前端编译成功"

# --- 3. 部署前端 ---
echo ""
echo "[3/4] 部署前端代码..."
cp -r build/web/* "$SCRIPT_DIR/../frontend/"
echo "前端部署成功"

# --- 4. 重启服务 ---
echo ""
echo "[4/4] 重启前后端服务..."
cd "$SCRIPT_DIR"
./restart-all.sh

echo ""
echo "==========================================="
echo "      部署完成"
echo "==========================================="
echo "后端地址: http://localhost:8080"
echo "前端地址: http://localhost:8081"
