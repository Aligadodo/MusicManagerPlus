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

public class FileTypeFixPlugin implements IPlugin {
    @Override
    public String getId() {
        return "file-type-fix";
    }

    @Override
    public String getName() {
        return "文件类型修复插件";
    }

    @Override
    public String getDescription() {
        return "一些网上下载的音频文件类型和实际类型不符，可以通过该工具智能进行修复。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("force", false);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO forceParam = new PluginParameterDTO();
        forceParam.setName("force");
        forceParam.setLabel("强制文件类型识别");
        forceParam.setDescription("通过读取文件内容来识别文件类型（准确率更高但会变慢）");
        forceParam.setType("boolean");
        forceParam.setDefaultValue(false);
        forceParam.setRequired(false);
        parameters.add(forceParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("文件类型修复的默认前置条件");
        group.setLogicType(PreconditionGroupDTO.LogicType.AND);
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        PreconditionDTO typeCondition = new PreconditionDTO();
        typeCondition.setId("type-condition");
        typeCondition.setField("fileExtension");
        typeCondition.setOperator(PreconditionDTO.OperatorType.IN);
        typeCondition.setValue(Arrays.asList("mp3", "wav", "flac", "ogg", "aac", "m4a", "wma"));
        typeCondition.setDescription("文件是音频文件");
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
            record.setNewName(getFixedFileName(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.FIX_TYPE);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getFixedFileName(String filePath, PluginConfigDTO config) {
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String nameWithoutExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        
        return filePath.substring(0, filePath.lastIndexOf('/') + 1) + nameWithoutExt + ".mp3";
    }
}