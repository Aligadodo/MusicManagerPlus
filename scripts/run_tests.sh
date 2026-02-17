#!/bin/bash

# 任务执行全链路测试运行脚本
# 用于运行前后端测试用例

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="/Users/hrcao/Documents/MusicManagerPlus"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/clients/flutter-web-cli"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}任务执行全链路测试运行脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 解析命令行参数
TEST_TYPE=${1:-all}

case $TEST_TYPE in
    backend)
        echo -e "${YELLOW}运行后端测试...${NC}"
        cd "$BACKEND_DIR"
        mvn test
        ;;
    frontend)
        echo -e "${YELLOW}运行前端测试...${NC}"
        cd "$FRONTEND_DIR"
        flutter test
        ;;
    unit)
        echo -e "${YELLOW}运行后端单元测试...${NC}"
        cd "$BACKEND_DIR"
        mvn test -Dtest=TaskInfoServiceTest
        mvn test -Dtest=ChangeRecordServiceTest
        ;;
    integration)
        echo -e "${YELLOW}运行后端集成测试...${NC}"
        cd "$BACKEND_DIR"
        mvn test -Dtest=TaskExecutionE2ETest
        ;;
    models)
        echo -e "${YELLOW}运行前端模型测试...${NC}"
        cd "$FRONTEND_DIR"
        flutter test test/task_models_test.dart
        ;;
    all)
        echo -e "${YELLOW}运行所有测试...${NC}"
        
        echo ""
        echo -e "${GREEN}1. 运行后端单元测试...${NC}"
        cd "$BACKEND_DIR"
        mvn test -Dtest=TaskInfoServiceTest
        mvn test -Dtest=ChangeRecordServiceTest
        
        echo ""
        echo -e "${GREEN}2. 运行前端模型测试...${NC}"
        cd "$FRONTEND_DIR"
        flutter test test/task_models_test.dart
        
        echo ""
        echo -e "${GREEN}3. 运行后端集成测试...${NC}"
        cd "$BACKEND_DIR"
        mvn test -Dtest=TaskExecutionE2ETest
        
        echo ""
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}所有测试完成！${NC}"
        echo -e "${GREEN}========================================${NC}"
        ;;
    *)
        echo -e "${RED}用法: $0 [backend|frontend|unit|integration|models|all]${NC}"
        echo ""
        echo "选项:"
        echo "  backend    - 运行所有后端测试"
        echo "  frontend   - 运行所有前端测试"
        echo "  unit       - 运行后端单元测试"
        echo "  integration - 运行后端集成测试"
        echo "  models     - 运行前端模型测试"
        echo "  all        - 运行所有测试（默认）"
        echo ""
        echo "示例:"
        echo "  $0          # 运行所有测试"
        echo "  $0 backend   # 只运行后端测试"
        echo "  $0 frontend  # 只运行前端测试"
        echo "  $0 unit      # 只运行单元测试"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}测试完成！${NC}"
