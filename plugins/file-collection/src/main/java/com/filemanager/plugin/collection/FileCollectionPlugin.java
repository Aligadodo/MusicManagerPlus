package com.filemanager.plugin.collection;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileCollectionPlugin implements IPlugin {
    @Override
    public String getId() {
        return "file-collection";
    }

    @Override
    public String getName() {
        return "文件收集插件";
    }

    @Override
    public String getDescription() {
        return "根据配置规则收集和整理文件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("targetDirectory", "/tmp/collected");
        config.setValue("recursive", true);
        config.setValue("includePatterns", Arrays.asList("*.mp3", "*.wav", "*.flac"));
        config.setValue("excludePatterns", Arrays.asList("*.tmp", "*.log"));
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        // 实现文件收集逻辑
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getTargetPath(filePath, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.MOVE);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    private String getTargetPath(String filePath, PluginConfigDTO config) {
        String targetDir = (String) config.getValue("targetDirectory");
        if (targetDir == null) {
            targetDir = "/tmp/collected";
        }
        
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        return targetDir + "/" + fileName;
    }
}
