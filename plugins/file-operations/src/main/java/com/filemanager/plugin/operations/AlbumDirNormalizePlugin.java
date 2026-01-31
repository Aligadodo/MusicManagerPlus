package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getNormalizedDirName(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.ALBUM_RENAME);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getNormalizedDirName(String filePath, PluginConfigDTO config) {
        String template = (String) config.getValue("template", "%artist% - %year% - %album%");
        String customTemplate = (String) config.getValue("customTemplate", "");
        boolean cleanSpecialChars = (Boolean) config.getValue("cleanSpecialChars", true);
        boolean removeYearPrefix = (Boolean) config.getValue("removeYearPrefix", true);
        
        String dirName = filePath.substring(filePath.lastIndexOf('/') + 1);
        
        if ("custom".equals(template) && !customTemplate.isEmpty()) {
            dirName = customTemplate;
        } else if (!"custom".equals(template)) {
            dirName = template;
        }
        
        if (cleanSpecialChars) {
            dirName = dirName.replaceAll("[\\\\/:*?\"<>|]", "-");
            dirName = dirName.replaceAll("\\s+", " ");
            dirName = dirName.replaceAll("[-_]{2,}", "-");
        }
        
        if (removeYearPrefix) {
            dirName = dirName.replaceAll("^\\d{4}[-\\s]+", "");
            dirName = dirName.replaceAll("^\\d{4}\\.\\s+", "");
        }
        
        return dirName.trim();
    }
}