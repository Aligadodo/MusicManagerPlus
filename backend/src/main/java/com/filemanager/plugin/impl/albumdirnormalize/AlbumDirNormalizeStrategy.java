package com.filemanager.plugin.impl.albumdirnormalize;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.impl.albumdirnormalize.enums.DirectoryTemplate;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ScanTarget getTargetType() {
        return ScanTarget.FOLDERS_ONLY;
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
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File directory = currentRecord.getFileHandle();
        if (!directory.isDirectory()) {
            return Collections.emptyList();
        }
        
        String template = getConfigValue(config, "template", "artist_year_album");
        String customTemplate = getConfigValue(config, "customTemplate", "");
        boolean cleanSpecialChars = getConfigValue(config, "cleanSpecialChars", true);
        boolean removeYearPrefix = getConfigValue(config, "removeYearPrefix", false);
        
        context.logInfo("分析专辑目录标准化: " + directory.getName() + ", 模板: " + template);
        
        String newName = generateDirectoryName(directory, template, customTemplate, cleanSpecialChars, removeYearPrefix, context);
        if (newName == null || newName.equals(directory.getName())) {
            return Collections.emptyList();
        }
        
        String newPath = directory.getParent() + File.separator + newName;
        
        Map<String, String> params = new HashMap<>();
        params.put("template", template);
        params.put("customTemplate", customTemplate);
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            newName,
            currentRecord.getFileHandle(),
            true,
            newPath,
            OperationType.RENAME,
            params,
            ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File sourceDir = record.getFileHandle();
        File targetDir = new File(record.getNewPath());
        
        if (!sourceDir.exists()) {
            context.logWarn("源目录不存在: " + sourceDir.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        if (targetDir.exists()) {
            context.logWarn("目标目录已存在: " + targetDir.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        try {
            if (sourceDir.renameTo(targetDir)) {
                context.logInfo("重命名专辑目录: " + sourceDir.getPath() + " -> " + targetDir.getPath());
                record.setStatus(ExecStatus.SUCCESS.name());
            } else {
                context.logError("重命名专辑目录失败: " + sourceDir.getPath());
                record.setStatus(ExecStatus.FAILED.name());
            }
        } catch (Exception e) {
            context.logError("重命名专辑目录失败: " + sourceDir.getPath() + ", 错误: " + e.getMessage());
            record.setStatus(ExecStatus.FAILED.name());
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
