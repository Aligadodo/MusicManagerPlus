package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.util.MetadataExtractor;
import com.filemanager.plugin.util.PlaylistGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件批量归档和移动策略插件
 * 支持复制/移动操作，多种路径模式选择
 */
public class FileMigratePlugin extends AbstractPlugin {

    public FileMigratePlugin() {
        super("file-migrate", "文件批量归档和移动", "文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择。", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("operationMode", "操作模式", "select", "移动 (MOVE)", "文件的操作方式", true,
            Arrays.asList("移动 (MOVE)", "复制 (COPY)"));
        addParameter("structureTemplate", "目录结构模板", "select", "艺术家/专辑", "目标目录结构模板", true,
            Arrays.asList("艺术家/专辑", "艺术家/专辑/年份", "艺术家/年份/专辑", "流派/艺术家/专辑", "自定义模板"));
        addParameter("customTemplate", "自定义模板", "text", "{artist}/{album}", "自定义目录结构模板（使用占位符：{artist}, {album}, {year}, {genre}, {track}）", false);
        addParameter("outputDirMode", "输出目录模式", "select", "子目录", "输出目录模式", true,
            Arrays.asList("子目录", "指定目录", "根目录"));
        addParameter("outputPath", "输出路径", "directory", "Archive", "目标路径", true);
        addParameter("scope", "生效范围", "select", "全部", "文件处理的生效范围", false,
            Arrays.asList("全部", "当前目录", "指定深度"));
        addParameter("depth", "深度值", "number", 0, "指定生效范围的深度值", false);
        addParameter("keepLargest", "保留最大文件", "boolean", true, "去重时保留最大的文件", false);
        addParameter("keepEarliest", "保留最早文件", "boolean", true, "去重时保留日期最早的文件", false);
        addParameter("keepExt", "优先后缀", "string", "wav", "去重时优先保留的文件后缀", false);
        addParameter("audioSpecial", "音频特殊处理", "boolean", true, "去重时对音频文件进行特殊处理", false);
        addParameter("validateMetadata", "验证元数据", "boolean", true, "验证元数据完整性", false);
        addParameter("preserveTimestamp", "保留时间戳", "boolean", true, "保留原始文件的时间戳", false);
        addParameter("generatePlaylist", "生成播放列表", "boolean", false, "生成播放列表文件", false);
        addParameter("playlistFormat", "播放列表格式", "select", "M3U", "播放列表格式", false,
            Arrays.asList("M3U", "PLS", "WPL"));
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("operationMode", "移动 (MOVE)");
        setDefaultConfigValue("structureTemplate", "艺术家/专辑");
        setDefaultConfigValue("customTemplate", "{artist}/{album}");
        setDefaultConfigValue("outputDirMode", "子目录");
        setDefaultConfigValue("outputPath", "Archive");
        setDefaultConfigValue("scope", "全部");
        setDefaultConfigValue("depth", 0);
        setDefaultConfigValue("keepLargest", true);
        setDefaultConfigValue("keepEarliest", true);
        setDefaultConfigValue("keepExt", "wav");
        setDefaultConfigValue("audioSpecial", true);
        setDefaultConfigValue("validateMetadata", true);
        setDefaultConfigValue("preserveTimestamp", true);
        setDefaultConfigValue("generatePlaylist", false);
        setDefaultConfigValue("playlistFormat", "M3U");
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String operationMode = getConfigValue(config, "operationMode", "移动 (MOVE)");
        String outputPath = getTargetPath(filePath, config, context);
        
        ChangeRecord record = createChangeRecord(filePath, outputPath, "PENDING");
        record.setOperationType(operationMode.equals("移动 (MOVE)") ? "MOVE" : "COPY");
        record.setReason("操作模式: " + operationMode);
        return record;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = super.execute(filePaths, config, context);
        
        // 生成播放列表
        generatePlaylist(changes, config, context);
        
        return changes;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String operationMode = getConfigValue(config, "operationMode", "移动 (MOVE)");
        String outputPath = getTargetPath(filePath, config, context);
        boolean preserveTimestamp = getConfigValue(config, "preserveTimestamp", true);
        boolean validateMetadata = getConfigValue(config, "validateMetadata", true);
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        // 验证元数据（如果启用）
        if (validateMetadata && isAudioFile(sourceFile)) {
            if (!validateMetadata(sourceFile, context)) {
                context.logWarn("Metadata validation failed for: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
        }
        
        try {
            File targetFile = new File(outputPath);
            
            // 创建目标目录
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
                context.logDebug("Created directory: " + targetFile.getParentFile().getPath());
            }
            
            // 处理文件冲突
            if (targetFile.exists()) {
                context.logDebug("Target file already exists: " + outputPath);
                if (!handleFileConflict(sourceFile, targetFile, config, context)) {
                    return createChangeRecord(filePath, filePath, "SKIPPED");
                }
            }
            
            // 执行文件操作
            if ("移动 (MOVE)".equals(operationMode)) {
                Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                context.logInfo("Moved file: " + filePath + " -> " + outputPath);
            } else {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                context.logInfo("Copied file: " + filePath + " -> " + outputPath);
            }
            
            // 保留时间戳（如果启用）
            if (preserveTimestamp) {
                preserveTimestamp(sourceFile, targetFile, context);
            }
            
            ChangeRecord record = createChangeRecord(filePath, outputPath, "SUCCESS");
            record.setOperationType(operationMode.equals("移动 (MOVE)") ? "MOVE" : "COPY");
            record.setReason("操作模式: " + operationMode);
            return record;
        } catch (Exception e) {
            context.logError("Error migrating file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    /**
     * 获取目标路径
     */
    private String getTargetPath(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String structureTemplate = getConfigValue(config, "structureTemplate", "艺术家/专辑");
        String customTemplate = getConfigValue(config, "customTemplate", "{artist}/{album}");
        String outputDirMode = getConfigValue(config, "outputDirMode", "子目录");
        String outputPath = getConfigValue(config, "outputPath", "Archive");
        String scope = getConfigValue(config, "scope", "全部");
        int depth = getConfigValue(config, "depth", 0);
        
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        
        // 提取元数据
        Map<String, String> metadata = MetadataExtractor.extractMetadata(sourceFile);
        
        // 根据目录结构模板生成目标路径
        String templatePath = applyTemplate(structureTemplate, customTemplate, metadata, fileName);
        
        // 根据输出目录模式确定目标路径
        switch (outputDirMode) {
            case "子目录":
                // 在源文件所在目录下创建子目录
                File sourceDir = sourceFile.getParentFile();
                if (sourceDir != null) {
                    return sourceDir.getPath() + File.separator + outputPath + File.separator + templatePath + File.separator + fileName;
                }
                return outputPath + File.separator + templatePath + File.separator + fileName;
            case "指定目录":
                // 直接使用指定的输出目录
                return outputPath + File.separator + templatePath + File.separator + fileName;
            case "根目录":
                // 使用源文件的根目录
                String rootPath = getRootPath(sourceFile, scope, depth);
                return rootPath + File.separator + outputPath + File.separator + templatePath + File.separator + fileName;
            default:
                return outputPath + File.separator + templatePath + File.separator + fileName;
        }
    }

    /**
     * 应用目录结构模板
     */
    private String applyTemplate(String structureTemplate, String customTemplate, Map<String, String> metadata, String fileName) {
        String template;
        
        switch (structureTemplate) {
            case "艺术家/专辑":
                template = "{artist}/{album}";
                break;
            case "艺术家/专辑/年份":
                template = "{artist}/{album}/{year}";
                break;
            case "艺术家/年份/专辑":
                template = "{artist}/{year}/{album}";
                break;
            case "流派/艺术家/专辑":
                template = "{genre}/{artist}/{album}";
                break;
            case "自定义模板":
                template = customTemplate;
                break;
            default:
                template = "{artist}/{album}";
        }
        
        // 替换占位符
        String result = template;
        result = result.replace("{artist}", MetadataExtractor.getArtist(metadata));
        result = result.replace("{album}", MetadataExtractor.getAlbum(metadata));
        result = result.replace("{year}", MetadataExtractor.getYear(metadata));
        result = result.replace("{genre}", MetadataExtractor.getGenre(metadata));
        result = result.replace("{track}", MetadataExtractor.getTrack(metadata));
        
        return result;
    }

    /**
     * 获取根路径
     */
    private String getRootPath(File file, String scope, int depth) {
        switch (scope) {
            case "当前目录":
                return file.getParent();
            case "指定深度":
                return getAncestorPath(file, depth);
            case "全部":
            default:
                File parent = file;
                while (parent.getParent() != null) {
                    parent = parent.getParentFile();
                }
                return parent.getPath();
        }
    }

    /**
     * 获取指定深度的祖先路径
     */
    private String getAncestorPath(File file, int depth) {
        File ancestor = file;
        for (int i = 0; i < depth && ancestor.getParent() != null; i++) {
            ancestor = ancestor.getParentFile();
        }
        return ancestor.getPath();
    }

    /**
     * 检查是否为音频文件
     */
    private boolean isAudioFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".flac") || 
               fileName.endsWith(".wav") || fileName.endsWith(".aac") ||
               fileName.endsWith(".ogg") || fileName.endsWith(".wma") ||
               fileName.endsWith(".m4a");
    }

    /**
     * 验证元数据
     */
    private boolean validateMetadata(File file, ExecutionContext context) {
        context.logDebug("Validating metadata for: " + file.getName());
        
        Map<String, String> metadata = MetadataExtractor.extractMetadata(file);
        boolean isValid = MetadataExtractor.validateMetadata(metadata);
        
        if (!isValid) {
            context.logWarn("Metadata validation failed for: " + file.getName());
            context.logDebug("Artist: " + MetadataExtractor.getArtist(metadata));
            context.logDebug("Album: " + MetadataExtractor.getAlbum(metadata));
        }
        
        return isValid;
    }

    /**
     * 处理文件冲突
     */
    private boolean handleFileConflict(File sourceFile, File targetFile, PluginConfigDTO config, ExecutionContext context) {
        boolean keepLargest = getConfigValue(config, "keepLargest", true);
        boolean keepEarliest = getConfigValue(config, "keepEarliest", true);
        String keepExt = getConfigValue(config, "keepExt", "wav");
        boolean audioSpecial = getConfigValue(config, "audioSpecial", true);
        
        // 简化处理：如果源文件更大或更新，则覆盖
        // 实际实现需要根据配置参数进行更复杂的判断
        
        if (keepLargest) {
            if (sourceFile.length() > targetFile.length()) {
                context.logInfo("Source file is larger, will overwrite");
                return true;
            }
        }
        
        if (keepEarliest) {
            if (sourceFile.lastModified() < targetFile.lastModified()) {
                context.logInfo("Source file is older, will overwrite");
                return true;
            }
        }
        
        // 检查文件后缀
        String sourceExt = getFileExtension(sourceFile);
        String targetExt = getFileExtension(targetFile);
        if (keepExt != null && !keepExt.isEmpty()) {
            if (keepExt.equals(sourceExt) && !keepExt.equals(targetExt)) {
                context.logInfo("Source file has preferred extension, will overwrite");
                return true;
            }
        }
        
        // 音频文件特殊处理
        if (audioSpecial && isAudioFile(sourceFile) && isAudioFile(targetFile)) {
            // 这里可以添加音频文件的特殊处理逻辑
            // 例如：比较音频时长、比特率等
        }
        
        context.logWarn("Skipping file due to conflict: " + sourceFile.getName());
        return false;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 保留时间戳
     */
    private void preserveTimestamp(File sourceFile, File targetFile, ExecutionContext context) throws IOException {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(sourceFile.toPath());
            Files.setLastModifiedTime(targetFile.toPath(), lastModifiedTime);
            context.logDebug("Preserved timestamp for: " + targetFile.getName());
        } catch (Exception e) {
            context.logWarn("Failed to preserve timestamp: " + e.getMessage());
        }
    }

    /**
     * 生成播放列表
     */
    private void generatePlaylist(List<ChangeRecord> records, PluginConfigDTO config, ExecutionContext context) {
        boolean generatePlaylist = getConfigValue(config, "generatePlaylist", false);
        if (!generatePlaylist) {
            return;
        }
        
        String playlistFormat = getConfigValue(config, "playlistFormat", "M3U");
        String outputPath = getConfigValue(config, "outputPath", "Archive");
        String structureTemplate = getConfigValue(config, "structureTemplate", "艺术家/专辑");
        
        context.logInfo("Generating " + playlistFormat + " playlist...");
        
        try {
            // 收集所有成功迁移的文件路径
            List<String> trackPaths = new ArrayList<>();
            for (ChangeRecord record : records) {
                if ("SUCCESS".equals(record.getStatus()) && record.getNewName() != null) {
                    trackPaths.add(record.getNewName());
                }
            }
            
            if (trackPaths.isEmpty()) {
                context.logWarn("No tracks to generate playlist");
                return;
            }
            
            // 根据目录结构模板生成播放列表
            generatePlaylistsByStructure(trackPaths, outputPath, structureTemplate, playlistFormat, context);
            
        } catch (Exception e) {
            context.logError("Failed to generate playlist: " + e.getMessage());
        }
    }

    /**
     * 根据目录结构生成播放列表
     */
    private void generatePlaylistsByStructure(List<String> trackPaths, String outputPath, 
            String structureTemplate, String playlistFormat, ExecutionContext context) throws IOException {
        
        switch (structureTemplate) {
            case "艺术家/专辑":
                // 按艺术家和专辑生成播放列表
                generatePlaylistsByArtistAlbum(trackPaths, outputPath, playlistFormat, context);
                break;
            case "艺术家/专辑/年份":
                // 按艺术家、专辑和年份生成播放列表
                generatePlaylistsByArtistAlbumYear(trackPaths, outputPath, playlistFormat, context);
                break;
            case "艺术家/年份/专辑":
                // 按艺术家、年份和专辑生成播放列表
                generatePlaylistsByArtistYearAlbum(trackPaths, outputPath, playlistFormat, context);
                break;
            case "流派/艺术家/专辑":
                // 按流派、艺术家和专辑生成播放列表
                generatePlaylistsByGenreArtistAlbum(trackPaths, outputPath, playlistFormat, context);
                break;
            default:
                // 生成单个播放列表
                File playlistFile = new File(outputPath + File.separator + "playlist." + playlistFormat.toLowerCase());
                PlaylistGenerator.generatePlaylist(playlistFile, trackPaths, playlistFormat);
                context.logInfo("Generated playlist: " + playlistFile.getPath());
        }
    }

    /**
     * 按艺术家和专辑生成播放列表
     */
    private void generatePlaylistsByArtistAlbum(List<String> trackPaths, String outputPath, 
            String playlistFormat, ExecutionContext context) throws IOException {
        
        Map<String, Map<String, List<String>>> artistAlbumMap = new HashMap<>();
        
        // 按艺术家和专辑分组
        for (String trackPath : trackPaths) {
            File trackFile = new File(trackPath);
            Map<String, String> metadata = MetadataExtractor.extractMetadata(trackFile);
            
            String artist = MetadataExtractor.getArtist(metadata);
            String album = MetadataExtractor.getAlbum(metadata);
            
            artistAlbumMap.putIfAbsent(artist, new HashMap<>());
            artistAlbumMap.get(artist).putIfAbsent(album, new ArrayList<>());
            artistAlbumMap.get(artist).get(album).add(trackPath);
        }
        
        // 为每个专辑生成播放列表
        for (Map.Entry<String, Map<String, List<String>>> artistEntry : artistAlbumMap.entrySet()) {
            String artist = artistEntry.getKey();
            File artistDir = new File(outputPath + File.separator + artist);
            
            for (Map.Entry<String, List<String>> albumEntry : artistEntry.getValue().entrySet()) {
                String album = albumEntry.getKey();
                List<String> albumTracks = albumEntry.getValue();
                
                File playlistFile = new File(artistDir, album + "." + playlistFormat.toLowerCase());
                if (!playlistFile.getParentFile().exists()) {
                    playlistFile.getParentFile().mkdirs();
                }
                
                PlaylistGenerator.generatePlaylist(playlistFile, albumTracks, playlistFormat);
                context.logInfo("Generated playlist: " + playlistFile.getPath());
            }
        }
    }

    /**
     * 按艺术家、专辑和年份生成播放列表
     */
    private void generatePlaylistsByArtistAlbumYear(List<String> trackPaths, String outputPath, 
            String playlistFormat, ExecutionContext context) throws IOException {
        
        Map<String, Map<String, Map<String, List<String>>>> artistAlbumYearMap = new HashMap<>();
        
        // 按艺术家、专辑和年份分组
        for (String trackPath : trackPaths) {
            File trackFile = new File(trackPath);
            Map<String, String> metadata = MetadataExtractor.extractMetadata(trackFile);
            
            String artist = MetadataExtractor.getArtist(metadata);
            String album = MetadataExtractor.getAlbum(metadata);
            String year = MetadataExtractor.getYear(metadata);
            
            artistAlbumYearMap.putIfAbsent(artist, new HashMap<>());
            artistAlbumYearMap.get(artist).putIfAbsent(album, new HashMap<>());
            artistAlbumYearMap.get(artist).get(album).putIfAbsent(year, new ArrayList<>());
            artistAlbumYearMap.get(artist).get(album).get(year).add(trackPath);
        }
        
        // 为每个专辑生成播放列表
        for (Map.Entry<String, Map<String, Map<String, List<String>>>> artistEntry : artistAlbumYearMap.entrySet()) {
            String artist = artistEntry.getKey();
            File artistDir = new File(outputPath + File.separator + artist);
            
            for (Map.Entry<String, Map<String, List<String>>> albumEntry : artistEntry.getValue().entrySet()) {
                String album = albumEntry.getKey();
                File albumDir = new File(artistDir, album);
                
                for (Map.Entry<String, List<String>> yearEntry : albumEntry.getValue().entrySet()) {
                    List<String> yearTracks = yearEntry.getValue();
                    
                    File playlistFile = new File(albumDir, "playlist." + playlistFormat.toLowerCase());
                    if (!playlistFile.getParentFile().exists()) {
                        playlistFile.getParentFile().mkdirs();
                    }
                    
                    PlaylistGenerator.generatePlaylist(playlistFile, yearTracks, playlistFormat);
                    context.logInfo("Generated playlist: " + playlistFile.getPath());
                }
            }
        }
    }

    /**
     * 按艺术家、年份和专辑生成播放列表
     */
    private void generatePlaylistsByArtistYearAlbum(List<String> trackPaths, String outputPath, 
            String playlistFormat, ExecutionContext context) throws IOException {
        
        Map<String, Map<String, Map<String, List<String>>>> artistYearAlbumMap = new HashMap<>();
        
        // 按艺术家、年份和专辑分组
        for (String trackPath : trackPaths) {
            File trackFile = new File(trackPath);
            Map<String, String> metadata = MetadataExtractor.extractMetadata(trackFile);
            
            String artist = MetadataExtractor.getArtist(metadata);
            String year = MetadataExtractor.getYear(metadata);
            String album = MetadataExtractor.getAlbum(metadata);
            
            artistYearAlbumMap.putIfAbsent(artist, new HashMap<>());
            artistYearAlbumMap.get(artist).putIfAbsent(year, new HashMap<>());
            artistYearAlbumMap.get(artist).get(year).putIfAbsent(album, new ArrayList<>());
            artistYearAlbumMap.get(artist).get(year).get(album).add(trackPath);
        }
        
        // 为每个专辑生成播放列表
        for (Map.Entry<String, Map<String, Map<String, List<String>>>> artistEntry : artistYearAlbumMap.entrySet()) {
            String artist = artistEntry.getKey();
            File artistDir = new File(outputPath + File.separator + artist);
            
            for (Map.Entry<String, Map<String, List<String>>> yearEntry : artistEntry.getValue().entrySet()) {
                String year = yearEntry.getKey();
                File yearDir = new File(artistDir, year);
                
                for (Map.Entry<String, List<String>> albumEntry : yearEntry.getValue().entrySet()) {
                    List<String> albumTracks = albumEntry.getValue();
                    
                    File playlistFile = new File(yearDir, albumEntry.getKey() + "." + playlistFormat.toLowerCase());
                    if (!playlistFile.getParentFile().exists()) {
                        playlistFile.getParentFile().mkdirs();
                    }
                    
                    PlaylistGenerator.generatePlaylist(playlistFile, albumTracks, playlistFormat);
                    context.logInfo("Generated playlist: " + playlistFile.getPath());
                }
            }
        }
    }

    /**
     * 按流派、艺术家和专辑生成播放列表
     */
    private void generatePlaylistsByGenreArtistAlbum(List<String> trackPaths, String outputPath, 
            String playlistFormat, ExecutionContext context) throws IOException {
        
        Map<String, Map<String, Map<String, List<String>>>> genreArtistAlbumMap = new HashMap<>();
        
        // 按流派、艺术家和专辑分组
        for (String trackPath : trackPaths) {
            File trackFile = new File(trackPath);
            Map<String, String> metadata = MetadataExtractor.extractMetadata(trackFile);
            
            String genre = MetadataExtractor.getGenre(metadata);
            String artist = MetadataExtractor.getArtist(metadata);
            String album = MetadataExtractor.getAlbum(metadata);
            
            genreArtistAlbumMap.putIfAbsent(genre, new HashMap<>());
            genreArtistAlbumMap.get(genre).putIfAbsent(artist, new HashMap<>());
            genreArtistAlbumMap.get(genre).get(artist).putIfAbsent(album, new ArrayList<>());
            genreArtistAlbumMap.get(genre).get(artist).get(album).add(trackPath);
        }
        
        // 为每个专辑生成播放列表
        for (Map.Entry<String, Map<String, Map<String, List<String>>>> genreEntry : genreArtistAlbumMap.entrySet()) {
            String genre = genreEntry.getKey();
            File genreDir = new File(outputPath + File.separator + genre);
            
            for (Map.Entry<String, Map<String, List<String>>> artistEntry : genreEntry.getValue().entrySet()) {
                String artist = artistEntry.getKey();
                File artistDir = new File(genreDir, artist);
                
                for (Map.Entry<String, List<String>> albumEntry : artistEntry.getValue().entrySet()) {
                    List<String> albumTracks = albumEntry.getValue();
                    
                    File playlistFile = new File(artistDir, albumEntry.getKey() + "." + playlistFormat.toLowerCase());
                    if (!playlistFile.getParentFile().exists()) {
                        playlistFile.getParentFile().mkdirs();
                    }
                    
                    PlaylistGenerator.generatePlaylist(playlistFile, albumTracks, playlistFormat);
                    context.logInfo("Generated playlist: " + playlistFile.getPath());
                }
            }
        }
    }
}
