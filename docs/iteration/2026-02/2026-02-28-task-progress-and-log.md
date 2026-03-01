# 任务进度修复与日志功能迭代

**迭代编号**: 2026-02-28-task-progress-and-log
**创建日期**: 2026-02-28
**负责人**: AI Assistant
**状态**: 进行中

## 1. 背景与目标

### 1.1 背景
用户反馈以下问题：
1. 任务进度显示异常，出现超过100%的情况（如10000%）
2. 任务执行过程中无法查看实时日志，无法了解任务执行状态
3. 任务卡在预览阶段，无法判断是否在正常运行或出现异常

### 1.2 目标
1. 修复进度显示问题，确保进度值在0-100%范围内
2. 实现任务日志功能，支持在任务详情页面查看实时日志
3. 完善任务日志存储，支持文件系统和数据库两种存储方式

## 2. 问题分析

### 2.1 进度显示问题
**根因分析**:
- 后端 `PipelineTaskManager.updateTaskProgress()` 方法存储的进度值是 0-100 的百分比值
- 前端 `task_list_widget.dart` 假设进度值是 0.0-1.0 的比例值，显示时乘以100
- 导致显示值 = 实际进度 × 100，如进度为100时显示为10000%

**修复方案**:
- 方案A: 修改前端，不再乘以100（推荐）
- 方案B: 修改后端，存储0.0-1.0的比例值

### 2.2 任务日志功能
**现有资源**:
- 数据库已有 `task_operation_log` 表，但字段设计不够完善
- 文件系统存储已有 `writeTaskLog` 方法

**需求设计**:
- 日志字段：任务ID、时间戳、日志级别、日志类型、日志详情
- 存储方式：文件系统（测试环境）+ 数据库（生产环境）
- 前端展示：任务详情页面添加日志面板

## 3. 迭代任务清单

### 3.1 进度修复
- [ ] 修复前端进度显示逻辑
- [ ] 验证进度更新流程

### 3.2 任务日志功能
- [ ] 设计任务日志表结构
- [ ] 创建数据库表和Mapper
- [ ] 实现日志存储接口
- [ ] 实现日志查询API
- [ ] 前端添加日志面板组件
- [ ] 集成日志实时更新

### 3.3 测试与验证
- [ ] 编写测试用例
- [ ] 执行测试验证
- [ ] 生成测试报告

## 4. 技术方案

### 4.1 任务日志表设计
```sql
CREATE TABLE IF NOT EXISTS task_execution_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    log_level VARCHAR(16) NOT NULL,  -- INFO, WARN, ERROR, DEBUG
    log_type VARCHAR(32) NOT NULL,   -- SCAN, PREVIEW, EXECUTE, SYSTEM
    message TEXT NOT NULL,
    details TEXT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);
```

### 4.2 API设计
- `POST /api/tasks/{taskId}/logs` - 添加日志
- `GET /api/tasks/{taskId}/logs` - 获取日志列表
- `GET /api/tasks/{taskId}/logs/stream` - WebSocket日志流

## 5. 风险评估

| 风险项 | 影响 | 概率 | 缓解措施 |
|--------|------|------|----------|
| 日志量大导致性能问题 | 高 | 中 | 分页查询、定期清理 |
| WebSocket连接不稳定 | 中 | 低 | 自动重连机制 |

## 6. 验收标准

1. 进度显示正确，不超过100%
2. 任务详情页面可查看日志
3. 日志实时更新
4. 测试用例全部通过

## 7. 变更记录

| 日期 | 变更内容 | 负责人 |
|------|----------|--------|
| 2026-02-28 | 创建迭代文档 | AI Assistant |
