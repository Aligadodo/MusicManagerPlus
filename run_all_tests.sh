#!/bin/bash

# =============================================================================
# MusicManagerPlus 统一测试脚本
# =============================================================================
# 功能：运行前后端所有测试用例，生成测试报告
# 使用方法：./run_all_tests.sh [选项]
# 选项：
#   --frontend-only   仅运行前端测试
#   --backend-only    仅运行后端测试
#   --quick           快速测试（跳过集成测试）
#   --full            完整测试（包含集成测试）
#   --report          生成详细测试报告
#   --help            显示帮助信息
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="/Users/hrcao/Documents/MusicManagerPlus"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/clients/flutter-web-cli"
REPORT_DIR="$PROJECT_ROOT/test_reports"

# 时间戳
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$REPORT_DIR/test_report_$TIMESTAMP.txt"
HTML_REPORT="$REPORT_DIR/test_report_$TIMESTAMP.html"

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# 测试结果数组
declare -a TEST_RESULTS
declare -a TEST_NAMES
declare -a TEST_DURATIONS

# =============================================================================
# 函数定义
# =============================================================================

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 打印分隔线
print_separator() {
    echo "============================================================================"
}

# 创建报告目录
create_report_dir() {
    if [ ! -d "$REPORT_DIR" ]; then
        mkdir -p "$REPORT_DIR"
        print_info "创建报告目录: $REPORT_DIR"
    fi
}

# 初始化报告文件
init_report() {
    cat > "$REPORT_FILE" << EOF
============================================================================
MusicManagerPlus 统一测试报告
============================================================================
测试时间: $(date "+%Y-%m-%d %H:%M:%S")
测试环境:
  - 操作系统: $(uname -s)
  - 主机名: $(hostname)
  - Java版本: $(java -version 2>&1 | head -n 1)
  - Maven版本: $(mvn -version | head -n 1)
  - Flutter版本: $(cd "$FRONTEND_DIR" && flutter --version 2>&1 | head -n 1)

============================================================================
EOF
}

# 添加测试结果到报告
add_test_result() {
    local test_name="$1"
    local test_status="$2"
    local test_duration="$3"
    local test_output="$4"
    
    echo "" >> "$REPORT_FILE"
    echo "测试名称: $test_name" >> "$REPORT_FILE"
    echo "测试状态: $test_status" >> "$REPORT_FILE"
    echo "测试时长: $test_duration" >> "$REPORT_FILE"
    echo "测试输出:" >> "$REPORT_FILE"
    echo "$test_output" | sed 's/^/  /' >> "$REPORT_FILE"
    
    TEST_NAMES+=("$test_name")
    TEST_RESULTS+=("$test_status")
    TEST_DURATIONS+=("$test_duration")
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$test_status" == "PASSED" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
    elif [ "$test_status" == "FAILED" ]; then
        FAILED_TESTS=$((FAILED_TESTS + 1))
    elif [ "$test_status" == "SKIPPED" ]; then
        SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
    fi
}

# 生成HTML报告
generate_html_report() {
    cat > "$HTML_REPORT" << EOF
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MusicManagerPlus 测试报告</title>
    <style>
        body {
            font-family: 'Microsoft YaHei', Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 2px solid #4CAF50;
            padding-bottom: 10px;
        }
        h2 {
            color: #555;
            margin-top: 30px;
        }
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }
        .summary-card {
            background-color: #f9f9f9;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            border-left: 4px solid #4CAF50;
        }
        .summary-card.total { border-left-color: #2196F3; }
        .summary-card.passed { border-left-color: #4CAF50; }
        .summary-card.failed { border-left-color: #f44336; }
        .summary-card.skipped { border-left-color: #FF9800; }
        .summary-card h3 {
            margin: 0 0 10px 0;
            font-size: 14px;
            color: #666;
        }
        .summary-card .value {
            font-size: 32px;
            font-weight: bold;
            color: #333;
        }
        .test-item {
            margin: 10px 0;
            padding: 15px;
            border-radius: 4px;
            border-left: 4px solid #ddd;
        }
        .test-item.passed { background-color: #e8f5e9; border-left-color: #4CAF50; }
        .test-item.failed { background-color: #ffebee; border-left-color: #f44336; }
        .test-item.skipped { background-color: #fff3e0; border-left-color: #FF9800; }
        .test-name {
            font-weight: bold;
            margin-bottom: 5px;
        }
        .test-meta {
            font-size: 12px;
            color: #666;
        }
        .test-output {
            margin-top: 10px;
            padding: 10px;
            background-color: #f5f5f5;
            border-radius: 4px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            white-space: pre-wrap;
            word-wrap: break-word;
            max-height: 200px;
            overflow-y: auto;
        }
        .progress-bar {
            height: 20px;
            background-color: #e0e0e0;
            border-radius: 10px;
            overflow: hidden;
            margin: 20px 0;
        }
        .progress-bar .passed {
            height: 100%;
            background-color: #4CAF50;
            float: left;
        }
        .progress-bar .failed {
            height: 100%;
            background-color: #f44336;
            float: left;
        }
        .progress-bar .skipped {
            height: 100%;
            background-color: #FF9800;
            float: left;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>MusicManagerPlus 测试报告</h1>
        
        <div class="summary">
            <div class="summary-card total">
                <h3>总测试数</h3>
                <div class="value">$TOTAL_TESTS</div>
            </div>
            <div class="summary-card passed">
                <h3>通过</h3>
                <div class="value">$PASSED_TESTS</div>
            </div>
            <div class="summary-card failed">
                <h3>失败</h3>
                <div class="value">$FAILED_TESTS</div>
            </div>
            <div class="summary-card skipped">
                <h3>跳过</h3>
                <div class="value">$SKIPPED_TESTS</div>
            </div>
        </div>
        
        <div class="progress-bar">
EOF

    # 计算百分比
    if [ $TOTAL_TESTS -gt 0 ]; then
        PASSED_PERCENT=$((PASSED_TESTS * 100 / TOTAL_TESTS))
        FAILED_PERCENT=$((FAILED_TESTS * 100 / TOTAL_TESTS))
        SKIPPED_PERCENT=$((SKIPPED_TESTS * 100 / TOTAL_TESTS))
        
        echo "            <div class=\"passed\" style=\"width: ${PASSED_PERCENT}%\"></div>" >> "$HTML_REPORT"
        echo "            <div class=\"failed\" style=\"width: ${FAILED_PERCENT}%\"></div>" >> "$HTML_REPORT"
        echo "            <div class=\"skipped\" style=\"width: ${SKIPPED_PERCENT}%\"></div>" >> "$HTML_REPORT"
    fi
    
    cat >> "$HTML_REPORT" << EOF
        </div>
        
        <h2>测试详情</h2>
EOF

    # 添加测试详情
    for i in "${!TEST_NAMES[@]}"; do
        local test_name="${TEST_NAMES[$i]}"
        local test_status="${TEST_RESULTS[$i]}"
        local test_duration="${TEST_DURATIONS[$i]}"
        
        echo "        <div class=\"test-item $test_status\">" >> "$HTML_REPORT"
        echo "            <div class=\"test-name\">$test_name</div>" >> "$HTML_REPORT"
        echo "            <div class=\"test-meta\">状态: $test_status | 时长: $test_duration</div>" >> "$HTML_REPORT"
        echo "        </div>" >> "$HTML_REPORT"
    done
    
    cat >> "$HTML_REPORT" << EOF
    </div>
</body>
</html>
EOF

    print_success "HTML报告已生成: $HTML_REPORT"
}

# 运行前端接口兼容性测试
run_frontend_api_tests() {
    print_info "运行前端接口兼容性测试..."
    local start_time=$(date +%s)
    
    if [ -f "$PROJECT_ROOT/test_frontend_api.sh" ]; then
        local output=$("$PROJECT_ROOT/test_frontend_api.sh" 2>&1)
        local exit_code=$?
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        if [ $exit_code -eq 0 ]; then
            add_test_result "前端接口兼容性测试" "PASSED" "${duration}s" "$output"
            print_success "前端接口兼容性测试通过 (耗时: ${duration}s)"
        else
            add_test_result "前端接口兼容性测试" "FAILED" "${duration}s" "$output"
            print_error "前端接口兼容性测试失败 (耗时: ${duration}s)"
        fi
    else
        add_test_result "前端接口兼容性测试" "SKIPPED" "0s" "测试脚本不存在"
        print_warning "前端接口兼容性测试脚本不存在，跳过"
    fi
}

# 运行后端单元测试
run_backend_unit_tests() {
    print_info "运行后端单元测试..."
    local start_time=$(date +%s)
    
    cd "$BACKEND_DIR"
    local output=$(mvn test -DskipTests=false 2>&1)
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ $exit_code -eq 0 ]; then
        add_test_result "后端单元测试" "PASSED" "${duration}s" "$output"
        print_success "后端单元测试通过 (耗时: ${duration}s)"
    else
        add_test_result "后端单元测试" "FAILED" "${duration}s" "$output"
        print_error "后端单元测试失败 (耗时: ${duration}s)"
    fi
}

# 运行后端集成测试
run_backend_integration_tests() {
    print_info "运行后端集成测试..."
    local start_time=$(date +%s)
    
    cd "$BACKEND_DIR"
    local output=$(mvn verify -DskipITs=false 2>&1)
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ $exit_code -eq 0 ]; then
        add_test_result "后端集成测试" "PASSED" "${duration}s" "$output"
        print_success "后端集成测试通过 (耗时: ${duration}s)"
    else
        add_test_result "后端集成测试" "FAILED" "${duration}s" "$output"
        print_error "后端集成测试失败 (耗时: ${duration}s)"
    fi
}

# 运行前端单元测试
run_frontend_unit_tests() {
    print_info "运行前端单元测试..."
    local start_time=$(date +%s)
    
    cd "$FRONTEND_DIR"
    local output=$(flutter test 2>&1)
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ $exit_code -eq 0 ]; then
        add_test_result "前端单元测试" "PASSED" "${duration}s" "$output"
        print_success "前端单元测试通过 (耗时: ${duration}s)"
    else
        add_test_result "前端单元测试" "FAILED" "${duration}s" "$output"
        print_error "前端单元测试失败 (耗时: ${duration}s)"
    fi
}

# 运行代码质量检查
run_code_quality_checks() {
    print_info "运行代码质量检查..."
    local start_time=$(date +%s)
    
    cd "$BACKEND_DIR"
    local output=$(mvn checkstyle:check 2>&1)
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ $exit_code -eq 0 ]; then
        add_test_result "代码质量检查" "PASSED" "${duration}s" "$output"
        print_success "代码质量检查通过 (耗时: ${duration}s)"
    else
        add_test_result "代码质量检查" "FAILED" "${duration}s" "$output"
        print_error "代码质量检查失败 (耗时: ${duration}s)"
    fi
}

# 打印测试摘要
print_summary() {
    print_separator
    echo ""
    print_info "测试摘要"
    print_separator
    echo "总测试数: $TOTAL_TESTS"
    echo "通过: $PASSED_TESTS"
    echo "失败: $FAILED_TESTS"
    echo "跳过: $SKIPPED_TESTS"
    
    if [ $TOTAL_TESTS -gt 0 ]; then
        local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
        echo "成功率: ${success_rate}%"
    fi
    
    print_separator
    echo ""
    
    if [ $FAILED_TESTS -gt 0 ]; then
        print_error "有 $FAILED_TESTS 个测试失败，请查看详细报告"
        print_info "详细报告: $REPORT_FILE"
        print_info "HTML报告: $HTML_REPORT"
        return 1
    else
        print_success "所有测试通过！"
        print_info "详细报告: $REPORT_FILE"
        print_info "HTML报告: $HTML_REPORT"
        return 0
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
  --frontend-only   仅运行前端测试
  --backend-only    仅运行后端测试
  --quick           快速测试（跳过集成测试）
  --full            完整测试（包含集成测试）
  --report          生成详细测试报告
  --help            显示帮助信息

示例:
  $0                    # 运行所有测试
  $0 --backend-only      # 仅运行后端测试
  $0 --quick            # 快速测试
  $0 --full --report    # 完整测试并生成报告

EOF
}

# =============================================================================
# 主程序
# =============================================================================

# 解析命令行参数
RUN_FRONTEND=true
RUN_BACKEND=true
RUN_INTEGRATION=false
GENERATE_REPORT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --frontend-only)
            RUN_BACKEND=false
            shift
            ;;
        --backend-only)
            RUN_FRONTEND=false
            shift
            ;;
        --quick)
            RUN_INTEGRATION=false
            shift
            ;;
        --full)
            RUN_INTEGRATION=true
            shift
            ;;
        --report)
            GENERATE_REPORT=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 打印开始信息
print_separator
echo "MusicManagerPlus 统一测试脚本"
print_separator
echo "开始时间: $(date "+%Y-%m-%d %H:%M:%S")"
print_separator
echo ""

# 创建报告目录
create_report_dir

# 初始化报告
init_report

# 运行测试
if [ "$RUN_BACKEND" = true ]; then
    print_info "========== 后端测试 =========="
    echo ""
    run_backend_unit_tests
    
    if [ "$RUN_INTEGRATION" = true ]; then
        run_backend_integration_tests
    fi
    
    echo ""
fi

if [ "$RUN_FRONTEND" = true ]; then
    print_info "========== 前端测试 =========="
    echo ""
    run_frontend_api_tests
    run_frontend_unit_tests
    echo ""
fi

# 代码质量检查
print_info "========== 代码质量检查 =========="
echo ""
run_code_quality_checks
echo ""

# 生成HTML报告
if [ "$GENERATE_REPORT" = true ] || [ $FAILED_TESTS -gt 0 ]; then
    generate_html_report
fi

# 添加摘要到报告
cat >> "$REPORT_FILE" << EOF

============================================================================
测试摘要
============================================================================
总测试数: $TOTAL_TESTS
通过: $PASSED_TESTS
失败: $FAILED_TESTS
跳过: $SKIPPED_TESTS

EOF

if [ $TOTAL_TESTS -gt 0 ]; then
    local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo "成功率: ${success_rate}%" >> "$REPORT_FILE"
fi

cat >> "$REPORT_FILE" << EOF
结束时间: $(date "+%Y-%m-%d %H:%M:%S")
============================================================================
EOF

# 打印摘要
print_summary
exit $?
