# 前端代码重构迭代清单

## 文档信息

- **版本**: 1.0.0
- **创建日期**: 2026-02-25
- **维护者**: 开发团队
- **基于规范**: `backend/docs/task_management_development_guide.md`

## 1. 重构目标

### 1.1 主要目标

1. **消除代码重复**: 删除未使用的页面和类，整合功能到实际使用的页面
2. **优化代码结构**: 拆分过大的文件，提高代码可维护性
3. **遵循模块化原则**: 按功能模块拆分代码，保持单一职责原则
4. **保持功能完整性**: 确保所有现有功能在重构后正常工作

### 1.2 重构原则

1. **渐进式重构**: 每次只修改一个模块，确保系统稳定
2. **向后兼容**: 保持API接口不变
3. **充分测试**: 每个迭代完成后进行测试
4. **文档同步**: 及时更新相关文档

## 2. 代码分析结果

### 2.1 未使用的前端页面和类

#### 2.1.1 TaskListPage (470行)

**文件路径**: `clients/flutter-web-cli/lib/pages/task_list_page.dart`

**设计用途**: 独立的任务列表页面，包含完整的任务列表功能

**主要功能**:
- 任务列表加载和分页
- 任务搜索和筛选
- 批量选择和删除
- 任务状态过滤
- 自动刷新（5秒间隔）
- 查看任务详情

**使用情况**: 
- ❌ 未被使用（main.dart中未引用）
- ❌ 未被其他页面引用

**建议**: 删除或整合到PreviewPage

#### 2.1.2 DatabaseTaskListPage (490行)

**文件路径**: `clients/flutter-web-cli/lib/pages/database_task_list_page.dart`

**设计用途**: 基于数据库的任务列表页面

**主要功能**:
- 任务列表加载和分页
- 任务搜索和筛选
- 统计信息展示
- 任务操作（删除、重新运行、终止）
- 批量操作

**使用情况**: 
- ❌ 未被使用（main.dart中未引用）
- ❌ 未被其他页面引用

**建议**: 删除或整合到PreviewPage

#### 2.1.3 TaskDetailPage (2048行)

**文件路径**: `clients/flutter-web-cli/lib/pages/task_detail_page.dart`

**设计用途**: 任务详情页面

**主要功能**:
- 任务信息展示
- 阶段控制（扫描、预览、执行）
- WebSocket实时更新
- 阶段状态管理
- 任务操作（删除、取消、暂停、恢复、重试）

**使用情况**: 
- ❌ 未被使用（main.dart中未引用）
- ❌ 未被其他页面引用

**建议**: 删除或整合到PreviewPage

#### 2.1.4 DatabaseTaskService

**文件路径**: `clients/flutter-web-cli/lib/api/database_task_service.dart`

**设计用途**: 提供数据库任务相关的API服务

**主要方法**:
- `getTasks()` - 获取任务列表
- `getStatistics()` - 获取统计信息
- `deleteTask()` - 删除任务
- `rerunTask()` - 重新运行任务
- `cancelTask()` - 取消任务

**使用情况**: 
- ❌ 仅被DatabaseTaskListPage使用
- ❌ 未被其他页面引用

**建议**: 如果DatabaseTaskListPage删除，则此文件也删除

### 2.2 过大的前端文件

#### 2.2.1 PreviewPage (1892行)

**文件路径**: `clients/flutter-web-cli/lib/pages/preview_page.dart`

**当前使用情况**: ✅ 实际使用（main.dart中引用）

**功能模块**:
1. 任务列表视图
   - 任务列表加载
   - 任务卡片展示
   - 任务操作按钮（重新运行、终止、删除）
   - 任务状态显示

2. 任务详情视图
   - 任务信息展示
   - 配置快照展示
   - 源目录列表
   - 策略配置展示
   - 扫描结果展示
   - 预览结果展示
   - 执行结果展示

3. 预览视图
   - 变更记录表格
   - 筛选和搜索
   - 分页功能
   - 状态栏

4. 任务操作
   - 创建新任务
   - 执行最新任务
   - 停止任务
   - 重新运行任务
   - 取消任务
   - 删除任务

**拆分策略**:
- 提取任务列表相关逻辑到 `widgets/task_list_widget.dart`
- 提取任务详情相关逻辑到 `widgets/task_detail_widget.dart`
- 提取预览相关逻辑到 `widgets/preview_widget.dart`
- 提取变更记录表格到 `widgets/change_record_table.dart`
- 提取任务操作逻辑到 `utils/task_operations.dart`

#### 2.2.2 TaskDetailPage (2048行)

**文件路径**: `clients/flutter-web-cli/lib/pages/task_detail_page.dart`

**当前使用情况**: ❌ 未使用

**功能模块**:
1. 任务信息展示
   - 任务基本信息
   - 任务状态
   - 进度条

2. 阶段控制
   - 扫描阶段控制
   - 预览阶段控制
   - 执行阶段控制
   - 阶段依赖检查

3. WebSocket实时更新
   - 连接管理
   - 消息处理
   - 状态更新

4. 任务操作
   - 删除任务
   - 取消任务
   - 暂停任务
   - 恢复任务
   - 重试失败

**拆分策略**:
- 提取任务信息卡片到 `widgets/task_info_card.dart`
- 提取阶段控制卡片到 `widgets/stage_control_card.dart`
- 提取WebSocket处理逻辑到 `utils/websocket_handler.dart`
- 提取任务操作逻辑到 `utils/task_operations.dart`

#### 2.2.3 GlobalSettingsPage (1023行)

**文件路径**: `clients/flutter-web-cli/lib/pages/global_settings_page.dart`

**当前使用情况**: ✅ 实际使用（main.dart中引用）

**功能模块**:
1. 线程池设置
   - 预览线程数
   - 执行线程数
   - 线程池模式

2. 扫描设置
   - 递归模式
   - 递归深度
   - 扫描过滤器

3. 过滤规则
   - 规则列表
   - 添加/删除规则

4. 文件类型树
   - 文件类型分类
   - 选择/取消选择
   - 树形结构

5. 运行设置
   - 自动刷新
   - 预览限制
   - 执行限制

**拆分策略**:
- 提取线程池设置到 `widgets/thread_pool_settings.dart`
- 提取扫描设置到 `widgets/scan_settings.dart`
- 提取过滤规则到 `widgets/filter_rules.dart`
- 提取文件类型树到 `widgets/file_type_tree.dart`
- 提取运行设置到 `widgets/run_settings.dart`

## 3. 迭代清单

### 3.1 第一阶段：删除未使用的代码

#### 迭代 1.1: 删除未使用的页面和类

**优先级**: 高

**任务描述**:
删除未被使用的页面和类，消除代码重复

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/task_list_page.dart`
- `clients/flutter-web-cli/lib/pages/database_task_list_page.dart`
- `clients/flutter-web-cli/lib/pages/task_detail_page.dart`
- `clients/flutter-web-cli/lib/api/database_task_service.dart`

**执行步骤**:
1. 确认这些文件未被其他代码引用
2. 删除 `task_list_page.dart`
3. 删除 `database_task_list_page.dart`
4. 删除 `task_detail_page.dart`
5. 删除 `database_task_service.dart`
6. 从 `main.dart` 中移除相关导入（如果有）
7. 从 `providers.dart` 中移除相关 provider（如果有）

**验证方法**:
- 运行 `flutter analyze` 检查是否有未解决的引用
- 运行 `flutter build web` 确保编译成功
- 手动测试应用功能正常

**预计耗时**: 30分钟

**状态**: ✅ 已完成

---

#### 迭代 1.2: 从 PreviewPage 中移除未使用的导入

**优先级**: 高

**任务描述**:
清理 PreviewPage 中未使用的导入语句

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/preview_page.dart`

**执行步骤**:
1. 检查所有导入语句
2. 移除未使用的导入
3. 运行 `flutter analyze` 确认无警告

**验证方法**:
- 运行 `flutter analyze` 检查是否有未使用的导入
- 手动测试应用功能正常

**预计耗时**: 10分钟

**状态**: ✅ 已完成

---

### 3.2 第二阶段：拆分 PreviewPage

#### 迭代 2.1: 提取任务列表相关逻辑

**优先级**: 高

**任务描述**:
将 PreviewPage 中的任务列表相关逻辑提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/preview_page.dart`
- `clients/flutter-web-cli/lib/widgets/task_list_widget.dart` (新建)

**执行步骤**:
1. 创建 `widgets/task_list_widget.dart`
2. 提取任务列表状态变量
3. 提取任务列表加载方法
4. 提取任务列表刷新方法
5. 提取任务列表展示方法
6. 提取任务卡片构建方法
7. 提取任务操作方法
8. 在 PreviewPage 中使用新的 widget

**提取内容**:
- 状态变量: `_tasks`, `_isLoading`, `_selectedTask`, `_viewMode`
- 方法: `_loadTasks()`, `_refreshTasks()`, `_buildTaskListView()`, `_buildTaskList()`, `_buildTaskCard()`, `_rerunTask()`, `_cancelTask()`, `_deleteTask()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试任务列表功能
- 测试任务操作（重新运行、终止、删除）

**预计耗时**: 2小时

**状态**: ✅ 已完成

---

#### 迭代 2.2: 提取任务详情相关逻辑

**优先级**: 高

**任务描述**:
将 PreviewPage 中的任务详情相关逻辑提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/preview_page.dart`
- `clients/flutter-web-cli/lib/widgets/task_detail_widget.dart` (新建)

**执行步骤**:
1. 创建 `widgets/task_detail_widget.dart`
2. 提取任务详情状态变量
3. 提取任务详情刷新方法
4. 提取任务详情展示方法
5. 提取任务信息卡片构建方法
6. 提取配置快照卡片构建方法
7. 提取源目录列表构建方法
8. 提取策略配置构建方法
9. 提取扫描结果卡片构建方法
10. 提取预览结果卡片构建方法
11. 提取执行结果卡片构建方法
12. 在 PreviewPage 中使用新的 widget

**提取内容**:
- 状态变量: `_selectedTask`, `_taskDetail`
- 方法: `_refreshTaskDetail()`, `_refreshTaskDetailSafe()`, `_buildTaskDetailView()`, `_buildTaskDetailHeader()`, `_buildTaskInfoCard()`, `_buildConfigSnapshotCard()`, `_buildSourceDirectoriesList()`, `_buildStrategyConfig()`, `_buildScanResultCard()`, `_buildPreviewResultCard()`, `_buildExecutionResultCard()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试任务详情功能
- 测试所有卡片展示

**预计耗时**: 3小时

**状态**: ✅ 已完成

---

#### 迭代 2.3: 提取预览相关逻辑

**优先级**: 高

**任务描述**:
将 PreviewPage 中的预览相关逻辑提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/preview_page.dart`
- `clients/flutter-web-cli/lib/widgets/preview_widget.dart` (新建)

**执行步骤**:
1. 创建 `widgets/preview_widget.dart`
2. 提取预览状态变量
3. 提取变更记录加载方法
4. 提取当前页记录获取方法
5. 提取预览展示方法
6. 提取筛选栏构建方法
7. 提取预览表格构建方法
8. 提取分页构建方法
9. 在 PreviewPage 中使用新的 widget

**提取内容**:
- 状态变量: `_changeRecords`, `_searchFilter`, `_statusFilter`, `_operationTypeFilter`, `_hideUnchanged`, `_currentPage`, `_pageSize`, `_totalRecords`, `_totalPages`
- 方法: `_fetchChanges()`, `_getCurrentPageRecords()`, `_buildPreviewView()`, `_buildFilterBar()`, `_buildPreviewTable()`, `_buildPagination()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试预览功能
- 测试筛选、搜索、分页

**预计耗时**: 2小时

**状态**: ✅ 已完成

---

#### 迭代 2.4: 提取任务操作逻辑

**优先级**: 中

**任务描述**:
将 PreviewPage 中的任务操作逻辑提取到独立的工具类

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/preview_page.dart`
- `clients/flutter-web-cli/lib/utils/task_operations.dart` (新建)

**执行步骤**:
1. 创建 `utils/task_operations.dart`
2. 提取创建新任务方法
3. 提取执行最新任务方法
4. 提取停止任务方法
5. 提取重新运行任务方法
6. 提取取消任务方法
7. 提取删除任务方法
8. 在 PreviewPage 中使用新的工具类

**提取内容**:
- 方法: `_createNewTask()`, `_executeLatestTask()`, `_stopTask()`, `_rerunTask()`, `_cancelTask()`, `_deleteTask()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试所有任务操作

**预计耗时**: 1.5小时

**状态**: ✅ 已完成

---

#### 迭代 2.5: 提取变更记录表格

**优先级**: 中

**任务描述**:
将 PreviewPage 中的变更记录表格提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/preview_page.dart`
- `clients/flutter-web-cli/lib/widgets/change_record_table.dart` (新建)

**执行步骤**:
1. 创建 `widgets/change_record_table.dart`
2. 提取变更记录表格构建方法
3. 提取表格列定义
4. 提取表格行构建方法
5. 在 PreviewPage 中使用新的 widget

**提取内容**:
- 方法: `_buildPreviewTable()` 中的表格部分

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试变更记录表格

**预计耗时**: 1小时

**状态**: ✅ 已完成

---

### 3.3 第三阶段：拆分 GlobalSettingsPage

#### 迭代 3.1: 提取线程池设置

**优先级**: 中

**任务描述**:
将 GlobalSettingsPage 中的线程池设置提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/global_settings_page.dart`
- `clients/flutter-web-cli/lib/widgets/thread_pool_settings.dart` (新建)

**执行步骤**:
1. 创建 `widgets/thread_pool_settings.dart`
2. 提取线程池状态变量
3. 提取线程池设置展示方法
4. 在 GlobalSettingsPage 中使用新的 widget

**提取内容**:
- 状态变量: `_previewThreads`, `_executionThreads`, `_threadPoolMode`
- 方法: `_buildThreadPoolSection()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试线程池设置

**预计耗时**: 1小时

**状态**: ✅ 已完成

---

#### 迭代 3.2: 提取扫描设置

**优先级**: 中

**任务描述**:
将 GlobalSettingsPage 中的扫描设置提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/global_settings_page.dart`
- `clients/flutter-web-cli/lib/widgets/scan_settings.dart` (新建)

**执行步骤**:
1. 创建 `widgets/scan_settings.dart`
2. 提取扫描设置状态变量
3. 提取扫描设置展示方法
4. 在 GlobalSettingsPage 中使用新的 widget

**提取内容**:
- 状态变量: `_recursionMode`, `_recursionDepth`, `_minRecursionDepth`, `_maxRecursionDepth`, `_scanFilterList`
- 方法: `_buildScanSettingsSection()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试扫描设置

**预计耗时**: 1.5小时

**状态**: ✅ 已完成

---

#### 迭代 3.3: 提取过滤规则

**优先级**: 中

**任务描述**:
将 GlobalSettingsPage 中的过滤规则提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/global_settings_page.dart`
- `clients/flutter-web-cli/lib/widgets/filter_rules.dart` (新建)

**执行步骤**:
1. 创建 `widgets/filter_rules.dart`
2. 提取过滤规则状态变量
3. 提取过滤规则展示方法
4. 在 GlobalSettingsPage 中使用新的 widget

**提取内容**:
- 状态变量: `_scanFilterList`, `_newFilterRule`
- 方法: `_buildFilterRulesSection()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试过滤规则

**预计耗时**: 1小时

**状态**: ✅ 已完成

---

#### 迭代 3.4: 提取文件类型树

**优先级**: 中

**任务描述**:
将 GlobalSettingsPage 中的文件类型树提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/global_settings_page.dart`
- `clients/flutter-web-cli/lib/widgets/file_type_tree.dart` (新建)

**执行步骤**:
1. 创建 `widgets/file_type_tree.dart`
2. 提取文件类型树相关类
3. 提取文件类型树状态变量
4. 提取文件类型树展示方法
5. 提取节点选择方法
6. 在 GlobalSettingsPage 中使用新的 widget

**提取内容**:
- 类: `FileTypeNode`
- 状态变量: `_customFileTypes`, `_newFileType`
- 方法: `_buildFileTypeTreeSection()`, `_buildCategoryNode()`, `_updateNodeSelection()`, `_updateChildren()`, `_updateParentSelection()`, `_selectAll()`, `_getSelectedFileTypes()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试文件类型树

**预计耗时**: 2小时

**状态**: ✅ 已完成

---

#### 迭代 3.5: 提取运行设置

**优先级**: 中

**任务描述**:
将 GlobalSettingsPage 中的运行设置提取到独立的 widget

**涉及文件**:
- `clients/flutter-web-cli/lib/pages/global_settings_page.dart`
- `clients/flutter-web-cli/lib/widgets/run_settings.dart` (新建)

**执行步骤**:
1. 创建 `widgets/run_settings.dart`
2. 提取运行设置状态变量
3. 提取运行设置展示方法
4. 在 GlobalSettingsPage 中使用新的 widget

**提取内容**:
- 状态变量: `_autoRefresh`, `_previewLimit`, `_executionLimit`
- 方法: `_buildRunSettingsSection()`

**验证方法**:
- 运行 `flutter analyze` 检查代码质量
- 手动测试运行设置

**预计耗时**: 1小时

**状态**: ✅ 已完成

---

### 3.4 第四阶段：代码优化和测试

#### 迭代 4.1: 代码质量检查

**优先级**: 高

**任务描述**:
运行代码质量检查工具，修复所有警告和错误

**执行步骤**:
1. 运行 `flutter analyze`
2. 修复所有警告
3. 修复所有错误
4. 运行 `flutter format` 格式化代码
5. 检查代码覆盖率

**验证方法**:
- `flutter analyze` 无警告和错误
- 代码格式符合规范

**预计耗时**: 1小时

**状态**: ✅ 已完成

---

#### 迭代 4.2: 功能测试

**优先级**: 高

**任务描述**:
对重构后的代码进行全面的功能测试

**测试用例**:
1. 任务列表功能测试
   - 任务列表加载
   - 任务详情查看
   - 任务操作（重新运行、终止、删除）

2. 任务详情功能测试
   - 任务信息展示
   - 配置快照展示
   - 扫描结果展示
   - 预览结果展示
   - 执行结果展示

3. 预览功能测试
   - 变更记录展示
   - 筛选功能
   - 搜索功能
   - 分页功能

4. 全局设置功能测试
   - 线程池设置
   - 扫描设置
   - 过滤规则
   - 文件类型树
   - 运行设置

**验证方法**:
- 手动测试所有功能
- 记录测试结果
- 修复发现的问题

**预计耗时**: 3小时

**状态**: ✅ 已完成

---

#### 迭代 4.3: 性能测试

**优先级**: 中

**任务描述**:
对重构后的代码进行性能测试

**测试内容**:
1. 页面加载时间
2. 任务列表加载时间
3. 变更记录加载时间
4. 内存使用情况

**验证方法**:
- 使用 Flutter DevTools 进行性能分析
- 对比重构前后的性能指标

**预计耗时**: 2小时

**状态**: ✅ 已完成

---

#### 迭代 4.4: 文档更新

**优先级**: 中

**任务描述**:
更新相关文档，反映代码结构的变化

**更新内容**:
1. 更新项目结构文档
2. 更新 API 文档
3. 更新开发指南
4. 更新测试文档

**验证方法**:
- 文档内容准确
- 文档格式规范

**预计耗时**: 2小时

**状态**: 待执行

---

## 4. 迭代执行计划

### 4.1 执行顺序

1. **第一阶段**: 删除未使用的代码（1小时）
   - 迭代 1.1: 删除未使用的页面和类（30分钟）
   - 迭代 1.2: 从 PreviewPage 中移除未使用的导入（10分钟）

2. **第二阶段**: 拆分 PreviewPage（9.5小时）
   - 迭代 2.1: 提取任务列表相关逻辑（2小时）
   - 迭代 2.2: 提取任务详情相关逻辑（3小时）
   - 迭代 2.3: 提取预览相关逻辑（2小时）
   - 迭代 2.4: 提取任务操作逻辑（1.5小时）
   - 迭代 2.5: 提取变更记录表格（1小时）

3. **第三阶段**: 拆分 GlobalSettingsPage（7.5小时）
   - 迭代 3.1: 提取线程池设置（1小时）
   - 迭代 3.2: 提取扫描设置（1.5小时）
   - 迭代 3.3: 提取过滤规则（1小时）
   - 迭代 3.4: 提取文件类型树（2小时）
   - 迭代 3.5: 提取运行设置（1小时）

4. **第四阶段**: 代码优化和测试（8小时）
   - 迭代 4.1: 代码质量检查（1小时）
   - 迭代 4.2: 功能测试（3小时）
   - 迭代 4.3: 性能测试（2小时）
   - 迭代 4.4: 文档更新（2小时）

**总计**: 26小时

### 4.2 里程碑

- **里程碑 1**: 完成第一阶段（删除未使用的代码）
- **里程碑 2**: 完成第二阶段（拆分 PreviewPage）
- **里程碑 3**: 完成第三阶段（拆分 GlobalSettingsPage）
- **里程碑 4**: 完成第四阶段（代码优化和测试）

### 4.3 风险管理

**风险 1**: 拆分过程中引入新的 bug
- **缓解措施**: 每个迭代完成后进行测试
- **应急方案**: 保留原始代码备份，必要时回滚

**风险 2**: 拆分后性能下降
- **缓解措施**: 进行性能测试，对比重构前后的性能指标
- **应急方案**: 优化性能瓶颈

**风险 3**: 文档更新不及时
- **缓解措施**: 每个阶段完成后立即更新文档
- **应急方案**: 集中时间更新所有文档

## 5. 验收标准

### 5.1 代码质量

- ✅ `flutter analyze` 无警告和错误
- ✅ 代码格式符合规范
- ✅ 代码覆盖率不低于 80%

### 5.2 功能完整性

- ✅ 所有现有功能正常工作
- ✅ 无功能缺失
- ✅ 无功能退化

### 5.3 性能指标

- ✅ 页面加载时间不超过重构前的 110%
- ✅ 内存使用不超过重构前的 110%

### 5.4 文档完整性

- ✅ 所有文档已更新
- ✅ 文档内容准确
- ✅ 文档格式规范

## 6. 附录

### 6.1 相关文档

- [任务管理系统开发指南](../backend/docs/task_management_development_guide.md)
- [测试计划和迭代清单](../backend/docs/test_plan_and_iteration.md)
- [API设计文档](../backend/docs/api_design.md)

### 6.2 工具和命令

```bash
# 代码分析
flutter analyze

# 代码格式化
flutter format .

# 构建测试
flutter build web

# 运行测试
flutter test

# 性能分析
flutter pub global activate devtools
flutter pub global run devtools
```

### 6.3 联系方式

- **维护者**: 开发团队
- **邮箱**: chrse1997@163.com
- **文档版本**: 1.0.0
- **最后更新**: 2026-02-25
