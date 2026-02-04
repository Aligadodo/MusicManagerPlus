package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlbumDirNormalizePlugin implements IPlugin {
    @Override
    public String getId() {
        return "album-dir-normalize";
    }

    @Override
    public String getName() {
        return "专辑目录标准化插件";
    }

    @Override
    public String getDescription() {
        return "智能规范化专辑目录名称，支持多种命名模板、元数据提取、特殊字符清理等功能。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("template", "%artist% - %year% - %album%");
        config.setValue("customTemplate", "");
        config.setValue("cleanSpecialChars", true);
        config.setValue("removeYearPrefix", true);
        config.setValue("useConsensusMetadata", true);
        config.setValue("preserveOriginalName", false);
        config.setValue("validateAlbumInfo", true);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO templateParam = new PluginParameterDTO();
        templateParam.setName("template");
        templateParam.setLabel("命名模板");
        templateParam.setDescription("用于设置专辑目录的命名模板");
        templateParam.setType("select");
        templateParam.setDefaultValue("%artist% - %year% - %album%");
        templateParam.setRequired(true);
        templateParam.setOptions(new String[]{
            "%artist% - %year% - %album%",
            "[%year%] %artist% - %album%",
            "%artist%/%album% (%year%)",
            "%year% - %album% - %artist%",
            "%album% - %artist% [%year%]",
            "%artist% - %album%",
            "%album% (%year%)",
            "custom"
        });
        parameters.add(templateParam);
        
        PluginParameterDTO customTemplateParam = new PluginParameterDTO();
        customTemplateParam.setName("customTemplate");
        customTemplateParam.setLabel("自定义模板");
        customTemplateParam.setDescription("当选择自定义模板时，在此输入自定义命名规则");
        customTemplateParam.setType("text");
        customTemplateParam.setDefaultValue("");
        customTemplateParam.setRequired(false);
        parameters.add(customTemplateParam);
        
        PluginParameterDTO cleanSpecialCharsParam = new PluginParameterDTO();
        cleanSpecialCharsParam.setName("cleanSpecialChars");
        cleanSpecialCharsParam.setLabel("清理特殊字符");
        cleanSpecialCharsParam.setDescription("移除目录名称中的特殊字符");
        cleanSpecialCharsParam.setType("boolean");
        cleanSpecialCharsParam.setDefaultValue(true);
        cleanSpecialCharsParam.setRequired(false);
        parameters.add(cleanSpecialCharsParam);
        
        PluginParameterDTO removeYearPrefixParam = new PluginParameterDTO();
        removeYearPrefixParam.setName("removeYearPrefix");
        removeYearPrefixParam.setLabel("移除年份前缀");
        removeYearPrefixParam.setDescription("移除目录名称开头的年份前缀");
        removeYearPrefixParam.setType("boolean");
        removeYearPrefixParam.setDefaultValue(true);
        removeYearPrefixParam.setRequired(false);
        parameters.add(removeYearPrefixParam);
        
        PluginParameterDTO useConsensusMetadataParam = new PluginParameterDTO();
        useConsensusMetadataParam.setName("useConsensusMetadata");
        useConsensusMetadataParam.setLabel("使用共识元数据");
        useConsensusMetadataParam.setDescription("从目录内所有音频文件中提取元数据，使用出现频率最高的值");
        useConsensusMetadataParam.setType("boolean");
        useConsensusMetadataParam.setDefaultValue(true);
        useConsensusMetadataParam.setRequired(false);
        parameters.add(useConsensusMetadataParam);
        
        PluginParameterDTO preserveOriginalNameParam = new PluginParameterDTO();
        preserveOriginalNameParam.setName("preserveOriginalName");
        preserveOriginalNameParam.setLabel("保留原始目录名");
        preserveOriginalNameParam.setDescription("在重命名前创建原始目录名的备份");
        preserveOriginalNameParam.setType("boolean");
        preserveOriginalNameParam.setDefaultValue(false);
        preserveOriginalNameParam.setRequired(false);
        parameters.add(preserveOriginalNameParam);
        
        PluginParameterDTO validateAlbumInfoParam = new PluginParameterDTO();
        validateAlbumInfoParam.setName("validateAlbumInfo");
        validateAlbumInfoParam.setLabel("验证专辑信息");
        validateAlbumInfoParam.setDescription("检查专辑信息的完整性，跳过信息不完整的目录");
        validateAlbumInfoParam.setType("boolean");
        validateAlbumInfoParam.setDefaultValue(true);
        validateAlbumInfoParam.setRequired(false);
        parameters.add(validateAlbumInfoParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        String template = (String) config.getValue("template", "%artist% - %year% - %album%");
        String customTemplate = (String) config.getValue("customTemplate", "");
        boolean cleanSpecialChars = (Boolean) config.getValue("cleanSpecialChars", true);
        boolean removeYearPrefix = (Boolean) config.getValue("removeYearPrefix", true);
        boolean useConsensusMetadata = (Boolean) config.getValue("useConsensusMetadata", true);
        boolean preserveOriginalName = (Boolean) config.getValue("preserveOriginalName", false);
        boolean validateAlbumInfo = (Boolean) config.getValue("validateAlbumInfo", true);
        
        for (String filePath : filePaths) {
            File dir = new File(filePath);
            
            if (!dir.isDirectory()) {
                continue;
            }
            
            List<File> audioFiles = getAudioFiles(dir);
            
            if (audioFiles.isEmpty()) {
                continue;
            }
            
            Map<String, String> consensus;
            if (useConsensusMetadata) {
                consensus = extractConsensusMetadata(audioFiles);
            } else {
                consensus = extractMetadata(audioFiles.get(0));
            }
            
            if (validateAlbumInfo) {
                String artist = consensus.getOrDefault("artist", "");
                String album = consensus.getOrDefault("album", "");
                
                if (artist.isEmpty() || artist.equals("Unknown Artist") ||
                    album.isEmpty() || album.equals("Unknown Album")) {
                    continue;
                }
            }
            
            String actualTemplate = "custom".equals(template) ? customTemplate : template;
            if (actualTemplate == null || actualTemplate.trim().isEmpty()) {
                actualTemplate = "%artist% - %year% - %album%";
            }
            
            String newDirName = applyTemplate(actualTemplate, consensus);
            
            if (cleanSpecialChars) {
                newDirName = cleanDirectoryName(newDirName);
            }
            
            if (removeYearPrefix) {
                newDirName = removeYearPrefix(newDirName);
            }
            
            newDirName = newDirName.trim();
            if (newDirName.endsWith(" - ")) {
                newDirName = newDirName.substring(0, newDirName.length() - 3);
            }
            
            if (!dir.getName().equals(newDirName)) {
                ChangeRecord record = new ChangeRecord();
                record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
                record.setOriginalName(filePath);
                record.setNewName(dir.getParent() + File.separator + newDirName);
                record.setFilePath(filePath);
                record.setChanged(true);
                record.setOperationType(ChangeRecord.OperationType.ALBUM_RENAME);
                record.setStatus(ChangeRecord.ExecStatus.PENDING);
                changes.add(record);
            }
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }
    
    private List<File> getAudioFiles(File dir) {
        List<File> audioFiles = new ArrayList<>();
        String[] audioExtensions = {".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".wma"};
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    for (String ext : audioExtensions) {
                        if (fileName.endsWith(ext)) {
                            audioFiles.add(file);
                            break;
                        }
                    }
                }
            }
        }
        
        return audioFiles;
    }
    
    private Map<String, String> extractMetadata(File file) {
        Map<String, String> metadata = new HashMap<>();
        
        String fileName = file.getName();
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileName = fileName.substring(0, lastDotIndex);
        }
        
        if (fileName.contains(" - ")) {
            String[] parts = fileName.split(" - ");
            if (parts.length >= 2) {
                metadata.put("artist", parts[0].trim());
                metadata.put("album", parts[1].trim());
            }
        }
        
        String yearMatch = extractYear(fileName);
        if (yearMatch != null) {
            metadata.put("year", yearMatch);
        }
        
        return metadata;
    }
    
    private Map<String, String> extractConsensusMetadata(List<File> audioFiles) {
        Map<String, Integer> artists = new HashMap<>();
        Map<String, Integer> albums = new HashMap<>();
        Map<String, Integer> years = new HashMap<>();
        Map<String, Integer> genres = new HashMap<>();

        for (File file : audioFiles) {
            Map<String, String> metadata = extractMetadata(file);
            
            String artist = metadata.getOrDefault("artist", "");
            if (!artist.isEmpty()) {
                artists.merge(normalizeArtistName(artist), 1, Integer::sum);
            }
            
            String album = metadata.getOrDefault("album", "");
            if (!album.isEmpty()) {
                albums.merge(normalizeAlbumName(album), 1, Integer::sum);
            }
            
            String year = metadata.getOrDefault("year", "");
            if (!year.isEmpty()) {
                years.merge(year, 1, Integer::sum);
            }
            
            String genre = metadata.getOrDefault("genre", "");
            if (!genre.isEmpty()) {
                genres.merge(genre, 1, Integer::sum);
            }
        }

        Map<String, String> consensus = new HashMap<>();
        consensus.put("artist", getTopKey(artists, "Unknown Artist"));
        consensus.put("album", getTopKey(albums, "Unknown Album"));
        consensus.put("year", getTopKey(years, ""));
        consensus.put("genre", getTopKey(genres, ""));

        return consensus;
    }
    
    private String normalizeArtistName(String artist) {
        if (artist == null || artist.isEmpty()) {
            return "";
        }
        String normalized = artist.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("、", ",");
        normalized = normalized.replaceAll("，", ",");
        normalized = normalized.replaceAll("&", ",");
        normalized = normalized.replaceAll("feat\\..*", "");
        normalized = normalized.replaceAll("ft\\..*", "");
        normalized = normalized.replaceAll("\\(.*\\)", "");
        normalized = normalized.replaceAll("\\[.*\\]", "");
        return normalized.trim();
    }
    
    private String normalizeAlbumName(String album) {
        if (album == null || album.isEmpty()) {
            return "";
        }
        String normalized = album.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("\\(.*\\)", "");
        normalized = normalized.replaceAll("\\[.*\\]", "");
        normalized = normalized.replaceAll("-\\s*CD\\s*\\d+", "");
        normalized = normalized.replaceAll("-\\s*Disc\\s*\\d+", "");
        normalized = normalized.replaceAll("-\\s*Vol\\.?\\s*\\d+", "");
        return normalized.trim();
    }
    
    private String applyTemplate(String template, Map<String, String> metadata) {
        String result = template;
        
        result = result.replaceAll("%artist%", metadata.getOrDefault("artist", ""));
        result = result.replaceAll("%album%", metadata.getOrDefault("album", ""));
        result = result.replaceAll("%year%", metadata.getOrDefault("year", ""));
        result = result.replaceAll("%genre%", metadata.getOrDefault("genre", ""));
        
        return result;
    }
    
    private String cleanDirectoryName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String cleaned = name;
        cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|]", "-");
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = cleaned.replaceAll("[-_]{2,}", "-");
        cleaned = cleaned.trim();
        return cleaned;
    }
    
    private String removeYearPrefix(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String cleaned = name;
        cleaned = cleaned.replaceAll("^\\d{4}[-\\s]+", "");
        cleaned = cleaned.replaceAll("^\\d{4}\\.\\s+", "");
        return cleaned.trim();
    }
    
    private String extractYear(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
    
    private String getTopKey(Map<String, Integer> map, String def) {
        return map.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(def);
    }
}