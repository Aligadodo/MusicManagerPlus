# 插件系统架构设计文档

## 概述

本文档描述了FileManager Plus的插件系统架构，包括插件的设计原则、实现方式、加载机制和扩展能力。

## 架构设计

### 设计目标

1. **模块化**：每个插件都是独立的模块，可以单独开发、测试和部署
2. **可扩展**：支持动态加载外部插件，无需重新编译主程序
3. **易维护**：相关插件可以合并到同一个包中，减少包的数量
4. **前后端分离**：插件定义参数DTO，前端页面一一对应参数配置页面
5. **独立管理**：每个插件独立管理参数页面的展示、加载、保存、数据处理结果预览、数据处理执行等环节

### 核心组件

#### 1. 插件接口 (IPlugin)

所有插件必须实现`IPlugin`接口：

```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    PluginConfigDTO getDefaultConfig();
    List<PluginParameterDTO> getParameters();
    List<PreconditionGroupDTO> getDefaultPreconditionGroups();
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
}
```

#### 2. 插件注册表 (PluginRegistry)

插件注册表负责管理所有已加载的插件：

```java
public class PluginRegistry {
    private static final PluginRegistry INSTANCE = new PluginRegistry();
    private final Map<String, IPlugin> plugins = new HashMap<>();
    private final PluginLoader pluginLoader = new PluginLoader();
    private String externalPluginDir;

    public static PluginRegistry getInstance();
    public void loadExternalPlugins(String pluginDirPath);
    public void reloadPlugins();
    public void reloadExternalPlugins();
    public List<String> scanExternalPluginDirectory(String pluginDirPath);
    public IPlugin getPlugin(String pluginId);
    public List<IPlugin> getAvailablePlugins();
    public List<IPlugin> getInternalPlugins();
    public List<IPlugin> getExternalPlugins();
    public boolean registerPlugin(IPlugin plugin);
    public boolean unregisterPlugin(String pluginId);
}
```

#### 3. 插件加载器 (PluginLoader)

插件加载器支持从外部JAR文件加载插件：

```java
public class PluginLoader {
    public List<IPlugin> loadPluginsFromDirectory(String pluginDirPath);
    public List<IPlugin> loadPluginFromJar(File jarFile);
    public boolean isPluginJar(File jarFile);
    public List<String> scanPluginDirectory(String pluginDirPath);
    public void unloadExternalPlugins();
    public List<IPlugin> getExternalPlugins();
    public void reloadExternalPlugins(String pluginDirPath);
}
```

#### 4. 执行上下文 (ExecutionContext)

执行上下文提供插件执行时的环境信息：

```java
public class ExecutionContext {
    private String taskId;
    private String userId;
    private Map<String, Object> contextData;

    public void setContextData(String key, Object value);
    public Object getContextData(String key);
}
```

### 插件分类

#### 内置插件

内置插件是随主程序一起发布的插件，位于`plugins/file-operations`模块中：

- **FileCleanupPlugin**: 文件清理插件，支持文件去重、文件夹去重、空目录清理、文件夹合并等
- **FileCollectionPlugin**: 文件收集插件，根据配置规则收集和整理文件
- **FileRenamePlugin**: 文件重命名插件，根据规则批量重命名文件
- **AudioConverterPlugin**: 音频转换插件，将音频文件转换为不同格式
- **MetadataScraperPlugin**: 元数据抓取插件，从网络或本地抓取并更新文件的元数据信息

#### 外部插件

外部插件是用户或第三方开发者开发的插件，可以动态加载到系统中。外部插件需要：

1. 打包为独立的JAR文件
2. 包含`META-INF/services/com.filemanager.plugin.IPlugin`文件
3. 实现IPlugin接口
4. 可以放置在任意目录中，通过API加载

### 插件包结构

#### 内置插件包结构

```
plugins/
├── base/                           # 插件基础接口
│   ├── src/main/java/com/filemanager/plugin/
│   │   ├── IPlugin.java
│   │   ├── PluginRegistry.java
│   │   ├── PluginLoader.java
│   │   └── ExecutionContext.java
│   └── pom.xml
└── file-operations/                 # 文件操作插件包
    ├── src/main/java/com/filemanager/plugin/operations/
    │   ├── FileCleanupPlugin.java
    │   ├── FileCollectionPlugin.java
    │   ├── FileRenamePlugin.java
    │   ├── AudioConverterPlugin.java
    │   └── MetadataScraperPlugin.java
    ├── src/main/resources/META-INF/services/
    │   └── com.filemanager.plugin.IPlugin
    └── pom.xml
```

#### 外部插件包结构

```
my-plugin.jar
├── META-INF/
│   ├── MANIFEST.MF
│   └── services/
│       └── com.filemanager.plugin.IPlugin
└── com/
    └── example/
        └── plugin/
            └── MyPlugin.java
```

### 插件配置

#### 插件配置DTO

插件配置使用`PluginConfigDTO`类来管理：

```java
public class PluginConfigDTO {
    private Map<String, Object> configValues;
    private List<PluginParameterDTO> parameters;
    private List<PreconditionGroupDTO> preconditionGroups;

    public void setValue(String key, Object value);
    public Object getValue(String key);
    public Object getValue(String key, Object defaultValue);
    public Map<String, Object> getConfigValues();
}
```

#### 插件参数DTO

插件参数使用`PluginParameterDTO`类来描述：

```java
public class PluginParameterDTO {
    private String name;
    private String label;
    private String description;
    private String type;
    private Object defaultValue;
    private boolean required;
    private String[] options;

    public PluginParameterDTO(String name, String label, String description, 
                          String type, Object defaultValue, boolean required);
}
```

#### 前置条件组DTO

前置条件组使用`PreconditionGroupDTO`类来描述：

```java
public class PreconditionGroupDTO {
    private String id;
    private String name;
    private String description;
    private LogicType logicType;
    private List<PreconditionDTO> preconditions;
}
```

### 插件生命周期

1. **加载阶段**：
   - 内置插件通过Java SPI机制自动加载
   - 外部插件通过`PluginLoader`从JAR文件加载

2. **配置阶段**：
   - 前端页面显示插件参数配置界面
   - 用户配置参数并保存

3. **预览阶段**：
   - 调用`preview()`方法预览变更
   - 显示变更记录，用户确认

4. **执行阶段**：
   - 调用`execute()`方法执行插件
   - 返回变更记录

5. **卸载阶段**：
   - 外部插件可以通过`unloadExternalPlugins()`卸载

### 插件开发指南

#### 创建内置插件

1. 在`plugins/file-operations`模块中创建插件类
2. 实现`IPlugin`接口
3. 在`META-INF/services/com.filemanager.plugin.IPlugin`文件中注册插件

#### 创建外部插件

1. 创建新的Maven项目
2. 添加对`plugin-base`和`domain`模块的依赖
3. 实现`IPlugin`接口
4. 创建`META-INF/services/com.filemanager.plugin.IPlugin`文件
5. 打包为JAR文件

#### 插件开发示例

```java
package com.example.plugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.List;

public class MyPlugin implements IPlugin {
    @Override
    public String getId() {
        return "my-plugin";
    }

    @Override
    public String getName() {
        return "我的插件";
    }

    @Override
    public String getDescription() {
        return "插件描述";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("key1", "default-value1");
        config.setValue("key2", "default-value2");
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO param = new PluginParameterDTO(
            "key1",
            "参数1",
            "参数描述",
            "text",
            "default-value1",
            true
        );
        parameters.add(param);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getNewName(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.CUSTOM);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getNewName(String filePath, PluginConfigDTO config) {
        return filePath;
    }
}
```

### 插件API

#### 获取所有插件

```http
GET /api/plugins
```

#### 获取内置插件

```http
GET /api/plugins/internal
```

#### 获取外部插件

```http
GET /api/plugins/external
```

#### 扫描外部插件目录

```http
POST /api/plugins/scan
Content-Type: application/json

{
  "pluginDir": "/path/to/plugins"
}
```

#### 加载外部插件

```http
POST /api/plugins/load-external
Content-Type: application/json

{
  "pluginDir": "/path/to/plugins"
}
```

#### 重载外部插件

```http
POST /api/plugins/reload-external
```

#### 重载所有插件

```http
POST /api/plugins/reload
```

### 插件管理

#### 插件目录

外部插件可以放置在任意目录中，建议使用以下目录：

- Windows: `%APPDATA%/FileManagerPlus/plugins`
- macOS: `~/Library/Application Support/FileManagerPlus/plugins`
- Linux: `~/.local/share/FileManagerPlus/plugins`

#### 插件热加载

系统支持插件的热加载，无需重启应用：

1. 将新的插件JAR文件放入插件目录
2. 调用`/api/plugins/reload-external` API
3. 插件自动加载并可用

#### 插件卸载

外部插件可以通过以下方式卸载：

1. 从插件目录删除插件JAR文件
2. 调用`/api/plugins/reload-external` API
3. 插件自动卸载

### 插件最佳实践

1. **配置验证**：在execute方法中验证配置参数的有效性
2. **错误处理**：使用try-catch块处理可能的异常
3. **日志记录**：使用适当的日志级别记录插件执行过程
4. **性能优化**：对于大量文件，考虑使用批处理或并行处理
5. **资源清理**：确保在插件执行完成后清理所有打开的资源
6. **文档完善**：为插件提供清晰的配置说明和使用示例
7. **版本管理**：使用语义化版本号（如1.0.0）
8. **依赖管理**：尽量减少外部依赖，使用系统提供的工具

### 插件安全性

1. **沙箱隔离**：外部插件在独立的类加载器中运行
2. **权限控制**：插件只能访问指定的文件路径
3. **资源限制**：限制插件的CPU和内存使用
4. **签名验证**：支持插件签名验证，确保插件来源可信

### 总结

FileManager Plus的插件系统提供了灵活的扩展机制，允许开发者轻松添加新的文件处理功能。通过实现IPlugin接口并遵循插件开发指南，可以创建功能强大的插件来满足各种文件管理需求。

系统支持内置插件和外部插件两种方式，内置插件随主程序发布，外部插件可以动态加载，提供了极大的灵活性和可扩展性。