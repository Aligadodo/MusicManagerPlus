package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CueSplitterPlugin implements IPlugin {
    @Override
    public String getId() {
        return "cue-splitter";
    }

    @Override
    public String getName() {
        return "CUE分轨插件";
    }

    @Override
    public String getDescription() {
        return "解析.cue索引文件，将整轨音频无损切割为单曲。支持预览详细的歌曲清单与时长信息。只需要扫描cue文件。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("afterSplitAction", "do_nothing");
        config.setValue("enableArchive", false);
        config.setValue("archiveDir", "");
        config.setValue("outputDirPrefix", "Split");
        config.setValue("overwrite", false);
        config.setValue("format", "%artist% - %album% - %track% - %title%");
        config.setValue("autoFormatFilename", true);
        config.setValue("useCacheDir", false);
        config.setValue("cacheDir", "");
        config.setValue("mirrorDir", "");
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO afterSplitActionParam = new PluginParameterDTO();
        afterSplitActionParam.setName("afterSplitAction");
        afterSplitActionParam.setLabel("切分后操作");
        afterSplitActionParam.setDescription("选择切分完成后对原始文件的处理方式");
        afterSplitActionParam.setType("select");
        afterSplitActionParam.setDefaultValue("do_nothing");
        afterSplitActionParam.setRequired(true);
        afterSplitActionParam.setOptions(new String[]{"do_nothing", "delete_original", "archive_original"});
        parameters.add(afterSplitActionParam);
        
        PluginParameterDTO enableArchiveParam = new PluginParameterDTO();
        enableArchiveParam.setName("enableArchive");
        enableArchiveParam.setLabel("启用归档目录");
        enableArchiveParam.setDescription("启用时，将原始文件移动到指定的归档目录");
        enableArchiveParam.setType("boolean");
        enableArchiveParam.setDefaultValue(false);
        enableArchiveParam.setRequired(false);
        parameters.add(enableArchiveParam);
        
        PluginParameterDTO archiveDirParam = new PluginParameterDTO();
        archiveDirParam.setName("archiveDir");
        archiveDirParam.setLabel("归档目录");
        archiveDirParam.setDescription("用于设置原始文件的归档目录路径");
        archiveDirParam.setType("directory");
        archiveDirParam.setDefaultValue("");
        archiveDirParam.setRequired(false);
        parameters.add(archiveDirParam);
        
        PluginParameterDTO outputDirPrefixParam = new PluginParameterDTO();
        outputDirPrefixParam.setName("outputDirPrefix");
        outputDirPrefixParam.setLabel("输出目录前缀");
        outputDirPrefixParam.setDescription("切分后文件的输出目录前缀");
        outputDirPrefixParam.setType("text");
        outputDirPrefixParam.setDefaultValue("Split");
        outputDirPrefixParam.setRequired(false);
        parameters.add(outputDirPrefixParam);
        
        PluginParameterDTO overwriteParam = new PluginParameterDTO();
        overwriteParam.setName("overwrite");
        overwriteParam.setLabel("覆盖已存在文件");
        overwriteParam.setDescription("是否覆盖已存在的目标文件");
        overwriteParam.setType("boolean");
        overwriteParam.setDefaultValue(false);
        overwriteParam.setRequired(false);
        parameters.add(overwriteParam);
        
        PluginParameterDTO formatParam = new PluginParameterDTO();
        formatParam.setName("format");
        formatParam.setLabel("文件名格式");
        formatParam.setDescription("切分后文件的命名格式，支持%artist%, %album%, %track%, %title%等占位符");
        formatParam.setType("text");
        formatParam.setDefaultValue("%artist% - %album% - %track% - %title%");
        formatParam.setRequired(false);
        parameters.add(formatParam);
        
        PluginParameterDTO autoFormatFilenameParam = new PluginParameterDTO();
        autoFormatFilenameParam.setName("autoFormatFilename");
        autoFormatFilenameParam.setLabel("自动格式化文件名");
        autoFormatFilenameParam.setDescription("是否自动将文件名转换为简体中文");
        autoFormatFilenameParam.setType("boolean");
        autoFormatFilenameParam.setDefaultValue(true);
        autoFormatFilenameParam.setRequired(false);
        parameters.add(autoFormatFilenameParam);
        
        PluginParameterDTO useCacheDirParam = new PluginParameterDTO();
        useCacheDirParam.setName("useCacheDir");
        useCacheDirParam.setLabel("使用缓存目录");
        useCacheDirParam.setDescription("是否使用缓存目录处理文件（适合机械硬盘）");
        useCacheDirParam.setType("boolean");
        useCacheDirParam.setDefaultValue(false);
        useCacheDirParam.setRequired(false);
        parameters.add(useCacheDirParam);
        
        PluginParameterDTO cacheDirParam = new PluginParameterDTO();
        cacheDirParam.setName("cacheDir");
        cacheDirParam.setLabel("缓存目录");
        cacheDirParam.setDescription("缓存目录路径");
        cacheDirParam.setType("directory");
        cacheDirParam.setDefaultValue("");
        cacheDirParam.setRequired(false);
        parameters.add(cacheDirParam);
        
        PluginParameterDTO mirrorDirParam = new PluginParameterDTO();
        mirrorDirParam.setName("mirrorDir");
        mirrorDirParam.setLabel("镜像目录");
        mirrorDirParam.setDescription("镜像目录路径（挂载到SSD盘下）");
        mirrorDirParam.setType("directory");
        mirrorDirParam.setDefaultValue("");
        mirrorDirParam.setRequired(false);
        parameters.add(mirrorDirParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("CUE分轨的默认前置条件");
        group.setLogicType("AND");
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        PreconditionDTO typeCondition = new PreconditionDTO();
        typeCondition.setId("type-condition");
        typeCondition.setField("fileExtension");
        typeCondition.setOperator(PreconditionDTO.OperatorType.EQUALS);
        typeCondition.setValue("cue");
        typeCondition.setDescription("文件是CUE文件");
        preconditions.add(typeCondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getSplitPath(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.SPLIT);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getSplitPath(String filePath, PluginConfigDTO config) {
        String outputDirPrefix = (String) config.getValue("outputDirPrefix", "Split");
        String format = (String) config.getValue("format", "%artist% - %album% - %track% - %title%");
        
        String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        
        return dir + outputDirPrefix + "/" + baseName.replace(".cue", "") + ".mp3";
    }
}