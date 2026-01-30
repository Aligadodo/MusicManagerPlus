# 插件系统文档

## 概述

FileManager Plus插件系统提供了一个灵活可扩展的框架，用于向应用程序添加新功能。插件可用于实现自定义文件处理策略、与外部服务集成或向系统添加新功能。

## 架构

插件系统建立在Java的ServiceLoader机制之上，该机制提供了在运行时发现和加载服务的标准方法。这种方法允许在不修改核心应用程序代码的情况下添加插件。

### 关键组件

1. **IPlugin接口**：定义所有插件必须实现的标准方法
2. **PluginRegistry**：管理插件发现、注册和生命周期
3. **ExecutionContext**：为插件执行提供运行时上下文
4. **PluginConfigDTO**：表示插件配置数据

## 创建插件

要创建新插件，请按照以下步骤操作：

### 1. 实现IPlugin接口

```java
package com.filemanager.plugin.example;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.ExecutionContext;

import java.util.List;

public class ExamplePlugin implements IPlugin {

    @Override
    public String getId() {
        return "example-plugin";
    }

    @Override
    public String getName() {
        return "Example Plugin";
    }

    @Override
    public String getDescription() {
        return "演示插件系统的示例插件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("exampleSetting", "defaultValue");
        config.setValue("enabled", true);
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        // 插件执行逻辑
        // 返回表示所做更改的ChangeRecord对象列表
        return List.of();
    }
}
```

### 2. 注册插件

在`src/main/resources/META-INF/services/`中创建一个名为`com.filemanager.plugin.IPlugin`的服务提供者配置文件，并添加插件的完全限定类名：

```
com.filemanager.plugin.example.ExamplePlugin
```

### 3. 构建和部署

将插件构建为JAR文件，并将其放置在插件发现的适当目录中。确切位置取决于部署环境，但通常插件放在与主应用程序并排的`plugins`目录中。

## 插件生命周期

1. **发现**：在应用程序启动期间使用ServiceLoader发现插件
2. **注册**：发现的插件向PluginRegistry注册
3. **初始化**：使用默认配置初始化插件
4. **执行**：当通过API请求时执行插件
5. **重新加载**：可以使用`/plugins/reload`端点在运行时重新加载插件

## 插件配置

插件可以使用`PluginConfigDTO`类定义自己的配置选项。配置值存储在内存中，可以通过API更新。

### 示例配置

```java
PluginConfigDTO config = new PluginConfigDTO();
config.setValue("targetDirectory", "/path/to/target");
config.setValue("recursive", true);
config.setValue("filePattern", "*.mp3");
```

## 插件执行

执行插件时，它会接收：

1. **filePaths**：要处理的文件路径列表
2. **config**：插件的当前配置
3. **context**：提供运行时信息的执行上下文

插件返回表示它们对文件所做更改的`ChangeRecord`对象列表。

### 示例执行流程

1. 客户端发送执行插件的请求
2. PluginController接收请求
3. PluginServiceImpl从注册表中检索插件
4. PluginServiceImpl调用插件的execute方法
5. 插件处理文件并生成更改记录
6. PluginServiceImpl将更改记录返回给控制器
7. 控制器将更改记录返回给客户端

## 内置插件

FileManager Plus包含几个内置插件：

| 插件ID | 名称 | 描述 |
|--------|------|------|
| `file-collection` | 文件收集插件 | 根据配置收集和组织文件 |
| `metadata-scraper` | 元数据抓取插件 | 从外部源抓取和更新文件元数据 |
| `file-cleanup` | 文件清理插件 | 根据年龄、大小和其他标准清理文件 |

## 与新功能集成

插件系统与添加到FileManager Plus的新功能无缝集成：

### 源目录管理

插件可以配置为使用特定的源目录。SourceDirectoryController管理插件处理的目录，允许用户：

- 定义要扫描的目录
- 配置并行处理的线程数
- 管理目录生命周期（添加、删除、清除）

### 流水线管理

插件可以在流水线中链接在一起，用于复杂的文件处理工作流。PipelineController允许用户：

- 按顺序组合多个插件
- 独立配置每个插件
- 执行前预览更改
- 在源目录上执行整个流水线

流水线配置示例：
```json
[
  {
    "strategyId": "file-collection",
    "name": "文件收集",
    "config": {
      "targetDirectory": "/organized",
      "recursive": true
    }
  },
  {
    "strategyId": "metadata-scraper",
    "name": "元数据更新",
    "config": {
      "source": "musicbrainz",
      "overwrite": false
    }
  }
]
```

### 线程池管理

插件可以利用线程池配置进行并行处理。ThreadPoolController允许用户：

- 配置分析操作的预览线程数
- 配置实际文件操作的执行线程数
- 根据系统资源优化性能

线程池配置示例：
```json
{
  "previewThreads": 8,
  "executionThreads": 16
}
```

## 扩展插件系统

插件系统可以通过多种方式扩展：

### 1. 自定义插件接口

创建专门的插件接口，扩展`IPlugin`以获取特定类型的功能：

```java
public interface MetadataPlugin extends IPlugin {
    List<MetadataField> getSupportedFields();
    void updateMetadata(String filePath, Map<MetadataField, Object> metadata);
}
```

### 2. 插件依赖

插件可以依赖其他插件或外部库。构建带有依赖项的插件时，确保所有必需的库都包含在插件JAR中或在类路径中可用。

### 3. 插件配置UI

在客户端应用程序中为插件创建自定义配置UI。Flutter Web客户端可以根据插件配置模式动态生成配置表单。

## 最佳实践

### 插件开发

1. **保持插件专注**：每个插件应实现单个、定义明确的功能
2. **优雅处理错误**：插件应在内部捕获和处理异常
3. **提供清晰的文档**：包括插件配置选项和使用的文档
4. **彻底测试**：使用各种文件类型和配置测试插件
5. **使用日志记录**：记录重要事件和错误以便调试

### 插件安全

1. **验证输入**：始终验证文件路径和配置值
2. **限制文件系统访问**：仅访问明确提供的文件
3. **避免系统命令**：使用Java API而不是执行系统命令
4. **注意资源**：清理资源以避免泄漏
5. **尊重用户偏好**：遵守配置设置和用户选择

## 故障排除

### 常见问题

1. **插件未被发现**：检查服务提供者配置文件是否正确命名和定位
2. **插件加载失败**：检查缺少的依赖项或运行时错误
3. **插件执行错误**：检查插件日志和错误消息
4. **配置未保存**：确保配置更新得到正确处理

### 调试提示

1. **启用调试日志**：为插件相关类设置日志级别为DEBUG
2. **使用重新加载端点**：测试插件更改而无需重新启动应用程序
3. **检查插件注册表**：验证插件是否正确注册
4. **使用示例文件测试**：使用小测试文件隔离问题

## 结论

插件系统是FileManager Plus的强大功能，允许广泛的定制和扩展。通过遵循本文档中概述的指南和最佳实践，您可以创建强大的插件，增强应用程序的功能并为用户提供价值。