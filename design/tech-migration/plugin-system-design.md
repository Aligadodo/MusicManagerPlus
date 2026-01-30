# 业务插件模块解耦设计

## 1. 插件系统概述

### 1.1 设计目标
- **解耦业务逻辑**：将具体的文件处理策略与核心框架分离
- **可扩展性**：支持动态加载和卸载插件
- **标准化接口**：统一插件开发规范
- **配置管理**：插件配置的标准化管理
- **并行支持**：与新老客户端架构兼容

### 1.2 核心概念
- **插件**：实现特定文件处理功能的模块
- **插件接口**：定义插件的标准行为
- **插件注册表**：管理和发现插件
- **执行上下文**：提供插件执行所需的环境和服务

## 2. 插件系统架构

### 2.1 模块结构

```
plugins/
├── base/                  # 插件基础定义
│   ├── src/main/java/com/filemanager/plugin/
│   │   ├── IPlugin.java              # 核心插件接口
│   │   ├── PluginRegistry.java       # 插件注册表
│   │   ├── PluginContext.java        # 插件执行上下文
│   │   ├── PluginConfigDTO.java      # 插件配置DTO
│   │   ├── PluginInfoDTO.java        # 插件信息DTO
│   │   └── spi/                      # SPI 相关
│   │       └── PluginProvider.java   # 插件提供者接口
│   └── pom.xml
├── file-collection/       # 文件收集插件
│   ├── src/main/java/com/filemanager/plugin/collection/
│   │   ├── FileCollectionPlugin.java
│   │   ├── FileCollectionConfig.java
│   │   └── ...
│   ├── src/main/resources/META-INF/services/
│   │   └── com.filemanager.plugin.spi.PluginProvider
│   └── pom.xml
├── metadata-scraper/      # 元数据抓取插件
│   ├── src/main/java/com/filemanager/plugin/scraper/
│   │   ├── MetadataScraperPlugin.java
│   │   ├── MetadataScraperConfig.java
│   │   └── ...
│   ├── src/main/resources/META-INF/services/
│   │   └── com.filemanager.plugin.spi.PluginProvider
│   └── pom.xml
├── file-cleanup/          # 文件清理插件
│   ├── ...
└── pom.xml                # 父模块配置
```

## 3. 核心接口设计

### 3.1 IPlugin 接口

```java
package com.filemanager.plugin;

import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginInfoDTO;

import java.util.List;
import java.util.Map;

/**
 * 插件核心接口
 */
public interface IPlugin {
    
    /**
     * 获取插件唯一标识符
     */
    String getId();
    
    /**
     * 获取插件名称
     */
    String getName();
    
    /**
     * 获取插件描述
     */
    String getDescription();
    
    /**
     * 获取插件版本
     */
    String getVersion();
    
    /**
     * 获取默认配置
     */
    PluginConfigDTO getDefaultConfig();
    
    /**
     * 获取插件信息
     */
    PluginInfoDTO getInfo();
    
    /**
     * 分析文件并生成变更记录
     * @param filePaths 文件路径列表
     * @param config 插件配置
     * @param context 执行上下文
     * @return 变更记录列表
     */
    List<ChangeRecord> analyze(List<String> filePaths, PluginConfigDTO config, PluginContext context);
    
    /**
     * 执行变更
     * @param records 变更记录列表
     * @param context 执行上下文
     */
    void execute(List<ChangeRecord> records, PluginContext context);
    
    /**
     * 验证配置
     * @param config 插件配置
     * @return 验证结果，空Map表示验证通过
     */
    Map<String, String> validateConfig(PluginConfigDTO config);
}
```

### 3.2 PluginRegistry 类

```java
package com.filemanager.plugin;

import com.filemanager.plugin.spi.PluginProvider;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 插件注册表
 */
public class PluginRegistry {
    
    private static PluginRegistry instance;
    private final Map<String, IPlugin> plugins = new java.util.concurrent.ConcurrentHashMap<>();
    
    private PluginRegistry() {
        loadPlugins();
    }
    
    public static synchronized PluginRegistry getInstance() {
        if (instance == null) {
            instance = new PluginRegistry();
        }
        return instance;
    }
    
    /**
     * 加载插件
     */
    public void loadPlugins() {
        ServiceLoader<PluginProvider> loader = ServiceLoader.load(PluginProvider.class);
        for (PluginProvider provider : loader) {
            IPlugin plugin = provider.createPlugin();
            plugins.put(plugin.getId(), plugin);
        }
    }
    
    /**
     * 获取所有插件
     */
    public List<IPlugin> getPlugins() {
        return List.copyOf(plugins.values());
    }
    
    /**
     * 根据ID获取插件
     */
    public IPlugin getPlugin(String id) {
        return plugins.get(id);
    }
    
    /**
     * 注册插件
     */
    public void registerPlugin(IPlugin plugin) {
        plugins.put(plugin.getId(), plugin);
    }
    
    /**
     * 卸载插件
     */
    public void unregisterPlugin(String id) {
        plugins.remove(id);
    }
}
```

### 3.3 PluginContext 类

```java
package com.filemanager.plugin;

import com.filemanager.domain.service.FileService;
import com.filemanager.domain.service.LogService;

/**
 * 插件执行上下文
 */
public class PluginContext {
    
    private final FileService fileService;
    private final LogService logService;
    private final java.util.Map<String, Object> attributes = new java.util.concurrent.ConcurrentHashMap<>();
    
    public PluginContext(FileService fileService, LogService logService) {
        this.fileService = fileService;
        this.logService = logService;
    }
    
    public FileService getFileService() {
        return fileService;
    }
    
    public LogService getLogService() {
        return logService;
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public void removeAttribute(String key) {
        attributes.remove(key);
    }
}
```

### 3.4 PluginProvider 接口 (SPI)

```java
package com.filemanager.plugin.spi;

import com.filemanager.plugin.IPlugin;

/**
 * 插件提供者接口（SPI）
 */
public interface PluginProvider {
    
    /**
     * 创建插件实例
     */
    IPlugin createPlugin();
}
```

## 3. 插件配置管理

### 3.1 配置结构

```java
package com.filemanager.domain.dto;

import java.util.List;
import java.util.Map;

/**
 * 插件配置DTO
 */
public class PluginConfigDTO {
    
    private String pluginId;
    private Map<String, Object> configData;
    private List<ConfigFieldDTO> configFields;
    
    // getters and setters
}

/**
 * 配置字段DTO
 */
public class ConfigFieldDTO {
    
    private String name;
    private String label;
    private String type; // text, number, boolean, select, etc.
    private Object defaultValue;
    private List<Object> options; // for select type
    private boolean required;
    private String description;
    
    // getters and setters
}
```

### 3.2 配置存储
- **文件存储**：JSON/YAML 配置文件
- **数据库存储**：可选，用于多用户场景
- **配置版本控制**：支持配置历史和回滚

### 3.3 配置验证
- **插件自身验证**：插件实现 validateConfig 方法
- **框架验证**：类型检查、必填项检查
- **前端验证**：基于配置元数据的表单验证

## 4. 插件实现示例

### 4.1 文件收集插件

```java
package com.filemanager.plugin.collection;

import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.PluginContext;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.type.OperationType;

import java.util.List;
import java.util.Map;

public class FileCollectionPlugin implements IPlugin {
    
    @Override
    public String getId() {
        return "file-collection";
    }
    
    @Override
    public String getName() {
        return "文件收集";
    }
    
    @Override
    public String getDescription() {
        return "根据规则收集和整理文件";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public PluginConfigDTO getDefaultConfig() {
        // 返回默认配置
        FileCollectionConfig config = new FileCollectionConfig();
        return config.toDTO();
    }
    
    @Override
    public List<ChangeRecord> analyze(List<String> filePaths, PluginConfigDTO config, PluginContext context) {
        // 分析文件并生成变更记录
        FileCollectionConfig collectionConfig = FileCollectionConfig.fromDTO(config);
        // 实现分析逻辑
        return new java.util.ArrayList<>();
    }
    
    @Override
    public void execute(List<ChangeRecord> records, PluginContext context) {
        // 执行文件收集操作
        for (ChangeRecord record : records) {
            // 实现执行逻辑
        }
    }
    
    @Override
    public Map<String, String> validateConfig(PluginConfigDTO config) {
        // 验证配置
        return java.util.Collections.emptyMap();
    }
}
```

### 4.2 SPI 注册

**META-INF/services/com.filemanager.plugin.spi.PluginProvider**:
```
com.filemanager.plugin.collection.FileCollectionPluginProvider
```

**FileCollectionPluginProvider.java**:
```java
package com.filemanager.plugin.collection;

import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.spi.PluginProvider;

public class FileCollectionPluginProvider implements PluginProvider {
    
    @Override
    public IPlugin createPlugin() {
        return new FileCollectionPlugin();
    }
}
```

## 5. 插件与服务端集成

### 5.1 服务端插件管理

```java
package com.filemanager.backend.service.impl;

import com.filemanager.plugin.PluginRegistry;
import com.filemanager.plugin.IPlugin;
import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.service.PluginService;

import java.util.List;
import java.util.stream.Collectors;

public class PluginServiceImpl implements PluginService {
    
    private final PluginRegistry pluginRegistry;
    
    public PluginServiceImpl() {
        this.pluginRegistry = PluginRegistry.getInstance();
    }
    
    @Override
    public List<PluginInfoDTO> getAvailablePlugins() {
        return pluginRegistry.getPlugins().stream()
                .map(IPlugin::getInfo)
                .collect(Collectors.toList());
    }
    
    @Override
    public PluginInfoDTO getPluginInfo(String pluginId) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        return plugin != null ? plugin.getInfo() : null;
    }
    
    @Override
    public void reloadPlugins() {
        pluginRegistry.loadPlugins();
    }
}
```

### 5.2 插件执行服务

```java
package com.filemanager.backend.service.impl;

import com.filemanager.plugin.PluginRegistry;
import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.PluginContext;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.FileService;
import com.filemanager.domain.service.LogService;

import java.util.List;

public class PluginExecutionServiceImpl implements PluginExecutionService {
    
    private final PluginRegistry pluginRegistry;
    private final FileService fileService;
    private final LogService logService;
    
    public PluginExecutionServiceImpl(FileService fileService, LogService logService) {
        this.pluginRegistry = PluginRegistry.getInstance();
        this.fileService = fileService;
        this.logService = logService;
    }
    
    @Override
    public List<ChangeRecord> executePlugin(String pluginId, List<String> filePaths, PluginConfigDTO config) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }
        
        PluginContext context = new PluginContext(fileService, logService);
        return plugin.analyze(filePaths, config, context);
    }
    
    @Override
    public void executeChanges(List<ChangeRecord> records) {
        // 按插件分组执行
        Map<String, List<ChangeRecord>> recordsByPlugin = groupRecordsByPlugin(records);
        
        PluginContext context = new PluginContext(fileService, logService);
        for (Map.Entry<String, List<ChangeRecord>> entry : recordsByPlugin.entrySet()) {
            String pluginId = entry.getKey();
            IPlugin plugin = pluginRegistry.getPlugin(pluginId);
            if (plugin != null) {
                plugin.execute(entry.getValue(), context);
            }
        }
    }
    
    private Map<String, List<ChangeRecord>> groupRecordsByPlugin(List<ChangeRecord> records) {
        // 实现分组逻辑
        return new java.util.HashMap<>();
    }
}
```

## 5. 前端集成

### 5.1 Flutter Web 插件配置界面

```dart
// lib/pages/plugin_config.dart

class PluginConfigPage extends StatefulWidget {
  final String pluginId;
  
  const PluginConfigPage({Key? key, required this.pluginId}) : super(key: key);
  
  @override
  _PluginConfigPageState createState() => _PluginConfigPageState();
}

class _PluginConfigPageState extends State<PluginConfigPage> {
  late Future<PluginConfigDTO> _configFuture;
  
  @override
  void initState() {
    super.initState();
    _configFuture = _fetchPluginConfig();
  }
  
  Future<PluginConfigDTO> _fetchPluginConfig() async {
    // 从API获取插件配置
    final apiClient = ApiClient();
    return apiClient.getPluginConfig(widget.pluginId);
  }
  
  @override
  Widget build(BuildContext context) {
    return FutureBuilder<PluginConfigDTO>(
      future: _configFuture,
      builder: (context, snapshot) {
        if (snapshot.hasData) {
          return _buildConfigForm(snapshot.data!);
        } else if (snapshot.hasError) {
          return Text('Error: ${snapshot.error}');
        }
        return CircularProgressIndicator();
      },
    );
  }
  
  Widget _buildConfigForm(PluginConfigDTO config) {
    // 基于配置元数据构建表单
    return ListView.builder(
      itemCount: config.configFields.length,
      itemBuilder: (context, index) {
        final field = config.configFields[index];
        return _buildConfigField(field);
      },
    );
  }
  
  Widget _buildConfigField(ConfigFieldDTO field) {
    // 根据字段类型构建不同的表单控件
    switch (field.type) {
      case 'text':
        return TextField(
          decoration: InputDecoration(labelText: field.label),
        );
      case 'boolean':
        return SwitchListTile(
          title: Text(field.label),
          value: field.defaultValue as bool,
          onChanged: (value) {},
        );
      // 其他类型...
      default:
        return Text(field.label);
    }
  }
}
```

### 5.2 JavaFX 插件配置界面

```java
// javafx-cli/src/main/java/com/filemanager/client/javafx/view/PluginConfigView.java

public class PluginConfigView extends VBox {
    
    public PluginConfigView(String pluginId, ApiClient apiClient) {
        // 初始化界面
        setPadding(new Insets(20));
        setSpacing(15);
        
        // 获取插件配置
        PluginConfigDTO config = apiClient.getPluginConfig(pluginId);
        
        // 构建配置表单
        buildConfigForm(config);
    }
    
    private void buildConfigForm(PluginConfigDTO config) {
        for (ConfigFieldDTO field : config.getConfigFields()) {
            Node fieldNode = createFieldControl(field);
            if (fieldNode != null) {
                getChildren().add(fieldNode);
            }
        }
    }
    
    private Node createFieldControl(ConfigFieldDTO field) {
        // 根据字段类型创建不同的控件
        switch (field.getType()) {
            case "text":
                return createTextField(field);
            case "boolean":
                return createCheckBox(field);
            // 其他类型...
            default:
                return null;
        }
    }
    
    private Node createTextField(ConfigFieldDTO field) {
        // 创建文本输入框
        TextField textField = new TextField();
        textField.setPromptText(field.getLabel());
        if (field.getDefaultValue() != null) {
            textField.setText(field.getDefaultValue().toString());
        }
        return new HBox(10, new Label(field.getLabel()), textField);
    }
    
    private Node createCheckBox(ConfigFieldDTO field) {
        // 创建复选框
        CheckBox checkBox = new CheckBox(field.getLabel());
        if (field.getDefaultValue() != null) {
            checkBox.setSelected((Boolean) field.getDefaultValue());
        }
        return checkBox;
    }
}
```

## 6. 插件系统优势

### 6.1 技术优势
- **松耦合**：插件与核心框架完全分离
- **热插拔**：支持运行时加载新插件
- **标准化**：统一的插件开发规范
- **可测试性**：插件可独立测试

### 6.2 业务优势
- **功能扩展**：快速添加新的文件处理功能
- **定制化**：根据需求定制特定功能
- **维护性**：插件问题不影响核心系统
- **生态系统**：鼓励第三方插件开发

### 6.3 迁移优势
- **平滑过渡**：与现有策略系统兼容
- **并行运行**：新老插件系统可同时存在
- **逐步迁移**：现有策略可逐步迁移为插件

## 7. 实施路径

### 7.1 阶段一：基础架构搭建
1. 实现插件基础接口和注册表
2. 构建 SPI 加载机制
3. 开发插件配置管理系统

### 7.2 阶段二：核心插件迁移
1. 将现有策略迁移为插件
2. 实现插件执行服务
3. 集成到服务端 API

### 7.3 阶段三：客户端集成
1. 实现 JavaFX 插件配置界面
2. 实现 Flutter Web 插件配置界面
3. 测试多端插件使用

### 7.4 阶段四：优化与扩展
1. 性能优化
2. 安全性增强
3. 插件市场搭建（可选）

## 8. 注意事项

### 8.1 安全性
- **插件权限控制**：限制插件的文件系统访问范围
- **代码签名**：验证插件的真实性
- **沙箱隔离**：运行插件在安全的环境中

### 8.2 性能
- **插件加载优化**：延迟加载非核心插件
- **执行效率**：避免插件执行阻塞主线程
- **内存管理**：防止插件内存泄漏

### 8.3 兼容性
- **版本管理**：插件与框架版本兼容性检查
- **向后兼容**：支持旧版本插件
- **API 稳定性**：保持插件接口的稳定性

### 8.4 文档
- **开发文档**：插件开发指南
- **API 文档**：插件接口文档
- **示例代码**：插件开发示例

## 9. 结论

插件系统的引入将显著提升 FileManager Plus 的可扩展性和可维护性。通过标准化的插件接口和动态加载机制，实现了业务逻辑与核心框架的解耦，为后续的功能扩展和技术架构升级奠定了基础。

同时，插件系统与新的 Flutter Web + 后端 API 架构完美结合，支持多端并行访问，为用户提供更灵活、更强大的文件管理工具。

---

**设计文档**：业务插件模块解耦设计  
**版本**：1.0  
**日期**：2026-01-30  
**适用范围**：FileManager Plus 项目技术迁移