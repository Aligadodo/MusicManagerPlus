# UI/UX 优化迭代文档

## 迭代信息

- **迭代编号**: ITER-2025-03-03-UI-001
- **迭代日期**: 2025-03-03
- **迭代目标**: 优化配置快照显示和任务列表交互体验
- **负责人**: AI Assistant
- **状态**: 进行中

---

## 1. 问题分析

### 1.1 配置快照显示问题

#### 问题描述
当前配置快照卡片存在以下问题：

1. **流水线配置信息不完整**
   - 仅显示流水线名称，无法查看流水线中包含的节点详情
   - 用户无法了解具体配置了哪些处理步骤

2. **全局设置展示混乱**
   - 参数顺序完全随机（Map 的 key 排序导致）
   - 缺乏模块化分组，与全局设置页面的设计不一致
   - 可读性差，难以理解参数之间的关系

3. **空间利用率低**
   - 一行只展示一个参数
   - 需要频繁滚动才能查看完整配置
   - 界面过于冗长

### 1.2 任务列表交互问题

#### 问题描述
当前任务列表存在以下交互问题：

1. **按钮点击区域问题**
   - 只有点击文字部分才能触发按钮效果
   - 非文字区域点击直接进入详情页
   - 交互体验差，容易误操作

2. **三阶段状态布局不合理**
   - 扫描、预览、执行三个阶段距离太小
   - 任务列表中间出现大量空白
   - 整体排版显得尴尬和奇怪
   - 缺乏清晰的视觉分区

---

## 2. 改进方案

### 2.1 配置快照优化

#### 2.1.1 流水线配置显示节点详情

**目标**: 在流水线配置区域显示完整的节点列表

**实现方案**:
- 后端：扩展 TaskConfigSnapshot 中的 pipelineConfig，包含 nodes 数组
- 前端：在 config_snapshot_card.dart 中添加节点列表展示
- 展示内容：节点名称、节点类型、节点配置摘要

**验收标准**:
- [ ] 流水线配置区域显示所有节点
- [ ] 每个节点显示名称和类型
- [ ] 节点按顺序排列

#### 2.1.2 全局设置按模块分组排序

**目标**: 按照全局设置页面的模块设计进行分组展示

**模块划分**:
```
├── 线程池设置
│   ├── maxThreads (最大线程数)
│   ├── previewThreads (预览线程数)
│   ├── executionThreads (执行线程数)
│   └── threadPoolMode (线程池模式)
├── 执行控制
│   ├── timeout (超时时间)
│   ├── retryCount (重试次数)
│   └── retryInterval (重试间隔)
├── 文件处理
│   ├── overwrite (覆盖已有文件)
│   ├── backup (启用备份)
│   └── backupPath (备份路径)
├── 扫描设置
│   ├── minRecursionDepth (最小递归深度)
│   ├── maxRecursionDepth (最大递归深度)
│   ├── previewLimit (预览文件上限)
│   └── executionLimit (执行文件上限)
├── 自动化设置
│   ├── autoRefresh (自动刷新)
│   └── autoExecute (自动执行)
└── 调试模式
    └── dryRun (模拟运行)
```

**实现方案**:
- 前端：在 config_snapshot_card.dart 中定义模块分组映射
- 使用 ExpansionPanel 或 Tab 进行分组展示
- 保持与全局设置页面一致的模块划分

**验收标准**:
- [ ] 参数按模块分组展示
- [ ] 每个模块有清晰的标题
- [ ] 参数顺序固定，不随机

#### 2.1.3 改进布局提高空间利用率

**目标**: 使用更紧凑的布局，减少滚动需求

**实现方案**:
- 采用 Tab 页面设计：源目录 / 流水线配置 / 全局设置
- 每个 Tab 内使用双列布局展示参数
- 使用卡片式分组，提高信息密度
- 添加折叠/展开功能

**验收标准**:
- [ ] 使用 Tab 切换不同配置类别
- [ ] 参数使用双列或网格布局
- [ ] 减少垂直滚动需求

### 2.2 任务列表优化

#### 2.2.1 修复按钮点击区域问题

**目标**: 确保按钮整个区域都可点击

**实现方案**:
- 修改 task_list_widget.dart 中的 _buildActionButton 方法
- 使用 GestureDetector 或 ElevatedButton 替代 InkWell + Container
- 确保点击事件覆盖整个按钮区域

**验收标准**:
- [ ] 点击按钮任意位置都能触发操作
- [ ] 按钮与详情页点击区域分离清晰

#### 2.2.2 改进三阶段状态布局为左中右三段式

**目标**: 重新设计任务列表项布局，采用左中右三段式

**布局设计**:
```
┌─────────────────────────────────────────────────────────────┐
│ [任务名称]          [扫描] [预览] [执行]          [操作按钮] │
│ [状态标签] [进度]                                           │
│ [文件数] [变更数] [成功/失败数]                             │
└─────────────────────────────────────────────────────────────┘
```

**具体分区**:
- **左侧区域 (30%)**: 任务名称、状态标签、进度、创建时间
- **中间区域 (40%)**: 扫描/预览/执行三个阶段状态指示器
- **右侧区域 (30%)**: 操作按钮（开始扫描、终止、删除等）

**实现方案**:
- 修改 task_list_widget.dart 中的 _buildTaskCard 方法
- 使用 Row + Expanded 实现三段式布局
- 阶段指示器居中显示，增加间距
- 操作按钮右对齐，垂直居中

**验收标准**:
- [ ] 布局分为左中右三段
- [ ] 阶段指示器位于中间区域
- [ ] 操作按钮位于右侧区域
- [ ] 各区域间距合理，视觉清晰

---

## 3. 实施计划

### 3.1 任务分解

| 序号 | 任务 | 文件 | 优先级 | 预估工时 |
|------|------|------|--------|----------|
| 1 | 编写迭代文档清单 | .trae/iterations/2025-03-03-ui-ux-optimization.md | 高 | 已完成 |
| 2 | 优化配置快照 - 流水线节点详情 | config_snapshot_card.dart | 高 | 2h |
| 3 | 优化配置快照 - 全局设置分组 | config_snapshot_card.dart | 高 | 2h |
| 4 | 优化配置快照 - Tab布局 | config_snapshot_card.dart | 高 | 2h |
| 5 | 优化任务列表 - 修复按钮点击 | task_list_widget.dart | 高 | 1h |
| 6 | 优化任务列表 - 三段式布局 | task_list_widget.dart | 高 | 2h |
| 7 | 构建并部署 | - | 高 | 0.5h |
| 8 | 验证测试 | - | 中 | 0.5h |

### 3.2 实施步骤

#### Phase 1: 配置快照优化
1. 分析 pipelineConfig 数据结构，确定节点信息字段
2. 重构 config_snapshot_card.dart，添加 Tab 布局
3. 实现全局设置模块化分组展示
4. 优化空间布局，使用双列展示

#### Phase 2: 任务列表优化
1. 修复 _buildActionButton 的点击区域问题
2. 重构 _buildTaskCard 为左中右三段式布局
3. 调整阶段指示器位置和样式
4. 优化按钮组和间距

#### Phase 3: 部署验证
1. 构建前端项目
2. 部署到服务器
3. 验证所有功能正常

---

## 4. 技术细节

### 4.1 关键文件

| 文件路径 | 说明 | 修改内容 |
|----------|------|----------|
| /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli/lib/widgets/config/config_snapshot_card.dart | 配置快照卡片 | 添加Tab布局、模块化分组、节点详情 |
| /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli/lib/widgets/preview/task_list_widget.dart | 任务列表 | 修复按钮点击、三段式布局 |

### 4.2 依赖关系

```
config_snapshot_card.dart
  ├── task_status.dart (TaskConfigSnapshot, SourceDirectoryConfig)
  └── flutter/material.dart

task_list_widget.dart
  ├── task_status.dart (TaskStatus)
  ├── task_service.dart
  ├── task_stage_indicator.dart
  └── flutter/material.dart
```

---

## 5. 风险评估

| 风险 | 可能性 | 影响 | 应对措施 |
|------|--------|------|----------|
| 布局调整后显示异常 | 中 | 中 | 充分测试不同屏幕尺寸 |
| Tab切换影响用户体验 | 低 | 低 | 添加默认选中状态 |
| 按钮点击事件冲突 | 中 | 高 | 使用 GestureDetector 的 behavior 属性 |

---

## 6. 验收清单

### 6.1 配置快照验收

- [ ] 流水线配置显示节点列表
- [ ] 全局设置按模块分组
- [ ] 参数顺序固定不随机
- [ ] 使用Tab切换不同配置类别
- [ ] 布局紧凑，信息密度高

### 6.2 任务列表验收

- [ ] 按钮整个区域可点击
- [ ] 点击按钮不触发详情页
- [ ] 左中右三段式布局清晰
- [ ] 阶段指示器居中显示
- [ ] 操作按钮右对齐

---

## 7. 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|----------|------|
| 2025-03-03 | v1.0 | 创建迭代文档 | AI Assistant |

---

## 8. 附录

### 8.1 参考文档

- [全局设置页面设计](file:///Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli/lib/pages/settings/global_settings_page.dart)
- [任务阶段指示器](file:///Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli/lib/widgets/common/task_stage_indicator.dart)

### 8.2 相关迭代

- [任务管理优化迭代](file:///Users/hrcao/Documents/MusicManagerPlus/backend/docs/iteration/task_management_optimization_iteration.md)
