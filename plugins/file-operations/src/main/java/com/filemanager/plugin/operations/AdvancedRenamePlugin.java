package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.List;

public class AdvancedRenamePlugin implements IPlugin {
    @Override
    public String getId() {
        return "advanced-rename";
    }

    @Override
    public String getName() {
        return "高级重命名插件";
    }

    @Override
    public String getDescription() {
        return "支持规则列表、正则表达式、元数据提取等多种重命名方式的高级重命名工具。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("crossDriveMode", "move");
        config.setValue("processScope", "all");
        config.setValue("rules", new ArrayList<String>());
        config.setValue("caseSensitive", false);
        config.setValue("useRegex", false);
        config.setValue("preserveExtension", true);
        config.setValue("overwrite", false);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO crossDriveModeParam = new PluginParameterDTO();
        crossDriveModeParam.setName("crossDriveMode");
        crossDriveModeParam.setLabel("跨盘动作");
        crossDriveModeParam.setDescription("设置跨盘操作时的动作");
        crossDriveModeParam.setType("select");
        crossDriveModeParam.setDefaultValue("move");
        crossDriveModeParam.setRequired(true);
        crossDriveModeParam.setOptions(new String[]{"move", "copy"});
        parameters.add(crossDriveModeParam);
        
        PluginParameterDTO processScopeParam = new PluginParameterDTO();
        processScopeParam.setName("processScope");
        processScopeParam.setLabel("处理范围");
        processScopeParam.setDescription("设置处理的文件类型范围");
        processScopeParam.setType("select");
        processScopeParam.setDefaultValue("all");
        processScopeParam.setRequired(true);
        processScopeParam.setOptions(new String[]{"files_only", "folders_only", "all"});
        parameters.add(processScopeParam);
        
        PluginParameterDTO rulesParam = new PluginParameterDTO();
        rulesParam.setName("rules");
        rulesParam.setLabel("重命名规则");
        rulesParam.setDescription("重命名规则列表");
        rulesParam.setType("list");
        rulesParam.setDefaultValue(new ArrayList<String>());
        rulesParam.setRequired(false);
        parameters.add(rulesParam);
        
        PluginParameterDTO caseSensitiveParam = new PluginParameterDTO();
        caseSensitiveParam.setName("caseSensitive");
        caseSensitiveParam.setLabel("区分大小写");
        caseSensitiveParam.setDescription("是否区分大小写");
        caseSensitiveParam.setType("boolean");
        caseSensitiveParam.setDefaultValue(false);
        caseSensitiveParam.setRequired(false);
        parameters.add(caseSensitiveParam);
        
        PluginParameterDTO useRegexParam = new PluginParameterDTO();
        useRegexParam.setName("useRegex");
        useRegexParam.setLabel("使用正则表达式");
        useRegexParam.setDescription("是否使用正则表达式进行匹配");
        useRegexParam.setType("boolean");
        useRegexParam.setDefaultValue(false);
        useRegexParam.setRequired(false);
        parameters.add(useRegexParam);
        
        PluginParameterDTO preserveExtensionParam = new PluginParameterDTO();
        preserveExtensionParam.setName("preserveExtension");
        preserveExtensionParam.setLabel("保留文件扩展名");
        preserveExtensionParam.setDescription("是否保留原始文件扩展名");
        preserveExtensionParam.setType("boolean");
        preserveExtensionParam.setDefaultValue(true);
        preserveExtensionParam.setRequired(false);
        parameters.add(preserveExtensionParam);
        
        PluginParameterDTO overwriteParam = new PluginParameterDTO();
        overwriteParam.setName("overwrite");
        overwriteParam.setLabel("覆盖已存在文件");
        overwriteParam.setDescription("是否覆盖已存在的文件");
        overwriteParam.setType("boolean");
        overwriteParam.setDefaultValue(false);
        overwriteParam.setRequired(false);
        parameters.add(overwriteParam);
        
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
            record.setNewName(getRenamedPath(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.RENAME);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getRenamedPath(String filePath, PluginConfigDTO config) {
        boolean preserveExtension = (Boolean) config.getValue("preserveExtension", true);
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
        
        String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
        
        if (preserveExtension) {
            return dir + baseName + ext;
        } else {
            return dir + baseName;
        }
    }
}