package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CUE分轨策略插件
 * 解析CUE文件，智能定位音频源，基于时间戳调用FFmpeg精确切割，并写入元数据。
 */
public class CueSplitterPlugin extends AbstractPlugin {

    public CueSplitterPlugin() {
        super("cue-splitter", "CUE分轨", "解析CUE文件，智能定位音频源，基于时间戳调用FFmpeg精确切割，并写入元数据。", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("targetFormat", "目标格式", "select", "WAV (CD标准)", "转换后的音频文件格式", true,
            Arrays.asList("WAV (CD标准)", "FLAC", "WAV", "MP3", "ALAC", "AAC", "OGG"));
        addParameter("outputDirMode", "输出目录模式", "select", "子目录", "输出目录模式", true,
            Arrays.asList("子目录", "指定目录", "根目录"));
        addParameter("outputPath", "输出路径", "directory", "Split - WAV", "转换后文件的输出路径", true);
        addParameter("sampleRate", "采样率", "select", "44100", "转换后的音频采样率", false,
            Arrays.asList("保持原样 (Original)", "44100", "48000", "88200", "96000", "192000"));
        addParameter("channels", "声道数", "select", "2 (Stereo)", "转换后的音频声道数", false,
            Arrays.asList("保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)"));
        addParameter("overwrite", "强制覆盖", "boolean", false, "是否覆盖已存在的目标文件", false);
        addParameter("ffmpegThreads", "FFmpeg线程数", "number", 4, "FFmpeg的线程数", false);
        addParameter("ffmpegPath", "FFmpeg路径", "string", "ffmpeg", "FFmpeg可执行文件的路径", false);
        addParameter("enableCache", "启用临时文件缓存", "boolean", false, "启用临时文件缓存以缓解IO瓶颈", false);
        addParameter("cacheDir", "缓存目录", "directory", "", "临时文件缓存目录路径", false);
        addParameter("enableSnap", "启用镜像路径暂存", "boolean", false, "启用镜像路径暂存（需要手动移动文件）", false);
        addParameter("snapDir", "镜像存储目录", "directory", "", "镜像存储目录路径", false);
        addParameter("enableTempSuffix", "启用.temp文件后缀", "boolean", true, "启用.temp文件后缀（文件缓存启用时不生效）", false);
        addParameter("forceFilenameMeta", "忽略原始文件标签", "boolean", false, "忽略原始文件标签，强制用文件名重构元数据", false);
        addParameter("autoFormatFilename", "自动格式化目标文件名", "boolean", true, "自动将目标文件名转换为简体中文并去除首尾空格", false);
        addParameter("afterSplitAction", "切分后操作", "select", "什么都不做 (默认)", "切分完成后对原始文件的处理方式", false,
            Arrays.asList("什么都不做 (默认)", "删除原始文件", "归档原始文件"));
        addParameter("enableArchive", "启用归档目录", "boolean", false, "启用时，将原始文件移动到指定的归档目录", false);
        addParameter("archiveDir", "归档目录路径", "directory", "", "原始文件的归档目录路径", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("targetFormat", "WAV (CD标准)");
        setDefaultConfigValue("outputDirMode", "子目录");
        setDefaultConfigValue("outputPath", "Split - WAV");
        setDefaultConfigValue("sampleRate", "44100");
        setDefaultConfigValue("channels", "2 (Stereo)");
        setDefaultConfigValue("overwrite", false);
        setDefaultConfigValue("ffmpegThreads", 4);
        setDefaultConfigValue("ffmpegPath", "ffmpeg");
        setDefaultConfigValue("enableCache", false);
        setDefaultConfigValue("cacheDir", "");
        setDefaultConfigValue("enableSnap", false);
        setDefaultConfigValue("snapDir", "");
        setDefaultConfigValue("enableTempSuffix", true);
        setDefaultConfigValue("forceFilenameMeta", false);
        setDefaultConfigValue("autoFormatFilename", true);
        setDefaultConfigValue("afterSplitAction", "什么都不做 (默认)");
        setDefaultConfigValue("enableArchive", false);
        setDefaultConfigValue("archiveDir", "");
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String outputDirMode = getConfigValue(config, "outputDirMode", "子目录");
        String outputPath = getConfigValue(config, "outputPath", "Split - WAV");
        
        File sourceFile = new File(filePath);
        String targetPath;
        
        switch (outputDirMode) {
            case "子目录":
                File sourceDir = sourceFile.getParentFile();
                if (sourceDir != null) {
                    targetPath = sourceDir.getPath() + File.separator + outputPath;
                } else {
                    targetPath = outputPath;
                }
                break;
            case "指定目录":
                targetPath = outputPath;
                break;
            case "根目录":
                File rootPath = sourceFile;
                while (rootPath.getParent() != null) {
                    rootPath = rootPath.getParentFile();
                }
                targetPath = rootPath.getPath() + File.separator + outputPath;
                break;
            default:
                targetPath = outputPath;
                break;
        }
        
        ChangeRecord record = createChangeRecord(filePath, targetPath, "PENDING");
        record.setOperationType("SPLIT");
        record.setReason("CUE分轨");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        File cueFile = new File(filePath);
        
        // 检查是否为CUE文件
        if (!isCueFile(cueFile)) {
            context.logDebug("Not a CUE file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (!cueFile.exists()) {
            context.logWarn("CUE file does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            // 解析CUE文件
            CueSheet cueSheet = parseCueFile(cueFile, context);
            if (cueSheet == null || cueSheet.getTracks().isEmpty()) {
                context.logWarn("Failed to parse CUE file or no tracks found: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            // 定位音频源文件
            File audioFile = locateAudioFile(cueFile, cueSheet, context);
            if (audioFile == null || !audioFile.exists()) {
                context.logWarn("Audio source file not found for CUE: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            // 切分音轨
            List<ChangeRecord> trackRecords = splitTracks(audioFile, cueSheet, config, context);
            
            // 执行切分后操作
            executeAfterSplitAction(cueFile, audioFile, config, context);
            
            // 返回第一个音轨的记录
            if (!trackRecords.isEmpty()) {
                return trackRecords.get(0);
            }
            
            return createChangeRecord(filePath, filePath, "SUCCESS");
        } catch (Exception e) {
            context.logError("Error splitting CUE file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    /**
     * 检查是否为CUE文件
     */
    private boolean isCueFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".cue");
    }

    /**
     * 解析CUE文件
     */
    private CueSheet parseCueFile(File cueFile, ExecutionContext context) {
        CueSheet cueSheet = new CueSheet();
        cueSheet.setCueFile(cueFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(cueFile))) {
            String line;
            Track currentTrack = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty()) {
                    continue;
                }
                
                // 解析TITLE命令
                if (line.startsWith("TITLE ")) {
                    String title = extractQuotedString(line);
                    if (currentTrack != null) {
                        currentTrack.setTitle(title);
                    } else {
                        cueSheet.setAlbum(title);
                    }
                }
                // 解析PERFORMER命令
                else if (line.startsWith("PERFORMER ")) {
                    String performer = extractQuotedString(line);
                    if (currentTrack != null) {
                        currentTrack.setArtist(performer);
                    } else {
                        cueSheet.setArtist(performer);
                    }
                }
                // 解析FILE命令
                else if (line.startsWith("FILE ")) {
                    String fileName = extractQuotedString(line);
                    cueSheet.setAudioFileName(fileName);
                }
                // 解析TRACK命令
                else if (line.startsWith("TRACK ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        currentTrack = new Track();
                        currentTrack.setNumber(Integer.parseInt(parts[1]));
                        cueSheet.getTracks().add(currentTrack);
                    }
                }
                // 解析INDEX命令
                else if (line.startsWith("INDEX 01 ")) {
                    if (currentTrack != null) {
                        String timeStr = line.substring("INDEX 01 ".length()).trim();
                        currentTrack.setStartTime(parseTime(timeStr));
                    }
                }
            }
            
            // 计算每个音轨的结束时间
            calculateTrackEndTimes(cueSheet);
            
            context.logInfo("Parsed CUE file: " + cueFile.getName() + ", tracks: " + cueSheet.getTracks().size());
            return cueSheet;
        } catch (Exception e) {
            context.logError("Error parsing CUE file: " + e.getMessage());
            return null;
        }
    }

    /**
     * 提取引号中的字符串
     */
    private String extractQuotedString(String line) {
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 解析时间字符串（MM:SS:FF格式）
     */
    private long parseTime(String timeStr) {
        String[] parts = timeStr.split(":");
        if (parts.length == 3) {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            int frames = Integer.parseInt(parts[2]);
            
            // 假设每秒75帧（CD标准）
            long totalSeconds = minutes * 60 + seconds;
            long milliseconds = totalSeconds * 1000 + (frames * 1000 / 75);
            
            return milliseconds;
        }
        return 0;
    }

    /**
     * 计算音轨结束时间
     */
    private void calculateTrackEndTimes(CueSheet cueSheet) {
        List<Track> tracks = cueSheet.getTracks();
        for (int i = 0; i < tracks.size(); i++) {
            Track currentTrack = tracks.get(i);
            if (i < tracks.size() - 1) {
                Track nextTrack = tracks.get(i + 1);
                currentTrack.setEndTime(nextTrack.getStartTime());
            }
        }
    }

    /**
     * 定位音频源文件
     */
    private File locateAudioFile(File cueFile, CueSheet cueSheet, ExecutionContext context) {
        // 优先使用CUE文件中指定的音频文件名
        String audioFileName = cueSheet.getAudioFileName();
        if (audioFileName != null && !audioFileName.isEmpty()) {
            File audioFile = new File(cueFile.getParent(), audioFileName);
            if (audioFile.exists()) {
                context.logInfo("Found audio file from CUE: " + audioFile.getName());
                return audioFile;
            }
        }
        
        // 在同目录下查找音频文件
        File parentDir = cueFile.getParentFile();
        if (parentDir != null) {
            File[] audioFiles = parentDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".wav") || lowerName.endsWith(".flac") ||
                       lowerName.endsWith(".ape") || lowerName.endsWith(".wv") ||
                       lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a");
            });
            
            if (audioFiles != null && audioFiles.length > 0) {
                // 优先选择最大的文件（通常是CD镜像文件）
                File largestFile = audioFiles[0];
                for (File file : audioFiles) {
                    if (file.length() > largestFile.length()) {
                        largestFile = file;
                    }
                }
                context.logInfo("Found audio file in directory: " + largestFile.getName());
                return largestFile;
            }
        }
        
        return null;
    }

    /**
     * 切分音轨
     */
    private List<ChangeRecord> splitTracks(File audioFile, CueSheet cueSheet, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> records = new ArrayList<>();
        String outputDirMode = getConfigValue(config, "outputDirMode", "子目录");
        String outputPath = getConfigValue(config, "outputPath", "Split - WAV");
        String targetFormat = getConfigValue(config, "targetFormat", "WAV (CD标准)");
        boolean autoFormatFilename = getConfigValue(config, "autoFormatFilename", true);
        boolean overwrite = getConfigValue(config, "overwrite", false);
        boolean enableCache = getConfigValue(config, "enableCache", false);
        boolean enableSnap = getConfigValue(config, "enableSnap", false);
        boolean enableTempSuffix = getConfigValue(config, "enableTempSuffix", true);
        
        // 确定输出目录
        File outputDir = getOutputDirectory(audioFile, outputDirMode, outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // 切分每个音轨
        for (Track track : cueSheet.getTracks()) {
            try {
                String trackFileName = generateTrackFileName(track, targetFormat, autoFormatFilename);
                File trackFile = new File(outputDir, trackFileName);
                
                // 检查文件是否存在
                if (trackFile.exists() && !overwrite) {
                    context.logWarn("Track file already exists: " + trackFile.getName());
                    continue;
                }
                
                // 确定实际输出文件路径
                String actualOutputPath = trackFile.getPath();
                if (enableCache) {
                    String cacheDir = getConfigValue(config, "cacheDir", "");
                    actualOutputPath = cacheDir + File.separator + trackFileName;
                } else if (enableSnap) {
                    String snapDir = getConfigValue(config, "snapDir", "");
                    actualOutputPath = snapDir + File.separator + trackFileName;
                } else if (enableTempSuffix) {
                    actualOutputPath = trackFile.getPath() + ".temp";
                }
                
                File actualTrackFile = new File(actualOutputPath);
                
                // 执行音轨切分
                boolean success = splitTrack(audioFile, actualTrackFile, track, config, context);
                
                if (success) {
                    // 如果使用了临时文件后缀，重命名为最终文件名
                    if (enableTempSuffix && actualTrackFile.getName().endsWith(".temp")) {
                        File finalFile = new File(actualOutputPath.replace(".temp", ""));
                        Files.move(actualTrackFile.toPath(), finalFile.toPath());
                        actualOutputPath = finalFile.getPath();
                    }
                    
                    context.logInfo("Track split successful: " + track.getNumber() + " - " + track.getTitle());
                    
                    ChangeRecord record = createChangeRecord(audioFile.getPath(), actualOutputPath, "SUCCESS");
                    record.setOperationType("SPLIT");
                    record.setReason("音轨 " + track.getNumber() + ": " + track.getTitle());
                    records.add(record);
                }
            } catch (Exception e) {
                context.logError("Error splitting track " + track.getNumber() + ": " + e.getMessage());
            }
        }
        
        return records;
    }

    /**
     * 获取输出目录
     */
    private File getOutputDirectory(File audioFile, String outputDirMode, String outputPath) {
        switch (outputDirMode) {
            case "子目录":
                return new File(audioFile.getParent(), outputPath);
            case "指定目录":
                return new File(outputPath);
            case "根目录":
                File rootPath = audioFile;
                while (rootPath.getParent() != null) {
                    rootPath = rootPath.getParentFile();
                }
                return new File(rootPath, outputPath);
            default:
                return new File(audioFile.getParent(), outputPath);
        }
    }

    /**
     * 生成音轨文件名
     */
    private String generateTrackFileName(Track track, String targetFormat, boolean autoFormatFilename) {
        String artist = track.getArtist() != null ? track.getArtist() : "Unknown Artist";
        String title = track.getTitle() != null ? track.getTitle() : "Unknown Title";
        String album = track.getAlbum() != null ? track.getAlbum() : "Unknown Album";
        int trackNumber = track.getNumber();
        
        // 格式化文件名
        String fileName = String.format("%02d - %s - %s", trackNumber, artist, title);
        
        // 清理非法字符
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // 添加扩展名
        String extension = getFileExtension(targetFormat);
        fileName += "." + extension;
        
        // 格式化文件名
        if (autoFormatFilename) {
            fileName = formatFilename(fileName);
        }
        
        return fileName;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String format) {
        switch (format) {
            case "WAV (CD标准)":
            case "WAV":
                return "wav";
            case "FLAC":
                return "flac";
            case "MP3":
                return "mp3";
            case "ALAC":
                return "m4a";
            case "AAC":
                return "aac";
            case "OGG":
                return "ogg";
            default:
                return "wav";
        }
    }

    /**
     * 格式化文件名
     */
    private String formatFilename(String filename) {
        return filename.trim();
    }

    /**
     * 切分音轨
     */
    private boolean splitTrack(File audioFile, File trackFile, Track track, PluginConfigDTO config, ExecutionContext context) {
        String ffmpegPath = getConfigValue(config, "ffmpegPath", "ffmpeg");
        String sampleRate = getConfigValue(config, "sampleRate", "44100");
        String channels = getConfigValue(config, "channels", "2 (Stereo)");
        int ffmpegThreads = getConfigValue(config, "ffmpegThreads", 4);
        boolean forceFilenameMeta = getConfigValue(config, "forceFilenameMeta", false);
        
        try {
            // 构建FFmpeg命令
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(audioFile.getPath());
            
            // 添加起始时间
            long startTime = track.getStartTime();
            command.add("-ss");
            command.add(String.valueOf(startTime / 1000.0));
            
            // 添加持续时间
            long duration = track.getEndTime() - track.getStartTime();
            if (duration > 0) {
                command.add("-t");
                command.add(String.valueOf(duration / 1000.0));
            }
            
            // 添加采样率参数
            if (!"保持原样 (Original)".equals(sampleRate)) {
                command.add("-ar");
                command.add(sampleRate);
            }
            
            // 添加声道数参数
            if (!"保持原样 (Original)".equals(channels)) {
                command.add("-ac");
                command.add(String.valueOf(getChannelCount(channels)));
            }
            
            // 添加线程数参数
            command.add("-threads");
            command.add(String.valueOf(ffmpegThreads));
            
            // 添加元数据
            if (!forceFilenameMeta) {
                if (track.getArtist() != null) {
                    command.add("-metadata");
                    command.add("artist=" + track.getArtist());
                }
                if (track.getAlbum() != null) {
                    command.add("-metadata");
                    command.add("album=" + track.getAlbum());
                }
                if (track.getTitle() != null) {
                    command.add("-metadata");
                    command.add("title=" + track.getTitle());
                }
                command.add("-metadata");
                command.add("track=" + track.getNumber());
            }
            
            // 添加输出文件
            command.add(trackFile.getPath());
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                context.logDebug(line);
            }
            
            // 等待进程完成
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            context.logError("Error executing FFmpeg: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取声道数
     */
    private int getChannelCount(String channels) {
        switch (channels) {
            case "1 (Mono)":
                return 1;
            case "2 (Stereo)":
                return 2;
            case "6 (5.1)":
                return 6;
            default:
                return 2;
        }
    }

    /**
     * 执行切分后操作
     */
    private void executeAfterSplitAction(File cueFile, File audioFile, PluginConfigDTO config, ExecutionContext context) {
        String afterSplitAction = getConfigValue(config, "afterSplitAction", "什么都不做 (默认)");
        
        switch (afterSplitAction) {
            case "删除原始文件":
                deleteOriginalFiles(cueFile, audioFile, context);
                break;
            case "归档原始文件":
                archiveOriginalFiles(cueFile, audioFile, config, context);
                break;
            case "什么都不做 (默认)":
            default:
                // 什么都不做
                break;
        }
    }

    /**
     * 删除原始文件
     */
    private void deleteOriginalFiles(File cueFile, File audioFile, ExecutionContext context) {
        try {
            if (cueFile.exists()) {
                Files.delete(cueFile.toPath());
                context.logInfo("Deleted CUE file: " + cueFile.getName());
            }
            if (audioFile.exists()) {
                Files.delete(audioFile.toPath());
                context.logInfo("Deleted audio file: " + audioFile.getName());
            }
        } catch (IOException e) {
            context.logError("Error deleting original files: " + e.getMessage());
        }
    }

    /**
     * 归档原始文件
     */
    private void archiveOriginalFiles(File cueFile, File audioFile, PluginConfigDTO config, ExecutionContext context) {
        boolean enableArchive = getConfigValue(config, "enableArchive", false);
        if (!enableArchive) {
            return;
        }
        
        String archiveDir = getConfigValue(config, "archiveDir", "");
        if (archiveDir.isEmpty()) {
            context.logWarn("Archive directory not specified");
            return;
        }
        
        File archivePath = new File(archiveDir);
        if (!archivePath.exists()) {
            archivePath.mkdirs();
        }
        
        try {
            if (cueFile.exists()) {
                File archivedCueFile = new File(archivePath, cueFile.getName());
                Files.move(cueFile.toPath(), archivedCueFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                context.logInfo("Archived CUE file: " + cueFile.getName());
            }
            if (audioFile.exists()) {
                File archivedAudioFile = new File(archivePath, audioFile.getName());
                Files.move(audioFile.toPath(), archivedAudioFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                context.logInfo("Archived audio file: " + audioFile.getName());
            }
        } catch (IOException e) {
            context.logError("Error archiving original files: " + e.getMessage());
        }
    }

    /**
     * CUE文件数据结构
     */
    private static class CueSheet {
        private File cueFile;
        private String artist;
        private String album;
        private String audioFileName;
        private List<Track> tracks = new ArrayList<>();
        
        public File getCueFile() {
            return cueFile;
        }
        
        public void setCueFile(File cueFile) {
            this.cueFile = cueFile;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public void setArtist(String artist) {
            this.artist = artist;
        }
        
        public String getAlbum() {
            return album;
        }
        
        public void setAlbum(String album) {
            this.album = album;
        }
        
        public String getAudioFileName() {
            return audioFileName;
        }
        
        public void setAudioFileName(String audioFileName) {
            this.audioFileName = audioFileName;
        }
        
        public List<Track> getTracks() {
            return tracks;
        }
    }

    /**
     * 音轨数据结构
     */
    private static class Track {
        private int number;
        private String artist;
        private String album;
        private String title;
        private long startTime;
        private long endTime;
        
        public int getNumber() {
            return number;
        }
        
        public void setNumber(int number) {
            this.number = number;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public void setArtist(String artist) {
            this.artist = artist;
        }
        
        public String getAlbum() {
            return album;
        }
        
        public void setAlbum(String album) {
            this.album = album;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
    }
}
