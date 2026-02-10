package com.filemanager.plugin.impl.tracknumber;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackNumberStrategy extends AbstractConfigurableStrategy {

    private int currentTrackNumber;

    public TrackNumberStrategy() {
        super();
        this.currentTrackNumber = 1;
    }

    @Override
    public String getId() {
        return "track-number";
    }

    @Override
    public String getName() {
        return "音轨编号";
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
    public List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addConfigField("mode", "编号模式", "select", "default", 
            "选择音轨编号的模式", true);
        addConfigField("startNumber", "起始编号", "number", 1, 
            "音轨编号的起始数字", false);
        addConfigField("padZero", "双位补零", "boolean", true, 
            "是否使用双位补零（如01, 02）", false);
        addConfigField("numberFormat", "编号格式", "select", "01", 
            "音轨编号的格式", false);
        addConfigField("separator", "分隔符", "text", ". ", 
            "音轨编号与文件名之间的分隔符", false);
        addConfigField("updateMetadata", "更新元数据", "boolean", true, 
            "是否更新音频文件元数据中的音轨编号", false);
        addConfigField("preserveOriginal", "保留原始文件", "boolean", false, 
            "是否保留原始文件", false);
        addConfigField("groupByDirectory", "按目录分组编号", "boolean", true, 
            "是否按目录分组进行编号", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "mode", "default");
        setConfigValue(config, "startNumber", 1);
        setConfigValue(config, "padZero", true);
        setConfigValue(config, "numberFormat", "01");
        setConfigValue(config, "separator", ". ");
        setConfigValue(config, "updateMetadata", true);
        setConfigValue(config, "preserveOriginal", false);
        setConfigValue(config, "groupByDirectory", true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String mode = getConfigValue(config, "mode", "default");
        String separator = getConfigValue(config, "separator", ". ");
        
        String formattedNumber = formatNumber(currentTrackNumber, config);
        String newFileName = generateNewFileName(filePath, formattedNumber, separator);
        
        ChangeRecord record = createChangeRecord(filePath, newFileName, "PENDING");
        record.setOperationType("RENAME");
        record.setReason("添加音轨编号: " + formattedNumber);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String mode = getConfigValue(config, "mode", "default");
        String separator = getConfigValue(config, "separator", ". ");
        boolean preserveOriginal = getConfigValue(config, "preserveOriginal", false);
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        try {
            String formattedNumber = formatNumber(currentTrackNumber, config);
            String newFileName = generateNewFileName(filePath, formattedNumber, separator);
            File targetFile = new File(sourceFile.getParent(), newFileName);
            
            if (targetFile.exists() && !preserveOriginal) {
                context.logWarn("Target file already exists: " + newFileName);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            if (sourceFile.renameTo(targetFile)) {
                context.logInfo("Added track number: " + filePath + " -> " + targetFile.getPath());
                ChangeRecord record = createChangeRecord(filePath, targetFile.getPath(), "SUCCESS");
                record.setOperationType("RENAME");
                record.setReason("添加音轨编号: " + formattedNumber);
                return record;
            } else {
                context.logError("Failed to rename file: " + filePath);
                return createChangeRecord(filePath, filePath, "ERROR");
            }
        } catch (Exception e) {
            context.logError("Error processing file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String formatNumber(int number, StrategyConfigDTO config) {
        String numberFormat = getConfigValue(config, "numberFormat", "01");
        boolean padZero = getConfigValue(config, "padZero", true);
        
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

    public void setCurrentTrackNumber(int number) {
        this.currentTrackNumber = number;
    }

    public int getCurrentTrackNumber() {
        return this.currentTrackNumber;
    }

    public void incrementTrackNumber() {
        this.currentTrackNumber++;
    }
}
