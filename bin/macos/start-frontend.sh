#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 前端启动脚本"
echo "      (macOS)"
echo "==========================================="

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# --- 1. 停止已运行的前端服务 ---
echo "[1/2] 停止已运行的前端服务..."

if lsof -i :8081 > /dev/null 2>&1; then
    echo "发现前端服务正在运行 (端口 8081)"
    FRONTEND_PID=$(lsof -i :8081 | grep LISTEN | awk '{print $2}')
    if [ -n "$FRONTEND_PID" ]; then
        kill -9 "$FRONTEND_PID" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "前端服务已停止"
        else
            echo "停止前端服务失败，请手动停止"
        fi
    fi
else
    echo "前端服务未运行"
fi

# 等待 2 秒让服务完全停止
sleep 2

# --- 2. 启动前端服务 ---
echo ""
echo "[2/2] 启动 FileManager Plus 前端服务..."
echo "访问地址: http://localhost:8081"
echo "按 Ctrl+C 停止服务"
echo ""

cd "$SCRIPT_DIR/../../clients/flutter-web-cli"
FLUTTER_WEB_RENDERER=html flutter run -d chrome --web-port=8081 &
FRONTEND_PID=$!

echo "前端服务已启动 (PID: $FRONTEND_PID)"
echo "将在 3 秒后自动退出..."
sleep 3
