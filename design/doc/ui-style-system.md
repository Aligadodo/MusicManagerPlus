# UI样式系统 设计文档

## 概述

**功能**: 提供统一的UI组件样式管理和创建

**设计模式**: 工厂模式、单例模式

**核心目标**: 实现样式统一、易于维护、支持主题切换

## 核心组件

### 1. StyleFactory

**职责**: UI组件样式工厂，提供统一的组件创建接口

**核心方法**:
- createButton(): 创建按钮
- createLabel(): 创建标签
- createHeader(): 创建标题
- createChapter(): 创建章节
- createSeparator(): 创建分隔线
- createVBoxPanel(): 创建VBox面板
- createHBoxPanel(): 创建HBox面板
- createTabPane(): 创建标签页

### 2. ComponentFactory

**职责**: 组件工厂，负责具体的组件创建

**核心方法**:
- createLabel(): 创建标签
- createButton(): 创建按钮
- createSeparator(): 创建分隔线
- createTextArea(): 创建文本区域
- createListView(): 创建列表视图

### 3. ComponentStyleManager

**职责**: 组件样式管理器，管理组件样式

**核心方法**:
- initComponentStyleManager(): 初始化样式管理器
- applyComponentStyle(): 应用组件样式
- refreshComponentStyle(): 刷新组件样式

### 4. ThemeManager

**职责**: 主题管理器，管理主题配置和切换

**核心方法**:
- getInstance(): 获取单例实例
- updateCurrentTheme(): 更新当前主题
- switchTheme(): 切换主题
- saveTheme(): 保存主题

### 5. ThemeConfig

**职责**: 主题配置，存储主题参数

**配置项**:
- 背景颜色
- 强调色
- 文本颜色
- 边框颜色
- 字体
- 透明度
- 圆角

### 6. StyleTemplateManager

**职责**: 样式模板管理器，管理样式模板

**核心方法**:
- getInstance(): 获取单例实例
- getTemplate(): 获取样式模板
- applyTemplate(): 应用样式模板

## 样式架构

### 1. 三层架构

**架构层次**:
1. **ThemeConfig**: 主题配置层，定义颜色、字体、透明度等
2. **StyleFactory**: 样式工厂层，提供统一的组件创建接口
3. **ComponentFactory**: 组件工厂层，负责具体的组件创建

**样式应用流程**:
```
ThemeConfig → StyleFactory → ComponentFactory → UI Component
```

### 2. 样式管理流程

**初始化流程**:
1. 创建ThemeConfig实例
2. 初始化StyleFactory
3. 初始化ComponentFactory
4. 初始化ComponentStyleManager
5. 初始化ThemeManager
6. 初始化StyleTemplateManager

**样式应用流程**:
1. 从ThemeConfig获取主题参数
2. 通过StyleFactory创建组件
3. 通过ComponentFactory应用样式
4. 通过ComponentStyleManager管理样式
5. 通过ThemeManager切换主题

## 核心样式

### 1. 面板样式

**样式特点**:
- 半透明背景
- 圆角边框
- 阴影效果

**实现方式**:
```java
String bgColor = theme.getPanelBgColor();
int alpha = (int) (theme.getGlassOpacity() * 255);
String alphaHex = String.format("%02x", alpha);
bgColor = bgColor + alphaHex;
panel.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: %.1f;", bgColor, theme.getCornerRadius()));
```

### 2. 按钮样式

**样式特点**:
- 统一的颜色
- 统一的字体
- 统一的圆角

**实现方式**:
```java
btn.setStyle(String.format(
    "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: %.1f;",
    theme.getAccentColor(), theme.getTextPrimaryColor(), theme.getCornerRadius()
));
```

### 3. 标签样式

**样式特点**:
- 统一的颜色
- 统一的字体
- 统一的大小

**实现方式**:
```java
label.setStyle(String.format(
    "-fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: %dpx;",
    theme.getTextPrimaryColor(), theme.getFontFamily(), size
));
```

### 4. 分隔线样式

**样式特点**:
- 统一的颜色
- 统一的宽度
- 统一的样式

**实现方式**:
```java
separator.setStyle(String.format(
    "-fx-border-color: %s; -fx-border-width: %.1f 0 0 0;",
    theme.getBorderColor(), theme.getBorderWidth()
));
```

## 主题系统

### 1. 主题配置

**配置项**:
- bgColor: 背景颜色
- accentColor: 强调色
- textPrimaryColor: 文本主色
- textSecondaryColor: 文本次色
- textTertiaryColor: 文本三级色
- textDisabledColor: 文本禁用色
- borderColor: 边框颜色
- fontFamily: 字体家族
- fontSize: 字体大小
- glassOpacity: 玻璃透明度
- cornerRadius: 圆角
- borderWidth: 边框宽度
- darkBackground: 深色背景

### 2. 主题切换

**切换方式**:
- 选择预设主题
- 手动配置主题
- 导入主题配置

**切换流程**:
1. 获取新主题配置
2. 更新ThemeConfig
3. 刷新所有组件样式
4. 应用新主题

### 3. 主题预设

**预设主题**:
- 默认主题
- 深色主题
- 浅色主题
- 自定义主题

## 样式工具

### 1. FloatingTooltip

**职责**: 悬浮提示工具

**功能**:
- 绑定到UI组件
- 显示多行提示信息
- 支持开关控制

**使用方式**:
```java
FloatingTooltip.bindToNode(component, "标题", Arrays.asList("提示1", "提示2"));
```

### 2. FXDialogUtils

**职责**: 对话框工具

**功能**:
- 创建对话框
- 显示提示信息
- 获取用户输入

**使用方式**:
```java
FXDialogUtils.showInfoDialog("标题", "内容");
```

### 3. NodeUtils

**职责**: 节点工具

**功能**:
- 节点操作
- 样式应用
- 事件绑定

**使用方式**:
```java
NodeUtils.applyStyle(node, style);
```

### 4. FontManager

**职责**: 字体管理器

**功能**:
- 加载字体
- 管理字体
- 应用字体

**使用方式**:
```java
FontManager.loadFont(fontPath);
```

## 样式优化

### 1. 性能优化

**优化方式**:
- 样式缓存
- 样式复用
- 延迟加载

**优势**:
- 减少样式计算
- 提高渲染速度
- 降低内存占用

### 2. 样式复用

**实现方式**:
- 使用样式模板
- 使用样式工厂
- 使用样式管理器

**优势**:
- 减少重复代码
- 提高开发效率
- 便于维护

### 3. 样式热更新

**实现方式**:
- 监听主题变化
- 自动刷新样式
- 实时预览效果

**优势**:
- 即时看到效果
- 提高用户体验
- 便于调试

## 设计要点

### 1. 统一管理

**实现方式**:
- 使用StyleFactory统一创建组件
- 使用ThemeConfig统一管理主题
- 使用ComponentStyleManager统一管理样式

**优势**:
- 样式统一
- 易于维护
- 易于扩展

### 2. 灵活配置

**实现方式**:
- 支持主题配置
- 支持自定义样式
- 支持样式模板

**优势**:
- 灵活配置
- 个性化定制
- 满足不同需求

### 3. 易于扩展

**实现方式**:
- 工厂模式
- 单例模式
- 模板模式

**优势**:
- 易于添加新组件
- 易于添加新主题
- 易于添加新样式

## 注意事项

### 1. 性能考虑

**注意事项**:
- 避免频繁更新样式
- 使用样式缓存
- 优化样式计算

### 2. 内存管理

**注意事项**:
- 及时清理缓存
- 限制缓存大小
- 避免内存泄漏

### 3. 线程安全

**注意事项**:
- UI更新在JavaFX线程
- 样式访问使用同步机制
- 避免死锁

## 常见问题

### Q1: 如何自定义样式？

**步骤**:
1. 创建自定义样式
2. 通过StyleFactory应用样式
3. 或者直接设置组件样式

### Q2: 如何切换主题？

**步骤**:
1. 获取新主题配置
2. 调用ThemeManager.switchTheme()
3. 主题自动应用到所有组件

### Q3: 如何添加新组件样式？

**步骤**:
1. 在ComponentFactory中添加创建方法
2. 在StyleFactory中添加工厂方法
3. 定义样式规则

## 最佳实践

### 1. 样式定义

**推荐方式**:
- 使用StyleFactory创建组件
- 使用ThemeConfig管理主题
- 避免硬编码样式

### 2. 样式应用

**推荐方式**:
- 统一使用StyleFactory
- 统一使用ThemeConfig
- 统一使用ComponentStyleManager

### 3. 样式维护

**推荐方式**:
- 定期检查样式一致性
- 及时更新样式
- 保持样式简洁

## 优化建议

### 1. 功能扩展

**当前实现**: 基本的样式系统

**优化方向**:
- 支持CSS外部样式表
- 支持样式继承
- 支持样式动画

### 2. 性能优化

**当前实现**: 基本的样式缓存和复用

**优化方向**:
- 优化样式计算
- 优化样式渲染
- 优化内存占用

### 3. 用户体验优化

**当前实现**: 基本的样式功能

**优化方向**:
- 添加样式预览
- 添加样式对比
- 添加样式建议

## 总结

UI样式系统提供了完整的样式管理功能，具有以下特点：

1. **统一管理**: 统一的样式创建和管理
2. **灵活配置**: 支持主题配置和自定义样式
3. **易于扩展**: 工厂模式，易于添加新组件
4. **性能优化**: 样式缓存和复用
5. **易于维护**: 清晰的架构和代码组织

通过UI样式系统，开发者可以方便地创建和管理UI组件样式，实现统一的界面风格，提高开发效率和用户体验。

---

**相关文档**:
- [ui-overview.md](ui-overview.md)
- [ui-appearance-manager.md](ui-appearance-manager.md)
- [ui-compose-view.md](ui-compose-view.md)

**文档版本**: 1.0  
**最后更新**: 2026-01-27  
**维护者**: FileEditTools Team
