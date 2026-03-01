#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 后端启动脚本"
echo "      (macOS)"
echo "==========================================="

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 项目根目录
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# 设置JDK路径（优先使用JDK 21，兼容JDK 8）
JDK21_PATH="/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home"
JDK8_PATH="/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home"

if [ -f "$JDK21_PATH/bin/java" ]; then
    JAVA_HOME="$JDK21_PATH"
elif [ -f "$JDK8_PATH/bin/java" ]; then
    JAVA_HOME="$JDK8_PATH"
else
    JAVA_HOME="$PROJECT_ROOT/jdk"
fi

# --- 1. 停止已运行的后端服务 ---
echo "[1/2] 停止已运行的后端服务..."

if lsof -i :8080 > /dev/null 2>&1; then
    echo "发现后端服务正在运行 (端口 8080)"
    BACKEND_PID=$(lsof -i :8080 | grep LISTEN | awk '{print $2}')
    if [ -n "$BACKEND_PID" ]; then
        kill -9 "$BACKEND_PID" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "后端服务已停止"
        else
            echo "停止后端服务失败，请手动停止"
        fi
    fi
else
    echo "后端服务未运行"
fi

# 等待 2 秒让服务完全停止
sleep 2

# --- 2. 启动后端服务 ---
echo ""
echo "[2/2] 启动 FileManager Plus 后端服务..."
echo "服务地址: http://localhost:8080"
echo "按 Ctrl+C 停止服务"
echo ""

"$JAVA_HOME/bin/java" -Xms512m -Xmx1g -jar "$PROJECT_ROOT/backend/target/backend-1.0.0.jar" &
BACKEND_PID=$!

echo "后端服务已启动 (PID: $BACKEND_PID)"
echo "将在 3 秒后自动退出..."
sleep 3
