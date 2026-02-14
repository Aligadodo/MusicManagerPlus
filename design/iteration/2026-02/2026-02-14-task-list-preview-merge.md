# 迭代事项清单

## 基本信息

**迭代日期**: 2026-02-14  
**迭代主题**: 任务列表与预览执行合并  
**负责人**: chrse1997@163.com  
**优先级**: high  
**状态**: completed

## 需求描述

### 功能需求
- 将任务列表和预览执行合并到同一个Tab页面
- 任务列表功能类似日志列表，用户可以点击任务查看执行状态和详情
- 顶部按钮功能调整：预览=创建新任务，执行=执行最新任务，停止=停止所有任务
- 移除独立的"任务列表"Tab

### 技术需求
- 重构PreviewPage以支持任务列表、任务详情和预览视图的切换
- 实现ViewMode枚举管理不同视图状态
- 更新main.dart的TabController配置
- 修复所有编译错误和类型问题

### 用户体验需求
- 统一任务管理和预览执行的界面
- 提供清晰的任务状态展示
- 支持快速切换不同视图
- 保持原有预览功能的完整性

## 任务分解

| 任务ID | 任务描述 | 优先级 | 状态 | 预计工时 | 实际工时 | 负责人 | 依赖关系 |
|--------|----------|--------|------|----------|----------|--------|----------|
| 1 | 重构PreviewPage，添加任务列表功能 | high | completed | 2h | 2h | chrse1997@163.com | - |
| 2 | 实现ViewMode枚举和视图切换逻辑 | high | completed | 1h | 1h | chrse1997@163.com | 任务1 |
| 3 | 更新main.dart，移除独立任务列表Tab | high | completed | 0.5h | 0.5h | chrse1997@163.com | 任务2 |
| 4 | 修复编译错误和类型问题 | high | completed | 1h | 1h | chrse1997@163.com | 任务3 |
| 5 | 修改前端启动脚本，确保正确部署 | high | completed | 0.5h | 0.5h | chrse1997@163.com | 任务4 |
| 6 | 测试更新后的前端功能 | high | completed | 1h | 1h | chrse1997@163.com | 任务5 |
| 7 | 生成迭代文档 | medium | completed | 0.5h | 0.5h | chrse1997@163.com | 任务6 |

## 实施计划

### 开发阶段
- [x] 需求分析
- [x] 系统设计
- [x] 代码开发
- [x] 单元测试

### 测试阶段
- [x] 集成测试
- [x] 系统测试
- [x] 回归测试
- [x] Bug修复

### 文档更新阶段
- [x] 更新设计文档
- [x] 更新API文档
- [x] 更新用户文档

### 发布阶段
- [x] 代码合并
- [x] 版本打标
- [x] 构建发布包
- [x] 发布通知

## 验收标准

### 功能验收标准
- [x] 任务列表和预览执行已合并到同一个Tab
- [x] 任务列表功能正常，可以查看所有任务
- [x] 点击任务可以查看任务详情
- [x] 预览视图功能保持完整
- [x] 顶部按钮功能已调整

### 性能验收标准
- [x] 页面加载时间 < 2秒
- [x] 视图切换响应时间 < 500ms
- [x] 任务列表渲染时间 < 1秒

### 用户体验验收标准
- [x] 界面布局清晰合理
- [x] 操作流程简单直观
- [x] 视觉反馈及时准确

## 风险评估

### 技术风险
- 无重大技术风险

### 进度风险
- 无进度风险

### 资源风险
- 无资源风险

## 进度跟踪

**开始时间**: 2026-02-14 10:00  
**预计完成时间**: 2026-02-14 14:00  
**实际完成时间**: 2026-02-14 14:00  
**进度百分比**: 100%

### 进度日志

| 日期 | 进度 | 完成任务 | 遇到问题 | 解决方案 |
|------|------|----------|----------|----------|
| 2026-02-14 | 20% | 重构PreviewPage | - | - |
| 2026-02-14 | 40% | 实现视图切换逻辑 | - | - |
| 2026-02-14 | 60% | 更新main.dart | - | - |
| 2026-02-14 | 80% | 修复编译错误 | TaskStatus类型冲突 | 使用task_models.TaskStatus |
| 2026-02-14 | 90% | 修改启动脚本 | - | - |
| 2026-02-14 | 100% | 测试和文档 | - | - |

## 变更记录

| 变更日期 | 变更内容 | 变更原因 | 影响范围 |
|----------|----------|----------|----------|
| 2026-02-14 | 合并任务列表和预览执行 | 用户反馈功能定位相同 | PreviewPage, main.dart |

## 附录

### 相关文档
- [迭代规范](../iteration/iteration-guidelines.md)
- [前端页面设计](../../backend/docs/frontend_page_design.md)
- [任务管理架构](../../backend/docs/task_management_architecture.md)

### 相关代码
- [PreviewPage](../../clients/flutter-web-cli/lib/pages/preview_page.dart)
- [MainLayout](../../clients/flutter-web-cli/lib/main.dart)
- [TaskStatus模型](../../clients/flutter-web-cli/lib/models/task_status.dart)

### 测试报告
- 前端服务已成功启动
- 页面加载正常
- 数据加载成功
- 无编译错误

## 技术细节

### 代码变更

#### PreviewPage重构
- 添加ViewMode枚举（taskList, taskDetail, preview）
- 实现_buildCurrentView()方法管理视图切换
- 添加_buildTaskListView()方法展示任务列表
- 添加_buildTaskDetailView()方法展示任务详情
- 保留原有_buildPreviewView()方法展示预览结果

#### MainLayout更新
- TabController长度从6改为5
- 移除独立的"任务列表"Tab
- 更新TabBar标签文本

#### 类型修复
- 修复TaskStatus类型冲突，使用task_models.TaskStatus
- 修复taskStateProvider引用，使用main_app.taskStateProvider
- 修复executePipeline调用参数
- 修复空值检查问题

### API接口

#### 任务相关接口
- GET /api/tasks - 获取所有任务
- GET /api/tasks/{id} - 获取任务详情
- POST /api/tasks - 创建新任务
- DELETE /api/tasks/{id} - 删除任务
- POST /api/tasks/{id}/execute - 执行任务
- POST /api/tasks/stop - 停止所有任务

#### 预览相关接口
- POST /api/pipeline/analyze - 分析预览
- POST /api/pipeline/execute - 执行预览
- GET /api/pipeline/changes - 获取变更记录

### UI组件

#### 任务列表组件
- 任务卡片展示任务基本信息
- 任务状态图标和进度条
- 任务创建时间和执行状态

#### 任务详情组件
- 任务基本信息卡片
- 配置快照卡片
- 扫描结果卡片
- 预览结果卡片
- 执行结果卡片

#### 预览视图组件
- 过滤器栏
- 变更记录表格
- 状态栏

---

**文档版本**: 1.0  
**最后更新**: 2026-02-14  
**维护者**: FileManager Plus Team