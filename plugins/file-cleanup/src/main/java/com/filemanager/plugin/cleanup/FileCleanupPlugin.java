package com.filemanager.plugin.cleanup;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileCleanupPlugin implements IPlugin {
    @Override
    public String getId() {
        return "file-cleanup";
    }

    @Override
    public String getName() {
        return "文件清理插件";
    }

    @Override
    public String getDescription() {
        return "根据配置规则清理不需要的文件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("maxFileAgeDays", 30);
        config.setValue("minFileSizeKB", 0);
        config.setValue("maxFileSizeKB", 10240);
        config.setValue("deleteEmptyDirectories", true);
        config.setValue("includePatterns", Arrays.asList("*.tmp", "*.log", "*.bak"));
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        // 实现文件清理逻辑
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(null); // 删除操作
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.DELETE);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }
}
