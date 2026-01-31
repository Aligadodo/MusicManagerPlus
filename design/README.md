# FileManager Plus 设计文档目录说明

## 目录结构

```
design/
├── README.md                      # 本文件，设计文档目录说明
├── current-architecture.md          # 当前系统架构设计文档（新）
├── current-features.md             # 当前系统功能设计文档（新）
├── strategy-parameters-config.md    # 策略参数配置文档（新）
├── old-framework/                 # 老框架文档目录（已归档）
│   ├── doc/                     # 老框架的详细设计文档
│   ├── command/                  # 老框架的命令文档
│   ├── problem/                  # 老框架的问题文档
│   ├── review/                   # 老框架的审查文档
│   └── skill/                   # 老框架的技能文档
└── new-framework/                 # 新框架文档目录（当前使用）
    ├── docV2/                   # 新框架的详细设计文档
    ├── tech-migration/            # 技术迁移文档
    └── migration-plan.md         # 迁移计划文档
```

## 文档说明

### 当前框架文档（new-framework/）

#### 1. 架构设计
- **current-architecture.md**: 当前系统的整体架构设计，包括技术栈、项目结构、核心系统架构等

#### 2. 功能设计
- **current-features.md**: 当前系统的功能设计，包括所有核心功能模块的详细说明

#### 3. 策略参数配置
- **strategy-parameters-config.md**: 所有策略的参数配置详细说明

#### 4. API设计
- **docV2/api-endpoints.md**: RESTful API端点详细说明

#### 5. 插件系统
- **docV2/plugin-architecture.md**: 插件系统架构设计
- **docV2/plugin-implementation.md**: 插件实现指南
- **docV2/plugin-system.md**: 插件系统概述

#### 6. 服务接口
- **docV2/service-interfaces.md**: 服务接口设计说明

#### 7. 测试指南
- **docV2/testing-guide.md**: 测试指南

#### 8. 技术迁移
- **tech-migration/backend-api-design.md**: 后端API设计
- **tech-migration/client-parallel-design.md**: 客户端并行设计
- **tech-migration/complete-migration-plan.md**: 完整迁移计划
- **tech-migration/flutter-web-architecture.md**: Flutter Web架构
- **tech-migration/implementation-checklist.md**: 实现检查清单
- **tech-migration/javaFx-to-web-migration-report.md**: JavaFX到Web迁移报告
- **tech-migration/plugin-system-design.md**: 插件系统设计
- **tech-migration/readme.md**: 技术迁移说明

#### 9. 迁移计划
- **docV2/migration-architecture.md**: 迁移架构设计
- **docV2/migration-plan.md**: 迁移计划

### 老框架文档（old-framework/）

#### 1. 策略设计文档
- **doc/AbstractFfmpegStrategy.md**: FFmpeg策略抽象设计
- **doc/AdvancedRenameStrategy.md**: 高级重命名策略设计
- **doc/AlbumDirNormalizeStrategy.md**: 专辑目录标准化策略设计
- **doc/AudioConverterStrategy.md**: 音频转换策略设计
- **doc/CueSplitterStrategy.md**: Cue分割策略设计
- **doc/FileCleanupStrategy.md**: 文件清理策略设计
- **doc/FileCollectionStrategy.md**: 文件收集策略设计
- **doc/FileMigrateStrategy.md**: 文件迁移策略设计
- **doc/MetadataScraperStrategy.md**: 元数据抓取策略设计
- **doc/NcmBaseStrategy.md**: NCM基础策略设计
- **doc/NcmConvertStrategy.md**: NCM转换策略设计
- **doc/TrackNumberStrategy.md**: 音轨号策略设计
- **doc/iappstrategy-interface-design.md**: IAppStrategy接口设计
- **doc/strategy-overview.md**: 策略概述

#### 2. UI设计文档
- **doc/ui-appearance-manager.md**: 外观管理器设计
- **doc/ui-compose-view.md**: 组合视图设计
- **doc/ui-global-settings-view.md**: 全局设置视图设计
- **doc/ui-log-view.md**: 日志视图设计
- **doc/ui-overview.md**: UI概述
- **doc/ui-preview-view.md**: 预览视图设计
- **doc/ui-style-system.md**: 样式系统设计

#### 3. 命令文档
- **command/windows-commands.md**: Windows命令文档

#### 4. 问题文档
- **problem/design-problems.md**: 设计问题文档
- **problem/implementation-problems.md**: 实现问题文档

#### 5. 审查文档
- **review/collection-naming-strategy-iteration-review.md**: 集合命名策略迭代审查
- **review/metadata-extraction-iteration-review.md**: 元数据提取迭代审查
- **review/strategy-refactoring-iteration-review.md**: 策略重构迭代审查

#### 6. 技能文档
- **skill/iteration-documentation-maintenance-guide.md**: 迭代文档维护指南
- **skill/metadata-extraction-optimization-skill.md**: 元数据提取优化技能
- **skill/readme-collection-develop.md**: 集合开发技能
- **skill/strategy-extension-skill.md**: 策略扩展技能

## 文档使用指南

### 新用户
1. 首先阅读 **current-architecture.md** 了解系统整体架构
2. 然后阅读 **current-features.md** 了解系统功能
3. 根据需要阅读 **new-framework/** 目录下的详细文档

### 开发者
1. 阅读 **current-architecture.md** 了解系统架构
2. 阅读 **new-framework/docV2/** 目录下的详细设计文档
3. 参考 **new-framework/tech-migration/** 目录了解技术迁移细节

### 维护者
1. 参考 **old-framework/** 目录了解历史设计
2. 使用 **new-framework/** 目录进行新功能开发
3. 更新 **current-architecture.md** 和 **current-features.md** 保持文档同步

## 文档维护

### 更新原则
1. **当前框架文档** 应该始终保持最新状态
2. **老框架文档** 作为历史参考，不再更新
3. 新功能开发应该更新 **new-framework/** 目录下的文档
4. 重大架构变更应该更新 **current-architecture.md**
5. 新功能添加应该更新 **current-features.md**

### 文档版本控制
- 所有文档都应该使用Git进行版本控制
- 重大变更应该在文档中注明变更日期和变更内容
- 过时的文档应该移动到 **old-framework/** 目录

## 总结

本目录包含了FileManager Plus项目的所有设计文档，分为当前框架文档和老框架文档。新用户和开发者应该优先阅读当前框架文档，维护者可以参考老框架文档了解历史设计。