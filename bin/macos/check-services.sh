#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 服务状态检查"
echo "      (macOS)"
echo "==========================================="

# 检查后端服务
echo ""
echo "[后端服务] (端口 8080)"
if lsof -i :8080 > /dev/null 2>&1; then
    BACKEND_PID=$(lsof -i :8080 | grep LISTEN | awk '{print $2}')
    BACKEND_CMD=$(ps -p $BACKEND_PID -o comm= 2>/dev/null)
    echo "状态: 运行中"
    echo "进程ID: $BACKEND_PID"
    echo "进程名: $BACKEND_CMD"
else
    echo "状态: 未运行"
fi

# 检查前端服务
echo ""
echo "[前端服务] (端口 8081)"
if lsof -i :8081 > /dev/null 2>&1; then
    FRONTEND_PID=$(lsof -i :8081 | grep LISTEN | awk '{print $2}')
    FRONTEND_CMD=$(ps -p $FRONTEND_PID -o comm= 2>/dev/null)
    echo "状态: 运行中"
    echo "进程ID: $FRONTEND_PID"
    echo "进程名: $FRONTEND_CMD"
else
    echo "状态: 未运行"
fi

echo ""
echo "==========================================="
echo "访问地址:"
echo "后端: http://localhost:8080"
echo "前端: http://localhost:8081"
echo "==========================================="
