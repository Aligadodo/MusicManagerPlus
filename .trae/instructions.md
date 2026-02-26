# MusicManagerPlus 项目开发规范

## 1. 项目概述

MusicManagerPlus是一个音乐文件管理系统，包含后端Spring Boot服务和前端Flutter Web客户端。项目旨在提供音乐文件的扫描、分析、预览和管理功能。

## 2. 核心规范

### 2.1 端口配置

- **后端服务**: 必须使用端口8080
- **前端服务**: 必须使用端口8081

### 2.2 JDK 兼容性

- **开发环境**: 支持JDK 8和JDK 21
- **推荐版本**: JDK 21
- **兼容要求**: 代码必须兼容JDK 8

### 2.3 跨平台部署

- **支持平台**: MacOS和Windows
- **部署脚本**: 
  - MacOS: `bin/macos/` 目录下的脚本
  - Windows: `bin/windows/` 目录下的脚本

### 2.4 目录结构

- 后端代码: `backend/src/main/java/com/filemanager/backend/`
- 前端代码: `clients/flutter-web-cli/lib/`
- 文档: `docs/`
- 迭代文档: `docs/iteration/YYYY-MM/` (按月份组织)

### 2.3 迭代流程

严格遵循以下迭代流程:
1. 需求分析
2. 方案设计
3. 开发实现
4. 单元测试
5. 集成测试
6. 代码审查
7. 测试回归
8. 部署上线

### 2.4 文档要求

每次迭代必须创建以下文档:
- 迭代计划文档: `YYYY-MM-DD-{description}.md`
- 测试用例文档: `YYYY-MM-DD-{description}_test_cases.md`
- 测试报告文档: `YYYY-MM-DD-{description}_test_report.md`

### 2.5 测试规范

- 单元测试覆盖率: ≥ 80%
- 集成测试: 验证前后端集成
- 端到端测试: 验证完整用户流程

## 3. AI工具使用规范

### 3.1 任务执行前

1. **读取规范文档**: 每次执行任务前，先读取以下文档:
   - `DEVELOPMENT_GUIDE.md`
   - `docs/iteration/ITERATION_SPECIFICATION.md`

2. **检查环境状态**:
   - 后端服务是否运行在8080端口
   - 前端服务是否运行在8081端口
   - 数据库连接是否正常

3. **规划任务**: 使用TodoWrite工具创建详细的任务计划，确保覆盖所有必要步骤。

### 3.2 开发过程

1. **代码规范**:
   - 遵循Java和Dart编码规范
   - 使用有意义的变量和方法名
   - 编写清晰的代码注释

2. **测试优先**:
   - 为核心功能编写单元测试
   - 确保测试覆盖率达到要求
   - 验证测试通过后再进行部署

3. **文档更新**:
   - 及时更新相关文档
   - 保持文档与代码同步
   - 确保文档结构清晰

### 3.3 部署和验证

1. **部署规范**:
   - 使用指定的端口配置
   - 执行完整的测试回归
   - 验证服务正常运行

2. **验证步骤**:
   - 检查服务启动状态
   - 验证API接口可用
   - 测试核心功能流程
   - 确认前端界面正常

### 3.4 问题处理

1. **错误处理**:
   - 记录详细的错误信息
   - 分析错误原因
   - 提供解决方案

2. **回滚策略**:
   - 保持代码版本的可回滚性
   - 记录部署变更
   - 确保能够快速回滚

## 4. 常用命令

### 4.1 后端命令

```bash
# 启动后端服务
cd backend
mvn spring-boot:run

# 运行测试
mvn test

# 构建项目
mvn clean package
```

### 4.2 前端命令

```bash
# 启动前端服务
cd clients/flutter-web-cli
flutter run -d web-server --web-port 8081

# 运行测试
flutter test

# 构建项目
flutter build web
```

### 4.3 测试命令

```bash
# 运行所有测试
/Users/hrcao/Documents/MusicManagerPlus/scripts/run_tests.sh all

# 运行后端测试
/Users/hrcao/Documents/MusicManagerPlus/scripts/run_tests.sh backend

# 运行前端测试
/Users/hrcao/Documents/MusicManagerPlus/scripts/run_tests.sh frontend
```

## 5. 注意事项

1. **端口冲突**: 如果端口被占用，使用`lsof -i :端口`查找并终止占用进程
2. **依赖问题**: 定期更新依赖，避免版本冲突
3. **测试覆盖**: 确保核心功能有足够的测试覆盖
4. **文档同步**: 代码变更后及时更新相关文档
5. **规范遵循**: 严格遵循项目规范，确保代码质量和一致性

## 6. 参考文档

- `DEVELOPMENT_GUIDE.md` - 开发指南
- `docs/iteration/ITERATION_SPECIFICATION.md` - 迭代规范
- `docs/TESTING.md` - 测试文档
- `docs/REGRESSION_TEST_SOP.md` - 回归测试标准操作流程

## 7. 执行流程

1. **理解需求**: 仔细分析用户需求，确保理解正确
2. **制定计划**: 使用TodoWrite工具创建详细的任务计划
3. **检查环境**: 确保开发环境配置正确，端口可用
4. **执行任务**: 按照计划执行任务，遵循规范要求
5. **测试验证**: 执行完整的测试回归，确保功能正常
6. **部署上线**: 按照部署规范进行部署，验证服务运行状态
7. **文档更新**: 更新相关文档，记录变更内容
8. **总结汇报**: 总结完成的工作，提供清晰的汇报