# NCM相关策略文档

## 概述

本文档详细描述了FileManager Plus系统中的网易云音乐（NCM）相关处理策略，包括NCM转换、缓存扫描、歌词下载等功能。这些策略通过插件系统实现，提供了专业的网易云音乐文件处理能力。

## 网易云音乐工具集插件 (NcmIntegratedPlugin)

### 插件信息

- **插件ID**: `ncm-integrated`
- **插件名称**: 网易云音乐工具集插件
- **插件描述**: 网易云音乐工具集，包含NCM转换、缓存扫描、歌词下载等功能
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| function | select | convert | 是 | 功能选择 |
| outputFormat | select | mp3 | 否 | 输出格式 |
| bitrate | select | 320k | 否 | 比特率 |
| cacheDir | directory | "" | 否 | 缓存目录 |
| outputDir | directory | "" | 否 | 输出目录 |
| downloadLyric | boolean | true | 否 | 下载歌词 |
| lyricFormat | select | lrc | 否 | 歌词格式 |
| overwrite | boolean | false | 否 | 覆盖已存在文件 |

### 功能选择

| 功能值 | 描述 | 适用场景 |
|--------|------|----------|
| convert | NCM转换 | 转换NCM加密文件为普通音频格式 |
| cache_scan | 缓存扫描 | 扫描网易云音乐缓存目录 |
| lyric_download | 歌词下载 | 下载对应的歌词文件 |

### 输出格式

| 格式值 | 描述 | 特点 |
|--------|------|------|
| mp3 | MP3格式 | 通用性好，压缩率高 |
| flac | FLAC格式 | 无损压缩，音质好 |
| wav | WAV格式 | 无损，文件较大 |
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

### 歌词格式

| 格式值 | 描述 | 特点 |
|--------|------|------|
| lrc | LRC格式 | 支持时间轴同步 |
| txt | TXT格式 | 纯文本格式 |

### 使用示例

#### NCM转换

```http
POST /api/plugins/ncm-integrated/execute
Content-Type: application/json

{
  "files": ["/path/to/song1.ncm", "/path/to/song2.ncm"],
  "config": {
    "values": {
      "function": "convert",
      "outputFormat": "mp3",
      "bitrate": "320k",
      "outputDir": "/converted",
      "downloadLyric": true,
      "lyricFormat": "lrc",
      "overwrite": false
    }
  }
}
```

#### 缓存扫描

```http
POST /api/plugins/ncm-integrated/execute
Content-Type: application/json

{
  "files": ["/path/to/cache"],
  "config": {
    "values": {
      "function": "cache_scan",
      "cacheDir": "/path/to/netease/cloudmusic/Cache",
      "outputFormat": "mp3",
      "outputDir": "/extracted",
      "downloadLyric": true,
      "lyricFormat": "lrc"
    }
  }
}
```

#### 歌词下载

```http
POST /api/plugins/ncm-integrated/execute
Content-Type: application/json

{
  "files": ["/path/to/song1.mp3", "/path/to/song2.mp3"],
  "config": {
    "values": {
      "function": "lyric_download",
      "lyricFormat": "lrc",
      "overwrite": false
    }
  }
}
```

### 功能特性

- NCM加密文件转换
- 网易云音乐缓存扫描
- 歌词自动下载
- 多种输出格式支持
- 可调节比特率
- 批量处理支持
- 原始文件保留

### 前置条件

- 文件扩展名必须是：ncm, uc, cache

## NCM文件格式

### NCM文件结构

NCM（NetEase Cloud Music）是网易云音乐使用的加密音频格式：

| 偏移量 | 长度 | 描述 |
|--------|------|------|
| 0-9 | 10 | 文件标识 "CTCN" + 版本 |
| 10-21 | 12 | 密钥数据 |
| 22-25 | 4 | 元数据长度 |
| 26-... | 变长 | 元数据 |
| ...-... | 变长 | 加密音频数据 |

### 加密算法

NCM使用RC4流加密算法：

1. **密钥生成**: 使用固定的密钥表生成加密密钥
2. **数据解密**: 使用RC4算法解密音频数据
3. **元数据解析**: 解析JSON格式的元数据

### 元数据格式

NCM元数据包含以下信息：

| 字段 | 描述 | 示例 |
|------|------|------|
| musicName | 歌曲名称 | "夜曲" |
| artist | 艺术家 | "周杰伦" |
| album | 专辑 | "十一月的萧邦" |
| albumPic | 封面图片 | Base64编码 |
| format | 格式 | "mp3" |
| duration | 时长 | 245000 |

## 网易云音乐缓存结构

### 缓存目录位置

| 操作系统 | 默认路径 |
|----------|----------|
| Windows | C:\Users\{用户名}\AppData\Local\Netease\CloudMusic\Cache |
| macOS | ~/Library/Caches/com.netease.cloudmusic/Cache |
| Linux | ~/.cache/netease-cloudmusic/Cache |

### 缓存文件命名

缓存文件使用以下命名格式：

```
{歌曲ID}_{文件哈希}.{扩展名}
```

示例：
```
12345678_abcdef123456.mp3
87654321_654321fedcba.ncm
```

### 缓存文件类型

| 扩展名 | 描述 |
|--------|------|
| .mp3 | MP3格式缓存 |
| .ncm | NCM加密格式 |
| .uc | UC加密格式 |
| .lrc | 歌词文件 |

## NCM处理最佳实践

### 1. NCM转换流程

```json
{
  "pipeline": [
    {
      "pluginId": "ncm-integrated",
      "config": {
        "values": {
          "function": "convert",
          "outputFormat": "mp3",
          "bitrate": "320k",
          "outputDir": "/converted",
          "downloadLyric": true,
          "lyricFormat": "lrc"
        }
      }
    }
  ]
}
```

### 2. 缓存扫描流程

```json
{
  "pipeline": [
    {
      "pluginId": "ncm-integrated",
      "config": {
        "values": {
          "function": "cache_scan",
          "cacheDir": "/path/to/netease/cloudmusic/Cache",
          "outputFormat": "mp3",
          "outputDir": "/extracted",
          "downloadLyric": true,
          "lyricFormat": "lrc"
        }
      }
    }
  ]
}
```

### 3. 完整NCM处理流程

```json
{
  "pipeline": [
    {
      "pluginId": "ncm-integrated",
      "config": {
        "values": {
          "function": "convert",
          "outputFormat": "mp3",
          "bitrate": "320k",
          "downloadLyric": true,
          "lyricFormat": "lrc"
        }
      }
    },
    {
      "pluginId": "metadata-scraper",
      "config": {
        "values": {
          "sources": ["discogs", "musicbrainz"],
          "updateTags": true,
          "updateCoverArt": true
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

### 4. 批量NCM转换流程

```json
{
  "pipeline": [
    {
      "pluginId": "file-collection",
      "config": {
        "values": {
          "targetDirectory": "/tmp/ncm",
          "recursive": true,
          "includePatterns": "*.ncm"
        }
      }
    },
    {
      "pluginId": "ncm-integrated",
      "config": {
        "values": {
          "function": "convert",
          "outputFormat": "mp3",
          "bitrate": "320k",
          "outputDir": "/converted",
          "downloadLyric": true
        }
      }
    }
  ]
}
```

## 歌词处理

### LRC格式

LRC（Lyric）格式是常用的歌词文件格式，支持时间轴同步：

```
[00:00.00]歌词第一行
[00:05.00]歌词第二行
[00:10.00]歌词第三行
```

### 时间轴格式

| 格式 | 示例 | 描述 |
|------|------|------|
| [mm:ss.xx] | [01:23.45] | 分:秒.毫秒 |
| [mm:ss] | [01:23] | 分:秒 |

### 歌词编码

- 推荐使用UTF-8编码
- 支持中文、英文等多语言
- 支持特殊字符

## 性能优化

### 1. 批量处理

使用流水线批量处理多个NCM文件：

```json
{
  "pipeline": [
    {
      "pluginId": "ncm-integrated",
      "config": {
        "values": {
          "function": "convert",
          "outputFormat": "mp3",
          "bitrate": "320k"
        }
      }
    }
  ]
}
```

### 2. 并行处理

使用线程池并行处理多个文件：

```json
{
  "threadPoolConfig": {
    "executionThreads": 4,
    "previewThreads": 2
  }
}
```

### 3. 缓存优化

使用缓存目录提高性能：

```json
{
  "config": {
    "values": {
      "cacheDir": "/tmp/ncm_cache"
    }
  }
}
```

## 注意事项

1. **版权问题**: NCM文件受版权保护，仅供个人使用
2. **备份原始文件**: 在转换前建议备份原始NCM文件
3. **网络连接**: 歌词下载需要稳定的网络连接
4. **编码问题**: 处理中文歌词时注意编码问题（UTF-8）
5. **文件权限**: 确保有足够的权限读取和写入文件
6. **磁盘空间**: 转换需要足够的磁盘空间
7. **元数据完整性**: 转换后可能需要补充元数据
8. **歌词同步**: 确保歌词与音频时间轴同步

## 常见问题

### 1. NCM转换失败

**可能原因**:
- NCM文件损坏
- 加密算法不匹配
- 文件权限不足

**解决方法**:
- 检查文件完整性
- 更新插件版本
- 检查文件权限

### 2. 歌词下载失败

**可能原因**:
- 网络连接问题
- 歌词不存在
- API限制

**解决方法**:
- 检查网络连接
- 手动搜索歌词
- 稍后重试

### 3. 缓存扫描无结果

**可能原因**:
- 缓存目录路径错误
- 缓存文件已删除
- 缓存格式不兼容

**解决方法**:
- 确认缓存目录路径
- 检查缓存文件是否存在
- 更新插件版本

### 4. 音质下降

**可能原因**:
- 比特率设置过低
- 原始文件音质较差
- 转换算法问题

**解决方法**:
- 提高比特率设置
- 检查原始文件音质
- 尝试不同的输出格式

## NCM API

### 转换NCM文件

```http
POST /api/plugins/ncm-integrated/convert
Content-Type: application/json

{
  "files": ["/path/to/song.ncm"],
  "config": {
    "outputFormat": "mp3",
    "bitrate": "320k",
    "outputDir": "/converted"
  }
}
```

### 扫描缓存目录

```http
POST /api/plugins/ncm-integrated/scan-cache
Content-Type: application/json

{
  "cacheDir": "/path/to/netease/cloudmusic/Cache",
  "outputDir": "/extracted"
}
```

### 下载歌词

```http
POST /api/plugins/ncm-integrated/download-lyric
Content-Type: application/json

{
  "files": ["/path/to/song.mp3"],
  "lyricFormat": "lrc"
}
```

## 相关文档

- [插件系统文档](plugin-system.md)
- [插件实现文档](plugin-implementation.md)
- [核心文件管理策略文档](core-file-management.md)
- [音频处理策略文档](audio-processing.md)
- [元数据处理策略文档](metadata-processing.md)
- [API端点文档](api-endpoints.md)
- [测试指南](testing-guide.md)

---

**文档版本**: 1.0  
**创建日期**: 2026-02-04  
**维护者**: FileManager Plus Team
