package com.filemanager.plugin.impl.albumdirnormalize;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.albumdirnormalize.enums.DirectoryTemplate;
import java.io.File;

public class AlbumDirNormalizeStrategy extends AbstractConfigurableStrategy {

    public AlbumDirNormalizeStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "album-dir-normalize";
    }

    @Override
    public String getName() {
        return "专辑目录标准化";
    }

    @Override
    public String getDescription() {
        return "根据元数据标准化专辑目录结构，支持多种命名模板。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public java.util.List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new java.util.ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("template", "目录命名模板", "select", (Object) DirectoryTemplate.ARTIST_YEAR_ALBUM.getCode(), 
            "专辑目录的命名模板", true, 
            getDirectoryTemplateOptions());
        addConfigField("customTemplate", "自定义模板", "string", (Object) "", 
            "自定义命名模板", false);
        addConfigField("cleanSpecialChars", "清理特殊字符", "boolean", (Object) true, 
            "清理目录名中的特殊字符", false);
        addConfigField("removeYearPrefix", "移除年份前缀", "boolean", (Object) false, 
            "移除目录名中的年份前缀", false);
        addConfigField("useConsensusMetadata", "使用共识元数据", "boolean", (Object) true, 
            "使用多个文件的共识元数据", false);
        addConfigField("preserveOriginalName", "保留原始名称", "boolean", (Object) true, 
            "当无法获取元数据时保留原始目录名", false);
        addConfigField("validateAlbumInfo", "验证专辑信息", "boolean", (Object) true, 
            "验证专辑信息的完整性", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "template", (Object) DirectoryTemplate.ARTIST_YEAR_ALBUM.getCode());
        setConfigValue(config, "customTemplate", (Object) "");
        setConfigValue(config, "cleanSpecialChars", (Object) true);
        setConfigValue(config, "removeYearPrefix", (Object) false);
        setConfigValue(config, "useConsensusMetadata", (Object) true);
        setConfigValue(config, "preserveOriginalName", (Object) true);
        setConfigValue(config, "validateAlbumInfo", (Object) true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String template = getConfigValue(config, "template", "artist_year_album");
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("RENAME");
        record.setReason("专辑目录标准化: " + template);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String template = getConfigValue(config, "template", "artist_year_album");
        String customTemplate = getConfigValue(config, "customTemplate", "");
        boolean cleanSpecialChars = getConfigValue(config, "cleanSpecialChars", true);
        boolean removeYearPrefix = getConfigValue(config, "removeYearPrefix", false);
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (!sourceFile.isDirectory()) {
            context.logDebug("Not a directory: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String newName = generateDirectoryName(sourceFile, template, customTemplate, cleanSpecialChars, removeYearPrefix, context);
            if (newName == null || newName.equals(sourceFile.getName())) {
                context.logDebug("No rename needed for: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            File targetDir = new File(sourceFile.getParent(), newName);
            
            if (targetDir.exists()) {
                context.logWarn("Target directory already exists: " + targetDir.getPath());
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            sourceFile.renameTo(targetDir);
            
            context.logInfo("Renamed album directory: " + filePath + " -> " + targetDir.getPath());
            ChangeRecord record = createChangeRecord(filePath, targetDir.getPath(), "SUCCESS");
            record.setOperationType("RENAME");
            record.setReason("专辑目录标准化: " + template);
            return record;
        } catch (Exception e) {
            context.logError("Error renaming album directory " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String generateDirectoryName(File directory, String template, String customTemplate, boolean cleanSpecialChars, boolean removeYearPrefix, ExecutionContext context) {
        String originalName = directory.getName();
        String newName = originalName;
        
        if (removeYearPrefix) {
            newName = removeYearPrefix(newName);
        }
        
        if (cleanSpecialChars) {
            newName = cleanSpecialCharacters(newName);
        }
        
        if (template.equals("custom") && customTemplate != null && !customTemplate.isEmpty()) {
            newName = customTemplate;
        }
        
        return newName;
    }

    private String removeYearPrefix(String name) {
        return name.replaceAll("^[0-9]{4}\\s*[-.]?\\s*", "");
    }

    private String cleanSpecialCharacters(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    private java.util.List<EnumOptionDTO> getDirectoryTemplateOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (DirectoryTemplate template : DirectoryTemplate.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(template.getCode());
            option.setLabel(template.getNameZh());
            option.setNameEn(template.getNameEn());
            option.setDescriptionZh(template.getDescriptionZh());
            option.setDescriptionEn(template.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}
