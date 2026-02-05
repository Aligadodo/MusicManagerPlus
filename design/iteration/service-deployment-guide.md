# 服务部署与重启规范

## 概述

本文档定义了FileManager Plus项目的服务部署、打包和重启流程，确保开发过程中能够快速、可靠地启动和重启服务。

## 目录结构

```
bin/
├── macos/
│   ├── start-all.sh          # 一键启动前后端服务
│   ├── start-backend.sh      # 启动后端服务
│   ├── start-frontend.sh     # 启动前端服务
│   ├── restart-all.sh       # 一键重启前后端服务
│   ├── restart-backend.sh    # 重启后端服务
│   ├── restart-frontend.sh   # 重启前端服务
│   └── stop-all.sh         # 一键停止前后端服务
```

## 脚本使用规范

### 1. 开发阶段

在开发过程中，需要频繁重启服务以验证代码变更。推荐使用以下脚本：

#### 启动服务
```bash
# 一键启动前后端服务
./bin/macos/start-all.sh

# 或者分别启动
./bin/macos/start-backend.sh
./bin/macos/start-frontend.sh
```

#### 重启服务
```bash
# 一键重启前后端服务
./bin/macos/restart-all.sh

# 或者分别重启
./bin/macos/restart-backend.sh
./bin/macos/restart-frontend.sh
```

#### 停止服务
```bash
# 一键停止前后端服务
./bin/macos/stop-all.sh
```

### 2. 打包阶段

在完成代码修改后，需要重新打包后端服务：

#### 后端打包
```bash
# 进入后端目录
cd backend

# 清理并编译
mvn clean package -DskipTests

# 打包后的文件位于: backend/target/backend.jar
```

#### 前端打包
```bash
# 进入前端目录
cd clients/flutter-web-cli

# 构建Web应用
flutter build web

# 打包后的文件位于: clients/flutter-web-cli/build/web
```

### 3. 部署阶段

在完成打包后，使用重启脚本部署新版本：

```bash
# 重启后端服务（使用新打包的jar文件）
./bin/macos/restart-backend.sh

# 重启前端服务（使用新打包的web文件）
./bin/macos/restart-frontend.sh

# 或者一键重启所有服务
./bin/macos/restart-all.sh
```

## 迭代流程中的服务管理

### 迭代开始阶段
1. 启动前后端服务
2. 验证服务正常运行
3. 开始开发工作

### 开发阶段
1. 完成代码修改
2. 根据需要重启服务验证修改
3. 使用 `restart-all.sh` 快速重启

### 打包测试阶段
1. 执行后端打包：`mvn clean package -DskipTests`
2. 执行前端打包：`flutter build web`
3. 重启服务验证打包结果

### 迭代完成阶段
1. 确保所有功能测试通过
2. 执行最终打包
3. 更新迭代事项清单
4. 提交代码和文档

## 服务端口配置

### 默认端口
- 后端服务：8080
- 前端服务：8081

### 修改端口
如需修改端口，可以编辑对应的脚本文件或使用命令行参数：

#### 后端端口
编辑 `backend/src/main/resources/application.yml`：
```yaml
server:
  port: 8080  # 修改为所需端口
```

#### 前端端口
使用 `start_web.sh` 脚本时指定端口：
```bash
./start_web.sh 8081  # 使用8081端口
```

## 故障排查

### 后端服务无法启动
1. 检查端口是否被占用：`lsof -i :8080`
2. 查看后端日志：`tail -f backend/logs/application.log`
3. 检查Java版本：`java -version`（需要Java 8+）

### 前端服务无法启动
1. 检查端口是否被占用：`lsof -i :8081`
2. 检查Flutter环境：`flutter doctor`
3. 查看前端日志：查看终端输出

### 服务启动缓慢
1. 后端：增加JVM内存参数 `-Xms512m -Xmx2g`
2. 前端：清理缓存 `flutter clean`

## AI助手使用规范

当AI助手参与迭代开发时，应遵循以下服务管理规范：

### 1. 代码修改后
- 优先使用 `restart-all.sh` 重启服务
- 等待服务完全启动后再进行测试
- 验证服务状态正常

### 2. 打包验证
- 执行完整的打包流程
- 重启服务验证打包结果
- 检查日志确认无错误

### 3. 问题排查
- 记录错误信息和日志
- 提供故障排查建议
- 必要时提供手动修复步骤

## 注意事项

1. **端口冲突**：确保8080和8081端口未被其他应用占用
2. **权限问题**：确保脚本有执行权限：`chmod +x bin/macos/*.sh`
3. **环境依赖**：确保已安装Java 8+、Flutter和Maven
4. **日志监控**：开发过程中应监控服务日志，及时发现问题
5. **数据备份**：在执行可能影响数据的操作前，先备份配置文件

## 相关文档

- [迭代流程规范](../standard/process/iteration-flow.md)
- [迭代事项清单管理规范](./iteration-checklist-management.md)
- [代码规范](../standard/code-style/code-standard.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-06  
**维护者**: FileManager Plus Team
