# 核心文件管理策略文档

## 概述

本文档详细描述了FileManager Plus系统中的核心文件管理策略，包括文件收集、清理、迁移、重命名等功能。这些策略通过插件系统实现，提供了灵活的文件管理能力。

## 文件收集插件 (FileCollectionPlugin)

### 插件信息

- **插件ID**: `file-collection`
- **插件名称**: 文件收集插件
- **插件版本**: `1.0.0`
- **插件描述**: 根据配置规则收集和整理文件

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| targetDirectory | directory | /tmp/collected | 是 | 文件收集的目标目录 |
| recursive | boolean | true | 否 | 是否递归收集子目录中的文件 |
| includePatterns | text | *.mp3,*.wav,*.flac | 否 | 要收集的文件模式列表，多个模式用逗号分隔 |
| excludePatterns | text | *.tmp,*.log | 否 | 要排除的文件模式列表，多个模式用逗号分隔 |

### 使用示例

```http
POST /api/plugins/file-collection/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "targetDirectory": "/path/to/target",
      "recursive": true,
      "includePatterns": "*.mp3,*.wav,*.flac",
      "excludePatterns": "*.tmp,*.log"
    }
  }
}
```

### 功能特性

- 支持递归扫描子目录
- 支持文件模式匹配（通配符）
- 支持排除特定文件类型
- 自动创建目标目录
- 保留原始文件名

## 文件清理插件 (FileCleanupPlugin)

### 插件信息

- **插件ID**: `file-cleanup`
- **插件名称**: 文件清理插件
- **插件描述**: 支持文件去重、文件夹去重、空目录清理、文件夹合并等多种清理模式
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| cleanupMode | select | dedup_files | 是 | 清理模式 |
| deleteMethod | select | pseudo_delete | 是 | 删除方法 |
| trashPath | text | .EchoTrash | 否 | 回收站路径 |
| keepLargest | boolean | true | 否 | 保留体积/质量最佳的副本 |
| keepEarliest | boolean | true | 否 | 保留日期最早/最晚的副本 |
| keepExt | text | wav | 否 | 优先后缀 |
| preprocessLower | boolean | true | 否 | 文件名转小写 |
| preprocessUpper | boolean | false | 否 | 文件名转大写 |
| preprocessSimplified | boolean | false | 否 | 文件名转简体中文 |
| audioSpecial | boolean | true | 否 | 音频文件特殊处理 |
| minFileSizeKB | number | 0 | 否 | 最小文件大小(KB) |
| maxFileSizeKB | number | 10240 | 否 | 最大文件大小(KB) |

### 清理模式

| 模式值 | 描述 |
|--------|------|
| dedup_files | 文件去重 |
| dedup_folders | 文件夹去重 |
| remove_empty_dirs | 移除空目录 |
| direct_cleanup | 直接清理 |
| merge_same_name | 合并同名文件夹 |
| merge_nested | 合并嵌套文件夹 |

### 删除方法

| 方法值 | 描述 |
|--------|------|
| direct_delete | 直接删除 |
| pseudo_delete | 伪删除（移动到回收站） |

### 使用示例

```http
POST /api/plugins/file-cleanup/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "cleanupMode": "dedup_files",
      "deleteMethod": "pseudo_delete",
      "keepLargest": true,
      "keepEarliest": true,
      "keepExt": "wav",
      "preprocessLower": true,
      "audioSpecial": true,
      "minFileSizeKB": 0,
      "maxFileSizeKB": 10240
    }
  }
}
```

### 功能特性

- 支持多种清理模式
- 智能文件去重算法
- 支持音频文件特殊处理
- 文件名标准化处理
- 文件大小范围过滤
- 安全的删除机制（伪删除）

## 文件迁移插件 (FileMigratePlugin)

### 插件信息

- **插件ID**: `file-migrate`
- **插件名称**: 文件迁移插件
- **插件描述**: 文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| operationMode | select | MOVE | 是 | 操作模式 |
| targetPath | directory | "" | 是 | 目标路径 |
| pathMode | select | absolute | 是 | 路径模式 |
| scope | select | all | 是 | 生效范围 |
| duplicateStrategy | select | skip | 是 | 去重策略 |
| overwrite | boolean | false | 否 | 覆盖已存在文件 |
| preserveStructure | boolean | true | 否 | 保留目录结构 |

### 操作模式

| 模式值 | 描述 |
|--------|------|
| MOVE | 移动文件 |
| COPY | 复制文件 |

### 路径模式

| 模式值 | 描述 |
|--------|------|
| absolute | 绝对路径 |
| relative | 相对路径 |
| flat | 扁平结构 |

### 生效范围

| 范围值 | 描述 |
|--------|------|
| all | 所有文件 |
| selected | 选中文件 |
| matched | 匹配文件 |

### 去重策略

| 策略值 | 描述 |
|--------|------|
| skip | 跳过重复文件 |
| overwrite | 覆盖重复文件 |
| rename | 重命名重复文件 |
| keep_both | 保留所有副本 |

### 使用示例

```http
POST /api/plugins/file-migrate/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "operationMode": "MOVE",
      "targetPath": "/path/to/target",
      "pathMode": "absolute",
      "scope": "all",
      "duplicateStrategy": "skip",
      "overwrite": false,
      "preserveStructure": true
    }
  }
}
```

### 功能特性

- 支持移动和复制操作
- 多种路径模式选择
- 灵活的重名处理策略
- 保留原始目录结构
- 跨盘符迁移支持

## 高级重命名插件 (AdvancedRenamePlugin)

### 插件信息

- **插件ID**: `advanced-rename`
- **插件名称**: 高级重命名插件
- **插件描述**: 支持规则列表、正则表达式、元数据提取等多种重命名方式的高级重命名工具
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| crossDriveMode | select | move | 是 | 跨盘动作 |
| processScope | select | all | 是 | 处理范围 |
| rules | list | [] | 否 | 重命名规则列表 |
| caseSensitive | boolean | false | 否 | 区分大小写 |
| useRegex | boolean | false | 否 | 使用正则表达式 |
| preserveExtension | boolean | true | 否 | 保留文件扩展名 |
| overwrite | boolean | false | 否 | 覆盖已存在文件 |

### 跨盘动作

| 动作值 | 描述 |
|--------|------|
| move | 移动文件 |
| copy | 复制文件 |

### 处理范围

| 范围值 | 描述 |
|--------|------|
| files_only | 仅处理文件 |
| folders_only | 仅处理文件夹 |
| all | 处理所有 |

### 使用示例

```http
POST /api/plugins/advanced-rename/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "crossDriveMode": "move",
      "processScope": "all",
      "rules": [],
      "caseSensitive": false,
      "useRegex": false,
      "preserveExtension": true,
      "overwrite": false
    }
  }
}
```

### 功能特性

- 支持多种重命名规则
- 正则表达式支持
- 元数据提取重命名
- 大小写转换
- 批量重命名
- 跨盘操作支持

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

| 模板值 | 描述 |
|--------|------|
| %artist% - %year% - %album% | 艺术家 - 年份 - 专辑 |
| %album% | 仅专辑名 |
| %artist% - %album% | 艺术家 - 专辑 |
| custom | 自定义模板 |

### 模板占位符

| 占位符 | 描述 |
|--------|------|
| %artist% | 艺术家名称 |
| %album% | 专辑名称 |
| %year% | 发行年份 |
| %genre% | 音乐类型 |

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

- 多种命名模板选择
- 自定义模板支持
- 元数据提取和共识计算
- 特殊字符清理
- 年份前缀处理
- 专辑信息验证

## 文件重命名插件 (FileRenamePlugin)

### 插件信息

- **插件ID**: `file-rename`
- **插件名称**: 文件重命名插件
- **插件描述**: 根据规则批量重命名文件
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| pattern | text | {name}_{index} | 是 | 重命名模式 |
| startIndex | number | 1 | 否 | 起始索引 |
| padZeros | boolean | true | 否 | 用零填充索引 |
| zeroPadding | number | 3 | 否 | 零填充位数 |
| preserveExtension | boolean | true | 否 | 保留文件扩展名 |
| overwriteExisting | boolean | false | 否 | 覆盖已存在的文件 |

### 模式占位符

| 占位符 | 描述 |
|--------|------|
| {name} | 原始文件名（不含扩展名） |
| {index} | 序号 |

### 使用示例

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
      "preserveExtension": true,
      "overwriteExisting": false
    }
  }
}
```

### 功能特性

- 灵活的重命名模式
- 序号自动递增
- 零填充支持
- 保留文件扩展名
- 批量重命名

## 文件解压插件 (FileUnzipPlugin)

### 插件信息

- **插件ID**: `file-unzip`
- **插件名称**: 文件解压插件
- **插件描述**: 批量智能解压文件，支持多种压缩格式、密码管理、智能目录等功能
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| engine | select | java | 是 | 解压引擎 |
| exePath | text | "" | 否 | 可执行文件路径 |
| outputMode | select | same_dir | 是 | 输出模式 |
| customPath | text | "" | 否 | 自定义路径 |
| smartFolder | boolean | true | 否 | 智能文件夹 |
| mergeSameName | boolean | false | 否 | 合并同名文件夹 |
| deleteSource | boolean | false | 否 | 解压成功后删除源文件 |
| overwrite | boolean | false | 否 | 覆盖已存在文件 |
| deleteOnFail | boolean | false | 否 | 解压失败后删除源文件 |
| nestedFolderMerge | boolean | false | 否 | 嵌套文件夹合并 |
| passwords | list | [] | 否 | 密码列表 |

### 解压引擎

| 引擎值 | 描述 |
|--------|------|
| java | Java内置解压 |
| 7zip | 7-Zip解压 |
| bandizip | Bandizip解压 |

### 输出模式

| 模式值 | 描述 |
|--------|------|
| same_dir | 同目录 |
| custom_dir | 自定义目录 |
| parent_dir | 父目录 |

### 使用示例

```http
POST /api/plugins/file-unzip/execute
Content-Type: application/json

{
  "files": ["/path/to/archive1.zip", "/path/to/archive2.rar"],
  "config": {
    "values": {
      "engine": "java",
      "exePath": "",
      "outputMode": "same_dir",
      "customPath": "",
      "smartFolder": true,
      "mergeSameName": false,
      "deleteSource": false,
      "overwrite": false,
      "deleteOnFail": false,
      "nestedFolderMerge": false,
      "passwords": []
    }
  }
}
```

### 功能特性

- 支持多种压缩格式（ZIP, RAR, 7Z等）
- 多种解压引擎支持
- 密码管理
- 智能文件夹处理
- 批量解压
- 解压后处理选项

## 文件类型修复插件 (FileTypeFixPlugin)

### 插件信息

- **插件ID**: `file-type-fix`
- **插件名称**: 文件类型修复插件
- **插件描述**: 一些网上下载的音频文件类型和实际类型不符，可以通过该工具智能进行修复
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| force | boolean | false | 否 | 强制文件类型识别 |

### 使用示例

```http
POST /api/plugins/file-type-fix/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "force": false
    }
  }
}
```

### 功能特性

- 智能文件类型检测
- 基于文件内容的类型识别
- 批量修复
- 保留原始文件

## 合集命名插件 (CollectionNamingPlugin)

### 插件信息

- **插件ID**: `collection-naming`
- **插件名称**: 合集命名插件
- **插件描述**: 支持多种合集命名策略，包括简洁风格、精确风格、选取模板等
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| strategy | select | concise | 是 | 命名策略 |
| removeYear | boolean | true | 否 | 移除年份 |
| removeFormat | boolean | true | 否 | 移除格式 |
| removeCDNumber | boolean | true | 否 | 移除CD序号 |
| removeDiscNumber | boolean | true | 否 | 移除Disc序号 |
| removeVolNumber | boolean | true | 否 | 移除Vol序号 |
| removeParentheses | boolean | false | 否 | 移除括号内容 |
| removeBrackets | boolean | false | 否 | 移除方括号内容 |
| keepTemplate | boolean | false | 否 | 保留模板 |
| overwrite | boolean | false | 否 | 覆盖已存在文件 |

### 命名策略

| 策略值 | 描述 |
|--------|------|
| concise | 简洁风格 |
| precise | 精确风格 |
| template | 选取模板 |

### 使用示例

```http
POST /api/plugins/collection-naming/execute
Content-Type: application/json

{
  "files": ["/path/to/collection1", "/path/to/collection2"],
  "config": {
    "values": {
      "strategy": "concise",
      "removeYear": true,
      "removeFormat": true,
      "removeCDNumber": true,
      "removeDiscNumber": true,
      "removeVolNumber": true,
      "removeParentheses": false,
      "removeBrackets": false,
      "keepTemplate": false,
      "overwrite": false
    }
  }
}
```

### 功能特性

- 多种命名策略
- 灵活的内容过滤
- 模板保留选项
- 批量重命名

## 文件去重插件 (DuplicatePlugin)

### 插件信息

- **插件ID**: `duplicate`
- **插件名称**: 文件去重插件
- **插件描述**: 支持多种去重策略，包括保留最佳版本、添加序号、保留最早/最新文件等
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| strategy | select | keep_best | 是 | 去重策略 |
| comparisonMethod | select | md5 | 是 | 比较方法 |
| caseInsensitive | boolean | true | 否 | 忽略大小写 |
| ignoreWhitespace | boolean | true | 否 | 忽略空白字符 |
| ignoreSpecialChars | boolean | true | 否 | 忽略特殊字符 |
| keepLargest | boolean | true | 否 | 保留最大文件 |
| keepEarliest | boolean | true | 否 | 保留最早文件 |
| keepLatest | boolean | false | 否 | 保留最新文件 |
| addSequence | boolean | false | 否 | 添加序号 |
| sequenceFormat | text | ({index}) | 否 | 序号格式 |
| moveToTrash | boolean | false | 否 | 移动到回收站 |
| trashPath | text | .EchoTrash | 否 | 回收站路径 |

### 去重策略

| 策略值 | 描述 |
|--------|------|
| keep_best | 保留最佳版本 |
| keep_largest | 保留最大文件 |
| keep_earliest | 保留最早文件 |
| keep_latest | 保留最新文件 |
| add_sequence | 添加序号 |

### 比较方法

| 方法值 | 描述 |
|--------|------|
| md5 | MD5哈希值 |
| sha1 | SHA1哈希值 |
| sha256 | SHA256哈希值 |
| size | 文件大小 |
| name | 文件名 |

### 使用示例

```http
POST /api/plugins/duplicate/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "strategy": "keep_best",
      "comparisonMethod": "md5",
      "caseInsensitive": true,
      "ignoreWhitespace": true,
      "ignoreSpecialChars": true,
      "keepLargest": true,
      "keepEarliest": true,
      "keepLatest": false,
      "addSequence": false,
      "sequenceFormat": "({index})",
      "moveToTrash": false,
      "trashPath": ".EchoTrash"
    }
  }
}
```

### 功能特性

- 多种去重策略
- 多种比较方法
- 灵活的文件名处理
- 序号添加支持
- 安全的删除机制

## CUE文件重命名插件 (CueFileRenamePlugin)

### 插件信息

- **插件ID**: `cue-file-rename`
- **插件名称**: CUE文件重命名插件
- **插件描述**: 为了解决cue文件在部分软件下，由于中文命名导致的无法加载的问题，支持统一调整cue及对应的音频文件命名
- **插件版本**: `1.0.0`

### 配置参数

| 参数名 | 类型 | 默认值 | 必填 | 描述 |
|--------|------|--------|------|------|
| mode | select | auto | 是 | 修改模式 |
| fileName | text | album | 否 | 文件名前缀 |
| overwrite | boolean | false | 否 | 覆盖已存在文件 |

### 修改模式

| 模式值 | 描述 |
|--------|------|
| auto | 自动模式 |

### 使用示例

```http
POST /api/plugins/cue-file-rename/execute
Content-Type: application/json

{
  "files": ["/path/to/album.cue", "/path/to/album.flac"],
  "config": {
    "values": {
      "mode": "auto",
      "fileName": "album",
      "overwrite": false
    }
  }
}
```

### 功能特性

- 自动CUE文件重命名
- 音频文件同步重命名
- 兼容性优化

## 插件使用最佳实践

### 1. 文件收集与清理流程

```json
{
  "pipeline": [
    {
      "pluginId": "file-collection",
      "config": {
        "values": {
          "targetDirectory": "/tmp/collected",
          "recursive": true,
          "includePatterns": "*.mp3,*.wav,*.flac"
        }
      }
    },
    {
      "pluginId": "file-cleanup",
      "config": {
        "values": {
          "cleanupMode": "dedup_files",
          "deleteMethod": "pseudo_delete",
          "keepLargest": true,
          "audioSpecial": true
        }
      }
    }
  ]
}
```

### 2. 专辑整理流程

```json
{
  "pipeline": [
    {
      "pluginId": "album-dir-normalize",
      "config": {
        "values": {
          "template": "%artist% - %year% - %album%",
          "useConsensusMetadata": true,
          "cleanSpecialChars": true
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

### 3. 批量重命名流程

```json
{
  "pipeline": [
    {
      "pluginId": "advanced-rename",
      "config": {
        "values": {
          "crossDriveMode": "move",
          "processScope": "all",
          "caseSensitive": false,
          "preserveExtension": true
        }
      }
    },
    {
      "pluginId": "file-rename",
      "config": {
        "values": {
          "pattern": "{name}_{index}",
          "startIndex": 1,
          "padZeros": true,
          "zeroPadding": 3
        }
      }
    }
  ]
}
```

## 注意事项

1. **备份重要文件**: 在执行删除或移动操作前，建议先备份重要文件
2. **预览操作**: 使用预览功能确认操作结果后再执行
3. **伪删除**: 推荐使用伪删除模式，避免误删文件
4. **元数据提取**: 某些插件依赖文件元数据，确保文件包含正确的元数据
5. **路径处理**: 注意绝对路径和相对路径的区别
6. **跨盘操作**: 跨盘操作时建议使用复制模式
7. **文件名编码**: 处理中文文件名时注意编码问题

## 相关文档

- [插件系统文档](plugin-system.md)
- [插件实现文档](plugin-implementation.md)
- [API端点文档](api-endpoints.md)
- [测试指南](testing-guide.md)

---

**文档版本**: 1.0  
**创建日期**: 2026-02-04  
**维护者**: FileManager Plus Team
