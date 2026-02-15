package com.filemanager.plugin.impl.tracknumber;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    protected void initConfigFields() {
        List<EnumOptionDTO> modeOptions = new ArrayList<>();
        modeOptions.add(createEnumOption("default", "默认模式"));
        modeOptions.add(createEnumOption("sequential", "顺序编号"));
        modeOptions.add(createEnumOption("preserve", "保留原号"));
        addEnumConfigField("mode", "编号模式", "select", "default", 
            "选择音轨编号的模式", true, modeOptions);
        
        List<EnumOptionDTO> formatOptions = new ArrayList<>();
        formatOptions.add(createEnumOption("01", "01"));
        formatOptions.add(createEnumOption("1", "1"));
        formatOptions.add(createEnumOption("001", "001"));
        addEnumConfigField("numberFormat", "编号格式", "select", "01", 
            "音轨编号的格式", false, formatOptions);
        
        addConfigField("startNumber", "起始编号", "number", 1, 
            "音轨编号的起始数字", false);
        addConfigField("padZero", "双位补零", "boolean", true, 
            "是否使用双位补零（如01, 02）", false);
        addConfigField("separator", "分隔符", "text", ". ", 
            "音轨编号与文件名之间的分隔符", false);
        addConfigField("updateMetadata", "更新元数据", "boolean", true, 
            "是否更新音频文件元数据中的音轨编号", false);
        addConfigField("preserveOriginal", "保留原始文件", "boolean", false, 
            "是否保留原始文件", false);
        addConfigField("groupByDirectory", "按目录分组编号", "boolean", true, 
            "是否按目录分组进行编号", false);
    }

    private EnumOptionDTO createEnumOption(String value, String label) {
        EnumOptionDTO option = new EnumOptionDTO();
        option.setValue(value);
        option.setLabel(label);
        return option;
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
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File file = currentRecord.getFileHandle();
        if (!file.isFile()) {
            return Collections.emptyList();
        }
        
        String separator = getConfigValue(config, "separator", ". ");
        
        String formattedNumber = formatNumber(currentTrackNumber, config);
        String newFileName = generateNewFileName(file.getPath(), formattedNumber, separator);
        String newPath = file.getParent() + File.separator + newFileName;
        
        context.logInfo("分析音轨编号: " + file.getName() + " -> " + newFileName);
        
        Map<String, String> params = new HashMap<>();
        params.put("trackNumber", formattedNumber);
        params.put("separator", separator);
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            newFileName,
            currentRecord.getFileHandle(),
            true,
            newPath,
            OperationType.RENAME,
            params,
            ExecStatus.PENDING
        );
        
        incrementTrackNumber();
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File sourceFile = record.getFileHandle();
        File targetFile = new File(record.getNewPath());
        
        if (!sourceFile.exists()) {
            context.logWarn("源文件不存在: " + sourceFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        boolean preserveOriginal = getConfigValue(config, "preserveOriginal", false);
        
        if (targetFile.exists() && !preserveOriginal) {
            context.logWarn("目标文件已存在: " + targetFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        try {
            if (sourceFile.renameTo(targetFile)) {
                context.logInfo("添加音轨编号: " + sourceFile.getPath() + " -> " + targetFile.getPath());
                record.setStatus(ExecStatus.SUCCESS.name());
            } else {
                context.logError("重命名文件失败: " + sourceFile.getPath());
                record.setStatus(ExecStatus.FAILED.name());
            }
        } catch (Exception e) {
            context.logError("处理文件失败: " + sourceFile.getPath() + ", 错误: " + e.getMessage());
            record.setStatus(ExecStatus.FAILED.name());
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
