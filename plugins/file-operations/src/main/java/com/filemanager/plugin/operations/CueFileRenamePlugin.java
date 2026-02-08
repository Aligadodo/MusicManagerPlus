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

public class CueFileRenamePlugin implements IPlugin {
    @Override
    public String getId() {
        return "cue-file-rename";
    }

    @Override
    public String getName() {
        return "CUE文件重命名插件";
    }

    @Override
    public String getDescription() {
        return "为了解决cue文件在部分软件下，由于中文命名导致的无法加载的问题，支持统一调整cue及对应的音频文件命名。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("mode", "auto");
        config.setValue("fileName", "album");
        config.setValue("overwrite", false);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO modeParam = new PluginParameterDTO();
        modeParam.setName("mode");
        modeParam.setLabel("修改模式");
        modeParam.setDescription("设置专辑文件重命名的模式");
        modeParam.setType("select");
        modeParam.setDefaultValue("auto");
        modeParam.setRequired(true);
        modeParam.setOptions(new String[]{"auto"});
        parameters.add(modeParam);
        
        PluginParameterDTO fileNameParam = new PluginParameterDTO();
        fileNameParam.setName("fileName");
        fileNameParam.setLabel("文件名前缀");
        fileNameParam.setDescription("设置重命名后的文件前缀");
        fileNameParam.setType("text");
        fileNameParam.setDefaultValue("album");
        fileNameParam.setRequired(true);
        parameters.add(fileNameParam);
        
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
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("CUE文件重命名的默认前置条件");
        group.setLogicType("AND");
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        PreconditionDTO typeCondition = new PreconditionDTO();
        typeCondition.setId("type-condition");
        typeCondition.setField("fileExtension");
        typeCondition.setOperator(PreconditionDTO.OperatorType.IN);
        typeCondition.setValue(Arrays.asList("cue", "flac", "wav", "ape", "mp3"));
        typeCondition.setDescription("文件是CUE或音频文件");
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
        String fileName = (String) config.getValue("fileName", "album");
        String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
        String ext = filePath.substring(filePath.lastIndexOf('.'));
        
        return dir + fileName + ext;
    }
}