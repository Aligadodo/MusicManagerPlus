#!/bin/bash

# 启动后端服务脚本
echo "开始启动后端服务..."

# 进入backend目录
cd "$(dirname "$0")/backend" || {
    echo "无法进入backend目录"
    exit 1
}

# 构建项目
echo "正在构建项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "项目构建失败"
    exit 1
fi

# 查找构建的jar文件
JAR_FILE=$(ls target/*.jar 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "未找到构建的jar文件"
    exit 1
fi

echo "找到构建文件: $JAR_FILE"

# 启动应用（后台运行）
echo "正在启动后端服务..."
nohup java -jar "$JAR_FILE" > backend.log 2>&1 &

# 保存进程ID
BACKEND_PID=$!
echo "后端服务已启动，进程ID: $BACKEND_PID"
echo $BACKEND_PID > backend.pid

echo "启动完成！后端服务正在运行中..."
echo "日志输出到: backend.log"
echo "可以使用以下命令停止服务: ./stop_backend.sh"
