#!/bin/bash

echo "==========================================="
echo "      FileManager Plus 一键启动脚本"
echo "      (macOS)"
echo "==========================================="

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 启动后端服务
echo "[1/2] 启动后端服务..."
open -a Terminal "$SCRIPT_DIR/start-backend.sh"

# 等待 3 秒
sleep 3

# 启动前端服务
echo "[2/2] 启动前端服务..."
open -a Terminal "$SCRIPT_DIR/start-frontend.sh"

echo ""
echo "==========================================="
echo "服务已启动！"
echo "后端服务地址: http://localhost:8080"
echo "前端访问地址: http://localhost:8081"
echo "如果需要停止服务，请运行: $SCRIPT_DIR/stop-all.sh"
echo "==========================================="
echo "将在 10 秒后自动退出..."
sleep 10
