#!/bin/bash

# Flutter Web 启动脚本
# 支持可配置的端口号和配置文件

# 默认配置
DEFAULT_PORT=8081
CONFIG_FILE=".flutter_config"

# 从配置文件读取端口
if [ -f "$CONFIG_FILE" ]; then
    PORT=$(grep "^web_port=" "$CONFIG_FILE" | cut -d'=' -f2)
fi

# 从环境变量获取端口（优先级高于配置文件）
PORT=${FLUTTER_WEB_PORT:-$PORT}

# 从命令行参数获取端口（优先级最高）
PORT=${1:-$PORT}

# 如果端口仍然为空，使用默认值
PORT=${PORT:-$DEFAULT_PORT}

echo "======================================"
echo "Flutter Web 启动脚本"
echo "======================================"
echo "端口: $PORT"
echo "设备: chrome"
echo "配置文件: $([ -f "$CONFIG_FILE" ] && echo "已加载" || echo "未找到")"
echo "======================================"
echo ""

cd "$(dirname "$0")"

flutter run -d chrome --web-port="$PORT"
