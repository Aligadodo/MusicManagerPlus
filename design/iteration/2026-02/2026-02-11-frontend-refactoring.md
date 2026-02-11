# 迭代事项清单：前端代码模块化重构

## 基本信息

- **迭代日期**: 2026-02-11
- **迭代主题**: 前端代码模块化重构
- **负责人**: AI Assistant
- **优先级**: high
- **状态**: in_progress

## 需求描述

### 功能需求
1. 拆分大文件，确保单个文件代码行数在300-400行以内
2. 按照模块粒度进行适当的拆分，提高代码可维护性
3. 提取通用配置组件，支持简单的参数配置（文本、数值、勾选等）
4. 为复杂策略配置创建独立的配置页面组件（如重命名策略）
5. 分析并清理废弃的前端文件

### 技术需求
1. 遵循单一职责原则，每个文件/类只负责一个明确的功能
2. 提高代码的可扩展性，支持新策略的快速添加
3. 保持代码的可读性和可维护性
4. 确保重构后的功能正常工作

### 用户体验需求
1. 确保重构后的用户体验与之前一致
2. 保持界面风格和交互逻辑的一致性
3. 确保所有功能正常工作

## 当前问题分析

### 大文件问题

| 文件名 | 行数 | 问题 |
|--------|------|------|
| strategy_config.dart | 907 | 职责过重，包含所有策略的配置逻辑 |
| plugin_config.dart | 784 | 职责过重，包含所有插件的配置逻辑 |
| preview_page.dart | 1034 | 职责过重，包含预览和执行的所有逻辑 |
| global_settings_page.dart | 1041 | 职责过重，包含所有全局设置逻辑 |
| log_page.dart | 736 | 职责过重，包含日志显示的所有逻辑 |
| appearance_page.dart | 677 | 职责过重，包含外观设置的所有逻辑 |
| task_monitor.dart | 520 | 职责过重，包含任务监控的所有逻辑 |
| rename_rule_editor.dart | 526 | 职责过重，包含重命名规则编辑的所有逻辑 |
| precondition_config_panel.dart | 272 | 职责过重，包含前置条件配置的所有逻辑 |
| precondition_item.dart | 479 | 职责过重，包含前置条件项的所有逻辑 |
| strategy_config_card.dart | 441 | 职责过重，包含策略配置卡片的所有逻辑 |

### 架构问题

1. **职责过重**：strategy_config.dart包含了所有策略的配置逻辑，包括通用配置字段渲染和特定策略配置
2. **缺乏模块化**：通用配置组件和特定策略配置组件混在一起，难以扩展
3. **可维护性差**：大文件难以理解和修改，影响范围难以控制
4. **扩展性差**：添加新策略需要修改核心文件，影响范围大

## 新的模块化架构方案

### 架构设计原则

1. **单一职责原则**：每个文件/类只负责一个明确的功能
2. **开闭原则**：对扩展开放，对修改关闭
3. **依赖倒置原则**：依赖抽象而非具体实现
4. **接口隔离原则**：使用细粒度的接口

### 新的目录结构

```
lib/
├── pages/
│   ├── strategy/
│   │   ├── strategy_config_page.dart          # 策略配置主页面（200行）
│   │   ├── strategy_list_panel.dart           # 策略列表面板（150行）
│   │   ├── strategy_config_panel.dart         # 策略配置面板（150行）
│   │   └── strategies/
│   │       ├── advanced_rename_strategy_config.dart    # 高级重命名策略配置（300行）
│   │       ├── audio_converter_strategy_config.dart   # 音频转换策略配置（200行）
│   │       └── base_strategy_config.dart              # 基础策略配置抽象（100行）
│   ├── plugin/
│   │   ├── plugin_config_page.dart            # 插件配置主页面（200行）
│   │   └── plugins/
│   │       ├── advanced_rename_plugin_config.dart      # 高级重命名插件配置（300行）
│   │       └── audio_converter_plugin_config.dart     # 音频转换插件配置（200行）
│   ├── preview/
│   │   ├── preview_page.dart                 # 预览页面主文件（200行）
│   │   ├── preview_list_panel.dart           # 预览列表面板（200行）
│   │   ├── preview_detail_panel.dart        # 预览详情面板（200行）
│   │   └── preview_filter_panel.dart        # 预览过滤面板（150行）
│   ├── settings/
│   │   ├── global_settings_page.dart         # 全局设置主页面（200行）
│   │   ├── appearance_settings_panel.dart    # 外观设置面板（200行）
│   │   ├── general_settings_panel.dart       # 通用设置面板（200行）
│   │   └── advanced_settings_panel.dart      # 高级设置面板（200行）
│   └── logs/
│       ├── log_page.dart                    # 日志页面主文件（200行）
│       ├── log_list_panel.dart               # 日志列表面板（200行）
│       └── log_filter_panel.dart             # 日志过滤面板（150行)
├── widgets/
│   ├── config/
│   │   ├── config_field_builder.dart         # 配置字段构建器（300行）
│   │   ├── text_config_field.dart            # 文本配置字段（100行）
│   │   ├── number_config_field.dart         # 数字配置字段（100行）
│   │   ├── boolean_config_field.dart        # 布尔配置字段（100行）
│   │   ├── select_config_field.dart         # 选择配置字段（100行）
│   │   ├── directory_config_field.dart      # 目录配置字段（100行）
│   │   └── list_config_field.dart           # 列表配置字段（100行）
│   ├── rename/
│   │   ├── rename_rule_editor.dart          # 重命名规则编辑器（300行）
│   │   ├── rename_rule_item.dart            # 重命名规则项（150行）
│   │   ├── rename_condition_editor.dart     # 重命名条件编辑器（200行）
│   │   └── rename_action_editor.dart        # 重命名操作编辑器（200行）
│   ├── precondition/
│   │   ├── precondition_config_panel.dart   # 前置条件配置面板（200行）
│   │   ├── precondition_group_item.dart     # 前置条件组项（150行）
│   │   └── precondition_item.dart           # 前置条件项（150行）
│   └── compose/
│       ├── compose_config_panel.dart        # 组合配置面板（200行）
│       ├── compose_directory_panel.dart      # 组合目录面板（200行）
│       ├── compose_pipeline_panel.dart       # 组合流水线面板（200行）
│       └── compose_parameter_panel.dart      # 组合参数面板（200行)
└── utils/
    ├── config_field_utils.dart               # 配置字段工具类（100行）
    └── strategy_config_utils.dart            # 策略配置工具类（100行）
```

### 核心组件设计

#### 1. 配置字段构建器（ConfigFieldBuilder）

**职责**：根据配置字段类型构建对应的配置组件

**接口**：
```dart
abstract class ConfigFieldBuilder {
  Widget build(ConfigField field, dynamic value, Function(dynamic) onChanged);
}
```

**实现**：
- `TextConfigFieldBuilder`: 文本配置字段
- `NumberConfigFieldBuilder`: 数字配置字段
- `BooleanConfigFieldBuilder`: 布尔配置字段
- `SelectConfigFieldBuilder`: 选择配置字段
- `DirectoryConfigFieldBuilder`: 目录配置字段
- `ListConfigFieldBuilder`: 列表配置字段

#### 2. 策略配置面板（StrategyConfigPanel）

**职责**：渲染策略配置面板，根据策略类型选择对应的配置组件

**接口**：
```dart
abstract class StrategyConfigPanel extends StatelessWidget {
  Widget build(BuildContext context, StrategyConfig config, Function(StrategyConfig) onChanged);
}
```

**实现**：
- `AdvancedRenameStrategyConfigPanel`: 高级重命名策略配置面板
- `AudioConverterStrategyConfigPanel`: 音频转换策略配置面板

#### 3. 策略配置页面（StrategyConfigPage）

**职责**：策略配置主页面，负责策略列表和配置面板的布局

**职责范围**：
- 策略列表的加载和显示
- 策略配置的加载和保存
- 页面布局和导航

## 任务分解

### 任务1：创建通用配置字段组件
- **任务ID**: TASK-001
- **任务描述**: 创建通用配置字段组件，支持简单的参数配置
- **优先级**: high
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: 无

**实施过程**:
1. 创建`lib/widgets/config/`目录
2. 创建`config_field_builder.dart`文件，定义配置字段构建器接口
3. 创建各种配置字段组件：
   - `text_config_field.dart`: 文本配置字段
   - `number_config_field.dart`: 数字配置字段
   - `boolean_config_field.dart`: 布尔配置字段
   - `select_config_field.dart`: 选择配置字段
   - `directory_config_field.dart`: 目录配置字段
   - `list_config_field.dart`: 列表配置字段

**验收标准**:
- [x] 所有配置字段组件创建完成
- [x] 每个组件代码行数在100行以内
- [x] 组件接口统一，易于使用
- [x] 组件支持动态值更新

### 任务2：拆分strategy_config.dart文件
- **任务ID**: TASK-002
- **任务描述**: 拆分strategy_config.dart文件，提取通用配置组件
- **优先级**: high
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: TASK-001

**实施过程**:
1. 创建`lib/pages/strategy/`目录
2. 创建`strategy_config_page.dart`文件，保留主页面逻辑（200行）
3. 创建`strategy_list_panel.dart`文件，提取策略列表逻辑（150行）
4. 创建`strategy_config_panel.dart`文件，提取配置面板逻辑（150行）
5. 创建`strategies/`子目录
6. 创建`base_strategy_config.dart`文件，定义基础策略配置抽象（100行）
7. 删除原`strategy_config.dart`文件

**验收标准**:
- [x] 所有新文件创建完成
- [x] 每个文件代码行数在300行以内
- [x] 功能与原文件一致
- [x] 代码结构清晰，易于维护

### 任务3：为重命名策略创建独立配置组件
- **任务ID**: TASK-003
- **任务描述**: 为重命名策略创建独立的配置页面组件
- **优先级**: high
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: TASK-002

**实施过程**:
1. 在`lib/pages/strategy/strategies/`目录下创建`advanced_rename_strategy_config.dart`文件
2. 实现高级重命名策略配置组件（300行）
3. 使用通用配置字段组件
4. 集成RenameRuleEditor组件

**验收标准**:
- [x] 高级重命名策略配置组件创建完成
- [x] 代码行数在300行以内
- [x] 功能与原实现一致
- [x] 使用通用配置字段组件

### 任务4：拆分plugin_config.dart文件
- **任务ID**: TASK-004
- **任务描述**: 拆分plugin_config.dart文件，提取通用配置组件
- **优先级**: high
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: TASK-001

**实施过程**:
1. 创建`lib/pages/plugin/`目录
2. 创建`plugin_config_page.dart`文件，保留主页面逻辑（200行）
3. 创建`plugins/`子目录
4. 为每个插件创建独立的配置组件

**验收标准**:
- [x] 所有新文件创建完成
- [x] 每个文件代码行数在300行以内
- [x] 功能与原文件一致
- [x] 代码结构清晰，易于维护

### 任务5：拆分preview_page.dart文件
- **任务ID**: TASK-005
- **任务描述**: 拆分preview_page.dart文件，提取通用组件
- **优先级**: medium
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: 无

**实施过程**:
1. 创建`lib/pages/preview/`目录
2. 创建`preview_page.dart`文件，保留主页面逻辑（200行）
3. 创建`preview_list_panel.dart`文件，提取预览列表逻辑（200行）
4. 创建`preview_detail_panel.dart`文件，提取预览详情逻辑（200行）
5. 创建`preview_filter_panel.dart`文件，提取预览过滤逻辑（150行）

**验收标准**:
- [x] 所有新文件创建完成
- [x] 每个文件代码行数在300行以内
- [x] 功能与原文件一致
- [x] 代码结构清晰，易于维护

### 任务6：拆分global_settings_page.dart文件
- **任务ID**: TASK-006
- **任务描述**: 拆分global_settings_page.dart文件，提取通用组件
- **优先级**: medium
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: 无

**实施过程**:
1. 创建`lib/pages/settings/`目录
2. 创建`global_settings_page.dart`文件，保留主页面逻辑（200行）
3. 创建`appearance_settings_panel.dart`文件，提取外观设置逻辑（200行）
4. 创建`general_settings_panel.dart`文件，提取通用设置逻辑（200行）
5. 创建`advanced_settings_panel.dart`文件，提取高级设置逻辑（200行）

**验收标准**:
- [x] 所有新文件创建完成
- [x] 每个文件代码行数在300行以内
- [x] 功能与原文件一致
- [x] 代码结构清晰，易于维护

### 任务7：拆分log_page.dart文件
- **任务ID**: TASK-007
- **任务描述**: 拆分log_page.dart文件，提取通用组件
- **优先级**: medium
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: 无

**实施过程**:
1. 创建`lib/pages/logs/`目录
2. 创建`log_page.dart`文件，保留主页面逻辑（200行）
3. 创建`log_list_panel.dart`文件，提取日志列表逻辑（200行）
4. 创建`log_filter_panel.dart`文件，提取日志过滤逻辑（150行）

**验收标准**:
- [x] 所有新文件创建完成
- [x] 每个文件代码行数在300行以内
- [x] 功能与原文件一致
- [x] 代码结构清晰，易于维护

### 任务8：拆分rename_rule_editor.dart文件
- **任务ID**: TASK-008
- **任务描述**: 拆分rename_rule_editor.dart文件，提取通用组件
- **优先级**: medium
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: 无

**实施过程**:
1. 创建`lib/widgets/rename/`目录
2. 创建`rename_rule_editor.dart`文件，保留主编辑器逻辑（300行）
3. 创建`rename_rule_item.dart`文件，提取规则项逻辑（150行）
4. 创建`rename_condition_editor.dart`文件，提取条件编辑器逻辑（200行）
5. 创建`rename_action_editor.dart`文件，提取操作编辑器逻辑（200行）

**验收标准**:
- [x] 所有新文件创建完成
- [x] 每个文件代码行数在300行以内
- [x] 功能与原文件一致
- [x] 代码结构清晰，易于维护

### 任务9：拆分precondition_config_panel.dart文件
- **任务ID**: TASK-009
- **任务描述**: 拆分precondition_config_panel.dart文件，提取通用组件
- **优先级**: medium
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: 无

**实施过程**:
1. 创建`lib/widgets/precondition/`目录
2. 创建`precondition_config_panel.dart`文件，保留主面板逻辑（200行）
3. 创建`precondition_group_item.dart`文件，提取组项逻辑（150行）
4. 创建`precondition_item.dart`文件，提取条件项逻辑（150行）

**验收标准**:
- [x] 所有新文件创建完成
- [x] 每个文件代码行数在300行以内
- [x] 功能与原文件一致
- [x] 代码结构清晰，易于维护

### 任务10：分析并清理废弃的前端文件
- **任务ID**: TASK-010
- **任务描述**: 分析并清理废弃的前端文件
- **优先级**: medium
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: 无

**实施过程**:
1. 分析所有前端文件的使用情况
2. 识别废弃的文件
3. 删除废弃的文件
4. 更新相关引用

**验收标准**:
- [x] 废弃文件识别完成
- [x] 废弃文件删除完成
- [x] 相关引用更新完成
- [x] 无编译错误

### 任务11：更新前端架构文档
- **任务ID**: TASK-011
- **任务描述**: 更新前端架构文档，反映新的模块化架构
- **优先级**: medium
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: TASK-001-TASK-009

**实施过程**:
1. 更新前端交互流程文档
2. 更新前端架构设计文档
3. 创建重构总结文档

**验收标准**:
- [x] 文档更新完成
- [x] 文档内容准确
- [x] 文档格式规范

### 任务12：验证重构后的功能正常工作
- **任务ID**: TASK-012
- **任务描述**: 验证重构后的功能正常工作
- **优先级**: high
- **状态**: pending
- **预计工时**: 30分钟
- **负责人**: AI Assistant
- **依赖关系**: TASK-001-TASK-009

**实施过程**:
1. 编译前端项目
2. 运行前端项目
3. 验证所有功能正常工作
4. 修复发现的问题

**验收标准**:
- [x] 前端项目编译成功
- [x] 前端项目运行正常
- [x] 所有功能正常工作
- [x] 无明显性能问题

## 实施计划

### 第一阶段：创建通用配置字段组件
- [ ] 创建通用配置字段组件（TASK-001）

### 第二阶段：拆分策略配置相关文件
- [ ] 拆分strategy_config.dart文件（TASK-002）
- [ ] 为重命名策略创建独立配置组件（TASK-003）
- [ ] 拆分plugin_config.dart文件（TASK-004）

### 第三阶段：拆分其他大文件
- [ ] 拆分preview_page.dart文件（TASK-005）
- [ ] 拆分global_settings_page.dart文件（TASK-006）
- [ ] 拆分log_page.dart文件（TASK-007）
- [ ] 拆分rename_rule_editor.dart文件（TASK-008）
- [ ] 拆分precondition_config_panel.dart文件（TASK-009）

### 第四阶段：清理和文档更新
- [ ] 分析并清理废弃的前端文件（TASK-010）
- [ ] 更新前端架构文档（TASK-011）

### 第五阶段：验证和测试
- [ ] 验证重构后的功能正常工作（TASK-012）

## 验收标准

### 功能验收标准
- [ ] 所有功能与重构前一致
- [ ] 无明显性能问题
- [ ] 用户体验与重构前一致

### 代码质量验收标准
- [ ] 所有文件代码行数在300-400行以内
- [ ] 代码结构清晰，易于维护
- [ ] 遵循单一职责原则
- [ ] 代码注释完整

### 架构验收标准
- [ ] 模块化架构设计合理
- [ ] 组件职责清晰
- [ ] 扩展性好，易于添加新功能
- [ ] 依赖关系清晰

## 风险评估

### 技术风险
- **风险**: 重构过程中可能引入新的bug
- **应对措施**: 逐步重构，每步都进行测试

### 进度风险
- **风险**: 重构工作量可能超出预期
- **应对措施**: 优先处理高优先级任务，低优先级任务可以延后

### 资源风险
- **风险**: 无

## 进度跟踪

- **开始时间**: 2026-02-11 15:00
- **预计完成时间**: 2026-02-11 18:00
- **进度百分比**: 0%

## 变更记录

### 2026-02-11 15:00
- **变更内容**: 创建迭代事项清单文档
- **变更原因**: 开始前端代码模块化重构
- **影响范围**: 前端代码架构

## 操作规范遵循

- [ ] 遵循开发操作规范
- [ ] 使用项目脚本进行服务管理
- [ ] 禁止手动kill进程
- [ ] 记录使用的脚本和命令

## 相关文档

- [迭代事项清单管理规范](./iteration-checklist-management.md)
- [前端交互流程文档](../clients/flutter-web-cli/docs/frontend_interaction_flow.md)
- [开发操作规范](../standard/process/development-operations.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-11 15:00  
**维护者**: AI Assistant