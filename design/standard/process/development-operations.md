# 开发操作规范

## 概述

本文档定义了FileManager Plus项目开发过程中的常用操作规范，包括服务管理、编译部署、代码检查等，确保开发过程标准化、高效。

## 服务管理

### 前端服务

**端口**: 8081

**启动服务**:
```bash
./bin/macos/start-frontend.sh
```

**停止服务**:
```bash
./bin/macos/stop-all.sh
```

**重启服务**:
```bash
./bin/macos/restart-frontend.sh
```

### 后端服务

**端口**: 8080

**启动服务**:
```bash
./bin/macos/start-backend.sh
```

**停止服务**:
```bash
./bin/macos/stop-all.sh
```

**重启服务**:
```bash
./bin/macos/restart-backend.sh
```

### 同时启停前后端

**启动所有服务**:
```bash
./bin/macos/start-all.sh
```

**停止所有服务**:
```bash
./bin/macos/stop-all.sh
```

**重启所有服务**:
```bash
./bin/macos/restart-all.sh
```

## 编译与部署

### 后端编译

**编译后端代码**:
```bash
cd backend
mvn clean compile -DskipTests
```

**打包后端**:
```bash
cd backend
mvn package -DskipTests
```

**编译并打包**:
```bash
cd backend
mvn clean package -DskipTests
```

### 前端编译

**编译前端代码**:
```bash
cd clients/flutter-web-cli
flutter build web --release
```

**开发模式编译**:
```bash
cd clients/flutter-web-cli
flutter build web --debug
```

### 部署

**部署前端到运行目录**:
```bash
cp -r clients/flutter-web-cli/build/web/* frontend/
```

**完整部署流程**:
```bash
# 1. 编译后端
cd backend && mvn clean package -DskipTests

# 2. 编译前端
cd ../clients/flutter-web-cli && flutter build web --release

# 3. 部署前端
cp -r build/web/* ../frontend/

# 4. 重启服务
cd ../.. && ./bin/macos/restart-all.sh
```

## 代码检查

### 后端代码检查

**编译检查**:
```bash
cd backend
mvn compile
```

**测试检查**:
```bash
cd backend
mvn test
```

**代码格式化**:
```bash
cd backend
mvn fmt:format
```

### 前端代码检查

**代码分析**:
```bash
cd clients/flutter-web-cli
flutter analyze
```

**代码格式化**:
```bash
cd clients/flutter-web-cli
dart format .
```

**测试检查**:
```bash
cd clients/flutter-web-cli
flutter test
```

## Git 操作

### 提交代码

**查看状态**:
```bash
git status
```

**添加文件**:
```bash
git add .
```

**提交代码**:
```bash
git commit -m "描述信息"
```

**推送到远程**:
```bash
git push
```

### 分支管理

**创建功能分支**:
```bash
git checkout -b feature/功能名称
```

**切换分支**:
```bash
git checkout 分支名称
```

**合并分支**:
```bash
git merge 分支名称
```

**删除分支**:
```bash
git branch -d 分支名称
```

## 常用命令

### 日志查看

**查看后端日志**:
```bash
tail -f backend/logs/application.log
```

**查看前端日志**:
```bash
# 前端日志在浏览器控制台查看
```

### 端口检查

**检查端口占用**:
```bash
lsof -i :8080  # 后端
lsof -i :8081  # 前端
```

### 进程管理

**查找Java进程**:
```bash
ps aux | grep java
```

**查找Python进程**:
```bash
ps aux | grep python
```

## 开发注意事项

### 服务管理

1. **禁止手动kill进程**: 必须使用项目提供的脚本启停服务
2. **服务启动顺序**: 先启动后端，再启动前端
3. **服务停止顺序**: 先停止前端，再停止后端
4. **端口检查**: 启动前检查端口是否被占用

### 命令执行

1. **避免交互式命令**: 禁止使用需要用户确认的命令（如 rm -rf 会提示确认）
2. **自动确认机制**: 对于需要确认的命令，使用自动确认方式：
   - 删除操作: 使用 `yes | rm -rf` 或 `rm -rf -f`
   - 其他确认操作: 使用 `yes | command` 或命令的 `-y`/`-f` 参数
3. **非阻塞执行**: 长时间运行命令应使用非阻塞模式
4. **命令组合**: 多个命令应组合成一个命令执行，减少交互次数

### 编译部署

1. **编译前检查**: 确保代码已保存，没有语法错误
2. **部署前备份**: 重要部署前备份当前版本
3. **部署后验证**: 部署后验证服务是否正常运行
4. **清理缓存**: 部署后清理浏览器缓存

### 代码提交

1. **提交前检查**: 确保代码编译通过，测试通过
2. **提交信息规范**: 使用清晰的提交信息，描述变更内容
3. **提交前更新**: 确保文档与代码同步更新
4. **提交频率**: 小步快跑，频繁提交，避免大量变更堆积

### 数据隔离

1. **前端状态隔离**: 每个策略、每个页面的状态必须独立
2. **深拷贝使用**: 传递对象时使用深拷贝，避免共享引用
3. **配置独立**: 每个策略的配置必须独立存储和管理
4. **数据验证**: 加载数据时验证数据完整性

## AI 提示词

当AI助手参与开发时，请遵循以下指导：

```
你正在参与FileManager Plus项目的开发。请按照以下操作规范进行：

1. 服务管理：
   - 使用 ./bin/macos/start-backend.sh 启动后端服务
   - 使用 ./bin/macos/start-frontend.sh 启动前端服务
   - 使用 ./bin/macos/stop-all.sh 停止所有服务
   - 使用 ./bin/macos/restart-all.sh 重启所有服务
   - 禁止手动 kill 进程，必须使用项目脚本

2. 编译部署：
   - 后端: cd backend && mvn clean package -DskipTests
   - 前端: cd clients/flutter-web-cli && flutter build web --release
   - 部署: cp -r build/web/* ../frontend/
   - 完整流程: 编译 -> 部署 -> 重启服务

3. 代码检查：
   - 后端: mvn compile / mvn test
   - 前端: flutter analyze / flutter test
   - 修改代码后必须运行检查，确保没有错误

4. 数据隔离：
   - 前端状态必须独立，避免共享引用
   - 使用深拷贝传递对象，确保数据隔离
   - 每个策略的配置必须独立管理

5. Git 操作：
   - 提交前检查代码编译和测试
   - 使用清晰的提交信息
   - 确保文档与代码同步更新

注意事项：
- 所有服务操作必须使用项目脚本
- 编译部署必须按顺序执行
- 代码修改后必须运行检查
- 遵循项目的代码规范和设计规范
- 命令执行必须避免交互式提示，使用自动确认机制
```

## 相关文档

- [迭代流程规范](./iteration-flow.md)
- [代码规范](../code-style/code-standard.md)
- [设计规范](../design-style/design-standard.md)
- [服务部署指南](../service-deployment-guide.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-08  
**维护者**: FileManager Plus Team
