#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 停止脚本"
echo "      (macOS)"
echo "==========================================="

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# --- 1. 检查服务状态 ---
echo "[1/2] 检查服务运行状态..."

# 检查后端服务 (8080 端口)
if lsof -i :8080 > /dev/null 2>&1; then
    echo "[发现] 后端服务正在运行 (端口 8080)"
    BACKEND_RUNNING=true
    # 获取进程信息
    BACKEND_PID=$(lsof -i :8080 | grep LISTEN | awk '{print $2}')
    echo "[信息] 后端服务进程 ID: $BACKEND_PID"
else
    echo "[未发现] 后端服务未运行 (端口 8080)"
    BACKEND_RUNNING=false
fi

# 检查前端服务 (8081 端口)
if lsof -i :8081 > /dev/null 2>&1; then
    echo "[发现] 前端服务正在运行 (端口 8081)"
    FRONTEND_RUNNING=true
    # 获取进程信息
    FRONTEND_PID=$(lsof -i :8081 | grep LISTEN | awk '{print $2}')
    echo "[信息] 前端服务进程 ID: $FRONTEND_PID"
else
    echo "[未发现] 前端服务未运行 (端口 8081)"
    FRONTEND_RUNNING=false
fi

if [ "$BACKEND_RUNNING" = "false" ] && [ "$FRONTEND_RUNNING" = "false" ]; then
    echo "[信息] 所有服务均未运行，无需停止。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 0
fi

# --- 2. 停止服务 ---
echo "[2/2] 正在停止服务..."

# 停止后端服务
if [ "$BACKEND_RUNNING" = "true" ]; then
    echo "正在停止后端服务..."
    if [ -n "$BACKEND_PID" ]; then
        kill -9 "$BACKEND_PID" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "后端服务已停止"
        else
            echo "停止后端服务失败，请手动停止"
        fi
    else
        echo "无法获取后端服务进程 ID，请手动停止"
    fi
fi

# 停止前端服务
if [ "$FRONTEND_RUNNING" = "true" ]; then
    echo "正在停止前端服务..."
    if [ -n "$FRONTEND_PID" ]; then
        kill -9 "$FRONTEND_PID" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "前端服务已停止"
        else
            echo "停止前端服务失败，请手动停止"
        fi
    else
        echo "无法获取前端服务进程 ID，请手动停止"
    fi
fi

# --- 3. 验证停止结果 ---
echo "[验证] 验证服务停止状态..."

# 检查后端服务是否停止
if lsof -i :8080 > /dev/null 2>&1; then
    echo "[警告] 后端服务可能未完全停止，请手动检查"
else
    echo "[成功] 后端服务已停止"
fi

# 检查前端服务是否停止
if lsof -i :8081 > /dev/null 2>&1; then
    echo "[警告] 前端服务可能未完全停止，请手动检查"
else
    echo "[成功] 前端服务已停止"
fi

echo ""
echo "==========================================="
echo "停止操作完成！"
echo "==========================================="
echo "将在 10 秒后自动退出..."
sleep 10
