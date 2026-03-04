# 任务数据列表展示组件迭代

**迭代编号**: 2026-03-03-task-data-list-component
**创建日期**: 2026-03-03
**负责人**: AI Assistant
**状态**: 开发中（后端API完成，前端组件完成，页面集成完成）

## 1. 背景与目标

### 1.1 背景
当前任务详情的扫描结果、预览结果、执行结果页面只展示统计信息（状态、时间、数量等），缺乏详细的数据列表展示。用户无法查看具体的扫描文件列表、变更记录详情、执行结果详情等信息。

### 1.2 目标
1. 设计并实现通用的数据列表展示组件，支持扫描/预览/执行三种数据类型
2. 支持分页查询，避免一次性加载大量数据
3. 支持搜索和筛选功能，方便用户快速定位数据
4. 组件高度可复用，通过配置即可适配不同数据类型
5. 参考老架构的 ChangeRecord 展示方式，提供更详细的数据展示
6. **解决通用性和易用性的平衡问题**：不同阶段展示不同的字段范围

## 2. 现有问题分析

### 2.1 当前实现
- **ScanResultCard**: 只展示扫描状态、时间、文件数量统计
- **PreviewResultCard**: 只展示预览状态、时间、变更数量统计
- **ExecutionResultCard**: 只展示执行状态、时间、成功/失败数量统计
- **ChangeRecordTable**: 简单的表格展示，无分页、搜索、筛选功能

### 2.2 问题根因
1. 数据存储在文件系统中，缺乏高效的分页查询机制
2. 前端组件高度耦合，无法复用
3. 缺少统一的列表展示组件设计
4. 用户体验不完善，无法查看详细数据

## 3. 设计方案

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    TaskDataListPage                         │
│                   (任务数据列表页面)                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │              GenericDataList                        │   │
│  │              (通用数据列表组件)                      │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │           SearchFilterBar                   │   │   │
│  │  │  (搜索框 + 筛选条件 + 操作按钮)              │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │           DataTableView                     │   │   │
│  │  │  (动态列定义 + 数据行 + 行操作)              │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │           PaginationBar                     │   │   │
│  │  │  (页码 + 每页数量 + 总记录数)                │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 组件设计

#### 3.2.1 通用列表组件 (GenericDataList)

**职责**: 提供通用的数据列表展示能力，支持分页、搜索、筛选

**核心属性**:
```dart
class GenericDataList extends StatefulWidget {
  // 列配置（核心：通过列配置控制展示哪些字段）
  final List<ColumnConfig> columns;
  
  // 数据获取函数
  final DataLoadCallback onLoadData;
  
  // 是否支持多选
  final bool multiSelect;
  
  // 是否支持行选择
  final bool enableRowSelection;
  
  // 行选择回调
  final RowSelectCallback? onRowSelect;
  
  // 行双击回调
  final RowDoubleTapCallback? onRowDoubleTap;
  
  // 行右键回调
  final RowContextMenuCallback? onRowContextMenu;
  
  // 空数据提示
  final Widget? emptyWidget;
  
  // 加载中提示
  final Widget? loadingWidget;
  
  // 标题
  final String? title;
  
  // 是否显示搜索框
  final bool showSearch;
  
  // 是否显示分页
  final bool showPagination;
  
  // 默认页大小
  final int defaultPageSize;
  
  // 页大小选项
  final List<int> pageSizeOptions;
  
  // 是否显示列设置按钮
  final bool showColumnSettings;
  
  // 是否显示刷新按钮
  final bool showRefresh;
  
  // 自定义工具栏
  final List<Widget>? toolbarActions;
  
  // 行高
  final double rowHeight;
  
  // 表头高度
  final double headerHeight;
}
```

**核心功能**:
1. **动态列渲染**: 根据 ColumnConfig 动态生成表格列
2. **分页控制**: 支持页码跳转、每页数量调整
3. **搜索功能**: 支持关键词搜索
4. **筛选功能**: 支持多条件筛选
5. **排序功能**: 支持点击列头排序
6. **行操作**: 支持行选择、双击、右键菜单
7. **批量操作**: 支持多选
8. **空状态处理**: 无数据时显示友好提示
9. **加载状态**: 数据加载时显示加载动画
10. **错误处理**: 数据加载失败时显示错误信息
11. **列配置管理**: 支持显示/隐藏列

#### 3.2.2 列配置方案 (ColumnConfig)

**设计思想**: 通过列配置控制不同阶段展示哪些字段，底层使用统一的数据模型

**列配置定义**:
```dart
class ColumnConfig {
  // 列标识（对应数据模型中的字段名）
  final String key;
  
  // 列标题
  final String title;
  
  // 列宽
  final double width;
  
  // 是否可排序
  final bool sortable;
  
  // 是否可筛选
  final bool filterable;
  
  // 是否默认显示
  final bool visible;
  
  // 是否可隐藏
  final bool hideable;
  
  // 自定义渲染器（可选）
  final Widget Function(dynamic value, dynamic row)? customRender;
  
  // 单元格对齐方式
  final Alignment alignment;
  
  // 列类型
  final ColumnType columnType;
  
  // 格式化函数
  final String Function(dynamic value)? formatter;
  
  // 列分组
  final String? group;
}

enum ColumnType {
  text,       // 文本类型
  number,     // 数字类型
  date,       // 日期类型
  boolean,    // 布尔类型
  enumeration, // 枚举类型
  custom,     // 自定义类型
}
```

**预定义列配置集合**:

针对不同阶段，我们预定义不同的列配置集合：

```dart
// 扫描阶段列配置
class ScanColumnConfigs {
  static List<ColumnConfig> get defaultColumns => [
    ColumnConfig(key: 'originalName', title: '文件名', width: 200, sortable: true, filterable: true, visible: true, hideable: false),
    ColumnConfig(key: 'originalPath', title: '文件路径', width: 300, sortable: true, filterable: true, visible: true, hideable: false),
    ColumnConfig(key: 'fileSize', title: '文件大小', width: 100, sortable: true, filterable: true, visible: true, hideable: true, formatter: _formatFileSize),
    ColumnConfig(key: 'fileType', title: '文件类型', width: 80, sortable: true, filterable: true, visible: true, hideable: true),
    ColumnConfig(key: 'lastModified', title: '修改时间', width: 150, sortable: true, filterable: true, visible: true, hideable: true, formatter: _formatTimestamp),
    ColumnConfig(key: 'metadata', title: '元数据', width: 200, sortable: false, filterable: false, visible: false, hideable: true, customRender: _renderMetadata),
  ];
}

// 预览阶段列配置
class PreviewColumnConfigs {
  static List<ColumnConfig> get defaultColumns => [
    ColumnConfig(key: 'originalName', title: '原文件名', width: 180, sortable: true, filterable: true, visible: true, hideable: false),
    ColumnConfig(key: 'newName', title: '新文件名', width: 180, sortable: true, filterable: true, visible: true, hideable: false, customRender: _renderNewName),
    ColumnConfig(key: 'originalPath', title: '原路径', width: 250, sortable: true, filterable: true, visible: true, hideable: true),
    ColumnConfig(key: 'newPath', title: '新路径', width: 250, sortable: true, filterable: true, visible: false, hideable: true),
    ColumnConfig(key: 'operationType', title: '操作类型', width: 100, sortable: true, filterable: true, visible: true, hideable: false, customRender: _renderOperationType),
    ColumnConfig(key: 'status', title: '状态', width: 100, sortable: true, filterable: true, visible: true, hideable: false, customRender: _renderStatus),
    ColumnConfig(key: 'changed', title: '是否变更', width: 80, sortable: true, filterable: true, visible: true, hideable: true, customRender: _renderChanged),
    ColumnConfig(key: 'reason', title: '变更原因', width: 200, sortable: false, filterable: false, visible: false, hideable: true),
    ColumnConfig(key: 'extraParams', title: '额外参数', width: 150, sortable: false, filterable: false, visible: false, hideable: true),
    ColumnConfig(key: 'analyzeTime', title: '分析时间', width: 150, sortable: true, filterable: true, visible: false, hideable: true, formatter: _formatTimestamp),
  ];
}

// 执行阶段列配置
class ExecutionColumnConfigs {
  static List<ColumnConfig> get defaultColumns => [
    ColumnConfig(key: 'originalName', title: '原文件名', width: 180, sortable: true, filterable: true, visible: true, hideable: false),
    ColumnConfig(key: 'newName', title: '目标文件名', width: 180, sortable: true, filterable: true, visible: true, hideable: false),
    ColumnConfig(key: 'operationType', title: '操作类型', width: 100, sortable: true, filterable: true, visible: true, hideable: false, customRender: _renderOperationType),
    ColumnConfig(key: 'status', title: '执行状态', width: 100, sortable: true, filterable: true, visible: true, hideable: false, customRender: _renderExecutionStatus),
    ColumnConfig(key: 'failReason', title: '错误信息', width: 250, sortable: false, filterable: false, visible: false, hideable: true, customRender: _renderErrorMessage),
    ColumnConfig(key: 'executeTime', title: '执行时间', width: 150, sortable: true, filterable: true, visible: true, hideable: true, formatter: _formatTimestamp),
    ColumnConfig(key: 'duration', title: '耗时', width: 80, sortable: true, filterable: true, visible: true, hideable: true, formatter: _formatDuration),
    ColumnConfig(key: 'retryCount', title: '重试次数', width: 80, sortable: true, filterable: true, visible: false, hideable: true),
    ColumnConfig(key: 'originalPath', title: '原路径', width: 250, sortable: true, filterable: true, visible: false, hideable: true),
    ColumnConfig(key: 'newPath', title: '目标路径', width: 250, sortable: true, filterable: true, visible: false, hideable: true),
  ];
}
```

### 3.3 数据模型设计

**设计思想**: 使用统一的数据模型包含所有可能的字段，不同阶段通过列配置展示不同的字段子集。

#### 3.3.1 统一任务记录模型 (TaskRecord)

```dart
class TaskRecord {
  // 基础字段（所有阶段通用）
  final String id;                              // 记录ID
  final String originalName;                    // 原文件名
  final String newName;                         // 新文件名
  final String originalPath;                    // 原路径
  final String newPath;                         // 新路径
  
  // 文件信息字段（扫描阶段主要使用）
  final int? fileSize;                          // 文件大小（字节）
  final String? fileType;                       // 文件类型（扩展名）
  final int? lastModified;                      // 最后修改时间戳
  final Map<String, dynamic>? metadata;         // 元数据
  
  // 操作相关字段（预览和执行阶段使用）
  final String? operationType;                  // 操作类型
  final String? status;                         // 状态
  final String? reason;                         // 变更原因/说明
  final String? failReason;                     // 失败原因
  final Map<String, String>? extraParams;       // 额外参数
  
  // 状态标记字段
  final bool? changed;                          // 是否发生变更
  final bool? isCreate;                         // 是否是新建文件
  final bool? isDeleteOrMove;                   // 是否是删除或移动操作
  final bool? selected;                         // 是否被选中
  
  // 时间相关字段
  final int? analyzeTime;                       // 分析时间戳
  final int? executeTime;                       // 执行时间戳
  final int? duration;                          // 执行耗时（毫秒）
  
  // 执行相关字段
  final int? retryCount;                        // 重试次数
  final List<String>? processInfo;              // 处理信息列表
  
  // 便捷属性
  String get fileName => originalName;
  String get filePath => originalPath;
  String get targetName => newName;
  String get targetPath => newPath;
}
```

**不同阶段的字段使用策略**:

| 字段名 | 扫描阶段 | 预览阶段 | 执行阶段 | 说明 |
|--------|---------|---------|---------|------|
| **基础字段** |
| id | 隐藏 | 隐藏 | 隐藏 | 系统字段 |
| originalName | ✓ (文件名) | ✓ (原文件名) | ✓ (原文件名) | 主要字段 |
| newName | - | ✓ (新文件名) | ✓ (目标文件名) | 主要字段 |
| originalPath | ✓ (文件路径) | ✓ (原路径) | 可选 | 主要字段/可选 |
| newPath | - | 可选 | 可选 | 可选 |
| **文件信息字段** |
| fileSize | ✓ | 隐藏 | 隐藏 | 主要字段 |
| fileType | ✓ | 隐藏 | 隐藏 | 主要字段 |
| lastModified | ✓ | 隐藏 | 隐藏 | 主要字段 |
| metadata | 可选 | 隐藏 | 隐藏 | 预留字段 |
| **操作相关字段** |
| operationType | - | ✓ | ✓ | 主要字段 |
| status | - | ✓ (状态) | ✓ (执行状态) | 主要字段 |
| reason | - | 可选 | - | 可选 |
| failReason | - | - | 可选 | 预留字段 |
| extraParams | - | 可选 | - | 预留字段 |
| **状态标记字段** |
| changed | - | ✓ | - | 主要字段 |
| isCreate | - | 隐藏 | - | 隐藏 |
| isDeleteOrMove | - | 隐藏 | - | 隐藏 |
| selected | - | 隐藏 | - | 隐藏 |
| **时间相关字段** |
| analyzeTime | - | 可选 | - | 可选 |
| executeTime | - | - | ✓ | 主要字段 |
| duration | - | - | ✓ | 主要字段 |
| **执行相关字段** |
| retryCount | - | - | 可选 | 预留字段 |
| processInfo | - | - | 隐藏 | 隐藏 |

### 3.4 API接口设计

#### 3.4.1 扫描记录查询接口

```
GET /api/tasks/{taskId}/scan/records

请求参数:
{
  "page": 1,                    // 页码，默认1
  "pageSize": 20,               // 每页数量，默认20
  "search": "关键词",            // 搜索关键词
  "fileType": "mp3",            // 文件类型筛选
  "minSize": 1024,              // 最小文件大小（字节）
  "maxSize": 10485760,          // 最大文件大小（字节）
  "startTime": 1642234567890,   // 开始时间戳
  "endTime": 1642234567890,     // 结束时间戳
  "sortField": "originalName",  // 排序字段
  "sortOrder": "asc"            // 排序方向：asc/desc
}

响应数据:
{
  "success": true,
  "data": {
    "list": [...],
    "total": 1000,
    "page": 1,
    "pageSize": 20,
    "totalPages": 50,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

#### 3.4.2 预览记录查询接口

```
GET /api/tasks/{taskId}/preview/records

请求参数:
{
  "page": 1,
  "pageSize": 20,
  "search": "关键词",
  "operationType": "RENAME",
  "status": "CHANGED",
  "changed": true,
  "sortField": "originalName",
  "sortOrder": "asc"
}
```

#### 3.4.3 执行记录查询接口

```
GET /api/tasks/{taskId}/execution/records

请求参数:
{
  "page": 1,
  "pageSize": 20,
  "search": "关键词",
  "operationType": "RENAME",
  "status": "SUCCESS",
  "sortField": "executeTime",
  "sortOrder": "desc"
}
```

## 4. 实现状态

### 4.1 后端开发任务

#### 阶段1: 基础设施 ✅ 已完成
- [x] 创建数据模型类 (TaskRecordDTO)
- [x] 创建分页查询参数类 (PaginationParams)
- [x] 创建分页响应类 (PaginatedResponse)
- [x] 实现分页查询逻辑

#### 阶段2: 查询服务 ✅ 已完成
- [x] 实现 TaskDataQueryService 类
- [x] 实现扫描记录分页查询
- [x] 实现预览记录分页查询
- [x] 实现执行记录分页查询
- [x] 实现筛选条件处理
- [x] 实现排序功能
- [x] 实现搜索功能

#### 阶段3: API接口 ✅ 已完成
- [x] 创建 TaskDataController 类
- [x] 实现 GET /api/tasks/{taskId}/scan/records 接口
- [x] 实现 GET /api/tasks/{taskId}/preview/records 接口
- [x] 实现 GET /api/tasks/{taskId}/execution/records 接口
- [x] 实现单条记录查询接口框架

### 4.2 前端开发任务

#### 阶段1: 基础组件 ✅ 已完成
- [x] 创建 ColumnConfig 数据模型
- [x] 创建 PaginationParams 数据模型
- [x] 创建 PaginatedResponse 数据模型
- [x] 创建 GenericDataList 组件

#### 阶段2: 通用列表组件 ✅ 已完成
- [x] 实现 GenericDataList 核心功能
- [x] 实现动态列渲染
- [x] 实现分页控制逻辑
- [x] 实现搜索功能
- [x] 实现筛选功能
- [x] 实现排序功能
- [x] 实现行操作功能
- [x] 实现空状态、加载状态、错误状态处理

#### 阶段3: 数据模型 ✅ 已完成
- [x] 创建 TaskRecord 数据模型
- [x] 创建对应的 fromJson/toJson 方法

#### 阶段4: API服务 ✅ 已完成
- [x] 创建 TaskDataService 类
- [x] 实现扫描记录查询方法
- [x] 实现预览记录查询方法
- [x] 实现执行记录查询方法
- [x] 实现单条记录查询方法

#### 阶段5: 页面集成 ✅ 已完成
- [x] 创建 ScanResultPage 页面
- [x] 创建 PreviewResultPage 页面
- [x] 创建 ExecutionResultPage 页面
- [x] 配置各列表的列定义
- [x] 配置各列表的筛选条件
- [x] 配置各列表的操作按钮

## 5. 关键文件清单

### 5.1 后端文件

| 文件路径 | 说明 | 状态 |
|---------|------|------|
| `backend/src/main/java/com/filemanager/backend/dto/PaginationParams.java` | 分页查询参数 | ✅ 已完成 |
| `backend/src/main/java/com/filemanager/backend/dto/PaginatedResponse.java` | 分页响应 | ✅ 已完成 |
| `backend/src/main/java/com/filemanager/backend/dto/TaskRecordDTO.java` | 任务记录DTO | ✅ 已完成 |
| `backend/src/main/java/com/filemanager/backend/service/TaskDataQueryService.java` | 数据查询服务 | ✅ 已完成 |
| `backend/src/main/java/com/filemanager/backend/controller/TaskDataController.java` | 数据查询控制器 | ✅ 已完成 |

### 5.2 前端文件

| 文件路径 | 说明 | 状态 |
|---------|------|------|
| `clients/flutter-web-cli/lib/models/task_record.dart` | 任务记录模型 | ✅ 已完成 |
| `clients/flutter-web-cli/lib/api/task_data_service.dart` | 数据查询服务 | ✅ 已完成 |
| `clients/flutter-web-cli/lib/widgets/common/column_config.dart` | 列配置 | ✅ 已完成 |
| `clients/flutter-web-cli/lib/widgets/common/generic_data_list.dart` | 通用列表组件 | ✅ 已完成 |
| `clients/flutter-web-cli/lib/pages/task/scan_result_page.dart` | 扫描结果页面 | ✅ 已完成 |
| `clients/flutter-web-cli/lib/pages/task/preview_result_page.dart` | 预览结果页面 | ✅ 已完成 |
| `clients/flutter-web-cli/lib/pages/task/execution_result_page.dart` | 执行结果页面 | ✅ 已完成 |

## 6. 技术要点

### 6.1 列配置设计

**核心设计思想**:

1. **统一数据模型**: 使用 `TaskRecord` 作为统一的数据模型，包含所有可能的字段。

2. **列配置驱动**: 不同阶段通过 `ColumnConfig` 控制展示哪些字段。同一个数据模型，通过不同的列配置实现不同的展示效果。

3. **灵活的展示控制**:
   - `visible`: 控制列是否默认显示
   - `hideable`: 控制用户是否可以隐藏此列
   - `sortable`: 控制列是否可排序
   - `filterable`: 控制列是否可筛选
   - `customRender`: 自定义渲染器，实现特殊的展示效果

4. **渐进式展示**:
   - 主要字段：默认显示，不可隐藏（如文件名、操作类型、状态）
   - 次要字段：默认显示，可隐藏（如文件大小、修改时间）
   - 预留字段：默认隐藏，可开启（如元数据、额外参数、错误信息）

### 6.2 不同阶段展示策略

**扫描阶段**:
- 主要展示：文件名、文件路径、文件大小、文件类型、修改时间
- 预留展示：元数据（音频信息）
- 隐藏字段：操作相关字段、状态标记字段、执行相关字段

**预览阶段**:
- 主要展示：原文件名、新文件名、原路径、操作类型、状态、是否变更
- 预留展示：新路径、变更原因、额外参数、分析时间
- 隐藏字段：执行相关字段

**执行阶段**:
- 主要展示：原文件名、目标文件名、操作类型、执行状态、执行时间、耗时
- 预留展示：错误信息、重试次数、原路径、目标路径
- 隐藏字段：扫描阶段的文件信息字段（可选隐藏）

## 7. 后续优化建议

### 7.1 短期优化

1. **数据导出**: 支持导出当前查询结果为 CSV/JSON
2. **列配置持久化**: 保存用户自定义列配置到本地存储
3. **数据详情弹窗**: 点击行查看完整数据详情

### 7.2 中期优化

1. **数据可视化**: 添加图表展示数据统计
2. **批量操作**: 支持批量重试失败记录
3. **实时更新**: WebSocket 推送数据变更

### 7.3 长期优化

1. **数据库存储**: 将数据迁移到 SQLite，提升查询性能
2. **全文搜索**: 集成全文搜索引擎
3. **索引优化**: 实现索引文件机制，提升大数据量查询性能

## 8. 部署说明

### 8.1 构建和部署

使用项目提供的部署脚本进行一键构建和部署：

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/bin/macos
./deploy-all.sh
```

部署脚本会自动完成以下步骤：
1. 编译后端代码（Maven clean package）
2. 编译前端代码（Flutter build web --release）
3. 部署前端代码到 frontend 目录
4. 重启前后端服务

### 8.2 服务地址

- 后端地址: http://localhost:8080
- 前端地址: http://localhost:8081

### 8.3 API 端点

本次迭代新增的 API 端点：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/tasks/{taskId}/scan/records` | GET | 查询扫描记录（分页） |
| `/api/tasks/{taskId}/preview/records` | GET | 查询预览记录（分页） |
| `/api/tasks/{taskId}/execution/records` | GET | 查询执行记录（分页） |
| `/api/tasks/{taskId}/scan/records/{recordId}` | GET | 查询单条扫描记录 |
| `/api/tasks/{taskId}/preview/records/{recordId}` | GET | 查询单条预览记录 |
| `/api/tasks/{taskId}/execution/records/{recordId}` | GET | 查询单条执行记录 |

## 9. 问题与解决方案

### 9.1 列配置 enum 关键字冲突

**问题描述**: Dart 中 `enum` 是关键字，不能用作枚举值名称。

**解决方案**: 将 `ColumnType.enum` 改为 `ColumnType.enumeration`。

**影响文件**:
- `clients/flutter-web-cli/lib/widgets/common/column_config.dart`

### 9.2 API 接口冲突

**问题描述**: `TaskDataController` 中的新接口与 `TaskController` 中的旧接口路径冲突。

**错误信息**:
```
Ambiguous mapping. Cannot map 'taskDataController' method 
com.filemanager.backend.controller.TaskDataController#queryPreviewRecords(...)
to {GET [/api/tasks/{taskId}/preview/records]}: There is already 'taskController' bean method
com.filemanager.backend.controller.TaskController#getPreviewRecords(...) mapped.
```

**解决方案**: 删除 `TaskController` 中的旧接口 `getPreviewRecords`，使用 `TaskDataController` 中的新接口。

**影响文件**:
- `backend/src/main/java/com/filemanager/backend/controller/TaskController.java`

**删除的方法**:
- `getPreviewRecords()` - 旧的预览记录查询接口

**保留的方法**:
- `exportPreviewRecords()` - 导出预览记录接口（不冲突）

### 9.3 任务配置快照中源目录配置丢失

**问题描述**: 用户反馈任务运行时配置快照中的源目录配置会消失。

**根因分析**:
1. 前端创建任务时只发送了简单的参数（taskName, description, autoExecute）
2. 没有将当前配置的源目录信息传递给后端
3. 后端 `createConfigSnapshot` 方法中 `request.getSourceDirectories()` 为 null
4. 导致配置快照中的源目录列表为空

**代码位置**:
- 前端: `main_layout.dart` 中的 `_createTask()` 方法
- 后端: `TaskExecutionService.java` 中的 `createConfigSnapshot()` 方法

**解决方案**: 修改前端创建任务时获取当前源目录配置并传递给后端。

**修改内容**:
```dart
// 修改前
final response = await apiClient.post(
  '/api/tasks',
  body: {
    'taskName': '文件管理任务',
    'description': '通过前端创建的任务',
    'autoExecute': false,
  },
);

// 修改后
final sourceDirectoryService = ref.read(sourceDirectoryServiceProvider);
final sourceDirectories = await sourceDirectoryService.getSourceDirectories();

final response = await apiClient.post(
  '/api/tasks',
  body: {
    'taskName': '文件管理任务',
    'description': '通过前端创建的任务',
    'autoExecute': false,
    'sourceDirectories': sourceDirectories.map((dir) => {
      'path': dir.path,
      'depth': 4,
      'recursive': true,
    }).toList(),
  },
);
```

**影响文件**:
- `clients/flutter-web-cli/lib/widgets/common/main_layout.dart`

## 10. 变更记录

| 日期 | 变更内容 | 负责人 |
|------|----------|--------|
| 2026-03-03 | 创建迭代文档，完成设计方案 | AI Assistant |
| 2026-03-03 | 实现后端分页查询API | AI Assistant |
| 2026-03-03 | 实现前端通用列表组件 | AI Assistant |
| 2026-03-03 | 实现扫描/预览/执行结果页面 | AI Assistant |
| 2026-03-04 | 更新设计文档，添加部署说明 | AI Assistant |
| 2026-03-04 | 修复列配置中 enum 关键字冲突问题 | AI Assistant |
| 2026-03-04 | 修复 TaskController 与 TaskDataController 接口冲突 | AI Assistant |
| 2026-03-04 | 修复任务配置快照中源目录配置丢失问题 | AI Assistant |
| 2026-03-04 | 重新构建和部署服务 | AI Assistant |

---

**备注**: 本次迭代完成了任务数据列表展示组件的核心功能，包括后端分页查询API和前端通用列表组件。通过列配置的方式，实现了不同阶段展示不同字段的需求，解决了通用性和易用性的平衡问题。所有代码已通过测试并成功部署到本地服务。
