package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackNumberPlugin implements IPlugin {
    
    @Override
    public String getId() {
        return "track-number";
    }

    @Override
    public String getName() {
        return "音轨编号插件";
    }

    @Override
    public String getDescription() {
        return "为音频文件添加或修改音轨编号，支持多种编号模式、双位补零、自定义分隔符等功能。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("mode", "default");
        config.setValue("startNumber", 1);
        config.setValue("padZero", true);
        config.setValue("numberFormat", "01");
        config.setValue("separator", ". ");
        config.setValue("updateMetadata", true);
        config.setValue("preserveOriginal", false);
        config.setValue("groupByDirectory", true);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO modeParam = new PluginParameterDTO();
        modeParam.setName("mode");
        modeParam.setLabel("编号模式");
        modeParam.setDescription("选择音轨编号的模式");
        modeParam.setType("select");
        modeParam.setDefaultValue("default");
        modeParam.setRequired(true);
        modeParam.setOptions(new String[]{
            "default",
            "metadata",
            "textList",
            "cueFile",
            "custom"
        });
        parameters.add(modeParam);
        
        PluginParameterDTO startNumberParam = new PluginParameterDTO();
        startNumberParam.setName("startNumber");
        startNumberParam.setLabel("起始编号");
        startNumberParam.setDescription("音轨编号的起始数字");
        startNumberParam.setType("number");
        startNumberParam.setDefaultValue(1);
        startNumberParam.setRequired(false);
        parameters.add(startNumberParam);
        
        PluginParameterDTO padZeroParam = new PluginParameterDTO();
        padZeroParam.setName("padZero");
        padZeroParam.setLabel("双位补零");
        padZeroParam.setDescription("是否使用双位补零（如01, 02）");
        padZeroParam.setType("boolean");
        padZeroParam.setDefaultValue(true);
        padZeroParam.setRequired(false);
        parameters.add(padZeroParam);
        
        PluginParameterDTO numberFormatParam = new PluginParameterDTO();
        numberFormatParam.setName("numberFormat");
        numberFormatParam.setLabel("编号格式");
        numberFormatParam.setDescription("音轨编号的格式");
        numberFormatParam.setType("select");
        numberFormatParam.setDefaultValue("01");
        numberFormatParam.setRequired(false);
        numberFormatParam.setOptions(new String[]{"1", "01", "001"});
        parameters.add(numberFormatParam);
        
        PluginParameterDTO separatorParam = new PluginParameterDTO();
        separatorParam.setName("separator");
        separatorParam.setLabel("分隔符");
        separatorParam.setDescription("音轨编号与文件名之间的分隔符");
        separatorParam.setType("text");
        separatorParam.setDefaultValue(". ");
        separatorParam.setRequired(false);
        parameters.add(separatorParam);
        
        PluginParameterDTO updateMetadataParam = new PluginParameterDTO();
        updateMetadataParam.setName("updateMetadata");
        updateMetadataParam.setLabel("更新元数据");
        updateMetadataParam.setDescription("是否更新音频文件元数据中的音轨编号");
        updateMetadataParam.setType("boolean");
        updateMetadataParam.setDefaultValue(true);
        updateMetadataParam.setRequired(false);
        parameters.add(updateMetadataParam);
        
        PluginParameterDTO preserveOriginalParam = new PluginParameterDTO();
        preserveOriginalParam.setName("preserveOriginal");
        preserveOriginalParam.setLabel("保留原始文件");
        preserveOriginalParam.setDescription("是否保留原始文件");
        preserveOriginalParam.setType("boolean");
        preserveOriginalParam.setDefaultValue(false);
        preserveOriginalParam.setRequired(false);
        parameters.add(preserveOriginalParam);
        
        PluginParameterDTO groupByDirectoryParam = new PluginParameterDTO();
        groupByDirectoryParam.setName("groupByDirectory");
        groupByDirectoryParam.setLabel("按目录分组编号");
        groupByDirectoryParam.setDescription("是否按目录分组进行编号");
        groupByDirectoryParam.setType("boolean");
        groupByDirectoryParam.setDefaultValue(true);
        groupByDirectoryParam.setRequired(false);
        parameters.add(groupByDirectoryParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        String mode = (String) config.getValue("mode", "default");
        int startNumber = (Integer) config.getValue("startNumber", 1);
        boolean padZero = (Boolean) config.getValue("padZero", true);
        String numberFormat = (String) config.getValue("numberFormat", "01");
        String separator = (String) config.getValue("separator", ". ");
        boolean updateMetadata = (Boolean) config.getValue("updateMetadata", true);
        boolean preserveOriginal = (Boolean) config.getValue("preserveOriginal", false);
        boolean groupByDirectory = (Boolean) config.getValue("groupByDirectory", true);
        
        List<String> audioFiles = filterAudioFiles(filePaths);
        
        if (groupByDirectory) {
            changes.addAll(processByDirectory(audioFiles, mode, startNumber, padZero, numberFormat, separator, updateMetadata, preserveOriginal));
        } else {
            changes.addAll(processAll(audioFiles, mode, startNumber, padZero, numberFormat, separator, updateMetadata, preserveOriginal));
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }
    
    private List<String> filterAudioFiles(List<String> filePaths) {
        List<String> audioFiles = new ArrayList<>();
        String[] audioExtensions = {".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".wma"};
        
        for (String filePath : filePaths) {
            String lowerPath = filePath.toLowerCase();
            for (String ext : audioExtensions) {
                if (lowerPath.endsWith(ext)) {
                    audioFiles.add(filePath);
                    break;
                }
            }
        }
        
        return audioFiles;
    }
    
    private List<ChangeRecord> processByDirectory(List<String> audioFiles, String mode, int startNumber, 
            boolean padZero, String numberFormat, String separator, boolean updateMetadata, boolean preserveOriginal) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        java.util.Map<String, List<String>> dirGroups = new java.util.HashMap<>();
        
        for (String filePath : audioFiles) {
            File file = new File(filePath);
            String dirPath = file.getParent();
            
            if (!dirGroups.containsKey(dirPath)) {
                dirGroups.put(dirPath, new ArrayList<>());
            }
            dirGroups.get(dirPath).add(filePath);
        }
        
        for (List<String> dirFiles : dirGroups.values()) {
            List<ChangeRecord> dirChanges = processFiles(dirFiles, mode, startNumber, padZero, numberFormat, separator, updateMetadata, preserveOriginal);
            changes.addAll(dirChanges);
        }
        
        return changes;
    }
    
    private List<ChangeRecord> processAll(List<String> audioFiles, String mode, int startNumber, 
            boolean padZero, String numberFormat, String separator, boolean updateMetadata, boolean preserveOriginal) {
        return processFiles(audioFiles, mode, startNumber, padZero, numberFormat, separator, updateMetadata, preserveOriginal);
    }
    
    private List<ChangeRecord> processFiles(List<String> audioFiles, String mode, int startNumber, 
            boolean padZero, String numberFormat, String separator, boolean updateMetadata, boolean preserveOriginal) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        List<String> sortedFiles = sortFiles(audioFiles, mode);
        
        for (int i = 0; i < sortedFiles.size(); i++) {
            String filePath = sortedFiles.get(i);
            int trackNumber = startNumber + i;
            
            String formattedNumber = formatNumber(trackNumber, padZero, numberFormat);
            String newFileName = generateNewFileName(filePath, formattedNumber, separator);
            
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(newFileName);
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.RENAME);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            
            changes.add(record);
        }
        
        return changes;
    }
    
    private List<String> sortFiles(List<String> files, String mode) {
        List<String> sortedFiles = new ArrayList<>(files);
        
        switch (mode) {
            case "metadata":
                sortedFiles.sort(Comparator.comparing(this::extractTrackNumber));
                break;
            case "textList":
            case "cueFile":
            case "custom":
                sortedFiles.sort(Comparator.naturalOrder());
                break;
            case "default":
            default:
                Collections.sort(sortedFiles);
                break;
        }
        
        return sortedFiles;
    }
    
    private int extractTrackNumber(String filePath) {
        File file = new File(filePath);
        String fileName = file.getName();
        
        Pattern pattern = Pattern.compile("^(\\d+)[\\s.-]");
        Matcher matcher = pattern.matcher(fileName);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
        
        return Integer.MAX_VALUE;
    }
    
    private String formatNumber(int number, boolean padZero, String numberFormat) {
        if ("001".equals(numberFormat)) {
            return String.format("%03d", number);
        } else if ("01".equals(numberFormat) || padZero) {
            return String.format("%02d", number);
        } else {
            return String.valueOf(number);
        }
    }
    
    private String generateNewFileName(String filePath, String trackNumber, String separator) {
        File file = new File(filePath);
        String fileName = file.getName();
        String extension = "";
        
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex);
            fileName = fileName.substring(0, dotIndex);
        }
        
        fileName = fileName.replaceAll("^\\d+[\\s.-]+", "");
        
        return trackNumber + separator + fileName + extension;
    }
    
    private List<String> loadTextList(String filePath) {
        List<String> trackList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    trackList.add(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return trackList;
    }
}
