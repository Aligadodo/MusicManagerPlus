# 前端目录结构重构文档

## 重构概述

本次重构旨在改善前端代码的组织结构，使代码更加清晰、易于维护和理解。通过将相关功能的文件分组到子目录中，减少了平铺式文件结构带来的混乱。

## 重构前的问题

1. **平铺式结构**：所有页面和组件文件都放在同一个目录下，导致文件过多
2. **依赖关系不清晰**：难以快速定位相关文件
3. **维护困难**：添加新功能时容易造成文件混乱
4. **可读性差**：代码审查和理解架构需要花费更多时间

## 当前目录结构

```
lib/
├── api/                          # API服务层
│   ├── api_client.dart
│   ├── config_service.dart
│   ├── enum_service.dart
│   ├── file_service.dart
│   ├── log_service.dart
│   ├── pipeline_service.dart
│   ├── plugin_service.dart
│   ├── providers.dart
│   ├── source_directory_service.dart
│   ├── strategy_service.dart
│   ├── task_service.dart
│   └── thread_pool_service.dart
│
├── models/                       # 数据模型层
│   ├── auto_fill_config.dart
│   ├── change_record.dart
│   ├── condition_type.dart
│   ├── config_field.dart
│   ├── enum_option.dart
│   ├── file_info.dart
│   ├── local_task_state.dart
│   ├── plugin_config.dart
│   ├── plugin_info.dart
│   ├── plugin_parameter.dart
│   ├── precondition.dart
│   ├── precondition_field_config.dart
│   ├── precondition_field_configs.dart
│   ├── precondition_group.dart
│   ├── rename_action.dart
│   ├── rename_condition.dart
│   ├── rename_rule.dart
│   ├── rule_condition.dart
│   ├── rule_condition_group.dart
│   ├── source_directory.dart
│   ├── strategy_config.dart
│   ├── strategy_info.dart
│   ├── task_request.dart
│   ├── task_state.dart
│   └── task_status.dart
│
├── pages/                        # 页面层（按功能模块分组）
│   ├── compose/                  # 组合页面
│   │   └── compose_page.dart
│   ├── preview/                  # 预览页面
│   │   └── preview_page.dart
│   ├── settings/                 # 设置页面
│   │   ├── appearance_page.dart
│   │   └── global_settings_page.dart
│   └── strategy/                 # 策略配置页面
│       ├── strategy_config_page.dart
│       ├── strategy_config_panel.dart
│       └── strategy_list_panel.dart
│
├── providers/                    # 状态管理
│   ├── config_provider.dart
│   └── theme_provider.dart
│
├── utils/                        # 工具类
│   ├── file_utils.dart
│   ├── task_estimator.dart
│   ├── theme_utils.dart
│   ├── tooltip_utils.dart
│   └── ui_utils.dart
│
├── widgets/                      # 组件层（按功能模块分组）
│   ├── appearance/               # 外观相关组件
│   │   ├── appearance_preset_widgets.dart
│   │   └── appearance_settings_fields.dart
│   ├── common/                   # 通用组件
│   │   ├── action_builder.dart
│   │   ├── change_record_table.dart
│   │   ├── condition_builder.dart
│   │   ├── data_loading_operations.dart
│   │   ├── directory_list_item.dart
│   │   ├── filter_rules.dart
│   │   ├── log_content_widget.dart
│   │   ├── log_file_list_widget.dart
│   │   ├── main_layout.dart
│   │   ├── preset_manager.dart
│   │   ├── preset_operations.dart
│   │   ├── rename_rule_card.dart
│   │   ├── rename_rule_editor.dart
│   │   ├── selectable_text_widget.dart
│   │   ├── stage_result_cards.dart
│   │   └── strategy_config_card.dart
│   ├── compose/                  # 组合相关组件
│   │   ├── compose_config_panel.dart
│   │   ├── compose_directory_panel.dart
│   │   ├── compose_global_config_panel.dart
│   │   ├── compose_parameter_panel.dart
│   │   ├── compose_pipeline_panel.dart
│   │   └── compose_precondition_panel.dart
│   ├── config/                   # 配置相关组件
│   │   ├── config_field_builder.dart
│   │   ├── config_snapshot_card.dart
│   │   ├── file_type_selector.dart
│   │   ├── file_type_tree.dart
│   │   ├── parameter_field.dart
│   │   ├── pipeline_list_item.dart
│   │   ├── plugin_parameter_fields.dart
│   │   ├── precondition_builder.dart
│   │   ├── precondition_config_panel.dart
│   │   ├── precondition_group_item.dart
│   │   └── precondition_item.dart
│   ├── preview/                  # 预览相关组件
│   │   ├── preview_widget.dart
│   │   ├── task_detail_header.dart
│   │   ├── task_detail_widget.dart
│   │   ├── task_list_widget.dart
│   │   └── task_operations.dart
│   ├── settings/                 # 设置相关组件
│   │   ├── run_settings.dart
│   │   ├── scan_settings.dart
│   │   └── thread_pool_settings.dart
│   └── task/                     # 任务相关组件
│       ├── task_execution_cards.dart
│       ├── task_result_cards.dart
│       └── task_status_helpers.dart
│
└── main.dart                     # 应用入口
```

## 目录结构说明

### 1. API服务层 (`api/`)
- 包含所有与后端API交互的服务类
- 每个服务类负责特定的API端点
- 使用统一的ApiClient进行HTTP请求

### 2. 数据模型层 (`models/`)
- 包含所有数据模型的定义
- 使用JSON序列化/反序列化
- 提供类型安全的数据访问

### 3. 页面层 (`pages/`)
按功能模块分组：
- **compose/**: 组合配置页面
- **preview/**: 预览和任务管理页面（集成了日志查看功能）
- **settings/**: 全局设置和外观设置页面
- **strategy/**: 策略配置页面

### 4. 状态管理层 (`providers/`)
- 使用Riverpod进行状态管理
- 提供全局配置和主题状态

### 5. 工具类层 (`utils/`)
- 提供通用的工具函数
- 包括文件操作、主题工具、UI工具等

### 6. 组件层 (`widgets/`)
按功能模块分组：
- **appearance/**: 外观主题相关的UI组件
- **common/**: 通用的可重用UI组件
- **compose/**: 组合配置相关的UI组件
- **config/**: 配置表单和参数相关的UI组件
- **preview/**: 预览和任务相关的UI组件
- **settings/**: 设置相关的UI组件
- **task/**: 任务状态和结果显示组件

## 重构优势

1. **清晰的层次结构**：相关功能的文件被组织在一起，易于查找
2. **降低耦合度**：通过合理的目录划分，减少了不必要的依赖
3. **提高可维护性**：新功能的添加和现有功能的修改更加清晰
4. **增强可读性**：代码审查和理解架构更加直观
5. **便于团队协作**：团队成员可以快速定位相关文件

## Import路径规范

### 同级目录引用
```dart
import 'some_file.dart';
```

### 跨目录引用
```dart
import '../../api/api_client.dart';
import '../models/task_status.dart';
import 'common/selectable_text_widget.dart';
```

### 包引用
```dart
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/task_status.dart';
```

## 迁移指南

### 添加新页面
1. 在`pages/`下选择合适的子目录（如`compose/`、`preview/`等）
2. 如果没有合适的子目录，创建新的子目录
3. 在`main_layout.dart`中注册新页面

### 添加新组件
1. 根据组件功能选择合适的子目录
2. 通用组件放在`common/`目录
3. 功能特定组件放在对应的功能子目录

### 修改现有功能
1. 根据文件的新路径更新所有import语句
2. 确保相对路径正确
3. 运行构建验证无错误

## 已完成的工作

### 1. 目录结构优化
- ✅ 按功能模块重新组织了页面和组件文件
- ✅ 创建了清晰的目录层次结构
- ✅ 统一了命名规范

### 2. 代码清理
- ✅ 删除了所有未使用的页面文件（10个）
- ✅ 删除了所有未使用的组件文件（4个）
- ✅ 验证了删除后的功能完整性

### 3. 功能增强
- ✅ 在任务列表页添加了"删除全部任务"操作
- ✅ 修复了全局设置页面文件类型筛选输入框样式问题
- ✅ 修复了过滤规则输入框样式问题
- ✅ 确保所有输入控件都跟随主题变化

### 4. 文档更新
- ✅ 更新了前端架构分析报告
- ✅ 更新了前端目录结构重构文档
- ✅ 确保文档与实际代码结构一致

## 注意事项

1. **保持一致性**：遵循现有的目录结构和命名规范
2. **避免过度嵌套**：目录层级不超过3层
3. **及时更新文档**：重大变更时更新相关文档
4. **代码审查**：重构后进行代码审查确保质量

## 未来优化方向

1. **进一步模块化**：考虑将大型页面拆分为更小的子组件
2. **共享组件库**：提取更多通用组件到common目录
3. **性能优化**：优化组件渲染和状态管理
4. **测试覆盖**：增加单元测试和集成测试
5. **统一任务管理**：创建统一的任务管理组件

## 总结

本次目录结构重构显著改善了代码的组织性和可维护性。通过合理的目录划分和清晰的层次结构，使得代码更加易于理解和维护。新的结构为未来的功能扩展和团队协作提供了良好的基础。

同时，我们还完成了一系列功能增强和bug修复，包括添加删除全部任务功能和修复样式问题，确保了应用的稳定性和用户体验。
