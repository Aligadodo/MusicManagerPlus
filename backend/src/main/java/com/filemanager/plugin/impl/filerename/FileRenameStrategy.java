package com.filemanager.plugin.impl.filerename;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class FileRenameStrategy extends AbstractConfigurableStrategy {

    private int currentIndex;

    public FileRenameStrategy() {
        super();
        this.currentIndex = 1;
    }

    @Override
    public String getId() {
        return "file-rename";
    }

    @Override
    public String getName() {
        return "文件重命名";
    }

    @Override
    public String getDescription() {
        return "根据规则批量重命名文件";
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
        addConfigField("pattern", "命名模式", "text", "{name}_{index}", 
            "重命名的命名模式，支持{name}原文件名, {index}序号", true);
        addConfigField("startIndex", "起始序号", "number", 1, 
            "序号的起始值", false);
        addConfigField("padZeros", "补零", "boolean", true, 
            "是否对序号进行补零", false);
        addConfigField("zeroPadding", "补零长度", "number", 3, 
            "序号补零的长度", false);
        addConfigField("preserveExtension", "保留扩展名", "boolean", true, 
            "是否保留原文件扩展名", false);
        addConfigField("overwriteExisting", "覆盖现有文件", "boolean", false, 
            "是否覆盖已存在的同名文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "pattern", "{name}_{index}");
        setConfigValue(config, "startIndex", 1);
        setConfigValue(config, "padZeros", true);
        setConfigValue(config, "zeroPadding", 3);
        setConfigValue(config, "preserveExtension", true);
        setConfigValue(config, "overwriteExisting", false);
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File file = currentRecord.getFileHandle();
        if (!file.isFile()) {
            return Collections.emptyList();
        }
        
        String pattern = getConfigValue(config, "pattern", "{name}_{index}");
        boolean preserveExtension = getConfigValue(config, "preserveExtension", true);
        boolean padZeros = getConfigValue(config, "padZeros", true);
        int zeroPadding = getConfigValue(config, "zeroPadding", 3);
        
        context.logInfo("分析文件重命名: " + file.getName() + ", 模式: " + pattern);
        
        String fileName = file.getName();
        String dir = file.getParent() + File.separator;
        String extension = "";
        String baseName = fileName;
        
        if (preserveExtension && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf('.'));
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        
        String indexStr = padZeros ? String.format("%0" + zeroPadding + "d", currentIndex) : String.valueOf(currentIndex);
        String newName = pattern
            .replace("{name}", baseName)
            .replace("{index}", indexStr);
        
        String newFilePath = dir + newName + extension;
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            newName + extension,
            currentRecord.getFileHandle(),
            true,
            newFilePath,
            OperationType.RENAME,
            new java.util.HashMap<>(),
            ExecStatus.PENDING
        );
        
        record.setId("change-" + System.currentTimeMillis() + "-" + file.hashCode());
        record.setFilePath(file.getAbsolutePath());
        
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
        
        boolean overwriteExisting = getConfigValue(config, "overwriteExisting", false);
        
        if (targetFile.exists() && !overwriteExisting) {
            context.logWarn("目标文件已存在: " + targetFile.getName());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        if (sourceFile.renameTo(targetFile)) {
            context.logInfo("重命名文件: " + sourceFile.getPath() + " -> " + targetFile.getPath());
            record.setStatus(ExecStatus.SUCCESS.name());
        } else {
            context.logError("重命名文件失败: " + sourceFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
        }
    }
}
