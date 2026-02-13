#!/bin/bash

# =============================================================================
# MusicManagerPlus 测试脚本使用示例
# =============================================================================
# 本脚本展示了如何在实际开发中使用统一测试脚本
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

# =============================================================================
# 示例1：日常开发流程
# =============================================================================

example1_daily_development() {
    print_info "示例1：日常开发流程"
    print_separator
    
    echo "场景：修改了FileCollectionStrategy的某个功能"
    echo ""
    echo "步骤1：修改代码"
    echo "  cd $PROJECT_ROOT/backend"
    echo "  # 修改 FileCollectionStrategy.java"
    echo ""
    echo "步骤2：运行快速测试"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --quick"
    echo ""
    echo "步骤3：如果测试通过，提交代码"
    echo "  git add ."
    echo "  git commit -m 'fix: 修复FileCollectionStrategy的bug'"
    echo ""
    echo "步骤4：推送代码，触发CI/CD测试"
    echo "  git push"
    echo ""
}

# =============================================================================
# 示例2：提交代码前测试
# =============================================================================

example2_before_commit() {
    print_info "示例2：提交代码前测试"
    print_separator
    
    echo "场景：准备提交代码到主分支"
    echo ""
    echo "步骤1：确保所有更改已暂存"
    echo "  git status"
    echo "  git add ."
    echo ""
    echo "步骤2：运行完整测试"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --full --report"
    echo ""
    echo "步骤3：查看测试报告"
    echo "  cat test_reports/test_report_*.txt | tail -n 20"
    echo "  open test_reports/test_report_*.html"
    echo ""
    echo "步骤4：如果所有测试通过，提交代码"
    echo "  git commit -m 'feat: 添加新功能'"
    echo "  git push"
    echo ""
}

# =============================================================================
# 示例3：API接口变更后测试
# =============================================================================

example3_api_change() {
    print_info "示例3：API接口变更后测试"
    print_separator
    
    echo "场景：修改了后端API接口"
    echo ""
    echo "步骤1：修改后端API"
    echo "  cd $PROJECT_ROOT/backend"
    echo "  # 修改 StrategyController.java"
    echo ""
    echo "步骤2：重启后端服务"
    echo "  # 停止旧服务"
    echo "  # 启动新服务"
    echo "  mvn spring-boot:run"
    echo ""
    echo "步骤3：运行前端接口兼容性测试"
    echo "  cd $PROJECT_ROOT"
    echo "  ./test_frontend_api.sh"
    echo ""
    echo "步骤4：如果测试通过，运行完整测试"
    echo "  ./run_all_tests.sh --full --report"
    echo ""
    echo "步骤5：查看测试报告，确保没有兼容性问题"
    echo "  open test_reports/test_report_*.html"
    echo ""
}

# =============================================================================
# 示例4：只修改了后端代码
# =============================================================================

example4_backend_only() {
    print_info "示例4：只修改了后端代码"
    print_separator
    
    echo "场景：只修改了后端代码，前端没有变化"
    echo ""
    echo "步骤1：修改后端代码"
    echo "  cd $PROJECT_ROOT/backend"
    echo "  # 修改某个策略类"
    echo ""
    echo "步骤2：只运行后端测试"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --backend-only"
    echo ""
    echo "步骤3：如果测试通过，提交代码"
    echo "  git add backend/"
    echo "  git commit -m 'fix: 修复后端bug'"
    echo "  git push"
    echo ""
}

# =============================================================================
# 示例5：只修改了前端代码
# =============================================================================

example5_frontend_only() {
    print_info "示例5：只修改了前端代码"
    print_separator
    
    echo "场景：只修改了前端代码，后端没有变化"
    echo ""
    echo "步骤1：修改前端代码"
    echo "  cd $PROJECT_ROOT/clients/flutter-web-cli"
    echo "  # 修改某个widget"
    echo ""
    echo "步骤2：只运行前端测试"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --frontend-only"
    echo ""
    echo "步骤3：如果测试通过，提交代码"
    echo "  git add clients/flutter-web-cli/"
    echo "  git commit -m 'fix: 修复前端bug'"
    echo "  git push"
    echo ""
}

# =============================================================================
# 示例6：添加新策略
# =============================================================================

example6_add_new_strategy() {
    print_info "示例6：添加新策略"
    print_separator
    
    echo "场景：添加一个新的策略类"
    echo ""
    echo "步骤1：创建策略类"
    echo "  cd $PROJECT_ROOT/backend/src/main/java/com/filemanager/plugin/impl"
    echo "  # 创建 NewStrategy.java"
    echo ""
    echo "步骤2：创建策略测试类"
    echo "  cd $PROJECT_ROOT/backend/src/test/java/com/filemanager/backend/service"
    echo "  # 创建 NewStrategyTest.java"
    echo ""
    echo "步骤3：在StrategyServiceImpl中注册新策略"
    echo "  cd $PROJECT_ROOT/backend/src/main/java/com/filemanager/backend/service/impl"
    echo "  # 修改 StrategyServiceImpl.java"
    echo ""
    echo "步骤4：运行后端测试"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --backend-only"
    echo ""
    echo "步骤5：如果测试通过，运行完整测试"
    echo "  ./run_all_tests.sh --full --report"
    echo ""
    echo "步骤6：查看测试报告"
    echo "  open test_reports/test_report_*.html"
    echo ""
    echo "步骤7：如果所有测试通过，提交代码"
    echo "  git add ."
    echo "  git commit -m 'feat: 添加新策略'"
    echo "  git push"
    echo ""
}

# =============================================================================
# 示例7：修复Bug
# =============================================================================

example7_fix_bug() {
    print_info "示例7：修复Bug"
    print_separator
    
    echo "场景：修复一个已知的Bug"
    echo ""
    echo "步骤1：创建测试用例复现Bug"
    echo "  cd $PROJECT_ROOT/backend/src/test/java/com/filemanager/backend/service"
    echo "  # 在 FileCollectionStrategyTest.java 中添加测试方法"
    echo "  @Test"
    echo "  public void testBugReproduction() {"
    echo "      // 复现Bug的测试代码"
    echo "  }"
    echo ""
    echo "步骤2：运行测试，确认Bug存在"
    echo "  cd $PROJECT_ROOT/backend"
    echo "  mvn test -Dtest=FileCollectionStrategyTest#testBugReproduction"
    echo ""
    echo "步骤3：修复Bug"
    echo "  # 修改 FileCollectionStrategy.java"
    echo ""
    echo "步骤4：运行测试，验证修复"
    echo "  mvn test -Dtest=FileCollectionStrategyTest#testBugReproduction"
    echo ""
    echo "步骤5：运行所有测试，确保没有引入新的问题"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --full --report"
    echo ""
    echo "步骤6：查看测试报告"
    echo "  open test_reports/test_report_*.html"
    echo ""
    echo "步骤7：如果所有测试通过，提交代码"
    echo "  git add ."
    echo "  git commit -m 'fix: 修复FileCollectionStrategy的bug'"
    echo "  git push"
    echo ""
}

# =============================================================================
# 示例8：代码重构
# =============================================================================

example8_refactor() {
    print_info "示例8：代码重构"
    print_separator
    
    echo "场景：重构某个策略的代码"
    echo ""
    echo "步骤1：运行测试，确保所有测试通过"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --full --report"
    echo ""
    echo "步骤2：记录测试结果"
    echo "  cp test_reports/test_report_*.txt test_reports/before_refactor.txt"
    echo ""
    echo "步骤3：重构代码"
    echo "  cd $PROJECT_ROOT/backend"
    echo "  # 重构 FileCollectionStrategy.java"
    echo ""
    echo "步骤4：运行测试，验证重构没有破坏功能"
    echo "  cd $PROJECT_ROOT"
    echo "  ./run_all_tests.sh --full --report"
    echo ""
    echo "步骤5：对比测试结果"
    echo "  diff test_reports/before_refactor.txt test_reports/test_report_*.txt"
    echo ""
    echo "步骤6：如果测试通过，提交代码"
    echo "  git add ."
    echo "  git commit -m 'refactor: 重构FileCollectionStrategy'"
    echo "  git push"
    echo ""
}

# =============================================================================
# 示例9：CI/CD集成
# =============================================================================

example9_ci_cd() {
    print_info "示例9：CI/CD集成"
    print_separator
    
    echo "场景：在GitHub Actions中自动运行测试"
    echo ""
    echo "步骤1：创建GitHub Actions工作流文件"
    echo "  mkdir -p .github/workflows"
    echo "  # 创建 .github/workflows/test.yml"
    echo ""
    echo "步骤2：配置工作流"
    echo "  name: Run Tests"
    echo "  on: [push, pull_request]"
    echo "  jobs:"
    echo "    test:"
    echo "      runs-on: ubuntu-latest"
    echo "      steps:"
    echo "        - uses: actions/checkout@v2"
    echo "        - name: Set up JDK"
    echo "          uses: actions/setup-java@v2"
    echo "          with:"
    echo "            java-version: '11'"
    echo "        - name: Set up Flutter"
    echo "          uses: subosito/flutter-action@v2"
    echo "          with:"
    echo "            flutter-version: '3.16.0'"
    echo "        - name: Run all tests"
    echo "          run: ./run_all_tests.sh --full --report"
    echo "        - name: Upload test reports"
    echo "          uses: actions/upload-artifact@v2"
    echo "          with:"
    echo "            name: test-reports"
    echo "            path: test_reports/"
    echo ""
    echo "步骤3：提交工作流文件"
    echo "  git add .github/workflows/test.yml"
    echo "  git commit -m 'ci: 添加GitHub Actions工作流'"
    echo "  git push"
    echo ""
    echo "步骤4：每次push或PR都会自动运行测试"
    echo ""
}

# =============================================================================
# 示例10：测试失败处理
# =============================================================================

example10_test_failure() {
    print_info "示例10：测试失败处理"
    print_separator
    
    echo "场景：测试失败了，如何处理"
    echo ""
    echo "步骤1：查看测试报告"
    echo "  cd $PROJECT_ROOT"
    echo "  cat test_reports/test_report_*.txt | grep -A 20 'FAILED'"
    echo ""
    echo "步骤2：在HTML报告中查看详细信息"
    echo "  open test_reports/test_report_*.html"
    echo ""
    echo "步骤3：定位失败的测试"
    echo "  # 在HTML报告中找到红色的失败测试"
    echo ""
    echo "步骤4：分析失败原因"
    echo "  # 查看测试的详细输出"
    echo "  # 分析代码逻辑"
    echo ""
    echo "步骤5：修复问题"
    echo "  # 修复代码或测试用例"
    echo ""
    echo "步骤6：重新运行测试"
    echo "  ./run_all_tests.sh --quick"
    echo ""
    echo "步骤7：如果测试通过，运行完整测试"
    echo "  ./run_all_tests.sh --full --report"
    echo ""
    echo "步骤8：如果所有测试通过，提交代码"
    echo "  git add ."
    echo "  git commit -m 'fix: 修复测试失败'"
    echo "  git push"
    echo ""
}

# =============================================================================
# 辅助函数
# =============================================================================

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

print_separator() {
    echo "============================================================================"
}

# =============================================================================
# 主程序
# =============================================================================

# 显示菜单
show_menu() {
    echo ""
    print_separator
    echo "MusicManagerPlus 测试脚本使用示例"
    print_separator
    echo ""
    echo "请选择要查看的示例："
    echo ""
    echo "1. 日常开发流程"
    echo "2. 提交代码前测试"
    echo "3. API接口变更后测试"
    echo "4. 只修改了后端代码"
    echo "5. 只修改了前端代码"
    echo "6. 添加新策略"
    echo "7. 修复Bug"
    echo "8. 代码重构"
    echo "9. CI/CD集成"
    echo "10. 测试失败处理"
    echo "0. 退出"
    echo ""
    echo -n "请输入选项 (0-10): "
}

# 主循环
while true; do
    show_menu
    read choice
    
    case $choice in
        1)
            example1_daily_development
            ;;
        2)
            example2_before_commit
            ;;
        3)
            example3_api_change
            ;;
        4)
            example4_backend_only
            ;;
        5)
            example5_frontend_only
            ;;
        6)
            example6_add_new_strategy
            ;;
        7)
            example7_fix_bug
            ;;
        8)
            example8_refactor
            ;;
        9)
            example9_ci_cd
            ;;
        10)
            example10_test_failure
            ;;
        0)
            print_success "退出示例"
            exit 0
            ;;
        *)
            print_error "无效选项，请重新输入"
            ;;
    esac
    
    echo ""
    echo -n "按Enter键继续..."
    read
done
