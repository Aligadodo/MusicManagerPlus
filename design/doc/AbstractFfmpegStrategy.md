# AbstractFfmpegStrategy 设计文档

## 概述

**功能**: FFmpeg相关操作的抽象基类，提供通用的音频转换和处理功能

**继承**: IAppStrategy

**子类**: AudioConverterStrategy, CueSplitterStrategy

**设计模式**: 模板方法模式

## 核心职责

1. FFmpeg初始化和配置管理
2. 通用音频转换参数设置
3. 路径选择和输出控制
4. 文件覆盖和缓存管理
5. 元数据处理和保留

## 核心组件

### 1. UI组件

#### 输出格式设置
- `cbTargetFormat`: 目标格式选择（WAV, FLAC, MP3, AAC, OGG等）
- 特殊格式：WAV (CD标准) - 16bit/44.1kHz

#### 路径选择
- `pathSelection`: PathSelectionComponent实例
- 支持根目录、子目录、自定义路径

#### 转换参数
- `cbSampleRate`: 采样率（保持原样、44100、48000等）
- `cbChannels`: 声道数（保持原样、Mono、Stereo、5.1）
- `spFfmpegThreads`: FFmpeg线程数（1-16）

#### FFmpeg配置
- `txtFFmpegPath`: FFmpeg可执行文件路径
- 自动扫描：优先检查tools/ffmpeg.exe，然后检查系统环境变量

#### 文件处理选项
- `chkOverwrite`: 强制覆盖已存在的目标文件
- `chkForceFilenameMeta`: 忽略原始文件标签，强制用文件名重构元数据
- `chkAutoFormatFilename`: 自动格式化目标文件名（简体中文、去空格）

#### 缓存和镜像
- `chkEnableCache`: 启用临时文件缓存（缓解IO瓶颈）
- `txtCacheDir`: 临时文件缓存目录
- `chkEnableSnap`: 启用镜像路径暂存（需要手动移动文件）
- `txtSnapDir`: 镜像存储目录
- `chkEnableTempSuffix`: 启用.temp文件后缀（文件缓存启用时不生效）

### 2. 运行时参数

- `pFormat`: 目标格式
- `pOverwrite`: 是否覆盖
- `pFFmpeg`: FFmpeg路径
- `pUseCache`: 是否使用缓存
- `pUseTempSuffix`: 是否使用临时后缀
- `pUseSnapPath`: 是否使用镜像路径
- `pCacheDir`: 缓存目录
- `pSnapDir`: 镜像目录
- `pForceMeta`: 是否强制元数据
- `pInnerThreads`: 内部线程数
- `pSampleRate`: 采样率
- `pChannels`: 声道数
- `pAutoFormatFilename`: 是否自动格式化文件名

## 核心方法

### 1. findFFmpeg()
**功能**: 自动扫描FFmpeg的安装路径

**查找顺序**:
1. 检查appDir/tools/ffmpeg.exe
2. 检查当前目录下的ffmpeg.exe
3. 检查系统环境变量（where ffmpeg）
4. 未找到则返回"ffmpeg"

### 2. convertAudioFile(File source, File target, Map<String, String> params)
**功能**: 执行音频文件转换

**处理流程**:
1. 初始化FFmpeg实例
2. 尝试使用元数据映射执行转换
3. 如果失败且forceMeta=false，重试不使用元数据映射
4. 处理临时文件和镜像路径

### 3. runFFmpegJob(FFmpeg ffmpeg, File source, File target, Map<String, String> params, boolean mapMetadata)
**功能**: 执行FFmpeg转换任务

**关键优化**:
- **起始时间优化**: 将起始时间设在输入上，使用原生寻址，瞬间跳到指定位置
- **流拷贝优化**: 如果输入输出格式一致且不需要重采样，使用copy模式，内存占用近乎0，速度提升百倍

**参数处理**:
- 起始时间（start）
- 持续时间（duration）
- 内部线程数（innerThreads）
- 采样率和声道数
- 元数据映射

### 4. execute(ChangeRecord rec)
**功能**: 执行文件转换操作

**处理流程**:
1. 检查覆盖控制
2. 处理临时文件路径
3. 调用convertAudioFile执行转换
4. 如果使用临时文件，移动到最终目标位置

## 设计要点

### 1. CD标准处理

**WAV (CD标准)** 特殊处理:
- 自动设置为16bit/44.1kHz
- 禁用采样率和声道选择，避免误导
- 适合CD刻录场景

### 2. 元数据处理

**两种模式**:
- **保留元数据**: 使用`-map_metadata 0`映射所有元数据
- **重构元数据**: 使用`-map_metadata -1`丢弃源元数据，从文件名和目录名生成Tag

**重构元数据字段**:
- title, artist, album, year, track, genre, date
- 支持从params中读取自定义元数据

### 3. 缓存和镜像策略

**临时文件缓存**:
- 缓解IO瓶颈
- 需要指定缓存目录
- 转换完成后移动到最终位置

**镜像路径暂存**:
- 将文件存储到镜像目录
- 避免IO性能低下
- 需要手动移动文件

### 4. 文件名格式化

**自动格式化**:
- 转换为简体中文
- 去除首尾空格
- 提高文件名规范性

## 注意事项

### 1. FFmpeg依赖

- 需要正确配置FFmpeg环境
- 支持自动扫描和手动指定
- 版本兼容性需要考虑

### 2. 文件操作安全

- 覆盖控制：默认不覆盖，需要显式勾选
- 临时文件处理：确保转换完成后正确移动
- 异常处理：转换失败时清理临时文件

### 3. 性能优化

- **流拷贝**: 格式一致时使用copy模式，速度提升百倍
- **原生寻址**: 起始时间设在输入上，避免读取多余数据
- **线程控制**: 合理设置FFmpeg线程数，建议为CPU核心数的一半

### 4. 元数据编码

- **乱码问题**: 使用forceMeta选项解决严重乱码
- **ID3v2版本**: MP3文件使用ID3v2版本3
- **编码处理**: 统一使用UTF-8编码

### 5. 路径处理

- **相对路径**: 支持相对路径和绝对路径
- **目录创建**: 自动创建不存在的目录
- **路径验证**: 检查路径有效性

## 交互设计

### 1. 配置界面

**章节划分**:
1. 输出格式设置
2. 路径选择
3. 转换参数设置
4. 文件处理选项

### 2. 悬浮提示

每个关键参数都有详细的悬浮提示：
- 参数名称
- 参数用途
- 使用示例

### 3. 动态交互

- **格式选择**: 选择WAV (CD标准)时自动禁用采样率和声道选择
- **缓存控制**: 启用缓存时自动禁用临时后缀选项
- **路径选择**: 提供浏览按钮选择路径

## 子类扩展

### 1. 必须实现的方法

- `getDefaultDirPrefix()`: 获取默认目录前缀
- `getName()`: 获取策略名称

### 2. 可选重写的方法

- `convertAudioFile()`: 自定义转换逻辑
- `runFFmpegJob()`: 自定义FFmpeg命令构建

### 3. 扩展点

- 添加自定义参数
- 添加自定义UI组件
- 重写转换流程

## 常见问题

### Q1: FFmpeg路径找不到怎么办？

**解决方案**:
1. 检查tools/ffmpeg.exe是否存在
2. 检查系统环境变量是否配置
3. 手动指定FFmpeg路径

### Q2: 转换速度慢怎么办？

**优化建议**:
1. 启用临时文件缓存
2. 使用流拷贝模式（格式一致时）
3. 调整FFmpeg线程数
4. 使用镜像路径暂存

### Q3: 元数据乱码怎么办？

**解决方案**:
1. 勾选"忽略原始文件标签"选项
2. 强制用文件名重构元数据
3. 检查文件编码格式

### Q4: 如何实现CD刻录？

**步骤**:
1. 选择"WAV (CD标准)"格式
2. 系统自动设置为16bit/44.1kHz
3. 转换完成后使用刻录软件刻录

## 最佳实践

### 1. 参数配置

- **CD刻录**: 使用WAV (CD标准)格式
- **无损存储**: 使用FLAC格式
- **移动设备**: 使用MP3格式（256kbps）
- **高保真**: 使用WAV或FLAC格式（24bit/96kHz）

### 2. 性能优化

- 大批量转换：启用临时文件缓存
- 格式一致转换：使用流拷贝模式
- 多核CPU：适当增加FFmpeg线程数
- 网络存储：使用镜像路径暂存

### 3. 元数据管理

- **保留原信息**: 不勾选forceMeta选项
- **解决乱码**: 勾选forceMeta选项
- **自定义标签**: 在params中指定元数据字段

### 4. 文件管理

- **避免覆盖**: 默认不勾选覆盖选项
- **临时文件**: 启用.temp后缀防止意外中断
- **批量处理**: 使用镜像路径暂存提高效率

## 总结

AbstractFfmpegStrategy提供了完整的FFmpeg音频转换框架，具有以下特点：

1. **通用性**: 提供通用的音频转换功能，支持多种格式
2. **灵活性**: 丰富的参数配置，满足不同场景需求
3. **性能优化**: 流拷贝、原生寻址等优化提升转换效率
4. **易用性**: 自动扫描FFmpeg、悬浮提示、动态交互
5. **可扩展性**: 模板方法设计，子类可灵活扩展

子类通过继承AbstractFfmpegStrategy，可以快速实现特定的音频处理功能，如音频转换、CUE分轨等。

---

**相关文档**:
- [AudioConverterStrategy设计文档](AudioConverterStrategy.md)
- [CueSplitterStrategy设计文档](CueSplitterStrategy.md)

**文档版本**: 1.0  
**最后更新**: 2026-01-27  
**维护者**: FileEditTools Team
