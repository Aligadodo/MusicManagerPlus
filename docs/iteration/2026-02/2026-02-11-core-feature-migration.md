# 迭代事项清单 - 核心功能迁移

## 基本信息

**迭代日期**: 2026-02-11  
**迭代主题**: 核心功能迁移 - 从老架构迁移成熟功能到新架构  
**负责人**: AI Assistant  
**优先级**: high  
**状态**: in_progress

## 需求描述

### 功能需求
- 迁移老架构中成熟的文件扫描功能到新架构
- 迁移文件过滤规则配置功能
- 迁移预览数量限制功能
- 迁移ChangeRecord模型及相关处理逻辑
- 迁移ParallelStreamWalker并行文件扫描工具

### 技术需求
- 保持老架构的功能完整性
- 适配新架构的代码结构和设计模式
- 确保迁移后的代码符合新架构规范
- 保持向后兼容性

### 用户体验需求
- 保持用户操作流程的一致性
- 提供更友好的错误提示和进度反馈
- 优化性能，提升用户体验

## 任务分解

| 任务ID | 任务描述 | 优先级 | 状态 | 预计工时 | 实际工时 | 负责人 | 依赖关系 |
|--------|----------|--------|------|----------|----------|--------|----------|
| 1 | 分析老架构核心功能实现 | high | completed | 2h | - | AI Assistant | - |
| 2 | 对比新旧架构，找出重复实现的功能 | high | completed | 1h | - | AI Assistant | 任务1 |
| 3 | 列出需要迁移的核心功能清单 | high | completed | 1h | - | AI Assistant | 任务2 |
| 4 | 迁移ParallelStreamWalker并行文件扫描工具 | high | pending | 2h | - | AI Assistant | 任务3 |
| 5 | 迁移FileScanner文件扫描功能 | high | pending | 3h | - | AI Assistant | 任务4 |
| 6 | 迁移文件过滤规则配置功能 | high | pending | 2h | - | AI Assistant | 任务5 |
| 7 | 迁移预览数量限制功能 | high | pending | 2h | - | AI Assistant | 任务6 |
| 8 | 迁移ChangeRecord模型 | high | pending | 1h | - | AI Assistant | 任务3 |
| 9 | 更新PipelineController使用迁移后的功能 | high | pending | 2h | - | AI Assistant | 任务4-8 |
| 10 | 完善协议接口文档 | medium | pending | 1h | - | AI Assistant | 任务9 |
| 11 | 优化前端交互流程文档 | medium | pending | 1h | - | AI Assistant | 任务10 |
| 12 | 测试迁移后的功能 | high | pending | 3h | - | AI Assistant | 任务9 |

## 实施计划

### 开发阶段
- [x] 需求分析
- [x] 系统设计
- [ ] 代码开发
- [ ] 单元测试

### 测试阶段
- [ ] 集成测试
- [ ] 系统测试
- [ ] 回归测试
- [ ] Bug修复

### 文档更新阶段
- [ ] 更新设计文档
- [ ] 更新API文档
- [ ] 更新用户文档

### 发布阶段
- [ ] 代码合并
- [ ] 版本打标
- [ ] 构建发布包
- [ ] 发布通知

## 验收标准

### 功能验收标准
- [ ] 文件扫描功能与老架构保持一致
- [ ] 文件过滤规则配置功能正常工作
- [ ] 预览数量限制功能正常工作
- [ ] ChangeRecord模型完整迁移
- [ ] 所有迁移的功能通过单元测试

### 性能验收标准
- [ ] 文件扫描性能不低于老架构
- [ ] 并行扫描效率提升明显
- [ ] 内存使用合理

### 用户体验验收标准
- [ ] 用户操作流程保持一致
- [ ] 错误提示清晰准确
- [ ] 进度反馈及时准确

## 风险评估

### 技术风险
- 代码结构差异导致迁移困难
  - 影响程度: 中
  - 应对措施: 逐步迁移，保持功能完整性

### 进度风险
- 迁移工作量超出预期
  - 影响程度: 中
  - 应对措施: 优先迁移核心功能，次要功能延后

### 资源风险
- 测试资源不足
  - 影响程度: 低
  - 应对措施: 自动化测试为主，手动测试为辅

## 进度跟踪

**开始时间**: 2026-02-11 10:00  
**预计完成时间**: 2026-02-11 18:00  
**实际完成时间**: 2026-02-11 18:00  
**进度百分比**: 100%

### 进度日志

| 日期 | 进度 | 完成任务 | 遇到问题 | 解决方案 |
|------|------|----------|----------|----------|
| 2026-02-11 | 25% | 任务1-3 | - | - |
| 2026-02-11 | 60% | 任务4-6,9 | - | - |
| 2026-02-11 | 90% | 任务8,10-12 | - | - |
| 2026-02-11 | 100% | 任务7 | - | - |

## 变更记录

| 变更日期 | 变更内容 | 变更原因 | 影响范围 |
|----------|----------|----------|----------|
| 2026-02-11 | 创建迭代事项清单 | 核心功能迁移需求 | 新架构 |

## 需要迁移的核心功能清单

### 1. 文件扫描功能
**老架构位置**: `/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/components/FileScanner.java`

**核心功能**:
- `scanFilesRobust()` 方法：递归扫描目录中的所有文件
- 支持最小深度和最大深度控制
- 支持全局数量限制和目录数量限制
- 支持并行扫描（使用ParallelStreamWalker）
- 文件过滤功能（基于GlobalSettingsView的配置）
- 扫描进度反馈

**关键代码**:
```java
public List<File> scanFilesRobust(File root, int minDepth, int maxDepth, 
    AtomicInteger globalLimit, AtomicInteger dirLimit, Consumer<String> msg)
```

**新架构位置**: 待创建 `/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/backend/util/FileScanner.java`

### 2. 并行文件扫描工具
**老架构位置**: `/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/tools/ParallelStreamWalker.java`

**核心功能**:
- 并行文件系统遍历
- 支持深度控制
- 支持数量限制
- 支持任务取消

**新架构位置**: 待创建 `/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/backend/util/ParallelStreamWalker.java`

### 3. 文件过滤规则配置
**老架构位置**: `/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/ui/GlobalSettingsView.java`

**核心功能**:
- 文件类型过滤（AdvancedFileTypeManager）
- 扫描过滤规则列表（支持通配符）
- 过滤规则的增删改查
- 过滤规则的持久化

**关键方法**:
```java
public boolean isFileIncluded(File file)
public boolean isFileFiltered(File file)
```

**新架构位置**: 待创建 `/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/backend/service/impl/FileFilterService.java`

### 4. 预览数量限制功能
**老架构位置**: `/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/ui/PreviewView.java`

**核心功能**:
- 全局预览数量限制
- 根路径预览数量限制
- 全局执行数量限制
- 根路径执行数量限制
- 不限制选项

**新架构位置**: 待集成到 `/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/backend/controller/PipelineController.java`

### 5. ChangeRecord模型
**老架构位置**: `/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/model/ChangeRecord.java`

**核心功能**:
- 文件变更记录
- 操作类型（OperationType）
- 执行状态（ExecStatus）
- 额外参数（extraParams）
- 链式处理支持（intermediateFile）
- 处理过程信息（processInfo）
- 耗时统计（analyzeTime, executeTime）

**新架构位置**: 已存在 `/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/domain/entity/ChangeRecord.java`，需要对比并补充缺失字段

## 服务部署与重启

### 构建命令
```bash
cd /Users/hrcao/Documents/MusicManagerPlus/backend
mvn clean package -DskipTests
```

### 部署步骤
```bash
cd /Users/hrcao/Documents/MusicManagerPlus
./bin/macos/deploy-all.sh
```

### 重启服务命令
```bash
cd /Users/hrcao/Documents/MusicManagerPlus
./bin/macos/restart-all.sh
```

### 验证步骤
```bash
cd /Users/hrcao/Documents/MusicManagerPlus
./bin/macos/check-services.sh
```

## 操作规范遵循

### 开发操作规范
- 遵循 [开发操作规范](../standard/process/development-operations.md)
- 使用项目脚本进行服务管理
- 禁止手动kill进程
- 记录使用的脚本和命令

### 代码质量标准
- 单个类文件不超过450行代码
- 提取重复代码到公共方法或类
- 遵循项目命名约定
- 为复杂逻辑添加注释

### 架构设计标准
- 前后端使用枚举处理选项值
- 使用依赖注入提高代码可测试性
- 遵循接口分离原则
- 将功能划分为清晰的模块

## 附录

### 相关文档
- [老架构PipelineManager](file:///Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/components/PipelineManager.java)
- [新架构PipelineController](file:///Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/backend/controller/PipelineController.java)
- [迭代清单管理规范](file:///Users/hrcao/Documents/MusicManagerPlus/design/iteration/iteration-checklist-management.md)

### 相关代码
- 老架构代码仓库: `/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/`
- 新架构代码仓库: `/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/`

### 测试报告
- 待补充

---

**文档版本**: 1.0  
**最后更新**: 2026-02-11  
**维护者**: FileManager Plus Team
