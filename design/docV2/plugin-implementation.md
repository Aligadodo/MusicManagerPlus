# 插件实现文档

## 概述

本文档详细描述了FileManager Plus系统中的插件实现，包括插件架构、已实现的插件列表、插件配置和使用方法。

## 插件架构

### 核心接口

#### IPlugin接口

所有插件必须实现`IPlugin`接口，该接口定义了插件的基本行为：

```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    PluginConfigDTO getDefaultConfig();
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
}
```

#### PluginRegistry

插件注册表负责管理所有已加载的插件：

```java
public class PluginRegistry {
    private static PluginRegistry instance;
    private final Map<String, IPlugin> plugins = new HashMap<>();
    
    public static synchronized PluginRegistry getInstance();
    public void registerPlugin(IPlugin plugin);
    public IPlugin getPlugin(String pluginId);
    public List<IPlugin> getAllPlugins();
    public void loadPlugins();
}
```

#### ExecutionContext

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

### 插件配置

插件配置使用`PluginConfigDTO`类来管理：

```java
public class PluginConfigDTO {
    private Map<String, Object> configValues;
    
    public void setValue(String key, Object value);
    public Object getValue(String key);
    public Object getValue(String key, Object defaultValue);
    public Map<String, Object> getConfigValues();
}
```

## 已实现的插件

### 1. 文件收集插件 (file-collection)

**插件ID**: `file-collection`

**描述**: 根据配置规则收集和整理文件

**默认配置**:
```json
{
  "targetDirectory": "/tmp/collected",
  "recursive": true,
  "includePatterns": ["*.mp3", "*.wav", "*.flac"],
  "excludePatterns": ["*.tmp", "*.log"]
}
```

**配置参数**:
- `targetDirectory`: 目标目录路径
- `recursive`: 是否递归扫描子目录
- `includePatterns`: 包含的文件模式列表
- `excludePatterns`: 排除的文件模式列表

**使用示例**:
```http
POST /api/plugins/file-collection/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "targetDirectory": "/path/to/target",
      "recursive": true
    }
  }
}
```

### 2. 文件清理插件 (file-cleanup)

**插件ID**: `file-cleanup`

**描述**: 根据配置规则清理不需要的文件

**默认配置**:
```json
{
  "maxFileAgeDays": 30,
  "minFileSizeKB": 0,
  "maxFileSizeKB": 10240,
  "deleteEmptyDirectories": true,
  "includePatterns": ["*.tmp", "*.log", "*.bak"]
}
```

**配置参数**:
- `maxFileAgeDays`: 最大文件年龄（天）
- `minFileSizeKB`: 最小文件大小（KB）
- `maxFileSizeKB`: 最大文件大小（KB）
- `deleteEmptyDirectories`: 是否删除空目录
- `includePatterns`: 包含的文件模式列表

**使用示例**:
```http
POST /api/plugins/file-cleanup/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.tmp", "/path/to/file2.log"],
  "config": {
    "values": {
      "maxFileAgeDays": 30,
      "deleteEmptyDirectories": true
    }
  }
}
```

### 3. 元数据抓取插件 (metadata-scraper)

**插件ID**: `metadata-scraper`

**描述**: 从网络或本地抓取并更新文件的元数据信息

**默认配置**:
```json
{
  "sources": ["discogs", "musicbrainz", "local"],
  "updateTags": true,
  "updateCoverArt": true,
  "forceUpdate": false
}
```

**配置参数**:
- `sources`: 数据源列表（discogs, musicbrainz, local）
- `updateTags`: 是否更新标签
- `updateCoverArt`: 是否更新封面艺术
- `forceUpdate`: 是否强制更新

**使用示例**:
```http
POST /api/plugins/metadata-scraper/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "sources": ["discogs", "musicbrainz"],
      "updateTags": true,
      "updateCoverArt": true
    }
  }
}
```

### 4. 音频转换插件 (audio-converter)

**插件ID**: `audio-converter`

**描述**: 将音频文件转换为不同格式

**默认配置**:
```json
{
  "targetFormat": "mp3",
  "bitrate": "320k",
  "sampleRate": 44100,
  "channels": 2,
  "outputDirectory": "",
  "overwriteExisting": false
}
```

**配置参数**:
- `targetFormat`: 目标格式（mp3, wav, flac, aac等）
- `bitrate`: 比特率（如320k, 256k, 128k）
- `sampleRate`: 采样率（如44100, 48000）
- `channels`: 声道数（1=单声道, 2=立体声）
- `outputDirectory`: 输出目录（空表示原文件所在目录）
- `overwriteExisting`: 是否覆盖已存在的文件

**使用示例**:
```http
POST /api/plugins/audio-converter/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.wav", "/path/to/file2.flac"],
  "config": {
    "values": {
      "targetFormat": "mp3",
      "bitrate": "320k",
      "sampleRate": 44100,
      "channels": 2,
      "outputDirectory": "/path/to/output"
    }
  }
}
```

### 5. 文件重命名插件 (file-rename)

**插件ID**: `file-rename`

**描述**: 根据规则批量重命名文件

**默认配置**:
```json
{
  "pattern": "{name}_{index}",
  "startIndex": 1,
  "padZeros": true,
  "zeroPadding": 3,
  "preserveExtension": true,
  "overwriteExisting": false
}
```

**配置参数**:
- `pattern`: 重命名模式（支持{name}和{index}占位符）
- `startIndex`: 起始索引
- `padZeros`: 是否用零填充索引
- `zeroPadding`: 零填充位数
- `preserveExtension`: 是否保留文件扩展名
- `overwriteExisting`: 是否覆盖已存在的文件

**使用示例**:
```http
POST /api/plugins/file-rename/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.txt", "/path/to/file2.txt"],
  "config": {
    "values": {
      "pattern": "{name}_{index}",
      "startIndex": 1,
      "padZeros": true,
      "zeroPadding": 3,
      "preserveExtension": true
    }
  }
}
```

## 插件开发指南

### 创建新插件

1. 创建新的插件模块目录结构：
```
plugins/your-plugin/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── filemanager/
                    └── plugin/
                        └── yourplugin/
                            └── YourPlugin.java
```

2. 实现IPlugin接口：
```java
package com.filemanager.plugin.yourplugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.List;

public class YourPlugin implements IPlugin {
    @Override
    public String getId() {
        return "your-plugin-id";
    }

    @Override
    public String getName() {
        return "你的插件名称";
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
        return config;
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

    private String getNewName(String filePath, PluginConfigDTO config) {
        return filePath;
    }
}
```

3. 创建pom.xml：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.filemanager</groupId>
        <artifactId>plugins</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>plugin-your-plugin</artifactId>
    <name>Your Plugin</name>
    <description>Your plugin description</description>

    <dependencies>
        <dependency>
            <groupId>com.filemanager</groupId>
            <artifactId>plugin-base</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.filemanager</groupId>
            <artifactId>domain</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

4. 在plugins/pom.xml中添加模块：
```xml
<modules>
    <module>base</module>
    <module>file-collection</module>
    <module>metadata-scraper</module>
    <module>file-cleanup</module>
    <module>audio-converter</module>
    <module>file-rename</module>
    <module>your-plugin</module>
</modules>
```

5. 构建插件：
```bash
cd plugins
mvn clean install
```

## 插件测试

### 测试插件配置

```bash
curl -X GET http://localhost:8080/api/plugins/file-collection/config
```

### 测试插件执行

```bash
curl -X POST http://localhost:8080/api/plugins/file-collection/execute \
  -H "Content-Type: application/json" \
  -d '{
    "files": ["/path/to/file1.mp3"],
    "config": {
      "values": {
        "targetDirectory": "/path/to/target"
      }
    }
  }'
```

## 插件管理API

### 获取所有插件

```http
GET /api/plugins
```

### 获取插件信息

```http
GET /api/plugins/{pluginId}
```

### 获取插件配置

```http
GET /api/plugins/{pluginId}/config
```

### 更新插件配置

```http
POST /api/plugins/{pluginId}/config
Content-Type: application/json

{
  "values": {
    "key1": "value1",
    "key2": "value2"
  }
}
```

### 执行插件

```http
POST /api/plugins/{pluginId}/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "key1": "value1"
    }
  }
}
```

### 重新加载插件

```http
POST /api/plugins/reload
```

## 插件最佳实践

1. **配置验证**: 在execute方法中验证配置参数的有效性
2. **错误处理**: 使用try-catch块处理可能的异常
3. **日志记录**: 使用适当的日志级别记录插件执行过程
4. **性能优化**: 对于大量文件，考虑使用批处理或并行处理
5. **资源清理**: 确保在插件执行完成后清理所有打开的资源
6. **文档完善**: 为插件提供清晰的配置说明和使用示例

## 总结

FileManager Plus的插件系统提供了灵活的扩展机制，允许开发者轻松添加新的文件处理功能。通过实现IPlugin接口并遵循插件开发指南，可以创建功能强大的插件来满足各种文件管理需求。
