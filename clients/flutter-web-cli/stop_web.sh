#!/bin/bash

# Flutter Web 停止脚本

cd "$(dirname "$0")"

# 查找所有 Flutter 进程
PIDS=$(ps aux | grep "flutter run" | grep -v grep | awk '{print $2}')

if [ -z "$PIDS" ]; then
    echo "没有找到运行中的 Flutter Web 服务"
    exit 0
fi

echo "找到运行中的进程: $PIDS"
echo "正在停止..."

# 尝试正常停止
kill $PIDS

# 等待进程结束
for i in {1..10}; do
    NEW_PIDS=$(ps aux | grep "flutter run" | grep -v grep | awk '{print $2}')
    if [ -z "$NEW_PIDS" ]; then
        echo "✓ Flutter Web 服务已停止"
        exit 0
    fi
    sleep 1
done

# 如果进程仍在运行，强制终止
echo "进程未响应，强制终止..."
kill -9 $PIDS
sleep 1

NEW_PIDS=$(ps aux | grep "flutter run" | grep -v grep | awk '{print $2}')
if [ -z "$NEW_PIDS" ]; then
    echo "✓ Flutter Web 服务已强制停止"
    exit 0
else
    echo "✗ 无法停止 Flutter Web 服务"
    exit 1
fi