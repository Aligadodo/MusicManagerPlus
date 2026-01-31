package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.List;

public class DuplicatePlugin implements IPlugin {
    @Override
    public String getId() {
        return "duplicate";
    }

    @Override
    public String getName() {
        return "文件去重插件";
    }

    @Override
    public String getDescription() {
        return "支持多种去重策略，包括保留最佳版本、添加序号、保留最早/最新文件等。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("strategy", "keep_best");
        config.setValue("comparisonMethod", "md5");
        config.setValue("caseInsensitive", true);
        config.setValue("ignoreWhitespace", true);
        config.setValue("ignoreSpecialChars", true);
        config.setValue("keepLargest", true);
        config.setValue("keepEarliest", true);
        config.setValue("keepLatest", false);
        config.setValue("addSequence", false);
        config.setValue("sequenceFormat", "({index})");
        config.setValue("moveToTrash", false);
        config.setValue("trashPath", ".EchoTrash");
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO strategyParam = new PluginParameterDTO();
        strategyParam.setName("strategy");
        strategyParam.setLabel("去重策略");
        strategyParam.setDescription("选择去重策略");
        strategyParam.setType("select");
        strategyParam.setDefaultValue("keep_best");
        strategyParam.setRequired(true);
        strategyParam.setOptions(new String[]{"keep_best", "keep_largest", "keep_earliest", "keep_latest", "add_sequence"});
        parameters.add(strategyParam);
        
        PluginParameterDTO comparisonMethodParam = new PluginParameterDTO();
        comparisonMethodParam.setName("comparisonMethod");
        comparisonMethodParam.setLabel("比较方法");
        comparisonMethodParam.setDescription("选择文件比较方法");
        comparisonMethodParam.setType("select");
        comparisonMethodParam.setDefaultValue("md5");
        comparisonMethodParam.setRequired(true);
        comparisonMethodParam.setOptions(new String[]{"md5", "sha1", "sha256", "size", "name"});
        parameters.add(comparisonMethodParam);
        
        PluginParameterDTO caseInsensitiveParam = new PluginParameterDTO();
        caseInsensitiveParam.setName("caseInsensitive");
        caseInsensitiveParam.setLabel("忽略大小写");
        caseInsensitiveParam.setDescription("是否忽略文件名大小写");
        caseInsensitiveParam.setType("boolean");
        caseInsensitiveParam.setDefaultValue(true);
        caseInsensitiveParam.setRequired(false);
        parameters.add(caseInsensitiveParam);
        
        PluginParameterDTO ignoreWhitespaceParam = new PluginParameterDTO();
        ignoreWhitespaceParam.setName("ignoreWhitespace");
        ignoreWhitespaceParam.setLabel("忽略空白字符");
        ignoreWhitespaceParam.setDescription("是否忽略文件名中的空白字符");
        ignoreWhitespaceParam.setType("boolean");
        ignoreWhitespaceParam.setDefaultValue(true);
        ignoreWhitespaceParam.setRequired(false);
        parameters.add(ignoreWhitespaceParam);
        
        PluginParameterDTO ignoreSpecialCharsParam = new PluginParameterDTO();
        ignoreSpecialCharsParam.setName("ignoreSpecialChars");
        ignoreSpecialCharsParam.setLabel("忽略特殊字符");
        ignoreSpecialCharsParam.setDescription("是否忽略文件名中的特殊字符");
        ignoreSpecialCharsParam.setType("boolean");
        ignoreSpecialCharsParam.setDefaultValue(true);
        ignoreSpecialCharsParam.setRequired(false);
        parameters.add(ignoreSpecialCharsParam);
        
        PluginParameterDTO keepLargestParam = new PluginParameterDTO();
        keepLargestParam.setName("keepLargest");
        keepLargestParam.setLabel("保留最大文件");
        keepLargestParam.setDescription("在重复文件中保留最大的文件");
        keepLargestParam.setType("boolean");
        keepLargestParam.setDefaultValue(true);
        keepLargestParam.setRequired(false);
        parameters.add(keepLargestParam);
        
        PluginParameterDTO keepEarliestParam = new PluginParameterDTO();
        keepEarliestParam.setName("keepEarliest");
        keepEarliestParam.setLabel("保留最早文件");
        keepEarliestParam.setDescription("在重复文件中保留最早创建的文件");
        keepEarliestParam.setType("boolean");
        keepEarliestParam.setDefaultValue(true);
        keepEarliestParam.setRequired(false);
        parameters.add(keepEarliestParam);
        
        PluginParameterDTO keepLatestParam = new PluginParameterDTO();
        keepLatestParam.setName("keepLatest");
        keepLatestParam.setLabel("保留最新文件");
        keepLatestParam.setDescription("在重复文件中保留最新创建的文件");
        keepLatestParam.setType("boolean");
        keepLatestParam.setDefaultValue(false);
        keepLatestParam.setRequired(false);
        parameters.add(keepLatestParam);
        
        PluginParameterDTO addSequenceParam = new PluginParameterDTO();
        addSequenceParam.setName("addSequence");
        addSequenceParam.setLabel("添加序号");
        addSequenceParam.setDescription("为重复文件添加序号");
        addSequenceParam.setType("boolean");
        addSequenceParam.setDefaultValue(false);
        addSequenceParam.setRequired(false);
        parameters.add(addSequenceParam);
        
        PluginParameterDTO sequenceFormatParam = new PluginParameterDTO();
        sequenceFormatParam.setName("sequenceFormat");
        sequenceFormatParam.setLabel("序号格式");
        sequenceFormatParam.setDescription("序号的格式，支持{index}占位符");
        sequenceFormatParam.setType("text");
        sequenceFormatParam.setDefaultValue("({index})");
        sequenceFormatParam.setRequired(false);
        parameters.add(sequenceFormatParam);
        
        PluginParameterDTO moveToTrashParam = new PluginParameterDTO();
        moveToTrashParam.setName("moveToTrash");
        moveToTrashParam.setLabel("移动到回收站");
        moveToTrashParam.setDescription("将重复文件移动到回收站而不是直接删除");
        moveToTrashParam.setType("boolean");
        moveToTrashParam.setDefaultValue(false);
        moveToTrashParam.setRequired(false);
        parameters.add(moveToTrashParam);
        
        PluginParameterDTO trashPathParam = new PluginParameterDTO();
        trashPathParam.setName("trashPath");
        trashPathParam.setLabel("回收站路径");
        trashPathParam.setDescription("回收站的路径");
        trashPathParam.setType("directory");
        trashPathParam.setDefaultValue(".EchoTrash");
        trashPathParam.setRequired(false);
        parameters.add(trashPathParam);
        
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
            record.setNewName(getDuplicatePath(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.DEDUP);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getDuplicatePath(String filePath, PluginConfigDTO config) {
        String strategy = (String) config.getValue("strategy", "keep_best");
        boolean addSequence = (Boolean) config.getValue("addSequence", false);
        String sequenceFormat = (String) config.getValue("sequenceFormat", "({index})");
        boolean moveToTrash = (Boolean) config.getValue("moveToTrash", false);
        String trashPath = (String) config.getValue("trashPath", ".EchoTrash");
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
        
        if (addSequence) {
            String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
            return dir + baseName + sequenceFormat.replace("{index}", "1") + ext;
        } else if (moveToTrash) {
            return trashPath + "/" + fileName;
        } else {
            return filePath;
        }
    }
}