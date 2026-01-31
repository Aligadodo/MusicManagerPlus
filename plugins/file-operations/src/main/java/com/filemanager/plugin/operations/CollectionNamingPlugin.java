package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.List;

public class CollectionNamingPlugin implements IPlugin {
    @Override
    public String getId() {
        return "collection-naming";
    }

    @Override
    public String getName() {
        return "合集命名插件";
    }

    @Override
    public String getDescription() {
        return "支持多种合集命名策略，包括简洁风格、精确风格、选取模板等。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("strategy", "concise");
        config.setValue("removeYear", true);
        config.setValue("removeFormat", true);
        config.setValue("removeCDNumber", true);
        config.setValue("removeDiscNumber", true);
        config.setValue("removeVolNumber", true);
        config.setValue("removeParentheses", false);
        config.setValue("removeBrackets", false);
        config.setValue("keepTemplate", false);
        config.setValue("overwrite", false);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO strategyParam = new PluginParameterDTO();
        strategyParam.setName("strategy");
        strategyParam.setLabel("命名策略");
        strategyParam.setDescription("选择合集命名策略");
        strategyParam.setType("select");
        strategyParam.setDefaultValue("concise");
        strategyParam.setRequired(true);
        strategyParam.setOptions(new String[]{"concise", "precise", "template"});
        parameters.add(strategyParam);
        
        PluginParameterDTO removeYearParam = new PluginParameterDTO();
        removeYearParam.setName("removeYear");
        removeYearParam.setLabel("移除年份");
        removeYearParam.setDescription("是否移除年份信息");
        removeYearParam.setType("boolean");
        removeYearParam.setDefaultValue(true);
        removeYearParam.setRequired(false);
        parameters.add(removeYearParam);
        
        PluginParameterDTO removeFormatParam = new PluginParameterDTO();
        removeFormatParam.setName("removeFormat");
        removeFormatParam.setLabel("移除格式");
        removeFormatParam.setDescription("是否移除文件格式信息");
        removeFormatParam.setType("boolean");
        removeFormatParam.setDefaultValue(true);
        removeFormatParam.setRequired(false);
        parameters.add(removeFormatParam);
        
        PluginParameterDTO removeCDNumberParam = new PluginParameterDTO();
        removeCDNumberParam.setName("removeCDNumber");
        removeCDNumberParam.setLabel("移除CD序号");
        removeCDNumberParam.setDescription("是否移除CD序号");
        removeCDNumberParam.setType("boolean");
        removeCDNumberParam.setDefaultValue(true);
        removeCDNumberParam.setRequired(false);
        parameters.add(removeCDNumberParam);
        
        PluginParameterDTO removeDiscNumberParam = new PluginParameterDTO();
        removeDiscNumberParam.setName("removeDiscNumber");
        removeDiscNumberParam.setLabel("移除Disc序号");
        removeDiscNumberParam.setDescription("是否移除Disc序号");
        removeDiscNumberParam.setType("boolean");
        removeDiscNumberParam.setDefaultValue(true);
        removeDiscNumberParam.setRequired(false);
        parameters.add(removeDiscNumberParam);
        
        PluginParameterDTO removeVolNumberParam = new PluginParameterDTO();
        removeVolNumberParam.setName("removeVolNumber");
        removeVolNumberParam.setLabel("移除Vol序号");
        removeVolNumberParam.setDescription("是否移除Vol序号");
        removeVolNumberParam.setType("boolean");
        removeVolNumberParam.setDefaultValue(true);
        removeVolNumberParam.setRequired(false);
        parameters.add(removeVolNumberParam);
        
        PluginParameterDTO removeParenthesesParam = new PluginParameterDTO();
        removeParenthesesParam.setName("removeParentheses");
        removeParenthesesParam.setLabel("移除括号内容");
        removeParenthesesParam.setDescription("是否移除圆括号中的内容");
        removeParenthesesParam.setType("boolean");
        removeParenthesesParam.setDefaultValue(false);
        removeParenthesesParam.setRequired(false);
        parameters.add(removeParenthesesParam);
        
        PluginParameterDTO removeBracketsParam = new PluginParameterDTO();
        removeBracketsParam.setName("removeBrackets");
        removeBracketsParam.setLabel("移除方括号内容");
        removeBracketsParam.setDescription("是否移除方括号中的内容");
        removeBracketsParam.setType("boolean");
        removeBracketsParam.setDefaultValue(false);
        removeBracketsParam.setRequired(false);
        parameters.add(removeBracketsParam);
        
        PluginParameterDTO keepTemplateParam = new PluginParameterDTO();
        keepTemplateParam.setName("keepTemplate");
        keepTemplateParam.setLabel("保留模板");
        keepTemplateParam.setDescription("是否保留模板文件");
        keepTemplateParam.setType("boolean");
        keepTemplateParam.setDefaultValue(false);
        keepTemplateParam.setRequired(false);
        parameters.add(keepTemplateParam);
        
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
            record.setNewName(getCollectionName(filePath, config));
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

    private String getCollectionName(String filePath, PluginConfigDTO config) {
        String strategy = (String) config.getValue("strategy", "concise");
        boolean removeYear = (Boolean) config.getValue("removeYear", true);
        boolean removeFormat = (Boolean) config.getValue("removeFormat", true);
        boolean removeCDNumber = (Boolean) config.getValue("removeCDNumber", true);
        boolean removeDiscNumber = (Boolean) config.getValue("removeDiscNumber", true);
        boolean removeVolNumber = (Boolean) config.getValue("removeVolNumber", true);
        boolean removeParentheses = (Boolean) config.getValue("removeParentheses", false);
        boolean removeBrackets = (Boolean) config.getValue("removeBrackets", false);
        
        String dirName = filePath.substring(filePath.lastIndexOf('/') + 1);
        
        if (removeYear) {
            dirName = dirName.replaceAll("\\d{4}", "");
        }
        
        if (removeFormat) {
            dirName = dirName.replaceAll("\\.(flac|wav|mp3|ogg|aac|m4a|wma)", "");
        }
        
        if (removeCDNumber) {
            dirName = dirName.replaceAll("-\\s*CD\\s*\\d+", "");
        }
        
        if (removeDiscNumber) {
            dirName = dirName.replaceAll("-\\s*Disc\\s*\\d+", "");
        }
        
        if (removeVolNumber) {
            dirName = dirName.replaceAll("-\\s*Vol\\.?\\s*\\d+", "");
        }
        
        if (removeParentheses) {
            dirName = dirName.replaceAll("\\([^)]*\\)", "");
        }
        
        if (removeBrackets) {
            dirName = dirName.replaceAll("\\[[^]]*\\]", "");
        }
        
        return dirName.trim();
    }
}