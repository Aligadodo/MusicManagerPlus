package com.filemanager.plugin.converter;

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

public class AudioConverterPlugin implements IPlugin {
    @Override
    public String getId() {
        return "audio-converter";
    }

    @Override
    public String getName() {
        return "音频转换插件";
    }

    @Override
    public String getDescription() {
        return "将音频文件转换为不同格式";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("targetFormat", "mp3");
        config.setValue("bitrate", "320k");
        config.setValue("sampleRate", 44100);
        config.setValue("channels", 2);
        config.setValue("outputDirectory", "");
        config.setValue("overwriteExisting", false);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO formatParam = new PluginParameterDTO(
            "targetFormat",
            "目标格式",
            "要转换的音频格式",
            "select",
            "mp3",
            true
        );
        formatParam.setOptions(new String[]{"mp3", "wav", "flac", "ogg", "aac"});
        parameters.add(formatParam);
        
        PluginParameterDTO bitrateParam = new PluginParameterDTO(
            "bitrate",
            "比特率",
            "目标音频的比特率",
            "select",
            "320k",
            false
        );
        bitrateParam.setOptions(new String[]{"64k", "128k", "192k", "256k", "320k"});
        parameters.add(bitrateParam);
        
        PluginParameterDTO sampleRateParam = new PluginParameterDTO(
            "sampleRate",
            "采样率",
            "目标音频的采样率",
            "select",
            44100,
            false
        );
        sampleRateParam.setOptions(new String[]{"22050", "44100", "48000", "96000"});
        parameters.add(sampleRateParam);
        
        PluginParameterDTO channelsParam = new PluginParameterDTO(
            "channels",
            "声道数",
            "目标音频的声道数",
            "select",
            2,
            false
        );
        channelsParam.setOptions(new String[]{"1", "2"});
        parameters.add(channelsParam);
        
        parameters.add(new PluginParameterDTO(
            "outputDirectory",
            "输出目录",
            "转换后文件的输出目录，留空则使用原目录",
            "directory",
            "",
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "overwriteExisting",
            "覆盖现有文件",
            "是否覆盖已存在的目标文件",
            "boolean",
            false,
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
        group.setDescription("音频转换的默认前置条件");
        group.setLogicType(PreconditionGroupDTO.LogicType.AND);
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        // 添加文件类型前置条件
        PreconditionDTO typeCondition = new PreconditionDTO();
        typeCondition.setId("type-condition");
        typeCondition.setField("fileExtension");
        typeCondition.setOperator(PreconditionDTO.OperatorType.IN);
        typeCondition.setValue(Arrays.asList("wav", "flac", "ogg", "aac", "mp3", "wma"));
        typeCondition.setDescription("文件是音频文件");
        preconditions.add(typeCondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        // 实现音频转换逻辑
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getConvertedPath(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.METADATA_UPDATE);
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

    private String getConvertedPath(String filePath, PluginConfigDTO config) {
        String targetFormat = (String) config.getValue("targetFormat", "mp3");
        String outputDir = (String) config.getValue("outputDirectory", "");
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        
        if (outputDir.isEmpty()) {
            String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
            return dir + baseName + "." + targetFormat;
        } else {
            return outputDir + "/" + baseName + "." + targetFormat;
        }
    }
}
