# 任务管理系统数据库化迭代方案

## 📋 需求分析

### 核心需求
1. **任务列表完整性**：显示所有任务（正在执行 + 历史任务）
2. **数据持久化**：使用 SQLite 本地数据库存储任务记录和统计信息
3. **操作支持**：支持对历史任务的查询、操作、删除等
4. **技术选型**：使用较新版本的 MyBatis 进行数据库操作
5. **配置管理**：所有配置信息（流水线、策略等）存储到数据库，支持模板化和切换
6. **模糊搜索**：支持对文件名、路径等字段的模糊搜索

### 业务场景
- 用户需要查看所有历史任务的执行情况
- 用户可以对已完成的任务进行重新执行或删除操作
- 系统需要长期保存任务执行记录和统计信息
- 需要支持任务的生命周期管理（创建、执行、完成、删除）
- 用户可以保存多个配置模板，根据需要切换使用
- 用户可以通过文件名或路径模糊搜索变更记录

---

## 🛠️ 技术选型

### 1. 数据库选择
- **SQLite**：轻量级本地数据库，无需额外服务
- **版本**：SQLite 3.x（通过 JDBC 驱动）
- **优势**：
  - 零配置，单文件存储
  - 跨平台支持
  - 适合桌面应用场景
  - 事务支持，数据安全

### 2. ORM 框架
- **MyBatis**：3.5.x 系列（最新稳定版）
- **MyBatis-Spring**：3.0.x（与 Spring Boot 2.7.x 兼容）
- **优势**：
  - SQL 灵活，易于优化
  - 性能优秀，适合复杂查询
  - 易于维护和调试
  - 支持动态 SQL

### 3. 连接池
- **HikariCP**：Spring Boot 默认连接池
- **优势**：
  - 性能优秀
  - 配置简单
  - 与 Spring Boot 深度集成

---

## 📊 数据库表结构设计

### 表1: task_info（任务主表）

```sql
CREATE TABLE task_info (
    task_id VARCHAR(64) PRIMARY KEY COMMENT '任务ID',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    status VARCHAR(32) NOT NULL COMMENT '任务状态: CREATED/SCANNING/SCANNED/PREVIEWING/PREVIEWED/EXECUTING/COMPLETED/FAILED/CANCELLED',
    current_stage VARCHAR(32) COMMENT '当前阶段: CREATED/SCAN/PREVIEW/EXECUTION',
    overall_progress DOUBLE DEFAULT 0.0 COMMENT '总体进度(0-100)',
    message TEXT COMMENT '任务消息',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    completed_at DATETIME COMMENT '完成时间',
    config_snapshot_id VARCHAR(64) COMMENT '配置快照ID',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
);
```

**字段说明**：
- `task_id`：任务唯一标识，格式：`pipeline_{timestamp}_{random}`
- `status`：任务状态枚举值
- `current_stage`：当前执行阶段
- `config_snapshot_id`：关联到配置快照表的外键
- 时间字段使用 `DATETIME` 类型

### 表2: task_stage（任务阶段表）

```sql
CREATE TABLE task_stage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    stage_type VARCHAR(32) NOT NULL COMMENT '阶段类型: SCAN/PREVIEW/EXECUTION',
    status VARCHAR(32) NOT NULL COMMENT '阶段状态: PENDING/RUNNING/COMPLETED/FAILED',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration BIGINT COMMENT '持续时间(毫秒)',
    total_files INTEGER DEFAULT 0 COMMENT '总文件数',
    processed_files INTEGER DEFAULT 0 COMMENT '已处理文件数',
    success_count INTEGER DEFAULT 0 COMMENT '成功数量',
    failed_count INTEGER DEFAULT 0 COMMENT '失败数量',
    changed_files INTEGER DEFAULT 0 COMMENT '变更文件数',
    stats_json TEXT COMMENT '统计信息JSON',
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE,
    INDEX idx_task_stage (task_id, stage_type),
    INDEX idx_task_status (task_id, status)
);
```

**字段说明**：
- `stage_type`：阶段类型（扫描、预览、执行）
- `stats_json`：存储阶段统计信息的 JSON，如文件类型分布、操作统计等
- 时间字段使用 `DATETIME` 类型
- 联合索引：`(task_id, stage_type)` 和 `(task_id, status)`

### 表3: change_record（变更记录表）

```sql
CREATE TABLE change_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    record_id VARCHAR(64) NOT NULL COMMENT '记录ID',
    original_name VARCHAR(512) NOT NULL COMMENT '原始文件名',
    new_name VARCHAR(512) COMMENT '新文件名',
    file_path VARCHAR(1024) NOT NULL COMMENT '文件路径',
    new_path VARCHAR(1024) COMMENT '新文件路径',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型: NONE/RENAME/MOVE/COPY/DELETE/CONVERT/SPLIT/MERGE/ZIP/UNZIP/METADATA/COLLECTION/CLEANUP',
    status VARCHAR(32) NOT NULL COMMENT '执行状态: PENDING/SUCCESS/FAILED/SKIPPED',
    changed BOOLEAN DEFAULT FALSE COMMENT '是否变更',
    selected BOOLEAN DEFAULT FALSE COMMENT '是否选中',
    fail_reason TEXT COMMENT '失败原因',
    extra_params TEXT COMMENT '额外参数JSON',
    analyze_time DATETIME COMMENT '分析时间',
    execute_time DATETIME COMMENT '执行时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE,
    UNIQUE KEY uk_task_record (task_id, record_id),
    INDEX idx_task_status (task_id, status),
    INDEX idx_task_operation (task_id, operation_type),
    INDEX idx_task_changed (task_id, changed),
    INDEX idx_original_name (original_name),
    INDEX idx_file_path (file_path)
);
```

**字段说明**：
- `record_id`：变更记录的唯一标识
- `operation_type`：操作类型枚举值
- `extra_params`：存储操作相关的额外参数
- 时间字段使用 `DATETIME` 类型
- 联合索引：`(task_id, status)`、`(task_id, operation_type)`、`(task_id, changed)`
- 支持模糊搜索的索引：`original_name`、`file_path`

### 表4: task_operation_log（任务操作日志表）

```sql
CREATE TABLE task_operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE/START/PAUSE/RESUME/CANCEL/DELETE/RESTART',
    operation_stage VARCHAR(32) COMMENT '操作阶段',
    operator VARCHAR(64) COMMENT '操作人',
    operation_time DATETIME NOT NULL COMMENT '操作时间',
    operation_detail TEXT COMMENT '操作详情',
    result VARCHAR(32) COMMENT '操作结果: SUCCESS/FAILED',
    error_message TEXT COMMENT '错误信息',
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE,
    INDEX idx_task_operation (task_id, operation_time),
    INDEX idx_task_type (task_id, operation_type)
);
```

**字段说明**：
- `operation_type`：操作类型（创建、启动、暂停、恢复、取消、删除、重启）
- `operation_stage`：操作涉及的阶段
- `operator`：操作人标识
- 时间字段使用 `DATETIME` 类型
- 联合索引：`(task_id, operation_time)`、`(task_id, operation_type)`

### 表5: config_snapshot（配置快照表）

```sql
CREATE TABLE config_snapshot (
    snapshot_id VARCHAR(64) PRIMARY KEY COMMENT '快照ID',
    snapshot_name VARCHAR(255) NOT NULL COMMENT '快照名称',
    snapshot_type VARCHAR(32) NOT NULL COMMENT '快照类型: PIPELINE/STRATEGY/PLUGIN',
    config_data TEXT NOT NULL COMMENT '配置数据JSON',
    description TEXT COMMENT '快照描述',
    is_template BOOLEAN DEFAULT FALSE COMMENT '是否为模板',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    created_by VARCHAR(64) COMMENT '创建人',
    INDEX idx_snapshot_type (snapshot_type),
    INDEX idx_is_template (is_template),
    INDEX idx_created_at (created_at)
);
```

**字段说明**：
- `snapshot_id`：快照唯一标识
- `snapshot_type`：快照类型（流水线、策略、插件）
- `config_data`：存储配置数据的 JSON
- `is_template`：是否为模板，模板可以被复用
- 时间字段使用 `DATETIME` 类型

### 表6: config_template（配置模板表）

```sql
CREATE TABLE config_template (
    template_id VARCHAR(64) PRIMARY KEY COMMENT '模板ID',
    template_name VARCHAR(255) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(32) NOT NULL COMMENT '模板类型: PIPELINE/STRATEGY/PLUGIN',
    snapshot_id VARCHAR(64) NOT NULL COMMENT '关联的快照ID',
    category VARCHAR(128) COMMENT '模板分类',
    tags TEXT COMMENT '标签JSON',
    description TEXT COMMENT '模板描述',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认模板',
    usage_count INTEGER DEFAULT 0 COMMENT '使用次数',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    created_by VARCHAR(64) COMMENT '创建人',
    FOREIGN KEY (snapshot_id) REFERENCES config_snapshot(snapshot_id) ON DELETE CASCADE,
    INDEX idx_template_type (template_type),
    INDEX idx_category (category),
    INDEX idx_is_default (is_default),
    INDEX idx_usage_count (usage_count)
);
```

**字段说明**：
- `template_id`：模板唯一标识
- `template_type`：模板类型
- `snapshot_id`：关联的配置快照
- `category`：模板分类，便于管理和查找
- `tags`：标签，支持多标签分类
- `is_default`：是否为默认模板
- `usage_count`：使用次数统计
- 时间字段使用 `DATETIME` 类型

### 表7: system_config（系统配置表）

```sql
CREATE TABLE system_config (
    config_key VARCHAR(128) PRIMARY KEY COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_type VARCHAR(32) NOT NULL COMMENT '配置类型: STRING/INTEGER/BOOLEAN/JSON',
    description TEXT COMMENT '配置描述',
    category VARCHAR(64) COMMENT '配置分类',
    is_encrypted BOOLEAN DEFAULT FALSE COMMENT '是否加密',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_category (category)
);
```

**字段说明**：
- `config_key`：配置键，唯一标识
- `config_value`：配置值
- `config_type`：配置类型（字符串、整数、布尔、JSON）
- `category`：配置分类
- `is_encrypted`：是否加密存储（如密码等敏感信息）
- 时间字段使用 `DATETIME` 类型

---

## 🏗️ 代码结构设计

### 目录结构
```
backend/src/main/java/com/filemanager/backend/
├── config/
│   ├── DatabaseConfig.java              # 数据库配置
│   └── DatabaseInitializer.java         # 数据库初始化
├── entity/
│   ├── TaskInfoPO.java                  # 任务信息实体
│   ├── TaskStagePO.java                 # 任务阶段实体
│   ├── ChangeRecordPO.java              # 变更记录实体
│   ├── TaskOperationLogPO.java          # 操作日志实体
│   ├── ConfigSnapshotPO.java            # 配置快照实体
│   ├── ConfigTemplatePO.java            # 配置模板实体
│   └── SystemConfigPO.java              # 系统配置实体
├── mapper/
│   ├── TaskInfoMapper.java              # 任务信息Mapper
│   ├── TaskStageMapper.java             # 任务阶段Mapper
│   ├── ChangeRecordMapper.java          # 变更记录Mapper
│   ├── TaskOperationLogMapper.java      # 操作日志Mapper
│   ├── ConfigSnapshotMapper.java        # 配置快照Mapper
│   ├── ConfigTemplateMapper.java        # 配置模板Mapper
│   └── SystemConfigMapper.java          # 系统配置Mapper
├── repository/
│   ├── TaskInfoRepository.java          # 任务信息仓储
│   ├── TaskStageRepository.java         # 任务阶段仓储
│   ├── ChangeRecordRepository.java      # 变更记录仓储
│   ├── TaskOperationLogRepository.java  # 操作日志仓储
│   ├── ConfigSnapshotRepository.java    # 配置快照仓储
│   ├── ConfigTemplateRepository.java    # 配置模板仓储
│   └── SystemConfigRepository.java      # 系统配置仓储
└── service/
    └── impl/
        ├── TaskPersistenceService.java   # 任务持久化服务
        ├── ConfigSnapshotService.java    # 配置快照服务
        ├── ConfigTemplateService.java    # 配置模板服务
        └── SystemConfigService.java      # 系统配置服务
```

### 资源文件结构
```
backend/src/main/resources/
├── mapper/
│   ├── TaskInfoMapper.xml               # 任务信息Mapper XML
│   ├── TaskStageMapper.xml              # 任务阶段Mapper XML
│   ├── ChangeRecordMapper.xml           # 变更记录Mapper XML
│   ├── TaskOperationLogMapper.xml       # 操作日志Mapper XML
│   ├── ConfigSnapshotMapper.xml         # 配置快照Mapper XML
│   ├── ConfigTemplateMapper.xml         # 配置模板Mapper XML
│   ├── SystemConfigMapper.xml           # 系统配置Mapper XML
├── db/
│   └── schema.sql                       # 数据库表结构脚本
└── application.yml                      # 应用配置
```

---

## 📝 实体类设计

### TaskInfoPO.java
```java
package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TaskInfoPO {
    private String taskId;
    private String taskName;
    private String status;
    private String currentStage;
    private Double overallProgress;
    private String message;
    private Date createdAt;
    private Date updatedAt;
    private Date completedAt;
    private String configSnapshotId;
}
```

### TaskStagePO.java
```java
package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TaskStagePO {
    private Long id;
    private String taskId;
    private String stageType;
    private String status;
    private Date startTime;
    private Date endTime;
    private Long duration;
    private Integer totalFiles;
    private Integer processedFiles;
    private Integer successCount;
    private Integer failedCount;
    private Integer changedFiles;
    private String statsJson;
}
```

### ChangeRecordPO.java
```java
package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ChangeRecordPO {
    private Long id;
    private String taskId;
    private String recordId;
    private String originalName;
    private String newName;
    private String filePath;
    private String newPath;
    private String operationType;
    private String status;
    private Boolean changed;
    private Boolean selected;
    private String failReason;
    private String extraParams;
    private Date analyzeTime;
    private Date executeTime;
    private Date createdAt;
}
```

### TaskOperationLogPO.java
```java
package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TaskOperationLogPO {
    private Long id;
    private String taskId;
    private String operationType;
    private String operationStage;
    private String operator;
    private Date operationTime;
    private String operationDetail;
    private String result;
    private String errorMessage;
}
```

### ConfigSnapshotPO.java
```java
package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ConfigSnapshotPO {
    private String snapshotId;
    private String snapshotName;
    private String snapshotType;
    private String configData;
    private String description;
    private Boolean isTemplate;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
}
```

### ConfigTemplatePO.java
```java
package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ConfigTemplatePO {
    private String templateId;
    private String templateName;
    private String templateType;
    private String snapshotId;
    private String category;
    private String tags;
    private String description;
    private Boolean isDefault;
    private Integer usageCount;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
}
```

### SystemConfigPO.java
```java
package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class SystemConfigPO {
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private String category;
    private Boolean isEncrypted;
    private Date createdAt;
    private Date updatedAt;
}
```

---

## 🔄 迭代计划

### 阶段一：基础设施搭建（1-2天）

#### 任务清单
1. ✅ 添加 Maven 依赖（MyBatis、SQLite、HikariCP）
2. ✅ 配置数据库连接和 MyBatis
3. ✅ 创建数据库表结构
4. ✅ 实现基础 PO 实体类
5. ✅ 实现数据库自动初始化逻辑

#### 详细步骤
1. 在 `pom.xml` 中添加依赖
2. 创建 `DatabaseConfig.java` 配置类，支持自定义数据库路径
3. 创建 `DatabaseInitializer.java` 初始化类，自动创建数据库和表
4. 创建数据库初始化脚本 `schema.sql`
5. 实现所有 PO 实体类

### 阶段二：Mapper 层开发（1-2天）

#### 任务清单
1. ✅ 实现 TaskInfoMapper（CRUD + 查询）
2. ✅ 实现 TaskStageMapper（CRUD + 查询）
3. ✅ 实现 ChangeRecordMapper（CRUD + 模糊搜索）
4. ✅ 实现 TaskOperationLogMapper（CRUD + 查询）
5. ✅ 实现 ConfigSnapshotMapper（CRUD + 查询）
6. ✅ 实现 ConfigTemplateMapper（CRUD + 查询）
7. ✅ 实现 SystemConfigMapper（CRUD + 查询）

#### 详细步骤
1. 创建 Mapper 接口
2. 创建 Mapper XML 文件
3. 实现基础 CRUD 方法
4. 实现复杂查询方法（分页、筛选、排序）
5. 实现模糊搜索功能（支持多字段）
6. 使用动态 SQL 实现灵活查询

### 阶段三：Repository 层开发（1天）

#### 任务清单
1. ✅ 实现 TaskInfoRepository（业务逻辑封装）
2. ✅ 实现 TaskStageRepository（业务逻辑封装）
3. ✅ 实现 ChangeRecordRepository（业务逻辑封装）
4. ✅ 实现 TaskOperationLogRepository（业务逻辑封装）
5. ✅ 实现 ConfigSnapshotRepository（业务逻辑封装）
6. ✅ 实现 ConfigTemplateRepository（业务逻辑封装）
7. ✅ 实现 SystemConfigRepository（业务逻辑封装）

#### 详细步骤
1. 创建 Repository 接口
2. 实现业务逻辑方法
3. 添加事务管理
4. 实现数据转换（PO ↔ Entity）

### 阶段四：Service 层开发（2-3天）

#### 任务清单
1. ✅ 实现 TaskPersistenceService（任务持久化服务）
2. ✅ 实现 ConfigSnapshotService（配置快照服务）
3. ✅ 实现 ConfigTemplateService（配置模板服务）
4. ✅ 实现 SystemConfigService（系统配置服务）
5. ✅ 集成现有 TaskRegistry 与数据库
6. ✅ 实现任务生命周期管理
7. ✅ 实现历史任务查询功能
8. ✅ 实现配置快照的保存和恢复

#### 详细步骤
1. 创建各个 Service 类
2. 实现任务创建、更新、删除方法
3. 实现任务状态同步机制
4. 实现历史任务查询接口
5. 实现配置快照的保存、加载、恢复
6. 实现配置模板的管理
7. 实现系统配置的管理

### 阶段五：Controller 层集成（1-2天）

#### 任务清单
1. ✅ 扩展 TaskController 支持历史任务查询
2. ✅ 添加任务操作接口（删除、重启等）
3. ✅ 实现任务统计接口
4. ✅ 实现分页查询功能
5. ✅ 添加配置快照管理接口
6. ✅ 添加配置模板管理接口
7. ✅ 添加系统配置管理接口

#### 详细步骤
1. 扩展现有 TaskController
2. 创建 ConfigSnapshotController
3. 创建 ConfigTemplateController
4. 创建 SystemConfigController
5. 添加新的 API 接口
6. 实现请求参数验证
7. 实现响应数据格式化

### 阶段六：前端适配（2-3天）

#### 任务清单
1. ✅ 修改任务列表页面支持历史任务
2. ✅ 添加任务操作按钮（删除、重启）
3. ✅ 实现任务筛选和排序功能
4. ✅ 优化任务详情页面
5. ✅ 添加配置快照管理界面
6. ✅ 添加配置模板管理界面
7. ✅ 实现配置切换功能
8. ✅ 实现模糊搜索功能

#### 详细步骤
1. 修改任务列表 API 调用
2. 添加操作按钮 UI
3. 实现筛选和排序逻辑
4. 创建配置快照管理页面
5. 创建配置模板管理页面
6. 实现配置保存、加载、切换
7. 实现多字段模糊搜索
8. 优化用户体验

### 阶段七：测试和优化（1-2天）

#### 任务清单
1. ✅ 单元测试
2. ✅ 集成测试
3. ✅ 性能优化
4. ✅ 文档更新

#### 详细步骤
1. 编写单元测试用例
2. 编写集成测试用例
3. 性能测试和优化
4. 更新 API 文档

---

## 🎨 关键功能设计

### 1. 任务查询功能

#### 功能描述
支持多维度查询任务列表，包括状态筛选、时间排序、关键字搜索等。

#### API 设计
```
GET /api/tasks/list
参数：
- page: 页码（默认1）
- size: 每页大小（默认20）
- status: 状态筛选（可选）
- startDate: 开始日期（可选）
- endDate: 结束日期（可选）
- keyword: 关键字（可选）
- sortBy: 排序字段（created_at/updated_at）
- sortOrder: 排序方向（ASC/DESC）

响应：
{
  "success": true,
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

#### 实现要点
- 使用 MyBatis 动态 SQL 实现灵活查询
- 建立合适的联合索引提高查询性能
- 实现分页查询避免数据量过大

### 2. 变更记录模糊搜索功能

#### 功能描述
支持对变更记录的多个字段进行模糊搜索，包括原始文件名、新文件名、文件路径等。

#### API 设计
```
GET /api/tasks/{taskId}/changes
参数：
- page: 页码（默认1）
- size: 每页大小（默认20）
- status: 状态筛选（可选）
- operationType: 操作类型筛选（可选）
- changed: 是否变更（可选）
- searchFields: 搜索字段（可选，多个字段用逗号分隔，如：originalName,newName,filePath）
- keyword: 搜索关键字（可选）
- sortBy: 排序字段（created_at/analyze_time/execute_time）
- sortOrder: 排序方向（ASC/DESC）

响应：
{
  "success": true,
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

#### 实现要点
- 使用 MyBatis 动态 SQL 实现多字段模糊搜索
- 支持同时搜索多个字段
- 建立合适的索引（original_name、file_path）
- 使用 LIKE 语句实现模糊匹配

### 3. 任务操作功能

#### 功能描述
支持对任务进行删除、重启等操作。

#### API 设计
```
DELETE /api/tasks/{taskId}
删除任务（包括数据库记录和文件目录）

POST /api/tasks/{taskId}/restart
重启任务（从指定阶段重新开始）
参数：
- stage: 重启阶段（SCAN/PREVIEW/EXECUTION）

POST /api/tasks/{taskId}/cancel
取消正在执行的任务
```

#### 实现要点
- 删除操作需要同时清理数据库记录和文件目录
- 重启操作需要创建新的任务记录
- 取消操作需要更新任务状态并记录日志

### 4. 配置快照管理功能

#### 功能描述
支持保存、加载、删除配置快照，以及从快照恢复配置。

#### API 设计
```
POST /api/config/snapshots
保存配置快照
参数：
{
  "snapshotName": "快照名称",
  "snapshotType": "PIPELINE",
  "configData": {...},
  "description": "快照描述",
  "isTemplate": false
}

GET /api/config/snapshots
查询配置快照列表
参数：
- snapshotType: 快照类型（可选）
- isTemplate: 是否为模板（可选）

GET /api/config/snapshots/{snapshotId}
查询配置快照详情

PUT /api/config/snapshots/{snapshotId}
更新配置快照

DELETE /api/config/snapshots/{snapshotId}
删除配置快照

POST /api/config/snapshots/{snapshotId}/restore
从快照恢复配置
```

#### 实现要点
- 配置快照支持 JSON 格式存储
- 支持快照版本管理
- 支持快照的导入导出

### 5. 配置模板管理功能

#### 功能描述
支持创建、管理、使用配置模板。

#### API 设计
```
POST /api/config/templates
创建配置模板
参数：
{
  "templateName": "模板名称",
  "templateType": "PIPELINE",
  "snapshotId": "快照ID",
  "category": "模板分类",
  "tags": ["标签1", "标签2"],
  "description": "模板描述",
  "isDefault": false
}

GET /api/config/templates
查询配置模板列表
参数：
- templateType: 模板类型（可选）
- category: 模板分类（可选）
- isDefault: 是否为默认模板（可选）

GET /api/config/templates/{templateId}
查询配置模板详情

PUT /api/config/templates/{templateId}
更新配置模板

DELETE /api/config/templates/{templateId}
删除配置模板

POST /api/config/templates/{templateId}/use
使用配置模板（创建新任务）
```

#### 实现要点
- 模板关联到配置快照
- 支持模板分类和标签
- 支持设置默认模板
- 记录模板使用次数

### 6. 数据同步机制

#### 功能描述
实现 TaskRegistry 与数据库的实时同步。

#### 同步策略
1. **任务创建**：创建任务时同时写入数据库
2. **状态更新**：任务状态变更时更新数据库
3. **阶段完成**：每个阶段完成时更新阶段信息
4. **变更记录**：变更记录生成时批量写入数据库
5. **配置保存**：配置变更时保存快照到数据库

#### 实现要点
- 使用异步方式写入数据库，避免影响主流程
- 实现失败重试机制
- 添加数据一致性检查
- 实现事务管理保证数据一致性

---

## 🔧 Maven 依赖配置

### pom.xml 添加依赖

```xml
<!-- MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- SQLite JDBC -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.44.1.0</version>
</dependency>

<!-- HikariCP (Spring Boot默认包含) -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### application.yml 配置

```yaml
spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    url: jdbc:sqlite:${app.database.path:data/music_manager.db}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.filemanager.backend.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

app:
  database:
    path: data/music_manager.db  # 数据库文件路径，支持自定义
    auto-init: true              # 是否自动初始化数据库
    backup-enabled: true         # 是否启用自动备份
    backup-path: data/backup     # 备份路径
```

---

## 📈 性能优化考虑

### 1. 索引优化
- **联合索引优先**：所有查询都基于 task_id，建立联合索引 `(task_id, status)`、`(task_id, operation_type)` 等
- **模糊搜索索引**：为需要模糊搜索的字段建立索引（original_name、file_path）
- **避免单字段索引**：status、operation_type 等单字段索引不建立，因为查询总是基于 task_id
- **时间字段索引**：为时间排序字段建立索引（created_at、updated_at）

### 2. 分页查询
- 避免一次性加载大量数据
- 使用 LIMIT 和 OFFSET 实现分页
- 考虑使用游标分页提高性能

### 3. 连接池配置
- 合理配置连接池参数
- 根据实际负载调整连接池大小
- 监控连接池使用情况

### 4. 批量操作
- 使用批量插入提高写入性能
- 使用批量更新减少数据库交互
- 考虑使用事务保证数据一致性

### 5. 缓存机制
- 对热点数据进行缓存
- 使用 Redis 或本地缓存
- 设置合理的缓存过期时间

### 6. 查询优化
- 使用动态 SQL 实现灵活查询
- 避免全表扫描
- 合理使用索引

---

## 🎯 预期效果

### 功能效果
1. **完整的任务管理**：所有任务都能在任务列表中显示
2. **高效的数据查询**：支持快速查询和筛选历史任务
3. **可靠的数据持久化**：任务数据不会丢失
4. **灵活的操作支持**：支持对历史任务的各种操作
5. **强大的配置管理**：支持配置快照、模板、切换等功能
6. **便捷的模糊搜索**：支持多字段模糊搜索变更记录
7. **良好的扩展性**：便于后续功能扩展和数据库迁移

### 性能效果
1. **查询性能**：支持万级任务数据快速查询
2. **写入性能**：支持高并发任务状态更新
3. **存储效率**：合理的数据结构设计，减少存储空间
4. **响应速度**：API 响应时间 < 100ms
5. **模糊搜索**：支持快速的多字段模糊搜索

### 用户体验
1. **直观的任务列表**：清晰展示所有任务状态
2. **便捷的操作方式**：简单的操作界面
3. **丰富的筛选功能**：支持多维度筛选
4. **详细的信息展示**：完整的任务详情
5. **灵活的配置管理**：支持配置保存、切换、模板化
6. **强大的搜索能力**：支持多字段模糊搜索

---

## 📚 相关文档

- [任务管理系统 API 文档](./task_management_api.md)
- [任务管理架构设计](./task_management_architecture.md)
- [任务管理开发指南](./task_management_development_guide.md)
- [任务管理测试报告](./task_management_test_report.md)

---

## � 前后端集成设计

### 前端架构设计

#### API 服务层
```
lib/api/
├── api_client.dart                    # HTTP 客户端基础类
├── task_service.dart                  # 原有任务服务
├── database_task_service.dart          # 数据库任务服务（新增）
├── database_config_service.dart        # 数据库配置服务（新增）
└── providers.dart                    # 依赖注入配置
```

#### 页面设计
```
lib/pages/
├── task_list_page.dart               # 原有任务列表页面
├── database_task_list_page.dart      # 数据库任务列表页面（新增）
├── task_detail_page.dart             # 任务详情页面
├── config_page.dart                 # 配置管理页面
└── config_snapshot_page.dart         # 配置快照页面（新增）
```

### 前端 API 设计

#### DatabaseTaskService 接口
```dart
class DatabaseTaskService {
  // 获取任务列表（支持分页、状态筛选、关键词搜索）
  Future<Map<String, dynamic>> getTasks({
    int page = 1,
    int size = 20,
    String? status,
    String? keyword,
  });

  // 获取任务详情
  Future<Map<String, dynamic>> getTask(String taskId);

  // 获取任务阶段
  Future<Map<String, dynamic>> getTaskStages(String taskId);

  // 获取变更记录（支持分页、状态筛选、操作类型筛选、关键词搜索）
  Future<Map<String, dynamic>> getTaskChanges(
    String taskId, {
    int page = 1,
    int size = 20,
    String? status,
    String? operationType,
    String? keyword,
  });

  // 获取操作日志
  Future<Map<String, dynamic>> getTaskLogs(
    String taskId, {
    int page = 1,
    int size = 20,
  });

  // 删除任务
  Future<Map<String, dynamic>> deleteTask(String taskId);

  // 获取统计信息
  Future<Map<String, dynamic>> getStatistics();
}
```

#### DatabaseConfigService 接口
```dart
class DatabaseConfigService {
  // 配置快照管理
  Future<Map<String, dynamic>> getSnapshots({
    int page = 1,
    int size = 20,
    String? type,
  });
  Future<Map<String, dynamic>> getSnapshot(String snapshotId);
  Future<Map<String, dynamic>> createSnapshot(Map<String, dynamic> snapshotData);
  Future<Map<String, dynamic>> updateSnapshot(String snapshotId, Map<String, dynamic> snapshotData);
  Future<Map<String, dynamic>> deleteSnapshot(String snapshotId);

  // 配置模板管理
  Future<Map<String, dynamic>> getTemplates({
    int page = 1,
    int size = 20,
    String? category,
  });
  Future<Map<String, dynamic>> getTemplate(String templateId);
  Future<Map<String, dynamic>> createTemplate(Map<String, dynamic> templateData);
  Future<Map<String, dynamic>> updateTemplate(String templateId, Map<String, dynamic> templateData);
  Future<Map<String, dynamic>> deleteTemplate(String templateId);

  // 系统配置管理
  Future<Map<String, dynamic>> getSystemConfigs({
    int page = 1,
    int size = 20,
    String? category,
  });
  Future<Map<String, dynamic>> getSystemConfig(String configKey);
  Future<Map<String, dynamic>> createSystemConfig(Map<String, dynamic> configData);
  Future<Map<String, dynamic>> updateSystemConfig(String configKey, Map<String, dynamic> configData);
  Future<Map<String, dynamic>> deleteSystemConfig(String configKey);
}
```

### 前端页面设计

#### DatabaseTaskListPage 功能特性
1. **统计信息卡片**
   - 总任务数
   - 总变更数
   - 总日志数

2. **搜索功能**
   - 支持关键词搜索
   - 实时搜索或回车搜索

3. **状态筛选**
   - 全部
   - 已创建 (CREATED)
   - 正在扫描 (SCANNING)
   - 扫描完成 (SCANNED)
   - 正在预览 (PREVIEWING)
   - 预览完成 (PREVIEWED)
   - 正在执行 (EXECUTING)
   - 执行完成 (COMPLETED)
   - 执行失败 (FAILED)
   - 已取消 (CANCELLED)

4. **任务列表**
   - 任务名称
   - 任务ID
   - 任务状态
   - 任务消息
   - 操作按钮（详情、删除）

5. **分页功能**
   - 上一页/下一页
   - 页码显示
   - 总页数显示

6. **任务详情对话框**
   - 完整的任务信息展示
   - 创建时间、更新时间
   - 当前阶段、进度
   - 关联的配置快照

### 依赖注入配置
```dart
// 创建 DatabaseTaskService 实例的 provider
final databaseTaskServiceProvider = Provider<DatabaseTaskService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DatabaseTaskService(apiClient);
});

// 创建 DatabaseConfigService 实例的 provider
final databaseConfigServiceProvider = Provider<DatabaseConfigService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DatabaseConfigService(apiClient);
});
```

---

## 🧪 测试设计

### 单元测试

#### 后端单元测试
```
backend/src/test/java/com/filemanager/backend/
├── service/
│   ├── TaskInfoServiceTest.java
│   ├── TaskStageServiceTest.java
│   ├── ChangeRecordServiceTest.java
│   ├── TaskOperationLogServiceTest.java
│   ├── ConfigSnapshotServiceTest.java
│   ├── ConfigTemplateServiceTest.java
│   └── SystemConfigServiceTest.java
├── mapper/
│   ├── TaskInfoMapperTest.java
│   ├── TaskStageMapperTest.java
│   ├── ChangeRecordMapperTest.java
│   ├── TaskOperationLogMapperTest.java
│   ├── ConfigSnapshotMapperTest.java
│   ├── ConfigTemplateMapperTest.java
│   └── SystemConfigMapperTest.java
└── repository/
    ├── TaskInfoRepositoryTest.java
    ├── TaskStageRepositoryTest.java
    ├── ChangeRecordRepositoryTest.java
    ├── TaskOperationLogRepositoryTest.java
    ├── ConfigSnapshotRepositoryTest.java
    ├── ConfigTemplateRepositoryTest.java
    └── SystemConfigRepositoryTest.java
```

#### 前端单元测试
```
clients/flutter-web-cli/test/
├── api/
│   ├── database_task_service_test.dart
│   └── database_config_service_test.dart
└── pages/
    └── database_task_list_page_test.dart
```

### 集成测试

#### 后端集成测试
```
backend/src/test/java/com/filemanager/backend/integration/
├── DatabaseIntegrationTest.java
├── TaskRegistryIntegrationTest.java
├── DatabaseTaskControllerIntegrationTest.java
└── DatabaseConfigControllerIntegrationTest.java
```

### 端到端测试

#### 测试场景设计

1. **任务生命周期测试**
   - 创建任务 → 验证数据库记录
   - 扫描阶段 → 验证阶段记录
   - 预览阶段 → 验证变更记录
   - 执行阶段 → 验证执行结果
   - 任务完成 → 验证最终状态

2. **任务查询测试**
   - 分页查询
   - 状态筛选
   - 关键词搜索
   - 时间范围筛选

3. **变更记录测试**
   - 批量插入
   - 模糊搜索
   - 状态筛选
   - 操作类型筛选

4. **配置管理测试**
   - 保存配置快照
   - 加载配置快照
   - 创建配置模板
   - 使用配置模板
   - 删除配置

5. **数据一致性测试**
   - TaskRegistry 与数据库同步
   - 并发操作数据一致性
   - 事务回滚验证

---

## �🔄 版本历史

- **v2.1** (2026-02-15): 完成前后端集成和测试
  - 添加前端 API 服务层设计
  - 添加前端页面设计
  - 添加依赖注入配置
  - 添加单元测试设计
  - 添加集成测试设计
  - 添加端到端测试设计
- **v2.0** (2026-02-15): 根据反馈优化方案
  - 时间字段改用 DATETIME 类型
  - 优化索引设计，使用联合索引
  - 增加配置快照和模板管理
  - 增加模糊搜索功能
  - 支持数据库路径自定义
  - 支持数据库自动初始化
- **v1.0** (2026-02-15): 初始版本，完成数据库化方案设计
