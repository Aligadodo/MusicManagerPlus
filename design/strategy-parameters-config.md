# 策略参数配置完整清单

## 1. AdvancedRenameStrategy - 高级重命名策略

### 基本参数
- **规则列表** (ListView<RenameRule>)
  - 描述：重命名规则列表
  - 类型：复杂对象列表
  - 默认值：空列表
  - 必填：否

- **跨盘动作** (cbCrossDriveMode)
  - 描述：跨盘操作时的动作
  - 类型：下拉选择
  - 选项：["移动 (Move)", "复制 (Copy)"]
  - 默认值："移动 (Move)"
  - 必填：否

- **处理范围** (cbProcessScope)
  - 描述：处理的文件类型范围
  - 类型：下拉选择
  - 选项：["仅处理文件", "仅处理文件夹", "全部处理"]
  - 默认值："全部处理"
  - 必填：否

### 条件参数
- **规则编辑** (双击规则列表项)
  - 描述：编辑选中的重命名规则
  - 类型：对话框
  - 显示条件：当规则列表不为空时

## 2. AudioConverterStrategy - 音频格式转换策略

### 基本参数（继承自AbstractFfmpegStrategy）
- **目标格式** (cbTargetFormat)
  - 描述：转换后的音频文件格式
  - 类型：下拉选择
  - 选项：["WAV (CD标准)", "FLAC", "WAV", "MP3", "ALAC", "AAC", "OGG"]
  - 默认值："WAV (CD标准)"
  - 必填：是

- **输出路径** (pathSelection)
  - 描述：转换后文件的输出路径
  - 类型：路径选择组件
  - 子参数：
    - outputDirMode: ["子目录", "指定目录", "根目录"]
    - path: 路径字符串
  - 默认值："Convert - WAV"
  - 必填：是

- **采样率** (cbSampleRate)
  - 描述：转换后的音频采样率
  - 类型：下拉选择
  - 选项：["保持原样 (Original)", "44100", "48000", "88200", "96000", "192000"]
  - 默认值："44100"
  - 必填：否
  - **条件参数**：当选中"WAV (CD标准)"时禁用

- **声道数** (cbChannels)
  - 描述：转换后的音频声道数
  - 类型：下拉选择
  - 选项：["保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)"]
  - 默认值："2 (Stereo)"
  - 必填：否
  - **条件参数**：当选中"WAV (CD标准)"时禁用

- **强制覆盖** (chkOverwrite)
  - 描述：是否覆盖已存在的目标文件
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **FFmpeg线程数** (spFfmpegThreads)
  - 描述：FFmpeg的线程数
  - 类型：数字输入（1-16）
  - 默认值：4
  - 必填：否

- **FFmpeg路径** (txtFFmpegPath)
  - 描述：FFmpeg可执行文件的路径
  - 类型：文本输入
  - 默认值：自动扫描
  - 必填：否

### 缓存和性能参数
- **启用临时文件缓存** (chkEnableCache)
  - 描述：启用临时文件缓存以缓解IO瓶颈
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **缓存目录** (txtCacheDir)
  - 描述：临时文件缓存目录路径
  - 类型：文本输入
  - 默认值：空
  - 必填：否
  - **条件参数**：当启用临时文件缓存时显示

- **启用镜像路径暂存** (chkEnableSnap)
  - 描述：启用镜像路径暂存（需要手动移动文件）
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **镜像存储目录** (txtSnapDir)
  - 描述：镜像存储目录路径
  - 类型：文本输入
  - 默认值：空
  - 必填：否
  - **条件参数**：当启用镜像路径暂存时显示

- **启用.temp文件后缀** (chkEnableTempSuffix)
  - 描述：启用.temp文件后缀（文件缓存启用时不生效）
  - 类型：复选框
  - 默认值：true
  - 必填：否
  - **条件参数**：当禁用临时文件缓存时显示

### 元数据参数
- **忽略原始文件标签** (chkForceFilenameMeta)
  - 描述：忽略原始文件标签，强制用文件名重构元数据
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **自动格式化目标文件名** (chkAutoFormatFilename)
  - 描述：自动将目标文件名转换为简体中文并去除首尾空格
  - 类型：复选框
  - 默认值：true
  - 必填：否

### AudioConverterStrategy特有参数
- **智能跳过处理** (chkSkipCueTracks)
  - 描述：当音频文件大于100MB且同目录下有.cue文件时，跳过处理
  - 类型：复选框
  - 默认值：true
  - 必填：否

## 3. FileCleanupStrategy - 文件清理与去重策略

### 基本参数
- **清理模式** (cbMode)
  - 描述：清理的逻辑规则
  - 类型：下拉选择
  - 选项：["文件去重", "文件夹去重", "清理空目录", "直接清理"]
  - 默认值："文件去重"
  - 必填：是

- **删除方式** (cbMethod)
  - 描述：删除的方式
  - 类型：下拉选择
  - 选项：["伪删除", "直接删除", "可回滚删除"]
  - 默认值："伪删除"
  - 必填：是

### 回收站参数
- **回收站路径** (txtTrashPath)
  - 描述：回收站的位置
  - 类型：文本输入
  - 默认值：".EchoTrash"
  - 必填：否
  - **条件参数**：当删除方式为"伪删除"或"可回滚删除"时显示

### 去重参数
- **保留体积/质量最佳的副本** (chkKeepLargest)
  - 描述：保留最大的文件
  - 类型：复选框
  - 默认值：true
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"时显示

- **保留日期最早/最晚的副本** (chkKeepEarliest)
  - 描述：保留日期最早的文件
  - 类型：复选框
  - 默认值：true
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"或"文件夹去重"时显示

- **优先后缀** (txtKeepExt)
  - 描述：去重时优先保留的文件后缀
  - 类型：文本输入
  - 默认值："wav"
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"时显示

### 文件名预处理参数
- **文件名转小写** (chkPreprocessLower)
  - 描述：将文件名转换为小写后进行比较
  - 类型：复选框
  - 默认值：true
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"时显示

- **文件名转大写** (chkPreprocessUpper)
  - 描述：将文件名转换为大写后进行比较
  - 类型：复选框
  - 默认值：false
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"时显示
  - **互斥参数**：与"文件名转小写"互斥

- **文件名转简体中文** (chkPreprocessSimplified)
  - 描述：将文件名中的繁体中文转换为简体中文后进行比较
  - 类型：复选框
  - 默认值：false
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"时显示

### 高级选项
- **文件大小范围** (cbSizeRange)
  - 描述：要处理的文件大小范围
  - 类型：下拉选择
  - 选项：["全部", "小于1MB", "小于10MB", "小于100MB", "小于1GB", "大于1MB", "大于10MB", "大于100MB", "大于1GB"]
  - 默认值："全部"
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"或"直接清理"时显示

- **音频文件特殊处理** (chkAudioSpecial)
  - 描述：对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件
  - 类型：复选框
  - 默认值：true
  - 必填：否
  - **条件参数**：当清理模式为"文件去重"时显示

## 4. MetadataScraperStrategy - 元数据抓取策略

### 基本参数
- **数据源** (cbSource)
  - 描述：元数据数据源
  - 类型：下拉选择
  - 选项：[
    "本地推断 (仅生成清单)",
    "网易云音乐 (中文歌曲) (不完善)",
    "咪咕音乐 (版权歌曲) (不完善)",
    "MusicBrainz (开源数据库)",
    "iTunes (苹果音乐)",
    "Last.fm (全球音乐平台) (不完善)",
    "Discogs (音乐数据库) (不完善)"
  ]
  - 默认值："本地推断 (仅生成清单)"
  - 必填：是

- **线程数** (spThreads)
  - 描述：并发抓取的线程数
  - 类型：数字输入
  - 默认值：4
  - 必填：否

### 模块化配置参数
- **歌词模块配置** (lyricsConfigUI)
  - 描述：歌词抓取配置
  - 类型：模块化配置
  - 子参数：
    - enabled: 是否启用
    - sources: 数据源列表
    - timeout: 超时时间
  - 必填：否

- **封面模块配置** (coverConfigUI)
  - 描述：封面抓取配置
  - 类型：模块化配置
  - 子参数：
    - enabled: 是否启用
    - sources: 数据源列表
    - maxSize: 最大尺寸
    - format: 图片格式
  - 必填：否

- **专辑信息模块配置** (albumInfoConfigUI)
  - 描述：专辑信息抓取配置
  - 类型：模块化配置
  - 子参数：
    - enabled: 是否启用
    - sources: 数据源列表
    - fields: 要抓取的字段列表
  - 必填：否

### 限流配置参数
- **最大请求数** (rateLimiterConfig.maxRequests)
  - 描述：单位时间内的最大请求数
  - 类型：数字输入
  - 默认值：10
  - 必填：否

- **时间周期** (rateLimiterConfig.periodMs)
  - 描述：限流的时间周期（毫秒）
  - 类型：数字输入
  - 默认值：1000
  - 必填：否

## 5. CueSplitterStrategy - CUE分轨策略

### 基本参数（继承自AbstractFfmpegStrategy）
- 继承AbstractFfmpegStrategy的所有参数

### CueSplitterStrategy特有参数
- **切分后操作** (cbAfterSplitAction)
  - 描述：切分完成后对原始文件的处理方式
  - 类型：下拉选择
  - 选项：["什么都不做 (默认)", "删除原始文件", "归档原始文件"]
  - 默认值："什么都不做 (默认)"
  - 必填：否

- **启用归档目录** (chkEnableArchive)
  - 描述：启用时，将原始文件移动到指定的归档目录
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **归档目录路径** (txtArchiveDir)
  - 描述：原始文件的归档目录路径
  - 类型：文本输入
  - 默认值：空
  - 必填：否
  - **条件参数**：当启用归档目录时显示

## 6. FileMigrateStrategy - 文件批量归档和移动策略

### 基本参数
- **操作模式** (cbOperationMode)
  - 描述：文件的操作方式
  - 类型：下拉选择
  - 选项：["移动 (MOVE)", "复制 (COPY)"]
  - 默认值："移动 (MOVE)"
  - 必填：是

- **路径选择** (pathSelectionComponent)
  - 描述：目标路径选择
  - 类型：路径选择组件
  - 子参数：
    - outputDirMode: ["子目录", "指定目录", "根目录"]
    - path: 路径字符串
  - 默认值："Archive"
  - 必填：是

- **生效范围选择** (scopeSelectionComponent)
  - 描述：文件处理的生效范围
  - 类型：范围选择组件
  - 子参数：
    - scope: ["全部", "当前目录", "指定深度"]
    - depth: 深度值
  - 默认值："全部"
  - 必填：否

### 去重策略配置参数
- **去重策略配置** (duplicateStrategyConfig)
  - 描述：去重策略配置
  - 类型：模块化配置
  - 子参数：
    - keepLargest: 保留最大文件
    - keepEarliest: 保留最早文件
    - keepExt: 优先后缀
    - audioSpecial: 音频特殊处理
  - 必填：否

## 7. AlbumDirNormalizeStrategy - 专辑目录标准化策略

### 基本参数
- **目录命名模板** (cbTemplate)
  - 描述：专辑目录的命名模板
  - 类型：下拉选择
  - 选项：[
    "%artist% - %year% - %album%",
    "[%year%] %artist% - %album%",
    "%artist%/%album% (%year%)",
    "%year% - %album% - %artist%",
    "%album% - %artist% [%year%]",
    "%artist% - %album%",
    "%album% (%year%)",
    "自定义模板"
  ]
  - 默认值："%artist% - %year% - %album%"
  - 必填：是

- **自定义模板** (txtCustomTemplate)
  - 描述：自定义命名模板
  - 类型：文本输入
  - 默认值：空
  - 必填：否
  - **条件参数**：当选择"自定义模板"时显示并启用

### 清理选项
- **清理特殊字符** (chkCleanSpecialChars)
  - 描述：清理目录名中的特殊字符
  - 类型：复选框
  - 默认值：true
  - 必填：否

- **移除年份前缀** (chkRemoveYearPrefix)
  - 描述：移除目录名中的年份前缀
  - 类型：复选框
  - 默认值：false
  - 必填：否

### 元数据选项
- **使用共识元数据** (chkUseConsensusMetadata)
  - 描述：使用多个文件的共识元数据
  - 类型：复选框
  - 默认值：true
  - 必填：否

- **保留原始名称** (chkPreserveOriginalName)
  - 描述：当无法获取元数据时保留原始目录名
  - 类型：复选框
  - 默认值：true
  - 必填：否

- **验证专辑信息** (chkValidateAlbumInfo)
  - 描述：验证专辑信息的完整性
  - 类型：复选框
  - 默认值：true
  - 必填：否

## 8. FileUnzipStrategy - 批量智能解压策略

### 基本参数
- **解压引擎** (cbEngine)
  - 描述：解压引擎选择
  - 类型：下拉选择
  - 选项：["Java 内置引擎", "7-Zip 引擎", "Bandizip 命令行工具"]
  - 默认值："Java 内置引擎"
  - 必填：是

- **可执行文件路径** (txtExePath)
  - 描述：外部解压工具的可执行文件路径
  - 类型：文本输入
  - 默认值：空
  - 必填：否
  - **条件参数**：当选择"7-Zip 引擎"或"Bandizip 命令行工具"时显示

- **输出模式** (cbOutputMode)
  - 描述：输出目录模式
  - 类型：下拉选择
  - 选项：["自动创建子目录", "解压到当前目录", "指定目录"]
  - 默认值："自动创建子目录"
  - 必填：是

- **自定义路径** (txtCustomPath)
  - 描述：自定义输出路径
  - 类型：文本输入
  - 默认值：空
  - 必填：否
  - **条件参数**：当选择"指定目录"时显示

### 解压选项
- **智能文件夹** (chkSmartFolder)
  - 描述：智能识别解压后的文件夹结构
  - 类型：复选框
  - 默认值：true
  - 必填：否

- **合并同名文件夹** (chkMergeSameName)
  - 描述：合并同名的文件夹
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **解压成功后删除源文件** (chkDeleteSource)
  - 描述：解压成功后删除原始压缩文件
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **覆盖已存在文件** (chkOverwrite)
  - 描述：覆盖已存在的文件
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **解压失败后删除源文件** (chkDeleteOnFail)
  - 描述：解压失败后删除原始压缩文件
  - 类型：复选框
  - 默认值：false
  - 必填：否

- **嵌套文件夹合并** (chkNestedFolderMerge)
  - 描述：合并嵌套的文件夹
  - 类型：复选框
  - 默认值：false
  - 必填：否

### 密码箱参数
- **密码列表** (lvPasswords)
  - 描述：解压密码列表
  - 类型：列表
  - 默认值：空列表
  - 必填：否

- **新密码** (txtNewPass)
  - 描述：要添加的新密码
  - 类型：文本输入
  - 默认值：空
  - 必填：否

- **添加密码按钮** (btnAddPass)
  - 描述：添加新密码到密码列表
  - 类型：按钮
  - 必填：否

- **删除密码按钮** (btnDelPass)
  - 描述：从密码列表删除选中的密码
  - 类型：按钮
  - 必填：否

## 9. FileCollectionStrategy - 文件收集策略

### 基本参数
- **目标目录** (targetDirectory)
  - 描述：文件收集的目标目录
  - 类型：目录选择
  - 默认值："/tmp/collected"
  - 必填：是

- **递归收集** (recursive)
  - 描述：是否递归收集子目录中的文件
  - 类型：复选框
  - 默认值：true
  - 必填：否

## 10. FileTypeFixStrategy - 文件类型修复策略

### 基本参数
- **目标格式** (targetFormat)
  - 描述：修复后的文件格式
  - 类型：下拉选择
  - 选项：["自动检测", "WAV", "FLAC", "MP3", "AAC", "OGG"]
  - 默认值："自动检测"
  - 必填：是

- **保留原始文件** (keepOriginal)
  - 描述：是否保留原始文件
  - 类型：复选框
  - 默认值：true
  - 必填：否

- **备份原始文件** (backupOriginal)
  - 描述：是否备份原始文件
  - 类型：复选框
  - 默认值：true
  - 必填：否

## 11. CueFileRenameStrategy - CUE文件重命名策略

### 基本参数
- **重命名模式** (renameMode)
  - 描述：CUE文件的重命名模式
  - 类型：下拉选择
  - 选项：["基于音频文件名", "基于目录名", "自定义"]
  - 默认值："基于音频文件名"
  - 必填：是

- **自定义模板** (customTemplate)
  - 描述：自定义重命名模板
  - 类型：文本输入
  - 默认值：空
  - 必填：否
  - **条件参数**：当选择"自定义"时显示

## 12. NcmIntegratedStrategy - 网易云音乐集成策略

### 基本参数
- **操作模式** (operationMode)
  - 描述：操作模式
  - 类型：下拉选择
  - 选项：["转换", "缓存转换", "歌词下载", "元数据修复"]
  - 默认值："转换"
  - 必填：是

- **输出格式** (outputFormat)
  - 描述：输出格式
  - 类型：下拉选择
  - 选项：["MP3", "FLAC", "WAV"]
  - 默认值："MP3"
  - 必填：是

- **输出目录** (outputDirectory)
  - 描述：输出目录
  - 类型：目录选择
  - 默认值：空
  - 必填：否

## 模块化设计模式

### 1. PathSelectionComponent - 路径选择组件
- **用途**：统一的路径选择界面
- **参数**：
  - outputDirMode: 输出目录模式
  - path: 路径字符串
- **复用策略**：被多个策略复用

### 2. ScopeSelectionComponent - 范围选择组件
- **用途**：统一的文件处理范围选择界面
- **参数**：
  - scope: 处理范围
  - depth: 深度值
- **复用策略**：被多个策略复用

### 3. DuplicateStrategyConfig - 去重策略配置
- **用途**：统一的去重策略配置
- **参数**：
  - keepLargest: 保留最大文件
  - keepEarliest: 保留最早文件
  - keepExt: 优先后缀
  - audioSpecial: 音频特殊处理
- **复用策略**：被FileCleanupStrategy和FileMigrateStrategy复用

### 4. RateLimiterConfig - 限流配置
- **用途**：统一的API限流配置
- **参数**：
  - maxRequests: 最大请求数
  - periodMs: 时间周期
- **复用策略**：被MetadataScraperStrategy复用

### 5. AbstractFfmpegStrategy - FFmpeg策略基类
- **用途**：FFmpeg相关策略的基类
- **参数**：所有FFmpeg相关参数
- **继承策略**：AudioConverterStrategy、CueSplitterStrategy继承
