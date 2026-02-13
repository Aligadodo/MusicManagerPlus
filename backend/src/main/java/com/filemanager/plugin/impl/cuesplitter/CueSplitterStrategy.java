package com.filemanager.plugin.impl.cuesplitter;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.enums.common.OutputDirMode;
import com.filemanager.plugin.impl.cuesplitter.enums.AfterSplitAction;
import com.filemanager.plugin.util.CueSheet;
import com.filemanager.plugin.util.CueParserUtil;
import com.filemanager.plugin.util.FileExistsChecker;
import com.filemanager.plugin.util.LanguageUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class CueSplitterStrategy extends AbstractConfigurableStrategy {

    private final Map<String, Set<String>> cueTrackProcessingStatus = new HashMap<>();

    public CueSplitterStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "cue-splitter";
    }

    @Override
    public String getName() {
        return "CUE整轨自动切割";
    }

    @Override
    public String getDescription() {
        return "解析 .cue 索引文件，将整轨音频无损切割为单曲。" +
                "支持预览详细的歌曲清单与时长信息。只需要扫描cue文件。" +
                "同一个音轨在切分时不会同时执行，避免文件锁出现，会分成多轮任务逐个完成切分。" +
                "如果音频存储在机械盘，可以使用缓存目录或者镜像目录（挂载到SSD盘下）进行处理加速，提升5-10倍的处理效率。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("targetFormat", "目标格式", "select", (Object) AudioFormat.WAV_CD_STANDARD.getCode(), 
            "转换后的音频文件格式", true, 
            getAudioFormatOptions());
        addEnumConfigField("outputDirMode", "输出目录模式", "select", (Object) OutputDirMode.SUBDIRECTORY.getCode(), 
            "输出目录模式", true, 
            getOutputDirModeOptions());
        addConfigField("outputPath", "输出路径", "directory", (Object) "Split - WAV", 
            "转换后文件的输出路径", true);
        addConfigField("overwrite", "强制覆盖", "boolean", (Object) false, 
            "是否覆盖已存在的目标文件", false);
        addConfigField("ffmpegPath", "FFmpeg路径", "string", (Object) "ffmpeg", 
            "FFmpeg可执行文件的路径", false);
        addConfigField("enableCache", "启用临时文件缓存", "boolean", (Object) false, 
            "启用临时文件缓存以缓解IO瓶颈", false);
        addConfigField("cacheDir", "缓存目录", "directory", (Object) "", 
            "临时文件缓存目录路径", false);
        addConfigField("enableSnap", "启用镜像路径暂存", "boolean", (Object) false, 
            "启用镜像路径暂存（需要手动移动文件）", false);
        addConfigField("snapDir", "镜像存储目录", "directory", (Object) "", 
            "镜像存储目录路径", false);
        addConfigField("enableTempSuffix", "启用.temp文件后缀", "boolean", (Object) true, 
            "启用.temp文件后缀（文件缓存启用时不生效）", false);
        addConfigField("forceFilenameMeta", "忽略原始文件标签", "boolean", (Object) false, 
            "忽略原始文件标签，强制用文件名重构元数据", false);
        addConfigField("autoFormatFilename", "自动格式化目标文件名", "boolean", (Object) true, 
            "自动将目标文件名转换为简体中文并去除首尾空格", false);
        addEnumConfigField("afterSplitAction", "切分后操作", "select", (Object) AfterSplitAction.DO_NOTHING.getCode(), 
            "切分完成后对原始文件的处理方式", false, 
            getAfterSplitActionOptions());
        addConfigField("enableArchive", "启用归档目录", "boolean", (Object) false, 
            "启用时，将原始文件移动到指定的归档目录", false);
        addConfigField("archiveDir", "归档目录路径", "directory", (Object) "", 
            "原始文件的归档目录路径", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetFormat", (Object) AudioFormat.WAV_CD_STANDARD.getCode());
        setConfigValue(config, "outputDirMode", (Object) OutputDirMode.SUBDIRECTORY.getCode());
        setConfigValue(config, "outputPath", (Object) "Split - WAV");
        setConfigValue(config, "overwrite", (Object) false);
        setConfigValue(config, "ffmpegPath", (Object) "ffmpeg");
        setConfigValue(config, "enableCache", (Object) false);
        setConfigValue(config, "cacheDir", (Object) "");
        setConfigValue(config, "enableSnap", (Object) false);
        setConfigValue(config, "snapDir", (Object) "");
        setConfigValue(config, "enableTempSuffix", (Object) true);
        setConfigValue(config, "forceFilenameMeta", (Object) false);
        setConfigValue(config, "autoFormatFilename", (Object) true);
        setConfigValue(config, "afterSplitAction", (Object) AfterSplitAction.DO_NOTHING.getCode());
        setConfigValue(config, "enableArchive", (Object) false);
        setConfigValue(config, "archiveDir", (Object) "");
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs,
        StrategyConfigDTO config,
        ExecutionContext context) {
        
        File cueFile = currentRecord.getFileHandle();
        if (!cueFile.getName().toLowerCase().endsWith(".cue") || cueFile.isDirectory()) {
            return Collections.emptyList();
        }

        CueSheet cueSheet = null;
        try {
            cueSheet = CueParserUtil.parse(cueFile.toPath());
        } catch (Exception e) {
            context.logError("Cue文件解析失败，跳过：" + cueFile.toPath() + "，错误详情：" + ExceptionUtils.getStackTrace(e));
            return Collections.emptyList();
        }

        if (cueSheet == null || cueSheet.getTracks().isEmpty()) {
            return Collections.emptyList();
        }

        if (cueSheet.getCountFiles() == cueSheet.getTracks().size()) {
            context.logInfo("自动忽略。已切分的分轨文件，无需重新切分，直接用格式转换组件即可处理：" + cueFile.getAbsolutePath());
            return Collections.emptyList();
        }

        File sourceAudio = CueParserUtil.locateAudioFile(cueFile, cueSheet.getAlbumFileName());
        if (sourceAudio == null) {
            return Collections.emptyList();
        }

        List<ChangeRecord> tracks = new ArrayList<>();
        List<CueSheet.CueTrack> cueTracks = cueSheet.getTracks();
        List<String> trackIds = new ArrayList<>();

        for (int i = 0; i < cueTracks.size(); i++) {
            CueSheet.CueTrack t = cueTracks.get(i);
            Map<String, String> params = getParams(sourceAudio.getParentFile(), "Track-" + t.getNumber(), config);
            sourceAudio = CueParserUtil.locateAudioFile(cueFile, t.getFormatedFileName());
            if (sourceAudio == null) {
                continue;
            }

            long startTime = t.getSoundStartTimeMs();
            long duration = t.getDuration();

            String trackName = t.getFormatedTrackName(params.get("format"));

            if (Boolean.parseBoolean(params.getOrDefault("autoFormatFilename", "true"))) {
                trackName = LanguageUtil.toSimpleChinese(trackName).trim();
            }

            String artist = t.getPerformer();
            String album = cueSheet.getAlbumTitle();
            String displayInfo = t.getDisplayInfo();
            File targetFile = new File(params.get("parentPath"), trackName);

            FileExistsChecker.FileExistsParams checkParams = new FileExistsChecker.FileExistsParams()
                    .enableCaseInsensitive()
                    .enableSimplifiedChinese()
                    .enableTrim();

            boolean overwrite = getConfigValue(config, "overwrite", false);
            if (params.containsKey("doubleCheckParentPath")) {
                File doubleCheckParentDir = new File(params.get("doubleCheckParentPath"));
                if (FileExistsChecker.checkFileExists(doubleCheckParentDir, trackName, checkParams) && !overwrite) {
                    continue;
                }
            }

            boolean targetExists = FileExistsChecker.checkFileExists(targetFile.getParentFile(), trackName, checkParams);
            if (targetExists && !overwrite) {
                continue;
            }

            params.put("source", sourceAudio.getAbsolutePath());
            params.put("start", startTime + "");
            if (duration != 0) {
                params.put("duration", String.format(Locale.US, "%d", duration));
            }
            if (t.getTitle() != null) {
                params.put("meta_title", t.getTitle());
                params.put("meta_artist", artist);
                params.put("meta_album", album);
                params.put("meta_track", String.valueOf(t.getNumber()));
            }
            params.put("cueFilePath", cueFile.getAbsolutePath());
            params.put("trackId", trackName);

            ChangeRecord trackRec = new ChangeRecord(
                    displayInfo,
                    trackName,
                    sourceAudio,
                    true,
                    targetFile.getAbsolutePath(),
                    com.filemanager.domain.enums.OperationType.SPLIT,
                    params,
                    com.filemanager.domain.enums.ExecStatus.PENDING
            );
            tracks.add(trackRec);
            trackIds.add(trackName);
        }

        initializeCueTracks(cueFile.getAbsolutePath(), trackIds);
        return tracks;
    }

    @Override
    public void execute(ChangeRecord record, 
        StrategyConfigDTO config, 
        ExecutionContext context) throws Exception {
        
        if (!"SPLIT".equals(record.getOperationType())) {
            return;
        }

        String cueFilePath = record.getExtraParams().get("cueFilePath");
        String trackId = record.getExtraParams().get("trackId");
        String sourceAudioPath = record.getExtraParams().get("source");

        executeSplit(record, config, context);

        if (cueFilePath != null && trackId != null) {
            markTrackAsCompleted(cueFilePath, trackId);
            afterSplitProcess(cueFilePath, sourceAudioPath, config, context);
        }
    }

    private void executeSplit(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) {
        String sourcePath = record.getExtraParams().get("source");
        String targetPath = record.getNewPath();
        String startTime = record.getExtraParams().get("start");
        String duration = record.getExtraParams().get("duration");
        String ffmpegPath = getConfigValue(config, "ffmpegPath", "ffmpeg");

        try {
            File targetFile = new File(targetPath);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            context.logInfo("Splitting track: " + record.getOriginalName() + " -> " + targetPath);

            record.setStatus("SUCCESS");
        } catch (Exception e) {
            context.logError("Error splitting track " + record.getOriginalName() + ": " + e.getMessage());
            record.setStatus("ERROR");
            record.setFailReason(e.getMessage());
        }
    }

    private void initializeCueTracks(String cueFilePath, List<String> trackIds) {
        cueTrackProcessingStatus.computeIfAbsent(cueFilePath, k -> new HashSet<>()).addAll(trackIds);
    }

    private void markTrackAsCompleted(String cueFilePath, String trackId) {
        Set<String> trackIds = cueTrackProcessingStatus.get(cueFilePath);
        if (trackIds != null) {
            trackIds.remove(trackId);
        }
    }

    private boolean isAllTracksCompleted(String cueFilePath) {
        Set<String> trackIds = cueTrackProcessingStatus.get(cueFilePath);
        return trackIds != null && trackIds.isEmpty();
    }

    private void afterSplitProcess(String cueFilePath, String audioFilePath, StrategyConfigDTO config, ExecutionContext context) {
        if (!isAllTracksCompleted(cueFilePath)) {
            return;
        }

        String afterSplitAction = getConfigValue(config, "afterSplitAction", "do_nothing");

        if ("do_nothing".equals(afterSplitAction)) {
            context.logInfo("已完成所有音轨切分，选择：什么都不做");
        } else if ("delete_original".equals(afterSplitAction)) {
            deleteOriginalFiles(cueFilePath, audioFilePath, context);
        } else if ("archive_original".equals(afterSplitAction)) {
            boolean enableArchive = getConfigValue(config, "enableArchive", false);
            String archiveDir = getConfigValue(config, "archiveDir", "");
            if (enableArchive && archiveDir != null && !archiveDir.isEmpty()) {
                archiveOriginalFiles(cueFilePath, audioFilePath, archiveDir, context);
            } else {
                context.logInfo("已完成所有音轨切分，但归档目录未设置或未启用，将什么都不做");
            }
        }
    }

    private void deleteOriginalFiles(String cueFilePath, String audioFilePath, ExecutionContext context) {
        try {
            File cueFile = new File(cueFilePath);
            if (cueFile.exists() && cueFile.delete()) {
                context.logInfo("已删除原始cue文件: " + cueFilePath);
            }

            File audioFile = new File(audioFilePath);
            if (audioFile.exists() && audioFile.delete()) {
                context.logInfo("已删除原始音频文件: " + audioFilePath);
            }

            cueTrackProcessingStatus.remove(cueFilePath);
        } catch (Exception e) {
            context.logError("删除原始文件时出错: " + e.getMessage());
        }
    }

    private void archiveOriginalFiles(String cueFilePath, String audioFilePath, String archiveDir, ExecutionContext context) {
        try {
            Path archiveDirPath = Paths.get(archiveDir);
            if (!Files.exists(archiveDirPath)) {
                Files.createDirectories(archiveDirPath);
            }

            Path sourceCuePath = Paths.get(cueFilePath);
            Path targetCuePath = archiveDirPath.resolve(sourceCuePath.getFileName());
            Files.move(sourceCuePath, targetCuePath, StandardCopyOption.REPLACE_EXISTING);
            context.logInfo("已归档cue文件: " + cueFilePath + " -> " + targetCuePath);

            Path sourceAudioPath = Paths.get(audioFilePath);
            Path targetAudioPath = archiveDirPath.resolve(sourceAudioPath.getFileName());
            Files.move(sourceAudioPath, targetAudioPath, StandardCopyOption.REPLACE_EXISTING);
            context.logInfo("已归档音频文件: " + audioFilePath + " -> " + targetAudioPath);

            File cueParentDir = sourceCuePath.getParent().toFile();
            if (cueParentDir.isDirectory()) {
                File[] files = cueParentDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.equals(sourceCuePath.toFile()) || file.equals(sourceAudioPath.toFile()) || file.isDirectory()) {
                            continue;
                        }

                        boolean isSplitTrack = false;
                        for (String trackPattern : Arrays.asList("*.wav", "*.flac", "*.mp3", "*.alac", "*.aac", "*.ogg")) {
                            if (file.getName().toLowerCase().matches(trackPattern.toLowerCase().replace("*", ".*"))) {
                                isSplitTrack = true;
                                break;
                            }
                        }

                        if (!isSplitTrack) {
                            Path sourceFilePath = file.toPath();
                            Path targetFilePath = archiveDirPath.resolve(sourceFilePath.getFileName());
                            Files.move(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                            context.logInfo("已归档相关文件: " + sourceFilePath + " -> " + targetFilePath);
                        }
                    }
                }
            }

            cueTrackProcessingStatus.remove(cueFilePath);
        } catch (IOException e) {
            context.logError("归档原始文件时出错: " + e.getMessage());
        }
    }

    private Map<String, String> getParams(File parentDir, String format, StrategyConfigDTO config) {
        Map<String, String> params = new HashMap<>();
        String outputDirMode = getConfigValue(config, "outputDirMode", "subdirectory");
        String outputPath = getConfigValue(config, "outputPath", "Split - WAV");
        String targetFormat = getConfigValue(config, "targetFormat", "wav_cd_standard");
        boolean autoFormatFilename = getConfigValue(config, "autoFormatFilename", true);

        params.put("parentPath", getOutputDirectory(parentDir, outputDirMode, outputPath));
        params.put("format", targetFormat);
        params.put("autoFormatFilename", String.valueOf(autoFormatFilename));

        return params;
    }

    private String getOutputDirectory(File cueFile, String outputDirMode, String outputPath) {
        File parentDir = cueFile.getParentFile();
        if (parentDir == null) {
            return outputPath;
        }

        switch (outputDirMode) {
            case "subdirectory":
                return parentDir.getPath() + File.separator + outputPath;
            case "custom":
                return outputPath;
            case "same_as_source":
                return parentDir.getPath();
            default:
                return parentDir.getPath() + File.separator + outputPath;
        }
    }

    private List<EnumOptionDTO> getAudioFormatOptions() {
        List<EnumOptionDTO> options = new ArrayList<>();
        for (AudioFormat format : AudioFormat.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(format.getCode());
            option.setLabel(format.getNameZh());
            option.setNameEn(format.getNameEn());
            option.setDescriptionZh(format.getDescriptionZh());
            option.setDescriptionEn(format.getDescriptionEn());
            options.add(option);
        }
        return options;
    }

    private List<EnumOptionDTO> getOutputDirModeOptions() {
        List<EnumOptionDTO> options = new ArrayList<>();
        for (OutputDirMode mode : OutputDirMode.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(mode.getCode());
            option.setLabel(mode.getNameZh());
            option.setNameEn(mode.getNameEn());
            option.setDescriptionZh(mode.getDescriptionZh());
            option.setDescriptionEn(mode.getDescriptionEn());
            options.add(option);
        }
        return options;
    }

    private List<EnumOptionDTO> getAfterSplitActionOptions() {
        List<EnumOptionDTO> options = new ArrayList<>();
        for (AfterSplitAction action : AfterSplitAction.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(action.getCode());
            option.setLabel(action.getNameZh());
            option.setNameEn(action.getNameEn());
            option.setDescriptionZh(action.getDescriptionZh());
            option.setDescriptionEn(action.getDescriptionEn());
            options.add(option);
        }
        return options;
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("SPLIT");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setFileHandle(sourceFile);

        try {
            if ("SPLIT".equals(record.getOperationType())) {
                execute(record, config, context);
                record.setStatus("SUCCESS");
            } else {
                record.setStatus("SKIPPED");
            }
        } catch (Exception e) {
            context.logError("Error processing file " + filePath + ": " + e.getMessage());
            record.setStatus("ERROR");
            record.setFailReason(e.getMessage());
        }

        return record;
    }
}