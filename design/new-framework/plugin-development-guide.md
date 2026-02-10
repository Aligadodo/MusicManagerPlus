# FileManager Plus 策略开发指南

## 概述

本文档详细说明如何为FileManager Plus开发策略。策略系统是FileManager Plus的核心功能之一，允许开发者扩展系统的功能，添加自定义的文件处理策略。

## 一、策略系统架构

### 1.1 策略接口定义

策略必须实现`IPlugin`接口，并继承`AbstractConfigurableStrategy`类：

```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    List<PluginParameterDTO> getParameters();
    PluginConfigDTO getDefaultConfig();
    List<PreconditionGroupDTO> getDefaultPreconditionGroups();
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
}

public interface StrategyConfigurable extends IPlugin {
    List<ConfigFieldDTO> getConfigFields();
    StrategyConfigDTO initializeDefaultConfig();
    boolean validateConfig(StrategyConfigDTO config);
    <T> T getConfigValue(StrategyConfigDTO config, String key, T defaultValue);
    void setConfigValue(StrategyConfigDTO config, String key, Object value);
}
```

### 1.2 策略生命周期
```
加载 → 初始化 → 配置 → 执行 → 清理 → 卸载
```

### 1.3 策略类型
- **内部策略**: 位于backend/src/main/java/com/filemanager/plugin/impl/目录下，随应用一起打包
- **外部策略**: 独立打包为JAR文件，可动态加载的策略

## 二、策略开发步骤

### 2.1 创建策略项目

#### 2.1.1 项目结构
```
backend/src/main/java/com/filemanager/plugin/impl/mystrategy/
├── MyStrategy.java
└── enums/
    └── MyEnum.java
```
### 2.2 实现策略接口

#### 2.2.1 基本策略实现
```java
package com.filemanager.plugin.impl.mystrategy;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyStrategy extends AbstractConfigurableStrategy {

    public MyStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "my-strategy";
    }

    @Override
    public String getName() {
        return "我的策略";
    }

    @Override
    public String getDescription() {
        return "这是一个示例策略";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addConfigField("prefix", "前缀", "string", "new_", 
            "添加到文件名前的前缀", false);
        addConfigField("suffix", "后缀", "string", "_processed", 
            "添加到文件名后缀的后缀", false);
        addConfigField("overwrite", "覆盖已存在文件", "boolean", false, 
            "是否覆盖已存在的文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "prefix", "new_");
        setConfigValue(config, "suffix", "_processed");
        setConfigValue(config, "overwrite", false);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String prefix = getConfigValue(config, "prefix", "new_");
        String suffix = getConfigValue(config, "suffix", "_processed");
        
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        String newFileName = prefix + fileName + suffix;
        String newFilePath = sourceFile.getParent() + File.separator + newFileName;
        
        ChangeRecord record = createChangeRecord(filePath, newFilePath, "PENDING");
        record.setOperationType("RENAME");
        record.setReason("应用重命名规则");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String prefix = getConfigValue(config, "prefix", "new_");
        String suffix = getConfigValue(config, "suffix", "_processed");
        boolean overwrite = getConfigValue(config, "overwrite", false);
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String fileName = sourceFile.getName();
            String newFileName = prefix + fileName + suffix;
            File targetFile = new File(sourceFile.getParent(), newFileName);
            
            if (targetFile.exists() && !overwrite) {
                context.logWarn("Target file already exists: " + newFileName);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            sourceFile.renameTo(targetFile);
            
            context.logInfo("Renamed file: " + filePath + " -> " + targetFile.getPath());
            ChangeRecord record = createChangeRecord(filePath, targetFile.getPath(), "SUCCESS");
            record.setOperationType("RENAME");
            record.setReason("应用重命名规则");
            return record;
        } catch (Exception e) {
            context.logError("Error renaming file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }
}
```

### 2.3 注册策略

#### 2.3.1 创建ServiceLoader配置文件
在`backend/src/main/resources/META-INF/services/`目录下创建文件`com.filemanager.plugin.IPlugin`，内容为策略实现类的全限定名：

```
com.filemanager.plugin.impl.mystrategy.MyStrategy
```

## 三、策略配置

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

## 六、策略部署

### 6.1 内部策略部署

将策略类放置在backend/src/main/java/com/filemanager/plugin/impl/目录下，并在backend/src/main/resources/META-INF/services/com.filemanager.plugin.IPlugin文件中注册策略类的全限定名，重启应用即可自动加载。

### 6.2 外部策略部署

#### 6.2.1 通过API加载策略

```bash
curl -X POST http://localhost:8080/api/plugins/load-external \
  -H "Content-Type: application/json" \
  -d '{
    "pluginPath": "/path/to/my-strategy-1.0.0.jar"
  }'
```

#### 6.2.2 扫描策略目录

```bash
curl -X POST http://localhost:8080/api/plugins/scan \
  -H "Content-Type: application/json" \
  -d '{
    "directory": "/path/to/strategies"
  }'
```

### 6.3 外部策略开发

开发外部策略需要创建独立的Maven项目，包含以下步骤：

1. 创建Maven项目
2. 添加对FileManager Plus的依赖
3. 实现策略类（继承AbstractConfigurableStrategy）
4. 创建META-INF/services/com.filemanager.plugin.IPlugin文件
5. 打包为JAR文件

#### 6.3.1 Maven项目配置

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-strategy</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.filemanager</groupId>
            <artifactId>shared-domain</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <Implementation-Title>${project.name}</Implementation-Title>
                            <Implementation-Version>${project.version}</Implementation-Version>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 6.3.2 策略实现

```java
package com.example.strategy;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyExternalStrategy extends AbstractConfigurableStrategy {

    public MyExternalStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "my-external-strategy";
    }

    @Override
    public String getName() {
        return "我的外部策略";
    }

    @Override
    public String getDescription() {
        return "这是一个外部策略示例";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addConfigField("targetDirectory", "目标目录", "directory", "/tmp/output", 
            "文件处理的目标目录", true);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetDirectory", "/tmp/output");
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetDir = getConfigValue(config, "targetDirectory", "/tmp/output");
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String targetPath = targetDir + "/" + fileName;

        ChangeRecord record = createChangeRecord(filePath, targetPath, "PENDING");
        record.setOperationType("COPY");
        record.setReason("复制文件到目标目录");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetDir = getConfigValue(config, "targetDirectory", "/tmp/output");
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        try {
            String fileName = sourceFile.getName();
            File targetDirFile = new File(targetDir);
            
            if (!targetDirFile.exists()) {
                targetDirFile.mkdirs();
            }
            
            String targetPath = targetDir + "/" + fileName;
            File targetFile = new File(targetPath);
            
            if (targetFile.exists()) {
                context.logWarn("Target file already exists: " + targetPath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            java.nio.file.Files.copy(
                java.nio.file.Paths.get(filePath), 
                java.nio.file.Paths.get(targetPath),
                java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
            );
            
            context.logInfo("Copied file: " + filePath + " -> " + targetPath);
            ChangeRecord record = createChangeRecord(filePath, targetPath, "SUCCESS");
            record.setOperationType("COPY");
            record.setReason("复制文件到目标目录");
            return record;
        } catch (Exception e) {
            context.logError("Error copying file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }
}
```

#### 6.3.3 注册策略

在`src/main/resources/META-INF/services/com.filemanager.plugin.IPlugin`文件中添加策略类的全限定名：

```
com.example.strategy.MyExternalStrategy
```

#### 6.3.4 打包和部署

```bash
# 打包策略
mvn clean package

# 将JAR文件复制到策略目录
cp target/my-strategy-1.0.0.jar /path/to/strategies/

# 通过API加载策略
curl -X POST http://localhost:8080/api/plugins/load-external \
  -H "Content-Type: application/json" \
  -d '{"pluginPath": "/path/to/strategies/my-strategy-1.0.0.jar"}'
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

### 8.1 策略无法加载

**问题**: 策略JAR文件已放置在策略目录下，但应用无法加载策略。

**解决方案**:
1. 检查`META-INF/services/com.filemanager.plugin.IPlugin`文件是否存在
2. 检查文件内容是否正确（策略实现类的全限定名）
3. 检查策略是否实现了`IPlugin`接口
4. 检查策略依赖是否正确配置

### 8.2 配置参数无法读取

**问题**: 策略无法读取配置参数。

**解决方案**:
1. 检查`initConfigFields()`方法是否正确定义了配置字段
2. 检查`initDefaultConfigValues()`方法是否正确设置了默认值
3. 检查参数名称是否一致
4. 检查参数类型是否正确

### 8.3 执行结果不正确

**问题**: 策略执行结果不符合预期。

**解决方案**:
1. 检查`executeForFile()`方法的实现逻辑
2. 检查`ChangeRecord`的设置是否正确
3. 检查文件路径是否正确
4. 检查文件权限是否足够

## 九、总结

本文档详细说明了如何为FileManager Plus开发策略，包括策略系统架构、开发步骤、配置、执行、测试、部署和最佳实践。通过遵循本文档的指导，开发者可以快速开发出高质量的策略，扩展FileManager Plus的功能。

**关键要点**:
- 系统采用统一的插件-策略架构，所有策略类都实现了IPlugin接口
- 策略通过继承AbstractConfigurableStrategy类实现，简化了开发流程
- 支持内部策略和外部策略两种部署方式
- 外部策略可以通过ServiceLoader机制动态加载
- PluginRegistry统一管理所有策略的加载和注册

---

**文档版本**: 2.0  
**创建日期**: 2026-02-08  
**更新日期**: 2026-02-10  
**维护者**: FileManager Plus Team
