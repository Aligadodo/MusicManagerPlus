# 主题管理系统设计文档

## 1. 架构设计

### 1.1 后端架构
- **主题存储方式**：文件化管理，每个主题对应一个JSON文件
- **主题类型**：
  - 系统预设主题：不可修改，存储在`themes/default/`目录
  - 用户自定义主题：可修改，存储在`themes/custom/`目录
- **默认主题**：系统预设的默认主题，不可删除

### 1.2 前端架构
- **主题样式提供器**：全局主题样式管理，提供统一的主题样式访问
- **组件样式应用**：所有组件自动应用主题样式，无需手动设置
- **主题管理界面**：支持主题选择、预览、创建、修改和删除

## 2. 数据结构设计

### 2.1 主题样式结构
```json
{
  "id": "unique-theme-id",
  "name": "主题名称",
  "description": "主题描述",
  "type": "default|custom",
  "createdAt": "2026-02-12T10:00:00Z",
  "updatedAt": "2026-02-12T10:00:00Z",
  "config": {
    "theme": "light|dark|custom",
    "bgColor": "#FFFFFF",
    "accentColor": "#2196F3",
    "textPrimaryColor": "#000000",
    "textSecondaryColor": "#666666",
    "textTertiaryColor": "#999999",
    "textDisabledColor": "#CCCCCC",
    "panelBgColor": "#FFFFFF",
    "listBgColor": "#FFFFFF",
    "listRowEvenBgColor": "#FFFFFF",
    "listRowOddBgColor": "#F5F5F5",
    "listRowSelectedBgColor": "#E3F2FD",
    "listRowSelectedTextColor": "#2196F3",
    "listRowHoverBgColor": "#F0F8FF",
    "listBorderColor": "#E1E4E8",
    "listHeaderBgColor": "#F8F9FA",
    "listHeaderTextColor": "#586069",
    "borderColor": "#E1E4E8",
    "glassOpacity": 0.8,
    "darkBackground": false,
    "fontFamily": "Roboto",
    "fontSize": 14,
    "cornerRadius": 6,
    "borderWidth": 1,
    "buttonLargeSize": 48,
    "buttonSmallSize": 36
  }
}
```

## 3. 后端接口设计

### 3.1 主题管理接口
- **GET /api/themes**：获取所有主题列表
- **GET /api/themes/{id}**：获取单个主题详情
- **POST /api/themes**：创建新的自定义主题
- **PUT /api/themes/{id}**：更新自定义主题
- **DELETE /api/themes/{id}**：删除自定义主题
- **GET /api/themes/default**：获取默认主题
- **PUT /api/themes/default**：设置默认主题

### 3.2 主题配置接口
- **GET /api/config/theme**：获取当前主题配置
- **PUT /api/config/theme**：更新当前主题配置

## 4. 前端实现设计

### 4.1 主题样式提供器
- **ThemeProvider**：全局主题状态管理
- **ThemeData**：主题样式数据模型
- **ThemeExtension**：组件主题样式扩展

### 4.2 组件样式应用
- **自动应用**：所有组件自动从主题提供器获取样式
- **主题适配**：根据当前主题自动调整组件样式
- **动态更新**：主题切换时自动更新所有组件样式

### 4.3 主题管理界面
- **主题列表**：展示所有可用主题
- **主题预览**：实时预览主题效果
- **主题创建**：创建新的自定义主题
- **主题编辑**：编辑现有自定义主题
- **主题删除**：删除自定义主题

## 5. 实现计划

### 5.1 后端实现
1. 创建主题管理服务
2. 实现主题文件管理
3. 实现主题管理接口
4. 集成到现有配置系统

### 5.2 前端实现
1. 创建主题样式提供器
2. 修改组件样式应用逻辑
3. 实现主题管理界面
4. 集成到现有应用

## 6. 测试计划

### 6.1 功能测试
- 主题切换功能
- 主题创建、修改、删除功能
- 主题样式应用一致性
- 多页面主题样式同步

### 6.2 性能测试
- 主题切换响应时间
- 主题加载性能
- 大型应用的主题样式应用性能

## 7. 扩展计划

### 7.1 未来功能
- 主题导出和导入
- 主题分享功能
- 主题样式版本控制
- 主题样式预览功能
- 主题样式自动生成