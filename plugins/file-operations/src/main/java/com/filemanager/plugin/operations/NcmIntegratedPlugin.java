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

public class NcmIntegratedPlugin implements IPlugin {
    @Override
    public String getId() {
        return "ncm-integrated";
    }

    @Override
    public String getName() {
        return "网易云音乐工具集插件";
    }

    @Override
    public String getDescription() {
        return "网易云音乐工具集，包含NCM转换、缓存扫描、歌词下载等功能。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("function", "convert");
        config.setValue("outputFormat", "mp3");
        config.setValue("bitrate", "320k");
        config.setValue("cacheDir", "");
        config.setValue("outputDir", "");
        config.setValue("downloadLyric", true);
        config.setValue("lyricFormat", "lrc");
        config.setValue("overwrite", false);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO functionParam = new PluginParameterDTO();
        functionParam.setName("function");
        functionParam.setLabel("功能选择");
        functionParam.setDescription("选择要执行的网易云音乐工具功能");
        functionParam.setType("select");
        functionParam.setDefaultValue("convert");
        functionParam.setRequired(true);
        functionParam.setOptions(new String[]{"convert", "cache_scan", "lyric_download"});
        parameters.add(functionParam);
        
        PluginParameterDTO outputFormatParam = new PluginParameterDTO();
        outputFormatParam.setName("outputFormat");
        outputFormatParam.setLabel("输出格式");
        outputFormatParam.setDescription("转换后的音频格式");
        outputFormatParam.setType("select");
        outputFormatParam.setDefaultValue("mp3");
        outputFormatParam.setRequired(false);
        outputFormatParam.setOptions(new String[]{"mp3", "flac", "wav", "ogg", "aac"});
        parameters.add(outputFormatParam);
        
        PluginParameterDTO bitrateParam = new PluginParameterDTO();
        bitrateParam.setName("bitrate");
        bitrateParam.setLabel("比特率");
        bitrateParam.setDescription("目标音频的比特率");
        bitrateParam.setType("select");
        bitrateParam.setDefaultValue("320k");
        bitrateParam.setRequired(false);
        bitrateParam.setOptions(new String[]{"64k", "128k", "192k", "256k", "320k"});
        parameters.add(bitrateParam);
        
        PluginParameterDTO cacheDirParam = new PluginParameterDTO();
        cacheDirParam.setName("cacheDir");
        cacheDirParam.setLabel("缓存目录");
        cacheDirParam.setDescription("网易云音乐缓存文件目录");
        cacheDirParam.setType("directory");
        cacheDirParam.setDefaultValue("");
        cacheDirParam.setRequired(false);
        parameters.add(cacheDirParam);
        
        PluginParameterDTO outputDirParam = new PluginParameterDTO();
        outputDirParam.setName("outputDir");
        outputDirParam.setLabel("输出目录");
        outputDirParam.setDescription("转换后文件的输出目录");
        outputDirParam.setType("directory");
        outputDirParam.setDefaultValue("");
        outputDirParam.setRequired(false);
        parameters.add(outputDirParam);
        
        PluginParameterDTO downloadLyricParam = new PluginParameterDTO();
        downloadLyricParam.setName("downloadLyric");
        downloadLyricParam.setLabel("下载歌词");
        downloadLyricParam.setDescription("是否下载对应的歌词文件");
        downloadLyricParam.setType("boolean");
        downloadLyricParam.setDefaultValue(true);
        downloadLyricParam.setRequired(false);
        parameters.add(downloadLyricParam);
        
        PluginParameterDTO lyricFormatParam = new PluginParameterDTO();
        lyricFormatParam.setName("lyricFormat");
        lyricFormatParam.setLabel("歌词格式");
        lyricFormatParam.setDescription("歌词文件的格式");
        lyricFormatParam.setType("select");
        lyricFormatParam.setDefaultValue("lrc");
        lyricFormatParam.setRequired(false);
        lyricFormatParam.setOptions(new String[]{"lrc", "txt"});
        parameters.add(lyricFormatParam);
        
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
        group.setDescription("网易云音乐工具的默认前置条件");
        group.setLogicType("AND");
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        PreconditionDTO typeCondition = new PreconditionDTO();
        typeCondition.setId("type-condition");
        typeCondition.setField("fileExtension");
        typeCondition.setOperator(PreconditionDTO.OperatorType.IN);
        typeCondition.setValue(Arrays.asList("ncm", "uc", "cache"));
        typeCondition.setDescription("文件是网易云音乐相关文件");
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
            record.setNewName(getConvertedPath(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.CONVERT);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private String getConvertedPath(String filePath, PluginConfigDTO config) {
        String function = (String) config.getValue("function", "convert");
        String outputFormat = (String) config.getValue("outputFormat", "mp3");
        String outputDir = (String) config.getValue("outputDir", "");
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        
        if ("convert".equals(function)) {
            if (outputDir.isEmpty()) {
                String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
                return dir + baseName + "." + outputFormat;
            } else {
                return outputDir + "/" + baseName + "." + outputFormat;
            }
        } else if ("cache_scan".equals(function)) {
            return outputDir + "/" + baseName + "." + outputFormat;
        } else {
            return filePath;
        }
    }
}