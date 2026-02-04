# 音频处理策略文档

## 概述

本文档详细描述了FileManager Plus系统中的音频处理策略，包括音频转换、CUE分轨、音轨编号等功能。这些策略通过插件系统实现，提供了专业的音频文件处理能力。

## 音频转换插件 (AudioConverterPlugin)

### 插件信息

- **插件ID**: `audio-converter`
- **插件名称**: 音频转换插件
- **插件描述**: 将音频文件转换为不同格式
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| targetFormat | select | mp3 | 是 | 目标格式 |
| bitrate | select | 320k | 否 | 比特率 |
| sampleRate | select | 44100 | 否 | 采样率 |
| channels | select | 2 | 否 | 声道数 |
| outputDirectory | directory | "" | 否 | 输出目录 |
| overwriteExisting | boolean | false | 否 | 覆盖现有文件 |

### 目标格式

| 格式值 | 描述 | 特点 |
|--------|------|------|
| mp3 | MP3格式 | 通用性好，压缩率高 |
| wav | WAV格式 | 无损，文件较大 |
| flac | FLAC格式 | 无损压缩，音质好 |
| ogg | OGG格式 | 开源，压缩率高 |
| aac | AAC格式 | 苹果设备支持好 |

### 比特率选项

| 比特率值 | 描述 | 适用场景 |
|----------|------|----------|
| 64k | 低比特率 | 语音、播客 |
| 128k | 中等比特率 | 普通音乐 |
| 192k | 高比特率 | 高质量音乐 |
| 256k | 很高比特率 | 高品质音乐 |
| 320k | 最高比特率 | 无损级别 |

### 采样率选项

| 采样率值 | 描述 | 适用场景 |
|----------|------|----------|
| 22050 | 低采样率 | 语音、播客 |
| 44100 | CD标准 | 普通音乐 |
| 48000 | 专业音频 | 专业制作 |
| 96000 | 高清音频 | 高品质制作 |

### 声道数选项

| 声道数值 | 描述 |
|----------|------|
| 1 | 单声道 |
| 2 | 立体声 |

### 使用示例

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
      "outputDirectory": "/path/to/output",
      "overwriteExisting": false
    }
  }
}
```

### 功能特性

- 支持多种音频格式转换
- 可调节比特率和采样率
- 支持单声道和立体声
- 自定义输出目录
- 批量转换处理
- 保留原始文件

### 前置条件

- 文件扩展名必须是音频格式：wav, flac, ogg, aac, mp3, wma

## CUE分轨插件 (CueSplitterPlugin)

### 插件信息

- **插件ID**: `cue-splitter`
- **插件名称**: CUE分轨插件
- **插件描述**: 解析.cue索引文件，将整轨音频无损切割为单曲。支持预览详细的歌曲清单与时长信息。只需要扫描cue文件。
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| afterSplitAction | select | do_nothing | 是 | 切分后操作 |
| enableArchive | boolean | false | 否 | 启用归档目录 |
| archiveDir | directory | "" | 否 | 归档目录 |
| outputDirPrefix | text | Split | 否 | 输出目录前缀 |
| overwrite | boolean | false | 否 | 覆盖已存在文件 |
| format | text | %artist% - %album% - %track% - %title% | 否 | 文件名格式 |
| autoFormatFilename | boolean | true | 否 | 自动格式化文件名 |
| useCacheDir | boolean | false | 否 | 使用缓存目录 |
| cacheDir | directory | "" | 否 | 缓存目录 |
| mirrorDir | directory | "" | 否 | 镜像目录 |

### 切分后操作

| 操作值 | 描述 |
|--------|------|
| do_nothing | 不做任何操作 |
| delete_original | 删除原始文件 |
| archive_original | 归档原始文件 |

### 文件名格式占位符

| 占位符 | 描述 |
|--------|------|
| %artist% | 艺术家名称 |
| %album% | 专辑名称 |
| %track% | 音轨编号 |
| %title% | 歌曲标题 |

### 使用示例

```http
POST /api/plugins/cue-splitter/execute
Content-Type: application/json

{
  "files": ["/path/to/album.cue"],
  "config": {
    "values": {
      "afterSplitAction": "archive_original",
      "enableArchive": true,
      "archiveDir": "/path/to/archive",
      "outputDirPrefix": "Split",
      "overwrite": false,
      "format": "%artist% - %album% - %track% - %title%",
      "autoFormatFilename": true,
      "useCacheDir": false,
      "cacheDir": "",
      "mirrorDir": ""
    }
  }
}
```

### 功能特性

- 无损音频切割
- CUE文件解析
- 支持多种音频格式
- 自动提取元数据
- 灵活的文件命名
- 原始文件归档
- 缓存目录支持
- 镜像目录支持

### 前置条件

- 文件扩展名必须是.cue格式

## 音轨编号插件 (TrackNumberPlugin)

### 插件信息

- **插件ID**: `track-number`
- **插件名称**: 音轨编号插件
- **插件描述**: 为音频文件添加或修改音轨编号，支持多种编号模式、双位补零、自定义分隔符等功能。
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| mode | select | default | 是 | 编号模式 |
| startNumber | number | 1 | 否 | 起始编号 |
| padZero | boolean | true | 否 | 双位补零 |
| numberFormat | select | 01 | 否 | 编号格式 |
| separator | text | ". " | 否 | 分隔符 |
| updateMetadata | boolean | true | 否 | 更新元数据 |
| preserveOriginal | boolean | false | 否 | 保留原始文件 |
| groupByDirectory | boolean | true | 否 | 按目录分组编号 |

### 编号模式

| 模式值 | 描述 |
|--------|------|
| default | 默认模式 |
| metadata | 元数据模式 |
| textList | 文本列表模式 |
| cueFile | CUE文件模式 |
| custom | 自定义模式 |

### 编号格式

| 格式值 | 示例 | 描述 |
|--------|------|------|
| 1 | 1, 2, 3 | 不补零 |
| 01 | 01, 02, 03 | 双位补零 |
| 001 | 001, 002, 003 | 三位补零 |

### 使用示例

```http
POST /api/plugins/track-number/execute
Content-Type: application/json

{
  "files": ["/path/to/song1.mp3", "/path/to/song2.mp3", "/path/to/song3.mp3"],
  "config": {
    "values": {
      "mode": "default",
      "startNumber": 1,
      "padZero": true,
      "numberFormat": "01",
      "separator": ". ",
      "updateMetadata": true,
      "preserveOriginal": false,
      "groupByDirectory": true
    }
  }
}
```

### 功能特性

- 多种编号模式
- 自动编号递增
- 双位补零支持
- 自定义分隔符
- 元数据更新
- 按目录分组
- 保留原始文件

## 音频处理最佳实践

### 1. 音频转换流程

```json
{
  "pipeline": [
    {
      "pluginId": "audio-converter",
      "config": {
        "values": {
          "targetFormat": "mp3",
          "bitrate": "320k",
          "sampleRate": 44100,
          "channels": 2,
          "outputDirectory": "/converted",
          "overwriteExisting": false
        }
      }
    }
  ]
}
```

### 2. CUE分轨流程

```json
{
  "pipeline": [
    {
      "pluginId": "cue-splitter",
      "config": {
        "values": {
          "afterSplitAction": "archive_original",
          "enableArchive": true,
          "archiveDir": "/archive",
          "outputDirPrefix": "Split",
          "format": "%artist% - %album% - %track% - %title%",
          "autoFormatFilename": true
        }
      }
    },
    {
      "pluginId": "track-number",
      "config": {
        "values": {
          "mode": "default",
          "startNumber": 1,
          "padZero": true,
          "separator": ". ",
          "updateMetadata": true
        }
      }
    }
  ]
}
```

### 3. 专辑整理流程

```json
{
  "pipeline": [
    {
      "pluginId": "cue-splitter",
      "config": {
        "values": {
          "afterSplitAction": "do_nothing",
          "format": "%artist% - %album% - %track% - %title%"
        }
      }
    },
    {
      "pluginId": "track-number",
      "config": {
        "values": {
          "mode": "default",
          "startNumber": 1,
          "padZero": true,
          "updateMetadata": true
        }
      }
    },
    {
      "pluginId": "audio-converter",
      "config": {
        "values": {
          "targetFormat": "mp3",
          "bitrate": "320k",
          "outputDirectory": "/organized"
        }
      }
    }
  ]
}
```

### 4. 批量格式统一流程

```json
{
  "pipeline": [
    {
      "pluginId": "audio-converter",
      "config": {
        "values": {
          "targetFormat": "mp3",
          "bitrate": "320k",
          "sampleRate": 44100,
          "channels": 2
        }
      }
    },
    {
      "pluginId": "track-number",
      "config": {
        "values": {
          "mode": "default",
          "startNumber": 1,
          "padZero": true,
          "groupByDirectory": true
        }
      }
    }
  ]
}
```

## 音频格式对比

### 有损格式

| 格式 | 比特率 | 文件大小 | 音质 | 兼容性 |
|------|--------|----------|------|--------|
| MP3 | 64-320k | 小 | 中 | 极好 |
| AAC | 64-256k | 小 | 中高 | 好 |
| OGG | 64-256k | 小 | 中 | 中 |

### 无损格式

| 格式 | 压缩率 | 文件大小 | 音质 | 兼容性 |
|------|--------|----------|------|--------|
| WAV | 无压缩 | 最大 | 完美 | 极好 |
| FLAC | 50-60% | 中 | 完美 | 好 |
| ALAC | 50-60% | 中 | 完美 | 中 |

## 音频质量建议

### 不同场景的推荐设置

| 场景 | 格式 | 比特率 | 采样率 | 声道 |
|------|------|--------|--------|------|
| 普通聆听 | MP3 | 192-256k | 44100 | 2 |
| 高品质聆听 | MP3 | 320k | 44100 | 2 |
| 无损收藏 | FLAC | - | 44100/48000 | 2 |
| 专业制作 | WAV | - | 48000/96000 | 2 |
| 播客/语音 | MP3 | 64-128k | 22050/44100 | 1 |

## 注意事项

1. **备份原始文件**: 在进行音频转换前，建议备份原始无损文件
2. **格式选择**: 根据使用场景选择合适的音频格式
3. **比特率设置**: 高比特率不等于高音质，需要平衡文件大小和音质
4. **CUE文件编码**: 确保CUE文件使用正确的编码（UTF-8或GBK）
5. **元数据完整性**: 音轨编号更新会影响文件元数据
6. **批量处理**: 大批量处理时建议使用缓存目录提高性能
7. **磁盘空间**: 音频转换需要足够的磁盘空间
8. **文件权限**: 确保有足够的权限读取和写入文件

## 性能优化

### 1. 使用缓存目录

对于机械硬盘，使用缓存目录可以显著提高性能：

```json
{
  "config": {
    "values": {
      "useCacheDir": true,
      "cacheDir": "/tmp/cache",
      "mirrorDir": "/ssd/mirror"
    }
  }
}
```

### 2. 批量处理

使用流水线批量处理多个文件：

```json
{
  "pipeline": [
    {
      "pluginId": "cue-splitter",
      "config": {
        "values": {
          "useCacheDir": true
        }
      }
    },
    {
      "pluginId": "audio-converter",
      "config": {
        "values": {
          "targetFormat": "mp3",
          "bitrate": "320k"
        }
      }
    }
  ]
}
```

### 3. 并行处理

使用线程池并行处理多个文件：

```json
{
  "threadPoolConfig": {
    "executionThreads": 4,
    "previewThreads": 2
  }
}
```

## 相关文档

- [插件系统文档](plugin-system.md)
- [插件实现文档](plugin-implementation.md)
- [核心文件管理策略文档](core-file-management.md)
- [元数据处理策略文档](metadata-processing.md)
- [API端点文档](api-endpoints.md)
- [测试指南](testing-guide.md)

---

**文档版本**: 1.0  
**创建日期**: 2026-02-04  
**维护者**: FileManager Plus Team
