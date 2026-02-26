# 前端代码清理和重构执行计划

## 概述

本文档详细说明了前端代码清理和重构的执行步骤，包括删除未使用的文件、优化代码结构和验证功能。

**计划制定日期**: 2026-02-26  
**预计执行时间**: 1-2小时  
**风险等级**: 低

---

## 执行前准备

### 1. 创建备份分支

```bash
cd /Users/hrcao/Documents/MusicManagerPlus
git checkout -b frontend-cleanup-20260226
```

### 2. 确认当前状态

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter doctor
flutter clean
flutter pub get
```

### 3. 记录当前功能状态

- [ ] 测试任务列表页面
- [ ] 测试任务详情页面
- [ ] 测试预览功能
- [ ] 测试执行功能
- [ ] 测试日志页面
- [ ] 测试全局设置页面
- [ ] 测试界面设置页面

---

## 阶段1：删除未使用的页面文件

### 执行步骤

#### 步骤1.1：删除任务监控页面

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli/lib/pages
rm task_monitor_page.dart
rm task_monitor.dart
```

**说明**: 这两个文件是旧的任务监控页面，已被`TaskListWidget`和`TaskDetailWidget`替代。

#### 步骤1.2：删除插件配置相关页面

```bash
rm plugin_config.dart
rm plugin_config_page.dart
rm plugin_list.dart
```

**说明**: 这些页面是旧的插件配置页面，功能已集成到`ComposePage`中。

#### 步骤1.3：删除配置相关页面

```bash
rm pipeline_config.dart
rm source_directories.dart
rm config_page.dart
```

**说明**: 这些页面是旧的配置页面，功能已集成到`ComposePage`中。

#### 步骤1.4：删除其他未使用的页面

```bash
rm home_page.dart
rm file_browser.dart
```

**说明**: 这些页面未被使用，可能是早期版本遗留。

### 验证步骤

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter build web
```

**预期结果**: 构建成功，无错误

---

## 阶段2：删除未使用的组件文件

### 执行步骤

#### 步骤2.1：删除任务相关组件

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli/lib/widgets
rm task_card.dart
rm task_list_item.dart
```

**说明**: 这些组件已被`TaskListWidget`替代。

#### 步骤2.2：删除对话框组件

```bash
rm change_details_dialog.dart
rm task_detail_dialog.dart
```

**说明**: 这些对话框组件未被使用，功能已集成到其他组件中。

### 验证步骤

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter build web
```

**预期结果**: 构建成功，无错误

---

## 阶段3：验证功能完整性

### 3.1 重新构建前端

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter build web
```

### 3.2 启动前端服务

```bash
./start_web.sh 8081
```

### 3.3 功能测试清单

#### 任务编排页面（ComposePage）

- [ ] 页面正常加载
- [ ] 源目录配置正常
- [ ] 流水线配置正常
- [ ] 预览按钮正常工作
- [ ] 执行按钮正常工作

#### 预览执行页面（PreviewPage）

- [ ] 页面正常加载
- [ ] 任务列表正常显示
- [ ] 任务详情正常显示
- [ ] 删除全部任务功能正常
- [ ] 刷新功能正常
- [ ] 预览功能正常
- [ ] 执行功能正常

#### 运行日志页面（LogPage）

- [ ] 页面正常加载
- [ ] 日志文件列表正常显示
- [ ] 日志内容正常显示

#### 全局设置页面（GlobalSettingsPage）

- [ ] 页面正常加载
- [ ] 线程池配置正常
- [ ] 扫描配置正常
- [ ] 过滤规则配置正常
- [ ] 文件类型筛选正常

#### 界面设置页面（AppearancePage）

- [ ] 页面正常加载
- [ ] 主题切换正常
- [ ] 字体大小调整正常
- [ ] 其他界面设置正常

---

## 阶段4：代码结构优化

### 4.1 优化组件目录结构

#### 创建子目录

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli/lib/widgets
mkdir -p common
mkdir -p task
mkdir -p config
mkdir -p preview
```

#### 移动组件文件

```bash
# 通用组件
mv selectable_text_widget.dart common/

# 任务相关组件
mv task_list_widget.dart task/
mv task_detail_widget.dart task/
mv task_operations.dart task/
mv task_status_helpers.dart task/

# 配置相关组件
mv compose_pipeline_panel.dart config/
mv compose_directory_panel.dart config/
mv compose_parameter_panel.dart config/
mv compose_config_panel.dart config/
mv compose_precondition_panel.dart config/

# 预览相关组件
mv preview_widget.dart preview/
mv change_record_table.dart preview/
```

#### 更新导入路径

需要更新以下文件中的导入路径：

1. `lib/pages/preview_page.dart`
2. `lib/pages/compose_page.dart`
3. 其他引用这些组件的文件

### 4.2 更新导入路径示例

```dart
// 旧的导入
import '../widgets/task_list_widget.dart';
import '../widgets/task_detail_widget.dart';

// 新的导入
import '../widgets/task/task_list_widget.dart';
import '../widgets/task/task_detail_widget.dart';
```

---

## 阶段5：最终验证

### 5.1 重新构建

```bash
cd /Users/hrcao/Documents/MusicManagerPlus/clients/flutter-web-cli
flutter clean
flutter build web
```

### 5.2 启动服务

```bash
./start_web.sh 8081
```

### 5.3 完整功能测试

重复执行阶段3.3的功能测试清单，确保所有功能正常。

---

## 回滚计划

如果执行过程中出现问题，可以按照以下步骤回滚：

```bash
cd /Users/hrcao/Documents/MusicManagerPlus
git checkout main
git branch -D frontend-cleanup-20260226
```

---

## 执行时间表

| 阶段 | 任务 | 预计时间 | 实际时间 | 状态 |
|------|------|----------|----------|------|
| 1 | 删除未使用的页面文件 | 10分钟 | - | 待执行 |
| 2 | 删除未使用的组件文件 | 5分钟 | - | 待执行 |
| 3 | 验证功能完整性 | 15分钟 | - | 待执行 |
| 4 | 代码结构优化 | 20分钟 | - | 待执行 |
| 5 | 最终验证 | 15分钟 | - | 待执行 |

---

## 执行记录

### 阶段1执行记录

- [ ] 删除task_monitor_page.dart
- [ ] 删除task_monitor.dart
- [ ] 删除plugin_config.dart
- [ ] 删除plugin_config_page.dart
- [ ] 删除plugin_list.dart
- [ ] 删除pipeline_config.dart
- [ ] 删除source_directories.dart
- [ ] 删除config_page.dart
- [ ] 删除home_page.dart
- [ ] 删除file_browser.dart
- [ ] 构建验证通过

### 阶段2执行记录

- [ ] 删除task_card.dart
- [ ] 删除task_list_item.dart
- [ ] 删除change_details_dialog.dart
- [ ] 删除task_detail_dialog.dart
- [ ] 构建验证通过

### 阶段3执行记录

- [ ] 重新构建成功
- [ ] 前端服务启动成功
- [ ] 任务编排页面测试通过
- [ ] 预览执行页面测试通过
- [ ] 运行日志页面测试通过
- [ ] 全局设置页面测试通过
- [ ] 界面设置页面测试通过

### 阶段4执行记录

- [ ] 创建子目录
- [ ] 移动组件文件
- [ ] 更新导入路径
- [ ] 构建验证通过

### 阶段5执行记录

- [ ] 重新构建成功
- [ ] 前端服务启动成功
- [ ] 完整功能测试通过

---

## 注意事项

1. **备份**: 执行前确保代码已提交到Git
2. **测试**: 每个阶段完成后都要进行构建测试
3. **验证**: 删除文件前再次确认无引用
4. **回滚**: 准备好回滚方案，以备不时之需

---

## 后续改进建议

1. **添加单元测试**: 为核心组件添加单元测试
2. **集成测试**: 添加端到端测试
3. **代码审查**: 建立代码审查流程
4. **文档更新**: 及时更新相关文档
5. **持续优化**: 定期进行代码清理和优化

---

**计划制定时间**: 2026-02-26  
**计划版本**: 1.0  
**执行人**: FileManager Plus Team
