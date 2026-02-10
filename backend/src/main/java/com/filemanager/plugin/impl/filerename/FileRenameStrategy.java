package com.filemanager.plugin.impl.filerename;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import java.io.File;
import java.util.ArrayList;
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
    public List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
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
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String pattern = getConfigValue(config, "pattern", "{name}_{index}");
        boolean preserveExtension = getConfigValue(config, "preserveExtension", true);
        boolean padZeros = getConfigValue(config, "padZeros", true);
        int zeroPadding = getConfigValue(config, "zeroPadding", 3);

        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
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

        ChangeRecord record = createChangeRecord(filePath, newFilePath, "PENDING");
        record.setOperationType("RENAME");
        record.setReason("应用重命名规则");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String pattern = getConfigValue(config, "pattern", "{name}_{index}");
        boolean preserveExtension = getConfigValue(config, "preserveExtension", true);
        boolean padZeros = getConfigValue(config, "padZeros", true);
        int zeroPadding = getConfigValue(config, "zeroPadding", 3);
        boolean overwriteExisting = getConfigValue(config, "overwriteExisting", false);

        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        try {
            String fileName = sourceFile.getName();
            String dir = sourceFile.getParent() + File.separator;
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

            File targetFile = new File(dir + newName + extension);

            if (targetFile.exists() && !overwriteExisting) {
                context.logWarn("Target file already exists: " + targetFile.getName());
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }

            if (sourceFile.renameTo(targetFile)) {
                context.logInfo("Renamed file: " + filePath + " -> " + targetFile.getPath());
                ChangeRecord record = createChangeRecord(filePath, targetFile.getPath(), "SUCCESS");
                record.setOperationType("RENAME");
                record.setReason("应用重命名规则");
                return record;
            } else {
                context.logError("Failed to rename file: " + filePath);
                return createChangeRecord(filePath, filePath, "ERROR");
            }
        } catch (Exception e) {
            context.logError("Error renaming file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }
}
