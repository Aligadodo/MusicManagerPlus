package com.filemanager.plugin.collection;

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

public class FileCollectionPlugin implements IPlugin {
    @Override
    public String getId() {
        return "file-collection";
    }

    @Override
    public String getName() {
        return "文件收集插件";
    }

    @Override
    public String getDescription() {
        return "根据配置规则收集和整理文件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("targetDirectory", "/tmp/collected");
        config.setValue("recursive", true);
        config.setValue("includePatterns", Arrays.asList("*.mp3", "*.wav", "*.flac"));
        config.setValue("excludePatterns", Arrays.asList("*.tmp", "*.log"));
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        parameters.add(new PluginParameterDTO(
            "targetDirectory",
            "目标目录",
            "文件收集的目标目录",
            "directory",
            "/tmp/collected",
            true
        ));
        
        parameters.add(new PluginParameterDTO(
            "recursive",
            "递归收集",
            "是否递归收集子目录中的文件",
            "boolean",
            true,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "includePatterns",
            "包含模式",
            "要收集的文件模式列表，多个模式用逗号分隔",
            "text",
            "*.mp3,*.wav,*.flac",
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "excludePatterns",
            "排除模式",
            "要排除的文件模式列表，多个模式用逗号分隔",
            "text",
            "*.tmp,*.log",
            false
        ));
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        // 创建默认前置条件组
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("文件收集的默认前置条件");
        group.setLogicType(PreconditionGroupDTO.LogicType.AND);
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        // 添加文件存在前置条件
        PreconditionDTO existCondition = new PreconditionDTO();
        existCondition.setId("exist-condition");
        existCondition.setField("fileExists");
        existCondition.setOperator(PreconditionDTO.OperatorType.EQUALS);
        existCondition.setValue(true);
        existCondition.setDescription("文件存在");
        preconditions.add(existCondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        // 实现文件收集逻辑
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getTargetPath(filePath, config));
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
        // 预览逻辑与执行逻辑类似，只是不实际执行操作
        return execute(filePaths, config, context);
    }

    private String getTargetPath(String filePath, PluginConfigDTO config) {
        String targetDir = (String) config.getValue("targetDirectory");
        if (targetDir == null) {
            targetDir = "/tmp/collected";
        }
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        return targetDir + "/" + fileName;
    }
}
