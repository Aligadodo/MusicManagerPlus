package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileMigratePlugin implements IPlugin {
    @Override
    public String getId() {
        return "file-migrate";
    }

    @Override
    public String getName() {
        return "文件迁移插件";
    }

    @Override
    public String getDescription() {
        return "文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("operationMode", "MOVE");
        config.setValue("targetPath", "");
        config.setValue("pathMode", "absolute");
        config.setValue("directoryTemplate", "Album/Artist");
        config.setValue("customTemplate", "");
        config.setValue("scope", "all");
        config.setValue("duplicateStrategy", "skip");
        config.setValue("overwrite", false);
        config.setValue("preserveStructure", true);
        config.setValue("validateMetadata", true);
        config.setValue("generatePlaylist", false);
        config.setValue("playlistFormat", "m3u");
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO operationModeParam = new PluginParameterDTO();
        operationModeParam.setName("operationMode");
        operationModeParam.setLabel("操作模式");
        operationModeParam.setDescription("选择文件的操作方式");
        operationModeParam.setType("select");
        operationModeParam.setDefaultValue("MOVE");
        operationModeParam.setRequired(true);
        operationModeParam.setOptions(new String[]{"MOVE", "COPY"});
        parameters.add(operationModeParam);
        
        PluginParameterDTO targetPathParam = new PluginParameterDTO();
        targetPathParam.setName("targetPath");
        targetPathParam.setLabel("目标路径");
        targetPathParam.setDescription("文件迁移的目标路径");
        targetPathParam.setType("directory");
        targetPathParam.setDefaultValue("");
        targetPathParam.setRequired(true);
        parameters.add(targetPathParam);
        
        PluginParameterDTO pathModeParam = new PluginParameterDTO();
        pathModeParam.setName("pathMode");
        pathModeParam.setLabel("路径模式");
        pathModeParam.setDescription("选择路径模式");
        pathModeParam.setType("select");
        pathModeParam.setDefaultValue("absolute");
        pathModeParam.setRequired(true);
        pathModeParam.setOptions(new String[]{"absolute", "relative", "flat"});
        parameters.add(pathModeParam);
        
        PluginParameterDTO directoryTemplateParam = new PluginParameterDTO();
        directoryTemplateParam.setName("directoryTemplate");
        directoryTemplateParam.setLabel("目录模板");
        directoryTemplateParam.setDescription("选择目录结构模板");
        directoryTemplateParam.setType("select");
        directoryTemplateParam.setDefaultValue("Album/Artist");
        directoryTemplateParam.setRequired(false);
        directoryTemplateParam.setOptions(DirectoryTemplate.getTemplateNames());
        parameters.add(directoryTemplateParam);
        
        PluginParameterDTO customTemplateParam = new PluginParameterDTO();
        customTemplateParam.setName("customTemplate");
        customTemplateParam.setLabel("自定义模板");
        customTemplateParam.setDescription("自定义目录模板，支持占位符如{artist}/{album}/{title}");
        customTemplateParam.setType("text");
        customTemplateParam.setDefaultValue("");
        customTemplateParam.setRequired(false);
        parameters.add(customTemplateParam);
        
        PluginParameterDTO scopeParam = new PluginParameterDTO();
        scopeParam.setName("scope");
        scopeParam.setLabel("生效范围");
        scopeParam.setDescription("选择生效范围");
        scopeParam.setType("select");
        scopeParam.setDefaultValue("all");
        scopeParam.setRequired(true);
        scopeParam.setOptions(new String[]{"all", "selected", "matched"});
        parameters.add(scopeParam);
        
        PluginParameterDTO duplicateStrategyParam = new PluginParameterDTO();
        duplicateStrategyParam.setName("duplicateStrategy");
        duplicateStrategyParam.setLabel("去重策略");
        duplicateStrategyParam.setDescription("处理重复文件的策略");
        duplicateStrategyParam.setType("select");
        duplicateStrategyParam.setDefaultValue("skip");
        duplicateStrategyParam.setRequired(true);
        duplicateStrategyParam.setOptions(new String[]{"skip", "overwrite", "rename", "keep_both"});
        parameters.add(duplicateStrategyParam);
        
        PluginParameterDTO overwriteParam = new PluginParameterDTO();
        overwriteParam.setName("overwrite");
        overwriteParam.setLabel("覆盖已存在文件");
        overwriteParam.setDescription("是否覆盖已存在的文件");
        overwriteParam.setType("boolean");
        overwriteParam.setDefaultValue(false);
        overwriteParam.setRequired(false);
        parameters.add(overwriteParam);
        
        PluginParameterDTO preserveStructureParam = new PluginParameterDTO();
        preserveStructureParam.setName("preserveStructure");
        preserveStructureParam.setLabel("保留目录结构");
        preserveStructureParam.setDescription("是否保留原始目录结构");
        preserveStructureParam.setType("boolean");
        preserveStructureParam.setDefaultValue(true);
        preserveStructureParam.setRequired(false);
        parameters.add(preserveStructureParam);
        
        PluginParameterDTO validateMetadataParam = new PluginParameterDTO();
        validateMetadataParam.setName("validateMetadata");
        validateMetadataParam.setLabel("验证元数据");
        validateMetadataParam.setDescription("是否验证文件元数据完整性");
        validateMetadataParam.setType("boolean");
        validateMetadataParam.setDefaultValue(true);
        validateMetadataParam.setRequired(false);
        parameters.add(validateMetadataParam);
        
        PluginParameterDTO generatePlaylistParam = new PluginParameterDTO();
        generatePlaylistParam.setName("generatePlaylist");
        generatePlaylistParam.setLabel("生成播放列表");
        generatePlaylistParam.setDescription("是否生成播放列表文件");
        generatePlaylistParam.setType("boolean");
        generatePlaylistParam.setDefaultValue(false);
        generatePlaylistParam.setRequired(false);
        parameters.add(generatePlaylistParam);
        
        PluginParameterDTO playlistFormatParam = new PluginParameterDTO();
        playlistFormatParam.setName("playlistFormat");
        playlistFormatParam.setLabel("播放列表格式");
        playlistFormatParam.setDescription("播放列表文件的格式");
        playlistFormatParam.setType("select");
        playlistFormatParam.setDefaultValue("m3u");
        playlistFormatParam.setRequired(false);
        playlistFormatParam.setOptions(new String[]{"m3u", "m3u8", "pls"});
        parameters.add(playlistFormatParam);
        
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
            record.setNewName(getMigratedPath(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.MOVE);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getMigratedPath(String filePath, PluginConfigDTO config) {
        String targetPath = (String) config.getValue("targetPath", "");
        String pathMode = (String) config.getValue("pathMode", "absolute");
        String directoryTemplate = (String) config.getValue("directoryTemplate", "Album/Artist");
        String customTemplate = (String) config.getValue("customTemplate", "");
        boolean preserveStructure = (Boolean) config.getValue("preserveStructure", true);
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        
        if ("flat".equals(pathMode)) {
            return targetPath + "/" + fileName;
        } else if ("relative".equals(pathMode) && preserveStructure) {
            String dir = filePath.substring(0, filePath.lastIndexOf('/'));
            String relativePath = dir.substring(dir.indexOf('/') + 1);
            return targetPath + "/" + relativePath + "/" + fileName;
        } else if ("template".equals(pathMode)) {
            String template = DirectoryTemplate.getTemplateByName(directoryTemplate);
            if (!customTemplate.isEmpty()) {
                template = customTemplate;
            }
            
            DirectoryTemplate dirTemplate = new DirectoryTemplate(template);
            String generatedPath = dirTemplate.generatePath(new File(filePath));
            return targetPath + "/" + generatedPath;
        } else {
            return targetPath + "/" + fileName;
        }
    }
}