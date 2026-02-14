#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 重启脚本"
echo "      (macOS)"
echo "==========================================="

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# --- 1. 停止服务 ---
echo "[1/3] 停止现有服务..."
if [ -f "$SCRIPT_DIR/stop-all.sh" ]; then
    "$SCRIPT_DIR/stop-all.sh"
else
    echo "[警告] 未找到 stop-all.sh 脚本，尝试手动停止服务"
    # 手动停止后端服务
    if lsof -i :8080 > /dev/null 2>&1; then
        BACKEND_PID=$(lsof -i :8080 | grep LISTEN | awk '{print $2}')
        if [ -n "$BACKEND_PID" ]; then
            kill -9 "$BACKEND_PID" > /dev/null 2>&1
            echo "后端服务已停止"
        fi
    fi
    # 手动停止前端服务
    if lsof -i :8081 > /dev/null 2>&1; then
        FRONTEND_PID=$(lsof -i :8081 | grep LISTEN | awk '{print $2}')
        if [ -n "$FRONTEND_PID" ]; then
            kill -9 "$FRONTEND_PID" > /dev/null 2>&1
            echo "前端服务已停止"
        fi
    fi
fi

# --- 2. 检查停止结果 ---
echo "[2/3] 检查服务停止状态..."

# 等待 2 秒让服务完全停止
sleep 2

# 检查后端服务是否停止
if lsof -i :8080 > /dev/null 2>&1; then
    echo "[警告] 后端服务可能未完全停止，继续重启操作"
else
    echo "[成功] 后端服务已停止"
fi

# 检查前端服务是否停止
if lsof -i :8081 > /dev/null 2>&1; then
    echo "[警告] 前端服务可能未完全停止，继续重启操作"
else
    echo "[成功] 前端服务已停止"
fi

# --- 3. 启动服务 ---
echo "[3/3] 启动服务..."
echo "正在启动 FileManager Plus 服务..."
echo "服务地址: http://localhost:8080"
echo "访问地址: http://localhost:8081"
echo "按 Ctrl+C 停止服务"
echo ""

# 启动后端服务
if [ -f "$SCRIPT_DIR/start-backend.sh" ]; then
    "$SCRIPT_DIR/start-backend.sh" > /dev/null 2>&1 &
else
    echo "[错误] 未找到 start-backend.sh 脚本"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 等待 3 秒让后端服务启动
sleep 3

# 检查后端服务是否启动成功
if lsof -i :8080 > /dev/null 2>&1; then
    echo "[成功] 后端服务启动成功 (端口 8080)"
else
    echo "[警告] 后端服务可能启动失败，请检查日志"
fi

# 启动前端服务
if [ -f "$SCRIPT_DIR/start-frontend.sh" ]; then
    "$SCRIPT_DIR/start-frontend.sh" > /dev/null 2>&1 &
else
    echo "[错误] 未找到 start-frontend.sh 脚本"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 等待 2 秒让前端服务启动
sleep 2

# 检查前端服务是否启动成功
if lsof -i :8081 > /dev/null 2>&1; then
    echo "[成功] 前端服务启动成功 (端口 8081)"
else
    echo "[警告] 前端服务可能启动失败，请检查日志"
fi

echo ""
echo "==========================================="
echo "重启操作完成！"
echo "服务已启动，请在浏览器中访问 http://localhost:8081"
echo "如果需要停止服务，请关闭对应的终端窗口"
echo "==========================================="
echo "将在 10 秒后自动退出..."
sleep 10
