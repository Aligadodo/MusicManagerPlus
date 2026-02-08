package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.List;

public class FileRenamePlugin implements IPlugin {
    @Override
    public String getId() {
        return "file-rename";
    }

    @Override
    public String getName() {
        return "文件重命名插件";
    }

    @Override
    public String getDescription() {
        return "根据规则批量重命名文件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("pattern", "{name}_{index}");
        config.setValue("startIndex", 1);
        config.setValue("padZeros", true);
        config.setValue("zeroPadding", 3);
        config.setValue("preserveExtension", true);
        config.setValue("overwriteExisting", false);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        parameters.add(new PluginParameterDTO(
            "pattern",
            "命名模式",
            "重命名的命名模式，支持{name}原文件名, {index}序号",
            "text",
            "{name}_{index}",
            true
        ));
        
        parameters.add(new PluginParameterDTO(
            "startIndex",
            "起始序号",
            "序号的起始值",
            "number",
            1,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "padZeros",
            "补零",
            "是否对序号进行补零",
            "boolean",
            true,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "zeroPadding",
            "补零长度",
            "序号补零的长度",
            "number",
            3,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "preserveExtension",
            "保留扩展名",
            "是否保留原文件扩展名",
            "boolean",
            true,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "overwriteExisting",
            "覆盖现有文件",
            "是否覆盖已存在的同名文件",
            "boolean",
            false,
            false
        ));
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("文件重命名的默认前置条件");
        group.setLogicType("AND");
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
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
        int index = (Integer) config.getValue("startIndex",1);
        
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getNewName(filePath, config, index));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.RENAME);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
            index++;
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getNewName(String filePath, PluginConfigDTO config, int index) {
        String pattern = (String) config.getValue("pattern", "{name}_{index}");
        boolean preserveExtension = (Boolean) config.getValue("preserveExtension", true);
        boolean padZeros = (Boolean) config.getValue("padZeros", true);
        int zeroPadding = (Integer) config.getValue("zeroPadding", 3);
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
        String extension = "";
        String baseName = fileName;
        
        if (preserveExtension && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf('.'));
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        
        String indexStr = padZeros ? String.format("%0" + zeroPadding + "d", index) : String.valueOf(index);
        String newName = pattern
            .replace("{name}", baseName)
            .replace("{index}", indexStr);
        
        return dir + newName + extension;
    }
}