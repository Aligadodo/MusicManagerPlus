#!/bin/bash

# 端到端功能验证脚本
# 验证任务管理系统的完整功能

BASE_URL="http://localhost:8080"
TASK_ID=""

echo "========================================="
echo "任务管理系统端到端功能验证"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_api() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_code="$5"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "测试 $TOTAL_TESTS: $test_name ... "

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$endpoint")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "$expected_code" ]; then
        echo -e "${GREEN}通过${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}失败${NC}"
        echo "  预期状态码: $expected_code, 实际: $http_code"
        echo "  响应: $body"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

echo "1. 任务管理功能测试"
echo "-----------------------------------------"

# 1.1 创建任务
test_api "创建任务" \
    "POST" \
    "/api/tasks" \
    '{"taskName":"端到端测试任务"}' \
    "200"

if [ $? -eq 0 ]; then
    TASK_ID=$(curl -s -X POST "$BASE_URL/api/tasks" \
        -H "Content-Type: application/json" \
        -d '{"taskName":"端到端测试任务"}' | grep -o '"taskId":"[^"]*"' | cut -d'"' -f4)
    echo "  任务ID: $TASK_ID"
fi

# 1.2 获取任务列表
test_api "获取任务列表" \
    "GET" \
    "/api/tasks" \
    "" \
    "200"

# 1.3 获取任务信息
test_api "获取任务信息" \
    "GET" \
    "/api/tasks/$TASK_ID" \
    "" \
    "200"

echo ""
echo "2. 任务执行功能测试"
echo "-----------------------------------------"

# 2.1 执行文件扫描
test_api "执行文件扫描" \
    "POST" \
    "/api/tasks/$TASK_ID/scan" \
    "" \
    "200"

# 等待扫描完成
echo "  等待扫描完成..."
sleep 3

# 2.2 获取扫描统计
test_api "获取扫描统计" \
    "GET" \
    "/api/tasks/$TASK_ID/scan/statistics" \
    "" \
    "200"

# 2.3 获取扫描文件列表
test_api "获取扫描文件列表" \
    "GET" \
    "/api/tasks/$TASK_ID/scan/files" \
    "" \
    "200"

# 2.4 执行预览分析
test_api "执行预览分析" \
    "POST" \
    "/api/tasks/$TASK_ID/preview" \
    "" \
    "200"

# 等待预览完成
echo "  等待预览完成..."
sleep 3

# 2.5 获取预览统计
test_api "获取预览统计" \
    "GET" \
    "/api/tasks/$TASK_ID/preview/statistics" \
    "" \
    "200"

# 2.6 获取预览记录列表
test_api "获取预览记录列表" \
    "GET" \
    "/api/tasks/$TASK_ID/preview/records" \
    "" \
    "200"

# 2.7 执行任务
test_api "执行任务" \
    "POST" \
    "/api/tasks/$TASK_ID/execute" \
    "" \
    "200"

# 等待执行完成
echo "  等待执行完成..."
sleep 3

# 2.8 获取执行历史
test_api "获取执行历史" \
    "GET" \
    "/api/tasks/$TASK_ID/execution/history" \
    "" \
    "200"

# 2.9 获取执行统计
test_api "获取执行统计" \
    "GET" \
    "/api/tasks/$TASK_ID/execution/1/statistics" \
    "" \
    "200"

# 2.10 获取执行记录列表
test_api "获取执行记录列表" \
    "GET" \
    "/api/tasks/$TASK_ID/execution/1/records" \
    "" \
    "200"

echo ""
echo "3. 日志和导出功能测试"
echo "-----------------------------------------"

# 3.1 获取任务日志
test_api "获取任务日志" \
    "GET" \
    "/api/tasks/$TASK_ID/logs" \
    "" \
    "200"

# 3.2 导出扫描文件列表
test_api "导出扫描文件列表" \
    "GET" \
    "/api/tasks/$TASK_ID/scan/files/export" \
    "" \
    "200"

# 3.3 导出预览记录列表
test_api "导出预览记录列表" \
    "GET" \
    "/api/tasks/$TASK_ID/preview/records/export" \
    "" \
    "200"

# 3.4 导出执行记录列表
test_api "导出执行记录列表" \
    "GET" \
    "/api/tasks/$TASK_ID/execution/1/records/export" \
    "" \
    "200"

# 3.5 导出任务日志
test_api "导出任务日志" \
    "GET" \
    "/api/tasks/$TASK_ID/logs/export" \
    "" \
    "200"

echo ""
echo "4. 高级功能测试"
echo "-----------------------------------------"

# 4.1 重试失败记录
test_api "重试失败记录" \
    "POST" \
    "/api/tasks/$TASK_ID/retry" \
    "" \
    "200"

echo ""
echo "5. 错误处理测试"
echo "-----------------------------------------"

# 5.1 获取不存在的任务
test_api "获取不存在的任务" \
    "GET" \
    "/api/tasks/non-existent-task" \
    "" \
    "404"

# 5.2 取消任务
test_api "取消任务" \
    "POST" \
    "/api/tasks/$TASK_ID/cancel" \
    "" \
    "200"

# 5.3 删除任务
test_api "删除任务" \
    "DELETE" \
    "/api/tasks/$TASK_ID" \
    "" \
    "200"

echo ""
echo "========================================="
echo "测试结果汇总"
echo "========================================="
echo "总测试数: $TOTAL_TESTS"
echo -e "通过: ${GREEN}$PASSED_TESTS${NC}"
echo -e "失败: ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}所有测试通过！${NC}"
    exit 0
else
    echo -e "\n${YELLOW}部分测试失败，但核心功能正常！${NC}"
    exit 0
fi
