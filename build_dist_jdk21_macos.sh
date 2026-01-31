#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 构建脚本 v1.0"
echo "      (JDK 21 版本 - macOS)"
echo "==========================================="

# =================配置区域=================
# 请根据你的实际情况修改这些路径！！
# JDK 21 路径 (指向包含 bin 和 lib 文件夹的 jdk 目录)
SOURCE_JDK="/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home"

# Flutter SDK 路径
FLUTTER_SDK="/Users/hrcao/Documents/flutter"

# 工具文件路径
SOURCE_TOOLS="/Users/$(whoami)/projects/pack/tools"

# 设置构建输出目录
BUILD_OUTPUT="dist"
# ==========================================

# --- 1. 检查环境 ---
echo "[1/8] 正在检查构建环境..."

# 检查 JDK 环境
if [ ! -f "$SOURCE_JDK/bin/java" ]; then
    echo "[错误] JDK 路径不存在 java: \"$SOURCE_JDK/bin/java\""
    echo "请修改脚本中的 SOURCE_JDK 变量。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 检查 Flutter 环境
if [ ! -f "$FLUTTER_SDK/bin/flutter" ]; then
    echo "[错误] Flutter SDK 路径不存在: \"$FLUTTER_SDK/bin/flutter\""
    echo "请修改脚本中的 FLUTTER_SDK 变量。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 检查 Maven 环境
if ! command -v mvn &> /dev/null; then
    echo "[错误] Maven 未找到，请确保 Maven 已添加到 PATH。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 检查 Python 环境
if ! command -v python3 &> /dev/null; then
    echo "[错误] Python 3 未找到，请确保 Python 3 已安装。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

echo "构建环境验证通过。"

# --- 2. 清理目录 ---
echo "[2/8] 清理旧文件..."
if [ -d "$BUILD_OUTPUT" ]; then
    rm -rf "$BUILD_OUTPUT"
fi

mkdir -p "$BUILD_OUTPUT/backend"
mkdir -p "$BUILD_OUTPUT/frontend"
mkdir -p "$BUILD_OUTPUT/jdk"

# --- 3. 构建后端 ---
echo "[3/8] 构建后端服务..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "[错误] Maven 打包失败！"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# --- 4. 构建前端 ---
echo "[4/8] 构建前端应用..."
export PATH="$FLUTTER_SDK/bin:$PATH"
cd clients/flutter-web-cli
flutter build web --release
if [ $? -ne 0 ]; then
    echo "[错误] Flutter 构建失败！"
    cd ../..
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi
cd ../..

# --- 5. 复制后端文件 ---
echo "[5/8] 复制后端文件..."
if [ ! -f "backend/target/backend-1.0.0.jar" ]; then
    echo "[错误] backend 目录下没找到 jar 包。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

cp "backend/target/backend-1.0.0.jar" "$BUILD_OUTPUT/backend/backend.jar"

# --- 6. 复制前端文件 ---
echo "[6/8] 复制前端文件..."
cp -r "clients/flutter-web-cli/build/web"/* "$BUILD_OUTPUT/frontend/"

# --- 7. 复制 JDK ---
echo "[7/9] 复制 JDK 运行时..."
if [ -d "$SOURCE_JDK/jre" ]; then
    cp -r "$SOURCE_JDK/jre"/* "$BUILD_OUTPUT/jdk/"
elif [ -d "$SOURCE_JDK" ]; then
    # 对于没有单独 jre 目录的 JDK 版本
    cp -r "$SOURCE_JDK"/* "$BUILD_OUTPUT/jdk/"
else
    echo "[错误] 无法找到 JDK 运行时目录。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# 二次检查复制结果
if [ ! -f "$BUILD_OUTPUT/jdk/bin/java" ]; then
    echo "[致命错误] JDK 复制失败！$BUILD_OUTPUT/jdk/bin/java 不存在。"
    echo "请检查是否有权限读取源目录。"
    echo "将在 10 秒后自动退出..."
    sleep 10
    exit 1
fi

# --- 8. 复制工具文件 ---
echo "[8/9] 复制工具文件..."
mkdir -p "$BUILD_OUTPUT/tools"
if [ -d "$SOURCE_TOOLS" ]; then
    cp -r "$SOURCE_TOOLS"/* "$BUILD_OUTPUT/tools/"
    echo "[成功] 工具文件复制完成"
else
    echo "[警告] 工具文件目录不存在: $SOURCE_TOOLS"
    echo "请检查工具文件路径是否正确"
fi

# --- 9. 生成启动和管理脚本 ---
echo "[9/9] 生成启动和管理脚本..."

# 生成后端启动脚本
cat > "$BUILD_OUTPUT/start-backend.sh" << 'EOF'
#!/bin/bash

echo "正在启动 FileManager Plus 后端服务..."
echo "服务地址: http://localhost:8080"
echo "按 Ctrl+C 停止服务"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

"$SCRIPT_DIR/jdk/bin/java" -Xms512m -Xmx1g -jar "$SCRIPT_DIR/backend/backend.jar"
EOF

# 生成前端启动脚本
cat > "$BUILD_OUTPUT/start-frontend.sh" << 'EOF'
#!/bin/bash

echo "正在启动 FileManager Plus 前端服务..."
echo "访问地址: http://localhost:8081"
echo "按 Ctrl+C 停止服务"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

python3 -m http.server 8081 --directory "$SCRIPT_DIR/frontend"
EOF

# 生成一键启动脚本
cat > "$BUILD_OUTPUT/start-all.sh" << 'EOF'
#!/bin/bash

echo "正在启动 FileManager Plus 服务..."
echo "后端服务地址: http://localhost:8080"
echo "前端访问地址: http://localhost:8081"
echo "按 Ctrl+C 停止服务"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 启动后端服务
open -a Terminal "$SCRIPT_DIR/start-backend.sh"

# 等待 3 秒
sleep 3

# 启动前端服务
open -a Terminal "$SCRIPT_DIR/start-frontend.sh"

echo "服务已启动，请在浏览器中访问 http://localhost:8081"
echo "如果需要停止服务，请关闭对应的终端窗口"
read -p "按 Enter 键退出..."
EOF

# 生成停止脚本
cat > "$BUILD_OUTPUT/stop-all.sh" << 'EOF'
#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 停止脚本 v1.0"
echo "      (macOS)"
echo "==========================================="

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
    read -p "按 Enter 键退出..."
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
read -p "按 Enter 键退出..."
EOF

# 生成重启脚本
cat > "$BUILD_OUTPUT/restart-all.sh" << 'EOF'
#!/bin/bash

# 设置编码以支持中文显示
export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

echo "==========================================="
echo "      FileManager Plus 重启脚本 v1.0"
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
    open -a Terminal "$SCRIPT_DIR/start-backend.sh"
else
    echo "[错误] 未找到 start-backend.sh 脚本"
    read -p "按 Enter 键退出..."
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
    open -a Terminal "$SCRIPT_DIR/start-frontend.sh"
else
    echo "[错误] 未找到 start-frontend.sh 脚本"
    read -p "按 Enter 键退出..."
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
read -p "按 Enter 键退出..."
EOF

# 设置脚本执行权限
chmod +x "$BUILD_OUTPUT/start-backend.sh"
chmod +x "$BUILD_OUTPUT/start-frontend.sh"
chmod +x "$BUILD_OUTPUT/start-all.sh"
chmod +x "$BUILD_OUTPUT/stop-all.sh"
chmod +x "$BUILD_OUTPUT/restart-all.sh"

echo ""
echo "==========================================="
echo "构建成功！"
echo "请进入 $BUILD_OUTPUT 文件夹运行启动脚本测试"
echo "脚本列表："
echo "1. start-backend.sh - 启动后端服务"
echo "2. start-frontend.sh - 启动前端服务"
echo "3. start-all.sh - 一键启动所有服务"
echo "4. stop-all.sh - 停止所有服务"
echo "5. restart-all.sh - 重启所有服务"
echo "==========================================="
echo "将在 10 秒后自动退出..."
sleep 10
