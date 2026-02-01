#!/bin/bash

# 停止后端服务脚本
echo "开始停止后端服务..."

# 进入backend目录
cd "$(dirname "$0")/backend" || {
    echo "无法进入backend目录"
    exit 1
}

# 检查是否存在进程ID文件
if [ -f "backend.pid" ]; then
    BACKEND_PID=$(cat backend.pid)
    echo "找到后端服务进程ID: $BACKEND_PID"
    
    # 检查进程是否存在
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo "正在停止后端服务..."
        kill $BACKEND_PID
        
        # 等待进程停止
        sleep 3
        
        # 再次检查进程是否存在
        if ps -p $BACKEND_PID > /dev/null 2>&1; then
            echo "服务停止失败，尝试强制停止..."
            kill -9 $BACKEND_PID
            sleep 2
        fi
        
        # 清理进程ID文件
        rm -f backend.pid
        echo "后端服务已停止"
    else
        echo "后端服务进程不存在，可能已经停止"
        rm -f backend.pid
    fi
else
    echo "未找到后端服务进程ID文件，服务可能未启动"
fi

# 清理日志文件（可选）
if [ -f "backend.log" ]; then
    echo "清理日志文件..."
    rm -f backend.log
fi

echo "停止操作完成！"
