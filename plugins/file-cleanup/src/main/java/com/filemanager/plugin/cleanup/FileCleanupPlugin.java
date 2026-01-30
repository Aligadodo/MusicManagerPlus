package com.filemanager.plugin.cleanup;

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

public class FileCleanupPlugin implements IPlugin {
    @Override
    public String getId() {
        return "file-cleanup";
    }

    @Override
    public String getName() {
        return "文件清理插件";
    }

    @Override
    public String getDescription() {
        return "根据配置规则清理不需要的文件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("maxFileAgeDays", 30);
        config.setValue("minFileSizeKB", 0);
        config.setValue("maxFileSizeKB", 10240);
        config.setValue("deleteEmptyDirectories", true);
        config.setValue("includePatterns", Arrays.asList("*.tmp", "*.log", "*.bak"));
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        parameters.add(new PluginParameterDTO(
            "maxFileAgeDays",
            "最大文件年龄（天）",
            "超过此天数的文件将被清理",
            "number",
            30,
            true
        ));
        
        parameters.add(new PluginParameterDTO(
            "minFileSizeKB",
            "最小文件大小（KB）",
            "小于此大小的文件将被清理",
            "number",
            0,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "maxFileSizeKB",
            "最大文件大小（KB）",
            "大于此大小的文件将被清理",
            "number",
            10240,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "deleteEmptyDirectories",
            "删除空目录",
            "是否删除清理后产生的空目录",
            "boolean",
            true,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "includePatterns",
            "包含模式",
            "要清理的文件模式列表",
            "text",
            "*.tmp,*.log,*.bak",
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
        group.setDescription("文件清理的默认前置条件");
        group.setLogicType(PreconditionGroupDTO.LogicType.AND);
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        // 添加文件大小前置条件
        PreconditionDTO sizeCondition = new PreconditionDTO();
        sizeCondition.setId("size-condition");
        sizeCondition.setField("fileSize");
        sizeCondition.setOperator(PreconditionDTO.OperatorType.LESS_THAN);
        sizeCondition.setValue(1024); // 1KB
        sizeCondition.setDescription("文件大小小于1KB");
        preconditions.add(sizeCondition);
        
        // 添加文件扩展名前置条件
        PreconditionDTO extensionCondition = new PreconditionDTO();
        extensionCondition.setId("extension-condition");
        extensionCondition.setField("fileExtension");
        extensionCondition.setOperator(PreconditionDTO.OperatorType.IN);
        extensionCondition.setValue(Arrays.asList("tmp", "log", "bak"));
        extensionCondition.setDescription("文件扩展名为临时文件类型");
        preconditions.add(extensionCondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        // 实现文件清理逻辑
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(null); // 删除操作
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.DELETE);
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
}
