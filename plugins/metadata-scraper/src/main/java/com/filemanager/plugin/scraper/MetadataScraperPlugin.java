package com.filemanager.plugin.scraper;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
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
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        // 源配置参数
        PluginParameterDTO sourcesParam = new PluginParameterDTO();
        sourcesParam.setName("sources");
        sourcesParam.setLabel("元数据源");
        sourcesParam.setDescription("选择要从哪些源抓取元数据");
        sourcesParam.setType("select");
        sourcesParam.setDefaultValue(Arrays.asList("discogs", "musicbrainz", "local"));
        sourcesParam.setRequired(true);
        sourcesParam.setOptions(new String[]{"discogs", "musicbrainz", "local"});
        parameters.add(sourcesParam);
        
        // 更新标签参数
        PluginParameterDTO updateTagsParam = new PluginParameterDTO();
        updateTagsParam.setName("updateTags");
        updateTagsParam.setLabel("更新标签");
        updateTagsParam.setDescription("是否更新文件的标签信息");
        updateTagsParam.setType("boolean");
        updateTagsParam.setDefaultValue(true);
        updateTagsParam.setRequired(false);
        parameters.add(updateTagsParam);
        
        // 更新封面参数
        PluginParameterDTO updateCoverArtParam = new PluginParameterDTO();
        updateCoverArtParam.setName("updateCoverArt");
        updateCoverArtParam.setLabel("更新封面");
        updateCoverArtParam.setDescription("是否更新文件的封面艺术");
        updateCoverArtParam.setType("boolean");
        updateCoverArtParam.setDefaultValue(true);
        updateCoverArtParam.setRequired(false);
        parameters.add(updateCoverArtParam);
        
        // 强制更新参数
        PluginParameterDTO forceUpdateParam = new PluginParameterDTO();
        forceUpdateParam.setName("forceUpdate");
        forceUpdateParam.setLabel("强制更新");
        forceUpdateParam.setDescription("是否强制更新所有元数据，即使已有数据");
        forceUpdateParam.setType("boolean");
        forceUpdateParam.setDefaultValue(false);
        forceUpdateParam.setRequired(false);
        parameters.add(forceUpdateParam);
        
        return parameters;
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

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        // 实现元数据抓取预览逻辑
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
