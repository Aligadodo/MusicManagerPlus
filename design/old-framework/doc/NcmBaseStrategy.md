# NcmBaseStrategy 设计文档

## 概述

**功能**: NCM基础策略类，提供通用的NCM处理功能

**继承**: IAppStrategy, IConfigComponent

**设计模式**: 模板方法模式

**子类**: NcmConvertStrategy, NcmCacheTransStrategy, NcmLyricDownloadStrategy

## 核心职责

1. 提供通用的NCM处理功能
2. 路径选择组件集成
3. 输出路径管理
4. 配置参数管理

## 核心组件

### 1. 路径选择组件

- `pathSelection`: PathSelectionComponent实例
  - 源路径选择
  - 输出路径选择
  - 路径模式选择（根目录、子目录等）

### 2. 默认配置

**默认参数**:
- 输出目录模式：子目录
- 默认路径：Convert - Cache

## 核心方法

### 1. getOutputPath(File file)
**功能**: 获取输出路径

**处理流程**:
1. 调用pathSelection的getOutputPath方法
2. 根据配置生成输出路径
3. 返回完整的输出路径

### 2. getConfigNode()
**功能**: 获取配置UI节点

**实现**: 由子类实现

### 3. captureParams()
**功能**: 捕获配置参数

**实现**: 由子类实现

## 设计要点

### 1. 路径选择集成

**集成方式**:
- 使用PathSelectionComponent
- 提供统一的路径选择界面
- 支持多种路径模式

**路径模式**:
- 根目录
- 子目录
- 自定义路径

### 2. 输出路径管理

**管理方式**:
- 基于源文件路径生成输出路径
- 支持路径模式选择
- 自动创建不存在的目录

### 3. 配置参数管理

**管理方式**:
- 通过pathSelection管理路径参数
- 子类扩展其他参数
- 统一的配置接口

## 注意事项

### 1. 路径处理

**注意事项**:
- 确保路径有效性
- 处理路径分隔符
- 处理相对路径和绝对路径

### 2. 目录创建

**创建方式**:
- 自动创建不存在的目录
- 检查目录权限
- 处理创建失败

### 3. 配置持久化

**持久化方式**:
- 通过Properties保存配置
- 支持配置加载
- 支持默认值

## 交互设计

### 1. 配置界面

**组件**:
- 路径选择组件
- 子类特定配置

### 2. 路径选择

**功能**:
- 源路径选择
- 输出路径选择
- 路径模式选择

## 配置管理

### 1. 配置保存

**配置项**:
- 路径相关配置
- 子类特定配置

### 2. 配置加载

**加载逻辑**:
- 从Properties中读取配置
- 设置UI组件状态
- 更新运行时参数

## 扩展指南

### 1. 创建子类

**步骤**:
1. 继承NcmBaseStrategy
2. 实现getConfigNode方法
3. 实现captureParams方法
4. 添加特定功能

### 2. 添加UI组件

**方式**:
- 在getConfigNode中添加
- 使用StyleFactory创建组件
- 添加悬浮提示

### 3. 添加参数

**方式**:
- 定义UI组件
- 定义运行时参数
- 在captureParams中捕获
- 在saveConfig/loadConfig中持久化

## 总结

NcmBaseStrategy提供了NCM处理的基础框架，具有以下特点：

1. **通用功能**: 提供通用的NCM处理功能
2. **路径管理**: 集成路径选择组件
3. **易于扩展**: 模板方法设计，子类易于扩展
4. **配置统一**: 统一的配置接口

通过继承NcmBaseStrategy，子类可以快速实现特定的NCM处理功能。

---

**相关文档**:
- [NcmConvertStrategy设计文档](NcmConvertStrategy.md)
- [NcmCacheTransStrategy设计文档](NcmCacheTransStrategy.md)
- [NcmLyricDownloadStrategy设计文档](NcmLyricDownloadStrategy.md)
- [PathSelectionComponent设计文档](../base/PathSelectionComponent.md)

**文档版本**: 1.0  
**最后更新**: 2026-01-27  
**维护者**: FileEditTools Team
