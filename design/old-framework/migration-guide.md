# 功能迁移指南

## 概述

本文档提供了从老架构（JavaFX）到新架构（Flutter Web + Spring Boot）的功能迁移指南，包括详细的迁移步骤、代码示例和最佳实践。

## 迁移原则

### 1. 核心原则

- ✅ **复用核心算法**：最大化复用老架构的核心算法和业务逻辑
- ✅ **适配新架构**：将核心逻辑适配到新的插件架构
- ✅ **保持功能一致**：确保迁移后的功能与老架构一致
- ✅ **优化用户体验**：利用新架构的优势优化用户体验

### 2. 迁移策略

| 策略类型 | 迁移方式 | 复用程度 |
|---------|----------|----------|
| 文件管理策略 | 插件化迁移 | 高 |
| 音频处理策略 | 插件化迁移 | 高 |
| 元数据处理策略 | 插件化迁移 | 高 |
| NCM相关策略 | 插件化迁移 | 高 |
| UI组件 | Flutter重写 | 低 |
| 配置管理 | JSON适配 | 中 |

## 迁移步骤

### 通用迁移步骤

#### 步骤1：创建插件类

```java
package com.filemanager.plugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
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
        config.setValue("param1", "defaultValue");
        config.setValue("param2", true);
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        // 执行插件逻辑
        return List.of();
    }
}
```

#### 步骤2：注册插件

在`src/main/resources/META-INF/services/com.filemanager.plugin.IPlugin`中添加：

```
com.filemanager.plugin.MyPlugin
```

#### 步骤3：创建REST API端点

```java
package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plugins/my-plugin")
public class MyPluginController {

    @PostMapping("/analyze")
    public ResponseEntity<List<ChangeRecord>> analyze(
            @RequestBody AnalysisRequest request) {
        // 分析逻辑
        return ResponseEntity.ok(results);
    }

    @PostMapping("/execute")
    public ResponseEntity<List<ChangeRecord>> execute(
            @RequestBody ExecutionRequest request) {
        // 执行逻辑
        return ResponseEntity.ok(results);
    }
}
```

#### 步骤4：创建Flutter UI

```dart
class MyPluginConfigPage extends StatefulWidget {
  @override
  _MyPluginConfigPageState createState() => _MyPluginConfigPageState();
}

class _MyPluginConfigPageState extends State<MyPluginConfigPage> {
  String param1 = 'defaultValue';
  bool param2 = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('我的插件配置')),
      body: ListView(
        children: [
          TextField(
            decoration: InputDecoration(labelText: '参数1'),
            onChanged: (value) => param1 = value,
          ),
          SwitchListTile(
            title: Text('参数2'),
            value: param2,
            onChanged: (value) => setState(() => param2 = value),
          ),
        ],
      ),
    );
  }
}
```

## 具体策略迁移

### 1. 文件归类策略迁移

#### 步骤1：创建FileCollectionPlugin

```java
package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;
import com.filemanager.strategy.collection.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class FileCollectionPlugin implements IPlugin {

    private FilenameNormalizer filenameNormalizer;
    private TextSimilarityCalculator similarityCalculator;
    private FileClusteringAlgorithm clusteringAlgorithm;
    private CollectionDeterminationAlgorithm determinationAlgorithm;

    public FileCollectionPlugin() {
        filenameNormalizer = new FilenameNormalizer();
        similarityCalculator = new TextSimilarityCalculator();
        clusteringAlgorithm = new FileClusteringAlgorithm();
        determinationAlgorithm = new CollectionDeterminationAlgorithm();
    }

    @Override
    public String getId() {
        return "file-collection";
    }

    @Override
    public String getName() {
        return "文件智能归类";
    }

    @Override
    public String getDescription() {
        return "基于文件名相似度和特征将文件/文件夹归类到系列合集文件夹中";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("threshold", 0.9);
        config.setValue("collectionSuffix", "【合集】");
        config.setValue("targetType", "FOLDERS_ONLY");
        config.setValue("namingStrategy", "PRECISE");
        config.setValue("mustContainKeywords", List.of("CD", "系列", "合集"));
        config.setValue("mustNotContainKeywords", List.of("下载", "Album", "群星"));
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        double threshold = config.getDoubleValue("threshold");
        String collectionSuffix = config.getStringValue("collectionSuffix");
        String targetType = config.getStringValue("targetType");
        String namingStrategy = config.getStringValue("namingStrategy");
        List<String> mustContainKeywords = config.getListValue("mustContainKeywords");
        List<String> mustNotContainKeywords = config.getListValue("mustNotContainKeywords");

        // 复用老架构的核心算法
        List<ChangeRecord> results = new ArrayList<>();

        // 1. 标准化文件名
        List<NormalizedFolder> normalizedFolders = new ArrayList<>();
        for (String path : filePaths) {
            NormalizedFolder normalized = new NormalizedFolder(path);
            normalized.setNormalizedName(filenameNormalizer.normalize(normalized.getOriginalName()));
            normalizedFolders.add(normalized);
        }

        // 2. 计算相似度矩阵
        double[][] similarityMatrix = new double[normalizedFolders.size()][normalizedFolders.size()];
        for (int i = 0; i < normalizedFolders.size(); i++) {
            for (int j = 0; j < normalizedFolders.size(); j++) {
                similarityMatrix[i][j] = similarityCalculator.calculateSimilarity(
                    normalizedFolders.get(i).getNormalizedName(),
                    normalizedFolders.get(j).getNormalizedName()
                );
            }
        }

        // 3. 基于相似度进行聚类
        List<Cluster> clusters = clusteringAlgorithm.clusterFolders(normalizedFolders, similarityMatrix, threshold);

        // 4. 生成合集名称
        for (Cluster cluster : clusters) {
            String collectionName = determinationAlgorithm.generateCollectionName(
                cluster, namingStrategy, collectionSuffix
            );

            // 5. 创建ChangeRecord
            for (NormalizedFolder folder : cluster.getFolders()) {
                ChangeRecord record = new ChangeRecord();
                record.setOriginalName(folder.getOriginalName());
                record.setNewName(folder.getOriginalName());
                record.setFilePath(folder.getOriginalPath());
                record.setNewPath(collectionName + File.separator + folder.getOriginalName());
                record.setOperationType("COLLECT");
                record.setChanged(true);
                results.add(record);
            }
        }

        return results;
    }
}
```

#### 步骤2：创建REST API端点

```java
package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plugins/file-collection")
public class FileCollectionController {

    @Autowired
    private PluginService pluginService;

    @PostMapping("/analyze")
    public ResponseEntity<List<ChangeRecord>> analyze(
            @RequestBody AnalysisRequest request) {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("threshold", request.getThreshold());
        config.setValue("collectionSuffix", request.getCollectionSuffix());
        config.setValue("targetType", request.getTargetType());
        config.setValue("namingStrategy", request.getNamingStrategy());
        config.setValue("mustContainKeywords", request.getMustContainKeywords());
        config.setValue("mustNotContainKeywords", request.getMustNotContainKeywords());

        List<ChangeRecord> results = pluginService.executePlugin(
            "file-collection",
            request.getFilePaths(),
            config,
            null
        );

        return ResponseEntity.ok(results);
    }

    @PostMapping("/execute")
    public ResponseEntity<List<ChangeRecord>> execute(
            @RequestBody ExecutionRequest request) {
        // 执行逻辑与analyze相同
        return analyze(request);
    }
}
```

#### 步骤3：创建Flutter UI

```dart
class FileCollectionConfigPage extends StatefulWidget {
  @override
  _FileCollectionConfigPageState createState() => _FileCollectionConfigPageState();
}

class _FileCollectionConfigPageState extends State<FileCollectionConfigPage> {
  double _threshold = 0.9;
  String _collectionSuffix = '【合集】';
  String _targetType = 'FOLDERS_ONLY';
  String _namingStrategy = 'PRECISE';
  List<String> _mustContainKeywords = ['CD', '系列', '合集'];
  List<String> _mustNotContainKeywords = ['下载', 'Album', '群星'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('文件智能归类配置')),
      body: ListView(
        children: [
          // 相似度阈值
          Slider(
            value: _threshold,
            min: 0.0,
            max: 1.0,
            divisions: 20,
            label: _threshold.toStringAsFixed(2),
            onChanged: (value) => setState(() => _threshold = value),
          ),
          Text('相似度阈值: ${_threshold.toStringAsFixed(2)}'),

          // 合集文件夹格式
          TextField(
            decoration: InputDecoration(labelText: '合集文件夹格式'),
            controller: TextEditingController(text: _collectionSuffix),
            onChanged: (value) => _collectionSuffix = value,
          ),

          // 目标类型
          DropdownButton<String>(
            value: _targetType,
            items: [
              DropdownMenuItem(value: 'FILES_ONLY', child: Text('仅文件')),
              DropdownMenuItem(value: 'FOLDERS_ONLY', child: Text('仅文件夹')),
              DropdownMenuItem(value: 'ALL', child: Text('全部')),
            ],
            onChanged: (value) => setState(() => _targetType = value),
          ),

          // 命名策略
          DropdownButton<String>(
            value: _namingStrategy,
            items: [
              DropdownMenuItem(value: 'PRECISE', child: Text('精确')),
              DropdownMenuItem(value: 'CONCISE', child: Text('简洁')),
              DropdownMenuItem(value: 'TEMPLATE', child: Text('模板')),
              DropdownMenuItem(value: 'UNIVERSAL', child: Text('通用')),
            ],
            onChanged: (value) => setState(() => _namingStrategy = value),
          ),

          // 必须包含关键词
          TextField(
            decoration: InputDecoration(labelText: '必须包含关键词（逗号分隔）'),
            controller: TextEditingController(text: _mustContainKeywords.join(',')),
            onChanged: (value) => _mustContainKeywords = value.split(','),
          ),

          // 不能包含关键词
          TextField(
            decoration: InputDecoration(labelText: '不能包含关键词（逗号分隔）'),
            controller: TextEditingController(text: _mustNotContainKeywords.join(',')),
            onChanged: (value) => _mustNotContainKeywords = value.split(','),
          ),

          // 保存按钮
          ElevatedButton(
            onPressed: () => _saveConfig(),
            child: Text('保存配置'),
          ),
        ],
      ),
    );
  }

  void _saveConfig() {
    Map<String, dynamic> config = {
      'threshold': _threshold,
      'collectionSuffix': _collectionSuffix,
      'targetType': _targetType,
      'namingStrategy': _namingStrategy,
      'mustContainKeywords': _mustContainKeywords,
      'mustNotContainKeywords': _mustNotContainKeywords,
    };
    // 保存配置到后端
    // ...
  }
}
```

### 2. 元数据抓取策略迁移

#### 步骤1：创建MetadataScraperPlugin

```java
package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;
import com.filemanager.strategy.scraper.*;
import com.filemanager.strategy.scraper.source.MetadataSource;
import com.filemanager.strategy.scraper.source.impl.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class MetadataScraperPlugin implements IPlugin {

    private final Map<String, MetadataSource> sources;
    private final MetadataCacheManager cacheManager;
    private final MetadataScraperProcessor processor;

    public MetadataScraperPlugin() {
        sources = new LinkedHashMap<>();
        sources.put("local_inference", new LocalInferenceSource());
        sources.put("netease", new NeteaseMusicSource());
        sources.put("migu", new MiguMusicSource());
        sources.put("musicbrainz", new MusicBrainzSource());
        sources.put("itunes", new ITunesSource());
        sources.put("lastfm", new LastFmSource());
        sources.put("discogs", new DiscogsSource());

        cacheManager = new MetadataCacheManager(true);
        processor = new MetadataScraperProcessor(cacheManager);
    }

    @Override
    public String getId() {
        return "metadata-scraper";
    }

    @Override
    public String getName() {
        return "音频元数据自动刮削";
    }

    @Override
    public String getDescription() {
        return "一站式补全：音频Tag、歌词、专辑封面图(jpg)及专辑简介文档(txt)。支持自动生成曲目列表。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("source", "musicbrainz");
        config.setValue("threads", 4);
        config.setValue("lyricsConfig", Map.of(
            "enabled", true,
            "overwrite", false,
            "format", "lrc"
        ));
        config.setValue("coverConfig", Map.of(
            "enabled", true,
            "overwrite", false,
            "maxSize", 1200
        ));
        config.setValue("albumInfoConfig", Map.of(
            "enabled", true,
            "overwrite", false,
            "fields", List.of("artist", "album", "year", "genre")
        ));
        config.setValue("rateLimiterConfig", Map.of(
            "maxRequests", 10,
            "periodMs", 1000
        ));
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        String source = config.getStringValue("source");
        int threads = config.getIntValue("threads");
        Map<String, Object> lyricsConfig = config.getMapValue("lyricsConfig");
        Map<String, Object> coverConfig = config.getMapValue("coverConfig");
        Map<String, Object> albumInfoConfig = config.getMapValue("albumInfoConfig");
        Map<String, Object> rateLimiterConfig = config.getMapValue("rateLimiterConfig");

        // 复用老架构的核心代码
        MetadataSource metadataSource = sources.get(source);
        if (metadataSource == null) {
            throw new IllegalArgumentException("Unknown metadata source: " + source);
        }

        List<ChangeRecord> results = new ArrayList<>();

        for (String filePath : filePaths) {
            try {
                // 1. 提取文件元数据
                ScrapedResult scrapedResult = processor.scrapeMetadata(
                    filePath,
                    metadataSource,
                    threads,
                    lyricsConfig,
                    coverConfig,
                    albumInfoConfig,
                    rateLimiterConfig
                );

                // 2. 创建ChangeRecord
                ChangeRecord record = new ChangeRecord();
                record.setOriginalName(new File(filePath).getName());
                record.setNewName(new File(filePath).getName());
                record.setFilePath(filePath);
                record.setNewPath(filePath);
                record.setOperationType("SCRAPER");
                record.setChanged(scrapedResult.hasChanges());
                results.add(record);

            } catch (Exception e) {
                // 处理异常
                context.logError("Failed to scrape metadata for " + filePath + ": " + e.getMessage());
            }
        }

        return results;
    }
}
```

### 3. 文件清理策略迁移

#### 步骤1：创建FileCleanupPlugin

```java
package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;
import com.filemanager.strategy.duplicate.*;
import com.filemanager.tool.file.DeleteExecutor;
import com.filemanager.tool.file.DuplicateAnalyzer;
import com.filemanager.model.CleanupParams;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class FileCleanupPlugin implements IPlugin {

    @Override
    public String getId() {
        return "file-cleanup";
    }

    @Override
    public String getName() {
        return "文件清理与去重";
    }

    @Override
    public String getDescription() {
        return "智能识别重复文件/文件夹、清理空目录、合并同名父子文件夹。支持按盘符结构伪删除。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("cleanupMode", "FILES_ONLY");
        config.setValue("deleteMethod", "ARCHIVE");
        config.setValue("fileSizeRange", Map.of("min", 0, "max", 1073741824));
        config.setValue("targetType", "FILES_ONLY");
        config.setValue("duplicateStrategy", "KEEP_LARGEST");
        config.setValue("audioSpecial", true);
        config.setValue("keepExtensions", List.of("flac", "mp3"));
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        String cleanupMode = config.getStringValue("cleanupMode");
        String deleteMethod = config.getStringValue("deleteMethod");
        Map<String, Object> fileSizeRange = config.getMapValue("fileSizeRange");
        String targetType = config.getStringValue("targetType");
        String duplicateStrategy = config.getStringValue("duplicateStrategy");
        boolean audioSpecial = config.getBooleanValue("audioSpecial");
        List<String> keepExtensions = config.getListValue("keepExtensions");

        // 复用老架构的核心代码
        CleanupParams params = new CleanupParams();
        params.setCleanupMode(cleanupMode);
        params.setDeleteMethod(deleteMethod);
        params.setFileSizeRange(fileSizeRange);
        params.setTargetType(targetType);
        params.setDuplicateStrategy(duplicateStrategy);
        params.setAudioSpecial(audioSpecial);
        params.setKeepExtensions(keepExtensions);

        DuplicateAnalyzer analyzer = new DuplicateAnalyzer(params);
        DuplicateStrategyManager strategyManager = DuplicateStrategyManager.createDefaultManager(
            duplicateStrategy.equals("KEEP_LARGEST"),
            duplicateStrategy.equals("KEEP_EARLIEST"),
            audioSpecial,
            keepExtensions
        );

        List<ChangeRecord> results = new ArrayList<>();

        for (String filePath : filePaths) {
            try {
                // 1. 分析文件
                List<ChangeRecord> analysisResults = analyzer.analyze(new File(filePath));

                // 2. 添加到结果
                results.addAll(analysisResults);

            } catch (Exception e) {
                // 处理异常
                context.logError("Failed to analyze " + filePath + ": " + e.getMessage());
            }
        }

        return results;
    }
}
```

## 配置迁移

### Properties到JSON转换

#### 老架构配置格式（Properties）

```properties
# FileCollectionStrategy配置
threshold=0.9
collectionSuffix=【合集】
targetType=FOLDERS_ONLY
namingStrategy=PRECISE
mustContainKeywords=CD,系列,合集
mustNotContainKeywords=下载,Album,群星
```

#### 新架构配置格式（JSON）

```json
{
  "threshold": 0.9,
  "collectionSuffix": "【合集】",
  "targetType": "FOLDERS_ONLY",
  "namingStrategy": "PRECISE",
  "mustContainKeywords": ["CD", "系列", "合集"],
  "mustNotContainKeywords": ["下载", "Album", "群星"]
}
```

#### 配置转换工具

```java
package com.filemanager.util;

import java.io.*;
import java.util.*;

public class ConfigConverter {

    public static String convertPropertiesToJson(Properties props) {
        Map<String, Object> jsonMap = new HashMap<>();

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);

            // 处理不同类型的值
            if (value.contains(",")) {
                // 转换为数组
                String[] values = value.split(",");
                jsonMap.put(key, Arrays.asList(values));
            } else if (value.equals("true") || value.equals("false")) {
                // 转换为布尔值
                jsonMap.put(key, Boolean.parseBoolean(value));
            } else if (value.matches("\\d+")) {
                // 转换为整数
                jsonMap.put(key, Integer.parseInt(value));
            } else if (value.matches("\\d+\\.\\d+")) {
                // 转换为浮点数
                jsonMap.put(key, Double.parseDouble(value));
            } else {
                // 保持为字符串
                jsonMap.put(key, value);
            }
        }

        // 转换为JSON字符串
        return JSON.toJSONString(jsonMap, true);
    }

    public static Properties convertJsonToProperties(String json) {
        Map<String, Object> jsonMap = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
        Properties props = new Properties();

        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            Object value = entry.getValue();

            // 处理不同类型的值
            if (value instanceof List) {
                // 转换为逗号分隔的字符串
                List<?> list = (List<?>) value;
                props.setProperty(entry.getKey(), String.join(",", list.stream().map(Object::toString).toArray(String[]::new)));
            } else {
                // 转换为字符串
                props.setProperty(entry.getKey(), value.toString());
            }
        }

        return props;
    }
}
```

## UI提示信息迁移

### 老架构的FloatingTooltip

```java
// 老架构中的提示信息
ArrayList<String> tooltipLines = new ArrayList<>();
tooltipLines.add("参数名称：相似度阈值");
tooltipLines.add("参数用途：用于设置文件归类时的相似度阈值");
tooltipLines.add("取值范围：0.0 - 1.0");
tooltipLines.add("说明：");
tooltipLines.add("- 0.8：宽松归类，可能产生误归类");
tooltipLines.add("- 0.9：推荐设置，平衡准确性和召回率");
tooltipLines.add("- 0.95：严格归类，可能遗漏相似文件");

FloatingTooltip.bindToNode(slSimilarityThreshold, "文件归类设置", tooltipLines);
```

### 新架构的Flutter Tooltip

```dart
// 新架构中的提示信息
class ParameterTooltip extends StatelessWidget {
  final String paramName;
  final String paramPurpose;
  final String valueRange;
  final List<String> descriptions;

  const ParameterTooltip({
    required this.paramName,
    required this.paramPurpose,
    required this.valueRange,
    required this.descriptions,
  });

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: _buildTooltipText(),
      child: Icon(Icons.info_outline),
    );
  }

  String _buildTooltipText() {
    StringBuffer sb = StringBuffer();
    sb.writeln('参数名称：$paramName');
    sb.writeln('参数用途：$paramPurpose');
    sb.writeln('取值范围：$valueRange');
    sb.writeln('说明：');
    for (String desc in descriptions) {
      sb.writeln(desc);
    }
    return sb.toString();
  }
}

// 使用示例
Row(
  children: [
    Text('相似度阈值'),
    SizedBox(width: 8),
    ParameterTooltip(
      paramName: '相似度阈值',
      paramPurpose: '用于设置文件归类时的相似度阈值',
      valueRange: '0.0 - 1.0',
      descriptions: [
        '- 0.8：宽松归类，可能产生误归类',
        '- 0.9：推荐设置，平衡准确性和召回率',
        '- 0.95：严格归类，可能遗漏相似文件',
      ],
    ),
  ],
),
```

## 测试迁移

### 单元测试迁移

#### 老架构单元测试

```java
@Test
public void testFileClustering() {
    FileClusteringAlgorithm algorithm = new FileClusteringAlgorithm();
    List<String> folders = Arrays.asList(
        "专辑1-CD1",
        "专辑1-CD2",
        "专辑2-CD1",
        "专辑3"
    );

    List<Collection> collections = algorithm.generateCollections(folders);

    assertEquals(3, collections.size());
}
```

#### 新架构单元测试

```java
@Test
public void testFileCollectionPlugin() {
    FileCollectionPlugin plugin = new FileCollectionPlugin();
    PluginConfigDTO config = plugin.getDefaultConfig();

    List<String> filePaths = Arrays.asList(
        "/path/to/专辑1-CD1",
        "/path/to/专辑1-CD2",
        "/path/to/专辑2-CD1",
        "/path/to/专辑3"
    );

    List<ChangeRecord> results = plugin.execute(filePaths, config, null);

    assertNotNull(results);
    assertTrue(results.size() > 0);
}
```

## 迁移检查清单

### 文件归类策略迁移检查清单

- [ ] 创建FileCollectionPlugin类
- [ ] 实现IPlugin接口
- [ ] 复用FilenameNormalizer
- [ ] 复用TextSimilarityCalculator
- [ ] 复用FileClusteringAlgorithm
- [ ] 复用CollectionDeterminationAlgorithm
- [ ] 创建REST API端点
- [ ] 创建Flutter UI配置界面
- [ ] 实现配置参数序列化/反序列化
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 迁移UI提示信息
- [ ] 验证功能一致性

### 元数据抓取策略迁移检查清单

- [ ] 创建MetadataScraperPlugin类
- [ ] 实现IPlugin接口
- [ ] 复用所有MetadataSource实现
- [ ] 复用MetadataScraperProcessor
- [ ] 复用MetadataCacheManager
- [ ] 复用RateLimiter
- [ ] 创建REST API端点
- [ ] 创建Flutter UI配置界面
- [ ] 实现配置参数序列化/反序列化
- [ ] 集成WebSocket进度推送
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 迁移UI提示信息
- [ ] 验证功能一致性

### 文件清理策略迁移检查清单

- [ ] 创建FileCleanupPlugin类
- [ ] 实现IPlugin接口
- [ ] 复用DuplicateAnalyzer
- [ ] 复用DeleteExecutor
- [ ] 复用DuplicateStrategyManager
- [ ] 创建REST API端点
- [ ] 创建Flutter UI配置界面
- [ ] 实现配置参数序列化/反序列化
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 迁移UI提示信息
- [ ] 验证功能一致性

## 常见问题

### Q1: 如何处理老架构中的JavaFX依赖？

**A**: 老架构中的JavaFX UI组件需要用Flutter重写，但核心算法和业务逻辑可以完全复用。将核心逻辑提取到独立的类中，不依赖JavaFX。

### Q2: 如何处理配置文件的迁移？

**A**: 提供配置转换工具，将Properties格式转换为JSON格式。用户首次使用新架构时，自动检测并转换老架构的配置文件。

### Q3: 如何确保迁移后的功能与老架构一致？

**A**: 使用老架构的测试用例进行回归测试，确保迁移后的功能与老架构一致。同时，进行充分的用户测试。

### Q4: 如何处理FFmpeg相关的策略？

**A**: FFmpeg相关的策略需要在后端集成FFmpeg，通过REST API调用FFmpeg命令。前端只负责配置参数和显示结果。

### Q5: 如何处理WebSocket实时更新？

**A**: 在插件执行过程中，通过WebSocket推送进度更新。前端监听WebSocket消息，实时更新UI。

## 最佳实践

### 1. 代码复用

- 最大化复用老架构的核心算法和业务逻辑
- 将核心逻辑提取到独立的类中，不依赖UI框架
- 使用接口和抽象类提高代码的可复用性

### 2. 配置管理

- 使用JSON格式存储配置
- 提供配置转换工具
- 支持配置的导入和导出

### 3. 错误处理

- 捕获并记录所有异常
- 提供清晰的错误信息
- 实现优雅的降级策略

### 4. 性能优化

- 使用线程池进行并发处理
- 实现缓存机制减少重复计算
- 优化大文件和大量文件的处理

### 5. 用户体验

- 提供详细的参数提示信息
- 实现实时进度反馈
- 支持操作的撤销和重做

## 相关文档

- [策略比对文档](strategy-comparison-old-new.md)
- [功能测试用例](functional-test-cases.md)
- [插件系统文档](../new-framework/docV2/plugin-system.md)
- [测试标准](../../standard/test-style/test-standard.md)

---

**文档版本**: 1.0  
**创建日期**: 2026-02-03  
**维护者**: FileManager Plus Team
