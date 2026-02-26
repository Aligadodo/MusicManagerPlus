# 开发指南

## 1. 项目结构

### 1.1 目录结构

```
MusicManagerPlus/
├── backend/              # 后端服务
│   ├── src/              # 源代码
│   ├── target/           # 编译输出
│   ├── pom.xml           # Maven配置
│   └── docs/             # 后端文档
├── clients/              # 前端客户端
│   ├── flutter-web-cli/  # Flutter Web客户端
│       ├── lib/          # Flutter源代码
│       ├── test/         # 测试代码
│       └── pubspec.yaml  # Flutter依赖配置
├── docs/                 # 项目文档
│   ├── architecture/     # 架构设计文档
│   ├── iteration/        # 迭代记录
│   │   └── 2026-02/      # 按月份组织的迭代文档
│   └── TESTING.md        # 测试文档
├── scripts/              # 脚本文件
├── .trae/                # Trae IDE配置
└── DEVELOPMENT_GUIDE.md  # 开发指南
```

### 1.2 核心文件说明

- **backend/src/main/java/com/filemanager/backend/**: 后端核心代码
- **clients/flutter-web-cli/lib/**: 前端核心代码
- **docs/iteration/**: 迭代文档，按月份组织
- **scripts/**: 部署和测试脚本

## 2. 开发环境配置

### 2.1 后端环境

- **Java 8 或 Java 21** (兼容JDK 8，推荐使用JDK 21)
- **Maven 3.6+**
- **Spring Boot 2.7.0**
- **H2 Database** (嵌入式数据库)

### 2.3 JDK 切换

#### MacOS
```bash
# 查看当前JDK版本
java -version

# 切换到JDK 8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)

# 切换到JDK 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

#### Windows
```cmd
# 查看当前JDK版本
java -version

# 切换到JDK 8
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
set PATH=%JAVA_HOME%\bin;%PATH%

# 切换到JDK 21
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
```

### 2.4 前端环境

- **Flutter 3.0+**
- **Dart 2.17+**
- **Web浏览器** (Chrome/Edge)

### 2.3 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `SERVER_PORT` | 后端服务端口 | 8080 |
| `DATABASE_URL` | 数据库连接URL | jdbc:h2:mem:testdb |
| `FLUTTER_WEB_PORT` | 前端服务端口 | 8081 |

## 3. 部署规范

### 3.1 端口配置

- **后端服务**: 8080
- **前端服务**: 8081

### 3.2 跨平台部署

#### MacOS
```bash
# 使用脚本启动所有服务
./bin/macos/start-all.sh

# 单独启动后端服务
./bin/macos/start-backend.sh

# 单独启动前端服务
./bin/macos/start-frontend.sh
```

#### Windows
```cmd
# 使用脚本启动所有服务
bin\windows\start-all.bat

# 单独启动后端服务
bin\windows\start-backend.bat

# 单独启动前端服务
bin\windows\start-frontend.bat
```

### 3.3 手动启动命令

#### 后端服务
```bash
cd backend
mvn spring-boot:run
```

#### 前端服务
```bash
cd clients/flutter-web-cli
flutter run -d web-server --web-port 8081
```

### 3.4 构建命令

#### 后端构建
```bash
cd backend
mvn clean package
```

#### 前端构建
```bash
cd clients/flutter-web-cli
flutter build web
```

## 4. 迭代流程

### 4.1 流程概述

```
需求分析 → 方案设计 → 开发实现 → 单元测试 → 集成测试 → 代码审查 → 测试回归 → 部署上线
```

### 4.2 迭代文档要求

- **迭代计划文档**: 每次迭代开始前创建，包含目标、范围和时间线
- **测试用例文档**: 迭代开始时创建，评估测试用例变更
- **测试报告文档**: 迭代完成后创建，记录测试结果

### 4.3 文档命名规范

- 迭代文档: `YYYY-MM-DD-{description}.md`
- 测试用例文档: `YYYY-MM-DD-{description}_test_cases.md`
- 测试报告文档: `YYYY-MM-DD-{description}_test_report.md`

## 5. 测试规范

### 5.1 测试类型

- **单元测试**: 测试单个组件或函数
- **集成测试**: 测试组件间的交互
- **端到端测试**: 测试完整的用户流程

### 5.2 测试命令

#### 后端测试
```bash
cd backend
mvn test
```

#### 前端测试
```bash
cd clients/flutter-web-cli
flutter test
```

### 5.3 测试覆盖率要求

- 核心功能: ≥ 80%
- 整体代码: ≥ 70%

## 6. 代码规范

### 6.1 后端代码规范

- 遵循Java编码规范
- 使用有意义的变量和方法名
- 编写清晰的代码注释
- 保持代码简洁和可读性

### 6.2 前端代码规范

- 遵循Dart编码规范
- 使用Flutter最佳实践
- 组件化设计
- 响应式布局

## 7. 版本管理

### 7.1 Git分支策略

- `main`: 主分支，包含稳定版本
- `develop`: 开发分支，集成新功能
- `feature/*`: 特性分支，开发新功能
- `bugfix/*`: 修复分支，修复bug

### 7.2 提交规范

```
<类型>(<范围>): <描述>

<详细描述>

<footer>
```

类型包括: feat, fix, docs, style, refactor, test, chore

## 8. 常见问题和解决方案

### 8.1 端口占用

**问题**: 端口8080或8081被占用
**解决方案**: 查找并终止占用端口的进程

```bash
lsof -i :8080  # 查找占用8080端口的进程
kill -9 <PID>  # 终止进程
```

### 8.2 依赖冲突

**问题**: Maven或Flutter依赖冲突
**解决方案**: 清理依赖并重新构建

```bash
# 后端
mvn clean

# 前端
flutter clean
flutter pub get
```

### 8.3 数据库连接问题

**问题**: 无法连接到数据库
**解决方案**: 检查数据库配置和连接字符串

## 9. 开发工具推荐

- **IDE**: IntelliJ IDEA (后端), VS Code (前端)
- **版本控制**: Git
- **测试工具**: JUnit (后端), Flutter Test (前端)
- **API测试**: Postman
- **性能测试**: JMeter

## 10. 联系方式

如有问题，请联系项目负责人。