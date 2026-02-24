#!/bin/bash

# Spring Boot 后端停止脚本

cd "$(dirname "$0")"

if [ ! -f "backend.pid" ]; then
    echo "未找到 PID 文件，尝试查找运行中的进程"
    PIDS=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')
    if [ -z "$PIDS" ]; then
        echo "没有找到运行中的后端服务"
        exit 0
    else
        echo "找到运行中的进程: $PIDS"
        echo "正在停止..."
        kill $PIDS
        sleep 2
        echo "✓ 后端服务已停止"
    fi
    exit 0
fi

PID=$(cat backend.pid)

if ! ps -p $PID > /dev/null 2>&1; then
    echo "后端服务未运行 (PID: $PID)"
    rm backend.pid
    exit 0
fi

echo "正在停止后端服务 (PID: $PID)..."
kill $PID

# 等待进程结束
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✓ 后端服务已停止"
        rm backend.pid
        exit 0
    fi
    sleep 1
done

# 如果进程仍在运行，强制终止
echo "进程未响应，强制终止..."
kill -9 $PID
sleep 1

if ! ps -p $PID > /dev/null 2>&1; then
    echo "✓ 后端服务已强制停止"
    rm backend.pid
    exit 0
else
    echo "✗ 无法停止后端服务"
    exit 1
fi