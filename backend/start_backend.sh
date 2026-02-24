#!/bin/bash

# Spring Boot 后端启动脚本
# 支持后台运行模式

echo "======================================"
echo "Spring Boot 后端启动脚本"
echo "======================================"
echo ""

cd "$(dirname "$0")"

# 检查是否已经在运行
if [ -f "backend.pid" ]; then
    OLD_PID=$(cat backend.pid)
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "后端服务已在运行中 (PID: $OLD_PID)"
        echo "如需重启，请先运行: ./stop_backend.sh"
        exit 1
    else
        echo "清理旧的 PID 文件"
        rm backend.pid
    fi
fi

# 后台运行模式
nohup mvn spring-boot:run > backend.log 2>&1 &
PID=$!

# 保存 PID
echo $PID > backend.pid

echo "Spring Boot 后端服务已在后台启动"
echo "进程 ID: $PID"
echo "日志文件: $(pwd)/backend.log"
echo "PID 文件: $(pwd)/backend.pid"
echo ""
echo "等待服务启动..."
sleep 10

# 检查进程是否还在运行
if ps -p $PID > /dev/null 2>&1; then
    echo "✓ 服务启动成功"
    echo "访问地址: http://localhost:8080"
    echo ""
    echo "查看日志: tail -f backend.log"
    echo "停止服务: ./stop_backend.sh"
    echo "或者: kill $PID"
else
    echo "✗ 服务启动失败，请查看日志文件"
    cat backend.log
    rm backend.pid
    exit 1
fi