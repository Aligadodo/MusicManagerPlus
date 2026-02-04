# 元数据处理策略文档

## 概述

本文档详细描述了FileManager Plus系统中的元数据处理策略，包括元数据抓取、专辑目录标准化等功能。这些策略通过插件系统实现，提供了专业的音频元数据管理能力。

## 元数据抓取插件 (MetadataScraperPlugin)

### 插件信息

- **插件ID**: `metadata-scraper`
- **插件名称**: 元数据抓取插件
- **插件描述**: 从网络或本地抓取并更新文件的元数据信息
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| sources | select | discogs, musicbrainz, local | 是 | 元数据源 |
| updateTags | boolean | true | 否 | 更新标签 |
| updateCoverArt | boolean | true | 否 | 更新封面 |
| forceUpdate | boolean | false | 否 | 强制更新 |

### 元数据源

| 源值 | 描述 | 特点 |
|------|------|------|
| discogs | Discogs音乐数据库 | 数据丰富，社区维护 |
| musicbrainz | MusicBrainz音乐数据库 | 开源，数据准确 |
| local | 本地元数据 | 快速，无需网络 |

### 使用示例

```http
POST /api/plugins/metadata-scraper/execute
Content-Type: application/json

{
  "files": ["/path/to/song1.mp3", "/path/to/song2.mp3"],
  "config": {
    "values": {
      "sources": ["discogs", "musicbrainz", "local"],
      "updateTags": true,
      "updateCoverArt": true,
      "forceUpdate": false
    }
  }
}
```

### 功能特性

- 多数据源支持
- 自动元数据匹配
- 封面艺术更新
- 标签信息更新
- 强制更新选项
- 本地缓存支持

### 元数据字段

支持的元数据字段包括：

| 字段名 | 描述 | 示例 |
|--------|------|------|
| title | 歌曲标题 | "Bohemian Rhapsody" |
| artist | 艺术家 | "Queen" |
| album | 专辑名称 | "A Night at the Opera" |
| year | 发行年份 | "1975" |
| genre | 音乐类型 | "Rock" |
| track | 音轨编号 | "1" |
| albumartist | 专辑艺术家 | "Queen" |
| composer | 作曲家 | "Freddie Mercury" |
| comment | 备注 | "Remastered" |
| cover | 封面图片 | 二进制数据 |

## 专辑目录标准化插件 (AlbumDirNormalizePlugin)

### 插件信息

- **插件ID**: `album-dir-normalize`
- **插件名称**: 专辑目录标准化插件
- **插件描述**: 智能规范化专辑目录名称，支持多种命名模板、元数据提取、特殊字符清理等功能
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| template | select | %artist% - %year% - %album% | 是 | 命名模板 |
| customTemplate | text | "" | 否 | 自定义模板 |
| cleanSpecialChars | boolean | true | 否 | 清理特殊字符 |
| removeYearPrefix | boolean | true | 否 | 移除年份前缀 |
| useConsensusMetadata | boolean | true | 否 | 使用共识元数据 |
| preserveOriginalName | boolean | false | 否 | 保留原始目录名 |
| validateAlbumInfo | boolean | true | 否 | 验证专辑信息 |

### 命名模板

| 模板值 | 示例 | 描述 |
|--------|------|------|
| %artist% - %year% - %album% | Queen - 1975 - A Night at the Opera | 标准格式 |
| [%year%] %artist% - %album% | [1975] Queen - A Night at the Opera | 年份前置 |
| %artist%/%album% (%year%) | Queen/A Night at the Opera (1975) | 子目录格式 |
| %year% - %album% - %artist% | 1975 - A Night at the Opera - Queen | 年份优先 |
| %album% - %artist% [%year%] | A Night at the Opera - Queen [1975] | 专辑优先 |
| %artist% - %album% | Queen - A Night at the Opera | 简洁格式 |
| %album% (%year%) | A Night at the Opera (1975) | 仅专辑 |
| custom | 自定义 | 自定义格式 |

### 模板占位符

| 占位符 | 描述 | 示例 |
|--------|------|------|
| %artist% | 艺术家名称 | "Queen" |
| %album% | 专辑名称 | "A Night at the Opera" |
| %year% | 发行年份 | "1975" |
| %genre% | 音乐类型 | "Rock" |

### 使用示例

```http
POST /api/plugins/album-dir-normalize/execute
Content-Type: application/json

{
  "files": ["/path/to/album1", "/path/to/album2"],
  "config": {
    "values": {
      "template": "%artist% - %year% - %album%",
      "customTemplate": "",
      "cleanSpecialChars": true,
      "removeYearPrefix": true,
      "useConsensusMetadata": true,
      "preserveOriginalName": false,
      "validateAlbumInfo": true
    }
  }
}
```

### 功能特性

- 多种命名模板
- 自定义模板支持
- 元数据提取和共识计算
- 特殊字符清理
- 年份前缀处理
- 专辑信息验证
- 原始目录名保留

### 共识元数据算法

插件使用共识算法从目录内所有音频文件中提取元数据：

1. **收集元数据**: 扫描目录内所有音频文件，提取每个文件的元数据
2. **统计频率**: 对每个元数据字段（艺术家、专辑、年份、类型）统计出现频率
3. **选择共识**: 选择出现频率最高的值作为共识元数据
4. **应用模板**: 使用共识元数据填充命名模板

### 特殊字符清理

插件会清理以下特殊字符：

| 字符 | 替换为 | 说明 |
|------|--------|------|
| / | - | 路径分隔符 |
| \ | - | 路径分隔符 |
| : | - | 驱动器分隔符 |
| * | - | 通配符 |
| ? | - | 通配符 |
| " | ' | 引号 |
| < | - | 小于号 |
| > | - | 大于号 |
| \| | - | 管道符 |

### 专辑信息验证

验证规则：

- 必须包含艺术家信息
- 必须包含专辑名称
- 年份必须是有效的4位数字
- 目录内必须包含音频文件

## 元数据处理最佳实践

### 1. 专辑整理流程

```json
{
  "pipeline": [
    {
      "pluginId": "metadata-scraper",
      "config": {
        "values": {
          "sources": ["discogs", "musicbrainz", "local"],
          "updateTags": true,
          "updateCoverArt": true,
          "forceUpdate": false
        }
      }
    },
    {
      "pluginId": "album-dir-normalize",
      "config": {
        "values": {
          "template": "%artist% - %year% - %album%",
          "useConsensusMetadata": true,
          "cleanSpecialChars": true,
          "validateAlbumInfo": true
        }
      }
    }
  ]
}
```

### 2. 批量元数据更新流程

```json
{
  "pipeline": [
    {
      "pluginId": "metadata-scraper",
      "config": {
        "values": {
          "sources": ["discogs", "musicbrainz"],
          "updateTags": true,
          "updateCoverArt": true,
          "forceUpdate": true
        }
      }
    }
  ]
}
```

### 3. 专辑目录标准化流程

```json
{
  "pipeline": [
    {
      "pluginId": "album-dir-normalize",
      "config": {
        "values": {
          "template": "%artist% - %year% - %album%",
          "cleanSpecialChars": true,
          "removeYearPrefix": true,
          "useConsensusMetadata": true
        }
      }
    },
    {
      "pluginId": "file-migrate",
      "config": {
        "values": {
          "operationMode": "MOVE",
          "targetPath": "/organized/music",
          "pathMode": "absolute",
          "preserveStructure": true
        }
      }
    }
  ]
}
```

### 4. 完整专辑处理流程

```json
{
  "pipeline": [
    {
      "pluginId": "metadata-scraper",
      "config": {
        "values": {
          "sources": ["discogs", "musicbrainz", "local"],
          "updateTags": true,
          "updateCoverArt": true
        }
      }
    },
    {
      "pluginId": "album-dir-normalize",
      "config": {
        "values": {
          "template": "%artist% - %year% - %album%",
          "useConsensusMetadata": true,
          "cleanSpecialChars": true,
          "validateAlbumInfo": true
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
    }
  ]
}
```

## 元数据标准

### ID3标签

#### ID3v1

ID3v1是音频文件末尾的128字节固定长度标签：

| 偏移量 | 长度 | 字段 | 描述 |
|--------|------|------|------|
| 0-2 | 3 | 标识 | "TAG" |
| 3-32 | 30 | 标题 | 歌曲标题 |
| 33-62 | 30 | 艺术家 | 艺术家名称 |
| 63-92 | 30 | 专辑 | 专辑名称 |
| 93-96 | 4 | 年份 | 发行年份 |
| 97-126 | 30 | 注释 | 备注 |
| 127 | 1 | 类型 | 音乐类型 |

#### ID3v2

ID3v2是可变长度的标签，支持更多字段：

| 帧ID | 描述 |
|-------|------|
| TIT2 | 标题 |
| TPE1 | 艺术家 |
| TALB | 专辑 |
| TYER/ TDRC | 年份 |
| TCON | 类型 |
| TRCK | 音轨 |
| TPE2 | 专辑艺术家 |
| TCOM | 作曲家 |
| COMM | 注释 |
| APIC | 封面图片 |

### Vorbis注释

用于OGG和FLAC文件：

| 字段名 | 描述 |
|--------|------|
| TITLE | 标题 |
| ARTIST | 艺术家 |
| ALBUM | 专辑 |
| DATE | 日期 |
| GENRE | 类型 |
| TRACKNUMBER | 音轨编号 |
| ALBUMARTIST | 专辑艺术家 |
| COMPOSER | 作曲家 |
| COMMENT | 注释 |
| COVERART | 封面图片 |

### MP4标签

用于M4A和MP4文件：

| 原子名 | 描述 |
|--------|------|
| ©nam | 标题 |
| ©ART | 艺术家 |
| ©alb | 专辑 |
| ©day | 日期 |
| ©gen | 类型 |
| trkn | 音轨 |
| aART | 专辑艺术家 |
| ©wrt | 作曲家 |
| ©cmt | 注释 |
| covr | 封面图片 |

## 元数据质量建议

### 1. 数据完整性

确保以下字段完整：

| 优先级 | 字段 | 说明 |
|--------|------|------|
| 高 | 标题 | 必须准确 |
| 高 | 艺术家 | 必须准确 |
| 高 | 专辑 | 必须准确 |
| 中 | 年份 | 建议填写 |
| 中 | 音轨 | 建议填写 |
| 低 | 类型 | 可选 |
| 低 | 作曲家 | 可选 |

### 2. 命名规范

- 使用标准的大小写（首字母大写）
- 避免使用特殊字符
- 使用标准的语言编码（UTF-8）
- 保持一致性（同一专辑使用相同的艺术家名称）

### 3. 封面艺术

- 推荐分辨率：500x500 或 1000x1000
- 文件格式：JPEG或PNG
- 文件大小：建议小于1MB
- 内容：专辑封面，不包含水印

## 注意事项

1. **备份原始文件**: 在更新元数据前，建议备份原始文件
2. **网络连接**: 使用在线数据源需要稳定的网络连接
3. **数据准确性**: 在线数据源可能存在错误，需要人工验证
4. **编码问题**: 处理中文元数据时注意编码问题（UTF-8）
5. **封面版权**: 注意封面艺术的版权问题
6. **批量处理**: 大批量处理时建议分批进行
7. **性能优化**: 使用本地缓存可以提高性能
8. **权限问题**: 确保有足够的权限修改文件元数据

## 性能优化

### 1. 使用本地缓存

```json
{
  "config": {
    "values": {
      "sources": ["local", "discogs"],
      "forceUpdate": false
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
      "pluginId": "metadata-scraper",
      "config": {
        "values": {
          "sources": ["discogs", "musicbrainz"]
        }
      }
    },
    {
      "pluginId": "album-dir-normalize",
      "config": {
        "values": {
          "template": "%artist% - %year% - %album%"
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

## 元数据API

### 获取文件元数据

```http
GET /api/files/{fileId}/metadata
```

### 更新文件元数据

```http
POST /api/files/{fileId}/metadata
Content-Type: application/json

{
  "title": "Song Title",
  "artist": "Artist Name",
  "album": "Album Name",
  "year": "2024",
  "genre": "Rock",
  "track": 1
}
```

### 批量更新元数据

```http
POST /api/files/metadata/batch
Content-Type: application/json

{
  "fileIds": ["file1", "file2", "file3"],
  "metadata": {
    "artist": "Artist Name",
    "album": "Album Name"
  }
}
```

## 相关文档

- [插件系统文档](plugin-system.md)
- [插件实现文档](plugin-implementation.md)
- [核心文件管理策略文档](core-file-management.md)
- [音频处理策略文档](audio-processing.md)
- [API端点文档](api-endpoints.md)
- [测试指南](testing-guide.md)

---

**文档版本**: 1.0  
**创建日期**: 2026-02-04  
**维护者**: FileManager Plus Team
