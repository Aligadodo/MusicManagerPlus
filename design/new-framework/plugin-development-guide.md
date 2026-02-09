# FileManager Plus 插件开发指南

## 概述

本文档详细说明如何为FileManager Plus开发插件。插件系统是FileManager Plus的核心功能之一，允许开发者扩展系统的功能，添加自定义的文件处理策略。

## 一、插件系统架构

### 1.1 插件接口定义

插件必须实现`IPlugin`接口：

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

### 1.2 插件生命周期

```
加载 → 初始化 → 配置 → 执行 → 清理 → 卸载
```

### 1.3 插件类型

- **内部插件**: 随应用一起打包的插件
- **外部插件**: 独立打包为JAR文件，可动态加载的插件

## 二、插件开发步骤

### 2.1 创建插件项目

#### 2.1.1 Maven项目结构
```
my-plugin/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── plugin/
        │               └── MyPlugin.java
        └── resources/
            └── META-INF/
                └── services/
                    └── com.filemanager.plugin.api.IPlugin
```

#### 2.1.2 pom.xml配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-plugin</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.filemanager</groupId>
            <artifactId>plugin-api</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.2 实现插件接口

#### 2.2.1 基本插件实现
```java
package com.example.plugin;

import com.filemanager.plugin.api.*;
import com.filemanager.domain.dto.*;
import java.util.*;

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
        return "这是一个示例插件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("targetDirectory", "/tmp/output");
        configValues.put("recursive", true);
        config.setConfigValues(configValues);
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO targetDirParam = new PluginParameterDTO();
        targetDirParam.setName("targetDirectory");
        targetDirParam.setLabel("目标目录");
        targetDirParam.setType("directory");
        targetDirParam.setDefaultValue("/tmp/output");
        targetDirParam.setDescription("文件处理的目标目录");
        targetDirParam.setRequired(true);
        parameters.add(targetDirParam);
        
        PluginParameterDTO recursiveParam = new PluginParameterDTO();
        recursiveParam.setName("recursive");
        recursiveParam.setLabel("递归处理");
        recursiveParam.setType("boolean");
        recursiveParam.setDefaultValue(true);
        recursiveParam.setDescription("是否递归处理子目录");
        recursiveParam.setRequired(false);
        parameters.add(recursiveParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setLogicType("AND");
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        PreconditionDTO precondition = new PreconditionDTO();
        precondition.setId("exist-condition");
        precondition.setField("fileExists");
        precondition.setOperator(PreconditionDTO.OperatorType.EQUALS);
        precondition.setValue(true);
        precondition.setDescription("文件存在");
        preconditions.add(precondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        Map<String, Object> configValues = config.getConfigValues();
        String targetDirectory = (String) configValues.get("targetDirectory");
        boolean recursive = (Boolean) configValues.getOrDefault("recursive", false);
        
        int total = filePaths.size();
        for (int i = 0; i < total; i++) {
            String filePath = filePaths.get(i);
            
            try {
                context.reportProgress((i * 100) / total, "处理文件: " + filePath);
                context.log("INFO", "开始处理文件: " + filePath);
                
                ChangeRecord change = processFile(filePath, targetDirectory, recursive);
                changes.add(change);
                
                context.log("INFO", "文件处理完成: " + filePath);
            } catch (Exception e) {
                context.log("ERROR", "处理文件失败: " + filePath + ", 错误: " + e.getMessage());
                
                ChangeRecord change = new ChangeRecord();
                change.setId(UUID.randomUUID().toString());
                change.setOriginalName(filePath);
                change.setNewName(filePath);
                change.setFilePath(filePath);
                change.setChanged(false);
                change.setOperationType("PROCESS");
                change.setStatus("FAILED");
                change.setMessage(e.getMessage());
                changes.add(change);
            }
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        Map<String, Object> configValues = config.getConfigValues();
        String targetDirectory = (String) configValues.get("targetDirectory");
        boolean recursive = (Boolean) configValues.getOrDefault("recursive", false);
        
        for (String filePath : filePaths) {
            ChangeRecord change = previewFile(filePath, targetDirectory, recursive);
            changes.add(change);
        }
        
        return changes;
    }
    
    private ChangeRecord processFile(String filePath, String targetDirectory, boolean recursive) {
        ChangeRecord change = new ChangeRecord();
        change.setId(UUID.randomUUID().toString());
        change.setOriginalName(filePath);
        
        File file = new File(filePath);
        String newFilePath = targetDirectory + File.separator + file.getName();
        
        try {
            Files.move(Paths.get(filePath), Paths.get(newFilePath));
            change.setNewName(newFilePath);
            change.setChanged(true);
            change.setOperationType("MOVE");
            change.setStatus("SUCCESS");
            change.setMessage("文件移动成功");
        } catch (IOException e) {
            change.setNewName(filePath);
            change.setChanged(false);
            change.setOperationType("MOVE");
            change.setStatus("FAILED");
            change.setMessage(e.getMessage());
        }
        
        return change;
    }
    
    private ChangeRecord previewFile(String filePath, String targetDirectory, boolean recursive) {
        ChangeRecord change = new ChangeRecord();
        change.setId(UUID.randomUUID().toString());
        change.setOriginalName(filePath);
        
        File file = new File(filePath);
        String newFilePath = targetDirectory + File.separator + file.getName();
        
        change.setNewName(newFilePath);
        change.setChanged(true);
        change.setOperationType("MOVE");
        change.setStatus("PENDING");
        change.setMessage("预览模式，文件未被修改");
        
        return change;
    }
}
```

### 2.3 注册插件

#### 2.3.1 创建ServiceLoader配置文件
在`src/main/resources/META-INF/services/`目录下创建文件`com.filemanager.plugin.api.IPlugin`，内容为插件实现类的全限定名：

```
com.example.plugin.MyPlugin
```

### 2.4 构建插件

```bash
mvn clean package
```

构建完成后，会在`target/`目录下生成JAR文件：`my-plugin-1.0.0.jar`

## 三、插件配置

### 3.1 配置参数类型

插件支持以下配置参数类型：

| 类型 | 说明 | 示例 |
|------|------|------|
| `string` | 字符串 | `"Hello"` |
| `number` | 数字 | `123` |
| `boolean` | 布尔值 | `true` |
| `directory` | 目录路径 | `"/tmp/output"` |
| `file` | 文件路径 | `"/path/to/file.txt"` |
| `enum` | 枚举值 | `"option1"` |
| `list` | 列表 | `["item1", "item2"]` |
| `map` | 映射 | `{"key": "value"}` |

### 3.2 配置参数示例

```java
@Override
public List<PluginParameterDTO> getParameters() {
    List<PluginParameterDTO> parameters = new ArrayList<>();
    
    PluginParameterDTO stringParam = new PluginParameterDTO();
    stringParam.setName("prefix");
    stringParam.setLabel("前缀");
    stringParam.setType("string");
    stringParam.setDefaultValue("new_");
    stringParam.setDescription("添加到文件名前的前缀");
    stringParam.setRequired(false);
    parameters.add(stringParam);
    
    PluginParameterDTO numberParam = new PluginParameterDTO();
    numberParam.setName("maxSize");
    numberParam.setLabel("最大文件大小(MB)");
    numberParam.setType("number");
    numberParam.setDefaultValue(100);
    numberParam.setDescription("文件的最大大小限制");
    numberParam.setRequired(false);
    parameters.add(numberParam);
    
    PluginParameterDTO booleanParam = new PluginParameterDTO();
    booleanParam.setName("overwrite");
    booleanParam.setLabel("覆盖已存在文件");
    booleanParam.setType("boolean");
    booleanParam.setDefaultValue(false);
    booleanParam.setDescription("是否覆盖已存在的文件");
    booleanParam.setRequired(false);
    parameters.add(booleanParam);
    
    PluginParameterDTO enumParam = new PluginParameterDTO();
    enumParam.setName("mode");
    enumParam.setLabel("处理模式");
    enumParam.setType("enum");
    enumParam.setDefaultValue("move");
    enumParam.setDescription("文件处理模式");
    enumParam.setRequired(true);
    
    List<EnumOptionDTO> enumOptions = new ArrayList<>();
    enumOptions.add(new EnumOptionDTO("move", "移动"));
    enumOptions.add(new EnumOptionDTO("copy", "复制"));
    enumOptions.add(new EnumOptionDTO("delete", "删除"));
    enumParam.setEnumOptions(enumOptions);
    parameters.add(enumParam);
    
    return parameters;
}
```

### 3.3 前置条件配置

```java
@Override
public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
    List<PreconditionGroupDTO> groups = new ArrayList<>();
    
    PreconditionGroupDTO group = new PreconditionGroupDTO();
    group.setId("file-size-group");
    group.setName("文件大小条件组");
    group.setLogicType("AND");
    
    List<PreconditionDTO> preconditions = new ArrayList<>();
    
    PreconditionDTO sizeCondition = new PreconditionDTO();
    sizeCondition.setId("size-condition");
    sizeCondition.setField("fileSize");
    sizeCondition.setOperator(PreconditionDTO.OperatorType.GREATER_THAN);
    sizeCondition.setValue(1024 * 1024); // 1MB
    sizeCondition.setDescription("文件大小大于1MB");
    preconditions.add(sizeCondition);
    
    PreconditionDTO extensionCondition = new PreconditionDTO();
    extensionCondition.setId("extension-condition");
    extensionCondition.setField("fileExtension");
    extensionCondition.setOperator(PreconditionDTO.OperatorType.EQUALS);
    extensionCondition.setValue("mp3");
    extensionCondition.setDescription("文件扩展名为mp3");
    preconditions.add(extensionCondition);
    
    group.setPreconditions(preconditions);
    groups.add(group);
    
    return groups;
}
```

## 四、插件执行

### 4.1 执行上下文

`ExecutionContext`提供了插件执行时的环境信息：

```java
public class ExecutionContext {
    private final String taskId;
    private final ProgressCallback progressCallback;
    private final LogCallback logCallback;
    
    public void reportProgress(int progress, String message) {
        progressCallback.onProgress(taskId, progress, message);
    }
    
    public void log(String level, String message) {
        logCallback.onLog(taskId, level, message);
    }
    
    public String getTaskId() {
        return taskId;
    }
}
```

### 4.2 进度报告

```java
@Override
public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
    int total = filePaths.size();
    for (int i = 0; i < total; i++) {
        String filePath = filePaths.get(i);
        
        context.reportProgress((i * 100) / total, "处理文件: " + filePath);
        
        ChangeRecord change = processFile(filePath);
        changes.add(change);
    }
    
    return changes;
}
```

### 4.3 日志记录

```java
@Override
public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
    context.log("INFO", "开始处理文件列表，共 " + filePaths.size() + " 个文件");
    
    for (String filePath : filePaths) {
        try {
            context.log("INFO", "处理文件: " + filePath);
            
            ChangeRecord change = processFile(filePath);
            changes.add(change);
            
            context.log("INFO", "文件处理成功: " + filePath);
        } catch (Exception e) {
            context.log("ERROR", "处理文件失败: " + filePath + ", 错误: " + e.getMessage());
        }
    }
    
    context.log("INFO", "所有文件处理完成");
    
    return changes;
}
```

## 五、插件测试

### 5.1 单元测试

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyPluginTest {

    @Test
    void testGetId() {
        MyPlugin plugin = new MyPlugin();
        assertEquals("my-plugin", plugin.getId());
    }

    @Test
    void testGetName() {
        MyPlugin plugin = new MyPlugin();
        assertEquals("我的插件", plugin.getName());
    }

    @Test
    void testGetVersion() {
        MyPlugin plugin = new MyPlugin();
        assertEquals("1.0.0", plugin.getVersion());
    }

    @Test
    void testGetDefaultConfig() {
        MyPlugin plugin = new MyPlugin();
        PluginConfigDTO config = plugin.getDefaultConfig();
        
        assertNotNull(config);
        assertNotNull(config.getConfigValues());
        assertEquals("/tmp/output", config.getConfigValues().get("targetDirectory"));
        assertEquals(true, config.getConfigValues().get("recursive"));
    }

    @Test
    void testGetParameters() {
        MyPlugin plugin = new MyPlugin();
        List<PluginParameterDTO> parameters = plugin.getParameters();
        
        assertNotNull(parameters);
        assertFalse(parameters.isEmpty());
        
        PluginParameterDTO targetDirParam = parameters.stream()
            .filter(p -> "targetDirectory".equals(p.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(targetDirParam);
        assertEquals("目标目录", targetDirParam.getLabel());
        assertEquals("directory", targetDirParam.getType());
        assertEquals("/tmp/output", targetDirParam.getDefaultValue());
    }

    @Test
    void testPreview() {
        MyPlugin plugin = new MyPlugin();
        PluginConfigDTO config = plugin.getDefaultConfig();
        ExecutionContext context = new TestExecutionContext();
        
        List<String> filePaths = Arrays.asList("/tmp/test1.txt", "/tmp/test2.txt");
        List<ChangeRecord> changes = plugin.preview(filePaths, config, context);
        
        assertNotNull(changes);
        assertEquals(2, changes.size());
        
        for (ChangeRecord change : changes) {
            assertEquals("PENDING", change.getStatus());
            assertTrue(change.isChanged());
        }
    }
}
```

### 5.2 集成测试

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MyPluginIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testExecute() throws IOException {
        MyPlugin plugin = new MyPlugin();
        PluginConfigDTO config = plugin.getDefaultConfig();
        
        Path sourceDir = tempDir.resolve("source");
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);
        
        Path file1 = sourceDir.resolve("test1.txt");
        Files.write(file1, "content1".getBytes());
        
        Path file2 = sourceDir.resolve("test2.txt");
        Files.write(file2, "content2".getBytes());
        
        config.getConfigValues().put("targetDirectory", targetDir.toString());
        
        ExecutionContext context = new TestExecutionContext();
        List<String> filePaths = Arrays.asList(file1.toString(), file2.toString());
        
        List<ChangeRecord> changes = plugin.execute(filePaths, config, context);
        
        assertNotNull(changes);
        assertEquals(2, changes.size());
        
        for (ChangeRecord change : changes) {
            assertEquals("SUCCESS", change.getStatus());
            assertTrue(change.isChanged());
            assertFalse(Files.exists(Paths.get(change.getOriginalName())));
            assertTrue(Files.exists(Paths.get(change.getNewName())));
        }
    }
}
```

## 六、插件部署

### 6.1 内部插件部署

将插件的JAR文件放置在应用的`plugins/`目录下，重启应用即可自动加载。

### 6.2 外部插件部署

#### 6.2.1 通过API加载插件

```bash
curl -X POST http://localhost:8080/api/plugins/load-external \
  -H "Content-Type: application/json" \
  -d '{
    "pluginPath": "/path/to/my-plugin-1.0.0.jar"
  }'
```

#### 6.2.2 扫描插件目录

```bash
curl -X POST http://localhost:8080/api/plugins/scan \
  -H "Content-Type: application/json" \
  -d '{
    "directory": "/path/to/plugins"
  }'
```

## 七、最佳实践

### 7.1 错误处理

```java
@Override
public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
    List<ChangeRecord> changes = new ArrayList<>();
    
    for (String filePath : filePaths) {
        try {
            ChangeRecord change = processFile(filePath);
            changes.add(change);
        } catch (Exception e) {
            context.log("ERROR", "处理文件失败: " + filePath + ", 错误: " + e.getMessage());
            
            ChangeRecord errorChange = new ChangeRecord();
            errorChange.setId(UUID.randomUUID().toString());
            errorChange.setOriginalName(filePath);
            errorChange.setNewName(filePath);
            errorChange.setFilePath(filePath);
            errorChange.setChanged(false);
            errorChange.setOperationType("PROCESS");
            errorChange.setStatus("FAILED");
            errorChange.setMessage(e.getMessage());
            changes.add(errorChange);
        }
    }
    
    return changes;
}
```

### 7.2 性能优化

```java
@Override
public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
    List<ChangeRecord> changes = new ArrayList<>();
    
    int total = filePaths.size();
    int processed = 0;
    
    for (String filePath : filePaths) {
        try {
            ChangeRecord change = processFile(filePath);
            changes.add(change);
            
            processed++;
            if (processed % 10 == 0) {
                context.reportProgress((processed * 100) / total, "已处理 " + processed + "/" + total + " 个文件");
            }
        } catch (Exception e) {
            context.log("ERROR", "处理文件失败: " + filePath + ", 错误: " + e.getMessage());
        }
    }
    
    context.reportProgress(100, "所有文件处理完成");
    
    return changes;
}
```

### 7.3 配置验证

```java
@Override
public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
    Map<String, Object> configValues = config.getConfigValues();
    
    String targetDirectory = (String) configValues.get("targetDirectory");
    if (targetDirectory == null || targetDirectory.isEmpty()) {
        throw new IllegalArgumentException("目标目录不能为空");
    }
    
    File targetDir = new File(targetDirectory);
    if (!targetDir.exists()) {
        if (!targetDir.mkdirs()) {
            throw new IOException("无法创建目标目录: " + targetDirectory);
        }
    }
    
    List<ChangeRecord> changes = new ArrayList<>();
    
    for (String filePath : filePaths) {
        ChangeRecord change = processFile(filePath, targetDirectory);
        changes.add(change);
    }
    
    return changes;
}
```

### 7.4 资源清理

```java
@Override
public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
    List<ChangeRecord> changes = new ArrayList<>();
    List<Closeable> resources = new ArrayList<>();
    
    try {
        for (String filePath : filePaths) {
            InputStream inputStream = new FileInputStream(filePath);
            resources.add(inputStream);
            
            ChangeRecord change = processFile(inputStream, filePath);
            changes.add(change);
        }
    } finally {
        for (Closeable resource : resources) {
            try {
                resource.close();
            } catch (IOException e) {
                context.log("WARN", "关闭资源失败: " + e.getMessage());
            }
        }
    }
    
    return changes;
}
```

## 八、常见问题

### 8.1 插件无法加载

**问题**: 插件JAR文件已放置在`plugins/`目录下，但应用无法加载插件。

**解决方案**:
1. 检查`META-INF/services/com.filemanager.plugin.api.IPlugin`文件是否存在
2. 检查文件内容是否正确（插件实现类的全限定名）
3. 检查插件是否实现了`IPlugin`接口
4. 检查插件依赖是否正确配置

### 8.2 配置参数无法读取

**问题**: 插件无法读取配置参数。

**解决方案**:
1. 检查`getDefaultConfig()`方法是否正确返回配置
2. 检查`getParameters()`方法是否正确返回参数定义
3. 检查参数名称是否一致
4. 检查参数类型是否正确

### 8.3 执行结果不正确

**问题**: 插件执行结果不符合预期。

**解决方案**:
1. 检查`execute()`方法的实现逻辑
2. 检查`ChangeRecord`的设置是否正确
3. 检查文件路径是否正确
4. 检查文件权限是否足够

## 九、总结

本文档详细说明了如何为FileManager Plus开发插件，包括插件系统架构、开发步骤、配置、执行、测试、部署和最佳实践。通过遵循本文档的指导，开发者可以快速开发出高质量的插件，扩展FileManager Plus的功能。

---

**文档版本**: 1.0  
**创建日期**: 2026-02-08  
**维护者**: FileManager Plus Team
