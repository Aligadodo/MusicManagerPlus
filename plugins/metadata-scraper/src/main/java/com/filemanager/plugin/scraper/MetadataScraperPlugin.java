package com.filemanager.plugin.scraper;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetadataScraperPlugin implements IPlugin {
    @Override
    public String getId() {
        return "metadata-scraper";
    }

    @Override
    public String getName() {
        return "元数据抓取插件";
    }

    @Override
    public String getDescription() {
        return "从网络或本地抓取并更新文件的元数据信息";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("sources", Arrays.asList("discogs", "musicbrainz", "local"));
        config.setValue("updateTags", true);
        config.setValue("updateCoverArt", true);
        config.setValue("forceUpdate", false);
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        // 实现元数据抓取逻辑
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(filePath); // 文件名不变，只更新元数据
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.METADATA_UPDATE);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }
}
