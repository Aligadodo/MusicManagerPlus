package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.plugin.PluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StrategyServiceImpl implements StrategyService {

    private final Map<String, StrategyConfigDTO> strategyConfigs = new ConcurrentHashMap<>();
    private final Map<String, StrategyInfoDTO> strategies = new ConcurrentHashMap<>();

    @Autowired
    private PluginRegistry pluginRegistry;

    public StrategyServiceImpl() {
        // 初始化内置策略
        initBuiltInStrategies();
    }
    
    @PostConstruct
    private void initPluginStrategies() {
        // 从插件注册表加载策略
        List<com.filemanager.plugin.IPlugin> plugins = pluginRegistry.getAvailablePlugins();
        for (com.filemanager.plugin.IPlugin plugin : plugins) {
            StrategyInfoDTO strategy = new StrategyInfoDTO();
            strategy.setId(plugin.getId());
            strategy.setName(plugin.getName());
            strategy.setDescription(plugin.getDescription());
            strategy.setEnabled(true);
            
            // 转换插件参数为策略配置字段
            List<ConfigFieldDTO> configFields = new ArrayList<>();
            if (plugin.getParameters() != null) {
                for (com.filemanager.domain.dto.PluginParameterDTO param : plugin.getParameters()) {
                    ConfigFieldDTO field = new ConfigFieldDTO(
                        param.getName(),
                        param.getLabel(),
                        param.getType(),
                        param.getDefaultValue(),
                        param.getDescription(),
                        param.isRequired()
                    );
                    configFields.add(field);
                }
            }
            strategy.setConfigFields(configFields);
            strategies.put(strategy.getId(), strategy);
        }
    }

    private void initBuiltInStrategies() {
        // 1. AdvancedRenameStrategy - 高级重命名策略
        StrategyInfoDTO advancedRenameStrategy = new StrategyInfoDTO();
        advancedRenameStrategy.setId("advanced-rename");
        advancedRenameStrategy.setName("高级重命名策略");
        advancedRenameStrategy.setDescription("基于规则的高级文件重命名功能，支持多种条件和操作");
        advancedRenameStrategy.setEnabled(true);
        advancedRenameStrategy.setConfigFields(Arrays.asList(
            createConfigField("crossDriveMode", "跨盘动作", "select", "移动 (Move)", 
                "跨盘操作时的动作", false, 
                Arrays.asList("移动 (Move)", "复制 (Copy)")),
            createConfigField("processScope", "处理范围", "select", "全部处理", 
                "处理的文件类型范围", false, 
                Arrays.asList("仅处理文件", "仅处理文件夹", "全部处理")),
            createConfigField("rules", "重命名规则", "list", new ArrayList<>(), 
                "重命名规则列表", false)
        ));
        strategies.put(advancedRenameStrategy.getId(), advancedRenameStrategy);

        // 2. AudioConverterStrategy - 音频格式转换策略
        StrategyInfoDTO audioConverterStrategy = new StrategyInfoDTO();
        audioConverterStrategy.setId("audio-converter");
        audioConverterStrategy.setName("音频格式转换");
        audioConverterStrategy.setDescription("高品质音频转换。支持参数微调、乱码修复及智能覆盖检测等。");
        audioConverterStrategy.setEnabled(true);
        audioConverterStrategy.setConfigFields(Arrays.asList(
            createConfigField("targetFormat", "目标格式", "select", "WAV (CD标准)", 
                "转换后的音频文件格式", true, 
                Arrays.asList("WAV (CD标准)", "FLAC", "WAV", "MP3", "ALAC", "AAC", "OGG")),
            createConfigField("outputDirMode", "输出目录模式", "select", "子目录", 
                "输出目录模式", true, 
                Arrays.asList("子目录", "指定目录", "根目录")),
            createConfigField("outputPath", "输出路径", "directory", "Convert - WAV", 
                "转换后文件的输出路径", true),
            createConfigField("sampleRate", "采样率", "select", "44100", 
                "转换后的音频采样率", false, 
                Arrays.asList("保持原样 (Original)", "44100", "48000", "88200", "96000", "192000")),
            createConfigField("channels", "声道数", "select", "2 (Stereo)", 
                "转换后的音频声道数", false, 
                Arrays.asList("保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)")),
            createConfigField("overwrite", "强制覆盖", "boolean", false, 
                "是否覆盖已存在的目标文件", false),
            createConfigField("ffmpegThreads", "FFmpeg线程数", "number", 4, 
                "FFmpeg的线程数", false),
            createConfigField("ffmpegPath", "FFmpeg路径", "string", "ffmpeg", 
                "FFmpeg可执行文件的路径", false),
            createConfigField("enableCache", "启用临时文件缓存", "boolean", false, 
                "启用临时文件缓存以缓解IO瓶颈", false),
            createConfigField("cacheDir", "缓存目录", "directory", "", 
                "临时文件缓存目录路径", false),
            createConfigField("enableSnap", "启用镜像路径暂存", "boolean", false, 
                "启用镜像路径暂存（需要手动移动文件）", false),
            createConfigField("snapDir", "镜像存储目录", "directory", "", 
                "镜像存储目录路径", false),
            createConfigField("enableTempSuffix", "启用.temp文件后缀", "boolean", true, 
                "启用.temp文件后缀（文件缓存启用时不生效）", false),
            createConfigField("forceFilenameMeta", "忽略原始文件标签", "boolean", false, 
                "忽略原始文件标签，强制用文件名重构元数据", false),
            createConfigField("autoFormatFilename", "自动格式化目标文件名", "boolean", true, 
                "自动将目标文件名转换为简体中文并去除首尾空格", false),
            createConfigField("skipCueTracks", "智能跳过处理", "boolean", true, 
                "当音频文件大于100MB且同目录下有.cue文件时，跳过处理", false)
        ));
        strategies.put(audioConverterStrategy.getId(), audioConverterStrategy);

        // 3. FileCleanupStrategy - 文件清理与去重策略
        StrategyInfoDTO cleanupStrategy = new StrategyInfoDTO();
        cleanupStrategy.setId("file-cleanup");
        cleanupStrategy.setName("文件清理与去重");
        cleanupStrategy.setDescription("智能识别重复文件/文件夹、清理空目录、合并同名父子文件夹。支持按盘符结构伪删除。");
        cleanupStrategy.setEnabled(true);
        cleanupStrategy.setConfigFields(Arrays.asList(
            createConfigField("mode", "清理模式", "select", "文件去重", 
                "清理的逻辑规则", true, 
                Arrays.asList("文件去重", "文件夹去重", "清理空目录", "直接清理")),
            createConfigField("method", "删除方式", "select", "伪删除", 
                "删除的方式", true, 
                Arrays.asList("伪删除", "直接删除", "可回滚删除")),
            createConfigField("trashPath", "回收站路径", "string", ".EchoTrash", 
                "回收站的位置", false),
            createConfigField("keepLargest", "保留体积/质量最佳的副本", "boolean", true, 
                "保留最大的文件", false),
            createConfigField("keepEarliest", "保留日期最早/最晚的副本", "boolean", true, 
                "保留日期最早的文件", false),
            createConfigField("keepExt", "优先后缀", "string", "wav", 
                "去重时优先保留的文件后缀", false),
            createConfigField("preprocessLower", "文件名转小写", "boolean", true, 
                "将文件名转换为小写后进行比较", false),
            createConfigField("preprocessUpper", "文件名转大写", "boolean", false, 
                "将文件名转换为大写后进行比较", false),
            createConfigField("preprocessSimplified", "文件名转简体中文", "boolean", false, 
                "将文件名中的繁体中文转换为简体中文后进行比较", false),
            createConfigField("sizeRange", "文件大小范围", "select", "全部", 
                "要处理的文件大小范围", false, 
                Arrays.asList("全部", "小于1MB", "小于10MB", "小于100MB", "小于1GB", 
                          "大于1MB", "大于10MB", "大于100MB", "大于1GB")),
            createConfigField("audioSpecial", "音频文件特殊处理", "boolean", true, 
                "对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件", false)
        ));
        strategies.put(cleanupStrategy.getId(), cleanupStrategy);

        // 4. MetadataScraperStrategy - 元数据抓取策略
        StrategyInfoDTO metadataScraperStrategy = new StrategyInfoDTO();
        metadataScraperStrategy.setId("metadata-scraper");
        metadataScraperStrategy.setName("元数据抓取");
        metadataScraperStrategy.setDescription("从网络或本地抓取并更新文件的元数据信息");
        metadataScraperStrategy.setEnabled(true);
        metadataScraperStrategy.setConfigFields(Arrays.asList(
            createConfigField("source", "数据源", "select", "本地推断 (仅生成清单)", 
                "元数据数据源", true, 
                Arrays.asList("本地推断 (仅生成清单)", "网易云音乐 (中文歌曲) (不完善)", 
                          "咪咕音乐 (版权歌曲) (不完善)", "MusicBrainz (开源数据库)", 
                          "iTunes (苹果音乐)", "Last.fm (全球音乐平台) (不完善)", 
                          "Discogs (音乐数据库) (不完善)")),
            createConfigField("threads", "线程数", "number", 4, 
                "并发抓取的线程数", false),
            createConfigField("lyricsEnabled", "启用歌词模块", "boolean", true, 
                "是否启用歌词抓取", false),
            createConfigField("coverEnabled", "启用封面模块", "boolean", true, 
                "是否启用封面抓取", false),
            createConfigField("albumInfoEnabled", "启用专辑信息模块", "boolean", true, 
                "是否启用专辑信息抓取", false),
            createConfigField("maxRequests", "最大请求数", "number", 10, 
                "单位时间内的最大请求数", false),
            createConfigField("periodMs", "时间周期", "number", 1000, 
                "限流的时间周期（毫秒）", false)
        ));
        strategies.put(metadataScraperStrategy.getId(), metadataScraperStrategy);

        // 5. CueSplitterStrategy - CUE分轨策略
        StrategyInfoDTO cueSplitterStrategy = new StrategyInfoDTO();
        cueSplitterStrategy.setId("cue-splitter");
        cueSplitterStrategy.setName("CUE分轨");
        cueSplitterStrategy.setDescription("解析CUE文件，智能定位音频源，基于时间戳调用FFmpeg精确切割，并写入元数据。");
        cueSplitterStrategy.setEnabled(true);
        cueSplitterStrategy.setConfigFields(Arrays.asList(
            createConfigField("targetFormat", "目标格式", "select", "WAV (CD标准)", 
                "转换后的音频文件格式", true, 
                Arrays.asList("WAV (CD标准)", "FLAC", "WAV", "MP3", "ALAC", "AAC", "OGG")),
            createConfigField("outputDirMode", "输出目录模式", "select", "子目录", 
                "输出目录模式", true, 
                Arrays.asList("子目录", "指定目录", "根目录")),
            createConfigField("outputPath", "输出路径", "directory", "Split - WAV", 
                "转换后文件的输出路径", true),
            createConfigField("sampleRate", "采样率", "select", "44100", 
                "转换后的音频采样率", false, 
                Arrays.asList("保持原样 (Original)", "44100", "48000", "88200", "96000", "192000")),
            createConfigField("channels", "声道数", "select", "2 (Stereo)", 
                "转换后的音频声道数", false, 
                Arrays.asList("保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)")),
            createConfigField("overwrite", "强制覆盖", "boolean", false, 
                "是否覆盖已存在的目标文件", false),
            createConfigField("ffmpegThreads", "FFmpeg线程数", "number", 4, 
                "FFmpeg的线程数", false),
            createConfigField("ffmpegPath", "FFmpeg路径", "string", "ffmpeg", 
                "FFmpeg可执行文件的路径", false),
            createConfigField("enableCache", "启用临时文件缓存", "boolean", false, 
                "启用临时文件缓存以缓解IO瓶颈", false),
            createConfigField("cacheDir", "缓存目录", "directory", "", 
                "临时文件缓存目录路径", false),
            createConfigField("enableSnap", "启用镜像路径暂存", "boolean", false, 
                "启用镜像路径暂存（需要手动移动文件）", false),
            createConfigField("snapDir", "镜像存储目录", "directory", "", 
                "镜像存储目录路径", false),
            createConfigField("enableTempSuffix", "启用.temp文件后缀", "boolean", true, 
                "启用.temp文件后缀（文件缓存启用时不生效）", false),
            createConfigField("forceFilenameMeta", "忽略原始文件标签", "boolean", false, 
                "忽略原始文件标签，强制用文件名重构元数据", false),
            createConfigField("autoFormatFilename", "自动格式化目标文件名", "boolean", true, 
                "自动将目标文件名转换为简体中文并去除首尾空格", false),
            createConfigField("afterSplitAction", "切分后操作", "select", "什么都不做 (默认)", 
                "切分完成后对原始文件的处理方式", false, 
                Arrays.asList("什么都不做 (默认)", "删除原始文件", "归档原始文件")),
            createConfigField("enableArchive", "启用归档目录", "boolean", false, 
                "启用时，将原始文件移动到指定的归档目录", false),
            createConfigField("archiveDir", "归档目录路径", "directory", "", 
                "原始文件的归档目录路径", false)
        ));
        strategies.put(cueSplitterStrategy.getId(), cueSplitterStrategy);

        // 6. FileMigrateStrategy - 文件批量归档和移动策略
        StrategyInfoDTO fileMigrateStrategy = new StrategyInfoDTO();
        fileMigrateStrategy.setId("file-migrate");
        fileMigrateStrategy.setName("文件批量归档和移动");
        fileMigrateStrategy.setDescription("文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择。");
        fileMigrateStrategy.setEnabled(true);
        fileMigrateStrategy.setConfigFields(Arrays.asList(
            createConfigField("operationMode", "操作模式", "select", "移动 (MOVE)", 
                "文件的操作方式", true, 
                Arrays.asList("移动 (MOVE)", "复制 (COPY)")),
            createConfigField("outputDirMode", "输出目录模式", "select", "子目录", 
                "输出目录模式", true, 
                Arrays.asList("子目录", "指定目录", "根目录")),
            createConfigField("outputPath", "输出路径", "directory", "Archive", 
                "目标路径", true),
            createConfigField("scope", "生效范围", "select", "全部", 
                "文件处理的生效范围", false, 
                Arrays.asList("全部", "当前目录", "指定深度")),
            createConfigField("depth", "深度值", "number", 0, 
                "指定生效范围的深度值", false),
            createConfigField("keepLargest", "保留最大文件", "boolean", true, 
                "去重时保留最大的文件", false),
            createConfigField("keepEarliest", "保留最早文件", "boolean", true, 
                "去重时保留日期最早的文件", false),
            createConfigField("keepExt", "优先后缀", "string", "wav", 
                "去重时优先保留的文件后缀", false),
            createConfigField("audioSpecial", "音频特殊处理", "boolean", true, 
                "去重时对音频文件进行特殊处理", false)
        ));
        strategies.put(fileMigrateStrategy.getId(), fileMigrateStrategy);

        // 7. AlbumDirNormalizeStrategy - 专辑目录标准化策略
        StrategyInfoDTO albumDirNormalizeStrategy = new StrategyInfoDTO();
        albumDirNormalizeStrategy.setId("album-dir-normalize");
        albumDirNormalizeStrategy.setName("专辑目录标准化");
        albumDirNormalizeStrategy.setDescription("根据元数据标准化专辑目录结构，支持多种命名模板。");
        albumDirNormalizeStrategy.setEnabled(true);
        albumDirNormalizeStrategy.setConfigFields(Arrays.asList(
            createConfigField("template", "目录命名模板", "select", "%artist% - %year% - %album%", 
                "专辑目录的命名模板", true, 
                Arrays.asList("%artist% - %year% - %album%", "[%year%] %artist% - %album%", 
                          "%artist%/%album% (%year%)", "%year% - %album% - %artist%", 
                          "%album% - %artist% [%year%]", "%artist% - %album%", 
                          "%album% (%year%)", "自定义模板")),
            createConfigField("customTemplate", "自定义模板", "string", "", 
                "自定义命名模板", false),
            createConfigField("cleanSpecialChars", "清理特殊字符", "boolean", true, 
                "清理目录名中的特殊字符", false),
            createConfigField("removeYearPrefix", "移除年份前缀", "boolean", false, 
                "移除目录名中的年份前缀", false),
            createConfigField("useConsensusMetadata", "使用共识元数据", "boolean", true, 
                "使用多个文件的共识元数据", false),
            createConfigField("preserveOriginalName", "保留原始名称", "boolean", true, 
                "当无法获取元数据时保留原始目录名", false),
            createConfigField("validateAlbumInfo", "验证专辑信息", "boolean", true, 
                "验证专辑信息的完整性", false)
        ));
        strategies.put(albumDirNormalizeStrategy.getId(), albumDirNormalizeStrategy);

        // 8. FileUnzipStrategy - 批量智能解压策略
        StrategyInfoDTO fileUnzipStrategy = new StrategyInfoDTO();
        fileUnzipStrategy.setId("file-unzip");
        fileUnzipStrategy.setName("批量智能解压");
        fileUnzipStrategy.setDescription("批量智能解压工具，支持多种解压引擎和智能目录处理。");
        fileUnzipStrategy.setEnabled(true);
        fileUnzipStrategy.setConfigFields(Arrays.asList(
            createConfigField("engine", "解压引擎", "select", "Java 内置引擎", 
                "解压引擎选择", true, 
                Arrays.asList("Java 内置引擎", "7-Zip 引擎", "Bandizip 命令行工具")),
            createConfigField("exePath", "可执行文件路径", "string", "", 
                "外部解压工具的可执行文件路径", false),
            createConfigField("outputMode", "输出模式", "select", "自动创建子目录", 
                "输出目录模式", true, 
                Arrays.asList("自动创建子目录", "解压到当前目录", "指定目录")),
            createConfigField("customPath", "自定义路径", "directory", "", 
                "自定义输出路径", false),
            createConfigField("smartFolder", "智能文件夹", "boolean", true, 
                "智能识别解压后的文件夹结构", false),
            createConfigField("mergeSameName", "合并同名文件夹", "boolean", false, 
                "合并同名的文件夹", false),
            createConfigField("deleteSource", "解压成功后删除源文件", "boolean", false, 
                "解压成功后删除原始压缩文件", false),
            createConfigField("overwrite", "覆盖已存在文件", "boolean", false, 
                "覆盖已存在的文件", false),
            createConfigField("deleteOnFail", "解压失败后删除源文件", "boolean", false, 
                "解压失败后删除原始压缩文件", false),
            createConfigField("nestedFolderMerge", "嵌套文件夹合并", "boolean", false, 
                "合并嵌套的文件夹", false),
            createConfigField("passwords", "密码列表", "list", new ArrayList<>(), 
                "解压密码列表", false)
        ));
        strategies.put(fileUnzipStrategy.getId(), fileUnzipStrategy);

        // 9. FileCollectionStrategy - 文件收集策略
        StrategyInfoDTO fileCollectionStrategy = new StrategyInfoDTO();
        fileCollectionStrategy.setId("file-collection");
        fileCollectionStrategy.setName("文件收集策略");
        fileCollectionStrategy.setDescription("根据配置规则收集和整理文件");
        fileCollectionStrategy.setEnabled(true);
        fileCollectionStrategy.setConfigFields(Arrays.asList(
            createConfigField("targetDirectory", "目标目录", "directory", "/tmp/collected", 
                "文件收集的目标目录", true),
            createConfigField("recursive", "递归收集", "boolean", true, 
                "是否递归收集子目录中的文件", false)
        ));
        strategies.put(fileCollectionStrategy.getId(), fileCollectionStrategy);

        // 10. FileTypeFixStrategy - 文件类型修复策略
        StrategyInfoDTO fileTypeFixStrategy = new StrategyInfoDTO();
        fileTypeFixStrategy.setId("file-type-fix");
        fileTypeFixStrategy.setName("文件类型修复");
        fileTypeFixStrategy.setDescription("修复损坏或格式错误的文件");
        fileTypeFixStrategy.setEnabled(true);
        fileTypeFixStrategy.setConfigFields(Arrays.asList(
            createConfigField("targetFormat", "目标格式", "select", "自动检测", 
                "修复后的文件格式", true, 
                Arrays.asList("自动检测", "WAV", "FLAC", "MP3", "AAC", "OGG")),
            createConfigField("keepOriginal", "保留原始文件", "boolean", true, 
                "是否保留原始文件", false),
            createConfigField("backupOriginal", "备份原始文件", "boolean", true, 
                "是否备份原始文件", false)
        ));
        strategies.put(fileTypeFixStrategy.getId(), fileTypeFixStrategy);

        // 11. CueFileRenameStrategy - CUE文件重命名策略
        StrategyInfoDTO cueFileRenameStrategy = new StrategyInfoDTO();
        cueFileRenameStrategy.setId("cue-file-rename");
        cueFileRenameStrategy.setName("CUE文件重命名");
        cueFileRenameStrategy.setDescription("根据音频文件名或目录名重命名CUE文件");
        cueFileRenameStrategy.setEnabled(true);
        cueFileRenameStrategy.setConfigFields(Arrays.asList(
            createConfigField("renameMode", "重命名模式", "select", "基于音频文件名", 
                "CUE文件的重命名模式", true, 
                Arrays.asList("基于音频文件名", "基于目录名", "自定义")),
            createConfigField("customTemplate", "自定义模板", "string", "", 
                "自定义重命名模板", false)
        ));
        strategies.put(cueFileRenameStrategy.getId(), cueFileRenameStrategy);

        // 12. NcmIntegratedStrategy - 网易云音乐集成策略
        StrategyInfoDTO ncmIntegratedStrategy = new StrategyInfoDTO();
        ncmIntegratedStrategy.setId("ncm-integrated");
        ncmIntegratedStrategy.setName("网易云音乐集成");
        ncmIntegratedStrategy.setDescription("网易云音乐格式转换和元数据修复");
        ncmIntegratedStrategy.setEnabled(true);
        ncmIntegratedStrategy.setConfigFields(Arrays.asList(
            createConfigField("operationMode", "操作模式", "select", "转换", 
                "操作模式", true, 
                Arrays.asList("转换", "缓存转换", "歌词下载", "元数据修复")),
            createConfigField("outputFormat", "输出格式", "select", "MP3", 
                "输出格式", true, 
                Arrays.asList("MP3", "FLAC", "WAV")),
            createConfigField("outputDirectory", "输出目录", "directory", "", 
                "输出目录", false)
        ));
        strategies.put(ncmIntegratedStrategy.getId(), ncmIntegratedStrategy);
    }

    private ConfigFieldDTO createConfigField(String name, String label, String type, Object defaultValue, 
                                         String description, boolean required) {
        ConfigFieldDTO field = new ConfigFieldDTO(name, label, type, defaultValue, description, required);
        return field;
    }
    
    private ConfigFieldDTO createConfigField(String name, String label, String type, Object defaultValue, 
                                         String description, boolean required, List<String> options) {
        ConfigFieldDTO field = new ConfigFieldDTO(name, label, type, defaultValue, description, required);
        field.setOptions(options);
        return field;
    }

    @Override
    public List<StrategyInfoDTO> getAvailableStrategies() {
        return new ArrayList<>(strategies.values());
    }

    @Override
    public StrategyInfoDTO getStrategyInfo(String strategyId) {
        return strategies.get(strategyId);
    }

    @Override
    public StrategyConfigDTO getStrategyConfig(String strategyId) {
        StrategyConfigDTO config = strategyConfigs.get(strategyId);
        if (config == null) {
            config = new StrategyConfigDTO();
            // 设置默认配置
            switch (strategyId) {
                case "file-collection":
                    config.setValue("targetDirectory", "/tmp/collected");
                    config.setValue("recursive", true);
                    break;
                case "metadata-scraper":
                    config.setValue("source", "本地推断 (仅生成清单)");
                    config.setValue("threads", 4);
                    config.setValue("lyricsEnabled", true);
                    config.setValue("coverEnabled", true);
                    config.setValue("albumInfoEnabled", true);
                    config.setValue("maxRequests", 10);
                    config.setValue("periodMs", 1000);
                    break;
                case "file-cleanup":
                    config.setValue("mode", "文件去重");
                    config.setValue("method", "伪删除");
                    config.setValue("trashPath", ".EchoTrash");
                    config.setValue("keepLargest", true);
                    config.setValue("keepEarliest", true);
                    config.setValue("keepExt", "wav");
                    config.setValue("preprocessLower", true);
                    config.setValue("preprocessUpper", false);
                    config.setValue("preprocessSimplified", false);
                    config.setValue("sizeRange", "全部");
                    config.setValue("audioSpecial", true);
                    break;
                case "advanced-rename":
                    config.setValue("crossDriveMode", "移动 (Move)");
                    config.setValue("processScope", "全部处理");
                    config.setValue("rules", new ArrayList<>());
                    break;
                case "audio-converter":
                    config.setValue("targetFormat", "WAV (CD标准)");
                    config.setValue("outputDirMode", "子目录");
                    config.setValue("outputPath", "Convert - WAV");
                    config.setValue("sampleRate", "44100");
                    config.setValue("channels", "2 (Stereo)");
                    config.setValue("overwrite", false);
                    config.setValue("ffmpegThreads", 4);
                    config.setValue("ffmpegPath", "ffmpeg");
                    config.setValue("enableCache", false);
                    config.setValue("cacheDir", "");
                    config.setValue("enableSnap", false);
                    config.setValue("snapDir", "");
                    config.setValue("enableTempSuffix", true);
                    config.setValue("forceFilenameMeta", false);
                    config.setValue("autoFormatFilename", true);
                    config.setValue("skipCueTracks", true);
                    break;
                case "cue-splitter":
                    config.setValue("targetFormat", "WAV (CD标准)");
                    config.setValue("outputDirMode", "子目录");
                    config.setValue("outputPath", "Split - WAV");
                    config.setValue("sampleRate", "44100");
                    config.setValue("channels", "2 (Stereo)");
                    config.setValue("overwrite", false);
                    config.setValue("ffmpegThreads", 4);
                    config.setValue("ffmpegPath", "ffmpeg");
                    config.setValue("enableCache", false);
                    config.setValue("cacheDir", "");
                    config.setValue("enableSnap", false);
                    config.setValue("snapDir", "");
                    config.setValue("enableTempSuffix", true);
                    config.setValue("forceFilenameMeta", false);
                    config.setValue("autoFormatFilename", true);
                    config.setValue("afterSplitAction", "什么都不做 (默认)");
                    config.setValue("enableArchive", false);
                    config.setValue("archiveDir", "");
                    break;
                case "file-migrate":
                    config.setValue("operationMode", "移动 (MOVE)");
                    config.setValue("outputDirMode", "子目录");
                    config.setValue("outputPath", "Archive");
                    config.setValue("scope", "全部");
                    config.setValue("depth", 0);
                    config.setValue("keepLargest", true);
                    config.setValue("keepEarliest", true);
                    config.setValue("keepExt", "wav");
                    config.setValue("audioSpecial", true);
                    break;
                case "album-dir-normalize":
                    config.setValue("template", "%artist% - %year% - %album%");
                    config.setValue("customTemplate", "");
                    config.setValue("cleanSpecialChars", true);
                    config.setValue("removeYearPrefix", false);
                    config.setValue("useConsensusMetadata", true);
                    config.setValue("preserveOriginalName", true);
                    config.setValue("validateAlbumInfo", true);
                    break;
                case "file-unzip":
                    config.setValue("engine", "Java 内置引擎");
                    config.setValue("exePath", "");
                    config.setValue("outputMode", "自动创建子目录");
                    config.setValue("customPath", "");
                    config.setValue("smartFolder", true);
                    config.setValue("mergeSameName", false);
                    config.setValue("deleteSource", false);
                    config.setValue("overwrite", false);
                    config.setValue("deleteOnFail", false);
                    config.setValue("nestedFolderMerge", false);
                    config.setValue("passwords", new ArrayList<>());
                    break;
                case "file-type-fix":
                    config.setValue("targetFormat", "自动检测");
                    config.setValue("keepOriginal", true);
                    config.setValue("backupOriginal", true);
                    break;
                case "cue-file-rename":
                    config.setValue("renameMode", "基于音频文件名");
                    config.setValue("customTemplate", "");
                    break;
                case "ncm-integrated":
                    config.setValue("operationMode", "转换");
                    config.setValue("outputFormat", "MP3");
                    config.setValue("outputDirectory", "");
                    break;
            }
            strategyConfigs.put(strategyId, config);
        }
        return config;
    }

    @Override
    public boolean updateStrategyConfig(String strategyId, StrategyConfigDTO config) {
        strategyConfigs.put(strategyId, config);
        return true;
    }

    @Override
    public List<ChangeRecord> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfigDTO config) {
        // 尝试从插件系统获取对应的插件
        com.filemanager.plugin.IPlugin plugin = pluginRegistry.getPlugin(strategyId);
        if (plugin != null) {
            // 转换配置为插件配置
            com.filemanager.domain.dto.PluginConfigDTO pluginConfig = convertToPluginConfig(config);
            return plugin.execute(filePaths, pluginConfig, new com.filemanager.plugin.ExecutionContext());
        }
        
        // 如果没有对应的插件，使用默认实现
        List<ChangeRecord> changes = new ArrayList<>();
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getTargetPath(filePath, strategyId, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        return changes;
    }

    @Override
    public List<ChangeRecord> executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config) {
        // 尝试从插件系统获取对应的插件
        com.filemanager.plugin.IPlugin plugin = pluginRegistry.getPlugin(strategyId);
        if (plugin != null) {
            // 转换配置为插件配置
            com.filemanager.domain.dto.PluginConfigDTO pluginConfig = convertToPluginConfig(config);
            List<ChangeRecord> changes = plugin.execute(filePaths, pluginConfig, new com.filemanager.plugin.ExecutionContext());
            // 更新执行状态
            for (ChangeRecord record : changes) {
                record.setStatus(ChangeRecord.ExecStatus.SUCCESS);
            }
            return changes;
        }
        
        // 如果没有对应的插件，使用默认实现
        List<ChangeRecord> changes = analyzeFiles(strategyId, filePaths, config);
        for (ChangeRecord record : changes) {
            record.setStatus(ChangeRecord.ExecStatus.SUCCESS);
        }
        return changes;
    }

    private String getTargetPath(String filePath, String strategyId, StrategyConfigDTO config) {
        switch (strategyId) {
            case "file-collection":
                String targetDir = (String) config.getValue("targetDirectory");
                if (targetDir == null) {
                    targetDir = "/tmp/collected";
                }
                String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                return targetDir + "/" + fileName;
            case "metadata-scraper":
                return filePath; // 元数据策略不改变文件名
            case "file-cleanup":
                return null; // 清理策略删除文件
            case "file-migrate":
                String migrateDir = (String) config.getValue("outputPath");
                if (migrateDir == null) {
                    migrateDir = "Archive";
                }
                String migrateFileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                return migrateDir + "/" + migrateFileName;
            default:
                return filePath;
        }
    }

    private com.filemanager.domain.dto.PluginConfigDTO convertToPluginConfig(StrategyConfigDTO config) {
        com.filemanager.domain.dto.PluginConfigDTO pluginConfig = new com.filemanager.domain.dto.PluginConfigDTO();
        if (config.getConfigValues() != null) {
            for (Map.Entry<String, Object> entry : config.getConfigValues().entrySet()) {
                pluginConfig.setValue(entry.getKey(), entry.getValue());
            }
        }
        return pluginConfig;
    }
}
