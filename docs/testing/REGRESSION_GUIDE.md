# 迭代回归操作指南

## 概述

本指南提供了在项目迭代过程中进行回归测试的详细操作步骤，确保每次代码变更后系统的稳定性和功能正确性。

## 回归测试时机

### 必须执行回归测试的情况
1. ✅ **每次代码提交到主分支**
2. ✅ **每次合并Pull Request**
3. ✅ **每次发布新版本**
4. ✅ **重大功能变更后**
5. ✅ **重构代码后**
6. ✅ **修复Bug后**

### 建议执行回归测试的情况
1. 📌 **每周至少一次**
2. 📌 **性能优化后**
3. 📌 **依赖库升级后**
4. 📌 **配置变更后**

## 回归测试级别

### 1. 快速回归测试（每次提交）

**执行时间**: 10-15分钟
**适用场景**: 每次代码提交、小的Bug修复

**测试范围**:
- 核心单元测试
- 关键功能测试
- 基本集成测试

**通过标准**: 通过率 ≥ 90%

**执行命令**:
```bash
# 运行核心单元测试
cd /Users/hrcao/Documents/MusicManagerPlus/backend
mvn test -Dtest=TaskInfoServiceTest
mvn test -Dtest=ChangeRecordServiceTest

cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter test test/task_models_test.dart
```

### 2. 标准回归测试（每次合并）

**执行时间**: 30-45分钟
**适用场景**: 合并PR、功能开发完成

**测试范围**:
- 所有单元测试
- 关键集成测试
- 端到端测试

**通过标准**: 通过率 ≥ 95%

**执行命令**:
```bash
# 运行所有测试
/Users/hrcao/Documents/MusicManagerPlus/scripts/run_tests.sh all
```

### 3. 完整回归测试（每次发布）

**执行时间**: 1-2小时
**适用场景**: 发布新版本、重大变更

**测试范围**:
- 所有单元测试
- 所有集成测试
- 所有端到端测试
- 性能测试
- 安全测试

**通过标准**: 通过率 ≥ 98%

**执行命令**:
```bash
# 运行所有测试
/Users/hrcao/Documents/MusicManagerPlus/scripts/run_tests.sh all

# 手动验证
# 1. 启动服务
# 2. 执行完整流程
# 3. 验证功能
# 4. 检查性能
```

## 回归测试流程

### 阶段1: 测试前准备

#### 1.1 环境检查
```bash
# 检查后端服务
curl http://localhost:8080/actuator/health

# 检查前端服务
curl http://localhost:8081

# 检查数据库
cd /Users/hrcao/Documents/MusicManagerPlus/backend
sqlite3 data/music_manager.db "SELECT COUNT(*) FROM task_info;"
```

**检查清单**:
- [ ] 后端服务正常运行（端口8080）
- [ ] 前端服务正常运行（端口8081）
- [ ] 数据库连接正常
- [ ] 测试数据准备就绪
- [ ] 网络连接正常

#### 1.2 代码同步
```bash
# 拉取最新代码
cd /Users/hrcao/Documents/MusicManagerPlus
git pull origin main

# 检查代码状态
git status

# 查看最近的提交
git log --oneline -10
```

#### 1.3 依赖更新
```bash
# 更新后端依赖
cd /Users/hrcao/Documents/MusicManagerPlus/backend
mvn clean install -DskipTests

# 更新前端依赖
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter pub get
```

### 阶段2: 执行自动化测试

#### 2.1 后端单元测试
```bash
cd /Users/hrcao/Documents/MusicManagerPlus/backend

# 运行所有单元测试
mvn test

# 查看测试结果
cat target/surefire-reports/TEST-*.xml

# 生成测试报告
mvn surefire-report:report
open target/site/surefire-report.html
```

**关键测试**:
- TaskInfoServiceTest (18个测试) - 必须全部通过
- ChangeRecordServiceTest (14个测试) - 必须全部通过

**预期结果**:
- 通过率 ≥ 90%
- 没有严重错误
- 测试覆盖率 ≥ 80%

#### 2.2 前端单元测试
```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli

# 运行所有测试
flutter test

# 生成覆盖率报告
flutter test --coverage

# 查看覆盖率报告
open coverage/lcov-report/index.html
```

**关键测试**:
- task_models_test.dart (32个测试) - 必须全部通过

**预期结果**:
- 通过率 ≥ 90%
- 没有编译错误
- 测试覆盖率 ≥ 70%

#### 2.3 集成测试
```bash
cd /Users/hrcao/Documents/MusicManagerPlus/backend

# 运行集成测试
mvn test -Dtest=TaskExecutionE2ETest
mvn test -Dtest=TaskExecutionIntegrationTest
mvn test -Dtest=TaskControllerTest
```

**预期结果**:
- 通过率 ≥ 85%
- 没有严重错误
- API响应正常

### 阶段3: 测试结果分析

#### 3.1 测试结果汇总
```bash
# 使用测试脚本运行所有测试
/Users/hrcao/Documents/MusicManagerPlus/scripts/run_tests.sh all

# 查看测试结果摘要
echo "后端测试: $(cd backend && mvn test -q 2>&1 | grep 'Tests run:')"
echo "前端测试: $(cd clients/flutter-web-cli && flutter test 2>&1 | grep 'tests passed')"
```

#### 3.2 失败测试分析
```bash
# 查看失败的测试
cd /Users/hrcao/Documents/MusicManagerPlus/backend
mvn test 2>&1 | grep -A 5 "FAILURE"

# 查看测试报告
cat target/surefire-reports/*.txt
```

#### 3.3 测试报告生成
```bash
# 生成测试报告
cd /Users/hrcao/Documents/MusicManagerPlus/backend
mvn surefire-report:report

# 查看测试报告
open target/site/surefire-report.html
```

### 阶段4: 问题处理

#### 4.1 问题分类
根据测试失败的严重程度进行分类：

**严重问题**（必须立即修复）:
- 系统崩溃
- 数据丢失
- 功能完全不可用
- 安全漏洞

**主要问题**（尽快修复）:
- 功能部分不可用
- 影响用户体验
- 性能严重下降

**次要问题**（可以延后）:
- 界面显示问题
- 提示信息不准确
- 小的Bug

**建议**（可以不修复）:
- 功能优化建议
- 用户体验改进建议

#### 4.2 问题修复流程

**步骤1: 记录问题**
```bash
# 创建问题记录文件
cat > /tmp/test_issues_$(date +%Y%m%d).md << EOF
# 测试问题记录

日期: $(date +%Y-%m-%d)
测试人员: $(whoami)
测试版本: $(git rev-parse --short HEAD)

## 严重问题
- [ ] 问题1描述

## 主要问题
- [ ] 问题2描述

## 次要问题
- [ ] 问题3描述

## 建议
- [ ] 建议描述
EOF
```

**步骤2: 分析问题**
```bash
# 查看错误日志
cd /Users/hrcao/Documents/MusicManagerPlus/backend
cat target/surefire-reports/*.txt | grep -A 10 "ERROR"

# 查看堆栈跟踪
mvn test -e 2>&1 | grep -A 20 "Exception"
```

**步骤3: 定位问题代码**
```bash
# 查找相关代码
cd /Users/hrcao/Documents/MusicManagerPlus/backend
grep -r "错误信息" src/main/java/

# 查看最近的代码变更
git log --oneline --since="1 week ago" --all
git diff HEAD~1 HEAD
```

**步骤4: 修复问题**
```bash
# 修复代码
# 编辑相关文件

# 运行相关测试验证修复
mvn test -Dtest=相关测试类

# 确保修复没有引入新问题
mvn test
```

**步骤5: 重新测试**
```bash
# 运行所有测试
/Users/hrcao/Documents/MusicManagerPlus/scripts/run_tests.sh all

# 验证修复效果
# 确保之前失败的测试现在通过
# 确保没有新的测试失败
```

#### 4.3 测试用例调整

**何时调整测试用例**:
- 测试用例本身有错误
- 需求发生变更
- API接口发生变更
- 发现新的测试场景

**调整步骤**:
```bash
# 1. 分析测试用例
# 2. 确定需要调整的部分
# 3. 修改测试用例
# 4. 运行测试验证
# 5. 提交变更
```

### 阶段5: 手动验证

#### 5.1 功能验证
```bash
# 启动服务
cd /Users/hrcao/Documents/MusicManagerPlus/backend
mvn spring-boot:run &

cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter run -d chrome &
```

**验证清单**:
- [ ] 创建任务功能正常
- [ ] 扫描文件功能正常
- [ ] 预览分析功能正常
- [ ] 执行任务功能正常
- [ ] 任务列表显示正常
- [ ] 任务详情显示正常
- [ ] 配置快照功能正常
- [ ] 任务删除功能正常

#### 5.2 界面验证
- [ ] 界面布局正常
- [ ] 按钮点击响应正常
- [ ] 表单验证正常
- [ ] 错误提示正常
- [ ] 加载状态显示正常

#### 5.3 性能验证
- [ ] 页面加载时间 < 3秒
- [ ] API响应时间 < 1秒
- [ ] 文件扫描速度正常
- [ ] 内存占用正常
- [ ] CPU占用正常

### 阶段6: 测试报告

#### 6.1 生成测试报告
```bash
# 创建测试报告
cat > /tmp/test_report_$(date +%Y%m%d).md << EOF
# 回归测试报告

## 测试信息
- 测试日期: $(date +%Y-%m-%d)
- 测试人员: $(whoami)
- 测试版本: $(git rev-parse --short HEAD)
- 测试级别: [快速/标准/完整]

## 测试结果

### 后端测试
- 总测试数: X
- 通过数: Y
- 失败数: Z
- 通过率: Y/X%

### 前端测试
- 总测试数: X
- 通过数: Y
- 失败数: Z
- 通过率: Y/X%

### 整体测试
- 总测试数: X
- 通过数: Y
- 失败数: Z
- 通过率: Y/X%

## 测试结论
☐ 可以发布
☐ 需要修复后重新测试
☐ 需要进一步调查

## 后续计划
EOF
```

#### 6.2 提交测试报告
```bash
# 提交测试报告到文档目录
cp /tmp/test_report_$(date +%Y%m%d).md /Users/hrcao/Documents/MusicManagerPlus/docs/test_reports/

# 提交到Git
cd /Users/hrcao/Documents/MusicManagerPlus
git add docs/test_reports/test_report_$(date +%Y%m%d).md
git commit -m "添加测试报告: $(date +%Y-%m-%d)"
git push origin main
```

## 常见问题处理

### 问题1: 配置快照创建失败

**症状**: 所有涉及任务创建的测试都失败

**原因**: ConfigSnapshotService未正确初始化

**解决方案**:
```bash
# 1. 检查ConfigSnapshotService实现
cd /Users/hrcao/Documents/MusicManagerPlus/backend
grep -r "ConfigSnapshotService" src/main/java/

# 2. 检查依赖注入配置
grep -r "@Autowired" src/main/java/ | grep -i config

# 3. 检查测试配置
grep -r "@MockBean" src/test/java/ | grep -i config

# 4. 修复配置问题
# 确保ConfigSnapshotService正确注入到测试中
```

### 问题2: API响应格式不匹配

**症状**: 测试期望的JSON路径在响应中不存在

**原因**: API响应格式与测试预期不一致

**解决方案**:
```bash
# 1. 检查实际API响应
curl http://localhost:8080/api/tasks | jq .

# 2. 检查测试期望
grep -r "taskId" src/test/java/

# 3. 更新测试用例或API响应
# 根据实际情况选择更新测试用例或修改API响应
```

### 问题3: 前端测试编译错误

**症状**: Mock类和接口不匹配

**原因**: ApiClient接口变更后Mock类未同步更新

**解决方案**:
```bash
# 1. 检查ApiClient接口
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
cat lib/api/api_client.dart

# 2. 检查MockApiClient实现
cat test/task_detail_page_test.dart | grep -A 20 "MockApiClient"

# 3. 更新Mock类以匹配新的接口
# 修改MockApiClient的方法签名以匹配ApiClient
```

### 问题4: 测试超时

**症状**: 测试执行时间过长或超时

**原因**: 测试数据量过大或网络问题

**解决方案**:
```bash
# 1. 增加测试超时时间
# 在测试配置中增加超时设置

# 2. 减少测试数据量
# 使用更小的测试数据集

# 3. 优化测试逻辑
# 避免不必要的等待和重复操作
```

### 问题5: 数据库锁定

**症状**: 数据库操作失败，提示数据库锁定

**原因**: 多个测试同时访问数据库

**解决方案**:
```bash
# 1. 清理数据库
cd /Users/hrcao/Documents/MusicManagerPlus/backend
rm -f data/music_manager.db*
rm -f data/music_manager_test.db*

# 2. 使用独立的测试数据库
# 在测试配置中指定独立的测试数据库

# 3. 确保测试后清理
# 在@AfterEach中清理测试数据
```

## 测试最佳实践

### 1. 测试独立性
- 每个测试应该独立运行
- 不依赖其他测试的执行顺序
- 使用@BeforeEach和@AfterEach进行清理

### 2. 测试可重复性
- 测试应该可以重复执行
- 不依赖外部状态
- 使用固定的测试数据

### 3. 测试速度
- 单元测试应该快速执行（< 1秒）
- 集成测试应该合理执行（< 10秒）
- 避免不必要的等待和延迟

### 4. 测试覆盖率
- 核心功能覆盖率 ≥ 90%
- 一般功能覆盖率 ≥ 80%
- 辅助功能覆盖率 ≥ 60%

### 5. 测试维护
- 定期更新测试用例
- 删除过时的测试用例
- 添加新的测试场景

## 回归检查清单

### 代码提交前
- [ ] 运行相关单元测试
- [ ] 确保所有测试通过
- [ ] 检查代码覆盖率
- [ ] 代码审查通过

### 代码合并前
- [ ] 运行所有单元测试
- [ ] 运行集成测试
- [ ] 运行端到端测试
- [ ] 手动验证关键功能

### 发布前
- [ ] 运行所有测试
- [ ] 性能测试通过
- [ ] 安全测试通过
- [ ] 测试报告完整

## 自动化建议

### 1. 持续集成
```yaml
# .github/workflows/regression-test.yml
name: Regression Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          distribution: 'adopt'
      
      - name: Run backend tests
        run: |
          cd backend
          mvn test
          mvn surefire-report:report
      
      - name: Set up Flutter
        uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.0.0'
      
      - name: Run frontend tests
        run: |
          cd clients/flutter-web-cli
          flutter test --coverage
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v2
        with:
          name: test-results
          path: |
            backend/target/surefire-reports/
            clients/flutter-web-cli/coverage/
```

### 2. 定时测试
```yaml
# .github/workflows/scheduled-test.yml
name: Scheduled Regression Test

on:
  schedule:
    - cron: '0 2 * * *'  # 每天凌晨2点运行

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run all tests
        run: |
          ./scripts/run_tests.sh all
```

### 3. 测试通知
```yaml
# 在测试失败时发送通知
- name: Notify on failure
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    text: '回归测试失败，请检查日志'
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

## 参考资源

### 文档
- [TEST_OVERVIEW.md](TEST_OVERVIEW.md) - 测试用例全景
- [ITERATION_SPECIFICATION.md](ITERATION_SPECIFICATION.md) - 迭代规范
- [REGRESSION_TEST_SOP.md](REGRESSION_TEST_SOP.md) - 回归测试SOP
- [TESTING.md](TESTING.md) - 测试文档

### 工具
- Maven Surefire Plugin - 后端测试框架
- Flutter Test - 前端测试框架
- JaCoCo - 代码覆盖率工具
- SonarQube - 代码质量分析

### 脚本
- [run_tests.sh](../scripts/run_tests.sh) - 测试运行脚本

## 总结

回归测试是保证项目质量的重要环节。通过系统化的回归测试流程，我们可以：

1. **及早发现问题** - 在代码合并前发现和修复问题
2. **保证代码质量** - 确保代码变更不会引入新问题
3. **提高开发效率** - 减少后期修复成本
4. **增强信心** - 对代码变更更有信心

遵循本指南进行回归测试，可以让我们的迭代过程更加平稳和可靠。
