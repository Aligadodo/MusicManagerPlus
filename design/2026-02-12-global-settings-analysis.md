# 全局设置参数检查与改造方案

## 一、前端全局设置参数清单

### 1. 线程池配置
| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| previewThreads | int | 10 | 预览线程数 (1-16) |
| executionThreads | int | 4 | 执行线程数 (1-12) |
| threadPoolMode | String | 'GLOBAL' | 线程池模式 (GLOBAL/ROOT_PATH) |

### 2. 运行配置
| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| autoRefresh | bool | true | 自动刷新 |
| previewLimit | int | 200 | 预览限制 |
| executionLimit | int | 1000 | 执行限制 |

### 3. 扫描配置
| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| recursionMode | String | 'ALL' | 扫描模式 (ALL/CURRENT/SPECIFIC/RANGE) |
| recursionDepth | int | 3 | 扫描层级 (1-10) |
| minRecursionDepth | int | 1 | 最小扫描层级 |
| maxRecursionDepth | int | 3 | 最大扫描层级 |

### 4. 过滤规则
| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| scanFilterList | List<String> | 见下方 | 扫描过滤规则列表 |

默认过滤规则：
```
*Convert*, *Split*, *System*, *trash*, *Temp*, *tmp*, *cache*, *backup*
```

### 5. 文件类型筛选
| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| fileTypeTree | FileTypeNode | 全选 | 文件类型树形结构 |
| customFileTypes | List<String> | [] | 自定义文件类型 |

## 二、后端实现情况分析

### 2.1 已实现的参数

#### ✅ ThreadPoolController
- **previewThreads**: ✅ 已实现 (默认值: 4)
- **executionThreads**: ✅ 已实现 (默认值: 8)
- **threadPoolMode**: ❌ 未实现

**API接口**:
- `GET /api/thread-pool` - 获取线程池配置
- `PUT /api/thread-pool/preview` - 设置预览线程数
- `PUT /api/thread-pool/execution` - 设置执行线程数

#### ✅ FileFilterServiceImpl
- **scanFilterList**: ✅ 已实现

**默认过滤规则**:
```java
*Convert*, *Split*, *System*, *trash*, *Temp*, *Cache*, *Log*,
*\Windows\*, *\Program Files\*, *\Program Files (x86)\*, *\ProgramData\*, *\AppData\*,
*\Local Settings\*, *\Application Data\*, *\Recycle Bin\*, *\System Volume Information\*,
*\.*, *\~*, *\Thumbs.db, *\Temp\*, *\TMP\*
```

**API接口**: 无独立接口，通过服务层调用

#### ✅ PreviewLimitServiceImpl
- **previewLimit**: ✅ 已实现 (默认值: 100)
- **executionLimit**: ✅ 已实现 (默认值: 100)

**支持的功能**:
- 全局预览/执行限制
- 根路径级别的预览/执行限制
- 无限制模式开关

**API接口**: 无独立接口，通过服务层调用

#### ✅ FileScanner
- **recursionDepth**: ✅ 已实现 (通过参数传递)
- **minRecursionDepth**: ✅ 已实现
- **maxRecursionDepth**: ✅ 已实现

**使用场景**: 在 PipelineController 的 analyzePipeline 方法中使用

### 2.2 未实现的参数

#### ❌ ConfigController
- **threadPoolMode**: ❌ 未实现
- **autoRefresh**: ❌ 未实现
- **recursionMode**: ❌ 未实现
- **fileTypeTree**: ❌ 未实现
- **customFileTypes**: ❌ 未实现

**问题**:
- ConfigController 只提供简单的内存存储，不持久化
- 没有实际使用前端传递的配置参数
- 缺少与业务逻辑的集成

#### ❌ 文件类型筛选功能
- **fileTypeTree**: ❌ 未实现
- **customFileTypes**: ❌ 未实现

**老架构实现**: AdvancedFileTypeManager

## 三、老架构参数原型分析

### 3.1 GlobalSettingsView

**核心功能**:
1. **扫描模式配置**
   - 全部文件: 扫描所有层级的文件
   - 当前目录: 只扫描当前目录
   - 指定目录层级: 扫描到指定的层级
   - 目录层级范围: 扫描指定范围内的层级

2. **文件类型过滤**
   - 使用 AdvancedFileTypeManager 管理文件类型
   - 支持树形级联选择
   - 支持用户自定义输入

3. **扫描过滤器**
   - 支持通配符过滤
   - 支持上下移动规则
   - 支持删除规则

**配置保存/加载**:
```java
props.setProperty("filter.recursion.mode", String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
props.setProperty("filter.recursion.depth", String.valueOf(spRecursionDepth.getValue()));
props.setProperty("filter.recursion.minDepth", String.valueOf(spMinRecursionDepth.getValue()));
props.setProperty("filter.recursion.maxDepth", String.valueOf(spMaxRecursionDepth.getValue()));
props.setProperty("filter.scan.rules", String.join("||", scanFilterList));
```

### 3.2 AdvancedFileTypeManager

**核心特性**:
1. **树形级联选择** (CheckBoxTreeItem)
2. **内置丰富的默认规则**
3. **支持用户自定义输入后缀**
4. **高性能匹配缓存**

**预设规则**:
- 音频: dsf, dff, dts, ape, wav, flac, m4a, dfd, tak, tta, wv, mp3, aac, ogg, wma
- 音频其他: cue, lrc
- 图片: jpg, jpeg, png, gif, bmp, webp, svg, ico, tif, tiff
- 视频: mp4, mkv, avi, mov, wmv, flv, webm, ts
- 文档: txt, pdf, doc, docx, xls, xlsx, ppt, pptx, md, csv
- 压缩包: zip, rar, 7z, tar, gz, iso, jar
- 代码: java, c, cpp, py, js, html, css, json, xml, sql, sh, bat
- 程序: exe, msi, bat, cmd, sh, app

**配置保存/加载**:
```java
props.setProperty("filter.custom.input", customInputCallback.getText());
props.setProperty("filter.option.folders", String.valueOf(isFolderSelected.get()));
props.setProperty("filter.tree.selected", String.join(",", treeSelectedExts));
```

## 四、设计评估

### 4.1 满足的功能 ✅

1. **基本配置管理**: ConfigController 提供了基本的配置存储接口
2. **文件过滤规则**: FileFilterServiceImpl 实现了完整的过滤规则管理
3. **线程数配置**: ThreadPoolController 实现了预览和执行线程数的配置
4. **预览/执行限制**: PreviewLimitServiceImpl 实现了限制功能
5. **文件扫描**: FileScanner 实现了文件扫描功能，支持层级控制

### 4.2 缺失的功能 ❌

1. **线程池模式**: threadPoolMode 参数未在后端实现
2. **扫描模式配置**: recursionMode 参数未在后端实现
3. **文件类型筛选**: fileTypeTree 和 customFileTypes 未在后端实现
4. **自动刷新**: autoRefresh 参数未在后端实现
5. **配置持久化**: ConfigController 只使用内存存储，不持久化

### 4.3 设计问题 ⚠️

1. **配置分散**: 配置分散在多个 Controller 和 Service 中
   - ThreadPoolController 管理线程数
   - FileFilterServiceImpl 管理过滤规则
   - PreviewLimitServiceImpl 管理限制
   - ConfigController 提供通用配置接口

2. **前后端不一致**:
   - 前端默认值与后端默认值不一致
   - 前端参数范围与后端参数范围不一致

3. **缺少统一配置管理**:
   - 没有统一的配置管理器
   - 配置保存和加载逻辑分散

4. **缺少配置验证**:
   - 没有配置参数的验证逻辑
   - 没有配置冲突检测

## 五、改造方案

### 5.1 短期改造 (1-2周)

#### 1. 统一配置管理
- 创建统一的配置管理器 ConfigManager
- 将所有配置参数集中管理
- 实现配置的持久化 (JSON/Properties)

#### 2. 实现缺失的参数
- 实现 threadPoolMode 参数
- 实现 recursionMode 参数
- 实现 autoRefresh 参数

#### 3. 修复前后端不一致
- 统一前后端默认值
- 统一前后端参数范围
- 添加参数验证逻辑

### 5.2 中期改造 (3-4周)

#### 1. 实现文件类型筛选功能
- 创建 FileTypeFilterService
- 实现文件类型树形结构
- 支持自定义文件类型
- 与 FileScanner 集成

#### 2. 完善线程池模式
- 实现全局统一配置模式
- 实现根路径独立配置模式
- 与 SourceDirectoryController 集成

#### 3. 完善扫描模式
- 实现全部文件模式
- 实现当前目录模式
- 实现指定目录层级模式
- 实现目录层级范围模式

### 5.3 长期改造 (5-8周)

#### 1. 配置管理系统
- 配置导入导出功能
- 配置版本管理
- 配置回滚功能
- 配置模板功能

#### 2. 配置验证和提示
- 配置参数验证
- 配置冲突检测
- 配置优化建议
- 配置错误提示

#### 3. 配置同步和备份
- 配置自动同步
- 配置自动备份
- 配置恢复功能